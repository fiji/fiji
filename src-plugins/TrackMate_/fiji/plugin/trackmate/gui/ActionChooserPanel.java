package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.ActionFactory;
import fiji.plugin.trackmate.action.TrackMateAction;

public class ActionChooserPanel extends ChooserPanel<ActionFactory, TrackMateAction> {

	private static final long serialVersionUID = 1L;
	private static final Icon EXECUTE_ICON = new ImageIcon(TrackMateFrame.class.getResource("images/control_play_blue.png"));
	
	private static ActionFactory factory = new ActionFactory();
	
	public final ActionEvent ACTION_STARTED = new ActionEvent(this, 0, "ActionStarted");
	public final ActionEvent ACTION_FINISHED = new ActionEvent(this, 1, "ActionFinished");
	private TrackMateModelInterface model;
	private LogPanel logPanel;
	private Logger logger;

	public ActionChooserPanel(TrackMateModelInterface model) {
		super(factory, factory.radiusToEstimatedValue, "Action");
		this.model = model;
		this.logPanel = new LogPanel();
		this.logger = logPanel.getLogger();
		init();
	}
	
	private void init() {
		
		logPanel.setBounds(8, 240, 276, 200);
		add(logPanel);
		
		final JButton executeButton = new JButton("Execute", EXECUTE_ICON);
		executeButton.setBounds(6, 170, 100, 30);
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
	}
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new ActionChooserPanel(new TrackMate_()));
		frame.setSize(300, 520);
		frame.setVisible(true);
	}

}
