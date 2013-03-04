package edu.utexas.clm.archipelago.listen;

import edu.utexas.clm.archipelago.network.node.ClusterNode;
import edu.utexas.clm.archipelago.network.node.ClusterNodeState;

public interface NodeStateListener
{
    public void stateChanged(ClusterNode node, ClusterNodeState stateNow, ClusterNodeState lastState);
}
