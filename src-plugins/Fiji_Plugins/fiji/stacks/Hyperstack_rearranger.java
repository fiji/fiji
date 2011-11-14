package fiji.stacks;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

public class Hyperstack_rearranger implements PlugIn
{
	public static int defaultIndexChannels = 0;
	public static int defaultIndexSlices = 1;
	public static int defaultIndexFrames = 2;
	public String[] choice = new String[] { "Channels (c)", "Slices (z)", "Frames (t)" };
	
	@Override
	public void run(String arg0) 
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		
		if ( imp == null )
		{			
			IJ.log( "No images open." );
			return;
		}
		
		final int nChannels = imp.getNChannels();
		final int nSlices = imp.getNSlices();
		final int nFrames = imp.getNFrames();
		
		if ( nChannels + nFrames + nSlices == 1 )
		{
			IJ.log( "This is only a 2d-image." );
			return;
		}
		
		final GenericDialog gd = new GenericDialog( "Re-order Hyperstack [" + imp.getTitle() + "]" );			
		
		gd.addChoice( "Channels (c) -> ", choice, choice[ defaultIndexChannels ] );
		gd.addChoice( "Slices (z) -> ", choice, choice[ defaultIndexSlices ] );
		gd.addChoice( "Frames (t) -> ", choice, choice[ defaultIndexFrames ] );
		gd.addMessage("");
		gd.addMessage("Current number of channels: " + nChannels );
		gd.addMessage("Current number of slices: " + nSlices );
		gd.addMessage("Current number of frames: " + nFrames );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final int indexChannels = gd.getNextChoiceIndex();
		final int indexSlices = gd.getNextChoiceIndex();
		final int indexFrames = gd.getNextChoiceIndex();
		
		// is it valid?
		final int[] verify = new int[ 3 ];
		verify[ indexChannels ]++;
		verify[ indexSlices ]++;
		verify[ indexFrames ]++;
		
		if ( verify[ 0 ] != 1 || verify[ 1 ] != 1 || verify[ 2 ] != 1 )
		{
			IJ.log( "Mapping is inconsistent: each - channel, slices and frames have to be assigned to an input dimension." );
			return;
		}
		
		defaultIndexChannels = indexChannels;
		defaultIndexSlices = indexSlices;
		defaultIndexFrames = indexFrames;
		
