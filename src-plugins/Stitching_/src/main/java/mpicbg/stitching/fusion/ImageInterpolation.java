package mpicbg.stitching.fusion;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * This class is necessary as it can create an {@link Interpolator} for an {@link Image} even if hold it as < ? extends RealType< ? > >
 * 
 * @author preibischs
 *
 * @param <T>
 */
public class ImageInterpolation< T extends RealType< T > > 
{
	final Image< T > image;
	final InterpolatorFactory< T > interpolatorFactory;
	
	public ImageInterpolation( final Image< T > image, final InterpolatorFactory< T > interpolatorFactory )
	{
		this.image = image;
		this.interpolatorFactory = interpolatorFactory;
	}
	
	public Image< T > getImage() { return image; }
	public Interpolator< T > createInterpolator() { return interpolatorFactory.createInterpolator( image ); }
}
