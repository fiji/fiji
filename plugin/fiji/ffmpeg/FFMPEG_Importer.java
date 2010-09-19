package ffmpeg;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.io.File;

import fiji.ffmpeg.AVUTIL;

import fiji.ffmpeg.AVCODEC;
import fiji.ffmpeg.AVCODEC.AVCodec;
import fiji.ffmpeg.AVCODEC.AVCodecContext;
import fiji.ffmpeg.AVCODEC.AVFrame;
import fiji.ffmpeg.AVCODEC.AVPacket;

import fiji.ffmpeg.AVFORMAT;
import fiji.ffmpeg.AVFORMAT.AVFormatContext;
import fiji.ffmpeg.AVFORMAT.AVStream;

/**
 * Based on the AVCodecSample example from ffmpeg-java by Ken Larson.
 */

public class FFMPEG_Importer extends ImagePlus implements PlugIn {
	int max_slice_count = 124;

	/** Takes path as argument, or asks for it and then open the image.*/
	public void run(final String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose movie file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		/* Need to do this because we already extend ImagePlus */
		FFMPEG ffmpeg = new FFMPEG();
		if (!ffmpeg.loadFFMPEG()) {
			IJ.error("This plugin needs ffmpeg to be installed!");
			return;
		}

		final AVUTIL AVUTIL = ffmpeg.AVUTIL;
		final AVCODEC AVCODEC = ffmpeg.AVCODEC;
		final AVFORMAT AVFORMAT = ffmpeg.AVFORMAT;

		// not sure what the consequences of such a mismatch are,
		// but it is worth logging a warning:
		if (AVCODEC.avcodec_version() !=
				AVCODEC.LIBAVCODEC_VERSION_INT)
			IJ.write("ffmpeg-java and ffmpeg versions do not match:"
					+ " avcodec_version="
					+ AVCODEC.avcodec_version()
					+ " LIBAVCODEC_VERSION_INT="
					+ AVCODEC.LIBAVCODEC_VERSION_INT);

		AVFORMAT.av_register_all();

		// TODO: get number of frames, and show dialog for max_count

		final PointerByReference ppFormatCtx = new PointerByReference();

		// Open video file
		if (AVFORMAT.av_open_input_file(ppFormatCtx,
					file.getAbsolutePath(), null, 0, null) != 0) {
			IJ.error("Could not open " + file);
			return;
		}

		final AVFormatContext formatCtx =
			new AVFormatContext(ppFormatCtx.getValue());

		// Retrieve stream information
		if (AVFORMAT.av_find_stream_info(formatCtx) < 0) {
			IJ.error("No stream in " + file);
			return;
		}

		// Find the first video stream
		int videoStream = -1;
		for (int i = 0; i < formatCtx.nb_streams; i++) {
			final AVStream stream =
				new AVStream(formatCtx.getStreams()[i]);
			final AVCodecContext codecCtx =
				new AVCodecContext(stream.codec);
			if (codecCtx.codec_type ==
					AVCODEC.CODEC_TYPE_VIDEO) {
				videoStream = i;
				break;
			}
		}
		if (videoStream == -1) {
			IJ.error("No video stream in " + file);
			return;
		}

		// Get a pointer to the codec context for the video stream
		final Pointer pCodecCtx =
			new AVStream(formatCtx.getStreams()[videoStream]).codec;
		final AVCodecContext codecCtx = new AVCodecContext(pCodecCtx);

		if (codecCtx.codec_id == 0) {
			IJ.error("Codec not available");
			return;
		}

		// Find the decoder for the video stream
		final AVCodec codec =
			AVCODEC.avcodec_find_decoder(codecCtx.codec_id);
		if (codec == null) {
			IJ.error("Codec not available");
			return;
		}

		// Open codec
		if (AVCODEC.avcodec_open(codecCtx, codec) < 0) {
			IJ.error("Codec not available");
			return;
		}

		// Allocate video frame
		final AVFrame frame = AVCODEC.avcodec_alloc_frame();
		if (frame == null) {
			IJ.error("Could not allocate frame");
			return;
		}

		// Allocate an AVFrame structure
		final AVFrame frameRGB = AVCODEC.avcodec_alloc_frame();
		if (frameRGB == null)
			throw new RuntimeException("Could not allocate frame");

		// Determine required buffer size and allocate buffer
		final int numBytes =
			AVCODEC.avpicture_get_size(AVCODEC.PIX_FMT_RGB24,
					codecCtx.width, codecCtx.height);
		final Pointer buffer = AVUTIL.av_malloc(numBytes);

		// Assign appropriate parts of buffer to image planes
		// in pFrameRGB
		AVCODEC.avpicture_fill(frameRGB, buffer,
				AVCODEC.PIX_FMT_RGB24,
				codecCtx.width, codecCtx.height);

		ImageStack stack =
			new ImageStack(codecCtx.width, codecCtx.height);

		// Read frames and save first five frames to disk
		final AVPacket packet = new AVPacket();
		while (AVFORMAT.av_read_frame(formatCtx, packet) >= 0) {
			// Is this a packet from the video stream?
			if (packet.stream_index != videoStream)
				continue;

			final IntByReference frameFinished =
				new IntByReference();
			// Decode video frame
			AVCODEC.avcodec_decode_video(codecCtx, frame,
					frameFinished,
					packet.data, packet.size);

			// Did we get a video frame?
			if (frameFinished.getValue() == 0)
				continue;

			// Convert the image from its native format to RGB
			AVCODEC.img_convert(frameRGB,
					AVCODEC.PIX_FMT_RGB24,
					frame, codecCtx.pix_fmt,
					codecCtx.width,
					codecCtx.height);

			if (stack.getSize() >= max_slice_count) {
				IJ.error("Movie " + file.getName()
					+ " is too large!\n"
					+ "Only imported the first "
					+ max_slice_count + " frames!");
				break;
			}
			ImageProcessor ip = toSlice(frameRGB,
					codecCtx.width, codecCtx.height);
			stack.addSlice(null, ip);

			// Free the packet that was allocated by av_read_frame
			// AVFORMAT.av_free_packet(packet.getPointer())
			// - cannot be called because it is an inlined function.
			// so we'll just do the JNA equivalent of the inline:
			if (packet.destruct != null)
				packet.destruct.callback(packet);

		}

		// Free the RGB image
		AVUTIL.av_free(frameRGB.getPointer());

		// Free the YUV frame
		AVUTIL.av_free(frame.getPointer());

		// Close the codec
		AVCODEC.avcodec_close(codecCtx);

		// Close the video file
		AVFORMAT.av_close_input_file(formatCtx);

		if (stack.getSize() > 0) {
			setStack(file.getName(), stack);

			if (arg.equals(""))
				show();
		}
	}

	static ColorProcessor toSlice(AVFrame frame, int width, int height) {
		final int len = height * frame.linesize[0];
		final byte[] data = frame.data0.getByteArray(0, len);
		int[] pixels = new int[width * height];
		for (int j = 0; j < height; j++) {
			final int off = j * frame.linesize[0];
			for (int i = 0; i < width; i++)
				pixels[i + j * width] =
					((data[off + 3 * i]) & 0xff) << 16 |
					((data[off + 3 * i + 1]) & 0xff) << 8 |
					((data[off + 3 * i + 2]) & 0xff);
		}
		return new ColorProcessor(width, height, pixels);
	}
}
