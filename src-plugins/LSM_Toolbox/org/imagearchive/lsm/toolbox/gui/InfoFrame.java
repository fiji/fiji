package org.imagearchive.lsm.toolbox.gui;

import ij.text.TextWindow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.MasterModelAdapter;
import org.imagearchive.lsm.toolbox.MasterModelEvent;

public class InfoFrame extends JFrame {

	private MasterModel masterModel;

	private DetailsFrame detailsFrame;

	private NotesDialog notesDialog;

	private int infoFrameXsize = 300;

	private int infoFrameYsize = 400;

	private JLabel[] infolab = new JLabel[22];

	private JTextArea[] area = new JTextArea[22];

	public InfoFrame(MasterModel masterModel) throws HeadlessException {
		super();
		this.masterModel = masterModel;
		detailsFrame = new DetailsFrame(masterModel);
		notesDialog = new NotesDialog(this, true, masterModel);
		initializeGUI();
	}

	public void initializeGUI() {
		setTitle("General file information");
		setSize(infoFrameXsize, infoFrameYsize);
		getContentPane().setLayout(new BorderLayout());
		String[] infolabels = new String[19];
		infolabels[0] = "File Name";
		infolabels[1] = "User";
		infolabels[2] = "Image Width";
		infolabels[3] = "Image Height";
		infolabels[4] = "Number of channels";
		infolabels[5] = "Z Stack size";
		infolabels[6] = "Time Stack size";
		infolabels[7] = "Scan Type";
		infolabels[8] = "Voxel X size";
		infolabels[9] = "Voxel Y size";
		infolabels[10] = "Voxel Z size";
		infolabels[11] = "Objective";
		infolabels[12] = "X zoom factor";
		infolabels[13] = "Y zoom factor";
		infolabels[14] = "Z zoom factor";
		infolabels[15] = "Plane width";
		infolabels[16] = "Plane height";
		infolabels[17] = "Volume depth";
		infolabels[18] = "Plane spacing";
		JPanel infopanel = new JPanel(new GridLayout(19, 2, 3, 3));
		Font dafont = new Font(null);
		float fontsize = 11;
		dafont = dafont.deriveFont(fontsize);
		Font dafontbold = dafont.deriveFont(Font.BOLD);

		for (int i = 0; i < 19; i++) {
			infolab[i] = new JLabel("  " + infolabels[i]);
			infolab[i].setFont(dafontbold);
			infopanel.add(infolab[i]);
			area[i] = new JTextArea("");
			area[i].setEditable(false);
			area[i].setFont(dafont);
			infopanel.add(area[i]);
		}

		JButton details_button = new JButton(new ImageIcon(getClass()
				.getResource("images/plus.png")));
		details_button.setToolTipText("More details...");
		addDetailsListener(details_button, this);

		JButton notes_button = new JButton(new ImageIcon(getClass()
				.getResource("images/info.png")));
		notes_button.setToolTipText("Notes");
		addNotesButtonListener(notes_button, this);

		JButton dumpinfos_button = new JButton(new ImageIcon(getClass()
				.getResource("images/dump.png")));
		dumpinfos_button.setToolTipText("Dump to text file");
		addDumpInfosListener(dumpinfos_button, this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(notes_button);
		buttonPanel.add(dumpinfos_button);
		buttonPanel.add(details_button);

		getContentPane().add(buttonPanel, BorderLayout.NORTH);
		getContentPane().add(infopanel, BorderLayout.CENTER);

		masterModel.addMasterModelListener(new MasterModelAdapter() {
			public void LSMFileInfoChanged(MasterModelEvent evt) {
				if (masterModel.getLsmFileInfo() != null)
					updateInfoFrame(masterModel.getInfo());
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				if (detailsFrame != null)
					detailsFrame.dispose();
			}
		});

		pack();
		centerWindow();
	}

	public void updateInfoFrame(String[] str) {
		for (int i = 0; i < 19; i++) {
			area[i].setText(str[i]);
		}
	}

	private void addDumpInfosListener(final JButton button, final JFrame parent) {
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dumpInfo();
			}
		});
	}

	private void addDetailsListener(final JButton button, final JFrame parent) {
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (detailsFrame.isShowing() == false) {
					detailsFrame.setVisible(true);
				} else
					detailsFrame.setVisible(false);
			}
		});
	}

	private void addNotesButtonListener(final JButton button,
			final JFrame parent) {
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (notesDialog.isShowing() == false) {
					notesDialog.setVisible(true);
				} else
					notesDialog.setVisible(false);
			}
		});
	}

	public void dumpInfo() {
		String header = new String("Parameter\tValue");
		TextWindow tw = new TextWindow("LSM Infos DUMP", header, null, 280, 450);
		String[] Parameters = new String[26];
		Parameters[0] = "File Name";
		Parameters[1] = "User";
		Parameters[2] = "Image Width";
		Parameters[3] = "Image Height";
		Parameters[4] = "Number of channels";
		Parameters[5] = "Z Stack size";
		Parameters[6] = "Time Stack size";
		Parameters[7] = "Scan Type";
		Parameters[8] = "Voxel X size";
		Parameters[9] = "Voxel Y size";
		Parameters[10] = "Voxel Z size";
		Parameters[11] = "Objective";
		Parameters[12] = "X Zoom factor";
		Parameters[13] = "Y Zoom factor";
		Parameters[14] = "Z Zoom factor";
		Parameters[15] = "Plane width";
		Parameters[16] = "Plane height";
		Parameters[17] = "Volume depth";
		Parameters[18] = "Plane spacing";
		String[] infos = masterModel.getInfo();
		for (int i = 0; i < 19; i++)
			tw.append(Parameters[i] + "\t" + infos[i]);
	}

	public DetailsFrame getDetailsFrame() {
		return detailsFrame;
	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - this.getWidth()) / 2,
				(screenSize.height - this.getHeight()) / 2);
	}
}
