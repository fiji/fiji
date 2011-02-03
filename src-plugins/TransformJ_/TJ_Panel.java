import ij.IJ;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class TJ_Panel implements PlugIn, ActionListener, WindowListener {
	
	private Dialog dialog; Panel panel;
	
	private Button affine, crop, embed;
	private Button matrix, mirror, rotate;
	private Button scale, trans, turn;
	private Button about, options, website;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Panel");
		
		final Frame parent = (IJ.getInstance() != null) ? IJ.getInstance() : new Frame();
		dialog = new Dialog(parent,TJ.name(),false);
		dialog.setLayout(new FlowLayout());
		dialog.addWindowListener(this);
		
		panel = new Panel();
		panel.setLayout(new GridLayout(4,3,5,5));
		
		affine = addButton("Affine");
		crop = addButton("Crop");
		embed = addButton("Embed");
		
		matrix = addButton("Matrix");
		mirror = addButton("Mirror");
		rotate = addButton("Rotate");
		
		scale = addButton("Scale");
		trans = addButton("Translate");
		turn = addButton("Turn");
		
		about = addButton("About");
		options = addButton("Options");
		website = addButton("Website");
		
		dialog.add(panel);
		dialog.pack();
		if (pos.x < 0 || pos.y < 0) GUI.center(dialog);
		else dialog.setLocation(pos);
		dialog.setVisible(true);
	}
	
	private Button addButton(String label) {
		
		final Button b = new Button("   "+label+"   ");
		b.addActionListener(this);
		panel.add(b);
		return b;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		final Object source = e.getSource();
		if (source == affine) IJ.doCommand("TransformJ Affine");
		else if (source == crop) IJ.doCommand("TransformJ Crop");
		else if (source == embed) IJ.doCommand("TransformJ Embed");
		else if (source == matrix) (new TJ_Matrix()).run("");
		else if (source == mirror) IJ.doCommand("TransformJ Mirror");
		else if (source == rotate) IJ.doCommand("TransformJ Rotate");
		else if (source == scale) IJ.doCommand("TransformJ Scale");
		else if (source == trans) IJ.doCommand("TransformJ Translate");
		else if (source == turn) IJ.doCommand("TransformJ Turn");
		else if (source == about) (new TJ_About()).run("");
		else if (source == options) IJ.doCommand("TransformJ Options");
		else if (source == website) (new TJ_Website()).run("");
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) {
		
		dialog.setVisible(false);
		dialog.dispose();
	}
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}
