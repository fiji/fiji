package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Marker {

	public LinkedHashMap records = new LinkedHashMap();
    
	public Object[][] data = {
			{new Long(0x014000001),"A","MARKER_NAME"},
			{new Long(0x014000002),"A","DESCRIPTION"},
			{new Long(0x014000003),"A","TRIGGER_IN"},
			{new Long(0x014000004),"A","TRIGGER_OUT"}
	};
	public static boolean isMarkers(long tagEntry) {
		if (tagEntry == 0x013000000)
			return true;
		else
			return false;
	}

	public static boolean isMarker(long tagEntry) {
		if (tagEntry == 0x014000000)
			return true;
		else
			return false;
	}
}
