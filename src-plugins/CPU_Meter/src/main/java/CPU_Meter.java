import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.Plot;
import ij.gui.PlotWindow;

import ij.plugin.PlugIn;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;



public class CPU_Meter implements PlugIn {
	public interface CLibrary extends Library {
		CLibrary INSTANCE = (CLibrary)
			Native.loadLibrary(Platform.isWindows() ? "msvcrt" : "c", CLibrary.class);

		int getloadavg(double[] loadavg, int nelem);
	}

	public void run(String arg) {
		new CPUMeter().start();
	}

	public class CPUMeter extends Thread {
		protected double[] avg = new double[3];
		protected double[] x, y;
		protected long msecsBetweenUpdates;
		protected PlotWindow plotWindow;

		public CPUMeter() {
			this(50, IJ.isLinux() ? 5000 : 1000);
		}

		public CPUMeter(int ticks, long msecsBetweenUpdates) {
			x = new double[ticks];
			y = new double[ticks];
			this.msecsBetweenUpdates = msecsBetweenUpdates;
			for (int i = 0; i < ticks; i++)
				x[ticks - 1 - i] = -i * msecsBetweenUpdates / 1000.0;
		}

		public void run() {
			for (;;) try {
				update();
				Thread.sleep(msecsBetweenUpdates);
			} catch (InterruptedException e) {
				break;
			}
		}

		protected void update() {
			System.arraycopy(y, 1, y, 0, y.length - 1);
			double load = CLibrary.INSTANCE.getloadavg(avg, 1) == 1 ? avg[0] : 0;
			y[y.length - 1] = load;

			double yMin = y[0], yMax = y[0];
			for (int i = 1; i < y.length; i++)
				if (yMin> y[i])
					yMin = y[i];
				else if (yMax < y[i])
					yMax = y[i];
			double delta = yMin == yMax ? 0.1 : (yMax - yMin) * 0.05;
			yMin -= delta;
			yMax += delta;

			Plot plot = new Plot("CPU Meter", "seconds", "load", x, y);
			plot.setLimits(x[0], x[x.length - 1], yMin, yMax);
			if (plotWindow == null) {
				plotWindow = plot.show();
				plotWindow.addWindowListener(new WindowAdapter() {
					public void windowClosed(WindowEvent e) {
						interrupt();
					}
				});
			}
			else {
				plotWindow.drawPlot(plot);
			}
		}
	}
}