package FlowJ;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import volume.*;

/**
 * This class links the FlowJ classes to the ImageJ interface.<br>
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
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
public class FlowJUserInterface extends ImagePlus implements ActionListener, ClipboardOwner
{
        private static          String version = "1.29";
        private Button          save, copy, error, read, rotate, display, centralImage, graph;
        private Button          compute, computeAll, trans, indexButton;
        private Label           rLabel, sLabel, alphaField, posLabel;
        private Choice          gradientChoice, regularizationChoice, algorithmChoice, mappingChoice;
        private TextField       rhoField, tauField, sigmasField, regionField, sigmatField, frameField;
        private TextField       xField, yField, recursionsLabel, lambdaField, kField, sigmawField;
        private TextField       resLabel, condField, sigmafField, maxampField, taufField, taus1Field, taus2Field;
        private TextField       scaleField;
        private Checkbox        staticCheckbox, normalsCheckbox;
        private static String   defaultDirectory = null;
        private float          rho, tau, sigmas, sigmat;
        private int             region, xError, yError, frame;
        private FlowJFlow     	trueFlow, flow;
        private Vector          flows;
        private FlowJError  	flowError;
        private ImagePlus       imp;
        private String          description = "";
        private boolean         firstTime;
        private boolean         hasVolume;

        private final static int   LK = 0;
        private final static int   URAS = 1;
        private final static int   SINGH = 2;
        private final static int   FLEET = 3;

        public FlowJUserInterface()
        {
                discoverEnvironment();
        }
        protected void discoverEnvironment()
        {
                if (! firstTime)
                {
                        firstTime = true;
                        imp = WindowManager.getCurrentImage();
                        int width = 100; int height = 100;
                        if (imp instanceof ImagePlus && imp.getStackSize() > 1)
                        {
                                this.imp = imp;
                                hasVolume = true;
                                imp.setSlice(imp.getStackSize()/2);
                                ImageStack stack = imp.getStack();
                                width = stack.getWidth(); height = stack.getHeight();
                        }
                        ColorProcessor cp = new ColorProcessor(width, height, new int[width*height]);
                        cp.setColor(new Color(0xffffff)); cp.fill();
                        setProcessor("2D Optical flow", cp);
                        show();
                }
        }
        /**
         * Shows Dialog. Sets all parameter entries.
         * Overrides ImagePlus.show().
         */
        public void show()
        {
		  setTitle("FlowJ "+version+" ");
		  img = ip.createImage();
		  win = new ImageWindow(this, new FlowJCanvas(this));
		  win.setLayout(new FlowLayout());

		  Panel buttons = new Panel();
		  buttons.setLayout(new GridLayout(0,1));

		  compute = new Button("Compute flow field");
		  compute.addActionListener(this);
		  buttons.add(compute);
		  computeAll = new Button("Compute all flow fields");
		  computeAll.addActionListener(this);
		  buttons.add(computeAll);
		  display = new Button("Display");
		  display.addActionListener(this);
		  buttons.add(display);
		  read = new Button("Open flow-field... ");
		  read.addActionListener(this);
		  buttons.add(read);
		  save = new Button("Save flow-field... ");
		  save.addActionListener(this);
		  buttons.add(save);
		  error = new Button("\u03c8 vs. true file... ");
		  error.addActionListener(this);
		  buttons.add(error);
		  copy = new Button("\u03c8 field to Clipboard ");
		  copy.addActionListener(this);
		  buttons.add(copy);
		  trans = new Button("Translating flow field ");
		  trans.setEnabled(false);
		  trans.addActionListener(this);
		  buttons.add(trans);
		  rotate = new Button(" Rotation flow field ");
		  rotate.setEnabled(false);
		  rotate.addActionListener(this);
		  buttons.add(rotate);
		  centralImage = new Button(" Copy central frame ");
		  centralImage.addActionListener(this);
		  buttons.add(centralImage);
		  graph = new Button(" Graph ");
		  graph.addActionListener(this);
		  buttons.add(graph);
		  indexButton = new Button(" Display DC index image");
		  indexButton.addActionListener(this);
		  buttons.add(indexButton);

		  staticCheckbox = new Checkbox("Display static background");
		  staticCheckbox.setState(true);
		  buttons.add(staticCheckbox);
		  normalsCheckbox = new Checkbox("Include normals");
		  normalsCheckbox.setState(false);
		  buttons.add(normalsCheckbox);
		  rLabel = new Label("");
		  buttons.add(rLabel);
		  sLabel = new Label("");
		  buttons.add(sLabel);
		  alphaField = new Label("");
		  buttons.add(alphaField);
		  posLabel = new Label("x:   y:            ");
		  buttons.add(posLabel);
		  win.add(buttons);
		  // sets the fields so they can be accessed in FlowCanvas.
		  ((FlowJCanvas) win.getCanvas()).setFields(alphaField, rLabel, sLabel, posLabel);

		  Panel params = new Panel();
		  params.setLayout(new GridLayout(0, 2));
		  if (hasVolume)
                        frameField = CreateTextField(params, "Central slice:", ""+((int) imp.getStackSize() / 2), 0);
		  String [] salgorithm = { "Lucas & Kanade", "Uras", "Singh", "Fleet & Jepson" };
		  algorithmChoice = CreateChoice(params, "Algorithm", salgorithm, 0);
		  gradientChoice = CreateChoice(params, "Gradient method:", FlowJLucas.sderiv, 0);
		  sigmasField = CreateTextField(params, "\u03a3-s (LK,U,FJ):", "1.5", 2);
		  sigmatField = CreateTextField(params, "\u03a3-t (LK,U,FJ):", "1.0", 2);
		  tauField = CreateTextField(params, "\u03c4 (LK,U):", "1.0", 2);
		  sigmawField = CreateTextField(params, "\u03a3-w (LK):", "1.0", 2);
		  regularizationChoice = CreateChoice(params, "Regularization (LK)", FlowJLucas.sregul, 0);
		  regionField = CreateTextField(params, "Region-size (U):", "1", 0);
		  resLabel = CreateTextField(params, "Residual < (F)", "0.5", 2);
		  condField = CreateTextField(params, "Condition number < (F)", "10.0", 2);
		  maxampField = CreateTextField(params, "Maxamp (F):", "0.1", 2);
		  taufField = CreateTextField(params, "\u03c4 (F):", "2.5", 2);
		  taus1Field = CreateTextField(params, "\u03c4-1 (S):", "0.0", 2);
		  taus2Field = CreateTextField(params, "\u03c4-2 (S):", "0.0", 2);
		  mappingChoice = CreateChoice(params, "Mapping type", FlowJDisplay.description, 0);
		  rhoField = CreateTextField(params, "\u03c1:", "2.0", 1);
		  scaleField = CreateTextField(params, "Display scaling:", "2", 1);
		  win.add(params);
		  win.pack();
		  draw();
        }
	public void actionPerformed(ActionEvent e)
	{
                if (e.getSource()==compute)
                        doCompute(false);
                else if (e.getSource()==computeAll)
                        doCompute(true);
                else if (e.getSource()==display)
                        display();
                else if (e.getSource()==rotate)
                        doRotation();
                else if (e.getSource()==error)
                        doError();
                else if (e.getSource()== read)
                        doRead();
                else if (e.getSource()== save)
                        doSave();
                else if (e.getSource()==centralImage)
                        doCentralImage();
                else if (e.getSource()==graph)
                        doGraph();
                else if (e.getSource()==indexButton)
                        doIndex();
                else
                        copyToClipboard();
        }
	/**
         * Display the flow field in a small size.
	*/
	private void fastDisplay()
	{
                if (flow instanceof FlowJFlow)
                {
                        ImageStack stack = imp.getStack();
                        ImageProcessor ip;
                        if (staticCheckbox.getState())
                                ip = flow.mapImage(stack.getProcessor((int) getFloatField(frameField)),
					FlowJDisplay.DCM2D, 0, 1, getFloatField(rhoField));
                        else
                                ip = flow.mapImage(null, FlowJDisplay.DCM2D, 0, 1, getFloatField(rhoField));
                        setProcessor("2D DCM "+description, ip);
                        draw();
                }
	}
	/**
         * Display the flow field.
	*/
	private void display()
	{
                rho = getFloatField(rhoField);
                ImageStack stack = imp.getStack();
                float scale = getFloatField(scaleField);
                int mapping = mappingChoice.getSelectedIndex();
                ImagePlus nimp;
                if (flows.size() == 1)
                {
		        ImageProcessor ip;
                        FlowJFlow flow = (FlowJFlow) flows.elementAt(0);
    	                if (staticCheckbox.getState())
                                // include the static background (grayscale) image.
                                ip = flow.mapImage(stack.getProcessor((int) getFloatField(frameField)),
                                        mapping, 0, scale, rho);
		        else
                                // Only display the flow.
                                ip = flow.mapImage(null, mapping, 0, scale, rho);
		        nimp = new ImagePlus(flow.getTitle(mapping, scale, rho), ip);
                }
                else
                {
                        // First get the size of a mapped flow field (the central one).
                        ImageProcessor ip = flow.mapImage(stack.getProcessor(stack.getSize() / 2),
                                mapping, 0, scale, rho);
                        ImageStack is = new ImageStack(ip.getWidth(), ip.getHeight());
                        for (int frame = 0; frame < stack.getSize(); frame++)
                        {
                                FlowJFlow flow = (FlowJFlow) flows.elementAt(frame);
                                if (flow != null)
                                {
                                        ip = flow.mapImage(stack.getProcessor(frame+1),
                                                mapping, 0, scale, rho);
				        is.addSlice(""+frame, ip);
                                }
                        }
                        nimp = new ImagePlus("All flows", is);
                }
                nimp.show();
	}
	/**
         * Make a dynamic color index image.
         */
	private void doIndex()
	{
		int w = 256, h = w;

		ImageProcessor ip = new ColorProcessor(w, h);
		int[] pixels = (int[])ip.getPixels();
		int i = 0;
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
			{
				float rx = (x - (float)w / 2)/((float)w/2);
				float ry = -(y - (float)h / 2)/((float)w/2);
				float [] p = FlowJFlow.polar(rx, ry);
				if (p[0] <= 1)
				{
                                        byte [] rgb = new byte[3];
                                        FlowJDynamicColor.map2D(rgb, p[0], p[1]);
                                        pixels[i] = ((rgb[0]&0xff) << 16) | ((rgb[1]&0xff) << 8) | (rgb[2]&0xff);;
				}
				i++;
			}
		new ImagePlus("DC Index", ip).show();
	}
	private void doGraph()
	{
		  if (flow instanceof FlowJFlow && trueFlow instanceof FlowJFlow)
		  {
				ImagePlus imp = new ImagePlus("True-Estimate "+description,
				GUI.createBlankImage(300,300));
				ImageWindow imw = new ImageWindow(imp);
				imp.show();
				Image img = imp.getImage();
				FlowJError.map(img.getGraphics(), 300, 300, flow, trueFlow);
		  }
		  else
				IJ.error("flow or true flow not known");
	}
	/**
         * Compare the current flowfield to a flowfield on disk and perform error analysis.
         * Conform to the error analysis in Barron, 1994.
	*/
	private void doError()
	{
                FileDialog fd = new FileDialog(win, "True flow field to compare to...", FileDialog.LOAD);
                if (defaultDirectory!=null)
                        fd.setDirectory(defaultDirectory);
                        fd.setVisible(true);
                String name = fd.getFile();
                String directory = fd.getDirectory();
                defaultDirectory = directory;
                fd.dispose();
                // Contains the true flow field (or at least the one to comapre to).
                trueFlow = new FlowJFlow();
                if (! trueFlow.read(directory+name))
				return;
                if (flow.getWidth() != trueFlow.getWidth() || flow.getHeight() != trueFlow.getHeight())
                {
				IJ.error("flow field and "+name+" not the same size");
				return;
                }
                flowError = new FlowJError(flow.getWidth(), flow.getHeight());
                ((FlowJCanvas) win.getCanvas()).setDisplayed(flowError);
                // Compute the deviation from the true flow field.
                flowError.computePsi(flow, trueFlow);
                ColorProcessor cp = new ColorProcessor(flow.getWidth(), flow.getHeight());
                flowError.map(cp);
                setProcessor("Errors", cp);
                draw();
                flowError.averageOverVelocity();
		  //ImagePlus imp = new ImagePlus("Error histogram - " + stack.getSliceLabel(), GUI.createBlankImage(WIN_WIDTH, WIN_HEIGHT));
		  //imp.show();
		  //Image img = imp.getImage();
		  //flowError.mapAverage(img.getGraphics());
		  //imp.draw();
	}
	private void doRotation()
	{
		  ImageStack stack = imp.getStack();
		  flow = new FlowJFlow(stack.getWidth(), stack.getHeight());
		  xError = getIntField(xField);
		  yError = getIntField(yField);
		  flow.createRotation(xError, yError, -5 *  (float) Math.PI / 180, getRoi());
		  ((FlowJCanvas) win.getCanvas()).setDisplayed(flow);
		  description = "rotation field";
		  fastDisplay();
	}
	private void doCentralImage()
	// Put the current frame of the stack into the flow field display.
	{
		  ImageStack stack = imp.getStack();
		  frame = (int) getFloatField(frameField);
		  setProcessor("Central image", stack.getProcessor(frame));
		  draw();
	}
	private void doRead()
	/*
		  Open an external flow field and display.
	*/
	{
		  FileDialog fd = new FileDialog(this.win, "Read flow file...", FileDialog.LOAD);
		  if (defaultDirectory!=null)
					fd.setDirectory(defaultDirectory);
		  fd.setVisible(true);
		  String name = fd.getFile();
		  String directory = fd.getDirectory();
		  defaultDirectory = directory;
		  fd.dispose();
		  FlowJFlow newflow = new FlowJFlow();
		  ImageStack stack = imp.getStack();
		  if (flow.read(directory+name))
		  {
				if (flow.getWidth() != stack.getWidth() || flow.getHeight() != stack.getHeight())
				{
					  IJ.error("Flow field does not fit this estimator.\nPlease create a new one.");
					  return;
				}
				IJ.wait(250);  // give system time to redraw ImageJ window
				description = name;
				fastDisplay();
				IJ.write("Read: "+name);
				flow = newflow;
		  }
	}
        private void doSave()
	/*
		  Save the flow field in a file.
	*/
	{
		  FileDialog fd = new FileDialog(this.win, "Save flow in file...", FileDialog.SAVE);
			  if (defaultDirectory!=null)
					fd.setDirectory(defaultDirectory);
			  fd.setFile(description+".flow");
		  fd.setVisible(true);
			  String name = fd.getFile();
			  String directory = fd.getDirectory();
			  defaultDirectory = directory;
			  fd.dispose();
		  flow.write(directory+name);
	}
	/**
         * Compute the flow for a single or a range of frames in the stack.
         * The parameters are read from the dialog window.
         * The flows are left in the flow object.
         * @param all indicates whether to compute only for a single frame or for all possible frames in the stack.
         */
        public void doCompute(boolean all)
	{
                long start = System.currentTimeMillis();
                IJ.showStatus("Computing 2D optical flow...");
                flows = new Vector();
                if (! all)
                {
                        int frame = (int) getFloatField(frameField);
                        try
                        {
                                computeSingleFrame(frame);
                                flows.addElement(flow);
                                // show as a stack.
		                (new ImagePlus("2D Flow xy", flow.toStack())).show();
		                // If a true flow field was previously loaded.
		                if (trueFlow instanceof FlowJFlow)
		                {
				        // Compute the deviation from the true flow field.
				        flowError.computePsi(flow, trueFlow);
				        flowError.averageOverVelocity();
                                }
                        }
                        catch (FlowJException e) { IJ.error(e.toString()); return; }
                }
                else
                {
                        ImageStack stack = imp.getStack();
                        // Just start at frame 0 to the end; exception will be thrown anyway.
                        int first = FlowJLucas.firstFrame(stack, getFloatField(sigmatField), gradientChoice.getSelectedIndex());
                        int last = FlowJLucas.lastFrame(stack, getFloatField(sigmatField), gradientChoice.getSelectedIndex());
                        for (int frame = 1; frame <= stack.getSize(); frame++)
                        {
                                if (frame >= first && frame <= last)
                                {
                                        try
                                        {
                                                IJ.showStatus("Computing 2D optical flow ("+(frame)+")...");
                                                computeSingleFrame(frame);
                                                flows.addElement(flow);
                                        }
                                        // Disregard FlowJExceptions
                                        catch (FlowJException e)
                                        {
                                                IJ.write("FlowJException: "+e);
                                                flows.addElement(null);
                                        }
                                }
                                else
                                        flows.addElement(null);
                        }
                }
                long elapsedTime = System.currentTimeMillis() - start;
                float seconds = (float) elapsedTime / 1000;
                long pxs = imp.getStack().getWidth() * imp.getStack().getHeight();
                IJ.write(""+seconds + " sec" + ", " + (int)((float)pxs / seconds) + " pixels/second");
        }
	/**
         * Compute the flow from the current stack.
         * The parameters are read from the dialog window.
         * The flows are left in the flow object.
         * @param frame the frame for which to compute the flow.
         * @exception FlowJException if there were problems (including frame out of bounds).
         */
        public void computeSingleFrame(int frame)
        throws FlowJException
	{
		  ImageStack stack = imp.getStack();
		  sigmat = getFloatField(sigmatField);
		  tau = getFloatField(tauField);
		  region = getIntField(regionField);
		  sigmas = getFloatField(sigmasField);
		  switch (algorithmChoice.getSelectedIndex())
		  {
			  case FLEET:
					  FlowJFleet fleet = new FlowJFleet();
					  fleet.filterAll(stack, frame, sigmat, sigmas);
                                          // compute the normals.
					  fleet.normals(getFloatField(maxampField), getFloatField(taufField));
					  flow = new FlowJFlow(fleet.getWidth(), fleet.getHeight());
					  IJ.showStatus("computing flows (Fleet)...");
					  // compute the flows
					  fleet.computeFull(flow, getFloatField(condField), getFloatField(resLabel));
					  description = fleet.toString();
					  IJ.write(description);
					  break;
			  case LK:
					  FlowJLucas lk = new FlowJLucas();
					  lk.filterAll(stack, frame, sigmat, sigmas,
							gradientChoice.getSelectedIndex());
					  flow = new FlowJFlow(stack.getWidth(), stack.getHeight());
					  IJ.showStatus("computing flows (LK)...");
					  lk.computeFull(flow, normalsCheckbox.getState(),
                                                getFloatField(sigmawField), tau, regularizationChoice.getSelectedIndex());
					  description = lk.toString();
					  IJ.write(description);
					  break;
			  case URAS:
					  // Uras.
					  FlowJUras uras = new FlowJUras();
					  uras.filterAll(stack, frame, sigmat, sigmas);
					  uras.gradients();
					  flow = new FlowJFlow(uras.getWidth(), uras.getHeight());
					  uras.computeFull(flow, region, tau);
					  description = uras.toString();
					  IJ.write(description);
					  break;
			  case SINGH:
					  FlowJSingh singh = new FlowJSingh();
					  singh.filterAll(stack, frame, 1);
					  flow = new FlowJFlow(singh.getWidth(), singh.getHeight());
					  singh.compute1(flow, getFloatField(taus1Field));
					  //ColorProcessor ip = new ColorProcessor(flow.getWidth(), flow.getHeight(), pixels);
					  //ImagePlus imp = new ImagePlus("Singh step 1" + stack.getSliceLabel(), ip); imp.show();
					  //singh.compute2(flow, getFloatField(taus2Field));
					  description = singh.toString();
					  IJ.write(description);
		  } // switch
		  // Display the flow field in the canvas.
		  IJ.showStatus("displaying...");
		  ((FlowJCanvas) win.getCanvas()).setDisplayed(flow);
		  fastDisplay();
		  flow.setCalibration(imp.getCalibration());
		  IJ.showStatus("");
	}
        void copyToClipboard()
	{
		  if (flowError instanceof FlowJError)
		  {
				Clipboard systemClipboard = null;
					try {systemClipboard = win.getToolkit().getSystemClipboard();}
					catch (Exception e) {systemClipboard = null; }
					if (systemClipboard==null)
				{IJ.error("Unable to copy to Clipboard."); return;}
					IJ.showStatus("Copying error statistics...");
				CharArrayWriter aw = new CharArrayWriter(8);
				PrintWriter pw = new PrintWriter(aw);
				flowError.clipboard(pw);
				String text = aw.toString();
					pw.close();
					StringSelection contents = new StringSelection(text);
					systemClipboard.setContents(contents, this);
					IJ.showStatus(text.length() + " characters copied to Clipboard");
		  }
        }
        public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	private Choice CreateChoice(Panel p, String s, String [] d, int i)
	{
                Label l;
                Choice t;

                l = new Label(s);
                p.add(l);
                t = new Choice();
                        for (int j=0; j<d.length; j++)
                                        t.addItem(d[j]);
                        t.select(i);
                p.add(t);
                return (t);
	}
	private TextField CreateTextField(Panel p, String s, String d, int i)
	{
			Label l;
			TextField t;

			l = new Label(s);
			p.add(l);
			t = new TextField(d + "", i);
			t.setEditable(true);
			p.add(t);
			return (t);
	}
	private float getFloatField(TextField t)
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
	private int getIntField(TextField t)
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
	public static void showStatus(String s) {   IJ.showStatus(s); }
	public static void write(String s) {   IJ.write(s); }
	public static void showProgress(float d) { IJ.showProgress(d); }
	public static void error(String s) { IJ.error(s); }
}

