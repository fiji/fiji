package siox;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import org.siox.SioxSegmentator;

import fiji.util.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;

public class Load_Segmentation implements PlugIn
{

	/** working image plus */
	private ImagePlus imRef;
	
	/**
	 * Plugin run method
	 */
	public void run(String args) 
	{
		this.imRef = IJ.getImage();
		
		GenericDialogPlus gd = new GenericDialogPlus("Load SIOX segmentator");
		
		gd.addFileField("SIOX segmentator file:", "", 50);
		
		gd.showDialog();
		
		// Exit when canceled
		if (gd.wasCanceled()) 
			return;
		
		String segmentatorFileName = gd.getNextString();
		
		FileInputStream fis = null;
		ObjectInputStream in = null;
		SegmentationInfo sioxInfo = null;
		try
		{
			fis = new FileInputStream(segmentatorFileName);
			in = new ObjectInputStream(fis);
			sioxInfo = (SegmentationInfo) in.readObject();
			in.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		
		final int w = this.imRef.getWidth();
		final int h = this.imRef.getHeight();
		
		// Create siox segmentator out of the info
		final SioxSegmentator siox = new SioxSegmentator(w, h, null, sioxInfo.getBgSignature(), sioxInfo.getFgSignature());
		
		// Output image
		ImageStack outputStack = new ImageStack(w, h, null);
		
		// Confidence matrix
		FloatProcessor confMatrix = new FloatProcessor(w, h);
		final float[] confMatrixArray = (float[])confMatrix.getPixels();
		
		// Iterate on all images in the stack
		for(int i = 0 ; i < this.imRef.getImageStack().getSize(); i++)
		{
			Arrays.fill(confMatrixArray, SioxSegmentator.UNKNOWN_REGION_CONFIDENCE);
			boolean success = false;
			
			ColorProcessor image = (ColorProcessor) this.imRef.getImageStack().getProcessor(i+1);
			
			try{
				success = siox.applyPrecomputedSignatures(
						(int[]) image.getPixels(), confMatrixArray, 
						sioxInfo.getSmoothness(), sioxInfo.getSizeFactorToKeep());
			}catch(IllegalStateException ex){
				IJ.error("Siox Segmentation", "ERROR: foreground signature does not exist.");
			}
			
			if(!success)		
				IJ.error("Siox Segmentation", "The segmentation failed on image "+ i+1 + "!");
			
			final ByteProcessor result = (ByteProcessor) confMatrix.convertToByte(false);
			result.multiply(255);
			// Set background color based on the Process > Binary > Options 
			if(!Prefs.blackBackground)
				result.invert();
			
			// Add binary image to output stack
			outputStack.addSlice(imRef.getImageStack().getSliceLabel(i+1), result.duplicate());
		}
		
		// Display result
		new ImagePlus("Segmented stack", outputStack).show();
	}
	
	
}
