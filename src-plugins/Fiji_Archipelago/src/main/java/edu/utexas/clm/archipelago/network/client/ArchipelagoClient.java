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

package edu.utexas.clm.archipelago.network.client;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.data.HeartBeat;
import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.listen.TransceiverExceptionListener;
import edu.utexas.clm.archipelago.listen.TransceiverListener;
import edu.utexas.clm.archipelago.compute.ProcessManager;
import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.util.XCErrorAdapter;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 *
 * @author Larry Lindsey
 */
public class ArchipelagoClient implements TransceiverListener
{
    
    
    
    private class HeartBeatThread extends Thread
    {
        private final long interval;
        private final Runtime runtime;

        public HeartBeatThread(long interval, Runtime runtime)
        {
            this.interval = interval;
            this.runtime = runtime;
        }
        
        public void run()
        {
            while (active.get())
            {
                try
                {
                    Thread.sleep(interval);
                    HeartBeat beat = new HeartBeat(runtime.freeMemory(),
                            runtime.totalMemory(), runtime.maxMemory());

                    xc.queueMessage(MessageType.BEAT, beat);
                }
                catch (InterruptedException ie)
                {
                    //
                }
            }
        }
    }
    
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
            final long s = System.currentTimeMillis();
            FijiArchipelago.debug("Client: Running process " + process.getID());
            process.run();
            FijiArchipelago.debug("Client: Process " + process.getID() + " has finished. Took " +
                    (System.currentTimeMillis() - s) + "ms");
            runningThreads.remove(this);
            if (running.get() && active.get())
            {
                xc.queueMessage(MessageType.PROCESS, process);
            }
        }
    }


    private final MessageXC xc;
    private long clientId = 0;
    private final AtomicBoolean active;
    private final Vector<ProcessThread> runningThreads;
    private final HeartBeatThread beatThread;
    private final TransceiverExceptionListener xcEListener;

    public ArchipelagoClient(final long id, final InputStream inStream,
                             final OutputStream outStream) throws IOException
    {
        this(id, inStream, outStream, new XCErrorAdapter()
        {
            protected boolean handleCustomRX(final Throwable t, final MessageXC xc)
            {
                if (t instanceof ClassCastException)
                {
                    reportRX(t, t.toString(), xc);
                    xc.queueMessage(MessageType.ERROR, t);
                    return false;
                }
                else if (t instanceof EOFException)
                {
                    reportRX(t, "Received EOF", xc);
                    xc.close();
                    return false;
                }
                else if (t instanceof StreamCorruptedException)
                {
                    reportRX(t, "Stream corrupted: " + t, xc);
                    xc.close();
                    return false;
                }
                else
                {
                    xc.queueMessage(MessageType.ERROR, t);
                    return true;
                }
            }

            protected boolean handleCustomTX(final Throwable t, final MessageXC xc)
            {
                if (t instanceof IOException)
                {
                    reportTX(t, t.toString(), xc);
                    xc.close();
                }
                else
                {
                    xc.queueMessage(MessageType.ERROR, t);
                }
                return true;
            }
        });
    }

    public ArchipelagoClient(long id, InputStream inStream, OutputStream outStream,
                             TransceiverExceptionListener tel) throws IOException
    {
        FijiArchipelago.log("Starting Archipelago Client...");
        try
        {
            xcEListener = tel;

            clientId = id;
            xc = new MessageXC(inStream, outStream, this, xcEListener);
            xc.setId(id);
            beatThread = new HeartBeatThread(1000, Runtime.getRuntime());

            runningThreads = new Vector<ProcessThread>();
            
            active = new AtomicBoolean(true);
        }
        catch (IOException ioe)
        {
            FijiArchipelago.log("Caught an IOE: " + ioe);
            throw ioe;
        }
        FijiArchipelago.log("Archipelago Client is Active");
    }
    
    public void handleMessage(final ClusterMessage cm) {

        final MessageType type = cm.type;
        final Object object = cm.o;

        FijiArchipelago.log("Got message " + ClusterMessage.messageToString(cm));
        
        try
        {
            switch (type)
            {
                case PROCESS:
                    final ProcessManager<?> pm = (ProcessManager<?>)object;
                    final ProcessThread pt = new ProcessThread(pm);
                    runningThreads.add(pt);
                    pt.start();
                    break;

                case HALT:
                    close();
                    break;

                case PING:
                    xc.queueMessage(MessageType.PING);
                    break;

                case USER:
                    cm.o = System.getProperty("user.name");
                    xc.queueMessage(cm);
                    break;

                case SETID:
                    clientId = (Long)object;
                    break;

                case GETID:
                    xc.queueMessage(MessageType.GETID, clientId);
                    break;

                case SETFILEROOT:
                    FijiArchipelago.setFileRoot((String) object);
                    break;

                case SETEXECROOT:
                    FijiArchipelago.setExecRoot((String) object);
                    break;

                case GETFILEROOT:
                    xc.queueMessage(MessageType.SETFILEROOT, FijiArchipelago.getFileRoot());
                    break;

                case GETEXECROOT:
                    xc.queueMessage(MessageType.GETEXECROOT, FijiArchipelago.getExecRoot());
                    break;

                case CANCELJOB:
                    long id = (Long)object;

                    for (ProcessThread processThread : runningThreads)
                    {
                        if (processThread.getID() == id)
                        {
                            processThread.cancel();
                            runningThreads.remove(processThread);
                            return;
                        }
                    }
                    break;
                
                case HOSTNAME:
                    try
                    {
                        xc.queueMessage(MessageType.HOSTNAME,
                                InetAddress.getLocalHost().getHostName());
                    }
                    catch (UnknownHostException uhe)
                    {
                        FijiArchipelago.log("Could not resolve local name: " + uhe);
                    }
                    break;
                
                case NUMTHREADS:
                    xc.queueMessage(MessageType.NUMTHREADS,
                            Runtime.getRuntime().availableProcessors());
                    break;

                case BEAT:
                    if (!beatThread.isAlive())
                    {
                        beatThread.start();
                    }
                    break;
            }
        }
        catch (ClassCastException cce)
        {
            xcEListener.handleRXThrowable(cce, xc);
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
            FijiArchipelago.log("Closing Client");
            active.set(false);
            
            for (ProcessThread t : runningThreads)
            {
                t.cancel();
            }

            beatThread.interrupt();

            xc.close();
        }
        
    }

    public synchronized void streamClosed()
    {
        FijiArchipelago.log("Lost socket connection");
        close();
    }
}
