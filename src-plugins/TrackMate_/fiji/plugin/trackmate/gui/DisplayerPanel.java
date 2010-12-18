package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;

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

	private JLabel jLabelTrackDisplayMode;
	private JComboBox jComboBoxDisplayMode;
	private JCheckBox jCheckBoxLimitDepth;
	private JTextField jTextFieldFrameDepth;
	private JLabel jLabelFrameDepth;

	
	
	public DisplayerPanel() {
		super();
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
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void trackDisplayModeChanged(ActionEvent e) {
		fireAction(e);
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
				jLabelTrackDisplayMode = new JLabel();
				this.add(jLabelTrackDisplayMode);
				jLabelTrackDisplayMode.setText("Track display mode:");
				jLabelTrackDisplayMode.setBounds(12, 12, 268, 15);
				jLabelTrackDisplayMode.setFont(FONT);
			}
			{
				TrackDisplayMode[] modes = SpotDisplayer.TrackDisplayMode.values();
				String[] keyNames = new String[modes.length];
				for (int i = 0; i < keyNames.length; i++)
					keyNames[i] = modes[i].toString();
				ComboBoxModel jComboBoxDisplayModeModel = new DefaultComboBoxModel(keyNames);
				jComboBoxDisplayMode = new JComboBox();
				this.add(jComboBoxDisplayMode);
				jComboBoxDisplayMode.setModel(jComboBoxDisplayModeModel);
				jComboBoxDisplayMode.setSelectedIndex(SpotDisplayer.DEFAULT_TRACK_DISPLAY_MODE.ordinal());
				jComboBoxDisplayMode.setBounds(7, 32, 276, 27);
				jComboBoxDisplayMode.setFont(SMALL_FONT);
				jComboBoxDisplayMode.addActionListener(trackDisplayModeListener);
			}
			{
				jCheckBoxLimitDepth = new JCheckBox();
				this.add(jCheckBoxLimitDepth);
				jCheckBoxLimitDepth.setText("Limit frame depth");
				jCheckBoxLimitDepth.setBounds(12, 62, 272, 23);
				jCheckBoxLimitDepth.setFont(FONT);
				jCheckBoxLimitDepth.setSelected(true);
				jCheckBoxLimitDepth.addActionListener(trackDisplayModeListener);
			}
			{
				jLabelFrameDepth = new JLabel();
				this.add(jLabelFrameDepth);
				jLabelFrameDepth.setText("Frame depth:");
				jLabelFrameDepth.setBounds(12, 89, 122, 16);
				jLabelFrameDepth.setFont(SMALL_FONT);
			}
			{
				jTextFieldFrameDepth = new JTextField();
				this.add(jTextFieldFrameDepth);
				jTextFieldFrameDepth.setText("10");
				jTextFieldFrameDepth.setBounds(143, 86, 52, 28);
				jTextFieldFrameDepth.setFont(SMALL_FONT);
				jTextFieldFrameDepth.setSize(TEXTFIELD_DIMENSION);
				jTextFieldFrameDepth.setText(""+SpotDisplayer.DEFAULT_TRACK_DISPLAY_DEPTH);
				jTextFieldFrameDepth.addActionListener(trackDisplayModeListener);
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
		frame.getContentPane().add(new DisplayerPanel());
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
