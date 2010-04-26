/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.*;
import ij.process.*;
import ij.gui.*;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;

import vib.oldregistration.RegistrationAlgorithm;

import vib.transforms.OrderedTransformations;
import vib.transforms.FastMatrixTransform;
import vib.FastMatrix;
import landmarks.NamedPointWorld;

import util.CombinationGenerator;

import vib.TransformedImage;

import pal.math.ConjugateDirectionSearch;
import pal.math.MultivariateFunction;

/* This method doesn't work terribly well, and is here largely for
   comparison purposes. */

public class AffineFromLandmarks extends RegistrationAlgorithm {

        OrderedTransformations transformation;

        static double scoreFromAllLandmarks(OrderedTransformations t,
					    ArrayList<String> common,
					    NamedPointSet inImage0,
					    NamedPointSet inImage1) {

                double sum_squared_differences = 0.0;

                for (Iterator i=common.listIterator();i.hasNext();) {
                        String s = (String)i.next();
                        NamedPointWorld p0 = null;
                        NamedPointWorld p1 = null;

                        for (Iterator i0=inImage0.listIterator();i0.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i0.next();
                                if (s.equals(current.getName())) {
                                        p0 = current;
                                        break;
                                }
                        }

                        for (Iterator i1=inImage1.listIterator();i1.hasNext();) {
                                NamedPointWorld current=(NamedPointWorld)i1.next();
                                if (s.equals(current.getName())) {
                                        p1 = current;
                                        break;
                                }
                        }

                        double[] p1_transformed = new double[3];
                        t.apply(p1.x,p1.y,p1.z,p1_transformed);

                        double distance = Math.sqrt(
                                (p1_transformed[0] - p0.x) * (p1_transformed[0] - p0.x) +
                                (p1_transformed[1] - p0.y) * (p1_transformed[1] - p0.y) +
                                (p1_transformed[2] - p0.z) * (p1_transformed[2] - p0.z)
                                );

                        sum_squared_differences += distance * distance;
                }

                return sum_squared_differences / common.size();

        }

        /* This finds an affine mapping that maps a1 onto a2, b1 onto
	   b2, etc.  (I suspect this is exactly equivalent to
	   FastMatrix.bestLinear, but I haven't done the maths to
	   prove that.)  */

        public static FastMatrix generateAffine(NamedPointWorld a1,
						NamedPointWorld b1,
						NamedPointWorld c1,
						NamedPointWorld d1,

						NamedPointWorld a2,
						NamedPointWorld b2,
						NamedPointWorld c2,
						NamedPointWorld d2) {

                double[][] p = new double[3][4];

                p[0][0] = b1.x - a1.x;
                p[0][1] = c1.x - a1.x;
                p[0][2] = d1.x - a1.x;

                p[1][0] = b1.y - a1.y;
                p[1][1] = c1.y - a1.y;
                p[1][2] = d1.y - a1.y;

                p[2][0] = b1.z - a1.z;
                p[2][1] = c1.z - a1.z;
                p[2][2] = d1.z - a1.z;


                double[][] q = new double[3][4];

                q[0][0] = b2.x - a2.x;
                q[0][1] = c2.x - a2.x;
                q[0][2] = d2.x - a2.x;

                q[1][0] = b2.y - a2.y;
                q[1][1] = c2.y - a2.y;
                q[1][2] = d2.y - a2.y;

                q[2][0] = b2.z - a2.z;
                q[2][1] = c2.z - a2.z;
                q[2][2] = d2.z - a2.z;

		FastMatrix Pfm = new FastMatrix(p);
		FastMatrix Qfm = new FastMatrix(q);

		FastMatrix Mfm = Qfm.times(Pfm.inverse());

		Mfm.apply( a1.x, a1.y, a1.z );

                double ox = a2.x - Mfm.x;
                double oy = a2.y - Mfm.y;
                double oz = a2.z - Mfm.z;

		FastMatrix resultFM = FastMatrix.translate( ox, oy, oz ).times( Mfm );

		return resultFM;
        }

	boolean allowScaling;

