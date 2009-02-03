/**
 * Copyright Albert Cardona 2008.
 * Released under the General Public License in its latest version.
 *
 * Modeled after scripts/pdf-extract-images.py by Johannes Schindelin
 */

package io;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.PdfImageData;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;
import java.io.File;
import java.io.FileInputStream;
import java.awt.image.BufferedImage;

/** Extract all images from a PDF file (or from an URL given as argument),
 *  and open them all within ImageJ in their original resolution.
*/
public class Extract_Images_From_PDF implements PlugIn {
	public void run(String arg) {

		final String path = PDF_Viewer.getPath(arg);
		if (null == path) return;
		PdfDecoder decoder = null;

		try {
			decoder = new PdfDecoder(false);
			decoder.setExtractionMode(PdfDecoder.RAWIMAGES | PdfDecoder.FINALIMAGES);
			if (path.startsWith("http://")) decoder.openPdfFileFromURL(path);
			else decoder.openPdfFile(path);

			final int page_count = decoder.getPageCount();

			for (int page=1; page<=page_count; page++) {
				IJ.showStatus("Decoding page " + page);
				decoder.decodePage(page);
				final PdfImageData images = decoder.getPdfImageData();
				final int image_count = images.getImageCount();

				for (int i=0; i<image_count; i++) {
					IJ.showStatus("Opening image " + i + "/" + image_count + " from page " + page + "/" + page_count);
					String name = images.getImageName(i);
					BufferedImage image = decoder.getObjectStore().loadStoredImage("R" + name);
					new ImagePlus(name, image).show();
				}
			}
			IJ.showStatus("Done.");
		} catch (Exception e) {
			IJ.log("Error: " + e);
			e.printStackTrace();
		} finally {
			decoder.flushObjectValues(true);
			decoder.closePdfFile();
		}
	}
}
