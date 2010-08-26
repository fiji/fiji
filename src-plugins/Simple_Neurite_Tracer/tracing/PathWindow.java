/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.io.*;

import javax.swing.*;

import java.awt.BorderLayout;

import java.io.File;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

public class PathWindow extends JFrame implements PathAndFillListener, TreeSelectionListener, ActionListener {

	public static class HelpfulJTree extends JTree {

		public HelpfulJTree(TreeNode root) {
			super( root );
		}

		public boolean isExpanded( Object [] path ) {
			TreePath tp = new TreePath( path );
			return isExpanded( tp );
		}

		public void setExpanded( Object [] path, boolean expanded ) {
			TreePath tp = new TreePath( path );
			setExpandedState( tp, expanded );
		}

		public void setSelected( Object [] path ) {
			TreePath tp = new TreePath( path );
			setSelectionPath( tp );
		}

	}

	public Set<Path> getSelectedPaths() {
		HashSet<Path> result = new HashSet<Path>();
		TreePath [] selectedPaths = tree.getSelectionPaths();
		if( selectedPaths == null || selectedPaths.length == 0 ) {
			return result;
		}
		for( int i = 0; i < selectedPaths.length; ++i ) {
			TreePath tp = selectedPaths[i];
			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode)(tp.getLastPathComponent());
			if( node != root ) {
				Path p = (Path)node.getUserObject();
				result.add(p);
			}
		}
		return result;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Set<Path> selectedPaths = getSelectedPaths();
		if( source == deleteButton ) {
			if( selectedPaths.isEmpty() ) {
				IJ.error("No paths were selected for deletion");
				return;
			}
			for( Path p : selectedPaths ) {
				p.disconnectFromAll();
				pathAndFillManager.deletePath( p );
			}
		} else if( source == makePrimaryButton ) {
			if( selectedPaths.size() != 1 ) {
				IJ.error("You must have exactly one path selected");
				return;
			}
			Path [] singlePath = selectedPaths.toArray( new Path[] {} );
			Path p = singlePath[0];
			HashSet<Path> pathsExplored = new HashSet<Path>();
			p.setPrimary(true);
			pathsExplored.add(p);
			p.unsetPrimaryForConnected(pathsExplored);
			pathAndFillManager.resetListeners(null);
		} else if( source == exportAsSWCButton ) {
			ArrayList<SWCPoint> swcPoints = null;
			try {
				swcPoints = pathAndFillManager.getSWCFor(selectedPaths);
			} catch( SWCExportException see ) {
				IJ.error(""+see.getMessage());
				return;
			}

			SaveDialog sd = new SaveDialog("Export SWC file ...",
						       plugin.getImagePlus().getShortTitle(),
						       ".swc");

			String savePath;
			if(sd.getFileName()==null) {
				return;
			}

			File saveFile = new File( sd.getDirectory(),
						  sd.getFileName() );
			if ((saveFile!=null)&&saveFile.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Export data...", "The file "+
					    saveFile.getAbsolutePath()+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Exporting SWC data to "+saveFile.getAbsolutePath());

			try {
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(saveFile),"UTF-8"));
				pw.println("# Exported from \"Simple Neurite Tracer\" version "+SimpleNeuriteTracer.PLUGIN_VERSION);
				for( SWCPoint p : swcPoints )
					p.println(pw);
				pw.close();

			} catch( IOException ioe) {
				IJ.error("Saving to "+saveFile.getAbsolutePath()+" failed");
				return;
			}

		} else if( source == fillOutButton ) {
			if( selectedPaths.size() < 1 ) {
				IJ.error("You must have one or more paths in the list selected");
				return;
			}
			plugin.startFillingPaths(selectedPaths);
		} else if( source == fitVolumeButton ) {
			if( selectedPaths.size() < 1 ) {
				IJ.error("You must have one or more paths in the list selected");
				return;
			}
			boolean allAlreadyFitted = allUsingFittedVersion( selectedPaths );
			for( Path p : selectedPaths ) {
				if( allAlreadyFitted ) {
					p.setUseFitted(false, plugin);
				} else {
					if( p.getUseFitted() ) {
						continue;
					}
					if( p.fitted == null ) {
						// There's not already a fitted version:
						Path fitted = p.fitCircles( 40, plugin.getImagePlus(), (e.getModifiers() & ActionEvent.SHIFT_MASK) > 0, plugin );
						if( fitted == null )
							continue;
						p.setFitted(fitted);
						p.setUseFitted(true, plugin);
						pathAndFillManager.addPath( fitted );
					} else {
						// Just use the existing fitted version:
						p.setUseFitted(true, plugin);
					}
				}
			}
			pathAndFillManager.resetListeners(null);
		} else if( source == renameButton ) {
			if( selectedPaths.size() != 1 ) {
				IJ.error("You must have exactly one path selected");
				return;
			}
			Path [] singlePath = selectedPaths.toArray( new Path[] {} );
			Path p = singlePath[0];
			// Pop up the rename dialog:
			String s = (String)JOptionPane.showInputDialog(
				this,
				"Rename this path to:",
				"Rename Path",
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				p.getName() );

			if( s == null )
				return;
			if( s.length() == 0 ) {
				IJ.error("The new name cannot be empty");
				return;
			}
			synchronized( pathAndFillManager) {
				if( pathAndFillManager.getPathFromName( s, false ) != null ) {
					IJ.error("There is already a path with that name ('"+s+"')");
					return;
				}
				// Otherwise this is OK, change the name:
				p.setName(s);
				pathAndFillManager.resetListeners(null);
			}
		} else {
			IJ.error("Unexpectedly got an event from an unknown source");
			return;
		}
	}

	public void updateButtonsNoneSelected( ) {
		renameButton.setEnabled(false);
		fitVolumeButton.setText("Fit Volume");
		fitVolumeButton.setEnabled(false);
		fillOutButton.setEnabled(false);
		makePrimaryButton.setEnabled(false);
		deleteButton.setEnabled(false);
		exportAsSWCButton.setEnabled(false);
	}

	public void updateButtonsOneSelected( Path p ) {
		renameButton.setEnabled(true);
		if( p.getUseFitted() )
			fitVolumeButton.setText("Un-fit Volume");
		else
			fitVolumeButton.setText("Fit Volume");
		fitVolumeButton.setEnabled(true);
		fillOutButton.setEnabled(true);
		makePrimaryButton.setEnabled(true);
		deleteButton.setEnabled(true);
		exportAsSWCButton.setEnabled(true);
	}

	public boolean allSelectedUsingFittedVersion() {
		return allUsingFittedVersion( getSelectedPaths() );
	}

	public boolean allUsingFittedVersion( Set<Path> paths ) {
		for( Path p : paths )
			if( ! p.getUseFitted() ) {
				return false;
			}
		return true;
	}

	public void updateButtonsManySelected( ) {
		renameButton.setEnabled(false);
		{
			if( allSelectedUsingFittedVersion() )
				fitVolumeButton.setText("Un-fit Volumes");
			else
				fitVolumeButton.setText("Fit Volumes");
		}
		fitVolumeButton.setEnabled(true);
		fillOutButton.setEnabled(true);
		makePrimaryButton.setEnabled(false);
		deleteButton.setEnabled(true);
		exportAsSWCButton.setEnabled(true);
	}

	public void valueChanged( TreeSelectionEvent e ) {
		Set<Path> selectedPaths = getSelectedPaths();
		if( selectedPaths.isEmpty() ) {
			pathAndFillManager.setSelected(new Path[]{},this);
			updateButtonsNoneSelected();
		} else {
			Path paths [] = selectedPaths.toArray( new Path[]{} );
			if( selectedPaths.isEmpty() )
				updateButtonsNoneSelected();
			else if( selectedPaths.size() == 1 ) {
				updateButtonsOneSelected(paths[0]);
			} else
				updateButtonsManySelected();
			pathAndFillManager.setSelected(paths,this);
		}
		plugin.update3DViewerContents();
	}

	public static class PathTreeNode extends DefaultMutableTreeNode {
	}

	JScrollPane scrollPane;

	HelpfulJTree tree;
	DefaultMutableTreeNode root;

	JPanel buttonPanel;

	JButton renameButton;
	JButton fitVolumeButton;
	JButton fillOutButton;
	JButton makePrimaryButton;
	JButton deleteButton;
	JButton exportAsSWCButton;

	SimpleNeuriteTracer plugin;
	PathAndFillManager pathAndFillManager;

	public PathWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin) {
		this( pathAndFillManager, plugin, 200, 60 );
	}

	public PathWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin, int x, int y) {
		super("All Paths");

		this.pathAndFillManager = pathAndFillManager;
		this.plugin = plugin;

		setBounds(x,y,700,300);
		root = new DefaultMutableTreeNode("All Paths");
		tree = new HelpfulJTree(root);
		// tree.setRootVisible(false);
		tree.addTreeSelectionListener(this);
		scrollPane = new JScrollPane();
		scrollPane.getViewport().add(tree);
		add(scrollPane, BorderLayout.CENTER);

		buttonPanel = new JPanel();

		renameButton = new JButton("Rename");
		fitVolumeButton = new JButton("Fit Volume");
		fillOutButton = new JButton("Fill Out");
		makePrimaryButton = new JButton("Make Primary");
		deleteButton = new JButton("Delete");
		exportAsSWCButton = new JButton("Export as SWC");

		buttonPanel.add(renameButton);
		buttonPanel.add(fitVolumeButton);
		buttonPanel.add(fillOutButton);
		buttonPanel.add(makePrimaryButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(exportAsSWCButton);

		renameButton.addActionListener(this);
		fitVolumeButton.addActionListener(this);
		fillOutButton.addActionListener(this);
		makePrimaryButton.addActionListener(this);
		deleteButton.addActionListener(this);
		exportAsSWCButton.addActionListener(this);

		add(buttonPanel, BorderLayout.PAGE_END);

	}

	void setButtonsEnabled( boolean enable ) {
		renameButton.setEnabled(enable);
		fitVolumeButton.setEnabled(enable);
		fillOutButton.setEnabled(enable);
		makePrimaryButton.setEnabled(enable);
		deleteButton.setEnabled(enable);
		exportAsSWCButton.setEnabled(enable);
	}

	void getExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( tree.isExpanded( (Object[])(child.getPath()) ) ) {
				set.add(p);
			}
			if( ! model.isLeaf(child) )
				getExpandedPaths( tree, model, child, set );
		}
	}

	void setExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set, Path justAdded ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( set.contains(p) || ((justAdded != null) && (justAdded == p)) ) {
				tree.setExpanded( (Object[])(child.getPath()), true );
			}
			if( ! model.isLeaf(child) )
				setExpandedPaths( tree, model, child, set, justAdded );
		}

	}

	public void setSelectedPaths( HashSet selectedPaths, Object source ) {
		if( source == this )
			return;
		TreePath [] noTreePaths = {};
		tree.setSelectionPaths( noTreePaths );
		setSelectedPaths( tree, tree.getModel(), root, selectedPaths );
	}

	void setSelectedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( set.contains(p) ) {
				tree.setSelected( (Object[])(child.getPath()) );
			}
			if( ! model.isLeaf(child) )
				setSelectedPaths( tree, model, child, set );
		}

	}

	public void setPathList( String [] pathList, Path justAdded, boolean expandAll ) {

		// Save the selection state:

		TreePath [] selectedBefore = tree.getSelectionPaths();
		HashSet selectedPathsBefore = new HashSet();
		HashSet expandedPathsBefore = new HashSet();

		if( selectedBefore != null )
			for( int i = 0; i < selectedBefore.length; ++i ) {
				TreePath tp = selectedBefore[i];
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tp.getLastPathComponent();
				if( dmtn != root ) {
					Path p = (Path)dmtn.getUserObject();
					selectedPathsBefore.add(p);
				}
			}

		// Save the expanded state:
		getExpandedPaths( tree, tree.getModel(), root, expandedPathsBefore );

		/* Ignore the arguments and get the real path list
		   from the PathAndFillManager: */

		DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("All Paths");
		DefaultTreeModel model = new DefaultTreeModel(newRoot);
		// DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		Path [] primaryPaths = pathAndFillManager.getPathsStructured();
		for( int i = 0; i < primaryPaths.length; ++i ) {
			Path primaryPath = primaryPaths[i];
			// Add the primary path if it's not just a fitted version of another:
			if( primaryPath.fittedVersionOf == null )
				addNode( newRoot, primaryPath, model );
		}
		root = newRoot;
		tree.setModel(model);

		model.reload();

		// Set back the expanded state:
		if( expandAll ) {
			for( int i = 0; i < tree.getRowCount(); ++i )
				tree.expandRow(i);
		} else
			setExpandedPaths( tree, model, root, expandedPathsBefore, justAdded );

		setSelectedPaths( tree, model, root, selectedPathsBefore );
	}

	public void addNode( MutableTreeNode parent, Path childPath, DefaultTreeModel model ) {
		MutableTreeNode newNode = new DefaultMutableTreeNode(childPath);
		model.insertNodeInto(newNode, parent, parent.getChildCount());
		for( Path p : childPath.children )
			addNode( newNode, p, model );
	}

	public void setFillList( String [] fillList ) {

	}

	public void setSelectedPaths( int [] selectedIndices ) {

	}
}
