package fiji;

import ij.IJ;

import org.scijava.log.AbstractLogService;

/**
 * A log service adapter for ImageJ 1.x.
 *
 * This class implements a SciJava LogService backend for ImageJ 1.x.
 */
public class IJ1LogService extends AbstractLogService {
	@Override
	protected void log(final String message) {
		IJ.log(message);
	}

	@Override
	protected void log(final Throwable t) {
		IJ.handleException(t);
	}
}