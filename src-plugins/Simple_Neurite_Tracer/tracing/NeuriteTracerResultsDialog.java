/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

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
import ij.io.*;
import ij.gui.YesNoCancelDialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import features.SigmaPalette;
import ij.gui.GenericDialog;

import java.text.DecimalFormat;

import ij.measure.Calibration;

public class NeuriteTracerResultsDialog
	extends Dialog
	implements ActionListener, WindowListener, ItemListener, PathAndFillListener, TextListener, SigmaPalette.SigmaPaletteListener, ImageListener {

	static final boolean verbose = SimpleNeuriteTracer.verbose;

	public PathWindow pw;
	public FillWindow fw;

	// These are the states that the UI can be in:

	static final int WAITING_TO_START_PATH    = 0;
	static final int PARTIAL_PATH             = 1;
	static final int SEARCHING                = 2;
	static final int QUERY_KEEP               = 3;
	static final int LOGGING_POINTS           = 4;
	static final int DISPLAY_EVS              = 5;
	static final int FILLING_PATHS            = 6;
	static final int CALCULATING_GAUSSIAN     = 7;
	static final int WAITING_FOR_SIGMA_POINT  = 8;
	static final int WAITING_FOR_SIGMA_CHOICE = 9;
	static final int SAVING                   = 10;
	static final int LOADING                  = 11;

	static final String [] stateNames = { "WAITING_TO_START_PATH",
					      "PARTIAL_PATH",
					      "SEARCHING",
					      "QUERY_KEEP",
					      "LOGGING_POINTS",
					      "DISPLAY_EVS",
					      "FILLING_PATHS",
					      "CALCULATING_GAUSSIAN",
					      "WAITING_FOR_SIGMA_POINT",
					      "WAITING_FOR_SIGMA_CHOICE",
					      "SAVING",
					      "LOADING" };

	static final String SEARCHING_STRING = "Searching for path between points...";

	private int currentState;

	SimpleNeuriteTracer plugin;

	Panel statusPanel;
	TextArea statusText;
	Button keepSegment, junkSegment;
	Button cancelSearch;

	Panel pathActionPanel;
	Button completePath;
	Button cancelPath;

	Choice viewPathChoice;
	String projectionChoice = "projected through all slices";
	String partsNearbyChoice = "parts in nearby slices";

	TextField nearbyField;

	PathColorsCanvas pathColorsCanvas;

	Choice colorImageChoice;
	String noColorImageString = "[None]";
	ImagePlus currentColorImage;

	Checkbox justShowSelected;

	Choice paths3DChoice;
	String [] paths3DChoicesStrings = {
		"BUG",
		"as surface reconstructions",
		"as lines",
		"as lines and discs" };

	Checkbox preprocess;
	Checkbox usePreprocessed;

	double currentSigma;
	double currentMultiplier;

	Label currentSigmaAndMultiplierLabel;

	Button editSigma;
	Button sigmaWizard;

	Button loadLabelsButton;
	Button exportCSVButton;
	Button makeLineStackButton;

	Button showCorrespondencesToButton;

	Button saveButton;
	Button loadButton;
	Button uploadButton;
	Button fetchButton;

	Button quitButton;

	Button showOrHidePathList;
	Button showOrHideFillList;

	// ------------------------------------------------------------------------
	// Implementing the ImageListener interface:

	public void imageOpened(ImagePlus imp) {
		updateColorImageChoice();
	}

	// Called when an image is closed
	public void imageClosed(ImagePlus imp) {
		updateColorImageChoice();
	}

	// Called when an image's pixel data is updated
	public void imageUpdated(ImagePlus imp) {
		updateColorImageChoice();
	}

	// ------------------------------------------------------------------------

	public static final boolean onlySameSizedImages = false;

	synchronized public void updateColorImageChoice() {

		// Try to preserve the old selection:
		String oldSelection = colorImageChoice.getSelectedItem();

		colorImageChoice.removeAll();

		int j = 0;
		colorImageChoice.addItem(noColorImageString);

		int selectedIndex = 0;

		int[] wList = WindowManager.getIDList();
		if (wList!=null) {
			for (int i=0; i<wList.length; i++) {
				ImagePlus imp = WindowManager.getImage(wList[i]);
				if( (! onlySameSizedImages) ||
				    ((imp.getWidth() == plugin.width) &&
				     (imp.getHeight() == plugin.height) &&
				     (imp.getStackSize() == plugin.depth)) ) {
					j ++;
					String title = imp.getTitle();
					colorImageChoice.addItem(title);
					if( title == oldSelection )
						selectedIndex = j;
				}
			}
		}

		colorImageChoice.select(selectedIndex);
		// This doesn't trigger an item event
		checkForColorImageChange();
	}

	public static boolean similarCalibrations(Calibration a, Calibration b) {
		double ax = 1, ay = 1, az = 1;
		double bx = 1, by = 1, bz = 1;
		if( a != null ) {
			ax = a.pixelWidth;
			ay = a.pixelHeight;
			az = a.pixelDepth;
		}
		if( b != null ) {
			bx = b.pixelWidth;
			by = b.pixelHeight;
			bz = b.pixelDepth;
		}
		double pixelWidthDifference = Math.abs( ax - bx );
		double pixelHeightDifference = Math.abs( ay - by );
		double pixelDepthDifference = Math.abs( az - bz );
		double epsilon = 0.000001;
		if( pixelWidthDifference > epsilon )
			return false;
		if( pixelHeightDifference > epsilon )
			return false;
		if( pixelDepthDifference > epsilon )
			return false;
		return true;
	}

	synchronized public void checkForColorImageChange() {
		String selectedTitle = colorImageChoice.getSelectedItem();

		ImagePlus intendedColorImage = null;
		if( ! selectedTitle.equals(noColorImageString) ) {
			intendedColorImage = WindowManager.getImage(selectedTitle);
		}

		if( intendedColorImage != currentColorImage ) {
			if( intendedColorImage != null ) {
				ImagePlus image = plugin.getImagePlus();
				Calibration calibration = plugin.getImagePlus().getCalibration();
				Calibration colorImageCalibration = intendedColorImage.getCalibration();
				if( ! similarCalibrations( calibration,
							   colorImageCalibration ) ) {
					IJ.error("Warning: the calibration of '"+intendedColorImage.getTitle()+"' is different from the image you're tracing ('"+image.getTitle()+"')'\nThis may produce unexpected results.");
				}
				if( (! onlySameSizedImages) &&
				    ! (intendedColorImage.getWidth() == image.getWidth() &&
				       intendedColorImage.getHeight() == image.getHeight() &&
				       intendedColorImage.getStackSize() == image.getStackSize()) )
					IJ.error("Warning: the dimensions (in voxels) of '"+intendedColorImage.getTitle()+"' is different from the image you're tracing ('"+image.getTitle()+"')'\nThis may produce unexpected results.");
			}
			currentColorImage = intendedColorImage;
			plugin.setColorImage(currentColorImage);
		}
	}

	public void newSigmaSelected( double sigma ) {
		setSigma( sigma, false );
	}

	public void newMaximum( double max ) {
		double multiplier = 256 / max;
		setMultiplier( multiplier );
	}

	// ------------------------------------------------------------------------
	// FIXME: consider moving these into SimpleNeuriteTracer

	public void setPathList( String [] newList, Path justAdded, boolean expandAll ) { }

	public void setFillList( String [] newList ) { }

	// Note that rather unexpectedly the p.setSelcted calls make sure that
	// the colour of the path in the 3D viewer is right...  (FIXME)
	public void setSelectedPaths( HashSet selectedPathsSet, Object source ) {
		if( source == this )
			return;
		for( int i = 0; i < pathAndFillManager.size(); ++i ) {
			Path p = pathAndFillManager.getPath(i);
			if( selectedPathsSet.contains(p) ) {
				p.setSelected( true );
			} else {
				p.setSelected( false );
			}
		}
	}

	// ------------------------------------------------------------------------

	int preGaussianState;
	int preSigmaPaletteState;

	public void gaussianCalculated(boolean succeeded) {
		if( !succeeded )
			preprocess.setState(false);
		changeState(preGaussianState);
		if( preprocess.getState() ) {
			editSigma.setEnabled(false);
			sigmaWizard.setEnabled(false);
		} else {
			editSigma.setEnabled(true);
			sigmaWizard.setEnabled(true);
		}
	}

	public void setMultiplier( double multiplier ) {
		currentMultiplier = multiplier;
		updateLabel( );
	}

	public void setSigma( double sigma, boolean mayStartGaussian ) {
		currentSigma = sigma;
		updateLabel( );
		if( mayStartGaussian ) {
			if( preprocess.getState() ) {
				IJ.error( "[BUG] The preprocess checkbox should never be on when setSigma is called" );
			} else {
				// Turn on the checkbox:
				preprocess.setState( true );
				/* ... according to the documentation
				   this doesn't generate an event, so
				   we manually turn on the Gaussian
				   calculation */
				turnOnHessian();
			}
		}
	}

	public void turnOnHessian( ) {
		preGaussianState = currentState;
		plugin.enableHessian(true);
	}

	DecimalFormat threeDecimalPlaces = new DecimalFormat("0.0000");
	DecimalFormat threeDecimalPlacesScientific = new DecimalFormat("0.00E00");

	public String formatDouble( double value ) {
		double absValue = Math.abs( value );
		if( absValue < 0.01 || absValue >= 1000 )
			return threeDecimalPlacesScientific.format(value);
		else
			return threeDecimalPlaces.format(value);
	}

	public void updateLabel( ) {
		currentSigmaAndMultiplierLabel.setText(
			"\u03C3 = " +
			formatDouble( currentSigma ) +
			", multiplier = " + formatDouble( currentMultiplier ) );
	}

	public double getSigma( ) {
		return currentSigma;
	}

	public double getMultiplier( ) {
		return currentMultiplier;
	}

	public void exitRequested() {

		// FIXME: check that everything is saved...

		if( plugin.pathsUnsaved() ) {

			YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Really quit?",
								     "There are unsaved paths. Do you really want to quit?" );

			if( ! d.yesPressed() )
				return;

		}

		plugin.cancelSearch( true );
		pw.dispose();
		fw.dispose();
		dispose();
		plugin.closeAndReset();
	}

	public void disableEverything() {

		fw.setEnabledNone();
		pw.setButtonsEnabled(false);

		statusText.setEnabled(false);
		keepSegment.setEnabled(false);
		junkSegment.setEnabled(false);
		cancelSearch.setEnabled(false);
		completePath.setEnabled(false);
		cancelPath.setEnabled(false);

		editSigma.setEnabled(false);
		sigmaWizard.setEnabled(false);

		viewPathChoice.setEnabled(false);
		paths3DChoice.setEnabled(false);
		preprocess.setEnabled(false);

		exportCSVButton.setEnabled(false);
		showCorrespondencesToButton.setEnabled(false);
		saveButton.setEnabled(false);
		loadButton.setEnabled(false);
		if( uploadButton != null ) {
			uploadButton.setEnabled(false);
			fetchButton.setEnabled(false);
		}
		loadLabelsButton.setEnabled(false);

		quitButton.setEnabled(false);
	}

	public void changeState( int newState ) {

		if (verbose) System.out.println("changeState to: "+stateNames[newState]);

		switch( newState ) {

		case WAITING_TO_START_PATH:
			statusText.setText("Click somewhere to start a new path...");
			disableEverything();
			pw.setButtonsEnabled(true);
			// Fake a selection change in the path tree:
			pw.valueChanged( null );

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			viewPathChoice.setEnabled(true);
			paths3DChoice.setEnabled(true);
			preprocess.setEnabled(true);

			editSigma.setEnabled( ! preprocess.getState() );
			sigmaWizard.setEnabled( ! preprocess.getState() );

			fw.setEnabledWhileNotFilling();

			loadLabelsButton.setEnabled(true);

			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			exportCSVButton.setEnabled(true);
			showCorrespondencesToButton.setEnabled(true);
			if( uploadButton != null ) {
				uploadButton.setEnabled(true);
				fetchButton.setEnabled(true);
			}

			quitButton.setEnabled(true);

			break;

		case PARTIAL_PATH:
			statusText.setText("Now select a point further along that structure...");
			disableEverything();

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			if( plugin.justFirstPoint() )
				completePath.setEnabled(false);
			else
				completePath.setEnabled(true);
			cancelPath.setEnabled(true);

			viewPathChoice.setEnabled(true);
			paths3DChoice.setEnabled(true);
			preprocess.setEnabled(true);

			editSigma.setEnabled( ! preprocess.getState() );
			sigmaWizard.setEnabled( ! preprocess.getState() );

			quitButton.setEnabled(false);

			break;

		case SEARCHING:
			statusText.setText("Searching for path between points...");
			disableEverything();

			cancelSearch.setLabel("Abandon search");
			cancelSearch.setEnabled(true);
			cancelSearch.setVisible(true);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			completePath.setEnabled(false);
			cancelPath.setEnabled(false);

			quitButton.setEnabled(true);

			break;

		case QUERY_KEEP:
			statusText.setText("Keep this new path segment?");
			disableEverything();

			keepSegment.setEnabled(true);
			junkSegment.setEnabled(true);

			cancelSearch.setVisible(false);
			keepSegment.setVisible(true);
			junkSegment.setVisible(true);

			break;

		case FILLING_PATHS:
			statusText.setText("Filling out from neuron...");
			disableEverything();

			fw.setEnabledWhileFilling();

			break;

		case CALCULATING_GAUSSIAN:
			statusText.setText("Calculating Gaussian...");
			disableEverything();

			cancelSearch.setLabel("Cancel");
			cancelSearch.setEnabled(true);
			cancelSearch.setVisible(true);
			keepSegment.setVisible(true);
			junkSegment.setVisible(true);

			break;

		case WAITING_FOR_SIGMA_POINT:
			statusText.setText("Click on a neuron in the image");
			disableEverything();
			break;

		case WAITING_FOR_SIGMA_CHOICE:
			statusText.setText("Close the sigma palette window to continue");
			disableEverything();
			break;

		case LOADING:
			statusText.setText("Loading...");
			disableEverything();
			break;

		case SAVING:
			statusText.setText("Saving...");
			disableEverything();
			break;

		default:
			IJ.error("BUG: switching to an unknown state");
			return;
		}

		pack();

		plugin.repaintAllPanes();

		currentState = newState;

	}

	public int getState() {
		return currentState;
	}

	// ------------------------------------------------------------------------

	public void windowClosing( WindowEvent e ) {
		exitRequested();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }

	public void updateSearchingStatistics( int pointsInOpenBoundary ) {
		statusText.setText( SEARCHING_STRING + " ("+pointsInOpenBoundary+" boundary points.)" );
	}

	private PathAndFillManager pathAndFillManager;

	boolean launchedByArchive;

	public NeuriteTracerResultsDialog( String title,
					   SimpleNeuriteTracer plugin,
					   boolean launchedByArchive ) {

		super( IJ.getInstance(), title, false );
		this.plugin = plugin;
		this.launchedByArchive = launchedByArchive;

		pathAndFillManager = plugin.getPathAndFillManager();

		addWindowListener(this);

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 3, 3, 3, 3 );

		{ /* Add the status panel */

			statusPanel = new Panel();
			statusPanel.setLayout(new BorderLayout());
			statusPanel.add(new Label("Instructions:"), BorderLayout.NORTH);
			statusText = new TextArea("Initial status text...",2,25,TextArea.SCROLLBARS_NONE);
			statusPanel.add(statusText,BorderLayout.CENTER);

			keepSegment = new Button("Yes");
			junkSegment = new Button("No");
			cancelSearch = new Button("Abandon Search");

			keepSegment.addActionListener( this );
			junkSegment.addActionListener( this );
			cancelSearch.addActionListener( this );

			Panel statusChoicesPanel = new Panel();
			statusChoicesPanel.setLayout( new GridBagLayout() );
			GridBagConstraints cs = new GridBagConstraints();
			cs.gridx = 0; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(keepSegment,cs);
			cs.gridx = 1; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(junkSegment,cs);
			cs.gridx = 2; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(cancelSearch,cs);

			statusPanel.add(statusChoicesPanel,BorderLayout.SOUTH);

			c.gridx = 0;
			c.gridy = 0;
			add(statusPanel,c);
		}

		{ /* Add the panel of actions to take on half-constructed paths */

			pathActionPanel = new Panel();
			completePath = new Button("Complete Path");
			cancelPath = new Button("Cancel Path");
			completePath.addActionListener( this );
			cancelPath.addActionListener( this );
			pathActionPanel.add(completePath);
			pathActionPanel.add(cancelPath);

			c.gridx = 0;
			++ c.gridy;
			add(pathActionPanel,c);
		}

		{ /* Add the panel with other options - preprocessing and the view of paths */

			Panel otherOptionsPanel = new Panel();

			otherOptionsPanel.setLayout(new GridBagLayout());
			GridBagConstraints co = new GridBagConstraints();
			co.anchor = GridBagConstraints.LINE_START;
			viewPathChoice = new Choice();
			viewPathChoice.addItem(projectionChoice);
			viewPathChoice.addItem(partsNearbyChoice);
			viewPathChoice.addItemListener(this);

			Panel nearbyPanel = new Panel();
			nearbyPanel.setLayout(new BorderLayout());
			nearbyPanel.add(new Label("(up to"),BorderLayout.WEST);
			nearbyField = new TextField("2",2);
			nearbyField.addTextListener(this);
			nearbyPanel.add(nearbyField,BorderLayout.CENTER);
			nearbyPanel.add(new Label("slices to each side)"),BorderLayout.EAST);

			co.gridx = 0;
			co.gridy = 0;
			otherOptionsPanel.add(new Label("View paths (2D): "),co);
			co.gridx = 1;
			co.gridy = 0;
			otherOptionsPanel.add(viewPathChoice,co);

			paths3DChoice = new Choice();
			if( plugin != null && plugin.use3DViewer ) {
				for( int choice = 1; choice < paths3DChoicesStrings.length; ++choice )
					paths3DChoice.addItem(paths3DChoicesStrings[choice]);

				co.gridx = 0;
				++ co.gridy;
				otherOptionsPanel.add(new Label("View paths (3D): "),co);
				co.gridx = 1;
				otherOptionsPanel.add(paths3DChoice,co);
			}
			paths3DChoice.addItemListener(this);

			co.gridx = 1;
			++ co.gridy;
			co.gridwidth = 1;
			co.anchor = GridBagConstraints.LINE_START;
			otherOptionsPanel.add(nearbyPanel,co);


			Panel colorOptionsPanel = new Panel();
			{
				Panel flatColorOptionsPanel = new Panel();
				flatColorOptionsPanel.setLayout(new BorderLayout());
				flatColorOptionsPanel.add( new Label("Click to change Path colours:"), BorderLayout.NORTH );
				pathColorsCanvas = new PathColorsCanvas( plugin, 150, 18 );
				flatColorOptionsPanel.add(pathColorsCanvas, BorderLayout.CENTER);

				Panel imageColorOptionsPanel = new Panel();
				imageColorOptionsPanel.setLayout(new BorderLayout());
				imageColorOptionsPanel.add(new Label("Use colors / labels from:"),BorderLayout.NORTH);

				colorImageChoice = new Choice();
				updateColorImageChoice();
				colorImageChoice.addItemListener(this);
				imageColorOptionsPanel.add(colorImageChoice,BorderLayout.CENTER);
				ImagePlus.addImageListener(this);

				colorOptionsPanel.add(flatColorOptionsPanel);
				colorOptionsPanel.add(imageColorOptionsPanel);
			}

			co.gridx = 0;
			++ co.gridy;
			co.gridwidth = 2;
			otherOptionsPanel.add(colorOptionsPanel,co);

			justShowSelected = new Checkbox( "Show only selected paths" );
			justShowSelected.addItemListener( this );
			co.gridx = 0;
			++ co.gridy;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_START;
			co.insets = new Insets( 0, 0, 0, 0 );
			otherOptionsPanel.add(justShowSelected,co);

			preprocess = new Checkbox("Hessian-based analysis");
			preprocess.addItemListener( this );

			co.gridx = 0;
			++ co.gridy;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_START;
			otherOptionsPanel.add(preprocess,co);

			++ co.gridy;
			usePreprocessed = new Checkbox("Use preprocessed image");
			usePreprocessed.addItemListener( this );
			usePreprocessed.setEnabled( plugin.tubeness != null );
			otherOptionsPanel.add(usePreprocessed,co);

			co.fill = GridBagConstraints.HORIZONTAL;

			currentSigmaAndMultiplierLabel = new Label();
			++ co.gridy;
			otherOptionsPanel.add(currentSigmaAndMultiplierLabel,co);
			setSigma( plugin.getMinimumSeparation(), false );
			setMultiplier( 4 );
			updateLabel( );
			++ co.gridy;

			Panel sigmaButtonPanel = new Panel( );

			editSigma = new Button( "Pick Sigma Manually" );
			editSigma.addActionListener( this );
			sigmaButtonPanel.add(editSigma);

			sigmaWizard = new Button( "Pick Sigma Visually" );
			sigmaWizard.addActionListener( this );
			sigmaButtonPanel.add(sigmaWizard);

			++ co.gridy;
			otherOptionsPanel.add(sigmaButtonPanel,co);

			c.gridx = 0;
			++ c.gridy;
			add(otherOptionsPanel,c);
		}

		{
			++ c.gridy;
			Panel hideWindowsPanel = new Panel();
			showOrHidePathList = new Button("Show / Hide Path List");
			showOrHidePathList.addActionListener(this);
			showOrHideFillList = new Button("Show / Hide Fill List");
			showOrHideFillList.addActionListener(this);
			hideWindowsPanel.add( showOrHidePathList );
			hideWindowsPanel.add( showOrHideFillList );
			c.fill = GridBagConstraints.HORIZONTAL;
			add( hideWindowsPanel, c );
		}

		{ /* The panel with options for saving, loading, network storage, etc. */

			Panel traceFileOptionsPanel = new Panel();

			traceFileOptionsPanel.setLayout(new GridBagLayout());

			GridBagConstraints ct = new GridBagConstraints();

			ct.gridy = 0;

			if( false ) {

				uploadButton = new Button("Upload Traces");
				uploadButton.addActionListener( this );
				fetchButton = new Button("Fetch Traces");
				fetchButton.addActionListener( this );
				ct.gridx = 0;
				traceFileOptionsPanel.add( uploadButton, ct );
				ct.gridx = 1;
				traceFileOptionsPanel.add( fetchButton, ct );

				++ ct.gridy;

			}

			c.gridx = 0;
			++ c.gridy;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.NONE;

			Panel otherImportExportPanel = new Panel();
			{
				loadLabelsButton = new Button("Load Labels");
				loadLabelsButton.addActionListener( this );

				exportCSVButton = new Button("Export as CSV");
				exportCSVButton.addActionListener( this );

				makeLineStackButton = new Button("Make Line Stack");
				makeLineStackButton.addActionListener( this );

				otherImportExportPanel.add(loadLabelsButton);
				otherImportExportPanel.add(exportCSVButton);
				otherImportExportPanel.add(makeLineStackButton);
			}
			add(otherImportExportPanel,c);

			++c.gridy;
			showCorrespondencesToButton = new Button("Show Correspondences to Traces...");
			showCorrespondencesToButton.addActionListener( this );
			add(showCorrespondencesToButton,c);

			saveButton = new Button("Save Traces File");
			saveButton.addActionListener( this );
			loadButton = new Button("Load Traces / SWC File");
			loadButton.addActionListener( this );
			ct.gridx = 0;
			ct.gridy = 1;
			traceFileOptionsPanel.add( saveButton, ct );
			ct.gridx = 1;
			ct.gridy = 1;
			traceFileOptionsPanel.add( loadButton, ct );

			c.gridx = 0;
			++ c.gridy;
			add(traceFileOptionsPanel,c);

		}

		/* Just add the quit button at the bottom... */

		quitButton = new Button("Quit Tracer");
		quitButton.addActionListener(this);
		c.gridx = 0;
		++ c.gridy;
		c.anchor = GridBagConstraints.CENTER;
		add(quitButton,c);

		pack();

		pw = new PathWindow(
			pathAndFillManager,
			plugin,
			getX() + getWidth(),
			getY() );

		fw = new FillWindow(
			pathAndFillManager,
			plugin,
			getX() + getWidth(),
			getY() + pw.getHeight() );

		changeState( WAITING_TO_START_PATH );
	}

	public void displayOnStarting( ) {
		setVisible( true );
		setPathListVisible( true );
		setFillListVisible( false );
	}

	public void showMouseThreshold( float t ) {
		String newStatus = null;
		if( t < 0 ) {
			newStatus = "Not reached by search yet";
		} else {
			newStatus = "Distance from path is: " + t;
		}
		fw.fillStatus.setText( newStatus );
	}

	public void actionPerformed( ActionEvent e ) {

		Object source = e.getSource();

		/* if( source == uploadButton ) {
			plugin.uploadTracings();
		} else if( source == fetchButton ) {
			plugin.getTracings( true );
		} else */ if( source == saveButton ) {

			FileInfo info = plugin.file_info;
			SaveDialog sd;

			if( info == null ) {

				sd = new SaveDialog("Save traces as...",
						    "image",
						    ".traces");

			} else {

				String fileName = info.fileName;
				String directory = info.directory;

				String suggestedSaveFilename;

				suggestedSaveFilename = fileName;

				sd = new SaveDialog("Save traces as...",
						    directory,
						    suggestedSaveFilename,
						    ".traces");
			}

			String savePath;
			if(sd.getFileName()==null) {
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
			}

			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Save traces file...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Saving traces to "+savePath);

			int preSavingState = currentState;
			changeState( SAVING );
			try {
				pathAndFillManager.writeXML( savePath, true );
			} catch( IOException ioe ) {
				IJ.showStatus("Saving failed.");
				IJ.error("Writing traces to '"+savePath+"' failed: "+ioe);
				changeState( preSavingState );
				return;
			}
			changeState( preSavingState );
			IJ.showStatus("Saving completed.");

			plugin.unsavedPaths = false;

		} else if( source == loadButton ) {

			if( plugin.pathsUnsaved() ) {
				YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Warning",
									     "There are unsaved paths. Do you really want to load new traces?" );

				if( ! d.yesPressed() )
					return;
			}

			int preLoadingState = currentState;
			changeState( LOADING );
			plugin.loadTracings();
			changeState( preLoadingState );

		} else if( source == exportCSVButton ) {

			FileInfo info = plugin.file_info;
			SaveDialog sd;

			if( info == null ) {

				sd = new SaveDialog("Export as CSV...",
						    "traces",
						    ".csv");

			} else {

				sd = new SaveDialog("Export as CSV...",
						    info.directory,
						    info.fileName,
						    ".csv");
			}

			String savePath;
			if(sd.getFileName()==null) {
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
			}

			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Export as CSV...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Exporting as CSV to "+savePath);

			int preExportingState = currentState;
			changeState( SAVING );
			// Export here...
			try {
				pathAndFillManager.exportToCSV(file);
			} catch( IOException ioe ) {
				IJ.showStatus("Exporting failed.");
				IJ.error("Writing traces to '"+savePath+"' failed: "+ioe);
				changeState( preExportingState );
				return;
			}
			IJ.showStatus("Export complete.");
			changeState( preExportingState );

		} else if( source == showCorrespondencesToButton ) {


			// Ask for the traces file to show correspondences to:

			String fileName = null;
			String directory = null;

			OpenDialog od;
			od = new OpenDialog("Select other traces file...",
					    directory,
					    null );

			fileName = od.getFileName();
			directory = od.getDirectory();

			if( fileName != null ) {

				File tracesFile = new File( directory, fileName );
				if( ! tracesFile.exists() ) {
					IJ.error("The file '"+tracesFile.getAbsolutePath()+"' does not exist.");
					return;
				}

				/* FIXME: test code: */

				// File tracesFile = new File("/media/LaCie/corpus/flybrain/Data/1/lo15r202.fitted.traces");
				// File fittedTracesFile = new File("/media/LaCie/corpus/flybrain/Data/1/LO15R202.traces");

				// plugin.showCorrespondencesTo( tracesFile, Color.YELLOW, 2.5 );
				// plugin.showCorrespondencesTo( fittedTracesFile, Color.RED, 2.5 );

				plugin.showCorrespondencesTo( tracesFile, Color.YELLOW, 2.5 );

				/* end of FIXME */

			}


		} else if( source == loadLabelsButton ) {

			plugin.loadLabels();

		} else if( source == makeLineStackButton ) {

			if( pathAndFillManager.size() == 0 ) {
				IJ.error("There are no paths traced yet - the stack would be empty");
			} else {
				plugin.makePathVolume();
			}

		} else if( source == cancelSearch ) {

			if( currentState == SEARCHING ) {
				statusText.setText("Cancelling path search...");
				plugin.cancelSearch( false );
			} else if( currentState == CALCULATING_GAUSSIAN ) {
				statusText.setText("Cancelling Gaussian generation...");
				plugin.cancelGaussian();
			} else {
				IJ.error("BUG! (wrong state for cancelling...)");
			}

		} else if( source == keepSegment ) {

			plugin.confirmTemporary( );

		} else if( source == junkSegment ) {

			plugin.cancelTemporary( );

		} else if( source == completePath ) {

			plugin.finishedPath( );

		} else if( source == cancelPath ) {

			plugin.cancelPath( );

		} else if( source == quitButton ) {

			exitRequested();

		}  else if( source == showOrHidePathList ) {

			togglePathListVisibility();

		}  else if( source == showOrHideFillList ) {

			toggleFillListVisibility();

		} else if( source == editSigma ) {

			double newSigma = -1;
			double newMultiplier = -1;
			while( newSigma <= 0 ) {
				GenericDialog gd = new GenericDialog("Select Scale of Structures");
				gd.addMessage("Please enter the approximate radius of the structures you are looking for:");
				gd.addNumericField("Sigma: ", plugin.getMinimumSeparation(), 4);
				gd.addMessage("(The default value is the minimum voxel separation.)");
				gd.addMessage("Please enter the scaling factor to apply:");
				gd.addNumericField("Multiplier: ", 4, 4);
				gd.addMessage("(If you're not sure, just leave this at 4.)");
				gd.showDialog();
				if( gd.wasCanceled() )
					return;

				newSigma = gd.getNextNumber();
				if( newSigma <= 0 ) {
					IJ.error("The value of sigma must be positive");
				}

				newMultiplier = gd.getNextNumber();
				if( newMultiplier <= 0 ) {
					IJ.error("The value of the multiplier must be positive");
				}
			}

			setSigma( newSigma, true );
			setMultiplier( newMultiplier );

		} else if( source == sigmaWizard ) {

			preSigmaPaletteState = currentState;
			changeState( WAITING_FOR_SIGMA_POINT );
		}
	}

	public void sigmaPaletteClosing() {
		changeState(preSigmaPaletteState);
		setSigma( currentSigma, true );
	}

	public void setPathListVisible(boolean makeVisible) {
		if( makeVisible ) {
			showOrHidePathList.setLabel("Hide Path List");
			pw.setVisible(true);
			pw.toFront();
		} else {
			showOrHidePathList.setLabel("Show Path List");
			pw.setVisible(false);
		}
	}

	public void togglePathListVisibility() {
		synchronized (pw) {
			setPathListVisible( ! pw.isVisible() );
		}
	}

	public void setFillListVisible(boolean makeVisible) {
		if( makeVisible ) {
			showOrHideFillList.setLabel("Hide Fill List");
			fw.setVisible(true);
			fw.toFront();
		} else {
			showOrHideFillList.setLabel("Show Fill List");
			fw.setVisible(false);
		}
	}

	public void toggleFillListVisibility() {
		synchronized (fw) {
			setFillListVisible( ! fw.isVisible() );
		}
	}

	public void thresholdChanged( double f ) {
		fw.thresholdChanged(f);
	}

	public boolean nearbySlices( ) {
		return ( viewPathChoice.getSelectedIndex() > 0 );
	}

	public void itemStateChanged( ItemEvent e ) {

		Object source = e.getSource();

		if( source == viewPathChoice ) {

			plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());

		} else if( source == preprocess ) {

			if( preprocess.getState() )
				turnOnHessian();
			else {
				plugin.enableHessian(false);
				// changeState(preGaussianState);
			}

		} else if( source == usePreprocessed ) {

			if( usePreprocessed.getState() ) {
				preprocess.setState(false);
			}

		}  else if( source == justShowSelected ) {

			plugin.setShowOnlySelectedPaths( justShowSelected.getState() );

		} else if( source == paths3DChoice ) {

			int selectedIndex = paths3DChoice.getSelectedIndex();
			plugin.setPaths3DDisplay( selectedIndex + 1 );

		} else if( source == colorImageChoice ) {
			checkForColorImageChange();
		}

	}

	public void paint(Graphics g) {
		super.paint(g);
	}

	boolean reportedInvalid;

	public int getEitherSide( ) {

		String s = nearbyField.getText();
		if( s.equals("") ) {
			reportedInvalid = false;
			return 0;
		}

		try {
			int e = Integer.parseInt( s );
			if( e < 0 ) {
				if( ! reportedInvalid ) {
					IJ.error("The number of slices either side cannot be negative.");
					reportedInvalid = true;
					return 0;
				}
			}
			reportedInvalid = false;
			return e;

		} catch( NumberFormatException nfe ) {
			if( ! reportedInvalid ) {
				IJ.error("The number of slices either side must be a non-negative integer.");
				reportedInvalid = true;
				return 0;
			}
			return 0;
		}

	}

	public void textValueChanged( TextEvent e ) {
		plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());
	}


	public void threadStatus( SearchThread source, int threadStatus ) {
		if (verbose) System.out.println("threadStatus reported as: "+threadStatus);
	}

	public void finished( SearchThread source, boolean success ) {
		// Unused
	}

	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// Unused
	}
}
