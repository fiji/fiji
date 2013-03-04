package edu.utexas.clm.archipelago.listen;


import edu.utexas.clm.archipelago.data.ClusterMessage;
/**
 *
 * @author Larry Lindsey
 */
public interface MessageListener {
    
    public void handleMessage(final ClusterMessage message);
    
}
