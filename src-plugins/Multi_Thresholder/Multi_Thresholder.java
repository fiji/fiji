import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.frame.*;
import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.Recorder;


public class Multi_Thresholder extends PlugInFrame implements PlugIn, Measurements, Runnable, ActionListener, ItemListener{

    static final int ISODATA=0, ENTROPY=1, OTSU=2, MIXTURE=3;
    static final String[] modes = {"IsoData","Maximum Entropy", "Otsu", "Mixture Modeling"};
    static final double defaultMinThreshold = 85; 
    static final double defaultMaxThreshold = 170;
    static boolean fill1 = true;
    static boolean fill2 = true;
    static boolean useBW = true;
    static boolean backgroundToNaN = true;
    static Frame instance; 
    static int mode = ISODATA;  
    int bitDepth = 8;
    int hMin = 0;
    int hMax = 256;
    ThresholdPlot2 plot = new ThresholdPlot2();
    Thread thread;
    boolean doRethreshold,doRefresh,doReset,doApplyLut,doSet,applyOptions,skipDialog,alreadyRecorded;
    
    Panel panel;
    Button refreshB, resetB, applyB, setB;
    int previousImageID;
    int previousImageType;
    double previousMin, previousMax;
    ImageJ ij;
    double minThreshold, maxThreshold, threshold;  // 0-255
    Label label1, label2, threshLabel;
    boolean done;
    boolean invertedLut;
//    int lutColor;
    static Choice choice;
    boolean firstActivation;
    boolean macro;
    String macroOptions;
    
/** Enables the user to use a variety of threshold algorithms to threshold the image.**/
    public Multi_Thresholder() {
        super("MultiThresholder");
        macroOptions = Macro.getOptions();
        macro = macroOptions!=null;
        if (instance!=null && !macro) {
            instance.toFront();
            return;
        }
        WindowManager.addWindow(this);
        instance = this;
        ij = IJ.getInstance();
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp!=null) {
            setup(imp);        
            bitDepth = imp.getBitDepth();
        }
        if (macro) {
            skipDialog = true;
            mode = 0;
            macroOptions = macroOptions.toLowerCase(Locale.US);
            instance = null;
            if (macroOptions.indexOf("entropy")!=-1)
                mode = 1;
            else if (macroOptions.indexOf("otsu")!=-1)
                mode = 2;
            else if (macroOptions.indexOf("mixture")!=-1)
                mode = 3;
            setThresholdAlgorithm(mode);
            if (macroOptions.indexOf("apply")!=-1)
                applyThreshold(imp);
            close();
            return;
        }
        Font font = new Font("SansSerif", Font.PLAIN, 10);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        // plot
        int y = 0;
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 10, 0, 10);
        add(plot, c);
        // Threshold label
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = y++;
        c.weightx = IJ.isMacintosh()?10:0;
        c.insets = new Insets(5, 0, 0, 10);
        threshLabel = new Label("Threshold:", Label.CENTER);
        threshLabel.setFont(font);
        add(threshLabel, c);
        // choice
        choice = new Choice();
        for (int i=0; i<modes.length; i++)
            choice.addItem(modes[i]);
        choice.select(mode);
        choice.addItemListener(this);
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 2;
        c.insets = new Insets(5, 5, 0, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        add(choice, c);
        // buttons
        int trim = IJ.isMacOSX()?11:0;
        panel = new Panel();
        applyB = new TrimmedButton("Apply",trim);
        applyB.addActionListener(this);
        applyB.addKeyListener(ij);
        panel.add(applyB);
        refreshB = new TrimmedButton("Refresh",trim);
        refreshB.addActionListener(this);
        refreshB.addKeyListener(ij);
        panel.add(refreshB);
        resetB = new TrimmedButton("Reset",trim);
        resetB.addActionListener(this);
        resetB.addKeyListener(ij);
        panel.add(resetB);
        setB = new TrimmedButton("Set",trim);
        setB.addActionListener(this);
        setB.addKeyListener(ij);
        panel.add(setB);
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 2;
        c.insets = new Insets(0, 5, 10, 5);
        add(panel, c);
        
        addKeyListener(ij);  // ImageJ handles keyboard shortcuts
        pack();
        GUI.center(this);
        setThresholdAlgorithm(mode);
        firstActivation = true;
        show();

        thread = new Thread(this, "MultiThresholder");
        thread.start();
        if (Recorder.record)
            Recorder.recordOption(modes[mode]);
    }
    
    public synchronized void actionPerformed(ActionEvent e) {
        Button b = (Button)e.getSource();
        if (b==null) return;
        if (b==resetB)
            doReset = true;
        else if (b==refreshB)
            doRefresh = true;
        else if (b==applyB)
            doApplyLut = true;
        else if (b==setB)
            doSet = true;
        notify();
    }
    
    void setThresholdAlgorithm(int mode) {
        ImagePlus imp;
        ImageProcessor ip;
        imp = WindowManager.getCurrentImage();
        if (imp==null || bitDepth!=8 && bitDepth!=16 && bitDepth!=32) {
            IJ.beep();
            IJ.showStatus("No 8-bit, 16-bit, or 32-bit image");
            return;
        }
        ip = setup(imp);
        if (ip==null) {
            imp.unlock();
            IJ.beep();
            IJ.showStatus("RGB images cannot be thresholded");
            return;
        }
         ImageStatistics stats = imp.getStatistics();
         switch (mode) {
            case ISODATA:
                threshold = ip.getAutoThreshold(stats.histogram);
                break;
            case ENTROPY:
                EntropyThreshold2 entropy = new EntropyThreshold2();
                entropy.run(ip, stats.histogram);
                threshold = entropy.getThreshold();
                break;
            case OTSU:
                OtsuThreshold2 otsu = new OtsuThreshold2();
                otsu.run(ip, stats.histogram);
                threshold = otsu.getThreshold();
                break;
            case MIXTURE:
                MixtureModeling2 mixture = new MixtureModeling2();
                mixture.run(ip, stats.histogram);
                threshold = mixture.getThreshold();
                break;
        }
        updatePlot(imp);
        setThreshold(threshold, imp);
    }

    void setThreshold(double threshold, ImagePlus imp) {
         ImageProcessor ip = imp.getProcessor();
         if (invertedLut) {
             minThreshold = threshold;
             maxThreshold = 255;
         } else {
             minThreshold = 0;
             maxThreshold = threshold;
         }
         double threshold2 = scaleUp(ip, threshold);
         double minThreshold2, maxThreshold2;
         if (invertedLut) {
             minThreshold2 = threshold2;
             maxThreshold2 = ip.getMax();
         } else {
             minThreshold2 = 0;
             maxThreshold2 = threshold2;
         }
         ip.setThreshold(minThreshold2, maxThreshold2, ip.RED_LUT);
         imp.updateAndDraw();
         if (Recorder.record) {
            Recorder.record("run", "MultiThresholder", modes[mode]);
        }
         updateLabels(imp, ip);
    }
    
    public synchronized void itemStateChanged(ItemEvent e) {
        mode = choice.getSelectedIndex();
        setThresholdAlgorithm(mode);
        doRethreshold = true;
        notify();
    }

    ImageProcessor setup(ImagePlus imp) {
        ImageProcessor ip;
        int type = imp.getType();
        if (type==ImagePlus.COLOR_RGB)
            return null;
        ip = imp.getProcessor();
        boolean minMaxChange = false;
        boolean not8Bits = type==ImagePlus.GRAY16 || type==ImagePlus.GRAY32;
        if (not8Bits) {
            if (ip.getMin()!=previousMin || ip.getMax()!=previousMax)
                minMaxChange = true;
            previousMin = ip.getMin();
            previousMax = ip.getMax();
        }
        int id = imp.getID();
        if (minMaxChange || id!=previousImageID || type!=previousImageType) {
            //IJ.log(minMaxChange +"  "+ (id!=previousImageID)+"  "+(type!=previousImageType));
            if (not8Bits && minMaxChange) {
                ip.resetMinAndMax();
                imp.updateAndDraw();
            }
            invertedLut = imp.isInvertedLut();
            minThreshold = ip.getMinThreshold();
            maxThreshold = ip.getMaxThreshold();
            ImageStatistics stats = plot.setHistogram(imp);
            if (minThreshold==ip.NO_THRESHOLD)
                autoSetLevels(ip, stats);
            else {
                minThreshold = scaleDown(ip, minThreshold);
                maxThreshold = scaleDown(ip, maxThreshold);
            }
            scaleUpAndSet(ip, minThreshold, maxThreshold);
            updatePlot(imp);
            imp.updateAndDraw();
        }
        previousImageID = id;
        previousImageType = type;
        return ip;
    }
    
    void autoSetLevels(ImageProcessor ip, ImageStatistics stats) {
        if (stats==null || stats.histogram==null) {
            minThreshold = defaultMinThreshold;
            maxThreshold = defaultMaxThreshold;
            return;
        }
        int threshold = ip.getAutoThreshold(stats.histogram);
        //IJ.log(threshold+" "+stats.min+" "+stats.max+" "+stats.dmode);
        if ((stats.max-stats.dmode)>(stats.dmode-stats.min)) {
            minThreshold = threshold;
            maxThreshold = stats.max;
        } else {
            minThreshold = stats.min;
            maxThreshold = threshold;
        }
    }
    
    /** Scales threshold levels in the range 0-255 to the actual levels. */
    void scaleUpAndSet(ImageProcessor ip, double minThreshold, double maxThreshold) {
        if (!(ip instanceof ByteProcessor) && minThreshold!=ImageProcessor.NO_THRESHOLD) {
            double min = ip.getMin();
            double max = ip.getMax();
            if (max>min) {
                minThreshold = min + (minThreshold/255.0)*(max-min);
                maxThreshold = min + (maxThreshold/255.0)*(max-min);
            } else
                minThreshold = ImageProcessor.NO_THRESHOLD;
        }
        ip.setThreshold(minThreshold, maxThreshold, ip.RED_LUT);
    }

    /** Scales a threshold level to the range 0-255. */
    double scaleDown(ImageProcessor ip, double threshold) {
        if (ip instanceof ByteProcessor)
            return threshold;
        double min = ip.getMin();
        double max = ip.getMax();
        if (max>min)
            return ((threshold-min)/(max-min))*255.0;
        else
            return ImageProcessor.NO_THRESHOLD;
    }
    
    /** Scales a threshold level in the range 0-255 to the actual level. */
    double scaleUp(ImageProcessor ip, double threshold) {
        double min = ip.getMin();
        double max = ip.getMax();
//IJ.log("scaleUp: "+ threshold+"  "+min+"  "+max+"  "+(min + (threshold/255.0)*(max-min)));
        if (max>min)
            return min + (threshold/255.0)*(max-min);
        else
            return ImageProcessor.NO_THRESHOLD;
    }

    void updatePlot(ImagePlus imp) {
        plot.minThreshold = minThreshold;
        plot.maxThreshold = maxThreshold;
        plot.repaint();
    }
    
    void updateLabels(ImagePlus imp, ImageProcessor ip) {
        if (threshLabel!=null) {
            double threshold2 = scaleUp(ip, threshold);
            if (threshold==ImageProcessor.NO_THRESHOLD) {
                threshLabel.setText("Threshold: None");
            } else {
                Calibration cal = imp.getCalibration();
                if (cal.calibrated()) {
                    threshold2 = cal.getCValue((int)threshold2);
                }
                if (((int)threshold2==threshold2 || (ip instanceof ShortProcessor))) {
                    threshLabel.setText("Threshold: "+(int)threshold2);
                } else {
                    threshLabel.setText("Threshold: "+IJ.d2s(threshold2,2));
                }
            }
        }
    }
    
    /** Restore image outside non-rectangular roi. */
    void doMasking(ImagePlus imp, ImageProcessor ip) {
        ImageProcessor mask = imp.getMask();
        if (mask!=null)
            ip.reset(mask);
    }

    void reset(ImagePlus imp, ImageProcessor ip) {
        plot.setHistogram(imp);
        ip.resetThreshold();
        if (Recorder.record)
            Recorder.record("resetThreshold");
        updateLabels(imp, ip);
    }

    void refresh() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null)
            return;
        ImageProcessor ip = setup(imp);
        bitDepth = imp.getBitDepth();
        mode = choice.getSelectedIndex();
        setThresholdAlgorithm(mode);
        updateLabels(imp, ip);
    }
    
    void doSet(ImagePlus imp, ImageProcessor ip) {
        double level1 = ip.getMinThreshold();
        double level2 = ip.getMaxThreshold();
        if (level1==ImageProcessor.NO_THRESHOLD) {
            level1 = scaleUp(ip, defaultMinThreshold);
            level2 = scaleUp(ip, defaultMaxThreshold);
        }
        Calibration cal = imp.getCalibration();
        int digits = (ip instanceof FloatProcessor)||cal.calibrated()?2:0;
        level1 = cal.getCValue(level1);
        level2 = cal.getCValue(level2);
        GenericDialog gd = new GenericDialog("Set Threshold Levels");
        gd.addNumericField("Lower Threshold Level: ", level1, digits);
        gd.addNumericField("Upper Threshold Level: ", level2, digits);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        level1 = gd.getNextNumber();
        level2 = gd.getNextNumber();
        level1 = cal.getRawValue(level1);
        level2 = cal.getRawValue(level2);
        if (level2<level1)
            level2 = level1;
        double minDisplay = ip.getMin();
        double maxDisplay = ip.getMax();
        ip.resetMinAndMax();
        double minValue = ip.getMin();
        double maxValue = ip.getMax();
        if (level1<minValue) level1 = minValue;
        if (level2>maxValue) level2 = maxValue;
        boolean outOfRange = level1<minDisplay || level2>maxDisplay;
        if (outOfRange)
            plot.setHistogram(imp);
        else
            ip.setMinAndMax(minDisplay, maxDisplay);
            
        minThreshold = scaleDown(ip,level1);
        maxThreshold = scaleDown(ip,level2);
        scaleUpAndSet(ip, minThreshold, maxThreshold);
        if (Recorder.record) {
            if (imp.getBitDepth()==32)
                Recorder.record("setThreshold", ip.getMinThreshold(), ip.getMaxThreshold());
            else
                Recorder.record("setThreshold", (int)ip.getMinThreshold(), (int)ip.getMaxThreshold());
        }
        threshold = maxThreshold;
        updateLabels(imp, ip);
    }
    
    void apply(ImagePlus imp) {
        try {
            if (imp.getBitDepth()==32) {
                GenericDialog gd = new GenericDialog("NaN Backround");
                gd.addCheckbox("Set Background Pixels to NaN", backgroundToNaN);
                gd.showDialog();
                if (gd.wasCanceled()) {
                    applyThreshold(imp);
                    return;
                }
                backgroundToNaN = gd.getNextBoolean();
                if (backgroundToNaN)
                    IJ.run("NaN Background");
                else
                    applyThreshold(imp);
            } else
                applyThreshold(imp);
        } catch (Exception e)
            {/* do nothing */}
        close();
    }
    
    public void applyThreshold(ImagePlus imp) {
        if (Recorder.record && !alreadyRecorded) {
            Recorder.record("run", "MultiThresholder", modes[mode] + " apply");
            alreadyRecorded = true;
        }
        ThresholdApplier2 ta = new ThresholdApplier2();
        if (skipDialog)
            ta.run("skip");
        else
            ta.run("");
    }
    
    static final int RESET=0, HIST=1, APPLY=2, THRESHOLD=3, MIN_THRESHOLD=4, MAX_THRESHOLD=5, SET=6, REFRESH=7;

    // Separate thread that does the potentially time-consuming processing 
    public void run() {
        while (!done) {
            synchronized(this) {
                try {wait();}
                catch(InterruptedException e) {}
            }
            doUpdate();
        }
    }

    void doUpdate() {
        ImagePlus imp;
        ImageProcessor ip;
        int action;
        if (doReset) action = RESET;
        else if (doRefresh) action = REFRESH;
        else if (doApplyLut) action = APPLY;
        else if (doRethreshold) action = THRESHOLD;
        else if (doSet) action = SET;
        else return;
        doReset = false;
        doRefresh = false;
        doApplyLut = false;
        doRethreshold = false;
        doSet = false;
        imp = WindowManager.getCurrentImage();
        if (imp==null) {
            IJ.beep();
            IJ.showStatus("No image");
            return;
        }
        ip = setup(imp);
        if (ip==null) {
            imp.unlock();
            IJ.beep();
            IJ.showStatus("RGB images cannot be thresholded");
            return;
        }
        switch (action) {
            case REFRESH: refresh(); break;
            case RESET: reset(imp, ip); break;
            case APPLY: apply(imp); break;
            case SET: doSet(imp, ip); break;
        }
        updatePlot(imp);
        ip.setLutAnimation(true);
        imp.updateAndDraw();
    }

    public void windowClosing(WindowEvent e) {
        close();
    }

    /** Overrides close() in PlugInFrame. */
    public void close() {
        super.close();
        instance = null;
        done = true;
        synchronized(this) {
            notify();
        }
    }

    public void windowActivated(WindowEvent e) {
        super.windowActivated(e);
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp!=null) {
            if (!firstActivation) {
                previousImageID = 0;
                setup(imp);
            }
            firstActivation = false;
        }
    }
    
}

