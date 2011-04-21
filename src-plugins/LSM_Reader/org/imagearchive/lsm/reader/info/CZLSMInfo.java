package org.imagearchive.lsm.reader.info;

public class CZLSMInfo {

	public long DimensionX = 0;

	public long DimensionY = 0;

	public long DimensionZ = 0;

	public long DimensionP = 0;

	public long DimensionM = 0;

	public long DimensionChannels = 0;

	public long DimensionTime = 0;

	public long IntensityDataType = 0;

	public long ThumbnailX = 0;

	public long ThumbnailY = 0;

	public double VoxelSizeX = 0;

	public double VoxelSizeY = 0;

	public double VoxelSizeZ = 0;

	public int ScanType = 0;

	public long OffsetChannelColors = 0;

	public ChannelNamesAndColors channelNamesAndColors;

	public long OffsetChannelDataTypes = 0;

	public int[] OffsetChannelDataTypesValues;
}