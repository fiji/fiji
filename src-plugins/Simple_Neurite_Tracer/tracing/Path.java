/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.awt.Color;
import java.awt.Graphics;

import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;

import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.measure.Calibration;

import pal.math.*;

import stacks.ThreePanes;

import ij3d.Image3DUniverse;
import ij3d.Content;
import ij3d.Pipe;
import ij3d.MeshMaker;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import javax.swing.tree.DefaultMutableTreeNode;

import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

/* This class represents a list of points, and has methods for drawing
 * them onto ThreePanes-style image canvases. */

public class Path implements Comparable {

	public int compareTo(Object o) {
		Path casted = (Path)o;
		if( id == casted.id )
			return 0;
		if( id < casted.id )
			return -1;
		else
			return 1;
	}

	/* The path's ID should be assigned by the PathAndFillManager
	   when it's added: */
	private int id = -1;
	public int getID() {
		return id;
	}
	void setID( int id ) {
		this.id = id;
	}

	static final boolean verbose = SimpleNeuriteTracer.verbose;

	boolean selected;

	Path startJoins;
	PointInImage startJoinsPoint = null;

	Path endJoins;
	PointInImage endJoinsPoint = null;

	public Path getStartJoins() {
		return startJoins;
	}

	public PointInImage getStartJoinsPoint() {
		return startJoinsPoint;
	}

	public Path getEndJoins() {
		return endJoins;
	}

	public PointInImage getEndJoinsPoint() {
		return endJoinsPoint;
	}

	public static final int PATH_START = 0;
	public static final int PATH_END = 1;

	// Paths should always be given a name (since the name
	// identifies them to the 3D viewer)...
	String name;

	public void setName(String newName) {
		this.name = newName;
	}

	public void setDefaultName() {
		this.name = "Path "+id;
	}

	public String getName() {
		if( name == null )
			throw new RuntimeException("In Path.getName() for id "+id+", name was null");
		return name;
	}

	/* This is a symmetrical relationship, showing all the other
	   paths this one is joined to... */
	ArrayList<Path> somehowJoins;

	/* We sometimes impose a tree structure on the Path graph,
	   which is largely for display purposes.  When this is done,
	   we regerated this list.  This should always be a subset of
	   'somehowJoins'... */
	ArrayList<Path> children;

	private String pathsToIDListString( ArrayList<Path> a ) {
		StringBuffer s = new StringBuffer("");
		int n = a.size();
		for( int i = 0; i < n; ++i ) {
			s.append(a.get(i).getID());
			if( i < n - 1 ) {
				s.append(",");
			}
		}
		return s.toString();
	}

	public String somehowJoinsAsString() {
		return pathsToIDListString( somehowJoins );
	}

	public String childrenAsString() {
		return pathsToIDListString( children );
	}

	public void setChildren( Set<Path> pathsLeft ) {
		// Set the children of this path in a breadth first fashion:
		children.clear();
		Iterator<Path> ci = somehowJoins.iterator();
		while( ci.hasNext() ) {
			Path c = ci.next();
			if( pathsLeft.contains(c) ) {
				children.add(c);
				pathsLeft.remove(c);
			}
		}
		ci = children.iterator();
		while( ci.hasNext() ) {
			Path c = ci.next();
			c.setChildren( pathsLeft );
		}
	}

/*
	public DefaultMutableTreeNode getNode( ) {
		DefaultMutableTreeNode thisNode = new DefaultMutableTreeNode( this );
		for( int i = 0; i < children.size(); ++i ) {
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( children.get(i) );
			thisNode.add( childNode );
		}
		return thisNode;
	}
*/

	public double getRealLength( ) {
		double totalLength = 0;
		for( int i = 1; i < points; ++i  ) {
			double xdiff = precise_x_positions[i] - precise_x_positions[i-1];
			double ydiff = precise_y_positions[i] - precise_y_positions[i-1];
			double zdiff = precise_z_positions[i] - precise_z_positions[i-1];
			totalLength += Math.sqrt(
				xdiff * xdiff +
				ydiff * ydiff +
				zdiff * zdiff );
		}
		return totalLength;
	}

	public String getRealLengthString( ) {
		return String.format( "%.4f", getRealLength() );
	}

	public void createCircles( ) {
		if( tangents_x != null || tangents_y != null || tangents_z != null || radiuses != null )
			throw new RuntimeException("BUG: Trying to create circles data arrays when at least one is already there");
		tangents_x = new double[maxPoints];
		tangents_y = new double[maxPoints];
		tangents_z = new double[maxPoints];
		radiuses = new double[maxPoints];
	}

	boolean primary = false;
	void setPrimary( boolean primary ) {
		this.primary = primary;
	}
	boolean getPrimary( ) {
		return primary;
	}

	/* We call this if we're going to delete the path represented
	   by this object */

	void disconnectFromAll( ) {
		/* This path can be connected to other ones either if:
		      - this starts on other
		      - this ends on other
		      - other starts on this
		      - other ends on this
		   In any of these cases, we need to also remove this
		   from other's somehowJoins and other from this's
		   somehowJoins.
		*/
		Iterator<Path> i = somehowJoins.iterator();
		while( i.hasNext() ) {
			Path other = i.next();
			if( other.startJoins != null && other.startJoins == this ) {
				other.startJoins = null;
				other.startJoinsPoint = null;
			}
			if( other.endJoins != null && other.endJoins == this ) {
				other.endJoins = null;
				other.endJoinsPoint = null;
			}
			int indexInOtherSomehowJoins = other.somehowJoins.indexOf( this );
			if( indexInOtherSomehowJoins >= 0 )
				other.somehowJoins.remove( indexInOtherSomehowJoins );
		}
		somehowJoins.clear();
		startJoins = null;
		startJoinsPoint = null;
		endJoins = null;
		endJoinsPoint = null;
	}

	public void setStartJoin( Path other, PointInImage joinPoint ) {
		setJoin( PATH_START, other, joinPoint );
	}

	public void setEndJoin( Path other, PointInImage joinPoint ) {
		setJoin( PATH_END, other, joinPoint );
	}

	/* This should be the only method that links one path to
	   another */
	void setJoin( int startOrEnd, Path other, PointInImage joinPoint ) {
		if( other == null ) {
			throw new RuntimeException("BUG: setJoin now should never take a null other path");
		}
		if( startOrEnd == PATH_START ) {
			// If there was an existing path, that's an error:
			if( startJoins != null )
				throw new RuntimeException("BUG: setJoin for START should not replace another join");
			startJoins = other;
			startJoinsPoint = joinPoint;
		} else if( startOrEnd == PATH_END ) {
			if( endJoins != null )
				throw new RuntimeException("BUG: setJoin for END should not replace another join");
			endJoins = other;
			endJoinsPoint = joinPoint;
		} else {
			IJ.error( "BUG: unknown first parameter to setJoin" );
		}
		// Also update the somehowJoins list:
		if( somehowJoins.indexOf(other) < 0 ) {
			somehowJoins.add(other);
		}
		if( other.somehowJoins.indexOf(this) < 0 ) {
			other.somehowJoins.add(this);
		}
	}

	public void unsetStartJoin() {
		unsetJoin( PATH_START );
	}

	public void unsetEndJoin() {
		unsetJoin( PATH_END );
	}

	void unsetJoin( int startOrEnd ) {
		Path other;
		Path leaveAloneJoin;
		if( startOrEnd == PATH_START ) {
			other = startJoins;
			leaveAloneJoin = endJoins;
		} else {
			other = endJoins;
			leaveAloneJoin = startJoins;
		}
		if( other == null ) {
			throw new RuntimeException( "Don't call unsetJoin if the other Path is already null" );
		}
		if( ! (other.startJoins == this ||
		       other.endJoins == this ||
		       leaveAloneJoin == other ) ) {
			somehowJoins.remove(other);
			other.somehowJoins.remove(this);
		}
		if( startOrEnd == PATH_START ) {
			startJoins = null;
			startJoinsPoint = null;
		} else {
			endJoins = null;
			endJoinsPoint = null;
		}
	}

	double x_spacing;
	double y_spacing;
	double z_spacing;
	String spacing_units;

	public double getMinimumSeparation() {
		return Math.min(Math.abs(x_spacing),Math.min(Math.abs(y_spacing),Math.abs(z_spacing)));
	}

	Path( double x_spacing, double y_spacing, double z_spacing, String spacing_units ) {
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.spacing_units = spacing_units;
		points = 0;
		maxPoints = 128;
		precise_x_positions = new double[maxPoints];
		precise_y_positions = new double[maxPoints];
		precise_z_positions = new double[maxPoints];
		somehowJoins = new ArrayList<Path>();
		children = new ArrayList<Path>();
	}

	Path( double x_spacing, double y_spacing, double z_spacing, String spacing_units, int reserve ) {
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.spacing_units = spacing_units;
		points = 0;
		maxPoints = reserve;
		precise_x_positions = new double[maxPoints];
		precise_y_positions = new double[maxPoints];
		precise_z_positions = new double[maxPoints];
		somehowJoins = new ArrayList<Path>();
		children = new ArrayList<Path>();
	}

	public int points;
	public int maxPoints;

	public int size( ) {
		return points;
	}

/* FIXME: put back
	public void getPoint( int i, int [] p ) {

		if( (i < 0) || i >= size() ) {
			p[0] = p[1] = p[2] = -1;
			return;
		}

		p[0] = x_positions[i];
		p[1] = y_positions[i];
		p[2] = z_positions[i];
	}
*/

	public void getPointDouble( int i, double [] p ) {

		if( (i < 0) || i >= size() ) {
			throw new RuntimeException("BUG: getPointDouble was asked for an out-of-range point: "+i);
		}

		p[0] = precise_x_positions[i];
		p[1] = precise_y_positions[i];
		p[2] = precise_z_positions[i];
	}

