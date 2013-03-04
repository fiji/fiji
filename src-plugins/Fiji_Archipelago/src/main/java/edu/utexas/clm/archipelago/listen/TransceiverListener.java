package edu.utexas.clm.archipelago.listen;

import edu.utexas.clm.archipelago.data.ClusterMessage;

/**
 *
 * @author Larry Lindsey
 */
public interface TransceiverListener
{
    public void streamClosed();
    
    public void handleMessage(final ClusterMessage cm);
}
