package gadgets;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.Type;

/**
 * This class produces new MaskImage objects, based on a before defined
 * mask.
 */
public class RoiImageFactory<T extends Type<T>> extends ImageFactory<T> {
	// the ROIs offset
	final int[] offset;
	// the ROIs size
	final int[] size;

	public RoiImageFactory(final int[] offset, final int size[], final T type,
			final ContainerFactory containerFactory) {
		super(type, containerFactory);
		this.offset = offset;
		this.size = size;
	}

	@Override
	public Image<T> createImage( final int dim[], final String name )
	{
		// create a new RoiImage with the this factorys data
		Image<T> img = super.createImage(dim, name);
		return new RoiImage<T>(img, offset, size);
	}
}

