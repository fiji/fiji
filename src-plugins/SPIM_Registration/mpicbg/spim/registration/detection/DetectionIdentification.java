package mpicbg.spim.registration.detection;

import java.util.ArrayList;

import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;

/**
 * The DetectionIdentification object stores the link (via ID) to an Detection-object and not an actual instance, but a link to the ViewDataBeads object where it belongs to.
 * This is necessary for storing/loading {@link Detection}on-relationships to/from a text file. The {@link Detection}-objects of every {@link ViewDataBeads} have an {@link ArrayList} 
 * of {@link DetectionIdentification} objects telling which other {@link Detection}s are correspondence candidates or true correspondences.
 *  
 * @author Stephan Preibisch
 *
 */
public abstract class DetectionIdentification< S extends DetectionIdentification<S, T>, T extends DetectionView< S, T > > 
{
	final protected int detectionID;
	final protected ViewDataBeads view;

	/**
	 * This constructor is used when a BeadIdenfication object is initialized during matching from an actual {@link Bead} object. 
	 * @param bead - The {@link Bead} it should identify
	 */
	public DetectionIdentification( final DetectionView< S, T > detection )
	{
		this.detectionID = detection.getID();
		this.view = detection.getView();
	}
	
	/**
	 * This constructor is used when a BeadIdenfication object is initialized from a file where only the BeadID and the {@link ViewDataBeads} are known.
	 * @param beadID - The BeadID of the {@link Bead} object it links to.
	 * @param view - The {@link ViewDataBeads} object the bead belongs to.
	 */
	public DetectionIdentification( final int detectionID, final ViewDataBeads view )
	{
		this.detectionID = detectionID;
		this.view = view;
	}
	
	/**
	 * Return the ID of the {@link Bead} object it describes
	 * @return the Bead ID
	 */
	public int getDetectionID() { return detectionID; }
	
	/**
	 * Returns the {@link ViewDataBeads} object of the view it belongs to.
	 * @return {@link ViewDataBeads}
	 */
	public ViewDataBeads getView() { return view; }
	
	/**
	 * Returns the ID of the view it belongs to.
	 * @return ID of the view
	 */
	public int getViewID() { return getView().getID(); }

	/**
	 * Prints the bead properties
	 */
	public String toString() { return "DetectionIdentification of " + getDetection().toString(); }	
	
	/**
	 * Returns the actual {@link Bead} object it links to
	 * @return the {@link Bead} object
	 */
	public abstract T getDetection();
	/*{		
		// this is just a good guess that might speed up a lot
		T detection = view.getDetectionStructure().getDetection( detectionID );

		// check if it is the bead with the right ID
		if ( detection.getID() != detectionID )
		{
			detection = null;
			for ( final T d : view.getDetectionStructure().getDetectionList() )
				if ( d.getID() == detectionID )
					detection = d;

			if ( detection == null )
			{
				IOFunctions.printErr( "DetectionIdentification.getDetection(): Cannot find a detection for detectionID=" + detectionID + " in view=" + view.getID() );
				return null;
			}
		}
		
		return detection;
	}
	*/
}
