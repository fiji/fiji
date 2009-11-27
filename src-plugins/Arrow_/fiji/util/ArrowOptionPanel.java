package fiji.util;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import javax.swing.BorderFactory;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



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
public class ArrowOptionPanel extends javax.swing.JFrame {

	private static final long serialVersionUID = 1;
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private JPanel jPanelMain;
	private JLabel jLabelArrowThickness;
	private JSlider jSliderArrowThickness;
	private Canvas canvasDrawingArea;
	private JPanel jPanelDrawArea;
	private JCheckBox jCheckBoxFillArrow;
	private JTextField jTextFieldArrowThickness;
	private JLabel jLabelArrowLength;
	private JTextField jTextFieldHeadLength;
	private JSlider jSliderHeadLength;
	private JComboBox jComboBoxHeadStyle;
	private JLabel jLabelHeadStyle;

	/*
	 * INNER CLASSES
	 */
	
	/**
	 * Canvas that will draw an example arrow as specified by the GUI of this frame. 
	 */
	private class ArrowExampleCanvas extends Canvas {
		private static final long serialVersionUID = 1L;
		Point2D start, end;
		Arrow arrow;
		public void paint(Graphics g) {
			super.paint(g);
			start 	= new Point2D.Double(jPanelDrawArea.getWidth()*0.25, jPanelDrawArea.getHeight()/2.0);
			end 	= new Point2D.Double(jPanelDrawArea.getWidth()*0.75, jPanelDrawArea.getHeight()/2.0);
			arrow = (Arrow) jComboBoxHeadStyle.getSelectedItem();
			arrow.setStartPoint(start);
			arrow.setEndPoint(end);
			try {
				final double length = Double.parseDouble(jTextFieldHeadLength.getText() );
				arrow.setLength(length);				
			} catch (NumberFormatException nfe) { }
			Graphics2D g2 = (Graphics2D) g;
			g2.draw(arrow.getPath());
		}
		
	}
	
