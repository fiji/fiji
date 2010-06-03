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

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;
import ij.measure.Calibration;
import ij.io.*;

import ij3d.Image3DUniverse;
import ij3d.Image3DMenubar;
import ij3d.Content;
import ij3d.Pipe;
import ij3d.Mesh_Maker;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import ij.gui.GUI;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.IndexColorModel;

import java.io.*;

import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import client.ArchiveClient;

import stacks.ThreePanes;

import util.BatchOpener;
import util.RGB_to_Luminance;

import features.GaussianGenerationCallback;
import features.ComputeCurvatures;

import amira.AmiraMeshDecoder;
import amira.AmiraParameters;

import features.Sigma_Palette;
import features.TubenessProcessor;

/* Note on terminology:

      "traces" files are made up of "paths".  Paths are non-branching
      sequences of adjacent points (including diagonals) in the image.
      Branches and joins are supported by attributes of paths that
      specify that they begin on (or end on) other paths.

 */

public class Simple_Neurite_Tracer extends SimpleNeuriteTracer
		implements PlugIn {
	public void run( String ignoredArguments ) {

		/* The useful macro options are:

		     imagefilename=<FILENAME>
		     tracesfilename=<FILENAME>
		     use_3d
		     use_three_pane
		*/

		String macroOptions = Macro.getOptions();

		String macroImageFilename = null;
		String macroTracesFilename = null;

		if( macroOptions != null ) {
			macroImageFilename = Macro.getValue(
				macroOptions, "imagefilename", null );
			macroTracesFilename = Macro.getValue(
				macroOptions, "tracesfilename", null );
		}

		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient = new ArchiveClient( applet, macroOptions );
		}

		if( archiveClient != null )
			archiveClient.closeChannelsWithTag("nc82");

		try {

			ImagePlus currentImage = null;
			if( macroImageFilename == null ) {
				currentImage = IJ.getImage();
			} else {
				currentImage = BatchOpener.openFirstChannel( macroImageFilename );
				if( currentImage == null ) {
					IJ.error("Opening the image file specified in the macro parameters ("+macroImageFilename+") failed.");
					return;
				}
				currentImage.show();
			}

			if( currentImage == null ) {
				IJ.error( "There's no current image to trace." );
				return;
			}

			// Check this isn't a composite image or hyperstack:
			if( currentImage.getNFrames() > 1 ) {
				IJ.error("This plugin only works with single images, not multiple images in a time series.");
				return;
			}

			if( currentImage.getNChannels() > 1 ) {
				IJ.error("This plugin only works with single channel images: use 'Image>Color>Split Channels' and choose a channel");
				return;
			}

			imageType = currentImage.getType();

			if( imageType == ImagePlus.COLOR_RGB ) {
				YesNoCancelDialog queryRGB = new YesNoCancelDialog( IJ.getInstance(),
										    "Convert RGB image",
										    "Convert this RGB image to an 8 bit luminance image first?\n" +
										    "(If you want to trace a particular channel instead, cancel and \"Split Channels\" first.)" );

				if( ! queryRGB.yesPressed() ) {
					return;
				}

				currentImage = RGB_to_Luminance.convertToLuminance(currentImage);
				currentImage.show();
				imageType = currentImage.getType();
			}

			if( currentImage.getStackSize() == 1 )
				singleSlice = true;

			width = currentImage.getWidth();
			height = currentImage.getHeight();
			depth = currentImage.getStackSize();

			Calibration calibration = currentImage.getCalibration();
			if( calibration != null ) {
				x_spacing = calibration.pixelWidth;
				y_spacing = calibration.pixelHeight;
				z_spacing = calibration.pixelDepth;
				spacing_units = calibration.getUnits();
				if( spacing_units == null || spacing_units.length() == 0 )
					spacing_units = "" + calibration.getUnit();
			}

			pathAndFillManager = new PathAndFillManager(this);

			file_info = currentImage.getOriginalFileInfo();

			// Turn it grey, since I find that helpful:
			{
				ImageProcessor imageProcessor = currentImage.getProcessor();
				byte [] reds = new byte[256];
				byte [] greens = new byte[256];
				byte [] blues = new byte[256];
				for( int i = 0; i < 256; ++i ) {
					reds[i] = (byte)i;
					greens[i] = (byte)i;
					blues[i] = (byte)i;
				}
				IndexColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);
				imageProcessor.setColorModel( cm );
				if( currentImage.getStackSize() > 1 )
					currentImage.getStack().setColorModel( cm );
				currentImage.updateAndRepaintWindow();
			}

			if( file_info != null ) {
				String originalFileName=file_info.fileName;
				if (verbose) System.out.println("originalFileName was: "+originalFileName);
				if( originalFileName != null ) {
					int lastDot=originalFileName.lastIndexOf(".");
					if( lastDot > 0 ) {
						String beforeExtension=originalFileName.substring(0, lastDot);
						String tubesFileName=beforeExtension+".tubes.tif";
						ImagePlus tubenessImage = null;
						File tubesFile=new File(file_info.directory,tubesFileName);
						if (verbose) System.out.println("Testing for the existence of "+tubesFile.getAbsolutePath());
						if( tubesFile.exists() ) {
							long megaBytesExtra = ( ((long)width) * height * depth * 4 ) / (1024 * 1024);
							String extraMemoryNeeded = megaBytesExtra + "MiB";
							YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(),
												     "Confirm",
												     "A tubeness file ("+tubesFile.getName()+") exists.  Load this file?\n"+
												     "(This would use an extra "+extraMemoryNeeded+" of memory.)");
							if( d.cancelPressed() )
								return;
							else if( d.yesPressed() ) {
								IJ.showStatus("Loading tubes file.");
								tubenessImage=BatchOpener.openFirstChannel(tubesFile.getAbsolutePath());
								if (verbose) System.out.println("Loaded the tubeness file");
								if( tubenessImage == null ) {
									IJ.error("Failed to load tubes image from "+tubesFile.getAbsolutePath()+" although it existed");
									return;
								}
								if( tubenessImage.getType() != ImagePlus.GRAY32 ) {
									IJ.error("The tubeness file must be a 32 bit float image - "+tubesFile.getAbsolutePath()+" was not.");
									return;
								}
								int width = tubenessImage.getWidth();
								int height = tubenessImage.getHeight();
								int depth = tubenessImage.getStackSize();
								ImageStack tubenessStack = tubenessImage.getStack();
								tubeness = new float[depth][];
								for( int z = 0; z < depth; ++z ) {
									FloatProcessor fp = (FloatProcessor)tubenessStack.getProcessor( z + 1 );
									tubeness[z] = (float[])fp.getPixels();
								}
							}
						}
					}
				}
			}

			single_pane = true;
			Image3DUniverse universeToUse = null;
			String [] choices3DViewer = null;;

			if( ! singleSlice ) {
				boolean java3DAvailable = haveJava3D();
				boolean showed3DViewerOption = false;

				GenericDialog gd = new GenericDialog("Simple Neurite Tracer (v" +
								     PLUGIN_VERSION + ")");
				gd.addMessage("Tracing the image: "+currentImage.getTitle());
				String extraMemoryNeeded = " (will use an extra: ";
				int bitDepth = currentImage.getBitDepth();
				int byteDepth = bitDepth == 24 ? 4 : bitDepth / 8;
				long megaBytesExtra = ( ((long)width) * height * depth * byteDepth * 2 ) / (1024 * 1024);
				extraMemoryNeeded += megaBytesExtra + "MiB of memory)";

				gd.addCheckbox("Use_three_pane view?"+extraMemoryNeeded, false);

				if( ! java3DAvailable ) {
					String message = "(Java3D classes don't seem to be available, so no 3D viewer option is available.)";
					System.out.println(message);
					gd.addMessage(message);
				} else if( currentImage.getBitDepth() != 8 ) {
					String message = "(3D viewer option is only currently available for 8 bit images)";
					System.out.println(message);
					gd.addMessage(message);
				} else {
					showed3DViewerOption = true;
					choices3DViewer = new String[Image3DUniverse.universes.size()+2];
					String no3DViewerString = "No 3D view";
					String useNewString = "Create New 3D Viewer";
					choices3DViewer[choices3DViewer.length-2] = useNewString;
					choices3DViewer[choices3DViewer.length-1] = no3DViewerString;
					for( int i = 0; i < choices3DViewer.length - 2; ++i ) {
						String contentsString = Image3DUniverse.universes.get(i).allContentsString();
						String shortContentsString;
						if( contentsString.length() == 0 )
							shortContentsString = "[Empty]";
						else
							shortContentsString = contentsString.substring(0,Math.min(40,contentsString.length()-1));
						choices3DViewer[i] = "Use 3D viewer ["+i+"] containing " + shortContentsString;
					}
					gd.addChoice( "Choice of 3D Viewer:", choices3DViewer, useNewString );
				}

				gd.showDialog();
				if (gd.wasCanceled())
					return;

				single_pane = ! gd.getNextBoolean();
				if( showed3DViewerOption ) {
					String chosenViewer = gd.getNextChoice();
					int chosenIndex;
					for( chosenIndex = 0; chosenIndex < choices3DViewer.length; ++chosenIndex )
						if( choices3DViewer[chosenIndex].equals(chosenViewer) )
							break;
					if( chosenIndex == choices3DViewer.length - 2 ) {
						use3DViewer = true;
						universeToUse = null;
					} else if( chosenIndex == choices3DViewer.length - 1 ) {
						use3DViewer = false;
						universeToUse = null;
					} else {
						use3DViewer = true;
						universeToUse = Image3DUniverse.universes.get(chosenIndex);;
					}
				}
			}

			initialize(currentImage);

			xy_tracer_canvas = (InteractiveTracerCanvas)xy_canvas;
			xz_tracer_canvas = (InteractiveTracerCanvas)xz_canvas;
			zy_tracer_canvas = (InteractiveTracerCanvas)zy_canvas;

			setupTrace = true;
			resultsDialog = new NeuriteTracerResultsDialog( "Tracing for: " + xy.getShortTitle(),
									this,
									applet != null );

			/* FIXME: the first could be changed to add
			   'this', and move the small implementation
			   out of NeuriteTracerResultsDialog into this
			   class. */
			pathAndFillManager.addPathAndFillListener(resultsDialog);
			pathAndFillManager.addPathAndFillListener(resultsDialog.pw);
			pathAndFillManager.addPathAndFillListener(resultsDialog.fw);

			if( (x_spacing == 0.0) ||
			    (y_spacing == 0.0) ||
			    (z_spacing == 0.0) ) {

				IJ.error( "One dimension of the calibration information was zero: (" +
					  x_spacing + "," + y_spacing + "," + z_spacing + ")" );
				return;

			}

			{
				ImageStack s = xy.getStack();
				switch(imageType) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					slices_data_b = new byte[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_b[z] = (byte []) s.getPixels( z + 1 );
					stackMin = 0;
					stackMax = 255;
					break;
				case ImagePlus.GRAY16:
					slices_data_s = new short[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_s[z] = (short []) s.getPixels( z + 1 );
					IJ.showStatus("Finding stack minimum / maximum");
					for( int z = 0; z < depth; ++z ) {
						for( int y = 0; y < height; ++y )
							for( int x = 0; x < width; ++x ) {
								short v = slices_data_s[z][y*width+x];
								if( v < stackMin )
									stackMin = v;
								if( v > stackMax )
									stackMax = v;
							}
						IJ.showProgress( z / (float)depth );
					}
					IJ.showProgress(1.0);
					break;
				case ImagePlus.GRAY32:
					slices_data_f = new float[depth][];
					for( int z = 0; z < depth; ++z )
						slices_data_f[z] = (float []) s.getPixels( z + 1 );
					IJ.showStatus("Finding stack minimum / maximum");
					for( int z = 0; z < depth; ++z ) {
						for( int y = 0; y < height; ++y )
							for( int x = 0; x < width; ++x ) {
								float v = slices_data_f[z][y*width+x];
								if( v < stackMin )
									stackMin = v;
								if( v > stackMax )
									stackMax = v;
							}
						IJ.showProgress( z / (float)depth );
					}
					IJ.showProgress(1.0);
					break;
				}
			}

			xy_tracer_canvas.addKeyListener( xy_tracer_canvas );
			xy_window.addKeyListener( xy_tracer_canvas );

			if( ! single_pane ) {

				xz_tracer_canvas.addKeyListener( xz_tracer_canvas );
				xz_window.addKeyListener( xz_tracer_canvas );

				zy_tracer_canvas.addKeyListener( zy_tracer_canvas );
				zy_window.addKeyListener( zy_tracer_canvas );

			}

			if( use3DViewer ) {

				boolean reusing;
				if( universeToUse == null ) {
					reusing = false;
					univ = new Image3DUniverse(512, 512);
				} else {
					reusing = true;
					univ = universeToUse;
				}
				univ.setUseToFront(false);
				univ.addUniverseListener(pathAndFillManager);
				if( ! reusing ) {
					univ.show();
					GUI.center(univ.getWindow());
				}
				boolean [] channels = { true, true, true };

				String title = "Image for tracing ["+currentImage.getTitle()+"]";
				String contentName = univ.getSafeContentName( title );
				// univ.resetView();
				Content c = univ.addContent(xy,
							    new Color3f(Color.white),
							    contentName,
							    10, // threshold
							    channels,
							    guessResamplingFactor(), // resampling factor
							    Content.VOLUME);
				c.setLocked(true);
				c.setTransparency(0.5f);
				if( ! reusing )
					univ.resetView();
				univ.setAutoAdjustView(false);
			}

			File tracesFileToLoad = null;
			if( macroTracesFilename != null ) {
				tracesFileToLoad = new File( macroTracesFilename );
				if( tracesFileToLoad.exists() )
					pathAndFillManager.loadGuessingType( tracesFileToLoad.getAbsolutePath() );
				else
					IJ.error("The traces file suggested by the macro parameters ("+macroTracesFilename+") does not exist");
			}

			resultsDialog.displayOnStarting();

		} finally {
			IJ.getInstance().addKeyListener( IJ.getInstance() );
		}
	}
}
