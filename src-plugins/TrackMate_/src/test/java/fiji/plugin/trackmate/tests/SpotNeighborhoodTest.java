package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import ij.ImageJ;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class SpotNeighborhoodTest {

	public static void main(String[] args) {
		ImageJ.main(args);
		
		// 3D
		ArrayImg<UnsignedShortType, ShortArray> image = ArrayImgs.unsignedShorts(100, 100, 100);
		ImgPlus<UnsignedShortType> img = new ImgPlus<UnsignedShortType>(image);
		Spot spot = new Spot(new double[] { 50, 50, 50 } );
		spot.putFeature(Spot.RADIUS, 30d);
		SpotNeighborhood<UnsignedShortType> neighborhood = new SpotNeighborhood<UnsignedShortType>(spot, img);
		SpotNeighborhoodCursor<UnsignedShortType> cursor = neighborhood.cursor();
		while(cursor.hasNext()) {
			cursor.next().set((int) cursor.getDistanceSquared());
		}
		System.out.println("Finished");
		ImageJFunctions.wrap(img, "3D").show();
		
		// 2D
		ArrayImg<UnsignedShortType, ShortArray> image2 = ArrayImgs.unsignedShorts(100, 100);
		ImgPlus<UnsignedShortType> img2 = new ImgPlus<UnsignedShortType>(image2);
		Spot spot2 = new Spot(new double[] { 50, 50, 0} );
		spot2.putFeature(Spot.RADIUS, 30d);
		SpotNeighborhood<UnsignedShortType> neighborhood2 = new SpotNeighborhood<UnsignedShortType>(spot2, img2);
		SpotNeighborhoodCursor<UnsignedShortType> cursor2 = neighborhood2.cursor();
		while(cursor2.hasNext()) {
			cursor2.next().set((int) cursor2.getDistanceSquared());
		}
		System.out.println("Finished");
		ImageJFunctions.wrap(img2, "3D").show();
	
	}

}
