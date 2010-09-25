package fiji.ffmpeg;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import fiji.ffmpeg.AVCODEC.AVCodec;
import fiji.ffmpeg.AVCODEC.AVCodecContext;
import fiji.ffmpeg.AVCODEC.AVFrame;
import fiji.ffmpeg.AVCODEC.AVPacket;
import fiji.ffmpeg.AVCODEC.AVPicture;

import fiji.ffmpeg.AVFORMAT.AVFormatContext;
import fiji.ffmpeg.AVFORMAT.AVOutputFormat;
import fiji.ffmpeg.AVFORMAT.AVStream;
//import fiji.ffmpeg.AVFORMAT.ByteIOContext;

import java.io.File;
import java.io.IOException;

public class IO extends FFMPEGSingle implements Progress {
	protected AVFormatContext formatContext;
	protected AVCodecContext codecContext;
	protected AVCodec codec;
	protected IntByReference gotPicture = new IntByReference();
	protected AVFrame frame, frameRGB;
	protected Pointer swsContext;
	protected byte[] video_outbuf;
	protected Memory video_outbuf_memory;
	protected AVPacket packet = new AVPacket();
	protected Progress progress;

	public IO() throws IOException {
		super();
		if (!loadFFMPEG())
			throw new IOException("Could not load the FFMPEG library!");
	}

