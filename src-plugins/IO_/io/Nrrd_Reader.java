package io;
// Nrrd_Reader
// -----------

// (c) Gregory Jefferis 2007
// Department of Zoology, University of Cambridge
// jefferis@gmail.com
// All rights reserved
// Source code released under Lesser Gnu Public License v2

// TODO
// - Support for multichannel images
//   (problem is how to figure out they are multichannel in the absence of 
//   other info - not strictly required by nrrd format)
// - time datasets
// - line skip (only byte skip at present)
// - calculating spacing information from axis mins/cell info

// Compiling:
// You must compile Nrrd_Writer.java first because this plugin
// depends on the NrrdFileInfo class declared in that file

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * ImageJ plugin to read a file in Gordon Kindlmann's NRRD 
 * or 'nearly raw raster data' format, a simple format which handles
 * coordinate systems and data types in a very general way.
 * See <A HREF="http://teem.sourceforge.net/nrrd">http://teem.sourceforge.net/nrrd</A>
 * and <A HREF="http://flybrain.stanford.edu/nrrd">http://flybrain.stanford.edu/nrrd</A>
 */

public class Nrrd_Reader extends ImagePlus implements PlugIn 
{
	public final String uint8Types="uchar, unsigned char, uint8, uint8_t";
	public final String int16Types="short, short int, signed short, signed short int, int16, int16_t";
	public final String uint16Types="ushort, unsigned short, unsigned short int, uint16, uint16_t";
	public final String int32Types="int, signed int, int32, int32_t";
	public final String uint32Types="uint, unsigned int, uint32, uint32_t";
	private String notes = "";
	
	private boolean detachedHeader=false;
	public String headerPath=null;
	public String imagePath=null;
	public String imageName=null;
	
	public void run(String arg) {
		String directory = "", name = arg;
		if ((arg==null) || (arg==""))
		{
			OpenDialog od = new OpenDialog("Load Nrrd (or .nhdr) File...", arg);
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
			// TOFIX - what should the name be?  There could be several
			// image files referenced in a single detached .nhdr
			setStack(imageName, imp.getStack());
		} else {
			setStack(name, imp.getStack());
		}
				
		if (!notes.equals(""))
			setProperty("Info", notes);
		// bring over the calibration information as well
		copyScale(imp);
		
		// if we weren't sent a filename but chose one, then show the image
		if (arg.equals("")) show();
	}

	public ImagePlus load(String directory, String fileName) {    

		if (!directory.endsWith(File.separator)) directory += File.separator;
		if ((fileName == null) || (fileName == "")) return null;
		
//		imagePath=fi.directory+fi.fileName;
//		headerPath=fi.directory+fi.fileName;
		
		NrrdFileInfo fi;
		try {
			fi=getHeaderInfo(directory, fileName);
		}
		catch (IOException e) { 
			IJ.write("readHeader: "+ e.getMessage()); 
			return null;
		}
		if (IJ.debugMode) IJ.log("fi:"+fi);
		
		IJ.showStatus("Loading Nrrd File: " + directory + fileName);
		
		ImagePlus imp; FlexibleFileOpener gzfo;
		
		if(fi.encoding.equals("gzip") && detachedHeader) {
			// call my nice gzip opener plugin which has had the 
			// createInputStream method overloaded.
			gzfo = new FlexibleFileOpener(fi,FlexibleFileOpener.GZIP);
			imp = gzfo.open(false);
		} else if(fi.encoding.equals("gzip")) {
			long preOffset=fi.longOffset>0?fi.longOffset:fi.offset;
			fi.offset=0;fi.longOffset=0;
			gzfo= new FlexibleFileOpener(fi,FlexibleFileOpener.GZIP,preOffset);
			if (IJ.debugMode) IJ.log("gzfo:"+gzfo);
			imp = gzfo.open(false);			
		} else {
			FileOpener fo = new FileOpener(fi);  
			imp = fo.open(false);
		}
		if(imp==null) return null;
		
		// Copy over the spatial scale info which we found in readHeader
		// nb the first we don't just overwrite the current calibration 
		// because this may have density calibration for signed images 
		Calibration cal = imp.getCalibration();
		Calibration spatialCal = this.getCalibration();
		cal.pixelWidth=spatialCal.pixelWidth;
		cal.pixelHeight=spatialCal.pixelHeight;        
		cal.pixelDepth=spatialCal.pixelDepth;
		cal.setUnit(spatialCal.getUnit());
		cal.xOrigin=spatialCal.xOrigin;		
		cal.yOrigin=spatialCal.yOrigin;		
		cal.zOrigin=spatialCal.zOrigin;
		imp.setCalibration(cal);		

		return imp; 
	} 
	
