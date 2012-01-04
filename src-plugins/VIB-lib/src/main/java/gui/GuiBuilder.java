/*
 * Created on 29-May-2006
 */
package gui;


import ij.IJ;
import ij.io.DirectoryChooser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.ChangeListener;

public class GuiBuilder {
	public static JComponent createField(String label, String actionCommand,
			ActionListener controllor) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel(label));

		JTextField b = new JTextField();
		panel.add(b);

		b.addActionListener(controllor);

		return panel;
	}

	public static JSpinner addLabeledNumericSpinner(Container c, String label, int initial,
			int min, int max, ChangeListener controllor)
	{
		SpinnerModel model = new SpinnerNumberModel(
				initial, // initial value
				min, // min
				max, // max
				1); // step
		JSpinner spinner = addLabeledSpinner(c, label, model);

		if(controllor != null) spinner.addChangeListener(controllor);
		return spinner;
	}

	public static JSpinner addLabeledSpinner(Container c, String label,
			SpinnerModel model) {
		JLabel l = new JLabel(label);

		Box b = new Box(BoxLayout.X_AXIS);
		b.add(l);

		JSpinner spinner = new JSpinner(model);
		l.setLabelFor(spinner);
		b.add(spinner);

		c.add(b);
		return spinner;
	}

	public static void addCommand(Container c, String label, String actionCmd, ActionListener controllor) {
		JButton b = new JButton(label);
		b.setActionCommand(actionCmd);
		b.addActionListener(controllor);
		
		c.add(b);
	}

    public static void add2Command(Container c, String label, String actionCmd, String label2, String actionCmd2, ActionListener controllor) {
		JPanel p  = new JPanel(new GridLayout(1,2));

        JButton b = new JButton(label);
		b.setActionCommand(actionCmd);
		b.addActionListener(controllor);

        JButton b2 = new JButton(label2);
		b2.setActionCommand(actionCmd2);
		b2.addActionListener(controllor);

        p.add(b);
        p.add(b2);

		c.add(p);
	}

	public static void add3Command(Container c, String label, String actionCmd, String label2, String actionCmd2, String label3, String actionCmd3, ActionListener controllor) {
		JPanel p  = new JPanel(new GridLayout(1,2));

        JButton b = new JButton(label);
		b.setActionCommand(actionCmd);
		b.addActionListener(controllor);

        JButton b2 = new JButton(label2);
		b2.setActionCommand(actionCmd2);
		b2.addActionListener(controllor);

		JButton b3 = new JButton(label3);
		b3.setActionCommand(actionCmd3);
		b3.addActionListener(controllor);

        p.add(b);
        p.add(b2);
        p.add(b3);

		c.add(p);
	}

	public static JCheckBox addCheckBox(Container c, String label) {
		JCheckBox check = new JCheckBox();
		check.setSelected(false);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(label));
		box.add(Box.createGlue());
		box.add(check);

		c.add(box);
		return check;
	}


	public static JTextField addDirectoryField(Container container, String label) {
		final JTextField field = new JTextField();

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(label));
		box.add(field);

		field.setPreferredSize(new Dimension(10000, 25));
		field.setMaximumSize(new Dimension(10000, 25));


		JButton dialogButton = new JButton("...");

		dialogButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(IJ.getDirectory("current"));
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.showOpenDialog(null);
				File selected = chooser.getSelectedFile();
				if(selected!=null){
					field.setText(selected.getPath());
				}
			}
		});

		box.add(dialogButton);

		container.add(box);
		return field;
	}

	public static JTextField addFileField(Container container, String label) {
		final JTextField field = new JTextField();

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(label));
		box.add(field);

		field.setPreferredSize(new Dimension(10000, 25));
		field.setMaximumSize(new Dimension(10000, 25));


		JButton dialogButton = new JButton("...");

		dialogButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(IJ.getDirectory("current"));
				chooser.showOpenDialog(null);
				File selected = chooser.getSelectedFile();
				if(selected!=null){
					field.setText(selected.getPath());
				}
			}
		});

		box.add(dialogButton);

		container.add(box);
		return field;
	}

	public static JTextField addFileSaveField(Container container, String label) {
		final JTextField field = new JTextField();

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(label));
		box.add(field);

		field.setPreferredSize(new Dimension(10000, 25));
		field.setMaximumSize(new Dimension(10000, 25));


		JButton dialogButton = new JButton("...");

		dialogButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(IJ.getDirectory("current"));
				chooser.showSaveDialog(null);
				File selected = chooser.getSelectedFile();
				if(selected!=null){
					field.setText(selected.getPath());
				}
			}
		});

		box.add(dialogButton);

		container.add(box);
		return field;
	}
}
