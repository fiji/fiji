package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import fiji.plugin.trackmate.util.TMUtils;

/**
 * A simple Panel to allow the selection of a X key amongst an enum, and of multiple Y keys
 * from the same enum. This is intended as a GUI panel to prepare for the plotting of data.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - January 2011
 */
public class FeaturePlotSelectionPanel extends javax.swing.JPanel {
	
	private static final long serialVersionUID = 4995336293032921408L;
	private static final ImageIcon ADD_ICON = new ImageIcon(FeaturePlotSelectionPanel.class.getResource("resources/add.png"));
	private static final ImageIcon REMOVE_ICON = new ImageIcon(FeaturePlotSelectionPanel.class.getResource("resources/delete.png"));
	private static final Dimension BUTTON_SIZE = new Dimension(24, 24);
	private static final Dimension COMBO_BOX_SIZE = new java.awt.Dimension(150, 22);
	private static final int MAX_FEATURE_ALLOWED = 10;
	
	
	private JLabel jLabelXFeature;
	private JScrollPane jScrollPaneYFeatures;
	private JButton jButtonRemove;
	private JPanel jPanelYFeatures;
	private JButton jButtonAdd;
	private JPanel jPanelButtons;
	private JLabel jLabelYFeatures;
	private JComboBox jComboBoxXFeature;
	
	private Stack<JComboBox> comboBoxes = new Stack<JComboBox>();
	private Stack<Component> struts = new Stack<Component>();
	private String xKey;
	private List<String> features;
	private Map<String, String> featureNames;
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public FeaturePlotSelectionPanel(String xKey, List<String> features, Map<String, String> featureNames) {
		super();
		this.xKey = xKey;
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
		addFeature();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return the enum constant selected in the X combo-box feature.
	 */
	public String getXKey() {
		return features.get(jComboBoxXFeature.getSelectedIndex());
	}
	
	/**
	 * Return a set of the keys selected in the Y feature panel. Since we
	 * use a {@link Set}, duplicates are trimmed.
	 */
	public Set<String> getYKeys() {
		Set<String> yKeys = new HashSet<String>(comboBoxes.size());
		for(JComboBox box : comboBoxes)
			yKeys.add(features.get(box.getSelectedIndex()));
		return yKeys;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void addFeature() {
		
		if (comboBoxes.size() > MAX_FEATURE_ALLOWED)
			return;
		
		ComboBoxModel jComboBoxYFeatureModel = new DefaultComboBoxModel(
				TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[] {}));
		JComboBox jComboBoxYFeature = new JComboBox();
		jComboBoxYFeature.setModel(jComboBoxYFeatureModel);
		jComboBoxYFeature.setPreferredSize(COMBO_BOX_SIZE);
		jComboBoxYFeature.setMaximumSize(COMBO_BOX_SIZE);
		jComboBoxYFeature.setFont(SMALL_FONT);
		
		if (!comboBoxes.isEmpty()) {
			int newIndex = comboBoxes.get(comboBoxes.size()-1).getSelectedIndex()+1;
			if (newIndex >= features.size())
				newIndex = 0;
			jComboBoxYFeature.setSelectedIndex(newIndex);
		}
		
		Component strut = Box.createVerticalStrut(5);
		jPanelYFeatures.add(strut);
		jPanelYFeatures.add(jComboBoxYFeature);
		jPanelYFeatures.revalidate();
		comboBoxes.push(jComboBoxYFeature);
		struts.push(strut);
	}
	
	private void removeFeature() {
		if (comboBoxes.isEmpty())
			return;		
		jPanelYFeatures.remove(comboBoxes.pop());
		jPanelYFeatures.remove(struts.pop());
		jPanelYFeatures.revalidate();
		jPanelYFeatures.repaint();
		
		
	}
	
	private void initGUI() {
		try {
			this.setLayout(null);
			this.setPreferredSize(new java.awt.Dimension(170, 250));
			{
				jLabelXFeature = new JLabel();
				this.add(jLabelXFeature);
				jLabelXFeature.setText("Feature for X axis:");
				jLabelXFeature.setFont(FONT);
				jLabelXFeature.setBounds(10, 5, 148, 14);
			}
			{
				ComboBoxModel jComboBoxXFeatureModel = new DefaultComboBoxModel(
						TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[] {}));
				jComboBoxXFeature = new JComboBox();
				this.add(jComboBoxXFeature);
				jComboBoxXFeature.setModel(jComboBoxXFeatureModel);
				jComboBoxXFeature.setFont(SMALL_FONT);
				jComboBoxXFeature.setBounds(10, 25, COMBO_BOX_SIZE.width, COMBO_BOX_SIZE.height);
				jComboBoxXFeature.setSelectedIndex(features.indexOf(xKey));
			}
			{
				jLabelYFeatures = new JLabel();
				this.add(jLabelYFeatures);
				jLabelYFeatures.setText("Features for Y axis:");
				jLabelYFeatures.setFont(FONT);
				jLabelYFeatures.setBounds(10, 50, 148, 14);
			}
			{
				jScrollPaneYFeatures = new JScrollPane();
				this.add(jScrollPaneYFeatures);
				jScrollPaneYFeatures.setPreferredSize(new java.awt.Dimension(169, 137));
				jScrollPaneYFeatures.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneYFeatures.setBounds(0, 64, 168, 152);
				{
					jPanelYFeatures = new JPanel();
					jPanelYFeatures.setLayout(new BoxLayout(jPanelYFeatures, BoxLayout.Y_AXIS));
					jScrollPaneYFeatures.setViewportView(jPanelYFeatures);
				}
			}
			{
				jPanelButtons = new JPanel();
				BoxLayout jPanelButtonsLayout = new BoxLayout(jPanelButtons, javax.swing.BoxLayout.X_AXIS);
				jPanelButtons.setLayout(jPanelButtonsLayout);
				this.add(jPanelButtons);
				jPanelButtons.setBounds(10, 216, 148, 29);
				{
					jButtonAdd = new JButton();
					jPanelButtons.add(jButtonAdd);
					jButtonAdd.setIcon(ADD_ICON);
					jButtonAdd.setMaximumSize(BUTTON_SIZE);
					jButtonAdd.addActionListener(new ActionListener() {	
						@Override
						public void actionPerformed(ActionEvent e) {
							addFeature();
						}
					});
				}
				{
					jButtonRemove = new JButton();
					jPanelButtons.add(jButtonRemove);
					jButtonRemove.setIcon(REMOVE_ICON);
					jButtonRemove.setMaximumSize(BUTTON_SIZE);
					jButtonRemove.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							removeFeature();
						}
					});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
