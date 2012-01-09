/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

/*
 * TODO:
 * - make translateMax/angleMax be dependent on the data
 * - make Choice auto-select when chosing the other neuropil
 *   (refactor center determination)
 */

/*
    A note on what the levels mean in this plugin:

     level (the start level) > stopLevel
     
 level = 4 implies eighth size i.e. width * ( 2 ** (1-N) ) where N = 4
 ...
 level = 2 implies half size   i.e. width * ( 2 ** (1-N) ) where N = 2

    Other points:

      * doRegister(int level) actually takes a level parameter
        which is startLevel - level.]

      * The guessed start level is the level you'd get if you
        keep halfing the size while the width is still greater than
	20 pixels.
 */

import amira.AmiraParameters;

import distance.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.text.TextWindow;
import java.awt.Choice;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import math3d.Point3d;
import pal.math.*;

public class RigidRegistration {
	String[] materials1, materials2;
	protected boolean verbose = false;
        
	public static int guessLevelFromWidth( int width ) {
		int level = 0;
		while((width >> level) > 20)
			level++;
		return level;
	}

	/**
	 *
	 *
	 * @param trans TransformedImage, needs the .measure setup correctly 
	 * along with the two images setup
	 * @param materialBBox  can be empty
	 * @param mat1  used in material distance measures
	 * @param mat2  used in material distance measures
	 * @param initial  can be empty, then 24 orientations are tried
	 * @param noOptimization
	 * @param level start level
	 * @param stopLevel end level
	 * @param tolerance
	 * @param nInitialPositions number of promising initial positions 
	 * to optimize further
	 * @param showTransformed  boolean whether to show the resultant 
	 * transofrmed image or not
	 * @param showDifferenceImage boolean whether to show the resultant 
	 * transofrmed image or not
	 * @return The best FastMatrix
	 */
	public FastMatrix rigidRegistration(
			TransformedImage trans,
			String materialBBox,
			String initial,
			int mat1,
			int mat2,
			boolean noOptimization,
			int level,
			int stopLevel,
			double tolerance,
			int nInitialPositions,
			boolean showTransformed,
			boolean showDifferenceImage, 
			boolean fastButInaccurate,
                        ArrayList<ImagePlus> alsoTransform ) {

/*
		System.out.println("        materialBBox: "+materialBBox);
		System.out.println("             initial: "+initial);
		System.out.println("                mat1: "+mat1);
		System.out.println("                mat2: "+mat2);
		System.out.println("      noOptimization: "+noOptimization);
		System.out.println("               level: "+level);
		System.out.println("           stopLevel: "+stopLevel);
		System.out.println("           tolerance: "+tolerance);
		System.out.println("   nInitialPositions: "+nInitialPositions);
		System.out.println("     showTransformed: "+showTransformed);
		System.out.println(" showDifferenceImage: "+showDifferenceImage);
		System.out.println("   fastButInaccurate: "+fastButInaccurate);
		System.out.println("       alsoTransform: "+alsoTransform);
*/

		if (mat1 >= 0)
			trans.narrowSearchToMaterial(mat1, 10);

		Point3d center = null;
		if (materialBBox!=null && !materialBBox.equals(""))
			center = parseMaterialBBox(trans, materialBBox);

		double[] params = null;
		if (initial!=null && ! initial.equals("")) {
			params = new double[9];
			try {
				if (center == null)
					center = getCenter(trans.transform,
							 mat1);

				FastMatrix m = FastMatrix.parseMatrix(
						initial);
				if (mat2 >= 0) {
					/*
					 * If registering labelfields, make
					 * sure that the center of gravity
					 * is transformed onto orig's
					 * center of gravity.
					 */
					m.apply(center);
					Point3d p = getCenter(trans.orig, mat2);
					p = p.minus(m.getResult());
					m = FastMatrix.translate(p.x, p.y, p.z)
						.times(m);
				}
				m.guessEulerParameters(params, center);
			} catch(Exception e) {
				StringTokenizer t =
					new StringTokenizer(initial);
				for (int i = 0; i < 9; i++)
					params[i] = Double.parseDouble(
							t.nextToken());
			}
		}

		FastMatrix matrix;
		if (!noOptimization) {
			Optimizer opt = fastButInaccurate 
				? new FastOptimizer(trans, level, stopLevel, tolerance, verbose)
				: new Optimizer(trans, level, stopLevel, tolerance, verbose);
			opt.eulerParameters = params;

			if(opt.eulerParameters == null){
				FastMatrix [] results = 
					new FastMatrix[nInitialPositions];
				double badnees[] = 
					new double[nInitialPositions];

				for(int i = 0; i < nInitialPositions; i++){
					opt.eulerParameters = null;
					results[i] = opt.doRegister(
							level - stopLevel, i);
					//todo probably recalculated wastefully	
					badnees[i] = opt.calculateBadness(
							results[i]);   
				}

				//now select the best
				double best = Double.MAX_VALUE;
				int bestIndex = -1;
				for (int i = 0; i < badnees.length; i++) {

					if(badnees[i] < best){
						best = badnees[i];
						bestIndex = i;
					}
				}


				matrix = results[bestIndex];
				if(verbose) {
					System.out.println("winner was " + 
							(bestIndex+1) + 
							" with matrix" + matrix);
					System.out.println("... and score: "+badnees[bestIndex]);
				}
				

			}else{
				matrix = opt.doRegister(level - stopLevel);
			}


			opt = null;
		} else {
			try {
				matrix = FastMatrix.parseMatrix(initial);
			} catch(Exception e) {
				StringTokenizer t =
					new StringTokenizer(initial);
				for (int i = 0; i < 9; i++)
					params[i] = Double.parseDouble(
							t.nextToken());
				matrix = RegistrationOptimizer.getEulerMatrix(
					params);
			}
		}

		trans.setTransformation(matrix);
		if(verbose) 
			VIB.println(matrix.toString());
		if (showTransformed)
			trans.getTransformed().show();
		if (showDifferenceImage)
			trans.getDifferenceImage().show();

                ImagePlus template=trans.getTemplate();
                PixelPairs measure = trans.measure;
                
		// give the garbage collector a chance:
		trans = null;
		System.gc();
		System.gc();
                
                if(alsoTransform!=null) {
                    for(Iterator<ImagePlus> i=alsoTransform.iterator();
                        i.hasNext(); ) {
                        
                        ImagePlus toTransform=i.next();
                        TransformedImage transOther=new TransformedImage(
                                template,
                                toTransform);
                        transOther.measure = measure;
                        transOther.measure.reset();
                        transOther.setTransformation(matrix);

                        ImagePlus result=transOther.getTransformed();

                        transOther = null;
                        System.gc();
                        System.gc();
                        
                        result.setTitle("Transformed "+toTransform.getTitle());
                        result.show();
                    }
                }
                
		return matrix;
	}

