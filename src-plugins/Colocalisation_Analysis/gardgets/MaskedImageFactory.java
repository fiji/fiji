package gadgets;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.Type;

/**
 * This class produces new MaskImage objects, based on a before defined
 * mask.
 */
public class MaskedImageFactory<T extends Type<T>> extends ImageFactory<T> {
	// the mask for newly created images
	Image<T> mask;
	// the masks bounding box offset
	int[] offset;
	// the size of the masks bounding box
	int[] size;

	public MaskedImageFactory(final Image<T> mask, int[] offset, int[] size, final T type,
			final ContainerFactory containerFactory) {
		super(type, containerFactory);
		this.mask = mask;
		this.offset = offset;
		this.size = size;
	}

	@Override
	public Image<T> createImage( final int dim[], final String name )
	{
		// create a new MaskImage with the mask of this factory
		Image<T> img = super.createImage(dim, name);
		return new MaskedImage(img, mask, offset, size);
	}
}

