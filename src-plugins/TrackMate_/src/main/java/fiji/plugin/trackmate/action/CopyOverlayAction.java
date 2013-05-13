package fiji.plugin.trackmate.action;

import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.ImagePlusChooser;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class CopyOverlayAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/page_copy.png"));
	public static final String NAME = "Copy overlay to...";
	public static final String INFO_TEXT = "<html>" +
			"This action copies the overlay (spots and tracks) to a new existing ImageJ window <br> " +
			"or to a new 3D viewer window. This can be useful to have the tracks and spots <br> " +
			"displayed on a modified image. <br> " +
			"</html>" ;
	
	public CopyOverlayAction() {
		icon = ICON;
	}	
	
	@Override
	public void execute(final TrackMate_ plugin) {
		final ImagePlusChooser impChooser = new ImagePlusChooser();
		impChooser.setLocationRelativeTo(null);
		impChooser.setVisible(true);
		final ActionListener copyOverlayListener = new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e == impChooser.OK_BUTTON_PUSHED) {
					new Thread("TrackMate copying thread") {
						public void run() {
							TrackMateModel model = plugin.getModel();
							// Instantiate displayer
							ImagePlus dest = impChooser.getSelectedImagePlus();
							impChooser.setVisible(false);
							TrackMateModelView newDisplayer;
							String title;
							if (null == dest) {
								logger.log("Copying data and overlay to new 3D viewer\n");
								newDisplayer = new SpotDisplayer3D(plugin.getModel());
								title = "3D viewer overlay";
							} else {
								logger.log("Copying overlay to "+dest.getShortTitle()+"\n");
								model.getSettings().imp = dest; // TODO TODO DANGER DANGER
								newDisplayer = new HyperStackDisplayer(plugin.getModel());
								title = dest.getShortTitle() + " ctrl";
							}
							newDisplayer.render();
							
							final DisplayerPanel newDisplayerPanel = new DisplayerPanel();
							newDisplayerPanel.setPlugin(plugin);
							newDisplayerPanel.register(newDisplayer);
							JFrame newFrame = new JFrame(); 
							newFrame.getContentPane().add(newDisplayerPanel);
							newFrame.pack();
							newFrame.setTitle(title);
							newFrame.setSize(300, 470);
							newFrame.setLocationRelativeTo(null);
							newFrame.setVisible(true);
							logger.log("Done.\n");
							
						}
					}.start();
				} else {
					impChooser.removeActionListener(this);
					impChooser.setVisible(false);
				}
			}
		};
		impChooser.addActionListener(copyOverlayListener);
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

}
