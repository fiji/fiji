package mpicbg.pointdescriptor.matcher;

import java.util.ArrayList;

import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.AbstractPointDescriptor;

public class SimpleMatcher implements Matcher
{
	final int numNeighbors;
	
	public SimpleMatcher( final int numNeighbors )
	{
		this.numNeighbors = numNeighbors;
	}
	
	@Override
	public ArrayList<ArrayList<PointMatch>> createCandidates( final AbstractPointDescriptor<?, ?> pd1, final AbstractPointDescriptor<?, ?> pd2 )
	{
		final ArrayList<PointMatch> matches = new ArrayList<PointMatch>( numNeighbors );		
		
		for ( int i = 0; i < numNeighbors; ++i )
		{
			final PointMatch pointMatch = new PointMatch( pd1.getDescriptorPoint( i ), pd2.getDescriptorPoint( i ) );
			matches.add( pointMatch );
		}		

		final ArrayList<ArrayList<PointMatch>> matchesList = new ArrayList<ArrayList<PointMatch>>();		
		matchesList.add( matches );
		
		return matchesList;
	}

	@Override
	public int getRequiredNumNeighbors() { return numNeighbors; }

	@Override
	public double getNormalizationFactor( final ArrayList<PointMatch> matches, final Object fitResult ) { return 1;	}
}
