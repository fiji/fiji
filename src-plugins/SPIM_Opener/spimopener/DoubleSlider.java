package spimopener;

import java.awt.*;
import java.awt.event.*;

public class DoubleSlider extends Panel implements TextListener, FocusListener {

	private TextField minTF = new TextField(4);
	private TextField maxTF = new TextField(4);
	private DoubleSliderCanvas slider;

	public DoubleSlider(int min, int max, int cmin, int cmax) {
		super();
		minTF.addTextListener(this);
		minTF.addFocusListener(this);
		maxTF.addTextListener(this);
		maxTF.addFocusListener(this);
		this.slider = new DoubleSliderCanvas(min, max, cmin, cmax, this);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 2, 0, 5);
		c.weightx = 1.0;
		add(slider, c);
		c.weightx = 0;
		add(minTF, c);
		add(maxTF, c);
		valueChanged();
	}

	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
		tf.selectAll();
	}

	public void focusLost(FocusEvent e) {}

	public void textValueChanged(TextEvent e) {
		try {
			int min = Integer.parseInt(minTF.getText());
			int max = Integer.parseInt(maxTF.getText());
			slider.setRange(min, max);
		} catch(NumberFormatException ex) {
		}
	}

	public void valueChanged() {
		minTF.setText(Integer.toString(slider.cmin));
		maxTF.setText(Integer.toString(slider.cmax));
	}
	public int getCurrentMin() {
		return slider.cmin;
	}

	public int getCurrentMax() {
		return slider.cmax;
	}

	private static class DoubleSliderCanvas extends Component implements MouseMotionListener, MouseListener {

		private int min, max;
		private int cmin, cmax;

		private int drawnMin, drawnMax;

		private int dragging = DRAGGING_NONE;

		private static final int DRAGGING_NONE  = 0;
		private static final int DRAGGING_LEFT  = 1;
		private static final int DRAGGING_RIGHT = 2;

		private DoubleSlider slider;

		public DoubleSliderCanvas(int min, int max, int cmin, int cmax, DoubleSlider slider) {
			this.min = min;
			this.max = max;
			this.cmin = cmin;
			this.cmax = cmax;
			this.slider = slider;
			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setPreferredSize(new Dimension(200, 30));
		}

		public void setRange(int cmin, int cmax) {
			this.cmin = cmin;
			this.cmax = cmax;
			repaint();
		}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}

		public void mouseDragged(MouseEvent e) {
			double inc = (double) getWidth() / (max - min + 1);
			int newx = (int)Math.round(e.getX() / inc) + min;
			switch(dragging) {
				case DRAGGING_LEFT:  cmin = Math.max(min, Math.min(cmax, newx)); repaint(); slider.valueChanged(); break;
				case DRAGGING_RIGHT: cmax = Math.min(max, Math.max(cmin, newx - 1)); repaint(); slider.valueChanged(); break;
			}
		}

		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			if(Math.abs(x - drawnMin) < 10) {
				setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
				dragging = DRAGGING_LEFT;
			} else if(Math.abs(x - drawnMax) < 10) {
				setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
				dragging = DRAGGING_RIGHT;
			} else {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				dragging = DRAGGING_NONE;
			}
		}

		public void paint(Graphics g) {
			int w = getWidth();
			int h = getHeight();

			double inc = (double) w / (max - min + 1);

			drawnMin = (int)Math.round((cmin - min) * inc);
			drawnMax = (int)Math.round((cmax + 1 - min) * inc);
			g.setColor(Color.GREEN);
			g.fillRect(drawnMin, 0, drawnMax - drawnMin, h);

			g.setColor(Color.BLACK);
			g.drawRect(0, 0, w-1, h-1);
		}
	}
}
