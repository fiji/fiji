package process3d;

import vib.PointList;
import vib.BenesNamedPoint;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;

import ij.text.TextWindow;

import ij.gui.GenericDialog;
import ij.WindowManager;

public class IFT_ implements PlugInFilter {
	
	private ImagePlus image;
	private int w, h, wh, d;
	private PriorityQueue queue;
	private byte[][] data;
	private int[][] result;
	private int[] C;
	private boolean[] flag;


	public IFT_(ImagePlus image) {
		this.image = image;
	}

	public IFT_() {}

	public void run(ImageProcessor ip) {
		int[] wIDs = WindowManager.getIDList();
		if(wIDs == null){
			IJ.error("No images open");
			return;
		}
		String[] titles = new String[wIDs.length + 2];
		for(int i = 0; i < wIDs.length; i++){
			titles[i] = WindowManager.getImage(wIDs[i]).getTitle();
		}
		titles[titles.length - 1] = "use seeds from point list";
		titles[titles.length - 2] = "use local minima";
		GenericDialog gd = new GenericDialog("Watershed from markers");
		gd.addChoice("Seeds", titles, titles[0]);
		gd.addCheckbox("Show class indices", true);
		gd.addCheckbox("Show mean intensities", true);
		gd.addCheckbox("Show results table", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int seed = gd.getNextChoiceIndex();
		if(seed == titles.length - 1)
			initFromPointList();
		else if(seed == titles.length - 2)
			initFromMinima();
		else
			initFromImage(WindowManager.getImage(titles[seed]));
		propagate();

		if(gd.getNextBoolean())
			createResult().show();
		if(gd.getNextBoolean())
			createMeans().show();
		if(gd.getNextBoolean())
			new TextWindow("Classes",
				"min\tmax\tmean\tvol\tcogx\tcogy\tcogz\tox\toy\toz",
				createSummaryString(), 400, 500);
	}

	Cls[] classes;
	public void initFromImage(ImagePlus seeds) {
		List<Cls> classlist = new ArrayList<Cls>();
		w = image.getWidth();
		h = image.getHeight();
		wh = w * h;
		d = image.getStackSize();
		data = new byte[d][];
		for(int z = 0; z < d; z++) {
			data[z] = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
		}
		C = new int[w*h*d];
		flag = new boolean[w*h*d];
		result = new int[d][w*h];
		for(int i = 0; i < C.length; i++)
			C[i] = 255;
		queue = new PriorityQueue();
		for(int z = 0; z < seeds.getStackSize(); z++) {
			byte[] b = (byte[])seeds.getStack().getPixels(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int i = y*w+x;
					if(b[i] == 0)
						continue;
					int index = z*b.length + i;
					int cost = 0;
					C[index] = cost;
					result[z][i] = b[i] & 0xff;
					queue.add(index, cost);
					addClass(classlist, x, y, z, b[i]);
				}
			}
		}
		classes = new Cls[classlist.size()];
		classlist.toArray(classes);
	}

