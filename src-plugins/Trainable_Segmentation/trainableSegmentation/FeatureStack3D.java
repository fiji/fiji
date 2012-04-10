package trainableSegmentation;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import imagescience.feature.Differentiator;
import imagescience.feature.Hessian;
import imagescience.feature.Laplacian;
import imagescience.feature.Structure;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;

public class FeatureStack3D 
{
	/** original input image */
	private ImagePlus originalImage = null;
	/** list of feature images (created by filtering) */
	private ArrayList<ImagePlus> wholeStack = null;
	
	private boolean colorFeatures = false;
	
	/** image width */
	private int width = 0;
	/** image height */
	private int height = 0;
	
	/** minmum sigma/radius used in the filters */
	private float minimumSigma = 1;
	/** maximum sigma/radius used in the filters */
	private float maximumSigma = 16;
	
	/** Gaussian filter flag index */
	public static final int GAUSSIAN 				=  0;
	/** Hessian filter flag index */
	public static final int HESSIAN 				=  1;
	/** Derivatives filter flag index */
	public static final int DERIVATIVES				=  2;
	/** Laplacian filter flag index */
	public static final int LAPLACIAN				=  3;
	/** structure tensor filter flag index */
	public static final int STRUCTURE				=  4;
	
	/** names of available filters */
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Hessian", "Derivatives", "Laplacian", "Structure"};
	
	/** flags of filters to be used */	
	private boolean[] enableFeatures = new boolean[]{
			true, 	/* Gaussian_blur */
			true, 	/* Hessian */
			true, 	/* Derivatives */
			true, 	/* Laplacian */
			true,	/* Structure */
	};
	
	
	private int minDerivativeOrder = 1;
	private int maxDerivativeOrder = 5;
	
	/**
	 * Construct object to store stack of image features
	 * @param image original image
	 */
	public FeatureStack3D(ImagePlus image)
	{
		width = image.getWidth();
		height = image.getHeight();
		originalImage = image;
		
		wholeStack = new ArrayList<ImagePlus>();
		
		ImageStack is = new ImageStack ( width, height );
		
		if( image.getType() == ImagePlus.COLOR_RGB)
		{			
			colorFeatures = true;
			for(int i=1; i<=image.getImageStackSize(); i++)
				is.addSlice("original-slice-" + i, image.getImageStack().getProcessor(i) );
		}
		else
		{
			colorFeatures = false;
			for(int i=1; i<=image.getImageStackSize(); i++)
				is.addSlice("original-slice-" + i, image.getImageStack().getProcessor(i).convertToFloat() );
		}
		
		
		wholeStack.add( new ImagePlus("original", is ) );		
	}
	
	
	/**
	 * Get derivatives features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale
	 * @param xOrder x-order of differentiation
	 * @param yOrder y-order of differentiation
	 * @param zOrder z-order of differentiation
	 * @return filter image after specific order derivatives
	 */
	public Callable<ArrayList<ImagePlus>> getDerivatives(
			final ImagePlus originalImage,
			final double sigma,
			final int xOrder,
			final int yOrder,
			final int zOrder)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ArrayList<ImagePlus>>()
		{
			public ArrayList<ImagePlus> call()
			{
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ArrayList<ImagePlus>[] results = new ArrayList[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					results[ ch ] = new ArrayList<ImagePlus>();
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] );
					Aspects aspects = img.aspects();


					imagescience.image.Image newimg = new FloatImage(img);
					Differentiator diff = new Differentiator();

					diff.run(newimg, sigma , xOrder, yOrder, zOrder);
					newimg.aspects(aspects);

					final ImagePlus ip = newimg.imageplus();
					if( xOrder + yOrder + zOrder == 0)
						ip.setTitle( availableFeatures[GAUSSIAN] +"_" + sigma );
					else
						ip.setTitle( availableFeatures[DERIVATIVES] +"_" + xOrder + "_" +yOrder+"_"+zOrder+ "_"+sigma );
					
					results[ch].add( ip );		
				}
						
