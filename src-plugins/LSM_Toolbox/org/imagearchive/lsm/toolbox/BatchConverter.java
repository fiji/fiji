package org.imagearchive.lsm.toolbox;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.MedianCut;
import ij.process.ShortProcessor;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.imagearchive.lsm.reader.info.CZLSMInfo;
import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;

/*******************************************************************************
 * Batch Converter Class - Adapted from Wayne Rasband's Batch Converter plug-in.
 ******************************************************************************/

public class BatchConverter {

	private MasterModel masterModel;

	public BatchConverter(MasterModel masterModel) {
		super();
		this.masterModel = masterModel;
	}

	public void convertFile(String file, String outputDir, String format,
			boolean verbose, boolean sepDir) {
		String finalDir = "";
		File f = new File(file);

		/*ImagePlus imp = new Reader(masterModel).open(f.getParent(),
				f.getName(), verbose, false);*/
		org.imagearchive.lsm.reader.Reader r = new org.imagearchive.lsm.reader.Reader();
		ImagePlus imp = r.open(f.getParent(), f.getName(), false, false);
		if (imp != null && imp.getStackSize() > 0) {
			LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
			CZLSMInfo cz = (CZLSMInfo)((ImageDirectory)lsm.imageDirectories.get(0)).TIF_CZ_LSMINFO;
			if (sepDir) {
				finalDir = outputDir + System.getProperty("file.separator")
						+ f.getName();
				File fdir = new File(finalDir);
				if (!fdir.exists())
					fdir.mkdirs();
			} else
				finalDir = outputDir;
			int position = 1;
			for (int i = 1; i <= cz.DimensionTime; i++)
				for (int j = 1; j <= cz.DimensionZ; j++)
					for (int k = 1; k <= cz.DimensionChannels; k++) {
						// imp.setPosition(k, j, i);
						// int stackPosition = imp.getCurrentSlice();
						String title = lsm.fileName + " - "
								+ cz.channelNamesAndColors.ChannelNames[k - 1]
								+ " - C" + new Integer(k).toString() + " Z"
								+ new Integer(j).toString() + " T"
								+ new Integer(i).toString();
						save(new ImagePlus(title, imp.getImageStack()
								.getProcessor(position++)), finalDir, format,
								title);
					}
		}
	}

	/***************************************************************************
	 * Provide a tab delimited "csv" file
	 * Format for each row:
	 *  LSM_FILE\tOUTPUT_DIR\tFORMAT\tVERBOSE\tCREATE_SEPARATE_DIR
	 **************************************************************************/
	public void convertBatchFile(String fileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String row = null;

			while ((row = br.readLine()) != null) {
				String[] arr = row.split("\t");
				String inputFile = arr[0];
				String outputDir = arr[1];
				String format = arr[2];
				if (arr[2] == null) format = "tiff";
				boolean verbose = false, createSepDir = false;
				if (!(arr[3].equals("0"))) verbose = true;
				if (!(arr[4].equals("0"))) createSepDir = true;
				IJ.showStatus("Conversion started");
				IJ.showStatus("Converting "+new File(inputFile).getName());
				convertFile(inputFile,outputDir,format,verbose,createSepDir);
					IJ.showStatus("Conversion done");
			}
		} catch (IOException e) {
			IJ.error("Incompatible batch file format");
			IJ.log("IOException error: " + e.getMessage());

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					IJ.log("IOException error trying to close the file: "
							+ e.getMessage());
				}
			}
		}
	}

	/***************************************************************************
	 * method : process, optional method to add some image processing before
	 * conversion
	 **************************************************************************/

	/**
	 * This is the place to add code to process each image. The image is not
	 * written if this method returns null.
	 */
	public ImagePlus process(ImagePlus imp) {
		/* No processing defined for this plugin */
		return imp;
	}

	/***************************************************************************
	 * method : save, saves the image with an appropriate file name
	 **************************************************************************/

	public void save(ImagePlus img, String dir, String format, String fileName) {
		String path = dir + System.getProperty("file.separator") + fileName;
		if (format.equals("Tiff"))
			new FileSaver(img).saveAsTiff(path + ".tif");
		else if (format.equals("8-bit Tiff"))
			saveAs8bitTiff(img, path + ".tif");
		else if (format.equals("Zip"))
			new FileSaver(img).saveAsZip(path + ".zip");
		else if (format.equals("Raw"))
			new FileSaver(img).saveAsRaw(path + ".raw");
		else if (format.equals("Jpeg"))
			new FileSaver(img).saveAsJpeg(path + ".jpg");
	}

	/***************************************************************************
	 * method : saveAs8bitTiff, image processing for 8-bit Tiff saving
	 **************************************************************************/

	void saveAs8bitTiff(ImagePlus img, String path) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor) {
			ip = reduceColors(ip);
			img.setProcessor(null, ip);
		} else if ((ip instanceof ShortProcessor)
				|| (ip instanceof FloatProcessor)) {
			ip = ip.convertToByte(true);
			img.setProcessor(null, ip);
		}
		new FileSaver(img).saveAsTiff(path);
	}

	/***************************************************************************
	 * method : reduceColors, reduces the color range for the appropriate format *
	 **************************************************************************/

	ImageProcessor reduceColors(ImageProcessor ip) {
		MedianCut mc = new MedianCut((int[]) ip.getPixels(), ip.getWidth(), ip
				.getHeight());
		Image img = mc.convert(256);
		return (new ByteProcessor(img));
	}

}
