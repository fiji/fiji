package jRenderer3D;

class Transform {
	
	// direction of z				// zOrientation = -1 means z is pointing out of the screen
	private int zOrientation = -1; 	// zOrientation =  1 means z is pointing into the screen
			
	
	Transform(int width, int height ) {
		xs = (double)(width/2.  + 0.5);
		ys = (double)(height/2. + 0.5);
		
		initializeTransformation();
	};
	
	protected void transform(SurfacePlotData plotItem) {
		y = plotItem.y;
		x = plotItem.x;
		z = plotItem.z;
		xyzPos();
	}
	
	protected void transform(Text3D textItem) {
		x = textItem.x;
		y = textItem.y;
		z = textItem.z;
		xyzPos();
	}
	
	protected void transform(double x_, double y_, double z_) {
		x = x_;
		y = y_;
		z = z_;
		xyzPos_noShift();
	}
	
	protected void transform(Point3D pt) {
		x = pt.x;
		y = pt.y;
		z = pt.z;
		xyzPos();
	}
	
	protected void setScale(double scale) {
		this.scale = scale;		
		initializeTransformation();
	}
	
	protected void setZOrientation(int zOrientation) {
		if (zOrientation <= 0)
			this.zOrientation = -1;
		else
			this.zOrientation = 1;
		initializeTransformation();
	}
	

	protected void setZAspectRatio(double zAspectRatio) {
		this.zAspect =  zAspectRatio;
		initializeTransformation();
	}

	
//	public void setMouseMovementOffset(int dx, int dy) {
//		xoff += dx;
//		yoff += dy;
//		
//		initializeTransformation();
//	}
	
	protected void setRotationZ(double angleZ) {
		this.angleZ = angleZ;
		initializeTransformation();
	}
	
	protected void setRotationY(double angleY) {
		this.angleY = angleY;
		initializeTransformation();
	}
	
	protected void setRotationX(double angleX) {
		this.angleX = angleX;
		initializeTransformation();
	}
	
	protected void setRotationXYZ(double angleX, double angleY, double angleZ) {
		this.angleX = angleX;
		this.angleY = angleY;
		this.angleZ = angleZ;
		initializeTransformation();
	}
	
	protected void setRotationXZ(double angleX, double angleZ) {
		this.angleX = angleX;
		this.angleZ = angleZ;
		initializeTransformation();	
	}
	
	protected double getRotationX() {
		return angleX;
	}
	
	protected double getRotationY() {
		return angleY;
	}
	
	protected double getRotationZ() {
		return angleZ;
	}
	
