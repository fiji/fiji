package fiji.updater.ui;

import com.jcraft.jsch.UserInfo;

import fiji.updater.Updater;

import fiji.updater.logic.FileUploader;
import fiji.updater.logic.Installer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.DependencyMap;
import fiji.updater.logic.PluginCollection.UpdateSite;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;
import fiji.updater.logic.PluginUploader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;
import fiji.updater.util.StderrProgress;
import fiji.updater.util.UpdateJava;
import fiji.updater.util.Util;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.TextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
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

public class UpdaterFrame extends JFrame implements TableModelListener, ListSelectionListener {
	protected PluginCollection plugins;

	protected JTextField txtSearch;
	protected ViewOptions viewOptions;
	protected PluginTable table;
	protected JLabel lblPluginSummary;
	protected PluginDetails pluginDetails;
	protected JButton apply, cancel, easy, updateSites;
	protected boolean easyMode;

	//For developers
	protected JButton upload;
	boolean canUpload;
	protected boolean hidden;

	public UpdaterFrame(PluginCollection plugins) {
		this(plugins, false);
	}

	public UpdaterFrame(final PluginCollection plugins, boolean hidden) {
		super("Fiji Updater");

		this.plugins = plugins;
		this.hidden = hidden;

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		//======== Start: LEFT PANEL ========
		JPanel leftPanel = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		leftPanel.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints(0, 0,  // x, y
				                              9, 1,  // rows, cols
							      0, 0,  // weightx, weighty
							      GridBagConstraints.NORTHWEST, // anchor
							      GridBagConstraints.HORIZONTAL, // fill
							      new Insets(0,0,0,0),
							      0, 0); // ipadx, ipady

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
		JPanel searchPanel = SwingTools.labelComponentRigid("Search:", txtSearch);
		gb.setConstraints(searchPanel, c);
		leftPanel.add(searchPanel);

		Component box = Box.createRigidArea(new Dimension(0,10));
		c.gridy = 1;
		gb.setConstraints(box, c);
		leftPanel.add(box);

		viewOptions = new ViewOptions();
		viewOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePluginsTable();
			}
		});
	
		JPanel viewOptionsPanel = SwingTools.labelComponentRigid("View Options:", viewOptions);
		c.gridy = 2;
		gb.setConstraints(viewOptionsPanel, c); 
		leftPanel.add(viewOptionsPanel);

		box = Box.createRigidArea(new Dimension(0,10));
		c.gridy = 3;
		gb.setConstraints(box, c);
		leftPanel.add(box);

		//Create labels to annotate table
		JPanel chooseLabel = SwingTools.label("Please choose what you want to install/uninstall:", null);
		c.gridy = 4;
		gb.setConstraints(chooseLabel, c);
		leftPanel.add(chooseLabel);

		box = Box.createRigidArea(new Dimension(0,5));
		c.gridy = 5;
		gb.setConstraints(box, c);
		leftPanel.add(box);

		//Label text for plugin summaries
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = SwingTools.horizontalPanel();
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());

		//Create the plugin table and set up its scrollpane
		table = new PluginTable(this);
		table.getSelectionModel().addListSelectionListener(this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		c.gridy = 6;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		gb.setConstraints(pluginListScrollpane, c);
		leftPanel.add(pluginListScrollpane);

		box = Box.createRigidArea(new Dimension(0,5));
		c.gridy = 7;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gb.setConstraints(box, c);
		leftPanel.add(box);

		c.gridy = 8;
		gb.setConstraints(lblSummaryPanel, c);
		leftPanel.add(lblSummaryPanel);

		//======== End: LEFT PANEL ========

		//======== Start: RIGHT PANEL ========
		JPanel rightPanel = SwingTools.verticalPanel();

		rightPanel.add(Box.createVerticalGlue());

		pluginDetails = new PluginDetails(this);
		SwingTools.tab(pluginDetails, "Details",
				"Individual Plugin information",
				350, 315, rightPanel);
		// TODO: put this into SwingTools, too
		rightPanel.add(Box.createRigidArea(new Dimension(0,25)));
		//======== End: RIGHT PANEL ========

		//======== Start: TOP PANEL (LEFT + RIGHT) ========
		JPanel topPanel = SwingTools.horizontalPanel();
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));
		//======== End: TOP PANEL (LEFT + RIGHT) ========

		//======== Start: BOTTOM PANEL ========
		JPanel bottomPanel = SwingTools.horizontalPanel();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
		bottomPanel.add(new PluginAction("Keep as-is", null));
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		bottomPanel.add(new PluginAction("Install", Action.INSTALL,
					"Update", Action.UPDATE));
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		bottomPanel.add(new PluginAction("Uninstall",
					Action.UNINSTALL));

		bottomPanel.add(Box.createHorizontalGlue());

		//Button to start actions
		apply = SwingTools.button("Apply changes",
				"Start installing/uninstalling plugins", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyChanges();
			}
		}, bottomPanel);
		apply.setEnabled(false);

		// Manage update sites
		updateSites = SwingTools.button("Manage update sites",
				"Manage multiple update sites for updating and uploading", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SitesDialog(UpdaterFrame.this, UpdaterFrame.this.plugins,
					UpdaterFrame.this.plugins.hasUploadableSites()).setVisible(true);
			}
		}, bottomPanel);

		//includes button to upload to server if is a Developer using
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		upload = SwingTools.button("Upload to server",
				"Upload selected plugins to server", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					public void run() {
						upload();
					}
				}.start();
			}
		}, bottomPanel);
		upload.setEnabled(false);
		upload.setVisible(plugins.hasUploadableSites());

		if (Util.isDeveloper) try {
			Class pluginChangesClass = Class.forName("fiji.scripting.ShowPluginChanges");
			if (pluginChangesClass != null && new File(System.getProperty("fiji.dir"), ".git").isDirectory()) {
				final PlugIn pluginChanges = (PlugIn)pluginChangesClass.newInstance();
				bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
				JButton showChanges = SwingTools.button("Show changes",
						"Show the changes in Git since the last upload", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						new Thread() {
							public void run() {
								for (PluginObject plugin : table.getSelectedPlugins())
									pluginChanges.run(plugin.filename);
							}
						}.start();
					}
				}, bottomPanel);
			}
		} catch (Exception e) { /* ignore */ }

		// offer to update Java, but only on non-Macs
		if (!IJ.isMacOSX() && new File(Util.fijiRoot, "java").canWrite()) {
			bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
			SwingTools.button("Update Java",
					"Update the Java version used for Fiji", new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new Thread() {
						public void run() {
							new UpdateJava(getProgress("Update Java")).run(null);
						}
					}.start();
				}
			}, bottomPanel);
		}

		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		easy = SwingTools.button("Toggle easy mode",
			"Toggle between easy and verbose mode",
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					toggleEasyMode();
				}
			}, bottomPanel);

		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		cancel = SwingTools.button("Close", "Exit Plugin Manager",
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

		SwingTools.addAccelerator(cancel, (JComponent)getContentPane(),
				cancel.getActionListeners()[0],
				KeyEvent.VK_ESCAPE, 0);

		addCustomViewOptions();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible && !hidden);
		if (visible)
			WindowManager.addWindow(this);
	}

	public void dispose() {
		WindowManager.removeWindow(this);
		super.dispose();
	}

	public Progress getProgress(String title) {
		if (hidden)
			return new StderrProgress();
		return new ProgressDialog(this, title);
	}

	public void valueChanged(ListSelectionEvent event) {
		pluginsChanged();
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
			if (table.isEditing())
				table.editingCanceled(null);
			for (PluginObject plugin : table.getSelectedPlugins()) {
				if (action == null)
					plugin.setNoAction();
				else if (!setAction(plugin))
					continue;
				table.firePluginChanged(plugin);
				pluginsChanged();
			}
		}

		protected boolean setAction(PluginObject plugin) {
			return plugin.setFirstValidAction(plugins, new Action[] {
					action, otherAction
			});
		}

		public void enableIfValid() {
			boolean enable = false, enableOther = false;

			for (PluginObject plugin : table.getSelectedPlugins()) {
				Status status = plugin.getStatus();
				if (action == null)
					enable = true;
				else if (status.isValid(action))
					enable = true;
				else if (status.isValid(otherAction))
					enableOther = true;
				if (enable && enableOther)
					break;
			}
			setText(!enableOther ? label :
				(enable ? label + "/" : "") + otherLabel);
			setEnabled(enable || enableOther);
		}
	}

	public void addCustomViewOptions() {
		viewOptions.clearCustomOptions();

		Collection<String> names = plugins.getUpdateSiteNames();
		if (names.size() > 1)
			for (String name : names)
				viewOptions.addCustomOption("View files of the '" + name + "' site", plugins.forUpdateSite(name));
	}

	public void setViewOption(ViewOptions.Option option) {
		viewOptions.setSelectedItem(option);
		updatePluginsTable();
	}

	public void updatePluginsTable() {
		Iterable<PluginObject> view = viewOptions.getView(table);
		// TODO: maybe we want to remember what was selected?
		table.clearSelection();

		String search = txtSearch.getText().trim();
		if (!search.equals(""))
			view = PluginCollection.filter(search, view);

		//Directly update the table for display
		table.setPlugins(view);
	}

	// TODO: once the editor is embedded, this can go
	private PluginObject getSingleSelectedPlugin() {
		int[] rows = table.getSelectedRows();
		return rows.length != 1 ? null : table.getPlugin(rows[0]);
	}

	public void applyChanges() {
		ResolveDependencies resolver = new ResolveDependencies(this, plugins);
		if (!resolver.resolve())
			return;
		new Thread() {
			public void run() {
				install();
			}
		}.start();
	}

	private void quit() {
		if (plugins.hasChanges()) {
			if (!SwingTools.showQuestion(hidden, this, "Quit?",
				"You have specified changes. Are you sure you want to quit?"))
			return;
		}
		else try {
			plugins.write();
		} catch (Exception e) {
			error("There was an error writing the local metadata cache: " + e);
		}
		dispose();
	}

	void setEasyMode(Container container) {
		for (Component child : container.getComponents()) {
			if ((child instanceof Container) &&
					child != table.getParent().getParent())
				setEasyMode((Container)child);
			if (child == upload && !easyMode && !plugins.hasUploadableSites())
				child.setVisible(false);
			else
				child.setVisible(!easyMode);
		}
	}

	public void setEasyMode(boolean easyMode) {
		this.easyMode = easyMode;
		setEasyMode(getContentPane());
		Component[] exempt = { table, easy, apply, cancel };
		for (Component child : exempt)
			for (; child != getContentPane();
					child = child.getParent())
				child.setVisible(true);
		easy.setText(easyMode ? "Advanced mode" : "Easy mode");
		if (isVisible())
			repaint();
	}

	public void toggleEasyMode() {
		setEasyMode(!easyMode);
	}

	public void install() {
		Installer installer = new Installer(plugins, getProgress("Installing..."));
		try {
			PluginCollection uninstalled = PluginCollection
				.clone(plugins.toUninstall());
			installer.start();
			for (PluginObject plugin : uninstalled)
				if (!plugin.isFiji())
					plugins.remove(plugin);
				else
					plugin.setStatus(plugin.isObsolete() ?
						Status.OBSOLETE_UNINSTALLED :
						Status.NOT_INSTALLED);
			updatePluginsTable();
			pluginsChanged();
			plugins.write();
			info("Updated successfully.  Please restart Fiji!");
			dispose();
		} catch (Canceled e) {
			// TODO: remove "update/" directory
			error("Canceled");
			installer.done();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: remove "update/" directory
			error("Installer failed: " + e);
			installer.done();
		}
	}

	public void updateTheUpdater() {
		PluginCollection.Filter filter = new PluginCollection.Filter() {
			public boolean matches(PluginObject plugin) {
				if (plugin.filename.equals("plugins/Fiji_Updater.jar")) {
					plugin.setAction(plugins, Action.UPDATE);
					return true;
				}
				return false;
			}
		};
		PluginCollection justTheUpdater = PluginCollection.clone(plugins.filter(filter));
		Installer installer = new Installer(justTheUpdater, getProgress("Installing the updater..."));
		try {
			installer.start();
		} catch (Canceled e) {
			// TODO: remove "update/" directory
			error("Canceled");
			installer.done();
		} catch (IOException e) {
			// TODO: remove "update/" directory
			error("Installer failed: " + e);
			installer.done();
		}
	}

	public void pluginsChanged() {
		// TODO: once this is editable, make sure changes are committed
		pluginDetails.reset();
		for (PluginObject plugin : table.getSelectedPlugins())
			pluginDetails.showPluginDetails(plugin);
		if (pluginDetails.getDocument().getLength() > 0 &&
				table.areAllSelectedPluginsUploadable())
			pluginDetails.setEditableForDevelopers();

		for (PluginAction button : pluginActions)
			button.enableIfValid();

		apply.setEnabled(plugins.hasChanges());
		cancel.setText(plugins.hasChanges() ? "Cancel" : "Close");

		if (plugins.hasUploadableSites())
			enableUploadOrNot();

		int size = plugins.size();
		int install = 0, uninstall = 0, upload = 0;
		long bytesToDownload = 0, bytesToUpload = 0;

		for (PluginObject plugin : plugins)
			switch (plugin.getAction()) {
			case INSTALL:
			case UPDATE:
				install++;
				bytesToDownload += plugin.filesize;
				break;
			case UNINSTALL:
				uninstall++;
				break;
			case UPLOAD:
				upload++;
				bytesToUpload += plugin.filesize;
				break;
			}
		int implicated = 0;
		DependencyMap map = plugins.getDependencies(true);
		for (PluginObject plugin : map.keySet()) {
			implicated++;
			bytesToUpload += plugin.filesize;
		}
		String text = "";
		if (install > 0)
			text += " install/update: " + install
				+ (implicated > 0 ? "+" + implicated : "")
				+ " download size: "
				+ sizeToString(bytesToDownload);
		if (uninstall > 0)
			text += " uninstall: " + uninstall;
		if (plugins.hasUploadableSites())
			text += ", upload: " + upload + ", upload size: "
				+ sizeToString(bytesToUpload);
		lblPluginSummary.setText(text);

	}

	protected final static String[] units = {"B", "kB", "MB", "GB", "TB"};
	public static String sizeToString(long size) {
		int i;
		for (i = 1; i < units.length && size >= 1l<<(10 * i); i++)
			; // do nothing
		if (--i == 0)
			return "" + size + units[i];
		// round
		size *= 100;
		size >>= (10 * i);
		size += 5;
		size /= 10;
		return "" + (size / 10) + "." + (size % 10) + units[i];
	}

	public void tableChanged(TableModelEvent e) {
		pluginsChanged();
	}

	// checkWritable() is guaranteed to be called after Checksummer ran
	public void checkWritable() {
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

	void markUploadable() {
		canUpload = true;
		enableUploadOrNot();
	}

	void enableUploadOrNot() {
		upload.setVisible(!easyMode && plugins.hasUploadableSites());
		upload.setEnabled(canUpload || plugins.hasUploadOrRemove());
	}

	protected void upload() {
		ResolveDependencies resolver =
			new ResolveDependencies(this, plugins, true);
		if (!resolver.resolve())
			return;

		String errors = plugins.checkConsistency();
		if (errors != null) {
			error(errors);
			return;
		}

		List<String> possibleSites =
			new ArrayList<String>(plugins.getSiteNamesToUpload());
		if (possibleSites.size() == 0) {
			error("Huh? No upload site?");
			return;
		}
		String updateSiteName;
		if (possibleSites.size() == 1)
			updateSiteName = possibleSites.get(0);
		else {
			updateSiteName = SwingTools.getChoice(hidden, this, possibleSites,
				"Which site do you want to upload to?", "Update site");
			if (updateSiteName == null)
				return;
		}
		PluginUploader uploader = new PluginUploader(plugins, updateSiteName);

		Progress progress = null;
		try {
			if (!uploader.hasUploader() && !interactiveSshLogin(uploader))
				return;
			progress = getProgress("Uploading...");
			uploader.upload(progress);
			for (PluginObject plugin : plugins.toUploadOrRemove())
				if (plugin.getAction() == Action.UPLOAD) {
					plugin.markUploaded();
					plugin.setStatus(Status.INSTALLED);
				}
				else {
					plugin.markRemoved();
					plugin.setStatus(Status
						.OBSOLETE_UNINSTALLED);
				}
			updatePluginsTable();
			canUpload = false;
			plugins.write();
			info("Uploaded successfully.");
			enableUploadOrNot();
			dispose();
		} catch (Canceled e) {
			// TODO: teach uploader to remove the lock file
			error("Canceled");
			if (progress != null)
				progress.done();
		} catch (Throwable e) {
			IJ.handleException(e);
			e.printStackTrace();
			error("Upload failed: " + e);
			if (progress != null)
				progress.done();
		}
	}

	protected boolean initializeUpdateSite(String url, String sshHost, String uploadDirectory) {
		String updateSiteName = "Dummy";
		PluginCollection plugins = new PluginCollection();
		plugins.addUpdateSite(updateSiteName, url, sshHost, uploadDirectory, Long.parseLong(Util.timestamp(-1)));
		PluginUploader uploader = new PluginUploader(plugins, updateSiteName);
		Progress progress = null;
		try {
			if (!uploader.hasUploader() && !interactiveSshLogin(uploader))
				return false;
			progress = getProgress("Initializing Update Site...");
			uploader.upload(progress);
			// JSch needs some time to finalize the SSH connection
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { /* ignore */ }
			return true;
		} catch (Canceled e) {
			if (progress != null)
				progress.done();
		} catch (Throwable e) {
			e.printStackTrace();
			IJ.handleException(e);
			if (progress != null)
				progress.done();
		}
		return false;
	}

	protected GenericDialog getPasswordDialog(String title, String username) {
		GenericDialog gd = new GenericDialog(title);
		if (username != null)
			gd.addStringField("Username", username, 20);
		gd.addStringField("Password", "", 20);

		final TextField pwd =
			(TextField)gd.getStringFields().lastElement();
		pwd.setEchoChar('*');
		if (username != null) {
			final TextField user =
				(TextField)gd.getStringFields().firstElement();
			if (!username.equals(""))
				user.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						pwd.requestFocus();
						user.removeFocusListener(this);
					}
				});
		}

		return gd;
	}

	protected UserInfo getUserInfo(final String password) {
		return new UserInfo() {
			protected String prompt;
			protected int count = 0;

			public String getPassphrase() {
				GenericDialog gd = getPasswordDialog(prompt, null);
				gd.showDialog();
				return gd.wasCanceled() ? null : gd.getNextString();
			}

			public String getPassword() {
				if (count == 1)
					return password;
				GenericDialog gd = getPasswordDialog(prompt, null);
				gd.showDialog();
				return gd.wasCanceled() ? null : gd.getNextString();
			}

			public boolean promptPassphrase(String message) {
				prompt = message;
				return count++ < 3;
			}

			public boolean promptPassword(String message) {
				prompt = message;
				return count++ < 4;
			}

			public boolean promptYesNo(String message) {
				return SwingTools.showYesNoQuestion(hidden, UpdaterFrame.this, "Password", message);
			}

			public void showMessage(String message) {
				info(message);
			}
		};
	}

	protected boolean interactiveSshLogin(PluginUploader uploader) {
		String username = uploader.getDefaultUsername();
		for (;;) {
			//Dialog to enter username and password
			GenericDialog gd = getPasswordDialog("Login", username);
			gd.showDialog();
			if (gd.wasCanceled())
				return false; //return back to user interface

			//Get the required login information
			username = gd.getNextString();
			String password = gd.getNextString();

			UserInfo userInfo = getUserInfo(password);
			if (uploader.setLogin(username, userInfo))
				break;
		}

		Prefs.set(Updater.PREFS_USER, username);
		return true;
	}

	public void error(String message) {
		SwingTools.showMessageBox(hidden, this, message, JOptionPane.ERROR_MESSAGE);
	}

	public void warn(String message) {
		SwingTools.showMessageBox(hidden, this, message, JOptionPane.WARNING_MESSAGE);
	}

	public void info(String message) {
		SwingTools.showMessageBox(hidden, this, message, JOptionPane.INFORMATION_MESSAGE);
	}
}