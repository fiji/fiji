/*
 * Created on 16-Sep-2005
 */
package org.imagearchive.lsm.toolbox;

import java.util.EventListener;



/**
 * @author patrick
 */
public interface MasterModelListener extends EventListener {
	public void LSMFileInfoChanged(MasterModelEvent evt);
}
