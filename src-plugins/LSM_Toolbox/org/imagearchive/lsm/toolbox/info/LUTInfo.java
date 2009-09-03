package org.imagearchive.lsm.toolbox.info;

public class LUTInfo {
	public long LUT_SIZE = 0;

	public long SUBBLOCKS_COUNT = 0;

	public long CHANNELS_COUNT = 0;

	public long LUT_TYPE = 0;

	public long ADVANCED = 0;

	public long CURRENT_CHANNEL = 0;

	// GAMMA
	public long G_TYPE = 0;

	public long G_SIZE = 0;

	public double[] G_CHANNEL;

	// BRIGHTNESS
	public long B_TYPE = 0;

	public long B_SIZE = 0;

	public double[] B_CHANNEL;

	// CONTRAST
	public long C_TYPE = 0;

	public long C_SIZE = 0;

	public double[] C_CHANNEL;

	// RAMP
	public long R_TYPE = 0;

	public long R_SIZE = 0;

	public double[] R_CHANNELSX;

	public double[] R_CHANNELSY;

	public double[] R_CHANNELEX;

	public double[] R_CHANNELEY;

	// KNOTS
	public long K_TYPE = 0;

	public long K_SIZE = 0;

	public long KNOTS = 0;

	public double[] K_CHANNELX;

	public double[] K_CHANNELY;

	// PALETTE
	public long P_TYPE = 0;

	public long P_SIZE = 0;

	public byte[] P_CHANNEL;
}
