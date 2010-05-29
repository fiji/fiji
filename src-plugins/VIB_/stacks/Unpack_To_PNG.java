/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation; either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License. 

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import util.BatchOpener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.gui.ImageWindow;
import ij.Macro;
import ij.LookUpTable;
import ij.plugin.PlugIn;
import ij.plugin.Thresholder;
import ij.plugin.filter.ThresholdToSelection;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.PolygonRoi;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import amira.AmiraParameters;

public class Unpack_To_PNG implements PlugIn {

	public Unpack_To_PNG( ) {
		
	}
	
	public void run( String pluginArguments ) {

		String realArguments = null;

		String macroArguments = Macro.getOptions();
				
		if( (macroArguments == null) || (macroArguments.equals("")) ) {

			if( (pluginArguments == null) || (pluginArguments.equals("")) ) {
				IJ.error("No parameters supplied either as macro options or a plugin argument.");
				return;
			} else {
				realArguments = pluginArguments;
			}

		} else { 
			realArguments = macroArguments;
		}
		
		String filename = Macro.getValue(
			realArguments,
			"filename",
			"");
		
		if( filename.equals("") ) {
			IJ.error("No macro parameter filename supplied");
			return;
		}
		
		String destinationDirectory = Macro.getValue(
			macroArguments,
			"directory",
			"");
		
		if( destinationDirectory.equals("") ) {
			IJ.error("No macro parameter directory supplied");
			return;
		}	
		
		ImagePlus [] imps = BatchOpener.open(
			filename );

		if( imps == null ) {
			IJ.error("Couldn't open the file: "+filename);
			return;
		}

		if( AmiraParameters.isAmiraLabelfield(imps[0]) ) {
			
			System.out.println("Looks like an Amira label file...");

			try {
				unpackAmiraLabelFieldToPNGs(imps[0],destinationDirectory);
			} catch( IOException e ) {
				IJ.error( "There was an IOException while unpacking the label file: "+e);
			}
			return;
		}

		System.out.println("Decided it doesn't look a label file, but has "+imps.length+" channels.");

		for( int i = 0; i < imps.length; ++i ) {

			ImagePlus imp = imps[i];

			int stackDepth = imp.getStackSize();
			
			for( int z = 0; z < stackDepth; ++z ) {

				DecimalFormat f2 = new DecimalFormat("00");
				DecimalFormat f5 = new DecimalFormat("00000");
				
				String outputFileName = f2.format(i) + "-" +
					f5.format(z)+".png";
				
				outputFileName = destinationDirectory +
					File.separator + outputFileName;
				
				try {
					writeImage( imp, z, outputFileName, -1 );
				} catch( Exception e ) {
					System.err.println("Caught an exception: "+e);
					return;
				}
				
			}
			
		}
	       
	}

	ArrayList<Polygon> getPolygonsNonBackground( ByteProcessor bp ) {

		ArrayList<Polygon> polygons = new ArrayList<Polygon>();
		
		Roi roi = getRoiNonBackground( bp );

		if( roi == null )
			return polygons;

		Roi [] rois = null;

		if( roi.getType() == Roi.COMPOSITE ) {
			ShapeRoi shapeRoi = (ShapeRoi)roi;
			rois = shapeRoi.getRois();			
		} else if( roi.getType() == Roi.TRACED_ROI ) {			
			PolygonRoi polygonRoi = (PolygonRoi)roi;
			rois = new Roi[1];
			rois[0] = polygonRoi;
		} else {
			System.out.println("Unhandled Roi type: "+roi.getType()+", "+roi.getTypeAsString());
		}

		for( int i = 0; i < rois.length; ++i ) {		

			Polygon p = rois[i].getPolygon();
			polygons.add(p);
		}

		return polygons;
	}

