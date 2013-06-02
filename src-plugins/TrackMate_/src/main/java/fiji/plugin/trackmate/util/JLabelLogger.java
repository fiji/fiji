package fiji.plugin.trackmate.util;

import java.awt.Color;

import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class JLabelLogger extends JLabel {
	
	private static final long serialVersionUID = 1L;
	private final MyLogger logger;

	public JLabelLogger() {
		this.logger = new MyLogger(this);
		setFont(TrackMateWizard.SMALL_FONT);
	}
	
	public Logger getLogger() {
		return logger;
	}

	
	
	
	/*
	 * INNER CLASS
	 */
	
	private class MyLogger extends Logger {

		private final JLabelLogger label;

		public MyLogger(JLabelLogger logger) {
			this.label = logger;
		}

		@Override
		public void log(String message, Color color) {
			label.setText(message);
			label.setForeground(color);
		}

		@Override
		public void error(String message) {
			log(message, Logger.ERROR_COLOR);
		}

		/** Ignored. */
		@Override
		public void setProgress(double val) {}

		@Override
		public void setStatus(String status) {
			log(status, Logger.BLUE_COLOR);
		}
		
	}
}
