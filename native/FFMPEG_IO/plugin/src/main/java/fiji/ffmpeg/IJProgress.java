package fiji.ffmpeg;

import ij.IJ;

public class IJProgress implements Progress {
	@Override
	public void start(String message) {
		IJ.showStatus(message);
	}

	@Override
	public void step(String message, double progress) {
		if (message != null)
			IJ.showStatus(message);
		IJ.showProgress(progress);
	}

	@Override
	public void done(String message) {
		IJ.showProgress(1, 1);
		IJ.showStatus(message);
	}

	@Override
	public void log(String message) {
		IJ.log(message);
	}
}
