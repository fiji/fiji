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
import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.archipelago.data.HeartBeat;
import edu.utexas.clm.archipelago.listen.MessageType;
import edu.utexas.clm.archipelago.listen.TransceiverExceptionListener;
import edu.utexas.clm.archipelago.listen.TransceiverListener;
import edu.utexas.clm.archipelago.compute.ProcessManager;
import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottler;
import edu.utexas.clm.archipelago.network.translation.PMAcknowledgingSender;
import edu.utexas.clm.archipelago.network.translation.PathSubstitutingFileTranslator;
import edu.utexas.clm.archipelago.util.XCErrorAdapter;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 *
 * @author Larry Lindsey
 */
public class ArchipelagoClient implements TransceiverListener
{
    private static ArrayList<ArchipelagoClient> clients = new ArrayList<ArchipelagoClient>();


    public static ArrayList<ArchipelagoClient> getClients()
    {
        return new ArrayList<ArchipelagoClient>(clients);
    }

    public static ArchipelagoClient getFirstClient()
    {
        if (clients.isEmpty())
        {
            return null;
        }
        else
        {
            return clients.get(0);
        }
    }
    
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
                PMAcknowledgingSender ack = new PMAcknowledgingSender(xc, process);
                processAcks.put(process.getID(), ack);
                ack.go();
                //xc.queueMessage(MessageType.PROCESS, process);
            }
        }
    }


    private final MessageXC xc;
    private long clientId = 0;
    private final AtomicBoolean active;
    private final Vector<ProcessThread> runningThreads;
    private final HeartBeatThread beatThread;
    private final TransceiverExceptionListener xcEListener;
    private final Hashtable<Long, PMAcknowledgingSender> processAcks;
    private final Set<Long> processIdMemory;

    public ArchipelagoClient(final long id, final InputStream inStream,
                             final OutputStream outStream) throws IOException
    {
        this(id, inStream, outStream, new XCErrorAdapter()
        {
            protected boolean handleCustomRX(final Throwable t, final MessageXC xc,
                                             final ClusterMessage cm)
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

            protected boolean handleCustomTX(final Throwable t, final MessageXC xc,
                                             final ClusterMessage cm)
            {
                xc.queueMessage(MessageType.ERROR, t);
                /*if (t instanceof IOException)
                {
                    reportTX(t, t.toString(), xc);
                    xc.close();
                }
                else
                {
                  xc.queueMessage(MessageType.ERROR, t);
                }*/
                return true;
            }
        });
    }

    public ArchipelagoClient(long id, InputStream inStream, OutputStream outStream,
                             TransceiverExceptionListener tel) throws IOException
    {
        FijiArchipelago.log("Starting Archipelago Client...");

        processAcks = new Hashtable<Long, PMAcknowledgingSender>();
        processIdMemory = Collections.synchronizedSet(new HashSet<Long>());

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

        if (clientHost.equals(""))
        {
            try
            {
                clientHost = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException uhe)
            {
                FijiArchipelago.log("Could not resolve local name: " + uhe);
                clientHost = "Unknown";
            }
        }

        clients.add(this);

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
                    boolean seen = processIdMemory.contains(pm.getID());
                    processIdMemory.add(pm.getID());

                    xc.queueMessage(MessageType.ACK, pm.getID());

                    if (!seen)
                    {
                        final ProcessThread pt = new ProcessThread(pm);
                        runningThreads.add(pt);
                        pt.start();
                    }

                    break;

                case ACK:
                    long pmid = (Long)object;
                    PMAcknowledgingSender ack = processAcks.remove(pmid);

                    // ack can be null in the situation in which a previous ACK message was sent by the remote just
                    // as we were re-sending a process message.

                    if (ack != null)
                    {
                        ack.acknowledge();
                    }

                    FijiArchipelago.debug("Processed ack for " + pmid);

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

/*
                case SETFILEROOT:
                    FijiArchipelago.setFileRoot((String) object);
                    break;
*/
                case BOTTLER:
                    xc.addBottler((Bottler)object);
                    break;

                case SETEXECROOT:
                    FijiArchipelago.setExecRoot((String) object);
                    break;


                case GETFSTRANSLATION:
                    xc.queueMessage(MessageType.GETFSTRANSLATION, FijiArchipelago.getFileRoot());
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
                    xc.queueMessage(MessageType.HOSTNAME,
                            clientHost);
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

                case SETFSTRANSLATION:
                    final Duplex<String, String> translation = (Duplex<String, String>)object;
                    xc.setFileSystemTranslator(
                            new PathSubstitutingFileTranslator(translation.a, translation.b));
                    break;
            }
        }
        catch (ClassCastException cce)
        {
            xcEListener.handleRXThrowable(cce, xc, cm);
        }
    }

    public void log(String string)
    {
        xc.queueMessage(MessageType.LOG, string);
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
            Thread.dumpStack();
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
    
    public static String clientHost = "";
}
