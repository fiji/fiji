package archipelago.network;

/**
 *
 * @author Larry Lindsey
 */
public interface QueueListener
{
    
    public void handleQueue(final ClusterNode node);
    
}