// Private class to create an interface for the flow field.
class FlowJCanvas extends ImageCanvas
{
        private Object    display;
        private Label     alphaField, rLabel, sLabel, posLabel;

        public FlowJCanvas(ImagePlus imp)
        {
                super(imp);
        }
        public void setDisplayed(Object display) { this.display = display; }
        public void setFields(Label alphaField, Label rLabel, Label sLabel, Label posLabel)
        {
                this.alphaField = alphaField;
                this.rLabel = rLabel;
                this.sLabel = sLabel;
                this.posLabel = posLabel;
        }
        public void mouseMoved(MouseEvent e)
        {
                int x = e.getX();
                int y = e.getY();
                if (display instanceof FlowJError)
                {
                        FlowJError fe = (FlowJError) display;
                        alphaField.setText("exp: " + IJ.d2s(fe.xExpected(x, y), 1) + ", " + IJ.d2s(fe.yExpected(x, y), 1));
                        rLabel.setText("act: " + IJ.d2s(fe.flow.getX(x, y), 1) + ", " + IJ.d2s(fe.flow.getY(x, y), 1));
                        posLabel.setText(x + ", " + y + ": psi = " + fe.psi(x, y)+"º");
                }
                else if (display instanceof FlowJFlow)
                {
                        FlowJFlow flow = (FlowJFlow) display;
                        if (flow.full(x, y))
                        {
                                alphaField.setText("" + IJ.d2s(flow.getAlphaDeg(x, y), 2)+"º");
                                rLabel.setText("" + IJ.d2s(flow.getMagnitude(x, y), 2)+"pxs/frame");
                                sLabel.setText("(" + flow.calibratedMagnitudeString(flow.getMagnitude(x,y), 2)+")");
                                posLabel.setText(x + ", " + y);
                        }
                        else
                        {
                                alphaField.setText("");
                                rLabel.setText("no flow");
                                posLabel.setText(x + ", " + y);
                        }
                }
                else if (alphaField instanceof Label)
                {
                        posLabel.setText("pos:");
                        alphaField.setText("orientation:");
                        rLabel.setText("magnitude:");
                }
        }
        public void mousePressed(MouseEvent e) { super.mousePressed(e); }
        public void mouseReleased(MouseEvent e)
        {
                super.mouseReleased(e);
                if (display instanceof FlowJFlow && imp.getRoi() instanceof Roi)
                {
                        FlowJFlow flow = (FlowJFlow) display;
                        int s = flow.average(imp.getRoi());
                        alphaField.setText("avg orientation: " + IJ.d2s(flow.averageAlpha(), 2)+"º");
                        rLabel.setText("avg speed: " + IJ.d2s(flow.averageMagnitude(), 2)+"pxs/frame");
                        sLabel.setText("(" + flow.calibratedMagnitudeString(flow.averageMagnitude(), 2)+")");
                        posLabel.setText("surface: " + s + "pxs");
                }
                else if (display instanceof FlowJError && imp.getRoi() instanceof Roi)
                {
                        FlowJError fe = (FlowJError) display;
                        alphaField.setText("avg psi: " + IJ.d2s(((FlowJError) display).average(imp.getRoi()), 2)+"º");
                        rLabel.setText("");
                        posLabel.setText("");
                }
        }
}
