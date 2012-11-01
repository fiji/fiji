/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
    You can get a similar effect for a single label (index 11 in this case)
    with a macro like this:
 
	working_directory = "/Users/mark/central-complex-complete-vib-protocol/";
	source_directory = working_directory + "warped_labels/";
	output_directory = working_directory + "fb_labels/";

	File.makeDirectory(output_directory);

	source_filenames = getFileList(source_directory);

	for( i = 0; i < source_filenames.length; ++i ) {

	  f = source_filenames[i];
	  source_filename = source_directory + f;
	  open( source_filename );
	  setThreshold(11, 11);
	  run("Convert to Mask", "  black");
	  output_filename = output_directory + f;
	  print("Writing to: "+output_filename);
	  saveAs("Tiff", output_filename);
	  close();

	}

   However it's slightly more complicated to do this in a macro with multiple
   indices.  Call this plugin with something like:
 
      -eval "run('Just Certain Labels','labels=[9,11] extension=[.warped] source=[/Users/mark/central-complex-complete-vib-protocol/labels-original] output=[/Users/mark/central-complex-complete-vib-protocol/labels/]');"
  
 */

package util;

import amira.AmiraMeshEncoder;
import amira.AmiraParameters;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.plugin.PlugIn;
import java.io.File;

public class Just_Certain_Labels implements PlugIn {

	public void run(String ignore) {
	
		String macroOptions = Macro.getOptions();

		String sourceDirectory = null;
		String outputDirectory = null;

		int[] labelsToKeep = null;

		boolean [] keep = new boolean[256];
		
		if (macroOptions == null) {
			IJ.error("Currently this can only be called as macro with options.");
			return;
		}

		sourceDirectory = Macro.getValue(macroOptions, "source", null);
		if (sourceDirectory == null) {
			IJ.error("No source directory specified. (Macro option 'source'.)");
			return;
		}

		File sourceDirectoryAsFile = new File(sourceDirectory);
		if (!(sourceDirectoryAsFile.exists() && sourceDirectoryAsFile.isDirectory())) {
			IJ.error("The source (" + sourceDirectory + ") must both exist and be a directory. ");
			return;
		}


		outputDirectory = Macro.getValue(macroOptions, "output", null);
		if (outputDirectory == null) {
			IJ.error("No output directory specified. (Macro option 'output'.)");
			return;
		}

		File outputDirectoryAsFile = new File(outputDirectory);
		if (!(outputDirectoryAsFile.exists() && outputDirectoryAsFile.isDirectory())) {
			IJ.error("The output (" + outputDirectory + ") must both exist and be a directory. ");
			return;
		}

		String extension = Macro.getValue(macroOptions, "extension", null);

		String labelsToKeepString = Macro.getValue(macroOptions, "labels", null);
		if (labelsToKeepString == null) {
			IJ.error("No labels to keep specified. (Macro option 'labels'.)");
			return;
		}

		String[] labelIndicesAsStrings = labelsToKeepString.split(",");

		labelsToKeep = new int[labelIndicesAsStrings.length];

		String currentString = null;

		for (int i = 0; i < labelIndicesAsStrings.length; ++i) {

			currentString = labelIndicesAsStrings[i];
			try {
				labelsToKeep[i] = Integer.parseInt(currentString);
			} catch (NumberFormatException e) {
				IJ.error("Couldn't parse '" + currentString + "' as a label index.");
				return;
			}
		}
		
		for( int i = 0; i < labelsToKeep.length; ++i ) {
			keep[labelsToKeep[i]] = true;
		}
		
		// Now we should be OK to start:
		LabelFilenameFilter filter = new LabelFilenameFilter(extension);
		
		File [] possibleSourceFiles = sourceDirectoryAsFile.listFiles(filter);
		
		for( int i = 0; i < possibleSourceFiles.length; ++i ) {
			
			File currentFile = possibleSourceFiles[i];
			
			String path = currentFile.getAbsolutePath();
			String leafName = currentFile.getName();
			
			System.out.println("Opening file: "+path);
			System.out.println("Leaf name is: "+leafName);
			
			ImagePlus [] channels = BatchOpener.open(path);
			if( channels == null ) {
				IJ.error("Opening '"+path+"' failed.");
				return;
			}
			
			if( channels.length != 1 ) {
				IJ.error("The labels file must only have one channel: there are "+channels.length+" in '"+path);
				return;
			}
			
			ImagePlus labelsImagePlus = channels[0];
			
			int type = labelsImagePlus.getType();
			
			if( ! (type == ImagePlus.GRAY8 || type == ImagePlus.COLOR_256) ) {
				IJ.error("Something's wrong: '"+path+"' doesn't seem to be an 8 bit file.");
				return;
			}
			
			if( ! AmiraParameters.isAmiraLabelfield(labelsImagePlus) ) {
				IJ.error("The file '"+path+"' isn't an Amira labelfield!");
				return;
			}
			
			
			int width = labelsImagePlus.getWidth();
			int height = labelsImagePlus.getHeight();
			int depth = labelsImagePlus.getStackSize();
			
			ImageStack stack = labelsImagePlus.getStack();
			
			for( int z = 0; z < depth; ++z ) {
				byte [] pixels = (byte[]) stack.getPixels(z+1);
				for( int p = 0; p < width * height; ++p ) {
					int value = pixels[p] & 0xFF;
					if( ! keep[value] )
						pixels[p] = 0;
				}
			}
			
			// labelsImagePlus.show();
			
			String outputFileName = outputDirectoryAsFile.getAbsolutePath() + File.separator + leafName;

			System.out.println("Would output to: "+outputFileName);
			
			AmiraMeshEncoder e=new AmiraMeshEncoder(outputFileName);

			if( ! e.open() ) {
				IJ.error("Could not write "+outputFileName);
				return;
			}

			if( ! e.write(labelsImagePlus) ) {
				IJ.error("Error writing "+outputFileName);
				return;
			}
			
			labelsImagePlus.close();
			
		}
	}

}
