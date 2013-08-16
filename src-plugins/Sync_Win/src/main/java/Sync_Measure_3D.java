import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.measure.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


/** This PlugIn synchronizes the cursor and z-slice in all selected windows, and performs measurements on 3D ROIs in all selected windows.

With the PlugIn one can measure
- the chromatic shift of a microscope by measuring the intensity gravity centers
  of multispectral beads in all color channels simultaneously
- central positions, volumes and total intensities of objects in an image stack
- the 3D distance of several signals (e.g. genes) from a common reference point
  (e.g. the intensity gravity center of the nucleus).
- relative 3D distances of signals
- ...


Installation instructions:
Place Sync_Win.jar into the ImageJ plugins directory or a subdirectory and restart ImageJ. 
Sync Windows and Sync Measure 3D can be found under the menu entry Analyze>Tools.


Usage instructions:
- Load the data, each color channel as a separate 8-bit grayscale stack.

- Run Sync_Measure_3D (Analyze>Tools>Sync Measure 3D).

- Synchronize the windows, in which you want to measure by selecting them in the
  window list.

- Click "Start Measurements".

- Click on "Projection" to obtain a combined maximum intensity projection of all
  selected images (stacks). This projection is not used for measurements, but is
  synchronized with the other images and might be a convenient reference.

- Choose a threshold for each image.

- Measure gravity center, Volume, Intensity and distance to preceding stacks:
  o Draw a ROI around the particle you want to measure. If the particles in different
    channels are at different positions, turn "Synchronize Cursor" off for that.
  o If necessary (when other particles are inside the ROI in different z-slices),
    Select first and last slice to include this particle and exclude the other ones.
  o Double-click (left mousebutton) into the ROI to perform a measurement.
    + The X, Y and Z values are the coordinates of the intensity gravity centers
      of the measured objects.
    + The Volume and Intensity values are the volumes and the total intensities
      of the measured objects.
    + The Dist 1, Dist 2, ... values (for 2 or more image stacks) are the 3D distances
      of the intensity gravity center of the object in the current stack to the intensity
      gravity center of the object in the first, second, ... stack in the list.
    + If the scale of the image is not set, the results are displayed in "pixels".
      3D-distances are meaningless in this case, if the distance between slices
      differs from the pixelsize. If the image scale is set, the results are displayed
      in the selected unit.
    + Intensity values are given in gray values or the selected units in case that the
      image has been density calibrated. If the image is density calibrated, the centers
      of mass are calculated with the calibrated values, but the threshold is still a raw
      value (0-255 for 8-bit images).
  o To calculate mean values and standard deviations, click on "Stop Measurements".
    + For the x- y- and z- coordinates the differences to the coordinates of image stack #1
      are calculated ( (value for this stack) minus (value for stack #1) ) and their mean
      and standard deviation values are displayed. This helps in calculating the chromatic
      shift to stack #1.
    + For the Dist 1, Dist 2, ... values, simply the mean and the standard deviation are
      calculated.
 */

public class Sync_Measure_3D extends Sync_Windows {

/* Components that "this" plugin is registered to as ...

    ActionListener:
        Buttons inherited from Sync_Windows
        Button bMeasure
        Button bFirstSlice
        IntField intFirstSlice
        Button bLastSlice
        IntField intLastSlice
        Button bProjection
        All Scrollbars in vScrollThresh


    MouseListener and MouseMotionListener:
        Selected Windows in list (i.e. their canvasses; inherited from Sync_Windows)
        ImageCanvas of Projection window (if present)

    WindowListener:
        Projection window (if present)
	    ImageJ main window (inherited from Sync_Windows)

*/
    protected int DEFAULT_FIRST_SLICE = 1;
    protected int DEFAULT_LAST_SLICE = 999;

    protected boolean measuring = false;

    protected Button bMeasure;
    protected Panel measurePanel;
    
    private int mainPanelSize = 0;

    protected Vector vScrollThresh = null;
    protected final String I_THRESH_SLIDER = "iThresholdSlider";
    protected Vector vFirstButton = null;
    protected final String I_FIRST_SLICE_BUTTON = "iFirstSliceButton";
    protected Vector vFirstInt = null;
    protected int[] iFirstSlice = null;
    protected final String I_FIRST_SLICE_BOX = "iFirstSliceBox";
	protected Vector vLastButton = null;
    protected final String I_LAST_SLICE_BUTTON = "iLastSliceButton";
    protected Vector vLastInt = null;
	protected int[] iLastSlice = null;
    protected final String I_LAST_SLICE_BOX = "iLastSliceBox";

    protected Button bFirstSlice = null;
    protected IntField intFirstSlice = null;
    protected final String COMMON_FIRST_SLICE_BOX = "CommonFirstSliceBox";
    protected Button bLastSlice = null;
    protected IntField intLastSlice = null;
    protected final String COMMON_LAST_SLICE_BOX = "LastSliceBox";
    protected Button bProjection = null;

    protected boolean hasProjection = false;
    protected ImagePlus projection = null;

    protected M3DRTab resultsTable=null;
    protected M3DRWin resultsWindow=null;

    protected int nWindows = 0;

    /* Quantities to measure for each slice via Statistics class: area, center of mass in x and y,
        mean value (gives total intensity together with nBins. Limit the measurements to
        thresholded pixels. */
    protected int mOptions = ImageStatistics.AREA | ImageStatistics.CENTER_OF_MASS |
                        ImageStatistics.MEAN | ImageStatistics.LIMIT;
    protected int nQuantities = 6; // Measuring x, y, z, Volume, Intensity, Threshold
    protected int nRelQuantities = 1;   // Number of quantities, which are measured relative to all stacks
                                        // in the list before the current stack.
                                        // Stack n (0..N-1) has n relative measurement quantities, and
                                        // nQuantities*n + nRelQuantities*(n-1)*n/2 (arith. row) are before its first quantity.
                                        // Currently: 3D distance of gravity centers


