package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.TrackMateAction;

public class ActionChooserPanel extends ListChooserPanel<TrackMateAction> {

	private static final long serialVersionUID = 1L;
	private static final Icon EXECUTE_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/control_play_blue.png"));
	
	public final ActionEvent ACTION_STARTED = new ActionEvent(this, 0, "ActionStarted");
	public final ActionEvent ACTION_FINISHED = new ActionEvent(this, 1, "ActionFinished");
	private TrackMateModel model;
	private LogPanel logPanel;
	private Logger logger;
	/** The view linked to the given model, in case some actions need it. */ 
	private TrackMateFrame view;
	private TrackMate_ plugin;

	public ActionChooserPanel(TrackMateModel model, TrackMateFrame view, TrackMate_ plugin) {
		super(plugin.getAvailableActions(), "action");
		this.model = model;
		this.view = view;
		this.plugin = plugin;
		this.logPanel = new LogPanel();
		this.logger = logPanel.getLogger();
		init();
	}
	
	private void init() {
		
		jLabelHelpText.setSize(270, 150);
		
		logPanel.setBounds(8, 260, 276, 200);
		add(logPanel);
		
		final JButton executeButton = new JButton("Execute", EXECUTE_ICON);
		executeButton.setBounds(6, 220, 100, 30);
		executeButton.setFont(FONT);
		executeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread("TrackMate action thread") {
					@Override
					public void run() {
						try {
							executeButton.setEnabled(false);
							fireAction(ACTION_STARTED);
							TrackMateAction action = getChoice();
							action.setLogger(logger);
							action.setView(view);
							action.setPlugin(plugin);
							action.execute(model);
							fireAction(ACTION_FINISHED);
						} finally {
							executeButton.setEnabled(true);
						}
					}
				}.start();
			}
		});
		add(executeButton);
		
		HashMap<String, ImageIcon> icons = new HashMap<String, ImageIcon>();
		String[] names = new String[list.size()];
		for (int i = 0; i < list.size(); i++) { 
			names[i] = list.get(i).toString();
			icons.put(names[i], list.get(i).getIcon());
		}
		IconListRenderer renderer = new IconListRenderer(icons);
		jComboBoxChoice.setRenderer(renderer);

	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new ActionChooserPanel(new TrackMateModel(), null, new TrackMate_()));
		frame.setSize(300, 520);
		frame.setVisible(true);
	}

	/*
	 * INNER CLASS
	 */

	private class IconListRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private HashMap<String, ImageIcon> icons = null;

		public IconListRenderer(HashMap<String, ImageIcon> icons) {
			this.icons = icons;
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			ImageIcon icon = icons.get(value);
			label.setIcon(icon);
			return label;
		}
	}
}
