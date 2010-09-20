package fiji.ffmpeg;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

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

public class IO extends FFMPEGSingle {
	public IO() throws IOException {
		super();
		if (!loadFFMPEG())
			throw new IOException("Could not load the FFMPEG library!");
	}

	/**
	 * Based on the AVCodecSample example from ffmpeg-java by Ken Larson.
	 */
	public ImagePlus readMovie(String path) throws IOException {
		/* Need to do this because we already extend ImagePlus */
		if (!loadFFMPEG())
			throw new IOException("Could not load the FFMPEG library!");

		if (AVCODEC.avcodec_version() != AVCODEC.LIBAVCODEC_VERSION_INT)
			throw new IOException("ffmpeg versions mismatch: native " + AVCODEC.avcodec_version()
					+ " != Java-bindings " + AVCODEC.LIBAVCODEC_VERSION_INT);

		AVFORMAT.av_register_all();

		// Open video file
		final PointerByReference formatContextPointer = new PointerByReference();
		if (AVFORMAT.av_open_input_file(formatContextPointer, path, null, 0, null) != 0)
			throw new IOException("Could not open " + path);
		final AVFormatContext formatContext = new AVFormatContext(formatContextPointer.getValue());

		// Retrieve stream information
		if (AVFORMAT.av_find_stream_info(formatContext) < 0)
			throw new IOException("No stream in " + path);

		// Find the first video stream
		int videoStream = -1;
		AVCodecContext codecContext = null;
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
		final AVCodec codec = AVCODEC.avcodec_find_decoder(codecContext.codec_id);
		if (codec == null || AVCODEC.avcodec_open(codecContext, codec) < 0)
			throw new IOException("Codec not available");

		// Allocate video frame
		final AVFrame frame = AVCODEC.avcodec_alloc_frame();
		if (frame == null)
			throw new OutOfMemoryError("Could not allocate frame");

		// Allocate an AVFrame structure
		final AVFrame frameRGB = AVCODEC.avcodec_alloc_frame();
		if (frameRGB == null)
			throw new RuntimeException("Could not allocate frame");

		// Allocate buffer
		if (AVCODEC.avpicture_alloc(new AVPicture(frameRGB.getPointer()),
				AVUTIL.PIX_FMT_RGB24, codecContext.width, codecContext.height) < 0)
			throw new OutOfMemoryError("Could not allocate tmp frame");
		frameRGB.read();

		ImageStack stack = new ImageStack(codecContext.width, codecContext.height);

		// Read frames and save first five frames to disk
		AVPacket packet = new AVPacket();
		IntByReference gotPicture = new IntByReference();
		Pointer swsContext = SWSCALE.sws_getContext(codecContext.width, codecContext.height, codecContext.pix_fmt,
				codecContext.width, codecContext.height, AVUTIL.PIX_FMT_RGB24,
				SWSCALE.SWS_BICUBIC, null, null, null);
		while (AVFORMAT.av_read_frame(formatContext, packet) >= 0) {
			// Is this a packet from the video stream?
			if (packet.stream_index != videoStream)
				continue;

			// Decode video frame
			AVCODEC.avcodec_decode_video2(codecContext, frame, gotPicture, packet);

			// Did we get a video frame?
			if (gotPicture.getValue() == 0)
				continue;

			// Convert the image from its native format to RGB
			SWSCALE.sws_scale(swsContext, frame.data, frame.linesize, 0, codecContext.height, frameRGB.data, frameRGB.linesize);
if (stack.getSize() >= 100) { IJ.error("TODO: make virtual stack!"); break; }
			ImageProcessor ip = toSlice(frameRGB, codecContext.width, codecContext.height);
			stack.addSlice(null, ip);

			// Free the packet that was allocated by av_read_frame
			// AVFORMAT.av_free_packet(packet.getPointer())
			// - cannot be called because it is an inlined function.
			// so we'll just do the JNA equivalent of the inline:
			if (packet.destruct != null)
				packet.destruct.callback(packet);

		}

		// TODO: refactor using the temporary frame
		// TODO: read the last frame, too

		// Free the RGB image
		AVUTIL.av_free(frameRGB.getPointer());

		// Close the codec
		AVCODEC.avcodec_close(codecContext);

		// Close the video file
		AVFORMAT.av_close_input_file(formatContext);

		return new ImagePlus(path, stack);
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

	public static void writeMovie(ImagePlus image, String path, int frameRate) throws IOException {
		final int STREAM_PIX_FMT = AVUTIL.PIX_FMT_YUV420P;

		//int sws_flags = SWScaleLibrary.SWS_BICUBIC;
		AVOutputFormat fmt = null;
		AVFormatContext formatContext = null;
		double video_pts;
		int i;
		ImageStack stack;

		stack = image.getStack();

		/* initialize libavcodec, and register all codecs and formats */
		AVFORMAT.av_register_all();

		/* auto detect the output format from the name. default is
		   mpeg. */
		fmt = AVFORMAT.guess_format(null, new File(path).getName(), null);
		if (fmt == null) {
			IJ.log("Could not deduce output format from file"
					+ " extension: using MPEG.");
			fmt = AVFORMAT.guess_format("mpeg2video", null, null);
		}

		if (fmt == null)
			throw new IOException("Could not find suitable output format");

		/* allocate the output media context */
		formatContext = AVFORMAT.av_alloc_format_context();
		if (formatContext == null)
			throw new IOException("Memory error");
		formatContext.oformat = fmt.getPointer();
		byte[] filename = path.getBytes();
		System.arraycopy(filename, 0, formatContext.filename, 0, filename.length);
		filename[filename.length] = 0;

		/* add the video stream using the default format
		 * codec and initialize the codec */
		AVStream video_st = null;
		if (fmt.video_codec != AVCODEC.CODEC_ID_NONE) {
			video_st = add_video_stream(formatContext, fmt.video_codec, stack.getWidth(), stack.getHeight(), frameRate, STREAM_PIX_FMT);
			if (video_st == null)
				throw new IOException("Could not add a video stream");
		}

		AVCodecContext c = new AVCodecContext(video_st.codec);

		/* allocate the encoded raw picture */
		AVFrame picture = alloc_picture(c.pix_fmt, c.width, c.height);
		if (picture == null)
			throw new OutOfMemoryError("could not allocate picture");

		/* if the output format is not RGB24, then a temporary RGB24
		   picture is needed too. It is then converted to the required
		   output format */
		AVFrame tmp_picture = null;
		if (c.pix_fmt != AVUTIL.PIX_FMT_RGB24) {
			tmp_picture = alloc_picture(AVUTIL.PIX_FMT_RGB24, c.width, c.height);
			if (tmp_picture == null)
				throw new OutOfMemoryError("could not allocate temporary picture");
		}

		/* set the output parameters (mustbe done even if no
		 * parameters). */
		if (AVFORMAT.av_set_parameters(formatContext, null) < 0)
			throw new IOException("Invalid output format parameters.");

		/* now that all the parameters are set, we can open the
		 * video codec and allocate the necessary encode buffer */
		if (video_st != null && !open_video(formatContext, video_st))
			throw new IOException("Could not open video");

		int video_outbuf_size = 0;
		Pointer video_outbuf = null;
		AVOutputFormat tmp_fmt = new AVOutputFormat(formatContext.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 0) {
			/* allocate output buffer */
			/* buffers passed into lav* can be allocated any way you prefer,
			   as long as they're aligned enough for the architecture, and
			   they're freed appropriately (such as using av_free for buffers
			   allocated with av_malloc) */
			video_outbuf_size = 200000;
			video_outbuf = AVUTIL.av_malloc(video_outbuf_size);
		}

		/* open the output file, if needed */
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			final PointerByReference p = new PointerByReference();
			if (AVFORMAT.url_fopen(p, path, AVFORMAT.URL_WRONLY) < 0)
				throw new IOException("Could not open " + path);
			formatContext.pb = p.getValue();
		}

		AVFORMAT.av_write_header(formatContext);

		if (video_st != null)
			video_pts = (double)video_st.pts.val * video_st.time_base.num / video_st.time_base.den;
		else
			video_pts = 0.0;

		// TODO: clean up all resources when throwing exceptions
		for (int frameCount = 0; frameCount <= stack.getSize(); frameCount++) {
			/* write video frame */
			write_video_frame(frameCount < stack.getSize() ? stack.getProcessor(frameCount + 1) : null, picture, formatContext, video_st);
		}

		/* close codec */
		close_video(formatContext, video_st);

		/* write the trailer, if any */
		AVFORMAT.av_write_trailer(formatContext);

		/* free the streams */
		// TODO: free all streams
		for (i = 0; i < formatContext.nb_streams; i++) {
			AVStream tmp_stream = new AVStream(formatContext.streams[0]);
			AVUTIL.av_free(tmp_stream.codec);
			AVUTIL.av_free(formatContext.streams[0]);
		}

		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			/* close the output file */
			//AVFORMAT.url_fclose(new ByteIOContext(formatContext.pb));
		}

