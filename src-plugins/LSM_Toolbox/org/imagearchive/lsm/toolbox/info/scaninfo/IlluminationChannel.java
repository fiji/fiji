package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class IlluminationChannel {

    public LinkedHashMap records = new LinkedHashMap();

	public Object[][] data = {
			{ new Long(0x090000001), "A", "ILL_NAME", }, // ILLUMINATION CHANNEL
			{ new Long(0x090000002), "R", "POWER" },
			{ new Long(0x090000003), "R", "WAVELENGTH" },
			{ new Long(0x090000004), "L", "ACQUIRE" },
			{ new Long(0x090000005), "A", "DETCHANNEL_NAME" },
			{ new Long(0x090000006), "R", "POWER_BC1" },
			{ new Long(0x090000007), "R", "POWER_BC2" } };

	public static boolean isIlluminationChannels(long tagEntry) {
		if (tagEntry == 0x080000000)
			return true;
		else
			return false;
	}

	public static boolean isIlluminationChannel(long tagEntry) {
		if (tagEntry == 0x090000000)
			return true;
		else
			return false;
	}
}
