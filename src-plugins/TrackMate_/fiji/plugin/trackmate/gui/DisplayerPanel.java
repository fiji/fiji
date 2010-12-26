package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;

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
public class DisplayerPanel extends ActionListenablePanel {

	private static final long serialVersionUID = 1L;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public ActionEvent TRACK_DISPLAY_MODE_CHANGED 	= new ActionEvent(this, 0, "Track display mode changed");
	public ActionEvent TRACK_VISIBILITY_CHANGED 	= new ActionEvent(this, 1, "Track visibility changed");
	public ActionEvent SPOT_COLOR_MODE_CHANGED; // instantiate later, from color gui panel
	public ActionEvent SPOT_VISIBILITY_CHANGED 		= new ActionEvent(this, 2, "Spot visibility changed");

	private JLabel jLabelTrackDisplayMode;
	private JComboBox jComboBoxDisplayMode;
	private JLabel jLabelDisplayOptions;
	private JPanel jPanelSpotOptions;
	private JCheckBox jCheckBoxDisplaySpots;
	private JPanel jPanelTrackOptions;
	private JCheckBox jCheckBoxDisplayTracks;
	private JCheckBox jCheckBoxLimitDepth;
	private JTextField jTextFieldFrameDepth;
	private JLabel jLabelFrameDepth;
	private JPanelSpotColorGUI jPanelSpotColor;
	private EnumMap<Feature, double[]> featureValues;

	
	
	public DisplayerPanel(EnumMap<Feature, double[]> featureValues) {
		super();
		this.featureValues = featureValues;
		initGUI();
	}
	
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public TrackDisplayMode getTrackDisplayMode() {
		return TrackDisplayMode.values()[jComboBoxDisplayMode.getSelectedIndex()];
	}
	
	public int getTrackDisplayDepth() {
		if (jCheckBoxLimitDepth.isSelected())
			return Integer.parseInt(jTextFieldFrameDepth.getText());
		else
			return Integer.MAX_VALUE;
	}
	
	public boolean isDisplayTrackSelected() {
		return jCheckBoxDisplayTracks.isSelected();
	}
	
	public boolean isDisplaySpotSelected() {
		return jCheckBoxDisplaySpots.isSelected();
	}
	
	public Feature getColorSpotByFeature() {
		return jPanelSpotColor.setColorByFeature;
	}
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void displayTrackFlagChanged(boolean display) {
		jPanelTrackOptions.setEnabled(display);
		fireAction(TRACK_VISIBILITY_CHANGED);
	}
	
	private void spotColorModeChanged() {
		fireAction(SPOT_COLOR_MODE_CHANGED);
	}
	private void displaySpotFlagChanged(boolean display) {
		jPanelSpotOptions.setEnabled(display);
		fireAction(SPOT_VISIBILITY_CHANGED);
	}
	
	
	private void trackDisplayModeChanged(ActionEvent e) {
		fireAction(TRACK_DISPLAY_MODE_CHANGED);
	}
	
