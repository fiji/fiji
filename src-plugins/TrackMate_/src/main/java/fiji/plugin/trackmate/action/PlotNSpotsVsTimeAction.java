package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class PlotNSpotsVsTimeAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/plots.png"));
	public static final String NAME = "Plot N spots vs time";
	public static final String INFO_TEXT =  "<html>" +
			"Plot the number of spots in each frame as a function <br>" +
			"of time. Only the filtered spots are taken into account. " +
			"</html>";

	public PlotNSpotsVsTimeAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMate_ plugin) {
		// Collect data
		final TrackMateModel model = plugin.getModel();
		final double dt = model.getSettings().dt;
		final SpotCollection spots = model.getFilteredSpots();
		final int nFrames = spots.keySet().size();
		final double[][] data = new double[2][nFrames];
		int index = 0;
		for (int frame : spots.keySet()) {
			data[0][index] = frame*dt;
			data[1][index] = spots.get(frame).size();
			index++;
		}
		
		// Plot data
		String xAxisLabel = "Time ("+model.getSettings().timeUnits+")";
		String yAxisLabel = "N spots";
		String title = "Nspots vs Time for "+model.getSettings().imp.getShortTitle();
		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Nspots", data);
		
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getTitle().setFont(FONT);
		chart.getLegend().setItemFont(SMALL_FONT);

		// The plot
		XYPlot plot = chart.getXYPlot();
//		plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont(FONT);
		plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		plot.getDomainAxis().setLabelFont(FONT);
		plot.getDomainAxis().setTickLabelFont(SMALL_FONT);
		
		ExportableChartPanel panel = new ExportableChartPanel(chart);
		
		JFrame frame = new JFrame(title);
		frame.setSize(500, 270);
		frame.getContentPane().add(panel);
		frame.setVisible(true);
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

}
