// saves the active image as PDF using iText library
// author: J Mutterer and U Dittmer
// TF 2008: Changes
// - Makes 'resize to fit' aware of the option to print the name/size of image so that 'save one image per page' is respected with large images
package io;

import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.*;
import ij.plugin.*;

public class PDF_Writer implements PlugIn {

	static String PREF_KEY = "PDF_Writer.";
	static boolean canUsePrefs = false;

	static boolean showName=true,			// show the name of the image
		       showSize=true,			// show the size in pixels of the image
		       scaleToFit=true,		// scale proportionately to max. page width/heigth
		       saveAllImages=false,	// save all images or just the frontmost one
		       includeAllSlices=false,	// save all slices in the stacks
		       singleImage=false,		// save one image per page or as many as possible
		       isLetter=true;			// output format is US Letter or A4
	int spcNm=0;                            // space to be reduced from image to fit the name   // will change to 30 if singleImage=true
	int spcSz=0;                            // space to be reduced from image to fit the size    // not tested with US letter size

	public PDF_Writer() {
		// This following trickery is necessary to outsmart the Java compiler.
		// Since ImageJ.VERSION is final, its value would normally be inserted
		// into the code, so we need to get it dynamically via reflection.
		try {
			Class<?> ImageJClass = Class.forName("ij.ImageJ");
			String vers = (String) ImageJClass.getField("VERSION").get(null);
			canUsePrefs = (vers.compareTo("1.32c") >= 0);
		} catch (Exception ex) { }

		if (canUsePrefs) {
			showName = Prefs.get(PREF_KEY+"showName", true);
			showSize = Prefs.get(PREF_KEY+"showSize", true);
			scaleToFit = Prefs.get(PREF_KEY+"scaleToFit", true);
			saveAllImages = Prefs.get(PREF_KEY+"saveAllImages", false);
			includeAllSlices = Prefs.get(PREF_KEY+"includeAllSlices", false);
			singleImage = Prefs.get(PREF_KEY+"singleImage", false);
			isLetter = Prefs.get(PREF_KEY+"isLetter", true);
		}
	}

	boolean isFirst;
	PdfWriter writer;
	Document document;
	Paragraph paragraph;
	Image image;

	protected void addImage(java.awt.Image awtImage, String printName)
			throws DocumentException, IOException {
		if (! isFirst) {
			if (singleImage) {
				document.newPage();
			} else {
				document.add(new Paragraph("\n"));
				float vertPos = writer.getVerticalPosition(true);
				PdfContentByte cb = writer.getDirectContent();
				cb.setLineWidth(1f);
				if (isLetter) {
					cb.moveTo(PageSize.LETTER.getLeft(50), vertPos);
					cb.lineTo(PageSize.LETTER.getRight(50), vertPos);
				} else {
					cb.moveTo(PageSize.A4.getLeft(50), vertPos);
					cb.lineTo(PageSize.A4.getRight(50), vertPos);
				}
				cb.stroke();
			}
		}

		if (showName) {
			paragraph = new Paragraph(printName);
			paragraph.setAlignment(Element.ALIGN_CENTER);
			document.add(paragraph);
			//spcNm = 40;
		}

		if (showSize) {
			paragraph = new Paragraph(awtImage.getWidth(null)+" x "+ awtImage.getHeight(null));
			paragraph.setAlignment(Element.ALIGN_CENTER);
			document.add(paragraph);
			//spcSz = 40;
		}

		if (singleImage) {
			if (showName) spcNm = 40;
			if (showSize) spcSz = 40;
		}

		image = Image.getInstance(awtImage, null);
		//				if (scaleToFit && (awtImage.getWidth(null) > 520) || (awtImage.getHeight(null) > 720))
		if (scaleToFit) {
			if (isLetter)
				image.scaleToFit(PageSize.LETTER.getRight(50+spcNm+spcSz), PageSize.LETTER.getTop(50+spcNm+spcSz));
			else
				image.scaleToFit(PageSize.A4.getRight(50+spcNm+spcSz), PageSize.A4.getTop(50+spcNm+spcSz));
		}
		image.setAlignment(Element.ALIGN_CENTER);
		document.add(image);

		isFirst = false;
	}

	public void run (String arg) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.showStatus("No image is open");
			return;
		}

		GenericDialog gd = new GenericDialog("PDF Writer");
		gd.addCheckbox("Show image name", showName);
		gd.addCheckbox("Show image size", showSize);
		gd.addCheckbox("Scale to fit", scaleToFit);
		gd.addCheckbox("Save all images", saveAllImages);
		gd.addCheckbox("Include all slices", includeAllSlices);
		gd.addCheckbox("One image per page", singleImage);
		gd.addCheckbox("US Letter", isLetter);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		showName = gd.getNextBoolean();
		showSize = gd.getNextBoolean();
		scaleToFit = gd.getNextBoolean();
		saveAllImages = gd.getNextBoolean();
		includeAllSlices = gd.getNextBoolean();
		singleImage = gd.getNextBoolean();
		isLetter = gd.getNextBoolean();

		if (canUsePrefs) {
			Prefs.set(PREF_KEY+"showName", showName);
			Prefs.set(PREF_KEY+"showSize", showSize);
			Prefs.set(PREF_KEY+"scaleToFit", scaleToFit);
			Prefs.set(PREF_KEY+"saveAllImages", saveAllImages);
			Prefs.set(PREF_KEY+"includeAllSlices", includeAllSlices);
			Prefs.set(PREF_KEY+"singleImage", singleImage);
			Prefs.set(PREF_KEY+"isLetter", isLetter);
		}

		String name = IJ.getImage().getTitle();
		SaveDialog sd = new SaveDialog("Save as PDF", name, ".pdf");
		name = sd.getFileName();
		String directory = sd.getDirectory();
		String path = directory+name;
		document = new Document(isLetter ? PageSize.LETTER : PageSize.A4);
		document.addCreationDate();
		document.addTitle(name);

		try {
			writer = PdfWriter.getInstance(document, new FileOutputStream(path));
			document.open();

			for (int idx=1; idx<=WindowManager.getWindowCount(); idx++) {
				String printName;

				if (saveAllImages) {
					image = WindowManager.getImage(idx);
					printName = WindowManager.getImage(idx).getTitle();
				} else
					printName = name;

				if (includeAllSlices) {
					ImageStack stack = image.getStack();
					int total = stack.getSize();
					for (int i = 1; i <= total; i++)
						addImage(stack.getProcessor(i)
							.createImage(),
							printName + " (" + i
							+ "/" + total + ")");
				}
				else
					addImage(image.getImage(), printName);

				if (! saveAllImages)
					break;
			}
		} catch(DocumentException de) {
			IJ.showMessage("PDF Writer", de.getMessage());
		} catch(IOException ioe) {
			IJ.showMessage("PDF Writer", ioe.getMessage());
		}
		document.close();
		IJ.showStatus("");
	}
}