	public PointInImage getPointInImage( int i ) {

		if( (i < 0) || i >= size() ) {
			throw new RuntimeException("BUG: getPointInImage was asked for an out-of-range point: "+i);
		}

		PointInImage result = new PointInImage( precise_x_positions[i],
							precise_y_positions[i],
							precise_z_positions[i] );
		result.onPath = this;
		return result;
	}

	public int getXUnscaled( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getXUnscaled was asked for an out-of-range point: "+i);
		return (int)Math.round( precise_x_positions[i] / x_spacing );
	}

	public int getYUnscaled( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getYUnscaled was asked for an out-of-range point: "+i);
		return (int)Math.round( precise_y_positions[i] / y_spacing );
	}

	public int getZUnscaled( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getZUnscaled was asked for an out-of-range point: "+i);
		return (int)Math.round( precise_z_positions[i] / z_spacing );
	}

	public double getXUnscaledDouble( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getXUnscaled was asked for an out-of-range point: "+i);
		return precise_x_positions[i] / x_spacing;
	}

	public double getYUnscaledDouble( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getYUnscaled was asked for an out-of-range point: "+i);
		return precise_y_positions[i] / y_spacing;
	}

	public double getZUnscaledDouble( int i ) {
		if( (i < 0) || i >= size() )
			throw new RuntimeException("BUG: getZUnscaled was asked for an out-of-range point: "+i);
		return precise_z_positions[i] / z_spacing;
	}

	/** Returns an array [3][npoints] of unscaled coordinates (that is, in pixels). */
	public double[][] getXYZUnscaled() {
		final double[][] p = new double[3][size()];
		for (int i=p[0].length-1; i>-1; i--) {
			p[0][i] = precise_x_positions[i] / x_spacing;
			p[1][i] = precise_y_positions[i] / y_spacing;
			p[2][i] = precise_z_positions[i] / z_spacing;
		}
		return p;
	}


/* FIXME:
	@Override
	public Path clone() {

		Path result = new Path( points );

		System.arraycopy( x_positions, 0, result.x_positions, 0, points );
		System.arraycopy( y_positions, 0, result.y_positions, 0, points );
		System.arraycopy( z_positions, 0, result.z_positions, 0, points );
		result.points = points;
		result.startJoins = startJoins;
		result.startJoinsIndex = startJoinsIndex;
		result.endJoins = endJoins;
		result.endJoinsIndex = endJoinsIndex;

		if( radiuses != null ) {
			this.radiuses = new double[radiuses.length];
			System.arraycopy( radiuses, 0, result.radiuses, 0, radiuses.length );
		}
		if( tangents_x != null ) {
			this.tangents_x = new double[tangents_x.length];
			System.arraycopy( tangents_x, 0, result.tangents_x, 0, tangents_x.length );
		}
		if( tangents_y != null ) {
			this.tangents_y = new double[tangents_y.length];
			System.arraycopy( tangents_y, 0, result.tangents_y, 0, tangents_y.length );
		}
		if( tangents_z != null ) {
			this.tangents_z = new double[tangents_z.length];
			System.arraycopy( tangents_z, 0, result.tangents_z, 0, tangents_z.length );
		}

		return result;
	}
*/

	PointInImage lastPoint( ) {
		if( points < 1 )
			return null;
		else
			return new PointInImage( precise_x_positions[points-1],
						 precise_y_positions[points-1],
						 precise_z_positions[points-1] );
	}

	void expandTo( int newMaxPoints  ) {

		double [] new_precise_x_positions = new double[newMaxPoints];
		double [] new_precise_y_positions = new double[newMaxPoints];
		double [] new_precise_z_positions = new double[newMaxPoints];
		System.arraycopy( precise_x_positions,
				  0,
				  new_precise_x_positions,
				  0,
				  points );
		System.arraycopy( precise_y_positions,
				  0,
				  new_precise_y_positions,
				  0,
				  points );
		System.arraycopy( precise_z_positions,
				  0,
				  new_precise_z_positions,
				  0,
				  points );
		precise_x_positions = new_precise_x_positions;
		precise_y_positions = new_precise_y_positions;
		precise_z_positions = new_precise_z_positions;
		if( hasCircles() ) {
			double [] new_tangents_x = new double[newMaxPoints];
			double [] new_tangents_y = new double[newMaxPoints];
			double [] new_tangents_z = new double[newMaxPoints];
			double [] new_radiuses = new double[newMaxPoints];
			System.arraycopy( tangents_x,
					  0,
					  new_tangents_x,
					  0,
					  points );
			System.arraycopy( tangents_y,
					  0,
					  new_tangents_y,
					  0,
					  points );
			System.arraycopy( tangents_z,
					  0,
					  new_tangents_z,
					  0,
					  points );
			System.arraycopy( radiuses,
					  0,
					  new_radiuses,
					  0,
					  points );
			tangents_x = new_tangents_x;
			tangents_y = new_tangents_y;
			tangents_z = new_tangents_z;
			radiuses = new_radiuses;
		}
		maxPoints = newMaxPoints;
	}

	void add( Path other ) {

		if( other == null ) {
			IJ.log("BUG: Trying to add null Path" );
			return;
		}

		if( maxPoints < (points + other.points) ) {
			expandTo( points + other.points );
		}

		int toSkip = 0;

		/* We may want to skip some points at the beginning of
		   the next path if they're the same as the last point
		   on this path: */

		if( points > 0 ) {
			double last_x = precise_x_positions[points-1];
			double last_y = precise_y_positions[points-1];
			double last_z = precise_z_positions[points-1];
			while((other.precise_x_positions[toSkip] == last_x) &&
			      (other.precise_y_positions[toSkip] == last_y) &&
			      (other.precise_z_positions[toSkip] == last_z)) {
				++toSkip;
			}
		}

		System.arraycopy( other.precise_x_positions,
				  toSkip,
				  precise_x_positions,
				  points,
				  other.points - toSkip );

		System.arraycopy( other.precise_y_positions,
				  toSkip,
				  precise_y_positions,
				  points,
				  other.points - toSkip );

		System.arraycopy( other.precise_z_positions,
				  toSkip,
				  precise_z_positions,
				  points,
				  other.points - toSkip );

		if( endJoins != null )
			throw new RuntimeException("BUG: we should never be adding to a path that already endJoins");

		if( other.endJoins != null ) {
			setEndJoin( other.endJoins, other.endJoinsPoint );
			other.disconnectFromAll();
		}

		points = points + (other.points - toSkip);
	}

	void unsetPrimaryForConnected( HashSet<Path> pathsExplored ) {
		Iterator<Path> i = somehowJoins.iterator();
		while( i.hasNext() ) {
			Path p = i.next();
			if( pathsExplored.contains(p) )
				continue;
			p.setPrimary(false);
			pathsExplored.add(p);
			p.unsetPrimaryForConnected(pathsExplored);
		}
	}

	Path reversed( ) {
		Path c = new Path( x_spacing, y_spacing, z_spacing, spacing_units, points );
		c.points = points;
		for( int i = 0; i < points; ++i ) {
			c.precise_x_positions[i] = precise_x_positions[ (points-1) - i ];
			c.precise_y_positions[i] = precise_y_positions[ (points-1) - i ];
			c.precise_z_positions[i] = precise_z_positions[ (points-1) - i ];
		}
		return c;
	}

	void addPointDouble( double x, double y, double z ) {
		if( points >= maxPoints ) {
			int newReserved = (int)( maxPoints * 1.2 + 1 );
			expandTo( newReserved );
		}
		precise_x_positions[points] = x;
		precise_y_positions[points] = y;
		precise_z_positions[points++] = z;
	}

	public void drawPathAsPoints( TracerCanvas canvas, Graphics g, java.awt.Color c, int plane ) {
		drawPathAsPoints( canvas, g, c, plane, 0, -1 );
	}

