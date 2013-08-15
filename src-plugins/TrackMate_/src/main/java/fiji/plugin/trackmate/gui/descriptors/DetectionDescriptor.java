package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.TMUtils;

public class DetectionDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "Detection";
	protected static final String CANCEL_TEXT = "Cancel";
	protected static final Icon CANCEL_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/cancel.png"));
	protected final LogPanel logPanel;
	protected final TrackMate trackmate;
	protected Thread motherThread;
	protected TrackMateGUIController controller;


	public DetectionDescriptor(final TrackMateGUIController controller) {
		this.controller = controller;
		this.logPanel = controller.getGUI().getLogPanel();
		this.trackmate = controller.getPlugin();
	}

	@Override
	public Component getComponent() {
		return logPanel;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		controller.disableButtonsAndStoreState();
		final Settings settings = trackmate.getSettings();
		final Logger logger = logPanel.getLogger();
		logger.log("Starting detection using "+settings.detectorFactory.toString()+"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(TMUtils.echoMap(settings.detectorSettings, 2));

		final JButton nextButton = controller.getGUI().getNextButton();
		// We have to tweak the GUI a bit from here
		final ActionListener[] actionListeners = nextButton.getActionListeners();
		for (final ActionListener actionListener : actionListeners) {
			nextButton.removeActionListener(actionListener);
		}
		nextButton.setText(CANCEL_TEXT);
		nextButton.setIcon(CANCEL_ICON);
		final CancelListener cancel = new CancelListener();
		nextButton.addActionListener(cancel);

		controller.getGUI().setNextButtonEnabled(true);

//		nextButton.setEnabled(true);

		motherThread = new Thread("TrackMate detection mother thread") {
			@Override
			public void run() {


				final long start = System.currentTimeMillis();
				try {
					trackmate.execDetection();
				} catch (final Exception e) {
					logger.error("An error occured:\n"+e+'\n');
					e.printStackTrace(logger);
				} finally {
					final long end = System.currentTimeMillis();
					logger.log(String.format("Detection done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
				motherThread = null;

				// Restore
				controller.restoreButtonsState();
				nextButton.removeActionListener(cancel);
				for (final ActionListener actionListener : actionListeners) {
					nextButton.addActionListener(actionListener);
				}
				nextButton.setText(TrackMateWizard.NEXT_TEXT);
				nextButton.setIcon(TrackMateWizard.NEXT_ICON);
			}
		};
		motherThread.start();
	}

	@Override
	public synchronized void aboutToHidePanel() {
		final Thread thread = motherThread;
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (final InterruptedException exc) {
				// ignore
			}
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}

	/*
	 * INNER CLASS
	 */

	private class CancelListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Thread thread = motherThread;
			if (thread != null) {
				thread.interrupt();
				try {
					thread.join();
				} catch (final InterruptedException exc) {
					// ignore
				}
			}
		}
	}
}
