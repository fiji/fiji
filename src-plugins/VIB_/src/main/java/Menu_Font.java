import ij.IJ;
import ij.ImageJ;
import ij.Menus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.awt.Font;
import java.awt.Menu;
import java.awt.MenuBar;

public class Menu_Font implements ij.plugin.PlugIn {
	public void run(String arg) {
		GenericDialog gd = new GenericDialog("New Menu Font Size");
		gd.addNumericField("menuFontSize", 16, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int size = (int)gd.getNextNumber();
		MenuBar menuBar = Menus.getMenuBar();
		Font font = menuBar.getFont();
		int oldSize = font.getSize();

		// this does not work, because default is "fixed"
		//menuBar.setFont(font.deriveFont(size));
		menuBar.setFont(Font.decode("sansserif-" + size));

		// work around AWT not recalculating the menu bar size
		int i, count = menuBar.getMenuCount();
		Menu[] menus = new Menu[count];
		for (i = 0; i < count; i++) {
			menus[i] = menuBar.getMenu(0);
			menuBar.remove(menus[i]);
		}
		for (i = 0; i < count; i++)
			menuBar.add(menus[i]);
		ImageJ ij = IJ.getInstance();
		ij.pack();
		ij.setSize(new java.awt.Dimension(ij.getWidth()
					* size / oldSize, ij.getHeight()));
	}
}