	public void drawPathAsPoints( TracerCanvas canvas, Graphics g, java.awt.Color c, int plane, int slice, int either_side ) {

		/* In addition, if this is a start or end point we
		   want to represent that with a circle or a square
		   (depending on whether that's a branch or not.) */

		g.setColor( c );

		int pixel_size = (int)canvas.getMagnification();
		if( pixel_size < 1 )
			pixel_size = 1;

		int spotExtra = pixel_size;
		int spotDiameter = pixel_size * 3;

		// boolean drawDiameter = hasCircles();
		boolean drawDiameter = false;

		Path realStartJoins = fittedVersionOf == null ? startJoins : fittedVersionOf.startJoins;
		Path realEndJoins = fittedVersionOf == null ? endJoins : fittedVersionOf.endJoins;

		int startIndexOfLastDrawnLine = -1;

		for( int i = 0; i < points; ++i ) {

			int x = Integer.MIN_VALUE;
			int y = Integer.MIN_VALUE;
			int previous_x_on_screen = Integer.MIN_VALUE;
			int previous_y_on_screen = Integer.MIN_VALUE;
			int next_x_on_screen = Integer.MIN_VALUE;
			int next_y_on_screen = Integer.MIN_VALUE;
			boolean notFirstPoint = i > 0;
			boolean notLastPoint = i < points - 1;
			int slice_of_point = Integer.MIN_VALUE;

			switch( plane ) {
			case ThreePanes.XY_PLANE:
				x = canvas.myScreenXD(getXUnscaledDouble(i));
				y = canvas.myScreenYD(getYUnscaledDouble(i));
				if( notFirstPoint ) {
					previous_x_on_screen = canvas.myScreenXD( precise_x_positions[i-1]/x_spacing );
					previous_y_on_screen = canvas.myScreenYD( precise_y_positions[i-1]/y_spacing );
				}
				if( notLastPoint ) {
					next_x_on_screen = canvas.myScreenXD( precise_x_positions[i+1]/x_spacing );
					next_y_on_screen = canvas.myScreenYD( precise_y_positions[i+1]/y_spacing );
				}
				slice_of_point = getZUnscaled(i);
				break;
			case ThreePanes.XZ_PLANE:
				x = canvas.myScreenXD(getXUnscaledDouble(i));
				y = canvas.myScreenYD(getZUnscaledDouble(i));
				if( notFirstPoint ) {
					previous_x_on_screen = canvas.myScreenXD( precise_x_positions[i-1]/x_spacing );
					previous_y_on_screen = canvas.myScreenYD( precise_z_positions[i-1]/z_spacing );
				}
				if( notLastPoint ) {
					next_x_on_screen = canvas.myScreenXD( precise_x_positions[i+1]/x_spacing );
					next_y_on_screen = canvas.myScreenYD( precise_z_positions[i+1]/z_spacing );
				}
				slice_of_point = getYUnscaled(i);
				break;
			case ThreePanes.ZY_PLANE:
				x = canvas.myScreenXD(getZUnscaledDouble(i));
				y = canvas.myScreenYD(getYUnscaledDouble(i));
				if( notFirstPoint ) {
					previous_x_on_screen = canvas.myScreenXD( precise_z_positions[i-1]/z_spacing );
					previous_y_on_screen = canvas.myScreenYD( precise_y_positions[i-1]/y_spacing );
				}
				if( notLastPoint ) {
					next_x_on_screen = canvas.myScreenXD( precise_z_positions[i+1]/z_spacing );
					next_y_on_screen = canvas.myScreenYD( precise_y_positions[i+1]/y_spacing );
				}
				slice_of_point = getXUnscaled(i);
				break;
			default:
				throw new RuntimeException("BUG: Unknown plane! ("+plane+")");
			}

			/* If we've been asked to draw the diameters
			   in the 2.5D view, just do it in XY - this is only
			   really for debugging... */

			if( plane == ThreePanes.XY_PLANE && drawDiameter ) {
				// Cross the tangents with a unit z vector:
				double n_x = 0;
				double n_y = 0;
				double n_z = 1;

				double t_x = tangents_x[i];
				double t_y = tangents_y[i];
				double t_z = tangents_z[i];

				double cross_x = n_y * t_z - n_z * t_y;
				double cross_y = n_z * t_x - n_x * t_z;
				double cross_z = n_x * t_y - n_y * t_x;

				double sizeInPlane = Math.sqrt( cross_x * cross_x + cross_y * cross_y );
				double normalized_cross_x = cross_x / sizeInPlane;
				double normalized_cross_y = cross_y / sizeInPlane;

				double left_x = precise_x_positions[i] + normalized_cross_x * radiuses[i];
				double left_y = precise_y_positions[i] + normalized_cross_y * radiuses[i];

				double right_x = precise_x_positions[i] - normalized_cross_x * radiuses[i];
				double right_y = precise_y_positions[i] - normalized_cross_y * radiuses[i];

				int left_x_on_screen = canvas.myScreenXD(left_x/x_spacing);
				int left_y_on_screen = canvas.myScreenYD(left_y/y_spacing);

				int right_x_on_screen = canvas.myScreenXD(right_x/x_spacing);
				int right_y_on_screen = canvas.myScreenYD(right_y/y_spacing);

				int x_on_screen = canvas.myScreenXD( precise_x_positions[i]/x_spacing );
				int y_on_screen = canvas.myScreenYD( precise_y_positions[i]/y_spacing );

				g.drawLine( x_on_screen, y_on_screen, left_x_on_screen, left_y_on_screen );
				g.drawLine( x_on_screen, y_on_screen, right_x_on_screen, right_y_on_screen );
			}

			if( (either_side >= 0) && (Math.abs(slice_of_point - slice) > either_side) )
				continue;

			// If there was a previous point in this path, draw a line from there to here:
			if( notFirstPoint ) {
				// Don't redraw the line if we drew it the previous time, though:
				if( startIndexOfLastDrawnLine != i - 1 ) {
					g.drawLine( previous_x_on_screen, previous_y_on_screen, x, y );
					startIndexOfLastDrawnLine = i - 1;
				}
			}

			// If there's a next point in this path, draw a line from here to there:
			if( notLastPoint ) {
				g.drawLine( x, y, next_x_on_screen, next_y_on_screen );
				startIndexOfLastDrawnLine = i;
			}

			if( ((i == 0) && (realStartJoins == null)) ||
			    ((i == points - 1) && (realEndJoins == null)) ) {
				// Then draw it as a rectangle...
				g.fillRect( x - (spotDiameter / 2), y - (spotDiameter / 2), spotDiameter, spotDiameter );
			} else if( ((i == 0) && (realStartJoins != null)) ||
				   ((i == points - 1) && (realEndJoins != null)) ) {
				// The draw it as an oval...
				g.fillOval( x - (spotDiameter / 2), y - (spotDiameter / 2), spotDiameter, spotDiameter );
			} else {
				// Just draw normally...
				g.fillRect( x - (spotExtra / 2), y - (spotExtra / 2), spotExtra, spotExtra );
			}
		}

	}

        public int indexNearestTo( double x, double y, double z ) {

		if( size() < 1 )
			throw new RuntimeException("indexNearestTo called on a Path of size() = 0");

		PointInImage result = new PointInImage( Double.MIN_VALUE,
							Double.MIN_VALUE,
							Double.MIN_VALUE );

		double minimumDistanceSquared = Double.MAX_VALUE;
		int indexOfMinimum = -1;

		for( int i = 0; i < size(); ++i ) {

			double diff_x = x - precise_x_positions[i];
			double diff_y = y - precise_y_positions[i];
			double diff_z = z - precise_z_positions[i];

			double thisDistanceSquared = diff_x * diff_x + diff_y * diff_y + diff_z * diff_z;

			if( thisDistanceSquared < minimumDistanceSquared ) {
				indexOfMinimum = i;
				minimumDistanceSquared = thisDistanceSquared;
			}
		}

		return indexOfMinimum;
	}

	// ------------------------------------------------------------------------
	// FIXME: adapt these for Path rather than SegmentedConnection, down to EOFIT

	class CircleAttempt implements MultivariateFunction, Comparable {

		double min;
		double [] best;
		double [] initial;

		byte [] data;
		int minValueInData;
		int maxValueInData;
		int side;

		public CircleAttempt(double [] start, byte [] data, int minValueInData, int maxValueInData, int side ) {

			this.data = data;
			this.minValueInData = minValueInData;
			this.maxValueInData = maxValueInData;
			this.side = side;

			min = Double.MAX_VALUE;
			initial = start;
		}

		public int compareTo(Object other) {
			CircleAttempt o = (CircleAttempt)other;
			if (min < o.min)
				return -1;
			else if (min > o.min)
				return +1;
			else
				return 0;
		}

		public int getNumArguments( ) {
			return 3;
		}

		public double getLowerBound( int n ) {
			return 0;
		}

		public double getUpperBound( int n ) {
			return side;
		}

		public double evaluate(double [] x) {
			double badness = evaluateCircle(x[0],x[1],x[2]);

			if (badness < min) {
				best = x.clone();
				min = badness;
			}

			return badness;
		}

		public double evaluateCircle( double x, double y, double r ) {

			double maximumPointPenalty = (maxValueInData - minValueInData) * (maxValueInData - minValueInData);

			double badness = 0;

			for( int i = 0; i < side; ++i ) {
				for( int j = 0; j < side; ++j ) {
					int value = data[j*side+i] & 0xFF;
					if( r * r > ((i - x) * (i - x)  + (j - y) * (j - y)) )
						badness += (maxValueInData - value) * (maxValueInData - value);
					else
						badness += (value - minValueInData) * (value - minValueInData);
				}
			}

			for( double ic = (x - r); ic <= (x + r); ++ic ) {
				for( double jc = (y - r); jc <= (y + r); ++jc ) {
					if( ic < 0 || ic > side || jc < 0 || jc > side )
						badness += maximumPointPenalty;
				}
			}

			badness /= (side * side);

			return badness;
		}

	}

	public boolean isFittedVersionOfAnotherPath() {
		return fittedVersionOf != null;
	}

	Path fitted; // If this path has a fitted version, this is it.
	boolean useFitted = false; // Use the fitted version in preference to this path
	Path fittedVersionOf; // If this path is a fitted version of another one, this is the original

	public void setFitted( Path p ) {
		if( fitted != null ) {
			throw new RuntimeException("BUG: Trying to set a fitted path when there already is one...");
		}
		fitted = p;
		p.fittedVersionOf = this;
	}

	public void setUseFitted( boolean useFitted ) {
		setUseFitted( useFitted, null );
	}

	public void setUseFitted( boolean useFitted, SimpleNeuriteTracer plugin ) {

		if( useFitted && fitted == null )
			throw new RuntimeException("BUG: setUseFitted(true) was called, but the 'fitted' member was null");

		this.useFitted = useFitted;
	}

	public boolean getUseFitted( ) {
		return useFitted;
	}

	public Path getFitted( ) {
		return fitted;
	}

	public void setGuessedTangents( int pointsEitherSide ) {
		if( tangents_x == null || tangents_y == null || tangents_z == null )
			throw new RuntimeException("BUG: setGuessedTangents called with one of the tangent arrays null");
		double [] tangent = new double[3];
		for( int i = 0; i < points; ++i ) {
			getTangent( i, pointsEitherSide, tangent );
			tangents_x[i] = tangent[0];
			tangents_y[i] = tangent[1];
			tangents_z[i] = tangent[2];
		}
	}