	/**
	 * Change listener that will listen to change in slider value and will set the corresponding {@link JTextField}
	 * value accordingly
	 */
	private class SliderChangeListener implements ChangeListener {
		private JTextField text_field;
		public SliderChangeListener(JTextField _text_field) {
			super();
			this.text_field = _text_field;
		}
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider) e.getSource();
			text_field.setText(String.format("%d", slider.getValue()) );
			canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
		}
	}
	
	/**
	 * ActionListener that will listen to change in text field value and set the corresponding {@link JSlider} 
	 * value accordingly
	 */
	private class TextFieldActionListener implements ActionListener {
		private JSlider slider;
		public TextFieldActionListener(JSlider _slider) {
			super();
			this.slider = _slider;
		}
		public void actionPerformed(ActionEvent e) {
			JTextField text_field = (JTextField) e.getSource();
			try {
				final double val = Double.parseDouble(text_field.getText());
				slider.setValue( (int) val);
			} catch (NumberFormatException nfe) {
				text_field.setText(String.format("%d", slider.getValue()));
			}
			canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
		}
		
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ArrowOptionPanel inst = new ArrowOptionPanel();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
	public ArrowOptionPanel() {
		super();
		initGUI();
		
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
			} 
		};
		jComboBoxHeadStyle.addActionListener(al);
		jTextFieldArrowThickness.addActionListener(new TextFieldActionListener(jSliderArrowThickness));
		jTextFieldHeadLength.addActionListener(new TextFieldActionListener(jSliderHeadLength));
		jSliderArrowThickness.addChangeListener(new SliderChangeListener(jTextFieldArrowThickness));
		jSliderHeadLength.addChangeListener(new SliderChangeListener(jTextFieldHeadLength));
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			this.setTitle("Arrow options");
			this.setResizable(false);
			{
			jPanelMain = new JPanel();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(null);
				jPanelMain.setFont(new java.awt.Font("Dialog",0,10));
				jPanelMain.setPreferredSize(new java.awt.Dimension(353, 165));
				{
					jLabelHeadStyle = new JLabel();
					jPanelMain.add(jLabelHeadStyle);
					jLabelHeadStyle.setText("Arrow head style");
					jLabelHeadStyle.setBounds(10, 15, 95, 15);
					jLabelHeadStyle.setFont(new java.awt.Font("Arial",0,10));
				}
				{
					ComboBoxModel jComboBoxHeadStyleModel = 
						new DefaultComboBoxModel( Arrow.values() );
					jComboBoxHeadStyle = new JComboBox();
					jPanelMain.add(jComboBoxHeadStyle);
					jComboBoxHeadStyle.setModel(jComboBoxHeadStyleModel);
					jComboBoxHeadStyle.setBounds(112, 11, 146, 24);
					jComboBoxHeadStyle.setFont(new java.awt.Font("Arial",0,10));
				}
				{
					jSliderHeadLength = new JSlider();
					jPanelMain.add(jSliderHeadLength);
					jSliderHeadLength.setBounds(107, 40, 115, 19);
					jSliderHeadLength.setFont(new java.awt.Font("Arial",0,10));
					jSliderHeadLength.setMinorTickSpacing(1);
					jSliderHeadLength.setMinimum(0);
					jSliderHeadLength.setMaximum(100);
					jSliderHeadLength.setValue(10);

				}
				{
					jTextFieldHeadLength = new JTextField();
					jPanelMain.add(jTextFieldHeadLength);
					jTextFieldHeadLength.setText("10");
					jTextFieldHeadLength.setBounds(225, 40, 30, 20);
					jTextFieldHeadLength.setFont(new java.awt.Font("Arial",0,10));
					jTextFieldHeadLength.setBackground(new java.awt.Color(238,238,238));
				}
				{
					jLabelArrowLength = new JLabel();
					jPanelMain.add(jLabelArrowLength);
					jLabelArrowLength.setText("Arrow head length");
					jLabelArrowLength.setBounds(10, 40, 94, 16);
					jLabelArrowLength.setFont(new java.awt.Font("Arial",0,10));
				}
				{
					jLabelArrowThickness = new JLabel();
					jPanelMain.add(jLabelArrowThickness);
					jLabelArrowThickness.setText("Arrow thickness");
					jLabelArrowThickness.setBounds(10, 65, 102, 16);
					jLabelArrowThickness.setFont(new java.awt.Font("Arial",0,10));
				}
				{
					jTextFieldArrowThickness = new JTextField(); 
					jPanelMain.add(jTextFieldArrowThickness);
					jTextFieldArrowThickness.setText("1");
					jTextFieldArrowThickness.setBounds(225, 60, 30, 20);
					jTextFieldArrowThickness.setFont(new java.awt.Font("Arial",0,10));
					jTextFieldArrowThickness.setBackground(new java.awt.Color(238,238,238));
				}
				{
					jSliderArrowThickness = new JSlider();
					jPanelMain.add(jSliderArrowThickness);
					jSliderArrowThickness.setBounds(110, 66, 115, 20);
					jSliderArrowThickness.setMinimum(1);
					jSliderArrowThickness.setMaximum(20);
					jSliderArrowThickness.setMinorTickSpacing(1);
					jSliderArrowThickness.setValue(1);
				}
				{
					jCheckBoxFillArrow = new JCheckBox();
					jPanelMain.add(jCheckBoxFillArrow);
					jCheckBoxFillArrow.setText("Fill");
					jCheckBoxFillArrow.setBounds(3, 85, 51, 23);
					jCheckBoxFillArrow.setFont(new java.awt.Font("Arial",0,10));
				}
				{
					jPanelDrawArea = new JPanel();
					BorderLayout jPanelDrawAreaLayout = new BorderLayout();
					jPanelDrawArea.setLayout(jPanelDrawAreaLayout);
					jPanelMain.add(jPanelDrawArea);
					jPanelDrawArea.setBounds(22, 108, 233, 45);
					jPanelDrawArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
					{
						canvasDrawingArea = new ArrowExampleCanvas();
						jPanelDrawArea.add(canvasDrawingArea, BorderLayout.CENTER);
						canvasDrawingArea.setPreferredSize(new java.awt.Dimension(231, 24));
					}
				}
			}
			pack();
			this.setSize(280, 180);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
