package fiji.plugin.nperry.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Random;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
	private JPanel jPanelHistogram;
	private JButton jButtonAutoThreshold;
	private JRadioButton jRadioButtonDisplay;
	private HistogramDataset dataset;
	private JFreeChart chart;
	private XYPlot plot;

	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	*/
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new ThresholdPanel());
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
	public ThresholdPanel() {
		super();
		initGUI();
		
		generateRandomData();
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
			thisLayout.columnWeights = new double[] {0.1, 1.0};
			thisLayout.columnWidths = new int[] {7, 7};
			{
				ComboBoxModel jComboBoxFeatureModel = 
					new DefaultComboBoxModel(
							new String[] { "Item One", "Item Two" });
				jComboBoxFeature = new JComboBox();
				this.add(jComboBoxFeature, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
				jComboBoxFeature.setModel(jComboBoxFeatureModel);
			}
			{
				createHistogramPlot();
				jPanelHistogram = new ChartPanel(chart);
				jPanelHistogram.setBackground(new Color(1, 1, 0, 1));
				
				jPanelHistogram.setPreferredSize(new Dimension(0, 0));
				this.add(jPanelHistogram, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jPanelHistogram.setForeground(new java.awt.Color(0,0,0));
				jPanelHistogram.setOpaque(false);
			}
			{
				jButtonAutoThreshold = new JButton();
				this.add(jButtonAutoThreshold, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jButtonAutoThreshold.setText("Auto");
			}
			{
				jRadioButtonDisplay = new JRadioButton();
				this.add(jRadioButtonDisplay, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void createHistogramPlot() {
		dataset = new HistogramDataset();
		chart = ChartFactory.createHistogram(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
		
		plot = chart.getXYPlot();
		
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setShadowVisible(false);
		renderer.setMargin(0);
		renderer.setDrawBarOutline(true);
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setSeriesPaint(0, new Color(1, 1, 1, 0));
		
	
		
//		plot.setBackgroundAlpha(0);
		plot.setOutlineVisible(false);
		plot.setDomainCrosshairVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		plot.getRangeAxis().setVisible(false);
		plot.getDomainAxis().setVisible(false);
		plot.setBackgroundPaint(new Color(1, 1, 1, 0));
		
		chart.setBorderVisible(true);
		chart.setBackgroundPaint(new Color(1, 0, 0));
	}
	
	private void generateRandomData() {
		int N = 10000;
		Random ran = new Random();
		double[] values = new double[N];
		for (int i = 0; i < N; i++)
			values[i] = 5 + ran.nextGaussian();
			
		
		dataset.addSeries("H", values, 100);
		
	}

}
