package fiji;

import ij.IJ;

import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.service.AbstractService;

/**
 * A status service adapter for ImageJ 1.x.
 *
 * This class implements a SciJava StatusService backend for ImageJ 1.x.
 */
public class IJ1StatusService extends AbstractService implements StatusService {
	@Override
	public void showProgress(final int value, final int maximum) {
		IJ.showProgress(value, maximum);
	}

	@Override
	public void showStatus(final String message) {
		IJ.showStatus(message);
	}

	@Override
	public void showStatus(final int progress, final int maximum, final String message) {
		showProgress(progress, maximum);
		showStatus(message);
	}

	@Override
	public void showStatus(final int progress, final int maximum, final String message, final boolean warn) {
		showProgress(progress, maximum);
		warn(message);
	}

	@Override
	public void warn(final String message) {
		IJ.log("Warning: " + message);
	}

	@Override
	public void clearStatus() {
		showStatus("");
	}

	@Override
	public String getStatusMessage(final String appName, final StatusEvent statusEvent) {
		return "";
	}
}
