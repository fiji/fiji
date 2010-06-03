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

import java.util.*;

import features.ComputeCurvatures;

import ij.ImagePlus;

/* This is the thread that explores between two points in an image,
 * doing an A* search with a choice of distance measures. */

public class TracerThread extends SearchThread {

        int start_x;
        int start_y;
        int start_z;
        int goal_x;
        int goal_y;
        int goal_z;

        boolean reciprocal;

        ComputeCurvatures hessian;
	double multiplier;

        Path result;

	@Override
	protected boolean atGoal( int x, int y, int z, boolean fromStart ) {
		if( fromStart )
			return (x == goal_x) && (y == goal_y) && (z == goal_z);
		else
			return (x == start_x) && (y == start_y) && (z == start_z);
	}

	@Override
	protected double minimumCostPerUnitDistance( ) {

		double minimum_cost;

                if( hessian == null ) {

                        minimum_cost = reciprocal ? ( 1 / 255.0 ) : 1;

                } else {

                        // result = 1E-4;  /* for the ratio of e0/e1 */
                        // result = 0.002; // 1;  /* for e1 - e0 */

                        minimum_cost = 1 / 60.0;
                }

		return minimum_cost;
	}


	float [][] tubeness;
	boolean useHessian;

	boolean singleSlice;

        /* If you specify 0 for timeoutSeconds then there is no timeout. */

        public TracerThread( ImagePlus imagePlus,
			     float stackMin,
			     float stackMax,
			     int timeoutSeconds,
			     long reportEveryMilliseconds,
			     int start_x,
			     int start_y,
			     int start_z,
			     int goal_x,
			     int goal_y,
			     int goal_z,
			     boolean reciprocal,
			     boolean singleSlice,
			     ComputeCurvatures hessian,
			     double multiplier,
			     float [][] tubeness,
			     boolean useHessian ) {

		super( imagePlus,
		       stackMin,
		       stackMax,
		       true, // bidirectional
		       true, // definedGoal
		       false, // startPaused,
		       timeoutSeconds,
		       reportEveryMilliseconds );

                this.reciprocal = reciprocal;
		this.singleSlice = singleSlice;
                this.hessian = hessian;
		this.tubeness = tubeness;
		this.multiplier = multiplier;
		// need to do this again since it needs to know if hessian is set...
		minimum_cost_per_unit_distance = minimumCostPerUnitDistance();

		this.tubeness = tubeness;

		this.useHessian = useHessian;

                this.start_x = start_x;
                this.start_y = start_y;
                this.start_z = start_z;
                this.goal_x = goal_x;
                this.goal_y = goal_y;
                this.goal_z = goal_z;

		SearchNode s = createNewNode( start_x, start_y, start_z,
					      0,
					      estimateCostToGoal( start_x, start_y, start_z, true ),
					      null, OPEN_FROM_START );
		addNode(s,true);

		SearchNode g = createNewNode( goal_x, goal_y, goal_z,
					      0,
					      estimateCostToGoal( goal_x, goal_y, goal_z, false ),
					      null, OPEN_FROM_GOAL );

		addNode(g,false);

                this.result = null;
        }

	@Override
        protected void foundGoal( Path pathToGoal ) {
		result = pathToGoal;
	}

        public Path getResult( ) {
                return result;
        }


        /* If we're taking the reciprocal of the value at the new
         * point as our cost, then values of zero cause a problem.
         * This is the value that we use instead of zero there. */

        static final double RECIPROCAL_FUDGE = 0.5;

        /* This cost doesn't take into account the distance between
         * the points - it will be post-multiplied by that value.
         *
         * The minimum cost should be > 0 - it is the value that is
         * used in calculating the heuristic for how far a given point
         * is from the goal. */

	@Override
        protected double costMovingTo( int new_x, int new_y, int new_z ) {

		double value_at_new_point = -1;

		switch(imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			value_at_new_point = slices_data_b[new_z][new_y*width+new_x] & 0xFF;
			break;
		case ImagePlus.GRAY16:
		{
			value_at_new_point = slices_data_s[new_z][new_y*width+new_x];
			value_at_new_point = 255.0 * (value_at_new_point - stackMin) / (stackMax - stackMin);
			break;
		}
		case ImagePlus.GRAY32:
		{
			value_at_new_point = slices_data_f[new_z][new_y*width+new_x];
			value_at_new_point = 255.0 * (value_at_new_point - stackMin) / (stackMax - stackMin);
			break;
		}
		}

                double cost;

		if( useHessian ) {

			if( tubeness == null ) {

				if( singleSlice ) {

					double [] hessianEigenValues = new double[2];

					boolean real = hessian.hessianEigenvaluesAtPoint2D( new_x, new_y,
											    true, hessianEigenValues, false, true, x_spacing, y_spacing );

					// Just use the absolute value
					// of the largest eigenvalue
					// (if it's < 0)

					if( real && (hessianEigenValues[1] < 0) ) {

						double measure = Math.abs( hessianEigenValues[1] );
						if( measure == 0 ) // This should never happen in practice...
							measure = 0.2;

						measure *= multiplier;
						if( measure > 256 )
							measure = 256;

						cost = 1 / measure;

					} else {

						cost = 1 / 0.2;

					}

				} else {

					double [] hessianEigenValues = new double[3];

					boolean real = hessian.hessianEigenvaluesAtPoint3D( new_x, new_y, new_z,
											    true, hessianEigenValues, false, true, x_spacing, y_spacing, z_spacing );

					/* FIXME: there's lots of literature on how to
					   pick this rule (see Sato et al,
					   "Three-dimensional multi-scale line filter
					   for segmentation and visualization of
					   curvilinear structures in medical images".
					   The rule I'm using here probably isn't optimal. */

					double e0 = hessianEigenValues[0];
					double e1 = hessianEigenValues[1];
					double e2 = hessianEigenValues[2];

					if( real && (hessianEigenValues[1] < 0) && (hessianEigenValues[2] < 0) ) {

						double measure = Math.sqrt( hessianEigenValues[1] * hessianEigenValues[2] );

						if( measure == 0 ) // This should never happen in practice...
							measure = 0.2;

						measure *= multiplier;
						if( measure > 256 )
							measure = 256;

						cost = 1 / measure;

					} else {

						cost = 1 / 0.2;

					}

				}


			} else {

				// Then this saves a lot of time:
				float measure = tubeness[new_z][new_y*width+new_x];
				if( measure == 0 )
					measure = 0.2f;
				cost = 1 / measure;

			}

		} else {

                        if( reciprocal ) {
                                cost = 1 / RECIPROCAL_FUDGE;
                                if( value_at_new_point != 0 )
                                        cost = 1.0 / value_at_new_point;
                        } else {
                                cost = 256 - value_at_new_point;
                        }

		}

                return cost;
        }

	@Override
        float estimateCostToGoal( int current_x, int current_y, int current_z, boolean fromStart ) {

                double xdiff = ((fromStart ? goal_x : start_x) - current_x) * x_spacing;
                double ydiff = ((fromStart ? goal_y : start_y) - current_y) * y_spacing;
                double zdiff = ((fromStart ? goal_z : start_z) - current_z) * z_spacing;

                double distance = Math.sqrt( xdiff * xdiff + ydiff * ydiff + zdiff * zdiff );

                return (float) ( minimum_cost_per_unit_distance * distance );
	}

}
