package mpicbg.pointdescriptor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import mpicbg.imglib.util.Util;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.fit.FitResult;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;

public abstract class AbstractPointDescriptor< P extends Point, F extends AbstractPointDescriptor<P, F> >
{
	/* The basis point for the point descriptor */
	final P basisPoint;
	
	/* The links to the point instances, they do not need to be duplicated */
	final ArrayList< P > neighbors;
	
	/* Point instances storing the relative coordinates to the basis point */
	final ArrayList< LinkedPoint< P > > descriptorPoints;
	
	/* Computes the similarity between two aligned AbstracPointDescriptors */
	SimilarityMeasure similarityMeasure;
	
	/* Creates different sets of PointMatches which are tested and computes a normalization factor for the score */
	final Matcher matcher;
	
	/* The best match combination from the last comparison */
	ArrayList<PointMatch> bestPointMatchSet = null;

	final int numDimensions;
	final long id;
	
	/* Used to assign a unique id to each PointDescriptor, if two points have the same distance we sort by id */
	protected final static AtomicLong index = new AtomicLong( 0 );

	public AbstractPointDescriptor( final P basisPoint, final ArrayList< P > orderedNearestNeighboringPoints, 
			final SimilarityMeasure similarityMeasure, final Matcher matcher ) throws NoSuitablePointsException
	{
		this.basisPoint = basisPoint;
		this.numDimensions = basisPoint.getL().length;
		this.similarityMeasure = similarityMeasure;
		this.matcher = matcher;
		this.id = index.getAndIncrement();

		this.neighbors = orderedNearestNeighboringPoints;
		
		for ( final P point : orderedNearestNeighboringPoints )
			if ( point.getL().length != numDimensions )
				throw new NoSuitablePointsException( "At least one of the Points<P> given as ArrayList< P > orderedNearestNeighboringPoints have a different dimensionality than the basis point." );

		/* Set up the Descriptor with relative distances */
		this.descriptorPoints = new ArrayList< LinkedPoint< P > >( neighbors.size() );		
		final float[] basis;
		
		if ( useWorldCoordinatesForDescriptorBuildUp() )
			basis = basisPoint.getW().clone();
		else
			basis = basisPoint.getL().clone();
		
		for ( final P absolute : orderedNearestNeighboringPoints )
		{
			final float[] localCoordinates;
			
			if ( useWorldCoordinatesForDescriptorBuildUp() )
				localCoordinates = absolute.getW().clone();
			else
				localCoordinates = absolute.getL().clone();
			
			for ( int d = 0; d < numDimensions; ++d )
				localCoordinates[ d ] -= basis[ d ]; 
			
			descriptorPoints.add( new LinkedPoint< P >( localCoordinates, absolute ) );
		}
	}
	
	/**
	 * Matches two {@link AbstractPointDescriptor}s of the same kind yielding a similarity value, the lower the better (0 means identical)
	 * @param pointDescriptor - the {@link AbstractPointDescriptor} to match 
	 * @return similarity
	 */
	public double descriptorDistance( final F pointDescriptor )
	{
		/* init the point matches */
		final ArrayList<ArrayList<PointMatch>> matchesList = matcher.createCandidates( this, pointDescriptor );
		
		double bestSimilarity = Double.MAX_VALUE;
		bestPointMatchSet = null;
		
		for ( final ArrayList<PointMatch> matches : matchesList )
		{
			/* fit the model and apply to local point descriptor */
			final FitResult fitResult = fitMatches( matches );
			
			/* compute the similarity */							
			final double similarity = similarityMeasure.getSimilarity( matches ) * matcher.getNormalizationFactor( matches, fitResult );
			
			if ( similarity < bestSimilarity )
			{
				bestSimilarity = similarity;
				bestPointMatchSet = matches;
			}
		}
		
		/* Reset the world coordinates of this descriptor */
		if ( resetWorldCoordinatesAfterMatching() )
			resetWorldCoordinates();

		return bestSimilarity;
	}
	
	/**
	 * The combination of descriptorpoints that yielded the best similarity in the last comparison
	 * 
	 * @return - List of {@link PointMatch}es containing the original datasets
	 */
	public ArrayList<PointMatch> getBestPointMatchSet() { return bestPointMatchSet;	}
	
