package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.NumberParser;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextSimpleAnnotation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.LogHistogramDataset;

public class FilterPanel extends javax.swing.JPanel {

	static final Font FONT = new Font("Arial", Font.PLAIN, 11);
	static final Font SMALL_FONT = FONT.deriveFont(10f);

	private static final Color annotationColor = new java.awt.Color(252,117,0);
	private static final long serialVersionUID = 1L;
	private static final String DATA_SERIES_NAME = "Data";
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	JComboBox jComboBoxFeature;
	private ChartPanel chartPanel;
	private JButton jButtonAutoThreshold;
	JRadioButton jRadioButtonBelow;
	JRadioButton jRadioButtonAbove;
	private LogHistogramDataset dataset;
	private JFreeChart chart;
	private XYPlot plot;
	private IntervalMarker intervalMarker;
	private double threshold;
	private Map<String, double[]> valuesMap;
	private XYTextSimpleAnnotation annotation;
	private String key;
	private List<String> allKeys;
	private Map<String, String> keyNames;

	private ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();

	/*
	 * CONSTRUCTOR
	 */

	public FilterPanel(final List<String> allKeys, Map<String, String> keyNames, final Map<String, double[]> valuesMap, final int selectedKey) {
		super();
		this.valuesMap = valuesMap;		
		this.allKeys = allKeys;
		this.keyNames = keyNames;
		initGUI();
		jComboBoxFeature.setSelectedIndex(selectedKey);
	}

