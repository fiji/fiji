package org.janelia.vaa3d.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Vaa3d_Reader class is a Fiji/ImageJ plugin for loading 
 * .v3draw (uncompressed) and .v3dpbd (compressed) 3D volume images.
 *
 * Based on example at 
 * http://albert.rierol.net/imagej_programming_tutorials.html#How%20to%20integrate%20a%20new%20file%20format%20reader%20and%20writer
 * 
 * @author Christopher M. Bruns
 *
 */
public class Vaa3d_Reader extends ImagePlus implements PlugIn {
	
	/**
	 * ImageJ plugin run() method, called by HandleExtraFileTypes class.
	 */
	@Override
	public void run(String fileName) {
		// System.out.println("Running Vaa3d_Reader plugin");
		URL url = getUrl(fileName);
		if (null == url)
			return;
        if (! parse(url)) return;  
        if (null == fileName || 0 == fileName.trim().length()) 
        	this.show(); // was opened by direct call to the plugin  
                         // not via HandleExtraFileTypes which would  
                         // have given a non-null arg.  
	}

    /** 
	 * Convert the String argument to run(String), to a URL.
	 * 
	 * @return null on error.
	 */
    private URL getUrl(String arg) {
    	File file = new File(arg);
    	try {
        	if (file.exists())
        		return file.toURI().toURL(); // File.toURL() is deprecated, so use File.toURI().toURL()
    		URL url = new URL(arg);
    		return url;
    	}
    	catch (MalformedURLException exc) {}
        // else, ask:
    	// Is there a GUI? You don't know!
    	if (!Interpreter.isBatchMode()) {
	        OpenDialog od = new OpenDialog("Choose a .v3draw file", null);
	        String dir = od.getDirectory();
	        if (null == dir)
	        	return null;
	        file = new File(dir, od.getFileName());
	        try {
	        	if (file.exists())
	        		return file.toURI().toURL(); // File.toURL() is deprecated
	        }
	    	catch (MalformedURLException exc) {}
    	}
        return null;
    }  

  
    /**
     * Reads a volume image and populates this <code>ImagePlus</code>.
     * 
     * @param url points to the input .v3draw format volume image
     * @return <code>true</code> on success
     */
    private boolean parse(URL url) {  
        // Open file and read header
    	V3dRawImageStream sliceStream;
        try {
            InputStream is = url.openStream();
    		sliceStream = new V3dRawImageStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }  
        
        int width = sliceStream.getDimension(0);
        int height = sliceStream.getDimension(1);
        int n_slices = sliceStream.getDimension(2);
        int n_channels = sliceStream.getDimension(3);
        int bytesPerPixel  = sliceStream.getPixelBytes();

        // actually parse image file
        ImagePlus hyperStack = IJ.createHyperStack(
        		new File(url.getPath()).getName(), 
        		width, height, n_channels, n_slices, 1, 
        		8 * bytesPerPixel);
        int nSlicePixels = width * height;
        ImageProcessor ip;
		double max[] = new double[n_channels]; // track maximum intensity in each channel for display calibration
        for (int c = 0; c < n_channels; ++c) {
        	max[c] = -Double.MAX_VALUE;
        	hyperStack.setC(c + 1);
        	for (int z  = 0; z < n_slices; ++z) {
        		hyperStack.setZ(z + 1);
        		try {
        			sliceStream.loadNextSlice();
        	    	if (!Interpreter.isBatchMode()) {
        	    		IJ.showProgress(c*n_slices+z, n_channels*n_slices);
        	    	}
        			ByteBuffer bb = sliceStream.getCurrentSlice().getByteBuffer();
        			bb.rewind();
        			switch (bytesPerPixel) {
        			case 1:
        				ip = new ByteProcessor(width, height);
        				byte[] ar8 = new byte[nSlicePixels];
        				// assert(bb.capacity() == nSlicePixels);
        				bb.get(ar8, 0, nSlicePixels);
        				ip.setPixels(ar8);
        				break;
        			case 2:
        				ip = new ShortProcessor(width, height);
        				short[] ar16 = new short[nSlicePixels];
        				bb.asShortBuffer().get(ar16);
        				ip.setPixels(ar16);
        				break;
        			case 4:
        				ip = new FloatProcessor(width, height);
        				float[] ar32 = new float[nSlicePixels];
        				bb.asFloatBuffer().get(ar32);
        				ip.setPixels(ar32);
        				break;
        			default:
        				return false;
        			}
        			ip.resetMinAndMax();
        			if (ip.getMax() > max[c]) {
        				max[c] = ip.getMax();
        			}
        			hyperStack.setProcessor(ip);
        		} catch (IOException exc) {
        			return false;
        		}
        	}
        }
        hyperStack.setC(1);
        hyperStack.setZ(1);
    	if (!Interpreter.isBatchMode()) {
    		IJ.showProgress(1.0);
    	}
        
        // hyperStack.show(); // for testing only
        
        setImage(hyperStack);
        setTitle(hyperStack.getTitle());
        /*
        setStack(hyperStack.getImageStack(), n_channels, n_slices, 1);
        setType(hyperStack.getType());
        */

        // Adjust display range for each channel
        setCalibration(new Calibration(this));
        for (int c = 0; c < n_channels; ++c) {
        	setC(c+1);
        	if (max[c] > 0) {
        		setDisplayRange(0, max[c]);
        		continue;
        	}
        	// I guess measuring max failed.
        	if (getBitDepth() > 8) {
        		setDisplayRange(0, 4095);
        	} else {
        		setDisplayRange(0, 255);
        	}
        }
        setC(1);
        
        setOpenAsHyperStack(true); // don't interleave channel slices
        
        return true;  
    }  
}
