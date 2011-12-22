package amira;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.*;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

public class AmiraMeshEncoder {
	private int width,height,numSlices;
	private int mode;
	final public int RAW = 0;
	final public int RLE = 1;
	final public int ZLIB = 2;
	private Hashtable parameters;
	private Vector materials;

	private String path;
	private RandomAccessFile file;
	private long offsetOfStreamLength;
	private String line;
	private byte[] rleOverrun;
	private int rleOverrunLength;
	private ZOutputStream zStream;
	private int zLength;

	public AmiraMeshEncoder(String path_) {
		path = path_;
		width = height = numSlices = -1;
		offsetOfStreamLength = 0;
		rleOverrunLength = 0;
		mode = ZLIB;
	}

	public boolean open() {
		try {
			file=new RandomAccessFile(path,"rw");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	public boolean writeHeader(ImagePlus ip) {
		try {
			AmiraParameters parameters=new AmiraParameters(ip);
			if (parameters.isAmiraLabelfield(ip))
				mode = RLE;
			Date date=new Date();
			file.writeBytes("# AmiraMesh 3D BINARY 2.0\n"
				+"# CreationDate: "+date.toString()+"\n"
				+"\n"
				+"define Lattice "+width+" "+height+" "+numSlices+"\n"
				+"\n"
				+"Parameters {\n"
				+parameters.toString()
				+"}\n"
				+"\n"
				+"Lattice { byte "
				+ ( mode == RLE ? "Labels" : "Data")
				+ " } @1");
			if (mode == RLE) {
				file.writeBytes("(HxByteRLE,");
				offsetOfStreamLength=file.getFilePointer();
				file.writeBytes("          ");
			} else if (mode == ZLIB) {
				file.writeBytes("(HxZip,");
				offsetOfStreamLength=file.getFilePointer();
				file.writeBytes("          ");
			}
			file.writeBytes("\n"
				+"\n"
				+"# Data section follows\n"
				+"@1\n");
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return true;
	}

	// TODO: adjust Colors of Materials

	public boolean write(ImagePlus ip) {
		IJ.showStatus("Writing "+path+" (AmiraMesh) ...");

		width=ip.getWidth();
		height=ip.getHeight();
		numSlices=ip.getStackSize();

		if(!writeHeader(ip))
			return false;

		try {
			long offsetOfData=file.getFilePointer();

			ImageStack is=ip.getStack();
			for(int k=1;k<=numSlices;k++) {
				ByteProcessor ipro=(ByteProcessor)is.getProcessor(k);
				byte[] pixels=(byte[])ipro.getPixels();
				if (mode == RLE)
					writeRLE(pixels);
				else if (mode == ZLIB)
					writeZlib(pixels);
				else
					file.write(pixels);
				IJ.showProgress(k, numSlices);
			}

			if (mode == ZLIB)
				zStream.finish();

			// fix file size
			long eof=file.getFilePointer();
			file.setLength(eof);

			// fix up stream length
			if (mode == RLE || mode == ZLIB) {
				long length = eof - offsetOfData;
				file.seek(offsetOfStreamLength);
				file.writeBytes("" + length + ")\n");
				file.seek(eof);
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}

		IJ.showStatus("");

		return true;
	}

	public void writeRLE(byte[] pixels) throws IOException {
		for(int i=0;i<pixels.length;) {
			if(i+1>=pixels.length) {
				file.writeByte(1);
				file.writeByte(pixels[i]);
				i++;
			} else if(pixels[i]==pixels[i+1]) {
				int j;
				for(j=2;j<127 && j+i+1<pixels.length && pixels[i]==pixels[i+j];j++);
				file.writeByte(j);
				file.writeByte(pixels[i]);
				i+=j;
			} else {
				int j;
				for(j=1;j<127 && j+i+1<pixels.length && pixels[i+j]!=pixels[i+j+1];j++);
				file.writeByte(j|0x80);
				file.write(pixels,i,j);
				i+=j;
			}
		}
	}

	public void writeZlib(byte[] pixels) throws IOException {
		if (zStream == null)
			zStream = new ZOutputStream(new BufferedOutputStream(new FileOutputStream(file.getFD())), JZlib.Z_BEST_COMPRESSION);
		zStream.write(pixels, 0, pixels.length);
	}
}

