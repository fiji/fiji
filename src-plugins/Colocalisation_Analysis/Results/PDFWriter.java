import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.SaveDialog;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

public class PDFWriter<T extends RealType<T>> extends ResultsCollector<T> {

	// indicates if we want to produce US letter or A4 size
	boolean isLetter = false;
	// indicates if the content is the first item on the page
	boolean isFirst  = true;
	// show the name of the image
	static boolean showName=true;
	// show the size in pixels of the image
    static boolean showSize=true;
	// a reference to the data container
	DataContainer container;
	PdfWriter writer;
	Document document;
	Paragraph paragraph;

	public PDFWriter(DataContainer container) {
		this.container = container;
	}

	/**
	 * Prints an image into the opened PDF.
	 * @param img The image to print.
	 * @param printName The name to print under the image.
	 */
	protected void addImage(Image<?> img, String printName)
			throws DocumentException, IOException {
		ImagePlus imp = ImageJFunctions.displayAsVirtualStack( img );

		boolean logScaleDisplayed = mapOf2DHistograms.containsKey(img);

		if (logScaleDisplayed) {
			imp.getProcessor().snapshot();
			imp.getProcessor().log();
			IJ.resetMinAndMax();
			imp.updateAndDraw();
			IJ.run(imp,"Fire", null);
		}

		// set the display range
		double max = ImageStatistics.getImageMax((Image<T>) img).getRealDouble();
		imp.setDisplayRange(0.0, max);

		java.awt.Image awtImage = imp.getImage();
		if (! isFirst) {
			document.add(new Paragraph("\n"));
			float vertPos = writer.getVerticalPosition(true);
			if (vertPos - document.bottom() < imp.getHeight()) {
				document.newPage();
			}
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

		if (showName) {
			paragraph = new Paragraph(printName);
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcNm = 40;
		}

		if (showSize) {
			paragraph = new Paragraph(awtImage.getWidth(null)+" x "+ awtImage.getHeight(null));
			paragraph.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(paragraph);
			//spcSz = 40;
		}

		com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(awtImage, null);
		image.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
		document.add(image);
		isFirst = false;

		if (logScaleDisplayed) {
			// reset the imp from the log scaling we applied earlier
			imp.getProcessor().reset();
		}
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
			String path = directory+name;
			// create a new iText Document and add date and title
			document = new Document(isLetter ? PageSize.LETTER : PageSize.A4);
			document.addCreationDate();
			document.addTitle(name);
			// get a writer object to do the actual output
			writer = PdfWriter.getInstance(document, new FileOutputStream(path));
			document.open();
			// iterate over all produced images
			for (Image<?> img : listOfImages) {
				addImage(img, img.getName());
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