	protected void changeRotationXZ(double dx, double dz) {
		angleX += dx;
		angleZ += dz;
		
		initializeTransformation();			
	}
	protected void changeRotationXYZ(double dx, double dy, double dz) {
		angleX += dx;
		angleY += dy;
		angleZ += dz;
		
		initializeTransformation();			
	}
	protected void setOffsets(double xOff, double yOff, double zOff) {
		xO = -xOff;
		yO = -yOff;
		zO = -zOff;
		initializeTransformation();			
	}
		
	
//	// translation
//	matrix[0][0] = 1; 	matrix[0][1] = 0; 		matrix[0][2] = 0;		matrix[0][3] = xO;
//	matrix[1][0] = 0;	matrix[1][1] = 1;		matrix[1][2] = 0;		matrix[1][3] = yO; 
//	matrix[2][0] = 0;	matrix[2][1] = 0;		matrix[2][2] = zOrientation; 	matrix[2][3] = zOrientation*zO;
//	matrix[3][0] = 0;	matrix[3][1] = 0;		matrix[3][2] = 0; 		matrix[3][3] = 1;
//	
//	// scaling		
//	mS[0][0] =  s;  	mS[0][1] =  0;			mS[0][2] =  0;				mS[0][3] = 0;
//	mS[1][0] =  0;		mS[1][1] =  s;			mS[1][2] =  0;				mS[1][3] = 0;
//	mS[2][0] =  0;		mS[2][1] =  0;			mS[2][2] =  s*zAspectRatio;	mS[2][3] = 0;
//	mS[3][0] =  0;		mS[3][1] =  0;			mS[3][2] =  0;				mS[3][3] = 1;
//	matProd(matrix, mS, matrix);
//
//	// z rotation
//	mZ[0][0] =  cosZ;	mZ[0][1] =  sinZ;	mZ[0][2] =  0;			mZ[0][3] =  0;
//	mZ[1][0] = -sinZ;	mZ[1][1] =  cosZ;	mZ[1][2] =  0;			mZ[1][3] =  0; 
//	mZ[2][0] =  0;	    mZ[2][1] =  0;		mZ[2][2] =  1;			mZ[2][3] =  0;
//	mZ[3][0] =  0;		mZ[3][1] =  0;		mZ[3][2] =  0;			mZ[3][3] =  1;
//	
//	// y rotation
//	mY[0][0] =  cosY;   mY[0][1] =  0; 		mY[0][2] =  -sinY;		mY[0][3] =  0;
//	mY[1][0] =  0;		mY[1][1] =  1;		mY[1][2] =  0;			mY[1][3] =  0; 
//	mY[2][0] =  sinY;	mY[2][1] =  0;		mY[2][2] =  cosY;		mY[2][3] =  0;
//	mY[3][0] =  0;		mY[3][1] =  0;		mY[3][2] =  0;			mY[3][3] =  1;
//	
//	// x rotation
//	mX[0][0] =  1;   	mX[0][1] =  0;		mX[0][2] =  0;			mX[0][3] =  0;
//	mX[1][0] =  0;		mX[1][1] =  cosX;	mX[1][2] =  sinX;		mX[1][3] =  0; 
//	mX[2][0] =  0;		mX[2][1] =  -sinX;	mX[2][2] =  cosX;		mX[2][3] =  0;
//	mX[3][0] =  0;		mX[3][1] =  0;		mX[3][2] =  0;			mX[3][3] =  1;
//
//	m_XYZ[0][3] +=  xs;  // Screen offsets
//	m_XYZ[1][3] +=  ys;


