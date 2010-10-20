package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.tracking.TrackerSettings;


public class TrackerSettingsPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = -2536527408461090418L;
	
	private TrackerSettings settings;
	private JPanelTrackerSettingsMain jPanelMain;
	private JScrollPane jScrollPaneMain;

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
	
	public TrackerSettingsPanel(TrackerSettings settings) {
		super();
		this.settings = settings;
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Update the {@link Settings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. Only the {@link Settings#trackerSettings} field
	 * and sub-fields will be updated here.
	 */
	public TrackerSettings getSettings() {
		return jPanelMain.getSettings();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	
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
				jScrollPaneMain.getVerticalScrollBar().setUnitIncrement(24);
				{
					jPanelMain = new JPanelTrackerSettingsMain(settings);
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
