package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.JButton;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JPanel;

import fiji.plugin.trackmate.SpotFeature;

public class JPanelFeatureSelectionGui extends javax.swing.JPanel {

	private static final long serialVersionUID = 7567178804475300833L;
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private JPanel jPanelButtons;
	private JButton jButtonRemove;
	private JButton jButtonAdd;
	
	private Stack<JPanelFeatureRatioThreshold> featurePanels = new Stack<JPanelFeatureRatioThreshold>();
	
	public JPanelFeatureSelectionGui() {
		super();
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public Map<SpotFeature, Double>	 getFeatureRatios() {
		Map<SpotFeature, Double> ratios = new HashMap<SpotFeature, Double>(featurePanels.size());
		for (JPanelFeatureRatioThreshold panel : featurePanels) 
			ratios.put(panel.getSelectedFeature(), panel.getRatioThreshold());
		return ratios;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		ArrayList<Component> components = new ArrayList<Component>(3 + featurePanels.size());
		components.add(jPanelButtons);
		components.add(jButtonAdd);
		components.add(jButtonRemove);
		components.addAll(featurePanels);
		for(Component component : components)
			component.setEnabled(enabled);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void addButtonPushed() {
		JPanelFeatureRatioThreshold panel = new JPanelFeatureRatioThreshold();
		featurePanels.push(panel);
		remove(jPanelButtons);
		add(panel);
		add(jPanelButtons);
		Dimension size = getSize();
		setSize(size.width, size.height + panel.getSize().height);
		revalidate();
	}
	
	private void removeButtonPushed() {
		if (featurePanels.isEmpty())
			return;
		JPanelFeatureRatioThreshold panel = featurePanels.pop();
		remove(panel);
		Dimension size = getSize();
		setSize(size.width, size.height - panel.getSize().height);
		revalidate();
	}
	
	private void initGUI() {
		try {
			BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
			this.setLayout(layout);
			{
				jPanelButtons = new JPanel();
				this.add(jPanelButtons);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(260, 25));
				jPanelButtons.setLayout(null);
				{
					jButtonRemove = new JButton();
					jPanelButtons.add(jButtonRemove);
					jButtonRemove.setIcon(new ImageIcon(getClass().getResource(REMOVE_ICON)));
					jButtonRemove.setBounds(48, 5, 21, 22);
					jButtonRemove.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							removeButtonPushed();
							
						}
					});
				}
				{
					jButtonAdd = new JButton();
					jPanelButtons.add(jButtonAdd);
					jButtonAdd.setIcon(new ImageIcon(getClass().getResource(ADD_ICON)));
					jButtonAdd.setBounds(12, 5, 24, 22);
					jButtonAdd.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							addButtonPushed();
						}
					});
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
		JFrame frame = new JFrame();
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setSize(260, 300);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setSize(260, 300);
		JPanelFeatureSelectionGui instance = new JPanelFeatureSelectionGui();
		scrollPane.setViewportView(instance);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		frame.getContentPane().add(mainPanel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
