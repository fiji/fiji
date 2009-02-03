package register_virtual_stack;

/** Albert Cardona 2008. This work released under the terms of the General Public License in its latest edition. */
/** Greg Jefferis enhanced progress report and interaction with the user. */

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.VirtualStack;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.FileSaver;

import ini.trakem2.ControlWindow;
import ini.trakem2.Project;
import ini.trakem2.display.Layer;
import ini.trakem2.display.Patch;
import ini.trakem2.imaging.StitchingTEM;
import ini.trakem2.imaging.Registration;
import ini.trakem2.persistence.Loader;
import ini.trakem2.utils.IJError;
import ini.trakem2.utils.Utils;
import mpi.fruitfly.general.MultiThreading;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;

import java.util.Arrays;

/** Requires: a directory with images, all of the same dimensions
 *  Performs: registration of one image to the next, by phase- and cross-correlation or by SIFT
 *  Outputs: the list of new images, one for slice, into a target directory as .tif files.
 */
public class Register_Virtual_Stack_MT implements PlugIn {

	// Registration types
	static public final int PHASE_CORRELATION = 0;
	static public final int SIFT = 1;

	public void run(String arg) {

		GenericDialog gd = new GenericDialog("Options");
		String[] types = {"translation only (phase-correlation)",
			          "translation and rotation (SIFT)"};
		gd.addChoice("Registration: ", types, types[0]);
		gd.addSlider("Scaling % (for performance): ", 1, 100, 25);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		final int registration_type = gd.getNextChoiceIndex();
		// parameter to scale down images (1.0 means no scaling)
		final float scale = (float)gd.getNextNumber() / 100.0f;

		final Registration.SIFTParameters sp = (1 == registration_type ? new Registration.SIFTParameters() : null );
		if (null != sp) {
			sp.scale = scale;
			sp.setup();
		}

		DirectoryChooser dc = new DirectoryChooser("Source images");
		String source_dir = dc.getDirectory();
		if (null == source_dir) return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";

		dc = new DirectoryChooser("Target folder");
		String target_dir = dc.getDirectory();
		if (null == target_dir) return;
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";

		if (source_dir.equals(target_dir)) {
			IJ.showMessage("Source and target directories MUST be different\n or images would get overwritten.");
			return;
		}
		exec(source_dir, target_dir, registration_type, sp, scale, StitchingTEM.DEFAULT_MIN_R);
	}

