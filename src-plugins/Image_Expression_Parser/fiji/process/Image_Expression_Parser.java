package fiji.process;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import mpicbg.imglib.container.imageplus.ImagePlusContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

import org.nfunk.jep.JEP;
import org.nfunk.jep.type.DoubleNumberFactory;

public class Image_Expression_Parser<T extends NumericType<T>> implements PlugIn, ActionListener {
	
	protected boolean user_has_canceled = false;
	/** Array of Imglib images, on which calculations will be done */
	protected ArrayList<Image<T>> images;
	/** Array of image variables */
	protected String[] variables;
	/** The expression to evaluate */
	protected String expression;
	/** Here is stored the result of the evaluation */
	protected Image<FloatType> result = null;
	/** If an error occurred, an error message is put here	 */
	protected String error_message = "";
	
	/*
	 * RUN METHOD
	 */
	
	/**
	 * Launch the interactive version if this plugin. This is made by first
	 * displaying the GUI. Must be launched from ImageJ.
	 */
	public synchronized void run(String arg) {
		// Launch GUI and wait for user 
		IepGui gui = displayGUI();
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}			
		gui.removeActionListener(this);
		ImagePlus.removeImageListener(gui);
		gui.dispose();
		if (user_has_canceled) {
			return;
		}

		// Get user settings
		expression 	= gui.getExpression();
		variables 	= gui.getVariables();
		ImagePlus[] imps = gui.getImages();
		convertToImglib(imps); // will set #images field
		
		// Check inputs (this should be done in the GUI)
		if (!dimensionsAreValid()) {
			error_message = "Input images do not have all the same dimensions.";
			IJ.error(error_message);
			return;
		}
		
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
		exec();
		ImagePlus imp_result = ImageJFunctions.copyToImagePlus(result);
		imp_result.show();
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
	 * @see {@link #getErrorMessage()}, {@link #setExpression(String)}, {@link #setVariables(String[])}, 
	 * {@link #setImageList(ArrayList)}, {@link #getResult()}
	 */
	public void exec() {
		
		// Check inputs 
		if (!dimensionsAreValid()) {
			error_message = "Input images do not have all the same dimensions.";
			result = null;
			return;
		}
		
		// Check if expression is valid 
		Object[] validity = isExpressionValid();
		boolean is_valid = (Boolean) validity[0];
		String error_msg = (String) validity[1];
		if (!is_valid) {
			error_message = "Expression is invalid:\n"+error_msg;
			result = null;
			return;
		}
		
		// Instantiate and prepare parser
		final JEP parser = new JEP(false, false, false, new DoubleNumberFactory());
		parser.addStandardConstants();
		parser.addStandardFunctions();
		for (String var : variables) {
			parser.addVariable(var, null);
		}
		parser.parseExpression(expression);
		
		// Build temp copy of images array
		ArrayList<Image<T>> temp = new ArrayList<Image<T>>(images);
		
		// Extract the first image, will be privileged. Might leave the temp array empty, but who cares.
		Image<T> first_im = temp.remove(0);
		LocalizableCursor<T> first_cursor = first_im.createLocalizableCursor(); // Canonical optimized cursor
		String first_var = variables[0];
		
		// Create cursors for other input images
		ArrayList<LocalizableByDimCursor<T>> cursors = new ArrayList<LocalizableByDimCursor<T>>(temp.size()); 
		for (Image<T> im : temp) {
			cursors.add(im.createLocalizableByDimCursor());
		}

		// Prepare new image
		Image<FloatType> result_im = new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory())
			.createImage(first_im.getDimensions(), expression);
		
		// Create cursor for new image
		LocalizableByDimCursor<FloatType> result_cursor = result_im.createLocalizableByDimCursor();
		
		// Iterate over cursors.
		// We iterate using the first cursor. The other ones are moved according to this one
		float local_value, result_value;
		LocalizableByDimCursor<T> cursor;
		while (first_cursor.hasNext()) {
			// first cursor
			first_cursor.fwd();
			local_value = first_cursor.getType().getReal();
			parser.addVariable(first_var, local_value);
			// other input cursors
			for (int i = 0; i < cursors.size(); i++) {
				cursor = cursors.get(i);
				cursor.setPosition(first_cursor);
				local_value = cursor.getType().getReal();
				parser.addVariable(variables[1+i], local_value);
			}
			// output cursors
			result_value = (float) parser.getValue();
			result_cursor.setPosition(first_cursor);
			result_cursor.getType().set(result_value);
		}
		
		// Done!
		error_message = "";
		result = result_im;	
	}
	
	
	
	/*
	 * SETTERS AND GETTERS 
	 */
	

	/**
	 * Return the result of the evaluation of the expression over the images given.
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
	
	public void setImageList(ArrayList<Image<T>> _images) {
		this.images = _images;
	}
	
	public ArrayList<Image<T>> getImageList() {
		return this.images;
	}
	
	public void setVariables(String[] _variables) {
		this.variables = _variables;
	}
	
	public String[] getVariables() {
		return this.variables;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
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
		if (images.size() == 1) { return true; }
		int[] previous_dims = images.get(0).getDimensions();
		int[] dims;
		for ( int i=1; i<images.size(); i++) {
			dims = images.get(i).getDimensions();
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
		final JEP parser = new JEP(false, false, false, new DoubleNumberFactory());
		parser.addStandardConstants();
		parser.addStandardFunctions();
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
	 * Convert the {@link ImagePlus} array "{@link #images}" to the {@link Image} ArrayList 
	 * "{@link #il_images}".
	 * <p>
	 * The internals of this plugin operate on {@link Image}, but for integration within 
	 * current ImageJ, the GUI returns {@link ImagePlus}, so we have to do a conversion when 
	 * we execute this plugin from ImageJ.
	 * <p>
	 * Warning: executing this method resets the {@link #images} field.
	 * @param imps  the ImagePlus array to convert
	 */
	private void convertToImglib(ImagePlus[] imps) {
		images = new ArrayList<Image<T>>(imps.length);
		Image<T> img;
		for (ImagePlus imp : imps) {
			img = ImagePlusAdapter.wrap(imp);
			images.add(img);
		}
	}
	
	/*
	 * ACTIONLISTENER METHOD
	 */

	public synchronized void actionPerformed(ActionEvent e) {
		if (e.getID() == IepGui.CANCELED) {
			user_has_canceled = true;
		}
	}
	
	/*
	 * MAIN METHOD
	 */
		
	public static <T extends NumericType<T>> void main(String[] args) {
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();
		
		Image_Expression_Parser<T> iep = new Image_Expression_Parser<T>();
		iep.setExpression("A^2");
		iep.setVariables(new String[] {"A"});
		ArrayList<Image<T>> images = new ArrayList<Image<T>>();
		images.add(img);
		iep.setImageList(images);
		iep.exec();
		Image<FloatType> result = iep.getResult();
		if (null != result) {
			ImagePlus result_imp = ImageJFunctions.copyToImagePlus(result);
			result_imp.getProcessor().resetMinAndMax();
			result_imp.show();			
		} else {
			System.err.println("Could not evaluate expression:");
			System.err.println(iep.getErrorMessage());
		}
	}
}


