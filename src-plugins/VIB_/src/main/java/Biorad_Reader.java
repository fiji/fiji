import ij.*;
import ij.io.*;
import ij.util.Tools;
import ij.plugin.*;
import java.io.*;
import java.util.zip.GZIPInputStream;

import ij.measure.*;
import java.awt.image.*;

/** Imports a Z series(image stack) from a Biorad MRC 600
    confocal microscope.  The width, height and number of images are
    extracted from the first 3 16-bit word in the 76 byte header.
    Use Image/Show Info to display the header information.

    See statements flagged // ghj 4/3/06
    for modifications by "Greg Joss" <gjoss@bio.mq.edu.au>
    to open 16-bit little-endian (Intel) Biorad files from "Winnok De Vos (ir.)" <winnok.devos@ugent.be>
    some if (IJ.debugMode)IJ.log(statements were also added.
*/

public class Biorad_Reader extends ImagePlus implements PlugIn {

    private final int NOTE_SIZE = 96;
    private BufferedInputStream f;
    private String directory;
    private String fileName;
    private String notes = "";	
    private int lutOffset = -1;

    public void run(String arg) {
	OpenDialog od = new OpenDialog("Open Biorad...", arg);
	directory = od.getDirectory();
	fileName = od.getFileName();
	if (fileName==null)
	    return;
	IJ.showStatus("Opening: " + directory + fileName);
	FileInfo fi = null;
	try {fi = getHeaderInfo();}
	catch (Exception e) {
	    IJ.showStatus("");
	    IJ.showMessage("BioradReader", ""+e);
	    return;
	}
	if (fi!=null) {

		if(fileName.toLowerCase().endsWith(".gz") && IJ.getVersion().compareTo("1.38s")<0) {
			IJ.error("ImageJ 1.38s or later required to open gzipped Biorad PIC files");
			return;
		}

		FileOpener fo = new FileOpener(fi);
		ImagePlus imp = fo.open(false);
		if(IJ.debugMode)IJ.log("imp="+imp);
		if (imp==null)
			return;
	    
		setStack(fileName, imp.getStack());
		setFileInfo(fi);
		if(IJ.debugMode) IJ.log("FileInfo="+fi);
		try {
			int pixelLength=1;
			switch (fi.fileType) {	// ghj 4/3/06
				case FileInfo.GRAY8: pixelLength=1; break;
				case FileInfo.GRAY16_UNSIGNED:pixelLength=2; break;
			}
			Calibration BioRadCal = getBioRadCalibration(fi.width, fi.height, fi.nImages, pixelLength);			// ghj 4/3/0
			setCalibration(BioRadCal);
		} catch (Exception e) {
			IJ.showStatus("");
			String msg = e.getMessage();
			if (e==null) msg = ""+e;
			if (msg.indexOf("EOF")==-1)
				IJ.showMessage("BioradReader", msg);
			return;
		}
	    
	    boolean hasLut = false;
	    
	    if (lutOffset!=-1) {
		try {hasLut = getLut(fi);}
		catch (Exception e) {
		    IJ.showStatus("");
		    IJ.showMessage("BioradReader",
				   "Can't read LUT from file because "+e);
		    hasLut = false;
		}
	    }
	    
	    if (hasLut) {
		ColorModel cm = 
		    new IndexColorModel(8, 256, fi.reds, fi.greens, fi.blues);
		getProcessor().setColorModel(cm);
		getStack().setColorModel(cm);
	    }
	    
	    if (!notes.equals(""))
		setProperty("Info", notes);
	    if(IJ.debugMode)IJ.log("arg=|"+arg+"|");
	    if (arg.equals("")) show();
	}
	if(IJ.debugMode)IJ.log("done");
    }

    int getByte() throws IOException {
	int b = f.read();
	if (b ==-1) throw new IOException("unexpected EOF");
	return b;
    }

    int getShort() throws IOException {
	int b0 = getByte();
	int b1 = getByte();
	return ((b1 << 8) + b0);
    }

    int getInt() throws IOException {
	int b0 = getShort();
	int b1 = getShort();
	return ((b1<<16) + b0);
    }

    void openFile() throws IOException {
		// Note that this has been changed to cope with gzipped files
		if (fileName.toLowerCase().endsWith(".pic.gz") ) {
			// gzipped pic file
			f = new BufferedInputStream(new GZIPInputStream(new FileInputStream(directory+fileName)));
		} else {
			// regular PIC file 
			f = new BufferedInputStream(new FileInputStream(directory+fileName));
		}
	}