		reorderHyperstack( imp, indexChannels, indexSlices, indexFrames, true, true );
	}

	/**
	 * Creates a new Hyperstack with a different order of dimensions, the ImageProcessors are not copied
	 * but just linked from the input ImagePlus
	 * 
	 * @param imp - the input ImagePlus
	 * @param newOrder - the new order, can be "CZT", "CTZ", "ZCT", "ZTC", TCZ" or "TZC"
	 * @param closeOldImp - close the old one?
	 * @param showNewImp - show the new one?
	 * 
	 * @return - a new ImagePlus with different order of Processors linked from the old ImagePlus
	 */
	public static ImagePlus reorderHyperstack( final ImagePlus imp, final String newOrder, final boolean closeOldImp, final boolean showNewImp )
	{
		if ( newOrder.equalsIgnoreCase( "CZT" ) )
			return reorderHyperstack( imp, 0, 1, 2, closeOldImp, showNewImp );
		else if ( newOrder.equalsIgnoreCase( "CTZ" ) )
			return reorderHyperstack( imp, 0, 2, 1, closeOldImp, showNewImp );
		else if ( newOrder.equalsIgnoreCase( "ZCT" ) )
			return reorderHyperstack( imp, 1, 0, 2, closeOldImp, showNewImp );
		else if ( newOrder.equalsIgnoreCase( "ZTC" ) )
			return reorderHyperstack( imp, 1, 2, 0, closeOldImp, showNewImp );
		else if ( newOrder.equalsIgnoreCase( "TCZ" ) )
			return reorderHyperstack( imp, 2, 0, 1, closeOldImp, showNewImp );
		else if ( newOrder.equalsIgnoreCase( "TZC" ) )
			return reorderHyperstack( imp, 2, 1, 0, closeOldImp, showNewImp );
		else
		{
			IJ.log( "Unknown reordering: " + newOrder );
			return null;
		}
	}
	
	/**
	 * Creates a new Hyperstack with a different order of dimensions, the ImageProcessors are not copied
	 * but just linked from the input ImagePlus
	 * 
	 * id's used here, reflecting (XY)CZT:
	 * channel = 0
	 * slices = 1
	 * frames = 2
	 * 
	 * @param imp - the input ImagePlus
	 * @param targetChannels - the new id for channel (0, 1 or 2, i.e. channels stays channels or become slices or frames)
	 * @param targetSlices - the new id for slices (0, 1 or 2 ...)
	 * @param targetFrames - the new id for frames (0, 1 or 2 ...)
	 * @param closeOldImp - close the old one?
	 * @param showNewImp - show the new one?
	 * 
	 * @return - a new ImagePlus with different order of Processors linked from the old ImagePlus
	 */
	public static CompositeImage reorderHyperstack( final ImagePlus imp, final int targetChannels, final int targetSlices, final int targetFrames, final boolean closeOldImp, final boolean showNewImp )
	{
		// dimensions of the input imageplus in order CZT
		final int[] dimensions = new int[] { imp.getNChannels(), imp.getNSlices(), imp.getNFrames() };

		// the new dimension assignments; 0->(c,z or t) 1->(c,z or t) 2->(c,z or t)
		final int[] newAssignment = new int[] { targetChannels, targetSlices, targetFrames };
		
		// we need a new stack
		final ImageStack stack = new ImageStack( imp.getWidth(), imp.getHeight() );

		// XYCZT is the order that ImageJ wants
		// so we arrange it like that.
		// However, we adjust the numbers to the new dimensions
		final int nChannelsNew = dimensions[ newAssignment[ 0 ] ];
		final int nSlicesNew = dimensions[ newAssignment[ 1 ] ];
		final int nFramesNew = dimensions[ newAssignment[ 2 ] ];
		
		// used to translate old -> new
		final int[] indexTmp = new int[ 3 ];
		
		for ( int t = 1; t <= nFramesNew; ++t )
		{
			for ( int z = 1; z <= nSlicesNew; ++z )
			{
				for ( int c = 1; c <= nChannelsNew; ++c )
				{
					indexTmp[ newAssignment[ 0 ] ] = c; 
					indexTmp[ newAssignment[ 1 ] ] = z; 
					indexTmp[ newAssignment[ 2 ] ] = t; 
					
					//final int index = imp.getStackIndex( c, z, t );
					final int index = imp.getStackIndex( indexTmp[ 0 ], indexTmp[ 1 ], indexTmp[ 2 ] );
					final ImageProcessor ip = imp.getStack().getProcessor( index );
					stack.addSlice( imp.getStack().getSliceLabel( index ), ip );
				}
			}
		}
		
		final ImagePlus newImp = new ImagePlus( imp.getTitle(), stack );
		newImp.setDimensions( nChannelsNew, nSlicesNew, nFramesNew );
		newImp.setCalibration( imp.getCalibration() );
		
		final CompositeImage c = new CompositeImage( newImp, CompositeImage.COMPOSITE );
		
		if ( closeOldImp )
			imp.close();
		
		if ( showNewImp )
			c.show(); 
		
		return c;
	}

	/**
	 * Returns an {@link ImagePlus} for a 2d or 3d stack where ImageProcessors are not copied but just added.
	 * 
	 * @param imp - the input image
	 * @param channel - which channel (first channel is 1, NOT 0)
	 * @param timepoint - which timepoint (first timepoint is 1, NOT 0)
	 */
	public static ImagePlus getImageChunk( final ImagePlus imp, final int channel, final int timepoint )
	{
		if ( imp.getNSlices() == 1 )
		{
			return new ImagePlus( "", imp.getStack().getProcessor( imp.getStackIndex( channel, 1, timepoint ) ) );
		}
		else
		{
			final ImageStack stack = new ImageStack( imp.getWidth(), imp.getHeight() );
			
			for ( int z = 1; z <= imp.getNSlices(); ++z )
			{
				final int index = imp.getStackIndex( channel, z, timepoint );
				final ImageProcessor ip = imp.getStack().getProcessor( index );
				stack.addSlice( imp.getStack().getSliceLabel( index ), ip );
			}
			
			return new ImagePlus( "", stack );
		}
	}
	
	/**
	 * Converts this image to Hyperstack if it is RGB or 8-bit color
	 * 
	 * @param imp - Inputimage
	 * @return the output image, might be the same
	 */
	public static ImagePlus convertToHyperStack( ImagePlus imp )
	{
		// first 8-bit color to RGB, directly to Hyperstack is not supported
		if ( imp.getType() == ImagePlus.COLOR_256 )
		{
			if ( imp.getStackSize() > 1 )
				new StackConverter( imp ).convertToRGB();
			else
				new ImageConverter( imp ).convertToRGB();
		}
		
		final Calibration cal = imp.getCalibration();
		
		// now convert to hyperstack, this creates a new imageplus
		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			imp = new CompositeConverter2().makeComposite( imp );
			imp.setCalibration( cal );
		}
		
		return imp;
	}
}
