/*
   Copyright 2005, 2006 by Gerald Friedland, Kristian Jantz and Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.siox.example;

import java.io.*;
import java.util.*;
import javax.imageio.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

import org.siox.*;

/**
 * GUI panel to interactively segmentate an image with SIOX.
 *
 * @author Kristian Jantz, Gerald Friedland, Lars Knipping
 * @version 1.18
 */
public class ControlJPanel extends JPanel
{
	// CHANGELOG
	// 2005-12-09 1.19 added set ruler action
	// 2005-12-06 1.18 (re-)added enter key listener for apply/segmentate,
	//                 fix for ColorIcon dimension accessors,
	//                 fix for internal frame close
	// 2005-12-02 1.17 added support for internal frames
	// 2005-12-02 1.16 added disable/enable check for unsaved changes,
	//                 fixed canceled image display close
	// 2005-11-30 1.15 changed apply w/ right mouse button to jbutton/enter
	// 2005-11-29 1.14 introduced elliptic and lasso selection
	// 2005-11-29 1.13 moved some functionalities to new class ScrollDisplay,
	//                 added support for file/url open action,
	//                 removed status line
	// 2005-11-24 1.12 added set bg support
	// 2005-11-22 1.11 added zoom support and put imagepane into scroll pane
	// 2005-11-17 1.11 renamed class from TestAPI to ControlJPanel
	// 2005-11-16 1.10 changed brush default settings
	// 2005-11-14 1.09 applies drb to brushed area instead to mouse event
	//                 point only and allows disk shaped brush
	// 2005-11-14 1.08 replaced status flags by single int status, changed
	//                 fg/bg selection to brushing (user interaction similar
	//                 to SIOX in Gimp, original idea by Sven Neumann)
	// 2005-11-10 1.07 some GUI changes, moved main to new class Main, changed
	//                 class from JFrame to JPanel
	// 2005-11-09 1.06 some GUI changes, code outsourcing to CursorFactory
	// 2005-11-04 1.05 improved usability of test program
	// 2005-11-03 1.04 major GUI changes, added save reminder
	// 2005-11-03 1.03 performed some restructuring, optical improvements
	// 2005-11-03 1.02 added a few more comments
	// 2005-11-02 1.01 added a busy cursor when segmentating, turned multiple
	//                 object selector into a checkbox
	// 2005-10-25 1.00 initial release

	// constants defining interaction steps:

	/** Denotes no image being loaded yet. */
	private final static int NO_IMAGE_LOADED_STATUS = 0;
	/** Denotes image being loaded with region of interest not yet selected. */
	private final static int IMAGE_LOADED_STATUS = 1;
	/** Denotes user currently dragging selection to define region of interest. */
	private final static int DRAGGING_ROI_STATUS = 2;
	/** Denotes selection region of interest done but not yet applied. */
	private final static int ROI_CANDIDATE_STATUS = 3;
	/** Denotes region of interest defined. Next foreground is to be added. */
	private final static int ROI_DEFINED_STATUS = 4;
	/** Denotes some fg being added. More fg/bg can be added or segmentation started. */
	private final static int FG_ADDED_STATUS = 5;
	/** Denotes basic segmentation finished.  Allows detail refinement. */
	private final static int SEGMENTATED_STATUS = 6;

	// GUI Actions

	/** Action that applies the selection candiate. */
	private final Action applySelectionAction=new ApplySelectionAction();

	/** Action that performs the segmentation. */
	private final Action startSioxAction=new SegmentationAction();

	// GUI Components

	private final JPanel roiJPanel=new JPanel(new GridBagLayout());
	private final JPanel segJPanel=new JPanel(new GridBagLayout());
	private final JPanel drbJPanel=new JPanel(new GridBagLayout());
	private final JLabel selectModeJLabel=new JLabel("Selection mode:");
	private final JLabel smoothJLabel=new JLabel("Smoothing:");
	private final JLabel brushsizeJLabel=new JLabel("Brush size:");
	private final JLabel brushtypeJLabel=new JLabel("Brush shape:");
	private final JLabel fgOrBgJLabel=new JLabel("Add Known ");
	private final JButton applyJButton = new JButton(applySelectionAction);
	private final JButton segmentateJButton=new JButton(startSioxAction);
	private final JRadioButton fgJRadioButton=new JRadioButton("Foreground");
	private final JRadioButton bgJRadioButton=new JRadioButton("Background");
	private final JSlider smoothness=new JSlider(0, 10, 6);
	private final JSlider brushsize=new JSlider(0, 31, 14);
	private final JSlider addThreshold=new JSlider(0, 100, 100);
	private final JSlider subThreshold=new JSlider(0, 100, 0);
	private final JCheckBox multipart=new JCheckBox("Allow multiple foreground components", false);
	private final JRadioButton rectJRadioButton=new JRadioButton(ImagePane.RECTANGLE_SELECTION);
	private final JRadioButton ellipJRadioButton=new JRadioButton(ImagePane.ELLIPSE_SELECTION);
	private final JRadioButton lassoJRadioButton=new JRadioButton(ImagePane.LASSO_SELECTION);
	private final JRadioButton addJRadioButton=	new JRadioButton(SioxSegmentator.ADD_EDGE);
	private final JRadioButton subJRadioButton=new JRadioButton(SioxSegmentator.SUB_EDGE);
	private final JRadioButton diskJRadioButton=new JRadioButton("Disk");
	private final JRadioButton squareJRadioButton=new JRadioButton("Square");
	// component containing image:
	private ScrollDisplay scrollDisplay;
	private Component imageWindow; // JDialog or JInternalFrame
	private JDesktopPane jDesktopPane = null;  // nonull <-> images shown in internal frames

