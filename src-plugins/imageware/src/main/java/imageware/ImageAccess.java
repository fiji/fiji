package imageware;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
* ImageAccess is an interface layer to facilitate the access
* to the pixels of ImageJ images. 
* Methods of ImageAccess provides an easy and robust way to 
* access to the pixels of images.
*
* The data are stored in an double array. 
* Many methods get/put allows to access to the data. If the
* user try to access outside of the image, the mirror boundary
* conditions are applied. 
* 
* This version of ImageAccess is based on the imageware library.
*
* @author	Daniel Sage
*			Biomedical Imaging Group
*			Swiss Federal Institute of Technology Lausanne
*			EPFL, CH-1015 Lausanne, Switzerland
*/

public class ImageAccess extends Object {

	public static final int PATTERN_SQUARE_3x3 = 0;
	public static final int PATTERN_CROSS_3x3  = 1;

	private ImageWare imageware = null;
	private int nx = 0;
	private int ny = 0;
	
	/**
	* Creates a new ImageAccess object from a 2D double array of pixels.
	* The size of the array determines the size of the image.
	*
	* @param array    an array of pixel (2D)
	*/
	public ImageAccess(double[][] array) {
		if (array == null) 
			throw new 
				ArrayStoreException("Constructor: array == null.");
		imageware = Builder.create(array);
		this.nx = imageware.getSizeX();
		this.ny = imageware.getSizeY();
	}


	/**
	* Creates a new object of the class ImageAccess from an 
	* ImageProcessor object.
	*
	* ImageProcessor object contains the image data, the size and 
	* the type of the image. The ImageProcessor is provided by ImageJ,
	* it should by a 8-bit, 16-bit. 
	*
	* @param ip    an ImageProcessor object provided by ImageJ
	*/
	public ImageAccess(ImageProcessor ip) {
		if (ip == null) 
			throw new 
				ArrayStoreException("Constructor: ImageProcessor == null.");
		ImagePlus imp = new ImagePlus("", ip);
		if (ip instanceof ByteProcessor)
			imageware = Builder.create(imp, ImageWare.DOUBLE);
		else if (ip instanceof ShortProcessor)
			imageware = Builder.create(imp, ImageWare.DOUBLE);
		else if (ip instanceof FloatProcessor)
			imageware = Builder.create(imp, ImageWare.DOUBLE);
		this.nx = imageware.getSizeX();
		this.ny = imageware.getSizeY();
	}


	/**
	* Creates a new object of the class ImageAccess from an 
	* ColorProcessor object.
	*
	* ImageProcessor object contains the image data, the size and 
	* the type of the image. The ColorProcessor is provided by ImageJ,
	* The ImageAccess contains one plane (red, green or blue) selected
	* with the colorPlane parameter.
	*
	* @param cp    			an ColorProcessor object
	* @param colorPlane   	index of the color plane 0, 1 or 2
	*/
	public ImageAccess(ColorProcessor cp, int colorPlane) {
		if (cp == null) 
			throw new 
				ArrayStoreException("Constructor: ColorProcessor == null.");
		if (colorPlane < 0)
			throw new 
				ArrayStoreException("Constructor: colorPlane < 0.");
		if (colorPlane > 2)
			throw new 
				ArrayStoreException("Constructor: colorPlane > 2.");
		this.nx = cp.getWidth();
		this.ny = cp.getHeight();
		ImagePlus imp = new ImagePlus("", cp);
		imageware = new DoubleSet(imp.getStack(), (byte)colorPlane);
	}

	/**
	* Creates a new object of the class tImageAccess.
	*
	* The size of the image are given as parameter.
	* The data pixels are empty and are not initialized.
	*
	* @param nx       	the size of the image along the X-axis
	* @param ny       	the size of the image along the Y-axis
	*/
	public ImageAccess(int nx, int ny) {
		imageware = new DoubleSet(nx, ny, 1);
		this.nx = nx;
		this.ny = ny;
	}
	
