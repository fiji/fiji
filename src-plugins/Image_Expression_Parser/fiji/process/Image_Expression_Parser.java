package fiji.process;

import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.NumericType;

public class Image_Expression_Parser<T extends NumericType<T>> implements PlugIn {
	ImagePlus image;

	public void run(String arg) {
		
		
		
	}

	public void run(ImagePlus image) {
		Image<T> img = ImagePlusAdapter.wrap(image);
		add(img, 20);
	}

	public static<T extends NumericType<T>> void add(Image<T> img, float value) {
		final Cursor<T> cursor = img.createCursor();
		final T summand = cursor.getType().createVariable();

		summand.setReal(value);

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().add(summand);
		}
	}

}


