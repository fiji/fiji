package trainableSegmentation;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import imagescience.feature.Differentiator;
import imagescience.feature.Edges;
import imagescience.feature.Hessian;
import imagescience.feature.Laplacian;
import imagescience.feature.Structure;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;

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
	/** edge filter flag index */
	public static final int EDGES					=  5;
	/** difference of Gaussian filter flag index */
	public static final int DOG						=  6;
	/** Minimum flag index */
	public static final int MINIMUM					=  7;
	/** Maximum flag index */
	public static final int MAXIMUM					=  8;
	/** Mean flag index */
	public static final int MEAN					=  9;
	
	/** names of available filters */
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Hessian", "Derivatives", "Laplacian", 
						"Structure", "Edges", "Difference_of_Gaussian", "Minimum",
						"Maximum", "Mean"};
	
	/** flags of filters to be used */	
	private boolean[] enableFeatures = new boolean[]{
			true, 	/* Gaussian_blur */
			true, 	/* Hessian */
			true, 	/* Derivatives */
			true, 	/* Laplacian */
			true,	/* Structure */
			true,	/* Edges */
			true,	/* Difference of Gaussian */
			false,	/* Minimum */
			false,	/* Maximum */
			false,	/* Mean */
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
	 * @param sigma isotropic smoothing scale
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
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
					
					// remove pad				
					ip.getImageStack().deleteLastSlice();
					ip.getImageStack().deleteSlice(1);				
					
					results[ch].add( ip );		
				}
						
				return mergeResultChannels(results);				
			}
		};
	}	
	
	
	/**
	 * Get difference of Gaussian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma1 sigma of the smaller Gausian
	 * @param sigma2 sigma of the larger Gausian  
	 * @return filter image after specific order derivatives
	 */
	public Callable<ArrayList<ImagePlus>> getDoG(
			final ImagePlus originalImage,
			final double sigma1,
			final double sigma2)
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
					Aspects aspects = img.aspects();


					imagescience.image.Image newimg = new FloatImage(img);
					imagescience.image.Image newimg2 = new FloatImage(img);
					
					Differentiator diff = new Differentiator();

					diff.run(newimg, sigma1 , 0, 0, 0);
					diff.run(newimg2, sigma2 , 0, 0, 0);
					
					newimg.aspects(aspects);
					newimg2.aspects(aspects);

					final ImagePlus ip = newimg.imageplus();
					final ImagePlus ip2 = newimg2.imageplus();
					
					ImageCalculator ic = new ImageCalculator();
					final ImagePlus res = ic.run("Difference create stack", ip2, ip );
					
					
					res.setTitle( availableFeatures[ DOG ] +"_" + sigma1 + "_" + sigma2 );
										
					// remove pad				
					res.getImageStack().deleteLastSlice();
					res.getImageStack().deleteSlice(1);				
					
					results[ch].add( res );		
				}
						
				return mergeResultChannels(results);				
			}
		};
	}	
	

	/**
	 * Get Hessian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma isotropic smoothing scale	
	 * @return filter Hessian filter images
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Hessian hessian = new Hessian();

					final Vector<imagescience.image.Image> hessianImages = hessian.run(newimg, sigma, absolute);
					
					final int nrimgs = hessianImages.size();
					for (int i=0; i<nrimgs; ++i)
						hessianImages.get(i).aspects(aspects);

					final ImageStack smallest = hessianImages.get(0).imageplus().getImageStack();
					final ImageStack middle   = hessianImages.get(1).imageplus().getImageStack();
					final ImageStack largest  = hessianImages.get(2).imageplus().getImageStack();
					// remove pad
					smallest.deleteLastSlice();
					smallest.deleteSlice(1);
					middle.deleteLastSlice();
					middle.deleteSlice(1);
					largest.deleteLastSlice();
					largest.deleteSlice(1);
					
					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_largest_"  + sigma + "_" + absolute, smallest ) );					
					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_middle_"   + sigma + "_" + absolute, middle ) );					
					results[ ch ].add( new ImagePlus( availableFeatures[HESSIAN] +"_smallest_" + sigma + "_" + absolute, largest ) );									
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Get Laplacian features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma isotropic smoothing scale	
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Laplacian laplace = new Laplacian();

					newimg = laplace.run(newimg, sigma);
					newimg.aspects(aspects);						

					
					final ImagePlus ip = newimg.imageplus();
					ip.setTitle(availableFeatures[LAPLACIAN] +"_" + sigma );
					
					// remove pad				
					ip.getImageStack().deleteLastSlice();
					ip.getImageStack().deleteSlice(1);	
					
					results[ch].add( ip );
					
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Get Edges features (to be submitted in an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma isotropic isotropic smoothing scale	
	 * @return filter Edges filter image
	 */
	public Callable<ArrayList< ImagePlus >> getEdges(
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
				
					final Aspects aspects = img.aspects();				

					imagescience.image.Image newimg = new FloatImage( img );

					final Edges edges = new Edges();

					newimg = edges.run(newimg, sigma, false);
					newimg.aspects(aspects);						

					
					final ImagePlus ip = newimg.imageplus();
					ip.setTitle(availableFeatures[EDGES] +"_" + sigma );
					
					// remove pad				
					ip.getImageStack().deleteLastSlice();
					ip.getImageStack().deleteSlice(1);	
					
					results[ch].add( ip );
					
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	/**
	 * Get Minimum features (to be submitted to an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma filter radius	
	 * @return filter Minimum filter image
	 */
	public Callable<ArrayList< ImagePlus >> getMinimum(
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
					
					
					// wrap it into an ImgLib image (no copying)
					Img<FloatType> image = ImagePlusAdapter.wrap( channels [ ch ] );
					
					// create a new Image with the same properties
					Img<FloatType> output = image.factory().create( image, image.firstElement() );
					
					// get mirror view
					ExtendedRandomAccessibleInterval<FloatType, Img<FloatType>> infinite = Views.extendMirrorSingle( image ); 

					Cursor<FloatType> cursorInput = image.cursor();
					Cursor<FloatType> cursorOutput = output.cursor();

					FloatType min = image.firstElement().createVariable();
					
					// iterate over the input
					while ( cursorInput.hasNext())
					{
						cursorInput.fwd();
						cursorOutput.fwd();

						// define a hypersphere (n-dimensional sphere)
						HyperSphere<FloatType> hyperSphere = new HyperSphere<FloatType>( infinite, cursorInput, (long)sigma );

						// create a cursor on the hypersphere
						HyperSphereCursor<FloatType> cursor2 = hyperSphere.cursor();

						cursor2.fwd();
						min.set( cursor2.get() );

						while ( cursor2.hasNext() )
						{
							cursor2.fwd();
							if( cursor2.get().compareTo( min ) <= 0 )
								min.set( cursor2.get() );
						}

						// set the value of this pixel of the output image to the minimum value of the sphere
						cursorOutput.get().set( min );

					}
					
					
					final ImagePlus ip = ImageJFunctions.wrap( output, availableFeatures[ MINIMUM ] +"_" + sigma );										
					results[ch].add( ip );					
				}
											
				return mergeResultChannels(results);
			}
		};
	}
		
	
	/**
	 * Get Maximum features (to be submitted to an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma filter radius	
	 * @return filter Maximum filter image
	 */
	public Callable<ArrayList< ImagePlus >> getMaximum(
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
					
					
					// wrap it into an ImgLib image (no copying)
					Img<FloatType> image = ImagePlusAdapter.wrap( channels [ ch ] );
					
					// create a new Image with the same properties
					Img<FloatType> output = image.factory().create( image, image.firstElement() );
					
					// get mirror view
					ExtendedRandomAccessibleInterval<FloatType, Img<FloatType>> infinite = Views.extendMirrorSingle( image ); 

					Cursor<FloatType> cursorInput = image.cursor();
					Cursor<FloatType> cursorOutput = output.cursor();

					FloatType max = image.firstElement().createVariable();
					
					// iterate over the input
					while ( cursorInput.hasNext())
					{
						cursorInput.fwd();
						cursorOutput.fwd();

						// define a hypersphere (n-dimensional sphere)
						HyperSphere<FloatType> hyperSphere = new HyperSphere<FloatType>( infinite, cursorInput, (long)sigma );

						// create a cursor on the hypersphere
						HyperSphereCursor<FloatType> cursor2 = hyperSphere.cursor();

						cursor2.fwd();
						max.set( cursor2.get() );

						while ( cursor2.hasNext() )
						{
							cursor2.fwd();
							if( cursor2.get().compareTo( max ) >= 0 )
								max.set( cursor2.get() );
						}

						// set the value of this pixel of the output image to the maximum value of the sphere
						cursorOutput.get().set( max );

					}
					
					
					final ImagePlus ip = ImageJFunctions.wrap( output, availableFeatures[ MAXIMUM ] +"_" + sigma );										
					results[ch].add( ip );					
				}
											
				return mergeResultChannels(results);
			}
		};
	}
	
	
	/**
	 * Get Mean features (to be submitted to an ExecutorService)
	 *
	 * @param originalImage input image
	 * @param sigma filter radius	
	 * @return filter Mean filter image
	 */
	public Callable<ArrayList< ImagePlus >> getMean(
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
					
					
					// wrap it into an ImgLib image (no copying)
					Img<FloatType> image = ImagePlusAdapter.wrap( channels [ ch ] );
					
					// create a new Image with the same properties
					Img<FloatType> output = image.factory().create( image, image.firstElement() );
					
					// get mirror view
					ExtendedRandomAccessibleInterval<FloatType, Img<FloatType>> infinite = Views.extendMirrorSingle( image ); 

					Cursor<FloatType> cursorInput = image.cursor();
					Cursor<FloatType> cursorOutput = output.cursor();

					FloatType mean = image.firstElement().createVariable();
					
					// iterate over the input
					while ( cursorInput.hasNext())
					{
						cursorInput.fwd();
						cursorOutput.fwd();

						// define a hypersphere (n-dimensional sphere)
						HyperSphere<FloatType> hyperSphere = new HyperSphere<FloatType>( infinite, cursorInput, (long)sigma );

						// create a cursor on the hypersphere
						HyperSphereCursor<FloatType> cursor2 = hyperSphere.cursor();

						cursor2.fwd();
						mean.set( cursor2.get() );
						int n = 1;

						while ( cursor2.hasNext() )
						{
							cursor2.fwd();
							n++;
							
							mean.add( cursor2.get() );
						}

						mean.div( new FloatType( n ));
						
						// set the value of this pixel of the output image to the mean value of the sphere
						cursorOutput.get().set( mean );

					}
					
					
					final ImagePlus ip = ImageJFunctions.wrap( output, availableFeatures[ MEAN ] +"_" + sigma );										
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
	 * @param sigma isotropic smoothing scale	
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
					
					// pad image on the back and the front
					final ImagePlus channel = channels [ ch ].duplicate();
					channel.getImageStack().addSlice("pad-back", channels[ch].getImageStack().getProcessor( channels[ ch ].getImageStackSize()));
					channel.getImageStack().addSlice("pad-front", channels[ch].getImageStack().getProcessor( 1 ), 1);
					
					imagescience.image.Image img = imagescience.image.Image.wrap( channel );
				
					final Aspects aspects = img.aspects();				

					final Structure structure = new Structure();
					final Vector<imagescience.image.Image> eigenimages = structure.run(new FloatImage(img), sigma, integrationScale);

					final int nrimgs = eigenimages.size();
					for (int i=0; i<nrimgs; ++i)
						eigenimages.get(i).aspects(aspects);

					final ImageStack largest  = eigenimages.get(0).imageplus().getImageStack();					
					final ImageStack smallest = eigenimages.get(1).imageplus().getImageStack();
					
					// remove pad
					smallest.deleteLastSlice();
					smallest.deleteSlice(1);					
					largest.deleteLastSlice();
					largest.deleteSlice(1);					
					
					results[ ch ].add( new ImagePlus( availableFeatures[STRUCTURE] +"_largest_"  + sigma + "_"  + integrationScale, largest ) );
					results[ ch ].add( new ImagePlus( availableFeatures[STRUCTURE] +"_smallest_" + sigma + "_"  + integrationScale, smallest ) );
				
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
				
				isRed.addSlice( null, redBp.convertToFloat() );
				isGreen.addSlice( null, greenBp.convertToFloat() );
				isBlue.addSlice( null, blueBp.convertToFloat() );

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
		
		for(int i=0; i<channels.length; i++)
			channels[i].setCalibration(originalImage.getCalibration());
		
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
		
		final double pixelWidth = originalImage.getCalibration().pixelWidth;
		
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
				
				double scaledSigma = i * pixelWidth;
				
				if (Thread.currentThread().isInterrupted()) 
					return false;
				
				// Gaussian blur
				if(enableFeatures[GAUSSIAN])
				{
					//IJ.log( n++ +": Calculating Gaussian filter ("+ i + ")");
					futures.add(exe.submit( getDerivatives(originalImage, scaledSigma, 0, 0, 0)) );
				}
				
				// Difference of Gaussian
				if(enableFeatures[ DOG ])
				{
					for (float j=minimumSigma; j<i; j*=2)
					{
						//IJ.log( n++ +": Calculating DoG filter ("+ i + ", " + j + ")");
						futures.add(exe.submit( getDoG( originalImage, scaledSigma, j * pixelWidth) ) );
					}
				}
			
				// Hessian
				if(enableFeatures[HESSIAN])
				{
					//IJ.log("Calculating Hessian filter ("+ i + ")");
					futures.add(exe.submit( getHessian(originalImage, scaledSigma, true)) );
				}
							
				// Derivatives
				if(enableFeatures[DERIVATIVES])
				{					
					for(int order = minDerivativeOrder; order<=maxDerivativeOrder; order++)
						futures.add(exe.submit( getDerivatives(originalImage, scaledSigma, order, order, order)) );
				}
				
				// Laplacian
				if(enableFeatures[LAPLACIAN])
				{
					futures.add(exe.submit( getLaplacian(originalImage, scaledSigma)) );
				}
				
				// Edges
				if(enableFeatures[ EDGES ])
				{
					futures.add(exe.submit( getEdges(originalImage, scaledSigma)) );
				}
				
				// Structure tensor
				if(enableFeatures[ STRUCTURE ])
				{					
					for(int integrationScale = 1; integrationScale <= 3; integrationScale+=2)
						futures.add(exe.submit( getStructure(originalImage, scaledSigma, integrationScale )) );
				}
				
				// Maximum
				if(enableFeatures[ MINIMUM ])
				{
					futures.add(exe.submit( getMinimum(originalImage, i)) );
				}
				
				// Maxmimum
				if(enableFeatures[ MAXIMUM ])
				{
					futures.add(exe.submit( getMaximum(originalImage, i)) );
				}
				
				// Mean
				if(enableFeatures[ MEAN ])
				{
					futures.add(exe.submit( getMean(originalImage, i)) );
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
	
	
	public void setMinimumSigma( float minimumSigma )
	{
		this.minimumSigma = minimumSigma;
	}
	
	public void setMaximumSigma( float maximumSigma )
	{
		this.maximumSigma = maximumSigma;
	}
	
}
