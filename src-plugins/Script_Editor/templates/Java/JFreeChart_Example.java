
import ij.IJ;
import ij.ImagePlus;

import ij.plugin.PlugIn;

import java.awt.Font;

import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.plot.PiePlot;

import org.jfree.data.general.DefaultPieDataset;

public class JFreeChart_Example implements PlugIn {
	public void run(String arg) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		dataset.setValue("Apples", 63);
		dataset.setValue("Oranges", 36);

		JFreeChart chart = ChartFactory.createPieChart("Comparison", dataset, true, true, false);
		PiePlot pie = (PiePlot)chart.getPlot();
		pie.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		pie.setLabelGap(0.05);
		pie.setCircular(true);

		ImagePlus imp = IJ.createImage("Comparison", "RGB", 512, 512, 1);
		BufferedImage image = imp.getBufferedImage();
		chart.draw(image.createGraphics(),
			new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
		imp.setImage(image);
		imp.show();
	}
}