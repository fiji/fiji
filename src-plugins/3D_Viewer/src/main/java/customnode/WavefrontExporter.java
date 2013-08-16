package customnode;

import customnode.CustomMesh;

import java.io.File;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedOutputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.vecmath.Color4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class WavefrontExporter {

	public static void save(
			Map<String, CustomMesh> meshes,
			String objFile) throws IOException {
		File objF = new File(objFile);

		String objname = objF.getName();
		String mtlname = objname;
		if(mtlname.endsWith(".obj"))
			mtlname = mtlname.substring(0, mtlname.length() - 4);
		mtlname += ".mtl";

		OutputStreamWriter dos_obj = null,
				   dos_mtl = null;
		try {
			dos_obj = new OutputStreamWriter(
				new BufferedOutputStream(
				new FileOutputStream(objF)), "8859_1");
			dos_mtl = new OutputStreamWriter(
				new BufferedOutputStream(
				new FileOutputStream(
				new File(objF.getParent(), mtlname))),
				"8859_1");
			save(meshes, mtlname, dos_obj, dos_mtl);
			dos_obj.flush();
			dos_obj.flush();
			for(String n : meshes.keySet()) {
				CustomMesh m = meshes.get(n);
				m.loadedFromFile = objFile;
				m.loadedFromName = n.replaceAll(" ", "_").
					replaceAll("#", "--");
				m.changed = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { if (null != dos_obj) dos_obj.close(); } catch (Exception e) {}
			try { if (null != dos_mtl) dos_mtl.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Write the given collection of <code>CustomMesh</code>es;
	 * @param meshes maps a name to a <code>CustomMesh</code>. The name
	 *        is used to set the group name ('g') in the obj file.
	 * @param mtlFileName name of the material file, which is used to
	 *        store in the obj-file.
	 * @param objWriter <code>Writer</code> for the obj file
	 * @param mtlWriter <code>Writer</code> for the material file.
	 */
	public static void save(
			Map<String, CustomMesh> meshes,
			String mtlFileName,
			Writer objWriter,
			Writer mtlWriter) throws IOException {

		objWriter.write("# OBJ File\n");
		objWriter.write("mtllib ");
		objWriter.write(mtlFileName);
		objWriter.write('\n');

		final HashMap<Mtl, Mtl> ht_mat = new HashMap<Mtl, Mtl>();

		// Vert indices in .obj files are global, not reset for every
		// object. Starting at '1' because vert indices start at one.
		int j = 1; 

		final StringBuffer tmp = new StringBuffer(100);

		for(String name : meshes.keySet()) {
			CustomMesh cmesh = meshes.get(name);

			final List<Point3f> vertices = cmesh.getMesh();
			// make material, and see whether it exists already
			Color3f color = cmesh.getColor();
			if (null == color) {
				// happens when independent colors
				// have been set for each vertex.
				color = CustomMesh.DEFAULT_COLOR;
			}
			Mtl mat = new Mtl(1 - cmesh.getTransparency(),
					color);
			if(ht_mat.containsKey(mat))
				mat = ht_mat.get(mat);
			else
				ht_mat.put(mat, mat);

			// make list of vertices
			String title = name.replaceAll(" ", "_").
					replaceAll("#", "--");
			HashMap<Point3f, Integer> ht_points =
					new HashMap<Point3f, Integer>();
			objWriter.write("g ");
			objWriter.write(title);
			objWriter.write('\n');
			final int len = vertices.size();
			int[] index = new int[len];

			// index over index array, to make faces later
			int k = 0;
			for (Point3f p : vertices) {
				// check if point already exists
				if(ht_points.containsKey(p)) {
					index[k] = ht_points.get(p);
				} else {
					// new point
					index[k] = j;
					// record
					ht_points.put(p, j);
					// append vertex
					tmp.append('v').append(' ')
					   .append(p.x).append(' ')
					   .append(p.y).append(' ')
					   .append(p.z).append('\n');
					objWriter.write(tmp.toString());
					tmp.setLength(0);
					j++;
				}
				k++;
			}
			objWriter.write("usemtl ");
			objWriter.write(mat.name);
			objWriter.write('\n');
			// print faces
			if(cmesh.getClass() == CustomTriangleMesh.class)
				writeTriangleFaces(index, objWriter, name);
			else if(cmesh.getClass() == CustomQuadMesh.class)
				writeQuadFaces(index, objWriter, name);
			else if(cmesh.getClass() == CustomPointMesh.class)
				writePointFaces(index, objWriter, name);
			else if(cmesh.getClass() == CustomLineMesh.class) {
				CustomLineMesh clm = (CustomLineMesh)cmesh;
				switch(clm.getMode()) {
				case CustomLineMesh.PAIRWISE:
					writePairwiseLineFaces(
						index, objWriter, name);
					break;
				case CustomLineMesh.CONTINUOUS:
					writeContinuousLineFaces(
						index, objWriter, name);
					break;
				default: throw new IllegalArgumentException(
					"Unknown line mesh mode");
				}
			} else {
				throw new IllegalArgumentException(
					"Unknown custom mesh class: " +
					cmesh.getClass());
			}
		}
		// make mtl file
		mtlWriter.write("# MTL File\n");
		for(Mtl mat : ht_mat.keySet()) {
			StringBuffer sb = new StringBuffer(150);
			mat.fill(sb);
			mtlWriter.write(sb.toString());
		}
	}

	/**
	 * Write faces for triangle meshes.
	 */
	static void writeTriangleFaces(int[] indices, Writer objWriter, String name)
						throws IOException {
		if(indices.length % 3 != 0)
			throw new IllegalArgumentException(
				"list of triangles not multiple of 3: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 3) {
			buf.append('f').append(' ')
				.append(indices[i]).append(' ')
				.append(indices[i+1]).append(' ')
				.append(indices[i+2]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for point meshes.
	 */
	static void writePointFaces(int[] indices, Writer objWriter, String name)
						throws IOException {
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i++) {
			buf.append('f').append(' ')
				.append(indices[i]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for quad meshes.
	 */
	static void writeQuadFaces(int[] indices, Writer objWriter, String name)
						throws IOException {
		if(indices.length % 4 != 0)
			throw new IllegalArgumentException(
				"list of quads not multiple of 4: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 4) {
			buf.append('f').append(' ')
				.append(indices[i]).append(' ')
				.append(indices[i+1]).append(' ')
				.append(indices[i+2]).append(' ')
				.append(indices[i+3]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for pairwise line meshes, ie the points addressed
	 * by the indices array are arranged in pairs each specifying 
	 * one line segment.
	 */
	static void writePairwiseLineFaces(int[] indices, Writer objWriter, String name)
						throws IOException {
		if(indices.length % 2 != 0)
			throw new IllegalArgumentException(
				"list of lines not multiple of 2: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 2) {
			buf.append('f').append(' ')
				.append(indices[i]).append(' ')
				.append(indices[i+1]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for continuous line meshes, ie the points addressed
	 * by the indices array represent a continuous line.
	 */
	static void writeContinuousLineFaces(int[] indices, Writer objWriter, String name)
						throws IOException {
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length - 1; i++) {
			buf.append('f').append(' ')
				.append(indices[i]).append(' ')
				.append(indices[i+1]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/** A Material, but avoiding name colisions. Not thread-safe. */
	static private int mat_index = 1;
	static private class Mtl {

		private Color4f col;
		String name;

		Mtl(float alpha, Color3f c) {
			this.col = new Color4f(c.x, c.y, c.z, alpha);
			name = "mat_" + mat_index;
			mat_index++;
		}

		Mtl(float alpha, float R, float G, float B) {
			this.col = new Color4f(R, G, B, alpha);
			name = "mat_" + mat_index;
			mat_index++;
		}

		public boolean equals(Object ob) {
			if (ob instanceof Mtl) {
				return this.col == ((Mtl)ob).col;
			}
			return false;
		}

		public int hashCode() {
			return col.hashCode();
		}

		void fill(StringBuffer sb) {
			sb.append("\nnewmtl ").append(name).append('\n')
			  .append("Ns 96.078431\n")
			  .append("Ka 0.0 0.0 0.0\n")
			  .append("Kd ").append(col.x).append(' ')
			  // this is INCORRECT but I'll figure out the
			  // conversion later
			  .append(col.y).append(' ').append(col.z).append('\n')
			  .append("Ks 0.5 0.5 0.5\n")
			  .append("Ni 1.0\n")
			  .append("d ").append(col.w).append('\n')
			  .append("illum 2\n\n");
		}
	}

	/** Utility method to encode text data in 8859_1. */
	static public boolean saveToFile(final File f, final String data)
							throws IOException {
		if (null == f) return false;
		OutputStreamWriter dos = new OutputStreamWriter(
			new BufferedOutputStream(
			 // encoding in Latin 1 (for macosx not to mess around
			new FileOutputStream(f), data.length()), "8859_1");
		dos.write(data, 0, data.length());
		dos.flush();
		return true;
	}
}

