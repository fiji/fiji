package mpicbg.spim.registration.segmentation;

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
public class NucleusIdentification
{
	final protected int nucleusID;
	final ViewDataBeads view;
	
	/**
	 * This constructor is used when a NucleusIdenfication object is initialized during matching from an actual {@link Nucleus} object. 
	 * @param nucleus - The {@link Nucleus} it should identify
	 */
	public NucleusIdentification( final Nucleus nucleus )
	{
		this.nucleusID = nucleus.getID();
		this.view = nucleus.getView();
	}

	/**
	 * This constructor is used when a NucleusIdenfication object is initialized from a file where only the NucleusID and the {@link ViewDataBeads} are known.
	 * @param nucleusID - The NucleusID of the {@link Nucleus} object it links to.
	 * @param view - The {@link ViewDataBeads} object the nucleus belongs to.
	 */
	public NucleusIdentification( final int nucleusID, final ViewDataBeads view )
	{
		this.nucleusID = nucleusID;
		this.view = view;
	}
	
	/**
	 * Return the ID of the {@link Nucleus} object it describes
	 * @return the Nucleus ID
	 */
	public int getNucleusID() { return nucleusID; }
	
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
	 * Prints the nucleus properties
	 */
	public String toString() { return "NucleusIdentification of " + getNucleus().toString(); }	
	
	/**
	 * Returns the actual {@link Nucleus} object it links to
	 * @return the {@link Nucleus} object
	 */
	public Nucleus getNucleus()
	{		
		Nucleus nucleus = null;
		
		// this is just a good guess that might speed up a lot
		if ( nucleusID < view.getNucleiList().size() )
			nucleus = view.getNucleiList().get( nucleusID );

		// check if it is the nucleus with the right ID
		if ( nucleus == null || nucleus.getID() != nucleusID )
		{
			nucleus = null;
			for ( final Nucleus n : view.getNucleiList() )
				if ( n.getID() == nucleusID )
					nucleus = n;

			if ( nucleus == null )
			{
				IOFunctions.printErr("NucleusIdentification.getNucleus(): Cannot find a nucleus for nucleusID=" + nucleusID + " in view=" + view.getID() );
				return null;
			}
		}
		
		return nucleus;
	}
}
