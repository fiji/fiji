package vib;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;

import ij.gui.GenericDialog;
import ij.WindowManager;

/**
 * Weiguo Lu et al (2004), Fast free-form deformable registration via calculus
 * of variations.
 */
public class Deformable_Registration implements PlugIn {

	private ImagePlus model;
	private ImagePlus template;

	private byte[][] modelP;
	private byte[][] templateP;

	private int w, h, d;

	private GradientField gf;
	private DisplacementField df;

	private static final float LAMBDA = 100f;
	private static final int ITERATIONS = 100;
	private static final int STARTLEVEL = 1;
	private static final int STOPLEVEL = 1;

	private int level = STARTLEVEL;

	public void run(String arg) {
		int[] ids = WindowManager.getIDList();
		String[] images = new String[ids.length];
		for(int i = 0; i < ids.length; i++) {
			images[i] = WindowManager.getImage(ids[i]).getTitle();
		}
		GenericDialog gd = new GenericDialog("Deformable Registration");
		gd.addChoice("Model: ", images, images[0]);
		gd.addChoice("Template: ", images, images[0]);
		gd.addCheckbox("Load displacement", false);
		gd.addCheckbox("Register", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		model = WindowManager.getImage(gd.getNextChoice());
		template = WindowManager.getImage(gd.getNextChoice());

		init();
		if(gd.getNextBoolean()) {
			df.load(0);
			df.load(1);
			df.load(2);
		}
		if(gd.getNextBoolean()) {
			register();
		}
		apply();
	}

	private float A(int x, int y, int z) {
		return (float)(templateP[z][y*w+x] & 0xff);
	}

	private float interpolate(float x, float y, float z, float v0, float v1,
		float v2, float v3, float v4, float v5, float v6, float v7, int grid) {

		float dx = x - ((int)x)/grid;
		float dy = y - ((int)y)/grid;
		float dz = z - ((int)z)/grid;

		float i1 = v0 * (grid-dz) + v4 * dz; 
		float i2 = v2 * (grid-dz) + v6 * dz;
		float j1 = v1 * (grid-dz) + v5 * dz;
		float j2 = v3 * (grid-dz) + v7 * dz;

		float w1 = i1 * (grid-dy) + i2*dy;
		float w2 = j1 * (grid-dy) + j2*dy;

		return w1 * (grid-dx) + w2 * dx;
	}
	
	private float interpolate(float x, float y, float z, float v0, float v1,
		float v2, float v3, float v4, float v5, float v6, float v7) {

		return interpolate(x, y, z, v0, v1, v2, v3, v4, v5, v6, v7, 1);
	}

	private float B(int xp, int yp, int zp) {
		float z = zp + df.u(xp, yp, zp, 0);
		float y = yp + df.u(xp, yp, zp, 1);
		float x = xp + df.u(xp, yp, zp, 2);

		int xl = (int)x;
		int yl = (int)y;
		int zl = (int)z;

		return interpolate(x, y, z,
			getB(xl, yl, zl), 
			getB(xl+level, yl, zl),
			getB(xl, yl+level, zl),
			getB(xl+level, yl+level, zl),
			getB(xl, yl, zl+level),
			getB(xl+level, yl, zl+level),
			getB(xl, yl+level, zl+level),
			getB(xl+level, yl+level, zl+level),
			level);
	}

	private float getB(int x, int y, int z) {
		if(z < 0) z = 0;
		if(z >= d) z = d-1;
		if(x < 0) x = 0;
		if(x >= w) x = w-1;
		if(y < 0) y = 0;
		if(y >= h) y = h-1;
		return (float)(modelP[z][y*w+x] & 0xff);
	}

	private final void update(int x, int y, int z, int n) {
		float g = gf.g(x, y, z, n);
		float L = LAMBDA * df.del2u(x, y, z, n) - 
				(B(x, y, z)-A(x, y, z)) * g;
		df.add(x, y, z, n, L / (LAMBDA + g * g));
		for(int iz = 0; iz < level; iz++)
			for(int iy = 0; iy < level; iy++)
				for(int ix = 0; ix < level; ix++)
					df.set(x+ix, y+iy, z+iz, n,
						df.u(x, y, z, n));
	}

	private void register() {
		int iterations = ITERATIONS;
		for(; level >= STOPLEVEL; level/=2) {
			IJ.showStatus("Level " + level);
			registerLevel(iterations);
			iterations *= 2;
		}
		level = STOPLEVEL;
	}

	private void registerLevel(int iterations) {
		for(int k = 0; k < iterations; k++) {
			for(int z = 0; z <= d-level; z+=level) {
				for(int y = 0; y <= h-level; y+=level) {
					for(int x = 0; x <= w-level; x+=level) {
						for(int n = 0; n < 3; n++) {
							update(x, y, z, n);
						}
					}
				}
			}
			IJ.showProgress(k, iterations);
		}
// 		df.interpolateU();
	}

	private void apply() {
		ImageStack stack = new ImageStack(w, h);
		byte[][] b = new byte[d][w*h];
		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					b[z][y*w+x] = (byte)B(x, y, z);
				}
			}
			stack.addSlice("", new ByteProcessor(w, h, b[z], null));
		}
		new ImagePlus("result", stack).show();
		df.show(0);
		df.show(1);
		df.show(2);
	}

	private void init() {
		w = template.getWidth();
		h = template.getHeight();
		d = template.getStackSize();

		templateP = new byte[d][];
		modelP = new byte[d][];
		for(int z = 0; z < d; z++) {
			templateP[z] = (byte[])template.getStack()
					.getProcessor(z+1).getPixels();
			modelP[z] = (byte[])model.getStack()
					.getProcessor(z+1).getPixels();
		}

		gf = new GradientField();
		df = new DisplacementField();
	}

	private class DisplacementField {
		private float[][][] u;

		private DisplacementField() {
			u = new float[3][d][w*h];
		}

		private float u(int x, int y, int z, int n) {
			return u[n][z][y*w+x];
		}

		private void add(int x, int y, int z, int n, float v) {
			u[n][z][y*w+x] += v;
		}

		private void set(int x, int y, int z, int n, float v) {
			u[n][z][y*w+x] = v;
		}

		private void show(int n) {
			ImageStack stack = new ImageStack(w, h);
			for(int z = 0; z < d; z++) {
				stack.addSlice("", new FloatProcessor(
					w, h, u[n][z], null));
			}
			new ImagePlus("Displacement_dim" + n, stack).show();
		}

		private void load(int n) {
			ImagePlus image = WindowManager
				.getImage("Displacement_dim" + n);
			if(image == null)
				return;
			for(int z = 0; z < d; z++) {
				u[n][z] = (float[])image.getStack()
					.getProcessor(z+1).getPixels();
			}
		}

		private float del2u(int x, int y, int z, int n) {

			int zm1 = z >= level ? z-level : z+level;
			int xm1 = x >= level ? x-level : x+level;
			int ym1 = y >= level ? y-level : y+level;

			int zp1 = z < d-level ? z+level : z-level;
			int xp1 = x < w-level ? x+level : x-level;
			int yp1 = y < h-level ? y+level : y-level;

			return (u[n][z][y*w+xp1] +
				u[n][z][y*w+xm1] +
				u[n][z][yp1*w+x] + 
				u[n][z][ym1*w+x] +
				u[n][zp1][y*w+x] + 
				u[n][zm1][y*w+x]) / 6.0f - 
				u[n][z][y*w+x];
		}

		private final void interpolateU() {
			for(int z = 0; z <= d-2*level; z+=level) {
				for(int y = 0; y <= h-2*level; y+=level) {
					for(int x = 0;x <= w-2*level;x+=level) {
						for(int n = 0; n < 3; n++) {
							interpolateU(x,y,z,n);
						}
					}
				}
			}
		}

		private final void interpolateU(int x, int y, int z, int n) {
			for(int iz = 0; iz < level; iz++) {
				for(int iy = 0; iy < level; iy++) {
					for(int ix = 0; ix < level; ix++) {
						u[n][z+iz][(y+iy)*w + x + ix] = 
							interpolate(x+ix, y+iy, z+iz,
							u[n][z][y*w+x],
							u[n][z][y*w+x+level],
							u[n][z][(y+level)*w+x],
							u[n][z][(y+level)*w+x+level],
							u[n][z+level][y*w+x],
							u[n][z+level][y+level+x+level],
							u[n][z+level][(y+level)*w+x],
							u[n][z+level][(y+level)*w+x+level]);
					}
				}
			}
		}
	}

	private class GradientField {
		private float[][][] g;

		private GradientField() {
			g = new float[3][d][w*h];
			gradX();
			gradY();
			gradZ();
		}

		private float g(int xp, int yp, int zp, int n) {
			float z = zp + df.u(xp, yp, zp, 0);
			float y = yp + df.u(xp, yp, zp, 1);
			float x = xp + df.u(xp, yp, zp, 2);

			int xl = (int)x;
			int yl = (int)y;
			int zl = (int)z;

			return interpolate(x, y, z,
				getG(xl, yl, zl, n), 
				getG(xl+level, yl, zl, n),
				getG(xl, yl+level, zl, n),
				getG(xl+level, yl+level, zl, n),
				getG(xl, yl, zl+level, n),
				getG(xl+level, yl, zl+level, n),
				getG(xl, yl+level, zl+level, n),
				getG(xl+level, yl+level, zl+level, n),
				level);
		}

		private float getG(int x, int y, int z, int n) {
			if(z < 0) z = 0;
			if(z >= d) z = d-1;
			if(x < 0) x = 0;
			if(x >= w) x = w-1;
			if(y < 0) y = 0;
			if(y >= h) y = h-1;
			return g[n][z][y*w + x];
		}

		private void gradX() {
			for(int z = 0; z < d; z++) {
				byte[] p = (byte[])model.getStack()
						.getProcessor(z+1).getPixels();
				for(int y = 0; y < h; y++) {
					g[2][z][y*w] = (float)(
						(int)(p[y*w+1]&0xff) - 
						(int)(p[y*w]&0xff));
					for(int x = 1; x < w-1; x++) {
						g[2][z][y*w+x] = (float)(
							(int)(p[y*w+x+1]&0xff) -
							(int)(p[y*w+x-1]&0xff))
							/2.0f;
					}
					g[2][z][y*w+w-1] = (float)(
						(int)(p[y*w+w-1]&0xff) - 
						(int)(p[y*w+w-2]&0xff));
				}
			}
		}

		private void gradY() {
			for(int z = 0; z < d; z++) {
				byte[] p = (byte[])model.getStack()
					.getProcessor(z+1).getPixels();
				for(int x = 0; x < w; x++) {
					g[1][z][x] = (float)(
						(int)(p[w+x]&0xff) -
						(int)(p[x]&0xff));
				}
				for(int y = 1; y < h-1; y++) {
					for(int x = 0; x < w; x++) {
						g[1][z][y*w+x] = (float)(
							(int)(p[(y+1)*w+x]&0xff) -
							(int)(p[(y-1)*w+x]&0xff))/2.0f;
					}
				}
				for(int x = 0; x < w; x++) {
					g[1][z][(h-1)*w+x] = (float)(
						(int)(p[(h-1)*w+x]&0xff) -
						(int)(p[(h-2)*w+x]&0xff));
				}
			}
		}

		private void gradZ() {
			ImageStack s = model.getStack();
			byte[] p = (byte[])s.getProcessor(1).getPixels();
			byte[] pp1 = (byte[])s.getProcessor(2).getPixels();
			for(int i = 0; i < w*h; i++) {
				g[0][0][i] = (float)(
					(int)(pp1[i]&0xff) - 
					(int)(p[i]&0xff));
			}
			for(int z = 1; z < d-1; z++) {
				byte[] pm1 = (byte[])s.getProcessor(z).getPixels();
				p = (byte[])s.getProcessor(z+1).getPixels();
				pp1 = (byte[])s.getProcessor(z+2).getPixels();
				for(int i = 0; i < w*h; i++) {
					g[0][z][i] = (float)(
						(int)(pp1[i]&0xff) - 
						(int)(pm1[i]&0xff))/2.0f;
				}
			}
			byte[] pm1 = (byte[])s.getProcessor(d-1).getPixels();
			p = (byte[])s.getProcessor(d).getPixels();
			for(int i = 0; i < w*h; i++) {
				g[0][d-1][i] = (float)(
					(int)(p[i]&0xff) - 
					(int)(pm1[i]&0xff));
			}
		}
	}
}
