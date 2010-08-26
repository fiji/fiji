/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.io.*;
import ij.gui.YesNoCancelDialog;

import java.awt.Dialog;
import java.awt.Insets;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Label;
import java.awt.CheckboxGroup;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.awt.event.TextEvent;
import java.awt.Button;
import java.awt.event.WindowListener;
import java.awt.event.TextListener;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.image.IndexColorModel;

import java.io.*;

import java.util.HashSet;import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

import features.SigmaPalette;
import ij.gui.GenericDialog;
import ij.gui.GUI;
import ij.measure.Calibration;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.text.DecimalFormat;

import util.FindConnectedRegions;

import org.jfree.chart.plot.PlotOrientation;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;

import org.w3c.dom.DOMImplementation;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.Document;
import org.apache.batik.svggen.SVGGraphics2D;

import org.apache.commons.math.stat.regression.SimpleRegression;

public class ShollAnalysisDialog extends Dialog implements WindowListener, ActionListener, TextListener, ItemListener {

	protected double x_start, y_start, z_start;

	protected CheckboxGroup pathsGroup = new CheckboxGroup();
	protected Checkbox useAllPathsCheckbox = new Checkbox("Use all paths in analysis?", pathsGroup, false);
	protected Checkbox useSelectedPathsCheckbox = new Checkbox("Use only selected paths in analysis?", pathsGroup, true);
	protected Button makeShollImageButton = new Button("Make Sholl image");
	protected Button drawShollGraphButton = new Button("Draw Graph");
	protected Button exportDetailAsCSVButton = new Button("Export detailed results as CSV");
	protected Button exportSummaryAsCSVButton = new Button("Export summary results as CSV");
	protected Button addToResultsTableButton = new Button("Add to Results Table");

	protected int numberOfSelectedPaths;
	protected int numberOfAllPaths;

	protected CheckboxGroup axesGroup = new CheckboxGroup();
	protected Checkbox normalAxes = new Checkbox("Use standard axes", axesGroup, true);
	protected Checkbox semiLogAxes = new Checkbox("Use semi-log axes", axesGroup, false);
	protected Checkbox logLogAxes = new Checkbox("Use log-log axes", axesGroup, false);

	protected CheckboxGroup normalizationGroup = new CheckboxGroup();
	protected Checkbox noNormalization = new Checkbox("No normalization of intersections", normalizationGroup, true);
	protected Checkbox normalizationForSphereVolume = new Checkbox("[BUG: should be set in the constructor]", normalizationGroup, false);

	protected TextField sampleSeparation = new TextField(
		""+Prefs.get("tracing.ShollAnalysisDialog.sampleSeparation",0));

	protected ShollResults currentResults;
	protected ImagePlus originalImage;

	GraphFrame graphFrame;

	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
		ShollResults results = null;
		synchronized(this) {
			results = getCurrentResults();
		}
		if( results == null ) {
			IJ.error("The sphere separation field must be a number, not '"+sampleSeparation.getText()+"'");
			return;
		}
		if( source == makeShollImageButton ) {
			results.makeShollCrossingsImagePlus(originalImage);
		} else if( source == exportDetailAsCSVButton || source == exportSummaryAsCSVButton ) {

			boolean detail = (source == exportDetailAsCSVButton);

			SaveDialog sd = new SaveDialog("Export data as...",
						       "sholl-"+(detail?"detail":"summary")+results.getSuggestedSuffix(),
						       ".csv");

			if(sd.getFileName()==null) {
				return;
			}

			File saveFile = new File( sd.getDirectory(),
						  sd.getFileName() );
			if ((saveFile!=null)&&saveFile.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Export data...", "The file "+
					    saveFile.getAbsolutePath()+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Exporting CSV data to "+saveFile.getAbsolutePath());

			try {
				if(detail)
					results.exportDetailToCSV(saveFile);
				else
					results.exportSummaryToCSV(saveFile);
			} catch( IOException ioe) {
				IJ.error("Saving to "+saveFile.getAbsolutePath()+" failed");
				return;
			}

		} else if( source == drawShollGraphButton ) {
			graphFrame.setVisible(true);
		} else if( source == addToResultsTableButton ) {
			results.addToResultsTable();
		}
	}

	public void textValueChanged( TextEvent e ) {
		Object source = e.getSource();
		if( source == sampleSeparation ) {
			String sampleSeparationText = sampleSeparation.getText();
			float s;
			try {
				s = Float.parseFloat(sampleSeparationText);
			} catch( NumberFormatException nfe ) {
				return;
			}
			if( s >= 0 )
				updateResults();
		}
	}

