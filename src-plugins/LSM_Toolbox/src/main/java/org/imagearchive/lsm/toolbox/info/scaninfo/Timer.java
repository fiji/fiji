package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Timer {

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

	public Object[][] data = {
			{ new Long(0x012000001), DataType.STRING, "TIMER_NAME" },
			{ new Long(0x012000003), DataType.DOUBLE, "INTERVAL" },
			{ new Long(0x012000004), DataType.STRING, "TRIGGER_IN" },
			{ new Long(0x012000005), DataType.STRING, "TRIGGER_OUT" } };

	public static boolean isTimers(long tagEntry) {
		if (tagEntry == 0x011000000)
			return true;
		else
			return false;
	}

	public static boolean isTimer(long tagEntry) {
		if (tagEntry == 0x012000000)
			return true;
		else
			return false;
	}
}
