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

	public void run( ) {

		ImagePlus cropped = ThreePaneCrop.performCrop( image, x_min, x_max, y_min, y_max, z_min, z_max, false );

		croppedWidth  = (x_max - x_min) + 1;
		croppedHeight = (y_max - y_min) + 1;
		croppedDepth  = (z_max - z_min) + 1;

		if( sigmaValues.length > sigmasAcross * sigmasDown ) {
			IJ.error( "A "+sigmasAcross+"x"+sigmasDown+" layout is not large enough for "+sigmaValues+" + 1 images" );
			return;
		}

		int paletteWidth = croppedWidth * sigmasAcross + (sigmasAcross + 1);
		int paletteHeight = croppedHeight * sigmasDown + (sigmasDown + 1);

		ImageStack newStack = new ImageStack( paletteWidth, paletteHeight );
		for( int z = 0; z < croppedDepth; ++z ) {
			FloatProcessor fp = new FloatProcessor( paletteWidth, paletteHeight );
			newStack.addSlice("",fp);
		}
		paletteImage = new ImagePlus("Pick Sigma and Maximum",newStack);
		setMax(defaultMax);

		PaletteCanvas paletteCanvas = new PaletteCanvas( paletteImage, this, croppedWidth, croppedHeight, sigmasAcross, sigmasDown );
		PaletteStackWindow paletteWindow = new PaletteStackWindow( paletteImage, paletteCanvas, this, defaultMax );

		paletteImage.setSlice( (initial_z - z_min) + 1 );

		for( int sigmaIndex = 0; sigmaIndex < sigmaValues.length; ++sigmaIndex ) {
			int sigmaY = sigmaIndex / sigmasAcross;
			int sigmaX = sigmaIndex % sigmasAcross;
			int offsetX = sigmaX * (croppedWidth + 1) + 1;
			int offsetY = sigmaY * (croppedHeight + 1) + 1;
			double sigma = sigmaValues[sigmaIndex];
			hep.setSigma(sigma);
			ImagePlus processed = hep.generateImage(cropped);
			if( ! paletteWindow.manuallyChangedAlready ) {
				float [] limits = Limits.getStackLimits( processed );
				int suggestedMax = (int)limits[1];
				paletteWindow.maxValueScrollbar.setValue( suggestedMax );
				paletteWindow.maxChanged( suggestedMax );
			}
			copyIntoPalette( processed, paletteImage, offsetX, offsetY );
			paletteImage.updateAndDraw();
		}
	}
}
