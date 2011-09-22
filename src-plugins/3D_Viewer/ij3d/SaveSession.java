package ij3d;

import ij.io.SaveDialog;
import ij.io.FileSaver;
import ij.io.FileInfo;
import ij.gui.GenericDialog;
import ij.IJ;

import orthoslice.OrthoGroup;
import surfaceplot.SurfacePlotGroup;

import customnode.MeshLoader;
import customnode.WavefrontExporter;
import customnode.CustomMeshNode;
import customnode.CustomMesh;
import customnode.CustomMultiMesh;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;

import javax.media.j3d.Transform3D;

import javax.vecmath.Color3f;

import java.awt.Panel;
import java.awt.Button;
import java.awt.TextField;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SaveSession {

	public static void saveView(Image3DUniverse univ, String path)
							throws IOException {
		SaveSession sase = new SaveSession();
		PrintWriter out = new PrintWriter(new FileWriter(path));
		sase.saveView(out, univ);
		out.close();
	}

	public static void loadView(Image3DUniverse univ, String path)
							throws IOException {
		SaveSession sase = new SaveSession();
		BufferedReader in = new BufferedReader(new FileReader(path));
		HashMap<String, String> view = sase.readView(in, univ);
		in.close();
		sase.apply(view, univ);
	}

	public static void saveScene(Image3DUniverse univ, String path)
							throws IOException {
		SaveSession sase = new SaveSession();
		if(!sase.ensureAllSaved(univ.getContents())) {
			IJ.error("Could not save session");
			return;
		}
		PrintWriter out = new PrintWriter(new FileWriter(path));
		sase.saveView(out, univ);
		for(Object c : univ.getContents())
			sase.saveContent(out, (Content)c);
		out.close();
	}

	public static void loadScene(Image3DUniverse univ, String path)
						throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(path));
		SaveSession sase = new SaveSession();
		univ.removeAllContents();
		HashMap<String, String> view = sase.readView(in, univ);
		boolean b = univ.getAutoAdjustView();
		Content c = null;
		while((c = sase.readContent(in)) != null) {
			// TODO
			c.setPointListDialog(univ.getPointListDialog());
			univ.addContent(c);
		}
		in.close();
		sase.apply(view, univ);
	}

	private class CMesh {
		private CustomMesh mesh;
		private String name;

		CMesh(CustomMesh mesh, String name) {
			this.mesh = mesh;
			this.name = name;
		}
	}

	boolean ensureAllSaved(Collection<Content> contents) throws IOException {
		// go through all contents, make sure that those with
		// images have saved images, and collect the custom
		// meshes file-wise.
		HashMap<String, ArrayList<CMesh>> custommeshes =
			new HashMap<String, ArrayList<CMesh>>();

		ArrayList<String> unsavedImages = new ArrayList<String>();
		for(Content content : contents) {
			for(ContentInstant c : content.getInstants().values()) {
				int t = c.getType();
				if(t != Content.CUSTOM) {
					FileInfo fi = c.getImage().getOriginalFileInfo();
					if(fi == null || c.image.changes)
						unsavedImages.add(c.image.getTitle());
					continue;
				}
				CustomMeshNode cn = (CustomMeshNode)c.getContent();
				ArrayList<CustomMesh> meshes = getMeshes(cn);
				int i = -1;
				for(CustomMesh cm : meshes) {
					i++;
					String file = cm.getFile();
					boolean changed = file == null || cm.hasChanged();
					if(!changed)
						continue;
					if(!custommeshes.containsKey(file))
						custommeshes.put(file,
							new ArrayList<CMesh>());
					String name = cm.getName();
					if(name == null)
						name = c.getName();
					if(meshes.size() > 1) // it's a multimesh; make sure the name is unique
						name += "-multimesh" + i;
					custommeshes.get(file).add(
						new CMesh(cm, name));
				}
			}
		}

		// show the user a dialog with all changed images and ask them
		// to save them all
		if(!unsavedImages.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Error.\n")
				.append("The following images have unsaved ")
				.append("changes:\n  \n");
			for(String s : unsavedImages)
				sb.append("  ").append(s).append("\n");
			sb.append("  \nPlease save them separately ").
				append("before calling 'Save session' again");

			IJ.error(sb.toString());
			return false;
		}

		// ask user to save all meshes with no file in single new file
		if(custommeshes.containsKey(null)) {
			ArrayList<CMesh> meshes = custommeshes.get(null);
			if(meshes != null && !saveObj(meshes)) {
				IJ.error("Error saving session");
				return false;
			}
			custommeshes.remove(null);
		}

		if(custommeshes.isEmpty())
			return true;

		// and the other files to where they came from
		StringBuilder sb = new StringBuilder();
		sb.append("The following mesh(es) were loaded from file \n");
		sb.append("but changed. Save them to their original\n");
		sb.append("file(s)?\n  \n");
		for(String file : custommeshes.keySet())
			for(CMesh m : custommeshes.get(file))
				sb.append("  " + m.name + "\n");
		sb.append("  \n");
		GenericDialog gd = new GenericDialog("");
		gd.addMessage(sb.toString());
		gd.showDialog();
		if(gd.wasCanceled())
			return false;

		for(String file : custommeshes.keySet()) {
			ArrayList<CMesh> meshes = custommeshes.get(file);
			if(meshes == null)
				continue;
			if(!updateObj(meshes, file))
				return false;
		}
		return true;
	}

	static boolean updateObj(ArrayList<CMesh> meshes, String path)
						throws IOException {
		Map<String, CustomMesh> prev = MeshLoader.load(path);
		for(CMesh m : meshes)
			prev.put(m.name, m.mesh);

		try {
			WavefrontExporter.save(prev, path);
			return true;
		} catch(IOException e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return false;
		}
	}

	static boolean saveObj(ArrayList<CMesh> meshes) {
		StringBuilder sb = new StringBuilder();
		sb.append("The following surfaces are not saved:\n");
		for(CMesh m : meshes)
			sb.append("  " + m.name + "\n");
		sb.append("Select a path below to save them");

		String path = showPathDialog("Save meshes", sb.toString());
		if(path == null)
			return false;

		HashMap<String, CustomMesh> m2w =
			new HashMap<String, CustomMesh>();
		for(CMesh m : meshes)
			m2w.put(m.name, m.mesh);

		try {
			WavefrontExporter.save(m2w, path);
			return true;
		} catch(IOException e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return false;
		}
	}

	static String showPathDialog(String title, String msg) {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		Panel p = new Panel(new FlowLayout());
		final TextField tf = new TextField(30);
		p.add(tf);
		Button b = new Button("...");
		p.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SaveDialog sd = new SaveDialog(
					"Select path", "untitled", ".obj");
				String dir = sd.getDirectory();
				String file = sd.getFileName();
				File f = new File(dir, file);
				tf.setText(f.getAbsolutePath());
			}
		});
		gd.addPanel(p);
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
		return new File(tf.getText()).getAbsolutePath();
	}

	void saveView(PrintWriter out, Image3DUniverse univ)
						throws IOException {
		out.println("BeginView");
		Transform3D t3d = new Transform3D();
		univ.getCenterTG().getTransform(t3d);
		out.println("center = " + toString(t3d));
		univ.getTranslateTG().getTransform(t3d);
		out.println("translate = " + toString(t3d));
		univ.getRotationTG().getTransform(t3d);
		out.println("rotate = " + toString(t3d));
		univ.getZoomTG().getTransform(t3d);
		out.println("zoom = " + toString(t3d));
		univ.getAnimationTG().getTransform(t3d);
		out.println("animate = " + toString(t3d));
		out.println("EndView");
	}

	HashMap<String, String> readView(BufferedReader in,
			Image3DUniverse univ) throws IOException {
		String line;
		boolean foundNext = false;
		while((line = in.readLine()) != null) {
			if(line.startsWith("BeginView")) {
				foundNext = true;
				break;
			}
		}
		if(!foundNext)
			return null;

		HashMap<String, String> props = new HashMap<String, String>();
		while((line = in.readLine()) != null) {
			if(line.startsWith("EndView"))
				break;
			if(line.startsWith("#"))
				continue;
			String[] keyval = line.split("=");
			props.put(keyval[0].trim(), keyval[1].trim());
		}
		return props;
	}

	public void apply(HashMap<String, String> props, Image3DUniverse univ) {
		String tmp;
		// Set up new Content
		if((tmp = props.get("center")) != null)
			univ.getCenterTG().setTransform(t(tmp));
		if((tmp = props.get("translate")) != null)
			univ.getTranslateTG().setTransform(t(tmp));
		if((tmp = props.get("rotate")) != null)
			univ.getRotationTG().setTransform(t(tmp));
		if((tmp = props.get("zoom")) != null)
			univ.getZoomTG().setTransform(t(tmp));
		if((tmp = props.get("animate")) != null)
			univ.getAnimationTG().setTransform(t(tmp));

		univ.getViewPlatformTransformer().updateFrontBackClip();
	}

	void saveContent(PrintWriter out, Content c) {
		out.println("BeginContent");
		out.println("name = " + c.getName());
		for(ContentInstant ci : c.getInstants().values())
			saveContentInstant(out, ci);
		out.println("EndContent");
	}

	void saveContentInstant(PrintWriter out, ContentInstant c) {
		// color string
		String col = c.color == null ? null : Integer.toString(
			c.color.get().getRGB());
		// channel string
		String chan = c.channels[0] + "%%%" + c.channels[1]
			+ "%%%" + c.channels[2];
		// transformations
		Transform3D t = new Transform3D();
		c.getLocalRotate(t);
		String rot = toString(t);
		c.getLocalTranslate(t);
		String trans = toString(t);

		out.println("BeginContentInstant");
		out.println("name = "         + c.getName());
		if(col != null)
			out.println("color = "        + col);
		out.println("timepoint = "    + c.timepoint);
		out.println("channels = "     + chan);
		out.println("transparency = " + c.transparency);
		out.println("threshold = "    + c.threshold);
		out.println("resampling = "   + c.resamplingF);
		out.println("type = "         + c.type);
		out.println("locked = "       + c.isLocked());
		out.println("shaded = "       + c.shaded);
		out.println("visible = "      + c.isVisible());
		out.println("coordVisible = " + c.hasCoord());
		out.println("plVisible = "    + c.isPLVisible());
		out.println("rotation = "     + rot);
		out.println("translation = "  + trans);
		if(c.image != null)
			out.println("imgfile = "      + getImageFile(c));

		int type = c.getType();
		ContentNode cn = c.getContent();
		if(type == Content.SURFACE_PLOT2D) {
			out.println("surfplt = " +
				((SurfacePlotGroup)cn).getSlice());
		} else if(type == Content.ORTHO) {
			out.println("ortho = " + getOrthoString(cn));
		} else if(type == Content.CUSTOM) {
			out.println("surffiles = " + getMeshString(c));
		}
		out.println("EndContentInstant");
	}

	public Content readContent(BufferedReader in) throws IOException {
		String name = null;
		String line;
		boolean foundNext = false;
		while((line = in.readLine()) != null) {
			if(line.startsWith("BeginContent")) {
				foundNext = true;
				break;
			}
		}
		if(!foundNext)
			return null;
		while((line = in.readLine()) != null) {
			if(line.startsWith("name")) {
				name = line.split("=")[1].trim();
				break;
			}
		}
		TreeMap<Integer, ContentInstant> cis =
			new TreeMap<Integer, ContentInstant>();
		ContentInstant ci = null;
		while((ci = readContentInstant(in)) != null)
			cis.put(ci.timepoint, ci);
		if(name == null)
			throw new RuntimeException("no name for content");
		return new Content(name, cis);
	}

	public ContentInstant readContentInstant(BufferedReader in) throws IOException {
		String line;
		boolean foundNext = false;
		while((line = in.readLine()) != null) {
			if(line.startsWith("EndContent"))
				break;
			if(line.startsWith("BeginContentInstant")) {
				foundNext = true;
				break;
			}
		}
		if(!foundNext)
			return null;

		HashMap<String, String> props = new HashMap<String, String>();
		while((line = in.readLine()) != null) {
			if(line.startsWith("EndContentInstant"))
				break;
			if(line.startsWith("#"))
				continue;
			String[] keyval = line.split("=");
			props.put(keyval[0].trim(), keyval[1].trim());
		}
		String tmp;
		String[] sp;

		// Set up new Content
		ContentInstant c = new ContentInstant(props.get("name"));
		if((tmp = props.get("channels")) != null) {
			sp= tmp.split("%%%");
			c.channels = new boolean[] {b(sp[0]),b(sp[1]),b(sp[2])};
		}
		if((tmp = props.get("timepoint")) != null)
			c.timepoint = i(tmp);
		if((tmp = props.get("resampling")) != null)
			c.resamplingF = i(tmp);
		if((tmp = props.get("rotation")) != null)
			c.getLocalRotate().setTransform(t(tmp));
		if((tmp = props.get("translation")) != null)
			c.getLocalTranslate().setTransform(t(tmp));
		int type = i(props.get("type"));
		if(type != Content.CUSTOM) {
			c.image = IJ.openImage(props.get("imgfile"));
			if(c.image == null)
				throw new RuntimeException("Cannot load image: " + props.get("imgfile"));
			c.displayAs(type);
			if(type == Content.SURFACE_PLOT2D &&
				(tmp = props.get("surfplt")) != null) {
				((SurfacePlotGroup)c.getContent()).
					setSlice(i(tmp));
			} else if(type == Content.ORTHO &&
				(tmp = props.get("ortho")) != null) {
				OrthoGroup og = (OrthoGroup)c.getContent();
				sp = tmp.split("%%%");

				int slice = i(sp[0]);
				if(slice == -1)
					og.setVisible(0, false);
				else
					og.setSlice(0, slice);

				slice = i(sp[1]);
				if(slice == -1)
					og.setVisible(1, false);
				else
					og.setSlice(1, slice);

				slice = i(sp[2]);
				if(slice == -1)
					og.setVisible(2, false);
				else
					og.setSlice(2, slice);

			}
		} else {
			tmp = props.get("surffiles");
			c.display(createCustomNode(tmp));
		}

		if((tmp = props.get("color")) != null)
			c.setColor(new Color3f(new Color(i(tmp))));
		if((tmp = props.get("transparency")) != null)
			c.setTransparency(f(tmp));
		if((tmp = props.get("threshold")) != null)
			c.setThreshold(i(tmp));
		if((tmp = props.get("shaded")) != null)
			c.setShaded(b(tmp));
		if((tmp = props.get("visible")) != null)
			c.setVisible(b(tmp));
		if((tmp = props.get("coordVisible")) != null)
			c.showCoordinateSystem(b(tmp));
		if((tmp = props.get("plVisible")) != null)
			c.showPointList(b(tmp));
		if((tmp = props.get("locked")) != null)
			c.setLocked(b(tmp));

		return c;
	}

	private CustomMeshNode createCustomNode(String s) {
		String[] sp = s.split("%%%");
		if(sp.length == 2) {
			Map<String, CustomMesh> meshes =
				MeshLoader.load(sp[0]);
			if(meshes == null) {
				IJ.error("Could not load " + sp[0]);
				return null;
			}
			return new CustomMeshNode(meshes.get(sp[1]));
		}

		HashMap<String, ArrayList<String>>file2name =
			new HashMap<String, ArrayList<String>>();
		for(int i = 0; i < sp.length; i += 2) {
			if(!file2name.containsKey(sp[i]))
				file2name.put(sp[i], new ArrayList<String>());
			file2name.get(sp[i]).add(sp[i + 1]);
		}

		ArrayList<CustomMesh> cms = new ArrayList<CustomMesh>();
		for(String file : file2name.keySet()) {
			ArrayList<String> names = file2name.get(file);
			Map<String, CustomMesh> meshes =
				MeshLoader.load(file);
			if(meshes == null) {
				IJ.error("Could not load " + file);
				continue;
			}
			for(String name : names)
				cms.add(meshes.get(name));
		}
		return new CustomMultiMesh(cms);
	}

	private static final int i(String s) {
		return Integer.parseInt(s);
	}

	private static final boolean b(String s) {
		return Boolean.parseBoolean(s);
	}

	private static final float f(String s) {
		return Float.parseFloat(s);
	}

	private static final Transform3D t(String s) {
		String[] sp = s.split(" ");
		float[] f = new float[16];
		for(int i = 0; i < sp.length; i++)
			f[i] = f(sp[i]);
		return new Transform3D(f);
	}

	private static final String toString(Transform3D t3d) {
		float[] xf = new float[16];
		t3d.get(xf);
		String ret = "";
		for(int i = 0; i < 16; i++)
			ret += " " + xf[i];
		return ret;
	}

	private static ArrayList<CustomMesh> getMeshes(CustomMeshNode cn) {
		ArrayList<CustomMesh> meshes = new ArrayList<CustomMesh>();
		if(cn instanceof CustomMultiMesh) {
			CustomMultiMesh cmm = (CustomMultiMesh)cn;
			for(int i = 0; i < cmm.size(); i++)
				meshes.add(cmm.getMesh(i));
		} else {
			meshes.add(cn.getMesh());
		}
		return meshes;
	}

	private static final String getMeshString(ContentInstant c) {
		ArrayList<CustomMesh> meshes = getMeshes(
				(CustomMeshNode)c.getContent());
		String ret = "";
		for(CustomMesh cm : meshes) {
			String name = cm.getName();
			if(name == null) name = c.getName();
			name.replaceAll(" ", "_").
				replaceAll("#", "--");
			ret += "%%%" + cm.getFile() + "%%%" + name;
		}
		return ret.substring(3, ret.length());
	}

	private static final String getOrthoString(ContentNode c) {
		OrthoGroup og = (OrthoGroup)c;
		int xSlide = og.isVisible(AxisConstants.X_AXIS) ?
			og.getSlice(AxisConstants.X_AXIS) : -1;
		int ySlide = og.isVisible(AxisConstants.Y_AXIS) ?
			og.getSlice(AxisConstants.Y_AXIS) : -1;
		int zSlide = og.isVisible(AxisConstants.Z_AXIS) ?
			og.getSlice(AxisConstants.Z_AXIS) : -1;
		return xSlide + "%%%" + ySlide + "%%%" + zSlide;
	}

	private static final String getImageFile(ContentInstant c) {
		if(c.image == null)
			return null;
		FileInfo fi = c.image.getOriginalFileInfo();
		if(fi == null || c.image.changes)
			throw new RuntimeException("Image not saved");
		return fi.directory + fi.fileName;
	}
}