/** This plugin implements the Proxess/Binary/Threshold command. */
class ThresholdApplier2 implements PlugIn, Measurements {
	
	private int slice;
	private double minThreshold;
	private double maxThreshold;
	boolean autoThreshold;
	boolean skipDialog;
	ImageStack stack1;
	static boolean fill1 = true;
	static boolean fill2 = true;
	static boolean useBW = true;


	public void run(String arg) {
		skipDialog = arg.equals("skip");
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		if (imp.getStackSize()==1) {
			Undo.setup(Undo.COMPOUND_FILTER, imp);
			applyThreshold(imp);
			Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		} else {
			Undo.reset();
			applyThreshold(imp);
		}
	}

	void applyThreshold(ImagePlus imp) {
		if (!imp.lock())
			return;
		imp.killRoi();
		ImageProcessor ip = imp.getProcessor();
		double saveMinThreshold = ip.getMinThreshold();
		double saveMaxThreshold = ip.getMaxThreshold();
		double saveMin = ip.getMin();
		double saveMax = ip.getMax();
		if (ip instanceof ByteProcessor)
			{saveMin =0; saveMax = 255;}
		autoThreshold = saveMinThreshold==ImageProcessor.NO_THRESHOLD;
					
		boolean useBlackAndWhite = true;
		if (skipDialog)
			fill1 = fill2 = useBlackAndWhite = true;
		else if (!autoThreshold) {
			GenericDialog gd = new GenericDialog("Apply Lut");
			gd.addCheckbox("Thresholded pixels to foreground color", fill1);
			gd.addCheckbox("Remaining pixels to background color", fill2);
			gd.addMessage("");
			gd.addCheckbox("Black foreground, white background", useBW);
			gd.showDialog();
			if (gd.wasCanceled())
				{imp.unlock(); return;}
			fill1 = gd.getNextBoolean();
			fill2 = gd.getNextBoolean();
			useBW = useBlackAndWhite = gd.getNextBoolean();
		} else
			fill1 = fill2 = true;

		if (!(imp.getType()==ImagePlus.GRAY8))
			convertToByte(imp);
		ip = imp.getProcessor();
		minThreshold = ((saveMinThreshold-saveMin)/(saveMax-saveMin))*255.0;
		maxThreshold = ((saveMaxThreshold-saveMin)/(saveMax-saveMin))*255.0;

		int fcolor, bcolor;
		ip.resetThreshold();
		int savePixel = ip.getPixel(0,0);
		if (useBlackAndWhite)
			ip.setColor(Color.black);
		else
			ip.setColor(Toolbar.getForegroundColor());
		ip.drawPixel(0,0);
		fcolor = ip.getPixel(0,0);
		if (useBlackAndWhite)
			ip.setColor(Color.white);
		else
			ip.setColor(Toolbar.getBackgroundColor());
		ip.drawPixel(0,0);
		bcolor = ip.getPixel(0,0);
		ip.setColor(Toolbar.getForegroundColor());
		ip.putPixel(0,0,savePixel);

		int[] lut = new int[256];
		for (int i=0; i<256; i++) {
			if (i>=minThreshold && i<=maxThreshold)
				lut[i] = fill1?fcolor:(byte)i;
			else {
				lut[i] = fill2?bcolor:(byte)i;
			}
		}
		int result = IJ.setupDialog(imp, 0);
		if (result==PlugInFilter.DONE) {
			if (stack1!=null)
				imp.setStack(null, stack1);
			imp.unlock();
			return;
		}
		if (result==PlugInFilter.DOES_STACKS)
			new StackProcessor(imp.getStack(), ip).applyTable(lut);
		else
			ip.applyTable(lut);
		imp.updateAndDraw();
		imp.unlock();
	}

