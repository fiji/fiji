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

package edu.utexas.clm.archipelago.network.server;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.ClusterStateListener;
import edu.utexas.clm.archipelago.listen.NodeShellListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Larry Lindsey
 */
public class ArchipelagoServer implements ClusterStateListener
{

    private class ListenThread extends Thread
    {
        private final ServerSocket socket;
        
        public ListenThread(ServerSocket ins)
        {
            socket = ins;
        }
        
        public void run()
        {
            while (isRunning.get())
            {
                try
                {
                    Socket clientSocket = socket.accept();
                    FijiArchipelago.log("ArchipelagoServer: Received connection from " + socket.getInetAddress());
                    lock.lock();
                    nodeListener.ioStreamsReady(clientSocket.getInputStream(),
                            clientSocket.getOutputStream());
                    lock.unlock();
                }
                catch (IOException ioe)
                {
                    FijiArchipelago.log("ArchipelagoServer: IOException: " + ioe);
                    //Ignore
                }
            }
        }
    }

    private static ArchipelagoServer server = null;

    private NodeShellListener nodeListener;    
    private ServerSocket socket;
    private int port;
    private AtomicBoolean isRunning;
    private Thread listenThread;
    private final ReentrantLock lock = new ReentrantLock();
    
    private ArchipelagoServer(NodeShellListener listener, int p)
    {
        nodeListener = listener;
        socket = null;
        isRunning = new AtomicBoolean(false);
        port = p;
    }
    
    public boolean active()
    {
        return isRunning.get();
    }

    public boolean start()
    {
        if (isRunning.get())
        {
            return true;
        }
        
        final int origPort = port;
        boolean success = false;
        IOException ioe = null;
        
        while (!success && port - origPort < 10)
        {
            try
            {
                socket = new ServerSocket(port);
                FijiArchipelago.debug("Started server on port " + port);
                success = true;
            }
            catch(IOException e)
            {
                port++;
                if (ioe == null)
                {
                    ioe = e;
                }
            }
        }

        if (!success)
        {
            port = -1;
            return false;
        }
        
        listenThread = new ListenThread(socket);

        listenThread.start();

        isRunning.set(true);
        
        return true;
    }
    
    public synchronized void close()
    {
        if (isRunning.get())
        {
            isRunning.set(false);
            listenThread.interrupt();

            try
            {
                socket.close();
            }
            catch (IOException ioe)
            {
                //Nothing to do
            }
        }
    }
    
    public boolean join()
    {
        try
        {
            listenThread.join();
            return true;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }

    public int getPort()
    {
        return port;
    }
    
    public void stateChanged(Cluster cluster)
    {
        Cluster.ClusterState state = cluster.getState();
        final ArchipelagoServer thisServer = this;
        
        if (state == Cluster.ClusterState.STOPPING || state == Cluster.ClusterState.STOPPED)
        {
            // Close in the background. Could be quick, might not be.
            new Thread()
            {
                public void run()
                {
                    server = null;
                    Cluster.getCluster().removeStateListener(thisServer);
                    close();
                }
            }.start();
        }
    }
    
    public void setListener(NodeShellListener listener)
    {
        lock.lock();
        nodeListener = listener;
        lock.unlock();
    }

    public synchronized static ArchipelagoServer getServer(NodeShellListener listener)
    {
        if (server == null)
        {
            server = new ArchipelagoServer(listener, 0xFAC);
            server.start();
            Cluster.getCluster().addStateListener(server);
        }
        else if (listener != server.nodeListener)
        {
            server.setListener(listener);
        }

        return server;
    }
}
