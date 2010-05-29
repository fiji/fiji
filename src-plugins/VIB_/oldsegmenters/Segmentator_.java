package oldsegmenters;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import events.RoiEvent;
import events.RoiListener;
import events.RoiWatcher;
import events.SliceEvent;
import events.SliceListener;
import events.SliceWatcher;
import adt.Connectivity2D;
import adt.Points;

import amira.AmiraParameters;
import amira.AmiraMeshEncoder;
import amira.AmiraTableEncoder;
import vib.SegmentationViewerCanvas;

import gui.GuiBuilder;
import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.MacroInstaller;
import ij.text.TextWindow;
import ij.io.SaveDialog;

/*
 * Created on 29-May-2006
 RigidRegistration results after 2 hours of first canton
-5.46005480365032  1.5882993139839305  3.725043376523524  3.0612490934121572  3.4673867538095906  2.4252573345013158
    -4898.3026368170795 2631.3418518058224 2626.009767456783 67008.35532186428 67125.35178189375 67015.18689711517 15.216110139797248 19.561286307352486 2.4120241461431644

 */

public class Segmentator_ extends JFrame implements PlugIn {

	private static final String LOAD_IMAGE = "load image";
	private static final String SAVE_IMAGE = "save image";
	private static final String LOAD_LABELS = "load labels";
	private static final String SAVE_LABELS = "save labels";
	private static final String LOAD_MATERIALS = "load materials";

	private static final String THRESHOLD = "threshold";
	private static final String THRESHOLD_UNDO = "undo";

	private static final String INTERPOLATOR = "interpolate";


	private static final String DILATE = "dilate";
	private static final String ERODE = "erode";
	private static final String OPEN = "open";
	private static final String CLOSE = "close";
	private static final String CLEAN = "clean";


	private static final String NAIVE_LABEL = "naive auto label (optic lobes)";


	JList labelList;
	DefaultListModel labelListModel;

	JSpinner minThreshold;
	JSpinner maxThreshold;

	JTextField autoLabelFileLoc;

	public Segmentator_() {
		super("segmentator");

		Controllor controllor = new Controllor();

		MacroInstaller installer = new ij.plugin.MacroInstaller();
		installer.install(LabelBrush_.MACRO_CMD);

		ImagePlus.addImageListener(controllor);


		//the stup method may be called AFTER images have been opened,
		//we need listeners attached to all image windows though, to track focus
		for (Frame frame : ImageWindow.getFrames()) {
			if (frame instanceof ImageWindow) {
				controllor.imageOpened(((ImageWindow) frame).getImagePlus());
			}
		}


		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		labelList = addLabelList(this);
		labelListModel = (DefaultListModel) labelList.getModel();

		labelList.addListSelectionListener(controllor);

		GuiBuilder.add2Command(this, LOAD_IMAGE, LOAD_IMAGE, SAVE_IMAGE, SAVE_IMAGE, controllor);
		GuiBuilder.add2Command(this, LOAD_LABELS, LOAD_LABELS, SAVE_LABELS, SAVE_LABELS, controllor);
		GuiBuilder.addCommand(this, LOAD_MATERIALS, LOAD_MATERIALS, controllor);


		minThreshold = GuiBuilder.addLabeledNumericSpinner(this, "min", 45, 0, 255, controllor);
		maxThreshold = GuiBuilder.addLabeledNumericSpinner(this, "max", 255, 0, 255, controllor);

		GuiBuilder.add2Command(this, THRESHOLD, THRESHOLD, THRESHOLD_UNDO, THRESHOLD_UNDO, controllor);

		GuiBuilder.addCommand(this, INTERPOLATOR, INTERPOLATOR, controllor);

		GuiBuilder.add2Command(this, DILATE, DILATE, ERODE, ERODE, controllor);
		GuiBuilder.add2Command(this, OPEN,OPEN,CLOSE,CLOSE, controllor);
		GuiBuilder.addCommand(this, CLEAN, CLEAN, controllor);


		autoLabelFileLoc = GuiBuilder.addFileField(this, "auto label file loc (av intensity");
		GuiBuilder.addCommand(this, NAIVE_LABEL, NAIVE_LABEL, controllor);


		pack();
	}

	public void run(String arg0) {
		setVisible(!isVisible());
	}


	public void clearLabelsList() {
		labelListModel.clear();
	}


	public void populateLabelList(AmiraParameters params) {
		clearLabelsList();
		if (params == null) return;
		for (int id = 0; id < params.getMaterialCount(); id++) {
			labelListModel.addElement(params.getMaterial(id));
		}
	}


	public AmiraParameters.Material getCurrentMaterial() {
		int selectedIndex = labelList.getSelectedIndex();
		if (selectedIndex == -1)
			return null;
		else {
			return (AmiraParameters.Material) labelListModel.get(selectedIndex);
		}
	}


