package fiji.updater;

public class StderrUI implements UI {
	/**
	 * Show the status
	 *
	 * @param status the status message to show
	 */
	@Override
	public void showStatus(String status) {
		System.err.println(status);
	}

	/**
	 * Show an error
	 *
	 * Since we would like to be able to run without having ij.jar in the class
	 * path, we ask {@link Updater#error(String)} to do the job and if it fails,
	 * fall back to printing to stderr instead.
	 *
	 * @param status the status message to show
	 */
	@Override
	public void error(String message) {
		System.err.println(message);
	}

	/**
	 * Show a stack trace
	 *
	 * Since we would like to be able to run without having ij.jar in the class
	 * path, we ask {@link Updater#handleException(Throwable)} to do the job and
	 * if it fails, fall back to printing to stderr instead.
	 *
	 * @param throwable the exception to show
	 */
	@Override
	public void handleException(Throwable exception) {
		exception.printStackTrace();
	}

	/**
	 * Tell whether we're in ImageJ 1.x batch mode
	 *
	 * To be able to run without
	 * ij.jar in the class path, we'll try to call {@linkplain
	 * Updater#isBatchMode()}. If it fails to run, it will assume that ImageJ 1.x
	 * is not available, and as such, no ImageJ batch mode.
	 */
	@Override
	public boolean isBatchMode() {
		return false;
	}
}
