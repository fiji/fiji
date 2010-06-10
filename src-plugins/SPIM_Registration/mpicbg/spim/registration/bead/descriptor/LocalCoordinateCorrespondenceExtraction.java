package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;
import java.util.Iterator;

import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadCorrespondence;

public class LocalCoordinateCorrespondenceExtraction implements CorrespondenceExtraction
{
	public static final boolean NORMALIZE = true;
	public static final boolean DONOT_NORMALIZE = false;
	
	final SPIMConfiguration conf;
	public boolean normalize;
	public int debugLevel;
	
	public LocalCoordinateCorrespondenceExtraction( final SPIMConfiguration conf, boolean normalize, int debugLevel )
	{
		this.conf = conf;
		this.normalize = normalize;
		this.debugLevel = debugLevel;
	}

	@Override
	public ArrayList<BeadCorrespondence> extractCorrespondenceCandidates(ViewDataBeads viewA, ViewDataBeads viewB, double differenceThreshold, double ratioOfDistance, boolean useAssociatedBeads)
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
		// Set up the KDTrees for finding the nearest beads in the same image
		//
		KDTree neighborKDTreeViewA = WekaFunctions.setupKDTree( wekaPointsViewA, false );
		KDTree neighborKDTreeViewB = WekaFunctions.setupKDTree( wekaPointsViewB, false );

		//
		// Set up Point descriptors
		//		
		ArrayList <LocalCoordinatePointDescriptor> descriptorsA = new ArrayList<LocalCoordinatePointDescriptor>();
		ArrayList <LocalCoordinatePointDescriptor> descriptorsB = new ArrayList<LocalCoordinatePointDescriptor>();

		final int numNeighbors = conf.neighbors;
		final int tolerance = 0;
		final int firstNeighborIndex = 1;

		for ( final Iterator<Bead> i = viewA.getBeadStructure().getBeadList().iterator(); i.hasNext(); )
			descriptorsA.addAll( LocalCoordinatePointDescriptor.PointDescriptorFactory( neighborKDTreeViewA, i.next(), viewA, numNeighbors, tolerance, firstNeighborIndex, normalize) );
		
		for ( final Iterator<Bead> i = viewB.getBeadStructure().getBeadList().iterator(); i.hasNext(); )
			descriptorsB.addAll( LocalCoordinatePointDescriptor.PointDescriptorFactory( neighborKDTreeViewB, i.next(), viewB, numNeighbors, tolerance, firstNeighborIndex, normalize) );

		// Create the lookup KDTree for ViewB - this is for matching the Point Descriptors from different views
		Instances lookUpPointsB = WekaFunctions.insertIntoWekaFloat( descriptorsB, viewB.toString(), normalize );		
		KDTree lookUpKDTreeB = WekaFunctions.setupKDTree( lookUpPointsB, false );
		
		// store the candidates for corresponding beads
		final ArrayList<BeadCorrespondence> correspondences = new ArrayList<BeadCorrespondence>();
		
		for (Iterator<LocalCoordinatePointDescriptor> i = descriptorsA.iterator(); i.hasNext();)
		{
			LocalCoordinatePointDescriptor pd1 = i.next();
			LocalCoordinatePointDescriptor pd2 = pd1.getMatch( lookUpKDTreeB, descriptorsB, differenceThreshold, ratioOfDistance );
			final float weight = 1;
			
			if ( pd2 != null )
			{
				pd1.getBead().addPointDescriptorCorrespondence( pd2.getBead(), weight );
				pd2.getBead().addPointDescriptorCorrespondence( pd1.getBead(), weight );
				
				correspondences.add( new BeadCorrespondence( pd1.getBead(), pd2.getBead(), weight ) );				
			}
		}
		
		if ( debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println( viewA + "<->" + viewB +  ": Finished comparison, found " + correspondences.size() + " possible correspondences.");
		
		return correspondences;
	}
	
	public int getNumInconsistentCorrespondences( ArrayList<BeadCorrespondence> correspondences, final ViewDataBeads viewA, final ViewDataBeads viewB )
	{
		ArrayList<Integer> inconsistentCorrespondences = new ArrayList<Integer>();
		
		for ( int i = 0; i < correspondences.size(); i++ )
		{
			Bead beadViewB = correspondences.get( i ).getBeadB();
			ArrayList<Integer> inconsistent = BeadCorrespondence.getOccurencesB( beadViewB, correspondences );
			
			if ( inconsistent.size() > 1 )
			{
				for ( int index : inconsistent )
					if ( !inconsistentCorrespondences.contains( index ) )						
						inconsistentCorrespondences.add( index );
			}
		}

		return inconsistentCorrespondences.size();
	}
	
}