    public Sync_Measure_3D() {
        this("Sync Measure 3D "+VERSIONSTRING);
    }
	public Sync_Measure_3D(String s) {
		super(s);
	}

    public void run(String args) {
        super.run(args);
    }

    protected Panel controlPanel() {
        Panel p = super.controlPanel();

        // Put button to start / stop Measurements in intermediate Panel that leaves
        // space for the measure controls.
        measurePanel = new Panel(new BorderLayout(0, 5));

        // Button to Start and Stop Measurements
        bMeasure = new Button("Start Measurements");
        bMeasure.addActionListener(this);
        measurePanel.add(bMeasure, BorderLayout.NORTH, 0);

        // Add intermediated panel ("measurePanel") at the lowest side of the main panel.
        p.add(measurePanel, BorderLayout.SOUTH, 2);
        return p;
    }

    /** Implementation of ActionListener interface. */
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        String command = e.getActionCommand(); 	

        // Determine if button for measurement was pressed.
        Object source = e.getSource();
        if (source instanceof Button) {
            Button bPressed = (Button)source;

            // Start / Stop measuring
            if (bPressed == bMeasure) {
                if (measuring == false && vwins != null && vwins.size() > 0) {
                    startMeasurements();
                } else if (measuring == true) {
                    // calculate shift from positions.
                    calcFinal();
                    stopMeasurements();
                }
            // button to mark common first slice
            } else if (bFirstSlice != null && bPressed == bFirstSlice) {
                int slice = WindowManager.getCurrentImage().getCurrentSlice();
                setCommonFirstSlice(slice);
            // button to mark common last slice
            } else if (bLastSlice != null && bPressed == bLastSlice) {
                int slice = WindowManager.getCurrentImage().getCurrentSlice();
                setCommonLastSlice(slice);
            // button to make projection
            } else if (bProjection != null && bPressed == bProjection) {
                if(hasProjection)
                    return;
                projection = computeProjection();
                projection.show();
                ImageCanvas projCanvas = projection.getWindow().getCanvas();
                vwins.addElement( new Integer(projection.getID()) );
                projCanvas.addMouseListener(this);
                projCanvas.addMouseMotionListener(this);
                projection.getWindow().addWindowListener(this);

                hasProjection = true;
            // button to mark first slice of one image
            } else if (command.startsWith(I_FIRST_SLICE_BUTTON)) {
            	int windowNumber = Integer.parseInt(command.substring(I_FIRST_SLICE_BUTTON.length() ));
                ImagePlus image =  getImageFromVector(windowNumber);
                if (image != null) {
	            	setFirstSlice(windowNumber, image.getCurrentSlice());
	            }	           
            // button to mark last slice of one image
            } else if (command.startsWith(I_LAST_SLICE_BUTTON)) {
            	int windowNumber = Integer.parseInt(command.substring(I_LAST_SLICE_BUTTON.length() ));
                ImagePlus image =  getImageFromVector(windowNumber);
                if (image != null) {
	            	setLastSlice(windowNumber, image.getCurrentSlice());
	            }	
            }


        // If value of one of the IntFields has changed.
        } else if (source instanceof IntField) {     
        	IntField field = (IntField) source;
            if (command.equals(COMMON_FIRST_SLICE_BOX)) {
                setCommonFirstSlice(field.getValue());
            } else if (command.equals(COMMON_LAST_SLICE_BOX)) {
                setCommonLastSlice(field.getValue());
            } else if (command.startsWith(I_FIRST_SLICE_BOX)) {
            	int windowNumber = Integer.parseInt(command.substring(I_FIRST_SLICE_BOX.length() ));
            	setFirstSlice(windowNumber, field.getValue());
            } else if (command.startsWith(I_LAST_SLICE_BOX)) {
            	int windowNumber = Integer.parseInt(command.substring(I_LAST_SLICE_BOX.length() ));
            	setLastSlice(windowNumber, field.getValue());	
        	} 
        	     
        // If value of one of the IntSliders (for threshold) has changed.
        } else if (source instanceof IntSlider) {
        	if (command.startsWith(I_THRESH_SLIDER)) {
	        	int windowNumber = Integer.parseInt(command.substring(I_THRESH_SLIDER.length() ));
	        	updateThreshold(windowNumber);
	        }
        }
    }

    public void startMeasurements() {
         // Update the window list to ensure that all selected windows indeed exist.
        updateWindowList();
        if (vwins == null)
            return;
        nWindows = vwins.size();
        if (nWindows == 0)
            return;
		
		// determine height of panel without measure controls
		mainPanelSize = panel.getBounds().height;            
        // build the panel with threshold sliders
        ScrollPane measureControl = buildMeasureControl();
        if (measureControl == null)
            return;
            
        // Do GUI stuff: add panel with sliders and panel with min/max and projection buttons.
        bMeasure.setLabel("Stop Measurements");
        setControlsEnabled(false);
        measurePanel.add(measureControl, BorderLayout.CENTER, 1);
        measurePanel.add(buildCommonControls(), BorderLayout.SOUTH, 2);
        pack();
        
        // Make ResultsTable.
        resultsTable = new M3DRTab(nQuantities*nWindows + nRelQuantities*(nWindows-1)*nWindows/2);

        // Set Flag.
        measuring = true;
        // Set Threshold in images.
        updateScrollbars();
    }