	public void getTangent( int i, int pointsEitherSide, double [] result ) {
		int min_index = i - pointsEitherSide;
		if( min_index < 0 )
			min_index = 0;

		int max_index = i + pointsEitherSide;
		if( max_index >= points )
			max_index = points - 1;

		result[0] = precise_x_positions[max_index] - precise_x_positions[min_index];
		result[1] = precise_y_positions[max_index] - precise_y_positions[min_index];
		result[2] = precise_z_positions[max_index] - precise_z_positions[min_index];
	}

	public byte [] squareNormalToVector( int side,        // The number of samples in x and y in the plane, separated by step
					     double step,     // step is in the same units as the _spacing, etc. variables.
					     double ox,      /* These are scaled now */
					     double oy,
					     double oz,
					     double nx,
					     double ny,
					     double nz,
					     double [] x_basis_vector, /* The basis vectors are returned here  */
					     double [] y_basis_vector, /* they *are* scaled by _spacing        */
					     ImagePlus image ) {

		byte [] result = new byte[side*side];

		double epsilon = 0.000001;

		/* To find an arbitrary vector in the normal plane, do
		   the cross product with (0,0,1), unless the normal
		   is parallel to that, in which case we cross it with
		   (0,1,0) instead... */

		double ax, ay, az;

		if( Math.abs(nx) < epsilon && Math.abs(ny) < epsilon ) {
			// Cross with (0,1,0):
			ax = nz;
			ay = 0;
			az = -nx;
		} else {
			// Cross with (0,0,1):
			ax = -ny;
			ay = nx;
			az = 0;
		}

		/* Now to find the other vector in that plane, do the
		 * cross product of (ax,ay,az) with (nx,ny,nz) */

		double bx = ay * nz - az * ny;
		double by = az * nx - ax * nz;
		double bz = ax * ny - ay * nx;

		/* Normalize a and b */

		double a_size = Math.sqrt( ax*ax + ay*ay + az*az );
		ax = ax / a_size; ay = ay / a_size; az = az / a_size;

		double b_size = Math.sqrt( bx*bx + by*by + bz*bz );
		bx = bx / b_size; by = by / b_size; bz = bz / b_size;

		/* Scale them with spacing... */

		double ax_s = ax * step;
		double ay_s = ay * step;
		double az_s = az * step;

		double bx_s = bx * step;
		double by_s = by * step;
		double bz_s = bz * step;

		if( verbose ) {
			System.out.println( "a (in normal plane) is "+ax+","+ay+","+az);
			System.out.println( "b (in normal plane) is "+bx+","+by+","+bz);
		}

		if( true  ) {

			// a and b must be perpendicular:
			double a_dot_b = ax * bx + ay * by + az * bz;

			// ... and each must be perpendicular to the normal
			double a_dot_n = ax * nx + ay * ny + az * nz;
			double b_dot_n = bx * nx + by * ny + bz * nz;

			if( verbose ) {
				System.out.println("a_dot_b: "+a_dot_b);
				System.out.println("a_dot_n: "+a_dot_n);
				System.out.println("b_dot_n: "+b_dot_n);
			}

		}

		// FIXME: do other image types too...
		int width = image.getWidth();
		int height = image.getHeight();
		int depth = image.getStackSize();
		byte [][] v = new byte[depth][];
		ImageStack s = image.getStack();
		for( int z = 0; z < depth; ++z )
			v[z] = (byte []) s.getPixels( z + 1 );

		for( int grid_i = 0; grid_i < side; ++grid_i ) {
			for( int grid_j = 0; grid_j < side; ++grid_j ) {

				double midside_grid = ((side - 1) / 2.0f);

				double gi = midside_grid - grid_i;
				double gj = midside_grid - grid_j;

				double vx = ox + gi * ax_s + gj * bx_s;
				double vy = oy + gi * ay_s + gj * by_s;
				double vz = oz + gi * az_s + gj * bz_s;

				// So now denormalize to pixel co-ordinates:

				double image_x = vx / x_spacing;
				double image_y = vy / y_spacing;
				double image_z = vz / z_spacing;

				/* And do a trilinear interpolation to
				   find the value there: */

				double x_d = image_x - Math.floor(image_x);
				double y_d = image_y - Math.floor(image_y);
				double z_d = image_z - Math.floor(image_z);

				int x_f = (int)Math.floor(image_x); int x_c = (int)Math.ceil(image_x);
				int y_f = (int)Math.floor(image_y); int y_c = (int)Math.ceil(image_y);
				int z_f = (int)Math.floor(image_z); int z_c = (int)Math.ceil(image_z);

				/* Check that these values aren't
				   poking off the edge of the screen -
				   if so then make them zero. */

				double fff;
				double cff;
				double fcf;
				double ccf;

				double ffc;
				double cfc;
				double fcc;
				double ccc;

				if( (x_f < 0) || (x_c < 0) || (y_f < 0) || (y_c < 0) || (z_f < 0) || (z_c < 0) ||
				    (x_f >= width) || (x_c >= width) || (y_f >= height) || (y_c >= height) || (z_f >= depth) || (z_c >= depth) ) {

					fff = 0;
					cff = 0;
					fcf = 0;
					ccf = 0;
					ffc = 0;
					cfc = 0;
					fcc = 0;
					ccc = 0;

				} else {

					fff = v[z_f][width*y_f+x_f]&0xFF;
					cff = v[z_c][width*y_f+x_f]&0xFF;

					fcf = v[z_f][width*y_c+x_f]&0xFF;
					ccf = v[z_c][width*y_c+x_f]&0xFF;

					ffc = v[z_f][width*y_f+x_c]&0xFF;
					cfc = v[z_c][width*y_f+x_c]&0xFF;

					fcc = v[z_f][width*y_c+x_c]&0xFF;
					ccc = v[z_c][width*y_c+x_c]&0xFF;

				}

				// Now we should be OK to do the interpolation for real:

				double i1 = (1 - z_d) * (fff) + (cff) * z_d;
				double i2 = (1 - z_d) * (fcf) + (ccf) * z_d;

				double j1 = (1 - z_d) * (ffc) + (cfc) * z_d;
				double j2 = (1 - z_d) * (fcc) + (ccc) * z_d;

				double w1 = i1 * (1 - y_d) + i2 * y_d;
				double w2 = j1 * (1 - y_d) + j2 * y_d;

				double value_f = w1 * (1 - x_d) + w2 * x_d;

				int value = (int)value_f;
				if( (value < 0) || (value > 255) ) {
					System.out.println("BUG: Out of range value!");
				}

				result[grid_j*side+grid_i] = (byte)value;
			}
		}

		x_basis_vector[0] = ax_s;
		x_basis_vector[1] = ay_s;
		x_basis_vector[2] = az_s;

		y_basis_vector[0] = bx_s;
		y_basis_vector[1] = by_s;
		y_basis_vector[2] = bz_s;

		return result;
	}

	public Path fitCircles( int side, ImagePlus image, boolean display ) {
		return fitCircles( side, image, display, null );
	}

