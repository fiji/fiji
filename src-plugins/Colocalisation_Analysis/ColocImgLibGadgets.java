import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.container.ContainerFactory;
import ij.measure.Measurements;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColocImgLibGadgets<T extends RealType<T>> implements PlugIn {

  protected Image<T> img1, img2;

  public void run(String arg) {
	ImagePlus imp1 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/red.tif");
	img1 = ImagePlusAdapter.wrap(imp1);
	ImagePlus imp2 = IJ.openImage("/Users/dan/Documents/Dresden/ipf/colocPluginDesign/green.tif");
	img2 = ImagePlusAdapter.wrap(imp2);

	double person = calculatePerson();

	Image<T> ranImg = generateRandomImageStack(img1, new int[] {2,2,1});
  }

  /**
   * To randomize blockwise we enumerate the blocks, shuffle that list and
   * write the data to their new position based on the shuffled list.
   */
  protected Image<T> generateRandomImageStack(Image<T> img, int[] blockDimensions) {
	int numberOfDimensions = Math.min(img.getNumDimensions(), blockDimensions.length);
	int numberOfBlocks = 0;
	int[] numberOfBlocksPerDimension = new int[numberOfDimensions];

	for (int i = 0 ; i<numberOfDimensions; i++){
		if (img.getDimension(i) % blockDimensions[i] != 0){
			System.out.println("sorry, for now image dims must be divisable by block size");
			return null;
		}
		numberOfBlocksPerDimension[i] = img.getDimension(i) / blockDimensions[i];
		numberOfBlocks *= numberOfBlocksPerDimension[i];
	}
	List<Integer> allTheBlocks = new ArrayList<Integer>(numberOfBlocks);
	for (int i = 0; i<numberOfBlocks; i++){
		allTheBlocks.add(new Integer(i));
	}
	Collections.shuffle(allTheBlocks, new Random());
	Cursor<T> cursor = img.createCursor();

	// create factories for new image stack
	ContainerFactory containerFactory = new ImagePlusContainerFactory();
	ImageFactory<T> imgFactory = new ImageFactory<T>(cursor.getType(), containerFactory);

	// crete a new stack for the random images
	Image<T> randomStack = imgFactory.createImage(img.getDimensions());

	// iterate over image data
	while (cursor.hasNext()) {
		cursor.fwd();
		T type = cursor.getType();
		// type.getRealDouble();
	}

	cursor.close();

	return randomStack;
  }

  protected double calculatePerson() {
	Cursor<T> cursor1 = img1.createCursor();
	Cursor<T> cursor2 = img2.createCursor();

	double mean1 = getImageMean(img1);
	double mean2 = getImageMean(img2);

	// Do some rather simple performance testing
	long startTime = System.currentTimeMillis();

	double pearson = calculatePerson(cursor1, mean1, cursor2, mean2);

	// End performance testing
	long finishTime = System.currentTimeMillis();
	long elapsed = finishTime - startTime;

	// close the cursors
	cursor1.close();
	cursor2.close();

	// print some output
	IJ.write("mean of ch1: " + mean1 + " " + "mean of ch2: " + mean2);
	IJ.write("Pearson's Coefficient " + pearson);
	IJ.write("That took: " + elapsed + " ms");

	return pearson;
  }

  protected double calculatePerson(Cursor<T> cursor1, double mean1, Cursor<T> cursor2, double mean2) {
	double pearsonDenominator = 0;
	double ch1diffSquaredSum = 0;
	double ch2diffSquaredSum = 0;
	while (cursor1.hasNext() && cursor2.hasNext()) {
		cursor1.fwd();
		cursor2.fwd();
		T type1 = cursor1.getType();
		double ch1diff = type1.getRealDouble() - mean1;
		T type2 = cursor2.getType();
		double ch2diff = type2.getRealDouble() - mean2;
		pearsonDenominator += ch1diff*ch2diff;
		ch1diffSquaredSum += (ch1diff*ch1diff);
		ch2diffSquaredSum += (ch2diff*ch2diff);
	}
	double pearsonNumerator = Math.sqrt(ch1diffSquaredSum * ch2diffSquaredSum);
	return pearsonDenominator / pearsonNumerator;
  }

  protected double getImageMean(Image<T> img) {
	  double sum = 0;
	  Cursor<T> cursor = img.createCursor();
	  while (cursor.hasNext()) {
		  cursor.fwd();
		  T type = cursor.getType();
		  sum += type.getRealDouble();
	  }
	  cursor.close();
	  return sum / img.getNumPixels();
  }


}
