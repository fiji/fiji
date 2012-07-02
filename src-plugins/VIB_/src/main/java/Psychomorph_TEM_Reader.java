/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/*
 * Use this plugin to read PsychoMorph .tem files
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class Psychomorph_TEM_Reader implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus ip) {
		image = ip;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor imp) {
		OpenDialog od = new OpenDialog("TEM File", null);
		String dir = od.getDirectory();
		String arg = od.getFileName();
		if(arg==null)
			return;
		try {
			TEM tem = new TEM();
			tem.readFile(dir + File.separator + arg);
			tem.setRoi(image);
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	private static class TEM {
		int pointCount, lineCount;
		float[] x, y;
		int[][] lines;

		void setRoi(ImagePlus imp) {
			int count = x.length;
			int[] x1 = new int[count];
			int[] y1 = new int[count];
			for (int i = 0; i < count; i++) {
				x1[i] = (int)Math.round(x[i]);
				y1[i] = (int)Math.round(y[i]);
			}
			PointRoi roi = new PointRoi(x1, y1, count);
			imp.setRoi(roi);
			imp.updateAndDraw();
		}

		// TODO: addToRoiManager()

		void readFile(String fileName) throws IOException {
			BufferedReader in = new BufferedReader(new
					FileReader(fileName));
			pointCount = Integer.parseInt(in.readLine());
			x = new float[pointCount];
			y = new float[pointCount];
			for (int i = 0; i < pointCount; i++) {
				String[] values =
					ij.util.Tools.split(in.readLine());
				x[i] = Float.parseFloat(values[0]);
				y[i] = Float.parseFloat(values[1]);
			}
			lineCount = Integer.parseInt(in.readLine());
			lines = new int[lineCount][];
			for (int i = 0; i < lineCount; i++) {
				in.readLine();
				int len = Integer.parseInt(in.readLine());
				String line = in.readLine();
				String[] values = ij.util.Tools.split(line);
				if (len != values.length)
					throw new RuntimeException("len "
						+ "mismatch: len=" + len
						+ ", line is " + line);
				lines[i] = new int[len];
				for (int j = 0; j < len; j++)
					lines[i][j] =
						Integer.parseInt(values[j]);
			}
			in.close();
		}

		void writeFile(String fileName) throws IOException {
			PrintStream out = new PrintStream(new
					FileOutputStream(fileName));
			out.println(pointCount);
			for (int i = 0; i < pointCount; i++)
				out.println("" + x[i] + "\t" + y[i]);
			out.println(lineCount);
			for (int i = 0; i < lineCount; i++) {
				out.println(0);
				int len = lines[i].length;
				out.println(len);
				for (int j = 0; j < len - 1; j++)
					out.print(lines[i][j] + " ");
				out.println(lines[i][len - 1]);
			}
			out.println(0);
			out.close();
		}

		boolean lineSetsEqual(TEM other) {
			if (lines.length != other.lines.length)
				return false;
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].length != other.lines[i].length)
					return false;
				for (int j = 0; j < lines[i].length; j++)
					if (lines[i][j] != other.lines[i][j])
						return false;
			}
			return true;
		}

		void addPoint(float x, float y) {
			if (pointCount + 1 > this.x.length) {
				int newLength = pointCount + 32;
				float[] dummy = new float[newLength];
				System.arraycopy(this.x, 0,
						dummy, 0, pointCount);
				this.x = dummy;
				dummy = new float[newLength];
				System.arraycopy(this.y, 0,
						dummy, 0, pointCount);
				this.y = dummy;
			}
			this.x[pointCount] = x;
			this.y[pointCount++] = y;
		}

		void addLine(int[] line) {
			if (lineCount + 1 > lines.length) {
				int newLength = lineCount + 32;
				int[][] dummy = new int[newLength][];
				System.arraycopy(lines, 0,
						dummy, 0, lineCount);
				lines = dummy;
			}
			lines[lineCount++] = line;
		}
	}

	public static void main(String[] args) {
		if (args.length != 3 && args.length != 4) {
			System.err.println("Usage: "
				+ "prog <original-tem> <incomplete-tem>"
				+ " <output-tem>");
			System.err.println("       "
				+ "prog -scale <factor> <original-tem> "
				+ " <output-tem>");
			System.exit(1);
		}

		TEM tem1 = new TEM(), tem2 = new TEM();
		if (args[0].equals("-scale")) {
			float factor = Float.parseFloat(args[1]);
			try {
				tem1.readFile(args[2]);
				for (int i = 0; i < tem1.pointCount; i++) {
					tem1.x[i] *= factor;
					tem1.y[i] *= factor;
				}
				tem1.writeFile(args[3]);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			System.exit(0);
		}

		try {
			tem1.readFile(args[0]);
			tem2.readFile(args[1]);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Moving_Least_Squares.Method m = Moving_Least_Squares.getMethod(
				Moving_Least_Squares.RIGID);
		int n1 = tem1.pointCount, n2 = tem2.pointCount;
		m.setCoordinates(tem1.x, tem1.y, tem2.x, tem2.y, n2);
		for (int i = n2; i < n1; i++) {
			m.calculate(tem1.x[i], tem1.y[i]);
			if (m.resultX < 1)
				m.resultX = 1;
			if (m.resultY < 1)
				m.resultY = 1;
			tem2.addPoint(m.resultX, m.resultY);
		}
		for (int i = tem2.lineCount; i < tem1.lineCount; i++)
			tem2.addLine(tem1.lines[i]);
		try {
			tem2.writeFile(args[2]);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}


