package results;

import gadgets.DataContainer;
import ij.IJ;
import ij.ImagePlus;
import ij.io.SaveDialog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.LongType;
import algorithms.Histogram2D;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class PDFWriter<T extends RealType<T>> implements ResultHandler<T> {

	// indicates if we want to produce US letter or A4 size
	boolean isLetter = false;
	// indicates if the content is the first item on the page
	boolean isFirst  = true;
	// show the name of the image
	static boolean showName=true;
	// show the size in pixels of the image
    static boolean showSize=true;
	// a reference to the data container
	DataContainer<T> container;
	PdfWriter writer;
	Document document;

	// a list of the available result images, no matter what specific kinds
	protected List<com.lowagie.text.Image> listOfPDFImages
		= new ArrayList<com.lowagie.text.Image>();
	protected List<Paragraph> listOfPDFTexts
		= new ArrayList<Paragraph>();

	public PDFWriter(DataContainer<T> container) {
		this.container = container;
	}

	public void handleImage(Image<T> image) {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( image );

		// set the display range
		double max = ImageStatistics.getImageMax(image).getRealDouble();
		imp.setDisplayRange(0.0, max);
		addImageToList(imp, image.getName());
	}

	/**
	 * Handles a histogram the following way: create snapshot, log data, reset the
	 * display range, apply the Fire LUT and finally store it as an iText PDF image.
	 * Afterwards the image is reset to its orignal state again
	 */
	public void handleHistogram(Histogram2D<T> histogram) {
		Image<LongType> image = histogram.getPlotImage();
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( image );
		// make a snapshot to be able to reset after modifications
		imp.getProcessor().snapshot();
		imp.getProcessor().log();
		imp.updateAndDraw();
		imp.getProcessor().resetMinAndMax();
		IJ.run(imp,"Fire", null);
		addImageToList(imp, image.getName());
		// reset the imp from the log scaling we applied earlier
		imp.getProcessor().reset();
	}

	protected void addImageToList(ImagePlus imp, String name) {
		java.awt.Image awtImage = imp.getImage();
		try {
			com.lowagie.text.Image pdfImage = com.lowagie.text.Image.getInstance(awtImage, null);
			pdfImage.setMarkupAttribute("name", name);
			listOfPDFImages.add(pdfImage);
		}
		catch (BadElementException e) {
			IJ.log("Could not convert image to correct format for PDF generation");
			IJ.handleException(e);
		}
		catch (IOException e) {
			IJ.log("Could not convert image to correct format for PDF generation");
			IJ.handleException(e);
		}
	}

	public void handleWarning(Warning warning) {
		listOfPDFTexts.add(new Paragraph("Warning! " + warning.getShortMessage() + " - " + warning.getLongMessage()));
	}

	public void handleValue(String name, double value) {
		handleValue(name, value, 3);
	}

	public void handleValue(String name, double value, int decimals) {
		listOfPDFTexts.add(new Paragraph(name + ": " + IJ.d2s(value, decimals)));
	}

	/**
	 * Prints an image into the opened PDF.
	 * @param img The image to print.
	 * @param printName The name to print under the image.
	 */
	protected void addImage(com.lowagie.text.Image image)
			throws DocumentException, IOException {

		if (! isFirst) {
			document.add(new Paragraph("\n"));
			float vertPos = writer.getVerticalPosition(true);
			if (vertPos - document.bottom() < image.height()) {
				document.newPage();
			} else {
				PdfContentByte cb = writer.getDirectContent();
				cb.setLineWidth(1f);
				if (isLetter) {
					cb.moveTo(PageSize.LETTER.left(50), vertPos);
					cb.lineTo(PageSize.LETTER.right(50), vertPos);
				} else {
					cb.moveTo(PageSize.A4.left(50), vertPos);
					cb.lineTo(PageSize.A4.right(50), vertPos);
				}
				cb.stroke();
			}
		}

		if (showName) {
			Paragraph paragraph = new Paragraph(image.getMarkupAttribute("name"));
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcNm = 40;
		}

		if (showSize) {
			Paragraph paragraph = new Paragraph(image.width() + " x " + image.height());
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcSz = 40;
		}

		image.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
		document.add(image);
		isFirst = false;
	}

	public void process() {
		try {
			// produce default name
			String nameCh1 = container.getSourceImage1().getName();
			String nameCh2 = container.getSourceImage2().getName();
			String name =  "coloc_" + nameCh1 + "_" + nameCh2;
			// get the path to the file we are about to create
			SaveDialog sd = new SaveDialog("Save as PDF", name, ".pdf");
			name = sd.getFileName();
			String directory = sd.getDirectory();
			// make sure we got what we need
			if ((name == null) || (directory == null)) {
				return;
			}
			String path = directory+name;
			// create a new iText Document and add date and title
			document = new Document(isLetter ? PageSize.LETTER : PageSize.A4);
			document.addCreationDate();
			document.addTitle(name);
			// get a writer object to do the actual output
			writer = PdfWriter.getInstance(document, new FileOutputStream(path));
			document.open();
			// iterate over all produced images
			for (com.lowagie.text.Image img : listOfPDFImages) {
				addImage(img);
			}
			//iterate over all produced text objects
			for (Paragraph p : listOfPDFTexts) {
				document.add(p);
			}
		} catch(DocumentException de) {
			IJ.showMessage("PDF Writer", de.getMessage());
		} catch(IOException ioe) {
			IJ.showMessage("PDF Writer", ioe.getMessage());
		}
		finally {
			if (document !=null)
				document.close();
		}
	}
}
