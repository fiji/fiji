package fiji.ffmpeg;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;
import java.io.IOException;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import fiji.ffmpeg.AVCODEC.AVCodec;
import fiji.ffmpeg.AVCODEC.AVCodecContext;
import fiji.ffmpeg.AVCODEC.AVFrame;
import fiji.ffmpeg.AVCODEC.AVPacket;
import fiji.ffmpeg.AVCODEC.AVPicture;
import fiji.ffmpeg.AVFORMAT.AVFormatContext;
import fiji.ffmpeg.AVFORMAT.AVOutputFormat;
import fiji.ffmpeg.AVFORMAT.AVStream;

import fiji.ffmpeg.AVLOG.AvLog;

public class IO extends FFMPEG implements Progress {
	protected AVFormatContext formatContext;
	protected AVCodecContext codecContext;
	protected AVCodec codec;
	protected IntByReference gotPicture = new IntByReference();
	protected int bufferFramePixelFormat = AVUTIL.PIX_FMT_RGB24;
	protected AVFrame frame, bufferFrame;
	protected Pointer swsContext;
	protected byte[] videoOutbut;
	protected Memory videoOutbutMemory;
	protected AVPacket packet = new AVPacket();
	protected Progress progress;

	public interface C extends Library {
		int snprintf(Pointer buffer, Long size, String fmt, Pointer va_list);
	}

	protected C C;

	public IO() throws IOException {
		super();
		if (!loadFFMPEG())
			throw new IOException("Could not load the FFMPEG library!");

		avLog.avSetLogCallback(new AvLog() {
			@Override
			public void callback(String line) {
				IJ.log(line);
			}
		});
	}

	public IO(Progress progress) throws IOException {
		this();
		setProgress(progress);
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
	}

	@Override
	public void start(String message) {
		if (progress != null)
			progress.start(message);
	}

	@Override
	public void step(String message, @SuppressWarnings("hiding") double progress) {
		if (this.progress != null)
			this.progress.step(message, progress);
	}

	@Override
	public void done(String message) {
		if (progress != null)
			progress.done(message);
	}

	@Override
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

		if (avCodec.avcodec_version() != AVCODEC.LIBAVCODEC_VERSION_INT)
			throw new IOException("ffmpeg versions mismatch: native " + avCodec.avcodec_version()
					+ " != Java-bindings " + AVCODEC.LIBAVCODEC_VERSION_INT);

		step("Opening " + path, 0);
		avFormat.av_register_all();

		// Open video file
		final PointerByReference formatContextPointer = new PointerByReference();
		if (avFormat.av_open_input_file(formatContextPointer, path, null, 0, null) != 0)
			throw new IOException("Could not open " + path);
		formatContext = new AVFormatContext(formatContextPointer.getValue());

		// Retrieve stream information
		if (avFormat.av_find_stream_info(formatContext) < 0)
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
		codec = avCodec.avcodec_find_decoder(codecContext.codec_id);
		if (codec == null || avCodec.avcodec_open(codecContext, codec) < 0)
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

				@Override
				public void finalize() {
					free();
				}

				@Override
				public int getSize() {
					int size = (int)(stream.duration / frameDuration);
					if (last >= 0)
						size = Math.min(last, size);
					if (first > 0)
						size -= first;
					return size;
				}

				@Override
				public String getSliceLabel(int slice) {
					return ""; // maybe calculate the time?
				}

				public long guessFrameDuration() {
					if (avFormat.av_read_frame(formatContext, packet) < 0)
						return 1;
					long firstPTS = packet.pts;
					int frameCount = 5;
					for (int i = 0; i < frameCount; previousSlice = i++)
						if (avFormat.av_read_frame(formatContext, packet) < 0)
							return 1;
					return (packet.pts - firstPTS) / frameCount;
				}

