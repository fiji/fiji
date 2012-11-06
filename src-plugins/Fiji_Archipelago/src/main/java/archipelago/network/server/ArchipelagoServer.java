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
                    System.out.println("Server waiting to accept a socket");
                    Socket clientSocket = socket.accept();
                    System.out.println("Got one! Assigning to node for " + socket.getInetAddress());
                    cluster.nodeFromSocket(clientSocket);
                }
                catch (IOException ioe)
                {
                    System.out.println("Got an error while waiting for a socket: " + ioe);
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
            System.err.println("Got IOExecption: " + ioe);
            return false;
        }
        
        
        return true;
    }
    
    public void close()
    {
        isRunning.set(false);
        try
        {
            System.out.println("interrupting thread");
            listenThread.interrupt();
            System.out.println("Closing server socket");
            socket.close();
            System.out.println("Closed socket");
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
