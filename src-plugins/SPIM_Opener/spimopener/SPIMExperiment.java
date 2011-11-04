package spimopener;

import ij.IJ;
import ij.ImagePlus;

import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.HashMap;


public class SPIMExperiment {

	public static final int X = 0;
	public static final int Y = 1;
	public static final int F = 2;
	public static final int Z = 3;
	public static final int T = 4;

	public final int sampleStart, sampleEnd;
	public final int timepointStart, timepointEnd;
	public final int regionStart, regionEnd;
	public final int angleStart, angleEnd;
	public final int channelStart, channelEnd;
	public final int planeStart, planeEnd;
	public final int frameStart, frameEnd;
	public final String pathFormatString;
	public final double pw, ph, pd;
	public final int w, h, d;
	public final String[] samples, regions, angles, channels;
	public final String experimentName;

	public static final int NO_PROJECTION          = 0;
	public static final int MAX_PROJECTION         = 1;
	public static final int MIN_PROJECTION         = 2;
	public static final int GAUSSIAN_STACK_FOCUSER = 3;

	public SPIMExperiment(String xmlfile) {
		if(!xmlfile.endsWith(".xml"))
			throw new IllegalArgumentException("Please select an xml file");
		File experimentFolder = new File(xmlfile.substring(0, xmlfile.length() - 4));
		experimentName = experimentFolder.getParentFile().getName() + " - " + experimentFolder.getName();
		experimentFolder = new File(experimentFolder.getParent(), experimentFolder.getName());
		if(!experimentFolder.exists() || !experimentFolder.isDirectory())
			throw new IllegalArgumentException("No directory " + experimentFolder.getAbsolutePath());

		File tmp = new File(experimentFolder.getAbsolutePath());
		System.out.println(tmp.getAbsolutePath());
		samples             = filter(tmp.list(), "s\\d{3}?"); Arrays.sort(samples);    tmp = new File(tmp, samples[0]);
		String[] timepoints = filter(tmp.list(), "t\\d{5}?"); Arrays.sort(timepoints); tmp = new File(tmp, timepoints[0]);
		regions             = filter(tmp.list(), "r\\d{3}?"); Arrays.sort(regions);    tmp = new File(tmp, regions[0]);
		angles              = filter(tmp.list(), "a\\d{3}?"); Arrays.sort(angles);     tmp = new File(tmp, angles[0]);
		channels            = filter(tmp.list(), "c\\d{3}?"); Arrays.sort(channels);   tmp = new File(tmp, channels[0]);
		String[] planes     = filter(tmp.list(), "z\\d{4}?"); Arrays.sort(planes);     tmp = new File(tmp, planes[0]);
		String[] frames     = tmp.list();                     Arrays.sort(frames);

		sampleStart    = getMin(samples);
		sampleEnd      = getMax(samples);
		timepointStart = getMin(timepoints);
		timepointEnd   = getMax(timepoints);
		regionStart    = getMin(regions);
		regionEnd      = getMax(regions);
		angleStart     = getMin(angles);
		angleEnd       = getMax(angles);
		channelStart   = getMin(channels);
		channelEnd     = getMax(channels);
		int zMin       = getMin(planes);
		int zMax       = getMax(planes);
		int fMin       = getMin(frames);
		int fMax       = getMax(frames);

		if(frames[0].startsWith("plane_")) {
			pathFormatString = experimentFolder.getAbsolutePath() + File.separator +
				"s%03d/t%05d/r%03d/a%03d/c%03d/z0000/plane_%010d.dat";
			zMin = fMin;
			zMax = fMax;
			fMin = fMax = 0;
		} else {
			pathFormatString = experimentFolder.getAbsolutePath() + File.separator +
				"s%03d/t%05d/r%03d/a%03d/c%03d/z%04d/%010d.dat";
		}

		planeStart = zMin;
		planeEnd   = zMax;
		frameStart = fMin;
		frameEnd   = fMax;

		try {
			XMLReader xmlreader = new XMLReader(xmlfile);
			pw = xmlreader.pw;
			ph = xmlreader.ph;
			pd = xmlreader.pd;
			w = xmlreader.width;
			h = xmlreader.height;
			d = xmlreader.depth;
		} catch(Exception e) {
			throw new IllegalArgumentException("Error reading xml file: " + xmlfile, e);
		}
	}

	public String getPath(int sample, int timepoint, int region, int angle, int channel, int plane, int frame) {
		return String.format(pathFormatString, sample, timepoint, region, angle, channel, plane, frame);
	}

