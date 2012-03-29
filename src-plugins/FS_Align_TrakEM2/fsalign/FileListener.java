package fsalign;

import java.util.EventListener;

public interface FileListener extends EventListener {

    public void handle(FolderWatcher fw);
}
