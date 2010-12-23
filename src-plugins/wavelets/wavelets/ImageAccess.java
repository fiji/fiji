package wavelets;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * ImageAccess is an interface layer to facilitate the access to the pixels of 
 * ImageJ images. Methods of ImageAccess provides an easy and robust way to 
 * access to the pixels of images. The data are stored in a double array. 
 * Many methods get/put allows to access to the data. If the user try to access 
 * outside of the image, the mirror boundary conditions are applied. 
 * 
 * @author
 * <p style="background-color:#EEEEEE; border-top:1px solid #CCCCCC; border-bottom:1px solid #CCCCCC"">
 * Daniel Sage<br>
 * <a href="http://bigwww.epfl.ch">Biomedical Imaging Group</a> (BIG), 
 * Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland<br>
 * More information: http://bigwww.epfl.ch/teaching/iplabsite/</p>
 * <p><b>Reference</b>: D. Sage, M. Unser, 
 * &#34;<a href="http://bigwww.epfl.ch/publications/sage0303.html">
 * Teaching Image-Processing Programming in Java</a>,&#34; IEEE Signal 
 * Processing Magazine, vol. 20, no. 6, pp. 43-52, November 2003.</p>
 * 
 * <p>You'll be free to use this software for research purposes, but you should
 *  not redistribute it without our consent. In addition, we expect you to 
 *  include a citation or acknowledgement whenever you present or publish 
 *  results that are based on it.</p>
 *  
 *  @version
 *  11 July 2009
 */

public class ImageAccess {
	public static final int PATTERN_SQUARE_3x3 = 0;
	public static final int PATTERN_CROSS_3x3  = 1;

