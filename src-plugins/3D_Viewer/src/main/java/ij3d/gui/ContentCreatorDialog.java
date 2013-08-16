package ij3d.gui;

import ij.util.Java2;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;

import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import javax.swing.JFileChooser;
import javax.swing.filechooser.*;
import java.awt.event.*;
import java.awt.*;

import java.util.Vector;

import java.io.File;

import javax.vecmath.Color3f;

public class ContentCreatorDialog {

	private Color3f color;
	private int threshold;
	private String name;
	private int resamplingFactor;
	private boolean[] channels;
	private int timepoint;
	private int type;
	private ImagePlus image;
	private File file;
	private Content content;

	private GenericDialog gd;

	public Content showDialog(final Image3DUniverse univ, final ImagePlus imp, final File fi) {
		if(fi != null)
			this.file = fi;

		// setup default values
		int img_count = WindowManager.getImageCount();
		Vector windows = new Vector();
		if(file != null) {
			windows.add(file.getAbsolutePath());
		} else {
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus ip = WindowManager.getImage(id);
				if(ip != null && ip.getWidth() != 0 && !ip.getTitle().equals("3d"))
					 windows.add(ip.getTitle());
			}
		}
		if (windows.size() == 0) {
			IJ.error("Need an image!");
			return null;
		}
		final String[] images = new String[windows.size()];
		windows.toArray(images);
		if(file != null)
			name = file.getName();
		else
			name = imp == null ? images[0] : imp.getTitle();
		String[] types = new String[] {
			"Volume", "Orthoslice", "Surface", "Surface Plot 2D", "Multiorthoslices"};
		type = type < 0 ? 0 : type;
		threshold = type == Content.SURFACE ? 50 : 0;
		resamplingFactor = 2;

		// create dialog
		gd = new GenericDialog("Add ...", univ.getWindow());
		gd.addChoice("Image", images, name);
		gd.addStringField("Name", getUniqueContentLabel(univ, name), 10);
		gd.addChoice("Display as", types, types[type]);
		gd.addChoice("Color", ColorTable.colorNames,
						ColorTable.colorNames[0]);
		gd.addNumericField("Threshold", threshold, 0);
		gd.addNumericField("Resampling factor", resamplingFactor, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3,
				new String[] {"red", "green", "blue"},
				new boolean[]{true, true, true});
		gd.addNumericField("Start at time point",
				univ.getCurrentTimepoint(), 0);

		// automatically set threshold if surface is selected
		final TextField th = (TextField)gd.getNumericFields().get(0);
		final Choice di = (Choice)gd.getChoices().get(1);
		di.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(di.getSelectedIndex() == Content.SURFACE)
					th.setText(Integer.toString(50));
				else
					th.setText(Integer.toString(0));
			}
		});
		// automatically update name if a different image is selected
		final Choice im = (Choice)gd.getChoices().get(0);
		final TextField na = (TextField)gd.getStringFields().get(0);
		im.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int idx = im.getSelectedIndex();
				String name;
				if(file == null || idx > 0)
					name = im.getSelectedItem();
				else
					name = file.getName();
				na.setText(getUniqueContentLabel(univ, name));
			}
		});
		gd.showDialog();
		if(gd.wasCanceled())
			return null;

		String imChoice = gd.getNextChoice();
		boolean fromFile = file != null &&
			im.getSelectedIndex() == 0;
		if(!fromFile) {
			image = WindowManager.getImage(imChoice);
			file = null;
		}

		name = gd.getNextString();
		type = gd.getNextChoiceIndex();
		color = ColorTable.getColor(gd.getNextChoice());
		threshold = (int)gd.getNextNumber();
		resamplingFactor = (int)gd.getNextNumber();
		channels = new boolean[] { gd.getNextBoolean(),
					gd.getNextBoolean(),
					gd.getNextBoolean() };
		timepoint = (int)gd.getNextNumber();

		if(resamplingFactor < 1) {
			IJ.error("Resampling factor must be greater than 0");
			return null;
		}

		if(univ.contains(name)) {
			IJ.error("Could not add new content. A content with " +
				"name \"" + name + "\" exists already.");
			return null;
		}

		return createContent();
	}

	// Avoid suggesting names that are taken already
	protected String getUniqueContentLabel(Image3DUniverse univ, String name) {
		if (!univ.contains(name))
			return name;
		for (int nr = 2; ; nr++)
			if (!univ.contains(name + "-" + nr))
				return name + "-" + nr;
	}

	private Content createContent() {
		ImagePlus[] imps = file != null ?
			ContentCreator.getImages(file) :
			ContentCreator.getImages(image);

		if(imps == null || imps.length == 0)
			return null;

		// check image type
		int imaget = imps[0].getType();
		if(imaget != ImagePlus.GRAY8 &&
			imaget != ImagePlus.COLOR_256 &&
			imaget != ImagePlus.COLOR_RGB) {
			// TODO correct message
			if(IJ.showMessageWithCancel("Convert...",
				"8-bit or RGB image required. Convert?")) {
				for(ImagePlus ip : imps)
					ContentCreator.convert(ip);
			} else {
				return null;
			}
		}

		Content c = ContentCreator.createContent(
			name, imps, type, resamplingFactor,
			timepoint, color, threshold,
			channels);
		return c;
	}

	public File getFile() {
		return file;
	}

	public ImagePlus getImage() {
		return image;
	}

	public Color3f getColor() {
		return color;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public int getResamplingFactor() {
		return resamplingFactor;
	}

	public int getThreshold() {
		return threshold;
	}

	public int getTimepoint() {
		return timepoint;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}
}