	void convertToByte(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		double min = ip.getMin();
		double max = ip.getMax();
		int currentSlice =  imp.getCurrentSlice();
		stack1 = imp.getStack();
		ImageStack stack2 = imp.createEmptyStack();
		int nSlices = imp.getStackSize();
		String label;
		for(int i=1; i<=nSlices; i++) {
			label = stack1.getSliceLabel(i);
			ip = stack1.getProcessor(i);
			ip.setMinAndMax(min, max);
			stack2.addSlice(label, ip.convertToByte(true));
		}
		imp.setStack(null, stack2);
		imp.setSlice(currentSlice);
		imp.setCalibration(imp.getCalibration()); //update calibration
	}
}

class ThresholdPlot2 extends Canvas implements Measurements, MouseListener {
	
	static final int WIDTH = 256, HEIGHT=48;
	double minThreshold = 85;
	double maxThreshold = 170;
	int[] histogram;
	Color[] hColors;
	int hmax;
	Image os;
	Graphics osg;
//    int mode;
	
	public ThresholdPlot2() {
		addMouseListener(this);
		setSize(WIDTH+1, HEIGHT+1);
	}
	
	/** Overrides Component getPreferredSize(). Added to work 
		around a bug in Java 1.4.1 on Mac OS X.*/
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH+1, HEIGHT+1);
	}
	
	ImageStatistics setHistogram(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		if (!(ip instanceof ByteProcessor)) {
			double min = ip.getMin();
			double max = ip.getMax();
			ip.setMinAndMax(min, max);
			ip = new ByteProcessor(ip.createImage());
		}
		ip.setRoi(imp.getRoi());
		ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MIN_MAX+MODE, null);
		int maxCount2 = 0;
		histogram = stats.histogram;
		for (int i = 0; i < stats.nBins; i++)
			if ((histogram[i] > maxCount2) && (i != stats.mode))
				maxCount2 = histogram[i];
		hmax = stats.maxCount;
		if ((hmax>(maxCount2 * 2)) && (maxCount2 != 0)) {
			hmax = (int)(maxCount2 * 1.5);
			histogram[stats.mode] = hmax;
			}
		os = null;

		ColorModel cm = ip.getColorModel();
		if (!(cm instanceof IndexColorModel))
			return null;
		IndexColorModel icm = (IndexColorModel)cm;
		int mapSize = icm.getMapSize();
		if (mapSize!=256)
			return null;
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		icm.getReds(r); 
		icm.getGreens(g); 
		icm.getBlues(b);
		hColors = new Color[256];
		for (int i=0; i<256; i++)
			hColors[i] = new Color(r[i]&255, g[i]&255, b[i]&255);
		return stats;
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (histogram!=null) {
			if (os==null && hmax>0) {
				os = createImage(WIDTH,HEIGHT);
				osg = os.getGraphics();
				osg.setColor(Color.white);
				osg.fillRect(0, 0, WIDTH, HEIGHT);
				osg.setColor(Color.gray);
				for (int i = 0; i < WIDTH; i++) {
					if (hColors!=null) osg.setColor(hColors[i]);
					osg.drawLine(i, HEIGHT, i, HEIGHT - ((int)(HEIGHT * histogram[i])/hmax));
				}
				osg.dispose();
			}
			g.drawImage(os, 0, 0, this);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		g.setColor(Color.black);
		g.drawRect(0, 0, WIDTH, HEIGHT);
		g.setColor(Color.red);
		g.drawRect((int)minThreshold, 1, (int)(maxThreshold-minThreshold), HEIGHT);
		g.drawLine((int)minThreshold, 0, (int)maxThreshold, 0);
	 }

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}

}

