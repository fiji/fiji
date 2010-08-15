package mpicbg.imglib.cursor.special;

import ij.ImagePlus;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

/**
 * This class implements a {@link Cursor} that iterates over all the pixel within the volume
 * of a 3D ball, whose center and radius are given at construction. It is made so that
 * if the ball volume is made of N pixels, this cursor will go exactly over N iterations 
 * before exhausting. 
 * <p>
 * It takes a spatial calibration into account, which may be non isotropic.
 * <p>
 * Bounding box: if we assume for instance that the calibration is 1 in every direction,
 * then the whole ball will be contained in a cube which side is <code>2 * ceil(radius) + 1</code>.
 * <p>
 * The iteration order is always the same. Iteration starts from the middle Z plane, and fill circles
 * away from this plane in alternating fashion: <code>Z = 0, 1, -1, 2, -2, ...</code>. For each 
 * circle, lines are drawn in the X positive direction from the middle line and away from it also in
 * an alternating fashion: <code>Y = 0, 1, -1, 2, -2, ...</code>. To parse all the pixels,
 * a line-scan algorithm is used, relying on McIlroy's algorithm to compute ellipse bounds.
 * <p>
 * Internally, this cursor relies on a {@link LocalizableByDimCursor}. It makes intensive use
 * of states to avoid calling the {@link Math#sqrt(double)} method. 
 * 
 * @author Jean-Yves Tinevez (jeanyves.tinevez@gmail.com) -  August, 2010
 *
 * @param <T>
 */
public class SphereCursor<T extends Type<T>> implements LocalizableCursor<T> {

	private final Image<T> img;
	private final int[] icenter;
	private final float radius;
	private final LocalizableByDimCursor<T> cursor;
	private final int[] position;
	/** The spatial calibration. */
	private final float[] calibration;

