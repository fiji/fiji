package edu.utexas.clm.archipelago.network.shell;

import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.listen.ShellExecListener;

public interface NodeShell
{
    public boolean exec(NodeManager.NodeParameters param, final String command,
                        final ShellExecListener listener)
            throws ShellExecutionException;
}
