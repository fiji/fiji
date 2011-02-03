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
 *    FastRandomForest.java
 *    Copyright (C) 2009 Fran Supek
 */

package hr.irb.fastRandomForest;

import java.util.Arrays;
import java.util.Random;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Stores a dataset that in FastRandomTrees use for training. The data points
 * are stored in a single-precision array indexed by attribute first, and then
 * by instance, to make access by FastRandomTrees faster. 
 * 
 * Also stores the sorted order of the instances by any attribute, can create
 * bootstrap samples, and seed a random number generator from the stored data.
 * 
 * @author Fran Supek (fran.supek[AT]irb.hr)
 */
public class DataCache {

  /** The dataset, first indexed by attribute, then by instance. */
  protected final float[][] vals;

  /**
   * Attribute description - holds a 0 for numeric attributes, and the number
   * of available categories for nominal attributes.
   */
  protected final int[] attNumVals;

  /** Numeric index of the class attribute. */
  protected final int classIndex;

  /** Number of attributes, including the class attribute. */
  protected final int numAttributes;

  /** Number of classes. */
  protected final int numClasses;

  /** Number of instances. */
  protected final int numInstances;
  
  /** The class an instance belongs to. */
  protected final int[] instClassValues;

  /** Ordering of instances, indexed by attribute, then by instance. */ 
  protected int[][] sortedIndices;
  
  /** Weights of instances. */
  protected double[] instWeights;
  
  /** Is instance in 'bag' created by bootstrap sampling. */
  protected boolean[] inBag = null;
  /** How many instances are in 'bag' created by bootstrap sampling. */
  protected int numInBag = 0;

  /** Used in training of FastRandomTrees. */
  protected int[] whatGoesWhere = null;
  
  /**
   * Used in training of FastRandomTrees. Each tree can store its own
   * custom-seeded random generator in this field.
   */
  protected Random reusableRandomGenerator = null;



  /**
   * Creates a DataCache by copying data from a weka.core.Instances object.
   */
  public DataCache(Instances origData) throws Exception {

    classIndex = origData.classIndex();
    numAttributes = origData.numAttributes();
    numClasses = origData.numClasses();
    numInstances = origData.numInstances();

    attNumVals = new int[origData.numAttributes()];
    for (int i = 0; i < attNumVals.length; i++) {
      if (origData.attribute(i).isNumeric()) {
        attNumVals[i] = 0;
      } else if (origData.attribute(i).isNominal()) {
        attNumVals[i] = origData.attribute(i).numValues();
      } else
        throw new Exception("Only numeric and nominal attributes are supported.");
    }

    /* Array is indexed by attribute first, to speed access in RF splitting. */
    vals = new float[numAttributes][numInstances];
    for (int a = 0; a < numAttributes; a++) {
      for (int i = 0; i < numInstances; i++) {
        if (origData.instance(i).isMissing(a))
          vals[a][i] = Float.MAX_VALUE;
        else
          vals[a][i] = (float) origData.instance(i).value(a);  // deep copy
      }
    }

    instWeights = new double[numInstances];
    instClassValues = new int[numInstances];
    for (int i = 0; i < numInstances; i++) {
      instWeights[i] = origData.instance(i).weight();
      instClassValues[i] = (int) origData.instance(i).classValue();
    }

    /* compute the sortedInstances for the whole dataset */
    
    sortedIndices = new int[numAttributes][];

    for (int a = 0; a < numAttributes; a++) { // ================= attr by attr

      if (a == classIndex)
        continue;

      if (attNumVals[a] > 0) { // ------------------------------------- nominal

        // Handling nominal attributes. Putting indices of
        // instances with missing values at the end.
        
        sortedIndices[a] = new int[numInstances];
        int count = 0;

        for (int i = 0; i < numInstances; i++) {
          if ( !this.isValueMissing(a, i) ) {
            sortedIndices[a][count] = i;
            count++;
          }
        }

        for (int i = 0; i < numInstances; i++) {
          if ( this.isValueMissing(a, i) ) {
            sortedIndices[a][count] = i;
            count++;
          }
        }

      } else { // ----------------------------------------------------- numeric

        // Sorted indices are computed for numeric attributes
        // missing values are coded as Float.MAX_VALUE and go to the end
        sortedIndices[a] = FastRfUtils.sort(vals[a]); 

      } // ---------------------------------------------------------- attr kind

    } // ========================================================= attr by attr

    // System.out.println(" Done.");

  }

  
  