	/** @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 *  @param target_fir Directory to store registered slices into.
	 *  @param registration_type Either PHASE_CORRELATION or SIFT (0 or 1)
	 *  @param sp The ini.trakem2.imaging.Registration.SIFTParameters class instance containing all SIFT parameters. Can be null only if not using SIFT as @param registration_type.
	 *  @param scale The scale at which phase-correlation should be executed.
	 *  @param min_R The minimal acceptable cross-correlation score, from 0 to 1, to evaluate the goodness of a phase-correlation.
	 */
	static public void exec(final String source_dir, final String target_dir, final int registration_type, final Registration.SIFTParameters sp, final float scale, final float min_R) {
		if (SIFT == registration_type && null == sp) {
			System.out.println("Can't use a null sp.");
			return;
		}
		// get file listing
		final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
		final String[] names = new File(source_dir).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return exts.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(names);

		try {
			// disable TrakEM2 windows
			ControlWindow.setGUIEnabled(false);
			// create temporary host project
			final Project project = Project.newFSProject("blank", null, source_dir);
			final Loader loader = project.getLoader();
			// disable mipmaps
			loader.setMipMapsRegeneration(false);
			// create a layer to work on
			// no need //Layer layer = project.getRootLayerSet().getLayer(0, 1, true);

			if (null != sp) sp.project = project;

			// Open first image
			ImagePlus first = new Opener().openImage(source_dir + names[0]);
			final int width = first.getWidth();
			final int height = first.getHeight();
			final int type = first.getType();
			final double min = first.getProcessor().getMin();
			final double max = first.getProcessor().getMax();
			final boolean color = (type == ImagePlus.COLOR_RGB);
			first = null; // don't interfere with memory management

			// create all patches. Images are NOT loaded
			IJ.showStatus("Loading Image Patches ...");
			final ArrayList<Patch> pa = new ArrayList<Patch>();

			for (int i=0; i<names.length; i++) {
				if (!new File(source_dir + names[i]).exists()) {
					System.out.println("Ignoring image " + names[i]);
					continue;
				}
				Patch patch = new Patch(project, loader.getNextId(), names[i], width, height, type, false, min, max, new AffineTransform());
				loader.addedPatchFrom(source_dir + names[i], patch);
				pa.add(patch);
			}

			// find out the affine transform of each slice to the previous
			final AffineTransform[] affine = new AffineTransform[pa.size()];
			affine[0] = new AffineTransform(); // first slice doesn't move

			final Thread[] threads = MultiThreading.newThreads();
			final AtomicInteger ai = new AtomicInteger(1); // start at second slice
			final AtomicInteger finished = new AtomicInteger(0);

			for (int ithread = 0; ithread < threads.length; ++ithread) {
				threads[ithread] = new Thread() { public void run() { setPriority(Thread.NORM_PRIORITY);

					for (int i = ai.getAndIncrement(); i < affine.length; i = ai.getAndIncrement()) {
						IJ.showStatus("Computing transform for slice (" + i + "/" + affine.length + ")");
						if (IJ.debugMode) IJ.log("Computing transform for slice (" + i + "/" + affine.length + ")");
						IJ.showProgress(finished.get(), affine.length);

						Patch prev = pa.get(i-1);
						Patch next = pa.get(i);
						// will load images on its own (only once for each, guaranteed)
						loader.releaseToFit(width * height * (ImagePlus.GRAY8 == type ? 1 : 5) * threads.length * 6);
						if ( 0 == registration_type ) {
							double[] c = StitchingTEM.correlate(prev, next, 1.0f, scale, StitchingTEM.TOP_BOTTOM, 0, 0, min_R);
							affine[i] = new AffineTransform();
							affine[i].translate(c[0], c[1]);
						} else if ( 1 == registration_type ) {
							Object[] result = Registration.registerWithSIFTLandmarks(prev, next, sp, null, false, true);
							affine[i] = (null == result ? new AffineTransform() : (AffineTransform)result[2]);
						}
						IJ.showProgress(finished.incrementAndGet(), affine.length);
					}

				}};
			}
			// wait until all threads finished
			MultiThreading.startAndJoin(threads);

			IJ.showProgress(0);

			// determine maximum canvas, make affines global by concatenation
			final Rectangle box = new Rectangle(0, 0, width, height);
			final Rectangle one = new Rectangle(0, 0, width, height);

			for (int i=1; i<affine.length; i++) {
				affine[i].concatenate(affine[i-1]);
				box.add(affine[i].createTransformedShape(one).getBounds());
				// reset
				one.setRect(0, 0, width, height);
			}
			box.width = box.width - box.x;
			box.height = box.height - box.y;
			final AffineTransform trans = new AffineTransform();
			trans.translate(-box.x, -box.y);
			box.x = box.y = 0;

			// fix stupid java defaults for indexed images, which are not purely grayscale
			final byte[] r = new byte[256];
			final byte[] g = new byte[256];
			final byte[] b = new byte[256];
			for (int i=0; i<256; i++) {
				r[i]=(byte)i;
				g[i]=(byte)i;
				b[i]=(byte)i;
			}
			final IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);

			// output images as 8-bit or RGB
			final Thread[] threads2 = MultiThreading.newThreads();
			final AtomicInteger ai2 = new AtomicInteger(0);

			final String[] tiffnames = new String[affine.length];

			for (int ithread = 0; ithread < threads2.length; ++ithread) {
				threads2[ithread] = new Thread() { public void run() { setPriority(Thread.NORM_PRIORITY);

					for (int i = ai2.getAndIncrement(); i < affine.length; i = ai2.getAndIncrement()) {
						affine[i].concatenate(trans);
						// ensure enough free memory
						loader.releaseToFit(width * height * (ImagePlus.GRAY8 == type ? 1 : 5) * threads2.length * 6); // 3: 1 processor + 1 image + 1 for safety , times 2 ...
						BufferedImage bi;
						Graphics2D g;
						if (color) {
							bi = new BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_ARGB);
							g = bi.createGraphics();
							g.setColor(Color.black);
							g.fillRect(0, 0, box.width, box.height);
						} else {
							bi = new BufferedImage(box.width, box.height, BufferedImage.TYPE_BYTE_INDEXED, icm);
							g = bi.createGraphics();
						}
						Patch patch = pa.get(i);
						g.drawImage(loader.fetchImage(patch, 1.0), affine[i], null);
						ImagePlus imp = new ImagePlus(patch.getTitle(), bi);
						// Trim off file extension
						String slice_name = patch.getTitle();
						int idot = slice_name.lastIndexOf('.');
						if (idot > 0) slice_name = slice_name.substring(0, idot);
						tiffnames[i] = slice_name + ".tif";
						new FileSaver(imp).saveAsTiff(target_dir + tiffnames[i]);

						if (0 == i % threads.length) {
							IJ.showStatus("Saving slice ("+i+"/"+affine.length+")");
							IJ.showProgress(i, affine.length);
						}
					}
				}};
			}
			// wait until all threads finished
			MultiThreading.startAndJoin(threads2);

			IJ.showStatus("done.");
			IJ.showProgress(1.0);

			project.destroy();

			// open virtual stack
			VirtualStack vs = new VirtualStack(box.width, box.height, icm, target_dir);
			for (int i=0; i<tiffnames.length; i++) vs.addSlice(tiffnames[i]);
			new ImagePlus("Registered", vs).show();

		} catch (Exception e) {
			IJError.print(e);
		} finally {
			ControlWindow.setGUIEnabled(false);
			IJ.showProgress(0);
		}
	}
}
