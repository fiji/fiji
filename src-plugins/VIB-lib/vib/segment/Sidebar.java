package vib.segment;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.io.File;
import java.io.FileInputStream;

import java.util.Vector;

import ij.*;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;
import ij.gui.Roi;

import amira.AmiraParameters;
import vib.InterpolatedImage;

/**
 * Sidebar: 
 * This class builds all the interface of the SegmentationEditor. It handles the
 * listeners too.
 * 
 * @author Francois KUSZTOS
 * @version 5
 */
public class Sidebar extends Panel implements CustomCanvas.CanvasListener, ItemListener {

	private CustomCanvas cc;
	
	private Font font = new Font("Helvetica", Font.PLAIN, 10);

	private GridBagConstraints constr;
	private Label lZoomLevel;
	private ImageButton bZoomPlus, bZoomMinus;
	private ImageButton bPlus, bMinus, bInterpolate;
	private ImageButton bThreshold, bOpen, bClose;
	private Checkbox check3d;
	private Choice labelImagesChoice;
	private Vector labelImages;


	private ActionListener al;
	private MaterialList materials;
	private Vector defaultMaterials;
	private boolean currentLabelsAreNew = false;
	
	
	public Sidebar(CustomCanvas cc, ActionListener al) {
		this.cc = cc;
		this.al = al;

		cc.addCanvasListener(this);
			
		setLayout(new GridBagLayout());

		constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.anchor = GridBagConstraints.WEST;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.insets = new Insets(0, 5, 0, 5);
		
		addLabel("Label sets:");
		add(addLabelImageChoice(), constr);
		
		addLabel("Labels:");
		materials = new MaterialList(cc);
		add(materials, constr);

		addZoom();
		addSelection();
		addTools();

		this.itemStateChanged(null);
	}

	public void updateLZoomLevel(double magnification) {
		lZoomLevel.setText(String.valueOf(magnification));
	}

	public boolean is3d() {
		return check3d.getState();
	}
	
	public int currentMaterialID() {
		return materials.currentMaterialID();
	}

	public MaterialList getMaterials() {
		return materials;
	}

	public void magnificationChanged(double d) {
		updateLZoomLevel(d);
	}

	public void setBackground(Color bg) {
		super.setBackground(bg);
		materials.setBackground(bg);
	}

