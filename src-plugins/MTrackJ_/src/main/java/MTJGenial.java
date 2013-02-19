import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ij.*;
import ij.gui.*;

public final class MTJGenial extends Dialog implements ActionListener, FocusListener, ItemListener, KeyListener, WindowListener {
	
	private Vector stringField, checkbox, choice;
	private Vector defaultValues,defaultText;
	private Component theLabel;
	private Button cancel, okay;
	private boolean wasCanceled;
	private int y;
	private int sfIndex, cbIndex, choiceIndex;
	private GridBagLayout grid;
	private GridBagConstraints c;
	private int topInset, leftInset, bottomInset;
	private boolean customInsets;
	private final static Font font = new Font("Dialog",Font.PLAIN,11);
	private static Point lastpos = null;
	private Point topleft = null;
	
	public MTJGenial(final String title, final Frame parent) {
		
		super(parent==null?new Frame():parent, title, true);
		if (Prefs.blackCanvas) {
			setForeground(SystemColor.controlText);
			setBackground(SystemColor.control);
		}
		grid = new GridBagLayout();
		c = new GridBagConstraints();
		setLayout(grid);
		addKeyListener(this);
		addWindowListener(this);
	}
	
	public MTJGenial(final String title, final Frame parent, final Point topleft) {
		
		this(title,parent);
		this.topleft = topleft;
	}
	
	private Label makeLabel(String label) {
		
		if (IJ.isMacintosh()) label += " ";
		final Label lab = new Label("  "+label);
		lab.setFont(font);
		return lab;
	}
	
	public void addStringField(String label, String defaultText) {
		
		addStringField(label,defaultText,8,null);
	}
	
	public void addStringField(String label, String defaultText, int columns) {
		
		addStringField(label,defaultText,columns,null);
	}
	
