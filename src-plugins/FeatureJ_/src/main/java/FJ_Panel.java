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

public class FJ_Panel implements PlugIn, ActionListener, WindowListener {

	private Dialog dialog; Panel panel;

	private Button derivatives, edges, hessian;
	private Button laplacian, statistics, structure;
	private Button about, options, website;

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!FJ.libcheck()) return;

		FJ.log(FJ.name()+" "+FJ.version()+": Panel");

		final Frame parent = (IJ.getInstance() != null) ? IJ.getInstance() : new Frame();
		dialog = new Dialog(parent,FJ.name(),false);
		dialog.setLayout(new FlowLayout());
		dialog.addWindowListener(this);

		panel = new Panel();
		panel.setLayout(new GridLayout(3,3,5,5));

		derivatives = addButton("Derivatives");
		edges = addButton("Edges");
		hessian = addButton("Hessian");

		laplacian = addButton("Laplacian");
		statistics = addButton("Statistics");
		structure = addButton("Structure");

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
		if (source == derivatives) IJ.doCommand("FeatureJ Derivatives");
		else if (source == edges) IJ.doCommand("FeatureJ Edges");
		else if (source == hessian) IJ.doCommand("FeatureJ Hessian");
		else if (source == laplacian) IJ.doCommand("FeatureJ Laplacian");
		else if (source == statistics) IJ.doCommand("FeatureJ Statistics");
		else if (source == structure) IJ.doCommand("FeatureJ Structure");
		else if (source == about) (new FJ_About()).run("");
		else if (source == options) IJ.doCommand("FeatureJ Options");
		else if (source == website) (new FJ_Website()).run("");
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