/** Removes contact to everything that is not further needed. */
    public void stopMeasurements() {
        bMeasure.setLabel("Start Measurements");
        setControlsEnabled(true);

        /* Remove all links to controls in the measurePanel to help garbage collection. */

        // Scrollbars
        for (int n=0; n<nWindows; ++n) {
            ((IntSlider)vScrollThresh.elementAt(n)).removeActionListener(this);
            ((Button)vFirstButton.elementAt(n)).removeActionListener(this);
            ((IntField)vFirstInt.elementAt(n)).removeActionListener(this);

            ((Button)vLastButton.elementAt(n)).removeActionListener(this);
			((IntField)vLastInt.elementAt(n)).removeActionListener(this);

            ImagePlus image = getImageFromVector(n);
			if (image != null) {
                image.getProcessor().resetThreshold();
                image.updateAndDraw();
        	}
        }
        vScrollThresh = null;
        vFirstButton = null;
        vFirstInt = null;
		vLastButton = null;
		vLastInt = null;

        // buttons and int fields of the common controls
        bFirstSlice.removeActionListener(this);
        bFirstSlice = null;
        intFirstSlice.removeActionListener(this);
        intFirstSlice = null;

        bLastSlice.removeActionListener(this);
        bLastSlice = null;
        intLastSlice.removeActionListener(this);
        intLastSlice = null;

        bProjection.removeActionListener(this);
        bProjection = null;

        // Adapt GUI
        nWindows = 0;
        measurePanel.remove(1);
        measurePanel.remove(1);
        pack();

        // Stop contact to resultsWindow.
		if (resultsWindow != null) {
            resultsWindow.nullParent();
            resultsWindow = null;
    	}

        // remove projection window
	    if (hasProjection && projection != null) {
            projection.getWindow().setVisible(false);
            projection.getWindow().close();
            WindowManager.removeWindow(projection.getWindow());
            disconnectProjection();
            projection = null;
            hasProjection = false;
        }
        	
        // Unset flag.
        measuring = false;
        
        // Check if windows have been opened or closed, meanwhile.
        updateWindowList();
    }


/* Check if the value of the "FirstSlice" field is valid and set it. */
    void setFirstSlice(int iWin, int slice) {
        int firstSlice;
        int lastSlice = iLastSlice[iWin];

        if ( slice >= 1 && slice <= lastSlice) {
            firstSlice = slice;
        } else if (slice > lastSlice) {
            firstSlice = lastSlice;
        } else { //(slice < 1)
            firstSlice = 1;
        }
        iFirstSlice[iWin] = firstSlice;
        ((IntField)vFirstInt.elementAt(iWin)).setValue(firstSlice);
    }

/* Check if value of the "LastSlice" field is valid and set it. */
    void setLastSlice(int iWin, int slice) {
        ImagePlus image = getImageFromVector(iWin);
        if (image == null)
            return;
        int stackSize = image.getStackSize();
        int firstSlice = iFirstSlice[iWin];
        int lastSlice;

        if ( slice >= firstSlice && slice <= stackSize) {
            lastSlice = slice;
        } else if (slice > stackSize) {
            lastSlice = stackSize;
        } else { // (slice < firstSlice)
            lastSlice = firstSlice;
        }
        iLastSlice[iWin] = lastSlice;
        ((IntField)vLastInt.elementAt(iWin)).setValue(lastSlice);
    }

/* Check if the value of the "FirstSlice" field is valid. */
    void setCommonFirstSlice(int slice) {
        int firstSlice;
        int lastSlice = intLastSlice.getValue();
        if ( slice >= 1 && slice <= lastSlice) {
            firstSlice = slice;
        } else if (slice > lastSlice) {
            firstSlice = lastSlice;
        } else { // (slice < 1)
            firstSlice = 1;
        }
        intFirstSlice.setValue(firstSlice);

        for (int n=0; n<nWindows; ++n) {
            setLastSlice(n, lastSlice);
            setFirstSlice(n, firstSlice);
        }
    }

/* Check if the value of the "LastSlice" field is valid. */
    void setCommonLastSlice(int slice)  {
        int lastSlice;
        int firstSlice = intFirstSlice.getValue();
        if ( slice >= firstSlice ) {
            lastSlice = slice;
        } else { // (slice < firstSlice)
            lastSlice = firstSlice;
        }
        intLastSlice.setValue(lastSlice);

        for (int n=0; n<nWindows; ++n) {
            setFirstSlice(n, firstSlice);
            setLastSlice(n, lastSlice);
        }
    }

// WindowListener classes, which react, if the projection is closed.
    public void windowClosing(WindowEvent e) {
        if (e.getSource() == this) {
			if(!measuring)
                super.windowClosing(e);
                
        } else if (projection != null && e.getSource() == projection.getWindow()) {
            disconnectProjection();
            
        }
    }

    public void windowActivated(WindowEvent e) {
        if (e.getSource() == this) {
            super.windowActivated(e);
        }
    }

