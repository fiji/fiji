/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

class Transform {
	private Control control;

	private float mP[][];

	private float xRot, yRot, zRot;		// rotation vector
	private float rotAngle=0;			// rotation angle

	private float xSOff; 	  	// screen center
	private float ySOff; 
	private float xOff; 	  	// screen center
	private float yOff; 

	private float xvOff;		// volume center		
	private float yvOff;
	private float zvOff;

	private float scale = 1;
	private float zAspect = 1;

	private float a00,  a01,  a02,  a03;  // coefficients of the tramsformation
	private float a10,  a11,  a12,  a13;
	private float a20,  a21,  a22,  a23;
	private float ai00, ai01, ai02, ai03;  // coefficients of the inverse tramsformation
	private float ai10, ai11, ai12, ai13;
	private float ai20, ai21, ai22, ai23;

	private float degreeX;
	private float degreeY;
	private float degreeZ;

	void initializeTransformation() {

		if (control.LOG) System.out.println("initializeTransformation");

		float m[][] = new float[4][4];

		// translation & scale
		float s = scale;
		m[0][0] = s; 	m[0][1] = 0; 	m[0][2] = 0;				m[0][3] = -s*xvOff;
		m[1][0] = 0;	m[1][1] = s;	m[1][2] = 0;				m[1][3] = -s*yvOff; 
		m[2][0] = 0;	m[2][1] = 0;	m[2][2] = s*zAspect; 		m[2][3] = -s*zAspect*zvOff;
		m[3][0] = 0;	m[3][1] = 0;	m[3][2] = 0; 				m[3][3] = 1;

		float mR[][] = new float[4][4];

		// rotation from mouse drag
		float u = xRot;
		float v = yRot;
		float w = zRot;

		float u2=u*u;
		float v2=v*v;
		float w2=w*w;

		float cos = (float)Math.cos(rotAngle);
		float sin = (float)Math.sin(rotAngle);
		rotAngle = 0;

		mR[0][0] = u2 + (1-u2)*cos;	
		mR[0][1] = u*v*(1-cos) - w*sin;	
		mR[0][2] = u*w*(1-cos) + v*sin;			
		mR[1][0] = u*v*(1-cos) + w*sin;	
		mR[1][1] = v2 + (1-v2)*cos;	
		mR[1][2] = v*w*(1-cos) - u*sin;			
		mR[2][0] = u*w*(1-cos) - v*sin;	    
		mR[2][1] = v*w*(1-cos) + u*sin;		
		mR[2][2] = w2 + (1-w2)*cos;			
		mR[3][3] = 1;

		float m_RP[][] = new float[4][4];
		matProd(m_RP, mR, mP);
		matCopy4(mP, m_RP);	// remember the previous rotation

		double ar=0, br=0, gr=0;
		if (mP[2][0] != 1 && mP[2][0] != -1) {
			br = -Math.asin(mP[2][0]);
			double cosbr = Math.cos(br);
			ar = Math.atan2(mP[2][1]/cosbr, mP[2][2]/cosbr);
			gr = Math.atan2(mP[1][0]/cosbr, mP[0][0]/cosbr);
		}
		else {
			gr = 0;
			if (mP[2][0] == -1) {
				br = Math.PI/2;
				ar = Math.atan2(mP[0][1], mP[0][2]);
			}
			else {
				br = -Math.PI/2;
				ar = Math.atan2(-mP[0][1], -mP[0][2]);
			}
		}

		degreeX = (float) Math.toDegrees(ar);
		degreeY = (float) Math.toDegrees(br);
		degreeZ = (float) Math.toDegrees(gr);

		float mat[][] = new float[4][4];
		matProd(mat, mP, m); 

		a00 =  mat[0][0];
		a01 =  mat[0][1];
		a02 =  mat[0][2];
		a03 =  mat[0][3] + xSOff + xOff*scale;

		a10 =  mat[1][0];
		a11 =  mat[1][1];
		a12 =  mat[1][2];
		a13 =  mat[1][3] + ySOff  + yOff*scale; 

		a20 =  mat[2][0];
		a21 =  mat[2][1];
		a22 =  mat[2][2];
		a23 =  mat[2][3];

		float matInv[][] = new float[4][4];
		matInv4(matInv, mat);

		ai00 = matInv[0][0];
		ai01 = matInv[0][1];
		ai02 = matInv[0][2];
		ai03 = matInv[0][3];

		ai10 = matInv[1][0];
		ai11 = matInv[1][1];
		ai12 = matInv[1][2];
		ai13 = matInv[1][3]; 

		ai20 = matInv[2][0];
		ai21 = matInv[2][1];
		ai22 = matInv[2][2];
		ai23 = matInv[2][3];
	}

