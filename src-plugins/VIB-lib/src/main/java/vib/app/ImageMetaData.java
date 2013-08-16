package vib.app;

/* This class is only a wrapper around AmiraTable for the moment */

import amira.AmiraTable;
import amira.AmiraMeshDecoder;
import amira.AmiraTableEncoder;

import ij.text.TextPanel;
import ij.macro.Interpreter;
import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import vib.FloatMatrix;

public class ImageMetaData {
	public static class Material {
		public String name;
		public int count;
		public double volume, centerX, centerY, centerZ;
	}
	public static class Transformation {
		public String name;
		FloatMatrix matrix;
	}

	public Material[] materials;
	public Transformation[] transformations;
	private String statisticsPath;

	public ImageMetaData() {
		materials = new Material[0];
		transformations = new Transformation[0];
	}

	public ImageMetaData(String fileName) {
		this();
		loadFrom(fileName);
	}

	public FloatMatrix getMatrix(String name) {
		for (int i = 0; i < transformations.length; i++)
			if (transformations[i].name.equals(name))
				return transformations[i].matrix;
		return null;
	}

	public void setMatrix(String name, FloatMatrix matrix) {
		int i;
		for (i = 0; i < transformations.length; i++)
			if (transformations[i].name.equals(name)) {
				transformations[i].matrix = matrix;
				return;
			}
		Transformation[] newTransformations =
			new Transformation[transformations.length + 1];
		System.arraycopy(transformations, 0,
				newTransformations, 0, transformations.length);
		Transformation newTransformation = new Transformation();
		newTransformation.name = name;
		newTransformation.matrix = matrix;
		newTransformations[transformations.length] = newTransformation;
		transformations = newTransformations;
	}

	public Material getMaterial(String name) {
		for (int i = 0; i < materials.length; i++)
			if (materials[i].name.equals(name))
				return materials[i];
		return null;
	}

	public int getMaterialIndex(String name) {
		for (int i = 0; i < materials.length; i++)
			if (materials[i].name.equals(name))
				return i;
		return -1;
	}

	public void setMaterial(String name, int count, double volume,
			double centerX, double centerY, double centerZ) {
		int i = getMaterialIndex(name);
		if (i < 0) {
			Material[] newMaterials =
				new Material[materials.length + 1];
			i = materials.length;
			System.arraycopy(materials, 0, newMaterials, 0, i);
			materials = newMaterials;
			materials[i] = new Material();
		}
		Material m = materials[i];
		m.name = name;
		m.count = count;
		m.volume = volume;
		m.centerX = centerX;
		m.centerY = centerY;
		m.centerZ = centerZ;
	}

	public void loadFrom(String path) {
		statisticsPath = path;
		if (!(new File(path).exists()))
			return;

		AmiraMeshDecoder decoder = new AmiraMeshDecoder();
		final AmiraTable table;

		if (decoder.open(path) &&
				decoder.isTable()) {
			table = decoder.getTable();
		} else
			return;

		TextPanel panel = table.getTextPanel();
		materials = new Material[panel.getLineCount()];
		for (int i = 0; i < materials.length; i++) {
			String[] values = split(panel.getLine(i));
			materials[i] = new Material();
			materials[i].name = values[1];
			materials[i].count = Integer.parseInt(values[2]);
			materials[i].volume = Double.parseDouble(values[3]);
			materials[i].centerX = Double.parseDouble(values[4]);
			materials[i].centerY = Double.parseDouble(values[5]);
			materials[i].centerZ = Double.parseDouble(values[6]);
		}

		Hashtable props = table.getParameters();
		table.close();
		ArrayList transforms = new ArrayList();
		Enumeration keys = props.keys();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			if (key.indexOf("Transformation") < 0)
				continue;
			Transformation t = new Transformation();
			t.name = key;
			String matrix = (String)props.get(key);
			t.matrix = FloatMatrix.parseMatrix(matrix);
			transforms.add(t);
		}

		transformations = new Transformation[transforms.size()];
		for (int i = 0; i < transformations.length; i++)
			transformations[i] = (Transformation)transforms.get(i);
	}

	private final static String AMIRA_HEADINGS = "Nr\tMaterial\tCount\t"
		+ "Volume\tCenterX\tCenterY\tCenterZ";

	public boolean saveTo(String path) {
		statisticsPath = path;
		String data = "";
		for (int i = 0; i < materials.length; i++) {
			Material m = materials[i];
			data += "" + (i + 1) + "\t" + m.name + "\t" +
				m.count + "\t" + m.volume + "\t" + m.centerX +
				"\t" + m.centerY + "\t" + m.centerZ + "\n";
		}

		// prevent table from showing
		AmiraTable table = new AmiraTable("Statistics for " +
			path, AMIRA_HEADINGS, data, true);

		Hashtable p = table.getParameters();
		for (int i = 0; i < transformations.length; i++) {
			Transformation t = transformations[i];
			p.put(t.name, t.matrix.toStringForAmira());
		}
		AmiraTableEncoder encoder = new AmiraTableEncoder(table);
		return encoder.write(path);
	}

	private static String[] split(String line) {
		ArrayList list = new ArrayList();
		int tab = -1;
		do {
			int lastTab = tab;
			tab = line.indexOf('\t', tab + 1);
			if (tab >= 0)
				list.add(line.substring(lastTab + 1, tab));
			else
				list.add(line.substring(lastTab + 1));
		} while (tab >= 0);

		String[] result = new String[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)list.get(i);
		return result;
	}

	public boolean upToDate(String sourcePath, String transformLabel) {
		File thisFile = new File(statisticsPath);
		if (!thisFile.exists())
			return false;
		File sourceFile = new File(sourcePath);
		if (sourceFile.exists() && thisFile.lastModified() <
				sourceFile.lastModified())
			return false;
		if (getMatrix(transformLabel) == null)
			return false;
		return true;
	}
}

