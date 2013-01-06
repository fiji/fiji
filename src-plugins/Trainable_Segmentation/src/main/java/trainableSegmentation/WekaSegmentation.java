package trainableSegmentation;

import java.awt.Point;
import java.awt.Rectangle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.vecmath.Point3f;

import hr.irb.fastRandomForest.FastRandomForest;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;

import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.classifiers.trees.RandomForest;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;

import weka.filters.Filter;

import weka.filters.supervised.attribute.AttributeSelection;

import weka.filters.supervised.instance.Resample;

import weka.gui.explorer.ClassifierPanel;


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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), Verena Kaynig (verena.kaynig@inf.ethz.ch),
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */

/**
 * This class contains all the library methods to perform image segmentation
 * based on the Weka classifiers.
 */
public class WekaSegmentation {

	/** maximum number of classes (labels) allowed */
	public static final int MAX_NUM_CLASSES = 5;

	/** array of lists of Rois for each slice (vector index) 
	 * and each class (arraylist index) of the training image */
	private Vector<ArrayList<Roi>> examples[];
	/** image to be used in the training */
	private ImagePlus trainingImage;
	/** result image after classification */
	private ImagePlus classifiedImage;
	/** features to be used in the training */
	private FeatureStackArray featureStackArray = null;
	/** set of instances for the whole training image */
	private Instances wholeImageData;
	/** set of instances from loaded data (previously saved segmentation) */
	private Instances loadedTrainingData = null;
	/** set of instances from the user's traces */
	private Instances traceTrainingData = null;
	/** current classifier */
	private AbstractClassifier classifier = null;
	/** train header */
	Instances trainHeader = null;

	/** default classifier (Fast Random Forest) */
	private FastRandomForest rf;
	/** flag to update the whole set of instances (used when there is any change on the features or the classes) */
	private boolean updateWholeData = false;
	/** flag to update the feature stack (used when there is any change on the features) */
	private boolean updateFeatures = false;
	
	/** array of boolean flags to update (or not) specific feature stacks during training */
	private boolean featureStackToUpdateTrain[];
	
	/** array of boolean flags to update (or not) specific feature stacks during test */
	private boolean featureStackToUpdateTest[];

	/** current number of classes */
	private int numOfClasses = 0;
	/** names of the current classes */
	private String[] classLabels = new String[]{"class 1", "class 2", "class 3", "class 4", "class 5"};

	// Random Forest parameters
	/** current number of trees in the fast random forest classifier */
	private int numOfTrees = 200;
	/** current number of random features per tree in the fast random forest classifier */
	private int randomFeatures = 2;
	/** maximum depth per tree in the fast random forest classifier */
	private int maxDepth = 0;
	/** list of class names on the loaded data */
	ArrayList<String> loadedClassNames = null;

	/** expected membrane thickness */
	private int membraneThickness = 1;
	/** size of the patch to use to enhance the membranes */
	private int membranePatchSize = 19;

	/** minimum sigma to use on the filters */
	private float minimumSigma = 1f;
	/** maximum sigma to use on the filters */
	private float maximumSigma = 16f;

	/** flags of filters to be used */
	private boolean[] enabledFeatures = new boolean[]{
			true, 	/* Gaussian_blur */
			true, 	/* Sobel_filter */
			true, 	/* Hessian */
			true, 	/* Difference_of_gaussians */
			true, 	/* Membrane_projections */
			false, 	/* Variance */
			false, 	/* Mean */
			false, 	/* Minimum */
			false, 	/* Maximum */
			false, 	/* Median */
			false,	/* Anisotropic_diffusion */
			false, 	/* Bilateral */
			false, 	/* Lipschitz */
			false, 	/* Kuwahara */
			false,	/* Gabor */
			false, 	/* Derivatives */
			false, 	/* Laplacian */
			false,	/* Structure */
			false,	/* Entropy */
			false	/* Neighbors */
	};
	/** use neighborhood flag */
	private boolean useNeighbors = false;

	/** list of the names of features to use */
	private ArrayList<String> featureNames = null;

	/** flag to set the resampling of the training data in order to guarantee the same number of instances per class */
	private boolean homogenizeClasses = false;

	/** Project folder name. It is used to stored temporary data if different from null */
	private String projectFolder = null;
	
	/** executor service to launch threads for the library operations */
	private ExecutorService exe = Executors.newFixedThreadPool(  Prefs.getThreads() );
	
	/**
	 * Default constructor.
	 *
	 * @param trainingImage The image to be segmented/trained
	 */
	public WekaSegmentation(ImagePlus trainingImage)
	{
		this.trainingImage = trainingImage;

		// Initialization of Fast Random Forest classifier
		rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);
		//this is the default that Breiman suggests
		//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		//but this seems to work better
		rf.setNumFeatures(randomFeatures);
		// Random seed
		rf.setSeed( (new Random()).nextInt() );

		// Set number of threads
		rf.setNumThreads( Prefs.getThreads() );
		
		classifier = rf;

		// Initialize feature stack (no features yet)
		featureStackArray = new FeatureStackArray(trainingImage.getImageStackSize(),
				minimumSigma, maximumSigma, useNeighbors, membraneThickness, membranePatchSize,
				enabledFeatures);
		
		featureStackToUpdateTrain = new boolean[trainingImage.getImageStackSize()];
		featureStackToUpdateTest = new boolean[trainingImage.getImageStackSize()];
		Arrays.fill(featureStackToUpdateTest, true);
		
