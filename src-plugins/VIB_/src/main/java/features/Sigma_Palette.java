/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.plugin.PlugIn;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import stacks.ThreePaneCrop;
import ij.plugin.filter.Duplicater;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Image;
import java.awt.Scrollbar;
import java.awt.Label;
import java.awt.event.*;

import util.Limits;

public class Sigma_Palette extends SigmaPalette implements PlugIn {

	public void run( String ignoredArguments ) {

		System.out.println("In the run() method...");

		image = IJ.getImage();
		if( image == null ) {
			IJ.error("There is no current image");
			return;
		}

		Calibration calibration = image.getCalibration();
                double minimumSeparation = 1;
                if( calibration != null )
                        minimumSeparation = Math.min(calibration.pixelWidth,
                                                     Math.min(calibration.pixelHeight,
                                                              calibration.pixelDepth));

		Roi roi = image.getRoi();
		if( roi == null ) {
			IJ.error("There is no current point selection");
			return;
		}

		if( roi.getType() != Roi.POINT ) {
			IJ.error("You must have a point selection");
			return;
		}

		Polygon p = roi.getPolygon();

		if(p.npoints != 1) {
			IJ.error("You must have exactly one point selected");
			return;
		}

		ImageProcessor processor = image.getProcessor();

		int x = p.xpoints[0];
		int y = p.ypoints[0];
		int z = image.getCurrentSlice() - 1;

		int either_side = 40;

		int x_min = x - either_side;
		int x_max = x + either_side;
		int y_min = y - either_side;
		int y_max = y + either_side;
		int z_min = z - either_side;
		int z_max = z + either_side;

		int originalWidth = image.getWidth();
		int originalHeight = image.getHeight();
		int originalDepth = image.getStackSize();

		if( x_min < 0 )
			x_min = 0;
		if( y_min < 0 )
			y_min = 0;
		if( z_min < 0 )
			z_min = 0;
		if( x_max >= originalWidth )
			x_max = originalWidth - 1;
		if( y_max >= originalHeight )
			y_max = originalHeight - 1;
		if( z_max >= originalDepth )
			z_max = originalDepth - 1;

		double [] sigmas = new double[9];
		for( int i = 0; i < sigmas.length; ++i ) {
			sigmas[i] = ((i + 1) * minimumSeparation) / 2;
		}

		makePalette( image, x_min, x_max, y_min, y_max, z_min, z_max, new TubenessProcessor(true), sigmas, 4, 3, 3, z );
	}

}
