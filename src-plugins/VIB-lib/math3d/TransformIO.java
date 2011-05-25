package math3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;

import nrrd.NrrdHeader;
import ij.IJ;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

/**
 * 
 */

/**
 * This class provides simple IO for affine transforms.
 * These are available as float[16]
 * reading row-wise along the Affine Matrix
 * See Transform3D Java Doc for details
 * http://java.sun.com/products/java-media/3D/forDevelopers/J3D_1_3_API/j3dapi/javax/media/j3d/Transform3D.html
 * @author jefferis
 *
 */
public class TransformIO {
	
	// Determines whether the transformations that are 
	// supplied are normalised before being returned
	// Should probably happen elsewhere though
	public boolean normaliseScaleFactors=false;
	
	public static final int matRows=4;
	public static final int matCols=4;
	public static final int matSize=matRows*matCols;
	
	NrrdHeader nh;
	
	String getTags() { return nh==null?null:nh.getTagStrings(); }
	String getFields() {return nh==null?null:nh.getFieldStrings();}	
	String getHeader() {return nh==null?null:nh.toString();}	

	float[] openAffineTransform(String path) {
		nh=new NrrdHeader();
		float[] mat = new float[matSize];
		try {
			nh.readHeader(path);
			LineNumberReader in=new LineNumberReader(
					new InputStreamReader(new FileInputStream(path), "UTF-8") );
			String s;
			int nLines=0;
			while((s = in.readLine()) != null){
				if(s.startsWith("#"))
					continue;
				String[] floatStrings = s.split("\\s+");
            	if(floatStrings.length!=4) throw new 
            		Exception("Could not read 4 floats from line "+in.getLineNumber()+" (" + s + ") of file "+path);
				for(int i=0;i<floatStrings.length;i++){
                    	mat[nLines*matCols+i]=s2f(floatStrings[i]);
				}
				nLines++;
			}
		} catch (Exception e){
			IJ.error("Unable to read affine transfomation from file: "+path+"\n"+e);
		}
		
		return mat;
	}
	
	public float[] openAffineTransform(){
		OpenDialog od = new OpenDialog("Open Affine Transformation...", "");
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName == null)
			return null;
		return openAffineTransform((new File(directory,fileName)).getPath());
	}
	
	public boolean saveAffineTransform(String path, float[] mat){
		File f = new File(path);
		//if (!f.canWrite()) return false;
		if (mat.length!=matSize) return false;
		
		//SimpleDateFormat regDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
		
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write("# Simple Affine Transformation written by Transform_IO\n");
			out.write("# at "+(new Date())+"\n");
			// There's no point in writing out the tags as long as we
			// are writing a simple format header
//			String tags=getTags();
//			if(tags!=null) out.write(tags+"\n");
			out.write(toString(mat));
			out.close();
		} catch (Exception e) {
			IJ.error("Unable to write transformation to file: "+f.getAbsolutePath()+"error: "+e);
			return false;
		}
		return true;
	}
	
	public String toString(float[]  mat){
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<matRows;i++){
			sb.append(mat[i*matCols]+" "+mat[i*matCols+1]+" "+mat[i*matCols+2]+" "+mat[i*matCols+3]+"\n");
		}
		return sb.toString();
	}
	
	public boolean saveAffineTransform(float[] mat){
		SaveDialog sd = new SaveDialog("Save Affine Transformation ...", "", ".mat");
		String file = sd.getFileName();
		if (file == null) return false;
		String directory = sd.getDirectory();
		return saveAffineTransform((new File(directory,file)).getPath(), mat);
	}
	
	// Converts a string to a float. Returns NAN if the string does not contain a valid number. */
	float s2f(String s) {
		Float f = null;
		try {f = new Float(s);}
		catch (NumberFormatException e) {}
		return f!=null?f.floatValue():Float.NaN;
	}
}

