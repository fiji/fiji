package octree;

import javax.media.j3d.*;
import ij3d.AxisConstants;

public class GeometryCreator implements AxisConstants {

	private float[] quadCoords = new float[12];
	private static GeometryCreator instance;

	private GeometryCreator() {}

	public static GeometryCreator instance() {
		if(instance == null)
			instance = new GeometryCreator();
		return instance;
	}

	public GeometryArray getQuad(CubeData cdata, int index) {
		calculateQuad(cdata, index);
		QuadArray quadArray = new QuadArray(4,
					GeometryArray.COORDINATES);

		quadArray.setCoordinates(0, quadCoords);
//		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
		return quadArray;
	}

	public float[] getQuadCoordinates(CubeData cdata, int index) {
		calculateQuad(cdata, index);
		return quadCoords;
	}

	private void calculateQuad(CubeData cdata, int index) {
		switch(cdata.axis) {
			case X_AXIS:
				setCoordsY(cdata);
				setCoordsZ(cdata);
				setCurCoordX(index, cdata);
				break;
			case Y_AXIS:
				setCoordsX(cdata);
				setCoordsZ(cdata);
				setCurCoordY(index, cdata);
				break;
			case Z_AXIS:
				setCoordsX(cdata);
				setCoordsY(cdata);
				setCurCoordZ(index, cdata);
				break;
		}
	}

	private void setCurCoordX(int i, CubeData cdata) {
		float curX = i * cdata.cal[0] + cdata.min[0];
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	private void setCurCoordY(int i, CubeData cdata) {
		float curY = i * cdata.cal[1] + cdata.min[1];
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	private void setCurCoordZ(int i, CubeData cdata) {
		float curZ = i * cdata.cal[2] + cdata.min[2];
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void setCoordsX(CubeData cdata) {
		// lower left
		quadCoords[1] = cdata.min[1];
		quadCoords[2] = cdata.min[2];
		// lower right
		quadCoords[4] = cdata.max[1];
		quadCoords[5] = cdata.min[2];
		// upper right
		quadCoords[7] = cdata.max[1];
		quadCoords[8] = cdata.max[2];
		// upper left
		quadCoords[10] = cdata.min[1];
		quadCoords[11] = cdata.max[2];
	}

	private void setCoordsY(CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.min[0];
		quadCoords[2] = cdata.min[2];
		// lower right
		quadCoords[3] = cdata.min[0];
		quadCoords[5] = cdata.max[2];
		// upper right
		quadCoords[6] = cdata.max[0];
		quadCoords[8] = cdata.max[2];
		// upper left
		quadCoords[9] = cdata.max[0];
		quadCoords[11] = cdata.min[2];
	}

	private void setCoordsZ(CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.min[0];
		quadCoords[1] = cdata.min[1];
		// lower right
		quadCoords[3] = cdata.max[0];
		quadCoords[4] = cdata.min[1];
		// upper right
		quadCoords[6] = cdata.max[0];
		quadCoords[7] = cdata.max[1];
		// upper left
		quadCoords[9] = cdata.min[0];
		quadCoords[10] = cdata.max[1];
	}
}
