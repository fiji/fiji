package vib.app;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;

import ij.IJ;

public class Options {
	public final static String CONFIG_FILE = "options.config";

	public final static int GREY = 0;
	public final static int LABEL_SURFACE = 1;
	public final static int CENTER = 2;
	public final static int LABEL_DIFFUSION = 3;
	public final static String[] TRANSFORMS = {
		"VIBgreyTransformation",
		"VIBlabelSurfaceTransformation",
		"VIBcenterTransformation",
		"VIBlabelDiffusionTransformation"
	};
	public final static String[] TRANSFORM_LABELS = {
		"RegistrationTransformation",
		"LabelSurfaceTransformation",
		"SCenterTransformation",
		"SLabelTransformation"
	};
	
	public String workingDirectory;
	public FileGroup fileGroup;
	public String templatePath;
	public int numChannels = 2;
	public int refChannel = 2;
	public int resamplingFactor = 1;
	public int transformationMethod = LABEL_DIFFUSION;

	// Constructors
	public Options() {
		fileGroup = new FileGroup("NewFilegroup");
	}

	public Options(String dirname) {
		workingDirectory = dirname;
		File dir = new File(dirname);
		if(!dir.exists())
			dir.mkdir();
		else {
			File optionsFile = new File(dirname, CONFIG_FILE);
			if (optionsFile.exists())
				loadFrom(optionsFile.getAbsolutePath());
		}
		if (fileGroup == null)
			fileGroup = new FileGroup("NewFilegroup");
	}

	public Options(File dir) {
		this(dir.getAbsolutePath());
	}

	public void setTransformationMethod(String method) {
		for (int i = 0; i < TRANSFORMS.length; i++)
			if (TRANSFORMS[i].equals(method)) {
				transformationMethod = i;
				return;
			}
		throw new RuntimeException("Unknown method: " + method);
	}

	public boolean needsLabels() {
		return needsLabels(transformationMethod);
	}

	public static boolean needsLabels(int method) {
		return method != GREY;
	}

	public boolean isTransformationValid(String method) {
		for (int i = 0; i < TRANSFORMS.length; i++)
			if (TRANSFORMS[i].equals(method))
				return true;
		return false;
	}

	public boolean isValid() {
		File dir = new File(workingDirectory);
		return !fileGroup.isEmpty() &&
			templatePath != null &&
			new File(templatePath).exists() &&
			numChannels > 0 &&
			refChannel > 0 && refChannel <= numChannels &&
			dir.exists() && dir.isDirectory() &&
			transformationMethod >= GREY &&
			transformationMethod <= LABEL_DIFFUSION;
	}

	public Options clone() {
		Options clone = new Options();
		clone.copy(this);
		return clone;
	}

	public void copy(Options options) {
		workingDirectory = options.workingDirectory;
		templatePath = options.templatePath;
		numChannels = options.numChannels;
		refChannel = options.refChannel;
		fileGroup = options.fileGroup;
		transformationMethod = options.transformationMethod;
		resamplingFactor = options.resamplingFactor;
	}

	public void saveTo(String path) {
		Properties p = new Properties();
		p.setProperty("template", templatePath);
		p.setProperty("numChannels", "" + numChannels);
		p.setProperty("refChannel", "" + refChannel);
		p.setProperty("resamplingFactor", "" + resamplingFactor);
		p.setProperty("transformationMethod",
				TRANSFORMS[transformationMethod]);
		p.setProperty("fileGroup", fileGroup.toCSV());

		try {
			OutputStream out = new FileOutputStream(path);
			p.store(out, "Created by the VIB application");
		} catch(FileNotFoundException e) {
			IJ.showMessage("Can't find file " + path);
		} catch(IOException e) {
			IJ.showMessage("Can't write to file " + path);
		}
	}

	static int getInt(Properties p, String key, int default_value) {
		String value = p.getProperty(key);
		if (value == null)
			return default_value;
		return Integer.parseInt(value);
	}

	public void loadFrom(String path) {
		Properties p = new Properties();
		try {
			File file = new File(path);
			p.load(new FileInputStream(file));
			workingDirectory =
				file.getParentFile().getAbsolutePath();
		} catch (FileNotFoundException e) {
			IJ.showMessage("Can't find file " + path);
			return;
		} catch(IOException e) {
			IJ.showMessage("Can't read from file " + path);
			return;
		}
		templatePath = p.getProperty("template", "");
		numChannels = getInt(p, "numChannels", 2);
		refChannel = getInt(p, "refChannel", 2);
		String t = p.getProperty("transformationMethod");
		try {
			setTransformationMethod(t);
		} catch (Exception e) {
			transformationMethod = LABEL_DIFFUSION;
		}
		resamplingFactor = getInt(p, "resamplingFactor", 1);
		if(!fileGroup.fromCSV(p.getProperty("fileGroup", "")))
			IJ.showMessage("Not all files specified in the "
					+ "file group exist.");
		if(!isValid()){
			IJ.showMessage("There occured an error while setting "
					+ "the options. I set some of them, "
					+ "but you should check them "
					+ "manually.");
			return;
		}
	}

	public String toString() {
		return "Options:\n" +
		"workingDirectory = " + workingDirectory + "\n" +
		"template = " + templatePath + "\n" +
		"numChannels = " + numChannels + "\n" +
		"refChannel = " + refChannel + "\n" +
		"transformationMethod = " + transformationMethod + "\n" +
		"resamplingFactor = " + resamplingFactor + "\n" +
		"fileGroup: " + fileGroup;
	}

	public void debug() {
		System.out.print("\n" + this);
	}

	public static void main(String[] args){
		System.out.println("Options");
		if (args.length > 0) {
			Options o = new Options();
			if (args[0].equals("--save") && args.length > 1)
				o.saveTo(args[1]);
			else
				o.loadFrom(args[0]);
			o.debug();
		}
	}
}
