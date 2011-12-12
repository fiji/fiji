package ij3d.gui;

import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import math3d.TransformIO;

@SuppressWarnings("serial")
public class InteractiveTransformDialog extends GenericDialog {

	private static final String ws = "\\s*";
	private static final String no = "(\\S*?)";

	private static final Pattern matrixPattern = Pattern.compile(
		ws + no + ws + no + ws + no + ws + no + ws + "\n" +
		ws + no + ws + no + ws + no + ws + no + ws + "\n" +
		ws + no + ws + no + ws + no + ws + no + ws);

	private static final Pattern vectorPattern = Pattern.compile(
		"\\(?" + no + "," + ws + no + "," + ws + no + "\\)?");

	private Vector3f axis = new Vector3f(0, 1, 0);
	private Vector3f origin = new Vector3f();
	private float angle = 0;
	private Vector3f translation = new Vector3f();
	private Point3f contentCenter;

	private TextField axisTF, angleTF, originTF, translationTF;
	private TextArea matrixTA;

	public InteractiveTransformDialog(String title, Point3f contentCenter, Matrix4f m) {
		super(title);
		this.contentCenter = contentCenter;

		addStringField("Rotation origin", "C", 15);
		originTF = (TextField)getStringFields().lastElement();
		addTextListener(originTF);
		addStringField("Rotation axis", toString(axis), 15);
		axisTF = (TextField)getStringFields().lastElement();
		addTextListener(axisTF);
		addNumericField("Angle (in deg)", angle, 5);
		angleTF = (TextField)getNumericFields().lastElement();
		addTextListener(angleTF);
		addStringField("Translation", toString(translation), 15);
		translationTF = (TextField)getStringFields().lastElement();
		addTextListener(translationTF);
		addTextAreas("", null, 4, 50);
		matrixTA = getTextArea1();
		matrixTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
		matrixTA.setColumns(50);
		matrixTA.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				Matrix4f m = new Matrix4f();
				try {
					fromString(matrixTA.getText(), m);
					if(m.determinant() != 0)
						setTransformation(m, origin, true);
				} catch(Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		});
		matrixTA.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				Matrix4f m = new Matrix4f();
				try {
					fromString(matrixTA.getText(), m);
					if(m.determinant() != 0)
						setTransformation(m, origin, false);
				} catch(Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		});

		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Open from file");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float[] m = new TransformIO().
						openAffineTransform();
				if(m != null)
					setTransformation(new Matrix4f(m), origin, true);
			}
		});
		p.add(b);
		addPanel(p);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					if(wasOKed())
						oked(fromFields());
					else
						canceled();

				} catch(Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		});
		addMessage("You can specify vectors either by tuples in the \n" +
			"form of (x, y, z) or using the following letters \n" +
			"as abbreviations:\n  \n" +
			"X for (1, 0, 0) \n" +
			"Y for (0, 1, 0) \n" +
			"Z for (0, 0, 1) \n" +
			"O for (0, 0, 0) \n" +
			"C for the object center " + contentCenter);
		setModal(false);
		setTransformation(m, new Vector3f(contentCenter), true);
		showDialog();
	}

	private void setTransformation(Matrix4f m, Vector3f center, boolean setMatrixField) {
		origin.set(center);
		try {
			AxisAngle4f rot = new AxisAngle4f();
			decompose(m, origin, rot, translation);
			angle = (float)(180 * rot.getAngle() / Math.PI);
			axis.x = rot.x; axis.y = rot.y; axis.z = rot.z;

			// update textfields
			axisTF.setText(toString(axis));
			angleTF.setText(Float.toString(angle));
			originTF.setText(toString(origin));
			translationTF.setText(toString(translation));
			if(setMatrixField)
				matrixTA.setText(toString(m));
			transformationUpdated(m);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

	protected void transformationUpdated(Matrix4f matrix) {
	}

	protected void oked(Matrix4f mat) {
	}

	protected void canceled() {
	}

	private Matrix4f fromFields() {
		fromString(axisTF.getText(), axis);
		angle = (float)(Math.PI * Float.parseFloat(angleTF.getText()) / 180.0);
		fromString(originTF.getText(), origin);
		fromString(translationTF.getText(), translation);

		final Matrix4f m = new Matrix4f();
		compose(new AxisAngle4f(axis, angle), origin, translation, m);
		return m;

	}

	private void addTextListener(final TextField tf) {
		tf.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				try {
					Matrix4f m = fromFields();
					matrixTA.setText(InteractiveTransformDialog.this.toString(m));
					transformationUpdated(m);
				} catch(Exception e) {
					System.out.println(e.getMessage());
				}
			}
		});
	}

	public static void compose(AxisAngle4f rot, Vector3f origin, Vector3f translation, Matrix4f ret) {
		ret.set(rot);
		Vector3f trans = new Vector3f(origin);
		trans.scale(-1);
		ret.transform(trans);
		trans.add(translation);
		trans.add(origin);

		ret.setTranslation(trans);
	}

	public static void decompose(Matrix4f mat, Vector3f origin, AxisAngle4f rotation, Vector3f translation) {
		Matrix3f rot = new Matrix3f();
		mat.get(rot);
		rotation.set(rot);
		Point3f tmp = new Point3f(origin);
		rot.transform(tmp);
		tmp.sub(origin);
		mat.get(translation);
		translation.add(tmp);
	}

	public String toString(Matrix4f mat) {
		return
			String.format("% 10.3f  % 10.3f  % 10.3f  % 10.3f\n", mat.m00, mat.m01, mat.m02, mat.m03) +
			String.format("% 10.3f  % 10.3f  % 10.3f  % 10.3f\n", mat.m10, mat.m11, mat.m12, mat.m13) +
			String.format("% 10.3f  % 10.3f  % 10.3f  % 10.3f",   mat.m20, mat.m21, mat.m22, mat.m23);
	}

	public void fromString(String s, Matrix4f mat) {
		try {
			Matcher m = matrixPattern.matcher(s);
			m.matches();
			float[] v = new float[16];
			for(int i = 0; i < 12; i++)
				v[i] = Float.parseFloat(m.group(i + 1));

			v[15] = 1;
			mat.set(v);
		} catch(Exception e) {
			throw new IllegalArgumentException("Cannot parse " + s, e);
		}
	}

	public String toString(Tuple3f tuple) {
		if(tuple.x == 0 && tuple.y == 0 && tuple.z == 0)
			return "O";
		if(tuple.x == 1 && tuple.y == 0 && tuple.z == 0)
			return "X";
		if(tuple.x == 0 && tuple.y == 1 && tuple.z == 0)
			return "Y";
		if(tuple.x == 0 && tuple.y == 0 && tuple.z == 1)
			return "Z";
		if(tuple.equals(contentCenter))
			return "C";
		return "(" + tuple.x + ", " + tuple.y + ", " + tuple.z + ")";
	}

	public void fromString(String s, Tuple3f tuple) {
		s = s.trim();
		if(s.equalsIgnoreCase("X"))
			s = "1, 0, 0";
		else if(s.equalsIgnoreCase("Y"))
			s = "0, 1, 0";
		else if(s.equalsIgnoreCase("Z"))
			s = "0, 0, 1";
		else if(s.equalsIgnoreCase("O") || s.equals("0"))
			s = "0, 0, 0";
		else if(s.equalsIgnoreCase("C"))
			s = contentCenter.x + ", " + contentCenter.y + ", " + contentCenter.z;

		try {
			Matcher m = vectorPattern.matcher(s);
			m.matches();
			tuple.x = Float.parseFloat(m.group(1));
			tuple.y = Float.parseFloat(m.group(2));
			tuple.z = Float.parseFloat(m.group(3));
		} catch(Exception e) {
			throw new IllegalArgumentException("Cannot parse " + s);
		}
	}
}
