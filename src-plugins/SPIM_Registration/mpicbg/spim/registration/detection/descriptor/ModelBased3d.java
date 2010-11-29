package mpicbg.spim.registration.detection.descriptor;

import java.util.ArrayList;

import mpicbg.models.Point;
import mpicbg.pointdescriptor.ModelPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.matcher.SimpleMatcher;
import mpicbg.pointdescriptor.model.TranslationInvariantModel;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel3D;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;
import mpicbg.pointdescriptor.similarity.SquareDistance;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.registration.detection.DetectionView;
import fiji.util.KDTree;
import fiji.util.NNearestNeighborSearch;
import fiji.util.node.Leaf;

public class ModelBased3d<T extends DetectionView<?,T>> implements CorrespondenceExtraction<T>
{
	final Matcher matcher;
	final TranslationInvariantModel<?> model;
	
	public ModelBased3d( final TranslationInvariantModel<?> model, final Matcher matcher )
	{
		this.matcher = matcher;
		this.model = model;
	}
	
	public ModelBased3d( final TranslationInvariantModel<?> model )
	{
		this( model, new SimpleMatcher( 3 ) );
	}
	
	public ModelBased3d( final Matcher matcher ) 
	{ 
		this ( new TranslationInvariantRigidModel3D(), matcher ); 
	}
	
	public ModelBased3d() 
	{ 
		this( new SimpleMatcher( 3 ) ); 
	}	
	
	@Override
	public ArrayList<PointMatchGeneric<T>> extractCorrespondenceCandidates( 
			final ArrayList< T > nodeListA, 
			final ArrayList< T > nodeListB, 
			final double differenceThreshold, 
			final double ratioOfDistance, 
			final boolean useAssociatedBeads) 
	{
		/* create KDTrees */	
		final KDTree< T > treeA = new KDTree< T >( nodeListA );
		final KDTree< T > treeB = new KDTree< T >( nodeListB );
		
		/* extract point descriptors */						
		final int numNeighbors = matcher.getRequiredNumNeighbors();
		
		//final TranslationInvariantModel<?> model = new TranslationInvariantRigidModel3D();
		final SimilarityMeasure similarityMeasure = new SquareDistance();
				
		final ArrayList< ModelPointDescriptor< T > > descriptors1 = 
			createModelPointDescriptors( treeA, nodeListA, numNeighbors, model, matcher, similarityMeasure );
		
		final ArrayList< ModelPointDescriptor< T > > descriptors2 = 
			createModelPointDescriptors( treeB, nodeListB, numNeighbors, model, matcher, similarityMeasure );
		
		// store the candidates for corresponding beads
		final ArrayList<PointMatchGeneric<T>> correspondences = new ArrayList<PointMatchGeneric<T>>();

		/* compute matching */
		for ( final ModelPointDescriptor< T > descriptorA : descriptors1 )
		{			
			double bestDifference = Double.MAX_VALUE;			
			double secondBestDifference = Double.MAX_VALUE;
			
			ModelPointDescriptor< T > bestMatch = null;
			ModelPointDescriptor< T > secondBestMatch = null;
			
			for ( final ModelPointDescriptor< T > descriptorB : descriptors2 )
			{
				final double difference = descriptorA.descriptorDistance( descriptorB );
				
				if ( difference < secondBestDifference )
				{					
					secondBestDifference = difference;
					secondBestMatch = descriptorB;
					
					if ( secondBestDifference < bestDifference )
					{
						double tmpDiff = secondBestDifference;
						ModelPointDescriptor< T > tmpMatch = secondBestMatch;
						
						secondBestDifference = bestDifference;
						secondBestMatch = bestMatch;
						
						bestDifference = tmpDiff;
						bestMatch = tmpMatch;
					}
				}								
			}
			
			if ( bestDifference < differenceThreshold && bestDifference * ratioOfDistance <= secondBestDifference )
			{
				final T detectionA = descriptorA.getBasisPoint();
				final T detectionB = bestMatch.getBasisPoint();
				
				detectionA.addPointDescriptorCorrespondence( detectionB, 1 );
				detectionB.addPointDescriptorCorrespondence( detectionA, 1 );
				
				correspondences.add( new PointMatchGeneric<T>( detectionA, detectionB, 1 ) );
			}							
		}
		
		return correspondences;
	}

	public static <P extends Point & Leaf<P>> ArrayList< ModelPointDescriptor< P > > createModelPointDescriptors( final KDTree< P > tree, 
               final ArrayList< P > basisPoints, 
               final int numNeighbors, 
               final TranslationInvariantModel<?> model, 
               final Matcher matcher, 
               final SimilarityMeasure similarityMeasure )
	{
		final NNearestNeighborSearch< P > nnsearch = new NNearestNeighborSearch< P >( tree );
		final ArrayList< ModelPointDescriptor< P > > descriptors = new ArrayList< ModelPointDescriptor< P > > ( );
		
		for ( final P point : basisPoints )
		{
			final ArrayList< P > neighbors = new ArrayList< P >();
			final P neighborList[] = nnsearch.findNNearestNeighbors( point, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
				neighbors.add( neighborList[ n ] );
			
			try
			{
				descriptors.add( new ModelPointDescriptor<P>( point, neighbors, model, similarityMeasure, matcher ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
		
		return descriptors;
	}
	
}
