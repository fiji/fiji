package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.action.ActionType;

public class ActionChooserPanel extends EnumChooserPanel<ActionType> {

	private static final long serialVersionUID = 1L;
	private static final Icon EXECUTE_ICON = null;
	public final ActionEvent ACTION_STARTED = new ActionEvent(this, 0, "ActionStarted");
	public final ActionEvent ACTION_FINISHED = new ActionEvent(this, 1, "ActionFinished");
	private TrackMateModelInterface model;

	public ActionChooserPanel(TrackMateModelInterface model) {
		super(ActionType.SET_RADIUS_TO_ESTIMATED, "Action");
		this.model = model;
		init();
	}
	
	private void init() {
		final JButton executeButton = new JButton("Execute", EXECUTE_ICON);
		executeButton.setBounds(12, 270, 80, 40);
		executeButton.setFont(FONT);
		executeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread("TrackMate action thread") {
					@Override
					public void run() {
						executeButton.setEnabled(false);
						fireAction(ACTION_STARTED);
						ActionType type = getChoice();
						type.execute(model);
						fireAction(ACTION_FINISHED);
						executeButton.setEnabled(true);
					}
				}.start();
			}
		});
	}

}
