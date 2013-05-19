package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class DataChannel{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

	public Object[][] data = {
			{new Long(0x0D0000001),DataType.STRING,"DATA_NAME"},
            {new Long(0x0D0000004),DataType.LONG,"COLOR"},
            {new Long(0x0D0000005),DataType.LONG,"SAMPLETYPE"},
            {new Long(0x0D0000006),DataType.LONG,"BITS_PER_SAMPLE"},
            {new Long(0x0D0000007),DataType.LONG,"RATIO_TYPE"},
            {new Long(0x0D0000008),DataType.LONG,"RATIO_TRACK1"},
            {new Long(0x0D0000009),DataType.LONG,"RATIO_TRACK2"},
            {new Long(0x0D000000A),DataType.STRING,"RATIO_CHANNEL1"},
            {new Long(0x0D000000B),DataType.STRING,"RATIO_CHANNEL2"},
            {new Long(0x0D000000C),DataType.DOUBLE,"RATIO_CONST1"},
            {new Long(0x0D000000D),DataType.DOUBLE,"RATIO_CONST2"},
            {new Long(0x0D000000E),DataType.DOUBLE,"RATIO_CONST3"},
            {new Long(0x0D000000F),DataType.DOUBLE,"RATIO_CONST4"},
            {new Long(0x0D0000010),DataType.DOUBLE,"RATIO_CONST5"},
            {new Long(0x0D0000011),DataType.DOUBLE,"RATIO_CONST6"},
            {new Long(0x0D0000012),DataType.LONG,"RATIO_FIRST_IMAGES1"},
            {new Long(0x0D0000013),DataType.LONG,"RATIO_FIRST_IMAGES2"},
            {new Long(0x0D0000014),DataType.STRING,"DYE_NAME"},
            {new Long(0x0D0000015),DataType.STRING,"DYE_FOLDER"},
            {new Long(0x0D0000016),DataType.STRING,"SPECTRUM"},
            {new Long(0x0D0000017),DataType.LONG,"ACQUIRE"}
	};

	public static boolean isDataChannels(long tagEntry) {
		if (tagEntry == 0x0C0000000)
			return true;
		else
			return false;
	}

	public static boolean isDataChannel(long tagEntry) {
		if (tagEntry == 0x0D0000000)
			return true;
		else
			return false;
	}
}
