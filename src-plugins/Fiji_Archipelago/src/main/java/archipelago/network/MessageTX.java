package archipelago.network;


import archipelago.data.ClusterMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageTX
{

    public static final long DEFAULT_WAIT = 10000;
    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;

    private final ArrayBlockingQueue<ClusterMessage> messageQ;
    private final ObjectOutputStream objectStream;
    private final Thread t;
    private final AtomicBoolean active;
    private final long waitTime;
    private final TimeUnit tUnit;
    
    public MessageTX(Socket s) throws IOException
    {
        this(s, DEFAULT_WAIT, DEFAULT_UNIT);
    }
    
    public MessageTX(Socket socket, long wait, TimeUnit unit) throws IOException
    {
        messageQ = new ArrayBlockingQueue<ClusterMessage>(16, true);
        objectStream = new ObjectOutputStream(socket.getOutputStream());
        active = new AtomicBoolean(true);
        waitTime = wait;
        tUnit = unit;

        t = new Thread()
        {
            public void run()
            {
                while (active.get())
                {
                    ClusterMessage nextMessage = null;
                    try
                    {
                        nextMessage = messageQ.poll(waitTime, tUnit);
                    } 
                    catch (InterruptedException ie)
                    {
                        active.set(false);
                    }
                    
                    if (nextMessage != null)
                    {
                        try
                        {
                            objectStream.writeObject(nextMessage);
                        }
                        catch (IOException ioe)
                        {
                            active.set(false);
                        }
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
    
    public boolean queueMessage(ClusterMessage message)
    {
        try
        {
            messageQ.put(message);
            return true;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }
    
    
    
}
