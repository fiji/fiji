package fiji.process;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class IepGui extends javax.swing.JFrame implements ImageListener {


	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = 1L;
	private static final int BOX_SPACE 	= 40;
	
	/** Number of image boxes currently displayed */
	private int n_image_box;
	/** List of ImagePlus currently opened in ImageJ */
	private ArrayList<ImagePlus> images;
	private JPanel jPanelImages;
	/** Array of images names, for display in image boxes */
	private String[] image_names;
	/** List of Labels for the image boxes */
	private ArrayList<JLabel> labels = new ArrayList<JLabel>();
	/** List of Combo boxes */
	private ArrayList<JComboBox> image_boxes = new ArrayList<JComboBox>();
	
	
	private JSplitPane jSplitPane1;
	private JButton jButtonMinus;
	private JButton jButtonPlus;
	private JPanel jPanelLeftButtons;
	private JScrollPane jScrollPaneImages;
	private JPanel jPanelLeft;
	private JPanel jPanelRight;

	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				IepGui inst = new IepGui();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
	public IepGui() {
		super();
		initImageList();
		initGUI();
		addImageBox();
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Called when the user presses the + button.
	 * Does not allow to add more than 26 boxes.
	 */
	private void addImageBox() {
		if (n_image_box >= 26) return;
		char c = (char) ('a'+n_image_box);
		final JLabel label = new JLabel(String.valueOf(c)+":");
		jPanelImages.add(label);
		final JComboBox combo_box = new JComboBox(image_names);
		jPanelImages.add(combo_box);
		combo_box.setSelectedIndex(Math.min(n_image_box, image_names.length-1));
		final int width = jPanelImages.getWidth();
		label.setBounds(10, 10+BOX_SPACE*n_image_box, 20, 25);
		combo_box.setBounds(30, 10+BOX_SPACE*n_image_box, width-50, 30);
		combo_box.setFont(new Font("Arial", Font.PLAIN, 10));
		labels.add(label);
		image_boxes.add(combo_box);
		jPanelImages.setPreferredSize(new Dimension(width, 50+BOX_SPACE*n_image_box));
		n_image_box++;
	}
	
	private void removeImageBox() {
		if (n_image_box <= 1) return;
		n_image_box--;
		jPanelImages.remove(image_boxes.remove(n_image_box));
		jPanelImages.remove(labels.remove(n_image_box));
		final int width = jPanelImages.getWidth();
		jPanelImages.setPreferredSize(new Dimension(width, 50+BOX_SPACE*n_image_box));
		jPanelImages.revalidate();
		jPanelImages.repaint();
	}
	
	/**
	 * Builds the list of currently opened {@link ImagePlus} in ImageJ.
	 */
	private void initImageList() {
		int[] IDs = WindowManager.getIDList();
		if (null == IDs) {
			image_names = new String[] {"No images are opened"};
			return;
		}
		ImagePlus imp;
		images = new ArrayList<ImagePlus>(IDs.length);
		for (int i = 0; i < IDs.length; i++) {
			imp = WindowManager.getImage(IDs[i]);
			images.add(imp);
		}
		refreshImageNames();
	}
	
	/**
	 * Refresh the name list of images, from the field {@link #images}.
	 */
	private void refreshImageNames() {
		image_names = new String[images.size()];
		for (int i = 0; i < images.size(); i++) {
			image_names[i] = images.get(i).getTitle();
		}		
	}
	
	/**
	 * Redisplay the image boxes
	 */
	private void refreshImageBoxes() {
		int width = jPanelImages.getWidth();
		for (int i=0; i<n_image_box; i++) {
			image_boxes.get(i).setBounds(30, 10+BOX_SPACE*i, width-50, 30);
		}
	}
	
	/**
	 * Display the GUI
	 */
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setTitle("Image Expression Parser");
			{
				jSplitPane1 = new JSplitPane();
				getContentPane().add(jSplitPane1, BorderLayout.CENTER);
				jSplitPane1.setPreferredSize(new java.awt.Dimension(439, 302));
				jSplitPane1.setDividerLocation(200);
				jSplitPane1.setResizeWeight(0.5);
				{
					jPanelRight = new JPanel();
					jSplitPane1.add(jPanelRight, JSplitPane.RIGHT);
					jPanelRight.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
				}
				{
					jPanelLeft = new JPanel();
					BorderLayout jPanelLeftLayout = new BorderLayout();
					jPanelLeft.setLayout(jPanelLeftLayout);
					jSplitPane1.add(jPanelLeft, JSplitPane.LEFT);
					jPanelLeft.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
					jPanelLeft.setPreferredSize(new java.awt.Dimension(198, 274));
					{
						jScrollPaneImages = new JScrollPane();
						jPanelLeft.add(jScrollPaneImages, BorderLayout.CENTER);
						jScrollPaneImages.setPreferredSize(new java.awt.Dimension(92, 234));
						jScrollPaneImages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						jScrollPaneImages.getVerticalScrollBar().setUnitIncrement(20);
						{
							jPanelImages = new JPanel();
							jScrollPaneImages.setViewportView(jPanelImages);
							jPanelImages.setLayout(null);
							jPanelImages.setPreferredSize(new java.awt.Dimension(1, 1));
						}
					}
					{
						jPanelLeftButtons = new JPanel();
						jPanelLeft.add(jPanelLeftButtons, BorderLayout.SOUTH);
						jPanelLeftButtons.setLayout(null);
						jPanelLeftButtons.setPreferredSize(new java.awt.Dimension(196, 35));
						jPanelLeftButtons.setSize(196, 35);
						{
							jButtonPlus = new JButton();
							jPanelLeftButtons.add(jButtonPlus);
							jButtonPlus.setText("+");
							jButtonPlus.setBorder(BorderFactory.createTitledBorder(""));
							jButtonPlus.setBounds(7, 5, 22, 28);
							jButtonPlus.setFont(new java.awt.Font("Arial",1,12));
							jButtonPlus.setSize(25, 25);
							jButtonPlus.addActionListener(new ActionListener() {								
								public void actionPerformed(ActionEvent e) {
									addImageBox();
								}
							});
						}
						{
							jButtonMinus = new JButton();
							jPanelLeftButtons.add(jButtonMinus);
							jButtonMinus.setText("-");
							jButtonMinus.setBorder(BorderFactory.createTitledBorder(""));
							jButtonMinus.setBounds(35, 5, 20, 28);
							jButtonMinus.setFont(new java.awt.Font("Arial",1,12));
							jButtonMinus.setSize(25, 25);
							jButtonMinus.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									removeImageBox();									
								}
							});
						}
					}
					jPanelLeft.addComponentListener(new ComponentListener() {						
						public void componentShown(ComponentEvent e) {	}
						public void componentResized(ComponentEvent e) {
							refreshImageBoxes();
						}						
						public void componentMoved(ComponentEvent e) { }
						public void componentHidden(ComponentEvent e) { }
					});
				}
			}
			pack();
			setSize(400, 300);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * IMAGELISTENER METHODSs
	 */
	
	public void imageClosed(ImagePlus imp) {
		images.remove(imp);
		refreshImageNames();
	}

	public void imageOpened(ImagePlus imp) {
		images.add(imp);
		refreshImageNames();
	}

	public void imageUpdated(ImagePlus imp) {	}
}
