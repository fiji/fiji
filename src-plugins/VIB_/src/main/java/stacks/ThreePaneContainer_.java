/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package stacks;

import ij.ImagePlus;
import ij.WindowManager;
import ij.IJ;
import ij.plugin.PlugIn;

public class ThreePaneContainer_ implements PlugIn {

    ThreePaneContainer threePaneContainer;

	public void run(String foo) {
        		
		ImagePlus currentImage = WindowManager.getCurrentImage();

		if( currentImage == null ) {
			IJ.error( "There's no current image to crop." );
			return;
		}

		if( currentImage.getStackSize() <= 1 ) {
			IJ.error( "This plugin is only for image stacks of more than one slice." );
			return;
		}

		if( currentImage.getType() != ImagePlus.GRAY8 ) {
			IJ.error("This plugin only works on 8 bit images at the moment.");
			return;
		}

		threePaneContainer = new ThreePaneContainer( currentImage );

    }

}