/**
* Automatic thresholding technique based on the entopy of the histogram.
* See: P.K. Sahoo, S. Soltani, K.C. Wong and, Y.C. Chen "A Survey of
* Thresholding Techniques", Computer Vision, Graphics, and Image
* Processing, Vol. 41, pp.233-260, 1988.
*
* @author Jarek Sacha
*/
class EntropyThreshold2 implements PlugInFilter {

int threshold = 0;
	
 public int setup(String s, ImagePlus imagePlus) {
   return PlugInFilter.DOES_8G | PlugInFilter.DOES_16 | PlugInFilter.DOES_32 | PlugInFilter.DOES_STACKS;
 }

 public void run(ImageProcessor imageProcessor) {
 }

public void run(ImageProcessor imageProcessor, int[] hist) {
	 threshold = entropySplit(hist);
//   imageProcessor.threshold(threshold);
 }
 
 public int getThreshold() {
	 return threshold;
 }

 /**
  * Calculate maximum entropy split of a histogram.
  * @param hist histogram to be thresholded.
  * @return index of the maximum entropy split.
  */
 private int entropySplit(int[] hist) {

   // Normalize histogram, that is makes the sum of all bins equal to 1.
   double sum = 0;
   for (int i = 0; i < hist.length; ++i) {
	 sum += hist[i];
   }
   if (sum == 0) {
	 // This should not normally happen, but...
	 throw new IllegalArgumentException("Empty histogram: sum of all bins is zero.");
   }

   double[] normalizedHist = new double[hist.length];
   for (int i = 0; i < hist.length; i++) {
	 normalizedHist[i] = hist[i] / sum;
   }

   //
   double[] pT = new double[hist.length];
   pT[0] = normalizedHist[0];
   for (int i = 1; i < hist.length; i++) {
	 pT[i] = pT[i - 1] + normalizedHist[i];
   }

   // Entropy for black and white parts of the histogram
   final double epsilon = Double.MIN_VALUE;
   double[] hB = new double[hist.length];
   double[] hW = new double[hist.length];
   for (int t = 0; t < hist.length; t++) {
	 // Black entropy
	 if (pT[t] > epsilon) {
	   double hhB = 0;
	   for (int i = 0; i <= t; i++) {
		 if (normalizedHist[i] > epsilon) {
		   hhB -= normalizedHist[i] / pT[t] * Math.log(normalizedHist[i] / pT[t]);
		 }
	   }
	   hB[t] = hhB;
	 } else {
	   hB[t] = 0;
	 }

	 // White  entropy
	 double pTW = 1 - pT[t];
	 if (pTW > epsilon) {
	   double hhW = 0;
	   for (int i = t + 1; i < hist.length; ++i) {
		 if (normalizedHist[i] > epsilon) {
		   hhW -= normalizedHist[i] / pTW * Math.log(normalizedHist[i] / pTW);
		 }
	   }
	   hW[t] = hhW;
	 } else {
	   hW[t] = 0;
	 }
   }

   // Find histogram index with maximum entropy
   double jMax = hB[0] + hW[0];
   int tMax = 0;
   for (int t = 1; t < hist.length; ++t) {
	 double j = hB[t] + hW[t];
	 if (j > jMax) {
	   jMax = j;
	   tMax = t;
	 }
   }

   return tMax;
 }
}

