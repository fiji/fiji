package FlowJ;
import volume.*;


/*
	This class implements the real and imaginary parts of a complex deconvolution kernel.
	Based on Fleet and Jepson, 1990, Computation of Component Image Velocity from
	Local Phase Information, IJCV

	(c) 1999, Michael Abramoff
*/
public class Demodulation extends Kernel1D
{
	public Demodulation(float k0, boolean isReal)
	{
		  halfwidth = 2;             // on both sides!
		  k = new double[halfwidth*2+1];
		  if (isReal)
		  {
				k[0] = Math.cos(2*k0)/12;
				k[1] = -8*Math.cos(k0)/12;
				k[2] = 0;
				k[3] = 8*Math.cos(k0)/12;
				k[4] = -Math.cos(2*k0)/12;
		  }
		  else  // Imaginary kernel
		  {
				k[0] = Math.sin(2*k0)/12;
				k[1] = -8*Math.sin(k0)/12;
				k[2] = 0;
				k[3] = -8*Math.sin(k0)/12;
				k[4] = Math.sin(2*k0)/12;
		  }

	}
}