// Process mouse pressed events (pressing mouse into ROI initiates measurement)
    public void mousePressed(MouseEvent e) {

        // get modifier (which button is clicked)
        boolean button1 = (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0;

        // get ImageCanvas that received event
        ImageCanvas icc = (ImageCanvas) e.getSource();
        ImageWindow iwc = (ImageWindow) icc.getParent();
        ImagePlus image = iwc.getImagePlus();

		int x = icc.offScreenX(e.getX());
		int y = icc.offScreenY(e.getY());

		// If click was double-click into ROI, measure.
		if ( measuring && button1 && e.getClickCount()==2 &&  image.getRoi() != null && 
			image.getRoi().contains(x, y) ) {
			measure(image, x, y);
			return;
		}        
		super.mousePressed(e);      
    }

    /** Implementation of ImageListener interface: update window list, if image is opened or closed */
    public void imageOpened(ImagePlus imp) {
        if (!measuring)
            updateWindowList();
    }

    /** Implementation of ImageListener interface: update window list, if image is opened or closed */
    public void imageClosed(ImagePlus imp) {
        if (!measuring)
            updateWindowList();
    }

/**  Builds the Panel with Scrollbars to adjust the threshold for each image and
 *   with buttons to select the first and the last slice. The Panel is embedded
 *   into a ScrollPane, which has a maximum height of 380 pixels. Up to 5 images
 *   can be controlled without scrolling. */
    protected ScrollPane buildMeasureControl() {

        vScrollThresh = new Vector(nWindows);
        vFirstButton = new Vector(nWindows);
        vFirstInt = new Vector(nWindows);
        iFirstSlice = new int[nWindows];
        vLastButton = new Vector(nWindows);
        vLastInt = new Vector(nWindows);
        iLastSlice = new int[nWindows];

        int minThresh = 0;
        int maxThresh = 255;

        Panel p = new Panel(new GridLayout(nWindows, 1, 0, 4));
        Font font = new Font("SansSerif", Font.PLAIN, 10);
        
        // loop over all selected windows and make a threshold slider and 
        // first/last slice controls
        for (int n=0; n < nWindows; ++n) {
            // Get Image and act according to image type.
            ImagePlus image = getImageFromVector(n);
            if (image == null)
                continue;
			ImageStack imageS = image.getStack();

		// find min and max threshold for current image
            int type = image.getType();
            switch (type) {
/* This plugin is not planned to work with color images. 
To make it work with 16-bit and 32-bit images two things have to be done:
1. The Min/Max values of the sliders have to be set to the Min/Max of the whole stack (not just the current image).
    => go through all slices and find Min and Max.
2. Constraining measurements to thresholds currently does not work with 32-bit images. To make it work:
   Either solve the problem in ImageJ and submit to the distribution, or measure "by hand" (i.e. by looping through
   the arrays that store the image data, see if each point is within the ROI (mask!) and above threshold.).
   Since recently, 16-bit images can constrain measurements to thresholded regions. So one problem less.
*/
                case ImagePlus.COLOR_256:
                case ImagePlus.COLOR_RGB:
                case ImagePlus.GRAY32:
                    vScrollThresh = null;
                    vFirstButton = null;
                    vFirstInt = null;
                    iFirstSlice = null;
                    vLastButton = null;
                    vLastInt = null;
                    iLastSlice = null;
                    IJ.error("This plugin only works with 8-bit and 16-bit images.");
                    return null;
                case ImagePlus.GRAY16:
					// find min and max intensities in stack.
					minThresh = 65535;
					maxThresh = 0;
					for (int i = 1; i<=image.getStackSize(); i++) {
						short pix[] = (short[])imageS.getPixels(i);
						for (int j = 0; j<pix.length; j++) {
							int pixel = 0xffff&pix[j];
							if(pixel<minThresh) {
								minThresh = pixel;
							}
							if(pixel>maxThresh) {
								maxThresh = pixel;
							}
						}
					}
					break;
                case ImagePlus.GRAY8:
                    minThresh = 0;
                    maxThresh = 255;
                    break;
                default:
                    IJ.error("Unknown image type.");
                    return null;
            }
            
            
            // make the panel, which contains threshold slider and slice controls
            Panel windowPanel = new Panel(new BorderLayout(4,0));
            windowPanel.setFont(font);
            windowPanel.add(new Label(image.getTitle()), BorderLayout.NORTH, 0);

			// make threshold slider and add to position 1 of panel
            IntSlider tScroll = new IntSlider(minThresh, minThresh, maxThresh+1, IntSlider.HORIZONTAL, 1, IntSlider.EAST, I_THRESH_SLIDER+Integer.toString(n));
			windowPanel.add(tScroll, BorderLayout.CENTER, 1);
            tScroll.addActionListener(this);
            vScrollThresh.addElement((Object) tScroll); 
                       
            // make selectors for first and last slice and add to position 2 of panel
            Panel zPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            Button bFirst = new Button("First  Z");
            bFirst.setActionCommand(I_FIRST_SLICE_BUTTON+Integer.toString(n));
            bFirst.addActionListener(this);
            zPanel.add(bFirst);
            vFirstButton.addElement((Object) bFirst);
            IntField iFirst = new IntField(1, 1, Integer.MAX_VALUE, 4, I_FIRST_SLICE_BOX+Integer.toString(n));
            iFirstSlice[n] = 1;
            iFirst.addActionListener(this);
    		zPanel.add(iFirst);
            vFirstInt.addElement((Object) iFirst);
            
            Button bLast = new Button("Last  Z");
            bLast.addActionListener(this);
            bLast.setActionCommand(I_LAST_SLICE_BUTTON+Integer.toString(n));
            zPanel.add(bLast);
            vLastButton.addElement((Object) bLast);
            IntField iLast = new IntField(image.getStackSize(), 1, Integer.MAX_VALUE, 4, I_LAST_SLICE_BOX+Integer.toString(n));
            iLastSlice[n] = image.getStackSize();
            iLast.addActionListener(this);
            zPanel.add(iLast);
            vLastInt.addElement((Object) iLast);
            
            windowPanel.add(zPanel, BorderLayout.SOUTH, 2);
            
            p.add(windowPanel, n);
        }
        
// Determine size of Panel. For that, it has to be added 
// to a window and the window to be packed.
// Still the dimensions determined by that method don't 
// match the dimensions of the panel in the ScrollPane 
// (hence add 4 to the height).
        Frame win = new Frame();
        win.add(p);
        win.pack();
        Rectangle bounds = p.getBounds(); 
        win.dispose();

// pane for more than 5 images has to be scrolled
        if (bounds.height > 380) {
            bounds.height = 380;
        }
// determine how much space is   left on the screen and set height accordingly
		int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;        
		// height of common controls: 44, add some for safety, titel bar...
		int restHeight = screenHeight-mainPanelSize-44-55;
		if (restHeight<1) {
			restHeight = 1;	
		}
		if (bounds.height>restHeight) {
			bounds.height = restHeight;	
		}
		        
        ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        pane.add(p);
        pane.setSize(100, bounds.height+4);
        pane.getVAdjustable().setUnitIncrement(20);
        return pane;
    }

/**  Builds the Panel with buttons and textfields for first and last slice and for projection. */
    protected Panel buildCommonControls() {
        Panel p1 = new Panel(new BorderLayout(6, 0));

        Panel upDownSlicePanel=new Panel(new GridLayout(0,4,2,0));
        bFirstSlice = new Button("First  Z");
        upDownSlicePanel.add(bFirstSlice);
        bFirstSlice.addActionListener(this);
        intFirstSlice = new IntField(DEFAULT_FIRST_SLICE, 1, Integer.MAX_VALUE, 4, COMMON_FIRST_SLICE_BOX);
        upDownSlicePanel.add(intFirstSlice);
        intFirstSlice.addActionListener(this);

        bLastSlice = new Button("Last  Z");
        upDownSlicePanel.add(bLastSlice);
        bLastSlice.addActionListener(this);
        intLastSlice = new IntField(DEFAULT_LAST_SLICE, 1, Integer.MAX_VALUE, 4, COMMON_LAST_SLICE_BOX);
        upDownSlicePanel.add(intLastSlice);
        intLastSlice.addActionListener(this);

        bProjection = new Button("Projection");
        bProjection.addActionListener(this);

        p1.add(upDownSlicePanel, BorderLayout.CENTER);
        p1.add(bProjection, BorderLayout.EAST);
        
        Label infoLabel = new Label("Double-click into ROI to measure!");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p1.add(infoLabel, BorderLayout.SOUTH);
        return p1;

    }

/** greys off and on the control elements for adding and removing windows */
    protected void setControlsEnabled(boolean enable) {
        if (wList != null) wList.setEnabled(enable);
        bSyncAll.setEnabled(enable);
        bUnsyncAll.setEnabled(enable);
    }

/** Sets the threshold values in the images from the scrollbar positions. */
    protected void updateScrollbars() {
        if (! measuring)
            return;

        for (int n=0; n < nWindows; ++n) {
        	updateThreshold(n);

        }
    }
    
/** Sets the threshold value in the image window iWin from the scrollbar position. */
    protected void updateThreshold(int iWin) {
        if (! measuring)
            return;
		if (iWin<0 || iWin >= nWindows) 
			return;
			
        double threshold = 0.0;
        
         // Get value of Scrollbar object
        threshold = (double) ((IntSlider)vScrollThresh.elementAt(iWin)).getValue();
        // Get Image and act according to image type.
        ImagePlus image = getImageFromVector(iWin);
        if (image == null)
            return;
        ImageProcessor imageP = image.getProcessor();
        int type = image.getType();
        switch (type) {
            case ImagePlus.GRAY8:
                imageP.setThreshold(threshold, 255.0, ImageProcessor.OVER_UNDER_LUT);
                break;
            case ImagePlus.GRAY16:
                imageP.setThreshold(threshold, 65535.0, ImageProcessor.OVER_UNDER_LUT);
                break;
        } // switch
        image.updateAndDraw();          	
    }

/** This method does the actual measuring work.
 *  Spatial units are pixels (by default) or the selected units in case that the scale of the
 *  image has been set. Intensity values are given in gray values or the selected units in case
 *  that the image has been density calibrated. If the image is density calibrated, the centers
 *  of mass are calculated with the calibrated values. The threshold, however, is always a raw
 *  value (0-255 for 8-bit images).
 *  This behaviour is consistent with the behaviour of the "measure" function of ImageJ. 
 *  The parameters image, x, y are not used here, but are usefull for extending Sync_Measure_3D. */
    protected void measure(ImagePlus image, int x, int y) {

        boolean nullWindows = false;
        resultsTable.incrementCounter();

        for (int n=0; n<nWindows; ++n) {
            ImagePlus imp = getImageFromVector(n);

            // If window in list has been deleted, just add zeros.
            if (imp == null) {
                for (int i=0; i<nQuantities+n*nRelQuantities; ++i) {
                    resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 + i, Double.NaN);
                }
                nullWindows = true;
                continue;
            }
            double Volume = 0;
            double Intensity = 0;
            double X = 0;
            double Y = 0;
            double Z = 0;

			Calibration cal = imp.getCalibration();
            double dZ = cal.pixelDepth;

            // Determine the first and last slice to loop over from the corresponding text fields.
            int currentSlice = imp.getCurrentSlice();
            int firstSlice = iFirstSlice[n];
            if (firstSlice > imp.getStackSize())
                firstSlice = imp.getStackSize();
            int lastSlice = iLastSlice[n];
            if (lastSlice > imp.getStackSize())
                lastSlice = imp.getStackSize();

            for (int slice=firstSlice; slice<=lastSlice; ++slice) {
                imp.setSlice(slice);
                ImageStatistics stats = imp.getStatistics(mOptions);
                if (stats == null) {
                    IJ.error("Measurement Error");
                    return;
                }

                // Read results of measurements and check for divide by zero.
                double mean = stats.mean;
                if (Double.isInfinite(mean) || Double.isNaN(mean)) {
                    mean = 0;
                }
                double area = stats.area;
                if (Double.isInfinite(area) || Double.isNaN(area)) {
                    area = 0;
                }
                double pixelCount = stats.pixelCount;
                if (Double.isInfinite(pixelCount) || Double.isNaN(pixelCount)) {
                    pixelCount = 0;
                }
                double xCenterOfMass = stats.xCenterOfMass;
                if (Double.isInfinite(xCenterOfMass) || Double.isNaN(xCenterOfMass)) {
                    xCenterOfMass = 0;
                }
                double yCenterOfMass = stats.yCenterOfMass;
                if (Double.isInfinite(yCenterOfMass) || Double.isNaN(yCenterOfMass)) {
                    yCenterOfMass = 0;
                }
                Volume = Volume + area;
                Intensity = Intensity + mean * pixelCount; // (mean*pixelCount: density calibrated total intensity of slice)
                X = X + xCenterOfMass * mean * pixelCount;
                Y = Y + yCenterOfMass * mean * pixelCount;
				if (cal.scaled()) {
					Z = Z + cal.getZ(slice-1) * mean * pixelCount;
				} else {
					Z = Z + (slice-1) * mean * pixelCount;
				}
            }

            imp.setSlice(currentSlice);

            X = X / Intensity;
            Y = Y / Intensity;
            Z = Z / Intensity;
			Volume = Volume * dZ;  // scale z-axis of volume

            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2, X);
            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +1, Y);
            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +2, Z);
            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +3, Volume);
            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +4, Intensity);
            resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +5, imp.getProcessor().getMinThreshold()); //output Threshold
            for (int i=0; i<nRelQuantities*n; ++i) {
                int counter = resultsTable.getCounter()-1;
                double Xi = resultsTable.getValue(nQuantities*i+nRelQuantities*(i-1)*i/2, counter);
                double Yi = resultsTable.getValue(nQuantities*i+nRelQuantities*(i-1)*i/2+1, counter);
                double Zi = resultsTable.getValue(nQuantities*i+nRelQuantities*(i-1)*i/2+2, counter);
                double D = Math.sqrt( (X-Xi)*(X-Xi) + (Y-Yi)*(Y-Yi) + (Z-Zi)*(Z-Zi) );
                resultsTable.addValue(nQuantities*n + nRelQuantities*(n-1)*n/2 +6+i, D);
            }

        } //for n (Windows)
        displayResults();
        if (nullWindows)
            IJ.error("Some Images in the list do not exist. Added \"zero\" results.");
    }

