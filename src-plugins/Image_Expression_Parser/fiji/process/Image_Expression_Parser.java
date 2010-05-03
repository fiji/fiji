package fiji.process;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibParser;

/**
 * This plugins parses mathematical expressions and compute results using images as variables. 
 * As of version 2.x, pixel per pixel based operations are supported and ImgLib algorithm
 * are supported.
 * <p>
 * The parsing ability is provided by the JEP library: Java Expression Parser v.jep-2.4.1-ext-1.1.1-gpl.
 * This is the last version released under the GPL by its authors Nathan Funk and Richard Morris,
 * see {@link http://www.singularsys.com/jep/}.
 * <p>
 * Internally, this plugin uses ImgLib to deal with images. 
 * <p>
 * The interactive version (launched from ImageJ) uses a GUI, see {@link IepGui}. It is possible 
 * to use this plugin in scripts using the following methods:
 * <ul>
 * 	<li> {@link #setExpression(String)} to pass the expression to parse
 * 	<li> {@link #setImageMap(Map)} to pass the couples (variable name, image) to the parser
 * 	<li> {@link #exec()} to compute the resulting image
 * 	<li> {@link #getResult()} to retrieve the resulting image
 * </ul>
 * 
 * versions:
 * <ul>
 * 	<li> v1.0 - Feb 2010 - First working version.
 * 	<li> v1.1 - Apr 2010 - Expression field now has a history.
 * 	<li> v2.0 - May 2010 - Complete logic rewrite:
 * 		<ul>
 * 			<li> functions are now handled by code specific for ImgLib;
 * 			<li> support for ImgLib algorithms and non pixel-based operations, such as gaussian convolution;
 * 			<li> faster evaluation, thanks to dealing with ImgLib images as objects within the parser instead of pixel per pixel evaluation.
 * 		</ul>
 * </ul>
 *   
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>
 */	
public class Image_Expression_Parser<T extends RealType<T>> implements PlugIn, ActionListener {
	
	protected boolean user_has_canceled = false;
	/** Array of Imglib images, on which calculations will be done */
	protected Map<String, Image<T>> image_map;
	/** The expression to evaluate */
	protected String expression;
	/** Here is stored the result of the evaluation */
	protected Image<FloatType> result = null;
	/** If an error occurred, an error message is put here	 */
	protected String error_message = "";
	
	private ArrayList<ActionListener> action_listeners = new ArrayList<ActionListener>();
	
	/** This plugin sends a ActionEvent with this command when the external calculation is over. */
	public static final String CALCULATION_DONE_COMMAND	= "CalculationDone";
	/** This plugin sends a ActionEvent with this command when the external calculation is over. */
	public static final String CALCULATION_STARTED_COMMAND	= "CalculationStarted";

	
	/*
	 * RUN METHOD
	 */
	
