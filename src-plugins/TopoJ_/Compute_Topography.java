import ij.*;
import ij.measure.Calibration;
import ij.gui.NewImage;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import java.util.Arrays;

import Jama.Matrix;

/**
 * Created by IntelliJ IDEA.
 * User: dhovis
 * Date: Mar 4, 2007
 * Time: 10:01:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class Compute_Topography implements PlugInFilter {
    final static private int Width = 0;

    protected ImageStack stack;
    protected String stackTitle;
    protected Calibration myCal;

    protected int[] Dimensions;
    protected boolean isHyperstack;
    protected ImagePlus sourceImagePlus;

    protected int medianFilterRange = 5;
    protected int medianFilterOffset;
    protected int medianFilterIncrement;

    public int setup(String string, ImagePlus imagePlus) {
        sourceImagePlus = imagePlus;
        return DOES_16 + STACK_REQUIRED + NO_CHANGES;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void run(ImageProcessor imageProcessor) {
        stackTitle = sourceImagePlus.getTitle();
        stack = sourceImagePlus.getStack();
        isHyperstack = sourceImagePlus.isHyperStack();
        Dimensions = sourceImagePlus.getDimensions();
        myCal = sourceImagePlus.getCalibration();

        //Some defaults

        //If the pixel intensity for a given xy position does not rise above the threshold value
        //then the z-value is set to -1
        int threshold = 0;

        //Determines if a height map should be generated.
        boolean doHM = true;

        //If this is set, the maximum pixel in z for each xy position will be set to be saturated
        boolean saturateMax = false;

        //If doQR is true, a quadratic refinement of the data is attempted, fitting a
        // second order polynomial to the data in z.  For each xy-position, the z-position with max
        // intensity is found first (zMax).  then a second order polynomial is fit to the data on
        // either size of zMax, within a range defined by qWindow.  The height is then defined by the point
        // where the first derivative is zero.  The result is deemed invalid if the second derivative is positive
        // or the computed value resides outside the range zMax +/- qWindow.
        // The polynomial that is fit is of the form z = ax^2+bx+c,
        // where the first derivative is 2ax+b
        // and the second derivative is 2a
        boolean doQR = false;
        int qWindow = 20;

        // If showQRS is selected, the "Quadratic Sharpness is shown
        boolean showQRS = false;
        boolean currentChannelOnly = true;
        int numChannelsToDo = Dimensions[2];


        GenericDialog gd = new GenericDialog("TopoJ method selection");
        gd.addCheckbox("Height Map:", doHM);
        gd.addNumericField("Threshold:", threshold, 0);
        gd.addCheckbox("Saturate Maximum:", saturateMax);
        gd.addCheckbox("Quadratic Refinement:", doQR);
        gd.addCheckbox("Show Quadratic Coefficient Map:", showQRS);
        gd.addNumericField("Quadratic Window:", qWindow, 0);
        /*
        gd.addCheckbox("Height Map Derivative:", doHMD);
        gd.addNumericField("Smoothing window:", medianFilterRange, 0);
        */
        if (isHyperstack && (Dimensions[2] > 1)) {
            gd.addCheckbox("Current Channel Only:", true);
        }
        gd.showDialog();
        if (gd.wasCanceled()) return;

        doHM = gd.getNextBoolean();
        threshold = (int) gd.getNextNumber();
        saturateMax = gd.getNextBoolean();

        doQR = gd.getNextBoolean();
        showQRS = gd.getNextBoolean();
        qWindow = (int) gd.getNextNumber();
/*
        doHMD = gd.getNextBoolean();
        medianFilterRange = (int) gd.getNextNumber();
*/
        if (isHyperstack && (Dimensions[2] > 1)) {
            currentChannelOnly = gd.getNextBoolean();
            if (currentChannelOnly == false)
                numChannelsToDo = Dimensions[2];
            else
                numChannelsToDo = 1;
        }

        // Make sure the median filter range is odd and positive
        if ((medianFilterRange % 2) == 0)
            medianFilterRange--;
        if (medianFilterRange < 1)
            medianFilterRange = 1;

        medianFilterIncrement = (medianFilterRange / 2);
        medianFilterOffset = medianFilterIncrement + 1;

        short[] pixels = (short[]) stack.getPixels(1);
        int dimension = Dimensions[0] * Dimensions[1];

        //IJ.write("numChannelsToDo: " + numChannelsToDo + "  " + Dimensions[2]);
        //IJ.write("Time Steps: " + Dimensions[4]);
        ImagePlus heightMap = NewImage.createFloatImage("HM_" + stackTitle, stack.getWidth(), stack.getHeight(), numChannelsToDo * Dimensions[4], NewImage.FILL_BLACK);
        ImageStack heightMapStack = heightMap.getStack();
        heightMap.setCalibration(myCal);
        heightMap.setDimensions(numChannelsToDo, 1, Dimensions[4]);

        ImagePlus qrMap = NewImage.createFloatImage("QR_" + stackTitle, stack.getWidth(), stack.getHeight(), numChannelsToDo * Dimensions[4], NewImage.FILL_BLACK);
        ImageStack qrMapStack = qrMap.getStack();
        qrMap.setCalibration(myCal);
        qrMap.setDimensions(numChannelsToDo, 1, Dimensions[4]);

        ImagePlus qrSharpness = NewImage.createFloatImage("QRS_" + stackTitle, stack.getWidth(), stack.getHeight(), numChannelsToDo * Dimensions[4], NewImage.FILL_BLACK);
        ImageStack qrSharpnessStack = qrSharpness.getStack();
        qrSharpness.setCalibration(myCal);
        qrSharpness.setDimensions(numChannelsToDo, 1, Dimensions[4]);

