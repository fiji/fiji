package process3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.FloatProcessor;
import ij.plugin.filter.PlugInFilter;

public class FFT_ implements PlugInFilter {
	
// 	private Data re;
// 	private Data im;

	private ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
System.out.println("input data");
		new StackConverter(image).convertToGray32();
		TestData data = new TestData(image);
// 		d.show();
System.out.println("output data");

		int w = image.getWidth(), h = image.getHeight();
		int d = image.getStackSize();


		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				data.setOffset(z * w * h + y * w);
				data.setIncrement(1);
				four1(data, w, false);
			}
			for(int x = 0; x < w; x++) {
				data.setOffset(z * w * h + x);
				data.setIncrement(w);
				four1(data, h, false);
			}
			IJ.showProgress(z, d-1);
		}
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				data.setOffset(y*w+x);
				data.setIncrement(w*h);
				four1(data, d, false);
			}
			IJ.showProgress(y, h-1);
		}
		data.show();

		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				data.setOffset(z * w * h + y * w);
				data.setIncrement(1);
				four1(data, w, true);
			}
			for(int x = 0; x < w; x++) {
				data.setOffset(z * w * h + x);
				data.setIncrement(w);
				four1(data, h, true);
			}
		}
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				data.setOffset(y*w+x);
				data.setIncrement(w*h);
				four1(data, d, true);
			}
		}
		data.show();
// System.out.println("reverse transformed");
// 		four1(d, ip.getWidth() * ip.getHeight(), true);
// 		d.show();
// 		new StackConverter(image).convertToGray32();
// 		image.setTitle("re");
// 		re = new FloatData(image);
// 
// 		int w = image.getWidth(), h = image.getHeight();
// 		int d = image.getStackSize();
// 		ImageStack stack = new ImageStack(w, h);
// 		for(int z = 0; z < d; z++) {
// 			stack.addSlice("", new FloatProcessor(w, h));
// 		}
// 		ImagePlus imImp = new ImagePlus("im", stack);
// 		imImp.setCalibration(image.getCalibration());
// 		im = new FloatData(imImp);
// 		// fourn(Data re,Data im, int[]nn, int ndim, boolean reverse)
// 		ComplexData cd = new ComplexData(re, im);
// 		fourn(cd, new int[] {w, h, d}, 3, false);
// 		fourn(cd, new int[] {w, h, d}, 3, true);
// 		re.show();
// 		im.show();
	}

// 	public void four2(Data d, int w, int h, boolean rvs) {
// 		for(int y = 0; y < h; y++) {
// 			four1(d, w, rvs);
// 		}
// 	}

