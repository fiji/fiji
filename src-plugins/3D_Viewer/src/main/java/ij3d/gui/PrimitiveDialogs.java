package ij3d.gui;

import ij.gui.GenericDialog;
import ij3d.Content;
import ij3d.Image3DUniverse;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point3f;

import customnode.Box;
import customnode.Cone;
import customnode.Primitive;
import customnode.Sphere;
import customnode.Tube;

public class PrimitiveDialogs {

	private static final String ws = "\\s*";
	private static final String no = "(\\S*?)";

	private static final Pattern tuplePattern = Pattern.compile(
		"\\(?" + no + "," + ws + no + "," + ws + no + "\\)?");

	@SuppressWarnings("serial")
	private static abstract class PrimitiveDialog extends GenericDialog implements FocusListener {
		protected final Image3DUniverse univ;
		protected Content content;

		public PrimitiveDialog(String title, Image3DUniverse univ) {
			super(title);
			this.univ = univ;
		}

		@Override
		public void focusLost(FocusEvent e) {
			try {
				boolean b = univ.getUseToFront();
				univ.setUseToFront(false);
				updatePreview();
				univ.setUseToFront(b);
			} catch(Exception ex) {}
		}

		public abstract Primitive getPrimitive();
		public abstract String getNameForContent();

		public void updatePreview() {
			String name = getNameForContent();
			if(content != null)
				univ.removeContent(content.getName());
			Primitive prim = getPrimitive();
			if(prim != null)
				content = univ.addCustomMesh(prim, name);
		}
	}

	public static void addBox(Image3DUniverse univ) {
		new BoxDialog(univ);
	}

	public static void addSphere(Image3DUniverse univ) {
		new SphereDialog(univ);
	}

	public static void addCone(Image3DUniverse univ) {
		new ConeDialog(univ);
	}

	public static void addTube(Image3DUniverse univ) {
		new TubeDialog(univ);
	}

	@SuppressWarnings("serial")
	private final static class BoxDialog extends PrimitiveDialog {
		private final TextField tf0, tf1, tf2;

		public BoxDialog(final Image3DUniverse univ) {
			super("Box", univ);
			addStringField("Name", "");
			addStringField("Lower corner", "");
			addStringField("Upper corner", "");
			@SuppressWarnings("rawtypes")
			Vector v = getStringFields();
			tf0 = (TextField)v.get(0);
			tf1 = (TextField)v.get(1);
			tf2 = (TextField)v.get(2);
			tf0.addFocusListener(this);
			tf1.addFocusListener(this);
			tf2.addFocusListener(this);
			showDialog();
			if(wasCanceled())
				univ.removeContent(tf0.getText());
			else
				updatePreview();
		}

		@Override
		public String getNameForContent() {
			return tf0.getText();
		}

		@Override
		public Primitive getPrimitive() {
			Point3f lc = parsePoint(tf1.getText());
			Point3f uc = parsePoint(tf2.getText());
			return new Box(lc, uc);
		}
	}

	@SuppressWarnings("serial")
	private final static class SphereDialog extends PrimitiveDialog {
		private final TextField tf0, tf1, tf2;

		public SphereDialog(final Image3DUniverse univ) {
			super("Sphere", univ);
			addStringField("Name", "");
			addStringField("Center", "");
			addNumericField("Radius", 0, 4);
			@SuppressWarnings("rawtypes")
			Vector v = getStringFields();
			tf0 = (TextField)v.get(0);
			tf1 = (TextField)v.get(1);
			v = getNumericFields();
			tf2 = (TextField)v.get(0);
			tf0.addFocusListener(this);
			tf1.addFocusListener(this);
			tf2.addFocusListener(this);
			showDialog();
			if(wasCanceled())
				univ.removeContent(tf0.getText());
			else
				updatePreview();
		}

		@Override
		public String getNameForContent() {
			return tf0.getText();
		}

		@Override
		public Primitive getPrimitive() {
			Point3f center = parsePoint(tf1.getText());
			float radius = Float.parseFloat(tf2.getText());
			return new Sphere(center, radius);
		}
	}