	public FilterPanel(Map<String, double[]> valuesMap, List<String> allKeys, Map<String, String> keyNames) {
		this(allKeys, keyNames, valuesMap, 0);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the threshold currently selected for the data displayed in this panel.
	 * @see #isAboveThreshold()
	 */
	public void setThreshold(double threshold) { 
		this.threshold = threshold;
		redrawThresholdMarker();
	}

	/**
	 * Set if the current threshold should be taken above or below its value.
	 * @param isAbove  if true, the threshold will be related as above its value.
	 */
	public void setAboveThreshold(boolean isAbove) {
		jRadioButtonAbove.setSelected(isAbove);
		jRadioButtonBelow.setSelected(!isAbove);
		redrawThresholdMarker();
	}


	/**
	 * Return the threshold currently selected for the data displayed in this panel.
	 * @see #isAboveThreshold()
	 */
	public double getThreshold() { return threshold; }

	/**
	 * Return true if the user selected the above threshold option for the data displayed 
	 * in this panel.
	 * @see #getThreshold()
	 */
	public boolean isAboveThreshold() { return jRadioButtonAbove.isSelected(); }


	/** 
	 * Return the Enum constant selected in this panel.
	 */
	public String getKey() { return key; }

	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener} will
	 * be notified when a change happens to the threshold displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addChangeListener(ChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove an ChangeListener. 
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener(ChangeListener listener) {
		return listeners.remove(listener);
	}

	public Collection<ChangeListener> getChangeListeners() {
		return listeners;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void fireThresholdChanged() {
		for (ChangeListener al : listeners) 
			al.stateChanged(CHANGE_EVENT);
	}

	private void comboBoxSelectionChanged() {
		key = allKeys.get(jComboBoxFeature.getSelectedIndex());
		double[] values = valuesMap.get(key);
		if (null == values || 0 == values.length) {
			dataset = new LogHistogramDataset();
			threshold = Double.NaN;
			annotation.setLocation(0.5f, 0.5f);
			annotation.setText("No data");
			fireThresholdChanged();
		} else {
			int nBins = TMUtils.getNBins(values, 8, 100);
			dataset = new LogHistogramDataset();
			if (nBins > 1) {
				dataset.addSeries(DATA_SERIES_NAME, values, nBins);
			}
		}
		plot.setDataset(dataset);
		resetAxes();
		autoThreshold(); // Will fire the fireThresholdChanged();
	}

	private void autoThreshold() {
		String selectedFeature = allKeys.get(jComboBoxFeature.getSelectedIndex());
		double[] values = valuesMap.get(selectedFeature);
		if (null != values) {
			threshold = TMUtils.otsuThreshold(valuesMap.get(selectedFeature));
			redrawThresholdMarker();
		}
	}

	private void initGUI() {
		Dimension panelSize = new java.awt.Dimension(250, 140);
		Dimension panelMaxSize = new java.awt.Dimension(1000, 140);
		try {
			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.rowWeights = new double[] {0.0, 1.0, 0.0};
			thisLayout.rowHeights = new int[] {10, 7, 15};
			thisLayout.columnWeights = new double[] {0.0, 0.0, 1.0};
			thisLayout.columnWidths = new int[] {7, 20, 7};
			this.setLayout(thisLayout);
			this.setPreferredSize(panelSize);
			this.setMaximumSize(panelMaxSize);
			this.setBorder(new LineBorder(annotationColor, 1, true));
			{
				ComboBoxModel jComboBoxFeatureModel = new DefaultComboBoxModel(
						TMUtils.getArrayFromMaping(allKeys, keyNames).toArray(new String[] {}));
				jComboBoxFeature = new JComboBox();
				this.add(jComboBoxFeature, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
				jComboBoxFeature.setModel(jComboBoxFeatureModel);
				jComboBoxFeature.setFont(FONT);
				jComboBoxFeature.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						comboBoxSelectionChanged();
					}
				});
			}
			{
				createHistogramPlot();
				chartPanel.setPreferredSize(new Dimension(0, 0));
				this.add(chartPanel, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				chartPanel.setOpaque(false);
			}
			{
				jButtonAutoThreshold = new JButton();
				this.add(jButtonAutoThreshold, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jButtonAutoThreshold.setText("Auto");
				jButtonAutoThreshold.setFont(SMALL_FONT);
				jButtonAutoThreshold.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						autoThreshold();
					}
				});
			}
			{
				jRadioButtonAbove = new JRadioButton();
				this.add(jRadioButtonAbove, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
				jRadioButtonAbove.setText("Above");
				jRadioButtonAbove.setFont(SMALL_FONT);
				jRadioButtonAbove.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						redrawThresholdMarker();
					}
				});
			}
			{
				jRadioButtonBelow = new JRadioButton();
				this.add(jRadioButtonBelow, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
				jRadioButtonBelow.setText("Below");
				jRadioButtonBelow.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						redrawThresholdMarker();
					}
				});
				jRadioButtonBelow.setFont(SMALL_FONT);
			}
			{
				ButtonGroup buttonGroup = new ButtonGroup();
				buttonGroup.add(jRadioButtonAbove);
				buttonGroup.add(jRadioButtonBelow);
				jRadioButtonAbove.setSelected(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Instantiate and configure the histogram chart.
	 */
	private void createHistogramPlot() {
		dataset = new LogHistogramDataset();
		chart = ChartFactory.createHistogram(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);

		plot = chart.getXYPlot();		
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setShadowVisible(false);
		renderer.setMargin(0);
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setDrawBarOutline(true);
		renderer.setSeriesOutlinePaint(0, Color.BLACK);
		renderer.setSeriesPaint(0, new Color(1, 1, 1, 0));

		plot.setBackgroundPaint(new Color(1, 1, 1, 0));
		plot.setOutlineVisible(false);
		plot.setDomainCrosshairVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setRangeGridlinesVisible(false);

		plot.getRangeAxis().setVisible(false);
		plot.getDomainAxis().setVisible(false);

		chart.setBorderVisible(false);
		chart.setBackgroundPaint(new Color(0.6f, 0.6f, 0.7f));

		intervalMarker = new IntervalMarker(0, 0, new Color(0.3f, 0.5f, 0.8f), new BasicStroke(), new Color(0, 0, 0.5f), new BasicStroke(1.5f), 0.5f);
		plot.addDomainMarker(intervalMarker);

		chartPanel = new ChartPanel(chart);
		MouseListener[] mls = chartPanel.getMouseListeners();
		for (MouseListener ml : mls)
			chartPanel.removeMouseListener(ml);

		chartPanel.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {
				chartPanel.requestFocusInWindow();
				threshold = getXFromChartEvent(e);
				redrawThresholdMarker();
			}
		});
		chartPanel.addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {}
			public void mouseDragged(MouseEvent e) {
				threshold = getXFromChartEvent(e);
				redrawThresholdMarker();
			}
		});
		chartPanel.setFocusable(true);
		chartPanel.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				annotation.setColor(annotationColor.darker());
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				annotation.setColor(Color.RED.darker());
			}
		});
		chartPanel.addKeyListener(new MyKeyListener());

		annotation = new XYTextSimpleAnnotation(chartPanel);
		annotation.setFont(SMALL_FONT.deriveFont(Font.BOLD));
		annotation.setColor(annotationColor.darker());
		plot.addAnnotation(annotation);
	}

	private double getXFromChartEvent(MouseEvent mouseEvent) {
		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue(mouseEvent.getX(), plotArea, plot.getDomainAxisEdge());
	}

	private void redrawThresholdMarker() {
		String selectedFeature = allKeys.get(jComboBoxFeature.getSelectedIndex());
		double[] values = valuesMap.get(selectedFeature);
		if (null == values)
			return;

		if (jRadioButtonAbove.isSelected()) {
			intervalMarker.setStartValue(threshold);
			intervalMarker.setEndValue(plot.getDomainAxis().getUpperBound());
		} else {
			intervalMarker.setStartValue(plot.getDomainAxis().getLowerBound());
			intervalMarker.setEndValue(threshold);
		}
		float x, y;
		if (threshold > 0.85 * plot.getDomainAxis().getUpperBound()) { 
			x = (float) (threshold - 0.15 * plot.getDomainAxis().getRange().getLength());
		} else { 
			x = (float) (threshold + 0.05 * plot.getDomainAxis().getRange().getLength());
		}

		y = (float) (0.85 * plot.getRangeAxis().getUpperBound());
		annotation.setText(String.format("%.1f", threshold));
		annotation.setLocation(x, y);
		fireThresholdChanged();
	}

	private void resetAxes() {
		plot.getRangeAxis().setLowerMargin(0);
		plot.getRangeAxis().setUpperMargin(0);
		plot.getDomainAxis().setLowerMargin(0);
		plot.getDomainAxis().setUpperMargin(0);
	}



	/*
	 * MAIN METHOD
	 */


	/**
	 * Display this JPanel inside a new JFrame.
	 */
	public static void main(String[] args) {
		// Prepare fake data
		final int N_ITEMS = 100;
		final Random ran = new Random();
		double mean;

		String[] features = new String[] { 
				SpotContrastAndSNRAnalyzerFactory.KEY, 
				SpotMorphologyAnalyzerFactory.KEY,
				SpotIntensityAnalyzerFactory.KEY } ;

		Map<String, String> featureNames = new HashMap<String, String>();
		featureNames.put(features[0], "Contrast");
		featureNames.put(features[1], "Morphology");
		featureNames.put(features[2], "Mean intensity");


		Map<String, double[]> fv = new HashMap<String, double[]>();
		for (String feature : features) {
			double[] val = new double[N_ITEMS];
			mean = ran.nextDouble() * 10;
			for (int j = 0; j < val.length; j++) 
				val[j] = ran.nextGaussian() + 5 + mean;
			fv.put(feature, val);
		}

		// Create GUI
		FilterPanel tp = new FilterPanel(fv, Arrays.asList(features), featureNames);
		tp.resetAxes();
		JFrame frame = new JFrame();
		frame.getContentPane().add(tp);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}


	/**
	 * A class that listen to the user typing a number, building a string representation
	 * as he types, then converting the string to a double after a wait time. 
	 * The number typed is used to set the threshold in the chart panel.
	 * @author Jean-Yves Tinevez
	 */
	private final class MyKeyListener implements KeyListener {

		private static final long WAIT_DELAY = 1; // s
		private static final double INCREASE_FACTOR = 0.1;
		private String strNumber = "";
		private ScheduledExecutorService ex;
		private ScheduledFuture<?> future;
		private boolean dotAdded = false;

		private final Runnable command = new Runnable() {
			@Override
			public void run() {
				// Convert to double and pass it to threshold value
				try { 
					double typedThreshold = NumberParser.parseDouble(strNumber);
					threshold = typedThreshold;
					redrawThresholdMarker();
				} catch (NumberFormatException nfe) { }
				// Reset
				ex = null;
				strNumber = "";
				dotAdded = false;
			}
		};

		@Override
		public void keyPressed(KeyEvent e) {
			// Is it arrow keys?
			if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT) {
				threshold -= INCREASE_FACTOR * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT) {
				threshold += INCREASE_FACTOR * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_KP_UP) {
				threshold = plot.getDomainAxis().getRange().getUpperBound();
				redrawThresholdMarker();
				return;
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
				threshold = plot.getDomainAxis().getRange().getLowerBound();
				redrawThresholdMarker();
				return;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {

			if (e.getKeyChar() < '0' || e.getKeyChar() > '9') {
				// Ok then it's number

				if (!dotAdded && e.getKeyChar() == '.') {
					// User added a decimal dot for the first and only time
					dotAdded  = true;
				} else {
					return;
				}
			} 

			if (ex == null) {
				// Create new waiting line
				ex = Executors.newSingleThreadScheduledExecutor();
				future = ex.schedule(command , WAIT_DELAY, TimeUnit.SECONDS);
			} else {
				// Reset waiting line
				future.cancel(false);
				future = ex.schedule(command , WAIT_DELAY, TimeUnit.SECONDS);
			}
			strNumber += e.getKeyChar();
		}
	}


}
