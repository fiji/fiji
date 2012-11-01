package mpicbg.spim.io;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.label.FakeType;

/**
 * 
 * @author Stephan Preibisch
 *
 */
public class ImgLibSaver 
{
	public static <T extends Type<T>> boolean saveAsTiffs( final Image<T> img, String directory, final int type )
	{
		return saveAsTiffs( img, directory, img.getName(), type );
	}

	public static <T extends Type<T>> boolean saveAsTiffs( final Image<T> img, String directory, final String name, final int type )
	{
		final Display<T> display = img.getDisplay();
		boolean everythingOK = true;

		if ( directory == null )
			directory = "";

		directory = directory.replace('\\', '/');
		directory = directory.trim();
		if (directory.length() > 0 && !directory.endsWith("/"))
			directory = directory + "/";

		final int numDimensions = img.getNumDimensions();

		final int[] dimensionPositions = new int[ numDimensions ];

		// x dimension for save is x
		final int dimX = 0;
		// y dimensins for save is y
		final int dimY = 1;

		if ( numDimensions <= 2 )
		{
			final ImageProcessor ip = new FloatProcessor( img.getDimension( dimX ), img.getDimension( dimY ), extractSliceFloat( img, display, dimX, dimY, dimensionPositions ), null);//extract2DSlice( img, display,type, dimX, dimY, dimensionPositions );
			final ImagePlus slice = new ImagePlus( name + ".tif", ip);
        	final FileSaver fs = new FileSaver( slice );
        	everythingOK = everythingOK && fs.saveAsTiff(directory + slice.getTitle());

        	slice.close();
		}
		else // n dimensions
		{
			final int extraDimensions[] = new int[ numDimensions - 2 ];
			final int extraDimPos[] = new int[ extraDimensions.length ];

			for ( int d = 2; d < numDimensions; ++d )
				extraDimensions[ d - 2 ] = img.getDimension( d );

			// the max number of digits for each dimension
			final int maxLengthDim[] = new int[ extraDimensions.length ];

			for ( int d = 2; d < numDimensions; ++d )
			{
				final String num = "" + (img.getDimension( d ) - 1);
				maxLengthDim[ d - 2 ] = num.length();
			}

			//
			// Here we "misuse" a ArrayLocalizableCursor to iterate through the dimensions (d > 2),
			// he will iterate all dimensions as we want ( iterate through d=3, inc 4, iterate through 3, inc 4, ... )
			//
			final ArrayLocalizableCursor<FakeType> cursor = ArrayLocalizableCursor.createLinearCursor( extraDimensions );

			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.getPosition( extraDimPos );

				for ( int d = 2; d < numDimensions; ++d )
					dimensionPositions[ d ] = extraDimPos[ d - 2 ];

				final ImageProcessor ip = new FloatProcessor( img.getDimension( dimX ), img.getDimension( dimY ), extractSliceFloat( img, display, dimX, dimY, dimensionPositions ), null);

	        	String desc = "";

				for ( int d = 2; d < numDimensions; ++d )
				{
		        	String descDim = "" + dimensionPositions[ d ];
		        	while( descDim.length() < maxLengthDim[ d - 2 ] )
		        		descDim = "0" + descDim;
		        	
		        	if ( d == 2 )
		        		desc = desc + "_z" + descDim;
		        	else
		        		desc = desc + "_" + descDim;
				}

	        	final ImagePlus slice = new ImagePlus( name + desc + ".tif", ip);

	        	final FileSaver fs = new FileSaver( slice );
	        	everythingOK = everythingOK && fs.saveAsTiff(directory + slice.getTitle());

	        	slice.close();

			}
		}

		return everythingOK;
	}

    private final static <T extends Type<T>> float[] extractSliceFloat( final Image<T> img, final Display<T> display, final int dimX, final int dimY, final int[] dimensionPositions )
    {
		final int sizeX = img.getDimension( dimX );
		final int sizeY = img.getDimension( dimY );
    	
    	final LocalizablePlaneCursor<T> cursor = img.createLocalizablePlaneCursor();		
		cursor.reset( dimX, dimY, dimensionPositions );   	
		
		// store the slice image
    	float[] sliceImg = new float[ sizeX * sizeY ];
    	
    	if ( dimY < img.getNumDimensions() )
    	{
	    	while ( cursor.hasNext() )
	    	{
	    		cursor.fwd();
	    		sliceImg[ cursor.getPosition( dimX ) + cursor.getPosition( dimY ) * sizeX ] = display.get32Bit( cursor.getType() );    		
	    	}
    	}
    	else // only a 1D image
    	{
	    	while ( cursor.hasNext() )
	    	{
	    		cursor.fwd();
	    		sliceImg[ cursor.getPosition( dimX ) ] = display.get32Bit( cursor.getType() );    		
	    	}    		
    	}
    	
    	cursor.close();

    	return sliceImg;
    }

}
