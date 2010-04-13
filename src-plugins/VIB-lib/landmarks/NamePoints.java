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
import util.OverlayRegistered;

class PointsDialog extends Dialog implements ActionListener, WindowListener {

	Label[] coordinateLabels;

	Button[] markButtons;
	Button[] showButtons;
	Button[] resetButtons;
	Button[] fineTuneButtons;
	Button[] renameButtons;
	Button[] deleteButtons;

	Button registerRigid;
	Button registerAffine;
	Button registerBookstein;

	HashMap< Button, Integer > buttonToAction;
	HashMap< Button, Integer > buttonToIndex;

	static final int MARK = 1;
	static final int SHOW = 2;
	static final int RESET = 3;
	static final int FINE_TUNE = 4;
	static final int RENAME = 5;
	static final int DELETE = 6;

	Label instructions;
	Panel pointsPanel;
	Panel buttonsPanel;
	Panel templatePanel;
	Panel registrationPanel;
	Checkbox overlayResult;

	NamePoints plugin;

	ArchiveClient archiveClient;

	Label templateFileName;
	Button chooseTemplate;
	Button setAsDefaultTemplate;
	Button clearTemplate;

	String defaultInstructions = "Mark the current point selection as:";

	public void recreatePointsPanel() {
		// Alias this for convenience:
		NamedPointSet points = plugin.points;
		// Remove all the action listeners:
		for( Component c : pointsPanel.getComponents() ) {
			if( c instanceof Button )
				((Button)c).removeActionListener(this);
		}
		// Remove all of them:
		pointsPanel.removeAll();
		// Make sure the arrays are the right size:
		coordinateLabels = new Label[points.size()];
		markButtons = new Button[points.size()];
		showButtons = new Button[points.size()];
		resetButtons = new Button[points.size()];
		fineTuneButtons = new Button[points.size()];
		renameButtons = new Button[points.size()];
		deleteButtons = new Button[points.size()];
		buttonToIndex = new HashMap< Button, Integer >();
		buttonToAction = new HashMap< Button, Integer >();

		// Now add everything again:
		pointsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		Button b;
		int counter = 0;
		Iterator<NamedPointWorld> i;
		for (i=points.listIterator();i.hasNext();) {
			NamedPointWorld p = i.next();

			c.gridx = 0;
			c.gridy = counter;
			c.anchor = GridBagConstraints.LINE_END;
			markButtons[counter] = b = new Button(p.getName());
			b.addActionListener(this);
			buttonToIndex.put( b, counter );
			buttonToAction.put( b, MARK );
			pointsPanel.add( b, c );

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridx;
			coordinateLabels[counter] = new Label("<unset>");
			pointsPanel.add( coordinateLabels[counter], c );

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridx;
			showButtons[counter] = b = new Button("Show");
			b.addActionListener(this);
			b.setEnabled(false);
			buttonToIndex.put( b, counter );
			buttonToAction.put( b, SHOW );
			pointsPanel.add( b, c );

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridx;
			resetButtons[counter] = b = new Button("Reset");
			b.addActionListener(this);
			b.setEnabled(false);
			buttonToIndex.put( b, counter );
			buttonToAction.put( b, RESET );
			pointsPanel.add( b, c );

			if( NamePoints.offerFineTuning ) {
				c.anchor = GridBagConstraints.LINE_START;
				++ c.gridx;
				fineTuneButtons[counter] = b = new Button("Fine Tune");
				b.addActionListener(this);
				b.setEnabled(true);
				buttonToIndex.put( b, counter );
				buttonToAction.put( b, FINE_TUNE );
				pointsPanel.add( b, c );
			}

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridx;
			renameButtons[counter] = b = new Button("Rename");
			b.addActionListener(this);
			b.setEnabled(true);
			buttonToIndex.put( b, counter );
			buttonToAction.put( b, RENAME );
			pointsPanel.add( b, c );

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridx;
			deleteButtons[counter] = b = new Button("Delete");
			b.addActionListener(this);
			b.setEnabled(true);
			buttonToIndex.put( b, counter );
			buttonToAction.put( b, DELETE );
			pointsPanel.add( b, c );

			if (p.set)
				setCoordinateLabel(counter,
						   p.x,
						   p.y,
						   p.z);
			++counter;
		}
	}

	public PointsDialog(String title,
			    ArchiveClient archiveClient,
			    String loadedTemplateFilename,
			    NamePoints plugin) {

		super(IJ.getInstance(),title,false);

		addWindowListener( this );

		this.plugin = plugin;
		this.archiveClient = archiveClient;

		setLayout(new GridBagLayout());

		GridBagConstraints outerc=new GridBagConstraints();

		Panel instructionsPanel = new Panel();
		pointsPanel = new Panel();
		buttonsPanel = new Panel();

		instructions = new Label( defaultInstructions );
		instructionsPanel.setLayout( new BorderLayout() );
		instructionsPanel.add(instructions,BorderLayout.WEST);

		outerc.gridx = 0;
		++ outerc.gridy;
		outerc.anchor = GridBagConstraints.LINE_START;
		outerc.fill = GridBagConstraints.HORIZONTAL;
		add(instructionsPanel,outerc);

		recreatePointsPanel();

		++ outerc.gridy;
		outerc.anchor = GridBagConstraints.CENTER;
		add(pointsPanel,outerc);

		// Leave some space below:
		++ outerc.gridy;
		add(new Label(""),outerc);

		addButton = new Button("Add New Point");
		addButton.addActionListener(this);
		buttonsPanel.add(addButton);

		if( archiveClient == null ) {

			saveButton = new Button("Save");
			saveButton.addActionListener(this);
			loadButton = new Button("Load");
			loadButton.addActionListener(this);
			igsSaveButton = new Button("Export to IGS");
			igsSaveButton.addActionListener(this);
			resetButton = new Button("Reset All");
			resetButton.addActionListener(this);
			closeButton = new Button("Close");
			closeButton.addActionListener(this);

			buttonsPanel.add(saveButton);
			buttonsPanel.add(loadButton);
			buttonsPanel.add(igsSaveButton);
			buttonsPanel.add(resetButton);
			buttonsPanel.add(closeButton);

		} else {

			getMyButton = new Button("Get My Most Recent Annotation");
			getMyButton.addActionListener(this);
			getAnyButton = new Button("Get Most Recent Annotation");
			getAnyButton.addActionListener(this);
			uploadButton = new Button("Upload");
			uploadButton.addActionListener(this);

			buttonsPanel.add(getMyButton);
			buttonsPanel.add(getAnyButton);
			buttonsPanel.add(uploadButton);

		}

		++ outerc.gridy;
		add(buttonsPanel,outerc);

		templatePanel=new Panel();
		templatePanel.add(new Label("Template File:"));
		if( plugin.templateImageFilename == null || plugin.templateImageFilename.length() == 0 )
			templateFileName = new Label("[None chosen]");
		else
			templateFileName = new Label(plugin.templateImageFilename);
		if( loadedTemplateFilename != null )
			templateFileName.setText(loadedTemplateFilename);
		templatePanel.add(templateFileName);
		chooseTemplate = new Button("Choose");
		chooseTemplate.addActionListener(this);
		templatePanel.add(chooseTemplate);

		setAsDefaultTemplate = new Button("Set As Default");
		setAsDefaultTemplate.addActionListener(this);
		templatePanel.add(setAsDefaultTemplate);

		clearTemplate = new Button("Clear Template");
		clearTemplate.addActionListener(this);
		templatePanel.add(clearTemplate);

		++ outerc.gridy;
		outerc.anchor = GridBagConstraints.LINE_START;
		outerc.fill = GridBagConstraints.NONE;
		add(templatePanel,outerc);

		registrationPanel = new Panel();
		registrationPanel.setLayout( new GridBagLayout() );
		GridBagConstraints rc = new GridBagConstraints();
		rc.gridx = 0;
		rc.gridy = 0;
		registrationPanel.add( new Label("Register to template based on common points:"), rc );
		registerRigid = new Button( "Best Rigid Registration" );
		registerAffine = new Button( "Affine Registration From Best 4 Points" );
		registerBookstein = new Button( "Thin-Plate Spline Registration" );
		++ rc.gridx;
		registrationPanel.add( registerRigid, rc );
		registerRigid.addActionListener( this );
		++ rc.gridx;
		registrationPanel.add( registerAffine, rc );
		registerAffine.addActionListener( this );
		++ rc.gridx;
		registrationPanel.add( registerBookstein, rc );
		registerBookstein.addActionListener( this );

		overlayResult = new Checkbox("Overlay result");
		rc.gridx = 0;
		rc.gridy = 1;
		registrationPanel.add( overlayResult, rc );

		++ outerc.gridy;
		add(registrationPanel,outerc);

		pack();
		setVisible(true);
	}

