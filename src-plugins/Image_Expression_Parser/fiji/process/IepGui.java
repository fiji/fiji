package fiji.process;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.RGBStackSplitter;

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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

import fiji.expressionparser.ImgLibParser;

/**
 * <h2>GUI for the plugin {@link Image_Expression_Parser}</h2>
 * <p>
 * When launched it displays a window allowing the user to enter a mathematical expression,
 * with variables (capital single letters, so up to 26 variables) and canonical functions.
 * The expression is checked by the parser of the JEP library (http://www.singularsys.com/jep/).
 * If it is not valid, a hopefully useful error message is displayed.
 * <p>
 * Variables can be added using +/- buttons. They are matched to opened images in ImageJ. 
 * <p>
 * When the images are RGB images, they are processed in a special way:
 * <ul>
 * 	<li> they are split in 3 RGB channels;
 * 	<li> each channel is parsed and evaluated separately;
 * 	<li> the 3 resulting images are put back together in a 3 channel composite image.
 * </ul>
 * 
 * <p>
 * The information the user entered can be retrieved afterwards with the following methods:
 * <ul>
 * <li>{@link #getExpression()} to retrieve the expression the user entered as a String
 * <li>{@link #getImageMap()} to retrieve the couples Variable names - Images set by the user.
 * </ul>
 * <p>
 * See {@link Image_Expression_Parser} for more information.
 * 
 * <p>
 * This GUI was built in part using Jigloo GUI builder http://www.cloudgarden.com/jigloo/.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>, Albert Cardona <acardona@ini.phys.ethz.ch>
 */
public class IepGui <T extends RealType<T>> extends javax.swing.JFrame implements ImageListener, WindowListener {


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
	
	private static final String PLUGIN_VERSION = "v2.2";
	private static final String PLUGIN_NAME = "Image Expression Parser";
	
	/** The GUI fires an ActionEvent with this command String when the quit button is pressed. */
	public static final String QUIT_ACTION_COMMAND = "Quit";
	/** The GUI fires an ActionEvent with this command String when the parse button is pressed
	 * or the enter key is pressed when in the expression field. */
	public static final String PARSE_ACTION_COMMAND = "Parse";
	
	private static final long serialVersionUID = 1L;
	private static final int BOX_SPACE 	= 40;
	/** Contains a html string referring to the expression syntax, and error messages to be displayed
	 * in the info text box. */
	public static final String[] MESSAGES = {		
		"Enter an expression using canonical mathematical functions, and capital single " +
		"letters as variable specifying the chosen image.\n " +
		"ImgLib algorithms are also supported. " +
		"<p>" +
		"Examples: <br>" +
		"&nbsp&nbsp ■ 2*A<br>" +
		"&nbsp&nbsp ■ A*(B+30)<br>" +
		"&nbsp&nbsp ■ sqrt(A^2+B^2)*cos(C)<br>" +
		"&nbsp&nbsp ■ A > B<br>" +
		"&nbsp&nbsp ■ gauss(A, 0.8)<br>" +
		"<p>" +
		"<u>Supported ImgLib algorithms:</u><br>" +
		"<table border=\"1\">" +
		"<tr>" +
		"<th>Description</th>" +
		"<th>Syntax</th>" +
		"</tr>"+
		"<tr>"+
		"<td>Gaussian convolution</td> <td>gauss(img, sigma)</td> "+
		"</tr>"+
		"<tr>"+
		"<td>Floyd-Steinberg dithering</td> <td>dither(img)</td> "+
		"</tr>"+
		"<tr>"+
		"<td>Image normalization (sum to 1)</td> <td>normalize(img)</td> "+
		"</table> " +
		"<p>" +
		"<u>Supported functions:</u><br>" +
		"<table border=\"1\">" +
		"<tr>" +
		"<th>Description</th>" +
		"<th>Syntax</th>" +
		"</tr>"+
		"<tr>"+
		"<td>Euler constant</td> <td>e</td>"+
		"</tr>"+
		"<tr>"+
		"<td>π</td> <td>pi</td>"+
		"</tr>"+
		"<tr>" +
		"<td>Standard operators</td><td>+, -, *, /, ^, %</td>"+
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
		"<td>Natural Logarithm</td><td>log</td>" +
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
		"<td>Round</td><td>round</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Floor</td><td>floor</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Ceiling</td><td>ceil</td>" +
		"</tr>"+
		"<tr>" +
		"<td>Boolean operators</td><td>!, &&, ||, <, >, !=, ==, >=, <=</td>" +	
		"</table> ",
		"No images are opened.",
		"Image dimensions are incompatibles."
		};
	
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
	/** This is where we store the history of valid command. */
	private DefaultComboBoxModel expression_history = new DefaultComboBoxModel();
	/** The plugin that normally calls this GUI and is responsible for calculations. 
	 * If null, it will be instantiated. */
	private Image_Expression_Parser<T> image_expression_parser;
	/** The ImagePlus that will be used to display results. */
	private ImagePlus target_imp;
	
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
	private JComboBox expressionField;
	private JLabel jLabelExpression;

