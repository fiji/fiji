/**
 * <p>Title: Principle Curvature Plugin for ImageJ</p>
 *
 * <p>Description: Computes the Principle Curvatures of for 2D and 3D
   images except the pixels/voxels directly at the borders of the
   image</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: MPI-CBG</p>
 *
 * <p>License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * In addition, as a special exception, the copyright holders give
 * you permission to combine this program with free software programs or
 * libraries that are released under the Apache Public License. 
 *
 * @author Stephan Preibisch
 * @version 1.0
 *
 * Change in this version (Mark Longair):
 *
 * - Made the top level plugin a wrapper for this class so that
 *   "features" package so that it can be used by classes in other
 *   packages.
 *
 * - Now implements Runnable, with the void run() method creating
 *   the Gaussian and reporting progress via an optional callback.
 *   (If used in this way you need to use the constructor where you
 *   supply an ImagePlus, sigma and an optional callback.
 *
 * - Switched to using Johannes's JacobiDouble / JacobiFloat classes
 *   for finding the eigenvalues instead of the Jama classes, so we
 *   don't introduce an additional dependency.  It's about 15% faster
 *   with JacobiDouble, but I haven't yet measured any speed gain due
 *   to using floats rather than doubles.
 *
 * - Added options to only generate particular eigenvalue images, and
 *   some other slight changes to the initial dialog.
 *
 * - Added additional methods using floats for calculation (and the
 *   interface) in case clients only need floats.
 *
 * - Added ordering of the eigenvalues (optionally on absolute
 *   values).
 *
 * - Added normalisation of the eigenvalues to that the largest has
 *   size 1 (some papers use methods that require this)
 *
 * - Now we take notice of the calibration information (or not, if
 *   that option is deselected).
 *
 * - Use some faster eigenvalue calculation code for the 3x3 case.
 */

package features;

import ij.IJ;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;

import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;

import ij.measure.Calibration;

import math3d.Eigensystem3x3Float;
import math3d.Eigensystem3x3Double;
import math3d.Eigensystem2x2Float;
import math3d.Eigensystem2x2Double;
import math3d.JacobiDouble;
import math3d.JacobiFloat;

public class ComputeCurvatures implements Runnable
{
    static class TrivialProgressDisplayer implements GaussianGenerationCallback {
        public void proportionDone( double proportion ) {
            if( proportion < 0 )
                IJ.showProgress(1.0);
            else
                IJ.showProgress(proportion);
        }
    }

    private boolean _3D;
    private FloatArray data;
    private double[][] hessianMatrix;
    private float[] eigenValues = new float[3];

    private FloatArray3D result3D[];
    private FloatArray3D result2D;

    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

    protected ImagePlus imp;
    protected double sigma;
    protected boolean useCalibration;
    protected GaussianGenerationCallback callback;

    /* This constructor should used if you're actually using this as
       an ImageJ PlugIn.  (This leaves this.imp, etc. null - they'll
       be set by the runAsPlugIn( ... ) method.) */

    public ComputeCurvatures( ) {

    }

    /* This version of the constructor should only be used if you're
       using run() instead of runAsPlugIn(), e.g. if you're just
       calling hessianEigenvaluesAtPoint( ... ) after calculating the
       Gaussian. */

    public ComputeCurvatures( ImagePlus imp,
                              double sigma,
                              GaussianGenerationCallback callback,
                              boolean useCalibration ) {
        this.imp = imp;
        this.sigma = sigma;
        this.callback = callback;
        this.useCalibration = useCalibration;
    }

