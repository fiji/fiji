package archipelago.network.shell;

import archipelago.NodeManager;
import archipelago.ShellExecListener;
import archipelago.network.ClusterNode;

public interface NodeShell
{
    public boolean exec(NodeManager.NodeParameters param, final String command, final ShellExecListener listener);
}
