package fiji.ffmpeg;

import ij.IJ;

public class IJProgress implements Progress {
	public void start(String message) {
		IJ.showStatus(message);
	}

	public void step(String message, double progress) {
		if (message != null)
			IJ.showStatus(message);
		IJ.showProgress(progress);
	}

	public void done(String message) {
		IJ.showProgress(1, 1);
		IJ.showStatus(message);
	}

	public void log(String message) {
		IJ.log(message);
	}
}
