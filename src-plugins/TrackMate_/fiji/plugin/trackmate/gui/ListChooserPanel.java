package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import fiji.plugin.trackmate.InfoTextable;

/**
 * A panel to let the user choose what displayer he wants to use.
 */
public class ListChooserPanel <K extends InfoTextable> extends ActionListenablePanel {
	
	private static final long serialVersionUID = -1837635847479649545L;
	protected JLabel jLabelHeader;
	protected JComboBox jComboBoxChoice;
	protected List<K> list;
	protected JLabel jLabelHelpText;
	protected String typeName;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public ListChooserPanel(List<K> list, String typeName) {
		super();
		this.typeName = typeName;
		this.list = list;
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public K getChoice() {
		return list.get(jComboBoxChoice.getSelectedIndex());
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
				String[] names = new String[list.size()];
				for (int i = 0; i < list.size(); i++) 
					names[i] = list.get(i).toString();
				ComboBoxModel jComboBoxDisplayerChoiceModel = new DefaultComboBoxModel(names);
				jComboBoxChoice = new JComboBox();
				jComboBoxChoice.setModel(jComboBoxDisplayerChoiceModel);
				this.add(jComboBoxChoice);
				jComboBoxChoice.setFont(FONT);
				jComboBoxChoice.setBounds(12, 48, 270, 27);
				jComboBoxChoice.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						echo(list.get(jComboBoxChoice.getSelectedIndex()));
					}
				});
			}
			{
				jLabelHelpText = new JLabel();
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelHelpText.setBounds(12, 80, 270, 150);
				echo(list.get(jComboBoxChoice.getSelectedIndex()));
				this.add(jLabelHelpText);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void echo(K choice) {
		jLabelHelpText.setText(choice.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
	}
	
}