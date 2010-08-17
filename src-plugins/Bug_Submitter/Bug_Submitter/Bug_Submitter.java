/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

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

import ij.gui.GUI;

import ij.plugin.PlugIn;
import ij.plugin.BrowserLauncher;

import ij.text.TextWindow;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ij.Prefs;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;

import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;

import fiji.updater.UptodateCheck;
import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginObject;
import fiji.updater.ui.ProgressDialog;
import fiji.updater.util.Canceled;

public class Bug_Submitter implements PlugIn {

	protected String bugzillaAssignee = "fiji-devel@googlegroups.com";

	public String e( String original ) {
		try {
			return URLEncoder.encode(original,"UTF-8");
		} catch( java.io.UnsupportedEncodingException e ) {
			// In practice this should never happen:
			IJ.error("UnsupportedEncodingException (!): "+e);
			return null;
		}
	}

	/* We warn the user that saving their password between
	   sessions is insecure, and only rot13 the password for
	   storing in the ImageJ preferences file so that someone
	   glancing over your shoulder while you're editing the file
	   is less likely to be able to read your password.  There's
	   no pretence here that this actually adds any meaningful
	   security. */

	public static String rot13(String s) {
		int n = s.length();
		char [] originalChars = s.toCharArray();
		char [] newChars = new char[n];
		for( int i = 0; i < n; ++i ) {
			char c = originalChars[i];
			if( (c >= 'a' && c <= 'm') || (c >= 'A' && c <= 'M') )
				c += 13;
			else if( (c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z') )
				c -= 13;
			newChars[i] = c;
		}
		return new String(newChars);
	}

	final String bugzillaBaseURI = "http://pacific.mpi-cbg.de/cgi-bin/bugzilla/";

	static final int SUCCESS = 1;
	static final int LOGIN_FAILURE = 2;
	static final int OTHER_FAILURE = 3;
	static final int CC_UNKNOWN_FAILURE = 4; // Not used any more...
	static final int IOEXCEPTION_FAILURE = 5;

	static class SubmissionResult {
		public SubmissionResult( int resultCode,
					 int bugNumber,
					 String bugURL,
					 String authenticationResultPage,
					 String submissionResultPage ) {
			this.resultCode = resultCode;
			this.bugNumber = bugNumber;
			this.bugURL = bugURL;
			this.authenticationResultPage = authenticationResultPage;
			this.submissionResultPage = submissionResultPage;
		}
		int resultCode;
		int bugNumber;
		String bugURL;
		String authenticationResultPage;
		String submissionResultPage;
	}

	public String getUsefulSystemInformation() {
		String [] interestingProperties = {
			"os.arch",
			"os.name",
			"os.version",
			"java.version",
			"java.vendor",
			"java.runtime.name",
			"java.runtime.version",
			"java.vm.name",
			"java.vm.version",
			"java.vm.vendor",
			"java.vm.info",
			"java.awt.graphicsenv",
			"java.specification.name",
			"java.specification.version",
			"sun.cpu.endian",
			"sun.desktop",
			"file.separator" };

		StringBuffer result = new StringBuffer();
		for( String k : interestingProperties ) {
			result.append("  ");
			result.append(k);
			result.append(" => ");
			String value = System.getProperty(k);
			result.append(value == null ? "null" : value);
			result.append("\n");
		}

		return result.toString();
	}

	protected String getInstalledVersions() {

		ProgressDialog progress = new ProgressDialog(IJ.getInstance(),"Finding installed plugin versions...");
		Checksummer checksummer = new Checksummer(progress);
		try {
				checksummer.updateFromLocal();
		} catch (Canceled e) {
			checksummer.done();
			IJ.error("Canceled");
			return null;
		}

		Map<String, PluginObject.Version> checksums =
			checksummer.getCachedChecksums();

		StringBuffer sb = new StringBuffer();

		for (Map.Entry<String, PluginObject.Version> entry : checksums.entrySet()) {
			    String file = entry.getKey();
			    PluginObject.Version version = entry.getValue();
			    sb.append("  ").append(version.checksum).append(" ");
			    sb.append(version.timestamp).append(" ");
			    sb.append(file).append("\n");
		}

		return sb.toString();
	}

	/** If the bug is submitted successfully, the URL for the bug
	    is returned in a String.  In the case of any error, null
	    is returned. */

	public SubmissionResult submitBug( String submitterEmail,
					   String submitterBugzillaPassword,
					   String bugSubject,
					   String bugText ) {

		String bugComponent = "Plugins";

		Pattern cookiePattern = Pattern.compile("^(\\w+)=(\\w+);.*$");

		Pattern linuxPattern = Pattern.compile("^Linux.*$",Pattern.CASE_INSENSITIVE);
		Pattern windowsPattern = Pattern.compile("^Windows.*$",Pattern.CASE_INSENSITIVE);
		Pattern macPattern = Pattern.compile("^Mac ?OS.*$",Pattern.CASE_INSENSITIVE);

		Pattern badAuthentication = Pattern.compile(
			"^.*username or password you entered is not valid.*$" );

		StringBuffer submissionReply = new StringBuffer();
		StringBuffer authenticationReply = new StringBuffer();

		try {
			URL url = new URL( bugzillaBaseURI + "enter_bug.cgi" );
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			PrintStream ps = new PrintStream(connection.getOutputStream());
			ps.println("Bugzilla_login="+e(submitterEmail)+
				   "&Bugzilla_password="+e(submitterBugzillaPassword)+
				   "&classification=__all"+
				   "&GoAheadAndLogIn="+e("Log in"));
			ps.close();

			// Get the cookies that were set:
			HashMap< String, String > cookies = new HashMap< String, String >();
			Map< String, List< String > > headers = connection.getHeaderFields();
			List< String > setCookiesFields = headers.get("Set-Cookie");
			if( setCookiesFields != null )
				for( String s : setCookiesFields ) {
					Matcher cookieMatcher = cookiePattern.matcher(s);
					if( cookieMatcher.matches() ) {
						String key = cookieMatcher.group(1);
						String value = cookieMatcher.group(2);
						cookies.put( key, value );
					}
				}

			if( cookies.size() == 0 ) {
				// That means the the authentication failed:
				return new SubmissionResult( LOGIN_FAILURE, -1, null,
							     authenticationReply.toString(),
							     submissionReply.toString() );
			}

			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader( new InputStreamReader(is) );
			String line = null;
			boolean authenticationFailed = false;
			while( (line = br.readLine()) != null ) {
				authenticationReply.append(line);
				if( badAuthentication.matcher(line).matches() ) {
					authenticationFailed = true;
				}
			}
			if( authenticationFailed ) {
				return new SubmissionResult( LOGIN_FAILURE, -1, null,
							     authenticationReply.toString(),
							     submissionReply.toString() );
			}

			String ccString = "";
			if( submitterEmail != null && submitterEmail.trim().length() > 0 )
				ccString = "&cc="+e(submitterEmail.trim());

			url = new URL( bugzillaBaseURI + "post_bug.cgi" );
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");

			boolean firstCookie = true;
			String cookieValueToSend = "";
			for( String cookieKey : cookies.keySet() ) {
				String value = cookies.get(cookieKey);
				if( firstCookie ) {
					firstCookie = false;
					cookieValueToSend += cookieKey + "=" + value;
				} else {
					cookieValueToSend += "; " + cookieKey + "=" + value;
				}
			}
			connection.setRequestProperty( "Cookie", cookieValueToSend );

			String osParameterValue = null;
			String platformParameterValue = null;

			String osName = System.getProperty("os.name");

			if( linuxPattern.matcher(osName).matches() ) {
				osParameterValue = "Linux";
				platformParameterValue = "PC";
			} else if( windowsPattern.matcher(osName).matches() ) {
				osParameterValue = "Windows";
				platformParameterValue = "PC";
			} else if( macPattern.matcher(osName).matches() ) {
				osParameterValue = "Mac OS";
				platformParameterValue = "Macintosh";
			} else {
				osParameterValue = "Other";
				platformParameterValue = "Other";
			}

			ps = new PrintStream(connection.getOutputStream());
			ps.println("product=Fiji"+
				   "&component="+e(bugComponent)+
				   "&rep_platform="+e(platformParameterValue)+
				   "&op_sys="+e(osParameterValue)+
				   "&priority=P4"+
				   "&bug_severity=normal"+
				   "&target_milestone="+e("---")+
				   "&version=unspecified"+
				   "&bug_file_loc="+e("http://")+
				   "&bug_status=NEW"+
				   "&assigned_to="+e(bugzillaAssignee)+
				   ccString+
				   "&short_desc="+e(bugSubject)+
				   "&comment="+e(bugText)+
				   "&commentprivacy=0"+
				   "&dependson="+
				   "&blocked="+
				   "&hidden=enter_bug");

			ps.close();

			Pattern successfullySubmitted = Pattern.compile(
				"^.*Bug\\s+(\\d+)\\s+Submitted.*$",
				Pattern.CASE_INSENSITIVE);

			Pattern ccEmailUnknown = Pattern.compile(
				"^.*did not match anything.*$" );

			int bugNumber = -1;
			boolean unknownCC = false;

			is = connection.getInputStream();
			br = new BufferedReader( new InputStreamReader(is) );
			line = null;
			while( (line = br.readLine()) != null ) {
				submissionReply.append(line);
				Matcher submittedMatcher = successfullySubmitted.matcher(line);
				if( submittedMatcher.matches() ) {
					bugNumber = Integer.parseInt( submittedMatcher.group(1), 10 );
				} else if( ccEmailUnknown.matcher(line).matches() ) {
					unknownCC = true;
				}
			}

			if( unknownCC ) {
				IJ.error("Your email address ("+submitterEmail+") didn't match" +
					 " a Bugzilla account.\nEither create an account for that" +
					 " email address, which is the\nrecommended option,"+
					 " or leave the email field blank.");
				return new SubmissionResult( CC_UNKNOWN_FAILURE, -1, null,
							     authenticationReply.toString(),
							     submissionReply.toString() );
			}

			if( bugNumber < 1 ) {
				return new SubmissionResult( OTHER_FAILURE, -1, null,
							     authenticationReply.toString(),
							     submissionReply.toString() );
			} else {
				return new SubmissionResult( SUCCESS,
							     bugNumber,
							     bugzillaBaseURI + "show_bug.cgi?id="+bugNumber,
							     authenticationReply.toString(),
							     submissionReply.toString() );
			}

		} catch( IOException e ) {
			System.out.println("Got an IOException: "+e);
			e.printStackTrace();
			return new SubmissionResult( IOEXCEPTION_FAILURE, -1, null,
						     authenticationReply.toString(),
						     submissionReply.toString() );
		}
	}

	@SuppressWarnings("serial")
	class NewBugDialog extends JFrame implements ActionListener, WindowListener {

		JButton bugzillaAccountCreation;
		JButton submitReport;
		JButton cancel;

		JTextField username;
		JPasswordField password;
		JCheckBox rememberPassword;

		JTextField summary;
		JTextArea description;
		UndoManager undo;

		private class HighlightingFocusListener implements FocusListener {
			String stringToHighlight;
			boolean notYetFocussed = true;
			JTextComponent textComponent;
			public HighlightingFocusListener( String stringToHighlight, JTextComponent textComponent ) {
				this.stringToHighlight = stringToHighlight;
				this.textComponent = textComponent;
			}
			public void focusGained(FocusEvent e) {
				if( notYetFocussed ) {
					String text = textComponent.getText();
					int startIndex = text.indexOf( stringToHighlight );
					if( startIndex >= 0 ) {
						textComponent.setSelectionStart(startIndex);
						textComponent.setSelectionEnd(startIndex+stringToHighlight.length());
					}
					notYetFocussed = false;
				}
			}
			public void focusLost(FocusEvent e) { }
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
			public void actionPerformed(ActionEvent evt) {
				try {
					if( undo.canUndo() )
						undo.undo();
				} catch (CannotUndoException e) { }
			}
		}

		protected class RedoAction extends AbstractAction {
			public RedoAction() {
				super("Redo");
			}
			public void actionPerformed(ActionEvent evt) {
				try {
					if( undo.canRedo() )
						undo.redo();
				} catch (CannotRedoException e) { }
			}
		}

		boolean askedToSubmit = false;
		boolean alreadyDisposed = false;

		@Override
		public void setVisible(boolean visible) {
			if (visible)
				WindowManager.addWindow(this);
			super.setVisible(visible);
		}

		@Override
		public synchronized void show() {
			WindowManager.addWindow(this);
			super.show();
			try {
				wait();
			} catch (InterruptedException e) { }
		}

		@Override
		public synchronized void dispose() {
			WindowManager.removeWindow(this);
			notify();
			super.dispose();
	        }


		public NewBugDialog( String suggestedUsername,
				     String suggestedPassword,
				     String suggestedSummary,
				     String suggestedDescription ) {

			super( "Bug Report Form" );

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
			password = new JPasswordField(20);
			password.setEchoChar('*');
			if( suggestedPassword != null )
				password.setText( suggestedPassword );
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
			summary.setText( suggestedSummary );
			summary.addFocusListener( new HighlightingFocusListener(
							      dummyBugTextSummary,
							      summary) );
			description = new JTextAreaTabFocus(16,42);
			description.setLineWrap(true);
			description.setWrapStyleWord(true);
			description.setText( suggestedDescription );
			undo = new UndoManager();
			description.getDocument().addUndoableEditListener(
				new SimpleEditListener() );
			ActionMap actionMap = description.getActionMap();
			actionMap.put("Undo",new UndoAction());
			actionMap.put("Redo",new RedoAction());
			InputMap inputMap = description.getInputMap();
			inputMap.put(KeyStroke.getKeyStroke("control Z"), "Undo");
			inputMap.put(KeyStroke.getKeyStroke("control Y"), "Redo");

			description.addFocusListener( new HighlightingFocusListener(
							      dummyBugTextDescription,
							      description) );

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

			JScrollPane scrollPane = new JScrollPane(description);

			++ c.gridy;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			contentPane.add( scrollPane, c );
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weighty = 0;

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
				contentPane.add( p, c );
			}

			// Call pack twice to workaround:
			//   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
			pack();

			addAccelerator(cancel, (JComponent)contentPane, this,
				KeyEvent.VK_W, Toolkit.getDefaultToolkit()
					.getMenuShortcutKeyMask());
		}

		public void resetForm() {
			askedToSubmit = false;
		}

		public void actionPerformed( ActionEvent e ) {
			Object source = e.getSource();
			if( source == bugzillaAccountCreation ) {
				new BrowserLauncher().run(bugzillaBaseURI+"createaccount.cgi");
			} else if( source == cancel ) {
				alreadyDisposed = true;
				dispose();
			} else if( source == submitReport ) {
				if( username.getText().trim().length() == 0 ) {
					IJ.error("You must supply a username");
					return;
				}
				if( password.getPassword().length == 0 ) {
					IJ.error("You must supply a password");
					return;
				}
				if( summary.getText().trim().length() == 0 ) {
					IJ.error("You must supply a summary of the bug");
					return;
				}
				if( description.getText().trim().length() == 0 ) {
					IJ.error("You must supply a description of the bug");
					return;
				}
				askedToSubmit = true;
				alreadyDisposed = true;
				dispose();
			}
		}

		public void windowClosing( WindowEvent e ) {
			dispose();
		}

		public void windowActivated( WindowEvent e ) { }
		public void windowDeactivated( WindowEvent e ) { }
		public void windowClosed( WindowEvent e ) { }
		public void windowOpened( WindowEvent e ) { }
		public void windowIconified( WindowEvent e ) { }
		public void windowDeiconified( WindowEvent e ) { }
	}

	String usernamePreferenceKey = "Bug_Submitter.username";
	String passwordPreferenceKey = "Bug_Submitter.password";

	final String dummyBugTextSummary = "[Your summary of the problem or bug.]";
	final String dummyBugTextDescription = "[Enter details of the problem or "+
			"bug and how to reproduce it here.]";

	public void run( String ignore ) {

		String summary = dummyBugTextSummary;
		String description = dummyBugTextDescription+"\n"+
			"\nInformation about your version of Java - "+
			"this information is useful for the Fiji developers:\n\n"+
			getUsefulSystemInformation()+
			"\nThe up-to-date check says: "+(new UptodateCheck()).check()+"\n"+
			"\nInformation about the version of each plugin:\n\n"+
			getInstalledVersions();

		while( true ) {

			String suggestedUsername = Prefs.get(usernamePreferenceKey,"");
			String suggestedPassword = Prefs.get(passwordPreferenceKey,null);
			if( suggestedPassword == null || suggestedPassword.length() == 0 )
				suggestedPassword = null;
			else
				suggestedPassword = rot13( suggestedPassword );

			NewBugDialog dialog = new NewBugDialog( suggestedUsername, suggestedPassword, summary, description );
			GUI.center(dialog);
			dialog.show();

			if( ! dialog.askedToSubmit )
				return;

			String username = dialog.username.getText().trim();
			Prefs.set( usernamePreferenceKey, username );
			Prefs.savePreferences();

			String password = new String(dialog.password.getPassword());
			if( dialog.rememberPassword.isSelected() )
				Prefs.set( passwordPreferenceKey, rot13(password) );
			else
				Prefs.set( passwordPreferenceKey, "" );
			Prefs.savePreferences();

			summary = dialog.summary.getText().trim();
			description = dialog.description.getText().trim();

			SubmissionResult result = submitBug( username, password, summary, description );

			switch( result.resultCode ) {
			case IOEXCEPTION_FAILURE:
				IJ.error("There was a network failure while submitting your bug.\n"+
					 "Please try again after checking you have internet connectivity.");
				break;
			case CC_UNKNOWN_FAILURE:
				IJ.error("The user in the Cc: field was unknown.");
				break;
			case LOGIN_FAILURE:
				IJ.error("The login failed: your username or password may be incorrect.");
				break;
			case OTHER_FAILURE:
				new TextWindow( "Login Reply Page", result.authenticationResultPage, 640, 480 );
				new TextWindow( "Submission Reply Page", result.submissionResultPage, 640, 480 );
				IJ.error("Sorry - there was an unknown error while submitting your bug.\n"+
					 "Please submit this as a bug manually, including the text from\nthe two windows which were just created.");
				break;
			case SUCCESS:
				new BrowserLauncher().run(result.bugURL);
				IJ.showMessage( "Bug Submitted", "Thank you!\n"+
						"Your bug report was successfully submitted.\n"+
						"(Your browser should now be launched with the bug report page.)" );
				return;
			default:
				IJ.error("[BUG] Unknown return code: "+result.resultCode);
				return;
			}
		}
	}

	/**
	 * Add a keyboard accelerator to a container.
	 *
	 * This method adds a keystroke to the input map of a container that
	 * sends an action event with the given source to the given listener.
	 */
	@SuppressWarnings("serial")
	public static void addAccelerator(final Component source,
			final JComponent container,
			final ActionListener listener, int key, int modifiers) {
                container.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(key, modifiers), source);
                if (container.getActionMap().get(source) != null)
                        return;
                container.getActionMap().put(source,
                                new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                                if (!source.isEnabled())
                                        return;
                                ActionEvent event = new ActionEvent(source,
                                        0, "Accelerator");
                                listener.actionPerformed(event);
                        }
                });
        }
}