	private double 	pixels[] = null;		// store the pixel data
	private int 	nx = 0;					// size in X axis
	private int 	ny = 0;					// size in Y axis
	private int 	size = 0;				// size = nx*ny

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
		this.ny = array[0].length;
		this.nx = array.length;
		this.size = nx*ny;
		pixels = new double[size];
		int k = 0;
		for (int j=0; j<ny; j++)
		for (int i=0; i<nx; i++)
			pixels[k++] = array[i][j];
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
		nx = ip.getWidth();
		ny = ip.getHeight();
		size = nx*ny;
		pixels = new double[size];
		if (ip.getPixels() instanceof byte[]) {
			byte[] bsrc = (byte[])ip.getPixels();
			for (int k=0; k<size; k++)
				pixels[k] = (double)(bsrc[k] & 0xFF);
		
		}	
		else if (ip.getPixels() instanceof short[]) {
			 short[] ssrc = (short[])ip.getPixels();
			 for (int k=0; k<size; k++)
				pixels[k] = (double)(ssrc[k] & 0xFFFF);
		}	
		else if (ip.getPixels() instanceof float[]) {
			 float[] fsrc = (float[])ip.getPixels();
			 for (int k=0; k<size; k++)
				pixels[k] = (double)fsrc[k];
		}
		else  {
			throw new 
				ArrayStoreException("Constructor: Unexpected image type.");
		}
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
		nx = cp.getWidth();
		ny = cp.getHeight();
		size = nx*ny;
		pixels = new double[size];
		byte[] r = new byte[size];
		byte[] g = new byte[size];
		byte[] b = new byte[size];
		cp.getRGB(r, g, b);
		if (colorPlane == 0)
			for (int k=0; k<size; k++)
				pixels[k] = (double)(r[k] & 0xFF);
		else if (colorPlane == 1)
			for (int k=0; k<size; k++)
				pixels[k] = (double)(g[k] & 0xFF);
		else if (colorPlane == 2)
			for (int k=0; k<size; k++)
				pixels[k] = (double)(b[k] & 0xFF);
	}

	/**
	* Creates a new object of the class ImageAccess.
	*
	* The size of the image are given as parameter.
	* The data pixels are empty and are not initialized.
	*
	* @param nx       	the size of the image along the X-axis
	* @param ny       	the size of the image along the Y-axis
	*/
	public ImageAccess(int nx, int ny) {
		if (nx < 1)
			throw new 
				ArrayStoreException("Constructor: nx < 1.");
		if (ny < 1)
			throw new 
				ArrayStoreException("Constructor: ny < 1.");
		this.nx = nx;
		this.ny = ny;
		size = nx*ny;
		pixels = new double[size];
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
		double maxi = pixels[0];
		for (int i=1; i<size; i++)
			if (pixels[i] > maxi) 
				maxi = pixels[i];
		return maxi;
	}

	/**
	* Return the minimum value of ImageAccess.
	*
	* @return     	the minimum value
	*/
	public double getMinimum() {
		double mini = pixels[0];
		for (int i=1; i<size; i++)
			if (pixels[i] < mini) 
				mini = pixels[i];
		return mini;
	}


	/**
	* Return the mean value of ImageAccess.
	*
	* @return     	the mean value
	*/
	public double getMean() {
		double mean=0.0;
		for ( int i=0; i<size; i++)
			mean += pixels[i];
		mean /= (double)(size);
		return mean;
	}

	/**
	* Returns a copy of the pixel data organize in a
	* 2D array.
	*
	* @return     	the 2D double array
	*/
	public double[][] getArrayPixels() {
		double[][] array = new double[nx][ny];
		int k = 0;
		for (int j=0; j<ny; j++)
			for (int i=0; i<nx; i++)
				array[i][j] = pixels[k++];
		return  array;
	}

	/**
	* Returns a reference to the pixel data in double (1D).
	*
	* @return     	the 1D double array
	*/
	public double[] getPixels() {
		return pixels;
	}

	/**
	* Create a FloatProcessor from the pixel data.
	* The double values of the pixel are simply casted in float.
	*
	* @return     	the FloatProcessor
	*/
	public FloatProcessor createFloatProcessor() {
		FloatProcessor fp = new  FloatProcessor(nx, ny) ;
		float[] fsrc = new float[size];
		for (int k=0; k<size; k++)
			fsrc[k] = (float)(pixels[k]);
	 	fp.setPixels(fsrc);
	 	return fp;
	}

	/**
	* Create a ByteProcessor from the pixel data.
	* The double values of the pixel are clipped in the [0..255] range.
	*
	* @return     	the ByteProcessor
	*/
	public ByteProcessor createByteProcessor() {
		ByteProcessor bp = new  ByteProcessor(nx, ny) ;
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
		ImageAccess ia = new ImageAccess(nx, ny);
		for (int i=0; i<size; i++)
			ia.pixels[i] = this.pixels[i];
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
		int periodx = 2*nx-2;
	 	int periody = 2*ny-2;
		if (x<0) {			
			while (x<0) x += periodx;		// Periodize	
			if (x >= nx) x = periodx - x;	// Symmetrize
		}
		else if (x>=nx) {			
			while (x>=nx) x -= periodx;		// Periodize	
			if (x < 0) x = -x;				// Symmetrize
		}
		
		if (y<0) {			
			while (y<0) y += periody;		// Periodize	
			if (y>=ny)  y = periody - y;	// Symmetrize	
		}
		else if (y>=ny) {			
			while (y>=ny) y -= periody;		// Periodize	
			if (y < 0) y = -y;				// Symmetrize
	 	}
		return pixels[x+y*nx];
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
		if (Double.isNaN(x))
			return 0;
		if (Double.isNaN(y))
			return 0;
				
		if ( x < 0) {
	    	int periodx = 2*nx - 2;				
			while (x < 0) x += periodx;		// Periodize
			if (x >= nx)  x = periodx - x;	// Symmetrize
		}
		else if ( x >= nx) {
	    	int periodx = 2*nx - 2;				
			while (x >= nx) x -= periodx;	// Periodize
			if (x < 0)  x = - x;			// Symmetrize
		}

		if ( y < 0) {
	    	int periody = 2*ny - 2;				
			while (y < 0) y += periody;		// Periodize
			if (y >= ny)  y = periody - y;	// Symmetrize
		}
		else if ( y >= ny) {
	    	int periody = 2*ny - 2;				
			while (y >= ny) y -= periody;	// Periodize
			if (y < 0)  y = - y;			// Symmetrize
		}
		int i;
		if (x >= 0.0) 
			i = (int)x;
		else {
			final int iAdd = (int)x - 1;
			i = ((int)(x - (double)iAdd) + iAdd);
		}
		int j;
		if (y >= 0.0) 
			j = (int)y;
		else {
			final int iAdd = (int)y - 1;
			j = ((int)(y - (double)iAdd) + iAdd);
		}

		double dx = x - (double)i;
		double dy = y - (double)j;
	    int di;
		if(i >= nx-1)  
			di = -1;
		else 
			di = 1;
		int index =	i+j*nx;
	 	double v00 = pixels[index];
		double v10 = pixels[index+di];
		if(j>=ny-1)
			index -= nx;
		else
			index += nx;
		double v01 = pixels[index];
		double v11 = pixels[index+di];
		return (dx*(v11*dy-v10*(dy-1.0)) - (dx-1.0)*(v01*dy-v00*(dy-1.0)));
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
			throw new IndexOutOfBoundsException("getColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("getColumn: x >= nx.");
		if (column == null)
			throw new 
				ArrayStoreException("getColumn: column == null.");
		if (column.length != ny)
			throw new 
				ArrayStoreException("getColumn: column.length != ny.");
		for (int i=0; i<ny; i++) {
			column[i] = pixels[x];
			x += nx;
		}
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
			throw new IndexOutOfBoundsException("getColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("getColumn: x >= nx.");
		if (column == null)
			throw new ArrayStoreException("getColumn: column == null.");
		int by = column.length; 
	 	if (y >= 0)
	 	if (y < ny-by-1) {
			int index = y*nx + x;	
	 		for (int i = 0; i < by; i++) {
				column[i] = pixels[index];
				index+=nx;			
			}
			return;
		}
	    // Getting information outside of the image
		int yt[] = new int[by];
		for (int k = 0; k < by; k++) {
			int ya = y + k;
	    	int periody = 2*ny - 2;				
			while (ya < 0) ya += periody;	// Periodize
			while (ya >= ny)  {
				ya = periody - ya;			// Symmetrize
				if (ya < 0)  ya = - ya;
			}
			yt[k] = ya;
		}
	 	int index = 0;
	 	for (int i = 0; i < by; i++) {
	 	     index = yt[i]*nx+x;
			 column[i] = pixels[index];
		}
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
	    	throw new IndexOutOfBoundsException("getRow: y < 0.");
		if (y >= ny)
	    	throw new IndexOutOfBoundsException("getRow: y >= ny.");
		if (row == null)
			throw new 
				ArrayStoreException("getColumn: row == null.");
		if (row.length != nx)
			throw new 
				ArrayStoreException("getColumn: row.length != nx.");
		y *= nx;
		for (int i=0; i<nx; i++)
			row[i] = pixels[y++];
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
			throw new IndexOutOfBoundsException("getRow: y < 0.");
		if (y >= ny)
			throw new IndexOutOfBoundsException("getRow: y >= ny.");
		if (row == null)
			throw new ArrayStoreException("getRow: row == null.");
		int bx = row.length; 
	 	if (x >=0)
	 	if (x < nx-bx-1){
			int index = y*nx + x;	
	 		for (int i = 0; i < bx; i++) {
				row[i] = pixels[index++];			
			}
			return;
		}
	    int periodx = 2*nx - 2;	
		int xt[] = new int[bx];
		for (int k = 0; k < bx; k++) {
			int xa = x + k;		
			while (xa < 0) xa += periodx;		// Periodize
			while (xa >= nx) {
				xa = periodx - xa;				// Symmetrize
				if (xa < 0)  xa = - xa;
			}
			xt[k] = xa;
		}
	 	int somme = 0;
		int index = y*nx;
	 	for (int i = 0; i < bx; i++) {
	 		somme =index+xt[i];
			row[i] = pixels[somme];
		}
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
	* <br>The pixel value of (x, y+1) is put into neigh[1][2]
	* <br>The pixel value of (x+1, y-2) is put into neigh[2][0]
	* <br>...
	* <br>For example if neigh is a double[5][5]:
	* <br>The pixel value of (x-2, y-2) is put into neigh[0][0]
	* <br>The pixel value of (x-1, y-1) is put into neigh[1][1]
	* <br>The pixel value of (x  , y  ) is put into neigh[2][2]
	* <br>The pixel value of (x, y+1) is put into neigh[2][3]
	* <br>The pixel value of (x+2, y-2) is put into neigh[4][0]

	* @param x		the integer x-coordinate of a selected central pixel
	* @param y		the integer y-coordinate of a selected central pixel
	* @param neigh	output, a 2D array s
	*/
	public void getNeighborhood(int x, int y, double neigh[][]) {
		int bx=neigh.length;
		int by=neigh[0].length;
		int bx2 = (bx-1)/2;
		int by2 = (by-1)/2;
	 	if (x >= bx2)
	 	if (y >= by2)
	 	if (x < nx-bx2-1)
	 	if (y < ny-by2-1) { 
			int index = (y-by2)*nx + (x-bx2);
			for (int j = 0; j < by; j++) {
	 			for (int i = 0; i < bx; i++) {
					neigh[i][j] = pixels[index++];			
				}
				index += (nx - bx);
			}	
			return;
		}
		int xt[] = new int[bx];
		for (int k = 0; k < bx; k++) {
			int xa = x + k - bx2;
	    	int periodx = 2*nx - 2;				
			while (xa < 0) 
				xa += periodx;				// Periodize
			while (xa >= nx) {
				xa = periodx - xa;			// Symmetrize
				if (xa < 0)  xa = - xa;
			}
			xt[k] = xa;
		}
		int yt[] = new int[by];
		for (int k = 0; k < by; k++) {
			int ya = y + k - by2;
	    	int periody = 2*ny - 2;			
			while (ya < 0) ya += periody;	// Periodize
			while (ya >= ny)  {
				ya = periody - ya;			// Symmetrize
				if (ya < 0)  ya = - ya;
			}
			yt[k] = ya;
		}
	 	int sum=0;
	 	for (int j = 0; j < by; j++) {
			int index = yt[j]*nx;
	 		for (int i = 0; i < bx; i++) {
	 	        sum =index+xt[i];
				neigh[i][j] = pixels[sum];
			}
		}	
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
			throw new ArrayStoreException("getPattern: neigh == null.");
		switch(pattern) {
			case PATTERN_SQUARE_3x3:
				if (neigh.length != 9)
					throw new 
						ArrayStoreException("getPattern: neigh.length != 9.");
				getPatternSquare3x3(x, y, neigh);
				break;
			case PATTERN_CROSS_3x3:
				if (neigh.length != 5)
					throw new 
						ArrayStoreException("getPattern: neigh.length != 5");
				getPatternCross3x3(x, y, neigh);
				break;
			default:
				throw new ArrayStoreException("getPattern: unexpected pattern.");
		}
	}
	/**
	* An ImageAccess object calls this method for getting
	* a 3x3 neighborhood of 8-connected pixels around a selected pixel.
	*
	* @param x		input, the integer x-coordinate of a selected central pixel
	* @param y		input, the integer y-coordinate of a selected central pixel
	* @param neigh	output, an array consisting of 9 elements of the type double
	*/
	private void getPatternSquare3x3(int x, int y, double neigh[]) {
	 	if (x >= 1)
	 	if (y >= 1)
	 	if (x < nx-1)
	 	if (y < ny-1) { 
			int index = (y-1)*nx + (x-1);
			neigh[0] = pixels[index++];
			neigh[1] = pixels[index++];
			neigh[2] = pixels[index];
			index += (nx - 2);
			neigh[3] = pixels[index++];
			neigh[4] = pixels[index++];
			neigh[5] = pixels[index];
			index += (nx - 2);
			neigh[6] = pixels[index++];
			neigh[7] = pixels[index++];
			neigh[8] = pixels[index];
			return;
		}
		int x1 = x - 1;
		int x2 = x;
		int x3 = x + 1;
		int y1 = y - 1;
		int y2 = y;
		int y3 = y + 1;
		if ( x == 0)
			x1 = x3;
		if ( y == 0)
			y1 = y3;
		if ( x == nx-1)
			x3 = x1;
		if ( y == ny-1)
			y3 = y1;
	 	int offset = y1*nx;
		neigh[0] = pixels[offset+x1];
		neigh[1] = pixels[offset+x2];
		neigh[2] = pixels[offset+x3];
	 	offset = y2*nx;
		neigh[3] = pixels[offset+x1];
		neigh[4] = pixels[offset+x2];
		neigh[5] = pixels[offset+x3];
	 	offset = y3*nx;
		neigh[6] = pixels[offset+x1];
		neigh[7] = pixels[offset+x2];
		neigh[8] = pixels[offset+x3];
	}

	/**
	* An ImageAccess object calls this method for getting
	* a 3x3 neighborhood of 4-connected pixels around a selected pixel.
	*
	* @param x		input, the integer x-coordinate of a selected central pixel
	* @param y		input, the integer y-coordinate of a selected central pixel
	* @param neigh	output, an array consisting of 5 elements of the type double
	*/
	private void getPatternCross3x3(int x, int y, double neigh[]) {
	 	if (x >= 1)
	 	if (y >= 1)
	 	if (x < nx-1)
	 	if (y < ny-1) { 
			int index = (y-1)*nx + x;
			neigh[0] = pixels[index];
			index += (nx - 1);
			neigh[1] = pixels[index++];
			neigh[2] = pixels[index++];
			neigh[3] = pixels[index];
			index += (nx - 1);
			neigh[4] = pixels[index];
			return;
		}
		int x1 = x - 1;
		int x2 = x;
		int x3 = x + 1;
		int y1 = y - 1;
		int y2 = y;
		int y3 = y + 1;
		if ( x == 0)
			x1 = x3;
		if ( y == 0)
			y1 = y3;
		if ( x == nx-1)
			x3 = x1;
		if ( y == ny-1)
			y3 = y1;
	 	int offset = y1*nx;
		neigh[0] = pixels[offset+x2];
	 	offset = y2*nx;
		neigh[1] = pixels[offset+x1];
		neigh[2] = pixels[offset+x2];
		neigh[3] = pixels[offset+x3];
	 	offset = y3*nx;
		neigh[4] = pixels[offset+x2];
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
	public void getSubImage(int x, int y, ImageAccess output) {
	    if (output == null)
			throw new ArrayStoreException("getSubImage: output == null.");
	    if (x<0)
			throw new ArrayStoreException("getSubImage: Incompatible image size");
	    if (y<0)
			throw new ArrayStoreException("getSubImage: Incompatible image size");
	    if (x>=nx)
			throw new ArrayStoreException("getSubImage: Incompatible image size");
	    if (y>=ny)
			throw new ArrayStoreException("getSubImage: Incompatible image size");
	 	int nxcopy = output.getWidth();
	 	int nycopy = output.getHeight();
	 	double[][] neigh = new double[nxcopy][nycopy];
	 	int nx2 = (nxcopy-1)/2;
	 	int ny2 = (nycopy-1)/2;
	 	this.getNeighborhood(x+nx2, y+ny2, neigh);
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
	    	return;
		if (x >= nx)
	    	return;
		if (y < 0)
	   		return;
		if (y >= ny)
		   	return;
		pixels[x+y*nx] = value;
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
			throw new IndexOutOfBoundsException("putColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("putColumn: x >= nx.");
		if (column == null)
			throw new ArrayStoreException("putColumn: column == null.");
		if (column.length != ny)
			throw new ArrayStoreException("putColumn: column.length != ny.");
		for (int i=0; i<ny; i++) {
			pixels[x] = column[i];
			x += nx;
		}
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
			throw new IndexOutOfBoundsException("putColumn: x < 0.");
		if (x >= nx)
			throw new IndexOutOfBoundsException("putColumn: x >= nx.");
		if (column == null)
			throw new ArrayStoreException("putColumn: column == null.");
	    int by = column.length;
		int index = y*nx+x;
		int top=0;
		int bottom=0;
		if (y >=0) {
	 		if (y < ny-by) 
	 			bottom = by;
	 		else 
	 			bottom = -y+ny;
			for (int i=top ; i<bottom  ; i++) {
				pixels[index] = column[i];
				index+=nx;		
			}	
			return;
		} 
		else {
			index = x;
			top = -y;
			if (y < ny-by) 
				bottom = by;
			else 
				bottom = -y+ny;
			for (int i=top; i<bottom; i++) {
				pixels[index] = column[i];	
				index+=nx;	
			}
		}
	}

	/**
	* An ImageAccess object calls this method to put a whole 
	* row in a specified position into the image.
	*
	* @param y       input, the integer y-coordinate of a row
	* @param row     input, an array of the type double
	*/
	public void putRow(int y, double[] row){
		if (y < 0) 
			throw new IndexOutOfBoundsException("putRow: y < 0.");
		if (y >= ny)
			throw new IndexOutOfBoundsException("putRow: y >= ny.");
		if (row == null)
			throw new ArrayStoreException("putRow: row == null.");
		if (row.length != nx)
			throw new ArrayStoreException("putRow: row.length != nx.");
		y *= nx;
		for (int i=0; i<nx; i++) {
			pixels[y++] = row[i];
		}
				
	}

	/**
	* An ImageAccess object calls this method to put a part of row
	* into the image. The starting poisition in given by x and the ending
	* position is determined by the size of the row array.
	*
	* @param x       	input, the integer x-coordinate of a column
	* @param y       	input, the integer y-coordinate of a row
	* @param row		input, an array of the type double
	*/
	public void putRow(int x, int y, double[] row) {
		if (y < 0) 
			throw new IndexOutOfBoundsException("putRow: y < 0.");
		if (y >= ny)
			throw new IndexOutOfBoundsException("putRow: y >= ny.");
		if (row == null)
			throw new ArrayStoreException("putRow: row == null.");
	   	int bx = row.length; 
	 	int index = y*nx+x;
		int left=0;
		int right=0;
		if (x >=0){
	 		if (x < nx-bx) 
	 			right=bx;
	 		else 
	 			right=-x+nx;
		
			for (int i = left; i < right; i++) {
				pixels[index++] = row[i];	
			}	
			return;
		} 
		else {
			index = y*nx;
			left=-x;
		
			if (x < nx-bx) 
				right=bx;
			else 
				right=-x+nx;
		
			for (int i = left; i < right; i++) {
				pixels[index++] = row[i];	
			}
		}		
	}

	/**
	* An ImageAccess object calls this method in order to put 
	* an 2D array of double in an ImageAccess.
	*
	* @param array		input, the double array
	*/
	public void putArrayPixels(double[][] array) {
	    if (array == null)
			throw new IndexOutOfBoundsException("putArrayPixels: array == null.");
	   	int bx = array.length;
		int by = array[0].length;
	    if (bx*by != size)
			throw new IndexOutOfBoundsException("putArrayPixels: imcompatible size.");
	    int k = 0;
		for (int j=0; j<by; j++)
		for (int i=0; i<bx; i++)
			pixels[k++] = array[i][j];
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
	public void putSubImage(int x, int y, ImageAccess input){
	    if (input == null)
			throw new ArrayStoreException("putSubImage: input == null.");
	    if (x < 0 )
			throw new IndexOutOfBoundsException("putSubImage: x < 0.");
	    if (y < 0)
			throw new IndexOutOfBoundsException("putSubImage: y < 0.");
	    if (x >= nx)
			throw new IndexOutOfBoundsException("putSubImage: x >= nx.");
	    if (y >= ny)
			throw new IndexOutOfBoundsException("putSubImage: y >= ny.");
	 	int nxcopy=input.getWidth();
	 	int nycopy=input.getHeight();
	 	// Reduces the size of the area to copy if it is too large
		if (x+nxcopy>nx) 
			nxcopy = nx-x;
		if (y+nycopy>ny) 
			nycopy = ny-y;
		// Copies lines per lines
		double[] dsrc = input.getPixels();
		for ( int j=0; j<nycopy; j++)
			System.arraycopy(dsrc, j*nxcopy, pixels, (j+y)*nx+x, nxcopy);
	}

	/**
	* An ImageAccess object calls this method to set a constant
	* value to all pixels of the image.
	*
	* @param constant a constant value 
	*/
	public void setConstant(double constant) {
		for (int k=0; k<size; k++)
			pixels[k] = constant;
	}

	/**
	* Stretches the contrast inside an image so that the gray levels 
	* are in the range 0 to 255.
	*/
	public void normalizeContrast() {
		double minGoal = 0.0;
		double maxGoal = 255.0;
		// Search the min and max
		double minImage = getMinimum();
		double maxImage = getMaximum();
		// Compute the parameter to rescale the gray levels
		double a;
		if ( minImage-maxImage == 0) {
			a = 1.0;
			minImage = (maxGoal-minGoal)/2.0;
		}
		else
			a = (maxGoal-minGoal) / (maxImage-minImage);
		for (int i = 0; i < size; i++) {
			pixels[i]= (float)(a*(pixels[i]-minImage) + minGoal);
		}
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
		impResult.show();
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
		FloatProcessor fp = createFloatProcessor();
		fp.resetMinAndMax();
		ImagePlus impResult = new ImagePlus(title, fp);
		impResult.show();
	}

	/**
	* Compute the absolute value.
	*/
	public void abs() {	
		for (int k=0; k<size; k++)
			pixels[k] = Math.abs(pixels[k]);
	}

	/**
	* Compute the square root of an ImageAccess.
	*/
	public void sqrt() {
		for (int k=0; k<size; k++) {
			pixels[k] = Math.sqrt(pixels[k]);
		}
	}

	/**
	* Raised an ImageAccess object to the power a.
	*
	* @param a 	input
	*/
	public void pow(final double a) {
		for (int k=0; k<size; k++) {
			pixels[k]= Math.pow(pixels[k], a);
		}
	}

	/**
	* An ImageAccess object calls this method for adding
	* a constant to each pixel. 
	*
	* @param constant   a constant to be added
	*/
	public void add(double constant) {
		for (int k=0; k<size; k++)
			pixels[k] += constant;
	}

	/**
	* An ImageAccess object calls this method for multiplying
	* a constant to each pixel. 
	*
	* @param constant   a constant to be multiplied
	*/
	public void multiply(final double constant) {
		for (int k=0; k<size; k++)
			pixels[k] *= constant;
	}

	/**
	* An ImageAccess object calls this method for adding
	* a constant to each pixel. 
	*
	* @param constant   a constant to be subtracted
	*/
	public void subtract(final double constant) {
		for (int k=0; k<size; k++)
			pixels[k] -= constant;
	}

	/**
	* An ImageAccess object calls this method for dividing
	* a constant to each pixel. 
	*
	* @param constant   a constant to be divided
	*/
	public void divide(final double constant) {
		if (constant == 0.0) 
			throw new ArrayStoreException("divide: Divide by 0");
		for (int k=0; k<size; k++)
			pixels[k] /= constant;
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
			throw new ArrayStoreException("add: incompatible size.");
		if (im1.getHeight() != ny)
			throw new ArrayStoreException("add: incompatible size.");
		if (im2.getWidth() != nx)
			throw new ArrayStoreException("add: incompatible size.");
		if (im2.getHeight() != ny)
			throw new ArrayStoreException("add: incompatible size.");
		double[] doubleOperand1 = im1.getPixels();
		double[] doubleOperand2 = im2.getPixels();
		for (int k=0; k<size; k++)
			pixels[k] = doubleOperand1[k] + doubleOperand2[k];
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
			throw new ArrayStoreException("multiply: incompatible size.");
		if (im1.getHeight() != ny)
			throw new ArrayStoreException("multiply: incompatible size.");
		if (im2.getWidth() != nx)
			throw new ArrayStoreException("multiply: incompatible size.");
		if (im2.getHeight() != ny)
			throw new ArrayStoreException("multiply: incompatible size.");
		double[] doubleOperand1 = im1.getPixels();
		double[] doubleOperand2 = im2.getPixels();
		for (int k=0; k<size; k++)
			pixels[k] = doubleOperand1[k] * doubleOperand2[k];
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
			throw new ArrayStoreException("subtract: incompatible size.");
		if (im1.getHeight() != ny)
			throw new ArrayStoreException("subtract: incompatible size.");
		if (im2.getWidth() != nx)
			throw new ArrayStoreException("subtract: incompatible size.");
		if (im2.getHeight() != ny)
			throw new ArrayStoreException("subtract: incompatible size.");
		double[] doubleOperand1 = im1.getPixels();
		double[] doubleOperand2 = im2.getPixels();
		for (int k=0; k<size; k++)
			pixels[k] = doubleOperand1[k] - doubleOperand2[k];
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
			throw new ArrayStoreException("divide: incompatible size.");
		if (im1.getHeight() != ny)
			throw new ArrayStoreException("divide: incompatible size.");
		if (im2.getWidth() != nx)
			throw new ArrayStoreException("divide: incompatible size.");
		if (im2.getHeight() != ny)
			throw new ArrayStoreException("divide: incompatible size.");
		double[] doubleOperand1 = im1.getPixels();
		double[] doubleOperand2 = im2.getPixels();
		for (int k=0; k<size; k++)
			pixels[k] = doubleOperand1[k] / doubleOperand2[k];
	}

}

