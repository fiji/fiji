package ai;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Randomizable;
import weka.core.TechnicalInformation;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 * This class implements a Balanced Random Forest classifier,
 * it is an ensemble classifier of random trees where all classes
 * have the same representation in the training process. 
 * 
 * <!-- globalinfo-start -->
 * Class for constructing a balanced forest of random trees.<br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Leo Breiman (2001). Random Forests. Machine Learning. 45(1):5-32.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Breiman2001,
 *    author = {Leo Breiman},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {5-32},
 *    title = {Random Forests},
 *    volume = {45},
 *    year = {2001}
 * }
 * </pre> * 
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -I &lt;number of trees&gt;
 *  Number of trees to build.</pre>
 * 
 * <pre> -K &lt;number of features&gt;
 *  Number of features to consider (&lt;1=int(logM+1)).</pre>
 * 
 * <pre> -S
 *  Seed for random number generator.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Ignacio Arganda-Carreras (iarganda@mit.edu)
 *
 */
public class BalancedRandomForest extends AbstractClassifier implements Randomizable
{
	/** serial version ID */
	private static final long serialVersionUID = "BalancedRandomForest".hashCode();

	/** random seed */
	int seed = 1;
	
	/** number of trees */
	int numTrees = 10;
		
	/** number of features used on each node of the trees */
	int numFeatures = 0;
	
	/** array of random trees that form the forest */
	BalancedRandomTree[] tree = null;

	/** the out of bag error which has been calculated */
	double outOfBagError = 0;
	