	protected synchronized void updateResults() {
		ShollResults results = getCurrentResults();
		resultsPanel.updateFromResults(results);
		JFreeChart chart = results.createGraph();
		if( chart == null )
			return;
		if( graphFrame == null )
			graphFrame = new GraphFrame( chart, results.getSuggestedSuffix() );
		else
			graphFrame.updateWithNewChart( chart, results.getSuggestedSuffix() );
	}

	public void itemStateChanged( ItemEvent e ) {
		updateResults();
	}

	public ShollResults getCurrentResults() {
		List<ShollPoint> pointsToUse;
		String description = "Sholl analysis ";
		String postDescription = " for "+originalImage.getTitle();
		boolean useAllPaths = ! useSelectedPathsCheckbox.getState();
		if( useAllPaths ) {
			pointsToUse = shollPointsAllPaths;
			description += "of all paths" + postDescription;
		} else {
			pointsToUse = shollPointsSelectedPaths;
			description += "of selected paths " + postDescription;
		}

		int axes = 0;
		if( normalAxes.getState() )
			axes = AXES_NORMAL;
		else if( semiLogAxes.getState() )
			axes = AXES_SEMI_LOG;
		else if( logLogAxes.getState() )
			axes = AXES_LOG_LOG;
		else
			throw new RuntimeException("BUG: somehow no axis checkbox was selected");

		int normalization = 0;
		if( noNormalization.getState() )
			normalization = NOT_NORMALIZED;
		else if( normalizationForSphereVolume.getState() )
			normalization = NORMALIZED_FOR_SPHERE_VOLUME;
		else
			throw new RuntimeException("BUG: somehow no normalization checkbox was selected");

		String sphereSeparationString = sampleSeparation.getText();
		double sphereSeparation = Double.MIN_VALUE;

		try {
			sphereSeparation = Double.parseDouble(sphereSeparationString);
		} catch( NumberFormatException nfe ) {
			return null;
		}

		ShollResults results = new ShollResults( pointsToUse,
							 originalImage,
							 useAllPaths,
							 useAllPaths ? numberOfAllPaths : numberOfSelectedPaths,
							 x_start,
							 y_start,
							 z_start,
							 description,
							 axes,
							 normalization,
							 sphereSeparation,
							 twoDimensional );

		return results;
	}

	public static class GraphFrame extends JFrame implements ActionListener {
		JButton exportButton;
		JFreeChart chart = null;
		ChartPanel chartPanel = null;
		JPanel mainPanel;
		String suggestedSuffix;
		public void updateWithNewChart( JFreeChart chart, String suggestedSuffix ) {
			updateWithNewChart( chart, suggestedSuffix, false );
		}
		synchronized public void updateWithNewChart( JFreeChart chart, String suggestedSuffix, boolean setSize ) {
			this.suggestedSuffix = suggestedSuffix;
			if( chartPanel != null )
				remove(chartPanel);
			chartPanel = null;
			this.chart = chart;
			chartPanel = new ChartPanel( chart );
			if( setSize )
				chartPanel.setPreferredSize(new java.awt.Dimension(800,600));
			mainPanel.add(chartPanel,BorderLayout.CENTER);
			validate();
		}
		public GraphFrame( JFreeChart chart, String suggestedSuffix ) {
			super();

			this.suggestedSuffix = suggestedSuffix;

			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());

			updateWithNewChart( chart, suggestedSuffix, true );

			JPanel buttonsPanel = new JPanel();
			exportButton = new JButton("Export graph as SVG");
			exportButton.addActionListener(this);
			buttonsPanel.add(exportButton);
			mainPanel.add(buttonsPanel,BorderLayout.SOUTH);

