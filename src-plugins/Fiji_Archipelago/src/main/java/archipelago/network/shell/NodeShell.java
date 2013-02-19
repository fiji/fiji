package archipelago.network.shell;

import archipelago.network.node.NodeManager;
import archipelago.listen.ShellExecListener;

public interface NodeShell
{
    public boolean exec(NodeManager.NodeParameters param, final String command, final ShellExecListener listener);
}
