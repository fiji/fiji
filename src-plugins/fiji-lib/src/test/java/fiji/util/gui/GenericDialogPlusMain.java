package fiji.util.gui;

import ij.IJ;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** Manual test for {@link GenericDialogPlus}. */
public class GenericDialogPlusMain {

	public static void main(final String[] args) {
		final GenericDialogPlus gd =
			new GenericDialogPlus("GenericDialogPlus Test");
		gd.addFileField("A_file", System.getProperty("ij.dir") + "/jars/ij.jar");
		gd.addDirectoryField("A_directory", System.getProperty("ij.dir") +
			"/plugins");
		gd.addButton("Click me!", new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				IJ.showMessage("You clicked me!");
			}
		});
		final JLabel label = new JLabel("Hello, Ignacio! You're the BEST!");
		final JPanel jp = new JPanel();
		jp.add(label);
		gd.addComponent(jp);
		gd.addMessage("(blush)");
		gd.showDialog();
		if (!gd.wasCanceled()) IJ.showMessage("You chose the file " +
			gd.getNextString() + "\nand the directory " + gd.getNextString());
	}

}