	public Path fitCircles( int side, ImagePlus image, boolean display, SimpleNeuriteTracer plugin ) {

		Path fitted = new Path( x_spacing, y_spacing, z_spacing, spacing_units );

		// if (verbose) System.out.println("Generating normal planes stack.");

		int totalPoints = size();

		if( verbose )
			System.out.println("There are: "+totalPoints+ " in the stack.");

		int pointsEitherSide = 4;

		if( verbose )
			System.out.println("Using spacing: "+x_spacing+","+y_spacing+","+z_spacing);

		int width = image.getWidth();
		int height = image.getHeight();
		int depth = image.getStackSize();

		ImageStack stack = new ImageStack( side, side );

		// We assume that the first and the last in the stack are fine;

		double [] centre_x_positionsUnscaled = new double[totalPoints];
		double [] centre_y_positionsUnscaled = new double[totalPoints];
		double [] rs = new double[totalPoints];
		double [] rsUnscaled = new double[totalPoints];

		double [] ts_x = new double[totalPoints];
		double [] ts_y = new double[totalPoints];
		double [] ts_z = new double[totalPoints];

		double [] optimized_x = new double[totalPoints];
		double [] optimized_y = new double[totalPoints];
		double [] optimized_z = new double[totalPoints];

		double [] scores = new double[totalPoints];

		double [] moved = new double[totalPoints];

		boolean [] valid = new boolean[totalPoints];

		int [] xs_in_image = new int[totalPoints];
		int [] ys_in_image = new int[totalPoints];
		int [] zs_in_image = new int[totalPoints];

		double scaleInNormalPlane = getMinimumSeparation();

		double [] tangent = new double[3];

		for( int i = 0; i < totalPoints; ++i ) {

			getTangent( i, pointsEitherSide, tangent );

			IJ.showProgress( i / (float)totalPoints );

			double x_world = precise_x_positions[i];
			double y_world = precise_y_positions[i];
			double z_world = precise_z_positions[i];

			double [] x_basis_in_plane = new double[3];
			double [] y_basis_in_plane = new double[3];

			byte [] normalPlane = squareNormalToVector(
				side,
				scaleInNormalPlane,   // This is in the same units as the _spacing, etc. variables.
				x_world,      // These are scaled now
				y_world,
				z_world,
				tangent[0],
				tangent[1],
				tangent[2],
				x_basis_in_plane,
				y_basis_in_plane,
				image );

			/* Now at this stage, try to optimize
			   a circle in there... */

			// n.b. thes aren't normalized
			ts_x[i] = tangent[0];
			ts_y[i] = tangent[1];
			ts_z[i] = tangent[2];

			ConjugateDirectionSearch optimizer = new ConjugateDirectionSearch();
			// optimizer.prin = 2; // debugging information on
			optimizer.step = side / 4.0;

			double [] startValues = new double[3];
			startValues[0] = side / 2.0;
			startValues[1] = side / 2.0;
			startValues[2] = 3;

			if( verbose )
				System.out.println("start search at: "+startValues[0]+","+startValues[1]+" with radius: "+startValues[2]);

			int minValueInSquare = Integer.MAX_VALUE;
			int maxValueInSquare = Integer.MIN_VALUE;
			for( int j = 0; j < (side * side); ++j ) {
				int value = normalPlane[j]&0xFF;
				if( value > maxValueInSquare )
					maxValueInSquare = value;
				if( value < minValueInSquare )
					minValueInSquare = value;
			}

			CircleAttempt attempt = new CircleAttempt(
				startValues,
				normalPlane,
				minValueInSquare,
				maxValueInSquare,
				side );

			try {
				optimizer.optimize( attempt, startValues, 2, 2 );
			} catch( ConjugateDirectionSearch.OptimizationError e ) {
				return null;
			}

			if( verbose )
				// System.out.println("u is: "+u[0]+","+u[1]+","+u[2]);
				System.out.println("search optimized to: "+startValues[0]+","+startValues[1]+" with radius: "+startValues[2]);

			centre_x_positionsUnscaled[i] = startValues[0];
			centre_y_positionsUnscaled[i] = startValues[1];
			rsUnscaled[i] = startValues[2];
			rs[i] = scaleInNormalPlane * rsUnscaled[i];

			scores[i] = attempt.min;

			// Now we calculate the real co-ordinates of the new centre:

			double x_from_centre_in_plane = startValues[0] - (side / 2.0);
			double y_from_centre_in_plane = startValues[1] - (side / 2.0);

			moved[i] = scaleInNormalPlane * Math.sqrt( x_from_centre_in_plane * x_from_centre_in_plane +
								   y_from_centre_in_plane * y_from_centre_in_plane );

			if( verbose )
				System.out.println("vector to new centre from original: "+x_from_centre_in_plane+","+y_from_centre_in_plane);

			double centre_real_x = x_world;
			double centre_real_y = y_world;
			double centre_real_z = z_world;

			if( verbose )
				System.out.println("original centre in real co-ordinates: "+centre_real_x+","+centre_real_y+","+centre_real_z);

			// FIXME: I really think these should be +=, but it seems clear from the results that I've got a sign wrong somewhere :(

			centre_real_x -= x_basis_in_plane[0] * x_from_centre_in_plane + y_basis_in_plane[0] * y_from_centre_in_plane;
			centre_real_y -= x_basis_in_plane[1] * x_from_centre_in_plane + y_basis_in_plane[1] * y_from_centre_in_plane;
			centre_real_z -= x_basis_in_plane[2] * x_from_centre_in_plane + y_basis_in_plane[2] * y_from_centre_in_plane;

			if( verbose )
				System.out.println("adjusted original centre in real co-ordinates: "+centre_real_x+","+centre_real_y+","+centre_real_z);

			optimized_x[i] = centre_real_x;
			optimized_y[i] = centre_real_y;
			optimized_z[i] = centre_real_z;

			int x_in_image = (int)Math.round( centre_real_x / x_spacing );
			int y_in_image = (int)Math.round( centre_real_y / y_spacing );
			int z_in_image = (int)Math.round( centre_real_z / z_spacing );

			if( verbose )
				System.out.println("gives in image co-ordinates: "+x_in_image+","+y_in_image+","+z_in_image);

			if( x_in_image < 0 ) x_in_image = 0; if( x_in_image >= width) x_in_image = width - 1;
			if( y_in_image < 0 ) y_in_image = 0; if( y_in_image >= height) y_in_image = height - 1;
			if( z_in_image < 0 ) z_in_image = 0; if( z_in_image >= depth) z_in_image = depth - 1;

			if( verbose )
				System.out.println("addingPoint: "+x_in_image+","+y_in_image+","+z_in_image);

			xs_in_image[i] = x_in_image;
			ys_in_image[i] = y_in_image;
			zs_in_image[i] = z_in_image;

			if( verbose )
				System.out.println("Adding a real slice.");

			ByteProcessor bp = new ByteProcessor( side, side );
			bp.setPixels(normalPlane);
			stack.addSlice(null,bp);
		}

		IJ.showProgress( 1.0 );

		/* Now at each point along the path we calculate the
		   mode of the radiuses in the nearby region: */

		int modeEitherSide = 4;
		double [] modeRadiusesUnscaled = new double[totalPoints];
		double [] modeRadiuses = new double[totalPoints];
		double [] valuesForMode = new double[modeEitherSide * 2 + 1];

		for( int i = 0; i < totalPoints; ++i ) {
			int minIndex = i - modeEitherSide;
			int maxIndex = i + modeEitherSide;
			int c = 0;
			for( int modeIndex = minIndex; modeIndex <= maxIndex; ++modeIndex ) {
				if( modeIndex < 0 )
					valuesForMode[c] = Double.MIN_VALUE;
				else if( modeIndex >= totalPoints )
					valuesForMode[c] = Double.MAX_VALUE;
				else {
					if( rsUnscaled[modeIndex] < 1 )
						valuesForMode[c] = 1;
					else
						valuesForMode[c] = rsUnscaled[modeIndex];
				}
				++c;
			}
			Arrays.sort( valuesForMode );
			modeRadiusesUnscaled[i] = valuesForMode[modeEitherSide];
			modeRadiuses[i] = scaleInNormalPlane * modeRadiusesUnscaled[i];

			valid[i] = moved[i] < modeRadiusesUnscaled[i];
		}

		// Calculate the angle between the vectors from the point to the one on either side:
		double [] angles = new double[totalPoints];
		// Set the end points to 180 degrees:
		angles[0] = angles[totalPoints-1] = Math.PI;
		for( int i = 1; i < totalPoints-1; ++i ) {
			// If there's no previously valid one then
			// just use the first:
			int previousValid = 0;
			for( int j = 0; j < i; ++j )
				if( valid[j] )
					previousValid = j;
			// If there's no next valid one then just use
			// the first:
			int nextValid = totalPoints - 1;
			for( int j = totalPoints - 1; j > i; --j )
				if( valid[j] )
					nextValid = j;
			double adiffx = optimized_x[previousValid] - optimized_x[i];
			double adiffy = optimized_y[previousValid] - optimized_y[i];
			double adiffz = optimized_z[previousValid] - optimized_z[i];
			double bdiffx = optimized_x[nextValid] - optimized_x[i];
			double bdiffy = optimized_y[nextValid] - optimized_y[i];
			double bdiffz = optimized_z[nextValid] - optimized_z[i];
			double adotb = adiffx * bdiffx + adiffy * bdiffy + adiffz * bdiffz;
			double asize = Math.sqrt( adiffx*adiffx + adiffy*adiffy + adiffz*adiffz );
			double bsize = Math.sqrt( bdiffx*bdiffx + bdiffy*bdiffy + bdiffz*bdiffz );
			angles[i] = Math.acos( adotb / (asize * bsize) );
			if( angles[i] < (Math.PI / 2) )
				valid[i] = false;
		}

		/* Repeatedly build an array indicating how many other
		   valid circles each one overlaps with, and remove
		   the worst culprits on each run until they're all
		   gone...  This is horrendously inefficient (O(n^3)
		   in the worst case) but I'm more sure of its
		   correctness than other things I've tried, and there
		   should be few overlapping circles.
		 */
		int [] overlapsWith = new int[totalPoints];
		boolean someStillOverlap = true;
		while( someStillOverlap ) {
			someStillOverlap = false;
			int maximumNumberOfOverlaps = -1;
			for( int i = 0; i < totalPoints; ++i ) {
				overlapsWith[i] = 0;
				if( ! valid[i] )
					continue;
				for( int j = 0; j < totalPoints; ++j ) {
					if( ! valid[j] )
						continue;
					if( i == j )
						continue;
					if( circlesOverlap(
						    ts_x[i], ts_y[i], ts_z[i],
						    optimized_x[i], optimized_y[i], optimized_z[i],
						    rs[i],
						    ts_x[j], ts_y[j], ts_z[j],
						    optimized_x[j], optimized_y[j], optimized_z[j],
						    rs[j] ) ) {
						++ overlapsWith[i];
						someStillOverlap = true;
					}
				}
				if( overlapsWith[i] > maximumNumberOfOverlaps )
					maximumNumberOfOverlaps = overlapsWith[i];
			}
			if( maximumNumberOfOverlaps <= 0 ) {
				break;
			}
			// Now we've built the array, go through and
			// remove the worst offenders:
			for( int i = 0; i < totalPoints; ++i ) {
				if( ! valid[i] )
					continue;
				int n = totalPoints;
				for( int j = totalPoints - 1; j > i; --j )
					if( valid[j] )
						n = j;
				if( overlapsWith[i] == maximumNumberOfOverlaps ) {
					// If the next valid one has
					// the same number, and that
					// has a larger radius, remove
					// that one instead...
					if( n < totalPoints && overlapsWith[n] == maximumNumberOfOverlaps && rs[n] > rs[i] ) {
						valid[n] = false;
					} else {
						valid[i] = false;
					}
					break;
				}
			}
		}

		int lastValidIndex = 0;

		for( int i = 0; i < totalPoints; ++i ) {

			boolean firstOrLast = (i == 0 || i == (points-1));

			if( ! valid[i] ) {
				// The if we're gone too far without a
				// successfully optimized datapoint,
				// add the original one:
				boolean goneTooFar = i - lastValidIndex >= noMoreThanOneEvery;
				boolean nextValid = false;
				if( i < (points - 1) )
					if( valid[i+1] )
						nextValid = true;

				if( (goneTooFar && ! nextValid) || firstOrLast ) {
					valid[i] = true;
					xs_in_image[i] = getXUnscaled(i);
					ys_in_image[i] = getYUnscaled(i);
					zs_in_image[i] = getZUnscaled(i);
					optimized_x[i] = precise_x_positions[i];
					optimized_y[i] = precise_y_positions[i];
					optimized_z[i] = precise_z_positions[i];
					rsUnscaled[i] = 1;
					rs[i] = scaleInNormalPlane;
					modeRadiusesUnscaled[i] = 1;
					modeRadiuses[i] = scaleInNormalPlane;
					centre_x_positionsUnscaled[i] = side / 2.0;
					centre_y_positionsUnscaled[i] = side / 2.0;
				}
			}

			if( valid[i] ) {
				if( rs[i] < scaleInNormalPlane ) {
					rsUnscaled[i] = 1;
					rs[i] = scaleInNormalPlane;
				}
				fitted.addPointDouble( optimized_x[i], optimized_y[i], optimized_z[i] );
				lastValidIndex = i;
			}
		}

		int fittedLength = fitted.size();

		double [] fitted_ts_x = new double[fittedLength];
		double [] fitted_ts_y = new double[fittedLength];
		double [] fitted_ts_z = new double[fittedLength];
		double [] fitted_rs = new double[fittedLength];
		double [] fitted_optimized_x = new double[fittedLength];
		double [] fitted_optimized_y = new double[fittedLength];
		double [] fitted_optimized_z = new double[fittedLength];

		int added = 0;

		for( int i = 0; i < points; ++i ) {
			if( ! valid[i] )
				continue;
			fitted_ts_x[added] = ts_x[i];
			fitted_ts_y[added] = ts_y[i];
			fitted_ts_z[added] = ts_z[i];
			fitted_rs[added] = rs[i];
			fitted_optimized_x[added] = optimized_x[i];
			fitted_optimized_y[added] = optimized_y[i];
			fitted_optimized_z[added] = optimized_z[i];
			++ added;
		}

		if( added != fittedLength )
			throw new RuntimeException( "Mismatch of lengths, added="+added+" and fittedLength="+fittedLength);

		fitted.setFittedCircles( fitted_ts_x,
					 fitted_ts_y,
					 fitted_ts_z,
					 fitted_rs,
					 fitted_optimized_x,
					 fitted_optimized_y,
					 fitted_optimized_z );

		if( display ) {

			ImagePlus imp = new ImagePlus( "normal stack", stack );

			NormalPlaneCanvas normalCanvas = new NormalPlaneCanvas(
				imp,
				plugin,
				centre_x_positionsUnscaled,
				centre_y_positionsUnscaled,
				rsUnscaled,
				scores,
				modeRadiusesUnscaled,
				angles,
				valid,
				fitted  );

			new StackWindow( imp, normalCanvas );

			imp.show();

		}

		fitted.setName( "Fitted Path ["+getID()+"]");

		return fitted;
	}

