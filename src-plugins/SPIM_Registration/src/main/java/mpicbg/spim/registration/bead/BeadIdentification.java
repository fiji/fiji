package mpicbg.spim.registration.bead;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.detection.DetectionIdentification;

public class BeadIdentification extends DetectionIdentification<BeadIdentification, Bead> 
{
	public BeadIdentification( final Bead detection ) 
	{
		super( detection );
	}

	public BeadIdentification( final int detectionID, final ViewDataBeads view )
	{
		super( detectionID, view );
	}

	public long getBeadID() { return detectionID; }

	/**
	 * Prints the nucleus properties
	 */
	public String toString() { return "BeadIdentification of " + getDetection().toString(); }	

	public Bead getBead() { return getDetection(); }
	
	@Override
	public Bead getDetection() 
	{
         // this is just a good guess that might speed up a lot
         Bead bead = view.getBeadStructure().getDetection( detectionID );

         // check if it is the bead with the right ID
         if ( bead.getID() != detectionID )
         {
                bead = null;
                for ( final Bead b : view.getBeadStructure().getDetectionList() )
                       if ( bead.getID() == detectionID )
                              bead = b;

                if ( bead == null )
                {
                	IOFunctions.printErr( "BeadIdentification.getBead(): Cannot find a bead for beadID=" + detectionID + " in view=" + view.getID() );
                    return null;
                }
         }
         return bead;
   }

}
