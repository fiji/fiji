package mpicbg.icp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.util.KDTree;
import fiji.util.node.Leaf;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;

/**
 * Implementation of the ICP 
 * 
 * @author Stephan Preibisch
 *
 * @param <P>
 */
public class ICP < P extends Point & Leaf<P> >
{
	final List< P > reference, target;
	
	List<PointMatch> pointMatches;
	ArrayList<PointMatch> ambigousMatches;
	PointMatchIdentification< P > pointMatchIdentifier;	
	
	float avgError, maxError;
	int numMatches;
	
	/**
	 * Instantiates a new {@link ICP} object with the {@link List} of target and reference points as well as the {@link PointMatchIdentification} interface that defines
	 * how corresponding points are identified. <br>
	 * Note that the elements of the {@link List}s have to implement {@link Point}(for compatibility with {@link Model}) and {@link Leaf}(for compatibility with {@link KDTree}). 
	 * 
	 * @param target - the {@link List} of target points
	 * @param reference - the {@link List} of reference points
	 * @param pointMatchIdentifier - the {@link PointMatchIdentification} which defines how correspondences are established
	 */
	public ICP( final List<P> target, final List<P> reference, final PointMatchIdentification<P> pointMatchIdentifier )
	{
		this.reference = reference;
		this.target = target;
		this.ambigousMatches = null;
		this.pointMatches = null;
		
		this.pointMatchIdentifier = pointMatchIdentifier;
		
		this.avgError = -1;
		this.maxError = -1;
		this.numMatches = -1;
	}

	/**
	 * Also instantiates a new {@link ICP} instance, but uses the {@link SimplePointMatchIdentification} to define corresponding points.
	 * 
	 * @param target - the {@link List} of target points
	 * @param reference - the {@link List} of reference points
	 * @param distanceThreshold - the maximal distance of {@link SimplePointMatchIdentification}, so that the nearest neighbor of a point is still counted as a corresponding point
	 */
	public ICP( final List<P> target, final List<P> reference, final float distanceThreshold )
	{
		this( target, reference, new SimplePointMatchIdentification<P>( distanceThreshold ) );
	}

	/**
	 * Also instantiates a new {@link ICP} instance, but uses the {@link SimplePointMatchIdentification} to define corresponding points.
	 * 
	 * @param target - the {@link List} of target points
	 * @param reference - the {@link List} of reference points
	 */
	public ICP( final List<P> target, final List<P> reference )
	{
		this( target, reference, new SimplePointMatchIdentification<P>() );
	}

	/**
	 * Performs one iteration of the {@link ICP}. It takes the last {@link Model} as input to find the corresponding points for the new {@link Model}. 
	 * The result is the new Model, the number of corresponding points, the average, and the maximal error. Note that lastModel and newModel can be the 
	 * same instance and it will be overwritten. 
	 * 
	 * @param lastModel - The last {@link Model} that maps the target.local coordinates to the reference.world coordinates, used to find the corresponding points 
	 * @param newModel - The {@link Model} that maps the target.local coordinates to the reference.world coordinates, will be fitted to the new points
	 * @throws NotEnoughDataPointsException
	 * @throws IllDefinedDataPointsException
	 */
	public void runICPIteration( final Model<?> lastModel, final Model<?> newModel ) throws NotEnoughDataPointsException, IllDefinedDataPointsException, NoSuitablePointsException
	{
		/* apply initial model of the target (from last iteration) */
		for ( final P point : target )
			point.apply( lastModel );
		
		/* get corresponding points for ICP */
		final List<PointMatch> matches = pointMatchIdentifier.assignPointMatches( target, reference );
		
		/* remove ambigous correspondences */
		ambigousMatches = removeAmbigousMatches( matches );

		/* fit the model */
		newModel.fit( matches );

		/* apply the new model of the target to determine the error */
		for ( final P point : target )
			point.apply( newModel );

		/* compute the output */
		avgError = PointMatch.meanDistance( matches );
		maxError = PointMatch.maxDistance( matches );
		numMatches = matches.size();
		pointMatches = matches;
	}
		
	/**
	 * Estimates an initial {@link Model} based on some given {@link PointMatch}es. Note that the {@link PointMatch}es have to be stored as PointMatch(target,reference). 
	 * 
	 * @param matches - The {@link List} of apriori known {@link PointMatch}es
	 * @param model - The {@link Model} to use
	 * @throws NotEnoughDataPointsException
	 * @throws IllDefinedDataPointsException
	 */
	public void estimateIntialModel( final List<PointMatch> matches, final Model<?> model ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		/* remove ambigous correspondences */
		ambigousMatches = removeAmbigousMatches( matches );
		
		/* fit the model */
		model.fit( matches );

		/* apply the new model of the target to determine the error */
		for ( final P point : target )
			point.apply( model );

		/* compute the output */
		avgError = PointMatch.meanDistance( matches );
		maxError = PointMatch.maxDistance( matches );
		numMatches = matches.size();
		pointMatches = matches;		
	}
	
	/**
	 * Sets the {@link PointMatchIdentification} that defines how {@link PointMatch}es between reference and target are identified.
	 * The simplest way to do it is the {@link SimplePointMatchIdentification} class which takes the nearest neighbor with a minimal distance threshold. 
	 *  
	 * @param assignMethod - the new {@link PointMatchIdentification}
	 */
	public void setPointMatchIdentification( final PointMatchIdentification< P > pointMatchIdentifier ) { this.pointMatchIdentifier = pointMatchIdentifier; }
	
