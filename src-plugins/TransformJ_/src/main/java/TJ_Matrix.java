import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;
import java.util.Vector;
import imagescience.image.Axes;
import imagescience.transform.Transform;
import imagescience.utility.Formatter;

public class TJ_Matrix implements PlugIn, ActionListener, ClipboardOwner, FocusListener, WindowListener {
	
	private Dialog dialog; Panel panel;
	private TextField[][] tfs;
	
	private Button rotate, scale, shear, trans;
	private Button invert, reset, copy, print;
	private Button undo, load, save, close;
	
	private Transform tpr = null;
	private static final Transform tfm = new Transform();
	private final Formatter fmt = new Formatter();
	private String saved = null;
	
	private static final Point pos = new Point(-1,-1);
	private static final Point rpos = new Point(-1,-1);
	private static final Point spos = new Point(-1,-1);
	private static final Point tpos = new Point(-1,-1);
	private static final Point hpos = new Point(-1,-1);
	
	private static String rangle = "0.0";
	private static String sfactor = "1.0";
	private static String tdistance = "0.0";
	private static String hfactor = "1.0";
	
	private static final String[] axes = { "x", "y", "z" };
	
	private static int raxis = 0;
	private static int saxis = 0;
	private static int taxis = 0;
	private static int haxis = 0;
	private static int daxis = 0;
	
	public void run(String arg) {
		
		if (!TJ.libcheck()) return;
		
		TJ.log(TJ.name()+" "+TJ.version()+": Matrix");
		
		final Frame parent = (IJ.getInstance() != null) ? IJ.getInstance() : new Frame();
		dialog = new Dialog(parent,TJ.name()+": Matrix",true);
		dialog.setLayout(new FlowLayout());
		dialog.addWindowListener(this);
		
		panel = new Panel();
		panel.setLayout(new GridLayout(7,4,5,5));
		
		tfs = new TextField[4][4];
		for (int r=0; r<4; ++r) {
			for (int c=0; c<4; ++c) {
				tfs[r][c] = addField();
				if (r == 3) tfs[r][c].setEditable(false);
			}
		}
		
		rotate = addButton("Rotate");
		scale = addButton("Scale");
		shear = addButton("Shear");
		trans = addButton("Translate");
		
		invert = addButton("Invert");
		reset = addButton("Reset");
		copy = addButton("Copy");
		print = addButton("Print");
		
		undo = addButton("Undo");
		load = addButton("Load");
		save = addButton("Save");
		close = addButton("Close");
		
		fmt.decs(10);
		fmt.chop(1E-10);
		refresh();
		dialog.add(panel);
		dialog.pack();
		if (pos.x < 0 || pos.y < 0) GUI.center(dialog);
		else dialog.setLocation(pos);
		dialog.setVisible(true);
	}
	
	private Button addButton(String label) {
		
		final Button bt = new Button("   "+label+"   ");
		bt.addActionListener(this);
		panel.add(bt);
		return bt;
	}
	
	private TextField addField() {
		
		final TextField tf = new TextField(10);
		tf.addFocusListener(this);
		tf.setEditable(true);
		panel.add(tf);
		return tf;
	}
	
	private void refresh() {
		
		for (int r=0; r<4; ++r) {
			for (int c=0; c<4; ++c) {
				tfs[r][c].setText(fmt.d2s(tfm.get(r,c)));
			}
		}
	}
	
	private String string(final String prefix, final String delim, final String postfix) {
		
		final StringBuffer sb = new StringBuffer();
		
		for (int r=0; r<4; ++r) {
			sb.append(prefix);
			for (int c=0; c<4; ++c) {
				sb.append(fmt.d2s(tfm.get(r,c)));
				if (c < 3) sb.append(delim);
			}
			sb.append(postfix);
		}
		
		return sb.toString();
	}
	
	private String file(final int mode) {
		
		String file = null;
		final String m = (mode == FileDialog.LOAD) ? "Load" : "Save";
		final FileDialog fdg = new FileDialog(IJ.getInstance(),TJ.name()+": "+m,mode);
		fdg.setFile(""); fdg.setVisible(true);
		final String d = fdg.getDirectory();
		final String f = fdg.getFile();
		fdg.dispose();
		if (d != null && f != null) {
			file = d + f;
			if (File.separator.equals("\\"))
				file = file.replace('\\','/');
		}
		return file;
	}
	
	Transform get() {
		
		return tfm.duplicate();
	}
	
	void set(final Transform a) {
		
		tfm.set(a);
	}
	
