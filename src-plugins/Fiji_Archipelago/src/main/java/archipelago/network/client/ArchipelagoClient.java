package archipelago.network.client;

import archipelago.FijiArchipelago;
import archipelago.StreamCloseListener;
import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.Cluster;
import archipelago.network.MessageListener;
import archipelago.network.MessageRX;
import archipelago.network.MessageTX;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArchipelagoClient implements MessageListener, StreamCloseListener
{
    
    private final Socket socket;
    private final MessageTX tx;
    private final MessageRX rx;
    private long clientId = 0;
    
    private final AtomicBoolean active;
    
    private class ProcessThread extends Thread
    {
        private final ProcessManager process;
        
        public ProcessThread(ProcessManager pm)
        {
            process = pm;
        }
        
        public void run()
        {
            process.run();
            tx.queueMessage("process", process);
        }
    }
    

    public ArchipelagoClient(long id, String host) throws IOException
    {
        this(id, host, Cluster.DEFAULT_PORT);
    }

    public ArchipelagoClient(long id, String host, int port) throws IOException
    {
        System.out.println("Client with id " + id + " attempting connection to " + host + ":" + port);
        try
        {
            clientId = id;
            
            socket = new Socket(host, port);

            tx = new MessageTX(socket, this);
            rx = new MessageRX(socket, this, this);

            active = new AtomicBoolean(true);
        }
        catch (IOException ioe)
        {
            System.out.println("Caught an IOE: " + ioe);
            throw ioe;
        }
    }
    
    public ArchipelagoClient(String host) throws IOException
    {
        this(host, Cluster.DEFAULT_PORT);
    }
    
    public ArchipelagoClient(String host, int port) throws IOException
    {
        this(-1, host, port);
    }
    

    public void handleMessage(final ClusterMessage cm) {

        final String message = cm.message;
        final Object object = cm.o;

        try
        {
            if (message.equals("process"))
            {
                final ProcessManager<?, ?> pm = (ProcessManager<?, ?>)object;
                new ProcessThread(pm).start();
            }
            else if (message.equals("halt"))
            {
                close();
            }
            else if (message.equals("ping"))
            {
                tx.queueMessage(cm);
            }
            else if (message.equals("user"))
            {
                cm.o = System.getProperty("user.name");
                tx.queueMessage(cm);
            }
            else if (message.equals("setid"))
            {
                clientId = (Long)object;
            }
            else if (message.equals("getid"))
            {
                tx.queueMessage("id", clientId);
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
                tx.queueMessage("setfileroot", FijiArchipelago.getFileRoot());
            }
            else if (message.equals("getexecroot"))
            {
                tx.queueMessage("setexecroot", FijiArchipelago.getExecRoot());
            }
        }
        catch (ClassCastException cce)
        {
            System.err.println("Caught CCE: " + cce);
            tx.queueMessage("error", cce);
        }
    }

    public boolean join()
    {
        return tx.join() && rx.join();
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
            
            tx.close();
            rx.close();
            try
            {
                socket.close();
            }
            catch (IOException ioe)
            {
                //
            }
        }
        
    }

    public synchronized void streamClosed()
    {
        FijiArchipelago.log("Lost socket connection");
        close();
    }
}
