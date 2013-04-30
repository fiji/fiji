package mpicbg.imglib.wrapper;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.imglib.image.Image;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.type.numeric.real.FloatType;

public class ImgLib2 
{
	/**
	 * Wraps an ImgLib2 {@link Img} (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.real.FloatType > wrapToImgLib1 ( final Img< FloatType > img )
	{
		// extract float[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< FloatType, net.imglib2.img.basictypeaccess.FloatAccess > array = (ArrayImg< FloatType, net.imglib2.img.basictypeaccess.FloatAccess >)img;
		final net.imglib2.img.basictypeaccess.array.FloatArray f = (net.imglib2.img.basictypeaccess.array.FloatArray)array.update( null );
		final float[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib2 Array		
		final FloatAccess floatAccess = new FloatArray( data );
		final Array<mpicbg.imglib.type.numeric.real.FloatType, FloatAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.real.FloatType, FloatAccess>( new ArrayContainerFactory(), floatAccess, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.real.FloatType linkedType = new mpicbg.imglib.type.numeric.real.FloatType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.real.FloatType> (arrayImgLib1, new mpicbg.imglib.type.numeric.real.FloatType() );
	}
	
}