	private void initializeTransformation() {
		
		//System.out.println(Math.toDegrees(angleX) + " " + Math.toDegrees(angleY) + " " + Math.toDegrees(angleZ));
		//System.out.println(angleX + " " + angleY + " " + angleZ);
		
		// calculate new cos and sins 
		cosX = (double)Math.cos(angleX); 
		sinX = (double)Math.sin(angleX);
		cosY = (double)Math.cos(angleY); 
		sinY = (double)Math.sin(angleY);
		cosZ = (double)Math.cos(angleZ);
		sinZ = (double)Math.sin(angleZ);
		

		double s = 1; //scale;

		// translation & scale
		m[0][0] = s; 	m[0][1] = 0; 	m[0][2] = 0;				m[0][3] = s*xO;
		m[1][0] = 0;	m[1][1] = s;	m[1][2] = 0;				m[1][3] = s*yO; 
		m[2][0] = 0;	m[2][1] = 0;	m[2][2] = s*zOrientation*zAspect; 	m[2][3] = s*zOrientation*zAspect*zO;
		m[3][0] = 0;	m[3][1] = 0;	m[3][2] = 0; 				m[3][3] = 1;
		
		// z rotation
		mZ[0][0] =  cosZ;	mZ[0][1] =  sinZ;	mZ[0][2] =  0;			mZ[0][3] =  0;
		mZ[1][0] = -sinZ;	mZ[1][1] =  cosZ;	mZ[1][2] =  0;			mZ[1][3] =  0; 
		mZ[2][0] =  0;	    mZ[2][1] =  0;		mZ[2][2] =  1;			mZ[2][3] =  0;
		mZ[3][0] =  0;		mZ[3][1] =  0;		mZ[3][2] =  0;			mZ[3][3] =  1;
		
		// y rotation
		mY[0][0] =  cosY;   mY[0][1] =  0; 		mY[0][2] =  -sinY;		mY[0][3] =  0;
		mY[1][0] =  0;		mY[1][1] =  1;		mY[1][2] =  0;			mY[1][3] =  0; 
		mY[2][0] =  sinY;	mY[2][1] =  0;		mY[2][2] =  cosY;		mY[2][3] =  0;
		mY[3][0] =  0;		mY[3][1] =  0;		mY[3][2] =  0;			mY[3][3] =  1;
		
		// x rotation
		mX[0][0] =  1;   	mX[0][1] =  0;		mX[0][2] =  0;			mX[0][3] =  0;
		mX[1][0] =  0;		mX[1][1] =  cosX;	mX[1][2] =  sinX;		mX[1][3] =  0; 
		mX[2][0] =  0;		mX[2][1] =  -sinX;	mX[2][2] =  cosX;		mX[2][3] =  0;
		mX[3][0] =  0;		mX[3][1] =  0;		mX[3][2] =  0;			mX[3][3] =  1;
		
//		double fl = 400;
//		double d = 400;
		
		// Orig
//		//	perspective
//		mP[0][0] =  fl;   	mP[0][1] =  0;		mP[0][2] =  xs;			mP[0][3] =  d*xs;
//		mP[1][0] =  0;		mP[1][1] =  fl;		mP[1][2] =  ys;			mP[1][3] =  d*ys; 
//		mP[2][0] =  0;		mP[2][1] =  0;		mP[2][2] =  fl;			mP[2][3] =  0;     // falsch
//		mP[3][0] =  0;		mP[3][1] =  0;		mP[3][2] =  1;			mP[3][3] =  d;
		
		
//		//	noperspective
//		mP[0][0] =  1;   	mP[0][1] =  0;		mP[0][2] =  0;			mP[0][3] =  0;
//		mP[1][0] =  0;		mP[1][1] =  1;		mP[1][2] =  0;			mP[1][3] =  0; 
//		mP[2][0] =  0;		mP[2][1] =  0;		mP[2][2] =  1;			mP[2][3] =  0;     // falsch
//		mP[3][0] =  0;		mP[3][1] =  0;		mP[3][2] =  0;			mP[3][3] =  1;
		

		matProd(m_Z,   mZ, m);
		matProd(m_YZ,  mY, m_Z);
		matProd(m_XYZ, mX, m_YZ);

//		matProd(m_PXYZ, mP, m_XYZ);
		
		a00 =  m_XYZ[0][0];
		a01 =  m_XYZ[0][1];
		a02 =  m_XYZ[0][2];
		a03 =  m_XYZ[0][3];
		
		a10 =  m_XYZ[1][0];
		a11 =  m_XYZ[1][1];
		a12 =  m_XYZ[1][2];
		a13 =  m_XYZ[1][3]; 
		
		a20 =  m_XYZ[2][0];
		a21 =  m_XYZ[2][1];
		a22 =  m_XYZ[2][2];
		a23 =  m_XYZ[2][3];
		
//		a30 =  m_XYZ[3][0];
//		a31 =  m_XYZ[3][1];
//		a32 =  m_XYZ[3][2];
//		a33 =  m_XYZ[3][3];

//		for (int i = 0; i < m_XYZ.length; i++) {
//			for (int j = 0; j < m_XYZ[0].length; j++) {
//				System.out.print(((int)(100*m_XYZ[i][j]))/100. +" ");
//			}
//			System.out.println();
//		}
//		System.out.println();
		

		matInv4(m_XYZInv, m_XYZ);
		
		ai00 = m_XYZInv[0][0];
		ai01 = m_XYZInv[0][1];
		ai02 = m_XYZInv[0][2];
		ai03 = m_XYZInv[0][3];
		
		ai10 = m_XYZInv[1][0];
		ai11 = m_XYZInv[1][1];
		ai12 = m_XYZInv[1][2];
		ai13 = m_XYZInv[1][3]; 
		
		ai20 = m_XYZInv[2][0];
		ai21 = m_XYZInv[2][1];
		ai22 = m_XYZInv[2][2];
		ai23 = m_XYZInv[2][3];
		
//		ai30 = m_XYZInv[3][0];
//		ai31 = m_XYZInv[3][1];
//		ai32 = m_XYZInv[3][2];
//		ai33 = m_XYZInv[3][3];
	}
	
	
	public void rotateTransformation(double angle_X, double angle_Y, double angle_Z) {
		
		// calculate new cos and sins 
		cosX = (double)Math.cos(angle_X); 
		sinX = (double)Math.sin(angle_X);
		cosY = (double)Math.cos(angle_Y); 
		sinY = (double)Math.sin(angle_Y);
		cosZ = (double)Math.cos(angle_Z);
		sinZ = (double)Math.sin(angle_Z);
		
		
		m_[0][0] = m_XYZ[0][0]; m_[0][1] = m_XYZ[0][1]; m_[0][2] = m_XYZ[0][2];	m_[0][3] = m_XYZ[0][3];
		m_[1][0] = m_XYZ[1][0];	m_[1][1] = m_XYZ[1][1];	m_[1][2] = m_XYZ[1][2];	m_[1][3] = m_XYZ[1][3]; 
		m_[2][0] = m_XYZ[2][0];	m_[2][1] = m_XYZ[2][1];	m_[2][2] = m_XYZ[2][2]; m_[2][3] = m_XYZ[2][3];
		m_[3][0] = m_XYZ[3][0];	m_[3][1] = m_XYZ[3][1];	m_[3][2] = m_XYZ[3][2]; m_[3][3] = m_XYZ[3][3];
		
		// z rotation
		mZ[0][0] =  cosZ;	mZ[0][1] =  sinZ;	mZ[0][2] =  0;			mZ[0][3] =  0;
		mZ[1][0] = -sinZ;	mZ[1][1] =  cosZ;	mZ[1][2] =  0;			mZ[1][3] =  0; 
		mZ[2][0] =  0;	    mZ[2][1] =  0;		mZ[2][2] =  1;			mZ[2][3] =  0;
		mZ[3][0] =  0;		mZ[3][1] =  0;		mZ[3][2] =  0;			mZ[3][3] =  1;
		
		// y rotation
		mY[0][0] =  cosY;   mY[0][1] =  0; 		mY[0][2] =  -sinY;		mY[0][3] =  0;
		mY[1][0] =  0;		mY[1][1] =  1;		mY[1][2] =  0;			mY[1][3] =  0; 
		mY[2][0] =  sinY;	mY[2][1] =  0;		mY[2][2] =  cosY;		mY[2][3] =  0;
		mY[3][0] =  0;		mY[3][1] =  0;		mY[3][2] =  0;			mY[3][3] =  1;
		
		// x rotation
		mX[0][0] =  1;   	mX[0][1] =  0;		mX[0][2] =  0;			mX[0][3] =  0;
		mX[1][0] =  0;		mX[1][1] =  cosX;	mX[1][2] =  sinX;		mX[1][3] =  0; 
		mX[2][0] =  0;		mX[2][1] =  -sinX;	mX[2][2] =  cosX;		mX[2][3] =  0;
		mX[3][0] =  0;		mX[3][1] =  0;		mX[3][2] =  0;			mX[3][3] =  1;
		
		//matProd(m_Z,   mZ, m);
		matProd(m_YZ,  mY, mZ);
		matProd(m_Z, mX, m_YZ);
		matProd(m_XYZ, m_Z, m_);
		

		a00 =  m_XYZ[0][0];
		a01 =  m_XYZ[0][1];
		a02 =  m_XYZ[0][2];
		a03 =  m_XYZ[0][3];
		
		a10 =  m_XYZ[1][0];
		a11 =  m_XYZ[1][1];
		a12 =  m_XYZ[1][2];
		a13 =  m_XYZ[1][3]; 
		
		a20 =  m_XYZ[2][0];
		a21 =  m_XYZ[2][1];
		a22 =  m_XYZ[2][2];
		a23 =  m_XYZ[2][3];

		matInv4(m_XYZInv, m_XYZ);
		
		ai00 = m_XYZInv[0][0];
		ai01 = m_XYZInv[0][1];
		ai02 = m_XYZInv[0][2];
		ai03 = m_XYZInv[0][3];
		
		ai10 = m_XYZInv[1][0];
		ai11 = m_XYZInv[1][1];
		ai12 = m_XYZInv[1][2];
		ai13 = m_XYZInv[1][3]; 
		
		ai20 = m_XYZInv[2][0];
		ai21 = m_XYZInv[2][1];
		ai22 = m_XYZInv[2][2];
		ai23 = m_XYZInv[2][3];
		
//		ai30 = m_XYZInv[3][0];
//		ai31 = m_XYZInv[3][1];
//		ai32 = m_XYZInv[3][2];
//		ai33 = m_XYZInv[3][3];
	}
	
