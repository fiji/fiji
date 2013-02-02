/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006 - 2013 Mark Longair */

/*
  This file is part of the ImageJ plugin "Bug_Submitter".

  The ImageJ plugin "Bug_Submitter" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Bug_Submitter" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package Bug_Submitter;

import ij.IJ;
import ij.WindowManager;
import ij.plugin.BrowserLauncher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

@SuppressWarnings("serial")
class NewBugDialog extends JFrame implements ActionListener, WindowListener {

	private final Bug_Submitter bugSubmitter;

	JButton bugzillaAccountCreation;
	JButton submitReport;
	JButton cancel;

	JTextField username;
	JPasswordField password;
	JCheckBox rememberPassword;

	JTextField summary;
	JTextArea description;
	JTextArea systemInfo;
	UndoManager undo;

	/**
	 * Helper class for resetting a component's background color after a
	 * validation error.
	 */
	private class FocusErrorReset extends FocusAdapter {

		private Color background;

		private FocusErrorReset(final Component c) {
			background = c.getBackground();
		}

		@Override
		public void focusGained(FocusEvent e) {
			e.getComponent().setBackground(background);
		}

	}

	private class JTextAreaTabFocus extends JTextArea {
		public JTextAreaTabFocus( int rows, int columns ) {
			super( rows, columns );
		}
		@Override
		protected void processComponentKeyEvent( KeyEvent e ) {
			if( e.getID() == KeyEvent.KEY_PRESSED &&
			    e.getKeyCode() == KeyEvent.VK_TAB ) {
				e.consume();
				if (e.isShiftDown())
					transferFocusBackward();
				else
					transferFocus();
			} else {
				super.processComponentKeyEvent( e );
			}
		}
	}

	// This example is derived from:
	//   http://java.sun.com/docs/books/tutorial/uiswing/components/generaltext.html#undo
	protected class SimpleEditListener implements UndoableEditListener {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
		}
	}

	// More "Programming with Google" from:
	//   http://www.exampledepot.com/egs/javax.swing.undo/UndoText.html

	protected class UndoAction extends AbstractAction {
		public UndoAction() {
			super("Undo");
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				if( undo.canUndo() )
					undo.undo();
			} catch (CannotUndoException e) {
				// do nothing
			}
		}
	}

	protected class RedoAction extends AbstractAction {
		public RedoAction() {
			super("Redo");
		}
		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				if( undo.canRedo() )
					undo.redo();
			} catch (CannotRedoException e) {
				// do nothing
			}
		}
	}

	boolean askedToSubmit = false;
	boolean alreadyDisposed = false;

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			WindowManager.addWindow(this);
		else
			WindowManager.removeWindow(this);
		super.setVisible(visible);
	}

	@SuppressWarnings("deprecation")
	@Override
	public synchronized void show() {
		WindowManager.addWindow(this);
		super.show();
		try {
			wait();
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	@Override
	public synchronized void dispose() {
		WindowManager.removeWindow(this);
		notify();
		super.dispose();
        }


	public NewBugDialog(Bug_Submitter bugSubmitter, String suggestedUsername,
			     String suggestedPassword,
			     String systemInfoText)
	{

		super( "Bug Report Form" );
		this.bugSubmitter = bugSubmitter;

		Container contentPane = getContentPane();

		addWindowListener( this );

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets( 5, 3, 5, 3 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;

		{
			JPanel labelsPanel = new JPanel();
			labelsPanel.setLayout( new GridBagLayout() );

			JTextArea instructions = new JTextArea(3,30);
			instructions.setEditable(false);
			instructions.setCursor(null);
			instructions.setOpaque(false);
			instructions.setFocusable(false);
			instructions.setLineWrap(true);
			instructions.setWrapStyleWord(true);

			instructions.setText(
				"In order to report a bug, we ask that you create a Bugzilla account. "+
				" This is so that you can follow the progress of fixing the problem by"+
				" email and enables us to ask follow-up questions if that's necessary.");

			bugzillaAccountCreation = new JButton( "Visit the Bugzilla account creation page" );
			bugzillaAccountCreation.addActionListener(this);

			GridBagConstraints clabels = new GridBagConstraints();
			clabels.fill = GridBagConstraints.BOTH;
			clabels.gridx = 0;
			clabels.gridy = 0;
			labelsPanel.add( instructions, clabels );
			clabels.gridx = 1;
			clabels.gridy = 0;
			labelsPanel.add( bugzillaAccountCreation, clabels );

			contentPane.add( labelsPanel, c );
		}

		username = new JTextField(20);
		username.setText( suggestedUsername );
		username.addFocusListener(new FocusErrorReset(username));
		password = new JPasswordField(20);
		password.setEchoChar('*');
		if( suggestedPassword != null )
			password.setText( suggestedPassword );
		password.addFocusListener(new FocusErrorReset(password));
		rememberPassword = new JCheckBox("",suggestedPassword != null);

		{
			JPanel p = new JPanel();
			p.setLayout( new GridBagLayout() );

			GridBagConstraints cl = new GridBagConstraints();

			cl.gridx = 0; cl.gridy = 0; cl.anchor = GridBagConstraints.LINE_END;
			p.add( new JLabel("Bugzilla username (your email address): "), cl );
			cl.gridx = 1; cl.gridy = 0; cl.anchor = GridBagConstraints.LINE_START;
			p.add( username, cl );

			cl.gridx = 0; cl.gridy = 1; cl.anchor = GridBagConstraints.LINE_END;
			p.add( new JLabel("Bugzilla password: "), cl );
			cl.gridx = 1; cl.gridy = 1; cl.anchor = GridBagConstraints.LINE_START;
			p.add( password, cl );

			cl.gridx = 0; cl.gridy = 2; cl.anchor = GridBagConstraints.LINE_END;
			p.add( new JLabel("Remember password (insecure): "), cl );
			cl.gridx = 1; cl.gridy = 2; cl.anchor = GridBagConstraints.LINE_START;
			p.add( rememberPassword, cl );

			c.anchor = GridBagConstraints.LINE_START;
			++ c.gridy;
			contentPane.add( p, c );
		}

		summary = new JTextField(30);
		summary.addFocusListener(new FocusErrorReset(summary));

		description = new JTextAreaTabFocus(8, 42);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.addFocusListener(new FocusErrorReset(description));
		undo = new UndoManager();
		description.getDocument().addUndoableEditListener(
new SimpleEditListener());
		ActionMap actionMap = description.getActionMap();
		actionMap.put("Undo", new UndoAction());
		actionMap.put("Redo", new RedoAction());
		InputMap inputMap = description.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke("control Z"), "Undo");
		inputMap.put(KeyStroke.getKeyStroke("control Y"), "Redo");

		systemInfo = new JTextArea(6, 42);
		systemInfo.setLineWrap(true);
		systemInfo.setWrapStyleWord(true);
		systemInfo.setText( systemInfoText );
		systemInfo.setEditable(false);

		{
			JPanel summaryPanel = new JPanel();
			summaryPanel.setLayout( new BorderLayout() );
			summaryPanel.add( new JLabel("Summary of the bug: "), BorderLayout.WEST );
			summaryPanel.add( summary, BorderLayout.CENTER );
			++ c.gridy;
			c.anchor = GridBagConstraints.LINE_START;
			contentPane.add( summaryPanel, c );
		}

		++ c.gridy;
		contentPane.add( new JLabel("A full description of the bug:"), c );

		++ c.gridy;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		contentPane.add( new JScrollPane(description), c );

		++ c.gridy;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0;
		contentPane.add( new JLabel("Useful information about your system "+
			"(will be sent as part of your bug report):"), c );

		++ c.gridy;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0.5;
		contentPane.add( new JScrollPane(systemInfo), c );

		{
			JPanel p = new JPanel();

			submitReport = new JButton( "Submit Bug Report" );
			submitReport.addActionListener( this );
			p.add( submitReport );

			cancel = new JButton( "Cancel" );
			cancel.addActionListener( this );
			p.add( cancel );

			c.anchor = GridBagConstraints.CENTER;
			++ c.gridy;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weighty = 0;
			contentPane.add( p, c );
		}

		// Call pack twice to workaround:
		//   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
		pack();

		Bug_Submitter.addAccelerator(cancel, (JComponent)contentPane, this,
			KeyEvent.VK_W, Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask());
	}

	public void resetForm() {
		askedToSubmit = false;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
		if( source == bugzillaAccountCreation ) {
			new BrowserLauncher().run(bugSubmitter.bugzillaBaseURI+"createaccount.cgi");
		} else if( source == cancel ) {
			alreadyDisposed = true;
			dispose();
		} else if( source == submitReport ) {
			boolean success = validateForm();
			if (!success) return;
			askedToSubmit = true;
			alreadyDisposed = true;
			dispose();
		}
	}

	@Override
	public void windowClosing( WindowEvent e ) {
		dispose();
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// no action needed
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// no action needed
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// no action needed
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// no action needed
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// no action needed
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// no action needed
	}

	private boolean validateForm() {
		boolean error = false;
		final Color errorColor = new Color(255, 128, 128);
		if (username.getText().trim().length() == 0) {
			username.setBackground(errorColor);
			error = true;
		}
		if (password.getPassword().length == 0) {
			password.setBackground(errorColor);
			error = true;
		}
		if (summary.getText().trim().length() == 0) {
			summary.setBackground(errorColor);
			error = true;
		}
		if (description.getText().trim().length() == 0) {
			description.setBackground(errorColor);
			error = true;
		}
		if (error) {
			IJ.error("Please fill in all the fields.");
			return false;
		}
		return true;
	}

}
