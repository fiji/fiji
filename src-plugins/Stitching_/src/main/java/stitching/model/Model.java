/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package stitching.model;

import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.Collection;

/**
 * Abstract class for arbitrary transformation models to be applied
 * to {@link Point Points} in n-dimensional space.
 * 
 * Model: R^n --> R^n 
 * 
 * Provides methods for generic optimization and model extraction algorithms.
 * Currently, the Random Sample Consensus \citet{FischlerB81} and a robust
 * regression method are implemented.
 *  
 * TODO: A model is planned to be a generic transformation pipeline to be
 *   applied to images, volumes or arbitrary sets of n-dimensional points.
 *   E.g. lens transformation of camera images, pose and location of mosaic
 *   tiles, non-rigid bending of confocal stacks etc.
 *  
 * 
 * BibTeX:
 * <pre>
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 * 
 * @version 0.2b
 * 
 */
public abstract class Model implements CoordinateTransform
{
	
	// minimal number of point correspondences required to solve the model
	abstract public int getMinSetSize();
	
	// real random
	//final Random random = new Random( System.currentTimeMillis() );
	// repeatable results
	final static protected Random rnd = new Random( 69997 );

	
	/**
	 * The cost depends on what kind of algorithm is running.  It is always
	 * true that a smaller cost is better than large cost
	 */
	protected double cost = Double.MAX_VALUE;
	final public double getCost(){ return cost; }
	final public void setCost( double c ){ cost = c; }

	/**
	 * @deprecated The term Error may be missleading---use {@link #getCost()} instead
	 */
	@Deprecated
	final public double getError(){ return getCost(); }
	/**
	 * @deprecated The term Error may be missleading---use {@link #getCost()} instead
	 */
	@Deprecated
	final public void setError( double e ){ setCost( e ); }

	/**
	 * less than operater to make the models comparable, returns false for error < 0
	 * 
	 * @param m
	 * @return false for error < 0, otherwise true if this.error is smaller than m.error
	 */
	public boolean betterThan( Model m )
	{
		if ( cost < 0 ) return false;
		return cost < m.cost;
	}