	Button saveButton;
	Button loadButton;
	Button igsSaveButton;
	Button resetButton;
	Button closeButton;

	Button getMyButton;
	Button getAnyButton;
	Button uploadButton;

	Button addButton;

	public void reset(int i) {
		assert i>0;
		assert i<coordinateLabels.length;
		coordinateLabels[i].setText("<unset>");
		showButtons[i].setEnabled(false);
		resetButtons[i].setEnabled(false);
		pack();
	}

	public void setCoordinateLabel(int i, double x, double y, double z) {
		DecimalFormat f = new DecimalFormat("0.000");
		String newText = "";
		newText += "x: " + f.format(x) + ", y: " + f.format(y) + ", z: " + f.format(z);
		coordinateLabels[i].setText(newText);
		showButtons[i].setEnabled(true);
		resetButtons[i].setEnabled(true);
	}

	public void setFineTuning( boolean busy ) {
		if( busy ) {
			instructions.setText("Fine tuning... (may take some time)");
			pointsPanel.setEnabled(false);
			buttonsPanel.setEnabled(false);
			templatePanel.setEnabled(false);
			registrationPanel.setEnabled(false);
		} else {
			instructions.setText(defaultInstructions);
			pointsPanel.setEnabled(true);
			buttonsPanel.setEnabled(true);
			templatePanel.setEnabled(true);
			registrationPanel.setEnabled(true);
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Integer index = buttonToIndex.get( source );
		if( index != null ) {
			int i = index.intValue();
			int action = buttonToAction.get( source );
			switch (action) {
			case MARK:
				plugin.mark(i);
				return;
			case SHOW:
				plugin.show(i);
				return;
			case RESET:
				plugin.reset(i);
				return;
			case FINE_TUNE:
				plugin.fineTune(i);
				return;
			case RENAME:
				plugin.rename(i);
				return;
			case DELETE:
				plugin.delete(i);
				return;
			}
		}
		if(source == addButton) {
			plugin.addNewPoint();
		} else if (source == closeButton) {
			closeSafely();
		} else if (source == saveButton) {
			plugin.save(".points");
		} else if (source == loadButton) {
			plugin.load();
		} else if (source == igsSaveButton) {
			plugin.save(".landmarks");
		} else if (source == resetButton) {
			plugin.reset();
		} else if (source == uploadButton) {
			plugin.upload();
/*
		} else if (source == getMyButton ) {
			plugin.get( true );
		} else if (source == getAnyButton ) {
			plugin.get( false );
*/
		} else if (source == chooseTemplate ) {
			OpenDialog od;
			String openTitle = "Select template image file...";
			File templateImageFile = null;
			if( plugin.templateImageFilename != null && plugin.templateImageFilename.length() > 0 )
				templateImageFile = new File( plugin.templateImageFilename );
			if( templateImageFile == null )
				od = new OpenDialog( openTitle, null );
			else
				od = new OpenDialog( openTitle, templateImageFile.getParent(), templateImageFile.getName() );
			if( od.getFileName() != null ) {
				String templateImageFilename=od.getDirectory()+od.getFileName();
				if( plugin.useTemplate( templateImageFilename ) ) {
					templateFileName.setText(templateImageFilename);
					pack();
				}
			}
		} else if (source == setAsDefaultTemplate) {
			plugin.setDefaultTemplate();
		} else if (source == clearTemplate) {
			plugin.useTemplate( null );
			templateFileName.setText("[None chosen]");
			pack();
		} else if( source == registerRigid ) {
			plugin.doRegistration(NamePoints.RIGID);
		} else if( source == registerAffine ) {
			plugin.doRegistration(NamePoints.AFFINE);
		} else if( source == registerBookstein ) {
			plugin.doRegistration(NamePoints.BOOKSTEIN);
		}
	}

	public void closeSafely() {
		if( plugin.unsaved ) {
			YesNoCancelDialog d = new YesNoCancelDialog(
				IJ.getInstance(), "Really quit?",
				"There are unsaved changes. Do you really want to quit?" );
			if( ! d.yesPressed() )
				return;
		}
		plugin.stopFineTuneThreads();
		dispose();
	}

	public void windowClosing( WindowEvent e ) {
		System.out.println("Got windowClosing...");
		closeSafely();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }

}

public class NamePoints implements FineTuneProgressListener {

	boolean unsaved = false;

	static final boolean offerFineTuning = false;

	String templateImageFilename=Prefs.get("landmarks.Name_Points.templateImageFilename","");
	ImagePlus templateImage;
	NamedPointSet templatePoints;
	String templateUnits;

	int numberOfFineTuneThreads = 2;

	double x_spacing;
	double y_spacing;
	double z_spacing;

	public void show(int i) {
		points.showAsROI(i, imp);
	}

	ProgressWindow progressWindow;

	void rename(int i) {
		NamedPointWorld npw = points.get(i);
		GenericDialog gd = new GenericDialog( "Rename Point" );
		gd.addStringField( "Rename point to:", npw.getName() );
		gd.showDialog();
		if( gd.wasCanceled() )
			return;
		String newName = gd.getNextString();
		boolean result = points.renamePointTo( i, newName );
		if( result ) {
			dialog.markButtons[i].setLabel( newName );
			dialog.pack();
		} else {
			IJ.error("Couldn't rename point: there already is one called \"" + newName + "\"" );
		}
	}

	void delete(int i) {
		String name = points.get(i).getName();
		YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Really delete?",
							     "Do you really want to delete the point \""+name+"\"?" );
		if( d.yesPressed() ) {
			points.delete(i);
			dialog.recreatePointsPanel();
			dialog.pack();
		}
	}

	void addNewPoint() {
		NamedPointWorld npw = points.addNewPoint();
		dialog.recreatePointsPanel();
		dialog.pack();
	}

	int maxThreads = Runtime.getRuntime().availableProcessors();

	/* To modify the fields down to EOSYNC, synchronize on
	   fineTuneThreadQueue */

	LinkedList< FineTuneThread > fineTuneThreadQueue = new LinkedList< FineTuneThread >();
	LinkedList< FineTuneThread > fineTuneThreadsStarted = new LinkedList< FineTuneThread >();
	boolean fineTuning = false;
	boolean startedAdditionalRefinement = false;
	int indexOfPointBeingFineTuned = -1;
	FineTuneThread finalRefinementThread;
	RegistrationResult bestSoFar;
	int currentlyRunningFineTuneThreads;

