package levelsets.algorithm;

import ij.IJ;
import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;


/**
 * Implementation of the Geodesic active contour algorithm as described by 
 * Caselles et.al. (International Journal of Computer Vision 22:61)
 * and implemented in the in the Geodesic Active Contour class of ITK.
 * This class provides the getDeltaPhi function to the SparseFieldLevelSet:
 * deltaPhi = - DELTA_T * (advection * -gradient(g) * ADVECTION_FORCE + 
 * 						propagation * g * PROPAGATION_WEIGHT + 
 * 						curvature * g * CURVATURE_WEIGHT)
 * with:
 * g = 1 / (1 + (gradients[x][y][z]) * 1)
 * advection, curvature from the base functions (upwind scheme and mean curvature)
 * ADVECTION_FORCE, CURVATURE_EPSILON are weights.
 * 
 * Geodesic active contours can find Canny-type edges in the region of interest,
 * while also introducing a smoothing term
 * . 
 * This is a first effort that is not necessarily feature complete or the entirely correct. 
 * Use with caution. 
*/


public class GeodesicActiveContour extends LevelSetImplementation {

	// Image gradients
	protected double [][][] gradients = null;
	// Image gradients 2nd differentation
	protected double [][][] grad_gradients = null;
	
	// Power of the advection force - expands contour along surface normals
	protected final double ALPHA_ADVECTION;
	// Power of the advection force - expands contour along surface normals
	protected final double BETA_PROPAGATION;
	// Power of regulatory curvature term
	protected final double GAMMA_CURVATURE;
	// Greyscale values
	protected final double GAMMA_GREYSCALE;

	// Greyscale intensity range (from GREY_T - GREY_EPSILON to GREY_T + GREY_EPSILON)
	protected final double GREY_EPSILON = 1;
	protected final double GREY_T = 1;
	
	
	public GeodesicActiveContour(ImageContainer image, ImageProgressContainer img_progress, StateContainer init_state,
			double convergence, double advection, double prop, double curve, double grey ) {
		super(image, img_progress, init_state, convergence);
		
		ALPHA_ADVECTION = advection;
		BETA_PROPAGATION = prop;
		GAMMA_CURVATURE = curve;
		GAMMA_GREYSCALE = grey;		
	}

	
	@Override
	protected final void init() {
	    super.init();

	    // pre-calculate the gradients and the gradient of the gradient in init()
	    gradients = this.img.calculateGradients();
		for (int z = 0; z < gradients[0][0].length; z++) {
			for (int x = 1; x < gradients.length - 1; x++) {
				for (int y = 1; y < gradients[0].length - 1; y++) {
					gradients[x][y][z] = (1 / (1 + ((gradients[x][y][z]) * 1)));
				}
			}
		}
		grad_gradients = this.calculateGradients(gradients);
	}

	
	@Override
	protected final double getDeltaPhi(int x, int y, int z) {
        
        double curvature = getCurvatureTerm(x, y, z);
        double advection = getAdvectionTerm(x, y, z);
        double propagation = getPropagationTerm(x, y, z);
        
        // calculate net change
        double delta_phi = - DELTA_T *
                		(
                		advection * ALPHA_ADVECTION
                		+ propagation * BETA_PROPAGATION
                   		+ curvature * GAMMA_CURVATURE 
//                  	+ advection * GAMMA_GREYSCALE * (GREY_EPSILON - Math.abs(img.getPixel(x, y, z) - GREY_T))
                		);
        
        return delta_phi;
	}

	@Override
	protected final void updateDeltaT() {
		DELTA_T = 1d/6d * 1d/(GAMMA_CURVATURE * BETA_PROPAGATION * ALPHA_ADVECTION);
	}

