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
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import ij.plugin.BrowserLauncher;

import java.net.URL;
import java.net.URLConnection;
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

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JPanel;

import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

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

			// Now we make the actual request.
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

			StringBuffer completeBugText = new StringBuffer();
			completeBugText.append("Useful Java System Properties:\n");
			for( String k : interestingProperties ) {
				completeBugText.append("  ");
				completeBugText.append(k);
				completeBugText.append(" => ");
				String value = System.getProperty(k);
				completeBugText.append(value == null ? "null" : value);
				completeBugText.append("\n");
			}
			completeBugText.append("\nBug Report Text:\n\n");
			completeBugText.append(bugText);

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
				   "&comment="+e(completeBugText.toString())+
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

	class NewBugDialog extends JDialog implements ActionListener, WindowListener {

		JButton bugzillaAccountCreation;
		JButton submitReport;
		JButton cancel;

		JTextField username;
		JPasswordField password;

		JTextField summary;
		JTextArea description;

		boolean askedToSubmit = false;
		boolean alreadyDisposed = false;

		public NewBugDialog( String suggestedUsername,
				     String suggestedSummary,
				     String suggestedDescription ) {

			super( IJ.getInstance(),
			       "Bug Report Form",
			       true );

			addWindowListener( this );

			setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();

			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets( 3, 3, 3, 3 );
			c.anchor = GridBagConstraints.CENTER;

			{
				JPanel labelsPanel = new JPanel();
				labelsPanel.setLayout( new GridBagLayout() );
				GridBagConstraints clabels = new GridBagConstraints();
				clabels.gridx = 0;
				clabels.gridy = 0;
				clabels.anchor = GridBagConstraints.LINE_START;
				labelsPanel.add( new JLabel( "In order to report a bug, we ask that you create a Bugzilla account." ), clabels );
				++ clabels.gridy;
				labelsPanel.add( new JLabel( "This is so that you can follow the progress of fixing the problem by" ), clabels );
				++ clabels.gridy;
				clabels.insets = new Insets( 0, 0, 4, 0 );
				labelsPanel.add( new JLabel( "email and enables us to ask follow-up questions if that's necessary." ), clabels );

				clabels.gridx = 1;
				clabels.gridy = 0;
				clabels.gridheight = 3;
				clabels.fill = GridBagConstraints.BOTH;
				bugzillaAccountCreation = new JButton( "Visit the Bugzilla account creation page" );
				bugzillaAccountCreation.addActionListener(this);
				labelsPanel.add( bugzillaAccountCreation, clabels );

				add( labelsPanel, c );
				++ c.gridy;
			}

			username = new JTextField(20);
			username.setText( suggestedUsername );
			password = new JPasswordField(20);
			password.setEchoChar('*');

			{
				JPanel p = new JPanel();
				p.setLayout( new GridBagLayout() );

				GridBagConstraints cl = new GridBagConstraints();

				cl.gridx = 0; cl.gridy = 0; cl.anchor = GridBagConstraints.LINE_END;
				p.add( new JLabel("Bugzilla username (your email address):"), cl );
				cl.gridx = 1; cl.gridy = 0; cl.anchor = GridBagConstraints.LINE_START;
				p.add( username, cl );

				cl.gridx = 0; cl.gridy = 1; cl.anchor = GridBagConstraints.LINE_END;
				p.add( new JLabel("Bugzilla password:"), cl );
				cl.gridx = 1; cl.gridy = 1; cl.anchor = GridBagConstraints.LINE_START;
				p.add( password, cl );

				c.anchor = GridBagConstraints.LINE_START;
				add( p, c );
				++ c.gridy;
			}

			summary = new JTextField(40);
			summary.setText( suggestedSummary );
			description = new JTextArea(16,76);
			description.setText( suggestedDescription );

			c.gridx = 0;
			c.gridwidth = 1;
			add( new JLabel("Summary of the bug:"), c );

			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			add( summary, c );
			++c.gridy;

			c.gridx = 0;
			c.gridwidth = 2;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.LINE_START;
			add( new JLabel("A full description of the bug:"), c );
			++c.gridy;

			add( description, c );
			++c.gridy;

			{
				JPanel p = new JPanel();

				submitReport = new JButton( "Submit Bug Report" );
				submitReport.addActionListener( this );
				p.add( submitReport );

				cancel = new JButton( "Cancel" );
				cancel.addActionListener( this );
				p.add( cancel );

				c.anchor = GridBagConstraints.CENTER;
				c.gridwidth = 2;
				add( p, c );
			}

			pack();
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
				if( password.getText().length() == 0 ) {
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

	public void run( String ignore ) {

		String summary = "[Your summary of the problem or bug.]";
		String description = "[A full description of the problem or bug and how to reproduce it.]";
		String username = Prefs.get(usernamePreferenceKey,"");

		while( true ) {

			NewBugDialog dialog = new NewBugDialog( username, summary, description );
			dialog.show();

			if( ! dialog.askedToSubmit )
				return;

			username = dialog.username.getText().trim();
			Prefs.set( usernamePreferenceKey, username );
			Prefs.savePreferences();

			String password = dialog.password.getText();
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
				new TextWindow( "Login Reply Page", result.authenticationResultPage, 30, 10 );
				new TextWindow( "Submission Reply Page", result.submissionResultPage, 30, 10 );
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
}
