package mpicbg.imglib.wrapper;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.DefaultCell;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class ImgLib2 
{
	/**
	 * wrapArrays an ImgLib2 {@link Img} of FloatType (has to be cell) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.real.FloatType > wrapCellFloatToImgLib1 ( final Img< FloatType > img )
	{
		// extract float[] arrays from the CellImg consisting of default cells
		@SuppressWarnings( "unchecked" )
		final CellImg< FloatType, FloatArray, DefaultCell< FloatArray > > array = (CellImg< FloatType, FloatArray, DefaultCell< FloatArray > > )img;
		
		//array.cells;
		
		final FloatArray f = (FloatArray)array.update( null );
		final float[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.FloatAccess access = new mpicbg.imglib.container.basictypecontainer.array.FloatArray( data );
		final Array<mpicbg.imglib.type.numeric.real.FloatType, mpicbg.imglib.container.basictypecontainer.FloatAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.real.FloatType, mpicbg.imglib.container.basictypecontainer.FloatAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.real.FloatType linkedType = new mpicbg.imglib.type.numeric.real.FloatType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.real.FloatType> (arrayImgLib1, new mpicbg.imglib.type.numeric.real.FloatType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of FloatType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.real.FloatType > wrapArrayFloatToImgLib1 ( final Img< FloatType > img )
	{
		// extract float[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< FloatType, FloatAccess > array = (ArrayImg< FloatType, FloatAccess >)img;
		final FloatArray f = (FloatArray)array.update( null );
		final float[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.FloatAccess access = new mpicbg.imglib.container.basictypecontainer.array.FloatArray( data );
		final Array<mpicbg.imglib.type.numeric.real.FloatType, mpicbg.imglib.container.basictypecontainer.FloatAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.real.FloatType, mpicbg.imglib.container.basictypecontainer.FloatAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.real.FloatType linkedType = new mpicbg.imglib.type.numeric.real.FloatType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.real.FloatType> (arrayImgLib1, new mpicbg.imglib.type.numeric.real.FloatType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of DoubleType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.real.DoubleType > wrapArrayDoubleToImgLib1 ( final Img< DoubleType > img )
	{
		// extract Double[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< DoubleType, DoubleAccess > array = (ArrayImg< DoubleType, DoubleAccess >)img;
		final DoubleArray f = (DoubleArray)array.update( null );
		final double[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.DoubleAccess access = new mpicbg.imglib.container.basictypecontainer.array.DoubleArray( data );
		final Array<mpicbg.imglib.type.numeric.real.DoubleType, mpicbg.imglib.container.basictypecontainer.DoubleAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.real.DoubleType, mpicbg.imglib.container.basictypecontainer.DoubleAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.real.DoubleType linkedType = new mpicbg.imglib.type.numeric.real.DoubleType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.real.DoubleType> (arrayImgLib1, new mpicbg.imglib.type.numeric.real.DoubleType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of LongType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.LongType > wrapArrayLongToImgLib1 ( final Img< LongType > img )
	{
		// extract Long[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< LongType, LongAccess > array = (ArrayImg< LongType, LongAccess >)img;
		final LongArray f = (LongArray)array.update( null );
		final long[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.LongAccess access = new mpicbg.imglib.container.basictypecontainer.array.LongArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.LongType, mpicbg.imglib.container.basictypecontainer.LongAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.LongType, mpicbg.imglib.container.basictypecontainer.LongAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.LongType linkedType = new mpicbg.imglib.type.numeric.integer.LongType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.LongType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.LongType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of IntType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.IntType > wrapArrayIntToImgLib1 ( final Img< IntType > img )
	{
		// extract Int[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< IntType, IntAccess > array = (ArrayImg< IntType, IntAccess >)img;
		final IntArray f = (IntArray)array.update( null );
		final int[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.IntAccess access = new mpicbg.imglib.container.basictypecontainer.array.IntArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.IntType, mpicbg.imglib.container.basictypecontainer.IntAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.IntType, mpicbg.imglib.container.basictypecontainer.IntAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.IntType linkedType = new mpicbg.imglib.type.numeric.integer.IntType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.IntType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.IntType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of UnsignedIntType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.UnsignedIntType > wrapArrayUnsignedIntToImgLib1 ( final Img< UnsignedIntType > img )
	{
		// extract Int[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< UnsignedIntType, IntAccess > array = (ArrayImg< UnsignedIntType, IntAccess >)img;
		final IntArray f = (IntArray)array.update( null );
		final int[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.IntAccess access = new mpicbg.imglib.container.basictypecontainer.array.IntArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.UnsignedIntType, mpicbg.imglib.container.basictypecontainer.IntAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.UnsignedIntType, mpicbg.imglib.container.basictypecontainer.IntAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.UnsignedIntType linkedType = new mpicbg.imglib.type.numeric.integer.UnsignedIntType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.UnsignedIntType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.UnsignedIntType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of ShortType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.ShortType > wrapArrayShortToImgLib1 ( final Img< ShortType > img )
	{
		// extract Short[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< ShortType, ShortAccess > array = (ArrayImg< ShortType, ShortAccess >)img;
		final ShortArray f = (ShortArray)array.update( null );
		final short[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.ShortAccess access = new mpicbg.imglib.container.basictypecontainer.array.ShortArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.ShortType, mpicbg.imglib.container.basictypecontainer.ShortAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.ShortType, mpicbg.imglib.container.basictypecontainer.ShortAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.ShortType linkedType = new mpicbg.imglib.type.numeric.integer.ShortType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.ShortType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.ShortType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of UnsignedShortType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.UnsignedShortType > wrapArrayUnsignedShortToImgLib1 ( final Img< UnsignedShortType > img )
	{
		// extract Short[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< UnsignedShortType, ShortAccess > array = (ArrayImg< UnsignedShortType, ShortAccess >)img;
		final ShortArray f = (ShortArray)array.update( null );
		final short[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.ShortAccess access = new mpicbg.imglib.container.basictypecontainer.array.ShortArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.UnsignedShortType, mpicbg.imglib.container.basictypecontainer.ShortAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.UnsignedShortType, mpicbg.imglib.container.basictypecontainer.ShortAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.UnsignedShortType linkedType = new mpicbg.imglib.type.numeric.integer.UnsignedShortType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.UnsignedShortType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.UnsignedShortType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of ByteType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.ByteType > wrapArrayByteToImgLib1 ( final Img< ByteType > img )
	{
		// extract Byte[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< ByteType, ByteAccess > array = (ArrayImg< ByteType, ByteAccess >)img;
		final ByteArray f = (ByteArray)array.update( null );
		final byte[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.ByteAccess access = new mpicbg.imglib.container.basictypecontainer.array.ByteArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.ByteType, mpicbg.imglib.container.basictypecontainer.ByteAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.ByteType, mpicbg.imglib.container.basictypecontainer.ByteAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.ByteType linkedType = new mpicbg.imglib.type.numeric.integer.ByteType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.ByteType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.ByteType() );
	}

	/**
	 * wrapArrays an ImgLib2 {@link Img} of UnsignedByteType (has to be array) into an ImgLib1 {@link Image}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Image< mpicbg.imglib.type.numeric.integer.UnsignedByteType > wrapArrayUnsignedByteToImgLib1 ( final Img< UnsignedByteType > img )
	{
		// extract Byte[] array
		@SuppressWarnings( "unchecked" )
		final ArrayImg< UnsignedByteType, ByteAccess > array = (ArrayImg< UnsignedByteType, ByteAccess >)img;
		final ByteArray f = (ByteArray)array.update( null );
		final byte[] data = f.getCurrentStorageArray();
		
		// convert coordinates
		final int dim[] = new int[ img.numDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = (int)img.dimension( d );		
		
		// create ImgLib1 Array		
		final mpicbg.imglib.container.basictypecontainer.ByteAccess access = new mpicbg.imglib.container.basictypecontainer.array.ByteArray( data );
		final Array<mpicbg.imglib.type.numeric.integer.UnsignedByteType, mpicbg.imglib.container.basictypecontainer.ByteAccess> arrayImgLib1 = 
			new Array<mpicbg.imglib.type.numeric.integer.UnsignedByteType, mpicbg.imglib.container.basictypecontainer.ByteAccess>( 
					new ArrayContainerFactory(), access, dim, 1 );
			
		// create a Type that is linked to the container
		final mpicbg.imglib.type.numeric.integer.UnsignedByteType linkedType = new mpicbg.imglib.type.numeric.integer.UnsignedByteType( arrayImgLib1 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib1.setLinkedType( linkedType );		
		
		return  new Image<mpicbg.imglib.type.numeric.integer.UnsignedByteType> (arrayImgLib1, new mpicbg.imglib.type.numeric.integer.UnsignedByteType() );
	}
}
