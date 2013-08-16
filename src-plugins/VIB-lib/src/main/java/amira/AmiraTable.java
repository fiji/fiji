package amira;

import ij.WindowManager;

import ij.macro.Interpreter;

import ij.text.TextWindow;

import java.text.DecimalFormat;

import java.util.Hashtable;
import java.util.Properties;

public class AmiraTable extends TextWindow {
	Properties properties;

	// variable to avoid automatic showing when the constructor is called
	private boolean show = false;

	public AmiraTable(String title, String headings, String data) {
		this(title, headings, data, false);
	}

	public AmiraTable(String title, String headings, String data,
			boolean initParameters) {
		super(title, headings, data, 500, 400);
		show = true;
		properties = new Properties();
		if (initParameters) {
			int rowCount = getTextPanel().getLineCount();
			String p = "Parameters { "
				+ getParameterString(rowCount,
						headings.split("\t")) + " }";
			AmiraParameters parameters = new AmiraParameters(p);
			parameters.setParameters(properties);
		}
		WindowManager.removeWindow(this);
	}

	public static String getParameterString(int rows, String[] headings) {
		String p = "\tContentType \"HxSpreadSheet\",\n";

		DecimalFormat format = new DecimalFormat("0000");
		for (int i = 0; i < headings.length; i++)
			p += "\t__ColumnName" + format.format(i)
				+ "\"" + headings[i] + "\",\n";

		p += "\tnumRows " + rows + ",\n";

		return p;
	}

	public Properties getProperties() {
		return properties;
	}

	public Hashtable getParameters() {
		return (Hashtable)properties.get("Parameters");
	}

	public String get(String key) {
		Hashtable p = getParameters();
		if (p == null)
			return null;
		return (String)p.get(key);
	}

	public void put(String key, String value) {
		Hashtable p = getParameters();
		if (p == null)
			return;
		p.put(key, value);
	}

	public void show() {
		if (!Interpreter.isBatchMode() && show) {
			super.show();
			WindowManager.removeWindow(this);
		}
	}
}

