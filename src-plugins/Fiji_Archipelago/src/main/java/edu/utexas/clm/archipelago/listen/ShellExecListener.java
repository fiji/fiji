package edu.utexas.clm.archipelago.listen;
/**
 *
 * @author Larry Lindsey
 */
public interface ShellExecListener
{
    public void execFinished(final long nodeID, final Exception e, final int status);
}
