/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.PriorityQueue;

class PathWithLength {

	public double length;
	public ArrayList< GraphNode > path;

}

public class Connectivity {

	ArrayList< GraphNode > allNodes;
	public double [][] distances;

	public int[] redValues;
	public int[] greenValues;
	public int[] blueValues;

	public String[] materialNames;
	public Hashtable< String, Integer > materialNameToIndex;

	public String colorString( String materialName ) {

		if( materialName.equals("Exterior") )
			return "#DDDDDD";
		else {

			Integer material_id_integer = materialNameToIndex.get(materialName);
			int material_id = material_id_integer.intValue();

			double scaling = 1.4;

			int r = (int)( redValues[material_id] * scaling );
			int g = (int)( greenValues[material_id] * scaling );
			int b = (int)( blueValues[material_id] * scaling );

			if( r > 255 )
				r = 255;
			if( g > 255 )
				g = 255;
			if( b > 255 )
				b = 255;

			String redValueString = Integer.toHexString(r);
			if( redValueString.length() <= 1 )
				redValueString = "0" + redValueString;

			String greenValueString = Integer.toHexString(g);
			if( greenValueString.length() <= 1 )
				greenValueString = "0" + greenValueString;

			String blueValueString = Integer.toHexString(b);
			if( blueValueString.length() <= 1 )
				blueValueString = "0" + blueValueString;

			return "#" + redValueString + greenValueString + blueValueString;
		}
	}

	/*
	  public ArrayList< GraphNode > makePath( GraphNode lastNode ) {
	  ArrayList< GraphNode > result = new ArrayList< GraphNode >();
	  makePathHidden( result, lastNode );
	  return result;
	  }

	  private void makePathHidden( ArrayList< GraphNode > result, GraphNode lastNode ) {

	  if( lastNode.previous != null ) {
	  makePathHidden( result, lastNode.previous );
	  }

	  result.add( lastNode );
	  }
	*/

	ArrayList< GraphNode > trimPath( ArrayList< GraphNode > originalPath, String from_material, String to_material ) {

		int last_from_material = -1;
		for( int i = 0; i < originalPath.size(); ++i ) {
			GraphNode g = originalPath.get(i);
			if( from_material.equals(g.material_name) ) {
				last_from_material = i;
			}
		}

		int first_to_material = -1;
		for( int i = originalPath.size() - 1; i >= 0; --i ) {
			GraphNode g = originalPath.get(i);
			if( to_material.equals(g.material_name) ) {
				first_to_material = i;
			}
		}

		if( first_to_material < last_from_material ) {
			if( true )
				return null;
			else {
				System.out.println( "********* Very odd path ********** (" + last_from_material +"," + first_to_material );
				System.out.println( "   from " + from_material + " to " +to_material );
				for( int i = 0; i < originalPath.size(); ++i ) {
					GraphNode g = 	originalPath.get(i);
					System.out.println("  - "+g.toDotName());
				}
			}
		}

		ArrayList< GraphNode > result = new ArrayList< GraphNode >();
		for( int i = last_from_material; i <= first_to_material; ++i ) {
			result.add( originalPath.get(i) );
		}

		return result;
	}

	ArrayList< GraphNode > makePath( GraphNode lastNode ) {

		// System.out.println( "Trying to return result" );

		ArrayList< GraphNode > resultReversed = new ArrayList< GraphNode >();
		GraphNode p = lastNode;
		do {
			resultReversed.add(p);
			// System.out.println(  "adding "+p.toDotName());
		} while( null != (p = p.previous) );

		ArrayList< GraphNode > realResult = new ArrayList< GraphNode >();

		for( int i = resultReversed.size() - 1; i >= 0; --i )
			realResult.add( resultReversed.get(i) );

		return realResult;
	}