	public ImagePlus open(int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int projectionMethod,
				boolean virtual) {
		return open(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, 0, h - 1, 0, w - 1, projectionMethod, virtual);				
	}

	public ImagePlus open(int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int yMin, int yMax,
				int xMin, int xMax,
				int projectionMethod,
				boolean virtual) {

	
		int xDir = X;
		int yDir = Y;
		int projectionDir = Z; // z is the default projection direction

		// now look through the different dimensions and select an appropriate direction for z
		int nTp = tpMax - tpMin + 1;
		int nZ  =  zMax -  zMin + 1;
		int nF  =  fMax -  fMin + 1;

		int nDimensionsWithMoreThanOne = 0;
		int zDir = -1;

		if(nTp > 1) { zDir = T; nDimensionsWithMoreThanOne++; }
		if(nF  > 1) { zDir = F; nDimensionsWithMoreThanOne++; }
		if(nZ  > 1 && projectionMethod == NO_PROJECTION) { zDir = Z; nDimensionsWithMoreThanOne++; }

		if(nDimensionsWithMoreThanOne > 1)
			throw new IllegalArgumentException("Only one dimension of time, plane and frame may contain an interval");

		if(zDir == -1)
			zDir = T;

		return open(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, yMin, yMax, xMin, xMax, xDir, yDir, zDir, virtual, projectionMethod, projectionDir);
	}

	public ImagePlus open(int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int yMin, int yMax,
				int xMin, int xMax,
				int xDir,
				int yDir,
				int zDir,
				boolean virtual,
				int projectionMethod,
				int projectionDir) {

		if(projectionMethod == NO_PROJECTION)
			return openNotProjected(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, yMin, yMax, xMin, xMax, xDir, yDir, zDir, virtual);

		Projector projector = null;
		switch(projectionMethod) {
			case MIN_PROJECTION:         projector = new MinimumProjector(); break;
			case MAX_PROJECTION:         projector = new MaximumProjector(); break;
			case GAUSSIAN_STACK_FOCUSER: projector = new GaussianStackFocuser(); break;
			default: throw new IllegalArgumentException("Unknown projection method: " + projectionMethod);
		}
		
		final int D = 5;
		final int[] MIN = new int[] { xMin, yMin, fMin, zMin, tpMin };
		final int[] MAX = new int[] { xMax, yMax, fMax, zMax, tpMax };

		// check that projection method is none of xdir, ydir, zdir and max-min+1 > 1
		if(projectionDir == xDir)
			throw new IllegalArgumentException("The projection direction cannot be the same as the dimension displayed in x direction");
		if(projectionDir == yDir)
			throw new IllegalArgumentException("The projection direction cannot be the same as the dimension displayed in y direction");
		if(projectionDir == zDir)
			throw new IllegalArgumentException("The projection direction cannot be the same as the dimension displayed in z direction");
		if(MAX[projectionDir] - MIN[projectionDir] + 1 <= 1)
			return openNotProjected(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, yMin, yMax, xMin, xMax, xDir, yDir, zDir, virtual);

		int ws = MAX[xDir] - MIN[xDir] + 1;
		int hs = MAX[yDir] - MIN[yDir] + 1;
		SPIMStack stack = virtual ? new SPIMVirtualStack(ws, hs) : new SPIMRegularStack(ws, hs);

		final int[] position = new int[D];
		System.arraycopy(MIN, 0, position, 0, D);

		if(xDir == X && yDir == Y) {
			for(int z = MIN[zDir]; z <= MAX[zDir]; z++) {
				position[zDir] = z;
				if(IJ.escapePressed()) {
					IJ.resetEscape();
					break;
				}
				projector.reset();
				for(int proj = MIN[projectionDir]; proj <= MAX[projectionDir]; proj++) {
					position[projectionDir] = proj;
					String path = getPath(sample, position[T], region, angle, channel, position[Z], position[F]);
					ImageProcessor ip = openRaw(path, w, h, MIN[xDir], MAX[xDir], MIN[yDir], MAX[yDir]);
					projector.add(ip);
				}
				stack.addSlice(projector.getProjection());
				IJ.showProgress(z - MIN[zDir], MAX[zDir] - MIN[zDir] + 1);
			}
		}
		else {
			int[] ordered = new int[2];
			ordered[0] = Math.min(xDir, yDir);
			ordered[1] = Math.max(xDir, yDir);
			for(int z = MIN[zDir]; z <= MAX[zDir]; z++) {
				position[zDir] = z;
				if(IJ.escapePressed()) {
					IJ.resetEscape();
					break;
				}
				projector.reset();
				for(int proj = MIN[projectionDir]; proj <= MAX[projectionDir]; proj++) {
					position[projectionDir] = proj;

					ImageProcessor ip = new ShortProcessor(MAX[xDir] - MIN[xDir] + 1, MAX[yDir] - MIN[yDir] + 1);
	
					for(int i1 = MIN[ordered[1]]; i1 <= MAX[ordered[1]]; i1++) {
						position[ordered[1]] = i1;
						String path = getPath(sample, position[T], region, angle, channel, position[Z], position[F]);
						ImageProcessor org = openRaw(path, w, h);
						for(int i2 = MIN[ordered[0]]; i2 <= MAX[ordered[0]]; i2++) {
							position[ordered[0]] = i2;
							ip.set(position[xDir] - MIN[xDir], position[yDir] - MIN[yDir], org.get(position[X], position[Y]));
						}
					}
					projector.add(ip);
				}

				stack.addSlice(projector.getProjection());
				System.out.println((z - MIN[zDir]) + " / " + (MAX[zDir] - MIN[zDir] + 1));
				IJ.showProgress(z - MIN[zDir], MAX[zDir] - MIN[zDir] + 1);
			}
		}
		IJ.showProgress(1);

		double[] pdiffs = new double[] { pw, ph, 1, pd, 1 };
		ImagePlus ret = new ImagePlus(experimentName, stack);

		ret.getCalibration().pixelWidth = pdiffs[xDir];
		ret.getCalibration().pixelWidth = pdiffs[yDir];
		ret.getCalibration().pixelWidth = pdiffs[zDir];
		return ret;
	}

