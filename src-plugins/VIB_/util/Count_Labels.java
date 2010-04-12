/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Call with, e.g.
 
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

public class Count_Labels implements PlugIn {

	public void run(String ignore) {
	
		String macroOptions = Macro.getOptions();

		String sourceDirectory = null;
		
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

		// Now we should be OK to start:
		LabelFilenameFilter filter = new LabelFilenameFilter(null);
		
		File [] possibleSourceFiles = sourceDirectoryAsFile.listFiles(filter);
		
		for( int i = 0; i < possibleSourceFiles.length; ++i ) {

			boolean [] someOfThisRegion = new boolean[256];
			
			File currentFile = possibleSourceFiles[i];
			
			String path = currentFile.getAbsolutePath();
			String leafName = currentFile.getName();
			
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
					someOfThisRegion[value] = true;
				}
			}
			
			int count = 0;
			for( int m = 0; m < 256; ++m ) {
				if( someOfThisRegion[m] )
					++count;
			}
			
			System.out.println(""+count+" - "+leafName);
			
			labelsImagePlus.close();
			
		}
	}

}