	// EOSYNC

	/* We synchronize on latch to get a notification that a fine
	   tune operation has finished when using a macro to fine-tune
	   in batches. */

	Object latch;

	/* Return true if we actually started threads, return false if
	   we didn't - typically because there are already threads
	   running...
	 */

	boolean fineTune( int i ) {

		NamedPointWorld p = points.get(i);
		if (p == null) {
			IJ.error("You must have set a point in order to fine-tune it.");
			return false;
		}

		return fineTune( p );
	}

	boolean fineTune( NamedPointWorld p ) {
		int index = points.getIndexOfPoint( p.getName() );
		return fineTune( p, index );
	}

	boolean fineTune( NamedPointWorld p, int indexOfPoint ) {

		synchronized( fineTuneThreadQueue ) {
			if( fineTuning ) {
				IJ.error( "Already fine-tuning some points" );
				return false;
			} else {
				fineTuning = true;
				indexOfPointBeingFineTuned = indexOfPoint;
				startedAdditionalRefinement = false;
				bestSoFar = null;
				fineTuneThreadQueue.clear();
				fineTuneThreadsStarted.clear();
			}
		}

		String pointName = p.getName();

		if( templatePoints == null ) {
			IJ.error("You must have a template file loaded in order to fine tune.");
			return false;
		}

		NamedPointWorld pointInTemplate = templatePoints.getPoint(pointName);

		if( pointInTemplate == null ) {
			IJ.error("The point you want to fine-tune must be set both in this image and the template.  \""+pointName+"\" is not set in the template.");
			return false;
		}

		/* We need at least 3 points in common between the two
		   point sets for an initial guess: */

		ArrayList<String> namesInCommon = points.namesSharedWith(templatePoints,true);

		System.out.println("namedInCommon are: "+namesInCommon.toString());

		boolean addInitialGuess = (namesInCommon.size() >= 3);
		boolean addAllRotations = true;

		if( dialog != null )
			dialog.setFineTuning(true);

		if( ! loadTemplateImage() )
			return false;

		int templateWidth = templateImage.getWidth();
		int templateHeight = templateImage.getHeight();
		int templateDepth = templateImage.getStackSize();

		// Get a small image from around that point...
		Calibration c = templateImage.getCalibration();

		double x_spacing_template = 1;
		double y_spacing_template = 1;
		double z_spacing_template = 1;
		templateUnits = "pixels";

		if( c != null ) {
			x_spacing_template = c.pixelWidth;
			y_spacing_template = c.pixelHeight;
			z_spacing_template = c.pixelDepth;
			templateUnits = c.getUnits();
		}

		double real_x_template = pointInTemplate.x;
		double real_y_template = pointInTemplate.y;
		double real_z_template = pointInTemplate.z;

		/* Ideally we want a side of 100, but this may extend
		   over one of the edges from this point, so make sure
		   the cube side isn't so large that that happens: */

		int sample_x_template = (int) Math.round( real_x_template / x_spacing_template );
		int sample_y_template = (int) Math.round( real_y_template / y_spacing_template );
		int sample_z_template = (int) Math.round( real_z_template / z_spacing_template );

		int maxSides [] = { 40,
				    sample_x_template * 2,
				    (templateWidth - sample_x_template) * 2,
				    sample_y_template * 2,
				    (templateHeight - sample_y_template) * 2,
				    sample_z_template * 2,
				    (templateDepth - sample_z_template) * 2 };

		int maxCubeSideSamples = Integer.MAX_VALUE;
		for( int maxSide : maxSides ) {
			if( maxSide < maxCubeSideSamples )
				maxCubeSideSamples = maxSide;
		}

		System.out.println("Decided on maxCubeSideSamples: "+maxCubeSideSamples);

		double minimumTemplateSpacing = Math.min( Math.abs(x_spacing_template),
							  Math.min( Math.abs(y_spacing_template), Math.abs(z_spacing_template) ) );

		double templateCubeSide = maxCubeSideSamples * minimumTemplateSpacing;
		System.out.println( "Using cube side in template of "+templateCubeSide+" "+templateUnits );
		System.out.println( "   So template samples in x are: "+(int)( templateCubeSide / x_spacing_template ) );
		System.out.println( "   So template samples in y are: "+(int)( templateCubeSide / y_spacing_template ) );
		System.out.println( "   So template samples in z are: "+(int)( templateCubeSide / z_spacing_template ) );

		double x_min_template = real_x_template - (templateCubeSide / 2);
		double x_max_template = real_x_template + (templateCubeSide / 2);
		double y_min_template = real_y_template - (templateCubeSide / 2);
		double y_max_template = real_y_template + (templateCubeSide / 2);
		double z_min_template = real_z_template - (templateCubeSide / 2);
		double z_max_template = real_z_template + (templateCubeSide / 2);

		int x_min_template_i = (int) Math.round( x_min_template / x_spacing_template );
		int x_max_template_i = (int) Math.round( x_max_template / x_spacing_template );
		int y_min_template_i = (int) Math.round( y_min_template / y_spacing_template );
		int y_max_template_i = (int) Math.round( y_max_template / y_spacing_template );
		int z_min_template_i = (int) Math.round( z_min_template / z_spacing_template );
		int z_max_template_i = (int) Math.round( z_max_template / z_spacing_template );

		ImagePlus cropped = ThreePaneCrop.performCrop(templateImage, x_min_template_i, x_max_template_i, y_min_template_i, y_max_template_i, z_min_template_i, z_max_template_i, false);

		if( ! batchFineTuning ) {
			ImageStack emptyStack = new ImageStack(100,100);
			ColorProcessor emptyCP = new ColorProcessor(100,100);
			emptyCP.setRGB( new byte[100*100], new byte[100*100], new byte[100*100] );
			emptyStack.addSlice("",emptyCP);
			// grrr, add two slices so that the scrollbar gets created:
			emptyStack.addSlice("",emptyCP);

			ImagePlus progressImagePlus = new ImagePlus( "Fine-Tuning Progress", emptyStack );
			ProgressCanvas progressCanvas = new ProgressCanvas( progressImagePlus );

			progressWindow = new ProgressWindow( progressImagePlus, progressCanvas, this );
		}

		fineTuneThreadQueue = new LinkedList< FineTuneThread >();

		/* A quick word about the transformations being
		   optimized here, which are arrays of 6 double
		   values.  These are:

		   	{ z1, x1, z2, tx, ty, tz }

		   This defines a transformation that will take a
		   real-world vector from the template point to
		   another point in the template, and map it to a
		   real-world co-ordinate in the image we're marking
		   up.

		   (FIXME: of course, this makes it harder to
		   understand than if the transformation was straight
		   from a real-world coordinate in the template to a
		   real-world coordinate in the image we're marking
		   up.  I will fix this when I have time.)

		   So, ( z1, x1, x2 ) are Euler angles and ( tx, ty,
		   tz ) are a translation.  First the vector from the
		   template-point is rotated according to the former,
		   and then it is translated with the latter. */

		double [] guessedTransformation = null;

		if( addInitialGuess ) {

			// FIXME: use bestRigid instead

			/* We could try to pick the other points we
			   want to use in a more subtle way, but for
			   the moment just pick the first two which
			   are in common. */

			String [] otherPoints=new String[2];

			int addAtIndex = 0;
			for( Iterator<String> nameIterator = namesInCommon.iterator();
			     nameIterator.hasNext(); ) {

				String otherName = nameIterator.next();
				if (pointName.equals(otherName)) {
					continue;
				}
				otherPoints[addAtIndex++] = otherName;
				if (addAtIndex >= 2) {
					break;
				}
			}

			System.out.println("... calculating vector to: "+otherPoints[0]);
			System.out.println("... and: "+otherPoints[1]);

			NamedPointWorld inThis1=points.getPoint(otherPoints[0]);
			NamedPointWorld inThis2=points.getPoint(otherPoints[1]);

			NamedPointWorld inTemplate1=templatePoints.getPoint(otherPoints[0]);
			NamedPointWorld inTemplate2=templatePoints.getPoint(otherPoints[1]);

			double [] inThisTo1 = new double[3];
			double [] inThisTo2 = new double[3];

			double [] inTemplateTo1 = new double[3];
			double [] inTemplateTo2 = new double[3];

			inThisTo1[0] = inThis1.x - p.x;
			inThisTo1[1] = inThis1.y - p.y;
			inThisTo1[2] = inThis1.z - p.z;

			inThisTo2[0] = inThis2.x - p.x;
			inThisTo2[1] = inThis2.y - p.y;
			inThisTo2[2] = inThis2.z - p.z;

			inTemplateTo1[0] = inTemplate1.x - pointInTemplate.x;
			inTemplateTo1[1] = inTemplate1.y - pointInTemplate.y;
			inTemplateTo1[2] = inTemplate1.z - pointInTemplate.z;

			inTemplateTo2[0] = inTemplate2.x - pointInTemplate.x;
			inTemplateTo2[1] = inTemplate2.y - pointInTemplate.y;
			inTemplateTo2[2] = inTemplate2.z - pointInTemplate.z;

			FastMatrix r=FastMatrix.rotateToAlignVectors(inTemplateTo1, inTemplateTo2, inThisTo1, inThisTo2);

			guessedTransformation=new double[6];
			r.guessEulerParameters(guessedTransformation);

			System.out.println("guessed euler 0 degrees: "+((180*guessedTransformation[0])/Math.PI));
			System.out.println("guessed euler 1 degrees: "+((180*guessedTransformation[1])/Math.PI));
			System.out.println("guessed euler 2 degrees: "+((180*guessedTransformation[2])/Math.PI));

			System.out.println("my inferred r is: "+r);

			FastMatrix rAnotherWay = FastMatrix.rotateEuler(guessedTransformation[0],
									guessedTransformation[1],
									guessedTransformation[2]);

			System.out.println("another r is:   "+rAnotherWay);


			double [] initialValues = { guessedTransformation[0],
						    guessedTransformation[1],
						    guessedTransformation[2],
						    p.x,
						    p.y,
						    p.z };

			FineTuneThread fineTuneThread = new FineTuneThread(
				CORRELATION,
				templateCubeSide,
				cropped,
				templateImage,
				pointInTemplate,
				imp,
				p,
				initialValues,
				guessedTransformation,
				progressWindow,
				this);

			fineTuneThreadQueue.addLast( fineTuneThread );
		}

		/* We want to generate all possible rigid rotations
		   of one axis onto another.  So, the x axis can be
		   mapped on to one of 6 axes.  Then the y axis can
		   be mapped on to one of 4 axes.  The z axis can
		   then only be mapped onto 1 axis if the handedness
		   is to be preserved.

		   As a special case, if guessedTransformation is supplied then
		   we try that first.
		*/

		for( int rotation = 0; rotation < 24; ++rotation ) {

			double [] initialValues = new double[6];

			int firstAxis = rotation / 8;
			int firstAxisParity = 2 * ((rotation / 4) % 2) - 1;
			int secondAxisInformation = rotation % 4;
			int secondAxisIncrement = 1 + (secondAxisInformation / 2);
			int secondAxisParity = 2 * (secondAxisInformation % 2) - 1;
			int secondAxis = (firstAxis + secondAxisIncrement) % 3;

			double [] xAxisMappedTo = new double[3];
			double [] yAxisMappedTo = new double[3];

			xAxisMappedTo[firstAxis] = firstAxisParity;
			yAxisMappedTo[secondAxis] = secondAxisParity;

			double [] zAxisMappedTo = FastMatrix.crossProduct( xAxisMappedTo, yAxisMappedTo );

			System.out.println("x axis mapped to: "+xAxisMappedTo[0]+","+xAxisMappedTo[1]+","+xAxisMappedTo[2]);
			System.out.println("y axis mapped to: "+yAxisMappedTo[0]+","+yAxisMappedTo[1]+","+yAxisMappedTo[2]);
			System.out.println("z axis mapped to: "+zAxisMappedTo[0]+","+zAxisMappedTo[1]+","+zAxisMappedTo[2]);

			double [][] m = new double[3][4];

			m[0][0] = xAxisMappedTo[0];
			m[1][0] = xAxisMappedTo[1];
			m[2][0] = xAxisMappedTo[2];

			m[0][1] = yAxisMappedTo[0];
			m[1][1] = yAxisMappedTo[1];
			m[2][1] = yAxisMappedTo[2];

			m[0][2] = zAxisMappedTo[0];
			m[1][2] = zAxisMappedTo[1];
			m[2][2] = zAxisMappedTo[2];

			FastMatrix rotationMatrix = new FastMatrix(m);
			double [] eulerParameters = new double[6];
			rotationMatrix.guessEulerParameters(eulerParameters);

			double z1 = eulerParameters[0];
			double x1 = eulerParameters[1];
			double z2 = eulerParameters[2];

			initialValues[0] = z1;
			initialValues[1] = x1;
			initialValues[2] = z2;
			initialValues[3] = p.x;
			initialValues[4] = p.y;
			initialValues[5] = p.z;

			FineTuneThread fineTuneThread = new FineTuneThread(
				CORRELATION,
				templateCubeSide,
				cropped,
				templateImage,
				pointInTemplate,
				imp,
				p,
				initialValues,
				guessedTransformation,
				progressWindow,
				this);

			fineTuneThreadQueue.addLast( fineTuneThread );
		}

		synchronized( fineTuneThreadQueue ) {

			/* Create a final thread which we will
			   use for a last refinement stage after all of the
			   others have finished. */

			finalRefinementThread = new FineTuneThread(
				CORRELATION,
				templateCubeSide,
				cropped,
				templateImage,
				pointInTemplate,
				imp,
				p,
				null,
				guessedTransformation,
				progressWindow,
				this);

			// Now start the initial threads:

			for( int j = Math.min( fineTuneThreadQueue.size(), maxThreads ); j > 0; --j ) {
				System.out.println("========== Starting an initial thread ==========");
				startNextThread();
			}
		}

		return true;
	}