	// upwind scheme
	protected final double getAdvectionTerm(int x, int y, int z) {
		double result = 0d;
		
		
		double xB = (x > 0) ?
				phi.get(x - 1, y, z) : Double.MAX_VALUE;
		double xF = (x + 1 < phi.getXLength()) ?
				phi.get(x + 1, y, z) : Double.MAX_VALUE;
		double yB = (y > 0) ?
				phi.get(x, y - 1, z) : Double.MAX_VALUE;
		double yF = (y + 1 < phi.getYLength()) ?
				phi.get(x, y + 1, z) : Double.MAX_VALUE;
		double zB = (z > 0) ?
				phi.get(x, y, z - 1) : Double.MAX_VALUE;
		double zF = (z + 1 < phi.getZLength()) ?
				phi.get(x, y, z + 1) : Double.MAX_VALUE;

		double cell_phi = phi.get(x, y, z);

		double xBdiff = cell_phi - xB;
		double xFdiff = xF - cell_phi;
		double yBdiff = cell_phi - yB;
		double yFdiff = yF - cell_phi;
//		double zBdiff = (cell_phi - zB) / zScale;
//		double zFdiff = (zF - cell_phi) / zScale;
		double zBdiff = 0;
		double zFdiff = 0;
		
		if ( grad_gradients[x][y][z] > 0 ) {
//			result = xBdiff * grad_gradients[x][y][z] + yBdiff * grad_gradients[x][y][z] + zBdiff * grad_gradients[x][y][z];
			// should be faster
			result = (xBdiff  + yBdiff  + zBdiff) * grad_gradients[x][y][z];
		} else {
//			result = xFdiff * grad_gradients[x][y][z] + yFdiff * grad_gradients[x][y][z] + zFdiff * grad_gradients[x][y][z];			
			result = (xFdiff + yFdiff + zFdiff) * grad_gradients[x][y][z];			
		}	
		
		return result;
	}
	