	private void threshold() {
		LabelThresholder_.min = ((SpinnerNumberModel) minThreshold.getModel()).getNumber().intValue();
		LabelThresholder_.max = ((SpinnerNumberModel) maxThreshold.getModel()).getNumber().intValue();

		IJ.runPlugIn("LabelThresholder_", "");
	}


	public static JList addLabelList(Container c) {
		final DefaultListModel model = new DefaultListModel();
		final JList list = new JList(model);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("labels..."), BorderLayout.NORTH);

		JPanel controlPanel = new JPanel(new GridLayout(1, 2));
		//panel.add(controlPanel, BorderLayout.SOUTH);
		panel.add(new JScrollPane(list));

		/*  REMOVED ADD AND REMOVE, thinkin might be better if the user is always forced to use a ready made .labels file
		JButton add = new JButton("add");
		add.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String name = IJ.getString("name?", "");
				ImagePlus imagePlus = IJ.getImage();

				//ImageLabels labels = new ImageLabels(imagePlus);
				//model.addElement(labels.newLabel(name));
			}
		});

		JButton remove = new JButton("remove");
		remove.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int index = list.getSelectedIndex();  //just currently working for the first of the selections
				System.out.println(index);
				if (index == -1) return;

				ImagePlus imagePlus = IJ.getImage();

				//ImageLabels labels = new ImageLabels(imagePlus);
				//ImageLabel selected = (ImageLabel) model.getElementAt(index);

				//remove from the list of labels and the list model...
				//labels.removeLabel(selected.getName());
				//model.removeElementAt(index);
			}
		});

		JButton deselect = new JButton("deselect");
		deselect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				list.clearSelection();
				IJ.getImage().killRoi();
			}
		});
		controlPanel.add(add);
		controlPanel.add(remove);
		controlPanel.add(deselect);
		*/
		c.add(panel);

