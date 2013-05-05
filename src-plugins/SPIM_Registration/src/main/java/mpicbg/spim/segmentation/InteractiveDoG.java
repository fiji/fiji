package mpicbg.spim.segmentation;

import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal;
import mpicbg.imglib.algorithm.math.LocalizablePoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.array.ArrayCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.detection.DetectionSegmentation;

/**
 * An interactive tool for determining the required sigma and peak threshold
 * 
 * @author Stephan Preibisch
 */
public class InteractiveDoG implements PlugIn
{
	final int extraSize = 40;
	final int scrollbarSize = 1000;
		
	float sigma = 0.5f;
	float sigma2 = 0.5f;
	float threshold = 0.0001f;
	
	// steps per octave
	public static int standardSenstivity = 4;
	int sensitivity = standardSenstivity;
	
	float imageSigma = 0.5f;
	float sigmaMin = 0.5f;
	float sigmaMax = 10f;
	int sigmaInit = 300;

	float thresholdMin = 0.0001f;
	float thresholdMax = 1f;
	int thresholdInit = 500;
	
	SliceObserver sliceObserver;
	RoiListener roiListener;
	ImagePlus imp;
	int channel = 0;
	Rectangle rectangle;
	Image<FloatType> img;
	Image<FloatType> source;
	ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks;
	
	Color originalColor = new Color( 0.8f, 0.8f, 0.8f );
	Color inactiveColor = new Color( 0.95f, 0.95f, 0.95f );
	public Rectangle standardRectangle;
	boolean isComputing = false;
	boolean isStarted = false;
	boolean enableSigma2 = false;
	boolean sigma2IsAdjustable = true;
	
	boolean lookForMinima = false;
	boolean lookForMaxima = true;
	
	public static enum ValueChange { SIGMA, THRESHOLD, SLICE, ROI, MINMAX, ALL }
	
	boolean isFinished = false;
	public boolean isFinished() { return isFinished; }
	public double getInitialSigma() { return sigma; }
	public void setInitialSigma( final float value ) 
	{ 
		sigma = value; 
		sigmaInit = computeScrollbarPositionFromValue( sigma, sigmaMin, sigmaMax, scrollbarSize );
	}
	public double getSigma2() { return sigma2; }
	public double getThreshold() { return threshold; }
	public void setThreshold( final float value ) 
	{ 
		threshold = value;
		final double log1001 = Math.log10( scrollbarSize + 1);
		thresholdInit = (int)Math.round( 1001-Math.pow(10, -(((threshold - thresholdMin)/(thresholdMax-thresholdMin))*log1001) + log1001 ) );
	}
	public boolean getSigma2WasAdjusted() { return enableSigma2; }
	public boolean getLookForMaxima() { return lookForMaxima; }
	public boolean getLookForMinima() { return lookForMinima; }
	public void setLookForMaxima( final boolean lookForMaxima ) { this.lookForMaxima = lookForMaxima; }
	public void setLookForMinima( final boolean lookForMinima ) { this.lookForMinima = lookForMinima; }
	
	public void setSigmaMax( final float sigmaMax ) { this.sigmaMax = sigmaMax; }
	public void setSigma2isAdjustable( final boolean state ) { sigma2IsAdjustable = state; }
	
	// for the case that it is needed again, we can save one conversion
	public Image<FloatType> getConvertedImage() { return source; }
	
	public InteractiveDoG( final ImagePlus imp, final int channel ) 
	{ 
		this.imp = imp;
		this.channel = channel;
	}
	public InteractiveDoG( final ImagePlus imp ) { this.imp = imp; }
	public InteractiveDoG() {}
	