	PathWithLength pathBetween( GraphNode start, GraphNode goal ) {

		for( int i = 0; i < allNodes.size(); i++ ) {
			GraphNode g = allNodes.get(i);
			g.g = 0;
			g.h = 0;
			g.previous = null;
		}

		PriorityQueue< GraphNode > closed_from_start = new PriorityQueue< GraphNode >();
		PriorityQueue< GraphNode > open_from_start = new PriorityQueue< GraphNode >();

		Hashtable< GraphNode, GraphNode > open_from_start_hash = new Hashtable< GraphNode, GraphNode >();
		Hashtable< GraphNode, GraphNode > closed_from_start_hash = new Hashtable< GraphNode, GraphNode >();

		start.g = 0;
		start.h = 0;
		start.previous = null;

		// add_node( open_from_start, open_from_start_hash, start );
		open_from_start.add( start );
		open_from_start_hash.put( start, start );

		while( open_from_start.size() > 0 ) {

			// GraphNode p = get_highest_priority( open_from_start, open_from_start_hash );

			// System.out.println("Before poll: "+open_from_start_hash.size()+"/"+open_from_start.size());
			GraphNode p = open_from_start.poll();
			open_from_start_hash.remove( p );
			// System.out.println("After poll: "+open_from_start_hash.size()+"/"+open_from_start.size());

			// System.out.println( " Got node "+p.toDotName()+" from the queue" );

			// Has the route from the start found the goal?

			if( p.id == goal.id ) {
				// System.out.println( "Found the goal! (from start to end)" );
				ArrayList< GraphNode > path = trimPath( makePath(p), start.material_name, goal.material_name );
				if( path == null )
					return null;
				else {
					PathWithLength result = new PathWithLength();
					result.path = path;
					result.length = p.g;
					return result;
				}
			}

			// add_node( closed_from_start, closed_from_start_hash, p );
			closed_from_start.add( p );
			closed_from_start_hash.put( p, p );

			// Now look at all the neighbours...

			for( int i = 0; i < distances.length; ++i ) {

				double d = distances[p.id][i];

				if( d >= 0 ) {

					GraphNode neighbour = allNodes.get(i);
					if( neighbour.material_name.equals("Exterior") ||
					    neighbour.material_name.equals(start.material_name) ||
					    neighbour.material_name.equals(goal.material_name) ) {

						// System.out.println( "  /Considering neighbour: " + neighbour.toDotName() );

						GraphNode newNode = new GraphNode();
						newNode.setFrom( neighbour );
						newNode.g = p.g + d;
						newNode.h = 0;
						newNode.previous = p;

						GraphNode foundInClosed = closed_from_start_hash.get(neighbour);

						GraphNode foundInOpen = open_from_start_hash.get(neighbour);

						// Is there an exisiting route which is
						// better?  If so, discard this new candidate...

						if( (foundInClosed != null) && (foundInClosed.f() <= newNode.f()) ) {
							// System.out.println( "  Found in closed, but no better.");
							continue;
						}

						if( (foundInOpen != null) && (foundInOpen.f() <= newNode.f()) ) {
							// System.out.println( "  Found in open, but no better.");
							continue;
						}

						if( foundInClosed != null ) {

							// System.out.println("Found in closed and better");

							// remove( closed_from_start, closed_from_start_hash, foundInClosed );
							closed_from_start.remove( foundInClosed );
							closed_from_start_hash.remove( foundInClosed );

							foundInClosed.setFrom( newNode );

							// add_node( open_from_start, open_from_start_hash, foundInClosed );
							open_from_start.add( foundInClosed );
							open_from_start_hash.put( foundInClosed, foundInClosed );

							continue;
						}

						if( foundInOpen != null ) {

							// System.out.println("Found in open and better");

							// remove( open_from_start, open_from_start_hash, foundInOpen );
							open_from_start.remove( foundInOpen );
							open_from_start_hash.remove( foundInOpen );

							foundInOpen.setFrom( newNode );

							// add_node( open_from_start, open_from_start_hash, foundInOpen );
							open_from_start.add( foundInOpen );
							open_from_start_hash.put( foundInOpen, foundInOpen );

							continue;
						}

						// Otherwise we add a new node:

						// System.out.println("  Adding new node to open " + newNode.toDotName() );

						// add_node( open_from_start, open_from_start_hash, newNode );
						open_from_start.add( newNode );
						open_from_start_hash.put( newNode, newNode );

					}
				}
			}
		}

		// If we get to here then we haven't found a route to the
		// point.  (With the current impmlementation this shouldn't
		// happen, so print a warning.)  However, in this case let's
		// return the best option:

		return null;

	}

}