	boolean loadTemplateImage( ) {
		// If the templateImage hasn't already been loaded, load it now:
		if( templateImage == null ) {
			File templateImageFile = new File( templateImageFilename );
			if( ! templateImageFile.exists() ) {
				IJ.error("The template file ('"+templateImageFile.getAbsolutePath()+"') does not exist");
				return false;
			}
			templateImage = BatchOpener.openFirstChannel(templateImageFile.getAbsolutePath());
			if( templateImage == null ) {
				IJ.error( "Couldn't load the template image from: " + templateImageFilename );
				return false;
			}
		}
		return true;
	}

	void startNextThread( ) {
		synchronized (fineTuneThreadQueue) {
			if( fineTuning ) {
				FineTuneThread ftt = fineTuneThreadQueue.removeFirst();
				ftt.start();
				fineTuneThreadsStarted.addLast(ftt);
				++ currentlyRunningFineTuneThreads;
			}
		}
	}

	static void printParameters( double [] parameters ) {
		System.out.println( "  z1: "+parameters[0] );
		System.out.println( "  x1: "+parameters[1] );
		System.out.println( "  z2: "+parameters[2] );
		System.out.println( "  z1 degrees: "+((180 *parameters[0])/Math.PI) );
		System.out.println( "  z1 degrees: "+((180 *parameters[1])/Math.PI) );
		System.out.println( "  z1 degrees: "+((180 *parameters[2])/Math.PI) );
		System.out.println( "  tx: "+parameters[3]);
		System.out.println( "  ty: "+parameters[4]);
		System.out.println( "  tz: "+parameters[5]);
	}

