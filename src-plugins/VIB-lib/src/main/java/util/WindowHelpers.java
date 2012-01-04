/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import ij.WindowManager;
import ij.ImagePlus;

public class WindowHelpers {
	
	public static void closeAllWithoutConfirmation() {
		
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			return;
		}
		
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			imp.changes=false;
			imp.getWindow().close();
		}
		
	}
	
}