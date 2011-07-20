package org.jfree.chart;

import ij.IJ;

import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.apache.batik.transcoder.TranscoderException;
import org.jfree.chart.util.ChartExporter;
import org.jfree.ui.ExtensionFileFilter;

public class ExportableChartPanel extends ChartPanel {

	/*
	 * CONSTRUCTORS
	 */

	private static final long serialVersionUID = -6556930372813672992L;

	public ExportableChartPanel(JFreeChart chart) {
		super(chart);
	}

	public ExportableChartPanel(JFreeChart chart,
			boolean properties,
			boolean save,
			boolean print,
			boolean zoom,
			boolean tooltips) {
		super(chart, properties, save, print, zoom, tooltips);
	}

	public ExportableChartPanel(JFreeChart chart, int width, int height,
			int minimumDrawWidth, int minimumDrawHeight, int maximumDrawWidth,
			int maximumDrawHeight, boolean useBuffer, boolean properties,
			boolean save, boolean print, boolean zoom, boolean tooltips) {
		super(chart, width, height, minimumDrawWidth, minimumDrawHeight, 
				maximumDrawWidth, maximumDrawHeight,
				useBuffer, properties, save, print, zoom, tooltips);
	}

	public ExportableChartPanel(JFreeChart chart, int width, int height,
			int minimumDrawWidth, int minimumDrawHeight, int maximumDrawWidth,
			int maximumDrawHeight, boolean useBuffer, boolean properties,
			boolean copy, boolean save, boolean print, boolean zoom,
			boolean tooltips) {
		super(chart, width, height, minimumDrawWidth, minimumDrawHeight, 
				maximumDrawWidth, maximumDrawHeight,
				useBuffer, properties, copy, save, print, zoom, tooltips);	
	}

	/*
	 * METHODS
	 */

	 /**
	  * Opens a file chooser and gives the user an opportunity to save the chart
	  * in PNG, PDF or SVG format.
	  *
	  * @throws IOException if there is an I/O error.
	  */
	public void doSaveAs() throws IOException {
		
		File file = null;

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			Container dialogParent = getParent();
			while ( !(dialogParent instanceof Frame)) {
				dialogParent = dialogParent.getParent();
			}
			Frame frame = (Frame) dialogParent;
			
			FileDialog dialog =	new FileDialog(frame, "Export chart to PNG, PDF or SVG", FileDialog.SAVE);
			String defaultDir = null;
			if (getDefaultDirectoryForSaveAs() != null) {
				defaultDir = getDefaultDirectoryForSaveAs().getPath();
			}
			dialog.setDirectory(defaultDir);
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".png") || name.endsWith(".pdf") || name.endsWith(".svg");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			
			if (null == selectedFile)
				return;
			
			file = new File(dialog.getDirectory(), selectedFile);
			
		} else {

			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(getDefaultDirectoryForSaveAs());
			ExtensionFileFilter filter1 = new ExtensionFileFilter("PNG Image File", ".png");
			ExtensionFileFilter filter2 = new ExtensionFileFilter("Portable Document File (PDF)", ".pdf");
			ExtensionFileFilter filter3 = new ExtensionFileFilter("Scalable Vector Graphics (SVG)", ".svg");
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilter(filter1);
			fileChooser.addChoosableFileFilter(filter2);
			fileChooser.addChoosableFileFilter(filter3);

			int option = fileChooser.showSaveDialog(this);
			if (option == JFileChooser.APPROVE_OPTION) {
				file =  fileChooser.getSelectedFile();
				
			} else {
				return;
			}
		}
		if (file.getPath().endsWith(".png")) {
			ChartUtilities.saveChartAsPNG(file, getChart(), getWidth(), getHeight());
			
		} else if (file.getPath().endsWith(".pdf")) {
			try {
				ChartExporter.exportChartAsPDF(getChart(), getBounds(), file);
			} catch (TranscoderException e) {
				e.printStackTrace();
			}
			
		} else if (file.getPath().endsWith(".svg")) {
			ChartExporter.exportChartAsSVG(getChart(), getBounds(), file);
		}
	}

}
