package archipelago;

public interface ShellExecListener
{

    public void execFinished(final long nodeID, final Exception e);
}
