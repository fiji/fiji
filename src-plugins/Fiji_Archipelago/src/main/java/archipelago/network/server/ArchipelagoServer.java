package archipelago.network.server;

import archipelago.network.Cluster;
import archipelago.network.client.ArchipelagoClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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
                    cluster.assignSocketToNode(clientSocket);
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
            return false;
        }
        
        
        return true;
    }
    
    public void stop()
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


}
