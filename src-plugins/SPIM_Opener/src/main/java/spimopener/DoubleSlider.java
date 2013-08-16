package spimopener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;

public class DoubleSlider extends Panel implements TextListener, FocusListener {

	private static final long serialVersionUID = 1L;


	private TextField minTF = new TextField(4);
	private TextField maxTF = new TextField(4);
	private TextField incTF = new TextField(4);
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
		incTF.setText("1");
		add(incTF, c);
		valueChanged();
	}

	@Override
	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {}

	@Override
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

	public int getIncrement() {
		return Integer.parseInt(incTF.getText());
	}

	private static class DoubleSliderCanvas extends Component implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
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

		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseDragged(MouseEvent e) {
			double inc = (double) getWidth() / (max - min + 1);
			int newx = (int)Math.round(e.getX() / inc) + min;
			switch(dragging) {
				case DRAGGING_LEFT:  cmin = Math.max(min, Math.min(cmax, newx)); repaint(); slider.valueChanged(); break;
				case DRAGGING_RIGHT: cmax = Math.min(max, Math.max(cmin, newx - 1)); repaint(); slider.valueChanged(); break;
			}
		}

		@Override
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

		@Override
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