	public NrrdFileInfo getHeaderInfo( String directory, String fileName ) throws IOException {

		if (IJ.debugMode) IJ.log("Entering Nrrd_Reader.readHeader():");
		NrrdFileInfo fi = new NrrdFileInfo();
		fi.directory=directory; fi.fileName=fileName;
		Calibration spatialCal = this.getCalibration();
		
		// NB Need RAF in order to ensure that we know file offset
		RandomAccessFile input = new RandomAccessFile(fi.directory+fi.fileName,"r");

		String thisLine,noteType,noteValue, noteValuelc;

		fi.fileType = FileInfo.GRAY8;  // just assume this for the mo
		spatialCal.setUnit("micron");  // just assume this for the mo    
		fi.fileFormat = FileInfo.RAW;
		fi.nImages = 1;

		// parse the header file, until reach an empty line//	boolean keepReading=true;
		while(true) {
			thisLine=input.readLine();
			if(thisLine==null || thisLine.equals("")) {
				if(!detachedHeader) fi.longOffset = input.getFilePointer();
				break;
			}		
			notes+=thisLine+"\n";

			if(thisLine.indexOf("#")==0) continue; // ignore comments

			noteType=getFieldPart(thisLine,0).toLowerCase(); // case irrelevant
			noteValue=getFieldPart(thisLine,1);
			noteValuelc=noteValue.toLowerCase();
			String firstNoteValue=getSubField(thisLine,0);
//			String firstNoteValuelc=firstNoteValue.toLowerCase();

			if (IJ.debugMode) IJ.log("NoteType:"+noteType+", noteValue:"+noteValue);

			if (noteType.equals("data file")||noteType.equals("datafile")) {
				// This is a detached header file
				// There are 3 kinds of specification for the data files
				// 	1.	data file: <filename>
				//	2.	data file: <format> <min> <max> <step> [<subdim>]
				//	3.	data file: LIST [<subdim>]
				if(firstNoteValue.equals("LIST")) {
					// TOFIX - type 3
					throw new IOException("Nrrd_Reader: not yet able to handle datafile: LIST specifications");
				} else if(!getSubField(thisLine,1).equals("")) {
					// TOFIX - type 2
					throw new IOException("Nrrd_Reader: not yet able to handle datafile: sprintf file specifications");
				} else {
					// Type 1 specification
					File imageFile;
					// Relative or absolute
					if(noteValue.indexOf("/")==0) {
						// absolute
						imageFile=new File(noteValue);
						// TOFIX could also check local directory if absolute path given
						// but dir does not exist
					} else {
						//IJ.log("fi.directory = "+fi.directory);					
						imageFile=new File(fi.directory,noteValue);
					}
					//IJ.log("image file ="+imageFile);

					if(imageFile.exists()) {
						fi.directory=imageFile.getParent();
						fi.fileName=imageFile.getName();
						imagePath=imageFile.getPath();
						detachedHeader=true;
					} else {
						throw new IOException("Unable to find image file ="+imageFile.getPath());
					}
				}										
			}

			if (noteType.equals("dimension")) {
				fi.dimension=Integer.valueOf(noteValue).intValue();
				if(fi.dimension>3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
			}
			if (noteType.equals("sizes")) {
				fi.sizes=new int[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					fi.sizes[i]=Integer.valueOf(getSubField(thisLine,i)).intValue();
					if(i==0) fi.width=fi.sizes[0];
					if(i==1) fi.height=fi.sizes[1];
					if(i==2) fi.nImages=fi.sizes[2];
				}
			}

			if (noteType.equals("units")) spatialCal.setUnit(firstNoteValue);
			if (noteType.equals("space units")) spatialCal.setUnit(firstNoteValue);
			if (noteType.equals("spacings")) {
				double[] spacings=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					spacings[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					if(i==0) spatialCal.pixelWidth=spacings[0];
					if(i==1) spatialCal.pixelHeight=spacings[1];
					if(i==2) spatialCal.pixelDepth=spacings[2];
				}
			}
			
			if(noteType.equals("space dimension")){
				fi.spaceDims=Integer.valueOf(noteValue).intValue();
				// TODO Add support for different image and space dimensions
				if(fi.spaceDims!=fi.dimension) throw new IOException
					("Nrrd_Reader: Don't yet know how to handle image dimension!=space dimension!");
				 				
			}
			if(noteType.equals("space")){
				fi.setSpace(noteValue);
				if(fi.spaceDims>3) throw new IOException
					("Nrrd_Reader: Dimension>3 not yet implemented!");				
			}
			
			if(noteType.equals("space directions")){
				double[][] spaceDirs = new double[fi.spaceDims][fi.spaceDims];

				// There should be fi.dimension entries in space directions
				for(int i=0,dim=0;i<fi.dimension;i++){
					double[] vec=getVector(noteValue, i);
					// ... but only spaceDim non-null vectors
					if(vec==null) continue;  
					for(int j=0;j<fi.spaceDims;j++){
						spaceDirs[dim][j]=vec[j];
					}
					dim++;
				}
				fi.setSpaceDirs(spaceDirs);
			}
			
			if (noteType.equals("space origin")){
				fi.setSpaceOrigin(getVector(thisLine,0));
			}
			
			if (noteType.equals("centers") || noteType.equals("centerings")) {
				fi.centers=new String[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					fi.centers[i]=getSubField(thisLine,i);
				}
			}

			if (noteType.equals("axis mins") || noteType.equals("axismins")) {
				double[] axismins=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					// NB xOrigin are in pixels, whereas axismins are of course
					// in units; these are converted later
					axismins[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					if(i==0) spatialCal.xOrigin=axismins[0];
					if(i==1) spatialCal.yOrigin=axismins[1];
					if(i==2) spatialCal.zOrigin=axismins[2];
				}
			}
			if (noteType.equals("type")) {
				if (uint8Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY8;
				} else if(uint16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_UNSIGNED;
				} else if(int16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_SIGNED;
				} else if(uint32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_UNSIGNED;
				} else if(int32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_INT;
				} else if(noteValuelc.equals("float")) {
					fi.fileType=FileInfo.GRAY32_FLOAT;
				} else if(noteValuelc.equals("double")) {
					fi.fileType=FileInfo.GRAY64_FLOAT;
				} else {
					throw new IOException("Unimplemented data type ="+noteValue);
				}
			}

			if (noteType.equals("byte skip")||noteType.equals("byteskip")) fi.longOffset=Long.valueOf(noteValue).longValue();
			if (noteType.equals("endian")) {
				if(noteValuelc.equals("little")) {
					fi.intelByteOrder = true;
				} else {
					fi.intelByteOrder = false;
				}
			}

			if (noteType.equals("encoding")) {
				if(noteValuelc.equals("gz")) noteValuelc="gzip";
				fi.encoding=noteValuelc;
			}	
		}
		
		if(fi.spaceDims>0){
			// If the nrrd contained spaceDirections, then use them
			spatialCal=fi.updateCalibration(spatialCal);
		} else {
			// ... else use the nrrd pixel spacings if available
			
			// Fix axis mins, converting them to pixels
			// if clause is to guard against cases where there is no spatial
			// calibration info leading to Inf
			if(spatialCal.pixelWidth!=0) spatialCal.xOrigin=spatialCal.xOrigin/spatialCal.pixelWidth;
			if(spatialCal.pixelHeight!=0) spatialCal.yOrigin=spatialCal.yOrigin/spatialCal.pixelHeight;
			if(spatialCal.pixelDepth!=0) spatialCal.zOrigin=spatialCal.zOrigin/spatialCal.pixelDepth;
	
			// Axis min will be the centre of the first pixel if this a "cell" nrrd
			// or at the (top, front, left) if this is a "node" nrrd.
			// ImageJ works on a node basis - that is it treats each voxel as a
			// cube located at its top left corner (or more accurately I think the 
			// corner closer to the coordinate origin); however the image extent
			// displayed is the "bounds" ie spacing*n
			// So to convert a cell based nrrd to a node based ImagePlus, need to
			// shift origin by 1/2 voxel dims in each dimension
			// Since the nrrd specified origin would have been the centre of the
			// voxel we need to SUBTRACT 1/2 voxel dims for ImageJ 
			// See http://teem.sourceforge.net/nrrd/format.html#centers
	
			if(fi.centers!=null) {
				if(fi.centers[0].equals("cell")) spatialCal.xOrigin-=spatialCal.pixelWidth/2;
				if(fi.centers[1].equals("cell")) spatialCal.yOrigin-=spatialCal.pixelHeight/2;
				if(fi.dimension>2 && fi.centers[2].equals("cell")) spatialCal.zOrigin-=spatialCal.pixelDepth/2;
			}
		}
		if(!detachedHeader) fi.longOffset = input.getFilePointer();
		input.close();
		this.setCalibration(spatialCal);
		return (fi);
	}

	// This gets a space delimited field from a nrrd string
	// of the form
	// a long name: space delimited values
	// but note only works with Java >=1.4 Ithink


	String getFieldPart(String str, int fieldIndex) {
		str=str.trim(); // trim the string
		String[] fieldParts=str.split(":\\s+");
		if(fieldParts.length<2) return(fieldParts[0]);
		//IJ.log("field = "+fieldParts[0]+"; value = "+fieldParts[1]+"; fieldIndex = "+fieldIndex);

		if(fieldIndex==0) return fieldParts[0];
		else return fieldParts[1];
	}
	String getSubField(String str, int fieldIndex) {
		String fieldDescriptor=getFieldPart(str,1);
		fieldDescriptor=fieldDescriptor.trim(); // trim the string

		if (IJ.debugMode) IJ.log("fieldDescriptor = "+fieldDescriptor+"; fieldIndex = "+fieldIndex);

		String[] fields_values=fieldDescriptor.split("\\s+");

		if (fieldIndex>=fields_values.length) {
			return "";
		} else {
			String rval=fields_values[fieldIndex];
			if(rval.startsWith("\"")) rval=rval.substring(1);
			if(rval.endsWith("\"")) rval=rval.substring(0, rval.length()-1);
			return rval;
		}
	}
	
	double[] getVector(String str, int vecIndex){
		String fieldDescriptor=getFieldPart(str,1);
		fieldDescriptor=fieldDescriptor.trim(); // trim the string

		if (IJ.debugMode) IJ.log("fieldDescriptor = "+fieldDescriptor);

		fieldDescriptor.replace("none", "(none)");
		String[] fields_values=fieldDescriptor.split("\\)\\s*\\(");
		
		if (vecIndex>=fields_values.length) return null;
		
		String svec=fields_values[vecIndex];
		svec=svec.trim();
		svec=svec.replaceAll("[()]", "");
		if(svec.equals("") || svec.equals("none")) return null;
		String[] svals = svec.split("\\s*,\\s*");
		double[] rvals = new double[svals.length];  
		for(int i=0;i<svals.length;i++){
			rvals[i]=(new Double(svals[i])).doubleValue();
		}
		return rvals;	
	}

}
