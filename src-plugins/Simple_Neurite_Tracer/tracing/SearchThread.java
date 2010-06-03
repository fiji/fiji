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

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.IJ;
import ij.text.TextWindow;

import stacks.ThreePanes;

import java.util.*;
import java.awt.Color;
import java.awt.Graphics;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

/* This is the thread that explores the image using a variety of
   strategies, for example to trace tubular structures or surfaces. */

public abstract class SearchThread extends Thread {

	boolean verbose = SimpleNeuriteTracer.verbose;

	public static final byte OPEN_FROM_START   = 1;
	public static final byte CLOSED_FROM_START = 2;
	public static final byte OPEN_FROM_GOAL    = 3;
	public static final byte CLOSED_FROM_GOAL  = 4;
	public static final byte FREE              = 5; // Indicates that this node isn't in a list yet...

	/* This calculates the cost of moving to a new point in the
	   image.  This does not take into account the distance to
	   this new point, only the value at it.  This will be
	   post-multiplied by the distance from the last point.  So,
	   if you want to take into account the curvature of the image
	   at that point then you should do so in this method. */

	// The default implementation does a simple reciprocal of the
	// image value scaled to 0 to 255 if it is not already an 8
	// bit value:

	protected double costMovingTo( int new_x, int new_y, int new_z ) {

		double value_at_new_point = -1;
		switch(imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			value_at_new_point = slices_data_b[new_z][new_y*width+new_x] & 0xFF;
			break;
		case ImagePlus.GRAY16:
			value_at_new_point = slices_data_s[new_z][new_y*width+new_x];
			value_at_new_point = 255.0 * (value_at_new_point - stackMin) / (stackMax - stackMin);
			break;
		case ImagePlus.GRAY32:
			value_at_new_point = slices_data_f[new_z][new_y*width+new_x];
			value_at_new_point = 255.0 * (value_at_new_point - stackMin) / (stackMax - stackMin);
			break;
		}

		if( value_at_new_point == 0 )
			return 2.0;
		else
			return 1.0 / value_at_new_point;

	}

	/* Use this for doing special progress updates, beyond what
	   SearchProgressCallback provides. */

	protected void reportPointsInSearch( ) {
		for (Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext();) {
			SearchProgressCallback progress = j.next();
			progress.pointsInSearch(this, open_from_start.size() + (bidirectional ? open_from_goal.size() : 0), closed_from_start.size() + (bidirectional ? closed_from_goal.size() : 0));
		}
	}

	public int pointsConsideredInSearch( ) {
		return open_from_start.size() +
			(bidirectional ? open_from_goal.size() : 0) +
			closed_from_start.size() +
			(bidirectional ? closed_from_goal.size() : 0);
	}

	/* This is a factory method for creating specialized search
	   nodes, subclasses of SearchNode: */

	protected SearchNode createNewNode( int x, int y, int z, float g, float h,
					    SearchNode predecessor,
					    byte searchStatus ) {
		return new SearchNode( x, y, z, g, h, predecessor, searchStatus );
	}

	/* This is called if the goal has been found in the search.
	   If your search has no defined goal, then this will never be
	   called, so don't bother to override it. */

	protected void foundGoal( Path pathToGoal ) {
		/* A dummy implementation that does nothing with this
		   exciting news. */
	}

	protected boolean atGoal( int x, int y, int z, boolean fromStart ) {
		return false;
	}

	Color openColor;
	Color closedColor;
	float drawingThreshold;

	void setDrawingColors( Color openColor, Color closedColor ) {
		this.openColor = openColor;
		this.closedColor = closedColor;
	}

	void setDrawingThreshold( float threshold ) {
		this.drawingThreshold = threshold;
	}

	/* If you need to force the distance between two points to
	   always be greater than some value (e.g. to make your A star
	   heuristic valid or something, then you should override this
	   method and return that value. */

	protected double minimumCostPerUnitDistance( ) {
		return 0.0;
	}

	protected double minimum_cost_per_unit_distance;

	byte [][] slices_data_b;
	short [][] slices_data_s;
	float [][] slices_data_f;

	ImagePlus imagePlus;

	float x_spacing;
	float y_spacing;
	float z_spacing;

	String spacing_units;

	int width;
	int height;
	int depth;

	/* The search may only be bidirectional if definedGoal is true */

	boolean bidirectional;

