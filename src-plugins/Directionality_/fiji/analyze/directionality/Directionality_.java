package fiji.analyze.directionality;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
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
 * from center-std to center+std, divided by the total sum of the histogram. The
 * real histogram values are used for the summation, not the gaussian fit. The
 * 'Goodness' column reports the goodness of the fit; 1 is good, 0 is bad.
 * <p>
 * A study made on artificial images reveal that the 'Amount' value as
 * calculated here underestimates the real proportion of structures with the
 * preferred orientation. So for the pine image up there, one can conclude that
 * the proportion of needle leaves oriented around +60º is at least 25%
 * (however, the image is not completely uniform, which cripples the meaning of
 * this amount value).
 * 
 * <h2>Method</h2>
 * 
 * The method implemented is based on Fourier spectrum analysis. For a square
 * image, structures with a preferred orientation generate a periodic pattern at
 * +90º orientation in the Fourier transform of the image, compared to the
 * direction of the objects in the input image. This plugin chops the image into
 * square pieces, and computes their Fourier power spectra. The later are
 * analyzed in polar coordinates, and the power is measured for each angle using
 * the spatial filters proposed in [1]
 * 
 * <h2>References</h2>
 * [1] Liu. Scale space approach to directional analysis of images. Appl. Opt. (1991) vol. 30 (11) pp. 1369-1373 
 * <p>
 * A discussion with A. Leroy is greatly acknowledged. 
 * <p>
 * 
 * @author Jean-Yves Tinevez jeanyves.tinevez@gmail.com
 * 
 */
public class Directionality_ implements ExtendedPlugInFilter {
	
	/*
	 * FIELDS
	 */
	
	private static final float FREQ_THRESHOLD = 5.0f; // get rid of pixels too close to the center in the FFT spectrum
	private static final String PLUGIN_NAME = "Directionality analysis";
	private static final String VERSION_STR = "1.0";
	
	/** If true, will display FFTs and filters. */
	protected boolean debug = false;
	
	protected ImagePlus imp;
	protected ImageStack filters;
	protected FloatProcessor window, r, theta;
	protected int width, height, small_side, long_side, npady, npadx, step, pad_size;
	protected int nbins = 90;
	/** The first bin in degrees, so that we are not forced to start at -90 */
	private double bin_start = -90;
	/** The bin centers, in degrees */
	protected double[] bins;
	/** The directionality histogram, one array per processor.*/
	protected ArrayList<double[]> directionality;
	
	private FloatProcessor padded_square_block; 
	private float[] window_pixels;
	/** Store fit results when fit method is called. */
	protected ArrayList<double[]> params_from_fit;
	/** Store goodness of fit results when fit method is called. */
	protected double[] goodness_of_fit;
	/** Store a String representing the fitting function. */
	protected String fit_string;
	/** False if fitting of the results have not been done, true otherwise. */
	private boolean fit_done;
	/** If set true, will display a {@link ResultsTable} with the histogram at the end of processing. */
	private boolean display_table = false;
	
	
	/*
	 * EXTENDEDPLUGINFILTER METHODS
	 */
	
