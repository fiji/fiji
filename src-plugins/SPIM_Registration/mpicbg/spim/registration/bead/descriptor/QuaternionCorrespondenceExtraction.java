package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;
import java.util.Iterator;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadCorrespondence;
import mpicbg.spim.registration.bead.OrderPointDescriptorMatch;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

public class QuaternionCorrespondenceExtraction implements CorrespondenceExtraction
{
	final SPIMConfiguration conf;
	public int debugLevel;
	
	public QuaternionCorrespondenceExtraction( final SPIMConfiguration conf, int debugLevel )
	{
		this.conf = conf;
		this.debugLevel = debugLevel;
	}
	
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
				
		//
		// Create the weka datastructure for 3D Points
		//
		Instances wekaPointsViewA = WekaFunctions.insertIntoWekaFloat( viewA.getBeadStructure().getBeadList(), viewA.toString() );
		Instances wekaPointsViewB = WekaFunctions.insertIntoWekaFloat( viewB.getBeadStructure().getBeadList(), viewB.toString() );
		
		// 
		// Set up the KDTrees
		//
		KDTree kdTreeViewA = WekaFunctions.setupKDTree( wekaPointsViewA, false );
		KDTree kdTreeViewB = WekaFunctions.setupKDTree( wekaPointsViewB, false );
		
		
		//KDTree<Bead> kdTreeViewA = KDTreeFunctions.insertIntoKDTree( viewA.beads.getBeadList(), viewA.shortName );
		//KDTree<Bead> kdTreeViewB = KDTreeFunctions.insertIntoKDTree( viewB.beads.getBeadList(), viewB.shortName );
				
		//
		// Set up Point descriptors
		//		
		ArrayList <PointDescriptor> descriptors1 = new ArrayList<PointDescriptor>();
		ArrayList <PointDescriptor> descriptors2 = new ArrayList<PointDescriptor>();
		
		final int numNeighbors = conf.neighbors;
		final int tolerance = 0;
		final int firstNeighborIndex = 1;

		for ( final Iterator<Bead> i = viewA.getBeadStructure().getBeadList().iterator(); i.hasNext(); )
			descriptors1.addAll( BestRigidPointDescriptor.PointDescriptorFactory( kdTreeViewA, i.next(), viewA, numNeighbors, tolerance, firstNeighborIndex) );
		
		for ( final Iterator<Bead> i = viewB.getBeadStructure().getBeadList().iterator(); i.hasNext(); )
			descriptors2.addAll( BestRigidPointDescriptor.PointDescriptorFactory( kdTreeViewB, i.next(), viewB, numNeighbors, tolerance, firstNeighborIndex) );

		// store the candidates for corresponding beads
		final ArrayList<BeadCorrespondence> correspondences = new ArrayList<BeadCorrespondence>();
		
		for (Iterator<PointDescriptor> i = descriptors1.iterator(); i.hasNext();)
		{
			PointDescriptor pd1 = (PointDescriptor)i.next();

			// Best and second best hit for the PointDescriptor
			final int bestN = 2;
			OrderPointDescriptorMatch[] bestPDMatches = new OrderPointDescriptorMatch[bestN];
			
			for ( final Iterator<PointDescriptor> j = descriptors2.iterator(); j.hasNext(); )
			{
				final PointDescriptor pd2 = (PointDescriptor)j.next();
				//double difference = pd1.getLengthNormalizedDifference(pd2);
				final double difference = pd1.getDifference(pd2);
				
				final OrderPointDescriptorMatch opdm = new OrderPointDescriptorMatch(pd2, difference);				
				insertIntoOrderedList(opdm, bestPDMatches);
			}
						
			// TODO: this works only if tolerance == 0
			if (bestPDMatches[0].difference < differenceThreshold && 
				bestPDMatches[1].difference > bestPDMatches[0].difference * ratioOfDistance )
			{
				final PointDescriptor pd2 = bestPDMatches[0].pd;
				
				final float weight = (float)( 1.0f / bestPDMatches[0].difference );
				pd1.getBead().addPointDescriptorCorrespondence( pd2.getBead(), weight );
				pd2.getBead().addPointDescriptorCorrespondence( pd1.getBead(), weight );
				
				if (useAssociatedBeads)
				{
					// add all beads
					Bead[] neighborsViewA = pd1.getNeighborBeads();
					Bead[] neighborsViewB = pd2.getNeighborBeads();

					for (int k = 0; k < neighborsViewA.length; k++)
					{
						Bead beadViewA = neighborsViewA[ k ];
						Bead beadViewB = neighborsViewB[ k ];
						
						if ( !BeadCorrespondence.containsBead( correspondences, beadViewA ) && 
							 !BeadCorrespondence.containsBead( correspondences, beadViewB ) )
						{
							correspondences.add( new BeadCorrespondence( beadViewA, beadViewB, weight ) );
						}
					}
				}

				correspondences.add( new BeadCorrespondence( pd1.getBead(), pd2.getBead(), weight ) );				
			}
		}

		if ( debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println( viewA + "<->" + viewB +  ": Finished comparison, found " + correspondences.size() + " possible correspondences.");

		return correspondences;
	}
	
	protected static void insertIntoOrderedList( final OrderPointDescriptorMatch opdm, final OrderPointDescriptorMatch[] bestPDMatches )
	{
		int describedPoint = opdm.pd.getDescribedPoint();
		
		for (int i = 0; i < bestPDMatches.length; i++)
		{
			// check if the spot is empty, then we are done
			if (bestPDMatches[i] == null)
			{
				bestPDMatches[i] = opdm;
				return;
			}
			
			// if the same point with another tolerance level is already in there we
			// either stop if it is better or we remove it and insert this one
			if (bestPDMatches[i].pd.getDescribedPoint() == describedPoint)
			{
				if (opdm.difference < bestPDMatches[i].difference)
					bestPDMatches[i] = opdm;
				return;
			}

			// if given one is smaller then the current one in the list
			if (opdm.difference < bestPDMatches[i].difference)
			{
				// we have to check if one below describes the same point
				int endPos = bestPDMatches.length - 1;				
				for (int j = i + 1; j < bestPDMatches.length; j++)
					if (bestPDMatches[j] != null && bestPDMatches[j].pd.getDescribedPoint() == describedPoint)
						endPos = j;
				
				// move all below one down
				for (int j = endPos; j > i; --j)
					bestPDMatches[j] = bestPDMatches[j - 1];
				
				bestPDMatches[i] = opdm;

				return;
			}
		}
	}

}
