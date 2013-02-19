package archipelago.network.client;

import archipelago.FijiArchipelago;
import archipelago.listen.MessageListener;
import archipelago.listen.TransceiverListener;
import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.MessageXC;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 *
 * @author Larry Lindsey
 */
public class ArchipelagoClient implements MessageListener, TransceiverListener
{
    
    private final MessageXC xc;
    private long clientId = 0;
    private final AtomicBoolean active;
    private final Vector<ProcessThread> runningThreads;
    
    
    private class ProcessThread extends Thread
    {
        private final ProcessManager process;
        private final AtomicBoolean running;
        
        public ProcessThread(ProcessManager pm)
        {
            process = pm;
            running = new AtomicBoolean(true);
        }
        
        public void cancel()
        {
            running.set(false);
            this.interrupt();
        }
        
        public long getID()
        {
            return process.getID(); 
        }
        
        public void run()
        {
            process.run();
            runningThreads.remove(this);
            if (running.get() && active.get())
            {
                xc.queueMessage("process", process);
            }
        }
    }
    

    public ArchipelagoClient(long id, String host, InputStream inStream, OutputStream outStream) throws IOException
    {
        try
        {
            clientId = id;
            xc = new MessageXC(inStream, outStream, this, host);

            runningThreads = new Vector<ProcessThread>();
            
            active = new AtomicBoolean(true);
        }
        catch (IOException ioe)
        {
            System.out.println("Caught an IOE: " + ioe);
            throw ioe;
        }
    }
    
    

    public void handleMessage(final ClusterMessage cm) {

        final String message = cm.message;
        final Object object = cm.o;

        try
        {
            if (message.equals("process"))
            {
                final ProcessManager<?> pm = (ProcessManager<?>)object;
                final ProcessThread pt = new ProcessThread(pm);
                runningThreads.add(pt);
                pt.start();
            }
            else if (message.equals("halt"))
            {
                close();
            }
            else if (message.equals("ping"))
            {
                xc.queueMessage(cm);
            }
            else if (message.equals("user"))
            {
                cm.o = System.getProperty("user.name");
                xc.queueMessage(cm);
            }
            else if (message.equals("setid"))
            {
                clientId = (Long)object;
            }
            else if (message.equals("getid"))
            {
                xc.queueMessage("id", clientId);
            }
            else if (message.equals("setfileroot"))
            {
                FijiArchipelago.setFileRoot((String) object);
            }
            else if (message.equals("setexecroot"))
            {
                FijiArchipelago.setExecRoot((String) object);
            }
            else if (message.equals("getfileroot"))
            {
                xc.queueMessage("setfileroot", FijiArchipelago.getFileRoot());
            }
            else if (message.equals("getexecroot"))
            {
                xc.queueMessage("setexecroot", FijiArchipelago.getExecRoot());
            }
            else if (message.equals("cancel"))
            {
                long id = (Long)object;
                for (ProcessThread pt : runningThreads)
                {
                    if (pt.getID() == id)
                    {
                        pt.cancel();
                        runningThreads.remove(pt);
                        return;
                    }
                }
            }
        }
        catch (ClassCastException cce)
        {
            System.err.println("Caught CCE: " + cce);
            xc.queueMessage("error", cce);
        }
    }

    public boolean join()
    {
        return xc.join();
    }
    
    public boolean isActive()
    {
        return active.get();
    }
    
    public synchronized void close()
    {
        if (active.get())
        {
            active.set(false);
            
            for (ProcessThread t : runningThreads)
            {
                t.cancel();
            }
            
            xc.close();
        }
        
    }

    public synchronized void streamClosed()
    {
        FijiArchipelago.log("Lost socket connection");
        close();
    }
}