/*
 * Otsu Thresholding algorithm
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
 *                    and Maxime Dauphin
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/**
 *  This algorithm is an implementation of Otsu thresholding technique 
 *  based on the minimization of inter-class variance [otsu79].
 *
 *  @Article{otsu79,
 *    author =       "N. Otsu",
 *    title =        "A threshold selection method from gray level
 *                    histograms",
 *    journal =      "{IEEE} Trans. Systems, Man and Cybernetics",
 *    year =         "1979",
 *    volume =       "9",
 *    pages =        "62--66",
 *    month =        mar,
 *    keywords =     "threshold selection",
 *    note =         "minimize inter class variance",
 *  }
 *  
 **/
class OtsuThreshold2 implements PlugInFilter {
	private int threshold;
	final static int HMIN = 0;
	final static int HMAX = 256;
//    DecimalFormat df0 = new DecimalFormat("##0");

	public int setup(String arg, ImagePlus imp) {
	if (arg.equals("about"))
		{showAbout(); return DONE;}
	return DOES_8G+DOES_16+DOES_32+DOES_STACKS+SUPPORTS_MASKING+NO_CHANGES;
	}
	
	public void run(ImageProcessor ip) {}
	
	public void run(ImageProcessor ip, int[] stats) {
	boolean debug = false;
	int width =  ip.getWidth();
	int height = ip.getHeight();
//        intMax = (int)ip.getMax(); //16
		OtsuGrayLevelClass2.N = width*height;
		 OtsuGrayLevelClass2.probabilityHistogramDone = false;
		OtsuGrayLevelClass2 C1 = new OtsuGrayLevelClass2(ip, true);
		OtsuGrayLevelClass2 C2 = new OtsuGrayLevelClass2(ip, false);

		float fullMu = C1.getOmega()*C1.getMu()+C2.getOmega()*C2.getMu();
		//IJ.write("Full Omega : "+fullMu);
		double sigmaMax = 0;
		threshold = 0;

		/** Start  **/
		for(int i=0 ; i<255 ; i++) {
			double sigma = C1.getOmega()*(Math.pow(C1.getMu()-fullMu,2))+C2.getOmega()*(Math.pow(C2.getMu()-fullMu,2));

			if(sigma>sigmaMax) 
					{
					sigmaMax = sigma;
					threshold = C1.getThreshold();
					}
			C1.addToEnd();
			C2.removeFromBeginning();
		}
	}
	
