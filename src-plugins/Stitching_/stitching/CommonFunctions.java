/**
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
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */
package stitching;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;

import edu.mines.jtk.dsp.FftComplex;
import edu.mines.jtk.dsp.FftReal;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.MultiLineLabel;
import ij.io.Opener;
import ij.plugin.BrowserLauncher;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ShortProcessor;

public class CommonFunctions
{
	public static String[] methodList = {"Average", "Linear Blending", "Max. Intensity", "Min. Intensity", "Red-Cyan Overlay"};	
	public final static int AVG = 0, LIN_BLEND = 1,  MAX = 2, MIN = 3, RED_CYAN = 4, NONE = 5;

	public static String[] methodListCollection = {"Average", "Linear Blending", "Max. Intensity", "Min. Intensity", "None"};	
	public static String[] rgbTypes = {"rgb", "rbg", "grb", "gbr", "brg", "bgr"}; 
	public static String[] colorList = { "Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue" };

	public static String[] fusionMethodList = { "Linear Blending", "Average", "Median", "Max. Intensity", "Min. Intensity", "Overlay into composite image", "Do not fuse images" };	
	public static String[] fusionMethodListSimple = { "Overlay into composite image", "Do not fuse images" };	
	public static String[] fusionMethodListGrid = { "Linear Blending", "Average", "Median", "Max. Intensity", "Min. Intensity", /* "Overlay into composite image", */ "Do not fuse images (only write TileConfiguration)" };	
	public static String[] timeSelect = { "Apply registration of first time-point to all other time-points", "Register images adjacently over time", "Register all images over all time-points globally (expensive!)" };
	public static String[] cpuMemSelect = { "Save memory (but be slower)", "Save computation time (but use more RAM)" };
	
	public static ImagePlus loadImage(String directory, String file, int seriesNumber) { return loadImage(directory, file, seriesNumber, "rgb"); }
	public static ImagePlus loadImage(String directory, String file, int seriesNumber, String rgb)
	{
		ImagePlus imp = null;
		
		String smallFile = file.toLowerCase();
		
		if (smallFile.endsWith("tif") || smallFile.endsWith("tiff") || smallFile.endsWith("jpg") || smallFile.endsWith("png") || smallFile.endsWith("bmp") || 
			smallFile.endsWith("gif") || smallFile.endsWith("jpeg"))
		{
			imp = new Opener().openImage((new File(directory, file)).getPath());
		}
		else
		{
			imp = openLOCIImagePlus(directory, file, seriesNumber, rgb);
			if (imp == null)
				imp = new Opener().openImage((new File(directory, file)).getPath());
		}

		
		return imp;
	}

	public static final void addHyperLinkListener(final MultiLineLabel text, final String myURL)
	{
		if ( text != null && myURL != null )
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
	}

	public static ImagePlus openLOCIImagePlus(String path, String fileName, int seriesNumber, String rgb) 
	{
		return openLOCIImagePlus(path, fileName, seriesNumber, rgb, -1, -1);
	}

	public static ImagePlus openLOCIImagePlus(String path, String fileName, int seriesNumber) 
	{
		return openLOCIImagePlus(path, fileName, seriesNumber, "rgb", -1, -1);
	}

