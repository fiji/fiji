package io;

import ij.plugin.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import java.io.*;

/** This plugin opens images in the IPLab/Windows (.IPL)  format. */
public class IPLab_Reader extends ImagePlus implements PlugIn {

    boolean littleEndian;
    RandomAccessStream f;
    boolean isColor48;
    int nextBlock;
    int itag;
 
    public void run(String arg) {
        OpenDialog od = new OpenDialog("Open IPLab...", arg);
        String directory = od.getDirectory();
        String name = od.getFileName();
        if (name==null)
            return;
        IJ.showStatus("Opening: " + directory + name);
        open(directory, name, arg);
    }

    void open(String directory, String name, String arg) {
        FileInfo fi = null;
        try {
            fi = getFileInfo(directory, name);
        } catch (IOException e) {
            IJ.showStatus("");
            String error = e.getMessage();
            if (error==null || error.equals(""))
                error = ""+e;
            IJ.showMessage("IPLab Reader", ""+error);
            return;
         }
         if (fi!=null) {
            FileOpener fo = new FileOpener(fi);
            ImagePlus imp = fo.open(false);
            if (imp==null)
                return;
            if (isColor48 && imp.getBitDepth()==16 && imp.getStackSize()==3) 
                handleColor48Image(imp);
            setStack(name,  imp.getStack());
            setCalibration(imp.getCalibration());
            if (arg.equals("")) show();
        }
    }

    void handleColor48Image(ImagePlus imp) {
            ImageStack stack  =  imp.getStack();
            stack.setSliceLabel( "Red", 1);
            stack.setSliceLabel( "Green", 2);
            stack.setSliceLabel( "Blue", 3);
            convertColor48ToRGB(imp);
    }

    void convertColor48ToRGB(ImagePlus imp) {
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageProcessor ip;
        ImageStack stack1 = imp.getStack();
        ImageStack stack2 = new ImageStack(width, height);
        for (int i=1; i<=stack1.getSize(); i++) {
            ip = stack1.getProcessor(i);
            ImageProcessor ip2 = ip.duplicate();
            stack2.addSlice(null, ip2);
        }
        ImagePlus imp2 = imp.createImagePlus();
        imp2.setStack("RGB of "+imp.getTitle(), stack2);
        ImageProcessor ip2 = imp2.getProcessor();
        StackConverter sc = new StackConverter(imp2);
        sc.convertToGray8();
        ImageConverter ic = new ImageConverter(imp2);
        ic.convertRGBStackToRGB();
        imp2.show();
    }

    FileInfo getFileInfo(String directory, String name) throws IOException {
        FileInfo fi = new FileInfo();
        f = new RandomAccessStream(new RandomAccessFile(directory + name, "r"));
        //f = new BufferedInputStream(new FileInputStream(directory+name));
        int b0=getByte(), b1=getByte(), b2=getByte(), b3=getByte();
        if (b0==105&&b1==105&&b2==105&&b3==105) // "iiii"
            littleEndian = true;
        else if (b0==109&&b1==109&&b2==109&&b3==109) // "mmmm"
            littleEndian = false;
        else
              throw new IOException("This does not appear to be an IPLab/Windows (.IPL) file.");
        f.skip(8);
        if (!(getByte()==100&&getByte()==97&&getByte()==116&&getByte()==97)) // "data"
            throw new IOException("This does not appear to be an IPLab/Windows (.IPL) file.");
        int blockSize = getInt();
        nextBlock = 16 + blockSize;
        fi.width = getInt();
        fi.height = getInt();
        int nChannels = getInt();
        int zPlanes = getInt();
        int tVolumes = getInt();
        int dataType = getInt();
        fi.fileFormat = fi.RAW;
        fi.fileName = name;
        fi.directory = directory;
        fi.nImages = zPlanes*tVolumes;
        fi.intelByteOrder = littleEndian;
        fi.offset = 44;
        switch (dataType) {
            case 0:
                if (nChannels==3)
                    fi.fileType = FileInfo.RGB_PLANAR;
                else
                    fi.fileType = FileInfo.GRAY8;
                break;
            case 1:
                fi.fileType = FileInfo.GRAY16_SIGNED; 
                break;
            case 2:
                fi.fileType = FileInfo.GRAY16_UNSIGNED;
                if (fi.nImages==1)
                    fi.nImages = nChannels;
                isColor48 = nChannels==3;
                break;
            case 3:
                fi.fileType = FileInfo.GRAY32_INT;
                break;
            case 4:
                 fi.fileType = FileInfo.GRAY32_FLOAT; 
                 break;
        }
        String tag = null;
        do {
            tag = getNextBlock();
            if (tag==null) break;
            if (tag.equals("unit"))
                getUnits(fi);
            else if (itag==0x2d45c4f6)
               decodeAcquireTag(fi);
        } while (tag!=null);
        f.close();
        return fi;
    }

    String getNextBlock() throws IOException {
        f.seek(nextBlock);
        String tag = getString();
        int blockSize = getInt();
        //IJ.log(blockSize+"  " + nextBlock+"  \""+tag+"\"" + "  " +Integer.toHexString(itag));
        nextBlock = nextBlock + 8 + blockSize;
        if (blockSize==0)
            return null;
        else
            return tag;
    }

