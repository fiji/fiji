package fiji.ffmpeg;

/*
 * Base class to handle loading the consolidated FFMPEG library.
 */

public class FFMPEGSingle extends JNALibraryLoader {
	protected interface FFMPEG extends AVUTIL, AVCORE, AVDEVICE, AVCODEC, AVFORMAT, AVLOG, SWSCALE { /* this block left intentionally blank */ }
	protected static FFMPEG AVUTIL;
	protected static FFMPEG AVCORE;
	protected static FFMPEG AVDEVICE;
	protected static FFMPEG AVCODEC;
	protected static FFMPEG AVFORMAT;
	protected static FFMPEG AVLOG;
	protected static FFMPEG SWSCALE;

	public boolean loadFFMPEG() {
		if (AVFORMAT != null)
			return true;

		try {
			AVCORE = AVDEVICE = AVCODEC = AVFORMAT = AVLOG = SWSCALE = AVUTIL =
				loadLibrary("ffmpeg", -1, FFMPEG.class);
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}
}
