package fiji.plugin.nperry.gui;
import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.JButton;

import javax.swing.Box;
import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;


public class ThresholdGuiPanel extends javax.swing.JPanel {
	private static final long serialVersionUID = 1L;
	private JScrollPane jScrollPaneThresholds;
	private JButton jButtonRemoveThreshold;
	private JButton jButtonNext;
	private JPanel jPanelAllThresholds;
	private JButton jButtonAddThreshold;
	private JPanel jPanelButtons;

	private Stack<ThresholdPanel> thresholdPanels = new Stack<ThresholdPanel>();
	private Stack<Component> struts = new Stack<Component>();
	private Collection<Spot> spots;
	private HashMap<Feature, double[]> featureValues = new HashMap<Feature, double[]>(Feature.values().length);
	private int newFeatureIndex;

	
	public ThresholdGuiPanel(Collection<Spot> spots) {
		super();
		this.spots = spots;
		prepareDataArrays();
		initGUI();
		addThresholdPanel();
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void prepareDataArrays() {
		int index;
		for(Feature feature : Feature.values()) {
			// Make a double array to comply to JFreeChart histograms
			double[] values = new double[spots.size()];
			index = 0;
			for (Spot spot : spots) {
				values[index] = spot.getFeature(feature);
				index++;
			}
			featureValues.put(feature, values);
		}
	}
	
	private void addThresholdPanel() {
		ThresholdPanel tp = new ThresholdPanel(featureValues, Feature.values()[newFeatureIndex]);
		newFeatureIndex++;
		if (newFeatureIndex >= Feature.values().length) 
			newFeatureIndex = 0;
		Component strut = Box.createVerticalStrut(5);
		struts.push(strut);
		thresholdPanels.push(tp);
		jPanelAllThresholds.add(tp);
		jPanelAllThresholds.add(strut);
		jPanelAllThresholds.revalidate();
	}
	
	private void removeThresholdPanel() {
		try {
			ThresholdPanel tp = thresholdPanels.pop();
			Component strut = struts.pop();
			jPanelAllThresholds.remove(strut);
			jPanelAllThresholds.remove(tp);
			jPanelAllThresholds.repaint();
		} catch (EmptyStackException ese) {	}
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			setPreferredSize(new Dimension(270, 500));
			{
				jScrollPaneThresholds = new JScrollPane();
				this.add(jScrollPaneThresholds, BorderLayout.CENTER);
				jScrollPaneThresholds.setPreferredSize(new java.awt.Dimension(250, 389));
				jScrollPaneThresholds.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneThresholds.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				{
					jPanelAllThresholds = new JPanel();
					BoxLayout jPanelAllThresholdsLayout = new BoxLayout(jPanelAllThresholds, BoxLayout.Y_AXIS);
					jPanelAllThresholds.setLayout(jPanelAllThresholdsLayout);
					jScrollPaneThresholds.setViewportView(jPanelAllThresholds);
				}
			}
			{
				jPanelButtons = new JPanel();
				BoxLayout jPanelButtonsLayout = new BoxLayout(jPanelButtons, javax.swing.BoxLayout.X_AXIS);
				jPanelButtons.setLayout(jPanelButtonsLayout);
				this.add(jPanelButtons, BorderLayout.SOUTH);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(250, 41));
				{
					jPanelButtons.add(Box.createHorizontalStrut(5));
					jButtonAddThreshold = new JButton();
					jPanelButtons.add(jButtonAddThreshold);
					jButtonAddThreshold.setText("+");
					jButtonAddThreshold.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							addThresholdPanel();
						}
					});
				}
				{
					jPanelButtons.add(Box.createHorizontalStrut(5));
					jButtonRemoveThreshold = new JButton();
					jPanelButtons.add(jButtonRemoveThreshold);
					jButtonRemoveThreshold.setText("-");
					jButtonRemoveThreshold.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							removeThresholdPanel();
						}
					});
				}
				{
					jPanelButtons.add(Box.createHorizontalGlue());
					jButtonNext = new JButton();
					jPanelButtons.add(jButtonNext);
					jButtonNext.setText("Next >>");
					jPanelButtons.add(Box.createHorizontalStrut(5));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	*/
	public static void main(String[] args) {
		// Generate fake Spot data
		final int NSPOT = 100;
		final Random rn = new Random();
		ArrayList<Spot> spots = new ArrayList<Spot>(NSPOT);
		Spot spot;
		for (int i = 0; i < NSPOT; i++) {
			spot = new Spot(new float[] {0, 0, 0});
			for (Feature feature : Feature.values())
				spot.putFeature(feature, (float) (rn.nextGaussian()+5));
			spots.add(spot);
		}
		
		// Generate GUI
		JFrame frame = new JFrame();
		frame.getContentPane().add(new ThresholdGuiPanel(spots));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
