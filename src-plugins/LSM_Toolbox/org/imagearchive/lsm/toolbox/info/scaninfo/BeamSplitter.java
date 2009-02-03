package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class BeamSplitter {

    public LinkedHashMap records = new LinkedHashMap();

	public Object[][] data = {
            { new Long(0x0B0000001), "A", "FILTER_SET" },
			{ new Long(0x0B0000002), "A", "FILTER" },
            { new Long(0x0B0000003), "A", "BS_NAME" }
            };

	public static boolean isBeamSplitters(long tagEntry) {
		if (tagEntry == 0x0A0000000)
			return true;
		else
			return false;
	}

	public static boolean isBeamSplitter(long tagEntry) {
		if (tagEntry == 0x0B0000000)
			return true;
		else
			return false;
	}
}