	@Override
	public void run( String arg )
	{
		if ( imp == null )
			imp = WindowManager.getCurrentImage();
		
		standardRectangle = new Rectangle( imp.getWidth()/4, imp.getHeight()/4, imp.getWidth()/2, imp.getHeight()/2 );
		
		if ( imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 )
		{			
			IJ.log( "Color images are not supported, please convert to 8, 16 or 32-bit grayscale" );
			return;
		}
		
		Roi roi = imp.getRoi();
		
		if ( roi == null )
		{
			//IJ.log( "A rectangular ROI is required to define the area..." );
			imp.setRoi( standardRectangle );
			roi = imp.getRoi();
		}
		
		if ( roi.getType() != Roi.RECTANGLE )
		{
			IJ.log( "Only rectangular rois are supported..." );
			return;
		}
		
		// copy the ImagePlus into an ArrayImage<FloatType> for faster access
		source = convertToFloat( imp, channel, 0 );
		
		// show the interactive kit
		displaySliders();

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver( imp, new ImagePlusListener() );

		// compute first version
		updatePreview( ValueChange.ALL );		
		isStarted = true;
		
		// check whenever roi is modified to update accordingly
		roiListener = new RoiListener();
		imp.getCanvas().addMouseListener( roiListener );
	}
	
	/**
	 * Updates the Preview with the current parameters (sigma, threshold, roi, slicenumber)
	 * 
	 * @param change - what did change
	 */
	protected void updatePreview( final ValueChange change )
	{		
		// check if Roi changed
		boolean roiChanged = false;
		Roi roi = imp.getRoi();
		
		if ( roi == null || roi.getType() != Roi.RECTANGLE )
		{
			imp.setRoi( new Rectangle( standardRectangle ) );
			roi = imp.getRoi();
			roiChanged = true;
		}
			
		final Rectangle rect = roi.getBounds();
				
		if ( roiChanged || img == null || change == ValueChange.SLICE || 
			 rect.getMinX() != rectangle.getMinX() || rect.getMaxX() != rectangle.getMaxX() ||
			 rect.getMinY() != rectangle.getMinY() || rect.getMaxY() != rectangle.getMaxY() )
		{
			rectangle = rect;			
			img = extractImage( source, rectangle, extraSize );
			roiChanged = true;
		}
		
		// if we got some mouse click but the ROI did not change we can return
		if ( !roiChanged && change == ValueChange.ROI )
		{
			isComputing = false;
			return;
		}
		
		// compute the Difference Of Gaussian if necessary
		if ( peaks == null || roiChanged || change == ValueChange.SIGMA || change == ValueChange.SLICE || change == ValueChange.ALL )
		{
	        //
	        // Compute the Sigmas for the gaussian folding
	        //
			
			final float k, K_MIN1_INV;
			final float[] sigma, sigmaDiff;
			
			if ( enableSigma2 )
			{				
				sigma = new float[ 2 ];
				sigma[ 0 ] = this.sigma;
				sigma[ 1 ] = this.sigma2;
				k = sigma[ 1 ] / sigma[ 0 ];
				K_MIN1_INV = DetectionSegmentation.computeKWeight( k );
				sigmaDiff = DetectionSegmentation.computeSigmaDiff( sigma, imageSigma );
			}
			else
			{
		        k = (float)DetectionSegmentation.computeK( sensitivity );
		        K_MIN1_INV = DetectionSegmentation.computeKWeight( k );
		        sigma = DetectionSegmentation.computeSigma( k, this.sigma );
		        sigmaDiff = DetectionSegmentation.computeSigmaDiff( sigma, imageSigma );
			}
			
	        // the upper boundary
	        this.sigma2 = sigma[ 1 ];
	        
			final DifferenceOfGaussianReal1<FloatType> dog = new DifferenceOfGaussianReal1<FloatType>( img, new OutOfBoundsStrategyValueFactory<FloatType>(), sigmaDiff[ 0 ], sigmaDiff[ 1 ], thresholdMin/4, K_MIN1_INV );
			dog.setKeepDoGImage( true );
			dog.process();
			
			final SubpixelLocalization<FloatType> subpixel = new SubpixelLocalization<FloatType>( dog.getDoGImage(), dog.getPeaks() );
			subpixel.process();
			
			peaks = dog.getPeaks();
		}
		
		// extract peaks to show
		Overlay o = imp.getOverlay();
		
		if ( o == null )
		{
			o = new Overlay();
			imp.setOverlay( o );
		}
		
		o.clear();
		
		for ( final DifferenceOfGaussianPeak<FloatType> peak : peaks )
		{
			if ( ( peak.isMax() && lookForMaxima ) || ( peak.isMin() && lookForMinima ) )
			{
				final float x = peak.getPosition( 0 ); 
				final float y = peak.getPosition( 1 );
				
				if ( Math.abs( peak.getValue().get() ) > threshold &&
					 x >= extraSize/2 && y >= extraSize/2 &&
					 x < rect.width+extraSize/2 && y < rect.height+extraSize/2 )
				{
					final OvalRoi or = new OvalRoi( Util.round( x - sigma ) + rect.x - extraSize/2, Util.round( y - sigma ) + rect.y - extraSize/2, Util.round( sigma+sigma2 ), Util.round( sigma+sigma2 ) );
					
					if ( peak.isMax() )
						or.setStrokeColor( Color.green );
					else if ( peak.isMin() )
						or.setStrokeColor( Color.red );
					
					o.add( or );
				}
			}
		}
		
		imp.updateAndDraw();
		
		isComputing = false;
	}
	
