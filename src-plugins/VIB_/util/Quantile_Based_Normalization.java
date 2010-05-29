/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Quantile Based Normalization".

    The ImageJ plugin "Quantile Based Normalization" is free software;
    you can redistribute it and/or modify it under the terms of the
    GNU General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Quantile Based Normalization" is distributed in
    the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A
    PARTICULAR PURPOSE.  See the GNU General Public License for more
    details.

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License. 

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package util;

import ij.*;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.process.*;
import ij.plugin.*;
import ij.io.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Random;
import vib.app.FileGroup;
import vib.app.gui.FileGroupDialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Panel;
import java.awt.Button;
import java.awt.TextField;
import java.awt.Label;
import java.awt.Checkbox;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ItemListener;
import java.awt.image.ColorModel;
import java.util.regex.Pattern;

public class Quantile_Based_Normalization implements PlugIn, ActionListener, ItemListener {
	
	/* The idea of this normalization is to rank all of the values
	   in an several images, divide each of lists of ranked values
	   up into a number of quantiles and replace each value in
	   each quantile with the mean of that rank across all the
	   images.  If you replace with the rank instead of the mean
	   then you get histogram equalization of all the images. */
	
	/* The only subtlety with the former method is that the mean
	   is not going to be integral, so we randomly replace the
	   values in a quatile with a mix of the two bytes around the
	   mean in such a proportion that the mean will be a close to
	   correct as we can get it. */
	
	public static final String PLUGIN_VERSION = "1.2";
	
	class Replacements {
		
		long [] replacements;
		long totalReplacements;
		int minReplacement = Integer.MAX_VALUE;
		int maxReplacement = Integer.MIN_VALUE;
		Random rng;
		int quantile;
		
		public Replacements(int possibleValues) {
			replacements = new long[possibleValues];
			rng=new Random();
		}
		
		public void addSomeReplacements( long howManyToReplace, int replacement ) {
			if( replacement < minReplacement )
				minReplacement = replacement;
			if( replacement > maxReplacement )
				maxReplacement = replacement;
			replacements[replacement] += howManyToReplace;
			totalReplacements += howManyToReplace;
		}
		
		public int getRandomReplacement() {
			if( totalReplacements == 0 ) {
				return -1;
			}
			
			long index=Math.abs(rng.nextLong()) % totalReplacements;
			
			long replacementsSkipped = 0;
			
			for( int r = minReplacement; r <= maxReplacement; ++r ) {
				
				long indexInThisSlot = index - replacementsSkipped;
				
				if( indexInThisSlot < replacements[r] ) {
					// Then we remove one of these and return
					// the value of r.
					-- replacements[r];
					-- totalReplacements;
					return r;
				} else {
					replacementsSkipped += replacements[r];
				}
			}
			return -1;
		}
		
		@Override
		public String toString() {
			if( totalReplacements == 0 )
				return "No replacements left.";
			else {
				String result = "" + totalReplacements + " replacements left (in";
				for( int i = minReplacement; i <= maxReplacement; ++i ) {
					if( replacements[i] > 0 )
						result += " " + i + " (" + replacements[i] + ")";
				}
				return result;
			}
			
		}
		
	}
	
	TextField outputDirectoryInput;
	Button chooseOutputDirectory;
	
	Checkbox useMaskCheckbox;
	TextField maskFileInput;
	Button chooseMaskButton;	
	
