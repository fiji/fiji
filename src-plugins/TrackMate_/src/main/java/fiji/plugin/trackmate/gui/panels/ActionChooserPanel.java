package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.providers.ActionProvider;

public class ActionChooserPanel {

	private static final Icon EXECUTE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/control_play_blue.png"));
	
	public final ActionEvent ACTION_STARTED = new ActionEvent(this, 0, "ActionStarted");
	public final ActionEvent ACTION_FINISHED = new ActionEvent(this, 1, "ActionFinished");
	private LogPanel logPanel;
	private Logger logger;
	private TrackMateWizard wizard;
	private TrackMate trackmate;
	private List<ImageIcon> icons;
	private final ListChooserPanel panel;
	private final ActionProvider actionProvider;

	/*
	 * CONSTRUCTORS
	 */
	
	public ActionChooserPanel(final ActionProvider actionProvider, final TrackMate trackmate) {
		
		List<String> actions = actionProvider.getAvailableActions();
		List<String> infoTexts = new ArrayList<String>(actions.size());
		List<ImageIcon> icons = new ArrayList<ImageIcon>(actions.size());
		for(String key : actions) {
			infoTexts.add( actionProvider.getInfoText(key) );
			icons.add( actionProvider.getIcon(key) );
		}
		
		this.panel = new ListChooserPanel(actions, infoTexts, "action");
		this.logPanel = new LogPanel();
		this.logger = logPanel.getLogger();
		this.actionProvider = actionProvider;
		init();
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public ListChooserPanel getPanel() {
		return panel;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void init() {
		
		logPanel.setBounds(8, 260, 276, 200);
		panel.add(logPanel);
		
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
							panel.fireAction(ACTION_STARTED);
							String actionName = panel.getChoice();
							TrackMateAction action = actionProvider.getAction(actionName);
							action.setLogger(logger);
							action.setWizard(wizard);
							action.execute(trackmate);
							panel.fireAction(ACTION_FINISHED);
						} finally {
							executeButton.setEnabled(true);
						}
					}
				}.start();
			}
		});
		panel.add(executeButton);
		
		HashMap<String, ImageIcon> iconMap = new HashMap<String, ImageIcon>();
		for (int i = 0; i < icons.size(); i++) { 
			iconMap.put(panel.items.get(i), icons.get(i));
		}
		IconListRenderer renderer = new IconListRenderer(iconMap);
		panel.jComboBoxChoice.setRenderer(renderer);
		panel.jLabelHelpText.setSize(270, 150);

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
