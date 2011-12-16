package ij3d.gui;

import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import customnode.CustomTriangleMesh;
import customnode.EdgeContraction;
import customnode.FullInfoMesh;

public class InteractiveMeshDecimation {
	public void run(final CustomTriangleMesh ctm) {
		@SuppressWarnings("unchecked")
		final FullInfoMesh fim = new FullInfoMesh(ctm.getMesh());
		final EdgeContraction ec = new EdgeContraction(fim, false);
		@SuppressWarnings("serial")
		final GenericDialog gd = new GenericDialog(
				"Mesh simplification") {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() != KeyEvent.VK_ENTER)
					super.keyPressed(e);
			}
		};
		gd.addNumericField("Contract next n edges", 100, 0);
		final TextField tf = (TextField)gd.getNumericFields().get(0);
		gd.addMessage(ec.getVertexCount() + " remaining vertices");
		final Label label = (Label)gd.getMessage();
		// gd.enableYesNoCancel("Simplify", "Save");
		gd.setModal(false);
		gd.showDialog();
		Button[] buttons = gd.getButtons();
		// yes button
		buttons[0].setLabel("Simplify");
		buttons[0].removeActionListener(gd);
		buttons[0].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final int n = Integer.parseInt(tf.getText());
				gd.setEnabled(false);
				new Thread() {
					@Override
					public void run() {
						int v = simplify(ec, n);
						gd.setEnabled(true);
						ctm.setMesh(fim.getMesh());
						label.setText(v + " remaining vertices");
					}
				}.start();
			}
		});
		// no button
		buttons[1].setLabel("Ok");
		buttons[1].removeActionListener(gd);
		buttons[1].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gd.dispose();
			}
		});
	}

	private int simplify(EdgeContraction ec, int n) {
		int part = n / 10;
		int last = n % 10;
		int ret = 0;
		for(int i = 0; i < 10; i++) {
			IJ.showProgress(i + 1, 10);
			ret = ec.removeNext(part);
		}
		if(last != 0)
			ret = ec.removeNext(last);
		IJ.showProgress(1);

		return ret;
	}
}

