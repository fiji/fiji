package levelsets.algorithm;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

/**
 * Implementation of the Levelset algorithm as originally implemented by Arne-Michael Toersel.
 * This class provides the getDeltaPhi function to the SparseFieldLevelSet:
 * deltaPhi = - DELTA_T * image_term * (advection * ADVECTION_FORCE + curvature * CURVATURE_EPSILON)
 * with:
 * image_term = 1 / (1 + ((gradients[x][y][z] + greyvalue_penalty) * 2))
 * advection, curvature from the base functions (upwind scheme and mean curvature)
 * ADVECTION_FORCE, CURVATURE_EPSILON are weights
 * 
 * This corresponds to the geometric curve evolution proposed by Malladi et.al. (IEEE Trans. on PAMI 16:158) 
 * and Eq. (15) in Caselles et.al. (International Journal of Computer Vision 22:61)
 * as referenced in the Geodesic Active Contour class of ITK. 
 * The greyvalue_penalty is a modification from Toersel. 
 */

public class ActiveContours extends LevelSetImplementation {

	protected double [][][] gradients = null;
		
	// Power of the advection force - expands contour along surface normals
	protected final double ADVECTION_FORCE;
	// Power of regulatory curvature term
	protected final double CURVATURE_EPSILON;
	// Grey value tolerance
	protected final double GREY_TOLERANCE;

	public ActiveContours(ImageContainer image, ImageProgressContainer img_progress, StateContainer init_state,
			double convergence, double advection, double curvature, double grey_tol) {
		super(image, img_progress, init_state, convergence);
		
		ADVECTION_FORCE = advection;
		CURVATURE_EPSILON = curvature;
		GREY_TOLERANCE = grey_tol;
	}

		
	@Override
	protected final void init() {
	    super.init();

	    gradients = this.img.calculateGradients();
	}


	@Override
	protected final void cleanup() {
		super.cleanup();
        this.gradients = null;
	}

	
	@Override
	protected void updateDeltaT() {
		DELTA_T = 1d/6d * 1d/(CURVATURE_EPSILON * ADVECTION_FORCE);
	}

	
	@Override
	protected final double getDeltaPhi(int x, int y, int z) {
        double image_term = getImageTerm(x, y, z);
        
        //         if (image_term < (CONVERGENCE_FACTOR / 2)) continue;
        
        double curvature = getCurvatureTerm(x, y, z);
        double advection = getAdvectionTerm(x, y, z);
        
        //image_term = 0;           else num_updated++;
        
        // calculate net change
        double delta_phi = - DELTA_T * image_term *
                (advection * ADVECTION_FORCE + curvature * CURVATURE_EPSILON);
        
        return delta_phi;
 	}

	
	private final double getImageTerm(int x, int y, int z)
	{
		int greyval = img.getPixel(x, y, z);
		int greyval_penalty = Math.abs(greyval - this.seed_greyvalue);
		if (greyval_penalty < GREY_TOLERANCE) greyval_penalty = 0;
		return (1 / (1 + ((gradients[x][y][z] + greyval_penalty) * 2)));
	}

	
	// upwind scheme
	protected final double getAdvectionTerm(int x, int y, int z)
	{
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

		double xBdiff = Math.max(cell_phi - xB, 0);
		double xFdiff = Math.min(xF - cell_phi, 0);
		double yBdiff = Math.max(cell_phi - yB, 0);
		double yFdiff = Math.min(yF - cell_phi, 0);
		double zBdiff = Math.max((cell_phi - zB) / zScale, 0);
		double zFdiff = Math.min((zF - cell_phi) / zScale, 0);

		return Math.sqrt(xBdiff * xBdiff + xFdiff * xFdiff +
				yBdiff * yBdiff + yFdiff * yFdiff +
				zBdiff * zBdiff + zFdiff * zFdiff);
	}

	// central differences
	protected final double getCurvatureTerm(int x, int y, int z)
	{
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

		return curvature * deltaPhi;
	}

}
