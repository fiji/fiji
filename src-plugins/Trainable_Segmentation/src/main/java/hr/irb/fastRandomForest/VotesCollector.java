/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    VotesCollector.java
 *    Copyright (C) 2009 Fran Supek
 */

package hr.irb.fastRandomForest;

import java.util.concurrent.Callable;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Used to retrieve the out-of-bag vote of an ensemble classifier for a single
 * instance. In classification, does not return the class distribution but only
 * class index of the dominant class.
 *
 * Implements callable so it can be run in multiple threads.
 *
 * @author Fran Supek
 */
public class VotesCollector implements Callable<Double> {

  protected final Classifier[] m_Classifiers;
  protected final int instanceIdx;
  protected final Instances data;
  protected final boolean[][] inBag;


  public VotesCollector(Classifier[] m_Classifiers, int instanceIdx,
          Instances data, boolean[][] inBag) {

    this.m_Classifiers = m_Classifiers;
    this.instanceIdx = instanceIdx;
    this.data = data;
    this.inBag = inBag;

  }


  /** Determine predictions for a single instance. */
  @Override
  public Double call() throws Exception {

    boolean regression = data.classAttribute().isNumeric();

    double[] classProbs = null;
    double regrValue = 0;

    if (!regression)
      classProbs = new double[data.numClasses()];

    int numVotes = 0;
    for (int treeIdx = 0; treeIdx < m_Classifiers.length; treeIdx++) {

      if (inBag[treeIdx][instanceIdx])
        continue;
      
      numVotes++;

      if (regression) {

        double curVote =
                m_Classifiers[treeIdx].classifyInstance( data.instance(instanceIdx) );
        regrValue += curVote;

      } else {
        
        double[] curDist = m_Classifiers[treeIdx].distributionForInstance( data.instance(instanceIdx) );

        for ( int classIdx = 0; classIdx < curDist.length; classIdx++ )
          classProbs[ classIdx ] += curDist[ classIdx ];

      }
      
    }

    double vote;
    if (regression)
      vote = regrValue / numVotes;         // average - for regression
    else
      vote = Utils.maxIndex(classProbs);   // consensus - for classification

    return vote;

  }

  
}
