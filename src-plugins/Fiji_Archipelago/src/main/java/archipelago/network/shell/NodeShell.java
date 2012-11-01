package archipelago.network.shell;

import archipelago.ShellExecListener;
import archipelago.network.ClusterNode;

public interface NodeShell
{
    public boolean exec(final ClusterNode node, final String command, final ShellExecListener listener);
}
