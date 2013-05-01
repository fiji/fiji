package mpicbg.imglib.wrapper;

import java.util.ArrayList;

import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.basictypecontainer.ByteAccess;
import mpicbg.imglib.container.basictypecontainer.DoubleAccess;
import mpicbg.imglib.container.basictypecontainer.FloatAccess;
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.container.basictypecontainer.LongAccess;
import mpicbg.imglib.container.basictypecontainer.ShortAccess;
import mpicbg.imglib.container.basictypecontainer.array.ByteArray;
import mpicbg.imglib.container.basictypecontainer.array.DoubleArray;
import mpicbg.imglib.container.basictypecontainer.array.FloatArray;
import mpicbg.imglib.container.basictypecontainer.array.IntArray;
import mpicbg.imglib.container.basictypecontainer.array.LongArray;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.container.cell.CellContainer;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.integer.ByteType;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedIntType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.DoubleType;
import mpicbg.imglib.type.numeric.real.FloatType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;

public class ImgLib1
{
	/**
	 * wrapArrays an ImgLib1 {@link Image} of FloatType (has to be cell) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.real.FloatType > wrapCellFloatToImgLib2 ( final Image< FloatType > image )
	{
		// extract float[] arrays
		@SuppressWarnings( "unchecked" )
		final CellContainer< FloatType, FloatArray > cellContainer = (CellContainer< FloatType, FloatArray >)image.getContainer();

		final ArrayList< net.imglib2.img.basictypeaccess.array.FloatArray > celldata = new ArrayList< net.imglib2.img.basictypeaccess.array.FloatArray >( );

		for ( int i = 0; i < cellContainer.getNumCells(); ++i )
			celldata.add( new net.imglib2.img.basictypeaccess.array.FloatArray( 
					cellContainer.getCell( i ).getData().getCurrentStorageArray() ) );
		
		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		final CellImgFactory< net.imglib2.type.numeric.real.FloatType > factory = new CellImgFactory< net.imglib2.type.numeric.real.FloatType >( cellContainer.getCellSize() );
		
		// create ImgLib2 CellImg		
		return new CellImg< net.imglib2.type.numeric.real.FloatType, net.imglib2.img.basictypeaccess.array.FloatArray, LoadCell< net.imglib2.img.basictypeaccess.array.FloatArray > >
			( factory, new ListImgCellsLoad< net.imglib2.img.basictypeaccess.array.FloatArray >( 
					celldata, 1, dim, cellContainer.getCellSize() ) );
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of FloatType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.real.FloatType > wrapArrayFloatToImgLib2 ( final Image< FloatType > image )
	{
		// extract float[] array
		@SuppressWarnings( "unchecked" )
		final Array< FloatType, FloatAccess> array = (Array< FloatType, FloatAccess> ) image.getContainer();		
		final FloatArray f = (FloatArray)array.update( null );		
		final float[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.FloatAccess access = new net.imglib2.img.basictypeaccess.array.FloatArray( data );
		final ArrayImg< net.imglib2.type.numeric.real.FloatType, net.imglib2.img.basictypeaccess.FloatAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.real.FloatType, net.imglib2.img.basictypeaccess.FloatAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.real.FloatType linkedType = new net.imglib2.type.numeric.real.FloatType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of DoubleType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.real.DoubleType > wrapArrayDoubleToImgLib2 ( final Image< DoubleType > image )
	{
		// extract float[] array
		@SuppressWarnings( "unchecked" )
		final Array< DoubleType, DoubleAccess> array = (Array< DoubleType, DoubleAccess> ) image.getContainer();		
		final DoubleArray f = (DoubleArray)array.update( null );		
		final double[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.DoubleAccess access = new net.imglib2.img.basictypeaccess.array.DoubleArray( data );
		final ArrayImg< net.imglib2.type.numeric.real.DoubleType, net.imglib2.img.basictypeaccess.DoubleAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.real.DoubleType, net.imglib2.img.basictypeaccess.DoubleAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.real.DoubleType linkedType = new net.imglib2.type.numeric.real.DoubleType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of LongType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.LongType > wrapArrayLongToImgLib2 ( final Image< LongType > image )
	{
		// extract Long[] array
		@SuppressWarnings( "unchecked" )
		final Array< LongType, LongAccess> array = (Array< LongType, LongAccess> ) image.getContainer();		
		final LongArray f = (LongArray)array.update( null );		
		final long[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.LongAccess access = new net.imglib2.img.basictypeaccess.array.LongArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.LongType, net.imglib2.img.basictypeaccess.LongAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.LongType, net.imglib2.img.basictypeaccess.LongAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.LongType linkedType = new net.imglib2.type.numeric.integer.LongType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of UnsignedIntType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.UnsignedIntType > wrapArrayUnsignedIntToImgLib2 ( final Image< UnsignedIntType > image )
	{
		// extract Int[] array
		@SuppressWarnings( "unchecked" )
		final Array< UnsignedIntType, IntAccess> array = (Array< UnsignedIntType, IntAccess> ) image.getContainer();		
		final IntArray f = (IntArray)array.update( null );		
		final int[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.IntAccess access = new net.imglib2.img.basictypeaccess.array.IntArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.UnsignedIntType, net.imglib2.img.basictypeaccess.IntAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.UnsignedIntType, net.imglib2.img.basictypeaccess.IntAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.UnsignedIntType linkedType = new net.imglib2.type.numeric.integer.UnsignedIntType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of IntType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.IntType > wrapArrayIntToImgLib2 ( final Image< IntType > image )
	{
		// extract Int[] array
		@SuppressWarnings( "unchecked" )
		final Array< IntType, IntAccess> array = (Array< IntType, IntAccess> ) image.getContainer();		
		final IntArray f = (IntArray)array.update( null );		
		final int[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.IntAccess access = new net.imglib2.img.basictypeaccess.array.IntArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.IntType, net.imglib2.img.basictypeaccess.IntAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.IntType, net.imglib2.img.basictypeaccess.IntAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.IntType linkedType = new net.imglib2.type.numeric.integer.IntType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of UnsignedShortType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.UnsignedShortType > wrapArrayUnsignedShortToImgLib2 ( final Image< UnsignedShortType > image )
	{
		// extract Short[] array
		@SuppressWarnings( "unchecked" )
		final Array< UnsignedShortType, ShortAccess> array = (Array< UnsignedShortType, ShortAccess> ) image.getContainer();		
		final ShortArray f = (ShortArray)array.update( null );		
		final short[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.ShortAccess access = new net.imglib2.img.basictypeaccess.array.ShortArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.UnsignedShortType, net.imglib2.img.basictypeaccess.ShortAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.UnsignedShortType, net.imglib2.img.basictypeaccess.ShortAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.UnsignedShortType linkedType = new net.imglib2.type.numeric.integer.UnsignedShortType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of ShortType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.ShortType > wrapArrayShortToImgLib2 ( final Image< ShortType > image )
	{
		// extract Short[] array
		@SuppressWarnings( "unchecked" )
		final Array< ShortType, ShortAccess> array = (Array< ShortType, ShortAccess> ) image.getContainer();		
		final ShortArray f = (ShortArray)array.update( null );		
		final short[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.ShortAccess access = new net.imglib2.img.basictypeaccess.array.ShortArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.ShortType, net.imglib2.img.basictypeaccess.ShortAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.ShortType, net.imglib2.img.basictypeaccess.ShortAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.ShortType linkedType = new net.imglib2.type.numeric.integer.ShortType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of ByteType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.ByteType > wrapArrayByteToImgLib2 ( final Image< ByteType > image )
	{
		// extract Byte[] array
		@SuppressWarnings( "unchecked" )
		final Array< ByteType, ByteAccess> array = (Array< ByteType, ByteAccess> ) image.getContainer();		
		final ByteArray f = (ByteArray)array.update( null );		
		final byte[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.ByteAccess access = new net.imglib2.img.basictypeaccess.array.ByteArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.ByteType, net.imglib2.img.basictypeaccess.ByteAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.ByteType, net.imglib2.img.basictypeaccess.ByteAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.ByteType linkedType = new net.imglib2.type.numeric.integer.ByteType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}

	/**
	 * wrapArrays an ImgLib1 {@link Image} of UnsignedByteType (has to be array) into an ImgLib2 {@link Img} using an {@link ArrayImg}
	 * 
	 * @param image - input image
	 * @return 
	 */	
	public static Img< net.imglib2.type.numeric.integer.UnsignedByteType > wrapArrayUnsignedByteToImgLib2 ( final Image< UnsignedByteType > image )
	{
		// extract Byte[] array
		@SuppressWarnings( "unchecked" )
		final Array< UnsignedByteType, ByteAccess> array = (Array< UnsignedByteType, ByteAccess> ) image.getContainer();		
		final ByteArray f = (ByteArray)array.update( null );		
		final byte[] data = f.getCurrentStorageArray();

		// convert coordinates
		final long dim[] = new long[ image.getNumDimensions() ];
		for ( int d = 0; d < dim.length; ++d )
			dim[ d ] = image.getDimension( d );		
		
		// create ImgLib2 Array		
		final net.imglib2.img.basictypeaccess.ByteAccess access = new net.imglib2.img.basictypeaccess.array.ByteArray( data );
		final ArrayImg< net.imglib2.type.numeric.integer.UnsignedByteType, net.imglib2.img.basictypeaccess.ByteAccess> arrayImgLib2 = 
			new ArrayImg< net.imglib2.type.numeric.integer.UnsignedByteType, net.imglib2.img.basictypeaccess.ByteAccess>( access, dim, 1 );
			
		// create a Type that is linked to the container
		final net.imglib2.type.numeric.integer.UnsignedByteType linkedType = new net.imglib2.type.numeric.integer.UnsignedByteType( arrayImgLib2 );
		
		// pass it to the DirectAccessContainer
		arrayImgLib2.setLinkedType( linkedType );		
		
		return arrayImgLib2;
	}
}