	/**
	 * randomly change the model a bit
	 * 
	 * estimates the necessary amount of shaking for each single dimensional
	 * distance in the set of matches
	 *
	 * @param matches point matches
	 * @param scale gives a multiplicative factor to each dimensional distance (scales the amount of shaking)
	 * @param center local pivot point for centered shakes (e.g. rotation)
	 */
	abstract public void shake(
			Collection< PointMatch > matches,
			float scale,
			float[] center );

	
	/**
	 * Fit the {@link Model} to a set of data points minimizing the global
	 * transfer error.  This is assumed to be implemented as a least squares
	 * minimization.  Use
	 * {@link #ransac(Class, List, Collection, int, double, double) ransac}
	 * and/ or {@link #filter(Class, Collection, Collection)} to remove
	 * outliers from your data points
	 * 
	 * The estimated model transfers match.p2.local to match.p1.world.
	 * 
	 * @param matches set of point correpondences
	 * @throws an exception if matches does not contain enough data points
	 */
	abstract public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException;

	
	/**
	 * Test the {@link Model} for a set of point correspondence candidates.
	 * Return true if the number of inliers / number of candidates is larger
	 * than or equal to min_inlier_ratio, otherwise false.
	 * 
	 * Clears inliers and fills it with the fitting subset of candidates.
	 * 
	 * @param candidates set of point correspondence candidates
	 * @param inliers set of point correspondences that fit the model
	 * @param epsilon maximal allowed transfer error
	 * @param min_inliers minimal ratio of inliers (0.0 => 0%, 1.0 => 100%)
	 */
	final static public boolean test(
			Model model,
			Collection< PointMatch > candidates,
			Collection< PointMatch > inliers,
			double epsilon,
			double min_inlier_ratio )
	{
		inliers.clear();
		
		for ( PointMatch m : candidates )
		{
			m.apply( model );
			if ( m.getDistance() < epsilon ) inliers.add( m );
		}
		
		float ir = ( float )inliers.size() / ( float )candidates.size();
		model.cost = Math.max( 0.0, Math.min( 1.0, 1.0 - ir ) );
		
		return ( ir > min_inlier_ratio );
	}
	
	
	/**
	 * Estimate a {@link Model} and filter potential outliers by robust
	 * iterative regression.
	 * 
	 * This method performs well on data sets with low amount of outliers.  If
	 * you have many outliers, you can filter those with a `tolerant' RANSAC
	 * first as done in {@link #filterRansac() filterRansac}.
	 * 
	 * @param modelClass Class of the model to be estimated
	 * @param candidates Candidate data points eventually inluding some outliers
	 * @param inliers Remaining after the robust regression filter
	 * 
	 */
	final static public < M extends Model >M filter(
			Class< M > modelClass,
			Collection< PointMatch > candidates,
			Collection< PointMatch > inliers ) throws NotEnoughDataPointsException
	{
		M model;
		try
		{
			model = modelClass.newInstance();
		}
		catch ( Exception e )
		{
			System.err.println( e.getMessage() );
			return null;
		}
		
		inliers.clear();
		inliers.addAll( candidates );
		final ArrayList< PointMatch > temp = new ArrayList< PointMatch >();
		int num_inliers;
		do
		{
			temp.clear();
			temp.addAll( inliers );
			num_inliers = inliers.size(); 
			model.fit( inliers );
			ErrorStatistic observer = new ErrorStatistic();
			for ( PointMatch m : temp )
			{
				m.apply( model );
				observer.add( m.getDistance() );
			}
			inliers.clear();
			double t = observer.median * 4;
			for ( PointMatch m : temp )
			{
				if ( m.getDistance() < t )
					inliers.add( m );
			}
			//System.out.println( ( num_inliers - inliers.size() ) + " candidates with e > " + t + " removed by iterative robust regression." );
			//System.out.println( inliers.size() + " inliers remaining." );
			
			model.cost = observer.mean;
		}
		while ( num_inliers > inliers.size() );
		
		if ( num_inliers < model.getMinSetSize() )
			return null;
		return model;
	}
	
	
	/**
	 * Estimate a {@link Model} from a set with many outliers by first
	 * filtering the worst outliers with
	 * {@link #ransac(Class, List, Collection, int, double, double) ransac}
	 * \citet[{FischlerB81} tand filter potential outliers by robust iterative regression.
	 * 
	 * This method performs well on data sets with low amount of outliers.  If
	 * you have many outliers, you can filter those with a `tolerant' RANSAC
	 * first as done in {@link #filterRansac() filterRansac}.
	 * 
	 * @param modelClass Class of the model to be estimated
	 * @param candidates Candidate data points inluding (many) outliers
	 * @param inliers Remaining candidates after RANSAC
	 * @param iterations
	 * @param epsilon
	 * @param min_inlier_ratio minimal number of inliers to number of
	 *   candidates
	 * 
	 * @return an instance of 
	 */
	final static public < M extends Model >M ransac(
			Class< M > modelClass,
			List< PointMatch > candidates,
			Collection< PointMatch > inliers,
			int iterations,
			double epsilon,
			double min_inlier_ratio ) throws NotEnoughDataPointsException
	{
		M model;
		try
		{
			model = modelClass.newInstance();
		}
		catch ( Exception e )
		{
			System.err.println( e.getMessage() );
			return null;
		}
		
		final int MIN_SET_SIZE = model.getMinSetSize();
		
		inliers.clear();
		
		if ( candidates.size() < MIN_SET_SIZE )
		{
			throw new NotEnoughDataPointsException( candidates.size() + " correspondences are not enough to estimate a model, at least " + MIN_SET_SIZE + " correspondences required." );
		}
		
		int i = 0;
		final ArrayList< PointMatch > min_matches = new ArrayList< PointMatch >();
		
		while ( i < iterations )
		{
			// choose model.MIN_SET_SIZE disjunctive matches randomly
			min_matches.clear();
			for ( int j = 0; j < MIN_SET_SIZE; ++j )
			{
				PointMatch p;
				do
				{
					p = candidates.get( ( int )( rnd.nextDouble() * candidates.size() ) );
				}
				while ( min_matches.contains( p ) );
				min_matches.add( p );
			}
			M m;
			try
			{
				m = modelClass.newInstance();
			}
			catch ( Exception e )
			{
				System.err.println( e.getMessage() );
				return null;
			}
			final ArrayList< PointMatch > temp_inliers = new ArrayList< PointMatch >();
			m.fit( min_matches );
			int num_inliers = 0;
			boolean is_good = test( m, candidates, temp_inliers, epsilon, min_inlier_ratio );
			while ( is_good && num_inliers < temp_inliers.size() )
			{
				num_inliers = temp_inliers.size();
				m.fit( temp_inliers );
				is_good = test( m, candidates, temp_inliers, epsilon, min_inlier_ratio );
			}
			if (
					is_good &&
					m.betterThan( model ) &&
					temp_inliers.size() >= 3 * MIN_SET_SIZE )
			{
				model = m;
				inliers.clear();
				inliers.addAll( temp_inliers );
			}
			++i;
		}
		if ( inliers.size() == 0 )
			return null;
		return model;
	}
	
	
	/**
	 * Estimate the best model for a set of feature correspondence candidates.
	 * 
	 * Increase the error as long as not more inliers apear.
	 * 
	 * TODO: This is a very crappy way of identifying the minimal max_epsilon
	 *   and maximal number of inliers at the same time.  We suggest a more
	 *   elaborate technique as follows:
	 *   1. Run RANSAC with large max_epsilon resulting in many outliers
	 *      included.
	 *   2. Get rid of the outliers using the robust regression method of
	 *      Dutter and Huber
	 */
	static public  < M extends Model >M filterRansac(
			Class< M > modelType,
			List< PointMatch > candidates,
			Collection< PointMatch > inliers,
			int iterations,
			float max_epsilon,
			float min_inlier_ratio ) throws NotEnoughDataPointsException
	{
		final ArrayList< PointMatch > temp = new ArrayList< PointMatch >();
		ransac(
				modelType,
				candidates,
				temp,
				iterations,
				max_epsilon,
				min_inlier_ratio );		
		return filter( modelType, temp, inliers );
	}
	
	/**
	 * Create a meaningful string representation of the model for save into
	 * text-files or display on terminals.
	 */
	abstract public String toString();

	
	/**
	 * Clone the model.
	 */
	abstract public Model clone();
};