				return mergeResultChannels(results);				
			}
		};
	}	
	

	/**
	 * Get Hessian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale	
	 * @return filter Laplacian filter image
	 */
	public Callable< ArrayList<ImagePlus> >getHessian(
			final ImagePlus originalImage,
			final double sigma,
			final boolean absolute)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable <ArrayList <ImagePlus> >()
		{
			public ArrayList< ImagePlus >call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ArrayList<ImagePlus>[] results = new ArrayList[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					results[ ch ] = new ArrayList<ImagePlus>();
					
					final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Hessian hessian = new Hessian();

					final Vector<imagescience.image.Image> hessianImages = hessian.run(newimg, sigma, absolute);
					
					final int nrimgs = hessianImages.size();
					for (int i=0; i<nrimgs; ++i)
						hessianImages.get(i).aspects(aspects);

					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_largest_" + sigma + "_" +  absolute, hessianImages.get(0).imageplus().getImageStack() ) );
					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_middle_" + sigma + "_" +   absolute, hessianImages.get(1).imageplus().getImageStack() ) );
					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_smallest_" + sigma + "_" + absolute, hessianImages.get(2).imageplus().getImageStack() ) );
										
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Get Laplacian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale	
	 * @return filter Laplacian filter image
	 */
	public Callable<ArrayList< ImagePlus >> getLaplacian(
			final ImagePlus originalImage,
			final double sigma)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ArrayList< ImagePlus >>()
		{
			public ArrayList< ImagePlus > call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ArrayList<ImagePlus>[] results = new ArrayList[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					results[ ch ] = new ArrayList<ImagePlus>();
					
					final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Laplacian laplace = new Laplacian();

					newimg = laplace.run(newimg, sigma);
					newimg.aspects(aspects);						

					
					final ImagePlus ip = newimg.imageplus();
					ip.setTitle(availableFeatures[LAPLACIAN] +"_" + sigma );
					
					results[ch].add( ip );
					
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	
	/**
	 * Get structure tensor features (to be submitted in an ExecutorService).
	 * It computes, for all pixels in the input image, the eigenvalues of the so-called structure tensor.
	 *
	 * @param originalImage input image
	 * @param sigma smoothing scale	
	 * @param integrationScale integration scale (standard deviation of the Gaussian 
	 * 		kernel used for smoothing the elements of the structure tensor, must be larger than zero)
	 * @return filter structure tensor filter image
	 */
	public Callable<ArrayList< ImagePlus >> getStructure(
			final ImagePlus originalImage,
			final double sigma,
			final double integrationScale)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ArrayList< ImagePlus >>()
		{
			public ArrayList< ImagePlus > call()
			{
				
				// Get channel(s) to process
				ImagePlus[] channels = extractChannels(originalImage);
				
				ArrayList<ImagePlus>[] results = new ArrayList[ channels.length ];
				
				for(int ch=0; ch < channels.length; ch++)
				{
					results[ ch ] = new ArrayList<ImagePlus>();
					
					final imagescience.image.Image img = imagescience.image.Image.wrap( channels[ ch ] ) ;
				
					final Aspects aspects = img.aspects();				

					final Structure structure = new Structure();
					final Vector<imagescience.image.Image> eigenimages = structure.run(new FloatImage(img), sigma, integrationScale);

					final int nrimgs = eigenimages.size();
					for (int i=0; i<nrimgs; ++i)
						eigenimages.get(i).aspects(aspects);

					results[ ch ].add( new ImagePlus( availableFeatures[STRUCTURE] +"_largest_" + sigma + "_"  + integrationScale, eigenimages.get(0).imageplus().getImageStack() ) );
					results[ ch ].add( new ImagePlus( availableFeatures[STRUCTURE] +"_smallest_" + sigma + "_" + integrationScale, eigenimages.get(1).imageplus().getImageStack() ) );
				
				}
				
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Merge input channels if they are more than 1
	 * @param channels results channels
	 * @return result image 
	 */
	ArrayList< ImagePlus > mergeResultChannels(final ArrayList<ImagePlus>[] channels) 
	{
		if(channels.length > 1)
		{					
			ArrayList< ImagePlus > mergedList = new ArrayList<ImagePlus> ();
			
			for(int i=0; i<channels[0].size(); i++)
			{
			
				ImageStack mergedColorStack = mergeStacks(channels[0].get(i).getImageStack(), channels[1].get(i).getImageStack(), channels[2].get(i).getImageStack());

				ImagePlus merged = new ImagePlus(channels[0].get(i).getTitle(), mergedColorStack); 

				for(int n = 1; n <= merged.getImageStackSize(); n++)
					merged.getImageStack().setSliceLabel(channels[0].get(i).getImageStack().getSliceLabel(n), n);
				mergedList.add( merged );
			}
			
			return mergedList;
		}
		else
			return channels[0];
	}
	
	/**
	 * Merge three image stack into a color stack (no scaling)
	 * 
	 * @param redChannel image stack representing the red channel 
	 * @param greenChannel image stack representing the green channel
	 * @param blueChannel image stack representing the blue channel
	 * @return RGB merged stack
	 */
	ImageStack mergeStacks(ImageStack redChannel, ImageStack greenChannel, ImageStack blueChannel)
	{
		final ImageStack colorStack = new ImageStack( redChannel.getWidth(), redChannel.getHeight());
		
		for(int n=1; n<=redChannel.getSize(); n++)
		{
			final ByteProcessor red = (ByteProcessor) redChannel.getProcessor(n).convertToByte(false); 
			final ByteProcessor green = (ByteProcessor) greenChannel.getProcessor(n).convertToByte(false); 
			final ByteProcessor blue = (ByteProcessor) blueChannel.getProcessor(n).convertToByte(false); 
			
			final ColorProcessor cp = new ColorProcessor(redChannel.getWidth(), redChannel.getHeight());
			cp.setRGB((byte[]) red.getPixels(), (byte[]) green.getPixels(), (byte[]) blue.getPixels() );
			
			colorStack.addSlice(redChannel.getSliceLabel(n), cp);
		}
		
		return colorStack;
	}
	
	/**
	 * Extract channels from input image if it is RGB
	 * @param originalImage input image
	 * @return array of channels
	 */
	ImagePlus[] extractChannels(final ImagePlus originalImage) 
	{
		final int width = originalImage.getWidth();
		final int height = originalImage.getHeight();
		ImagePlus[] channels;
		if( originalImage.getType() == ImagePlus.COLOR_RGB )
		{
			final ImageStack isRed = new ImageStack ( width, height );
			final ImageStack isGreen = new ImageStack ( width, height );
			final ImageStack isBlue = new ImageStack ( width, height );
			
			for(int n = 1; n<= originalImage.getImageStackSize(); n++)
			{
				final ByteProcessor redBp = new ByteProcessor(width, height);
				final ByteProcessor greenBp = new ByteProcessor(width, height);
				final ByteProcessor blueBp = new ByteProcessor(width, height);
	
				final byte[] redPixels = (byte[]) redBp.getPixels();
				final byte[] greenPixels = (byte[]) greenBp.getPixels();
				final byte[] bluePixels = (byte[]) blueBp.getPixels();
	
				
				((ColorProcessor)(originalImage.getImageStack().getProcessor( n ).duplicate())).getRGB(redPixels, greenPixels, bluePixels);
				
				isRed.addSlice( redBp.convertToFloat() );
				isGreen.addSlice( greenBp.convertToFloat() );
				isBlue.addSlice( blueBp.convertToFloat() );

			}
			
			channels = new ImagePlus[]{new ImagePlus("red", isRed), 
					new ImagePlus("green", isGreen), 
					new ImagePlus("blue", isBlue )};
		}
		else
		{
			channels = new ImagePlus[1];
			final ImageStack is = new ImageStack ( width, height );
			for(int i=1; i<=originalImage.getImageStackSize(); i++)
				is.addSlice("original-slice-" + i, originalImage.getImageStack().getProcessor(i).convertToFloat() );
			channels[0] = new ImagePlus(originalImage.getTitle(), is );
		}
		return channels;
	}

	/**
	 * Update features with current list in a multi-thread fashion
	 * 
	 * @return true if the features are correctly updated 
	 */
	public boolean updateFeaturesMT()
	{
		if (Thread.currentThread().isInterrupted() )
			return false;
		
		ExecutorService exe = Executors.newFixedThreadPool( Prefs.getThreads() );
		
		wholeStack = new ArrayList<ImagePlus>();
		
		ImageStack is = new ImageStack ( width, height );
		
		if( colorFeatures )
		{			
			for(int i=1; i<=originalImage.getImageStackSize(); i++)
				is.addSlice("original-slice-" + i, originalImage.getImageStack().getProcessor(i) );
		}
		else
		{
			for(int i=1; i<=originalImage.getImageStackSize(); i++)
				is.addSlice("original-slice-" + i, originalImage.getImageStack().getProcessor(i).convertToFloat() );
		}
		
		
		wholeStack.add( new ImagePlus("original", is ) );
		
			
		// Count the number of enabled features
		int finalIndex = 0;
		for(int i=0; i<enableFeatures.length; i++)
			if(enableFeatures[i])
				finalIndex ++;

		final ArrayList< Future < ArrayList<ImagePlus> > > futures = new ArrayList< Future<ArrayList<ImagePlus>> >();
		//int n=0;
		
		int currentIndex = 0;
		IJ.showStatus("Updating features...");
		try{
			
			
		
			
			for (float i=minimumSigma; i<= maximumSigma; i *=2)
			{		
				if (Thread.currentThread().isInterrupted()) 
					return false;
				
				// Gaussian blur
				if(enableFeatures[GAUSSIAN])
				{
					//IJ.log( n++ +": Calculating Gaussian filter ("+ i + ")");
					futures.add(exe.submit( getDerivatives(originalImage, i, 0, 0, 0)) );
				}
			
				// Hessian
				if(enableFeatures[HESSIAN])
				{
					//IJ.log("Calculating Hessian filter ("+ i + ")");
					futures.add(exe.submit( getHessian(originalImage, i, true)) );
				}
							
				// Derivatives
				if(enableFeatures[DERIVATIVES])
				{					
					for(int order = minDerivativeOrder; order<=maxDerivativeOrder; order++)
						futures.add(exe.submit( getDerivatives(originalImage, i, order, order, order)) );
				}
				
				// Laplacian
				if(enableFeatures[LAPLACIAN])
				{
					futures.add(exe.submit( getLaplacian(originalImage, i)) );
				}
				
				// Structure tensor
				if(enableFeatures[ STRUCTURE ])
				{					
					for(int integrationScale = 1; integrationScale <= 3; integrationScale+=2)
						futures.add(exe.submit( getStructure(originalImage, i, integrationScale )) );
				}
				
			

			}
			
			// Wait for the jobs to be done
			for(Future<ArrayList<ImagePlus>> f : futures)
			{
				final ArrayList<ImagePlus> res = f.get();
				currentIndex ++;
				IJ.showStatus("Updating features...");
				IJ.showProgress(currentIndex, finalIndex);
				
				for( final ImagePlus ip : res)
					this.wholeStack.add( ip );				
			}
		
		}
		catch(InterruptedException ie)
		{
			IJ.log("The features udpate was interrupted by the user.");
			return false;
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating feature stack.");
			ex.printStackTrace();
			return false;
		}
		finally{
			exe.shutdownNow();
		}	
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
		return true;
	}
	
	/**
	 * Convert FeatureStack3D into a feature stack array (for 2D stacks). Experimental.
	 * @return
	 */
	public FeatureStackArray getFeatureStackArray()
	{
		// create empty feature stack array, with space for one stack per slice in the original image 
		FeatureStackArray fsa = new FeatureStackArray( originalImage.getImageStackSize(), minimumSigma, maximumSigma, false, 0, 0, null);
		
		// Initialize each feature stack (one per slice)
		for(int i=0; i<originalImage.getImageStackSize(); i++)
		{
			FeatureStack fs = new FeatureStack( width, height, colorFeatures );
			fsa.set(fs, i);
		}
		
		// now, read current 3D features and add them to the 2D feature stacks
		for( final ImagePlus ip : wholeStack)
		{
			//IJ.log(" Adding feature '"+ ip.getTitle() + "' from 3D stack to feature stack array... ");
			for(int n=1; n<=ip.getImageStackSize(); n++)
				fsa.get(n-1).getStack().addSlice(ip.getTitle(), ip.getImageStack().getProcessor(n));
			
		}
		
		return fsa;
	}
	
	
}
