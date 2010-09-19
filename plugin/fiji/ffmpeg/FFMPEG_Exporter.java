package ffmpeg;

/*
 * Simple movie writer for ImageJ using ffmpeg-java; based on Libavformat API
 * example from FFMPEG
 *
 * Based on the example "output_example.c" provided with FFMPEG.  LGPL version
 * (no SWSCALE) by Uwe Mannl.
 */

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.io.SaveDialog;

import ij.plugin.PlugIn;

import fiji.ffmpeg.AVUTIL;

import fiji.ffmpeg.AVCODEC;
import fiji.ffmpeg.AVCODEC.AVCodec;
import fiji.ffmpeg.AVCODEC.AVCodecContext;
import fiji.ffmpeg.AVCODEC.AVFrame;
import fiji.ffmpeg.AVCODEC.AVPacket;

import fiji.ffmpeg.AVFORMAT;
import fiji.ffmpeg.AVFORMAT.AVFormatContext;
import fiji.ffmpeg.AVFORMAT.AVOutputFormat;
import fiji.ffmpeg.AVFORMAT.AVStream;
//import fiji.ffmpeg.AVFORMAT.ByteIOContext;

public class FFMPEG_Exporter extends FFMPEG implements PlugIn {

	double frame_rate;
	final int STREAM_PIX_FMT = AVFORMAT.PIX_FMT_YUV420P;

	AVFrame picture, tmp_picture;
	int frame_count, video_outbuf_size;
	//int sws_flags = SWScaleLibrary.SWS_BICUBIC;
	float t, tincr, tincr2;

	Pointer video_outbuf;
	Pointer samples;

	AVOutputFormat fmt = null;
	AVFormatContext oc = null;
	AVStream video_st;
	double video_pts;
	int i;
	ImageStack stack;

	public void run(String arg) {

		if (!loadFFMPEG()) {
			IJ.error("This plugin needs ffmpeg to be installed!");
			return;
		}

		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.error("No image is open");
			return;
		}

		if (image.getType() != ImagePlus.COLOR_RGB) {
			IJ.error("Need a color image");
			return;
		}

		GenericDialog gd = new GenericDialog("FFMPEG Exporter");
		gd.addNumericField("Framerate: ", 25, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		frame_rate = gd.getNextNumber();

		String name = IJ.getImage().getTitle();
		SaveDialog sd = new SaveDialog("Export via FFMPEG",
				name, ".mpg");
		name = sd.getFileName();
		String directory = sd.getDirectory();
		String path = directory+name;


		stack = image.getStack();

		/* initialize libavcodec, and register all codecs and formats */
		AVFORMAT.av_register_all();

		/* auto detect the output format from the name. default is
		   mpeg. */
		fmt = AVFORMAT.guess_format(null, name, null);
		if (fmt == null) {
			IJ.log("Could not deduce output format from file"
					+ " extension: using MPEG.");
			fmt = AVFORMAT.guess_format("mpeg2video", null, null);
		}

		if (fmt == null) {
			IJ.error("Could not find suitable output format");
			return;
		}

		/* allocate the output media context */
		oc = AVFORMAT.av_alloc_format_context();
		if (oc == null) {
			IJ.error("Memory error");
			return;
		}
		oc.oformat = fmt.getPointer();
		oc.filename = path.getBytes();

		/* add the video stream using the default format
		 * codec and initialize the codec */
		video_st = null;
		if (fmt.video_codec != AVCODEC.CODEC_ID_NONE) {
			video_st = add_video_stream(oc, fmt.video_codec);
			if (video_st == null)
				return;
		}

		/* set the output parameters (mustbe done even if no
		 * parameters). */
		if (AVFORMAT.av_set_parameters(oc, null) < 0) {
			IJ.error("Invalid output format parameters.");
			return;
		}

		/* now that all the parameters are set, we can open the
		 * video codec and allocate the necessary encode buffer */
		if (video_st != null && !open_video(oc, video_st))
			return;

		/* open the output file, if needed */
		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			final PointerByReference p = new PointerByReference();
			if (AVFORMAT.url_fopen(p, path,
						AVFORMAT.URL_WRONLY) < 0) {
				IJ.error("Could not open " + path);
				return;
			}
			oc.pb = p.getValue();
		}