	public static final int MEAN_ABSOLUTE_DIFFERENCES     = 1;
	public static final int MEAN_SQUARED_DIFFERENCES      = 2;
	public static final int CORRELATION		      = 3;
	public static final int NORMALIZED_MUTUAL_INFORMATION = 4;

	public static final String [] methodName = {
		"UNSET!",
		"mean abs diffs",
		"mean squ diffs",
		"correlation",
		"norm mut inf"
	};

	/**
	    toTransform is just a cropped region of the template
	    around the template point.

	    toTransform must have calibration data; if it weren't for
	    the possibility of the cube being clipped by the edges of
	    the template, cubeSide may be calculable from that.

	    templatePoint is the real world coordinate of the centre
	    of toTransform.

	    toKeep is the complete image we're marking ("guessing")
	    points in.

	    guessPoint is the real world coordinate of the point we've
	    guess as corresponding in toKeep.
	 */

	static RegistrationResult mapImageWith( ImagePlus toTransform,
						ImagePlus toKeep,
						NamedPointWorld templatePoint,
						NamedPointWorld guessedPoint,
						double[] mapValues,
						double cubeSide,
						int similarityMeasure,
						String imageTitle ) {

		double sumSquaredDifferences = 0;
		double sumAbsoluteDifferences = 0;
		long numberOfPoints = 0;
		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumXSquared = 0;
		double sumYSquared = 0;

		FastMatrix scalePointInToTransform = FastMatrix.fromCalibration(toTransform);
		FastMatrix scalePointInToKeep = FastMatrix.fromCalibration(toKeep);
		FastMatrix scalePointInToKeepInverse = scalePointInToKeep.inverse();

		FastMatrix backToOriginBeforeRotation = FastMatrix.translate(-cubeSide / 2, -cubeSide / 2, -cubeSide / 2);

		double z1 = mapValues[0];
		double x1 = mapValues[1];
		double z2 = mapValues[2];
		double tx = mapValues[3];
		double ty = mapValues[4];
		double tz = mapValues[5];

		FastMatrix rotateFromValues = FastMatrix.rotateEuler(z1, x1, z2);
		FastMatrix transformFromValues = FastMatrix.translate(tx, ty, tz);

		FastMatrix mFM = new FastMatrix(scalePointInToTransform);
		mFM = backToOriginBeforeRotation.times(mFM);
		mFM = rotateFromValues.times(mFM);
		mFM = transformFromValues.times(mFM);
		mFM = scalePointInToKeepInverse.times(mFM);

		/* Now transform the corner points of the cropped
		   template image to find the maximum and minimum
		   extents of the transformed image. */

		int w = toTransform.getWidth();
		int h = toTransform.getHeight();
		int d = toTransform.getStackSize();

		int[][] corners = {{0, 0, 0}, {w, 0, 0}, {0, h, 0}, {0, 0, d}, {w, 0, d}, {0, h, d}, {w, h, 0}, {w, h, d}};

		double xmin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymin = Double.MAX_VALUE;
		double ymax = Double.MIN_VALUE;
		double zmin = Double.MAX_VALUE;
		double zmax = Double.MIN_VALUE;

		for (int i = 0; i < corners.length; ++i) {

			mFM.apply(corners[i][0], corners[i][1], corners[i][2]);
			if (mFM.x < xmin) {
				xmin = mFM.x;
			}
			if (mFM.x > xmax) {
				xmax = mFM.x;
			}
			if (mFM.y < ymin) {
				ymin = mFM.y;
			}
			if (mFM.y > ymax) {
				ymax = mFM.y;
			}
			if (mFM.z < zmin) {
				zmin = mFM.z;
			}
			if (mFM.z > zmax) {
				zmax = mFM.z;
			}
		}

		int transformed_x_min = (int) Math.floor(xmin);
		int transformed_y_min = (int) Math.floor(ymin);
		int transformed_z_min = (int) Math.floor(zmin);

		int transformed_x_max = (int) Math.ceil(xmax);
		int transformed_y_max = (int) Math.ceil(ymax);
		int transformed_z_max = (int) Math.ceil(zmax);

		/*
		  System.out.println("x min, max: " + transformed_x_min + "," + transformed_x_max);
		  System.out.println("y min, max: " + transformed_y_min + "," + transformed_y_max);
		  System.out.println("z min, max: " + transformed_z_min + "," + transformed_z_max);
		*/

		int transformed_width = (transformed_x_max - transformed_x_min) + 1;
		int transformed_height = (transformed_y_max - transformed_y_min) + 1;
		int transformed_depth = (transformed_z_max - transformed_z_min) + 1;

		if( transformed_width < 0 || transformed_height < 0 || transformed_depth < 0 ) {
			System.out.println("=== Error ==================");
			System.out.println("transformed dimensions: " + transformed_width + "," + transformed_height + "," + transformed_depth);
		}

		int k_width = toKeep.getWidth();
		int k_height = toKeep.getHeight();
		int k_depth = toKeep.getStackSize();

		byte[][] toKeepCroppedBytes = new byte[transformed_depth][transformed_height * transformed_width];

		ImageStack toKeepStack = toKeep.getStack();
		for (int z = 0; z < transformed_depth; ++z) {
			int z_uncropped = z + transformed_z_min;
			if ((z_uncropped < 0) || (z_uncropped >= k_depth)) {
				continue;
			}
			byte[] slice_pixels = (byte[]) toKeepStack.getPixels(z_uncropped+1);
			for (int y = 0; y < transformed_height; ++y) {
				for (int x = 0; x < transformed_width; ++x) {
					int x_uncropped = transformed_x_min + x;
					int y_uncropped = transformed_y_min + y;
					if ((x_uncropped < 0) || (x_uncropped >= k_width) || (y_uncropped < 0) || (y_uncropped >= k_height)) {
						continue;
					}
					toKeepCroppedBytes[z][y * transformed_width + x] = slice_pixels[y_uncropped * k_width + x_uncropped];
				}
			}
		}

		ImageStack toTransformStack=toTransform.getStack();
		byte [][] toTransformBytes=new byte[d][];
		for( int z_s = 0; z_s < d; ++z_s)
			toTransformBytes[z_s]=(byte[])toTransformStack.getPixels(z_s+1);

		FastMatrix back_to_template = mFM.inverse();

		byte [][] transformedBytes = new byte[transformed_depth][transformed_height * transformed_width];

		for( int z = 0; z < transformed_depth; ++z ) {
			for( int y = 0; y < transformed_height; ++y ) {
				for( int x = 0; x < transformed_width; ++x ) {

					int x_in_original = x + transformed_x_min;
					int y_in_original = y + transformed_y_min;
					int z_in_original = z + transformed_z_min;

					// System.out.println("in original: "+x_in_original+","+y_in_original+","+z_in_original);

					back_to_template.apply(
						x_in_original,
						y_in_original,
						z_in_original );

					int x_in_template = (int)back_to_template.x;
					int y_in_template = (int)back_to_template.y;
					int z_in_template = (int)back_to_template.z;

					// System.out.print("Got back *_in_template "+x_in_template+","+y_in_template+","+z_in_template);

					if( (x_in_template < 0) || (x_in_template >= w) ||
					    (y_in_template < 0) || (y_in_template >= h) ||
					    (z_in_template < 0) || (z_in_template >= d) ) {
						// System.out.println("skipping");
						continue;
					}
					// System.out.println("including");

					int value=toTransformBytes[z_in_template][y_in_template*w+x_in_template]&0xFF;

					transformedBytes[z][y*transformed_width+x]=(byte)value;

					int valueInOriginal = toKeepCroppedBytes[z][y*transformed_width+x] &0xFF;

					int difference = Math.abs( value - valueInOriginal );
					int differenceSquared = difference * difference;

					sumAbsoluteDifferences += difference;
					sumSquaredDifferences += differenceSquared;

					sumX += value;
					sumXSquared += value * value;

					sumY += valueInOriginal;
					sumYSquared += valueInOriginal * valueInOriginal;

					sumXY += value * valueInOriginal;

					++numberOfPoints;

				}
			}
		}

		RegistrationResult result = new RegistrationResult();

		result.overlay_width = transformed_width;
		result.overlay_height = transformed_height;
		result.overlay_depth = transformed_depth;
		result.transformed_bytes = transformedBytes;
		result.fixed_bytes = toKeepCroppedBytes;

		result.parameters = mapValues;

		/* Work out the score... */

		double maximumValue = 0;

		switch(similarityMeasure) {

		case MEAN_ABSOLUTE_DIFFERENCES:
			maximumValue = 255;
			break;

		case MEAN_SQUARED_DIFFERENCES:
			maximumValue = 255 * 255;
			break;

		case CORRELATION:
			maximumValue = 2;
			break;

		case NORMALIZED_MUTUAL_INFORMATION:
			maximumValue = 1;
			break;

		default:
			assert false : "Unknown similarity measure: "+similarityMeasure;

		}

		Calibration c = toKeep.getCalibration();
		double toKeep_x_spacing = 1;
		double toKeep_y_spacing = 1;
		double toKeep_z_spacing = 1;
		if( c != null ) {
			toKeep_x_spacing = c.pixelWidth;
			toKeep_y_spacing = c.pixelHeight;
			toKeep_z_spacing = c.pixelDepth;
		}

		double pointDrift;

		{
			// Map the centre of the cropped template with this
			// transformation and see how far away it is from the
			// guessed point.

			int centre_cropped_template_x = toTransform.getWidth() / 2;
			int centre_cropped_template_y = toTransform.getHeight() / 2;
			int centre_cropped_template_z = toTransform.getStackSize() / 2;

			mFM.apply( centre_cropped_template_x,
				   centre_cropped_template_y,
				   centre_cropped_template_z );

			result.point_would_be_moved_to_x = mFM.x * toKeep_x_spacing;
			result.point_would_be_moved_to_y = mFM.y * toKeep_y_spacing;
			result.point_would_be_moved_to_z = mFM.z * toKeep_z_spacing;

			double xdiff = result.point_would_be_moved_to_x - guessedPoint.x;
			double ydiff = result.point_would_be_moved_to_y - guessedPoint.y;
			double zdiff = result.point_would_be_moved_to_z - guessedPoint.z;

			double pointDriftSquared =
				(xdiff * xdiff) + (ydiff * ydiff) + (zdiff * zdiff);

			pointDrift = Math.sqrt(pointDriftSquared);
		}

		result.pointMoved = pointDrift;

		/* Now what happens to the template point
		 * (transformed) and the original guessed point. */

		/* The original guessed point just has to have the
		 * offset of newImage subtracted from it:
		 */

		result.fixed_point_x = (int)( (guessedPoint.x / toKeep_x_spacing) - transformed_x_min );
		result.fixed_point_y = (int)( (guessedPoint.y / toKeep_y_spacing) - transformed_y_min );
		result.fixed_point_z = (int)( (guessedPoint.z / toKeep_z_spacing) - transformed_z_min );

		/* The template point - we worked out where it moved
		 * to above, but not adjusted for the cropping... */

		result.transformed_point_x = (int)( (result.point_would_be_moved_to_x / toKeep_x_spacing) - transformed_x_min );
		result.transformed_point_y = (int)( (result.point_would_be_moved_to_y / toKeep_y_spacing) - transformed_y_min );
		result.transformed_point_z = (int)( (result.point_would_be_moved_to_z / toKeep_z_spacing) - transformed_z_min );

		// Back to the scoring now: now use the logistic
		// function to scale up the penalty as we get further
		// away in translation...

		double proportionOfCubeSideAway = pointDrift / cubeSide;

		double additionalTranslationalPenalty = Penalty.logisticPenalty( proportionOfCubeSideAway,
										 0.8,
										 1.0,
										 maximumValue );

		/* Also use the logistic function to penalize the
		   rotation from getting too near to the extrema: 4PI
		   and -4PI. */

		double absz1 = Math.abs(z1);
		double absx1 = Math.abs(x1);
		double absz2 = Math.abs(z2);

		double mostExtremeAngle =  Math.max(Math.max(absz1,absx1),absz2);

		double additionalAnglePenalty = Penalty.logisticPenalty( mostExtremeAngle,
									(7 * Math.PI) / 2,
									4 * Math.PI,
									maximumValue );

		if( numberOfPoints == 0 ) {
			/* This should be unneccessary, since there
			   are heavy penalties for moving towards the
			   point of no overlap. */
			result.score = maximumValue;
		} else {

			switch(similarityMeasure) {

			case MEAN_ABSOLUTE_DIFFERENCES:
				result.score = sumAbsoluteDifferences / numberOfPoints;
				break;

			case MEAN_SQUARED_DIFFERENCES:
				result.score = sumSquaredDifferences / numberOfPoints;
				break;

			case CORRELATION:
				double n2 = numberOfPoints * numberOfPoints;
				double numerator = (sumXY/numberOfPoints) - (sumX * sumY) / n2;
				double varX = (sumXSquared / numberOfPoints) - (sumX * sumX) / n2;
				double varY = (sumYSquared / numberOfPoints) - (sumY * sumY) / n2;
				double denominator = Math.sqrt(varX)*Math.sqrt(varY);
				if( denominator <= 0.00000001 ) {
					// System.out.println("Fixing near zero correlation denominator: "+denominator);
					result.score = 0;
				} else {
					result.score = numerator / denominator;
				}
				// System.out.println("raw correlation is: "+result.score);
				/* The algorithm tries to minimize the
				   score, and we want a correlation
				   close to 1, change the score somewhat:
				*/
				result.score = 1 - result.score;
				break;

			case NORMALIZED_MUTUAL_INFORMATION:
				assert false : "Mutual information measure not implemented yet";
				break;

			}
		}

		result.score += additionalAnglePenalty;
		result.score += additionalTranslationalPenalty;

		return result;
	}