	// Orig
//	private final void invxyzPosf() {
//		xf = ai00*X + ai01*Y + ai02*Z + ai03;
//		yf = ai10*X + ai11*Y + ai12*Z + ai13;
//		zf = ai20*X + ai21*Y + ai22*Z + ai23;
//
//		double w = ai30*X + ai31*Y + ai32*Z + ai33;
//		
//		xf/= w;
//		yf/= w;
//		zf/= w;
//	}
//	private final void xyzPos() {
//		X = (a00*x + a01*y + a02*z + a03);
//		Y = (a10*x + a11*y + a12*z + a13);
//		Z = (a20*x + a21*y + a22*z + a23);
//		
//		double W = (a30*x + a31*y + a32*z + a33);
//		
//		X/= W;
//		Y/= W;
//	//	Z/= W;
//
//	}

	
//	private final void invxyzPos() {
//		double sz = (Z+perspective)/(perspective*scale);
//		X = (X - xs)*sz;
//		Y = (Y - ys)*sz;
//		
//		xf = ai00*X + ai01*Y + ai02*Z + ai03;
//		yf = ai10*X + ai11*Y + ai12*Z + ai13;
//		zf = ai20*X + ai21*Y + ai22*Z + ai23;
//	}
//	
//	
//	private final void xyzPos() {
//		X = a00*x + a01*y + a02*z + a03;
//		Y = a10*x + a11*y + a12*z + a13;
//		Z = a20*x + a21*y + a22*z + a23;
//		
//		double sz = scale * perspective /(Z + perspective);
//		
//		X = sz*X + xs;
//		Y = sz*Y + ys;
//	}