	public static double evaluateFastMatrix( FastMatrix fm,
						 NamedPointSet from,
						 NamedPointSet to ) {
		if( from.size() != to.size() ) {
			throw new RuntimeException("In evaluateFastMatrix, 'from' (size "+from.size()+") and 'to' (size "+to.size()+") must be equal");
		}
		double sumDistances = 0;
		int n = from.size();
		for( int i = 0; i < n; ++i ) {
			NamedPointWorld fromPoint = from.get( i );
			NamedPointWorld toPoint = to.get( i );
			if( ! fromPoint.name.equals(toPoint.name) )
				throw new RuntimeException("In evaluateFastMatrix, point index "+i+" has a name mismatch: fromPoint = "+fromPoint+", toPoint = "+toPoint);
			fm.apply( fromPoint.x, fromPoint.y, fromPoint.z );
			double xdiff = fm.x - toPoint.x;
			double ydiff = fm.y - toPoint.y;
			double zdiff = fm.z - toPoint.z;
			sumDistances += Math.sqrt( xdiff * xdiff + ydiff * ydiff + zdiff * zdiff );
		}
		return sumDistances / n;
	}

	public static class CandidateAffine implements MultivariateFunction {

		FastMatrix m = new FastMatrix();
		NamedPointSet from, to;
		double bestScore = Double.MAX_VALUE;
		double [] bestArgument = new double[12];
		double sizeOfLargestDimension;

		public CandidateAffine( NamedPointSet from, NamedPointSet to, double sizeOfLargestDimension ) {
			this.from = from;
			this.to = to;
			this.sizeOfLargestDimension = sizeOfLargestDimension;
		}

		public double evaluate( double[] argument ) {
			double [] argumentAdjusted = argument.clone();
			argumentAdjusted[3] *= sizeOfLargestDimension;
			argumentAdjusted[7] *= sizeOfLargestDimension;
			argumentAdjusted[11] *= sizeOfLargestDimension;
			m.setFromFlatDoubleArray( argumentAdjusted );
			double score = evaluateFastMatrix( m, from, to );
			if( score < bestScore ) {
				bestScore = score;
				System.arraycopy( argument, 0, bestArgument, 0, 12 );
			}
			return score;
		}

		public int getNumArguments() {
			return 12;
		}

		// We might need to set these values to something more sensible:

		public double getLowerBound(int n) {
			// return Double.MIN_VALUE;
			return -1;
		}

		public double getUpperBound(int n) {
			// return Double.MAX_VALUE;
			return 1;
		}
	}

	public static final boolean tryOptimizing = false;

