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

package edu.utexas.clm.archipelago.network.node;

import edu.utexas.clm.archipelago.*;
import edu.utexas.clm.archipelago.data.HeartBeat;
import edu.utexas.clm.archipelago.listen.*;
import edu.utexas.clm.archipelago.compute.ProcessManager;
import edu.utexas.clm.archipelago.data.ClusterMessage;
import edu.utexas.clm.archipelago.network.MessageXC;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Larry Lindsey
 */
public class ClusterNode implements TransceiverListener
{
    private MessageXC xc;

    private final Hashtable<Long, ProcessListener> processHandlers;
    private final Hashtable<Long, ProcessManager> runningProcesses;
    private final AtomicInteger ramMBAvail, ramMBTot, ramMBMax, runningCores;
    private long nodeID;
    private long lastBeatTime;
    private NodeManager.NodeParameters nodeParam;
    private final AtomicBoolean idSet, cpuSet;
    private ClusterNodeState state;
    private final Vector<NodeStateListener> stateListeners;
    private final TransceiverExceptionListener xcEListener;

   
    public ClusterNode(final TransceiverExceptionListener tel)
    {
        xc = null;
        lastBeatTime = 0;
        state = ClusterNodeState.INACTIVE;

//        ready = new AtomicBoolean(false);
        idSet = new AtomicBoolean(false);
        cpuSet = new AtomicBoolean(false);

        ramMBAvail = new AtomicInteger(0);
        ramMBTot = new AtomicInteger(0);
        ramMBMax = new AtomicInteger(0);
        runningCores = new AtomicInteger(0);
        processHandlers = new Hashtable<Long, ProcessListener>();
        runningProcesses = new Hashtable<Long, ProcessManager>();
        nodeID = -1;
        nodeParam = null;
        stateListeners = new Vector<NodeStateListener>();
        xcEListener = tel;
    }
    
    private void doSyncEnvironment()
    {
        if (getUser() == null || getUser().equals(""))
        {
            xc.queueMessage(MessageType.USER);
        }

        if (getExecPath() == null || getExecPath().equals(""))
        {
            xc.queueMessage(MessageType.GETEXECROOT);
        }
        else
        {
            xc.queueMessage(MessageType.SETEXECROOT, getExecPath());
        }

        if (getFilePath() == null || getFilePath().equals(""))
        {
            xc.queueMessage(MessageType.GETEXECROOT);
        }
        else
        {
            xc.queueMessage(MessageType.SETFILEROOT, getFilePath());
        }
        xc.queueMessage(MessageType.BEAT);
    }
    
    public boolean setExecPath(String path)
    {
        if (nodeParam == null)
        {
            return false;
        }
        else
        {
            nodeParam.setExecRoot(path);
            return true;
        }
    }
    
    public boolean setFilePath(String path)
    {
        if (nodeParam == null)
        {
            return false;
        }
        else
        {
            nodeParam.setFileRoot(path);
            return xc.queueMessage(MessageType.SETFILEROOT, path);
        }
    }

    public void streamClosed()
    {
        FijiArchipelago.log("Stream closed on " + getHost());
        if (state != ClusterNodeState.STOPPED)
        {
            close();
        }
    }

    /**
     * Sets the InputStream and OutputStream used to communicate with the remote compute node.
     * is and os are used to create a MessageXC. The remote node is then queried for its unique id
     * as well as the number of available threads if that was not set explicitly. Once we've
     * received that information, messages are passed to synchronize nonessential parameters and
     * start BEAT messages, then the Cluster's state is set to ready.
     * @param is InputStream to receive data from the remote machine
     * @param os OutputStream to send data to the remote machine
     * @throws IOException if a problem arises opening one of the streams
     * @throws TimeoutException if we time out while waiting for the remote node
     * @throws InterruptedException if we are interrupted while waiting for the remote node
     */
    public synchronized void setIOStreams(final InputStream is, final OutputStream os)
            throws IOException, TimeoutException, InterruptedException
    {
        int waitCnt = 0;

        FijiArchipelago.debug("Setting IO Streams for a new Cluster Node");
        
        xc = new MessageXC(is, os, this, xcEListener);
        xc.queueMessage(MessageType.GETID);
        
        idSet.set(false);
        cpuSet.set(false);

        FijiArchipelago.debug("Waiting for id...");
        while (!idSet.get() && !cpuSet.get())
        {
            String message = "Waiting for ";
            if (!idSet.get())
            {
                message += "id ";
            }
            if (!cpuSet.get())
            {
                message += "ncpu";
            }
            
            FijiArchipelago.debug(message);
            
            Thread.sleep(1000);
            if (waitCnt++ > 15)
            {
                FijiArchipelago.debug("Waited too long, giving up.");
                throw new TimeoutException("Timed out waiting for remote node");
            }
        }

        FijiArchipelago.debug("Cluster Node " + getHost() + " ready to go");

        doSyncEnvironment();
        setState(ClusterNodeState.ACTIVE);
    }

