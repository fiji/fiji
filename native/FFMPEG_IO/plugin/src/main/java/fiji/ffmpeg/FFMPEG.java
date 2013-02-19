package fiji.ffmpeg;

/*
 * Base class to handle loading the FFMPEG libraries.
 */

import fiji.ffmpeg.AVCODEC;
import fiji.ffmpeg.AVFORMAT;
import fiji.ffmpeg.AVUTIL;

public class FFMPEG extends JNALibraryLoader {
	protected static AVUTIL avUtil;
	protected static AVCORE avCore;
	protected static AVDEVICE avDevice;
	protected static AVCODEC avCodec;
	protected static AVFORMAT avFormat;
	protected static AVLOG avLog;
	protected static SWSCALE swScale;

	public boolean loadFFMPEG() {
		if (avFormat != null)
			return true;

		try {
			avUtil = loadLibrary("avutil",
				AVUTIL.LIBAVUTIL_VERSION_MAJOR, AVUTIL.class);
			avCore = loadLibrary("avcore",
				AVCORE.LIBAVCORE_VERSION_MAJOR, AVCORE.class);
			avDevice = loadLibrary("avdevice",
				AVDEVICE.LIBAVDEVICE_VERSION_MAJOR, AVDEVICE.class);
			avCodec = loadLibrary("avcodec",
				AVCODEC.LIBAVCODEC_VERSION_MAJOR, AVCODEC.class);
			avFormat = loadLibrary("avformat",
				AVFORMAT.LIBAVFORMAT_VERSION_MAJOR, AVFORMAT.class);
			avLog = loadLibrary("avlog",
				AVLOG.LIBAVLOG_VERSION_MAJOR, AVLOG.class);
			swScale = loadLibrary("swscale",
				SWSCALE.LIBSWSCALE_VERSION_MAJOR, SWSCALE.class);
		} catch (UnsatisfiedLinkError e) {
			showException(e);
			return false;
		}
		return true;
	}
}