	/**
	 * Main method for debug
	 */
	public static <T extends RealType<T>> void main(String[] args) {
		// Load an image
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		imp.show();
		// Launch the GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				IepGui<T> inst = new IepGui<T>();
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
		addWindowListener(this);
		addImageBox();
		jButtonMinus.setEnabled(false);
		jTextAreaInfo.setText(MESSAGES[0]);
		jTextAreaInfo.setCaretPosition(0);
		ImagePlus.addImageListener(this);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	
	/**
	 * Store a link to the Image_Expression_Parser class that will be used for
	 * computation.
	 */
	public void setIep(Image_Expression_Parser<T> iep) {
		this.image_expression_parser = iep;
	}
		
	
	/**
	 * Return a {@link Map} whose keys are the variable names as String, and the values
	 * are the {@link ImagePlus} linked to the key variable. In the case of this GUI,
	 * variables are single capital letters.
	 * @see {@link IepGui#getExpression()}
	 */
	public Map<String, ImagePlus> getImageMap() {
		if (images.size() > 0) {
			final HashMap<String, ImagePlus> image_map = new HashMap<String, ImagePlus>(variables.length);
			ImagePlus imp;
			String var;
			JComboBox box;
			for ( int i=0; i<n_image_box; i++) {
				box = image_boxes.get(i);
				imp = images.get(box.getSelectedIndex());
				var = variables[i];
				image_map.put(var, imp);
			}
			return image_map;
		} else {
			return null;
		}
	}
	
	/**
	 * Return the expression set by this GUI. The GUI works ensures that the user can press the 'OK'
	 * button only if this expression is mathematically valid and has valid variables.
	 * @see {@link #getImageMap()}  
	 */
	public String getExpression() {
		String expression = (String) expressionField.getSelectedItem();
		return expression.trim();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Return the {@link ImagePlus} array set by this GUI. In the expression, the matching
	 * variable name has the same index (A for the first, etc...)
	 * @see {@link IepGui#getExpression()}, {@link #getVariables()}
	 */
	private ImagePlus[] getImages() {
		if (images.size() > 0) {
			final ImagePlus[] imps = new ImagePlus[variables.length];
			JComboBox box;
			for ( int i=0; i<n_image_box; i++) {
				box = image_boxes.get(i);
				if (box.getSelectedIndex() < 0) {
					continue;
				}
				imps[i] = images.get(box.getSelectedIndex());
			}
			return imps;
		} else {
			return null;
		}
	}
	
	/**
	 * Called when something is selected or typed.
	 * Goal is to check that everything is valid. 
	 */
	private boolean checkValid() {	
		if (!this.isShowing()) return true; // prevent to check while init
		if (!compatibleDimensions()) {
			jButtonOK.setEnabled(false);
			for (JComboBox box : image_boxes) {
				box.setForeground(Color.RED);
			}
			jTextAreaInfo.setText(MESSAGES[2]);
			jTextAreaInfo.setCaretPosition(0);
			return false;

		} else { 

			String error = getExpressionError();
			if ( error.length() != 0) {
				jButtonOK.setEnabled(false);
				expressionField.getEditor().getEditorComponent().setForeground(Color.RED);
				jTextAreaInfo.setText(error);			
				jTextAreaInfo.setCaretPosition(0);
				return false;

			} else {

				jButtonOK.setEnabled(true);
				for (JComboBox box : image_boxes) {
					box.setForeground(Color.BLACK);
				}
				expressionField.getEditor().getEditorComponent().setForeground(Color.BLACK);
				jTextAreaInfo.setText(MESSAGES[0]);
				jTextAreaInfo.setCaretPosition(0);
				return true;
			}
		}
	}
	
	
	private void addCurrentExpressionToHistory() {
		String current_expression = (String) expressionField.getSelectedItem();
		String str;
		for (int i=0; i<expression_history.getSize(); i++) {
			str = (String) expression_history.getElementAt(i);
			if (str.equals(current_expression)) {
				return; // already there, we do not add
			}
		}
		expression_history.addElement(current_expression);
		
	}
	
	/**
	 * Check that dimensions are compatible.
	 */
	private boolean compatibleDimensions() {
		ImagePlus[] selected_images = getImages();
		if (null == selected_images) return true; // avoid check for empty list
		if (selected_images.length <= 1) {
			return true;
		}
		int[] dim;
		int[] old_dim = selected_images[0].getDimensions();
		for (int i=1; i<selected_images.length; i++) {
			dim = selected_images[i].getDimensions();
			if (dim.length != old_dim.length) {
				return false;
			}
			for (int j = 0; j < old_dim.length; j++) {
				if (dim[j] != old_dim[j]) {
					return false;
				}
			}
			old_dim = dim;
		}
		return true;
	}
	
	/**
	 * Called when the user type something in the expression area. 
	 */
	@SuppressWarnings("unchecked")
	private String getExpressionError() {
		final String expression = getExpression();
		if ( (null == expression) || (expression.equals(""))  ) {
			return "";
		}
		final ImgLibParser parser = new ImgLibParser();
		parser.addStandardConstants();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
		for ( String var : variables ) {
			parser.addVariable(var, null); // we do not care for value yet
		}
		parser.parseExpression(expression);
		if (!parser.hasError()) {
			return "";
		} else {
			final String error = parser.getErrorInfo();
			String[] errors = error.split("\\n");
			StringBuilder formatted_error = new StringBuilder();
			formatted_error.append("Found errors in expression:\n<p>");
			for (String str : errors) {
				if (str.startsWith("Encountered")) { // catch lengthy errors
					Pattern column_getter = Pattern.compile("column \\d+");
					Matcher match = column_getter.matcher(str);
					if (match.find()) {
						formatted_error.append("&nbsp&nbsp ■ Syntax error in expression at column "+match.group().substring(7)+"\n");
					}
				} else if (str.startsWith("Was expecting") || str.startsWith("    ") ){
					continue;
				} else {
					formatted_error.append("&nbsp&nbsp ■ "+str+"\n");
				}
				formatted_error.append("<p>\n");
			}
			return formatted_error.toString();
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
			public void actionPerformed(ActionEvent e) { checkValid(); }
		});
		labels.add(label);
		image_boxes.add(combo_box);
		jPanelImages.setPreferredSize(new Dimension(width, 50+BOX_SPACE*n_image_box));
		n_image_box++;
		refreshVariableList();
		checkValid();
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
		checkValid();
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
		if (this.isShowing()) checkValid();
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

	/** 
	 * Quit the GUI
	 */
	private void quitGui() {
		ImagePlus.removeImageListener(this);
		this.removeWindowListener(this);
		this.dispose();
	}

	/**
	 * Invoked when an action occur. This causes the GUI to grab all input and to start
	 * calculation.
	 */
	private void launchCalculation() {
		// This method is called in the context of the event dispatch thread

		// Check inputs
		boolean is_valid = checkValid();
		if (!is_valid) 
			return;

		// Collect input from GUI widgets while under the event dispatch thread
		final Map<String, ImagePlus> imp_map = getImageMap();
		final String expression	= getExpression();
		
		// Fork a new process to carry on the bulk of the execution,
		// freeing the event dispatch thread for other tasks like repainting windows
		// and dispatching other events:
		new Thread() {
			
			{
				// Reduce, at construction time, the priority of this Thread from ~15
				// (that of the parent Thread, the AWT-EventQueue-0, aka Event Dispatch Thread)
				// to a more suitable one that doesn't compete with event dispatch:
				setPriority(Thread.NORM_PRIORITY);
			}

			public void run() {
				
				// Lock the GUI
				IJ.showStatus("IEP parsing....");
				setGUIEnabled(false);

				try {
					if (null == image_expression_parser) {
						image_expression_parser = new Image_Expression_Parser<T>();
					}
					

					// Prepare parser
					image_expression_parser.setExpression(expression);

					Image<T> result_img = null;

					// Here, we check if we get a RGB image. They are handled in a special way: 
					// They are converted to 3 8-bit images which are processed separately, and
					// assembled back after processing.
					boolean is_rgb_image = false;
					for (String key : imp_map.keySet()) {
						if (imp_map.get(key).getType() == ImagePlus.COLOR_RGB) {
							is_rgb_image = true;
						}
					}
					
					if (is_rgb_image) {
						
						// Prepare holders
						Map<String, ImagePlus> red_map = new HashMap<String, ImagePlus>();
						Map<String, ImagePlus> green_map = new HashMap<String, ImagePlus>();
						Map<String, ImagePlus> blue_map = new HashMap<String, ImagePlus>();
						ArrayList<Map<String, ImagePlus>> map_array = new ArrayList<Map<String,ImagePlus>>(3);
						map_array.add(red_map);
						map_array.add(green_map);
						map_array.add(blue_map);
						
						// Split stacks
						RGBStackSplitter channel_splitter = new RGBStackSplitter();
						ImagePlus current_imp = null;
						for (String key : imp_map.keySet()) {
							current_imp = imp_map.get(key);
							// And stored individual channels in a new map
							channel_splitter.split(current_imp.getImageStack(), true);
							red_map.put  (key, new ImagePlus(current_imp.getShortTitle()+"-R", channel_splitter.red));
							green_map.put(key, new ImagePlus(current_imp.getShortTitle()+"-G", channel_splitter.green));
							blue_map.put (key, new ImagePlus(current_imp.getShortTitle()+"-B", channel_splitter.blue));
						}
						
						
						// Have the parser process individual channel separately
						Map<String, Image<T>> img_map;
						ImageStack[] result_array = new ImageStack[3];
						Image<T> tmp_image;
						int index = 0;
						for (Map<String, ImagePlus> current_map : map_array) {
							img_map = image_expression_parser.convertToImglib(current_map);
							image_expression_parser.setImageMap(img_map);
							image_expression_parser.process();
							// Collect results
							tmp_image = image_expression_parser.getResult();
							result_array[index] = ImageJFunctions.copyToImagePlus(tmp_image).getImageStack();
							index++;
						}
						
						// Merge back channels
						RGBStackMerge rgb_merger = new RGBStackMerge();
						ImagePlus new_imp = rgb_merger.createComposite(current_imp.getWidth(), current_imp.getHeight(), current_imp.getNSlices(), 
								result_array, false);
						new_imp.resetDisplayRange();
						
						if (target_imp == null) {
							target_imp = new_imp;
							target_imp.show();
						} else {
							if (!target_imp.isVisible()) {
								target_imp = new_imp;
								target_imp.show();
							} else {
								target_imp.setStack(expression, new_imp.getStack());
							}
						}

					} else {


						Map<String, Image<T>> img_map = image_expression_parser.convertToImglib(imp_map);
						image_expression_parser.setImageMap(img_map);
						// Call calculation
						image_expression_parser.process();
						// Collect result
						result_img = image_expression_parser.getResult();

						if (target_imp == null) {
							target_imp = ImageJFunctions.copyToImagePlus(result_img);
							target_imp.show();
						} else {
							ImagePlus new_imp = ImageJFunctions.copyToImagePlus(result_img);
							if (!target_imp.isVisible()) {
								target_imp = new_imp;
								target_imp.show();
							} else {
								target_imp.setStack(expression, new_imp.getStack());
							}
						}
						
					}

					target_imp.resetDisplayRange();
					target_imp.updateAndDraw();

				} catch (Exception e) {
					e.printStackTrace();
					IJ.error("An error occurred: " + e);
				} finally {
					// Re-enable the GUI
					IJ.showStatus("");
					setGUIEnabled(true);
				}
			}
		}.start();
	}

	/** Toggle GUI enabled/disabled via the event dispatch thread. */
	protected void setGUIEnabled(final boolean enabled) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				jButtonOK.setEnabled(enabled);
				expressionField.setEnabled(enabled);
				for (JComboBox box : image_boxes) {
					box.setEnabled(enabled);
				}
				if (enabled) {
					toFront();
					expressionField.requestFocusInWindow(); // give focus to expression field
				}
			}
		});
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
			this.setTitle(PLUGIN_NAME + " - " + PLUGIN_VERSION);
			{
				jSplitPane1 = new JSplitPane();
				getContentPane().add(jSplitPane1, BorderLayout.CENTER);
				jSplitPane1.setPreferredSize(new java.awt.Dimension(500, 500));
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
						jButtonCancel = new JButton();
						jPanelRight.add(jButtonCancel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
						jButtonCancel.setText("Quit");
						jButtonCancel.addActionListener(new ActionListener() {							
							// Close the GUI
							@Override
							public void actionPerformed(ActionEvent e) {
								quitGui();
							}
						});
					}
					{
						jButtonOK = new JButton();
						jPanelRight.add(jButtonOK, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 10), 0, 0));
						jButtonOK.setText("Parse");
						jButtonOK.setEnabled(false);
						jButtonOK.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								boolean valid = checkValid();
								if (valid) {
									addCurrentExpressionToHistory();
									launchCalculation();
								} 
							}
						});
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
						}
					}
				}
				{
					jPanelLeft = new JPanel();
					GridBagLayout jPanelLeftLayout = new GridBagLayout();
					jPanelLeftLayout.rowWeights = new double[] {0.0, 0.0, 1.0, 0.0};
					jPanelLeftLayout.rowHeights = new int[] {7, 7, -33, 50};
					jPanelLeftLayout.columnWeights = new double[] {0.1};
					jPanelLeftLayout.columnWidths = new int[] {7};
					jPanelLeft.setLayout(jPanelLeftLayout);
					jSplitPane1.add(jPanelLeft, JSplitPane.LEFT);
					jPanelLeft.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
					jPanelLeft.setPreferredSize(new java.awt.Dimension(198, 470));
					{
						jLabelExpression = new JLabel();
						jPanelLeft.add(jLabelExpression, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 0), 0, 0));
						jLabelExpression.setText("Expression:");
						jLabelExpression.setPreferredSize(new java.awt.Dimension(196, 16));
					}
					{
						expressionField = new JComboBox(expression_history);
						expressionField.setEditable(true);
						expressionField.setBorder(new LineBorder(new java.awt.Color(252,117,0), 1, false));
						expressionField.setSize(12, 18);
						jPanelLeft.add(expressionField, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
						expressionField.addActionListener(new ActionListener()  {
							@Override
							public void actionPerformed(ActionEvent e) {
								// Two action events are fired on edit: one for editing the textfield, one for changing
								// the combo box selection. We only catch the edition.								
								if (e.getActionCommand().equalsIgnoreCase("comboBoxEdited")) { 
									boolean valid = checkValid();
									if (valid) {
										addCurrentExpressionToHistory();
										launchCalculation();
									}
								}
							}

						});
					}
					{
						jScrollPaneImages = new JScrollPane();
						jPanelLeft.add(jScrollPaneImages, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0, 10, 0), 0, 0));
						jScrollPaneImages.setPreferredSize(new java.awt.Dimension(196, 267));
						jScrollPaneImages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						jScrollPaneImages.getVerticalScrollBar().setUnitIncrement(20);
						{
							jPanelImages = new JPanel();
							jScrollPaneImages.setViewportView(jPanelImages);
							jPanelImages.setLayout(null);
							jPanelImages.setPreferredSize(new java.awt.Dimension(190, 45));
						}
					}
					{
						jPanelLeftButtons = new JPanel();
						jPanelLeft.add(jPanelLeftButtons, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0, 10, 0), 0, 0));
						jPanelLeftButtons.setLayout(null);
						jPanelLeftButtons.setPreferredSize(new java.awt.Dimension(196, 35));
						jPanelLeftButtons.setSize(196, 35);
						{
							jButtonPlus = new JButton();
							jPanelLeftButtons.add(jButtonPlus);
							jButtonPlus.setText("+");
							jButtonPlus.setBounds(9, -2, 35, 35);
							jButtonPlus.setFont(new java.awt.Font("Times New Roman",0,18));
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
							jButtonMinus.setBounds(46, -2, 35, 35);
							jButtonMinus.setFont(new java.awt.Font("Arial",0,12));
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
			setSize(500, 500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * WINDOWLISTENER METHODS
	 */
	
	public void windowActivated(WindowEvent e) {	}
	public void windowClosed(WindowEvent e) {	}

	public void windowClosing(WindowEvent e) {	
		quitGui();
	}

	public void windowDeactivated(WindowEvent e) {	}
	public void windowDeiconified(WindowEvent e) {	}
	public void windowIconified(WindowEvent e) {	}
	public void windowOpened(WindowEvent e) {	}
}
