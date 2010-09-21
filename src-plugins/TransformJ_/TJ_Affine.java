import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.image.Image;
import imagescience.transform.Affine;
import imagescience.transform.Transform;
import java.awt.Button;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

public class TJ_Affine implements PlugIn, ActionListener, WindowListener {
	
	private static String file = "";
	
	private static final String[] schemes = {
		"nearest neighbor",
		"linear",
		"cubic convolution",
		"cubic B-spline",
		"cubic O-MOMS",
		"quintic B-spline"
	};
	private static int scheme = 1;
	
	private static String bgvalue = "0.0";
	
	private static boolean adjust = true;
	private static boolean antialias = false;
	
	private Button browseButton, createButton;
	private TextField fileField;
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		final ImagePlus imp = TJ.imageplus();
		if (imp == null) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Affine");
		
		GenericDialog gd = new GenericDialog(TJ.name()+": Affine");
		gd.addStringField("Matrix file:",file,30);
		fileField = (TextField)gd.getStringFields().get(0);
		
		final Panel buttons = new Panel();
		GridBagLayout bgbl = new GridBagLayout();
		buttons.setLayout(bgbl);
		browseButton = new Button("    Browse    ");
		browseButton.addActionListener(this);
		createButton = new Button("     Create     ");
		createButton.addActionListener(this);
		GridBagConstraints bgbc = new GridBagConstraints();
		bgbc.anchor = GridBagConstraints.WEST;
		bgbc.insets = new Insets(0,0,0,5);
		bgbl.setConstraints(browseButton,bgbc);
		buttons.add(browseButton);
		bgbc.insets = new Insets(0,0,0,0);
		bgbl.setConstraints(createButton,bgbc);
		buttons.add(createButton);
		gd.addPanel(buttons,GridBagConstraints.WEST,new Insets(0,0,20,0));
		bgbl = (GridBagLayout)gd.getLayout();
		bgbc = bgbl.getConstraints(buttons); bgbc.gridx = 1;
		bgbl.setConstraints(buttons,bgbc);
		
		gd.addChoice("Interpolation scheme:",schemes,schemes[scheme]);
		gd.addStringField("Background value:",bgvalue);
		
		gd.addCheckbox(" Adjust size to fit result",adjust);
		gd.addCheckbox(" Anti-alias borders",antialias);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		file = gd.getNextString();
		scheme = gd.getNextChoiceIndex();
		bgvalue = gd.getNextString();
		adjust = gd.getNextBoolean();
		antialias = gd.getNextBoolean();
		
		(new TJAffine()).run(imp,file,scheme,bgvalue,adjust,antialias);
	}
	
	public void actionPerformed(final ActionEvent e) {
		
		if (e.getSource() == browseButton) {
			final FileDialog fdg = new FileDialog(IJ.getInstance(),TJ.name()+": Load",FileDialog.LOAD);
			fdg.setFile(""); fdg.setVisible(true);
			final String d = fdg.getDirectory();
			final String f = fdg.getFile();
			fdg.dispose();
			if (d != null && f != null) {
				String path = d + f;
				if (File.separator.equals("\\"))
					path = path.replace('\\','/');
				fileField.setText(path);
			}
		} else if (e.getSource() == createButton) {
			final TJ_Matrix tjm = new TJ_Matrix();
			try { tjm.load(fileField.getText()); }
			catch (Throwable x) { }
			tjm.run("");
			final String path = tjm.saved();
			if (path != null) fileField.setText(path);
		}
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) { }
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}

class TJAffine {
	
	void run(
		final ImagePlus imp,
		final String file,
		final int scheme,
		final String bgvalue,
		final boolean adjust,
		final boolean antialias
	) {
		
		try {
			if (file == null || file.equals(""))
				throw new IllegalArgumentException("Empty matrix file name");
			final TJ_Matrix tjm = new TJ_Matrix();
			tjm.load(file);
			final Transform tfm = tjm.get();
			final Image img = Image.wrap(imp);
			final Affine affiner = new Affine();
			affiner.messenger.log(TJ_Options.log);
			affiner.messenger.status(TJ_Options.pgs);
			affiner.progressor.display(TJ_Options.pgs);
			double bg;
			try { bg = Double.parseDouble(bgvalue); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid background value"); }
			affiner.background = bg;
			int ischeme = Affine.NEAREST;
			switch (scheme) {
				case 0: ischeme = Affine.NEAREST; break;
				case 1: ischeme = Affine.LINEAR; break;
				case 2: ischeme = Affine.CUBIC; break;
				case 3: ischeme = Affine.BSPLINE3; break;
				case 4: ischeme = Affine.OMOMS3; break;
				case 5: ischeme = Affine.BSPLINE5; break;
			}
			final Image newimg = affiner.run(img,tfm,ischeme,adjust,antialias);
			TJ.show(newimg,imp);
			
		} catch (OutOfMemoryError e) {
			TJ.error("Not enough memory for this operation");
			
		} catch (UnknownError e) {
			TJ.error("Could not create output image for some reason.\nPossibly there is not enough free memory");
			
		} catch (IllegalArgumentException e) {
			TJ.error(e.getMessage());
			
		} catch (Throwable e) {
			TJ.error("An unidentified error occurred while running the plugin");
			
		}
	}
	
}
