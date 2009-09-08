package fiji.updater.ui;

import fiji.updater.logic.PluginObject;

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

// TODO: this should not be a frame, but a JPanel, and it should automatically
// update the plugin details, activating the "Upload" button.
public class DetailsEditor extends JFrame {
	private DocumentListener changeListener;
	private UpdaterFrame updaterFrame;
	private PluginObject selectedPlugin;
	private JTextPane[] txtEdits; //0: Authors, 1: Description, 2: Links
	private boolean textChanged;

	public DetailsEditor(UpdaterFrame frame, PluginObject selectedPlugin) {
		super("Description Editor: " + selectedPlugin.getFilename());
		updaterFrame = frame;
		this.selectedPlugin = selectedPlugin;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel uiPanel = SwingTools.verticalPanel();
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

		SwingTools.label("For multiple authors or links, separate each "
			+ "using a new line.", uiPanel);
		SwingTools.label("Authors(s):", uiPanel);
		txtEdits[0] = SwingTools.scrolledText(450, 120,
				selectedPlugin.getAuthors(),
				changeListener, uiPanel);

		SwingTools.label("Description:", uiPanel);
		txtEdits[1] = SwingTools.scrolledText(450, 200,
				selectedPlugin.getDescription(),
				changeListener, uiPanel);

		SwingTools.label("Link(s):", uiPanel);
		txtEdits[2] = SwingTools.scrolledText(450, 120,
				selectedPlugin.getLinks(),
				changeListener, uiPanel);

		textChanged = false;

		JPanel buttonPanel = SwingTools.horizontalPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		//Button to save description
		SwingTools.button("Save", "Save Description", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveText();
			}
		}, buttonPanel);
		buttonPanel.add(Box.createHorizontalGlue());

		//Button to cancel and return to Plugin Manager
		SwingTools.button("Close", "Exit Description Editor", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeFrame();
			}
		}, buttonPanel);
		uiPanel.add(buttonPanel);

		getContentPane().add(uiPanel);
	}

	private void closeFrame() {
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
		dispose();
	}

	private void saveText() {
		selectedPlugin.description = txtEdits[1].getText().trim();
		for (String link : txtEdits[2].getText().trim().split("\n"))
			selectedPlugin.addLink(link);
		for (String author : txtEdits[0].getText().trim().split("\n"))
			selectedPlugin.addAuthor(author);
		updaterFrame.pluginsChanged();
		textChanged = false;
	}
}
