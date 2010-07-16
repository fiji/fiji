package mpicbg.spim.registration.bead;

import java.util.ArrayList;

public class BeadStructure
{
	final ArrayList<Bead> beads = new ArrayList<Bead>();
		
	public void addBead( final Bead bead ) { beads.add( bead ); }
	public ArrayList<Bead> getBeadList() { return beads; }
	public Bead getBead( final int i ) { return beads.get( i ); }
	
	public Bead getBead( final float x, final float y, final float z )
	{
		for ( final Bead b : getBeadList() )
		{
			float[] location = b.getL();
			
			if ( x == location[0] && y == location[1] && z == location[2] )
				return b;
		}
		return null;
	}

	public void clearAllCorrespondenceCandidates()
	{
		for ( final Bead bead : getBeadList() )
			bead.getDescriptorCorrespondence().clear();
	}
	
	public void clearAllRANSACCorrespondences()
	{
		for ( final Bead bead : getBeadList() )
			bead.getRANSACCorrespondence().clear();
	}
}