	public void processToDirectory( FileGroup fg,
					String outputDirectory,
					String maskFileName,
					int channelToUse,
					int numberOfQuantiles,
					boolean replaceWithRankInstead,
					boolean rescaleRanks ) {
				
		File o=new File(outputDirectory);
		if( ! o.exists() ) {
			IJ.error("The output directory ('"+outputDirectory+"') doesn't exist.");
			return;
		}
		if( ! o.isDirectory() ) {
			IJ.error("'"+outputDirectory+"' is not a directory");
			return;
		}
		
                boolean [][] inMask = null;
		
                int maskWidth = -1;
                int maskHeight = -1;
                int maskDepth = -1;
		
                long pointsInMask=0;

		if( maskFileName != null ) {
		
			IJ.showStatus("Loading mask file: "+maskFileName);
			ImagePlus [] channels=BatchOpener.open(maskFileName);
			if( channels == null ) {
				IJ.error("Couldn't open the mask file: "+maskFileName);
				return;
			}
			if( channels.length != 1 ) {
				IJ.error("The mask file must have one channel - "+maskFileName+" has "+channels.length);
				return;
			}
			ImagePlus maskImagePlus=channels[0];
			ImageStack maskStack=maskImagePlus.getStack();
			maskWidth=maskImagePlus.getWidth();
			maskHeight=maskImagePlus.getHeight();
			maskDepth=maskImagePlus.getStackSize();
			inMask=new boolean[maskDepth][maskWidth*maskHeight];
			for( int z = 0; z < maskDepth; ++z ) {
				byte [] pixels = (byte[])maskStack.getPixels(z+1);
				for( int y = 0; y < maskHeight; ++y ) {
					for( int x = 0; x < maskWidth; ++x ) {
						if( (pixels[y*maskWidth+x]&0xFF) > 127 ) {
							inMask[z][y*maskWidth+x] = true;
							++pointsInMask;
						}
					}
				}
			}
			maskImagePlus.close();
		}
		
		int n = fg.size();
		if (n < 1) {
			IJ.error("No image files selected");
			return;
		}
		
		
                /* First go through each image building totalling the
                   frequencies of each value. */
		
                long frequencies[][] = new long[n][256];
                long pointsInImage[] = new long[n];
		
		long [][] sumValuesInQuantile = new long[n][numberOfQuantiles];
		long [][] numberOfValuesInQuantile = new long[n][numberOfQuantiles];
		
		for (int b = 0; b < n; ++b) {
			
			File f = fg.get(b);
			String path = f.getAbsolutePath();
			
			ImagePlus [] channels=BatchOpener.open(path);
			
			if( channelToUse >= channels.length ) {
				IJ.error("There is no channel "+channelToUse+" in "+path);
				return;
			}
			
			ImagePlus imagePlus=channels[channelToUse];
			
			int type=imagePlus.getType();
			if( ! ((type == ImagePlus.GRAY8) || (type == ImagePlus.COLOR_256)) ) {
				IJ.error("Error processing '"+path+"': This plugin only works on 8bit (GRAY8 or COLOR_256) images.");
				return;
			}
			
			String freeMemory = IJ.freeMemory();
			System.out.println("free memory is: "+freeMemory);
			
			int width=imagePlus.getWidth();
			int height=imagePlus.getHeight();
			int depth=imagePlus.getStackSize();
			
                        // If we're using a mask they all have to be the right
                        // dimensions.			
			
                        if( (maskFileName != null) &&
				! ((width == maskWidth ) &&
                                   (height == maskHeight) &&
                                   (depth == maskDepth)) ) {
                                IJ.error("The image file "+path+" was not the same dimensions as the mask file");
                                return;
                        }
			
			ImageStack stack=imagePlus.getStack();
			
			IJ.showStatus("Calculating frequencies and quantiles for "+imagePlus.getShortTitle()+" ...");
			
			for( int z=0; z<depth; ++z ) {
				byte [] pixels=(byte[])stack.getPixels(z+1);
				for( int y=0; y<height; ++y )
					for( int x=0; x<width; ++x ) {
                                                if( (maskFileName == null) || inMask[z][y*width+x] ) {
							int value=pixels[y*width+x]&0xFF;
							++frequencies[b][value];
                                                }
					}
			}
			
			pointsInImage[b]= (maskFileName == null) ? width*height*depth : pointsInMask;

			System.out.println("Proportion of points to consider: "+((double)pointsInMask/(width*height*depth)));
			
			for (int q = 0; q < numberOfQuantiles; ++q) {
				
				long [] replacementsInThisQuantile=new long[256];
				
				long indexStartThisQuantile = (int) (q * pointsInImage[b] / numberOfQuantiles);
				long indexStartNextQuantile = (int) (((q + 1) * pointsInImage[b]) / numberOfQuantiles);
				
				long pointsInQuantile = indexStartNextQuantile - indexStartThisQuantile;
				
				// If this is the last quantile, make sure we actually
				// include everything...
				if (q == numberOfQuantiles - 1) {
					indexStartNextQuantile = pointsInImage[b];
				}
				
				// Keep track of the sum of the values
				long cumulativeIncluding = 0;
				long cumulativeBefore = 0;
				
				sumValuesInQuantile[b][q] = 0;
				numberOfValuesInQuantile[b][q] = 0;
				
				for (int value = 0; value < frequencies[b].length; ++value) {
					
					cumulativeIncluding += frequencies[b][value];
					
					if ((cumulativeIncluding < indexStartThisQuantile) || (cumulativeBefore >= indexStartNextQuantile)) {
						
						// Then there's no overlap...
						
					} else {
						
						long startInValues = 0;
						
						if (indexStartThisQuantile > cumulativeBefore) {
							startInValues = indexStartThisQuantile - cumulativeBefore;
						}
						
						// This is the end inclusive...
						long endInValues = frequencies[b][value] - 1;
						
						if (indexStartNextQuantile < cumulativeIncluding) {
							endInValues = (indexStartNextQuantile - cumulativeBefore) - 1;
						}
						long pointsInOverlap = (endInValues - startInValues) + 1;
						// System.out.println("points in overlap: "+pointsInOverlap);
						numberOfValuesInQuantile[b][q] += pointsInOverlap;
						sumValuesInQuantile[b][q] += value * pointsInOverlap;
						// replacementsInThisQuantile[value] = pointsInOverlap;
					}
					
					cumulativeBefore += frequencies[b][value];
				}
				
			}
			
			imagePlus.close();
		}
		
		System.out.println("Now going on to calculate the mean in each quantile.");
		
		// Calculate the mean in each quantile (even if we're
		// not going to use it)...
		
		double [] quantileMeans = new double[numberOfQuantiles];
		
		for( int q = 0; q < numberOfQuantiles; ++q ) {
			long sum = 0;
			long values = 0;
			for( int b = 0; b < n; ++ b ) {
				sum += sumValuesInQuantile[b][q];
				values += numberOfValuesInQuantile[b][q];
			}
			quantileMeans[q] = sum / (double)values;
		}
		
		// Now we go through each image again, remap the
		// values according to the options chosen and write
		// the new image out to the output directory....
		
		for (int b = 0; b < n; ++b) {
			
			File f = fg.get(b);
			String path = f.getAbsolutePath();			
			ImagePlus [] channels=BatchOpener.open(path);
			ImagePlus imagePlus=channels[channelToUse];
			
			String newLeafName;		     
			String leafName = f.getName();
			int dotIndex=leafName.lastIndexOf(".");
			if(dotIndex >= 0) {
				newLeafName = leafName.substring(0,dotIndex) + "-normalized.tif";
			} else {
				newLeafName = leafName + "-normalized";
			}
			
			File outputFile=new File(outputDirectory,newLeafName);
			
			Replacements [] meanReplacements = new Replacements[256];
			for( int value = 0; value < 256; ++value )
				meanReplacements[value] = new Replacements(256);
			
			Replacements [] rankReplacements = new Replacements[256];
			for( int value = 0; value < 256; ++value )
				rankReplacements[value] = new Replacements(numberOfQuantiles);
			
			int width=imagePlus.getWidth();
			int height=imagePlus.getHeight();
			int depth=imagePlus.getStackSize();
			
			ImageStack stack=imagePlus.getStack();
			
			IJ.showStatus("Replacing values in: "+imagePlus.getShortTitle()+" ...");
			
			for (int q = 0; q < numberOfQuantiles; ++q) {
				
				long [] replacementsInThisQuantile=new long[256];
				
				long indexStartThisQuantile = (int) (q * pointsInImage[b] / numberOfQuantiles);
				long indexStartNextQuantile = (int) (((q + 1) * pointsInImage[b]) / numberOfQuantiles);
				
				long pointsInQuantile = indexStartNextQuantile - indexStartThisQuantile;
				
				// If this is the last quantile, make sure we actually
				// include everything...
				if (q == numberOfQuantiles - 1) {
					indexStartNextQuantile = pointsInImage[b];
				}
				
				// Keep track of the sum of the values
				long cumulativeIncluding = 0;
				long cumulativeBefore = 0;
				
				for (int value = 0; value < frequencies[b].length; ++value) {
					
					cumulativeIncluding += frequencies[b][value];
					
					if ((cumulativeIncluding < indexStartThisQuantile) || (cumulativeBefore >= indexStartNextQuantile)) {
						
						// Then there's no overlap...
						
					} else {
						
						long startInValues = 0;
						
						if (indexStartThisQuantile > cumulativeBefore) {
							startInValues = indexStartThisQuantile - cumulativeBefore;
						}
						
						// This is the end inclusive...
						long endInValues = frequencies[b][value] - 1;
						
						if (indexStartNextQuantile < cumulativeIncluding) {
							endInValues = (indexStartNextQuantile - cumulativeBefore) - 1;
						}
						long pointsInOverlap = (endInValues - startInValues) + 1;
						numberOfValuesInQuantile[b][q] += pointsInOverlap;
						sumValuesInQuantile[b][q] += value * pointsInOverlap;
						replacementsInThisQuantile[value] = pointsInOverlap;
					}
					
					cumulativeBefore += frequencies[b][value];
				}
				
				double mean = quantileMeans[q];
				
				int byteLowerThanMean = (int) Math.floor(mean);
				int byteHigherThanMean = (int) Math.ceil(mean);
				
				double proportionLower = Math.ceil(mean) - mean;
				int lowerBytes = (int) Math.round(proportionLower*(indexStartNextQuantile-indexStartThisQuantile));
				int higherBytes = (int) (numberOfValuesInQuantile[b][q] - lowerBytes);
				
				long replacementsAddedAlready = 0;
				
				for( int i = 0; i < 256; ++i ) {
					
					long r = replacementsInThisQuantile[i];
					
					if( r == 0 )
						continue;
					
					long howManyLowerToAdd = 0;
					long howManyHigherToAdd = 0;
					
					if( replacementsAddedAlready >= lowerBytes ) {
						howManyHigherToAdd = r;
					} else if( replacementsAddedAlready + r >= lowerBytes ) {
						howManyLowerToAdd = lowerBytes - replacementsAddedAlready;
						howManyHigherToAdd = r - howManyLowerToAdd;
					} else {
						howManyLowerToAdd = r;
					}
					
					meanReplacements[i].addSomeReplacements(howManyLowerToAdd, byteLowerThanMean);
					meanReplacements[i].addSomeReplacements(howManyHigherToAdd, byteHigherThanMean);
					
					rankReplacements[i].addSomeReplacements(r, q);
					
					replacementsAddedAlready += r;
				}
			}
			
			
			IJ.showProgress(0);
			
			ImageStack newStack = new ImageStack(width,height);
			for( int z = 0; z < depth; ++z ) {
				byte [] oldPixels = (byte[])stack.getPixels(z+1);
				byte [] newPixels = new byte[width*height];
				for( int y = 0; y < height; ++y )
					for( int x = 0; x < width; ++x ) {
                                                if( (maskFileName != null) && ! inMask[z][y*width+x] )
							continue;
						int oldValue = oldPixels[y*width+x]&0xFF;
						int replacement;
						if( replaceWithRankInstead ) {
							replacement = rankReplacements[oldValue].getRandomReplacement();
							if(rescaleRanks)
								replacement = (255*replacement) / (numberOfQuantiles - 1);
						} else {                              
							replacement = meanReplacements[oldValue].getRandomReplacement();
						}
						if( replacement < 0 ) {
							System.out.println("BUG: ran out of replacements for "+oldValue);
							newPixels[y*width+x] = (byte)oldValue;
						} else {
							newPixels[y*width+x] = (byte)replacement;
						}
					}
				ByteProcessor bp=new ByteProcessor(width,height);
				bp.setPixels(newPixels);
				newStack.addSlice("",bp);
				
				IJ.showProgress( z / (double)depth );
				
			}
			
			
			IJ.showProgress(1.0);

			if( ImagePlus.COLOR_256 == imagePlus.getType() ) {
				ColorModel cm = null;
				cm = stack.getColorModel();
				if( cm != null ) {
					newStack.setColorModel( cm );
				}
			}
						
			ImagePlus newImage = new ImagePlus( "normalized "+imagePlus.getTitle(), newStack );
			newImage.setCalibration(imagePlus.getCalibration());
						
			// newImage.show();
			
                        if( outputFile.exists() ) {
                                YesNoCancelDialog yncd=new YesNoCancelDialog(
					IJ.getInstance(),
					"Confirm",
					"The file "+outputFile.getAbsolutePath()+" already exists.  Overwrite it?");
                                if( ! yncd.yesPressed() ) {
					if( yncd.cancelPressed() ) {
						IJ.showStatus("Quantile based normalization cancelled.");
						newImage.close();
						return;
					} else {
						newImage.close();
						continue; // "No" was pressed, so skip writing this file and move on.
					}
                                }
                        }
			
			boolean saved=new FileSaver(newImage).saveAsTiffStack(outputFile.getAbsolutePath());
			if( ! saved )
				return;
			
                        newImage.close();
			imagePlus.close();
			
		}
		
		IJ.showStatus("Normalization complete: files written to: "+outputDirectory);		
		
	}
		
		
	public void run(String ignored) {

		Pattern macOSPattern = Pattern.compile("^Mac ?OS.*$",Pattern.CASE_INSENSITIVE);
		String osName = (String)System.getProperties().get("os.name");
		if( osName != null && macOSPattern.matcher(osName).matches() ) {
			IJ.error("The Quantile Based Normalization plugin "+
				 "is currently disabled on Mac OS due to Bug 29.");
			return;
		}

		/*
		String [] defaultFiles = { "/home/mark/corpus/central-complex/mhl-middle(C)c5(0).lsm",
					   "/home/mark/corpus/central-complex/mhl-71yxUAS-lacZ(0).lsm",
					   "/home/mark/corpus/central-complex/mhl-theotherone(A)c61(0).lsm" };
		*/
		String [] defaultFiles = { };

		// String  defaultOutputDirectory = "/home/mark/tmp-output/";
		String defaultOutputDirectory = "";

		String defaultMaskFileName = "";
		
                GenericDialog gd=new GenericDialog("Quantile Based Normalization (version: "+PLUGIN_VERSION+")");
		
		FileGroup fg = new FileGroup("foo");
		
		for( int i = 0; i < defaultFiles.length; ++i )
			fg.add(defaultFiles[i]);
		
		FileGroupDialog fgd = new FileGroupDialog(fg,false);
		
		gd.addPanel(fgd);
		
		Panel outputDirectoryPanel=new Panel();
		outputDirectoryPanel.add(new Label("Output directory: "));
		outputDirectoryInput = new TextField(defaultOutputDirectory,18);
		outputDirectoryPanel.add(outputDirectoryInput);
		chooseOutputDirectory = new Button("Choose ...");
		outputDirectoryPanel.add(chooseOutputDirectory);
		chooseOutputDirectory.addActionListener(this);
		
		gd.addPanel(outputDirectoryPanel);
		
		Panel useMaskPanel=new Panel();
		useMaskPanel.setLayout(new GridBagLayout());
		GridBagConstraints c=new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;		
		c.anchor = GridBagConstraints.LINE_START;
		useMaskCheckbox = new Checkbox("Use an image mask?");
                useMaskCheckbox.addItemListener(this);
		useMaskPanel.add(useMaskCheckbox,c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;		
		useMaskPanel.add(new Label("(If you use a mask, all images must be the same dimensions."),c);		
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		useMaskPanel.add(new Label("Mask file: "),c);		
		c.gridx = 1;
		maskFileInput = new TextField(defaultMaskFileName,18);
                maskFileInput.setEnabled(false);
		useMaskPanel.add(maskFileInput,c);
		c.gridx = 2;
		chooseMaskButton = new Button("Choose...");
                chooseMaskButton.setEnabled(false);
		useMaskPanel.add(chooseMaskButton,c);
		
                gd.addPanel(useMaskPanel);
		
		gd.addNumericField("Number of channel to use (starting at 1): ", 1, 0);
                gd.addNumericField("Quantiles", 256, 0);
                String [] choices={ "mean", "rank"};
                gd.addChoice("Replace each quantile with", choices, "mean");
                gd.addCheckbox("Rescale (if replacing with ranks)", true);
		
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		
                // First find the output directory...
		
		String outputDirectory=outputDirectoryInput.getText();
		
                // Now whether there's a mask file, and if so then load it.		
		
                boolean useMask=useMaskCheckbox.getState();
		
		String maskFileName = null;
		
                if( useMask ) {
			maskFileName=maskFileInput.getText();
                }
		
		int channelToUse = (int) gd.getNextNumber();
                // ImageJ consistently 1-indexes things in its interface,
                // so turn this into a zero-indexed channel number.
		-- channelToUse;
				
                int numberOfQuantiles = (int)gd.getNextNumber();
                if( numberOfQuantiles < 1 || numberOfQuantiles > 256 ) {
			IJ.error("Number of quantiles must be between 1 and 256 inclusive.");
			return;
                }
		
                boolean replaceWithRankInstead=false;
                String choice=gd.getNextChoice();
                replaceWithRankInstead = choice.equals("rank");		
		
                boolean rescaleRanks=gd.getNextBoolean();

		processToDirectory( fg,
				    outputDirectory,
				    maskFileName,
				    channelToUse,
				    numberOfQuantiles,
				    replaceWithRankInstead,
				    rescaleRanks );
		
		
	}	
	
	public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
		if( source == chooseOutputDirectory ) {
			DirectoryChooser dc=new DirectoryChooser("Choose output directory...");
			String directory = dc.getDirectory();
			if (directory != null) {
				outputDirectoryInput.setText(directory);
			}
			
       		} else if( source == chooseMaskButton ) {
			
			OpenDialog od;
			
			od = new OpenDialog("Select mask image file...",
					    null,
					    null );
			
			String fileName = od.getFileName();
			String directory = od.getDirectory();
			
			if( fileName == null ) {
				return;
			}
			
			String fullFileName=directory+fileName;
			
                        // Just do a quick check that this file really exists:
			
                        File maskFile=new File(fullFileName);
			if( maskFile.exists() ) {
				maskFileInput.setText(fullFileName);
                        } else {
				IJ.error("The chosen mask file ("+fullFileName+") doesn't exist.");
                        }
                }
	}
	
	public void itemStateChanged(ItemEvent e) {
                if( e.getSource() == useMaskCheckbox ) {
			boolean useMask = useMaskCheckbox.getState();
			maskFileInput.setEnabled(useMask);
			chooseMaskButton.setEnabled(useMask);
                }
	}
}