	Point3d parseMaterialBBox(TransformedImage trans, String bbox) {
		StringTokenizer t = new StringTokenizer(bbox);
		try {
			Point3d center = new Point3d();
			center.x = Double.parseDouble(t.nextToken());
			center.y = Double.parseDouble(t.nextToken());
			center.z = Double.parseDouble(t.nextToken());
			double x0, x1, y0, y1, z0, z1;
			x0 = Double.parseDouble(t.nextToken());
			x1 = Double.parseDouble(t.nextToken());
			y0 = Double.parseDouble(t.nextToken());
			y1 = Double.parseDouble(t.nextToken());
			z0 = Double.parseDouble(t.nextToken());
			z1 = Double.parseDouble(t.nextToken());
			FastMatrix toOrig = trans.fromOrig.inverse();
			toOrig.apply(x0, y0, z0);
			Point3d llf = toOrig.getResult();
			toOrig.apply(x1, y1, z1);
			Point3d urb = toOrig.getResult();
			trans.narrowBBox((int)llf.x - 10,
					(int)Math.ceil(urb.x) + 10,
					(int)llf.y - 10,
					(int)Math.ceil(urb.y) + 10,
					(int)llf.z - 10,
					(int)Math.ceil(urb.z) + 10);
			return center;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Point3d getCenter(InterpolatedImage ii, int mat1) {
		if (mat1 >= 0)
			return ii.getCenterOfGravity(mat1);
		else
			return ii.getCenterOfGravity();
	}

	static FastMatrix lastResult;

	public static String getLastResult() {
		return lastResult.toStringForAmira();
	}

	static class Optimizer extends RegistrationOptimizer {
		TransformedImage t;
		int start, stop;
		double tolerance;

		public Optimizer(TransformedImage trans,
				 int startLevel, int stopLevel,
				 double tol,boolean verbose) {
			this.verbose = verbose;
			if (stopLevel < 2)
				t = trans;
			else
				t = trans.resample(1 << (stopLevel - 1));
			start = startLevel;
			stop = stopLevel;
			tolerance = tol;
		}

		public FastMatrix doRegister(int level) {
			return doRegister(level, 0);
		}

		/**
		 *
		 * @param level
		 * @param initialGuessPlace 0 uses the best distance measure from the initial set of guesses if EulerParams were not supplied
		 * @return
		 */

		public FastMatrix doRegister(int level, int initialGuessPlace) {
			if (level > 0) {
				TransformedImage backup = t;
				t = t.resample(2);
				t.setTransformation(doRegister(level - 1));
				//t.getTransformed().show();
				//t.getDifferenceImage().show();
				t = backup;
				System.gc();
				System.gc();
			}
			if(verbose)
				VIB.println("level is " + (start - level));

			double factor = (1 << (start - level));
			int minFactor = (1 << start);
			angleMax = Math.PI / 4 * factor / minFactor;
			translateMax = 20.0 * factor / minFactor;
			if(eulerParameters == null){
				eulerParameters = 
				searchInitialEulerParams()[initialGuessPlace];
			}
			return doRegister(tolerance / factor);
		}

		public void getInitialCenters() {
			if (t.measure instanceof distance.TwoValues) {
				distance.TwoValues d = (distance.TwoValues)
					t.measure;
				int m1 = d.getMaterial(0);
				int m2 = d.getMaterial(1);
				transC = t.transform.getCenterOfGravity(m1);
				origC = t.orig.getCenterOfGravity(m2);
			} else {
				transC = t.transform.getCenterOfGravity();
				origC = t.orig.getCenterOfGravity();
			}
		}

		public double calculateBadness(FastMatrix matrix) {
			t.setTransformation(matrix);
			return t.getDistance();
		}
	}
	
	static class FastOptimizer extends Optimizer {
		private int centerX, centerY, centerZ;
		
		public FastOptimizer(TransformedImage trans,
				int startLevel, int stopLevel,
				double tol, boolean verbose) {
			super(trans, startLevel, stopLevel, tol, verbose);
			current = new Point3d();
		}

		@Override
		public void getInitialCenters(){
			super.getInitialCenters();
			Calibration calib = t.orig.getImage().getCalibration();
			centerX = (int)Math.round((origC.x - calib.xOrigin) 
							/ calib.pixelWidth);
			centerY = (int)Math.round((origC.y - calib.yOrigin) 
							/ calib.pixelHeight);
			centerZ = (int)Math.round((origC.z - calib.zOrigin) 
							/ calib.pixelDepth);
		}


		private Point3d start, stop, current;
		
		public void initStartStop(int i0, int j0, int k0, 
				int i1, int j1, int k1) {
			t.matrix.apply(i0, j0, k0);
			start = t.matrix.getResult();
			t.matrix.apply(i1, j1, k1);
			stop = t.matrix.getResult().minus(start);
		}

		public void calculateCurrent(int i, int total) {
			current.x = start.x + i * stop.x / total;
			current.y = start.y + i * stop.y / total;
			current.z = start.z + i * stop.z / total;
		}
			
		@Override
		public double calculateBadness(FastMatrix matrix) {
			t.setTransformation(matrix);
			t.measure.reset();
			for (int i = 0; i < t.orig.w; i++) {
				initStartStop(i, 0, centerZ, 
							i, t.orig.h, centerZ);
				for (int j = 0; j < t.orig.h; j++) {
					calculateCurrent(j, t.orig.h);
					float vOrig = t.orig.
						getNoInterpol(i, j, centerZ);
					float  vTrans = (float)t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			for (int i = 0; i < t.orig.d; i++) {
				initStartStop(0,centerY,i,t.orig.w,centerY,i);
				for (int j = 0; j < t.orig.w; j++) {
					calculateCurrent(j, t.orig.w);
					float vOrig = (float)t.orig.
						getNoInterpol(j, centerY, i);
					float vTrans = (float) t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			for (int i = 0; i < t.orig.d; i++) {
				initStartStop(centerX,0,i,centerX,t.orig.h,i);
				for (int j = 0; j < t.orig.h; j++) {
					calculateCurrent(j, t.orig.h);
					float vOrig = (float)t.orig.
						getNoInterpol(centerX, j, i);
					float vTrans = (float)t.transform.
						interpol.get(current.x, 
						current.y, current.z);
					t.measure.add(vOrig, vTrans);
				}
			}
			return t.measure.distance();
		}
	}
}
