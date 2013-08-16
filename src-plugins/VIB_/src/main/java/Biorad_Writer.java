import java.io.*;
import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.measure.*;  //for Calibration

// Class to save an image as a Biorad PIC file
// (c) Greg Jefferis <jefferis@gmail.com> 2006-8
// Source code released under the terms of GPLv2 or later
//
// In addition, as a special exception, the copyright holders give
// you permission to combine this program with free software programs or
// libraries that are released under the Apache Public License. 

public class Biorad_Writer implements PlugIn {

	private static final String plugInName = "Biorad Writer";
	private static final String noImages = plugInName+"...\n"+ "No images are open.";
	private static final String supportedTypes =
		plugInName+"..."+				"Supported types:\n" +
				"\n" +
/*				"32-bit Grayscale float : FLOAT\n" +
				"(32-bit Grayscale integer) : LONG\n" +*/
				"16-bit Greyscale integer: INT\n" +
				"(16-bit Greyscale unsigned integer) : UNINT\n"+
				"8-bit Greyscale : BYTE\n"+
				"8-bit Colour LUT (converted to greyscale): BYTE\n";
	
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage(noImages);
			return;
		}
		if (imgType(imp.getFileInfo().fileType) < 0) {
			IJ.showMessage(supportedTypes);
			return;
		}
		String name = arg;
		if (arg == null || arg.equals("")) {
			name = imp.getTitle();
		}
		
		SaveDialog sd = new SaveDialog(plugInName+"...", name, ".PIC");
		String file = sd.getFileName();
		if (file == null) return;
		String directory = sd.getDirectory();
		save(imp, directory, file);
	}

	public void save(ImagePlus imp, String directory, String file) {
		if (imp == null) {
			IJ.showMessage(noImages);
			return;
		}
		FileInfo fi = imp.getFileInfo();

		// Make sure that we can save this kind of image
		int datatype = imgType(fi.fileType);
		// Store this image type for later use
		fi.fileType = datatype;
		if (datatype < 0) {
			IJ.showMessage(supportedTypes);
			return;
		}
		
		// Set the fileName stored in the file info record to the
		// file name that was passed in or chosen in the dialog box
		fi.fileName=file;
		fi.directory=directory;
		
		// ie little endian as used on the Intel platforms which
		// run Biorad's Lasersharp software
		fi.intelByteOrder = true;

		// Put the calibration info into the FileInfo object that
		// will be passed down to the writeImage() function
		Calibration cal=imp.getCalibration();
		fi.pixelWidth=cal.pixelWidth; fi.pixelHeight=cal.pixelHeight;
		fi.pixelDepth=cal.pixelDepth; fi.unit=cal.getUnits();
		
		// This saves problems when loading - the mu seems to flummox
		// the reader (which is set up only to accept ASCII chars
		// also allow um as an alternative form indicating microns
		if(fi.unit.equals("µm") || fi.unit.equals("um")) fi.unit="microns";

		if (IJ.debugMode) IJ.log("FileInfo: "+fi);

		// Actually write out the image
		try {
			writeImage(fi,cal); 
		} catch (IOException e) {
			IJ.error("An error occured writing the file.\n \n" + e);
			IJ.showStatus("");
		}
	}
	void writeImage(FileInfo fi, Calibration cal) throws IOException {
		FileOutputStream out = new FileOutputStream(new File(fi.directory,fi.fileName));
		// First write out the full 76 byte header
		writeHeader(fi,out);
		// Then the image data
		ImageWriter writer = new ImageWriter(fi);
		writer.write(out);
		// Now write out notes - these are only the basic notes
		// write out the footer - ie the calibration information
		writeFooter(fi,out);
		out.close();	
		IJ.showStatus("Saved "+fi.fileName);
	}

	public static int imgType(int fiType) {
		switch (fiType) {
			/*
			case FileInfo.GRAY32_FLOAT:
				return SpeHeader.FLOAT;
			case FileInfo.GRAY32_INT:
				return SpeHeader.LONG;
				*/
			
			case FileInfo.GRAY16_SIGNED:
				return FileInfo.GRAY16_SIGNED;
			
			case FileInfo.GRAY16_UNSIGNED:
				return FileInfo.GRAY16_UNSIGNED;
		
			case FileInfo.GRAY8:
				return FileInfo.GRAY8;

			case FileInfo.COLOR8:
				return FileInfo.GRAY8;
			default:
				return -1;
		}
	}

	void writeDimensions(FileInfo fi,OutputStream out) throws IOException {
		writeShort(out, (short) fi.width );				// 0-1 nx
		writeShort(out, (short) fi.height );			// 2-3 ny 
		writeShort(out, (short) fi.nImages );			// 4-5 npic 
	}
	
	void writeHeader(FileInfo fi,OutputStream out) throws IOException {
		writeDimensions(fi,out);   // write the first 3 fields, nx,ny,npic
		writeShort(out, (short) 0 );				 	// 6-7 ramp1_min 
		writeShort(out, (short) 255 );				 	// 8-9 ramp1_max
		// Changed so that sets notes flag to 1
		writeInt(out, 1);						// 10-13 notes = 1
		// Set to 1 = 8 bit or 0 for anything else             						
		if (fi.fileType==FileInfo.GRAY8) {
			writeShort(out, (short) 1);				// 14-15 byte_format
		} else {
			// The image is 16 bit
			writeShort(out, (short) 0);				// 14-15 byte_format
		}
		
		
		writeShort(out, (short) 0);				// 16-17 Ignored
		
		String tFileName;
		int fileNameLength = fi.fileName.length();
		if (fileNameLength<=31) {
			int terminatorLength = 32-fileNameLength;
			String terminator = "";
			for (int i=0; i<terminatorLength;i++) terminator+="\0";			
			tFileName = new String(fi.fileName+terminator);
		} else {
			// GJ: formerly 32 instead of 31
			tFileName = new String(fi.fileName.substring(0,31)+"\0");
		}
		// Write out the file name as a 32 byte zero terminated string		
		out.write(tFileName.getBytes());  // 18-49 name
		//IJ.write("original file name fi.fileName: "+fi.fileName);
		//IJ.write("truncated file name tFileName: "+tFileName);
		
		writeShort(out, (short) 0);				// 50-51 merged status
		//  not sure what this colour1 means
		writeShort(out, (short) 7);				// 52-53 colour1		                        
		writeShort(out, (short) 12345);			// 54-55 file_id
		writeShort(out, (short) 0 );				 	// 56-57 ramp2_min 
		writeShort(out, (short) 255 );				 	// 58-59 ramp2_max
		//  not sure what this colour2 means
		writeShort(out, (short) 7);				// 60-61 colour2
		writeShort(out, (short) 0);				// 62-63 Ignored
		writeShort(out, (short) 0);				// 64-65 int_lens
		writeFloat(out, (float) 0);				// 66-69 mag_factor
		writeShort(out, (short) 0);				// 70-71 Ignored
		writeShort(out, (short) 0);				// 72-73 Ignored
		writeShort(out, (short) 0);				// 74-75 Ignored		
	}
	
	void writeFooter(FileInfo fi,OutputStream out) throws IOException { 
		out.write(formNote("AXIS_2 001 0.000000e+00 "+IJ.d2s(fi.pixelWidth,8)+" "+fi.unit,1,20));
		out.write(formNote("AXIS_3 001 0.000000e+00 "+IJ.d2s(fi.pixelHeight,8)+" "+fi.unit,1,20));
		if(fi.nImages>1) {
			// this is a stack so write out Z calibration
			out.write(formNote("AXIS_4 001 0.000000e+00 "+IJ.d2s(fi.pixelDepth,8)+" "+fi.unit,1,20));
			out.write(formNote("AXIS_9 001 0.000000e+00 1.000000e+00 RGB channel",0,20));
		} else {
			// this is a single slice image
			out.write(formNote("AXIS_4 011 0.000000e+00 1.000000e+00 RGB channel",0,20));
		}
		
	}
	byte[] formNote(String noteText,int noteFlag,int noteType) throws IOException {
		ByteArrayOutputStream line = new ByteArrayOutputStream();
		// the Display level of the note
		writeShort(line,(short) 0xFFFF);
		// whether this is the last note or not
		writeInt(line,noteFlag);
		// Skip 4 bytes
		writeInt(line,0);
		// The note type
		writeShort(line,(short)noteType);
		// Skip 4 bytes
		writeInt(line,0);
		// PAD the note text to 80 chars wide
		if(noteText.length()<80) {
			String eightyBlanks="                                                                                ";
			noteText+=eightyBlanks.substring(noteText.length());
		}
		//IJ.log("noteText='"+noteText+"'");
		
		// Get the note text 1 byte for each char
		line.write(noteText.getBytes("ISO-8859-1"));
		return(line.toByteArray());
	}
	
	void writeShort(OutputStream out, short value) throws IOException
	{
		byte b1 = (byte) (value & 0xff);
		byte b2 = (byte) ((value >> 8) & 0xff);
		out.write(b1);
		out.write(b2);
	}
	
	void writeInt(OutputStream out, int value) throws IOException 
	{
		byte b1 = (byte) (value & 0xff);
		byte b2 = (byte) ((value >> 8) & 0xff);
		byte b3 = (byte) ((value >> 16) & 0xff);
		byte b4 = (byte) ((value >> 24) & 0xff); 
		out.write(b1);
		out.write(b2);
		out.write(b3);
		out.write(b4);
	}
	
	void writeFloat(OutputStream out, float value) throws IOException
	{
		// convert to int and then use writeInt
		writeInt(out,Float.floatToIntBits( value ));
	}
}
