package fiji.plugin.nperry;

import java.awt.event.MouseEvent;

import ij3d.Content;
import ij3d.Image3DMenubar;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;

public class ThresholdAdjuster extends InteractiveBehavior {
	
	private Content c;
	private int currCI;
	
	public ThresholdAdjuster(Image3DUniverse univ, Content c) {
		super(univ);
		this.c = c;
		//this.currCI = c.
	}
	
	public void doProcess(MouseEvent e) {
		if (e.getID() != MouseEvent.MOUSE_CLICKED) {
			super.doProcess(e);
			return;
		}
		
		// Check if the content 
		//Image3DMenubar menu = new Image3DMenubar();
	}
}
