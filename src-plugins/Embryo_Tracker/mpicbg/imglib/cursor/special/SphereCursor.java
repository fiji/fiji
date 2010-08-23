package mpicbg.imglib.cursor.special;

import ij.ImagePlus;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * This class implements a {@link LocalizableCursor} that iterates over all the pixel within the volume
 * of a 3D ball, whose center and radius are given at construction. It is made so that
 * if the ball volume is made of N pixels, this cursor will go exactly over N iterations 
 * before exhausting. 
 * It takes a spatial calibration into account, which may be non isotropic, so that the region
 * iterated over is a sphere in <b>physical coordinates</b>.
 * <p>
 * The center of the sphere can be set to a {@link LocalizableCursor} position, allowing
 * to use this cursor in neighborhood processing operations. The two cursors (this one and 
 * the main one) are not linked in any way, so the method to move the sphere center must
 * be called every time. For instance:
 * <pre>
 * Image<T> img;
 * // ...
 * float radius = 10;
 * LocalizableCursorr< T > mainCursor = img.createLocalizableCursor();
 * SphereCursor< T > sphereCursor = new SphereCursorr< T >(img, mainCursor, radius);
 * while (mainCursor.hasNext()) {
 * 	mainCursor.fwd();
 * 	sphereCursor.moveCenterTo(mainCursor);
 * 	while (sphereCursor.hasNext()) {
 * 		sphereCursor.fwd();
 * 		// Have fun here ...
 * 	}
 * }
 * sphereCursor.close();
 * mainCursor.close();
 * </pre>
 * 
 * <p>
 * Bounding box: if we assume for instance that the calibration is 1 in every direction,
 * then the whole ball will be contained in a cube which side is <code>2 * ceil(radius) + 1</code>.
 * <p>
 * The iteration order is always the same. Iteration starts from the middle Z plane, and fill circles
 * away from this plane in alternating fashion: <code>Z = 0, 1, -1, 2, -2, ...</code>. For each 
 * circle, lines are drawn in the X positive direction from the middle line and away from it also in
 * an alternating fashion: <code>Y = 0, 1, -1, 2, -2, ...</code>. To parse all the pixels,
 * a line-scan algorithm is used, relying on McIlroy's algorithm to compute ellipse bounds
 * efficiently.
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
	private float radius;
	private final LocalizableByDimCursor<T> cursor;
	/** Store the sphere center in pixel coordinates. */
	private int icenterX, icenterY, icenterZ;
	/** Cursor relative position. */
	private int positionX, positionY, positionZ;
	/** The spatial calibration. */
	private final float calibrationX, calibrationY, calibrationZ; 

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
	/** Is true when all Z and Y have been done, just the last line is to be drawn. */
	private boolean allDone;
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
	private enum CursorState {
		DRAWING_LINE					,
		INITIALIZED						,
		INCREMENT_Y						,
		MIRROR_Y						,
		INCREMENT_Z						;
	}



	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 * @param outOfBoundsFactory  the {@link OutOfBoundsStrategyFactory} that will be used to handle off-bound locations
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius, final float[] calibration, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("Ball3DCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.radius = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) { 
			this.calibrationX = 1f;
			this.calibrationY = 1f;
			this.calibrationZ = 1f;
		} else { 
			this.calibrationX = calibration[0];
			this.calibrationY = calibration[1];
			this.calibrationZ = calibration[2];
		}
		this.icenterX = Math.round(center[0] / this.calibrationX); 
		this.icenterY = Math.round(center[1] / this.calibrationY); 
		this.icenterZ = Math.round(center[2] / this.calibrationZ); 

		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ (int) (Math.max(Math.ceil(radius/this.calibrationY), Math.ceil(radius/this.calibrationX))  +  1) ];
		rys = new int[(int) (Math.ceil(radius/this.calibrationZ)+1)];
		reset();
	}
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius, final float[] calibration) {
		this(img, center, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final double[] center, double radius, final double[] calibration) {
		// Simply cast to float
		this(img, 
				new float[] {(float)center[0], (float) center[1], (float) center[2]}, 
				(float) radius, 
				null == calibration ? null : new float[] {(float) calibration[0], (float) calibration[1], (float) calibration[2] } );
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
	 * 
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius) {
		this(img, center, radius, img.getCalibration());
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
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
				img.getCalibration() );
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration
	 * and a given {@link OutOfBoundsStrategyFactory} to handle off-bounds locations.
 	 * The center of the sphere is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the localizable object which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration,
			OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("Ball3DCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.radius = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) { 
			this.calibrationX = 1f;
			this.calibrationY = 1f;
			this.calibrationZ = 1f;
		} else { 
			this.calibrationX = calibration[0];
			this.calibrationY = calibration[1];
			this.calibrationZ = calibration[2];
		}
		this.icenterX = centerCursor.getPosition(0);
		this.icenterY = centerCursor.getPosition(1);
		this.icenterZ = centerCursor.getPosition(2);
		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ Math.max(Math.round(radius/calibrationY), Math.round(radius/calibrationX))  +  1 ];
		rys = new int[Math.round(radius/calibrationZ)+1];
		reset();
	}
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the given spatial calibration
	 * and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the sphere
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the cursor which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration) {
		this(img, centerCursor, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}
	
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the sphere
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the localizable object which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius) {
		this(img, centerCursor, radius, img.getCalibration(), new OutOfBoundsStrategyValueFactory<T>());
	}
	
	/*
	 * SPECIFIC SPHEREVOLUME METHODS
	 */
	
	
	/**
	 * Return the number of pixels this cursor will iterate on (or, the number of iterations
	 * it will do before exhausting). This can be seen as an approximation of the sphere volume
	 * (though, in pixel coordinates), and is useful when one needs to know 
	 * the number of pixel iterated on in advance. For instance:
	 * <pre>
	 * SphereCursor<T> sc = new SphereCursor(img, center, 5);
	 * int arrraySize = sc.getNPixels();
	 * float[] pixelVal = new float[arraySize];
	 * int index = 0;
	 * while (sc.hasNext()) {
	 * 	sc.fwd();
	 * 	pixelVal[index] = sc.getType().getRealFloat();
	 * 	index++;
	 * }
	 * </pre>
	 */
	public int getNPixels() {
		int pixel_count = 0;
		final int nzplanes = Math.round(radius/calibrationX); // half nbr of planes
		final int[] local_rys = new int[nzplanes+1];
		final int[] local_rxs = new int [ Math.max(Math.round(radius/calibrationY), Math.round(radius/calibrationX))  +  1 ];
		int local_ry, local_rx;

		// Get all XY circles radiuses
		getXYEllipseBounds(Math.round(radius/calibrationY), Math.round(radius/calibrationZ), local_rys); 
		
		// Deal with plane Z = 0
		getXYEllipseBounds(Math.round(radius/calibrationX), Math.round(radius/calibrationY), local_rxs);
		local_ry = local_rys[0];
		local_rx = local_rxs[0]; // middle line
		pixel_count += 2 * local_rx + 1;
		for (int i = 1; i <= local_ry; i++) {
			local_rx = local_rxs[i];
			pixel_count += 2 * (2 * local_rx + 1); // Twice because we mirror
		}
		
		// Deal with other planes
		for (int j = 1; j <= nzplanes; j++) {
			local_ry = local_rys[j];
			if (local_ry ==0) 
				continue;
			getXYEllipseBounds(Math.round(local_ry*calibrationY/calibrationX), local_ry, local_rxs); 
			local_rx = local_rxs[0]; // middle line
			pixel_count += 2 * (2 * local_rx + 1); // twice we mirror in Z
			for (int i = 1; i <= local_ry; i++) {
				local_rx = local_rxs[i];
				pixel_count += 4 * (2 * local_rx + 1); // 4 times because we mirror in Z and in Y
			}
		}
		
		return pixel_count;
		
	}
	
	/**
	 * Move the center of the sphere to the location specified by the {@link Localizable} object. 
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterTo(final Localizable cursor) {
		icenterX = cursor.getPosition(0);
		icenterY = cursor.getPosition(1);
		icenterZ = cursor.getPosition(2);
		reset();
	}
	
	/**
	 * Move the center of the sphere to the pixel location specified by the array, in <b>pixel coordinates</b>..
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterToPosition(final int[] icenter) {
		icenterX = icenter[0];
		icenterY = icenter[1];
		icenterZ = icenter[2];
		reset();
	}
	
	/**
	 * Move the center of the sphere to the physical location specified by the array, in <b>physical coordinates</b>,
	 * taking the calibration into account. 
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterToCoordinates(final float[] center) {
		icenterX = Math.round(center[0] / calibrationX);
		icenterY = Math.round(center[1] / calibrationY);
		icenterZ = Math.round(center[2] / calibrationZ);
		reset();
	}
	
	/**
	 * Change the radius of the sphere this cursor iterates on.  This <b>resets</b> this cursor.
	 * @param radius
	 */
	public void setRadius(float radius) {
		this.radius = radius;
		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ Math.max(Math.round(radius/calibrationY), Math.round(radius/calibrationX))  +  1 ];		
		rys = new int[Math.round(radius/calibrationZ)+1];
		reset();
	}
	

	/**
	 * Return the square distance measured from the center of the ball to the current
	 * cursor position, in physical units.
	 */
	public final double getDistanceSquared() {
		return positionX * positionX * calibrationX * calibrationX +
			positionY * positionY * calibrationY * calibrationY +
			positionZ * positionZ * calibrationZ * calibrationZ;
	}

	/**
	 * Store the relative position of the current cursor with respect to the ball center in 
	 * the array given in argument. The position is returned in <b>pixel units</b> and as
	 * such, is stored in an <code>int</code> array.
	 */
	public final void getRelativePosition(int[] position) {
		position[0] = positionX;
		position[1] = positionY;
		position[2] = positionZ;
	}
	
	/**
	 * Return the current inclination with respect to this sphere center. Will be in
	 * the range [0, π]. 
	 * <p>
	 * In spherical coordinates, the inclination is the angle 
	 * between the Z axis and the line OM where O is the sphere center and M is 
	 * the point location.
	 */
	public final double getTheta() {
		return Math.acos( positionZ * calibrationZ / Math.sqrt( getDistanceSquared() ) );
	}
	
	/**
	 * Return the azimuth of the spherical coordinates of this cursor, with respect 
	 * to its center. Will be in the range ]-π, π].
	 * <p>
	 * In spherical coordinates, the azimuth is the angle measured in the plane XY between 
	 * the X axis and the line OH where O is the sphere center and H is the orthogonal 
	 * projection of the point M on the XY plane.
	 */
	public final double getPhi() {
		return Math.atan2(positionY*calibrationY, positionX*calibrationX);
	}

	/**
	 * Return the relative calibrated position of this cursor in physical units.
	 */
	public final void getPhysicalRelativeCoordinates(double[] coordinates) {
		coordinates[0] = positionX * calibrationX;
		coordinates[1] = positionY * calibrationY;
		coordinates[2] = positionZ * calibrationZ;
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
	public final void fwd() {

		switch(state) {

		case DRAWING_LINE:

			cursor.fwd(0);
			positionX++;
			if (positionX >= rx) {
				state = nextState;
				if (allDone)
					hasNext = false;
			}
			break;

		case INITIALIZED:

			// Compute XY circle radiuses for all Z in advance
			getXYEllipseBounds(Math.round(radius/calibrationY), Math.round(radius/calibrationZ), rys); 
			ry = rys[0] ;
			
			getXYEllipseBounds(Math.round(radius/calibrationX), Math.round(radius/calibrationY), rxs); 
			rx = rxs[0] ; 
			cursor.setPosition(icenterX - rx, 0);
			cursor.setPosition(icenterY, 1);
			cursor.setPosition(icenterZ, 2);
			positionX = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		case INCREMENT_Y:

			positionY = -positionY + 1; // y should be negative (coming from mirroring or init = 0)
			rx = rxs[positionY];

			cursor.setPosition(icenterY + positionY, 1);
			positionX = -rx;
			cursor.setPosition(icenterX - rx, 0);
			nextState = CursorState.MIRROR_Y;
			if (rx ==0)
				state = CursorState.MIRROR_Y;
			else
				state = CursorState.DRAWING_LINE;				
			break;

		case MIRROR_Y:

			positionX = -rx;
			positionY = - positionY;
			cursor.setPosition(icenterY + positionY, 1);
			cursor.setPosition(icenterX - rx, 0);
			if (positionY <= - ry) {
				if (doneZ) 
					allDone  = true ;
				else
					nextState = CursorState.INCREMENT_Z;
			}
			else 
				nextState = CursorState.INCREMENT_Y;
			if (rx ==0)
				if (allDone)
					hasNext = false;
				else
					state = nextState;
			else
				state = CursorState.DRAWING_LINE;

			break;

		case INCREMENT_Z:

			if (mirrorZ) {

				positionZ = - positionZ;
				mirrorZ = false;
				if (positionZ <= - Math.round(radius/calibrationZ)) 
					doneZ = true;

			} else {

				positionZ = - positionZ + 1;
				ry = rys[positionZ];
				mirrorZ = true;
			}

			getXYEllipseBounds(Math.round(ry*calibrationY/calibrationX), ry, rxs); 
			rx = rxs[0] ; 
			
			cursor.setPosition(icenterX-rx, 0);
			cursor.setPosition(icenterY, 1);
			cursor.setPosition(icenterZ + positionZ, 2);
			positionX = -rx;
			positionY = 0;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		}

	}


	/*
	 * CURSOR METHODS
	 * We simply forward them to the internal cursor
	 */
	
	
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
	public final void reset() {
		cursor.reset();
		state = CursorState.INITIALIZED;
		mirrorZ = false;
		doneZ = false;
		allDone = false;
		positionX = 0;
		positionY = 0;
		positionZ = 0;
		hasNext = true;
	}

	@Override
	public void setDebug(boolean debug) {
		cursor.setDebug(debug);
	}

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

	@Override
	public final T next() {
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
		).createImage(new int[] {40, 40, 40}); //{80, 80, 40}); // 40µm x 40µm x 40µm

		float radius = 5; // µm
		float[] calibration = new float[] {1, 1, 1}; //{0.5f, 0.5f, 1}; 
		SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(
				testImage, 
				new float[] {20, 20, 20}, // in µm
				radius, // µm
				calibration);
		int volume = 0;
		while(cursor.hasNext()) {
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
		System.out.println(String.format("Volume iterated prediction: %d pixels.", cursor.getNPixels()));
		System.out.println(String.format("Iterated actually over %d pixels, real volume is: %.1f", volume, 4/3.0*Math.PI*radius*radius*radius));
		System.out.println(String.format("Each pixel have been walked on at most %d times.", maxPixelValue));

		
		// Visualize results
		ij.ImageJ.main(args);

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().pixelDepth = calibration[2];
		imp.getCalibration().setUnit("um");
		imp.show();
		
		
		float iRadius = 5;
		
		// Iterates over all pixels of the image, using the sphere cursor as a neighborhood cursor.
		// We simply convolve.
		
		Image<FloatType> newImage = new ImageFactory<FloatType>(
				new FloatType(),
				new ArrayContainerFactory()
			).createImage(testImage.getDimensions());
		LocalizableCursor<UnsignedByteType> mainCursor = testImage.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> destCursor = newImage.createLocalizableByDimCursor();
		SphereCursor<UnsignedByteType> sphereCursor = new SphereCursor<UnsignedByteType>(testImage, mainCursor, iRadius, calibration);
		System.out.println("\nUsing the sphere cursor to convolve the whole image with a sphere of radius " + iRadius + "...");
		int sum;
		int pixelNumber = 0;
		long start = System.currentTimeMillis();
		while (mainCursor.hasNext()) {
			mainCursor.fwd();
			sphereCursor.moveCenterTo(mainCursor);
			sum = 0;
			while (sphereCursor.hasNext()) {
				sphereCursor.fwd();
				sum += sphereCursor.getType().get();
				pixelNumber++;
			}
			destCursor.setPosition(mainCursor);
			destCursor.getType().set(sum);
		}
		sphereCursor.close();
		mainCursor.close();
		destCursor.close();
		long end = System.currentTimeMillis();
		System.out.println(String.format("Iterated over in total %d pixels in %d ms: %.1e pixel/s.", pixelNumber, (end-start), pixelNumber/((float) (end-start)/1000) ));
		
		ImagePlus dest = ImageJFunctions.copyToImagePlus(newImage);
		dest.getCalibration().pixelWidth = calibration[0];
		dest.getCalibration().pixelHeight = calibration[1];
		dest.getCalibration().pixelDepth = calibration[2];
		dest.getCalibration().setUnit("um");
		dest.show();
		
		// Compare with HyperSphereIterator
		// Compare with neighborhood cursor
		Image<FloatType> newImage3 = new ImageFactory<FloatType>(
				new FloatType(),
				new ArrayContainerFactory()
			).createImage(testImage.getDimensions());
		LocalizableByDimCursor<FloatType> destCursor3 = newImage3.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<FloatType>());
		
		System.out.println("\nUsing the hyper-sphere cursor to convolve the whole image with a sphere of radius " + iRadius + "...");
		pixelNumber = 0;
		start = System.currentTimeMillis();
		while (destCursor3.hasNext()) {
			destCursor3.fwd();
			final HyperSphereIterator<UnsignedByteType> hsc = new HyperSphereIterator<UnsignedByteType>(testImage, destCursor3, (int) iRadius, new OutOfBoundsStrategyValueFactory<UnsignedByteType>());
			sum = 0;
			while (hsc.hasNext()) {
				hsc.fwd();
				sum += hsc.getType().get();
				pixelNumber++;
			}
			hsc.close();
			destCursor3.getType().set(sum);
		}
		destCursor3.close();
		end = System.currentTimeMillis();
		System.out.println(String.format("Iterated over in total %d pixels in %d ms: %.1e pixel/s.", pixelNumber, (end-start), pixelNumber/((float) (end-start)/1000) ));
		
		ImagePlus dest3 = ImageJFunctions.copyToImagePlus(newImage3);
		dest3.getCalibration().pixelWidth = calibration[0];
		dest3.getCalibration().pixelHeight = calibration[1];
		dest3.getCalibration().pixelDepth = calibration[2];
		dest3.getCalibration().setUnit("um");
		dest3.show();
		
		/*
		
		// Compare with neighborhood cursor
		Image<FloatType> newImage2 = new ImageFactory<FloatType>(
				new FloatType(),
				new ArrayContainerFactory()
			).createImage(testImage.getDimensions());
		LocalizableByDimCursor<FloatType> destCursor2 = newImage2.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<FloatType>());
		
		final int roioffsetX = (int) Math.ceil(radius/calibration[0]);
		final int roioffsetY = (int) Math.ceil(radius/calibration[1]);
		final int roioffsetZ = (int) Math.ceil(radius/calibration[2]);
		final int roiSizeX = 2 * roioffsetX + 1;
		final int roiSizeY = 2 * roioffsetY + 1;
		final int roiSizeZ = 2 * roioffsetZ + 1;
		LocalizableByDimCursor<UnsignedByteType> mainCursor2 = testImage.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<UnsignedByteType>());

		RegionOfInterestCursor<UnsignedByteType> regionCursor = mainCursor2.createRegionOfInterestCursor( 
				new int[] { -roioffsetX, -roioffsetY, -roioffsetZ}, 
				new int[] { roiSizeX, roiSizeY, roiSizeZ} );
		
		int pixelNumber2 = 0;
		long start2 = System.currentTimeMillis();
		int sum2;
		System.out.println("\nUsing a cube neighborhood cursor to convolve the whole image with a sphere of radius " + iRadius + "...");
		int[] position = mainCursor2.createPositionArray();
		int[] offsetPos = mainCursor2.createPositionArray();
		int[] regPos = mainCursor2.createPositionArray();
		double dist2;
		final double radius2 = iRadius * iRadius; 
		while (destCursor2.hasNext()) {
			destCursor2.fwd();
			destCursor2.getPosition(position);
			offsetPos[0] = position[0];
			offsetPos[1] = position[1];
			offsetPos[2] = position[2];
//			offsetPos[0] = (int) (position[0] - roioffsetX );
//			offsetPos[1] = (int) (position[1] - roioffsetY );
//			offsetPos[2] = (int) (position[2] - roioffsetZ );
			regionCursor.reset(offsetPos);
			sum2 = 0;
			while(regionCursor.hasNext()) {
				regionCursor.fwd();
				regionCursor.getPosition(regPos);
				dist2 = (position[0]-regPos[0])*(position[0]-regPos[0]) * calibration[0] * calibration[0] +
					(position[1]-regPos[1])*(position[1]-regPos[1]) * calibration[1] * calibration[1] +
					(position[2]-regPos[2])*(position[2]-regPos[2]) * calibration[2] * calibration[2];
				if (dist2 <= radius2) {
					sum2 += regionCursor.getType().get();
					pixelNumber2++;
				}
			}
			destCursor2.getType().set(sum2);
		}
		long end2 = System.currentTimeMillis();
		regionCursor.close();
		destCursor2.close();
		mainCursor2.close();
		System.out.println(String.format("Iterated over %d pixels in %d ms: %.1e pixel/s.", pixelNumber2, (end2-start2), pixelNumber2/((float) (end2-start2)/1000) ));

		
		ImagePlus dest2 = ImageJFunctions.copyToImagePlus(newImage2);
		dest2.getCalibration().pixelWidth = calibration[0];
		dest2.getCalibration().pixelHeight = calibration[1];
		dest2.getCalibration().pixelDepth = calibration[2];
		dest2.getCalibration().setUnit("um");
		dest2.show();
		
		*/
	}
}