	public static ImagePlus openLOCIImagePlus(String path, String fileName, int seriesNumber, String rgb, int from, int to) 
	{
		if (path.length() > 1) 
		{
			path = path.replace('\\', '/');
			if (!path.endsWith("/"))
				path = path + "/";
		}
		
		// parse howto assign channels
		rgb = rgb.toLowerCase().trim();
		final int colorAssign[][] = new int[rgb.length()][];
		final int colorWeight[] = new int[3];
		
		for (int i = 0; i < colorAssign.length; i++)
		{
			if (rgb.charAt(i) == 'r')
			{
				colorAssign[i] = new int[1]; 
				colorAssign[i][0] = 0;
				colorWeight[0]++;
			}
			else if (rgb.charAt(i) == 'b')
			{
				colorAssign[i] = new int[1]; 
				colorAssign[i][0] = 2;
				colorWeight[2]++;
			}
			else if (rgb.charAt(i) == 'g')
			{
				colorAssign[i] = new int[1]; 
				colorAssign[i][0] = 1;
				colorWeight[1]++;
			}
			else //leave out
			{
				colorAssign[i] = new int[0]; 
			}
		}
		
		for (int i = 0; i < colorWeight.length; i++)
			if (colorWeight[i] == 0)
				colorWeight[i] = 1;
		
		ImagePlus imp = null;
		
		final String id = path + fileName;
		final IFormatReader r = new ChannelSeparator();
		
		try 
		{
			r.setId(id);

			// if loaded from a multiple series file (like LSM 710) select the correct series
			if ( seriesNumber >= 0 )
				r.setSeries( seriesNumber );

			//final int num = r.getImageCount();
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			final int timepoints = r.getSizeT();
			final int channels;
			//final String formatType = r.getFormat();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			//final String dimensionOrder = r.getDimensionOrder();
			
			if (timepoints > 1)
				IJ.log("More than one timepoint. Not implemented yet. Returning first timepoint");
			
			if (r.getSizeC() > 3)
			{
				IJ.log("More than three channels. ImageJ supports only 3 channels, returning the first three channels.");
				channels = 3;
			}
			else
			{
				channels = r.getSizeC();
			}				
			
			if (!(pixelType == FormatTools.UINT8 || pixelType == FormatTools.UINT16))
			{
				IJ.log("PixelType " + pixelTypeString + " not supported yet, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}
			
			/*System.out.println("width: " + width);
			System.out.println("height: " + height);
			System.out.println("depth: " + depth);
			System.out.println("timepoints: " + timepoints);
			System.out.println("channels: " + channels);
			System.out.println("images: " + num);
			System.out.println("image format: " + formatType);
			System.out.println("bytes per pixel: " + bytesPerPixel);
			System.out.println("pixel type: " + pixelTypeString);			
			System.out.println("dimensionOrder: " + dimensionOrder);*/

			final ImageStack stack = new ImageStack(width, height);	
			final int t = 0;			
			
			for (int z = start; z < end; z++)
			{				
				byte[][] b = new byte[channels][width * height * bytesPerPixel];
				
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);					
					//System.out.println(index);
				}
				
				if (channels == 1)
				{
					if (pixelType == FormatTools.UINT8)
					{
						final ByteProcessor bp = new ByteProcessor(width, height, b[0], null);
						stack.addSlice("" + (z + 1), bp);
					}	
					else if (pixelType == FormatTools.UINT16)
					{
						final short[] data = new short[width * height];
						
						for (int i = 0; i < data.length; i++)
							data[i] = getShortValue(b[0], i * 2);
													
						final ShortProcessor sp = new ShortProcessor(width, height, data, null);
						
						stack.addSlice("" + (z + 1), sp);						
					}						
				}
				else
				{
					final ColorProcessor cp = new ColorProcessor(width, height);
					final int color[] = new int[3];
					                            
					if (pixelType == FormatTools.UINT8)
						for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
							{
								color[0] = color[1] = color[2] = 0;
								
								for (int c = 0; c < channels; c++)
									for (int e = 0; e < colorAssign[c].length; e++)
									color[colorAssign[c][e]] += b[c][x + y*width] & 0xff;
								
								color[0] /= colorWeight[0]; 
								color[1] /= colorWeight[1]; 
								color[2] /= colorWeight[2]; 
									
								cp.putPixel(x, y, color);
							}
					else if (pixelType == FormatTools.UINT16)
						for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
							{
								color[0] = color[1] = color[2] = 0;
								
								for (int c = 0; c < channels; c++)
									color[c] = (byte)((int)getShortValue(b[c], 2 * (x + y*width))/256);
								
								cp.putPixel(x, y, color);
							}
					stack.addSlice("" + (z + 1), cp);						
				}
			}
			
			imp = new ImagePlus(fileName, stack);
		}
		catch (IOException exc) { IJ.log("IOException: " + exc.getMessage()); return null;}
		catch (FormatException exc) { IJ.log("FormatException: " + exc.getMessage()); return null;}
	                
		return imp;
	}

	private static final short getShortValue(final byte[] b, final int i)
	{
		return (short)getShortValueInt(b, i);
	}

	private static final int getShortValueInt(final byte[] b, final int i)
	{
		return ((((b[i] & 0xff) << 8)) + (b[i+1] & 0xff));
	}
	