/* Writes the last row in the results table to the resultsWindow.
    If this is the first row in the table, also writes heading.*/
    private void displayResults() {
        if (resultsTable == null || resultsTable.getCounter() < 1)
            return;
        // If first row, write heading.
        int counter = resultsTable.getCounter();
        if (counter==1) {
            String heading = " ";
            for (int n=0; n<nWindows; ++n) {
                ImagePlus imp = getImageFromVector(n);
                String title;
                String unit;
                String valueUnit;

                if (imp != null) {
                    title = getImageTitleFromVector(n);
                    unit = imp.getCalibration().getUnits();
                    valueUnit = imp.getCalibration().getValueUnit();
                    if (unit != "")
                        unit = " / "+unit;
                    if (valueUnit != "" && valueUnit != "Gray Value")
                        valueUnit = " / "+valueUnit;
                    else
                        valueUnit = "";
                } else {
                    title = "null";
                    unit = "";
                    valueUnit = "";
                }
                heading = heading+"\t"+title;
                heading = heading+"\t"+"X"+unit;
                heading = heading+"\t"+"Y"+unit;
                heading = heading+"\t"+"Z"+unit;
                heading = heading+"\t"+"Volume";
                heading = heading+"\t"+"Intensity"+valueUnit;
                heading = heading+"\t"+"Threshold";
                for (int i=0; i<n; ++i) {
                    heading = heading+"\t"+"Dist "+(i+1)+unit;
                }
                // Add lines here for more Quantities.
            }
            resultsWindow = new M3DRWin("Sync Measurements", heading, heading, 500, 200, this);
        }

        // Write data from resultsTable to resultsWindow
        String dataRow = ""+resultsTable.getCounter();
        for (int n=0; n<nWindows; ++n) {
            dataRow = dataRow+"\t "; // Empty place below the image name.
            for (int i=0; i<nQuantities+nRelQuantities*n; ++i) {
                dataRow = dataRow+"\t"+ IJ.d2s(resultsTable.getValue(nQuantities*n + nRelQuantities*(n-1)*n/2 + i,
                                                                                    resultsTable.getCounter()-1), 3);
            }
        }
        resultsWindow.append(dataRow);
    }