	private final void xyzPos() {
		X = a00*x + a01*y + a02*z + a03;
		Y = a10*x + a11*y + a12*z + a13;
		Z = a20*x + a21*y + a22*z + a23;
		
		double sz = scale * maxDistance /(maxDistance + perspective*Z);
		
		X = sz*X + xs;
		Y = sz*Y + ys;
	}
	
	private final void xyzPos_noShift() {
		X = a00*x + a01*y + a02*z + a03;
		Y = a10*x + a11*y + a12*z + a13;
		Z = a20*x + a21*y + a22*z + a23;
	}


	private final void invxyzPos() {
		double sz = (maxDistance + perspective*Z)/(scale * maxDistance);
		
		X = (X - xs)*sz;
		Y = (Y - ys)*sz;
		
		x = ai00*X + ai01*Y + ai02*Z + ai03;
		y = ai10*X + ai11*Y + ai12*Z + ai13;
		z = ai20*X + ai21*Y + ai22*Z + ai23;
	}
	
	
	
	/* 4x4 matrix inverse */
	void matInv4(double z[][], double u[][]) {
		int    i, j, n, ii[] = new int[4];
		double f;
		double w[][] = new double[4][4];
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
	int matge4(double p[][], int n) {
		double g, h; 
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
	void matCopy4(double z[][], double x[][]) {
		int i, j;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) 
				z[i][j]=x[i][j];
	}
	
	/* 4x4 unit matrix */
	void matUnit4(double z[][]) {
		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) 
				z[i][j]=0.0f;
			z[i][i]=1.0f;
		}
	}
	
	/* exchange ith and jth columns of a 4x4 matrix */
	void matXc4(double z[][], int i, int j) {
		double t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[k][i]; 
			z[k][i]=z[k][j]; 
			z[k][j]=t;}
	}
	
	/* exchange ith and jth rows of a 4x4 matrix */
	void matXr4(double z[][], int i, int j) {
		double t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[i][k]; 
			z[i][k]=z[j][k]; 
			z[j][k]=t;
		}
	}
	
	/* extract nth column from 4x4 matrix */
	void matXtc4(double p[], double z[][], int n) {
		int i;
		for (i=0; i<4; i++) 
			p[i]=z[i][n];
	}
	
	/* augment column of a 4x4 matrix */
	void matAc4(double z[][], int i, int j, double f, int k) {
		int l;
		for (l=0; l<4; l++) 
			z[l][i] = z[l][j] + f*z[l][k];
	}
	
	/* multiply ith column of 4x4 matrix by a factor */
	void matMc4(double z[][], double f, int i) {
		int j;
		for (j=0; j<4; j++) 
			z[j][i]*=f;
	}
	
	/* product of two 4x4 matrices */
	// z = x * y