	public ImagePlus openNotProjected(int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int yMin, int yMax,
				int xMin, int xMax,
				int xDir,
				int yDir,
				int zDir,
				boolean virtual) {

		final int D = 5;
		final int[] MIN = new int[] { xMin, yMin, fMin, zMin, tpMin };
		final int[] MAX = new int[] { xMax, yMax, fMax, zMax, tpMax };

		int ws = MAX[xDir] - MIN[xDir] + 1;
		int hs = MAX[yDir] - MIN[yDir] + 1;
		SPIMStack stack = virtual ? new SPIMVirtualStack(ws, hs) : new SPIMRegularStack(ws, hs);

		final int[] position = new int[D];
		System.arraycopy(MIN, 0, position, 0, D);

		if(xDir == X && yDir == Y) {
			stack.setRange(w, h, MIN[xDir], MIN[yDir]);
			for(int z = MIN[zDir]; z <= MAX[zDir]; z++) {
				if(IJ.escapePressed()) {
					IJ.resetEscape();
					break;
				}
				position[zDir] = z;
				String path = getPath(sample, position[T], region, angle, channel, position[Z], position[F]);
				stack.addSlice(path);
				IJ.showProgress(z - MIN[zDir], MAX[zDir] - MIN[zDir] + 1);
			}
		}
		else {
			int[] ordered = new int[2];
			ordered[0] = Math.min(xDir, yDir);
			ordered[1] = Math.max(xDir, yDir);
			for(int z = MIN[zDir]; z <= MAX[zDir]; z++) {
				if(IJ.escapePressed()) {
					IJ.resetEscape();
					break;
				}
				position[zDir] = z;
				ImageProcessor ip = new ShortProcessor(MAX[xDir] - MIN[xDir] + 1, MAX[yDir] - MIN[yDir] + 1);

				for(int i1 = MIN[ordered[1]]; i1 <= MAX[ordered[1]]; i1++) {
					position[ordered[1]] = i1;
					String path = getPath(sample, position[T], region, angle, channel, position[Z], position[F]);
					ImageProcessor org = openRaw(path, w, h);
					for(int i2 = MIN[ordered[0]]; i2 <= MAX[ordered[0]]; i2++) {
						position[ordered[0]] = i2;
						ip.set(position[xDir] - MIN[xDir], position[yDir] - MIN[yDir], org.get(position[X], position[Y]));
					}
				}

				stack.addSlice(ip);
				System.out.println((z - MIN[zDir]) + " / " + (MAX[zDir] - MIN[zDir] + 1));
				IJ.showProgress(z - MIN[zDir], MAX[zDir] - MIN[zDir] + 1);
			}
		}
		IJ.showProgress(1);

		double[] pdiffs = new double[] { pw, ph, 1, pd, 1 };
		ImagePlus ret = new ImagePlus(experimentName, stack);

		ret.getCalibration().pixelWidth = pdiffs[xDir];
		ret.getCalibration().pixelWidth = pdiffs[yDir];
		ret.getCalibration().pixelWidth = pdiffs[zDir];
		return ret;
	}

