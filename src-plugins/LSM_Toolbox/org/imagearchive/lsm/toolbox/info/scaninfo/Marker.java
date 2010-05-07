package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Marker{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

	public Object[][] data = {
			{new Long(0x014000001),DataType.STRING,"MARKER_NAME"},
			{new Long(0x014000002),DataType.STRING,"DESCRIPTION"},
			{new Long(0x014000003),DataType.STRING,"TRIGGER_IN"},
			{new Long(0x014000004),DataType.STRING,"TRIGGER_OUT"}
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