    /**
     * This method will be called when running the PlugIn, it coordinates the main process.
     *
     * @param args UNUSED

     * @author   Stephan Preibisch
     */
    public void runAsPlugIn(String arg)
    {
        imp = WindowManager.getCurrentImage();

        if (null == imp)
        {
            IJ.error("No images open.");
            return;
        }

        //
        // check whether it is a 3D image and not RGB
        //
        if (imp.getStackSize() > 1)
        {
            // IJ.log("3D");
            ImageStack stack = imp.getStack();
            _3D = true;
            data = StackToFloatArray(stack);
            if (data == null)
                return;
        }
        else
        {
            // IJ.log("2D");
            _3D = false;
            data = ImageToFloatArray(imp.getProcessor());
            if (data == null)
                return;
        }

        // Get the calibration data:
        Calibration calibration = imp.getCalibration();
        double minimumSeparation = 1;
        if( calibration != null )
            minimumSeparation = Math.min(calibration.pixelWidth,
                                         Math.min(calibration.pixelHeight,
                                                  calibration.pixelDepth));

        //
        // Show the dialog
        //
        GenericDialog gd = new GenericDialog("Principle curvature computing");

        gd.addMessage("Options:");
        if (_3D) {
            gd.addCheckbox("Show Eigenvalue 1 (smallest)",true);
            gd.addCheckbox("Show Eigenvalue 2",true);
            gd.addCheckbox("Show Eigenvalue 3 (largest)",true);
            gd.addMessage("");
        }
        gd.addCheckbox("Compute a gaussian convolution", true);
        gd.addMessage("Please define sigma (>= 0.5) for computing.");
        gd.addMessage("(Applies only if you wish to compute the convolution first)");
        gd.addNumericField("Sigma: ", (calibration==null) ? 0.5f : (minimumSeparation/2), 4);
        gd.addCheckbox("Use calibration information", calibration!=null);
        gd.addCheckbox("Show Gauss Image", false);
        gd.addCheckbox("Order eigenvalues on absolute values", true);
        gd.addCheckbox("Normalize eigenvalues", false);

        gd.showDialog();
        if (gd.wasCanceled())return;

        boolean [] evaluesToShow = new boolean[3];
        if (_3D) {
            evaluesToShow[0] = gd.getNextBoolean();
            evaluesToShow[1] = gd.getNextBoolean();
            evaluesToShow[2] = gd.getNextBoolean();
        }

        boolean computeGauss = gd.getNextBoolean();
        sigma = gd.getNextNumber();
        useCalibration = gd.getNextBoolean();
        boolean showGauss = gd.getNextBoolean();

        boolean orderOnAbsoluteValues = gd.getNextBoolean();
        boolean normalizeEigenValues = gd.getNextBoolean();

        //
        // compute Gaussian
        //
        if (computeGauss)
        {
            // IJ.log("Computing Gauss image");
            if (_3D)
            {
                data = computeGaussianFastMirror((FloatArray3D) data, (float)sigma, new TrivialProgressDisplayer(), useCalibration?calibration:null);
                if (showGauss)
                    FloatArrayToStack((FloatArray3D)data, "Gauss image", 0, 255).show();
            }
            else
            {
                data = computeGaussianFastMirror((FloatArray2D) data, (float)sigma, new TrivialProgressDisplayer(), useCalibration?calibration:null);
                if (showGauss)
                    FloatArrayToImagePlus((FloatArray2D)data, "Gauss image", 0, 255).show();
            }
        } else {
            // Otherwise the correction in calculating the Hessian
            // matrix won't make sense.
            sigma = 1;
        }

        float sepX = 1, sepY = 1, sepZ = 1;
        if( useCalibration && (calibration!=null) ) {
            sepX = (float)calibration.pixelWidth;
            sepY = (float)calibration.pixelHeight;
            sepZ = (float)calibration.pixelDepth;
        }

        //
        // Compute Hessian Matrix and principle curvatures for all pixels/voxels
        //

        // IJ.log("Computing Principle Curvatures");
        if (_3D)
        {
            FloatArray3D data3D = (FloatArray3D) data;

            result3D = new FloatArray3D[3];
            for( int i = 0; i < 3; ++i )
                if (evaluesToShow[i])
                    result3D[i] = new FloatArray3D(data3D.width, data3D.height, data3D.depth);

            IJ.showProgress(0);

            for (int z = 1; z < data3D.depth - 1; z++) {
                for (int y = 1; y < data3D.height - 1; y++)
                    for (int x = 1; x < data3D.width - 1; x++)
                    {
                        if( hessianEigenvaluesAtPoint3D( x, y, z,
                                                         orderOnAbsoluteValues,
                                                         eigenValues,
                                                         normalizeEigenValues,
                                                         false,
                                                         sepX,
                                                         sepY,
                                                         sepZ ) ) {

                            for( int i = 0; i < 3; ++i)
                                if (evaluesToShow[i])
                                    result3D[i].set((float)eigenValues[i], x, y, z);

                            if (eigenValues[0] < min) min = eigenValues[0];
                            if (eigenValues[1] < min) min = eigenValues[1];
                            if (eigenValues[2] < min) min = eigenValues[2];
                            if (eigenValues[0] > max) max = eigenValues[0];
                            if (eigenValues[1] > max) max = eigenValues[1];
                            if (eigenValues[2] > max) max = eigenValues[2];

                        } else {

                            // there were imaginary numbers
                            for( int i = 0; i < 3; ++i )
                                if (evaluesToShow[i])
                                    result3D[i].set(0, x, y, z);

                            if (0 < min) min = 0;
                            if (0 > max) max = 0;
                        }
                    }
                IJ.showProgress(z/(double)data3D.depth);
            }
            IJ.showProgress(1.0);
        }
        else
        {
            FloatArray2D data2D = (FloatArray2D) data;
            result2D = new FloatArray3D(data2D.width, data2D.height, 2);

            for (int y = 1; y < data2D.height - 1; y++)
                for (int x = 1; x < data2D.width - 1; x++)
                {
                    if( hessianEigenvaluesAtPoint2D( x, y,
                                                     orderOnAbsoluteValues,
                                                     eigenValues,
                                                     normalizeEigenValues,
                                                     false,
                                                     sepX,
                                                     sepY ) ) {

                        result2D.set((float)eigenValues[0], x, y, 0);
                        result2D.set((float)eigenValues[1], x, y, 1);

                        if (eigenValues[0] < min) min = eigenValues[0];
                        if (eigenValues[1] < min) min = eigenValues[1];
                        if (eigenValues[0] > max) max = eigenValues[0];
                        if (eigenValues[1] > max) max = eigenValues[1];

                    } else {

                        // there were imaginary numbers
                        result2D.set(0, x, y, 0);
                        result2D.set(0, x, y, 1);

                        if (0 < min) min = 0;
                        if (0 > max) max = 0;
                    }
                }
        }

        //
        // Output the data
        //

        if (_3D)
            for (int i = 0; i < 3; i++) {
                if (evaluesToShow[i])
                    FloatArrayToStack(result3D[i], "Eigenvalues " + (i+1), (float)min, (float)max).show();
            }
        else
            FloatArrayToStack(result2D, "Eigenvalues", (float)min, (float)max).show();

    }

    private boolean cancelGeneration = false;

    public void cancelGaussianGeneration( ) {
        cancelGeneration = true;
    }

    /* The 2 methods below are useful for using this class
       programmatically (i.e. not as a plugin) and creating the
       Gaussian in a separate thread. */

    public void run( ) { // Implements the Runnable interface
        if( imp == null ) {
            IJ.error("BUG: imp should not be null - are you using the right constructor?");
            return;
        }
        setup( );
    }

    /* Generate the Gaussian based on this.imp and this.sigma */

    public void setup( ) {

        try {

            if( imp == null ) {
                IJ.error("BUG: imp should not be null - are you using the right constructor?");
                return;
            }

            if( callback != null )
                callback.proportionDone( 0.0 );

            if (imp.getStackSize() > 1)
                {
                    // IJ.log("3D");
                    ImageStack stack = imp.getStack();
                    _3D = true;
                    data = StackToFloatArray(stack);
                    if (data == null)
                        return;
                }
            else
                {
                    // IJ.log("2D");
                    _3D = false;
                    data = ImageToFloatArray(imp.getProcessor());
                    if (data == null)
                        return;
                }

            boolean computeGauss = true;
            boolean showGauss = false;

            Calibration calibration=imp.getCalibration();

            // IJ.log("Computing Gauss image");
            if (_3D)
                {
                    data = computeGaussianFastMirror((FloatArray3D) data, (float)sigma, callback, useCalibration ? calibration : null );
                    if( data == null ) {
                        if( callback != null )
                            callback.proportionDone( -1 );
                        return;
                    }
                    if (showGauss)
                        FloatArrayToStack((FloatArray3D)data, "Gauss image", 0, 255).show();
                }
            else
                {
                    data = computeGaussianFastMirror((FloatArray2D) data, (float)sigma, callback, useCalibration ? calibration : null );
                    if( data == null ) {
                        if( callback != null )
                            callback.proportionDone( -1 );
                        return;
                    }
                    if (showGauss)
                        FloatArrayToImagePlus((FloatArray2D)data, "Gauss image", 0, 255).show();
                }

            if( callback != null )
                callback.proportionDone( 1.0 );


        } catch( OutOfMemoryError e ) {

            long requiredMiB = ( imp.getWidth() *
                                 imp.getHeight() *
                                 imp.getStackSize() * 4 ) / (1024 * 1024);

            IJ.error("Out of memory when calculating the Gaussian " +
                     "convolution of the image (requires " +
                     requiredMiB + "MiB");

            if( callback != null )
                callback.proportionDone( -1 );

            return;
        }

    }

    // ------------------------------------------------------------------------

