package archipelago.network.factory;

import archipelago.network.ClusterNode;
import archipelago.network.NodeShell;

public interface NodeShellFactory
{
    public NodeShell getShell(ClusterNode node);

}
