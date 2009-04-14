package bijnum;

import java.io.*;
import ij.*;
import ij.process.*;
import java.awt.*;
import ij.gui.*;
import volume.*;
import bijnum.*;

/**
 * k Nearest Neighborhood (brute-force) implementation.
 * You can create a kNN object, add datasets to it, and classify vectors.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJknn
{
        /** The dataset, a MxN matrix of N-dimensional vectors. */
        public float [][] dataset = null;
        /** The corresponding classification for each vector in the dataset, a 1xM vector. */
        public float [] classset = null;
        /** The number of different classes in classset. */
        public int n = 0;
        public static int inserts = 0;

        /** Some variables that I do not want to allocate for every element. */
        protected float [] kDistances;
        protected int [] kIndices;

        public BIJknn(int n)
        {
                this.n = n;
                inserts = 0;
        }
        public BIJknn(float [][] dataset, float [] classset, int n)
        {
                try
                {
                        add(dataset, classset);
                } catch (Exception ignored) {}
                this.n = n;
                inserts = 0;
        }
        /**
         * Add extra data and classification to this knn.
         * @param extradataset an extra dataset
         * @param extraclassset an extra class set.
         */
        public void add(float [][] extradataset, float [] extraclassset)
        throws Exception
        {
                if (dataset == null && classset == null)
                {
                        dataset = extradataset;
                        classset = extraclassset;
                        System.out.println("kNN initialized with: "+dataset.length+" vectors("+dataset[0].length+")");
                        return;
                }
                if (dataset[0].length != extradataset[0].length)
                        throw new IllegalArgumentException("Vector lengths of existing and new data do not match");
                float [][] tds = dataset;
                float [] tcs = classset;
                float [][] dataset = new float[tds.length+extradataset.length][];
                float [] classset = new float[tcs.length+extraclassset.length];
                for (int i = 0; i < tds.length; i++)
                {
                        dataset[i] = tds[i];
                        classset[i] = tcs[i];
                }
                for (int i = tds.length; i < tds.length + extradataset.length; i++)
                {
                        dataset[i] = extradataset[i-extradataset.length];
                        classset[i] = extraclassset[i-extradataset.length];
                }
                System.out.println("kNN added "+extradataset.length+" vectors, total now: "+dataset.length+" vectors");
        }
        /**
         * Find the classification of M (N dimensional) vectors unknown in the dataset.
         * @param unknown a MxN matrix of M vectors to be classified in the dataset.
         * @param k the number of neighbors to use for classification
         * @return classification a vector that will contain the nearest neighbor classificiations for the corresponding vectors in unknown.
         */
        public float [] classify(float [][] unknown, int k)
        {
                int [] kneighbors = new int[k];
                float [] kDistances = new float[k];
                float [] classification = new float[unknown.length];
                for (int i = 0; i < classification.length; i++)
                {
                        search(kneighbors, kDistances, unknown[i], k);
                        int [] votes = new int[n];
                        // Tally the votes for each class in kneighbors.
                        for (int l = 0; l < kneighbors.length; l++)
                        {
                                try
                                {
                                        votes[(int) classset[kneighbors[l]]]++;
                                }
                                catch (ArrayIndexOutOfBoundsException e)
                                {
                                        System.out.print("knn: index out of bounds. votes.length="+votes.length+" kneighbors.length= "+kneighbors.length+" inserts= "+BIJknn.inserts);
                                        for (int j = 0; j < kneighbors.length; j++)
                                        {
                                                System.out.print(" kneighbors[i]="+kneighbors[j]);
                                                System.out.print(" truth[kneighbors[i]]="+classset[kneighbors[j]]);
                                        }
                                        System.out.println();
                                }
                        }
                        int c = 0;
                        // Find the class with the largest vote.
                        for (int j = 0; j < votes.length; j++)
                                if (votes[j] > votes[c]) c = j;
                        classification[i] = c;
                }
                return classification;
        }
        /**
         * Find the k-nearest neighbors of vector v (N dimensional) in dataset and put indices in kIndices, distances in kDistances.
         * @param kIndices a int[] of length k that will contain the indices to the k closest neighbors of v
         * @param kDistances a float[] of length k that will contain the distances to the k closest neighbors of v.
         * @param v the vector to classify
         * @param k the number of neighbors to use for classification
         */
        public void search(int [] kIndices, float [] kDistances, float [] v, int k)
        {
                kDistances[0] = Float.MAX_VALUE;
                kIndices[0] = -1;
                int n = 1;
                for (int j = 0; j < dataset.length; j++)
                {
                        float distance = distance(v, dataset[j]);
                        // Is distance smaller than last neighbor?
                        if (distance < kDistances[n-1])
                        {
                                // Yes, find out where exactly.
                                int i;
                                for (i = n-2; i >= 0; i--) if (kDistances[i] < distance) break;
                                // i now points to the neighbor that is smaller than distance, may even be -1 if smallest.
                                i++;
                                // i now points to where j will go into.
                                int l = i;
                                while (l < (n-1))
                                {
                                        // From i, move all of them down one space, keeping room for j.
                                        kDistances[i+1] = kDistances[i];
                                        kIndices[i+1] = kIndices[i];
                                }
                                kDistances[i] = distance;
                                kIndices[i] = j;
                                inserts++;
                                if (n < k) n++;
                        }
                }
        }
         /**
          * Cleans the dataset
          * using Wilson's pruning algorithm. Classifies each point in the dataset (k=3)
          * and removes it if its class is not the same as the classification by its k neighbors.
          */
         public void prune(int k)
         {
                float [] classification = classify(dataset, k);
                boolean [] keep = new boolean[dataset.length];
                for (int i = 0; i < classification.length; i++)
                        // Check whether true classification and knn classification match.
                        keep[i] = classset[i] == classification[i];
                // Count number of remaining nodes.
                int nr = 0;
                for (int i = 0; i < dataset.length; i++)
                        if (keep[i]) nr++;
                System.out.println("BIJknn.prune(): removing "+(dataset.length - nr)+" nodes");
                int [] indices = new int[nr];
                // Get indices of remaining entries.
                int j = 0;
                for (int i = 0; i < dataset.length; i++)
                        if (keep[i]) indices[j++] = i;
                dataset = BIJmatrix.subset(dataset, indices);
                classset = BIJmatrix.subset(classset, indices);
         }
         /**
          * Return the distance according to some Minkowski metric.
          * This implements Euclidean distance. To save time, the POWER(1/k) is not performed.
          * @param a a float[] vector containing a point
          * @param b a float[] vector containing another point.
          * @return the Minkowski distance between a and b.
          */
          protected static float distance(float [] a, float [] b)
          {
                  double d = 0;
                  for (int i = 0; i < a.length; i++)
                         d += Math.pow(a[i] - b[i], 2);
                  return (float) d;
          }
}
