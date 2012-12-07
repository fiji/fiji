package archipelago.network;

import archipelago.FijiArchipelago;
import archipelago.data.ClusterMessage;
import archipelago.listen.TransceiverListener;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Message transciever class
 */
public class MessageXC
{

    private class RXThread extends Thread
    {
        public void run()
        {
            while (active.get())
            {
                try
                {
                    ClusterMessage message = (ClusterMessage)objectInputStream.readObject();
                    FijiArchipelago.debug("RX: " + hostName + " Recieved message " + message.message);
                    xcListener.handleMessage(message);
                }
                catch (ClassCastException cce)
                {
                    FijiArchipelago.err("RX: " + hostName + " Got ClassCastException: " + cce);
                }
                catch (IOException ioe)
                {
                    close();
                }
                catch (ClassNotFoundException cfne)
                {
                    FijiArchipelago.err("RX: " + hostName + " Got ClassNotFoundException: " + cfne);
                }
            }
        }
    }
    
    private class TXThread extends Thread
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
                        FijiArchipelago.debug("TX: " + hostName + " Writing message " + nextMessage.message + " ... ");
                        objectOutputStream.writeObject(nextMessage);
                        FijiArchipelago.debug("TX: " + hostName + " success.");
                    }
                    catch (NotSerializableException nse)
                    {
                        FijiArchipelago.err("TX " + hostName + " tried to send a non serializable object: " + nse);
                    }
                    catch (IOException ioe)
                    {
                        FijiArchipelago.err("TX " + hostName + " failed: " + ioe);

                        active.set(false);
                        close();
                    }
                }
            }
        }
    }
    
    public static final long DEFAULT_WAIT = 10000;
    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;

    private final ArrayBlockingQueue<ClusterMessage> messageQ;
    private final ObjectOutputStream objectOutputStream;
    private final ObjectInputStream objectInputStream;
    private final Thread txThread, rxThread;
    private final AtomicBoolean active;
    private final long waitTime;
    private final TimeUnit tUnit;
    private final String hostName;
    private final TransceiverListener xcListener;

    public MessageXC(InputStream inStream, OutputStream outStream, final TransceiverListener listener, String hostName) throws IOException
    {
        this(inStream, outStream, listener, hostName, DEFAULT_WAIT, DEFAULT_UNIT);
    }

    public MessageXC(InputStream inStream, OutputStream outStream, final TransceiverListener listener, String name, long wait, TimeUnit unit) throws IOException
    {
        hostName = name;
        messageQ = new ArrayBlockingQueue<ClusterMessage>(16, true);
        objectOutputStream = new ObjectOutputStream(outStream);
        objectInputStream =  new ObjectInputStream(inStream);
        active = new AtomicBoolean(true);
        waitTime = wait;
        tUnit = unit;
        xcListener = listener;

        txThread = new TXThread();
        rxThread = new RXThread();

        rxThread.start();
        txThread.start();
    }

    public synchronized void close()
    {
        if (active.get())
        {
            active.set(false);
            xcListener.streamClosed();
            txThread.interrupt();
            rxThread.interrupt();
        }
    }

    public boolean join()
    {
        if (txThread.isAlive() || rxThread.isAlive())
        {
            try
            {
                txThread.join();
                rxThread.join();
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

    public synchronized boolean queueMessage(ClusterMessage message)
    {
        try
        {
            FijiArchipelago.debug("Queuing message to " + hostName);
            messageQ.put(message);
            FijiArchipelago.debug("Message to " + hostName + " queued successfully");
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