	double [] radiuses;

	double [] tangents_x;
	double [] tangents_y;
	double [] tangents_z;

	double [] precise_x_positions;
	double [] precise_y_positions;
        double [] precise_z_positions;

	// Going by the meanings of the types given in:
	//   http://www.soton.ac.uk/~dales/morpho/morpho_doc/

	static final int SWC_UNDEFINED       = 0;
	static final int SWC_SOMA            = 1;
	static final int SWC_AXON            = 2;
	static final int SWC_DENDRITE        = 3;
	static final int SWC_APICAL_DENDRITE = 4;
	static final int SWC_FORK_POINT      = 5;
	static final int SWC_END_POINT       = 6;
	static final int SWC_CUSTOM          = 7;

	static final String [] swcTypeNames = { "undefined",
						"soma",
						"axon",
						"dendrite",
						"apical dendrite",
						"fork point",
						"end point",
						"custom" };

	int swcType = 0;

	public boolean circlesOverlap( double n1x, double n1y, double n1z,
				       double c1x, double c1y, double c1z,
				       double radius1,
				       double n2x, double n2y, double n2z,
				       double c2x, double c2y, double c2z,
				       double radius2 ) {
		/* Roughly following the steps described here:
		      http://local.wasp.uwa.edu.au/~pbourke/geometry/planeplane/
		 */
		double epsilon = 0.000001;
		/* Take the cross product of n1 and n2 to see if they
		   are colinear, in which case there is overlap: */
		double crossx = n1y * n2z - n1z * n2y;
		double crossy = n1z * n2x - n1x * n2z;
		double crossz = n1x * n2y - n1y * n2x;
		if( Math.abs(crossx) < epsilon &&
		    Math.abs(crossy) < epsilon &&
		    Math.abs(crossz) < epsilon ) {
			// Then they don't overlap unless they're in
			// the same plane:
			double cdiffx = c2x - c1x;
			double cdiffy = c2y - c1y;
			double cdiffz = c2z - c1z;
			double cdiffdotn1 = cdiffx * n1x + cdiffy * n1y + cdiffz * n1z;
			return Math.abs(cdiffdotn1) < epsilon;
		}
		double n1dotn1 = n1x * n1x + n1y * n1y + n1z * n1z;
		double n2dotn2 = n2x * n2x + n2y * n2y + n2z * n2z;
		double n1dotn2 = n1x * n2x + n1y * n2y + n1z * n2z;

		double det = n1dotn1 * n2dotn2 - n1dotn2 * n1dotn2;
		if( Math.abs(det) < epsilon ) {
			System.out.println("WARNING: det was nearly zero: "+det);
			return true;
		}

		// A vector r in the plane is defined by:
		//      n1 . r = (n1 . c1) = d1

		double d1 = n1x * c1x + n1y * c1y + n1z * c1z;
		double d2 = n2x * c2x + n2y * c2y + n2z * c2z;

		double constant1 = ( d1 * n2dotn2 - d2 * n1dotn2 ) / det;
		double constant2 = ( d2 * n1dotn1 - d1 * n1dotn2 ) / det;

		/* So points on the line, paramaterized by u are now:

		       constant1 n1 + constant2 n2 + u ( n1 x n2 )

		   To find if the two circles overlap, we need to find
		   the values of u where each crosses that line, in
		   other words, for the first circle:

                      radius1 = |constant1 n1 + constant2 n2 + u ( n1 x n2 ) - c1|

                   => 0 = [ (constant1 n1 + constant2 n2 - c1).(constant1 n1 + constant2 n2 - c1) - radius1 ^ 2 ] +
                          [ 2 * ( n1 x n2 ) . ( constant1 n1 + constant2 n2 - c1 ) ] * u
			  [ ( n1 x n2 ) . ( n1 x n2 ) ] * u^2 ]

                   So we solve that quadratic:

		 */
		double a1 = crossx * crossx + crossy * crossy + crossz * crossz;
		double b1 = 2 * ( crossx * ( constant1 * n1x + constant2 * n2x - c1x ) +
				  crossy * ( constant1 * n1y + constant2 * n2y - c1y ) +
				  crossz * ( constant1 * n1z + constant2 * n2z - c1z ) );
		double c1 =
			( constant1 * n1x + constant2 * n2x - c1x ) * ( constant1 * n1x + constant2 * n2x - c1x ) +
			( constant1 * n1y + constant2 * n2y - c1y ) * ( constant1 * n1y + constant2 * n2y - c1y ) +
			( constant1 * n1z + constant2 * n2z - c1z ) * ( constant1 * n1z + constant2 * n2z - c1z ) -
			radius1 * radius1;

		double a2 = crossx * crossx + crossy * crossy + crossz * crossz;
		double b2 = 2 * ( crossx * ( constant1 * n1x + constant2 * n2x - c2x ) +
				  crossy * ( constant1 * n1y + constant2 * n2y - c2y ) +
				  crossz * ( constant1 * n1z + constant2 * n2z - c2z ) );
		double c2 =
			( constant1 * n1x + constant2 * n2x - c2x ) * ( constant1 * n1x + constant2 * n2x - c2x ) +
			( constant1 * n1y + constant2 * n2y - c2y ) * ( constant1 * n1y + constant2 * n2y - c2y ) +
			( constant1 * n1z + constant2 * n2z - c2z ) * ( constant1 * n1z + constant2 * n2z - c2z ) -
			radius2 * radius2;

		// So now calculate the discriminants:
		double discriminant1 = b1 * b1 - 4 * a1 * c1;
		double discriminant2 = b2 * b2 - 4 * a2 * c2;

		if( discriminant1 < 0 || discriminant2 < 0 ) {
			// Then one of the circles doesn't even reach the line:
			return false;
		}

		if( Math.abs(a1) < epsilon ) {
			System.out.println("WARNING: a1 was nearly zero: "+a1);
			return true;
		}

		double u1_1 =   Math.sqrt( discriminant1 ) / ( 2 * a1 ) - b1 / (2 * a1);
		double u1_2 = - Math.sqrt( discriminant1 ) / ( 2 * a1 ) - b1 / (2 * a1);

		double u2_1 =   Math.sqrt( discriminant2 ) / ( 2 * a2 ) - b2 / (2 * a2);
		double u2_2 = - Math.sqrt( discriminant2 ) / ( 2 * a2 ) - b2 / (2 * a2);

		double u1_smaller = Math.min( u1_1, u1_2 );
		double u1_larger  = Math.max( u1_1, u1_2 );

		double u2_smaller = Math.min( u2_1, u2_2 );
		double u2_larger  = Math.max( u2_1, u2_2 );

		// Non-overlapping cases:
		if( u1_larger < u2_smaller )
			return false;
		if( u2_larger < u1_smaller )
			return false;

		// Totally overlapping cases:
		if( u1_smaller <= u2_smaller && u2_larger <= u1_larger )
			return true;
		if( u2_smaller <= u1_smaller && u1_larger <= u2_larger )
			return true;

		// Partially overlapping cases:
		if( u1_smaller <= u2_smaller && u2_smaller <= u1_larger && u1_larger <= u2_larger )
			return true;
		if( u2_smaller <= u1_smaller && u1_smaller <= u2_larger && u2_larger <= u1_larger )
			return true;

		/* We only reach here if something has gone badly
		   wrong, so dump helpful values to aid in debugging: */

		System.out.println("det is: "+det);

		System.out.println("discriminant1 is: "+discriminant1);
		System.out.println("discriminant2 is: "+discriminant2);

		System.out.println("n1: ("+n1x+","+n1y+","+n1z+")");
		System.out.println("n2: ("+n2x+","+n2y+","+n2z+")");

		System.out.println("c1: ("+c1x+","+c1y+","+c1z+")");
		System.out.println("c2: ("+c2x+","+c2y+","+c2z+")");

		System.out.println("radius1: "+radius1);
		System.out.println("radius2: "+radius2);

		throw new RuntimeException("BUG: some overlapping case missed: "+
					   "u1_smaller="+u1_smaller+
					   "u1_larger="+u1_larger+
					   "u2_smaller="+u2_smaller+
					   "u2_larger="+u2_larger);
	}

