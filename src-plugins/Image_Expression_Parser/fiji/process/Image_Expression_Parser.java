package fiji.process;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

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
import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.DoubleNumberFactory;

public class Image_Expression_Parser<T extends NumericType<T>> implements PlugIn, ActionListener {
	
	protected ImagePlus image;
	protected boolean user_has_canceled = false;
	/** Array of Imglib images, on which calculations will be done */
	protected ArrayList<Image<T>> images;
	/** Array of image variables */
	protected String[] variables;
	/** The expression to evaluate */
	protected String expression;


	
	/**
	 * 
	 */
//	@Override
	public synchronized void run(String arg) {
		// Launch GUI and wait for user 
		IepGui gui = displayGUI();
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}			
		gui.dispose();
		ImagePlus.removeImageListener(gui);
		if (user_has_canceled) {
			return;
		}

		// Get user settings
		expression 	= gui.getExpression();
		variables 	= gui.getVariables();
		ImagePlus[] imps = gui.getImages();
		convertToImglib(imps); // will set #images field
		
		// Check inputs (this should be done in the GUI)
		boolean valid_dimensions = checkDimensions();
		if (!valid_dimensions) { 
			IJ.error("Input images do not have all the same dimensions.");
			return;
		}

		// Exec
		Image<FloatType> result = getResult();
		ImagePlus imp_result = ImageJFunctions.copyToImagePlus(result);
		imp_result.show();
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private Image<FloatType> getResult() {
		
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
		Image<FloatType> result = new ImageFactory<FloatType>(new FloatType(), new ImagePlusContainerFactory())
			.createImage(first_im.getDimensions(), expression);
		
		// Create cursor for new image
		LocalizableByDimCursor<FloatType> result_cursor = result.createLocalizableByDimCursor();
		
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
		return result;	
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
	 * Check that all images have the same dimensions
	 */
	private boolean checkDimensions() {
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
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Image_Expression_Parser iep = new Image_Expression_Parser();
		iep.run("");
	}


}