	/**
	 * This method will be run once for every slice of the stack given in argument. It will analyze it 
	 * using the fields set in the {@link #setup(String, ImagePlus)} method, and push results to the 
	 * {@link #directionality} list, containing the histograms for all this image slices. 
	 */
	public void run(ImageProcessor _ip) {
		fit_done = false;
		final FloatProcessor ip = (FloatProcessor) _ip; // we can do that, for the flag of this plugin is set accordingly
		final Roi original_square = new Roi((pad_size-small_side)/2, (pad_size-small_side)/2, small_side, small_side); 

		float[] fpx, spectrum_px;
		double[] dir = new double[nbins];
		ImageProcessor square_block;
		Roi square_roi;
		FHT fft, pspectrum;		
		FloatProcessor small_pspectrum;
		
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
				padded_square_block.insert(square_block, 0, 0);
				
				// Computes its FFT
				fft = new FHT(padded_square_block);
				fft.setShowProgress(false);
				fft.transform();
				fft.swapQuadrants();
				
				// Get a centered power spectrum with right size
				pspectrum = fft.conjugateMultiply(fft);
				pspectrum .setRoi(original_square);
				small_pspectrum = (FloatProcessor) pspectrum .crop();
				spectrum_px = (float[]) small_pspectrum.getPixels(); 
				
				if (debug) {
					displayLog(small_pspectrum);
				}
				
				// Computes angular density
				for (int bin=0; bin<nbins; bin++) {
					fpx = (float[]) filters.getPixels(bin+1);
					for (int i = 0; i < spectrum_px.length; i++) {
						dir[bin] += spectrum_px[i] * fpx[i]; // will sum out with every block
					}
				}
			}
		}
		
		// Normalize directionality
		double sum = dir[0];
		for (int i = 1; i < dir.length; i++) {
			sum += dir[i];
		}
		for (int i = 0; i < dir.length; i++) {
			dir[i] = dir[i] / sum;
		}
		
		// Draw histogram		
		directionality.add(dir);		
	}

	public int setup(String arg, ImagePlus _imp) {
		
		if (arg.contains("final")) {
			displayResults();
			return DONE;
		}
		
		if (arg.contains("nbins=")) {
			int narg = arg.indexOf("nbins=");
			String nbins_str = arg.substring(narg+"nbins=".length());
			try {
				nbins = Integer.parseInt(nbins_str);
			} catch (NumberFormatException nfe) {
				IJ.error("Directionality: bad argument for number of bins: "+nbins_str);
				return DONE;
			}
		} 
		
		// Prepare data storage
		directionality = new ArrayList<double[]>(_imp.getStack().getSize());
		
		// Assign fields
		this.imp = _imp;
		this.width = _imp.getWidth();
		this.height = _imp.getHeight();
		
		// Flags
		return DOES_ALL
			+ DOES_STACKS
			+ CONVERT_TO_FLOAT
			+ NO_CHANGES
			+ FINAL_PROCESSING;
	}
	
	public void setNPasses(int nPasses) {	}

	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		
		// Prepare dialog
		String current = imp.getTitle();
		GenericDialog gd = new GenericDialog(PLUGIN_NAME + " v" + VERSION_STR);
		gd.addMessage(current);
		gd.addNumericField("Nbins: ", nbins, 0);
		gd.addNumericField("Histogram start", bin_start , 0, 4, "º");
		gd.addCheckbox("Display table", display_table);
		gd.addCheckbox("Debug", debug);

		gd.showDialog();
		
		
		// Collect dialog settings
		if (gd.wasCanceled())
			return DONE;
		nbins = (int) gd.getNextNumber();
		bin_start = gd.getNextNumber();
		display_table = gd.getNextBoolean();
		debug = gd.getNextBoolean();

		// Compute dimensions
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
		
		// Prepare bins
		bins = new double[nbins];
		for (int i = 0; i < nbins; i++) {
			bins[i] = (float) (i * Math.PI / nbins / Math.PI * 180) + bin_start; // in FFT there is a rotation of 90º
		}
		
		// Prepare windowing
		window = getBlackmanProcessor(small_side, small_side);
		window_pixels = (float[]) window.getPixels();
		
		// Prepare polar coordinates
		r = makeRMatrix(small_side, small_side);
		theta = makeThetaMatrix(small_side, small_side);
		
		// Prepare filters
		filters = makeFftFilters();
		
		if (debug) {
			new ImagePlus("Angular filters", filters).show();
		}

		return DOES_ALL
			+ DOES_STACKS
			+ CONVERT_TO_FLOAT
			+ NO_CHANGES
			+ FINAL_PROCESSING;		
	}

	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * This method is called when all slices have been analyzed. It is used to call 
	 * the display of the fitting of the histograms, their display, and the display
	 * of the result table.
	 */
	public void displayResults() {
		fitResults();
		JFrame plot_frame = drawResults();
		JFrame data_frame = analyzeFits();
		
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
			ResultsTable table = exportResults();
			table.show("Directionality histograms for "+imp.getShortTitle());
		}
	}
	
	/**
	 * This method generates a {@link ResultsTable} containing the histogram data for display 
	 * in ImageJ. It can be used to export the data to a CSV file.
	 * 
	 * @return  the result table, which show() method must be called to become visible.
	 */
	public ResultsTable exportResults() {
		ResultsTable table = new ResultsTable();
		String[] names = makeNames();
		double[] dir;
		for (int i = 0; i < bins.length; i++) {
			table.incrementCounter();
			table.addValue("Direction (º)", bins[i]);
			for (int j = 0; j < names.length; j++) {
				dir = directionality.get(j);
				table.addValue(names[j], dir[i]);
			}
		}
		return table;		
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	/**
	 * This method generates the angular filters used for analysis. It reads the fields {@link #nbins},
	 * {@link #bin_start} to determine how many individual angle filter to generate, and {@link #small_side}
	 * to determine the image filter size. As such, they must be set before calling this method.
	 * 
	 * @return  an {@link ImageStack} made of each individual angular filter
	 */
	private final ImageStack makeFftFilters() {
		final ImageStack filters = new ImageStack(small_side, small_side, nbins);
		float[] pixels;
		
		final float[] r_px = (float[]) r.getPixels();
		final float[] theta_px = (float[]) theta.getPixels();
		
		double current_r, current_theta, theta_c, angular_part;
		final double theta_bw = Math.PI/(nbins-1);
		
		for (int i=1; i<= nbins; i++) {
			
			pixels = new float[small_side*small_side];
			theta_c = (i-1) * Math.PI / nbins - (90 + bin_start)*Math.PI/180;
			
			for (int index = 0; index < pixels.length; index++) {

				current_r = r_px[index];
				if ( current_r < FREQ_THRESHOLD || current_r > small_side/2) {
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
	 * This method is called to draw the histograms resulting from image analysis. It reads the result
	 * in the {@link #directionality} list field, and use the JFreeChart library to draw a nice 
	 * plot window. If the {@link #fitResults()} method was called before, the fits are also drawn.
	 * 
	 * @return  a {@link JFrame} containing the histogram plots, which setVisible(boolean) method must
	 * be called in order to be displayed
	 */
	private final JFrame drawResults() {
		final XYSeriesCollection histograms = new XYSeriesCollection();
		final LookupPaintScale lut = createLUT(directionality.size());
		final String[] names = makeNames();
		XYSeries series;
		
		double[] dir;
		for (int i = 0; i < directionality.size(); i++) {
			dir = directionality.get(i);
			series = new XYSeries(names[i]);
			for (int j = 0; j < dir.length; j++) {
				series.add(bins[j], dir[j]);
			}
			histograms.addSeries(series);
		}
		histograms.setIntervalWidth(bins[1]-bins[0]);
		
		// Create chart with histograms
		final JFreeChart chart = ChartFactory.createHistogram(
				"Directionality histograms",
				"Direction (º)",
				"Amount", 
				histograms, 
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		
		// Set the look of histograms
		final XYPlot plot = (XYPlot) chart.getPlot();
		final ClusteredXYBarRenderer renderer = new ClusteredXYBarRenderer(0.3, false);
		float color_index;
		for (int i = 0; i < directionality.size(); i++) {
			color_index = (float)i/(float)(directionality.size()-1);
			renderer.setSeriesPaint(i, lut.getPaint(color_index) );
		}
		plot.setRenderer(0, renderer);
		
		// Draw fit results
		if (fit_done) {
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
			for (int i = 0; i < directionality.size(); i++) { // we have to deal with periodic issue here too
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
			for (int i = 0; i < directionality.size(); i++) {
				color_index = (float)i/(float)(directionality.size()-1);
				plot.getRenderer(1).setSeriesPaint(i, lut.getPaint(color_index) );
			}
			
		}
		plot.getDomainAxis().setRange(bins[0], bins[bins.length-1]);
		
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		JFrame window = new JFrame("Directionality for "+imp.getShortTitle());
        window.add(chartPanel);
        window.validate();
        window.setSize(new java.awt.Dimension(500, 270));
        return window;
	}
	
	/**
	 * This method tries to fit a gaussian to the highest peak of each directionality histogram, 
	 * and store fit results in the {@link #params_from_fit} field.
	 */
	private final void fitResults() {
		params_from_fit = new ArrayList<double[]>(directionality.size());
		goodness_of_fit = new double[directionality.size()];
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
		for (int i = 0; i < directionality.size(); i++) {
			
			dir = directionality.get(i);
			
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
			params_from_fit.add(params);
		}
		fit_done = true;
		fit_string = fitter.getFormula();
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
	
	/**
	 * This method generates statistics from the gaussian fit made in {@link #fitResults()}, and 
	 * display them in a {@link JTable}.
	 * 
	 * @return  a {@link JFrame} containing the table,  which setVisible(boolean) method must
	 * be called in order to be displayed
	 */
	private final JFrame analyzeFits() {
		if (!fit_done) {
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
		double[] params, dir;
		double sum, center, xn;
		final String[] names = makeNames();
		for (int i = 0; i < table_data.length; i++) {
			params =  params_from_fit.get(i);
			dir = directionality.get(i);
			table_data[i][0]	= names[i];
			table_data[i][1]	= String.format("%.2f", params[2]); // peak center
			table_data[i][2]	= String.format("%.2f", Math.abs(params[3])); // standard deviation
			sum = 0; // we sum under +/- sigma, taking periodicity into account
			center = params[2];
			for (int j = 0; j < dir.length; j++) {
				xn = bins[j];
				if (Math.abs(xn-center) > -bins[0] ) { // too far
					if (xn>center) {
						xn = xn - 2*(-bins[0]);							
					} else {
						xn = xn + 2*(-bins[0]);
					}
				}
				if ( (xn<params[2]-params[3]) || (xn>params[2]+params[3]) ) {
					continue;
				}
				sum += dir[j];				
			}
			table_data[i][3] = String.format("%.2f", sum);
			table_data[i][4] = String.format("%.2f", Math.abs(goodness_of_fit[i]));
		}
		JTable table = new JTable(table_data, column_names);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		JPanel		table_panel = new JPanel(new GridLayout());
		table_panel.add(scrollPane);	
	    JFrame 		frame = new JFrame("Directionality analysis for "+imp.getShortTitle());

	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    //Create and set up the content pane.
	    frame.setContentPane(table_panel);

	    //Display the window.
	    frame.pack();
	    return frame;
	}
	
	/*
	 * STATIC METHODS
	 */
	
	/**
	 * This utility method displays an {@link ImagePlus} window with the log10 of each pixel in the
	 * {@link FloatProcessor} given in argument. Usefull to display power spectrum.
	 * 
	 * @param ip  the source FloatProcessor
	 */
	public static final void displayLog(final FloatProcessor ip) {
		final FloatProcessor log10 = new FloatProcessor(ip.getWidth(), ip.getHeight());
		final float[] log10_pixels = (float[]) log10.getPixels();
		final float[] pixels = (float[]) ip.getPixels();
		for (int i = 0; i < pixels.length; i++) {
			log10_pixels[i] = (float) Math.log10(1+pixels[i]); 
		}
		new ImagePlus("Log10 of "+ip.toString(), log10).show();
		log10.resetMinAndMax();
	}
	
	/**
	 * This utility generates a <b>periodic</b> Blackman window over n points.
	 * @param n  the number of point in the window
	 * @return  a double array containing the Blackman window
	 * @see #getBlackmanProcessor(int, int)
	 */
	public static final double[] getBlackmanPeriodicWindow1D(final int n) {
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
	public static  final FloatProcessor getBlackmanProcessor(final int nx, final int ny) {
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
	 * @return  the coordinate matrix, as aFloatProcessor
	 * @see #makeThetaMatrix(int, int)
	 */
	public static final FloatProcessor makeRMatrix(final int nx, final int ny) {
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
	 * @return  the coordinate matrix, as aFloatProcessor
	 * @see #makeRMatrix(int, int)
	 */
	public static final FloatProcessor makeThetaMatrix(final int nx, final int ny) {
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
	public static final LookupPaintScale createLUT(final int ncol) {
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
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/gel.gif");
		imp.show();
		
		Directionality_ da = new Directionality_();
		String command = "Directionality";
		
		new PlugInFilterRunner(da, command, "nbins=10");
	}
	
}
