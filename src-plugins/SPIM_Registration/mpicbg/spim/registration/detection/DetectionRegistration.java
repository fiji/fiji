package mpicbg.spim.registration.detection;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.pointdescriptor.LinkedPoint;
import mpicbg.pointdescriptor.model.RigidModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.PointMatchGeneric;

public class DetectionRegistration 
{
	/**
	*  Remove possible inconsistent correspondences 
	*  (where one bead in viewB corresponds to more than one bead in viewA)
	*/
	public synchronized static <S extends DetectionIdentification<S,T>, T extends DetectionView<S,T>> void removeInconsistentCorrespondences( final ArrayList<PointMatchGeneric<T>> correspondences )
	{
		final ArrayList<Integer> inconsistentCorrespondences = new ArrayList<Integer>();
	
		for ( int i = 0; i < correspondences.size(); i++ )
		{
			final T detectionA = correspondences.get( i ).getPoint1();
			final T detectionB = correspondences.get( i ).getPoint2();
			final ArrayList<Integer> inconsistent = getOccurences( detectionA, detectionB, correspondences );
			
			if ( inconsistent.size() > 0 )
				for ( int index : inconsistent )
					if ( !inconsistentCorrespondences.contains( index ) )						
						inconsistentCorrespondences.add( index );
		}

		if ( inconsistentCorrespondences.size() > 0 )
		{
			Collections.sort( inconsistentCorrespondences );
						
			for ( int i = inconsistentCorrespondences.size() - 1; i >= 0; i-- )
			{
				final PointMatchGeneric<T> pm = correspondences.get( inconsistentCorrespondences.get(i) );
				
				final T detectionA = pm.getPoint1();
				final T detectionB = pm.getPoint2();

				// remove link for nucleusA
				boolean removed = false;
				for ( int j = 0; j < detectionA.getDescriptorCorrespondence().size(); ++j )
				{
					final S n = detectionA.getDescriptorCorrespondence().get( j );
					if ( n.getDetectionID() == detectionB.getID() && n.getViewID() == detectionB.getViewID() )
					{
						detectionA.getDescriptorCorrespondence().remove( j );
						removed = true;
						break;
					}					
				}
				
				if ( !removed )
					IOFunctions.println("Error removing ambigous descriptor correspondence for detectionA: " + detectionA.getID() + " vs. " + detectionB.getID() );

				// remove link for nucleusB
				removed = false;
				for ( int j = 0; j < detectionB.getDescriptorCorrespondence().size(); ++j )
				{
					final DetectionIdentification<?, ?> n = detectionB.getDescriptorCorrespondence().get( j );
					if ( n.getDetectionID() == detectionA.getID() && n.getViewID() == detectionA.getViewID() )
					{
						detectionB.getDescriptorCorrespondence().remove( j );
						removed = true;
						break;
					}
				}
				
				if ( !removed )
					IOFunctions.println("Error removing ambigous descriptor correspondence for detectionB: " + detectionB.getID() + " vs. " + detectionA.getID() );
								
				// the cast to (int) is essential as otherwise he is looking to remove the Integer object that does not exist in the list 
				correspondences.remove( (int)inconsistentCorrespondences.get(i) );
			}
		}	
	}
	
	protected static <S extends DetectionIdentification<S,T>, T extends DetectionView<S,T>> ArrayList<Integer> getOccurences( 
			final T detectionA, 
			final T detectionB, 
			final ArrayList<PointMatchGeneric<T>> list )
	{
		final ArrayList<Integer> occurences = new ArrayList<Integer>();
		
		boolean differentOccurence = false;
				
		/* Test if nucleusB has matches with different nuclei than nucleusA */
		for ( final PointMatchGeneric<T> pm : list )
		{			
			if ( detectionB.equals( pm.getPoint2() ) )
			{
				// it is NOT twice the correct occurence
				if ( !detectionA.equals( pm.getPoint1() ) )
				{
					differentOccurence = true;
					break;
				}
			}

			if ( detectionA.equals( pm.getPoint1() ) )
			{
				// it is NOT twice the correct occurence
				if ( !detectionB.equals( pm.getPoint2() ) )
				{
					differentOccurence = true;
					break;
				}				
			}
		}
		
		if ( differentOccurence )
		{
			/* remove all occurences/matches with nucleusB as it is ambigous */
			for ( int i = 0; i < list.size(); i++ )
			{
				final PointMatchGeneric<T> pm = list.get( i );
				
				if ( detectionB.equals( pm.getPoint2() ) )
					occurences.add( i );
				
				if ( detectionA.equals( pm.getPoint1() ) )
					occurences.add( i );
			}
		}
		else
		{
			/* remove all occurences/matches with nucleusB as it is ambigous */
			boolean sameOccurence = false;

			for ( int i = 0; i < list.size(); i++ )
			{
				final PointMatchGeneric<T> pm = list.get( i );
				
				/* remove all but the first occurence/match */
				if ( detectionB.equals( pm.getPoint2() ) )
				{
					if ( sameOccurence )
						occurences.add( i );
					else
						sameOccurence = true;
				}
			}			
		}
		
		return occurences;
	}

	/**
	 * Computes the RANSAC with reasonable parameters
	 * @param correspondenceCandidates - the candidates
	 * @param inlierList - the list of inliers that will be eventually populated
	 * @param model - the model to use
	 * @return the String that describes the result for feedback
	 */
	public static <S extends DetectionIdentification<S,T>, T extends DetectionView<S,T> > String computeRANSAC( 
			final ArrayList<PointMatchGeneric<T>> correspondenceCandidates, 
			final ArrayList<PointMatchGeneric<T>> inlierList, 
			final Model<?> model )
	{
		return computeRANSAC( correspondenceCandidates, inlierList, model, 10, 0.1f, 3, 10000 );
	}