	public float getDegreeX() {
		return degreeX;
	}

	public float getDegreeY() {
		return degreeY;
	}

	public float getDegreeZ() {
		return degreeZ;
	}

	void matProd(float z[][], float u[][], float v[][]) {
		int i, j, k;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) {
				z[i][j]=0.0f;
				for (k=0; k<4; k++) 
					z[i][j]+=u[i][k]*v[k][j];
			}
	}

	public void setView(double angleX, double angleY, double angleZ) {
		// calculate new cos and sins 
		float cosX = (float)Math.cos(angleX); 
		float sinX = (float)Math.sin(angleX);
		float cosY = (float)Math.cos(angleY); 
		float sinY = (float)Math.sin(angleY);
		float cosZ = (float)Math.cos(angleZ);
		float sinZ = (float)Math.sin(angleZ);			

		// x rotation
		float mX[][] = new float[4][4];
		mX[0][0] =  1;   	mX[0][1] =  0;		mX[0][2] =  0;			mX[0][3] =  0;
		mX[1][0] =  0;		mX[1][1] =  cosX;	mX[1][2] = -sinX;		mX[1][3] =  0; 
		mX[2][0] =  0;		mX[2][1] =  sinX;	mX[2][2] =  cosX;		mX[2][3] =  0;
		mX[3][0] =  0;		mX[3][1] =  0;		mX[3][2] =  0;			mX[3][3] =  1;

		// y rotation
		float mY[][] = new float[4][4];
		mY[0][0] =  cosY;   mY[0][1] =  0; 		mY[0][2] =  sinY;		mY[0][3] =  0;
		mY[1][0] =  0;		mY[1][1] =  1;		mY[1][2] =  0;			mY[1][3] =  0; 
		mY[2][0] = -sinY;	mY[2][1] =  0;		mY[2][2] =  cosY;		mY[2][3] =  0;
		mY[3][0] =  0;		mY[3][1] =  0;		mY[3][2] =  0;			mY[3][3] =  1;

		// z rotation
		float mZ[][] = new float[4][4];
		mZ[0][0] =  cosZ;	mZ[0][1] = -sinZ;	mZ[0][2] =  0;			mZ[0][3] =  0;
		mZ[1][0] =  sinZ;	mZ[1][1] =  cosZ;	mZ[1][2] =  0;			mZ[1][3] =  0; 
		mZ[2][0] =  0;	    mZ[2][1] =  0;		mZ[2][2] =  1;			mZ[2][3] =  0;
		mZ[3][0] =  0;		mZ[3][1] =  0;		mZ[3][2] =  0;			mZ[3][3] =  1;

		float m_XY[][] = new float[4][4];
		matProd(m_XY,  mY, mX);
		matProd(mP, mZ, m_XY);

		initializeTransformation();	
	}

	/* 4x4 matrix inverse */
	void matInv4(float z[][], float u[][]) {
		int    i, j, n, ii[] = new int[4];
		float f;
		float w[][] = new float[4][4];
		n=4;
		matCopy4(w,u);
		matUnit4(z);
		for (i=0; i<n; i++) {
			ii[i]=matge4(w,i);
			matXr4(w,i,ii[i]);
			for (j=0; j<n; j++) {
				if (i==j) continue;
				f=-w[i][j]/w[i][i];
				matAc4(w,j,j,f,i);
				matAc4(z,j,j,f,i);
			}
		}
		for (i=0; i<n; i++) 
			matMc4(z,1.0f/w[i][i],i);
		for (i=0; i<n; i++) {
			j=n-i-1; 
			matXc4(z,j,ii[j]);
		}
	}

	/* greatest element in the nth column of 4x4 matrix */
	int matge4(float p[][], int n) {
		float g, h; 
		int m;
		m=n;
		g=p[n][n];
		g=(g<0.0?-g:g);
		for (int i=n; i<4; i++) {
			h=p[i][n];
			h=(h<0.0?-h:h);
			if (h<g) continue;
			g=h; m=i;
		}
		return m;
	}

	/* copy 4x4 matrix */
	void matCopy4(float z[][], float x[][]) {
		int i, j;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) 
				z[i][j]=x[i][j];
	}

	/* 4x4 unit matrix */
	void matUnit4(float z[][]) {
		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) 
				z[i][j]=0.0f;
			z[i][i]=1.0f;
		}
	}

	/* exchange ith and jth columns of a 4x4 matrix */
	void matXc4(float z[][], int i, int j) {
		float t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[k][i]; 
			z[k][i]=z[k][j]; 
			z[k][j]=t;}
	}

	/* exchange ith and jth rows of a 4x4 matrix */
	void matXr4(float z[][], int i, int j) {
		float t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[i][k]; 
			z[i][k]=z[j][k]; 
			z[j][k]=t;
		}
	}

	/* augment column of a 4x4 matrix */
	void matAc4(float z[][], int i, int j, float f, int k) {
		int l;
		for (l=0; l<4; l++) 
			z[l][i] = z[l][j] + f*z[l][k];
	}

	/* multiply ith column of 4x4 matrix by a factor */
	void matMc4(float z[][], float f, int i) {
		int j;
		for (j=0; j<4; j++) 
			z[j][i]*=f;
	}

	final float[] trVol2Screen(float[] xyzV) {
		float xV = xyzV[0];
		float yV = xyzV[1];
		float zV = xyzV[2];
		float[] xyzS = new float[3];
		xyzS[0] = a00*xV + a01*yV + a02*zV + a03;
		xyzS[1] = a10*xV + a11*yV + a12*zV + a13;
		xyzS[2] = a20*xV + a21*yV + a22*zV + a23;
		return xyzS;
	}

	final float[]  trVol2Screen(float xV, float yV, float zV) {
		float[] xyzS = new float[3];
		xyzS[0] = a00*xV + a01*yV + a02*zV + a03;
		xyzS[1] = a10*xV + a11*yV + a12*zV + a13;
		xyzS[2] = a20*xV + a21*yV + a22*zV + a23;
		return xyzS;
	}

	final float[] trScreen2Vol(float xS, float yS, float zS) {
		xS -= xSOff + xOff*scale;
		yS -= ySOff + yOff*scale;
		float[] xyzV = new float[3];
		xyzV[0] = ai00*xS + ai01*yS + ai02*zS + ai03;
		xyzV[1] = ai10*xS + ai11*yS + ai12*zS + ai13;
		xyzV[2] = ai20*xS + ai21*yS + ai22*zS + ai23;
		return xyzV;
	}

	final float[] screen2Volume(float[] xyzS) {
		float xS = xyzS[0] - xSOff - xOff*scale;
		float yS = xyzS[1] - ySOff - yOff*scale;
		float zS = xyzS[2];
		float[] xyzV = new float[3];
		xyzV[0] = ai00*xS + ai01*yS + ai02*zS + ai03;
		xyzV[1] = ai10*xS + ai11*yS + ai12*zS + ai13;
		xyzV[2] = ai20*xS + ai21*yS + ai22*zS + ai23;
		return xyzV;
	}

	public void setScale(float scale) {
		this.scale = scale;		
		initializeTransformation();
	}

	// virtual trackball
	public void setMouseMovement(int xAct, int yAct, int xStart, int yStart, float width) {
		//if (control.LOG) System.out.println("setMouseMovement");

		float size = 2*width;

		float x1 = (xStart - width/2f)/size;
		float y1 = (yStart - width/2f)/size;
		float z1 = 1-x1*x1-y1*y1;
		z1 = (z1 > 0) ? (float) Math.sqrt(z1) : 0;

		float x2 = (xAct - width/2f)/size;
		float y2 = (yAct - width/2f)/size;
		float z2 = 1-x2*x2-y2*y2;
		z2 = (z2>0) ? (float) Math.sqrt(z2) : 0;

		// Cross product
		xRot = y1 * z2 - z1 * y2;
		yRot = z1 * x2 - x1 * z2;
		zRot = -x1 * y2 + y1 * x2;
		float len = xRot*xRot+yRot*yRot+zRot*zRot;
		if (len <= 0)
			return;

		float len_ = (float) (1./Math.sqrt(xRot*xRot+yRot*yRot+zRot*zRot));
		xRot *= len_;
		yRot *= len_;
		zRot *= len_;

		float dot = x1*x2+y1*y2+z1*z2;
		if (dot > 1) dot = 1;
		else if (dot < -1) dot = -1;
		rotAngle = (float) Math.acos(dot)*10;
		//System.out.println("xRot " + xRot + " yRot " + yRot + " zRot " + zRot + " rotAngle " + rotAngle);

		initializeTransformation();	
	}

	public void setMouseMovementOffset(int dx, int dy) {
		xOff += dx/scale;
		yOff += dy/scale;

		initializeTransformation();
	}

	Transform(Control control, float width, float height, float xOffa, float yOffa, float zOffa) {
		this.control = control;
		xSOff = (float)(width/2.  + 0.5);
		ySOff = (float)(height/2. + 0.5);

		xvOff = xOffa;
		yvOff = yOffa;
		zvOff = zOffa;

		mP = new float[4][4];
		mP[0][0] = mP[1][1] = mP[2][2] = mP[3][3] = 1;
	}

	public void setZAspect(float zAspect) {
		this.zAspect = zAspect;
	};
}
