package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class IlluminationChannel{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

	public Object[][] data = {
			{ new Long(0x090000001), DataType.STRING, "ILL_NAME", }, // ILLUMINATION CHANNEL
			{ new Long(0x090000002), DataType.DOUBLE, "POWER" },
			{ new Long(0x090000003), DataType.DOUBLE, "WAVELENGTH" },
			{ new Long(0x090000004), DataType.LONG, "ACQUIRE" },
			{ new Long(0x090000005), DataType.STRING, "DETCHANNEL_NAME" },
			{ new Long(0x090000006), DataType.DOUBLE, "POWER_BC1" },
			{ new Long(0x090000007), DataType.DOUBLE, "POWER_BC2" } };

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