	void load(final String file) {
		
		// Read lines:
		final Vector lines = new Vector(10,10);
		String line = null;
		try {
			final BufferedReader br = new BufferedReader(new FileReader(file));
			line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.equals(""))
					lines.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Unable to find "+file);
		} catch (Throwable e) {
			throw new IllegalArgumentException("Error reading from "+file);
		}
		
		// Convert lines:
		if (lines.size() != 4)
			throw new IllegalArgumentException("File "+file+" does not contain a 4 x 4 matrix");
		String delim = "\t";
		line = (String)lines.get(0);
		if (line.indexOf(",") >= 0) delim = ",";
		else if (line.indexOf(" ") >= 0) delim = " ";
		final double[][] matrix = new double[4][4];
		for (int r=0; r<4; ++r) {
			line = (String)lines.get(r);
			final StringTokenizer st = new StringTokenizer(line,delim);
			if (st.countTokens() != 4)
				throw new IllegalArgumentException("File "+file+" does not contain a 4 x 4 matrix");
			for (int c=0; c<4; ++c) {
				try {
					matrix[r][c] = Double.parseDouble(st.nextToken());
				} catch (Throwable e) {
					throw new IllegalArgumentException("Error reading element ("+r+","+c+") in "+file);
				}
			}
		}
		
