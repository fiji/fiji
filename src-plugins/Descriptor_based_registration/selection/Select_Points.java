package selection;

import fiji.tool.AbstractTrackingTool;
import fiji.tool.ToolToggleListener;
import fiji.tool.ToolWithOptions;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * This is a template for a generic tool using Fiji's AbstractTool infrastructure.
 */

public class Select_Points extends AbstractTrackingTool implements ToolToggleListener, ToolWithOptions 
{
	{
		// for debugging, all custom tools can be removed to make space for this one if necessary
		clearToolsIfNecessary = true;
	}

	/**
	 * which windows to select in
	 */
	final ImagePlus imp1, imp2;
	
	public Select_Points( final ImagePlus imp1, final ImagePlus imp2 )
	{
		this.imp1 = imp1;
		this.imp2 = imp2;

		initDisplay();
		
		IJ.log( "Manual selection of landmarks for: " + imp1.getTitle() + " vs. " + imp2.getTitle() );
	}
	

	public void initDisplay()
	{
		final Frame frame = new Frame( "Selected Landmarks" );
		frame.setSize( 800, 600 );        
		
		/* Instantiation */		
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
		/* Elements */
		final String title1 = imp1.getTitle().substring( 0 , Math.min( 30,  imp1.getTitle().length() ) );
		final String title2 = imp2.getTitle().substring( 0 , Math.min( 30,  imp2.getTitle().length() ) );
		
	    final Button button = new Button( "Done" );

	    final Label text1 = new Label( title1, Label.CENTER );
	    final Label text2 = new Label( title2, Label.CENTER );
		
	    /* Location */
	    frame.setLayout( layout );

	    c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
	    frame.add ( text1, c );
	    
	    c.gridx++;
	    frame.add ( text2, c );
	    c.gridx--;
	    
	    ++c.gridy;
	    //c.insets = new Insets(10,150,0,150);
	    frame.add( button, c );

	    /* Configuration */
	    button.addActionListener( new DoneButtonListener( frame ) );

		frame.setVisible( true );
	}
	
	@Override
	public Roi optimizeRoi(Roi roi, ImageProcessor ip) 
	{
		Roi result = new ShapeRoi(roi);
		Roi[] rois = ((ShapeRoi)result).getRois();
		if (rois.length == 1)
			result = rois[0];
		result.setImage(WindowManager.getCurrentImage());
		result.nudge(KeyEvent.VK_RIGHT);
		return result;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		IJ.log("mouse clicked: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		IJ.log("mouse pressed: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		IJ.log("mouse released: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		IJ.log("mouse entered: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
		IJ.log("mouse exited: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		IJ.log("mouse moved: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		IJ.log("mouse dragged: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void sliceChanged(ImagePlus image) {
		super.sliceChanged(image);
		IJ.log("slice changed to " + image.getCurrentSlice() + " in " + image.getTitle());
	}

	@Override
	public void showOptionDialog() {
		GenericDialogPlus gd = new GenericDialogPlus(getToolName() + " Options");
		gd.addMessage("Here could be your option dialog!");
		addIOButtons(gd);
		gd.showDialog();
	}

	@Override
	public void toolToggled(boolean enabled) {
		IJ.log(getToolName() + " was switched " + (enabled ? "on" : "off"));
	}
}