        public static FastMatrix bestBetweenPoints( NamedPointSet points0, ImagePlus image0, NamedPointSet points1, ImagePlus image1 ) {

                ArrayList<String> commonPointNames = points0.namesSharedWith( points1, true );

                int n = commonPointNames.size();

                if (n<4) {
                        String error = "There are fewer than 4 points in these two "+
                                "images that have been marked up with the same "+
                                "names:";
                        if(n==0) {
                                error += " (none in common)";
                        } else {
                                for(Iterator i=commonPointNames.iterator();i.hasNext();)
                                        error += "\n    "+i.next();
                        }
                        IJ.error(error);
                        return null;
                }

		NamedPointSet fromCommon = new NamedPointSet();
		NamedPointSet toCommon = new NamedPointSet();

		for( String name : commonPointNames ) {
			toCommon.add( points0.get( name ) );
			fromCommon.add( points1.get( name ) );
		}

                int[] indices = new int[n];
                for(int i=0;i<n;++i)
                        indices[i] = i;

                CombinationGenerator generator = new CombinationGenerator(n,4);

		FastMatrix bestFastMatrixSoFar = null;
                double minimumScoreSoFar = Double.MAX_VALUE;

                double totalCombinations = generator.getTotal().doubleValue();

                if(totalCombinations>1024) {
                        IJ.error("There are over 1024 combinations; you probably"+
                                 "shouldn't be using this method.");
                }

		double sizeOfLargestDimension = Double.MIN_VALUE;
		double xSpacing0 = 1, ySpacing0 = 1, zSpacing0 = 1;
		double xSpacing1 = 1, ySpacing1 = 1, zSpacing1 = 1;
		Calibration c0 = image0.getCalibration();
		if( c0 != null ) {
			xSpacing0 = c0.pixelWidth;
			ySpacing0 = c0.pixelHeight;
			zSpacing0 = c0.pixelDepth;
		}
		Calibration c1 = image1.getCalibration();
		if( c1 != null ) {
			xSpacing1 = c1.pixelWidth;
			ySpacing1 = c1.pixelHeight;
			zSpacing1 = c1.pixelDepth;
		}
		double [] sides = new double[6];
		sides[0] = Math.abs( image0.getWidth() * xSpacing0 );
		sides[1] = Math.abs( image0.getHeight() * ySpacing0 );
		sides[2] = Math.abs( image0.getStackSize() * zSpacing0 );
		sides[3] = Math.abs( image1.getWidth() * xSpacing1 );
		sides[4] = Math.abs( image1.getHeight() * ySpacing1 );
		sides[5] = Math.abs( image1.getStackSize() * zSpacing1 );
		for( int i = 0; i < 6; ++i )
			if( sides[i] > sizeOfLargestDimension )
				sizeOfLargestDimension = sides[i];

                IJ.showProgress(0.0);

                int done = 0;

                while(generator.hasMore()) {

                        int [] choice = generator.getNext();

                        // So, for each set of 4, generate an affine
                        // transformation between the two...

                        FastMatrix affine = generateAffine(
                                fromCommon.get(choice[0]),
                                fromCommon.get(choice[1]),
                                fromCommon.get(choice[2]),
                                fromCommon.get(choice[3]),
                                toCommon.get(choice[0]),
                                toCommon.get(choice[1]),
                                toCommon.get(choice[2]),
                                toCommon.get(choice[3]) );

			double originalScore = evaluateFastMatrix( affine, fromCommon, toCommon );
			if( originalScore < minimumScoreSoFar ) {
				minimumScoreSoFar = originalScore;
				bestFastMatrixSoFar = affine;
			}

			Point3d [] from = new Point3d[4];
			Point3d [] to = new Point3d[4];
			for( int i = 0; i < 4; ++i ) {
				NamedPointWorld npw1 = fromCommon.get(choice[i]);
				NamedPointWorld npw0 = toCommon.get(choice[i]);
				from[i] = npw1.toPoint3d();
				to[i] = npw0.toPoint3d();
			}

			if( tryOptimizing ) {

				double [] initialValues = new double[12];
				affine.copyToFlatDoubleArray( initialValues );
				initialValues[3] /= sizeOfLargestDimension;
				initialValues[7] /= sizeOfLargestDimension;
				initialValues[11] /= sizeOfLargestDimension;

				ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();
				optimizer.scbd = 1;
				optimizer.step = 1;
				optimizer.illc = true;

				CandidateAffine candidate = new CandidateAffine( fromCommon, toCommon, sizeOfLargestDimension );
				optimizer.optimize(candidate, initialValues, 2, 2);

				/* This will leave the optimized
				   values in initialValues.  Work out
				   the score that we would get from
				   this: */

				FastMatrix fm = new FastMatrix();
				double [] initialValuesAdjusted = initialValues.clone();
				initialValuesAdjusted[3] *= sizeOfLargestDimension;
				initialValuesAdjusted[7] *= sizeOfLargestDimension;
				initialValuesAdjusted[11] *= sizeOfLargestDimension;
				fm.setFromFlatDoubleArray( initialValuesAdjusted );
				double score = evaluateFastMatrix( fm, fromCommon, toCommon );

				if( score < minimumScoreSoFar ) {
					minimumScoreSoFar = score;
					bestFastMatrixSoFar = fm;
				}

				/* Strangely, it seems that sometimes
				   one gets a better score than the
				   final one during the search, so
				   check for that as well: */

				FastMatrix bestFromOptimization = new FastMatrix();
				double [] candidateBestArgumentAdjusted = candidate.bestArgument.clone();
				candidateBestArgumentAdjusted[3] *= sizeOfLargestDimension;
				candidateBestArgumentAdjusted[7] *= sizeOfLargestDimension;
				candidateBestArgumentAdjusted[11] *= sizeOfLargestDimension;
				bestFromOptimization.setFromFlatDoubleArray( candidateBestArgumentAdjusted ) ;

				if( candidate.bestScore < minimumScoreSoFar ) {
					minimumScoreSoFar = candidate.bestScore;
					bestFastMatrixSoFar = bestFromOptimization;
				}
			}



                        ++ done;
                        IJ.showProgress( done / totalCombinations );
                }

                IJ.showProgress(1.0);

                return bestFastMatrixSoFar;
        }

        public ImagePlus register() {

		NamedPointSet points0 = null;
		NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage( sourceImages[0] );
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error( "Failed to find a corresponding points file for: "+sourceImages[0].getTitle() );
		}
		try {
			points1 = NamedPointSet.forImage( sourceImages[1] );
		} catch( NamedPointSet.PointsFileException e ) {
			IJ.error( "Failed to find a corresponding points file for: "+sourceImages[1].getTitle() );
		}

		return register( points0, points1 );
	}

	public ImagePlus register( NamedPointSet points0, NamedPointSet points1 ) {

                FastMatrix affine=bestBetweenPoints( points0, sourceImages[0], points1, sourceImages[1] );

		TransformedImage ti = new TransformedImage( sourceImages[0], sourceImages[1] );
		ti.setTransformation( affine );

		ImagePlus transformed = ti.getTransformed();
		transformed.setTitle( "Transformed" );
		return transformed;
        }
}

