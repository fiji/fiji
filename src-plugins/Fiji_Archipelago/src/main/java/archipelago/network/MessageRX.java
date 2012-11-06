package archipelago.network;


import archipelago.FijiArchipelago;
import archipelago.StreamCloseListener;
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
    
    public MessageRX(Socket socket, MessageListener ml, final StreamCloseListener closeListener) throws IOException
    {
        final String remoteHost = socket.getInetAddress().getCanonicalHostName();
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
                        FijiArchipelago.debug("RX: " + remoteHost + " Recieved message " + message.message);
                        listener.handleMessage(message);
                    }
                    catch (ClassCastException cce)
                    {
                        FijiArchipelago.err("RX: " + remoteHost + " Got ClassCastException: " + cce);
                    }
                    catch (IOException ioe)
                    {
                        active.set(false);
                        closeListener.streamClosed();
                    }
                    catch (ClassNotFoundException cfne)
                    {
                        FijiArchipelago.err("RX: " + remoteHost + " Got ClassNotFoundException: " + cfne);
                    }
                }
            }
        };

        FijiArchipelago.debug("RX: Listening on socket for address " + socket.getInetAddress());
        
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
