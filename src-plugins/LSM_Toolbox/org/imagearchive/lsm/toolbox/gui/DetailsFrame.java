package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;
import ij.WindowManager;
import ij.text.TextWindow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.DomXmlExporter;
import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.Reader;
import org.imagearchive.lsm.toolbox.ServiceMediator;
import org.imagearchive.lsm.toolbox.info.CZLSMInfoExtended;
import org.imagearchive.lsm.toolbox.info.scaninfo.Recording;
import org.imagearchive.lsm.toolbox.info.scaninfo.ScanInfo;

public class DetailsFrame extends JFrame {

	private MasterModel masterModel = MasterModel.getMasterModel();

	private int detailsFrameXsize = 260;

	private int detailsFrameYsize = 400;

	private int detailsFrameXlocation = 0;

	private int detailsFrameYlocation = 200;

	public JTree detailsTree;

	private JTable table;

	private JToolBar toolBar;

	private JButton exitButton;

	private JButton dumpButton;

	private JButton xmlButton;

	private JToggleButton filterButton;

	private JButton searchButton;

	private JTextField searchTF;

	private DefaultTreeModel treemodel;

	private TreeTableModel tablemodel;

	private JMenuItem expandAllItem;

	private JMenuItem collapseAllItem;

	private JCheckBoxMenuItem filterCBItem;

	private JPopupMenu detailsTreePopupMenu;

	private String frameTitle = "Details";

	private String title = "Image acquisition properties";

	private DefaultMutableTreeNode lastNodeResult = null;

	private Point searchCoordinates;

	private LSMFileInfo lsm = null;

	public DetailsFrame() throws HeadlessException {
		super();
		initializeGUI();
		ServiceMediator.registerDetailsFrame(this);
	}

	public void initializeGUI() {
		setTitle(frameTitle);
		setSize(detailsFrameXsize, detailsFrameYsize);
		setLocation(detailsFrameXlocation, detailsFrameYlocation);
		treemodel = new DefaultTreeModel(new DefaultMutableTreeNode(
				"LSM File Information"));
		detailsTree = new JTree(treemodel);
		detailsTree.putClientProperty("JTree.lineStyle", "Angled");
		detailsTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		detailsTree.setShowsRootHandles(true);
		tablemodel = new TreeTableModel();
		table = new JTable(tablemodel);
		table.setCellSelectionEnabled(true);
		JScrollPane treepane = new JScrollPane(detailsTree);
		JScrollPane detailspane = new JScrollPane(table);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				treepane, detailspane);
		splitPane.setBorder(null);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(300);
		Dimension minimumSize = new Dimension(WIDTH, HEIGHT - 50);
		treepane.setMinimumSize(minimumSize);
		detailspane.setMinimumSize(minimumSize);
		exitButton = new JButton(new ImageIcon(getClass().getResource(
				"images/exit.png")));
		exitButton.setToolTipText("Close this window");
		searchButton = new JButton(new ImageIcon(getClass().getResource(
				"images/find.png")));
		searchButton.setToolTipText("Find tag, property or value");
		filterButton = new JToggleButton(new ImageIcon(getClass().getResource(
				"images/filter.png")));
		filterButton.setToolTipText("Filter unused tags");
		dumpButton = new JButton(new ImageIcon(getClass().getResource(
				"images/dump.png")));
		dumpButton
				.setToolTipText("Dump data to textwindow, saving to text file is possible");
		xmlButton = new JButton(new ImageIcon(getClass().getResource(
		"images/xml.png")));
		xmlButton
		.setToolTipText("Dump xml data to textwindow, saving to text file is possible");

		searchTF = new JTextField("");
		detailsTreePopupMenu = new JPopupMenu();
		expandAllItem = new JMenuItem("Expand all", new ImageIcon(getClass()
				.getResource("images/plus.png")));
		collapseAllItem = new JMenuItem("Collapse all", new ImageIcon(
				getClass().getResource("images/minus.png")));
		filterCBItem = new JCheckBoxMenuItem("Filtered", new ImageIcon(
				getClass().getResource("images/filter.png")));
		detailsTreePopupMenu.add(expandAllItem);
		detailsTreePopupMenu.add(collapseAllItem);
		detailsTreePopupMenu.add(new JSeparator());
		detailsTreePopupMenu.add(filterCBItem);
		detailsTreePopupMenu.setOpaque(true);
		detailsTree.add(detailsTreePopupMenu);
		detailsTree.setExpandsSelectedPaths(true);
		toolBar = new JToolBar();

