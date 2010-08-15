package mpicbg.imglib.cursor.special;

import ij.ImagePlus;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

/**
 * This class implements a {@link Cursor} that iterates over all the pixel within the volume
 * of a 3D ball, whose center and radius are given at construction. It is made so that
 * if the ball volume is made of N pixels, this cursor will go exactly over N iterations 
 * before exhausting. The ball iterated is symmetrical along X, Y and Z equatorial planes.
 * <p>
 * It takes a spatial calibration into account, which may be non isotropic.
 * <p>
 * Bounding box: if we assume for instance that the calibration is 1 in every direction,
 * then the whole ball will be contained in a cube which side is <code>2 * ceil(radius) + 1</code>.
 * <p>
 * The iteration order is always the same. Iteration starts from the middle Z plane, and fill circles
 * away from this plane in alternating fashion: <code>Z = 0, 1, -1, 2, -2, ...</code>. For each 
 * circle, lines are drawn in the X positive direction from the middle line and away from it also in
 * an alternating fashion: <code>Y = 0, 1, -1, 2, -2, ...</code>
 * <p>
 * Internally, this cursor relies on a {@link LocalizableByDimCursor}. It makes intensive use
 * of states to avoid calling the {@link Math#sqrt(double)} method too often. 
 * 
 * @author Jean-Yves Tinevez (jeanyves.tinevez@gmail.com) -  August, 2010
 *
 * @param <T>
 */
public class Ball3DCursor<T extends Type<T>> implements Cursor<T> {
	
	private final Image<T> img;
	private final int[] icenter;
	private final double radius;
	private final LocalizableByDimCursor<T> cursor;
	private final int[] position;
	/** The spatial calibration. */
	private final double[] calibration;

	private boolean hasNext;
	/** The state of the cursor. */
	private CursorState state, nextState;
	/** Store the position index. */
	/** For mirroring, indicate if we must take the mirror in the Z direction. */
	private boolean mirrorZ;
	/** The line half-length. */
	private int rx;
	/** The XY circle radius at height Z. */
	private int ry;
	/** Indicate whether we finished all Z planes. */
	private boolean doneZ = false;
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
	 * @param calibration  the spatial calibration (pixel size)
	 */
	public Ball3DCursor(final Image<T> img, final double[] center, double radius, final double[] calibration) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("Ball3DCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.radius = radius;
		this.cursor = img.createLocalizableByDimCursor();		
		this.hasNext = true;
		this.state = CursorState.INITIALIZED;
		this.position = new int[] {0, 0, 0};
		this.calibration = calibration;
		this.icenter = new int[] {
				(int) (center[0] / calibration[0]), 
				(int) (center[1] / calibration[1]), 
				(int) (center[2] / calibration[2]) };
		cursor.setPosition(icenter);
		mirrorZ = false;
		doneZ = false;
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
	public Ball3DCursor(final Image<T> img, final double[] center, double radius) {
		this(img, center, radius, 
				new double[] {img.getCalibration(0), img.getCalibration(1), img.getCalibration(2)});
	}
	
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
	
	@Override
	public void fwd() {
		
		switch(state) {
		
		case DRAWING_LINE:
			
			if (position[0] >= rx) {
				state = nextState;
				fwd();
			} else {
				cursor.fwd(0);
				position[0]++;				
			}
			break;
		
		case INITIALIZED:
			
			// Will draw the line a z=0, y=0 with x from -radius to +radius
			rx = (int) (radius / calibration[0]);
			cursor.setPosition(icenter[0] - rx, 0);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			ry = (int) (radius / calibration[1]);
			break;
		
		case INCREMENT_Y:
			
			position[1] = -position[1] + 1; // y should be negative (coming from mirroring or init = 0)
			cursor.setPosition(icenter[1] + position[1], 1);
			state = CursorState.DRAWING_LINE;
			rx = (int) ( Math.sqrt( ry * ry - position[1] * position[1]) * calibration[1] / calibration[0] );
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
			if (position[1] <= - ry) 
				nextState = CursorState.INCREMENT_Z;
			else 
				nextState = CursorState.INCREMENT_Y;
			break;
			
		case INCREMENT_Z:

			if (mirrorZ) {

				position[2] = - position[2];
				mirrorZ = false;

			} else {
				
				position[2] = - position[2] + 1;
				if (position[2] >= radius / calibration[2]) 
					doneZ = true;
				mirrorZ = true;
			}
			if (doneZ) {
				if (!mirrorZ)
					nextState = CursorState.FINISHED;
			} else {
				nextState = CursorState.INCREMENT_Y;
			}
			
			position[1] = 0;
			ry = (int) ( Math.sqrt(radius * radius - position[2] * position[2] * calibration[2] * calibration[2]) / calibration[1] );
			rx = (int) (ry * calibration[1] / calibration[0]); // We start at middle Y
			cursor.setPosition(icenter[0]-rx, 0);
			cursor.setPosition(icenter[1], 1);
			cursor.setPosition(icenter[2] + position[2], 2);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			break;
			
		case FINISHED:
			
			hasNext = false;
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
	 * Testing
	 */

	public static void main(String[] args) {
		
		ij.ImageJ.main(args);
		
		// Create 3 spots image
		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
					new UnsignedByteType(),
					new ArrayContainerFactory()
				).createImage(new int[] {80, 80, 40}); // 40µm x 40µm x 40µm
		
		double[] calibration = new double[] {0.5, 0.5, 1}; 
		Ball3DCursor<UnsignedByteType> cursor = new Ball3DCursor<UnsignedByteType>(
				testImage, 
				new double[] {20, 20, 20}, // in units
				10, // µm
				calibration);
		while(cursor.hasNext) {
			cursor.fwd();
//			cursor.getType().set((int) cursor.getDistanceSquared()); // to check we paint a sphere in physical coordinates
			cursor.getType().inc(); // to check we did not walk multiple times on a single pixel
		}

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().pixelDepth = calibration[2];
		imp.getCalibration().setUnit("um");
		imp.show();
	}
	
}