		examples = new Vector[trainingImage.getImageStackSize()];
		for(int i=0; i< trainingImage.getImageStackSize(); i++)
		{
			examples[i] = new Vector<ArrayList<Roi>>(MAX_NUM_CLASSES);

			for(int j=0; j<MAX_NUM_CLASSES; j++)
				examples[i].add(new ArrayList<Roi>());
			
			// Initialize each feature stack (one per slice)
			featureStackArray.set(new FeatureStack(trainingImage.getImageStack().getProcessor(i+1)), i);
		}
		// start with two classes
		addClass();
		addClass();
	}

	/**
	 * No-image constructor. If you use this constructor, the image has to be
	 * set using setTrainingImage().
	 */
	public WekaSegmentation()
	{
		// Initialization of Fast Random Forest classifier
		rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);
		//this is the default that Breiman suggests
		//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		//but this seems to work better
		rf.setNumFeatures(randomFeatures);
		// Random seed
		rf.setSeed( (new Random()).nextInt() );
		// Set number of threads
		rf.setNumThreads( Prefs.getThreads() );

		classifier = rf;
		
		// start with two classes
		addClass();
		addClass();
	}
	
	/**
	 * Set the training image (single image or stack)
	 * 
	 * @param imp training image
	 */
	public void setTrainingImage(ImagePlus imp)
	{
		this.trainingImage = imp;

		// Initialize feature stack (no features yet)
		featureStackArray = new FeatureStackArray(trainingImage.getImageStackSize(),
				minimumSigma, maximumSigma, useNeighbors, membraneThickness, membranePatchSize,
				enabledFeatures);
		
		featureStackToUpdateTrain = new boolean[trainingImage.getImageStackSize()];
		featureStackToUpdateTest = new boolean[trainingImage.getImageStackSize()];
		Arrays.fill(featureStackToUpdateTest, true);

		// update list of examples
		examples = new Vector[trainingImage.getImageStackSize()];
		for(int i=0; i < trainingImage.getImageStackSize(); i++)
		{
			examples[i] = new Vector<ArrayList<Roi>>(MAX_NUM_CLASSES);
			
			for(int j=0; j<MAX_NUM_CLASSES; j++)
				examples[i].add(new ArrayList<Roi>());
			
			// Initialize each feature stack (one per slice)
			featureStackArray.set(new FeatureStack(trainingImage.getImageStack().getProcessor(i+1)), i);
		}	
	}

	/**
	 * Adds a ROI to the list of examples for a certain class
	 * and slice.
	 *
	 * @param classNum the number of the class
	 * @param roi the ROI containing the new example
	 * @param n number of the current slice
	 */
	public void addExample(int classNum, Roi roi, int n) 
	{
		if(featureStackToUpdateTrain[n-1] == false)
		{
			boolean updated = false;
			for(final ArrayList<Roi> list : examples[n-1])
				if(list.isEmpty() == false)
				{
					updated = true;
					break;
				}
			if(updated == false && featureStackToUpdateTest[n-1] == true)
			{
				//IJ.log("Feature stack for slice " + n 
				//		+ " needs to be updated");
				featureStackToUpdateTrain[n-1] = true;
				featureStackToUpdateTest[n-1] = false;
				updateFeatures = true;
			}
					
		}
				
		examples[n-1].get(classNum).add(roi);
	}

	/**
	 * Remove an example list from a class and specific slice
	 * 
	 * @param classNum the number of the examples' class
	 * @param nSlice the slice number
	 * @param index the index of the example list to remove
	 */
	public void deleteExample(int classNum, int nSlice, int index)
	{
		getExamples(classNum, nSlice).remove(index);
	}
	
	/**
	 * Return the list of examples for a certain class.
	 * 
	 * @param classNum the number of the examples' class
	 * @param n the slice number
	 */
	public List<Roi> getExamples(int classNum, int n) 
	{
		return examples[n-1].get(classNum);
	}

	/**
	 * Set flag to homogenize classes before training
	 *
	 * @param homogenizeClasses true to resample the classes before training
	 */
	public void setHomogenizeClasses(boolean homogenizeClasses)
	{
		this.homogenizeClasses = homogenizeClasses;
	}

	/**
	 * Set the current number of classes. Should not be used to create new
	 * classes. Use <link>addClass<\link> instead.
	 *
	 * @param numOfClasses the new number of classes
	 */
	public void setNumOfClasses(int numOfClasses) {
		this.numOfClasses = numOfClasses;
	}

	/**
	 * Get the current number of classes.
	 *
	 * @return the current number of classes
	 */
	public int getNumOfClasses() 
	{
		return numOfClasses;
	}

	/**
	 * Add new segmentation class.
	 */
	public void addClass()
	{
		if(null != trainingImage)
			for(int i=1; i <= trainingImage.getImageStackSize(); i++)
				examples[i-1].add(new ArrayList<Roi>());

		// increase number of available classes
		numOfClasses ++;
		updateWholeData = true;
	}

	/**
	 * Set the name of a class.
	 * 
	 * @param classNum class index
	 * @param label new name for the class
	 */
	public void setClassLabel(int classNum, String label) 
	{
		getClassLabels()[classNum] = label;
		updateWholeData = true;
	}

	/**
	 * Get the label name of a class.
	 * 
	 * @param classNum class index
	 */
	public String getClassLabel(int classNum) 
	{
		return getClassLabels()[classNum];
	}

	/**
	 * Load training data
	 *
	 * @param pathname complete path name of the training data file (.arff)
	 * @return false if error
	 */
	public boolean loadTrainingData(String pathname)
	{
		IJ.log("Loading data from " + pathname + "...");
		loadedTrainingData = readDataFromARFF(pathname);

		// Check the features that were used in the loaded data
		Enumeration<Attribute> attributes = loadedTrainingData.enumerateAttributes();
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		while(attributes.hasMoreElements())
		{
			final Attribute a = attributes.nextElement();
			for(int i = 0 ; i < numFeatures; i++)
				if(a.name().startsWith(FeatureStack.availableFeatures[i]))
					usedFeatures[i] = true;
		}

		// Check if classes match
		Attribute classAttribute = loadedTrainingData.classAttribute();
		Enumeration<String> classValues  = classAttribute.enumerateValues();

		// Update list of names of loaded classes
		loadedClassNames = new ArrayList<String>();

		int j = 0;
		while(classValues.hasMoreElements())
		{
			final String className = classValues.nextElement().trim();
			loadedClassNames.add(className);

			IJ.log("Read class name: " + className);
			if( !className.equals(this.getClassLabels()[j]))
			{
				String s = getClassLabels()[0];
				for(int i = 1; i < numOfClasses; i++)
					s = s.concat(", " + getClassLabels()[i]);
				IJ.error("ERROR: Loaded classes and current classes do not match!\nExpected: " + s);
				loadedTrainingData = null;
				return false;
			}
			j++;
		}

		if(j != numOfClasses)
		{
			IJ.error("ERROR: Loaded number of classes and current number do not match!");
			loadedTrainingData = null;
			return false;
		}
		
		boolean featuresChanged = false;
		final boolean[] oldEnableFeatures = this.featureStackArray.getEnabledFeatures();
		// Read checked features and check if any of them chasetButtonsEnablednged
		for(int i = 0; i < numFeatures; i++)
		{
			if (usedFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}
		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			this.setEnabledFeatures( usedFeatures );
			// Force features to be updated
			updateFeatures = true;
			
		}

		if (false == adjustSegmentationStateToData(loadedTrainingData) )
			loadedTrainingData = null;
		else
			IJ.log("Loaded data: " + loadedTrainingData.numInstances() + " instances (" + loadedTrainingData.numAttributes() + " attributes)");

		return true;
	}

	/**
	 * Returns a the loaded training data or null, if no training data was
	 * loaded.
	 */
	public Instances getLoadedTrainingData() {
		return loadedTrainingData;
	}

	/**
	 * Returns a the trace training data or null, if no examples have been
	 * given.
	 */
	public Instances getTraceTrainingData() {
		return traceTrainingData;
	}

	/**
	 * Get current classification result
	 * @return classified image
	 */
	public ImagePlus getClassifiedImage()
	{
		return classifiedImage;
	}

	/**
	 * Get the current training header
	 *
	 * @return training header (empty set of instances with the current attributes and classes)
	 */
	public Instances getTrainHeader()
	{
		return this.trainHeader;
	}

	/**
	 * Read header classifier from a .model file
	 * @param filename complete path and file name
	 * @return false if error
	 */
	public boolean loadClassifier(String filename)
	{
		AbstractClassifier newClassifier = null;
		Instances newHeader = null;
		File selected = new File(filename);
		try {
			InputStream is = new FileInputStream( selected );
			if (selected.getName().endsWith(ClassifierPanel.PMML_FILE_EXTENSION))
			{
				PMMLModel model = PMMLFactory.getPMMLModel(is, null);
				if (model instanceof PMMLClassifier)
					newClassifier = (PMMLClassifier)model;
				else
					throw new Exception("PMML model is not a classification/regression model!");
			}
			else
			{
				if (selected.getName().endsWith(".gz"))
					is = new GZIPInputStream(is);

				ObjectInputStream objectInputStream = new ObjectInputStream(is);
				newClassifier = (AbstractClassifier) objectInputStream.readObject();
				try
				{ // see if we can load the header
					newHeader = (Instances) objectInputStream.readObject();
				}
				catch (Exception e)
				{
					IJ.error("Load Failed", "Error while loading train header");
					return false;
				}
				finally
				{
					objectInputStream.close();
				}
			}
		}
		catch (Exception e)
		{
			IJ.error("Load Failed", "Error while loading classifier");
			e.printStackTrace();
			return false;
		}

		try{
			// Check if the loaded information corresponds to current state of the segmentator
			// (the attributes can be adjusted, but the classes must match)
			if(false == adjustSegmentationStateToData(newHeader))
			{
				IJ.log("Error: current segmentator state could not be updated to loaded data requirements (attributes and classes)");
				return false;
			}
		}catch(Exception e)
		{
			IJ.log("Error while adjusting data!");
			e.printStackTrace();
			return false;
		}
		
		this.classifier = newClassifier;
		this.trainHeader = newHeader;

		return true;
	}

	/**
	 * Returns the current classifier.
	 */
	public AbstractClassifier getClassifier() {
		return classifier;
	}

	/**
	 * Write current classifier into a file
	 *
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public boolean saveClassifier(String filename)
	{
		File sFile = null;
		boolean saveOK = true;


		IJ.log("Saving model to file...");

		try {
			sFile = new File(filename);
			OutputStream os = new FileOutputStream(sFile);
			if (sFile.getName().endsWith(".gz"))
			{
				os = new GZIPOutputStream(os);
			}
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
			objectOutputStream.writeObject(classifier);
			if (trainHeader != null)
				objectOutputStream.writeObject(trainHeader);
			objectOutputStream.flush();
			objectOutputStream.close();
		}
		catch (Exception e)
		{
			IJ.error("Save Failed", "Error when saving classifier into a file");
			saveOK = false;
		}
		if (saveOK)
			IJ.log("Saved model into " + filename );

		return saveOK;
	}

	/**
	 * Save training data into a file (.arff)
	 * @param pathname complete path name
	 * @return false if error
	 */
	public boolean saveData(final String pathname)
	{
		boolean examplesEmpty = true;
		for(int i = 0; i < numOfClasses; i ++)
		{
			for(int n=0; n<trainingImage.getImageStackSize(); n++)
				if(examples[n].get(i).size() > 0)
				{
					examplesEmpty = false;
					break;
				}
		}
		if (examplesEmpty && loadedTrainingData == null){
			IJ.log("There is no data to save");
			return false;
		}

		if(featureStackArray.isEmpty() || updateFeatures)
		{
			IJ.log("Creating feature stack...");
			if ( false == featureStackArray.updateFeaturesMT(featureStackToUpdateTrain) )
				return false;
			Arrays.fill(featureStackToUpdateTrain, false);
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		Instances data = null;

		if(examplesEmpty == false)
		{
			data = createTrainingInstances();
			data.setClassIndex(data.numAttributes() - 1);
		}
		if (null != loadedTrainingData && null != data){
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished: total number of instances = " + data.numInstances());
		}
		else if (null == data)
			data = loadedTrainingData;


		IJ.log("Writing training data: " + data.numInstances() + " instances...");

		//IJ.log("Data: " + data.numAttributes() +" attributes, " + data.numClasses() + " classes");

		writeDataToARFF(data, pathname);
		IJ.log("Saved training data: " + pathname);

		return true;
	}

	public void setUseNeighbors(boolean useNeighbors)
	{
		this.featureStackArray.setUseNeighbors(useNeighbors);
	}


	/**
	 * Add instances to a specific class from a label (binary) image.
	 * Only white (non black) pixels will be added to the corresponding class.
	 *
	 * @param labelImage binary image
	 * @param featureStack corresponding feature stack
	 * @param className name of the class which receives the instances
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			FeatureStack featureStack,
			String className)
	{

		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class index
		int classIndex = 0;
		for(classIndex = 0 ; classIndex < this.getClassLabels().length; classIndex++)
			if(className.equalsIgnoreCase(this.getClassLabels()[classIndex]))
				break;
		if(classIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + className + "' not found.");
			return false;
		}
		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++){
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}
			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}



		// Check all pixels different from black
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final ImageProcessor img = labelImage.getProcessor();
		int nl = 0;
		for(int x = 0 ; x < width ; x++)
			for(int y = 0 ; y < height; y++)
			{
				// White pixels are added to the class
				if(img.getPixelValue(x, y) > 0)
				{


						double[] values = new double[featureStack.getSize()+1];
						for (int z=1; z<=featureStack.getSize(); z++)
							values[z-1] = featureStack.getProcessor(z).getPixelValue(x, y);
						values[featureStack.getSize()] = (double) classIndex;
						loadedTrainingData.add(new DenseInstance(1.0, values));
						// increase number of instances for this class
						nl ++;
				}
			}


		IJ.log("Added " + nl + " instances of '" + className +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}

	/**
	 * Add instances to two classes from a label (binary) image.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param labelImage binary image
	 * @param featureStack corresponding feature stack
	 * @param className1 name of the class which receives the white pixels
	 * @param className2 name of the class which receives the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			FeatureStack featureStack,
			String className1,
			String className2)
	{		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class indexes
		int classIndex1 = 0;
		for(classIndex1 = 0 ; classIndex1 < this.getClassLabels().length; classIndex1++)
			if(className1.equalsIgnoreCase(this.getClassLabels()[classIndex1]))
				break;
		if(classIndex1 == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return false;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.getClassLabels().length; classIndex2++)
			if(className2.equalsIgnoreCase(this.getClassLabels()[classIndex2]))
				break;
		if(classIndex2 == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + className2 + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}

			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Check all pixels 
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final ImageProcessor img = labelImage.getProcessor();
		int n1 = 0;
		int n2 = 0;
		int classIndex = -1;

		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the class 1
				// and black to class 2
				if(img.getPixelValue(x, y) > 0)
				{
					classIndex = classIndex1;
					n1++;
				}
				else
				{
					classIndex = classIndex2;
					n2++;
				}

				/*
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++)
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x, y);
				values[featureStack.getSize()] = (double) classIndex;
				*/
				loadedTrainingData.add(featureStack.createInstance(x, y, classIndex));
			}

		IJ.log("Added " + n1 + " instances of '" + className1 +"'.");
		IJ.log("Added " + n2 + " instances of '" + className2 +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}

	/**
	 * Add instances to two classes from a label (binary) image in a random
	 * and balanced way.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param labelImage binary image
	 * @param featureStack corresponding feature stack
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param blackClassName name of the class which receives the black pixels
	 * @param numSamples number of samples to add of each class
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImageProcessor labelImage,
			FeatureStack featureStack,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}
		int blackClassIndex = 0;
		for(blackClassIndex = 0 ; blackClassIndex < this.getClassLabels().length; blackClassIndex++)
			if(blackClassName.equalsIgnoreCase(this.getClassLabels()[blackClassIndex]))
				break;
		if(blackClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + blackClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}

			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Create lists of coordinates of pixels of both classes
		ArrayList<Point> blackCoordinates = new ArrayList<Point>();
		ArrayList<Point> whiteCoordinates = new ArrayList<Point>();
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		
		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the class 1
				// and black to class 2
				if(labelImage.getPixelValue(x, y) > 0)				
					whiteCoordinates.add(new Point(x, y));					
				else				
					blackCoordinates.add(new Point(x, y));						
			}

		// Select random samples from both classes
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomBlack = rand.nextInt( blackCoordinates.size() );
			int randomWhite = rand.nextInt( whiteCoordinates.size() );
			
						
			loadedTrainingData.add(featureStack.createInstance( blackCoordinates.get(randomBlack).x, 
					blackCoordinates.get(randomBlack).y, blackClassIndex));
			loadedTrainingData.add(featureStack.createInstance(whiteCoordinates.get(randomWhite).x, 
					whiteCoordinates.get(randomWhite).y, whiteClassIndex));
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");
		IJ.log("Added " + numSamples + " instances of '" + blackClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}	
	
	/**
	 * Add instances to two classes from a label (binary) image in a random
	 * and balanced way.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param labelImage binary image
	 * @param mask binary mask image to prevent some pixel to be selected (null if all pixels are eligible)
	 * @param featureStack corresponding feature stack
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param blackClassName name of the class which receives the black pixels
	 * @param numSamples number of samples to add of each class
	 * 
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImageProcessor labelImage,
			ImageProcessor mask,
			FeatureStack featureStack,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}
		int blackClassIndex = 0;
		for(blackClassIndex = 0 ; blackClassIndex < this.getClassLabels().length; blackClassIndex++)
			if(blackClassName.equalsIgnoreCase(this.getClassLabels()[blackClassIndex]))
				break;
		if(blackClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + blackClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}

			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Create lists of coordinates of pixels of both classes
		ArrayList<Point> blackCoordinates = new ArrayList<Point>();
		ArrayList<Point> whiteCoordinates = new ArrayList<Point>();
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();

		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the class 1
				// and black to class 2
				if(null != mask && mask.getPixelValue(x, y) > 0)
				{
					if(labelImage.getPixelValue(x, y) > 0)				
						whiteCoordinates.add(new Point(x, y));					
					else				
						blackCoordinates.add(new Point(x, y));
				}
			}

		// Select random samples from both classes
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomBlack = rand.nextInt( blackCoordinates.size() );
			int randomWhite = rand.nextInt( whiteCoordinates.size() );
			
						
			loadedTrainingData.add(featureStack.createInstance( blackCoordinates.get(randomBlack).x, 
					blackCoordinates.get(randomBlack).y, blackClassIndex));
			loadedTrainingData.add(featureStack.createInstance(whiteCoordinates.get(randomWhite).x, 
					whiteCoordinates.get(randomWhite).y, whiteClassIndex));
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");
		IJ.log("Added " + numSamples + " instances of '" + blackClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}	

	
	/**
	 * Add instances to two classes from lists of coordinates in a random
	 * and balanced way.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param classPoints list of 3D coordinates to be used (x, y, slice)
	 * @param fsa feature stack array
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param blackClassName name of the class which receives the black pixels
	 * @param numSamples number of samples to add of each class
	 * 
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			List< Point3f >[] classPoints,
			FeatureStackArray fsa,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{		

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}
		int blackClassIndex = 0;
		for(blackClassIndex = 0 ; blackClassIndex < this.getClassLabels().length; blackClassIndex++)
			if(blackClassName.equalsIgnoreCase(this.getClassLabels()[blackClassIndex]))
				break;
		if(blackClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + blackClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=fsa.getNumOfFeatures(); i++)
			{
				String attString = fsa.getLabel( i );
				attributes.add(new Attribute(attString));
			}

			if(fsa.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}
				
		// Select random samples from both classes
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomBlack = rand.nextInt( classPoints[ 0 ].size() );
			int randomWhite = rand.nextInt( classPoints[ 1 ].size() );
			
			// add random black sample
			loadedTrainingData.add( fsa.get( (int) (classPoints[ 0 ].get(randomBlack).z) ) 
										.createInstance( 	(int) (classPoints[ 0 ].get(randomBlack).x), 
															(int) (classPoints[ 0 ].get(randomBlack).y), 
															blackClassIndex) );
			
			// add random white sample
			loadedTrainingData.add(fsa.get( (int) (classPoints[ 1 ].get(randomWhite).z) ) .createInstance( (int) (classPoints[ 1 ].get(randomWhite).x), 
					(int) (classPoints[ 1 ].get(randomWhite).y), whiteClassIndex));
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");
		IJ.log("Added " + numSamples + " instances of '" + blackClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}
	
	/**
	 * Add instances to two classes from lists of coordinates in a random
	 * and balanced way.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param classPoints list of 3D coordinates to be used (x, y, slice)
	 * @param fsa feature stack array
	 * @param weights weight image
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param blackClassName name of the class which receives the black pixels
	 * @param numSamples number of samples to add of each class
	 * 
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			List< Point3f >[] classPoints,			
			FeatureStackArray fsa,
			ImagePlus weights,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{		

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}
		int blackClassIndex = 0;
		for(blackClassIndex = 0 ; blackClassIndex < this.getClassLabels().length; blackClassIndex++)
			if(blackClassName.equalsIgnoreCase(this.getClassLabels()[blackClassIndex]))
				break;
		if(blackClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + blackClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=fsa.getNumOfFeatures(); i++)
			{
				String attString = fsa.getLabel( i );
				attributes.add(new Attribute(attString));
			}

			if(fsa.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}
				
		final int width = weights.getWidth();
		
		// Select random samples from both classes
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomBlack = rand.nextInt( classPoints[ 0 ].size() );
			int randomWhite = rand.nextInt( classPoints[ 1 ].size() );
			
			// add random black sample
			final int blackZ = (int) (classPoints[ 0 ].get(randomBlack).z);
			final int blackX = (int) (classPoints[ 0 ].get(randomBlack).x);
			final int blackY = (int) (classPoints[ 0 ].get(randomBlack).y);
			
			DenseInstance blackInstance = fsa.get( blackZ ) 
											.createInstance( 	blackX, 
																blackY, 
																blackClassIndex);
			
			blackInstance.setWeight( ((float[]) weights.getImageStack().getProcessor( 
										blackZ + 1 ).getPixels())[ blackX + blackY * width ] ); 
			
			loadedTrainingData.add( blackInstance );
			
			// add random white sample
			final int whiteZ = (int) (classPoints[ 1 ].get(randomWhite).z);
			final int whiteX = (int) (classPoints[ 1 ].get(randomWhite).x);
			final int whiteY = (int) (classPoints[ 1 ].get(randomWhite).y);
			
			DenseInstance whiteInstance = fsa.get( whiteZ )
											.createInstance( 	whiteX , 
																whiteY , 
																whiteClassIndex);
			
			whiteInstance.setWeight( ((float[]) weights.getImageStack().getProcessor( 
										whiteZ + 1 ).getPixels())[ whiteX + whiteY * width ] );
			
			loadedTrainingData.add( whiteInstance );
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");
		IJ.log("Added " + numSamples + " instances of '" + blackClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}
	
	
	/**
	 * Add instances to two classes from a label (binary) image in a random
	 * and balanced way.
	 * White pixels will be added to the corresponding class 1 and
	 * black pixels will be added to class 2.
	 *
	 * @param labelImage binary image
	 * @param mask mask image
	 * @param weights weight image
	 * @param featureStack corresponding feature stack
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param blackClassName name of the class which receives the black pixels
	 * @param numSamples number of samples to add of each class
	 * @param mask binary mask image to prevent some pixel to be selected (null if all pixels are eligible)
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImageProcessor labelImage,
			ImageProcessor mask,
			ImageProcessor weights,
			FeatureStack featureStack,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}
		int blackClassIndex = 0;
		for(blackClassIndex = 0 ; blackClassIndex < this.getClassLabels().length; blackClassIndex++)
			if(blackClassName.equalsIgnoreCase(this.getClassLabels()[blackClassIndex]))
				break;
		if(blackClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + blackClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}

			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Create lists of coordinates of pixels of both classes
		ArrayList<Point> blackCoordinates = new ArrayList<Point>();
		ArrayList<Point> whiteCoordinates = new ArrayList<Point>();
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();

		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the class 1
				// and black to class 2
				if(null != mask && mask.getPixelValue(x, y) > 0)
				{
					if(labelImage.getPixelValue(x, y) > 0)				
						whiteCoordinates.add(new Point(x, y));					
					else				
						blackCoordinates.add(new Point(x, y));
				}
			}

		// Select random samples from both classes
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomBlack = rand.nextInt( blackCoordinates.size() );
			int randomWhite = rand.nextInt( whiteCoordinates.size() );
			
						
			DenseInstance blackSample = featureStack.createInstance( blackCoordinates.get(randomBlack).x, 
					blackCoordinates.get(randomBlack).y, blackClassIndex);
			blackSample.setWeight( weights.getPixelValue(  	blackCoordinates.get(randomBlack).x, 
														 	blackCoordinates.get(randomBlack).y) );
			loadedTrainingData.add(blackSample);
			
			DenseInstance whiteSample = featureStack.createInstance(whiteCoordinates.get(randomWhite).x, 
					whiteCoordinates.get(randomWhite).y, whiteClassIndex);
			
			whiteSample.setWeight( weights.getPixelValue(  	whiteCoordinates.get(randomWhite).x, 
				 											whiteCoordinates.get(randomWhite).y) );
			loadedTrainingData.add(whiteSample);
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");
		IJ.log("Added " + numSamples + " instances of '" + blackClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}
	
	/**
	 * Add instances to two classes from a label (binary) image in a random
	 * way.
	 * White pixels will be added to the corresponding class 1 
	 * (defined by whiteClassName)
	 *
	 * @param labelImage binary image
	 * @param featureStack corresponding feature stack
	 * @param whiteClassName name of the class which receives the white pixels
	 * @param numSamples number of samples to add of each class
	 * @return false if error
	 */
	public boolean addRandomData(
			ImagePlus labelImage,
			FeatureStack featureStack,
			String whiteClassName,
			int numSamples)
	{		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Detect class indexes
		int whiteClassIndex = 0;
		for(whiteClassIndex = 0 ; whiteClassIndex < this.getClassLabels().length; whiteClassIndex++)
			if(whiteClassName.equalsIgnoreCase(this.getClassLabels()[whiteClassIndex]))
				break;
		if(whiteClassIndex == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + whiteClassName + "' not found.");
			return false;
		}

		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}

			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}

			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(getClassLabels()[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Create lists of coordinates of pixels of white class
		ArrayList<Point> whiteCoordinates = new ArrayList<Point>();
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final ImageProcessor img = labelImage.getProcessor();

		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the white class			
				if(img.getPixelValue(x, y) > 0)				
					whiteCoordinates.add(new Point(x, y));					
			}

		// Select random samples from white class
		Random rand = new Random();
		for(int i=0; i<numSamples; i++)
		{
			int randomWhite = rand.nextInt( whiteCoordinates.size() );			
						
			loadedTrainingData.add(featureStack.createInstance(whiteCoordinates.get(randomWhite).x, 
					whiteCoordinates.get(randomWhite).y, whiteClassIndex));
		}
		
		IJ.log("Added " + numSamples + " instances of '" + whiteClassName +"'.");

		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() +
				" instances, " + loadedTrainingData.numAttributes() +
				" attributes, " + loadedTrainingData.numClasses() + " classes).");

		return true;
	}	
	
	/**
	 * Get current feature stack
	 * 
	 * @param i number of feature stack slice (>=1)
	 * @return feature stack of the corresponding slice
	 */
	public FeatureStack getFeatureStack(int i)
	{
		return this.featureStackArray.get(i-1);
	}

	/**
	 * Get the current feature stack array
	 * @return current feature stack array 
	 */
	public FeatureStackArray getFeatureStackArray()
	{
		return this.featureStackArray;
	}
	
	/**
	 * Get loaded (or accumulated) training instances
	 *
	 * @return loaded/accumulated training instances
	 */
	public Instances getTrainingInstances()
	{
		return this.loadedTrainingData;
	}

	/**
	 * Set current classifier
	 * @param cls new classifier
	 */
	public void setClassifier(AbstractClassifier cls)
	{
		this.classifier = cls;
	}

	/**
	 * Load a new image to segment (no GUI)
	 *
	 * @param newImage new image to segment
	 * @return false if error
	 */
	public boolean loadNewImage( ImagePlus newImage )
	{
		// Accumulate current data in "loadedTrainingData"
		IJ.log("Storing previous image instances...");

		if(featureStackArray.isEmpty() || updateFeatures)
		{
			IJ.log("Creating feature stack...");
			if ( false == featureStackArray.updateFeaturesMT(featureStackToUpdateTrain) )
				return false;
			Arrays.fill(featureStackToUpdateTrain, false);
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		// Create instances
		Instances data = createTrainingInstances();
		if (null != loadedTrainingData && null != data)
		{
			data.setClassIndex(data.numAttributes() - 1);
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished");
		}
		else if (null == data)
			data = loadedTrainingData;

		// Store merged data as loaded data
		loadedTrainingData = data;

		if(null != loadedTrainingData)
		{
			Attribute classAttribute = loadedTrainingData.classAttribute();
			Enumeration<String> classValues  = classAttribute.enumerateValues();

			// Update list of names of loaded classes
			loadedClassNames = new ArrayList<String>();
			while(classValues.hasMoreElements())
			{
				final String className = classValues.nextElement().trim();
				loadedClassNames.add(className);
			}
			IJ.log("Number of accumulated examples: " + loadedTrainingData.numInstances());
		}
		else
			IJ.log("Number of accumulated examples: 0");
		
		// Updating image
		IJ.log("Updating image...");

		// Set new image as training image
		trainingImage = new ImagePlus("Advanced Weka Segmentation", newImage.getImageStack());
		
		// Initialize feature stack array (no features yet)
		featureStackArray = new FeatureStackArray(trainingImage.getImageStackSize(),
				minimumSigma, maximumSigma, useNeighbors, membraneThickness, membranePatchSize,
				enabledFeatures);
		
		// Remove traces from the lists and ROI overlays and initialize each feature stack
		IJ.log("Removing previous markings...");
		examples = new Vector[trainingImage.getImageStackSize()];
		for(int i=0; i< trainingImage.getImageStackSize(); i++)
		{
			examples[i] = new Vector<ArrayList<Roi>>(MAX_NUM_CLASSES);

			for(int j=0; j<MAX_NUM_CLASSES; j++)
				examples[i].add(new ArrayList<Roi>());
			// Initialize each feature stack (one per slice)
			featureStackArray.set(new FeatureStack(trainingImage.getImageStack().getProcessor(i+1)), i);
		}
		
		
		featureStackToUpdateTrain = new boolean[trainingImage.getImageStackSize()];
		featureStackToUpdateTest = new boolean[trainingImage.getImageStackSize()];
		Arrays.fill(featureStackToUpdateTest, true);
		updateFeatures = true;
		updateWholeData = true;

		// Remove current classification result image
		classifiedImage = null;

		IJ.log("New image: " + newImage.getTitle() + " ("+trainingImage.getImageStackSize() + " slice(s))");
		
		IJ.log("Done");

		return true;
	}

	/**
	 * Add center lines of label image as binary data
	 *
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addCenterLinesBinaryData(
			ImagePlus labelImage,
			int n,
			String whiteClassName,
			String blackClassName)
	{
		// Update features if necessary
		if(featureStackArray.get(n).getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStackArray.get(n).updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		// Process white pixels
		final ImagePlus whiteIP = new ImagePlus ("white", labelImage.getProcessor().duplicate());
		IJ.run(whiteIP, "Skeletonize","");
		// Add skeleton to white class
		if( false == this.addBinaryData(whiteIP, featureStackArray.get(n), whiteClassName) )
		{
			IJ.log("Error while loading white class center-lines data.");
			return false;
		}

		// Process black pixels
		final ImagePlus blackIP = new ImagePlus ("black", labelImage.getProcessor().duplicate());
		IJ.run(blackIP, "Invert","");
		IJ.run(blackIP, "Skeletonize","");
		// Add skeleton to black class
		if( false == this.addBinaryData(blackIP, featureStackArray.get(n), blackClassName))
		{
			IJ.log("Error while loading black class center-lines data.");
			return false;
		}
		return true;
	}

	/**
	 * Filter feature stack based on the list of feature names to use
	 */
	public void filterFeatureStackByList()
	{
		if (null == this.featureNames)
			return;

		
		for(int i=1; i<=this.featureStackArray.getNumOfFeatures(); i++)
		{
			final String featureName = this.featureStackArray.getLabel(i);
			if(false == this.featureNames.contains( featureName ) )
			{
				// Remove feature
				for(int j=0; j<=this.featureStackArray.getSize(); j++)
					this.featureStackArray.get(j).removeFeature( featureName );
				// decrease i to avoid skipping any name
				i--;
			}
		}
	}


	/**
	 * Filter feature stack based on the list of feature names to use
	 *
	 * @param featureNames list of feature names to use
	 * @param featureStack feature stack to filter
	 */
	public static void filterFeatureStackByList(
			ArrayList<String> featureNames,
			FeatureStack featureStack)
	{
		if (null == featureNames)
			return;
		
		if (Thread.currentThread().isInterrupted() )
			return;

		IJ.log("Filtering feature stack by selected attributes...");

		for(int i=1; i<=featureStack.getSize(); i++)
		{
			final String featureName = featureStack.getSliceLabel(i);
			//IJ.log(" " + featureName + "...");
			if(false == featureNames.contains( featureName ) )
			{
				// Remove feature
				featureStack.removeFeature( featureName );
				// decrease i to avoid skipping any name
				i--;
			}
		}
	}

	/**
	 * Add label image as binary data
	 *
	 * @param labelImage binary label image
	 * @param n slice number
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			int n,
			String whiteClassName,
			String blackClassName)
	{
		
		// Update features if necessary
		if(featureStackArray.get(n).getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStackArray.get(n).updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		// Process label pixels
		final ImagePlus labelIP = new ImagePlus ("labels", labelImage.getProcessor().duplicate());
		// Make sure it's binary
		labelIP.getProcessor().autoThreshold();
		
		if( false == this.addBinaryData(labelIP, featureStackArray.get(n), whiteClassName, blackClassName) )
		{
			IJ.log("Error while loading binary label data.");
			return false;
		}

		return true;
	}

	/**
	 * Add binary training data from input and label images.
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnabledFeatures(this.featureStackArray.getEnabledFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);

			featureStack.setUseNeighbors(this.featureStackArray.useNeighborhood());

			if( false == this.addBinaryData(labelIP, featureStack, whiteClassName, blackClassName) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}

	
	/**
	 * Add binary training data from input and label images in a
	 * random and balanced way (same number of samples per class).
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @param numSamples number of samples to pick for each class
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnabledFeatures(this.featureStackArray.getEnabledFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			IJ.log("Creating feature stack for slice "+i+"...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			IJ.log("Feature stack is now updated.");

			featureStack.setUseNeighbors(this.featureStackArray.useNeighborhood());

			if( false == addRandomBalancedBinaryData(labelIP.getProcessor(), featureStack, whiteClassName, blackClassName, numSamples) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Add binary training data from input and label images in a
	 * random and balanced way (same number of samples per class).
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @param numSamples number of samples to pick for each class
	 * @param mask mask to prevent some pixel to be selected (null if all pixels are eligible)
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName,
			int numSamples,
			ImagePlus mask)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnabledFeatures(this.featureStackArray.getEnabledFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			IJ.log("Creating feature stack for slice "+i+"...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			IJ.log("Feature stack is now updated.");

			featureStack.setUseNeighbors(this.featureStackArray.useNeighborhood());

			if( false == addRandomBalancedBinaryData(labelIP.getProcessor(), 
					null == mask ? null : mask.getImageStack().getProcessor(i), 
					featureStack, whiteClassName, blackClassName, numSamples) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Add binary training data from input and label images in a
	 * random and balanced way (same number of samples per class).
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @param numSamples number of samples to pick for each class
	 * @param mask mask to prevent some pixel to be selected (null if all pixels are eligible)
	 * @param weights image containing the weight of each sample
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName,
			int numSamples,
			ImagePlus mask,
			ImagePlus weights)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnabledFeatures(this.featureStackArray.getEnabledFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			IJ.log("Creating feature stack for slice "+i+"...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			IJ.log("Feature stack is now updated.");

			featureStack.setUseNeighbors(this.featureStackArray.useNeighborhood());

			if( false == addRandomBalancedBinaryData(labelIP.getProcessor(), 
					null == mask ? null : mask.getImageStack().getProcessor(i), 
					weights.getImageStack().getProcessor( i ),
					featureStack, whiteClassName, blackClassName, numSamples) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Add training data from input and label images in a
	 * random way.
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image (labels in white)
	 * @param whiteClassName class name for the white pixels
	 * @param numSamples number of samples to pick for each class
	 * @return false if error
	 */
	public boolean addRandomData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			int numSamples)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnabledFeatures(this.featureStackArray.getEnabledFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			IJ.log("Creating feature stack for slice "+i+"...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			IJ.log("Feature stack is now updated.");

			featureStack.setUseNeighbors(this.featureStackArray.useNeighborhood());

			if( false == addRandomData(labelIP, featureStack, whiteClassName, numSamples) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Add binary training data from input and label images in a
	 * random and balanced way (same number of samples per class).
	 * The features will be created out of a list of filters.
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param filters stack of filters to create features
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @param numSamples number of samples to pick for each class
	 * @return false if error
	 */
	public boolean addRandomBalancedBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			ImagePlus filters,
			String whiteClassName,
			String blackClassName,
			int numSamples)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));			
			featureStack.addFeaturesMT( filters );


			if( false == addRandomBalancedBinaryData(labelIP.getProcessor(), featureStack, whiteClassName, blackClassName, numSamples) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Add binary training data from input and label images.
	 * The features will be created out of a list of filters.
	 * Input and label images can be 2D or stacks and their
	 * sizes must match.
	 *
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param filters stack of filters to create features
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			ImagePlus filters,
			String whiteClassName,
			String blackClassName)
	{

		// Check sizes
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();

		for(int i=1; i <= inputSlices.getSize(); i++)
		{

			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			labelIP.getProcessor().autoThreshold();
			
			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));			
			featureStack.addFeaturesMT( filters );


			if( false == this.addBinaryData(labelIP, featureStack, whiteClassName, blackClassName) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	

	/**
	 * Add eroded version of label image as binary data
	 *
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addErodedBinaryData(
			ImagePlus labelImage,
			int n,
			String whiteClassName,
			String blackClassName)
	{
		// Update features if necessary
		if(featureStackArray.get(n).getSize() < 2)			
		{
			IJ.log("Creating feature stack...");
			featureStackArray.get(n).updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Feature stack is now updated.");
		}

		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}

		// Process white pixels
		final ImagePlus whiteIP = new ImagePlus ("white", labelImage.getProcessor().duplicate());
		IJ.run(whiteIP, "Erode","");
		// Add skeleton to white class
		if( false == this.addBinaryData(whiteIP, featureStackArray.get(n), whiteClassName) )
		{
			IJ.log("Error while loading white class center-lines data.");
			return false;
		}

		// Process black pixels
		final ImagePlus blackIP = new ImagePlus ("black", labelImage.getProcessor().duplicate());
		IJ.run(blackIP, "Invert","");
		IJ.run(blackIP, "Erode","");
		// Add skeleton to white class
		if( false == this.addBinaryData(blackIP, featureStackArray.get(n), blackClassName))
		{
			IJ.log("Error while loading black class center-lines data.");
			return false;
		}
		return true;
	}

	/**
	 * Set pre-loaded training data (not from the user traces)
	 * @param data new data
	 */
	public void setLoadedTrainingData(Instances data)
	{
		this.loadedTrainingData = data;
	}

	/**
	 * Force segmentator to use all available features
	 */
	public void useAllFeatures()
	{
		boolean[] enableFeatures = this.featureStackArray.getEnabledFeatures();
		for (int i = 0; i < enableFeatures.length; i++)
			enableFeatures[i] = true;
		this.featureStackArray.setEnabledFeatures(enableFeatures);
	}

	/**
	 * Set the project folder
	 * @param projectFolder complete path name for project folder
	 */
	public void setProjectFolder(final String projectFolder)
	{
		this.projectFolder = projectFolder;
	}


	/**
	 * Homogenize number of instances per class
	 *
	 * @param data input set of instances
	 * @return resampled set of instances
	 */
	public static Instances homogenizeTrainingData(Instances data)
	{
		final Resample filter = new Resample();
		Instances filteredIns = null;
		filter.setBiasToUniformClass(1.0);
		try {
			filter.setInputFormat(data);
			filter.setNoReplacement(false);
			filter.setSampleSizePercent(100);
			filteredIns = Filter.useFilter(data, filter);
		} catch (Exception e) {
			IJ.log("Error when resampling input data!");
			e.printStackTrace();
		}
		return filteredIns;

	}

	/**
	 * Homogenize number of instances per class (in the loaded training data)
	 */
	public void homogenizeTrainingData()
	{
		final Resample filter = new Resample();
		Instances filteredIns = null;
		filter.setBiasToUniformClass(1.0);
		try {
			filter.setInputFormat(this.loadedTrainingData);
			filter.setNoReplacement(false);
			filter.setSampleSizePercent(100);
			filteredIns = Filter.useFilter(this.loadedTrainingData, filter);
		} catch (Exception e) {
			IJ.log("Error when resampling input data!");
			e.printStackTrace();
		}
		this.loadedTrainingData = filteredIns;
	}

	/**
	 * Select attributes of current data by BestFirst search.
	 * The data is reduced to the selected attributes (features).
	 *
	 * @return false if the current dataset is empty
	 */
	public boolean selectAttributes()
	{
		if(null == loadedTrainingData)
		{
			IJ.error("There is no data so select attributes from.");
			return false;
		}
		// Select attributes by BestFirst
		loadedTrainingData = selectAttributes(loadedTrainingData);
		// Update list of features to use
		this.featureNames = new ArrayList<String>();
		IJ.log("Selected attributes:");
		for(int i = 0; i < loadedTrainingData.numAttributes(); i++)
		{
			this.featureNames.add(loadedTrainingData.attribute(i).name());
			IJ.log((i+1) + ": " + this.featureNames.get(i));
		}

		// force data (ARFF) update
		this.updateWholeData = true;

		return true;
	}

	/**
	 * Select attributes using BestFirst search to reduce
	 * the number of parameters per instance of a dataset
	 *
	 * @param data input set of instances
	 * @return resampled set of instances
	 */
	public static Instances selectAttributes(Instances data)
	{
		final AttributeSelection filter = new AttributeSelection();
		Instances filteredIns = null;
		// Evaluator
		final CfsSubsetEval evaluator = new CfsSubsetEval();
		evaluator.setMissingSeparate(true);
		// Assign evaluator to filter
		filter.setEvaluator(evaluator);
		// Search strategy: best first (default values)
		final BestFirst search = new BestFirst();
		filter.setSearch(search);
		// Apply filter
		try {
			filter.setInputFormat(data);

			filteredIns = Filter.useFilter(data, filter);
		} catch (Exception e) {
			IJ.log("Error when resampling input data with selected attributes!");
			e.printStackTrace();
		}
		return filteredIns;

	}

	/**
	 * Get training error (from loaded data).
	 *
	 * @param verbose option to display evaluation information in the log window
	 * @return classifier error on the training data set.
	 */
	public double getTrainingError(boolean verbose)
	{
		if(null == this.trainHeader)
			return -1;

		double error = -1;
		try {
			final Evaluation evaluation = new Evaluation(this.loadedTrainingData);
			evaluation.evaluateModel(classifier, this.loadedTrainingData);
			if(verbose)
				IJ.log(evaluation.toSummaryString("\n=== Training set evaluation ===\n", false));
			error = evaluation.errorRate();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return error;
	}

	/**
	 * Get test error of current classifier on a specific image and its binary labels
	 *
	 * @param image input image
	 * @param labels binary labels
	 * @param whiteClassIndex index of the white class
	 * @param blackClassIndex index of the black class
	 * @param verbose option to display evaluation information in the log window
	 * @return pixel classification error
	 */
	public double getTestError(
			ImagePlus image,
			ImagePlus labels,
			int whiteClassIndex,
			int blackClassIndex,
			boolean verbose)
	{
		IJ.showStatus("Creating features for test image...");
		if(verbose)
			IJ.log("Creating features for test image " + image.getTitle() +  "...");


		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[0].get(i).size() > 0)
					classNames.add(getClassLabels()[i]);
		}
		else
			classNames = loadedClassNames;


		// Apply labels
		final int height = image.getHeight();
		final int width = image.getWidth();
		final int depth = image.getStackSize();

		Instances testData = null;

		for(int z=1; z <= depth; z++)
		{
			final ImagePlus testSlice = new ImagePlus(image.getImageStack().getSliceLabel(z), image.getImageStack().getProcessor(z));
			// Create feature stack for test image
			IJ.showStatus("Creating features for test image (slice "+z+")...");
			if(verbose)
				IJ.log("Creating features for test image (slice "+z+")...");
			final FeatureStack testImageFeatures = new FeatureStack(testSlice);
			// Use the same features as the current classifier
			testImageFeatures.setEnabledFeatures(featureStackArray.getEnabledFeatures());
			testImageFeatures.setMaximumSigma(maximumSigma);
			testImageFeatures.setMinimumSigma(minimumSigma);
			testImageFeatures.setMembranePatchSize(membranePatchSize);
			testImageFeatures.setMembraneSize(membraneThickness);
			testImageFeatures.updateFeaturesMT();
			testImageFeatures.setUseNeighbors(featureStackArray.useNeighborhood());
			filterFeatureStackByList(this.featureNames, testImageFeatures);

			final Instances data = testImageFeatures.createInstances(classNames);
			data.setClassIndex(data.numAttributes()-1);
			if(verbose)
				IJ.log("Assigning classes based on the labels...");

			final ImageProcessor slice = labels.getImageStack().getProcessor(z);
			for(int n=0, y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
				{
					final double newValue = slice.getPixel(x, y) > 0 ? whiteClassIndex : blackClassIndex;
					data.get(n).setClassValue(newValue);
				}

			if(null == testData)
				testData = data;
			else
			{
				for(int i=0; i<data.numInstances(); i++)
					testData.add( data.get(i) );
			}
		}
		if(verbose)
			IJ.log("Evaluating test data...");

		double error = -1;
		try {
			final Evaluation evaluation = new Evaluation(testData);
			evaluation.evaluateModel(classifier, testData);
			if(verbose)
			{
				IJ.log(evaluation.toSummaryString("\n=== Test data evaluation ===\n", false));
				IJ.log(evaluation.toClassDetailsString() + "\n");
				IJ.log(evaluation.toMatrixString());
			}
			error = evaluation.errorRate();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return error;
	}

	/**
	 * Get the confusion matrix for an input image and its expected labels
	 *
	 * @param image input image
	 * @param expectedLabels binary labels
	 * @param whiteClassIndex index of the white class
	 * @param blackClassIndex index of the black class
	 * @return confusion matrix
	 */
	public int[][] getTestConfusionMatrix(
			ImagePlus image,
			ImagePlus expectedLabels,
			int whiteClassIndex,
			int blackClassIndex)
	{
	
		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[0].get(i).size() > 0)
					classNames.add(getClassLabels()[i]);
		}
		else
			classNames = loadedClassNames;


		// Apply current classifier
		ImagePlus resultLabels = applyClassifier(image, 0, false);
		
		//resultLabels.show();
		
		return getConfusionMatrix(resultLabels, expectedLabels, whiteClassIndex, blackClassIndex);
	}

	/**
	 * Get confusion matrix (binary images)
	 * @param proposedLabels proposed binary labels
	 * @param expectedLabels original binary labels
	 * @param whiteClassIndex index of white class
	 * @param blackClassIndex index of black class
	 * @return confusion matrix
	 */
	public int[][] getConfusionMatrix(
			ImagePlus proposedLabels,
			ImagePlus expectedLabels, 
			int whiteClassIndex, 
			int blackClassIndex) 
	{
		int[][] confusionMatrix = new int[2][2];
		
		// Compare labels
		final int height = proposedLabels.getHeight();
		final int width = proposedLabels.getWidth();
		final int depth = proposedLabels.getStackSize();
		

		for(int z=1; z <= depth; z++)
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++)
				{
					if( expectedLabels.getImageStack().getProcessor(z).get(x, y) > 0)
					{
						if( proposedLabels.getImageStack().getProcessor(z).get(x, y) > 0 )
							confusionMatrix[whiteClassIndex][whiteClassIndex] ++; 							                                 
						else
							confusionMatrix[whiteClassIndex][blackClassIndex] ++; 
					}
					else
					{
						if( proposedLabels.getImageStack().getProcessor(z).get(x, y) > 0 )
							confusionMatrix[blackClassIndex][whiteClassIndex] ++; 							                                 
						else
							confusionMatrix[blackClassIndex][blackClassIndex] ++;							
					}
				}
		
		return confusionMatrix;
	}
	
	/**
	 * Get confusion matrix (2 classes)
	 * @param proposal probability image
	 * @param expectedLabels original labels
	 * @param threshold binary threshold to be applied to proposal
	 * @return confusion matrix
	 */
	public static int[][] getConfusionMatrix(
			ImagePlus proposal,
			ImagePlus expectedLabels, 
			double threshold) 
	{
		int[][] confusionMatrix = new int[2][2];
		
		final int depth = proposal.getStackSize();
		
		ExecutorService exe = Executors.newFixedThreadPool( Prefs.getThreads() );
		ArrayList< Future <int[][]>  > fu = new ArrayList<Future <int[][]>>();		
		
		// Compare labels
		for(int z=1; z <= depth; z++)
		{
			fu.add( exe.submit( confusionMatrixBinarySlice(proposal.getImageStack().getProcessor( z ), expectedLabels.getImageStack().getProcessor( z ), threshold)) ); 
		}
				 		
		for(int z=0; z < depth; z++)
		{
			try {
				int[][] temp = fu.get( z ).get();
				for(int i=0 ; i<2; i++)
					for(int j=0 ; j<2; j++)
						confusionMatrix[i][j] += temp[i][j];
						
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			finally{
				exe.shutdown();
			}
		}
		
		
		return confusionMatrix;
	}
	
	
	/**
	 * Calculate the confusion matrix of a slice (2 classes)
	 * @param proposal probability image (single 2D slice)
	 * @param expectedLabels original binary labels
	 * @param threshold threshold to apply to proposal
	 * @return confusion matrix (first row: black, second raw: white)
	 */
	public static Callable<int[][]> confusionMatrixBinarySlice(
			final ImageProcessor proposal,
			final ImageProcessor expectedLabels, 
			final double threshold)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<int[][]>(){
			public int[][] call()
			{
				int[][] confusionMatrix = new int[2][2];
				for(int y=0; y<proposal.getHeight(); y++)
					for(int x=0; x<proposal.getWidth(); x++)
					{
						double pix = proposal.getPixelValue(x, y) > threshold ? 1.0 : 0.0; 
						
						if( expectedLabels.get(x, y) > 0)
						{
							if( pix > 0 )
								confusionMatrix[1][1] ++; 							                                 
							else
								confusionMatrix[1][0] ++; 
						}
						else
						{
							if( pix > 0 )
								confusionMatrix[0][1] ++; 							                                 
							else
								confusionMatrix[0][0] ++;							
						}
					}
				return confusionMatrix;
			}
		};
	}
	
	
	/**
	 * Get the confusion matrix for an input image and its expected labels
	 *
	 * @param image input image
	 * @param filters stack of filters to apply to the original image in order to create the features
	 * @param expectedLabels binary labels
	 * @param whiteClassIndex index of the white class
	 * @param blackClassIndex index of the black class
	 * @return confusion matrix
	 */
	public int[][] getTestConfusionMatrix(
			ImagePlus image,
			ImagePlus filters,
			ImagePlus expectedLabels,
			int whiteClassIndex,
			int blackClassIndex)
	{
	
		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[0].get(i).size() > 0)
					classNames.add(getClassLabels()[i]);
		}
		else
			classNames = loadedClassNames;


		// Apply current classifier
		ImagePlus resultLabels = applyClassifier(image, filters, 0, false);
		
		//resultLabels.show();
		
		return getConfusionMatrix(resultLabels, expectedLabels, whiteClassIndex, blackClassIndex);
	}
	
	
	/**
	 * Get test error of current classifier on a specific image and its binary labels
	 *
	 * @param image input image
	 * @param labels binary labels
	 * @param filters list of filters to create features
	 * @param whiteClassIndex index of the white class
	 * @param blackClassIndex index of the black class
	 * @param verbose option to display evaluation information in the log window
	 * @return pixel classification error
	 */
	public double getTestError(
			ImagePlus image,
			ImagePlus labels,
			ImagePlus filters,
			int whiteClassIndex,
			int blackClassIndex,
			boolean verbose)
	{
		IJ.showStatus("Creating features for test image...");
		if(verbose)
			IJ.log("Creating features for test image " + image.getTitle() +  "...");


		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[0].get(i).size() > 0)
					classNames.add(getClassLabels()[i]);
		}
		else
			classNames = loadedClassNames;


		// Apply labels
		final int height = image.getHeight();
		final int width = image.getWidth();
		final int depth = image.getStackSize();

		Instances testData = null;

		for(int z=1; z <= depth; z++)
		{
			final ImagePlus testSlice = new ImagePlus(image.getImageStack().getSliceLabel(z), image.getImageStack().getProcessor(z));
			// Create feature stack for test image
			IJ.showStatus("Creating features for test image...");
			if(verbose)
				IJ.log("Creating features for test image " + z +  "...");
			final FeatureStack testImageFeatures = new FeatureStack(testSlice);
			// Create features by applying the filters
			testImageFeatures.addFeaturesMT(filters);

			final Instances data = testImageFeatures.createInstances(classNames);
			data.setClassIndex(data.numAttributes()-1);
			if(verbose)
				IJ.log("Assigning classes based on the labels...");

			final ImageProcessor slice = labels.getImageStack().getProcessor(z);
			for(int n=0, y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
				{
					final double newValue = slice.getPixel(x, y) > 0 ? whiteClassIndex : blackClassIndex;
					data.get(n).setClassValue(newValue);
				}

			if(null == testData)
				testData = data;
			else
			{
				for(int i=0; i<data.numInstances(); i++)
					testData.add( data.get(i) );
			}
		}
		if(verbose)
			IJ.log("Evaluating test data...");

		double error = -1;
		try {
			final Evaluation evaluation = new Evaluation(testData);
			evaluation.evaluateModel(classifier, testData);
			if(verbose)
			{
				IJ.log(evaluation.toSummaryString("\n=== Test data evaluation ===\n", false));
				IJ.log(evaluation.toClassDetailsString() + "\n");
				IJ.log(evaluation.toMatrixString());
			}
			error = evaluation.errorRate();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return error;
	}	
	

	/**
	 * Update the class attribute of "loadedTrainingData" from
	 * the input binary labels. The number of instances of "loadedTrainingData"
	 * must match the size of the input labels image (or stack)
	 *
	 * @param labels input binary labels (single image or stack)
	 * @param className1 name of the white (different from 0) class
	 * @param className2 name of the black (0) class
	 */
	public void udpateDataClassification(
			ImagePlus labels,
			String className1,
			String className2)
	{

		// Detect class indexes
		int classIndex1 = 0;
		for(classIndex1 = 0 ; classIndex1 < this.getClassLabels().length; classIndex1++)
			if(className1.equalsIgnoreCase(this.getClassLabels()[classIndex1]))
				break;
		if(classIndex1 == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.getClassLabels().length; classIndex2++)
			if(className2.equalsIgnoreCase(this.getClassLabels()[classIndex2]))
				break;
		if(classIndex2 == this.getClassLabels().length)
		{
			IJ.log("Error: class named '" + className2 + "' not found.");
			return;
		}

		updateDataClassification(this.loadedTrainingData, labels, classIndex1, classIndex2);
	}

	/**
	 * Update the class attribute of "data" from
	 * the input binary labels. The number of instances of "data"
	 * must match the size of the input labels image (or stack)
	 *
	 * @param data input instances
	 * @param labels binary labels
	 * @param classIndex1 index of the white (different from 0) class
	 * @param classIndex2 index of the black (0) class
	 */
	public static void updateDataClassification(
			Instances data,
			ImagePlus labels,
			int classIndex1,
			int classIndex2)
	{
		// Check sizes
		final int size = labels.getWidth() * labels.getHeight() * labels.getStackSize();
		if (size != data.numInstances())
		{
			IJ.log("Error: labels size does not match loaded training data set size.");
			return;
		}

		final int width = labels.getWidth();
		final int height = labels.getHeight();
		final int depth = labels.getStackSize();
		// Update class with new labels
		for(int n=0, z=1; z <= depth; z++)
		{
			final ImageProcessor slice = labels.getImageStack().getProcessor(z);
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
					data.get(n).setClassValue(slice.getPixel(x, y) > 0 ? classIndex1 : classIndex2);

		}
	}

	/**
	 * Update the class attribute of "data" from
	 * the input binary labels. The number of instances of "data"
	 * must match the size of the input labels image (or stack)
	 *
	 * @param data input instances
	 * @param labels binary labels
	 * @param classIndex1 index of the white (different from 0) class
	 * @param classIndex2 index of the black (0) class
	 */
	public static void updateDataClassification(
			Instances data,
			ImagePlus labels,
			int classIndex1,
			int classIndex2,
			ArrayList<Point3f>[] mismatches)
	{
		// Check sizes
		final int size = labels.getWidth() * labels.getHeight() * labels.getStackSize();
		if (size != data.numInstances())
		{
			IJ.log("Error: labels size does not match loaded training data set size.");
			return;
		}

		final int width = labels.getWidth();
		final int height = labels.getHeight();
		final int depth = labels.getStackSize();
		// Update class with new labels
		for(int n=0, z=1; z <= depth; z++)
		{
			final ImageProcessor slice = labels.getImageStack().getProcessor(z);
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
				{
					final double newValue = slice.getPixel(x, y) > 0 ? classIndex1 : classIndex2;
					/*
					// reward matching with previous value...
					if(data.get(n).classValue() == newValue)
					{
						double weight = data.get(n).weight();
						data.get(n).setWeight(++weight);
					}
					*/
					data.get(n).setClassValue(newValue);
				}

		}
		/*
		if(null !=  mismatches)
			for(int i=0; i<depth; i++)
			{
				IJ.log("slice " + i + ": " + mismatches[i].size() + " mismatches");

				for(Point3f p : mismatches[i])
				{
					//IJ.log("point = " + p);
					final int n = (int) p.x + ((int) p.y -1) * width + i * (width*height);
					double weight = data.get(n).weight();
					data.get(n).setWeight(++weight);
				}
			}
			*/
	}
	
	/**
	 * Read ARFF file
	 * @param filename ARFF file name
	 * @return set of instances read from the file
	 */
	public Instances readDataFromARFF(String filename){
		try{
			BufferedReader reader = new BufferedReader(
					new FileReader(filename));
			try{
				Instances data = new Instances(reader);
				// setting class attribute
				data.setClassIndex(data.numAttributes() - 1);
				reader.close();
				return data;
			}
			catch(IOException e){IJ.showMessage("IOException");}
		}
		catch(FileNotFoundException e){IJ.showMessage("File not found!");}
		return null;
	}

	/**
	 * Write current instances into an ARFF file
	 * @param data set of instances
	 * @param filename ARFF file name
	 */
	public boolean writeDataToARFF(Instances data, String filename)
	{
		BufferedWriter out = null;
		try{
			out = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream( filename ) ) );

			final Instances header = new Instances(data, 0);
			out.write(header.toString());

			for(int i = 0; i < data.numInstances(); i++)
			{
				out.write(data.get(i).toString()+"\n");
			}
		}
		catch(Exception e)
		{
			IJ.log("Error: couldn't write instances into .ARFF file.");
			IJ.showMessage("Exception while saving data as ARFF file");
			e.printStackTrace();
			return false;
		}
		finally{
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;

	}

	/**
	 * Adjust current segmentation state (attributes and classes) to
	 * loaded data
	 * @param data loaded instances
	 * @return false if error
	 */
	public boolean adjustSegmentationStateToData(Instances data)
	{
		// Check the features that were used in the loaded data
		boolean featuresChanged = false;
		Enumeration<Attribute> attributes = data.enumerateAttributes();
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];

		// Initialize list of names for the features to use
		this.featureNames = new ArrayList<String>();

		float minSigma = Float.MAX_VALUE;
		float maxSigma = Float.MIN_VALUE;

		while(attributes.hasMoreElements())
		{
			final Attribute a = attributes.nextElement();
			this.featureNames.add(a.name());
			for(int i = 0 ; i < numFeatures; i++)
			{
				if(a.name().startsWith(FeatureStack.availableFeatures[i]))
				{
					usedFeatures[i] = true;
					if(i == FeatureStack.MEMBRANE)
					{
						int index = a.name().indexOf("s_") + 4;
						int index2 = a.name().indexOf("_", index+1 );
						final int patchSize = Integer.parseInt(a.name().substring(index, index2));
						if(patchSize != membranePatchSize)
						{
							membranePatchSize = patchSize;
							this.featureStackArray.setMembranePatchSize(patchSize);
							featuresChanged = true;
						}
						index = a.name().lastIndexOf("_");
						final int thickness = Integer.parseInt(a.name().substring(index+1));
						if(thickness != membraneThickness)
						{
							membraneThickness = thickness;
							this.featureStackArray.setMembraneSize(thickness);
							featuresChanged = true;
						}

					}
					else if(i < FeatureStack.ANISOTROPIC_DIFFUSION)
					{
						String[] tokens = a.name().split("_");
						for(int j=0; j<tokens.length; j++)
							if(tokens[j].indexOf(".") != -1)
							{
								final float sigma = Float.parseFloat(tokens[j]);
								if(sigma < minSigma)
									minSigma = sigma;
								if(sigma > maxSigma)
									maxSigma = sigma;
							}
					}
				}
			}
		}

		IJ.log("Field of view: max sigma = " + maxSigma + ", min sigma = " + minSigma);
		IJ.log("Membrane thickness: " + membraneThickness + ", patch size: " + membranePatchSize);
		if(minSigma != this.minimumSigma && minSigma != 0)
		{
			this.minimumSigma = minSigma;
			featuresChanged = true;
			this.featureStackArray.setMinimumSigma(minSigma);
		}
		if(maxSigma != this.maximumSigma)
		{
			this.maximumSigma = maxSigma;
			featuresChanged = true;
			this.featureStackArray.setMaximumSigma(maxSigma);
		}

		// Check if classes match
		Attribute classAttribute = data.classAttribute();
		Enumeration<String> classValues  = classAttribute.enumerateValues();

		// Update list of names of loaded classes
		loadedClassNames = new ArrayList<String>();

		int j = 0;
		setNumOfClasses(0);

		while(classValues.hasMoreElements())
		{
			final String className = classValues.nextElement().trim();
			loadedClassNames.add(className);
		}

		for(String className : loadedClassNames)
		{
			IJ.log("Read class name: " + className);

			setClassLabel(j, className);
			addClass();
			j++;
		}

		final boolean[] oldEnableFeatures = this.featureStackArray.getEnabledFeatures();
		// Read checked features and check if any of them changed
		for(int i = 0; i < numFeatures; i++)
		{
			if (usedFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}
		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			this.setEnabledFeatures( usedFeatures );
			// Force features to be updated
			updateFeatures = true;
		}

		return true;
	}

	/**
	 * Create training instances out of the user markings
	 * @return set of instances (feature vectors in Weka format)
	 */
	public Instances createTrainingInstances()
	{
		//IJ.log("create training instances: num of features = " + featureStackArray.getNumOfFeatures());

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=featureStackArray.getNumOfFeatures(); i++)
		{
			String attString = featureStackArray.getLabel(i);
			attributes.add(new Attribute(attString));
			//IJ.log("Add attribute " + attString);
		}

		final ArrayList<String> classes;

		int numOfInstances = 0;
		int numOfUsedClasses = 0;
		if(null == this.loadedTrainingData)
		{
			classes = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
			{
				// Do not add empty lists
				for(int n=0; n<trainingImage.getImageStackSize(); n++)
				{
					if(examples[n].get(i).size() > 0)
					{
						if(classes.contains(getClassLabels()[i]) == false)
							classes.add(getClassLabels()[i]);
						numOfUsedClasses++;
					}									
					numOfInstances += examples[n].get(i).size();
				}
			}
		}
		else
		{
			classes = this.loadedClassNames;
		}


		attributes.add(new Attribute("class", classes));
		/*
		IJ.log("added class attribute with values:");
		for(int i=0; i<classes.size(); i++)
			IJ.log("  " + classes.get(i));
		*/
		final Instances trainingData =  new Instances("segment", attributes, numOfInstances);

		IJ.log("Training input:");

		final boolean colorFeatures = this.trainingImage.getType() == ImagePlus.COLOR_RGB;
		
		// For all classes
		for(int l = 0; l < numOfClasses; l++)
		{
			int nl = 0;
			// Read all lists of examples
			for(int sliceNum = 1; sliceNum <= trainingImage.getImageStackSize(); sliceNum ++)
				for(int j=0; j<examples[sliceNum-1].get(l).size(); j++)
				{
					Roi r = examples[sliceNum-1].get(l).get(j);

					// For polygon rois we get the list of points
					if( r instanceof PolygonRoi && r.getType() == Roi.FREELINE )
					{
						if(r.getStrokeWidth() == 1)
						{
							int[] x = r.getPolygon().xpoints;
							int[] y = r.getPolygon().ypoints;
							final int n = r.getPolygon().npoints;

							for (int i=0; i<n; i++)
							{
								double[] values = new double[featureStackArray.getNumOfFeatures()+1];

								for (int z=1; z<=featureStackArray.getNumOfFeatures(); z++)
									values[z-1] = featureStackArray.get(sliceNum-1).getProcessor(z).getPixelValue(x[i], y[i]);
								
								values[featureStackArray.getNumOfFeatures()] = (double) l;
								trainingData.add(new DenseInstance(1.0, values));
								// increase number of instances for this class
								nl ++;
							}
						}
						else // For thicker lines, include also neighbors
						{
							final int width = (int) Math.round(r.getStrokeWidth());
							FloatPolygon p = r.getFloatPolygon();
							int n = p.npoints;

							double x1, y1;
							double x2=p.xpoints[0]-(p.xpoints[1]-p.xpoints[0]);
							double y2=p.ypoints[0]-(p.ypoints[1]-p.ypoints[0]);
							for (int i=0; i<n; i++)
							{
								x1 = x2;
								y1 = y2;
								x2 = p.xpoints[i];
								y2 = p.ypoints[i];

								double dx = x2-x1;
								double dy = y1-y2;
								double length = (float)Math.sqrt(dx*dx+dy*dy);
								dx /= length;
								dy /= length;
								double x = x2-dy*width/2.0;
								double y = y2-dx*width/2.0;

								int n2 = width;
								do {
									if(x >= 0 && x < featureStackArray.get(sliceNum-1).getWidth() 
											&& y >= 0 && y <featureStackArray.get(sliceNum-1).getHeight())
									{
										double[] values = new double[featureStackArray.getNumOfFeatures()+1];
										if(colorFeatures)
											for (int z=1; z<=featureStackArray.getNumOfFeatures(); z++)
												values[z-1] = featureStackArray.get(sliceNum-1).getProcessor(z).getInterpolatedPixel(x, y);
										else
											for (int z=1; z<=featureStackArray.getNumOfFeatures(); z++)
												values[z-1] = featureStackArray.get(sliceNum-1).getProcessor(z).getInterpolatedValue(x, y);
										values[featureStackArray.getNumOfFeatures()] = (double) l;
										trainingData.add(new DenseInstance(1.0, values));
										// increase number of instances for this class
										nl ++;
									}
									x += dy;
									y += dx;
								} while (--n2>0);
							}

						}
					}
					else // for the rest of rois we get ALL points inside the roi
					{
						final ShapeRoi shapeRoi = new ShapeRoi(r);
						final Rectangle rect = shapeRoi.getBounds();

						final int lastX = rect.x + rect.width;
						final int lastY = rect.y + rect.height;

						for(int x = rect.x; x < lastX; x++)
							for(int y = rect.y; y < lastY; y++)
								if(shapeRoi.contains(x, y))
								{
									double[] values = new double[featureStackArray.getNumOfFeatures()+1];
									for (int z=1; z<=featureStackArray.getNumOfFeatures(); z++)
										values[z-1] = featureStackArray.get(sliceNum-1).getProcessor(z).getPixelValue(x, y);
									values[featureStackArray.getNumOfFeatures()] = (double) l;
									trainingData.add(new DenseInstance(1.0, values));
									// increase number of instances for this class
									nl ++;
								}
					}


				}

			IJ.log("# of pixels selected as " + getClassLabels()[l] + ": " +nl);
		}

		if (trainingData.numInstances() == 0)
			return null;

		// Set the index of the class attribute
		trainingData.setClassIndex(featureStackArray.getNumOfFeatures());

		return trainingData;
	}

	/**
	 * Create instances of a feature stack (to be submitted to an Executor Service)
	 * 
	 * @param classNames names of the classes of data
	 * @param featureStack feature stack to create the instances from
	 * @return set of instances
	 */
	public Callable<Instances> createInstances(
			final ArrayList<String> classNames,
			final FeatureStack featureStack)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<Instances>(){
			public Instances call()
			{
				return featureStack.createInstances(classNames);
			}
		};
	}
	
	
	/**
	 * Train classifier with the current instances
	 */
	public boolean trainClassifier()
	{
		if (Thread.currentThread().isInterrupted() )
		{
			IJ.log("Classifier training was interrupted.");
			return false;
		}
		
		// At least two lists of different classes of examples need to be non empty
		int nonEmpty = 0;
		for(int i = 0; i < numOfClasses; i++)
			for(int j=0; j<trainingImage.getImageStackSize(); j++)
				if(examples[j].get(i).size() > 0)
				{
					nonEmpty++;
					break;
				}
		
		if (nonEmpty < 2 && null == loadedTrainingData)
		{
			IJ.showMessage("Cannot train without at least 2 sets of examples!");
			return false;
		}

		// Create feature stack if necessary (training from traces
		// and the features stack is empty or the settings changed)
		if(nonEmpty > 1 && featureStackArray.isEmpty() || updateFeatures)
		{
			IJ.showStatus("Creating feature stack...");
			IJ.log("Creating feature stack...");
			long start = System.currentTimeMillis();
			if ( false == featureStackArray.updateFeaturesMT(featureStackToUpdateTrain) )
			{
				IJ.log("Feature stack was not updated.");
				IJ.showStatus("Feature stack was not updated.");
				return false;
			}
			Arrays.fill(featureStackToUpdateTrain, false);
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			long end = System.currentTimeMillis();
			IJ.log("Feature stack array is now updated (" + featureStackArray.getSize() 
					+ " slice(s) with " + featureStackArray.getNumOfFeatures() 
					+ " features, took " + (end-start) + "ms).");
		}

		IJ.showStatus("Creating training instances...");
		Instances data = null;
		if (nonEmpty < 1)
			IJ.log("Training from loaded data only...");
		else
		{
			final long start = System.currentTimeMillis();

			traceTrainingData = data = createTrainingInstances();

			final long end = System.currentTimeMillis();
			IJ.log("Creating training data took: " + (end-start) + "ms");
		}

		if (loadedTrainingData != null && data != null)
		{
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++)
				data.add(loadedTrainingData.instance(i));
			IJ.log("Finished: total number of instances = " + data.numInstances());
		}
		else if (data == null)
		{
			data = loadedTrainingData;
			IJ.log("Taking loaded data as only data...");
		}

		if (null == data){
			IJ.log("WTF");
		}

		// Update train header
		this.trainHeader = new Instances(data, 0);

		// Resample data if necessary
		if(homogenizeClasses)
		{
			final long start = System.currentTimeMillis();
			IJ.showStatus("Homogenizing classes distribution...");
			IJ.log("Homogenizing classes distribution...");
			data = homogenizeTrainingData(data);
			final long end = System.currentTimeMillis();
			IJ.log("Done. Homogenizing classes distribution took: " + (end-start) + "ms");
		}

		IJ.showStatus("Training classifier...");
		IJ.log("Training classifier...");

		if (Thread.currentThread().isInterrupted() )
		{
			IJ.log("Classifier training was interrupted.");
			return false;
		}
		
		// Train the classifier on the current data
		final long start = System.currentTimeMillis();
		try{
			classifier.buildClassifier(data);
		}
		catch (InterruptedException ie)
		{
			IJ.log("Classifier construction was interrupted.");
			return false;
		}
		catch(Exception e){
			IJ.showMessage(e.getMessage());
			e.printStackTrace();
			return false;
		}

		// Print classifier information
		IJ.log( this.classifier.toString() );

		final long end = System.currentTimeMillis();

		IJ.log("Finished training in "+(end-start)+"ms");
		return true;
	}

	/**
	 * Apply current classifier to a given image.
	 *
	 * @param imp image (2D single image or stack)
	 * @return result image (classification)
	 */
	public ImagePlus applyClassifier(final ImagePlus imp)
	{
		return applyClassifier(imp, 0, false);
	}

	
	/**
	 * Apply current classifier to a given image. It divides the
	 * whole slices of the input image into the available CPUs.
	 *
	 * @param imp image (2D single image or stack)
	 * @param numThreads The number of threads to use. Set to zero for
	 * auto-detection.
	 * @param probabilityMaps create probability maps for each class instead of
	 * a classification
	 * @return result image
	 */
	public ImagePlus applyClassifier(
			final ImagePlus imp, 
			int numThreads, 
			final boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Prefs.getThreads();

		final int numSliceThreads = Math.min(imp.getStackSize(), numThreads);
		final int numClasses      = numOfClasses;
		final int numChannels     = (probabilityMaps ? numClasses : 1);

		IJ.log("Processing slices of " + imp.getTitle() + " in " + numSliceThreads + " thread(s)...");

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				for(int j=0; j<trainingImage.getImageStackSize(); j++)
					if(examples[j].get(i).size() > 0)
					{
						classNames.add(getClassLabels()[i]);
						break;
					}
		}
		else
			classNames = loadedClassNames;

		final ImagePlus[] classifiedSlices = new ImagePlus[imp.getStackSize()];

		class ApplyClassifierThread extends Thread 
		{

			final int startSlice;
			final int numSlices;
			final int numFurtherThreads;
			final ArrayList<String> classNames;
			
			public ApplyClassifierThread(
					int startSlice, 
					int numSlices, 
					int numFurtherThreads, 
					ArrayList<String> classNames) 
			{

				this.startSlice         = startSlice;
				this.numSlices          = numSlices;
				this.numFurtherThreads  = numFurtherThreads;
				this.classNames         = classNames;
			}

			public void run() 
			{

				for (int i = startSlice; i < startSlice + numSlices; i++)
				{
					final ImagePlus slice = new ImagePlus(imp.getImageStack().getSliceLabel(i), imp.getImageStack().getProcessor(i));
                    // Create feature stack for slice
                    IJ.showStatus("Creating features...");
                    IJ.log("Creating features for slice " + i +  "...");
                    final FeatureStack sliceFeatures = new FeatureStack(slice);
                    // Use the same features as the current classifier
                    sliceFeatures.setEnabledFeatures(featureStackArray.getEnabledFeatures());
                    sliceFeatures.setMaximumSigma(maximumSigma);
                    sliceFeatures.setMinimumSigma(minimumSigma);
                    sliceFeatures.setMembranePatchSize(membranePatchSize);
                    sliceFeatures.setMembraneSize(membraneThickness);
                    sliceFeatures.updateFeaturesST();
                    filterFeatureStackByList(featureNames, sliceFeatures);
 
                    final Instances sliceData = sliceFeatures.createInstances(classNames);
                    sliceData.setClassIndex(sliceData.numAttributes() - 1); 

					IJ.log("Classifying slice " + i + " in " + numFurtherThreads + " thread(s)...");
					final ImagePlus classImage = applyClassifier(sliceData, slice.getWidth(), slice.getHeight(), numFurtherThreads, probabilityMaps);
					
					if( null == classImage )
					{
						IJ.log("Error while applying classifier!");
						return;
					}
											
					classImage.setTitle("classified_" + slice.getTitle());
					if(probabilityMaps)
						classImage.setProcessor(classImage.getProcessor().duplicate());
					else
						classImage.setProcessor(classImage.getProcessor().convertToByte(true).duplicate());
					classifiedSlices[i-1] = classImage;
				}
			}
		}

		final int numFurtherThreads = (int)Math.ceil((double)(numThreads - numSliceThreads)/numSliceThreads) + 1;
		final ApplyClassifierThread[] threads = new ApplyClassifierThread[numSliceThreads];

		// calculate optimum number of slices per thread
		int[] numSlicesPerThread = new int [ numSliceThreads ];
		for(int i=0; i<imp.getImageStackSize(); i++)
		{
			numSlicesPerThread[ i % numSliceThreads ] ++;
		}
		
		int aux = 0;
		for (int i = 0; i < numSliceThreads; i++) 
		{

			int startSlice = aux + 1;
			
			aux += numSlicesPerThread[ i ];
									
			IJ.log("Starting thread " + i + " processing " + numSlicesPerThread[ i ] + " slices, starting with " + startSlice);
			threads[i] = new ApplyClassifierThread(startSlice, numSlicesPerThread[ i ], numFurtherThreads, classNames );

			threads[i].start();
		}

		// create classified image
		final ImageStack classified = new ImageStack(imp.getWidth(), imp.getHeight());

		// join threads
		for(Thread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		// assemble classified image
		for (int i = 0; i < imp.getStackSize(); i++)
			for (int c = 0; c < numChannels; c++)
				classified.addSlice("", classifiedSlices[i].getStack().getProcessor(c+1));

		ImagePlus result = new ImagePlus("Classification result", classified);

		if (probabilityMaps)
		{
			result.setDimensions(numOfClasses, imp.getNSlices(), imp.getNFrames());
			if (imp.getNSlices()*imp.getNFrames() > 1)
				result.setOpenAsHyperStack(true);
		}

		return result;
	}
	
	
	/**
	 * Apply current classifier to a given image in a complete concurrent way.
	 * This method is experimental, it divides the image(s) in pieces and that
	 * can cause artifacts using some filters.
	 *
	 * @param imp image (2D single image or stack)
	 * @param numThreads The number of threads to use. Set to zero for
	 * auto-detection.
	 * @param probabilityMaps create probability maps for each class instead of
	 * a classification
	 * @return result image
	 */
	public ImagePlus applyClassifierMT(
			final ImagePlus imp, 
			int numThreads, 
			final boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Prefs.getThreads();
		
		
		final int numClasses = numOfClasses;
		final int numChannels = (probabilityMaps ? numClasses : 1);		

		IJ.log("Classifying data from image " + imp.getTitle() + " using " + numThreads + " thread(s)...");

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				for(int j=0; j<trainingImage.getImageStackSize(); j++)
					if(examples[j].get(i).size() > 0)
					{
						classNames.add(getClassLabels()[i]);
						break;
					}
		}
		else
			classNames = loadedClassNames;
		
		// Create instances information (each instance needs a pointer to this)
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=featureStackArray.getNumOfFeatures(); i++)
		{
			String attString = featureStackArray.getLabel(i);
			attributes.add(new Attribute(attString));
		}

		if(featureStackArray.useNeighborhood())
			for (int i=0; i<8; i++)
			{
				IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
				attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
			}
		
		attributes.add(new Attribute("class", classNames));
		Instances dataInfo = new Instances("segment", attributes, 1);
		dataInfo.setClassIndex(dataInfo.numAttributes()-1);
				
		final long start = System.currentTimeMillis();

		// Initialize executor service
		if(exe.isShutdown())
			exe = Executors.newFixedThreadPool(numThreads);
		
	
		// counter to display the progress
		final AtomicInteger counter = new AtomicInteger();

		// slice dimensions
		final int height = imp.getHeight(); 
		final int width = imp.getWidth();
		final int pad = (int) maximumSigma; 
		
		// Calculate number of rows per thread
		// (with this division we may miss one row, 
		// but it will be added to the last thread)
		int numOfRows = height * imp.getImageStackSize() / numThreads; 				
		
		// set each slice in a thread
		Future<ArrayList <ImagePlus> > fu[] = new Future [ numThreads ];
		
		ArrayList<int[]> imagePad = new ArrayList<int[]>();
		
		ArrayList <ImagePlus> list[] = new ArrayList [ numThreads ];
		
		// Divide work among available threads
		//IJ.log("Dividing image data among the " + numThreads + " available threads...");
		//final long time1 = System.currentTimeMillis();
		for(int i = 0; i < numThreads; i++)
		{
			list[ i ] = new ArrayList < ImagePlus > ();
			if (Thread.currentThread().isInterrupted()) 
				return null;
			
			// Calculate list of images to be classified on each thread
			int firstRow = i * numOfRows;
			int lastRow = i < (numThreads-1) ? (i+1) * numOfRows - 1 : height * imp.getImageStackSize()-1;
			
			//IJ.log("Thread " + i + ": first row = " + firstRow + ", last row = " + lastRow);		
			
			
			int r = firstRow;
			int rowsToDo = lastRow - firstRow + 1;
			
			while( r < lastRow )
			{
				final int slice = r / height;
				final int begin = r - slice * height;
				
				final int end = (begin + rowsToDo) > height ? height-1 : begin + rowsToDo-1; 
													
				// Create image
				ImageProcessor sliceImage = imp.getImageStack().getProcessor(slice+1);
				
				// We pad the images if necessary (for the filtering)
				final int paddedBegin = begin - pad;
				final int paddedEnd = end + pad;
				
				// Crop the area of the slice that will be process on this thread
				sliceImage.setRoi(new Rectangle(0, paddedBegin, width, paddedEnd-paddedBegin+1 ) );
				ImageProcessor im = sliceImage.crop();
				
				final ImagePlus ip = new ImagePlus( "slice-" + slice + "-" + begin, im); 
				// add image to list
				list[ i ].add( ip );
				
				//IJ.log(" begin = " + begin + ", end = " + end + ", paddedBegin = " + paddedBegin + ", paddedEnd = " + paddedEnd + ", height = " + height + ", pad = " + pad);				
				
				// We store the padding number to recover the area of interest later
				final int padTop = (paddedBegin >= 0) ? pad : pad + paddedBegin ;
				final int padBottom = (paddedEnd < height) ? pad : pad - (paddedEnd - height + 1);
				
				//IJ.log(" padTop = " + padTop + ", padBottom = " + padBottom );
				
				imagePad.add( new int[]{slice,  		/* slice number (starting at 0) */
										padTop, 		/* top padding */
										padBottom, 		/* bottom padding */
										end-begin+1} );	/* size (number of rows) */
				
				int rowsDone = end-begin+1;
				r += rowsDone;
				rowsToDo -= rowsDone;
			}
						
			
			
		}
		//final long time2 = System.currentTimeMillis();
		//IJ.log(" Done. Image division took " + (time2-time1)  + " ms.");
		
		
		// Create a copy of the classifier for each thread
		AbstractClassifier[] classifierCopy = new AbstractClassifier[ numThreads ];
		IJ.log("Creating classifier copy for each thread...");
		for(int i = 0; i < numThreads; i++)
		{
			
			try {
				// The Weka random forest classifiers do not need to be duplicated on each thread 
				// (that saves much memory)				
				if( classifier instanceof FastRandomForest || classifier instanceof RandomForest )
					classifierCopy[ i ] = classifier;
				else				
					classifierCopy[ i ] = (AbstractClassifier) (AbstractClassifier.makeCopy( classifier ));
				
				
							
			} catch (Exception e) {
				IJ.log("Error: classifier could not be copied to classify in a multi-thread way.");
				e.printStackTrace();
			}
			
		}
		//final long time3 = System.currentTimeMillis();
		//IJ.log(" Done. Classifiers duplication took " + (time3-time2)  + " ms.");
		
		// Submit the jobs		
		for(int i = 0; i < numThreads; i++)
		{
			// classify slice
			fu[i] = exe.submit( classifyListOfImages( list[ i ] , dataInfo, classifierCopy[ i ], counter, probabilityMaps ));
		}

		final int numInstances = imp.getHeight() * imp.getWidth() * imp.getStackSize();
		
		ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
		ScheduledFuture task = monitor.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				IJ.showProgress(counter.get(), numInstances);
			}
		}, 0, 1, TimeUnit.SECONDS);		
		
		// array of images to store the classification results
		final ArrayList< ImagePlus> classifiedImages = new ArrayList < ImagePlus > ();
		
		// Join threads
		for(int i = 0; i < numThreads; i++)
		{
			try {
				ArrayList<ImagePlus> result = fu[i].get();
				for(ImagePlus ip : result)
				{
					classifiedImages.add( ip );
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				task.cancel(true);
				monitor.shutdownNow();
				IJ.showProgress(1);
			}
		}
		
		// create classified image
		final ImageStack classified = new ImageStack(imp.getWidth(), imp.getHeight() );
		for(int i=0; i < imp.getStackSize(); i++)
		{
			if(numChannels > 1)
			{
				for (int c = 0; c < numChannels; c++)
					classified.addSlice("", new FloatProcessor(width, height));
			}
			else
				classified.addSlice("", new ByteProcessor(width, height));
		}
					
		// assemble classified image
		int n = 0;
		int raw = 0;
		for( final ImagePlus ip : classifiedImages )
		{
			raw = raw % height;
			
			//ip.show();
			
			int[] coord = imagePad.get( n );
			
			//final int sliceNum = coord[ 0 ] + 1;
			final int beginPad = coord[ 1 ];
			final int endPad = coord[ 2 ];
			final int size = coord[ 3 ];
			//IJ.log(" coord[0] = " + coord[0] + ", coord[1] = " + coord[1] + ", coord[2] = " + coord[2] + ", coord[3] = " + coord[3] );
			
			for (int c = 0; c < numChannels; c++)
			{
				// target image
				ImageProcessor target = classified.getProcessor(coord[ 0 ] * numChannels + c + 1);
				// source image 
				ImageProcessor source = ip.getImageStack().getProcessor(c+1);
				//IJ.log(" set roi = 0, " + beginPad + ", " + width + ", " + (ip.getHeight() - endPad));
				source.setRoi( new Rectangle( 0, beginPad, width, ip.getHeight() - endPad));
				source = source.crop();
				// copy
				target.copyBits(source, 0, raw, Blitter.COPY);
			}
			raw += size;			
			n++;
		}
		
		
			

		ImagePlus result = new ImagePlus("Classification result", classified);

		if (probabilityMaps)
		{
			result.setDimensions(numOfClasses, imp.getNSlices(), imp.getNFrames());
			if (imp.getNSlices()*imp.getNFrames() > 1)
				result.setOpenAsHyperStack(true);
		}

		final long end = System.currentTimeMillis();
		IJ.log("Whole image classification took " + (end-start) + " ms.");
		return result;
	}
	

	/**
	 * Classify a slice in a concurrent way
	 * @param slice image to classify
	 * @param dataInfo empty set of instances containing the data structure (attributes and classes)
	 * @param classifier classifier to use
	 * @param counter counter used to display the progress in the tool bar
	 * @param probabilityMaps flag to calculate probabilities or binary results
	 * @return classification result
	 */
	public Callable<ImagePlus> classifySlice(
			final ImagePlus slice,
			final Instances dataInfo,
			final AbstractClassifier classifier,
			final AtomicInteger counter,
			final boolean probabilityMaps)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<ImagePlus>(){
			public ImagePlus call()
			{				
				// Create feature stack for slice
				IJ.showStatus("Creating features...");
				IJ.log("Creating features of slice " + slice.getTitle() + "...");
				final FeatureStack sliceFeatures = new FeatureStack(slice);
				// Use the same features as the current classifier
				sliceFeatures.setEnabledFeatures(featureStackArray.getEnabledFeatures());
				sliceFeatures.setMaximumSigma(maximumSigma);
				sliceFeatures.setMinimumSigma(minimumSigma);
				sliceFeatures.setMembranePatchSize(membranePatchSize);
				sliceFeatures.setMembraneSize(membraneThickness);
				if(false == sliceFeatures.updateFeaturesST())
				{
					IJ.log("Classifier execution was interrupted.");
					return null;
				}
				filterFeatureStackByList(featureNames, sliceFeatures);
				
				final int width = slice.getWidth();
				final int height = slice.getHeight();				
				final int numClasses = dataInfo.numClasses();

				ImageStack classificationResult = new ImageStack(width, height);
				
				final int numInstances = width * height;
				
				final double[][] probArray;

				if (probabilityMaps)
					probArray = new double[numClasses][numInstances];
				else
					probArray = new double[1][numInstances];
										
				IJ.log("Classifying slice " + slice.getTitle() + "...");
				
				for (int x=0; x<width; x++)
					for(int y=0; y<height; y++)
					{
						try{

							if (0 == (x+y*width) % 4000)
							{
								if (Thread.currentThread().isInterrupted()) 
									return null;
								counter.addAndGet(4000);
							}
							
							final DenseInstance ins = sliceFeatures.createInstance(x, y, 0);
							ins.setDataset(dataInfo);

							if (probabilityMaps)
							{							
								double[] prob = classifier.distributionForInstance( ins );
								for(int k = 0 ; k < numClasses; k++)
								{
									probArray[k][x+y*width] = prob[ k ];
								}
							}
							else
							{
								probArray[0][ x+y*width ] = classifier.classifyInstance( ins );
							}

						}catch(Exception e){

							IJ.showMessage("Could not apply Classifier!");
							e.printStackTrace();
							return null;
						}
					}		
				
				if( probabilityMaps )
				{
					for(int k = 0 ; k < numClasses; k++)
						classificationResult.addSlice("class-" + (k+1), new FloatProcessor(width, height, probArray[k]) );
				}
				else
					classificationResult.addSlice("result", new FloatProcessor(width, height, probArray[0]) );
				
				return new ImagePlus("classified-slice", classificationResult);
			}
		};
	}
	
	/**
	 * Classify a list of images in a concurrent way
	 * @param list of images to classify
	 * @param dataInfo empty set of instances containing the data structure (attributes and classes)
	 * @param classifier classifier to use
	 * @param counter counter used to display the progress in the tool bar
	 * @param probabilityMaps flag to calculate probabilities or binary results
	 * @return classification result
	 */
	public Callable< ArrayList< ImagePlus >> classifyListOfImages(
			final ArrayList<ImagePlus> images,
			final Instances dataInfo,
			final AbstractClassifier classifier,
			final AtomicInteger counter,
			final boolean probabilityMaps)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable < ArrayList< ImagePlus >>(){
			public ArrayList < ImagePlus > call()
			{			
				ArrayList < ImagePlus > result = new ArrayList < ImagePlus >();
				
				for(ImagePlus image : images )
				{
					// Create feature stack for the image
					IJ.showStatus("Creating features...");
					IJ.log("Creating features of slice " + image.getTitle() + ", size = " + image.getWidth() + "x" + image.getHeight() + "...");
					final FeatureStack sliceFeatures = new FeatureStack( image );
					// Use the same features as the current classifier
					sliceFeatures.setEnabledFeatures(featureStackArray.getEnabledFeatures());
					sliceFeatures.setMaximumSigma(maximumSigma);
					sliceFeatures.setMinimumSigma(minimumSigma);
					sliceFeatures.setMembranePatchSize(membranePatchSize);
					sliceFeatures.setMembraneSize(membraneThickness);
					if(false == sliceFeatures.updateFeaturesST())
					{
						IJ.log("Classifier execution was interrupted.");
						return null;
					}
					filterFeatureStackByList(featureNames, sliceFeatures);

					final int width = image.getWidth();
					final int height = image.getHeight();				
					final int numClasses = dataInfo.numClasses();

					ImageStack classificationResult = new ImageStack(width, height);

					final int numInstances = width * height;

					final double[][] probArray;

					if (probabilityMaps)
						probArray = new double[numClasses][numInstances];
					else
						probArray = new double[1][numInstances];

					IJ.log("Classifying slice " + image.getTitle() + "...");

					for (int x=0; x<width; x++)
						for(int y=0; y<height; y++)
						{
							try{

								if (0 == (x+y*width) % 4000)
								{
									if (Thread.currentThread().isInterrupted()) 
										return null;
									counter.addAndGet(4000);
								}

								final DenseInstance ins = sliceFeatures.createInstance(x, y, 0);
								ins.setDataset(dataInfo);

								if (probabilityMaps)
								{							
									double[] prob = classifier.distributionForInstance( ins );
									for(int k = 0 ; k < numClasses; k++)
									{
										probArray[k][x+y*width] = prob[ k ];
									}
								}
								else
								{
									probArray[0][ x+y*width ] = classifier.classifyInstance( ins );
								}

							}catch(Exception e){

								IJ.showMessage("Could not apply Classifier!");
								e.printStackTrace();
								return null;
							}
						}		

					if( probabilityMaps )
					{
						for(int k = 0 ; k < numClasses; k++)
							classificationResult.addSlice("class-" + (k+1), new FloatProcessor(width, height, probArray[k]) );
					}
					else
						classificationResult.addSlice("result", new FloatProcessor(width, height, probArray[0]) );

					result.add( new ImagePlus("classified-image-"+image.getTitle(), classificationResult) );
				}
				return result;
			}
		};
	}

	/**
	 * Apply current classifier to a given image with precomputed features.
	 *
	 * @param imp image (2D single image or stack)
	 * @param filters stack of filters to apply to the original image in order to create the features
	 * @param numThreads The number of threads to use. Set to zero for auto-detection. 					
	 * @param probabilityMaps create probability maps for each class instead of a classification
	 * @return result image
	 */
	public ImagePlus applyClassifier(
			final ImagePlus imp,
			final ImagePlus filters,
			int numThreads, 
			final boolean probabilityMaps)
	{
		return applyClassifier(imp, new FeatureStackArray(imp, filters), numThreads, probabilityMaps);
	}
	
	/**
	 * Apply current classifier to a given image with precomputed features.
	 *
	 * @param imp image (2D single image or stack)
	 * @param fsa precomputed feature stack array
	 * @param numThreads The number of threads to use. Set to zero for auto-detection. 					
	 * @param probabilityMaps create probability maps for each class instead of a classification
	 * @return result image
	 */
	public ImagePlus applyClassifier(
			final ImagePlus imp,
			FeatureStackArray fsa,
			int numThreads, 
			final boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Prefs.getThreads();

		final int numSliceThreads = Math.min(imp.getStackSize(), numThreads);
		final int numClasses      = numOfClasses;
		final int numChannels     = (probabilityMaps ? numClasses : 1);

		IJ.log("Processing slices of " + imp.getTitle() + " in " + numSliceThreads + " thread(s)...");

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				for(int j=0; j<trainingImage.getImageStackSize(); j++)
					if(examples[j].get(i).size() > 0)
					{
						classNames.add(getClassLabels()[i]);
						break;
					}
		}
		else
			classNames = loadedClassNames;

		final ImagePlus[] classifiedSlices = new ImagePlus[imp.getStackSize()];

		class ApplyClassifierThread extends Thread 
		{

			final int startSlice;
			final int numSlices;
			final int numFurtherThreads;
			final ArrayList<String> classNames;
			final FeatureStackArray fsa;
			
			public ApplyClassifierThread(
					int startSlice, 
					int numSlices, 
					int numFurtherThreads, 
					ArrayList<String> classNames,
					FeatureStackArray fsa) 
			{

				this.startSlice         = startSlice;
				this.numSlices          = numSlices;
				this.numFurtherThreads  = numFurtherThreads;
				this.classNames         = classNames;
				this.fsa 				= fsa;
			}

			public void run() 
			{

				for (int i = startSlice; i < startSlice + numSlices; i++)
				{
					final ImagePlus slice = new ImagePlus(imp.getImageStack().getSliceLabel(i), imp.getImageStack().getProcessor(i));
				

					final Instances sliceData = fsa.get(i-1).createInstances(classNames);
					sliceData.setClassIndex(sliceData.numAttributes() - 1);

					IJ.log("Classifying slice " + i + " in " + numFurtherThreads + " thread(s)...");
					final ImagePlus classImage = applyClassifier(sliceData, slice.getWidth(), slice.getHeight(), numFurtherThreads, probabilityMaps);
					
					if( null == classImage )
					{
						IJ.log("Error while applying classifier!");
						return;
					}
											
					classImage.setTitle("classified_" + slice.getTitle());
					if(probabilityMaps)
						classImage.setProcessor(classImage.getProcessor().duplicate());
					else
						classImage.setProcessor(classImage.getProcessor().convertToByte(true).duplicate());
					classifiedSlices[i-1] = classImage;
				}
			}
		}

		final int numFurtherThreads = (int)Math.ceil((double)(numThreads - numSliceThreads)/numSliceThreads) + 1;
		final ApplyClassifierThread[] threads = new ApplyClassifierThread[numSliceThreads];

		// calculate optimum number of slices per thread
		int[] numSlicesPerThread = new int [ numSliceThreads ];
		for(int i=0; i<imp.getImageStackSize(); i++)
		{
			numSlicesPerThread[ i % numSliceThreads ] ++;
		}
		
		int aux = 0;
		for (int i = 0; i < numSliceThreads; i++) 
		{

			int startSlice = aux + 1;
			
			aux += numSlicesPerThread[ i ];
									
			IJ.log("Starting thread " + i + " processing " + numSlicesPerThread[ i ] + " slices, starting with " + startSlice);
			threads[i] = new ApplyClassifierThread(startSlice, numSlicesPerThread[ i ], numFurtherThreads, classNames, fsa);

			threads[i].start();
		}

		// create classified image
		final ImageStack classified = new ImageStack(imp.getWidth(), imp.getHeight());

		// join threads
		for(Thread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		// assemble classified image
		for (int i = 0; i < imp.getStackSize(); i++)
			for (int c = 0; c < numChannels; c++)
				classified.addSlice("", classifiedSlices[i].getStack().getProcessor(c+1));

		ImagePlus result = new ImagePlus("Classification result", classified);

		if (probabilityMaps)
		{
			result.setDimensions(numOfClasses, imp.getNSlices(), imp.getNFrames());
			if (imp.getNSlices()*imp.getNFrames() > 1)
				result.setOpenAsHyperStack(true);
		}

		return result;
	}
	
	/**
	 * Apply current classifier to current image.
	 * 
	 * @param classify flag to get labels or probability maps (false = labels)
	 */
	public void applyClassifier(boolean classify)
	{
		if( Thread.currentThread().isInterrupted() )
		{
			IJ.log("Classification was interrupted by the user.");
			return;
		}
		applyClassifier(0, classify);
	}

	/**
	 * Apply current classifier to current image.
	 *
	 * @param numThreads The number of threads to use. Set to zero for
	 * auto-detection.
	 * @param classify
	 */
	public void applyClassifier(int numThreads, boolean classify)
	{
		if( Thread.currentThread().isInterrupted() )
		{
			IJ.log("Training was interrupted by the user.");
			return;
		}
		
		if (numThreads == 0)
			numThreads = Prefs.getThreads();

		// Check if all feature stacks were used during training
		boolean allUsed = true;
		for(int j=0; j<featureStackToUpdateTest.length; j++)
			if(featureStackToUpdateTest[j] == true)
			{
				allUsed = false;
				break;
			}
										
		// Create feature stack if it was not created yet
		if(!allUsed || featureStackArray.isEmpty() || updateFeatures)
		{
			IJ.showStatus("Creating feature stack...");
			IJ.log("Creating feature stack...");
			long start = System.currentTimeMillis();
			if ( false == featureStackArray.updateFeaturesMT(featureStackToUpdateTest) )
			{
				IJ.log("Feature stack was not updated.");
				IJ.showStatus("Feature stack was not updated.");
				return;
			}
			Arrays.fill(featureStackToUpdateTest, false);
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			long end = System.currentTimeMillis();
			IJ.log("Feature stack array is now updated (" + featureStackArray.getSize() 
					+ " slice(s) with " + featureStackArray.getNumOfFeatures() 
					+ " features, took " + (end-start) + "ms).");
		}
		
		/*
		if(updateWholeData)
		{			
			wholeImageData = updateWholeImageData();
			if( null == wholeImageData)
				return;
		}
*/
		IJ.log("Classifying whole image using " + numThreads + " threads...");
		try{
			classifiedImage = applyClassifier(featureStackArray, numThreads, classify);
		}
		catch(Exception ex)
		{
			IJ.log("Error while classifying whole image! ");
			ex.printStackTrace();
		}
		
		IJ.log("Finished segmentation of whole image.\n");
	}

	/**
	 * Create the whole image data (instances) from the current image and feature stack.
	 * 
	 * @return feature vectors (Weka instances) of the entire image
	 */
	public Instances updateWholeImageData() 
	{
		Instances wholeImageData = null;
		
		IJ.showStatus("Reading whole image data...");
		IJ.log("Reading whole image data...");

		long start = System.currentTimeMillis();
		ArrayList<String> classNames = null;

		if(null != loadedClassNames)
			classNames = loadedClassNames;
		else
		{
			classNames = new ArrayList<String>();

			for(int j=0; j<trainingImage.getImageStackSize(); j++)
				for(int i = 0; i < numOfClasses; i++)					
					if(examples[j].get(i).size() > 0)
						if(false == classNames.contains(getClassLabels()[i]))
							classNames.add(getClassLabels()[i]);
		}
		
		final int numProcessors = Prefs.getThreads();
		final ExecutorService exe = Executors.newFixedThreadPool( numProcessors );
		
		final ArrayList< Future<Instances> > futures = new ArrayList< Future<Instances> >();
		
		try{
			

			for(int z = 1; z<=trainingImage.getImageStackSize(); z++)
			{
				IJ.log("Creating feature vectors for slice number " + z + "...");
				futures.add( exe.submit( createInstances(classNames, featureStackArray.get(z-1))) );
			}

			Instances data[] = new Instances[ futures.size() ];

			for(int z = 1; z<=trainingImage.getImageStackSize(); z++)
			{
				data[z-1] = futures.get(z-1).get();				
				data[z-1].setClassIndex(data[z-1].numAttributes() - 1);
			}						
					
			for(int n=0; n<data.length; n++)
			{
				//IJ.log("Test dataset updated ("+ data[n].numInstances() + " instances, " + data[n].numAttributes() + " attributes).");
				
				if(null == wholeImageData)
					wholeImageData = data[n];
				else
					mergeDataInPlace(wholeImageData, data[n]);				
			}
			
			IJ.log("Total dataset: "+ wholeImageData.numInstances() + 
					" instances, " + wholeImageData.numAttributes() + " attributes.");
			long end = System.currentTimeMillis();
			IJ.log("Creating whole image data took: " + (end-start) + "ms");
		
		}
		catch(InterruptedException e) 
		{
			IJ.log("The data update was interrupted by the user.");
			IJ.showStatus("The data update was interrupted by the user.");
			IJ.showProgress(1.0);
			exe.shutdownNow();
			return null;
		}
		catch(Exception ex)
		{
			IJ.log("Error when updating data for the whole image test set.");
			ex.printStackTrace();
			exe.shutdownNow();
			return null;
		}
		finally{
			exe.shutdown();
		}
		
		// Set the whole data update to false after classification
		updateWholeData = false;
		return wholeImageData;
	}
	
	/**
	 * Apply current classifier to set of instances
	 * @param data set of instances
	 * @param w image width
	 * @param h image height
	 * @param numThreads The number of threads to use. Set to zero for
	 * auto-detection.
	 * @return result image
	 */
	public ImagePlus applyClassifier(final Instances data, int w, int h, int numThreads, boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Prefs.getThreads();

		final int numClasses   = data.numClasses();
		final int numInstances = data.numInstances();
		final int numChannels  = (probabilityMaps ? numClasses : 1);
		final int numSlices    = (numChannels*numInstances)/(w*h);

		IJ.showStatus("Classifying image...");

		final long start = System.currentTimeMillis();

		ExecutorService exe = Executors.newFixedThreadPool(numThreads);
		final double[][][] results = new double[numThreads][][];
		final Instances[] partialData = new Instances[numThreads];
		final int partialSize = numInstances / numThreads;
		Future<double[][]> fu[] = new Future[numThreads];

		final AtomicInteger counter = new AtomicInteger();

		for(int i = 0; i < numThreads; i++)
		{
			if (Thread.currentThread().isInterrupted()) 
			{
				exe.shutdown();		
				return null;
			}
			if(i == numThreads - 1)
				partialData[i] = new Instances(data, i*partialSize, numInstances - i*partialSize);
			else
				partialData[i] = new Instances(data, i*partialSize, partialSize);

			AbstractClassifier classifierCopy = null;
			try {			
				// The Weka random forest classifiers do not need to be duplicated on each thread 
				// (that saves much memory)				
				if( classifier instanceof FastRandomForest || classifier instanceof RandomForest )
					classifierCopy = classifier;
				else				
					classifierCopy = (AbstractClassifier) (AbstractClassifier.makeCopy( classifier ));
				
			} catch (Exception e) {
				IJ.log("Error: classifier could not be copied to classify in a multi-thread way.");
				e.printStackTrace();
			}		
			fu[i] = exe.submit(classifyInstances(partialData[i], classifierCopy, counter, probabilityMaps));
		}

		ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
		ScheduledFuture task = monitor.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				IJ.showProgress(counter.get(), numInstances);
			}
		}, 0, 1, TimeUnit.SECONDS);

		// Join threads
		for(int i = 0; i < numThreads; i++)
		{
			try {
				results[i] = fu[i].get();
			} catch (InterruptedException e) {				
				//e.printStackTrace();
				return null;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return null;
			} finally {
				exe.shutdown();
				task.cancel(true);
				monitor.shutdownNow();
				IJ.showProgress(1);
			}
		}

		exe.shutdown();

		// Create final array
		double[][] classificationResult;
		classificationResult = new double[numChannels][numInstances];

		for(int i = 0; i < numThreads; i++)
			for (int c = 0; c < numChannels; c++)
				System.arraycopy(results[i][c], 0, classificationResult[c], i*partialSize, results[i][c].length);

		IJ.showProgress(1.0);
		final long end = System.currentTimeMillis();
		IJ.log("Classifying whole image data took: " + (end-start) + "ms");

		double[]         classifiedSlice = new double[w*h];
		final ImageStack classStack      = new ImageStack(w, h);

		for (int i = 0; i < numSlices/numChannels; i++)
		{
			for (int c = 0; c < numChannels; c++)
			{
				System.arraycopy(classificationResult[c], i*(w*h), classifiedSlice, 0, w*h);
				ImageProcessor classifiedSliceProcessor = new FloatProcessor(w, h, classifiedSlice);				
				classStack.addSlice(probabilityMaps ? getClassLabels()[c] : "", classifiedSliceProcessor);
			}
		}
		ImagePlus classImg = new ImagePlus(probabilityMaps ? "Probability maps" : "Classification result", classStack);

		return classImg;
	}

	
	/**
	 * Apply current classifier to a set of feature vectors (given in a feature stack array)
	 * 
	 * @param fsa feature stack array
	 * @param numThreads The number of threads to use. Set to zero for auto-detection.
	 * @param probabilityMaps probability flag. Tue: probability maps are calculated, false: binary classification 
	 * @return result image containing the probability maps or the binary classification
	 */
	public ImagePlus applyClassifier(
			final FeatureStackArray fsa, 
			int numThreads, 
			boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Prefs.getThreads();

		ArrayList<String> classNames = null;
		
		if(null != loadedClassNames)
			classNames = loadedClassNames;
		else
		{
			classNames = new ArrayList<String>();

			for(int j=0; j<trainingImage.getImageStackSize(); j++)
				for(int i = 0; i < numOfClasses; i++)					
					if(examples[j].get(i).size() > 0)
						if(false == classNames.contains(getClassLabels()[i]))
							classNames.add(getClassLabels()[i]);
		}

		// Create instances information (each instance needs a pointer to this)
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=fsa.getNumOfFeatures(); i++)
		{
			String attString = fsa.getLabel(i);
			attributes.add(new Attribute(attString));
		}

		if(fsa.useNeighborhood())
			for (int i=0; i<8; i++)
			{
				IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
				attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
			}
		
		attributes.add(new Attribute("class", classNames));
		Instances dataInfo = new Instances("segment", attributes, 1);
		dataInfo.setClassIndex(dataInfo.numAttributes()-1);
		
		// number of classes
		final int numClasses   = classNames.size();
		// total number of instances (i.e. feature vectors)
		final int numInstances = fsa.getSize() * trainingImage.getWidth() * trainingImage.getHeight();
		// number of channels of the result image
		final int numChannels  = (probabilityMaps ? numClasses : 1);
		// number of slices of the result image
		final int numSlices    = (numChannels*numInstances)/(trainingImage.getWidth()*trainingImage.getHeight());

		IJ.showStatus("Classifying image...");

		final long start = System.currentTimeMillis();

		exe = Executors.newFixedThreadPool(numThreads);
		final double[][][] results = new double[numThreads][][];
		final int partialSize = numInstances / numThreads;
		Future<double[][]> fu[] = new Future[numThreads];

		final AtomicInteger counter = new AtomicInteger();

		for(int i = 0; i < numThreads; i++)
		{
			if (Thread.currentThread().isInterrupted()) 
				return null;
			
			int first = i*partialSize;
			int size = (i == numThreads - 1) ? numInstances - i*partialSize : partialSize;
			

			AbstractClassifier classifierCopy = null;
			try {
				// The Weka random forest classifiers do not need to be duplicated on each thread 
				// (that saves much memory)
				if( classifier instanceof FastRandomForest || classifier instanceof RandomForest )
					classifierCopy = classifier;
				else
					classifierCopy = (AbstractClassifier) (AbstractClassifier.makeCopy( classifier ));
			} catch (Exception e) {
				IJ.log("Error: classifier could not be copied to classify in a multi-thread way.");
				e.printStackTrace();
			}
			
			fu[i] = exe.submit(classifyInstances(fsa, dataInfo, first, size, classifierCopy, counter, probabilityMaps));
		}

		ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
		ScheduledFuture task = monitor.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				IJ.showProgress(counter.get(), numInstances);
			}
		}, 0, 1, TimeUnit.SECONDS);

		// Join threads
		for(int i = 0; i < numThreads; i++)
		{
			try {
				results[i] = fu[i].get();
			} catch (InterruptedException e) {				
				//e.printStackTrace();
				return null;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return null;
			} finally {
				exe.shutdown();
				task.cancel(true);
				monitor.shutdownNow();
				IJ.showProgress(1);
			}
		}

		exe.shutdown();

		// Create final array
		double[][] classificationResult = new double[numChannels][numInstances];

		for(int i = 0; i < numThreads; i++)
			for (int c = 0; c < numChannels; c++)
				System.arraycopy(results[i][c], 0, classificationResult[c], i*partialSize, results[i][c].length);

		IJ.showProgress(1.0);
		final long end = System.currentTimeMillis();
		IJ.log("Classifying whole image data took: " + (end-start) + "ms");

		double[] classifiedSlice = new double[trainingImage.getWidth() * trainingImage.getHeight()];
		final ImageStack classStack = new ImageStack(trainingImage.getWidth(), trainingImage.getHeight());

		for (int i = 0; i < numSlices/numChannels; i++)
		{
			for (int c = 0; c < numChannels; c++)
			{
				System.arraycopy(classificationResult[c], i*(trainingImage.getWidth()*trainingImage.getHeight()), classifiedSlice, 0, trainingImage.getWidth()*trainingImage.getHeight());
				ImageProcessor classifiedSliceProcessor = new FloatProcessor(trainingImage.getWidth(), trainingImage.getHeight(), classifiedSlice);				
				classStack.addSlice(probabilityMaps ? getClassLabels()[c] : "", classifiedSliceProcessor);
			}
		}
		ImagePlus classImg = new ImagePlus(probabilityMaps ? "Probability maps" : "Classification result", classStack);

		return classImg;
	}
	
	/**
	 * Classify instances concurrently
	 * 
	 * @param fsa feature stack array with the feature vectors
	 * @param dataInfo empty set of instances containing the data structure (attributes and classes)
	 * @param first index of the first instance to classify (considering the feature stack array as a 1D array)
	 * @param numInstances number of instances to classify in this thread
	 * @param classifier current classifier
	 * @param counter auxiliary counter to be able to update the progress bar
	 * @param probabilityMaps if true return a probability map for each class instead of a classified image
	 * @return classification result
	 */
	private static Callable<double[][]> classifyInstances(
			final FeatureStackArray fsa,
			final Instances dataInfo,
			final int first,
			final int numInstances,
			final AbstractClassifier classifier,
			final AtomicInteger counter,
			final boolean probabilityMaps)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<double[][]>(){

			public double[][] call(){

				final double[][] classificationResult;
				
				final int width = fsa.getWidth();
				final int height = fsa.getHeight();
				final int sliceSize = width * height;
				final int numClasses = dataInfo.numClasses();

				if (probabilityMaps)
					classificationResult = new double[numClasses][numInstances];
				else
					classificationResult = new double[1][numInstances];
			
				for (int i=0; i<numInstances; i++)
				{
					try{

						if (0 == i % 4000)
						{
							if (Thread.currentThread().isInterrupted()) 
								return null;
							counter.addAndGet(4000);
						}

						final int absolutePos = first + i;
						final int slice = absolutePos / sliceSize;
						final int localPos = absolutePos - slice * sliceSize;
						final int x = localPos % width;
						final int y = localPos / width;
						DenseInstance ins = fsa.get( slice ).createInstance(x, y, 0);
						ins.setDataset(dataInfo);
						
						if (probabilityMaps)
						{							
							double[] prob = classifier.distributionForInstance( ins );
							for(int k = 0 ; k < numClasses; k++)
								classificationResult[k][i] = prob[k];
						}
						else
						{
							classificationResult[0][i] = classifier.classifyInstance( ins );
						}

					}catch(Exception e){

						IJ.showMessage("Could not apply Classifier!");
						e.printStackTrace();
						return null;
					}
				}
				return classificationResult;
			}
		};
	}
	
	
	/**
	 * Classify instances concurrently
	 * 
	 * @param data set of instances to classify
	 * @param classifier current classifier
	 * @param counter auxiliary counter to be able to update the progress bar
	 * @param probabilityMaps return a probability map for each class instead of a
	 * classified image
	 * @return classification result
	 */
	private static Callable<double[][]> classifyInstances(
			final Instances data,
			final AbstractClassifier classifier,
			final AtomicInteger counter,
			final boolean probabilityMaps)
	{
		if (Thread.currentThread().isInterrupted()) 
			return null;
		
		return new Callable<double[][]>(){

			public double[][] call(){

				final int numInstances = data.numInstances();
				final int numClasses   = data.numClasses();

				final double[][] classificationResult;

				if (probabilityMaps)
					classificationResult = new double[numClasses][numInstances];
				else
					classificationResult = new double[1][numInstances];

				for (int i=0; i<numInstances; i++)
				{
					try{

						if (0 == i % 4000)
						{
							if (Thread.currentThread().isInterrupted()) 
								return null;
							counter.addAndGet(4000);
						}

						if (probabilityMaps)
						{
							double[] prob = classifier.distributionForInstance(data.get(i));
							for(int k = 0 ; k < numClasses; k++)
								classificationResult[k][i] = prob[k];
						}
						else
						{
							classificationResult[0][i] = classifier.classifyInstance(data.get(i));
						}

					}catch(Exception e){

						IJ.showMessage("Could not apply Classifier!");
						e.printStackTrace();
						return null;
					}
				}
				return classificationResult;
			}
		};
	}

	/**
	 * Set features to use during training
	 *
	 * @param featureNames list of feature names to use
	 * @return false if error
	 */
	public boolean setFeatures(ArrayList<String> featureNames)
	{
		if (null == featureNames)
			return false;

		this.featureNames = featureNames;

		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		for(final String name : featureNames)
		{
			for(int i = 0 ; i < numFeatures; i++)
				if(name.startsWith(FeatureStack.availableFeatures[i]))
					usedFeatures[i] = true;
		}

		this.featureStackArray.setEnabledFeatures(usedFeatures);

		return true;
	}

	/**
	 * Set expected membrane thickness (this will be the width of 
	 * the center column of the membrane filter)
	 * @param thickness expected membrane thickness (in pixels)
	 */
	public void setMembraneThickness(int thickness)
	{
		this.membraneThickness = thickness;
		featureStackArray.setMembraneSize(thickness);
	}

	/**
	 * Get current expected membrane thickness
	 * @return expected membrane thickness
	 */
	public int getMembraneThickness()
	{
		return membraneThickness;
	}

	/**
	 * Set the membrane patch size (it must be an odd number)
	 * @param patchSize membrane patch size
	 */
	public void setMembranePatchSize(int patchSize)
	{
		membranePatchSize = patchSize;
		featureStackArray.setMembranePatchSize(patchSize); 
	}
	public int getMembranePatchSize()
	{
		return membranePatchSize;
	}

	/**
	 * Set the maximum sigma/radius to use in the features
	 * @param sigma maximum sigma to use in the features filters
	 */
	public void setMaximumSigma(float sigma)
	{
		maximumSigma = sigma;
		featureStackArray.setMaximumSigma(sigma);
	}

	/**
	 * Get the maximum sigma/radius to use in the features
	 * @return maximum sigma/radius to use in the features
	 */
	public float getMaximumSigma()
	{
		return maximumSigma;
	}

	/**
	 * Set the minimum sigma (radius) to use in the features
	 * @param sigma minimum sigma (radius) to use in the features filters
	 */
	public void setMinimumSigma(float sigma)
	{
		minimumSigma = sigma;
		featureStackArray.setMinimumSigma(sigma);
	}
	
	/**
	 * Get the minimum sigma (radius) to use in the features
	 * @return minimum sigma (radius) to use in the features
	 */
	public float getMinimumSigma()
	{
		return minimumSigma;
	}

	/**
	 * Get current number of trees (for random forest training)
	 * @return number of trees
	 */
	public int getNumOfTrees()
	{
		return numOfTrees;
	}
	/**
	 * Get number of random features (random forest training)
	 * @return number of random feature per node of the random forest
	 */
	public int getNumRandomFeatures()
	{
		return randomFeatures;
	}
	/**
	 * Get maximum depth of the random forest
	 * @return maximum depth of the random forest
	 */
	public int getMaxDepth()
	{
		return maxDepth;
	}

	/**
	 * Set the flag to balance the class distributions
	 * @param homogenizeClasses boolean flag to enable/disable the class balance
	 */
	public void setDoHomogenizeClasses(boolean homogenizeClasses)
	{
		this.homogenizeClasses = homogenizeClasses;
	}

	/**
	 * Get the boolean flag to enable/disable the class balance
	 * @return flag to enable/disable the class balance
	 */
	public boolean doHomogenizeClasses()
	{
		return homogenizeClasses;
	}

	/**
	 * Set feature update flag
	 * @param udpateFeatures new feature update flag
	 */
	public void setUpdateFeatures(boolean updateFeatures)
	{
		this.updateFeatures = updateFeatures;
	}
	
	/**
	 * Forces the feature stack to be updated whenever it is needed next.
	 */
	public void setFeaturesDirty()
	{
		updateFeatures = true;
		// Set feature stacks belonging to slices with traces 
		// to be updated during training and not test 
		Arrays.fill(featureStackToUpdateTrain, false);
		Arrays.fill(featureStackToUpdateTest, true);
		
		for(int indexSlice=0; indexSlice<trainingImage.getImageStackSize(); indexSlice++)
		{
			for(int indexClass = 0; indexClass < numOfClasses; indexClass++)
				if(examples[indexSlice].get(indexClass).size() > 0)
				{
					//IJ.log("feature stack for slice " + (indexSlice+1) + " needs to be updated for training");
					featureStackToUpdateTrain[indexSlice] = true;
					featureStackToUpdateTest[indexSlice] = false;
					break;
				}
		}
		
		// Reset the reference index in the feature stack array
		featureStackArray.resetReference();
	}

	/**
	 * Update fast random forest classifier with new values
	 *
	 * @param newNumTrees new number of trees
	 * @param newRandomFeatures new number of random features per tree
	 * @param newMaxDepth new maximum depth per tree
	 * @return false if error
	 */
	public boolean updateClassifier(
			int newNumTrees,
			int newRandomFeatures,
			int newMaxDepth)
	{
		if(newNumTrees < 1 || newRandomFeatures < 0)
			return false;
		numOfTrees = newNumTrees;
		randomFeatures = newRandomFeatures;
		maxDepth = newMaxDepth;

		rf.setNumTrees(numOfTrees);
		rf.setNumFeatures(randomFeatures);
		rf.setMaxDepth(maxDepth);

		return true;
	}

	/**
	 * Set the new enabled features
	 * @param newFeatures new enabled feature flags
	 */
	public void setEnabledFeatures(boolean[] newFeatures) 
	{
		this.enabledFeatures = newFeatures;
		featureStackArray.setEnabledFeatures(newFeatures);
	}
	
	/**
	 * Get the current enabled features
	 * @return current enabled feature flags
	 */
	public boolean[] getEnabledFeatures() 
	{
		return this.enabledFeatures;
	}
	
	/**
	 * Merge two datasets of Weka instances in place
	 * @param first first (and destination) dataset
	 * @param second second dataset
	 */
	public void mergeDataInPlace(Instances first, Instances second)
	{
		for(int i=0; i<second.numInstances(); i++)
			first.add(second.get(i));
	}

	/**
	 * Shut down the executor service for training and feature creation
	 */
	public void shutDownNow()
	{
		featureStackArray.shutDownNow();
		exe.shutdownNow();	
	}

	/**
	 * Assign an arbitrary filter stack array
	 * @param fsa new filter stack array
	 */
	public void setFeatureStackArray(FeatureStackArray fsa)
	{
		this.featureStackArray = fsa;
		// set feature stacks to be updated during train and test 
		featureStackToUpdateTrain = new boolean[featureStackArray.getSize()];
		featureStackToUpdateTest = new boolean[featureStackArray.getSize()];
		Arrays.fill(featureStackToUpdateTest, true);
	}

	/**
	 * Set the list of loaded class names
	 * @param classNames new list of class names
	 */
	public void setLoadedClassNames(ArrayList<String> classNames)
	{
		this.loadedClassNames = classNames;
	}

	/**
	 * Save specific slice feature stack
	 * 
	 * @param slice slice number
	 * @param dir directory to save the stack(s)
	 * @param fileWithExt file name with extension for the file(s)
	 */
	public void saveFeatureStack(int slice, String dir, String fileWithExt)
	{
		
		if(featureStackArray.isEmpty())
		{
			featureStackArray.updateFeaturesMT();
		}

		if(null == dir || null == fileWithExt)
			return;


		final String fileName = dir + fileWithExt.substring(0, fileWithExt.length()-4) 
									+ String.format("%04d", slice) + ".tif";
		if(false == featureStackArray.get(slice-1).saveStackAsTiff(fileName))
		{
			IJ.error("Error", "Feature stack could not be saved");
			return;
		}

		IJ.log("Saved feature stack for slice " + (slice) + " as " + fileName);
			
	}

	/**
	 * Set the labels for each class
	 * @param classLabels array containing all the class labels
	 */
	public void setClassLabels(String[] classLabels) 
	{
		this.classLabels = classLabels;
	}

	/**
	 * Get the current class labels
	 * @return array containing all the class labels
	 */
	public String[] getClassLabels() 
	{
		return classLabels;
	}
	
}
