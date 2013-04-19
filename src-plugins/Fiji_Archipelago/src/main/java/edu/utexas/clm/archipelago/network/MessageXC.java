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

package edu.utexas.clm.archipelago.network;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.compute.ProcessManager;
import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.listen.TransceiverExceptionListener;
import edu.utexas.clm.archipelago.listen.TransceiverListener;

import java.io.*;
import java.util.ConcurrentModificationException;
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
                    // Don't debug beats, or they'll fill your log
                    if (message.type != MessageType.BEAT)
                    {
                        if (message.type == MessageType.PROCESS)
                        {
                            ProcessManager pm = (ProcessManager)message.o;
                            FijiArchipelago.debug("RX: Got message for job " + pm.getID());
                        }
                    }
                    xcListener.handleMessage(message);
                    objectInputStream = new ObjectInputStream(inStream);
                }
                catch (ClassCastException cce)
                {
                    xcExceptionListener.handleRXThrowable(cce, xc);
                }
                catch (IOException ioe)
                {
                    xcExceptionListener.handleRXThrowable(ioe, xc);
                }
                catch (ClassNotFoundException cnfe)
                {
                    xcExceptionListener.handleRXThrowable(cnfe, xc);
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
                        objectOutputStream.writeObject(nextMessage);
                        objectOutputStream.flush();
                        objectOutputStream = new ObjectOutputStream(outStream);
                    }
                    catch (NotSerializableException nse)
                    {
                        xcExceptionListener.handleTXThrowable(nse, xc);
                    }
                    catch (IOException ioe)
                    {
                        xcExceptionListener.handleTXThrowable(ioe, xc);
                    }
                    catch (ConcurrentModificationException ccme)
                    {
                        xcExceptionListener.handleTXThrowable(ccme, xc);
                    }
                    catch (Exception e)
                    {
                        xcExceptionListener.handleTXThrowable(e, xc);
                    }
                }
            }
        }
    }
    
    public static final long DEFAULT_WAIT = 10000;
    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;

    private final ArrayBlockingQueue<ClusterMessage> messageQ;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private final Thread txThread, rxThread;
    private final AtomicBoolean active;
    private final long waitTime;
    private final TimeUnit tUnit;
    private final TransceiverListener xcListener;
    private final TransceiverExceptionListener xcExceptionListener;
    private final OutputStream outStream;
    private final InputStream inStream;
    private long id;
    
    private final MessageXC xc = this;

    public MessageXC(final InputStream inStream,
                     final OutputStream outStream,
                     final TransceiverListener listener,
                     final TransceiverExceptionListener listenerE) throws IOException
    {
        this(inStream, outStream, listener, listenerE, DEFAULT_WAIT, DEFAULT_UNIT);
    }

    public MessageXC(InputStream inStream,
                     OutputStream outStream,
                     final TransceiverListener listener,
                     final TransceiverExceptionListener listenerE,
                     final long wait,
                     TimeUnit unit) throws IOException
    {
        messageQ = new ArrayBlockingQueue<ClusterMessage>(16, true);
        objectOutputStream = new ObjectOutputStream(outStream);
        objectInputStream =  new ObjectInputStream(inStream);
        this.inStream = inStream;
        this.outStream = outStream;
        active = new AtomicBoolean(true);
        waitTime = wait;
        tUnit = unit;
        xcListener = listener;
        xcExceptionListener = listenerE;

        txThread = new TXThread();
        rxThread = new RXThread();

        id = -1;

        rxThread.start();
        txThread.start();
    }

    public void close()
    {
        if (active.get())
        {
            FijiArchipelago.debug("XC: Got close.");
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
            messageQ.put(message);
            return true;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }

    public boolean queueMessage(final MessageType type)
    {
        ClusterMessage cm = new ClusterMessage(type);
        return queueMessage(cm);
    }

    public boolean queueMessage(final MessageType type, final Serializable o)
    {
        ClusterMessage cm = new ClusterMessage(type);
        cm.o = o;
        return queueMessage(cm);
    }

    public long getId()
    {
        return id;
    }

    public void setId(final long id)
    {
        this.id = id;
    }
}
