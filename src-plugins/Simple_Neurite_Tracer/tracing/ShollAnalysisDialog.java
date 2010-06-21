/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

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

import org.w3c.dom.DOMImplementation;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.Document;
import org.apache.batik.svggen.SVGGraphics2D;

public class ShollAnalysisDialog extends Dialog implements WindowListener, ActionListener, TextListener, ItemListener {

	protected double x_start, y_start, z_start;

	protected CheckboxGroup pathsGroup = new CheckboxGroup();
	protected Checkbox useAllPathsCheckbox = new Checkbox("Use all paths in analysis?", pathsGroup, false);
	protected Checkbox useSelectedPathsCheckbox = new Checkbox("Use only selected paths in analysis?", pathsGroup, true);
	protected Button makeShollImageButton = new Button("Make Sholl image");
	protected Button drawShollGraphButton = new Button("Draw Graph");
	protected Button exportAsCSV = new Button("Export results as CSV");

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
		} else if( source == exportAsCSV ) {
			// results.makeShollCrossingsImagePlus(originalImage);
		} else if( source == drawShollGraphButton ) {
			results.drawGraph();
		}
	}

	public void textValueChanged( TextEvent e ) {
		Object source = e.getSource();
		if( source == sampleSeparation ) {

		}
	}

	void updateResults() {
		ShollResults results = getCurrentResults();
	}

	public void itemStateChanged( ItemEvent e ) {
		updateResults();
	}

	public ShollResults getCurrentResults() {
		List<ShollPoint> pointsToUse;
		String description = "Sholl analysis ";
		String postDescription = " for "+originalImage.getTitle();
		if( useSelectedPathsCheckbox.getState() ) {
			pointsToUse = shollPointsSelectedPaths;
			description += "of selected paths " + postDescription;
		} else {
			pointsToUse = shollPointsAllPaths;
			description += "of all paths" + postDescription;
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

	public static final int AXES_NORMAL   = 1;
	public static final int AXES_SEMI_LOG = 2;
	public static final int AXES_LOG_LOG  = 3;

	public static final int NOT_NORMALIZED = 4;
	public static final int NORMALIZED_FOR_SPHERE_VOLUME = 5;

	public static class ShollResults {
		protected double [] squaredRangeStarts;
		protected int [] crossingsPastEach;
		protected int n;
		protected double x_start, y_start, z_start;
		protected int maxCrossings = Integer.MIN_VALUE;
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
		public double getMaxDistanceSquared() {
			return squaredRangeStarts[n-1];
		}
		public ShollResults( List<ShollPoint> shollPoints,
				     double x_start,
				     double y_start,
				     double z_start,
				     String description,
				     int axes,
				     int normalization,
				     double sphereSeparation,
				     boolean twoDimensional ) {
			this.x_start = x_start;
			this.y_start = y_start;
			this.z_start = z_start;
			this.description = description;
			this.axes = axes;
			this.normalization = normalization;
			this.sphereSeparation = sphereSeparation;
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
				if( currentCrossings > maxCrossings )
					maxCrossings = currentCrossings;
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
					double x = x_graph_points[i];
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
			maxY = Double.MIN_VALUE;
			minY = Double.MAX_VALUE;
			for( int i = 0; i < graphPoints; ++i ) {
				double y = y_graph_points[i];
				if( ! (Double.isInfinite(y) || Double.isNaN(y)) ) {
					if( y > maxY )
						maxY = y;
					if( y < minY )
						minY = y;
				}
			}
			if( maxY == Double.MIN_VALUE )
				throw new RuntimeException("[BUG] Somehow there were no valid points found");
		}

		public void drawGraph() {

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
				return;
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


			System.out.println("Setting x axis range to: "+minX+" -> "+maxX);
			xAxis.setRange(minX,maxX);
			System.out.println("Setting y axis range to: "+minY+" -> "+maxY);
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

			JFreeChart chart = new JFreeChart( description, plot );

			final ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(800,600));

			JFrame window = new JFrame(description);
			window.add(chartPanel);
			window.validate();
			window.setSize(new java.awt.Dimension(500, 270));
			window.setVisible(true);
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

		int numberOfAllPaths = 0;
		int numberOfSelectedPaths = 0;

		Iterator<Path> pi = pafm.allPaths.iterator();
		while( pi.hasNext() ) {
			Path p = pi.next();
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
		add(useAllPathsCheckbox,c);

		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		add(useSelectedPathsCheckbox,c);

		++ c.gridy;
		c.insets = new Insets( margin, margin, 0, margin );
		add(normalAxes,c);
		++ c.gridy;
		c.insets = new Insets( 0, margin, 0, margin );
		add(semiLogAxes,c);
		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		add(logLogAxes,c);

		++ c.gridy;
		c.insets = new Insets( margin, margin, 0, margin );
		add(noNormalization,c);
		++ c.gridy;
		c.insets = new Insets( 0, margin, margin, margin );
		if( twoDimensional )
			normalizationForSphereVolume.setLabel("Normalize for area enclosed by circle");
		else
			normalizationForSphereVolume.setLabel("Normalize for volume enclosed by circle");
		add(normalizationForSphereVolume,c);

		++ c.gridy;
		add(new Label("Circle / sphere separation (0 for unsampled analysis)"),c);
		sampleSeparation.addTextListener(this);
		c.gridx = 1;
		add(sampleSeparation,c);

		c.gridx = 0;
		++ c.gridy;
		c.insets = new Insets( margin, margin, margin, margin );
		add(resultsPanel,c);

		++ c.gridy;
		Panel buttonsPanel = new Panel();

		makeShollImageButton.addActionListener(this);
		buttonsPanel.add(makeShollImageButton);

		buttonsPanel.add(exportAsCSV);
		exportAsCSV.addActionListener(this);

		buttonsPanel.add(drawShollGraphButton);
		drawShollGraphButton.addActionListener(this);

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
		Label schoenenRamificationIndexLabel = new Label(defaultText);
		Label semiLogKLabel = new Label(defaultText);
		Label logLogKLabel = new Label(defaultText);
		Label modifiedShollMethodLabel = new Label(defaultText);
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
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Schoenen Ramification Index:"),c);
			c.gridx = 1;
			add(schoenenRamificationIndexLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Sholl's Regression Coefficient (Semi-Log):"),c);
			c.gridx = 1;
			add(semiLogKLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Sholl's Regression Coefficient (Log-Log):"),c);
			c.gridx = 1;
			add(logLogKLabel,c);
			c.gridx = 0;
			++ c.gridy;
			add(new Label("Modified Sholl method:"),c);
			c.gridx = 1;
			add(modifiedShollMethodLabel,c);
		}
		public void updateFromResults( ShollResults results ) {
			// FIXME: complete
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

	public static class ShollGraphFrame extends JFrame {

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
}