	/* If there is no definedGoal then the search is just
	   Dijkstra's algorithm (h = 0 in the A* search algorithm. */

	boolean definedGoal;

	boolean startPaused;

	int timeoutSeconds;
	long reportEveryMilliseconds;
	long lastReportMilliseconds;

	ArrayList< SearchProgressCallback > progressListeners;

	public void addProgressListener( SearchProgressCallback callback ) {
		progressListeners.add( callback );
	}

	/* The thread can be in one of these states:

	   - STOPPING: the thread cannot be used again
	   - PAUSED: run() hasn't been started yet or the thread is paused
	   - RUNNING: the run method is going and the thread is unpaused
	*/

	/* This can only be changed in a block synchronized on this object */

	private int threadStatus = PAUSED;

	public static final int RUNNING  = 0;
	public static final int PAUSED   = 1;
	public static final int STOPPING = 2;

	public int getThreadStatus( ) {
		return threadStatus;
	}

	// Safely stops the thread (for discarding the object.)

	public void requestStop( ) {
		if (verbose) System.out.println("requestStop called, about to enter synchronized");
		synchronized (this) {
			if (verbose) System.out.println("... entered synchronized");
			if( threadStatus == PAUSED ) {
				if (verbose) System.out.println("was paused so interrupting");
				this.interrupt();
				if (verbose) System.out.println("done interrupting");
			}
			threadStatus = STOPPING;
			reportThreadStatus();
			if (verbose) System.out.println("... leaving synchronized");
		}
		if (verbose) System.out.println("requestStop finished (threadStatus now "+threadStatus+")");
	}

	/** Override this method if you want to find out when a point
	 * was first discovered:
	 */
	protected void addingNode( SearchNode n ) { }

	public void reportThreadStatus( ) {
		for( Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			SearchProgressCallback progress = j.next();
			progress.threadStatus( this, threadStatus );
		}
	}

	public void reportFinished( boolean success ) {
		for( Iterator<SearchProgressCallback> j = progressListeners.iterator(); j.hasNext(); ) {
			SearchProgressCallback progress = j.next();
			progress.finished( this, success );
		}
	}

	// Toggles the paused or unpaused status of the thread.

	public void pauseOrUnpause( ) {
		// Toggle the paused status:
		if (verbose) System.out.println("pauseOrUnpause called, about to enter synchronized");
		synchronized (this) {
			if (verbose) System.out.println("... entered synchronized");
			switch( threadStatus) {
			case PAUSED:
				if (verbose) System.out.println("paused, going to switch to running - interrupting first");
				this.interrupt();
				if (verbose) System.out.println("finished interrupting");
				threadStatus = RUNNING;
				break;
			case RUNNING:
				if (verbose) System.out.println("running, going to switch to paused");
				threadStatus = PAUSED;
				break;
			default:
				// Do nothing, we're actually stopping anyway.
			}
			reportThreadStatus();
			if (verbose) System.out.println("... leaving synchronized");
		}
		if (verbose) System.out.println("pauseOrUnpause finished");
	}

	int imageType = -1;
	float stackMin;
	float stackMax;

	/* If you specify 0 for timeoutSeconds then there is no timeout. */

	public SearchThread( ImagePlus imagePlus,
			     float stackMin,
			     float stackMax,
			     boolean bidirectional,
			     boolean definedGoal,
			     boolean startPaused,
			     int timeoutSeconds,
			     long reportEveryMilliseconds ) {

		this.imagePlus = imagePlus;

		this.stackMin = stackMin;
		this.stackMax = stackMax;

		this.bidirectional = bidirectional;
		this.definedGoal = definedGoal;
		this.startPaused = startPaused;

		this.imageType = imagePlus.getType();

		width = imagePlus.getWidth();
		height = imagePlus.getHeight();
		depth = imagePlus.getStackSize();

		{
			ImageStack s = imagePlus.getStack();
			switch(imageType) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				slices_data_b = new byte[depth][];
				for( int z = 0; z < depth; ++z )
					slices_data_b[z] = (byte []) s.getPixels( z + 1 );
				break;
			case ImagePlus.GRAY16:
				slices_data_s = new short[depth][];
				for( int z = 0; z < depth; ++z )
					slices_data_s[z] = (short []) s.getPixels( z + 1 );
				break;
			case ImagePlus.GRAY32:
				slices_data_f = new float[depth][];
				for( int z = 0; z < depth; ++z )
					slices_data_f[z] = (float []) s.getPixels( z + 1 );
				break;
			}
		}