				// TODO: cache two
				@Override
				public ImageProcessor getProcessor(int slice) {
					long time = (first + slice - 1) * frameDuration;
					if (time > 0)
						time -=  frameDuration / 2;
					if (stream.start_time != 0x8000000000000000l /* TODO: AVUTIL.AV_NOPTS_VALUE */)
						time += stream.start_time;
					if (previousSlice != slice - 1)
						avFormat.av_seek_frame(formatContext, videoStreamIndex, time,
								AVFORMAT.AVSEEK_FLAG_BACKWARD);
					for (;;) {
						if (avFormat.av_read_frame(formatContext, packet) < 0) {
							packet.data = null;
							packet.size = 0;
							break;
						}
						if (packet.stream_index != videoStreamIndex)
							continue;
						if (previousSlice == slice - 1 || packet.pts >= time)
							break;
						avCodec.avcodec_decode_video2(codecContext, frame, gotPicture, packet);
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
		start("Reading " + path);
		while (avFormat.av_read_frame(formatContext, packet) >= 0 &&
				(last < 0 || frameCounter < last)) {
			// Is this a packet from the video stream?
			if (packet.stream_index != videoStream)
				continue;

			step(null, frameCounter * factor);
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
			frame = avCodec.avcodec_alloc_frame();
			if (frame == null)
				throw new OutOfMemoryError("Could not allocate frame");

			if (forEncoding) {
				// Allocate buffer
				if (avCodec.avpicture_alloc(new AVPicture(frame.getPointer()),
						codecContext.pix_fmt, codecContext.width, codecContext.height) < 0)
					throw new OutOfMemoryError("Could not allocate tmp frame");
				frame.read();
			}
		}

		// Allocate an AVFrame structure
		if (bufferFrame == null) {
			bufferFramePixelFormat = AVUTIL.PIX_FMT_RGB24;
			if (codecContext.pix_fmt == AVUTIL.PIX_FMT_GRAY8 ||
					codecContext.pix_fmt == AVUTIL.PIX_FMT_MONOWHITE ||
					codecContext.pix_fmt == AVUTIL.PIX_FMT_MONOBLACK ||
					codecContext.pix_fmt == AVUTIL.PIX_FMT_PAL8)
				bufferFramePixelFormat = AVUTIL.PIX_FMT_GRAY8;
			else if (codecContext.pix_fmt == AVUTIL.PIX_FMT_GRAY16BE ||
					codecContext.pix_fmt == AVUTIL.PIX_FMT_GRAY16LE)
				bufferFramePixelFormat = AVUTIL.PIX_FMT_GRAY16BE;

			bufferFrame = avCodec.avcodec_alloc_frame();
			if (bufferFrame == null)
				throw new RuntimeException("Could not allocate frame");

			// Allocate buffer
			if (avCodec.avpicture_alloc(new AVPicture(bufferFrame.getPointer()),
					bufferFramePixelFormat, codecContext.width, codecContext.height) < 0)
				throw new OutOfMemoryError("Could not allocate tmp frame");
			bufferFrame.read();
		}

		if (swsContext == null) {
			swsContext = swScale.sws_getContext(codecContext.width, codecContext.height,
					forEncoding ? bufferFramePixelFormat : codecContext.pix_fmt,
					codecContext.width, codecContext.height,
					forEncoding ? codecContext.pix_fmt : bufferFramePixelFormat,
					SWSCALE.SWS_BICUBIC, null, null, null);
			if (swsContext == null)
				throw new OutOfMemoryError("Could not allocate swscale context");
		}
	}

	protected ImageProcessor readOneFrame(@SuppressWarnings("hiding") AVPacket packet) {
		// Decode video frame
		avCodec.avcodec_decode_video2(codecContext, frame, gotPicture, packet);

		// Did we get a video frame?
		if (gotPicture.getValue() == 0)
			return null;

		// Convert the image from its native format to RGB
		convertTo();
		return toSlice(bufferFrame, codecContext.width, codecContext.height);
	}

	protected void convertTo() {
		swScale.sws_scale(swsContext, frame.data, frame.linesize, 0, codecContext.height, bufferFrame.data, bufferFrame.linesize);
	}

	protected void convertFrom() {
		swScale.sws_scale(swsContext, bufferFrame.data, bufferFrame.linesize, 0, codecContext.height, frame.data, frame.linesize);
	}

	protected void free() {
		// Free the RGB image
		if (bufferFrame != null) {
			avUtil.av_free(bufferFrame.getPointer());
			bufferFrame = null;
		}

		// Close the codec
		if (codecContext != null) {
			codecContext = null;
		}

		// Close the video file
		if (formatContext != null) {
			if (formatContext.iformat != null)
				avFormat.av_close_input_file(formatContext);
			formatContext = null;
		}

		if (swsContext != null) {
			swScale.sws_freeContext(swsContext);
			swsContext = null;
		}
	}

	protected ImageProcessor toSlice(@SuppressWarnings("hiding") AVFrame frame, int width, int height) {
		final int len = height * frame.linesize[0];
		final byte[] data = frame.data[0].getByteArray(0, len);
		if (bufferFramePixelFormat == AVUTIL.PIX_FMT_RGB24) {
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
		if (bufferFramePixelFormat == AVUTIL.PIX_FMT_GRAY16BE) {
			short[] pixels = new short[width * height];
			for (int j = 0; j < height; j++) {
				final int off = j * frame.linesize[0];
				for (int i = 0; i < width; i++)
					pixels[i + j * width] = (short)
						(((data[off + 2 * i]) & 0xff) << 8 |
						((data[off + 2 * i + 1]) & 0xff));
			}
			return new ShortProcessor(width, height, pixels, null);
		}
		if (bufferFramePixelFormat == AVUTIL.PIX_FMT_GRAY8 ||
				bufferFramePixelFormat == AVUTIL.PIX_FMT_PAL8) {
			byte[] pixels = new byte[width * height];
			for (int j = 0; j < height; j++) {
				final int off = j * frame.linesize[0];
				for (int i = 0; i < width; i++)
					pixels[i + j * width] = (byte)
						((data[off +  i]) & 0xff);
			}
			/* TODO: in case of PAL8, we should get a colormap */
			return new ByteProcessor(width, height, pixels, null);
		}
		throw new RuntimeException("Unhandled pixel format: " + bufferFramePixelFormat);
	}

	public static int strncpy(byte[] dst, String src) {
		int len = Math.min(src.length(), dst.length - 1);
		System.arraycopy(src.getBytes(), 0, dst, 0, len);
		dst[len] = 0;
		return len;
	}

	public void writeMovie(ImagePlus image, String path, int frameRate, int bitRate) throws IOException {
		final int STREAM_PIX_FMT = AVUTIL.PIX_FMT_YUV420P;

		//int swsFlags = SWScaleLibrary.SWS_BICUBIC;
		AVOutputFormat fmt = null;
		int i;
		ImageStack stack;

		stack = image.getStack();

		start("Writing " + path);
		/* initialize libavcodec, and register all codecs and formats */
		avFormat.av_register_all();

		/* auto detect the output format from the name. default is
		   mpeg. */
		fmt = avFormat.guess_format(null, new File(path).getName(), null);
		if (fmt == null) {
			IJ.log("Could not deduce output format from file extension: using MPEG.");
			fmt = avFormat.guess_format("mpeg2video", null, null);
		}

		if (fmt == null)
			throw new IOException("Could not find suitable output format");

		/* allocate the output media context */
		formatContext = avFormat.av_alloc_format_context();
		if (formatContext == null)
			throw new OutOfMemoryError("Could not allocate format context");
		formatContext.oformat = fmt.getPointer();
		strncpy(formatContext.filename, path);

		/* add the video stream using the default format
		 * codec and initialize the codec */
		if (fmt.video_codec == AVCODEC.CODEC_ID_NONE)
			throw new IOException("Could not determine codec for " + path);
		AVStream videoSt = addVideoStream(fmt.video_codec, stack.getWidth(), stack.getHeight(), frameRate, bitRate, STREAM_PIX_FMT);
		if (videoSt == null)
			throw new IOException("Could not add a video stream");

		/* set the output parameters (mustbe done even if no
		 * parameters). */
		if (avFormat.av_set_parameters(formatContext, null) < 0)
			throw new IOException("Invalid output format parameters.");

		/* now that all the parameters are set, we can open the
		 * video codec and allocate the necessary encode buffer */
		openVideo();

		// Dump the format to stderr
		avFormat.dump_format(formatContext, 0, path, 1);

		AVOutputFormat tmpFmt = new AVOutputFormat(formatContext.oformat);
		if ((tmpFmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 0) {
			/* allocate output buffer */
			/* buffers passed into lav* can be allocated any way you prefer,
			   as long as they're aligned enough for the architecture, and
			   they're freed appropriately (such as using av_free for buffers
			   allocated with av_malloc) */
			videoOutbut = new byte[200000];
		}

		/* open the output file, if needed */
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			final PointerByReference p = new PointerByReference();
			if (avFormat.url_fopen(p, path, AVFORMAT.URL_WRONLY) < 0)
				throw new IOException("Could not open " + path);
			formatContext.pb = p.getValue();
		}

		bufferFramePixelFormat = AVUTIL.PIX_FMT_RGB24;
		switch (image.getType()) {
		case ImagePlus.GRAY8:
			bufferFramePixelFormat = AVUTIL.PIX_FMT_PAL8;
			break;
		case ImagePlus.GRAY16:
			bufferFramePixelFormat = AVUTIL.PIX_FMT_GRAY16BE;
			break;
		}

		allocateFrames(true);

		avFormat.av_write_header(formatContext);

		// videoPts = (double)videoSt.pts.val * videoSt.time_base.num / videoSt.time_base.den;

		for (int frameCount = 1; frameCount <= stack.getSize(); frameCount++) {
			/* write video frame */
			step(null, frameCount / (double)stack.getSize());
			writeVideoFrame(stack.getProcessor(frameCount), videoSt);
		}

		// flush last frame
		//writeVideoFrame(null, formatContext, videoSt);

		/* write the trailer, if any */
		avFormat.av_write_trailer(formatContext);

		/* close codec */
		//closeVideo(formatContext, videoSt);

		/* free the streams */
		for (i = 0; i < formatContext.nb_streams; i++) {
			AVStream tmpStream = new AVStream(formatContext.streams[i]);
			avUtil.av_free(tmpStream.codec);
			avUtil.av_free(formatContext.streams[i]);
		}
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			/* close the output file */
			avFormat.url_fclose(formatContext.pb);
		}

		free();
	}

	protected void writeVideoFrame(ImageProcessor ip, AVStream st) throws IOException {
		int outSize = 0;
		//SwsContext imgConvertCtx = null;

		if (ip == null) {
			/* no more frame to compress. The codec has a latency of a few
			   frames if using B frames, so we get the last frames by
			   passing the same picture again */
		} else {
			if (codecContext.pix_fmt == bufferFramePixelFormat)
				fillImage(frame, ip);
			else {
				fillImage(bufferFrame, ip);
				convertFrom();
			}
		}

		AVOutputFormat tmpFmt = new AVOutputFormat(formatContext.oformat);
		if ((tmpFmt.flags & AVFORMAT.AVFMT_RAWPICTURE) != 0) {
			/* raw video case. The API will change slightly in the near
			   future for that */
			avCodec.av_init_packet(packet);

			packet.flags |= AVCODEC.PKT_FLAG_KEY;
			packet.stream_index = st.index;
			packet.data = frame.getPointer();
			packet.size = frame.size();

			if (avFormat.av_interleaved_write_frame(formatContext, packet) != 0)
				throw new IOException("Error while writing video frame");
		} else {
			/* encode the image */
			if (videoOutbutMemory == null)
				videoOutbutMemory = new Memory(videoOutbut.length);
			// TODO: special-case avcodec_encode_video() to take a Pointer to avoid frequent copying
			outSize = avCodec.avcodec_encode_video(codecContext, videoOutbut, videoOutbut.length, frame);
			/* if zero size, it means the image was buffered */
			if (outSize > 0) {
				avCodec.av_init_packet(packet);

				AVFrame tmpFrame = new AVFrame(codecContext.coded_frame);
				packet.pts = avUtil.av_rescale_q(tmpFrame.pts, new AVUTIL.AVRational.ByValue(codecContext.time_base), new AVUTIL.AVRational.ByValue(st.time_base));
				if (tmpFrame.key_frame == 1)
					packet.flags |= AVCODEC.PKT_FLAG_KEY;
				packet.stream_index = st.index;
				videoOutbutMemory.write(0, videoOutbut, 0, outSize);
				packet.data = videoOutbutMemory;
				packet.size = outSize;

				/* write the compressed frame in the media file */
				if (avFormat.av_interleaved_write_frame(formatContext, packet) != 0)
					throw new IOException("Error while writing video frame");

				st.pts.val = packet.pts; // necessary for calculation of video length
			}
		}
	}

	protected void fillImage(AVFrame pict, final ImageProcessor ip) {
		ImageProcessor ip2 = ip;
		if (bufferFramePixelFormat == AVUTIL.PIX_FMT_RGB24) {
			if (!(ip2 instanceof ColorProcessor))
				ip2 = ip2.convertToRGB();
			int[] pixels = (int[])ip2.getPixels();

			int i = 0, total = ip2.getWidth() * ip2.getHeight();
			for (int j = 0; j < total; j++) {
				int v = pixels[j];
				pict.data[0].setByte(i++, (byte)((v >> 16) & 0xff));
				pict.data[0].setByte(i++, (byte)((v >> 8) & 0xff));
				pict.data[0].setByte(i++, (byte)(v & 0xff));
			}
		}
		else if (bufferFramePixelFormat == AVUTIL.PIX_FMT_GRAY16BE) {
			if (!(ip2 instanceof ShortProcessor))
				ip2 = ip2.convertToShort(false);
			short[] pixels = (short[])ip2.getPixels();

			int i = 0, total = ip2.getWidth() * ip2.getHeight();
			for (int j = 0; j < total; j++) {
				int v = pixels[j] & 0xffff;
				pict.data[0].setByte(i++, (byte)((v >> 8) & 0xff));
				pict.data[0].setByte(i++, (byte)(v & 0xff));
			}
		}
		else if (bufferFramePixelFormat == AVUTIL.PIX_FMT_GRAY8 ||
				bufferFramePixelFormat == AVUTIL.PIX_FMT_PAL8) {
			if (!(ip2 instanceof ByteProcessor))
				ip2 = ip2.convertToByte(false);
			byte[] pixels = (byte[])ip2.getPixels();

			int i = 0, total = ip2.getWidth() * ip2.getHeight();
			for (int j = 0; j < total; j++) {
				int v = pixels[j] & 0xff;
				pict.data[0].setByte(i++, (byte)(v & 0xff));
			}
		}
		else
			throw new RuntimeException("Unhandled pixel format: " + bufferFramePixelFormat);
	}

	protected void openVideo() throws IOException {
		@SuppressWarnings("hiding")
		AVCodec codec;

		/* find the video encoder */
		codec = avCodec.avcodec_find_encoder(codecContext.codec_id);
		if (codec == null)
			throw new IOException("video codec not found for codec id: " + codecContext.codec_id);

		/* open the codec */
		if (avCodec.avcodec_open(codecContext, codec) < 0)
			throw new IOException("Could not open video codec");
	}

	protected static void closeVideo(AVStream st) {
		AVCodecContext tmpCodec = new AVCodecContext(st.codec);
		avCodec.avcodec_close(tmpCodec);
	}

	protected AVStream addVideoStream(int codecId, int width, int height, int frameRate, int bitRate, int pixelFormat) {
		AVStream st;

		st = avFormat.av_new_stream(formatContext, 0);
		if (st == null) {
			IJ.error("Could not alloc video stream.");
			return null;
		}
		codecContext = new AVCodecContext(st.codec);
		codecContext.codec_id = codecId;
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
			codecContext.flags |= AVCODEC.CODEC_FLAG_GLOBAL_HEADER;

		return st;
	}
}
