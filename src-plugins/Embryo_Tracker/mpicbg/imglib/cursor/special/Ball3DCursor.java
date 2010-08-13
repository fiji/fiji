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
 * The iteration order is deterministic. Iteration starts from the middle Z plane, and fill circles
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
	
	private Image<T> img;
	private int[] center;
	private int radius;
	private LocalizableByDimCursor<T> cursor;
	private boolean hasNext;
	/** The state of the cursor. */
	private CursorState state, nextState;
	/** Store the position index. */
	private int[] position;
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
	 * Construct a {@link Ball3DCursor} on a 3D image.
	 * @param img  the image, must be 3D
	 * @param center  the ball center
	 * @param radius  the ball radius
	 */
	public Ball3DCursor(Image<T> img, final int[] center, int radius) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("Ball3DCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.center = center;
		this.radius = radius;
		this.cursor = img.createLocalizableByDimCursor();		
		this.hasNext = true;
		this.state = CursorState.INITIALIZED;
		this.position = new int[] {0, 0, 0};
		cursor.setPosition(center);
		mirrorZ = false;
		doneZ = false;
	}
	
	
	/**
	 * Return the square distance measured from the center of the ball to the current
	 * cursor position.
	 */
	public final int getDistanceSquared() {
		return position[0] * position[0] + position[1] * position[1] + position[2] * position[2];  
	}
	
	/**
	 * Store the relative position of the current cursor with respect to the ball center in 
	 * the array given in argument.
	 */
	public final void getRelativePosition(int[] position) {
		position[0] = this.position[0];
		position[1] = this.position[1];
		position[2] = this.position[2];
	}
	
	@Override
	public void fwd() {
		
		switch(state) {
		
		case DRAWING_LINE:
			// Will draw a line towards positives x until we exceed rx
			
			
			if (position[0] >= rx) 
				state = nextState;
			else {
				cursor.fwd(0);
				position[0]++;				
			}
			break;
		
		case INITIALIZED:
			// Will draw the line a z=0, y=0 with x from -radius to +radius
			rx = radius;
			cursor.setPosition(center[0] - rx, 0);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			ry = radius;
			break;
		
		case INCREMENT_Y:
			
			position[1] = -position[1] + 1; // y should be negative (coming from mirroring or init = 0)
			cursor.setPosition(center[1] + position[1], 1);
			state = CursorState.DRAWING_LINE;
			rx = (int) Math.sqrt(ry * ry - position[1] * position[1]);
			position[0] = -rx;
			cursor.setPosition(center[0] - rx, 0);
			nextState = CursorState.MIRROR_Y;
			break;
			
		case MIRROR_Y:
			
			position[0] = -rx;
			position[1] = - position[1];
			cursor.setPosition(center[1] + position[1], 1);
			cursor.setPosition(center[0] - rx, 0);
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
				if (position[2] >= radius) 
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
			ry = (int) Math.sqrt(radius * radius - position[2] * position[2]);
			rx = ry; // We start at middle Y
			cursor.setPosition(center[0]-rx, 0);
			cursor.setPosition(center[1], 1);
			cursor.setPosition(center[2] + position[2], 2);
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
				).createImage(new int[] {80, 80, 80});
		
		Ball3DCursor<UnsignedByteType> cursor = new Ball3DCursor<UnsignedByteType>(
				testImage, 
				new int[] {40, 40, 40}, 
				15);
		while(cursor.hasNext) {
			cursor.fwd();
			cursor.getType().set(cursor.getDistanceSquared());
		}

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.show();
	}
	
}
