package fiji.util;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
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
import javax.swing.event.EventListenerList;


public class ArrowOptionPanel extends javax.swing.JFrame {

	private static final long serialVersionUID = 1;
    /** A list of event listeners for this component. */
    private  EventListenerList listenerList = new EventListenerList();
    private boolean firingActionEvent = false;

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

	private static BasicStroke stroke;
	private static ArrowShape arrow;
	
	/*
	 * INNER CLASSES
	 */
	
	/**
	 * Canvas that will draw an example arrow as specified by the GUI of this frame. 
	 */
	private class ArrowExampleCanvas extends Canvas {
		private static final long serialVersionUID = 1L;
		Point2D start, end;
		public void paint(Graphics g) {
			super.paint(g);
			start 	= new Point2D.Double(jPanelDrawArea.getWidth()*0.25, jPanelDrawArea.getHeight()/2.0);
			end 	= new Point2D.Double(jPanelDrawArea.getWidth()*0.75, jPanelDrawArea.getHeight()/2.0);
			arrow = new ArrowShape((ArrowShape.ArrowStyle) jComboBoxHeadStyle.getSelectedItem());
			arrow.setStartPoint(start);
			arrow.setEndPoint(end);
			try {
				final double length = Double.parseDouble(jTextFieldHeadLength.getText());
				final float width = Float.parseFloat(jTextFieldArrowThickness.getText());
				arrow.setLength(length);				
				stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
				        RenderingHints.VALUE_ANTIALIAS_ON);
//				g2.setStroke(stroke);
//				g2.draw(arrow);
				Shape shape = stroke.createStrokedShape(arrow);
				Area area = new Area(shape); // this will get us the thick outline
				area.add(new Area(arrow)); // to fill inside
				g2.draw(area);
//				if (jCheckBoxFillArrow.isSelected()) {	g2.fill(arrow);		}
				if (jCheckBoxFillArrow.isSelected()) {	g2.fill(area);		}
				// Fire a property change
				fireActionEvent();
			} catch (NumberFormatException nfe) { }
		}
		
	}
	
	/**
	 * Change listener that will listen to change in slider value and will set the corresponding {@link JTextField}
	 * value accordingly
	 */
	private class SliderChangeListener implements ChangeListener {
		private JTextField text_field;
		public SliderChangeListener(JTextField _text_field) {
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
	
	/**
	 * MouseWheellistener that will listen to mouse scroll over a slider and update the slider value
	 * accordingly.
	 */
	private class SliderMouseWheelListener implements MouseWheelListener {
		private JSlider slider;
		public SliderMouseWheelListener(JSlider _slider) {
			this.slider = _slider;
		}
				public void mouseWheelMoved(MouseWheelEvent e) {
			int steps = e.getWheelRotation();
			slider.setValue(slider.getValue()+steps);
		}
	}
		
	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * Instantiates the config panel with using settings from arguments. 
	 */
	public ArrowOptionPanel(ArrowShape _arrow, BasicStroke _stroke) {
		super();
		initGUI();
		stroke = _stroke;
		arrow = _arrow;
		jComboBoxHeadStyle.setSelectedItem(arrow.getStyle());
		jTextFieldArrowThickness.setText(String.format("%.0f", stroke.getLineWidth()));
		jSliderArrowThickness.setValue((int) stroke.getLineWidth());
		jTextFieldHeadLength.setText(String.format("%.0f", arrow.getLength()));
		jSliderHeadLength.setValue((int) arrow.getLength() );
		
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
			} 
		};
		jComboBoxHeadStyle.addActionListener(al);
		jTextFieldArrowThickness.addActionListener(new TextFieldActionListener(jSliderArrowThickness));
		jTextFieldHeadLength.addActionListener(new TextFieldActionListener(jSliderHeadLength));
		jSliderArrowThickness.addChangeListener(new SliderChangeListener(jTextFieldArrowThickness));
		jSliderArrowThickness.addMouseWheelListener(new SliderMouseWheelListener(jSliderArrowThickness));
		jSliderHeadLength.addChangeListener(new SliderChangeListener(jTextFieldHeadLength));
		jSliderHeadLength.addMouseWheelListener(new SliderMouseWheelListener(jSliderHeadLength));
		jCheckBoxFillArrow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
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
		jSliderArrowThickness.addMouseWheelListener(new SliderMouseWheelListener(jSliderArrowThickness));
		jSliderHeadLength.addChangeListener(new SliderChangeListener(jTextFieldHeadLength));
		jSliderHeadLength.addMouseWheelListener(new SliderMouseWheelListener(jSliderHeadLength));
		jCheckBoxFillArrow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvasDrawingArea.paint(canvasDrawingArea.getGraphics());
			}
		});
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
	
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class,l);
	}
	
    /** Removes an <code>ActionListener</code>.
    *
    * @param l  the <code>ActionListener</code> to remove
    */
   public void removeActionListener(ActionListener l) {
	    listenerList.remove(ActionListener.class, l);
   }

   /**
    * Returns an array of all the <code>ActionListener</code>s added
    * to this JComboBox with addActionListener().
    *
    * @return all of the <code>ActionListener</code>s added or an empty
    *         array if no listeners have been added
    * @since 1.4
    */
   public ActionListener[] getActionListeners() {
       return (ActionListener[])listenerList.getListeners(
               ActionListener.class);
   }
   
  
   /*
    * GETTERS AND SETTERS
    */
   
   public BasicStroke getStroke() {	   return stroke;   }
   public double getLength() { return arrow.getLength(); }
   public ArrowShape.ArrowStyle getStyle() { return arrow.getStyle(); }   
	
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
						new DefaultComboBoxModel( ArrowShape.ArrowStyle.values() );
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

	/**
	 * Notifies all listeners that have registered interest for
	 * notification on this event type.
	 *  
	 * @see EventListenerList
	 */
	private void fireActionEvent() {
		if (!firingActionEvent) {
			// Set flag to ensure that an infinite loop is not created
			firingActionEvent = true;
			ActionEvent e = null;
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			long mostRecentEventTime = EventQueue.getMostRecentEventTime();
			int modifiers = 0;
			AWTEvent currentEvent = EventQueue.getCurrentEvent();
			if (currentEvent instanceof InputEvent) {
				modifiers = ((InputEvent)currentEvent).getModifiers();
			} else if (currentEvent instanceof ActionEvent) {
				modifiers = ((ActionEvent)currentEvent).getModifiers();
			}
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for ( int i = listeners.length-2; i>=0; i-=2 ) {
				if ( listeners[i]==ActionListener.class ) {
					// Lazily create the event:
					if ( e == null )
						e = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,
								"arrowPropertyChanged",
								mostRecentEventTime, modifiers);
					((ActionListener)listeners[i+1]).actionPerformed(e);
				}
			}
			firingActionEvent = false;
		}
	}

}
