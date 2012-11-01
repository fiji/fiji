package archipelago;

import archipelago.network.ClusterNode;

public interface ShellExecListener
{

    public void execFinished(final ClusterNode node, final Exception e);
}
