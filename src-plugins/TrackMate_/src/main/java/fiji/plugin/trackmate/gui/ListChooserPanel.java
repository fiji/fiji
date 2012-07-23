package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 * A panel to let the user choose what displayer he wants to use.
 */
public class ListChooserPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -1837635847479649545L;
	protected JLabel jLabelHeader;
	protected JComboBox jComboBoxChoice;
	protected List<String> items;
	protected List<String> infoTexts;
	protected JLabel jLabelHelpText;
	protected String typeName;

	/*
	 * CONSTRUCTOR
	 */

	public ListChooserPanel(List<String> items, List<String> infoTexts, String typeName) {
		super();
		this.infoTexts = infoTexts;
		this.typeName = typeName;
		this.items = items;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	public String getChoice() {
		return items.get(jComboBoxChoice.getSelectedIndex());
	}


	/*
	 * PRIVATE METHODS
	 */

	private void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 470));
			this.setLayout(null);
			{
				jLabelHeader = new JLabel();
				this.add(jLabelHeader);
				jLabelHeader.setFont(BIG_FONT);
				jLabelHeader.setText("Select a "+typeName);
				jLabelHeader.setBounds(20, 20, 270, 16);
			}
			{
				String[] names = new String[items.size()];
				for (int i = 0; i < items.size(); i++) 
					names[i] = items.get(i).toString();
				ComboBoxModel jComboBoxDisplayerChoiceModel = new DefaultComboBoxModel(names);
				jComboBoxChoice = new JComboBox();
				jComboBoxChoice.setModel(jComboBoxDisplayerChoiceModel);
				this.add(jComboBoxChoice);
				jComboBoxChoice.setFont(FONT);
				jComboBoxChoice.setBounds(12, 48, 270, 27);
				jComboBoxChoice.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						echo(jComboBoxChoice.getSelectedIndex());
					}
				});
			}
			{
				jLabelHelpText = new JLabel();
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelHelpText.setBounds(12, 80, 270, 366);
				echo(jComboBoxChoice.getSelectedIndex());
				this.add(jLabelHelpText);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void echo(int index) {
		jLabelHelpText.setText(infoTexts.get(index)
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">")
				);
	}

}