	private void initGUI() {
		final ActionListener trackDisplayModeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trackDisplayModeChanged(e);
			}
		};
		
		
		try {
			this.setPreferredSize(new java.awt.Dimension(268, 469));
			this.setSize(300, 500);
			this.setLayout(null);
			{
				jPanelTrackOptions = new JPanel() {
					private static final long serialVersionUID = -1805693239189343720L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				FlowLayout jPanelSpotOptionsLayout = new FlowLayout();
				jPanelSpotOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelTrackOptions.setLayout(jPanelSpotOptionsLayout);
				this.add(jPanelTrackOptions);
				jPanelTrackOptions.setBounds(10, 287, 280, 117);
				jPanelTrackOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					jLabelTrackDisplayMode = new JLabel();
					jPanelTrackOptions.add(jLabelTrackDisplayMode);
					jLabelTrackDisplayMode.setText("Track display mode:");
					jLabelTrackDisplayMode.setBounds(10, 163, 268, 15);
					jLabelTrackDisplayMode.setFont(FONT);
					jLabelTrackDisplayMode.setPreferredSize(new java.awt.Dimension(261, 14));
				}
				{
					TrackDisplayMode[] modes = SpotDisplayer.TrackDisplayMode.values();
					String[] keyNames = new String[modes.length];
					for (int i = 0; i < keyNames.length; i++)
						keyNames[i] = modes[i].toString();
					ComboBoxModel jComboBoxDisplayModeModel = new DefaultComboBoxModel(keyNames);
					jComboBoxDisplayMode = new JComboBox();
					jPanelTrackOptions.add(jComboBoxDisplayMode);
					jComboBoxDisplayMode.setModel(jComboBoxDisplayModeModel);
					jComboBoxDisplayMode.setSelectedIndex(SpotDisplayer.DEFAULT_TRACK_DISPLAY_MODE.ordinal());
					jComboBoxDisplayMode.setFont(SMALL_FONT);
					jComboBoxDisplayMode.setPreferredSize(new java.awt.Dimension(265, 27));
					jComboBoxDisplayMode.addActionListener(trackDisplayModeListener);
				}
				{
					jCheckBoxLimitDepth = new JCheckBox();
					jPanelTrackOptions.add(jCheckBoxLimitDepth);
					jCheckBoxLimitDepth.setText("Limit frame depth");
					jCheckBoxLimitDepth.setBounds(6, 216, 272, 23);
					jCheckBoxLimitDepth.setFont(FONT);
					jCheckBoxLimitDepth.setSelected(true);
					jCheckBoxLimitDepth.setPreferredSize(new java.awt.Dimension(259, 23));
					jCheckBoxLimitDepth.addActionListener(trackDisplayModeListener);
				}
				{
					jLabelFrameDepth = new JLabel();
					jPanelTrackOptions.add(jLabelFrameDepth);
					jLabelFrameDepth.setText("Frame depth:");
					jLabelFrameDepth.setFont(SMALL_FONT);
					jLabelFrameDepth.setPreferredSize(new java.awt.Dimension(103, 14));
				}
				{
					jTextFieldFrameDepth = new JTextField();
					jPanelTrackOptions.add(jTextFieldFrameDepth);
					jTextFieldFrameDepth.setText("10");
					jTextFieldFrameDepth.setFont(SMALL_FONT);
					jTextFieldFrameDepth.setSize(TEXTFIELD_DIMENSION);
					jTextFieldFrameDepth.setText(""+SpotDisplayer.DEFAULT_TRACK_DISPLAY_DEPTH);
					jTextFieldFrameDepth.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldFrameDepth.addActionListener(trackDisplayModeListener);
				}
			}
			{
				jCheckBoxDisplayTracks = new JCheckBox();
				this.add(jCheckBoxDisplayTracks);
				jCheckBoxDisplayTracks.setText("Display tracks");
				jCheckBoxDisplayTracks.setFont(FONT);
				jCheckBoxDisplayTracks.setBounds(10, 263, 233, 23);
				jCheckBoxDisplayTracks.setSelected(true);
				jCheckBoxDisplayTracks.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						displayTrackFlagChanged(jCheckBoxDisplayTracks.isSelected());
					}
				});
			}
			{
				jCheckBoxDisplaySpots = new JCheckBox();
				this.add(jCheckBoxDisplaySpots);
				jCheckBoxDisplaySpots.setText("Display spots");
				jCheckBoxDisplaySpots.setFont(FONT);
				jCheckBoxDisplaySpots.setBounds(10, 38, 280, 23);
				jCheckBoxDisplaySpots.setSelected(true);
				jCheckBoxDisplaySpots.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						displaySpotFlagChanged(jCheckBoxDisplaySpots.isSelected());
					}
				});
			}
			{
				jPanelSpotOptions = new JPanel() {
					private static final long serialVersionUID = 3259314983744108471L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				this.add(jPanelSpotOptions);
				jPanelSpotOptions.setBounds(10, 63, 280, 67);
				jPanelSpotOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					jPanelSpotColor = new JPanelSpotColorGUI(this);
					jPanelSpotColor.featureValues = featureValues;
					jPanelSpotOptions.add(jPanelSpotColor);
					SPOT_COLOR_MODE_CHANGED  = jPanelSpotColor.COLOR_FEATURE_CHANGED;
					jPanelSpotColor.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							spotColorModeChanged();
						}
					});
				}
			}
			{
				jLabelDisplayOptions = new JLabel();
				this.add(jLabelDisplayOptions);
				jLabelDisplayOptions.setText("Display options");
				jLabelDisplayOptions.setFont(FONT.deriveFont(Font.BOLD));
				jLabelDisplayOptions.setBounds(10, 11, 280, 14);
				jLabelDisplayOptions.setHorizontalAlignment(SwingConstants.CENTER);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	*/
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new DisplayerPanel(null));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