	/**
	* Return the imageware of the image.
	*
	* @return     	the imageware object
	*/
	public ImageWare getDataset() {
		return imageware;
	}

	/**
	* Return the width of the image.
	*
	* @return     	the image width
	*/
	public int getWidth() {
		return nx;
	}

	/**
	* Return the height of the image.
	*
	* @return     	the image height
	*/
	public int getHeight() {
		return ny;
	}

	/**
	* Return the maximum value of ImageAccess.
	*
	* @return     	the maximum value
	*/
	public double getMaximum() {
		return imageware.getMaximum();
	}

	/**
	* Return the minimum value of ImageAccess.
	*
	* @return     	the minimum value
	*/
	public double getMinimum() {
		return imageware.getMinimum();
	}

	/**
	* Return the mean value of ImageAccess.
	*
	* @return     	the mean value
	*/
	public double getMean() {
		return imageware.getMean();
	}

	/**
	* Returns a copy of the pixel data organize in a
	* 2D array.
	*
	* @return     	the 2D double array
	*/
	public double[][] getArrayPixels() {
		double[][] array = new double[nx][ny];
		imageware.getXY(0, 0, 0, array);
		return  array;
	}

	/**
	* Returns a reference to the pixel data in double (1D).
	*
	* @return     	the 1D double array
	*/
	public double[] getPixels() {
		return imageware.getSliceDouble(0);
	}


	/**
	* Create a FloatProcessor from the pixel data.
	*
	* @return     	the FloatProcessor
	*/
	public FloatProcessor createFloatProcessor()  {
		FloatProcessor fp = new  FloatProcessor(nx, ny) ;
		double[] pixels = getPixels();
		int size = pixels.length;
		float[] fsrc = new float[size];
		for (int k=0; k<size; k++)
			fsrc[k] = (float)(pixels[k]);
	 	fp.setPixels(fsrc);
	 	return fp;
	}


	/**
	* Create a ByteProcessor from the pixel data.
	*
	* @return     	the ByteProcessor
	*/
	public ByteProcessor createByteProcessor() {
		ByteProcessor bp = new  ByteProcessor(nx, ny) ;
		double[] pixels = getPixels();
		int size = pixels.length;
		byte[] bsrc = new byte[size];
		double p;
		for (int k=0; k<size; k++) {
				p = pixels[k];
				if (p < 0)
					p = 0.0;
				if (p > 255.0)
					p = 255.0;
				bsrc[k] = (byte)p;
		}
	 	bp.setPixels(bsrc);
	 	return bp;
	}

	/**
	* Create a new ImageAccess object by duplication of the current the 
	* ImageAccess object.
	*
	* @return   a new ImageAccess object
	**/
	public ImageAccess duplicate() {
		double[][] array = new double[nx][ny];
		imageware.getXY(0, 0, 0, array);
		ImageAccess ia = new ImageAccess(array);
		return ia;
	}

