package mpicbg.imglib.algorithm.findmax;

import mpicbg.imglib.Factory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Provides a way for the user to automatically create the appropriate RegionalMaximaFinder class
 * without having to deal with the dimensionality.
 * 
 * @author Nick Perry
 *
 * @param <T>
 */
public class RegionalMaximaFactory<T extends RealType<T>> implements Factory {
	
	/*
	 * CONSTRUCTOR
	 */
	
	private Image<T> image;
	private boolean overTime;
	private String errorMessage;

	/**
	 * Instantiate the factory with a source image and a over-time flag.
	 * If the later is set to true, then the last dimension is supposed to be time, and 
	 * an adequate {@link LocalMaximaFinder} will be returned, that takes into account
	 * time.  
	 * @param img
	 * @param overTime
	 */
	public RegionalMaximaFactory(Image<T> img, boolean overTime) {
		this.image = img;
		this.overTime = overTime;
	}
	
	/**
	 * Return the adequate {@link LocalMaximaFinder}, given the dimensionality of the source image.
	 * Special cases of image over time will be addressed using the flag given in the constructor.  
	 * @return  a new {@link LocalMaximaFinder} 
	 */
    public RegionalMaximaFinder<T> createRegionalMaximaFinder() {
    	
    	switch ( image.getNumDimensions() ) {
    	
    	case 1:
    		errorMessage = "1D is not implemented yet.";
        	throw new IllegalArgumentException(errorMessage);
    		
    	case 2:
    			
    		if (overTime) {
    			errorMessage = "1D over time is not implemented yet."; 
            	throw new IllegalArgumentException(errorMessage);    			
    		} else {
    			return new RegionalMaximaFinder2D<T>(image);    			
    		}
    		
    	case 3:
    	
    		if (overTime) {
    			errorMessage = "2D over time is not implemented yet.";
            	throw new IllegalArgumentException(errorMessage);    			
    		} else {    			
    			return new RegionalMaximaFinder3D<T>(image);
    		}
        
    	default:
    		errorMessage = "Dimensionality of " + image.getNumDimensions() + " is not implemented yet.";
        	throw new IllegalArgumentException(errorMessage);

    	}
    }

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public void printProperties() {
		System.out.println( this.getClass().getCanonicalName() + ": " );
		System.out.println("Over time: "+overTime );
		image.getContainerFactory().printProperties();
	}

	/*
	 * No idea what it is meant for.
	 * (non-Javadoc)
	 * @see mpicbg.imglib.Factory#setParameters(java.lang.String)
	 */
	@Override
	public void setParameters(String configuration) { }
}