	// Identical to GeometricCurveEvolution
	protected final double getPropagationTerm(int x, int y, int z) {
		double xB = (x > 0) ?
				phi.get(x - 1, y, z) : Double.MAX_VALUE;
		double xF = (x + 1 < phi.getXLength()) ?
				phi.get(x + 1, y, z) : Double.MAX_VALUE;
		double yB = (y > 0) ?
				phi.get(x, y - 1, z) : Double.MAX_VALUE;
		double yF = (y + 1 < phi.getYLength()) ?
				phi.get(x, y + 1, z) : Double.MAX_VALUE;
		double zB = (z > 0) ?
				phi.get(x, y, z - 1) : Double.MAX_VALUE;
		double zF = (z + 1 < phi.getZLength()) ?
				phi.get(x, y, z + 1) : Double.MAX_VALUE;

		double cell_phi = phi.get(x, y, z);

		double xBdiff, xFdiff, yBdiff, yFdiff, zBdiff = 0, zFdiff = 0;
		
		if ( gradients[x][y][z] > 0 ) {
			xBdiff = Math.max(cell_phi - xB, 0);
			xFdiff = Math.min(xF - cell_phi, 0);
			yBdiff = Math.max(cell_phi - yB, 0);
			yFdiff = Math.min(yF - cell_phi, 0);
//			zBdiff = Math.max((cell_phi - zB) / zScale, 0);
//			zFdiff = Math.min((zF - cell_phi) / zScale, 0);
		} else {
			xBdiff = Math.min(cell_phi - xB, 0);
			xFdiff = Math.max(xF - cell_phi, 0);
			yBdiff = Math.min(cell_phi - yB, 0);
			yFdiff = Math.max(yF - cell_phi, 0);
//			zBdiff = Math.min((cell_phi - zB) / zScale, 0);
//			zFdiff = Math.max((zF - cell_phi) / zScale, 0);			
		}

		return Math.sqrt(xBdiff * xBdiff + xFdiff * xFdiff +
				yBdiff * yBdiff + yFdiff * yFdiff +
				zBdiff * zBdiff + zFdiff * zFdiff) * gradients[x][y][z];
	}

	
	// Identical to GeometricCurveEvolution
	protected final double getCurvatureTerm(int x, int y, int z) {
		if (x == 0 || x >= (phi.getXLength() - 1)) return 0;
		if (y == 0 || y >= (phi.getYLength() - 1)) return 0;
		boolean curvature_3d = false; //((z > 0) && (z < phi.getZLength() - 1));

		/* access to the deferred array is costly, so avoid multiple queries
	         for the same value and pre assign here
		 */
		double cell_phi = phi.get(x, y, z);
		double phiXB = phi.get(x - 1, y, z);
		double phiXF = phi.get(x + 1, y, z);
		double phiYB = phi.get(x, y - 1, z);
		double phiYF = phi.get(x, y + 1, z);

		double phiX = (phiXF - phiXB) / 2;
		double phiY = (phiYF - phiYB) / 2;
		double phiXX = (phiXF + phiXB - (2 * cell_phi));
		double phiYY = (phiYF + phiYB - (2 * cell_phi));
		double phiXY = (phi.get(x + 1, y + 1, z) - phi.get(x + 1, y - 1, z) -
				phi.get(x - 1, y + 1, z) + phi.get(x - 1, y - 1, z)) / 4;

		double phiZ = 0, phiZZ = 0, phiXZ = 0, phiYZ = 0;
		if (curvature_3d)
		{
			double phiZB = phi.get(x, y, z - 1);
			double phiZF = phi.get(x, y, z + 1);
			phiZ = (phiZF - phiZB) / 2;
			phiZZ = (phiZF + phiZB - (2 * cell_phi));
			phiXZ = (phi.get(x + 1, y, z + 1) - phi.get(x + 1, y, z - 1) - phi.get(x - 1, y, z + 1) + phi.get(x - 1, y, z - 1)) / 4;
			phiYZ = (phi.get(x, y + 1, z + 1) - phi.get(x, y + 1, z - 1) - phi.get(x, y - 1, z + 1) + phi.get(x, y - 1, z - 1)) / 4;
		}

		if (phiX == 0 || phiY == 0) return 0;
		if (curvature_3d && phiZ == 0) return 0;

		double curvature = 0, deltaPhi = 0;
		if (curvature_3d)
		{
			deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY + phiZ * phiZ);
			curvature = -1 * ((phiXX * (phiY * phiY + phiZ * phiZ)) +
					(phiYY * (phiX * phiX + phiZ * phiZ)) +
					(phiZZ * (phiX * phiX + phiY * phiY)) -
					(2 * phiX * phiY * phiXY) -
					(2 * phiX * phiZ * phiXZ) -
					(2 * phiY * phiZ * phiYZ)) /
					Math.pow(phiX * phiX + phiY * phiY + phiZ * phiZ, 3/2);
		}
		else
		{
			deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY);
			curvature = -1 * ((phiXX * phiY * phiY) + (phiYY * phiX * phiX)
					- (2 * phiX * phiY * phiXY)) /
					Math.pow(phiX * phiX + phiY * phiY, 3/2);
		}

		return curvature * deltaPhi * gradients[x][y][z];
	}
	

	
	   /**
	    * Calculates grey value gradients of an input double array
	    * Required for the grad_gradient calculation, taken from ImageContainer
	    * @return The result array
	    */
	protected final double[][][] calculateGradients(double [][][]grad_src)
	{
		IJ.log("Calculating gradients");
		double zScale = img.getzScale();
		double[][][] gradients = new double[img.getWidth()][img.getHeight()][img.getImageCount()];

		for (int z = 0; z < gradients[0][0].length; z++)
		{
			for (int x = 1; x < gradients.length - 1; x++)
			{
				for (int y = 1; y < gradients[0].length - 1; y++)
				{

					double xGradient =
						(grad_src[x + 1][y][z] - grad_src[x - 1][y][z]) / 2;
					double yGradient =
						(grad_src[x][y + 1][z] - grad_src[x][y - 1][z]) / 2;
					double zGradient = 0;
					if ((z > 0) && (z < gradients[0][0].length - 1))
					{
						zGradient =
							(grad_src[x][y][z + 1] - grad_src[x][y][z - 1]) / (2 * zScale);
					}
					gradients[x][y][z] = -Math.sqrt(xGradient * xGradient + yGradient * yGradient + zGradient * zGradient);
				}
			}
		}

		return gradients;
	}
	
}
