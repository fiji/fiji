package fiji.process;

import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.NumericType;

public class Image_Expression_Parser implements PlugIn, ActionListener {
	
	protected ImagePlus image;
	protected boolean user_has_canceled = false;
	/** Array of images names, for display in image boxes */
	protected ImagePlus[] images;
	/** Array of image variables */
	protected String[] variables;
	/** The expression to evaluate */
	protected String expression;

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
		images 		= gui.getImages();
		
		// DEBUG
		StringBuilder str = new StringBuilder();
		str.append("To evaluate:\n\t");
		str.append(expression);
		str.append('\n');
		str.append("with the current variables:\n");
		for (int i = 0; i < variables.length; i++) {
			str.append('\t');
			str.append(variables[i]);
			str.append(" = ");
			if ( null == images[i]) {
				str.append("no image");
			} else {
				str.append(images[i].getTitle());
			}
			str.append('\n');
		}
		System.out.println(str);

	}

	public static<T extends NumericType<T>> void add(Image<T> img, float value) {
		final Cursor<T> cursor = img.createCursor();
		final T summand = cursor.getType().createVariable();

		summand.setReal(value);

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().add(summand);
		}
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private IepGui displayGUI() {
		IepGui gui = new IepGui();
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
		ImagePlus.addImageListener(gui);
		gui.addActionListener(this);
		return gui;
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
	
	public static void main(String[] args) {
		Image_Expression_Parser iep = new Image_Expression_Parser();
		iep.run("");
	}

}


