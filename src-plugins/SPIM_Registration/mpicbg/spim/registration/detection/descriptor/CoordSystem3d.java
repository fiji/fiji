package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.pointdescriptor.LocalCoordinateSystemPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.detection.DetectionView;
import fiji.util.KDTree;
import fiji.util.NNearestNeighborSearch;
import fiji.util.node.Leaf;

public class CoordSystem3d<T extends DetectionView<?,T>> implements CorrespondenceExtraction<T>
{
	@Override
	public ArrayList<PointMatchGeneric<T>> extractCorrespondenceCandidates( 
			final ArrayList< T > nodeListA, 
			final ArrayList< T > nodeListB, 
			double differenceThreshold, 
			double ratioOfDistance, 
			final boolean useAssociatedBeads ) 
	{
		final int numNeighbors = 3;
		
		// this is slightly less expressive
		//ratioOfDistance /= 1.25;
		//differenceThreshold *= 1.25;
		
		final KDTree< T > tree1 = new KDTree< T >( nodeListA );
		final KDTree< T > tree2 = new KDTree< T >( nodeListB );

		final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors1 = 
			createLocalCoordinateSystemPointDescriptors( tree1, nodeListA, numNeighbors, false );
		
		final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors2 = 
			createLocalCoordinateSystemPointDescriptors( tree2, nodeListB, numNeighbors, false );
		
		// create lookup tree for descriptors2		
		final KDTree< LocalCoordinateSystemPointDescriptor< T > > lookUpTree2 = new KDTree< LocalCoordinateSystemPointDescriptor< T > >( descriptors2 );
		final NNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< T > > nnsearch = new NNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< T > >( lookUpTree2 );
	
		// store the candidates for corresponding beads
		final ArrayList<PointMatchGeneric<T>> correspondences = new ArrayList<PointMatchGeneric<T>>();
		
		/* compute matching */
		computeMatching( descriptors1, nnsearch, correspondences, differenceThreshold, ratioOfDistance );
		
		return correspondences;
	}
	
	protected void computeMatching( 
			final ArrayList< LocalCoordinateSystemPointDescriptor< T > > descriptors1, 
			final NNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< T > > nnsearch2,
			final ArrayList<PointMatchGeneric<T>> correspondences, 
			final double differenceThreshold, 
			final double ratioOfDistance )
	{
		//System.out.println( "BeadA" + "\t" + "BeadB1" + "\t" + "BeadB2" + "\t" + "Diff1" + "\t" + "Diff2" );

		for ( final LocalCoordinateSystemPointDescriptor< T > descriptorA : descriptors1 )
		{
			final LocalCoordinateSystemPointDescriptor< T > matches[] = nnsearch2.findNNearestNeighbors( descriptorA, 2 );

			double best = descriptorA.descriptorDistance( matches[ 0 ] );
			double secondBest = descriptorA.descriptorDistance( matches[ 1 ] );

			if ( best < differenceThreshold && best * ratioOfDistance <= secondBest )
			{
				final T detectionA = descriptorA.getBasisPoint();
				final T detectionB = matches[ 0 ].getBasisPoint();

				//System.out.println( beadA.getID() + "\t" + matches[ 0 ].getBasisPoint().getID() + "\t" + matches[ 1 ].getBasisPoint().getID() + "\t" + best + "\t" + secondBest );

				detectionA.addPointDescriptorCorrespondence( detectionB, 1 );
				detectionB.addPointDescriptorCorrespondence( detectionA, 1 );
				
				correspondences.add( new PointMatchGeneric<T>( detectionA, detectionB, 1 ) );
			}				
		}					
		
		//System.exit( 0 );
	}

	public static <P extends Point & Leaf<P>> ArrayList< LocalCoordinateSystemPointDescriptor< P > > createLocalCoordinateSystemPointDescriptors( 
			final KDTree< P > tree, 
            final ArrayList< P > basisPoints, 
            final int numNeighbors,
            final boolean normalize )
	{
		final NNearestNeighborSearch< P > nnsearch = new NNearestNeighborSearch< P >( tree );
		final ArrayList< LocalCoordinateSystemPointDescriptor< P > > descriptors = new ArrayList< LocalCoordinateSystemPointDescriptor< P > > ( );
		
		for ( final P point : basisPoints )
		{
			final ArrayList< P > neighbors = new ArrayList< P >();
			final P neighborList[] = nnsearch.findNNearestNeighbors( point, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
			neighbors.add( neighborList[ n ] );
			
			try
			{
				descriptors.add( new LocalCoordinateSystemPointDescriptor<P>( point, neighbors, normalize ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
		
		return descriptors;
	}
	
}
