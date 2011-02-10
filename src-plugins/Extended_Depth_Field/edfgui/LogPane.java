//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package edfgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edf.LogSingleton;

public class LogPane extends JPanel implements ActionListener{

	private JButton 	bnClear = new JButton("Clear");
	private JTextArea log;
	JScrollPane jScrollPane;
	JScrollBar vbar;
	boolean autoScroll;

	/**
	* Constructor.
	*/
	public LogPane() {
		super();
		setLayout(new BorderLayout(5, 5));
		log = LogSingleton.getInstance().getJTextArea();
		log.setEditable(false);
		log.setBackground(Color.white);

		jScrollPane = new JScrollPane(log);

		JPanel pnButtons = new JPanel();
		pnButtons.add(bnClear);
		add("North", new JLabel(""));
		add("Center", jScrollPane);
		add("South", pnButtons);

		bnClear.addActionListener(this);
	}


	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.bnClear.setVisible(enabled);
		this.log.setVisible(enabled);
	}

	/**
	 *
	 */
	public Insets getInsets () {
		return(new Insets(5, 5, 5, 5));
	}

	/**
	* Implements the actionPerformed for the ActionListener.
	*/
	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnClear) {
			this.log.setText("");
		}
		notify();
	}

} // end of main
