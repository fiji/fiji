package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Timer {

    public LinkedHashMap records = new LinkedHashMap();
    
	public Object[][] data = { 
			{ new Long(0x012000001), "A", "TIMER_NAME" },
			{ new Long(0x012000003), "R", "INTERVAL" },
			{ new Long(0x012000004), "A", "TRIGGER_IN" },
			{ new Long(0x012000005), "A", "TRIGGER_OUT" } };
	
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