		toolBar.add(exitButton);
		toolBar.add(dumpButton);
		toolBar.add(xmlButton);
		toolBar.add(filterButton);
		toolBar.add(new JSeparator());
		toolBar.add(new JLabel("  Search: "));
		toolBar.add(searchTF);
		toolBar.add(searchButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(toolBar, BorderLayout.NORTH);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		//filterCBItem.setSelected(true);
		//filterButton.setSelected(true);
		pack();
		centerWindow();
		setListeners();
	}

	public void setListeners() {

		addWindowFocusListener(new WindowFocusListener(){
			public void windowGainedFocus(WindowEvent e) {
				updateTreeAndLabels();
			}

			public void windowLostFocus(WindowEvent e) {
			}
		});

		dumpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dumpData();
			}
		});

		xmlButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dumpXmlData();
			}
		});

		expandAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				expandAll();
			}
		});

		collapseAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				collapseAll();
			}
		});

		detailsTree.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					detailsTreePopupMenu.show((JComponent) e.getSource(), e
							.getX(), e.getY());
				}
			}
		});

		exitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}

		});

		filterCBItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				filterButton.setSelected(filterCBItem.isSelected());
				updateTreeAndLabels();
			}
		});

		filterButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				filterCBItem.setSelected(filterButton.isSelected());
				updateTreeAndLabels();
			}
		});

		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!searchTF.getText().equals("")) {
					lastNodeResult = findNode(searchTF.getText(),
							((DefaultMutableTreeNode) detailsTree.getModel()
									.getRoot()).breadthFirstEnumeration(),
							lastNodeResult);
					if (lastNodeResult != null) {
						TreePath tp = new TreePath(lastNodeResult.getPath());
						detailsTree.setSelectionPath(tp);
						detailsTree.scrollPathToVisible(tp);
						if (lastNodeResult instanceof InfoNode
								&& searchCoordinates != null) {

							table.changeSelection(searchCoordinates.x,
									searchCoordinates.y, false, false);
						}
					}

				}
			}
		});

		detailsTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (detailsTree.getLastSelectedPathComponent() instanceof InfoNode) {
					InfoNode node = (InfoNode) detailsTree
							.getLastSelectedPathComponent();
					if (node == null)
						return;
					Object nodeInfo = node.getUserObject();
					if (nodeInfo == null)
						return;
					if (nodeInfo instanceof LinkedHashMap) {
						LinkedHashMap<String, Object> info = (LinkedHashMap<String, Object>)nodeInfo;
						((TreeTableModel) table.getModel()).setData(info);
					}
				} else
					((TreeTableModel) table.getModel()).setData(null);
			}
		});

		updateTreeAndLabels();
	}



	public DefaultMutableTreeNode findNode(String string, Enumeration<DefaultMutableTreeNode> nodes,
			DefaultMutableTreeNode last) {
		DefaultMutableTreeNode result = null;
		DefaultMutableTreeNode node = null;
		Object[][] data = null;
		string = string.toLowerCase();
		if (last != null) {
			if (last instanceof InfoNode) {
				InfoNode info = (InfoNode) last;
				LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) info.data;
				if (filterCBItem.isSelected())
					dataMap = getFilteredMap(dataMap);
				Iterator<String> iterator = dataMap.keySet().iterator();
				String tag;
				data = new Object[dataMap.size()][2];
				for (int i = 0; iterator.hasNext(); i++) {
					tag = iterator.next();
					data[i][0] = tag;
					data[i][1] = dataMap.get(tag);

				}
				boolean pointCreated = false;
				if (searchCoordinates == null) {
					searchCoordinates = new Point(0, 0);
					pointCreated = true;
				}

				int i = searchCoordinates.x;
				int j = searchCoordinates.y;
				while (i < data.length) {

					while (j < 2) {
						if (pointCreated
								|| !searchCoordinates.equals(new Point(i, j))) {
							Object property = data[i][j];
							if (property.toString().toLowerCase().indexOf(
									string) > 0) {
								searchCoordinates = new Point(i, j);
								return last;
							}

						}
						j++;
					}
					j = 0;
					i++;
				}
			}
			while (nodes.hasMoreElements()
					&& (!nodes.nextElement().equals(last)))
				;
		}

		searchCoordinates = null;

		while (nodes.hasMoreElements() && (result == null)) {
			node = (DefaultMutableTreeNode) nodes.nextElement();
			String nodeTitle = node.getUserObject().toString();

			if (nodeTitle.toLowerCase().indexOf(string) > 0) {
				result = node;
			}
		}
		if (result == null)
			JOptionPane
					.showMessageDialog(
							this,
							"End of metadata reached. I could not find any tags, properties or values. The next search will start from the beginning.",
							"Find...", JOptionPane.INFORMATION_MESSAGE);
		return result;
	}

	public LinkedHashMap<String, Object> getFilteredMap(LinkedHashMap<String, Object> dataMap) {
		LinkedHashMap<String, Object> filteredMap = new LinkedHashMap<String, Object>();
		Iterator<String> iterator = dataMap.keySet().iterator();
		String tag;
		for (int i = 0; iterator.hasNext(); i++) {
			tag = iterator.next();
			if (tag.indexOf("<UNKNOWN@") == -1)
				filteredMap.put(tag, dataMap.get(tag));
		}
		return filteredMap;
	}


	public void expandAll() {
		int row = 0;
		while (row < detailsTree.getRowCount()) {
			detailsTree.expandRow(row);
			row++;
		}
	}

	public void collapseAll() {
		int row = detailsTree.getRowCount() - 1;
		while (row >= 0) {
			detailsTree.collapseRow(row);
			row--;
		}
	}

	public void updateTreeAndLabels() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) return;
		Reader reader = ServiceMediator.getReader();
		reader.updateMetadata(imp);
		if (imp.getOriginalFileInfo() instanceof LSMFileInfo) {
			lsm = (LSMFileInfo) imp.getOriginalFileInfo();
			setTitle(title + " - " + lsm.fileName);
			detailsTree.clearSelection();
			table.clearSelection();
			collapseAll();
			if (filterCBItem.isSelected())
				updateFilteredTree(true);
			else
				updateFilteredTree(false);
		}

	}

	public void dumpData() {
		String header = new String("Data dump");
		new TextWindow("SCANINFO DUMP", header, getTreeAsStringBuffer()
				.toString(), 250, 400);
	}

	protected void dumpXmlData() {
		if (lsm == null) return;
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		new TextWindow("XML SCANINFO DUMP", new DomXmlExporter().buildTree(cz,filterButton.isSelected()), 250, 400);
	}

	public void updateFilteredTree(boolean filter) {

		((DefaultMutableTreeNode) detailsTree.getModel().getRoot())
				.removeAllChildren();
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		InfoNode czNode = new InfoNode("CarlZeiss", convertCZ(cz));
		((DefaultMutableTreeNode) treemodel.getRoot()).add(czNode);
		if (cz.scanInfo == null)
			return;
		ScanInfo scanInfo = cz.scanInfo;
		ArrayList<Recording> recordings = scanInfo.recordings;
		for (int i = 0; i < recordings.size(); i++) {
			Recording recording = recordings.get(i);
			InfoNode recordingsNode = new InfoNode("Recordings",
					recording.records);

			if (recording.lasers != null) {
				DefaultMutableTreeNode lasersNode = new DefaultMutableTreeNode(
						"Lasers");

				for (int j = 0; j < recording.lasers.length; j++) {
					LinkedHashMap<String, Object> map = recording.lasers[j].records;
					Object o = map.get("LASER_ACQUIRE");
					if (!filter || (o == null)
							|| (o != null & o.toString().equals("-1")))
						lasersNode.add(new InfoNode("Laser " + j,
								recording.lasers[j].records));
				}
				recordingsNode.add(lasersNode);
			}
			if (recording.tracks != null) {
				DefaultMutableTreeNode tracksNode = new DefaultMutableTreeNode(
						"Tracks");

				for (int j = 0; j < recording.tracks.length; j++) {
					DefaultMutableTreeNode trackNode = new InfoNode("Track "
							+ j, recording.tracks[j].records);
					if (recording.tracks[j].detectionChannels != null) {
						if (recording.tracks[j].detectionChannels.length > 0) {
							DefaultMutableTreeNode detectionChannelsNode = new DefaultMutableTreeNode(
									"DetectionChannels");
							for (int k = 0; k < recording.tracks[j].detectionChannels.length; k++) {
								LinkedHashMap<String, Object> map = recording.tracks[j].detectionChannels[k].records;
								Object o = map.get("ACQUIRE");
								if (!filter
										|| (o == null)
										|| (o != null & o.toString().equals(
												"-1")))
									detectionChannelsNode
											.add(new InfoNode(
													"DetectionChannel " + k,
													recording.tracks[j].detectionChannels[k].records));
							}
							trackNode.add(detectionChannelsNode);
						}
					}
					if (recording.tracks[j].illuminationChannels != null) {
						if (recording.tracks[j].illuminationChannels.length > 0) {
							DefaultMutableTreeNode illuminationChannelsNode = new DefaultMutableTreeNode(
									"IlluminationChannels");
							for (int k = 0; k < recording.tracks[j].illuminationChannels.length; k++) {
								LinkedHashMap<String, Object> map = recording.tracks[j].illuminationChannels[k].records;
								Object o = map.get("ACQUIRE");
								if (!filter
										|| (o == null)
										|| (o != null & o.toString().equals(
												"-1")))
									illuminationChannelsNode
											.add(new InfoNode(
													"IlluminationChannel " + k,
													recording.tracks[j].illuminationChannels[k].records));
							}
							trackNode.add(illuminationChannelsNode);
						}
					}

					if (recording.tracks[j].beamSplitters != null) {
						if (recording.tracks[j].beamSplitters.length > 0) {
							DefaultMutableTreeNode beamSplittersNode = new DefaultMutableTreeNode(
									"BeamSplitters");

							for (int k = 0; k < recording.tracks[j].beamSplitters.length; k++) {
								InfoNode bsNode = new InfoNode(
										"Beamsplitter " + k,
										recording.tracks[j].beamSplitters[k].records);
								beamSplittersNode.add(bsNode);
							}
							trackNode.add(beamSplittersNode);
						}
					}
					if (recording.tracks[j].dataChannels != null) {
						if (recording.tracks[j].dataChannels.length > 0) {
							DefaultMutableTreeNode dataChannelsNode = new DefaultMutableTreeNode(
									"DataChannels");
							for (int k = 0; k < recording.tracks[j].dataChannels.length; k++) {
								LinkedHashMap<String, Object> map = recording.tracks[j].dataChannels[k].records;
								Object o = map.get("ACQUIRE");
								if (!filter
										|| (o == null)
										|| (o != null & o.toString().equals(
												"-1")))
									dataChannelsNode
											.add(new InfoNode(
													"DataChannel " + k,
													recording.tracks[j].dataChannels[k].records));
							}
							trackNode.add(dataChannelsNode);
						}
					}
					tracksNode.add(trackNode);
				}
				if (tracksNode.getChildCount() > 0)
					recordingsNode.add(tracksNode);
			}

			if (recording.markers != null) {
				DefaultMutableTreeNode markersNode = new DefaultMutableTreeNode(
						"Markers");

				for (int j = 0; j < recording.markers.length; j++) {
					markersNode.add(new InfoNode("Marker " + j,
							recording.markers[j].records));
				}
				recordingsNode.add(markersNode);
			}
			if (recording.timers != null) {
				DefaultMutableTreeNode timersNode = new DefaultMutableTreeNode(
						"Timers");

				for (int j = 0; j < recording.timers.length; j++) {
					timersNode.add(new InfoNode("Timer " + j,
							recording.timers[j].records));
				}
				recordingsNode.add(timersNode);
			}

			((DefaultMutableTreeNode) treemodel.getRoot()).add(recordingsNode);
		}
		lastNodeResult = null;
		searchCoordinates = null;
		((TreeTableModel) table.getModel()).setFiltered(filter);
		treemodel.reload();
		expandAll();
	}

	public void expandTree() {
		expandEntireTree((DefaultMutableTreeNode) treemodel.getRoot());
	}

	private void expandEntireTree(DefaultMutableTreeNode tNode) {
		TreePath tp = new TreePath(((DefaultMutableTreeNode) tNode).getPath());
		detailsTree.expandPath(tp);

		for (int i = 0; i < tNode.getChildCount(); i++) {
			expandEntireTree((DefaultMutableTreeNode) tNode.getChildAt(i));
		}
	}

	private StringBuffer getTreeAsStringBuffer() {
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		StringBuffer sb = new StringBuffer();

		sb.append("CarlZeiss\t\n");
		sb.append(getRecordAsString(convertCZ(cz)));

		ScanInfo scanInfo = cz.scanInfo;
		ArrayList<Recording> recordings = scanInfo.recordings;
		for (int i = 0; i < recordings.size(); i++) {
			Recording recording = recordings.get(i);
			sb.append("Recording " + i + "\t\n");
			sb.append(getRecordAsString(recording.records));
			if (recording.lasers != null) {
				for (int j = 0; j < recording.lasers.length; j++) {
					sb.append("Laser " + j + "\t\n");
					sb.append(getRecordAsString(recording.lasers[j].records));
				}
			}
			if (recording.tracks != null) {
				for (int j = 0; j < recording.tracks.length; j++) {
					sb.append("Track" + j + "\t\n");
					sb.append(getRecordAsString(recording.tracks[j].records));
					if (recording.tracks[j].dataChannels != null)
						for (int k = 0; k < recording.tracks[j].dataChannels.length; k++) {
							sb.append("DataChannel " + k + "\t\n");

							sb
									.append(getRecordAsString(recording.tracks[j].dataChannels[k].records));
						}
					if (recording.tracks[j].beamSplitters != null)
						for (int k = 0; k < recording.tracks[j].beamSplitters.length; k++) {
							sb.append("BeamSplitter " + k + "\t\n");
							sb
									.append(getRecordAsString(recording.tracks[j].beamSplitters[k].records));
						}
					if (recording.tracks[j].detectionChannels != null)
						for (int k = 0; k < recording.tracks[j].detectionChannels.length; k++) {
							sb.append("DetectionChannel " + k + "\t\n");
							sb
									.append(getRecordAsString(recording.tracks[j].detectionChannels[k].records));
						}
					if (recording.tracks[j].illuminationChannels != null) {
						for (int k = 0; k < recording.tracks[j].illuminationChannels.length; k++) {
							sb.append("IlluminationChannel " + k + "\t\n");
							sb
									.append(getRecordAsString(recording.tracks[j].illuminationChannels[k].records));
						}
					}
				}
			}
			if (recording.markers != null)
				for (int j = 0; j < recording.markers.length; j++) {
					sb.append("Marker " + j + "\t\n");
					sb.append(getRecordAsString(recording.markers[j].records));
				}

			if (recording.timers != null)
				for (int j = 0; j < recording.timers.length; j++) {
					sb.append("Timer " + j + "\t\n");
					sb.append(getRecordAsString(recording.timers[j].records));
				}
		}
		return sb;
	}

	private StringBuffer getRecordAsString(LinkedHashMap<String,Object> hm) {
		StringBuffer sb = new StringBuffer();
		if (hm != null) {
			Iterator<String> iterator = hm.keySet().iterator();
			for (int i = 0; iterator.hasNext(); i++) {
				String tag = (String) iterator.next();
				sb.append(tag + ":\t" + hm.get(tag) + "\n");
			}
		}
		return sb;
	}

	public static LinkedHashMap<String, Object> convertCZ(CZLSMInfoExtended cz) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("DimensionX", new Long(cz.DimensionX));
		map.put("DimensionY", new Long(cz.DimensionY));
		map.put("DimensionZ", new Long(cz.DimensionZ));
		map.put("DimensionChannels", new Long(cz.DimensionChannels));
		map.put("DimensionTime", new Long(cz.DimensionTime));
		map.put("IntensityDataType", new Long(cz.IntensityDataType));
		map.put("ThumbnailX", new Long(cz.ThumbnailX));
		map.put("ThumbnailY", new Long(cz.ThumbnailY));
		map.put("VoxelSizeX", new Double(cz.VoxelSizeX));
		map.put("VoxelSizeY", new Double(cz.VoxelSizeY));
		map.put("VoxelSizeZ", new Double(cz.VoxelSizeZ));
		map.put("OriginX", new Double(cz.OriginX));
		map.put("OriginY", new Double(cz.OriginY));
		map.put("OriginZ", new Double(cz.OriginZ));
		map.put("ScanType", new Integer(cz.ScanType));
		map.put("SpectralScan", new Integer(cz.SpectralScan));
		map.put("DataType", new Long(cz.DataType));
		map.put("TimeIntervall", new Double(cz.TimeIntervall));
		map.put("DisplayAspectX", new Double(cz.DisplayAspectX));
		map.put("DisplayAspectY", new Double(cz.DisplayAspectY));
		map.put("DisplayAspectZ", new Double(cz.DisplayAspectZ));
		map.put("DisplayAspectTime", new Double(cz.DisplayAspectTime));
		map.put("ToolbarFlags", new Long(cz.ToolbarFlags));
		return map;
	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - this.getWidth()) / 2,
				(screenSize.height - this.getHeight()) / 2);
	}
}
