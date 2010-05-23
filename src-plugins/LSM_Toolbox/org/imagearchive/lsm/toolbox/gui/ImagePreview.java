package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;
import ij.io.RandomAccessStream;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.SystemColor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.Reader;

public class ImagePreview extends JPanel implements PropertyChangeListener {
	ImageIcon thumbnail = null;

	ImagePlus imp = null;

	JSlider slider = new JSlider(1, 1, 1);

	File file = null;

	org.imagearchive.lsm.reader.Reader reader;

	JPanel panel ;

	Color backgroundcolor = SystemColor.window;

	public ImagePreview(JFileChooser fc) {
		setPreferredSize(new Dimension(138, 50));
		fc.addPropertyChangeListener(this);
		//reader = ServiceMediator.getReader();
		reader = new org.imagearchive.lsm.reader.Reader();
		setLayout(new BorderLayout());
		add(slider, BorderLayout.NORTH);
		slider.setPaintLabels(true);
		addSliderListener();
		backgroundcolor = getBackground();
	}

	private void addSliderListener() {
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (imp != null) {
					imp.setSlice(slider.getValue());
					ImageIcon tmpIcon = new ImageIcon(imp.getProcessor()
							.createImage());
					if (tmpIcon != null) {
						if (tmpIcon.getIconWidth() > 128) {
							thumbnail = new ImageIcon(tmpIcon.getImage()
									.getScaledInstance(128, -1,
											Image.SCALE_DEFAULT));
						} else {
							thumbnail = tmpIcon;
						}
					}
					repaint();
				}
			}
		});
	}

	public void loadImage() {
		if (file == null) {
			thumbnail = null;
			imp = null;
			return;
		}
		ImageIcon tmpIcon = null;
		try {

			RandomAccessStream stream = new RandomAccessStream(
					new RandomAccessFile(file, "r"));
			if (reader.isLSMfile(stream)) {
				imp = reader.open(file.getParent(), file.getName(), false, true);
				if (imp != null) {
					slider.setValue(1);
					slider.setMaximum(imp.getNSlices());
					if (imp.getNSlices()==1){
						slider.setVisible(false);
					} else {
						slider.setLabelTable(slider.createStandardLabels(imp.getNSlices()-1));
						slider.setVisible(true);
					}
					tmpIcon = new ImageIcon(imp.getImage());
				} else {
					thumbnail = null;
					imp = null;
					return;
				}
			}
		} catch (IOException e) {
			thumbnail = null;
			imp = null;
			return;
		}

		if (tmpIcon != null) {
			if (tmpIcon.getIconWidth() > 128) {
				thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(
						128, -1, Image.SCALE_DEFAULT));
			} else {
				thumbnail = tmpIcon;
			}
		}
	}

	public void propertyChange(PropertyChangeEvent e) {
		boolean update = false;
		String prop = e.getPropertyName();

		// If the directory changed, don't show an image.
		if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
			file = null;
			update = true;

			// If a file became selected, find out which one.
		} else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
			file = (File) e.getNewValue();
			update = true;
		}

		// Update the preview accordingly.
		if (update) {
			thumbnail = null;
			if (isShowing()) {
				loadImage();
				repaint();
			}
		}
	}

	protected void paintComponent(Graphics g) {
		if (thumbnail == null) {
			loadImage();
		}
		if (thumbnail != null) {
			int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
			int y = getHeight() / 2 - thumbnail.getIconHeight() / 2
					+ slider.getHeight();

			if (y < 0) {
				y = 0;
			}

			if (x < 5) {
				x = 5;
			}

			g.setColor(backgroundcolor);
			g.fillRect(slider.getX(), slider.getY(), getWidth(), getHeight());
			thumbnail.paintIcon(this, g, x, y);
		}
	}
}