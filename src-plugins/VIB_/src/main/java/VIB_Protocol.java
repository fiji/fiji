import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.io.File;

import ij.IJ;
import ij.macro.Interpreter;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.io.DirectoryChooser;

import vib.app.gui.ProgressIndicator;
import vib.app.gui.Console;
import vib.app.gui.FileGroupDialog;
import vib.app.FileGroup;
import vib.app.Options;
import vib.app.module.Module;
import vib.app.module.EndModule;
import vib.app.module.State;

public class VIB_Protocol implements PlugIn, ActionListener {

	// indices in the getStringFields() / getNumericFields()
	static final int WD = 0;
	static final int TEMPL = 1;
	static final int NO_CHANNEL = 0;
	static final int REF_CHANNEL = 1;
	static final int RES_F = 2;

	private Button fg, load, save, templateButton;
	private Options options;
	private GenericDialog gd;
	private FileGroupDialog fgd;
	private File template;
	
	public void run(String arg) {
		options = new Options();
		String option;

		if(Macro.getOptions() != null && !(option = Macro.getValue(
			Macro.getOptions(), "load", "")).equals("")) {
			
			options.loadFrom(option);
			State state = new State(options);
			Module.addModuleListener(new ProgressIndicator(options));
			new EndModule().runOnAllImages(state);
			return;
		}

		gd = new GenericDialog("VIB Protocol");
		gd.addMessage("Do you want to load a stored configuration? ");
		gd.addMessage("If not, leave blank and click 'OK'");
		gd.addMessage("  ");
		Panel panel = new Panel();
		Button button = new Button("Load");
		panel.add(button);
		gd.addPanel(panel);
		gd.addStringField("Configuration file", "", 25);
		final TextField confTF = (TextField)gd.getStringFields().get(0);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OpenDialog d = new OpenDialog("Load", "");
				if (d.getFileName() == null)
					return; // canceled
				String f = d.getDirectory() + d.getFileName();
				confTF.setText(f);
				gd.repaint();
			}
		});
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		
		gd = new GenericDialog("VIB Protocol");
		fgd = new FileGroupDialog(options.fileGroup);
		templateButton = fgd.getTemplateButton();
		templateButton.addActionListener(this);
		
		gd.addPanel(fgd);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		load = new Button("Select working directory");
		load.addActionListener(this);
		// work around not being able to access gd.y
		panel = new Panel();
		gd.addPanel(panel);
		gd.remove(panel);
		gd.add(load, c);


		gd.addStringField("Working directory","", 25);
		gd.addStringField("Template", "", 25);
		gd.addNumericField("No of channels", 2, 0);
		gd.addNumericField("No of the reference channel", 2, 0);
		gd.addNumericField("Resampling factor", 2, 0);

		final TextField wdtf = (TextField)gd.getStringFields().get(WD);

		// if an option file is available, fill the forms
		if(!confTF.getText().trim().equals("")) {
			loadFrom(confTF.getText());
		}

		// make the template textfield ineditable
		TextField templateField =
			(TextField)gd.getStringFields().get(TEMPL);
		templateField.setEditable(false);


		gd.showDialog();
		if(gd.wasCanceled())
			return;

		initOptions();
		String confFile = options.workingDirectory + File.separator
					+ Options.CONFIG_FILE;
		options.saveTo(confFile);
		if(!Interpreter.isBatchMode() && Macro.getOptions() == null) {
			IJ.showMessage("Stored configuration in \n" + confFile 
				+ "\nYou can load it the next time you start "
				+ "the protocol.");
		}

		State state = new State(options);
		Module.addModuleListener(new ProgressIndicator(options));
		new EndModule().runOnAllImages(state);
	}

	public void loadFrom(String confFile) {
		if(new File(confFile).exists()) {
			options.loadFrom(confFile);
			initTextFields();
		}
	}

	public void initTextFields() {
		template = new File(options.templatePath);
		setString(WD, options.workingDirectory);
		setString(TEMPL, options.templatePath);
		setNumber(NO_CHANNEL, options.numChannels);
		setNumber(REF_CHANNEL, options.refChannel);
		setNumber(RES_F, options.resamplingFactor);
		//int method = options.transformationMethod;
		//setChoice(TRANSF, Options.TRANSFORMS[method]);
		fgd.update();
	}

	public void initOptions() {
		options.workingDirectory = getString(WD);
		options.templatePath = getString(TEMPL);
		options.numChannels = getNumber(NO_CHANNEL);
		options.refChannel = getNumber(REF_CHANNEL);
		options.resamplingFactor = getNumber(RES_F);
		//options.setTransformationMethod(getChoice(TRANSF));
	}

	private String getChoice(int i) {
		Choice c = (Choice)gd.getChoices().get(i);
		return c.getSelectedItem();
	}

	private void setChoice(int i, String val) {
		((Choice)gd.getChoices().get(i)).select(val);
	}

	private int getNumber(int i) {
		TextField tf = (TextField)gd.getNumericFields().get(i);
		double d = 0;
		try {
			d = Double.parseDouble(tf.getText());
		} catch (NumberFormatException e) {
			IJ.error(tf.getText() + " is not a number");
		}
		return (int)Math.round(d);
	}

	private String getString(int i) {
		TextField tf = (TextField)gd.getStringFields().get(i);
		return tf.getText();
	}

	private void setNumber(int i, int num) {
		((TextField)gd.getNumericFields().get(i)).
			setText(Integer.toString(num));
	}

	private void setString(int i, String st) {
		((TextField)gd.getStringFields().get(i)).setText(st);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == templateButton) {
			File selected = fgd.getSelected();
			if(selected != null) {
				template = selected;
				setString(TEMPL, selected.getAbsolutePath());
			}
		} else if (e.getSource() == load) {
			DirectoryChooser dialog = 
				new DirectoryChooser("Working Directory");
			String dir = dialog.getDirectory();
			if (dir != null)
				setString(WD, dir);
// 				loadFrom(dir);
		}
	}
}
