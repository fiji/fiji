package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.SpotDisplayer.DisplayerType;

/**
 * A panel to let the user choose what displayer he wants to use.
 */
public class DisplayerChooserPanel extends ActionListenablePanel {
	
	private static final long serialVersionUID = -2349025481368788479L;
	private JLabel jLabelHeader;
	private JComboBox jComboBoxDisplayerChoice;
	private JButton jButtonHelp;
	private Settings settings;
	private DisplayerType[] displayerTypes;
	private static final String INFO_ICON = "images/information.png";
	

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTOR
	 */
	

	
	public DisplayerChooserPanel(Settings settings) {
		super();
		this.settings = settings;
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public DisplayerType getDisplayerType() {
		return displayerTypes[jComboBoxDisplayerChoice.getSelectedIndex()];
	}
	

	/*
	 * PRIVATE METHODS
	 */

	private void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 470));
			this.setLayout(null);
			{
				jLabelHeader = new JLabel();
				this.add(jLabelHeader);
				jLabelHeader.setFont(FONT.deriveFont(Font.BOLD));
				jLabelHeader.setText("Select a displayer");
				jLabelHeader.setBounds(6, 20, 288, 16);
				jLabelHeader.setHorizontalAlignment(SwingConstants.CENTER);
			}
			{
				String[] displayerNames;
				boolean is3D = settings.imp.getNSlices() > 1;
				if (is3D) 
					displayerTypes = DisplayerType.get3DDisplayers();
				else
					displayerTypes = DisplayerType.get2DDisplayers();				
				displayerNames = new String[displayerTypes.length];
				for (int i = 0; i < displayerTypes.length; i++) 
					displayerNames[i] = displayerTypes[i].toString();
				ComboBoxModel jComboBoxDisplayerChoiceModel = new DefaultComboBoxModel(displayerNames);
				jComboBoxDisplayerChoice = new JComboBox();
				this.add(jComboBoxDisplayerChoice);
				jComboBoxDisplayerChoice.setFont(FONT);
				jComboBoxDisplayerChoice.setModel(jComboBoxDisplayerChoiceModel);
				jComboBoxDisplayerChoice.setBounds(12, 48, 234, 27);
			}
			{
				jButtonHelp = new JButton();
				this.add(jButtonHelp);
				jButtonHelp.setBounds(258, 48, 24, 24);
				jButtonHelp.setIcon(new ImageIcon(getClass().getResource(INFO_ICON)));
				jButtonHelp.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {						
						String text = displayerTypes[jComboBoxDisplayerChoice.getSelectedIndex()].getInfoText();
						jButtonHelp.setToolTipText(text);
						Action toolTipAction = jButtonHelp.getActionMap().get("postTip");						
						ActionEvent postTip = new ActionEvent(jButtonHelp, ActionEvent.ACTION_PERFORMED, "");
						toolTipAction.actionPerformed( postTip );
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
