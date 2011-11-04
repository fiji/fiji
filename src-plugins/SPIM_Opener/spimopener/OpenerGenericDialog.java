package spimopener;

import fiji.util.gui.GenericDialogPlus;

import java.util.List;
import java.util.ArrayList;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class OpenerGenericDialog extends GenericDialogPlus {

	private ActionListener listener;
	private List<DoubleSlider> doubleSliders = new ArrayList<DoubleSlider>();
	private int cIdx = 0;
	private Button okButton, cancelButton;

	public OpenerGenericDialog(String title) {
		super(title);
	}

	public OpenerGenericDialog(String title, Frame parent) {
		super(title, parent);
	}

	public void setActionListener(ActionListener l) {
		this.listener = l;
	}

	public void showDialog() {
		super.showDialog();
		Button[] buttons = getButtons();
		okButton = buttons[0];
		cancelButton = buttons[1];
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == okButton && listener != null)
			listener.actionPerformed(e);
		else
			super.actionPerformed(e);
	}

	public DoubleSlider getNextDoubleSlider() {
		return doubleSliders.get(cIdx++);
	}

	public List<DoubleSlider> getDoubleSliders() {
		return doubleSliders;
	}

	public void addChoice(String label, String[] choice) {
		super.addChoice(label, choice, choice[0]);
	}

	public void addDoubleSlider(String label, int min, int max) {
		addDoubleSlider(label, min, max, min, max);
	}

	public void addDoubleSlider(String label, int min, int max, int cmin, int cmax) {
		DoubleSlider slider = new DoubleSlider(min, max, cmin, cmax);
		doubleSliders.add(slider);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		Label theLabel = new Label(label);
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		layout.setConstraints(theLabel, c);
		add(theLabel);

		c.gridx++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		layout.setConstraints(slider, c);
		add(slider);
	}

	public static void main(String[] args) {
		OpenerGenericDialog gd = new OpenerGenericDialog("GenericDialogOpener Test");
		gd.addDoubleSlider("range", 1, 4, 1, 4);
		gd.addNumericField("lkjl", 0, 3);
		gd.showDialog();
	}
}