/** Calculates the shift from the measured intensity gravity centers.
    The calculated values are the shift of a position in each image to the position in the first image
    and the standard deviation.
    The order is (position in image) Minus (position in first image). */
    public void calcFinal() {
        if (resultsTable == null || resultsTable.getCounter() < 1 || nWindows<=1)
            return;
        resultsWindow.append("===");

        String rowShift = "Shift";
        String rowStddev = "Stddev";
        int nResults = resultsTable.getCounter();
        
        double[][] shifts = new double[3][nWindows];

        for (int n=0; n<nWindows; ++n) { //loop over windows
            rowShift = rowShift + "\t ";
            rowStddev = rowStddev + "\t ";
            for (int xyz=0; xyz<3; xyz++) { //loop over coordinates to calculate mean and stddev of shift
                double shift = 0;
                double shiftSq = 0;
                for (int i=0; i<nResults; ++i) { // loop over rows
                    double tmpShift = resultsTable.getValue(nQuantities*n + nRelQuantities*(n-1)*n/2 + xyz,i) - resultsTable.getValue(xyz,i);
                    shift = shift + tmpShift;
                    shiftSq = shiftSq + tmpShift*tmpShift;
                }
                shift = shift / nResults;
                shifts[xyz][n] = shift;
                rowShift = rowShift + "\t"+IJ.d2s(shift, 3);
                if (nResults > 1) {
                    shiftSq = Math.sqrt( (shiftSq - nResults*shift*shift)/(double)(nResults-1) );
                    rowStddev = rowStddev + "\t"+IJ.d2s(shiftSq, 3);
                } else {
                    rowStddev = rowStddev + "\t -";
                }
                
            }
            for (int xyz=0; xyz<nQuantities-3; ++xyz) { //fill remaining columns
                rowShift = rowShift+"\t ";
                rowStddev = rowStddev+"\t ";
            }
            for (int nDist=0; nDist<n; ++nDist) { //loop over relative distances to calculate mean & stddev
                double shift = 0;
                double shiftSq = 0;
                for (int i=0; i<nResults; ++i) { // loop over rows
                    double tmpShift = resultsTable.getValue(nQuantities*(n+1) + nRelQuantities*(n-1)*n/2 + nDist,i);
                    shift = shift + tmpShift;
                    shiftSq = shiftSq + tmpShift*tmpShift;
                }
                shift = shift / nResults;
                rowShift = rowShift + "\t"+IJ.d2s(shift, 3);
                if (nResults > 1) {
                    shiftSq = Math.sqrt( (shiftSq - nResults*shift*shift)/(double)(nResults-1) );
                    rowStddev = rowStddev + "\t"+IJ.d2s(shiftSq, 3);
                } else {
                    rowStddev = rowStddev + "\t -";
                }
            } // nDist

        }
        resultsWindow.append(rowShift);
        resultsWindow.append(rowStddev);
        
        // Append shift data for each channel in columns for easy import to Boris Joffe's 
        // "_StackGroom" plugin.
        resultsWindow.append("");
        resultsWindow.append("Channel\tTitle\tShift x\tShift y\tShift z");
        for (int n=0; n<nWindows; n++) {
            String tempShiftRow = "Ch "+Integer.toString(n+1)+"\t";
            tempShiftRow += getImageTitleFromVector(n)+"\t";
            tempShiftRow += IJ.d2s(shifts[0][n], 3)+"\t";
            tempShiftRow += IJ.d2s(shifts[1][n], 3)+"\t";
            tempShiftRow += IJ.d2s(shifts[2][n], 3);
            resultsWindow.append(tempShiftRow);
        }
        
    }