		Calibration calibration = imagePlus.getCalibration();

		x_spacing = (float)calibration.pixelWidth;
		y_spacing = (float)calibration.pixelHeight;
		z_spacing = (float)calibration.pixelDepth;
		spacing_units = calibration.getUnit();

		if( (x_spacing == 0.0) ||
		    (y_spacing == 0.0) ||
		    (z_spacing == 0.0) ) {

			IJ.error( "SearchThread: One dimension of the calibration information was zero: (" +
				  x_spacing + "," + y_spacing + "," + z_spacing + ")" );
			return;

		}

		this.timeoutSeconds = timeoutSeconds;
		this.reportEveryMilliseconds = reportEveryMilliseconds;

		closed_from_start = new PriorityQueue<SearchNode>();
		open_from_start = new PriorityQueue<SearchNode>();
		if( bidirectional ) {
			closed_from_goal = new PriorityQueue<SearchNode>();
			open_from_goal = new PriorityQueue<SearchNode>();
		}

		nodes_as_image_from_start = new SearchNode[depth][];
		if( bidirectional )
			nodes_as_image_from_goal = new SearchNode[depth][];

		minimum_cost_per_unit_distance = minimumCostPerUnitDistance();

		progressListeners = new ArrayList< SearchProgressCallback >();
	}

	PriorityQueue<SearchNode> closed_from_start;
	PriorityQueue<SearchNode> open_from_start;

	// The next two are null if the search is not bidirectional
	PriorityQueue<SearchNode> closed_from_goal;
	PriorityQueue<SearchNode> open_from_goal;

	SearchNode [][] nodes_as_image_from_start;
	SearchNode [][] nodes_as_image_from_goal;

	public void printStatus() {
		System.out.println("... with " + open_from_start.size() + " open nodes at the start" );
		System.out.println(" ... and " + closed_from_start.size() + " closed nodes at the start" );
		if( bidirectional ) {
			System.out.println("... with " + open_from_goal.size() + " open nodes at the goal" );
			System.out.println(" ... and " + closed_from_goal.size() + " closed nodes at the goal" );
		} else
			System.out.println(" ... unidirectional search");
	}

	@Override
	public void run( ) {

		try {

			if (verbose) System.out.println("New SearchThread running!");
			if (verbose) printStatus();
			if (verbose) System.out.println("... was asked to start it in the " + (startPaused ? "paused" : "unpaused") + " state.");

			synchronized (this) {
				threadStatus = startPaused ? PAUSED : RUNNING;
				reportThreadStatus();
			}

			long started_at = lastReportMilliseconds = System.currentTimeMillis();

			int loops_at_last_report = 0;
			int loops = 0;

			/*
			  We maintain the list of nodes in the search in a
			  couple of different data structures here, which is
			  bad for memory usage but good for the speed of the
			  search.

			  As well as keeping the nodes in priority lists, we
			  keep them in a set of arrays that are indexed in the
			  same way as voxels in the image.
			*/

			while( (open_from_start.size() > 0) ||
			       (bidirectional && (open_from_goal.size() > 0)) ) {

				if( threadStatus == STOPPING ) {
					reportThreadStatus();
					setExitReason(CANCELLED);
					reportFinished(false);
					return;
				} else if( threadStatus == PAUSED ) {
					try {
						reportThreadStatus();
						Thread.sleep(4000);
					} catch( InterruptedException e ) {
					}
				}

				// We only check every thousandth loop for
				// whether we should report the progress, etc.

				if( 0 == (loops % 1000) ) {

					long currentMilliseconds = System.currentTimeMillis();

					long millisecondsSinceStart = currentMilliseconds - started_at;

					if( (timeoutSeconds > 0) && (millisecondsSinceStart > (1000 * timeoutSeconds)) ) {
						if (verbose) System.out.println("Timed out...");
						setExitReason(TIMED_OUT);
						reportFinished( false );
						return;
					}

					long since_last_report = currentMilliseconds - lastReportMilliseconds;

					if( (reportEveryMilliseconds > 0) && (since_last_report > reportEveryMilliseconds ) ) {

						int loops_since_last_report = loops - loops_at_last_report;
						if (verbose) System.out.println( "milliseconds per loop: " +
										 ( since_last_report / (double)loops_since_last_report ) );

						if (verbose) printStatus();

						reportPointsInSearch();

						loops_at_last_report = loops;
					}
				}

				boolean fromStart = true;
				if( bidirectional )
					fromStart = open_from_goal.size() > open_from_start.size();

				PriorityQueue<SearchNode> open_queue = fromStart ? open_from_start : open_from_goal;
				PriorityQueue<SearchNode> closed_queue = fromStart ? closed_from_start : closed_from_goal;

				SearchNode [][] nodes_as_image_this_search = fromStart ? nodes_as_image_from_start : nodes_as_image_from_goal;
				SearchNode [][] nodes_as_image_other_search = fromStart ? nodes_as_image_from_goal : nodes_as_image_from_start;

				SearchNode p = null;

				if( open_queue.size() == 0 )
					continue;

				// p = get_highest_priority( open_from_start, open_from_start_hash );
				p = open_queue.poll();
				if( p == null )
					continue;

				// Has the route from the start found the goal?
				if( definedGoal && atGoal( p.x, p.y, p.z, fromStart ) ) {
					if (verbose) System.out.println( "Found the goal!" );
					if( fromStart )
						foundGoal( p.asPath( x_spacing, y_spacing, z_spacing, spacing_units ) );
					else
						foundGoal( p.asPathReversed( x_spacing, y_spacing, z_spacing, spacing_units ) );
					setExitReason(SUCCESS);
					reportFinished( true );
					return;
				}

				p.searchStatus = fromStart ? CLOSED_FROM_START : CLOSED_FROM_GOAL;
				closed_queue.add( p );
				nodes_as_image_this_search[p.z][p.y*width+p.x] = p;

				// Now look at the neighbours of p.  We're going to consider
				// the 26 neighbours in 3D.

				for( int zdiff = -1; zdiff <= 1; zdiff++ ) {

					int new_z = p.z + zdiff;
					if( new_z < 0 || new_z >= depth )
						continue;

					if( nodes_as_image_this_search[new_z] == null ) {
						nodes_as_image_this_search[new_z] = new SearchNode[width*height];
					}

					for( int xdiff = -1; xdiff <= 1; xdiff++ )
						for( int ydiff = -1; ydiff <= 1; ydiff++ ) {

							if( (xdiff == 0) && (ydiff == 0) && (zdiff == 0) )
								continue;

							int new_x = p.x + xdiff;
							int new_y = p.y + ydiff;

							if( new_x < 0 || new_x >= width )
								continue;

							if( new_y < 0 || new_y >= height )
								continue;

							double xdiffsq = (xdiff * x_spacing) * (xdiff * x_spacing);
							double ydiffsq = (ydiff * y_spacing) * (ydiff * y_spacing);
							double zdiffsq = (zdiff * z_spacing) * (zdiff * z_spacing);

							float h_for_new_point = estimateCostToGoal( new_x, new_y, new_z, fromStart );

							double cost_moving_to_new_point = costMovingTo( new_x, new_y, new_z );
							if( cost_moving_to_new_point < minimum_cost_per_unit_distance ) {
								cost_moving_to_new_point = minimum_cost_per_unit_distance;
							}

							float g_for_new_point = (float) ( p.g + Math.sqrt( xdiffsq + ydiffsq + zdiffsq ) * cost_moving_to_new_point );

							float f_for_new_point = h_for_new_point + g_for_new_point;

							SearchNode newNode = createNewNode( new_x, new_y, new_z,
											    g_for_new_point, h_for_new_point,
											    p, FREE );

							// Is this newNode really new?
							SearchNode alreadyThereInThisSearch = nodes_as_image_this_search[new_z][new_y*width+new_x];

							if( alreadyThereInThisSearch == null ) {

								newNode.searchStatus = fromStart ? OPEN_FROM_START : OPEN_FROM_GOAL;
								open_queue.add( newNode );
								addingNode( newNode );
								nodes_as_image_this_search[new_z][new_y*width+new_x] = newNode;

							} else {

								// The other alternative is that this node is already in one
								// of the lists working from the start but has a better way
								// of getting to that point.

								if( alreadyThereInThisSearch.f > f_for_new_point ) {

									if( alreadyThereInThisSearch.searchStatus == (fromStart ? OPEN_FROM_START : OPEN_FROM_GOAL) ) {

										open_queue.remove( alreadyThereInThisSearch );
										alreadyThereInThisSearch.setFrom( newNode );
										alreadyThereInThisSearch.searchStatus = fromStart ? OPEN_FROM_START : OPEN_FROM_GOAL;
										open_queue.add( alreadyThereInThisSearch );

									} else if( alreadyThereInThisSearch.searchStatus == (fromStart ? CLOSED_FROM_START : CLOSED_FROM_GOAL) ) {

										closed_queue.remove( alreadyThereInThisSearch );
										alreadyThereInThisSearch.setFrom( newNode );
										alreadyThereInThisSearch.searchStatus = fromStart ? OPEN_FROM_START : OPEN_FROM_GOAL;
										open_queue.add( alreadyThereInThisSearch );
									}
								}
							}

							if( bidirectional && nodes_as_image_other_search[new_z] != null ) {

								SearchNode alreadyThereInOtherSearch = nodes_as_image_other_search[new_z][new_y*width+new_x];
								if( alreadyThereInOtherSearch != null ) {

									Path result = null;

									// If either of the next two if conditions are true
									// then we've finished.

									if( alreadyThereInOtherSearch != null &&
									    (alreadyThereInOtherSearch.searchStatus == CLOSED_FROM_START ||
									     alreadyThereInOtherSearch.searchStatus == CLOSED_FROM_GOAL) ) {

										if( fromStart ) {
											result = p.asPath( x_spacing, y_spacing, z_spacing, spacing_units );
											Path fromGoalReversed = alreadyThereInOtherSearch.asPathReversed( x_spacing, y_spacing, z_spacing, spacing_units );
											result.add( fromGoalReversed );
										} else {
											result = alreadyThereInOtherSearch.asPath( x_spacing, y_spacing, z_spacing, spacing_units );
											result.add( p.asPathReversed( x_spacing, y_spacing, z_spacing, spacing_units ) );
										}
										if (verbose) System.out.println("Searches met!");
										foundGoal( result );
										setExitReason(SUCCESS);
										reportFinished( true );
										return;
									}
								}
							}
						}
				}
				++ loops;
			}

			/* If we get to here then we haven't found a route to
			   the point.  (With the current impmlementation this
			   shouldn't happen, so print a warning - probably the
			   programmer hasn't populated the open list to start
			   with.)  However, in this case let's return the best
			   path so far anyway... */

			if (verbose) System.out.println( "FAILED to find a route.  Shouldn't happen..." );
			setExitReason(POINTS_EXHAUSTED);
			reportFinished( false );
		} catch( OutOfMemoryError oome ) {
			System.out.println("Got an OOME: "+oome);
			oome.printStackTrace();
			IJ.error("Out of memory while searching for a path");
			setExitReason(OUT_OF_MEMORY);
			reportFinished( false );
		} catch( Throwable t ) {
			// This is more-or-less based on the catch( Throwable )
			// in Excecuter.java in ImageJ.  FIXME: change this to
			// call the Bug_Submitter directly...
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			t.printStackTrace(pw);
			String s = "There was an exception in the search thread:\n";
			s += caw.toString();
			int w=350, h=250;
			if (IJ.getInstance()!=null)
				new TextWindow("Exception in SearchThread", s, w, h);
			else
				IJ.log(s);
		}
		return;

	}

	/* This is the heuristic value for the A* search.  There's no
	 * defined goal in this default superclass implementation, so
	 * always return 0 so we end up with Dijkstra's algorithm. */

	float estimateCostToGoal( int current_x, int current_y, int current_z, boolean fromStart ) {
		return 0;
	}

	public static int SUCCESS = 0;
	public static int CANCELLED = 1;
	public static int TIMED_OUT = 2;
	public static int POINTS_EXHAUSTED = 3;
	public static int OUT_OF_MEMORY = 4;

	public static String [] exitReasonStrings = { "SUCCESS",
						      "CANCELLED",
						      "TIMED_OUT",
						      "POINTS_EXHAUSTED",
						      "OUT_OF_MEMORY" };

	protected int exitReason;

	/* This method is used to set the reason for the thread finishing */
	void setExitReason( int exitReason ) {
		this.exitReason = exitReason;
	}

	/* Use this to find out why the thread exited if you're not adding
	   listeners to do that. */
	int getExitReason( ) {
		return exitReason;
	}

	SearchNode anyNodeUnderThreshold( int x, int y, int z, double threshold ) {
		SearchNode [] startSlice = nodes_as_image_from_start[z];
		SearchNode [] goalSlice = null;
		if( nodes_as_image_from_goal != null )
			goalSlice = nodes_as_image_from_goal[z];
		int index = y*width+x;
		SearchNode n = null;
		if( startSlice != null ) {
			n = startSlice[index];
			if( n != null && threshold >= 0 && n.g > threshold )
				n = null;
			if( n == null && goalSlice != null ) {
				n = goalSlice[index];
				if( threshold >= 0 && n.g > threshold )
					n = null;
			}
		}
		return n;
	}

	/* This draws over the Graphics object the current progress of
	   the search at this slice.  If openColor or closedColor are
	   null then that means "don't bother to draw that list". */

	void drawProgressOnSlice( int plane,
				  int currentSliceInPlane,
				  TracerCanvas canvas,
				  Graphics g ){

		for( int i = 0; i < 2; ++i ) {

			/* The first time through we draw the nodes in
			   the open list, the second time through we
			   draw the nodes in the closed list. */

			byte start_status = (i == 0) ? OPEN_FROM_START : CLOSED_FROM_START;
			byte goal_status = (i == 0) ? OPEN_FROM_GOAL : CLOSED_FROM_GOAL;
			Color c = (i == 0) ? openColor : closedColor;
			if( c == null )
				continue;

			g.setColor(c);

			int pixel_size = (int)canvas.getMagnification();
			if( pixel_size < 1 )
				pixel_size = 1;

			if( plane == ThreePanes.XY_PLANE ) {
				int z = currentSliceInPlane;
				for( int y = 0; y < height; ++y )
					for( int x = 0; x < width; ++x ) {
						SearchNode n = anyNodeUnderThreshold(x,y,z,drawingThreshold);
						if (n == null)
							continue;
						byte status = n.searchStatus;
						if( status == start_status || status == goal_status )
							g.fillRect( canvas.myScreenX(x) - pixel_size / 2, canvas.myScreenY(y) - pixel_size / 2, pixel_size, pixel_size );
					}
			} else if( plane == ThreePanes.XZ_PLANE ) {
				int y = currentSliceInPlane;
				for( int z = 0; z < depth; ++ z )
					for( int x = 0; x < width; ++x ) {
						SearchNode n = anyNodeUnderThreshold(x,y,z,drawingThreshold);
						if (n == null)
							continue;
						byte status = n.searchStatus;
						if( status == start_status || status == goal_status )
							g.fillRect( canvas.myScreenX(x) - pixel_size / 2, canvas.myScreenY(z) - pixel_size / 2, pixel_size, pixel_size );
					}
			} else if( plane == ThreePanes.ZY_PLANE ) {
				int x = currentSliceInPlane;
				for( int y = 0; y < height; ++y )
					for( int z = 0; z < depth; ++z ) {
						SearchNode n = anyNodeUnderThreshold(x,y,z,drawingThreshold);
						if (n == null)
							continue;
						byte status = n.searchStatus;
						if( status == start_status || status == goal_status )
							g.fillRect( canvas.myScreenX(z) - pixel_size / 2, canvas.myScreenY(y) - pixel_size / 2, pixel_size, pixel_size );
					}
			}
		}
	}

	// Add a node, ignoring requests to add duplicate nodes:

	public void addNode( SearchNode n, boolean fromStart ) {

		SearchNode [][] nodes_as_image = fromStart ? nodes_as_image_from_start : nodes_as_image_from_goal;

		if( nodes_as_image[n.z] == null ) {
			nodes_as_image[n.z] = new SearchNode[width*height];
		}

		if( nodes_as_image[n.z][n.y*width+n.x] != null ) {
			// Then there's already a node there:
			return;
		}

		if( n.searchStatus == OPEN_FROM_START ) {

			open_from_start.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;

		} else if( n.searchStatus == OPEN_FROM_GOAL ) {
			assert( ! (bidirectional && definedGoal ) );

			open_from_goal.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;

		} else if( n.searchStatus == CLOSED_FROM_START ) {

			closed_from_start.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;

		} else if( n.searchStatus == CLOSED_FROM_GOAL ) {
			assert( ! (bidirectional && definedGoal ) );

			closed_from_goal.add( n );
			nodes_as_image[n.z][n.y*width+n.x] = n;

		}

	}

}