	public int getThreshold() {
		return threshold;
	}
	
	void showAbout() {
	IJ.showMessage("About OtsuThresholding_...",
			   "This plug-in filter calculates the OtsuThresholding of an 8-bit, 16-bit, or 32-bit image.\n"
			   );
	}
}

/*
 * Mixture Modeling algorithm
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
 *                    and Maxime Dauphin
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/**
 *  This algorithm thresholds the image using a gray-level 
 *  histogram Gaussian characterisation.
 *  
 **/

class MixtureModeling2 implements PlugInFilter {
	private int threshold;
	final static int HMIN = 0;
	final static int HMAX = 256;
	private boolean runHistogram;

	public int setup(String arg, ImagePlus imp) {
	if (arg.equals("about"))
		{showAbout(); return DONE;}
	
	if(arg.equals("")) {
		runHistogram = true;
		return DOES_8G+DOES_16+DOES_32+DOES_STACKS+SUPPORTS_MASKING+NO_CHANGES;
	}
	else {
		int val1 = arg.indexOf("true");
		int val2 = arg.indexOf("false");
		//IJ.write("Ok");
		if ((val1 == -1)&&(val2 == -1)) {
		IJ.showMessage("Wrong parameters for TreeWatershed.");
		return DONE;
		}
		else {
		if ((val1 != -1)&&(val2 != -1)) {
			IJ.showMessage("Wrong parameters for TreeWatershed.");
			return DONE;
		}
		if (val1 != -1)
			runHistogram = true;
		else
			runHistogram = false;
		}
	}
	
	return DOES_8G+DOES_16+DOES_32+DOES_STACKS+SUPPORTS_MASKING+NO_CHANGES;
	}
 
	public void run(ImageProcessor ip) {}
	
	public void run(ImageProcessor ip, int[] stats) {
	boolean debug = false;

	GrayLevelClassMixtureModeling2 classes = new GrayLevelClassMixtureModeling2(ip);	
	double sigmaMax = 0;
	threshold = 0;

	float error = 0;
	float errorMin = 9999999;
	float mu1 = 0, mu2 = 0, variance1 = 0, variance2 = 0;

	/** Start  **/
	while(classes.addToIndex()) {
		error = calculateError(classes);
		
		//IJ.write("Error "+i+" : "+error+", threshold : "+C1.getThreshold());
		
		if(error<errorMin) {
		errorMin = error;
		threshold = classes.getThreshold();
		mu1 = classes.getMu1();
		variance1 = classes.getVariance1();
		mu2 = classes.getMu2();
		variance2 = classes.getVariance2();
		//IJ.write(""+C1+C2+"\n");
		}
	}
	classes.setIndex(threshold);
	
	if(runHistogram)
		affHist(classes);

//	IJ.write("Mu1 : "+mu1+", variance1 : "+variance1);
//	IJ.write("Mu2 : "+mu2+", variance2 : "+variance2);
//	IJ.write("ErrorMin : "+errorMin);
//	IJ.write("Diff Mu : "+(mu2-mu1));
//	IJ.write("Direct threshold : "+threshold);
//	IJ.write("Real threshold : "+findThreshold((int)mu1, (int)mu2, classes));
	}

