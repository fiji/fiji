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
 * This class links the VJ classes to the ImageJ interface.<br>
 *
 * Copyright (c) 1999-2005, Michael Abramoff. All rights reserved.
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
public class VJUserInterface
extends PlugInFrame
implements ActionListener, ClipboardOwner, ItemListener, KeyListener
{
	protected Button        		render, renderCine, renderStereo, renderStop, renderExtra;
	protected Choice        		gradientChoice, classificationChoice, axisChoice;
	protected Choice        		sourceChoice, indexChoice, mappingChoice, interpolationChoice;
	protected Choice                        lutChoice;
	protected Checkbox      		testCheckbox, cutoutCheckbox, traceCheckbox, backCheckbox, diskCheckbox;
	protected TextField []  		rotField;
	protected TextField []  		aspectField;
	protected TextField []  		lightField;
	protected TextField []  		cutoutField;
	protected TextField []  		traceField;
	protected TextField			cineField, cineNField, widthField, thresholdField, scaleField;
	protected TextArea                      classifierTextArea;

	protected static String  		defaultDirectory = null;
	// List with all open stacks.
	protected int []        		iList;
	protected static final String  	        VERSION = "1.8";
	protected static final int              RAYTRACE = 0;
	protected static final int              ISOSURFACE = 1;
	protected static final int              VIEWSPACE = 2;

	protected VJRenderView		   	rs;
	protected VJRenderer 		   	renderer;
	// The volume to be rendered.
	protected Volume        		v;
	protected VJShader                      shader;
	protected VJClassifier                  classifier;
	// sequential rendering number.
	protected static int		   	number = 0;
	// axis chosen.
	protected static int  	   	        axis = 0;
	// interpolation method chosen.
	protected static int  	   	        interpolation ;

	// Rendering parameters kept static for convenience.
	protected static float 	   	        aspectx = 1;
	protected static float 		        aspecty = 1;
	protected static float 		        aspectz = 1;
	protected static float			scale = 1;
	protected static float 	                xrot = 0;
	protected static float 		        yrot = 0;
	protected static float 		        zrot = 0;
	protected static float 		        lightx = 0;
	protected static float                 lighty = 0;
	protected static float                 lightz = -1000;
	// VLClassifierLevoy.
	protected static float			threshold = 128;
	protected static float 		        deviation = 2;
	/** This window. */
	static Frame                            instance = null;
	protected boolean                       initializing = false;

	/**
	 * Open a new VJUserInterface.
	 */
	public VJUserInterface()
	{
		super("VolumeJ");
		if (instance != null)
		{
			instance.toFront();
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		showDialog();
		setVisible(true);
	}
	/**
	 * VJUserInterface has been closed. Set the instance back to null.
	 * @param e a WindowEvent containing the type of event that caused closing.
	 */
	public void windowClosing(WindowEvent e)
	{
		super.windowClosing(e);
		instance = null;
		if (rs instanceof VJRenderView)
			rs.kill();
		rs = null; v = null; renderer = null; System.gc();
		synchronized(this)
		{
			notify();
		}
	}
	/**
	 * Sets up the dialog window with all (dynamic) elements filled in.
	 */
	public void showDialog()
	{
		initializing = true;
		setTitle("VolumeJ "+VERSION);

		// Determine layout of window.
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constr = new GridBagConstraints();
		setLayout(layout);

		// Fit in buttons panel, at the topleft.
		Panel buttons = new Panel();
		constr.weightx = 1.0;
		constr.anchor = GridBagConstraints.NORTH;
		constr.insets = new Insets(10,10,10,10);
		layout.setConstraints(buttons, constr);
		add(buttons);
		buttons.setLayout(new GridLayout(0,1));

		// Fit in the params panel top right.
		Panel allParams = new Panel();
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.anchor = GridBagConstraints.NORTHEAST;
		layout.setConstraints(allParams, constr);
		add(allParams);
		allParams.setLayout(new FlowLayout());

		// Fit in the classifierOptions panel, below.
		Panel classifierOptions = new Panel();
		constr.weightx = 0.0;
		constr.anchor = GridBagConstraints.WEST;
		constr.fill = GridBagConstraints.NONE;
		layout.setConstraints(classifierOptions, constr);
		add(classifierOptions);

		// Make buttons panel.
		Label l = new Label("VolumeJ: \u00a92000-2005 michael-abramoff@uiowa.edu", Label.LEFT);
		buttons.add(l);
		Label l1 = new Label("Volume visualization in Java. Now supports macros", Label.LEFT);
		buttons.add(l1);
		buttons.setLayout(new GridLayout(0,1));
		render = new Button("Render");
		render.addActionListener(this);
		buttons.add(render);
		renderCine = new Button("Render Cine-mode");
		renderCine.addActionListener(this);
		buttons.add(renderCine);
		renderStereo = new Button("Render stereo pair (\u0394="+IJ.d2s(VJRenderViewStereo.stereoDifference,1)+"º)");
		renderStereo.addActionListener(this);
		buttons.add(renderStereo);
		renderStop = new Button("Stop renderer");
		renderStop.addActionListener(this);
		buttons.add(renderStop);
		renderStop.setEnabled(false);
		renderExtra = new Button("Interactive rendering");
		renderExtra.addActionListener(this);
		buttons.add(renderExtra);
		diskCheckbox = new Checkbox("Cine rendering to disk, do not display.");
		diskCheckbox.setState(false);
		diskCheckbox.addItemListener(this);
		buttons.add(diskCheckbox);
		backCheckbox = new Checkbox("Render backfaces (MRI's)");
		backCheckbox.setState(false);
		backCheckbox.addItemListener(this);
		buttons.add(backCheckbox);
		// Create a choice for the volume stack.
		iList = getImageWindows();
		String[] stitles = getWindowNames(iList);
		if (iList.length > 0)
		{
			sourceChoice = createChoice(buttons, "Volume stack", stitles, 0);
			sourceChoice.addItemListener(this);
		}
		// Create a choice for the index volume stack.
		if (iList.length > 1)
		{
			indexChoice = createChoice(buttons, "Index stack, if any", stitles, 1);
			indexChoice.addItemListener(this);
		}
		String [] smapping = { VJRender.desc()+" rendering algorithm", VJIsosurfaceRender.desc()+" rendering algorithm", VJViewspaceRender.desc()+" rendering algorithm" };
		mappingChoice = createChoice(buttons, "", smapping, 0);

		// Make the classifierOptions panel.
		layout = new GridBagLayout();
		classifierOptions.setLayout(layout);
		constr = new GridBagConstraints();
		constr.weightx = 1.0;
		constr.anchor = GridBagConstraints.WEST;
		String [] sclassifiers = VJClassifiers.getNames();
		// Choice list of all classifiers
		classificationChoice = createChoice(classifierOptions, constr, layout,
			"Classifier:", sclassifiers, VJClassifiers.LEVOYNOINDEX);
		if (indexChoice instanceof Choice)
			indexChoice.setEnabled(true);
		Label lT = new Label("Description", Label.RIGHT);
		constr.gridwidth = GridBagConstraints.RELATIVE;
		layout.setConstraints(lT, constr);
		classifierOptions.add(lT);
		classifierTextArea = new TextArea("", 3, 60, TextArea.SCROLLBARS_NONE);
		classifierTextArea.setEditable(false);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(classifierTextArea, constr);
		classifierOptions.add(classifierTextArea);
		// User loadable LUT?
		String [] sluts = { "spectrum LUT", "load custom" };
		lutChoice = createChoice(classifierOptions, constr, layout, "Index LUT type", sluts, 0);
		thresholdField = createTextField(classifierOptions, constr, layout, "Classifier threshold:", ""+threshold, 4);
		widthField = createTextField(classifierOptions, constr, layout, "Classifier deviation:", ""+deviation, 4);
		propagateClassifier();

		// Make the other params panel.
		Panel params1 = new Panel();
		params1.setLayout(new GridLayout(0, 2));

		String [] sxyz = { ""+xrot, ""+yrot, ""+zrot };
		rotField = createXYZField(params1, "Rotation(º):", sxyz, 1);
		scaleField = createTextField(params1, "Scaling:", ""+scale, 1);
		if (iList.length > 0)
		{
			ImagePlus imp = WindowManager.getImage(iList[sourceChoice.getSelectedIndex()]);
			Calibration c = imp.getCalibration();
			aspectz = (float) (c.pixelDepth / c.pixelWidth);
		}
		String [] saspect = { ""+aspectx, ""+aspecty, ""+aspectz };
		aspectField = createXYZField(params1, "Aspect ratios:", saspect, 1);

		String [] sinterpolation = { "nearest neighbor", "trilinear" };
		interpolationChoice = createChoice(params1, "Interpolation", sinterpolation, 1);
		cineNField = createTextField(params1, "Cine total rotation(º):", "360", 1);
		cineField = createTextField(params1, "Cine frame increment(º):", "10", 1);
		String [] sAxis = { "x", "y", "z" };
		axisChoice = createChoice(params1, "Cine rotation axis", sAxis, 1);
		String [] slight = { ""+lightx, ""+lighty, ""+lightz };
		lightField = createXYZField(params1, "Light:", slight, 1);
		cutoutCheckbox = new Checkbox("Add cutout centered at:");
		cutoutCheckbox.setState(false);
		params1.add(cutoutCheckbox);
		String [] scutout = { ""+10, ""+10, ""+10 };
		cutoutField = createXYZField(params1, scutout, 0);
		if (true || IJ.debugMode)
		{
			traceCheckbox = new Checkbox("Print trace for pixel at (x,y):");
			traceCheckbox.setState(false);
			params1.add(traceCheckbox);
			String [] strace = { ""+50, ""+50 };
			traceField = createXYZField(params1, strace, 0);
		}
		// Rendering algorithm.
		allParams.add(params1);

		pack();
		GUI.center(this);
		setVisible(true);
		initializing = false;
		activateButtons(iList.length);
	}
	/**
	 * React to action e.
	 * @param e an ActionEvent.
	 */
	public void actionPerformed(ActionEvent e)
	{
		perform((AWTEvent) e);
	}
	/**
	 * React to action e.
	 * @param e an ItemEvent.
	 */
	public void itemStateChanged(ItemEvent e)
	{
		perform((AWTEvent) e);
	}
	/**
	 * React to action e.
	 * @param e a KeyEvent.
	 */
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e)
	{
		perform((AWTEvent) e);
	}
	/**
	 * Activate the rendering buttons, depending on whether or
	 * not there is a stack, an image, or nothing available.
	 * @param listLength the number of stacks available.
	 */
	protected void activateButtons(int listLength)
	{
		render.setEnabled(listLength > 0);
		renderStereo.setEnabled(listLength > 0);
		renderCine.setEnabled(listLength > 0);
	}
	public void keyReleased(KeyEvent e) {}
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
		threshold = getFloatField(thresholdField);
		deviation = getFloatField(widthField);
		scale = getFloatField(scaleField);
		xrot = getFloatField(rotField[0]);
		yrot = getFloatField(rotField[1]);
		zrot = getFloatField(rotField[2]);
		lightx = getFloatField(lightField[0]);
		lighty = getFloatField(lightField[1]);
		lightz = getFloatField(lightField[2]);
		axis = axisChoice.getSelectedIndex();
		interpolation = interpolationChoice.getSelectedIndex();
		float cineN = getFloatField(cineNField);
		float cine = getFloatField(cineField);
		int classification = classificationChoice.getSelectedIndex();

		boolean cineToDisk = backCheckbox instanceof Checkbox && backCheckbox.getState();
		/** The window containing the stack (which will determine the volume to be rendered. */
		ImagePlus imp = null;
		if (iList.length > 0)
			// There are stacks.
			imp = WindowManager.getImage(iList[sourceChoice.getSelectedIndex()]);
		else
			// Only an image for surface plotting.
			imp = WindowManager.getCurrentImage();
		/** Maybe an index stack window. */
		ImagePlus impindex = null;
		if (imp != null && iList.length > 1)
			impindex = WindowManager.getImage(iList[indexChoice.getSelectedIndex()]);
		if (e.getSource()==render || e.getSource()==renderCine || e.getSource()==renderStereo
			|| e.getSource() == renderExtra)
		{
			ImagePlus impViewer = null;
			if (e.getSource() == renderExtra)
			{
				// Open an interactive rendering window.
				ColorProcessor cp = new ColorProcessor(500, 500, new int[500*500]);
				cp.setColor(new Color(0xffffff)); cp.fill();
				impViewer = new ImagePlus("VJViewerCanvas", cp);
				new ImageWindow(impViewer, new VJViewerCanvas(impViewer));
				mappingChoice.select(ISOSURFACE);
			}
			/**
			 * Rendering action pushed.
			 * Choose the right view sequence, and set the proper renderer, volume etc.
			 * First check the renderer.
			 */
			VJInterpolator interpolator = null;
			if (interpolation == 1)
				interpolator = new VJTrilinear();
			else
				interpolator = new VJNearestNeighbor();
			// Check the shader.
			if (shader == null)
				shader = resetShader(lightx, lighty, lightz, aspectx, aspecty, aspectz);
			// Check the classifier (and initialize the indexes if needed).
			if (classifier == null)
			{
				if (mappingChoice.getSelectedIndex() == ISOSURFACE)
					classifier = resetClassifier(VJClassifiers.ISOSURFACE,
						lutChoice.getSelectedIndex());
				else
					classifier = resetClassifier(classification,
						lutChoice.getSelectedIndex());
			}
			renderer = resetRenderer(interpolator, classifier, shader,
				 mappingChoice.getSelectedIndex(), cutoutCheckbox.getState(),
				 (int) getFloatField(cutoutField[0]),
				 (int) getFloatField(cutoutField[1]),
				 (int) getFloatField(cutoutField[2]),
				 (traceCheckbox instanceof Checkbox && traceCheckbox.getState()),
				 (int) getFloatField(traceField[0]), (int) getFloatField(traceField[1]));
			if (renderer == null)
				return;
			if (v == null)
				v = resetVolume(renderer, imp, impindex, aspectx, aspecty, aspectz);
			renderer.setVolume(v);
			if (e.getSource()==render)
				rs = new VJRenderViewSingle(renderer, scale, xrot, yrot, zrot, "Rendering["+number+"]");
			else if (e.getSource()==renderStereo)
				rs = new VJRenderViewStereo(renderer, scale, xrot, yrot, zrot, "Rendering["+number+"]");
			else if (e.getSource()==renderExtra)
			{
				write("renderview init");
				rs = new VJRenderViewInteractive(impViewer, renderer, scale, "Interactive rendering");
			}
			else if (e.getSource()==renderCine)
			{
				rs = new VJRenderViewCine(renderer, scale, xrot, yrot, zrot, "Rendering["+number+"]",
					(int) (cineN/cine), cineToDisk);
				switch (axis)
				{
					case 0: // x axis.
					((VJRenderViewCine) rs).setRotationSteps(cine, 0, 0);
					break;
					case 1: // y axis.
					((VJRenderViewCine) rs).setRotationSteps(0, cine, 0);
					break;
					case 2: // z axis.
					((VJRenderViewCine) rs).setRotationSteps(0, 0, cine);
					break;
				}
			}
			renderStop.setEnabled(true);
			/** Start rendering. */
			if (IJ.debugMode) IJ.showStatus("render up and starting");
			rs.start();
			write(rs.toString());
			number++;
		}
		else if (e.getSource()==renderStop)
		{
			rs.kill();
			write("Rendering interrupted.");
			renderStop.setEnabled(false);
		}
		else if (e.getSource() == sourceChoice || e.getSource() == indexChoice)
			v = null;
		else if (e.getSource() == classificationChoice ||
			e.getSource() == thresholdField ||
			e.getSource() == mappingChoice ||
			e.getSource() == lutChoice || e.getSource() == widthField)
		{
			propagateClassifier();
		}
		else if (e.getSource() == backCheckbox ||
			e.getSource() == lightField[0] || e.getSource() == lightField[1] || e.getSource() == lightField[2] ||
			e.getSource() == rotField[0] || e.getSource() == rotField[1] || e.getSource() == rotField[2])
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
	 * Create a renderer for the shader, interpolator and renderer.
	 * Do not set the volume.
	 * @param interpolator a VJInterpolator
	 * @param classifier a VJClassifier
	 * @param algorithm the type of renderer: RAYTRACE,  ISOSURFACE or VIEWSPACE.
	 * @return a VJRenderer.
	*/
	protected VJRenderer resetRenderer(VJInterpolator interpolator, VJClassifier classifier, VJShader shader,
		  int algorithm, boolean doCutouts, int cutoutX, int cutoutY, int cutoutZ,
		boolean doTrace, int traceX, int traceY)
	{
		try
		{
			if (algorithm == RAYTRACE)
				renderer = new VJRender(interpolator, shader, classifier);
			else if (algorithm == ISOSURFACE)
				renderer = new VJIsosurfaceRender(interpolator, shader, classifier);
			   else
				renderer = new VJViewspaceRender(interpolator, shader, classifier);
			/** Set cutouts. */
			if (doCutouts)
			{
				VJCutout cutout = new VJCutout(v,
					cutoutX, cutoutY, cutoutZ,
					renderer.getInterpolator(),
					renderer.getShader(), renderer.getClassifier());
				renderer.setCutout(cutout);
			}
			/** Set tracing. */
			if (doTrace)
				renderer.trace(traceX, traceY);
		}
		catch (Exception e) { write("Cannot create renderer!" + e); renderer = null; }
		return renderer;
	}
	/**
	 * Change the volume to be rendered, based on the stack in window imp.
	 * Make a new volume from the imp window, which should contain a stack.
	 * @param renderer a VJRenderer that will render the volume.
	 * @param imp an Imageplus, which should contain a stack.
	 * @param impindex an ImagePlus, which can contain an index stack. If null no index is used.
	 * @param aspectx the aspect ratio x-dimension for the volume.
	 * @param aspecty the aspect ratio y-dimension for the volume.
	 * @param aspectz the aspect ratio z-dimension for the volume.
	 */
	public static Volume resetVolume(VJRenderer renderer, ImagePlus imp, ImagePlus impindex,
					 double aspectx, double aspecty, double aspectz)
	{
		if (IJ.debugMode) write("resetting volume");
		Volume v = null;
		// Make a volume from the image stack imp.
		if (imp == null || imp.getStackSize() <= 1)
		{
			IJ.error("Improper or no volume stack selected");
			return null;
		}
		if (imp.getStack().getImageArray()[0] instanceof byte []
			|| imp.getStack().getImageArray()[0] instanceof short [])
		{
			try
			{
				v = (Volume) new VolumeShort(imp.getStack(),
					aspectx, aspecty, aspectz);
			} catch (Exception exc) {  }
			renderer.setOutputGrayscale();
		}
		else if (imp.getStack().getImageArray()[0] instanceof int [])
		{
			Object [] array = imp.getStack().getImageArray();
			v = (Volume) new VolumeRGB(array,
				imp.getStack().getWidth(),
				imp.getStack().getSize(),
				aspectx, aspecty, aspectz);
			// Maybe user wants quick monochrome. Check flag here.
			renderer.setOutputColor();
		}
		else if (imp.getStack().getImageArray()[0] instanceof float [])
		{
			v = (Volume) new VolumeFloat(imp.getStack(),
				aspectx, aspecty, aspectz);
			// Should depend on indexing. Check flag here.
			renderer.setOutputGrayscale();
		}
		// If the classifier wants an index, load the index values from the stack into the voxels.
		if (renderer.getClassifier().doesIndex())
		{
			if (impindex == null)
				write(""+renderer.getClassifier()+" needs index, but none selected: set to 0.");
			else if (! ((impindex.getStack().getImageArray()[0]) instanceof byte[]))
				IJ.error("Index stack can only be 8-bit");
			else if (v instanceof VolumeShort)
				((VolumeShort)v).setHighBits(impindex.getStack());
			else if (v instanceof VolumeRGB)
				((VolumeRGB)v).setIndex((byte [][]) impindex.getStack().getImageArray());
			else
				write("Indexing implemented for 8 bit grayscale or 32 bit color volumes only.");
		}
		return v;
	}
	/**
	 * Create a shader and light for the current light coordinates and aspect ratio.
	 * The position of the light is in in viewspace coordinates, so a correction
	 * to objectspace is necessary later.
	 * The position of the light is influenced by the aspect ratio, since that is included as
	 * an axis dependent scaling in the transformation matrix of the renderer,
	 * and not really as a property of the volume. Therefore correct the position of the light
	 * for the aspect ratio.
	 * @param lightx the x-position of the light in viewspace.
	 * @param lighty the y-position of the light in viewspace.
	 * @param lightz the z-position of the light in viewspace.
	 * @param aspectx the aspect ratio of the volume along the x-axis.
	 * @param aspecty the aspect ratio of the volume along the y-axis.
	 * @param aspectz the aspect ratio of the volume along the z-axis.
	 * @return the VJShader.
	*/
	protected VJShader resetShader(float lightx, float lighty, float lightz,
		float aspectx, float aspecty, float aspectz)
	{
		// Create a white light with 0.9 diffuse light and no specular light.
		VJLight light = new VJLight((float) lightx, (float) lighty, (float) lightz, (float) 0.9, 0);
		// Create a shader, with 0.1 background light.
		VJShader shader = new VJPhongShader((float) 0.1, light,
			backCheckbox instanceof Checkbox && backCheckbox.getState());
		//VJShader shader = new VJOutlineShader();
		return shader;
	}
	/**
	 * Create and setup a classifier according to the classifier number index (index only vali within VJUserInterface).
	 * @param index the local index number of the classifier in VJClassifiers.
	 * @param lutIndex a flag indicating what LUT to use. 0 is built in LUT, 1 = user loadable LUT..
	 * @return a VJClassifier.
	*/
	protected VJClassifier resetClassifier(int index, int lutIndex)
	{
		VJClassifier classifier = VJClassifiers.getClassifier(index);
		if (classifier instanceof VJClassifierIsosurface)
			((VJClassifierIsosurface) classifier).setThreshold((float) getFloatField(thresholdField));
		if (classifier instanceof VJClassifierLevoy)
		{
			((VJClassifierLevoy) classifier).setThreshold((float) getFloatField(thresholdField));
			((VJClassifierLevoy) classifier).setWidth((float) getFloatField(widthField));
		}
		// Ask for LUT if requested by user
		if (classifier.hasLUT() && lutIndex==1)
			getLUT(classifier, "");
		// Set the description String for this classifier.
		classifierTextArea.setText(classifier.toLongString());
		return classifier;
	}
	/**
	 * Propagate the change of classifier onto other dialog items and instances.
	 */
	protected void propagateClassifier()
	{
		switch (mappingChoice.getSelectedIndex())
		{
			case RAYTRACE: // raytrace rendering.
			case VIEWSPACE: // viewspace.
				classificationChoice.setEnabled(true);
				if (classificationChoice.getSelectedIndex() == VJClassifiers.ISOSURFACE)
					classificationChoice.select(VJClassifiers.LEVOYNOINDEX);
				break;
			case ISOSURFACE: // isosurface.
				classificationChoice.setEnabled(false);
				classificationChoice.select(VJClassifiers.ISOSURFACE);
				break;
		}
		VJClassifier c = VJClassifiers.getClassifier(classificationChoice.getSelectedIndex());
		if (c instanceof VJClassifier)
		{
			// Does classifier support indexing?
			if (indexChoice instanceof Choice)
				indexChoice.setEnabled(c.doesIndex());
			if (c instanceof VJClassifierIsosurface)
			{
				thresholdField.setEnabled(true);
				widthField.setEnabled(false);
				((VJClassifierIsosurface) c).setThreshold((float) getFloatField(thresholdField));
			}
			else if (c instanceof VJClassifierValue)
			{
				thresholdField.setEnabled(true);
				widthField.setEnabled(false);
				((VJClassifierValue) c).setThreshold((float) getFloatField(thresholdField));
			}
			else if (c instanceof VJClassifierLevoy)
			{
				thresholdField.setEnabled(true);
				widthField.setEnabled(true);
				((VJClassifierLevoy) c).setThreshold((float) getFloatField(thresholdField));
				((VJClassifierLevoy) c).setWidth((float) getFloatField(widthField));
			}
			lutChoice.setEnabled(c.hasLUT());
			classifierTextArea.setText(c.toLongString());
		}
		classifier = null;
		v = null;
	}
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	// Create a series of text fields with one label (useful for vectors)
	protected TextField [] createXYZField(Panel p, String s, String [] d, int i)
	{
			Label ll = new Label(s+"  ", Label.RIGHT);
			p.add(ll);
			Panel xyz = new Panel();
			xyz.setLayout(new GridLayout(0, d.length));
			TextField [] t = new TextField[d.length];
			for (int j = 0; j < d.length; j++)
			{
					t[j] = new TextField(""+d[j], i);
					t[j].setEditable(true);
					xyz.add(t[j]);
					t[j].addKeyListener(this);
			}
			p.add(xyz);
			return (t);
	}
	/** Create a series of text fields labeled with no label */
	protected TextField [] createXYZField(Panel p, String [] d, int i)
	{
			Panel xyz = new Panel();
			xyz.setLayout(new GridLayout(0, d.length));
			TextField [] t = new TextField[d.length];
			for (int j = 0; j < d.length; j++)
			{
					t[j] = new TextField(""+d[j], i);
					t[j].setEditable(true);
					xyz.add(t[j]);
					t[j].addKeyListener(this);
			}
			p.add(xyz);
			return (t);
	}
	// Create a series of text fields with one label (useful for vectors)
	protected TextField [] createXYZField(Panel [] p, String s, String [] d, int i)
	{
		Label ll = new Label(s+"  ", Label.RIGHT);
		p[0].add(ll);
		Panel xyz = new Panel();
		xyz.setLayout(new GridLayout(0, d.length));
		TextField [] t = new TextField[d.length];
		for (int j = 0; j < d.length; j++)
		{
			t[j] = new TextField(""+d[j], i);
			t[j].setEditable(true);
			xyz.add(t[j]);
			t[j].addKeyListener(this);
		}
		p[1].add(xyz);
		return (t);
	}
	/** Create a choice item, fill it with d strings and preselect item i. */
	protected Choice createChoice(Panel [] p, String s, String [] d, int i)
	{
		p[0].add(new Label(s+"  ", Label.RIGHT));
		Choice t = new Choice();
		for (int j = 0; j < d.length; j++)
			t.addItem(d[j]);
		t.select(i);
		p[1].add(t);
		t.addItemListener(this);
		return (t);
	}
	/** Create a choice item, fill it with d strings and preselect item i. */
	protected Choice createChoice(Panel p, String s, String [] d, int i)
	{
		p.add(new Label(s+"  ", Label.RIGHT));
		Choice t = new Choice();
		for (int j = 0; j < d.length; j++)
			t.addItem(d[j]);
		t.select(i);
		p.add(t);
		t.addItemListener(this);
		return (t);
	}
	/** Create a choice item, fill it with d strings and preselect item i. */
	protected Choice createChoice(Panel p, GridBagConstraints constr, GridBagLayout layout,
		String s, String [] d, int i)
	{
		Label l = new Label(s+"  ", Label.RIGHT);
		constr.gridwidth = GridBagConstraints.RELATIVE;
		layout.setConstraints(l, constr);
		p.add(l);
		Choice c = new Choice();
		for (int j = 0; j < d.length; j++)
			c.addItem(d[j]);
		c.select(i);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(c, constr);
		p.add(c);
		c.addItemListener(this);
		return (c);
	}
	/**
	 * Create an entry item named s, with i characters in it.
	 */
	protected TextField createTextField(Panel p, GridBagConstraints constr,
		GridBagLayout layout, String s, String d, int i)
	{
		Label l = new Label(s+"  ", Label.RIGHT);
		constr.gridwidth = GridBagConstraints.RELATIVE;
		layout.setConstraints(l, constr);
		p.add(l);
		TextField t = new TextField(d, i);
		t.setEditable(true);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(t, constr);
		p.add(t);
		t.addActionListener(this);
		return (t);
	}
	/**
	 * Create a text field with default content "d" of length i (in characters), labeled by "s".
	 * @param p the panel to which the text field will be added.
	 * @param label the label of the textfield.
	 * @param defaultContents the default contents of the text field.
	 * @param nrChars the length in characters of the text field.
	 * @return the Textfield that was created.
	 */
	protected TextField createTextField(Panel p, String label, String defaultContents, int nrChars)
	{
		Label l = new Label(label+"  ", Label.RIGHT);
		p.add(l);
		TextField t = new TextField(""+defaultContents, nrChars);
		t.setEditable(true);
		p.add(t);
		t.addKeyListener(this);
		return (t);
	}
	/**
	 * Get the value of a Textfield as a float.
	 * @param t a TextField.
	 * @return a float with the value of the text field, 0 if an error.
	 */
	protected float getFloatField(TextField t)
	{
		String s = t.getText();
		Float d;
		try {d = new Float(s);}
		catch (NumberFormatException e)
		{
			d = null;
		}
		if (d != null)
			return d.floatValue();
		else
			return 0;
	}
	/**
	 * Get the value of a Textfield as an int.
	 * @param t a TextField.
	 * @return an int with the value of the text field, 0 if an error.
	 */
	protected int getIntField(TextField t)
	{
		String s = t.getText();
		Integer d;
		try {d = new Integer(s);}
		catch (NumberFormatException e)
		{
			d = null;
		}
		if (d != null)
			return d.intValue();
		else
			return 0;
	}
	/**
	 * Get a long from a TextField.
	 * If contains an erroneous entry, return 0.
	 * @param t the Textfield to parse.
	 * @return a long, 0 if contents could not be parsed.
	 */
	protected long getLongField(TextField t)
	{
		String s = t.getText();
		Long d;
		try {d = new Long(s);}
		catch (NumberFormatException e)
		{
			d = null;
		}
		if (d != null)
			return d.longValue();
		else
			return 0;
	}
	/**
	 * Get the ids of all open windows that are ImageJ stacks.
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
			// Is it a suitable stack window?
			if (imp instanceof ImagePlus && /*imp != this &&*/ imp.getStackSize() > 1)
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
	 * Get the names (by getTitle() method) from a list of windows.
	 * @param windows an int array containing the ids of windows.
	 * @return a String array containing all names of windows
	*/
	protected String [] getWindowNames(int [] windows)
	{
		String [] s = new String[windows.length];
		for (int i = 0; i < s.length; i++)
		{
			s[i] = WindowManager.getImage(windows[i]).getTitle();
			if (s[i] == null)
				s[i] = "";
		}
		return s;
	}
	/**
	 * Writes a progress bar to a status channel.
	 * @param s the percentage that is filled.
	 */
	static void progress(float d)
	{ IJ.showProgress(d); }
	/**
	 * Writes a string to an error channel.
	 * @param s the String that will be output.
	 */
	static void error(String s)
	{ IJ.error(s); }
	/**
	 * Writes a string to an output channel. IJ cannot write if an applet, divert to showStatus instead.
	 * @param s the String that will be output.
	 */
	static void write(String s)
	{ if (IJ.getApplet() == null) IJ.write(s); else IJ.showStatus(s); }
	/**
	 * Writes a string to a status channel.
	 * @param s the String that will be shown in the status window.
	 */
	static void status(String s)
	{ IJ.showStatus(s); }
	/**
	 * Writes a string to an output channel.
	 * @param s the StringBuffer that will be output.
	 */
	static void write(StringBuffer s)
	{ write(s.toString()); }
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
			write("Classifier LUT set to: "+path);
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
