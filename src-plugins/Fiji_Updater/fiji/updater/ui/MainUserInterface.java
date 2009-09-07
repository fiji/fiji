package fiji.updater.ui;

import fiji.updater.Updater;

import fiji.updater.logic.Installer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginUploader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Util;

import ij.IJ;
import ij.Prefs;

import ij.gui.GenericDialog;

import java.awt.Dimension;
import java.awt.TextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import java.io.File;

import java.util.Observable;
import java.util.Observer;

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

public class MainUserInterface extends JFrame implements TableModelListener {
	PluginCollection plugins;
	long xmlLastModified;

	private JFrame loadedFrame;
	private String[] arrViewingOptions;
	private JTextField txtSearch;
	private JComboBox viewOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private PluginObject currentPlugin;
	private JButton btnStart;

	//For developers
	// TODO: no more Hungarian notation
	private JButton btnUpload;
	private JButton btnEditDetails;

	public MainUserInterface(long xmlLastModified) {
		super("Plugin Manager");

		plugins = PluginCollection.getInstance();

		String list = null;
		for (PluginObject plugin : plugins) {
			File file = new File(Util.prefix(plugin.getFilename()));
			if (!file.exists() || file.canWrite())
				continue;
			if (list == null)
				list = plugin.getFilename();
			else
				list += ", " + plugin.getFilename();
		}
		if (list != null)
			IJ.showMessage("Read-only Plugins",
					"WARNING: The following plugin files "
					+ "are set to read-only: '"
					+ list + "'");
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
		viewOptions = new JComboBox(arrViewingOptions);
		viewOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				changeListingListener();
			}
		});
		SwingTools.createLabelledComponent("View Options:", viewOptions, leftPanel);
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
		table = new PluginTable(plugins, this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		leftPanel.add(pluginListScrollpane);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(lblSummaryPanel);
		//======== End: LEFT PANEL ========

		//======== Start: RIGHT PANEL ========
		JPanel rightPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);

		rightPanel.add(Box.createVerticalGlue());
		if (Util.isDeveloper) {
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
		if (Util.isDeveloper) {
			bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
			btnUpload = SwingTools.createButton("Upload to server",
					"Upload selected plugins to server", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					upload();
				}
			}, bottomPanel);
		}
		bottomPanel.add(Box.createHorizontalGlue());

		//Button to quit Plugin Manager
		SwingTools.createButton("Cancel",
				"Exit Plugin Manager without applying changes", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clickToQuitUpdater();
			}
		}, bottomPanel);
		//======== End: BOTTOM PANEL ========

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);

		table.getModel().addTableModelListener(this);

		//initial selection
		table.changeSelection(0, 0, false, false);
	}

	//Whenever search text or ComboBox has been changed
	private void changeListingListener() {
		Iterable<PluginObject> view;

		// TODO: OUCH!
		int index = viewOptions.getSelectedIndex();
		if (index == 1)
			view = plugins.installed();
		else if (index == 2)
			view = plugins.uninstalled();
		else if (index == 3)
			view = plugins.upToDate();
		else if (index == 4)
			view = plugins.updateable();
		else if (index == 5)
			view = plugins.fijiPlugins();
		else if (index == 6)
			view = plugins.nonFiji();
		else
			view = plugins;

		String search = txtSearch.getText().trim();
		if (!search.equals(""))
			view = PluginCollection.filter(search, view);

		//Directly update the table for display
		table.setupTableModel(view);
		table.getModel().addTableModelListener(this);
	}

	// TODO: why should this function need to know that it is triggered by a click?  That is so totally unnecessary.
	private void clickToEditDescriptions() {
		// TODO: embed this, rather than having an extra editor
		loadedFrame = new DetailsEditor(this, currentPlugin);
		showFrame();
		setEnabled(false);
	}

	private void clickToBeginOperations() {
		// TODO: check conflicts
		download();
	}

	private void clickToQuitUpdater() {
		//if there exists plugins where actions have been specified by user
		if (plugins.hasChanges() &&
				JOptionPane.showConfirmDialog(this,
					"You have specified changes. Are you "
					+ "sure you want to quit?",
					"Quit?", JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) !=
				JOptionPane.YES_OPTION)
			return;
		dispose();
	}

	private void showFrame() {
		if (loadedFrame != null) {
			loadedFrame.setVisible(true);
			loadedFrame.setLocationRelativeTo(null); //center of the screen
		}
	}

	public void download() {
		new Thread() {
			public void run() {
				// TODO: use ProgressPane
				// TODO: handle Cancel
				Installer installer =
					new Installer(new IJProgress());
				try {
					installer.start();
				} catch (Exception e) {
					// TODO: make error() method
					IJ.error("Installer failed: " + e);
				}
			}
		}.start();
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
		if (Util.isDeveloper) //This button only exists if is a Developer
			btnEditDetails.setEnabled(currentPlugin.toUpload());
	}

	public void tableChanged(TableModelEvent e) {
		int size = plugins.size();
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;
		int uploadCount = 0;

		//Refresh count information
		for (PluginObject myPlugin : plugins) {
			if (myPlugin.toInstall()) {
				installCount += 1;
			} else if (myPlugin.toRemove()) {
				removeCount += 1;
			} else if (myPlugin.toUpdate()) {
				updateCount += 1;
			} else if (Util.isDeveloper &&
					myPlugin.toUpload()) {
				uploadCount += 1;
			}
		}
		String txtAction = "Total: " + size + ", To install: " + installCount +
		", To remove: " + removeCount + ", To update: " + updateCount;
		if (Util.isDeveloper)
			txtAction += ", To upload: " + uploadCount;
		lblPluginSummary.setText(txtAction);

		//Refresh plugin details and status
		if (Util.isDeveloper && btnEditDetails != null) {
			if (currentPlugin != null)
				displayPluginDetails(currentPlugin);
			else
				btnEditDetails.setEnabled(false);
		}
		enableIfAnyChange(btnStart);
		// TODO: "Upload" is activated by default!"
		enableIfAnyUpload(btnUpload);
	}

	private void enableIfAnyUpload(JButton button) {
		enableIfActions(button, plugins.hasUpload());
	}

	private void enableIfAnyChange(JButton button) {
		enableIfActions(button, plugins.hasChanges());
	}

	private void enableIfActions(JButton button, boolean flag) {
		button.setEnabled(flag);
	}

	protected void upload() {
		PluginUploader uploader = new PluginUploader(xmlLastModified);

		try {
			if (!interactiveSshLogin(uploader))
				return;
			uploader.upload(new IJProgress());
		} catch (Throwable e) {
			e.printStackTrace();
			IJ.error("Upload failed: " + e);
		}
	}

	protected boolean interactiveSshLogin(PluginUploader uploader) {
		String username = Prefs.get(Updater.PREFS_USER, "");
		String password = "";
		do {
			//Dialog to enter username and password
			GenericDialog gd = new GenericDialog("Login");
			gd.addStringField("Username", username, 20);
			gd.addStringField("Password", "", 20);

			final TextField user =
				(TextField)gd.getStringFields().firstElement();
			final TextField pwd =
				(TextField)gd.getStringFields().lastElement();
			pwd.setEchoChar('*');
			if (!username.equals(""))
				user.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						pwd.requestFocus();
						user.removeFocusListener(this);
					}
				});

			gd.showDialog();
			if (gd.wasCanceled())
				return false; //return back to user interface

			//Get the required login information
			username = gd.getNextString();
			password = gd.getNextString();

		} while (!uploader.setLogin(username, password));

		Prefs.set(Updater.PREFS_USER, username);
		return true;
	}

}