    void getUnits(FileInfo fi) throws IOException {
        float[] unitsPerPixel = new float[5];
        int[] unit = new int[5];
        for (int i=0; i<4; i++) {
            getInt();
            unitsPerPixel[i] = getFloat(); 
            unit[i] = getShort();
            getShort(); // skip 2 filler bytes
            //IJ.log(i+"  "+unitsPerPixel[i]+"    "+unit[i]);
        }
        if (unitsPerPixel[0]!=0.0) {
            fi.pixelWidth = 1.0/unitsPerPixel[0];
            fi.pixelHeight = fi.pixelWidth;
        }
        switch (unit[0]) {
            case 1: fi.unit="um"; break;
            case 2: fi.unit="mm"; break;
            case 3: fi.unit="cm"; break;
            case 4: fi.unit="m"; break;
            case 5: fi.unit="inch"; break;
            case 6: fi.unit="ft"; break;
        }
    }
    
    void decodeAcquireTag(FileInfo fi) throws IOException {
        f.skip(1276);
        double zSpacing = getFloat();
        //IJ.log("decodeAcquireTag: "+zSpacing);
        if (zSpacing>0.0)
            fi.pixelDepth = zSpacing;
    }

    int getByte() throws IOException {
        int b = f.read();
        if (b ==-1) throw new IOException("unexpected EOF");
        return b;
    }

    int getShort() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        if (littleEndian)
            return ((b1<<8) + b0);
        else
            return ((b0<<8) + b1);
    }
    
    final int getInt() throws IOException {
        int b0 = getByte();
        int b1 = getByte();
        int b2 = getByte();
        int b3 = getByte();
        if (littleEndian)
            return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
        else
            return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
    }

    float getFloat()  throws IOException {
        return Float.intBitsToFloat(getInt());
    }

    String getString() throws IOException {
        //reads next  4 bytes and returns them as a string
        byte[]  list = new byte[4];
        f.read(list);
        if (littleEndian)
            itag = ((list[0]<<24) + (list[1]<<16) + (list[2]<<8) + list[3]);
        else
            itag = ((list[3]<<24) + (list[2]<<16) + (list[1]<<8) + list[0]);
        return new String(list);
    }

}

/*
#define kAcquireTagID		0xF6C5462D//TAG value use this to identify the tag
		
struct AcquireXTag {
	char			cTag[4];					//0xF6C5462D //tag id //check first four bytes for this long hex value
	long			lSize;						// 2016 bytes size of the tag without tag and size (2024 - 8)
	float			fVersion;					//4
	long			lTime;						//4 will cast to time_t structure for time extraction
	char			szExpName[32];			//32 Exp. Name
	char			cExpID[16];				//16 GUID per experiment
	char			cGeneralUnused[512];	//512 (568)
	////////////////////////////////////////////////////////////////////
	//////////////Camera Settings/    //////////////////////////////////
	////////////////////////////////////////////////////////////////////
	char			szCameraName[32];		//32 Camera Name
	long			lXResolution;				//4 CC X Max
	long			lYResolution;				//4 CCD Y Max
	long			lLeft;						//4 CCD Coordinates used
	long			lRight;						//4
	long			lTop;						//4
	long			lBottom;					//4
	long			lBinX;						//4 Binning in X direction
	long			lBinY;						//4 Binning in Y Direction
	float			fExposure;				//4 Exposure
	float			fCalibrationXY;			//4 XY image calibration for current binn
	long			lCalibrationXYUnits;		//4 Units for the XY calibration
	long			lGain;						//4 Camera Gain
	long			lEGain;					//4 Multiplicative/Intensifier Gain
	long			lCameraSpeed;			//4 Speed setting for camera
	char			szCameraSpeed[32];	//32 Speed as string
	long			lCameraOffset;			//4 Camera Offset
	long			lTriggerMode;				//4 Trigger mode
	char			szTriggerMode[32];		//32 Trigger mode as string
	long			lBitDepth;					//4
	char			cCameraUnused[508];	//508 (672)
	////////////////////////////////////////////////////////////////////
	//////////////Microscope Settings///////////////////////////////////
	////////////////////////////////////////////////////////////////////
	//Z Axis Settings
	char			szZAxis[32];				//32 Z Axis Name
	float			fZStepsPerMicron;		//4 Steps per micron for Z Axis 
	float			fZSpacing;				//4 Z Spacing  //1276 (568+672+36)
	float			fZCurPos;					//4 Z Current Position
	//XY Stage Settings
	char			szXYStage[32];			//32 XY Stage Name
	float			fXStepsPerMicron;		//4 X Steps per micron
	float			fYStepsPerMicron;		//4 Y Steps per micron
	float			fXPosition;				//4 X Current position
	float			fYPosition;				//4 Y Current position
	long			lCurRow;					//4 Current array row
	long			lCurCol;					//4 Current array col
	long			lCurSuperRow;			//4 Current super row
	long			lCurSuperCol;			//4 Current super col
	//Shutter Settings
	char			szShutterName[32];		//32 Shutter Name
	float			fShutterDelay;			//4 Shutter Delay
	//Filter Settings
	char			szFilterName[32];		//32 Filter Name
	long			lFilterPos;					//4 Filter Position
	long			lFilterWavelength;		//4 Filter Wavelength
	char			szFilterPosName[32];	//32 Filter Position Name
	//Objective Settings
	float			fNA;						//4 Numerical Aperature
	float			fRefactiveIndex;			//4 Refractive Index
	long			lObjectivePos;			//4 Objective Position
	char			szObjectiveName[32];	//32 Objective Name
	char			szObjectivePosName[32]; //32 Objective Position Name
	//Other Settingsing
	float			fMagFactor;				//4 Magnification factor other than objective

	char			cMicUnused[480];		//480
	////////////////////////////////////////////////////////////////////
};
*/


