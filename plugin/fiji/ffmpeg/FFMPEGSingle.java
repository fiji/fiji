package fiji.ffmpeg;

/*
 * Base class to handle loading the consolidated FFMPEG library.
 */

public class FFMPEGSingle extends JNALibraryLoader {
	protected interface FFMPEG extends AVUTIL, AVCORE, AVDEVICE, AVCODEC, AVFORMAT {
	}
	protected static FFMPEG AVUTIL;
	protected static FFMPEG AVCORE;
	protected static FFMPEG AVDEVICE;
	protected static FFMPEG AVCODEC;
	protected static FFMPEG AVFORMAT;
	//protected static SWScaleLibrary SWSCALE;

	public boolean loadFFMPEG() {
		if (AVFORMAT != null)
			return true;

		try {
			AVUTIL = (FFMPEG)loadLibrary("ffmpeg", 0, FFMPEG.class);
			AVCORE = AVDEVICE = AVCODEC = AVFORMAT = AVUTIL;
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}
}
