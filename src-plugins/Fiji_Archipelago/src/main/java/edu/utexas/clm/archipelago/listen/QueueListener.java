package edu.utexas.clm.archipelago.listen;

import edu.utexas.clm.archipelago.network.node.ClusterNode;

/**
 *
 * @author Larry Lindsey
 */
public interface QueueListener
{
    
    public void handleQueue(final ClusterNode node);
    
}
