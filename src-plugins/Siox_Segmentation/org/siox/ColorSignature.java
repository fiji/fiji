/*
   Copyright 2005, 2006 by Gerald Friedland and Kristian Jantz

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.siox;

import java.util.*;

/**
 * Representation of a color signature.
 * <br><br>
 * This class implements a clustering algorithm based on a modified kd-tree.
 * The splitting rule is to simply divide the given interval into two equally
 * sized subintervals.
 * In the <code>stageone()</code>, approximate clusters are found by building
 * up such a tree and stopping when an interval at a node has become smaller
 * than the allowed cluster diameter, which is given by <code>limits</code>.
 * At this point, clusters may be split in several nodes.<br>
 * Therefore, in <code>stagetwo()</code>, nodes that belong to several clusters
 * are recombined by another k-d tree clustering on the prior cluster
 * centroids. To guarantee a proper level of abstraction, clusters that contain
 * less than 0.01% of the pixels of the entire sample are removed. Please
 * refer to the file NOTICE to get links to further documentation.
 *
 * @author Gerald Friedland, Lars Knipping
 * @version 1.02
 */
class ColorSignature
{
	// CHANGELOG
	// 2005-11-02 1.02 add further comments
	// 2005-11-02 1.01 changed clusters1 and cluster2 from Vector to ArrayList
	// 2005-11-02 1.00 initial release

	private static ArrayList clusters1=new ArrayList();
	private static ArrayList clusters2=new ArrayList();

	/**
	 * Stage one of clustering.
	 * @param points float[][] the input points in LAB space
	 * @param depth int used for recursion, start with 0
	 * @param clusters ArrayList an Arraylist to store the clusters
	 * @param limits float[] the cluster diameters
	 * @param length int the total number of points to be processed from points
	 */
	private static void stageone(float[][] points, int depth, ArrayList clusters, float[] limits, int length)
	{
		if (length<1) {
			return;
		}
		int dims=points[0].length;
		int curdim=depth%dims;
		float min=points[0][curdim];
		float max=points[0][curdim];
		// find maximum and minimum
		for (int i=1; i<length; i++) {
			if (min>points[i][curdim]) {
				min=points[i][curdim];
			}
			if (max<points[i][curdim]) {
				max=points[i][curdim];
			}
		}
		if (max-min>limits[curdim]) { // Split according to Rubner-Rule
			// split
			float pivotvalue=((max-min)/2.0f)+min;

			int countsm=0;
			int countgr=0;
			for (int i=0; i<length; i++) { // find out cluster sizes
				if (points[i][curdim]<=pivotvalue) {
					countsm++;
				} else {
					countgr++;
				}
			}
			float[][] smallerpoints=new float[countsm][dims]; // allocate mem
			float[][] biggerpoints=new float[countgr][dims];
			int smallc=0;
			int bigc=0;
			for (int i=0; i<length; i++) { // do actual split
				if (points[i][curdim]<=pivotvalue) {
					smallerpoints[smallc++]=points[i];
				} else {
					biggerpoints[bigc++]=points[i];
				}
			} // create subtrees
			stageone(smallerpoints, depth+1, clusters, limits, smallerpoints.length);
			stageone(biggerpoints, depth+1, clusters, limits, biggerpoints.length);
		} else { // create leave
			clusters.add(points);
		}
	}

	/**
	 * Stage two of clustering.
	 * @param points float[][] the input points in LAB space
	 * @param depth int used for recursion, start with 0
	 * @param clusters ArrayList an Arraylist to store the clusters
	 * @param limits float[] the cluster diameters
	 * @param total int the total number of points as given to stageone
	 * @param threshold should be 0.01 - abstraction threshold
	 */
	private static void stagetwo(float[][] points, int depth, ArrayList clusters, float[] limits, int total, float threshold)
	{
		if (points.length<1) {
			return;
		}
		int dims=points[0].length-1; // without cardinality
		int curdim=depth%dims;
		float min=points[0][curdim];
		float max=points[0][curdim];
		// find maximum and minimum
		for (int i=1; i<points.length; i++) {
			if (min>points[i][curdim]) {
				min=points[i][curdim];
			}
			if (max<points[i][curdim]) {
				max=points[i][curdim];
			}
		}
		if (max-min>limits[curdim]) { // Split according to Rubner-Rule
			// split
			float pivotvalue=((max-min)/2.0f)+min;

			int countsm=0;
			int countgr=0;
			for (int i=0; i<points.length; i++) { // find out cluster sizes
				if (points[i][curdim]<=pivotvalue) {
					countsm++;
				} else {
					countgr++;
				}
			}
			float[][] smallerpoints=new float[countsm][dims]; // allocate mem
			float[][] biggerpoints=new float[countgr][dims];
			int smallc=0;
			int bigc=0;
			for (int i=0; i<points.length; i++) { // do actual split
				if (points[i][curdim]<=pivotvalue) {
					smallerpoints[smallc++]=points[i];
				} else {
					biggerpoints[bigc++]=points[i];
				}
			} // create subtrees
			stagetwo(smallerpoints, depth+1, clusters, limits, total, threshold);
			stagetwo(biggerpoints, depth+1, clusters, limits, total, threshold);
		} else { // create leave
			int sum=0;
			for (int i=0; i<points.length; i++) {
				sum+=points[i][points[i].length-1];
			}
			if (((sum*100.0)/total)>=threshold) {
				float[] point=new float[points[0].length];
				for (int i=0; i<points.length; i++) {
					for (int j=0; j<points[0].length; j++) {
						point[j]+=points[i][j];
					}
				}
				for (int j=0; j<points[0].length-1; j++) {
					point[j]/=points.length;
				}
				clusters.add(point);
			}
		}
	}

	/**
	 * Create a color signature for the given set of pixels.
	 * @param input float[][] a set of pixels in LAB space
	 * @param length int the number of pixels that should be processed from the input
	 * @param limits float[] the cluster diameters for each dimension
	 * @param threshold float the abstraction threshold
	 * @return float[][] a color siganture containing cluster centroids in LAB space
	 */
	static float[][] createSignature(float[][] input, int length, float[] limits, float threshold)
	{
		clusters1.clear();
		clusters2.clear();
		stageone(input, 0, clusters1, limits, length);

		float[][] centroids=new float[clusters1.size()][];
		for (int i=0; i<clusters1.size(); i++) {
			float[][] cluster=(float[][])clusters1.get(i);
			float[] centroid=new float[cluster[0].length+1]; // +1 for the cardinality
			for (int k=0; k<cluster.length; k++) {
				for (int j=0; j<cluster[k].length; j++) {
					centroid[j]+=cluster[k][j];
				}
			}
			for (int j=0; j<cluster[0].length; j++) {
				centroid[j]/=cluster.length;
			}
			centroid[cluster[0].length]=cluster.length;
			centroids[i]=centroid;
		}
		stagetwo(centroids, 0, clusters2, limits, length, threshold); // 0.1 -> see paper by tomasi

		float[][] res=(float[][])clusters2.toArray(new float[0][0]);
		return res;
	}

}
