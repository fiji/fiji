package jRenderer3D;

import java.util.ArrayList;

class PointsPlot {
	
	private int[] bufferPixels;
	private double[] zbufferPixels;
	private int bufferWidth;
	private int bufferHeight;
	private Transform tr;
	private ArrayList point3D = new ArrayList();

	
	private static final int OPAQUE = 0xFF000000;
	
	protected void addPoints3D(ArrayList points3D) {
		this.point3D = points3D;
	}
	
	protected void addPoint3D(Point3D point3D) {
		this.point3D.add(point3D);
	}
	
	protected void removeLastPoint() {
		int size = point3D.size();
		if (size > 0)
			this.point3D.remove(size-1);
	}
	
	protected void draw() {
		
		if (point3D != null){
			for (int i=point3D.size()-1; i>=0; i--){
				if (point3D.get(i)!= null && point3D.get(i) instanceof Point3D){
					Point3D pt = (Point3D)point3D.get(i);
					if (pt != null) {
						if (pt.drawMode == Point3D.DOT)
							pointsColoredDots(pt);
						else if (pt.drawMode == Point3D.CIRCLE)
							pointsColoredCircles(pt);
						else if (pt.drawMode == Point3D.SPHERE)
							pointsColoredSpheres(pt);
					}
				}
			}
		}
	}
	
	
///////////////////////////////////////////////////////////////////////////////////////////////
	