	Roi getRoiNonBackground( ByteProcessor bp ) {
		
		bp.setThreshold(1,255,ImageProcessor.NO_LUT_UPDATE);
		// bp.threshold(0);
		
		ImageStack tempStack = new ImageStack(bp.getWidth(),bp.getHeight());
		tempStack.addSlice(null,bp);
		ImagePlus imp = new ImagePlus("example stack",tempStack);
		
		ThresholdToSelection tts = new ThresholdToSelection();
		tts.setup( "", imp );
		tts.run(bp);
				
		// imp.show();
				
		return imp.getRoi();

	}
	
	public static String polygonToAreaCoords( Polygon polygon ) {
		
		String result = "";

		double first_x = -1, first_y = -1;

		double[] coordinates = new double[6];

		boolean empty = true;
		boolean first_points_set = false;

		/* The path starts with a SEG_MOVETO, then lots of
		   SEG_LINETOs and finally a SEG_CLOSE */

		for( PathIterator pi = polygon.getPathIterator(null);
		     ! pi.isDone();
		     pi.next() ) {
		     
			empty = false;

			int type = pi.currentSegment(coordinates);

			/*
			System.out.println( "Type: "+type+" and: [ " +
					    coordinates[0] + ", " +
					    coordinates[1] + ", " +
					    coordinates[2] + ", " +
					    coordinates[3] + ", " +
					    coordinates[4] + ", " +
					    coordinates[5] + "]" );
			*/
			
			if( type == PathIterator.SEG_MOVETO ) {
				first_x = coordinates[0];
				first_y = coordinates[1];
				first_points_set = true;
				result += (int)coordinates[0] + ", " + (int)coordinates[1] + ", ";
			} else if( type == PathIterator.SEG_LINETO ) {
				result += (int)coordinates[0] + ", " + (int)coordinates[1] + ", ";
			} else if( type == PathIterator.SEG_CLOSE ) {
				break;
			} else {
				IJ.error("Unhandled PathIterator type: "+type );
				return "";
			}
		}
		
		if( empty )
			return "";
		
		if( (first_x == coordinates[0]) && (first_y == coordinates[1]) ) {
			// We're done, remove the trailing ", ":		       
			if( result.endsWith(", ") ) {
				result = result.substring(0,result.length()-2);
			}
		} else {
			// Add the first point again:
			result += (int)first_x + ", " + (int)first_y;
		}

		return result;
	}	

