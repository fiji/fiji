package fiji.updater;

/**
 * User interface abstraction of the ImageJ Updater adapter.
 * 
 * Since we would like to be able to run without having ij.jar in the class
 * path, we want to be able to ask {@link IJ} to do the job or in the
 * alternative output to stderr instead.
 * 
 * @see StderrUI, ImageJ1UI
 */
public interface UI {
	public void showStatus(String message);
	public void error(String message);
	public void handleException(Throwable exception);
	public boolean isBatchMode();
}
