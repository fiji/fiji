package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Listable;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerFactory;

/**
 * A panel to let the user choose what displayer he wants to use.
 */
public class ChooserPanel <L extends Listable<K>, K extends InfoTextable> extends ActionListenablePanel {
	
	private static final long serialVersionUID = -2349025481368788479L;
	private JLabel jLabelHeader;
	private JComboBox jComboBoxChoice;
	private List<K> types;
	private JLabel jLabelHelpText;
	private String typeName;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTOR
	 */
	
	public ChooserPanel(L factory, K defaultChoice, String typeName) {
		super();
		this.typeName = typeName;
		this.types = factory.getList();
		initGUI(defaultChoice);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public K getChoice() {
		return types.get(jComboBoxChoice.getSelectedIndex());
	}
	

	/*
	 * PRIVATE METHODS
	 */

	private void initGUI(K defaultChoice) {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 470));
			this.setLayout(null);
			{
				jLabelHeader = new JLabel();
				this.add(jLabelHeader);
				jLabelHeader.setFont(FONT.deriveFont(Font.BOLD));
				jLabelHeader.setText("Select a "+typeName);
				jLabelHeader.setBounds(6, 20, 270, 16);
				jLabelHeader.setHorizontalAlignment(SwingConstants.CENTER);
			}
			{
				String[] names = new String[types.size()];
				for (int i = 0; i < types.size(); i++) 
					names[i] = types.get(i).toString();
				ComboBoxModel jComboBoxDisplayerChoiceModel = new DefaultComboBoxModel(names);
				jComboBoxChoice = new JComboBox();
				jComboBoxChoice.setModel(jComboBoxDisplayerChoiceModel);
				jComboBoxChoice.setSelectedIndex(types.indexOf(defaultChoice));
				this.add(jComboBoxChoice);
				jComboBoxChoice.setFont(FONT);
				jComboBoxChoice.setBounds(12, 48, 270, 27);
				jComboBoxChoice.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						echo(types.get(jComboBoxChoice.getSelectedIndex()));
					}
				});
			}
			{
				jLabelHelpText = new JLabel();
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelHelpText.setBounds(12, 40, 270, 150);
				echo(defaultChoice);
				this.add(jLabelHelpText);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void echo(K choice) {
		jLabelHelpText.setText(choice.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
	}
	

	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		{
			TrackerFactory factory = new TrackerFactory();
			SpotTracker defaultChoice = factory.getList().get(0);
			ChooserPanel<TrackerFactory, SpotTracker> instance = new ChooserPanel<TrackerFactory, SpotTracker>(factory, defaultChoice, "Tracker" );
			frame.getContentPane().add(instance);
			instance.setPreferredSize(new java.awt.Dimension(300, 469));
		}
	}

}