	public static float getPixelValueRGB( final int rgb, final int rgbType )
	{
		final int r = (rgb & 0xff0000) >> 16;
		final int g = (rgb & 0xff00) >> 8;
		final int b = rgb & 0xff;

		// colorList = {"Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue"};

		if (rgbType == 0) return r;
		else if (rgbType == 1) return g;
		else if (rgbType == 2) return b;
		else if (rgbType == 3) return (r + g) / 2.0f;
		else if (rgbType == 4) return (r + b) / 2.0f;
		else if (rgbType == 5) return (g + b) / 2.0f;
		else return (r + g + b) / 3.0f;
	}

	public static FloatArray3D zeroPad(final FloatArray3D ip, final int width, final int height, final int depth)
	{
		final FloatArray3D image = new FloatArray3D(width, height, depth);

		final int offsetX = (width - ip.width) / 2;
		final int offsetY = (height - ip.height) / 2;
		final int offsetZ = (depth - ip.depth) / 2;

		if (offsetX < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		if (offsetZ < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int z = 0; z < ip.depth; z++)
			for (int y = 0; y < ip.height; y++)
				for (int x = 0; x < ip.width; x++)
					image.set(ip.get(x, y, z), x + offsetX, y + offsetY, z + offsetZ);

		return image;
	}

	public static FloatArray3D[] zeroPadImages(final FloatArray3D img1, final FloatArray3D img2)
	{
		final int width = Math.max(img1.width, img2.width);
		final int height = Math.max(img1.height, img2.height);
		final int depth = Math.max(img1.depth, img2.depth);

		final int widthFFT = FftReal.nfftFast(width);
		final int heightFFT = FftComplex.nfftFast(height);
		final int depthFFT = FftComplex.nfftFast(depth);

		final FloatArray3D[] result = new FloatArray3D[2];

		result[0] = zeroPad(img1, widthFFT, heightFFT, depthFFT);
		img1.data = null;

		result[1] = zeroPad(img2, widthFFT, heightFFT, depthFFT);
		img2.data = null;

		return result;
	}
	
	public static FloatArray2D zeroPad(FloatArray2D ip, int width, int height)
	{
		FloatArray2D image = new FloatArray2D(width, height);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;

		if (offsetX < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		for (int y = 0; y < ip.height; y++)
			for (int x = 0; x < ip.width; x++)
				image.set(ip.get(x, y), x + offsetX, y + offsetY);

		return image;
	}

	public static FloatArray2D[] zeroPadImages(FloatArray2D img1, FloatArray2D img2)
	{
		int width = Math.max(img1.width, img2.width);
		int height = Math.max(img1.height, img2.height);

		int widthFFT = FftReal.nfftFast(width);
		int heightFFT = FftComplex.nfftFast(height);

		FloatArray2D[] result = new FloatArray2D[2];

		result[0] = zeroPad(img1, widthFFT, heightFFT);
		img1.data = null;
		img1 = null;

		result[1] = zeroPad(img2, widthFFT, heightFFT);
		img2.data = null;
		img2 = null;

		return result;
	}
	
	public static void quicksort(final Quicksortable[] data, final int left, final int right)
	{
		if (data == null || data.length < 2) return;
		int i = left, j = right;

		double x = data[(left + right) / 2].getQuicksortValue();

		do
		{
			while (data[i].getQuicksortValue() < x)
				i++;
			while (x < data[j].getQuicksortValue())
				j--;
			if (i <= j)
			{
				Quicksortable temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				i++;
				j--;
			}
		} while (i <= j);
		if (left < j) quicksort(data, left, j);
		if (i < right) quicksort(data, i, right);
	}

	public static FloatArray2D pffft2D(FloatArray2D values, boolean scale)
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

	public static FloatArray2D pffftInv2D(FloatArray2D values, int nfft)
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

			fft.scale(width, tempOut);

			for (int x = 0; x < width; x++)
				result.set(tempOut[x], x, y);
		}

		return result;
	}
	
