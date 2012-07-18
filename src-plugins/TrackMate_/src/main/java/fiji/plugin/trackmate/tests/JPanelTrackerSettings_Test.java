package fiji.plugin.trackmate.tests;

import javax.swing.JFrame;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;

public class JPanelTrackerSettings_Test {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		
		LAPTrackerSettingsPanel panel = new LAPTrackerSettingsPanel();
		
		TrackMateModel<T> model = new TrackMateModel<T>();
		model.getSettings().trackerSettings = new LAPTrackerSettings();
		panel.setTrackerSettings(model);
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(250, 500);
		frame.setVisible(true);
		
		
	}
	
}
