package ai;

import hr.irb.fastRandomForest.FastRandomForest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

public class BalancedRandomForest extends AbstractClassifier implements Randomizable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** random seed */
	int seed = 1;
	
	/** number of trees */
	int numTrees = 200;
		
	/** number of features used on each node of the trees */
	int numFeatures = 0;
	
	/** array of random trees that form the forest */
	BalancedRandomTree[] tree = null;
	
	
	/**
	 * Returns a string describing classifier
	 * @return a description suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String globalInfo() {

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
	public TechnicalInformation getTechnicalInformation() {
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
	public void buildClassifier(Instances data) throws Exception 
	{
		// If number of features is 0 then set it to log2 of M (number of attributes)
		if (numFeatures < 1) 
			numFeatures = (int) Utils.log2(data.numAttributes())+1;
		
		// Initialize array of trees
		tree = new BalancedRandomTree[ numTrees ];
		
		// total number of instances
		final int numInstances = data.numInstances();
		// total number of classes
		final int numClasses = data.numClasses();
		
		final ArrayList<Integer>[] indexSample = new ArrayList[ numClasses ];
		for(int i = 0; i < numClasses; i++)
			indexSample[i] = new ArrayList<Integer>();
		
		System.out.println("numClasses = " + numClasses);
		
		// fill indexSample with the indices of each class
		for(int i = 0 ; i < numInstances; i++)
		{
			System.out.println("data.get("+i+").classValue() = " + data.get(i).classValue());
			indexSample[ (int) data.get(i).classValue() ].add( i );
		}
		
		Random random = new Random(seed);
		
		// Executor service to run concurrent trees
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		List< Future<?> > futures =
            new ArrayList< Future<?> >( numTrees );

		try{
			for(int i = 0; i < numTrees; i++)
			{
				ArrayList<Integer> bagIndices = new ArrayList<Integer>(); 

				for(int j = 0 ; j < numInstances; j++)
				{
					// Randomly select the indices in a balanced way
					int randInt = random.nextInt( numClasses );				
					bagIndices.add( indexSample[ randInt ].get( random.nextInt( indexSample[randInt].size() ) ) );
				}

				// Create random tree
				final Splitter splitter = new Splitter(new GiniFunction(numFeatures));
				tree[i] = new BalancedRandomTree(data, bagIndices, splitter);

				futures.add( exe.submit( tree[i]) );
			}
			
			// Make sure all trees have been trained before proceeding
			for (int treeIdx = 0; treeIdx < numTrees; treeIdx++) 
				futures.get(treeIdx).get();		     			
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
	public void setOptions(String[] options) throws Exception{
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
	
	
	public int getNumFeatures() 
	{
		return this.numFeatures;
	}

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


	public int getSeed() 
	{
		return seed;
	}

	public void setSeed(int seed) 
	{
		this.seed = seed;
	}

	
	public void setNumTrees(int numTrees) 
	{
		this.numTrees = numTrees;
	}

	public void setNumFeatures(int numFeatures) 
	{
		this.numFeatures = numFeatures;
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
			+ this.numFeatures + " random feature" + (this.numFeatures==1 ? "" : "s") + ".\n";
			//+ "Out of bag error: "
			//+ Utils.doubleToString(m_bagger.measureOutOfBagError(), 4) + "\n";
	}

	/**
	 * Main method for this class.
	 *
	 * @param argv the options
	 */
	public static void main(String[] argv) {
		runClassifier(new FastRandomForest(), argv);
	}
	
}
