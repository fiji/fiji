package org.imagearchive.lsm.toolbox.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.imagearchive.lsm.toolbox.BatchConverter;
import org.imagearchive.lsm.toolbox.MasterModel;

public class BatchDialog extends JDialog {

	private JButton okButton;

	private JButton cancelButton;

	private MasterModel masterModel;

	private String title = "Zeiss LSM batch conversion";

	private JPanel buttonsPanel;

	private JPanel mainPanel;

	private JLabel sourceLabel;

	private JLabel outputLabel;

	private JLabel formatLabel;

	private JTable sourceTable;

	private JScrollPane sourcePane;

	private JTextField outputTF;

	private JButton sourceButton;

	private JButton outputButton;

	private JButton resetButton;

	private JComboBox formatCombo;

	private JCheckBox verboseCB;

	private JCheckBox dirCB;

	private JFrame parent;

	public BatchDialog(Object parent, MasterModel masterModel) {
		super((JFrame) parent, true);
		this.parent = (JFrame)parent;
		this.masterModel = masterModel;
		initComponents();
		setGUI();
		centerWindow();
		setListeners();
	}

	public void initComponents() {
		okButton = new JButton("Run batch",new ImageIcon(getClass().getResource(
				"images/ok.png")));
		cancelButton = new JButton("Cancel",new ImageIcon(getClass().getResource(
				"images/cancel.png")));
		sourceLabel = new JLabel("Select source folder");
		outputLabel = new JLabel("Select output folder");
		sourceTable = new JTable();
		sourcePane = new JScrollPane();
		outputTF = new JTextField();
		sourceButton = new JButton("Browse");
		outputButton = new JButton("Browse");
		resetButton = new JButton("Reset list");
		formatLabel = new JLabel("Save as type:");
		formatCombo = new JComboBox(masterModel.supportedBatchTypes);
		verboseCB = new JCheckBox("Verbose (popups on error!)");
		dirCB = new JCheckBox("Output each image to separate directory");
		buttonsPanel = new JPanel();
		mainPanel = new JPanel();
		//sourceTableModel = new DefaultTableModel();
	}

	public void setGUI() {
		getContentPane().setLayout(new BorderLayout());

		mainPanel.setLayout(new GridBagLayout());

		sourcePane.setViewportView(sourceTable);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(sourceLabel, mainPanel, 0, 0,
				1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH,0.125d, 1d);

		sourcePane.setMinimumSize(new Dimension(400, 200));

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(sourcePane, mainPanel, 1, 0,
				1,4, GridBagConstraints.BOTH,GridBagConstraints.CENTER, 1d, 1d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(sourceButton, mainPanel, 2, 0,
				1, 1, GridBagConstraints.HORIZONTAL,GridBagConstraints.NORTH, 0.125d, 0.5d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(outputLabel, mainPanel, 0, 5,
				1, 1, GridBagConstraints.BOTH, GridBagConstraints.CENTER,0.125d, 1d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(outputTF, mainPanel, 1, 5,
				1,1, GridBagConstraints.HORIZONTAL,GridBagConstraints.CENTER, 0.125d, 1d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(outputButton, mainPanel, 2, 5,
				1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER,0.125d, 0.5d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(formatLabel, mainPanel, 0, 6,
				1, 1,GridBagConstraints.HORIZONTAL,GridBagConstraints.CENTER, 0.125d, 0.5d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(formatCombo, mainPanel, 1, 6,
				1, 1,GridBagConstraints.HORIZONTAL,GridBagConstraints.CENTER, 0.125d, 0.5d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(dirCB, mainPanel, 0, 7,
				3,1, GridBagConstraints.HORIZONTAL,GridBagConstraints.CENTER, 0.125d, 0.5d);

		mainPanel = (JPanel) GUIMaker.addComponentToGrid(verboseCB, mainPanel, 0, 8,
				3,1, GridBagConstraints.HORIZONTAL,GridBagConstraints.CENTER, 0.125d, 0.5d);

		buttonsPanel.add(resetButton);
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		verboseCB.setSelected(true);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		pack();
		setTitle(title);
	}



	public void setListeners() {
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outputDir = new File(outputTF.getText());
				if (!(sourceTable.getModel().getRowCount() >0)){
						IJ.error("You have to select some files or a directory containing images first!");
						return;
					}
				ArrayList list = ((LsmImageTableModel)sourceTable.getModel()).getFiles();
				if (outputTF.getText()!= "" | (outputDir.isDirectory() | outputDir.exists())){
					if (!outputDir.exists()){
						int result = JOptionPane.showConfirmDialog(new JFrame(), "The output directory does not exist. Do you want to create it and continue the processing?", "Create directory", JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.YES_OPTION)
							if (outputDir.mkdirs()) doConvert(list,outputDir);
					} else {
						doConvert(list,outputDir);
					}
				}
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		sourceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setMultiSelectionEnabled(true);
				fc
						.setDialogTitle("Select a source directory or multiselect files");
				fc.addChoosableFileFilter(new ImageFilter());
				int returnVal = fc.showDialog(null, "Select source");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File[] files = fc.getSelectedFiles();
					LsmImageTableModel tm = new LsmImageTableModel();
					for (int i = 0; i < files.length; i++)	{
						processPath(tm,files[i]);
					}
					sourceTable.setModel(tm);
				}
			}
		});
		outputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Select a output directory");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setAcceptAllFileFilterUsed(false);
				int returnVal = fc.showDialog(null, "Select target directory");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
						outputTF.setText(file.getAbsolutePath());
				}
			}
		});
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			((LsmImageTableModel)sourceTable.getModel()).removeAllFiles();
			}
		});

	}

	public void doConvert(ArrayList list, File outputDir){
		BatchConverter converter = new BatchConverter(masterModel);
		IJ.showStatus("Conversion started");
		for (int i = 0; i < list.size(); i++){
			 IJ.showStatus("Converting "+i+"/"+list.size());
			 converter.convertFile(((File)list.get(i)).getAbsolutePath(),outputDir.getAbsolutePath(),
					 (String)formatCombo.getSelectedItem(),verboseCB.isSelected(),
					 dirCB.isSelected());
		 }
		 IJ.showProgress(1.0);
	     IJ.showStatus("Conversion done");
	     IJ.showMessage("Conversion done");
	     this.dispose();
	}

	public LsmImageTableModel processPath(LsmImageTableModel tm, File path) {
        if (path.isDirectory()) {
            String[] children = path.list();
            for (int i=0; i<children.length; i++) {
		tm = processPath(tm, new File(path, children[i]));
            }
        } else {
		if (ImageFilter.getExtension(path).equals("lsm"))
		tm.addFile(path);
        }
        return tm;

	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth()) / 2,
				(screenSize.height - getHeight()) / 2);
	}
}
