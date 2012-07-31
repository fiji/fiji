package fiji.ffmpeg;

/*
 * Base class to handle loading the FFMPEG libraries.
 */

import fiji.ffmpeg.AVCODEC;
import fiji.ffmpeg.AVFORMAT;
import fiji.ffmpeg.AVUTIL;

public class FFMPEG extends JNALibraryLoader {
	protected static AVUTIL AVUTIL;
	protected static AVCORE AVCORE;
	protected static AVDEVICE AVDEVICE;
	protected static AVCODEC AVCODEC;
	protected static AVFORMAT AVFORMAT;
	protected static AVLOG AVLOG;
	protected static SWSCALE SWSCALE;

	public boolean loadFFMPEG() {
		if (AVFORMAT != null)
			return true;

		try {
			AVUTIL = (AVUTIL)loadLibrary("avutil",
				AVUTIL.LIBAVUTIL_VERSION_MAJOR, AVUTIL.class);
			AVCORE = (AVCORE)loadLibrary("avcore",
				AVCORE.LIBAVCORE_VERSION_MAJOR, AVCORE.class);
			AVDEVICE = (AVDEVICE)loadLibrary("avdevice",
				AVDEVICE.LIBAVDEVICE_VERSION_MAJOR, AVDEVICE.class);
			AVCODEC = (AVCODEC)loadLibrary("avcodec",
				AVCODEC.LIBAVCODEC_VERSION_MAJOR, AVCODEC.class);
			AVFORMAT = (AVFORMAT)loadLibrary("avformat",
				AVFORMAT.LIBAVFORMAT_VERSION_MAJOR, AVFORMAT.class);
			AVLOG = (AVLOG)loadLibrary("avlog",
				AVLOG.LIBAVLOG_VERSION_MAJOR, AVLOG.class);
			SWSCALE = (SWSCALE)loadLibrary("swscale",
				SWSCALE.LIBSWSCALE_VERSION_MAJOR, SWSCALE.class);
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}
}