	public boolean hasCircles() {
		return radiuses != null;
	}

	public void setFittedCircles( double [] tangents_x,
				      double [] tangents_y,
				      double [] tangents_z,
				      double [] radiuses,
				      double [] optimized_x,
				      double [] optimized_y,
				      double [] optimized_z ) {

		this.tangents_x = tangents_x.clone();
		this.tangents_y = tangents_y.clone();
		this.tangents_z = tangents_z.clone();

		this.radiuses = radiuses.clone();

		this.precise_x_positions = optimized_x.clone();
		this.precise_y_positions = optimized_y.clone();
		this.precise_z_positions = optimized_z.clone();
	}

	@Override
	public String toString() {
		if( useFitted )
			return fitted.toString();
		String pathName;
		String name = getName();
		if( name == null )
			name = "Path " + id;
		name += " [" + getRealLengthString( ) + " " + spacing_units + "]";
		if( startJoins != null ) {
			name += ", starts on " + startJoins.getName();
		}
		if( endJoins != null ) {
			name += ", ends on " + endJoins.getName();
		}
		return name;
	}

/*
	@Override
	public String toString() {
		int n = size();
		String result = "";
		if( name != null )
			result += "\"" + name + "\" ";
		result += n + " points";
		if( n > 0 ) {
			result += " from " + x_positions[0] + ", " + y_positions[0] + ", " + z_positions[0];
			result += " to " + x_positions[n-1] + ", " + y_positions[n-1] + ", " + z_positions[n-1];
		}
		return result;
	}
*/

	/* These are various fields that have the current 3D
	   representations of this path.  They should only be updated
	   by synchronized methods, currently:

	        updateContent3D
	        addTo3DViewer
	        removeFrom3DViewer
	*/
	int paths3DDisplay = 1;
	Content content3D;
	Content content3DExtra;
	ImagePlus content3DMultiColored;
	ImagePlus content3DExtraMultiColored;
	String nameWhenAddedToViewer;
	String nameWhenAddedToViewerExtra;

	synchronized void removeIncludingFittedFrom3DViewer(Image3DUniverse univ) {
		removeFrom3DViewer(univ);
		if( useFitted )
			fitted.removeFrom3DViewer(univ);
	}

	synchronized void updateContent3D( Image3DUniverse univ,
					   boolean visible,
					   int paths3DDisplay,
					   Color3f color,
					   ImagePlus colorImage ) {

		if( verbose ) {
			System.out.println("In updateContent3D, colorImage is: "+colorImage);
			System.out.println("In updateContent3D, color is: "+color);
		}

		// So, go through each of the reasons why we might
		// have to remove (and possibly add back) the path:

		if( ! visible ) {
			/* It shouldn't be visible - if any of the
			   contents are non-null, remove them: */
			removeIncludingFittedFrom3DViewer(univ);
			return;
		}

		// Now we know it should be visible.

		Path pathToUse = null;

		if( useFitted ) {
			/* If the non-fitted versions are currently
			   being displayed, remove them: */
			removeFrom3DViewer(univ);
			pathToUse = fitted;
		} else {
			/* If the fitted version is currently being
			   displayed, remove it: */
			if( fitted != null ) {
				fitted.removeFrom3DViewer(univ);
			}
			pathToUse = this;
		}

		if( verbose ) {
			System.out.println("pathToUse is: "+pathToUse);
			System.out.println("  pathToUse.content3D is: "+pathToUse.content3D);
			System.out.println("  pathToUse.content3DExtra is: "+pathToUse.content3DExtra);
			System.out.println("  pathToUse.content3DMultiColored: "+pathToUse.content3DMultiColored);
		}

		// Is the the display (lines-and-discs or surfaces) right?
		if( pathToUse.paths3DDisplay != paths3DDisplay ) {
			pathToUse.removeFrom3DViewer(univ);
			pathToUse.paths3DDisplay = paths3DDisplay;
			pathToUse.addTo3DViewer(univ,color,colorImage);
			return;
		}

		/* Are we now asked to use the color image, but
		   previously were not? */

		if( colorImage == null ) {
			if( (paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_LINES_AND_DISCS
			     && pathToUse.content3DExtraMultiColored != null) ||
			    (paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_SURFACE
			     && pathToUse.content3DMultiColored != null) ) {
				pathToUse.removeFrom3DViewer(univ);
				pathToUse.addTo3DViewer(univ,color,colorImage);
				return;
			}
		} else {
			if( (paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_LINES_AND_DISCS
			     && pathToUse.content3DExtraMultiColored != colorImage) ||
			    (paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_SURFACE
			     && pathToUse.content3DMultiColored != colorImage) ) {
				pathToUse.removeFrom3DViewer(univ);
				pathToUse.addTo3DViewer(univ,color,colorImage);
				return;
			}
		}

		// Is the (flat) color wrong?
		if( realColor == null || ! realColor.equals(color) ) {
			pathToUse.removeFrom3DViewer(univ);
			pathToUse.paths3DDisplay = paths3DDisplay;
			pathToUse.addTo3DViewer(univ,color,colorImage);
			return;
		}

		if( pathToUse.nameWhenAddedToViewer == null || ! univ.contains(pathToUse.nameWhenAddedToViewer) ) {
			pathToUse.paths3DDisplay = paths3DDisplay;
			pathToUse.addTo3DViewer(univ,color,colorImage);
		}
	}

	/* FIXME: this should be based on distance between points in
	   the path, not a static number: */
	public static final int noMoreThanOneEvery = 2;

	synchronized public void removeFrom3DViewer(Image3DUniverse univ) {
		if( content3D != null ) {
			univ.removeContent( nameWhenAddedToViewer );
			content3D = null;
		}
		if( content3DExtra != null ) {
			univ.removeContent( nameWhenAddedToViewerExtra );
			content3DExtra = null;
		}
	}

	public java.util.List<Point3f> getPoint3fList() {
		ArrayList<Point3f> linePoints = new ArrayList<Point3f>();
		for( int i = 0; i < points; ++i ) {
			linePoints.add( new Point3f( (float)precise_x_positions[i],
						     (float)precise_y_positions[i],
						     (float)precise_z_positions[i] ) );
		}
		return linePoints;
	}

	public Content addAsLinesTo3DViewer(Image3DUniverse univ, Color c, ImagePlus colorImage ) {
		return addAsLinesTo3DViewer(univ,new Color3f(c),colorImage);
	}

	public Content addAsLinesTo3DViewer(Image3DUniverse univ, Color3f c, ImagePlus colorImage ) {
		String safeName = univ.getSafeContentName(getName()+" as lines");
		return univ.addLineMesh( getPoint3fList(), c, safeName, true );
	}

	public Content addDiscsTo3DViewer(Image3DUniverse univ, Color c, ImagePlus colorImage ) {
		return addDiscsTo3DViewer(univ,new Color3f(c),colorImage);
	}

	public Content addDiscsTo3DViewer(Image3DUniverse  univ, Color3f c, ImagePlus colorImage ) {
		if( ! hasCircles() )
			return null;

		Color3f [] originalColors = Pipe.getPointColors( precise_x_positions,
								 precise_y_positions,
								 precise_z_positions,
								 c,
								 colorImage );

		List<Color3f> meshColors = new ArrayList<Color3f>();

		int edges = 8;
		List<Point3f> allTriangles = new ArrayList<Point3f>(edges*points);
		for( int i = 0; i < points; ++i ) {
			List<Point3f> discMesh =
				MeshMaker.createDisc( precise_x_positions[i],
						      precise_y_positions[i],
						      precise_z_positions[i],
						      tangents_x[i],
						      tangents_y[i],
						      tangents_z[i],
						      radiuses[i],
						      8 );
			int pointsInDiscMesh = discMesh.size();
			for( int j = 0; j < pointsInDiscMesh; ++j )
				meshColors.add( originalColors[i] );
			allTriangles.addAll(discMesh);
		}
		return univ.addTriangleMesh( allTriangles,
					     meshColors,
					     univ.getSafeContentName("Discs for path "+getName()) );
	}

	synchronized public void addTo3DViewer(Image3DUniverse univ, Color c, ImagePlus colorImage) {
		if( c == null )
			throw new RuntimeException("In addTo3DViewer, Color can no longer be null");
		addTo3DViewer(univ, new Color3f(c), colorImage);
	}

