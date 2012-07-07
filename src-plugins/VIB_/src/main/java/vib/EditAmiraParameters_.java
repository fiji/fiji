package vib;

import amira.AmiraParameters;
import amira.AmiraTable;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.macro.Interpreter;
import ij.plugin.*;
import ij.io.SaveDialog;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.*;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.image.*;

public class EditAmiraParameters_ implements PlugIn, ActionListener,
	TextListener {

	GenericDialog gd;
	Choice windowList;
	TextField keyField, valueField;

	public void run(String arg) {
		gd = new GenericDialog("Parameters");
		AmiraParameters.addWindowList(gd, "window", true);
		gd.addStringField("key", "");
		gd.addStringField("value", "");

		Vector v = gd.getChoices();
		windowList = (Choice)v.get(0);
		v = gd.getStringFields();
		keyField = (TextField)v.get(0);
		valueField = (TextField)v.get(1);
		keyField.addActionListener(this);
		keyField.addTextListener(this);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String title = gd.getNextChoice();
		String key = gd.getNextString();
		String value = gd.getNextString();
		for (int i = 0; i < value.length(); i++)
			if (value.charAt(i) == '"') {
				value = value.substring(0, i) + "\\"
					 + value.substring(i);
				i++;
			}
		value = "\"" + value + "\"";

		ImagePlus img = WindowManager.getImage(title);
		if (img != null) {
			AmiraParameters p = new AmiraParameters(img);
			p.put(key, value);
			p.setParameters(img);
			return;
		}
		Frame frame = WindowManager.getFrame(title);
		if (frame == null || !(frame instanceof AmiraTable)) {
			IJ.error("Invalid window: " + title);
			return;
		}
		AmiraTable table = (AmiraTable)frame;
		table.getProperties().put(key, value);
	}

	public void actionPerformed(ActionEvent e) {
		checkForValue();
	}

	public void textValueChanged(TextEvent e) {
		checkForValue();
	}

	void checkForValue() {
		int index = windowList.getSelectedIndex();
		String title = windowList.getItem(index);
		AmiraParameters p;
		ImagePlus img = WindowManager.getImage(title);
		if (img != null)
			p = new AmiraParameters(img);
		else {
			AmiraTable table =
				(AmiraTable)WindowManager.getFrame(title);
			p = new AmiraParameters(table.getProperties());
		}
		String key = keyField.getText();
		Object o = p.get(key);
		if (o instanceof String) {
			String value = (String)o;
			if (value.startsWith("\""))
				value = value.substring(1);
			if (value.endsWith("\""))
				value = value.substring(0, value.length() - 1);
			valueField.setText(value);
		}
	}
}


