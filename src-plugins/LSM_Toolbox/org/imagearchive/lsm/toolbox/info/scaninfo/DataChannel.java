package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class DataChannel {

    public LinkedHashMap records = new LinkedHashMap();
	
	public Object[][] data = {
			{new Long(0x0D0000001),"A","DATA_NAME"}, 
            {new Long(0x0D0000004),"L","COLOR"},
            {new Long(0x0D0000005),"L","SAMPLETYPE"},
            {new Long(0x0D0000006),"L","BITS_PER_SAMPLE"},
            {new Long(0x0D0000007),"L","RATIO_TYPE"},
            {new Long(0x0D0000008),"L","RATIO_TRACK1"},
            {new Long(0x0D0000009),"L","RATIO_TRACK2"},
            {new Long(0x0D000000A),"A","RATIO_CHANNEL1"},
            {new Long(0x0D000000B),"A","RATIO_CHANNEL2"},
            {new Long(0x0D000000C),"R","RATIO_CONST1"},
            {new Long(0x0D000000D),"R","RATIO_CONST2"},
            {new Long(0x0D000000E),"R","RATIO_CONST3"},
            {new Long(0x0D000000F),"R","RATIO_CONST4"},
            {new Long(0x0D0000010),"R","RATIO_CONST5"},
            {new Long(0x0D0000011),"R","RATIO_CONST6"},
            {new Long(0x0D0000012),"L","RATIO_FIRST_IMAGES1"},
            {new Long(0x0D0000013),"L","RATIO_FIRST_IMAGES2"},
            {new Long(0x0D0000014),"A","DYE_NAME"},
            {new Long(0x0D0000015),"A","DYE_FOLDER"},
            {new Long(0x0D0000016),"A","SPECTRUM"},
            {new Long(0x0D0000017),"L","ACQUIRE"}            
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
