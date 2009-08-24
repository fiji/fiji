package fiji.pluginManager.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import fiji.pluginManager.logic.UpdateTracker;

/*
 * Interface of a separate window, when downloading/removing plugins.
 */
class Installer extends JFrame {
	private MainUserInterface mainUserInterface;
	private Timer timer;
	private JButton btnClose;
	private JProgressBar progressBar;
	private JTextPane txtProgressDetails;
	private UpdateTracker updateTracker;

	//Download Window opened from Plugin Manager UI
	public Installer(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setTitle("Download");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel panel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		/* progress bar */
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(555, 30));
		progressBar.setStringPainted(true);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		panel.add(progressBar);
		panel.add(Box.createRigidArea(new Dimension(0,15)));

		/* Create textpane to hold the information */
		txtProgressDetails = new TextPaneDisplay();
		SwingTools.getTextScrollPane(txtProgressDetails, 555, 200, panel);
		panel.add(Box.createRigidArea(new Dimension(0,15)));

		/* Button to cancel progressing task (Or press done when complete) */
		btnClose = new JButton();
		btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
		closeFrameInstaller();
            }
        });

		JPanel btnPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(btnClose);
		panel.add(btnPanel);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
	}

	public void setInstaller(UpdateTracker updateTracker) {
		if (this.updateTracker != null) throw new Error("Installer object already exists.");
		else {
			this.updateTracker = updateTracker;
			updateTracker.start(); //download
		}
	}

	private void closeFrameInstaller() {
		//plugin manager will deal with this
		if (updateTracker.isDownloading()) {
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to cancel the ongoing download?",
					"Stop?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				updateTracker.stopDownload();
			} else
				return;
		}

		mainUserInterface.exitWithRestartFijiMessage();
	}

	private void setPercentageComplete(int downloaded, int total) {
		int percent = (total > 0 ? downloaded*100/total : 0);
		progressBar.setString(percent + "%");
		progressBar.setValue(percent);
	}
}
