package CLI;
/**
 *
 * Command Line Interface plugin for ImageJ(C).
 * Copyright (C) 2004 Albert Cardona.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 *
 * You may contact Albert Cardona at albert at pensament.net, and at http://www.lallum.org/java/index.html
 *
 * **/


/* VERSION: 1.01
 * RELEASE DATE: 2004-10-15
 * AUTHOR: Albert Cardona at albert at pensament.net
 */

/*
 *	This class adapted from Open_as_Stack.java ImageJ plugin by Albert Cardona.
 */


import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import ij.ImagePlus;
import ij.io.Opener;
import ij.ImageStack;
import ij.IJ;

import java.io.File;

public class OpenDirectory {

	static public final int STACK = 0;
	static public final int MONTAGE = 1;

	String message = null;
	
	OpenDirectory(String dir_path, int option) {
			switch(option) {
				case STACK: openAsStack(dir_path); break;
				case MONTAGE: openAsMontage(dir_path); break;
				default: openAsStack(dir_path); break;
			}
	}

	String getMessage() {
		return message;
	}

	void openAsMontage(String dir_path) {
		//TODO
	}

	void openAsStack(String dir_path) {
			//
			//open all files from dir into a stack:
			//
			File dir = new File(dir_path);
			if (!(dir.exists() && dir.isDirectory())) {
				//end here!
				message = "No such directory.";
				return;
			}
			String[] images = dir.list(new ImageFileFilter("*"));
			if (0 == images.length) {
				//end here!
				message = "No image files in " + dir.getName();
				return;
			}
			Opener opener = new Opener();
			
			ImagePlus[] all_images = null;
			int[] all_width = null;
			int[] all_height = null;
			
			try {
			
				all_images = new ImagePlus[images.length];
				all_width = new int[images.length];
				all_height = new int[images.length];
			
			}catch(OutOfMemoryError o1) {
				IJ.showMessage("Not enough memory to allocate stack.");
				all_images = null;
				all_width = null;
				all_height = null;
				return;
			}
			
			try {

			for (int j=0; j<images.length; j++) {
			    all_images[j] = opener.openImage(dir_path, images[j]);
			    all_width[j] = all_images[j].getWidth();
			    all_height[j] = all_images[j].getHeight();
			}

			}catch(OutOfMemoryError o2) {
				IJ.showMessage("Not enough memory to open all.");
				all_images = null;
				all_width = null;
				all_height = null;
				return;
			}
			
			int largest_width = 0;
			int largest_height = 0;  //initializing variables
			
			//find largest width and heigth
			for (int k=0; k<images.length; k++) {
			    if (all_width[k] > largest_width) largest_width = all_width[k];
			    if (all_height[k] > largest_height) largest_height = all_height[k];
			}
			
			//stack to show
			ImageStack istack = new ImageStack(largest_width, largest_height);
			
			//fill in the stack
			for (int i=0; i<images.length; i++) {
			    	String name = images[i];
				ImageProcessor ipr = all_images[i].getProcessor();
				int type = all_images[i].getType();
				  
				if (type != ImagePlus.COLOR_RGB) {
					TypeConverter tp = new TypeConverter(ipr, false);
					ipr = tp.convertToRGB();
				}
					    
				ImageProcessor ipr2 = ipr.createProcessor(largest_width, largest_height);
				ipr2.invert();
				ipr2.insert(ipr, largest_width/2 - all_width[i]/2, largest_height/2 - all_height[i]/2);
				istack.addSlice(name,ipr2);
			}
			
			ImagePlus stack = new ImagePlus(dir.getName() + " images", istack);
			stack.show();

			message = all_images.length + " images opened as a stack.";
	}
}