	public void save(String fileType) {

		FileInfo info = imp.getOriginalFileInfo();
		if( info == null ) {
			IJ.error("There's no original file name that these points refer to.");
			return;
		}
		String fileName = info.fileName;
		String url = info.url;
		String directory = info.directory;

		// GJ: Note that the image fileName is used directly here since
		// ij.io.SaveDialog.setExtension() appends rather than replaces
		// file extensions of more than 5 characters
		SaveDialog sd = new SaveDialog("Save points annotation file as...",
					       directory,
					       fileName,
					       fileType);

		String savePath;
		if(sd.getFileName()==null)
			return;
		else {
			savePath = sd.getDirectory()+sd.getFileName();
		}

		File file = new File(savePath);
		if ((file!=null)&&file.exists()) {
			if (!IJ.showMessageWithCancel(
				    "Save points annotation file", "The file "+
				    savePath+" already exists.\n"+
				    "Do you want to replace it?"))
				return;
		}

		IJ.showStatus("Saving point annotations to "+savePath);

		boolean saveResult=false;
		if(fileType.equalsIgnoreCase(".landmarks")) {
			saveResult=points.saveIGSPointsFile(savePath);
		} else {
			saveResult=points.savePointsFile(savePath);
		}

		if( ! saveResult )
			IJ.error("Error saving to: "+savePath+"\n");

		unsaved = false;
		IJ.showStatus("Saved point annotations.");

	}