	public IO(Progress progress) throws IOException {
		this();
		setProgress(progress);
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
		AVUTIL.avSetLogCallback(new AVUTIL.AvLog() {
			public void callback(String message) {
				try {
					log(message);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	public void step(String message, double progress) {
		if (this.progress != null)
			this.progress.step(message, progress);
	}

	public void done(String message) {
		if (progress != null)
			progress.done(message);
	}

	public void log(String message) {
		if (progress != null)
			progress.log(message);
	}

	/**
	 * Based on the AVCodecSample example from ffmpeg-java by Ken Larson.
	 */
	public ImagePlus readMovie(String path, boolean useVirtualStack, final int first, final int last) throws IOException {
		/* Need to do this because we already extend ImagePlus */
		if (!loadFFMPEG())
			throw new IOException("Could not load the FFMPEG library!");

		if (AVCODEC.avcodec_version() != AVCODEC.LIBAVCODEC_VERSION_INT)
			throw new IOException("ffmpeg versions mismatch: native " + AVCODEC.avcodec_version()
					+ " != Java-bindings " + AVCODEC.LIBAVCODEC_VERSION_INT);

		step("Initializing FFMPEG", 0);
		AVFORMAT.av_register_all();

		// Open video file
		step("Opening " + path, 0);
		final PointerByReference formatContextPointer = new PointerByReference();
		if (AVFORMAT.av_open_input_file(formatContextPointer, path, null, 0, null) != 0)
			throw new IOException("Could not open " + path);
		formatContext = new AVFormatContext(formatContextPointer.getValue());

		// Retrieve stream information
		if (AVFORMAT.av_find_stream_info(formatContext) < 0)
			throw new IOException("No stream in " + path);

		// Find the first video stream
		int videoStream = -1;
		for (int i = 0; i < formatContext.nb_streams; i++) {
			final AVStream stream = new AVStream(formatContext.streams[i]);
			codecContext = new AVCodecContext(stream.codec);
			if (codecContext.codec_type == AVCODEC.CODEC_TYPE_VIDEO) {
				videoStream = i;
				break;
			}
		}
		if (codecContext == null)
			throw new IOException("No video stream in " + path);

		if (codecContext.codec_id == 0)
			throw new IOException("Codec not available");

		// Find and open the decoder for the video stream
		codec = AVCODEC.avcodec_find_decoder(codecContext.codec_id);
		if (codec == null || AVCODEC.avcodec_open(codecContext, codec) < 0)
			throw new IOException("Codec not available");

		allocateFrames(false);

		final AVStream stream = new AVStream(formatContext.streams[videoStream]);
		if (useVirtualStack) {
			// TODO: handle stream.duration == 0 by counting the frames
			if (stream.duration == 0)
				throw new IOException("Cannot determine stack size (duration is 0)");
			final int videoStreamIndex = videoStream;
			ImageStack stack = new VirtualStack(codecContext.width, codecContext.height, null, null) {
				int previousSlice = -1;
				long frameDuration = guessFrameDuration();

				public void finalize() {
					free();
				}

				public int getSize() {
					int size = (int)(stream.duration / frameDuration);
					if (last >= 0)
						size = Math.min(last, size);
					if (first > 0)
						size -= first;
					return size;
				}

				public String getSliceLabel(int slice) {
					return ""; // maybe calculate the time?
				}

				public long guessFrameDuration() {
					if (AVFORMAT.av_read_frame(formatContext, packet) < 0)
						return 1;
					long firstPTS = packet.pts;
					int frameCount = 5;
					for (int i = 0; i < frameCount; previousSlice = i++)
						if (AVFORMAT.av_read_frame(formatContext, packet) < 0)
							return 1;
					return (packet.pts - firstPTS) / frameCount;
				}

				// TODO: cache two
				public ImageProcessor getProcessor(int slice) {
					long time = (first + slice - 1) * frameDuration;
					if (time > 0)
						time -=  frameDuration / 2;
					if (stream.start_time != 0x8000000000000000l /* TODO: AVUTIL.AV_NOPTS_VALUE */)
						time += stream.start_time;
					if (previousSlice != slice - 1)
						AVFORMAT.av_seek_frame(formatContext, videoStreamIndex, time,
								AVFORMAT.AVSEEK_FLAG_BACKWARD);
					for (;;) {
						if (AVFORMAT.av_read_frame(formatContext, packet) < 0) {
							packet.data = null;
							packet.size = 0;
							break;
						}
						if (packet.stream_index != videoStreamIndex)
							continue;
						if (previousSlice == slice - 1 || packet.pts >= time)
							break;
						AVCODEC.avcodec_decode_video2(codecContext, frame, gotPicture, packet);
					}
					previousSlice = slice;
					return readOneFrame(packet);
				}
			};
			done("Opened " + path + " as virtual stack");
			return new ImagePlus(path, stack);
		}

		double factor = stream.duration > 0 ? 1.0 / stream.duration : 0;
		if (last >= 0)
			factor = 1.0 / last;
		ImageStack stack = new ImageStack(codecContext.width, codecContext.height);
		int frameCounter = 0;
		while (AVFORMAT.av_read_frame(formatContext, packet) >= 0 &&
				(last < 0 || frameCounter < last)) {
			// Is this a packet from the video stream?
			if (packet.stream_index != videoStream)
				continue;

			step("Reading frame " + (frameCounter + 1), frameCounter * factor);
			ImageProcessor ip = readOneFrame(packet);
			if (ip != null && frameCounter++ >= first)
				stack.addSlice(null, ip);
		}

		// Read the last frame
		packet.data = null;
		packet.size = 0;
		ImageProcessor ip = readOneFrame(packet);
		if (ip != null)
			stack.addSlice(null, ip);

		free();

		done("Opened " + path);
		return new ImagePlus(path, stack);
	}

	protected void allocateFrames(boolean forEncoding) {
		// Allocate video frame
		if (frame == null) {
			frame = AVCODEC.avcodec_alloc_frame();
			if (frame == null)
				throw new OutOfMemoryError("Could not allocate frame");

			if (forEncoding) {
				// Allocate buffer
				if (AVCODEC.avpicture_alloc(new AVPicture(frame.getPointer()),
						codecContext.pix_fmt, codecContext.width, codecContext.height) < 0)
					throw new OutOfMemoryError("Could not allocate tmp frame");
				frame.read();
			}
		}

		// Allocate an AVFrame structure
		if (frameRGB == null) {
			frameRGB = AVCODEC.avcodec_alloc_frame();
			if (frameRGB == null)
				throw new RuntimeException("Could not allocate frame");

			// Allocate buffer
			if (AVCODEC.avpicture_alloc(new AVPicture(frameRGB.getPointer()),
					AVUTIL.PIX_FMT_RGB24, codecContext.width, codecContext.height) < 0)
				throw new OutOfMemoryError("Could not allocate tmp frame");
			frameRGB.read();
		}

		if (swsContext == null) {
			swsContext = SWSCALE.sws_getContext(codecContext.width, codecContext.height,
					forEncoding ? AVUTIL.PIX_FMT_RGB24 : codecContext.pix_fmt,
					codecContext.width, codecContext.height,
					forEncoding ? codecContext.pix_fmt : AVUTIL.PIX_FMT_RGB24,
					SWSCALE.SWS_BICUBIC, null, null, null);
			if (swsContext == null)
				throw new OutOfMemoryError("Could not allocate swscale context");
		}
	}

	protected ImageProcessor readOneFrame(AVPacket packet) {
		// Decode video frame
		AVCODEC.avcodec_decode_video2(codecContext, frame, gotPicture, packet);

		// Did we get a video frame?
		if (gotPicture.getValue() == 0)
			return null;

		// Convert the image from its native format to RGB
		convertToRGB();
		return toSlice(frameRGB, codecContext.width, codecContext.height);
	}

	protected void convertToRGB() {
		SWSCALE.sws_scale(swsContext, frame.data, frame.linesize, 0, codecContext.height, frameRGB.data, frameRGB.linesize);
	}

	protected void convertFromRGB() {
		SWSCALE.sws_scale(swsContext, frameRGB.data, frameRGB.linesize, 0, codecContext.height, frame.data, frame.linesize);
	}

	protected void free() {
		// Free the RGB image
		if (frameRGB != null) {
			AVUTIL.av_free(frameRGB.getPointer());
			frameRGB = null;
		}

		// Close the codec
		if (codecContext != null) {
			AVCODEC.avcodec_close(codecContext);
			codecContext = null;
		}

		// Close the video file
		if (formatContext != null) {
			if (formatContext.iformat != null)
				AVFORMAT.av_close_input_file(formatContext);
			formatContext = null;
		}

		if (swsContext != null) {
			SWSCALE.sws_freeContext(swsContext);
			swsContext = null;
		}
	}

	protected static ColorProcessor toSlice(AVFrame frame, int width, int height) {
		final int len = height * frame.linesize[0];
		final byte[] data = frame.data[0].getByteArray(0, len);
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

	public static int strncpy(byte[] dst, String src) {
		int len = Math.min(src.length(), dst.length - 1);
		System.arraycopy(src.getBytes(), 0, dst, 0, len);
		dst[len] = 0;
		return len;
	}

	public void writeMovie(ImagePlus image, String path, int frameRate, int bitRate) throws IOException {
		final int STREAM_PIX_FMT = AVUTIL.PIX_FMT_YUV420P;

		//int sws_flags = SWScaleLibrary.SWS_BICUBIC;
		AVOutputFormat fmt = null;
		double video_pts;
		int i;
		ImageStack stack;

		stack = image.getStack();

		step("Initializing FFMPEG", 0);
		/* initialize libavcodec, and register all codecs and formats */
		AVFORMAT.av_register_all();

		/* auto detect the output format from the name. default is
		   mpeg. */
		fmt = AVFORMAT.guess_format(null, new File(path).getName(), null);
		if (fmt == null) {
			IJ.log("Could not deduce output format from file extension: using MPEG.");
			fmt = AVFORMAT.guess_format("mpeg2video", null, null);
		}

		if (fmt == null)
			throw new IOException("Could not find suitable output format");

		/* allocate the output media context */
		formatContext = AVFORMAT.av_alloc_format_context();
		if (formatContext == null)
			throw new OutOfMemoryError("Could not allocate format context");
		formatContext.oformat = fmt.getPointer();
		strncpy(formatContext.filename, path);

		/* add the video stream using the default format
		 * codec and initialize the codec */
		if (fmt.video_codec == AVCODEC.CODEC_ID_NONE)
			throw new IOException("Could not determine codec for " + path);
		AVStream video_st = add_video_stream(formatContext, fmt.video_codec, stack.getWidth(), stack.getHeight(), frameRate, bitRate, STREAM_PIX_FMT);
		if (video_st == null)
			throw new IOException("Could not add a video stream");

		/* set the output parameters (mustbe done even if no
		 * parameters). */
		if (AVFORMAT.av_set_parameters(formatContext, null) < 0)
			throw new IOException("Invalid output format parameters.");

		/* now that all the parameters are set, we can open the
		 * video codec and allocate the necessary encode buffer */
		step("Opening " + path, 0);
		open_video(formatContext, video_st);

		// Dump the format to stderr
		AVFORMAT.dump_format(formatContext, 0, path, 1);

		AVOutputFormat tmp_fmt = new AVOutputFormat(formatContext.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 0) {
			/* allocate output buffer */
			/* buffers passed into lav* can be allocated any way you prefer,
			   as long as they're aligned enough for the architecture, and
			   they're freed appropriately (such as using av_free for buffers
			   allocated with av_malloc) */
			video_outbuf = new byte[200000];
		}

		/* open the output file, if needed */
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			final PointerByReference p = new PointerByReference();
			if (AVFORMAT.url_fopen(p, path, AVFORMAT.URL_WRONLY) < 0)
				throw new IOException("Could not open " + path);
			formatContext.pb = p.getValue();
		}

		allocateFrames(true);

		AVFORMAT.av_write_header(formatContext);

		video_pts = (double)video_st.pts.val * video_st.time_base.num / video_st.time_base.den;

		for (int frameCount = 1; frameCount <= stack.getSize(); frameCount++) {
			/* write video frame */
			step("Writing frame " + frameCount, frameCount / (double)stack.getSize());
			write_video_frame(stack.getProcessor(frameCount), formatContext, video_st);
		}

		// flush last frame
		//write_video_frame(null, formatContext, video_st);

		/* write the trailer, if any */
		AVFORMAT.av_write_trailer(formatContext);

		/* close codec */
		//close_video(formatContext, video_st);

		/* free the streams */
		for (i = 0; i < formatContext.nb_streams; i++) {
			AVStream tmp_stream = new AVStream(formatContext.streams[i]);
			AVUTIL.av_free(tmp_stream.codec);
			AVUTIL.av_free(formatContext.streams[i]);
		}
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			/* close the output file */
			AVFORMAT.url_fclose(formatContext.pb);
		}

		//free();
	}

	protected void write_video_frame(ImageProcessor ip, AVFormatContext formatContext, AVStream st) throws IOException {
		int out_size = 0;
		//SwsContext img_convert_ctx = null;

		if (ip == null) {
			/* no more frame to compress. The codec has a latency of a few
			   frames if using B frames, so we get the last frames by
			   passing the same picture again */
		} else {
			if (codecContext.pix_fmt == AVUTIL.PIX_FMT_RGB24)
				fill_image(frame, ip);
			else {
				fill_image(frameRGB, ip);
				convertFromRGB();
			}
		}

		AVOutputFormat tmp_fmt = new AVOutputFormat(formatContext.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) != 0) {
			/* raw video case. The API will change slightly in the near
			   future for that */
			AVCODEC.av_init_packet(packet);

			packet.flags |= AVCODEC.PKT_FLAG_KEY;
			packet.stream_index = st.index;
			packet.data = frame.getPointer();
			packet.size = frame.size();

			if (AVFORMAT.av_interleaved_write_frame(formatContext, packet) != 0)
				throw new IOException("Error while writing video frame");
		} else {
			/* encode the image */
			if (video_outbuf_memory == null)
				video_outbuf_memory = new Memory(video_outbuf.length);
			// TODO: special-case avcodec_encode_video() to take a Pointer to avoid frequent copying
			out_size = AVCODEC.avcodec_encode_video(codecContext, video_outbuf, video_outbuf.length, frame);
			/* if zero size, it means the image was buffered */
			if (out_size > 0) {
				AVCODEC.av_init_packet(packet);

				AVFrame tmp_frame = new AVFrame(codecContext.coded_frame);
				packet.pts = AVUTIL.av_rescale_q(tmp_frame.pts, new AVUTIL.AVRational.ByValue(codecContext.time_base), new AVUTIL.AVRational.ByValue(st.time_base));
				if (tmp_frame.key_frame == 1)
					packet.flags |= AVCODEC.PKT_FLAG_KEY;
				packet.stream_index = st.index;
				video_outbuf_memory.write(0, video_outbuf, 0, out_size);
				packet.data = video_outbuf_memory;
				packet.size = out_size;

				/* write the compressed frame in the media file */
				if (AVFORMAT.av_interleaved_write_frame(formatContext, packet) != 0)
					throw new IOException("Error while writing video frame");

				st.pts.val = packet.pts; // necessary for calculation of video length
			}
		}
	}

	protected static void fill_image(AVFrame pict, ImageProcessor ip) {
		if (!(ip instanceof ColorProcessor))
			ip = ip.convertToRGB();
		int[] pixels = (int[])ip.getPixels();

		int i = 0, total = ip.getWidth() * ip.getHeight();
		for (int j = 0; j < total; j++) {
			int v = pixels[j];
			pict.data[0].setByte(i++, (byte)((v >> 16) & 0xff));
			pict.data[0].setByte(i++, (byte)((v >> 8) & 0xff));
			pict.data[0].setByte(i++, (byte)(v & 0xff));
		}
	}

	protected void open_video(AVFormatContext formatContext, AVStream st) throws IOException {
		AVCodec codec;

		/* find the video encoder */
		codec = AVCODEC.avcodec_find_encoder(codecContext.codec_id);
		if (codec == null)
			throw new IOException("video codec not found for codec id: " + codecContext.codec_id);

		/* open the codec */
		if (AVCODEC.avcodec_open(codecContext, codec) < 0)
			throw new IOException("Could not open video codec");
	}

	protected static void close_video(AVFormatContext formatContext, AVStream st) {
		AVCodecContext tmp_codec = new AVCodecContext(st.codec);
		AVCODEC.avcodec_close(tmp_codec);
	}

	protected AVStream add_video_stream(AVFormatContext formatContext, int codec_id, int width, int height, int frameRate, int bitRate, int pixelFormat) {
		AVStream st;

		st = AVFORMAT.av_new_stream(formatContext, 0);
		if (st == null) {
			IJ.error("Could not alloc video stream.");
			return null;
		}
		codecContext = new AVCodecContext(st.codec);
		codecContext.codec_id = codec_id;
		codecContext.codec_type = AVCODEC.CODEC_TYPE_VIDEO;

		/* put sample parameters */
		codecContext.bit_rate = bitRate;
		/* resolution must be a multiple of two */
		codecContext.width = width; //352;
		codecContext.height = height; //288;
		/* time base: this is the fundamental unit of time (in seconds) in terms
		   of which frame timestamps are represented. for fixed-fps content,
		   timebase should be 1/framerate and timestamp increments should be
		   identically 1. */
		codecContext.time_base.den = frameRate;
		codecContext.time_base.num = 1;
		codecContext.gop_size = 12;
		codecContext.pix_fmt = pixelFormat;

		if (codecContext.codec_id == AVCODEC.CODEC_ID_MPEG2VIDEO) {
			/* just for testing, we also add B frames */
			codecContext.max_b_frames = 2;
		}
		if (codecContext.codec_id == AVCODEC.CODEC_ID_MPEG1VIDEO) {
			/* Needed to avoid using macroblocks in which some coeffs overflow.
			   This does not happen with normal video, it just happens here as
			   the motion of the chroma plane does not match the luma plane. */
			codecContext.mb_decision = 2;
		}

		// some formats want stream headers to be separate
		if ((new AVFORMAT.AVOutputFormat(formatContext.oformat).flags & AVFORMAT.AVFMT_GLOBALHEADER) != 0)
			codecContext.flags |= AVFORMAT.CODEC_FLAG_GLOBAL_HEADER;

		return st;
	}
}
