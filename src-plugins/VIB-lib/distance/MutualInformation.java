/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package distance;

/*

  For dealing with images of depth greater than 8 bits, we need to bin
  the values so we don't have to maintain (for example with GRAY16
  images) a "joint" array with (2**16)**2 longs.
  

   0.3             0.4             0.5             0.6
 minimum                                         maximum
    |               |               |               |
     <--- bin 0 ---> <--- bin 1 ---> <--- bin 2 --->


  Just to remind me, the diagram from MacKay (Figure 8.1) showing the
  relationship between the entropies of X, Y and the mutual
  information is:

     ------------------------------------------------------
     |                  H(X,Y)                            |
     ------------------------------------------------------
     |        H(X)                       |
     ------------------------------------------------------
                              |           H(Y)            |
     ------------------------------------------------------
     |        H(X|Y)          |  I(X;Y)  |    H(Y|X)      |
     ------------------------------------------------------

*/

public class MutualInformation implements PixelPairs {
        private float minimum;
        private float maximum;
	private float width;
	long joint[];
	private int count;
        private int bins;
	
	// So that this is as efficient as possible, 

	public MutualInformation(float minimumValue, float maximumValue, int bins) {
		this.minimum = minimumValue;
		this.maximum = maximumValue;
		this.width = maximumValue - minimumValue;
		this.bins = bins;		
	}

	// The default constructor is for 8 bit images.
	public MutualInformation() {
		this(0,255,256);
	}

	public void reset() {
		joint = new long[bins * bins];
		count = 0;
	}

	public void add(float v1, float v2) {
		/*
		if( v1 < minimum )
			throw new RuntimeException("v1 less than minimum");
		if( v1 > maximum )
			throw new RuntimeException("v1 greater than maximum");
		if( v2 < minimum )
			throw new RuntimeException("v2 less than minimum");
		if( v2 > maximum )
			throw new RuntimeException("v2 greater than maximum");
		*/
		int i1 = (int)Math.floor((v1 - minimum) * bins / width);
		int i2 = (int)Math.floor((v2 - minimum) * bins / width);
		if( i1 >= bins )
			i1 = bins - 1;
		if( i2 >= bins )
			i2 = bins - 1;
		joint[i1 * bins + i2]++;
		count++;
	}

	public float getEntropy1() {
		return h1;
	}

	public float getEntropy2() {
		return h2;
	}

	public float getJointEntropy() {
		return h12;
	}

	float h1, h2, h12;

	public float mutualInformation() {
		
		float h12 = 0, h1 = 0, h2 = 0;
		for(int i = 0; i < bins; i++) {
			long p1 = 0, p2 = 0;
			for(int j = 0; j < bins; j++) {
				float p = joint[i * bins + j] / (float)count;
				if(p > 0)
					h12 -= p * (Math.log(p) / Math.log(2));
				p1 += joint[i * bins + j];
				p2 += joint[j * bins + i];
			}
			if(p1 > 0) {
				float p = p1 / (float)count;
				h1 -= p	* (Math.log(p) / Math.log(2));
			}
			if(p2 > 0) {
				float p = p2 / (float)count;
				h2 -= p * (Math.log(p) / Math.log(2));
			}
		}

		this.h1 = h1;
		this.h2 = h2;
		this.h12 = h12;

		return h1 + h2 - h12;
	}

	public float distance() {
		float result = -mutualInformation();
		return result;
	}

	public void printJointMatrix() {
		for( int i = 0; i < bins; ++i )
			for( int j = 0; j < bins; ++j )
				System.out.println("["+i+","+j+"]: "+joint[i*bins+j]);

	}
}