	/**
	* An ImageAccess object calls this method for getting
	* the gray level of a selected pixel.
	*
	* Mirror border conditions are applied.
	*
	* @param x		input, the integer x-coordinate of a pixel
	* @param y		input, the integer y-coordinate of a pixel
	* @return     	the gray level of the pixel (double) 
	*/
	public double getPixel(int x, int y) {
		return imageware.getPixel(x, y, 0, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting
	* the gray level of a selected pixel using a bilinear interpolation.
	* The coordinates can be given in double and the 
	* bilinear interpolation is applied the find the gray level.
	*
	* Mirror border conditions are applied.
	*
	* @param x		input, the double x-coordinate of a pixel
	* @param y		input, the double y-coordinate of a pixel
	* @return     	the gray level of the pixel (double) 
	*/
	public double getInterpolatedPixel(double x, double y) {	
		return imageware.getInterpolatedPixel(x, y, 0, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a 
	* whole column of the image.
	*
	* The column should already created with the correct size [ny].
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param column     output, an array of the type double
	*/
	public void getColumn(int x, double[] column) {
		if (x < 0) 
			throw new 
				IndexOutOfBoundsException("getColumn: x < 0.");
		if (x >= nx)
			throw new 
				IndexOutOfBoundsException("getColumn: x >= nx.");
		if (column == null)
			throw new 
				ArrayStoreException("getColumn: column == null.");

		if (column.length != ny)
			throw new 
				ArrayStoreException("getColumn: column.length != ny.");
		imageware.getBlockY(x, 0, 0, column, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a part 
	* of column.
	* The starting point is given by the y parameter and the ending
	* determine by the size of the column parameter. The column 
	* parameter should already created.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param y       	input, starting point
	* @param column     output, an array of the type double
	*/
	public void getColumn(int x, int y, double[] column) {
		if ( x < 0)
			throw new 
				IndexOutOfBoundsException("getColumn: x < 0.");				
		if (x >= nx)
			throw new 
				IndexOutOfBoundsException("getColumn: x >= nx.");
		if (column == null)
			throw new 
				ArrayStoreException("getColumn: column == null.");		
		imageware.getBlockY(x, y, 0, column, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a 
	* whole row of the image.
	*
	* The row should already created with the correct size [nx].
	*
	* @param y       	input, the integer y-coordinate of a row
	* @param row        output, an array of the type double
	*/
	public void getRow(int y, double[] row) {
		if ( y < 0)
	    	throw new 
	    		IndexOutOfBoundsException("getRow: y < 0.");
		if (y >= ny)
	    	throw new 
	    		IndexOutOfBoundsException("getRow: y >= ny.");
		if (row == null)
			throw new 
				ArrayStoreException("getColumn: row == null.");
		if (row.length != nx)
			throw new 
				ArrayStoreException("getColumn: row.length != nx.");
		imageware.getBlockX(0, y, 0, row, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a part 
	* of row.
	* The starting point is given by the y parameter and the ending
	* determine by the size of the row parameter. The row 
	* parameter should already created.
	*
	* @param x       	input, starting point
	* @param y       	input, the integer y-coordinate of a row
	* @param row        output, an array of the type double
	*/
	public void getRow(int x, int y, double[] row) {
		if (y < 0)
			throw new 
				IndexOutOfBoundsException("getRow: y < 0.");		
		if (y >= ny)
			throw new 
				IndexOutOfBoundsException("getRow: y >= ny.");
		if (row == null)
			throw new 
				ArrayStoreException("getRow: row == null.");
		imageware.getBlockX(x, y, 0, row, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a neighborhood
	* arround a pixel position.
	*
	* The neigh parameter should already created. The size of the array 
	* determines the neighborhood block.
	*
	* <br>Mirror border conditions are applied.
	* <br>
	* <br>The pixel value of (x-n/2, y-n/2) is put into neigh[0][0]
	* <br>...
	* <br>The pixel value of (x+n/2, y+n/2) is put into neigh[n-1][n-1]
	* <br>
	* <br>For example if neigh is a double[4][4]:
	* <br>The pixel value of (x-1, y-1) is put into neigh[0][0]
	* <br>The pixel value of (x  , y  ) is put into neigh[1][1]
	* <br>The pixel value of (x+1, y+1) is put into neigh[2][2]
	* <br>The pixel value of (x+2, y+2) is put into neigh[3][3]
	* <br>...
	* <br>For example if neigh is a double[5][5]:
	* <br>The pixel value of (x-2, y-2) is put into neigh[0][0]
	* <br>The pixel value of (x-1, y-1) is put into neigh[1][1]
	* <br>The pixel value of (x  , y  ) is put into neigh[2][2]
	* <br>The pixel value of (x+1, y+1) is put into neigh[3][3]
	* <br>The pixel value of (x+2, y+2) is put into neigh[4][4]

	* @param x		the integer x-coordinate of a selected central pixel
	* @param y		the integer y-coordinate of a selected central pixel
	* @param neigh	output, a 2D array s
	*/
	public void getNeighborhood(int x, int y, double neigh[][]) {
		imageware.getNeighborhoodXY(x, y, 0, neigh, ImageWare.MIRROR);
	}

	/**
	* An ImageAccess object calls this method for getting a
	* neighborhood of a predefined pattern around a selected pixel (x,y).
	* <br>The available patterns are:
	* <br>- a 3*3 block: PATTERN_SQUARE_3x3 (8-connect)
	* <br>- a 3*3 cross: PATTERN_CROSS_3x3  (4-connect)
	* <br>
	* <br>Mirror border conditions are applied.
	* <br>The pixel is arranged in a 1D array according the following rules:
	* <br>
	* <br>If the pattern is PATTERN_SQUARE_3x3  (8-connect)
	* <br>The pixel value of (x-1, y-1) are put into neigh[0]
	* <br>The pixel value of (x  , y-1) are put into neigh[1]
	* <br>The pixel value of (x+1, y-1) are put into neigh[2]
	* <br>The pixel value of (x-1, y  ) are put into neigh[3]
	* <br>The pixel value of (x  , y  ) are put into neigh[4]
	* <br>The pixel value of (x+1, y  ) are put into neigh[5]
	* <br>The pixel value of (x-1, y+1) are put into neigh[6]
	* <br>The pixel value of (x  , y+1) are put into neigh[7]
	* <br>The pixel value of (x+1, y+1) are put into neigh[8]
	* <br>
	* <br>If the pattern is PATTERN_CROSS_3x3   (4-connect)
	* <br>The pixel value of (x  , y-1) are put into neigh[0]
	* <br>The pixel value of (x-1, y  ) are put into neigh[1]
	* <br>The pixel value of (x  , y  ) are put into neigh[2]
	* <br>The pixel value of (x+1, y  ) are put into neigh[3]
	* <br>The pixel value of (x  , y+1) are put into neigh[4]
	* <br>
	* <br>The neigh should already created as a double array of 9 elements
	* for the PATTERN_SQUARE_3x3 or 5 elements for the PATTERN_CROSS_3x3.
	* 
	* @param x			x-coordinate of a selected central pixel
	* @param y			y-coordinate of a selected central pixel
	* @param neigh		output, an array consisting of 9 or 5 elements
	* @param pattern	PATTERN_SQUARE_3x3 or PATTERN_CROSS_3x3.
	*/
	public void getPattern(int x, int y, double neigh[], int pattern) {
		if (neigh == null)
			throw new 
				ArrayStoreException("getPattern: neigh == null.");
		
		double block[][] = new double[3][3];
		imageware.getNeighborhoodXY(x, y, 0, block, ImageWare.MIRROR);
		
		switch(pattern) {
			case PATTERN_SQUARE_3x3:
				if (neigh.length != 9)
					throw new 
						ArrayStoreException("getPattern: neigh.length != 9.");
				neigh[0] = block[0][0];
				neigh[1] = block[1][0];
				neigh[2] = block[2][0];
				neigh[3] = block[0][1];
				neigh[4] = block[1][1];
				neigh[5] = block[2][1];
				neigh[6] = block[0][2];
				neigh[7] = block[1][2];
				neigh[8] = block[2][2];
				break;
				
			case PATTERN_CROSS_3x3:
				if (neigh.length != 5)
					throw new 
						ArrayStoreException("getPattern: neigh.length != 5");
				neigh[0] = block[1][0];
				neigh[1] = block[0][1];
				neigh[2] = block[1][1];
				neigh[3] = block[2][1];
				neigh[4] = block[0][1];
				break;
			
			default:
				throw new 
					ArrayStoreException("getPattern: unexpected pattern.");
		}
	}
	
	/**
	* An ImageAccess object calls this method to get a sub-image 
	* with the upper left corner in the coordinate (x,y).
	*
	* The sub-image ouptut should be already created.
	*
	* @param x      x-coordinate in the source image
	* @param y      y-coordinate in the source image
	* @param output an ImageAccess object with the sub-image;
	*/
	public void getSubImage(int x, int y, ImageAccess output)
	{
	    if (output == null)
			throw new 
				ArrayStoreException("getSubImage: output == null.");
	    if (x<0)
			throw new 
				ArrayStoreException("getSubImage: Incompatible image size");
	    if (y<0)
			throw new 
				ArrayStoreException("getSubImage: Incompatible image size");
	    if (x>=nx)
			throw new 
				ArrayStoreException("getSubImage: Incompatible image size");
	    if (y>=ny)
			throw new 
				ArrayStoreException("getSubImage: Incompatible image size");
	 	int nxcopy = output.getWidth();
	 	int nycopy = output.getHeight();
	 	double[][] neigh = new double[nxcopy][nycopy];
	 	imageware.getBlockXY(x, y, 0, neigh, ImageWare.MIRROR);
	 	output.putArrayPixels(neigh);
	}

	/**
	* An ImageAccess object calls this method in order a value
	* of the gray level to be put to a position inside it
	* given by the coordinates.
	*
	* @param x		input, the integer x-coordinate of a pixel
	* @param y		input, the integer y-coordinate of a pixel
	* @param value	input, a value of the gray level of the type double
	*/
	public void putPixel(int x, int y, double value) {
	   	if (x < 0)
	    	throw new 
	    		IndexOutOfBoundsException("putPixel: x < 0");
		if (x >= nx)
	    	throw new 
	    		IndexOutOfBoundsException("putPixel: x >= nx");
		if (y < 0)
	    	throw new 
	    		IndexOutOfBoundsException("putPixel:  y < 0");
		if (y >= ny)
	    	throw new 
	    		IndexOutOfBoundsException("putPixel:  y >= ny");
		imageware.putPixel(x, y, 0, value);
	}

	/**
	* An ImageAccess object calls this method to put a whole 
	* column in a specified position into the image.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param column     input, an array of the type double
	*/
	public void putColumn (int x, double[] column) {
		if (x < 0) 
			throw new 
				IndexOutOfBoundsException("putColumn: x < 0.");
		if (x >= nx)
			throw new 
				IndexOutOfBoundsException("putColumn: x >= nx.");
		if (column == null)
			throw new 
				ArrayStoreException("putColumn: column == null.");
		if (column.length != ny)
			throw new 
				ArrayStoreException("putColumn: column.length != ny.");
		imageware.putBoundedY(x, 0, 0, column);
	}

	/**
	* An ImageAccess object calls this method to put a part of column
	* into the image. The starting poisition in given by y and the ending
	* position is determined by the size of the column array.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param y       	input, the integer y-coordinate of a column
	* @param column     input, an array of the type double
	*/
	public void putColumn(int x, int y, double[] column) {
		if (x < 0) 
			throw new 
				IndexOutOfBoundsException("putColumn: x < 0.");
		if (x >= nx)
			throw new 
				IndexOutOfBoundsException("putColumn: x >= nx.");
		if (column == null)
			throw new 
				ArrayStoreException("putColumn: column == null.");
		imageware.putBoundedY(x, y, 0, column);
	}

	/**
	* An ImageAccess object calls this method to put a whole 
	* row in a specified position into the image.
	*
	* @param y       input, the integer x-coordinate of a column
	* @param row     input, an array of the type double
	*/
	public void putRow(int y, double[] row) {
		if (y < 0) 
			throw new 
				IndexOutOfBoundsException("putRow: y < 0.");
		if (y >= ny)
			throw new 
				IndexOutOfBoundsException("putRow: y >= ny.");
		if (row == null)
			throw new 
				ArrayStoreException("putRow: row == null.");
		if (row.length != nx)
			throw new 
				ArrayStoreException("putRow: row.length != nx.");
		imageware.putBoundedX(0, y, 0, row);
	}

	/**
	* An ImageAccess object calls this method to put a part of row
	* into the image. The starting poisition in given by x and the ending
	* position is determined by the size of the row array.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param y       	input, the integer y-coordinate of a column
	* @param row		input, an array of the type double
	*/
	public void putRow(int x, int y, double[] row) {
		if (y < 0) 
			throw new 
				IndexOutOfBoundsException("putRow: y < 0.");
		if (y >= ny)
			throw new 
				IndexOutOfBoundsException("putRow: y >= ny.");
		if (row == null)
			throw new 
				ArrayStoreException("putRow: row == null.");
		imageware.putBoundedX(x, y, 0, row);
	}

	/**
	* An ImageAccess object calls this method in order to put 
	* an 2D array of double in an ImageAccess.
	*
	* @param array		input, the double array
	*/
	public void putArrayPixels(double[][] array) {
	    if (array == null)
			throw new 
				IndexOutOfBoundsException("putArrayPixels: array == null.");
		imageware.putBoundedXY(0, 0, 0, array);
	}

	/**
	* An ImageAccess object calls this method to put a sub-image 
	* with the upper left corner in the coordinate (x,y).
	*
	* The sub-image input should be already created.
	*
	* @param x      x-coordinate in the source image
	* @param y      y-coordinate in the source image
	* @param input  an ImageAccess object that we want to put;
	*/
	public void putSubImage(int x, int y, ImageAccess input) {
	    if (input == null)
			throw new 
				ArrayStoreException("putSubImage: input == null.");
	    if (x < 0 )
			throw new 
				IndexOutOfBoundsException("putSubImage: x < 0.");
	    if (y < 0)
			throw new 
				IndexOutOfBoundsException("putSubImage: y < 0.");
	    if (x >= nx)
			throw new 
				IndexOutOfBoundsException("putSubImage: x >= nx.");
	    if (y >= ny)
			throw new 
				IndexOutOfBoundsException("putSubImage: y >= ny.");
	 	
	 	double[][] sub = input.getArrayPixels();
	 	imageware.putBoundedXY(x, y, 0, sub);
	}

	/**
	* An ImageAccess object calls this method to set a constant
	* value to all pixels of the image.
	*
	* @param constant a constant value 
	*/
	public void setConstant(double constant) {
		imageware.fillConstant(constant);
	}

	/**
	* Stretches the contrast inside an image so that the gray levels 
	* are in the range 0 to 255.
	*/
	public void normalizeContrast() {
		imageware.rescale();
	}


	/**
	* Display an image at a specific position (x, y).
	*
	* @param title  a string for the title
	* @param loc   	Point for the location
	*/
	public void show(String title, java.awt.Point loc) {
		FloatProcessor fp = createFloatProcessor();
		fp.resetMinAndMax();
		ImagePlus impResult = new ImagePlus(title, fp);
	    ij.gui.ImageWindow window = impResult.getWindow();
	    window.setLocation(loc.x, loc.y);
		impResult.show();
	}


	/**
	* Display an image.
	*
	* @param title   a string for the title of the window
	*/
	public void show(String title) {
		imageware.show(title);
	}

	/**
	* Compute the absolute value.
	*/
	public void abs()  {	
		imageware.abs();
	}

	/**
	* Compute the square root of an ImageAccess.
	*/
	public void sqrt() {
		imageware.sqrt();
	}

	/**
	* Raised an ImageAccess object to the power a.
	*
	* @param a 	input
	*/
	public void pow(final double a) {
		imageware.pow(a);
	}

	/**
	* An ImageAccess object calls this method for adding
	* a constant to each pixel. 
	*
	* @param constant   a constant to be added
	*/
	public void add(double constant){
		imageware.add(constant);
	}

	/**
	* An ImageAccess object calls this method for multiplying
	* a constant to each pixel. 
	*
	* @param constant   a constant to be multiplied
	*/
	public void multiply(final double constant) {
		imageware.multiply(constant);
	}

	/**
	* An ImageAccess object calls this method for adding
	* a constant to each pixel. 
	*
	* @param constant   a constant to be added
	*/
	public void subtract(final double constant) {
		imageware.add(-constant);
	}

	/**
	* An ImageAccess object calls this method for dividing
	* a constant to each pixel. 
	*
	* @param constant   a constant to be multiplied
	*/
	public void divide(final double constant) {
		if (constant == 0.0) 
			throw new 
				ArrayStoreException("divide: Divide by 0");
		imageware.multiply(1.0/constant);
	}

	/**
	* An ImageAccess object calls this method for adding
	* two ImageAccess objects.
	*
	* [this = im1 + im2]
	*
	* The resulting ImageAccess and the two operands should have 
	* the same size.
	*
	* @param im1		an ImageAccess object to be added
	* @param im2		an ImageAccess object to be added
	*/
	public void add(ImageAccess im1, ImageAccess im2) {
		if (im1.getWidth() != nx)
			throw new 
				ArrayStoreException("add: incompatible size.");
				
		if (im1.getHeight() != ny)
			throw new 
				ArrayStoreException("add: incompatible size.");
				
		if (im2.getWidth() != nx)
			throw new 
				ArrayStoreException("add: incompatible size.");

		if (im2.getHeight() != ny)
			throw new 
				ArrayStoreException("add: incompatible size.");
		imageware.copy(im1.getDataset());
		imageware.add(im2.getDataset());
	}

	/**
	* An ImageAccess object calls this method for multiplying
	* two ImageAccess objects.
	*
	* The resulting ImageAccess and the two operands should have 
	* the same size.
	*
	* [this = im1 * im2]
	*
	* @param im1		an ImageAccess object to be multiplied
	* @param im2		an ImageAccess object to be multiplied
	*/
	public void multiply(ImageAccess im1,ImageAccess im2) {
		if (im1.getWidth() != nx)
			throw new 
				ArrayStoreException("multiply: incompatible size.");
				
		if (im1.getHeight() != ny)
			throw new 
				ArrayStoreException("multiply: incompatible size.");
				
		if (im2.getWidth() != nx)
			throw new 
				ArrayStoreException("multiply: incompatible size.");

		if (im2.getHeight() != ny)
			throw new 
				ArrayStoreException("multiply: incompatible size.");
		
		imageware.copy(im1.getDataset());
		imageware.multiply(im2.getDataset());
	}

	/**
	* An ImageAccess object calls this method for subtracting
	* two ImageAccess objects.
	*
	* The resulting ImageAccess and the two operands should have 
	* the same size.
	*
	* [this = im1 - im2]
	*
	* @param im1		an ImageAccess object to be subtracted
	* @param im2		an ImageAccess object to be subtracted
	*/
	public void subtract(ImageAccess im1,ImageAccess im2) {
		if (im1.getWidth() != nx)
			throw new 
				ArrayStoreException("subtract: incompatible size.");
				
		if (im1.getHeight() != ny)
			throw new 
				ArrayStoreException("subtract: incompatible size.");
				
		if (im2.getWidth() != nx)
			throw new 
				ArrayStoreException("subtract: incompatible size.");

		if (im2.getHeight() != ny)
			throw new 
				ArrayStoreException("subtract: incompatible size.");
		
		imageware.copy(im1.getDataset());
		imageware.subtract(im2.getDataset());
	}

	/**
	* An ImageAccess object calls this method for dividing
	* two ImageAccess objects.
	*
	* [this = im1 / im2]
	*
	* The resulting ImageAccess and the two operands should have 
	* the same size.
	*
	* @param im1		numerator
	* @param im2		denominator
	*/
	public void divide(ImageAccess im1,ImageAccess im2) {
		if (im1.getWidth() != nx)
			throw new 
				ArrayStoreException("divide: incompatible size.");
				
		if (im1.getHeight() != ny)
			throw new 
				ArrayStoreException("divide: incompatible size.");
				
		if (im2.getWidth() != nx)
			throw new 
				ArrayStoreException("divide: incompatible size.");

		if (im2.getHeight() != ny)
			throw new 
				ArrayStoreException("divide: incompatible size.");
				
		imageware.copy(im1.getDataset());
		imageware.divide(im2.getDataset());
	}

} // end of class ImageAccess