	public static ImageProcessor openRaw(String path, int orgW, int orgH, int xMin, int xMax, int yMin, int yMax) {
		if(xMin == 0 && xMax == orgW - 1 && yMin == 0 && yMax == orgH - 1)
			return openRaw(path, orgW, orgH);
		int ws = xMax - xMin + 1;
		int hs = yMax - yMin + 1;

		byte[] bytes = new byte[ws * hs * 2];
		short[] pixels = new short[ws * hs];

		FileInputStream in = null;
		try {
			in = new FileInputStream(path);

			// skip the top
			int toSkip = 2 * (yMin * orgW + xMin);
			while(toSkip > 0)
				toSkip -= in.skip(toSkip);

			// read through it line by line
			int offs = 0;
			for(int r = 0; r < hs; r++) {
				// read the data
				int read = 0;
				while(read < ws)
					read += in.read(bytes, offs + read, 2 * ws - read);
				offs += 2 * ws;

				// skip to next line
				toSkip = 2 * (orgW - xMax - 1 + xMin);
				while(toSkip > 0)
					toSkip -= in.skip(toSkip);
			}
			in.close();
		} catch(IOException e) {
			throw new RuntimeException("Cannot load " + path, e);
		}

		for(int i = 0; i < pixels.length; i++) {
			int low  = 0xff & bytes[2 * i];
			int high = 0xff & bytes[2 * i + 1];
			pixels[i] = (short)((high << 8) | low);
		}

		return new ShortProcessor(ws, hs, pixels, null);
	}

	public static ImageProcessor openRaw(String path, int w, int h) {
		byte[] bytes = new byte[w * h * 2];
		short[] pixels = new short[w * h];

		FileInputStream in = null;
		try {
			in = new FileInputStream(path);
			int read = 0;
			while(read < bytes.length)
				read += in.read(bytes, read, bytes.length - read);
			in.close();
		} catch(IOException e) {
			throw new RuntimeException("Cannot load " + path, e);
		}

		for(int i = 0; i < pixels.length; i++) {
			int low  = 0xff & bytes[2 * i];
			int high = 0xff & bytes[2 * i + 1];
			pixels[i] = (short)((high << 8) | low);
		}

		return new ShortProcessor(w, h, pixels, null);
	}

	private HashMap<String, RandomAccessFile> cache = new HashMap<String, RandomAccessFile>();

	private final void closeAllFiles() throws IOException {
		for(RandomAccessFile f : cache.values())
			f.close();
		cache.clear();
	}

	public static void saveRaw(ImageProcessor ip, String path) {
		short[] pixels = (short[])ip.getPixels();
		byte[] bytes = new byte[pixels.length * 2];

		for(int i = 0; i < pixels.length; i++) {
			short pixel = pixels[i];
			bytes[2 * i] = (byte)pixel;
			bytes[2 * i + 1] = (byte)(pixel >> 8);
		}

		try {
			FileOutputStream out = new FileOutputStream(path);
			out.write(bytes);
			out.close();
		} catch(IOException e) {
			throw new RuntimeException("Cannot save to " + path, e);
		}
	}

	private static String[] filter(String[] in, String pattern) {
		ArrayList<String> all = new ArrayList(in.length);
		for(String s : in)
			if(s.matches(pattern))
				all.add(s);
		String[] out = new String[all.size()];
		all.toArray(out);
		return out;
	}

	private static int getMin(String[] s) {
		int idx = 0;
		int start = s[idx].startsWith("plane_") ? 6 : 1;
		int stop = s[idx].indexOf('.') >= 0 ? s[idx].length() - 4 : s[idx].length();
		return Integer.parseInt(s[idx].substring(start, stop));
	}

	private static int getMax(String[] s) {
		int idx = s.length - 1;
		int start = s[idx].startsWith("plane_") ? 6 : 1;
		int stop = s[idx].indexOf('.') >= 0 ? s[idx].length() - 4 : s[idx].length();
		return Integer.parseInt(s[idx].substring(start, stop));
	}
}
