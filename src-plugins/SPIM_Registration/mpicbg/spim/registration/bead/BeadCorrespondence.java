package mpicbg.spim.registration.bead;

import java.util.ArrayList;
import java.util.Collection;

public class BeadCorrespondence
{
	final private Bead beadA, beadB;
	final private float weight;
	
	public Bead getBeadA() { return beadA; }
	public Bead getBeadB() { return beadB; }
	public float getWeight() { return weight; }
	
	public BeadCorrespondence(final Bead beadA, final Bead beadB, final float weight )
	{
		this.beadA = beadA;
		this.beadB = beadB;
		this.weight = weight;
	}

	public static boolean containsBead( Collection<BeadCorrespondence> list, final Bead bead )
	{
		boolean contains = false;
		
		for ( final BeadCorrespondence content : list )
			if ( Bead.equals( bead, content.beadA) || Bead.equals( bead, content.beadB) )
				contains = true;
		
		return contains;
	}
	
	public static ArrayList<Integer> getOccurences( final Bead bead, ArrayList<BeadCorrespondence> list )
	{
		ArrayList<Integer> occurences = new ArrayList<Integer>();
		
		for ( int i = 0; i < list.size(); i++ )
		{
			BeadCorrespondence c = list.get( i );
			if ( Bead.equals( c.beadA, bead ) || Bead.equals( c.beadB, bead ) )
				occurences.add( i );
		}
		
		return occurences;
	}

	public static ArrayList<Integer> getOccurencesA( final Bead bead, ArrayList<BeadCorrespondence> list )
	{
		ArrayList<Integer> occurences = new ArrayList<Integer>();
		
		for ( int i = 0; i < list.size(); i++ )
		{
			BeadCorrespondence c = list.get( i );
			if ( Bead.equals( c.beadA, bead ) )
				occurences.add( i );
		}
		
		return occurences;
	}

	public static ArrayList<Integer> getOccurencesB( final Bead bead, ArrayList<BeadCorrespondence> list )
	{
		ArrayList<Integer> occurences = new ArrayList<Integer>();
		
		for ( int i = 0; i < list.size(); i++ )
		{
			BeadCorrespondence c = list.get( i );
			if ( Bead.equals( c.beadB, bead ) )
				occurences.add( i );
		}
		
		return occurences;
	}
}
