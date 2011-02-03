package imagescience.segment;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Thresholds images. */
public class Thresholder {
	
	/** Default constructor. */
	public Thresholder() { }
	
	/** Applies hard thresholding to an image.
		
		@param image the input image to be thresholded. The image is overwritten with the thresholding results.
		
		@param threshold the threshold value. Elements with a value larger than or equal to {@code threshold} are set to {@code 255}, and elements with a value less than {@code threshold} are set to {@code 0}.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void hard(final Image image, final double threshold) {
		
		messenger.log(ImageScience.prelude()+"Thresholder");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		messenger.log("Hard thresholding "+image.type()+" at value "+threshold);
		final Dimensions dims = image.dimensions();
		messenger.status("Applying hard thresholding...");
		final double[] array = new double[dims.x];
		final Coordinates coords = new Coordinates();
		progressor.steps(dims.c*dims.t*dims.z);
		progressor.start();
		image.axes(Axes.X);
		
		// Threshold:
		for (coords.c=0; coords.c<dims.c; ++coords.c) {
			for (coords.t=0; coords.t<dims.t; ++coords.t) {
				for (coords.z=0; coords.z<dims.z; ++coords.z) {
					for (coords.y=0; coords.y<dims.y; ++coords.y) {
						image.get(coords,array);
						for (int x=0; x<dims.x; ++x)
							if (array[x] >= threshold) array[x] = ABOVE;
							else array[x] = BELOW;
						image.set(coords,array);
					}
					progressor.step();
				}
			}
		}
		
		// Finish up:
		image.name(image.name() + " hard thresholded");
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	/** Applies hysteresis thresholding to an image.
		
		@param image the input image to be thresholded. The image is overwritten with the thresholding results.
		
		@param low {@code high} - the lower and higher threshold values that define the hysteresis. Elements with a value larger than or equal to {@code high} are set to {@code 255}, elements with a value less than {@code low} are set to {@code 0}, and elements with a value larger than or equal to the {@code low} are set to {@code 255} only if they are connected to elements with a value larger than or equal to {@code high} through elements all with a value larger than or equal to {@code low}. If the size of the image in the z-dimension equals {@code 1}, this method uses 8-connectivity in x-y space, otherwise it uses 26-connectivity in x-y-z space. The algorithm is applied to every x-y(-z) subimage in a 5D image.
		
		@exception NullPointerException if {@code image} image is {@code null}.
	*/
	public void hysteresis(final Image image, final double low, final double high) {
		
		messenger.log(ImageScience.prelude()+"Thresholder");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		messenger.log("Hysteresis thresholding "+image.type()+" in range ["+low+","+high+"]");
		final Dimensions dims = image.dimensions();
		messenger.status("Applying hysteresis thresholding...");
		final double[] array = new double[dims.x];
		final double[] marray = new double[dims.x+2];
		final Coordinates coords = new Coordinates();
		final Coordinates mcoords = new Coordinates();
		final Coordinates cand = new Coordinates();
		final Candidates cands = new Candidates();
		image.axes(Axes.X);
		
		progressor.steps(3*dims.c*dims.t*dims.z*dims.y);
		progressor.start();
		
		// Classify and expand:
		if (dims.z == 1) { // 2D case
			
			// Create map with border:
			final Dimensions mdims = new Dimensions(dims.x+2,dims.y+2,dims.z,dims.t,dims.c);
			final ByteImage map = new ByteImage(mdims); map.axes(Axes.X);
			for (coords.c=mcoords.c=0; coords.c<dims.c; ++coords.c, ++mcoords.c) {
				for (coords.t=mcoords.t=0; coords.t<dims.t; ++coords.t, ++mcoords.t) {
					for (coords.y=0, mcoords.y=1; coords.y<dims.y; ++coords.y, ++mcoords.y) {
						coords.x=0; image.get(coords,array);
						for (mcoords.x=1; coords.x<dims.x; ++coords.x, ++mcoords.x)
							if (array[coords.x] >= high) marray[mcoords.x] = ON;
							else if (array[coords.x] >= low) marray[mcoords.x] = CAND;
							else marray[mcoords.x] = OFF;
						mcoords.x=0; map.set(mcoords,marray);
						progressor.step();
					}
				}
			}
			
			// Expand ON pixels:
			for (mcoords.c=0; mcoords.c<dims.c; ++mcoords.c) {
				for (mcoords.t=0; mcoords.t<dims.t; ++mcoords.t) {
					for (mcoords.y=1; mcoords.y<=dims.y; ++mcoords.y) {
						mcoords.x=0; map.get(mcoords,marray);
						for (mcoords.x=1; mcoords.x<=dims.x; ++mcoords.x) {
							if (marray[mcoords.x] == ON) {
								cands.push(mcoords);
								// Search for candidates iteratively in 8-connected neighborhoods:
								while (cands.size > 0) {
									cands.pop(cand); map.set(cand,ON);
									++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
								}
							}
						}
						progressor.step();
					}
				}
			}
			
			// Copy ON pixels:
			for (coords.c=mcoords.c=0; coords.c<dims.c; ++coords.c, ++mcoords.c) {
				for (coords.t=mcoords.t=0; coords.t<dims.t; ++coords.t, ++mcoords.t) {
					for (coords.y=0, mcoords.y=1; coords.y<dims.y; ++coords.y, ++mcoords.y) {
						mcoords.x=0; map.get(mcoords,marray);
						for (coords.x=0, mcoords.x=1; coords.x<dims.x; ++coords.x, ++mcoords.x)
							if (marray[mcoords.x] == ON) array[coords.x] = ABOVE;
							else array[coords.x] = BELOW;
						coords.x=0; image.set(coords,array);
						progressor.step();
					}
				}
			}
			
		} else { // 3D case
			
			// Create map with border:
			final Dimensions mdims = new Dimensions(dims.x+2,dims.y+2,dims.z+2,dims.t,dims.c);
			final ByteImage map = new ByteImage(mdims); map.axes(Axes.X);
			for (coords.c=mcoords.c=0; coords.c<dims.c; ++coords.c, ++mcoords.c) {
				for (coords.t=mcoords.t=0; coords.t<dims.t; ++coords.t, ++mcoords.t) {
					for (coords.z=0, mcoords.z=1; coords.z<dims.z; ++coords.z, ++mcoords.z) {
						for (coords.y=0, mcoords.y=1; coords.y<dims.y; ++coords.y, ++mcoords.y) {
							coords.x=0; image.get(coords,array);
							for (mcoords.x=1; coords.x<dims.x; ++coords.x, ++mcoords.x)
								if (array[coords.x] >= high) marray[mcoords.x] = ON;
								else if (array[coords.x] >= low) marray[mcoords.x] = CAND;
								else marray[mcoords.x] = OFF;
							mcoords.x=0; map.set(mcoords,marray);
							progressor.step();
						}
					}
				}
			}
			
			// Expand ON voxels:
			for (mcoords.c=0; mcoords.c<dims.c; ++mcoords.c) {
				for (mcoords.t=0; mcoords.t<dims.t; ++mcoords.t) {
					for (mcoords.z=1; mcoords.z<=dims.z; ++mcoords.z) {
						for (mcoords.y=1; mcoords.y<=dims.y; ++mcoords.y) {
							mcoords.x=0; map.get(mcoords,marray);
							for (mcoords.x=1; mcoords.x<=dims.x; ++mcoords.x)
								if (marray[mcoords.x] == ON) {
									cands.push(mcoords);
									// Search for candidates iteratively in 26-connected neighborhoods:
									while (cands.size > 0) {
										cands.pop(cand); map.set(cand,ON);
										// Back 9 voxels:
										++cand.z; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										// Middle 8 voxels:
										--cand.z; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										// Front 9 voxels:
										--cand.z; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.x; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										--cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
										++cand.x; ++cand.y; if (map.get(cand)==CAND) { cands.push(cand); map.set(cand,QUEUED); }
									}
								}
							progressor.step();
						}
					}
				}
			}
			
			// Copy ON voxels:
			for (coords.c=mcoords.c=0; coords.c<dims.c; ++coords.c, ++mcoords.c) {
				for (coords.t=mcoords.t=0; coords.t<dims.t; ++coords.t, ++mcoords.t) {
					for (coords.z=0, mcoords.z=1; coords.z<dims.z; ++coords.z, ++mcoords.z) {
						for (coords.y=0, mcoords.y=1; coords.y<dims.y; ++coords.y, ++mcoords.y) {
							mcoords.x=0; map.get(mcoords,marray);
							for (coords.x=0, mcoords.x=1; coords.x<dims.x; ++coords.x, ++mcoords.x)
								if (marray[mcoords.x] == ON) array[coords.x] = ABOVE;
								else array[coords.x] = BELOW;
							coords.x=0; image.set(coords,array);
							progressor.step();
						}
					}
				}
			}
		}
		
		// Finish up:
		image.name(image.name() + " hysteresis thresholded");
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	private final static double ON = 10;
	private final static double OFF = 0;
	private final static double CAND = 2;
	private final static double QUEUED = 9;
	private final static double ABOVE = 255;
	private final static double BELOW = 0;
}

final class Candidates {
	