	public static FloatArray3D pffft3DMT(final FloatArray3D values, final boolean scale)
	{
		final int height = values.height;
		final int width = values.width;
		final int depth = values.depth;
		final int complexWidth = (width / 2 + 1) * 2;

		final FloatArray3D result = new FloatArray3D(complexWidth, height, depth);

		// do fft's in x direction
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
						if (z % numThreads == myNumber) for (int y = 0; y < height; y++)
						{
							tempOut = new float[complexWidth];

							for (int x = 0; x < width; x++)
								tempIn[x] = values.get(x, y, z);

							fft.realToComplex(-1, tempIn, tempOut);

							if (scale) fft.scale(width, tempOut);

							for (int x = 0; x < complexWidth; x++)
								result.set(tempOut[x], x, y, z);
						}
				}
			});
		startAndJoin(threads);

		// do fft's in y direction
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
						if (z % numThreads == myNumber) for (int x = 0; x < complexWidth / 2; x++)
						{
							tempOut = new float[height * 2];

							for (int y = 0; y < height; y++)
							{
								tempIn[y * 2] = result.get(x * 2, y, z);
								tempIn[y * 2 + 1] = result.get(x * 2 + 1, y, z);
							}

							fftc.complexToComplex(-1, tempIn, tempOut);

							for (int y = 0; y < height; y++)
							{
								result.set(tempOut[y * 2], x * 2, y, z);
								result.set(tempOut[y * 2 + 1], x * 2 + 1, y, z);
							}
						}
				}
			});

		startAndJoin(threads);

		// do fft's in z direction
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
						if (y % numThreads == myNumber) for (int x = 0; x < complexWidth / 2; x++)
						{
							tempOut = new float[depth * 2];

							for (int z = 0; z < depth; z++)
							{
								tempIn[z * 2] = result.get(x * 2, y, z);
								tempIn[z * 2 + 1] = result.get(x * 2 + 1, y, z);
							}

							fftc.complexToComplex(-1, tempIn, tempOut);

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

	public static FloatArray2D computePhaseCorrelationMatrix(FloatArray2D fft1, FloatArray2D fft2, int width)
	{
		//
		// Do Phase Correlation
		//

		FloatArray2D pcm = new FloatArray2D(computePhaseCorrelationMatrix(fft1.data, fft2.data, false), fft1.width, fft1.height);

		fft1.data = fft2.data = null;
		fft1 = fft2 = null;

		FloatArray2D ipcm = pffftInv2D(pcm, width);

		pcm.data = null;
		pcm = null;

		return ipcm;
	}

	public static FloatArray2D computeFFT(FloatArray2D img)
	{
		FloatArray2D fft = pffft2D(img, false);
		//img.data = null; img = null;

		return fft;
	}

	public static FloatArray3D computePhaseCorrelationMatrix(FloatArray3D fft1, FloatArray3D fft2, int width)
	{
		//
		// Do Phase Correlation
		//

		FloatArray3D pcm = new FloatArray3D(computePhaseCorrelationMatrix(fft1.data, fft2.data, false), fft1.width, fft1.height, fft1.depth);

		fft1.data = fft2.data = null;
		fft1 = fft2 = null;

		FloatArray3D ipcm = pffftInv3DMT(pcm, width);

		pcm.data = null;
		pcm = null;

		return ipcm;
	}

	public static FloatArray3D computeFFT(FloatArray3D img)
	{
		FloatArray3D fft = pffft3DMT(img, false);
		// img.data = null; img = null;

		return fft;
	}

	
	public static float[] computePhaseCorrelationMatrix(final float[] fft1, final float[] fft2, boolean inPlace)
	{
		normalizeComplexVectorsToUnitVectors(fft1);
		normalizeComplexVectorsToUnitVectors(fft2);

		float[] fftTemp1 = fft1;
		float[] fftTemp2 = fft2;

		// do complex conjugate
		if (inPlace) complexConjugate(fft2);
		else complexConjugate(fftTemp2);

		// multiply both complex arrays elementwise
		if (inPlace) multiply(fft1, fft2, true);
		else fftTemp1 = multiply(fftTemp1, fftTemp2, false);

		if (inPlace) return null;
		else return fftTemp1;
	}

	public static void normalizeComplexVectorsToUnitVectors(float[] complex)
	{
		int wComplex = complex.length / 2;

		double length;

		for (int pos = 0; pos < wComplex; pos++)
		{
			length = Math.sqrt(Math.pow(complex[pos * 2], 2) + Math.pow(complex[pos * 2 + 1], 2));

			if (length > 1E-5)
			{
				complex[pos * 2 + 1] /= length;
				complex[pos * 2] /= length;
			}
			else
			{
				complex[pos * 2 + 1] = complex[pos * 2] = 0;
			}
		}

	}

	public static void complexConjugate(float[] complex)
	{
		int wComplex = complex.length / 2;

		for (int pos = 0; pos < wComplex; pos++)
			complex[pos * 2 + 1] = -complex[pos * 2 + 1];
	}

	public static float multiplyComplexReal(float a, float b, float c, float d)
	{
		return a * c - b * d;
	}

	public static float multiplyComplexImg(float a, float b, float c, float d)
	{
		return a * d + b * c;
	}

	public static float[] multiply(float[] complexA, float[] complexB, boolean overwriteA)
	{
		if (complexA.length != complexB.length) return null;

		float[] complexResult = null;

		if (!overwriteA) complexResult = new float[complexA.length];

		// this is the amount of complex numbers
		// the actual array size is twice as high
		int wComplex = complexA.length / 2;

		// we compute: (a + bi) * (c + di)
		float a, b, c, d;

		if (!overwriteA) for (int pos = 0; pos < wComplex; pos++)
		{
			a = complexA[pos * 2];
			b = complexA[pos * 2 + 1];
			c = complexB[pos * 2];
			d = complexB[pos * 2 + 1];

			// compute new real part
			complexResult[pos * 2] = multiplyComplexReal(a, b, c, d);

			// compute new imaginary part
			complexResult[pos * 2 + 1] = multiplyComplexImg(a, b, c, d);
		}
		else for (int pos = 0; pos < wComplex; pos++)
		{
			a = complexA[pos * 2];
			b = complexA[pos * 2 + 1];
			c = complexB[pos * 2];
			d = complexB[pos * 2 + 1];

			// compute new real part
			complexA[pos * 2] = multiplyComplexReal(a, b, c, d);

			// compute new imaginary part
			complexA[pos * 2 + 1] = multiplyComplexImg(a, b, c, d);
		}

		if (overwriteA) return complexA;
		else return complexResult;
	}

	public static FloatArray3D pffftInv3DMT(final FloatArray3D values, final int nfft)
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
						if (y % numThreads == myNumber) for (int x = 0; x < complexWidth / 2; x++)
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
						if (z % numThreads == myNumber) for (int x = 0; x < complexWidth / 2; x++)
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

		// do inverse fft's in x direction
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
						if (z % numThreads == myNumber) for (int y = 0; y < height; y++)
						{
							tempOut = new float[width];

							for (int x = 0; x < complexWidth; x++)
								tempIn[x] = values.get(x, y, z);

							fft.complexToReal(1, tempIn, tempOut);

							for (int i = 0; i < tempOut.length; i++)
								tempOut[i] /= (float) (width * height * depth);

							// fft.scale(width, tempOut);

							for (int x = 0; x < width; x++)
								result.set(tempOut[x], x, y, z);
						}
				}
			});

		startAndJoin(threads);

		return result;
	}
	
	public static void startTask(Runnable run, int numThreads)
	{
		Thread[] threads = newThreads(numThreads);

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(run);

		startAndJoin(threads);
	}

	public static Thread[] newThreads()
	{
		int nthread = Runtime.getRuntime().availableProcessors();
		return new Thread[nthread];
	}

	public static Thread[] newThreads(int numThreads)
	{
		return new Thread[numThreads];
	}

	public static void startAndJoin(Thread[] threads)
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
		}
		catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}
	
	final public static int[] getPixelMinRGB(final Object[] imageStack, final int width, final int height, final int depth, final int x, final int y, final int z, final double min)
	{
		final int[] rgb = new int[3];

		if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth)
		{
			rgb[0] = rgb[1] = rgb[2] = (int) min;
			return rgb;
		}

		int[] pixelTmp = (int[]) imageStack[z];
		int color = (pixelTmp[x + y * width] & 0xffffff);

		rgb[0] = (color & 0xff0000) >> 16;
		rgb[1] = (color & 0xff00) >> 8;
		rgb[2] = color & 0xff;

		return rgb;
	}
	
	final public static float getPixelMin(final int imageType, final Object[] imageStack, final int width, final int height, final int depth, final int x, final int y, final int z, final double min)
	{
		if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return (float) min;

		if (imageType == ImagePlus.GRAY8)
		{
			byte[] pixelTmp = (byte[]) imageStack[z];
			return (float) (pixelTmp[x + y * width] & 0xff);
		}
		else if (imageType == ImagePlus.GRAY16)
		{
			short[] pixelTmp = (short[]) imageStack[z];
			return (float) (pixelTmp[x + y * width] & 0xffff);
		}
		else
		// instance of float[]
		{
			float[] pixelTmp = (float[]) imageStack[z];
			return pixelTmp[x + y * width];
		}
	}

}