		AVFORMAT.av_write_header(oc);

		while (frame_count < stack.getSize()) {
			if (video_st != null)
				video_pts = (double)video_st.pts.val *
					video_st.time_base.num /
					video_st.time_base.den;
			else
				video_pts = 0.0;

			/* write video frame */
			if (!write_video_frame(oc, video_st))
				break;
		}

		/* close each codec */
		if (video_st != null)
			close_video(oc, video_st);

		/* write the trailer, if any */
		AVFORMAT.av_write_trailer(oc);

		/* free the streams */
		// TODO: free all streams
		for (i = 0; i < oc.nb_streams; i++) {
			AVStream tmp_stream = new AVStream(oc.streams0);
			AVUTIL.av_free(tmp_stream.codec);
			AVUTIL.av_free(oc.streams0);
		}

		if ((fmt.flags & AVFORMAT.AVFMT_NOFILE) == 0) {
			/* close the output file */
			AVFORMAT.url_fclose(new ByteIOContext(oc.pb));
		}

		/* free the stream */
		AVUTIL.av_free(oc.getPointer());
	}

	private boolean write_video_frame(AVFormatContext oc, AVStream st) {
		int out_size, ret;
		AVCodecContext c = new AVCodecContext(st.codec);
		//SwsContext img_convert_ctx = null;

		if (frame_count >= stack.getSize()) {
			/* no more frame to compress. The codec has a latency of a few
			   frames if using B frames, so we get the last frames by
			   passing the same picture again */
		} else {
			if (c.pix_fmt == AVCODEC.PIX_FMT_RGB24)
				fill_image(picture, frame_count,
						c.width, c.height);
			else {
				/*
				 * As we only generate a RGB24 picture, we
				 * must convert it to the codec pixel format
				 * if needed.
				 */
				fill_image(tmp_picture, frame_count,
						c.width, c.height);
				AVCODEC.img_convert(picture, c.pix_fmt,
						tmp_picture,
						AVCODEC.PIX_FMT_RGB24,
						c.width, c.height);
			}
		}

		AVOutputFormat tmp_fmt = new AVOutputFormat(oc.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 1) {
			/* raw video case. The API will change slightly in the near
			   futur for that */
			AVPacket pkt = new AVPacket();
			AVFORMAT.av_init_packet(pkt);

			pkt.flags |= AVFORMAT.PKT_FLAG_KEY;
			pkt.stream_index = st.index;
			pkt.data = picture.getPointer();
			pkt.size = picture.size();

			ret = AVFORMAT.av_write_frame(oc, pkt);
		} else {
			/* encode the image */
			out_size = AVCODEC.avcodec_encode_video(c, video_outbuf, video_outbuf_size, picture);
			/* if zero size, it means the image was buffered */
			if (out_size > 0) {
				AVPacket pkt = new AVPacket();
				AVFORMAT.av_init_packet(pkt);

				AVFrame tmp_frame = new AVFrame(c.coded_frame);
				pkt.pts = AVUTILWorkarounds.av_rescale_q(tmp_frame.pts, c.time_base, st.time_base);
				//System.out.println("tmp_frame.pts=" + tmp_frame.pts + " c.time_base=" + c.time_base.num + "/" + c.time_base.den + " st.time_base=" + st.time_base.num + "/" + st.time_base.den + " pkt.pts=" + pkt.pts);
				if (tmp_frame.key_frame == 1)
					pkt.flags |= AVFORMAT.PKT_FLAG_KEY;
				pkt.stream_index = st.index;
				pkt.data = video_outbuf;
				pkt.size = out_size;

				/* write the compressed frame in the media file */
				ret = AVFORMAT.av_write_frame(oc, pkt);

				st.pts.val = pkt.pts; // necessary for calculation of video length
			} else {
				ret = 0;
			}
		}
		if (ret != 0) {
			IJ.error("Error while writing video frame");
			return false;
		}
		frame_count++;

		return true;
	}

	private void fill_image(AVFrame pict, int frame_index,
			int width, int height) {
		int[] pixels =
			(int[])stack.getProcessor(frame_index + 1).getPixels();

		int i = 0;
		for (int j = 0; j < width * height; j++) {
			int v = pixels[j];
			pict.data0.setByte(i++, (byte)((v >> 16) & 0xff));
			pict.data0.setByte(i++, (byte)((v >> 8) & 0xff));
			pict.data0.setByte(i++, (byte)(v & 0xff));
		}
	}

	private boolean open_video(AVFormatContext oc, AVStream st) {
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

		video_outbuf = null;
		AVOutputFormat tmp_fmt = new AVOutputFormat(oc.oformat);
		if ((tmp_fmt.flags & AVFORMAT.AVFMT_RAWPICTURE) == 0) {
			/* allocate output buffer */
			/* buffers passed into lav* can be allocated any way you prefer,
			   as long as they're aligned enough for the architecture, and
			   they're freed appropriately (such as using av_free for buffers
			   allocated with av_malloc) */
			video_outbuf_size = 200000;
			video_outbuf = AVUTIL.av_malloc(video_outbuf_size);
		}

		/* allocate the encoded raw picture */
		picture = alloc_picture(c.pix_fmt, c.width, c.height);
		if (picture == null) {
			IJ.error("could not allocate picture");
			return false;
		}

		/* if the output format is not RGB24, then a temporary RGB24
		   picture is needed too. It is then converted to the required
		   output format */
		tmp_picture = null;
		if (c.pix_fmt != AVFORMAT.PIX_FMT_RGB24) {
			tmp_picture = alloc_picture(AVFORMAT.PIX_FMT_RGB24, c.width, c.height);
			if (tmp_picture == null) {
				IJ.error("could not allocate temporary picture");
				return false;
			}
		}

		return true;
	}

	private AVFrame alloc_picture(int pix_fmt, int width, int height) {
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
		AVCODEC.avpicture_fill(picture, picture_buf, pix_fmt, width, height);
		return picture;
	}

	private void close_video(AVFormatContext oc, AVStream st) {
		AVCodecContext tmp_codec = new AVCodecContext(st.codec);
		AVCODEC.avcodec_close(tmp_codec);
		AVUTIL.av_free(picture.data0);
		AVUTIL.av_free(picture.getPointer());
		if (tmp_picture != null) {
			AVUTIL.av_free(tmp_picture.data0);
			AVUTIL.av_free(tmp_picture.getPointer());
		}
		AVUTIL.av_free(video_outbuf);
	}

	private AVStream add_video_stream(AVFormatContext oc, int codec_id) {
		AVStream st;

		st = AVFORMAT.av_new_stream(oc, 0);
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
		c.width = stack.getWidth(); //352;
		c.height = stack.getHeight(); //288;
		/* time base: this is the fundamental unit of time (in seconds) in terms
		   of which frame timestamps are represented. for fixed-fps content,
		   timebase should be 1/framerate and timestamp increments should be
		   identically 1. */
		c.time_base.den = (int)frame_rate;
		c.time_base.num = 1;
		c.gop_size = 12;
		c.pix_fmt = STREAM_PIX_FMT;

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
		AVOutputFormat tmp_fmt = new AVOutputFormat(oc.oformat);
		if (tmp_fmt.name.equals("mp4") || tmp_fmt.name.equals("mov") || tmp_fmt.name.equals("3gp")) {
			c.flags |= AVCODEC.CODEC_FLAG_GLOBAL_HEADER;
		}

		c.write(); // very very important!!!

		return st;
	}

}
