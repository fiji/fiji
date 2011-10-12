package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;

import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import fiji.plugin.trackmate.util.TMUtils;

public class JPanelFeaturePenalty extends javax.swing.JPanel {

	private static final long serialVersionUID = 3848390144561204540L;
	private JComboBox jComboBoxFeature;
	private JNumericTextField jTextFieldFeatureWeight;
	private final List<String> features;
	private Map<String, String> featureNames;

	public JPanelFeaturePenalty(List<String> features, Map<String, String> featureNames, int index) {
		super();
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
		jComboBoxFeature.setSelectedIndex(index);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public String getSelectedFeature() {
		return features.get(jComboBoxFeature.getSelectedIndex());
	}
	
	public double getPenaltyWeight() {
		return Double.parseDouble(jTextFieldFeatureWeight.getText());
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		jComboBoxFeature.setEnabled(enabled);
		jTextFieldFeatureWeight.setEnabled(enabled);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(280, 40));
			this.setSize(280, 40);
			this.setLayout(null);
			{
				ComboBoxModel jComboBoxFeatureModel = new DefaultComboBoxModel(
						TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[] {}));
				jComboBoxFeature = new JComboBox();
				this.add(jComboBoxFeature);
				jComboBoxFeature.setModel(jComboBoxFeatureModel);
				jComboBoxFeature.setBounds(12, 4, 214, 22);
				jComboBoxFeature.setFont(SMALL_FONT);
			}
			{
				jTextFieldFeatureWeight = new JNumericTextField();
				this.add(jTextFieldFeatureWeight);
				jTextFieldFeatureWeight.setText("1.0");
				jTextFieldFeatureWeight.setBounds(238, 4, 30, 22);
				jTextFieldFeatureWeight.setSize(TEXTFIELD_DIMENSION);
				jTextFieldFeatureWeight.setFont(SMALL_FONT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
