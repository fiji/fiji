package amira;

import ij.IJ;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class AmiraTableEncoder {
	AmiraParameters parameters;
	TextWindow textWindow;
	int columnCount, rowCount;
	String[] headings;
	float[][] values;
	int[] textSizes;
	String[][] text;

	public AmiraTableEncoder(TextWindow window) {
		textWindow = window;
		TextPanel panel = window.getTextPanel();
		String h = panel.getColumnHeadings();
		headings = h.split("\t");
		for (int i = 0; i < headings.length; i++)
			if (headings[i].equals("") || headings[i].equals(" "))
				headings[i] = "column" + i;

		columnCount = headings.length;
		rowCount = panel.getLineCount();
		text = new String[rowCount][columnCount];
		for (int i = 0; i < rowCount; i++)
			text[i] = panel.getLine(i).split("\t");

		if (window instanceof AmiraTable)
			parameters = new AmiraParameters(
					((AmiraTable)window).properties);
		else {
			String p = "Parameters {\n"
				+ AmiraTable.getParameterString(rowCount,
						headings)
				+ "}\n";

			parameters = new AmiraParameters(p);
		}

		// analyse type
		values = new float[columnCount][];
		textSizes = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			values[i] = new float[rowCount];
			textSizes[i] = -1;
			for (int j = 0; j < rowCount; j++) {
				try {
					values[i][j] = Float.parseFloat(
							text[j][i]);
				} catch(NumberFormatException e) {
					values[i] = null;
					textSizes[i] = 0;
					for (j = 0; j < rowCount; j++)
						textSizes[i] +=
							text[j][i].length() + 1;
				}
			}
		}
	}

	public boolean write(String name) {
		try {
			FileWriter writer = new FileWriter(name);
			Date date=new Date();
			writer.write("# AmiraMesh 3D ASCII 2.0\n"
					+ "# CreationDate: " + date + "\n\n");

			for (int i = 0; i < columnCount; i++)
				writer.write("define " + headings[i] + " "
						+ (values[i] != null ? rowCount
							: textSizes[i]) + "\n");

			writer.write("\nParameters {\n" + parameters
					+ "\n}\n\n");

			for (int i = 0; i < columnCount; i++)
				writer.write(headings[i] + " { "
						+ (values[i] != null ? "float"
							: "byte")
						+ " " + headings[i] + " } @"
						+ (i + 1) + "\n");

			writer.write("\n#Data section follows\n");

			for (int i = 0; i < columnCount; i++) {
				writer.write("@" + (i + 1) + "\n");
				if (values[i] != null)
					for (int j = 0; j < rowCount; j++)
						writer.write(text[j][i] + "\n");
				else
					writeStringColumn(writer, i);
				writer.write("\n");
			}

			writer.close();
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			IJ.error(e.toString());
		}
		return false;
	}

	void writeStringColumn(FileWriter writer, int i) throws IOException {
		for (int j = 0; j < rowCount; j++) {
			byte[] b = text[j][i].getBytes();
			for (int k = 0; k < b.length; k++)
				writer.write(Integer.toString(b[k]) + "\n");
			writer.write("0\n");
		}
	}
}