			setContentPane(mainPanel);
			validate();
			setSize(new java.awt.Dimension(500, 270));
			GUI.center(this);
		}
		public void actionPerformed( ActionEvent e ) {
			Object source = e.getSource();
			if( source == exportButton ) {
				exportGraphAsSVG();
			}
		}
		public void exportGraphAsSVG() {

			SaveDialog sd = new SaveDialog("Export graph as...",
						       "sholl"+suggestedSuffix,
						       ".svg");

			if(sd.getFileName()==null) {
				return;
			}

			File saveFile = new File( sd.getDirectory(),
						  sd.getFileName() );
			if ((saveFile!=null)&&saveFile.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Export graph...", "The file "+
					    saveFile.getAbsolutePath()+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Exporting graph to "+saveFile.getAbsolutePath());

			try {
				exportChartAsSVG( chart, chartPanel.getBounds(), saveFile );
			} catch( IOException ioe) {
				IJ.error("Saving to "+saveFile.getAbsolutePath()+" failed");
				return;
			}

		}

		/**
		 * Exports a JFreeChart to a SVG file.
		 *
		 * @param chart JFreeChart to export
		 * @param bounds the dimensions of the viewport
		 * @param svgFile the output file.
		 * @throws IOException if writing the svgFile fails.

		 * This method is taken from:
		 *    http://dolf.trieschnigg.nl/jfreechart/
		 */
		void exportChartAsSVG(JFreeChart chart, Rectangle bounds, File svgFile) throws IOException {

			// Get a DOMImplementation and create an XML document
			DOMImplementation domImpl =
				GenericDOMImplementation.getDOMImplementation();
			Document document = domImpl.createDocument(null, "svg", null);

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
	}

	public static final int AXES_NORMAL   = 1;
	public static final int AXES_SEMI_LOG = 2;
	public static final int AXES_LOG_LOG  = 3;

	public static final String [] axesParameters = { null, "normal", "semi-log", "log-log"  };

	public static final int NOT_NORMALIZED = 1;
	public static final int NORMALIZED_FOR_SPHERE_VOLUME = 2;

	public static final String [] normalizationParameters = { null, "not-normalized", "normalized" };

	public static class ShollResults {
		protected double [] squaredRangeStarts;
		protected int [] crossingsPastEach;
		protected int n;
		protected double x_start, y_start, z_start;
		/* maxCrossings is the same as the "Dendrite Maximum". */
		protected int maxCrossings = Integer.MIN_VALUE;
		protected double criticalValue = Double.MIN_VALUE;
		protected String description;
		protected int axes;
		protected int normalization;
		protected double sphereSeparation;
		protected double [] x_graph_points;
		protected double [] y_graph_points;
		protected double minY;
		protected double maxY;
		protected int graphPoints;
		protected String yAxisLabel;
		protected String xAxisLabel;
		protected double regressionGradient = Double.MIN_VALUE;
		protected double regressionIntercept = Double.MIN_VALUE;
		String parametersSuffix;
		public int getDendriteMaximum() {
			return maxCrossings;
		}
		public double getCriticalValue() {
			return criticalValue;
		}
		public double getRegressionGradient() {
			return regressionGradient;
		}
		public double getShollRegressionCoefficient() {
			return - regressionGradient;
		}
		public double getRegressionIntercept() {
			return regressionIntercept;
		}
		public double getMaxDistanceSquared() {
			return squaredRangeStarts[n-1];
		}
		public String getSuggestedSuffix() {
			return parametersSuffix;
		}
		public void addToResultsTable() {
			ResultsTable rt = Analyzer.getResultsTable();
			String [] headings = {  };
			rt.setHeading(0,  "Filename");
			rt.setHeading(1,  "AllPathsUsed");
			rt.setHeading(2,  "NumberOfPathsUsed");
			rt.setHeading(3,  "SphereSeparation");
			rt.setHeading(4,  "Normalization");
			rt.setHeading(5,  "Axes");
			rt.setHeading(6,  "CriticalValue");
			rt.setHeading(7,  "DendriteMaximum");
			rt.setHeading(8,  "ShollRegressionCoefficient");
			rt.setHeading(9,  "RegressionGradient");
			rt.setHeading(10, "RegressionIntercept");
			rt.incrementCounter();
			rt.addLabel("Filename",getOriginalFilename());
			rt.addValue("AllPathsUsed",useAllPaths?1:0);
			rt.addValue("NumberOfPathsUsed",numberOfPathsUsed);
			rt.addValue("SphereSeparation",sphereSeparation);
			rt.addValue("Normalization",normalization);
			rt.addValue("Axes",axes);
			rt.addValue("CriticalValue",getCriticalValue());
			rt.addValue("DendriteMaximum",getDendriteMaximum());
			rt.addValue("ShollRegressionCoefficient",getShollRegressionCoefficient());
			rt.addValue("RegressionGradient",getRegressionGradient());
			rt.addValue("RegressionIntercept",getRegressionIntercept());
			rt.show("Results");
		}
		boolean twoDimensional;
		ImagePlus originalImage;
		boolean useAllPaths;
		int numberOfPathsUsed;
		public ShollResults( List<ShollPoint> shollPoints,
				     ImagePlus originalImage,
				     boolean useAllPaths,
				     int numberOfPathsUsed,
				     double x_start,
				     double y_start,
				     double z_start,
				     String description,
				     int axes,
				     int normalization,
				     double sphereSeparation,
				     boolean twoDimensional ) {
			parametersSuffix = "_"+axesParameters[axes]+"_"+normalizationParameters[normalization]+"_"+sphereSeparation;
			this.originalImage = originalImage;
			this.useAllPaths = useAllPaths;
			this.numberOfPathsUsed = numberOfPathsUsed;
			this.x_start = x_start;
			this.y_start = y_start;
			this.z_start = z_start;
			this.description = description;
			this.axes = axes;
			this.normalization = normalization;
			this.sphereSeparation = sphereSeparation;
			this.twoDimensional = twoDimensional;
			Collections.sort(shollPoints);
			n = shollPoints.size();
			squaredRangeStarts = new double[n];
			crossingsPastEach = new int[n];
			int currentCrossings = 0;
			for( int i = 0; i < n; ++i ) {
				ShollPoint p = shollPoints.get(i);
				if( p.nearer )
					++ currentCrossings;
				else
					-- currentCrossings;
				squaredRangeStarts[i] = p.distanceSquared;
				crossingsPastEach[i] = currentCrossings;
				if( currentCrossings > maxCrossings ) {
					maxCrossings = currentCrossings;
					criticalValue = Math.sqrt(p.distanceSquared);
				}
				// System.out.println("Range starting at: "+Math.sqrt(p.distanceSquared)+" has crossings: "+currentCrossings);
			}
			xAxisLabel = "Distance in space from ( "+x_start+", "+y_start+", "+z_start+" )";
			yAxisLabel = "Number of intersections";
			if( sphereSeparation > 0 ) {
				graphPoints = (int)Math.ceil(Math.sqrt(getMaxDistanceSquared()) / sphereSeparation);
				x_graph_points = new double[graphPoints];
				y_graph_points = new double[graphPoints];
				for( int i = 0; i < graphPoints; ++i ) {
					double x = i * sphereSeparation;
					x_graph_points[i] = x;
					double distanceSquared = x * x;
					y_graph_points[i] = crossingsAtDistanceSquared(distanceSquared);
				}
			} else {
				graphPoints = n;
				x_graph_points = new double[n];
				y_graph_points = new double[n];
				for( int i = 0; i < graphPoints; ++i ) {
					double distanceSquared = squaredRangeStarts[i];
					double x = Math.sqrt(distanceSquared);
					x_graph_points[i] = x;
					y_graph_points[i] = crossingsAtDistanceSquared(distanceSquared);
				}
			}
			if( normalization == NORMALIZED_FOR_SPHERE_VOLUME ) {
				for( int i = 0; i < graphPoints; ++i ) {
					double x;
					if( sphereSeparation > 0 ) {
						x = x_graph_points[i];
					} else {
						double startX = x_graph_points[i];
						double endX;
						if( i < graphPoints - 1 )
							endX = x_graph_points[i+1];
						else
							endX = x_graph_points[i];
						x = (startX + endX) / 2;
					}
					double distanceSquared = x * x;
					if( twoDimensional )
						y_graph_points[i] /= (Math.PI * distanceSquared);
					else
						y_graph_points[i] /= ((4.0 * Math.PI * x * distanceSquared) / 3.0);
				}
				if( twoDimensional )
					xAxisLabel += " / area enclosed by circle";
				else
					yAxisLabel += " / volume enclosed by sphere";
			}

			SimpleRegression regression = new SimpleRegression();

			maxY = Double.MIN_VALUE;
			minY = Double.MAX_VALUE;
			for( int i = 0; i < graphPoints; ++i ) {
				double x = x_graph_points[i];
				double y = y_graph_points[i];
				double x_for_regression = x;
				double y_for_regression = y;
				if( ! (Double.isInfinite(y) || Double.isNaN(y)) ) {
					if( y > maxY )
						maxY = y;
					if( y < minY )
						minY = y;
					if( axes == AXES_SEMI_LOG ) {
						if( y <= 0 )
							continue;
						y_for_regression = Math.log(y);
					} else if( axes == AXES_LOG_LOG ) {
						if( x <= 0 || y <= 0 )
							continue;
						x_for_regression = Math.log(x);
						y_for_regression = Math.log(y);
					}
					regression.addData(x_for_regression,y_for_regression);
				}
			}
			regressionGradient = regression.getSlope();
			regressionIntercept = regression.getIntercept();

			if( maxY == Double.MIN_VALUE )
				throw new RuntimeException("[BUG] Somehow there were no valid points found");
		}

		public JFreeChart createGraph() {

			PrintWriter pw = null;
			boolean debug = true;

			XYSeriesCollection data = null;

			double minX = Double.MAX_VALUE;
			double maxX = Double.MIN_VALUE;

			try {
				if (debug)
					pw = new PrintWriter(new  FileWriter("/tmp/last-graph"));

				final XYSeries series = new XYSeries("Intersections");
				for( int i = 0; i < graphPoints; ++i ) {
					double x = x_graph_points[i];
					double y = y_graph_points[i];
					if( Double.isInfinite(y) || Double.isNaN(y) )
						continue;
					if( axes == AXES_SEMI_LOG || axes == AXES_LOG_LOG ) {
						if( y <= 0 )
							continue;
					}
					if( axes == AXES_LOG_LOG ) {
						if( x <= 0 )
							continue;
					}
					if( x < minX )
						minX = x;
					if( x > maxX )
						maxX = x;
					series.add(x,y);
					if (debug)
						pw.print(x_graph_points[i]+"\t"+y+"\n");
				}
				data = new XYSeriesCollection(series);

				if (debug)
					pw.close();

			} catch( IOException e ) {
				IJ.error("Failed to write out the graph points");
				return null;
			}

			ValueAxis xAxis = null;
			ValueAxis yAxis = null;
			if( axes == AXES_NORMAL ) {
				xAxis = new NumberAxis(xAxisLabel);
				yAxis = new NumberAxis(yAxisLabel);
			} else if( axes == AXES_SEMI_LOG ) {
				xAxis = new NumberAxis(xAxisLabel);
				yAxis = new LogAxis(yAxisLabel);
			} else if( axes == AXES_LOG_LOG ) {
				xAxis = new LogAxis(xAxisLabel);
				yAxis = new LogAxis(yAxisLabel);
			}


			xAxis.setRange(minX,maxX);
			if( axes == AXES_NORMAL )
				yAxis.setRange(0,maxY);
			else
				yAxis.setRange(minY,maxY);

			XYItemRenderer renderer = null;
			if( sphereSeparation > 0 ) {
				renderer = new XYLineAndShapeRenderer();
			} else {
				XYBarRenderer barRenderer = new XYBarRenderer();
				barRenderer.setShadowVisible(false);
				barRenderer.setGradientPaintTransformer(null);
				barRenderer.setDrawBarOutline(false);
				barRenderer.setBarPainter(new StandardXYBarPainter());
				renderer = barRenderer;
			}

			XYPlot plot = new XYPlot(
				data,
				xAxis,
				yAxis,
				renderer );

			return new JFreeChart( description, plot );
		}

		public int crossingsAtDistanceSquared( double distanceSquared ) {

			int minIndex = 0;
			int maxIndex = n - 1;

			if( distanceSquared < squaredRangeStarts[minIndex] )
				return 1;
			else if( distanceSquared > squaredRangeStarts[maxIndex] )
				return 0;

			while( maxIndex - minIndex > 1 ) {

				int midPoint = (maxIndex + minIndex) / 2;

				if( distanceSquared < squaredRangeStarts[midPoint] )
					maxIndex = midPoint;
				else
					minIndex = midPoint;
			}
			return crossingsPastEach[minIndex];
		}

		public ImagePlus makeShollCrossingsImagePlus(ImagePlus original) {
			int width = original.getWidth();
			int height = original.getHeight();
			int depth = original.getStackSize();
			Calibration c = original.getCalibration();
			double x_spacing = 1;
			double y_spacing = 1;
			double z_spacing = 1;
			if( c != null ) {
				x_spacing = c.pixelWidth;
				y_spacing = c.pixelHeight;
				z_spacing = c.pixelDepth;
			}
			ImageStack stack = new ImageStack(width,height);
			for( int z = 0; z < depth; ++z ) {
				short [] pixels = new short[width*height];
				for( int y = 0; y < height; ++y ) {
					for( int x = 0; x < width; ++x ) {
						double xdiff = x_spacing * x - x_start;
						double ydiff = y_spacing * y - y_start;
						double zdiff = z_spacing * z - z_start;
						double distanceSquared =
							xdiff * xdiff +
							ydiff * ydiff +
							zdiff * zdiff;
						pixels[y*width+x] = (short)crossingsAtDistanceSquared(distanceSquared);
					}
				}
				ShortProcessor sp = new ShortProcessor(width,height);
				sp.setPixels(pixels);
				stack.addSlice( "", sp );
			}
			ImagePlus result = new ImagePlus( description, stack );
			result.show();
			IndexColorModel icm = FindConnectedRegions.backgroundAndSpectrum(255);
			stack.setColorModel(icm);
			ImageProcessor ip = result.getProcessor();
			if( ip != null ) {
				ip.setColorModel(icm);
				ip.setMinAndMax( 0, maxCrossings );
			}
			result.updateAndDraw();

			if( c != null )
				result.setCalibration(c);
			return result;
		}

		public static void csvQuoteAndPrint(PrintWriter pw, Object o) {
			pw.print(PathAndFillManager.stringForCSV(""+o));
		}

		public String getOriginalFilename() {
			FileInfo originalFileInfo = originalImage.getOriginalFileInfo();
			if( originalFileInfo.directory == null )
				return "[unknown]";
			else
				return new File(originalFileInfo.directory,
						originalFileInfo.fileName).getAbsolutePath();


		}

		public void exportSummaryToCSV( File outputFile ) throws IOException {
			String [] headers = new String[]{ "Filename",
							  "AllPathsUsed",
							  "NumberOfPathsUsed",
							  "SphereSeparation",
							  "Normlization",
							  "Axes",
							  "CriticalValue",
							  "DendriteMaximum",
							  "ShollRegressionCoefficient",
							  "RegressionGradient",
							  "RegressionIntercept" };

			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.getAbsolutePath()),"UTF-8"));
			int columns = headers.length;
			for( int c = 0; c < columns; ++c ) {
				csvQuoteAndPrint(pw,headers[c]);
				if( c < (columns - 1) )
					pw.print(",");
			}
			pw.print("\r\n");
			csvQuoteAndPrint(pw,getOriginalFilename());
			pw.print(",");
			csvQuoteAndPrint(pw,useAllPaths);
			pw.print(",");
			csvQuoteAndPrint(pw,numberOfPathsUsed);
			pw.print(",");
			csvQuoteAndPrint(pw,sphereSeparation);
			pw.print(",");
			csvQuoteAndPrint(pw,normalizationParameters[normalization]);
			pw.print(",");
			csvQuoteAndPrint(pw,axesParameters[axes]);
			pw.print(",");
			csvQuoteAndPrint(pw,getCriticalValue());
			pw.print(",");
			csvQuoteAndPrint(pw,getDendriteMaximum());
			pw.print(",");
			csvQuoteAndPrint(pw,getShollRegressionCoefficient());
			pw.print(",");
			csvQuoteAndPrint(pw,getRegressionGradient());
			pw.print(",");
			csvQuoteAndPrint(pw,getRegressionIntercept());
			pw.print("\r\n");

