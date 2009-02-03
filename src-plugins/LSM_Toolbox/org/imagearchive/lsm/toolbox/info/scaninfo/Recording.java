package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Recording {
  
    public LinkedHashMap records = new LinkedHashMap();

    public static Object[][] data = {
            { new Long(0x010000001), "A", "ENTRY_NAME" },
            { new Long(0x010000002), "A", "ENTRY_DESCRIPTION" },
            { new Long(0x010000003), "A", "ENTRY_NOTES" },
            { new Long(0x010000004), "A", "ENTRY_OBJECTIVE" },
            { new Long(0x010000005), "A", "PROCESSING_SUMMARY" },
            { new Long(0x010000006), "A", "SPECIAL_SCAN" },
            { new Long(0x010000007), "A", "SCAN_TYPE" },
            { new Long(0x010000008), "A", "SCAN_MODE" },
            { new Long(0x010000009), "L", "STACKS_COUNT" },
            { new Long(0x01000000A), "L", "LINES_PER_PLANE" },
            { new Long(0x01000000B), "L", "SAMPLES_PER_LINE" },
            { new Long(0x01000000C), "L", "PLANES_PER_VOLUME" },
            { new Long(0x01000000D), "L", "IMAGES_WIDTH" },
            { new Long(0x01000000E), "L", "IMAGES_HEIGHT" },
            { new Long(0x01000000F), "L", "NUMBER_OF_PLANES" },
            { new Long(0x010000010), "L", "IMAGES_NUMBER_STACKS" },
            { new Long(0x010000011), "L", "IMAGES_NUMBER_CHANNELS" },
            { new Long(0x010000012), "L", "LINESCAN_XY" },
            { new Long(0x010000013), "L", "SCAN_DIRECTION" },
            { new Long(0x010000014), "L", "TIME_SERIES" },
            { new Long(0x010000015), "L", "ORIGNAL_SCAN_DATA" },
            { new Long(0x010000016), "R", "ZOOM_X" },
            { new Long(0x010000017), "R", "ZOOM_Y" },
            { new Long(0x010000018), "R", "ZOOM_Z" },
            { new Long(0x010000019), "R", "SAMPLE_0X" },
            { new Long(0x01000001A), "R", "SAMPLE_0Y" },
            { new Long(0x01000001B), "R", "SAMPLE_0Z" },
            { new Long(0x01000001C), "R", "SAMPLE_SPACING" },
            { new Long(0x01000001D), "R", "LINE_SPACING" },
            { new Long(0x01000001E), "R", "PLANE SPACING" },
            { new Long(0x01000001F), "R", "PLANE_WIDTH" },
            { new Long(0x010000020), "R", "PLANE_HEIGHT" },
            { new Long(0x010000021), "R", "VOLUME_DEPTH" },
            { new Long(0x010000034), "R", "ROTATION" },
            { new Long(0x010000035), "R", "PRECESSION" },
            { new Long(0x010000036), "R", "SAMPLE_0TIME" },
            { new Long(0x010000037), "A", "START_SCAN_TRIGGER_IN" },
            { new Long(0x010000038), "A", "START_SCAN_TRIGGER_OUT" },
            { new Long(0x010000039), "L", "START_SCAN_EVENT" },
            { new Long(0x010000040), "R", "START_SCAN_TIME" },
            { new Long(0x010000041), "A", "STOP_SCAN_TRIGGER_IN" },
            { new Long(0x010000042), "A", "STOP_SCAN_TRIGGER_OUT" },
            { new Long(0x010000043), "L", "STOP_SCAN_EVENT" },
            { new Long(0x010000044), "R", "START_SCAN_TIME2" },
            { new Long(0x010000045), "L", "USE_ROIS" },
            { new Long(0x010000046), "R", "USE_REDUCED_MEMORY_ROIS" },
            { new Long(0x010000047), "A", "USER" },
            { new Long(0x010000048), "L", "USE_BCCORECCTION" },
            { new Long(0x010000049), "R", "POSITION_BCCORRECTION1" },
            { new Long(0x010000050), "R", "POSITION_BCCORRECTION2" },
            { new Long(0x010000051), "L", "INTERPOLATIONY" },
            { new Long(0x010000052), "L", "CAMERA_BINNING" },
            { new Long(0x010000053), "L", "CAMERA_SUPERSAMPLING" },
            { new Long(0x010000054), "L", "CAMERA_FRAME_WIDTH" },
            { new Long(0x010000055), "L", "CAMERA_FRAME_HEIGHT" },
    		{ new Long(0x010000056), "R", "CAMERA_OFFSETX" },
			{ new Long(0x010000057), "R", "CAMERA_OFFSETY" }};
    
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
