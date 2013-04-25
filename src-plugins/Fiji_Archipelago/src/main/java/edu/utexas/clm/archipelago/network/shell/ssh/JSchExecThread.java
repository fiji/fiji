package edu.utexas.clm.archipelago.network.shell.ssh;

import com.jcraft.jsch.*;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.listen.NodeShellListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class JSchExecThread
{
    private final long node;
    private final Channel channel;
    private final Session session;

    private class ExecThread extends Thread
    {
        private final NodeShellListener listener;

        public ExecThread(NodeShellListener l)
        {
            listener = l;
        }

        public void run()
        {
            String host = "";
            try
            {
                host = channel.getSession().getHost();

                FijiArchipelago.debug(host + ": connecting channel");
                channel.connect();

                FijiArchipelago.debug(host + ": connect() returned");

                while (!channel.isClosed())
                {
                    Thread.sleep(1000);
                }

                FijiArchipelago.debug(host + ": channel closed");

                listener.execFinished(node, null, channel.getExitStatus());
            }
            catch (JSchException jse)
            {
                FijiArchipelago.debug(host + ": JSchException: " + jse);
                listener.execFinished(node, jse, -1);
            }
            catch (InterruptedException ie)
            {
                FijiArchipelago.debug(host + ": Interrupted");
                listener.execFinished(node, ie, -1);
            }

            channel.disconnect();
            session.disconnect();
        }
    }
    

    public JSchExecThread(long id, Session s) throws JSchException
    {
        node = id;
        session = s;
        channel = session.openChannel("exec");
    }

    public Channel exec(UserInfo ui,
                        String command,
                        NodeShellListener listener) throws JSchException
    {
        if (!session.isConnected())
        {
            session.setUserInfo(ui);
            session.connect();
        }

        ((ChannelExec)channel).setCommand(command);

        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);

        new ExecThread(listener).start();

        return channel;
    }

    public boolean fileExists(UserInfo ui, String file) throws JSchException
    {
        final AtomicInteger result = new AtomicInteger(-1);
        final ReentrantLock lock = new ReentrantLock();
        final Thread t = Thread.currentThread();

        final NodeShellListener existListener = new NodeShellListener() {
            public void execFinished(long nodeID, Exception e, int status)
            {
                lock.lock();
                result.set(status);
                t.interrupt();
                lock.unlock();
            }

            public void ioStreamsReady(InputStream is, OutputStream os) {}
        };

        try
        {
            lock.lock();
            exec(ui, "test -e " + file, existListener);
            lock.unlock();
            Thread.sleep(Long.MAX_VALUE);
            // The Universe ends, and we return false.
            return false;
        }
        catch (InterruptedException ie)
        {
            // We expect to be interrupted
            return (result.get() == 0);
        }

    }

}