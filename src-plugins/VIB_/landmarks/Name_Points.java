/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */
/* Copyright 2008, 2009 Gregory Jefferis */

/*
  This file is part of the ImageJ plugin "Name Landmarks and Register".

  The ImageJ plugin "Name Landmarks and Register" is free software;
  you can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software Foundation;
  either version 3 of the License, or (at your option) any later
  version.

  The ImageJ plugin "Name Landmarks and Register" is distributed in
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

package landmarks;

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.text.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.*;
import java.text.DecimalFormat;

import client.ArchiveClient;
import ij.measure.Calibration;
import pal.math.MultivariateFunction;
import stacks.ThreePaneCrop;
import util.BatchOpener;
import util.Penalty;
import vib.FastMatrix;
import vib.oldregistration.RegistrationAlgorithm;
import util.Overlay_Registered;

public class Name_Points extends NamePoints implements PlugIn {
	public void run( String arguments ) {

		boolean promptForTemplate = IJ.altKeyDown();

		String macroOptions=Macro.getOptions();
		String templateParameter = null;
		if( macroOptions != null ) {
			templateParameter = Macro.getValue(macroOptions,"template",null);
			String actionParameter = Macro.getValue(macroOptions,"action",null);
			if( actionParameter != null ) {
				if( actionParameter.equals( "finetunetofile" ) ) {
					/* This is the only supported action at the moment.
					   You must also supply the options:
					      - inputimage (name of the imagefile)
					      - templateimage (name of the templatefile)
					      - outputpointsfile (filename for the fine-tuned pointset)
					 */
					batchFineTuning = true;
					String inputImageParameter = Macro.getValue(macroOptions,"inputimage",null);
					if( inputImageParameter == null ) {
						IJ.error("You must supply an 'inputimage' parameter when using the macro action 'finetunetofile'");
						return;
					}
					String templateImageParameter = Macro.getValue(macroOptions,"templateimage",null);
					if( templateImageParameter == null ) {
						IJ.error("You must supply an 'templateimage' parameter when using the macro action 'finetunetofile'");
						return;
					}
					String outputPointsFileParameter = Macro.getValue(macroOptions,"outputpointsfile",null);
					if( outputPointsFileParameter == null ) {
						IJ.error("You must supply an 'outputpointsfile' parameter when using the macro action 'finetunetofile'");
						return;
					}

					batchFineTune( templateImageParameter, inputImageParameter, outputPointsFileParameter );
					return;

				} else {
					IJ.error("Unknown macro action '"+actionParameter+"'");
					return;
				}
			}
		}


		File templateImageFile = null;
		if( templateImageFilename != null && templateImageFilename.length() > 0 )
			templateImageFile = new File(templateImageFilename);

		if (promptForTemplate) {
			OpenDialog od;
			String openTitle = "Select template image file...";
			if( templateImageFile == null )
				od = new OpenDialog( openTitle, null );
			else
				od = new OpenDialog( openTitle, templateImageFile.getParent(), templateImageFile.getName() );
			if( od.getFileName() != null ) {
				templateImageFilename=od.getDirectory()+od.getFileName();
				useTemplate( templateImageFilename );
				setDefaultTemplate( templateImageFilename );
			}
		} else if( templateImageFilename != null && templateImageFilename.length() > 0 ) {
			if( templateImageFile.exists() ) {
				useTemplate( templateImageFilename );
			} else {
				IJ.error( "The default template file ('" + templateImageFilename + "') did not exist.");
			}
		}

		Applet applet = IJ.getApplet();
		if( applet != null ) {
			archiveClient=new ArchiveClient( applet );
		}

		if( archiveClient != null ) {

			// We go for a channel that's tagged 'nc82'

			Hashtable<String,String> parameters = new Hashtable<String,String>();
			parameters.put("method","channel-tags");
			parameters.put("md5sum",archiveClient.getValue("md5sum"));

			ArrayList< String [] > tsv_results = archiveClient.synchronousRequest(parameters,null);
			int tags = Integer.parseInt((tsv_results.get(0))[1]); // FIXME error checking
			int nc82_channel = -1;
			for( int i = 0; i < tags; ++i ) {
				String [] row = tsv_results.get(i);
				if( "nc82".equals(row[1]) ) {
					nc82_channel = Integer.parseInt(row[0]);
					break;
				}
			}
			if( nc82_channel < 0 ) {

				imp = IJ.getImage();

				if(imp == null) {
					IJ.error("There's no image to annotate.");
					return;
				}

			} else {

				// Look for the one with the right name...
				String lookFor = "Ch"+(nc82_channel+1);

				int[] wList = WindowManager.getIDList();
				if (wList==null) {
					IJ.error("NamePoints: no images have been loaded");
					return;
				}

				for (int i=0; i<wList.length; i++) {
					ImagePlus tmpImp = WindowManager.getImage(wList[i]);
					String title = tmpImp!=null?tmpImp.getTitle():"";
					int indexOfChannel = title.indexOf(lookFor);
					if( indexOfChannel < 0 ) {
						tmpImp.close();
					}
				}

				imp = IJ.getImage();

				if(imp == null) {
					IJ.error("There's no image to annotate.");
					return;
				}

			}

		} else {

			imp = IJ.getImage();

			if(imp == null) {
				IJ.error("There's no image to annotate.");
				return;
			}

		}

		Calibration c=imp.getCalibration();
		this.x_spacing=c.pixelWidth;
		this.y_spacing=c.pixelHeight;
		this.z_spacing=c.pixelDepth;

		canvas = imp.getCanvas();

		if( applet == null ) {
			boolean foundExistingPointsFile = loadAtStart();
			if( ! foundExistingPointsFile ) {
				points = new NamedPointSet();
				if( templateImageFile != null && templateImageFile.exists() ) {
					try {
						templatePoints = NamedPointSet.forImage( templateImageFilename );
					} catch( NamedPointSet.PointsFileException e )  {
						IJ.error("Couldn't load points file for template image.  The error was: " + e );
					}
				}
				if( templatePoints == null ) {
					points.addNewPoint();
				} else {
					String [] templatePointNames = templatePoints.getPointNames();
					for( String name : templatePointNames )
						points.add( new NamedPointWorld(name) );
				}
			}
		}

		boolean loadedTemplate = false;

		if( (templateParameter != null) && useTemplate(templateParameter) ) {
			loadedTemplate = true;
		}

		dialog = new PointsDialog( "Marking up: "+imp.getTitle(),
					   archiveClient,
					   loadedTemplate ? templateParameter : null,
					   this );
	}

}
