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

import ij.ImagePlus;

import java.util.PriorityQueue;
import java.util.ArrayList;

public class AutoSearchThread extends SearchThread {

	float [][] tubeValues;
	float tubenessThreshold;

	SinglePathsGraph previousPathGraph;

	ArrayList<AutoPoint> destinations = new ArrayList<AutoPoint>(512);

	public ArrayList<AutoPoint> getDestinations() {
		return destinations;
	}

	int start_x, start_y, start_z;

	public AutoSearchThread(ImagePlus image,
				float [][] tubeValues,
				AutoPoint startPoint,
				float tubenessThreshold,
				SinglePathsGraph previousPathGraph ) {

		super(
			image,  // Image to trace
			-1,     // stackMin (which we don't use at all in the automatic tracer)
			-1,     // stackMax (which we don't use at all in the automatic tracer)
			false,  // bidirectional
			false,  // definedGoal
			false,  // startPaused
			0,      // timeoutSeconds
			1000 ); // reportEveryMilliseconds

		this.verbose = false;

		this.tubeValues = tubeValues;
		this.tubenessThreshold = tubenessThreshold;

		this.previousPathGraph = previousPathGraph;

		this.start_x = startPoint.x;
		this.start_y = startPoint.y;
		this.start_z = startPoint.z;

		SearchNode s = createNewNode( start_x, start_y, start_z,
					      0,
					      estimateCostToGoal( start_x, start_y, start_z, 0 ),
					      null, OPEN_FROM_START );
		addNode(s,true);
	}

	protected double costMovingTo( int new_x, int new_y, int new_z ) {

		double cost;

		// Then this saves a lot of time:
		float measure = tubeValues[new_z][new_y*width+new_x];
		if( measure == 0 )
			measure = 0.2f;
		cost = 1 / measure;

		return cost;
	}

	protected void addingNode( SearchNode n ) {
		if( tubeValues[n.z][n.y*width+n.x] > tubenessThreshold ) {
			AutoPoint p=new AutoPoint(n.x,n.y,n.z);
			destinations.add(p);
		} else if( null != previousPathGraph.get(n.x,n.y,n.z) ) {
			AutoPoint p=new AutoPoint(n.x,n.y,n.z);
			destinations.add(p);
		}
	}

        /* This is the heuristic value for the A* search.  There's no
	 * defined goal in this default superclass implementation, so
	 * always return 0 so we end up with Dijkstra's algorithm. */

        float estimateCostToGoal( int current_x, int current_y, int current_z, int to_goal_or_start ) {
		return 0;
        }

	Path getPathBack( int from_x, int from_y, int from_z ) {
		return nodes_as_image_from_start[from_z][from_y*width+from_x].asPath( x_spacing, y_spacing, z_spacing, spacing_units );
	}

}
