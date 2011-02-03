package ij3d;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

public class ScenegraphDebugger {

	public static void displayTree(Node root) {
		displayTree(root, "");
	}

	private static void displayTree(Node node, String indent) {
		System.out.println(indent + node);
		if(node instanceof Group) {
			Enumeration ch = ((Group)node).getAllChildren();
			while(ch.hasMoreElements())
				displayTree((Node)ch.nextElement(), indent + "   ");
		}
	}

	public static void showTree(Node root) {
		JTree tree = new JTree(new J3DNode(root, null));
		JFrame parent = null;
		JDialog dialog = new JDialog(parent, "Scenegraph");
		JScrollPane scroll = new JScrollPane(tree);
		dialog.getContentPane().add(scroll);
		dialog.pack();
		dialog.setVisible(true);
	}

	private static class J3DNode implements TreeNode {

		private Node node;
		private J3DNode parent;
		private J3DNode[] children = null;

		public J3DNode(Node n, J3DNode parent) {
			this.node = n;
			this.parent = parent;
			if(node instanceof Group) {
				Group g = (Group)node;
				children = new J3DNode[g.numChildren()];
				for(int i = 0; i < children.length; i++)
					children[i] = new J3DNode(g.getChild(i), this);
			}
		}

		public Enumeration children() {
			if(children != null) {
				return Collections.enumeration(Arrays.asList(children));
			}
			return null;
		}

		public boolean getAllowsChildren() {
			return node instanceof Group;
		}

		public TreeNode getChildAt(int arg0) {
			if(!(node instanceof Group))
				return null;
			return children[arg0];
		}

		public int getChildCount() {
			return children.length;
		}

		public int getIndex(TreeNode arg0) {
			for(int i = 0; i < children.length; i++) {
				if(children[i].equals(arg0))
					return i;
			}
			return -1;
		}

		public TreeNode getParent() {
			return parent;
		}

		public boolean isLeaf() {
			return !(node instanceof Group);
		}

		@Override
		public String toString() {
			return node.toString();
		}
	}
}