	private float findThreshold(int mu1, int mu2, GrayLevelClassMixtureModeling2 classes) {
	float min = 9999999;
	int threshold = 0;

	for(int i=mu1; i<mu2; i++) {
		float val = (float)Math.pow(classes.differenceGamma(i),2); 
		if(min>val) {
		min = val;
		threshold = i;
		}
	}
	return threshold;
	}
	
	public int getThreshold() {
		return threshold;
	}

	private float calculateError(GrayLevelClassMixtureModeling2 classes) {
	float error = 0;

	for(int i=0; i<=GrayLevelClassMixtureModeling2.MAX; i++) {
		error += Math.pow(classes.gamma(i)-GrayLevelClassMixtureModeling2.getHistogram(i),2);
	}

	return error/(GrayLevelClassMixtureModeling2.MAX+1);
	}
		
	void showAbout() {
	IJ.showMessage("About MixtureModeling_...",
			   "This plug-in filter calculates the mixtureModeling of 8-bit, 16-bit, or 32-bit images.\n"
			   );
	}  


	//************************************************************
	//* Affiche un histogramme avec les classes
	//************************************************************
	public void affHist(GrayLevelClassMixtureModeling2 classes)
	{
	int max = maxi();
	ImagePlus imp = NewImage.createRGBImage ("Histogram", 256, 100, 1, NewImage.FILL_WHITE);
	ImageProcessor nip = imp.getProcessor();
	int pixel=0; //pixel courant

	int red   = 255;
	int green = 0;
	int blue  = 0;
	int gamma1 = ((red & 0xff) << 16) + ((green & 0xff) << 8) + (blue & 0xff);
	red   = 0;
	green = 0;
	blue  = 255;
	int gamma2 = ((red & 0xff) << 16) + ((green & 0xff) << 8) + (blue & 0xff);
	red = 0; green = 0; blue = 0;
	int hist = ((red & 0xff) << 16) + ((green & 0xff) << 8) + (blue & 0xff);

	for (int x = 0; x < 256 ; x++)
		{
		float approx1 = classes.gamma1(x);
		double app1 = 100.0 - ((double)(approx1) /(double)(max)) * 100.0;
		float approx2 = classes.gamma2(x);
		double app2 = 100.0 - ((double)(approx2) /(double)(max)) * 100.0;
		
		double t = 100.0 - ((double)(GrayLevelClassMixtureModeling2.histogram[x]) /(double)(max)) * 100.0;
		for (int y = 100; y > (int)(t); y--)
			nip.putPixel(x,y,hist);
		
		nip.putPixel(x,(int)app1,gamma1);
		nip.putPixel(x,(int)app2,gamma2);
		}

	imp.show();
	}//fin affHist

	//************************************************************
	//* Recherche d'un maximun dans un tableau de int
	//************************************************************
	private int maxi()
	{
	int max = 0;
	for (int i=0;i<GrayLevelClassMixtureModeling2.histogram.length;i++)
		if (GrayLevelClassMixtureModeling2.histogram[i]>max) max = GrayLevelClassMixtureModeling2.histogram[i];
	return max;
	}//fin maxi
}


/** 
 *  This class implements a class of pixels with basic statistical functions.
 **/

class OtsuGrayLevelClass2 {
	private static float[] probabilityHistogram;
	public static boolean probabilityHistogramDone;
	public static int N;

	private int index;
	private float omega;
	private float mu;

	public OtsuGrayLevelClass2(ImageProcessor ip, boolean first) {
	int[] histogram;
		if(!probabilityHistogramDone) {
			ImageStatistics stats = ImageStatistics.getStatistics(ip, 0, null);
			histogram = stats.histogram;
		probabilityHistogram = new float[256];
		
		for(int i=0; i<256 ; i++) {
		probabilityHistogram[i] = ((float) histogram[i])/((float) N);
		//IJ.write(" "+probabilityHistogram[i]);
		}
		probabilityHistogramDone = true;
	}

	if(first) {
		index = 1;
		omega = probabilityHistogram[index-1];
		if(omega == 0)
		mu = 0;
		else
		mu =  1*probabilityHistogram[index-1]/omega;
	}
	else {
		index = 2;
		omega = 0;
		mu = 0;
		for(int i=index; i<256 ; i++) {
		omega +=  probabilityHistogram[i-1];
		mu +=  probabilityHistogram[i-1]*i;
		}
		if(omega == 0)
		mu = 0;
		else
		mu /= omega;
	}
	}
	
	public void removeFromBeginning() {
	index++;
	mu = 0;
	omega = 0;

	for(int i=index; i<256 ; i++) {
		omega +=  probabilityHistogram[i-1];
		mu +=  i*probabilityHistogram[i-1];//i*
	}
	if(omega == 0)
		mu = 0;
	else
		mu /= omega;
	/*mu *= omega;
	  mu -= probabilityHistogram[index-2];//(index-1)*
	  omega -= probabilityHistogram[index-2];
	  if(omega == 0)
	  mu = 0;
	  else
	  mu /= omega;*/
	}

	public void addToEnd() {
	index++;
	mu = 0;
	omega = 0;
	for(int i=1; i<index ; i++) {
		omega +=  probabilityHistogram[i-1];
		mu +=  i*probabilityHistogram[i-1];
	}
	if(omega == 0)
		mu = 0;
	else
		mu /= omega;
	/*mu *= omega;
	  omega += probabilityHistogram[index-1];
	  mu += probabilityHistogram[index-1];//index*
	  if(omega == 0)
	  mu = 0;
	  else
	  mu /=omega;
	*/
	}