	void unpackAmiraLabelFieldToPNGs(ImagePlus labelFileImp,
					 String destinationDirectory) throws IOException {

		if( (labelFileImp.getType() != ImagePlus.GRAY8) &&
		    (labelFileImp.getType() != ImagePlus.COLOR_256) ) {
			IJ.error("The label file appeared not to be 8 bit (!)");
			return;
		}

		AmiraParameters ap = new AmiraParameters(labelFileImp);
	       
		int materialCount = ap.getMaterialCount();

		String [] materialNames = ap.getMaterialList();
		
		int stackDepth = labelFileImp.getStackSize();

		int width=labelFileImp.getWidth();
		int height=labelFileImp.getHeight();

		// Write a material index:

		String jsIndexFileName = destinationDirectory + File.separator + "material-index.js";

		PrintStream ps = new PrintStream(jsIndexFileName);

		ps.println("var materials = [");

		byte [] reds =   new byte[materialCount];
		byte [] greens = new byte[materialCount];
		byte [] blues =  new byte[materialCount];
		for( int i = 0; i < materialCount; ++i ) {
			double [] color = ap.getMaterialColor(i);
			reds[i] = (byte)(color[0] * 255);
		        greens[i] = (byte)(color[1] * 255);
			blues[i] = (byte)(color[2] * 255);
			
			ps.print("  [ \"" + ap.getMaterialName(i) + "\", [ " +
				   (reds[i]&0xFF) + ", " + (greens[i]&0xFF) + ", " +
				   (blues[i]&0xFF) + " ] ]" );
			
			if( i != (materialCount - 1) )
				ps.println(",");
			else
				ps.println("");
		}

		ps.println("];");
		ps.close();

		// Write the dimensions too...

		String dimensionsFileName = destinationDirectory + File.separator + "dimensions.js";

		ps = new PrintStream(dimensionsFileName);
		ps.println("var stack_width = "+width+";");
		ps.println("var stack_height = "+height+";");
		ps.println("var stack_depth = "+stackDepth+";");
		ps.close();

		long [] pixelCountsForMaterial = new long[materialCount];
		float [] xSumsForMaterial = new float[materialCount];
		float [] ySumsForMaterial = new float[materialCount];
		float [] zSumsForMaterial = new float[materialCount];

		byte [] alphas = new byte[materialCount];

		IndexColorModel cm = new IndexColorModel(8,materialCount,reds,greens,blues,0 /* the transparent color */ );

		/* I don't think this approach actually works:
		alphas[0] = (byte)255;
		for( int i = 1; i < materialCount; ++i )
			alphas[i] = (byte)128;
			
		IndexColorModel cm = new IndexColorModel(8,materialCount,reds,greens,blues,alphas );
		*/

		String allMapsFileName = destinationDirectory + File.separator + "all-maps.html";
		PrintStream allps = new PrintStream( allMapsFileName );

		for( int z = 0; z < stackDepth; ++z ) {
			
			DecimalFormat f2 = new DecimalFormat("00");
			DecimalFormat f5 = new DecimalFormat("00000");
			
			String outputFileNameStem = f2.format(0) + "-" +
				f5.format(z)+".png";
			
			outputFileNameStem = destinationDirectory +
				File.separator + outputFileNameStem;
			
			ImageStack stack = labelFileImp.getStack();
			ImageProcessor imageProcessor = stack.getProcessor(z+1);
			
			byte [] pixels = (byte [])imageProcessor.getPixelsCopy();
			
			/* Actually we don't really need to create
			   this Hashset, but never mind... */
			
			HashSet materialsInThisSlice = new HashSet();

			String mapOpen = "<MAP NAME=\"map-"+f5.format(z)+"\">";
			String mapClose = "</MAP>";

			for(int i = 0; i<pixels.length; ++i ) {
				
				int intValue = pixels[i]&0xFF;
				Integer value = new Integer(intValue);
				materialsInThisSlice.add(value);
				
				++ pixelCountsForMaterial[intValue];
				int x = (i % width);
				int y = (i / width);
				xSumsForMaterial[intValue] += x;
				ySumsForMaterial[intValue] += y;
				zSumsForMaterial[intValue] += z;

			}

			allps.println(mapOpen);

			// First just write all the labels out in one PNG:

			{
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
			
				String outputFileName = outputFileNameStem + "-all.png";

				Graphics2D g = (Graphics2D)bi.getGraphics();
				Image imageToDraw = imageProcessor.createImage();
				g.drawImage(imageToDraw, 0, 0, null);
				File f = new File(outputFileName);
				ImageIO.write(bi, "png", f);

			}


			byte [] emptySliceData = new byte[pixels.length];
			for( int i = 0; i < pixels.length; ++i )
				emptySliceData[i] = 0;
			
			ByteProcessor emptyBP = new ByteProcessor( width, height );
			emptyBP.setColorModel(cm);
			emptyBP.setPixels( emptySliceData );

			String imageMapFileName = destinationDirectory + File.separator + "map-" + f5.format(z) + ".html";
			ps = new PrintStream( imageMapFileName );

			for(int material=1; material<materialCount; ++material ) {
				
				String regionName = ap.getMaterialName(material);

				DecimalFormat dfm = new DecimalFormat("000");
				String outputFileName=outputFileNameStem+"-"+
					dfm.format(material)+"-"+
					regionName+".png";

				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
				Graphics2D g = (Graphics2D)bi.getGraphics();				
						
				if( materialsInThisSlice.contains(new Integer(material)) ) {

					byte [] sliceData = new byte[pixels.length];
					for( int i = 0; i < pixels.length; ++i ) {
						if( (pixels[i]&0xFF) == material ) {
							sliceData[i] = (byte)material;
						} else
							sliceData[i] = 0;
					}

					byte [] sliceDataCopy = new byte[sliceData.length];
					System.arraycopy(sliceData,0,sliceDataCopy,0,sliceData.length);

					ByteProcessor singleMaterialBP = new ByteProcessor(width,height);
					singleMaterialBP.setColorModel(cm);
					singleMaterialBP.setPixels(sliceData);
					Image imageToDraw = singleMaterialBP.createImage();
					g.drawImage(imageToDraw, 0, 0, null);
					File f = new File(outputFileName);
					ImageIO.write(bi, "png", f);

					/* Also generate the polygons that surround each neuropil: */
					
					ArrayList<Polygon> polygons = getPolygonsNonBackground( singleMaterialBP );
						
					for( Iterator iterator = polygons.iterator();
					     iterator.hasNext(); ) {
						
						Polygon p = (Polygon)iterator.next();
						
						String areaString = "<area shape=\"poly\" " +
							"alt=\"" + regionName + "\" " +
							"coords=\"" +
							polygonToAreaCoords(p) + "\" href=\"#" +
							regionName + "-" +
							f5.format(z) + "\" onclick=\"selectedArea("+
							z+",'" + regionName + "')\">";
						
						ps.println( areaString  );
						allps.println( areaString );						
					}
					
				} else {
					
					Image imageToDraw = emptyBP.createImage();
					g.drawImage(imageToDraw, 0, 0, null);
					File f = new File(outputFileName);
					ImageIO.write(bi, "png", f);					
				}
			}
			
			ps.close();

			allps.println(mapClose);
					
		}

		allps.close();
		

		String centresFileName = destinationDirectory + File.separator + "centres.js";

		ps = new PrintStream(centresFileName);

		ps.println( "var centres_of_gravity = [" );

		for( int i = 0; i < materialCount; ++i ) {

			xSumsForMaterial[i] /= pixelCountsForMaterial[i];
			ySumsForMaterial[i] /= pixelCountsForMaterial[i];
			zSumsForMaterial[i] /= pixelCountsForMaterial[i];
			
			ps.print("[ "+((int)xSumsForMaterial[i])+
				 ", "+((int)ySumsForMaterial[i])+
				 ", "+((int)zSumsForMaterial[i])+" ]");
			
			if( i != (materialCount - 1) )
				ps.println(",");
			else
				ps.println("");
		}

		ps.println( "];" );
		ps.close();


	}

