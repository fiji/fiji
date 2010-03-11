package fiji.debugging;

import ij.plugin.PlugIn;

import java.awt.Dimension;
import java.awt.Frame;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;

import javax.swing.tree.DefaultMutableTreeNode;

public class Object_Inspector implements PlugIn, TreeWillExpandListener {
	DefaultMutableTreeNode windows, root;
	
	public void run(String arg) {
		windows = new DefaultMutableTreeNode("Windows");
		openFrame("Windows", windows);
	}

	public static void openFrame(String label, Object object) {
		new Object_Inspector().openFrame(label,
				new ObjectWrapper(label, object));
	}

	public void openFrame(String label, DefaultMutableTreeNode node) {
		node(node, label);
		
		root = new DefaultMutableTreeNode("Root");
		root.add(node);
		
		JTree tree = new JTree(root);
		tree.setRootVisible(false);
		tree.addTreeWillExpandListener(this);

		JScrollPane pane = new JScrollPane(tree);
		//pane.setMinimumSize(new Dimension(300, 100));
		
		JFrame frame = new JFrame("Object Inspector");
		frame.setContentPane(pane);
		frame.setMinimumSize(new Dimension(600, 500));
		frame.pack();
		frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	public void treeWillCollapse(TreeExpansionEvent e) {}
	
	public void treeWillExpand(TreeExpansionEvent e) {
		Object leaf = e.getPath().getLastPathComponent();
		if (leaf == windows) {
			windows.removeAllChildren();
			for (Frame frame : Frame.getFrames())
				windows.add(new ObjectWrapper(frame));
		}
		else if (leaf instanceof ObjectWrapper) {
			ObjectWrapper wrapper = (ObjectWrapper)leaf;
			if (wrapper.initialized)
				return;
			wrapper.removeAllChildren();
			Object object = wrapper.getUserObject();
			if (object == null) {
				node(wrapper, "<null>");
				return;
			}
			
			if (object.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(object); i++)
					wrapper.add(new ObjectWrapper(object.getClass().getComponentType(), "[" + i + "]", Array.get(object, i)));
				return;
			}
			
			Class clazz = object.getClass();
			while (clazz != null && clazz != Object.class) {
				node(wrapper, "Fields of " + clazz.getName());
				for (Field field : clazz.getDeclaredFields())
					wrapper.add(new ObjectWrapper(field, object));
				clazz = clazz.getSuperclass();
			}
			wrapper.initialized = true;
		}
	}

	static void node(DefaultMutableTreeNode parent, String title) {
		parent.add(new DefaultMutableTreeNode(title));
	}
	
	static Object get(Field field, Object object) {
		field.setAccessible(true);
		try {
			return field.get(object);
		} catch (Exception e) { /* ignore */ }
		return null;
	}

	static String getName(Class clazz, Object object) {
		if (clazz.isArray())
			return clazz.getComponentType() + "[" + (object == null ? "": "" + Array.getLength(object)) + "]";
		return clazz.getName();
	}
	
	static class ObjectWrapper extends DefaultMutableTreeNode {
		String title;
		boolean initialized;
		
		public ObjectWrapper(String title, Object object) {
			super(object);
			this.title = title;
			node(this, "Dummy");
		}

		public ObjectWrapper(Frame frame) {
			this(frame.getTitle(), frame);
		}

		public ObjectWrapper(Field field, Object object) {
			this(field.getType(),  field.getName()
				+ ((field.getModifiers() & Modifier.STATIC) != 0 ? " [static]" : "")
				+ " (" + getName(field.getType(), get(field, object)) + ")", get(field, object));
		}
		
		public ObjectWrapper(Class clazz, String title, Object object) {
			super(object);
			this.title = title;
			if (clazz.isPrimitive() || clazz == String.class) {
				this.title += ": " + toString(object);
				initialized = true;
			}
			else
				node(this, "Dummy");
		}

		static String toString(Object object) {
			return object == null ? "<null>" : object.toString();
		}
		
		public String toString() {
			return title;
		}
	}
}
