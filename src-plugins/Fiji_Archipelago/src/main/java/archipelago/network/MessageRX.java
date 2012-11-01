package archipelago.network;


import archipelago.data.ClusterMessage;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageRX
{

    private final MessageListener listener;
    private final ObjectInputStream objectStream;
    private final AtomicBoolean active;
    private final Thread t;
    
    public MessageRX(Socket socket, MessageListener ml) throws IOException
    {
        listener = ml;        
        active = new AtomicBoolean(true);
        
        objectStream = new ObjectInputStream(socket.getInputStream());
        
        t = new Thread()
        {
            public void run()
            {
                while (active.get())
                {
                    try
                    {
                        ClusterMessage message = (ClusterMessage)objectStream.readObject();
                        listener.handleMessage(message);
                    }
                    catch (ClassCastException cce)
                    {
                        //
                    }
                    catch (IOException ioe)
                    {
                        active.set(false);
                    }
                    catch (ClassNotFoundException cfne)
                    {
                        //
                    }
                }
            }
        };
        
        t.start();
    }

    public void close()
    {
        active.set(false);
        t.interrupt();
    }
    
    public boolean join()
    {
        if (t.isAlive())
        {
            try
            {
                t.join();
                return true;
            }
            catch (InterruptedException ie)
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }
    
    
    
    
}