/*
        ImagePlus heightMapDerivative = NewImage.createFloatImage("HMD_" + stackTitle, stack.getWidth(), stack.getHeight(), numChannelsToDo * Dimensions[4], NewImage.FILL_BLACK);
        ImageStack heightMapDerivativeStack = heightMapDerivative.getStack();
        heightMapDerivative.setCalibration(myCal);
        heightMapDerivative.setDimensions(numChannelsToDo, 1, Dimensions[4]);
*/

        int startChannel = 0;
        int endChannel = Dimensions[2];

        if (currentChannelOnly) {
            startChannel = sourceImagePlus.getChannel() - 1;
            endChannel = startChannel + 1;
            //IJ.write("startChannel: " + startChannel);
            //IJ.write("endChannel: " + endChannel);
        }

        for (int time = 0; time < Dimensions[4]; time++) {
            for (int channel = startChannel; channel < endChannel; channel++) {
                FloatProcessor heightMapProcessor = (FloatProcessor) heightMap.getProcessor();
                //FloatProcessor heightMapDerivativeProcessor = (FloatProcessor) heightMapDerivative.getProcessor();
                FloatProcessor qrMapProcessor = (FloatProcessor) qrMap.getProcessor();
                FloatProcessor qrSharpnessProcessor = (FloatProcessor) qrSharpness.getProcessor();


                int outputSlice;
                if (currentChannelOnly)
                    outputSlice = time + 1;
                else
                    outputSlice = time * channel + channel + 1;

                //IJ.write("time: "+ time);
                //IJ.write("time: "+ time);
                //IJ.write("outputSlice: "+ outputSlice);
                float[] heightPixels = (float[]) heightMapStack.getPixels(outputSlice);
                int[] heightPixelsInt = new int[heightPixels.length];
                short[] maxValue = new short[dimension];
                boolean[] heightAboveThreshold = new boolean[dimension];

                float[] qrPixels = (float[]) qrMapStack.getPixels(outputSlice);
                Arrays.fill(qrPixels, -1.f);
                //float[] heightPixelsDerivative = (float[]) heightMapDerivativeStack.getPixels(outputSlice);
                //Arrays.fill(heightPixelsDerivative, -1.f);

                float[] qrSharpnessPixels = (float[]) qrSharpnessStack.getPixels(outputSlice);
                Arrays.fill(qrSharpnessPixels, 0.f);


                //IJ.write(Integer.toString(threshold & 0xffff));
                for (int ii = 0; ii < dimension; ii++) {
                    heightAboveThreshold[ii] = false;
                    heightPixels[ii] = (float) 1.0;
                    heightPixelsInt[ii] = -1;
                    maxValue[ii] = pixels[ii];
                    //centerOfMassNumerator[ii] = (float) 0.0;
                    //centerOfMassDenominator[ii] = (float) 0.0;
                }


                if (doHM) {
                    heightMap.show();

                    for (int slice = 1; slice < Dimensions[3]; slice++) {
                        IJ.showProgress(slice, Dimensions[3]);
                        pixels = (short[]) stack.getPixels(1 + channel + slice * Dimensions[2] + time * Dimensions[2] * Dimensions[3]);
                        for (int jj = 0; jj < dimension; jj++) {
                            if (pixels[jj] > maxValue[jj]) {
                                maxValue[jj] = pixels[jj];
                                heightPixels[jj] = (float) ((Dimensions[3] - slice) * myCal.pixelDepth);
                                //IJ.write("stack.getSize()" + stack.getSize());
                                heightPixelsInt[jj] = slice;
                            }
                            //centerOfMassDenominator[jj] += pixels[jj] & 0xffff;
                            //centerOfMassNumerator[jj] += (pixels[jj] & 0xffff) * (float) (stack.getSize() - slice);

                        }


                        heightMapProcessor.resetMinAndMax();
                        heightMapProcessor.findMinAndMax();
                        heightMapProcessor.setMinAndMax(0.0f, Dimensions[3] * myCal.pixelDepth);
                        heightMap.updateAndDraw();


                    }
                    for (int ii = 0; ii < dimension; ii++) {
                        if ((maxValue[ii] & 0xffff) < threshold) {
                            heightPixels[ii] = -1.0f;
                        }
                    }
                    heightMapProcessor.resetMinAndMax();
                    heightMapProcessor.findMinAndMax();
                    heightMapProcessor.setMinAndMax(heightMapProcessor.getMin(), heightMapProcessor.getMax());
                    heightMap.updateAndDraw();

                }

                if (doHM & doQR){
                    qrMap.show();
                    if (showQRS)
                        qrSharpness.show();
                    for (int xyPixel = 0; xyPixel < dimension; xyPixel++) {

                        if (xyPixel % 1000 == 0){
                            qrMapProcessor.setMinAndMax(0.0f, Dimensions[3] * myCal.pixelDepth);
                            qrMap.updateAndDraw();
                        }
                        //IJ.write("xyPixel: " + xyPixel);
                        int qWindowMin, qWindowMax;
                        qWindowMin = heightPixelsInt[xyPixel] - qWindow;
                        qWindowMax = heightPixelsInt[xyPixel] + qWindow;

                        if (qWindowMin < 1)
                            qWindowMin = 1;

                        if (qWindowMax > Dimensions[3])
                            qWindowMax = Dimensions[3];

                        double[] QuadraticWindow = new double[qWindowMax-qWindowMin];
                        double[] QuadraticWindowX = new double[qWindowMax-qWindowMin];

                        double sumN=0., sumXi=0., sumXi2=0., sumXi3=0., sumXi4=0.;
                        double sumYi=0., sumXiYi=0., sumXi2Yi=0.;

                        for (int slice = 0; slice<QuadraticWindow.length; slice++){
                            QuadraticWindow[slice] = ((short[]) stack.getPixels(1 + channel + (qWindowMin+slice) * Dimensions[2] + time * Dimensions[2] * Dimensions[3]))[xyPixel] & 0xffff;
                            QuadraticWindowX[slice]= qWindowMin+slice;
                            sumN  += 1.;
                            sumXi += qWindowMin+slice;
                            sumXi2 += Math.pow((qWindowMin+slice), 2.);
                            sumXi3 += Math.pow((qWindowMin+slice), 3.);
                            sumXi4 += Math.pow((qWindowMin+slice), 4.);
                            sumYi += QuadraticWindow[slice];
                            sumXiYi += QuadraticWindow[slice]*(qWindowMin+slice);
                            sumXi2Yi += QuadraticWindow[slice]*Math.pow(qWindowMin+slice, 2.);

                        }
                        double[][] Xd = {{sumN, sumXi, sumXi2}, {sumXi, sumXi2, sumXi3}, {sumXi2, sumXi3, sumXi4}};
                        Matrix X = new Matrix(Xd);

                        double[][] Yd = {{sumYi},{sumXiYi},{sumXi2Yi}};
                        Matrix Y = new Matrix(Yd);

                        Matrix C = X.solve(Y);
                        double[] Cd = C.getRowPackedCopy();
                        // y=ax^2 + bx + c
                        // dy/dx = 2ax +b
                        // peak location = -b/2a

                        double b = Cd[1];
                        double a = Cd[2];

                        qrSharpnessPixels[xyPixel] = (float) a;

                        float maxima = (float) (b/-2./a);

                        if (heightPixels[xyPixel] < 0.f)
                            qrPixels[xyPixel] = -1.f;
                        else if ((a>=0.)|(b <= 0.)){
                            qrPixels[xyPixel] = -1.f;
                            //IJ.write("Bad values: " + a + ",  "+ b);
                        }else if(maxima < 0.  | maxima > (float)Dimensions[3]){
                            qrPixels[xyPixel] = -1.f;
                        }

                        else{
                            qrPixels[xyPixel] = ((float)Dimensions[3]-maxima) * (float)myCal.pixelDepth; //a should be negative, b should be positive
                        }
                    }
                    qrMapProcessor.resetMinAndMax();
                    qrMapProcessor.findMinAndMax();
                    qrMapProcessor.setMinAndMax(0.0f, Dimensions[3] * myCal.pixelDepth);
                    qrMap.updateAndDraw();

                    if(showQRS){
                        qrSharpnessProcessor.resetMinAndMax();
                        qrSharpnessProcessor.findMinAndMax();
                        qrSharpnessProcessor.setMinAndMax(qrMapProcessor.minValue(), qrMapProcessor.maxValue());
                    }

                }

                if(doHM & saturateMax){
                    for (int xyPixel = 0; xyPixel < dimension; xyPixel++){
                        if (heightPixelsInt[xyPixel]>=0 & (maxValue[xyPixel] & 0xffff) > threshold){
                            ((short[]) stack.getPixels(1 + channel + heightPixelsInt[xyPixel] * Dimensions[2] + time * Dimensions[2] * Dimensions[3]))[xyPixel]=Short.MAX_VALUE;
                        }
                    }
                }


            }
        }
    }

}
