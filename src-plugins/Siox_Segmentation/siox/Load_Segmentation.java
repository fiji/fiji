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

	/** SIOX segmentation info */
	SegmentationInfo sioxInfo = null;
	/** SIOX segmentator */
	private SioxSegmentator siox = null; 
	
	public Load_Segmentation()
	{
		
	}
	
	/**
	 * Constructor 
	 * 
	 * @param img image to be segmented
	 * @param filename name of the SIOX info file
	 */
	public Load_Segmentation(ImagePlus img, String filename)
	{
		this.sioxInfo = readFileInfo(filename);
	}
	
	/**
	 * Read segmentation information from file
	 * @param filename name of the segmentation info file
	 * @return segmentation information object
	 */
	private SegmentationInfo readFileInfo(String filename) 
	{
		// Read file
		FileInputStream fis = null;
		ObjectInputStream in = null;
		SegmentationInfo sioxInfo = null;
		try
		{
			fis = new FileInputStream(filename);
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
		
		return sioxInfo;
	}

	/**
	 * Plugin run method
	 */
	public void run(String args) 
	{
		ImagePlus imRef = IJ.getImage();
		
		// Chek if the file name has been set
		if(null == this.siox)
			if(!setup())
				return;
		
		ImagePlus out = execute(imRef);
		
		if(null != out)
			out.show();
			
	}
	
	/**
	 * Plugin setup (ask for the SIOX info file)
	 * 
	 * @return false if the user cancel the dialog
	 */
	public boolean setup()
	{
		GenericDialogPlus gd = new GenericDialogPlus("Load SIOX segmentator");
		
		gd.addFileField("SIOX segmentator file:", "", 50);
		
		gd.showDialog();
		
		// Exit when canceled
		if (gd.wasCanceled()) 
			return false;
		
		String segmentatorFileName = gd.getNextString();
		
		this.sioxInfo = readFileInfo(segmentatorFileName);		
		
		return true;
	}
	
	/**
	 * Execute SIOX segmentator
	 * 
	 * @return segmentation result image
	 */
	public ImagePlus execute(final ImagePlus imRef)
	{
		synchronized (this) {
			if (null == this.siox)
				// Create SIOX segmentator out of the info
				this.siox = new SioxSegmentator(imRef.getWidth(), imRef.getHeight(), null, sioxInfo.getBgSignature(), sioxInfo.getFgSignature());
		}
		
		final int w = imRef.getWidth();
		final int h = imRef.getHeight();
		
		// Output image
		ImageStack outputStack = new ImageStack(w, h, null);
		
		// Confidence matrix
		FloatProcessor confMatrix = new FloatProcessor(w, h);
		final float[] confMatrixArray = (float[])confMatrix.getPixels();
		
		final int size = imRef.getImageStack().getSize();
		
		// Iterate on all images in the stack
		for(int i = 0 ; i < size; i++)
		{
			IJ.showStatus("Segmenting image " + (i+1) + "/" + size);
			IJ.showProgress((double) (i+1) / size);
			
			Arrays.fill(confMatrixArray, SioxSegmentator.UNKNOWN_REGION_CONFIDENCE);
			boolean success = false;
			
			ColorProcessor image = (ColorProcessor) imRef.getImageStack().getProcessor(i+1);
			
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
		
		IJ.showStatus("Segmentation done!");
		IJ.showProgress(1.0);
		
		// Display result
		return new ImagePlus("Segmented stack", outputStack);
	}
	
}
