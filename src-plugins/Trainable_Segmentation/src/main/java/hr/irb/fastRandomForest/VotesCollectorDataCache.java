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
 *    VotesCollectorDataCache.java
 *    Copyright (C) 2013 Fran Supek
 */

package hr.irb.fastRandomForest;

import java.util.Random;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.Utils;

import java.util.concurrent.Callable;

/**
 * Used to retrieve the out-of-bag vote of an FastRandomForest classifier for a
 * single instance from a DataCache the forest was trained on. New in 0.99.
 * Used for OOB error calculation and feature importances. <p>
 * 
 * In classification, does not return the class distribution, but only the class
 * index of the dominant class.
 * <p/>
 * Implements callable so it can be run in multiple threads.
 *
 * @author Fran Supek
 */
public class VotesCollectorDataCache implements Callable<Double>{

  protected final Classifier[] m_Classifiers;
  protected final int instanceIdx;
  protected final DataCache data;
  /** NumTrees x numInstances indicating out-of-bag instances. */
  protected final boolean[][] inBag;


  public VotesCollectorDataCache(Classifier[] m_Classifiers, int instanceIdx,
                        DataCache data, boolean[][] inBag){
    this.m_Classifiers = m_Classifiers;
    this.instanceIdx = instanceIdx;
    this.data = data;
    this.inBag = inBag;
  }
  
  
  /** Determine predictions for a single instance (defined in "instanceIdx"). */
  public Double call() throws Exception{

    double[] classProbs = null;

    classProbs = new double[data.numClasses];

    int numVotes = 0;
    
    for (int treeIdx = 0; treeIdx < m_Classifiers.length; treeIdx++){

      if ( inBag[treeIdx][instanceIdx] ) {
        continue;
      }

      numVotes++;
      
      FastRandomTree aTree;
      if ( m_Classifiers[treeIdx] instanceof FastRandomTree)
        aTree = (FastRandomTree) m_Classifiers[treeIdx];
      else
        throw new IllegalArgumentException("Only FastRandomTrees accepted in the VotesCollector.");

      double[] curDist;
      curDist = aTree.distributionForInstanceInDataCache(data, instanceIdx);

      for(int classIdx = 0; classIdx < curDist.length; classIdx++) {
        classProbs[classIdx] += curDist[classIdx];
      }

    }

    double vote;
    //if(regression)
    //  vote = regrValue / numVotes;         // average - for regression
    //else
    vote = Utils.maxIndex(classProbs);   // consensus - for classification

    return vote;
  }
}
