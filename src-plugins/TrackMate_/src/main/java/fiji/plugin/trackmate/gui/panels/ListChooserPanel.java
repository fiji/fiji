package fiji.plugin.trackmate.gui.panels;

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
import javax.swing.SpringLayout;
import java.awt.Dimension;


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

	public int getChoice() {
		return jComboBoxChoice.getSelectedIndex();
	}

	public void setChoice(int index) {
		jComboBoxChoice.setSelectedIndex(index);
	}


	/*
	 * PRIVATE METHODS
	 */

	private void initGUI() {
		try {
			this.setPreferredSize(new Dimension(392, 470));
			SpringLayout springLayout = new SpringLayout();
			setLayout(springLayout);
			{
				jLabelHeader = new JLabel();
				springLayout.putConstraint(SpringLayout.NORTH, jLabelHeader, 20, SpringLayout.NORTH, this);
				springLayout.putConstraint(SpringLayout.WEST, jLabelHeader, 20, SpringLayout.WEST, this);
				springLayout.putConstraint(SpringLayout.SOUTH, jLabelHeader, 36, SpringLayout.NORTH, this);
				springLayout.putConstraint(SpringLayout.EAST, jLabelHeader, 290, SpringLayout.WEST, this);
				this.add(jLabelHeader);
				jLabelHeader.setFont(BIG_FONT);
				jLabelHeader.setText("Select a "+typeName);
			}
			{
				String[] names = items.toArray(new String[] {});
				ComboBoxModel jComboBoxDisplayerChoiceModel = new DefaultComboBoxModel(names);
				jComboBoxChoice = new JComboBox();
				springLayout.putConstraint(SpringLayout.NORTH, jComboBoxChoice, 48, SpringLayout.NORTH, this);
				springLayout.putConstraint(SpringLayout.WEST, jComboBoxChoice, 10, SpringLayout.WEST, this);
				springLayout.putConstraint(SpringLayout.SOUTH, jComboBoxChoice, 75, SpringLayout.NORTH, this);
				springLayout.putConstraint(SpringLayout.EAST, jComboBoxChoice, -10, SpringLayout.EAST, this);
				jComboBoxChoice.setModel(jComboBoxDisplayerChoiceModel);
				this.add(jComboBoxChoice);
				jComboBoxChoice.setFont(FONT);
				jComboBoxChoice.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						echo(jComboBoxChoice.getSelectedIndex());
					}
				});
			}
			{
				jLabelHelpText = new JLabel();
				springLayout.putConstraint(SpringLayout.NORTH, jLabelHelpText, 81, SpringLayout.NORTH, this);
				springLayout.putConstraint(SpringLayout.SOUTH, jLabelHelpText, -24, SpringLayout.SOUTH, this);
				springLayout.putConstraint(SpringLayout.WEST, jLabelHelpText, 10, SpringLayout.WEST, this);
				springLayout.putConstraint(SpringLayout.EAST, jLabelHelpText, -10, SpringLayout.EAST, this);
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
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