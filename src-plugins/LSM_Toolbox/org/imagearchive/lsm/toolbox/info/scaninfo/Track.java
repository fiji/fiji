package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Track{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

    public Object[][] data = {
            { new Long(0x040000001), DataType.LONG, "MULTIPLEX_TYPE" },
            { new Long(0x040000002), DataType.LONG, "MULTIPLEX_ORDER" },
            { new Long(0x040000003), DataType.LONG, "SAMPLING_MODE" },
            { new Long(0x040000004), DataType.LONG, "SAMPLING_METHOD" },
            { new Long(0x040000005), DataType.LONG, "SAMPLING_NUMBER" },
            { new Long(0x040000006), DataType.LONG, "ACQUIRE" },
            { new Long(0x040000007), DataType.DOUBLE, "OBSERVATION_TIME" },
            { new Long(0x04000000B), DataType.DOUBLE, "TIME_BETWEEN_STACKS" },
            { new Long(0x04000000C), DataType.STRING, "TRACK_NAME" },
            { new Long(0x04000000D), DataType.STRING, "COLLIMATOR1_NAME" },
            { new Long(0x04000000E), DataType.LONG, "COLLIMATOR1_POSITION" },
            { new Long(0x04000000F), DataType.STRING, "COLLIMATOR2_NAME" },
            { new Long(0x040000010), DataType.STRING, "COLLIMATOR2_POSITION" },
            { new Long(0x040000011), DataType.LONG, "BLEACH_TRACK" },
            { new Long(0x040000012), DataType.LONG, "BLEACH_AFTER_SCAN_NUMBER" },
            { new Long(0x040000013), DataType.LONG, "BLEACH_SCAN_NUMBER" },
            { new Long(0x040000014), DataType.STRING, "TRIGGER_IN" },
            { new Long(0x040000015), DataType.STRING, "TRIGGER_OUT" },
            { new Long(0x040000016), DataType.LONG, "IS_RATIO_TRACK" },
            { new Long(0x040000017), DataType.LONG, "BLEACH_COUNT" },
            { new Long(0x040000018), DataType.DOUBLE, "SPI_CENTER_WAVELENGTH" },
            { new Long(0x040000019), DataType.DOUBLE, "PIXEL_TIME" },
            { new Long(0x040000020), DataType.STRING, "ID_CONDENSOR_FRONTLENS"},
            { new Long(0x040000021), DataType.LONG, "CONDENSOR_FRONTLENS" },
            { new Long(0x040000022), DataType.STRING, "ID_FIELD_STOP" },
            { new Long(0x040000023), DataType.DOUBLE, "FIELD_STOP_VALUE" },
            { new Long(0x040000024), DataType.STRING, "ID_CONDENSOR_APERTURE" },
            { new Long(0x040000025), DataType.DOUBLE, "CONDENSOR_APERTURE" },
            { new Long(0x040000026), DataType.STRING, "ID_CONDENSOR_REVOLVER" },
            { new Long(0x040000027), DataType.STRING, "CONDENSOR_FILTER" },
            { new Long(0x040000028), DataType.DOUBLE, "ID_TRANSMISSION_FILTER1" },
            { new Long(0x040000029), DataType.STRING, "ID_TRANSMISSION1" },
            { new Long(0x040000030), DataType.DOUBLE, "ID_TRANSMISSION_FILTER2" },
            { new Long(0x040000031), DataType.STRING, "ID_TRANSMISSION2" },
            { new Long(0x040000032), DataType.LONG, "REPEAT_BLEACH" },
            { new Long(0x040000033), DataType.LONG, "ENABLE_SPOT_BLEACH_POS" },
            { new Long(0x040000034), DataType.DOUBLE, "SPOT_BLEACH_POSX" },
            { new Long(0x040000035), DataType.DOUBLE, "SPOT_BLEACH_POSY" },
            { new Long(0x040000036), DataType.DOUBLE, "BLEACH_POSITION_Z" },
            { new Long(0x040000037), DataType.STRING, "ID_TUBELENS" },
            { new Long(0x040000038), DataType.STRING, "ID_TUBELENS_POSITION" },
            { new Long(0x040000039), DataType.DOUBLE, "TRANSMITTED_LIGHT" },
            { new Long(0x04000003a), DataType.DOUBLE, "REFLECTED_LIGHT" },
            { new Long(0x04000003b), DataType.LONG, "TRACK_SIMULTAN_GRAB_AND_BLEACH" },
            { new Long(0x04000003c), DataType.DOUBLE, "BLEACH_PIXEL_TIME" }
    };

    public static boolean isTracks(long tagEntry) {
        if (tagEntry == 0x020000000)
            return true;
        else
            return false;
    }

    public static boolean isTrack(long tagEntry) {
        if (tagEntry == 0x040000000)
            return true;
        else
            return false;
    }

    public BeamSplitter[] beamSplitters;

    public DataChannel[] dataChannels;

    public DetectionChannel[] detectionChannels;

    public IlluminationChannel[] illuminationChannels;
}
