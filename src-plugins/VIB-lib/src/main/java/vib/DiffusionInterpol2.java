/***************************************************************
 *
 * DiffusionInterpol2
 *
 * ported from Amira module
 *
 ***************************************************************/

package vib;

import amira.AmiraParameters;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

public class DiffusionInterpol2 {
	ImagePlus image;
	FloatMatrix[] labelTransformations;
	FloatMatrix globalTransformation;
	boolean reuse;
	boolean remember;
	float tolerance;

	public void initialize(ImagePlus image, ImagePlus templateLabels, 
			ImagePlus model, FloatMatrix[] labelTransformations, boolean reuse, 
			boolean remember, float tolerance) {

		this.image = image;
		this.template = new InterpolatedImage(image);
		this.templateLabels = new InterpolatedImage(templateLabels);
		this.model = new InterpolatedImage(model);
		this.labelTransformations = labelTransformations;
		this.reuse = reuse;
		this.remember = remember;
		this.tolerance = tolerance;
	}

	static float[][] savedDisplace;

	public void doit() {
		try {
			reuse = savedDisplace != null ? reuse : false;
			FloatMatrix fromTemplate = FloatMatrix.fromCalibration(template.image);
			FloatMatrix toModel = FloatMatrix.fromCalibration(model.image).inverse();
	
			for (int i = 1; i < labelTransformations.length; i++) {
				if (labelTransformations[i] != null)
					labelTransformations[i] = toModel.times(
						labelTransformations[i].inverse().times(fromTemplate));
			}
			labelTransformations[0] = null; // Exterior does not matter

			globalTransformation = FloatMatrix.average(labelTransformations);
			if (reuse)
				displace = savedDisplace;
			else {
				if (savedDisplace != null) {
					// give the garbage collector a chance
					savedDisplace = null;
					System.gc();
					System.gc();
				}

				displace = new float[template.d][];
				for (int k = 0; k < template.d; k++)
					displace[k] = new float[3
						* template.w * template.h];

				init();
				iterate(tolerance, false);
			}

			apply();

			savedDisplace = (remember ? displace : null);
		} catch(OutOfMemoryError e) {
			System.err.println("Out of Memory: DiffusionInterpol2 " + 
									ij.Macro.getOptions());
			e.printStackTrace();
			throw e;
		}
	}

	InterpolatedImage template;
	InterpolatedImage templateLabels;
	InterpolatedImage model;

	float[][] displace;

	int level;
	float accumX, accumY, accumZ;
	float curX, curY, curZ;

	final private void accumAdd(int i, int j, int k) {
		accumX += displace[k][3 * (j * template.w + i) + 0];
		accumY += displace[k][3 * (j * template.w + i) + 1];
		accumZ += displace[k][3 * (j * template.w + i) + 2];
	}

	final void iterateInnerPart(int i, int j, int k) {
		if (templateLabels.getNoInterpol(i, j, k) != 0)
			return;

		accumX = accumY = accumZ = 0;

		curX = displace[k][3 * (j * template.w + i) + 0];
		curY = displace[k][3 * (j * template.w + i) + 1];
		curZ = displace[k][3 * (j * template.w + i) + 2];

		if (k - level >= 0) accumAdd(i, j, k - level); else accumAdd(i, j, k + level);
		if (j - level >= 0) accumAdd(i, j - level, k); else accumAdd(i, j + level, k);
		if (i - level >= 0) accumAdd(i - level, j, k); else accumAdd(i + level, j, k);
		if (k + level < template.d) accumAdd(i, j, k + level); else accumAdd(i, j, k - level);
		if (j + level < template.h) accumAdd(i, j + level, k); else accumAdd(i, j - level, k);
		if (i + level < template.w) accumAdd(i + level, j, k); else accumAdd(i - level, j, k);

		accumX /= 6;
		accumY /= 6;
		accumZ /= 6;

		float delta = Math.abs(curX - accumX) +
			Math.abs(curY - accumY) +
			Math.abs(curZ - accumZ);

		if (delta > 0) {
			displace[k][3 * (j * template.w + i) + 0] = accumX;
			displace[k][3 * (j * template.w + i) + 1] = accumY;
			displace[k][3 * (j * template.w + i) + 2] = accumZ;

			if (delta > 0.1) changed++;
			if (delta > 3) changed++;
			if (delta > mdelta)
				mdelta=delta;
		}

	}

	int changed, bchanged;
	float mdelta;

