package fiji.analyze.directionality;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * <h2>Usage</h2> This plugin is used to infer the preferred orientation of
 * structures present in the input image. It computes a histogram indicating the
 * amount of structures in a given direction. Images with completely isotropic
 * content are expected to give a flat histogram, whereas images in which there
 * is a preferred orientation are expected to give a histogram with a peak at
 * that orientation.
 * <p>
 * Angles are reported in their common mathematical sense. That is: 0º is the
 * East direction, and the orientation is counterclockwise.
 * 
 * <h2>Statistics generated</h2>
 * 
 * On top of the histogram, the plugin tries to generate statistics on the
 * highest peak found.
 * <p>
 * The highest peak is fitted by a Gaussian function, taking into account the
 * periodic nature of the histogram. The 'Direction (º)' column reports the
 * center of the gaussian. The 'Dispersion (º)' column reports the standard
 * deviation of the gaussian. The 'Amount' column is the sum of the histogram
 * from the center of the peak to <b>two</b> standard deviation away, divided by 
 * the total sum of the histogram. The
 * real histogram values are used for the summation, not the gaussian fit. The
 * 'Goodness' column reports the goodness of the fit; 1 is good, 0 is bad.
 * <p>
 * A study made on artificial images reveal that the 'Amount' value as
 * calculated here underestimates the real proportion of structures with the
 * preferred orientation.
 * 
 * <h2>Analysis methods</h2>
 * 
 * Two methods are implemented:
 * 
 * <h3>Fourier components analysis</h3>
 * 
 * This method is based on Fourier spectrum analysis. For a square
 * image, structures with a preferred orientation generate a periodic pattern at
 * +90º orientation in the Fourier transform of the image, compared to the
 * direction of the objects in the input image. 
 * <p>
 * This plugin chops the image into
 * square pieces, and computes their Fourier power spectra. The later are
 * analyzed in polar coordinates, and the power is measured for each angle using
 * the spatial filters proposed in [1]
 * 
 * <h3>Local gradient orientation</h3>
 * 
 * This method is a local analysis. The gradient of the image is calculated using a 5x5
 * Sobel filter, and is used to derive the local gradient orientation. This orientation 
 * is then used to build the histogram, by putting the square of the gradient norm 
 * in the adequate bin. The square of the norm was retained, so as to have an histogram 
 * with the same dimension that for the Fourier analysis.
 * 
 * 
 * <h2>Code structure</h2>
 * 
 * This plugin is written as a classical ImageJ plugin. It implements {@link PlugIn}. 
 * <p>
 * String arguments can be passed to it, using the {@link #run(String)} method.
 * For instance:
 * 
 *  <pre>
 *  ImagePlus imp = IJ.openImage("./TwoLines.tif");
 *  imp.show();
 *  Directionality_ da = new Directionality_();
 *  da.run("nbins=60, start=-90, method=gradient");
 *  </pre>
 * 
 * <h2>Version history</h2>
 * 
 * <ul>
 * <li> v1.3 - 2010-03-17: Heavy refactoring, made it implement Plugin interface, so has to 
 * be conveniently called from a script.
 * <li> v1.2 - 2010-03-10: Added a new analysis method based on local gradient orientation.
 * <li> v1.1 - 2010-03-05: Added an option to export the histogram as a table, and option 
 * to circular-shifts the histogram.
 * <li> v1.0 - 2010-03-01: First working commit with the Fourier method.
 * </ul>
 * 
 * <h2>References</h2>
 * [1] Liu. Scale space approach to directional analysis of images. Appl. Opt. (1991) vol. 30 (11) pp. 1369-1373 
 * <p>
 * A discussion with A. Leroy and another one with J. Schindein are greatly acknowledged. 
 * <p>
 * 
 * @author Jean-Yves Tinevez jeanyves.tinevez@gmail.com
 * @version 1.3
 */
public class Directionality_ implements PlugIn {
	
	/*
	 * ENUMS
	 */
	
	public enum AnalysisMethod {
		FOURIER_COMPONENTS,
		LOCAL_GRADIENT_ORIENTATION;
		public String toString() {
			switch (this) {
			case FOURIER_COMPONENTS:
				return "Fourier components";
			case LOCAL_GRADIENT_ORIENTATION:
				return "Local gradient orientation";
			}
			return "Not implemented";
		}
		public String toCommandName() {
			switch (this) {
			case FOURIER_COMPONENTS:
				return "Fourier";
			case LOCAL_GRADIENT_ORIENTATION:
				return "Gradient";
			}
			return "Not implemented";
		}
	}
	
	
	/*
	 * FIELDS
	 */
	
	/* CONSTANTS */
	
	private static final float FREQ_THRESHOLD = 5.0f; // get rid of pixels too close to the center in the FFT spectrum
	/** How many sigmas away from the gaussian center we sum to get the amount value. */ 
	private static final double SIGMA_NUMBER = 2;
	private static final String PLUGIN_NAME = "Directionality analysis";
	private static final String VERSION_STR = "1.3";
	
	/* SETTING FIELDS, they determine results */
	
	/** The ImagePlus this plugin operates on. */
	protected ImagePlus imp;
	/** If true, will display FFTs and filters. */
	protected boolean debug = false;
	/** The number of bins to create. */
	protected int nbins = 90;
	/** The first bin in degrees, so that we are not forced to start at -90 */
	private double bin_start = -90;
	/** Method used for analysis, as set by the user. */
	private AnalysisMethod method = AnalysisMethod.FOURIER_COMPONENTS;
	/** If set true, will display a {@link ResultsTable} with the histogram at the end of processing. */
	private boolean display_table = false;
	/** If true, will calculate a map of orientation. */
	private boolean build_orientation_map = false;
	/** If true, will display a color wheel to interpret the orientation map. */
	private boolean display_color_wheel = false;


	/* STD FIELDS */
	
	/** FloatProcessor to convert source ImageProcessor to. */
	private FloatProcessor fip;
	/** Fourier filters are stored as a stack */
	protected ImageStack filters;
	/** Polar coordinates, stored as a FloatProcessor. */
	protected FloatProcessor window, r, theta;
	protected int width, height, small_side, long_side, npady, npadx, step, pad_size;
	/** The bin centers, in degrees */
	protected double[] bins;
	/** The directionality histogram, one array per processor (3 in the case of a ColorProcessor).*/
	protected ArrayList<double[]> histograms;
	
	private FloatProcessor padded_square_block; 
	private float[] window_pixels;
	/** Store fit results when fit method is called. */
	protected ArrayList<double[]> params_from_fit;
	/** Store goodness of fit results when fit method is called. */
	protected double[] goodness_of_fit;
	/** Store a String representing the fitting function. */
	protected String fit_string;
	/** Used to pass the slice we are currently analyzing. */
	private int slice_index;
	/** This stack stores the orientation map. */
	ImageStack orientation_map;
	
	
	/*
	 * PLUGIN METHODS
	 */
	
	
	/**
	 * Called when this plugin is launched from ImageJ. 
	 * This method
	 * <ol>
	 * 	<li> grabs the current ImagePlus
	 * 	<li> displays the user dialog and sets setting fields accordingly
	 * 	<li> calls the {@link #computesHistograms()} method, which computes the histograms
	 * 	<li> calls the {@link #fitHistograms()} method, which fits the histograms
	 * 	<li> display the results
	 * </ol>
	 * <p>
	 * If the method is called with String arguments, fields are set according to it, and
	 * no dialog are displayed (macro recordable).
	 * 
	 *  @param  arg  the string argument, for instance "nbins=90, start=-90, method=gradient"
	 */
	public void run(String arg) {
		
		// Test if we get an image
		imp = WindowManager.getCurrentImage();
		if (null == imp) {
			IJ.error("Directionality", "No images are open.");
			return;
		}		

		// Non-interactive mode?
		if (null != arg && arg.length() > 0) {
			
			// Parse possible macro inputs
			String str = parseArgumentString(arg, "nbins=");
			if (null != str) {
				try {
					nbins = Integer.parseInt(str);
				} catch (NumberFormatException nfe) {
					IJ.error("Directionality: bad argument for number of bins: "+str);
					return;
				}
			}
			str = parseArgumentString(arg, "start=");
			if (null != str) {
				try {
					bin_start = Double.parseDouble(str);
				} catch (NumberFormatException nfe) {
					IJ.error("Directionality: bad argument for start point: "+str);
					return;
				}
			}
			str = parseArgumentString(arg, "method=");
			if (null != str) {
				for (AnalysisMethod m : AnalysisMethod.values()) {
					if (m.toCommandName().equalsIgnoreCase(str)) {
						method = m;
					}
				}
			}
		} else {
			showDialog();
		}
		
		// Launch analysis, this will set the directionality field
		computesHistograms();
		
		// Fit histograms
		fitHistograms();
		
		// Display results
		JFrame plot_frame = plotResults();
		JFrame data_frame = displayFitAnalysis();
		
		int x = Math.max(0, imp.getWindow().getLocation().x - plot_frame.getSize().width);
		int y = imp.getWindow().getLocation().y;
		plot_frame.setLocation(x, y);
		plot_frame.setVisible(true);
		
		y += plot_frame.getHeight();
		if (y>Toolkit.getDefaultToolkit().getScreenSize().getHeight()) {
			y = (int) (0.9 * Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		}
		data_frame.setLocation(x, y);
		data_frame.setVisible(true);
		
		if (display_table) {
			ResultsTable table = displayResultsTable();
			table.show("Directionality histograms for "+imp.getShortTitle()+" (using "+method.toString()+")");
		}
		
		if (build_orientation_map) {
			ImagePlus imp_map = new ImagePlus("Orientation map for "+imp.getShortTitle(), orientation_map);
			imp_map.show();
			ImageCanvas canvas_map = imp_map.getCanvas();
			addColorMouseListener(canvas_map);
		}
		
		if (display_color_wheel) {
			ImagePlus cw = generateColorWheel();
			cw.show();
			ImageCanvas canvas_cw = cw.getCanvas();
			addColorMouseListener(canvas_cw);
		}
	}
	
	
	
	
	
	
	

	/*
	 * PUBLIC METHODS
	 */


	
	
	
	/**
	 * This method runs the analysis on all slices, and store resulting histograms in the 
	 * {@link #histograms} fields. Calling this method resets the aforementioned field.
	 */
	public void computesHistograms() {
		if (null == imp) return;
		
		// Reset analysis fields
		params_from_fit = null;
		goodness_of_fit = null;
		
		// Prepare helper fields
		bins = prepareBins(nbins, bin_start);
		switch (method) {
		case FOURIER_COMPONENTS:
			initFourierFields();
			break;
		case LOCAL_GRADIENT_ORIENTATION:
			break;		
		}

		// Prepare result holder
		int n_slices = imp.getStackSize();
		histograms = new ArrayList<double[]>(n_slices * imp.getNChannels()); 
		if (build_orientation_map) {
			orientation_map = new ImageStack(imp.getWidth(), imp.getHeight());
		}
		
		
		// Loop over each slice
		ImageProcessor ip = null;
		double[] dir = null;
		for (int i = 0; i < n_slices; i++) {
			slice_index = i;
			ip = imp.getStack().getProcessor(i+1);
			for (int channel_number = 0; channel_number < ip.getNChannels(); channel_number++) {
				
				// Convert to float processor
				fip = ip.toFloat(channel_number, fip);
				
				// Dispatch to specialized method
				switch (method) {
				case FOURIER_COMPONENTS:
					dir = fourier_component(fip);
					break;
				case LOCAL_GRADIENT_ORIENTATION:
					dir = local_gradient_orientation(fip);
					break;
				}
				
				// Normalize directionality
				double sum = dir[0];
				for (int j = 1; j < dir.length; j++) {
					sum += dir[i];
				}
				for (int j = 0; j < dir.length; j++) {
					dir[i] = dir[i] / sum;
				}
				
				histograms.add( dir );
			}
		}
	}
	
	/**
	 * This method generates a {@link ResultsTable} containing the histogram data for display 
	 * in ImageJ. It can be used to export the data to a CSV file.
	 * 
	 * @return  the result table, which show() method must be called to become visible.
	 */
	public ResultsTable displayResultsTable() {
		if (null == histograms) 
			return null;
		
		ResultsTable table = new ResultsTable();
		String[] names = makeNames();
		double[] dir;
		for (int i = 0; i < bins.length; i++) {
			table.incrementCounter();
			table.addValue("Direction (º)", bins[i]);
			for (int j = 0; j < names.length; j++) {
				dir = histograms.get(j);
				table.addValue(names[j], dir[i]);
			}
		}
		return table;		
	}
	
	/**
	 * Return the result of analyzing the gaussian fit of the peak.
	 * Results are returned in the shape of an ArrayList of double[], one element
	 * per slice. The content of the double arrays is as follow:
	 * <ol start=0>
	 * 	<li> gaussian peak center
	 * 	<li> gaussian standard deviation
	 * 	<li> amount, that is: the sum of the histogram data from the gaussian center until {@value #SIGMA_NUMBER} times
	 * its standard deviation away
	 * 	<li> the goodness of fit 
	 * </ol>
	 * The periodic nature of the data is taken into account. For the amount value, the actual values
	 * of the histogram are summed, not the values from the fit.
	 * 
	 * 
	 * @return  the fit analysis
	 */
	public ArrayList<double[]> getFitAnalysis() {
		if (null == histograms)
			return null;
		
		final ArrayList<double[]> fit_analysis = new ArrayList<double[]>(histograms.size());
		double[] gof = getGoodnessOfFit();
		double[] params = null;
		double[] dir = null;
		double[] analysis = null;
		double amount, center, std, xn;
		
		for (int i = 0; i < histograms.size(); i++) {
			params =  params_from_fit.get(i);
			dir = histograms.get(i);
			analysis = new double[4];
			
			amount = 0; // we sum under +/- N*sigma, taking periodicity into account
			center = params[2];
			std = params[3];
			for (int j = 0; j < dir.length; j++) {
				xn = bins[j];
				if (Math.abs(xn-center) > 90.0 ) { // too far, we want to shift then
					if (xn>center) {
						xn = xn - 180.0;							
					} else {
						xn = xn + 180.0;
					}
				}
				if ( (xn<center-SIGMA_NUMBER*std) || (xn>center+SIGMA_NUMBER*std) ) {
					continue;
				}
				amount += dir[j];
			}
			
			analysis[0] = center;
			analysis[1] = std;
			analysis[2] = amount;
			analysis[3] = gof[i];
			fit_analysis.add(analysis);
		}
		return fit_analysis;
	}
	
	/**
	 * This method is called to draw the histograms resulting from image analysis. It reads the result
	 * in the {@link #histograms} list field, and use the JFreeChart library to draw a nice 
	 * plot window. If the {@link #fitResults()} method was called before, the fits are also drawn.
	 * 
	 * @return  a {@link JFrame} containing the histogram plots, which setVisible(boolean) method must
	 * be called in order to be displayed
	 */
	public JFrame plotResults() {
		final XYSeriesCollection histogram_plots = new XYSeriesCollection();
		final LookupPaintScale lut = createLUT(histograms.size());
		final String[] names = makeNames();
		XYSeries series;
		
		double[] dir;
		for (int i = 0; i < histograms.size(); i++) {
			dir = histograms.get(i);
			series = new XYSeries(names[i]);
			for (int j = 0; j < dir.length; j++) {
				series.add(bins[j], dir[j]);
			}
			histogram_plots.addSeries(series);
		}
		histogram_plots.setIntervalWidth(bins[1]-bins[0]);
		
		// Create chart with histograms
		final JFreeChart chart = ChartFactory.createHistogram(
				"Directionality histograms",
				"Direction (º)",
				"Amount", 
				histogram_plots, 
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		
		// Set the look of histograms
		final XYPlot plot = (XYPlot) chart.getPlot();
		final ClusteredXYBarRenderer renderer = new ClusteredXYBarRenderer(0.3, false);
		float color_index;
		for (int i = 0; i < histograms.size(); i++) {
			color_index = (float)i/(float)(histograms.size()-1);
			renderer.setSeriesPaint(i, lut.getPaint(color_index) );
		}
		plot.setRenderer(0, renderer);
		
		// Draw fit results
		if (null == params_from_fit) {
			// Make new X
			final double[] X = new double[bins.length*10]; // oversample 10 times
			for (int i = 0; i < X.length; i++) {
				X[i] = bins[0] + (bins[bins.length-1]-bins[0])/X.length * i;
			}
			// Create dataset
			final XYSeriesCollection fits = new XYSeriesCollection();
			XYSeries fit_series;
			double val, center, xn;
			double[] params;
			final double half_range = (bins[bins.length-1] - bins[0])/2.0;
			for (int i = 0; i < histograms.size(); i++) { // we have to deal with periodic issue here too
				params = params_from_fit.get(i).clone();
				center = params[2];
				fit_series = new XYSeries(names[i]);
				for (int j = 0; j < X.length; j++) {
					xn = X[j];
					if (Math.abs(xn-center) > half_range ) { // too far
						if (xn>center) {
							xn = xn - 2*half_range;							
						} else {
							xn = xn + 2*half_range;
						}
					}
					val = CurveFitter.f(CurveFitter.GAUSSIAN, params, xn);
					fit_series.add(X[j], val);
				}
				fits.addSeries(fit_series);
			}
			plot.setDataset(1, fits);
			plot.setRenderer(1, new XYLineAndShapeRenderer(true, false));
			for (int i = 0; i < histograms.size(); i++) {
				color_index = (float)i/(float)(histograms.size()-1);
				plot.getRenderer(1).setSeriesPaint(i, lut.getPaint(color_index) );
			}
			
		}
		plot.getDomainAxis().setRange(bins[0], bins[bins.length-1]);
		
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		JFrame window = new JFrame("Directionality for "+imp.getShortTitle()+" (using "+method.toString()+")");
        window.add(chartPanel);
        window.validate();
        window.setSize(new java.awt.Dimension(500, 270));
        return window;
	}
	
	/**
	 * This method tries to fit a gaussian to the highest peak of each directionality histogram, 
	 * and store fit results in the {@link #params_from_fit} field. The goodness of fit will be
	 * stored in {@link #goodness_of_fit}.
	 */
	public void fitHistograms() {
		if (null == histograms)
			return;
		
		params_from_fit = new ArrayList<double[]>(histograms.size());
		goodness_of_fit = new double[histograms.size()];
		double[] dir;
		double[] init_params = new double[4];
		double[] params = new double[4];
		double[] padded_dir;
		double[] padded_bins;
		
		double ymax, ymin;
		int imax, shift_index, current_index;
		
		// Prepare fitter and function
		CurveFitter fitter = null;
		
		// Loop over slices
		for (int i = 0; i < histograms.size(); i++) {
			
			dir = histograms.get(i);
			
			// Infer initial values
			ymax = Double.NEGATIVE_INFINITY;
			ymin = Double.POSITIVE_INFINITY;
			imax = 0;
			for (int j = 0; j < dir.length; j++) {
				if (dir[j] > ymax) {
					ymax = dir[j];
					imax = j;
				}
				if (dir[j]<ymin) {
					ymin = dir[j];
				}
			}
						
			// Shift found peak to the center (periodic issue) and add to fitter
			padded_dir 	= new double[bins.length];
			padded_bins = new double[bins.length];
			shift_index = bins.length/2 - imax;
			for (int j = 0; j < bins.length; j++) {
				current_index = j - shift_index;
				if (current_index < 0) {
					current_index += bins.length; 
				}
				if (current_index >= bins.length) {
					current_index -= bins.length;
				}
				padded_dir[j] 	= dir[current_index];
				padded_bins[j] 	= bins[j];
			}			
			fitter = new CurveFitter(padded_bins, padded_dir);
			
			init_params[0] = ymin; // base
			init_params[1] = ymax; // peak value 
			init_params[2] = padded_bins[bins.length/2]; // peak center with padding
			init_params[3] = 2 * ( bins[1] - bins[0]); // std
			
			// Do fit
			fitter.doFit(CurveFitter.GAUSSIAN);
			params = fitter.getParams();
			goodness_of_fit[i] = fitter.getFitGoodness();
			if (shift_index < 0) { // back into orig coordinates
				params[2] += (bins[-shift_index]-bins[0]);
			} else {
				params[2] -= (bins[shift_index]-bins[0]);
			}
			params[3] = Math.abs(params[3]); // std is positive
			params_from_fit.add(params);
		}
		fit_string = fitter.getFormula();
	}
	
	/**
	 * This method displays the fit analysis results in a {@link JTable}.
	 * 
	 * @return  a {@link JFrame} containing the table; its setVisible(boolean) method must
	 * be called in order to be displayed
	 */
	public JFrame displayFitAnalysis() {
		if (null == params_from_fit) {
			return null;
		}
		// Display result
		String[] column_names = {
				"Slice",
				"Direction (º)",
				"Dispersion (º)",
				"Amount",
				"Goodness" };
		Object[][]  table_data = new Object[params_from_fit.size()][column_names.length];
		final String[] names = makeNames();
		final ArrayList<double[]> fit_analysis = getFitAnalysis();
		double[] analysis = null;
		
		for (int i = 0; i < table_data.length; i++) {
			analysis = fit_analysis.get(i);
			table_data[i][0]	= names[i];
			table_data[i][1]	= String.format("%.2f", analysis[0]); // peak center
			table_data[i][2]	= String.format("%.2f", analysis[1]); // standard deviation
			table_data[i][3] 	= String.format("%.2f", analysis[2]); // amount
			table_data[i][4] 	= String.format("%.2f", analysis[3]); // goodness of fit
		}
		JTable table = new JTable(table_data, column_names);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		JPanel		table_panel = new JPanel(new GridLayout());
		table_panel.add(scrollPane);	
	    JFrame 		frame = new JFrame("Directionality analysis for "+imp.getShortTitle()+" (using "+method.toString()+")");

	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    //Create and set up the content pane.
	    frame.setContentPane(table_panel);

	    //Display the window.
	    frame.pack();
	    return frame;
	}
	
	
	
	
	
	
	
	
	/*
	 * SETTERS AND GETTERS
	 */
	
	
	
	/**
	 * Set the image for analysis. Calling this method resets the field {@link #histograms} to null.
	 */
	public void setImagePlus(ImagePlus imp) {
		this.imp = imp;
		histograms = null;
	}
	
	/**
	 * Get the image analyzed.
	 */
	public ImagePlus getImagePlus() {
		return imp;
	}
	
	/**
	 * Return the parameters of the gaussian fit of the main peak in histogram. Results
	 * are arranged in an ArrayList of double[], one array per slice analyzed.
	 * If the fit was not done prior to this method call, it is called. If the 
	 * method {@link #computesHistograms()} was not called, null is returned.
	 * <p>
	 * The double array is organized as follow, for the fitting model y = a + (b-a)*exp(-(x-c)*(x-c)/(2*d*d))
	 * <ol start=0>
	 * 	<li> a
	 * 	<li> b
	 *  <li> x
	 *  <li> d
	 * </ul>
	 *  
	 * @return  the fitting parameters
	 * @see {@link #getGoodnessOfFit()}, {@link #getHistograms()}, {@link #getBins()}
	 */
	public ArrayList<double[]> getFitParameters() {
		if (null == params_from_fit) {
			fitHistograms();
		}
		return params_from_fit;
	}
	
	/** 
	 * Return the goodness of fit for the gaussian fit; 1 is good, 0 is bad. One value per slice.
	 * If the fit was not done prior to this method call, it is called. If the 
	 * method {@link #computesHistograms()} was not called, null is returned.
	 * @see {@link #getFitParameters()}, {@link #getHistograms()}, {@link #getBins()}
	 * @return the goodness of fit
	 */
	public double[] getGoodnessOfFit() {
		if (null == params_from_fit) {
			fitHistograms();
		}
		return goodness_of_fit;
	}
	
	/**
	 * Return the directionality histograms as an ArrayList of double[], one array
	 * per slice.
	 * @see {@link #getBins()}, {@link #getFitParameters()}, {@link #getGoodnessOfFit()}
	 * @return  the directionality histograms; is null if the method {@link #computesHistograms()} 
	 * was not called before.
	 */
	public ArrayList<double[]> getHistograms() {
		return histograms;
	}
	
	/**
	 * Return the center of the bins for the directionality histograms. They are in degrees.
	 * @see {@link #getHistograms()}, {@link #getFitParameters()}, {@link #getGoodnessOfFit()}
	 * @return  the bin centers, in degrees
	 */
	public double[] getBins() {
		return bins;
	}
	
	/**
	 * Set the desired number of bins. This resets the {@link #histograms} field to null.
	 */
	public void setBinNumber(int nbins) {
		this.nbins = nbins;
		prepareBins(nbins, bin_start);
		histograms = null;
	}
	
	/**
	 * Return the current number of bins for this instance.
	 * @return
	 */
	public int getBinNumber() {
		return nbins;
	}
	
	/**
	 * Set the desired start for the angle bins, in degrees. This resets the {@link #histograms} field to null.
	 */
	public void setBinStart(double bin_start) {
		this.bin_start = bin_start;
		histograms = null;
	}
	
	/**
	 * Return the current value for angle bin start, in degrees.
	 */
	public double getBinStart() {
		return bin_start;
	}
	
	/**
	 * Set the desired method for analysis. This resets the {@link #histograms} field to null.
	 * @see {@link AnalysisMethod}
	 */
	public void setMethod(AnalysisMethod method) {
		this.method = method;
		histograms = null;
	}
	
	/**
	 * Return the analysis method used by this instance.
	 * @return
	 */
	public AnalysisMethod getMethod() {
		return method;
	}
	
	/**
	 * Set the debug flag.
	 */
	public void setDebugFlag(boolean flag) {
		this.debug = flag;
	}
	
	/**
	 * Set the build orientation map flag
	 */
	public void setBuildOrientationMapFlag(boolean flag) {
		this.build_orientation_map = flag;
	}

	/**
	 * Return the orientation map as an {@link ImageStack}, one slice per slice in the source image.
	 * Return null if the orientation map flag was not set, or if computation was not done.  
	 */
	public ImageStack getOrientationMap() {
//		if (!build_orientation_map) 
//			return null;
		return orientation_map;
	}
	
	
	
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	
	
	
	
	/**
	 * Display the dialog when the plugin is launched from ImageJ. A successful interaction will
	 * result in setting the {@link #nbins}, {@link #bin_start}, {@link #display_table} 
	 * and {@link #debug} fields.
	 */
	private void showDialog() {

		// Prepare dialog
		String current = imp.getTitle();
		String[] method_names = new String[AnalysisMethod.values().length];
		for (int i = 0; i < method_names.length; i++) {
			method_names[i] = AnalysisMethod.values()[i].toString();
		}

		// Layout dialog
		GenericDialog gd = new GenericDialog(PLUGIN_NAME + " v" + VERSION_STR);
		gd.addMessage(current);
		gd.addChoice("Method:", method_names, method.toString());
		gd.addNumericField("Nbins: ", nbins, 0);
		gd.addNumericField("Histogram start", bin_start , 0, 4, "º");
		gd.addCheckbox("Build orientation map", build_orientation_map);
		gd.addCheckbox("Display color wheel", display_color_wheel);
		gd.addCheckbox("Display table", display_table);
		gd.addCheckbox("Debug", debug);
		gd.showDialog();

		// Collect dialog settings
		if (gd.wasCanceled())
			return;
		String chosen_method = gd.getNextChoice();
		for (int i = 0; i < method_names.length; i++) {
			if (chosen_method.equals(method_names[i])) {
				method = AnalysisMethod.values()[i];
				break;
			}
		} 

		// Reflect user settings in fields
		nbins = (int) gd.getNextNumber();
		bin_start = gd.getNextNumber();
		build_orientation_map = gd.getNextBoolean();
		display_color_wheel = gd.getNextBoolean();
		display_table = gd.getNextBoolean();
		debug = gd.getNextBoolean();
	}
	
	/**
	 * This method is used to initialize variables required for the Fourier analysis
	 * after parameters have been set by the user.
	 */
	private void initFourierFields() {
		if (null == imp)
			return;
		
		// Compute dimensions
		width = imp.getWidth();
		height = imp.getHeight();
		if ( width == height) {
			npadx = 1;
			npady = 1;
			long_side = width;
			small_side = width;
			step = 0;
		} else {
			small_side = Math.min(width, height);
			long_side  = Math.max(width, height);
			int npad = long_side / small_side + 1;
			if (width == long_side) {
				npadx = npad;
				npady = 1;
			} else {
				npadx = 1;
				npady = npad;
			}
			int delta = (long_side - small_side);
			step = delta / (npad-1);
		}
		
		// Computes power of 2 image dimension
		pad_size = 2;
        while(pad_size<small_side) pad_size *= 2;
		padded_square_block = new FloatProcessor(pad_size, pad_size);
		
		// Prepare windowing
		window = getBlackmanProcessor(small_side, small_side);
		window_pixels = (float[]) window.getPixels();
		
		// Prepare polar coordinates
		r = makeRMatrix(pad_size, pad_size);
		theta = makeThetaMatrix(pad_size, pad_size);
		
		// Prepare filters
		filters = makeFftFilters();
		
		if (debug) {
			new ImagePlus("Angular filters", filters).show();
		}
	}
	
	/**
	 * This method implements the local gradient orientation analysis method.
	 * <p>
	 * The gradient is calculated using a 5x5 Sobel filter. The gradient orientation
	 * from -pi/2 to pi/2 is calculated and put in the histogram. The histogram
	 * get the square of the norm as value, so that only strong gradient contribute to it.
	 * We use the square of the norm, so that the histogram calculated with this method
	 * has the same dimension that with the Fourier method.
	 * 
	 * @see #fourier_component(FloatProcessor)
	 *  
	 */
	private final double[] local_gradient_orientation(final FloatProcessor ip) {
		double[] dir = new double[nbins]; // histo with #bins
		final double[] norm_dir = new double[nbins]; // histo from -pi to pi;
		final FloatProcessor grad_x = (FloatProcessor) ip.duplicate();
		final FloatProcessor grad_y = (FloatProcessor) ip.duplicate();
		final Convolver convolver = new Convolver();
		final float[] kernel_y = new float[] { 
				-2f,  	-1f, 	0f, 	1f, 	2f,
				-3f,  	-2f,  	0f, 	2f, 	3f,
				-4f, 	-3f, 	0f, 	3f, 	4f,
				-3f,  	-2f,  	0f, 	2f, 	3f,
				-2f,  	-1f,  	0f, 	1f, 	2f		} ; // That's gx, but we want to have a 90º shift, to comply to the rest of the plugin
		final float[] kernel_x = new float[] {
				2f, 	3f, 	4f, 	3f, 	2f,
				1f, 	2f, 	3f, 	2f, 	1f,
				0, 		0, 		0, 		0, 		0,
				-1f, 	-2f, 	-3f, 	-2f, 	-1f,
				-2f, 	-3f, 	-4f, 	-3f, 	-2f		};

		convolver.convolveFloat(grad_x, kernel_x, 5, 5);
		convolver.convolveFloat(grad_y, kernel_y, 5, 5);
		
		final float[] pixels_gx = (float[]) grad_x.getPixels();
		final float[] pixels_gy = (float[]) grad_y.getPixels();
		final float[] pixels_theta = new float[pixels_gx.length];
		final float[] pixels_r = new float[pixels_gx.length];
		
		double norm, max_norm = 0.0;
		double angle;
		int histo_index;
		float dx, dy;
		for (int i = 0; i < pixels_gx.length; i++) {
			dx = pixels_gx[i];
			dy =  - pixels_gy[i]; // upright orientation
			norm = dx*dx+dy*dy; // We keep the square so as to have the same dimension that Fourier components analysis
			if (norm > max_norm) { 
				max_norm = norm;
			}
			angle = Math.atan(dy/dx);
			pixels_theta[i] = (float) (angle * 180.0 / Math.PI);
			pixels_r[i] = (float) norm;
			histo_index = (int) ((nbins/2.0) * (1 + angle / (Math.PI/2)) ); // where to put it
			if (histo_index == nbins) {
				histo_index = 0; // circular shift in case of exact vertical orientation
			}
			norm_dir[histo_index] += norm; // we put the norm, the stronger the better
		}
		
		// Shift histogram				
		final double shift = Math.toRadians(bins[0]) - (-Math.PI/2);
		final int index_shift = (int) ( nbins / Math.PI * shift );
		int j;
		for (int i = 0; i < norm_dir.length; i++) {
			j = i + index_shift;
			while (true) {
				if (j<0) { j += nbins; } 
				else if (j>=nbins) { j-= nbins; } 
				else { break;}
			}
			dir[j] = norm_dir[i];
		}
		
		if (build_orientation_map) {
			final float[] pixels = (float[]) ip.getPixels();
			float max_brightness = Float.NEGATIVE_INFINITY;
			float min_brightness = Float.POSITIVE_INFINITY;
			
			for (int i = 0; i < pixels.length; i++) {
				if (pixels[i] > max_brightness){
					max_brightness = pixels[i];
				}
				if (pixels[i] < min_brightness) {
					min_brightness = pixels[i];
				}
			}
			final ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight());
			final byte[] H = new byte[pixels_r.length];
			final byte[] S = new byte[pixels_r.length];
			for (int i = 0; i < pixels_r.length; i++) {
				H[i] = (byte) (255.0 * (pixels_theta[i]+90.0)/180.0);
				S[i] = (byte) (255.0 * pixels_r[i] / max_norm) ; //Math.log10(1.0 + 9.0*pixels_r[i] / max_norm) );
			}
			final byte[] B = (byte[]) ip.convertToByte(true).getPixels();
			cp.setHSB(H, S, B);
			orientation_map.addSlice(makeNames()[slice_index], cp);
		}
		
		return dir;
	}

	/**
	 * This method implements the Fourier component analysis method. The method {@link #initFourierFields()}
	 * must be called before this method is.
	 * <p>
	 * Images are chopped in squares, and the Fourier spectrum of each square is calculated.
	 * For the spectrum, we get the angular component using the filters generated by {@link #makeFftFilters()}.
	 * <p>
	 * We return the results as a double array, containing the amount of orientation for each angles
	 * specified in the {@link #bins} field. 
	 * @see {@link #local_gradient_orientation(FloatProcessor)}
	 */
	private final double[] fourier_component(FloatProcessor ip) {
		final Roi original_square = new Roi((pad_size-small_side)/2, (pad_size-small_side)/2, small_side, small_side); 
		final Roi top_corner = new Roi(0, 0, small_side, small_side);
		
		float[] fpx, spectrum_px;
		final double[] dir = new double[nbins];
		ImageProcessor square_block;
		Roi square_roi;
		FHT fft, pspectrum;		
		FloatProcessor small_pspectrum;
		
		ImageStack spectra = null;
		if (debug) {
			spectra = new ImageStack(small_side, small_side);
		}
		
		ByteProcessor[] hue_images = null;
		ByteProcessor[] saturation_images = null;
		ByteProcessor hue, saturation;
		if (build_orientation_map) {
			hue_images = new ByteProcessor[npadx*npady];
			saturation_images = new ByteProcessor[npadx*npady];
		}
		
		
		// If the image is not square, split it in small square padding all the image
		for (int ix = 0; ix<npadx; ix++) {
			
			for (int iy = 0; iy<npady; iy++) {
				
				// Extract a square block from the image
				square_roi = new Roi( ix*step, iy*step, small_side, small_side );
				ip.setRoi(square_roi);
				square_block = ip.crop();
				
				// Window the block
				float[] block_pixels = (float[]) square_block.getPixels();
				for (int i = 0; i < block_pixels.length; i++) {
					block_pixels[i] *= window_pixels[i]; 
				}
				
				// Pad the block with a power of 2 size
				padded_square_block.setValue(0.0);
				padded_square_block.fill();
				padded_square_block.insert(square_block, (pad_size-small_side)/2, (pad_size-small_side)/2);
				
				// Computes its FFT
				fft = new FHT(padded_square_block);
				fft.setShowProgress(false);
				fft.transform();
				fft.swapQuadrants();
				
				// Get a centered power spectrum with right size
				pspectrum = fft.conjugateMultiply(fft);
				pspectrum .setRoi(original_square);
				small_pspectrum = (FloatProcessor) pspectrum.crop();
				spectrum_px = (float[]) pspectrum.getPixels(); //small_pspectrum.getPixels(); 
				
				if (debug) {
					spectra.addSlice("block nbr "+(ix+1)*(iy+1), displayLog(small_pspectrum));
				}

				// For orientation map
				float[] weights = null, sini = null, cosi = null;
				FHT tmp;
				float[] tmp_px; 
				double sangle, cangle;
				if (build_orientation_map) {
					weights = new float[fft.getPixelCount()];
					sini 	= new float[fft.getPixelCount()];
					cosi 	= new float[fft.getPixelCount()];
				}
				
				// Loop over all bins
				for (int bin=0; bin<nbins; bin++) {
					
					// Get filter pixels
					fpx = (float[]) filters.getPixels(bin+1);
					
					// Loop over all pixels
					if (build_orientation_map) {
						
						tmp = fft.getCopy();
						tmp.setShowProgress(false);
						tmp_px = (float[]) tmp.getPixels();
						for (int i = 0; i < spectrum_px.length; i++) {
							// Computes angular density
							dir[bin] += spectrum_px[i] * fpx[i]; // will sum out with every block
							// Build orientation map if needed
							tmp_px[i] *= fpx[i];							
						}
						tmp.inverseTransform();
						// Build angular statistics arrays -> 2nd loop
						sangle = Math.sin(Math.toRadians(bins[bin]));
						cangle = Math.cos(Math.toRadians(bins[bin]));
						tmp_px = (float[]) tmp.getPixels();
						for (int j = 0; j < tmp_px.length; j++) {
							weights[j] 		+= tmp_px[j] * tmp_px[j];
							sini[j] 		+= weights[j] * sangle;
							cosi[j] 		+= weights[j] * cangle;
						}
						
					} else {
						
						for (int i = 0; i < spectrum_px.length; i++) {
							// Computes angular density, and that's all
							dir[bin] += spectrum_px[i] * fpx[i]; // will sum out with every block
						}							
					}

				} // end loop over all bins
				
				if (build_orientation_map) {
					// Computes angular statistics -> 3rd loop!
					final double[] mean_angle = new double[cosi.length];
					final double[] mean_norm = new double[cosi.length];
					double max_norm = 0.0;
					for (int j = 0; j < cosi.length; j++) {
						mean_angle[j] 	= Math.atan(sini[j]/cosi[j]);
						mean_norm[j]	= 1 / weights[j] * Math.sqrt(cosi[j]*cosi[j]+sini[j]*sini[j]);
						if (mean_norm[j] > max_norm) {
							max_norm = mean_norm[j];
						}
					}
					// Build the HSV image -> 4th loop!!
					hue 		= new ByteProcessor(pad_size, pad_size);
					saturation 	= new ByteProcessor(pad_size, pad_size);
					byte[] H = (byte[]) hue.getPixels();
					byte[] S = (byte[]) saturation.getPixels();
					for (int j = 0; j < cosi.length; j++) {
						H[j] = (byte) (255.0 * (mean_angle[j]+Math.PI/2)/(Math.PI));
						S[j] = (byte) (255.0 * mean_norm[j] / max_norm);//Math.log10(1.0 + 9.0*mean_norm[j] / max_norm) );
					}
					hue.setRoi(top_corner);
					saturation.setRoi(top_corner);
					hue_images[ix+npadx*iy] = (ByteProcessor) hue.crop(); 
					saturation_images[ix+npadx*iy] = (ByteProcessor) hue.crop();
				}			
			}
			
		}
		
		// Reconstruct final orientation map
		if (build_orientation_map) {
			ByteProcessor big_hue = new ByteProcessor(ip.getWidth(), ip.getHeight());
			ByteProcessor big_saturation = new ByteProcessor(ip.getWidth(), ip.getHeight());
			ByteProcessor big_brightness = (ByteProcessor) ip.convertToByte(true);
			for (int ix = 0; ix<npadx; ix++) {
				for (int iy = 0; iy<npady; iy++) {					
					big_hue.insert(hue_images[ix+npadx*iy], ix*step,  iy*step);
					big_saturation.insert(saturation_images[ix+npadx*iy], ix*step,  iy*step);					
				}
			}
			ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight());
			cp.setHSB(
					(byte[]) big_hue.getPixels(), 
					(byte[]) big_saturation.getPixels(), 
					(byte[]) big_brightness.getPixels()
					);
			orientation_map.addSlice(makeNames()[slice_index], cp);
		}

		
		if (debug) {
			new ImagePlus("Log10 power FFT of "+makeNames()[slice_index], spectra).show();
		}
		
		return dir;		
	}
	
	/**
	 * This method generates the angular filters used by the Fourier analysis. It reads the fields {@link #nbins},
	 * {@link #bin_start} to determine how many individual angle filter to generate, and {@link #pad_size}
	 * to determine the image filter size. As such, they must be set before calling this method.
	 * 
	 * @return  an {@link ImageStack} made of each individual angular filter
	 * @see {@link #fourier_component(FloatProcessor)}, {@link #prepareBins()}
	 */
	private final ImageStack makeFftFilters() {
		final ImageStack filters = new ImageStack(pad_size, pad_size, nbins);
		float[] pixels;
		
		final float[] r_px = (float[]) r.getPixels();
		final float[] theta_px = (float[]) theta.getPixels();
		
		double current_r, current_theta, theta_c, angular_part;
		final double theta_bw = Math.PI/(nbins-1);
		
		for (int i=1; i<= nbins; i++) {
			
			pixels = new float[pad_size*pad_size];
			theta_c = (i-1) * Math.PI / nbins - (90 + bin_start)*Math.PI/180;
			
			for (int index = 0; index < pixels.length; index++) {

				current_r = r_px[index];
				if ( current_r < FREQ_THRESHOLD || current_r > pad_size/2) {
					continue;
				}
				
				current_theta = theta_px[index];
				if ( Math.abs(current_theta-theta_c) < theta_bw) {
					angular_part = Math.cos( (current_theta-theta_c) / theta_bw * Math.PI / 2.0) ;
					angular_part = angular_part * angular_part;
					
				} else if ( 
						Math.abs(current_theta-(theta_c-Math.PI)) < theta_bw
						|| Math.abs(current_theta-2*Math.PI-(theta_c-Math.PI)) < theta_bw
						) {
					angular_part = Math.cos( (current_theta-theta_c) / theta_bw * Math.PI / 2.0) ;
					if (nbins % 2 == 0) {
						angular_part = 1 - angular_part * angular_part;
					} else {
						angular_part = angular_part * angular_part;
					}
				} else 	{
					continue; // leave it to 0
				}
				
				pixels[index] = (float) angular_part; // only angular for now 

			}
			
			filters.setPixels(pixels, i);
			filters.setSliceLabel("Angle: "+String.format("%.1f",theta_c*180/Math.PI), i);
		}
		return filters;
	}
	
	/**
	 * This method generate a name for each analyzed slice, to display in result tables.
	 * 
	 * @return  a String array with the names
	 */
	private final String[] makeNames() {
		final int n_slices = imp.getStack().getSize();
		String[] names;
		String label;
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			names = new String[3*n_slices];
			for (int i=0; i<n_slices; i++) {
				label = imp.getStack().getShortSliceLabel(i+1);
				if (null == label) {				
					names[0+i*3] = "Slice_"+(i+1)+"R";
					names[1+i*3] = "Slice_"+(i+1)+"G";
					names[2+i*3] = "Slice_"+(i+1)+"B";
				} else {
					names[0+i*3] = label+"_R";
					names[1+i*3] = label+"_G";
					names[2+i*3] = label+"_B";					
				}
			}
		} else {
			if (n_slices <= 1) {
				return new String[] { imp.getShortTitle() };
			}
			names = new String[n_slices];
			for (int i=0; i<n_slices; i++) {
				label = imp.getStack().getShortSliceLabel(i+1);
				if (null == label) {
					names[i] = "Slice_"+(i+1);
				} else {
					names[i] = label;
				}
			}
		}
		return names;		
	}
	
	
	
	
	
	
	
	/*
	 * STATIC METHODS
	 */
	
	
	public static final ImagePlus generateColorWheel() {
		final int cw_height= 256;
		final int cw_width = cw_height/2;
		final ColorProcessor color_ip = new ColorProcessor(cw_width, cw_height);
		FloatProcessor R = makeRMatrix(cw_height, cw_height);
		FloatProcessor T = makeThetaMatrix(cw_height, cw_height);
		final Roi half_roi = new Roi(cw_height/2, 0, cw_width, cw_height);
		R.setRoi(half_roi);
		R = (FloatProcessor) R.crop();
		T.setRoi(half_roi);
		T = (FloatProcessor) T.crop();
		final float[] r = (float[]) R.getPixels();
		final float[] t = (float[]) T.getPixels();
		final byte[] hue = new byte[r.length];
		final byte[] sat = new byte[r.length];
		final byte[] bgh = new byte[r.length];
		for (int i = 0; i < t.length; i++) {
			if (r[i] > cw_height) {
				hue[i] = (byte) 255;
				sat[i] = (byte) 255;
				bgh[i] = 0;
			} else {
				hue[i] = (byte) ( 255 * (t[i]+Math.PI/2)/Math.PI);
				sat[i] = (byte) ( 255 * r[i]/cw_width);
				bgh[i] = (byte) 255;
			}
		}
		color_ip.setHSB(hue, sat, bgh);
		final ImagePlus imp = new ImagePlus("Color wheel", color_ip);
		return imp;
	}
	
	
	protected static final void addColorMouseListener(final ImageCanvas canvas) {

		MouseMotionListener ml = new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) {}
			public void mouseMoved(MouseEvent e) {
				Point coord = canvas.getCursorLoc();
				int x = coord.x;
				int y = coord.y;
				try {
					final ColorProcessor cp = (ColorProcessor) canvas.getImage().getProcessor();
					final int c = cp.getPixel(x, y);
					final int r = (c&0xff0000) >>16;
					final int g = (c&0xff00)>>8;
					final int b = c&0xff;
					final float[] hsb = Color.RGBtoHSB(r, g, b, null);
					final float angle = hsb[0] * 180 - 90;
					final float amount = hsb[1];
					IJ.showStatus( String.format("Orientation: %5.1f º - Amont: %5.1f %%", angle, 100*amount));
				} catch (ClassCastException cce) {
					return;
				}
			}};
			canvas.addMouseMotionListener(ml);
	}

	
	/**
	 * Generate a bin array of angle in degrees, from the start value to value+PI.
	 * @return a double array of n elements, the angles in degrees
	 * @param n the number of elements to generate
	 * @param start  the value of the first element
	 */
	protected final static double[] prepareBins(final int n, final double start) {
		// Prepare bins
		final double[] bins = new double[n];
		for (int i = 0; i < n; i++) {
			bins[i] = (float) Math.toDegrees(i * Math.PI / n) + start;
		}
		return bins;
	}
	
	/**
	 * Utility method to analyze the content of the argument string passed by ImageJ to 
	 * this plugin using the {@link #setup(String, ImagePlus)} method. Not as clever as it
	 * could be.
	 * @param  argument_string  the argument string to parse
	 * @param  command_str  the command to search for
	 * @return  a string containing the value after the command string, null if the command string 
	 * was not found
	 */
	protected final static String parseArgumentString(String argument_string, String command_str) {
		if (argument_string.contains(command_str)) {
			int narg = argument_string.indexOf(command_str)+command_str.length();
			int next_arg = argument_string.indexOf(",", narg);
			if (next_arg == -1) {
				next_arg = argument_string.length();
			}
			String str = argument_string.substring(narg, next_arg);
			return str;
		}
		return null;
	}
	
	/**
	 * This utility method returns a FloatProcessor with the log10 of each pixel in the
	 * {@link FloatProcessor} given in argument. Usefull to display power spectrum.
	 * 
	 * @param ip  the source FloatProcessor
	 */
	protected static final FloatProcessor displayLog(final FloatProcessor ip) {
		final FloatProcessor log10 = new FloatProcessor(ip.getWidth(), ip.getHeight());
		final float[] log10_pixels = (float[]) log10.getPixels();
		final float[] pixels = (float[]) ip.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			log10_pixels[i] = (float) Math.log10(1+pixels[i]); 
		}
		return log10;
	}
	
	/**
	 * This utility generates a <b>periodic</b> Blackman window over n points.
	 * @param n  the number of point in the window
	 * @return  a double array containing the Blackman window
	 * @see #getBlackmanProcessor(int, int)
	 */
	protected static final double[] getBlackmanPeriodicWindow1D(final int n) {
		final double[] window = new double[n];
		for (int i = 0; i < window.length; i++) {
			window[i] = 0.42 - 0.5 * Math.cos(2*Math.PI*i/n) + 0.08 * Math.cos(4*Math.PI/n);
		}		
		return window;
	}

	/** 
	 * Generate a 2D Blackman window used in this plugin before computing FFT, so as to avoid the cross
	 * artifact at x=0 and y=0.
	 * @param nx  the width in pixel of the desired window
	 * @param ny  
	 * @return  the window, as a FloatProcessor
	 * @see #getBlackmanPeriodicWindow1D(int)
	 */
	protected static  final FloatProcessor getBlackmanProcessor(final int nx, final int ny) {
		final FloatProcessor bpw = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) bpw.getPixels();
		final double[] bpwx = getBlackmanPeriodicWindow1D(nx);
		final double[] bpwy = getBlackmanPeriodicWindow1D(nx);
		int ix,iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) (bpwx[ix] * bpwy[iy]);
		}
		return bpw;
	}
	
	/**
	 * Generate a 2D matrix of the radius polar coordinates, centered in the middle of the image.
	 * @param nx  the width in pixel of the desired matrix
	 * @param ny  the height in pixel of the desired matrix
	 * @return  the coordinate matrix, as a FloatProcessor, where values range from 0 to 
	 * sqrt(nx^2+ny^2)/2
	 * @see #makeThetaMatrix(int, int)
	 */
	protected static final FloatProcessor makeRMatrix(final int nx, final int ny) {
		final FloatProcessor r = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) r.getPixels();
		final float xc = nx / 2.0f;
		final float yc = ny / 2.0f;
		int ix, iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) Math.sqrt( (ix-xc)*(ix-xc) + (iy-yc)*(iy-yc));
		}		
		return r;
	}
	
	/**
	 * Generate a 2D matrix of the angle polar coordinates, centered in the middle of the image.
	 * @param nx  the width in pixel of the desired matrix
	 * @param ny  the height in pixel of the desired matrix
	 * @return  the coordinate matrix, as a FloatProcessor, where angles are in radians, and range 
	 * from -pi to pi
	 * @see #makeRMatrix(int, int)
	 */
	protected static final FloatProcessor makeThetaMatrix(final int nx, final int ny) {
		final FloatProcessor theta = new FloatProcessor(nx, ny);
		final float[] pixels = (float[]) theta.getPixels();
		final float xc = nx / 2.0f;
		final float yc = ny / 2.0f;
		int ix, iy;
		for (int i = 0; i < pixels.length; i++) {
			iy = i / nx;
			ix = i % nx;
			pixels[i] = (float) Math.atan2( -(iy-yc), ix-xc); // so that we have upright orientation
		}		
		return theta;
	}
	
	/**
	 * Generate a bluish to greenish to redish LUT for the display of histograms.
	 * @param ncol  the number of colors in the LUT
	 * @return  the LUT
	 */
	protected static final LookupPaintScale createLUT(final int ncol) {
		final float[][] colors = new float[][]  { 
				{0, 75/255f, 150/255f},
				{0.1f, 0.8f, 0.1f},
				{150/255f, 75/255f, 0}
		};
		final float[] limits = new float[] {0, 0.5f, 1};
		final LookupPaintScale lut = new LookupPaintScale(0, 1, Color.BLACK);
		float val;
		float r, g, b;
		for (int j = 0; j < ncol; j++) {			
			val = (float)j/(float)(ncol-0.99f);
			int i = 0;
			for (i = 0; i < limits.length; i++) {
				if (val < limits[i]) {
					break;
				}
			}
			i = i - 1;
			r = colors[i][0] + (val-limits[i])/(limits[i+1]-limits[i])*(colors[i+1][0]-colors[i][0]); 
			g = colors[i][1] + (val-limits[i])/(limits[i+1]-limits[i])*(colors[i+1][1]-colors[i][1]); 
			b = colors[i][2] + (val-limits[i])/(limits[i+1]-limits[i])*(colors[i+1][2]-colors[i][2]); 
			lut.add(val, new Color(r, g, b));
		}
		return lut;
	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		// Generate a test image
		ImagePlus imp = NewImage.createByteImage("Lines", 200, 200, 1, NewImage.FILL_BLACK);
		ImageProcessor ip = imp.getProcessor();
		ip.setLineWidth(4);
		ip.setColor(Color.WHITE);
		Line line_30deg 	= new Line(10.0, 412.0, 446.4102, 112.0); // 400px long line, 30º
		Line line_30deg2 = new Line(10.0, 312.0, 446.4102, 12.0); // 400px long line, 30º
		Line line_m60deg = new Line(10.0, 10, 300.0, 446.4102); // 400px long line, 60º
		Line[] rois = new Line[] { line_30deg, line_30deg2, line_m60deg };
		for ( Line roi : rois) {
			ip.draw(roi);
		}		
		GaussianBlur smoother = new GaussianBlur();
		smoother.blurGaussian(ip, 2.0, 2.0, 1e-2);		
//		smoother.blurGaussian(ip, 2.0, 2.0, 1e-2);		
		imp.show();
		
		AnalysisMethod method;
		ArrayList<double[]> fit_results;
		double center;
		
		Directionality_ da = new Directionality_();
		da.setImagePlus(imp);
		
		da.setBinNumber(60);
		da.setBinStart(-90);

		da.setBuildOrientationMapFlag(true);
		da.setDebugFlag(false);
		
		
		method = AnalysisMethod.FOURIER_COMPONENTS;
		da.setMethod(method);
		da.computesHistograms();
		fit_results = da.getFitParameters();
		center = fit_results.get(0)[2];
		System.out.println("With method: "+method);
		System.out.println(String.format("Found maxima at %.1f, expected it at 30º.\n", center, 30));
		new ImagePlus("Orientation map for "+imp.getShortTitle(),da.getOrientationMap()).show();
		
		
		method = AnalysisMethod.LOCAL_GRADIENT_ORIENTATION;
		da.setMethod(method);
		da.computesHistograms();
		fit_results = da.getFitParameters();
		center = fit_results.get(0)[2];
		System.out.println("With method: "+method);
		System.out.println(String.format("Found maxima at %.1f, expected it at 30º.\n", center, 30));
		new ImagePlus("Orientation map for "+imp.getShortTitle(),da.getOrientationMap()).show();
		
		ImagePlus cw = generateColorWheel();
		cw.show();
		addColorMouseListener(cw.getCanvas());

	}
	
}
