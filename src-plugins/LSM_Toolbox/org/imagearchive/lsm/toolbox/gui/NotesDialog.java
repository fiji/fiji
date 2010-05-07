package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.Reader;
import org.imagearchive.lsm.toolbox.ServiceMediator;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;

public class NotesDialog extends JDialog {

	private JTextArea tsnotes;

	private JTextArea tdnotes;

	private JPanel panel;

	private MasterModel masterModel = MasterModel.getMasterModel();

	public NotesDialog(JFrame parent, boolean modal) {
		super(parent, modal);
		initializeGUI();
	}

	private void initializeGUI() {
		setTitle("LSM Notes");
		getContentPane().setLayout(new BorderLayout());
		JLabel snotes = new JLabel("Short Notes :");
		JLabel dnotes = new JLabel("Detailed Notes :");
		tsnotes = new JTextArea("");
		tdnotes = new JTextArea("");
		tsnotes.setEditable(false);
		tsnotes.setEditable(false);
		tdnotes.setRows(4);
		tdnotes.setRows(4);
		tsnotes.setColumns(20);
		tdnotes.setColumns(20);
		panel = new JPanel();

		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		panel.add(snotes, gbc);
		gbc.gridy = 1;
		panel.add(dnotes, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(tsnotes, gbc);
		gbc.gridy = 1;
		panel.add(tdnotes, gbc);
		getContentPane().add(panel, BorderLayout.NORTH);
		JButton okb = new JButton("Ok");
		addokbListener(okb, this);
		getContentPane().add(okb, BorderLayout.SOUTH);
		pack();
		centerWindow();
	}

	public void setNotes() {
	ImagePlus imp = WindowManager.getCurrentImage();
		Reader reader = ServiceMediator.getReader();
		reader.updateMetadata(imp);
		if (imp == null) return;
		if (imp.getOriginalFileInfo() instanceof LSMFileInfo){
			LSMFileInfo lsm = (LSMFileInfo)imp.getOriginalFileInfo();

		ArrayList<ImageDirectory> imageDirectories = lsm.imageDirectories;
		ImageDirectory imDir = (ImageDirectory)(imageDirectories.get(0));
		if (imDir == null) return;
		CZLSMInfoExtended cz = (CZLSMInfoExtended )imDir.TIF_CZ_LSMINFO;
		Recording r = (Recording) cz.scanInfo.recordings.get(0);
		if (r == null)  return;
				String shortNotes = (String) r.records.get("ENTRY_DESCRIPTION");
				String detailedNotes = (String) r.records.get("ENTRY_NOTES");
				tsnotes.setText(shortNotes);
				tdnotes.setText(detailedNotes);
		}
    }

	private void addokbListener(final JButton button, final JDialog parent) {
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - this.getWidth()) / 2,
				(screenSize.height - this.getHeight()) / 2);
	}
}