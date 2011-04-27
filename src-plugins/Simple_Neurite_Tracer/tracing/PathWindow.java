/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("serial")
public class PathWindow extends JFrame implements PathAndFillListener, TreeSelectionListener, ActionListener {

	public static class HelpfulJTree extends JTree {

		public HelpfulJTree(TreeNode root) {
			super( root );
			assert SwingUtilities.isEventDispatchThread();
		}

		public boolean isExpanded( Object [] path ) {
			assert SwingUtilities.isEventDispatchThread();
			TreePath tp = new TreePath( path );
			return isExpanded( tp );
		}

		public void setExpanded( Object [] path, boolean expanded ) {
			assert SwingUtilities.isEventDispatchThread();
			TreePath tp = new TreePath( path );
			setExpandedState( tp, expanded );
		}

		public void setSelected( Object [] path ) {
			assert SwingUtilities.isEventDispatchThread();
			TreePath tp = new TreePath( path );
			setSelectionPath( tp );
		}

	}

	public Set<Path> getSelectedPaths() {

		return SwingSafeResult.getResult( new Callable<Set<Path>>() {
			public Set<Path> call() {
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
		});
	}

	public void fitPaths( final List<PathFitter> pathsToFit ) {

		final int numberOfPathsToFit = pathsToFit.size();

		new Thread( new Runnable(){
				public void run(){

					final int preFittingState = plugin.getUIState();
					plugin.changeUIState(NeuriteTracerResultsDialog.FITTING_PATHS);

					try {

						final FittingProgress progress = new FittingProgress(numberOfPathsToFit);
						for( int i = 0; i < numberOfPathsToFit; ++i ) {
							PathFitter pf = pathsToFit.get(i);
							pf.setProgressCallback( i, progress );
						}
						final int processors = Runtime.getRuntime().availableProcessors();
						ExecutorService es = Executors.newFixedThreadPool(processors);
						final List<Future<Path>> futures = es.invokeAll(pathsToFit);
						SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									try {
										for( Future<Path> future : futures ) {
											Path result = future.get();
											pathAndFillManager.addPath( result );
										}
									} catch( Exception e ) {
										IJ.error("The following exception was thrown: "+e);
										e.printStackTrace();
										return;
									}
									pathAndFillManager.resetListeners(null);
									progress.done();
								}});
					} catch( InterruptedException ie ) {
						/* We never call interrupt on these threads,
						   so this should never happen... */
					} finally {
						plugin.changeUIState(preFittingState);
					}
				}
			}).start();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		assert SwingUtilities.isEventDispatchThread();
		Object source = e.getSource();
		Set<Path> selectedPaths = getSelectedPaths();
		if( source == deleteButton || source == deleteMenuItem ) {
			if( selectedPaths.isEmpty() ) {
				IJ.error("No paths were selected for deletion");
				return;
			}
			int n = selectedPaths.size();
			String message = "Are you sure you want to delete ";
			if( n == 1 ) {
				message += "the path \""+selectedPaths.iterator().next()+"\"";
			} else {
				message += "these "+n+" paths?";
			}
			message += "?";
			if (!IJ.showMessageWithCancel("Delete paths...",message))
				return;
			for( Path p : selectedPaths ) {
				p.disconnectFromAll();
				pathAndFillManager.deletePath( p );
			}
		} else if( source == makePrimaryButton || source == makePrimaryMenuItem ) {
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
		} else if( source == exportAsSWCButton || source == exportAsSWCMenuItem ) {
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

		} else if( source == fillOutButton || source == fillOutMenuItem) {
			if( selectedPaths.size() < 1 ) {
				IJ.error("You must have one or more paths in the list selected");
				return;
			}
			plugin.startFillingPaths(selectedPaths);
		} else if( source == fitVolumeButton || source == fitVolumeMenuItem) {
			if( selectedPaths.size() < 1 ) {
				IJ.error("You must have one or more paths in the list selected");
				return;
			}
			boolean showDetailedFittingResults = (e.getModifiers() & ActionEvent.SHIFT_MASK) > 0;

			final ArrayList<PathFitter> pathsToFit = new ArrayList<PathFitter>();
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
						PathFitter pathFitter = new PathFitter(
							plugin,
							p,
							showDetailedFittingResults );
						pathsToFit.add(pathFitter);
					} else {
						// Just use the existing fitted version:
						p.setUseFitted(true, plugin);
					}
				}
			}
			pathAndFillManager.resetListeners(null);

			if( pathsToFit.size() > 0 )
				fitPaths(pathsToFit);

		} else if( source == renameButton || source == renameMenuItem ) {
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
			// Check if the source was from one of the SWC menu
			// items:
			int swcType = -1;
			int i = 0;
			for( JMenuItem menuItem : swcTypeMenuItems ) {
				if( source == menuItem ) {
					swcType = i;
					break;
				}
				++i;
			}

			if( swcType >= 0 ) {
				for( Path p : selectedPaths )
					p.setSWCType(swcType);
				pathAndFillManager.resetListeners(null);
			} else {

				IJ.error("Unexpectedly got an event from an unknown source");
				return;
			}
		}
	}

	protected void renameSetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		renameButton.setEnabled(enabled);
		renameMenuItem.setEnabled(enabled);
	}
	protected void fitVolumeSetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		fitVolumeButton.setEnabled(enabled);
		fitVolumeMenuItem.setEnabled(enabled);
	}
	protected void fillOutSetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		fillOutButton.setEnabled(enabled);
		fillOutMenuItem.setEnabled(enabled);
	}
	protected void makePrimarySetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		makePrimaryButton.setEnabled(enabled);
		makePrimaryMenuItem.setEnabled(enabled);
	}
	protected void deleteSetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		deleteButton.setEnabled(enabled);
		deleteMenuItem.setEnabled(enabled);
	}
	protected void exportAsSWCSetEnabled(boolean enabled) {
		assert SwingUtilities.isEventDispatchThread();
		exportAsSWCButton.setEnabled(enabled);
		exportAsSWCMenuItem.setEnabled(enabled);
	}

	protected void fitVolumeSetText(String s) {
		assert SwingUtilities.isEventDispatchThread();
		fitVolumeButton.setText(s);
		fitVolumeMenuItem.setText(s);
	}

	protected void updateButtonsNoneSelected( ) {
		assert SwingUtilities.isEventDispatchThread();
		renameSetEnabled(false);
		fitVolumeSetText("Fit Volume");
		fitVolumeSetEnabled(false);
		fillOutSetEnabled(false);
		makePrimarySetEnabled(false);
		deleteSetEnabled(false);
		exportAsSWCSetEnabled(false);
		swcTypeMenu.setEnabled(false);
	}

	protected void updateButtonsOneSelected( Path p ) {
		assert SwingUtilities.isEventDispatchThread();
		renameSetEnabled(true);
		if( p.getUseFitted() )
			fitVolumeSetText("Un-fit Volume");
		else
			fitVolumeSetText("Fit Volume");
		fitVolumeSetEnabled(true);
		fillOutSetEnabled(true);
		makePrimarySetEnabled(true);
		deleteSetEnabled(true);
		exportAsSWCSetEnabled(true);
		swcTypeMenu.setEnabled(true);
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

	protected void updateButtonsManySelected( ) {
		assert SwingUtilities.isEventDispatchThread();
		renameSetEnabled(false);
		{
			if( allSelectedUsingFittedVersion() )
				fitVolumeSetText("Un-fit Volumes");
			else
				fitVolumeSetText("Fit Volumes");
		}
		fitVolumeSetEnabled(true);
		fillOutSetEnabled(true);
		makePrimarySetEnabled(false);
		deleteSetEnabled(true);
		exportAsSWCSetEnabled(true);
		swcTypeMenu.setEnabled(true);
	}

	@Override
	public void valueChanged( TreeSelectionEvent e ) {
		assert SwingUtilities.isEventDispatchThread();
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

	protected JScrollPane scrollPane;

	protected HelpfulJTree tree;
	protected DefaultMutableTreeNode root;

	protected JPopupMenu popup;
	protected JMenu swcTypeMenu;

	protected JPanel buttonPanel;

	protected JButton renameButton;
	protected JButton fitVolumeButton;
	protected JButton fillOutButton;
	protected JButton makePrimaryButton;
	protected JButton deleteButton;
	protected JButton exportAsSWCButton;

	protected JMenuItem renameMenuItem;
	protected JMenuItem fitVolumeMenuItem;
	protected JMenuItem fillOutMenuItem;
	protected JMenuItem makePrimaryMenuItem;
	protected JMenuItem deleteMenuItem;
	protected JMenuItem exportAsSWCMenuItem;

	protected ArrayList<JMenuItem> swcTypeMenuItems = new ArrayList<JMenuItem>();

	protected SimpleNeuriteTracer plugin;
	protected PathAndFillManager pathAndFillManager;

	public PathWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin) {
		this( pathAndFillManager, plugin, 200, 60 );
	}

	public PathWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin, final int x, final int y) {
		super("All Paths");
		assert SwingUtilities.isEventDispatchThread();

		new ClarifyingKeyListener().addKeyAndContainerListenerRecursively(this);

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

		add(buttonPanel, BorderLayout.PAGE_END);

		// Create all the menu items:

		popup = new JPopupMenu();

		renameMenuItem = new JMenuItem("Rename");
		fitVolumeMenuItem = new JMenuItem("Fit Volume");
		fillOutMenuItem = new JMenuItem("Fill Out");
		makePrimaryMenuItem = new JMenuItem("Make Primary");
		deleteMenuItem = new JMenuItem("Delete");
		exportAsSWCMenuItem = new JMenuItem("Export as SWC");

		popup.add(renameMenuItem);
		popup.add(fitVolumeMenuItem);
		popup.add(fillOutMenuItem);
		popup.add(makePrimaryMenuItem);
		popup.add(deleteMenuItem);
		popup.add(exportAsSWCMenuItem);

		renameMenuItem.addActionListener(this);
		fitVolumeMenuItem.addActionListener(this);
		fillOutMenuItem.addActionListener(this);
		makePrimaryMenuItem.addActionListener(this);
		deleteMenuItem.addActionListener(this);
		exportAsSWCMenuItem.addActionListener(this);

		// Now also add the SWC types submenu:
		swcTypeMenu = new JMenu("Set SWC type");
		for( String s : Path.swcTypeNames ) {
			JMenuItem jmi = new JMenuItem(s);
			jmi.addActionListener(this);
			swcTypeMenu.add(jmi);
			swcTypeMenuItems.add(jmi);
		}
		popup.add(swcTypeMenu);

		// Create all the menu items:

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

		MouseListener ml = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				maybeShowPopup(me);
			}
			@Override
			public void mouseReleased(MouseEvent me) {
				maybeShowPopup(me);
			}
			protected void maybeShowPopup(MouseEvent me) {
				if( me.isPopupTrigger() )
					showPopup(me);
			}
		};
		tree.addMouseListener(ml);
	}

	protected void showPopup(MouseEvent me) {
		assert SwingUtilities.isEventDispatchThread();
		// Possibly adjust the selection here:
		popup.show(me.getComponent(),
				me.getX(),
				me.getY());
	}

	protected void setButtonsEnabled( final boolean enable ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				renameSetEnabled(enable);
				fitVolumeSetEnabled(enable);
				fillOutSetEnabled(enable);
				makePrimarySetEnabled(enable);
				deleteSetEnabled(enable);
				exportAsSWCSetEnabled(enable);
			}
		});
	}

	protected void getExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet<Path> set ) {
		assert SwingUtilities.isEventDispatchThread();
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

	protected void setExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet<Path> set, Path justAdded ) {
		assert SwingUtilities.isEventDispatchThread();
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

	@Override
	public void setSelectedPaths( final HashSet<Path> selectedPaths, final Object source ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( source == this )
					return;
				TreePath [] noTreePaths = {};
				tree.setSelectionPaths( noTreePaths );
				setSelectedPaths( tree, tree.getModel(), root, selectedPaths );
			}
		});
	}

	protected void setSelectedPaths( final HelpfulJTree tree, final TreeModel model, final MutableTreeNode node, final HashSet<Path> set ) {
		assert SwingUtilities.isEventDispatchThread();
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

	@Override
	public void setPathList( String [] pathList, final Path justAdded, final boolean expandAll ) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// Save the selection state:

				TreePath [] selectedBefore = tree.getSelectionPaths();
				HashSet<Path> selectedPathsBefore = new HashSet<Path>();
				HashSet<Path> expandedPathsBefore = new HashSet<Path>();

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
		});
	}

	protected void addNode( MutableTreeNode parent, Path childPath, DefaultTreeModel model ) {
		assert SwingUtilities.isEventDispatchThread();
		MutableTreeNode newNode = new DefaultMutableTreeNode(childPath);
		model.insertNodeInto(newNode, parent, parent.getChildCount());
		for( Path p : childPath.children )
			addNode( newNode, p, model );
	}

	@Override
	public void setFillList( String [] fillList ) {

	}

}