	private void pointsColoredDots(Point3D pt){
		
		tr.transform(pt);
					
		int x = (int) tr.X;
		int y = (int) tr.Y;
		
		if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
			int pos = y*bufferWidth + x;  
			int z = (int) tr.Z;
			
			if (z < zbufferPixels[pos]) {
				zbufferPixels[pos] = z;
				bufferPixels[pos] = pt.rgb; 
			}
		}
	}	
	
	private void pointsColoredCircles(Point3D pt){
		
		int rad = (int) pt.size; //(pt.size+1)/2;
		int rad2 = rad*rad+1;
		
		tr.transform(pt);
		
		int x = (int) tr.X;
		int y = (int) tr.Y;
		
		for (int y_ = -rad; y_ <= rad; y_++) {
			int Yy = y+y_;
			for (int x_ = -rad; x_ <= rad; x_++) {
				
				if (x_*x_+y_*y_ <= rad*rad) {
					int rxy = x_*x_ + y_*y_;
					
					int Xx = x+x_;
					
					if (Xx >= 0 && Yy >= 0 && Xx < bufferWidth && Yy < bufferHeight) { 
						int z = (int) tr.Z;
						int pos = Yy*bufferWidth + Xx;
						int z_ = z - fred_sqrt(rad2-rxy); // a little bit faster
						
						if (z_ < zbufferPixels[pos]) {
							zbufferPixels[pos] = z_;
							bufferPixels[pos] = pt.rgb;
//									int v = Math.min(255, Math.max(0, z +128));
//									bufferPixels[pos] = (0xFF << 24) | (v<<16) | (v << 8) | v;
							
						}
					}
				}
			}
		}
	}
	
	private void pointsColoredSpheres(Point3D pt){
		
		tr.transform(pt);
					
		int x = (int) tr.X;
		int y = (int) tr.Y;
		
		int rad = (int) pt.size;
		if (rad > 0) {
			int rad2 = rad*rad+1;
			
			int c = pt.rgb;
			int r = ((c >> 16)& 0xff);
			int g = ((c >> 8 )& 0xff);
			int b = ( c       & 0xff);
			
			double r2_ = 1./(rad2*rad2); 
			int r22 = rad2*rad2;
			
			for (int y_ = -rad; y_ <= rad; y_++) {
				int Yy = y+y_;
				for (int x_ = -rad; x_ <= rad; x_++) {
					int rxy = x_*x_ + y_*y_;
					
					if (rxy < rad2) {
						
						int Xx = x+x_;
						
						if (Xx >= 0 && Yy >= 0 && Xx < bufferWidth && Yy < bufferHeight) { 
							int z = (int) tr.Z;
							int pos = Yy*bufferWidth + Xx;
							//int z_ = (int) (z - Math.sqrt(rad2-rxy));
							int z_ = z - fred_sqrt(rad2-rxy); // a little bit faster
							
							if (z_ < zbufferPixels[pos]) {
								zbufferPixels[pos] = z_;
								
								bufferPixels[pos] = pt.rgb; 
								double a = (r22-rxy*rxy) * r2_;
								int r_ = (int) (a*r);
								int g_ = (int) (a*g);
								int b_ = (int) (a*b);
								
								bufferPixels[pos] = OPAQUE | (r_ <<16) | (g_ << 8) | b_;
							}
						}
					}
				}
			}
		}
	}

	/*
	// Integer Square Root function
	// Contributors include Arne Steinarson for the basic approximation idea, 
	// Dann Corbit and Mathew Hendry for the first cut at the algorithm, 
	// Lawrence Kirby for the rearrangement, improvments and range optimization
	// and Paul Hsieh for the round-then-adjust idea.
	*/
	
	static final int sqq_table[] = {
	       0,  16,  22,  27,  32,  35,  39,  42,  45,  48,  50,  53,  55,  57,
	      59,  61,  64,  65,  67,  69,  71,  73,  75,  76,  78,  80,  81,  83,
	      84,  86,  87,  89,  90,  91,  93,  94,  96,  97,  98,  99, 101, 102,
	     103, 104, 106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118,
	     119, 120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132,
	     133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 144, 145,
	     146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 155, 155, 156, 157,
	     158, 159, 160, 160, 161, 162, 163, 163, 164, 165, 166, 167, 167, 168,
	     169, 170, 170, 171, 172, 173, 173, 174, 175, 176, 176, 177, 178, 178,
	     179, 180, 181, 181, 182, 183, 183, 184, 185, 185, 186, 187, 187, 188,
	     189, 189, 190, 191, 192, 192, 193, 193, 194, 195, 195, 196, 197, 197,
	     198, 199, 199, 200, 201, 201, 202, 203, 203, 204, 204, 205, 206, 206,
	     207, 208, 208, 209, 209, 210, 211, 211, 212, 212, 213, 214, 214, 215,
	     215, 216, 217, 217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223,
	     224, 224, 225, 225, 226, 226, 227, 227, 228, 229, 229, 230, 230, 231,
	     231, 232, 232, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238,
	     239, 240, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246,
	     246, 247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253,
	     253, 254, 254, 255
	    };
	
	static int fred_sqrt(int x) {
		int xn;

	    if (x >= 0x10000)
	        if (x >= 0x1000000)
	            if (x >= 0x10000000)
	                if (x >= 0x40000000) {
	                    if (x >= 65535*65535)
	                        return 65535;
	                    xn = sqq_table[ (x>>24)] << 8;
	                } else
	                    xn = sqq_table[ (x>>22)] << 7;
	            else
	                if (x >= 0x4000000)
	                    xn = sqq_table[ (x>>20)] << 6;
	                else
	                    xn = sqq_table[ (x>>18)] << 5;
	        else {
	            if (x >= 0x100000)
	                if (x >= 0x400000)
	                    xn = sqq_table[ (x>>16)] << 4;
	                else
	                    xn = sqq_table[ (x>>14)] << 3;
	            else
	                if (x >= 0x40000)
	                    xn = sqq_table[ (x>>12)] << 2;
	                else
	                    xn = sqq_table[ (x>>10)] << 1;

	            xn = (xn + 1 + x / xn) / 2;
//	            if (xn * xn > x) /* Correct rounding if necessary */
//	     	       xn--;

	     	    return  xn;
	          
	        }
	    else
	        if (x >= 0x100) {
	            if (x >= 0x1000)
	                if (x >= 0x4000)
	                    xn = (sqq_table[ (x>>8)] >> 0) + 1;
	                else
	                    xn = (sqq_table[ (x>>6)] >> 1) + 1;
	            else
	                if (x >= 0x400)
	                    xn = (sqq_table[ (x>>4)] >> 2) + 1;
	                else
	                    xn = (sqq_table[ (x>>2)] >> 3) + 1;
	
//	            if (xn * xn > x) /* Correct rounding if necessary */
//	     	       xn--;
	     	    return  xn;
	     	    	    
	        } else
	            return sqq_table[ x] >> 4;

	/* Run two iterations of the standard convergence formula */

	    xn = (xn + 1 + x / xn) / 2;
	    xn = (xn + 1 + x / xn) / 2;
	
//	    if (xn * xn > x) /* Correct rounding if necessary */
//	       xn--;

	    return  xn;
	}


	

	protected void setBuffers(int[] bufferPixels, double[] zbufferPixels, int bufferWidth, int bufferHeight) {
		this.bufferPixels =  bufferPixels;
		this.zbufferPixels = zbufferPixels;
		this.bufferWidth = bufferWidth;
		this.bufferHeight = bufferHeight;
		
	}


	protected void setTransform(Transform transform) {
		this.tr = transform;	
	}

	public int getSize() {
		// TODO Auto-generated method stub
		return point3D.size();
	}




	
}
