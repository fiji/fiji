/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* A convenience class for returning Threshold information for the
   PCA_Registration plugin. */

package vib.transforms;

public class Threshold {
	
	public int value;
	public long belowThreshold;
	
	public Threshold( int value, long belowThreshold ) {
		this.value = value;
		this.belowThreshold = belowThreshold;
	}
	
}
