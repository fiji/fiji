package mpicbg.spim.registration.bead;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

/**
 * The BeadIdentification object stores the link (via ID) to an Bead-object and not an actual instance, but a link to the ViewDataBeads object where it belongs to.
 * This is necessary for storing/loading Bead-relationships to/from a text file. The Bead-objects of every {@link ViewDataBeads} have an {@link ArrayList} of BeadIdentification
 * objects telling which other Beads are correspondence candidates or true correspondences.
 *  
 * @author Stephan Preibisch
 *
 */
public class BeadIdentification
{
	final protected int beadID;
	final ViewDataBeads view;
	
	/**
	 * This constructor is used when a BeadIdenfication object is initialized during matching from an actual {@link Bead} object. 
	 * @param bead - The {@link Bead} it should identify
	 */
	public BeadIdentification( final Bead bead )
	{
		this.beadID = bead.getID();
		this.view = bead.getView();
	}

	/**
	 * This constructor is used when a BeadIdenfication object is initialized from a file where only the BeadID and the {@link ViewDataBeads} are known.
	 * @param beadID - The BeadID of the {@link Bead} object it links to.
	 * @param view - The {@link ViewDataBeads} object the bead belongs to.
	 */
	public BeadIdentification( final int beadID, final ViewDataBeads view )
	{
		this.beadID = beadID;
		this.view = view;
	}
	
	/**
	 * Return the ID of the {@link Bead} object it describes
	 * @return the Bead ID
	 */
	public int getBeadID() { return beadID; }
	
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
	public String toString() { return "BeadIdentification of " + getBead().toString(); }	
	
	/**
	 * Returns the actual {@link Bead} object it links to
	 * @return the {@link Bead} object
	 */
	public Bead getBead()
	{		
		// this is just a good guess that might speed up a lot
		Bead bead = view.getBeadStructure().getBead( beadID );

		// check if it is the bead with the right ID
		if ( bead.getID() != beadID )
		{
			bead = null;
			for ( final Bead b : view.getBeadStructure().getBeadList() )
				if ( bead.getID() == beadID )
					bead = b;

			if ( bead == null )
			{
				IOFunctions.printErr("BeadIdentification.getBead(): Cannot find a bead for beadID=" + beadID + " in view=" + view.getID() );
				return null;
			}
		}
		
		return bead;
	}
}