	public static float computeSigma2( final float sigma1, final int sensitivity )
	{
        final float k = (float)DetectionSegmentation.computeK( sensitivity );
        final float[] sigma = DetectionSegmentation.computeSigma( k, sigma1 );
        
        return sigma[ 1 ];
	}
	
	/**
	 * Extract the current 2d region of interest from the souce image
	 * 
	 * @param source - the source image, a {@link Image} which is a copy of the {@link ImagePlus}
	 * @param rectangle - the area of interest
	 * @param extraSize - the extra size around so that detections at the border of the roi are not messed up
	 * @return
	 */
	protected Image<FloatType> extractImage( final Image<FloatType> source, final Rectangle rectangle, final int extraSize )
	{
		final Image<FloatType> img = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ rectangle.width+extraSize, rectangle.height+extraSize } );
		
		final int offsetX = rectangle.x - extraSize/2;
		final int offsetY = rectangle.y - extraSize/2;

		final int[] location = new int[ source.getNumDimensions() ];
		
		if ( location.length > 2 )
			location[ 2 ] = (imp.getCurrentSlice()-1)/imp.getNChannels();
				
		final LocalizableCursor<FloatType> cursor = img.createLocalizableCursor();
		final LocalizableByDimCursor<FloatType> positionable;
		
		if ( offsetX >= 0 && offsetY >= 0 && 
			 offsetX + img.getDimension( 0 ) < source.getDimension( 0 ) && 
			 offsetY + img.getDimension( 1 ) < source.getDimension( 1 ) )
		{
			// it is completely inside so we need no outofbounds for copying
			positionable = source.createLocalizableByDimCursor();
		}
		else
		{
			positionable = source.createLocalizableByDimCursor( new OutOfBoundsStrategyMirrorFactory<FloatType>() );
		}
			
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.getPosition( location );
			
			location[ 0 ] += offsetX;
			location[ 1 ] += offsetY;
			
			positionable.setPosition( location );
			
			cursor.getType().set( positionable.getType() );			
		}
		
		return img;
	}
	
	/**
	 * Normalize and make a copy of the {@link ImagePlus} into an {@link Image}<FloatType> for faster access when copying the slices
	 * 
	 * @param imp - the {@link ImagePlus} input image
	 * @return - the normalized copy [0...1]
	 */
	public static Image<FloatType> convertToFloat( final ImagePlus imp, int channel, int timepoint )
	{
		// stupid 1-offset of imagej
		channel++;
		timepoint++;
		
		final Image<FloatType> img;
		
		if ( imp.getNSlices() > 1 )
			img = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ imp.getWidth(), imp.getHeight(), imp.getNSlices() } );
		else
			img = new ImageFactory<FloatType>( new FloatType(), new ArrayContainerFactory() ).createImage( new int[]{ imp.getWidth(), imp.getHeight() } );
		
		final int sliceSize = imp.getWidth() * imp.getHeight();
		
		int z = 0;
		ImageProcessor ip = imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) );
		
		if ( ip instanceof FloatProcessor )
		{
			final ArrayCursor<FloatType> cursor = (ArrayCursor<FloatType>)img.createCursor();
			
			float[] pixels = (float[])ip.getPixels();
			int i = 0;
			
			while ( cursor.hasNext() )
			{
				// only get new imageprocessor if necessary
				if ( i == sliceSize )
				{
					++z;
					
					pixels = (float[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels();
						 
					i = 0;
				}
				
				cursor.next().set( pixels[ i++ ] );
			}
		}
		else if ( ip instanceof ByteProcessor )
		{
			final ArrayCursor<FloatType> cursor = (ArrayCursor<FloatType>)img.createCursor();

			byte[] pixels = (byte[])ip.getPixels();
			int i = 0;
			
			while ( cursor.hasNext() )
			{
				// only get new imageprocessor if necessary
				if ( i == sliceSize )
				{
					++z;
					pixels = (byte[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels();
					
					i = 0;
				}
				
				cursor.next().set( pixels[ i++ ] & 0xff );
			}
		}
		else if ( ip instanceof ShortProcessor )
		{
			final ArrayCursor<FloatType> cursor = (ArrayCursor<FloatType>)img.createCursor();

			short[] pixels = (short[])ip.getPixels();
			int i = 0;
			
			while ( cursor.hasNext() )
			{
				// only get new imageprocessor if necessary
				if ( i == sliceSize )
				{
					++z;
					
					pixels = (short[])imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) ).getPixels();
					
					i = 0;
				}
				
				cursor.next().set( pixels[ i++ ] & 0xffff );
			}
		}
		else // some color stuff or so 
		{
			final LocalizableCursor<FloatType> cursor = img.createLocalizableCursor();
			final int[] location = new int[ img.getNumDimensions() ];

			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.getPosition( location );
				
				// only get new imageprocessor if necessary
				if ( location[ 2 ] != z )
				{
					z = location[ 2 ];
					
					ip = imp.getStack().getProcessor( imp.getStackIndex( channel, z + 1, timepoint ) );
				}
				
				cursor.getType().set( ip.getPixelValue( location[ 0 ], location[ 1 ] ) );
			}
		}
		
		ViewDataBeads.normalizeImage( img );
		
		return img;
	}
	
	/**
	 * Instantiates the panel for adjusting the paramters
	 */
	protected void displaySliders()
	{
		final Frame frame = new Frame("Adjust Difference-of-Gaussian Values");
		frame.setSize( 400, 300 );        
		
		/* Instantiation */		
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
				
		final Scrollbar sigma1 = new Scrollbar ( Scrollbar.HORIZONTAL, sigmaInit, 10, 0, 10 + scrollbarSize );		
	    this.sigma = computeValueFromScrollbarPosition( sigmaInit, sigmaMin, sigmaMax, scrollbarSize); 
	    
	    final Scrollbar threshold = new Scrollbar ( Scrollbar.HORIZONTAL, thresholdInit, 10, 0, 10 + scrollbarSize );
	    final float log1001 = (float)Math.log10( scrollbarSize + 1);
	    
	    this.threshold = thresholdMin + ( (log1001 - (float)Math.log10(1001-thresholdInit))/log1001 ) * (thresholdMax-thresholdMin);
	    
	    this.sigma2 = computeSigma2( this.sigma, this.sensitivity );
	    final int sigma2init = computeScrollbarPositionFromValue( this.sigma2, sigmaMin, sigmaMax, scrollbarSize ); 
		final Scrollbar sigma2 = new Scrollbar ( Scrollbar.HORIZONTAL, sigma2init, 10, 0, 10 + scrollbarSize );
		
	    final Label sigmaText1 = new Label( "Sigma 1 = " + this.sigma, Label.CENTER );
	    final Label sigmaText2 = new Label( "Sigma 2 = " + this.sigma2, Label.CENTER );
	    	    
	    final Label thresholdText = new Label( "Threshold = " + this.threshold, Label.CENTER );
	    final Button apply = new Button( "Apply to Stack (will take some time)" );
	    final Button button = new Button( "Done" );
	    
	    final Checkbox sigma2Enable = new Checkbox( "Enable Manual Adjustment of Sigma 2 ", enableSigma2 );
	    final Checkbox min = new Checkbox( "Look for Minima (red)", lookForMinima );
	    final Checkbox max = new Checkbox( "Look for Maxima (green)", lookForMaxima );
	    
	    /* Location */
	    frame.setLayout( layout );
	    
	    c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
	    frame.add ( sigma1, c );

	    ++c.gridy;
	    frame.add( sigmaText1, c );

	    ++c.gridy;
	    frame.add ( sigma2, c );

	    ++c.gridy;
	    frame.add( sigmaText2, c );
	    
	    ++c.gridy;
	    c.insets = new Insets(0,65,0,65);
	    frame.add( sigma2Enable, c );
	    
		++c.gridy;
	    c.insets = new Insets(10,0,0,0);
	    frame.add ( threshold, c );
	    c.insets = new Insets(0,0,0,0);

	    ++c.gridy;
	    frame.add( thresholdText, c );

	    ++c.gridy;
	    c.insets = new Insets(0,130,0,75);
	    frame.add( min, c );

	    ++c.gridy;
	    c.insets = new Insets(0,125,0,75);
	    frame.add( max, c );

	    ++c.gridy;
	    c.insets = new Insets(0,75,0,75);
	    frame.add( apply, c );

	    ++c.gridy;
	    c.insets = new Insets(10,150,0,150);
	    frame.add( button, c );

	    /* Configuration */
	    sigma1.addAdjustmentListener( new SigmaListener( sigmaText1, sigmaMin, sigmaMax, scrollbarSize, sigma1, sigma2, sigmaText2 ) );
	    sigma2.addAdjustmentListener( new Sigma2Listener( sigmaMin, sigmaMax, scrollbarSize, sigma2, sigmaText2 ) );
	    threshold.addAdjustmentListener( new ThresholdListener( thresholdText, thresholdMin, thresholdMax ) );
	    button.addActionListener( new DoneButtonListener( frame ) );
		apply.addActionListener( new ApplyButtonListener() );
		min.addItemListener( new MinListener() );
		max.addItemListener( new MaxListener() );
		sigma2Enable.addItemListener( new EnableListener( sigma2, sigmaText2 ) );
		
		if ( !sigma2IsAdjustable )
			sigma2Enable.setEnabled( false );
		
	    frame.addWindowListener( new FrameListener( frame ) );
		
		frame.setVisible( true );
		
		originalColor = sigma2.getBackground();
		sigma2.setBackground( inactiveColor );
	    sigmaText1.setFont( sigmaText1.getFont().deriveFont( Font.BOLD ) );
	    thresholdText.setFont( thresholdText.getFont().deriveFont( Font.BOLD ) );
	}

	protected class EnableListener implements ItemListener
	{
		final Scrollbar sigma2;
		final Label sigmaText2;
		
		public EnableListener( final Scrollbar sigma2, final Label sigmaText2 )
		{
			this.sigmaText2 = sigmaText2;
			this.sigma2 = sigma2;
		}
		
		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			if ( arg0.getStateChange() == ItemEvent.DESELECTED )
			{
				sigmaText2.setFont( sigmaText2.getFont().deriveFont( Font.PLAIN ) );
				sigma2.setBackground( inactiveColor );
				enableSigma2 = false;
			}
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
			{
				sigmaText2.setFont( sigmaText2.getFont().deriveFont( Font.BOLD ) );
				sigma2.setBackground( originalColor );
				enableSigma2 = true;
			}
		}
	}
	
	protected class MinListener implements ItemListener
	{
		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			boolean oldState = lookForMinima;
			
			if ( arg0.getStateChange() == ItemEvent.DESELECTED )				
				lookForMinima = false;			
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
				lookForMinima = true;
			
			if ( lookForMinima != oldState )
			{
				while ( isComputing )
					SimpleMultiThreading.threadWait( 10 );
				
				updatePreview( ValueChange.MINMAX );
			}
		}
	}

	protected class MaxListener implements ItemListener
	{
		@Override
		public void itemStateChanged( final ItemEvent arg0 )
		{
			boolean oldState = lookForMaxima;
			
			if ( arg0.getStateChange() == ItemEvent.DESELECTED )				
				lookForMaxima = false;			
			else if ( arg0.getStateChange() == ItemEvent.SELECTED  )
				lookForMaxima = true;
			
			if ( lookForMaxima != oldState )
			{
				while ( isComputing )
					SimpleMultiThreading.threadWait( 10 );
				
				updatePreview( ValueChange.MINMAX );
			}
		}
	}

	/**
	 * Tests whether the ROI was changed and will recompute the preview 
	 * 
	 * @author Stephan Preibisch
	 */
	protected class RoiListener implements MouseListener
	{
		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			// here the ROI might have been modified, let's test for that
			final Roi roi = imp.getRoi();
			
			if ( roi == null || roi.getType() != Roi.RECTANGLE )
				return;
			
			while ( isComputing )
				SimpleMultiThreading.threadWait( 10 );
			
			updatePreview( ValueChange.ROI );				
		}
		
	}

	protected class ApplyButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed( final ActionEvent arg0 ) 
		{
			// test the parameters on the complete stack
			final ArrayList<DifferenceOfGaussianPeak<FloatType>> peaks = 
				DetectionSegmentation.extractBeadsLaPlaceImgLib( 
			                                				source, 
			                                				new OutOfBoundsStrategyMirrorFactory<FloatType>(), 
			                                				imageSigma, 
			                                				sigma,
			                                				sigma2,
			                                				threshold, 
			                                				threshold/4, 
			                                				lookForMaxima,
			                                				lookForMinima,
			                                				ViewStructure.DEBUG_MAIN );
			
			// display as extra image
			Image<FloatType> detections = source.createNewImage();
			final LocalizableByDimCursor<FloatType> c = detections.createLocalizableByDimCursor();
			
	        for ( final DifferenceOfGaussianPeak<FloatType> peak : peaks )
	        {
				final LocalizablePoint p = new LocalizablePoint( new float[]{ peak.getSubPixelPosition( 0 ), peak.getSubPixelPosition( 1 ), peak.getSubPixelPosition( 2 ) } );
				
				c.setPosition( p );
				c.getType().set( 1 );
	        }
	        
	        
	        final GaussianConvolutionReal<FloatType> gauss = new GaussianConvolutionReal<FloatType>( detections, new OutOfBoundsStrategyValueFactory<FloatType>(), 2 );
	        gauss.process();
	        
	        detections = gauss.getResult();
	        
	        ImageJFunctions.show( detections );
		}
	}

	protected class DoneButtonListener implements ActionListener
	{
		final Frame parent;
		
		public DoneButtonListener( Frame parent )
		{
			this.parent = parent;
		}
		
		@Override
		public void actionPerformed( final ActionEvent arg0 ) 
		{ 
			close( parent, sliceObserver, imp, roiListener );
		}
	}
	
	protected class FrameListener extends WindowAdapter
	{
		final Frame parent;
		
		public FrameListener( Frame parent )
		{
			super();
			this.parent = parent;
		}
		
		@Override
        public void windowClosing (WindowEvent e) 
		{ 
			close( parent, sliceObserver, imp, roiListener );
		}
	}
	
	protected final void close( final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp, final RoiListener roiListener )
	{
		if ( parent != null )
			parent.dispose();
		
		if ( sliceObserver != null )
			sliceObserver.unregister();
		
		if ( imp != null )
		{
			if ( roiListener != null )
				imp.getCanvas().removeMouseListener( roiListener );
			
			imp.getOverlay().clear();
			imp.updateAndDraw();
		}
		
		isFinished = true;
	}

	protected class Sigma2Listener implements AdjustmentListener
	{
		final float min, max;
		final int scrollbarSize;
		
		final Scrollbar sigmaScrollbar2;
		final Label sigma2Label;
		
		public Sigma2Listener( final float min, final float max, final int scrollbarSize, final Scrollbar sigmaScrollbar2, final Label sigma2Label )
		{
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;
			
			this.sigmaScrollbar2 = sigmaScrollbar2;
			this.sigma2Label = sigma2Label;
		}
		
		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{
			if ( enableSigma2 )
			{
				sigma2 = computeValueFromScrollbarPosition( event.getValue(), min, max, scrollbarSize );
				
				if ( sigma2 < sigma )
				{
					sigma2 = sigma + 0.001f;
					sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
				}
				
				sigma2Label.setText( "Sigma 2 = " + sigma2 );
				
				if ( !event.getValueIsAdjusting() )
				{
					while ( isComputing )
					{
						SimpleMultiThreading.threadWait( 10 );
					}
					updatePreview( ValueChange.SIGMA );
				}
				
			}
			else
			{
				// if no manual adjustment simply reset it
				sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
			}
		}		
	}

	protected class SigmaListener implements AdjustmentListener
	{
		final Label label;
		final float min, max;
		final int scrollbarSize;
		
		final Scrollbar sigmaScrollbar1;
		final Scrollbar sigmaScrollbar2;		
		final Label sigmaText2;
		
		public SigmaListener( final Label label, final float min, final float max, final int scrollbarSize, final Scrollbar sigmaScrollbar1,  final Scrollbar sigmaScrollbar2, final Label sigmaText2  )
		{
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;
			
			this.sigmaScrollbar1 = sigmaScrollbar1;
			this.sigmaScrollbar2 = sigmaScrollbar2;
			this.sigmaText2 = sigmaText2;
		}
		
		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{
			sigma = computeValueFromScrollbarPosition( event.getValue(), min, max, scrollbarSize );			
			
			if ( !enableSigma2 )
			{
				sigma2 = computeSigma2( sigma, sensitivity );
				sigmaText2.setText( "Sigma 2 = " + sigma2 );			    
				sigmaScrollbar2.setValue( computeScrollbarPositionFromValue( sigma2, min, max, scrollbarSize ) );
			}
			else if ( sigma > sigma2 )
			{
				sigma = sigma2 - 0.001f;
				sigmaScrollbar1.setValue( computeScrollbarPositionFromValue( sigma, min, max, scrollbarSize ) );
			}
			
			label.setText( "Sigma 1 = " + sigma );
			
			if ( !event.getValueIsAdjusting() )
			{
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.SIGMA );
			}
		}		
	}

	protected static float computeValueFromScrollbarPosition( final int scrollbarPosition, final float min, final float max, final int scrollbarSize )
	{
		return min + (scrollbarPosition/(float)scrollbarSize) * (max-min);
	}

	protected static int computeScrollbarPositionFromValue( final float sigma, final float min, final float max, final int scrollbarSize )
	{
		return Util.round( ((sigma - min)/(max-min)) * scrollbarSize );
	}

	protected class ThresholdListener implements AdjustmentListener
	{
		final Label label;
		final float min, max;
		final float log1001 = (float)Math.log10(1001);
		
		public ThresholdListener( final Label label, final float min, final float max )
		{
			this.label = label;
			this.min = min;
			this.max = max;
		}
		
		@Override
		public void adjustmentValueChanged( final AdjustmentEvent event )
		{			
			threshold = min + ( (log1001 - (float)Math.log10(1001-event.getValue()))/log1001 ) * (max-min);
			label.setText( "Threshold = " + threshold );

			if ( !isComputing )
			{
				updatePreview( ValueChange.THRESHOLD );
			}
			else if ( !event.getValueIsAdjusting() )
			{
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.THRESHOLD );
			}
		}		
	}

	protected class ImagePlusListener implements SliceListener
	{
		@Override
		public void sliceChanged(ImagePlus arg0)
		{
			if ( isStarted )
			{
				while ( isComputing )
				{
					SimpleMultiThreading.threadWait( 10 );
				}
				updatePreview( ValueChange.SLICE );
			}
		}		
	}
	
	public static void main( String[] args )
	{
		new ImageJ();
		
		ImagePlus imp = new Opener().openImage( "/Users/preibischs/Documents/Microscopy/SPIM/HisYFP-SPIM/spim_TL18_Angle0_cropped.tif" );
		//ImagePlus imp = new Opener().openImage( "D:/Documents and Settings/Stephan/My Documents/Downloads/1-315--0.08-isotropic-subvolume/1-315--0.08-isotropic-subvolume.tif" );
		imp.show();
		
		imp.setSlice( 27 );		
		imp.setRoi( imp.getWidth()/4, imp.getHeight()/4, imp.getWidth()/2, imp.getHeight()/2 );		
		
		new InteractiveDoG().run( null ); 	
			
	}
}
