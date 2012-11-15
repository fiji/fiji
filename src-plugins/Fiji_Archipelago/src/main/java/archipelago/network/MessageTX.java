package archipelago.network;


import archipelago.FijiArchipelago;
import archipelago.StreamCloseListener;
import archipelago.data.ClusterMessage;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
    private final String remoteHost;
    
    public MessageTX(Socket s, final StreamCloseListener listener) throws IOException
    {
        this(s, DEFAULT_WAIT, DEFAULT_UNIT, listener);
    }
    
    public MessageTX(Socket socket, long wait, TimeUnit unit, final StreamCloseListener listener) throws IOException
    {
        remoteHost = socket.getInetAddress().getCanonicalHostName();
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
                            FijiArchipelago.debug("TX: " + remoteHost + " Writing message " + nextMessage.message + " ... ");
                            objectStream.writeObject(nextMessage);
                            FijiArchipelago.debug("TX: " + remoteHost + " success.");
                        }
                        catch (NotSerializableException nse)
                        {
                            FijiArchipelago.err("TX " + remoteHost + " tried to send a non serializable object: " + nse);
                        }
                        catch (IOException ioe)
                        {
                            FijiArchipelago.err("TX " + remoteHost + " failed: " + ioe);

                            active.set(false);
                            listener.streamClosed();
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
            FijiArchipelago.debug("Queuing message to " + remoteHost);
            messageQ.put(message);
            FijiArchipelago.debug("Message to " + remoteHost + " queued successfully");
            return true;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }

    public boolean queueMessage(String message)
    {
        ClusterMessage cm = new ClusterMessage();
        cm.message = message;
        return queueMessage(cm);                
    }
    
    public boolean queueMessage(String message, Serializable o)
    {
        ClusterMessage cm = new ClusterMessage();
        cm.message = message;
        cm.o = o;
        return queueMessage(cm);
    }
    
}
    
    
    

