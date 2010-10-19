package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.tracking.TrackerSettings;

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
public class TrackerSettingsPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = -2536527408461090418L;
	
	private TrackerSettings settings;
	private JPanelSegmenterSettingsMain jPanelMain;
	private JScrollPane jScrollPaneMain;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	
	public TrackerSettingsPanel(TrackerSettings settings) {
		super();
		this.settings = settings;
		initGUI();
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			setPreferredSize(new Dimension(300, 500));
			this.setLayout(thisLayout);
			{
				jScrollPaneMain = new JScrollPane();
				this.add(jScrollPaneMain, BorderLayout.CENTER);
				jScrollPaneMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jScrollPaneMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				{
					jPanelMain = new JPanelSegmenterSettingsMain(settings);
					jScrollPaneMain.setViewportView(jPanelMain);
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
		frame.getContentPane().add(new TrackerSettingsPanel(new TrackerSettings()));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
