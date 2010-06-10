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
 *    FastRfBagging.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, NZ (original code,
 *      Bagging.java )
 *    Copyright (C) 2009 Fran Supek (adapted code)
 */

package hr.irb.fastRandomForest;

import java.util.ArrayList;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.core.AdditionalMeasureProducer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import weka.classifiers.Classifier;
import weka.core.RevisionUtils;


/** 
 * Based on the "weka.classifiers.meta.Bagging" class, revision 1.39,
 * by Kirkby, Frank and Trigg, with modifications:
 * <ul>
 * 
 * <li>Instead of Instances, produces DataCaches; consequently, FastRfBagging
 *     is compatible only with FastRandomTree as base classifier
 *
 * <li>The function for resampling the data is removed; this is a responsibility
 *     of the DataCache objects now
 *
 * <li>Not a TechnicalInformationHandler anymore
 *
 * <li>The classifiers are trained in separate "tasks" which are handled by
 *     an ExecutorService (the FixedThreadPool) which runs the tasks in
 *     more threads in parallel. If the number of threads is not specified,
 *     it will be set automatically to the available number of cores.
 *
 * <li>Estimating the out-of-bag (OOB) error is also multithreaded, using
 *     the VotesCollector class
 *
 * <li>OOB estimation in Weka's Bagging is one tree - one vote. In FastRF 0.97
 *     onwards, some trees will have a heavier weight in the overall vote
 *     depending on the averaged weights of instances that ended in the specific
 *     leaf.
 *
 * </ul>
 * This class should be used only from within the FastRandomForest classifier.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) - original code
 * @author Len Trigg (len@reeltwo.com) - original code
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz) - original code
 * @author Fran Supek (fran.supek[AT]irb.hr) - adapted code
 * @version $Revision: 0.98$
 */
