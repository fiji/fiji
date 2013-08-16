/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

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

import features.SigmaPalette;
import ij.gui.GenericDialog;

import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import ij.measure.Calibration;

import Skeletonize3D_.Skeletonize3D_;
import skeleton_analysis. AnalyzeSkeleton_;

@SuppressWarnings("serial")
public class NeuriteTracerResultsDialog
	extends JDialog
	implements ActionListener, WindowListener, ItemListener, TextListener, SigmaPalette.SigmaPaletteListener, ImageListener {

	public static final boolean verbose = SimpleNeuriteTracer.verbose;

	public PathWindow pw;
	public FillWindow fw;

	protected JMenuBar menuBar;
	protected JMenu fileMenu;
	protected JMenu analysisMenu;
	protected JMenu viewMenu;

	protected JMenuItem loadMenuItem;
	protected JMenuItem loadLabelsMenuItem;
	protected JMenuItem saveMenuItem;
	protected JMenuItem exportCSVMenuItem;
	protected JMenuItem exportAllSWCMenuItem;
	protected JMenuItem quitMenuItem;

	protected JMenuItem analyzeSkeletonMenuItem;
	protected JMenuItem makeLineStackMenuItem;
	protected JMenuItem exportCSVMenuItemAgain;
	protected JMenuItem sendToTrakEM2;
	protected JMenuItem shollAnalysiHelpMenuItem;

	protected JCheckBoxMenuItem mipOverlayMenuItem;
	protected JCheckBoxMenuItem drawDiametersXYMenuItem;

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
	static final int FITTING_PATHS            = 12;

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
					      "LOADING",
					      "FITTING_PATHS" };

	static final String SEARCHING_STRING = "Searching for path between points...";

	protected volatile int currentState;

	public int getCurrentState() {
		return currentState;
	}

	final protected SimpleNeuriteTracer plugin;

	protected JPanel statusPanel;
	protected JLabel statusText;
	protected JButton keepSegment, junkSegment;
	protected JButton cancelSearch;

	protected JPanel pathActionPanel;
	protected JButton completePath;
	protected JButton cancelPath;

	protected JComboBox viewPathChoice;
	protected String projectionChoice = "projected through all slices";
	protected String partsNearbyChoice = "parts in nearby slices";

	protected TextField nearbyField;

	protected PathColorsCanvas pathColorsCanvas;

	protected JComboBox colorImageChoice;
	protected String noColorImageString = "[None]";
	protected ImagePlus currentColorImage;

	protected JCheckBox justShowSelected;

	protected JComboBox paths3DChoice;
	protected String [] paths3DChoicesStrings = {
		"BUG",
		"as surface reconstructions",
		"as lines",
		"as lines and discs" };

	protected JCheckBox useTubularGeodesics;

	protected JCheckBox preprocess;
	protected JCheckBox usePreprocessed;

	protected volatile double currentSigma;
	protected volatile double currentMultiplier;

	protected JLabel currentSigmaAndMultiplierLabel;

	protected JButton editSigma;
	protected JButton sigmaWizard;

	protected JButton showCorrespondencesToButton;

	protected JButton uploadButton;
	protected JButton fetchButton;

	protected JButton showOrHidePathList;
	protected JButton showOrHideFillList;

	// ------------------------------------------------------------------------
	// Implementing the ImageListener interface:

	@Override
	public void imageOpened(ImagePlus imp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateColorImageChoice();
			}
		});
	}

	// Called when an image is closed
	@Override
	public void imageClosed(ImagePlus imp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateColorImageChoice();
			}
		});
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		/* This is called whenever ImagePlus.updateAndDraw
		  is called - i.e. potentially very often */
	}

	// ------------------------------------------------------------------------

	protected void updateStatusText(String newStatus) {
		assert SwingUtilities.isEventDispatchThread();
		statusText.setText("<html><strong>"+newStatus+"</strong></html>");
	}

	volatile boolean ignoreColorImageChoiceEvents = false;
	volatile boolean ignorePreprocessEvents = false;

	synchronized protected void updateColorImageChoice() {
		assert SwingUtilities.isEventDispatchThread();

		ignoreColorImageChoiceEvents = true;

		// Try to preserve the old selection:
		String oldSelection = (String) colorImageChoice.getSelectedItem();

		colorImageChoice.removeAllItems();

		int j = 0;
		colorImageChoice.addItem(noColorImageString);

		int selectedIndex = 0;

		int[] wList = WindowManager.getIDList();
		if (wList!=null) {
			for (int i=0; i<wList.length; i++) {
				ImagePlus imp = WindowManager.getImage(wList[i]);
				j++;
				String title = imp.getTitle();
				colorImageChoice.addItem(title);
				if (title == oldSelection)
					selectedIndex = j;
			}
		}

		colorImageChoice.setSelectedIndex(selectedIndex);

		ignoreColorImageChoiceEvents = false;

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

	synchronized protected void checkForColorImageChange() {
		String selectedTitle = (String) colorImageChoice.getSelectedItem();

		ImagePlus intendedColorImage = null;
		if( selectedTitle != null && ! selectedTitle.equals(noColorImageString) ) {
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
				if( ! (intendedColorImage.getWidth() == image.getWidth() &&
				       intendedColorImage.getHeight() == image.getHeight() &&
				       intendedColorImage.getStackSize() == image.getStackSize()) )
					IJ.error("Warning: the dimensions (in voxels) of '"+intendedColorImage.getTitle()+"' is different from the image you're tracing ('"+image.getTitle()+"')'\nThis may produce unexpected results.");
			}
			currentColorImage = intendedColorImage;
			plugin.setColorImage(currentColorImage);
		}
	}

	@Override
	public void newSigmaSelected( final double sigma ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setSigma( sigma, false );
			}
		});
	}

	@Override
	public void newMaximum( final double max ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				double multiplier = 256 / max;
				setMultiplier( multiplier );
			}
		});
	}

	// ------------------------------------------------------------------------

	volatile protected int preGaussianState;
	volatile protected int preSigmaPaletteState;

	public void gaussianCalculated(final boolean succeeded) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( !succeeded ) {
					ignorePreprocessEvents = true;
					preprocess.setSelected(false);
					ignorePreprocessEvents = false;
				}
				changeState(preGaussianState);
				if( preprocess.isSelected() ) {
					editSigma.setEnabled(false);
					sigmaWizard.setEnabled(false);
				} else {
					editSigma.setEnabled(true);
					sigmaWizard.setEnabled(true);
				}
			}
		});
	}

	public void setMultiplier( final double multiplier ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				currentMultiplier = multiplier;
				updateLabel( );
			}
		});
	}

	public void setSigma( final double sigma, final boolean mayStartGaussian ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				currentSigma = sigma;
				updateLabel( );
				if( mayStartGaussian ) {
					if( preprocess.isSelected() ) {
						IJ.error( "[BUG] The preprocess checkbox should never be on when setSigma is called" );
					} else {
						// Turn on the checkbox:
						ignorePreprocessEvents = true;
						preprocess.setSelected( true );
						ignorePreprocessEvents = false;
						/* ... according to the documentation
						   this doesn't generate an event, so
						   we manually turn on the Gaussian
						   calculation */
						turnOnHessian();
					}
				}
			}
		});
	}

	protected void turnOnHessian( ) {
		preGaussianState = currentState;
		plugin.enableHessian(true);
	}

	protected DecimalFormat threeDecimalPlaces = new DecimalFormat("0.0000");
	protected DecimalFormat threeDecimalPlacesScientific = new DecimalFormat("0.00E00");

	protected String formatDouble( double value ) {
		double absValue = Math.abs( value );
		if( absValue < 0.01 || absValue >= 1000 )
			return threeDecimalPlacesScientific.format(value);
		else
			return threeDecimalPlaces.format(value);
	}

	protected void updateLabel( ) {
		assert SwingUtilities.isEventDispatchThread();
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

	protected void exitRequested() {
		assert SwingUtilities.isEventDispatchThread();

		// FIXME: check that everything is saved...

		if( plugin.pathsUnsaved() ) {

			YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Really quit?",
								     "There are unsaved paths. Do you really want to quit?" );

			if( ! d.yesPressed() )
				return;

		}

		plugin.cancelSearch( true );
		plugin.notifyListeners(new SNTEvent(SNTEvent.QUIT));
		pw.dispose();
		fw.dispose();
		dispose();
		plugin.closeAndReset();
	}

	protected void disableEverything() {
		assert SwingUtilities.isEventDispatchThread();

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
		useTubularGeodesics.setEnabled(false);

		exportCSVMenuItem.setEnabled(false);
		exportAllSWCMenuItem.setEnabled(false);
		exportCSVMenuItemAgain.setEnabled(false);
		sendToTrakEM2.setEnabled(false);
		analyzeSkeletonMenuItem.setEnabled(false);
		saveMenuItem.setEnabled(false);
		loadMenuItem.setEnabled(false);
		if( uploadButton != null ) {
			uploadButton.setEnabled(false);
			fetchButton.setEnabled(false);
		}
		loadLabelsMenuItem.setEnabled(false);

		quitMenuItem.setEnabled(false);
	}

	public void changeState( final int newState ) {

		if (verbose) System.out.println("changeState to: "+stateNames[newState]);

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				switch( newState ) {

				case WAITING_TO_START_PATH:
					updateStatusText("Click somewhere to start a new path...");
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
					useTubularGeodesics.setEnabled(plugin.oofFileAvailable());

					editSigma.setEnabled( ! preprocess.isSelected() );
					sigmaWizard.setEnabled( ! preprocess.isSelected() );

					fw.setEnabledWhileNotFilling();

					loadLabelsMenuItem.setEnabled(true);

					saveMenuItem.setEnabled(true);
					loadMenuItem.setEnabled(true);
					exportCSVMenuItem.setEnabled(true);
					exportAllSWCMenuItem.setEnabled(true);
					exportCSVMenuItemAgain.setEnabled(true);
					sendToTrakEM2.setEnabled(plugin.anyListeners());
					analyzeSkeletonMenuItem.setEnabled(true);
					if( uploadButton != null ) {
						uploadButton.setEnabled(true);
						fetchButton.setEnabled(true);
					}

					quitMenuItem.setEnabled(true);

					break;

				case PARTIAL_PATH:
					updateStatusText("Now select a point further along that structure...");
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
					useTubularGeodesics.setEnabled(plugin.oofFileAvailable());

					editSigma.setEnabled( ! preprocess.isSelected() );
					sigmaWizard.setEnabled( ! preprocess.isSelected() );

					quitMenuItem.setEnabled(false);

					break;

				case SEARCHING:
					updateStatusText("Searching for path between points...");
					disableEverything();

					cancelSearch.setText("Abandon search");
					cancelSearch.setEnabled(true);
					cancelSearch.setVisible(true);
					keepSegment.setVisible(false);
					junkSegment.setVisible(false);

					completePath.setEnabled(false);
					cancelPath.setEnabled(false);

					quitMenuItem.setEnabled(true);

					break;

				case QUERY_KEEP:
					updateStatusText("Keep this new path segment?");
					disableEverything();

					keepSegment.setEnabled(true);
					junkSegment.setEnabled(true);

					cancelSearch.setVisible(false);
					keepSegment.setVisible(true);
					junkSegment.setVisible(true);

					break;

				case FILLING_PATHS:
					updateStatusText("Filling out from neuron...");
					disableEverything();

					fw.setEnabledWhileFilling();

					break;

				case FITTING_PATHS:
					updateStatusText("Fitting volumes around neurons...");
					disableEverything();
					break;

				case CALCULATING_GAUSSIAN:
					updateStatusText("Calculating Gaussian...");
					disableEverything();

					cancelSearch.setText("Cancel");
					cancelSearch.setEnabled(true);
					cancelSearch.setVisible(true);
					keepSegment.setVisible(true);
					junkSegment.setVisible(true);

					break;

				case WAITING_FOR_SIGMA_POINT:
					updateStatusText("Click on a neuron in the image");
					disableEverything();
					break;

				case WAITING_FOR_SIGMA_CHOICE:
					updateStatusText("Close the sigma palette window to continue");
					disableEverything();
					break;

				case LOADING:
					updateStatusText("Loading...");
					disableEverything();
					break;

				case SAVING:
					updateStatusText("Saving...");
					disableEverything();
					break;

				default:
					IJ.error("BUG: switching to an unknown state");
					return;
				}

				pack();

				plugin.repaintAllPanes();
			}
		});

		currentState = newState;
	}

	public int getState() {
		return currentState;
	}

	// ------------------------------------------------------------------------

	@Override
	public void windowClosing( WindowEvent e ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				exitRequested();
			}
		});
	}

	@Override
	public void windowActivated( WindowEvent e ) { }
	@Override
	public void windowDeactivated( WindowEvent e ) { }
	@Override
	public void windowClosed( WindowEvent e ) { }
	@Override
	public void windowOpened( WindowEvent e ) { }
	@Override
	public void windowIconified( WindowEvent e ) { }
	@Override
	public void windowDeiconified( WindowEvent e ) { }

	private PathAndFillManager pathAndFillManager;

	protected boolean launchedByArchive;

	public NeuriteTracerResultsDialog( String title,
					   SimpleNeuriteTracer plugin,
					   boolean launchedByArchive ) {

		super( IJ.getInstance(), title, false );
		assert SwingUtilities.isEventDispatchThread();

		new ClarifyingKeyListener().addKeyAndContainerListenerRecursively(this);

		this.plugin = plugin;
		final SimpleNeuriteTracer thisPlugin = plugin;
		this.launchedByArchive = launchedByArchive;

		pathAndFillManager = plugin.getPathAndFillManager();

		// Create the menu bar and menus:

		menuBar = new JMenuBar();

		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		analysisMenu = new JMenu("Analysis");
		menuBar.add(analysisMenu);

		viewMenu = new JMenu("View");
		menuBar.add(viewMenu);

		loadMenuItem = new JMenuItem("Load traces / SWC file...");
		loadMenuItem.addActionListener(this);
		fileMenu.add(loadMenuItem);

		loadLabelsMenuItem = new JMenuItem("Load labels file...");
		loadLabelsMenuItem.addActionListener(this);
		fileMenu.add(loadLabelsMenuItem);

		saveMenuItem = new JMenuItem("Save traces file...");
		saveMenuItem.addActionListener(this);
		fileMenu.add(saveMenuItem);

		exportCSVMenuItem = new JMenuItem("Export as CSV...");
		exportCSVMenuItem.addActionListener(this);
		fileMenu.add(exportCSVMenuItem);

		exportAllSWCMenuItem = new JMenuItem("Export all as SWC...");
		exportAllSWCMenuItem.addActionListener(this);
		fileMenu.add(exportAllSWCMenuItem);

		sendToTrakEM2 = new JMenuItem("Send to TrakEM2");
		sendToTrakEM2.addActionListener(this);
		fileMenu.add(sendToTrakEM2);

		quitMenuItem = new JMenuItem("Quit");
		quitMenuItem.addActionListener(this);
		fileMenu.add(quitMenuItem);

		analyzeSkeletonMenuItem = new JMenuItem("Run \"Analyze Skeleton\"");
		analyzeSkeletonMenuItem.addActionListener(this);
		analysisMenu.add(analyzeSkeletonMenuItem);

		makeLineStackMenuItem = new JMenuItem("Make Line Stack");
		makeLineStackMenuItem.addActionListener(this);
		analysisMenu.add(makeLineStackMenuItem);

		exportCSVMenuItemAgain = new JMenuItem("Export as CSV...");
		exportCSVMenuItemAgain.addActionListener(this);
		analysisMenu.add(exportCSVMenuItemAgain);

		shollAnalysiHelpMenuItem = new JMenuItem("Sholl Analysis help...");
		shollAnalysiHelpMenuItem.addActionListener(this);
		analysisMenu.add(shollAnalysiHelpMenuItem);

		String opacityLabel = "Show MIP overlay(s) at "+
			SimpleNeuriteTracer.OVERLAY_OPACITY_PERCENT+
			"% opacity";
		mipOverlayMenuItem = new JCheckBoxMenuItem(opacityLabel);
		mipOverlayMenuItem.addItemListener(this);
		viewMenu.add(mipOverlayMenuItem);

		drawDiametersXYMenuItem = new JCheckBoxMenuItem("Draw diameters in XY plane", plugin.getDrawDiametersXY());
		drawDiametersXYMenuItem.addItemListener(this);
		viewMenu.add(drawDiametersXYMenuItem);

		setJMenuBar(menuBar);

		addWindowListener(this);

		getContentPane().setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.insets = new Insets( 10, 10, 4, 10 );
		c.gridy = 0;
		c.weightx = 1;

		{ /* Add the status panel */

			statusPanel = new JPanel();
			statusPanel.setLayout(new BorderLayout());
			statusPanel.add(new JLabel("Instructions:"), BorderLayout.NORTH);
			statusText = new JLabel("");
			statusText.setOpaque(true);
			statusText.setForeground(Color.black);
			statusText.setBackground(Color.white);
			updateStatusText("Initial status text");
			statusText.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
			statusPanel.add(statusText,BorderLayout.CENTER);

			keepSegment = new JButton("Yes");
			junkSegment = new JButton("No");
			cancelSearch = new JButton("Abandon Search");

			keepSegment.addActionListener( this );
			junkSegment.addActionListener( this );
			cancelSearch.addActionListener( this );

			JPanel statusChoicesPanel = new JPanel();
			/*
			statusChoicesPanel.setLayout( new GridBagLayout() );
			GridBagConstraints cs = new GridBagConstraints();
			cs.weightx = 1;
			cs.gridx = 0; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(keepSegment,cs);
			cs.gridx = 1; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(junkSegment,cs);
			cs.gridx = 2; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(cancelSearch,cs);
			*/
			statusChoicesPanel.add(keepSegment);
			statusChoicesPanel.add(junkSegment);
			statusChoicesPanel.add(cancelSearch);
			statusChoicesPanel.setLayout(new FlowLayout());

			statusPanel.add(statusChoicesPanel,BorderLayout.SOUTH);

			getContentPane().add(statusPanel,c);
		}

		c.insets = new Insets( 4, 10, 10, 10 );

		{ /* Add the panel of actions to take on half-constructed paths */

			pathActionPanel = new JPanel();
			completePath = new JButton("Complete Path");
			cancelPath = new JButton("Cancel Path");
			completePath.addActionListener( this );
			cancelPath.addActionListener( this );
			pathActionPanel.add(completePath);
			pathActionPanel.add(cancelPath);

			++ c.gridy;
			getContentPane().add(pathActionPanel,c);
		}

		c.insets = new Insets( 10, 10, 10, 10 );

		{
			JPanel viewOptionsPanel = new JPanel();

			viewOptionsPanel.setLayout(new GridBagLayout());
			GridBagConstraints cv = new GridBagConstraints();
			cv.insets = new Insets(3, 2, 3, 2);
			cv.anchor = GridBagConstraints.LINE_START;
			viewPathChoice = new JComboBox();
			viewPathChoice.addItem(projectionChoice);
			viewPathChoice.addItem(partsNearbyChoice);
			viewPathChoice.addItemListener(this);

			JPanel nearbyPanel = new JPanel();
			nearbyPanel.setLayout(new BorderLayout());
			nearbyPanel.add(new JLabel("(up to"),BorderLayout.WEST);
			nearbyField = new TextField("2",2);
			nearbyField.addTextListener(this);
			nearbyPanel.add(nearbyField,BorderLayout.CENTER);
			nearbyPanel.add(new JLabel("slices to each side)"),BorderLayout.EAST);

			cv.gridx = 0;
			cv.gridy = 0;
			viewOptionsPanel.add(new JLabel("View paths (2D): "),cv);
			cv.gridx = 1;
			cv.gridy = 0;
			viewOptionsPanel.add(viewPathChoice,cv);

			paths3DChoice = new JComboBox();
			if( thisPlugin != null && thisPlugin.use3DViewer ) {
				for( int choice = 1; choice < paths3DChoicesStrings.length; ++choice )
					paths3DChoice.addItem(paths3DChoicesStrings[choice]);

				cv.gridx = 0;
				++ cv.gridy;
				viewOptionsPanel.add(new JLabel("View paths (3D): "),cv);
				cv.gridx = 1;
				viewOptionsPanel.add(paths3DChoice,cv);
			}
			paths3DChoice.addItemListener(this);

			cv.gridx = 1;
			++ cv.gridy;
			cv.gridwidth = 1;
			cv.anchor = GridBagConstraints.LINE_START;
			viewOptionsPanel.add(nearbyPanel, cv);

			JPanel flatColorOptionsPanel = new JPanel();
			flatColorOptionsPanel.setLayout(new BorderLayout());
			flatColorOptionsPanel.add(new JLabel("Click to change Path colours:"), BorderLayout.NORTH);
			pathColorsCanvas = new PathColorsCanvas(thisPlugin, 150, 18);
			flatColorOptionsPanel.add(pathColorsCanvas, BorderLayout.CENTER);

			JPanel imageColorOptionsPanel = new JPanel();
			imageColorOptionsPanel.setLayout(new BorderLayout());
			imageColorOptionsPanel.add(new JLabel("Use colors / labels from:"), BorderLayout.NORTH);

			colorImageChoice = new JComboBox();
			updateColorImageChoice();
			colorImageChoice.addActionListener(this);
			imageColorOptionsPanel.add(colorImageChoice, BorderLayout.CENTER);
			ImagePlus.addImageListener(this);

			cv.gridx = 0;
			++cv.gridy;
			cv.gridwidth = 2;
			viewOptionsPanel.add(flatColorOptionsPanel,cv);

			cv.gridx = 0;
			++ cv.gridy;
			cv.gridwidth = 2;
			viewOptionsPanel.add(imageColorOptionsPanel,cv);

			justShowSelected = new JCheckBox( "Show only selected paths" );
			justShowSelected.addItemListener( this );
			cv.gridx = 0;
			++ cv.gridy;
			cv.gridwidth = 2;
			cv.anchor = GridBagConstraints.LINE_START;
			cv.insets = new Insets( 0, 0, 0, 0 );
			viewOptionsPanel.add(justShowSelected,cv);

			++ c.gridy;
			getContentPane().add(viewOptionsPanel,c);
		}

		{ /* Add the panel with other options - preprocessing and the view of paths */

			JPanel otherOptionsPanel = new JPanel();

			otherOptionsPanel.setLayout(new GridBagLayout());
			GridBagConstraints co = new GridBagConstraints();
			co.anchor = GridBagConstraints.LINE_START;

			useTubularGeodesics = new JCheckBox("Use Tubular Geodesics");
			useTubularGeodesics.addItemListener( this );

			co.gridx = 0;
			++ co.gridy;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_START;
			otherOptionsPanel.add(useTubularGeodesics,co);

			preprocess = new JCheckBox("Hessian-based analysis");
			preprocess.addItemListener( this );

			co.gridx = 0;
			++ co.gridy;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_START;
			otherOptionsPanel.add(preprocess,co);

			++ co.gridy;
			usePreprocessed = new JCheckBox("Use preprocessed image");
			usePreprocessed.addItemListener( this );
			usePreprocessed.setEnabled( thisPlugin.tubeness != null );
			otherOptionsPanel.add(usePreprocessed,co);

			co.fill = GridBagConstraints.HORIZONTAL;

			currentSigmaAndMultiplierLabel = new JLabel();
			++ co.gridy;
			otherOptionsPanel.add(currentSigmaAndMultiplierLabel,co);
			setSigma( thisPlugin.getMinimumSeparation(), false );
			setMultiplier( 4 );
			updateLabel( );
			++ co.gridy;

			JPanel sigmaButtonPanel = new JPanel( );

			editSigma = new JButton( "Pick Sigma Manually" );
			editSigma.addActionListener( this );
			sigmaButtonPanel.add(editSigma);

			sigmaWizard = new JButton( "Pick Sigma Visually" );
			sigmaWizard.addActionListener( this );
			sigmaButtonPanel.add(sigmaWizard);

			++ co.gridy;
			otherOptionsPanel.add(sigmaButtonPanel,co);

			++ c.gridy;
			getContentPane().add(otherOptionsPanel,c);
		}

		{
			JPanel hideWindowsPanel = new JPanel();
			showOrHidePathList = new JButton("Show / Hide Path List");
			showOrHidePathList.addActionListener(this);
			showOrHideFillList = new JButton("Show / Hide Fill List");
			showOrHideFillList.addActionListener(this);
			hideWindowsPanel.add( showOrHidePathList );
			hideWindowsPanel.add( showOrHideFillList );
			c.fill = GridBagConstraints.HORIZONTAL;
			++ c.gridy;
			getContentPane().add( hideWindowsPanel, c );
		}

		pack();

		pw = new PathWindow(
			pathAndFillManager,
			thisPlugin,
			getX() + getWidth(),
			getY() );
		pathAndFillManager.addPathAndFillListener(pw);

		fw = new FillWindow(
			pathAndFillManager,
			thisPlugin,
			getX() + getWidth(),
			getY() + pw.getHeight() );
		pathAndFillManager.addPathAndFillListener(fw);

		changeState( WAITING_TO_START_PATH );
	}

	protected void displayOnStarting( ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setVisible( true );
				setPathListVisible( true );
				setFillListVisible( false );
			}
		});
	}

	public void showMouseThreshold( final float t ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String newStatus = null;
				if( t < 0 ) {
					newStatus = "Not reached by search yet";
				} else {
					newStatus = "Distance from path is: " + t;
				}
				fw.fillStatus.setText( newStatus );
			}
		});
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		assert SwingUtilities.isEventDispatchThread();

		Object source = e.getSource();

		/* if( source == uploadButton ) {
			plugin.uploadTracings();
		} else if( source == fetchButton ) {
			plugin.getTracings( true );
		} else */ if( source == saveMenuItem ) {

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

		} else if( source == loadMenuItem ) {

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

		} else if( source == exportAllSWCMenuItem ) {

			FileInfo info = plugin.file_info;
			SaveDialog sd;

			if( info == null ) {

				sd = new SaveDialog("Export all as SWC...",
						    "exported",
						    "");

			} else {

				String suggestedFilename;
				int extensionIndex = info.fileName.lastIndexOf(".");
				if (extensionIndex == -1)
					suggestedFilename = info.fileName;
				else
					suggestedFilename = info.fileName.substring(0, extensionIndex);

				sd = new SaveDialog("Export all as SWC...",
						    info.directory,
						    suggestedFilename+"-exported",
						    "");
			}

			String savePath;
			if(sd.getFileName()==null) {
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
			}
			IJ.error("got savePath: "+savePath);
			if( ! pathAndFillManager.checkOKToWriteAllAsSWC( savePath ) )
				return;
			pathAndFillManager.exportAllAsSWC( savePath );

		} else if( source == exportCSVMenuItem || source == exportCSVMenuItemAgain ) {

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

		} else if( source == sendToTrakEM2 ) {

			plugin.notifyListeners(new SNTEvent(SNTEvent.SEND_TO_TRAKEM2));

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


		} else if( source == loadLabelsMenuItem ) {

			plugin.loadLabels();

		} else if( source == makeLineStackMenuItem ) {

			if( pathAndFillManager.size() == 0 ) {
				IJ.error("There are no paths traced yet - the stack would be empty");
			} else {
				ImagePlus imagePlus = plugin.makePathVolume();
				imagePlus.show();
			}

		} else if( source == analyzeSkeletonMenuItem ) {

			if( pathAndFillManager.size() == 0 ) {
				IJ.error("There are no paths traced yet!");
			} else {
				ImagePlus imagePlus = plugin.makePathVolume();
				Skeletonize3D_ skeletonizer = new Skeletonize3D_();
				skeletonizer.setup("",imagePlus);
				skeletonizer.run(imagePlus.getProcessor());
				AnalyzeSkeleton_ analyzer = new AnalyzeSkeleton_();
				analyzer.setup("",imagePlus);
				analyzer.run(imagePlus.getProcessor());
				imagePlus.show();
			}

		} else if( source == shollAnalysiHelpMenuItem ) {

			IJ.runPlugIn("ij.plugin.BrowserLauncher", "http://fiji.sc/wiki/index.php/Simple_Neurite_Tracer:_Sholl_analysis");

		} else if( source == cancelSearch ) {

			if( currentState == SEARCHING ) {
				updateStatusText("Cancelling path search...");
				plugin.cancelSearch( false );
			} else if( currentState == CALCULATING_GAUSSIAN ) {
				updateStatusText("Cancelling Gaussian generation...");
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

		} else if( source == quitMenuItem ) {

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

		} else if( source == colorImageChoice ) {

			if( ! ignoreColorImageChoiceEvents )
				checkForColorImageChange();
		}
	}

	@Override
	public void sigmaPaletteClosing() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				changeState(preSigmaPaletteState);
				setSigma( currentSigma, true );
			}
		});
	}

	protected void setPathListVisible(boolean makeVisible) {
		assert SwingUtilities.isEventDispatchThread();
		if( makeVisible ) {
			showOrHidePathList.setText("Hide Path List");
			pw.setVisible(true);
			pw.toFront();
		} else {
			showOrHidePathList.setText("Show Path List");
			pw.setVisible(false);
		}
	}

	protected void togglePathListVisibility() {
		assert SwingUtilities.isEventDispatchThread();
		synchronized (pw) {
			setPathListVisible( ! pw.isVisible() );
		}
	}

	protected void setFillListVisible(boolean makeVisible) {
		assert SwingUtilities.isEventDispatchThread();
		if( makeVisible ) {
			showOrHideFillList.setText("Hide Fill List");
			fw.setVisible(true);
			fw.toFront();
		} else {
			showOrHideFillList.setText("Show Fill List");
			fw.setVisible(false);
		}
	}

	protected void toggleFillListVisibility() {
		assert SwingUtilities.isEventDispatchThread();
		synchronized (fw) {
			setFillListVisible( ! fw.isVisible() );
		}
	}

	public void thresholdChanged( double f ) {
		fw.thresholdChanged(f);
	}

	public boolean nearbySlices( ) {
		assert SwingUtilities.isEventDispatchThread();
		return ( viewPathChoice.getSelectedIndex() > 0 );
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		assert SwingUtilities.isEventDispatchThread();

		Object source = e.getSource();

		if( source == viewPathChoice ) {

			plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());

		} else if( source == useTubularGeodesics ) {

			plugin.enableTubularGeodesicsTracing(useTubularGeodesics.isSelected());

		} else if( source == preprocess && ! ignorePreprocessEvents) {

			if( preprocess.isSelected() )
				turnOnHessian();
			else {
				plugin.enableHessian(false);
				// changeState(preGaussianState);
			}

		} else if( source == usePreprocessed ) {

			if( usePreprocessed.isSelected() ) {
				preprocess.setSelected(false);
			}

		}  else if( source == justShowSelected ) {

			plugin.setShowOnlySelectedPaths( justShowSelected.isSelected() );

		} else if( source == paths3DChoice ) {

			int selectedIndex = paths3DChoice.getSelectedIndex();
			plugin.setPaths3DDisplay( selectedIndex + 1 );

		} else if( source == mipOverlayMenuItem ) {

			plugin.showMIPOverlays(e.getStateChange() == ItemEvent.SELECTED);

		} else if( source == drawDiametersXYMenuItem ) {
			plugin.setDrawDiametersXY(e.getStateChange() == ItemEvent.SELECTED);
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

	volatile boolean reportedInvalid;

	protected int getEitherSide( ) {
		assert SwingUtilities.isEventDispatchThread();

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

	@Override
	public void textValueChanged( TextEvent e ) {
		assert SwingUtilities.isEventDispatchThread();
		plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());
	}

}