	private boolean hasNext;
	/** The state of the cursor. */
	private CursorState state, nextState;
	/** Store the position index. */
	/** For mirroring, indicate if we must take the mirror in the Z direction. */
	private boolean mirrorZ;
	/** When drawing a line, the line length. */
	private int rx;
	/** The XY circle radius at height Z. */
	private int ry;
	/** Store XY circle radiuses for all Z */
	private int[] rys;
	/** Store X line bounds for all Y */
	private int[] rxs;
	/** Indicate whether we finished all Z planes. */
	private boolean doneZ = false;
	/** Is true when all Z and Y have been done, just the last line si to be drawn. */
	private boolean allDone;
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
	private enum CursorState {
		DRAWING_LINE					,
		INITIALIZED						,
		INCREMENT_Y						,
		MIRROR_Y						,
		INCREMENT_Z						,
		FINISHED						;
	}



	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link Ball3DCursor} on a 3D image with a given spatial calibration.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius, final float[] calibration) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("Ball3DCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.radius = radius;
		this.cursor = img.createLocalizableByDimCursor();		
		this.hasNext = true;
		this.state = CursorState.INITIALIZED;
		this.position = new int[] {0, 0, 0};
		if (null == calibration) 
			this.calibration = new float[] {1, 1, 1};
		else 
			this.calibration = calibration;
		this.icenter = new int[] {
				(int) (center[0] / this.calibration[0]), 
				(int) (center[1] / this.calibration[1]), 
				(int) (center[2] / this.calibration[2]) };
		cursor.setPosition(icenter);
		mirrorZ = false;
		doneZ = false;
		allDone = false;
	}

	public SphereCursor(final Image<T> img, final double[] center, double radius, final double[] calibration) {
		// Simply cast to float
		this(img, 
				new float[] {(float)center[0], (float) center[1], (float) center[2]}, 
				(float) radius, 
				new float[] {(float) calibration[0], (float) calibration[1], (float) calibration[2] } );
	}

	/**
	 * Construct a {@link Ball3DCursor} on a 3D image, using the spatial calibration
	 * stored in the image {@link #img}.
	 * 
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius) {
		this(img, center, radius, 
				new float[] {img.getCalibration(0), img.getCalibration(1), img.getCalibration(2)});
	}

	/**
	 * Construct a {@link Ball3DCursor} on a 3D image, using the spatial calibration
	 * stored in the image {@link #img}.
	 * 
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public SphereCursor(final Image<T> img, final double[] center, double radius) {
		// Simply cast to float
		this(img, 
				new float[] {(float)center[0], (float) center[1], (float) center[2]}, 
				(float) radius, 
				new float[] {img.getCalibration(0), img.getCalibration(1), img.getCalibration(2)});
	}

	/*
	 * METHODS
	 */

	/**
	 * Return the square distance measured from the center of the ball to the current
	 * cursor position, in physical units.
	 */
	public final double getDistanceSquared() {
		return position[0] * position[0] * calibration[0] * calibration[0] + 
		position[1] * position[1] * calibration[1] * calibration[1] +
		position[2] * position[2] * calibration[2] * calibration[2];  
	}

	/**
	 * Store the relative position of the current cursor with respect to the ball center in 
	 * the array given in argument. The position is returned in <b>pixel units</b> and as
	 * such, is stored in an <code>int</code> array.
	 */
	public final void getRelativePosition(int[] position) {
		position[0] = this.position[0];
		position[1] = this.position[1];
		position[2] = this.position[2];
	}

	/**
	 * Return the relative calibrated position of this cursor in physical units.
	 */
	public final void getPhysicalRelativeCoordinates(double[] coordinates) {
		coordinates[0] = position[0] / calibration[0];
		coordinates[1] = position[1] / calibration[1];
		coordinates[2] = position[2] / calibration[2];
	}

	/**
	 * Midpoint circle algorithm: store the bounds of a circle in the given array. From
	 * {@link http://en.wikipedia.org/wiki/Midpoint_circle_algorithm}
	 * @param radius  the radius of the circle
	 * @param lineBounds  the array to store bounds in
	 */
	@SuppressWarnings("unused")
	private static final void getXYCircleBounds(int radius, int[] lineBounds)	{
		int f = 1 - radius;
		int ddF_x = 1;
		int ddF_y = -2 * radius;
		int x = 0;
		int y = radius;

		lineBounds[0] = radius;

		while(x < y) 		  {
			// ddF_x == 2 * x + 1;
			// ddF_y == -2 * y;
			// f == x*x + y*y - radius*radius + 2*x - y + 1;
			if(f >= 0)  {
				y--;
				ddF_y += 2;
				f += ddF_y;
			}
			x++;
			ddF_x += 2;
			f += ddF_x;  
			lineBounds[y] = x;
			lineBounds[x] = y;
		}
	}

	/** 
	 * Store the half-widths of a X line to scan to fill an ellipse of given axis lengths.
	 * The parameter <code>a</code> is the axis half-length in the X direction, and <code>b</code>
	 * is the axis half-length in the Y direction. 
	 * <p>
	 * The half-widhts will be stored in the array <code>lineBounds</code>, which must be of size equal
	 * to at least <code>b+1</code>.
	 * <p>
	 * This is an implementation of the McIlroy's algorithm, adapted freely from 
	 * {@link http://enchantia.com/software/graphapp/doc/tech/ellipses.html}.
	 * 
	 * @param a  half-length of the ellipse in the X direction
	 * @param b  half-length of the ellipse in the Y direction
	 * @param lineBounds  will store the half-length of the ellipse lines in the X direction
	 */
	private static final void getXYEllipseBounds(int a, int b, int[] lineBounds) {
		/* e(x,y) = b^2*x^2 + a^2*y^2 - a^2*b^2 */
		int x = 0, y = b;
		int width = 0;
		long a2 = (long)a*a, b2 = (long)b*b;
		long crit1 = -(a2/4 + a%2 + b2);
		long crit2 = -(b2/4 + b%2 + a2);
		long crit3 = -(b2/4 + b%2);
		long t = -a2*y; /* e(x+1/2,y-1/2) - (a^2+b^2)/4 */
		long dxt = 2*b2*x, dyt = -2*a2*y;
		long d2xt = 2*b2, d2yt = 2*a2;

		while (y>=0 && x<=a) {
			if (t + b2*x <= crit1 ||     /* e(x+1,y-1/2) <= 0 */
					t + a2*y <= crit3) {     /* e(x+1/2,y) <= 0 */
				x++; dxt += d2xt; t += dxt;// incx();
				width += 1;
			}
			else if (t - a2*y > crit2) { /* e(x+1/2,y-1) > 0 */
				lineBounds[y] = width; //row(y, width);
				//					if (y!=0)
				//						row(xc-x, yc+y, width);
				y--; dyt += d2yt; t += dyt; // incy();
			}
			else {
				lineBounds[y] = width; // row(y, width);
				//					if (y!=0)
				//						row(xc-x, yc+y, width);
				x++; dxt += d2xt; t += dxt; //incx();
				y--; dyt += d2yt; t += dyt; //incy();
				width += 1;
			}
		}
		if (b == 0)
			lineBounds[0] = a; //row(0, 2*a+1);
	}


	@Override
	public void fwd() {

		switch(state) {

		case DRAWING_LINE:

			cursor.fwd(0);
			position[0]++;
			if (position[0] >= rx) {
				state = nextState;
				if (allDone)
					hasNext = false;
			}
			break;

		case INITIALIZED:

			// Compute XY circle radiuses for all Z in advance
			rys = new int[Math.round(radius/calibration[2])+1];
			getXYEllipseBounds(Math.round(radius/calibration[1]), Math.round(radius/calibration[2]), rys); 
			ry = rys[0] ;
			
			rxs = new int[Math.round(ry/calibration[1])+1]; 
			getXYEllipseBounds(Math.round(radius/calibration[0]), Math.round(radius/calibration[1]), rxs); 
			rx = rxs[0] ; 
			cursor.setPosition(icenter[0] - rx, 0);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		case INCREMENT_Y:

			position[1] = -position[1] + 1; // y should be negative (coming from mirroring or init = 0)
			rx = rxs[position[1]];

			cursor.setPosition(icenter[1] + position[1], 1);
			state = CursorState.DRAWING_LINE;
			position[0] = -rx;
			cursor.setPosition(icenter[0] - rx, 0);
			nextState = CursorState.MIRROR_Y;
			break;

		case MIRROR_Y:

			position[0] = -rx;
			position[1] = - position[1];
			cursor.setPosition(icenter[1] + position[1], 1);
			cursor.setPosition(icenter[0] - rx, 0);
			state = CursorState.DRAWING_LINE;
			if (position[1] <= - ry) {
				if (doneZ) {
					nextState = CursorState.FINISHED;
					allDone  = true ;
				} else
					nextState = CursorState.INCREMENT_Z;
			}
			else 
				nextState = CursorState.INCREMENT_Y;
			break;

		case INCREMENT_Z:

			if (doneZ) {
				state = CursorState.FINISHED;
				fwd();
				break;
			}

			if (mirrorZ) {

				position[2] = - position[2];
				mirrorZ = false;
				if (position[2] <= - radius) 
					doneZ = true;

			} else {

				position[2] = - position[2] + 1;
				ry = rys[position[2]];
				mirrorZ = true;
			}


			rxs = new int[Math.round(ry/calibration[1])+1]; 
			getXYEllipseBounds(Math.round(ry*calibration[1]/calibration[0]), Math.round(ry), rxs); 
			rx = rxs[0] ; 
			
			cursor.setPosition(icenter[0]-rx, 0);
			cursor.setPosition(icenter[1], 1);
			cursor.setPosition(icenter[2] + position[2], 2);
			position[0] = -rx;
			position[1] = 0;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		case FINISHED:

			hasNext = false;
			System.out.println("RYs:");
			for (int i = 0; i < rys.length; i++) {
				System.out.print(rys[i]+" ");
			}
			System.out.println("\nRXs:");
			for (int i = 0; i < rxs.length; i++) {
				System.out.print(rxs[i]+" ");
			}
			System.out.println("\nCursor position: "+cursor.toString());
			break;

		}

	}


	@Override
	public void close() {
		cursor.close();
	}

	@Override
	public int[] createPositionArray() {
		return cursor.createPositionArray();
	}

	@Override
	public int getArrayIndex() {
		return cursor.getArrayIndex();
	}

	@Override
	public Image<T> getImage() {
		return img;
	}

	@Override
	public Container<T> getStorageContainer() {
		return cursor.getStorageContainer();
	}

	@Override
	public int getStorageIndex() {
		return cursor.getStorageIndex();
	}

	@Override
	public T getType() {
		return cursor.getType();
	}

	@Override
	public boolean isActive() {
		return cursor.isActive();
	}

	@Override
	public void reset() {
		cursor.reset();
		cursor.setPosition(icenter);
		state = CursorState.INITIALIZED;
		mirrorZ = false;
		doneZ = false;
		position[0] = 0;
		position[1] = 0;
		position[2] = 0;
	}

	@Override
	public void setDebug(boolean debug) {
		cursor.setDebug(debug);
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public T next() {
		fwd();
		return cursor.getType();
	}

	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public Iterator<T> iterator() {
		reset();
		return this;
	}

	@Override
	public void fwd(long steps) {
		for (int i = 0; i < steps; i++) 
			fwd();
	}

	@Override
	public int[] getDimensions() {
		return cursor.getDimensions();
	}

	@Override
	public void getDimensions(int[] position) {
		cursor.getDimensions(position);
	}

	@Override
	public int getNumDimensions() {
		return 3;
	}

	/*
	 * LOCALIZABLE METHODS
	 */
	
	@Override
	public int[] getPosition() {
		return cursor.getPosition();
	}

	@Override
	public void getPosition(int[] position) {
		cursor.getPosition(position);
	}

	@Override
	public int getPosition(int dim) {
		return cursor.getPosition(dim);
	}

	@Override
	public String getPositionAsString() {
		return cursor.getPositionAsString();
	}

	/*
	 * Testing
	 */

	public static void main(String[] args) {

		/*
		// Check circle routine
		int radius = 9;
		int[] lineBounds = new int[radius+1];
		Ball3DCursor.getXYCircleBounds(radius, lineBounds);

		for (int i = radius; i >= 0; i--) {
			for (int ix = 0; ix < radius-lineBounds[i]; ix++) 
				System.out.print(' ');
			for (int ix = -lineBounds[i]; ix <= lineBounds[i]; ix++) 
				System.out.print(Math.abs(ix));
			System.out.print('\n');

		}
		for (int i = 1; i <= radius; i++) {
			for (int ix = 0; ix < radius-lineBounds[i]; ix++) 
				System.out.print(' ');
			for (int ix = -lineBounds[i]; ix <= lineBounds[i]; ix++) 
				System.out.print(Math.abs(ix));
			System.out.print('\n');	
		}
		 */

		/*
		// Check ellipse routine
		int radiusY = 3;
		int radiusX = 9;
		int[] lineBounds = new int[radiusY+1];
		Ball3DCursor.getXYEllipseBounds(radiusX, radiusY, lineBounds);

		for (int i = radiusY; i >= 0; i--) {
			for (int ix = 0; ix < radiusX-lineBounds[i]; ix++) 
				System.out.print(' ');
			for (int ix = -lineBounds[i]; ix <= lineBounds[i]; ix++) 
				System.out.print(Math.abs(ix));
			System.out.print('\n');

		}
		for (int i = 1; i <= radiusY; i++) {
			for (int ix = 0; ix < radiusX-lineBounds[i]; ix++) 
				System.out.print(' ');
			for (int ix = -lineBounds[i]; ix <= lineBounds[i]; ix++) 
				System.out.print(Math.abs(ix));
			System.out.print('\n');
		}
		 */

		
		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {80, 80, 40}); // 40µm x 40µm x 40µm

		float radius = 10;
		float[] calibration = new float[] {0.5f, 0.5f, 1}; 
		SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(
				testImage, 
				new float[] {20, 20, 20}, // in units
				radius, // µm
				calibration);
		int volume = 0;
		while(cursor.hasNext) {
			volume++;
			cursor.fwd();
//			cursor.getType().set((int) cursor.getDistanceSquared()); // to check we paint a sphere in physical coordinates
			cursor.getType().inc(); // to check we did not walk multiple times on a single pixel
		}
		cursor.close();

		int  maxPixelValue = 0;
		Cursor<UnsignedByteType> c = testImage.createCursor();
		while(c.hasNext()) {
			c.fwd();
			if (c.getType().get() > maxPixelValue) 
				maxPixelValue = c.getType().get();
		}
		c.close();

		System.out.println(String.format("Cursor for a shpere of radius %.1f", radius));
		System.out.println(String.format("Iterated over %d pixels, real volume is: %.1f", volume, 4/3.0*Math.PI*radius*radius*radius));
		System.out.println(String.format("Each pixel have been walked on at most %d times.", maxPixelValue));

		// Visualize results
		ij.ImageJ.main(args);

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().pixelDepth = calibration[2];
		imp.getCalibration().setUnit("um");
		imp.show();
		 
	}


}
