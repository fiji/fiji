package fiji.updater.ui;

import fiji.updater.Updater;

import fiji.updater.logic.Installer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;
import fiji.updater.logic.PluginUploader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class UpdaterFrame extends JFrame
		implements TableModelListener, ListSelectionListener {
	PluginCollection plugins;
	long xmlLastModified;

	private JFrame loadedFrame;
	private String[] arrViewingOptions;
	private JTextField txtSearch;
	private JComboBox viewOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	// TODO: this _is_ a TextPaneDisplay.  (Oh, and rename it to PluginDetails)
	private JTextPane txtPluginDetails;
	private PluginObject currentPlugin;
	private JButton btnStart;

	//For developers
	// TODO: no more Hungarian notation
	private JButton btnUpload;
	private JButton btnEditDetails;

	public UpdaterFrame() {
		super("Plugin Manager");

		plugins = PluginCollection.getInstance();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		//======== Start: LEFT PANEL ========
		JPanel leftPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);
		txtSearch = new JTextField();
		txtSearch.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				updatePluginsTable();
			}

			public void removeUpdate(DocumentEvent e) {
				updatePluginsTable();
			}

			public void insertUpdate(DocumentEvent e) {
				updatePluginsTable();
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
				updatePluginsTable();
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
		table = new PluginTable(plugins);
		table.getSelectionModel().addListSelectionListener(this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		leftPanel.add(pluginListScrollpane);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(lblSummaryPanel);
		//======== End: LEFT PANEL ========

		//======== Start: RIGHT PANEL ========
		// TODO: do we really want to win the "Who can make the longest function names?" contest?
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
		bottomPanel.add(new PluginAction("Keep as-is", null));
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		bottomPanel.add(new PluginAction("Install", Action.INSTALL,
			"Update", Action.UPDATE));
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		bottomPanel.add(new PluginAction("Remove", Action.REMOVE));

		bottomPanel.add(Box.createHorizontalGlue());

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
			btnUpload.setEnabled(false);
		}

		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		SwingTools.createButton("Cancel", "Exit Plugin Manager",
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					quit();
				}
			}, bottomPanel);
		//======== End: BOTTOM PANEL ========

		getContentPane().setLayout(new BoxLayout(getContentPane(),
					BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);

		table.getModel().addTableModelListener(this);

		pack();
		setVisible(true);
	}

	public Progress getProgress(String title) {
		return new ProgressDialog(this, title);
	}

	public void valueChanged(ListSelectionEvent event) {
		table.requestFocusInWindow();
		setCurrentPlugin(table.getSelectedPlugin());
	}

	List<PluginAction> pluginActions = new ArrayList<PluginAction>();

	class PluginAction extends JButton implements ActionListener {
		String label, otherLabel;
		Action action, otherAction;

		PluginAction(String label, Action action) {
			this(label, action, null, null);
		}

		PluginAction(String label, Action action,
				String otherLabel, Action otherAction) {
			super(label);
			this.label = label;
			this.action = action;
			this.otherLabel = otherLabel;
			this.otherAction = otherAction;
			addActionListener(this);
			pluginActions.add(this);
		}

		public void actionPerformed(ActionEvent e) {
			if (currentPlugin == null)
				return;
			if (table.isEditing())
				table.editingCanceled(null);
			if (action == null)
				currentPlugin.setNoAction();
			else
				currentPlugin.setAction(action);
			/* table.getModel().setValueAt(currentPlugin.getAction(),
				table.getSelectedRow(), 1); */
			table.firePluginChanged(currentPlugin);
		}

		public void enableIfValid() {
			boolean enable = false;

			if (currentPlugin != null) {
				Status status = currentPlugin.getStatus();
				if (action == null)
					enable = true;
				else if (status.isValid(action))
					enable = true;
				else if (status.isValid(otherAction)) {
					String dummy = label;
					label = otherLabel;
					otherLabel = dummy;
					Action dummyAction = action;
					action = otherAction;
					otherAction = dummyAction;
					setLabel(label);
					enable = true;
				}
			}
			setEnabled(enable);
		}
	}

	public void updatePluginsTable() {
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
		table.setPlugins(view);
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
		new Thread() {
			public void run() {
				download();
			}
		}.start();
	}

	private void quit() {
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
		Installer installer =
			new Installer(getProgress("Installing..."));
		try {
			installer.start();
			updatePluginsTable();
		} catch (Canceled e) {
			// TODO: remove "update/" directory
			IJ.error("Canceled");
		} catch (IOException e) {
			// TODO: remove "update/" directory
			// TODO: make error() method
			IJ.error("Installer failed: " + e);
		}
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

	public void setCurrentPlugin(PluginObject plugin) {
		currentPlugin = plugin;
		if (txtPluginDetails != null)
			((TextPaneDisplay)txtPluginDetails).showPluginDetails(plugin);

		for (PluginAction button : pluginActions)
			button.enableIfValid();

		btnStart.setEnabled(plugins.hasChanges());

		// TODO: "Upload" is activated by default!"
		if (Util.isDeveloper) {
			btnEditDetails.setEnabled(plugin != null);
			btnUpload.setEnabled(plugins.hasUpload());
		}
	}

	public void tableChanged(TableModelEvent e) {
		int size = plugins.size();
		int install = 0;
		int remove = 0;
		int update = 0;
		int upload = 0;

		//Refresh count information
		for (PluginObject myPlugin : plugins)
			if (myPlugin.toInstall())
				install += 1;
			else if (myPlugin.toRemove())
				remove += 1;
			else if (myPlugin.toUpdate())
				update += 1;
			else if (myPlugin.toUpload())
				upload += 1;
		String text = "Total: " + size + ", To install: " + install
			+ ", To remove: " + remove + ", To update: " + update;
		if (Util.isDeveloper)
			text += ", To upload: " + upload;
		lblPluginSummary.setText(text);

		setCurrentPlugin(currentPlugin);
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

	public void setLastModified(long lastModified) {
		xmlLastModified = lastModified;

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
	}

	protected void upload() {
		PluginUploader uploader = new PluginUploader(xmlLastModified);

		try {
			if (!interactiveSshLogin(uploader))
				return;
			uploader.upload(getProgress("Uploading..."));
			// TODO: download list instead
			IJ.showMessage("You need to restart this plugin now");
		} catch (Canceled e) {
			// TODO: teach uploader to remove the lock file
			IJ.error("Canceled");
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
