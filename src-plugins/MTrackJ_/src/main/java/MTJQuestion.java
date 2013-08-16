import java.awt.*;
import java.awt.event.*;
import ij.gui.*;

public final class MTJQuestion extends Dialog implements ActionListener, KeyListener, WindowListener {
	
	private final static Font font = new Font("Dialog",Font.PLAIN,11);
	
	private Button yes, no;
	private boolean wasNo = false;
	private Point topleft = null;
	
	public MTJQuestion(final String title, final Frame parent) {
		
		super(parent==null?new Frame():parent, title, true);
		setLayout(new BorderLayout(0,0));
		addKeyListener(this);
		addWindowListener(this);
	}
	
	public MTJQuestion(final String title, final Frame parent, final Point topleft) {
		
		this(title,parent);
		this.topleft = topleft;
	}
	
	public void addMessage(final String text) {
		
		final Label label = new Label("   "+text);
		label.setFont(font);
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,10,10));
		panel.add(label);
		add("North",panel);
	}
	
	public boolean wasNo() {
		
		return wasNo;
	}
	
	public void showDialog() {
		
		yes = new Button("  Yes  ");
		yes.setFont(font);
		yes.addActionListener(this);
		yes.addKeyListener(this);
		
		no = new Button("   No   ");
		no.setFont(font);
		no.addActionListener(this);
		no.addKeyListener(this);
		
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
		panel.add(yes);
		panel.add(no);
		add("Center",panel);
		
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,0,5));
		add("South",panel);
		
		pack();
		if (topleft != null) setLocation(topleft);
		else GUI.center(this);
		setResizable(true);
		setVisible(true);
	}
	
	public void actionPerformed(final ActionEvent e) {
		
		Object source = e.getSource();
		if (source==yes || source==no) {
			wasNo = (source==no);
			closeDialog();
		}
	}
	
	private void closeDialog() {
		
		setVisible(false);
		dispose();
	}
	
	public void keyPressed(final KeyEvent e) { 
		
		int keyCode = e.getKeyCode(); 
		if (keyCode == KeyEvent.VK_ESCAPE) {
			wasNo = true;
			closeDialog();
		} else if (keyCode == KeyEvent.VK_ENTER) {
			if (e.getSource() == no)
				wasNo = true;
			closeDialog();
		}
	}
	
	public void keyReleased(final KeyEvent e) { }
	
	public void keyTyped(final KeyEvent e) { }
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) { }
	
	public void windowClosing(final WindowEvent e) {
		
		wasNo = true;
		closeDialog();
	}
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}
