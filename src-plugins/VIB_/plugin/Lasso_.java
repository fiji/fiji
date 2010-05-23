package plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import ij.plugin.filter.ThresholdToSelection;

import java.util.TreeMap;

public class Lasso_ implements PlugIn {
	public static final String MACRO_CMD =
		"var clicked = 0;\n" +
		"var spacePressed = 0;\n" +
		"var leftClick = 16;\n" +
		"var currentX = -1;\n" +
		"var currentY = -1;\n" +
		"\n" +
		"macro 'Lasso Tool - C000Pdaa79796a6c4c2a1613215276998a6a70' {\n" +
		"  tool = toolID();\n" +
		"  while (tool == toolID()) {\n" +
		"    if (!spacePressed) {\n" +
		"        if (isKeyDown('space'))\n" +
		"            spacePressed = 1;\n" +
		"    } else {\n" +
		"        if (!isKeyDown('space')) {\n" +
		"            spacePressed = 0;\n" +
		"            call('plugin.Lasso_.toggleMode');\n" +
		"        }\n" +
		"    }\n" +
		"    getCursorLoc(x, y, z, flags);\n" +
		"    if (!clicked) {\n" +
		"        if ((flags & leftClick) != 0) {\n" +
		"            clicked = 1;\n" +
		"            call('plugin.Lasso_.start', x, y);\n" +
		"        }\n" +
		"    } else {\n" +
		"        if ((flags & leftClick) == 0)\n" +
		"            clicked = 0;\n" +
		"        else if (x != currentX || y != currentY) {\n" +
		"            call('plugin.Lasso_.move', x, y);\n" +
		"            currentX = x;\n" +
		"            currentY = y;\n" +
		"        }\n" +
		"    }\n" +
		"    wait(100);\n" +
		"  }\n" +
		"}\n" +
		"\n" +
		"macro 'Lasso Tool Options' {\n" +
		"    call('plugin.Lasso_.callOptionDialog');\n" +
		"}\n";

	protected boolean macroInstalled = false;

	public void run(String arg){
		if (IJ.versionLessThan("1.37j"))
			return;

		if (macroInstalled)
			return;
		MacroInstaller installer = new MacroInstaller();
		installer.install(MACRO_CMD);
		Toolbar.getInstance().setTool(Toolbar.SPARE1);
		macroInstalled = true;
	}

	private static Lasso instance;

	public synchronized static void setMode(int mode) {
		if (instance == null || IJ.getImage() != instance.getImage())
			instance = new Lasso(IJ.getImage(), mode);
		else instance.setMode(mode);
		IJ.showStatus(Lasso.modeTitles[mode]);
	}

	public synchronized static void setMode(String mode) {
		if (mode.equals("lasso"))
			setMode(Lasso.LASSO);
		else if (mode.equals("blow"))
			setMode(Lasso.BLOW);
		else
			IJ.error("Unknown Lasso/Blow mode: " + mode);
	}

	public synchronized static void toggleMode() {
		int mode = instance == null ? 0 : instance.getMode();
		setMode((mode + 1) % (Lasso.MAX_TOOL + 1));
	}

	public synchronized static void callOptionDialog() {
		if (instance == null || IJ.getImage() != instance.getImage())
			instance = new Lasso(IJ.getImage());
		instance.optionDialog();
	}

	public synchronized static void start(String x_, String y_) {
		if (null == instance || IJ.getImage() != instance.getImage()) {
			instance = new Lasso(IJ.getImage());
		}
		int x = (int)Float.parseFloat(x_);
		int y = (int)Float.parseFloat(y_);
		instance.initDijkstra(x, y, IJ.shiftKeyDown());
	}

	public synchronized static void move(String x_, String y_) {
		int x = (int)Float.parseFloat(x_);
		int y = (int)Float.parseFloat(y_);
		if (x < 0) x = 0;
		if (x >= instance.w) x = instance.w - 1;
		if (y < 0) y = 0;
		if (y >= instance.h) y = instance.h - 1;
		try {
				if (instance.getMode() == Lasso.BLOW)
					instance.moveBlow(x, y);
				else
					instance.moveLasso(x, y);
		} catch (Throwable t) {
			System.err.println("Caught throwable " + t);
			t.printStackTrace();
		}
	}
}