    /* There are four versions of the this function, for calculating
       the eigenvalues of the Hessian matrix at a particular point in
       the image.  There are versions for 2D or 3D and using floats of
       doubles. */

    /* The float version of hessianEigenvaluesAtPoint2D.  Returns
       false if there was an error or the eigenvalues would be
       complex, or true if they are real and the values in result can
       be used. */

    public boolean hessianEigenvaluesAtPoint2D( int x,
                                                int y,
                                                boolean orderOnAbsoluteSize,
                                                float [] result, /* should be 2 elements */
                                                boolean normalize,
                                                boolean fixUp,
                                                float sepX,
                                                float sepY ) {

        if( _3D ) {
            IJ.error("hessianEigenvaluesAtPoint2D( x, y, z, ... ) is only for 2D data.");
            return false;
        }

        FloatArray2D data2D = (FloatArray2D)data;

        if( fixUp ) {

            if( x == 0 )
                x = 1;
            if( x == (data2D.width - 1) )
                x = data2D.width - 2;

            if( y == 0 )
                y = 1;
            if( y == (data2D.height - 1) )
                y = data2D.height - 2;
        }

        float [][] hessianMatrix = computeHessianMatrix2DFloat(data2D, x, y, sigma, sepX, sepY);
        float [] eigenValues = computeEigenValues(hessianMatrix);
        if( eigenValues == null )
            return false;

        // Don't assume that these are ordered.

        float e0 = eigenValues[0];
        float e1 = eigenValues[1];

        float e0c = orderOnAbsoluteSize ? Math.abs( e0 ) : e0;
        float e1c = orderOnAbsoluteSize ? Math.abs( e1 ) : e1;

        if( e0c <= e1c ) {
            result[0] = e0;
            result[1] = e1;
        } else {
            result[0] = e1;
            result[1] = e0;
        }
        if( normalize ) {
            float divideBy = Math.abs(result[1]);
            result[0] /= divideBy;
            result[1] /= divideBy;
        }
        return true;
    }

    /* The double version of hessianEigenvaluesAtPoint2D.  Returns
       false if there was an error or the eigenvalues would be
       complex, or true if they are real and the values in result can
       be used. */

    public boolean hessianEigenvaluesAtPoint2D( int x,
                                                int y,
                                                boolean orderOnAbsoluteSize,
                                                double [] result, /* should be 2 elements */
                                                boolean normalize,
                                                boolean fixUp,
                                                float sepX,
                                                float sepY ) {

        if( _3D ) {
            IJ.error("hessianEigenvaluesAtPoint2D( x, y, z, ... ) is only for 2D data.");
            return false;
        }

        FloatArray2D data2D = (FloatArray2D)data;

        if( fixUp ) {

            if( x == 0 )
                x = 1;
            if( x == (data2D.width - 1) )
                x = data2D.width - 2;

            if( y == 0 )
                y = 1;
            if( y == (data2D.height - 1) )
                y = data2D.height - 2;
        }

        double [][] hessianMatrix = computeHessianMatrix2DDouble(data2D, x, y, sigma, sepX, sepY);
        double [] eigenValues = computeEigenValues(hessianMatrix);
        if( eigenValues == null )
            return false;

        // Don't assume that these are ordered.

        double e0 = eigenValues[0];
        double e1 = eigenValues[1];

        double e0c = orderOnAbsoluteSize ? Math.abs( e0 ) : e0;
        double e1c = orderOnAbsoluteSize ? Math.abs( e1 ) : e1;

        if( e0c <= e1c ) {
            result[0] = e0;
            result[1] = e1;
        } else {
            result[0] = e1;
            result[1] = e0;
        }
        if( normalize ) {
            double divideBy = Math.abs(result[1]);
            result[0] /= divideBy;
            result[1] /= divideBy;
        }
        return true;
    }

    /* The float version of hessianEigenvaluesAtPoint3D.  Returns
       false if there was an error or the eigenvalues would be
       complex, or true if they are real and the values in result can
       be used. */

    public boolean hessianEigenvaluesAtPoint3D( int x,
                                                int y,
                                                int z, // zero-indexed
                                                boolean orderOnAbsoluteSize,
                                                float [] result, /* should be 3 elements */
                                                boolean normalize,
                                                boolean fixUp,
                                                float sepX,
                                                float sepY,
                                                float sepZ ) {

        if( ! _3D ) {
            IJ.error("hessianEigenvaluesAtPoint3D( x, y, z, ... ) is only for 3D data.");
            return false;
        }

        FloatArray3D data3D = (FloatArray3D)data;

        if( fixUp ) {

            if( x == 0 )
                x = 1;
            if( x == (data3D.width - 1) )
                x = data3D.width - 2;

            if( y == 0 )
                y = 1;
            if( y == (data3D.height - 1) )
                y = data3D.height - 2;

            if( z == 0 )
                z = 1;
            if( z == (data3D.depth - 1) )
                z = data3D.depth - 2;


        }

        float [][] hessianMatrix = computeHessianMatrix3DFloat(data3D, x, y, z, sigma, sepX, sepY, sepZ);
        float [] eigenValues = computeEigenValues(hessianMatrix);
        if( eigenValues == null )
            return false;

        // Don't assume that these are ordered.

        float e0 = eigenValues[0];
        float e1 = eigenValues[1];
        float e2 = eigenValues[2];

        float e0c = orderOnAbsoluteSize ? Math.abs( e0 ) : e0;
        float e1c = orderOnAbsoluteSize ? Math.abs( e1 ) : e1;
        float e2c = orderOnAbsoluteSize ? Math.abs( e2 ) : e2;

        /* This should sort a, b and c with the minimum number of
           comparisons - it's not necessarily faster than Arrays.sort,
           but we may want to reorder the evectors with them, in which
           case it would be. */

        if( e0c <= e1c ) {
            if( e1c <= e2c ) {
                result[0] = e0;
                result[1] = e1;
                result[2] = e2;
            } else {
                if( e0c <= e2c ) {
                    result[0] = e0;
                    result[1] = e2;
                    result[2] = e1;
                } else {
                    result[0] = e2;
                    result[1] = e0;
                    result[2] = e1;
                }
            }
        } else {
            if( e0c <= e2c ) {
                result[0] = e1;
                result[1] = e0;
                result[2] = e2;
            } else {
                if( e1c <= e2c ) {
                    result[0] = e1;
                    result[1] = e2;
                    result[2] = e0;
                } else {
                    result[0] = e2;
                    result[1] = e1;
                    result[2] = e0;
                }
            }
        }
        if( normalize ) {
            float divideBy = Math.abs(result[2]);
            result[0] /= divideBy;
            result[1] /= divideBy;
            result[2] /= divideBy;
        }
        return true;
    }