	// Internal variables

	/** WindowListeners added to any image display dialog. */
	private final ArrayList arrayListOfWindowListener = new ArrayList();
	/** InternalFrameListeners added to any internal frame with image display. */
	private final ArrayList arrayListOfJifListener = new ArrayList();
	/** One of the status constants, denotes current processing step. */
	private int status = NO_IMAGE_LOADED_STATUS;
	/** Should image displays be checked for unsaved changes before closing them? */
	private boolean checkForUnsavedChanges = true;

	/**
	 * Constructs a control panel for interactive SIOX segmentation on given image.
	 */
	public ControlJPanel()
	{
		super(new BorderLayout());

		final JPanel controlsBox=new JPanel(new GridBagLayout());

		roiJPanel.setBorder(BorderFactory.createTitledBorder("1. Select Region of Interest"));
		segJPanel.setBorder(BorderFactory.createTitledBorder("2. Initial Segmentation"));
		drbJPanel.setBorder(BorderFactory.createTitledBorder("3. Detail Refinement Brush"));

		final ButtonGroup selectModeButtonGroup=new ButtonGroup();
		selectModeButtonGroup.add(rectJRadioButton);
		selectModeButtonGroup.add(ellipJRadioButton);
		selectModeButtonGroup.add(lassoJRadioButton);
		rectJRadioButton.setSelected(true);
		final String selectModeTooltip= "Selection Mode for choosing Area of Interest.";
		selectModeJLabel.setToolTipText(selectModeTooltip);
		rectJRadioButton.setToolTipText(selectModeTooltip);
		ellipJRadioButton.setToolTipText(selectModeTooltip);
		lassoJRadioButton.setToolTipText(selectModeTooltip);
		roiJPanel.add(selectModeJLabel, getGbc(0, 0, 1, false, false));
		roiJPanel.add(rectJRadioButton, getGbc(1, 0, 1, false, false));
		roiJPanel.add(ellipJRadioButton, getGbc(2, 0, 1, false, false));
		roiJPanel.add(lassoJRadioButton, getGbc(3, 0, 1, false, false));
		final GridBagConstraints applyGc = getGbc(0, 3, 4, false, false);
		applyGc.anchor = GridBagConstraints.CENTER;
		roiJPanel.add(applyJButton, applyGc);

		final ButtonGroup fgOrBgButtonGroup=new ButtonGroup();
		fgOrBgButtonGroup.add(fgJRadioButton);
		fgOrBgButtonGroup.add(bgJRadioButton);
		fgJRadioButton.setSelected(true);
		final String fgOrBgTooltip=
		  "Add Selection as Known Foreground/Background.";
		fgOrBgJLabel.setToolTipText(fgOrBgTooltip);
		fgJRadioButton.setToolTipText(fgOrBgTooltip);
		bgJRadioButton.setToolTipText(fgOrBgTooltip);
		segJPanel.add(fgOrBgJLabel, getGbc(0, 0, 1, false, false));
		segJPanel.add(fgJRadioButton, getGbc(1, 0, 1, false, false));
		segJPanel.add(bgJRadioButton, getGbc(2, 0, 1, false, false));

		multipart.setToolTipText("Use All Foreground Components of at Least a Fourth of the Biggest Connected Component.");
		smoothness.setToolTipText("Number of Smoothing Cycles in Postprocessing.");
		smoothness.setPaintTicks(true);
		smoothness.setMinorTickSpacing(1);
		smoothness.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		segJPanel.add(multipart, getGbc(0, 1, 3, false, true));
		segJPanel.add(smoothJLabel, getGbc(0, 2, 3, false, true));
		segJPanel.add(smoothness, getGbc(1, 2, 2, false, true));
		final GridBagConstraints segGc = getGbc(0, 3, 3, false, false);
		segGc.anchor = GridBagConstraints.CENTER;
		segJPanel.add(segmentateJButton, segGc);

		final ButtonGroup drbButtonGroup=new ButtonGroup();
		drbButtonGroup.add(addJRadioButton);
		drbButtonGroup.add(subJRadioButton);
		final ActionListener drbModeListener = new ActionListener()
		  {
			  public void actionPerformed(ActionEvent e)
			  {
				  addThreshold.setEnabled(addJRadioButton.isSelected());
				  subThreshold.setEnabled(subJRadioButton.isSelected());
			  }
		  };
		addJRadioButton.addActionListener(drbModeListener);
		subJRadioButton.addActionListener(drbModeListener);
		subJRadioButton.setSelected(true);

		final String drbTooltip=
		  "Additive or Subtractive Alpha Brush to Improve Edges or Highly Detailed Regions.";
		addJRadioButton.setToolTipText(drbTooltip);
		subJRadioButton.setToolTipText(drbTooltip);
		addThreshold.setToolTipText("Threshold Defining Subpixel Granularity for Additive Refinement Brush.");
		subThreshold.setToolTipText("Threshold Defining Subpixel Granularity for Subrtractive Refinement Brush.");
		addThreshold.setPaintTicks(true);
		addThreshold.setMinorTickSpacing(5);
		addThreshold.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		subThreshold.setPaintTicks(true);
		subThreshold.setMinorTickSpacing(5);
		subThreshold.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		drbJPanel.add(subJRadioButton, getGbc(0, 1, 1, false, false));
		drbJPanel.add(subThreshold, getGbc(1, 1, 2, false, true));
		drbJPanel.add(addJRadioButton, getGbc(0, 2, 1, false, false));
		drbJPanel.add(addThreshold, getGbc(1, 2, 2, false, true));
		drbJPanel.add(Box.createVerticalStrut(6), getGbc(0, 3, 1, false, false)); // temp filler

		final JPanel brushJPanel = new JPanel(new GridBagLayout());
		final String brushsizeTooltip = "Size of Brush for Foregroung/Background and Detail Refinement.";
		brushsizeJLabel.setToolTipText(brushsizeTooltip);
		brushsize.setToolTipText(brushsizeTooltip);
		brushsize.setPaintTicks(true);
		brushsize.setMinorTickSpacing(1);
		brushsize.setMajorTickSpacing(10);
		brushsize.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		brushsize.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				updateBrushCursor();
			}
		});
		final String brushtypeTooltip = "Shape of Brush for Foregroung/Background and Detail Refinement.";
		brushtypeJLabel.setToolTipText(brushtypeTooltip);
		diskJRadioButton.setToolTipText(brushtypeTooltip);
		squareJRadioButton.setToolTipText(brushtypeTooltip);
		final ButtonGroup brushshapeButtonGroup=new ButtonGroup();
		brushshapeButtonGroup.add(diskJRadioButton);
		brushshapeButtonGroup.add(squareJRadioButton);
		final ActionListener brushshapebModeListener = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					updateBrushCursor();
				}
		  };
		diskJRadioButton.addActionListener(brushshapebModeListener);
		squareJRadioButton.addActionListener(brushshapebModeListener);
		diskJRadioButton.setSelected(true);
		brushJPanel.add(brushsizeJLabel, getGbc(0, 0, 1, false, false));
		brushJPanel.add(brushsize, getGbc(1, 0, 2, false, true));
		brushJPanel.add(brushtypeJLabel, getGbc(0, 1, 1, false, false));
		brushJPanel.add(diskJRadioButton, getGbc(1, 1, 1, false, false));
		brushJPanel.add(squareJRadioButton, getGbc(2, 1, 1, false, false));

		controlsBox.add(roiJPanel, getGbc(0, 0, 1, false, true));
		controlsBox.add(segJPanel, getGbc(0, 1, 1, false, true));
		controlsBox.add(drbJPanel, getGbc(0, 2, 1, false, true));
		controlsBox.add(brushJPanel, getGbc(0, 3, 1, false, true));

		add(controlsBox, BorderLayout.EAST);

		// keep track of image dialogs closed ...
		arrayListOfWindowListener.add(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					closeImage();
				}
		  });
		 arrayListOfJifListener.add(new InternalFrameAdapter()
			{
				public void internalFrameClosing(InternalFrameEvent e)
				{
					closeImage();
				}
		  });
		updateComponentEnabling();
	}

	/**
	 * Enables or disable checks for unsaved changes in
	 * <CODE>closeImage()</CODE>.
	 * <P>
	 * By default the check is enabled, but for environments, wherr
	 * save is notr possible (ie unsigned Applets) this may be undesirable.
	 */
	public void enableCheckForUnsavedChanges(boolean on) {
		this.checkForUnsavedChanges = on;
	}

	/**
	 * Closes the image display, if there is an open one.
	 * <P>
	 * The user may cancel this, if there are unsaved changes,
	 * unless.
	 * The method returns <CODE>true</CODE> on uncanceled close
	 * or if there is no opened image anyway.
	 */
	public boolean closeImage()
	{
		final Component window = imageWindow;
		final ScrollDisplay sd = scrollDisplay;
		if (window==null || sd == null) {
			return true;
		}
		if (checkForUnsavedChanges && hasUnsavedChanges()
			&& !showConfirmDialog(window, "Confirm",
								  "There are unsaved changes to the image.\n"
								  +"Discard changes?")) {
			return false;
		}
		imageWindow = null;
		scrollDisplay = null;
		status = NO_IMAGE_LOADED_STATUS;
		reset();
		if (window instanceof JDialog) {
			((JDialog) window).dispose();
		} else {
		  ((JInternalFrame) window).hide();
		  ((JInternalFrame) window).dispose();
		}
		return true;
	}

	/**
	 * Returns the desktop to open image displays in, whicha are then
	 * contained in internal frames.
	 * <P>
	 * If <CODE>null</CODE>(which is the default on creation), the
	 * images will be displayed in dialogs instead.
	 *
	 * @see #setDesktopPane
	 */
	public JDesktopPane getDesktopPane(JDesktopPane jDesktopPane) {
		return jDesktopPane;
	}

	/**
	 * Set a desktop to open image displays in, contained by internal frames.
	 * <P>
	 * If set to <CODE>null</CODE> (which is the default on creation), the
	 * images will be displayed in dialogs instead.
	 *
	 * @see #getDesktopPane
	 */
	public void setDesktopPane(JDesktopPane jDesktopPane) {
		this.jDesktopPane = jDesktopPane;
	}

	/**
	 * Add a <CODE>WindowListener</CODE> to any image display window (except
	 * internal frames), wether it is now open or opened in the future.
	 * <P>
	 * Can be used to track if any image is opened (in dialog, not in
	 * an internal frame).
	 *
	 * @see #getDesktopPane
	 * @see #setDesktopPane
	 */
	public void addImageWindowListener(WindowListener windowListener)
	{
		synchronized (arrayListOfWindowListener) {
			arrayListOfWindowListener.add(windowListener);
			final Component window = imageWindow;
			if (window instanceof Window) {
				((Window) window).addWindowListener(windowListener);
			}
		}
	}

	/**
	 * Removes a <CODE>WindowListener</CODE> for image dialogs.
	 */
	public boolean removeImageWindowListener(WindowListener windowListener)
	{
		synchronized (arrayListOfWindowListener) {
			final Component window = imageWindow;
			if (window instanceof Window) {
				((Window) window).removeWindowListener(windowListener);
			}
			return arrayListOfWindowListener.remove(windowListener);
		}
	}

	/**
	 * Add an <CODE>InternalFrameListener</CODE> to any internal frame
	 * for displaying an image.
	 * <P>
	 * Can be used to track if any image is opened (in an internal frame,
	 * not in a dialog).
	 *
	 * @see #getDesktopPane
	 * @see #setDesktopPane
	 */
	 public void addImageWindowListener(InternalFrameListener windowListener)
	{
		synchronized (arrayListOfJifListener) {
			arrayListOfJifListener.add(windowListener);
			final Component window = imageWindow;
			if (window instanceof JInternalFrame) {
				((JInternalFrame) window).addInternalFrameListener(windowListener);
			}
		}
	}

	/**
	 * Removes a <CODE>InternalFrameListener</CODE> for internal image frames.
	 */
	public boolean removeImageWindowListener(InternalFrameListener windowListener)
	{
		synchronized (arrayListOfJifListener) {
			final Component window = imageWindow;
			if (window instanceof JInternalFrame) {
				((JInternalFrame) window).removeInternalFrameListener(windowListener);
			}
			return arrayListOfJifListener.remove(windowListener);
		}
	}

	/**
	 * Opens the given image for editing into a dialog with the
	 * given title (<CODE>null</CODE>) for none) and displays the
	 * dialog.
	 */
	public void openImage(String title, BufferedImage image)
	{
		try {
			// unload old, if any
			final ScrollDisplay sd = scrollDisplay;
			if (sd != null) {
				reset();
				scrollDisplay = null;
				status = IMAGE_LOADED_STATUS;
			}
			final Component window = imageWindow;
			imageWindow = null;
			if (window instanceof JDialog) {
				((JDialog) window).dispose();
			} else if (window instanceof JInternalFrame) {
				((JInternalFrame) window).dispose();
			}

			final ScrollDisplay newScrollDisplay = new ScrollDisplay(image);
			status = IMAGE_LOADED_STATUS;
			final MouseInputListener mil = new SelectionHandler();
			newScrollDisplay.getImagePane().addMouseListener(mil);
			newScrollDisplay.getImagePane().addMouseMotionListener(mil);
			final JDesktopPane desktop = jDesktopPane;
			scrollDisplay = newScrollDisplay;
			if (desktop != null) { // open in internal frame
				final JInternalFrame jif = new JInternalFrame(title, true, true, true, false);
				jif.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
				synchronized (arrayListOfJifListener) {
				  final InternalFrameListener[] jifListeners =
					  (InternalFrameListener[]) arrayListOfJifListener.toArray(new InternalFrameListener[0]);
				  for (int i=0; i<jifListeners.length; ++i) {
					  jif.addInternalFrameListener(jifListeners[i]);
				  }
				}
				jif.getContentPane().add(newScrollDisplay);
				jif.pack();
				desktop.add(jif);
				jif.setLocation(50, 50);
				imageWindow = jif;
				jif.show();
			} else { // open in dialog
				final Container ancestor = getTopLevelAncestor();
				final Frame frame = (ancestor instanceof Frame) ? (Frame) ancestor : null;
				final JDialog jDialog = new JDialog(frame, title);
				jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				synchronized (arrayListOfWindowListener) {
				  final WindowListener[] winListeners =
					  (WindowListener[]) arrayListOfWindowListener.toArray(new WindowListener[0]);
				  for (int i=0; i<winListeners.length; ++i) {
					jDialog.addWindowListener(winListeners[i]);
				  }
				}
				jDialog.getContentPane().add(newScrollDisplay );
				jDialog.pack();
				imageWindow = jDialog;
				jDialog.show();
			}
			// allow select/segmentate with enter on dialog
			// (TODO: does not work for applets/internal frame - check/fix)
			imageWindow.setFocusable(true);
			imageWindow.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent keyEvent)
				{
					if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
						if (status == ROI_CANDIDATE_STATUS) {
							applyJButton.doClick();
						} else if (status == FG_ADDED_STATUS) {
							segmentateJButton.doClick();
						}
					}
				}
			  });
		} finally {
			updateComponentEnabling();
		}
	}

	/** Enables/disables GUI components according to current status. */
	private void updateComponentEnabling()
	{
		final Color onColor = UIManager.getColor("TitledBorder.titleColor");
		final Color offColor = UIManager.getColor("Label.disabledForeground");
		// panel for the select region of interest step:
		final boolean roiPhase = (status>=IMAGE_LOADED_STATUS) && (status<=ROI_CANDIDATE_STATUS);
		final boolean roiCandidatePhase = status==ROI_CANDIDATE_STATUS;
		final String colorRcName = roiPhase ? "Label.disabledForeground" : "TitledBorder.titleColor";
		((TitledBorder) roiJPanel.getBorder()).setTitleColor(roiPhase ? onColor : offColor);
		selectModeJLabel.setEnabled(roiPhase);
		rectJRadioButton.setEnabled(roiPhase);
		ellipJRadioButton.setEnabled(roiPhase);
		lassoJRadioButton.setEnabled(roiPhase);
		applySelectionAction.setEnabled(roiCandidatePhase);
		roiJPanel.repaint(); // update new border title color on screen
		// panel for the SIOX segmentation step:
		final boolean addPhase = (status==ROI_DEFINED_STATUS) || (status==FG_ADDED_STATUS);
		((TitledBorder) segJPanel.getBorder()).setTitleColor(addPhase ? onColor : offColor);
		startSioxAction.setEnabled(status == FG_ADDED_STATUS);
		smoothness.setEnabled(addPhase);
		multipart.setEnabled(addPhase);
		smoothJLabel.setEnabled(addPhase);
		fgOrBgJLabel.setEnabled(addPhase);
		fgJRadioButton.setEnabled(addPhase);
		// force fg selection when where no fg is defined yet:
		bgJRadioButton.setEnabled(status == FG_ADDED_STATUS);
		if (!bgJRadioButton.isEnabled()) {
			fgJRadioButton.setSelected(true);
		}
		segJPanel.repaint(); // update new border title color on screen
		// panel for the detail refinement step:
		final boolean drbPhase = (status == SEGMENTATED_STATUS);
		((TitledBorder) drbJPanel.getBorder()).setTitleColor(drbPhase? onColor : offColor);
		addJRadioButton.setEnabled(drbPhase);
		subJRadioButton.setEnabled(drbPhase);
		addThreshold.setEnabled(drbPhase && addJRadioButton.isSelected());
		subThreshold.setEnabled(drbPhase && subJRadioButton.isSelected());
		drbJPanel.repaint(); // update new border title color on screen
		// brush values
		final boolean brushActive = addPhase || drbPhase;
		brushsizeJLabel.setEnabled(brushActive);
		brushsize.setEnabled(brushActive);
		brushtypeJLabel.setEnabled(brushActive);
		squareJRadioButton.setEnabled(brushActive);
		diskJRadioButton.setEnabled(brushActive);
		if (status != NO_IMAGE_LOADED_STATUS) {
			updateBrushCursor();
		}
	}

	/** Set image dialogs cursor according to current editing state. */
	private void updateBrushCursor()
	{
		final Component window = imageWindow;
		final ScrollDisplay sd = scrollDisplay;
		if (window == null || sd == null) { // nothing to do
			return;
		}
		final boolean brushActive =
		  status>=ROI_DEFINED_STATUS && status<=SEGMENTATED_STATUS;
		if (brushActive) {
			final int sz = 2*brushsize.getValue()+1;
			final float scale = (float) sd.getImagePane().getZoomScale();
			final Cursor defCurs = Cursor.getDefaultCursor();
			final Cursor c = squareJRadioButton.isSelected()
			  ? CursorFactory.createFilledSquareCursor(sz, scale, defCurs)
			  : CursorFactory.createDiskCursor(sz, scale, defCurs);
			window.setCursor(c);
		} else {
			window.setCursor(Cursor.getDefaultCursor());
		}
	}

	/** Checks currently shown image for unsaved changes, if there is nay. */
	public boolean hasUnsavedChanges()
	{
		final ScrollDisplay sd = scrollDisplay;
		return sd!=null && sd.getImagePane().hasUnsavedChanges();
	}

	/**
	 * Stores current image as PNG.
	 *
	 * @param outfile Target to write the file to.
	 * @param needToConfirmOverwrite If true, user is queried before an
	 *        existing file is overwritten.
	 * @return True on successful save, false on write failure, user cancel,
	 *         or if there is currently no open image.
	 */
	public boolean storeCurrentImageTo(File outfile, boolean needToConfirmOverwrite)
	{
		final Component window = imageWindow;
		final ScrollDisplay sd = scrollDisplay;
		if (window==null || sd == null) { // no image to save
			Toolkit.getDefaultToolkit().beep();
			return false;
		}
		if (needToConfirmOverwrite && outfile.exists()
			&& !showConfirmDialog(window, "Confirm",
				  "File "+outfile.getName()+" exists already.\nOverwrite?")) {
			return false;
		}
		try {
			sd.getImagePane().storeCurrentImage(outfile);
		} catch (SecurityException e) {
			showErrorDialog(window, "I/O Error",
							"Not allowed to save image to "+outfile+".",
							false);
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			showErrorDialog(window, "I/O Error",
							"Saving image to "+outfile+" failed.", false);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/** Reset image, its selections, and the other GUI components. */
	public void reset()
	{
		final ScrollDisplay sd = scrollDisplay;
		fgJRadioButton.setSelected(true);
		subJRadioButton.setSelected(true);
		diskJRadioButton.setSelected(true);
		subThreshold.setValue(0);
		addThreshold.setValue(100);
		if (sd != null) {
			sd.setConf(0);
			sd.getImagePane().resetPane();
		}
		if (status != NO_IMAGE_LOADED_STATUS) {
			status = IMAGE_LOADED_STATUS;
		}
		updateComponentEnabling();
	}

	/**
	 *  Creates an action to zoom the shown image.
	 *
	 * @param factor the strictly positive zoom factor.
	 * @param zoomIn determies if the image should be shown zoomed in
	 *       (magnified) or zoomed out (scaled down).
	 */
	public Action createZoomAction(int factor, boolean zoomIn)
	{
		return new ZoomAction(factor, zoomIn);
	}

  /**
   * Creates an action for setting the background to the image,
   * ie marking transparent areas.
   *
   * @param tileIcon background will be set to given icon as tiles.
   *        A <CODE>null</CODE> will set the background to it default
   *        apperance.
   * @param name description of the background to be used as action
   *        name.
   * @param smallIcon optional small icon for action to be displayed

   *        in toolbars, menue items, etc, usually a scaled down
   *        version  of the tileIcon (as 24*24 or 16*16 pixel icon).
   */
	public Action createSetBgAction(Icon tileIcon, String name, Icon smallIcon)
	{
		return new SetBgAction(tileIcon, name, smallIcon);
	}

  /**
   * Creates an action for setting the background to the image,
   * ie marking transparent areas.
   *
   * @param color the color background will be set to.
   * @param name name of the color to be used as action name.
   */
	public Action createSetBgAction(Color color, String name)
	{
		final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
		final Icon tileIcon = new ColorIcon(color, size.width, size.height);
		final Icon smallIcon = new ColorIcon(color, 16, 16);
		return new SetBgAction(tileIcon, name, smallIcon);
	}


	/**
	 * Creates an action for setting the rulers for the currently shown
	 * image display.
	 *
	 * @param type the type of ruler to be shown, one of
	 *        <CODE>ScrollDisplay.NO_RULER</CODE>
	 *        <CODE>ScrollDisplay.EMPTY_RULER</CODE>,
	 *        <CODE>ScrollDisplay.METRIC_RULER</CODE>,
	 *        <CODE>ScrollDisplay.INCH_RULER</CODE>, and
	 *        <CODE>ScrollDisplay.PIXEL_RULER</CODE>.
	 * @see ScrollDisplay#NO_RULER
	 * @see ScrollDisplay#EMPTY_RULER
	 * @see ScrollDisplay#METRIC_RULER
	 * @see ScrollDisplay#INCH_RULER
	 * @see ScrollDisplay#PIXEL_RULER
	 */
	public Action createSetRulerAction(int type)
	{
		return new SetRulerAction(type);
	}

	/**
	 * Shows an error/warning dialog or internal frame for the given
	 * image display window.
	 */
	private static void showErrorDialog(Component window, String title, String msg, boolean warning)
	{
		final int type = warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
		if (window instanceof JInternalFrame) {
			JOptionPane.showInternalMessageDialog(window, msg, title, type);
		} else {
			JOptionPane.showMessageDialog(window, msg, title, type);
		}
	}

	/**
	 * Shows an OK/Cacels type conform dialog or internal frame for the given
	 * image display window.
	 */
	private static boolean showConfirmDialog(Component window, String title, String msg)
	{
		final int answer = (window instanceof JInternalFrame)
		  ? JOptionPane.showInternalConfirmDialog(window, msg, title,
												  JOptionPane.OK_CANCEL_OPTION,
												  JOptionPane.QUESTION_MESSAGE)
		  : JOptionPane.showConfirmDialog(window, msg, title,
										  JOptionPane.OK_CANCEL_OPTION,
										  JOptionPane.QUESTION_MESSAGE);
		return JOptionPane.OK_OPTION == answer;
	}

	/**
	 * Returns a gridbag constraint with the given parameters, standard
	 * L&amp;F insets and a west anchor.
	 */
	private static GridBagConstraints getGbc(int x, int y, int width,
											 boolean vFill, boolean hFill)
	{
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(6, 6, 5, 5);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		if (vFill) { // position may grow vertical
			c.fill = GridBagConstraints.VERTICAL;
			c.weighty = 1.0;
		}
		if (hFill) { // position may grow horizontally
			c.fill = hFill
			  ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
	  }
	  return c;
	}

	/////////////////////////////////////////////////////////////////////
	// INNER ACTION CLASSES
	/////////////////////////////////////////////////////////////////////

	/**
	 * Action that applies the selection candiate.
	 *
	 * Only enabled when a selection candidate is available.
	 */
	private class ApplySelectionAction extends AbstractAction
	{
		ApplySelectionAction()
		{
			super("Apply Selection");
			putValue(Action.SHORT_DESCRIPTION, "Apply Selection as Area of Interst.");
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final ScrollDisplay sd = scrollDisplay;
			if (sd == null || status != ROI_CANDIDATE_STATUS) { // "can"t happen"
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			sd.applySelection(0.5f);
			sd.getImagePane().clearSelection();
			status = ROI_DEFINED_STATUS;
			updateComponentEnabling();
		}
	}

	/**
	 * Action to start segmentation.
	 *
	 * Only enabled when area of interest and foreground are ready selected.
	 */
	private class SegmentationAction extends AbstractAction
	{

		SegmentationAction()
		{
			super("Segmentate");
			putValue(Action.SHORT_DESCRIPTION, "Start SIOX Segmentation.");
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final Component window = imageWindow;
			final ScrollDisplay sd = scrollDisplay;
			if (window==null || sd == null) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
			   sd.segmentate(smoothness.getValue(), multipart.isSelected());
			} catch (IllegalStateException e) {
				// should happen only iff all fg is deleted by later bg def
				showErrorDialog(window, "Sorry",
								"Segmentation needs nonempty foreground definition.",
								true);
				return;
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
			status = SEGMENTATED_STATUS;
			updateComponentEnabling();
		}
	}

	/**
	 * Action for setting a new zoom to the image pane, retaining the
	 * image center.
	 */
	private class ZoomAction extends AbstractAction
	{
		private final int zoomFactor;
		private final boolean zoomIn;

		ZoomAction(int factor, boolean zoomIn)
		{
			super(zoomIn ? (factor+":1") : ("1:"+factor));
			if (factor < 1)
				throw new IllegalArgumentException("nonpositive zoom factor: "
												   +factor);
			this.zoomFactor = factor;
			this.zoomIn = zoomIn;
			if (zoomFactor == 1)
			  putValue(Action.SHORT_DESCRIPTION,
					   "Zoom Shown Image.");
			else
			  putValue(Action.SHORT_DESCRIPTION, zoomIn
					   ? ("Zoom into Image by Factor "+zoomFactor+".")
					   : ("Zoom out of Image by Factor "+zoomFactor+"."));
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final ScrollDisplay sd = scrollDisplay;
			if (sd == null) {
				Toolkit.getDefaultToolkit().beep();
			} else {
				sd.setZoom(zoomFactor, zoomIn);
				updateBrushCursor(); // cursor zooms, too
			}
		}
	}

	/**
	 * Action for setting tiles as image background.
	 */
	private class SetBgAction extends AbstractAction
	{
		private final Icon tileIcon;

		/**
		 * Action for setting background.
		 * @param tileIcon background will be set to given icon as tiles.
		 *        A <CODE>null</CODE> will set the background to it default
		 *        apperance.
		 * @param name description of the background to be used as action
		 *        name.
		 * @param smallIcon optional small icon for action to be displayed
		 *        in toolbars, menue items, etc, usually a scaled down
		 *        version  of the tileIcon (as 24*24 or 16*16 pixel icon).
		 */
		SetBgAction(Icon tileIcon, String name, Icon smallIcon)
		{
			super(name);
			this.tileIcon = tileIcon;
			putValue(Action.SHORT_DESCRIPTION, "Set Image Background.");
			if (smallIcon != null)
				putValue(Action.SMALL_ICON, smallIcon);
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final ScrollDisplay sd = scrollDisplay;
			if (sd == null) {
				Toolkit.getDefaultToolkit().beep();
			} else {
				sd.getImagePane().setBackgroundTile(tileIcon);
			}
		}
	}

	/**
	 * Action for setting the ruler type to the current image display.
	 */
	private class SetRulerAction extends AbstractAction
	{
		/** Type of ruler to be set by this action. */
		private final int type;

		/** Action for setting the ruler type. */
		SetRulerAction(int type)
		{
			this.type = type;
			switch (type) {
			case ScrollDisplay.NO_RULER:
			  putValue(Action.NAME, "No Ruler"); break;
			case ScrollDisplay.EMPTY_RULER:
			  putValue(Action.NAME, "Empty Ruler"); break;
			case ScrollDisplay.METRIC_RULER:
			  putValue(Action.NAME, "Metric Ruler"); break;
			case ScrollDisplay.INCH_RULER:
			  putValue(Action.NAME, "Inch Ruler"); break;
			case ScrollDisplay.PIXEL_RULER:
			  putValue(Action.NAME, "Pixel Ruler"); break;
			default:
			  throw new IllegalArgumentException("invalid ruler type: "+type);
			}
			putValue(Action.SHORT_DESCRIPTION, "Set Image Display Ruler.");
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final Component window = imageWindow;
			final ScrollDisplay sd = scrollDisplay;
			if (window==null || sd==null) {
				Toolkit.getDefaultToolkit().beep();
			} else {
			  if (sd.setRuler(type)) {
				  if (window instanceof JDialog)
					  ((JDialog) window).pack();
				  else if (window instanceof JInternalFrame)
					  ((JInternalFrame) window).pack();
			  } else {
				  sd.repaint();
			  }

			}
		}
	}

	/////////////////////////////////////////////////////////////////////
	// INNER MOUSE LISTENER & ACTION CLASSES
	/////////////////////////////////////////////////////////////////////

	/**
	 * Class handling selections by mouse drags on the image pane.
	 */
	private class SelectionHandler extends MouseInputAdapter
	{

		/** Previous mouse position in drag actions. */
		private int lastX = -1, lastY = -1;

		/** Handles start of selection. */
		public void mousePressed(MouseEvent e)
		{
			if (e.getButton()==1) {
				final ImagePane imagePane = scrollDisplay.getImagePane();
				if (status==IMAGE_LOADED_STATUS || status==ROI_CANDIDATE_STATUS) {
					status = DRAGGING_ROI_STATUS;
					final int x = imagePane.unzoomX(e.getX());
					final int y = imagePane.unzoomY(e.getY());
					final String mode;
					if (rectJRadioButton.isSelected()) {
						mode = ImagePane.RECTANGLE_SELECTION;
					} else if (ellipJRadioButton.isSelected()) {
						mode = ImagePane.ELLIPSE_SELECTION;
					} else {
						mode = ImagePane.LASSO_SELECTION;
					}
					imagePane.startSelection(x, y, mode);
				} else if (status==ROI_DEFINED_STATUS || status==FG_ADDED_STATUS) {
					fgOrBgBrushing(imagePane.unzoomX(e.getX()), imagePane.unzoomY(e.getY()));
				} else if (status==SEGMENTATED_STATUS) {
					detailRefinementBrushing(imagePane.unzoomX(e.getX()), imagePane.unzoomY(e.getY()));
				}
			}
		}

		/** Handles selection during drag. */
		public void mouseDragged(MouseEvent e)
		{
			final ImagePane imagePane = scrollDisplay.getImagePane();
			final int x = imagePane.unzoomX(e.getX());
			final int y = imagePane.unzoomY(e.getY());
			if (status == DRAGGING_ROI_STATUS) {
				imagePane.selectTo(x, y);
			} else if (status==FG_ADDED_STATUS || status==ROI_DEFINED_STATUS) { // add fg/bg
				fgOrBgBrushing(x, y);
			} else if (status==SEGMENTATED_STATUS) { // apply detail refinement brush
				detailRefinementBrushing(x, y);
			}
		}

		/** Handles end of selection. */
		public void mouseReleased(MouseEvent e)
		{
			if (e.getButton()!=1)
				return;
			lastX = lastY = -1;
			if (status == DRAGGING_ROI_STATUS) {
				status = ROI_CANDIDATE_STATUS;
				updateComponentEnabling();
				final ImagePane imagePane = scrollDisplay.getImagePane();
				final int x = imagePane.unzoomX(e.getX());
				final int y = imagePane.unzoomY(e.getY());
				scrollDisplay.getImagePane().selectTo(x, y);
			} else if (status==ROI_DEFINED_STATUS || status==FG_ADDED_STATUS) {
				status = FG_ADDED_STATUS;
			}
		}

		// workaround for lost cursor in internal frame mode
		public void mouseEntered(MouseEvent e) {
			updateBrushCursor();
		}

		private void fgOrBgBrushing(int x, int y)
		{
			final int size = 2*brushsize.getValue()+1;
			if (lastX < 0) {
				lastX = x; lastY = y;
			}
			final Area area = squareJRadioButton.isSelected()
			  ? CursorFactory.getAreaBrushedByRect(lastX, lastY, x, y, size, size)
			  : CursorFactory.getAreaBrushedByDisk(lastX, lastY, x, y, size);
			final float alpha = fgJRadioButton.isSelected() ? 1.0f : 0.0f;
			scrollDisplay.setConf(area, alpha);
			lastX = x; lastY = y;
			if (status == ROI_DEFINED_STATUS) {
				status = FG_ADDED_STATUS;
				updateComponentEnabling();
			}
		}

		private void detailRefinementBrushing(int x, int y)
		{
			final int size = 2*brushsize.getValue()+1;
			if (lastX < 0) {
				lastX = x; lastY = y;
			}
			final Area area = squareJRadioButton.isSelected()
			  ? CursorFactory.getAreaBrushedByRect(lastX, lastY, x, y, size, size)
			  : CursorFactory.getAreaBrushedByDisk(lastX, lastY, x, y, size);
			final boolean add = addJRadioButton.isSelected();
			final float alpha = (add ? addThreshold : subThreshold).getValue() / 100.0f;
			scrollDisplay.subpixelRefine(area, add, alpha);
			lastX = x; lastY = y;
		}
	}

	/////////////////////////////////////////////////////////////////////
	// OTHER INNER CLASSES
	/////////////////////////////////////////////////////////////////////

	/* Single colored icon used for bg tiling. */
	private static class ColorIcon implements Icon {
		private final Color c;
		private final int w,h;

		ColorIcon(Color c, int w, int h)
		{
		  this.c = c;
		  this.w = w;
		  this.h = h;
		}

		public int getIconWidth()
		{
			return w;
		}

		public int getIconHeight()
		{
		  return h;
		}

		public void paintIcon(Component comp, Graphics graphics, int x, int y) {
			final Graphics g = graphics.create();
			g.setColor(c);
			g.fillRect(x, y, w, h);
			g.dispose();
		}
	}
}

