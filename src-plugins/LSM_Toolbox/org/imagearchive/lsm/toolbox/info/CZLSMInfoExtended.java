package org.imagearchive.lsm.toolbox.info;

import org.imagearchive.lsm.reader.info.CZLSMInfo;
import org.imagearchive.lsm.toolbox.info.scaninfo.ScanInfo;

public class CZLSMInfoExtended extends CZLSMInfo{

	public long MagicNumber = 0;

	public long StructureSize = 0;

	public double OriginX = 0;

	public double OriginY = 0;

	public double OriginZ = 0;

	public int SpectralScan = 0;

	public long DataType = 0;

	public long OffsetVectorOverlay = 0;

	public long OffsetInputLut = 0;

	public long OffsetOutputLut = 0;

	public double TimeIntervall = 0;

	public long OffsetScanInformation = 0;

	public long OffsetKsData = 0;

	public long OffsetTimeStamps = 0;

	public long OffsetEventList = 0;

	public long OffsetRoi = 0;

	public long OffsetBleachRoi = 0;

	public long OffsetNextRecording = 0;

	public double DisplayAspectX = 0;

	public double DisplayAspectY = 0;

	public double DisplayAspectZ = 0;

	public double DisplayAspectTime = 0;

	public long OffsetMeanOfRoisOverlay = 0;

	public long OffsetTopoIsolineOverlay = 0;

	public long OffsetTopoProfileOverlay = 0;

	public long OffsetLinescanOverlay = 0;

	public long ToolbarFlags = 0;

	public long OffsetChannelWavelength = 0;

	public long OffsetChannelFactors = 0;

	public double ObjectiveSphereCorrection = 0;

	public long OffsetUnmixParameters = 0;

	public long[] Reserved;

	public TimeStamps timeStamps;

	public ChannelWavelengthRange channelWavelength; // lambda stamps

	public EventList eventList;

	public ScanInfo scanInfo;

	public String toString() {
		return new String("DimensionX:  " + DimensionX + "\n" +
				"DimensionY:  "+ DimensionY + "\n" +
				"DimensionZ:  " + DimensionZ + "\n"+
				"DimensionChannels:  " + DimensionChannels + "\n"+
				"ScanType:  " + getScanTypeText(ScanType)+"("+ScanType + ")\n" + "DataType:  " + DataType);
	}
	public String getScanTypeText(int scanType) {
		switch (scanType) {
		case 0:
			return "normal x-y-z-scan";
		case 1:
			return "z-Scan (x-z-plane)";
		case 2:
			return "line scan";
		case 3:
			return "time series x-y";
		case 4:
			return "time series x-z (release 2.0  or later)";
		case 5:
			return "time series ?Mean of ROIs?";
		case 6:
			return "time series x-y-z";
		case 7:
			return "spline scan";
		case 8:
			return "spline plane x-z";
		case 9:
			return "time series spline plane x-z";
		case 10:
			return "point mode";
		default:
			return "normal x-y-z-scan";
		}
	}
}
