/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

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

class SimplePoint {
	public double x = 0, y = 0, z = 0;
	public int originalIndex;
	public SimplePoint(double x, double y, double z, int originalIndex) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.originalIndex = originalIndex;
	}
}

public class PathDownsampler {

	/**
	 * This is an implementation of the Ramer-Douglas-Peucker algorithm for
	 * simplifying a curve represented by line-segments, as described here:
	 *
	 * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
	 *
	 * @param points
	 * @param permittedDeviation
	 * @return
	 */

	public static ArrayList<SimplePoint> downsample(ArrayList<SimplePoint> points,
			double permittedDeviation) {
		int n = points.size();
		SimplePoint startPoint = points.get(0);
		SimplePoint endPoint = points.get(n - 1);
		double vx = endPoint.x - startPoint.x;
		double vy = endPoint.y - startPoint.y;
		double vz = endPoint.z - startPoint.z;
		double vSize = Math.sqrt(vx * vx + vy * vy + vz * vz);
		// Scale v to be a unit vector along the line:
		vx /= vSize;
		vy /= vSize;
		vz /= vSize;
		// Now find the point between the end points that is the greatest
		// distance from the line:
		double maxDistanceSquared = 0;
		int maxIndex = -1;
		for(int i = 1; i < n - 1; ++i) {
			SimplePoint midPoint = points.get(i);
			double dx = midPoint.x - startPoint.x;
			double dy = midPoint.y - startPoint.y;
			double dz = midPoint.z - startPoint.z;
			double projectedLength = dx*vx + dy*vy + dz*vz;
			double dLengthSquared = dx*dx + dy*dy + dz*dz;
			double distanceSquared = dLengthSquared - projectedLength * projectedLength;
			if (distanceSquared > maxDistanceSquared) {
				maxDistanceSquared = distanceSquared;
				maxIndex = i;
			}
		}
		if (maxDistanceSquared > (permittedDeviation * permittedDeviation)) {
			// Then divide at that point and recurse:
			ArrayList<SimplePoint> firstPart = new ArrayList<SimplePoint>();
			for (int i = 0; i <= maxIndex; ++i)
				firstPart.add(points.get(i));
			ArrayList<SimplePoint> secondPart = new ArrayList<SimplePoint>();
			for (int i = maxIndex; i < n; ++i)
				secondPart.add(points.get(i));
			firstPart = downsample(firstPart, permittedDeviation);
			secondPart = downsample(secondPart, permittedDeviation);
			firstPart.remove(firstPart.size() - 1);
			firstPart.addAll(secondPart);
			return firstPart;
		} else {
			// Otherwise just return the first and last points:
			ArrayList<SimplePoint> result = new ArrayList<SimplePoint>();
			result.add(startPoint);
			result.add(endPoint);
			return result;
		}
	}
}
