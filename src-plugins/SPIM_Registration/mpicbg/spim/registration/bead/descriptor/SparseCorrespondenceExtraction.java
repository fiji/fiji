package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;

import mpicbg.models.Model;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadCorrespondence;

public class SparseCorrespondenceExtraction<M extends Model<M>> implements CorrespondenceExtraction
{
	M initialTransform;
	int debugLevel;

	public SparseCorrespondenceExtraction( M model, int debugLevel )
	{
		this.initialTransform = model;
		this.debugLevel = debugLevel;
	}
	
	public void setInitialTransform( final M model ) { this.initialTransform = model; }
	public M getInitialTransform(){ return initialTransform; }
	
	@Override
	public ArrayList<BeadCorrespondence> extractCorrespondenceCandidates( final ViewDataBeads viewA, final ViewDataBeads viewB, final double differenceThreshold, final double ratioOfDistance, final boolean useAssociatedBeads )
	{ 
		final int minNumCorrespondences = Math.max( viewA.getTile().getModel().getMinNumMatches(), viewB.getTile().getModel().getMinNumMatches() );				

		if ( viewA.getBeadStructure().getBeadList().size() < minNumCorrespondences * 3 )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Not enough beads found in " + viewA + ": " + viewA.getBeadStructure().getBeadList().size());
			return new ArrayList<BeadCorrespondence>();
		}
		if ( viewB.getBeadStructure().getBeadList().size() < minNumCorrespondences * 3 )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("Not enough beads found in " + viewB + ": " + viewB.getBeadStructure().getBeadList().size());
			return new ArrayList<BeadCorrespondence>();
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
		final ArrayList<BeadCorrespondence> correspondences = new ArrayList<BeadCorrespondence>();
		
		for ( Bead sparseBead : viewA.getBeadStructure().getBeadList() )
		{
			double minDistance = Double.MAX_VALUE;
			Bead nearestBead = null;
			
			// update sparse bead with approximate transformation
			final Bead transformedBead = sparseBead.clone();			
			transformedBead.apply( viewA.getTile().getModel() );
			
			for ( Bead nonSparseBead : viewB.getBeadStructure().getBeadList() )
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
				correspondences.add( new BeadCorrespondence( sparseBead, nearestBead, 1.0f ) );
				sparseBead.addPointDescriptorCorrespondence( nearestBead, 1 );
				nearestBead.addPointDescriptorCorrespondence( sparseBead, 1 );
			}
		}
		
		return correspondences;
	}
	
}
