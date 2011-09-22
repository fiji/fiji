package fiji.scripting;

import fiji.build.Fake;
import fiji.build.Parser;
import fiji.build.Rule;

import fiji.scripting.JTextAreaOutputStream;

import ij.IJ;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class RunFijiBuild implements PlugIn {
	public void run(String arg) {
		String fijiDir = System.getProperty("fiji.dir");
		if (!fijiDir.endsWith("/"))
			fijiDir += "/";
		if (arg == null || "".equals(arg)) {
			OpenDialog dialog = new OpenDialog("Which Fiji component",
				fijiDir + "plugins", "");
			if (dialog.getDirectory() == null)
				return;
			arg = dialog.getDirectory() + dialog.getFileName();
		}
		if (arg.startsWith(fijiDir))
			arg = arg.substring(fijiDir.length());

		final JFrame frame = new JFrame("Building " + arg + "...");
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		JTextArea textArea = new JTextArea("Calling Fiji Build\n", 25, 80);
		textArea.setFont(new Font("Courier", Font.PLAIN, 12));
		textArea.setEditable(false);
		frame.getContentPane().add(textArea);
		final JButton okay = new JButton("okay");
		okay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}
		});
		okay.setEnabled(false);
		frame.getContentPane().add(okay);
		frame.pack();
		frame.setLocationRelativeTo(null);
		okay.requestFocus();
		frame.setVisible(true);

		try {
			Fake fake = new Fake();
			fake.out = fake.err = new PrintStream(new JTextAreaOutputStream(textArea));
			Parser parser = fake.parse(new FileInputStream(fijiDir + "/Fakefile"), new File(fijiDir));
		        final Rule all = parser.parseRules(Arrays.asList(arg.split("\\s+")));
			all.make();
			fake.out.println("Finished.");
			frame.setTitle("Built " + arg);
			okay.setEnabled(true);
	        } catch (Exception e) {
			IJ.handleException(e);
	        }
	}
}