package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.EventObject;

import fiji.plugin.trackmate.Spot;

/**
 * An event that characterizes a change in the current selection. Based on the
 * GraphSelectionEvent by Gaudenz Alder in the JGraph project.
 */
public class SpotSelectionEvent extends EventObject {

	private static final long serialVersionUID = -371486225561393185L;

	/** Spots this event represents. */
	protected Spot[] spots;

	/**
	 * For each spot identifies whether or not that spot is newly selected.
	 */
	protected boolean[] areNew;

	/*
	 * CONSTRUCTORS 
	 */
	
	/**
	 * Represents a change in the selection of a displayed spot model.
	 * <code>spots</code> identifies the spots that have been either added or
	 * removed from the selection.
	 * 
	 * @param source  source of event
	 * @param spots  the spots that have changed in the selection
	 * @param areNew  for each spot, defines whether or not that spot is newly selected
	 */
	public SpotSelectionEvent(Object source, Spot[] spots, boolean[] areNew) {
		super(source);
		this.spots = spots;
		this.areNew = areNew;
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Returns the spots that have been added or removed from the selection.
	 * 
	 * @return added or removed spots
	 */
	public Spot[] getSpots() {
		int nspots = spots.length;
		Spot[] retSpots = new Spot[nspots];
		System.arraycopy(spots, 0, retSpots, 0, nspots);
		return retSpots;
	}

	/**
	 * Returns the first spot in selection.
	 */
	public Spot getSpot() {
		return spots[0];
	}

	/**
	 * Returns true if the first spot has been added to the selection, a return
	 * value of false means the first spot has been removed from the selection.
	 */
	public boolean isAddedSpot() {
		return areNew[0];
	}

	/**
	 * Returns true if the spot identified by <code>spot</code> was added to the selection. A
	 * return value of false means the spot was in the selection but is no
	 * longer in the selection.
	 * @param spot the spot that is to be indicated as newly selected or not
	 * @return <code>true</code> if the specified spot is newly selected
	 */
	public boolean isAddedSpot(Spot cell) {
		for (int counter = spots.length - 1; counter >= 0; counter--)
			if (spots[counter].equals(cell))
				return areNew[counter];
		throw new IllegalArgumentException("spot is not a spot identified by the SpotSelectionEvent");
	}

	/**
	 * Returns true if the spot identified by <code>index</code> was added to
	 * the selection. A return value of false means the spot was in the
	 * selection but is no longer in the selection. 
	 * @param index  the index of the spot that is to be indicated as newly selected or not
	 * @return whether or not the spot is newly selected or not
	 */
	public boolean isAddedSpot(int index) {
		if (spots == null || index < 0 || index >= spots.length) {
			throw new IllegalArgumentException(
					"index is beyond range of added spots identified by SpotSelectionEvent");
		}
		return areNew[index];
	}


}