    /* The double version of hessianEigenvaluesAtPoint3D.  Returns
       false if there was an error or the eigenvalues would be
       complex, or true if they are real and the values in result can
       be used. */

    public boolean hessianEigenvaluesAtPoint3D( int x,
                                                int y,
                                                int z, // zero-indexed
                                                boolean orderOnAbsoluteSize,
                                                double [] result, /* should be 3 elements */
                                                boolean normalize,
                                                boolean fixUp,
                                                float sepX,
                                                float sepY,
                                                float sepZ ) {

        if( ! _3D ) {
            IJ.error("hessianEigenvaluesAtPoint3D( x, y, z, ... ) is only for 3D data.");
            return false;
        }

        FloatArray3D data3D = (FloatArray3D)data;

        if( fixUp ) {

            if( x == 0 )
                x = 1;
            if( x == (data3D.width - 1) )
                x = data3D.width - 2;

            if( y == 0 )
                y = 1;
            if( y == (data3D.height - 1) )
                y = data3D.height - 2;

            if( z == 0 )
                z = 1;
            if( z == (data3D.depth - 1) )
                z = data3D.depth - 2;


        }

        double [][] hessianMatrix = computeHessianMatrix3DDouble(data3D, x, y, z, sigma, sepX, sepY, sepZ);
        double [] eigenValues = computeEigenValues(hessianMatrix);
        if( eigenValues == null )
            return false;

        // Don't assume that these are ordered.

        double e0 = eigenValues[0];
        double e1 = eigenValues[1];
        double e2 = eigenValues[2];

        double e0c = orderOnAbsoluteSize ? Math.abs( e0 ) : e0;
        double e1c = orderOnAbsoluteSize ? Math.abs( e1 ) : e1;
        double e2c = orderOnAbsoluteSize ? Math.abs( e2 ) : e2;

        /* This should sort a, b and c with the minimum number of
           comparisons - it's not necessarily faster than Arrays.sort,
           but we may want to reorder the evectors with them, in which
           case it would be. */

        if( e0c <= e1c ) {
            if( e1c <= e2c ) {
                result[0] = e0;
                result[1] = e1;
                result[2] = e2;
            } else {
                if( e0c <= e2c ) {
                    result[0] = e0;
                    result[1] = e2;
                    result[2] = e1;
                } else {
                    result[0] = e2;
                    result[1] = e0;
                    result[2] = e1;
                }
            }
        } else {
            if( e0c <= e2c ) {
                result[0] = e1;
                result[1] = e0;
                result[2] = e2;
            } else {
                if( e1c <= e2c ) {
                    result[0] = e1;
                    result[1] = e2;
                    result[2] = e0;
                } else {
                    result[0] = e2;
                    result[1] = e1;
                    result[2] = e0;
                }
            }
        }
        if( normalize ) {
            double divideBy = Math.abs(result[2]);
            result[0] /= divideBy;
            result[1] /= divideBy;
            result[2] /= divideBy;
        }
        return true;
    }

    // ------------------------------------------------------------------------

    /**
     * This method converts my FloatArray2D to an ImageJ ImagePlus
     *
     * @param image The image as FloatArray2D
     * @param name The name of the ImagePlus
     * @param min Lowest brightness value that will be displayed (see Brightness&Contrast in Imagej)
     * @param max Highest brightness value that will be displayed (set both to zero for automatic)
     * @return ImagePlus The ImageJ image
     *
     * @author   Stephan Preibisch
     */
    public static ImagePlus FloatArrayToImagePlus(FloatArray2D image, String name, float min, float max)
    {
        ImagePlus imp = IJ.createImage(name,"32-Bit Black", image.width, image.height, 1);
        FloatProcessor ip = (FloatProcessor)imp.getProcessor();
        FloatArrayToFloatProcessor(ip, image);

        if (min == max)
            ip.resetMinAndMax();
        else
            ip.setMinAndMax(min, max);

        imp.updateAndDraw();

        return imp;
    }

    /**
     * This method converts my FloatArray2D to an ImageJ ImageProcessor
     *
     * @param ImageProcessor Will be overwritten with the data from the FloatArray2D
     * @param FloatArray2D The image as FloatArray2D
     * @return
     *
     * @author   Stephan Preibisch
     */
    public static void FloatArrayToFloatProcessor(ImageProcessor ip, FloatArray2D pixels)
    {
        float[] data = new float[pixels.width * pixels.height];

        int count = 0;
        for (int y = 0; y < pixels.height; y++)
            for (int x = 0; x < pixels.width; x++)
                data[count] = pixels.data[count++];

        ip.setPixels(data);
        ip.resetMinAndMax();
    }

    /**
     * This method converts my FloatArray3D to an ImageJ image stack packed into an ImagePlus
     *
     * @param image The image as FloatArray3D
     * @param name The name of the ImagePlus
     * @param min Lowest brightness value that will be displayed (see Brightness&Contrast in Imagej)
     * @param max Highest brightness value that will be displayed (set both to zero for automatic)
     * @return ImagePlus The ImageJ image
     *
     * @author   Stephan Preibisch
     */
    public ImagePlus FloatArrayToStack(FloatArray3D image, String name, float min, float max)
    {
        int width = image.width;
        int height = image.height;
        int nstacks = image.depth;

        ImageStack stack = new ImageStack(width, height);

        for (int slice = 0; slice < nstacks; slice++)
        {
            ImagePlus impResult = IJ.createImage(name, "32-Bit Black", width, height, 1);
            ImageProcessor ipResult = impResult.getProcessor();
            float[] sliceImg = new float[width * height];

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    sliceImg[y * width + x] = image.get(x,y,slice);

            ipResult.setPixels(sliceImg);

            if (min == max)
                ipResult.resetMinAndMax();
            else
                ipResult.setMinAndMax(min, max);

            stack.addSlice("Slice " + slice, ipResult);
        }

         return new ImagePlus(name, stack);
    }

    /**
     * This method computes the Eigenvalues of the Hessian Matrix, the
     * Eigenvalues correspond to the Principle Curvatures.  (MHL
     * modified to use the JacobiDouble class in math3d, or an optimized
     * version for the special case of 3x3 matrices.)<br>
     *
     * <br>
     * Note: If the Eigenvalues contain imaginary numbers, this method will return null
     *
     * @param double[][] The hessian Matrix
     * @return double[] The Real Parts of the Eigenvalues or null (if there were imganiary parts)
     *
     * @author   Stephan Preibisch
     */