		// Store matrix:
		tfm.set(matrix);
	}
	
	void save(final String file) {
		
		try {
			final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(string("","\t","\n"));
			bw.close();
			saved = file;
		} catch (Throwable e) {
			throw new IllegalArgumentException("Error writing to "+file);
		}
	}
	
	String saved() {
		
		return saved;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		final Object source = e.getSource();
		
		if (source == rotate) {
			final GenericDialog gd = new GenericDialog(TJ.name()+": Rotate");
			gd.addStringField("Rotation angle (degrees):",rangle);
			gd.addChoice("Rotation axis:",axes,axes[raxis]);
			if (rpos.x >= 0 && rpos.y >= 0) {
				gd.centerDialog(false);
				gd.setLocation(rpos);
			} else gd.centerDialog(true);
			gd.showDialog();
			gd.getLocation(rpos);
			if (!gd.wasCanceled()) {
				rangle = gd.getNextString();
				raxis = gd.getNextChoiceIndex();
				try {
					int axis = Axes.X;
					if (raxis == 1) axis = Axes.Y;
					else if (raxis == 2) axis = Axes.Z;
					double angle = Double.parseDouble(rangle);
					tpr = tfm.duplicate();
					tfm.rotate(angle,axis);
					refresh();
					TJ.log("Rotated the matrix "+rangle+" degrees around "+axes[raxis]);
				} catch (Exception x) {
					TJ.error("Invalid rotation angle");
				}
			}
			
		} else if (source == scale) {
			final GenericDialog gd = new GenericDialog(TJ.name()+": Scale");
			gd.addStringField("Scaling factor:",sfactor);
			gd.addChoice("Scaling axis:",axes,axes[saxis]);
			if (spos.x >= 0 && spos.y >= 0) {
				gd.centerDialog(false);
				gd.setLocation(spos);
			} else gd.centerDialog(true);
			gd.showDialog();
			gd.getLocation(spos);
			if (!gd.wasCanceled()) {
				sfactor = gd.getNextString();
				saxis = gd.getNextChoiceIndex();
				try {
					int axis = Axes.X;
					if (saxis == 1) axis = Axes.Y;
					else if (saxis == 2) axis = Axes.Z;
					double factor = Double.parseDouble(sfactor);
					tpr = tfm.duplicate();
					tfm.scale(factor,axis);
					refresh();
					TJ.log("Scaled the matrix by a factor of "+sfactor+" in "+axes[saxis]);
				} catch (Exception x) {
					TJ.error("Invalid scaling factor");
				}
			}
			
		} else if (source == shear) {
			final GenericDialog gd = new GenericDialog(TJ.name()+": Shearing");
			gd.addStringField("Shearing factor:",hfactor);
			gd.addChoice("Shearing axis:",axes,axes[haxis]);
			gd.addChoice("Driving axis:",axes,axes[daxis]);
			if (hpos.x >= 0 && hpos.y >= 0) {
				gd.centerDialog(false);
				gd.setLocation(hpos);
			} else gd.centerDialog(true);
			gd.showDialog();
			gd.getLocation(hpos);
			if (!gd.wasCanceled()) {
				hfactor = gd.getNextString();
				haxis = gd.getNextChoiceIndex();
				daxis = gd.getNextChoiceIndex();
				try {
					int axis = Axes.X;
					if (haxis == 1) axis = Axes.Y;
					else if (haxis == 2) axis = Axes.Z;
					int drive = Axes.X;
					if (daxis == 1) drive = Axes.Y;
					else if (daxis == 2) drive = Axes.Z;
					double factor = Double.parseDouble(hfactor);
					tpr = tfm.duplicate();
					tfm.shear(factor,axis,drive);
					refresh();
					TJ.log("Sheared the matrix by a factor of "+hfactor+" in "+axes[haxis]+" by "+axes[daxis]);
				} catch (Exception x) {
					TJ.error("Invalid shearing factor");
				}
			}
			
		} else if (source == trans) {
			final GenericDialog gd = new GenericDialog(TJ.name()+": Translate");
			gd.addStringField("Translation distance:",tdistance);
			gd.addChoice("Translation axis:",axes,axes[taxis]);
			if (tpos.x >= 0 && tpos.y >= 0) {
				gd.centerDialog(false);
				gd.setLocation(tpos);
			} else gd.centerDialog(true);
			gd.showDialog();
			gd.getLocation(tpos);
			if (!gd.wasCanceled()) {
				tdistance = gd.getNextString();
				taxis = gd.getNextChoiceIndex();
				try {
					int axis = Axes.X;
					if (taxis == 1) axis = Axes.Y;
					else if (taxis == 2) axis = Axes.Z;
					double distance = Double.parseDouble(tdistance);
					tpr = tfm.duplicate();
					tfm.translate(distance,axis);
					refresh();
					TJ.log("Translated the matrix by "+tdistance+" in "+axes[taxis]);
				} catch (Exception x) {
					TJ.error("Invalid translation distance");
				}
			}
			
		} else if (source == invert) {
			try {
				final Transform tmp = tfm.duplicate();
				tfm.invert();
				refresh();
				tpr = tmp;
				TJ.log("Inverted the matrix");
			} catch (Throwable x) {
				TJ.error(x.getMessage());
			}
			
		} else if (source == reset) {
			tpr = tfm.duplicate();
			tfm.reset();
			refresh();
			TJ.log("Reset the matrix to the identity matrix");
			
		} else if (source == copy) {
			try {
				final StringSelection ss = new StringSelection(string("","\t","\n"));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss,this);
				TJ.log("Copied the matrix to the clipboard");
				TJ.status("Copied matrix to clipboard");
			} catch (Throwable x) {
				TJ.error("Failed to copy the matrix to the clipboard");
			}
			
		} else if (source == print) {
			IJ.log(string("[  ","   ","  ]\n"));
			
		} else if (source == undo) {
			if (tpr != null) {
				final Transform tmp = tfm.duplicate();
				tfm.set(tpr);
				refresh();
				tpr = tmp;
				TJ.log("Undone last change");
			}
			
		} else if (source == load) {
			final String file = file(FileDialog.LOAD);
			if (file != null) try {
				final Transform tmp = tfm.duplicate();
				load(file);
				refresh();
				tpr = tmp;
				TJ.log("Loaded matrix from "+file);
				TJ.status("Loaded matrix from "+file);
			} catch (Throwable x) {
				TJ.error(x.getMessage());
			}
			
		} else if (source == save) {
			final String file = file(FileDialog.SAVE);
			if (file != null) try {
				save(file);
				TJ.log("Saved matrix to "+file);
				TJ.status("Saved matrix to "+file);
			} catch (Throwable x) {
				TJ.error(x.getMessage());
			}
			
		} else if (source == close) {
			dialog.setVisible(false);
			dialog.dispose();
		}
	}
	
	public void focusGained(final FocusEvent e) {
		
		final Object source = e.getComponent();
		if (source instanceof TextField) {
			((TextField)source).selectAll();
		}
	}
	
	public void focusLost(final FocusEvent e) {
		
		final Object source = e.getSource();
		if (source instanceof TextField) {
			final TextField tf = (TextField)source;
			tf.select(0,0);
			for (int r=0; r<4; ++r) {
				for (int c=0; c<4; ++c) {
					if (tfs[r][c] == tf) {
						try {
							final double d = Double.parseDouble(tf.getText());
							if (d != tfm.get(r,c)) {
								tpr = tfm.duplicate();
								tfm.set(r,c,d);
								refresh();
								TJ.log("Updated matrix");
							}
						} catch (Throwable x) {
							TJ.error("Invalid input value (will be reverted)");
							refresh();
						}
						return;
					}
				}
			}
		}
	}
	
 	public void lostOwnership(final Clipboard clip, final Transferable contents) { }
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) {
		
		dialog.setVisible(false);
		dialog.dispose();
	}
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}