		/* free the stream */
		AVUTIL.av_free(formatContext.getPointer());

		AVUTIL.av_free(picture.data[0]);
		AVUTIL.av_free(picture.getPointer());
		if (tmp_picture != null) {
			AVUTIL.av_free(tmp_picture.data[0]);
			AVUTIL.av_free(tmp_picture.getPointer());
		}
	}

	protected static void write_video_frame(ImageProcessor ip, AVFrame picture, AVFormatContext formatContext, AVStream st) throws IOException {
		int out_size = 0;
		AVCodecContext c = new AVCodecContext(st.codec);
		//SwsContext img_convert_ctx = null;

		if (ip == null) {
			/* no more frame to compress. The codec has a latency of a few
			   frames if using B frames, so we get the last frames by
			   passing the same picture again */
		} else {
			if (c.pix_fmt == AVUTIL.PIX_FMT_RGB24)
				fill_image(picture, ip);
			else {
				/*
				 * As we only generate a RGB24 picture, we
				 * must convert it to the codec pixel format
				 * if needed.
				 */
				/*
				   fill_image(tmp_picture, frameCount,
				   c.width, c.height);
				   AVCODEC.img_convert(picture, c.pix_fmt,
				   tmp_picture,
				   AVUTIL.PIX_FMT_RGB24,
				   c.width, c.height);
				 */
			}
		}

		AVOutputFormat tmp_fmt = new AVOutputFormat(formatContext.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 1) {
			/* raw video case. The API will change slightly in the near
			   futur for that */
			AVPacket pkt = new AVPacket();
			AVCODEC.av_init_packet(pkt);

			pkt.flags |= AVCODEC.PKT_FLAG_KEY;
			pkt.stream_index = st.index;
			//pkt.data = picture.getPointer();
			//pkt.size = picture.size();
if (true) throw new RuntimeException("TODO");

			if (AVFORMAT.av_write_frame(formatContext, pkt) != 0)
				throw new IOException("Error while writing video frame");
		} else {
			/* encode the image */
			//out_size = AVCODEC.avcodec_encode_video(c, video_outbuf, video_outbuf_size, picture);
			/* if zero size, it means the image was buffered */
			if (out_size > 0) {
				AVPacket pkt = new AVPacket();
				AVCODEC.av_init_packet(pkt);

				AVFrame tmp_frame = new AVFrame(c.coded_frame);
				//pkt.pts = AVUTILWorkarounds.av_rescale_q(tmp_frame.pts, c.time_base, st.time_base);
				//System.out.println("tmp_frame.pts=" + tmp_frame.pts + " c.time_base=" + c.time_base.num + "/" + c.time_base.den + " st.time_base=" + st.time_base.num + "/" + st.time_base.den + " pkt.pts=" + pkt.pts);
				if (tmp_frame.key_frame == 1)
					pkt.flags |= AVCODEC.PKT_FLAG_KEY;
				pkt.stream_index = st.index;
				//pkt.data = video_outbuf;
				pkt.size = out_size;

				/* write the compressed frame in the media file */
				if (AVFORMAT.av_write_frame(formatContext, pkt) != 0)
					throw new IOException("Error while writing video frame");

				st.pts.val = pkt.pts; // necessary for calculation of video length
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

	protected static boolean open_video(AVFormatContext formatContext, AVStream st) {
		AVCodec codec;
		AVCodecContext c = new AVCodecContext(st.codec);

		/* find the video encoder */
		codec = AVCODEC.avcodec_find_encoder(c.codec_id);
		if (codec == null) {
			IJ.error("video codec not found for codec id: " + c.codec_id);
			return false;
		}

		/* open the codec */
		if (AVCODEC.avcodec_open(c, codec) < 0) {
			IJ.error("could not open video codec");
			return false;
		}

		return true;
	}

	protected static AVFrame alloc_picture(int pix_fmt, int width, int height) {
		AVFrame picture;
		Pointer picture_buf;
		int size;

		picture = AVCODEC.avcodec_alloc_frame();
		if (picture == null) {
			return null;
		}
		size = AVCODEC.avpicture_get_size(pix_fmt, width, height);
		picture_buf = AVUTIL.av_malloc(size);
		if (picture_buf == null) {
			AVUTIL.av_free(picture.getPointer());
			return null;
		}
		//AVCODEC.avpicture_fill(picture, picture_buf, pix_fmt, width, height);
		return picture;
	}

	protected static void close_video(AVFormatContext formatContext, AVStream st) {
		AVCodecContext tmp_codec = new AVCodecContext(st.codec);
		AVCODEC.avcodec_close(tmp_codec);
	}

	protected static AVStream add_video_stream(AVFormatContext formatContext, int codec_id, int width, int height, int frameRate, int pixelFormat) {
		AVStream st;

		st = AVFORMAT.av_new_stream(formatContext, 0);
		if (st == null) {
			IJ.error("Could not alloc video stream.");
			return null;
		}
		AVCodecContext c = new AVCodecContext(st.codec);
		c.codec_id = codec_id;
		c.codec_type = AVCODEC.CODEC_TYPE_VIDEO;

		/* put sample parameters */
		c.bit_rate = 400000;
		/* resolution must be a multiple of two */
		c.width = width; //352;
		c.height = height; //288;
		/* time base: this is the fundamental unit of time (in seconds) in terms
		   of which frame timestamps are represented. for fixed-fps content,
		   timebase should be 1/framerate and timestamp increments should be
		   identically 1. */
		c.time_base.den = frameRate;
		c.time_base.num = 1;
		c.gop_size = 12;
		c.pix_fmt = pixelFormat;

		if (c.codec_id == AVCODEC.CODEC_ID_MPEG2VIDEO) {
			/* just for testing, we also add B frames */
			c.max_b_frames = 2;
		}
		if (c.codec_id == AVCODEC.CODEC_ID_MPEG1VIDEO) {
			/* Needed to avoid using macroblocks in which some coeffs overflow.
			   This does not happen with normal video, it just happens here as
			   the motion of the chroma plane does not match the luma plane. */
			c.mb_decision = 2;
		}
		// some formats want stream headers to be separate
		AVOutputFormat tmp_fmt = new AVOutputFormat(formatContext.oformat);
		if (tmp_fmt.name.equals("mp4") || tmp_fmt.name.equals("mov") || tmp_fmt.name.equals("3gp")) {
			c.flags |= AVCODEC.CODEC_FLAG_GLOBAL_HEADER;
		}

		c.write(); // very very important!!!

		return st;
	}
}