	@SuppressWarnings("serial")
	private final static class ConeDialog extends PrimitiveDialog {
		private final TextField tf0, tf1, tf2, tf3;

		public ConeDialog(final Image3DUniverse univ) {
			super("Cone", univ);
			addStringField("Name", "");
			addStringField("From", "");
			addStringField("To", "");
			addNumericField("Radius", 0, 4);
			@SuppressWarnings("rawtypes")
			Vector v = getStringFields();
			tf0 = (TextField)v.get(0);
			tf1 = (TextField)v.get(1);
			tf2 = (TextField)v.get(2);
			v = getNumericFields();
			tf3 = (TextField)v.get(0);
			tf0.addFocusListener(this);
			tf1.addFocusListener(this);
			tf2.addFocusListener(this);
			tf3.addFocusListener(this);
			showDialog();
			if(wasCanceled())
				univ.removeContent(tf0.getText());
			else
				updatePreview();
		}

		@Override
		public String getNameForContent() {
			return tf0.getText();
		}

		@Override
		public Primitive getPrimitive() {
			Point3f from = parsePoint(tf1.getText());
			Point3f to = parsePoint(tf2.getText());
			float radius = Float.parseFloat(tf3.getText());
			return new Cone(from, to, radius);
		}
	}

	@SuppressWarnings("serial")
	private final static class TubeDialog extends PrimitiveDialog {
		private final TextField tf0;
		private final TextField tf1;
		private final List<TextField> tfs = new ArrayList<TextField>();

		public TubeDialog(final Image3DUniverse univ) {
			super("Tube", univ);
			addStringField("Name", "");
			tf0 = (TextField)getStringFields().get(0);
			addNumericField("Radius", 0, 5);
			tf1 = (TextField)getNumericFields().get(0);
			final Panel p = new Panel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);

			c.gridx = 0;
			c.gridy = 0;
			p.add(new Label("Point1"), c);
			c.gridx++;
			TextField tf = new TextField(10);
			tfs.add(tf);
			tf.addFocusListener(this);
			p.add(tf, c);

			c.gridx = 0;
			c.gridy++;
			p.add(new Label("Point2"), c);
			c.gridx++;
			tf = new TextField(10);
			tfs.add(tf);
			tf.addFocusListener(this);
			p.add(tf, c);

			addPanel(p);
			addMessage("Add Point");
			Component co = getMessage();
			co.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					c.gridy++;
					c.gridx = 0;
					int i = tfs.size() + 1;
					p.add(new Label("Point" + i), c);
					c.gridx++;
					TextField tf = new TextField(10);
					tfs.add(tf);
					tf.addFocusListener(TubeDialog.this);
					p.add(tf, c);
					pack();
				}
			});
			co.setForeground(java.awt.Color.BLUE);
			showDialog();
			if(wasCanceled())
				univ.removeContent(tf0.getText());
			else
				updatePreview();
		}

		@Override
		public String getNameForContent() {
			return tf0.getText();
		}

		@Override
		public Primitive getPrimitive() {
			float radius = Float.parseFloat(tf1.getText());
			if(tfs.size() < 2)
				return null;
			List<Point3f> pts = new ArrayList<Point3f>();
			for(int i = 0; i < tfs.size(); i++)
				pts.add(parsePoint(tfs.get(i).getText()));
			return new Tube(pts, radius);

//			Point3f first = parsePoint(tfs.get(0).getText());
//			Point3f second = parsePoint(tfs.get(1).getText());
//			Tube t = new Tube(first, radius, second, radius);
//			for(int i = 2; i < tfs.size(); i++)
//				t.add(parsePoint(tfs.get(i).getText()), radius);
//			return t;
		}
	}

	public static Point3f parsePoint(String s) {
		Point3f tuple = new Point3f();
		s = s.trim();

		try {
			Matcher m = tuplePattern.matcher(s);
			m.matches();
			tuple.x = Float.parseFloat(m.group(1));
			tuple.y = Float.parseFloat(m.group(2));
			tuple.z = Float.parseFloat(m.group(3));
		} catch(Exception e) {
			throw new IllegalArgumentException("Cannot parse " + s);
		}
		return tuple;
	}
}
