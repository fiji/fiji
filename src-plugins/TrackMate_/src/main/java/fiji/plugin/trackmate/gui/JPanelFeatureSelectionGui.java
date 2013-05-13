package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class JPanelFeatureSelectionGui extends javax.swing.JPanel {

	private static final long serialVersionUID = -891462567905389989L;
	private static final String ADD_ICON = "images/add.png";
	private static final String REMOVE_ICON = "images/delete.png";

	private JPanel jPanelButtons;
	private JButton jButtonRemove;
	private JButton jButtonAdd;

	private Stack<JPanelFeaturePenalty> featurePanels = new Stack<JPanelFeaturePenalty>();
	private List<String> features;
	private Map<String, String> featureNames;
	private int index;

	public JPanelFeatureSelectionGui() {
		initGUI();
		index = -1;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the features and their names that should be presented by this GUI.
	 * The user will be allowed to choose amongst the given features. 
	 */
	public void setDisplayFeatures(List<String> features, Map<String, String> featureNames) {
		this.features = features;
		this.featureNames = featureNames;
	}

	public void setSelectedFeaturePenalties(Map<String, Double> penalties) {
		// Remove old features
		while (!featurePanels.isEmpty()) {
			JPanelFeaturePenalty panel = featurePanels.pop();
			remove(panel);
		}
		// Remove buttons 
		remove(jPanelButtons);
		// Add new panels
		for (String feature : penalties.keySet()) {
			int localIndex = features.indexOf(feature);
			JPanelFeaturePenalty panel = new JPanelFeaturePenalty(features, featureNames, localIndex);
			panel.setSelectedFeature(feature, penalties.get(feature));
			add(panel);
			featurePanels.push(panel);
		}
		// Add buttons back
		add(jPanelButtons);
	}

	public Map<String, Double>	getFeaturePenalties() {
		Map<String, Double> weights = new HashMap<String, Double>(featurePanels.size());
		for (JPanelFeaturePenalty panel : featurePanels) 
			weights.put(panel.getSelectedFeature(), panel.getPenaltyWeight());
		return weights;
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
		index = index + 1;
		if (index >= features.size())
			index = 0;
		JPanelFeaturePenalty panel = new JPanelFeaturePenalty(features, featureNames, index);
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
		JPanelFeaturePenalty panel = featurePanels.pop();
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

}
