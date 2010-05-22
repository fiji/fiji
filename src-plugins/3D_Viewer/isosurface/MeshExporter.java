/** Albert Cardona and Bene Schmid 20070614 at Janelia Farms*/
package isosurface;

import ij3d.Content;
import ij3d.ContentNode;
import customnode.WavefrontExporter;
import customnode.CustomMeshNode;
import customnode.CustomMesh;
import customnode.CustomMultiMesh;

import ij.IJ;
import ij.io.SaveDialog;
import ij.gui.YesNoCancelDialog;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point3f;
import javax.vecmath.Color3f;


public class MeshExporter {

	private MeshExporter() {}

	static private Collection<Content> filterMeshes(final Collection contents) {
		ArrayList<Content> meshes = new ArrayList<Content>();
		for (Iterator it = contents.iterator(); it.hasNext(); ) {
			Content c = (Content)it.next();
			ContentNode node = c.getContent();
			if (node instanceof voltex.VoltexGroup
			 || node instanceof orthoslice.OrthoGroup
			 || node instanceof surfaceplot.SurfacePlotGroup) {
				continue;
			}
			meshes.add(c);
		}
		return meshes;
	}

	/** Accepts a collection of MeshGroup objects. */
	static public void saveAsWaveFront(Collection contents_) {
		if (null == contents_ || 0 == contents_.size())
			return;
		Collection<Content> contents = filterMeshes(contents_);
		if (0 == contents.size()) {
			IJ.log("No meshes to export!");
			return;
		}
		SaveDialog sd = new SaveDialog(
				"Save WaveFront", "untitled", ".obj");
		String dir = sd.getDirectory();
		if (null == dir)
			return;
		if (IJ.isWindows()) dir = dir.replace('\\', '/');
		if (!dir.endsWith("/")) dir += "/";
		String obj_filename = sd.getFileName();
		if (!obj_filename.toLowerCase().endsWith(".obj"))
			obj_filename += ".obj";

		File obj_file = new File(dir + "/" + obj_filename);
		// check if file exists
		if (!IJ.isMacOSX() && obj_file.exists()) {
			YesNoCancelDialog yn = new YesNoCancelDialog(
				IJ.getInstance(),
				"Overwrite?",
				"File  "+obj_filename+" exists!\nOverwrite?");
			if (!yn.yesPressed()) return;
		}

		String mtl_filename = obj_filename.substring(
			0, obj_filename.lastIndexOf('.')) + ".mtl";

		OutputStreamWriter dos_obj = null,
				   dos_mtl = null;
		try {
			dos_obj = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(obj_file)), "8859_1"); // encoding in Latin 1 (for macosx not to mess around
			dos_mtl = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(dir + "/" + mtl_filename))), "8859_1"); // encoding in Latin 1 (for macosx not to mess around
			writeAsWaveFront(contents, mtl_filename, dos_obj, dos_mtl);
			dos_obj.flush();
			dos_obj.flush();
		} catch (IOException e) {
			IJ.log("Some error ocurred while saving to wavefront:\n" + e);
			e.printStackTrace();
		} finally {
			try { if (null != dos_obj) dos_obj.close(); } catch (Exception e) {}
			try { if (null != dos_mtl) dos_mtl.close(); } catch (Exception e) {}
		}
	}

	static public void saveAsDXF(Collection meshgroups) {
		if (null == meshgroups || 0 == meshgroups.size()) return;
		meshgroups = filterMeshes(meshgroups);
		if (0 == meshgroups.size()) {
			IJ.log("No meshes to export!");
			return;
		}
		SaveDialog sd = new SaveDialog("Save as DXF", "untitled", ".dxf");
		String dir = sd.getDirectory();
		if (null == dir) return;
		if (IJ.isWindows()) dir = dir.replace('\\', '/');
		if (!dir.endsWith("/")) dir += "/";
		String dxf_filename = sd.getFileName();
		if (!dxf_filename.toLowerCase().endsWith(".dxf")) dxf_filename += ".dxf";

		File dxf_file = new File(dir + "/" + dxf_filename);
		// check if file exists
		if (!IJ.isMacOSX()) {
			if (dxf_file.exists()) {
				YesNoCancelDialog yn = new YesNoCancelDialog(IJ.getInstance(), "Overwrite?", "File  " + dxf_filename + " exists!\nOverwrite?");
				if (!yn.yesPressed()) return;
			}
		}

		OutputStreamWriter dos = null;
		try {
			dos = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dxf_file)), "8859_1"); // encoding in Latin 1 (for macosx not to mess around
			writeDXF(meshgroups, dos);
			dos.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try { if (null != dos) dos.close(); } catch (Exception e) {}
		}
	}

	static public void writeDXF(final Collection contents, final Writer w) throws IOException {
		w.write("0\nSECTION\n2\nENTITIES\n");   //header of file
		for (Iterator it = contents.iterator(); it.hasNext(); ) {
			Content ob = (Content)it.next();

			CustomMesh cmesh=null;

			if (ob.getContent() instanceof CustomMeshNode) {
				CustomMeshNode cmeshnode = (CustomMeshNode) ob.getContent();
				cmesh = cmeshnode.getMesh();
			} else if (ob.getContent() instanceof MeshGroup) {
				MeshGroup mg = (MeshGroup)ob.getContent();
				cmesh = mg.getMesh();
			} else
				continue;

			final List triangles = cmesh.getMesh();

			String title = ob.getName().replaceAll(" ", "_").replaceAll("#", "--");
			Mtl mat = new Mtl(1 - ob.getTransparency(), cmesh.getColor());
			writeTrianglesDXF(w, triangles, title, "" + mat.getAsSingle());
		}
		w.append("0\nENDSEC\n0\nEOF\n");         //TRAILER of the file
	}

	@Deprecated
	static public String createDXF(final Collection contents) {
		StringWriter sw = new StringWriter();
		try {
			writeDXF(contents, sw);
		} catch (IOException ioe) {}
		return sw.toString();
	}

	@Deprecated
	static public void writeTrianglesDXF(final StringBuffer sb, final List triangles, final String the_group, final String the_color) {
		try {
			StringWriter sw = new StringWriter();
			writeTrianglesDXF(sw, triangles, the_group, the_color);
			sb.append(sw.getBuffer());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	static private void writeTrianglesDXF(final Writer w, final List triangles, final String the_group, final String the_color) throws IOException {

		final char L = '\n';
		final String s10 = "10\n"; final String s11 = "11\n"; final String s12 = "12\n"; final String s13 = "13\n";
		final String s20 = "20\n"; final String s21 = "21\n"; final String s22 = "22\n"; final String s23 = "23\n";
		final String s30 = "30\n"; final String s31 = "31\n"; final String s32 = "32\n"; final String s33 = "33\n";
		final String triangle_header = "0\n3DFACE\n8\n" + the_group + "\n6\nCONTINUOUS\n62\n" + the_color + L;

		final int len = triangles.size();
		final Point3f[] vert = new Point3f[len];
		triangles.toArray(vert);

		final StringBuffer sb = new StringBuffer(150);

		for (int i=0; i<len; i+=3) {

			w.write(triangle_header);

			sb
			.append(s10).append(vert[i].x).append(L)
			.append(s20).append(vert[i].y).append(L)
			.append(s30).append(vert[i].z).append(L)

			.append(s11).append(vert[i+1].x).append(L)
			.append(s21).append(vert[i+1].y).append(L)
			.append(s31).append(vert[i+1].z).append(L)

			.append(s12).append(vert[i+2].x).append(L)
			.append(s22).append(vert[i+2].y).append(L)
			.append(s32).append(vert[i+2].z).append(L)

			.append(s13).append(vert[i+2].x).append(L) // repeated point
			.append(s23).append(vert[i+2].y).append(L)
			.append(s33).append(vert[i+2].z).append(L);

			w.write(sb.toString());
			sb.setLength(0);
		}
	}

	/**
	 * Expects a collection of MeshGroup objects, and the material file name to point to.
	 * Returns two String objects:
	 * - the contents of the .obj file with mesh data
	 * - the contents of the .mtl file with material data
	 */
	@Deprecated
	static public String[] createWaveFront(Collection contents, String mtl_filename) {
		StringWriter sw_obj = new StringWriter();
		StringWriter sw_mtl = new StringWriter();
		try {
			writeAsWaveFront(contents, mtl_filename, sw_obj, sw_mtl);
			return new String[]{sw_obj.toString(), sw_mtl.toString()};
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	static public void writeAsWaveFront(Collection contents, String mtl_filename, Writer w_obj, Writer w_mtl) throws IOException {
		HashMap<String, CustomMesh> meshes = new HashMap<String, CustomMesh>();

		for(Iterator it = contents.iterator(); it.hasNext(); ) {
			Content mob = (Content)it.next();
			
			ContentNode node = mob.getContent();

			// First CustomMultiMesh, which is also a CustomMeshNode:
			if (node instanceof CustomMultiMesh) {
				CustomMultiMesh multi = (CustomMultiMesh)node;
				for (int i=0; i<multi.size(); i++) {
					meshes.put(mob.getName() + " [" + (i+1) + "]", multi.getMesh(i));
				}
			// Then CustomMeshNode (all custom meshes):
			} else if (node instanceof CustomMeshNode) {
				meshes.put(mob.getName(), ((CustomMeshNode)node).getMesh());
			// An image volume rendered as isosurface:
			} else if (node instanceof MeshGroup) {
				meshes.put(mob.getName(), ((MeshGroup)node).getMesh());
			} else {
				IJ.log("Ignoring " + mob.getName() + " with node of class " + node.getClass());
				continue;
			}
		}
		WavefrontExporter.save(meshes, mtl_filename, w_obj, w_mtl);
	}

	/** A Material, but avoiding name colisions. Not thread-safe. */
	static private int mat_index = 1;
	static private class Mtl {
		float alpha = 1;
		float R = 1;
		float G = 1;
		float B = 1;
		String name;
		Mtl(float alpha, Color3f c) {
			this.alpha = alpha;
			float[] f = new float[3];
			c.get(f);
			this.R = f[0];
			this.G = f[1];
			this.B = f[2];
			name = "mat_" + mat_index;
			mat_index++;
		}
		Mtl(float alpha, float R, float G, float B) {
			this.alpha = alpha;
			this.R = R;
			this.G = G;
			this.B = B;
			name = "mat_" + mat_index;
			mat_index++;
		}
		public boolean equals(Object ob) {
			if (ob instanceof MeshExporter.Mtl) {
				Mtl mat = (Mtl)ob;
				if (mat.alpha == alpha
				 && mat.R == R
				 && mat.G == G
				 && mat.B == B) {
					return true;
				 }
			}
			return false;
		}
		public int hashCode() {
			long bits = 1L;
			bits = 31L * bits + (long)Float.floatToIntBits(alpha);
			bits = 31L * bits + (long)Float.floatToIntBits(R);
			bits = 31L * bits + (long)Float.floatToIntBits(G);
			bits = 31L * bits + (long)Float.floatToIntBits(B);
			return (int) (bits ^ (bits >> 32));
		}

		void fill(StringBuffer sb) {
			sb.append("\nnewmtl ").append(name).append('\n')
			  .append("Ns 96.078431\n")
			  .append("Ka 0.0 0.0 0.0\n")
			  .append("Kd ").append(R).append(' ').append(G).append(' ').append(B).append('\n') // this is INCORRECT but I'll figure out the conversion later
			  .append("Ks 0.5 0.5 0.5\n")
			  .append("Ni 1.0\n")
			  .append("d ").append(alpha).append('\n')
			  .append("illum 2\n\n");
		}
		/** For DXF color */
		int getAsSingle() {
			return (int)((R + G + B / 3) * 255);
		}
	}

	/** Utility method to encode text data in 8859_1. */
	static public boolean saveToFile(final File f, final String data) {
		if (null == f) return false;
		try {
			OutputStreamWriter dos = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f), data.length()), "8859_1"); // encoding in Latin 1 (for macosx not to mess around
			dos.write(data, 0, data.length());
			dos.flush();
		} catch (Exception e) {
			e.printStackTrace();
			IJ.showMessage("ERROR: Most likely did NOT save your file.");
			return false;
		}
		return true;
	}

}
