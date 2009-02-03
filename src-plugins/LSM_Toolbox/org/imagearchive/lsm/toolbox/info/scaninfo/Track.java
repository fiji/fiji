package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Track {

    public LinkedHashMap records = new LinkedHashMap();

    public Object[][] data = {
            { new Long(0x040000001), "L", "MULTIPLEX_TYPE" },
            { new Long(0x040000002), "L", "MULTIPLEX_ORDER" },
            { new Long(0x040000003), "L", "SAMPLING_MODE" },
            { new Long(0x040000004), "L", "SAMPLING_METHOD" },
            { new Long(0x040000005), "L", "SAMPLING_NUMBER" },
            { new Long(0x040000006), "L", "ACQUIRE" },
            { new Long(0x040000007), "R", "OBSERVATION_TIME" },
            { new Long(0x04000000B), "R", "TIME_BETWEEN_STACKS" },
            { new Long(0x04000000C), "A", "TRACK_NAME" },
            { new Long(0x04000000D), "A", "COLLIMATOR1_NAME" },
            { new Long(0x04000000E), "L", "COLLIMATOR1_POSITION" },
            { new Long(0x04000000F), "A", "COLLIMATOR2_NAME" },
            { new Long(0x040000010), "A", "COLLIMATOR2_POSITION" },
            { new Long(0x040000011), "L", "BLEACH_TRACK" },
            { new Long(0x040000012), "L", "BLEACH_AFTER_SCAN NUMBER" },
            { new Long(0x040000013), "L", "BLEACH_SCAN_NUMBER" },
            { new Long(0x040000014), "A", "TRIGGER_IN" },
            { new Long(0x040000015), "A", "TRIGGER_OUT" },
            { new Long(0x040000016), "L", "IS_RATIO_TRACK" },
            { new Long(0x040000017), "L", "BLEACH_COUNT" },
            { new Long(0x040000018), "R", "SPI_CENTER_WAVELENGTH" },
            { new Long(0x040000019), "R", "PIXEL_TIME" },
            { new Long(0x040000020), "A", "ID_CONDENSOR_FRONTLENS"},
            { new Long(0x040000021), "L", "CONDENSOR_FRONTLENS" },
            { new Long(0x040000022), "A", "ID_FIELD_STOP" },
            { new Long(0x040000023), "R", "FIELD_STOP_VALUE" },
            { new Long(0x040000024), "A", "ID_CONDENSOR_APERTURE" },
            { new Long(0x040000025), "R", "CONDENSOR_APERTURE" },
            { new Long(0x040000026), "A", "ID_CONDENSOR_REVOLVER" },
            { new Long(0x040000027), "A", "CONDENSOR_FILTER" },
            { new Long(0x040000028), "R", "ID_TRANSMISSION_FILTER1" },
            { new Long(0x040000029), "A", "ID_TRANSMISSION1" },
            { new Long(0x040000030), "R", "ID_TRANSMISSION_FILTER2" },
            { new Long(0x040000031), "A", "ID_TRANSMISSION2" },
            { new Long(0x040000032), "L", "REPEAT_BLEACH" },
            { new Long(0x040000033), "L", "ENABLE_SPOT_BLEACH_POS" },
            { new Long(0x040000034), "R", "SPOT_BLEACH_POSX" },
            { new Long(0x040000035), "R", "SPOT_BLEACH_POSY" },
            { new Long(0x040000036), "R", "BLEACH_POSITION_Z" },
            { new Long(0x040000037), "A", "ID_TUBELENS" },
            { new Long(0x040000038), "A", "ID_TUBELENS_POSITION" },
            { new Long(0x040000039), "R", "TRANSMITTED_LIGHT" },
            { new Long(0x04000003a), "R", "REFLECTED_LIGHT" },
            { new Long(0x04000003b), "L", "TRACK_SIMULTAN_GRAB_AND_BLEACH" },
            { new Long(0x04000003c), "R", "BLEACH_PIXEL_TIME" }
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
