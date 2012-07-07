package voltex;

import ij3d.AxisConstants;

import javax.media.j3d.*;

/**
 * This class is a helper class whose main task is to create Geometry
 * objects for a specified axis and direction.
 * The returned GeometryArray objects are QuadArray objects.
 * 
 * @author Benjamin Schmid
 */
public class GeometryCreator implements AxisConstants {

	/** Temporary array of the coordinates of one rectangle */
	private double[] quadCoords = new double[12];

	/** Image data to be displayed */
	private VoltexVolume volume;

	/**
	 * Initializes this GeometryCreator with the given volume
	 * @param volume
	 */
	public GeometryCreator(VoltexVolume volume) {
		setVolume(volume);
	}

	/**
	 * Change the image data for this GeometryCreator
	 * @param volume
	 */
	public void setVolume(VoltexVolume volume) {
		this.volume = volume;
	}

	/**
	 * Returns the QuadArray for the specified axis and slice index.
	 * @param direction
	 * @param index
	 * @return
	 */
	public GeometryArray getQuad(int direction, int index) {
		calculateQuad(direction, index);
		QuadArray quadArray = new QuadArray(4, 
					GeometryArray.COORDINATES);

		quadArray.setCoordinates(0, quadCoords);
		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
		quadArray.setCapability(QuadArray.ALLOW_COORDINATE_WRITE);
		return quadArray;
	}

	/**
	 * Returns the coordinates of the rectangle of the specified
	 * axis and slice index as a double array.
	 * @param direction
	 * @param index
	 * @return
	 */
	public double[] getQuadCoords(int direction, int index) {
		calculateQuad(direction, index);
		return quadCoords;
	}

	/**
	 * Calculate the quad coordinates for the given axis and index and
	 * store them in the field.
	 * @param direction
	 * @param index
	 */
	private void calculateQuad(int direction, int index) {
		switch(direction) {
			case X_AXIS: 	
				setCoordsY();
				setCoordsZ();
				setCurCoordX(index); 
				break;
			case Y_AXIS:
				setCoordsX();
				setCoordsZ();
				setCurCoordY(index); 
				break;
			case Z_AXIS:
				setCoordsX();
				setCoordsY();
				setCurCoordZ(index); 
				break;
		}
	}

	private void setCurCoordX(int i) {
		double curX = i * volume.pw + volume.minCoord.x;
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	private void setCurCoordY(int i) {
		double curY = i * volume.ph + volume.minCoord.y;
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	private void setCurCoordZ(int i) {
		double curZ = i * volume.pd + volume.minCoord.z;
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void setCoordsX() {
		// lower left
		quadCoords[1] = volume.minCoord.y;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[4] = volume.maxCoord.y;
		quadCoords[5] = volume.minCoord.z;
		// upper right
		quadCoords[7] = volume.maxCoord.y;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[10] = volume.minCoord.y;
		quadCoords[11] = volume.maxCoord.z;
	}

	private void setCoordsY() {
		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[3] = volume.minCoord.x;
		quadCoords[5] = volume.maxCoord.z;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[9] = volume.maxCoord.x;
		quadCoords[11] = volume.minCoord.z;
	}

	private void setCoordsZ() {
		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[1] = volume.minCoord.y;
		// lower right
		quadCoords[3] = volume.maxCoord.x;
		quadCoords[4] = volume.minCoord.y;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[7] = volume.maxCoord.y;
		// upper left
		quadCoords[9] = volume.minCoord.x;
		quadCoords[10] = volume.maxCoord.y;
	}
}
