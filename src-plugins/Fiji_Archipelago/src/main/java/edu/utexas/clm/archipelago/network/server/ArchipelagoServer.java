package edu.utexas.clm.archipelago.network.server;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 *
 * @author Larry Lindsey
 */
public class ArchipelagoServer
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
                    FijiArchipelago.log("Received connection from " + socket.getInetAddress());
                    cluster.nodeFromSocket(clientSocket);
                }
                catch (IOException ioe)
                {
                    //Ignore
                }
            }
        }
    }
    
    private final Cluster cluster;    
    private ServerSocket socket;
    AtomicBoolean isRunning;
    Thread listenThread;
    
    public ArchipelagoServer(Cluster clusterIn)
    {
        cluster = clusterIn;
        socket = null;
        isRunning = new AtomicBoolean(false);
    }

    public boolean start()
    {
        if (isRunning.get())
        {
            return true;
        }
            
        try
        {
            socket = new ServerSocket(cluster.getServerPort());
            
            listenThread = new ListenThread(socket);
            
            listenThread.start();
            
            isRunning.set(true);
        }
        catch(IOException ioe)
        {
            FijiArchipelago.err("Archipelago Server: Got IOException: " + ioe);
            return false;
        }
        
        
        return true;
    }
    
    public void close()
    {
        isRunning.set(false);
        try
        {
            listenThread.interrupt();
            socket.close();
        }
        catch (IOException ioe)
        {
            //Nothing to do
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


}
