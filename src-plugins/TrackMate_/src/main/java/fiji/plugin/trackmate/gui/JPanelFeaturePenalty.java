package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.NumberParser;

import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

public class JPanelFeaturePenalty extends javax.swing.JPanel {

	private static final long serialVersionUID = 3848390144561204540L;
	private JComboBox jComboBoxFeature;
	private JNumericTextField jTextFieldFeatureWeight;
	private final List<String> features;
	private Map<String, String> featureNames;

	public JPanelFeaturePenalty(final List<String> features, final Map<String, String> featureNames, final int index) {
		super();
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
		jComboBoxFeature.setSelectedIndex(index);
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setSelectedFeature(String feature, double weight) {
		int index = features.indexOf(feature);
		if (index < 0) {
			return;
		}
		jComboBoxFeature.setSelectedIndex(index);
		jTextFieldFeatureWeight.setText(""+weight);
	}

	public String getSelectedFeature() {
		return features.get(jComboBoxFeature.getSelectedIndex());
	}

	public double getPenaltyWeight() {
		return NumberParser.parseDouble(jTextFieldFeatureWeight.getText());
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
				jComboBoxFeature.setBounds(2, 4, 205, 22);
				jComboBoxFeature.setFont(SMALL_FONT);
			}
			{
				jTextFieldFeatureWeight = new JNumericTextField();
				this.add(jTextFieldFeatureWeight);
				jTextFieldFeatureWeight.setText("1.0");
				jTextFieldFeatureWeight.setBounds(220, 4, 30, 22);
				jTextFieldFeatureWeight.setSize(TEXTFIELD_DIMENSION);
				jTextFieldFeatureWeight.setFont(SMALL_FONT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
