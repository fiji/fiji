package archipelago.listen;


import archipelago.data.ClusterMessage;
/**
 *
 * @author Larry Lindsey
 */
public interface MessageListener {
    
    public void handleMessage(final ClusterMessage message);
    
}
