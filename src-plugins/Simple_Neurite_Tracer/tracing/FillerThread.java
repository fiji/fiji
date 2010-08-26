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

import ij.*;
import ij.process.*;

import java.awt.*;
import java.util.*;

public class FillerThread extends SearchThread {

	static final boolean verbose = SimpleNeuriteTracer.verbose;

	/* You should synchronize on this object if you want to rely
	 * on the pause status not changing.  (The run() method is not
	 * synchronized itself, for possibly dubious performance
	 * reasons.) */

        boolean reciprocal;

        double reciprocal_fudge = 0.5;

        public float getDistanceAtPoint( double xd, double yd, double zd ) {

		int x = (int)Math.round( xd );
		int y = (int)Math.round( yd );
		int z = (int)Math.round( zd );

                SearchNode [] slice = nodes_as_image_from_start[z];
                if( slice == null )
			return -1.0f;

		SearchNode n = slice[y*width+x];
		if( n == null )
			return -1.0f;
		else
			return n.g;
        }

        // FIXME: may be buggy, synchronization issues

        Fill getFill( ) {

                Hashtable< SearchNode, Integer > h =
                        new Hashtable< SearchNode, Integer >();

                ArrayList< SearchNode > a =
                        new ArrayList< SearchNode >();

                // The tricky bit here is that we want to create a
                // Fill object with index

		int openAtOrAbove;

                int i = 0;

                for( SearchNode current : closed_from_start ) {
                        /* if( current.g <= threshold ) { */
			h.put( current, new Integer(i) );
			a.add( current );
			++ i;
			/* } */
                }

		openAtOrAbove = i;

		if (verbose) System.out.println("openAtOrAbove is: "+openAtOrAbove);

		for( SearchNode current : open_from_start ) {
                        /* if( current.g <= threshold ) { */
			h.put( current, new Integer(i) );
			a.add( current );
			++ i;
			/* } */
                }

                Fill fill = new Fill();

                fill.setThreshold( threshold );
                if( reciprocal )
                        fill.setMetric( "reciprocal-intensity-scaled" );
                else
                        fill.setMetric( "256-minus-intensity-scaled" );

                fill.setSpacing( x_spacing,
                                 y_spacing,
                                 z_spacing,
                                 spacing_units );

		if (verbose) System.out.println("... out of a.size() "+a.size()+" entries");

                for( i = 0; i < a.size(); ++i ) {
                        SearchNode f = a.get(i);
                        int previousIndex = -1;
                        SearchNode previous = f.getPredecessor();
                        if( previous != null ) {
                                Integer p = h.get(previous);
                                if( p != null ) {
                                        previousIndex = p.intValue();
                                }
                        }
                        fill.add( f.x, f.y, f.z, f.g, previousIndex, i >= openAtOrAbove );
                }

                if( sourcePaths != null ) {
                        fill.setSourcePaths( sourcePaths );
                }

                return fill;
        }

        Set< Path > sourcePaths;

	public static FillerThread fromFill( ImagePlus imagePlus,
					     float stackMin,
					     float stackMax,
					     boolean startPaused,
					     Fill fill ) {

		boolean reciprocal;
		float initialThreshold;
		String metric = fill.getMetric();

		if( metric.equals("reciprocal-intensity-scaled") ) {
			reciprocal = true;
		} else if( metric.equals("256-minus-intensity-scaled") ) {
			reciprocal = false;
		} else {
			IJ.error("Trying to load a fill with an unknown metric ('" + metric + "')");
			return null;
		}

		if (verbose) System.out.println("loading a fill with threshold: " + fill.getThreshold() );

		FillerThread result = new FillerThread( imagePlus,
							stackMin,
							stackMax,
							startPaused,
							reciprocal,
							fill.getThreshold(),
							5000 );

		ArrayList< SearchNode > tempNodes = new ArrayList< SearchNode >();

		for( Fill.Node n : fill.nodeList ) {

			SearchNode s = new SearchNode( n.x,
						       n.y,
						       n.z,
						       (float)n.distance,
						       0,
						       null,
						       SearchThread.FREE );
			tempNodes.add(s);
		}

		for( int i = 0; i < tempNodes.size(); ++i ) {
			Fill.Node n = fill.nodeList.get(i);
			SearchNode s = tempNodes.get(i);
			if( n.previous >= 0 ) {
				s.setPredecessor( tempNodes.get(n.previous) );
			}
			if( n.open ) {
				s.searchStatus = OPEN_FROM_START;
				result.addNode( s, true );
			} else {
				s.searchStatus = CLOSED_FROM_START;
				result.addNode( s, true );
			}
		}
		result.setSourcePaths( fill.sourcePaths );
		return result;
	}