	/**
	 * Returns the current {@link PointMatchIdentification} that is used to identfy corresponding points
	 * @return PointMatchIdentification< P >
	 */
	public PointMatchIdentification< P > getPointMatchIdentification() { return pointMatchIdentifier; }
	
	/**
	 * Return the {@link List} of {@link PointMatch}es (target, reference) of the last {@link ICP} iteration
	 * @return - {@link List} of {@link PointMatch}es
	 */
	public List<PointMatch> getPointMatches() { return pointMatches; }
	
	/**
	 * Returns the average error of the last ICP iteration, or -1 if no iteration has been computed yet.
	 * @return float - average error
	 */
	public float getAverageError() { return avgError; }

	/**
	 * Returns the maximum error of a {@link PointMatch} of the last ICP iteration, or -1 if no iteration has been computed yet.
	 * @return float - maximal error
	 */
	public float getMaximalError() { return maxError; }
	
	/**
	 * Returns the number of {@link PointMatch}es of the last ICP iteration, or -1 if no iteration has been computed yet.
	 * @return int - number of {@link PointMatch}es
	 */
	public int getNumPointMatches() { return numMatches; }
	
	/**
	 * Returns the number of ambigous {@link PointMatch}es indentified in the last ICP iteration, or -1 if no iteration has been computed yet.
	 * @return int - number of ambigous {@link PointMatch}es
	 */
	public int getNumAmbigousMatches()
	{
		if ( ambigousMatches == null )
			return -1;
		else
			return ambigousMatches.size();
	}
	
	/**
	 * Returns the {@link ArrayList} of ambigous {@link PointMatch}es indentified in the last ICP iteration, or null if no iteration has been computed yet.
	 * @return int - {@link ArrayList} of {@link PointMatch}es
	 */
	public ArrayList<PointMatch> getAmbigousMatches() { return this.ambigousMatches; }

	public List<P> getTargetPoints() { return target; }
	public List<P> getReferencePoints() { return reference; }
	
	/**
	 * Detects ambigous (and duplicate) {@link PointMatch}es, i.e. if a {@link Point} corresponds with more than one other {@link Point}
	 * @param matches - the {@link List} of {@link PointMatch}es
	 * @return - the {@link ArrayList} containing the removed ambigous or duplicate {@link PointMatch}es 
	 */
	public static ArrayList<PointMatch> removeAmbigousMatches( final List<PointMatch> matches )
	{
		final ArrayList<Integer> inconsistentCorrespondences = new ArrayList<Integer>();
		final ArrayList<PointMatch> ambigousMatches = new ArrayList<PointMatch>();
		
		for ( int i = 0; i < matches.size(); i++ )
		{
			final Point pointTarget = matches.get( i ).getP1();
			final Point pointReference = matches.get( i ).getP2();

			final ArrayList<Integer> inconsistent = getOccurences( pointTarget, pointReference, matches );
			
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
				// save the ambigous match
				final PointMatch pm = matches.get( (int)inconsistentCorrespondences.get(i) );				
				ambigousMatches.add( pm );
				
				// the cast to (int) is essential as otherwise he is looking to remove the Integer object that does not exist in the list 
				matches.remove( (int)inconsistentCorrespondences.get(i) );
			}
		}	
		
		return ambigousMatches;
	}
	
	/**
	 * Computes if one {@link Point} pair occurs more than once in a {@link List} of {@link PointMatch}es 
	 * 
	 * @param pointTarget - one {@link Point}
	 * @param pointReference - the other {@link Point}
	 * @param list - the {@link List} of {@link PointMatch}es (target, reference)
	 * @return - an {@link ArrayList} of indices which should be removed due to duplicate or ambigous occurence
	 */
	protected static ArrayList<Integer> getOccurences( final Point pointTarget, final Point pointReference, List<PointMatch> list )
	{
		final ArrayList<Integer> occurences = new ArrayList<Integer>();
		
		boolean differentOccurence = false;
				
		/* Test if pointReference has matches with different points than pointTarget */
		for ( final PointMatch pm : list )
		{			
			if ( pm.getP2() == pointReference )
			{
				// it is NOT twice the correct occurence
				if ( pm.getP1() != pointTarget )
				{
					differentOccurence = true;
					break;
				}
			}

			if ( pm.getP1() == pointTarget )
			{
				// it is NOT twice the correct occurence
				if ( pm.getP2() != pointReference )
				{
					differentOccurence = true;
					break;
				}				
			}
		}
		
		if ( differentOccurence )
		{
			/* remove all occurences/matches with pointReference as it is ambigous */
			for ( int i = 0; i < list.size(); i++ )
			{
				final PointMatch pm = list.get( i );
				
				if ( pm.getP2() == pointReference )
					occurences.add( i );				
				
				if ( pm.getP1() == pointTarget )
					occurences.add( i );
			}
		}
		else
		{
			/* remove double occurences */
			boolean sameOccurence = false;

			for ( int i = 0; i < list.size(); i++ )
			{
				final PointMatch pm = list.get( i );
				
				/* remove all but the first occurence/match */
				if ( pm.getP2() == pointReference )
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
	
}
