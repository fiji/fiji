package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class DetectionChannel {

    public LinkedHashMap records = new LinkedHashMap();
    
	public Object[][] data = {
			{new Long(0x070000003),"R","DETECTOR_GAIN"},       //DETECTION CHANNELS
            {new Long(0x070000005),"R","AMPLIFIER_GAIN"},
            {new Long(0x070000007),"R","AMPLIFIER_OFFSET"},
            {new Long(0x070000009),"R","PINHOLE_DIAMETER"},
            {new Long(0x07000000B),"L","ACQUIRE"},
            {new Long(0x07000000C),"A","DETECTOR_NAME"},
            {new Long(0x07000000D),"A","AMPLIFIER_NAME"},
            {new Long(0x07000000E),"A","PINHOLE_NAME"},
            {new Long(0x07000000F),"A","FILTER_SET_NAME"},
            {new Long(0x070000010),"A","FILTER_NAME"},
            {new Long(0x070000013),"A","INTEGRATOR_NAME"},
            {new Long(0x070000014),"A","DETECTION_CHANNEL_NAME"},
            {new Long(0x070000015),"R","DETECTOR_GAIN_BC1"},
            {new Long(0x070000016),"R","DETECTOR_GAIN_BC2"},
            {new Long(0x070000017),"R","AMPLIFIER_GAIN_BC1"},
            {new Long(0x070000018),"R","AMPLIFIER_GAIN_BC2"},
            {new Long(0x070000019),"R","AMPLIFIER_OFFSET_BC1"},
            {new Long(0x070000020),"R","AMPLIFIER_OFFSET_BC2"},
            {new Long(0x070000021),"L","SPECTRAL_SCAN_CHANNELS"},
            {new Long(0x070000022),"R","SPI_WAVE_LENGTH_START"},
            {new Long(0x070000023),"R","SPI_WAVELENGTH_END"},
            {new Long(0x070000026),"A","DYE_NAME"},
            {new Long(0x070000027),"A","DYE_FOLDER"}
	};
	
	public static boolean isDetectionChannels(long tagEntry) {
		if (tagEntry == 0x060000000)
			return true;
		else
			return false;
	}

	public static boolean isDetectionChannel(long tagEntry) {
		if (tagEntry == 0x070000000)
			return true;
		else
			return false;
	}

}
