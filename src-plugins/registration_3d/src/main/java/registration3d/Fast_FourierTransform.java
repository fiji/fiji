package registration3d;
/**
 * <p>Title: Fast Fourier Transform (2D/3D) Plugin for ImageJ</p>
 *
 * <p>Description: Computes the Fourier Transform of an 2D/3D image and outputs the power- and phase-spectrum</p>
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
 * @author Stephan Preibisch
 * @version 1.0
 */

import ij.IJ;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.process.StackConverter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

import edu.mines.jtk.dsp.FftComplex;
import edu.mines.jtk.dsp.FftReal;

public class Fast_FourierTransform implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
    private static String methodList[] = {"No Logarithm", "Base-e Logarithm", "Base-10 Logarithm", "Generalized Logarithm (gLog, c = 2)"};
    private static String[] colorList = {"Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue"};

	public Fast_FourierTransform()
    {
    }

    /**
     * This method will be called when running the PlugIn, it coordinates the main process.
     *
     * @param args UNUSED

     * @author   Stephan Preibisch
     */
    public void run(String arg)
    {
        String fftdirection;        
        boolean multiThreaded;
        boolean windowing, isRGB;
        String behaviour, channel;
        String powerName, phaseName;
        ImagePlus power = null, phase = null;

    	ImagePlus imp = WindowManager.getCurrentImage();

	if (null == imp) {
		IJ.noImage();
		return;
	}
  
    	if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256)
    	{
    		isRGB = true;

    		if (imp.getType() == ImagePlus.COLOR_256)
        	{
	    		 if (imp.getStackSize() > 1)
	    			 new StackConverter(imp).convertToRGB();
	    		 else
	    			 imp.setProcessor(imp.getTitle(), imp.getProcessor().convertToRGB());
        	}
   			
    	}
    	else
    		isRGB = false;

        if (null == imp)
        {
            IJ.log("No images open.");
            return;
        }

        //
        // Show the dialog
        //
        GenericDialog gd = new GenericDialog("Fast Fourier Transform (2D/3D)");
        
        String FFTs[] = {"Forward", "Backward"};
        
		gd.addChoice("Use_Channel", colorList, colorList[colorList.length - 1]);
		
		if (!isRGB)
			((Component)gd.getChoices().get(0)).setEnabled(false);
		
        gd.addChoice("Direction_of_FFT_Transform", FFTs, FFTs[0]);
        gd.addCheckbox("Use_Multi-Threaded FFT", true);
        gd.addCheckbox("Windowing", true);
        gd.addChoice("Type_of_Logarithm_for_Power_Spectrum", methodList, methodList[3]);
        /*gd.addMessage("");
        gd.addMessage("Some explanation on the Windowing:");
        gd.addMessage("The FFT assumes that your image data is continious (imagine your image on a sphere)");
        gd.addMessage("If your image is not, which is the case for most images, a workaround is");
        gd.addMessage("to extend the image by its mirrored content in all directions and then fade that to black.");
        gd.addMessage("");
        gd.addMessage("Some explanation on the Logarithms:");
        gd.addMessage("When computing the Base-n Logarithms, a value of 1 has to be added to all values first!");
        gd.addMessage("(Otherwise some values will lie between -Infinity and Zero)");
        gd.addMessage("The generalized logarithm uses an approximation for values < 1");
        gd.addMessage("which is bijective (invertable)");
        gd.addMessage("gLog(x) = Log10((x + Math.sqrt(x * x + c * c)) / 2.0)");*/
        //gd.addMessage("Durbin, Blythe; Rocke, David (2003) Estimation of transformation parameters for microarray data");
        //gd.addMessage("Bioinformatics 19:1360-1367");
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n"+myURL);
		
		MultiLineLabel text = (MultiLineLabel)gd.getMessage();
		addHyperLinkListener(text);		        
        
        gd.showDialog();

        if (gd.wasCanceled())
            return;
                
        channel = gd.getNextChoice();
        fftdirection = gd.getNextChoice();        
        multiThreaded = gd.getNextBoolean();
        windowing = gd.getNextBoolean();
        behaviour = gd.getNextChoice();
        
        boolean forward = true;
        
        if (fftdirection.equals(FFTs[1]))
        {
        	forward = false;
        	
    		// get list of image stacks
    		int[] idList = WindowManager.getIDList();

    		if (idList == null || idList.length < 2)
    		{
    			IJ.error("You need two open images, the power and the phase spectrum.");
    			return;
    		}

    		String[] list = new String[idList.length];

    		for (int i = 0; i < idList.length; i++)
    			list[i] = WindowManager.getImage(idList[i]).getTitle();

    		GenericDialog gdInv = new GenericDialog("Images for inverse FFT");
    		
    		gdInv.addChoice("Power_Spectrum", list, list[0]);
    		gdInv.addChoice("Phase_Spectrum", list, list[1]);    		
    		
            gdInv.showDialog();

            if (gdInv.wasCanceled())
                return;
            
    		powerName = gdInv.getNextChoice();
    		phaseName = gdInv.getNextChoice();
    		
    		power = WindowManager.getImage(powerName);
			phase = WindowManager.getImage(phaseName);    		
        }      
        
        if (forward)
        	computeForwardTransform(imp, windowing, behaviour, channel, multiThreaded);
        else
        	computeBackwardTransform(power, phase, behaviour, multiThreaded);
    }

	private final void addHyperLinkListener(final MultiLineLabel text)
	{
		text.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				try
				{
					BrowserLauncher.openURL(myURL);
				}
				catch (Exception ex)
				{
					IJ.error("" + ex);
				}
			}
			public void mouseEntered(MouseEvent e)
			{
				text.setForeground(Color.BLUE);
				text.setCursor(new Cursor(Cursor.HAND_CURSOR));				
			}
			public void mouseExited(MouseEvent e)
			{
				text.setForeground(Color.BLACK);
				text.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}			 
		});
	}
    
    public void computeBackwardTransform(ImagePlus power, ImagePlus phase, String behaviour, boolean multiThreaded)
    {
    	if (power.getStackSize() != phase.getStackSize() || power.getWidth() != phase.getWidth() || power.getHeight() != phase.getHeight())
    	{
    		IJ.error("Power and Phase Spectrum are not same size. Tip: Extend or crop one of both.");
    		return;
    	}

    	boolean _3D;
    	FloatArray powerSpectrum, phaseSpectrum;
    	
        //
        // check whether it is a 3D image and not RGB
        //
        if (power.getStackSize() > 1)
        {
            ImageStack powerStack = power.getStack();
            ImageStack phaseStack = phase.getStack();
            
            _3D = true;
            powerSpectrum = StackToFloatArray(powerStack);
            phaseSpectrum = StackToFloatArray(phaseStack);            
        }
        else
        {
            _3D = false;
            
            powerSpectrum = ImageToFloatArray(power.getProcessor());
            phaseSpectrum = ImageToFloatArray(phase.getProcessor());
        }        

        // rearrange the data for the backtransform
        if (_3D)
        {
	        rearrangeFFT((FloatArray3D)powerSpectrum);
	        rearrangeFFT((FloatArray3D)phaseSpectrum);
        }
        else
        {
	        rearrangeFFT((FloatArray2D)powerSpectrum);
	        rearrangeFFT((FloatArray2D)phaseSpectrum);        	
        }
        
        // un-log the power spectrum
        if (behaviour.equals(methodList[3]))
        {
        	// gLog
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)gLogInv(powerSpectrum.data[i], 2);
        	
        }
        else if (behaviour.equals(methodList[2]))
        {
        	// Log10
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)Math.pow(10, powerSpectrum.data[i]) - 1;
        	
        }
        else if ((behaviour.equals(methodList[1])))
        {
        	// Loge
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)Math.pow(Math.E, powerSpectrum.data[i]) - 1;
        }

        
        // compute the complex numbers
        float[] complex = computeComplexValues(powerSpectrum.data, phaseSpectrum.data);        
        FloatArray img;        
        int complexWidth;
                
        if (_3D)
        {
        	img = new FloatArray3D(complex, ((FloatArray3D)powerSpectrum).width*2, ((FloatArray3D)powerSpectrum).height,((FloatArray3D)powerSpectrum).depth);
        	complexWidth = ((FloatArray3D)img).width;
        }
        else
        {
        	img = new FloatArray2D(complex, ((FloatArray2D)powerSpectrum).width*2, ((FloatArray2D)powerSpectrum).height);
        	complexWidth = ((FloatArray2D)img).width;
        }
        
        powerSpectrum.data = phaseSpectrum.data = null;
        powerSpectrum = phaseSpectrum = null;
        
        
        int nfft = ((complexWidth/2)-1)*2;
        	
        // compute Inverse FFT
        if (_3D)
        	img = computeInvFFT((FloatArray3D)img, nfft, multiThreaded);
        else
        	img = computeInvFFT((FloatArray2D)img, nfft);
        
        // display+
        if (_3D)
        	FloatArrayToStack((FloatArray3D)img, "Inverse FFT", 0, 0).show();
        else
        	FloatArrayToImagePlus((FloatArray2D)img, "Inverse FFT", 0, 0).show();
        
        img.data = null; img = null;
    }
    
    public void computeForwardTransform(ImagePlus imp, boolean windowing, String behaviour, String rgbType, boolean multiThreaded)
    {
        String imageName = imp.toString();        
    	
    	boolean _3D;
        FloatArray img;

        //
        // check whether it is a 3D image and not RGB
        //
        if (imp.getStackSize() > 1)
        {
            ImageStack stack = imp.getStack();
            _3D = true;
            img = StackToFloatArray(stack, rgbType);
        }
        else
        {
            _3D = false;
            img = ImageToFloatArray(imp.getProcessor(), rgbType);
        }        
    	
        if (windowing)
        {
        	int imgW, imgH, imgD = 0;
        	
        	if (_3D)
        	{
        		imgW = ((FloatArray3D)img).width;
        		imgH = ((FloatArray3D)img).height;
        		imgD = ((FloatArray3D)img).depth;
        	}
        	else
        	{
        		imgW = ((FloatArray2D)img).width;
        		imgH = ((FloatArray2D)img).height;        		
        	}
        	
			int extW = imgW / 4;
			int extH = imgH / 4;
			int extD = imgD / 4;

			// add an even number so that both sides extend equally
			if (extW % 2 != 0) extW++;
			if (extH % 2 != 0) extH++;
			if (extD % 2 != 0) extD++;

			// extend images
			if (_3D)
				img = extendImageMirror((FloatArray3D)img, imgW + extW, imgH + extH, imgD + extD);
			else
				img = extendImageMirror((FloatArray2D)img, imgW + extW, imgH + extH);
			
			// apply the windowing
        	if (_3D)
        		exponentialWindow((FloatArray3D)img);
        	else
        		exponentialWindow((FloatArray2D)img);
        }
        
        // do the zero-padding
        if (_3D)
        	img = zeroPadImage((FloatArray3D)img);
        else
        	img = zeroPadImage((FloatArray2D)img);
        
        // compute the FFT
        if (_3D)
            img = computeFFT((FloatArray3D)img, multiThreaded);
        else
            img = computeFFT((FloatArray2D)img);
        	
        // compute Power and Phase Spectrum
        float[] power = computePowerSpectrum(img.data);
        float[] phase = computePhaseSpectrum(img.data);

        // convert to FloatArray3D
        FloatArray powerSpectrum, phaseSpectrum;
        
        if (_3D)
        {
        	powerSpectrum = new FloatArray3D( power, ((FloatArray3D)img).width/2, ((FloatArray3D)img).height, ((FloatArray3D)img).depth);
        	phaseSpectrum = new FloatArray3D( phase, ((FloatArray3D)img).width/2, ((FloatArray3D)img).height, ((FloatArray3D)img).depth);
        }
        else
        {
        	powerSpectrum = new FloatArray2D( power, ((FloatArray2D)img).width/2, ((FloatArray2D)img).height);
        	phaseSpectrum = new FloatArray2D( phase, ((FloatArray2D)img).width/2, ((FloatArray2D)img).height);
        }

        // release the old data
        img.data = null; img = null;

        if (behaviour.equals(methodList[3]))
        {
        	// gLog
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)gLog(powerSpectrum.data[i], 2);
        	
        }
        else if (behaviour.equals(methodList[2]))
        {
        	// Log10
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)Math.log10(1 + powerSpectrum.data[i]);
        	
        }
        else if ((behaviour.equals(methodList[1])))
        {
        	// Loge
    		for (int i = 0; i < powerSpectrum.data.length; i++)
    			powerSpectrum.data[i] = (float)Math.log(1 + powerSpectrum.data[i]);
        }
                
        // rearrange the data
        if (_3D)
        {
	        rearrangeFFT((FloatArray3D)powerSpectrum);
	        rearrangeFFT((FloatArray3D)phaseSpectrum);
        }
        else
        {
	        rearrangeFFT((FloatArray2D)powerSpectrum);
	        rearrangeFFT((FloatArray2D)phaseSpectrum);        	
        }
        
        // display the data
        if (_3D)
        {
        	FloatArrayToStack((FloatArray3D)powerSpectrum, "Power of " + imageName, 0, 0).show();
        	FloatArrayToStack((FloatArray3D)phaseSpectrum, "Phase of " + imageName, 0, 0).show();
        }
        else
        {
        	FloatArrayToImagePlus((FloatArray2D)powerSpectrum, "Power of " + imageName, 0, 0).show();
        	FloatArrayToImagePlus((FloatArray2D)phaseSpectrum, "Phase of " + imageName, 0, 0).show();
        }     
        
        powerSpectrum.data = phaseSpectrum.data = null;
        powerSpectrum = phaseSpectrum = null;
    }

	private void rearrangeFFT(FloatArray2D values)
	{
		float[] fft = values.data;
		int w = values.width;
		int h = values.height;

		int halfDimYRounded = ( int )( h / 2 );

		float buffer[] = new float[w];
		int pos1, pos2;

		for (int y = 0; y < halfDimYRounded; y++)
		{
			// copy upper line
			pos1 = y * w;
			for (int x = 0; x < w; x++)
				buffer[x] = fft[pos1++];

			// copy lower line to upper line
			pos1 = y * w;
			pos2 = (y+halfDimYRounded) * w;
			for (int x = 0; x < w; x++)
				fft[pos1++] = fft[pos2++];

			// copy buffer to lower line
			pos1 = (y+halfDimYRounded) * w;
			for (int x = 0; x < w; x++)
				fft[pos1++] = buffer[x];
		}
	}

	private void rearrangeFFT(FloatArray3D values)
	{
		int w = values.width;
		int h = values.height;
		int d = values.depth;

		//int halfDimYRounded = ( int ) Math.round( h / 2d );
		//int halfDimZRounded = ( int ) Math.round( d / 2d );
		int halfDimYRounded = ( int ) ( h / 2 );
		int halfDimZRounded = ( int ) ( d / 2 );

		float buffer[] = new float[h];

		// swap data in y-direction
		for ( int x = 0; x < w; x++ )
			for ( int z = 0; z < d; z++ )
			{
				// cache first "half" to buffer
				for ( int y = 0; y < h / 2; y++ )
					buffer[ y ] = values.get(x,y,z);

				// move second "half" to first "half"
				for ( int y = 0; y < halfDimYRounded; y++ )
					values.set(values.get(x, y + h/2, z), x, y, z);

				// move data in buffer to second "half"
				for ( int y = halfDimYRounded; y < h; y++ )
					values.set(buffer[ y - halfDimYRounded ], x, y, z);
			}

		buffer = new float[d];

		// swap data in z-direction
		for ( int x = 0; x < w; x++ )
			for ( int y = 0; y < h; y++ )
			{
				// cache first "half" to buffer
				for ( int z = 0; z < d/2; z++ )
					buffer[ z ] = values.get(x, y, z);

				// move second "half" to first "half"
				for ( int z = 0; z < halfDimZRounded; z++ )
					values.set(values.get(x, y, z + d/2 ), x, y, z);

				// move data in buffer to second "half"
				for ( int z = halfDimZRounded; z<d; z++ )
					values.set(buffer[ z - halfDimZRounded ], x, y, z);
			}

	}

	private static double gLog(double z, double c)
	{
		if (c == 0)
			return z;
		else
			return Math.log10((z + Math.sqrt(z * z + c * c)) / 2.0);
	}

	private static double gLogInv(double w, double c)
	{
		if (c == 0)
			return w;
		else
			return Exp10(w) - (((c * c) * Exp10( -w)) / 4.0);
	}

	private static double Exp10(double x)
	{
		return (Math.pow(10, x));
	}

	private float[] computeComplexValues(float[] power, float[] phase)
	{
		if (power.length != phase.length)
		{
			System.err.println("Power and Phase Spectrum are not of same size.");
			return null;
		}
		
		float[] complex = new float[power.length * 2];
		
		for (int pos = 0; pos < power.length; pos++)
		{
			if (power[pos] == 0)
			{
				complex[pos*2] = 0;
				complex[pos*2+1] = 0;
			}
			else
			{
				complex[pos*2] = (float)(Math.cos(phase[pos]) * power[pos]);
				complex[pos*2 + 1] = (float)(Math.sin(phase[pos]) * power[pos]);
			}
		}
			
		
		return complex;
	}

	private float[] computePhaseSpectrum(float[] complex)
	{
		int wComplex = complex.length / 2;

		float[] phaseSpectrum = new float[wComplex];
		float a,b;

		for (int pos = 0; pos < phaseSpectrum.length; pos++)
		{
			a = complex[pos * 2];
			b = complex[pos * 2 + 1];

			if (a != 0.0 || b != 0)
				phaseSpectrum[pos] = (float)Math.atan2(b,a);
			else
				phaseSpectrum[pos] = 0;
		}
		return phaseSpectrum;
	}
	
	private float[] computePowerSpectrum(float[] complex)
	{
		int wComplex = complex.length / 2;

		float[] powerSpectrum = new float[wComplex];

		for (int pos = 0; pos < wComplex; pos++)
			powerSpectrum[pos] = (float)Math.sqrt(Math.pow(complex[pos*2],2) + Math.pow(complex[pos*2 + 1],2));

		return powerSpectrum;
	}

	private FloatArray2D zeroPadImage(FloatArray2D img)
	{
		int widthFFT = FftReal.nfftFast(img.width);
		int heightFFT = FftComplex.nfftFast(img.height);

		FloatArray2D result = zeroPad(img, widthFFT, heightFFT);
		img.data = null;
		img = null;

		return result;
	}

	private FloatArray3D zeroPadImage(FloatArray3D img)
	{
		int widthFFT = FftReal.nfftFast(img.width);
		int heightFFT = FftComplex.nfftFast(img.height);
		int depthFFT = FftComplex.nfftFast(img.depth);

		FloatArray3D result = zeroPad(img, widthFFT, heightFFT, depthFFT);
		img.data = null;
		img = null;

		return result;
	}


	private void exponentialWindow(FloatArray2D img)
	{
		double a = 1000;

		// create lookup table
		double weightsX[] = new double[img.width];
		double weightsY[] = new double[img.height];

		for (int x = 0; x < img.width; x++)
		{
			double relPos = (double) x / (double) (img.width - 1);

			if (relPos <= 0.5)
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
		{
			double relPos = (double) y / (double) (img.height - 1);

			if (relPos <= 0.5)
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
			for (int x = 0; x < img.width; x++)
				img.set((float) (img.get(x, y) * weightsX[x] * weightsY[y]), x, y);
	}

	private void exponentialWindow(FloatArray3D img)
	{
		double a = 1000;

		// create lookup table
		double weightsX[] = new double[img.width];
		double weightsY[] = new double[img.height];
		double weightsZ[] = new double[img.depth];

		for (int x = 0; x < img.width; x++)
		{
			double relPos = (double) x / (double) (img.width - 1);

			if (relPos <= 0.5)
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
		{
			double relPos = (double) y / (double) (img.height - 1);

			if (relPos <= 0.5)
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
		{
			double relPos = (double) z / (double) (img.depth - 1);

			if (relPos <= 0.5)
				weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
			for (int y = 0; y < img.height; y++)
				for (int x = 0; x < img.width; x++)
					img.set((float) (img.get(x, y, z) * weightsX[x] * weightsY[y] * weightsZ[z]), x, y, z);
	}

	private FloatArray2D extendImageMirror(FloatArray2D ip, int width, int height)
	{
		FloatArray2D image = new FloatArray2D(width, height);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;

		if (offsetX < 0)
		{
			IJ.error("Fast_FourierTransform.extendImageMirror(): Extended size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Fast_FourierTransform.extendImageMirror(): Extended size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				image.set(ip.getMirror(x - offsetX, y - offsetY), x, y);

		return image;
	}
	
	private FloatArray3D extendImageMirror(FloatArray3D ip, int width, int height, int depth)
	{
		FloatArray3D image = new FloatArray3D(width, height, depth);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;
		int offsetZ = (depth - ip.depth) / 2;

		if (offsetX < 0)
		{
			IJ.error("Fast_FourierTransform.extendImageMirror(): Extended size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Fast_FourierTransform.extendImageMirror(): Extended size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		if (offsetZ < 0)
		{
			IJ.error("Fast_FourierTransform.extendImageMirror(): Extended size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++)
					image.set(ip.getMirror(x - offsetX, y - offsetY, z - offsetZ), x, y, z);

		return image;
	}

	private FloatArray2D zeroPad(FloatArray2D ip, int width, int height)
	{
		FloatArray2D image = new FloatArray2D(width,  height);

		int offsetX = (width - ip.width)/2;
		int offsetY = (height - ip.height)/2;

		if (offsetX < 0)
		{
			System.err.println("Fast_FourierTransform.ZeroPad(): Zero-Padding size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			System.err.println("Fast_FourierTransform.ZeroPad(): Zero-Padding size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		int count = 0;

		for (int y = 0; y < ip.height; y++)
			for (int x = 0; x < ip.width; x++)
				image.set(ip.data[count++], x+offsetX, y+offsetY);

		return image;
	}

	private FloatArray3D zeroPad(FloatArray3D ip, int width, int height, int depth)
	{
		FloatArray3D image = new FloatArray3D(width, height, depth);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;
		int offsetZ = (depth - ip.depth) / 2;

		if (offsetX < 0)
		{
			System.err.println("Fast_FourierTransform.ZeroPad(): Zero-Padding size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			System.err.println("Fast_FourierTransform.ZeroPad(): Zero-Padding size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		if (offsetZ < 0)
		{
			System.err.println("Fast_FourierTransform.ZeroPad(): Zero-Padding size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int z = 0; z < ip.depth; z++)
			for (int y = 0; y < ip.height; y++)
				for (int x = 0; x < ip.width; x++)
				image.set(ip.get(x,y,z), x + offsetX, y + offsetY, z + offsetZ);

		return image;
	}
    
	private FloatArray2D computeFFT(FloatArray2D img)
	{
		FloatArray2D fft = pffft2D(img, false);
		img.data = null; img = null;

		return fft;
	}

	private FloatArray2D computeInvFFT(FloatArray2D values, int nfft)
	{
		FloatArray2D img = pffftInv2D(values, nfft);
		values.data = null; values = null;
		
		return img;
	}

	private FloatArray3D computeInvFFT(FloatArray3D values, int nfft, boolean multiThreaded)
	{
		FloatArray3D img;
		
		if (multiThreaded)			
			img = pffftInv3DMT(values, nfft);
		else
			img = pffftInv3D(values, nfft);
		
		values.data = null; values = null;
		
		return img;
	}

	private FloatArray2D pffft2D(FloatArray2D values, boolean scale)
	{
		int height = values.height;
		int width = values.width;
		int complexWidth = (width / 2 + 1) * 2;

		FloatArray2D result = new FloatArray2D(complexWidth, height);

		//do fft's in x direction
		float[] tempIn = new float[width];
		float[] tempOut;

		FftReal fft = new FftReal(width);

		for (int y = 0; y < height; y++)
		{
			tempOut = new float[complexWidth];

			for (int x = 0; x < width; x++)
				tempIn[x] = values.get(x, y);

			fft.realToComplex( -1, tempIn, tempOut);

			if (scale)
				fft.scale(width, tempOut);

			for (int x = 0; x < complexWidth; x++)
				result.set(tempOut[x], x, y);
		}

		// do fft's in y-direction on the complex numbers
		tempIn = new float[height * 2];

		FftComplex fftc = new FftComplex(height);

		for (int x = 0; x < complexWidth / 2; x++)
		{
			tempOut = new float[height * 2];

			for (int y = 0; y < height; y++)
			{
				tempIn[y * 2] = result.get(x * 2, y);
				tempIn[y * 2 + 1] = result.get(x * 2 + 1, y);
			}

			fftc.complexToComplex( -1, tempIn, tempOut);

			for (int y = 0; y < height; y++)
			{
				result.set(tempOut[y * 2], x * 2, y);
				result.set(tempOut[y * 2 + 1], x * 2 + 1, y);
			}
		}

		return result;
	}

	private FloatArray2D pffftInv2D(FloatArray2D values, int nfft)
	{
		int height = values.height;
		int width = nfft;
		int complexWidth = (width / 2 + 1) * 2;

		FloatArray2D result = new FloatArray2D(width, height);

		// do inverse fft's in y-direction on the complex numbers
		float[] tempIn = new float[height * 2];
		float[] tempOut;

		FftComplex fftc = new FftComplex(height);

		for (int x = 0; x < complexWidth / 2; x++)
		{
			tempOut = new float[height * 2];

			for (int y = 0; y < height; y++)
			{
				tempIn[y * 2] = values.get(x * 2, y);
				tempIn[y * 2 + 1] = values.get(x * 2 + 1, y);
			}

			fftc.complexToComplex(1, tempIn, tempOut);

			for (int y = 0; y < height; y++)
			{
				values.set(tempOut[y * 2], x * 2, y);
				values.set(tempOut[y * 2 + 1], x * 2 + 1, y);
			}
		}

		//do inverse fft's in x direction
		tempIn = new float[complexWidth];

		FftReal fft = new FftReal(width);

		for (int y = 0; y < height; y++)
		{
			tempOut = new float[width];

			for (int x = 0; x < complexWidth; x++)
				tempIn[x] = values.get(x, y);

			fft.complexToReal(1, tempIn, tempOut);

			//fft.scale(width, tempOut);
			for (int i = 0; i < tempOut.length; i++)
				tempOut[i] /= (float)(width * height);

			for (int x = 0; x < width; x++)
				result.set(tempOut[x], x, y);
		}

		return result;
	}

	
	private FloatArray3D computeFFT(FloatArray3D img, boolean multiThreaded)
	{
		FloatArray3D fft;
		
		if (multiThreaded)
			fft = pffft3DMT(img, false);
		else
			fft = pffft3D(img, false);
		img.data = null; img = null;

		return fft;
	}
	
	private FloatArray3D pffft3D(FloatArray3D values, boolean scale)
	{
		int height = values.height;
		int width = values.width;
		int depth = values.depth;
		int complexWidth = (width/2+1)*2;

		FloatArray3D result = new FloatArray3D(complexWidth, height, depth);

		//do fft's in x direction
		float[] tempIn = new float[width];
		float[] tempOut;

		FftReal fft = new FftReal(width);

		for (int z = 0; z < depth; z++)
			for (int y = 0; y < height; y++)
			{
				tempOut = new float[complexWidth];

				for (int x = 0; x < width; x++)
					tempIn[x] = values.get(x,y,z);

				fft.realToComplex( -1, tempIn, tempOut);

				if (scale)
					fft.scale(width, tempOut);

				for (int x = 0; x < complexWidth; x++)
					result.set(tempOut[x], x, y, z);
			}

		// do fft's in y-direction on the complex numbers
		tempIn = new float[height*2];

		FftComplex fftc = new FftComplex(height);

		for (int z = 0; z < depth; z++)
			for (int x = 0; x < complexWidth/2; x++)
			{
				tempOut = new float[height*2];

				for (int y = 0; y < height; y++)
				{
					tempIn[y*2] = result.get(x*2,y,z);
					tempIn[y*2+1] = result.get(x*2+1,y,z);
				}

				fftc.complexToComplex(-1, tempIn, tempOut);

				for (int y = 0; y < height; y++)
				{
					result.set(tempOut[y*2], x*2, y, z);
					result.set(tempOut[y*2+1], x*2+1, y, z);
				}
			}

		// do fft's in z-direction on the complex numbers
		tempIn = new float[depth*2];
		fftc = new FftComplex(depth);

		for (int y = 0; y < height; y++)
			for (int x = 0; x < complexWidth/2; x++)
			{
				//tempOut = new float[height*2];
				tempOut = new float[depth*2];

				for (int z = 0; z < depth; z++)
				{
					tempIn[z*2] = result.get(x*2,y,z);
					tempIn[z*2+1] = result.get(x*2+1,y,z);
				}

				fftc.complexToComplex(-1, tempIn, tempOut);

				for (int z = 0; z < depth; z++)
				{
					result.set(tempOut[z*2], x*2, y, z);
					result.set(tempOut[z*2+1], x*2+1, y, z);
				}
			}

		return result;
	}

	private FloatArray3D pffft3DMT(final FloatArray3D values, final boolean scale)
	{
		final int height = values.height;
		final int width = values.width;
		final int depth = values.depth;
		final int complexWidth = (width / 2 + 1) * 2;

		final FloatArray3D result = new FloatArray3D(complexWidth, height, depth);

		//do fft's in x direction
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = newThreads();
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					int myNumber = ai.getAndIncrement();

					float[] tempIn = new float[width];
					float[] tempOut;
					FftReal fft = new FftReal(width);

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int y = 0; y < height; y++)
							{
								tempOut = new float[complexWidth];

								for (int x = 0; x < width; x++)
									tempIn[x] = values.get(x, y, z);

								fft.realToComplex( -1, tempIn, tempOut);

								if (scale)
									fft.scale(width, tempOut);

								for (int x = 0; x < complexWidth; x++)
									result.set(tempOut[x], x, y, z);
							}
				}
			});
		startAndJoin(threads);

		//do fft's in y direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[height * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(height);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[height * 2];

								for (int y = 0; y < height; y++)
								{
									tempIn[y * 2] = result.get(x * 2, y, z);
									tempIn[y * 2 + 1] = result.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex( -1, tempIn, tempOut);

								for (int y = 0; y < height; y++)
								{
									result.set(tempOut[y * 2], x * 2, y, z);
									result.set(tempOut[y * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});

		startAndJoin(threads);

		//do fft's in z direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[depth * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(depth);

					int myNumber = ai.getAndIncrement();

					for (int y = 0; y < height; y++)
						if (y % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[depth * 2];

								for (int z = 0; z < depth; z++)
								{
									tempIn[z * 2] = result.get(x * 2, y, z);
									tempIn[z * 2 + 1] = result.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex( -1, tempIn, tempOut);

								for (int z = 0; z < depth; z++)
								{
									result.set(tempOut[z * 2], x * 2, y, z);
									result.set(tempOut[z * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});

		startAndJoin(threads);

		return result;
	}

	private FloatArray3D pffftInv3D(FloatArray3D values, int nfft)
	{
		int depth = values.depth;
		int height = values.height;
		int width = nfft;
		int complexWidth = (width/2+1)*2;

		FloatArray3D result = new FloatArray3D(width, height, depth);

		// do inverse fft's in z-direction on the complex numbers
		float[] tempIn = new float[depth*2];
		float[] tempOut;
		FftComplex fftc = new FftComplex(depth);

		for (int y = 0; y < height; y++)
			for (int x = 0; x < complexWidth/2; x++)
			{
				tempOut = new float[depth*2];

				for (int z = 0; z < depth; z++)
				{
					tempIn[z*2] = values.get(x*2,y,z);
					tempIn[z*2+1] = values.get(x*2+1,y,z);
				}

				fftc.complexToComplex(1, tempIn, tempOut);

				for (int z = 0; z < depth; z++)
				{
					values.set(tempOut[z*2], x*2, y, z);
					values.set(tempOut[z*2+1], x*2+1, y, z);
				}
			}


		// do inverse fft's in y-direction on the complex numbers
		tempIn = new float[height*2];
		fftc = new FftComplex(height);

		for (int z = 0; z < depth; z++)
			for (int x = 0; x < complexWidth/2; x++)
			{
				tempOut = new float[height*2];

				for (int y = 0; y < height; y++)
				{
					tempIn[y*2] = values.get(x*2,y, z);
					tempIn[y*2+1] = values.get(x*2+1,y, z);
				}

				fftc.complexToComplex(1, tempIn, tempOut);

				for (int y = 0; y < height; y++)
				{
					values.set(tempOut[y*2], x*2, y, z);
					values.set(tempOut[y*2+1], x*2+1, y, z);
				}
			}

		//do inverse fft's in x direction
		tempIn = new float[complexWidth];

		FftReal fft = new FftReal(width);

		for (int z = 0; z < depth; z++)
			for (int y = 0; y < height; y++)
			{
				tempOut = new float[width];

				for (int x = 0; x < complexWidth; x++)
					tempIn[x] = values.get(x,y,z);

				fft.complexToReal( 1, tempIn, tempOut);

				for (int i = 0; i < tempOut.length; i++)
					tempOut[i] /= (float)(width * height * depth);

				//fft.scale(width, tempOut);

				for (int x = 0; x < width; x++)
					result.set(tempOut[x], x, y, z);
			}

		return result;
	}

	private FloatArray3D pffftInv3DMT(final FloatArray3D values, final int nfft)
	{
		final int depth = values.depth;
		final int height = values.height;
		final int width = nfft;
		final int complexWidth = (width / 2 + 1) * 2;

		final FloatArray3D result = new FloatArray3D(width, height, depth);

		// do inverse fft's in z-direction on the complex numbers
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = newThreads();
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					int myNumber = ai.getAndIncrement();

					float[] tempIn = new float[depth * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(depth);

					for (int y = 0; y < height; y++)
						if (y % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[complexWidth];

								tempOut = new float[depth * 2];

								for (int z = 0; z < depth; z++)
								{
									tempIn[z * 2] = values.get(x * 2, y, z);
									tempIn[z * 2 + 1] = values.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex(1, tempIn, tempOut);

								for (int z = 0; z < depth; z++)
								{
									values.set(tempOut[z * 2], x * 2, y, z);
									values.set(tempOut[z * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});
		startAndJoin(threads);

		// do inverse fft's in y-direction on the complex numbers
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[height * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(height);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[height * 2];

								for (int y = 0; y < height; y++)
								{
									tempIn[y * 2] = values.get(x * 2, y, z);
									tempIn[y * 2 + 1] = values.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex(1, tempIn, tempOut);

								for (int y = 0; y < height; y++)
								{
									values.set(tempOut[y * 2], x * 2, y, z);
									values.set(tempOut[y * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});
		startAndJoin(threads);

		//do inverse fft's in x direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[complexWidth];
					float[] tempOut;
					FftReal fft = new FftReal(width);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int y = 0; y < height; y++)
							{
								tempOut = new float[width];

								for (int x = 0; x < complexWidth; x++)
									tempIn[x] = values.get(x, y, z);

								fft.complexToReal(1, tempIn, tempOut);

								for (int i = 0; i < tempOut.length; i++)
									tempOut[i] /= (float) (width * height * depth);

								//fft.scale(width, tempOut);

								for (int x = 0; x < width; x++)
									result.set(tempOut[x], x, y, z);
							}
				}
			});

		startAndJoin(threads);

		return result;
	}

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
    private ImagePlus FloatArrayToImagePlus(FloatArray2D image, String name, float min, float max)
    {
        ImagePlus imp = IJ.createImage(name, "32-Bit Black", image.width, image.height, 1);
        FloatProcessor ip = (FloatProcessor) imp.getProcessor();
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
     * @param ImageProcessor Will be overwritten with the img from the FloatArray2D
     * @param FloatArray2D The image as FloatArray2D
     * @return
     *
     * @author   Stephan Preibisch
     */
    private void FloatArrayToFloatProcessor(ImageProcessor ip, FloatArray2D pixels)
    {
        float[] img = new float[pixels.width * pixels.height];

        int count = 0;
        for (int y = 0; y < pixels.height; y++)
            for (int x = 0; x < pixels.width; x++)
                img[count] = pixels.data[count++];

        ip.setPixels(img);
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
	private ImagePlus FloatArrayToStack(FloatArray3D image, String name, float min, float max)
	{
        if (min == max)
        {
            float[] minmax = getMinMax(image);
            min = minmax[0];
            max = minmax[1];
        }

        int width = image.width;
        int height = image.height;
        int nstacks = image.depth;

        ImageStack stack = new ImageStack(width, height);

        for (int slice = 0; slice < nstacks; slice++)
        {
            ImagePlus impResult = IJ.createImage("Result", "32-Bit Black", width, height, 1);
            ImageProcessor ipResult = impResult.getProcessor();
            float[] sliceImg = new float[width * height];

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    sliceImg[y * width + x] = image.get(x, y, slice);

            ipResult.setPixels(sliceImg);

            ipResult.setMinAndMax(min, max);

            stack.addSlice("Slice " + slice, ipResult);
        }

        return new ImagePlus(name, stack);
    }

    private float[] getMinMax(FloatArray3D img)
    {
        float[] result = new float[2];

        result[0] = Float.MAX_VALUE;
        result[1] = Float.MIN_VALUE;

        for (int i = 0; i < img.data.length; i++)
        {
            if (img.data[i] < result[0])
                result[0] = img.data[i];

            if (img.data[i] > result[1])
                result[1] = img.data[i];
        }

        return result;
    }

    private Thread[] newThreads()
    {
        int nthread = Runtime.getRuntime().availableProcessors();
        return new Thread[nthread];
    }

    private void startAndJoin(Thread[] threads)
    {
        for (int ithread = 0; ithread < threads.length; ++ithread)
        {
            threads[ithread].setPriority(Thread.NORM_PRIORITY);
            threads[ithread].start();
        }

        try
        {
            for (int ithread = 0; ithread < threads.length; ++ithread)
                threads[ithread].join();
        } catch (InterruptedException ie)
        {
            throw new RuntimeException(ie);
        }
    }

	private FloatArray3D StackToFloatArray(ImageStack stack)
	{
		return StackToFloatArray(stack, null);
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
	private FloatArray3D StackToFloatArray(ImageStack stack, String handleRGB)
	{
		Object[] imageStack = stack.getImageArray();
		int width = stack.getWidth();
		int height = stack.getHeight();
		int nstacks = stack.getSize();
		
		int rgbType = -1;

		if (imageStack == null || imageStack.length == 0)
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (imageStack[0] instanceof int[])
		{
			if (handleRGB == null || handleRGB.trim().length() == 0)
				handleRGB = colorList[colorList.length - 1];
			
			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase()))
					rgbType = i;
			}
			
			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}

		FloatArray3D pixels = new FloatArray3D(width, height, nstacks);
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
		else if (imageStack[0] instanceof float[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				float[] pixelTmp = (float[])imageStack[countSlice];
				count = 0;

				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = pixelTmp[count++];
			}
		else if (imageStack[0] instanceof int[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				int[] pixelTmp = (int[])imageStack[countSlice];
				count = 0;
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = getPixelValueRGB(pixelTmp[count++], rgbType);				
			}
		else
		{
			IJ.error("StackToFloatArray: Unknown image type.");
			return null;
		}


		return pixels;
	}
	
	private float getPixelValueRGB(int rgb, int rgbType)
	{
		int r = (rgb&0xff0000)>>16;
        int g = (rgb&0xff00)>>8;
        int b = rgb&0xff;
        
        //colorList = {"Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue"};

        if (rgbType == 0)
        	return r;
        else if (rgbType == 1)
        	return g;
        else if (rgbType == 2)
        	return b;
        else if (rgbType == 3)
        	return (r+g)/2.0f;
        else if (rgbType == 4)
        	return (r+b)/2.0f;
        else if (rgbType == 5)
        	return (g+b)/2.0f;
        else
        	return (r+g+b)/3.0f;
	}

    private FloatArray2D ImageToFloatArray(ImageProcessor ip)
    {
    	return ImageToFloatArray(ip, null);
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
    private FloatArray2D ImageToFloatArray(ImageProcessor ip, String handleRGB)
    {
        FloatArray2D image;
        Object pixelArray = ip.getPixels();
        int count = 0;

		int rgbType = -1;


		if (ip instanceof ColorProcessor)
		{
			if (handleRGB == null || handleRGB.trim().length() == 0)
				handleRGB = colorList[colorList.length - 1];
			
			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase()))
					rgbType = i;
			}
			
			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}

        if (ip instanceof ByteProcessor)
        {
            image = new FloatArray2D(ip.getWidth(), ip.getHeight());
            byte[] pixels = (byte[]) pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xff;
        }
        else if (ip instanceof ShortProcessor)
        {
            image = new FloatArray2D(ip.getWidth(), ip.getHeight());
            short[] pixels = (short[]) pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++] & 0xffff;
        }
        else if (ip instanceof FloatProcessor)
        {
            image = new FloatArray2D(ip.getWidth(), ip.getHeight());
            float[] pixels = (float[]) pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                    image.data[count] = pixels[count++];
        }
        else if (ip instanceof ColorProcessor)
		{
            image = new FloatArray2D(ip.getWidth(), ip.getHeight());
			int[] pixels = (int[]) pixelArray;

            for (int y = 0; y < ip.getHeight(); y++)
                for (int x = 0; x < ip.getWidth(); x++)
                	image.data[count] = getPixelValueRGB(pixels[count++], rgbType);				
		}
		else
		{
			IJ.error("StackToFloatArray: Unknown image type.");
			return null;
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
        public abstract FloatArray clone();
    }


    /**
     * The 2D implementation of the FloatArray
     *
     * @author   Stephan Preibisch
     */
    public class FloatArray2D extends FloatArray
    {
        public int width = 0;
        public int height = 0;

        private int doubleWidth, doubleHeight;

        public FloatArray2D(int width, int height)
        {
            data = new float[width * height];
            this.width = width;
            this.height = height;

            doubleWidth = 2 * width;
            doubleHeight = 2 * height;
        }

        public FloatArray2D(float[] data, int width, int height)
        {
            this.data = data;
            this.width = width;
            this.height = height;

            doubleWidth = 2 * width;
            doubleHeight = 2 * height;
        }

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
            return data[getPos(x, y)];
        }

        public float getValueOutOfImage(int x, int y, float value)
        {
            if (x > 0 && x < width && y > 0 && y < height)
                return data[getPos(x, y)];
            else
                return value;
        }

        public float getFlipInRange(int x, int y)
        {
            if (x < 0) x = doubleWidth + x % doubleWidth;
            if (x >= doubleWidth) x = x % doubleWidth;
            if (x >= width) x = width - x % width - 1;

            if (y < 0) y = doubleHeight + y % doubleHeight;
            if (y >= doubleHeight) y = y % doubleHeight;
            if (y >= height) y = height - y % height - 1;

            return data[getPos(x, y)];
            //return get(flipInRange(x, width), flipInRange(y, height) );
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

            return data[getPos(x, y)];
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

            return data[getPos(x, y)];
        }

        public void set(float value, int x, int y)
        {
            data[getPos(x, y)] = value;
        }
    }


    /**
     * The 3D implementation of the FloatArray
     *
     * @author   Stephan Preibisch
     */
    public class FloatArray3D extends FloatArray
    {
        public int width = 0;
        public int height = 0;
        public int depth = 0;

        private int doubleWidth, doubleHeight, doubleDepth;

        public FloatArray3D(float[] data, int width, int height, int depth)
        {
            this.data = data;
            this.width = width;
            this.height = height;
            this.depth = depth;

            doubleWidth = 2 * width;
            doubleHeight = 2 * height;
            doubleDepth = 2 * depth;
        }

        public FloatArray3D(int width, int height, int depth)
        {
            data = new float[width * height * depth];
            this.width = width;
            this.height = height;
            this.depth = depth;

            doubleWidth = 2 * width;
            doubleHeight = 2 * height;
            doubleDepth = 2 * depth;
        }

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
            return data[getPos(x, y, z)];
        }

        public float getValueOutOfImage(int x, int y, int z, float value)
        {
            if (x > 0 && x < width && y > 0 && y < height && z > 0 && z < depth)
                return data[getPos(x, y, z)];
            else
                return value;
        }

        /**
         * Return the value at an arbitrary position, where the image data is flipped like that:
         *
         * Size = 3
         *
         * -4 -> 2
         * -3 -> 2
         * -2 -> 1
         * -1 -> 0
         * 0 -> 0
         * 1 -> 1
         * 2 -> 2
         * 3 -> 2
         * 4 -> 1
         * 5 -> 0
         * 6 -> 2
         *
         * @param x int x position
         * @param y int y position
         * @param z int z position
         * @return float The value
         */
        public float getFlipInRange(int x, int y, int z)
        {
            if (x < 0) x = doubleWidth + x % doubleWidth;
            if (x >= doubleWidth) x = x % doubleWidth;
            if (x >= width) x = width - x % width - 1;

            if (y < 0) y = doubleHeight + y % doubleHeight;
            if (y >= doubleHeight) y = y % doubleHeight;
            if (y >= height) y = height - y % height - 1;

            if (z < 0) z = doubleDepth + z % doubleDepth;
            if (z >= doubleDepth) z = z % doubleDepth;
            if (z >= depth) z = depth - z % depth - 1;

            return data[getPos(x, y, z)];
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

            return data[getPos(x, y, z)];
        }

        public void set(float value, int x, int y, int z)
        {
            data[getPos(x, y, z)] = value;
        }

        public FloatArray2D getXPlane(int x)
        {
            FloatArray2D plane = new FloatArray2D(height, depth);

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x, y, z), y, z);

            return plane;
        }

        public float[][] getXPlane_float(int x)
        {
            float[][] plane = new float[height][depth];

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane[y][z] = this.get(x, y, z);

            return plane;
        }

        public FloatArray2D getYPlane(int y)
        {
            FloatArray2D plane = new FloatArray2D(width, depth);

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x, y, z), x, z);

            return plane;
        }

        public float[][] getYPlane_float(int y)
        {
            float[][] plane = new float[width][depth];

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane[x][z] = this.get(x, y, z);

            return plane;
        }

        public FloatArray2D getZPlane(int z)
        {
            FloatArray2D plane = new FloatArray2D(width, height);

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane.set(this.get(x, y, z), x, y);

            return plane;
        }

        public float[][] getZPlane_float(int z)
        {
            float[][] plane = new float[width][height];

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane[x][y] = this.get(x, y, z);

            return plane;
        }

        public void setXPlane(FloatArray2D plane, int x)
        {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(y, z), x, y, z);
        }

        public void setXPlane(float[][] plane, int x)
        {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane[y][z], x, y, z);
        }

        public void setYPlane(FloatArray2D plane, int y)
        {
            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(x, z), x, y, z);
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
                    this.set(plane.get(x, y), x, y, z);
        }

        public void setZPlane(float[][] plane, int z)
        {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    this.set(plane[x][y], x, y, z);
        }

    }
}