	/**
	 * Resets the world coordinates of the descriptorPoints
	 */
	protected void resetWorldCoordinates()
	{
		for ( final Point point : descriptorPoints )
		{
			final float[] l = point.getL();
			final float[] w = point.getW();
			
			for ( int d = 0; d < l.length; ++d )
				w[ d ] = l[ d ];
		}
	}
	
	/**
	 * Computes a fit between this these {@link PointMatch}es, this method is called by the {@link Matcher}
	 * 
	 * @param ArrayList<PointMatch> - The {@link Point}s to match
	 *
	public abstract CoordinateTransform fit( final ArrayList<PointMatch> pointMatch );
	*/	
	public abstract FitResult fitMatches( final ArrayList<PointMatch> matches );
	
	/**
	 * Tells if the descriptormatching should reset the world coordinates after the matching
	 * @return
	 */
	public abstract boolean resetWorldCoordinatesAfterMatching();

	/**
	 * Tells if the build up of the descriptor should use the world coordinates or rather the local coordinates
	 * @return - true (world), false(local)
	 */
	public abstract boolean useWorldCoordinatesForDescriptorBuildUp();

	/**
	 * @return The {@link SimilarityMeasure} object that is used to compute how similar two descriptors are
	 */
	public SimilarityMeasure getSimilarityMeasure() { return similarityMeasure; }

	/**
	 * Sets the {@link SimilarityMeasure} object that is used to compute how similar two descriptors are
	 * @param similarityMeasure
	 */
	public void setSimilarityMeasure( final SimilarityMeasure similarityMeasure) { this.similarityMeasure = similarityMeasure; }
	
	/**
	 * Return the unique id of this descriptor
	 * @return long id
	 */
	public long getId() { return id; }
	
	/**
	 * The basis point for this descriptor
	 * @return the {@link Point} instance
	 */
	public P getBasisPoint() { return basisPoint; }
	
	/**
	 * Returns a certain nearest neighbor (relative to the basis point) of this {@link AbstractPointDescriptor}
	 *  
	 * @param index - the index (0 means first nearest neighbor)
	 * @return the {@link Point} instance
	 */
	public Point getDescriptorPoint( final int index ) { return descriptorPoints.get( index ); }
	
	/**
	 * The points forming the {@link AbstractPointDescriptor} relative to the basis point
	 * @return - Arraylist< P >
	 */
  	public ArrayList< P > getOrderedNearestNeighboringPoints(){ return neighbors; }

  	/**
  	 * The number of neighbors used for this descriptor
  	 * @return - int number
  	 */
  	public int numNeighbors() { return neighbors.size(); }
  	
  	/**
  	 * Dimensionality of the {@link Point}s of this {@link AbstractPointDescriptor}
  	 * @return - int dimensions
  	 */
  	public int numDimensions() { return numDimensions; }
  	
  	/**
  	 * Computes the difference between two PointDescriptors by summing up all individual differences.
  	 * 
  	 * @param genericPointDescriptor - the PointDescriptor to compare to 
  	 * @return The differences between two PointDescriptors 
  	 *
  	public double getDifference( final AbstractPointDescriptor<P,?> genericPointDescriptor )
  	{
  		double difference = 0;
  		
  		for ( int d = 0; d < numDimensions; ++d )
  			difference += Point.distance( getNeighborPoint( d ), genericPointDescriptor.getNeighborPoint( d ) );
  		
  		return difference;
  	}

  	/**
  	 * Computes the squared difference between two PointDescriptors by summing up all individual squared differences.
  	 * 
  	 * @param genericPointDescriptor - the PointDescriptor to compare to 
  	 * @return The differences between two PointDescriptors 
  	 *
  	public double getSquaredDifference( final AbstractPointDescriptor<P,?> genericPointDescriptor )
  	{
  		double difference = 0;
  		
  		for ( int d = 0; d < numDimensions; ++d )
  			difference += Point.squareDistance( getNeighborPoint( d ), genericPointDescriptor.getNeighborPoint( d ) );
  		
  		return difference;
  	} 	  	
  	
  	/**
  	 * Overwrites the toString method
  	 */
  	public String toString()
  	{
  		String out = this.getClass().getName() + "[" +id + "] has position: " + Util.printCoordinates( basisPoint.getW() );
  		
  		for (int i = 0; i < neighbors.size(); i++)
  			out += "\nneighbor " + (i+1) + " vector: " + Util.printCoordinates( basisPoint.getW() );
  		
  		return out;
  	}
  	
}
