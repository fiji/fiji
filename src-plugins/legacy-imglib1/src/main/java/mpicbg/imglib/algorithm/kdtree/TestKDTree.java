/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.kdtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import mpicbg.imglib.algorithm.kdtree.node.SimpleNode;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class TestKDTree
{
	protected static boolean testNNearestNeighbor(final int neighbors, final int numDimensions, final int numPoints, final int numTests, final float min, final float max) {
		final ArrayList<SimpleNode> points = new ArrayList<SimpleNode>();
		final Random rnd = new Random(435435435);

		final float[] p = new float[numDimensions];

		for (int i = 0; i < numPoints; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (max - min) + min;

			final SimpleNode t = new SimpleNode(p);
			points.add(t);
		}

		long start = System.currentTimeMillis();
		final KDTree<SimpleNode> kdTree = new KDTree<SimpleNode>(points);
		final NNearestNeighborSearch<SimpleNode> kd = new NNearestNeighborSearch<SimpleNode>(kdTree);
		final long kdSetupTime = System.currentTimeMillis() - start;		System.out.println("kdtree setup took: " + kdSetupTime + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;

			final SimpleNode t = new SimpleNode(p);
			final SimpleNode[] nnKdtree = kd.findNNearestNeighbors(t, neighbors);
			final SimpleNode[] nnExhaustive = findNNearestNeighborExhaustive(points, t, neighbors);

			for (int j = 0; j < neighbors; ++j) {
				if (!nnKdtree[j].equals(nnExhaustive[j])) {
					System.out.println((j+1) + " - Nearest neighbor to: " + t);
					System.out.println("KD-Tree says: " + nnKdtree[j] + " (" + nnKdtree[j].distanceTo(t) + ")");
					System.out.println("Exhaustive says: " + nnExhaustive[j] + " (" + nnExhaustive[j].distanceTo(t) + ")");
					return false;
				}
			}
		}
		final long compareTime = System.currentTimeMillis() - start;
		System.out.println("comparison (kd-exhaustive) search took: " + (compareTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
		}
		final long initTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			final SimpleNode[] nnKdtree = kd.findNNearestNeighbors(t, neighbors);
			nnKdtree.clone();
		}
		final long kdTime = System.currentTimeMillis() - start;
		System.out.println("kdtree search took: " + (kdTime-initTime) + " ms.");
		System.out.println("kdtree all together took: " + (kdSetupTime+kdTime-initTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			final SimpleNode[] nnExhaustive = findNNearestNeighborExhaustive(points, t, neighbors);
			nnExhaustive.clone();
		}
		final long exhaustiveTime = System.currentTimeMillis() - start;
		System.out.println("exhaustive search took: " + (exhaustiveTime-initTime) + " ms.");

		return true;
	}
	
	protected static boolean testRadiusSearch( final int numDimensions, final int numPoints, final int numTests, final float min, final float max  )
	{
		final ArrayList<SimpleNode> points = new ArrayList<SimpleNode>();
		final Random rnd = new Random(435435435);

		final float[] p = new float[numDimensions];

		for (int i = 0; i < numPoints; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (max - min) + min;

			final SimpleNode t = new SimpleNode(p);
			points.add(t);
		}

		long start = System.currentTimeMillis();
		final KDTree<SimpleNode> kdTree = new KDTree<SimpleNode>( points );
		final RadiusNeighborSearch<SimpleNode> kd = new RadiusNeighborSearch<SimpleNode>( kdTree );
		final long kdSetupTime = System.currentTimeMillis() - start;
		System.out.println("kdtree setup took: " + (kdSetupTime) + " ms.");

		start = System.currentTimeMillis();
		
		for (int i = 0; i < numTests; ++i) 
		{
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;

			double radius = rnd.nextDouble() * (max-min) / 10;
			
			final SimpleNode t = new SimpleNode(p);

			final ArrayList<SimpleNode> radiusKdtree = kd.findNeighborsSorted( t, radius );
			final ArrayList<SimpleNode> radiusExhaustive = findNeighborsRadiusExhaustive( points, t, radius );

			if ( radiusKdtree.size() != radiusExhaustive.size() )
			{
				
				System.out.println("Not same number of points within radius(" + radius + ") of " + t + " found, kdTree = " + radiusKdtree.size() + ", exhaustive = " + radiusExhaustive.size() );
				
				System.out.println("KDTree:");				
				for ( final SimpleNode n : radiusKdtree  )
					System.out.println( n + ", distance: " + n.distanceTo( t ) );

				System.out.println("Exhaustive:");				
				for ( final SimpleNode n : radiusExhaustive )
					System.out.println( n + ", distance: " + n.distanceTo( t ) );
				
				return false;
			}
			else
			{
				boolean success = true;
				for ( int j = 0; j < radiusKdtree.size(); ++j )
				{
					if ( radiusKdtree.get( j ) != radiusExhaustive.get( j ) && radiusKdtree.get( j ) .distanceTo( t ) != radiusExhaustive.get( j ).distanceTo( t ) )
					{
						System.out.println( "Point " + j + " disagrees within radius(" + radius + ") of " + t + " found, \n\tkdTree = " + radiusKdtree.get( j )  + ", distance: " + radiusKdtree.get( j ) .distanceTo( t ) + ", \n\texhaustive = " + radiusExhaustive.get( j ) + ", distance: " + radiusExhaustive.get( j ).distanceTo( t ) );
						success = false;
					}					
				}
				
				if ( !success )
					return false;
			}			
		}
		
		final long compareTime = System.currentTimeMillis() - start;		
		System.out.println("comparison (kdtree <-> exhaustive) successfull, took: " + (compareTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			t.getClass();
		}
		final long initTime = System.currentTimeMillis() - start;

		double radius = (max-min)/20;
		
		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (max - min) + min;
			final SimpleNode t = new SimpleNode(p);
			final ArrayList<SimpleNode> nnKdtree = kd.findNeighborsSorted( t, radius );
			nnKdtree.getClass();
		}
		final long kdTime = System.currentTimeMillis() - start;
		System.out.println("kdtree search took: " + (kdTime-initTime) + " ms.");
		System.out.println("kdtree all together took: " + (kdSetupTime+kdTime-initTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (max - min) + min;
			final SimpleNode t = new SimpleNode(p);
			final ArrayList<SimpleNode> nnExhaustive = findNeighborsRadiusExhaustive( points, t, radius );
			nnExhaustive.getClass();
		}
		final long exhaustiveTime = System.currentTimeMillis() - start;
		System.out.println("exhaustive search took: " + (exhaustiveTime-initTime) + " ms.");
		
		return true;
	}

	protected static boolean testNearestNeighbor( final int numDimensions, final int numPoints, final int numTests, final float min, final float max ) 
	{
		final ArrayList<SimpleNode> points = new ArrayList<SimpleNode>();
		final Random rnd = new Random(435435435);

		final float[] p = new float[numDimensions];

		for (int i = 0; i < numPoints; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (max - min) + min;

			final SimpleNode t = new SimpleNode(p);
			points.add(t);
		}

		long start = System.currentTimeMillis();
		final KDTree<SimpleNode> kdTree = new KDTree<SimpleNode>(points);
		final NearestNeighborSearch<SimpleNode> kd = new NearestNeighborSearch<SimpleNode>(kdTree);
		final long kdSetupTime = System.currentTimeMillis() - start;
		System.out.println("kdtree setup took: " + (kdSetupTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;

			final SimpleNode t = new SimpleNode(p);

			final SimpleNode nnKdtree = kd.findNearestNeighbor(t);
			final SimpleNode nnExhaustive = findNearestNeighborExhaustive(points, t);

			if (!nnKdtree.equals(nnExhaustive)) {
				System.out.println("Nearest neighbor to: " + t);
				System.out.println("KD-Tree says: " + nnKdtree);
				System.out.println("Exhaustive says: " + nnExhaustive);
				return false;
			}
		}
		final long compareTime = System.currentTimeMillis() - start;
		System.out.println("comparison (kdtree <-> exhaustive) search took: " + (compareTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			t.getClass();
		}
		final long initTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			final SimpleNode nnKdtree = kd.findNearestNeighbor(t);
			nnKdtree.getClass();
		}
		final long kdTime = System.currentTimeMillis() - start;
		System.out.println("kdtree search took: " + (kdTime-initTime) + " ms.");
		System.out.println("kdtree all together took: " + (kdSetupTime+kdTime-initTime) + " ms.");

		start = System.currentTimeMillis();
		for (int i = 0; i < numTests; ++i) {
			for (int d = 0; d < numDimensions; ++d)
				p[d] = rnd.nextFloat() * (2*max - 2*min) + 2*min;
			final SimpleNode t = new SimpleNode(p);
			final SimpleNode nnExhaustive = findNearestNeighborExhaustive(points, t);
			nnExhaustive.getClass();
		}
		final long exhaustiveTime = System.currentTimeMillis() - start;
		System.out.println("exhaustive search took: " + (exhaustiveTime-initTime) + " ms.");

		return true;
	}

	private static SimpleNode findNearestNeighborExhaustive(final ArrayList<SimpleNode> points, final SimpleNode t) {
		float minDistance = Float.MAX_VALUE;
		SimpleNode nearest = null;

		for (final SimpleNode n : points) {
			final float dist = n.distanceTo(t);
			if (dist < minDistance) {
				minDistance = dist;
				nearest = n;
			}
		}

		return new SimpleNode(nearest);
	}

	private static ArrayList<SimpleNode> findNeighborsRadiusExhaustive(final ArrayList<SimpleNode> points, final SimpleNode t, final double radius) 		
	{
		final ArrayList<SimpleNode> withinRadius = new ArrayList<SimpleNode>(); 
		
		for (final SimpleNode n : points) 
		{
			final float dist = n.distanceTo(t);
			if (dist <= radius) 
				withinRadius.add( n );
		}
		
		// now sort
		Collections.sort( withinRadius, new DistanceComparator<SimpleNode>( t ) );

		return withinRadius;
	}
	
	private static SimpleNode[] findNNearestNeighborExhaustive(final ArrayList<SimpleNode> points, final SimpleNode t, final int n) {
		final SimpleNode[] nearest = new SimpleNode[n];
		final float[] minDistance = new float[n];

		for (int i = 0; i < n; ++i)
			minDistance[i] = Float.MAX_VALUE;

A:		for (final SimpleNode node : points) {
			final float dist = node.distanceTo(t);

			for (int i = 0; i < n; ++i) {
				if (dist < minDistance[i]) {
					for (int j = n-2; j >= i; --j) {
						nearest[j + 1] = nearest[j];
						minDistance[j +1] = minDistance[j];
					}

					nearest[i] = node;
					minDistance[i] = dist;

					continue A;
				}
			}
		}

		return nearest;
	}

	public static void main(String[] args) 
	{
		if ( testRadiusSearch( 3, 100000, 1000, -100, 100 ) )
			System.out.println("Radius neighbor test (3) successfull\n");

		if (testNNearestNeighbor(3, 3, 100000, 1000, -5, 5))
			System.out.println("N-Nearest neighbor test (3) successfull\n");

		if (testNearestNeighbor(3, 100000, 1000, -5, 5))
			System.out.println("Nearest neighbor test successfull\n");
	}
}