	private int iCap = 10000;
	private final static int CAPINC = 10000;
	int size = 0;
	
	private int[] cx = new int[iCap];
	private int[] cy = new int[iCap];
	private int[] cz = new int[iCap];
	private int[] ct = new int[iCap];
	private int[] cc = new int[iCap];
	
	void push(final Coordinates coords) {
		
		if (size == iCap) inccap();
		cx[size] = coords.x;
		cy[size] = coords.y;
		cz[size] = coords.z;
		ct[size] = coords.t;
		cc[size] = coords.c;
		++size;
	}
	
	void pop(final Coordinates coords) {
		
		--size;
		coords.x = cx[size];
		coords.y = cy[size];
		coords.z = cz[size];
		coords.t = ct[size];
		coords.c = cc[size];
	}
	
	private void inccap() {
		
		final int iNewCap = iCap + CAPINC;
		final int[] newcx = new int[iNewCap];
		final int[] newcy = new int[iNewCap];
		final int[] newcz = new int[iNewCap];
		final int[] newct = new int[iNewCap];
		final int[] newcc = new int[iNewCap];
		for (int i=0; i<iCap; ++i) {
			newcx[i] = cx[i];
			newcy[i] = cy[i];
			newcz[i] = cz[i];
			newct[i] = ct[i];
			newcc[i] = cc[i];
		}
		cx = newcx;
		cy = newcy;
		cz = newcz;
		ct = newct;
		cc = newcc;
		iCap = iNewCap;
	}
	
}