    public double[] computeEigenValues(double[][] matrix) {

        if(matrix.length == 3 && matrix[0].length == 3) {
			Eigensystem3x3Double e = new Eigensystem3x3Double(matrix);
			boolean result = e.findEvalues();
			return result ? e.getEvaluesCopy() : null;
        } else if(matrix.length == 2 && matrix[0].length ==2) {
			Eigensystem2x2Double e = new Eigensystem2x2Double(matrix);
			boolean result = e.findEvalues();
			return result ? e.getEvaluesCopy() : null;
        } else {
            JacobiDouble jc=new JacobiDouble(matrix,50);
            return jc.getEigenValues();
        }
    }

    /**
     * This method computes the Eigenvalues of the Hessian Matrix, the
     * Eigenvalues correspond to the Principle Curvatures.  (MHL
     * modified to use the JacobiFloat class in math3d.)<br>
     *
     * <br>
     * Note: If the Eigenvalues contain imaginary numbers, this method will return null
     *
     * @param float[][] The hessian Matrix
     * @return float[] The Real Parts of the Eigenvalues or null (if there were imganiary parts)
     *
     * @author   Stephan Preibisch
     */

    public float[] computeEigenValues(float[][] matrix) {

        if(matrix.length == 3 && matrix[0].length == 3) {
			Eigensystem3x3Float e = new Eigensystem3x3Float(matrix);
			boolean result = e.findEvalues();
			return result ? e.getEvaluesCopy() : null;
        } else if(matrix.length == 2 && matrix[0].length ==2) {
			Eigensystem2x2Float e = new Eigensystem2x2Float(matrix);
			boolean result = e.findEvalues();
			return result ? e.getEvaluesCopy() : null;
        } else {
            JacobiFloat jc=new JacobiFloat(matrix,50);
            return jc.getEigenValues();
        }
    }

