/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package util;

import java.awt.Color;

public class Arrow {
	
	public Arrow( Color c,
		      double start_x, double start_y, double start_z,
		      double vx, double vy, double vz,
		      int length ) {
		
		this.c = c;
		this.start_x = start_x;
		this.start_y = start_y;
		this.start_z = start_z;
		this.vx = vx;
		this.vy = vy;
		this.vz = vz;
		this.length = length;
	}
	
	public Color c;
	
	public double start_x;
	public double start_y;
	public double start_z;
	
	public double vx;
	public double vy;
	public double vz;
	
	public int length;
}