  /**
   * Makes a copy of a DataCache. Most array fields are shallow copied, with the
   * exception of in inBag and whatGoesWhere arrays, which are created anew.
   * 
   * @param origData
   */
  public DataCache(DataCache origData) {

    classIndex = origData.classIndex;       // copied
    numAttributes = origData.numAttributes; // copied
    numClasses = origData.numClasses;       // copied
    numInstances = origData.numInstances;   // copied

    attNumVals = origData.attNumVals;       // shallow copied
    instClassValues =
            origData.instClassValues;       // shallow copied
    vals = origData.vals;                   // shallow copied - very big array!
    sortedIndices = origData.sortedIndices; // shallow copied - also big

    instWeights = origData.instWeights;     // shallow copied

    inBag = new boolean[numInstances];      // gets its own inBag array
    numInBag = 0;
    
    whatGoesWhere = null;     // this will be created when tree building starts

  }

  
  
  /**
   * Uses sampling with replacement to create a new DataCache from an existing
   * one.
   * 
   * The probability of sampling a specific instance does not depend on its
   * weight. When an instance is sampled multiple times, its weight in the new
   * DataCache increases to a multiple of the original weight.
   * 
   * @param bagSize If this is equal to the DataCache.numInstances, makes a
   * a bootstrap sample (n of of in
   * @param random A random number generator.
   * @return a new DataCache - consult "DataCache(DataCache origData)"
   * constructor to see what's deep / shallow copied
   */
  public DataCache resample(int bagSize, Random random) {

    DataCache result =
            new DataCache(this); // makes shallow copy of vals matrix

    double[] newWeights = new double[ numInstances ]; // all 0.0 by default
             
    for ( int r = 0; r < bagSize; r++ ) {

    	int curIdx = random.nextInt( numInstances );
    	newWeights[curIdx] += instWeights[curIdx];
    	if ( !result.inBag[curIdx] ) {
    		result.numInBag++;
    		result.inBag[curIdx] = true;
    	}

    }
    
    result.instWeights = newWeights;

    // we also need to fill sortedIndices by peeking into the inBag array, but
    // this can be postponed until the tree training begins
    // we will use the "createInBagSortedIndices()" for this

    return result;

  }

  

  /** Invoked only when tree is trained. */
  protected void createInBagSortedIndices() {

	  // If the bag size is equal to the number of instances, there is
	  // no need to create the indices again.
	if(this.numInBag == this.numInstances)
		  return;

	  
    int[][] newSortedIndices = new int[ numAttributes ][ ];
    
    for (int a = 0; a < numAttributes; a++) {
      
      if (a == classIndex)
        continue;      
      
      newSortedIndices[a] = new int[this.numInBag];
      
      int inBagIdx = 0;
      for (int i = 0; i < sortedIndices[a].length; i++) {
        int origIdx = sortedIndices[a][i];
        if ( !this.inBag[origIdx] )
          continue;
        newSortedIndices[a][inBagIdx] = sortedIndices[a][i];
        inBagIdx++;

      }
      
    }    
    
    this.sortedIndices = newSortedIndices;

  }


  
  /** Does the given attribute - instance combination contain a missing value? */
  public final boolean isValueMissing( int attIndex, int instIndex ) {
    return this.vals[attIndex][instIndex] == Float.MAX_VALUE;
  }

  
  /** Is an attribute with the given index nominal? */
  public final boolean isAttrNominal( int attIndex ) {
    return attNumVals[attIndex] > 0;
  }
  
  
  /**
   * Returns a random number generator. The initial seed of the random
   * number generator depends on the given seed and the contents of the
   * sortedIndices array (a single attribute is picked, its sortedIndices
   * converted to String and a hashcode computed).
   *
   * @param seed the given seed
   * @return the random number generator
   */
  public Random getRandomNumberGenerator(long seed) {

    Random r = new Random(seed);
    long dataSignature
            = Arrays.toString( sortedIndices[ r.nextInt( numAttributes ) ] )
            .hashCode();
    r.setSeed( dataSignature + seed );
    return r;
    
  }
   
  
  
}

