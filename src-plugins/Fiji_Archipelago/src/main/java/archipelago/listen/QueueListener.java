package archipelago.listen;

import archipelago.network.node.ClusterNode;

/**
 *
 * @author Larry Lindsey
 */
public interface QueueListener
{
    
    public void handleQueue(final ClusterNode node);
    
}