	protected Color3f realColor;

	synchronized public void addTo3DViewer(Image3DUniverse univ, Color3f c, ImagePlus colorImage ) {
		if( c == null )
			throw new RuntimeException("In addTo3DViewer, Color3f can no longer be null");

		realColor = (c == null) ? new Color3f(Color.magenta) : c;

		if(points <= 1) {
			content3D = null;
			content3DExtra = null;
			return;
		}

		if( paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_LINES ||
		    paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_LINES_AND_DISCS ) {
			content3D = addAsLinesTo3DViewer(univ,realColor,colorImage);
			content3D.setLocked(true);
			nameWhenAddedToViewer = content3D.getName();
			if( paths3DDisplay == SimpleNeuriteTracer.DISPLAY_PATHS_LINES_AND_DISCS ) {
				content3DExtra = addDiscsTo3DViewer(univ,realColor,colorImage);
				content3DExtraMultiColored = colorImage;
				if( content3DExtra == null ) {
					nameWhenAddedToViewerExtra = null;
				} else {
					content3DExtra.setLocked(true);
					nameWhenAddedToViewerExtra = content3DExtra.getName();
				}
			}
			// univ.resetView();
			return;
		}

		int pointsToUse = -1;

		double [] x_points_d = new double[points];
		double [] y_points_d = new double[points];
		double [] z_points_d = new double[points];
		double [] radiuses_d = new double[points];

		if( hasCircles() ) {
			int added = 0;
			int lastIndexAdded = - noMoreThanOneEvery;
			for( int i = 0; i < points; ++i ) {
				if( (points <= noMoreThanOneEvery) || (i - lastIndexAdded >= noMoreThanOneEvery) ) {
					x_points_d[added] = precise_x_positions[i];
					y_points_d[added] = precise_y_positions[i];
					z_points_d[added] = precise_z_positions[i];
					radiuses_d[added] = radiuses[i];
					lastIndexAdded = i;
					++ added;
				}
			}
			pointsToUse = added;
		} else {
			for(int i=0; i<points; ++i) {
				x_points_d[i] = precise_x_positions[i];
				y_points_d[i] = precise_y_positions[i];
				z_points_d[i] = precise_z_positions[i];
				radiuses_d[i] = getMinimumSeparation() * 2;
			}
			pointsToUse = points;
		}

		if( pointsToUse == 2 ) {
			// If there are only two points, then makeTube
			// fails, so interpolate:
			double [] x_points_d_new = new double[3];
			double [] y_points_d_new = new double[3];
			double [] z_points_d_new = new double[3];
			double [] radiuses_d_new = new double[3];

			x_points_d_new[0] = x_points_d[0];
			y_points_d_new[0] = y_points_d[0];
			z_points_d_new[0] = z_points_d[0];
			radiuses_d_new[0] = radiuses_d[0];

			x_points_d_new[1] = (x_points_d[0] + x_points_d[1]) / 2;
			y_points_d_new[1] = (y_points_d[0] + y_points_d[1]) / 2;
			z_points_d_new[1] = (z_points_d[0] + z_points_d[1]) / 2;
			radiuses_d_new[1] = (radiuses_d[0] + radiuses_d[1]) / 2;

			x_points_d_new[2] = x_points_d[1];
			y_points_d_new[2] = y_points_d[1];
			z_points_d_new[2] = z_points_d[1];
			radiuses_d_new[2] = radiuses_d[1];

			x_points_d = x_points_d_new;
			y_points_d = y_points_d_new;
			z_points_d = z_points_d_new;
			radiuses_d = radiuses_d_new;

			pointsToUse = 3;
		}

		double [] x_points_d_trimmed = new double[pointsToUse];
		double [] y_points_d_trimmed = new double[pointsToUse];
		double [] z_points_d_trimmed = new double[pointsToUse];
		double [] radiuses_d_trimmed = new double[pointsToUse];

		System.arraycopy( x_points_d, 0, x_points_d_trimmed, 0, pointsToUse );
		System.arraycopy( y_points_d, 0, y_points_d_trimmed, 0, pointsToUse );
		System.arraycopy( z_points_d, 0, z_points_d_trimmed, 0, pointsToUse );
		System.arraycopy( radiuses_d, 0, radiuses_d_trimmed, 0, pointsToUse );

		/* Work out whether to resample or not.  I've found
		   that the resampling is only really required in
		   cases where the points are at adjacent voxels.  So,
		   work out the mean distance between all the points
		   but in image co-ordinates - if there are points
		   only at adjacent voxels this will be between 1 and
		   sqrt(3) ~= 1.73.  However, after the "fitting"
		   process here, we might remove many of these points,
		   so I'll say that we won't resample if the mean is
		   rather higher - above 3.  Hopefully this is a
		   good compromise... */

		double total_length_in_image_space = 0;
		for( int i = 1; i < pointsToUse; ++i ) {
			double x_diff = (x_points_d_trimmed[i] - x_points_d_trimmed[i-1]) / x_spacing;
			double y_diff = (y_points_d_trimmed[i] - y_points_d_trimmed[i-1]) / y_spacing;
			double z_diff = (z_points_d_trimmed[i] - z_points_d_trimmed[i-1]) / z_spacing;
			total_length_in_image_space += Math.sqrt(x_diff*x_diff +
								 y_diff*y_diff +
								 z_diff*z_diff);
		}
		double mean_inter_point_distance_in_image_space = total_length_in_image_space / (pointsToUse - 1);
		if (verbose)
			System.out.println("For path "+this+", got mean_inter_point_distance_in_image_space: "+mean_inter_point_distance_in_image_space);
		boolean resample = mean_inter_point_distance_in_image_space < 3;

		if (verbose)
			System.out.println("... so"+(resample?"":" not")+" resampling");

		ArrayList tubeColors = new ArrayList<Color3f>();

		double [][][] allPoints = Pipe.makeTube(x_points_d_trimmed,
							y_points_d_trimmed,
							z_points_d_trimmed,
							radiuses_d_trimmed,
							resample ? 2 : 1,       // resample - 1 means just "use mean distance between points", 3 is three times that, etc.
							12,         // "parallels" (12 means cross-sections are dodecagons)
							resample,   // do_resample
							realColor,
							colorImage,
							tubeColors);

		if( allPoints == null ) {
			content3D = null;
			content3DExtra = null;
			return;
		}

		// Make tube adds an extra point at the beginning and end:

		List vertexColorList = new ArrayList<Color3f>();
		java.util.List triangles = Pipe.generateTriangles(allPoints,
								  1, // scale
								  tubeColors,
								  vertexColorList);

		nameWhenAddedToViewer = univ.getSafeContentName( getName() );
		// univ.resetView();
		content3D = univ.addTriangleMesh(triangles,
						 vertexColorList,
						 nameWhenAddedToViewer);
		content3D.setLocked(true);
		content3DMultiColored = colorImage;

		content3DExtra = null;
		nameWhenAddedToViewerExtra = null;

		// univ.resetView();
		return;
	}

	public void setSelected(boolean newSelectedStatus) {
		if( newSelectedStatus != selected )
			selected = newSelectedStatus;
	}

	public boolean getSelected() {
		return selected;
	}

	public boolean versionInUse() {
		if( fittedVersionOf != null )
			return fittedVersionOf.useFitted;
		else {
			return ! useFitted;
		}
	}

	/* This doesn't deal with the startJoins, endJoins or fitted
	   fields, since they involve other paths which were probably
	   also transformed by the caller. */

	public Path transform( PathTransformer transformation, ImagePlus template, ImagePlus model ) {

		int modelWidth = model.getWidth();
		int modelHeight = model.getHeight();
		int modelDepth = model.getStackSize();

		int templateWidth = template.getWidth();
		int templateHeight = template.getHeight();
		int templateDepth = template.getStackSize();

		double templatePixelWidth = 1;
		double templatePixelHeight = 1;
		double templatePixelDepth = 1;
		String templateUnits = "pixels";

		Calibration templateCalibration = template.getCalibration();
		if( templateCalibration != null ) {
			templatePixelWidth = templateCalibration.pixelWidth;
			templatePixelHeight = templateCalibration.pixelHeight;
			templatePixelDepth = templateCalibration.pixelDepth;
			templateUnits = templateCalibration.getUnits();
		}

		double modelPixelWidth = 1;
		double modelPixelHeight = 1;
		double modelPixelDepth = 1;

		Calibration modelCalibration = model.getCalibration();
		if( modelCalibration != null ) {
			modelPixelWidth = modelCalibration.pixelWidth;
			modelPixelHeight = modelCalibration.pixelHeight;
			modelPixelDepth = modelCalibration.pixelDepth;
		}

		Path result = new Path( templatePixelWidth, templatePixelHeight, templatePixelDepth, templateUnits, size() );
		double [] transformed = new double[3];

		// Actually, just say you'll have to refit all the
		// previously fitted paths...

		for( int i = 0; i < points; ++i ) {
			double original_x = precise_x_positions[i];
			double original_y = precise_y_positions[i];
			double original_z = precise_z_positions[i];
			transformation.transformPoint( original_x, original_y, original_z, transformed );
			double new_x = transformed[0];
			double new_y = transformed[1];
			double new_z = transformed[2];
			if( Double.isNaN(new_x) ||
			    Double.isNaN(new_y) ||
			    Double.isNaN(new_z) )
				continue;
			result.addPointDouble( new_x, new_y, new_z );
		}

		result.primary = primary;
		result.id = id;
		result.selected = selected;
		result.name = name;

		result.x_spacing = x_spacing;
		result.y_spacing = y_spacing;
		result.z_spacing = z_spacing;
		result.spacing_units = spacing_units;

		result.swcType = swcType;

		return result;
	}
}
