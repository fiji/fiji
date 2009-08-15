package fiji.pluginManager.ui;
import ij.IJ;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import fiji.pluginManager.logic.DependencyBuilder;
import fiji.pluginManager.logic.UpdateTracker;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.PluginObject;

/*
 * Main User Interface, where the user chooses his options...
 */
public class MainUserInterface extends JFrame implements TableModelListener {
	private PluginManager pluginManager;
	private PluginCollection viewList;

	//User Interface elements
	private JFrame loadedFrame;
	private String[] arrViewingOptions;
	private JTextField txtSearch;
	private JComboBox comboBoxViewingOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private PluginObject currentPlugin;
	private JButton btnStart;

	//For developers
	private JButton btnUpload;
	private JButton btnEditDetails;

	public MainUserInterface(PluginManager pluginManager) {
		super("Plugin Manager");
		this.pluginManager = pluginManager;

		//Pulls required information from pluginManager
		viewList = pluginManager.pluginCollection; //initially, view all
		PluginCollection readOnlyList = pluginManager.pluginCollection.getReadOnly();
		if (readOnlyList.size() > 0) {
			StringBuilder namelist = new StringBuilder();
			for (int i = 0; i < readOnlyList.size(); i++) {
				if (i != 0 && i % 3 == 0)
					namelist.append("\n");
				namelist.append((namelist.length() > 0 ? ", " : "") +
						readOnlyList.get(i).getFilename());
			}
			IJ.showMessage("Read-Only Plugins", "WARNING: The following plugin files are set to read-only, " +
					"you are advised to quit Fiji and set to writable:\n" + namelist.toString());
		}
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		//======== Start: LEFT PANEL ========
		JPanel leftPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);
		//Create text search
		txtSearch = new JTextField();
		txtSearch.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void removeUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void insertUpdate(DocumentEvent e) {
				changeListingListener();
			}
		});
		SwingTools.createLabelledComponent("Search:", txtSearch, leftPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));

		//Create combo box of options
		arrViewingOptions = new String[] {
				"View all plugins",
				"View installed plugins only",
				"View uninstalled plugins only",
				"View up-to-date plugins only",
				"View update-able plugins only",
				"View Fiji plugins only",
				"View Non-Fiji plugins only"
		};
		comboBoxViewingOptions = new JComboBox(arrViewingOptions);
		comboBoxViewingOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				changeListingListener();
			}
		});
		SwingTools.createLabelledComponent("View Options:", comboBoxViewingOptions, leftPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));

		//Create labels to annotate table
		SwingTools.createLabelPanel("Please choose what you want to install/uninstall:", leftPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));

		//Label text for plugin summaries
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());

		//Create the plugin table and set up its scrollpane
		table = new PluginTable(viewList, this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		leftPanel.add(pluginListScrollpane);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(lblSummaryPanel);
		//======== End: LEFT PANEL ========

		//======== Start: RIGHT PANEL ========
		JPanel rightPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);

		rightPanel.add(Box.createVerticalGlue());
		if (pluginManager.isDeveloper()) {
			JPanel editButtonPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
			editButtonPanel.add(Box.createHorizontalGlue());
			btnEditDetails = SwingTools.createButton("Edit Details",
					"Edit selected plugin's details", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					clickToEditDescriptions();
				}
			}, editButtonPanel);
			btnEditDetails.setEnabled(false);
			rightPanel.add(editButtonPanel);
		}

		//Create textpane to hold the information and its container tabbed pane
		txtPluginDetails = new TextPaneDisplay();
		SwingTools.getSingleTabbedPane(txtPluginDetails,
				"Details", "Individual Plugin information", 350, 315, rightPanel);
		rightPanel.add(Box.createRigidArea(new Dimension(0,25)));
		//======== End: RIGHT PANEL ========

		//======== Start: TOP PANEL (LEFT + RIGHT) ========
		JPanel topPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));
		//======== End: TOP PANEL (LEFT + RIGHT) ========

		//======== Start: BOTTOM PANEL ========
		JPanel bottomPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

		//Button to start actions
		btnStart = SwingTools.createButton("Apply changes",
				"Start installing/uninstalling plugins", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}
		}, bottomPanel);
		btnStart.setEnabled(false);

		//includes button to upload to server if is a Developer using
		if (pluginManager.isDeveloper()) {
			bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
			btnUpload = SwingTools.createButton("Upload to server",
					"Upload selected plugins to server", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					clickToUploadRecords();
				}
			}, bottomPanel);
		}
		bottomPanel.add(Box.createHorizontalGlue());

		//Button to quit Plugin Manager
		SwingTools.createButton("Cancel",
				"Exit Plugin Manager without applying changes", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}
		}, bottomPanel);
		//======== End: BOTTOM PANEL ========

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);

		//initial selection
		table.changeSelection(0, 0, false, false);
	}

	//Whenever search text or ComboBox has been changed
	private void changeListingListener() {
		viewList = pluginManager.pluginCollection;
		if (!txtSearch.getText().trim().isEmpty())
			viewList = pluginManager.pluginCollection.getMatchingText(txtSearch.getText().trim());

		int index = comboBoxViewingOptions.getSelectedIndex();
		if (index == 1)
			viewList = viewList.getStatusesFullyUpdated();
		else if (index == 2)
			viewList = viewList.getStatusesUninstalled();
		else if (index == 3)
			viewList = viewList.getStatusesInstalled();
		else if (index == 4)
			viewList = viewList.getStatusesUpdateable();
		else if (index == 5)
			viewList = viewList.getFijiPlugins();
		else if (index == 6)
			viewList = viewList.getNonFiji();

		//Directly update the table for display
		table.setupTableModel(viewList);
	}

	private void clickToUploadRecords() {
		//There's no frame interface for Uploader, makes disabling pointless, thus set invisible
		Uploader uploader = new Uploader(this);
		setEnabled(false);
		uploader.setUploadInformationAndStart(pluginManager);
	}

	private void clickToEditDescriptions() {
		loadedFrame = new DetailsEditor(this, currentPlugin);
		showFrame();
		setEnabled(false);
	}

	private void clickToBeginOperations() {
		loadedFrame = new Confirmation(this);
		Confirmation confirmation = (Confirmation)loadedFrame;
		confirmation.displayInformation(new DependencyBuilder(pluginManager.pluginCollection));
		showFrame();
		setEnabled(false);
	}

	private void clickToQuitPluginManager() {
		//if there exists plugins where actions have been specified by user
		if (pluginManager.pluginCollection.getActionsSpecified().size() > 0) {
			if (JOptionPane.showConfirmDialog(this,
					"You have specified changes. Are you sure you want to quit?",
					"Quit?", JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
				return;
			}
		}
		dispose();
	}

	private void showFrame() {
		if (loadedFrame != null) {
			loadedFrame.setVisible(true);
			loadedFrame.setLocationRelativeTo(null); //center of the screen
		}
	}

	public void openDownloader() {
		backToPluginManager();
		loadedFrame = new Installer(this);
		showFrame();
		Installer installer = (Installer)loadedFrame;
		installer.setInstaller(new UpdateTracker(pluginManager.pluginCollection));
		setEnabled(false);
	}

	public void backToPluginManager() {
		removeLoadedFrameIfExists();
		setEnabled(true);
		setVisible(true);
	}

	public void exitWithRestartFijiMessage() {
		removeLoadedFrameIfExists();
		IJ.showMessage("Restart Fiji", "You must restart Fiji application for the Plugin status changes to take effect.");
		dispose();
	}

	public void exitWithRestartMessage(String title, String message) {
		IJ.showMessage(title, message);
		dispose();
	}

	private void removeLoadedFrameIfExists() {
		if (loadedFrame != null) {
			loadedFrame.setVisible(false);
			loadedFrame.dispose();
			loadedFrame = null;
		}
	}

	public void displayPluginDetails(PluginObject currentPlugin) {
		this.currentPlugin = currentPlugin;
		if (txtPluginDetails != null)
			((TextPaneDisplay)txtPluginDetails).showPluginDetails(currentPlugin);

		//Enable/Disable edit button depending on Action of selected plugin
		if (pluginManager.isDeveloper()) //This button only exists if is a Developer
			btnEditDetails.setEnabled(currentPlugin.toUpload());
	}

	public void tableChanged(TableModelEvent e) {
		int size = pluginManager.pluginCollection.size();
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;
		int uploadCount = 0;

		//Refresh count information
		for (PluginObject myPlugin : pluginManager.pluginCollection) {
			if (myPlugin.toInstall()) {
				installCount += 1;
			} else if (myPlugin.toRemove()) {
				removeCount += 1;
			} else if (myPlugin.toUpdate()) {
				updateCount += 1;
			} else if (pluginManager.isDeveloper() &&
					myPlugin.toUpload()) {
				uploadCount += 1;
			}
		}
		String txtAction = "Total: " + size + ", To install: " + installCount +
		", To remove: " + removeCount + ", To update: " + updateCount;
		if (pluginManager.isDeveloper())
			txtAction += ", To upload: " + uploadCount;
		lblPluginSummary.setText(txtAction);

		//Refresh plugin details and status
		if (pluginManager.isDeveloper() && btnEditDetails != null) {
			if (currentPlugin != null)
				displayPluginDetails(currentPlugin);
			else
				btnEditDetails.setEnabled(false);
		}
		enableIfAnyChange(btnStart);
		enableIfAnyUpload(btnUpload);
	}

	private void enableIfAnyUpload(JButton button) {
		enableIfActions(button, pluginManager.pluginCollection.getToUpload().size());
	}

	private void enableIfAnyChange(JButton button) {
		enableIfActions(button, pluginManager.pluginCollection.getNonUploadActions().size());
	}

	private void enableIfActions(JButton button, int size) {
		if (button != null)
			button.setEnabled(size > 0);
	}

	public boolean isDeveloper() {
		return pluginManager.isDeveloper();
	}
}
