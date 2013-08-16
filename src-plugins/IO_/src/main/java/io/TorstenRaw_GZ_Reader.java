package io;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.io.*;
import java.net.MalformedURLException;
import java.util.zip.GZIPInputStream;

/** This plugin loads the raw binary files returned
 *by Torsten Rohlfing's software
 *by parsing the accompanying header file.

  Greg Jefferis 5 Feb 2004

 The bare bones were pinched from Guy Williams'
 Analyze_Reader plugin
*/

// 2007-02-16
// Fixed reading of signed 16 bit images

public class TorstenRaw_GZ_Reader extends ImagePlus implements PlugIn
{
	public String imageName=null;
	protected File headerFile=null;
	protected File dataFile=null;

	public void run(String arg) {
		String directory = "", name = arg;
		if ((arg==null) || (arg==""))
		{
			OpenDialog od = new OpenDialog("Load TorstenRaw File...", arg);
			name = od.getFileName();
			if (name==null)
			  return;
			directory = od.getDirectory();
		}
		else
		{
			File dest = new File(arg);
			directory = dest.getParent();
			name = dest.getName();
		}

		ImagePlus imp = load(directory, name);

		if (imp==null) return;  // failed to load the file
		if (imageName!=null) {
			// set the name of the image to the name found inside the load method
			// ie the name of the enclosing directory rather than the
			// generic image.bin.gz of the image file itself
			setStack(imageName, imp.getStack());
		} else {
			setStack(name, imp.getStack());
		}

		// bring over the calibration information as well
		copyScale(imp);

		// if we weren't sent a filename but chose one, then show the image
		if (arg.equals("")) show();
	}

	public ImagePlus load(String directory, String name) {

		FileInfo fi = new FileInfo();
		try {
			fi=getHeaderInfo(directory, name);
		}
		catch (IOException e) {
			IJ.write("FileLoader: "+ e.getMessage());
			// should we continue at this point?
			// I suppose we could just load the image file as raw
			// but we should really pop up the raw reader
			return null;
		}

		if (!directory.endsWith(File.separator)) directory += File.separator;
		IJ.showStatus("Loading Torsten Raw File: " + directory + name);

		ImagePlus imp;
		FileOpener fo;
		// if running ImageJ >=1.38s then use built in FileOpener
		// else call my gzip opener plugin which has had the
		// createInputStream method overloaded.
		if(name.toLowerCase().endsWith(".gz")  && (IJ.getVersion().compareTo("1.38s")<0)) {
			fo=new GZIPFileOpener(fi);
		} else {
			fo = new FileOpener(fi);
		}
		imp = fo.open(false);
		if(imp==null) return null;

		// Copy over the spatial scale info which we found in readHeader
		// nb the first we don't just overwrite the current calibration
		// because this may have density calibration for signed images
		Calibration cal = imp.getCalibration();
		Calibration spatialCal=this.getCalibration();
		cal.pixelWidth=spatialCal.pixelWidth;
		cal.pixelHeight=spatialCal.pixelHeight;
		cal.pixelDepth=spatialCal.pixelDepth;
		cal.setUnit(spatialCal.getUnit());
		imp.setCalibration(cal);

		return imp;
	}

	File getHeaderFile (String directory, String name) throws IOException {

		if ((name == null) || (name == "")) return null;

		if (name.endsWith(".bin") || name.endsWith(".bin.gz")) {
			File imageFile = new File(directory,name);
			File imageDir = new File(imageFile.getParent());
			File commonDir = new File(imageDir.getParent());
			// set the class variable imageName to the name of the parent directory
			// since the image file itself has a boring name
			imageName=imageDir.getName();

			File headerFile=null;
			String[] list = commonDir.list();
			if (list==null) return null;
			for (int i=0; i<list.length; i++) {
				File f = new File(commonDir.getPath(),list[i]);
				//IJ.log(f.getName()+f.exists());
				if (f.isDirectory() &&
					f.getName().equals(imageDir.getName()+".study")) {
					// found the header file that describes the image
					headerFile=new File(f.getPath(),"images");
					//IJ.log(headerFile.getName()+f.exists());
					if (headerFile.exists()){
						return headerFile;
					} else {
						//IJ.log("No header file for image: "+name);
						// make a default fi?
						throw new IOException("No header file for image");
					}
				}
			}
		}
		return null;
	}

