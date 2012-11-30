/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

public class Interpolation {

	float[][] pw = new float[256][4]; 
	float[][] sw = new float[256][4];
	private Control control; 

	public Interpolation(Control control) {
		this.control = control;
		initializeCubicPolynomialWeights();
		initializeCubicSplineWeights();
	}

//	float [] getCubicSplineWeights(float dx) {
//		float[] wx = new float[4];
//
//		float dx2 = dx*dx;
//		float dx_ = 1-dx;
//		float dx_2 = dx_*dx_;
//
//		// cubic spline
//		wx[0] = dx_2*dx_/6;
//		wx[1] = 2/3f - 0.5f*dx2*(2-dx);
//		wx[2] = 2/3f - 0.5f*dx_2*(1+dx);
//		wx[3] = dx2*dx/6;
//
//		return wx;
//	}


	void initializeCubicPolynomialWeights() {
		for (int i = 0; i < pw.length; i++) {
			float dx = i/256f;

			float dx2 = dx*dx;
			float dx3 = dx*dx2;

			pw[i][0] = (  -dx3 + 2*dx2 - dx)/2;
			pw[i][1] = ( 3*dx3 - 5*dx2 + 2 )/2;
			pw[i][2] = (-3*dx3 + 4*dx2 + dx)/2;
			pw[i][3] = (   dx3 -   dx2)/2;

			//			float a = -3f;
			//			pw[i][0] =  a*dx3 - 2*a*dx2 + a*dx;
			//			pw[i][1] = (a+2)*dx3 -(a+3)*dx2 + 1;
			//			pw[i][2] = -(a+2)*dx3 + (2*a+3)*dx2 - a*dx;
			//			pw[i][3] = -a*dx3 + a*dx2;
		}
	}

	void initializeCubicSplineWeights() {
		for (int i = 0; i < pw.length; i++) {
			float dx = i/256f;

			float dx2 = dx*dx;
			float dx_ = 1-dx;
			float dx_2 = dx_*dx_;

			sw[i][0] = dx_2*dx_/6;
			sw[i][1] = 2/3f - 0.5f*dx2*(2-dx);
			sw[i][2] = 2/3f - 0.5f*dx_2*(1+dx);
			sw[i][3] = dx2*dx/6;

			// equal to
			//			sw[i][0] = (  -dx3 + 3*dx2 -3*dx + 1)/6;
			//			sw[i][1] = ( 3*dx3 - 6*dx2 +4)/6;
			//			sw[i][2] = (-3*dx3 + 3*dx2 + 3*dx +1)/6;
			//			sw[i][3] = dx3/6;
		}
	}

	float [] getCubicPolynomialWeights(float dx) {
		float[] wx = new float[4];

		float dx2 = dx*dx;
		float dx3 = dx*dx2;

		wx[0] = (  -dx3 + 2*dx2 - dx)/2;
		wx[1] = ( 3*dx3 - 5*dx2 + 2 )/2;
		wx[2] = (-3*dx3 + 4*dx2 + dx)/2;
		wx[3] = (   dx3 -   dx2)/2;

		return wx;
	}

	int get(byte[][][] data3D, float z, float y, float x) {

		x += 0.5; 
		y += 0.5;
		z += 0.5;

		int z0 = (int)z;
		float dz = z - z0;
		int y0 = (int)y;
		float dy = y - y0;
		int x0 = (int)x;
		float dx = x - x0;			

		if (control.interpolationMode == Control.TRICUBIC_POLYNOMIAL) {

			float[] wx = pw[(int) (dx*256)]; // getCubicPolynomialWeights(dx);
			float[] wy = pw[(int) (dy*256)]; // getCubicPolynomialWeights(dy);
			float[] wz = pw[(int) (dz*256)]; // getCubicPolynomialWeights(dz);

			float vz = 0;
			for (int zi = 0; zi < 4; zi++) {
				byte[][] vDataZ = data3D[z0+zi];
				float vy = 0; 
				for (int yi = 0; yi < 4; yi++) {
					byte[] vDataZY = vDataZ[y0+yi];
					float vx = wx[0]*(0xFF & vDataZY[x0]) +
							wx[1]*(0xFF & vDataZY[x0+1]) +
							wx[2]*(0xFF & vDataZY[x0+2]) +
							wx[3]*(0xFF & vDataZY[x0+3]);

					vy += wy[yi]*vx;
				}
				vz += wz[zi]*vy;
			}
			return (int)Math.min(255, Math.max(0, vz));
		}
		else if (control.interpolationMode == Control.TRICUBIC_SPLINE) {
			float[] wx = sw[(int) (dx*256)]; // getCubicSplineWeights(dx);
			float[] wy = sw[(int) (dy*256)]; // getCubicSplineWeights(dy);
			float[] wz = sw[(int) (dz*256)]; // getCubicSplineWeights(dz);

			float vz = 0;
			for (int zi = 0; zi < 4; zi++) {
				float vy = 0;
				byte[][] vDataZ = data3D[z0+zi];
				for (int yi = 0; yi < 4; yi++) {
					byte[] vDataZY = vDataZ[y0+yi];
					float vx = wx[0]*(0xFF & vDataZY[x0]) +
							wx[1]*(0xFF & vDataZY[x0+1]) +
							wx[2]*(0xFF & vDataZY[x0+2]) +
							wx[3]*(0xFF & vDataZY[x0+3]);

					vy += wy[yi]*vx;
				}
				vz += wz[zi]*vy;
			}
			//return (int)Math.min(255, Math.max(0, vz));
			return (int)vz;
		}	
		else if (control.interpolationMode == Control.TRILINEAR) {
			x0++;
			y0++;
			z0++;
			int x1 = x0+1;
			int y1 = y0+1;
			float dx_ = 1-dx;
			byte[][] data3D_z0 = data3D[z0]; 
			byte[][] data3D_z1 = data3D[z0+1]; 

			float ab = (0xff & data3D_z0[y0][x0 ])*dx_ + dx*(0xff & data3D_z0[y0][x1]);
			float ef = (0xff & data3D_z1[y0][x0 ])*dx_ + dx*(0xff & data3D_z1[y0][x1]);
			float cd = (0xff & data3D_z0[y1][x0 ])*dx_ + dx*(0xff & data3D_z0[y1][x1]);
			float gh = (0xff & data3D_z1[y1][x0 ])*dx_ + dx*(0xff & data3D_z1[y1][x1]);

			float dy_ = 1-dy;
			ab = ab*dy_ + dy*cd;
			ef = ef*dy_ + dy*gh;

			return (int) (ab + dz*(ef-ab));
		}
		else {// NN
			x += 1.5; 
			y += 1.5;
			z += 1.5;
			return 0xff & data3D[(int)z][(int)y][(int)x];	
		}
	}
}
