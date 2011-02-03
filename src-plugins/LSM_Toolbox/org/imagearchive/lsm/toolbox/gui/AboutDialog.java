package org.imagearchive.lsm.toolbox.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.text.html.HTMLEditorKit;

import org.imagearchive.lsm.toolbox.MasterModel;

public class AboutDialog extends JDialog {

	private MasterModel masterModel;

	private JEditorPane about;

	private JEditorPane changelog;

	private JEditorPane iconset;

	private JEditorPane license;

	private JEditorPane help;

	private JScrollPane aboutScroller;

	private JScrollPane changelogScroller;

	private JScrollPane helpScroller;

	private JScrollPane licenseScroller;

	private JScrollPane iconsetScroller;

	private JTabbedPane tabber;

	private JButton okButton;

	private Dimension ScreenDimension = Toolkit.getDefaultToolkit()
			.getScreenSize();

	private int ScreenX = (int) ScreenDimension.getWidth();

	private int ScreenY = (int) ScreenDimension.getHeight();

	private JLabel infoTitle;

	private String infoText = "<html><center>LSM_Toolbox ver "
			+ MasterModel.VERSION
			+ " (C) 2003-2009 Patrick Pirrotte </center></html>";

	//private HtmlPageLoader loader;

	public AboutDialog(JFrame parent, MasterModel masterModel)
			throws HeadlessException {
		super(parent, true);
		this.masterModel = masterModel;
		initializeGUI();
		loadPages();
	}

	public AboutDialog() throws HeadlessException {
		initializeGUI();
	}

	public void initializeGUI() {
		setTitle("About");
		tabber = new javax.swing.JTabbedPane();
		okButton = new JButton(new ImageIcon(getClass().getResource(
				"images/ok.png")));
		aboutScroller = new JScrollPane();
		changelogScroller = new JScrollPane();
		about = new JEditorPane();
		changelog = new JEditorPane();
		license = new JEditorPane();
		iconset = new JEditorPane();
		help = new JEditorPane();
		helpScroller = new JScrollPane();
		licenseScroller = new JScrollPane();
		iconsetScroller = new JScrollPane();
		infoTitle = new JLabel();
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				dispose();
			}
		});
		tabber.setPreferredSize(new Dimension(ScreenX / 2, ScreenY / 2));
		infoTitle.setText(infoText);
		aboutScroller.setViewportView(about);
		changelogScroller.setViewportView(changelog);
		help.setEditorKit(new HTMLEditorKit());
		iconsetScroller.setViewportView(iconset);
		helpScroller.setViewportView(help);
		licenseScroller.setViewportView(license);

		tabber.addTab("About", aboutScroller);
		tabber.addTab("Changelog", changelogScroller);
		tabber.addTab("Help", helpScroller);
		tabber.addTab("LSM_Toolbox Licence", licenseScroller);
		tabber.addTab("Nuvola Iconset License", iconsetScroller);

		String loadingText = "Loading... please wait...";
		about.setText(loadingText);
		changelog.setText(loadingText);
		help.setText(loadingText);
		license.setText(loadingText);
		iconset.setText(loadingText);

		about.setContentType("text/html");
		about.setEditable(false);
		changelog.setContentType("text/html");
		changelog.setEditable(false);
		iconset.setContentType("text/html");
		iconset.setEditable(false);
		license.setContentType("text/html");
		license.setEditable(false);
		help.setContentType("text/html");
		help.setEditable(false);

		infoTitle.setBorder(BorderFactory.createEtchedBorder());
		infoTitle.setHorizontalAlignment(JLabel.CENTER);

		getContentPane().add(infoTitle, BorderLayout.NORTH);
		getContentPane().add(tabber, BorderLayout.CENTER);
		getContentPane().add(okButton, BorderLayout.SOUTH);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		pack();
		centerWindow();
	}

	public void loadPages() {
		Object[][] pages = new Object[][]{{about,"html/about.htm"},{changelog,"html/changelog.htm"},{iconset,"html/lgpl.txt"},{license,"html/licence.txt"},{help,"html/help.htm"}};
		HtmlPageLoader loader = new HtmlPageLoader(this, pages);
		loader.start();

	}

	public void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - this.getWidth()) / 2,
				(screenSize.height - this.getHeight()) / 2);
	}

	public static void main(String[] args) {
		new AboutDialog(new JFrame(),null).setVisible(true);
		System.exit(-1);
	}
}
