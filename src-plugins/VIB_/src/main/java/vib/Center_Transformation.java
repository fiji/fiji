package vib;

import amira.AmiraParameters;
import amira.AmiraTable;

import ij.util.Tools;
import ij.gui.GenericDialog;
import ij.text.TextPanel;
import ij.WindowManager;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import math3d.Point3d;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

public class Center_Transformation implements PlugIn {
	
	public Center_Transformation(){}

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Center Transformation");
		AmiraParameters.addAmiraTableList(gd, "Model table");
		AmiraParameters.addAmiraTableList(gd, "Template table");
		gd.addNumericField("radius", 10, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String titleM = gd.getNextChoice();
		String titleT = gd.getNextChoice();
		float radius = (float)gd.getNextNumber();
		AmiraTable tableModel = (AmiraTable)WindowManager.getFrame(titleM);
		AmiraTable tableTemplate = (AmiraTable)WindowManager.getFrame(titleT);
		bestRigid(tableModel, tableTemplate);
	}
		
	public static FastMatrix bestRigid(AmiraTable tModel,AmiraTable tTemplate) {
		Point3d[] setModel = getList(tModel);
		Point3d[] setTemplate = getList(tTemplate);

		FastMatrix transform = FastMatrix.bestRigid(setModel,setTemplate,false);
		// write this into amira parameters
		String templName = tTemplate.getTitle();
		templName = templName.substring(0, templName.lastIndexOf('.'));
		String key = templName + "SCenterTransformation";
		String value = transform.toStringForAmira();
		tModel.getProperties().put(key, value);
		return transform;
	}

	public static Point3d[] getList(AmiraTable table) {
		TextPanel panel = table.getTextPanel();
		int count = panel.getLineCount();
		List<Point3d> points = new ArrayList<Point3d>();
		// start with 1, since 0 is 'Exterior'
		for (int i = 1; i < count; i++) {
			String[] line = Tools.split(panel.getLine(i), "\t");
			int voxelCount = Integer.parseInt(line[2]);
			// only take materials into account which have more
			// than 0 voxels (which are labelled)
			if(voxelCount == 0)
				continue;
			points.add(new Point3d(Double.parseDouble(line[4]),
					Double.parseDouble(line[5]),
					Double.parseDouble(line[6])));
		}
		return (Point3d[])points.toArray(new Point3d[]{});
	}
}
