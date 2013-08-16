package fsalign;

import java.util.EventListener;

/**
 * @author Larry Lindsey
 */
public interface FileListener extends EventListener {

    /**
     * Handle a FolderWatcher new-file event.
     * @param fw the FolderWatcher calling this method.
     */
    public void handle(FolderWatcher fw);

    /**
     * Cease functioning.
     */
    public void stop();
}
