package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Recording{

	public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

    public static Object[][] data = {
            { new Long(0x010000001), DataType.STRING, "ENTRY_NAME" },
            { new Long(0x010000002), DataType.STRING, "ENTRY_DESCRIPTION" },
            { new Long(0x010000003), DataType.STRING, "ENTRY_NOTES" },
            { new Long(0x010000004), DataType.STRING, "ENTRY_OBJECTIVE" },
            { new Long(0x010000005), DataType.STRING, "PROCESSING_SUMMARY" },
            { new Long(0x010000006), DataType.STRING, "SPECIAL_SCAN" },
            { new Long(0x010000007), DataType.STRING, "SCAN_TYPE" },
            { new Long(0x010000008), DataType.STRING, "SCAN_MODE" },
            { new Long(0x010000009), DataType.LONG, "STACKS_COUNT" },
            { new Long(0x01000000A), DataType.LONG, "LINES_PER_PLANE" },
            { new Long(0x01000000B), DataType.LONG, "SAMPLES_PER_LINE" },
            { new Long(0x01000000C), DataType.LONG, "PLANES_PER_VOLUME" },
            { new Long(0x01000000D), DataType.LONG, "IMAGES_WIDTH" },
            { new Long(0x01000000E), DataType.LONG, "IMAGES_HEIGHT" },
            { new Long(0x01000000F), DataType.LONG, "NUMBER_OF_PLANES" },
            { new Long(0x010000010), DataType.LONG, "IMAGES_NUMBER_STACKS" },
            { new Long(0x010000011), DataType.LONG, "IMAGES_NUMBER_CHANNELS" },
            { new Long(0x010000012), DataType.LONG, "LINESCAN_XY" },
            { new Long(0x010000013), DataType.LONG, "SCAN_DIRECTION" },
            { new Long(0x010000014), DataType.LONG, "TIME_SERIES" },
            { new Long(0x010000015), DataType.LONG, "ORIGNAL_SCAN_DATA" },
            { new Long(0x010000016), DataType.DOUBLE, "ZOOM_X" },
            { new Long(0x010000017), DataType.DOUBLE, "ZOOM_Y" },
            { new Long(0x010000018), DataType.DOUBLE, "ZOOM_Z" },
            { new Long(0x010000019), DataType.DOUBLE, "SAMPLE_0X" },
            { new Long(0x01000001A), DataType.DOUBLE, "SAMPLE_0Y" },
            { new Long(0x01000001B), DataType.DOUBLE, "SAMPLE_0Z" },
            { new Long(0x01000001C), DataType.DOUBLE, "SAMPLE_SPACING" },
            { new Long(0x01000001D), DataType.DOUBLE, "LINE_SPACING" },
            { new Long(0x01000001E), DataType.DOUBLE, "PLANE_SPACING" },
            { new Long(0x01000001F), DataType.DOUBLE, "PLANE_WIDTH" },
            { new Long(0x010000020), DataType.DOUBLE, "PLANE_HEIGHT" },
            { new Long(0x010000021), DataType.DOUBLE, "VOLUME_DEPTH" },
            { new Long(0x010000034), DataType.DOUBLE, "ROTATION" },
            { new Long(0x010000035), DataType.DOUBLE, "PRECESSION" },
            { new Long(0x010000036), DataType.DOUBLE, "SAMPLE_0TIME" },
            { new Long(0x010000037), DataType.STRING, "START_SCAN_TRIGGER_IN" },
            { new Long(0x010000038), DataType.STRING, "START_SCAN_TRIGGER_OUT" },
            { new Long(0x010000039), DataType.LONG, "START_SCAN_EVENT" },
            { new Long(0x010000040), DataType.DOUBLE, "START_SCAN_TIME" },
            { new Long(0x010000041), DataType.STRING, "STOP_SCAN_TRIGGER_IN" },
            { new Long(0x010000042), DataType.STRING, "STOP_SCAN_TRIGGER_OUT" },
            { new Long(0x010000043), DataType.LONG, "STOP_SCAN_EVENT" },
            { new Long(0x010000044), DataType.DOUBLE, "START_SCAN_TIME2" },
            { new Long(0x010000045), DataType.LONG, "USE_ROIS" },
            { new Long(0x010000046), DataType.LONG, "USE_REDUCED_MEMORY_ROIS" }, //in the description it's a Double
            { new Long(0x010000047), DataType.STRING, "USER" },
            { new Long(0x010000048), DataType.LONG, "USE_BCCORECCTION" },
            { new Long(0x010000049), DataType.DOUBLE, "POSITION_BCCORRECTION1" },
            { new Long(0x010000050), DataType.DOUBLE, "POSITION_BCCORRECTION2" },
            { new Long(0x010000051), DataType.LONG, "INTERPOLATIONY" },
            { new Long(0x010000052), DataType.LONG, "CAMERA_BINNING" },
            { new Long(0x010000053), DataType.LONG, "CAMERA_SUPERSAMPLING" },
            { new Long(0x010000054), DataType.LONG, "CAMERA_FRAME_WIDTH" },
            { new Long(0x010000055), DataType.LONG, "CAMERA_FRAME_HEIGHT" },
		{ new Long(0x010000056), DataType.DOUBLE, "CAMERA_OFFSETX" },
			{ new Long(0x010000057), DataType.DOUBLE, "CAMERA_OFFSETY" }};

    public static boolean isRecording(long tagEntry) {//268435456
        if (tagEntry == 0x010000000){
            return true;
        }
        else
            return false;
    }

    public Track[] tracks;

    public Marker[] markers;

    public Timer[] timers;

    public Laser[] lasers;
}