	public void addStringField(String label, String defaultText, int columns, String units) {
		
		Label theLabel = makeLabel(label);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		if (stringField == null) {
			stringField = new Vector(4);
			c.insets = getInsets(10,0,2,0);
		} else c.insets = getInsets(1,0,2,0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		TextField tf = new TextField(defaultText,columns);
		tf.setFont(font);
		tf.addActionListener(this);
		tf.addFocusListener(this);
		tf.addKeyListener(this);
		tf.setEditable(true);
		stringField.addElement(tf);
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		if (units == null) units = "";
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		panel.add(tf);
		final Label unitslabel = new Label(" "+units);
		unitslabel.setFont(font);
		panel.add(unitslabel);
		grid.setConstraints(panel,c);
		add(panel);    		
		y++;
	}
	
	public void addCheckbox(String label, boolean defaultValue) {
		
		String label2 = label;
		if (label2.indexOf('_')!=-1) label2 = label2.replace('_', ' ');
		if (checkbox==null) {
			checkbox = new Vector(4);
			c.insets = getInsets(10,10,0,0);
		} else c.insets = getInsets(0,10,0,0);
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		Checkbox cb = new Checkbox(" "+label2);
		cb.setFont(font);
		grid.setConstraints(cb, c);
		cb.setState(defaultValue);
		cb.addItemListener(this);
		cb.addKeyListener(this);
		add(cb);
		checkbox.addElement(cb);
		y++;
	}
	
	public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues) {
		
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(rows,columns,5,0));
		int startCBIndex = cbIndex;
		int i1 = 0;
		int[] index = new int[labels.length];
		if (checkbox==null) checkbox = new Vector(12);
		boolean addListeners = labels.length<=4;
		for (int row=0; row<rows; row++) {
			for (int col=0; col<columns; col++) {
				int i2 = col*rows+row;
				if (i2>=labels.length) break;
				index[i1] = i2;
				String label = labels[i1];
				if (label.indexOf('_')!=-1) label = label.replace('_', ' ');
				Checkbox cb = new Checkbox(" "+label);
				cb.setFont(font);
				checkbox.addElement(cb);
				cb.setState(defaultValues[i1]);
				if (addListeners) cb.addItemListener(this);
				panel.add(cb);
				i1++;
			}
		}
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = getInsets(10,0,0,0);
		grid.setConstraints(panel, c);
		add(panel);
		y++;
	}
	
	public void addChoice(String label, String[] items, String defaultItem) {
		
		addChoice(label,items,defaultItem,null,null);
	}
	
	public void addChoice(String label, String[] items, String defaultItem, String units) {
		
		addChoice(label,items,defaultItem,units,null);
	}
	
	public void addChoice(String label, String[] items, String defaultItem, Dimension dims) {
		
		addChoice(label,items,defaultItem,null,dims);
	}
	
	public void addChoice(String label, String[] items, String defaultItem, String units, Dimension dims) {
		
		Label theLabel = makeLabel(label);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		if (choice == null) {
			choice = new Vector(4);
			c.insets = getInsets(2,0,2,0);
		} else c.insets = getInsets(1,0,2,0);
		grid.setConstraints(theLabel,c);
		add(theLabel);
		Choice thisChoice = new Choice();
		if (dims != null) thisChoice.setPreferredSize(dims);
		thisChoice.setFont(font);
		thisChoice.addKeyListener(this);
		thisChoice.addItemListener(this);
		for (int i=0; i<items.length; i++) thisChoice.addItem(items[i]);
		thisChoice.select(defaultItem);
		choice.addElement(thisChoice);
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		if (units == null) units = "";
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		panel.add(thisChoice);
		final Label unitslabel = new Label(" "+units);
		unitslabel.setFont(font);
		panel.add(unitslabel);
		grid.setConstraints(panel,c);
		add(panel);    		
		y++;
	}
	
	public void addMessage(String text) {
		
		if (text.indexOf('\n')>=0) theLabel = new MultiLineLabel(text);
		else theLabel = new Label(text);
		theLabel.setFont(font);
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = getInsets(text.equals("")?0:10,10,0,0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		y++;
	}
	
	public void addPanel(Panel panel) {
		
		addPanel(panel,GridBagConstraints.WEST,new Insets(5,0,0,0));
	}
	
	public void addPanel(Panel panel, int contraints, Insets insets) {
		
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = contraints;
		c.insets = insets;
		grid.setConstraints(panel, c);
		add(panel);
		y++;
	}
	
	public void setInsets(int top, int left, int bottom) {
		
		topInset = top;
		leftInset = left;
		bottomInset = bottom;
		customInsets = true;
	}
	
	private Insets getInsets(int top, int left, int bottom, int right) {
		
		if (customInsets) {
			customInsets = false;
			return new Insets(topInset,leftInset,bottomInset,0);
		} else
			return new Insets(top,left,bottom,right);
	}
	
	public boolean wasCanceled() {
		
		return wasCanceled;
	}
	
	public String getNextString() {
		
		String theText;
		if (stringField==null) return "";
		TextField tf = (TextField)(stringField.elementAt(sfIndex));
		theText = tf.getText();
		sfIndex++;
		return theText;
	}
	
	public boolean getNextBoolean() {
		
		if (checkbox==null) return false;
		Checkbox cb = (Checkbox)(checkbox.elementAt(cbIndex));
		boolean state = cb.getState();
		cbIndex++;
		return state;
	}
	
	public String getNextChoice() {
		
		if (choice==null) return "";
		Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
		String item = thisChoice.getSelectedItem();
		choiceIndex++;
		return item;
	}
	
	public int getNextChoiceIndex() {
		
		if (choice==null) return -1;
		Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
		int index = thisChoice.getSelectedIndex();
		choiceIndex++;
		return index;
	}
	
	public void showDialog() {
		
		sfIndex = 0;
		cbIndex = 0;
		choiceIndex = 0;
		if (stringField!=null) {
			TextField tf = (TextField)(stringField.elementAt(0));
			tf.selectAll();
		}
		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		cancel = new Button("Cancel");
		cancel.setFont(font);
		cancel.addActionListener(this);
		cancel.addKeyListener(this);
		okay = new Button("   OK   ");
		okay.setFont(font);
		okay.addActionListener(this);
		okay.addKeyListener(this);
		if (IJ.isMacintosh()) {
			buttons.add(cancel);
			buttons.add(okay);
		} else {
			buttons.add(okay);
			buttons.add(cancel);
		}
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 2;
		c.insets = new Insets(15,0,0,0);
		grid.setConstraints(buttons, c);
		add(buttons);
		pack();
		if (topleft != null) setLocation(topleft);
		else if (lastpos != null) setLocation(lastpos);
		else GUI.center(this);
		setResizable(false);
		setVisible(true);
	}
	
	public Vector getStringFields() { return stringField; }
	
	public Vector getCheckboxes() { return checkbox; }
	
	public Vector getChoices() { return choice; }
	
	public Component getMessage() { return theLabel; }
	
	public void actionPerformed(ActionEvent e) {
		
		Object source = e.getSource();
		if (source==okay || source==cancel) {
			wasCanceled = source==cancel;
			closeDialog();
		}
	}
	
	void closeDialog() {
		
		setVisible(false);
		dispose();
	}
	
	public void itemStateChanged(ItemEvent e) { }
	
	public void focusGained(FocusEvent e) {
		
		Component c = e.getComponent();
		if (c instanceof TextField)
			((TextField)c).selectAll();
	}
	
	public void focusLost(FocusEvent e) {
		
		Component c = e.getComponent();
		if (c instanceof TextField)
			((TextField)c).select(0,0);
	}
	
	public void keyPressed(KeyEvent e) { 
		
		int keyCode = e.getKeyCode(); 
		if (keyCode == KeyEvent.VK_ESCAPE) {
			wasCanceled = true;
			closeDialog();
			IJ.resetEscape();
		} else if (keyCode == KeyEvent.VK_ENTER) {
			if (e.getSource() == cancel)
				wasCanceled = true;
			closeDialog();
		}
	}
	
	public void keyReleased(KeyEvent e) { }
	
	public void keyTyped(KeyEvent e) { }
	
	public Insets getInsets() {
		
		Insets i = super.getInsets();
		return new Insets(i.top+10,i.left+10,i.bottom+10,i.right+10);
	}
	
	public void paint(Graphics g) {
		
		super.paint(g);
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		if (lastpos == null) lastpos = new Point();
		lastpos.x = e.getWindow().getX();
		lastpos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) {
		
		wasCanceled = true;
		closeDialog();
	}
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}
