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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class PlotNSpotsVsTimeAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/plots.png"));
	public static final String NAME = "Plot N spots vs time";
	public static final String INFO_TEXT =  "<html>" +
			"Plot the number of spots in each frame as a function <br>" +
			"of time. Only the filtered spots are taken into account. " +
			"</html>";

	public PlotNSpotsVsTimeAction(final TrackMate trackmate, final TrackMateGUIController controller) {
		super(trackmate, controller);
		this.icon = ICON;
	}

	@Override
	public void execute() {
		// Collect data
		final Model model = trackmate.getModel();
		final Settings settings = trackmate.getSettings();
		final SpotCollection spots = model.getSpots();
		final int nFrames = spots.keySet().size();
		final double[][] data = new double[2][nFrames];
		int index = 0;
		for (final int frame : spots.keySet()) {
			data[1][index] = spots.getNSpots(frame, true);
			if (data[1][index] > 0) {
				data[0][index] = spots.iterator(frame, false).next().getFeature(Spot.POSITION_T);
			} else {
				data[0][index] = frame * settings.dt;
			}
			index++;
		}

		// Plot data
		final String xAxisLabel = "Time ("+trackmate.getModel().getTimeUnits()+")";
		final String yAxisLabel = "N spots";
		final String title = "Nspots vs Time for "+trackmate.getSettings().imp.getShortTitle();
		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Nspots", data);

		final JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getTitle().setFont(FONT);
		chart.getLegend().setItemFont(SMALL_FONT);

		// The plot
		final XYPlot plot = chart.getXYPlot();
//		plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont(FONT);
		plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		plot.getDomainAxis().setLabelFont(FONT);
		plot.getDomainAxis().setTickLabelFont(SMALL_FONT);

		final ExportableChartPanel panel = new ExportableChartPanel(chart);

		final JFrame frame = new JFrame(title);
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