	/**
	 * Launch the interactive version if this plugin. This is made by first
	 * displaying the GUI, then looping, waiting for the user to press the 
	 * compute button. When it does so, initiate calculation, and resume wait mode.
	 *  Must be launched from ImageJ.
	 */
	public synchronized void run(String arg) {
		// Launch GUI and wait for user 
		IepGui gui = displayGUI();		
		addActionListener(gui);
		ImagePlus target_imp = null;
		while (true) {
			
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
			if (user_has_canceled) {
				gui.removeActionListener(this);
				ImagePlus.removeImageListener(gui);
				gui.dispose();
				return;
			}

			// Get user settings
			expression 	= gui.getExpression();
			Map<String,ImagePlus> imp_map = gui.getImageMap(); 
			convertToImglib(imp_map); // will set #images field 

			// Check inputs (this should be done in the GUI)
			if (!dimensionsAreValid()) {
				error_message = "Input images do not have all the same dimensions.";
				IJ.error(error_message);
			} else {

				// Check if expression is valid (this too)
				Object[] validity = isExpressionValid();
				boolean is_valid = (Boolean) validity[0];
				String error_msg = (String) validity[1];
				if (!is_valid) {
					error_message = "Expression is invalid:\n"+error_msg; 
					IJ.error(error_message);
					return;
				}

				// Exec
				IJ.showStatus("IEP parsing....");
				fireActionProperty(CALCULATION_STARTED_COMMAND);
				exec();

				if (target_imp == null) {
					target_imp = ImageJFunctions.copyToImagePlus(result);
					target_imp.show();
				} else {
					ImagePlus new_imp = ImageJFunctions.copyToImagePlus(result);
					if (!target_imp.isVisible()) {
						target_imp = new_imp;
						target_imp.show();
					} else {
						target_imp.setStack(expression, new_imp.getStack());
					}
				}
				target_imp.resetDisplayRange();
				target_imp.updateAndDraw();
				IJ.showStatus("");
				fireActionProperty(CALCULATION_DONE_COMMAND);
			}
		}
	}

	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Execute calculation, given the expression, variable list and image list set for
	 * this instance. The resulting image can be accessed afterwards by using {@link #getResult()}.
	 * <p>
	 * If the expression is invalid or if the image dimensions mismatch, and error
	 * is thrown and the field {@link #result} is set to <code>null</code>. In this
	 * case, an explanatory error message can be obtained by {@link #getErrorMessage()}.
	 * @see {@link #setExpression(String)}, {@link #setImageMap(Map)}, {@link #getErrorMessage()}, {@link #getResult()}
	 */
	@SuppressWarnings("unchecked")
	public void exec() {
		
		result = null;

		// Check inputs 
		if (!dimensionsAreValid()) {
			error_message = "Input images do not have all the same dimensions.";
			return;
		}
		
		// Check if expression is valid 
		Object[] validity = isExpressionValid();
		boolean is_valid = (Boolean) validity[0];
		String error_msg = (String) validity[1];
		if (!is_valid) {
			error_message = "Expression is invalid:\n"+error_msg;
			return;
		}
		
		// Instantiate and prepare parser
		final ImgLibParser<T> parser = new ImgLibParser<T>();
		parser.addStandardConstants();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
		Set<String> variables = image_map.keySet();
		for (String var : variables) {
			parser.addVariable(var, image_map.get(var));
		}
		
		try {
			Node root_node = parser.parse(expression);
			result = (Image<FloatType>) parser.evaluate(root_node);
			result.setName(expression);
			error_message = "";

		} catch (ParseException e) {
			error_message = e.getErrorInfo();

		}
		

		
	}
	
	
	
	/*
	 * SETTERS AND GETTERS 
	 */
	

	/**
	 * Return the result of the last evaluation of the expression over the images given.
	 * Is <code>null</code> if {@link #exec()} was not called before.
	 */
	public Image<FloatType> getResult()  {
		return this.result;
	}
	
	/**
	 * If an error occurred during the call of {@link #exec()}, an error message can be read here. 
	 */
	public String getErrorMessage() {
		return this.error_message;
	}
	
	/**
	 * Set the expression to evaluate.
	 */
	public void setExpression(String _expression) {
		this.expression = _expression;
	}
	
	public String getExpression() {
		return this.expression;
	}
	
	public void setImageMap(Map<String, Image<T>> im) {
		this.image_map = im;
	}
	
	public Map<String, Image<T>> getImageMap() {
		return this.image_map;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private synchronized void fireActionProperty(String command) {
		ActionEvent action = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
		for (ActionListener l : action_listeners) {
			synchronized (l) {
				l.actionPerformed(action);
			}
		}
	}
	
	public void addActionListener(ActionListener l) {
		action_listeners.add(l);
	}

	public void removeActionListener(ActionListener l) {
		action_listeners.remove(l);
	}

	public ActionListener[] getActionListeners() {
		return (ActionListener[]) action_listeners.toArray();
	}
	
	/**
	 * Launch and display the GUI. Returns a reference to it that can be used
	 * to retrieve settings.
	 */
	private IepGui displayGUI() {
		IepGui gui = new IepGui();
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
		ImagePlus.addImageListener(gui);
		gui.addActionListener(this);
		return gui;
	}

	/**
	 * Check that all images have the same dimensions. 
	 */
	private boolean dimensionsAreValid() {
		if (image_map.size() == 1) { return true; }
		Collection<Image<T>> images = image_map.values();
		Iterator<Image<T>> it = images.iterator();
		int[] previous_dims = it.next().getDimensions();
		int[] dims;
		while (it.hasNext()) {
			dims = it.next().getDimensions();
			if (previous_dims.length != dims.length) { return false; }
			for (int j = 0; j < dims.length; j++) {
				if (dims[j] != previous_dims[j]) { return false; }
			}
			previous_dims = dims;
		}
		return true;		
	}

	/**
	 * Check that the current expression is valid.
	 * <p>
	 * Return a 2 elements array:
	 * <ul>
	 * <li> the first one is a boolean, true if the expression is valid, false otherwise;
	 * <li> the second one is a String containing the parser error message if the expression is invalid, 
	 * or the empty string if it is valid.
	 * </ul>
	 */
	private Object[] isExpressionValid() {
		final ImgLibParser<T> parser = new ImgLibParser<T>();
		parser.addStandardConstants();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
		Set<String> variables = image_map.keySet();
		for ( String var : variables ) {
			parser.addVariable(var, null); // we do not care for value yet
		}
		parser.parseExpression(expression);
		final String error = parser.getErrorInfo();
		if ( null == error) {
			return new Object[] { true, "" };
		} else {
			return new Object[] { false, error };
		}
	}
	
	/**
	 * Convert the <String ImagePlus> map in argument to a <String, {@link Image}> HasMap 
	 * and put it in the "{@link #image_map}" field.
	 * <p>
	 * The internals of this plugin operate on {@link Image}, but for integration within 
	 * current ImageJ, the GUI returns {@link ImagePlus}, so we have to do a conversion when 
	 * we execute this plugin from ImageJ.
	 * <p>
	 * Warning: executing this method resets the {@link #image_map} field.
	 * @param imp_map  the <String, ImagePlus> map to convert
	 */
	private void convertToImglib(Map<String, ImagePlus> imp_map) {
		image_map = new HashMap<String, Image<T>>(imp_map.size());
		Image<T> img;
		Set<String> variables = imp_map.keySet();
		for (String var : variables) {
			img = ImagePlusAdapter.wrap(imp_map.get(var));
			image_map.put(var, img);
		}
	}
	
	/*
	 * ACTIONLISTENER METHOD
	 */

	public synchronized void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if (command.equals(IepGui.QUIT_ACTION_COMMAND)) {
			user_has_canceled = true;
		} else if (command.equals(IepGui.PARSE_ACTION_COMMAND)) {
			user_has_canceled = false;
		}
	}
	
	/*
	 * MAIN METHOD
	 */
		
	public static <T extends RealType<T>> void main(String[] args) {
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();
		
		Image_Expression_Parser<T> iep = new Image_Expression_Parser<T>();
		iep.setExpression("A^2");
		HashMap<String, Image<T>> map = new HashMap<String, Image<T>>(1);
		map.put("A", img);
		iep.setImageMap(map);
		iep.exec();
		Image<FloatType> result = iep.getResult();
		if (null != result) {
			ImagePlus result_imp = ImageJFunctions.copyToImagePlus(result);

			float max = Float.NEGATIVE_INFINITY;
			float min = Float.POSITIVE_INFINITY;
			for (int i = 0; i < result_imp.getStackSize(); i++) {
				FloatProcessor fp = (FloatProcessor) result_imp.getStack().getProcessor(i+1);
				float[] arr = (float[]) fp.getPixels();
				for (int j = 0; j < arr.length; j++) {
					if (arr[j] > max) max = arr[j];
					if (arr[j] < min) min = arr[j];
				}
			}
			result_imp.show();	
			result_imp.setDisplayRange(min, max);
			result_imp.updateAndDraw();
		} else {
			System.err.println("Could not evaluate expression:");
			System.err.println(iep.getErrorMessage());
		}
	}
}


