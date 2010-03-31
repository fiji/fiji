/**
 * Gaussian downsampling of an image with ImageJ on-board tools.
 *
 * Motivation:
 * Sound downsampling of an image requires the elimination of image frequencies
 * higher than half the sampling frequency in the result image (see the
 * Nyquist-Shannon sampling theorem).  The exclusive tool for this is Gaussian
 * convolution.
 *
 * This script calculates the required Gaussian kernel for a given target size,
 * smoothes the image and resamples it.
 *
 * Furthermore, you can define the "intrinsic" Gaussian kernel of the source and
 * target images.  An optimal sampler is identified by sigma=0.5.  If your
 * source image was blurred already, you may set a higher source sigma for a
 * sharper result.  Setting target sigma to values smaller than 0.5 makes the
 * result appear sharper and therefore eventually aliased.
 */
var imp = WindowManager.getCurrentImage();
var width = 0;
var height = 0;
var sourceSigma = 0.5;
var targetSigma = 0.5;
var widthField;
var heightField;
var fieldWithFocus;

var textListener = new java.awt.event.TextListener(
	{
		textValueChanged : function( e )
		{
			var source = e.getSource();
			var newWidth = Math.round( widthField.getText() );
			var newHeight = Math.round( heightField.getText() );
			
			if ( source == widthField && fieldWithFocus == widthField && newWidth )
			{
				newHeight = Math.round( newWidth * imp.getHeight() / imp.getWidth() );
				heightField.setText( newHeight );
			}
			else if ( source == heightField && fieldWithFocus == heightField && newHeight )
			{
				newWidth = Math.round( newHeight * imp.getWidth() / imp.getHeight() );
				widthField.setText( newWidth );
			}
		} 
	} );

var focusListener = new java.awt.event.FocusListener(
	{
		focusGained : function ( e )
		{
			fieldWithFocus = e.getSource();
		},
		focusLost : function( e ){} 
	} );

if ( imp )
{
	width = imp.getWidth();
	height = imp.getHeight();
	
	gd = new GenericDialog( "Downsample" );
	gd.addNumericField( "width :", width, 0 );
	gd.addNumericField( "height :", height, 0 );
	gd.addNumericField( "source sigma :", sourceSigma, 2 );
	gd.addNumericField( "target sigma :", targetSigma, 2 );
	gd.addCheckbox( "keep source image", true );
	var fields = gd.getNumericFields();
	
	widthField = fields.get( 0 );
	heightField = fields.get( 1 );
	fieldWithFocus = widthField;
	
	widthField.addFocusListener( focusListener );
	widthField.addTextListener( textListener );
	heightField.addFocusListener( focusListener );
	heightField.addTextListener( textListener );
		
	gd.showDialog();
	if ( gd.wasOKed() )
	{
		width = gd.getNextNumber();
		height = gd.getNextNumber();
		sourceSigma = gd.getNextNumber();
		targetSigma = gd.getNextNumber();
		keepSource = gd.getNextBoolean();
		
		if ( width <= imp.getWidth() )
		{
			var s;
			if ( fieldWithFocus == widthField )
				s = targetSigma * imp.getWidth() / width;
			else
				s = targetSigma * imp.getHeight() / height;
			
			if ( keepSource )
				IJ.run( "Duplicate...", "title=" + imp.getTitle() + " duplicate" );
			IJ.run( "Gaussian Blur...", "sigma=" + Math.sqrt( s * s - sourceSigma * sourceSigma ) + " stack" );
                        //IJ.log( "sigma = " + Math.sqrt( s * s - sourceSigma * sourceSigma ));
			IJ.run( "Scale...", "x=- y=- width=" + width + " height=" + height + " process title=- interpolation=None" );
                        
                        var extraX = (imp.getWidth() % 2 == 0) ? 0 : 1;
                        var extraY = (imp.getHeight() % 2 == 0) ? 0 : 1;
			var initialX = (width % 2 == 0) ? (imp.getWidth() / 2 - width/2 + extraX) : (imp.getWidth() / 2 - width/2 +1 -extraX);
                        var initialY = (height % 2 == 0) ? (imp.getHeight() / 2 - height/2 + extraY) : (imp.getHeight() / 2 - height/2 +1 -extraY);
                        IJ.makeRectangle(initialX, initialY, width, height);
                        IJ.run("Crop");
                        //IJ.run( "Canvas Size...", "width=" + width + " height=" + height + " position=Center" );
		}
		else
			IJ.showMessage( "You try to upsample the image.  You need an interpolator for that not a downsampler." );
	}
}
else
	IJ.showMessage( "You should have at least one image open." );
