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

public class RJ_Panel implements PlugIn, ActionListener, WindowListener {

	private Dialog dialog; Panel panel;

	private Button binomial, exponential, gamma;
	private Button gaussian, poisson, uniform;
	private Button about, options, website;

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!RJ.libcheck()) return;

		RJ.log(RJ.name()+" "+RJ.version()+": Panel");

		final Frame parent = (IJ.getInstance() != null) ? IJ.getInstance() : new Frame();
		dialog = new Dialog(parent,RJ.name(),false);
		dialog.setLayout(new FlowLayout());
		dialog.addWindowListener(this);

		panel = new Panel();
		panel.setLayout(new GridLayout(3,3,5,5));

		binomial = addButton("Binomial");
		exponential = addButton("Exponential");
		gamma = addButton("Gamma");

		gaussian = addButton("Gaussian");
		poisson = addButton("Poisson");
		uniform = addButton("Uniform");

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
		if (source == binomial) { IJ.doCommand("RandomJ Binomial"); }
		else if (source == exponential) { IJ.doCommand("RandomJ Exponential"); }
		else if (source == gamma) { IJ.doCommand("RandomJ Gamma"); }
		else if (source == gaussian) { IJ.doCommand("RandomJ Gaussian"); }
		else if (source == poisson) { IJ.doCommand("RandomJ Poisson"); }
		else if (source == uniform) { IJ.doCommand("RandomJ Uniform"); }
		else if (source == about) (new RJ_About()).run("");
		else if (source == options) IJ.doCommand("RandomJ Options");
		else if (source == website) (new RJ_Website()).run("");
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
