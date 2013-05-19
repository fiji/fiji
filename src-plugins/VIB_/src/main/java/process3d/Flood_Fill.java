package process3d;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.Toolbar;

import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

public class Flood_Fill implements PlugIn {
	
	private static boolean debug = false;
	private static int tol = 0;

	public static final String MACRO_CMD =
		"var leftClick=16, alt=9;\n" +
		"macro 'Flood Fill Tool - C111O11ffC100T6c0aF' {\n" +
		" while (true) {\n" +
		"  getCursorLoc(x, y, z, flags);\n" +
		"  if (flags&leftClick==0) exit();\n" +
		"  call('process3d.Flood_Fill.fill', x,y,z);\n" +
		"  exit();" + 
		" }\n" +
		"}\n" +
		"\n";
	
	public void run(String arg) {
		MacroInstaller installer = new MacroInstaller();
        	installer.install(MACRO_CMD);
	}

	public synchronized static void fill(String x, String y, String z) {
		fill(Integer.parseInt(x),
				Integer.parseInt(y),
				Integer.parseInt(z));
	}

	public synchronized static void fill(int sx, int sy, int sz) {
		fill(IJ.getImage(), sx, sy, sz,
			Toolbar.getForegroundColor().getRGB());
	}

	public synchronized static void fill(ImagePlus imp,
					int sx, int sy, int sz, int color) {

		IJ.showStatus("Flood fill");
		long start = System.currentTimeMillis();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int wh = w * h;
		ImageProcessor[] b = new ImageProcessor[d];
		for(int z = 0; z < d; z++)
			b[z] = imp.getStack().getProcessor(z+1);

		Difference diff = null;
		if(b[0] instanceof ByteProcessor)
			diff = new DifferenceInt();
		else if(b[0] instanceof ShortProcessor)
			diff = new DifferenceInt();
		else if(b[0] instanceof FloatProcessor)
			diff = new DifferenceFloat();
		else if(b[0] instanceof ColorProcessor)
			diff = new DifferenceRGB();

		int colorToFill = b[sz].get(sx, sy);

		Stack stack = new Stack();
		stack.push(sz * wh + sy * w + sx);
		while(!stack.isEmpty()) {
			int p = stack.pop();
			int pz = p / wh;
			int pi = p % wh;
			int py = pi / w;
			int px = pi % w;

			int by = b[pz].get(px, py);
			if(diff.getDifference(by, colorToFill) > tol)
				continue;

			b[pz].set(px, py, color);

			int pzwh = pz * wh;
			if(px > 0 && b[pz].get(px-1, py) != color)
				stack.push(pzwh + pi - 1);
			if(px < w - 1 && b[pz].get(px+1, py) != color)
				stack.push(pzwh + pi + 1);
			if(py > 0 && b[pz].get(px, py-1) != color)
				stack.push(pzwh + pi - w);
			if(py < h - 1 && b[pz].get(px, py+1) != color)
				stack.push(pzwh + pi + w);
			if(pz > 0 && b[pz-1].get(px, py) != color)
				stack.push((pz - 1) * wh + pi);
			if(pz < d - 1 && b[pz+1].get(px, py) != color)
				stack.push((pz + 1) * wh + pi);
		}
		imp.updateAndDraw();
		long end = System.currentTimeMillis();
		System.out.println("Needed " + ((end - start)/1000) + " seconds");
		IJ.showStatus("");
	}

	static interface Difference {
		float getDifference(int p1, int p2);
	}

	static final class DifferenceInt implements Difference {
		public float getDifference(int p1, int p2) {
			return (float)Math.abs(p2 - p1);
		}
	}

	static final class DifferenceRGB implements Difference {
		public final float getDifference(int p1, int p2) {
			return (float)
				Math.abs((p1&0xff0000)>>16 - (p2&0xff0000)>>16) +
				Math.abs((p1&0xff00)>>8 - (p2&0xff00)>>8) +
				Math.abs((p1&0xff) - (p2&0xff));
		}
	}

	static final class DifferenceFloat implements Difference {
		public float getDifference(int p1, int p2) {
			return Math.abs(Float.intBitsToFloat(p2) -
				Float.intBitsToFloat(p1));
		}
	}

	static final class Stack {
		private int[] array;
		private int size;
		
		Stack() {
			array = new int[1000000];
			size = 0;
		}

		public void push(int n) {
			if(size == array.length) {
				int[] tmp = new int[array.length + 1000000];
				System.arraycopy(array, 0, tmp, 0, array.length);
				array = tmp;
			}
			array[size] = n;
			size++;
		}

		public int pop() {
			size--;
			return array[size];
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}
	}
}