    FileInfo getHeaderInfo() throws IOException {
	openFile();
	int width = getShort();      // 0-1
	int height = getShort();     // 2-3
	int nImages = getShort();  // 4-5
	f.skip(8);
	int byte_format = getShort(); //14-15
	f.skip(38);
	int magicNumber = getShort(); // 54-55
	f.close();
	
	// A Biorad .PIC file should have 12345 in bytes 54-55
	String notBioradPicFile = 
	    "This does not seem to be a Biorad Pic File";
	if (magicNumber!=12345)
	    throw new IOException(notBioradPicFile);
	
	FileInfo fi = new FileInfo();
	fi.fileFormat = FileInfo.RAW;
	fi.fileName = fileName;
	fi.directory = directory;
	fi.width = width;
	fi.height = height;
	fi.nImages = nImages;
	fi.offset = 76;
   		
	switch (byte_format) {
		case 1:
			fi.fileType = FileInfo.GRAY8;
			break;
		case 0:
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
			fi.intelByteOrder = true;	// ghj 4/3/06
			break;
	}
	if(IJ.debugMode)IJ.log("fileType ("+FileInfo.GRAY8+","+FileInfo.GRAY16_UNSIGNED+")="+fi.fileType);
	return fi;
    }

    /** Extracts the calibration info from the ASCII "notes" at the
	end of Biorad pic files. */
    Calibration getBioRadCalibration(int width, int height, int nImages,int pixelLength) // ghj 4/3/06
	throws IOException {
	Calibration BioRadCal = new Calibration();
	int NoteFlag, NoteType, Offset;
	String NoteContent = new String();
	byte[] TempByte = new byte[80];
	byte[] RawNote = new byte[80];
	double ScaleX, ScaleY;
	double ScaleZ=0, framesPerSecond = 0, frameInterval = 0;
	boolean xyt = false;
		
	Offset = 76 + height * width * nImages*pixelLength;	// ghj 4/3/06
	openFile();
	f.skip(Offset);
	if(IJ.debugMode)IJ.log("getBioRadCalibration");
	/** Do ... While : cycle through notes until you reach the last
	    note (indicated by bytes 2-5) For each note, different from
	    'live note', see if it contains axis calibration data.  of
	    so, extract the necessary values. */
	do {
	    f.skip(2);
	    NoteFlag = getInt(); // 2-5
	    f.skip(4);
	    NoteType = getShort(); // 10-11 
	    f.skip(4);
			
	    // store bytes 16-95 in a byte array
	    f.read(RawNote);
			
	    // replace illegal characters with 32
	    byte ch;
	    for (int i=0; i<80; i++) {
		ch = RawNote[i];
		TempByte[i] = (ch>=32 && ch<=126)?ch:32; 
	    }	
	    String Note = new String(TempByte);
	    notes += Note + "\n";	
	    // save note for Image/Show Info command
			
	    /** only analyze notes != 1, so skip all the notes that say
		'Live ...', they don't contain calibration info */
	    if (NoteType!=1) {
		NoteContent = getField(Note,1);
		/** See if first field contains keyword "AXIS_2" -->
		    X-axis calibration (don't know why Biorad calls it
		    '2'. */
		if (NoteContent.indexOf("AXIS_2") >= 0 ) {
		    //IJ.showMessage(Note);
		    //IJ.showMessage(getField(Note, 4));
		    ScaleX = s2d(getField(Note, 4));
		    BioRadCal.pixelWidth = ScaleX;
		    // fifth field contains units (mostly microns)
		    BioRadCal.setUnit(getField(Note, 5));
		} else if ( NoteContent.indexOf("AXIS_3") >= 0 )  { 
		    // contains Y-axis calibration
		    //IJ.showMessage(Note);
		    //IJ.showMessage(getField(Note, 4));
		    ScaleY = s2d( getField(Note, 4));
		    BioRadCal.pixelHeight = ScaleY;
		    BioRadCal.setUnit(getField(Note, 5));
		} else if ( NoteContent.indexOf("AXIS_4") >= 0 )  { 
		    // contains Z-axis calibration
		    //IJ.showMessage(Note);
		    //IJ.showMessage(getField(Note, 4));
		    ScaleZ = s2d( getField(Note, 4));
		    BioRadCal.pixelDepth = ScaleZ;
		    if (getField(Note,5).indexOf("Seconds")>=0) {
			xyt = true;
		    }					
		    /**ImageJ does not contain seperate units for the
		       Z-direction.  If the AXIS_4 units are "Seconds"
		       we are dealing with an xyt scan (no z movement)
		       otherwise we are dealing with a z-series.  see
		       below "if (xyt)" */
		} else if ( NoteContent.indexOf("INFO_FRAME_RATE")>=0 ) {
		    // Contains frame rate
		    //IJ.showMessage(Note);
		    //IJ.showMessage(getField(Note, 3));					
		    framesPerSecond = s2d(getField(Note,3));
		    frameInterval = 1/framesPerSecond;
		    BioRadCal.frameInterval = frameInterval;
		}											
	    }
	if(IJ.debugMode)IJ.log("Offset ="+Offset );
	    Offset += NOTE_SIZE; // Jump to next note
	    // stop if this was the last note
	} while ( NoteFlag != 0 &  f.available()>=NOTE_SIZE); 
	if(IJ.debugMode)IJ.log("lut?");
	/** A LUT can optionally follow the notes as a raw 768 byte
	    LUT, save the current offset and latter we will try to read
	    the LUT */
	lutOffset = Offset;
	if (xyt) {
	    /**If the file is an xyt scan (no z movement) the frame
	       interval (1/frame rate from the INFO_FRAME_RATE note)
	       should be used as the pixel depth, otherwise (z-series)
	       the pixel depth from the AXIS_4 note should be used */
					
	    BioRadCal.pixelDepth = frameInterval;
	} 
	f.close();
	if(IJ.debugMode)IJ.log("BioRadCal"+BioRadCal);
	return  BioRadCal; // return the filled biorad calibration
    }

