package fiji.process;

import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import org.nfunk.jep.JEP;
import org.nfunk.jep.type.DoubleNumberFactory;




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
public class IepGui extends javax.swing.JFrame implements ImageListener, ActionListener {


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
	
	public static final int CANCELED 	= 1;
	public static final int OK			= 0;	
	
	private static final long serialVersionUID = 1L;
	private static final int BOX_SPACE 	= 40;
	public static final String[] MESSAGES = {		
		"Enter an expression using canonical mathematical functions, and capital single" +
		"letters as variable specifying the chosen image.\n" +
		"<p>" +
		"Examples: <br>" +
		"&nbsp&nbsp A+2*B*C<br>" +
		"&nbsp&nbsp sqrt(A^2+B^2)*cos(C)<br>" +
		"&nbsp&nbsp A > B<br>" +
		"<p>" +
		"<u>Supported functions:</u><br>" +
		"<table border=\"1\">" +
		"<tr>" +
		"<th>Function</th>" +
		"<th>Syntax</th>" +
		"</tr>" +
		"<tr>" +
		"<td><i>e</i></td> <td>e</td>"+
		"</tr>"+
		"<tr>"+
		"<td>π</td> <td>pi</td>"+
		"</tr>"+
		"<tr>" +
		"<td>Sine</td><td>sin</td>"+
		"</tr>"+
		"<tr>" +
		"<td>Cosine</td><td>cos</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Tangent</td><td>tan</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Arc Sine</td><td>asin</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Arc Cosine</td><td>acos</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Arc Tangent</td><td>atan</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Arc Tangent 2 args</td><td>atan2(y,x)</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Sine</td><td>sinh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Cosine</td><td>cosh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Tangent</td><td>tanh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Arc Sine</td><td>asinh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Arc Cosine</td><td>acosh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Hyperbolic Arc Tangent</td><td>atanh</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Logarithm (base 10)</td><td>log</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Natural Logarithm</td><td>ln</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Exponential</td><td>exp</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Power</td><td>pow</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Square Root</td><td>sqr</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Absolute Value</td><td>abs</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Modulus</td><td>mod</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Sum</td><td>sum</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Randon Number</td><td>rand</td>" +
		"</tr>"+
		"<tr>" +
		"<td>'If' tests</td><td>if(condExpr,posExpr,negExpr)</td>" +
		"</tr>"+
		"<tr>" +
		"<td>To String</td><td>str</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Binomial Function</td><td>binom(n,i)</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Round</td><td>round</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Floor</td><td>floor</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Ceiling</td><td>ceil</td>" +
		"</table> ",
		"No images are opened.",
		"Image dimensions are incompatibles."
		};
	
	 private ArrayList<ActionListener> action_listeners = new ArrayList<ActionListener>();
	
	/** Number of image boxes currently displayed */
	private int n_image_box = 0;
	/** List of ImagePlus currently opened in ImageJ */
	private ArrayList<ImagePlus> images;
	/** Array of images names, for display in image boxes */
	private String[] image_names;
	/** List of Labels for the image boxes */
	private ArrayList<JLabel> labels = new ArrayList<JLabel>();
	/** List of Combo boxes */
	private ArrayList<JComboBox> image_boxes = new ArrayList<JComboBox>();
	/** Array of image variables */
	private String[] variables;
	
	private JPanel jPanelImages;
	private JSplitPane jSplitPane1;
	private JButton jButtonMinus;
	private JButton jButtonPlus;
	private JPanel jPanelLeftButtons;
	private JScrollPane jScrollPaneImages;
	private JPanel jPanelLeft;
	private JPanel jPanelRight;
	private JEditorPane jTextAreaInfo;
	private JScrollPane jScrollPane1;
	private JButton jButtonOK;
	private JButton jButtonCancel;
	private JTextField jTextFieldExpression;
	private JLabel jLabelExpression;

	/**
	 * Main method for debug
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
	
	/*
	 * CONSTRUCTOR
	 */
	
