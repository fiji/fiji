package io;

/*
A PDF viewer plugin for ImageJ(C), using the jpedal and JAI libraries.
Copyright (C) 2005 Albert Cardona.
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 

You may contact Albert Cardona at acardona at ini phys ethz ch
*/
import ij.plugin.PlugIn;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.Menus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.process.ColorProcessor;
import org.jpedal.PdfDecoder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;



/* Open one or all pages of a PDF file as an image or a stack of images.
 *
 * When the run method is called with a null parameter, then two dialogs pop up:
 *  - to choose the file to open
 *  - to set the parameters: a page or zero for all pages,
 *                           and the scale at which to generate the images
 *
 * Call the static method 'open' directly from your plugin.
 *
 */
public class PDF_Viewer extends ImagePlus implements PlugIn {

	int scaling = 1; // 1 is 100%
	int page = 0;
	
	public void run(String arg) {
		String path = getPath(arg);
		if (null == path) return;
		int scale = 1,
		    page = 0; // zero means all pages
		if (null == arg || 0 == arg.trim().length()) {
			// user-opened from menus. Ask for params
			GenericDialog gd = new GenericDialog("Options");
			final String[] scales = new String[]{"100","200","300","400","500","600","700", "800","900","1000"};
			gd.addChoice("Scale: ", scales, scales[0]);
			gd.addNumericField("Page (0 for all): ", 0, 0);
			gd.showDialog();
			if (gd.wasCanceled()) return;

			scale = gd.getNextChoiceIndex() + 1;
			page = (int)gd.getNextNumber();
			if (page < 0) page = 0;
		}
		ImagePlus imp = this.open(path, page, scale);
		if (null == imp) return;

		// Integrate data into this ImagePlus
		if (null != imp.getStack()) {
			this.setStack(imp.getTitle(), imp.getStack());
		} else {
			this.setTitle(imp.getTitle());
		}
		Object obinfo = imp.getProperty("Info");
		if (null != obinfo) this.setProperty("Info", obinfo);
		this.setFileInfo(imp.getOriginalFileInfo());


		if (null == arg || 0 == arg.trim().length()) this.show(); // was opened by direct call to the plugin
					      // not via HandleExtraFileTypes which would
					      // have given a non-null arg.
	}

	/** Accepts URLs as well. */
	static String getPath(String arg) {
		if (null != arg) {
			if (0 == arg.indexOf("http://")
			 || new File(arg).exists()) return arg;
		}
		// else, ask:
		OpenDialog od = new OpenDialog("Choose a PDF file", null);
		String dir = od.getDirectory();
		if (null == dir) return null; // dialog was canceled
		String filename = od.getFileName();
		if (!filename.toLowerCase().endsWith(".pdf")) {
			IJ.log("Not a PDF file: " + arg);
			return null;
		}
		dir = dir.replace('\\', '/'); // Windows safe
		if (!dir.endsWith("/")) dir += "/";
		return dir + filename;
	}

	/**
	 * @param path The .pdf file path or http URL.
	 * @param page ranges from 0 (all pages) to any index (starting at 1) of a page.
	 * @param scale ranges from 1 (100%) to infinite, according to your RAM capabilities. */
	static public ImagePlus open(final String path, int page, int scale) {
		if (page < 0) {
			IJ.log("Can't open negative page number " + page);
			return null;
		}
		if (scale < 1) {
			IJ.log("Can't use a scale smaller than 1 (100%).");
			return null;
		}
		// open the PDF
		PdfDecoder decoder = null;
		try {
			decoder = new PdfDecoder();
			decoder.setDefaultDisplayFont("SansSerif");
			if (path.startsWith("http://")) decoder.openPdfFileFromURL(path);
			else decoder.openPdfFile(path);
			decoder.setPageParameters(scale, 1);
			String msg = decoder.getPageFailureMessage();
			if (null != msg && !msg.equals("")) {
				IJ.log(msg);
			}
			int n_pages = decoder.getPageCount();
			if (0 == n_pages) {
				IJ.log("PDF file has zero pages.");
				return null;
			}
			if (page > n_pages) {
				IJ.log("Can't open page " + page + ": There are only " + n_pages);
				return null;
			}
			if (0 == page) {
				// Open all pages
				// get first page
				BufferedImage bi_first = decoder.getPageAsImage(1);
				int width = bi_first.getWidth();
				int height = bi_first.getHeight();
				msg = decoder.getPageFailureMessage();
				if (null != msg && !msg.equals("")) {
					IJ.log(msg);
				}
				if (null == bi_first) {
					IJ.log("PDF Viewer: Can't read first page.");
					return null;
				}
				ImageStack stack = new ImageStack(width, height);
				stack.addSlice("1", new ColorProcessor(bi_first));
				bi_first.flush();
				// get rest of pages
				for (int i=2; i<=n_pages; i++) {
					BufferedImage bi = decoder.getPageAsImage(i);
					ColorProcessor cp = null;
					if (bi.getWidth() == width && bi.getHeight() == height) {
						cp = new ColorProcessor(bi);
					} else {
						ColorProcessor cp2 = new ColorProcessor(bi);
						cp2 = (ColorProcessor)cp2.resize(width, cp2.getHeight() * width / cp2.getWidth());
						cp = new ColorProcessor(width, height);
						cp.insert(cp2, 0, 0);
					}
					stack.addSlice(Integer.toString(i+1), cp);
					bi.flush();
				}
				return new ImagePlus(new File(path).getName(), stack);
			} else {
				// Open only the give page
				BufferedImage bi = decoder.getPageAsImage(page);
				return new ImagePlus(new File(path).getName(), bi);
			}
		} catch (Exception e) {
			IJ.log("Error: " + e);
			e.printStackTrace();
		} finally {
			if (null != decoder) {
				// release all memory
				decoder.flushObjectValues(true);
				decoder.closePdfFile();
			}
		}
		return null;
	}
}