    /** tries to read a 768 byte raw LUT at the end of the pic file.
	lutOffset is the byte after the last note, it was saved when
	the calibration information was read **/
    boolean getLut(FileInfo fi) throws IOException {
	
	openFile();
	f.skip(lutOffset);
	
	fi.reds = new byte[256];
	fi.greens = new byte[256];
	fi.blues = new byte[256];
	
	boolean hasLut = false;
	
	if (f.available()>=768) {
	    hasLut = true;
	    f.read(fi.reds);
	    f.read(fi.greens);
	    f.read(fi.blues);
	}
	
	f.close();
	
	return hasLut;
	}

    /* Extracts a certain field from a string. One (or more) spaces are 
        considered the field delimiter. This version corrects a 
        long-standing bug that resulted in spaces being prepended to units. */
    String getField(String str, int fieldIndex) {
        char delimiter = ' ';
        int startIndex=0, endIndex;
        for (int i=1; i<fieldIndex; i++)
            startIndex = str.indexOf(delimiter, startIndex)+1;
        // NB This means that each field must be at least length 1
        endIndex = str.indexOf(delimiter, startIndex+1);
        // If we can't find another instance of delim, read to end of Note
        if(endIndex==-1) endIndex=str.length();
        if (startIndex>=0 && endIndex>=0)
            return str.substring(startIndex, endIndex); 
        else
            return "";
    }
	
    /** Converts a string to a double. Returns 1.0 if the string does
	not contain a valid number. Updated to use ImageJ's Tool class 
	from the utility package */
    double s2d(String s) {
	return Tools.parseDouble(s);
    }
}

