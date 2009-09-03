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
		if (dependencyBuilder.conflicts())
			return;
		// TODO: bah!  Confirmation now serves as a controller?  Messy!
		mainUserInterface.openDownloader();
	}

	private void backToframeManager() {
		// TODO: bah!  Confirmation now serves as a controller?  Messy!
		mainUserInterface.backToPluginManager();
	}
}
