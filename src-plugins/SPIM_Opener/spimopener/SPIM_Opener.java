package spimopener;

import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;



public class SPIM_Opener implements PlugIn {

	@Override
	public void run(String args) {
		OpenDialog od = new OpenDialog("Open experiment xml", "");
		final String filename = od.getFileName();
		final String directory = od.getDirectory();
		if(filename == null || directory == null)
			return;

		SPIMExperiment tmp = null;

		try {
			tmp = new SPIMExperiment(directory + filename);
		} catch(Exception e) {
			IJ.error(e.getMessage());
			e.printStackTrace();
			return;
		}

		final SPIMExperiment exp = tmp;

		final OpenerGenericDialog gd = new OpenerGenericDialog("Open SPIM experiment");
		gd.addChoice("Sample", exp.samples);
		gd.addDoubleSlider("Time points", exp.timepointStart, exp.timepointEnd);
		gd.addChoice("Region", exp.regions);
		gd.addChoice("Angle", exp.angles);
		gd.addChoice("Channel", exp.channels);
		gd.addDoubleSlider("x Range", 0, exp.w - 1);
		gd.addDoubleSlider("y Range", 0, exp.h - 1);
		gd.addDoubleSlider("Planes",  exp.planeStart, exp.planeEnd);
		gd.addDoubleSlider("Frames",  exp.frameStart, exp.frameEnd);
		String[] dirs = new String[] {"x", "y", "frame", "plane", "time"};
		gd.addChoice("Display_horizontally: ", dirs, dirs[0]);
		gd.addChoice("Display_vertically: ", dirs, dirs[1]);
		gd.addChoice("Display_in_depth: ", dirs, dirs[2]);
		String[] projMethods = new String[] {"None", "Maximum", "Minimum", "Gaussian Stack Focuser"};
		gd.addChoice("Projection Method", projMethods, "None");
		gd.addChoice("Projection Direction", dirs, dirs[3]);
		gd.addCheckbox("Use Virtual Stack", true);
		gd.setModal(false);
		gd.setActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				Vector<Choice> choices = gd.getChoices();
				final int sample           = Integer.parseInt(choices.get(0).getSelectedItem().substring(1));
				final int region           = Integer.parseInt(choices.get(1).getSelectedItem().substring(1));
				final int angle            = Integer.parseInt(choices.get(2).getSelectedItem().substring(1));
				final int channel          = Integer.parseInt(choices.get(3).getSelectedItem().substring(1));
				final int xDir             = choices.get(4).getSelectedIndex();
				final int yDir             = choices.get(5).getSelectedIndex();
				final int zDir             = choices.get(6).getSelectedIndex();
				final int projectionMethod = choices.get(7).getSelectedIndex();
				final int projectionDir    = choices.get(8).getSelectedIndex();

				List<DoubleSlider> sliders = gd.getDoubleSliders();
				DoubleSlider slider = sliders.get(0);
				final int tpMin = slider.getCurrentMin();
				final int tpMax = slider.getCurrentMax();
				slider = sliders.get(1);
				final int xMin = slider.getCurrentMin();
				final int xMax = slider.getCurrentMax();
				slider = sliders.get(2);
				final int yMin = slider.getCurrentMin();
				final int yMax = slider.getCurrentMax();
				slider = sliders.get(3);
				final int zMin = slider.getCurrentMin();
				final int zMax = slider.getCurrentMax();
				slider = sliders.get(4);
				final int fMin = slider.getCurrentMin();
				final int fMax = slider.getCurrentMax();

				@SuppressWarnings("unchecked")
				Vector<Checkbox> checkboxes = gd.getCheckboxes();
				final boolean virtual = checkboxes.get(0).getState();

				new Thread() {
					@Override
					public void run() {
long start = System.currentTimeMillis();
						try {
							exp.open(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, yMin, yMax, xMin, xMax, xDir, yDir, zDir, virtual, projectionMethod, projectionDir).show();
long end = System.currentTimeMillis();
System.out.println("needed " + (end - start) + " ms");
							String command = "call(\"huisken.opener.SPIM_Opener.open\",\n";
							command += "\t\"" + directory + filename + "\",  // path to xml\n";
							command += "\t\"" + sample               + "\",  // sample\n";
							command += "\t\"" + tpMin                + "\",  // first timepoint\n";
							command += "\t\"" + tpMax                + "\",  // last timepoint\n";
							command += "\t\"" + region               + "\",  // region\n";
							command += "\t\"" + angle                + "\",  // angle\n";
							command += "\t\"" + channel              + "\",  // channel\n";
							command += "\t\"" + zMin                 + "\",  // first plane\n";
							command += "\t\"" + zMax                 + "\",  // last plane\n";
							command += "\t\"" + fMin                 + "\",  // first frame\n";
							command += "\t\"" + fMax                 + "\",  // last frame\n";
							command += "\t\"" + yMin                 + "\",  // minimum y\n";
							command += "\t\"" + yMax                 + "\",  // maximum y\n";
							command += "\t\"" + xMin                 + "\",  // minimum x\n";
							command += "\t\"" + xMax                 + "\",  // maximum x\n";
							command += "\t\"" + xDir                 + "\",  // direction which is displayed horizontally\n";
							command += "\t\"" + yDir                 + "\",  // direction which is displayed vertically\n";
							command += "\t\"" + zDir                 + "\",  // direction which is displayed in depth\n";
							command += "\t\"" + virtual              + "\"); // virtual?";
							command += "\t\"" + projectionMethod     + "\",  // projection method\n";
							command += "\t\"" + projectionDir        + "\",  // projection axis\n";

							if(Recorder.record)
								Recorder.recordString(command);
						} catch(Exception e) {
							IJ.error(e.getMessage());
							e.printStackTrace();
						}
					}
				}.start();
			}
		});
		gd.showDialog();
	}

	public static void open(String xmlpath,
				String sample,
				String tpMin, String tpMax,
				String region,
				String angle,
				String channel,
				String zMin, String zMax,
				String fMin, String fMax,
				String yMin, String yMax,
				String xMin, String xMax,
				String xDir, String yDir, String zDir,
				String virtual,
				String projectionMethod,
				String projectionDir) {

		open(xmlpath,
			Integer.parseInt(sample),
			Integer.parseInt(tpMin),
			Integer.parseInt(tpMax),
			Integer.parseInt(region),
			Integer.parseInt(angle),
			Integer.parseInt(channel),
			Integer.parseInt(zMin),
			Integer.parseInt(zMax),
			Integer.parseInt(fMin),
			Integer.parseInt(fMax),
			Integer.parseInt(yMin),
			Integer.parseInt(yMax),
			Integer.parseInt(xMin),
			Integer.parseInt(xMax),
			Integer.parseInt(xDir),
			Integer.parseInt(yDir),
			Integer.parseInt(zDir),
			Boolean.parseBoolean(virtual),
			Integer.parseInt(projectionMethod),
			Integer.parseInt(projectionDir));
	}

	public static void open(String xmlpath,
				int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int yMin, int yMax,
				int xMin, int xMax,
				int xDir, int yDir, int zDir,
				boolean virtual,
				int projectionMethod,
				int projectionDir) {
		SPIMExperiment exp = null;
		try {
			exp = new SPIMExperiment(xmlpath);
		} catch(Exception e) {
			throw new RuntimeException("Cannot load experiment " + xmlpath, e);
		}

		exp.open(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, yMin, yMax, xMin, xMax, xDir, yDir, zDir, virtual, projectionMethod, projectionDir).show();
	}

	public static void open(String xmlpath,
				String sample,
				String tpMin, String tpMax,
				String region,
				String angle,
				String channel,
				String zMin, String zMax,
				String fMin, String fMax,
				String projection,
				String virtual) {
		open(xmlpath,
			Integer.parseInt(sample),
			Integer.parseInt(tpMin),
			Integer.parseInt(tpMax),
			Integer.parseInt(region),
			Integer.parseInt(angle),
			Integer.parseInt(channel),
			Integer.parseInt(zMin),
			Integer.parseInt(zMax),
			Integer.parseInt(fMin),
			Integer.parseInt(fMax),
			Integer.parseInt(projection),
			Boolean.parseBoolean(virtual));
	}

	public static void open(String xmlpath,
				int sample,
				int tpMin, int tpMax,
				int region,
				int angle,
				int channel,
				int zMin, int zMax,
				int fMin, int fMax,
				int projection,
				boolean virtual) {

		SPIMExperiment exp = null;
		try {
			exp = new SPIMExperiment(xmlpath);
		} catch(Exception e) {
			throw new RuntimeException("Cannot load experiment " + xmlpath, e);
		}

		exp.open(sample, tpMin, tpMax, region, angle, channel, zMin, zMax, fMin, fMax, projection, virtual).show();
	}
}