    public String getHost()
    {
        return nodeParam == null ? null : nodeParam.getHost();
    }
    
    public String getUser()
    {
        return nodeParam.getUser();
    }
    
    public String getExecPath()
    {
        return nodeParam.getExecRoot();
    }
    
    public String getFilePath()
    {
        return nodeParam.getFileRoot();
    }

    public boolean isReady()
    {
        return state == ClusterNodeState.ACTIVE;
    }
    
    public long getID()
    {
        return nodeID;
    }
    
/*
    public NodeShell getShell()
    {
        return nodeParam.getShell();
    }
*/

    public NodeManager.NodeParameters getParam()
    {
        return nodeParam;
    }

    /*public void setShell(final NodeShell shell)
    {
        nodeParam.setShell(shell);
    }*/
    
    public int numRunningThreads()
    {
        return runningCores.get();
    }

    public int numAvailableThreads()
    {
        int n = nodeParam.getThreadLimit() - runningCores.get();
        return n > 0 ? n : 0;
    }
    
    public int getThreadLimit()
    {
        return nodeParam.getThreadLimit();
    }
    
    public void setActive(boolean active)
    {        
        setState(active ? ClusterNodeState.ACTIVE : ClusterNodeState.INACTIVE);
    }

    public boolean submit(final ProcessManager<?> process, final ProcessListener listener)
    {
        if (isReady())
        {
            if (processHandlers.get(process.getID()) == null)
            {
                //FijiArchipelago.debug(getHost() + " scheduling process");
                processHandlers.put(process.getID(), listener);
                runningProcesses.put(process.getID(), process);
                process.setRunningOn(this);
                runningCores.addAndGet(process.requestedCores(this));
                return xc.queueMessage(MessageType.PROCESS, process);
            }
            else
            {
                FijiArchipelago.debug("There is already a process " + process.getID() + " on "
                        + getHost());
                return false;
            }
        }
        else
        {
            return false;
        }
    }

/*
    public void ping()
    {
        xc.queueMessage(MessageType.PING);
    }
*/

    public long lastBeat()
    {
        return lastBeatTime;
    }

    public void handleMessage(final ClusterMessage cm)
    {
        MessageType type = cm.type;
        Object object = cm.o;

        try
        {
            switch (type)
            {
                case GETID:
                    Long id = (Long)object;
                    nodeParam = Cluster.getCluster().getNodeManager().getParam(id);
                    FijiArchipelago.debug("Got id message. Setting ID to " + id + ". Param: " + nodeParam);
                    nodeID = id;
                    idSet.set(true);
                    xc.setId(nodeID);
                    nodeParam.setNode(this);

                    if (nodeParam.getThreadLimit() <= 0)
                    {
                        FijiArchipelago.debug("Queueing ncpu request...");
                        xc.queueMessage(MessageType.NUMTHREADS);
                        FijiArchipelago.debug("Done.");
                    }
                    else
                    {
                        cpuSet.set(true);
                    }

                    break;

                case PROCESS:
                    ProcessManager<?> pm = (ProcessManager<?>)object;
                    ProcessListener listener = processHandlers.remove(pm.getID());
                    removeProcess(pm);
                    //runningProcesses.remove(pm.getID());

                    listener.processFinished(pm);
                    break;

                case NUMTHREADS:
                    int n = (Integer)object;
                    nodeParam.setThreadLimit(n);
                    cpuSet.set(true);
                    break;
                
                case PING:                
                    FijiArchipelago.log("Received ping from " + getHost());
                    break;
            
                case USER:
                    if (nodeParam == null)
                    {
                        FijiArchipelago.err("Tried to set user but params are null");
                    }
                    if (object != null)
                    {
                        String username = (String)object;
                        nodeParam.setUser(username);
                    }
                    else
                    {
                        FijiArchipelago.err("Got username message with null user");
                    }
                    break;

                case GETFILEROOT:
                    // Results of a GETFILEROOT request sent to the client.
                    setFilePath((String)object);
                    break;
                
                case GETEXECROOT:
                    // Results of a GETEXECROOT request sent to the client.
                    setExecPath((String)object);
                    break;

                case ERROR:
                    Exception e = (Exception)object;
                    xcEListener.handleRXThrowable(e, xc);
                    break;
                
                case BEAT:
                    HeartBeat beat = (HeartBeat)object;
                    lastBeatTime = System.currentTimeMillis();
                    ramMBAvail.set(beat.ramMBAvailable);
                    ramMBTot.set(beat.ramMBTotal);
                    ramMBMax.set(beat.ramMBMax);
                    break;
                
                default:
                    FijiArchipelago.log("Got unexpected message type. The local version " +
                            "of Archipelago may not be up to date with the clients.");
            
            }
            
        }
        catch (ClassCastException cce)
        {
            FijiArchipelago.err("Caught ClassCastException while handling message " 
                    + ClusterMessage.typeToString(type) + " on " + getHost() + " : "+ cce);
        }
        catch (NullPointerException npe)
        {
            FijiArchipelago.err("Expected a message object but got null for " +
                    ClusterMessage.typeToString(type) + " on "+ getHost());
        }
    }

