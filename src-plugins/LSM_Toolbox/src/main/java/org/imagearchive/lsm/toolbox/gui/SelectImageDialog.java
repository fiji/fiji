package org.imagearchive.lsm.toolbox.gui;

import ij.WindowManager;
import ij.io.FileInfo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;

public class SelectImageDialog extends JDialog {

	private JPanel panel;

	private JList imageList;

	private MasterModel masterModel = MasterModel.getMasterModel();

	private Vector<ListBoxImage> fileInfos;

	private Vector<String> images;

	private String label = "Please select:";

	private JButton okButton;

	private JButton cancelButton;

	private int returnVal = -1;

	private int[] values = null;

	public static final int OK_OPTION = 0;

	public static final int CANCEL_OPTION = 2;

	private boolean channel = false;

	public SelectImageDialog(JFrame parent, String label, boolean channel, byte filter) {
		super(parent, true);
		this.label = label;
		this.channel = channel;
		initiliazeGUI();
		fillList(filter);
	}

	public SelectImageDialog(JFrame parent, String label, boolean channel) {
		super(parent, true);
		this.label = label;
		this.channel = channel;
		initiliazeGUI();
		fillList(MasterModel.NONE);
	}

	private void initiliazeGUI() {
		panel = new JPanel();
		imageList = new JList();
		okButton = new JButton("OK", new ImageIcon(getClass().getResource(
				"images/ok.png")));
		cancelButton = new JButton("Cancel", new ImageIcon(getClass()
				.getResource("images/cancel.png")));
		panel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		setSize(new Dimension(200, 300));
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.gridheight = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		constraints.ipadx = 0;
		constraints.ipady = 10;
		constraints.weightx = 0.25;
		constraints.weighty = 0.25;
		panel.add(new JLabel(label), constraints);
		constraints.gridy = 1;
		constraints.ipadx = 0;
		constraints.ipady = 0;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		panel.add(imageList, constraints);
		constraints.gridy = 3;
		constraints.gridwidth = 1;
		constraints.ipadx = 0;
		constraints.ipady = 10;
		constraints.weightx = 0.25;
		constraints.weighty = 0.25;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(okButton, constraints);
		constraints.gridx = 1;
		panel.add(cancelButton, constraints);
		this.getContentPane().add(panel);
		setTitle("Select...");
		if (channel)
			imageList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		else
			imageList
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setListeners();
		centerWindow();
	}

	private void fillList(byte filter) {
		int[] imagesIDs = WindowManager.getIDList();
		images = new Vector<String>();
		fileInfos = new Vector<ListBoxImage>();
		if (imagesIDs == null)
			return;
		for (int i = 0; i < imagesIDs.length; i++) {
			if (WindowManager.getImage(imagesIDs[i]) != null) {
				FileInfo fi = WindowManager.getImage(imagesIDs[i])
						.getOriginalFileInfo();
				boolean add = false;
				if (fi != null && fi instanceof LSMFileInfo) {
					LSMFileInfo lsm = (LSMFileInfo) fi;
					CZLSMInfoExtended cz = (CZLSMInfoExtended) ((ImageDirectory)lsm.imageDirectories
							.get(0)).TIF_CZ_LSMINFO;
					if (filter == MasterModel.TIME)
						if (cz.DimensionTime > 1)
							add = true;
					if (filter == MasterModel.DEPTH)
						if (cz.DimensionZ > 1){
							add = true;}
					if (filter == MasterModel.CHANNEL)
						if ((cz.SpectralScan == 1 && cz.channelWavelength != null)
								&& cz.channelWavelength.Channels >= 1)
							add = true;
					if (filter == MasterModel.NONE)
						add = true;
					if (add) {
						images.add(lsm.fileName);
						fileInfos.add(new ListBoxImage(lsm.fileName, lsm,
								imagesIDs[i]));
					}
				}
			}
		}
		ComboBoxModel cbm = new DefaultComboBoxModel(images);
		imageList.setModel(cbm);
	}

	private void setListeners() {
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (imageList.getModel().getSize() == 0)
					return;
				int[] selectedIndices = imageList.getSelectedIndices();
				values = new int[selectedIndices.length];
				for (int i = 0; i < selectedIndices.length; i++) {
					ListBoxImage im = (ListBoxImage) fileInfos
							.get(selectedIndices[i]);
					values[i] = im.imageIndex;
				}
				returnVal = OK_OPTION;
				setVisible(false);
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				returnVal = CANCEL_OPTION;
				setVisible(false);
			}
		});
	}

	public int showDialog() {
		this.setVisible(true);
		this.dispose();
		return returnVal;
	}

	public int[] getSelected() {
		return values;
	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - this.getWidth()) / 2,
				(screenSize.height - this.getHeight()) / 2);
	}
}
