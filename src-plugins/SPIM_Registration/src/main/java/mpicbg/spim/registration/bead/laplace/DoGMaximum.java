package mpicbg.spim.registration.bead.laplace;

import Jama.Matrix;

public class DoGMaximum
{
    public DoGMaximum(int x, int y, int z, int iteration)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.iteration = iteration;
    }

  	/**
  	 * Computes the distance between two DoG Maxima
  	 * @param target - Destination DoG Maxima
  	 * @return Distance to target or NaN if target null
  	 */
  	public float getDistanceTo(DoGMaximum target)
  	{
  		if (target == null)
  			return Float.NaN;
  		
  		float difference = 0;
  		
		difference += Math.pow(this.x - target.x,2); 
		difference += Math.pow(this.y - target.y,2);
		difference += Math.pow(this.z - target.z,2);
		difference += Math.pow(this.sigma - target.sigma,2);    		
  		
		return (float)Math.sqrt(difference);
  	}
  	

    public int x, y, z;
    public int iteration;
    //public double[][] hessianMatrix;
    public double[] hessianMatrix3x3;
    public double[] derivativeVector;
    public double[] eigenValues;     
    public double[][] eigenVectors; 

    public Matrix A = null, B = null, X = null;
    public double xd, yd, zd;

    public double EVratioA, EVratioB, EVratioC, minEVratio;

    public double laPlaceValue, quadrFuncValue, sumValue, sigma;

}
