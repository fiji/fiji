/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package fiji.plugin.trackmate.tracking.kdtree;

import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.collection.KDTree;
import net.imglib2.collection.KDTreeNode;
import net.imglib2.neighborsearch.NearestNeighborSearch;

public class NearestNeighborFlagSearchOnKDTree<T> implements NearestNeighborSearch< FlagNode<T> > {
	
	protected KDTree< FlagNode<T>  > tree;
	
	protected final int n;
	protected final double[] pos;

	protected KDTreeNode< FlagNode<T> > bestPoint;
	protected double bestSquDistance;
	
	public NearestNeighborFlagSearchOnKDTree( KDTree< FlagNode<T>  > tree ) {
		n = tree.numDimensions();
		pos = new double[ n ];
		this.tree = tree;
	}
	
	@Override
	public int numDimensions()	{
		return n;
	}
	
	@Override
	public void search( RealLocalizable p ) {
		p.localize( pos );
		bestSquDistance = Double.MAX_VALUE;
		searchNode( tree.getRoot() );
	}
	
	protected void searchNode( KDTreeNode< FlagNode<T>  > current )	{
		// consider the current node
		final double distance = current.squDistanceTo( pos );
		boolean visited = current.get().isVisited();
		if ( distance < bestSquDistance && !visited  ) {
			bestSquDistance = distance;
			bestPoint = current;
		}
		
		final double axisDiff = pos[ current.getSplitDimension() ] - current.getSplitCoordinate();
		final double axisSquDistance = axisDiff * axisDiff;
		final boolean leftIsNearBranch = axisDiff < 0;

		// search the near branch
		final KDTreeNode< FlagNode<T> > nearChild = leftIsNearBranch ? current.left : current.right;
		final KDTreeNode< FlagNode<T> > awayChild = leftIsNearBranch ? current.right : current.left;
		if ( nearChild != null )
			searchNode( nearChild );

	    // search the away branch - maybe
		if ( ( axisSquDistance <= bestSquDistance ) && ( awayChild != null ) )
			searchNode( awayChild );
	}

	@Override
	public Sampler<fiji.plugin.trackmate.tracking.kdtree.FlagNode<T>>  getSampler() {
		return bestPoint;
	}

	@Override
	public RealLocalizable getPosition() {
		return bestPoint;
	}

	@Override
	public double getSquareDistance() {
		return bestSquDistance;
	}

	@Override
	public double getDistance() {
		return Math.sqrt( bestSquDistance );
	}
	
	@Override
	public NearestNeighborFlagSearchOnKDTree<T> copy() {
		final NearestNeighborFlagSearchOnKDTree<T> copy = new NearestNeighborFlagSearchOnKDTree<T>( tree );
		System.arraycopy( pos, 0, copy.pos, 0, pos.length );
		copy.bestPoint = bestPoint;
		copy.bestSquDistance = bestSquDistance;
		return copy;
	}
	
}