// safe way of converting strings to integers without getting exceptions
    protected int stringToInt(String s) {
        int i;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            i = 0;
        }
        return i;
    }

/** Computes the combined maximum intensity projection of all images.
 *  This is for a rough orientation to avoid getting the next bead in the
 *  selected ROI. */
    ImagePlus computeProjection() {
 	int xSize = getImageFromVector(0).getWidth();
        int ySize = getImageFromVector(0).getHeight();

 	FloatProcessor fp = new FloatProcessor(xSize, ySize);
    	float[] fpixels = (float[]) fp.getPixels();
        for (int i=0; i<xSize*ySize; i++) // initialize to lowest possible number
            fpixels[i] = -Float.MAX_VALUE;

        for (int n=0; n<nWindows; ++n) {
            ImagePlus imp = getImageFromVector(n);
            int type = imp.getType();
            int size = imp.getStackSize();
            int xSizeN = imp.getWidth();
            int ySizeN = imp.getHeight();
            int minxSize = (xSize<xSizeN)?xSize:xSizeN; //minimum
            int minySize = (ySize<ySizeN)?ySize:ySizeN; //minimum

            ImageStack stk = imp.getStack();
            for (int slice=1; slice<=size; ++slice) {

				if (type == ImagePlus.GRAY8) {
		                    byte[] pixels = (byte[]) stk.getPixels(slice);
		                    for (int y=0; y<minySize; ++y) {
		                        for (int x=0; x<minxSize; ++x) {
		                            if((pixels[y*xSizeN+x]&0xff)>fpixels[y*xSize+x])
		                                fpixels[y*xSize+x] = (pixels[y*xSizeN+x]&0xff);
		                        }
		                    }
				}
		
				if (type == ImagePlus.GRAY16) {
		                    short[] pixels = (short[]) stk.getPixels(slice);
		                    for (int y=0; y<minySize; ++y) {
		                        for (int x=0; x<minxSize; ++x) {
		                            if((pixels[y*xSizeN+x]&0xffff)>fpixels[y*xSize+x])
		                                fpixels[y*xSize+x] = (pixels[y*xSizeN+x]&0xffff);
		                        }
		                    }
				}
		
				if (type == ImagePlus.GRAY32) {
		                    float[] pixels = (float[]) stk.getPixels(slice);
		                    for (int y=0; y<minySize; ++y) {
		                        for (int x=0; x<minxSize; ++x) {
						if(pixels[y*xSizeN+x]>fpixels[y*xSize+x])
				    		fpixels[y*xSize+x] = pixels[y*xSizeN+x];
		                        }
		                    }
				}
            } // for slice
        } // for n (Window)
        return new ImagePlus("Projection", fp.convertToShort(false));
    } // end of computeProjection()

/* Removes all links from here to the projection, so that it can be garbage collected. */
    void disconnectProjection() {
        if (! hasProjection)
            return;
        vwins.removeElementAt(vwins.size()-1);
        projection.getWindow().getCanvas().removeMouseListener(this);
        projection.getWindow().getCanvas().removeMouseMotionListener(this);
        projection.getWindow().removeWindowListener(this);

        hasProjection = false;
        projection = null;
    }
    
	protected void showAbout() {
		Dialog dialog = new Dialog(ijInstance);
		dialog.setSize(550, 350);
		dialog.setTitle("Sync Measure 3D "+VERSIONSTRING);

		dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					Dialog src = (Dialog) e.getSource();
					src.setVisible(false);
					src.dispose();
				}
			});			
		
		TextArea tArea = new TextArea();
		tArea.setEditable(false);
		tArea.setBackground(Color.white);		
		tArea.append("Sync Measure 3D\n \n" +
		"Synchronizes the cursor and z-slice in all selected windows, \n" +
		"and performs measurements on 3D ROIs in all selected windows.\n \n" +
		
		"Author: Joachim Walter\n \n"+
		
		"Usage:\n" +
		"- Load the data, each color channel as a separate 8- or 16-bit grayscale stack.\n \n"+
		
		"- Run Sync_Measure_3D. (Analyze>Tools>Sync Measure 3D)\n \n"+
		
		"- Synchronize the windows, in which you want to measure by selecting them in the\n"+
		"    window list.\n \n"+
		
		"- Click \"Start Measurements\".\n \n"+
		
		"- Click on \"Projection\" to obtain a combined maximum intensity projection of all\n"+
		"  selected images (stacks). This projection is not used for measurements, but is\n"+
		"  synchronized with the other images and might be a convenient reference.\n \n"+
		
		"- Choose a threshold for each image.\n \n"+
		
		"- Measure gravity center, Volume, Intensity and distance to preceding stacks:\n"+
		"    o Draw a ROI around the particle you want to measure. If the particles in different\n"+
		"        channels are at different positions, turn \"Synchronize Cursor\" off for that.\n"+
		"    o If necessary (when other particles are inside the ROI in different z-slices),\n"+
		"        Select first and last slice to include this particle and exclude the other ones.\n"+
		"    o Double-click with the left mousebutton into the ROI to perform a measurement.\n" +
		"        + The X, Y and Z values are the coordinates of the intensity gravity centers\n"+
		"            of the measured objects.\n"+
		"        + The Volume and Intensity values are the volumes and the total intensities\n"+
		"            of the measured objects.\n"+
		"        + The Dist 1, Dist 2, ... values (for 2 or more image stacks) are the 3D distances\n"+
		"            of the intensity gravity center of the object in the current stack to the intensity\n"+
		"            gravity center of the object in the first, second, ... stack in the list.\n"+
		"        + If the scale of the image is not set, the results are displayed in \"pixels\".\n"+
		"            3D-distances are meaningless in this case, if the distance between slices\n"+
		"            differs from the pixelsize. If the image scale is set, the results are displayed\n"+
		"            in the selected unit.\n"+
		"        + Intensity values are given in gray values or the selected units in case that the\n"+
		"            image has been density calibrated. If the image is density calibrated, the centers\n"+
		"            of mass are calculated with the calibrated values, but the threshold is still a raw\n"+
		"            value (0-255 for 8-bit images).\n"+
		"    o To calculate mean values and standard deviations, click on \"Stop Measurements\".\n"+
		"        + For the x- y- and z- coordinates the differences to the coordinates of image stack #1\n"+
		"             are calculated ( (value for this stack) minus (value for stack #1) ) and their mean\n"+
		"             and standard deviation values are displayed. This helps in calculating the chromatic\n"+
		"             shift to stack #1.\n"+
		"        + For the Dist 1, Dist 2, ... values, simply the mean and the standard deviation are\n"+
		"             calculated.");
				
		dialog.add(tArea);
		dialog.setVisible(true);
	}