    public int getMaxRamMB()
    {
        return ramMBMax.get();
    }
    
    public int getAvailableRamMB()
    {
        return ramMBAvail.get();
    }
    
    public int getTotalRamMB()
    {
        return ramMBTot.get();
    }
    
    public synchronized void close()
    {        
        if (state != ClusterNodeState.STOPPED)
        {
            FijiArchipelago.debug("Setting state");

            setState(ClusterNodeState.STOPPED);

            FijiArchipelago.debug("Sending shutdown");

            sendShutdown();
            
            for (ProcessManager pm : new ArrayList<ProcessManager>(runningProcesses.values()))
            {
                removeProcess(pm);
            }

            FijiArchipelago.debug("Closing XC");

            xc.close();

            FijiArchipelago.debug("Node: Close finished");
        }
        else
        {
            FijiArchipelago.debug("Node: Close() called, but I'm already stopped");
        }
    }

    private boolean sendShutdown()
    {
        return xc.queueMessage(MessageType.HALT);
    }
    
    public static String stateString(final ClusterNodeState state)
    {
        switch(state)
        {
            case ACTIVE:
                return "active";
            case INACTIVE:
                return "inactive";
            case STOPPED:
                return "stopped";
            default:
                return "unknown";
        }
    }
    
    protected synchronized void setState(final ClusterNodeState nextState)
    {
        if (state != nextState)
        {
            // Order is very important
            ClusterNodeState lastState = state;
            state = nextState;
            FijiArchipelago.log("Node state changed from "
                    + stateString(lastState) + " to " + stateString(nextState));
            for (NodeStateListener listener : stateListeners)
            {
                listener.stateChanged(this, state, lastState);
            }
        }
    }
    
    public boolean cancelJob(long id)
    {
        ProcessManager<?> pm = runningProcesses.get(id);
        if (pm == null)
        {
            return false;
        }
        else if (xc.queueMessage(MessageType.CANCELJOB, id))
        {
            removeProcess(pm);
            return true;
        }
        return false;
    }
    
    private void removeProcess(ProcessManager pm)
    {
        runningProcesses.remove(pm.getID());
        processHandlers.remove(pm.getID());
        runningCores.addAndGet(-(pm.requestedCores(this)));
    }

    public List<ProcessManager> getRunningProcesses()
    {
        return new ArrayList<ProcessManager> (runningProcesses.values());
    }
    
    /**
     * Adds a NodeStateListener to the list of listeners that are notified when the state of this
     * ClusterNode changes. Immediately upon addition, the listener is called with the current
     * state, and given that the last state was ClusterNodeState.INACTIVE to indicate that this is
     * the initial call.
     * @param listener a NodeStateListener to register with the ClusterNode
     */
    public void addListener(final NodeStateListener listener)
    {
        stateListeners.add(listener);
        listener.stateChanged(this, state, ClusterNodeState.INACTIVE);
    }
    
    public void removeListener(final NodeStateListener listener)
    {
        stateListeners.remove(listener);
    }
    
    public ClusterNodeState getState()
    {
        return state;
    }
    
    public String toString()
    {
        return getHost();
    }
}
