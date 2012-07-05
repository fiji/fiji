package org.imagearchive.lsm.toolbox.info.scaninfo;

import java.util.LinkedHashMap;

public class Laser{

    public LinkedHashMap<String, Object> records = new LinkedHashMap<String, Object>();

    public Object[][] data = {
            { new Long(0x050000001), DataType.STRING, "LASER_NAME" },
            { new Long(0x050000002), DataType.LONG, "LASER_ACQUIRE" },
            { new Long(0x050000003), DataType.DOUBLE, "LASER_POWER" } }; //in the description it's a Long

    public static boolean isLasers(long tagEntry) {//805306368
        if (tagEntry == 0x030000000)
            return true;
        else
            return false;
    }

    public static boolean isLaser(long tagEntry) {
        if (tagEntry == 0x050000000)
            return true;
        else
            return false;
    }

}
