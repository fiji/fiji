import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.LUT;
import imagescience.image.Image;
import imagescience.utility.I5DResource;
import imagescience.utility.ImageScience;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;

public final class TJ {
	
	private static final String NAME = "TransformJ";
	private static final String VERSION = "2.8.0";
	private static final String MINIJVERSION = "1.44a";
	private static final String MINISVERSION = "2.4.0";
	
	public static String name() { return NAME; }
	
	public static String version() { return VERSION; }
	
	static boolean libcheck() {
		
		if (IJ.getVersion().compareTo(MINIJVERSION) < 0) {
			error("This plugin requires ImageJ version "+MINIJVERSION+" or higher");
			return false;
		}
		
		try {
			if (ImageScience.version().compareTo(MINISVERSION) < 0)
			throw new IllegalStateException();
		} catch (Throwable e) {
			error("This plugin requires ImageScience version "+MINISVERSION+" or higher");
			return false;
		}
		
		return true;
	}
	
	static ImagePlus imageplus() {
		
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)  {
			error("There are no images open");
			return null;
		}
		
		return imp;
	}
	
	static void show(final Image img, final ImagePlus imp) {
		
		show(img,imp,unimap(imp.getNChannels()));
	}
	
	static void show(final Image img, final ImagePlus imp, final int[][] map) {
		
		ImagePlus newimp = img.imageplus();
		
		final int imptype = type(imp);
		if (imptype == IMAGE5D) {
			newimp = I5DResource.convert(newimp,true);
		} else if (imptype == COMPOSITEIMAGE && newimp.getNChannels() > 1) {
			newimp = new CompositeImage(newimp);
		} else {
			newimp.setOpenAsHyperStack(true);
		}
		
		transfer(imp,newimp,map);
		
		if (!TJ_Options.adopt) {
			if (newimp.getType() == newimp.COLOR_RGB) minmax(newimp,0,255);
			else { final double[] mm = img.extrema(); minmax(newimp,mm[0],mm[1]); }
		}
		
		newimp.changes = TJ_Options.save;
		
		log("Showing result image");
		newimp.show();
		
		if (TJ_Options.close) {
			log("Closing input image");
			imp.close();
		}
	}
	
	private static void transfer(final ImagePlus src, final ImagePlus dst, final int[][] map) {
		
		// Transfer basic calibration:
		final Calibration cal = src.getCalibration().copy();
		final Calibration dstcal = dst.getCalibration();
		cal.pixelWidth = dstcal.pixelWidth;
		cal.pixelHeight = dstcal.pixelHeight;
		cal.pixelDepth = dstcal.pixelDepth;
		cal.frameInterval = dstcal.frameInterval;
		dst.setCalibration(cal);
		
		// Transfer channel properties:
		if (type(dst) == IMAGE5D) {
			I5DResource.transfer(src,dst,map);
		} else {
			final int[] si = map[0];
			final int[] di = map[1];
			final int nc = src.getNChannels();
			final double[] min = new double[nc];
			final double[] max = new double[nc];
			final ColorModel[] cms = new ColorModel[nc];
			
			// Get source channel properties:
			if (src.isComposite()) {
				final CompositeImage srcci = (CompositeImage)src;
				for (int i=0; i<nc; ++i) {
					final LUT lut = srcci.getChannelLut(i+1);
					cms[i] = lut; min[i] = lut.min; max[i] = lut.max;
				}
			} else {
				final ImageProcessor ip = src.getProcessor();
				cms[0] = ip.getColorModel(); min[0] = ip.getMin(); max[0] = ip.getMax();
				for (int i=1; i<nc; ++i) { cms[i] = cms[0]; min[i] = min[0]; max[i] = max[0]; }
			}
			
			// Set destination channel properties:
			if (dst.isComposite()) { // Only true if src is also composite
				final CompositeImage srcci = (CompositeImage)src;
				final CompositeImage dstci = (CompositeImage)dst;
				for (int i=0; i<si.length; ++i) {
					dstci.updatePosition(di[i],1,1);
					final int sii = si[i] - 1;
					dstci.setChannelLut(new LUT((LUT)cms[sii],min[sii],max[sii]));
					dstci.setDisplayRange(min[sii],max[sii]);
				}
				dstci.updatePosition(1,1,1);
				dstci.setMode(srcci.getMode());
			} else {
				final int sii = si[0] - 1;
				final ColorModel srccm = cms[sii];
				if (srccm instanceof DirectColorModel) { // No need to clone
					dst.getProcessor().setColorModel(srccm);
					dst.getStack().setColorModel(srccm);
				} else if (srccm instanceof IndexColorModel) { // Also includes LUTs
					final IndexColorModel scm = (IndexColorModel)srccm;
					final int size = scm.getMapSize();
					final byte[] r = new byte[size]; scm.getReds(r);
					final byte[] g = new byte[size]; scm.getGreens(g);
					final byte[] b = new byte[size]; scm.getBlues(b);
					final IndexColorModel dcm = new IndexColorModel(8,size,r,g,b);
					dst.getProcessor().setColorModel(dcm);
					dst.getStack().setColorModel(dcm);
				}
				dst.setDisplayRange(min[sii],max[sii]);
			}
		}
	}
	
	private static void minmax(final ImagePlus imp, final double min, final double max) {
		
		switch (type(imp)) {
			
			case IMAGE5D: {
				I5DResource.minmax(imp,min,max);
				break;
			}
			case COMPOSITEIMAGE: {
				final CompositeImage ci = (CompositeImage)imp;
				final int nc = ci.getNChannels();
				for (int c=1; c<=nc; ++c) {
					final LUT lut = ci.getChannelLut(c);
					lut.min = min;
					lut.max = max;
				}
				break;
			}
			case HYPERSTACK:
			case IMAGESTACK: 
			case SINGLEIMAGE: {
				imp.setDisplayRange(min,max);
				break;
			}
		}
	}
	
	private static int[][] unimap(final int nc) {
		
		final int[][] idx = new int[2][nc];
		for (int i=0; i<nc; ++i)
			idx[0][i] = idx[1][i] = i + 1;
		return idx;
	}
	
	private static final int SINGLEIMAGE=1, IMAGESTACK=2, HYPERSTACK=3, COMPOSITEIMAGE=4, IMAGE5D=5;
	
	private static int type(final ImagePlus imp) {
		
		int type = SINGLEIMAGE;
		boolean i5dexist = false;
		try { Class.forName("i5d.Image5D"); i5dexist = true; } catch (Throwable e) { }
		if (i5dexist && I5DResource.instance(imp)) type = IMAGE5D;
		else if (imp.isComposite()) type = COMPOSITEIMAGE;
		else if (imp.isHyperStack()) type = HYPERSTACK;
		else if (imp.getImageStackSize() > 1) type = IMAGESTACK;
		return type;
	}
	
	static void error(final String message) {
		
		IJ.showMessage(NAME+": Error",message+".");
		IJ.showProgress(1);
		IJ.showStatus("");
	}
	
	static void log(final String message) {
		
		if (TJ_Options.log) IJ.log(message);
	}
	
	static void status(final String message) {
		
		IJ.showStatus(message);
	}
	
}