class FastRfBagging extends RandomizableIteratedSingleClassifierEnhancer 
  implements WeightedInstancesHandler, AdditionalMeasureProducer {
  
  /** for serialization */
  static final long serialVersionUID = -505879962237199702L;
  
  /**
   * Bagging method. Produces DataCache objects with bootstrap samples of
   * the original data, and feeds them to the base classifier (which can only
   * be a FastRandomTree).
   *
   * @param data The training set to be used for generating the
   * bagged classifier.
   * @param numThreads The number of simultaneous threads to use for
   * computation. Pass zero (0) for autodetection.
   * @param motherForest A reference to the FastRandomForest object that
   * invoked this.
   * @throws Exception if the classifier could not be built successfully
   */ 
  public void buildClassifier(Instances data, int numThreads,
          FastRandomForest motherForest) throws Exception {

    // can classifier handle the vals?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    //data = new Instances(data); // not sure this line is necessary, it duplicates the data
    data.deleteWithMissingClass();

    if ( ! (m_Classifier instanceof FastRandomTree) )
        throw new IllegalArgumentException("The FastRfBagging class accepts " +
                "only FastRandomTree as its base classifier.");

    /* We fill the m_Classifiers array by creating lots of trees with new()
     * because this is much faster than using serialization to deep-copy the
     * one tree in m_Classifier - this is what the super.buildClassifier(data)
     * normally does. */
    m_Classifiers = new Classifier[m_NumIterations];
    for ( int i = 0; i < m_Classifiers.length; i++ ) {
      FastRandomTree curTree = new FastRandomTree();
      // all parameters for training will be looked up in the motherForest (maxDepth, k_Value)
      curTree.m_MotherForest = motherForest;
      m_Classifiers[i] = curTree;
    }

    // this was SLOW.. takes approx 1/2 time as training the forest afterwards (!!!)
    // super.buildClassifier(data);
    
    if (m_CalcOutOfBag && (m_BagSizePercent != 100)) {
      throw new IllegalArgumentException("Bag size needs to be 100% if " +
					 "out-of-bag error is to be calculated!");
    }
    
    
    // sorting is performed inside this constructor
    DataCache myData = new DataCache(data);

    int bagSize = data.numInstances() * m_BagSizePercent / 100;
    Random random = new Random(m_Seed);

    boolean[][] inBag = new boolean[m_Classifiers.length][];

    // thread management
    ExecutorService threadPool = Executors.newFixedThreadPool(
            numThreads > 0 ? numThreads : Runtime.getRuntime().availableProcessors() );
    List< Future<?> > futures =
            new ArrayList< Future<?> >( m_Classifiers.length );

    try {

      for (int treeIdx = 0; treeIdx < m_Classifiers.length; treeIdx++) {

        // create the in-bag dataset (and be sure to remember what's in bag)
        // for computing the out-of-bag error later
        DataCache bagData = myData.resample(bagSize, random);
        bagData.reusableRandomGenerator = bagData.getRandomNumberGenerator(
                random.nextInt());
        inBag[treeIdx] = bagData.inBag;

        // build the classifier
        if (m_Classifiers[treeIdx] instanceof FastRandomTree) {

          FastRandomTree aTree = (FastRandomTree) m_Classifiers[treeIdx];
          aTree.data = bagData;

          Future<?> future = threadPool.submit((Runnable) aTree);
          futures.add(future);

        } else {
          throw new IllegalArgumentException("The FastRfBagging class accepts " +
                  "only FastRandomTree as its base classifier.");
        }

      }

      // make sure all trees have been trained before proceeding
      for (int treeIdx = 0; treeIdx < m_Classifiers.length; treeIdx++) {
        futures.get(treeIdx).get();

      }

      // calc OOB error?
      if (getCalcOutOfBag()) {

        boolean numeric = data.classAttribute().isNumeric();

        List<Future<Double>> votes =
                new ArrayList<Future<Double>>(data.numInstances());
        for (int i = 0; i < data.numInstances(); i++) {
          VotesCollector aCollector = new VotesCollector(m_Classifiers, i, data, inBag);
          votes.add(threadPool.submit(aCollector));
        }


        double outOfBagCount = 0.0;
        double errorSum = 0.0;

        for (int i = 0; i < data.numInstances(); i++) {

          double vote = votes.get(i).get();

          // error for instance
          outOfBagCount += data.instance(i).weight();
          if (numeric) {
            errorSum += StrictMath.abs(vote - data.instance(i).classValue()) * data.instance(i).weight();
          } else {
            if (vote != data.instance(i).classValue())
              errorSum += data.instance(i).weight();
          }

        }

        m_OutOfBagError = errorSum / outOfBagCount;

      } else {

        m_OutOfBagError = 0;

      }

      threadPool.shutdown();

    } finally {

      threadPool.shutdownNow();

    }

  }  
  

  /**
   * Not supported.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    throw new Exception("FastRfBagging can be built only from within a FastRandomForest.");
  }

  
  /** The size of each bag sample, as a percentage of the training size */
  protected int m_BagSizePercent = 100;

  /** Whether to calculate the out of bag error */
  protected boolean m_CalcOutOfBag = true;

  /** The out of bag error that has been calculated */
  protected double m_OutOfBagError;  
    
  /**
   * Constructor.
   */
  public FastRfBagging() {
    
    m_Classifier = new hr.irb.fastRandomForest.FastRandomTree();
  }


  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
 
    return "Class for bagging a classifier to reduce variance. Can do classification "
      + "and regression depending on the base learner. \n\n";
  }


  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {
    return "hr.irb.fastRandomForest.FastRfTree";
  }


  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
              "\tSize of each bag, as a percentage of the\n" 
              + "\ttraining set size. (default 100)",
              "P", 1, "-P"));
    newVector.addElement(new Option(
              "\tCalculate the out of bag error.",
              "O", 0, "-O"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -P
   *  Size of each bag, as a percentage of the
   *  training set size. (default 100)</pre>
   * 
   * <pre> -O
   *  Calculate the out of bag error.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: fastRandomForest.classifiers.FastRandomTree)</pre>
   *
   * <!-- options-end -->
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String bagSize = Utils.getOption('P', options);
    if (bagSize.length() != 0) {
      setBagSizePercent(Integer.parseInt(bagSize));
    } else {
      setBagSizePercent(100);
    }

    setCalcOutOfBag(Utils.getFlag('O', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String [] getOptions() {


    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 3];

    int current = 0;
    options[current++] = "-P"; 
    options[current++] = "" + getBagSizePercent();

    if (getCalcOutOfBag()) { 
      options[current++] = "-O";
    }

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String bagSizePercentTipText() {
    return "Size of each bag, as a percentage of the training set size.";
  }

  /**
   * Gets the size of each bag, as a percentage of the training set size.
   *
   * @return the bag size, as a percentage.
   */
  public int getBagSizePercent() {

    return m_BagSizePercent;
  }
  
  /**
   * Sets the size of each bag, as a percentage of the training set size.
   *
   * @param newBagSizePercent the bag size, as a percentage.
   */
  public void setBagSizePercent(int newBagSizePercent) {

    m_BagSizePercent = newBagSizePercent;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String calcOutOfBagTipText() {
    return "Whether the out-of-bag error is calculated.";
  }

  /**
   * Set whether the out of bag error is calculated.
   *
   * @param calcOutOfBag whether to calculate the out of bag error
   */
  public void setCalcOutOfBag(boolean calcOutOfBag) {

    m_CalcOutOfBag = calcOutOfBag;
  }

  /**
   * Get whether the out of bag error is calculated.
   *
   * @return whether the out of bag error is calculated
   */
  public boolean getCalcOutOfBag() {

    return m_CalcOutOfBag;
  }

  /**
   * Gets the out of bag error that was calculated as the classifier
   * was built.
   *
   * @return the out of bag error 
   */
  public double measureOutOfBagError() {
    
    return m_OutOfBagError;
  }
  
  /**
   * Returns an enumeration of the additional measure names.
   *
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    
    Vector newVector = new Vector(1);
    newVector.addElement("measureOutOfBagError");
    return newVector.elements();
  }
  
  /**
   * Returns the value of the named measure.
   *
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    
    if (additionalMeasureName.equalsIgnoreCase("measureOutOfBagError")) {
      return measureOutOfBagError();
    }
    else {throw new IllegalArgumentException(additionalMeasureName 
					     + " not supported (Bagging)");
    }
  }

  
  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @throws Exception if distribution can't be computed successfully 
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] sums = new double[instance.numClasses()], newProbs; 
    
    for (int i = 0; i < m_NumIterations; i++) {
      if ( instance.classAttribute().isNumeric() ) {
        sums[0] += m_Classifiers[i].classifyInstance(instance);
      } else {
        newProbs = m_Classifiers[i].distributionForInstance(instance);
        for (int j = 0; j < newProbs.length; j++)
          sums[j] += newProbs[j];
      }
    }

    if ( instance.classAttribute().isNumeric() ) {
      sums[0] /= (double)m_NumIterations;
      return sums;
    } else if (Utils.eq(Utils.sum(sums), 0)) {
      return sums;
    } else {
      Utils.normalize(sums);
      return sums;
    }
    
  }

  /**
   * Returns description of the bagged classifier.
   *
   * @return description of the bagged classifier as a string
   */
  @Override
  public String toString() {
    
    if (m_Classifiers == null) {
      return "FastRfBagging: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("All the base classifiers: \n\n");
    for (int i = 0; i < m_Classifiers.length; i++)
      text.append(m_Classifiers[i].toString() + "\n\n");
    
    if (m_CalcOutOfBag) {
      text.append("Out of bag error: "
		  + Utils.doubleToString(m_OutOfBagError, 4)
		  + "\n\n");
    }

    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new FastRfBagging(), argv);
  }

  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 0.98$");
  }
  
}
  
  
  
  
  
  
  
  
  
  
  
  

