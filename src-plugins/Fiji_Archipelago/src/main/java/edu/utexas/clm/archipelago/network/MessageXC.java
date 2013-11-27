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
import edu.utexas.clm.archipelago.network.translation.Bottle;
import edu.utexas.clm.archipelago.network.translation.Bottler;
import edu.utexas.clm.archipelago.network.translation.FileTranslator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Message transceiver class
 */
public class MessageXC
{
    private class NullFileTranslator implements FileTranslator
    {

        public String getLocalPath(final String remotePath)
        {
            return remotePath;
        }

        public String getRemotePath(final String localPath)
        {
            return localPath;
        }
    }


    private class BottlingInputStream extends ObjectInputStream
    {
        public BottlingInputStream(final InputStream is) throws IOException
        {
            super(is);
            enableResolveObject(true);
        }

        protected final Object resolveObject(final Object object) throws IOException
        {
            if (object instanceof Bottle)
            {
                Bottle bottle = (Bottle)object;
                return bottle.unBottle(xc);
            }
            else
            {
                return object;
            }
        }
    }

    private class BottlingOutputStream extends ObjectOutputStream
    {

        public BottlingOutputStream(final OutputStream os) throws IOException
        {
            super(os);
            enableReplaceObject(true);
        }

        protected final Object replaceObject(final Object object) throws IOException
        {
            /*
            This seems like it could get costly as the number of bottles increases.
            If this is the case, explore lower cost options, like look up tables or hashmaps on
            the class name.
             */
            List<Bottler> bottlerList = new ArrayList<Bottler>(bottlers);
            for (final Bottler bottler : bottlerList)
            {
                if (bottler.accepts(object))
                {
                    return bottler.bottle(object, xc);
                }
            }
            return object;
        }
    }

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
                        FijiArchipelago.debug("RX: " + id + " got message " +
                                ClusterMessage.messageToString(message));
                        if (message.type == MessageType.PROCESS)
                        {
                            ProcessManager pm = (ProcessManager)message.o;
                            FijiArchipelago.debug("RX: Got message for job " + pm.getID());
                        }
                    }
                    xcListener.handleMessage(message);
                }
                /*catch (ClassCastException cce)
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
                }*/
                catch (Throwable e)
                {
                    xcExceptionListener.handleRXThrowable(e, xc, null);
                }
                finally
                {
                    try
                    {
                        objectInputStream = new BottlingInputStream(inStream);
                    }
                    catch (IOException ioe)
                    {
                        close();
                    }
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
/*                    if (nextMessage.type == MessageType.PROCESS)
                    {
                        lastSentID.set(((ProcessManager) nextMessage.o).getID());
                    }*/
                }
                catch (InterruptedException ie)
                {
                    active.set(false);
                }

                if (nextMessage != null)
                {
                    try
                    {
                        if (nextMessage.type != MessageType.BEAT)
                        {
                            FijiArchipelago.debug("TX: " + id + " writing message " +
                                    ClusterMessage.messageToString(nextMessage));
                        }
                        objectOutputStream.writeObject(nextMessage);
                        objectOutputStream.flush();

                    }
                    /*catch (NotSerializableException nse)
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
                    catch (RuntimeException re)
                    {
                        xcExceptionListener.handleTXThrowable(re, xc);
                    }*/
                    catch (Throwable e)
                    {
                        xcExceptionListener.handleTXThrowable(e, xc, nextMessage);
                    }
                    finally
                    {
                        try
                        {
                            objectOutputStream = new BottlingOutputStream(outStream);
                        }
                        catch (IOException ioe)
                        {
                            close();
                        }
                    }
                }
            }
        }
    }
    
    public static final long DEFAULT_WAIT = 10000;
    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;

    private final List<Bottler> bottlers;
    private final ArrayBlockingQueue<ClusterMessage> messageQ;
    private BottlingOutputStream objectOutputStream;
    private BottlingInputStream objectInputStream;
    private FileTranslator fileTranslator;
    private final Thread txThread, rxThread;
    private final AtomicBoolean active;
    private final AtomicLong lastSentID;
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
        FijiArchipelago.debug("Creating Message Transciever");
        fileTranslator = new NullFileTranslator();
        bottlers = Collections.synchronizedList(new Vector<Bottler>());
        messageQ = new ArrayBlockingQueue<ClusterMessage>(16, true);
        objectOutputStream = new BottlingOutputStream(outStream);
        objectInputStream =  new BottlingInputStream(inStream);
        FijiArchipelago.debug("XC: streams are set");
        this.inStream = inStream;
        this.outStream = outStream;
        active = new AtomicBoolean(true);
        lastSentID = new AtomicLong(-1);
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

    public long getLastProcessID()
    {
        return lastSentID.get();
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
            try
            {
                inStream.close();
            }
            catch (IOException ioe) {/**/}
            try
            {
                outStream.close();
            }
            catch (IOException ioe) {/**/}
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

    public String getLocalPath(final String remotePath)
    {
        return fileTranslator.getLocalPath(remotePath);
    }

    public String getRemotePath(final String localPath)
    {
        return fileTranslator.getRemotePath(localPath);
    }

    public File getLocalFile(final File remoteFile)
    {
        return new File(getLocalPath(remoteFile.getAbsolutePath()));
    }

    public File getRemoteFile(final File localFile)
    {
        return new File(fileTranslator.getRemotePath(localFile.getAbsolutePath()));
    }

    public synchronized void setFileSystemTranslator(final FileTranslator ft)
    {
        fileTranslator = ft;
    }

    public synchronized void unsetFileSystemTranslation()
    {
        if (!(fileTranslator instanceof NullFileTranslator))
        {
            fileTranslator = new NullFileTranslator();
        }
    }

    public void addBottler(final Bottler bottler)
    {
        bottlers.add(bottler);
    }

    public List<Bottler> getBottlers()
    {
        return bottlers;
    }
}