			pw.close();
		}

		public void exportDetailToCSV( File outputFile ) throws IOException {
			String [] headers;
			if( sphereSeparation > 0 )
				headers = new String[]{ "Radius",
							"Crossings",
							"NormalizedCrossings" };
			else
				headers = new String []{ "StartRadius",
							 "EndRadius",
							 "Crossings",
							 "NormalizedCrossings" };

			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile.getAbsolutePath()),"UTF-8"));
			int columns = headers.length;
			for( int c = 0; c < columns; ++c ) {
				csvQuoteAndPrint(pw,headers[c]);
				if( c < (columns - 1) )
					pw.print(",");
			}
			pw.print("\r\n");
			if( sphereSeparation > 0 ) {
				int graphPoints = (int)Math.ceil(Math.sqrt(getMaxDistanceSquared()) / sphereSeparation);
				for( int i = 0; i < graphPoints; ++i ) {
					double x = i * sphereSeparation;
					double distanceSquared = x * x;
					int crossings = crossingsAtDistanceSquared(distanceSquared);
					double normalizedCrossings = - Double.MIN_VALUE;
					if( twoDimensional )
						normalizedCrossings = crossings / (Math.PI * distanceSquared);
					else
						normalizedCrossings = ((4.0 * Math.PI * x * distanceSquared) / 3.0);
					csvQuoteAndPrint(pw,x);
					pw.print(",");
					csvQuoteAndPrint(pw,crossings);
					pw.print(",");
					csvQuoteAndPrint(pw,normalizedCrossings);
					pw.print("\r\n");
				}
			} else {
				for( int i = 0; i < (n - 1); ++i ) {
					double startXSquared = squaredRangeStarts[i];
					double endXSquared = squaredRangeStarts[i+1];
					// Omit the empty ranges, since they're not likely to matter to anyone:
					if( endXSquared == startXSquared )
						continue;
					double startX = Math.sqrt(startXSquared);
					double endX = Math.sqrt(endXSquared);
					double midX = (startX + endX) / 2;
					int crossings = crossingsAtDistanceSquared(midX*midX);
					double normalizedCrossings = - Double.MIN_VALUE;
					if( twoDimensional )
						normalizedCrossings = crossings / (Math.PI * (midX * midX));
					else
						normalizedCrossings = ((4.0 * Math.PI * (midX * midX * midX)) / 3.0);
					csvQuoteAndPrint(pw,startX);
					pw.print(",");
					csvQuoteAndPrint(pw,endX);
					pw.print(",");
					csvQuoteAndPrint(pw,crossings);
					pw.print(",");
					csvQuoteAndPrint(pw,normalizedCrossings);
					pw.print("\r\n");
				}
			}
			pw.close();
		}
	}

	public static class ShollPoint implements Comparable {
		protected boolean nearer;
		protected double distanceSquared;
		public int compareTo(Object o) {
			ShollPoint other = (ShollPoint)o;
			return Double.compare(this.distanceSquared,other.distanceSquared);
		}
		ShollPoint(double distanceSquared,boolean nearer) {
			this.distanceSquared = distanceSquared;
			this.nearer = nearer;
		}
	}

	public static void addPathPointsToShollList( Path p,
						     double x_start,
						     double y_start,
						     double z_start,
						     List<ShollPoint> shollPointsList ) {

		for( int i = 0; i < p.points - 1; ++i ) {
			double xdiff_first = p.precise_x_positions[i] - x_start;
			double ydiff_first = p.precise_y_positions[i] - y_start;
			double zdiff_first = p.precise_z_positions[i] - z_start;
			double xdiff_second = p.precise_x_positions[i+1] - x_start;
			double ydiff_second = p.precise_y_positions[i+1] - y_start;
			double zdiff_second = p.precise_z_positions[i+1] - z_start;
			double distanceSquaredFirst = xdiff_first*xdiff_first + ydiff_first*ydiff_first + zdiff_first*zdiff_first;
			double distanceSquaredSecond = xdiff_second*xdiff_second + ydiff_second*ydiff_second + zdiff_second*zdiff_second;
			shollPointsList.add( new ShollPoint( distanceSquaredFirst, distanceSquaredFirst < distanceSquaredSecond ) );
			shollPointsList.add( new ShollPoint( distanceSquaredSecond, distanceSquaredFirst >=  distanceSquaredSecond ) );
		}

	}

	ArrayList<ShollPoint> shollPointsAllPaths;
	ArrayList<ShollPoint> shollPointsSelectedPaths;

	ResultsPanel resultsPanel = new ResultsPanel();

	protected boolean twoDimensional;

	public ShollAnalysisDialog( String title,
				    double x_start,
				    double y_start,
				    double z_start,
				    PathAndFillManager pafm,
				    ImagePlus originalImage ) {

		super( IJ.getInstance(), title, false );

		this.x_start = x_start;
		this.y_start = y_start;
		this.z_start = z_start;

		this.originalImage = originalImage;
		twoDimensional = (originalImage.getStackSize() == 1);

		shollPointsAllPaths = new ArrayList<ShollPoint>();
		shollPointsSelectedPaths = new ArrayList<ShollPoint>();

		numberOfAllPaths = 0;
		numberOfSelectedPaths = 0;

		for( Path p : pafm.allPaths ) {
			boolean selected = p.getSelected();
			if( p.getUseFitted() ) {
				p = p.fitted;
			} else if( p.fittedVersionOf != null )
				continue;
			addPathPointsToShollList(p,
						 x_start,
						 y_start,
						 z_start,
						 shollPointsAllPaths);
			++ numberOfAllPaths;
			if( selected ) {
				addPathPointsToShollList(p,
							 x_start,
							 y_start,
							 z_start,
							 shollPointsSelectedPaths);
				++ numberOfSelectedPaths;
			}
		}

		useAllPathsCheckbox.setLabel("Use all "+numberOfAllPaths+" paths in analysis?");
		useSelectedPathsCheckbox.setLabel("Use only the "+numberOfSelectedPaths+" selected paths in analysis?");

		addWindowListener(this);

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_START;
		int margin = 10;
		c.insets = new Insets( margin, margin, 0, margin );
		useAllPathsCheckbox.addItemListener(this);
		add(useAllPathsCheckbox,c);

		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		add(useSelectedPathsCheckbox,c);
		useSelectedPathsCheckbox.addItemListener(this);

		++ c.gridy;
		c.insets = new Insets( margin, margin, 0, margin );
		add(normalAxes,c);
		normalAxes.addItemListener(this);
		++ c.gridy;
		c.insets = new Insets( 0, margin, 0, margin );
		add(semiLogAxes,c);
		semiLogAxes.addItemListener(this);
		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		add(logLogAxes,c);
		logLogAxes.addItemListener(this);

		++ c.gridy;
		c.insets = new Insets( margin, margin, 0, margin );
		add(noNormalization,c);
		noNormalization.addItemListener(this);
		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		if( twoDimensional )
			normalizationForSphereVolume.setLabel("Normalize for area enclosed by circle");
		else
			normalizationForSphereVolume.setLabel("Normalize for volume enclosed by circle");
		add(normalizationForSphereVolume,c);
		normalizationForSphereVolume.addItemListener(this);

		++ c.gridy;
		c.gridx = 0;
		Panel separationPanel = new Panel();
		separationPanel.add(new Label("Circle / sphere separation (0 for unsampled analysis)"));
		sampleSeparation.addTextListener(this);
		separationPanel.add(sampleSeparation);
		add(separationPanel,c);

		c.gridx = 0;
		++ c.gridy;
		c.insets = new Insets( margin, margin, margin, margin );
		add(resultsPanel,c);

		++ c.gridy;
		Panel buttonsPanel = new Panel();
		buttonsPanel.setLayout(new BorderLayout());
		Panel topRow = new Panel();
		Panel middleRow = new Panel();
		Panel bottomRow = new Panel();

		topRow.add(makeShollImageButton);
		makeShollImageButton.addActionListener(this);

		topRow.add(drawShollGraphButton);
		drawShollGraphButton.addActionListener(this);

		middleRow.add(exportDetailAsCSVButton);
		exportDetailAsCSVButton.addActionListener(this);

		middleRow.add(exportSummaryAsCSVButton);
		exportSummaryAsCSVButton.addActionListener(this);

		bottomRow.add(addToResultsTableButton);
		addToResultsTableButton.addActionListener(this);

		buttonsPanel.add(topRow,BorderLayout.NORTH);
		buttonsPanel.add(middleRow,BorderLayout.CENTER);
		buttonsPanel.add(bottomRow,BorderLayout.SOUTH);

		add(buttonsPanel,c);

		pack();

		GUI.center(this);
		setVisible(true);

		updateResults();
	}

	public class ResultsPanel extends Panel {
		Label headingLabel = new Label("Results:");
		String defaultText = "[Not calculated yet]";
		Label criticalValuesLabel = new Label(defaultText);
		Label dendriteMaximumLabel = new Label(defaultText);
		// Label schoenenRamificationIndexLabel = new Label(defaultText);
		Label shollsRegressionCoefficientLabel = new Label(defaultText);
		Label shollsRegressionInterceptLabel = new Label(defaultText);
		public ResultsPanel() {
			super();
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			add(headingLabel,c);
			c.anchor = GridBagConstraints.LINE_END;
			c.gridx = 0;
			++ c.gridy;
			c.gridwidth = 1;
			add(new Label("Critical value(s):"),c);
			c.gridx = 1;
			add(criticalValuesLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Dendrite maximum:"),c);
			c.gridx = 1;
			add(dendriteMaximumLabel,c);
			// c.gridx = 0;
			// ++ c.gridy;
			// add(new Label("Schoenen Ramification Index:"),c);
			// c.gridx = 1;
			// add(schoenenRamificationIndexLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Sholl's Regression Coefficient:"),c);
			c.gridx = 1;
			add(shollsRegressionCoefficientLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Sholl's Regression Intercept:"),c);
			c.gridx = 1;
			add(shollsRegressionInterceptLabel,c);
		}
		public void updateFromResults( ShollResults results ) {
			dendriteMaximumLabel.setText(""+results.getDendriteMaximum());
			criticalValuesLabel.setText(""+results.getCriticalValue());
			shollsRegressionCoefficientLabel.setText(""+results.getShollRegressionCoefficient());
			shollsRegressionInterceptLabel.setText(""+results.getRegressionIntercept());
		}
	}

	public void windowClosing( WindowEvent e ) {
		dispose();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }

}
