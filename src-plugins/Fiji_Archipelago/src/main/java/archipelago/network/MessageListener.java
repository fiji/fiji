package archipelago.network;


import archipelago.data.ClusterMessage;

public interface MessageListener {
    
    public void handleMessage(final ClusterMessage message);
    
}