	/* This is basically an enhanced version of the method in PNG_Writer. */

	void writeImage(ImagePlus imp, int slice, String path, int transparentColorIndex ) throws Exception {
		int width = imp.getWidth();
		int height = imp.getHeight();
		BufferedImage bi = null;
		if( (imp.getType() == ImagePlus.GRAY8) || (imp.getType() == ImagePlus.COLOR_256)  ) {
			LookUpTable lut = imp.createLut();
			if( (lut != null) && (lut.getMapSize() > 0) ) {
				int size = lut.getMapSize();
				byte [] reds = lut.getReds();
				byte [] greens = lut.getGreens();
				byte [] blues = lut.getBlues();
				IndexColorModel cm = null;
				if( transparentColorIndex < 0 )
					cm = new IndexColorModel(8,size,reds,greens,blues);
				else
					cm = new IndexColorModel(8,size,reds,greens,blues,transparentColorIndex);
				bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
			} else {
				if( (lut == null) && (imp.getType() == ImagePlus.COLOR_256) ) {
					IJ.error("createLut() returned null for a COLOR_256 image");
					return;
				} 
				bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			}
		} else if( imp.getType() == ImagePlus.COLOR_256 ) {
			LookUpTable lut = imp.createLut();
		} else {
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		Graphics2D g = (Graphics2D)bi.getGraphics();
		ImageStack stack = imp.getStack();
		ImageProcessor imageProcessor = stack.getProcessor(slice+1);
		Image imageToDraw = imageProcessor.createImage();
		g.drawImage(imageToDraw, 0, 0, null);
		File f = new File(path);
		ImageIO.write(bi, "png", f);
	}
}
