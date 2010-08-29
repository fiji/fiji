package fiji.plugin.nperry.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class ThresholdPanel extends javax.swing.JPanel {
	
	private static final long serialVersionUID = 1L;
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
	
	public ThresholdPanel() {
		super();
		initGUI();
		
	}
	
	private void initGUI() {
		try {
			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.columnWidths = new int[] {7, 20};
			thisLayout.rowHeights = new int[] {7, 7, 7};
			thisLayout.columnWeights = new double[] {0.1, 0.1};
			thisLayout.rowWeights = new double[] {0.0, 1.0, 0.0};
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(266, 137));
			thisLayout.rowWeights = new double[] {0.0, 1.0, 0.0};
			thisLayout.rowHeights = new int[] {10, 7, 15};
			thisLayout.columnWeights = new double[] {0.0, 0.0, 1.0};
			thisLayout.columnWidths = new int[] {7, 20, 7};
			{
				ComboBoxModel jComboBoxFeatureModel = 
					new DefaultComboBoxModel(
							new String[] { "Item One", "Item Two" });
				jComboBoxFeature = new JComboBox();
				this.add(jComboBoxFeature, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 6), 0, 0));
				jComboBoxFeature.setModel(jComboBoxFeatureModel);
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
			}
			{
				jRadioButtonAbove = new JRadioButton();
				this.add(jRadioButtonAbove, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
				jRadioButtonAbove.setText("Above");
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
		
	}
	
	private double getXFromChartEvent(MouseEvent mouseEvent) {
		Point2D p = chartPanel.translateScreenToJava2D(mouseEvent.getPoint());
		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
	}
	
	private void redrawThresholdMarker() {
		if (jRadioButtonAbove.isSelected()) {
			intervalMarker.setStartValue(threshold);
			intervalMarker.setEndValue(plot.getDomainAxis().getUpperBound());
		} else {
			intervalMarker.setStartValue(plot.getDomainAxis().getLowerBound());
			intervalMarker.setEndValue(threshold);
		}
	}
	
	private void generateRandomData() {
		int N = 10000;
		Random ran = new Random();
		double[] values = new double[N];
		for (int i = 0; i < N; i++)
			values[i] = 5 + ran.nextGaussian();
			
		
		dataset.addSeries("H", values, 100);
		
	}
	
	private void resetAxes() {
		plot.getRangeAxis().setLowerMargin(0);
		plot.getRangeAxis().setUpperMargin(0);
		plot.getDomainAxis().setLowerMargin(0);
		plot.getDomainAxis().setUpperMargin(0);
	}
	
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	*/
	public static void main(String[] args) {
		ThresholdPanel tp = new ThresholdPanel();
		tp.generateRandomData();
		tp.resetAxes();
		JFrame frame = new JFrame();
		frame.getContentPane().add(tp);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	

}
