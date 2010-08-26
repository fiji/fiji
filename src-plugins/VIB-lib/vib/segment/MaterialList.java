package vib.segment;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ColorChooser;
import ij.gui.GenericDialog;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.CheckboxMenuItem;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.naming.OperationNotSupportedException;

import amira.AmiraParameters;

public class MaterialList extends ScrollPane implements ActionListener, ItemListener {
	PopupMenu popup;

	ImagePlus labels;
	AmiraParameters params;
	CustomCanvas cc;

	Font font;
	int lineHeight, lineWidth;
	List list;
	private boolean[] locked;

	public MaterialList(CustomCanvas cc) {
		super();

		this.cc = cc;
		createPopup();
		font = new Font("Monospaced", Font.PLAIN, 12);
		lineHeight = font.getSize() + 1;
		lineWidth = 200;
		list = new List();
		add(list);
		locked = new boolean[0];
	}

	public MaterialList(ImagePlus ip, CustomCanvas cc) {
		this(cc);
		initFrom(ip);
	}

	public void releaseImage() {
		labels = null;
	}

	public void setBackground(Color color) {
		super.setBackground(color);
		list.setBackground(color);
	}

	private int getSelectedIndex() {
		return list.selectedIndex;
	}

	private void select(int index) {
		list.selectedIndex = index;
	}

	public int getItemCount() {
		return params.getMaterialCount();
	}

	public String getItem(int index) {
		return params.getMaterialName(index);
	}

	public String getSelectedItem() {
		return getItem(getSelectedIndex());
	}

	public int getIndexOf(String item) {
		for(int i = 0; i < getItemCount(); i++) {
			if(getItem(i).equals(item))
				return i;
		}
		return -1;
	}

	MenuItem remove, add, rename, color;
	CheckboxMenuItem lock;

	public void createPopup() {
		popup = new PopupMenu("");
		add = new MenuItem("Add Material");
		popup.add(add);
		remove = new MenuItem("Remove Material");
		// FIXME: there's no point in adding this option since
		// it just creates a RuntimeException at the moment.
		// popup.add(remove);
		rename = new MenuItem("Rename Material");
		popup.add(rename);
		color = new MenuItem("Change Color");
		popup.add(color);
		popup.addSeparator();
		lock = new CheckboxMenuItem("Locked");
		popup.add(lock);
		add.addActionListener(this);
		remove.addActionListener(this);
		rename.addActionListener(this);
		color.addActionListener(this);
		lock.addItemListener(this);
		add(popup);
	}

	public void initFrom(ImagePlus image) {
		labels = image;
		params = new AmiraParameters(image);
		if (params.getMaterialCount() == 0) {
			params.addMaterial("Exterior", 0,0,0);
			params.addMaterial("Interior", 1,0,0);
			params.setParameters(labels);
		}
		// initialize locked array, defaults to false
		locked = new boolean[params.getMaterialCount()];
		if (list != null) {
			list.invalidate();
			list.repaint();
		}
	}

	public void setMaterials(String materials) {
		params = new AmiraParameters(materials);
		locked = new boolean[params.getMaterialCount()];
		if (list != null) {
			list.invalidate();
			list.repaint();
		}
	}

	public void addMaterial() {
		int num = getItemCount();
		num++;
		params.addMaterial("Material" + num, 1,0,0); // TODO change color
		params.setParameters(labels);
		boolean[] newlocked = new boolean[num];
		System.arraycopy(locked, 0, newlocked, 0, locked.length);
		locked = newlocked;
		select(num-1);
		doLayout();
		list.repaint();
	}

	public void delMaterial() {
		int selected = getSelectedIndex();
		if (selected < 1) {
			IJ.error("Cannot delete first material!");
			return;
		}
		throw new RuntimeException("delete not yet implemented");
		// not forget to delete entry in locked-array
	}

	private void renameMaterial() {
		GenericDialog gd = new GenericDialog("Rename");
		gd.addStringField("name", getSelectedItem());
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		
		params.editMaterial(currentMaterialID(), gd.getNextString(),-1,-1,-1);
		params.setParameters(labels);
		list.repaint();
	}

