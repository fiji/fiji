/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import java.awt.Color;

class Cube {

	private int widthV;
	private int heightV;
	private int depthV;

	private int numIntersections; // number of the intersection
	private int cornerT[][]; 
	private float corners[][]; 
	private float cornersXY[][]; 
	private float cornersYZ[][]; 
	private float cornersXZ[][]; 
	private int cornersXYT[][]; 
	private int cornersYZT[][]; 
	private int cornersXZT[][]; 

	private float interSections[][]; // intersections

	private float [][] textPos;

	private Color backColor;
	private Color frontColor;

	private final static int dm = 16; 
	private final static int dp = 10;

	private final  String[] letters = {"0", "x", "y", "z"};
	private Transform tr;
	
	private Control control; 
	
	
	public void initTextsAndDrawColors(ImageRegion imageRegion) {
		imageRegion.newText(4);  
		imageRegion.newLines(12 + 12); 
		imageRegion.newClipLine(6); 

		backColor  = Color.BLACK; 
		frontColor = Color.LIGHT_GRAY;

		Color color = Color.orange; 

		for (int i= 0; i < letters.length; i++) {
			imageRegion.setText(letters[i], i, color);
		}
	}

	void transformCorners(Transform tr) {
		for (int i=0; i<8; i++) {
			float[] xyzS = tr.trVol2Screen(corners[i]);
			cornerT[i][0] = (int)xyzS[0]; 
			cornerT[i][1] = (int)xyzS[1];
			cornerT[i][2] = (int)xyzS[2]; 
		}

		for (int i=0; i<4; i++) {
			float[] xyzS = tr.trVol2Screen(cornersXY[i]);
			cornersXYT[i][0] = (int) xyzS[0]; 
			cornersXYT[i][1] = (int) xyzS[1];
			cornersXYT[i][2] = (int) xyzS[2]; 

			xyzS = tr.trVol2Screen(cornersYZ[i]);
			cornersYZT[i][0] = (int) xyzS[0]; 
			cornersYZT[i][1] = (int) xyzS[1];
			cornersYZT[i][2] = (int) xyzS[2]; 

			xyzS = tr.trVol2Screen(cornersXZ[i]);
			cornersXZT[i][0] = (int) xyzS[0]; 
			cornersXZT[i][1] = (int) xyzS[1];
			cornersXZT[i][2] = (int) xyzS[2]; 
		}
	}

	public void setTextAndLines(ImageRegion imageRegion) {

		if (imageRegion == null)
			return;
		for (int i=0; i<textPos.length; i++) {
			float[] xyzS = tr.trVol2Screen(textPos[i]);
			imageRegion.setTextPos(i, (int)xyzS[0], (int)xyzS[1], (int)xyzS[2]);
		}

		float zMax = -10000000;
		int iHidden = -1;
		for (int i = 0; i < 8; i++) {
			if (cornerT[i][2] > zMax) {
				zMax = cornerT[i][2];
				iHidden = i;
			}
		}

		int line= 0;
		for (int i = 0; i < 4; i++)
			for (int j = 4; j < 8; j++) {
				if (i+j != 7) {
					int z = (i == iHidden || j == iHidden) ? 1 : -1; 
					Color c = (z <= 0) ? frontColor : backColor;
					if (!control.showAxes) c = new Color(0, 0, 0, 0);
					imageRegion.setLine(line++, cornerT[i][0], cornerT[i][1], cornerT[j][0], cornerT[j][1], z, c);
				}
			}

		Color color = Color.red;
		int[][] corT = cornersXZT;
		for (int s = 0; s < 3; s++) {
			if (s == 1) { color = Color.green; 	corT = cornersYZT; }
			else if (s == 2){ color = Color.cyan; corT = cornersXYT; }

			zMax = -10000000;
			iHidden = -1;
			for (int i = 0; i < 4; i++) {
				if (corT[i][2] > zMax) {
					zMax = corT[i][2];
					iHidden = i;
				}
			}	
			for (int i = 0; i < 4; i++) {
				int ip1 = (i<3) ? i+1 : 0;
				int z = (i == iHidden || ip1 == iHidden) ? 1 : -1; 
				Color c = control.showSlices ? color : new Color(0, 0, 0, 0);
				imageRegion.setLine(line++, corT[i][0], corT[i][1], corT[ip1][0], corT[ip1][1], z, c);
			}
		}
	}

	boolean isInside(int x, int y) {

		int[] p = new int[3];
		p[0] = x;
		p[1] = y;

		if (inside(p, cornerT[0], cornerT[1], cornerT[4])) return true;
		if (inside(p, cornerT[0], cornerT[1], cornerT[5])) return true;
		if (inside(p, cornerT[2], cornerT[3], cornerT[6])) return true;
		if (inside(p, cornerT[2], cornerT[3], cornerT[7])) return true;

		if (inside(p, cornerT[1], cornerT[3], cornerT[5])) return true;
		if (inside(p, cornerT[1], cornerT[3], cornerT[7])) return true;
		if (inside(p, cornerT[0], cornerT[2], cornerT[4])) return true;
		if (inside(p, cornerT[0], cornerT[2], cornerT[6])) return true;

		if (inside(p, cornerT[1], cornerT[2], cornerT[4])) return true;
		if (inside(p, cornerT[1], cornerT[2], cornerT[7])) return true;
		if (inside(p, cornerT[0], cornerT[3], cornerT[5])) return true;
		if (inside(p, cornerT[0], cornerT[3], cornerT[6])) return true;

		return false;
	}

