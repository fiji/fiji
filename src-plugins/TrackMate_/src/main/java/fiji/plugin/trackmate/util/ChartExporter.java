package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JFrame;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;


/**
 * A collection of static utilities made to export {@link JFreeChart} charts 
 * to various scalable file format.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jul 20, 2011
 */
public class ChartExporter {

	/**
	 * Exports a JFreeChart to a SVG file.
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param svgFile the output file.
	 * @throws IOException if writing the svgFile fails.
	 */
	public static void exportChartAsSVG(JFreeChart chart, Rectangle bounds, File svgFile) throws IOException {
		// Get a DOMImplementation and create an XML document
		org.w3c.dom.DOMImplementation domImpl =	GenericDOMImplementation.getDOMImplementation();
		org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// draw the chart in the SVG generator
		chart.draw(svgGenerator, bounds);

		// Write svg file
		OutputStream outputStream = new FileOutputStream(svgFile);
		Writer out = new OutputStreamWriter(outputStream, "UTF-8");
		svgGenerator.stream(out, true /* use css */);						
		outputStream.flush();
		outputStream.close();
	}

	/**
	 * Exports a JFreeChart to a PDF file.
	 * <p>
	 * We use a dirty hack for that: we first export to a physical SVG file, reload it, and
	 * use Apache FOP PDF transcoder to convert it to pdfs. It only works partially, for
	 * the text ends up in not being selectable in the pdf.
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param pdfFile the output file.
	 * @throws IOException if writing the pdfFile fails.
	 * @throws DocumentException  
	 */
	public static void exportChartAsPDF(JFreeChart chart, Rectangle bounds, File pdfFile) throws IOException, DocumentException {
		// step 1
		com.itextpdf.text.Rectangle pageSize = new com.itextpdf.text.Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(pageSize);
        // step 2
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        // step 3
        document.open();
        // step 4
        PdfContentByte canvas = writer.getDirectContent();
        Graphics2D g2 = canvas.createGraphics(pageSize.getWidth(), pageSize.getHeight());
        chart.draw(g2, bounds);
        g2.dispose();
        // step 5
        document.close();

	}


	private static Object[] createDummyChart() {
		// Collect data
		int nPoints = 200;
		final double[][] data = new double[2][nPoints];

		int index = 0;
		for (int i = 0; i<nPoints; i++) {
			data[0][index] = Math.random() * 100;
			data[1][index] = Math.random() * 10;
			index++;
		}

		// Plot data
		String xAxisLabel = "Time (s)";
		String yAxisLabel = "N spots";
		String title = "Nspots vs Time for something.";
		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Nspots", data);

		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getTitle().setFont(FONT);
		chart.getLegend().setItemFont(SMALL_FONT);

		// The plot
		XYPlot plot = chart.getXYPlot();
		//				plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont(FONT);
		plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		plot.getDomainAxis().setLabelFont(FONT);
		plot.getDomainAxis().setTickLabelFont(SMALL_FONT);

		ExportableChartPanel panel = new ExportableChartPanel(chart);

		JFrame frame = new JFrame(title);
		frame.setSize(500, 270);
		frame.getContentPane().add(panel);
		frame.setVisible(true);

		Object[] out = new Object[2];
		out[0] = chart;
		out[1] = panel;

		return out;
	}

	public static void main(String[] args) throws IOException, DocumentException {
		Object[] stuff = createDummyChart();
		ChartPanel panel = (ChartPanel) stuff[1];
		JFreeChart chart = (JFreeChart) stuff[0];
		ChartExporter.exportChartAsPDF(chart, panel.getBounds(), new File("/Users/tinevez/Desktop/ExportTest.pdf"));
	}

}
