package archipelago.network;

import archipelago.network.ClusterNode;

public interface QueueListener
{
    
    public void handleQueue(final ClusterNode node);
    
}