	float threshold;

        public void setThreshold( double threshold ) {
                this.threshold = (float)threshold;
        }

	public float getThreshold( ) {
		return threshold;
	}

        /* If you specify 0 for timeoutSeconds then there is no timeout. */

        public FillerThread( ImagePlus imagePlus,
			     float stackMin,
			     float stackMax,
			     boolean startPaused,
                             boolean reciprocal,
                             double initialThreshold,
			     long reportEveryMilliseconds ) {

		super( imagePlus,
		       stackMin,
		       stackMax,
		       false, // bidirectional
		       false, // definedGoal
		       startPaused,
		       0,
		       reportEveryMilliseconds );

                this.reciprocal = reciprocal;
                setThreshold( initialThreshold );

                long lastThresholdChange = 0;

		setPriority( MIN_PRIORITY );
        }

	public void setSourcePaths( Set<Path> newSourcePaths ) {
		sourcePaths = new HashSet<Path>();
		sourcePaths.addAll(newSourcePaths);
		for( Path p : newSourcePaths ) {
			if( p == null )
				return;
                        for( int k = 0; k < p.size(); ++k ) {
                                SearchNode f = new SearchNode( p.getXUnscaled(k),
                                                               p.getYUnscaled(k),
                                                               p.getZUnscaled(k),
                                                               0,
							       0,
                                                               null,
							       OPEN_FROM_START );
				addNode(f,true);
                        }
		}
	}

        public ImagePlus fillAsImagePlus( boolean realData ) {

		byte [][] new_slice_data_b = new byte[depth][];
		short [][] new_slice_data_s = new short[depth][];
		float [][] new_slice_data_f = new float[depth][];

                for( int z = 0; z < depth; ++z ) {
			switch( imageType ) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				new_slice_data_b[z] = new byte[width*height];
				break;
			case ImagePlus.GRAY16:
				new_slice_data_s[z] = new short[width*height];
				break;
			case ImagePlus.GRAY32:
				new_slice_data_f[z] = new float[width*height];
				break;
			}
                }

                ImageStack stack = new ImageStack(width,height);

                for( int z = 0; z < depth; ++z ) {
			SearchNode [] nodes_this_slice=nodes_as_image_from_start[z];
			if( nodes_this_slice != null )
				for( int y = 0; y < height; ++y ) {
					for( int x = 0; x < width; ++x ) {
						SearchNode s = nodes_as_image_from_start[z][y*width+x];
						if( (s != null) && (s.g <= threshold) ) {
							switch( imageType ) {
							case ImagePlus.GRAY8:
							case ImagePlus.COLOR_256:
								new_slice_data_b[z][y*width+x] = realData ? slices_data_b[z][y*width+x] : (byte)255;
								break;
							case ImagePlus.GRAY16:
								new_slice_data_s[z][y*width+x] = realData ? slices_data_s[z][y*width+x] : 255;
								break;
							case ImagePlus.GRAY32:
								new_slice_data_f[z][y*width+x] = realData ? slices_data_f[z][y*width+x] : 255;
								break;
							default:
								break;
							}
						}
					}
				}

			switch( imageType ) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				ByteProcessor bp = new ByteProcessor(width,height);
				bp.setPixels( new_slice_data_b[z] );
				stack.addSlice(null,bp);
				break;
			case ImagePlus.GRAY16:
				ShortProcessor sp = new ShortProcessor(width,height);
				sp.setPixels( new_slice_data_s[z] );
				stack.addSlice(null,sp);
				break;
			case ImagePlus.GRAY32:
				FloatProcessor fp = new FloatProcessor(width,height);
				fp.setPixels( new_slice_data_f[z] );
				stack.addSlice(null,fp);
				break;
			default:
				break;
			}

                }

                ImagePlus imp=new ImagePlus("filled neuron",stack);

                imp.setCalibration(imagePlus.getCalibration());

		return imp;
        }

	@Override
	protected void reportPointsInSearch() {

		super.reportPointsInSearch();

		// Find the minimum distance in the open list.
		SearchNode p = open_from_start.peek();
		if( p == null )
			return;

		float minimumDistanceInOpen = p.g;

		for( SearchProgressCallback progress : progressListeners ) {
			if( progress instanceof FillerProgressCallback ) {
				FillerProgressCallback fillerProgress = (FillerProgressCallback)progress;
				fillerProgress.maximumDistanceCompletelyExplored( this, minimumDistanceInOpen );
			}
		}

	}


	@Override
	void drawProgressOnSlice( int plane,
				  int currentSliceInPlane,
				  TracerCanvas canvas,
				  Graphics g )  {

		super.drawProgressOnSlice(plane,currentSliceInPlane,canvas,g);

	}


}
