/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.awt.*;
import java.util.ArrayList;
import java.io.*;

import amira.AmiraParameters;

import ij.*;
import ij.io.*;
import ij.process.ColorProcessor;

import ij.gui.*;

import ij.plugin.PlugIn;

import landmarks.Bookstein_From_Landmarks;

import vib.transforms.OrderedTransformations;

import util.BatchOpener;
import util.FileAndChannel;
import vib.oldregistration.RegistrationAlgorithm;

public class CreateTracingVolume_ implements PlugIn {

	static final boolean verbose = SimpleNeuriteTracer.verbose;

        static final int NONE=0;
        static final int EB=9;
        static final int NOD=10;
        static final int FB=11;
        static final int PB=12;

        public void run(String arg) {

                // - take our big template image
                // - map that to the labelled standard brain image
                // - create a new image stack the same size as the big template
                // - go through each pixel in the template - if it maps to one in one of the central complex region, include it with the right colour
                // - set calibration on new image


		/* changes:
		   take our random image with point and trace labellings
		   map that to the labelled standard brain image
		   work out double the size of the standard brain image
		   go through each pixel in the standard brain image, colouring it with the

		*/

                String standardBrainFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.grey";
                String standardBrainLabelsFileName="/media/WD USB 2/standard-brain/data/vib-drosophila/CantonM43c.labels";

                FileAndChannel standardBrainFC=new FileAndChannel(standardBrainFileName,0);

                String realImageFileName="/media/WD USB 2/corpus/central-complex/c061AK.lsm";

                FileAndChannel realImageFC=new FileAndChannel(realImageFileName,0);
		String tracesFileName = realImageFileName + ".traces";

		// ------------------------------------------------------------------------

		PathAndFillManager manager=new PathAndFillManager();
		manager.loadGuessingType(tracesFileName);
		ArrayList< Path > allPaths=manager.getAllPaths();

                Bookstein_From_Landmarks matcher=new Bookstein_From_Landmarks();
                matcher.loadImages(standardBrainFC,realImageFC);
		matcher.generateTransformation();

                ImageStack realImageStack=matcher.getDomain().getStack();

		ImagePlus standardBrain=matcher.getTemplate();

		double scaleStandard = 1;

                int newWidth = (int)( standardBrain.getWidth() * scaleStandard );
                int newHeight = (int)( standardBrain.getHeight() * scaleStandard );
                int newDepth = (int)( standardBrain.getStackSize() * scaleStandard );

                ImagePlus labels;
                {
                        ImagePlus[] tmp=BatchOpener.open(standardBrainLabelsFileName);
                        labels=tmp[0];
                }
                if (verbose) System.out.println("   labels were: "+labels);

		// need to get the AmiraParameters object for that image...

		AmiraParameters parameters = new AmiraParameters(labels);

		int materials = parameters.getMaterialCount();

		int redValues[] = new int[materials];
		int greenValues[] = new int[materials];
		int blueValues[] = new int[materials];

		for( int i=0; i < materials; i++ ) {

			double[] c = parameters.getMaterialColor(i);

			redValues[i] = (int)(255*c[0]);
			greenValues[i] = (int)(255*c[1]);
			blueValues[i] = (int)(255*c[2]);
		}

                ImageStack labelStack=labels.getStack();

                int templateWidth=labelStack.getWidth();
                int templateHeight=labelStack.getHeight();
                int templateDepth=labelStack.getSize();

                if (verbose) System.out.println("About to create stack of size: "+newWidth+","+newHeight+","+newDepth);

                ImageStack newStack=new ImageStack(newWidth,newHeight);

                int x, y, z;

                byte[][] label_data=new byte[templateDepth][];
                for( z = 0; z < templateDepth; ++z )
                        label_data[z] = (byte[])labelStack.getPixels( z + 1 );

                byte [][] redPixels = new byte[newDepth][];
                byte [][] greenPixels = new byte[newDepth][];
                byte [][] bluePixels = new byte[newDepth][];

                for( z=0;z<newDepth;++z) {

                        if (verbose) System.out.println("Creating slice: "+z);

                        redPixels[z] = new byte[ newWidth * newHeight ];
                        greenPixels[z] = new byte[ newWidth * newHeight ];
                        bluePixels[z] = new byte[ newWidth * newHeight ];

                        for(y=0;y<newHeight;++y) {
                                for(x=0;x<newWidth;++x) {

					int label_value=label_data[(int)(z/scaleStandard)][(int)(y/scaleStandard)*templateWidth+(int)(x/scaleStandard)]&0xFF;

					if( label_value >= materials ) {
						IJ.error( "A label value of " + label_value + " was found, which is not a valid material (max " + (materials - 1) + ")" );
						return;
					}

					redPixels[z][y*newWidth+x] = (byte)( redValues[label_value] / 1 );
					greenPixels[z][y*newWidth+x] = (byte)( greenValues[label_value] / 1 );
					bluePixels[z][y*newWidth+x] = (byte)( blueValues[label_value] / 1 );

				}
			}
		}

		RegistrationAlgorithm.ImagePoint imagePoint = new RegistrationAlgorithm.ImagePoint();

		if( allPaths != null ) {
			// if (verbose) System.out.println("Have some allPaths paths to draw.");
			int paths = allPaths.size();
			// if (verbose) System.out.println("Paths to draw: "+paths);
			for( int i = 0; i < paths; ++i ) {

				Path p = allPaths.get(i);

				int last_x_in_template = -1;
				int last_y_in_template = -1;
				int last_z_in_template = -1;

				for( int k = 0; k < p.size(); ++k ) {

					int x_in_domain = p.getXUnscaled(k);
					int y_in_domain = p.getYUnscaled(k);
					int z_in_domain = p.getZUnscaled(k);

					matcher.transformDomainToTemplate( x_in_domain, y_in_domain, z_in_domain, imagePoint );

					int x_in_template=imagePoint.x;
					int y_in_template=imagePoint.y;
					int z_in_template=imagePoint.z;

					if( (last_x_in_template >= 0) &&
					    (last_y_in_template >= 0) &&
					    (last_z_in_template >= 0) ) {

						int xdiff = Math.abs( x_in_template - last_x_in_template );
						int ydiff = Math.abs( y_in_template - last_y_in_template );
						int zdiff = Math.abs( z_in_template - last_z_in_template );

						if( xdiff > 5 || ydiff > 5 || zdiff > 5 ) {
							if (verbose) System.out.println("too long in path: "+i+", at point "+k);
						}

						int xdiff_s = x_in_template - last_x_in_template;
						int ydiff_s = y_in_template - last_y_in_template;
						int zdiff_s = z_in_template - last_z_in_template;

						// Draw a line from last_ to current...

						// Shoddy algorithm for the moment.

						// In order of size, must be one of these options:
						//
						//    zdiff >= ydiff >= xdiff
						//    zdiff >= xdiff >= ydiff
						//    ydiff >= xdiff >= zdiff
						//    ydiff >= zdiff >= xdiff
						//    xdiff >= ydiff >= zdiff
						//    xdiff >= zdiff >= ydiff

						// For the moment i'm collapsing these into 3 cases (zdiff, ydiff or xdiff largest)

						// Each of these cases:

						// if (verbose) System.out.println( "x from: " + last_x_in_template + " to " + x_in_template );
						// if (verbose) System.out.println( "y from: " + last_y_in_template + " to " + y_in_template );
						// if (verbose) System.out.println( "z from: " + last_z_in_template + " to " + z_in_template );

						long line_x, line_y, line_z;

						if( (zdiff >= ydiff) && (zdiff >= xdiff) ) {

							if( zdiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int z_step;
								if( last_z_in_template <= z_in_template ) {
									z_step = 1;
								} else {
									z_step = -1;
								}
								line_z = last_z_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_z - last_z_in_template) / (double)zdiff;
									// if (verbose) System.out.println( proportion_along + " of xdiff_s " + xdiff_s );
									// if (verbose) System.out.println( proportion_along + " of ydiff_s " + ydiff_s );
									double y_delta = proportion_along * ydiff_s;
									double x_delta = proportion_along * xdiff_s;
									line_y = Math.round(y_delta + last_y_in_template);
									line_x = Math.round(x_delta + last_x_in_template);
									// if (verbose) System.out.println( "x is: "+line_x+" (width: "+newWidth+")");
									// if (verbose) System.out.println( "y is: "+line_y+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_z += z_step;
								} while( line_z != z_in_template );
							}

						} else if( (ydiff >= zdiff) && (ydiff >= xdiff) ) {

							if( ydiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int y_step;
								if( last_y_in_template <= y_in_template ) {
									y_step = 1;
								} else {
									y_step = -1;
								}
								line_y = last_y_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_y - last_y_in_template) / (double)ydiff;
									// if (verbose) System.out.println( proportion_along + " of xdiff_s " + xdiff_s );
									// if (verbose) System.out.println( proportion_along + " of zdiff_s " + zdiff_s );
									double z_delta = proportion_along * zdiff_s;
									double x_delta = proportion_along * xdiff_s;
									line_z = Math.round(z_delta + last_z_in_template);
									line_x = Math.round(x_delta + last_x_in_template);
									// if (verbose) System.out.println( "x is: "+line_x+" (width: "+newWidth+")");
									// if (verbose) System.out.println( "z is: "+line_z+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_y += y_step;
								} while( line_y != y_in_template );
							}

						} else if( (xdiff >= ydiff) && (xdiff >= zdiff) ) {

							if( xdiff == 0 ) {
								int in_plane = y_in_template*newWidth+x_in_template;
								redPixels[z_in_template][in_plane] = (byte)255;
								greenPixels[z_in_template][in_plane] = (byte)255;
								bluePixels[z_in_template][in_plane] = (byte)255;
							} else {
								int x_step;
								if( last_x_in_template <= x_in_template ) {
									x_step = 1;
								} else {
									x_step = -1;
								}
								line_x = last_x_in_template;
								do {
									// So the vector from the start point (last_(xyz)) to the end point is
									double proportion_along = Math.abs(line_x - last_x_in_template) / (double)xdiff;
									// if (verbose) System.out.println( proportion_along + " of ydiff_s " + ydiff_s );
									// if (verbose) System.out.println( proportion_along + " of zdiff_s " + zdiff_s );
									double z_delta = proportion_along * zdiff_s;
									double y_delta = proportion_along * ydiff_s;
									line_z = Math.round(z_delta + last_z_in_template);
									line_y = Math.round(y_delta + last_y_in_template);
									// if (verbose) System.out.println( "z is: "+line_z+" (depth: "+newDepth+")");
									// if (verbose) System.out.println( "y is: "+line_y+" (height: "+newHeight+")");
									int in_plane = (int)( line_y * newWidth + line_x );
									redPixels[(int)line_z][in_plane] = (byte)255;
									greenPixels[(int)line_z][in_plane] = (byte)255;
									bluePixels[(int)line_z][in_plane] = (byte)255;
									line_x += x_step;
								} while( line_x != x_in_template );

							}
						}
					}

					last_x_in_template = x_in_template;
					last_y_in_template = y_in_template;
					last_z_in_template = z_in_template;
				}
			}
		}

		for( z = 0; z < newDepth; ++z ) {

			// if (verbose) System.out.println("Actually adding slice: "+z);

                        ColorProcessor cp = new ColorProcessor( newWidth, newHeight );
                        cp.setRGB( redPixels[z], greenPixels[z], bluePixels[z] );
                        newStack.addSlice( null, cp );
                }

                ImagePlus impNew=new ImagePlus("tracings stack",newStack);

		impNew.show();

                // String outputFilename="/home/s9808248/saturn1/vib/ImageJ/hanesch.tif";
                // FileSaver fileSaver=new FileSaver(impNew);
                // fileSaver.saveAsTiffStack(outputFilename);

        }

}