	private void setColor() {
		int id = currentMaterialID();
		double[] values = params.getMaterialColor(id);
		Color current = new Color((float)values[0],
				(float)values[1], (float)values[2]);
		String name = params.getMaterialName(id) + " Color";
		ColorChooser chooser = new ColorChooser(name, current, false);
		Color changed = chooser.getColor();
		if (changed != null) {
			params.editMaterial(id, null, changed.getRed() / 255.0,
					changed.getGreen() / 255.0,
					changed.getBlue() / 255.0);
			params.setParameters(labels);
			labels.updateAndDraw();
			list.repaint();
			if (cc != null)
				cc.setLabels(labels);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == remove)
			delMaterial();
		else if (e.getSource() == add)
			addMaterial();
		else if (e.getSource() == rename)
			renameMaterial();
		else if (e.getSource() == color)
			setColor();
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == lock) {
			boolean b = lock.getState();
			locked[getSelectedIndex()] = b;
		}
	}

	public int currentMaterialID(){
		if(getSelectedIndex()==-1){
			return -1;
		}
		return params.getMaterialID(getSelectedItem());
	}
	
	public double[] currentMaterialColor(){
		int mID = currentMaterialID();
		if(mID == -1){
			return null;
		}
		return params.getMaterialColor(mID);
	}
	
	public int getDefaultMaterialID(){
		return params.getMaterialID(getItem(0));
	}

	public boolean isLocked(int matID) {
		return locked[getIndexOf(params.getMaterialName(matID))];
	}

	private class List extends Canvas {
		Color fgCol = Color.BLACK;
		Color bgCol = Color.LIGHT_GRAY;
		private int selectedIndex = 0;


		public List() {
			enableEvents(AWTEvent.MOUSE_EVENT_MASK |
					AWTEvent.KEY_EVENT_MASK);
		}

		public void setBackground(Color color) {
			this.bgCol = color;
		}

		public void processMouseEvent(MouseEvent e) {
			if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				selectedIndex = e.getY() / lineHeight;
				repaint();
			}
			if (e.isPopupTrigger()) {
				int index = e.getY() / lineHeight;
				if (index < getItemCount()) {
					selectedIndex = index;
					repaint();
					lock.setState(locked[index]);
					popup.show(this, e.getX(), e.getY());
				}
			}
		}

		public void processKeyEvent(KeyEvent e) {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				int code = e.getKeyCode();
				if (code == e.VK_UP && selectedIndex > 0)
					ensureVisible(--selectedIndex, true);
				else if (code == e.VK_DOWN && selectedIndex
						< getItemCount() - 1)
					ensureVisible(++selectedIndex, true);
			}
		}

		public void ensureVisible(int index, boolean repaintAnyway) {
			Point p = getScrollPosition();
			Dimension d = getViewportSize();
			if (p.y + d.height < (index + 1) * lineHeight ||
					p.y > index * lineHeight) {
				setScrollPosition(p.x, index * lineHeight);
				repaint();
			} else if (repaintAnyway)
				repaint();
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			g.setFont(font);
			for (int i = 0; i < getItemCount(); i++) {
				g.setColor(i == selectedIndex ? fgCol : bgCol);
				g.fillRect(0, i * lineHeight,
						lineWidth, lineHeight);
				double[] c = params.getMaterialColor(i);
				g.setColor(new Color((float)c[0], (float)c[1],
							(float)c[2]));
				g.fillRect(1, i * lineHeight + 1,
						lineHeight - 2, lineHeight - 2);
				g.setColor(i == selectedIndex ? bgCol : fgCol);
				g.drawString(getItem(i), lineHeight,
						(i + 1) * lineHeight - 1);
			}
			int y = lineHeight * getItemCount();
			if (y < getHeight()) {
				g.setColor(bgCol);
				g.fillRect(0, y, lineWidth, getHeight() - y);
			}
		}

		public Dimension getPreferredSize() {
			return new Dimension(lineWidth, getItemCount() *
					(font.getSize() + 1) + 1);
		}
	}
}
