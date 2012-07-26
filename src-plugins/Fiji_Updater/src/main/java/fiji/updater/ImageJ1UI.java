package fiji.updater;

import ij.IJ;
import ij.macro.Interpreter;

/**
 * Simple ImageJ 1.x implementation of the ImageJ Updater User Interface
 * 
 * To be able to run without ij.jar in the class path, {@link Adapter} tries to
 * call this method. If it fails to run, we will fall back to outputting to stderr
 * instead.
 *
 * @author Johannes Schindelin
 */
public class ImageJ1UI implements UI {

	/**
	 * Show the status using the {@link IJ} class.
	 * 
	 * @param message
	 *            the status message
	 */
	@Override
	public void showStatus(String message) {
		IJ.showStatus(message);
	}

	/**
	 * Show an error using the {@link IJ} class.
	 * 
	 * @param message
	 *            the error message
	 */
	@Override
	public void error(String message) {
		IJ.showStatus(message);
	}

	/**
	 * Show a stack trace using the {@link IJ} class.
	 * 
	 * @param throwable
	 *            the exception to show
	 */
	@Override
	public void handleException(Throwable exception) {
		IJ.handleException(exception);
	}

	/**
	 * Determine whether we should not bother to show the dialog.
	 * 
	 * @return whether we're in batch mode or other circumstances indicate we
	 *         should not run the ImageJ updater interactively
	 */
	@Override
	public boolean isBatchMode() {
		return IJ.getInstance() == null || !IJ.getInstance().isVisible()
			|| Interpreter.isBatchMode();
	}
}
