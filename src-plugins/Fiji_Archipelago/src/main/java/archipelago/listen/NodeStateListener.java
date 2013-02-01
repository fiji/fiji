package archipelago.listen;

import archipelago.network.node.ClusterNode;
import archipelago.network.node.ClusterNodeState;

public interface NodeStateListener
{
    
    public void stateChanged(ClusterNode node, ClusterNodeState stateNow, ClusterNodeState lastState);
}
