package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class DetectionChannel{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

	public Object[][] data = {
			{new Long(0x070000003),DataType.DOUBLE,"DETECTOR_GAIN"},       //DETECTION CHANNELS
            {new Long(0x070000005),DataType.DOUBLE,"AMPLIFIER_GAIN"},
            {new Long(0x070000007),DataType.DOUBLE,"AMPLIFIER_OFFSET"},
            {new Long(0x070000009),DataType.DOUBLE,"PINHOLE_DIAMETER"},
            {new Long(0x07000000B),DataType.LONG,"ACQUIRE"},
            {new Long(0x07000000C),DataType.STRING,"DETECTOR_NAME"},
            {new Long(0x07000000D),DataType.STRING,"AMPLIFIER_NAME"},
            {new Long(0x07000000E),DataType.STRING,"PINHOLE_NAME"},
            {new Long(0x07000000F),DataType.STRING,"FILTER_SET_NAME"},
            {new Long(0x070000010),DataType.STRING,"FILTER_NAME"},
            {new Long(0x070000013),DataType.STRING,"INTEGRATOR_NAME"},
            {new Long(0x070000014),DataType.STRING,"DETECTION_CHANNEL_NAME"},
            {new Long(0x070000015),DataType.DOUBLE,"DETECTOR_GAIN_BC1"},
            {new Long(0x070000016),DataType.DOUBLE,"DETECTOR_GAIN_BC2"},
            {new Long(0x070000017),DataType.DOUBLE,"AMPLIFIER_GAIN_BC1"},
            {new Long(0x070000018),DataType.DOUBLE,"AMPLIFIER_GAIN_BC2"},
            {new Long(0x070000019),DataType.DOUBLE,"AMPLIFIER_OFFSET_BC1"},
            {new Long(0x070000020),DataType.DOUBLE,"AMPLIFIER_OFFSET_BC2"},
            {new Long(0x070000021),DataType.LONG,"SPECTRAL_SCAN_CHANNELS"},
            {new Long(0x070000022),DataType.DOUBLE,"SPI_WAVE_LENGTH_START"},
            {new Long(0x070000023),DataType.DOUBLE,"SPI_WAVELENGTH_END"},
            {new Long(0x070000026),DataType.STRING,"DYE_NAME"},
            {new Long(0x070000027),DataType.STRING,"DYE_FOLDER"}
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