//	void matProd(double z[][], double x[][], double y[][]) {
//		int i, j, k;
//		double u[][] = new double[4][4];
//		double v[][] = new double[4][4];
//		
//		matCopy4(u,x);
//		matCopy4(v,y);
//		for (i=0; i<4; i++) 
//			for (j=0; j<4; j++) {
//				z[i][j]=0.0f;
//				for (k=0; k<4; k++) 
//					z[i][j]+=u[i][k]*v[k][j];
//			}
//	}
	void matProd(double z[][], double u[][], double v[][]) {
		int i, j, k;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) {
				z[i][j]=0.0f;
				for (k=0; k<4; k++) 
					z[i][j]+=u[i][k]*v[k][j];
			}
	}
	
	final void xyzPos(int[] xyz) {
		x = xyz[0];
		y = xyz[1];
		z = xyz[2];
		xyzPos();
	}
	
	final double getScalarProduct() {
		return cosZ*x + sinZ*y;
		//return -cosZ*y - sinZ*x;
	}
		
	final void invxyzPosf(int[] XYZ) {
		X = XYZ[0];
		Y = XYZ[1];
		Z = XYZ[2];
		invxyzPos();
	}
	final void invxyzPosf(double[] XYZ) {
		X = XYZ[0];
		Y = XYZ[1];
		Z = XYZ[2];
		invxyzPos();
	}
		
	private double angleX = 0;   // angle for X-rotation
	private double angleY = 0;
	private double angleZ = 0; 	// angle for Z-rotation
//	public double angleX = 0;   // angle for X-rotation
//	public double angleY = 0;
//	public double angleZ = 0; 	// angle for Z-rotation
	
	private double xs = 256; 
	private double ys = 256; 
	
	private double cosZ; // = (double)Math.cos(angleB), 
	private double sinZ; // = (double)Math.sin(angleB);
	private double cosX; // = (double)Math.cos(angleR); 
	private double sinX; // = (double)Math.sin(angleR);
	private double cosY;
	private double sinY;

	
	private double scale = 1;
	private double zAspect = 1;
	
	
	double m[][] = new double[4][4];
	double m_[][] = new double[4][4];
	double mX[][] = new double[4][4];
	double mY[][] = new double[4][4];
	double mZ[][] = new double[4][4];
	double mP[][] = new double[4][4];
	
	double m_Z[][] = new double[4][4];
	double m_YZ[][] = new double[4][4];
	double m_XYZ[][] = new double[4][4];
	double m_PXYZ[][] = new double[4][4];
	double m_XYZInv[][] = new double[4][4];
	
	double xO;
	double yO;
	double zO;
	
	double a00, a01, a02, a03;  // coefficients of the transformation
	double a10, a11, a12, a13;
	double a20, a21, a22, a23;
	//double a30, a31, a32, a33;
	
	double ai00, ai01, ai02, ai03;  // coefficients of the inverse transformation
	double ai10, ai11, ai12, ai13;
	double ai20, ai21, ai22, ai23;
	//double ai30, ai31, ai32, ai33;
	
	double y, x, z; // volume coordinates
	int[] xyz;

	double X, Y, Z; // screen coordinates
	
	private double perspective = 0;
	private double maxDistance = 256;
	
	protected double getScale() {
		return scale;
	}

	protected double getZAspectRatio() {
		return zAspect;
	}

	protected int getZOrientation() {
		return zOrientation;
	}

	public void setPerspective(double perspective) {
		this.perspective = perspective;
//		System.out.println("Perspective: " + perspective);
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public double getPerspective() {
		return perspective;
	}

	public double getMaxDistance() {
		return maxDistance;
	}


	
}