	public void reset(int i) {
		points.unset(i);
		dialog.reset(i);
		unsaved = true;
	}

	public void reset() {
		for( int i = 0; i < points.size(); ++i ) {
			reset( i );
		}
	}

	public void mark(int i) {
		Roi roi = imp.getRoi();
		if (roi!=null && roi.getType()==Roi.POINT) {
			Polygon p = roi.getPolygon();
			if(p.npoints > 1) {
				IJ.error("You can only have one point selected to mark.");
				return;
			}

			System.out.println("Fetched ROI with co-ordinates: "+p.xpoints[0]+", "+p.ypoints[0]);

			/* The ROI co-ordinates are indexes into the
			   samples in the image stack as opposed to
			   values modified by the view (zoom, offset)
			   or calibration.
			 */

			int x = p.xpoints[0];
			int y = p.ypoints[0];
			int z = imp.getCurrentSlice()-1;
			int channels = imp.getNChannels();
			z /= channels;

			Calibration c = imp.getCalibration();
			double xWorld = x, yWorld = y, zWorld = z;
			if( c != null ) {
				xWorld = x * c.pixelWidth;
				yWorld = y * c.pixelHeight;
				zWorld = z * c.pixelDepth;
			}

			System.out.println("Converted to our co-ordinates: "+xWorld+","+yWorld+","+zWorld);

			dialog.setCoordinateLabel(i,xWorld,yWorld,zWorld);
			dialog.pack();

			NamedPointWorld point = points.get(i);
			point.set( xWorld, yWorld, zWorld );

			unsaved = true;

		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}

	}