	public void initFromMinima() {
		IJ.showStatus("Find minima");
		List<Cls> classlist = new ArrayList<Cls>();
		w = image.getWidth();
		h = image.getHeight();
		wh = w * h;
		d = image.getStackSize();
		data = new byte[d][];
		for(int z = 0; z < d; z++) {
			data[z] = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
		}
		C = new int[w*h*d];
		flag = new boolean[w*h*d];
		result = new int[d][w*h];
		for(int i = 0; i < C.length; i++)
			C[i] = 255;

		ImagePlus minima = new Find_Minima(image).classify();

		queue = new PriorityQueue();
		int counter = 0;
		for(int z = 0; z < minima.getStackSize(); z++) {
			byte[] b = (byte[])minima.getStack().getPixels(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int i = y * w + x;
					if(b[i] == 0)
						continue;
					int index = z * b.length + i;
					int cost = 0;
					C[index] = cost;
					result[z][i] = counter++;
					queue.add(index, cost);
					addClass(classlist, x, y, z,
							data[z][y * w + x]);
					Flood_Fill.fill(minima, x, y, z, (byte)0);
				}
			}
			IJ.showProgress(z + 1, d);
		}
System.out.println(counter + " classes");
		classes = new Cls[counter];
		classlist.toArray(classes);
	}

	protected static void addClass(List<Cls> classlist,
			int x, int y, int z, byte value) {
		Cls cls = new Cls();
		cls.add(x, y, z, value);
		cls.originx = x;
		cls.originy = y;
		cls.originz = z;
		classlist.add(cls);
	}

	public void initFromPointList() {
		List<Cls> classlist = new ArrayList<Cls>();
		w = image.getWidth();
		h = image.getHeight();
		wh = w * h;
		d = image.getStackSize();
		PointList markers = PointList.load(image);
		data = new byte[d][];
		for(int z = 0; z < d; z++) {
			data[z] = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
		}
		C = new int[w*h*d];
		flag = new boolean[w*h*d];
		result = new int[d][w*h];
		for(int i = 0; i < C.length; i++)
			C[i] = 255;
		queue = new PriorityQueue();
		int m = 1;
		for(Iterator it = markers.iterator(); it.hasNext();) {
			BenesNamedPoint p = (BenesNamedPoint)it.next();
			int z = (int)p.z;
			int i = (int)p.y * w + (int)p.x;
			int index = z*w*h + i;
			int cost = 0;
			C[index] = cost;
			m += 10;
			result[z][i] = m;
			queue.add(index, cost);
			addClass(classlist,
				(int)p.x, (int)p.y, (int)p.z, (byte)m);
		}
		classes = new Cls[classlist.size()];
		classlist.toArray(classes);
	}

	private int[] neighbors = new int[6];

	public void propagate() {
System.out.println("Propagate");
		int counter = 0;
		int whd = wh * d;
		while(!queue.isEmpty()) {
			int v = queue.poll();
			flag[v] = true;
			getNeighbours(v);
			for(int i = 0; i < neighbors.length; i++) {
				int p = neighbors[i];
				if(p == -1)
					continue;
				int pCost = C[p];
 				int m = C[v] + weight(v, p);
				if(m < C[p]) {
					C[p] = m;
					int pz = p / wh, pi = p % wh;
					int vz = v / wh, vi = v % wh;
					result[pz][pi] = result[vz][vi];
					classes[result[vz][vi]].add(pi % w, pi / w, pz, data[pz][pi]);
					counter++;
					queue.removeFromBucket(p, pCost);
					queue.add(p, C[p]);
				}
			}
			if(counter % 1000 == 0)
				IJ.showProgress(counter, whd);
		}
		IJ.showProgress(1);
		for(int i = 0; i < classes.length; i++)
			classes[i].finished();
	}

	public ImagePlus createResult() {
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("", 
				new ColorProcessor(w, h, result[z]));
		}
		ImagePlus ret = new ImagePlus("Result", stack);
		ret.setCalibration(image.getCalibration());
		return ret;
	}

	public ImagePlus createMeans() {
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] means = new byte[wh];
			for(int i = 0; i < wh; i++)
				means[i] = (byte)classes[result[z][i]].mean;
			stack.addSlice("", 
				new ByteProcessor(w, h, means, null));
		}
		ImagePlus ret = new ImagePlus("Result", stack);
		ret.setCalibration(image.getCalibration());
		return ret;
	}

	public String createSummaryString() {
		String ret = "";
		for(int i = 0; i < classes.length; i++) {
			Cls c = classes[i];
			ret += c.min + "\t" + c.max + "\t" + c.mean + "\t" + c.vol + "\t" + c.cogx + "\t" + c.cogy + "\t" + c.cogz + c.originx + "\t" + c.originy + "\t" + c.originz + "\n";
		}
		return ret;
	}

	public Cls[] getClasses() {
		return classes;
	}

 	public int weight(int n1, int n2) {
		int z1 = n1 / wh; int i1 = n1 % wh;
		int z2 = n2 / wh; int i2 = n2 % wh;

		return (int)Math.abs((int)(data[z1][i1] & 0xff) - 
			(int)(data[z2][i2] & 0xff));
	}

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G;
	}

	public void getNeighbours(int index) {
		int z = index / (wh);
		int s = index % (wh);
		int x = s % w, y = s / w;
		for(int i = 0; i < neighbors.length; i++)
			neighbors[i] = -1;

		if(x > 1 && !flag[z * wh + s-1])
			neighbors[0] = (z * wh + s-1);
		if(x < w-1 && !flag[z * wh + s+1])
			neighbors[1] = (z * wh + s+1);
		if(y > 1 && !flag[z * wh + (y-1)*w+x])
			neighbors[2] = (z * wh + (y-1)*w+x);
		if(y < h-1 && !flag[z * wh + (y+1)*w+x])
			neighbors[3] = (z * wh + (y+1)*w+x);
		if(z > 1 && !flag[(z-1) * wh + s])
			neighbors[4] = (z-1) * wh + s;
		if(z < d-1 && !flag[(z+1) * wh + s])
			neighbors[5] = (z+1) * wh + s;
	}

	public ImagePlus checkLabelfield(ImagePlus labels) {
		vib.InterpolatedImage ilabels = new vib.InterpolatedImage(labels);
		vib.InterpolatedImage created = ilabels.cloneDimensionsOnly();

		int w = labels.getWidth(), h = labels.getHeight();
		int d = labels.getStackSize();
		ImagePlus result = createResult();

		for(int z = 0; z < d; z++) {
			int[] res_p = (int[])result.getStack().getPixels(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int c_idx = res_p[y * w + x] & 0x00ffffff;
					Cls c = classes[c_idx];
					created.set(x, y, z,
						ilabels.getNoCheck(c.originx, c.originy, c.originz));
				}
			}
		}

		return created.getImage();
	}


	public static final class Cls {
		public int min = 255;
		public int max = 0;
		public int mean = 0;
		public int vol = 0;
		public int cogx = 0;
		public int cogy = 0;
		public int cogz = 0;
		public int originx = 0;
		public int originy = 0;
		public int originz = 0;

		private final void add(int x, int y, int z, byte pixel) {
			int value = (int)(pixel & 0xff);
			if(value < min) min = value;
			if(value > max) max = value;
			mean += value;
			vol++;
			cogx += x;
			cogy += y;
			cogz += z;
		}

		private final void finished() {
			mean /= vol;
			cogx /= vol;
			cogy /= vol;
			cogz /= vol;
		}

		static Cls initFromString(String line) {
			Cls cls = new Cls();
			String[] tokens = line.split("\\s");
			cls.min     = Integer.parseInt(tokens[0]);
			cls.max     = Integer.parseInt(tokens[1]);
			cls.mean    = Integer.parseInt(tokens[2]);
			cls.vol     = Integer.parseInt(tokens[3]);
			cls.cogx    = Integer.parseInt(tokens[4]);
			cls.cogy    = Integer.parseInt(tokens[5]);
			cls.cogz    = Integer.parseInt(tokens[6]);
			cls.originx = Integer.parseInt(tokens[7]);
			cls.originy = Integer.parseInt(tokens[8]);
			cls.originz = Integer.parseInt(tokens[9]);
			return cls;
		}

		public String toString() {
			return min + " " + max + " " + mean + " "
				 + vol + " " + cogx + " " + cogy + " " + cogz
				 + " " + originx + " " + originy + " " + originz;
		}
	}

	public static void writeClasses(String path, Cls[] classes) throws Exception {
		PrintWriter out = new PrintWriter(new FileWriter(path));
		out.println(classes.length + " classes");
		for(int i = 0; i < classes.length; i++)
			out.println(classes[i]);
		out.close();
	}

	public static Cls[] readClasses(String path) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line = reader.readLine();
		int nClasses = Integer.parseInt(line.split("\\s")[0]);
		Cls[] classes = new Cls[nClasses];
		for(int i = 0; i < nClasses; i++)
			classes[i] = Cls.initFromString(reader.readLine());
		reader.close();
		return classes;
	}


	private static final class PriorityQueue {
		private IntArray[] arr = new IntArray[256];
		private int size = 0;

		public PriorityQueue() {}

		public final void add(int value, int cost) {
			if(arr[cost] == null)
				arr[cost] = new IntArray(10000);
			arr[cost].add(value);
			size++;
		}

		public final boolean isEmpty() {
			return size == 0;
		}

		public final int poll() {
			for(int i = 0; i < arr.length; i++) {
				if(arr[i] != null && !arr[i].isEmpty()) {
					int ret = arr[i].removeLast();
					size--;
					return ret;
				}
			}
			return -1;
		}

		public final void removeFromBucket(int value, int bucket) {
			if(arr[bucket] != null)
				if(arr[bucket].removeValue(value))
					size--;
		}
	}

	private static final class IntArray {
		private int[] array;
		private int size = 0;
		private int initCap;

		public IntArray(int cap) {
			initCap = cap;
			array = new int[cap];
		}

		public final int size() {
			return size;
		}

		public final boolean isEmpty() {
			return size == 0;
		}

		public final int get(int i) {
			if(i >= 0 && i < size)
				return array[i];
			return -1;
		}

		public final void add(int n) {
			if(size == array.length) {
				int additional = initCap / 5;
				additional = additional == 0 ? 1 : additional;
				int[] tmp = array;
				array = new int[array.length + additional];
				System.arraycopy(tmp, 0, array, 0, tmp.length);
			}
			array[size] = n;
			size++;
		}

		public final void removeIndex(int i) {
			if(i < 0 || i >= size)
				return;
			size--;
			for(int k = i; k < size; k++) 
				array[k] = array[k+1];
		}

		public final int removeLast() {
			size--;
			return array[size];
		}

		public final boolean removeValue(int v) {
			int k = 0;
			for(; k < size && array[k] != v; k++)
				;
			if(k == size)
				return false;
			size--;
			for(; k < size; k++) 
				array[k] = array[k+1];
			return true;
		}

		public final void set(int index, int value) {
			if(index >= 0 && index < size)
				array[index] = value;
		}

		public final void removeAll() {
			size = 0;
		}
	}
}
