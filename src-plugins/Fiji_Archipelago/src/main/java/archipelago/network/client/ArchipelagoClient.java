package archipelago.network.client;

import archipelago.compute.ProcessManager;
import archipelago.data.ClusterMessage;
import archipelago.network.Cluster;
import archipelago.network.MessageListener;
import archipelago.network.MessageRX;
import archipelago.network.MessageTX;
import archipelago.network.server.ArchipelagoServer;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArchipelagoClient implements MessageListener
{
    
    private final Socket socket;
    private final MessageTX tx;
    private final MessageRX rx;
    
    private final AtomicBoolean active;

    public ArchipelagoClient(String host) throws IOException
    {
        this(host, Cluster.DEFAULT_PORT);
    }
    
    public ArchipelagoClient(String host, int port) throws IOException
    {
        socket = new Socket(host, port);
        tx = new MessageTX(socket);
        rx = new MessageRX(socket, this);
        active = new AtomicBoolean(true);
    }
    

    public void handleMessage(ClusterMessage message) {
        System.out.println("Got message " + message.message);
        
        if (message.message.equals("process"))
        {
            ProcessManager<?, ?> pm = (ProcessManager<?, ?>)message.o;
            pm.run();
            tx.queueMessage(message);
        } 
        else if (message.message.equals("halt"))
        {
            close();
        }
        else if (message.message.equals("ping"))
        {
            tx.queueMessage(message);
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
    
    public void close()
    {
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
        active.set(false);
        
    }
    
}
