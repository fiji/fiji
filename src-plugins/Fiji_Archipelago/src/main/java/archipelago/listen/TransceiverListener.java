package archipelago.listen;

import archipelago.data.ClusterMessage;

/**
 *
 * @author Larry Lindsey
 */
public interface TransceiverListener
{
    public void streamClosed();
    
    public void handleMessage(final ClusterMessage cm);
}
