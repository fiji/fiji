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
 *    FastRandomTree.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, NZ (original code,
 *      RandomTree.java)
 *    Copyright (C) 2009 Fran Supek (adapted code)
 */

package hr.irb.fastRandomForest;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import java.util.Random;
import weka.core.RevisionUtils;


/**
 * Based on the "weka.classifiers.trees.RandomTree" class, revision 1.19,
 * by Eibe Frank and Richard Kirkby, with major modifications made to improve
 * the speed of classifier training.
 * 
 * Please refer to the Javadoc of buildTree, splitData and distribution
 * function, as well as the changelog.txt, for the details of changes to 
 * FastRandomTree.
 * 
 * This class should be used only from within the FastRandomForest classifier.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) - original code
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz) - original code
 * @author Fran Supek (fran.supek[AT]irb.hr) - adapted code
 * @version Adapted by Ignacio Arganda-Carreras (iarganda at mit.edu) to work on Weka 3.7
 */
class FastRandomTree
        extends AbstractClassifier
        implements OptionHandler, WeightedInstancesHandler, Runnable {

  /** for serialization */
  static final long serialVersionUID = 8934314652175299375L;
  
  /** The subtrees appended to this tree. */
  protected FastRandomTree[] m_Successors;
  protected FastRandomForest m_MotherForest;

  /** The attribute to split on. */
  protected int m_Attribute = -1;

  /** The split point. */
  protected double m_SplitPoint = Double.NaN;
  
  /** The proportions of training instances going down each branch. */
  protected double[] m_Prop = null;

  /** Class probabilities from the training vals. */
  protected double[] m_ClassProbs = null;

  /** The dataset used for training. */
  protected transient DataCache data = null;

  /** Minimum number of instances for leaf. */
  protected static final int m_MinNum = 1;

  /**
   * Get the value of MinNum.
   *
   * @return Value of MinNum.
   */
  public final int getMinNum() {

    return m_MinNum;
  }


  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String KValueTipText() {
    return "Sets the number of randomly chosen attributes.";
  }


  /**
   * Get the value of K.
   *
   * @return Value of K.
   */
  public final int getKValue() {
    return m_MotherForest.m_KValue;
  }


  /**
   * Get the maximum depth of the tree, 0 for unlimited.
   *
   * @return 		the maximum depth.
   */
  public final int getMaxDepth() {
    return m_MotherForest.m_MaxDepth;
  }


  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }


  /**
   * This function is not supported by FastRandomTree, as it requires a
   * DataCache for training.

   * @throws Exception every time this function is called
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    throw new Exception("FastRandomTree can be used only by FastRandomForest " +
            "and FastRfBagger classes, not directly.");
  }



  /**
   * Builds classifier. Makes the initial call to the recursive buildTree 
   * function. The name "run()" is used to support multithreading via an
   * ExecutorService. <p>
   *
   * The "data" field of the FastRandomTree should contain a
   * reference to a DataCache prior to calling this function, and that
   * DataCache should have the "reusableRandomGenerator" field initialized.
   * The FastRfBagging class normally takes care of this before invoking this
   * function.
   */
  public void run() {

    // compute initial class counts
    double[] classProbs = new double[data.numClasses];
    for (int i = 0; i < data.numInstances; i++) {
      classProbs[data.instClassValues[i]] += data.instWeights[i];
    }

    // create the attribute indices window - skip class
    int[] attIndicesWindow = new int[data.numAttributes - 1];
    int j = 0;
    for (int i = 0; i < attIndicesWindow.length; i++) {
      if (j == data.classIndex)
        j++; // do not include the class
      attIndicesWindow[i] = j++;
    }

    // prepare the DataCache by:
    // ... creating an array for the whatGoesWhere field of the data
    // ... creating the sortedIndices
    data.whatGoesWhere = new int[ data.inBag.length ];
    data.createInBagSortedIndices();

    buildTree(data.sortedIndices, classProbs, m_Debug,
            attIndicesWindow, 0);

    this.data = null;
      
  }

  

  /**
   * Computes class distribution of an instance using the FastRandomTree.<p>
   *
   * In Weka's RandomTree, the distributions were normalized so that all
   * probabilities sum to 1; this would abolish the effect of instance weights
   * on voting. In FastRandomForest 0.97 onwards, the distributions are
   * normalized by dividing with the number of instances going into a leaf.<p>
   * 
   * @param instance the instance to compute the distribution for
   * @return the computed class distribution
   * @throws Exception if computation fails
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] returnedDist = null;

    if (m_Attribute > -1) {  // ============================ node is not a leaf

      if (instance.isMissing(m_Attribute)) {  // ---------------- missing value

        returnedDist = new double[m_MotherForest.m_Info.numClasses()];
        // split instance up
        for (int i = 0; i < m_Successors.length; i++) {
          double[] help = m_Successors[i].distributionForInstance(instance);
          if (help != null) {
            for (int j = 0; j < help.length; j++) {
              returnedDist[j] += m_Prop[i] * help[j];
            }
          }
        }

      } else if (m_MotherForest.m_Info
              .attribute(m_Attribute).isNominal()) { // ------ nominal

        returnedDist = m_Successors[(int) instance.value(m_Attribute)]
                .distributionForInstance(instance);

      } else { // ------------------------------------------ numeric attributes

        if (instance.value(m_Attribute) < m_SplitPoint) {
          returnedDist = m_Successors[0].distributionForInstance(instance);
        } else {
          returnedDist = m_Successors[1].distributionForInstance(instance);
        }
      }

      return returnedDist;

    } else { // =============================================== node is a leaf

      return m_ClassProbs;

    }

  }


  
  /**
   * Recursively generates a tree. A derivative of the buildTree function from
   * the "weka.classifiers.trees.RandomTree" class, with the following changes
   * made:
   * <ul>
   *
   * <li>m_ClassProbs are now remembered only in leaves, not in every node of
   *     the tree
   *
   * <li>m_Distribution has beed removed
   *
   * <li>members of dists, splits, props and vals arrays which are not used are
   *     dereferenced prior to recursion to reduce memory requirements
   *
   * <li>a check for "branch with no training instances" is now (FastRF 0.98)
   *     made before recursion; with the current implementation of splitData(),
   *     empty branches can appear only with nominal attributes with more than
   *     two categories
   *
   * <li>each new 'tree' (i.e. node or leaf) is passed a reference to its
   *     'mother forest', necessary to look up parameters such as maxDepth and K
   *
   * <li>pre-split entropy is not recalculated unnecessarily
   *
   * <li>uses DataCache instead of weka.core.Instances, the reference to the
   *     DataCache is stored as a field in FastRandomTree class and not passed
   *     recursively down new buildTree() calls
   *
   * <li>similarly, a reference to the random number generator is stored
   *     in a field of the DataCache
   *
   * <li>m_ClassProbs are now normalized by dividing with number of instances
   *     in leaf, instead of forcing the sum of class probabilities to 1.0;
   *     this has a large effect when class/instance weights are set by user
   *
   * <li>a little imprecision is allowed in checking whether there was a
   *     decrease in entropy after splitting
   * 
   * </ul>
   * 
   * @param sortedIndices the indices of the instances
   * @param classProbs the class distribution
   * @param debug whether debugging is on
   * @param attIndicesWindow the attribute window to choose attributes from
   * @param depth the current depth
   */
  protected void buildTree(int[][] sortedIndices,
          double[] classProbs,
          boolean debug,
          int[] attIndicesWindow,
          int depth)  {

    m_Debug = debug;

    // Check if node doesn't contain enough instances or is pure 
    // or maximum depth reached, make leaf.
    if ( ( sortedIndices.length > 0  &&  sortedIndices[0].length < Math.max(2, getMinNum()) )  // small
            || Utils.eq( classProbs[Utils.maxIndex(classProbs)], Utils.sum(classProbs) )       // pure
            || ( (getMaxDepth() > 0)  &&  (depth >= getMaxDepth()) )                           // deep
            ) {

      m_Attribute = -1;
      
      // normalize by dividing with the number of instances (as of ver. 0.97)
      // unless leaf is empty - this can happen with splits on nominal
      // attributes with more than two categories
      if ( sortedIndices[0].length != 0 )
        for (int c = 0; c < classProbs.length; c++) {
          classProbs[c] /= sortedIndices[0].length;
        }
      m_ClassProbs = classProbs;
      this.data = null;
      return;
    }
    
    double[] vals = new double[data.numAttributes]; // value of splitting criterion for each attribute
    double[][][] dists = new double[data.numAttributes][][];  // class distributions for each attribute 
    double[][] props = new double[data.numAttributes][];
    double[] splits = new double[data.numAttributes];

    // Investigate K random attributes
    int attIndex = 0;
    int windowSize = attIndicesWindow.length;
    int k = getKValue();
    boolean sensibleSplitFound = false;
    double prior = Double.NaN;
    while ((windowSize > 0) && (k-- > 0 || !sensibleSplitFound ) ) {

      int chosenIndex = data.reusableRandomGenerator.nextInt(windowSize);
      attIndex = attIndicesWindow[chosenIndex];

      // shift chosen attIndex out of window
      attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize - 1];
      attIndicesWindow[windowSize - 1] = attIndex;
      windowSize--;

      splits[attIndex] = distribution( props, dists,
              attIndex, sortedIndices[attIndex] );

      if ( Double.isNaN(prior) ) // needs to be computed only once per branch
        prior = SplitCriteria.entropyOverColumns(dists[attIndex]);
      
      double posterior = SplitCriteria.entropyConditionedOnRows(dists[attIndex]);
      vals[attIndex] = prior - posterior;  // we want the greatest reduction in entropy

      if ( vals[attIndex] > 1e-2 )   // we allow some leeway here to compensate
        sensibleSplitFound = true;   // for imprecision in entropy computation
      
    }

    if ( sensibleSplitFound ) { 

      m_Attribute = Utils.maxIndex(vals);   // find best attribute

      m_SplitPoint = splits[m_Attribute]; 
      m_Prop = props[m_Attribute];
      double[][] chosenAttDists = dists[m_Attribute]; // remember dist for most important attribute
      dists = null;                                   // other dists can be GC'd
      splits = null; props = null; vals = null;       // mark for GC before recursion happens
      
      int[][][] subsetIndices =
              new int[chosenAttDists.length][data.numAttributes][];
      splitData( subsetIndices, m_Attribute,
              m_SplitPoint, sortedIndices );
      //int numInstancesBeforeSplit = sortedIndices[0].length;
      

      m_Successors = new FastRandomTree[chosenAttDists.length];
      for (int i = 0; i < chosenAttDists.length; i++) {
        m_Successors[i] = new FastRandomTree();
        m_Successors[i].m_MotherForest = this.m_MotherForest;
        m_Successors[i].data = this.data;

        // check if we're about to make an empty branch - this can happen with
        // nominal attributes with more than two categories (as of ver. 0.98)
        if ( subsetIndices[i][0].length == 0  ) {
            // in this case, modify the chosenAttDists[i] so that it contains
            // the current, before-split class probabilities, properly normalized
            // by the number of instances (as we won't be able to normalize
            // after the split)
            for ( int j = 0; j < chosenAttDists[i].length; j++ )
              chosenAttDists[i][j] = classProbs[j] / sortedIndices[0].length;
        }


        m_Successors[i].buildTree(subsetIndices[i], 
                chosenAttDists[i], m_Debug,
                attIndicesWindow, depth + 1);

        subsetIndices[i] = null;
        chosenAttDists[i] = null;
        
      }
      sortedIndices = null;

    } else { // Make leaf

      m_Attribute = -1;
      
      // normalize by dividing with the number of instances (as of ver. 0.97)
      // unless leaf is empty - this can happen with splits on nominal attributes
      if ( sortedIndices[0].length != 0 )
        for (int c = 0; c < classProbs.length; c++) {
          classProbs[c] /= sortedIndices[0].length;
        }

      m_ClassProbs = classProbs;
      
    }

    this.data = null; // dereference all pointers so data can be GC'd after tree is built
    
  }



  /**
   * Computes size of the tree.
   * 
   * @return the number of nodes
   */
  public int numNodes() {

    if (m_Attribute == -1) {
      return 1;
    } else {
      int size = 1;
      for (int i = 0; i < m_Successors.length; i++) {
        size += m_Successors[i].numNodes();
      }
      return size;
    }
  }



  /**
   * Splits instances into subsets. This is a derivative of the splitData
   * function from "weka.classifiers.trees.RandomTree", with the following
   * changes: <p>
   *
   * - When handling instances with missing values in attribute chosen for the
   * split, the FastRandomTree assignes the instance to one of the branches at 
   * random, with bigger branches having a higher probability of getting the
   * instance. <p>
   *
   * - When splitting sortedIndices into two or more subsetIndices,
   * FastRandomTree checks whether an instance's split attribute value was above 
   * splitpoint only once per instances, and stores result into the DataCache's
   * whatGoesWhere field, which is then read in splitting subsetIndices. <p>
   * 
   * As a consequence of the above points, the exact branch sizes (even with
   * instances having unknowns in the split attribute) are known in advance so
   * subsetIndices arrays don't have to be 'resized' (i.e. a new shorter copy
   * of each one created and the old one GCed). <p>
   *
   * @param subsetIndices the sorted indices of the subset
   * @param att the attribute index
   * @param splitPoint the splitpoint for numeric attributes
   * @param sortedIndices the sorted indices of the whole set
   */
  protected void splitData( int[][][] subsetIndices,
          int att, double splitPoint,
          int[][] sortedIndices ) {

    Random random = data.reusableRandomGenerator;
    int j;
    int[] num; // how many instances go to each branch

    if ( data.isAttrNominal(att) ) { // ============================ if nominal

      num = new int[ data.attNumVals[att] ];

      for (j = 0; j < sortedIndices[att].length; j++) {
        
        int inst = sortedIndices[att][j];

        if ( data.isValueMissing(att, inst) ) { // ---------- has missing value

          // decide where to put this instance randomly, with bigger branches
          // getting a higher chance
          double rn = random.nextDouble();
          int myBranch = -1;
          for (int k = 0; k < m_Prop.length; k++) {
            rn -= m_Prop[k];
            if ( (rn <= 0) || k == (m_Prop.length-1) ) {
              myBranch = k;
              break;
            }
          }

          data.whatGoesWhere[ inst ] = myBranch;
          num[myBranch]++;

        } else { // ----------------------------- does not have missing value

          int subset = (int) data.vals[att][inst];
          data.whatGoesWhere[ inst ] = subset;
          num[subset]++;

        } // --------------------------------------- end if has missing value

      }

    } else { // =================================================== if numeric

      num = new int[2];

      for (j = 0; j < sortedIndices[att].length; j++) {
        
        int inst = sortedIndices[att][j];
        
        //Instance inst = data.instance(sortedIndices[att][j]);

        if ( data.isValueMissing(att, inst) ) { // ---------- has missing value

          // for numeric attributes, ALWAYS num.length == 2
          // decide if instance goes into subset 0 or 1 randomly,
          // with bigger subsets having a greater probability of getting
          // the instance assigned to them
          // instances with missing values get processed LAST (sort order)
          // so branch sizes are known by now (and stored in m_Prop)
          double rn = random.nextDouble();
          int branch = ( rn > m_Prop[0] ) ? 1 : 0;
          data.whatGoesWhere[ inst ] = branch;
          num[ branch ]++;

        } else { // ----------------------------- does not have missing value

          int branch = ( data.vals[att][inst] < splitPoint ) ? 0 : 1;
          
          data.whatGoesWhere[ inst ] = branch;
          num[ branch ]++;

        } // --------------------------------------- end if has missing value

      } // end for instance by instance

    }  // ============================================ end if nominal / numeric




    for (int a = 0; a < data.numAttributes; a++) {

      if ( a == data.classIndex )
        continue;
      
      // create the new subset (branch) arrays of correct size
      for (int branch = 0; branch < num.length; branch++) {
        subsetIndices[branch][a] = new int[num[branch]];
      }
      
    }
  
    for (int a = 0; a < data.numAttributes; a++) { // xxxxxxxxxx attr by attr
      
      if (a == data.classIndex)
        continue;
      for (int branch = 0; branch < num.length; branch++) {
        num[branch] = 0;
      }
      
      // fill them with stuff by looking at goesWhere array
      for (j = 0; j < sortedIndices[ a ].length; j++) {
        
        int inst = sortedIndices[ a ][j];
        int branch = data.whatGoesWhere[ inst ];
        
        subsetIndices[ branch ][ a ][ num[branch] ] = sortedIndices[a][j];
        num[branch]++;
        
      }

    } // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx end for attr by attr
    
  }

  
  

  /**
   * Computes class distribution for an attribute. Based on the splitData
   * function from "weka.classifiers.trees.RandomTree", with the following
   * changes:<ul>
   * 
   * <li>entropy pre-split is not computed at this point as the only thing
   *     relevant for the (comparative) goodness of a split is entropy after splitting
   * <li>dist[][] is now computed only after the split point has been found,
   *     and not updated continually by copying from currDist
   * <li>also, in Weka's RandomTree it was possible to create a split 'in the
   *     middle' of instance 0, which would result in empty nodes after the
   *     split; this is now fixed
   * <li>instance 0 is now generally skipped when looking for split points,
   *     as the split point 'before instance 0' is not sensible; in versions
   *     prior to 0.96 this change introduced a bug where attributes with
   *     all missing values had their dists computed wrongly, which might
   *     result in useless (but harmless) branches being added to the tree
   * </ul>
   * 
   * @param props gets filled with relative sizes of branches (total = 1)
   * @param dists
   * @param att the attribute index
   * @param sortedIndices the sorted indices of the vals
   */
  protected double distribution( double[][] props, double[][][] dists,
          int att, int[] sortedIndices ) {

    double splitPoint = -Double.MAX_VALUE;
    double[][] dist = null;
    int i;  
    
    if ( data.isAttrNominal(att) ) { // ====================== nominal attributes

      dist = new double[data.attNumVals[att]][data.numClasses];
      for (i = 0; i < sortedIndices.length; i++) {
        int inst = sortedIndices[i];
        if ( data.isValueMissing(att, inst) )
          break;
        dist[ (int)data.vals[att][inst] ][ data.instClassValues[inst] ] += data.instWeights[inst];        
      }

      splitPoint = 0; // signals we've found a sensible split point; by
                      // definition, a split on a nominal attribute is sensible
      
    } else { // ============================================ numeric attributes

      double[][] currDist = new double[2][data.numClasses];
      dist = new double[2][data.numClasses];

      //begin with moving all instances into second subset
      for (int j = 0; j < sortedIndices.length; j++) {
        int inst = sortedIndices[j];
        if ( data.isValueMissing(att, inst) ) 
          break;
        currDist[1][ data.instClassValues[inst] ] += data.instWeights[inst]; 
      }
      
      for (int j = 0; j < currDist.length; j++) 
        System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);

      double currVal = -Double.MAX_VALUE; // current value of splitting criterion 
      double bestVal = -Double.MAX_VALUE; // best value of splitting criterion
      int bestI = 0; // the value of "i" BEFORE which the splitpoint is placed

      for (i = 1; i < sortedIndices.length; i++) {  // --- try all split points

        int inst = sortedIndices[i];
        if ( data.isValueMissing(att, inst) ) 
          break;

        int prevInst = sortedIndices[i-1];

        currDist[0][ data.instClassValues[ prevInst ] ]
                += data.instWeights[ prevInst ] ;
        currDist[1][ data.instClassValues[ prevInst ] ]
                -= data.instWeights[ prevInst ] ;        
        
        // do not allow splitting between two instances with the same value
        if ( data.vals[att][inst] > data.vals[att][prevInst] ) {

          // we want the lowest impurity after split; at this point, we don't
          // really care what we've had before spliting
          currVal = -SplitCriteria.entropyConditionedOnRows(currDist);          
          
          if (currVal > bestVal) {
            bestVal = currVal;
            bestI = i;
          }
          
        }

      }                                             // ------- end split points

      /*
       * Determine the best split point:
       * bestI == 0 only if all instances had missing values, or there were
       * less than 2 instances; splitPoint will remain set as -Double.MAX_VALUE. 
       * This is not really a useful split, as all of the instances are 'below'
       * the split line, but at least it's formally correct. And the dists[]
       * also has a default value set previously.
       */
      if ( bestI > 0 ) { // ...at least one valid splitpoint was found

        int instJustBeforeSplit = sortedIndices[bestI-1];
        int instJustAfterSplit = sortedIndices[bestI];
        splitPoint = ( data.vals[ att ][ instJustAfterSplit ]
                + data.vals[ att ][ instJustBeforeSplit ] ) / 2.0;
        
        // Now make the correct dist[] from the default dist[] (all instances
        // in the second branch, by iterating through instances until we reach
        // bestI, and then stop.
        for ( int ii = 0; ii < bestI; ii++ ) {
          int inst = sortedIndices[ii];
          dist[0][ data.instClassValues[ inst ] ] += data.instWeights[ inst ] ;
          dist[1][ data.instClassValues[ inst ] ] -= data.instWeights[ inst ] ;                  
        }
        
      }      
            
    } // ================================================== nominal or numeric?

    // compute total weights for each branch (= props)
    props[att] = countsToFreqs(dist);

    // distribute counts of instances with missing values

    // ver 0.96 - check for special case when *all* instances have missing vals
    if ( data.isValueMissing(att, sortedIndices[0]) )
      i = 0;

    while (i < sortedIndices.length) {
      int inst = sortedIndices[i];
      for (int branch = 0; branch < dist.length; branch++) {
        dist[ branch ][ data.instClassValues[inst] ]
                += props[ att ][ branch ] * data.instWeights[ inst ] ;
      }
      i++;
    }

    // return distribution after split and best split point
    dists[att] = dist;
    return splitPoint;
    
  }



  /**
   * Normalizes branch sizes so they contain frequencies (stored in "props")
   * instead of counts (stored in "dist").
   */  
  protected static double[] countsToFreqs( double[][] dist ) {
    
    double[] props = new double[dist.length];
    
    for (int k = 0; k < props.length; k++) {
      props[k] = Utils.sum(dist[k]);
    }
    if (Utils.eq(Utils.sum(props), 0)) {
      for (int k = 0; k < props.length; k++) {
        props[k] = 1.0 / (double) props.length;
      }
    } else {
      FastRfUtils.normalize(props);
    }
    return props;
  }



  /**
   * Main method for this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {
    runClassifier(new FastRandomTree(), argv);
  }



  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 0.98$");
  }


  
}

