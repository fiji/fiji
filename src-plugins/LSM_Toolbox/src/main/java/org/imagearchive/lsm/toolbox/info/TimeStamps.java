package org.imagearchive.lsm.toolbox.info;

public class TimeStamps {

	public long Size = 0;

	public long NumberTimeStamps = 0;

	public double[] Stamps;

	public double FirstTimeStamp = 0;

    public double[] TimeStamps; //calculated Stamps[n+1] - lsm_fi.TS_STAMPS[n]
}