	public static <S extends DetectionIdentification<S,T>, T extends DetectionView<S,T> > String computeRANSAC( 
			final ArrayList<PointMatchGeneric<T>> correspondenceCandidates, 
			final ArrayList<PointMatchGeneric<T>> inlierList, 
			final Model<?> model, 
			final float maxEpsilon, 
			final float minInlierRatio, 
			final int minNumberInlierFactor, 
			final int numIterations)
	{
		final int numCorrespondences = correspondenceCandidates.size();
		final int minNumCorrespondences = model.getMinNumMatches() * minNumberInlierFactor;
		
		/*
		 * First remove the inconsistent correspondences
		 */
		removeInconsistentCorrespondences( correspondenceCandidates );

		// if there are not enough correspondences for the used model
		if ( numCorrespondences < minNumCorrespondences )
			return "Not enough correspondences found " + numCorrespondences + ", should be at least " + minNumCorrespondences;

		/**
		 * The ArrayList that stores the inliers after RANSAC, contains PointMatches of LinkedPoints
		 * so that MultiThreading is possible
		 */
		//final ArrayList< PointMatchGeneric<LinkedPoint<T>> > candidates = new ArrayList<PointMatchGeneric<LinkedPoint<T>>>();		
		final ArrayList< PointMatch > candidates = new ArrayList<PointMatch>();
		final ArrayList< PointMatch > inliers = new ArrayList<PointMatch>();
		
		// clone the beads for the RANSAC as we are working multithreaded and they will be modified
		for ( final PointMatchGeneric<T> correspondence : correspondenceCandidates )
		{
			final T detectionA = correspondence.getPoint1();
			final T detectionB = correspondence.getPoint2();
			
			final LinkedPoint<T> p1 = new LinkedPoint<T>( detectionA.getL(), detectionA.getW(), detectionA );
			final LinkedPoint<T> p2 = new LinkedPoint<T>( detectionB.getL(), detectionB.getW(), detectionB );
			final float weight = correspondence.getWeight(); 

			candidates.add( new PointMatchGeneric<LinkedPoint<T>>( p1, p2, weight ) );
		}
		
		boolean modelFound = false;
		
		try
		{
			/*modelFound = m.ransac(
  					candidates,
					inliers,
					numIterations,
					maxEpsilon, minInlierRatio );*/
		
			modelFound = model.filterRansac(
					candidates,
					inliers,
					numIterations,
					maxEpsilon, minInlierRatio ); 
		}
		catch ( NotEnoughDataPointsException e )
		{
			return e.toString();
		}
			
		final NumberFormat nf = NumberFormat.getPercentInstance();
		final float ratio = ((float)inliers.size() / (float)candidates.size());
		
		if ( modelFound && inliers.size() >= minNumCorrespondences )
		{			
			for ( final PointMatch pointMatch : inliers )
			{
				final PointMatchGeneric<LinkedPoint<T>> pm = (PointMatchGeneric<LinkedPoint<T>>) pointMatch;
				
				final T detectionA = pm.getPoint1().getLinkedObject();
				final T detectionB = pm.getPoint2().getLinkedObject();
				
				// we are working multithreaded here, so avoid collisions while adding correspondences to the ArrayLists
				synchronized ( detectionA ) { detectionA.addRANSACCorrespondence( detectionB ); }
				synchronized ( detectionB ) { detectionB.addRANSACCorrespondence( detectionA ); }
				
				inlierList.add( new PointMatchGeneric<T>( detectionA, detectionB ) );
			}

			return "Remaining inliers after RANSAC: " + inliers.size() + " of " + candidates.size() + " (" + nf.format(ratio) + ") with average error " + model.getCost();
		}
		else
		{
			if ( modelFound )					
				return "Model found but not enough remaining inliers (" + inliers.size() + "/" + minNumCorrespondences + ") after RANSAC of " + candidates.size();
			else
				return "NO Model found after RANSAC of " + candidates.size();
		}
	}

	protected static Quat4f getQuaternion( final RigidModel3D model )
	{
        final Matrix4f matrix = new Matrix4f();
        model.getTransform3D().get( matrix );	        	        
        final Quat4f quaternion = new Quat4f();	        
        quaternion.set( matrix );

        return quaternion;
	}
	
	public static double getApproximateRotationAngle( final RigidModel3D model )
	{
		final Quat4f qu = getQuaternion( model );
		return Math.toDegrees( Math.acos( qu.getW() ) * 2 );
	}

	public static Vector3f getApproximateAxis( final RigidModel3D model )
	{        
		final Quat4f qu = getQuaternion( model );
        final Vector3f n = new Vector3f(qu.getX(),qu.getY(), qu.getZ());
        n.normalize();
        
		return n;      		
	}

	public synchronized static <T extends Detection<T>> void addPointMatches( final ArrayList<PointMatchGeneric<T>> correspondences, final Tile<?> tileA, final Tile<?> tileB )
	{
		final ArrayList<PointMatch> pm = new ArrayList<PointMatch>();
		pm.addAll( correspondences );
	
		if ( correspondences.size() > 0 )
		{
			tileA.addMatches( pm );							
			tileB.addMatches( PointMatch.flip( pm ) );
			tileA.addConnectedTile( tileB );
			tileB.addConnectedTile( tileA );
		}
	}  
	
}
