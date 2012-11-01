package fiji.plugin.trackmate.tests;

import javax.swing.JFrame;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;

public class JPanelTrackerSettings_Test {

	public static void main(String[] args) {
		
		LAPTrackerSettingsPanel panel = new LAPTrackerSettingsPanel();
		
		TrackMateModel model = new TrackMateModel();
		model.getSettings().trackerSettings = new LAPTrackerSettings();
		panel.setTrackerSettings(model);
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(250, 500);
		frame.setVisible(true);
		
		
	}
	
}
