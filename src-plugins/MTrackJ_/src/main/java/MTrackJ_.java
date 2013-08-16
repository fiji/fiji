// *************************************************************************************************
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;
import imagescience.array.DoubleArray;
import imagescience.color.Palette;
import imagescience.color.Wave2Color;
import imagescience.utility.FMath;
import imagescience.utility.Formatter;
import imagescience.utility.I5DResource;
import imagescience.utility.ImageScience;
import imagescience.utility.MouseCursor;
import imagescience.utility.Progressor;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.GeneralPath;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

// *************************************************************************************************
public class MTrackJ_ implements PlugIn {
	
	// Minimum version numbers:
	private static final String MIN_VERSION_IJ = "1.46a";
	private static final String MIN_VERSION_IS = "2.4.1";
	private static final String MIN_VERSION_JRE = "1.6.0";
	
	// Check version numbers and arguments and launch program:
	public void run(final String args) {
		
		// Check version numbers:
		if (System.getProperty("java.version").compareTo(MIN_VERSION_JRE) < 0) {
			error("This plugin requires Java version "+MIN_VERSION_JRE+" or higher");
			return;
		}
		
		if (IJ.getVersion().compareTo(MIN_VERSION_IJ) < 0) {
			error("This plugin requires ImageJ version "+MIN_VERSION_IJ+" or higher");
			return;
		}
		
		try { // This also works to check if ImageScience is installed
			if (ImageScience.version().compareTo(MIN_VERSION_IS) < 0)
			throw new IllegalStateException();
		} catch (Throwable e) {
			error("This plugin requires ImageScience version "+MIN_VERSION_IS+" or higher");
			return;
		}
		
		// Check argument(s):
		int iid = 0; String mdf = null;
		if (args != null && args.length() > 0) {
			final StringTokenizer argstok = new StringTokenizer(args,",");
			final int nrargs = argstok.countTokens();
			if (nrargs == 1) {
				final String tok = argstok.nextToken();
				try { iid = Integer.parseInt(tok); }
				catch (Throwable e) { mdf = tok; }
			} else if (nrargs == 2) {
				try { iid = Integer.parseInt(argstok.nextToken()); }
				catch (Throwable e) { iid = 1; }
				mdf = argstok.nextToken();
			} else {
				error("Invalid number of arguments passed to plugin");
				return;
			}
		}
		if (iid == 0 && WindowManager.getCurrentImage() == null) {
			error("There are no images open");
			return;
		} else if ((iid > 0) || (iid < 0 && WindowManager.getImage(iid) == null)) {
			error("Invalid image ID passed to plugin");
			return;
		}
		if (mdf != null) {
			try {
				final FileReader fr = new FileReader(mdf);
				fr.close();
			} catch (Throwable e) {
				error("Invalid file name passed to plugin");
				return;
			}
		}
		
		// Check for other instance:
		final ImageWindow window = (iid == 0) ? WindowManager.getCurrentImage().getWindow() : WindowManager.getImage(iid).getWindow();
		final WindowListener[] wls = (WindowListener[])window.getListeners(WindowListener.class);
		for (int i=0; i<wls.length; ++i)
			if (wls[i] instanceof MTrackJ) {
				error("Another instance of "+MTrackJ.name()+" is already attached to the image");
				return;
			}
		
		// Launch program:
		mtrackj = new MTrackJ(iid,mdf);
	}
	
	private MTrackJ mtrackj = null;
	
	public void quit() {
		
		if (mtrackj != null) mtrackj.dialog().quit();
	}
	
	static void error(final String message) {
		
		if (IJ.getInstance() != null) new MTJMessage(IJ.getInstance(),MTrackJ.MTJ_ERROR,message+".");
		else IJ.showMessage(MTrackJ.MTJ_ERROR,message+".");
	}
	
}

// *************************************************************************************************
final class MTrackJ implements WindowListener {
	
	final static String NAME = "MTrackJ";
	final static String VERSION = "1.5.0";
	
	private final static String COPYRIGHT = NAME+" "+VERSION+" (C) Erik Meijering";
	
	private boolean lastdoublebuffering = false;
	private int lastimagejtool;
	
	private final static int SINGLEIMAGE=0, IMAGESTACK=1, HYPERSTACK=2, IMAGE5D=3;
	private int imagetype = IMAGESTACK;
	
	private ImagePlus image = null;
	private ImageWindow window = null;
	private ImageCanvas canvas = null;
	
	private TextWindow pointswindow = null;
	private TextWindow trackswindow = null;
	private TextWindow clusterswindow = null;
	private TextWindow assemblywindow = null;
	private TextWindow logwindow = null;
	private TextPanel logpanel = null;
	
	private MTJDialog dialog = null;
	private MTJHandler handler = null;
	private MTJSettings settings = null;
	private MTJCatcher catcher = null;
	
	private final Formatter fm = new Formatter();
	
	MTrackJ(final int iid, final String mdf) {
		
		// Activate uncaught exception catcher:
		catcher = new MTJCatcher();
		try { Thread.currentThread().setUncaughtExceptionHandler(catcher); }
		catch (final Throwable e) { }
		
		// Restore settings (also determines whether to show log messages):
		settings = new MTJSettings(this);
		settings.restore(MTJSettings.ALL);
		
		// Show version numbers:
		log("Running on "+System.getProperty("os.name")+" version "+System.getProperty("os.version"));
		log("Running on Java version "+System.getProperty("java.version"));
		log("Running on ImageJ version "+IJ.getVersion());
		log("Running on ImageScience version "+ImageScience.version());
		
		// Initialize fields:
		if (iid == 0) {
			log("Getting current image from WindowManager...");
			image = WindowManager.getCurrentImage();
			logok();
		} else {
			log("Getting image with ID = "+iid+" from WindowManager...");
			image = WindowManager.getImage(iid);
			logok();
		}
		
		boolean image5dinstalled = false;
		final String typeprelude = "Image \""+image.getTitle()+"\" is ";
		try { Class.forName("i5d.Image5D"); image5dinstalled = true; } catch (Throwable e) { }
		if (image5dinstalled && I5DResource.instance(image)) {
			log(typeprelude+"an Image5D object");
			imagetype = IMAGE5D;
		} else if (image.isHyperStack()) {
			log(typeprelude+"a hyperstack");
			imagetype = HYPERSTACK;
		} else if (image.getImageStackSize() > 1) {
			log(typeprelude+"an image stack");
			imagetype = IMAGESTACK;
		} else {
			log(typeprelude+"a single image");
			imagetype = SINGLEIMAGE;
		}
		
		log("Preparing "+NAME+" for \""+image.getTitle()+"\"");
		window = image.getWindow();
		canvas = window.getCanvas();
		
		log("Activating double buffering...");
		lastdoublebuffering = Prefs.doubleBuffer;
		Prefs.doubleBuffer = true;
		logok();
		
		log("Detaching ImageJ listeners...");
		window.removeKeyListener(IJ.getInstance());
		window.removeMouseWheelListener(window);
		canvas.removeKeyListener(IJ.getInstance());
		canvas.removeMouseMotionListener(canvas);
		canvas.removeMouseListener(canvas);
		logok();
		
		log("Attaching "+NAME+" listeners...");
		window.addWindowListener(this);
		handler = new MTJHandler(this);
		window.addKeyListener(handler);
		window.addMouseWheelListener(handler);
		canvas.addKeyListener(handler);
		canvas.addMouseMotionListener(handler);
		canvas.addMouseListener(handler);
		logok();
		
		log("Launching "+NAME+" dialog...");
		dialog = new MTJDialog(this);
		logok();
		
		fm.inf(MTJMeasurer.INF);
		fm.nan(MTJMeasurer.NA);
		fm.decs(3);
		
		lastimagejtool = Toolbar.getToolId();
		handler.mode(MTJHandler.NONE);
		log("Initialization completed");
		copyright();
		
		if (mdf != null) {
			final MTJReader reader = new MTJReader(this,mdf,MTJReader.LOAD);
			reader.start();
		}
	}
	
	void quit() {
		
		if (handler.changed() && settings.savechanges)
			if (dialog.saveconfirm()) {
				boolean canceled = false;
				final MTJAssembly assembly = handler.assembly();
				if (assembly.file() == null) {
					final String savepath = dialog.getsavepath();
					if (savepath == null) canceled = true;
					else assembly.file(savepath);
				}
				if (!canceled) {
					settings.showlog = false; // To avoid reopening of log window
					final MTJWriter writer = new MTJWriter(this,assembly.file(),MTJWriter.SAVE);
					writer.start();
				}
			}
		
		window.removeWindowListener(this);
		window.removeKeyListener(handler);
		window.removeMouseWheelListener(handler);
		canvas.removeKeyListener(handler);
		canvas.removeMouseMotionListener(handler);
		canvas.removeMouseListener(handler);
		
		window.addKeyListener(IJ.getInstance());
		window.addMouseWheelListener(window);
		canvas.addKeyListener(IJ.getInstance());
		canvas.addMouseMotionListener(canvas);
		canvas.addMouseListener(canvas);
		
		dialog.close();
		closelogwindow();
		closepointswindow();
		closetrackswindow();
		closeclusterswindow();
		closeassemblywindow();
		image.killRoi();
		Prefs.doubleBuffer = lastdoublebuffering;
		IJ.setTool(lastimagejtool);
		status(NAME+" closed");
	}
	
	static String name() { return NAME; }
	
	static String version() { return VERSION; }
	
	void copyright() { IJ.showStatus(COPYRIGHT); }
	