	public void get( boolean mineOnly ) {

		Hashtable<String,String> parameters = new Hashtable<String,String>();

		parameters.put("method","most-recent-annotation");
		parameters.put("type","points");
		parameters.put("variant","around-central-complex");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));
		if( mineOnly )
			parameters.put("for_user",archiveClient.getValue("user"));
		else
			parameters.put("for_user","");

		// Need to included data too....

		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, null );

		String [] first_line = tsv_results.get(0);
		int urls_found;
		String bestUrl = null;
		if( first_line[0].equals("success") ) {
			urls_found = Integer.parseInt(first_line[1]);
			if( urls_found == 0 )
				IJ.error( "No anntation files by " + (mineOnly ? archiveClient.getValue("user") : "any user") + " found." );
			else {
				bestUrl = (tsv_results.get(1))[1];
				// IJ.error( "Got the URL: " + bestUrl );
			}
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while getting the most recent annotation: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to request for an annotation file: " + first_line[0]);
		}

		// Now fetch that file:

		if( bestUrl == null )
			return;

		String fileContents =  ArchiveClient.justGetFileAsString( bestUrl );
		if( fileContents == null ) {
			IJ.error("Failed to fetch URL: "+bestUrl);
		} else {
			NamedPointSet nps = NamedPointSet.fromString( fileContents );
			this.points = nps;
			dialog.recreatePointsPanel();
			dialog.pack();
			unsaved = false;
		}
	}

	public void upload() {

		Hashtable<String,String> parameters = new Hashtable<String,String>();

		parameters.put("method","upload-annotation");
		parameters.put("type","points");
		parameters.put("variant","around-central-complex");
		parameters.put("md5sum",archiveClient.getValue("md5sum"));

		// Need to included data too....

		byte [] fileAsBytes = points.xmlDataAsBytes( );

		ArrayList< String [] > tsv_results = archiveClient.synchronousRequest( parameters, fileAsBytes );

		String [] first_line = tsv_results.get(0);
		if( first_line[0].equals("success") ) {
			IJ.error("Annotations uploaded successfully!");
		} else if( first_line[0].equals("error") ) {
			IJ.error("There was an error while uploading the annotation file: "+first_line[1]);
		} else {
			IJ.error("There was an unknown response to the annotation file upload request: " + first_line[0]);
		}

	}

	PointsDialog dialog;
	ImagePlus imp;

	NamedPointSet points;

	ArchiveClient archiveClient;

	ImageCanvas canvas;

	boolean batchFineTuning = false;

	public void batchFineTune( String templateImageFilename, String inputImageFilename, String outputPointsFilename ) {

		boolean templateSet = useTemplate( templateImageFilename );
		if( ! templateSet )
			return;

		imp = BatchOpener.openFirstChannel(inputImageFilename);
		if( imp == null ) {
			IJ.error("Couldn't open the input image file '"+inputImageFilename+"'");
			return;
		}

		Calibration c=imp.getCalibration();
		x_spacing=c.pixelWidth;
		y_spacing=c.pixelHeight;
		z_spacing=c.pixelDepth;

		try {
			points = NamedPointSet.forImage(imp);
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error("Couldn't load points file for image '"+imp.getTitle()+"': " + e ) ;
			return;
		}

		ArrayList<String> namesInCommon = points.namesSharedWith(templatePoints,true);

		latch = new Object();

		for( String name : namesInCommon ) {

			NamedPointWorld guessedPoint = points.get( name );

			boolean started = fineTune( guessedPoint );
			if( ! started ) {
				IJ.error( "Failed to start a fineTuneThread" );
				return;
			}

			synchronized ( latch ) {
				try {
					latch.wait( );
				} catch( InterruptedException e ) { }
			}
		}

		// Now we should have an adjusted NamedPointSet, which
		// we can write out to the output filename...

		if( ! points.savePointsFile( outputPointsFilename ) ) {
			IJ.error( "Saving the points file to: " + outputPointsFilename );
		}
	}


	public void load() {

		OpenDialog od;

		FileInfo info = imp.getOriginalFileInfo();
		String fileName, url, directory;
		if( info == null ) {
			fileName = null;
			url = null;
			directory = null;
		} else {
			fileName = info.fileName;
			url = info.url;
			directory = info.directory;
		}

		String openTitle = "Select points file...";

		if( directory == null )
			od = new OpenDialog( openTitle, null );
		else
			od = new OpenDialog( openTitle, directory, null );
		if( od.getFileName() == null )
			return;
		else {
			File f = new File( od.getDirectory(), od.getFileName() );
			NamedPointSet newNamedPoints = null;
			try {
				newNamedPoints = NamedPointSet.fromFile( f.getAbsolutePath() );
			} catch( NamedPointSet.PointsFileException pfe ) {
				IJ.error( "Failed to load points file: "+pfe );
			}
			if( newNamedPoints == null )
				return;
			else {
				points = newNamedPoints;
				dialog.recreatePointsPanel();
				dialog.pack();
			}
		}
	}

	public boolean loadAtStart() {

		NamedPointSet newNamedPoints = null;
		try {
			newNamedPoints = NamedPointSet.forImage(imp);
		} catch( NamedPointSet.PointsFileException e ) {
			return false;
		}

		if( points == null ) {
			points=new NamedPointSet();
		}

		if( newNamedPoints == null )
			return false;

		ListIterator<NamedPointWorld> i;
		for (i = newNamedPoints.listIterator();i.hasNext();) {
			NamedPointWorld current = i.next();
			boolean foundName = false;
			ListIterator<NamedPointWorld> j;
			for(j=points.listIterator();j.hasNext();) {
				NamedPointWorld p = j.next();
				if (current.getName().equals(p.getName())) {
					p.x = current.x;
					p.y = current.y;
					p.z = current.z;
					p.set = true;
					foundName = true;
				}
			}
			if (!foundName)
				points.add(current);
		}
		unsaved = false;
		return true;
	}

	public void setDefaultTemplate( ) {
		setDefaultTemplate( templateImageFilename );
	}

	public void setDefaultTemplate( String defaultTemplateImageFilename ) {
		if( defaultTemplateImageFilename == null )
			defaultTemplateImageFilename = "";
		System.out.println("setDefaultTemplate called with: "+defaultTemplateImageFilename);
		Prefs.set("landmarks.Name_Points.templateImageFilename", defaultTemplateImageFilename );
		System.out.println("After setting preference, the value got back was: "+Prefs.get("landmarks.Name_Points.templateImageFilename",null));
	}

	public boolean useTemplate( String possibleTemplateImageFilename ) {
		if( possibleTemplateImageFilename == null ) {
			// Then unset the template:
			templateImageFilename = null;
			templateImage = null;
			templatePoints = null;
			return true;
		}
		File possibleTemplateImageFile = new File( possibleTemplateImageFilename );
		if( ! possibleTemplateImageFile.exists() ) {
			IJ.error( "The file " + possibleTemplateImageFilename + " doesn't exist.");
			return false;
		}
		templateImageFilename = possibleTemplateImageFilename;
		templateImage = null;
		NamedPointSet newTemplatePointSet = null;
		try {
			newTemplatePointSet = NamedPointSet.forImage(templateImageFilename);
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error( "Warning: Couldn't load a points file corresponding to this template: " + e );
			return true;
		}
		templatePoints = newTemplatePointSet;
		return true;
	}

	public void fineTuneNewBestResult( RegistrationResult result ) {
		if( progressWindow != null )
			progressWindow.offerNewResult( result );
	}

	public void stopFineTuneThreads( ) {

		synchronized( fineTuneThreadQueue ) {
			for( FineTuneThread runningThread : fineTuneThreadsStarted )
				runningThread.askToFinish();
			fineTuning = false;
		}
		for( FineTuneThread f : fineTuneThreadsStarted ) {
			System.out.println( "Waiting for thread " + f + " to finish..." );
			try {
				f.join();
			} catch( InterruptedException e ) {
			}
			System.out.println("... done waiting for thread.");
		}
		if( dialog != null )
			dialog.setFineTuning( false );

		if( progressWindow != null && progressWindow.useTheResult ) {
			useFineTuneResult( progressWindow.bestSoFar );
		}

		System.out.println("FINISHED! (in stopFineTuneThreads)");
	}

	public void useFineTuneResult( ) {
		useFineTuneResult( bestSoFar );
	}

	public void useFineTuneResult( RegistrationResult r ) {

		if( r != null ) {

			NamedPointWorld point = points.get( indexOfPointBeingFineTuned );
			point.x = r.point_would_be_moved_to_x;
			point.y = r.point_would_be_moved_to_y;
			point.z = r.point_would_be_moved_to_z;
			point.set = true;
			System.out.println("Got a result, changed point to: "+point);

			unsaved = true;

			if( dialog != null ) {
				dialog.setCoordinateLabel( indexOfPointBeingFineTuned,
							   point.x,
							   point.y,
							   point.z );
				dialog.pack();
			}
		}

		progressWindow = null;
	}

	public void updateBest( RegistrationResult r ) {
		if( bestSoFar == null || r.score < bestSoFar.score ) {
			bestSoFar = r;
		}
	}

	public void fineTuneThreadFinished( int reason, RegistrationResult result, FineTuneThread fineTuneThread ) {

		synchronized( fineTuneThreadQueue ) {

			if( result != null ) {
				if( progressWindow != null )
					progressWindow.offerNewResult( result );
				updateBest( result );
			}

			if( currentlyRunningFineTuneThreads <= maxThreads &&
			    reason == FineTuneProgressListener.COMPLETED ) {
				if( fineTuneThreadQueue.size() > 0 ) {
					System.out.println( "========== A thread finished, and with currentlyRunningFineTuneThreads = " + currentlyRunningFineTuneThreads + ", starting a thread ==========" );
					startNextThread();
				}
			}

			-- currentlyRunningFineTuneThreads;

			if( currentlyRunningFineTuneThreads == 0 ) {

				if( fineTuning && ! startedAdditionalRefinement ) {

					/* Then take the best so far, and restart from there.
					   Sometimes this seems to produce some improvement. */

					System.out.println("Starting refinement thread!");

					startedAdditionalRefinement = true;
					finalRefinementThread.setInitialTransformation( bestSoFar.parameters );
					fineTuneThreadQueue.addLast( finalRefinementThread );
					startNextThread();
				} else {

					// When the fine-tuning is finished, we should always pass through this section of code:

					if( reason == COMPLETED ) {
						// Then indicated that we've finished in the instructions panel:
						if( dialog != null ) {
							dialog.instructions.setText("Completed: select 'Use this' or 'Cancel' in the fine-tune window.");
							dialog.pack();
						}
						if( batchFineTuning ) {
							System.out.println("########################################################################");
							System.out.println("Finished batchFineTuning one point, was: "+fineTuneThread.guessedPoint.toString());
							// Then adjust the point as if someone had clicked useThis:
							useFineTuneResult();
							System.out.println("Point is now: "+points.get( indexOfPointBeingFineTuned ).toString());
							fineTuning = false;
						}
					}

					if( latch != null ) {
						synchronized (latch) {
							latch.notifyAll();
						}

					}
				}
			}
		}
	}

	final static int AFFINE = 1;
	final static int RIGID = 2;
	final static int BOOKSTEIN = 3;

	public void doRegistration( int method ) {
		RegistrationAlgorithm r = null;
		switch( method ) {
		case AFFINE:
			r = new AffineFromLandmarks();
			break;
		case RIGID:
			r = new RigidFromLandmarks();
			break;
		case BOOKSTEIN:
			r = new BooksteinFromLandmarks();
			break;
		default:
			IJ.error("BUG: unknown registration method requested");
			return;
		}
		if( templatePoints == null ) {
			IJ.error("You must have a template file loaded in order to perform register the images");
			return;
		}
		if( ! loadTemplateImage() )
			return;
		boolean overlayResult = dialog.overlayResult.getState();
		r.setImages( templateImage, imp );
		ImagePlus transformed = r.register( templatePoints, points );
		if( transformed == null )
			return;
		if( overlayResult ) {
			ImagePlus merged = OverlayRegistered.overlayToImagePlus( templateImage, transformed );
			merged.setTitle( "Registered and Overlayed" );
			merged.show();
		} else
			transformed.show();
	}
}