// 	public interface Data {
// 		public float get(int i);
// 		public void set(int i, float v);
// 		public void swap(int i, int j);
// 		public void show();
// 	}
// 
// 	public class FloatData implements Data {
// 		private float[][] pixels;
// 		private ImagePlus imp;
// 		int w, h, d, wh;
// 		public FloatData(ImagePlus imp) {
// 			this.imp = imp;
// 			w = imp.getWidth();
// 			h = imp.getHeight();
// 			d = imp.getStackSize();
// 			wh = w*h;
// 			pixels = new float[d][];
// 			for(int z = 0; z < d; z++) {
// 				pixels[z] = (float[])imp.getStack()
// 							.getPixels(z+1);
// 			}
// 		}
// 
// 		/*
// 		 * remember: i ranges from 0..whd-1
// 		 */
// 		public float get(int i) {
// 			try {
// 				return pixels[i/wh][i%wh];
// 			} catch(RuntimeException e) {
// 				System.out.println("Caught exception");
// 				System.out.println("i = " + i);
// 				throw e;
// 			}
// 		}
// 
// 		/*
// 		 * remember: i ranges from 0..whd-1
// 		 */
// 		public void set(int i, float v) {
// 			pixels[i/wh][i%wh] = v;
// 		}
// 
// 		public void swap(int i, int j) {
// 			float tmp = pixels[i/wh][i%wh];
// 			pixels[i/wh][i%wh] = pixels[j/wh][j%wh];
// 			pixels[j/wh][j%wh] = tmp;
// 		}
// 
// 		public void show() {
// 			imp.show();
// 		}
// 	}
// 			
// 	public class ComplexData implements Data {
// 		private Data re;
// 		private Data im;
// 		public ComplexData(Data re, Data im) {
// 			this.re = re;
// 			this.im = im;
// 		}
// 
// 		/* 
// 		 * index 0: re(0), index1: im(0), ...
// 		 * note:
// 		 * indices range from 1..whd
// 		 */
// 		public float get(int i) {
// 			i--;
// 			return i % 2 == 0 ? re.get(i/2) : im.get(i/2);
// 		}
// 
// 		public void set(int i, float v) {
// 			i--;
// 			Data d = i % 2 == 0 ? re : im;
// 			d.set(i/2, v);
// 		}
// 
// 		public void swap(int i, int j) {
// 			float tmp = get(i);
// 			set(i, get(j));
// 			set(j, tmp);
// 		}
// 
// 		public void show() {
// 			re.show();
// 			im.show();
// 		}
// 	}

	public class TestData {
		private float[][] re;
		private float[][] im;
		private int w, h, d;
		private int wh;
		private int offset = 0;
		private int incr = 1;

		public TestData(ImagePlus imp) {
			w = imp.getWidth();
			h = imp.getHeight();
			d = imp.getStackSize();
			wh = w * h;
			re = new float[d][];
			im = new float[d][];
			for(int z = 0; z < d; z++) {
				re[z] = (float[])imp.getStack().getPixels(z+1);
				im[z] = new float[wh];
			}
		}


		public TestData(ImageProcessor ip) {
			w = ip.getWidth();
			h = ip.getHeight();
			d = 1;
			wh = w * h;
			re = new float[d][];
			im = new float[d][];
			re[0] = (float[])ip.getPixels();
			im[0] = new float[wh];
		}

		public void setIncrement(int i) {
			incr = i;
		}

		public void setOffset(int o) {
			offset = o;
		}

		/* 
		 * note:
		 * indices range from 1..whd
		 */
		public float getRe(int i) {
			int j = offset + (i-1) * incr;
			try {
				return re[j / wh][j % wh];
			} catch(RuntimeException e) {
				System.out.println("offset = " + offset);
				System.out.println("incr = " + incr);
				System.out.println("j / wh = " + j / wh);
				System.out.println("j % wh = " + j % wh);
				System.out.println(i);
				System.out.println(j);
				throw e;
			}
		}

		public float getIm(int i) {
			i = offset + (i-1) * incr;
			return im[i / wh][i % wh];
		}

		public void setRe(int i, float v) {
			i = offset + (i-1) * incr;
			re[i / wh][i % wh] = v;
		}

		public void setIm(int i, float v) {
			i = offset + (i-1) * incr;
			im[i / wh][i % wh] = v;
		}

		public void swap(int i, int j) {
			float tmp = getRe(i);
			setRe(i, getRe(j));
			setRe(j, tmp);
			tmp = getIm(i);
			setIm(i, getIm(j));
			setIm(j, tmp);
		}

		java.text.DecimalFormat df = new java.text.DecimalFormat(
			"###.##");

		public void show() {
			ImageStack rest = new ImageStack(w, h);
			ImageStack imst = new ImageStack(w, h);
			for(int z = 0; z < d; z++) {
				rest.addSlice("", 
					new FloatProcessor(w, h, re[z], null));
				imst.addSlice("", 
					new FloatProcessor(w, h, im[z], null));
			}
			new ImagePlus("re", rest).show();
			new ImagePlus("im", imst).show();
// 			for(int i = 0; i < re.length; i++) {
// 				System.out.print(df.format(re[i]) + "  ");
// 				System.out.print(df.format(im[i]) + "  ");
// 			}
// 			System.out.println();
		}
	}

	public void four1(TestData d, int nn, boolean reverse) {
		int isign = reverse ? -1 : 1;
		int j = 1;
		// bit reversal
		for(int i = 1; i < nn; i++) {
			if(j > i) {
				d.swap(j, i);
			}
			int m = nn >> 1;
			while(m >= 2 && j > m) {
				j -= m;
				m >>= 1;
			}
			j += m;
		}

 		int mmax = 2;
 		while(nn >= mmax) {
			int istep = mmax;
			double theta = isign * (2 * Math.PI / mmax);
			double wtemp = Math.sin(0.5*theta);
			double wpr = -2.0 * wtemp * wtemp;
			double wpi = Math.sin(theta);
			double wr = 1.0, wi = 0.0;
			for(int m = 1; m <= mmax>>1; m++) {
				for(int i = m; i <= nn; i += mmax) {
					j = i + (mmax >> 1);
					float tempr = (float)(wr * d.getRe(j) - 
							wi * d.getIm(j));
					float tempi = (float)(wr * d.getIm(j) +
							wi * d.getRe(j));
					d.setRe(j, d.getRe(i) - tempr);
					d.setIm(j, d.getIm(i) - tempi);
					d.setRe(i, d.getRe(i) + tempr);
					d.setIm(i, d.getIm(i) + tempi);
				}
				wtemp = wr;
				wr = wtemp * wpr - wi * wpi + wr;
				wi = wi * wpr + wtemp * wpi + wi;
			}
			mmax <<= 1;
		}
		
		// normalizing
		if(reverse) {
			for(int i = 1; i <= nn; i++) {
				d.setRe(i, d.getRe(i) / nn);
				d.setIm(i, d.getIm(i) / nn);
			}
		}
	}

	/**
	 * FFT in n dimensions, based on the implementation of 
	 * Numerical recipes in C, 1992.
	 * @param data data array
	 * @param nn length of each dimension
	 * @param ndim number of dimensions
	 * @param reverse flag indicating whether FFT or its inverse to be used
	 */
