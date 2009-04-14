package VolumeJ;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.lang.reflect.*;
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.Frame;
import java.applet.Applet;
import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import ij.io.*;
import volume.*;
import ij.plugin.frame.*;
/**
 * This class offers SurfaceJ, a surface plotting plugin based on VJ.<br>
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class VJSurfaceJ extends VJUserInterface
implements ActionListener, ClipboardOwner, ItemListener, KeyListener
{
	protected static final String  	        VERSION = "1.2";
	protected TextField     		sigmaField;
	protected Button        		renderSurfaceplot;
        protected static float                  sigma = 0;

	/**
         * Open a new dialog.
         */
        public VJSurfaceJ()
	{
		super();
		if (instance!=null)
                {
			instance.toFront();
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
                showDialog();
                setVisible(true);
                xrot = -50;
                scale = 2;
        }
        /**
         * Sets up the dialog window with all (dynamic) elements filled in.
         */
	public void showDialog()
	{
		initializing = true;
                setTitle("SurfaceJ "+VERSION);
		setLayout(new FlowLayout());

		Panel buttonsPlusData = new Panel();
		buttonsPlusData.setLayout(new GridLayout(0,1));
		add(buttonsPlusData);

		Panel allParams = new Panel();
		allParams.setLayout(new FlowLayout());
		add(allParams);

		Panel buttons = new Panel();
		Label l = new Label("SurfaceJ: \u00a92000-2002 Michael Abramoff", Label.LEFT);
		buttons.add(l);
		Label l1 = new Label("3-D Surface Plotting", Label.LEFT);
		buttons.add(l1);
		buttons.setLayout(new GridLayout(0,1));
		renderSurfaceplot = new Button("Render surface plot");
		renderSurfaceplot.addActionListener(this);
		buttons.add(renderSurfaceplot);
		renderStop = new Button("Stop rendering");
		renderStop.addActionListener(this);
		buttons.add(renderStop);
		renderStop.setEnabled(false);

		// Create a choice for the volume stack.
		iList = getImageWindows();
		String[] stitles = getWindowNames(iList);
		if (iList.length > 0)
		{
                        sourceChoice = createChoice(buttons, "Source image(s):", stitles, 0);
                        sourceChoice.addItemListener(this);
		}
		buttonsPlusData.add(buttons);

		Panel params1 = new Panel();
		params1.setLayout(new GridLayout(0, 2));

		// Create the other items in the VJ window.
		String [] sxyz = { ""+xrot, ""+yrot, ""+zrot };
		rotField = createXYZField(params1, "Rotate:(º)", sxyz, 1);
		scaleField = createTextField(params1, "Scale:", ""+scale, 1);
		if (iList.length > 0)
		{
                        ImagePlus imp = WindowManager.getImage(iList[sourceChoice.getSelectedIndex()]);
                        Calibration c = imp.getCalibration();
                        aspectz = (float) (c.pixelDepth / c.pixelWidth);
		}
		String [] saspect = { ""+aspectx, ""+aspecty, ""+aspectz };
		aspectField = createXYZField(params1, "Aspect/:", saspect, 1);

		String [] sluts = { "spectrum LUT", "load custom" };
                lutChoice = createChoice(params1, "Index LUT type", sluts, 0);
		String [] sinterpolation = { "nearest neighbor", "trilinear" };
		interpolationChoice = createChoice(params1, "Interpolation:", sinterpolation, 1);
		cineNField = createTextField(params1, "Cine total rotation(º):", "360", 1);
		cineField = createTextField(params1, "Cine frame increment(º):", "10", 1);
		String [] sAxis = { "x", "y", "z" };
		axisChoice = createChoice(params1, "Cine rotation axis", sAxis, 1);
		sigmaField = createTextField(params1, "Gaussian smoothing:", ""+sigma, 1);
		allParams.add(params1);
		pack();
                GUI.center(this);
		setVisible(true);
		initializing = false;
                activateButtons(iList.length);
	}
	/**
         * React to Event e.
         * @param e an Event.
         */
        protected void perform(AWTEvent e)
        {
                if (initializing)
                        return;
                // keep values for convenience (no retyping).
                aspectx = getFloatField(aspectField[0]);
                aspecty = getFloatField(aspectField[1]);
                aspectz = getFloatField(aspectField[2]);
                scale = getFloatField(scaleField);
                xrot = getFloatField(rotField[0]);
                yrot = getFloatField(rotField[1]);
                zrot = getFloatField(rotField[2]);
                sigma = getFloatField(sigmaField);
                boolean cineToDisk = backCheckbox instanceof Checkbox && backCheckbox.getState();
                /** The window containing the stack (which will determine the volume to be rendered. */
                ImagePlus imp = null;
                if (iList.length > 0)
                        // There are images.
                        imp = WindowManager.getImage(iList[sourceChoice.getSelectedIndex()]);
                if (e.getSource()==renderSurfaceplot)
                {
                        /**
                         * Rendering action pushed.
                         * Choose the right view sequence, and set the proper renderer, volume etc.
                         * First check the renderer.
                         */
                        VJInterpolator interpolator = null;
                        if (interpolationChoice.getSelectedIndex() == 1)
                                interpolator = new VJTrilinear();
                        else
                                interpolator = new VJNearestNeighbor();
                        // Check the shader.
                        if (shader == null)
	                        shader = resetShader(lightx, lighty, lightz, aspectx, aspecty, aspecty);
	                // Check the classifier (and initialize the indexes if needed).
	                if (classifier == null)
                                classifier = resetClassifier(VJClassifiers.LEVOY,
                                        lutChoice.getSelectedIndex());
                        renderer = resetRenderer(interpolator, classifier, shader,
                                 RAYTRACE, false, 0, 0, 0, false, 0, 0);
                        if (renderer == null)
                                return;
                        // First create a volume with all images from the stack from the current window.
                        Volume vimages = null;
                        if (! (imp.getProcessor() instanceof ByteProcessor))
                                error("Currently only 8-bit stacks allowed.");
                                else
                        {
                                try { vimages = new VolumeShort(imp.getStack()); } catch (Exception exc) { return; }
                                rs = new VJSurfacePlotShell(renderer, scale, xrot, yrot, zrot,
                                        vimages, 0, 255, aspectz, sigma, "Rendering["+number+"]");
                        }
                        renderStop.setEnabled(true);
                        /** Start rendering. */
                        rs.start();
                        VJUserInterface.write(rs.toString());
                        number++;
                }
                else if (e.getSource()==renderStop)
                {
                        rs.kill();
                        VJUserInterface.write("Rendering interrupted.");
                }
                else if (e.getSource() == lutChoice)
                        classifier = null;
                else if (e.getSource() == sourceChoice)
                        v = null;
                else if (e.getSource() == rotField[0] || e.getSource() == rotField[1] || e.getSource() == rotField[2])
                        shader = null;
                else if (e.getSource() == aspectField[0] ||
                        e.getSource() == aspectField[1] ||
                        e.getSource() == aspectField[2])
                {
                        v = null;
                        shader = null;
                }
        }
	/**
         * Create and setup a classifier according to the classifier number index (index only valid within VJUserInterface).
         * @param index the local index number of the classifier in VJClassifiers. NOT USED
         * @param lutIndex a flag indicating what LUT to use. 0 is built in LUT, 1 = user loadable LUT.
         * @return a VJClassifier.
	*/
	protected VJClassifier resetClassifier(int index, int lutIndex)
	{
                VJClassifier classifier = new VJClassifierLevoy();
                ((VJClassifierLevoy) classifier).setThreshold(128);
                ((VJClassifierLevoy) classifier).setWidth(2);
                // Ask for LUT if requested by user
                if (classifier.hasLUT() && lutIndex==1)
                        getLUT(classifier, "");
                return classifier;
	}
	/**
         * Get the ids of all open windows that are suitable for this plugin.
         * The id's can be used by the .getImage(id) method.
         * @return an int[] containg the ids of the stacks.
	*/
	protected int [] getImageWindows()
	{
                int [] idList = WindowManager.getIDList(); // find all windows.
                int [] iLongList;
                if (idList != null)
                        iLongList = new int[idList.length];
                else
                        // No windows open. Isnt volumej window a window - not yet.
                        iLongList = new int[0];
                int inx = 0;
                // Get them and count total of stacks.
                for (int i = 0; i < iLongList.length; i++)
                {
                        ImagePlus imp = WindowManager.getImage(idList[i]);
                        // Is it a suitable window?
                        if (imp instanceof ImagePlus)
                        {
                                iLongList[i] = idList[i];
                                inx++;
                        }
                        else
                        iLongList[i] = 0;
                }
                // Now shrink the list
                int [] iList = new int[inx];
                inx = 0;
                for (int i = 0; i < iLongList.length; i++)
                {
                        if (iLongList[i] != 0)
                                iList[inx++] = iLongList[i];
                }
                return iList;
	}
        /**
         * Activate the rendering and surface plot buttons, depending on whether or
         * not there is a stack, an image, or nothing available.
         * @param iListLength the number of stacks available when VolumeJ started.
         */
        protected void activateButtons(int listLength)
        {
                ImagePlus imp = WindowManager.getCurrentImage();
                renderSurfaceplot.setEnabled(imp instanceof ImagePlus);
        }
        /**
         * Set the LUT of a classifier from an ImageJ or NIH LUT file.
         * @param classifier, a VJClassifier for which you want to read the LUT.
         * @param path a filename to be read. Get from defaultpath, if defaultpath == "",
         * ask user for filename.
         * @return true if LUT of classifier was sucessfully set, false otherwise.
         */
        public boolean getLUT(VJClassifier classifier, String path)
        {
		if (path.equals(""))
                {
                        OpenDialog od = new OpenDialog("Open LUT...", path);
		        if (od.getFileName()==null)
                                return false;
                        path = od.getDirectory() + od.getFileName();
                }
		IJ.showStatus("Opening: " + path);
                byte [] reds = new byte[256]; byte [] greens = new byte[256]; byte [] blues = new byte[256];
		try
                {
			int size = readLUT(reds, greens, blues, path, false); // attempt to read NIH Image LUT
			if (size==0) readLUT(reds, greens, blues, path, true);  // otherwise read 768 byte raw LUT
		}
                catch (IOException e)
                {
			VJUserInterface.error(e.getMessage());
			return false;
		}
                if (classifier.setLUT(reds, greens, blues))
                {
                        VJUserInterface.write("Classifier LUT set to: "+path);
                        return true;
                }
                else
                {
                        IJ.error("Cannot change LUT");
                        return false;
                }
	}
        /**
         * Read an ImageJ or NIH LUT from file.
         * @param reds, greens, blues must be byte[256] and will be filled with the LUT values.
         * @param path the filename/filepath to be read.
         * @param raw flag for reading NIH (false) or ImageJ (true) format LUT files.
         * @return the number of entries in the LUT.
         */
        private int readLUT(byte [] reds, byte [] greens, byte [] blues, String path, boolean raw)
        throws IOException
        {
		InputStream is;
		is = new FileInputStream(path);
		DataInputStream f = new DataInputStream(is);
		int nColors = 256;
		if (!raw) {
			// attempt to read 32 byte NIH Image LUT header
			int id = f.readInt();
			if (id!=1229147980) // Macintosh 'ICOL' magic number
				return 0;
			int version = f.readShort();
			nColors = f.readShort();
			int start = f.readShort();
			int end = f.readShort();
			long fill1 = f.readLong();
			long fill2 = f.readLong();
			int filler = f.readInt();
		}
		f.read(reds, 0, nColors);
		f.read(greens, 0, nColors);
		f.read(blues, 0, nColors);
		if (nColors < 256 || nColors > 256)
			throw new IOException("LUT not right number of colors: "+nColors);
		f.close();
		return 256;
        }
}


