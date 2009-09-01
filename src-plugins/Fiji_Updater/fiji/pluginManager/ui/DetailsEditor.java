package fiji.pluginManager.ui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import fiji.pluginManager.logic.PluginObject;

public class DetailsEditor extends JFrame {
	private DocumentListener changeListener;
	private MainUserInterface mainUserInterface;
	private PluginObject selectedPlugin;
	private JTextPane[] txtEdits; //0: Authors, 1: Description, 2: Links
	private boolean textChanged;

	public DetailsEditor(MainUserInterface mainUserInterface, PluginObject selectedPlugin) {
		super("Description Editor: " + selectedPlugin.getFilename());
		this.mainUserInterface = mainUserInterface;
		this.selectedPlugin = selectedPlugin;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel uiPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);
		uiPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		changeListener = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void insertUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void removeUpdate(DocumentEvent e) {
				textChanged = true;
			}
		};

		txtEdits = new JTextPane[3];
		for (int i = 0; i < txtEdits.length; i++)
			txtEdits[i] = new JTextPane();

		SwingTools.createLabelPanel("For multiple authors or links, separate each using a new line.", uiPanel);
		SwingTools.createLabelPanel("Authors(s):", uiPanel);
		SwingTools.getTextScrollPane(txtEdits[0], 450, 120,
				selectedPlugin.getPluginDetails().getAuthors(), changeListener, uiPanel);

		SwingTools.createLabelPanel("Description:", uiPanel);
		SwingTools.getTextScrollPane(txtEdits[1], 450, 200,
				selectedPlugin.getPluginDetails().getDescription(), changeListener, uiPanel);

		SwingTools.createLabelPanel("Link(s):", uiPanel);
		SwingTools.getTextScrollPane(txtEdits[2], 450, 120,
				selectedPlugin.getPluginDetails().getLinks(), changeListener, uiPanel);

		textChanged = false;

		JPanel buttonPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		//Button to save description
		SwingTools.createButton("Save", "Save Description", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveText();
			}
		}, buttonPanel);
		buttonPanel.add(Box.createHorizontalGlue());

		//Button to cancel and return to Plugin Manager
		SwingTools.createButton("Close", "Exit Description Editor", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				backToPluginManager();
			}
		}, buttonPanel);
		uiPanel.add(buttonPanel);

		getContentPane().add(uiPanel);
	}

	private void backToPluginManager() {
		if (textChanged) {
			int option = JOptionPane.showConfirmDialog(this,
					"Description has changed.\n\nSave it before exiting Editor?",
					"Save?",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (option == JOptionPane.CANCEL_OPTION) {
				return;
			} else if (option == JOptionPane.YES_OPTION) {
				saveText();
			} //else ("No"), just exit
		}
		mainUserInterface.backToPluginManager();
	}

	private void saveText() {
		selectedPlugin.getPluginDetails().setDescription(txtEdits[1].getText().trim());
		selectedPlugin.getPluginDetails().setLinks(txtEdits[2].getText().trim().split("\n"));
		selectedPlugin.getPluginDetails().setAuthors(txtEdits[0].getText().trim().split("\n"));
		mainUserInterface.displayPluginDetails(selectedPlugin);
		textChanged = false;
	}
}
