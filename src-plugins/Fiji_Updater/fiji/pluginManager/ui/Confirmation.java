package fiji.pluginManager.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import fiji.pluginManager.logic.DependencyBuilder;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginObject;

public class Confirmation extends JFrame {
	private MainUserInterface mainUserInterface;
	private JTextPane txtPluginList;
	private JTextPane txtAdditionalList;
	private JTextPane txtConflictsList;
	private JLabel lblStatus;
	private JButton btnDownload;
	private String msgConflictExists = "Conflicts exist. Please return to resolve them.";
	private String msgConflictNone = "No conflicts found. You may proceed.";
	private DependencyBuilder dependencyBuilder;

	public Confirmation(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
		setupUserInterface();
		pack();
	}

	private void setupUserInterface() {
		setTitle("Dependency and Conflict check");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* Create a panel to hold the text panes of plugin info */
		JPanel listsPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		listsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		/* Create textpane to hold the information and its container tabbed pane */
		txtPluginList = new TextPaneDisplay();
		SwingTools.getSingleTabbedPane(txtPluginList,
				"Selected Plugins", "Your selection of plugins", 400, 260, listsPanel);

		listsPanel.add(Box.createRigidArea(new Dimension(15,0)));

		/* Create textpane to hold the information and its container tabbed pane */
		txtAdditionalList = new TextPaneDisplay();
		SwingTools.getSingleTabbedPane(txtAdditionalList,
				"Additional Changes", "Additional installations or removals to be made due to dependencies",
				260, 260, listsPanel);

		/* Create textpane to hold the information and its scrollpane */
		txtConflictsList = new TextPaneDisplay();
		JScrollPane txtScrollpane3 = SwingTools.getTextScrollPane(txtConflictsList, 675, 120, null);
		JPanel conflictsPanel = new JPanel();
		conflictsPanel.setLayout(new BorderLayout());
		conflictsPanel.add(txtScrollpane3, BorderLayout.CENTER);
		conflictsPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

		JPanel buttonPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		lblStatus = new JLabel();
		buttonPanel.add(lblStatus);
		buttonPanel.add(Box.createHorizontalGlue());

		//Buttons to start actions
		btnDownload = SwingTools.createButton("Confirm changes",
				"Start installing/uninstalling", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startActualChanges();
			}
		}, buttonPanel);
		btnDownload.setEnabled(false);

		buttonPanel.add(Box.createHorizontalGlue());

		SwingTools.createButton("Cancel", "Cancel and return to Plugin Manager", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				backToframeManager();
			}
		}, buttonPanel);

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(listsPanel);
		getContentPane().add(conflictsPanel);
		getContentPane().add(buttonPanel);
	}

	private void startActualChanges() {
		//indicate the actions as reference for Downloader (Installer) to refer to
		dependencyBuilder.toInstallList.setToInstall();
		dependencyBuilder.toUpdateList.setToUpdate();
		dependencyBuilder.toRemoveList.setToRemove();
		dependencyBuilder = null;
		mainUserInterface.openDownloader();
	}

	private void backToframeManager() {
		mainUserInterface.backToPluginManager();
	}

	public void displayInformation(DependencyBuilder dependencyBuilder) {
		this.dependencyBuilder = dependencyBuilder;

		// ********** Display of plugins listed by user **********
		PluginCollection installs = dependencyBuilder.toInstallList.getToInstall();
		PluginCollection updates = dependencyBuilder.toUpdateList.getToUpdate();
		PluginCollection removals = dependencyBuilder.toRemoveList.getToUninstall();

		// Actual display of information, textpane explicitly set by user to take action
		TextPaneDisplay txtPluginList = (TextPaneDisplay) this.txtPluginList;
		if (installs.size() > 0)
			txtPluginList.insertPluginDescriptions("Install", installs);
		if (updates.size() > 0)
			txtPluginList.insertPluginDescriptions("Update", updates);
		if (removals.size() > 0)
			txtPluginList.insertPluginDescriptions("Remove", removals);
		txtPluginList.scrollToTop();
		// ********** End display of plugins listed by user **********

		// ********** Display of involved plugins which are not listed by user **********
		//Objective is to show user only information that was previously invisible
		PluginCollection additionalInstalls = dependencyBuilder.toInstallList.getUnlistedForInstall();
		PluginCollection addtionalUpdates = dependencyBuilder.toUpdateList.getUnlistedForUpdate();
		PluginCollection addtionalRemovals = dependencyBuilder.toRemoveList.getUnlistedForUninstall();

		// textpane listing additional plugins to add/remove
		TextPaneDisplay txtAdditionalList = (TextPaneDisplay) this.txtAdditionalList;
		if (additionalInstalls.size() > 0)
			txtAdditionalList.insertPluginNamelist("To Install", additionalInstalls);
		if (addtionalUpdates.size() > 0) {
			if (additionalInstalls.size() > 0)
				txtAdditionalList.insertBlankLine();
			txtAdditionalList.insertPluginNamelist("To Update", addtionalUpdates);
		}
		if (addtionalRemovals.size() > 0) {
			if (additionalInstalls.size() > 0 || addtionalUpdates.size() > 0)
				txtAdditionalList.insertBlankLine();
			txtAdditionalList.insertPluginNamelist("To Remove", addtionalRemovals);
		}
		if (additionalInstalls.size() == 0 && addtionalUpdates.size() == 0
				&& addtionalRemovals.size() == 0)
			txtAdditionalList.setText("None.");
		txtAdditionalList.scrollToTop();
		// ********** End display of involved plugins which are not listed by user **********

		// ********** Display of conflicts (if any) **********
		//Compile a list of plugin names that conflicts with uninstalling (if any)
		Map<PluginObject,PluginCollection> installDependenciesMap = dependencyBuilder.installDependenciesMap;
		Map<PluginObject,PluginCollection> updateDependenciesMap = dependencyBuilder.updateDependenciesMap;
		Map<PluginObject,PluginCollection> uninstallDependentsMap = dependencyBuilder.uninstallDependentsMap;

		List<String[]> installConflicts = new ArrayList<String[]>();
		List<String[]> updateConflicts = new ArrayList<String[]>();
		Iterator<PluginObject> iterInstall = installDependenciesMap.keySet().iterator();
		while (iterInstall.hasNext()) {
			PluginObject pluginAdd = iterInstall.next();
			PluginCollection pluginInstallList = installDependenciesMap.get(pluginAdd);
			PluginCollection pluginUpdateList = updateDependenciesMap.get(pluginAdd);
			Iterator<PluginObject> iterUninstall = uninstallDependentsMap.keySet().iterator();
			while (iterUninstall.hasNext()) {
				PluginObject pluginUninstall = iterUninstall.next();
				PluginCollection pluginUninstallList = uninstallDependentsMap.get(pluginUninstall);

				if (dependencyBuilder.conflicts(pluginInstallList, pluginUpdateList, pluginUninstallList)) {
					String installName = pluginAdd.getFilename();
					String uninstallName = pluginUninstall.getFilename();
					String[] arrNames = {installName, uninstallName};
					if (pluginAdd.isUpdateable()) {
						updateConflicts.add(arrNames);
					} else {
						installConflicts.add(arrNames);
					}
				}
			}
		}

		// conflicts list textpane
		TextPaneDisplay txtConflictsList = (TextPaneDisplay) this.txtConflictsList;
		for (String[] names : installConflicts)
			txtConflictsList.normal("Installing " + names[0]
					+ " would conflict with uninstalling " + names[1] + "\n");
		for (String[] names : updateConflicts)
			txtConflictsList.normal("Updating " + names[0]
					+ " would conflict with uninstalling " + names[1] + "\n");
		txtConflictsList.scrollToTop();
		// ********** End display of conflicts (if any) **********

		// enable download button if no conflicts recorded
		if (installConflicts.size() == 0 && updateConflicts.size() == 0) {
			txtConflictsList.normal("None.");
			btnDownload.setEnabled(true);
			lblStatus.setText(msgConflictNone);
			lblStatus.setForeground(Color.GREEN);
		} else {
			// otherwise, prevent user from clicking to download
			btnDownload.setEnabled(false);
			lblStatus.setText(msgConflictExists);
			lblStatus.setForeground(Color.RED);
		}

	}
}