	public IepGui() {
		super();
		initImageList();
		initGUI();
		addImageBox();
		jButtonMinus.setEnabled(false);
		jTextAreaInfo.setText(MESSAGES[0]);
		jTextAreaInfo.setCaretPosition(0);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Return the variable list set by this GUI. Variables names are formed by single
	 * capital letter, from A to Z, in alphabetical order. The matching {@link ImagePlus}
	 * in the array returned by {@link #getImages()} has the same index.
	 * @see {@link #getImages()}, {@link #getExpression()}
	 */
	public String[] getVariables() {
		return variables;
	}
	
	/**
	 * Return the {@link ImagePlus} array set by this GUI. In the expression, the matching
	 * variable name has the same index (A for the first, etc...)
	 * @see {@link IepGui#getExpression()}, {@link #getVariables()}
	 */
	public ImagePlus[] getImages() {
		final ImagePlus[] imps = new ImagePlus[n_image_box];
		JComboBox box;
		for ( int i=0; i<n_image_box; i++) {
			box = image_boxes.get(i);
			imps[i] = images.get(box.getSelectedIndex());
		}
		return imps;
	}
	
	/**
	 * Return the expression set by this GUI. The GUI works ensures that the user can press the 'OK'
	 * button only if this expression is mathematically valid and has valid variables.
	 * @see {@link #getVariables()}, {@link #getImages()}  
	 */
	public String getExpression() {
		return jTextFieldExpression.getText();
	}
	
	public void addActionListener(ActionListener l) {
		action_listeners.add(l);
	}

	public void removeActionListener(ActionListener l) {
		action_listeners.remove(l);
	}

	public ActionListener[] getActionListeners() {
		return (ActionListener[]) action_listeners.toArray();
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private void fireActionProperty(int event_id, String command) {
		ActionEvent action = new ActionEvent(this, event_id, command);
		for (ActionListener l : action_listeners) {
			synchronized (l) {
				l.notifyAll();
				l.actionPerformed(action);
			}
		}
	}
	
	/**
	 * Called when an image is selected in a box. 
	 */
	private void checkDimensions() {
		if (compatibleDimensions()) {
			jButtonOK.setEnabled(true);
			for (JComboBox box : image_boxes) {
				box.setForeground(Color.BLACK);
			}
			jTextAreaInfo.setText(MESSAGES[0]);
			jTextAreaInfo.setCaretPosition(0);
		} else {
			jButtonOK.setEnabled(false);
			for (JComboBox box : image_boxes) {
				box.setForeground(Color.RED);
			}
			jTextAreaInfo.setText(MESSAGES[2]);
			jTextAreaInfo.setCaretPosition(0);
		}
	}
	
	/**
	 * Check that dimensions are compatible.
	 */
	private boolean compatibleDimensions() {
		if (images.size() <= 1) {
			return true;
		}
		int[] dim;
		int[] old_dim = images.get(0).getDimensions();
		for (int i=1; i<images.size(); i++) {
			dim = images.get(i).getDimensions();
			if (dim.length != old_dim.length) {
				return false;
			}
			for (int j = 0; j < old_dim.length; j++) {
				if (dim[j] != old_dim[j]) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Called when the user type something in the expression area. 
	 */
	private void checkExpression() {
		final String expression = jTextFieldExpression.getText();
		if ( (null == expression) || (expression.equals(""))  ) {
			jButtonOK.setEnabled(false);
			jTextAreaInfo.setText(MESSAGES[0]);
			jTextAreaInfo.setCaretPosition(0);
			return;
		}
		final JEP parser = new JEP(false, false, false, new DoubleNumberFactory());
		parser.addStandardConstants();
		parser.addStandardFunctions();
		for ( String var : variables ) {
			parser.addVariable(var, null); // we do not care for value yet
		}
		parser.parseExpression(expression);
		final String error = parser.getErrorInfo();
		if ( null == error) {
			jButtonOK.setEnabled(true);
			jTextFieldExpression.setForeground(Color.BLACK);
			jTextAreaInfo.setText(MESSAGES[0]);
			jTextAreaInfo.setCaretPosition(0);
		} else {
			jButtonOK.setEnabled(false);
			jTextFieldExpression.setForeground(Color.RED);
			jTextAreaInfo.setText(error);
		}
	}
	
	/**
	 * Called when the user presses the + button.
	 * Does not allow to add more than 26 boxes.
	 */
	private void addImageBox() {
		if (n_image_box >= 26) return;
		char c = (char) ('A'+n_image_box);
		final JLabel label = new JLabel(String.valueOf(c)+":");
		jPanelImages.add(label);
		final JComboBox combo_box = new JComboBox(image_names);
		jPanelImages.add(combo_box);
		combo_box.setSelectedIndex(Math.min(n_image_box, image_names.length-1));
		final int width = jPanelImages.getWidth();
		label.setBounds(10, 10+BOX_SPACE*n_image_box, 20, 25);
		combo_box.setBounds(30, 10+BOX_SPACE*n_image_box, width-50, 30);
		combo_box.setFont(new Font("Arial", Font.PLAIN, 10));
		combo_box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { checkDimensions();	}
		});
		labels.add(label);
		image_boxes.add(combo_box);
		jPanelImages.setPreferredSize(new Dimension(width, 50+BOX_SPACE*n_image_box));
		n_image_box++;
		refreshVariableList();
		checkExpression();
		if (n_image_box >= 26) {
			jButtonPlus.setEnabled(false);
		}
		if (n_image_box > 1) {
			jButtonMinus.setEnabled(true);
		}
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
		refreshVariableList();
		checkExpression();
		if (n_image_box <= 1) {
			jButtonMinus.setEnabled(false);
		}
		if (n_image_box < 26) {
			jButtonPlus.setEnabled(true);
		}
	}
	
	/**
	 * Builds the list of currently opened {@link ImagePlus} in ImageJ.
	 */
	private void initImageList() {
		int[] IDs = WindowManager.getIDList();
		if (null == IDs) {
			image_names = new String[] { MESSAGES[1] };
			images = new ArrayList<ImagePlus>();
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
	 * Refresh the name list of images, from the field {@link #images}, and send it
	 * to the {@link JComboBox} that display then.
	 */
	private void refreshImageNames() {
		if (images.size() < 1) {
			image_names = new String[] { MESSAGES[1] };
		} else {
			image_names = new String[images.size()];
		}
		for (int i = 0; i < images.size(); i++) {
			image_names[i] = images.get(i).getTitle();			
		}
		int current_index;
		int max_index = images.size()-1;
		for (JComboBox box : image_boxes) {
			current_index = box.getSelectedIndex();
			box.setModel(new DefaultComboBoxModel(image_names));
			box.setSelectedIndex(Math.min(current_index, max_index));
		}
		checkDimensions();
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
	 * Refresh the array of variables
	 */
	private void refreshVariableList() {
		variables = new String[n_image_box];
		for (int i=0; i < n_image_box; i++) {
			char c = (char) ('A'+i);
			variables[i] = String.valueOf(c);
		}
	}
	
	/*
	 * ACTIONLISTENER METHOD
	 */
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.contentEquals(jButtonOK.getText())) {
			fireActionProperty(OK, jButtonOK.getText());
		} else if (command.contentEquals(jButtonCancel.getText())) {
			fireActionProperty(CANCELED, jButtonCancel.getText());
		}
	}
	
	/*
	 * IMAGELISTENER METHODS
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
					GridBagLayout jPanelRightLayout = new GridBagLayout();
					jPanelRightLayout.rowWeights = new double[] {0.0, 0.0, 0.5, 0.0};
					jPanelRightLayout.rowHeights = new int[] {7, 7, 7, 7};
					jPanelRightLayout.columnWeights = new double[] {0.0, 0.5, 0.0};
					jPanelRightLayout.columnWidths = new int[] {4, 7, 7};
					jSplitPane1.add(jPanelRight, JSplitPane.RIGHT);
					jPanelRight.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
					jPanelRight.setLayout(jPanelRightLayout);
					jPanelRight.setMinimumSize(new java.awt.Dimension(1, 1));
					{
						jLabelExpression = new JLabel();
						jPanelRight.add(jLabelExpression, new GridBagConstraints(-1, 0, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 0, 0));
						jLabelExpression.setText("Expression:");
					}
					{
						jTextFieldExpression = new JTextField();
						jPanelRight.add(jTextFieldExpression, new GridBagConstraints(-1, 1, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(20, 10, 0, 10), 0, 0));
						jTextFieldExpression.setBorder(new LineBorder(new java.awt.Color(252,117,0), 1, false));
						jTextFieldExpression.setSize(12, 18);
						jTextFieldExpression.addKeyListener(new KeyListener() {
							public void keyTyped(KeyEvent e) {}
							public void keyReleased(KeyEvent e) {
								checkExpression();
							}
							public void keyPressed(KeyEvent e) {}
						});
					}
					{
						jButtonCancel = new JButton();
						jPanelRight.add(jButtonCancel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
						jButtonCancel.setText("Cancel");
						jButtonCancel.addActionListener(this);
					}
					{
						jButtonOK = new JButton();
						jPanelRight.add(jButtonOK, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 10), 0, 0));
						jButtonOK.setText("OK");
						jButtonOK.setEnabled(false);
						jButtonOK.addActionListener(this);
					}
					{
						jScrollPane1 = new JScrollPane();
						jPanelRight.add(jScrollPane1, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
						jScrollPane1.setOpaque(false);
						jScrollPane1.setBorder(null);
						jScrollPane1.getViewport().setOpaque(false);
						jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						{
							jTextAreaInfo = new JEditorPane();
							jTextAreaInfo.setBorder(null);
							jScrollPane1.setViewportView(jTextAreaInfo);
							jTextAreaInfo.setFont(new Font("Arial", Font.PLAIN, 10));
							jTextAreaInfo.setEditable(false);
							jTextAreaInfo.setOpaque(false);
							jTextAreaInfo.setContentType("text/html");
							jTextAreaInfo.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
//							jTextAreaInfo.setLineWrap(true);
//							jTextAreaInfo.setWrapStyleWord(true);
						}
					}
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
							jButtonPlus.setBounds(7, 5, 22, 28);
							jButtonPlus.setFont(new java.awt.Font("Times New Roman",0,18));
							jButtonPlus.setSize(25, 25);
							jButtonPlus.setOpaque(true);
							jButtonPlus.addActionListener(new ActionListener() {								
								public void actionPerformed(ActionEvent e) {
									addImageBox();
								}
							});
						}
						{
							jButtonMinus = new JButton();
							jPanelLeftButtons.add(jButtonMinus);
							jButtonMinus.setText("—");
							jButtonMinus.setBounds(31, 5, 20, 28);
							jButtonMinus.setFont(new java.awt.Font("Arial",0,12));
							jButtonMinus.setSize(25, 25);
							jButtonMinus.setOpaque(true);
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
}