// 	public void fourn(Data d,int[]nn, int ndim, boolean reverse) {
// 		int isign = reverse ? -1 : 1;
// 		int ntot = 1;
// 		float tmp;
// 		for(int idim = 0; idim < ndim; idim++)
// 			ntot *= nn[idim];
// 		int nprev = 1;
// 		int ip1 = 0, ip2 = 0, ip3 = 0;
// 		for(int idim = ndim - 1; idim >= 0; idim--) {
// 			int n = nn[idim];
// 			int nrem = ntot / (n * nprev);
// 			ip1 = nprev << 1;
// 			ip2 = ip1 * n;
// 			ip3 = ip2 * nrem;
// 			int i2rev = 1;
// 			for(int i2 = 1; i2 <= ip2; i2+=ip1) {
// 				if(i2 < i2rev) {
// 					for(int i1=i2; i1<=i2+ip1-2; i1+=2) {
// 						for(int i3=i1; i3<=ip3; i3+=ip2){
// 							int i3rev = i2rev+i3-i2;
// 							d.swap(i3rev, i3);
// 							d.swap(i3rev+1, i3+1);
// 						}
// 					}
// 				}
// 				int ibit = ip2 >> 1;
// 				while(ibit >= ip1 && i2rev > ibit) {
// 					i2rev -= ibit;
// 					ibit >>= 1;
// 				}
// 				i2rev += ibit;
// 			}
// 			int ifp1 = ip1;
// 			while(ifp1 < ip2) {
// 				int ifp2 = ifp1 << 1;
// 				double theta = isign*2*Math.PI / (ifp2 / ip1);
// 				double wtemp = Math.sin(0.5 * theta);
// 				double wpr = -2.0 * wtemp * wtemp;
// 				double wpi = Math.sin(theta);
// 				double wr = 1.0, wi = 0.0;
// 				for(int i3 = 1; i3 <= ifp1; i3 += ip1) {
// 					for(int i1=i3; i1<=i3+ip1-2; i1+=2) {
// 						for(int i2=i1;i2<=ip3; i2+=ifp2){
// 
// int k1 = 0, k2 = 0; float tempr = 0, tempi = 0;
// try {
// 					k1 = i2;
// 					k2 = k1 + ifp1;
// 					tempr=(float)wr*d.get(k2)
// 							- (float)wi*d.get(k2+1);
// 					tempi=(float)wr*d.get(k2+1)
// 							+ (float)wi*d.get(k2);
// 					d.set(k2, d.get(k1)-tempr);
// 					d.set(k2+1, d.get(k1+1)-tempi);
// 					d.set(k1, d.get(k1)+tempr);
// 					d.set(k1+1, d.get(k1+1)+tempi);
// } catch(Exception e) {
// 	e.printStackTrace();
// 	System.out.println("k1 = " + k1);
// 	System.out.println("k2 = " + k2);
// 	System.out.println("tempr = " + tempr);
// 	System.out.println("tempi = " + tempi);
// 	return;
// }
// 
// 						}
// 					}
// 					wtemp = wr;
// 					wr = wr * wpr - wi * wpi + wr;
// 					wi = wi * wpr + wtemp * wpi + wi;
// 				}
// 				ifp1 = ifp2;
// 			}
// 			nprev *= n;
// 		}
// 	}
/*
	public void fourn(float[] data, int[] nn, int ndim, boolean reverse) {
		int isign = reverse ? -1 : 1;
		int ntot = 1;
		float tmp;
		for(int idim = 0; idim < ndim; idim++)
			ntot *= nn[idim];
		int nprev = 1;
		int ip1 = 0, ip2 = 0, ip3 = 0;
		for(int idim = ndim - 1; idim >= 0; idim--) {
			int n = nn[idim];
			int nrem = ntot / (n * nprev);
			ip1 = nprev << 1;
			ip2 = ip1 * n;
			ip3 = ip2 * nrem;
			int i2rev = 0;
			for(int i2 = 0; i2 < ip2; i2++) {
				if(i2 < i2rev) {
					for(int i1 = i2; i1 < i2+ip1-2; i1+=2) {
						for(int i3=i1; i3<ip3; i3+=ip2){
							int i3rev = i2rev+i3-i2;
							tmp = data[i3];
							data[i3] = data[i3rev];
							data[i3rev] = tmp;
							tmp = data[i3+1];
							data[i3+1]=data[i3rev+1];
							data[i3rev+1] = tmp;
						}
					}
				}
				int ibit = ip2 >> 1;
				while(ibit >= ip1 && i2rev >= ibit) {
					i2rev -= ibit;
					ibit >>= 1;
				}
				i2rev += ibit;
			}
			int ifp1 = ip1;
			while(ifp1 < ip2) {
				int ifp2 = ifp1 << 1;
				double theta = isign * Math.PI / (ifp2 / ip1);
				double wtemp = Math.sin(0.5 * theta);
				double wpr = -2.0 * wtemp * wtemp;
				double wpi = Math.sin(theta);
				double wr = 1.0, wi = 0.0;
				for(int i3 = 0; i3 < ifp1; i3 += ip1) {
					for(int i1=i3; i1<i3+ip1-2; i1 += 2) {
						for(int i2=i1;i2<ip3; i2+=ifp2){
							int k1 = i2;
							int k2 = k1 + ifp1;
							float tempr=(float) wr*data[k2]
								- (float) wi*data[k2+1];
							float tempi=(float)wr*data[k2+1]
								- (float) wi*data[k2];
							data[k2] = data[k1]-tempr;
							data[k2+1] = data[k1+1]-tempi;
							data[k1] += tempr;
							data[k1+1] += tempi;
						}
					}
					wtemp = wr;
					wr = wr * wpr - wi * wpi + wr;
					wi = wi * wpr + wtemp * wpi + wi;
				}
				ifp1 = ifp2;
			}
			nprev *= n;
		}
	}
*/
}
