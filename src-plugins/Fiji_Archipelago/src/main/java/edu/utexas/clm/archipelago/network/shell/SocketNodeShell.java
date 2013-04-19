/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.network.shell;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import com.jcraft.jsch.*;
import edu.utexas.clm.archipelago.network.server.ArchipelagoServer;
import ij.IJ;
import ij.gui.GenericDialog;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class SocketNodeShell implements NodeShell
{

    private static class SNSUserInfo implements UserInfo
    {

        private String passPhrase = "";
        private boolean displayEnabled = true;
        private boolean passSet = false;

        public String getPassphrase() {
            return passPhrase;
        }

        public String getPassword() {
            return null;
        }

        public boolean promptPassword(String s) {
            return true;
        }

        public boolean promptPassphrase(String s) {
            if (displayEnabled && !passSet)
            {
                GenericDialog gd = new GenericDialog("Enter Passphrase");

                gd.addStringField("Please enter the public key passphrase", "");

                gd.showDialog();

                passPhrase = gd.getNextString();

                passSet = true;
            }
            return true;
        }

        public boolean promptYesNo(String s) {
            return true;
        }

        public void showMessage(String s)
        {
            if (displayEnabled)
            {
                IJ.showMessage(s);
            }
        }

        public void enableDisplay()
        {
            displayEnabled = true;
        }

        public void disableDisplay()
        {
            displayEnabled = false;
        }

        public void unsetPass()
        {
            passSet = false;
        }

    }

    private class JSchShellExecThread extends Thread
    {
        private final long node;
        private final Channel channel;
        private final NodeShellListener listener;
        private final Session session;


        public JSchShellExecThread(long id, Channel c, Session s, NodeShellListener l)
        {
            node = id;
            channel = c;
            listener = l;
            session = s;
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

    private static final SocketNodeShell shell = new SocketNodeShell();

    private boolean exec(JSch jsch,
                         UserInfo ui,
                         NodeManager.NodeParameters param,
                         String command,
                         NodeShellListener listener,
                         int port) throws JSchException
    {
        Session session = jsch.getSession(param.getUser(), param.getHost(), port);
        Channel channel;

        session.setUserInfo(ui);
        session.connect();

        channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);


        new JSchShellExecThread(param.getID(), channel, session, listener).start();

        return true;
    }

    public boolean start(final NodeManager.NodeParameters param, final NodeShellListener listener)
            throws ShellExecutionException
    {
        final String eroot = param.getExecRoot();
        int port;
        String keyfile, executableFP, arguments;
        final ArchipelagoServer server = ArchipelagoServer.getServer(listener);

        try
        {
            port = param.getShellParams().getInteger("ssh-port");
            keyfile = param.getShellParams().getString("keyfile");
            executableFP = eroot + "/" + param.getShellParams().getString("executable");
            arguments = "--jar-path " + eroot +
                    "/plugins/ --jar-path " + eroot +"/jars/ --classpath " + eroot +
                    " --allow-multiple --main-class edu.utexas.clm.archipelago.Fiji_Archipelago " +
                    Cluster.getCluster().getLocalHostName() + " " + server.getPort() + " " + param.getID() +
                    " 2>&1 > ~/" + param.getHost() + "_" + param.getID() + ".log";
        }
        catch (Exception e)
        {
            throw new ShellExecutionException(e);
        }
        
        try
        {
            final JSch jsch = new JSch();
            final UserInfo ui = new SNSUserInfo();
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

            jsch.addIdentity((new File(keyfile)).getAbsolutePath());
            
            try
            {
                lock.lock();
                exec(jsch, ui, param, "test -e " + executableFP, existListener, port);
                lock.unlock();
                Thread.sleep(Long.MAX_VALUE);
                throw new ShellExecutionException("The Universe ended while " +
                        "waiting to test for existence of executable");
            }
            catch (InterruptedException ie)
            {
                // We expect to be interrupted
                if (result.get() != 0)
                {
                    // If we get a nonzero return, then fiji does not exist.
                    throw new ShellExecutionException(executableFP +
                            " does not exist on host " + param.getHost());
                }
            }
            
            
            return exec(jsch, ui, param, executableFP + " " + arguments, listener, port);

        }
        catch (JSchException jse)
        {
            if (jse.getMessage().equals("Auth cancel"))
            {
                throw new ShellExecutionException(
                        "Authentication failed on " + param.getHost(), jse);
            }
            else if(jse.getCause() != null && jse.getCause() instanceof UnknownHostException)
            {
                throw new ShellExecutionException("Unknown host " + param.getHost(), jse);
            }

            throw new ShellExecutionException(jse);
        }
        
    }

    public NodeShellParameters defaultParameters()
    {
        final String userHome = System.getProperty("user.home");
        final String sep = "/";
        final NodeShellParameters nsp = new NodeShellParameters();
        nsp.addKey("ssh-port", 22);
        nsp.addKey("keyfile", new File(userHome + sep + ".ssh" + sep + "id_dsa"));
        nsp.addKey("executable", "fiji");
        return nsp;
    }

    public String paramToString(NodeShellParameters nsp)
    {
        try
        {
            return ":" + nsp.getInteger("ssh-port");
        }
        catch (Exception e)
        {
            return "";
        }
    }
    
    public String name()
    {
        return "Insecure Socket Shell";
    }
    
    public String description()
    {
        return "Uses ssh to cause remote nodes to connect via an insecure socket";
    }

    public static SocketNodeShell getShell()
    {
        return shell;
    }
    
}