	private boolean inside(int[] p, int[] p1, int[] p2, int[] p3) {
		float x  = p[0];
		float y  = p[1];
		float x1 = p1[0];
		float y1 = p1[1];
		float x2 = p2[0];
		float y2 = p2[1];
		float x3 = p3[0];
		float y3 = p3[1];

		float a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
		float b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
		float c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);

		if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
			return true;
		return false; 
	}

	void findIntersections(float d) {
		numIntersections = 0;
		
		for (int i = 0; i < 4; i++) {
			for (int j = 4; j < 8; j++) {
				if (i+j != 7) 
					findIntersection(corners[i], corners[j], d);
			}
		}
		if (numIntersections == 0)
			numIntersections = 1;
		for (int i = numIntersections; i < getInterSections().length; i++) {
			getInterSections()[i][0] = getInterSections()[numIntersections-1][0];
			getInterSections()[i][1] = getInterSections()[numIntersections-1][1];
			getInterSections()[i][2] = getInterSections()[numIntersections-1][2];
		}
	}

	void findSliceIntersectionsXY(float d) {
		numIntersections = 0;
		findIntersection(cornersXY[0], cornersXY[1], d);
		findIntersection(cornersXY[1], cornersXY[2], d);
		findIntersection(cornersXY[2], cornersXY[3], d);
		findIntersection(cornersXY[3], cornersXY[0], d);
	}

	void findSliceIntersectionsYZ(float d) {
		numIntersections = 0;
		findIntersection(cornersYZ[0], cornersYZ[1], d);
		findIntersection(cornersYZ[1], cornersYZ[2], d);
		findIntersection(cornersYZ[2], cornersYZ[3], d);
		findIntersection(cornersYZ[3], cornersYZ[0], d);
	}

	void findIntersections_xz(float d) {
		numIntersections = 0;
		findIntersection(cornersXZ[0], cornersXZ[1], d);
		findIntersection(cornersXZ[1], cornersXZ[2], d);
		findIntersection(cornersXZ[2], cornersXZ[3], d);
		findIntersection(cornersXZ[3], cornersXZ[0], d);
	}

	/** Finds the intersection between a line (through p0 and p1) and the plane z = dist
	 * @param p0 Point 0
	 * @param p1 Point 1
	 */
	private void findIntersection(float[]p0, float[]p1, float d) {

		float[] xyzS = tr.trVol2Screen(p0);
		float z0 = xyzS[2]; 
		xyzS = tr.trVol2Screen(p1);
		float z1 = xyzS[2]; 

		if ((z0 - z1) != 0) {
			float t = (z0 - d) / ( z0 - z1);

			if (t >= 0 && t <= 1) {
				float x0 = p0[0];
				float y0 = p0[1];
				z0 = p0[2];
				float x1 = p1[0];
				float y1 = p1[1];
				z1 = p1[2];

				float xs = x0 + t*(x1-x0);
				float ys = y0 + t*(y1-y0);
				float zs = z0 + t*(z1-z0);
				xyzS = tr.trVol2Screen(xs, ys, zs);

				boolean newIntersection = true;

				for (int i = 0; i < numIntersections; i++){
					if (getInterSections()[i][0] == xs  && getInterSections()[i][1] == ys && getInterSections()[i][2] == zs)
						newIntersection = false;
				}
				if (newIntersection) {
					getInterSections()[numIntersections][0] = xs;
					getInterSections()[numIntersections][1] = ys;
					getInterSections()[numIntersections][2] = zs;
					numIntersections++;
				}
			}
		}
	}

	Cube (Control control, int widthV, int heightV, int depthV) {
		this.control = control;
		this.widthV = widthV;
		this.heightV = heightV;
		this.depthV = depthV;
		
		corners = new float[8][3];
		cornerT = new int[8][3]; 			// 8 x X, Y, Z
		setInterSections(new float[6][3]); 	// 6 x X, Y, Z
		cornersXY = new float[4][3];
		cornersYZ = new float[4][3];
		cornersXZ = new float[4][3];
		cornersXYT = new int[4][3];
		cornersYZT = new int[4][3];
		cornersXZT = new int[4][3];
		textPos = new float[4][3];
	}


	public float[][] getInterSections() {
		return interSections;
	}

	public void setInterSections(float interSections[][]) {
		this.interSections = interSections;
	}

	public void setTextPositions(float scale, float zAspect) {
		
		// 0
		textPos[0][0] = -Cube.dm/scale; 
		textPos[0][1] = -Cube.dm/scale; 
		textPos[0][2] = -Cube.dm/(scale*zAspect); 

		// x
		textPos[1][0] =  widthV + Cube.dp/scale; 
		textPos[1][1] = -Cube.dm/scale; 
		textPos[1][2] = -Cube.dm/(scale*zAspect); 

		// y
		textPos[2][0] = -Cube.dm/scale; 
		textPos[2][1] =  heightV + Cube.dp/scale; 
		textPos[2][2] = -Cube.dm/(scale*zAspect); 

		// z
		textPos[3][0] = -Cube.dm/scale; 
		textPos[3][1] = -Cube.dm/scale; 
		textPos[3][2] =  depthV + Cube.dp/(scale*zAspect); 	
	}

	public void setCornersYZ(float xV) {
		cornersYZ[0][0] = cornersYZ[1][0] = cornersYZ[2][0] = cornersYZ[3][0] = xV + 0.5f;
		transformCorners(tr);
	}

	public void setCornersXZ(float yV) {
		cornersXZ[0][1] = cornersXZ[1][1] = cornersXZ[2][1] = cornersXZ[3][1] = yV + 0.5f; 	
		transformCorners(tr);
	}

	public void setCornersXY(float zV) {
		cornersXY[0][2] = cornersXY[1][2] = cornersXY[2][2] = cornersXY[3][2] = zV + 0.5f;
		transformCorners(tr);
	}

	public void setSlicePositions(float positionFactorX, float positionFactorY, float positionFactorZ, float zAspect) {
		corners[1][1] =  heightV;     
		corners[1][2] =  depthV;     
		corners[2][0] =  widthV;     
		corners[2][2] =  depthV;     
		corners[3][0] =  widthV;     
		corners[3][1] =  heightV;     
		corners[4][2] =  depthV;     
		corners[5][1] =  heightV;     
		corners[6][0] =  widthV;     
		corners[7][0] =  widthV;     
		corners[7][1] =  heightV;     
		corners[7][2] =  depthV;     

		// Slice detection
		cornersXY[0][0] =  0;     
		cornersXY[1][0] =  widthV;     
		cornersXY[2][0] =  widthV;     
		cornersXY[3][0] =  0;     
		cornersXY[0][1] =  0;     
		cornersXY[1][1] =  0;     
		cornersXY[2][1] =  heightV;     
		cornersXY[3][1] =  heightV;     
		cornersXY[0][2] =  depthV*positionFactorZ-0.5f;
		cornersXY[1][2] =  depthV*positionFactorZ-0.5f;     
		cornersXY[2][2] =  depthV*positionFactorZ-0.5f;
		cornersXY[3][2] =  depthV*positionFactorZ-0.5f;

		cornersYZ[0][0] =  widthV*positionFactorX-0.5f;     
		cornersYZ[1][0] =  widthV*positionFactorX-0.5f;     
		cornersYZ[2][0] =  widthV*positionFactorX-0.5f;  
		cornersYZ[3][0] =  widthV*positionFactorX-0.5f;
		cornersYZ[0][1] =  0;     
		cornersYZ[1][1] =  0;     
		cornersYZ[2][1] =  heightV;     
		cornersYZ[3][1] =  heightV;     
		cornersYZ[0][2] =  0;
		cornersYZ[1][2] =  depthV;     
		cornersYZ[2][2] =  depthV;
		cornersYZ[3][2] =  0;

		cornersXZ[0][0] =  0;     
		cornersXZ[1][0] =  0;     
		cornersXZ[2][0] =  widthV;  
		cornersXZ[3][0] =  widthV;
		cornersXZ[0][1] =  heightV*positionFactorY-0.5f;     
		cornersXZ[1][1] =  heightV*positionFactorY-0.5f;     
		cornersXZ[2][1] =  heightV*positionFactorY-0.5f; 
		cornersXZ[3][1] =  heightV*positionFactorY-0.5f;     
		cornersXZ[0][2] =  0;
		cornersXZ[1][2] =  depthV;     
		cornersXZ[2][2] =  depthV;
		cornersXZ[3][2] =  0;

		// 0
		textPos[0][0] =  - Cube.dm; 
		textPos[0][1] =  - Cube.dm; 
		textPos[0][2] =  - Cube.dm/zAspect; 

		// x
		textPos[1][0] =   widthV + Cube.dp; 
		textPos[1][1] =  -Cube.dm; 
		textPos[1][2] =  -Cube.dm/zAspect; 

		// y
		textPos[2][0] =  -Cube.dm; 
		textPos[2][1] =   heightV + Cube.dp; 
		textPos[2][2] =  -Cube.dm/zAspect; 

		// z
		textPos[3][0] =  -Cube.dm; 
		textPos[3][1] =  -Cube.dm; 
		textPos[3][2] =   depthV + Cube.dp/zAspect; 	
	}

	public float[][] getCorners() {
		return corners;
	}

	public void setTransform(Transform tr) {
		this.tr = tr;
	}

}
