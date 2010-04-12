package vib;
/**
 * @author Benjamin Schmid
 * 
 * @date 07.08.2006
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

import java.awt.BorderLayout;
import java.awt.ScrollPane;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class Points_Dialog extends Dialog implements ActionListener, 
					PlugIn, PointList.PointListListener {
	
	private PointsPanel panel;
	private final PopupMenu popup = createPopup();

	private Calibration cal;
	private ImagePlus imp;	
	private PointList points;
	private BenesNamedPoint current;

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if(imp == null) {
			IJ.error("There's no image to annotate.");
			return;
		}
		new Points_Dialog(imp);
	}

	public Points_Dialog(ImagePlus imp) {
		this(imp, null);
	}
	
	public Points_Dialog(ImagePlus imp, PointList points) {
		super(IJ.getInstance(), "Marking up: " + imp.getTitle(), false);
		
		this.imp = imp;
		this.cal = imp.getCalibration();
		this.points = points;
		if(this.points == null)
			this.points = new PointList();
		this.points.addPointListListener(this);
		
		add(popup);
		setLayout(new BorderLayout());
		
		add(new Label("Mark the current point selection as:"),
			BorderLayout.NORTH);
		
		panel = new PointsPanel();
		panel.update();
		ScrollPane scroll = new ScrollPane();
		scroll.add(panel);
		add(scroll,BorderLayout.CENTER);
		
		
		Panel buttonsPanel = new Panel();
		Button button = new Button("Load");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Add point");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Save");
		button.addActionListener(this);
		buttonsPanel.add(button);	
		button = new Button("Reset");
		button.addActionListener(this);
		buttonsPanel.add(button);
		button = new Button("Close");
		button.addActionListener(this);
		buttonsPanel.add(button);	
		add(buttonsPanel,BorderLayout.SOUTH);
	
		GUI.center(this);
		pack();
		setVisible(true);
	}
	
	private PopupMenu createPopup(){
		PopupMenu popup = new PopupMenu();
		MenuItem mi = new MenuItem("Rename point");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Remove point");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Reset point");
		mi.addActionListener(this);
		popup.add(mi);
		return popup;
	}

	public PointList getPointList() {
		return points;
	}

	public void setPoints(PointList points) {
		this.points = points;
		panel.update();
	}
	
	public void addEmptyPoint(){
		BenesNamedPoint p = new BenesNamedPoint(
						"point" + points.size());
		points.add(p);
	}
	
	public void removePoint(BenesNamedPoint p){
		points.remove(p);
	}
	
	public void renamePoint(BenesNamedPoint p){
		String name = IJ.getString("New name", p.name);
		if(name.equals(""))
			return;
		points.rename(p, name); 
	}

	public void resetPoint(BenesNamedPoint p) {
		p.set = false;
		panel.update();
	}

	public void resetAll() {
		for(BenesNamedPoint p : (Iterable<BenesNamedPoint>)points) {
			p.set = false;
		}
		panel.update();
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("Close")){
			dispose();
		} else if (command.equals("Save")) {
			save();
		} else if (command.equals("Reset")) {
			resetAll();
		} else if (command.equals("Add point")){
			addEmptyPoint();
		} else if (command.equals("Rename point")){
			renamePoint(current);
		} else if (command.equals("Remove point")){
			removePoint(current);
		} else if (command.equals("Reset point")){
			resetPoint(current);
		} else if (command.equals("Load")){
			load();
		} 
	}

	public void save() {
		FileInfo info = imp.getOriginalFileInfo();
		if(info == null) {
			IJ.error("There's no original file name that " + 
				"these points refer to.");
			return;
		}
		String fileName = info.fileName;
		String directory = info.directory;
		IJ.showStatus("Saving point annotations to "
			+ directory + fileName);
		points.save(directory, fileName);
		IJ.showStatus("Saved point annotations.");
	}

	public void showPoint(BenesNamedPoint p) {
		if(imp.getWindow() == null)
			return;
		if(!p.set){
			IJ.error("Point is not set yet");
			return;
		}
		int slice = (int)(p.z / cal.pixelDepth);
		if(slice < 0)
			slice = 0;
		if(slice > imp.getStackSize())
			slice = imp.getStackSize()-1;
		imp.setSlice(slice+1);
		Roi roi	= new PointRoi((int)(p.x / cal.pixelWidth),
				       (int)(p.y / cal.pixelHeight),
				       imp);
		imp.setRoi(roi);
	}
	
	public void load() {
		PointList newNamedPoints = PointList.load(imp);
		if(newNamedPoints==null)
			return;

		for(BenesNamedPoint current : newNamedPoints) {
			boolean foundName = false;
			for(BenesNamedPoint p : points) {
				if (current.name.equals(p.name)) {
					p.set(current.x, current.y, current.z);
					p.set = true;
					foundName = true;
				}
			}
			if (!foundName)
				points.add(current);
		}
	}
	
	public void mark(BenesNamedPoint point) {
		Roi roi = imp.getRoi();
		if (roi != null && roi.getType() == Roi.POINT) {
			Polygon p = roi.getPolygon();
			if(p.npoints > 1) {
				IJ.error("You can only have one point "
					+ "selected to mark.");
				return;
			}

			double x = p.xpoints[0] * cal.pixelWidth;
			double y = p.ypoints[0] * cal.pixelHeight;
			double z = (imp.getCurrentSlice()-1) * cal.pixelDepth;

			points.placePoint(point, x, y, z);
		} else {
			IJ.error("You must have a current point selection in "+
				 imp.getTitle()+" in order to mark points.");
		}
	}
	// PointListListener interface
	public void added(BenesNamedPoint p) {
		panel.update();
	}

	public void removed(BenesNamedPoint p) {
		panel.update();
	}

	public void renamed(BenesNamedPoint p) {
		panel.update();
	}

	public void moved(BenesNamedPoint p) {
		panel.update();
	}

	public void highlighted(BenesNamedPoint p) {}

	public void reordered() {
		panel.update();
	}

	private class PointsPanel extends Panel {
		public void update(){
			if(points.size() == 0)
				addEmptyPoint();
			panel.removeAll();
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			for (BenesNamedPoint p : points)
				addRow(p, c);
			pack();
		}
		
		private void addRow(final BenesNamedPoint p, 
						GridBagConstraints c) {
			c.gridx = 0;
			c.gridy = GridBagConstraints.RELATIVE;
			c.anchor = GridBagConstraints.LINE_START;			
			final Button button = new Button(p.name);
			button.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					if(e.isPopupTrigger()){
						current = p;
						popup.show(button, 
							e.getX(),e.getY());
					}
				}
				public void mouseReleased(MouseEvent e){
					if(e.isPopupTrigger()){
						current = p;
						popup.show(button,
							e.getX(),e.getY());
					}
				}
			});
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					mark(p);
				}
			});
			panel.add(button,c);
			
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 1;
			Label coordinateLabel = 
				p.set ? new Label(p.coordinatesAsString())
						: new Label("     <unset>     ");
			panel.add(coordinateLabel,c);
			
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 2;
			Button showB = new Button("Show");
			showB.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					showPoint(p);
				}
			});
			showB.setEnabled(p.set);
			panel.add(showB,c);
		}
	}
}