	public void setLabelImage(ImagePlus image) {
		if (image == null) {
			image = InterpolatedImage.cloneDimensionsOnly(
					cc.getImage(),ImagePlus.COLOR_256)
						.getImage();
			// Make sure the new labels image has a helpful title:
			String originalTitle = cc.getImage().getTitle();
			String newName = "New Labels";
			if (originalTitle.length() > 0) {
				int lastDot = originalTitle.lastIndexOf('.');
				if( lastDot >= 0 )
					newName = originalTitle.substring(0,lastDot)+".labels";
				else
					newName = originalTitle+".labels";
			} else
				image.setTitle(newName);
			image.setTitle(newName);
			// TODO: get initial parameters
		}
		cc.setLabels(image);
		cc.repaint();
		if (materials != null) {
			materials.initFrom(image);
			materials.repaint();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		int selected = labelImagesChoice.getSelectedIndex();
		if (selected < labelImages.size()) {
			Object image = labelImages.get(selected);
			setLabelImage((ImagePlus)image);
			currentLabelsAreNew = false;
			cc.requestFocus();
			return;
		}
		selected -= labelImages.size();
		String materials = (String)defaultMaterials.get(selected);
		if (!currentLabelsAreNew)
			setLabelImage(null);
		ImagePlus labels = cc.getLabels();
		AmiraParameters params = new AmiraParameters(materials);
		params.setParameters(labels);
		// force repaint
		setLabelImage(labels);
		this.materials.params = params;
		currentLabelsAreNew = true;
		cc.requestFocus();
	}
	
	private ImageButton addImageButton(String path, ActionListener l) {
		URL url;
		Image img = null;
		try {
			url = getClass().getResource("icons/" + path);
			img = createImage((ImageProducer)url.getContent());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch(Exception e) { 
			e.printStackTrace(); }
		if (img == null)
			throw new RuntimeException("Image not found: " + path);
		ImageButton button = new ImageButton(img);
		button.addActionListener(l);
		add(button, constr);
		return button;
	}

	private Label addLabel(String txt) {
		constr.insets = new Insets(10, 5, 0, 5);
		Label label = new Label(txt);
		label.setFont(font);
		add(label, constr);
		constr.insets = new Insets(0, 5, 0, 5);
		return label;
	}
	
	private Choice addLabelImageChoice() {
		labelImagesChoice = new Choice();
		labelImages = new Vector();
		defaultMaterials = new Vector();
		int count = WindowManager.getWindowCount();

		// TODO: add image listener
		for (int i = 0; i < count; i++) {
			ImagePlus image = WindowManager.getImage(i + 1);
			if (!AmiraParameters.isAmiraLabelfield(image) ||
					image == cc.getImage() ||
					image.getWidth() != 
					cc.getImage().getWidth() ||
					image.getHeight() != 
					cc.getImage().getHeight() ||
					image.getStack().getSize() != 
					cc.getImage().getStack().getSize())
				continue;
			labelImagesChoice.add(image.getTitle());
			labelImages.add(image);
		}
		labelImagesChoice.add("<new>");
		defaultMaterials.add("Parameters {\n"
				                + "\tMaterials {\n"
				                + "\t\tExterior {\n"
				                + "\t\t\tColor 0.0 0.0 0.0\n"
				                + "\t\t}\n"
				                + "\t\tInterior {\n"
				                + "\t\t\tColor 1.0 0.0 0.0\n"
				                + "\t\t}\n"
				                + "\t}\n"
				                + "}\n");

//		URL materials = getClass().getResource("materials/");
//		File folder = materials != null ?
//			new File(materials.getPath()).getParentFile() : null;
//		if (folder != null && folder.isDirectory()) {
//			String[] files = folder.list();
//			for (int i = 0; i < files.length; i++) {
//				String path = materials.getPath() + File.separator + files[i];
//				String contents = readFile(path);
//				if (contents != null) {
//					defaultMaterials.add(contents);
//					labelImagesChoice.add(files[i]);
//				}
//			}
//		}

		// For the moment, just load the CompactStandard
		URL materials = getClass().getResource(
					"materials/CompactStandard");
		String contents = readURL(materials);
		defaultMaterials.add(contents);
		labelImagesChoice.add("CompactStandard");

		labelImagesChoice.addItemListener(this);
		return labelImagesChoice;
	}
	

	private void addZoom() {
		addLabel("Zoom:");
		
		constr.gridwidth = 1;
		bZoomPlus = addImageButton("iconZoomPlus.png", al);
		bZoomPlus.setActionCommand("zoomin");
		bZoomMinus = addImageButton("iconZoomMinus.png", al);
		bZoomMinus.setActionCommand("zoomout");
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.NONE;
		lZoomLevel = addLabel(String.valueOf(cc.getMagnification()));
		constr.fill = GridBagConstraints.BOTH;
	}
	
	private void addSelection() {
		constr.gridwidth = GridBagConstraints.REMAINDER;
		addLabel("Selection:");
		
		constr.gridwidth = 1;
		bPlus = addImageButton("iconPlus.png", al);
		bPlus.setActionCommand("plus");
		bMinus = addImageButton("iconMinus.png", al);
		bMinus.setActionCommand("minus");
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.NONE;
		check3d = new Checkbox("3d", false);
		add(check3d, constr);
		constr.fill = GridBagConstraints.BOTH;
	}

	private void addTools() {
		constr.gridwidth = GridBagConstraints.REMAINDER;
		addLabel("Tools:");
		
		constr.gridwidth = 1;
		bInterpolate = addImageButton("iconInterpolate.png", al);
		bInterpolate.setActionCommand("interpolate");
		bThreshold = addImageButton("iconThreshold.png", al);
		bThreshold.setActionCommand("threshold");
		bOpen = addImageButton("iconOpen.png", al);
		bOpen.setActionCommand("open");
		bClose = addImageButton("iconClose.png", al);
		bClose.setActionCommand("close");
		constr.fill = GridBagConstraints.BOTH;
	}

	private String readURL(URL url) {
		StringBuffer buffer = new StringBuffer();
		try {
			java.io.InputStream input = url.openStream();
			int c;
			while((c = input.read()) != -1) {
				buffer.append((char)c);
			}
			return new String(buffer);
		} catch(Exception e) {
			return null;
		}
	}
}

