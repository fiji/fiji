package trainableSegmentation;

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
import java.util.Collections;
import java.util.Comparator;
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

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import util.FindConnectedRegions;

import util.FindConnectedRegions.Results;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;

import weka.classifiers.pmml.consumer.PMMLClassifier;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;

import weka.filters.Filter;

import weka.filters.supervised.attribute.AttributeSelection;

import weka.filters.supervised.instance.Resample;

import weka.gui.explorer.ClassifierPanel;

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
	private boolean updateFeatures = true;
	
	/** array of boolean flags to update (or not) specific feature stacks during training */
	private boolean featureStackToUpdateTrain[];
	
	/** array of boolean flags to update (or not) specific feature stacks during test */
	private boolean featureStackToUpdateTest[];

	/** current number of classes */
	private int numOfClasses = 0;
	/** names of the current classes */
	String[] classLabels = new String[]{"class 1", "class 2", "class 3", "class 4", "class 5"};

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
	private boolean[] enabledFeatures = new boolean[]{true, true, true, true, true, false, false, 
													 false, false, false, false, false, false, false, false /*, false, false */};
	/** use neighborhood flag */
	private boolean useNeighbors = false;

	/** list of the names of features to use */
	private ArrayList<String> featureNames = null;

	/** flag to set the resampling of the training data in order to guarantee the same number of instances per class */
	private boolean homogenizeClasses = false;

	/** temporary folder name. It is used to stored intermediate results if different from null */
	private String tempFolder = null;

	public static final double SIMPLE_POINT_THRESHOLD = 0;
	
	/** executor service to launch threads for the library operations */
	private ExecutorService exe = Executors.newFixedThreadPool(1);
	

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
		classLabels[classNum] = label;
		updateWholeData = true;
	}

	/**
	 * Get the label name of a class.
	 * 
	 * @param classNum class index
	 */
	public String getClassLabel(int classNum) 
	{
		return classLabels[classNum];
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
			if( !className.equals(this.classLabels[j]))
			{
				String s = classLabels[0];
				for(int i = 1; i < numOfClasses; i++)
					s = s.concat(", " + classLabels[i]);
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

		IJ.log("Loaded data: " + loadedTrainingData.numInstances() + " instances");

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
			this.featureStackArray.setEnabledFeatures(usedFeatures);
			// Force features to be updated
			updateFeatures = true;
		}

		if (false == adjustSegmentationStateToData(loadedTrainingData) )
			loadedTrainingData = null;
		else
			IJ.log("Loaded data: " + loadedTrainingData.numInstances() + " instances");

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
		for(classIndex = 0 ; classIndex < this.classLabels.length; classIndex++)
			if(className.equalsIgnoreCase(this.classLabels[classIndex]))
				break;
		if(classIndex == this.classLabels.length)
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
				loadedClassNames.add(classLabels[i]);
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
		for(classIndex1 = 0 ; classIndex1 < this.classLabels.length; classIndex1++)
			if(className1.equalsIgnoreCase(this.classLabels[classIndex1]))
				break;
		if(classIndex1 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return false;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.classLabels.length; classIndex2++)
			if(className2.equalsIgnoreCase(this.classLabels[classIndex2]))
				break;
		if(classIndex2 == this.classLabels.length)
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
				loadedClassNames.add(classLabels[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);

			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}

		// Check all pixels different from black
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
/*
		if (featureStack == null)
			featureStack = new FeatureStack(newImage);

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

		// Remove traces from the lists and ROI overlays
		IJ.log("Removing previous markings...");
		examples.clear();
		for(int i = 0; i < numOfClasses; i ++)
		{
			examples.add(new ArrayList<SliceRoi>());
		}

		// Updating image
		IJ.log("Updating image...");

		if (trainingImage == null)
			trainingImage = new ImagePlus("Advanced Weka Segmentation", newImage.getProcessor().duplicate().convertToByte(true));
		else
			trainingImage.setProcessor("Advanced Weka Segmentation", newImage.getProcessor().duplicate().convertToByte(true));

		// Initialize feature stack (no features yet)
		final boolean[] enabledFeatures = featureStack.getEnableFeatures();
		featureStack = new FeatureStack(trainingImage);
		featureStack.setEnableFeatures(enabledFeatures);
		featureStack.setMaximumSigma(this.maximumSigma);
		featureStack.setMinimumSigma(this.minimumSigma);
		featureStack.setMembranePatchSize(this.membranePatchSize);
		featureStack.setMembraneSize(this.membraneThickness);
		updateFeatures = true;
		updateWholeData = true;

		// Remove current classification result image
		classifiedImage = null;

		IJ.log("Done");
*/
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
		final byte[] pix = (byte[])labelIP.getProcessor().getPixels();
		for(int i =0; i < pix.length; i++)
			if(pix[i] > 0)
				pix[i] = (byte)255;


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
			final byte[] pix = (byte[])labelIP.getProcessor().getPixels();
			for(int j =0; j < pix.length; j++)
				if(pix[j] > 0)
					pix[j] = (byte)255;

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
			final byte[] pix = (byte[])labelIP.getProcessor().getPixels();
			for(int j =0; j < pix.length; j++)
				if(pix[j] > 0)
					pix[j] = (byte)255;

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
	 * Set the temporary folder
	 * @param tempFolder complete path name for temporary folder
	 */
	public void setTempFolder(final String tempFolder)
	{
		this.tempFolder = tempFolder;
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
					classNames.add(classLabels[i]);
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
			final ImagePlus testSlice = new ImagePlus(image.getImageStack().getSliceLabel(z), image.getImageStack().getProcessor(z).convertToByte(true));
			// Create feature stack for test image
			IJ.showStatus("Creating features for test image...");
			if(verbose)
				IJ.log("Creating features for test image " + z +  "...");
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
					classNames.add(classLabels[i]);
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
			final ImagePlus testSlice = new ImagePlus(image.getImageStack().getSliceLabel(z), image.getImageStack().getProcessor(z).convertToByte(true));
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
	

	// **********************
	// BLOTC-related  methods
	// **********************

	/**
	 * Train a FastRandomForest classifier using BLOTC:
	 * Boundary Learning by Optimization with Topological Constraints
	 * Jain, Bollmann, Richardson, Berger, Helmstaedter, Briggman, Denk, Bowden,
	 * Mendenhall, Abraham, Harris, Kasthuri, Hayworth, Schalek, Tapia, Lichtman, and Seung.
	 * IEEE Conference on Computer Vision and Pattern Recognition [CVPR 2010]
	 *
	 *  @param image input image
	 *  @param labels corresponding binary labels
	 *  @param numOfTrees number of trees to use in the random forest
	 *  @param randomFeatures number of random features in the random forest
	 *  @param maxDepth maximum depth allowed in the trees
	 *  @param seed fast random forest seed
	 *  @param resample flag to resample input data (to homogenize classes distribution)
	 *  @param selectAttributes flag to select best attributes and reduce the data size
	 *  @return trained fast random forest classifier
	 */
	public static FastRandomForest trainRandomForestBLOTC(
			final ImagePlus image,
			final ImagePlus labels,
			final int numOfTrees,
			final int randomFeatures,
			final int maxDepth,
			final int seed,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Initialization of Fast Random Forest classifier
		final FastRandomForest rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);
		rf.setNumFeatures(randomFeatures);
		rf.setMaxDepth(maxDepth);
		rf.setSeed(seed);

		ImagePlus result = trainBLOTC(image, labels, rf, resample, selectAttributes);
		result.show();

		return rf;
	}

	/**
	 * Train current classifier using BLOTC (non-static method)
	 *
	 * @param image input image
	 * @param labels binary labels
	 * @param mask binary mask to use in the warping
	 * @param resample flag to resample input data (to homogenize classes distribution)
	 * @param selectAttributes flag to select best attributes and filter the data
	 * @return warped labels from applying BLOTC
	 */
	public ImagePlus trainBLOTC(
			final ImagePlus image,
			final ImagePlus labels,
			final ImagePlus mask,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Create a float copy of the labels
		final ImageStack warpedLabelStack = new ImageStack(image.getWidth(), image.getHeight());
		for(int i=1; i<=labels.getStackSize(); i++)
			warpedLabelStack.addSlice("warped label " + i, labels.getStack().getProcessor(i).duplicate().convertToFloat());
		ImagePlus warpedLabels = new ImagePlus("warped labels", warpedLabelStack);

		// At the moment, use all features
		String firstClass = classLabels[0];
		String secondClass = classLabels[1];

		double error = Double.MAX_VALUE;

		final int numOfPixelsPerImage = image.getWidth() * image.getHeight();

		IJ.log("Adding labels to training data set...");

		// Add all labels as binary data (each input slice)
		addBinaryData(image, labels, secondClass, firstClass);

		Instances originalData = this.loadedTrainingData;

		// Reduce data size by selecting attributes
		if(selectAttributes)
		{
			// Reduce size of data by attribute selection
			IJ.log("Selecting best attributes...");
			final long start = System.currentTimeMillis();
			selectAttributes();
			final long end = System.currentTimeMillis();
			originalData = this.loadedTrainingData;
			IJ.log("Filtered data: " + originalData.numInstances()
					+ " instances, " + originalData.numAttributes()
					+ " attributes, " + originalData.numClasses() + " classes.");
			IJ.log("Filtering training data took: " + (end-start) + "ms");
		}

		Instances trainingData = originalData;

		// homogenize classes if resample is true
		if(resample)
		{
			// Resample data
			IJ.log("Resampling input data (to homogenize the class distributions)...");
			trainingData = homogenizeTrainingData(trainingData);
			setLoadedTrainingData(trainingData);
		}

		// train BLOTC
		int iter = 1;
		while(true)
		{
			IJ.log("BLOTC training...");

			// Train classifier with current ground truth
			trainClassifier();

			double newError = getTrainingError(true);

			IJ.log("BLOTC iteration " + iter + ": training error = " + newError);

			if(newError >= error)
				break;

			error = newError;

			final ImageStack proposalStack = new ImageStack(image.getWidth(), image.getHeight());

			for(int i=1; i<=image.getStackSize(); i++)
			{
				final Instances subDataSet = new Instances (originalData, (i-1)*numOfPixelsPerImage, numOfPixelsPerImage);
				IJ.log("Calculating class probability for whole image " + i + "...");
				ImagePlus result = applyClassifier(subDataSet, image.getWidth(), image.getHeight(), 0, true);
				proposalStack.addSlice("probability map " + i, result.getImageStack().getProcessor(2));
			}

			final ImagePlus proposal = new ImagePlus("proposal", proposalStack);

			//warpedLabels.show();
			//proposal.show();

			IJ.log("Warping ground truth...");

			final ArrayList<Point3f>[] mismatches = new ArrayList[image.getStackSize()];

			// Warp ground truth, relax original labels to proposal. Only simple
			// points warping is allowed.
			warpedLabels = simplePointWarp2dMT(warpedLabels, proposal, mask, 0.5, mismatches);

			// Update training data with warped labels
			if(!resample)
				udpateDataClassification(warpedLabels, secondClass, firstClass);
			else
			{
				IJ.log("Resampling training data...");
				updateDataClassification(originalData, warpedLabels, 1, 0, mismatches);
				trainingData = homogenizeTrainingData(originalData);
				setLoadedTrainingData(trainingData);
			}

			if(null != this.tempFolder)
			{
				final File temp = new File(tempFolder);
				if(null != temp && temp.exists())
				{
					saveClassifier(tempFolder + "/classifier-" + iter + ".model");
					IJ.saveAs(warpedLabels, "Tiff", tempFolder + "/warped-labels-" + iter + ".tif");
				}
			}

			iter++;
		}
		return warpedLabels;
	}



	/**
	 * Train a classifier using BLOTC (static method)
	 *
	 * @param image input image
	 * @param labels binary labels
	 * @param classifier Weka classifier
	 * @param resample flag to resample input data (to homogenize classes distribution)
	 * @param selectAttributes flag to select best attributes and filter the data
	 * @return warped labels from applying BLOTC
	 */
	public static ImagePlus trainBLOTC(
			final ImagePlus image,
			final ImagePlus labels,
			final AbstractClassifier classifier,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Create a float copy of the labels
		final ImageStack warpedLabelStack = new ImageStack(image.getWidth(), image.getHeight());
		for(int i=1; i<=labels.getStackSize(); i++)
			warpedLabelStack.addSlice("warped label " + i, labels.getStack().getProcessor(i).duplicate().convertToFloat());
		ImagePlus warpedLabels = new ImagePlus("warped labels", warpedLabelStack);

		// Create segmentation project
		final WekaSegmentation seg = new WekaSegmentation(image);

		if( null != classifier )
			seg.setClassifier(classifier);

		// At the moment, use all features
		seg.useAllFeatures();
		String firstClass = seg.classLabels[0];
		String secondClass = seg.classLabels[1];

		double error = Double.MAX_VALUE;

		final int numOfPixelsPerImage = image.getWidth() * image.getHeight();

		IJ.log("Adding labels to training data set...");

		// Add all labels as binary data (each input slice)
		// class 2 = white, class 1 = black
		seg.addBinaryData(image, labels, secondClass, firstClass);

		Instances originalData = seg.getTrainingInstances();

		// Reduce data size by selecting attributes
		if(selectAttributes)
		{
			// Reduce size of data by attribute selection
			IJ.log("Selecting best attributes...");
			final long start = System.currentTimeMillis();
			originalData = selectAttributes(originalData);
			final long end = System.currentTimeMillis();
			seg.setLoadedTrainingData(originalData);
			IJ.log("Filtered data: " + originalData.numInstances()
					+ " instances, " + originalData.numAttributes()
					+ " attributes, " + originalData.numClasses() + " classes.");
			IJ.log("Filtering training data took: " + (end-start) + "ms");
		}

		Instances trainingData = originalData;

		// homogenize classes if resample is true
		if(resample)
		{
			// Resample data
			IJ.log("Resampling input data (to homogenize the class distributions)...");
			trainingData = homogenizeTrainingData(trainingData);

			seg.setLoadedTrainingData(trainingData);
		}

		// train using BLOTC
		int iter = 1;
		while(true)
		{
			IJ.log("BLOTC training...");

			// Train classifier with current ground truth
			seg.trainClassifier();

			double newError = seg.getTrainingError(true);

			IJ.log("BLOTC iteration " + iter + ": training error = " + newError);

			if(newError >= error)
				break;

			error = newError;

			final ImageStack proposalStack = new ImageStack(image.getWidth(), image.getHeight());

			for(int i=1; i<=image.getStackSize(); i++)
			{
				final Instances subDataSet = new Instances (originalData, (i-1)*numOfPixelsPerImage, numOfPixelsPerImage);
				IJ.log("Calculating class probability for whole image " + i + "...");
				ImagePlus result = seg.applyClassifier(subDataSet, image.getWidth(), image.getHeight(), 0, true);
				proposalStack.addSlice("probability map " + i, result.getImageStack().getProcessor(2));
			}

			final ImagePlus proposal = new ImagePlus("proposal", proposalStack);

			//warpedLabels.show();
			//proposal.show();
			IJ.log("Warping ground truth...");

			final ArrayList<Point3f>[] mismatches = new ArrayList[image.getStackSize()];

			// Warp ground truth, relax original labels to proposal. Only simple
			// points warping is allowed.
			warpedLabels = seg.simplePointWarp2dMT(warpedLabels, proposal, null, 0.5, mismatches);

			// Update training data with warped labels
			if(!resample)
				seg.udpateDataClassification(warpedLabels, secondClass, firstClass);
			else
			{
				IJ.log("Resampling training data...");
				updateDataClassification(originalData, warpedLabels, 1, 0);
				trainingData = homogenizeTrainingData(originalData);
				seg.setLoadedTrainingData(trainingData);
			}

			iter++;
		}
		return warpedLabels;
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
		for(classIndex1 = 0 ; classIndex1 < this.classLabels.length; classIndex1++)
			if(className1.equalsIgnoreCase(this.classLabels[classIndex1]))
				break;
		if(classIndex1 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.classLabels.length; classIndex2++)
			if(className2.equalsIgnoreCase(this.classLabels[classIndex2]))
				break;
		if(classIndex2 == this.classLabels.length)
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
	 * Calculate warping error
	 *
	 * @param label original labels (single image or stack)
	 * @param proposal proposed new labels
	 * @param mask image mask
	 * @param binaryThreshold binary threshold to binarize proposal
	 * @return total warping error
	 */
	public static double warpingError(
			ImagePlus label,
			ImagePlus proposal,
			ImagePlus mask,
			double binaryThreshold)
	{
		final ImagePlus warpedLabels = simplePointWarp2d(label, proposal, mask, binaryThreshold);

		if(null == warpedLabels)
			return -1;

		double error = 0;
		double count = 0;


		for(int j=1; j<=proposal.getImageStackSize(); j++)
		{
			final float[] proposalPixels = (float[])proposal.getImageStack().getProcessor(j).getPixels();
			final float[] warpedPixels = (float[])warpedLabels.getImageStack().getProcessor(j).getPixels();
			for(int i=0; i<proposalPixels.length; i++)
			{
				count ++;
				final float thresholdedProposal = (proposalPixels[i] > binaryThreshold) ? 1.0f : 0.0f;
				if (warpedPixels[i] != thresholdedProposal)
					error++;
			}

		}

		if(count != 0)
			return error / count;
		else
			return -1;
	}

	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 *
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image
	 */
	public static ImagePlus simplePointWarp2d(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}

		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());

		double warpingError = 0;
		for(int i = 1; i <= sourceSlices.getSize(); i++)
		{
			WarpingResults wr = simplePointWarp2d(sourceSlices.getProcessor(i),
					targetSlices.getProcessor(i), null != mask ? maskSlices.getProcessor(i) : null,
					binaryThreshold);
			if(null != wr.warpedSource)
				warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());
			if(wr.warpingError != -1)
				warpingError += wr.warpingError;
		}

		IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));

		return new ImagePlus("warped source", warpedSource);
	}

	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 * (multi-thread version)
	 *
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image
	 */
	public ImagePlus simplePointWarp2dMT(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold,
			ArrayList<Point3f>[] mismatches)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}


		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;

		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());

		if(null == mismatches)
			mismatches = new ArrayList[sourceSlices.getSize()];

		// Executor service to produce concurrent threads
		exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<WarpingResults> > futures = new ArrayList< Future<WarpingResults> >();

		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( simplePointWarp2DConcurrent(sourceSlices.getProcessor(i),
										targetSlices.getProcessor(i),
										null != maskSlices ? maskSlices.getProcessor(i) : null,
										binaryThreshold ) ) );
			}

			double warpingError = 0;
			int i = 0;
			// Wait for the jobs to be done
			for(Future<WarpingResults> f : futures)
			{
				final WarpingResults wr = f.get();
				if(null != wr.warpedSource)
					warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());
				if(wr.warpingError != -1)
					warpingError += wr.warpingError;
				if(null != wr.mismatches)
					mismatches[i] = wr.mismatches;
				i++;
			}
			IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));
		}
		catch(Exception ex)
		{
			IJ.log("Error when warping ground truth in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}

		return new ImagePlus("warped source", warpedSource);
	}


	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @return warping results (warped labels, warping error value and mismatching points)
	 */
	public Callable<WarpingResults> simplePointWarp2DConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold)
	{
		return new Callable<WarpingResults>(){
			public WarpingResults call(){

				return simplePointWarp2d(source, target, mask, binaryThreshold);
			}
		};
	}


	/**
	 * Results from simple point warping (2D)
	 *
	 */
	public static class WarpingResults{
		/** warped source image after 2D simple point relaxation */
		public ImagePlus warpedSource;
		/** warping error */
		public double warpingError;

		public ArrayList<Point3f> mismatches;
	}

	/**
	 * Use simple point relaxation to warp 2D source into 2D target.
	 * Source is only modified at nonzero locations in the mask
	 *
	 * @param source input 2D image to be relaxed
	 * @param target target 2D image
	 * @param mask 2D image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image and warping error
	 */
	public static WarpingResults simplePointWarp2d(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			double binaryThreshold)
	{
		if(binaryThreshold < 0 || binaryThreshold > 1)
			binaryThreshold = 0.5;

		// Grayscale target
		final ImagePlus targetReal;// = new ImagePlus("target_real", target.duplicate());
		// Binarized target
		final ImagePlus targetBin; // = new ImagePlus("target_aux", target.duplicate());

		final ImagePlus sourceReal; // = new ImagePlus("source_real", source.duplicate());

		final ImagePlus maskReal; // = (null != mask) ? new ImagePlus("mask_real", mask.duplicate().convertToFloat()) : null;

		final int width = target.getWidth();
		final int height = target.getHeight();

		// Resize canvas to avoid checking the borders
		//IJ.run(targetReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
		ImageProcessor ip = target.createProcessor(width+2, height+2);
		ip.insert(target, 1, 1);
		targetReal = new ImagePlus("target_real", ip.duplicate());

		// IJ.run(targetBin, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
		targetBin = new ImagePlus("target_aux", ip.duplicate());

		// IJ.run(sourceReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
		ip = target.createProcessor(width+2, height+2);
		ip.insert(source, 1, 1);
		sourceReal = new ImagePlus("source_real", ip.duplicate());

		if(null != mask)
		{
			//IJ.run(maskReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
			ip = target.createProcessor(width+2, height+2);
			ip.insert(mask, 1, 1);
			maskReal = new ImagePlus("mask_real", ip.duplicate());
		}
		else{
			maskReal = null;
		}

		// make sure source and target are binary images
		final float[] sourceRealPix = (float[])sourceReal.getProcessor().getPixels();
		for(int i=0; i < sourceRealPix.length; i++)
			if(sourceRealPix[i] > 0)
				sourceRealPix[i] = 1.0f;

		final float[] targetBinPix = (float[])targetBin.getProcessor().getPixels();
		for(int i=0; i < targetBinPix.length; i++)
			targetBinPix[i] = (targetBinPix[i] > binaryThreshold) ? 1.0f : 0.0f;

		double diff = Double.MIN_VALUE;
		double diff_before = 0;

		final WarpingResults result = new WarpingResults();

		while(true)
		{
			ImageProcessor missclass_points_image = sourceReal.getProcessor().duplicate();
			missclass_points_image.copyBits(targetBin.getProcessor(), 0, 0, Blitter.DIFFERENCE);

			diff_before = diff;

			// Count mismatches
			float pixels[] = (float[]) missclass_points_image.getPixels();
			float mask_pixels[] = (null != maskReal) ? (float[]) maskReal.getProcessor().getPixels() : new float[pixels.length];
			if(null == maskReal)
				Arrays.fill(mask_pixels, 1f);

			diff = 0;
			for(int k = 0; k < pixels.length; k++)
				if(pixels[k] != 0 && mask_pixels[k] != 0)
					diff ++;

			//IJ.log("Difference = " + diff);

			if(diff == diff_before || diff == 0)
				break;

			final ArrayList<Point3f> mismatches = new ArrayList<Point3f>();

			final float[] realTargetPix = (float[])targetReal.getProcessor().getPixels();

			// Sort mismatches by the absolute value of the target pixel value - threshold
			for(int x = 1; x < width+1; x++)
				for(int y = 1; y < height+1; y++)
				{
					if(pixels[x+y*(width+2)] != 0 && mask_pixels[x+y*(width+2)] != 0)
						mismatches.add(new Point3f(x , y , (float) Math.abs( realTargetPix[x+y*(width+2)] - binaryThreshold) ));
				}

			// Sort mismatches in descending order
			Collections.sort(mismatches,  new Comparator<Point3f>() {
			    public int compare(Point3f o1, Point3f o2) {
			        return (int)((o2.z - o1.z) *10000);
			    }});

			// Process mismatches
			for(final Point3f p : mismatches)
			{
				final int x = (int) p.x;
				final int y = (int) p.y;

				if(p.z < SIMPLE_POINT_THRESHOLD)
					continue;

				double[] val = new double[]{
						sourceRealPix[ (x-1) + (y-1) * (width+2) ],
						sourceRealPix[ (x  ) + (y-1) * (width+2) ],
						sourceRealPix[ (x+1) + (y-1) * (width+2) ],
						sourceRealPix[ (x-1) + (y  ) * (width+2) ],
						sourceRealPix[ (x  ) + (y  ) * (width+2) ],
						sourceRealPix[ (x+1) + (y  ) * (width+2) ],
						sourceRealPix[ (x-1) + (y+1) * (width+2) ],
						sourceRealPix[ (x  ) + (y+1) * (width+2) ],
						sourceRealPix[ (x+1) + (y+1) * (width+2) ]
				};

				final double pix = val[4];

				final ImagePlus patch = new ImagePlus("patch", new FloatProcessor(3,3,val));
				if( simple2D(patch, 4) )
				{/*
							for(int i=0; i<9;i++)
								IJ.log(" " + val[i]);
							IJ.log("pix = " + pix);*/
					sourceRealPix[ x + y * (width+2)] =  pix > 0.0 ? 0.0f : 1.0f ;
					//IJ.log("flipping pixel x: " + x + " y: " + y + " to " + (pix > 0  ? 0.0 : 1.0));

				}

			}

			result.mismatches = mismatches;


		}

		//IJ.run(sourceReal, "Canvas Size...", "width="+ width + " height=" + height + " position=Center zero");
		ip = source.createProcessor(width, height);
		ip.insert(sourceReal.getProcessor(), -1, -1);
		sourceReal.setProcessor(ip.duplicate());


		result.warpedSource = sourceReal;
		result.warpingError = diff / (width * height);
		return result;
	}


	/**
	 * Check if a point is simple (in 2D)
	 * @param im input patch
	 * @param n neighbors
	 * @return true if the center pixel of the patch is a simple point
	 */
	public static boolean simple2D(ImagePlus im, int n)
	{
		final ImagePlus invertedIm = new ImagePlus("inverted", im.getProcessor().duplicate());
		//IJ.run(invertedIm, "Invert","");
		final float[] pix = (float[])invertedIm.getProcessor().getPixels();
		for(int i=0; i<pix.length; i++)
			pix[i] = pix[i] == 0f ? 1f : 0f;

		switch (n)
		{
			case 4:
				if ( topo(im,4)==1 && topo(invertedIm, 8)==1 )
	            	return true;
				else
					return false;
			case 8:
				if ( topo(im,8)==1 && topo(invertedIm, 4)==1 )
					return true;
				else
					return false;
			default:
				IJ.error("Non valid adjacency value");
				return false;
		}
	}

	/**
	 * Computes topological numbers for the central point of an image patch.
	 * These numbers can be used as the basis of a topological classification.
	 * T_4 and T_8 are used when IM is a 2d image patch of size 3x3
	 * defined on p. 172 of Bertrand & Malandain, Patt. Recog. Lett. 15, 169-75 (1994).
	 *
	 * @param im input image
	 * @param adjacency number of neighbors
	 * @return number of components in the patch excluding the center pixel
	 */
	public static int topo(final ImagePlus im, final int adjacency)
	{
		ImageProcessor components = null;
		final ImagePlus im2 = new ImagePlus("copy of im", im.getProcessor().duplicate());
		switch (adjacency)
		{
			case 4:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=4 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point

				im2.getProcessor().set(1, 1, 0);
				components = connectedComponents(im2, adjacency).allRegions.getProcessor();
				// zero out locations that are not in the four-neighborhood
				components.set(0,0,0);
				components.set(0,2,0);
				components.set(1,1,0);
				components.set(2,0,0);
				components.set(2,2,0);
				break;
			case 8:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=8 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point
				im2.getProcessor().set(1, 1, 0);
				components = connectedComponents(im2, adjacency).allRegions.getProcessor();
				break;
			default:
				IJ.error("Non valid adjacency value");
				return -1;
		}

		if(null == components)
			return -1;

		int t = 0;
		ArrayList<Integer> uniqueId = new ArrayList<Integer>();
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
			{
				if(( t = components.get(i, j) ) != 0)
					if(!uniqueId.contains(t))
						uniqueId.add(t);
			}

		return uniqueId.size();

	}

	/**
	 * Connected components based on Find Connected Regions (from Mark Longair)
	 * @param im input image
	 * @param adjacency number of neighbors to check (4, 8...)
	 * @return list of images per regsion, all-regions image and regions info
	 */
	public static Results connectedComponents(final ImagePlus im, final int adjacency)
	{
		if( adjacency != 4 && adjacency != 8 )
			return null;

		final boolean diagonal = adjacency == 8 ? true : false;

		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			final Results r = fcr.run( im,
				 diagonal,
				 false,
				 true,
				 false,
				 false,
				 false,
				 false,
				 0,
				 1,
				 -1,
				 true /* noUI */ );
			return r;

		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return null;
		}

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
			this.featureStackArray.setEnabledFeatures(usedFeatures);
			// Force features to be updated
			updateFeatures = true;
		}

		return true;
	}

	/**
	 * Create training instances out of the user markings
	 * @return set of instances
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
						if(classes.contains(classLabels[i]) == false)
							classes.add(classLabels[i]);
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
					if( r instanceof PolygonRoi && r.getType() != Roi.FREEROI )
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

			IJ.log("# of pixels selected as " + classLabels[l] + ": " +nl);
		}

		if (trainingData.numInstances() == 0)
			return null;

		// Set the index of the class attribute
		trainingData.setClassIndex(featureStackArray.getNumOfFeatures());

		return trainingData;
	}

	/**
	 * Update whole data set with current number of classes and features
	 * 
	 * @param n slice number (>=1)
	 */
	private Instances updateTestSet(int n)
	{
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
						if(false == classNames.contains(classLabels[i]))
							classNames.add(classLabels[i]);
		}
		Instances data = featureStackArray.get(n-1).createInstances(classNames);
		long end = System.currentTimeMillis();
		IJ.log("Creating whole image data for section " + n + " took: " + (end-start) + "ms");
		data.setClassIndex(data.numAttributes() - 1);
		
		return data;
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
			IJ.log("Feature stack is now updated (" + (end-start) + "ms).");
			IJ.log("Feature stack array is now updated.");
		}

		IJ.showStatus("Training classifier...");
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
			return false;
		
		// Train the classifier on the current data
		final long start = System.currentTimeMillis();
		try{
			classifier.buildClassifier(data);
		}
		catch(Exception e){
			IJ.showMessage(e.getMessage());
			e.printStackTrace();
			return false;
		}

		// Print classifier information
		IJ.log( this.classifier.toString() );

		final long end = System.currentTimeMillis();

		if (Thread.currentThread().isInterrupted() )
			return false;
		
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
	 * Apply current classifier to a given image.
	 *
	 * @param imp image (2D single image or stack)
	 * @param numThreads The number of threads to use. Set to zero for
	 * auto-detection.
	 * @param probabilityMaps create probability maps for each class instead of
	 * a classification
	 * @return result image
	 */
	public ImagePlus applyClassifier(final ImagePlus imp, int numThreads, final boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Runtime.getRuntime().availableProcessors();

		final int numSliceThreads = Math.min(imp.getStackSize(), numThreads);
		final int numClasses      = numOfClasses;
		final int numChannels     = (probabilityMaps ? numClasses : 1);

		IJ.log("Processing slices of " + imp.getTitle() + " in " + numSliceThreads + " threads...");

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				for(int j=0; j<trainingImage.getImageStackSize(); j++)
					if(examples[j].get(i).size() > 0)
					{
						classNames.add(classLabels[i]);
						break;
					}
		}
		else
			classNames = loadedClassNames;

		final ImagePlus[] classifiedSlices = new ImagePlus[imp.getStackSize()];

		class ApplyClassifierThread extends Thread {

			final int startSlice;
			final int numSlices;
			final int numFurtherThreads;
			final ArrayList<String> classNames;

			public ApplyClassifierThread(int startSlice, int numSlices, int numFurtherThreads, ArrayList<String> classNames) {

				this.startSlice        = startSlice;
				this.numSlices         = numSlices;
				this.numFurtherThreads = numFurtherThreads;
				this.classNames        = classNames;
			}

			public void run() {

				for (int i = startSlice; i < startSlice + numSlices; i++)
				{
					final ImagePlus slice = new ImagePlus(imp.getImageStack().getSliceLabel(i), imp.getImageStack().getProcessor(i).convertToByte(true));
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
					sliceFeatures.updateFeaturesMT();
					filterFeatureStackByList(featureNames, sliceFeatures);

					final Instances sliceData = sliceFeatures.createInstances(classNames);
					sliceData.setClassIndex(sliceData.numAttributes() - 1);

					final ImagePlus classImage;
					classImage = applyClassifier(sliceData, slice.getWidth(), slice.getHeight(), numFurtherThreads, probabilityMaps);

					IJ.log("Classifying slice " + i + " in " + numFurtherThreads + " threads...");
					classImage.setTitle("classified_" + slice.getTitle());
					classImage.setProcessor(classImage.getProcessor().convertToByte(true).duplicate());
					classifiedSlices[i-1] = classImage;
				}
			}
		}

		final int numFurtherThreads = (int)Math.ceil((double)(numThreads - numSliceThreads)/numSliceThreads) + 1;
		final ApplyClassifierThread[] threads = new ApplyClassifierThread[numSliceThreads];

		int numSlices  = imp.getStackSize()/numSliceThreads;
		for (int i = 0; i < numSliceThreads; i++) {

			int startSlice = i*numSlices + 1;
			// last thread takes all the remaining slices
			if (i == numSliceThreads - 1)
				numSlices = imp.getStackSize() - (numSliceThreads - 1)*(imp.getStackSize()/numSliceThreads);

			IJ.log("Starting thread " + i + " processing " + numSlices + " slices, starting with " + startSlice);
			threads[i] = new ApplyClassifierThread(startSlice, numSlices, numFurtherThreads, classNames);

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

		// assamble classified image
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
	 * @param classify
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
			numThreads = Runtime.getRuntime().availableProcessors();

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
			IJ.log("Feature stack is now updated (" + (end-start) + "ms).");
		}
		
		if(updateWholeData)
		{			
			wholeImageData = updateWholeImageData();
			if( null == wholeImageData)
				return;
		}

		IJ.log("Classifying whole image...");
		classifiedImage = applyClassifier(wholeImageData, trainingImage.getWidth(), trainingImage.getHeight(), numThreads, classify);
		
		IJ.log("Finished segmentation of whole image.\n");
	}

	/**
	 * 
	 * @return
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
						if(false == classNames.contains(classLabels[i]))
							classNames.add(classLabels[i]);
		}
		
		final int numProcessors = Runtime.getRuntime().availableProcessors();
		final ExecutorService exe = Executors.newFixedThreadPool( numProcessors );
		
		final ArrayList< Future<Instances> > futures = new ArrayList< Future<Instances> >();
		
		try{
			

			for(int z = 1; z<=trainingImage.getImageStackSize(); z++)
			{
				IJ.log("Creating fetaure vectors for slice number " + z + "...");
				futures.add( exe.submit( createInstances(classNames, featureStackArray.get(z-1))) );
			}

			Instances data[] = new Instances[ futures.size() ];

			for(int z = 1; z<=trainingImage.getImageStackSize(); z++)
			{
				data[z-1] = futures.get(z-1).get();				
				data[z-1].setClassIndex(data[z-1].numAttributes() - 1);
			}						
					
			for(int n =0; n<data.length; n++)
			{
				//IJ.log("Test dataset updated ("+ data[n].numInstances() + " instances, " + data[n].numAttributes() + " attributes).");

				if(null == wholeImageData)
					wholeImageData = data[n];
				else
					mergeDataInPlace(wholeImageData, data[n]);				
			}
			
			IJ.log("Total dataset: "+ wholeImageData.numInstances() + 
					" instances, " + wholeImageData.numAttributes() + " attributes).");
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
	private ImagePlus applyClassifier(final Instances data, int w, int h, int numThreads, boolean probabilityMaps)
	{
		if (numThreads == 0)
			numThreads = Runtime.getRuntime().availableProcessors();

		final int numClasses   = data.numClasses();
		final int numInstances = data.numInstances();
		final int numChannels  = (probabilityMaps ? numClasses : 1);
		final int numSlices    = (numChannels*numInstances)/(w*h);

		IJ.showStatus("Classifying image...");

		final long start = System.currentTimeMillis();

		exe = Executors.newFixedThreadPool(numThreads);
		final double[][][] results = new double[numThreads][][];
		final Instances[] partialData = new Instances[numThreads];
		final int partialSize = numInstances / numThreads;
		Future<double[][]> fu[] = new Future[numThreads];

		final AtomicInteger counter = new AtomicInteger();

		for(int i = 0; i < numThreads; i++)
		{
			if (Thread.currentThread().isInterrupted()) 
				return null;
			if(i == numThreads - 1)
				partialData[i] = new Instances(data, i*partialSize, numInstances - i*partialSize);
			else
				partialData[i] = new Instances(data, i*partialSize, partialSize);

			fu[i] = exe.submit(classifyInstances(partialData[i], classifier, counter, probabilityMaps));
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
				e.printStackTrace();
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
				classStack.addSlice(probabilityMaps ? classLabels[c] : "", classifiedSliceProcessor);
			}
		}
		ImagePlus classImg = new ImagePlus(probabilityMaps ? "Probability maps" : "Classification result", classStack);

		return classImg;
	}

	/**
	 * Classify instance concurrently
	 * @param data set of instances to classify
	 * @param classifier current classifier
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

						if (0 == i % 4000) counter.addAndGet(4000);

						if (probabilityMaps)
						{
							double[] prob = classifier.distributionForInstance(data.get(i));
							for(int k = 0 ; k < numClasses; k++)
								classificationResult[k][i] = prob[k];
						}
						else
							classificationResult[0][i] = classifier.classifyInstance(data.instance(i));

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

	public void setMembraneThickness(int thickness)
	{
		this.membraneThickness = thickness;
		featureStackArray.setMembraneSize(thickness);
	}

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

	public float getMinimumSigma()
	{
		return minimumSigma;
	}

	public int getNumOfTrees()
	{
		return numOfTrees;
	}

	public int getNumRandomFeatures()
	{
		return randomFeatures;
	}

	public int getMaxDepth()
	{
		return maxDepth;
	}

	public void setDoHomogenizeClasses(boolean homogenizeClasses)
	{
		this.homogenizeClasses = homogenizeClasses;
	}

	public boolean doHomogenizeClasses()
	{
		return homogenizeClasses;
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
	
	public void mergeDataInPlace(Instances first, Instances second)
	{
		for(int i=0; i<second.numInstances(); i++)
			first.add(second.get(i));
	}

	/**
	 * Shut down the executor service
	 */
	public void shutDownNow()
	{
		featureStackArray.shutDownNow();
		exe.shutdownNow();		
	}
	
}