		return list;
	}

	private class Controllor implements ActionListener, ImageListener, WindowFocusListener, SliceListener, RoiListener, ListSelectionListener, ChangeListener {

		ImagePlus currentImage;
        
        
        /* This is pointless duplication from AmiraMeshWriter_, but
         * simpler for the moment than moving that into a package. */

        public void writeImage(Object frame) {
            SaveDialog od = new SaveDialog("AmiraFile", null, ".am");
            String dir=od.getDirectory();
            String name=od.getFileName();
            if(name==null)
                return;
            
            if (frame instanceof TextWindow) {
                TextWindow t = (TextWindow)frame;
                AmiraTableEncoder e = new AmiraTableEncoder(t);
                if (!e.write(dir + name))
                    IJ.error("Could not write to " + dir + name);
                return;
            }
            
            AmiraMeshEncoder e=new AmiraMeshEncoder(dir+name);
            
            if(!e.open()) {
                IJ.error("Could not write "+dir+name);
                return;
            }
            
            if(!e.write((ImagePlus)frame))
                IJ.error("Error writing "+dir+name);
        }

		public void actionPerformed(ActionEvent e) {
			//IJ.showMessage(e.getActionCommand());

			if (e.getActionCommand().equals(LOAD_IMAGE)) {
				IJ.runPlugIn("AmiraMeshReader_", "");

				if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
					IJ.showMessage("file was not an image file");
				} else {
					updateCurrent(IJ.getImage());
				}


			} else if (e.getActionCommand().equals(SAVE_IMAGE)) {
				IJ.runPlugIn("AmiraMeshWriter_", "");
				//IJ.showMessage("greyscale edits not implemented yet, you can  save your labels though...");
			} else if (e.getActionCommand().equals(LOAD_LABELS)) {
				IJ.runPlugIn("AmiraMeshReader_", "");
				if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
					//load label data
					loadLabels(IJ.getImage());
				} else {
					IJ.showMessage("file was not a labels file");
				}
			} else if (e.getActionCommand().equals(SAVE_LABELS)) {
                writeImage(new SegmentatorModel(currentImage).getLabelImagePlus());
			} else if (e.getActionCommand().equals(LOAD_MATERIALS)) {
				IJ.runPlugIn("AmiraMeshReader_", "");
				if (AmiraParameters.isAmiraLabelfield(IJ.getImage())) {
					//load label data
					loadMaterials(IJ.getImage());
				} else {
					IJ.showMessage("file was not a labels file");
				}
			} else if (e.getActionCommand().equals(THRESHOLD)) {
				threshold();
			} else if (e.getActionCommand().equals(THRESHOLD_UNDO)) {
				LabelThresholder_.rollback();
			} else if (e.getActionCommand().equals(INTERPOLATOR)) {
				IJ.runPlugIn("LabelInterpolator_", "");
			} else if (e.getActionCommand().equals(DILATE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);

				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.dilate(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(ERODE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				System.out.println("eroding");
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.erode(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);

					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(CLOSE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);

				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.close(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(OPEN)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);

				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.open(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(CLEAN)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);

				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.close(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					LabelBinaryOps.open(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							, currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			}else if(e.getActionCommand().equals(NAIVE_LABEL)){
                new RunAsyncronous(e.getActionCommand()).start();
			}

			if(currentImage != null)
				currentImage.updateAndDraw();
		}

		private class RunAsyncronous extends Thread{
			String cmd;
			public RunAsyncronous (String command) {
				cmd = command;
			}

			public void run() {
				if(cmd.equals(NAIVE_LABEL)){
					try {
						AutoLabellerNaive naive = new AutoLabellerNaive(autoLabelFileLoc.getText());
						naive.segment(new SegmentatorModel(currentImage));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}


		//reads the parameters to get what the labels should be
		//but does not use any of the pixel data
		private void loadMaterials(ImagePlus materialImage) {


			AmiraParameters params = new AmiraParameters(materialImage);

			//create a blank image to work with
			System.out.println("currentImage.getStackSize() = " + currentImage.getStackSize());

			//todo does not work...
			IJ.newImage("new", "8-bit", currentImage.getWidth(), currentImage.getHeight(), currentImage.getStackSize());

			ImagePlus newImage = IJ.getImage();

			newImage.setProperty(AmiraParameters.INFO, materialImage.getProperty(AmiraParameters.INFO));

			//copy the parameter information across
			AmiraParameters newParams = new AmiraParameters(newImage);

			//close the initial image
			materialImage.close();

			//and load the new blank one as normal
			loadLabels(newImage);
		}

		private void loadLabels(ImagePlus labelImage) {

			labelImage.hide();//don't want the extra one visible to the user

			new SegmentatorModel(currentImage).setLabelImagePlus(labelImage);

			AmiraParameters params = new AmiraParameters(labelImage);

			populateLabelList(params);

			SegmentationViewerCanvas canvas = new SegmentationViewerCanvas(currentImage, labelImage);

			new SegmentatorModel(currentImage).setLabelCanvas(canvas);


			if (currentImage.getStackSize() > 1)
				new StackWindow(currentImage, canvas);
			else
				new ImageWindow(currentImage, canvas);

			//after a new window is constructed. the old one is
			//cloased and the listener tidied up
			//so we need to make sure we add a new one
			//we do not need to do this for ROIs becuase
			//they work by polling
			new SliceWatcher(currentImage).addSliceListener(this);


			currentImage.getWindow().addWindowFocusListener(this);
			//new RoiWatcher(currentImage).addRoiListener(this);
		}


		public void imageOpened(ImagePlus ip) {
			//when a new image is opened in the environement we need to listen to it gaining foces
			ip.getWindow().addWindowFocusListener(this);
		}

		public void imageClosed(ImagePlus ip) {
//			when a new image is closed we need to tidy up the listeners
			ip.getWindow().removeWindowFocusListener(this);
		}

		public void imageUpdated(ImagePlus ip) {
			//System.out.println("image Updated");
		}

		public void windowGainedFocus(WindowEvent e) {
			updateCurrent(IJ.getImage());
		}


		public void windowLostFocus(WindowEvent e) {
			//clearLabelsList(); removes labels when toolbar is highlighted
		}

		private void updateCurrent(ImagePlus newCurrent) {

			if (newCurrent == currentImage)
				return;
			else {
				if (currentImage != null) {
					new SliceWatcher(currentImage).removeSliceListener(this);
					//new RoiWatcher(currentImage).removeRoiListener(this);
				}

				if (newCurrent != null) {
					System.out.println("newCurrent = " + newCurrent);
					new SliceWatcher(newCurrent).addSliceListener(this);
					//new RoiWatcher(newCurrent).addRoiListener(this);

					//we temporaritly turn of list events while repopulating
					labelList.removeListSelectionListener(this);
					populateLabelList(new SegmentatorModel(newCurrent).getMaterialParams());
					labelList.addListSelectionListener(this);
				}
				currentImage = newCurrent;
			}

		}

		public void sliceNumberChanged(SliceEvent e) {
			LabelThresholder_.commit(); //commit any thesholdings if any
			//System.out.println(e.getSource().getCurrentSlice());
			//drawLabels(currentImage);
			//drawLabels(currentImage, currentLabels, canvas);
		}

		public void roiChanged(RoiEvent e) {
			//setLabel(currentImage);
		}

		public void valueChanged(ListSelectionEvent e) {
			new SegmentatorModel(currentImage).setCurrentMaterial(getCurrentMaterial());
		}

		public void stateChanged(ChangeEvent e) {
			if (e.getSource().equals(minThreshold) || e.getSource().equals(maxThreshold)) {
				//the spinners have changed. We will try a live update
				//if we have an active ROI at the time
				if (currentImage.getRoi() != null) {
					LabelThresholder_.rollback();
					threshold();
				}
			}
		}
	}

}