	public String toString() {
	StringBuffer ret = new StringBuffer();
	
	ret.append("Index : "+index+"\n");
	ret.append("Mu : "+mu+"\n");
	ret.append("Omega : "+omega+"\n");
	/*for(int i=0; i<10; i++) {
		ret.append( "\n" );
		}*/
	
	return ret.toString();
	}

	public float getMu() {
	return mu;
	}

	public float getOmega() {
	return omega;
	}

	public int getThreshold() {
	return index;
	}
}
/*
 * Mixture Modeling algorithm
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)

 *                    and Maxime Dauphin
 *
 * This plugin is free software; you can redistribute it and/or modify

 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/** This class implements a GrayLevelClassMixtureModeling2.
 **/

class GrayLevelClassMixtureModeling2 {
  public static int[] histogram;
	/** The index must vary between 1 and 253
	C1 : [0;index]
	C2 : [index+1; 255]
	**/
	private int index;
	private float mu1, mu2;
	private float sigma2_1, sigma2_2;
	private float mult1, mult2;
	private float twoVariance1, twoVariance2;
	private float max1, max2;
	private int cardinal1, cardinal2;
	private int cardinal;

	private int INDEX_MIN = 1;
	private int INDEX_MAX = 253;
	private int MIN = 0;
	public static final int MAX = 255;
	
	public GrayLevelClassMixtureModeling2(ImageProcessor img) {
	cardinal = img.getWidth()*img.getHeight();
	ImageStatistics stats = ImageStatistics.getStatistics(img, 0, null);
	histogram = stats.histogram;
	index = INDEX_MIN-1;
	//setValues();
	}

	public boolean addToIndex() {
	index++;

	if(!(index<=INDEX_MAX))
		return false;	

	setValues();
	return true;
	}

	private float calculateMax(int index) {
	float sum = histogram[index];
	float num = 1;
	if(index-1>=0) {
		sum += histogram[index-1];
		num++;
	}
	if(index+1<MAX) {
		sum += histogram[index+1];
		num++;
	}
	return sum/num;
	}

	public String toString() {
	StringBuffer ret = new StringBuffer();
	
	ret.append("Index : "+index+"\n");
	ret.append("Max1 : "+max1+" ");
	ret.append("Max2 : "+max2+"\n");
	ret.append("Mu1 : "+mu1+" ");
	ret.append("Mu2 : "+mu2+"\n");
	ret.append("Cardinal1 : "+cardinal1+" ");
	ret.append("Cardinal2 : "+cardinal2+"\n");
	ret.append("Variance1 : "+sigma2_1+" ");
	ret.append("Variance2 : "+sigma2_2+"\n");
	
	return ret.toString();
	}

	public float getCardinal() {
	return cardinal;
	}

	public float getMu1() {
	return mu1;
	}

	public float getMu2() {
	return mu2;
	}

	public float getMax1() {
	return max1;
	}

	public float getMax2() {
	return max2;
	}

	public float getVariance1() {
	return sigma2_1;
	}

	public float getVariance2() {
	return sigma2_2;
	}

	public float getCardinal1() {
	return cardinal1;
	}

	public float getCardinal2() {
	return cardinal2;
	}

	public int getThreshold() {
	return index;
	}

	public void setIndex(int index) {
	this.index = index;
	setValues();
	}

	private void setValues() {	
	mu1 = 0; mu2 = 0;
	sigma2_1 = 0; sigma2_2 = 0;
	max1 = 0; max2 = 0;
	cardinal1 = 0; cardinal2 = 0;

	for(int i=MIN; i<=index ; i++) {
		cardinal1 +=  histogram[i];
		mu1 +=  i*histogram[i];
	}
	
	for(int i=index+1; i<=MAX ; i++) {
		cardinal2 +=  histogram[i];
		mu2 +=  i*histogram[i];
	}
	
	if(cardinal1 == 0) {
		mu1 = 0;
		sigma2_1 = 0;

	}
	else 
		mu1 /= (float)cardinal1; 

	if(cardinal2 == 0) {
		mu2 = 0;
		sigma2_2 = 0;
	}
	else 
		mu2 /= (float)cardinal2; 

	if( mu1 != 0 ) {
		for(int i=MIN; i<=index ; i++) 
		sigma2_1 += histogram[i]*Math.pow(i-mu1,2);
		
		sigma2_1 /= (float)cardinal1;
	
		max1 = calculateMax((int) mu1);

		mult1 = (float) max1;
		twoVariance1 = 2*sigma2_1; 
	}
	if( mu2 != 0 ) {
		for(int i=index+1; i<=MAX ; i++) 
		sigma2_2 += histogram[i]*Math.pow(i-mu2,2);
		
		sigma2_2 /= (float)cardinal2;
	
		max2 = calculateMax((int) mu2);

		mult2 = (float) max2;
		twoVariance2 = 2*sigma2_2; 
	}
	}

	public final float gamma1(int i) {
	if(sigma2_1 == 0) 
		return 0;
	return (float)(mult1*Math.exp(-(Math.pow((float)i-mu1,2))/twoVariance1));

	}

	public final float gamma2(int i) {
	if(sigma2_2 == 0) 
		return 0;
	return (float)(mult2*Math.exp(-(Math.pow((float)i-mu2,2))/twoVariance2));

	}

	public float gamma(int i) {
	return gamma1(i)+gamma2(i);
	}

	public float differenceGamma(int i) {
	return gamma1(i)-gamma2(i);
	}

	public static int getHistogram(int i) {
	return histogram[i];
	}
}