    /**
     * This method computes the Hessian Matrix for the 3x3 environment of a certain pixel <br><br>
     *
     * The 3D Hessian Matrix:<br>
     * xx xy <br>
     * yx yy <br>
     *
     * @param img The image as FloatArray3D
     * @param x The x-position of the voxel
     * @param y The y-position of the voxel
     * @return double[][] The 2D - Hessian Matrix
     *
     * @author   Stephan Preibisch
     */
    public double[][] computeHessianMatrix2DDouble(FloatArray2D laPlace, int x, int y, double sigma, float sepX, float sepY)
    {
        if( laPlace == null )
            laPlace = (FloatArray2D)data;

        double[][] hessianMatrix = new double[2][2]; // zeile, spalte

        double temp = 2 * laPlace.get(x, y);

        // xx
        hessianMatrix[0][0] = laPlace.get(x + 1, y) - temp + laPlace.get(x - 1, y);

        // yy
        hessianMatrix[1][1] = laPlace.get(x, y + 1) - temp + laPlace.get(x, y - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] =
            (
                (laPlace.get(x + 1, y + 1) - laPlace.get(x - 1, y + 1)) / 2
                -
                (laPlace.get(x + 1, y - 1) - laPlace.get(x - 1, y - 1)) / 2
            ) / 2;

        // FIXME: get Stephan to remind me why this is needed...
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    /**
     * This method computes the Hessian Matrix for the 3x3 environment of a certain pixel <br><br>
     *
     * The 3D Hessian Matrix:<br>
     * xx xy <br>
     * yx yy <br>
     *
     * @param img The image as FloatArray3D
     * @param x The x-position of the voxel
     * @param y The y-position of the voxel
     * @return float[][] The 2D - Hessian Matrix
     *
     * @author   Stephan Preibisch
     */
    public float[][] computeHessianMatrix2DFloat(FloatArray2D laPlace, int x, int y, double sigma, float sepX, float sepY)
    {
        if( laPlace == null )
            laPlace = (FloatArray2D)data;

        float[][] hessianMatrix = new float[2][2]; // zeile, spalte

        float temp = 2 * laPlace.get(x, y);

        // xx
        hessianMatrix[0][0] = laPlace.get(x + 1, y) - temp + laPlace.get(x - 1, y);

        // yy
        hessianMatrix[1][1] = laPlace.get(x, y + 1) - temp + laPlace.get(x, y - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] =
                (
                    (laPlace.get(x + 1, y + 1) - laPlace.get(x - 1, y + 1)) / 2
                        -
                    (laPlace.get(x + 1, y - 1) - laPlace.get(x - 1, y - 1)) / 2
                ) / 2;

        // FIXME: get Stephan to remind me why this is needed...
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    /**
     * This method computes the Hessian Matrix for the 3x3x3 environment of a certain voxel <br><br>
     *
     * The 3D Hessian Matrix:<br>
     * xx xy xz <br>
     * yx yy yz <br>
     * zx zy zz <br>
     *
     * @param img The image as FloatArray3D
     * @param x The x-position of the voxel
     * @param y The y-position of the voxel
     * @param z The z-position of the voxel
     * @return double[][] The 3D - Hessian Matrix
     *
     * @author   Stephan Preibisch
     */
    public double[][] computeHessianMatrix3DDouble(FloatArray3D img, int x, int y, int z, double sigma, float sepX, float sepY, float sepZ)
    {
        if( img == null )
            img = (FloatArray3D)data;

        double[][] hessianMatrix = new double[3][3]; // zeile, spalte

        double temp = 2 * img.get(x, y, z);

        // xx
        hessianMatrix[0][0] = img.get(x + 1, y, z) - temp + img.get(x - 1, y, z);

        // yy
        hessianMatrix[1][1] = img.get(x, y + 1, z) - temp + img.get(x, y - 1, z);

        // zz
        hessianMatrix[2][2] = img.get(x, y, z + 1) - temp + img.get(x, y, z - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] =
            (
                (img.get(x + 1, y + 1, z) - img.get(x - 1, y + 1, z)) / 2
                -
                (img.get(x + 1, y - 1, z) - img.get(x - 1, y - 1, z)) / 2
                ) / 2;

        // xz
        hessianMatrix[0][2] = hessianMatrix[2][0] =
            (
                (img.get(x + 1, y, z + 1) - img.get(x - 1, y, z + 1)) / 2
                -
                (img.get(x + 1, y, z - 1) - img.get(x - 1, y, z - 1)) / 2
                ) / 2;

        // yz
        hessianMatrix[1][2] = hessianMatrix[2][1] =
            (
                (img.get(x, y + 1, z + 1) - img.get(x, y - 1, z + 1)) / 2
                -
                (img.get(x, y + 1, z - 1) - img.get(x, y - 1, z - 1)) / 2
                ) / 2;

        // FIXME: get Stephan to remind me why this is needed...
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    /**
     * This method computes the Hessian Matrix for the 3x3x3 environment of a certain voxel <br><br>
     *
     * The 3D Hessian Matrix:<br>
     * xx xy xz <br>
     * yx yy yz <br>
     * zx zy zz <br>
     *
     * @param img The image as FloatArray3D
     * @param x The x-position of the voxel
     * @param y The y-position of the voxel
     * @param z The z-position of the voxel
     * @return float[][] The 3D - Hessian Matrix
     *
     * @author   Stephan Preibisch
     */
    public float[][] computeHessianMatrix3DFloat(FloatArray3D img, int x, int y, int z, double sigma, float sepX, float sepY, float sepZ)
    {
        if( img == null )
            img = (FloatArray3D)data;

        float[][] hessianMatrix = new float[3][3]; // zeile, spalte

        float temp = 2 * img.get(x, y, z);

        // xx
        hessianMatrix[0][0] = img.get(x + 1, y, z) - temp + img.get(x - 1, y, z);

        // yy
        hessianMatrix[1][1] = img.get(x, y + 1, z) - temp + img.get(x, y - 1, z);

        // zz
        hessianMatrix[2][2] = img.get(x, y, z + 1) - temp + img.get(x, y, z - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] =
            (
                (img.get(x + 1, y + 1, z) - img.get(x - 1, y + 1, z)) / 2
                -
                (img.get(x + 1, y - 1, z) - img.get(x - 1, y - 1, z)) / 2
                ) / 2;

        // xz
        hessianMatrix[0][2] = hessianMatrix[2][0] =
            (
                (img.get(x + 1, y, z + 1) - img.get(x - 1, y, z + 1)) / 2
                -
                (img.get(x + 1, y, z - 1) - img.get(x - 1, y, z - 1)) / 2
                ) / 2;

        // yz
        hessianMatrix[1][2] = hessianMatrix[2][1] =
            (
                (img.get(x, y + 1, z + 1) - img.get(x, y - 1, z + 1)) / 2
                -
                (img.get(x, y + 1, z - 1) - img.get(x, y - 1, z - 1)) / 2
                ) / 2;

        // FIXME: get Stephan to remind me why this is needed...
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    /**
     * This method creates a gaussian kernel
     *
     * @param sigma Standard Derivation of the gaussian function
     * @param normalize Normalize integral of gaussian function to 1 or not...
     * @return float[] The gaussian kernel
     *
     * @author   Stephan Saalfeld
     */
    public static float[] createGaussianKernel1D(float sigma, boolean normalize)
    {
        float[] gaussianKernel;

        if (sigma <= 0) {

            gaussianKernel = new float[3];
            gaussianKernel[1] = 1;

        } else {

            int size = Math.max(3, (int)(2*(int)(3*sigma + 0.5)+1));

            float two_sq_sigma = 2*sigma*sigma;
            gaussianKernel = new float[size];

            for (int x = size/2; x >= 0; --x) {

                float val = (float)Math.exp(-(float)(x*x)/two_sq_sigma);

                gaussianKernel[size/2-x] = val;
                gaussianKernel[size/2+x] = val;
            }
     }

     if (normalize)
     {
         float sum = 0;

         for (int i = 0; i < gaussianKernel.length; i++)
             sum += gaussianKernel[i];

         /*for (float value : gaussianKernel)
             sum += value;*/

         for (int i = 0; i < gaussianKernel.length; i++)
             gaussianKernel[i] /= sum;
     }


        return gaussianKernel;
    }

    /**
     * This method does the gaussian filtering of an image. On the edges of
     * the image it does mirror the pixels. It also uses the seperability of
     * the gaussian convolution.
     *
     * @param input FloatProcessor which will be folded (will not be touched)
     * @param sigma Standard Derivation of the gaussian function
     * @return FloatProcessor The folded image
     *
     * @author   Stephan Preibisch
     */
    public FloatArray2D computeGaussianFastMirror(FloatArray2D input, float sigma, GaussianGenerationCallback callback, Calibration calibration)
    {
        FloatArray2D output = new FloatArray2D(input.width, input.height);

        float avg;
        float kernelsumX = 0, kernelsumY = 0, kernelsumZ = 0;

        float pixelWidth = 1, pixelHeight = 1, pixelDepth = 1;

        if (calibration != null) {
            pixelWidth = (float)calibration.pixelWidth;
            pixelHeight = (float)calibration.pixelHeight;
            pixelDepth = (float)calibration.pixelDepth;
        }

        float[] kernelX = createGaussianKernel1D(sigma / pixelWidth, true);
        float[] kernelY = createGaussianKernel1D(sigma / pixelHeight, true);
        int filterSizeX = kernelX.length;
        int filterSizeY = kernelY.length;

        // get kernel sums
        for (int i = 0; i < kernelX.length; i++)
            kernelsumX += kernelX[i];
        for (int i = 0; i < kernelY.length; i++)
            kernelsumY += kernelY[i];

        double totalPoints = input.width * input.height * 2;
        long pointsDone = 0;

        // fold in x
        for (int x = 0; x < input.width; x++) {
            if( cancelGeneration )
                return null;
            for (int y = 0; y < input.height; y++)
                {
                    avg = 0;

                    if (x -filterSizeX / 2 >= 0 && x + filterSizeX / 2 < input.width)
                        for (int f = -filterSizeX / 2; f <= filterSizeX / 2; f++)
                            avg += input.get(x + f, y) * kernelX[f + filterSizeX / 2];
                    else
                        for (int f = -filterSizeX / 2; f <= filterSizeX / 2; f++)
                            avg += input.getMirror(x + f, y) * kernelX[f + filterSizeX / 2];

                    output.set(avg / kernelsumX, x, y);

                }
            pointsDone += input.height;
            if(callback != null)
                callback.proportionDone( pointsDone / totalPoints );
        }

        // fold in y
        for (int x = 0; x < input.width; x++) {
            if( cancelGeneration )
                return null;
            {
                float[] temp = new float[input.height];

                for (int y = 0; y < input.height; y++)
                {
                    avg = 0;

                    if (y -filterSizeY / 2 >= 0 && y + filterSizeY / 2 < input.height)
                        for (int f = -filterSizeY / 2; f <= filterSizeY / 2; f++)
                            avg += output.get(x, y + f) * kernelY[f + filterSizeY / 2];
                     else
                        for (int f = -filterSizeY / 2; f <= filterSizeY / 2; f++)
                            avg += output.getMirror(x, y + f) * kernelY[f + filterSizeY / 2];

                    temp[y] = avg / kernelsumY;
                }

                for (int y = 0; y < input.height; y++)
                    output.set(temp[y], x, y);

                pointsDone += input.height;
                if(callback != null)
                    callback.proportionDone( pointsDone / totalPoints );

            }
        }

        if( callback != null )
            callback.proportionDone(1.0);

        return output;
    }

    /**
     * This method does the gaussian filtering of an 3D image. On the edges of
     * the image it does mirror the pixels. It also uses the seperability of
     * the gaussian convolution.
     *
     * @param input FloatProcessor which will be folded (will not be touched)
     * @param sigma Standard Derivation of the gaussian function
     * @param calibration Calibration data for the image, or null if we assume separation in all three dimensions is 1
     * @return FloatProcessor The folded image
     *
     * @author   Stephan Preibisch
     */
    public FloatArray3D computeGaussianFastMirror(FloatArray3D input, float sigma,  GaussianGenerationCallback callback, Calibration calibration)
    {
        FloatArray3D output = new FloatArray3D(input.width, input.height, input.depth);

        float avg;
        float kernelsumX = 0, kernelsumY = 0, kernelsumZ = 0;

        float pixelWidth = 1, pixelHeight = 1, pixelDepth = 1;

        if (calibration != null) {
            pixelWidth = (float)calibration.pixelWidth;
            pixelHeight = (float)calibration.pixelHeight;
            pixelDepth = (float)calibration.pixelDepth;
        }

        float[] kernelX = createGaussianKernel1D(sigma / pixelWidth, true);
        float[] kernelY = createGaussianKernel1D(sigma / pixelHeight, true);
        float[] kernelZ = createGaussianKernel1D(sigma / pixelDepth, true);
        int filterSizeX = kernelX.length;
        int filterSizeY = kernelY.length;
        int filterSizeZ = kernelZ.length;

        // get kernel sums
        for (int i = 0; i < kernelX.length; i++)
            kernelsumX += kernelX[i];
        for (int i = 0; i < kernelY.length; i++)
            kernelsumY += kernelY[i];
        for (int i = 0; i < kernelZ.length; i++)
            kernelsumZ += kernelZ[i];

        double totalPoints = input.width * input.height * input.depth * 3;
        long pointsDone = 0;

        // fold in x
        for (int x = 0; x < input.width; x++) {
            if( cancelGeneration )
                return null;
            for (int y = 0; y < input.height; y++)
                for (int z = 0; z < input.depth; z++)
                {
                    avg = 0;

                    if (x -filterSizeX / 2 >= 0 && x + filterSizeX / 2 < input.width)
                        for (int f = -filterSizeX / 2; f <= filterSizeX / 2; f++)
                            avg += input.get(x + f, y, z) * kernelX[f + filterSizeX / 2];
                    else
                        for (int f = -filterSizeX / 2; f <= filterSizeX / 2; f++)
                            avg += input.getMirror(x + f, y, z) * kernelX[f + filterSizeX / 2];

                    output.set(avg / kernelsumX, x, y, z);

                }
            pointsDone += input.height * input.depth;
            if(callback != null)
                callback.proportionDone( pointsDone / totalPoints );
        }

        // fold in y
        for (int x = 0; x < input.width; x++) {
            if( cancelGeneration )
                return null;
            for (int z = 0; z < input.depth; z++)
            {
                float[] temp = new float[input.height];

                for (int y = 0; y < input.height; y++)
                {
                    avg = 0;

                    if (y -filterSizeY / 2 >= 0 && y + filterSizeY / 2 < input.height)
                        for (int f = -filterSizeY / 2; f <= filterSizeY / 2; f++)
                            avg += output.get(x, y + f, z) * kernelY[f + filterSizeY / 2];
                    else
                       for (int f = -filterSizeY / 2; f <= filterSizeY / 2; f++)
                           avg += output.getMirror(x, y + f, z) * kernelY[f + filterSizeY / 2];

                    temp[y] = avg / kernelsumY;
                }

                for (int y = 0; y < input.height; y++)
                    output.set(temp[y], x, y, z);
            }
            pointsDone += input.depth * input.height;
            if(callback != null)
                callback.proportionDone( pointsDone / totalPoints );

        }

        // fold in z
        for (int x = 0; x < input.width; x++) {
            if( cancelGeneration )
                return null;
            for (int y = 0; y < input.height; y++)
            {
                float[] temp = new float[input.depth];

                for (int z = 0; z < input.depth; z++)
                {
                    avg = 0;

                    if (z -filterSizeZ / 2 >= 0 && z + filterSizeZ / 2 < input.depth)
                        for (int f = -filterSizeZ / 2; f <= filterSizeZ / 2; f++)
                            avg += output.get(x, y, z + f) * kernelZ[f + filterSizeZ / 2];
                    else
                       for (int f = -filterSizeZ / 2; f <= filterSizeZ / 2; f++)
                           avg += output.getMirror(x, y, z + f) * kernelZ[f + filterSizeZ / 2];

                    temp[z] = avg / kernelsumZ;
                }

                for (int z = 0; z < input.depth; z++)
                    output.set(temp[z], x, y, z);

            }
            pointsDone += input.height * input.depth;
            if(callback != null)
                callback.proportionDone( pointsDone / totalPoints );

        }

        if( callback != null )
            callback.proportionDone( 1.0 );

        return output;
    }

    /**
     * This method convertes an ImageJ image stack to my FloatArray3D,
     * which is a one dimensional structure with methods for 3D access
     *
     * @param stack ImageJ image stack
     * @return FloatArray3D The image packed into a FloatArray3D
     *
     * @author   Stephan Preibisch
     */
    public FloatArray3D StackToFloatArray(ImageStack stack)
    {
        int width = stack.getWidth();
        int height = stack.getHeight();
        int nstacks = stack.getSize();
        Object[] imageStack = new Object[nstacks];

        if (nstacks == 0)
        {
            IJ.error("Image Stack is empty.");
            return null;
        }

	for( int z = 0; z < nstacks; ++z )
		imageStack[z] = stack.getPixels(z+1);

        if (imageStack[0] instanceof int[])
        {
            IJ.error("RGB images not supported at the moment.");
            return null;
        }

        FloatArray3D pixels = new FloatArray3D(width, height, nstacks);
        //float[][][] pixels = new float[width][height][nstacks];
        int count;


        if (imageStack[0] instanceof byte[])
            for (int countSlice = 0; countSlice < nstacks; countSlice++)
            {
                byte[] pixelTmp = (byte[])imageStack[countSlice];
                count = 0;

                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        pixels.data[pixels.getPos(x,y,countSlice)] = (float)(pixelTmp[count++] & 0xff);
            }
        else if (imageStack[0] instanceof short[])
            for (int countSlice = 0; countSlice < nstacks; countSlice++)
            {
                short[] pixelTmp = (short[])imageStack[countSlice];
                count = 0;

                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        pixels.data[pixels.getPos(x,y,countSlice)] = (float)(pixelTmp[count++] & 0xffff);
            }
        else // instance of float[]
            for (int countSlice = 0; countSlice < nstacks; countSlice++)
            {
                float[] pixelTmp = (float[])imageStack[countSlice];
                count = 0;

                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        pixels.data[pixels.getPos(x,y,countSlice)] = pixelTmp[count++];
            }


        return pixels;
    }

    /**
     * This method convertes an ImageJ ImageProcessor to my FloatArray2D,
     * which is a one dimensional structure with methods for 2D access
     *
     * @param stack ImageJ ImageProcessor
     * @return FloatArray2D The image packed into a FloatArray2D
     *
     * @author   Stephan Preibisch
     */
    public FloatArray2D ImageToFloatArray(ImageProcessor ip)
    {
        FloatArray2D image;
        Object pixelArray = ip.getPixels();
        int count = 0;

        if (ip instanceof ByteProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            byte[] pixels = (byte[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xff;
        }
        else if (ip instanceof ShortProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            short[] pixels = (short[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xffff;
        }
        else if (ip instanceof FloatProcessor)
        {
            image = new FloatArray2D(ip.getWidth(),  ip.getHeight());
            float[] pixels = (float[])pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++];
        }
        else //RGB
        {
            IJ.error("RGB images not supported");
            image = null;
        }

        return image;
    }

    /**
     * This class is the abstract class for my FloatArrayXDs,
     * which are a one dimensional structures with methods for access in n dimensions
     *
     * @author   Stephan Preibisch
     */
    public abstract class FloatArray
    {
        public float data[] = null;
        @Override
        public abstract FloatArray clone();
    }

    /**
     * The 2D implementation of the FloatArray
     *
     * @author   Stephan Preibisch
     */
    public class FloatArray2D extends FloatArray
    {
        public float data[] = null;
        public int width = 0;
        public int height = 0;


        public FloatArray2D(int width, int height)
        {
            data = new float[width * height];
            this.width = width;
            this.height = height;
        }

        public FloatArray2D(float[] data, int width, int height)
        {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        @Override
        public FloatArray2D clone()
        {
            FloatArray2D clone = new FloatArray2D(width, height);
            System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
            return clone;
        }

        public int getPos(int x, int y)
        {
            return x + width * y;
        }

        public float get(int x, int y)
        {
            return data[getPos(x,y)];
        }

        public float getMirror(int x, int y)
        {
            if (x >= width)
                x = width - (x - width + 2);

            if (y >= height)
                y = height - (y - height + 2);

            if (x < 0)
            {
                int tmp = 0;
                int dir = 1;

                while (x < 0)
                {
                    tmp += dir;
                    if (tmp == width - 1 || tmp == 0)
                        dir *= -1;
                    x++;
                }
                x = tmp;
            }

            if (y < 0)
            {
                int tmp = 0;
                int dir = 1;

                while (y < 0)
                {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    y++;
                }
                y = tmp;
            }

            return data[getPos(x,y)];
        }

        public float getZero(int x, int y)
        {
            if (x >= width)
                return 0;

            if (y >= height)
                return 0;

            if (x < 0)
                return 0;

            if (y < 0)
                return 0;

            return data[getPos(x,y)];
        }

        public void set(float value, int x, int y)
        {
            data[getPos(x,y)] = value;
        }
    }

    /**
     * The 3D implementation of the FloatArray
     *
     * @author   Stephan Preibisch
     */
    public class FloatArray3D extends FloatArray
    {
        public float data[] = null;
        public int width = 0;
        public int height = 0;
        public int depth = 0;

        public FloatArray3D(float[] data, int width, int height, int depth)
        {
            this.data = data;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public FloatArray3D(int width, int height, int depth)
        {
            data = new float[width * height * depth];
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public FloatArray3D clone()
        {
            FloatArray3D clone = new FloatArray3D(width, height, depth);
            System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
            return clone;
        }

        public int getPos(int x, int y, int z)
        {
            return x + width * (y + z * height);
        }

        public float get(int x, int y, int z)
        {
            return data[getPos(x,y,z)];
        }

        public float getMirror(int x, int y, int z)
        {
            if (x >= width)
                x = width - (x - width + 2);

            if (y >= height)
                y = height - (y - height + 2);

            if (z >= depth)
                z = depth - (z - depth + 2);

            if (x < 0)
            {
                int tmp = 0;
                int dir = 1;

                while (x < 0)
                {
                    tmp += dir;
                    if (tmp == width - 1 || tmp == 0)
                        dir *= -1;
                    x++;
                }
                x = tmp;
            }

            if (y < 0)
            {
                int tmp = 0;
                int dir = 1;

                while (y < 0)
                {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    y++;
                }
                y = tmp;
            }

            if (z < 0)
            {
                int tmp = 0;
                int dir = 1;

                while (z < 0)
                {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    z++;
                }
                z = tmp;
            }

            return data[getPos(x,y,z)];
        }

        public void set(float value, int x, int y, int z)
        {
            data[getPos(x,y,z)] = value;
        }

        public FloatArray2D getXPlane(int x)
        {
            FloatArray2D plane = new FloatArray2D(height, depth);

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x,y,z),y,z);

            return plane;
        }

        public float[][] getXPlane_float(int x)
        {
            float[][] plane = new float[height][depth];

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane[y][z]=this.get(x,y,z);

            return plane;
        }

        public FloatArray2D getYPlane(int y)
        {
            FloatArray2D plane = new FloatArray2D(width, depth);

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x,y,z),x,z);

            return plane;
        }

        public float[][] getYPlane_float(int y)
        {
            float[][] plane = new float[width][depth];

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane[x][z] = this.get(x,y,z);

            return plane;
        }

        public FloatArray2D getZPlane(int z)
        {
            FloatArray2D plane = new FloatArray2D(width, height);

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane.set(this.get(x,y,z),x,y);

            return plane;
        }

        public float[][] getZPlane_float(int z)
        {
            float[][] plane = new float[width][height];

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane[x][y] = this.get(x,y,z);

            return plane;
        }

        public void setXPlane(FloatArray2D plane, int x)
        {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(y,z),x,y,z);
        }

        public void setXPlane(float[][] plane, int x)
        {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane[y][z],x,y,z);
        }

        public void setYPlane(FloatArray2D plane, int y)
        {
            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(x,z),x,y,z);
        }

        public void setYPlane(float[][] plane, int y)
        {
            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    this.set(plane[x][z], x, y, z);
        }

        public void setZPlane(FloatArray2D plane, int z)
        {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    this.set(plane.get(x,y),x,y,z);
        }

        public void setZPlane(float[][] plane, int z)
        {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    this.set(plane[x][y],x,y,z);
        }

    }

}
