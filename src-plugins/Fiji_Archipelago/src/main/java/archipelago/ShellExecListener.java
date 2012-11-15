package archipelago;
/**
 *
 * @author Larry Lindsey
 */
public interface ShellExecListener
{

    public void execFinished(final long nodeID, final Exception e);
}
