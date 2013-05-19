package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.models.Model;
import mpicbg.pointdescriptor.LinkedPoint;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.detection.DetectionView;

public class SparseCorrespondenceExtraction<M extends Model<M>, T extends DetectionView<?,T>> implements CorrespondenceExtraction<T>
{
	int debugLevel;
	final ViewDataBeads viewA, viewB;

	public SparseCorrespondenceExtraction( final ViewDataBeads viewA, final ViewDataBeads viewB, M model )
	{
		this.debugLevel = viewA.getViewStructure().getDebugLevel();
		this.viewA = viewA;
		this.viewB = viewB;
	}
	
	@Override
	public ArrayList<PointMatchGeneric<T>> extractCorrespondenceCandidates( 
			final ArrayList< T > nodeListA, 
			final ArrayList< T > nodeListB, 
			final double differenceThreshold, 
			final double ratioOfDistance, 
			final boolean useAssociatedBeads )
	{ 
		final int minNumCorrespondences = Math.max( viewA.getTile().getModel().getMinNumMatches(), viewB.getTile().getModel().getMinNumMatches() );				

		if ( nodeListA.size() < minNumCorrespondences * 3 )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println( "Not enough beads found in " + viewA + ": " + nodeListA.size() );
			
			return new ArrayList<PointMatchGeneric<T>>();
		}
		if ( nodeListB.size() < minNumCorrespondences * 3 )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println( "Not enough beads found in " + viewB + ": " + nodeListB.size() );
			
			return new ArrayList<PointMatchGeneric<T>>();
		}
		
		/*
		//
		// Create the weka datastructure for 3D Points
		//
		Instances wekaPointsNonSparse = WekaFunctions.insertIntoWekaFloat( viewB.beads.getBeadList(), viewB.shortName );
		
		// 
		// Set up the KDTree
		//
		KDTree kdTreeNonSparse = WekaFunctions.setupKDTree( wekaPointsNonSparse, false );
		*/
		
		// store the candidates for corresponding beads
		final ArrayList<PointMatchGeneric<T>> correspondences = new ArrayList<PointMatchGeneric<T>>();
		
		for ( T sparseBead : nodeListA )
		{
			double minDistance = Double.MAX_VALUE;
			T nearestBead = null;
			
			// update sparse bead with approximate transformation
			//final Bead transformedBead = sparseBead.clone();	
			final LinkedPoint<T> transformedBead = new LinkedPoint<T>( sparseBead.getL(), sparseBead.getW(), sparseBead ); 
			
			transformedBead.apply( viewA.getTile().getModel() );
			
			for ( T nonSparseBead : nodeListB )
			{
				double distance = nonSparseBead.getDistance( transformedBead );
				
				if ( distance < minDistance )
				{
					minDistance = distance;
					nearestBead = nonSparseBead;
				}
			}

			if ( minDistance < 30 )		
			{
				correspondences.add( new PointMatchGeneric<T>( sparseBead, nearestBead, 1.0f ) );
				sparseBead.addPointDescriptorCorrespondence( nearestBead, 1 );
				nearestBead.addPointDescriptorCorrespondence( sparseBead, 1 );
			}
		}
		
		return correspondences;
	}
	
}
