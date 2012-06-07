package fiji.plugin.trackmate.gui;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;


public class ImagePlusChooser extends javax.swing.JFrame {

	private static final long serialVersionUID = 322598397229876595L;

	public final ActionEvent OK_BUTTON_PUSHED = new ActionEvent(this, 0, "OK");
	public final ActionEvent CANCEL_BUTTON_PUSHED = new ActionEvent(this, 1, "Cancel");

	private JPanel jPanelMain;
	private JLabel jLabelSelect;
	private JComboBox jComboBoxImage;
	private JButton jButtonCancel;
	private JButton jButtonOK;
	private ArrayList<ImagePlus> images;
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ImagePlusChooser inst = new ImagePlusChooser();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}

	/*
	 * CONSTRUCTOR
	 */

	public ImagePlusChooser() {
		super();
		initGUI();
		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
			@Override
			public void windowClosing(WindowEvent e) {}
			@Override
			public void windowClosed(WindowEvent e) {
				fireAction(CANCEL_BUTTON_PUSHED);
			}
			@Override
			public void windowActivated(WindowEvent e) {}
		});
	}


	/*
	 * METHODS
	 */

	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	public boolean removeActionListener(ActionListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Return the selected {@link ImagePlus} in the combo list, or <code>null</code> if 
	 * the first choice "3D viewer" was selected.
	 */
	public ImagePlus getSelectedImagePlus() {
		int index = jComboBoxImage.getSelectedIndex();
		if (index < 1) 
			return null;
		else 
			return images.get(index-1);
	}


	/*
	 * PRIVATE METHODS
	 */

	private void fireAction(ActionEvent event) {
		for(ActionListener listener : listeners) 
			listener.actionPerformed(event);
	}


	/**
	 * Refresh the name list of images, from the field {@link #images}, and send it
	 * to the {@link JComboBox} that display then.
	 */
	private String[] getImageNames() {
		int[] IDs = WindowManager.getIDList();
		String[] image_names = null;
		if (null == IDs) {
			image_names = new String[] { "New 3D viewer" };
			images = new ArrayList<ImagePlus>();
			return image_names;
		}
		ImagePlus imp;
		images = new ArrayList<ImagePlus>(IDs.length);
		for (int i = 0; i < IDs.length; i++) {
			imp = WindowManager.getImage(IDs[i]);
			images.add(imp);
		}
		if (images.size() < 1) {
			image_names = new String[] { "New 3D viewer" };
		} else {
			image_names = new String[images.size() + 1];
		}
		image_names[0] = "New 3D viewer";
		for (int i = 0; i < images.size(); i++) {
			image_names[i+1] = images.get(i).getTitle();			
		}
		return image_names;
	}


	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			{
				jPanelMain = new JPanel();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(null);
				{
					jLabelSelect = new JLabel();
					jPanelMain.add(jLabelSelect);
					jLabelSelect.setFont(FONT);
					jLabelSelect.setText("Copy overlay to:");
					jLabelSelect.setBounds(12, 10, 258, 15);
				}
				{
					ComboBoxModel jComboBoxImageModel = new DefaultComboBoxModel(getImageNames());
					jComboBoxImage = new JComboBox();
					jPanelMain.add(jComboBoxImage);
					jComboBoxImage.setFont(SMALL_FONT);
					jComboBoxImage.setModel(jComboBoxImageModel);
					jComboBoxImage.setBounds(12, 31, 258, 22);
				}
				{
					jButtonCancel = new JButton();
					jPanelMain.add(jButtonCancel);
					jButtonCancel.setFont(FONT);
					jButtonCancel.setText("Cancel");
					jButtonCancel.setBounds(12, 65, 64, 26);
					jButtonCancel.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							fireAction(CANCEL_BUTTON_PUSHED);
						}
					});
				}
				{
					jButtonOK = new JButton();
					jPanelMain.add(jButtonOK);
					jButtonOK.setFont(FONT);
					jButtonOK.setText("OK");
					jButtonOK.setBounds(205, 65, 65, 26);
					jButtonOK.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							fireAction(OK_BUTTON_PUSHED);
						}
					});
				}
			}
			pack();
			this.setSize(280, 130);
			this.setTitle("Copy overlay");
			this.setResizable(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
