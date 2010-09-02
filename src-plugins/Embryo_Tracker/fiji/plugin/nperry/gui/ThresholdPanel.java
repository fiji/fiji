package fiji.plugin.nperry.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextSimpleAnnotation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import fiji.plugin.nperry.Utils;

/**
 * 
 */
public class ThresholdPanel <K extends Enum<K>>  extends javax.swing.JPanel {
	
	private static final Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
	private static final Font boldFont = smallFont; //.deriveFont(Font.BOLD);
	private static final Color annotationColor = new java.awt.Color(252,117,0);
	private static final long serialVersionUID = 1L;
	private static final String DATA_SERIES_NAME = "Data";
	private JComboBox jComboBoxFeature;
	private ChartPanel chartPanel;
	private JButton jButtonAutoThreshold;
	private JRadioButton jRadioButtonBelow;
	private JRadioButton jRadioButtonAbove;
	private HistogramDataset dataset;
	private JFreeChart chart;
	private XYPlot plot;
	private IntervalMarker intervalMarker;
	private double threshold;
	private EnumMap<K, double[]> valuesMap;
	private XYTextSimpleAnnotation annotation;
	private K key;
	private K[] allKeys;
	
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public ThresholdPanel(EnumMap<K, double[]> valuesMap, K selectedKey) {
		super();
		this.valuesMap = valuesMap;		
		this.allKeys = selectedKey.getDeclaringClass().getEnumConstants(); // get all enum values
		initGUI();
		jComboBoxFeature.setSelectedItem(selectedKey.toString());

	}
	
	public ThresholdPanel(EnumMap<K, double[]> valuesMap) {
		this(valuesMap, valuesMap.keySet().iterator().next());
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
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
	public K getKey() { return key; }
	
	/**
	 * Add an {@link ActionListener} to this panel. The {@link ActionListener} will
	 * be notified when a change happens to the threshold displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove an ActionListener. 
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return listeners.remove(listener);
	}
	
	public Collection<ActionListener> getActionListeners() {
		return listeners;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void fireThresholdChanged() {
		ActionEvent ae = new ActionEvent(this, 0, "ThresholdChanged");
		for (ActionListener al : listeners) 
			al.actionPerformed(ae);
	}
	
	private void comboBoxSelectionChanged() {
		key = allKeys[jComboBoxFeature.getSelectedIndex()];
		double[] values = valuesMap.get(key);
		if (null == values) {
			dataset = new HistogramDataset();
			threshold = Double.NaN;
			annotation.setLocation(0.5f, 0.5f);
			annotation.setText("No data");
			fireThresholdChanged();
		} else {
			int nBins = Utils.getNBins(values);
			dataset = new HistogramDataset();
			if (nBins > 1)
				dataset.addSeries(DATA_SERIES_NAME, values, nBins);
		}
		plot.setDataset(dataset);
		resetAxes();
		autoThreshold(); // Will fire the fireThresholdChanged();
	}
	
	private void autoThreshold() {
		K selectedFeature = allKeys[jComboBoxFeature.getSelectedIndex()];
		double[] values = valuesMap.get(selectedFeature);
		if (null != values) {
			threshold = Utils.otsuThreshold(valuesMap.get(selectedFeature));
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
				String[] keyNames = new String[allKeys.length];
				for (int i = 0; i < keyNames.length; i++) 
					keyNames[i] = allKeys[i].toString();
				ComboBoxModel jComboBoxFeatureModel = new DefaultComboBoxModel(keyNames);
				jComboBoxFeature = new JComboBox();
				this.add(jComboBoxFeature, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
				jComboBoxFeature.setModel(jComboBoxFeatureModel);
				jComboBoxFeature.setFont(boldFont);
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
				jButtonAutoThreshold.setFont(smallFont);
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
				jRadioButtonAbove.setFont(smallFont);
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
				jRadioButtonBelow.setFont(smallFont);
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
		dataset = new HistogramDataset();
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
		
		annotation = new XYTextSimpleAnnotation(chartPanel);
		annotation.setFont(smallFont.deriveFont(Font.BOLD));
		annotation.setColor(annotationColor.darker());
		plot.addAnnotation(annotation);
	}
	
	private double getXFromChartEvent(MouseEvent mouseEvent) {
		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue(mouseEvent.getX(), plotArea, plot.getDomainAxisEdge());
	}
	
	private void redrawThresholdMarker() {
		K selectedFeature = allKeys[jComboBoxFeature.getSelectedIndex()];
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
		if (threshold > 0.85 * plot.getDomainAxis().getUpperBound()) 
			x = (float) (threshold - 0.15 * plot.getDomainAxis().getRange().getLength());
		else 
			x = (float) (threshold + 0.05 * plot.getDomainAxis().getRange().getLength());
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
		fiji.plugin.nperry.Feature[] features = new fiji.plugin.nperry.Feature[] { 
				fiji.plugin.nperry.Feature.CONTRAST, 
				fiji.plugin.nperry.Feature.ELLIPSOIDFIT_AXISPHI_A, 
				fiji.plugin.nperry.Feature.MEAN_INTENSITY };
		EnumMap<fiji.plugin.nperry.Feature, double[]> fv = new EnumMap<fiji.plugin.nperry.Feature, double[]>(fiji.plugin.nperry.Feature.class);
		for (fiji.plugin.nperry.Feature feature : features) {
			double[] val = new double[N_ITEMS];
			mean = ran.nextDouble() * 10;
			for (int j = 0; j < val.length; j++) 
				val[j] = ran.nextGaussian() + 5 + mean;
			fv.put(feature, val);
		}
		
		// Create GUI
		ThresholdPanel<fiji.plugin.nperry.Feature> tp = new ThresholdPanel<fiji.plugin.nperry.Feature>(fv);
		tp.resetAxes();
		JFrame frame = new JFrame();
		frame.getContentPane().add(tp);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
