import ij.gui.Plot;
import ij.gui.PlotWindow;

import ij.plugin.PlugIn;

import java.awt.Color;

/**
 * An example how to use ImageJ's Plot class
 */
public class Plot_Example implements PlugIn {
	public void run(String arg) {
		// Some data to show
		double[] x = { 1, 3, 4, 5, 6, 7, 8, 9, 11 };
		double[] y = { 20, 5, -2, 3, 10, 12, 8, 3, 0 };
		double[] y2 = { 18, 10, 3, 1, 7, 11, 11, 5, 2 };
		double[] x3 = { 2, 10 };
		double[] y3 = { 13, 3 };

		// Initialize the plot with x/y
		Plot plot = new Plot("Example plot", "x", "y", x, y);

		// make some margin (xMin, xMax, yMin, yMax)
		plot.setLimits(0, 12, -3, 21);

		// Add x/y2 in blue; need to draw the previous data first
		plot.draw();
		plot.setColor(Color.BLUE);
		plot.addPoints(x, y2, Plot.LINE);

		// Add x3/y3 as circles instead of connected lines
		plot.draw();
		plot.setColor(Color.BLACK);
		plot.addPoints(x3, y3, Plot.CIRCLE);

		// Finally show it, but remember the window
		PlotWindow window = plot.show();

		// Wait 5 seconds
		try { Thread.sleep(5000); } catch (InterruptedException e) {}

		// Make a new plot and update the window
		plot = new Plot("Example plot2", "x", "y", x, y2);
		plot.setLimits(0, 12, -3, 21);
		plot.draw();
		plot.setColor(Color.GREEN);
		plot.addPoints(x, y, Plot.CROSS);
		plot.draw();
		plot.setColor(Color.RED);
		plot.addPoints(x3, y3, Plot.LINE);
		window.drawPlot(plot);
	}
}