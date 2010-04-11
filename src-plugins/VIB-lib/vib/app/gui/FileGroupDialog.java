/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.app.gui;

import ij.io.OpenDialog;
import vib.app.FileGroup;

import java.io.File;

import java.util.Iterator;

import java.awt.List;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Insets;
import java.awt.Font;
import java.awt.ScrollPane;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.TextField;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class FileGroupDialog extends Panel 
				implements ActionListener, DropTargetListener {


	private FileGroup files;
	private boolean showWholePath = false;
	
	private List list;
	private TextField nameTF;
	private Button add, delete, template;
	private Checkbox wholePath;
	private boolean showTemplateButton;

	public FileGroupDialog(FileGroup fg) {
		this(fg,true);
	}

	public FileGroupDialog(FileGroup fg,boolean showTemplateButton) {
		super();
		this.files = fg;
                this.showTemplateButton = showTemplateButton;
		list = new List();
		list.setDropTarget(null);
		DropTarget dropTarget = new DropTarget(list, this);
		createList();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);

		Label name = new Label("Files: ");
		name.setFont(new Font("Monospace", Font.BOLD, 14));
		c.gridx = c.gridy = 0;
		c.insets = new Insets(5,5,5,5);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(name, c);
		this.add(name);

		ScrollPane scroll = new ScrollPane();
		scroll.add(list);
		scroll.setPreferredSize(new Dimension(300,100));
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = c.weighty = 0.5;
		gridbag.setConstraints(scroll, c);
		this.add(scroll);

		Panel buttons = new Panel(new GridLayout(3,1));
		add = new Button("Add to files");
		add.addActionListener(this);
		buttons.add(add);
		delete = new Button("Delete from files");
		delete.addActionListener(this);
		buttons.add(delete);
		template = new Button("Use as template");
		template.addActionListener(this);
                if(showTemplateButton)
        		buttons.add(template);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(buttons, c);
		this.add(buttons);

		wholePath = new Checkbox("Show absolute path", showWholePath);
		wholePath.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showWholePath = wholePath.getState();
				createList();
				repaint();
			}
		});
		c.gridy++;
		c.gridx--;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		gridbag.setConstraints(wholePath, c);
		this.add(wholePath);
		
	}

	public File getSelected() {
		int selected = list.getSelectedIndex();
		if(selected != -1)
			return files.get(selected);
		return null;
	}

	public Button getTemplateButton() {
		return template;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == add) {
			OpenDialog dialog = new OpenDialog("Add file...", "");
			String f = dialog.getDirectory() + dialog.getFileName();
			
			if(f != null)
				if(!files.add(f))
					System.out.println("File " + f + 
					" could not be added to the filegroup");
			createList();
			repaint();			
		} else if(e.getSource() == delete) {
			int[] idx = list.getSelectedIndexes();
			for(int i = 0; i < idx.length; i++){
				files.remove(idx[i]);
			}
			createList();
			repaint();
		}
	}

	public void update() {
		createList();
		repaint();
	}

	private void createList() {
		list.clear();
		for(int i = 0; i < files.size(); i++) {
			if(showWholePath)
				list.add(files.get(i).getAbsolutePath());
			else
				list.add(files.get(i).getName());
		}
	}

	// From: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4899516
	
	private static java.util.List textURIListToFileList(String data) {
		java.util.List list = new java.util.ArrayList(1);
		for (java.util.StringTokenizer st = new java.util.StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
			String s = st.nextToken();
			if (s.startsWith("#")) {
				// the line is a comment (as per the RFC 2483)
				continue;
			}
			try {
				java.net.URI uri = new java.net.URI(s);
				java.io.File file = new java.io.File(uri);
				list.add(file);
			} catch (java.net.URISyntaxException e) {
				// malformed URI
			} catch (IllegalArgumentException e) {
				// the URI is not a valid 'file:' URI
			}
		}
		return list;
	}
	
	// drag & drop
	public void dragEnter(DropTargetDragEvent e) {}
	public void dragExit(DropTargetEvent e) {}
	public void dragOver(DropTargetDragEvent e) {}
	public void dropActionChanged(DropTargetDragEvent e) {}
	public void drop(DropTargetDropEvent e) {
		e.acceptDrop(DnDConstants.ACTION_COPY);
		try {
			Transferable t = e.getTransferable();
			
			java.util.List l = null;
			
			if(t.isDataFlavorSupported(
				   DataFlavor.javaFileListFlavor)) {
				l = (java.util.List) t.getTransferData(
					DataFlavor.javaFileListFlavor);
				
			} else {
				
				// The previous case doesn't work on Gnome, but
				// the code below does.  From:
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4899516
				
				DataFlavor uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
				
				if(t.isDataFlavorSupported(uriListFlavor)) {
					String data = (String)t.getTransferData(uriListFlavor);
					System.out.println("got data: "+data);
					l = textURIListToFileList(data);
				}			    			    
			}
			
                        if(l != null) {
                                Iterator it;
                                for(it = l.iterator(); it.hasNext();) {
                                        File f = (File)it.next();
                                        files.add(f.getAbsolutePath());
                                }
                                update();
                        }
			
			e.dropComplete(true);
		} catch(Exception ex) {
			e.dropComplete(false);
		}
	}

	public static void main(String[] args) {
		FileGroup fg = new FileGroup("TestGroup");
		fg.add("/home/bene/apt_list");
		fg.add("/home/bene/tmp.java");
		fg.add("/home/bene/tmp.class");

		java.awt.Frame f = new java.awt.Frame();
		FileGroupDialog d = new FileGroupDialog(fg);
		f.add(d);
		f.pack();
		f.show();
		fg.debug();
	}
}