	float iterateNormal() {
		changed = bchanged = 0;
		mdelta = 0;

		int maxW = template.w - 1 - ((template.w - 1) % level);
		int maxH = template.h - 1 - ((template.h - 1) % level);
		int maxD = template.d - 1 - ((template.d - 1) % level);

		for (int k = maxD; k >= 0; k -= level) {
			for (int j = maxH; j >= 0; j -= level)
				for (int i = maxW; i >= 0; i -= level)
					iterateInnerPart(i, j, k);
			IJ.showProgress(1 * template.d - k, 2 * template.d);
		}

		for (int k = 0; k < template.d; k += level) {
			for (int j = 0; j < template.h; j += level)
				for (int i = 0; i < template.w; i += level)
					iterateInnerPart(i, j, k);
			IJ.showProgress(template.d + k + 1, template.d * 2);
		}

		IJ.showProgress(1, 1);

		return mdelta;
	}

	final float MAGIC = (float)40711.22;

	void propagateInitial() {
		VIB.showStatus("initializing domain (1/2)");

		for (int k = 0; k < template.d; k++) {
			IJ.showProgress(k, template.d * 2);
			for (int j = 0; j < template.h; j++)
				for (int i = 0; i < template.w; i++)
					if (displace[k][3 * (j * template.w + i)] == MAGIC) {
						if (k > 0 && displace[k - 1][3 * (j * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j, k - 1);
						else if (j > 0 && displace[k][3 * ((j - 1) * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j - 1, k);
						else if (i > 0 && displace[k][3 * (j * template.w + i - 1)] != MAGIC)
							copyDisplace(i, j, k, i - 1, j, k);
						else if (i > 0 && j > 0 && displace[k][3 * ((j - 1) * template.w + i - 1)] != MAGIC)
							copyDisplace(i, j, k, i - 1, j - 1, k);
						else if (i > 0 && k > 0 && displace[k - 1][3 * (j * template.w + i - 1)] != MAGIC)
							copyDisplace(i, j, k, i - 1, j, k - 1);
						else if (j > 0 && k > 0 && displace[k - 1][3 * ((j - 1) * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j - 1, k - 1);
						else if (i > 0 && j > 0 && k > 0 && displace[k - 1][3 * ((j - 1) * template.w + i - 1)] != MAGIC)
							copyDisplace(i, j, k, i - 1, j - 1, k - 1);
					}
		}

		VIB.showStatus("initializing domain (2/2)");

		for (int k = template.d - 1; k >= 0; k--) {
			IJ.showProgress(2 * template.d - k, 2 * template.d);
			for (int j = template.h - 1; j >= 0; j--)
				for (int i = template.w - 1; i >= 0; i--)
					if (displace[k][3 * (j * template.w + i)] == MAGIC) {
						if (k < template.d - 1 && displace[k + 1][3 * (j * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j, k + 1);
						else if (j < template.h - 1  && displace[k][3 * ((j + 1) * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j + 1, k);
						else if (i < template.w - 1  && displace[k][3 * (j * template.w + i + 1)] != MAGIC)
							copyDisplace(i, j, k, i + 1, j, k);
						else if (i < template.w - 1  && j < template.h - 1 && displace[k][3 * ((j + 1) * template.w + i + 1)] != MAGIC)
							copyDisplace(i, j, k, i + 1, j + 1, k);
						else if (i < template.w - 1  && k < template.d - 1 && displace[k + 1][3 * (j * template.w + i + 1)] != MAGIC)
							copyDisplace(i, j, k, i + 1, j, k + 1);
						else if (k < template.d - 1  && j < template.h - 1 && displace[k + 1][3 * ((j + 1) * template.w + i)] != MAGIC)
							copyDisplace(i, j, k, i, j + 1, k + 1);
						else if (i < template.w - 1  && j < template.h - 1 && k < template.d - 1 && displace[k + 1][3 * ((j + 1) * template.w + i + 1)] != MAGIC)
							copyDisplace(i, j, k, i + 1, j + 1, k + 1);
					}
		}

		IJ.showProgress(1, 1);

		for (int k = 0; k < template.d; k++)
			for (int j = 0; j < template.h; j++)
				for (int i = 0; i < template.w; i++)
					if (displace[k][3 * (j * template.w + i)] == MAGIC)
						throw new RuntimeException("Nonono: "+i+", "+j+", "+k);


	}

	void copyDisplace(int di, int dj, int dk, int i, int j, int k) {
		displace[dk][3 * (dj * template.w + di) + 0] = displace[k][3 * (j * template.w + i) + 0];
		displace[dk][3 * (dj * template.w + di) + 1] = displace[k][3 * (j * template.w + i) + 1];
		displace[dk][3 * (dj * template.w + di) + 2] = displace[k][3 * (j * template.w + i) + 2];
	}

	void iterate(float tolerance, boolean fine) {
		VIB.showStatus("diffusion in progress");

		level = fine ? 1 : 16;

		for ( ; level>=1 ; level /= 2) {
			for (int i = 1; true; i++) {
				float tol = tolerance / level;

				VIB.showStatus("Level " + level + ", Iteration " + i + " (delta was "+mdelta+")");

				if (iterateNormal() < tol)
					break;

			}

			if (level>1) {
				VIB.showStatus("Level jump " + level + " -> " + (level/2));
				for (int k = 0 ; k < template.d; k += level) {
					for (int j = 0; j < template.h ; j += level)
						for (int i = 0; i < template.w; i += level) {
							int kMax = (k + level >= template.d ? template.d : k + level);
							int jMax = (j + level >= template.h ? template.h : j + level);
							int iMax = (i + level >= template.w ? template.w : i + level);
							for (int dk = k; dk < kMax; dk++)
								for (int dj = j; dj < jMax; dj++)
									for (int di = i; di < iMax; di++) {
										if (templateLabels.getNoInterpol(di, dj, dk) == 0)
											copyDisplace(di, dj, dk, i, j, k);
									}
						}
				}
			}
		}
	}

	void apply() {
		VIB.showStatus("Applying displacement");

		for (int k = 0; k < template.d; k++) {
			for (int j = 0; j < template.h; j++)
				for(int i = 0; i < template.w; i++) {
					globalTransformation.apply(i, j, k);
					template.set(i, j, k,
						(byte)(int)model.interpol.get(displace[k][3 * (j * template.w + i) + 0] + globalTransformation.x,
						displace[k][3 * (j * template.w + i) + 1] + globalTransformation.y,
						displace[k][3 * (j * template.w + i) + 2] + globalTransformation.z));
				}
			IJ.showProgress(k + 1, template.d);
		}
		new AmiraParameters(model.image).setParameters(template.image,
				false);
	}

	void init() {
		VIB.showStatus("Initializing displacement");

		for (int k = 0; k < template.d; k++) {
			IJ.showProgress(k, template.d);
			for (int j = 0; j < template.h; j++)
				for (int i = 0; i < template.w; i++) {
					int material = templateLabels.
						getNoInterpol(i, j, k);
					if (material > 0 && material < labelTransformations.length
							&& labelTransformations[material] != null) {
						labelTransformations[material].apply(i, j, k);
						globalTransformation.apply(i, j, k);
						displace[k][3 * (j * template.w + i) + 0] = labelTransformations[material].x - globalTransformation.x;
						displace[k][3 * (j * template.w + i) + 1] = labelTransformations[material].y - globalTransformation.y;
						displace[k][3 * (j * template.w + i) + 2] = labelTransformations[material].z - globalTransformation.z;
					} else {
						displace[k][3 * (j * template.w + i) + 0] = 0;
						displace[k][3 * (j * template.w + i) + 1] = 0;
						displace[k][3 * (j * template.w + i) + 2] = 0;
					}
				}
		}

		IJ.showProgress(1, 1);

		propagateInitial();
	}

    public ImagePlus[] getDisplacementField( ) {
        ImagePlus[] results = new ImagePlus[3];
		ImageStack[] stack = new ImageStack[3];
		for (int i = 0; i < 3; i++)
			stack[i] = new ImageStack(template.w, template.h);
		for (int k = 0; k < template.d; k++) {
			float[][] slice = new float[3][];
			for (int i = 0; i < 3; i++)
				slice[i] = new float[template.w * template.h];
			for (int j = 0; j < template.w * template.h; j++)
//if (getLabel(j % w, j / w, k) != 0)
				for (int i = 0; i < 3; i++)
					slice[i][j] = displace[k][3 * j + i];
			for (int i = 0; i < 3; i++)
				stack[i].addSlice("", slice[i]);
		}
		for (int i = 0; i < 3; i++)
			results[i] = new ImagePlus("displace "+i, stack[i]);
        return results;
    }

	private void debugDisplace() {
        ImagePlus [] xyz = getDisplacementField();
        for( int i = 0; i < 3; ++i )
            xyz[i].show();
		throw new RuntimeException("debugDisplace");        
	}
}
