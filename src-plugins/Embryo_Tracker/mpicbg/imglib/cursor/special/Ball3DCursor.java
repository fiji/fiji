package mpicbg.imglib.cursor.special;

import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Iterator;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

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
 * of a 3D ball, whose center and radius are given at construction. 
 * <p>
 * Internally, it relies on a {@link LocalizableByDimCursor}.
 * 
 * @author Jean-Yves Tinevez (jeanyves.tinevez@gmail.com)
 *
 * Aug 12, 2010
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
	private CursorState state;
	/** Store the position index. */
	private int[] position;
	/** For mirroring, indicate if we must take the mirror in the X direction. */
	private boolean mirrorX;
	/** For mirroring, indicate if we must take the mirror in the Y direction. */
	private boolean mirrorY;
	/** For mirroring, indicate if we must take the mirror in the Z direction. */
	private boolean mirrorZ;
	/** Indicate whether we finished iterating over a whole X line. */
	private boolean doneX;
	/** Indicate whether we finished iterating over possible Ys. */
	private boolean doneY;
	/** The line half-length. */
	private int rx;
	/** The XY circle radius at height Z. */
	private int ry;
	/** The XY circle squared radius at height Z. */
	private double ry2;
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
 	private enum CursorState {
		INITIALIZED						,
		MIDDLE_Z_MIDDLE_Y				,
		MIDDLE_Z_OTHER_Y_MIDDLE_X		,
		MIDDLE_Z_OTHER_Y_OTHER_X		,
		OTHER_Z_MIDDLE_Y				,
		OTHER_Z_OTHER_Y_MIDDLE_X		,
		OTHER_Z_OTHER_Y_OTHER_X			;		
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
		this.position = new int[] { 0, -radius, -radius } ;
		
		cursor.setPosition(center[0], 0);
		cursor.setPosition(center[1] - radius, 1);
		cursor.setPosition(center[2] - radius, 2);
		rx = 0;
		ry = 0;
	}
	
	
	/**
	 * Return the square distance measured from the center of the ball to the current
	 * cursor position.
	 */
	public final int getDistanceSquared() {
		return position[0] * position[0] + position[1] * position[1] + position[2] * position[2];  
		
	}
	
	@Override
	public void fwd() {
				
		position[0]++;

		if (position[0] > rx) {
			
			// We finished the line at height Y
			position[1]++;
			
			if (position[1] > ry) {

				// We finished the circle at height Z
				position[2]++;
				
				if (position[2] > radius) {
				
					// We finished the ball
					hasNext = false;
					position[0] = 0;
					position[1] = 0;
					position[2] = 0;
					cursor.setPosition(center);
					
				
				} else {
					
					// We did not finish the ball, move up in Z
					ry2 = radius * radius - position[2] * position [2];
					ry = (int) Math.sqrt( ry2 );
					rx = (int) Math.sqrt(ry * ry - position[1] * position[1]);
					position[1] = -ry; // Bottom of the circle
					position[0] = 0;
					cursor.setPosition(center[0]-rx, 0);
					cursor.setPosition(center[1]-ry, 1);
					cursor.fwd(2);

				}
				
			} else {
				
				// We did not finish the circle, move up in Y
				rx = (int) Math.sqrt(ry * ry - position[1] * position[1]);
				position[0] = -rx;				
				cursor.setPosition(center[0]-rx, 0);
				cursor.fwd(1);
				
			}
			
		} else {

			// We did not finish the line. Move forward in it.
			cursor.fwd(0);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void fwd(long steps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int[] getDimensions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getDimensions(int[] position) {
		// TODO Auto-generated method stub
		
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
//			cursor.getType().set(cursor.getDistanceSquared());
			cursor.getType().set(255);
		}

		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.show();
	}
	
}
