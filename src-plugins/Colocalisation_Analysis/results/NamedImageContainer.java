package results;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A small image container for Imglib images that overrides
 * the toString method to get only the name of the image.
 *
 */
public class NamedImageContainer {
	Image<? extends RealType> image;

	public NamedImageContainer(Image<? extends RealType> image) {
		this.image = image;
	}

	public Image<? extends RealType> getImage() {
		return image;
	}

	public String toString() {
		return image.getName();
	}
}