	public FileInfo getHeaderInfo( String dir, String fileName ) throws IOException {
		if(dataFile==null) dataFile = new File(dir, fileName);
		if(!dataFile.exists()){
			IJ.write("FileLoader: unable to find data file: "+dataFile);
			return null;
		}
		if(headerFile==null) headerFile = getHeaderFile(dir, fileName);

		BufferedReader input = new BufferedReader(new FileReader(headerFile));
		FileInfo fi = new FileInfo();
		fi.fileName=dataFile.getName();
		fi.directory=dataFile.getParent();
		fi.fileFormat = FileInfo.RAW;

		String thisLine,noteType,noteValue;
		Calibration spatialCal=this.getCalibration();

		boolean signed = false;
		int bytesPerPixel=1;
		fi.fileType = FileInfo.GRAY8;  // just assume this for the mo
		// parse the header file
		while((thisLine=input.readLine())!=null) {
			noteType=getField(thisLine,1);
			noteValue=getField(thisLine,2);
			if (IJ.debugMode) IJ.log("NoteType:"+noteType+", noteValue:"+noteValue);

			if (noteType.equals("width")) fi.width=Integer.valueOf(noteValue).intValue();
			if (noteType.equals("height")) fi.height=Integer.valueOf(noteValue).intValue();
			if (noteType.equals("depth")) fi.nImages=Integer.valueOf(noteValue).intValue();

			if (noteType.equals("bytesperpixel")) bytesPerPixel=Integer.valueOf(noteValue).intValue();
			if (noteType.equals("signed")) signed=noteValue.equals("yes");

			// for some reason the second set of notes in the header file seems to set the
			// calibrationx and calibrationy to 0 in many instances even though there
			// was a perfectly good value further up
			if (noteType.equals("calibrationx") && Double.valueOf(noteValue).doubleValue()>0)
				spatialCal.pixelWidth =Double.valueOf(noteValue).doubleValue();
			if (noteType.equals("calibrationy") && Double.valueOf(noteValue).doubleValue()>0)
				spatialCal.pixelHeight =Double.valueOf(noteValue).doubleValue();
			if (noteType.equals("slicedistance")) spatialCal.pixelDepth =Double.valueOf(noteValue).doubleValue();
			if (noteType.equals("offset")) fi.offset=Integer.valueOf(noteValue).intValue();
			if (noteType.equals("littleendian")) {
				if(noteValue.equals("yes")) {
					fi.intelByteOrder = true;
				} else {
					fi.intelByteOrder = false;
				}
			}
			if (noteType.equals("width")) fi.width=Integer.valueOf(noteValue).intValue();
		}

		switch(bytesPerPixel){
			case 1:
				if (!signed) fi.fileType=FileInfo.GRAY8;
				else throw new IOException("Unimplemented ImageData signed 8 bit");
				break;
			case 2:
				if(signed) fi.fileType=FileInfo.GRAY16_SIGNED;
				else fi.fileType=FileInfo.GRAY16_UNSIGNED;
				break;

			case 4:
				//if(signed) fi.fileType=FileInfo.GRAY32_INT;
				//else fi.fileType=FileInfo.GRAY32_UNSIGNED;
				// GJ: This is daft, but there seems to be no way to tell a 4
				// byte float from a 4 byte long; since floats are a much
				// better use of 4 bytes, will just assume that.
				fi.fileType=FileInfo.GRAY32_FLOAT;
				IJ.log("Assuming data is float");
				break;
			default:
				throw new IOException("Unimplemented ImageData bytes/pixel="+bytesPerPixel+", signed ="+signed+"");
		}

		spatialCal.setUnit("micron");  // just assume this for the mo
		this.setCalibration(spatialCal);
		return (fi);
	}

	// This gets a space delimited field from a string
	// but note only works with Java >=1.4 Ithink
	String getField(String str, int fieldIndex) {
		str=str.trim(); // trim the string
		String[] fields=str.split("\\s+");
		if (fieldIndex>fields.length) {
			return "";
		} else {
			return fields[fieldIndex-1];
		}
	}

	class GZIPFileOpener extends FileOpener {
		// private class which allows gzip files to be opened
		public GZIPFileOpener(FileInfo fi) {
			super(fi);
		}

		public InputStream createInputStream(FileInfo fi) throws IOException, MalformedURLException {
			// use the method in the FileOpener class to generate an input stream
			InputStream is=super.createInputStream(fi);
			if (is!=null) {
				// then stick a GZIPInputStream on top of it!
				return new GZIPInputStream(is);
			} else {
				return is;
			}
		}
	}
}