	/**
	 * Returns a string describing classifier
	 * @return a description suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String globalInfo() 
	{
		return  
		"Class for constructing a balanced forest of random trees.\n\n"
		+ "For more information see: \n\n"
		+ getTechnicalInformation().toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing 
	 * detailed information about the technical background of this class,
	 * e.g., paper reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	public TechnicalInformation getTechnicalInformation() 
	{
		TechnicalInformation 	result;

		result = new TechnicalInformation(Type.ARTICLE);
		result.setValue(Field.AUTHOR, "Leo Breiman");
		result.setValue(Field.YEAR, "2001");
		result.setValue(Field.TITLE, "Random Forests");
		result.setValue(Field.JOURNAL, "Machine Learning");
		result.setValue(Field.VOLUME, "45");
		result.setValue(Field.NUMBER, "1");
		result.setValue(Field.PAGES, "5-32");

		return result;
	}

	
	
	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String numTreesTipText() 
	{
		return "The number of trees to be generated.";
	}
	
	
	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String numFeaturesTipText() 
	{
		return "The number of attributes to be used in random selection of each node.";
	}
	
	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String seedTipText() 
	{
		return "The random number seed to be used.";
	}
	
	
	/**
	 * Build Balanced Random Forest
	 */
	public void buildClassifier(final Instances data) throws Exception 
	{
		// If number of features is 0 then set it to log2 of M (number of attributes)
		if (numFeatures < 1) 
			numFeatures = (int) Utils.log2(data.numAttributes())+1;
		// Check maximum number of random features
		if (numFeatures >= data.numAttributes())
			numFeatures = data.numAttributes() - 1;
		
		// Initialize array of trees
		tree = new BalancedRandomTree[ numTrees ];
		
		// total number of instances
		final int numInstances = data.numInstances();
		// total number of classes
		final int numClasses = data.numClasses();
		
		final ArrayList<Integer>[] indexSample = new ArrayList[ numClasses ];
		for(int i = 0; i < numClasses; i++)
			indexSample[i] = new ArrayList<Integer>();
		
		//System.out.println("numClasses = " + numClasses);
		
		// fill indexSample with the indices of each class
		for(int i = 0 ; i < numInstances; i++)
		{
			//System.out.println("data.get("+i+").classValue() = " + data.get(i).classValue());
			indexSample[ (int) data.get(i).classValue() ].add( i );
		}
		
		final Random random = new Random(seed);
		
		// Executor service to run concurrent trees
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		List< Future<BalancedRandomTree> > futures =
            new ArrayList< Future<BalancedRandomTree> >( numTrees );

		
		final boolean[][] inBag = new boolean [ numTrees ][ numInstances ];
		
		try
		{
			for(int i = 0; i < numTrees; i++)
			{
				final ArrayList<Integer> bagIndices = new ArrayList<Integer>(); 

				// Randomly select the indices in a balanced way
				for(int j = 0 ; j < numInstances; j++)
				{
					// Select first the class
					final int randomClass = random.nextInt( numClasses );
					// Select then a random sample of that class
					final int randomSample = random.nextInt( indexSample[randomClass].size() );
					bagIndices.add( indexSample[ randomClass ].get( randomSample ) );
					inBag[ i ][ indexSample[ randomClass ].get( randomSample ) ] = true;
				}

				// Create random tree
				final Splitter splitter = 
					new Splitter(new GiniFunction(numFeatures, data.getRandomNumberGenerator( random.nextInt() ) ));

				futures.add(exe.submit(new Callable<BalancedRandomTree>() {
					public BalancedRandomTree call() {
						return new BalancedRandomTree( data, bagIndices, splitter );
					}
				}));
			}
			
			// Grab all trained trees before proceeding
			for (int treeIdx = 0; treeIdx < numTrees; treeIdx++) 
				tree[treeIdx] = futures.get(treeIdx).get();

			// Calculate out of bag error
			final boolean numeric = data.classAttribute().isNumeric();

			List<Future<Double>> votes =
				new ArrayList<Future<Double>>(data.numInstances());
			
			for (int i = 0; i < data.numInstances(); i++) 
			{
				VotesCollector aCollector = new VotesCollector(tree, i, data, inBag);
				votes.add(exe.submit(aCollector));
			}

			double outOfBagCount = 0.0;
			double errorSum = 0.0;

			for (int i = 0; i < data.numInstances(); i++) 
			{

				double vote = votes.get(i).get();

				// error for instance
				outOfBagCount += data.instance(i).weight();
				if (numeric) 
				{
					errorSum += StrictMath.abs(vote - data.instance(i).classValue()) * data.instance(i).weight();
				} 
				else 
				{
					if (vote != data.instance(i).classValue())
						errorSum += data.instance(i).weight();
				}

			}

			outOfBagError = errorSum / outOfBagCount;
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally 
		{
			exe.shutdownNow();
		}
		
	}

	/**
	 * Calculates the class membership probabilities for the given test
	 * instance.
	 *
	 * @param instance the instance to be classified
	 * @return predicted class probability distribution
	 */
	public double[] distributionForInstance(Instance instance) 
	{
		double[] sums = new double[instance.numClasses()], newProbs; 
		for (int i=0; i < numTrees; i++)
		{
			newProbs = tree[i].evaluate(instance);
			for (int j = 0; j < newProbs.length; j++)
				sums[j] += newProbs[j];
		}

		// Divide by the number of trees
		for (int j = 0; j < sums.length; j++)
			sums[j] /= (double) numTrees;

		return sums;
	}


	/**
	 * Gets the current settings of the forest.
	 *
	 * @return an array of strings suitable for passing to setOptions()
	 */
	public String[] getOptions() 
	{
		Vector        result;
		String[]      options;
		int           i;

		result = new Vector();

		result.add("-I");
		result.add("" + getNumTrees());

		result.add("-K");
		result.add("" + getNumFeatures());

		result.add("-S");
		result.add("" + getSeed());



		options = super.getOptions();
		for (i = 0; i < options.length; i++)
			result.add(options[i]);

		return (String[]) result.toArray(new String[result.size()]);
	}
	
	
	/**
	 * Parses a given list of options. <p/>
	 * 
	   <!-- options-start -->
	 * Valid options are: <p/>
	 * 
	 * <pre> -I &lt;number of trees&gt;
	 *  Number of trees to build.</pre>
	 * 
	 * <pre> -K &lt;number of features&gt;
	 *  Number of features to consider (&lt;1=int(logM+1)).</pre>
	 * 
	 * <pre> -S
	 *  Seed for random number generator.
	 *  (default 1)</pre>
	 * 
	 * <pre> -D
	 *  If set, classifier is run in debug mode and
	 *  may output additional info to the console</pre>
	 * 
	   <!-- options-end -->
	 * 
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception
	{
		String	tmpStr;

		tmpStr = Utils.getOption('I', options);
		if (tmpStr.length() != 0) {
			this.numTrees = Integer.parseInt(tmpStr);
		} else {
			this.numTrees = 100;
		}

		tmpStr = Utils.getOption('K', options);
		if (tmpStr.length() != 0) {
			this.numFeatures = Integer.parseInt(tmpStr);
		} else {
			this.numFeatures = 0;
		}

		tmpStr = Utils.getOption('S', options);
		if (tmpStr.length() != 0) {
			setSeed(Integer.parseInt(tmpStr));
		} else {
			setSeed(1);
		}
	
		super.setOptions(options);

		Utils.checkForRemainingOptions(options);
	}
	
	
	/**
	 * Get the number of random features to use
	 * in each node of the random trees.
	 * 
	 * @return number of random features
	 */
	public int getNumFeatures() 
	{
		return this.numFeatures;
	}

	/**
	 * Get the number of trees in the forest
	 * 
	 * @return number of trees being used
	 */
	public int getNumTrees() 
	{
		return this.numTrees;
	}

	/**
	 * Returns default capabilities of the classifier.
	 *
	 * @return the capabilities of this classifier
	 */
	public Capabilities getCapabilities() 
	{
		Capabilities result = super.getCapabilities();

		// attributes		
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		
		// class
		result.enable(Capability.NOMINAL_CLASS);

		return result;
	}

	/**
	 * Get the seed used to initialize the
	 * random number generators
	 * 
	 * @return random seed
	 */
	public int getSeed() 
	{
		return seed;
	}

	/**
	 * Set the seed used to initialize the
	 * random number generators
	 * 
	 * @param seed random seed
	 */
	public void setSeed(int seed) 
	{
		this.seed = seed;
	}

	/**
	 * Set the number of trees in the forest
	 * 
	 * @param numTrees number of trees
	 */
	public void setNumTrees(int numTrees) 
	{
		this.numTrees = numTrees;
	}

	/**
	 * Set the number of random features to use
	 * in each node of the random trees
	 * 
	 * @param numFeatures number of random features
	 */
	public void setNumFeatures(int numFeatures) 
	{
		this.numFeatures = numFeatures;
	}

	/**
	 * Gets the out of bag error that was calculated as the classifier
	 * was built.
	 *
	 * @return the out of bag error 
	 */
	public double measureOutOfBagError() 
	{
		return outOfBagError;
	}

	/**
	 * Returns an enumeration of the additional measure names.
	 *
	 * @return an enumeration of the measure names
	 */
	public Enumeration enumerateMeasures() 
	{

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
	public double getMeasure(String additionalMeasureName) 
	{

		if (additionalMeasureName.equalsIgnoreCase("measureOutOfBagError")) 
		{
			return measureOutOfBagError();
		}
		else 
		{
			throw new IllegalArgumentException(additionalMeasureName 
				+ " not supported (Bagging)");
		}
	}
	
	
	/**
	 * Outputs a description of this classifier.
	 *
	 * @return a string containing a description of the classifier
	 */
	public String toString() 
	{

		if (tree == null) 
			return "Balanced random forest not built yet";
		else 
			return "Balanced random forest of " + this.numTrees
			+ " trees, each constructed while considering "
			+ this.numFeatures + " random feature" + (this.numFeatures==1 ? "" : "s")
			+ "\nOut of bag error: "
			+ Utils.doubleToString(measureOutOfBagError(), 4) + ".\n";
	}

	/**
	 * Main method for this class.
	 *
	 * @param argv the options
	 */
	public static void main(String[] argv) {
		runClassifier(new BalancedRandomForest(), argv);
	}
	
}