/* TextWindow, which shows a save changes dialog, when closing is attempted, and has the "clear" and "cut"
    menu entries disabled, which I did not assign a sensible behaviour to.
    When results are saved, the shifts are calculated and the measurements are stopped.
    The name means Measure3DResultsWindow, but has been shortened, because otherwise it would get longer
    than 32 letters, which would make problems on MacOS <= 9.*/
    private class M3DRWin extends TextWindow {

        private Sync_Measure_3D parent; // Sync_Measure object that produced this object, is set to null, when Stop Measurin is pressed.

        M3DRWin(String title, String heading, String data, int sizex, int sizey, Sync_Measure_3D parent) {
            super(title, heading, data, sizex, sizey);
            this.parent = parent;
            this.getMenuBar().getMenu(1).remove(3); // remove "clear" from menu
            this.getMenuBar().getMenu(1).remove(0); // remove "cut" from menu
        }

        public void processWindowEvent(WindowEvent e) {
            if(e.getID() == WindowEvent.WINDOW_CLOSING) {
                SaveChangesDialog d = new SaveChangesDialog(this, "Save Measurements?");
                if (d.cancelPressed()) return;
                if (d.savePressed()) {
                    if (parent != null ) {
                        parent.calcFinal(); // appends shift measurements to bottom of results table
                    }
                    this.getTextPanel().saveAs("");
                }
                if (parent !=  null) {
                    parent.stopMeasurements();
                }

            }
            super.processWindowEvent(e);
        }

        public void nullParent() {
            parent = null;
        }

    }



    /* This is a table for storing measurement results as columns of real numbers.
       I wanted to extend the ResultsTable class to allow more columns, but
       unfortunately, ResultsTable.MAX_COLUMNS is final. */
    private class M3DRTab {

	private int counter;
	private float[][] columns;
	private int maxRows = 100; // will be increased as needed
        private int nColumns;
	private int lastColumn = -1;

	/* Constructs an empty M3DRTab with the counter=0 and the specified number of columns. */
	public M3DRTab(int nColumns) {
            columns = new float[nColumns][];
            this.nColumns = nColumns;
	}

	/* Increments the measurement counter by one. */
	public synchronized void incrementCounter() {
            counter++;
            if (counter==maxRows) {
                for (int i=0; i<nColumns; i++) {
                    if (columns[i]!=null) {
                        float[] tmp = new float[maxRows*2];
                        System.arraycopy(columns[i], 0, tmp, 0, maxRows);
                        columns[i] = tmp;
                    }
                }
                maxRows *= 2;
            }
	}

	/* Returns the current value of the measurement counter. */
	public int getCounter() {
		return counter;
	}

	/** Adds a value to the end of the given column. Counter must be >0.*/
	public void addValue(int column, double value) {
            if ((column<0) || (column>=nColumns))
                throw new IllegalArgumentException("Index out of range: "+column);
            if (counter==0)
                throw new IllegalArgumentException("Counter==0");
            if (columns[column]==null) {
                columns[column] = new float[maxRows];
                if (column>lastColumn) lastColumn = column;
            }
            columns[column][counter-1] = (float)value;
	}

	/* Returns a copy of the given column as a float array.
		Returns null if the column is empty. */
	public float[] getColumn(int column) {
		if ((column<0) || (column>=nColumns))
			throw new IllegalArgumentException("Index out of range: "+column);
		if (columns[column]==null)
			return null;
		else {
			float[] data = new float[counter];
			for (int i=0; i<counter; i++)
				data[i] = columns[column][i];
			return data;
		}
	}

	/*	Returns the value of the given column and row, where
		column must be greater than or equal zero and less than
		nColumns and row must be greater than or equal zero
		and less than counter. */
	public float getValue(int column, int row) {
		if (columns[column]==null)
			throw new IllegalArgumentException("Column not defined: "+column);
		if (column>=nColumns || row>=counter)
			throw new IllegalArgumentException("Index out of range: "+column+","+row);
		return columns[column][row];
	}

	/* Sets the value of the given column and row, where
		where 0<=column<nColumns and 0<=row<counter. */
	public void setValue(int column, int row, double value) {
		if ((column<0) || (column>=nColumns))
			throw new IllegalArgumentException("Column out of range: "+column);
		if (row>=counter)
			throw new IllegalArgumentException("row>=counter");
		if (columns[column]==null) {
			columns[column] = new float[maxRows];
			if (column>lastColumn) lastColumn = column;
		}
		columns[column][row] = (float)value;
	}


	/* Clears all the columns and sets the counter to zero. */
	public synchronized void reset() {
		counter = 0;
		maxRows = 100;
		for (int i=0; i<=lastColumn; i++) {
			columns[i] = null;
		}
		lastColumn = -1;
	}

    } // end of class M3DRTab


}
