package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.TrackerSettings;


public class LAPTrackerSettingsPanel extends TrackerSettingsPanel {

	private static final long serialVersionUID = -2536527408461090418L;
	private JPanelTrackerSettingsMain jPanelMain;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public LAPTrackerSettingsPanel() {
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public TrackerSettings getSettings() {
		return jPanelMain.getSettings();
	}

	@Override
	public void setTrackerSettings(TrackerSettings settings, TrackMateModel model) {
		echoSettings(settings, model);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void echoSettings(TrackerSettings settings, TrackMateModel model) {
		jPanelMain.echoSettings(settings, model);
	}
	
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			setPreferredSize(new Dimension(300, 500));
			this.setLayout(thisLayout);
			{
				JScrollPane jScrollPaneMain = new JScrollPane();
				this.add(jScrollPaneMain, BorderLayout.CENTER);
				jScrollPaneMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jScrollPaneMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneMain.getVerticalScrollBar().setUnitIncrement(24);
				{
					jPanelMain = new JPanelTrackerSettingsMain();
					jScrollPaneMain.setViewportView(jPanelMain);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