/*
  Bio-Rad(TM) .PIC Image File Information
  (taken from: "Introductory Edited Version 1.0", issue 1/12/93.)
  (Location of Image Calibration Parameters in Comos 6.03 and MPL .PIC files)

  The general structure of Bio-Rad .PIC files is as follows:

  HEADER (76 bytes)
  Image data (#1)
  .
  .
  Image data (#npic)
  NOTE (#1)
  .                       ; NOTES are optional.
  .
  NOTE (#notes)
  RGB LUT (color Look Up Table)


  Header Information:

  The header of Bio-Rad .PIC files is fixed in size, and is 76 bytes.

  ------------------------------------------------------------------------------
  'C' Definition              byte    size    Information
  (bytes)   
  ------------------------------------------------------------------------------
  int nx, ny;                 0       2*2     image width and height in pixels
  int npic;                   4       2       number of images in file
  int ramp1_min, ramp1_max;   6       2*2     LUT1 ramp min. and max.
  NOTE *notes;                10      4       no notes=0; has notes=non zero
  BOOL byte_format;           14      2       bytes=TRUE(1); words=FALSE(0)
  int n;                      16      2       image number within file
  char name[32];              18      32      file name
  int merged;                 50      2       merged format
  unsigned color1;            52      2       LUT1 color status
  unsigned file_id;           54      2       valid .PIC file=12345
  int ramp2_min, ramp2_max;   56      2*2     LUT2 ramp min. and max.
  unsigned color2;            60      2       LUT2 color status
  BOOL edited;                62      2       image has been edited=TRUE(1)
  int _lens;                  64      2       Integer part of lens magnification
  float mag_factor;           66      4       4 byte real mag. factor (old ver.)
  unsigned dummy[3];          70      6       NOT USED (old ver.=real lens mag.)
  ------------------------------------------------------------------------------

  Additional information about the HEADER structure:

  Bytes   Description     Details
  ------------------------------------------------------------------------------
  0-9     nx, ny, npic, ramp1_min, ramp1_max; (all are 2-byte integers)

  10-13   notes           NOTES are present in the file, otherwise there are
  none.  NOTES follow immediately after image data at
  the end of the file.  Each note os 96 bytes long.

  14-15   byte_format     Read as a 2 byte integer.  If this is set to 1, then
  each pixel is 8-bits; otherwise pixels are 16-bits.

  16-17   n               Only used in COMOS/SOM when the file is loaded into
  memory.

  18-49   name            The name of the file (without path); zero terminated.

  50-51   merged          see Note 1.

  52-53   colour1

  54-55   file_id         Read as a 2 byte integer.  Aways set to 12345.
  Just a check that the file is in Bio-Rad .PIC format.

  56-59   ramp2_min/max   Read as 2 byte integers.

  60-61   color2          Read as a 2 byte integer.

  62-63   edited          Not used in disk files.

  64-65   int_lens        Read as a 2 byte integer.
  Integer part of the objective lens used.

  66-69   mag_factor      Read as a 4-byte real.

  mag. factor=(float)(dispbox.dy*2)/(float)(512.0*scandata.ly)

  where:  dispbox.dy = the width of the image.
  scandata.ly = the width of the scan region.

  the pixel size in microns can be calculated as follows:

  pixel size = scale_factor/lens/mag_factor

  where:  lens = the objective lens used as a floating pt. number
  scale_factor = the scaling number setup for the system
  on which the image was collected.

  70-75   dummy[3]    Last 6 bytes not used in current version of disk file
  format. (older versions stored a 4 byte real lens mag
  here.)
  ------------------------------------------------------------------------------

  Note 1 : Values stored in bytes 50-51 :

  0        : Merge off
  1        : 4-bit merge
  2        : Alternate 8-bit merge
  3        : Alternate columns merge
  4        : Alternate rows merge
  5        : Maximum pixel intensity merge
  6        : 256 colour optimised merge with RGB LUT saved at the end
  of each merge.
  7        : As 6 except that RGB LUT saved after all the notes.


  Information about NOTE structure and the RGB LUT are not included in this
  file.  Please see the Bio-Rad manual for more information.


  ==============================================================================

  Info added by Geert Meesen from MRC-600 and MRC-1024 Manuals.

  -------------------------------------------------------------

  Note Structure : 

  Bytes   Description     Details
  ------------------------------------------------------------------------------
  0-1     Display level of this note

  2-5     =0 if this is the last note, else there is another note (32 bit integer)

  10-11   Note type := 1 for live collection note,
  := 2 for note including file name,
  := 3 if note for multiplier file,
  := 4, 5, etc.,; additional descriptive notes

  16-95   Text of note (80 bytes)


  =============================================================================

  Info added by Geert Meesen from personal experiments.

  ------------------------------------------------------------

  - Until now I only have experience with 8-bit images from the MRC-1024 confocal microscope. 
  The newer microscopes (Radiance 2000, for example) are capable of generating 16 bit images, 
  I think. I have access to such a microscope and will try to find out later. For now it
  should be possible to look at the byte-word flag in the header.

  - I have experience with two types of images : 
  --- One slice in the Z-direction, 3 channels of recording. This type is stored as a three-slice image
  with the 3 channels in consecutive layers. (Single-Slice)
  --- Different Z slices with only one channel. (Z-stack)

  - The header should contain some info about the pixel-size, but until now I was not really
  able to interpret this info. It's easier to extract the info from the notes at the end.
  You can find 3 notes saying something like (from AUPCE.NOT, a Z-stack file)

  AXIS_2 001 0.000000e+00 2.999667e-01 microns                                    
  AXIS_3 001 0.000000e+00 2.999667e-01 microns                                    
  AXIS_4 001 0.000000e+00 1.000000e+00 microns                                    
  AXIS_9 011 0.000000e+00 1.000000e+00 RGB channel

  These lines give the pixelsize for the X (axis_2), Y (axis_3) and Z (axis_4) axis in the units mentioned. I don't
  know if this unit is always 'microns'.

  For a Single-Slice images you get ( from AB003A.NOT, a Single-Slice image) :

  AXIS_2 001 0.000000e+00 1.799800e+00 microns
  AXIS_3 001 0.000000e+00 1.799800e+00 microns
  AXIS_4 011 0.000000e+00 1.000000e+00 RGB channel

  It seems that AXIS_4 is used for indicating an RGB channel image.
*/
