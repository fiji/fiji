package levelsets.algorithm;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;




/**
 * @author erwin
 * Stub class for all Level set  variants. SparseFieldLevelSet is an implementation of the sparse field method.
 * If there is some more efficient method in future, just replace the superclass. 
 * The current superclass requires two functions, the updateDeltaT and the getDeltaPhi methods which differ in
 * the sub-classed implementations. 
 * Before iterations, init() is called, after run cleanup() is called - implementations can add their own stuff there.
 */
public abstract class LevelSetImplementation extends SparseFieldLevelSet {
	
	public LevelSetImplementation(ImageContainer image,
			ImageProgressContainer img_progress, StateContainer init_state, double convergence) {
		super(image, img_progress, init_state, convergence);
		// TODO Auto-generated constructor stub
	}
	

	/* 
	 * Any initializations go here - make sure to call the super-class
	 * @see levelsets.algorithm.SparseFieldLevelSet#init()
	 */
	@Override
	protected void init() {
	    super.init();
	}

	/* 
	 * Any cleanup after the function is finished goes here.
	 * @see levelsets.algorithm.SparseFieldLevelSet#cleanup()
	 */
	@Override
	protected void cleanup() {
		super.cleanup();
 	}


	/**
	 * Updates the time step for numerical solution.
	 * Abstract class forces the derived class to implement it.
	 * If default settings are fine, just don't do anything
	 */
	protected abstract void updateDeltaT();


	/* Calculates delta Phi for voxel at x/y/z
	 * Abstract base class, overridden by the implementation
	 */
	protected abstract double getDeltaPhi(int x, int y, int z);

}