	boolean doslices() {
		
		boolean val = false;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = (image.getNSlices() > 1); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	boolean doframes() {
		
		boolean val = false;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = (image.getNFrames() > 1); break;
			case IMAGESTACK: val = (image.getImageStackSize() > 1); break;
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	boolean dochannels() {
		
		boolean val = false;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = (image.getNChannels() > 1); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int nrslices() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = image.getNSlices(); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int nrframes() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = image.getNFrames(); break;
			case IMAGESTACK: val = image.getImageStackSize(); break;
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int nrchannels() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D:
			case HYPERSTACK: val = image.getNChannels(); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int getwidth() { return image.getWidth(); }
	
	int getheight() { return image.getHeight(); }
	
	int getslice() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D: val = I5DResource.position(image,3) + 1; break;
			case HYPERSTACK: val = image.getSlice(); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int getframe() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D: val = I5DResource.position(image,4) + 1; break;
			case HYPERSTACK: val = image.getFrame(); break;
			case IMAGESTACK: val = image.getCurrentSlice(); break;
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	int getchannel() {
		
		int val = 1;
		switch (imagetype) {
			case IMAGE5D: val = I5DResource.position(image,2) + 1; break;
			case HYPERSTACK: val = image.getChannel(); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
		return val;
	}
	
	void setslice(final int z) {
		
		switch (imagetype) {
			case IMAGE5D: I5DResource.position(image,3,z-1); break;
			case HYPERSTACK: image.setPosition(image.getChannel(),z,image.getFrame()); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
	}
	
	void setframe(final int t) {
		
		switch (imagetype) {
			case IMAGE5D: I5DResource.position(image,4,t-1); break;
			case HYPERSTACK: image.setPosition(image.getChannel(),image.getSlice(),t); break;
			case IMAGESTACK: image.setSlice(t); break;
			case SINGLEIMAGE: break;
		}
	}
	
	void setchannel(final int c) {
		
		switch (imagetype) {
			case IMAGE5D: I5DResource.position(image,2,c-1); break;
			case HYPERSTACK: image.setPosition(c,image.getSlice(),image.getFrame()); break;
			case IMAGESTACK:
			case SINGLEIMAGE: break;
		}
	}
	
	void setsliders(final int c, final int z, final int t) {
		
		switch (imagetype) {
			case IMAGE5D: I5DResource.position(image,0,0,c-1,z-1,t-1); break;
			case HYPERSTACK: image.setPosition(c,z,t); break;
			case IMAGESTACK: image.setSlice(t); break;
			case SINGLEIMAGE: break;
		}
	}
	
	ImagePlus image() { return image; }
	
	ColorModel colormodel(final int c) {
		
		if (imagetype == IMAGE5D) return I5DResource.processor(image,c).getColorModel();
		else if (image.isComposite()) return ((CompositeImage)image).getChannelLut(c);
		else return image.getProcessor().getColorModel();
	}
	
	double minshown(final int c) {
		
		if (imagetype == IMAGE5D) return I5DResource.processor(image,c).getMin();
		else if (image.isComposite()) return ((CompositeImage)image).getChannelLut(c).min;
		else return image.getProcessor().getMin();
	}
	
	double maxshown(final int c) {
		
		if (imagetype == IMAGE5D) return I5DResource.processor(image,c).getMax();
		else if (image.isComposite()) return ((CompositeImage)image).getChannelLut(c).max;
		else return image.getProcessor().getMax();
	}
	
	Calibration density(final int c) {
		
		return (imagetype == IMAGE5D) ? I5DResource.density(image,c) : image.getCalibration().copy();
	}
	
	ImageWindow window() { return window; }
	
	ImageCanvas canvas() { return canvas; }
	
	TextWindow logwindow() { return logwindow; }
	
	TextWindow pointswindow() { return pointswindow; }
	
	TextWindow trackswindow() { return trackswindow; }
	
	TextWindow clusterswindow() { return clusterswindow; }
	
	TextWindow assemblywindow() { return assemblywindow; }
	
	MTJHandler handler() { return handler; }
	
	MTJDialog dialog() { return dialog; }
	
	MTJSettings settings() { return settings; }
	
	MTJCatcher catcher() { return catcher; }
	
	void settings(final MTJSettings settings) { this.settings = settings; }
	
	static final String MTJ_ERROR = NAME+": Error";
	
	void error(final String message) {
		
		log(message);
		if (image != null) new MTJMessage((Frame)image.getWindow(),MTJ_ERROR,message+".");
		else if (IJ.getInstance() != null) new MTJMessage(IJ.getInstance(),MTJ_ERROR,message+".");
		else IJ.showMessage(MTJ_ERROR,message+".");
	}
	
	static final String MTJ_NOTE = NAME+": Note";
	
	void note(final String message) {
		
		if (image != null) new MTJMessage((Frame)image.getWindow(),MTJ_NOTE,message+".");
		else if (IJ.getInstance() != null) new MTJMessage(IJ.getInstance(),MTJ_NOTE,message+".");
		else IJ.showMessage(MTJ_NOTE,message+".");
	}
	
	void status(final String message) { IJ.showStatus(message); }
	
	boolean locked() {
		
		if (handler.assembly().locked()) {
			note("Please wait until the current operation has finished");
			return true;
		}
		
		return false;
	}
	
	void log(final String message) {
		
		if (settings.showlog) {
			checklogwindow();
			logpanel.append(message);
		}
	}
	
	void logok() {
		
		if (settings.showlog) {
			checklogwindow();
			final int lastindex = logpanel.getLineCount() - 1;
			final String lastline = logpanel.getLine(lastindex);
			if (lastline.endsWith("...")) logpanel.setLine(lastindex,lastline+"OK");
			else logpanel.append("OK");
		}
	}
	
	TextWindow checklogwindow() {
		
		if (logwindow == null || !logwindow.isShowing()) {
			// Trick to make TextWindow use desired location and size:
			final String orgloc = Prefs.get("results.loc","0,0");
			final double orgwidth = Prefs.get("results.width",0);
			final double orgheight = Prefs.get("results.height",0);
			final Dimension scrdim = IJ.getScreenSize();
			final int logwidth = 500, logheight = 400;
			final String logloc = (scrdim.width-logwidth-10)+","+(scrdim.height-logheight-40);
			Prefs.set("results.loc",logloc);
			Prefs.set("results.width",logwidth);
			Prefs.set("results.height",logheight);
			// Set up log window:
			final String startupmsg = NAME+" version "+VERSION+"\nCopyright (C) Erik Meijering";
			logwindow = new TextWindow("Results",startupmsg,logwidth,logheight);
			logwindow.setTitle(NAME+": Log");
			logpanel = logwindow.getTextPanel();
			// Restore original preferences:
			Prefs.set("results.loc",orgloc);
			Prefs.set("results.width",orgwidth);
			Prefs.set("results.height",orgheight);
		}
		
		return logwindow;
	}
	
	void closelogwindow() { if (logwindow != null) logwindow.setVisible(false); }
	
	TextWindow openpointswindow() {
		
		if (pointswindow == null || !pointswindow.isShowing()) {
			pointswindow = new TextWindow(NAME+": Points","","",800,400);
			final Point loc = pointswindow.getLocation();
			loc.x += 60; loc.y += 60;
			pointswindow.setLocation(loc.x,loc.y);
		}
		pointswindow.toFront();
		
		return pointswindow;
	}
	
	TextWindow opentrackswindow() {
		
		if (trackswindow == null || !trackswindow.isShowing()) {
			trackswindow = new TextWindow(NAME+": Tracks","","",800,400);
			final Point loc = trackswindow.getLocation();
			loc.x += 40; loc.y += 40;
			trackswindow.setLocation(loc.x,loc.y);
		}
		trackswindow.toFront();
		
		return trackswindow;
	}
	
	TextWindow openclusterswindow() {
		
		if (clusterswindow == null || !clusterswindow.isShowing()) {
			clusterswindow = new TextWindow(NAME+": Clusters","","",800,400);
			final Point loc = clusterswindow.getLocation();
			loc.x += 20; loc.y += 20;
			clusterswindow.setLocation(loc.x,loc.y);
		}
		clusterswindow.toFront();
		
		return clusterswindow;
	}
	
	TextWindow openassemblywindow() {
		
		if (assemblywindow == null || !assemblywindow.isShowing())
			assemblywindow = new TextWindow(NAME+": Assembly","","",800,400);
		assemblywindow.toFront();
		
		return assemblywindow;
	}
	
	void closepointswindow() { if (pointswindow != null) pointswindow.setVisible(false); }
	
	void closetrackswindow() { if (trackswindow != null) trackswindow.setVisible(false); }
	
	void closeclusterswindow() { if (clusterswindow != null) clusterswindow.setVisible(false); }
	
	void closeassemblywindow() { if (assemblywindow != null) assemblywindow.setVisible(false); }
	
	void decimals(final int decs) { fm.decs(decs); }
	
	String d2s(final double d) { return fm.d2s(d); }
	
	double value(final ImageProcessor ip, double x, double y) {
		
		double val = 0;
		final int width = ip.getWidth();
		final int height = ip.getHeight();
		if (x < 0) x = 0; else if (x >= width-1) x = width - 1.0001;
		if (y < 0) y = 0; else if (y >= height-1) y = height - 1.0001;
		
		if (Prefs.interpolateScaledImages) {
			final int lx = (int)x, ly = (int)y;
			final double dx = x - lx, dy = y - ly;
			double ul=0, ur=0, ll=0, lr=0;
			if ((ip instanceof ByteProcessor) || (ip instanceof ShortProcessor)) {
				final float[] ctable = ip.getCalibrationTable();
				if (ctable != null) {
					ul = ctable[ip.get(lx,ly)];
					ur = ctable[ip.get(lx+1,ly)];
					ll = ctable[ip.get(lx,ly+1)];
					lr = ctable[ip.get(lx+1,ly+1)];
				} else {
					ul = ip.get(lx,ly);
					ur = ip.get(lx+1,ly);
					ll = ip.get(lx,ly+1);
					lr = ip.get(lx+1,ly+1);
				}
			} else if (ip instanceof ColorProcessor) {
				final int ulc = ip.get(lx,ly);
				final int urc = ip.get(lx+1,ly);
				final int llc = ip.get(lx,ly+1);
				final int lrc = ip.get(lx+1,ly+1);
				ul = ((ulc&0xFF0000)>>16)*MTJSettings.RED_WEIGHT + ((ulc&0xFF00)>>8)*MTJSettings.GREEN_WEIGHT + (ulc&0xFF)*MTJSettings.BLUE_WEIGHT;
				ur = ((urc&0xFF0000)>>16)*MTJSettings.RED_WEIGHT + ((urc&0xFF00)>>8)*MTJSettings.GREEN_WEIGHT + (urc&0xFF)*MTJSettings.BLUE_WEIGHT;
				ll = ((llc&0xFF0000)>>16)*MTJSettings.RED_WEIGHT + ((llc&0xFF00)>>8)*MTJSettings.GREEN_WEIGHT + (llc&0xFF)*MTJSettings.BLUE_WEIGHT;
				lr = ((lrc&0xFF0000)>>16)*MTJSettings.RED_WEIGHT + ((lrc&0xFF00)>>8)*MTJSettings.GREEN_WEIGHT + (lrc&0xFF)*MTJSettings.BLUE_WEIGHT;
			} else if (ip instanceof FloatProcessor) {
				ul = ip.getf(lx,ly);
				ur = ip.getf(lx+1,ly);
				ll = ip.getf(lx,ly+1);
				lr = ip.getf(lx+1,ly+1);
			}
			final double uv = ul + (ur - ul)*dx;
			final double lv = ll + (lr - ll)*dx;
			val = uv + (lv - uv)*dy;
			
		} else {
			final int rx = FMath.round(x);
			final int ry = FMath.round(y);
			if ((ip instanceof ByteProcessor) || (ip instanceof ShortProcessor)) {
				final float[] ctable = ip.getCalibrationTable();
				if (ctable != null) val = ctable[ip.get(rx,ry)];
				else val = ip.get(rx,ry);
			} else if (ip instanceof ColorProcessor) {
				final int c = ip.get(rx,ry);
				val = ((c&0xFF0000)>>16)*MTJSettings.RED_WEIGHT + ((c&0xFF00)>>8)*MTJSettings.GREEN_WEIGHT + (c&0xFF)*MTJSettings.BLUE_WEIGHT;
			} else if (ip instanceof FloatProcessor) {
				val = ip.getf(rx,ry);
			}
		}
		
		return val;
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) { }
	
	public void windowClosing(final WindowEvent e) { try {
		
		quit();
		
	} catch (Throwable x) { catcher.uncaughtException(Thread.currentThread(),x); } }
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}

// *************************************************************************************************
final class MTJSettings {
	
	private final MTrackJ mtrackj;
	
	// Constants:
	final static double NEARBY_RANGE=1.0;
	final static double RED_WEIGHT=0.3, GREEN_WEIGHT=0.6, BLUE_WEIGHT=0.1;
	final static int MAX_INTENSITY=0, MIN_INTENSITY=1, BRIGHT_CENTROID=2, DARK_CENTROID=3;
	final static int LOADING=1, IMPORTING=2, SAVING=4, TRACKING=8, DISPLAYING=16, PROGRAM=32, ALL=63;
	final static int ENTIRE_TRACK=0, UP_TO_CURRENT=1, FROM_CURRENT=2;
	final static int WHITE=0, GRAY=1, BLACK=2, RED=3, ORANGE=4, YELLOW=5, GREEN=6, CYAN=7, BLUE=8, MAGENTA=9, PINK=10;
	final static int BACKGROUND_IMAGE=0, BACKGROUND_WHITE=1, BACKGROUND_GRAY=2, BACKGROUND_BLACK=3;
	final static int MONOCHROME=0, PERCLUSTER=1, PERTRACK=2;
	final static int PLAIN=0, BOLD=1, ITALIC=2, ITALIC_BOLD=3;
	final static int CIRCLE=0, SQUARE=1, TRIANGLE=2, CROSS=3;
	final static int OPEN=0, CLOSED=1;
	final static int NOCODING=0, DEPTH_SIZE=1, DEPTH_COLOR=2, DEPTH_BOTH=3, DISTANCE_SIZE=4, DISTANCE_COLOR=5, DISTANCE_BOTH=6;
	final static int TAIL=0, HEAD=1, MIDDLE=2;
	
	// Loading settings:
	double xloadoffset = 0;
	double yloadoffset = 0;
	double zloadoffset = 0;
	double tloadoffset = 0;
	double cloadoffset = 0;
	
	// Importing settings:
	double ximportoffset = 0;
	double yimportoffset = 0;
	double zimportoffset = 0;
	double timportoffset = 0;
	double cimportoffset = 0;
	
	// Saving settings:
	double xsaveoffset = 0;
	double ysaveoffset = 0;
	double zsaveoffset = 0;
	double tsaveoffset = 0;
	double csaveoffset = 0;
	
	// Tracking settings:
	boolean steptime = true;
	int timestep = 1;
	boolean endfinish = false;
	boolean resettime = false;
	boolean snapping = false;
	int snapfeature = MAX_INTENSITY;
	int snaprange = 25;
	
	// Displaying settings:
	boolean showreference = true;
	boolean showactivetrack = true;
	boolean showfinishedtracks = true;
	int visibility = UP_TO_CURRENT;
	int coloring = PERTRACK;
	int bgcode = BACKGROUND_IMAGE;
	int hilicode = WHITE;
	int opacity = 100;
	int pointsize = 10;
	int pointshape = CIRCLE;
	int pointstyle = OPEN;
	int pointcoding = NOCODING;
	int trackwidth = 2;
	int tracklocus = HEAD;
	int fontsize = 12;
	int fontstyle = PLAIN;
	boolean showonlytrackscurrentchannel = true;
	boolean showonlytrackscurrenttime = false;
	boolean showonlypointscurrenttime = false;
	boolean showcalibrated = false;
	
	AlphaComposite trackopacity = null;
	final static AlphaComposite snapopacity = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1);
	final static BasicStroke snapstroke = new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,new float[]{0,2},0);
	final static BasicStroke pointstroke = new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	BasicStroke trackstroke = null;
	Font trackfont = null;
	Color hilicolor = null;
	Color bgcolor = null;
	
	// Program settings:
	boolean showlog = false;
	boolean safemodify = false;
	boolean usedispmdf = true;
	boolean activateimage = true;
	boolean movedialog = true;
	boolean separatefolders = false;
	boolean reusefolders = false;
	boolean savechanges = true;
	boolean confirmquit = true;
	
	MTJSettings(final MTrackJ mtrackj) {
		
		this.mtrackj = mtrackj;
		checkapply();
	}
	
	MTJSettings duplicate() {
		
		final MTJSettings settings = new MTJSettings(mtrackj);
		
		settings.xloadoffset = xloadoffset;
		settings.yloadoffset = yloadoffset;
		settings.zloadoffset = zloadoffset;
		settings.tloadoffset = tloadoffset;
		settings.cloadoffset = cloadoffset;
		
		settings.ximportoffset = ximportoffset;
		settings.yimportoffset = yimportoffset;
		settings.zimportoffset = zimportoffset;
		settings.timportoffset = timportoffset;
		settings.cimportoffset = cimportoffset;
		
		settings.xsaveoffset = xsaveoffset;
		settings.ysaveoffset = ysaveoffset;
		settings.zsaveoffset = zsaveoffset;
		settings.tsaveoffset = tsaveoffset;
		settings.csaveoffset = csaveoffset;
		
		settings.steptime = steptime;
		settings.timestep = timestep;
		settings.endfinish = endfinish;
		settings.resettime = resettime;
		settings.snapping = snapping;
		settings.snapfeature = snapfeature;
		settings.snaprange = snaprange;
		
		settings.showreference = showreference;
		settings.showactivetrack = showactivetrack;
		settings.showfinishedtracks = showfinishedtracks;
		settings.visibility = visibility;
		settings.coloring = coloring;
		settings.bgcode = bgcode;
		settings.hilicode = hilicode;
		settings.opacity = opacity;
		settings.pointsize = pointsize;
		settings.pointshape = pointshape;
		settings.pointstyle = pointstyle;
		settings.pointcoding = pointcoding;
		settings.trackwidth = trackwidth;
		settings.tracklocus = tracklocus;
		settings.fontsize = fontsize;
		settings.fontstyle = fontstyle;
		settings.showonlytrackscurrentchannel = showonlytrackscurrentchannel;
		settings.showonlytrackscurrenttime = showonlytrackscurrenttime;
		settings.showonlypointscurrenttime = showonlypointscurrenttime;
		settings.showcalibrated = showcalibrated;
		settings.checkapply();
		
		settings.showlog = showlog;
		settings.safemodify = safemodify;
		settings.usedispmdf = usedispmdf;
		settings.activateimage = activateimage;
		settings.movedialog = movedialog;
		settings.separatefolders = separatefolders;
		settings.reusefolders = reusefolders;
		settings.savechanges = savechanges;
		settings.confirmquit = confirmquit;
		
		return settings;
	}
	
	void restore(final int which) {
		
		String whichsettings = "no";
		
		if ((which & LOADING) != 0) {
			xloadoffset = Prefs.get("mtj.xloadoffset",xloadoffset);
			yloadoffset = Prefs.get("mtj.yloadoffset",yloadoffset);
			zloadoffset = Prefs.get("mtj.zloadoffset",zloadoffset);
			tloadoffset = Prefs.get("mtj.tloadoffset",tloadoffset);
			cloadoffset = Prefs.get("mtj.cloadoffset",cloadoffset);
			whichsettings = "settings for loading";
		}
		if ((which & IMPORTING) != 0) {
			ximportoffset = Prefs.get("mtj.ximportoffset",ximportoffset);
			yimportoffset = Prefs.get("mtj.yimportoffset",yimportoffset);
			zimportoffset = Prefs.get("mtj.zimportoffset",zimportoffset);
			timportoffset = Prefs.get("mtj.timportoffset",timportoffset);
			cimportoffset = Prefs.get("mtj.cimportoffset",cimportoffset);
			whichsettings = "settings for importing";
		}
		if ((which & SAVING) != 0) {
			xsaveoffset = Prefs.get("mtj.xsaveoffset",xsaveoffset);
			ysaveoffset = Prefs.get("mtj.ysaveoffset",ysaveoffset);
			zsaveoffset = Prefs.get("mtj.zsaveoffset",zsaveoffset);
			tsaveoffset = Prefs.get("mtj.tsaveoffset",tsaveoffset);
			csaveoffset = Prefs.get("mtj.csaveoffset",csaveoffset);
			whichsettings = "settings for saving";
		}
		if ((which & TRACKING) != 0) {
			steptime = Prefs.get("mtj.steptime",steptime);
			timestep = (int)Prefs.get("mtj.timestep",timestep);
			endfinish = Prefs.get("mtj.endfinish",endfinish);
			resettime = Prefs.get("mtj.resettime",resettime);
			snapping = Prefs.get("mtj.snapping",snapping);
			snapfeature = (int)Prefs.get("mtj.snapfeature",snapfeature);
			snaprange = (int)Prefs.get("mtj.snaprange",snaprange);
			whichsettings = "settings for tracking";
		}
		if ((which & DISPLAYING) != 0) {
			showreference = Prefs.get("mtj.showreference",showreference);
			showactivetrack = Prefs.get("mtj.showactivetrack",showactivetrack);
			showfinishedtracks = Prefs.get("mtj.showfinishedtracks",showfinishedtracks);
			visibility = (int)Prefs.get("mtj.visibility",visibility);
			coloring = (int)Prefs.get("mtj.coloring",coloring);
			bgcode = (int)Prefs.get("mtj.bgcode",bgcode);
			hilicode = (int)Prefs.get("mtj.hilicode",hilicode);
			opacity = (int)Prefs.get("mtj.opacity",opacity);
			pointsize = (int)Prefs.get("mtj.pointsize",pointsize);
			pointshape = (int)Prefs.get("mtj.pointshape",pointshape);
			pointstyle = (int)Prefs.get("mtj.pointstyle",pointstyle);
			pointcoding = (int)Prefs.get("mtj.pointcoding",pointcoding);
			trackwidth = (int)Prefs.get("mtj.trackwidth",trackwidth);
			tracklocus = (int)Prefs.get("mtj.tracklocus",tracklocus);
			fontsize = (int)Prefs.get("mtj.fontsize",fontsize);
			fontstyle = (int)Prefs.get("mtj.fontstyle",fontstyle);
			showonlytrackscurrentchannel = Prefs.get("mtj.showonlytrackscurrentchannel",showonlytrackscurrentchannel);
			showonlytrackscurrenttime = Prefs.get("mtj.showonlytrackscurrenttime",showonlytrackscurrenttime);
			showonlypointscurrenttime = Prefs.get("mtj.showonlypointscurrenttime",showonlypointscurrenttime);
			showcalibrated = Prefs.get("mtj.showcalibrated",showcalibrated);
			whichsettings = "settings for displaying";
		}
		if ((which & PROGRAM) != 0) {
			showlog = Prefs.get("mtj.showlog",showlog);
			safemodify = Prefs.get("mtj.safemodify",safemodify);
			usedispmdf = Prefs.get("mtj.usedispmdf",usedispmdf);
			activateimage = Prefs.get("mtj.activateimage",activateimage);
			movedialog = Prefs.get("mtj.movedialog",movedialog);
			separatefolders = Prefs.get("mtj.separatefolders",separatefolders);
			reusefolders = Prefs.get("mtj.reusefolders",reusefolders);
			savechanges = Prefs.get("mtj.savechanges",savechanges);
			confirmquit = Prefs.get("mtj.confirmquit",confirmquit);
			whichsettings = "settings for program";
		}
		if (which == ALL) {
			whichsettings = "settings";
		}
		
		checkapply();
		
		mtrackj.log("Restored "+whichsettings+" from ImageJ prefs file");
	}
	
	void store(final int which) {
		
		switch (which) {
			case LOADING: {
				mtrackj.log("Storing settings for loading...");
				
				Prefs.set("mtj.xloadoffset",xloadoffset);
				mtrackj.log("   Offset x-coordinates = "+xloadoffset);
				
				Prefs.set("mtj.yloadoffset",yloadoffset);
				mtrackj.log("   Offset y-coordinates = "+yloadoffset);
				
				Prefs.set("mtj.zloadoffset",zloadoffset);
				mtrackj.log("   Offset z-coordinates = "+zloadoffset);
				
				Prefs.set("mtj.tloadoffset",tloadoffset);
				mtrackj.log("   Offset t-coordinates = "+tloadoffset);
				
				Prefs.set("mtj.cloadoffset",cloadoffset);
				mtrackj.log("   Offset c-coordinates = "+cloadoffset);
				
				mtrackj.logok();
				break;
			}
			case IMPORTING: {
				mtrackj.log("Storing settings for importing...");
				
				Prefs.set("mtj.ximportoffset",ximportoffset);
				mtrackj.log("   Offset x-coordinates = "+ximportoffset);
				
				Prefs.set("mtj.yimportoffset",yimportoffset);
				mtrackj.log("   Offset y-coordinates = "+yimportoffset);
				
				Prefs.set("mtj.zimportoffset",zimportoffset);
				mtrackj.log("   Offset z-coordinates = "+zimportoffset);
				
				Prefs.set("mtj.timportoffset",timportoffset);
				mtrackj.log("   Offset t-coordinates = "+timportoffset);
				
				Prefs.set("mtj.cimportoffset",cimportoffset);
				mtrackj.log("   Offset c-coordinates = "+cimportoffset);
				
				mtrackj.logok();
				break;
			}
			case SAVING: {
				mtrackj.log("Storing settings for saving...");
				
				Prefs.set("mtj.xsaveoffset",xsaveoffset);
				mtrackj.log("   Offset x-coordinates = "+xsaveoffset);
				
				Prefs.set("mtj.ysaveoffset",ysaveoffset);
				mtrackj.log("   Offset y-coordinates = "+ysaveoffset);
				
				Prefs.set("mtj.zsaveoffset",zsaveoffset);
				mtrackj.log("   Offset z-coordinates = "+zsaveoffset);
				
				Prefs.set("mtj.tsaveoffset",tsaveoffset);
				mtrackj.log("   Offset t-coordinates = "+tsaveoffset);
				
				Prefs.set("mtj.csaveoffset",csaveoffset);
				mtrackj.log("   Offset c-coordinates = "+csaveoffset);
				
				mtrackj.logok();
				break;
			}
			case TRACKING: {
				mtrackj.log("Storing settings for tracking...");
				
				Prefs.set("mtj.steptime",steptime);
				if (steptime) mtrackj.log("   Moving to next time index after adding point");
				else mtrackj.log("   Not moving to next time index after adding point");
				
				Prefs.set("mtj.timestep",timestep);
				if (steptime) mtrackj.log("   Time step size = "+timestep+" frames");
				
				Prefs.set("mtj.endfinish",endfinish);
				if (endfinish) mtrackj.log("   Finishing track after adding point at last time index");
				else mtrackj.log("   Not finishing track after adding point at last time index");
				
				Prefs.set("mtj.resettime",resettime);
				if (resettime) mtrackj.log("   Resetting time to last start index after finishing track");
				else mtrackj.log("   Not resetting time to last start index after finishing track");
				
				Prefs.set("mtj.snapping",snapping);
				if (snapping) mtrackj.log("   Applying local cursor snapping during tracking");
				else mtrackj.log("   Not applying local cursor snapping during tracking");
				
				Prefs.set("mtj.snapfeature",snapfeature);
				if (snapping) {
					final String sfprefix = "   Snap feature = ";
					switch (snapfeature) {
						case MAX_INTENSITY: mtrackj.log(sfprefix+"maximum intensity"); break;
						case MIN_INTENSITY: mtrackj.log(sfprefix+"minimum intensity"); break;
						case BRIGHT_CENTROID: mtrackj.log(sfprefix+"bright centroid"); break;
						case DARK_CENTROID: mtrackj.log(sfprefix+"dark centroid"); break;
					}
				}
				
				Prefs.set("mtj.snaprange",snaprange);
				if (snapping) mtrackj.log("   Snap range = "+snaprange+" x "+snaprange+" pixels");
				
				mtrackj.logok();
				break;
			}
			case DISPLAYING: {
				mtrackj.log("Storing settings for displaying...");
				
				Prefs.set("mtj.showreference",showreference);
				if (showreference) mtrackj.log("   Displaying reference");
				else mtrackj.log("   Not displaying reference");
				
				Prefs.set("mtj.showactivetrack",showactivetrack);
				if (showactivetrack) mtrackj.log("   Displaying active track");
				else mtrackj.log("   Not displaying active track");
				
				Prefs.set("mtj.showfinishedtracks",showfinishedtracks);
				if (showfinishedtracks) mtrackj.log("   Displaying finished tracks");
				else mtrackj.log("   Not displaying finished tracks");
				
				Prefs.set("mtj.visibility",visibility);
				final String visprefix = "   Visibility = ";
				switch (visibility) {
					case ENTIRE_TRACK: mtrackj.log(visprefix+"entire track"); break;
					case UP_TO_CURRENT: mtrackj.log(visprefix+"up to current time"); break;
					case FROM_CURRENT: mtrackj.log(visprefix+"from current time"); break;
				}
				
				Prefs.set("mtj.coloring",coloring);
				final String colprefix = "   Coloring = ";
				switch (coloring) {
					case MONOCHROME: mtrackj.log(colprefix+"monochrome"); break;
					case PERCLUSTER: mtrackj.log(colprefix+"per cluster"); break;
					case PERTRACK: mtrackj.log(colprefix+"per track"); break;
				}
				
				Prefs.set("mtj.bgcode",bgcode);
				final String bgprefix = "   Background = ";
				switch (bgcode) {
					case BACKGROUND_IMAGE: mtrackj.log(bgprefix+"image"); break;
					case BACKGROUND_WHITE: mtrackj.log(bgprefix+"white"); break;
					case BACKGROUND_GRAY: mtrackj.log(bgprefix+"gray"); break;
					case BACKGROUND_BLACK: mtrackj.log(bgprefix+"black"); break;
				}
				
				Prefs.set("mtj.hilicode",hilicode);
				final String hiliprefix = "   Highlighting = ";
				switch (hilicode) {
					case WHITE: mtrackj.log(hiliprefix+"white"); break;
					case GRAY: mtrackj.log(hiliprefix+"gray"); break;
					case BLACK: mtrackj.log(hiliprefix+"black"); break;
					case RED: mtrackj.log(hiliprefix+"red"); break;
					case ORANGE: mtrackj.log(hiliprefix+"orange"); break;
					case YELLOW: mtrackj.log(hiliprefix+"yellow"); break;
					case GREEN: mtrackj.log(hiliprefix+"green"); break;
					case CYAN: mtrackj.log(hiliprefix+"cyan"); break;
					case BLUE: mtrackj.log(hiliprefix+"blue"); break;
					case MAGENTA: mtrackj.log(hiliprefix+"magenta"); break;
					case PINK: mtrackj.log(hiliprefix+"pink"); break;
				}
				
				Prefs.set("mtj.opacity",opacity);
				mtrackj.log("   Opacity = "+opacity+" percent");
				
				Prefs.set("mtj.pointsize",pointsize);
				mtrackj.log("   Point size = "+pointsize+" pixels");
				
				Prefs.set("mtj.pointshape",pointshape);
				final String pointshapeprefix = "   Point shape = ";
				switch (pointshape) {
					case CIRCLE: mtrackj.log(pointshapeprefix+"circle"); break;
					case SQUARE: mtrackj.log(pointshapeprefix+"square"); break;
					case TRIANGLE: mtrackj.log(pointshapeprefix+"triangle"); break;
					case CROSS: mtrackj.log(pointshapeprefix+"cross"); break;
				}
				
				Prefs.set("mtj.pointstyle",pointstyle);
				final String pointstyleprefix = "   Point style = ";
				switch (pointstyle) {
					case OPEN: mtrackj.log(pointstyleprefix+"open"); break;
					case CLOSED: mtrackj.log(pointstyleprefix+"closed"); break;
				}
				
				Prefs.set("mtj.pointcoding",pointcoding);
				final String pointcodingprefix = "   Point coding = ";
				switch (pointcoding) {
					case NOCODING: mtrackj.log(pointcodingprefix+"none"); break;
					case DEPTH_SIZE: mtrackj.log(pointcodingprefix+"depth > size"); break;
					case DEPTH_COLOR: mtrackj.log(pointcodingprefix+"depth > color"); break;
					case DEPTH_BOTH: mtrackj.log(pointcodingprefix+"depth > both"); break;
					case DISTANCE_SIZE: mtrackj.log(pointcodingprefix+"distance > size"); break;
					case DISTANCE_COLOR: mtrackj.log(pointcodingprefix+"distance > color"); break;
					case DISTANCE_BOTH: mtrackj.log(pointcodingprefix+"distance > both"); break;
				}
				
				Prefs.set("mtj.trackwidth",trackwidth);
				mtrackj.log("   Track width = "+trackwidth+" pixels");
				
				Prefs.set("mtj.tracklocus",tracklocus);
				final String tracklocusprefix = "   Track IDs = ";
				switch (tracklocus) {
					case TAIL: mtrackj.log(tracklocusprefix+"tail"); break;
					case HEAD: mtrackj.log(tracklocusprefix+"head"); break;
					case MIDDLE: mtrackj.log(tracklocusprefix+"middle"); break;
				}
				
				Prefs.set("mtj.fontsize",fontsize);
				mtrackj.log("   Font size = "+fontsize+" pixels");
				
				Prefs.set("mtj.fontstyle",fontstyle);
				final String fontstyleprefix = "   Font style = ";
				switch (fontstyle) {
					case PLAIN: mtrackj.log(fontstyleprefix+"plain"); break;
					case BOLD: mtrackj.log(fontstyleprefix+"bold"); break;
					case ITALIC: mtrackj.log(fontstyleprefix+"italic"); break;
					case ITALIC_BOLD: mtrackj.log(fontstyleprefix+"italic-bold"); break;
				}
				
				Prefs.set("mtj.showonlytrackscurrentchannel",showonlytrackscurrentchannel);
				if (showonlytrackscurrentchannel) mtrackj.log("   Displaying only tracks present in current channel");
				else mtrackj.log("   Displaying also tracks not present in current channel");
				
				Prefs.set("mtj.showonlytrackscurrenttime",showonlytrackscurrenttime);
				if (showonlytrackscurrenttime) mtrackj.log("   Displaying only tracks present at current time");
				else mtrackj.log("   Displaying also tracks not present at current time");
				
				Prefs.set("mtj.showonlypointscurrenttime",showonlypointscurrenttime);
				if (showonlypointscurrenttime) mtrackj.log("   Displaying only track points at current time");
				else mtrackj.log("   Displaying also track points not at current time");
				
				Prefs.set("mtj.showcalibrated",showcalibrated);
				if (showcalibrated) mtrackj.log("   Displaying calibrated coordinate values");
				else mtrackj.log("   Displaying non-calibrated coordinate values");
				
				mtrackj.logok();
				break;
			}
			case PROGRAM: {
				mtrackj.log("Storing settings for program...");
				
				Prefs.set("mtj.showlog",showlog);
				if (showlog) mtrackj.log("   Displaying log messages");
				else mtrackj.closelogwindow();
				
				Prefs.set("mtj.safemodify",safemodify);
				if (safemodify) mtrackj.log("   Operating in safe modification mode");
				else mtrackj.log("   Not operating in safe modification mode");
				
				Prefs.set("mtj.usedispmdf",usedispmdf);
				if (usedispmdf) mtrackj.log("   Using displaying settings from data files");
				else mtrackj.log("   Not using displaying settings from data files");
				
				Prefs.set("mtj.activateimage",activateimage);
				if (activateimage) mtrackj.log("   Activating image window when mouse enters");
				else mtrackj.log("   Not activating image window when mouse enters");
				
				Prefs.set("mtj.movedialog",movedialog);
				if (movedialog) mtrackj.log("   Moving main dialog along with image window");
				else mtrackj.log("   Not moving main dialog along with image window");
				
				Prefs.set("mtj.separatefolders",separatefolders);
				if (separatefolders) mtrackj.log("   Separating folders for loading/importing/saving");
				else mtrackj.log("   Not separating folders for loading/importing/saving");
				
				Prefs.set("mtj.reusefolders",reusefolders);
				if (reusefolders) mtrackj.log("   Reusing last file folders from previous session");
				else mtrackj.log("   Not reusing last file folders from previous session");
				
				Prefs.set("mtj.savechanges",savechanges);
				if (savechanges) mtrackj.log("   Asking to save changes before loading or quitting");
				else mtrackj.log("   Not asking to save changes before loading or quitting");
				
				Prefs.set("mtj.confirmquit",confirmquit);
				if (confirmquit) mtrackj.log("   Asking for confirmation before quitting program");
				else mtrackj.log("   Not asking for confirmation before quitting program");
				
				mtrackj.logok();
				break;
			}
		}
	}
	
	void decode(final String line, final String version) {
		
		final StringTokenizer litems = new StringTokenizer(line);
		final String settingstype = litems.nextToken();
		if (settingstype.equals("Loading")) {
			xloadoffset = Double.parseDouble(litems.nextToken());
			yloadoffset = Double.parseDouble(litems.nextToken());
			zloadoffset = Double.parseDouble(litems.nextToken());
			tloadoffset = Double.parseDouble(litems.nextToken());
			cloadoffset = Double.parseDouble(litems.nextToken());
		} else if (settingstype.equals("Importing")) {
			ximportoffset = Double.parseDouble(litems.nextToken());
			yimportoffset = Double.parseDouble(litems.nextToken());
			zimportoffset = Double.parseDouble(litems.nextToken());
			timportoffset = Double.parseDouble(litems.nextToken());
			cimportoffset = Double.parseDouble(litems.nextToken());
		} else if (settingstype.equals("Saving")) {
			xsaveoffset = Double.parseDouble(litems.nextToken());
			ysaveoffset = Double.parseDouble(litems.nextToken());
			zsaveoffset = Double.parseDouble(litems.nextToken());
			tsaveoffset = Double.parseDouble(litems.nextToken());
			csaveoffset = Double.parseDouble(litems.nextToken());
		} else if (settingstype.equals("Tracking")) {
			steptime = Boolean.parseBoolean(litems.nextToken());
			timestep = Integer.parseInt(litems.nextToken());
			endfinish = Boolean.parseBoolean(litems.nextToken());
			resettime = Boolean.parseBoolean(litems.nextToken());
			snapping = Boolean.parseBoolean(litems.nextToken());
			snapfeature = Integer.parseInt(litems.nextToken());
			snaprange = Integer.parseInt(litems.nextToken());
		} else if (settingstype.equals("Displaying")) {
			showreference = Boolean.parseBoolean(litems.nextToken());
			if (version.compareTo("1.2.0") <= 0) {
				final boolean showimage = Boolean.parseBoolean(litems.nextToken());
				if (showimage) bgcode = BACKGROUND_IMAGE;
				else bgcode = BACKGROUND_BLACK;
			}
			showactivetrack = Boolean.parseBoolean(litems.nextToken());
			showfinishedtracks = Boolean.parseBoolean(litems.nextToken());
			visibility = Integer.parseInt(litems.nextToken());
			coloring = Integer.parseInt(litems.nextToken());
			if (version.compareTo("1.3.0") >= 0) {
				bgcode = Integer.parseInt(litems.nextToken());
			}
			hilicode = Integer.parseInt(litems.nextToken());
			opacity = Integer.parseInt(litems.nextToken());
			pointsize = Integer.parseInt(litems.nextToken());
			pointshape = Integer.parseInt(litems.nextToken());
			pointstyle = Integer.parseInt(litems.nextToken());
			if (version.compareTo("1.4.0") >= 0) {
				pointcoding = Integer.parseInt(litems.nextToken());
			}
			trackwidth = Integer.parseInt(litems.nextToken());
			if (version.compareTo("1.4.0") >= 0) {
				tracklocus = Integer.parseInt(litems.nextToken());
			}
			fontsize = Integer.parseInt(litems.nextToken());
			fontstyle = Integer.parseInt(litems.nextToken());
			showonlytrackscurrentchannel = Boolean.parseBoolean(litems.nextToken());
			showonlytrackscurrenttime = Boolean.parseBoolean(litems.nextToken());
			showonlypointscurrenttime = Boolean.parseBoolean(litems.nextToken());
			showcalibrated = Boolean.parseBoolean(litems.nextToken());
		} else if (settingstype.equals("Program")) {
			showlog = Boolean.parseBoolean(litems.nextToken());
			safemodify = Boolean.parseBoolean(litems.nextToken());
			usedispmdf = Boolean.parseBoolean(litems.nextToken());
			activateimage = Boolean.parseBoolean(litems.nextToken());
			movedialog = Boolean.parseBoolean(litems.nextToken());
			separatefolders = Boolean.parseBoolean(litems.nextToken());
			reusefolders = Boolean.parseBoolean(litems.nextToken());
			savechanges = Boolean.parseBoolean(litems.nextToken());
			confirmquit = Boolean.parseBoolean(litems.nextToken());
		}
		
		checkapply();
	}
	
	String encode(final int which) {
		
		final StringBuffer s = new StringBuffer();
		
		switch (which) {
			case LOADING:
				s.append("Loading");
				s.append(" "+String.valueOf(xloadoffset));
				s.append(" "+String.valueOf(yloadoffset));
				s.append(" "+String.valueOf(zloadoffset));
				s.append(" "+String.valueOf(tloadoffset));
				s.append(" "+String.valueOf(cloadoffset));
				break;
			case IMPORTING:
				s.append("Importing");
				s.append(" "+String.valueOf(ximportoffset));
				s.append(" "+String.valueOf(yimportoffset));
				s.append(" "+String.valueOf(zimportoffset));
				s.append(" "+String.valueOf(timportoffset));
				s.append(" "+String.valueOf(cimportoffset));
				break;
			case SAVING:
				s.append("Saving");
				s.append(" "+String.valueOf(xsaveoffset));
				s.append(" "+String.valueOf(ysaveoffset));
				s.append(" "+String.valueOf(zsaveoffset));
				s.append(" "+String.valueOf(tsaveoffset));
				s.append(" "+String.valueOf(csaveoffset));
				break;
			case TRACKING:
				s.append("Tracking");
				s.append(" "+String.valueOf(steptime));
				s.append(" "+String.valueOf(timestep));
				s.append(" "+String.valueOf(endfinish));
				s.append(" "+String.valueOf(resettime));
				s.append(" "+String.valueOf(snapping));
				s.append(" "+String.valueOf(snapfeature));
				s.append(" "+String.valueOf(snaprange));
				break;
			case DISPLAYING:
				s.append("Displaying");
				s.append(" "+String.valueOf(showreference));
				s.append(" "+String.valueOf(showactivetrack));
				s.append(" "+String.valueOf(showfinishedtracks));
				s.append(" "+String.valueOf(visibility));
				s.append(" "+String.valueOf(coloring));
				s.append(" "+String.valueOf(bgcode));
				s.append(" "+String.valueOf(hilicode));
				s.append(" "+String.valueOf(opacity));
				s.append(" "+String.valueOf(pointsize));
				s.append(" "+String.valueOf(pointshape));
				s.append(" "+String.valueOf(pointstyle));
				s.append(" "+String.valueOf(pointcoding));
				s.append(" "+String.valueOf(trackwidth));
				s.append(" "+String.valueOf(tracklocus));
				s.append(" "+String.valueOf(fontsize));
				s.append(" "+String.valueOf(fontstyle));
				s.append(" "+String.valueOf(showonlytrackscurrentchannel));
				s.append(" "+String.valueOf(showonlytrackscurrenttime));
				s.append(" "+String.valueOf(showonlypointscurrenttime));
				s.append(" "+String.valueOf(showcalibrated));
				break;
			case PROGRAM:
				s.append("Program");
				s.append(" "+String.valueOf(showlog));
				s.append(" "+String.valueOf(safemodify));
				s.append(" "+String.valueOf(usedispmdf));
				s.append(" "+String.valueOf(activateimage));
				s.append(" "+String.valueOf(movedialog));
				s.append(" "+String.valueOf(separatefolders));
				s.append(" "+String.valueOf(reusefolders));
				s.append(" "+String.valueOf(savechanges));
				s.append(" "+String.valueOf(confirmquit));
				break;
		}
		
		return s.toString();
	}
	
	boolean input(final int which) {
		
		boolean proceed = true;
		
		switch (which) {
			case LOADING:
			case IMPORTING: 
			case SAVING: {
				double xoffset = 0;
				double yoffset = 0;
				double zoffset = 0;
				double toffset = 0;
				double coffset = 0;
				if (which == LOADING) {
					xoffset = xloadoffset;
					yoffset = yloadoffset;
					zoffset = zloadoffset;
					toffset = tloadoffset;
					coffset = cloadoffset;
				} else if (which == IMPORTING) {
					xoffset = ximportoffset;
					yoffset = yimportoffset;
					zoffset = zimportoffset;
					toffset = timportoffset;
					coffset = cimportoffset;
				} else if (which == SAVING) {
					xoffset = xsaveoffset;
					yoffset = ysaveoffset;
					zoffset = zsaveoffset;
					toffset = tsaveoffset;
					coffset = csaveoffset;
				}
				final MTJGenial gd = new MTJGenial(mtrackj.name()+": Offsets",mtrackj.dialog());
				gd.addStringField("Offset x-coordinates:",String.valueOf(xoffset),8,"pixels");
				gd.addStringField("Offset y-coordinates:",String.valueOf(yoffset),8,"pixels");
				gd.addStringField("Offset z-coordinates:",String.valueOf(zoffset),8,"slices");
				gd.addStringField("Offset t-coordinates:",String.valueOf(toffset),8,"frames");
				gd.addStringField("Offset c-coordinates:",String.valueOf(coffset),8,"channels");
				gd.showDialog();
				if (!gd.wasCanceled()) {
					try { xoffset = Double.parseDouble(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for x-coordinates offset"); proceed = false; }
					try { yoffset = Double.parseDouble(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for y-coordinates offset"); proceed = false; }
					try { zoffset = Double.parseDouble(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for z-coordinates offset"); proceed = false; }
					try { toffset = Double.parseDouble(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for t-coordinates offset"); proceed = false; }
					try { coffset = Double.parseDouble(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for c-coordinates offset"); proceed = false; }
					if (which == LOADING) {
						xloadoffset = xoffset;
						yloadoffset = yoffset;
						zloadoffset = zoffset;
						tloadoffset = toffset;
						cloadoffset = coffset;
						store(LOADING);
					} else if (which == IMPORTING) {
						ximportoffset = xoffset;
						yimportoffset = yoffset;
						zimportoffset = zoffset;
						timportoffset = toffset;
						cimportoffset = coffset;
						store(IMPORTING);
					} else if (which == SAVING) {
						xsaveoffset = xoffset;
						ysaveoffset = yoffset;
						zsaveoffset = zoffset;
						tsaveoffset = toffset;
						csaveoffset = coffset;
						store(SAVING);
					}
				} else proceed = false;
				break;
			}
			case TRACKING: {
				final MTJGenial gd = new MTJGenial(mtrackj.name()+": Tracking",mtrackj.dialog());
				gd.addCheckbox("Move to next time index after adding point",steptime);
				gd.setInsets(1,0,2); gd.addStringField("            Time step size:",String.valueOf(timestep),6,"frames");
				gd.addCheckbox("Finish track after adding point at last time index",endfinish);
				gd.addCheckbox("Reset time to last start index after finishing track",resettime);
				gd.addCheckbox("Apply local cursor snapping during tracking",snapping);
				final String[] snapfeatures = { "Maximum intensity", "Minimum intensity", "Bright centroid", "Dark centroid" };
				gd.addChoice("            Snap feature:",snapfeatures,snapfeatures[snapfeature],new Dimension(120,20));
				final String[] snapranges = new String[25]; for (int sr=3, i=0; i<=24; ++i, sr+=2) snapranges[i] = sr+" x "+sr;
				gd.addChoice("            Snap range:",snapranges,(snaprange+" x "+snaprange),"pixels",new Dimension(85,20));
				gd.showDialog();
				if (!gd.wasCanceled()) {
					steptime = gd.getNextBoolean();
					try { timestep = Integer.parseInt(gd.getNextString()); }
					catch (Throwable e) { mtrackj.error("Invalid value for time step size"); }
					endfinish = gd.getNextBoolean();
					resettime = gd.getNextBoolean();
					snapping = gd.getNextBoolean();
					snapfeature = gd.getNextChoiceIndex();
					snaprange = 3 + 2*gd.getNextChoiceIndex();
					store(TRACKING);
				}
				break;
			}
			case DISPLAYING: {
				final MTJGenial gd = new MTJGenial(mtrackj.name()+": Displaying",mtrackj.dialog());
				final Dimension choicedims = new Dimension(120,20);
				gd.addCheckbox("Display reference",showreference);
				gd.addCheckbox("Display active track",showactivetrack);
				gd.addCheckbox("Display finished tracks",showfinishedtracks);
				final String[] visibilities = {"Entire track","Up to current time","From current time"};
				gd.addChoice("            Visibility:",visibilities,visibilities[visibility],choicedims);
				final String[] colorings = { "Monochrome", "Per cluster", "Per track" };
				gd.addChoice("            Coloring:",colorings,colorings[coloring],choicedims);
				final String[] bgcodes = { "Image", "White", "Gray", "Black" };
				gd.addChoice("            Background:",bgcodes,bgcodes[bgcode],choicedims);
				final String[] hilicodes = { "White", "Gray", "Black", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Magenta", "Pink" };
				gd.addChoice("            Highlighting:",hilicodes,hilicodes[hilicode],choicedims);
				final String[] opacities = new String[21]; for (int i=0; i<=20; ++i) opacities[i] = String.valueOf(5*i);
				gd.addChoice("            Opacity:",opacities,String.valueOf(opacity),"percent",choicedims);
				final String[] pointsizes = new String[51]; for (int i=0; i<=50; ++i) pointsizes[i] = String.valueOf(i*2);
				gd.addChoice("            Point size:",pointsizes,String.valueOf(pointsize),"pixels",choicedims);
				final String[] pointshapes = { "Circle", "Square", "Triangle", "Cross" };
				gd.addChoice("            Point shape:",pointshapes,pointshapes[pointshape],choicedims);
				final String[] pointstyles = { "Open", "Closed" };
				gd.addChoice("            Point style:",pointstyles,pointstyles[pointstyle],choicedims);
				final String[] pointcodings = { "None", "Depth > size", "Depth > color", "Depth > both", "Distance > size", "Distance > color", "Distance > both" };
				gd.addChoice("            Point coding:",pointcodings,pointcodings[pointcoding],choicedims);
				final String[] trackwidths = new String[101]; for (int i=0; i<=100; ++i) trackwidths[i] = String.valueOf(i);
				gd.addChoice("            Track width:",trackwidths,String.valueOf(trackwidth),"pixels",choicedims);
				final String[] trackloci = { "Tail", "Head", "Middle" };
				gd.addChoice("            Track IDs:",trackloci,trackloci[tracklocus],choicedims);
				final String[] fontsizes = new String[101]; for (int i=0; i<=100; ++i) fontsizes[i] = String.valueOf(i);
				gd.addChoice("            Font size:",fontsizes,String.valueOf(fontsize),"pixels",choicedims);
				final String[] fontstyles = { "Plain", "Bold", "Italic", "Italic Bold" };
				gd.addChoice("            Font style:",fontstyles,fontstyles[fontstyle],choicedims);
				gd.addCheckbox("Display only tracks present in current channel",showonlytrackscurrentchannel);
				gd.addCheckbox("Display only tracks present at current time",showonlytrackscurrenttime);
				gd.addCheckbox("Display only track points at current time",showonlypointscurrenttime);
				gd.addCheckbox("Display calibrated coordinates and values",showcalibrated);
				gd.showDialog();
				if (!gd.wasCanceled()) {
					showreference = gd.getNextBoolean();
					showactivetrack = gd.getNextBoolean();
					showfinishedtracks = gd.getNextBoolean();
					visibility = gd.getNextChoiceIndex();
					coloring = gd.getNextChoiceIndex();
					bgcode = gd.getNextChoiceIndex();
					hilicode = gd.getNextChoiceIndex();
					opacity = Integer.parseInt(gd.getNextChoice());
					pointsize = Integer.parseInt(gd.getNextChoice());
					pointshape = gd.getNextChoiceIndex();
					pointstyle = gd.getNextChoiceIndex();
					pointcoding = gd.getNextChoiceIndex();
					trackwidth = Integer.parseInt(gd.getNextChoice());
					tracklocus = gd.getNextChoiceIndex();
					fontsize = Integer.parseInt(gd.getNextChoice());
					fontstyle = gd.getNextChoiceIndex();
					showonlytrackscurrentchannel = gd.getNextBoolean();
					showonlytrackscurrenttime = gd.getNextBoolean();
					showonlypointscurrenttime = gd.getNextBoolean();
					showcalibrated = gd.getNextBoolean();
					checkapply();
					store(DISPLAYING);
					mtrackj.handler().redraw();
				}
				break;
			}
			case PROGRAM: {
				final MTJGenial gd = new MTJGenial(mtrackj.name()+": Options",mtrackj.dialog());
				gd.addCheckbox("Display log messages",showlog);
				gd.addCheckbox("Operate in safe modification mode",safemodify);
				gd.addCheckbox("Use displaying settings from data files",usedispmdf);
				gd.addCheckbox("Activate image window when mouse enters",activateimage);
				gd.addCheckbox("Move main dialog along with image window",movedialog);
				gd.addCheckbox("Separate folders for loading/importing/saving",separatefolders);
				gd.addCheckbox("Reuse last file folders from previous session",reusefolders);
				gd.addCheckbox("Ask to save changes before loading or quitting",savechanges);
				gd.addCheckbox("Ask for confirmation before quitting program",confirmquit);
				gd.showDialog();
				if (!gd.wasCanceled()) {
					showlog = gd.getNextBoolean();
					safemodify = gd.getNextBoolean();
					usedispmdf = gd.getNextBoolean();
					activateimage = gd.getNextBoolean();
					movedialog = gd.getNextBoolean();
					separatefolders = gd.getNextBoolean();
					reusefolders = gd.getNextBoolean();
					savechanges = gd.getNextBoolean();
					confirmquit = gd.getNextBoolean();
					store(PROGRAM);
				}
				break;
			}
		}
		
		return proceed;
	}
	
	private void checkapply() {
		
		// Tracking settings:
		if (snapfeature < 0) snapfeature = 0; else if (snapfeature > 3) snapfeature = 3;
		if (snaprange < 3) snaprange = 3;
		
		// Displaying settings:
		if (visibility < 0) visibility = 0; else if (visibility > 2) visibility = 2;
		if (coloring < 0) coloring = 0; else if (coloring > 2) coloring = 2;
		if (bgcode < 0) bgcode = 0; else if (bgcode > 3) bgcode = 3;
		switch (bgcode) {
			case BACKGROUND_IMAGE: break;
			case BACKGROUND_WHITE: bgcolor = Color.white; break;
			case BACKGROUND_GRAY: bgcolor = Color.gray; break;
			case BACKGROUND_BLACK: bgcolor = Color.black; break;
		}
		if (hilicode < 0) hilicode = 0; else if (hilicode > 10) hilicode = 10;
		switch (hilicode) {
			case WHITE: hilicolor = color(Color.white); break;
			case GRAY: hilicolor = color(Color.gray); break;
			case BLACK: hilicolor = color(Color.black); break;
			case RED: hilicolor = color(Color.red); break;
			case ORANGE: hilicolor = color(Color.orange); break;
			case YELLOW: hilicolor = color(Color.yellow); break;
			case GREEN: hilicolor = color(Color.green); break;
			case CYAN: hilicolor = color(Color.cyan); break;
			case BLUE: hilicolor = color(Color.blue); break;
			case MAGENTA: hilicolor = color(Color.magenta); break;
			case PINK: hilicolor = color(Color.pink); break;
		}
		if (opacity < 0) opacity = 0; else if (opacity > 100) opacity = 100;
		trackopacity = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,opacity/100f);
		if (pointsize < 0) pointsize = 0; else if (pointsize > 100) pointsize = 100;
		if (pointshape < 0) pointshape = 0; else if (pointshape > 3) pointshape = 3;
		if (pointstyle < 0) pointstyle = 0; else if (pointstyle > 1) pointstyle = 1;
		if (pointcoding < 0) pointcoding = 0; else if (pointcoding > 6) pointcoding = 6;
		if (trackwidth < 0) trackwidth = 0; else if (trackwidth > 100) trackwidth = 100;
		if (tracklocus < 0) tracklocus = 0; else if (tracklocus > 2) tracklocus = 2;
		trackstroke = new BasicStroke(trackwidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		if (fontsize < 0) fontsize = 0; else if (fontsize > 100) fontsize = 100;
		if (fontstyle < 0) fontstyle = 0; else if (fontstyle > 3) fontstyle = 3;
		switch (fontstyle) {
			case PLAIN: trackfont = new Font("Dialog",Font.PLAIN,fontsize); break;
			case BOLD: trackfont = new Font("Dialog",Font.BOLD,fontsize); break;
			case ITALIC: trackfont = new Font("Dialog",Font.ITALIC,fontsize); break;
			case ITALIC_BOLD: trackfont = new Font("Dialog",Font.ITALIC|Font.BOLD,fontsize); break;
		}
	}
	
	private Color color(final Color color) {
		
		return new Color(color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha());
	}
	
}

// *************************************************************************************************
final class MTJHandler extends Roi implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	private final MTrackJ mtrackj;
	
	private MTJAssembly activeassembly = null;
	private MTJCluster activecluster = null;
	private MTJTrack activetrack = null;
	
	private MTJAssembly hiliassembly = null;
	private MTJCluster hilicluster = null;
	private MTJTrack mergetrack = null;
	private MTJTrack hilitrack = null;
	private MTJPoint hilipoint = null;
	
	private final MTJPoint curpos = new MTJPoint();
	private final MTJPoint orgpos = new MTJPoint();
	private final MTJPoint movpos = new MTJPoint();
	private final MTJPoint snapos = new MTJPoint();
	private int scrlx, scrly, laststarttime=1;
	
	private final Palette clustercolors = new Palette(Palette.ARBITRARY);
	private final Palette trackcolors = new Palette(Palette.ARBITRARY);
	private final Color[] depthcolors;
	
	private boolean changed = false;
	private boolean redraw = false;
	private boolean oncanvas = false;
	private boolean snapctrl = false;
	private boolean spacedown = false;
	
	final static int INIT=-1, NONE=0, ADD=1, CLUSTER=2, HIDE=3, MERGE=4, SPLIT=5, MOVE=6, DELETE=7, COLOR=8, REFER=9, SETIDS=10;
	private int mode = INIT;
	
	private final static Cursor arrowcursor = makecursor(MouseCursor.ARROW);
	private final static Cursor crosscursor = makecursor(MouseCursor.CROSSHAIR);
	private final static Cursor magnicursor = makecursor(MouseCursor.MAGNIFIER);
	private final static Cursor handycursor = makecursor(MouseCursor.HAND);
	
	MTJHandler(final MTrackJ mtrackj) {
		
		super(0,0,mtrackj.getwidth(),mtrackj.getheight());
		this.mtrackj = mtrackj;
		activeassembly = new MTJAssembly();
		depthcolors = depthcolors();
		ic = null; // Work-around to prevent cloning in ImagePlus.setRoi()
		mtrackj.image().setRoi(this);
	}
	
	private Color[] depthcolors() {
		
		int nrcolors = mtrackj.nrslices();
		if (nrcolors < 2) nrcolors = 2;
		final Color[] colors = new Color[nrcolors];
		final Wave2Color w2c = new Wave2Color();
		final double min = 440;
		final double max = 645;
		final double ran = max - min;
		final double nrc = nrcolors - 1;
		for (int i=0; i<nrcolors; ++i) {
			final Color color = w2c.color(max - ran*i/nrc);
			colors[i] = color;
		}
		return colors;
	}
	
	boolean changed() { return changed; }
	
	void changed(final boolean changed) { this.changed = changed; }
	
	int mode() { return mode; }
	
	void mode(final int mode) {
		
		if (mode != this.mode) {
			final int oldmode = this.mode;
			this.mode = mode;
			mtrackj.dialog().resetbuttons();
			finishactivetrack(); dehighlight();
			if (mode == NONE) IJ.setTool(Toolbar.MAGNIFIER);
			else IJ.setTool(Toolbar.POINT);
			cursor(); redraw();
			switch (oldmode) {
				case ADD: mtrackj.log("Deactivated track adding"); break;
				case CLUSTER: mtrackj.log("Deactivated track clustering"); break;
				case HIDE: mtrackj.log("Deactivated track hiding"); break;
				case MERGE: mtrackj.log("Deactivated track merging"); break;
				case SPLIT: mtrackj.log("Deactivated track splitting"); break;
				case MOVE: mtrackj.log("Deactivated object moving"); break;
				case DELETE: mtrackj.log("Deactivated object deleting"); break;
				case COLOR: mtrackj.log("Deactivated color setting"); break;
				case REFER: mtrackj.log("Deactivated reference setting"); break;
				case SETIDS: mtrackj.log("Deactivated ID setting"); break;
			}
			switch (mode) {
				case ADD: mtrackj.log("Activated track adding"); break;
				case CLUSTER: mtrackj.log("Activated track clustering"); break;
				case HIDE: mtrackj.log("Activated track hiding"); break;
				case MERGE: mtrackj.log("Activated track merging"); break;
				case SPLIT: mtrackj.log("Activated track splitting"); break;
				case MOVE: mtrackj.log("Activated object moving"); break;
				case DELETE: mtrackj.log("Activated object deleting"); break;
				case COLOR: mtrackj.log("Activated color setting"); break;
				case REFER: mtrackj.log("Activated reference setting"); break;
				case SETIDS: mtrackj.log("Activated ID setting"); break;
			}
		}
	}
	
	Cursor cursor(final int which) {
		
		Cursor cursor = null;
		switch (which) {
			case MouseCursor.ARROW: cursor = arrowcursor; break;
			case MouseCursor.CROSSHAIR: cursor = crosscursor; break;
			case MouseCursor.MAGNIFIER: cursor = magnicursor; break;
			case MouseCursor.HAND: cursor = handycursor; break;
		}
		return cursor;
	}
	
	void cursor() {
		
		switch (Toolbar.getToolId()) {
			case Toolbar.POINT:
				switch (mode) {
					case ADD: case REFER: mtrackj.canvas().setCursor(crosscursor); break;
					default: mtrackj.canvas().setCursor(arrowcursor); break;
				}
				break;
			case Toolbar.MAGNIFIER: mtrackj.canvas().setCursor(magnicursor); break;
			case Toolbar.HAND: mtrackj.canvas().setCursor(handycursor); break;
			default: mtrackj.canvas().setCursor(arrowcursor); break;
		}
	}
	
	private static Cursor makecursor(final int type) {
		
		Cursor cursor = null;
		final MouseCursor mc = new MouseCursor();
		switch(type) {
			case MouseCursor.ARROW: cursor = mc.create(MouseCursor.ARROW); break;
			case MouseCursor.CROSSHAIR: cursor = mc.create(MouseCursor.CROSSHAIR); break;
			case MouseCursor.MAGNIFIER: cursor = mc.create(MouseCursor.MAGNIFIER); break;
			case MouseCursor.HAND: cursor = mc.create(MouseCursor.HAND); break;
		}
		return cursor;
	}
	
	public void mouseClicked(final MouseEvent e) { }
	
	public void mouseEntered(final MouseEvent e) { try {
		
		if (mtrackj.settings().activateimage) {
			mtrackj.window().toFront();
			mtrackj.canvas().requestFocusInWindow();
		}
		cursor();
		oncanvas = true;
		snapctrl = e.isControlDown();
		if (Toolbar.getToolId() != Toolbar.HAND && spacedown) spacedown = false;
		redraw(); // In case snapping is on
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mouseExited(final MouseEvent e) { try {
		
		mtrackj.copyright();
		oncanvas = false;
		redraw(); // In case snapping is on
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	private long lastpresstime = System.currentTimeMillis();
	
	public void mousePressed(final MouseEvent e) { try {
		
		switch (Toolbar.getToolId()) {
			case Toolbar.POINT: {
				switch (mode) {
					case ADD: {
						if (hilitrack != null) {
							finishactivetrack();
							activetrack = hilitrack;
							hilitrack = null;
							mtrackj.log("Prepared track "+id2string(activetrack)+" for continuation");
							final boolean reverse = e.isShiftDown();
							if (reverse) {
								mtrackj.log("Moving to first point of track...");
								setcoords(activetrack.first());
								mtrackj.logok();
							} else {
								mtrackj.log("Moving to last point of track...");
								setcoords(activetrack.last());
								mtrackj.logok();
							}
							steptime(reverse);
						} else {
							final long curpresstime = System.currentTimeMillis();
							if ((curpresstime - lastpresstime) > 200) {
								if (activetrack == null) {
									initactivetrack();
									laststarttime = curpos.t;
								}
								final MTJPoint addpoint = snapping() ? snapos.duplicate() : curpos.duplicate();
								final int addpointc = addpoint.c;
								mtrackj.log("Adding point "+coordstring(addpoint)+"...");
								activetrack.add(addpoint);
								changed = true;
								mtrackj.logok();
								if (addpoint.c != addpointc) mtrackj.log("Channel index of point changed to "+mtrackj.d2s(addpoint.c));
								if (mtrackj.settings().steptime) {
									final boolean reverse = e.isShiftDown();
									final int maxt = mtrackj.nrframes();
									if (mtrackj.settings().endfinish && ((reverse && curpos.t == 1) || (!reverse && curpos.t == maxt))) {
										finishactivetrack();
										if (mtrackj.settings().resettime) resettime();
									} else steptime(reverse);
								}
							} else {
								finishactivetrack();
								if (mtrackj.settings().resettime) resettime();
							}
							lastpresstime = curpresstime;
						}
						redraw();
						break;
					}
					case CLUSTER: {
						if (hilitrack != null) {
							boolean doit = true;
							boolean confirmcluster = true;
							final String hilitrackid = id2string(hilitrack);
							if (e.isControlDown()) {
								final MTJGenial gd = new MTJGenial(mtrackj.name()+": Cluster",mtrackj.window(),topleft(e));
								final int nrclusters = activeassembly.size();
								final String[] clusters = new String[nrclusters+1];
								for (int c=0; c<nrclusters; ++c) clusters[c] = String.valueOf(activeassembly.get(c).id());
								clusters[nrclusters] = "New";
								gd.setInsets(10,0,0);
								gd.addChoice("Active cluster:",clusters,String.valueOf(activecluster.id()));
								gd.showDialog();
								if (!gd.wasCanceled()) {
									final int i = gd.getNextChoiceIndex();
									if (i == nrclusters) initactivecluster();
									else activecluster = activeassembly.get(i);
									mtrackj.log("Activated cluster "+activecluster.id());
								} else doit = false;
								confirmcluster = false;
							}
							if (mtrackj.settings().safemodify && confirmcluster) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Cluster",mtrackj.window(),topleft(e));
								gd.addMessage("Are you sure you want to assign track "+hilitrackid+" to cluster "+activecluster.id()+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								if (hilitrack.cluster() != activecluster) {
									mtrackj.log("Assigning track "+hilitrackid+" to cluster "+activecluster.id()+"...");
									hilitrack.cluster().delete(hilitrack);
									activecluster.add(hilitrack);
									changed = true;
									mtrackj.logok();
									delete0size();
									mtrackj.log("Track "+hilitrackid+" has become track "+id2string(hilitrack));
								} else mtrackj.log("Track "+hilitrackid+" is already in cluster "+activecluster.id());
							} else delete0size();
							hilitrack = null;
							redraw();
						}
						break;
					}
					case HIDE: {
						final boolean invhide = e.isControlDown();
						final boolean althide = e.isAltDown();
						if (hilitrack != null) {
							boolean doit = true;
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Hide",mtrackj.window(),topleft(e));
								if (invhide) gd.addMessage("Are you sure you want to hide all except track "+id2string(hilitrack)+"?");
								else gd.addMessage("Are you sure you want to hide track "+id2string(hilitrack)+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								if (invhide) {
									mtrackj.log("Hiding all except track "+id2string(hilitrack)+"...");
									activeassembly.hidden(true);
									hilitrack.hidden(false);
								} else {
									mtrackj.log("Hiding track "+id2string(hilitrack)+"...");
									hilitrack.hidden(true);
								}
								mtrackj.logok();
							}
							hilitrack = null;
							redraw();
						} else if (hilicluster != null) {
							boolean doit = true;
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Hide",mtrackj.window(),topleft(e));
								if (invhide) gd.addMessage("Are you sure you want to hide all except cluster "+hilicluster.id()+"?");
								else gd.addMessage("Are you sure you want to hide cluster "+hilicluster.id()+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								if (invhide) {
									mtrackj.log("Hiding all except cluster "+hilicluster.id()+"...");
									activeassembly.hidden(true);
									hilicluster.hidden(false);
								} else {
									mtrackj.log("Hiding cluster "+hilicluster.id()+"...");
									hilicluster.hidden(true);
								}
								mtrackj.logok();
							}
							hilicluster = null;
							redraw();
						} else if (invhide) {
							boolean doit = true;
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Unhide",mtrackj.window(),topleft(e));
								if (althide) gd.addMessage("Are you sure you want to (un)hide all tracks?");
								else gd.addMessage("Are you sure you want to unhide all tracks?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) unhidetracks(althide);
						}
						break;
					}
					case MERGE: {
						if (hilitrack != null) {
							if (mergetrack == null) {
								mtrackj.log("Selecting track "+id2string(hilitrack)+" as basis for merging...");
								mergetrack = hilitrack;
								mtrackj.logok();
							} else if (hilitrack == mergetrack) {
								mtrackj.log("Deselecting track "+id2string(mergetrack)+" as basis for merging...");
								mergetrack = hilitrack = null;
								mtrackj.logok();
							} else {
								boolean doit = true;
								if (mtrackj.settings().safemodify) {
									final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Merge",mtrackj.window(),topleft(e));
									gd.addMessage("Are you sure you want to merge track "+id2string(hilitrack)+" with track "+id2string(mergetrack)+"?");
									gd.showDialog(); if (gd.wasNo()) doit = false;
								}
								if (doit) {
									final int nrpoints = hilitrack.size();
									mtrackj.log("Merging track "+id2string(hilitrack)+" with track "+id2string(mergetrack)+"...");
									for (int i=0; i<nrpoints; ++i) mergetrack.add(hilitrack.get(i));
									mtrackj.logok();
									mtrackj.log("Deleting original track "+id2string(hilitrack)+"...");
									hilitrack.cluster().delete(hilitrack);
									changed = true;
									mtrackj.logok();
									delete0size();
								}
								mergetrack = hilitrack = null;
							}
							redraw();
						}
						break;
					}
					case SPLIT: {
						if (hilipoint != null) {
							boolean doit = true;
							final MTJTrack hilipointtrack = hilipoint.track();
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Split",mtrackj.window(),topleft(e));
								gd.addMessage("Are you sure you want to split track "+id2string(hilipointtrack)+" at point "+hilipoint.id()+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								mtrackj.log("Splitting track "+id2string(hilipointtrack)+" at point "+hilipoint.id()+"...");
								final MTJTrack splittrack = new MTJTrack();
								splittrack.color(trackcolors.next());
								hilipointtrack.cluster().add(splittrack);
								final int istop = hilipointtrack.size();
								int istart = hilipointtrack.index(hilipoint);
								splittrack.add(hilipointtrack.get(istart).duplicate());
								for (int i=istart+1; i<istop; ++i) splittrack.add(hilipointtrack.get(i));
								hilipointtrack.cut(e.isControlDown()?(istart+1):istart);
								changed = true;
								mtrackj.logok();
								delete0size();
							}
							hilipoint = null;
							redraw();
						}
						break;
					}
					case MOVE: {
						if (hilipoint != null) {
							orgpos.coordinates(hilipoint);
							movpos.coordinates(curpos);
						} else if (hilitrack != null) {
							orgpos.coordinates(hilitrack.first());
							movpos.coordinates(curpos);
						} else if (hilicluster != null) {
							orgpos.coordinates(hilicluster.first().first());
							movpos.coordinates(curpos);
						}
						break;
					}
					case DELETE: {
						if (hilipoint != null) {
							boolean doit = true;
							final MTJTrack hilipointtrack = hilipoint.track();
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Delete",mtrackj.window(),topleft(e));
								gd.addMessage("Are you sure you want to delete point "+id2string(hilipoint)+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								mtrackj.log("Deleting point "+id2string(hilipoint)+"...");
								hilipointtrack.delete(hilipoint);
								changed = true;
								mtrackj.logok();
								delete0size();
							}
							hilipoint = null;
							redraw();
						} else if (hilitrack != null) {
							boolean doit = true;
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Delete",mtrackj.window(),topleft(e));
								gd.addMessage("Are you sure you want to delete track "+id2string(hilitrack)+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								mtrackj.log("Deleting track "+id2string(hilitrack)+"...");
								hilitrack.cluster().delete(hilitrack);
								changed = true;
								mtrackj.logok();
								delete0size();
							}
							hilitrack = null;
							redraw();
						} else if (hilicluster != null) {
							boolean doit = true;
							if (mtrackj.settings().safemodify) {
								final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Delete",mtrackj.window(),topleft(e));
								gd.addMessage("Are you sure you want to delete cluster "+hilicluster.id()+"?");
								gd.showDialog(); if (gd.wasNo()) doit = false;
							}
							if (doit) {
								mtrackj.log("Deleting cluster "+hilicluster.id()+"...");
								activeassembly.delete(hilicluster);
								changed = true;
								mtrackj.logok();
								if (hilicluster == activecluster) {
									if (activeassembly.size() > 0)
										activecluster = activeassembly.first();
									else {
										activecluster = null;
										trackcolors.reset();
										clustercolors.reset();
									}
								}
							}
							hilicluster = null;
							redraw();
						}
						break;
					}
					case SETIDS: {
						if (hilipoint != null) {
							final MTJGenial gd = new MTJGenial(mtrackj.name()+": ID",mtrackj.window(),topleft(e));
							gd.addStringField("Point ID:",String.valueOf(hilipoint.id()));
							gd.showDialog();
							if (!gd.wasCanceled()) {
								try {
									final int id = Integer.parseInt(gd.getNextString());
									if (id < 1) throw new IllegalArgumentException();
									if (id != hilipoint.id()) {
										final MTJTrack hilipointtrack = hilipoint.track();
										final int nrpoints = hilipointtrack.size();
										for (int i=0; i<nrpoints; ++i) {
											final MTJPoint point = hilipointtrack.get(i);
											if (point.id() == id) {
												mtrackj.log("Renumbering point "+id2string(point)+" to "+id2string(hilipoint)+"...");
												point.id(hilipoint.id());
												mtrackj.logok();
												break;
											}
										}
										final String oldid = id2string(hilipoint);
										final String newid = id2string(hilipointtrack)+":"+id;
										mtrackj.log("Renumbering point "+oldid+" to "+newid+"...");
										hilipoint.id(id);
										hilipointtrack.resetlastpointid();
										changed = true;
										mtrackj.logok();
									} else mtrackj.log("Point "+id2string(hilipoint)+" already has the specified ID");
								} catch (Throwable anye) {
									mtrackj.error("The point ID should be an integer larger than zero");
								}
							}
							hilipoint = null;
							redraw();
						} else if (hilitrack != null) {
							final MTJGenial gd = new MTJGenial(mtrackj.name()+": ID",mtrackj.window(),topleft(e));
							gd.addStringField("Track ID:",String.valueOf(hilitrack.id()));
							gd.showDialog();
							if (!gd.wasCanceled()) {
								try {
									final int id = Integer.parseInt(gd.getNextString());
									if (id < 1) throw new IllegalArgumentException();
									if (id != hilitrack.id()) {
										final MTJCluster hilitrackcluster = hilitrack.cluster();
										final int nrtracks = hilitrackcluster.size();
										for (int i=0; i<nrtracks; ++i) {
											final MTJTrack track = hilitrackcluster.get(i);
											if (track.id() == id) {
												mtrackj.log("Renumbering track "+id2string(track)+" to "+id2string(hilitrack)+"...");
												track.id(hilitrack.id());
												mtrackj.logok();
												break;
											}
										}
										final String oldid = id2string(hilitrack);
										hilitrack.id(id); // Cluster numbering depends on assembly size:
										mtrackj.log("Renumbering track "+oldid+" to "+id2string(hilitrack)+"...");
										hilitrackcluster.resetlasttrackid();
										changed = true;
										mtrackj.logok();
									} else mtrackj.log("Track "+id2string(hilitrack)+" already has the specified ID");
								} catch (Throwable anye) {
									mtrackj.error("The track ID should be an integer larger than zero");
								}
							}
							hilitrack = null;
							redraw();
						} else if (hilicluster != null) {
							final MTJGenial gd = new MTJGenial(mtrackj.name()+": ID",mtrackj.window(),topleft(e));
							gd.addStringField("Cluster ID:",String.valueOf(hilicluster.id()));
							gd.showDialog();
							if (!gd.wasCanceled()) {
								try {
									final int id = Integer.parseInt(gd.getNextString());
									if (id < 1) throw new IllegalArgumentException();
									if (id != hilicluster.id()) {
										final int nrclusters = activeassembly.size();
										for (int i=0; i<nrclusters; ++i) {
											final MTJCluster cluster = activeassembly.get(i);
											if (cluster.id() == id) {
												mtrackj.log("Renumbering cluster "+cluster.id()+" to "+hilicluster.id()+"...");
												cluster.id(hilicluster.id());
												mtrackj.logok();
												break;
											}
										}
										mtrackj.log("Renumbering cluster "+hilicluster.id()+" to "+id+"...");
										hilicluster.id(id);
										activeassembly.resetlastclusterid();
										changed = true;
										mtrackj.logok();
									} else mtrackj.log("Cluster "+hilicluster.id()+" already has the specified ID");
								} catch (Throwable anye) {
									mtrackj.error("The cluster ID should be an integer larger than zero");
								}
							}
							hilicluster = null;
							redraw();
						}
						break;
					}
					case COLOR: {
						if (hiliassembly != null) {
							final MTJColors colors = new MTJColors(mtrackj,topleft(e));
							if (!colors.canceled()) {
								mtrackj.log("Assigning color to assembly...");
								activeassembly.color(colors.color());
								changed = true;
								mtrackj.logok();
							}
							hiliassembly = null;
							redraw();
						} else if (hilicluster != null) {
							final MTJColors colors = new MTJColors(mtrackj,topleft(e));
							if (!colors.canceled()) {
								mtrackj.log("Assigning color to cluster "+hilicluster.id()+"...");
								hilicluster.color(colors.color());
								clustercolors.used(colors.color());
								changed = true;
								mtrackj.logok();
							}
							hilicluster = null;
							redraw();
						} else if (hilitrack != null) {
							final MTJColors colors = new MTJColors(mtrackj,topleft(e));
							if (!colors.canceled()) {
								mtrackj.log("Assigning color to track "+id2string(hilitrack)+"...");
								hilitrack.color(colors.color());
								trackcolors.used(colors.color());
								changed = true;
								mtrackj.logok();
							}
							hilitrack = null;
							redraw();
						}
						break;
					}
					case REFER: {
						if (!mtrackj.settings().showreference) mtrackj.settings().showreference = true;
						mtrackj.log("Setting reference to "+spacecoordstring(curpos)+"...");
						activeassembly.reference(curpos.duplicate());
						changed = true;
						mtrackj.logok();
						redraw();
						break;
					}
				}
					break;
			}
			case Toolbar.MAGNIFIER: {
				if ((e.getModifiers() & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) zoomout(e.getX(),e.getY());
				else zoomin(e.getX(),e.getY());
				break;
			}
			case Toolbar.HAND: {
				scrlx = mtrackj.canvas().offScreenX(e.getX());
				scrly = mtrackj.canvas().offScreenY(e.getY());
				break;
			}
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	// The arguments are canvas (screen) coordinates:
	private void zoomout(final int cx, final int cy) {
		
		mtrackj.log("Zooming out...");
		mtrackj.canvas().zoomOut(cx,cy);
		getcoords(new MouseEvent(mtrackj.canvas(),0,0,0,cx,cy,0,false));
		mtrackj.logok();
	}
	
	// The arguments are canvas (screen) coordinates:
	private void zoomin(final int cx, final int cy) {
		
		mtrackj.log("Zooming in...");
		mtrackj.canvas().zoomIn(cx,cy);
		getcoords(new MouseEvent(mtrackj.canvas(),0,0,0,cx,cy,0,false));
		mtrackj.logok();
	}
	
	private Point topleft(final MouseEvent e) {
		
		final Point topleft = mtrackj.window().getLocation();
		topleft.x += mtrackj.canvas().getX() + e.getX() + 10;
		topleft.y += mtrackj.canvas().getY() + e.getY() + 10;
		return topleft;
	}
	
	public void mouseReleased(final MouseEvent e) { try {
		
		if (moving) {
			mtrackj.logok();
			boolean doit = true;
			boolean reset = false;
			
			final boolean movepoint = (hilipoint != null);
			final boolean movetrack = (hilitrack != null);
			final boolean movecluster = (hilicluster != null);
			
			String movestring = "";
			if (movepoint) movestring = "point "+id2string(hilipoint);
			else if (movetrack) movestring = "track "+id2string(hilitrack);
			else if (movecluster) movestring = "cluster "+hilicluster.id();
			
			if (mtrackj.settings().safemodify) {
				final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Move",mtrackj.window(),topleft(e));
				gd.addMessage("Are you sure you want to move "+movestring+" to the new position?");
				gd.showDialog(); if (gd.wasNo()) doit = false;
				reset = true;
			}
			if (doit) {
				mtrackj.log("Moved "+movestring+" to the new position");
				if (reset) {
					hilipoint = null;
					hilitrack = null;
					hilicluster = null;
					redraw();
				}
			} else {
				mtrackj.log("Moving "+movestring+" back to its original position...");
				if (movepoint) {
					hilipoint.coordinates(orgpos);
				} else if (movetrack) {
					final MTJPoint curorgpos = hilitrack.first();
					final double dx = curorgpos.x - orgpos.x;
					final double dy = curorgpos.y - orgpos.y;
					final double dz = curorgpos.z - orgpos.z;
					final int nrpoints = hilitrack.size();
					for (int p=0; p<nrpoints; ++p) {
						final MTJPoint point = hilitrack.get(p);
						point.x -= dx;
						point.y -= dy;
						point.z -= dz;
					}
				} else if (movecluster) {
					final MTJPoint curorgpos = hilicluster.first().first();
					final double dx = curorgpos.x - orgpos.x;
					final double dy = curorgpos.y - orgpos.y;
					final double dz = curorgpos.z - orgpos.z;
					final int nrtracks = hilicluster.size();
					for (int t=0; t<nrtracks; ++t) {
						final MTJTrack track = hilicluster.get(t);
						final int nrpoints = track.size();
						for (int p=0; p<nrpoints; ++p) {
							final MTJPoint point = track.get(p);
							point.x -= dx;
							point.y -= dy;
							point.z -= dz;
						}
					}
				}
				mtrackj.logok();
				changed = false;
				hilipoint = null;
				hilitrack = null;
				hilicluster = null;
				redraw();
			}
			moving = false;
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	private boolean moving = false;
	
	private void moveobject() {
		
		final boolean movepoint = (hilipoint != null);
		final boolean movetrack = (hilitrack != null);
		final boolean movecluster = (hilicluster != null);
		
		if (movepoint || movetrack || movecluster) {
			if (!moving) {
				moving = true;
				if (movepoint) mtrackj.log("Moving point "+id2string(hilipoint)+"...");
				else if (movetrack) mtrackj.log("Moving track "+id2string(hilitrack)+"...");
				else if (movecluster) mtrackj.log("Moving cluster "+hilicluster.id()+"...");
			}
			final double dx = curpos.x - movpos.x;
			final double dy = curpos.y - movpos.y;
			final double dz = curpos.z - movpos.z;
			if (dx != 0 || dy != 0 || dz != 0) {
				if (movepoint) {
					hilipoint.x += dx;
					hilipoint.y += dy;
					hilipoint.z += dz;
				} else if (movetrack) {
					final int nrpoints = hilitrack.size();
					for (int p=0; p<nrpoints; ++p) {
						final MTJPoint point = hilitrack.get(p);
						point.x += dx;
						point.y += dy;
						point.z += dz;
					}
				} else if (movecluster) {
					final int nrtracks = hilicluster.size();
					for (int t=0; t<nrtracks; ++t) {
						final MTJTrack track = hilicluster.get(t);
						final int nrpoints = track.size();
						for (int p=0; p<nrpoints; ++p) {
							final MTJPoint point = track.get(p);
							point.x += dx;
							point.y += dy;
							point.z += dz;
						}
					}
				}
				movpos.x += dx;
				movpos.y += dy;
				movpos.z += dz;
				changed = true;
				redraw();
			}
		}
	}
	
	public void mouseDragged(final MouseEvent e) { try {
		
		if (Toolbar.getToolId() != Toolbar.HAND) getcoords(e);
		
		switch (Toolbar.getToolId()) {
			case Toolbar.POINT: {
				if (mode == MOVE) moveobject();
				break;
			}
			case Toolbar.HAND: {
				final int ex = e.getX();
				final int ey = e.getY();
				scroll(ex,ey);
				break;
			}
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mouseMoved(final MouseEvent e) { try {
		
		getcoords(e);
		
		if (Toolbar.getToolId() == Toolbar.POINT) {
			
			switch (mode) {
				case ADD:
					if (e.isControlDown()) {
						findnearesttrack();
					} else if (hilitrack != null) {
						hilitrack = null;
						redraw = true;
					}
					if (snapping())
						redraw = true;
					break;
				case MOVE:
				case DELETE:
				case SETIDS:
					if (e.isControlDown())
						if (e.isShiftDown()) findnearestcluster();
						else findnearesttrack();
					else if (findnearestpoint())
						setcoords(hilipoint);
					break;
				case HIDE:
					if (e.isShiftDown()) findnearestcluster();
					else findnearesttrack();
					break;
				case MERGE:
				case CLUSTER:
					findnearesttrack();
					break;
				case SPLIT:
					if (!e.isControlDown() && findnearestpoint())
						setcoords(hilipoint);
					break;
				case COLOR:
					switch (mtrackj.settings().coloring) {
						case MTJSettings.MONOCHROME:
							findnearestassembly();
							break;
						case MTJSettings.PERCLUSTER:
							findnearestcluster();
							break;
						case MTJSettings.PERTRACK:
							findnearesttrack();
							break;
					}
					break;
			}
		}
		
		if (redraw) { redraw(); showcoords(); }
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mouseWheelMoved(final MouseWheelEvent e) { try {
		
		if (e.isShiftDown()) {
			if (e.getWheelRotation() > 0) stepslice(false);
			else if (e.getWheelRotation() < 0) stepslice(true);
			if (Toolbar.getToolId() == Toolbar.POINT && mode == MOVE) moveobject();
		} else if (e.isControlDown()) {
			if (e.getWheelRotation() > 0) stepchannel(false);
			else if (e.getWheelRotation() < 0) stepchannel(true);
		} else {
			if (e.getWheelRotation() > 0) steptime(false);
			else if (e.getWheelRotation() < 0) steptime(true);
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	private boolean findnearestassembly() {
		
		if (hilipoint != null) { hilipoint = null; redraw = true; }
		else if (hilitrack != null) { hilitrack = null; redraw = true; }
		else if (hilicluster != null) { hilicluster = null; redraw = true; }
		
		boolean found = false;
		double mindist = Double.MAX_VALUE;
		MTJAssembly minassembly = null;
		final double mag = mtrackj.canvas().getMagnification();
		final int nrclusters = activeassembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t) {
				final MTJTrack track = cluster.get(t);
				final double dist = distance(curpos,track,mag);
				if (dist < mindist) {
					mindist = dist;
					minassembly = activeassembly;
				}
			}
		}
		if (mindist <= MTJSettings.NEARBY_RANGE) {
			if (minassembly != hiliassembly) {
				mtrackj.log("Cursor nearby assembly");
				hiliassembly = minassembly;
				found = redraw = true;
			}
		} else if (hiliassembly != null) {
			hiliassembly = null;
			redraw = true;
		}
		
		return found;
	}
	
	private boolean findnearestcluster() {
		
		if (hilipoint != null) { hilipoint = null; redraw = true; }
		else if (hilitrack != null) { hilitrack = null; redraw = true; }
		else if (hiliassembly != null) { hiliassembly = null; redraw = true; }
		
		boolean found = false;
		double mindist = Double.MAX_VALUE;
		MTJCluster mincluster = null;
		final double mag = mtrackj.canvas().getMagnification();
		final int nrclusters = activeassembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t) {
				final MTJTrack track = cluster.get(t);
				final double dist = distance(curpos,track,mag);
				if (dist < mindist) {
					mindist = dist;
					mincluster = cluster;
				}
			}
		}
		if (mindist <= MTJSettings.NEARBY_RANGE) {
			if (mincluster != hilicluster) {
				mtrackj.log("Cursor nearby cluster "+mincluster.id());
				hilicluster = mincluster;
				found = redraw = true;
			}
		} else if (hilicluster != null) {
			hilicluster = null;
			redraw = true;
		}
		
		return found;
	}
	
	private boolean findnearesttrack() {
		
		if (hilipoint != null) { hilipoint = null; redraw = true; }
		else if (hilicluster != null) { hilicluster = null; redraw = true; }
		else if (hiliassembly != null) { hiliassembly = null; redraw = true; }
		
		boolean found = false;
		double mindist = Double.MAX_VALUE;
		MTJTrack mintrack = null;
		final double mag = mtrackj.canvas().getMagnification();
		final int nrclusters = activeassembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t) {
				final MTJTrack track = cluster.get(t);
				final double dist = distance(curpos,track,mag);
				if (dist < mindist) {
					mindist = dist;
					mintrack = track;
				}
			}
		}
		if (mindist <= MTJSettings.NEARBY_RANGE) {
			if (mintrack != hilitrack) {
				mtrackj.log("Cursor nearby track "+id2string(mintrack));
				hilitrack = mintrack;
				found = redraw = true;
			}
		} else if (hilitrack != null) {
			hilitrack = null;
			redraw = true;
		}
		
		return found;
	}
	
	private boolean findnearestpoint() {
		
		if (hilitrack != null) { hilitrack = null; redraw = true; }
		else if (hilicluster != null) { hilicluster = null; redraw = true; }
		else if (hiliassembly != null) { hiliassembly = null; redraw = true; }
		
		boolean found = false;
		double mindist = Double.MAX_VALUE;
		MTJPoint minpoint = null;
		final double mag = mtrackj.canvas().getMagnification();
		final int nrclusters = activeassembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t) {
				final MTJTrack track = cluster.get(t);
				final int nrpoints = track.size();
				for (int p=0; p<nrpoints; ++p) {
					final MTJPoint point = track.get(p);
					final double dist = distance(curpos,point,mag);
					if (dist < mindist) {
						mindist = dist;
						minpoint = point;
					}
				}
			}
		}
		if (mindist <= MTJSettings.NEARBY_RANGE) {
			if (minpoint != hilipoint) {
				mtrackj.log("Cursor nearby point "+id2string(minpoint)+" "+coordstring(minpoint));
				hilipoint = minpoint;
				found = redraw = true;
			}
		} else if (hilipoint != null) {
			hilipoint = null;
			redraw = true;
		}
		
		return found;
	}
	
	public void keyPressed(final KeyEvent e) { try {
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				mtrackj.dialog().dobutton(MTJDialog.ADD,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_B:
				mtrackj.dialog().dobutton(MTJDialog.SPLIT,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_C:
				mtrackj.dialog().dobutton(MTJDialog.COLOR,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_D:
				mtrackj.dialog().dobutton(MTJDialog.DELETE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_F:
				mtrackj.log("Putting all related windows to front...");
				final TextWindow logwindow = mtrackj.logwindow();
				final TextWindow pointswindow = mtrackj.pointswindow();
				final TextWindow trackswindow = mtrackj.trackswindow();
				final TextWindow clusterswindow = mtrackj.clusterswindow();
				final TextWindow assemblywindow = mtrackj.assemblywindow();
				if (logwindow != null && logwindow.isShowing()) logwindow.toFront();
				if (assemblywindow != null && assemblywindow.isShowing()) assemblywindow.toFront();
				if (clusterswindow != null && clusterswindow.isShowing()) clusterswindow.toFront();
				if (trackswindow != null && trackswindow.isShowing()) trackswindow.toFront();
				if (pointswindow != null && pointswindow.isShowing()) pointswindow.toFront();
				mtrackj.dialog().toFront();
				mtrackj.window().toFront();
				mtrackj.canvas().requestFocusInWindow();
				mtrackj.logok();
				break;
			case KeyEvent.VK_G:
				mtrackj.dialog().dobutton(MTJDialog.CLUSTER,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_H:
				mtrackj.dialog().dobutton(MTJDialog.HIDE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_I:
				mtrackj.dialog().dobutton(MTJDialog.IMPORT,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_J:
				mtrackj.dialog().dobutton(MTJDialog.MERGE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_L:
				mtrackj.dialog().dobutton(MTJDialog.SETIDS,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_M:
				mtrackj.dialog().dobutton(MTJDialog.MEASURE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_N:
				mtrackj.dialog().dobutton(MTJDialog.CLEAR,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_O:
				mtrackj.dialog().dobutton(MTJDialog.LOAD,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_P:
				mtrackj.dialog().dobutton(MTJDialog.MOVIE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_Q:
				mtrackj.dialog().quit();
				break;
			case KeyEvent.VK_R:
				mtrackj.dialog().dobutton(MTJDialog.REFER,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_S:
				mtrackj.dialog().dobutton(MTJDialog.SAVE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_V:
				mtrackj.dialog().dobutton(MTJDialog.MOVE,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_HOME:
				mtrackj.log("Moving to first time index...");
				mtrackj.setframe(1);
				mtrackj.logok();
				break;
			case KeyEvent.VK_END:
				mtrackj.log("Moving to last time index...");
				mtrackj.setframe(mtrackj.nrframes());
				mtrackj.logok();
				break;
			case KeyEvent.VK_ESCAPE:
				finishactivetrack();
				if (mtrackj.settings().resettime) resettime();
				else redraw();
				break;
			case KeyEvent.VK_F1:
				mtrackj.dialog().dobutton(MTJDialog.HELP,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_F2:
				mtrackj.dialog().dobutton(MTJDialog.TRACKING,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_F3:
				mtrackj.dialog().dobutton(MTJDialog.DISPLAYING,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_F4:
				mtrackj.dialog().dobutton(MTJDialog.PROGRAM,e.isAltDown(),e.isControlDown());
				break;
			case KeyEvent.VK_LEFT:
				if (e.isShiftDown()) stepslice(true);
				else if (e.isControlDown()) stepchannel(true);
				else steptime(true);
				break;
			case KeyEvent.VK_RIGHT:
				if (e.isShiftDown()) stepslice(false);
				else if (e.isControlDown()) stepchannel(false);
				else steptime(false);
				break;
			case KeyEvent.VK_SPACE:
				if (!spacedown) {
					spacedown = true;
					lastspacetool = Toolbar.getToolId();
					IJ.setTool(Toolbar.HAND);
					cursor(); redraw();
				}
				break;
			case KeyEvent.VK_CONTROL:
				if (!snapctrl) {
					snapctrl = true;
					redraw();
				}
				break;
			case KeyEvent.VK_ADD:
			case KeyEvent.VK_EQUALS: {
				final Rectangle vof = mtrackj.canvas().getSrcRect();
				final double mag = mtrackj.canvas().getMagnification();
				zoomin((int)((curpos.x - vof.x + 0.5)*mag),(int)((curpos.y - vof.y + 0.5)*mag));
				break;
			}
			case KeyEvent.VK_MINUS:
			case KeyEvent.VK_SUBTRACT: {
				final Rectangle vof = mtrackj.canvas().getSrcRect();
				final double mag = mtrackj.canvas().getMagnification();
				zoomout((int)((curpos.x - vof.x + 0.5)*mag),(int)((curpos.y - vof.y + 0.5)*mag));
				break;
			}
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	private int lastspacetool = Toolbar.MAGNIFIER;
	
	public void keyReleased(final KeyEvent e) { try {
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_CONTROL:
				snapctrl = false;
				getcoords(null); // In case snapping is on
				redraw();
				break;
			case KeyEvent.VK_SPACE:
				spacedown = false;
				IJ.setTool(lastspacetool);
				final ImageCanvas canvas = mtrackj.canvas();
				final java.awt.Rectangle vof = canvas.getSrcRect();
				final double mag = canvas.getMagnification();
				final int sx = (int)((curpos.x - vof.x + 0.5)*mag);
				final int sy = (int)((curpos.y - vof.y + 0.5)*mag);
				getcoords(new MouseEvent(canvas,0,0,0,sx,sy,0,false));
				cursor(); redraw();
				break;
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void keyTyped(final KeyEvent e) { }
	
	void redraw() { mtrackj.canvas().repaint(); redraw = false; }
	
	public void draw(final Graphics g) { try {
		
		// Check drawability:
		if (!(g instanceof Graphics2D)) return;
		final Graphics2D g2d = (Graphics2D)g;
		
		// Hide image if requested:
		final MTJSettings settings = mtrackj.settings();
		final ImageCanvas canvas = mtrackj.canvas();
		if (settings.bgcode != MTJSettings.BACKGROUND_IMAGE) {
			g2d.setColor(settings.bgcolor);
			g2d.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
		}
		
		// Initialize variables:
		g2d.setFont(settings.trackfont);
		g2d.setStroke(settings.trackstroke);
		try { g2d.setComposite(settings.trackopacity); } catch (Throwable e) { }
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		activeassembly.drawn(false);
		activeassembly.visible(false);
		final Rectangle vof = canvas.getSrcRect();
		final double mag = canvas.getMagnification();
		int st=1, ct=mtrackj.getframe(), et=mtrackj.nrframes();
		if (settings.visibility == MTJSettings.UP_TO_CURRENT) et = ct;
		else if (settings.visibility == MTJSettings.FROM_CURRENT) st = ct;
		
		// Draw finished tracks:
		if (settings.showfinishedtracks) {
			if (settings.coloring == MTJSettings.MONOCHROME)
				g2d.setColor(hiliassembly!=null?settings.hilicolor:activeassembly.color());
			final int nrclusters = activeassembly.size();
			for (int c=0; c<nrclusters; ++c) {
				final MTJCluster cluster = activeassembly.get(c);
				if (cluster != hilicluster) {
					if (settings.coloring == MTJSettings.PERCLUSTER) g2d.setColor(cluster.color());
					final int nrtracks = cluster.size();
					for (int t=0; t<nrtracks; ++t) {
						final MTJTrack track = cluster.get(t);
						if (track != activetrack && track != hilitrack && track != mergetrack) {
							if (settings.coloring == MTJSettings.PERTRACK) g2d.setColor(track.color());
							drawtrack(track,settings,g2d,vof,mag,ct,st,et);
						}
					}
				}
			}
			// Draw highlighted objects:
			g2d.setColor(settings.hilicolor);
			if (hilicluster != null) {
				final int nrtracks = hilicluster.size();
				for (int t=0; t<nrtracks; ++t)
					drawtrack(hilicluster.get(t),settings,g2d,vof,mag,ct,st,et);
			}
			if (hilitrack != null) {
				drawtrack(hilitrack,settings,g2d,vof,mag,ct,st,et);
			}
			if (mergetrack != null) {
				drawtrack(mergetrack,settings,g2d,vof,mag,ct,st,et);
			}
			if (hilipoint != null) {
				g2d.setStroke(settings.pointstroke);
				drawpoint(hilipoint,settings,g2d,vof,mag);
				if (settings.fontsize > 0)
					drawid(hilipoint,null,settings,g2d,vof,mag);
			}
		}
		
		// Draw active track:
		if (settings.showactivetrack && activetrack != null) {
			g2d.setColor(settings.hilicolor);
			drawtrack(activetrack,settings,g2d,vof,mag,ct,st,et);
		}
		
		// Draw reference:
		final MTJPoint reference = activeassembly.reference();
		if (settings.showreference && reference != null) {
			g2d.setColor(settings.hilicolor);
			g2d.setStroke(settings.pointstroke);
			drawpoint(reference,settings,g2d,vof,mag);
			drawid(reference,"R",settings,g2d,vof,mag);
		}
		
		// Draw snapping objects:
		if (snapping()) {
			g2d.setColor(settings.hilicolor);
			try { g2d.setComposite(settings.snapopacity); } catch (Throwable e) { }
			// Snap ROI:
			g2d.setStroke(settings.snapstroke);
			final int slx = (int)((snaprect.x - vof.x + 0.5)*mag);
			final int sly = (int)((snaprect.y - vof.y + 0.5)*mag);
			final int sux = (int)((snaprect.x + snaprect.width - vof.x - 0.5)*mag);
			final int suy = (int)((snaprect.y + snaprect.height - vof.y - 0.5)*mag);
			g2d.drawLine(slx,sly,sux,sly);
			g2d.drawLine(sux,sly,sux,suy);
			g2d.drawLine(sux,suy,slx,suy);
			g2d.drawLine(slx,suy,slx,sly);
			// Snap cursor:
			g2d.setStroke(settings.pointstroke);
			final int xi = (int)((snapos.x - vof.x + 0.5)*mag);
			final int yi = (int)((snapos.y - vof.y + 0.5)*mag);
			final int hps = 6;
			g2d.drawLine(xi,yi-hps,xi,yi+hps);
			g2d.drawLine(xi-hps,yi,xi+hps,yi);
		}
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	void drawassembly(final MTJAssembly assembly, final MTJSettings settings, final Graphics2D g2d, final Rectangle vof, final double mag, final int ct, final int st, final int et) {
		
		// Draw the given assembly as is:
		if (settings.coloring == MTJSettings.MONOCHROME) g2d.setColor(assembly.color());
		final int nrclusters = assembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = assembly.get(c);
			if (settings.coloring == MTJSettings.PERCLUSTER) g2d.setColor(cluster.color());
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t) {
				final MTJTrack track = cluster.get(t);
				if (settings.coloring == MTJSettings.PERTRACK) g2d.setColor(track.color());
				drawtrack(track,settings,g2d,vof,mag,ct,st,et);
			}
		}
		// Draw the given assembly's reference:
		final MTJPoint reference = assembly.reference();
		if (reference != null && settings.showreference) {
			g2d.setColor(settings.hilicolor);
			g2d.setStroke(settings.pointstroke);
			drawpoint(reference,settings,g2d,vof,mag);
			drawid(reference,"R",settings,g2d,vof,mag);
		}
	}
	
	private void drawtrack(final MTJTrack track, final MTJSettings settings, final Graphics2D g2d, final Rectangle vof, final double mag, final int ct, final int st, final int et) {
		
		if (track.hidden()) return;
		
		final int nrpoints = track.size();
		final int curc = mtrackj.getchannel();
		
		if (nrpoints > 0 &&
			!(settings.showonlytrackscurrentchannel && (track.first().c != curc)) &&
			!(settings.showonlytrackscurrenttime && (ct < track.first().t || ct > track.last().t))) {
			
			// Draw track lines:
			if (settings.trackwidth > 0) {
				g2d.setStroke(settings.trackstroke);
				final GeneralPath gp = new GeneralPath();
				boolean firstadd = true;
				MTJPoint pointim1 = track.first();
				boolean pim1visi = pointim1.t >= st && pointim1.t <= et;
				for (int i=1; i<nrpoints; ++i) {
					final MTJPoint pointi = track.get(i);
					final boolean pivisi = pointi.t >= st && pointi.t <= et;
					if (pim1visi && pivisi) {
						if (firstadd) {
							gp.moveTo((float)((pointim1.x - vof.x + 0.5)*mag),(float)((pointim1.y - vof.y + 0.5)*mag));
							gp.lineTo((float)((pointi.x - vof.x + 0.5)*mag),(float)((pointi.y - vof.y + 0.5)*mag));
							pointim1.visible(true); pointi.visible(true);
							firstadd = false;
						} else {
							gp.lineTo((float)((pointi.x - vof.x + 0.5)*mag),(float)((pointi.y - vof.y + 0.5)*mag));
							pointi.visible(true);
						}
					}
					pointim1 = pointi; pim1visi = pivisi;
				}
				g2d.draw(gp);
			}
			
			// Draw track points:
			if (settings.pointsize > 0) {
				g2d.setStroke(settings.pointstroke);
				if (settings.showonlypointscurrenttime) {
					for (int i=0; i<nrpoints; ++i) {
						final MTJPoint point = track.get(i);
						if (point.t == ct) { drawpoint(point,settings,g2d,vof,mag); break; }
					}
				} else {
					for (int i=0; i<nrpoints; ++i) {
						final MTJPoint point = track.get(i);
						if (point.t >= st && point.t <= et) drawpoint(point,settings,g2d,vof,mag);
					}
				}
			}
			
			// Draw track ID:
			if (settings.fontsize > 0) {
				drawid(track,null,settings,g2d,vof,mag);
			}
		}
	}
	
	// Minimum point size is square root of minimum point area:
	private final static double MPS = Math.sqrt(0.1);
	
	private int pointsize(final MTJPoint point, final MTJSettings settings) {
		
		int ps = settings.pointsize;
		
		switch (settings.pointcoding) {
			case MTJSettings.NOCODING: {
				break;
			}
			case MTJSettings.DEPTH_SIZE:
			case MTJSettings.DEPTH_BOTH: {
				double nrs = mtrackj.nrslices() - 1; if (nrs < 1) nrs = 1;
				ps = FMath.round(settings.pointsize*((1-MPS)*FMath.clip(1 - (point.z - 1)/nrs,0,1) + MPS));
				break;
			}
			case MTJSettings.DISTANCE_SIZE:
			case MTJSettings.DISTANCE_BOTH: {
				double nrs = mtrackj.nrslices() - 1; if (nrs < 1) nrs = 1;
				double depth = point.z - mtrackj.getslice(); if (depth < 0) depth = -depth;
				ps = FMath.round(settings.pointsize*((1-MPS)*FMath.clip(1 - depth/nrs,0,1) + MPS));
				break;
			}
		}
		
		return ps;
	}
	
	private Color pointcolor(final MTJPoint point, final Color color, final MTJSettings settings) {
		
		Color pc = color;
		if (pc == settings.hilicolor)
			return pc;
		
		switch (settings.pointcoding) {
			case MTJSettings.NOCODING: {
				break;
			}
			case MTJSettings.DEPTH_COLOR:
			case MTJSettings.DEPTH_BOTH: {
				double nrs = mtrackj.nrslices() - 1; if (nrs < 1) nrs = 1;
				pc = depthcolors[FMath.round(nrs*FMath.clip((point.z - 1)/nrs,0,1))];
				break;
			}
			case MTJSettings.DISTANCE_COLOR:
			case MTJSettings.DISTANCE_BOTH: {
				double nrs = mtrackj.nrslices() - 1; if (nrs < 1) nrs = 1;
				double dist = point.z - mtrackj.getslice(); if (dist < 0) dist = -dist;
				pc = depthcolors[FMath.round(nrs*FMath.clip(dist/nrs,0,1))];
				break;
			}
		}
		
		return pc;
	}
	
	private void drawpoint(final MTJPoint point, final MTJSettings settings, final Graphics2D g2d, final Rectangle vof, final double mag) {
		
		final Color color = g2d.getColor();
		g2d.setColor(pointcolor(point,color,settings));
		final int ps = pointsize(point,settings), hps = ps/2;
		final int xi = (int)((point.x - vof.x + 0.5)*mag);
		final int yi = (int)((point.y - vof.y + 0.5)*mag);
		g2d.drawLine(xi,yi,xi,yi);
		switch (settings.pointshape) {
			case MTJSettings.CIRCLE:
				if (settings.pointstyle == MTJSettings.OPEN) g2d.drawOval(xi-hps,yi-hps,ps,ps);
				else g2d.fillOval(xi-hps,yi-hps,ps,ps);
				break;
			case MTJSettings.SQUARE:
				if (settings.pointstyle == MTJSettings.OPEN) g2d.drawRect(xi-hps,yi-hps,ps,ps);
				else g2d.fillRect(xi-hps,yi-hps,ps,ps);
				break;
			case MTJSettings.TRIANGLE:
				final int[] trixi = { xi, xi+hps, xi-hps};
				final int[] triyi = { (int)(yi-1.33*hps), (int)(yi+0.66*hps), (int)(yi+0.66*hps)};
				if (settings.pointstyle == MTJSettings.OPEN) g2d.drawPolygon(trixi,triyi,3);
				else g2d.fillPolygon(trixi,triyi,3);
				break;
			case MTJSettings.CROSS:
				g2d.drawLine(xi-hps,yi-hps,xi+hps,yi+hps);
				g2d.drawLine(xi+hps,yi-hps,xi-hps,yi+hps);
				break;
		}
		g2d.setColor(color);
		point.visible(true);
		point.drawn(true);
	}
	
	private void drawid(final MTJTrack track, final String id, final MTJSettings settings, final Graphics2D g2d, final Rectangle vof, final double mag) {
		
		final int nrpoints = track.size();
		MTJPoint point0=null, point1=null, point2=null;
		switch (settings.tracklocus) {
			case MTJSettings.TAIL:
			case MTJSettings.HEAD: {
				int istart = 0, istep = 1;
				if (settings.tracklocus == MTJSettings.HEAD) {
					istart = nrpoints - 1; istep = -1;
				}
				for (int i=istart; i>=0 && i<nrpoints; i+=istep) {
					MTJPoint point = track.get(i);
					if (point.visible()) {
						point0 = point;
						i += istep;
						if (i>=0 && i<nrpoints) {
							point = track.get(i);
							if (point.visible())
								point1 = point;
						}
						break;
					}
				}
				break;
			}
			case MTJSettings.MIDDLE: {
				int ifirst = -1, ilast = -1;
				for (int i=0; i<nrpoints; ++i) {
					if (track.get(i).visible()) {
						if (ifirst < 0) ifirst = i;
						ilast = i;
					}
				}
				if (ifirst >= 0) {
					final int imiddle = (ifirst + ilast)/2;
					point0 = track.get(imiddle);
					if (ilast > ifirst) {
						final int inext = imiddle + 1;
						final int iprev = imiddle - 1;
						if (inext >= 0 && inext < nrpoints) point1 = track.get(inext);
						if (iprev >= 0 && iprev < nrpoints) point2 = track.get(iprev);
					}
				}
				break;
			}
		}
		if (point0 != null) {
			int ps = settings.trackwidth;
			final int point0size = pointsize(point0,settings);
			if (point0.drawn() && point0size > settings.trackwidth)
				ps = point0size;
			final int hps = (int)(0.5*ps + 0.1*settings.fontsize);
			int xpos = (int)((point0.x - vof.x + 0.5)*mag) + hps;
			int ypos = (int)((point0.y - vof.y + 0.5)*mag) - hps;
			int quad1 = 0, quad2 = 0;
			if (point1 != null) {
				final double dx = point1.x - point0.x;
				final double dy = point1.y - point0.y;
				if (dx > 0) quad1 = (dy > 0) ? 2 : 1;
				else quad1 = (dy > 0) ? 3 : 4;
			}
			if (point2 != null) {
				final double dx = point2.x - point0.x;
				final double dy = point2.y - point0.y;
				if (dx > 0) quad2 = (dy > 0) ? 2 : 1;
				else quad2 = (dy > 0) ? 3 : 4;
			}
			if (quad2 < quad1) {
				int tmp = quad1;
				quad1 = quad2;
				quad2 = tmp;
			}
			int quad = 1;
			if (quad == quad1) ++quad;
			if (quad == quad2) ++quad;
			final String idtxt = (id == null) ? id2string(track) : id;
			final int idwd = g2d.getFontMetrics().stringWidth(idtxt);
			final int idhg = (int)(0.8*settings.fontsize);
			final int flip = 2*hps;
			switch (quad) {
				case 1: break;
				case 2: ypos += idhg + flip; break;
				case 3: xpos -= idwd + flip; ypos += idhg + flip; break;
				case 4: xpos -= idwd + flip; break;
			}
			g2d.drawString(idtxt,xpos,ypos);
		}
	}
	
	private void drawid(final MTJPoint point, final String id, final MTJSettings settings, final Graphics2D g2d, final Rectangle vof, final double mag) {
		
		int ps = settings.trackwidth;
		final int pointsize = pointsize(point,settings);
		if (point.drawn() && pointsize > settings.trackwidth)
			ps = pointsize;
		final int hps = (int)(0.5*ps + 0.1*settings.fontsize);
		int xpos = (int)((point.x - vof.x + 0.5)*mag) + hps;
		int ypos = (int)((point.y - vof.y + 0.5)*mag) - hps;
		g2d.drawString(id==null?id2string(point):id,xpos,ypos);
	}
	
	private void setcoords(final MTJPoint coords) {
		
		if (coords.z >= 0.5 && coords.z < (mtrackj.nrslices()+0.5) &&
			coords.t >= 1 && coords.t <= mtrackj.nrframes() &&
			coords.c >= 1 && coords.c <= mtrackj.nrchannels())
			mtrackj.setsliders(coords.c,FMath.round(coords.z),coords.t);
	}
	
	private boolean snapping() {
		
		return (mode == ADD && !snapctrl && oncanvas && mtrackj.settings().snapping && Toolbar.getToolId() == Toolbar.POINT);
	}
	
	private static final int OTSU_BINS = 100;
	private Rectangle snaprect = new Rectangle();
	private double[][] snaproi = new double[1][1];
	
	private void snapcoords() {
		
		// Initialize:
		snapos.coordinates(curpos);
		final MTJSettings settings = mtrackj.settings();
		final int snaprange = settings.snaprange;
		if (snaproi.length != snaprange) snaproi = new double[snaprange][snaprange];
		final ImageProcessor ip = mtrackj.image().getProcessor();
		final int width = ip.getWidth();
		final int height = ip.getHeight();
		final int xr = FMath.round(curpos.x);
		final int yr = FMath.round(curpos.y);
		final int hsr = snaprange/2;
		snaprect.x = xr - hsr; if (snaprect.x < 0) snaprect.x = 0;
		snaprect.y = yr - hsr; if (snaprect.y < 0) snaprect.y = 0;
		int ux = xr + hsr; if (ux > (width-1)) ux = width-1;
		int uy = yr + hsr; if (uy > (height-1)) uy = height-1;
		snaprect.width = ux - snaprect.x + 1;
		snaprect.height = uy - snaprect.y + 1;
		
		// Extract intensities from snap ROI:
		final Object opixels = ip.getPixels();
		if (opixels instanceof byte[]) {
			final byte[] pixels = (byte[])opixels;
			for (int y=0, py=snaprect.y*width+snaprect.x; y<snaprect.height; ++y, py+=width)
				for (int x=0, pxy=py; x<snaprect.width; ++x, ++pxy)
					snaproi[y][x] = pixels[pxy]&0xFF;
			
		} else if (opixels instanceof short[]) {
			final short[] pixels = (short[])opixels;
			for (int y=0, py=snaprect.y*width+snaprect.x; y<snaprect.height; ++y, py+=width)
				for (int x=0, pxy=py; x<snaprect.width; ++x, ++pxy)
					snaproi[y][x] = pixels[pxy]&0xFFFF;
			
		} else if (opixels instanceof int[]) {
			final int[] pixels = (int[])opixels;
			for (int y=0, py=snaprect.y*width+snaprect.x; y<snaprect.height; ++y, py+=width)
				for (int x=0, pxy=py; x<snaprect.width; ++x, ++pxy) {
					final int val = pixels[pxy];
					snaproi[y][x] = (
						((val&0xFF0000)>>16)*settings.RED_WEIGHT +
						((val&0xFF00)>>8)*settings.GREEN_WEIGHT +
						(val&0xFF)*settings.BLUE_WEIGHT
					);
				}
		} else if (opixels instanceof float[]) {
			final float[] pixels = (float[])opixels;
			for (int y=0, py=snaprect.y*width+snaprect.x; y<snaprect.height; ++y, py+=width)
				for (int x=0, pxy=py; x<snaprect.width; ++x, ++pxy)
					snaproi[y][x] = pixels[pxy];
		}
		// Determine min and max value:
		double minval = snaproi[0][0];
		double maxval = minval;
		double minx=0, miny=0, maxx=0, maxy=0;
		for (int y=0; y<snaprect.height; ++y)
			for (int x=0; x<snaprect.width; ++x) {
				final double val = snaproi[y][x];
				if (val < minval) { minval = val; minx = x; miny = y; }
				else if (val > maxval) { maxval = val; maxx = x; maxy = y; }
			}
		if (minval == maxval) return; // snapos = curpos
		
		// Calculate snap coordinates:
		double ox=0, oy=0;
		switch (settings.snapfeature) {
			case MTJSettings.MAX_INTENSITY: {
				ox = maxx; oy = maxy;
				break;
			}
			case MTJSettings.MIN_INTENSITY: {
				ox = minx; oy = miny;
				break;
			}
			case MTJSettings.DARK_CENTROID: {
				// Invert and proceed with bright centroid:
				for (int y=0; y<snaprect.height; ++y)
					for (int x=0; x<snaprect.width; ++x)
					snaproi[y][x] = -snaproi[y][x];
					minval = -maxval;
					maxval = -minval;
			}
			case MTJSettings.BRIGHT_CENTROID: {
				// Make all weights > 0:
				if (minval <= 0) {
					final double offset = -minval + 1;
					for (int y=0; y<snaprect.height; ++y)
						for (int x=0; x<snaprect.width; ++x)
							snaproi[y][x] += offset;
					minval += offset;
					maxval += offset;
				}
				// Calculate Otsu threshold:
				double otsu = minval;
				final double maxi = OTSU_BINS;
				final double range = maxval - minval;
				double maxvari = -Double.MAX_VALUE;
				for (int i=1; i<OTSU_BINS; ++i) {
					double sum1=0, sum2=0, n1=0, n2=0;
					final double thres = minval + (i/maxi)*range;
					// Notice that we always have minval < thres < maxval,
					// so sum1, sum2, n1, n2 are > 0 after the loop:
					for (int y=0; y<snaprect.height; ++y)
						for (int x=0; x<snaprect.width; ++x) {
							final double val = snaproi[y][x];
							if (val < thres) { ++n1; sum1 += val; }
							else { ++n2; sum2 += val; }
						}
					final double mean1 = sum1/n1;
					final double mean2 = sum2/n2;
					final double vari = n1*n2*(mean1-mean2)*(mean1-mean2);
					if (vari > maxvari) {
						maxvari = vari;
						otsu = thres;
					}
				}
				// Calculate centroid >= threshold:
				double val=0, sum=0;
				for (int y=0; y<snaprect.height; ++y)
					for (int x=0; x<snaprect.width; ++x) {
						val = snaproi[y][x];
						if (val >= otsu) {
							val -= otsu;
							ox += x*val;
							oy += y*val;
							sum += val;
						}
					}
				ox /= sum; // sum can never be 0
				oy /= sum;
				break;
			}
		}
		snapos.x = snaprect.x + ox;
		snapos.y = snaprect.y + oy;
	}
	
	private void getcoords(final MouseEvent e) {
		
		// Get current cursor position:
		final Rectangle vof = mtrackj.canvas().getSrcRect();
		final double mag = mtrackj.canvas().getMagnification();
		final int width = mtrackj.getwidth();
		final int height = mtrackj.getheight();
		if (e != null) {
			curpos.x = vof.x + e.getX()/mag - (mag<=1 ? 0.0 : 0.5);
			curpos.y = vof.y + e.getY()/mag - (mag<=1 ? 0.0 : 0.5);
			if (curpos.x < 0) curpos.x = 0;
			else if (curpos.x > (width-1)) curpos.x = width-1;
			if (curpos.y < 0) curpos.y = 0;
			else if (curpos.y > (height-1)) curpos.y = height-1;
		}
		curpos.z = mtrackj.getslice();
		curpos.t = mtrackj.getframe();
		curpos.c = mtrackj.getchannel();
		
		// Calculate snapped cursor position:
		if (snapping()) snapcoords();
		
		// Show current cursor position:
		showcoords();
	}
	
	private void showcoords() {
		
		final StringBuffer sb = new StringBuffer();
		final Calibration cal = mtrackj.image().getCalibration();
		final boolean showcal = mtrackj.settings().showcalibrated;
		sb.append("x=" + (showcal ? mtrackj.d2s(curpos.x*cal.pixelWidth) : mtrackj.d2s(curpos.x)));
		sb.append(", y=" + (showcal ? mtrackj.d2s(curpos.y*cal.pixelHeight) : mtrackj.d2s(curpos.y)));
		if (mtrackj.doslices()) sb.append(", z=" + (showcal ? mtrackj.d2s((curpos.z-1)*cal.pixelDepth) : mtrackj.d2s(curpos.z)));
		if (mtrackj.doframes()) sb.append(", t=" + (showcal ? mtrackj.d2s((curpos.t-1)*cal.frameInterval) : mtrackj.d2s(curpos.t)));
		if (mtrackj.dochannels()) sb.append(", c=" + mtrackj.d2s(curpos.c));
		final ImageProcessor ip = mtrackj.image().getProcessor();
		ip.setCalibrationTable(showcal?cal.getCTable():null);
		sb.append(", I=" + mtrackj.d2s(mtrackj.value(ip,curpos.x,curpos.y)));
		if (mode == CLUSTER && activecluster != null) sb.append(", AC=" + activecluster.id());
		if (hilicluster != null) sb.append(", C=" + hilicluster.id());
		else if (hilitrack != null) sb.append(", C=" + hilitrack.cluster().id() + ", T=" + hilitrack.id());
		else if (hilipoint != null) sb.append(", C=" + hilipoint.track().cluster().id() + ", T=" + hilipoint.track().id() + ", P=" + hilipoint.id());
		mtrackj.status(sb.toString());
	}
	
	private String coordstring(final MTJPoint point) {
		
		final Calibration cal = mtrackj.image().getCalibration();
		final boolean showcal = mtrackj.settings().showcalibrated;
		return ("("+
			"x"+
			",y"+
			(mtrackj.doslices()?",z":"")+
			(mtrackj.doframes()?",t":"")+
			(mtrackj.dochannels()?",c":"")+
			") = ("+
			(showcal?mtrackj.d2s(point.x*cal.pixelWidth):mtrackj.d2s(point.x))+
			","+(showcal?mtrackj.d2s(point.y*cal.pixelHeight):mtrackj.d2s(point.y))+
			(mtrackj.doslices()?(","+(showcal?mtrackj.d2s((point.z-1)*cal.pixelDepth):mtrackj.d2s(point.z))):"")+
			(mtrackj.doframes()?(","+(showcal?mtrackj.d2s((point.t-1)*cal.frameInterval):mtrackj.d2s(point.t))):"")+
			(mtrackj.dochannels()?(","+mtrackj.d2s(point.c)):"")+
			")"
		);
	}
	
	private String spacecoordstring(final MTJPoint point) {
		
		final Calibration cal = mtrackj.image().getCalibration();
		final boolean showcal = mtrackj.settings().showcalibrated;
		return ("("+
			"x"+
			",y"+
			(mtrackj.doslices()?",z":"")+
			") = ("+
			(showcal?mtrackj.d2s(point.x*cal.pixelWidth):mtrackj.d2s(point.x))+
			","+(showcal?mtrackj.d2s(point.y*cal.pixelHeight):mtrackj.d2s(point.y))+
			(mtrackj.doslices()?(","+(showcal?mtrackj.d2s((point.z-1)*cal.pixelDepth):mtrackj.d2s(point.z))):"")+
			")"
		);
	}
	
	private void scroll(final int sx, final int sy) {
		
		final Rectangle vof = mtrackj.canvas().getSrcRect();
		final double mag = mtrackj.canvas().getMagnification();
		int newx = scrlx - (int)(sx/mag);
		int newy = scrly - (int)(sy/mag);
		if (newx < 0) newx = 0;
		if (newy < 0) newy = 0;
		final int width = mtrackj.getwidth();
		final int height = mtrackj.getheight();
		if ((newx + vof.width) > width) newx = width - vof.width;
		if ((newy + vof.height) > height) newy = height - vof.height;
		vof.x = newx;
		vof.y = newy;
		redraw();
	}
	
	private void resettime() {
		
		mtrackj.log("Resetting time to last start index ("+laststarttime+")...");
		mtrackj.setframe(laststarttime);
		mtrackj.logok();
		getcoords(null);
	}
	
	private void steptime(final boolean reverse) {
		
		final int size = mtrackj.nrframes();
		final int cur = mtrackj.getframe();
		final int step = mtrackj.settings().timestep;
		int next = cur + (reverse ? -step : step);
		if (next < 1) next = 1;
		else if (next > size) next = size;
		if (next != cur) {
			mtrackj.log("Moving to "+(reverse?"previous":"next")+" time index...");
			mtrackj.setframe(next);
			mtrackj.logok();
		}
		getcoords(null);
	}
	
	private void stepslice(final boolean reverse) {
		
		final int size = mtrackj.nrslices();
		final int cur = mtrackj.getslice();
		int next = cur + (reverse ? -1 : 1);
		if (next < 1) next = 1;
		else if (next > size) next = size;
		if (next != cur) {
			mtrackj.log("Moving to "+(reverse?"previous":"next")+" slice index...");
			mtrackj.setslice(next);
			mtrackj.logok();
		}
		getcoords(null);
	}
	
	private void stepchannel(final boolean reverse) {
		
		final int size = mtrackj.nrchannels();
		final int cur = mtrackj.getchannel();
		int next = cur + (reverse ? -1 : 1);
		if (next < 1) next = 1;
		else if (next > size) next = size;
		if (next != cur) {
			mtrackj.log("Moving to "+(reverse?"previous":"next")+" channel index...");
			mtrackj.setchannel(next);
			mtrackj.logok();
		}
		getcoords(null);
	}
	
	private void dehighlight() {
		
		hiliassembly = null;
		hilicluster = null;
		mergetrack = null;
		hilitrack = null;
		hilipoint = null;
	}
	
	private void delete0size() {
		
		for (int c=0; c<activeassembly.size(); ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			if (cluster.size() == 0) {
				mtrackj.log("Deleting zero-size cluster "+cluster.id()+"...");
				activeassembly.delete(c--);
				changed = true;
				mtrackj.logok();
				if (cluster == activecluster) {
					if (activeassembly.size() > 0)
						activecluster = activeassembly.first();
					else {
						activecluster = null;
						trackcolors.reset();
						clustercolors.reset();
					}
				}
			} else {
				for (int t=0; t<cluster.size(); ++t) {
					final MTJTrack track = cluster.get(t);
					if (track.size() == 0) {
						mtrackj.log("Deleting zero-size track "+id2string(track)+"...");
						cluster.delete(t--);
						changed = true;
						mtrackj.logok();
					}
				}
				if (cluster.size() == 0) --c;
			}
		}
	}
	
	private void initactivecluster() {
		
		mtrackj.log("Initiating new cluster...");
		activecluster = new MTJCluster();
		activecluster.color(clustercolors.next());
		activeassembly.add(activecluster);
		mtrackj.logok();
	}
	
	private void initactivetrack() {
		
		if (activecluster == null) initactivecluster();
		mtrackj.log("Initiating new track...");
		activetrack = new MTJTrack();
		activetrack.color(trackcolors.next());
		activecluster.add(activetrack);
		mtrackj.logok();
	}
	
	private void finishactivetrack() {
		
		if (activetrack != null) {
			mtrackj.log("Finishing track "+id2string(activetrack)+"...");
			activetrack = null;
			mtrackj.logok();
		}
	}
	
	void cleartracks() {
		
		finishactivetrack();
		mtrackj.log("Deleting all tracks...");
		activeassembly.reset();
		activecluster = null;
		dehighlight();
		clustercolors.reset();
		trackcolors.reset();
		changed = true;
		mtrackj.logok();
		mtrackj.settings().restore(MTJSettings.DISPLAYING);
		redraw();
	}
	
	void redoiding() {
		
		mtrackj.log("Renumbering all IDs...");
		activeassembly.redoclusteriding();
		final int nrclusters = activeassembly.size();
		for (int c=0; c<nrclusters; ++c) {
			final MTJCluster cluster = activeassembly.get(c);
			cluster.redotrackiding();
			final int nrtracks = cluster.size();
			for (int t=0; t<nrtracks; ++t)
				cluster.get(t).redopointiding();
		}
		changed = true;
		mtrackj.logok();
		redraw();
	}
	
	void unhidetracks(final boolean althide) {
		
		if (althide) {
			mtrackj.log("(Un)hiding tracks...");
			final int nrclusters = activeassembly.size();
			for (int c=0; c<nrclusters; ++c) {
				final MTJCluster cluster = activeassembly.get(c);
				final int nrtracks = cluster.size();
				for (int t=0; t<nrtracks; ++t) {
					final MTJTrack track = cluster.get(t);
					track.hidden(!track.hidden());
				}
			}
			mtrackj.logok();
		} else {
			mtrackj.log("Unhiding all tracks...");
			activeassembly.hidden(false);
			mtrackj.logok();
		}
		redraw();
	}
	
	MTJAssembly assembly() { delete0size(); return activeassembly; }
	
	void reassemble(final MTJAssembly assembly, final MTJSettings settings) {
		
		finishactivetrack();
		
		if (assembly != null) {
			mtrackj.log("Activating new assembly...");
			activeassembly = assembly;
			mtrackj.logok();
			delete0size();
			final int nrclusters = activeassembly.size();
			if (nrclusters == 0) activecluster = null;
			else activecluster = activeassembly.last();
			dehighlight();
			clustercolors.reset();
			trackcolors.reset();
			if (settings != null) {
				mtrackj.settings(settings);
				mtrackj.log("Registering loaded colors...");
				for (int c=0; c<nrclusters; ++c) {
					final MTJCluster cluster = activeassembly.get(c);
					clustercolors.used(cluster.color());
					final int nrtracks = cluster.size();
					for (int t=0; t<nrtracks; ++t)
						trackcolors.used(cluster.get(t).color());
				}
				mtrackj.logok();
			} else {
				mtrackj.settings(mtrackj.settings().duplicate());
				mtrackj.settings().restore(MTJSettings.DISPLAYING);
				mtrackj.log("Assigning colors to all entities...");
				for (int c=0; c<nrclusters; ++c) {
					final MTJCluster cluster = activeassembly.get(c);
					cluster.color(clustercolors.next());
					final int nrtracks = cluster.size();
					for (int t=0; t<nrtracks; ++t)
						cluster.get(t).color(trackcolors.next());
				}
				mtrackj.logok();
			}
			changed = false;
		}
		
		redraw();
	}
	
	void addclusters(final MTJAssembly assembly) {
		
		finishactivetrack();
		
		if (assembly != null) {
			final int nrclusters = assembly.size();
			mtrackj.log("Assigning new IDs to imported clusters...");
			for (int c=0; c<nrclusters; ++c)
				activeassembly.add(assembly.get(c));
			mtrackj.logok();
			delete0size();
			mtrackj.log("Assigning new colors to imported entities...");
			for (int c=0; c<nrclusters; ++c) {
				final MTJCluster cluster = assembly.get(c);
				cluster.color(clustercolors.next());
				final int nrtracks = cluster.size();
				for (int t=0; t<nrtracks; ++t)
					cluster.get(t).color(trackcolors.next());
			}
			mtrackj.logok();
			if (activeassembly.size() == 0) activecluster = null;
			else activecluster = activeassembly.last();
			changed = true;
		}
		
		redraw();
	}
	
	private double distance(final MTJPoint cp, final MTJTrack track, final double mag) {
		
		double mindist = Double.MAX_VALUE;
		
		// Minimum distance to track points:
		final int nrpoints = track.size();
		for (int i=0; i<nrpoints; ++i) {
			final double dist = distance(cp,track.get(i),mag);
			if (dist < mindist) mindist = dist;
		}
		
		// Minimum distance to track lines:
		if (nrpoints > 1) {
			MTJPoint pointim1 = track.first();
			for (int i=1; i<nrpoints; ++i) {
				final MTJPoint pointi = track.get(i);
				if (pointim1.visible() && pointi.visible()) {
					final double v12x = pointi.x - pointim1.x;
					final double v12y = pointi.y - pointim1.y;
					final double v13x = cp.x - pointim1.x;
					final double v13y = cp.y - pointim1.y;
					final double inprod = v12x*v13x + v12y*v13y;
					if (inprod >= 0) {
						final double v12len2 = v12x*v12x + v12y*v12y;
						if (inprod <= v12len2) {
							final double v13len2 = v13x*v13x + v13y*v13y;
							final double dist = Math.sqrt(v13len2 - inprod*inprod/v12len2) - 0.5*mtrackj.settings().trackwidth/mag;
							if (dist < mindist) mindist = dist;
						}
					}
				}
				pointim1 = pointi;
			}
		}
		
		return mindist;
	}
	
	private double distance(final MTJPoint cp, final MTJPoint point, final double mag) {
		
		double distance = Double.MAX_VALUE;
		
		if (point.visible()) {
			final double dx = cp.x - point.x;
			final double dy = cp.y - point.y;
			final int mtjps = pointsize(point,mtrackj.settings());
			final int mtjlw = mtrackj.settings().trackwidth;
			final double ps = (point.drawn() && mtjps > mtjlw) ? mtjps : mtjlw;
			distance = Math.sqrt(dx*dx + dy*dy) - 0.5*ps/mag;
		}
		
		return distance;
	}
	
	private String id2string(final MTJTrack track) {
		
		final String tid = ""+track.id();
		return (activeassembly.size()==1) ? tid : (track.cluster().id()+":"+tid);
	}
	
	private String id2string(final MTJPoint point) {
		
		return id2string(point.track())+":"+point.id();
	}
	
}

// *************************************************************************************************
final class MTJDialog extends PlugInFrame implements ActionListener, MouseListener, ComponentListener {
	
	private final MTrackJ mtrackj;
	
	private Button clearbutton, loadbutton, importbutton, savebutton;
	private Button addbutton, clusterbutton, hidebutton, mergebutton, splitbutton, movebutton, deletebutton, referbutton, colorbutton, setidsbutton;
	private Button measurebutton, moviebutton;
	private Button trackbutton, displaybutton, programbutton, helpbutton;
	
	final static int CLEAR=0, LOAD=1, IMPORT=2, SAVE=3;
	final static int ADD=10, CLUSTER=11, HIDE=12, MERGE=13, SPLIT=14, MOVE=15, DELETE=16, REFER=17, COLOR=18, SETIDS=19;
	final static int MEASURE=100, MOVIE=101;
	final static int TRACKING=1001, DISPLAYING=1002, PROGRAM=1003, HELP=1004;
	
	private final static Font dialogfont = new Font("Dialog",Font.PLAIN,11);
	private final GridBagConstraints gbc = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0);
	private final GridBagLayout gbl = new GridBagLayout();
	
	private String loaddir, importdir, savedir;
	private boolean measurepoints, measuretracks, measureclusters, measureassembly, measurealltracks;
	private boolean movietrim, moviedrop;
	private int measuredecimals;
	
	MTJDialog(final MTrackJ mtrackj) {
		
		super(mtrackj.name());
		this.mtrackj = mtrackj;
		this.addMouseListener(this);
		setLayout(gbl);
		setBackground(new Color(220,220,220));
		
		clearbutton = addButton("Clear");
		loadbutton = addButton("Load");
		importbutton = addButton("Import");
		savebutton = addButton("Save");
		newblock = true;
		addbutton = addButton("Add");
		clusterbutton = addButton("Cluster");
		hidebutton = addButton("Hide");
		colorbutton = addButton("Color");
		deletebutton = addButton("Delete");
		movebutton = addButton("Move");
		mergebutton = addButton("Merge");
		splitbutton = addButton("Split");
		referbutton = addButton("Refer");
		setidsbutton = addButton("ID");
		newblock = true;
		measurebutton = addButton("Measure");
		moviebutton = addButton("Movie");
		newblock = true;
		trackbutton = addButton("Tracking");
		displaybutton = addButton("Displaying");
		programbutton = addButton("Options");
		helpbutton = addButton("Help");
		
		pack();
		mtrackj.window().addComponentListener(this);
		initiateLocation();
		setVisible(true);
		
		restoreFolders();
		restoreMeasures();
		restoreMovies();
	}
	
	private final int space = 10;
	private boolean newblock = false;
	private boolean leftbutton = true;
	
	private Button addButton(final String label) {
		
		final Button b = new Button(label);
		b.setPreferredSize(new Dimension(86,22));
		b.setForeground(Color.black);
		b.setFont(dialogfont);
		b.addActionListener(this);
		b.addMouseListener(this);
		gbc.gridx = leftbutton ? 0 : 1;
		gbc.insets = new Insets(newblock?space:0,0,0,0);
		gbl.setConstraints(b,gbc);
		add(b);
		leftbutton = !leftbutton;
		if (leftbutton) {
			++gbc.gridy;
			newblock = false;
		}
		return b;
	}
	
	private void restoreFolders() {
		
		final String imgdir = IJ.getDirectory("image");
		
		if (mtrackj.settings().reusefolders) {
			loaddir = Prefs.get("mtj.loaddir",imgdir);
			importdir = Prefs.get("mtj.importdir",imgdir);
			savedir = Prefs.get("mtj.savedir",imgdir);
		} else {
			loaddir = importdir = savedir = imgdir;
		}
	}
	
	private void storeFolders() {
		
		if (loaddir != null) Prefs.set("mtj.loaddir",loaddir);
		if (importdir != null) Prefs.set("mtj.importdir",importdir);
		if (savedir != null) Prefs.set("mtj.savedir",savedir);
	}
	
	private void restoreMeasures() {
		
		measurepoints = Prefs.get("mtj.measurepoints",true);
		measuretracks = Prefs.get("mtj.measuretracks",true);
		measureclusters = Prefs.get("mtj.measureclusters",false);
		measureassembly = Prefs.get("mtj.measureassembly",false);
		measurealltracks = Prefs.get("mtj.measurealltracks",false);
		measuredecimals = (int)Prefs.get("mtj.measuredecimals",3);
		if (measuredecimals < 0) measuredecimals = 0;
		else if (measuredecimals > 10) measuredecimals = 10;
	}
	
	private void storeMeasures() {
		
		Prefs.set("mtj.measurepoints",measurepoints);
		Prefs.set("mtj.measuretracks",measuretracks);
		Prefs.set("mtj.measureclusters",measureclusters);
		Prefs.set("mtj.measureassembly",measureassembly);
		Prefs.set("mtj.measurealltracks",measurealltracks);
		Prefs.set("mtj.measuredecimals",measuredecimals);
	}
	
	private void restoreMovies() {
		
		movietrim = Prefs.get("mtj.movietrim",false);
		moviedrop = Prefs.get("mtj.moviedrop",false);
	}
	
	private void storeMovies() {
		
		Prefs.set("mtj.movietrim",movietrim);
		Prefs.set("mtj.moviedrop",moviedrop);
	}
	
	void resetbuttons() {
		
		final int mode = mtrackj.handler().mode();
		addbutton.setForeground(mode==MTJHandler.ADD?Color.red:Color.black);
		clusterbutton.setForeground(mode==MTJHandler.CLUSTER?Color.red:Color.black);
		hidebutton.setForeground(mode==MTJHandler.HIDE?Color.red:Color.black);
		mergebutton.setForeground(mode==MTJHandler.MERGE?Color.red:Color.black);
		splitbutton.setForeground(mode==MTJHandler.SPLIT?Color.red:Color.black);
		movebutton.setForeground(mode==MTJHandler.MOVE?Color.red:Color.black);
		deletebutton.setForeground(mode==MTJHandler.DELETE?Color.red:Color.black);
		referbutton.setForeground(mode==MTJHandler.REFER?Color.red:Color.black);
		colorbutton.setForeground(mode==MTJHandler.COLOR?Color.red:Color.black);
		setidsbutton.setForeground(mode==MTJHandler.SETIDS?Color.red:Color.black);
	}
	
	void dobutton(final int button, final boolean alt, final boolean ctrl) {
		
		if (mtrackj.locked()) return;
		
		switch (button) {
			
			case CLEAR: {
				boolean doit = true;
				if (mtrackj.settings().safemodify) {
					final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Clear",this);
					gd.addMessage("Are you sure you want to delete all tracks?");
					gd.showDialog(); if (gd.wasNo()) doit = false;
				}
				if (doit) {
					mtrackj.handler().cleartracks();
					mtrackj.status("Cleared tracks");
				} else mtrackj.copyright();
				break;
			}
			case LOAD: {
				if (mtrackj.handler().changed() && mtrackj.settings().savechanges) {
					if (saveconfirm()) {
						boolean canceled = false;
						final MTJAssembly assembly = mtrackj.handler().assembly();
						if (assembly.file() == null) {
							final String savepath = getsavepath();
							if (savepath == null) canceled = true;
							else assembly.file(savepath);
						}
						if (!canceled) {
							mtrackj.handler().mode(MTJHandler.NONE);
							final MTJWriter writer = new MTJWriter(mtrackj,assembly.file(),MTJWriter.SAVE);
							writer.start();
						}
					}
				}
				boolean dodialog = true;
				if (ctrl) dodialog = mtrackj.settings().input(MTJSettings.LOADING);
				if (dodialog) {
					final FileDialog fdg = new FileDialog(this,mtrackj.name()+": Load",FileDialog.LOAD);
					fdg.setDirectory(loaddir);
					fdg.setVisible(true);
					final String dir = fdg.getDirectory();
					final String file = fdg.getFile();
					fdg.dispose();
					if (dir != null && file != null) {
						loaddir = dir;
						if (!mtrackj.settings().separatefolders)
							importdir = savedir = dir;
						storeFolders();
						final MTJReader reader = new MTJReader(mtrackj,dir+file,MTJReader.LOAD);
						reader.start();
					}
				}
				break;
			}
			case IMPORT: {
				boolean dodialog = true;
				if (ctrl) dodialog = mtrackj.settings().input(MTJSettings.IMPORTING);
				if (dodialog) {
					final FileDialog fdg = new FileDialog(this,mtrackj.name()+": Import",FileDialog.LOAD);
					fdg.setDirectory(importdir);
					fdg.setVisible(true);
					final String dir = fdg.getDirectory();
					final String file = fdg.getFile();
					fdg.dispose();
					if (dir != null && file != null) {
						importdir = dir;
						if (!mtrackj.settings().separatefolders)
							loaddir = savedir = dir;
						storeFolders();
						final MTJReader reader = new MTJReader(mtrackj,dir+file,MTJReader.IMPORT);
						reader.start();
					}
				}
				break;
			}
			case SAVE: {
				boolean dodialog = true;
				if (ctrl) dodialog = mtrackj.settings().input(MTJSettings.SAVING);
				if (dodialog) {
					final MTJAssembly assembly = mtrackj.handler().assembly();
					boolean canceled = false;
					if (alt || assembly.file() == null) {
						final String savepath = getsavepath();
						if (savepath == null) canceled = true;
						else assembly.file(savepath);
					}
					if (!canceled) {
						mtrackj.handler().mode(MTJHandler.NONE);
						final MTJWriter writer = new MTJWriter(mtrackj,assembly.file(),MTJWriter.SAVE);
						writer.start();
					}
				}
				break;
			}
			case ADD: {
				if (mtrackj.handler().mode() == MTJHandler.ADD)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.ADD);
				break;
			}
			case CLUSTER: {
				if (mtrackj.handler().mode() == MTJHandler.CLUSTER)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.CLUSTER);
				break;
			}
			case HIDE: {
				if (ctrl) {
					boolean doit = true;
					if (mtrackj.settings().safemodify) {
						final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Unhide",this);
						if (alt) gd.addMessage("Are you sure you want to (un)hide all tracks?");
						else gd.addMessage("Are you sure you want to unhide all tracks?");
						gd.showDialog(); if (gd.wasNo()) doit = false;
					}
					if (doit) mtrackj.handler().unhidetracks(alt);
					else mtrackj.copyright();
				} else {
					if (mtrackj.handler().mode() == MTJHandler.HIDE)
						mtrackj.handler().mode(MTJHandler.NONE);
					else mtrackj.handler().mode(MTJHandler.HIDE);
				}
				break;
			}
			case MERGE: {
				if (mtrackj.handler().mode() == MTJHandler.MERGE)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.MERGE);
				break;
			}
			case SPLIT: {
				if (mtrackj.handler().mode() == MTJHandler.SPLIT)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.SPLIT);
				break;
			}
			case MOVE: {
				if (mtrackj.handler().mode() == MTJHandler.MOVE)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.MOVE);
				break;
			}
			case DELETE: {
				if (mtrackj.handler().mode() == MTJHandler.DELETE)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.DELETE);
				break;
			}
			case REFER: {
				if (mtrackj.handler().mode() == MTJHandler.REFER)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.REFER);
				break;
			}
			case COLOR: {
				if (mtrackj.handler().mode() == MTJHandler.COLOR)
					mtrackj.handler().mode(MTJHandler.NONE);
				else mtrackj.handler().mode(MTJHandler.COLOR);
				break;
			}
			case SETIDS: {
				if (ctrl) {
					boolean doit = true;
					if (mtrackj.settings().safemodify) {
						final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Renumber",this);
						gd.addMessage("Are you sure you want to renumber all IDs?");
						gd.showDialog(); if (gd.wasNo()) doit = false;
					}
					if (doit) mtrackj.handler().redoiding();
					else mtrackj.copyright();
				} else {
					if (mtrackj.handler().mode() == MTJHandler.SETIDS)
						mtrackj.handler().mode(MTJHandler.NONE);
					else mtrackj.handler().mode(MTJHandler.SETIDS);
				}
				break;
			}
			case MEASURE: {
				boolean canceled = false;
				if (ctrl) {
					final MTJGenial gd = new MTJGenial(mtrackj.name()+": Measure",this);
					gd.addCheckbox("Display point measurements",measurepoints);
					gd.addCheckbox("Display track measurements",measuretracks);
					gd.addCheckbox("Display cluster measurements",measureclusters);
					gd.addCheckbox("Display assembly measurements",measureassembly);
					gd.addCheckbox("Include all tracks in measurements",measurealltracks);
					final String[] decimals = new String[11]; for (int i=0; i<11; ++i) decimals[i] = String.valueOf(i);
					gd.addChoice("            Maximum decimal places:",decimals,String.valueOf(measuredecimals));
					gd.showDialog();
					if (!gd.wasCanceled()) {
						measurepoints = gd.getNextBoolean();
						measuretracks = gd.getNextBoolean();
						measureclusters = gd.getNextBoolean();
						measureassembly = gd.getNextBoolean();
						measurealltracks = gd.getNextBoolean();
						measuredecimals = gd.getNextChoiceIndex();
						storeMeasures();
					} else canceled = true;
				}
				if (!canceled) {
					final int measuremode = (
						(measurepoints ? MTJMeasurer.MEASURE_POINTS : 0) +
						(measuretracks ? MTJMeasurer.MEASURE_TRACKS : 0) +
						(measureclusters ? MTJMeasurer.MEASURE_CLUSTERS : 0) +
						(measureassembly ? MTJMeasurer.MEASURE_ASSEMBLY : 0) +
						(measurealltracks ? MTJMeasurer.MEASURE_ALL : 0)
					);
					final MTJMeasurer measurer = new MTJMeasurer(mtrackj,measuremode,measuredecimals);
					mtrackj.handler().mode(MTJHandler.NONE);
					mtrackj.log("Starting measurements...");
					measurer.start();
					mtrackj.logok();
				}
				break;
			}
			case MOVIE: {
				boolean canceled = false;
				if (ctrl) {
					final MTJGenial gd = new MTJGenial(mtrackj.name()+": Movie",this);
					gd.addCheckbox("Trim movie to all-first and all-last track points",movietrim);
					gd.addCheckbox("Drop frames for which there are no track points",moviedrop);
					gd.showDialog();
					if (!gd.wasCanceled()) {
						movietrim = gd.getNextBoolean();
						moviedrop = gd.getNextBoolean();
						storeMovies();
					} else canceled = true;
				}
				if (!canceled) {
					final int moviemode = (
						(movietrim ? MTJProducer.MOVIE_TRIM : 0) +
						(moviedrop ? MTJProducer.MOVIE_DROP : 0)
					);
					final MTJProducer moviemaker = new MTJProducer(mtrackj,moviemode);
					mtrackj.handler().mode(MTJHandler.NONE);
					mtrackj.log("Starting movie production...");
					moviemaker.start();
					mtrackj.logok();
				}
				break;
			}
			case TRACKING: {
				mtrackj.settings().input(MTJSettings.TRACKING);
				mtrackj.status("Configured tracking");
				break;
			}
			case DISPLAYING: {
				mtrackj.settings().input(MTJSettings.DISPLAYING);
				mtrackj.status("Configured displaying");
				break;
			}
			case PROGRAM: {
				mtrackj.settings().input(MTJSettings.PROGRAM);
				if (mtrackj.settings().reusefolders) restoreFolders();
				mtrackj.status("Configured program");
				break;
			}
			case HELP: {
				try {
					mtrackj.log("Opening default browser with online "+mtrackj.name()+" manual");
					BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/mtrackj/manual.html");
					mtrackj.status("Opened manual");
				} catch (Throwable anye) {
					mtrackj.error("Could not open default internet browser");
				}
				break;
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) { try {
		
		int button = HELP;
		final Object source = e.getSource();
		if (source == clearbutton) button = CLEAR;
		else if (source == loadbutton) button = LOAD;
		else if (source == importbutton) button = IMPORT;
		else if (source == savebutton) button = SAVE;
		else if (source == addbutton) button = ADD;
		else if (source == clusterbutton) button = CLUSTER;
		else if (source == hidebutton) button = HIDE;
		else if (source == mergebutton) button = MERGE;
		else if (source == splitbutton) button = SPLIT;
		else if (source == movebutton) button = MOVE;
		else if (source == deletebutton) button = DELETE;
		else if (source == referbutton) button = REFER;
		else if (source == colorbutton) button = COLOR;
		else if (source == setidsbutton) button = SETIDS;
		else if (source == measurebutton) button = MEASURE;
		else if (source == moviebutton) button = MOVIE;
		else if (source == trackbutton) button = TRACKING;
		else if (source == displaybutton) button = DISPLAYING;
		else if (source == programbutton) button = PROGRAM;
		else if (source == helpbutton) button = HELP;
		dobutton(button,altdown,ctrldown);
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	void quit() {
		
		boolean doit = true;
		if (mtrackj.settings().confirmquit) {
			final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Quit",this);
			gd.addMessage("Are you sure you want to quit?");
			gd.showDialog(); if (gd.wasNo()) doit = false;
		}
		if (doit) mtrackj.quit();
		else mtrackj.copyright();
	}
	
	boolean saveconfirm() {
		
		final MTJQuestion gd = new MTJQuestion(mtrackj.name()+": Save",this);
		gd.addMessage("Do you want to save the current tracks and settings?");
		gd.showDialog();
		if (gd.wasNo()) {
			mtrackj.copyright();
			return false;
		}
		return true;
	}
	
	String getsavepath() {
		
		final FileDialog fdg = new FileDialog(this,mtrackj.name()+": Save",FileDialog.SAVE);
		fdg.setDirectory(savedir);
		fdg.setFile(mtrackj.image().getTitle()+".mdf");
		fdg.setVisible(true);
		final String dir = fdg.getDirectory();
		final String file = fdg.getFile();
		fdg.dispose();
		if (dir != null && file != null) {
			savedir = dir;
			if (!mtrackj.settings().separatefolders)
				loaddir = importdir = dir;
			storeFolders();
			return dir+file;
		} else return null;
	}
	
	public void mouseClicked(final MouseEvent e) { }
	
	public void mouseEntered(final MouseEvent e) { mtrackj.copyright(); }
	
	public void mouseExited(final MouseEvent e) { mtrackj.copyright(); }
	
	private boolean altdown = false;
	private boolean ctrldown = false;
	
	public void mousePressed(final MouseEvent e) {
		
		altdown = e.isAltDown();
		ctrldown = e.isControlDown();
	}
	
	public void mouseReleased(final MouseEvent e) { }
	
	public void windowClosing(WindowEvent e) { quit(); }
	
	public void componentHidden(final ComponentEvent e) { }
	
	public void componentMoved(final ComponentEvent e) { try {
		
		updateLocation(e.getComponent());
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void componentResized(final ComponentEvent e) { try {
		
		updateLocation(e.getComponent());
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void componentShown(final ComponentEvent e) { }
	
	private final Point dialoc = new Point();
	private final Point oldwinloc = new Point();
	private final Point curwinloc = new Point();
	private final Dimension oldwindim = new Dimension();
	private final Dimension curwindim = new Dimension();
	
	private void initiateLocation() {
		
		mtrackj.window().getLocation(curwinloc);
		mtrackj.window().getSize(curwindim);
		
		final Dimension diadim = getSize();
		final Dimension scrdim = IJ.getScreenSize();
		final int rspx = curwinloc.x + curwindim.width; // right starting point x
		final int lspx = curwinloc.x - diadim.width; // left starting point x
		final double rvpx = ((rspx >= 0) ?
			FMath.clip(scrdim.width-rspx,0,diadim.width) :
			FMath.clip(diadim.width+rspx,0,diadim.width))/((double)diadim.width);
		final double lvpx = ((lspx >= 0) ?
			FMath.clip(scrdim.width-lspx,0,diadim.width) :
			FMath.clip(diadim.width+lspx,0,diadim.width))/((double)diadim.width);
		final double vpy = ((curwinloc.y >= 0) ?
			FMath.clip(scrdim.height-curwinloc.y,0,diadim.height) :
			FMath.clip(diadim.height+curwinloc.y,0,diadim.height))/((double)diadim.height);
		final double rvp = rvpx*vpy; // right visible percentage
		final double lvp = lvpx*vpy; // left visible percentage
		
		if (rvp >= lvp) {
			if (rvp >= 0.5) {
				dialoc.x = rspx;
				dialoc.y = curwinloc.y;
				setLocation(dialoc);
			} else GUI.center(this);
		} else {
			if (lvp >= 0.5) {
				dialoc.x = lspx;
				dialoc.y = curwinloc.y;
				setLocation(dialoc);
			} else GUI.center(this);
		}
		
		oldwinloc.setLocation(curwinloc);
		oldwindim.setSize(curwindim);
	}
	
	private void updateLocation(final Component c) {
		
		c.getLocation(curwinloc);
		c.getSize(curwindim);
		
		if (mtrackj.settings().movedialog) {
			getLocation(dialoc);
			final int dx = curwinloc.x - oldwinloc.x;
			final int dy = curwinloc.y - oldwinloc.y;
			final int dw = curwindim.width - oldwindim.width;
			final int dh = curwindim.height - oldwindim.height;
			if (dw != 0)
				if (dialoc.x >= (oldwinloc.x + oldwindim.width)) dialoc.x += dw;
				else if (dialoc.x > oldwinloc.x)
					dialoc.x = curwinloc.x + FMath.round(curwindim.getWidth()*(dialoc.getX() - oldwinloc.getX())/oldwindim.getWidth());
			if (dh != 0)
				if (dialoc.y >= (oldwinloc.y + oldwindim.height)) dialoc.y += dh;
				else if (dialoc.y > oldwinloc.y)
					dialoc.y = curwinloc.y + FMath.round(curwindim.getHeight()*(dialoc.getY() - oldwinloc.getY())/oldwindim.getHeight());
			dialoc.x += dx;
			dialoc.y += dy;
			setLocation(dialoc);
		}
		
		oldwinloc.setLocation(curwinloc);
		oldwindim.setSize(curwindim);
	}
	
}

// *************************************************************************************************
final class MTJColors extends Dialog implements WindowListener {
	
	private final MTrackJ mtrackj;
	
	private final GridBagConstraints gbc = new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
	private final GridBagLayout gbl = new GridBagLayout();
	
	MTJColors(final MTrackJ mtrackj, final Point topleft) {
		
		super(mtrackj.window(),mtrackj.name()+": Colors",true);
		this.mtrackj = mtrackj;
		
		setLayout(gbl);
		setBackground(new Color(220,220,220));
		final MTJSpectrum spectrum = new MTJSpectrum(mtrackj,this);
		gbl.setConstraints(spectrum,gbc);
		add(spectrum);
		
		pack();
		setLocation(topleft);
		addWindowListener(this);
		setVisible(true);
	}
	
	private boolean canceled = false;
	
	void canceled(boolean canceled) { this.canceled = canceled; }
	
	boolean canceled() { return canceled; }
	
	private Color color = Color.red;
	
	void color(final Color color) { this.color = color; }
	
	Color color() { return color; }
	
	void close() { setVisible(false); dispose(); mtrackj.copyright(); }
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) { }
	
	public void windowClosing(final WindowEvent e) { try {
		
		canceled = true;
		close();
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}

// *************************************************************************************************
final class MTJSpectrum extends Canvas implements MouseListener, MouseMotionListener, KeyListener {
	
	private final MTrackJ mtrackj;
	private final MTJColors colors;
	private final static Image im;
	private final static ColorProcessor cp;
	private final static int width, height;
	private int r=255, g=0, b=0;
	private String R="255", G="0", B="0";
	private boolean hexmode = false;
	private boolean oncanvas = false;
	
	static {
		final int binsize = 10;
		final int colors = 20;
		final int tints = 5;
		width = binsize*colors;
		height = binsize*(tints+1);
		final int[] pixels = new int[width*height];
		final Palette palette = new Palette(Palette.SPECTRUM);
		// Colors:
		for (int c=0, x0=0; c<colors; ++c, x0+=binsize)
			for (int t=0, y0=(tints-1)*binsize; t<tints; ++t, y0-=binsize) {
				final int color = palette.get(t+c*tints).getRGB();
				for (int y=0, yi=y0*width; y<binsize; ++y, yi+=width)
					for (int x=0, xyi=yi+x0; x<binsize; ++x, ++xyi)
						pixels[xyi] = color;
			}
		// Grays:
		final int y0 = tints*binsize;
		for (int c=0, x0=0; c<colors; ++c, x0+=binsize) {
			final float gray = c/(colors-1f);
			final int color = (new Color(gray,gray,gray)).getRGB();
			for (int y=0, yi=y0*width; y<binsize; ++y, yi+=width)
				for (int x=0, xyi=yi+x0; x<binsize; ++x, ++xyi)
					pixels[xyi] = color;
		}
		cp = new ColorProcessor(width,height,pixels);
		final MemoryImageSource source = new MemoryImageSource(width,height,ColorModel.getRGBdefault(),pixels,0,width);
		im = Toolkit.getDefaultToolkit().createImage(source);
	}
	
	MTJSpectrum(final MTrackJ mtrackj, final MTJColors colors) {
		
		this.mtrackj = mtrackj;
		this.colors = colors;
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setSize(width,height);
	}
	
	public void paint(final Graphics g) {
		
		g.drawImage(im,0,0,null);
		colors.toFront();
		requestFocusInWindow();
		setCursor(mtrackj.handler().cursor(MouseCursor.ARROW));
	}
	
	public void keyPressed(final KeyEvent e) { try {
		
		final int keycode = e.getKeyCode();
		if (keycode == KeyEvent.VK_CONTROL && hexmode == false) {
			hexmode = true;
			showRGB();
		} else if (keycode == KeyEvent.VK_ESCAPE) {
			colors.canceled(true);
			colors.close();
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void keyReleased(final KeyEvent e) { try {
		
		if (e.getKeyCode() == KeyEvent.VK_CONTROL && hexmode == true) {
			hexmode = false;
			showRGB();
		}
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void keyTyped(final KeyEvent e) { }
	
	public void mouseClicked(final MouseEvent e) { }
	
	public void mouseEntered(final MouseEvent e) { try {
		
		oncanvas = true;
		showRGB();
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mouseExited(final MouseEvent e) { try {
		
		oncanvas = false;
		showRGB();
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mousePressed(final MouseEvent e) { try {
		
		mtrackj.log("Picked color (R,G,B) = ("+R+","+G+","+B+")");
		colors.color(new Color(r,g,b));
		colors.close();
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	public void mouseReleased(final MouseEvent e) { }
	
	public void mouseDragged(final MouseEvent e) { }
	
	public void mouseMoved(final MouseEvent e) { try {
		
		final int value = cp.getPixel(e.getX(),e.getY());
		r = (value&0xFF0000)>>16;
		g = (value&0x00FF00)>>8;
		b = (value&0x0000FF);
		showRGB();
		
	} catch (Throwable x) { mtrackj.catcher().uncaughtException(Thread.currentThread(),x); } }
	
	private void showRGB() {
		
		if (oncanvas) {
			if (hexmode) {
				R = Integer.toHexString(r).toUpperCase();
				G = Integer.toHexString(g).toUpperCase();
				B = Integer.toHexString(b).toUpperCase();
			} else {
				R = String.valueOf(r);
				G = String.valueOf(g);
				B = String.valueOf(b);
			}
			mtrackj.status("Color=("+R+","+G+","+B+")");
		} else mtrackj.copyright();
	}
	
}

// *************************************************************************************************
final class MTJAssembly {
	
	private int cap = 100;
	private final int capinc = 100;
	private int size = 0;
	private MTJCluster[] clusters = new MTJCluster[cap];
	private int id = 1;
	private Color color = Color.red;
	private MTJPoint reference = null;
	private int lastclusterid = 0;
	private boolean clusteriding = true;
	private String file = null;
	private int locks = 0;
	
	MTJAssembly duplicate() {
		
		final MTJAssembly assembly = new MTJAssembly();
		assembly.cap = assembly.size = size;
		assembly.clusters = new MTJCluster[size];
		for (int i=0; i<size; ++i) {
			final MTJCluster cluster = clusters[i].duplicate();
			assembly.clusters[i] = cluster;
			cluster.assembly(assembly);
		}
		assembly.id = id;
		assembly.color = color;
		assembly.reference = (reference == null) ? null : reference.duplicate();
		assembly.lastclusterid = lastclusterid;
		assembly.clusteriding = clusteriding;
		assembly.file = file;
		assembly.locks = locks;
		return assembly;
	}
	
	void id(final int id) { this.id = id; }
	
	int id() { return id; }
	
	void color(final Color color) { this.color = color; }
	
	Color color() { return color; }
	
	void reference(final MTJPoint reference) { this.reference = reference; }
	
	MTJPoint reference() { return reference; }
	
	void clusteriding(final boolean clusteriding) { this.clusteriding = clusteriding; }
	
	void file(final String file) { this.file = file; }
	
	String file() { return file; }
	
	void lock() { ++locks; }
	
	void unlock() { --locks; if (locks < 0) locks = 0; }
	
	boolean locked() { return (locks > 0); }
	
	void drawn(final boolean drawn) { for (int i=0; i<size; ++i) clusters[i].drawn(drawn); }
	
	void hidden(final boolean hidden) { for (int i=0; i<size; ++i) clusters[i].hidden(hidden); }
	
	void visible(final boolean visible) { for (int i=0; i<size; ++i) clusters[i].visible(visible); }
	
	void redoclusteriding() {
		
		for (int i=0; i<size; ++i) clusters[i].id(i+1);
		lastclusterid = size;
	}
	
	void resetlastclusterid() {
		
		lastclusterid = 0;
		for (int i=0; i<size; ++i)
			if (lastclusterid < clusters[i].id())
				lastclusterid = clusters[i].id();
	}
	
	private void inccap() {
		
		cap += capinc;
		final MTJCluster[] newclusters = new MTJCluster[cap];
		for (int i=0; i<size; ++i) newclusters[i] = clusters[i];
		clusters = newclusters;
	}
	
	void add(final MTJCluster cluster) {
		
		if (size == cap) inccap();
		clusters[size++] = cluster;
		cluster.assembly(this);
		if (clusteriding) cluster.id(++lastclusterid);
		else if (lastclusterid < cluster.id()) lastclusterid = cluster.id();
	}
	
	MTJCluster first() {
		
		if (size > 0) return clusters[0];
		return null;
	}
	
	MTJCluster last() {
		
		if (size > 0) return clusters[size-1];
		return null;
	}
	
	MTJCluster get(final int index) {
		
		if (index >= 0 && index < size)
			return clusters[index];
		return null;
	}
	
	int index(final MTJCluster cluster) {
		
		int index = -1;
		for (int i=0; i<size; ++i)
			if (clusters[i] == cluster) {
				index = i;
				break;
			}
		return index;
	}
	
	void delete(final int index) {
		
		if (index >= 0 && index < size) {
			for (int i1=index, i2=index+1; i2<size; ++i1, ++i2)
				clusters[i1] = clusters[i2];
			--size; resetlastclusterid();
		}
	}
	
	void delete(final MTJCluster cluster) {
		
		for (int i=0; i<size; ++i)
			if (clusters[i] == cluster) {
				delete(i);
				break;
			}
	}
	
	void reset() {
		
		if (size > 0) {
			size = 0;
			lastclusterid = 0;
		}
		reference = null;
		file = null;
	}
	
	int size() { return size; }
	
}

// *************************************************************************************************
final class MTJCluster {
	
	private int cap = 100;
	private final int capinc = 100;
	private int size = 0;
	private MTJTrack[] tracks = new MTJTrack[cap];
	
	private int id = 0;
	private Color color = Color.red;
	private MTJAssembly assembly = null;
	
	private int lasttrackid = 0;
	private boolean trackiding = true;
	
	MTJCluster duplicate() {
		
		final MTJCluster cluster = new MTJCluster();
		cluster.cap = cluster.size = size;
		cluster.tracks = new MTJTrack[size];
		for (int i=0; i<size; ++i) {
			final MTJTrack track = tracks[i].duplicate();
			cluster.tracks[i] = track;
			track.cluster(cluster);
		}
		cluster.id = id;
		cluster.color = color;
		cluster.assembly = assembly;
		cluster.lasttrackid = lasttrackid;
		cluster.trackiding = trackiding;
		return cluster;
	}
	
	void id(final int id) { this.id = id; }
	
	int id() { return id; }
	
	void color(final Color color) { this.color = color; }
	
	Color color() { return color; }
	
	void assembly(final MTJAssembly assembly) { this.assembly = assembly; }
	
	MTJAssembly assembly() { return assembly; }
	
	void drawn(final boolean drawn) { for (int i=0; i<size; ++i) tracks[i].drawn(drawn); }
	
	void hidden(final boolean hidden) { for (int i=0; i<size; ++i) tracks[i].hidden(hidden); }
	
	void visible(final boolean visible) { for (int i=0; i<size; ++i) tracks[i].visible(visible); }
	
	void trackiding(final boolean trackiding) { this.trackiding = trackiding; }
	
	void redotrackiding() {
		
		for (int i=0; i<size; ++i) tracks[i].id(i+1);
		lasttrackid = size;
	}
	
	void resetlasttrackid() {
		
		lasttrackid = 0;
		for (int i=0; i<size; ++i)
			if (lasttrackid < tracks[i].id())
				lasttrackid = tracks[i].id();
	}
	
	private void inccap() {
		
		cap += capinc;
		final MTJTrack[] newtracks = new MTJTrack[cap];
		for (int i=0; i<size; ++i) newtracks[i] = tracks[i];
		tracks = newtracks;
	}
	
	void add(final MTJTrack track) {
		
		if (size == cap) inccap();
		tracks[size++] = track;
		track.cluster(this);
		if (trackiding) track.id(++lasttrackid);
		else if (lasttrackid < track.id()) lasttrackid = track.id();
	}
	
	MTJTrack first() {
		
		if (size > 0) return tracks[0];
		return null;
	}
	
	MTJTrack last() {
		
		if (size > 0) return tracks[size-1];
		return null;
	}
	
	MTJTrack get(final int index) {
		
		if (index >= 0 && index < size)
			return tracks[index];
		return null;
	}
	
	int index(final MTJTrack track) {
		
		int index = -1;
		for (int i=0; i<size; ++i)
			if (tracks[i] == track) {
				index = i;
				break;
			}
		return index;
	}
	
	void delete(final int index) {
		
		if (index >= 0 && index < size) {
			for (int i1=index, i2=index+1; i2<size; ++i1, ++i2)
				tracks[i1] = tracks[i2];
			--size; resetlasttrackid();
		}
	}
	
	void delete(final MTJTrack track) {
		
		for (int i=0; i<size; ++i)
			if (tracks[i] == track) {
				delete(i);
				break;
			}
	}
	
	int size() { return size; }
	
}

// *************************************************************************************************
final class MTJTrack {
	
	private int cap = 100;
	private final int capinc = 100;
	private int size = 0;
	private MTJPoint[] points = new MTJPoint[cap];
	
	private int id = 0;
	private boolean hidden = false;
	private Color color = Color.red;
	private MTJCluster cluster = null;
	
	private int lastpointid = 0;
	private boolean pointiding = true;
	
	MTJTrack duplicate() {
		
		final MTJTrack track = new MTJTrack();
		track.cap = track.size = size;
		track.points = new MTJPoint[size];
		for (int i=0; i<size; ++i) {
			final MTJPoint point = points[i].duplicate();
			track.points[i] = point;
			point.track(track);
		}
		track.id = id;
		track.color = color;
		track.cluster = cluster;
		track.lastpointid = lastpointid;
		track.pointiding = pointiding;
		return track;
	}
	
	void id(final int id) { this.id = id; }
	
	int id() { return id; }
	
	void color(final Color color) { this.color = color; }
	
	Color color() { return color; }
	
	void cluster(final MTJCluster cluster) { this.cluster = cluster; }
	
	MTJCluster cluster() { return cluster; }
	
	boolean hidden() { return hidden; }
	
	void hidden(final boolean hidden) { this.hidden = hidden; }
	
	void drawn(final boolean drawn) { for (int i=0; i<size; ++i) points[i].drawn(drawn); }
	
	void visible(final boolean visible) { for (int i=0; i<size; ++i) points[i].visible(visible); }
	
	void pointiding(final boolean pointiding) { this.pointiding = pointiding; }
	
	void redopointiding() {
		
		for (int i=0; i<size; ++i) points[i].id(i+1);
		lastpointid = size;
	}
	
	void resetlastpointid() {
		
		lastpointid = 0;
		for (int i=0; i<size; ++i)
			if (lastpointid < points[i].id())
				lastpointid = points[i].id();
	}
	
	private void inccap() {
		
		cap += capinc;
		final MTJPoint[] newpoints = new MTJPoint[cap];
		for (int i=0; i<size; ++i) newpoints[i] = points[i];
		points = newpoints;
	}
	
	void add(final MTJPoint point) {
		
		if (size > 0) point.c = points[0].c;
		boolean insert = true;
		int i; for (i=0; i<size; ++i)
			if (point.t == points[i].t) { insert = false; break; }
			else if (point.t < points[i].t) { insert = true; break; }
		if (insert) {
			if (size == cap) inccap();
			for (int j=size++; j>i; --j)
				points[j] = points[j-1];
		}
		points[i] = point; point.track(this);
		if (pointiding) point.id(++lastpointid);
		else if (lastpointid < point.id()) lastpointid = point.id();
	}
	
	MTJPoint first() {
		
		if (size > 0) return points[0];
		return null;
	}
	
	MTJPoint last() {
		
		if (size > 0) return points[size-1];
		return null;
	}
	
	MTJPoint get(final int index) {
		
		if (index >= 0 && index < size)
			return points[index];
		return null;
	}
	
	int index(final MTJPoint point) {
		
		int index = -1;
		for (int i=0; i<size; ++i)
			if (points[i] == point) {
				index = i;
				break;
			}
		return index;
	}
	
	void delete(final int index) {
		
		if (index >= 0 && index < size) {
			for (int i=index+1; i<size; ++i)
				points[i-1] = points[i];
			--size; resetlastpointid();
		}
	}
	
	void delete(final MTJPoint point) {
		
		for (int i=0; i<size; ++i)
			if (points[i] == point) {
				delete(i);
				break;
			}
	}
	
	void cut(final int index) {
		
		if (index < size) {
			size = index;
			if (size < 0) size = 0;
				resetlastpointid();
		}
	}
	
	int size() { return size; }
	
}

// *************************************************************************************************
final class MTJPoint {
	
	double x=0, y=0, z=1;
	int t=1, c=1;
	
	private int id = 0;
	private MTJTrack track = null;
	private boolean visible = false;
	private boolean drawn = false;
	
	MTJPoint duplicate() {
		
		final MTJPoint point = new MTJPoint();
		point.x = x;
		point.y = y;
		point.z = z;
		point.t = t;
		point.c = c;
		point.id = id;
		point.track = track;
		point.visible = visible;
		point.drawn = drawn;
		return point;
	}
	
	void id(final int id) { this.id = id; }
	
	int id() { return id; }
	
	void track(final MTJTrack track) { this.track = track; }
	
	MTJTrack track() { return track; }
	
	void drawn(final boolean drawn) { this.drawn = drawn; }
	
	boolean drawn() { return drawn; }
	
	void visible(final boolean visible) { this.visible = visible; }
	
	boolean visible() { return visible; }
	
	void coordinates(final MTJPoint point) {
		
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		this.t = point.t;
		this.c = point.c;
	}
	
}

// *************************************************************************************************
final class MTJProducer extends Thread {
	
	final static int MOVIE_TRIM=1, MOVIE_DROP=2;
	
	private final MTrackJ mtrackj;
	private final int mode;
	
	MTJProducer(final MTrackJ mtrackj, final int mode) {
		
		this.mtrackj = mtrackj;
		this.mode = mode;
		try { this.setUncaughtExceptionHandler(mtrackj.catcher()); }
		catch (final Throwable e) { }
	}
	
	public void run() {
		
		final MTJAssembly assembly = mtrackj.handler().assembly();
		final MTJSettings settings = mtrackj.settings();
		assembly.lock();
		
		mtrackj.status("Producing movie...");
		final Progressor pgs = new Progressor();
		pgs.display(true);
		
		try {
			// Initialize values:
			final boolean movietrim = (mode & MOVIE_TRIM) > 0;
			final boolean moviedrop = (mode & MOVIE_DROP) > 0;
			final ImageCanvas canvas = mtrackj.canvas();
			final Rectangle vof = new Rectangle(canvas.getSrcRect());
			final double mag = canvas.getMagnification();
			final int width = canvas.getWidth();
			final int height = canvas.getHeight();
			final Frame f = new Frame(); f.pack();
			final Image frame = f.createImage(width,height);
			if (!(frame.getGraphics() instanceof Graphics2D)) {
				mtrackj.error("Could not initialize required graphics object for producing movie");
				return;
			}
			int z = mtrackj.getslice();
			int c = mtrackj.getchannel();
			boolean single5d = false;
			MTJTrack track5d = null;
			final int zdim = mtrackj.nrslices();
			final int tdim = mtrackj.nrframes();
			final int cdim = mtrackj.nrchannels();
			
			// Check number of tracks:
			if (assembly.size() == 0) {
				mtrackj.log("No tracks");
				if (cdim > 1) mtrackj.log("Using current channel value");
				if (zdim > 1) mtrackj.log("Using current slice value");
			} else if (assembly.size() == 1 && assembly.first().size() == 1) {
				mtrackj.log("Single track");
				if (zdim > 1 || cdim > 1) {
					single5d = true;
					track5d = assembly.first().first();
					if (cdim > 1) {
						mtrackj.log("Using track channel value");
						c = track5d.first().c;
						if (c < 1) { mtrackj.log("Clipping to first channel"); c = 1; }
						else if (c > cdim) { mtrackj.log("Clipping to last channel"); c = cdim; }
						if (mtrackj.getchannel() != c) {
							mtrackj.log("Changing current channel to track channel");
							mtrackj.setchannel(c);
						}
					}
					if (zdim > 1) mtrackj.log("Using point slice value per time frame");
				}
			} else {
				mtrackj.log("Multiple tracks");
				if (cdim > 1) mtrackj.log("Using current channel value");
				if (zdim > 1) mtrackj.log("Using current slice value");
			}
			
			// Draw frames and tracks:
			final ImageStack movie = new ImageStack(width,height);
			final ImageStack stack = mtrackj.image().getImageStack();
			final ColorModel colormodel = mtrackj.colormodel(c);
			final double minshown = mtrackj.minshown(c);
			final double maxshown = mtrackj.maxshown(c);
			
			int tfirst = 1;
			int tlast = tdim;
			int nrframes = 0;
			int drawstarttime = 1;
			int drawendtime = tdim;
			boolean validrange = true;
			
			if (movietrim) {
				tfirst = Integer.MAX_VALUE;
				tlast = Integer.MIN_VALUE;
				final int nrclusters = assembly.size();
				for (int ci=0; ci<nrclusters; ++ci) {
					final MTJCluster cluster = assembly.get(ci);
					final int nrtracks = cluster.size();
					for (int ti=0; ti<nrtracks; ++ti) {
						final MTJTrack track = cluster.get(ti);
						if (!track.hidden() && !(settings.showonlytrackscurrentchannel && track.first().c != c)) {
							final int ft = track.first().t;
							if (ft < tfirst) tfirst = ft;
							final int lt = track.last().t;
							if (lt > tlast) tlast = lt;
						}
					}
				}
				if (tlast < tfirst || tfirst > tdim || tlast < 1) {
					validrange = false;
				} else { // Clip to valid range:
					if (tfirst < 1) tfirst = 1;
					if (tlast > tdim) tlast = tdim;
				}
			}
			
			if (validrange) {
				pgs.steps(tlast - tfirst + 1); pgs.start();
				timeloop: for (int t=tfirst; t<=tlast; ++t, pgs.step()) {
					if (moviedrop) {
						boolean drop = true;
						final int nrclusters = assembly.size();
						searchloop: for (int ci=0; ci<nrclusters; ++ci) {
							final MTJCluster cluster = assembly.get(ci);
							final int nrtracks = cluster.size();
							for (int ti=0; ti<nrtracks; ++ti) {
								final MTJTrack track = cluster.get(ti);
								if (!track.hidden() && !(settings.showonlytrackscurrentchannel && track.first().c != c)) {
									if (track.first().t <= t && t <= track.last().t) {
										final int nrpoints = track.size();
										for (int pi=0; pi<nrpoints; ++pi) {
											if (track.get(pi).t == t) {
												drop = false;
												break searchloop;
											}
										}
									}
								}
							}
						}
						if (drop) continue timeloop;
					}
					final Graphics2D g2d = (Graphics2D)frame.getGraphics();
					if (settings.bgcode == MTJSettings.BACKGROUND_IMAGE) {
						if (single5d) {
							final int nrpoints = track5d.size();
							for (int i=0; i<nrpoints; ++i) {
								final MTJPoint point = track5d.get(i);
								if (point.t == t) {
									z = FMath.round(point.z);
									if (z < 1) z = 1;
									else if (z > zdim) z = zdim;
									break;
								}
							}
						}
						final ImageProcessor tp = stack.getProcessor(((t-1)*zdim + (z-1))*cdim + c);
						tp.setColorModel(colormodel); tp.setMinAndMax(minshown,maxshown);
						g2d.drawImage(tp.createImage(),0,0,(int)(vof.width*mag),(int)(vof.height*mag),vof.x,vof.y,vof.x+vof.width,vof.y+vof.height,null);
					} else {
						g2d.setColor(settings.bgcolor);
						g2d.fillRect(0,0,width,height);
					}
					if (settings.showfinishedtracks) {
						g2d.setFont(settings.trackfont);
						g2d.setStroke(settings.trackstroke);
						try { g2d.setComposite(settings.trackopacity); } catch (Throwable e) { }
						g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
						g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						assembly.drawn(false); assembly.visible(false);
						if (settings.visibility == MTJSettings.UP_TO_CURRENT) drawendtime = t;
						else if (settings.visibility == MTJSettings.FROM_CURRENT) drawstarttime = t;
						mtrackj.handler().drawassembly(assembly,settings,g2d,vof,mag,t,drawstarttime,drawendtime);
					}
					movie.addSlice("", new ColorProcessor(frame));
					++nrframes;
				}
				pgs.stop();
			}
			
			if (nrframes > 0) {
				mtrackj.log("Displaying movie...");
				final ImagePlus movieimageplus = new ImagePlus("Movie",movie);
				movieimageplus.setDimensions(1,1,nrframes);
				movieimageplus.show();
				mtrackj.logok();
				mtrackj.status("Finished movie");
			} else {
				mtrackj.error("None of the frames contains visible tracks");
				mtrackj.copyright();
			}
			
		} catch(OutOfMemoryError e) {
			mtrackj.error("Out of memory while producing movie");
			pgs.stop(); // To close progress indication
			mtrackj.copyright();
			
		} // The uncaught-exception handler takes care of the rest
		
		assembly.unlock();
	}
	
}

// *************************************************************************************************
final class MTJMeasurer extends Thread {
	
	final static int MEASURE_POINTS=1, MEASURE_TRACKS=2, MEASURE_CLUSTERS=4, MEASURE_ASSEMBLY=8, MEASURE_ALL=16;
	final static String INF = "\u221E";
	final static String NA = "NA";
	
	private final MTrackJ mtrackj;
	private final int mode;
	private final int decs;
	
	MTJMeasurer(final MTrackJ mtrackj, final int mode, final int decs) {
		
		this.mtrackj = mtrackj;
		this.mode = mode;
		this.decs = decs;
		try { this.setUncaughtExceptionHandler(mtrackj.catcher()); }
		catch (final Throwable e) { }
	}
	
	public void run() {
		
		final MTJAssembly assembly = mtrackj.handler().assembly();
		final MTJSettings settings = mtrackj.settings();
		assembly.lock();
		
		mtrackj.status("Performing measurements...");
		final Progressor pgs = new Progressor();
		pgs.display(true);
		
		try {
			// Initialize fixed values:
			final boolean doslices = mtrackj.doslices();
			final boolean doframes = mtrackj.doframes();
			final boolean dochannels = mtrackj.dochannels();
			final boolean measurepoints = (mode & MEASURE_POINTS) > 0;
			final boolean measuretracks = (mode & MEASURE_TRACKS) > 0;
			final boolean measureclusters = (mode & MEASURE_CLUSTERS) > 0;
			final boolean measureassembly = (mode & MEASURE_ASSEMBLY) > 0;
			final boolean measurealltracks = (mode & MEASURE_ALL) > 0;
			final int nrclusters = assembly.size();
			final boolean doclusters = (nrclusters > 1) || measureclusters || measureassembly;
			final ImageStack stack = mtrackj.image().getImageStack();
			final MTJPoint reference = assembly.reference();
			final int xdim = mtrackj.getwidth();
			final int ydim = mtrackj.getheight();
			final int zdim = mtrackj.nrslices();
			final int tdim = mtrackj.nrframes();
			final int cdim = mtrackj.nrchannels();
			final Calibration[] dens = new Calibration[cdim];
			for (int c=1; c<=cdim; ++c) dens[c-1] = mtrackj.density(c);
			final Calibration cal = mtrackj.image().getCalibration().copy();
			final double pw = cal.pixelWidth;
			final double ph = cal.pixelHeight;
			final double pd = cal.pixelDepth;
			final double fi = cal.frameInterval;
			final String su = new String(cal.getUnit());
			final String tu = new String(cal.getTimeUnit());
			final String cu = "idx";
			final String vu = su + "/" + tu;
			final String au = "deg";
			String Iu = new String(cal.getValueUnit());
			if (Iu.equals("Gray Value") || (cdim > 1)) Iu = "val";
			int nrassemblytracks = 0;
			for (int c=0; c<nrclusters; ++c)
				nrassemblytracks += assembly.get(c).size();
			pgs.steps(nrassemblytracks); pgs.start();
			mtrackj.decimals(decs);
			
			// Initialize result windows:
			TextPanel assemblyresults = null;
			if (measureassembly) {
				assemblyresults = mtrackj.openassemblywindow().getTextPanel();
				assemblyresults.setColumnHeadings(
					"Nr\t"+
					"Clusters\t"+
					"Tracks\t"+
					"Points\t"+
					"Min x ["+su+"]\t"+
					"Max x ["+su+"]\t"+
					"Mean x ["+su+"]\t"+
					"SD x ["+su+"]\t"+
					"Min y ["+su+"]\t"+
					"Max y ["+su+"]\t"+
					"Mean y ["+su+"]\t"+
					"SD y ["+su+"]\t"+
					(doslices ?
						"Min z ["+su+"]\t"+
						"Max z ["+su+"]\t"+
						"Mean z ["+su+"]\t"+
						"SD z ["+su+"]\t"
						: ""
					)+
					(doframes ?
						"Min Dur ["+tu+"]\t"+
						"Max Dur ["+tu+"]\t"+
						"Mean Dur ["+tu+"]\t"+
						"SD Dur ["+tu+"]\t"
						: ""
					)+
					"Min I ["+Iu+"]\t"+
					"Max I ["+Iu+"]\t"+
					"Mean I ["+Iu+"]\t"+
					"SD I ["+Iu+"]\t"+
					"Min Len ["+su+"]\t"+
					"Max Len ["+su+"]\t"+
					"Mean Len ["+su+"]\t"+
					"SD Len ["+su+"]\t"+
					"Min D2S ["+su+"]\t"+
					"Max D2S ["+su+"]\t"+
					"Mean D2S ["+su+"]\t"+
					"SD D2S ["+su+"]\t"+
					"Min D2R ["+su+"]\t"+
					"Max D2R ["+su+"]\t"+
					"Mean D2R ["+su+"]\t"+
					"SD D2R ["+su+"]\t"+
					"Min D2P ["+su+"]\t"+
					"Max D2P ["+su+"]\t"+
					"Mean D2P ["+su+"]\t"+
					"SD D2P ["+su+"]\t"+
					"Min v ["+vu+"]\t"+
					"Max v ["+vu+"]\t"+
					"Mean v ["+vu+"]\t"+
					"SD v ["+vu+"]\t"+
					"Min \u03B1 ["+au+"]\t"+
					"Max \u03B1 ["+au+"]\t"+
					"Mean \u03B1 ["+au+"]\t"+
					"SD \u03B1 ["+au+"]\t"+
					"Min \u0394\u03B1 ["+au+"]\t"+
					"Max \u0394\u03B1 ["+au+"]\t"+
					"Mean \u0394\u03B1 ["+au+"]\t"+
					"SD \u0394\u03B1 ["+au+"]"+
					(doslices ?
						"\tMin \u03B8 ["+au+"]"+
						"\tMax \u03B8 ["+au+"]"+
						"\tMean \u03B8 ["+au+"]"+
						"\tSD \u03B8 ["+au+"]"+
						"\tMin \u0394\u03B8 ["+au+"]"+
						"\tMax \u0394\u03B8 ["+au+"]"+
						"\tMean \u0394\u03B8 ["+au+"]"+
						"\tSD \u0394\u03B8 ["+au+"]"
						: ""
					)
				);
			} else mtrackj.closeassemblywindow();
			
			TextPanel clusterresults = null;
			if (measureclusters) {
				clusterresults = mtrackj.openclusterswindow().getTextPanel();
				clusterresults.setColumnHeadings(
					"Nr\t"+
					"CID\t"+
					"Tracks\t"+
					"Points\t"+
					"Min x ["+su+"]\t"+
					"Max x ["+su+"]\t"+
					"Mean x ["+su+"]\t"+
					"SD x ["+su+"]\t"+
					"Min y ["+su+"]\t"+
					"Max y ["+su+"]\t"+
					"Mean y ["+su+"]\t"+
					"SD y ["+su+"]\t"+
					(doslices ?
						"Min z ["+su+"]\t"+
						"Max z ["+su+"]\t"+
						"Mean z ["+su+"]\t"+
						"SD z ["+su+"]\t"
						: ""
					)+
					(doframes ?
						"Min Dur ["+tu+"]\t"+
						"Max Dur ["+tu+"]\t"+
						"Mean Dur ["+tu+"]\t"+
						"SD Dur ["+tu+"]\t"
						: ""
					)+
					"Min I ["+Iu+"]\t"+
					"Max I ["+Iu+"]\t"+
					"Mean I ["+Iu+"]\t"+
					"SD I ["+Iu+"]\t"+
					"Min Len ["+su+"]\t"+
					"Max Len ["+su+"]\t"+
					"Mean Len ["+su+"]\t"+
					"SD Len ["+su+"]\t"+
					"Min D2S ["+su+"]\t"+
					"Max D2S ["+su+"]\t"+
					"Mean D2S ["+su+"]\t"+
					"SD D2S ["+su+"]\t"+
					"Min D2R ["+su+"]\t"+
					"Max D2R ["+su+"]\t"+
					"Mean D2R ["+su+"]\t"+
					"SD D2R ["+su+"]\t"+
					"Min D2P ["+su+"]\t"+
					"Max D2P ["+su+"]\t"+
					"Mean D2P ["+su+"]\t"+
					"SD D2P ["+su+"]\t"+
					"Min v ["+vu+"]\t"+
					"Max v ["+vu+"]\t"+
					"Mean v ["+vu+"]\t"+
					"SD v ["+vu+"]\t"+
					"Min \u03B1 ["+au+"]\t"+
					"Max \u03B1 ["+au+"]\t"+
					"Mean \u03B1 ["+au+"]\t"+
					"SD \u03B1 ["+au+"]\t"+
					"Min \u0394\u03B1 ["+au+"]\t"+
					"Max \u0394\u03B1 ["+au+"]\t"+
					"Mean \u0394\u03B1 ["+au+"]\t"+
					"SD \u0394\u03B1 ["+au+"]"+
					(doslices ?
						"\tMin \u03B8 ["+au+"]"+
						"\tMax \u03B8 ["+au+"]"+
						"\tMean \u03B8 ["+au+"]"+
						"\tSD \u03B8 ["+au+"]"+
						"\tMin \u0394\u03B8 ["+au+"]"+
						"\tMax \u0394\u03B8 ["+au+"]"+
						"\tMean \u0394\u03B8 ["+au+"]"+
						"\tSD \u0394\u03B8 ["+au+"]"
						: ""
					)
				);
			} else mtrackj.closeclusterswindow();
			
			TextPanel trackresults = null;
			if (measuretracks) {
				trackresults = mtrackj.opentrackswindow().getTextPanel();
				trackresults.setColumnHeadings(
					"Nr\t"+
					(doclusters ?
						"CID\t"
						: ""
					)+
					"TID\t"+
					"Points\t"+
					"Min x ["+su+"]\t"+
					"Max x ["+su+"]\t"+
					"Mean x ["+su+"]\t"+
					"SD x ["+su+"]\t"+
					"Min y ["+su+"]\t"+
					"Max y ["+su+"]\t"+
					"Mean y ["+su+"]\t"+
					"SD y ["+su+"]\t"+
					(doslices ?
						"Min z ["+su+"]\t"+
						"Max z ["+su+"]\t"+
						"Mean z ["+su+"]\t"+
						"SD z ["+su+"]\t"
						: ""
					)+
					(doframes ?
						"Dur ["+tu+"]\t"
						: ""
					)+
					(dochannels ?
						"c ["+cu+"]\t"
						: ""
					)+
					"Min I ["+Iu+"]\t"+
					"Max I ["+Iu+"]\t"+
					"Mean I ["+Iu+"]\t"+
					"SD I ["+Iu+"]\t"+
					"Len ["+su+"]\t"+
					"Min D2S ["+su+"]\t"+
					"Max D2S ["+su+"]\t"+
					"Mean D2S ["+su+"]\t"+
					"SD D2S ["+su+"]\t"+
					"Min D2R ["+su+"]\t"+
					"Max D2R ["+su+"]\t"+
					"Mean D2R ["+su+"]\t"+
					"SD D2R ["+su+"]\t"+
					"Min D2P ["+su+"]\t"+
					"Max D2P ["+su+"]\t"+
					"Mean D2P ["+su+"]\t"+
					"SD D2P ["+su+"]\t"+
					"Min v ["+vu+"]\t"+
					"Max v ["+vu+"]\t"+
					"Mean v ["+vu+"]\t"+
					"SD v ["+vu+"]\t"+
					"Min \u03B1 ["+au+"]\t"+
					"Max \u03B1 ["+au+"]\t"+
					"Mean \u03B1 ["+au+"]\t"+
					"SD \u03B1 ["+au+"]\t"+
					"Min \u0394\u03B1 ["+au+"]\t"+
					"Max \u0394\u03B1 ["+au+"]\t"+
					"Mean \u0394\u03B1 ["+au+"]\t"+
					"SD \u0394\u03B1 ["+au+"]"+
					(doslices ?
						"\tMin \u03B8 ["+au+"]"+
						"\tMax \u03B8 ["+au+"]"+
						"\tMean \u03B8 ["+au+"]"+
						"\tSD \u03B8 ["+au+"]"+
						"\tMin \u0394\u03B8 ["+au+"]"+
						"\tMax \u0394\u03B8 ["+au+"]"+
						"\tMean \u0394\u03B8 ["+au+"]"+
						"\tSD \u0394\u03B8 ["+au+"]"
						: ""
					)
				);
			} else mtrackj.closetrackswindow();
			
			TextPanel pointresults = null;
			if (measurepoints) {
				pointresults = mtrackj.openpointswindow().getTextPanel();
				pointresults.setColumnHeadings(
					"Nr\t"+
					(doclusters ?
						"CID\t"
						: ""
					)+
					"TID\t"+
					"PID\t"+
					"x ["+su+"]\t"+
					"y ["+su+"]\t"+
					(doslices ?
						"z ["+su+"]\t"
						: ""
					)+
					(doframes ?
						"t ["+tu+"]\t"
						: ""
					)+
					(dochannels ?
						"c ["+cu+"]\t"
						: ""
					)+
					"I ["+Iu+"]\t"+
					"Len ["+su+"]\t"+
					"D2S ["+su+"]\t"+
					"D2R ["+su+"]\t"+
					"D2P ["+su+"]\t"+
					"v ["+vu+"]\t"+
					"\u03B1 ["+au+"]\t"+
					"\u0394\u03B1 ["+au+"]"+
					(doslices ?
						"\t\u03B8 ["+au+"]"+
						"\t\u0394\u03B8 ["+au+"]"
						: ""
					)
				);
			} else mtrackj.closepointswindow();
			
			// Initialize measurement arrays:
			final DoubleArray mx = new DoubleArray();
			final DoubleArray my = new DoubleArray();
			final DoubleArray mz = new DoubleArray();
			final DoubleArray mI = new DoubleArray();
			final DoubleArray mLen = new DoubleArray();
			final DoubleArray mDur = new DoubleArray();
			final DoubleArray mD2S = new DoubleArray();
			final DoubleArray mD2R = new DoubleArray();
			final DoubleArray mD2P = new DoubleArray();
			final DoubleArray mv = new DoubleArray();
			final DoubleArray ma = new DoubleArray();
			final DoubleArray mDa = new DoubleArray();
			final DoubleArray me = new DoubleArray();
			final DoubleArray mDe = new DoubleArray();
			int mxyz_ts=0, mI_ts=0, mLen_ts=0, mDur_ts=0, mD2S_ts=0, mD2R_ts=0, mD2P_ts=0, mv_ts=0, ma_ts=0, mDa_ts=0, me_ts=0, mDe_ts=0; // Track start indices
			int mxyz_cs=0, mI_cs=0, mLen_cs=0, mDur_cs=0, mD2S_cs=0, mD2R_cs=0, mD2P_cs=0, mv_cs=0, ma_cs=0, mDa_cs=0, me_cs=0, mDe_cs=0; // Cluster start indices
			
			// Measure and display:
			int assemblypointcount = 0;
			int assemblytrackcount = 0;
			int assemblyclustercount = 0;
			int anr=0, cnr=0, tnr=0, pnr=0;
			final int cc = mtrackj.getchannel();
			for (int c=0; c<nrclusters; ++c) {
				final MTJCluster cluster = assembly.get(c);
				final int nrtracks = cluster.size();
				int clusterpointcount = 0;
				int clustertrackcount = 0;
				for (int t=0; t<nrtracks; ++t) {
					final MTJTrack track = cluster.get(t);
					final MTJPoint startpoint = track.first();
					if (measurealltracks || !(track.hidden() || (settings.showonlytrackscurrentchannel && startpoint.c != cc))) {
						final String sc = mtrackj.d2s(startpoint.c);
						double tDur0 = (startpoint.t-1)*fi;
						double tLen = 0, tDur1 = 0;
						MTJPoint prepoint = null;
						MTJPoint preprepoint = null;
						int trackpointcount = 0;
						final int nrpoints = track.size();
						for (int p=0; p<nrpoints; ++p) {
							final MTJPoint point = track.get(p);
							final double px = point.x*pw;
							final double py = point.y*ph;
							final double pz = (point.z-1)*pd;
							final double pt = (point.t-1)*fi;
							final String sx = mtrackj.d2s(px); mx.add(px);
							final String sy = mtrackj.d2s(py); my.add(py);
							final String sz = mtrackj.d2s(pz); mz.add(pz);
							final String st = mtrackj.d2s(pt); tDur1 = pt;
							String sI=NA, sLen="0", sD2S=NA, sD2R=NA, sD2P=NA, sv=NA, sa=NA, sDa=NA, se=NA, sDe=NA;
							if (point.x >= 0 && point.x < xdim &&
								point.y >= 0 && point.y < ydim &&
								point.z >= 0.5 && point.z < (zdim+0.5) &&
								point.t >= 1 && point.t <= tdim &&
								point.c >= 1 && point.c <= cdim) {
								final ImageProcessor ip = stack.getProcessor(((point.t-1)*zdim + (FMath.round(point.z)-1))*cdim + point.c);
								ip.setCalibrationTable(dens[point.c-1].getCTable());
								final double pI = mtrackj.value(ip,point.x,point.y);
								sI = mtrackj.d2s(pI); mI.add(pI);
							}
							if (true) { // startpoint != null
								final double dx = (point.x - startpoint.x)*pw;
								final double dy = (point.y - startpoint.y)*ph;
								final double dz = doslices ? (point.z - startpoint.z)*pd : 0;
								final double pD2S = Math.sqrt(dx*dx + dy*dy + dz*dz);
								sD2S = mtrackj.d2s(pD2S); mD2S.add(pD2S);
							}
							if (reference != null) {
								final double dx = (point.x - reference.x)*pw;
								final double dy = (point.y - reference.y)*ph;
								final double dz = doslices ? (point.z - reference.z)*pd : 0;
								final double pD2R = Math.sqrt(dx*dx + dy*dy + dz*dz);
								sD2R = mtrackj.d2s(pD2R); mD2R.add(pD2R);
							}
							if (prepoint != null) {
								final double dx = (point.x - prepoint.x)*pw;
								final double dy = (point.y - prepoint.y)*ph;
								final double dz = doslices ? (point.z - prepoint.z)*pd : 0;
								final double dt = (point.t - prepoint.t)*fi;
								final double dxy = Math.sqrt(dx*dx + dy*dy);
								final double dxyz = Math.sqrt(dx*dx + dy*dy + dz*dz);
								sD2P = mtrackj.d2s(dxyz); mD2P.add(dxyz);
								final double a = angle(dx,dy);
								final double e = angle(dxy,dz);
								if (preprepoint != null) {
									final double Da = difangle(a,ma.last());
									sDa = mtrackj.d2s(Da); mDa.add(Da);
									final double De = difangle(e,me.last());
									sDe = mtrackj.d2s(De); mDe.add(De);
								}
								sa = mtrackj.d2s(a); ma.add(a);
								se = mtrackj.d2s(e); me.add(e);
								if (dt != 0) {
									final double pv = dxyz/dt;
									sv = mtrackj.d2s(pv); mv.add(pv);
								}
								tLen += dxyz; sLen = mtrackj.d2s(tLen);
							}
							if (measurepoints) {
								pointresults.append(
									(++pnr)+"\t"+
									(doclusters ?
										cluster.id()+"\t"
										: ""
									)+
									track.id()+"\t"+
									point.id()+"\t"+
									sx+"\t"+
									sy+"\t"+
									(doslices ?
										sz+"\t"
										: ""
									)+
									(doframes ?
										st+"\t"
										: ""
									)+
									(dochannels ?
										sc+"\t"
										: ""
									)+
									sI+"\t"+
									sLen+"\t"+
									sD2S+"\t"+
									sD2R+"\t"+
									sD2P+"\t"+
									sv+"\t"+
									sa+"\t"+
									sDa+
									(doslices ?
										"\t"+se+
										"\t"+sDe
										: ""
									)
								);
							}
							preprepoint = prepoint;
							prepoint = point;
							trackpointcount++;
							clusterpointcount++;
							assemblypointcount++;
						} // End of points loop
						mDur.add(tDur1 - tDur0);
						mLen.add(tLen);
						if (measuretracks) {
							final String[] sx = stats(mx,mxyz_ts);
							final String[] sy = stats(my,mxyz_ts);
							final String[] sz = stats(mz,mxyz_ts);
							final String[] sI = stats(mI,mI_ts);
							final String[] sLen = stats(mLen,mLen_ts);
							final String[] sDur = stats(mDur,mDur_ts);
							final String[] sD2S = stats(mD2S,mD2S_ts);
							final String[] sD2R = stats(mD2R,mD2R_ts);
							final String[] sD2P = stats(mD2P,mD2P_ts);
							final String[] sv = stats(mv,mv_ts);
							final String[] sa = stats(ma,ma_ts);
							final String[] sDa = stats(mDa,mDa_ts);
							final String[] se = stats(me,me_ts);
							final String[] sDe = stats(mDe,mDe_ts);
							trackresults.append(
								(++tnr)+"\t"+
								(doclusters ?
									cluster.id()+"\t"
									: ""
								)+
								track.id()+"\t"+
								trackpointcount+"\t"+
								sx[0]+"\t"+
								sx[1]+"\t"+
								sx[2]+"\t"+
								sx[3]+"\t"+
								sy[0]+"\t"+
								sy[1]+"\t"+
								sy[2]+"\t"+
								sy[3]+"\t"+
								(doslices ?
									sz[0]+"\t"+
									sz[1]+"\t"+
									sz[2]+"\t"+
									sz[3]+"\t"
									: ""
								)+
								(doframes ?
									sDur[1]+"\t"
									: ""
								)+
								(dochannels ?
									sc+"\t"
									: ""
								)+
								sI[0]+"\t"+
								sI[1]+"\t"+
								sI[2]+"\t"+
								sI[3]+"\t"+
								sLen[1]+"\t"+
								sD2S[0]+"\t"+
								sD2S[1]+"\t"+
								sD2S[2]+"\t"+
								sD2S[3]+"\t"+
								sD2R[0]+"\t"+
								sD2R[1]+"\t"+
								sD2R[2]+"\t"+
								sD2R[3]+"\t"+
								sD2P[0]+"\t"+
								sD2P[1]+"\t"+
								sD2P[2]+"\t"+
								sD2P[3]+"\t"+
								sv[0]+"\t"+
								sv[1]+"\t"+
								sv[2]+"\t"+
								sv[3]+"\t"+
								sa[0]+"\t"+
								sa[1]+"\t"+
								sa[2]+"\t"+
								sa[3]+"\t"+
								sDa[0]+"\t"+
								sDa[1]+"\t"+
								sDa[2]+"\t"+
								sDa[3]+
								(doslices ?
									"\t"+se[0]+
									"\t"+se[1]+
									"\t"+se[2]+
									"\t"+se[3]+
									"\t"+sDe[0]+
									"\t"+sDe[1]+
									"\t"+sDe[2]+
									"\t"+sDe[3]
									: ""
								)
							);
							mxyz_ts = mx.size();
							mI_ts = mI.size();
							mLen_ts = mLen.size();
							mDur_ts = mDur.size();
							mD2S_ts = mD2S.size();
							mD2R_ts = mD2R.size();
							mD2P_ts = mD2P.size();
							mv_ts = mv.size();
							ma_ts = ma.size();
							mDa_ts = mDa.size();
							me_ts = me.size();
							mDe_ts = mDe.size();
						}
						clustertrackcount++;
						assemblytrackcount++;
					}
					pgs.step();
				} // End of tracks loop
				if (clusterpointcount > 0) {
					assemblyclustercount++;
					if (measureclusters) {
						final String[] sx = stats(mx,mxyz_cs);
						final String[] sy = stats(my,mxyz_cs);
						final String[] sz = stats(mz,mxyz_cs);
						final String[] sI = stats(mI,mI_cs);
						final String[] sLen = stats(mLen,mLen_cs);
						final String[] sDur = stats(mDur,mDur_cs);
						final String[] sD2S = stats(mD2S,mD2S_cs);
						final String[] sD2R = stats(mD2R,mD2R_cs);
						final String[] sD2P = stats(mD2P,mD2P_cs);
						final String[] sv = stats(mv,mv_cs);
						final String[] sa = stats(ma,ma_cs);
						final String[] sDa = stats(mDa,mDa_cs);
						final String[] se = stats(me,me_cs);
						final String[] sDe = stats(mDe,mDe_cs);
						clusterresults.append(
							(++cnr)+"\t"+
							cluster.id()+"\t"+
							clustertrackcount+"\t"+
							clusterpointcount+"\t"+
							sx[0]+"\t"+
							sx[1]+"\t"+
							sx[2]+"\t"+
							sx[3]+"\t"+
							sy[0]+"\t"+
							sy[1]+"\t"+
							sy[2]+"\t"+
							sy[3]+"\t"+
							(doslices ?
								sz[0]+"\t"+
								sz[1]+"\t"+
								sz[2]+"\t"+
								sz[3]+"\t"
								: ""
							)+
							(doframes ?
								sDur[0]+"\t"+
								sDur[1]+"\t"+
								sDur[2]+"\t"+
								sDur[3]+"\t"
								: ""
							)+
							sI[0]+"\t"+
							sI[1]+"\t"+
							sI[2]+"\t"+
							sI[3]+"\t"+
							sLen[0]+"\t"+
							sLen[1]+"\t"+
							sLen[2]+"\t"+
							sLen[3]+"\t"+
							sD2S[0]+"\t"+
							sD2S[1]+"\t"+
							sD2S[2]+"\t"+
							sD2S[3]+"\t"+
							sD2R[0]+"\t"+
							sD2R[1]+"\t"+
							sD2R[2]+"\t"+
							sD2R[3]+"\t"+
							sD2P[0]+"\t"+
							sD2P[1]+"\t"+
							sD2P[2]+"\t"+
							sD2P[3]+"\t"+
							sv[0]+"\t"+
							sv[1]+"\t"+
							sv[2]+"\t"+
							sv[3]+"\t"+
							sa[0]+"\t"+
							sa[1]+"\t"+
							sa[2]+"\t"+
							sa[3]+"\t"+
							sDa[0]+"\t"+
							sDa[1]+"\t"+
							sDa[2]+"\t"+
							sDa[3]+
							(doslices ?
								"\t"+se[0]+
								"\t"+se[1]+
								"\t"+se[2]+
								"\t"+se[3]+
								"\t"+sDe[0]+
								"\t"+sDe[1]+
								"\t"+sDe[2]+
								"\t"+sDe[3]
								: ""
							)
						);
						mxyz_cs = mx.size();
						mI_cs = mI.size();
						mLen_cs = mLen.size();
						mDur_cs = mDur.size();
						mD2S_cs = mD2S.size();
						mD2R_cs = mD2R.size();
						mD2P_cs = mD2P.size();
						mv_cs = mv.size();
						ma_cs = ma.size();
						mDa_cs = mDa.size();
						me_cs = me.size();
						mDe_cs = mDe.size();
					}
				}
			} // End of clusters loop
			if (assemblypointcount > 0 && measureassembly) {
				final String[] sx = stats(mx,0);
				final String[] sy = stats(my,0);
				final String[] sz = stats(mz,0);
				final String[] sI = stats(mI,0);
				final String[] sLen = stats(mLen,0);
				final String[] sDur = stats(mDur,0);
				final String[] sD2S = stats(mD2S,0);
				final String[] sD2R = stats(mD2R,0);
				final String[] sD2P = stats(mD2P,0);
				final String[] sv = stats(mv,0);
				final String[] sa = stats(ma,0);
				final String[] sDa = stats(mDa,0);
				final String[] se = stats(me,0);
				final String[] sDe = stats(mDe,0);
				assemblyresults.append(
					(++anr)+"\t"+
					assemblyclustercount+"\t"+
					assemblytrackcount+"\t"+
					assemblypointcount+"\t"+
					sx[0]+"\t"+
					sx[1]+"\t"+
					sx[2]+"\t"+
					sx[3]+"\t"+
					sy[0]+"\t"+
					sy[1]+"\t"+
					sy[2]+"\t"+
					sy[3]+"\t"+
					(doslices ?
						sz[0]+"\t"+
						sz[1]+"\t"+
						sz[2]+"\t"+
						sz[3]+"\t"
						: ""
					)+
					(doframes ?
						sDur[0]+"\t"+
						sDur[1]+"\t"+
						sDur[2]+"\t"+
						sDur[3]+"\t"
						: ""
					)+
					sI[0]+"\t"+
					sI[1]+"\t"+
					sI[2]+"\t"+
					sI[3]+"\t"+
					sLen[0]+"\t"+
					sLen[1]+"\t"+
					sLen[2]+"\t"+
					sLen[3]+"\t"+
					sD2S[0]+"\t"+
					sD2S[1]+"\t"+
					sD2S[2]+"\t"+
					sD2S[3]+"\t"+
					sD2R[0]+"\t"+
					sD2R[1]+"\t"+
					sD2R[2]+"\t"+
					sD2R[3]+"\t"+
					sD2P[0]+"\t"+
					sD2P[1]+"\t"+
					sD2P[2]+"\t"+
					sD2P[3]+"\t"+
					sv[0]+"\t"+
					sv[1]+"\t"+
					sv[2]+"\t"+
					sv[3]+"\t"+
					sa[0]+"\t"+
					sa[1]+"\t"+
					sa[2]+"\t"+
					sa[3]+"\t"+
					sDa[0]+"\t"+
					sDa[1]+"\t"+
					sDa[2]+"\t"+
					sDa[3]+
					(doslices ?
						"\t"+se[0]+
						"\t"+se[1]+
						"\t"+se[2]+
						"\t"+se[3]+
						"\t"+sDe[0]+
						"\t"+sDe[1]+
						"\t"+sDe[2]+
						"\t"+sDe[3]
						: ""
					)
				);
			}
			if (measureassembly && assemblyresults.getLineCount() == 0) assemblyresults.append(" ");
			if (measureclusters && clusterresults.getLineCount() == 0) clusterresults.append(" ");
			if (measuretracks && trackresults.getLineCount() == 0) trackresults.append(" ");
			if (measurepoints && pointresults.getLineCount() == 0) pointresults.append(" ");
			pgs.stop(); // To close progress indication
			mtrackj.status("Finished measurements");
			
		} catch(OutOfMemoryError e) {
			mtrackj.error("Out of memory while performing measurements");
			pgs.stop(); // To close progress indication
			mtrackj.copyright();
			
		} // The uncaught-exception handler takes care of the rest
		
		assembly.unlock();
	}
	
	private String[] stats(final DoubleArray da, final int start) {
		
		final String[] stats = new String[4];
		final int end = da.size();
		final int len = end - start;
		if (len <= 0) {
			stats[0] = stats[1] = stats[2] = stats[3] = NA;
		} else if (len == 1) {
			stats[0] = stats[1] = stats[2] = mtrackj.d2s(da.get(start));
			stats[3] = NA;
		} else {
			double val = da.get(start);
			double min, max, sum;
			min = max = sum = val;
			for (int i=start+1; i<end; ++i) {
				val = da.get(i); sum += val;
				if (val < min) min = val;
				else if (val > max) max = val;
			}
			final double mean = sum/len;
			double d = 0, ssd = 0;
			for (int i=start; i<end; ++i) {
				d = da.get(i) - mean;
				ssd += d*d;
			}
			final double sd = Math.sqrt(ssd/(len - 1));
			stats[0] = mtrackj.d2s(min);
			stats[1] = mtrackj.d2s(max);
			stats[2] = mtrackj.d2s(mean);
			stats[3] = mtrackj.d2s(sd);
		}
		return stats;
	}
	
	private double angle(final double ad, final double op) {
		
		if (ad == 0) {
			if (op == 0) return 0;
			else return (op > 0) ? 90 : -90;
		} else if (ad > 0) {
			return R2A*Math.atan(op/ad);
		} else {
			if (op >= 0) return R2A*Math.atan(op/ad) + 180;
			else return R2A*Math.atan(op/ad) - 180;
		}
	}
	
	private double difangle(final double cur, final double pre) {
		
		double dif = cur - pre;
		if (dif > 180) dif -= 360;
		else if (dif < -180) dif += 360;
		return dif;
	}
	
	private static final double R2A = 180/Math.PI;
	
}

// *************************************************************************************************
final class MTJReader extends Thread {
	
	final static int LOAD=0, IMPORT=1;
	private final MTrackJ mtrackj;
	private final String file;
	private final int mode;
	
	MTJReader(final MTrackJ mtrackj, final String file, final int mode) {
		
		this.mtrackj = mtrackj;
		this.file = file;
		this.mode = mode;
		try { this.setUncaughtExceptionHandler(mtrackj.catcher()); }
		catch (final Throwable e) { }
	}
	
	public void run() {
		
		int linenr = 0;
		BufferedReader br = null;
		final String opmode = (mode == LOAD) ? "Load" : "Import";
		
		mtrackj.status(opmode+"ing from \""+file+"\"...");
		final Progressor pgs = new Progressor();
		pgs.display(true);
		
		try {
			MTJAssembly readassembly = null;
			MTJSettings readsettings = null;
			final MTJSettings settings = mtrackj.settings();
			double xoffset = 0;
			double yoffset = 0;
			double zoffset = 0;
			double toffset = 0;
			double coffset = 0;
			if (mode == LOAD) {
				xoffset = settings.xloadoffset;
				yoffset = settings.yloadoffset;
				zoffset = settings.zloadoffset;
				toffset = settings.tloadoffset;
				coffset = settings.cloadoffset;
			} else {
				xoffset = settings.ximportoffset;
				yoffset = settings.yimportoffset;
				zoffset = settings.zimportoffset;
				toffset = settings.timportoffset;
				coffset = settings.cimportoffset;
			}
			final File fo = new File(file);
			pgs.steps((int)fo.length()); pgs.start();
			br = new BufferedReader(new FileReader(fo));
			mtrackj.log("Started "+opmode.toLowerCase()+"ing from \""+file+"\"");
			String line = br.readLine(); ++linenr; pgs.step(line.length()+2);
			if (!checkheader(line)) { br.close(); pgs.stop(); return; }
			final String fileversion = fileversion(line);
			line = br.readLine(); ++linenr; pgs.step(line.length()+2);
			boolean loadprops = false;
			if (line.startsWith("Displaying")) {
				if (settings.usedispmdf) {
					loadprops = true;
					if (mode == LOAD) {
						readsettings = settings.duplicate();
						readsettings.decode(line,fileversion);
					}
				}
				line = br.readLine(); ++linenr; pgs.step(line.length()+2);
			}
			if (line.startsWith("Offset")) {
				final StringTokenizer oitems = new StringTokenizer(line);
				oitems.nextToken(); // Skip "Offset"
				xoffset = Double.parseDouble(oitems.nextToken());
				yoffset = Double.parseDouble(oitems.nextToken());
				zoffset = Double.parseDouble(oitems.nextToken());
				toffset = Double.parseDouble(oitems.nextToken());
				coffset = Double.parseDouble(oitems.nextToken());
				line = br.readLine(); ++linenr; pgs.step(line.length()+2);
			}
			MTJPoint reference = null;
			if (line.startsWith("Origin") || line.startsWith("Reference")) {
				if (mode == LOAD) {
					final StringTokenizer oitems = new StringTokenizer(line);
					oitems.nextToken(); // Skip "Origin" or "Reference"
					reference = new MTJPoint();
					reference.x = Double.parseDouble(oitems.nextToken()) + xoffset;
					reference.y = Double.parseDouble(oitems.nextToken()) + yoffset;
					reference.z = Double.parseDouble(oitems.nextToken()) + zoffset;
				}
				line = br.readLine(); ++linenr; pgs.step(line.length()+2);
			}
			while (line.startsWith("Assembly")) {
				final StringTokenizer aitems = new StringTokenizer(line);
				aitems.nextToken(); // Skip "Assembly"
				readassembly = new MTJAssembly();
				readassembly.file(file);
				readassembly.reference(reference);
				readassembly.clusteriding(false);
				readassembly.id(Integer.parseInt(aitems.nextToken()));
				if (loadprops) readassembly.color(string2color(aitems.nextToken()));
				line = br.readLine(); ++linenr; pgs.step(line.length()+2);
				while (line.startsWith("Cluster")) {
					final StringTokenizer citems = new StringTokenizer(line);
					citems.nextToken(); // Skip "Cluster"
					final MTJCluster cluster = new MTJCluster();
					cluster.trackiding(false);
					cluster.id(Integer.parseInt(citems.nextToken()));
					if (loadprops) cluster.color(string2color(citems.nextToken()));
					readassembly.add(cluster);
					line = br.readLine(); ++linenr; pgs.step(line.length()+2);
					while (line.startsWith("Track")) {
						final StringTokenizer titems = new StringTokenizer(line);
						titems.nextToken(); // Skip "Track"
						final MTJTrack track = new MTJTrack();
						track.pointiding(false);
						track.id(Integer.parseInt(titems.nextToken()));
						if (loadprops) {
							track.color(string2color(titems.nextToken()));
							if (fileversion.compareTo("1.5.0") >= 0) track.hidden(!Boolean.parseBoolean(titems.nextToken()));
						}
						cluster.add(track);
						line = br.readLine(); ++linenr; pgs.step(line.length()+2);
						while (line.startsWith("Point")) {
							final StringTokenizer pitems = new StringTokenizer(line);
							pitems.nextToken(); // Skip "Point"
							final MTJPoint point = new MTJPoint();
							point.id(Integer.parseInt(pitems.nextToken()));
							point.x = Double.parseDouble(pitems.nextToken()) + xoffset;
							point.y = Double.parseDouble(pitems.nextToken()) + yoffset;
							point.z = Double.parseDouble(pitems.nextToken()) + zoffset;
							point.t = FMath.round(Double.parseDouble(pitems.nextToken()) + toffset);
							point.c = FMath.round(Double.parseDouble(pitems.nextToken()) + coffset);
							track.add(point);
							line = br.readLine(); ++linenr; pgs.step(line.length()+2);
						}
						track.pointiding(true);
					}
					cluster.trackiding(true);
				}
				readassembly.clusteriding(true);
			}
			br.close(); pgs.stop();
			mtrackj.status(opmode+"ed from \""+file+"\"");
			mtrackj.log("Finished "+opmode.toLowerCase()+"ing from \""+file+"\"");
			if (mode == LOAD) mtrackj.handler().reassemble(readassembly,readsettings);
			else mtrackj.handler().addclusters(readassembly);
			
		} catch(OutOfMemoryError e) {
			mtrackj.error("Out of memory while "+opmode.toLowerCase()+"ing from \""+file+"\"");
			pgs.stop(); // To close progress indication
			try { br.close(); } catch (Throwable x) { }
			mtrackj.copyright();
			
		} catch (Throwable e) {
			mtrackj.log("Could not read or interpret line "+linenr);
			mtrackj.error("An error occurred while "+opmode.toLowerCase()+"ing from \""+file+"\"");
			pgs.stop(); // To close progress indication
			try { br.close(); } catch (Throwable x) { }
			mtrackj.copyright();
		}
		
	}
	
	private boolean checkheader(final String fileheader) {
		
		final StringTokenizer hitems = new StringTokenizer(fileheader);
		
		if (!fileheader.startsWith(mtrackj.name()) || hitems.countTokens() < 2) {
			mtrackj.error("It is not clear from the header that the file is an "+mtrackj.name()+" data file");
			return false;
		} else {
			hitems.nextToken(); // Skip "MTrackJ"
			final String version = hitems.nextToken();
			if (version.compareTo(mtrackj.version()) > 0) {
				mtrackj.error("The file version number is "+version+" while this is "+mtrackj.name()+" version "+mtrackj.version());
				return false;
			}
		}
		
		return true;
	}
	
	private String fileversion(final String fileheader) {
		
		final StringTokenizer hitems = new StringTokenizer(fileheader);
		hitems.nextToken(); // Skip "MTrackJ"
		return hitems.nextToken();
	}
	
	private Color string2color(final String hexcol) {
		
		try { return new Color(0xFF000000|Integer.parseInt(hexcol,16)); }
		catch (Throwable e) { return Color.red; }
	}
	
}

// *************************************************************************************************
final class MTJWriter extends Thread {
	
	final static int SAVE=0, EXPORT=1;
	final static String EOL = "\r\n";
	private final MTrackJ mtrackj;
	private final String file;
	private final int mode;
	
	MTJWriter(final MTrackJ mtrackj, final String file, final int mode) {
		
		this.mtrackj = mtrackj;
		this.file = file;
		this.mode = mode;
		try { this.setUncaughtExceptionHandler(mtrackj.catcher()); }
		catch (final Throwable e) { }
	}
	
	public void run() {
		
		if (mode == SAVE) save();
		else export();
	}
	
	private void save() {
		
		BufferedWriter bw = null;
		final MTJHandler handler = mtrackj.handler();
		final boolean orgchanged = handler.changed();
		handler.changed(false); // Gets corrected below if saving fails...
		
		final MTJAssembly assembly = handler.assembly();
		assembly.lock();
		
		mtrackj.status("Saving to \""+file+"\"...");
		final Progressor pgs = new Progressor();
		pgs.display(true);
		
		try {
			final MTJSettings settings = mtrackj.settings();
			final double xoffset = settings.xsaveoffset;
			final double yoffset = settings.ysaveoffset;
			final double zoffset = settings.zsaveoffset;
			final double toffset = settings.tsaveoffset;
			final double coffset = settings.csaveoffset;
			bw = new BufferedWriter(new FileWriter(file));
			mtrackj.log("Started saving to \""+file+"\"");
			bw.write(mtrackj.name()+" "+mtrackj.version()+" Data File"+EOL);
			bw.write(settings.encode(MTJSettings.DISPLAYING)+EOL);
			final MTJPoint reference = assembly.reference();
			if (reference != null) bw.write("Reference "+reference.x+" "+reference.y+" "+reference.z+EOL);
			bw.write("Assembly "+assembly.id()+" "+color2string(assembly.color())+EOL);
			final int nrclusters = assembly.size();
			int nrsteps = 0;
			for (int c=0; c<nrclusters; ++c)
				nrsteps += assembly.get(c).size();
			pgs.steps(nrsteps); pgs.start();
			for (int c=0; c<nrclusters; ++c) {
				final MTJCluster cluster = assembly.get(c);
				bw.write("Cluster "+cluster.id()+" "+color2string(cluster.color())+EOL);
				final int nrtracks = cluster.size();
				for (int t=0; t<nrtracks; ++t) {
					final MTJTrack track = cluster.get(t);
					bw.write("Track "+track.id()+" "+color2string(track.color())+" "+String.valueOf(!track.hidden())+EOL);
					final int nrpoints = track.size();
					for (int j=0; j<nrpoints; ++j) {
						final MTJPoint point = track.get(j);
						final int pid = point.id();
						final double px = point.x + xoffset;
						final double py = point.y + yoffset;
						final double pz = point.z + zoffset;
						final double pt = point.t + toffset;
						final double pc = point.c + coffset;
						bw.write("Point "+pid+" "+px+" "+py+" "+pz+" "+pt+" "+pc+EOL);
					}
					pgs.step();
				}
			}
			bw.write("End of "+mtrackj.name()+" Data File"+EOL);
			bw.close(); pgs.stop();
			mtrackj.log("Finished saving to \""+file+"\"");
			mtrackj.status("Saved to \""+file+"\"");
			
		} catch (Throwable e) {
			mtrackj.error("An error occurred while saving to \""+file+"\"");
			pgs.stop(); // To close progress indication
			try { bw.close(); } catch (Throwable x) { }
			if (orgchanged) handler.changed(true);
			mtrackj.copyright();
		}
		
		assembly.unlock();
	}
	
	private void export() { }
	
	private String color2string(final Color color) {
		
		return Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
	}
	
}

// *************************************************************************************************
final class MTJCatcher implements Thread.UncaughtExceptionHandler {
	
	public void uncaughtException(Thread t, Throwable e) {
		
		IJ.log("Unexpected exception in "+MTrackJ.NAME+" "+MTrackJ.VERSION);
		IJ.log("OS version: "+System.getProperty("os.name")+" "+System.getProperty("os.version"));
		IJ.log("Java version: "+System.getProperty("java.version"));
		IJ.log("ImageJ version: "+IJ.getVersion());
		IJ.log("ImageScience version: "+ImageScience.version());
		IJ.log(t.toString());
		final java.io.CharArrayWriter cw = new java.io.CharArrayWriter();
		final java.io.PrintWriter pw = new java.io.PrintWriter(cw);
		e.printStackTrace(pw);
		IJ.log(cw.toString());
	}
	
}

// *************************************************************************************************
