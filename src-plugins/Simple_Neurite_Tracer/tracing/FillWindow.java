/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.util.HashSet;
import java.util.Iterator;

public class FillWindow extends JFrame implements PathAndFillListener, ActionListener, ItemListener, FillerProgressCallback {

	SimpleNeuriteTracer plugin;
	PathAndFillManager pathAndFillManager;

	public FillWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin) {
		this( pathAndFillManager, plugin, 200, 60 );
	}

	JScrollPane scrollPane;

	JList fillList;
	DefaultListModel listModel;

	JButton deleteFills;
	JButton reloadFill;

	JPanel fillControlPanel;

	JLabel fillStatus;

	float maxThresholdValue = 0;

	JTextField thresholdField;
	JLabel maxThreshold;
	JButton setThreshold;
	JButton setMaxThreshold;

	JButton view3D;
	JCheckBox maskNotReal;
	JCheckBox transparent;

	boolean currentlyFilling = true;
	JButton pauseOrRestartFilling;

	JButton saveFill;
	JButton discardFill;

	public void setControlsEnabled( boolean enable ) {

	}

	public void setEnabledWhileFilling( ) {
		fillList.setEnabled(false);
		deleteFills.setEnabled(false);
		reloadFill.setEnabled(false);
		fillStatus.setEnabled(true);
		thresholdField.setEnabled(true);
		maxThreshold.setEnabled(true);
		setThreshold.setEnabled(true);
		setMaxThreshold.setEnabled(true);
		view3D.setEnabled(true);
		maskNotReal.setEnabled(true);
		transparent.setEnabled(true);
		pauseOrRestartFilling.setEnabled(true);
		saveFill.setEnabled(false);
		discardFill.setEnabled(true);
	}

	public void setEnabledWhileNotFilling( ) {
		fillList.setEnabled(true);
		deleteFills.setEnabled(true);
		reloadFill.setEnabled(true);
		fillStatus.setEnabled(true);
		thresholdField.setEnabled(false);
		maxThreshold.setEnabled(false);
		setThreshold.setEnabled(false);
		setMaxThreshold.setEnabled(false);
		view3D.setEnabled(false);
		maskNotReal.setEnabled(false);
		transparent.setEnabled(false);
		pauseOrRestartFilling.setEnabled(false);
		saveFill.setEnabled(false);
		discardFill.setEnabled(false);
	}

	public void setEnabledNone( ) {
		fillList.setEnabled(false);
		deleteFills.setEnabled(false);
		reloadFill.setEnabled(false);
		fillStatus.setEnabled(false);
		thresholdField.setEnabled(false);
		maxThreshold.setEnabled(false);
		setThreshold.setEnabled(false);
		setMaxThreshold.setEnabled(false);
		view3D.setEnabled(false);
		maskNotReal.setEnabled(false);
		transparent.setEnabled(false);
		pauseOrRestartFilling.setEnabled(false);
		saveFill.setEnabled(false);
		discardFill.setEnabled(false);
	}

	public FillWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin, int x, int y) {
		super("All Fills");
		this.plugin = plugin;
		this.pathAndFillManager = pathAndFillManager;
		setBounds(x,y,400,400);

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		listModel = new DefaultListModel();
		fillList = new JList(listModel);

		scrollPane = new JScrollPane();
		scrollPane.getViewport().add(fillList);

		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets( 8, 8, 1, 8 );
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		add( scrollPane, c );

		{
			JPanel fillListCommandsPanel = new JPanel();
			fillListCommandsPanel.setLayout(new BorderLayout());

			deleteFills = new JButton("Delete Fill(s)");
			deleteFills.addActionListener( this );
			fillListCommandsPanel.add(deleteFills,BorderLayout.WEST);

			reloadFill = new JButton("Reload Fill");
			reloadFill.addActionListener( this );
			fillListCommandsPanel.add(reloadFill,BorderLayout.CENTER);

			c.insets = new Insets( 1, 8, 8, 8 );
			c.gridx = 0;
			++ c.gridy;

			add(fillListCommandsPanel,c);
		}

		{

			JPanel fillingOptionsPanel = new JPanel();

			fillingOptionsPanel.setLayout(new GridBagLayout());

			GridBagConstraints cf = new GridBagConstraints();

			cf.gridx = 0;
			cf.gridy = 0;
			cf.gridwidth = 4;
			cf.weightx = 1;
			cf.anchor = GridBagConstraints.LINE_START;
			cf.fill = GridBagConstraints.HORIZONTAL;
			fillStatus = new JLabel("(Not filling at the moment.)");
			fillingOptionsPanel.add(fillStatus,cf);

			thresholdField = new JTextField("",20);
			thresholdField.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 1;
			cf.weightx = 0;
			cf.gridwidth = 2;
			cf.fill = GridBagConstraints.NONE;
			fillingOptionsPanel.add(thresholdField,cf);

			maxThreshold = new JLabel("(0)                  ",JLabel.LEFT);
			cf.gridx = 2;
			cf.gridy = 1;
			cf.gridwidth = 1;
			cf.fill = GridBagConstraints.HORIZONTAL;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(maxThreshold,cf);

			setThreshold = new JButton("Set");
			setThreshold.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 2;
			cf.gridwidth = 1;
			cf.fill = GridBagConstraints.NONE;
			fillingOptionsPanel.add(setThreshold,cf);

			setMaxThreshold = new JButton("Set Max");
			setMaxThreshold.addActionListener(this);
			cf.gridx = 1;
			cf.gridy = 2;
			fillingOptionsPanel.add(setMaxThreshold,cf);

			view3D = new JButton("Create Image Stack from Fill");
			view3D.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 3;
			cf.gridwidth = 2;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(view3D,cf);

			maskNotReal = new JCheckBox("Create as Mask");
			maskNotReal.addItemListener(this);
			cf.gridx = 0;
			cf.gridy = 4;
			cf.gridwidth = 3;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(maskNotReal,cf);

			transparent = new JCheckBox("Transparent fill display (slow!)");
			transparent.addItemListener(this);
			cf.gridx = 0;
			cf.gridy = 5;
			cf.gridwidth = 3;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(transparent,cf);

			{
				fillControlPanel = new JPanel();
				fillControlPanel.setLayout(new BorderLayout());

				pauseOrRestartFilling = new JButton("Pause");
				currentlyFilling = true;
				pauseOrRestartFilling.addActionListener(this);
				fillControlPanel.add(pauseOrRestartFilling,BorderLayout.WEST);

				saveFill = new JButton("Save Fill");
				saveFill.addActionListener(this);
				fillControlPanel.add(saveFill,BorderLayout.CENTER);

				discardFill = new JButton("Cancel Fill");
				discardFill.addActionListener(this);
				fillControlPanel.add(discardFill,BorderLayout.EAST);

				cf.gridx = 0;
				cf.gridy = 6;
				cf.gridwidth = 3;
				cf.fill = GridBagConstraints.HORIZONTAL;
				cf.anchor = GridBagConstraints.LINE_START;

				fillingOptionsPanel.add(fillControlPanel,cf);
			}

			c.gridx = 0;
			++ c.gridy;
			c.insets = new Insets( 8, 8, 8, 8 );
			add(fillingOptionsPanel,c);
		}

		deleteFills.addActionListener(this);
		reloadFill.addActionListener(this);
	}

	public void setPathList( String [] pathList, Path justAdded, boolean expandAll ) { }

	public void setFillList( String [] newList ) {
		listModel.removeAllElements();
		for( int i = 0; i < newList.length; ++i )
			listModel.addElement( newList[i] );
	}

	public void setSelectedPaths( HashSet selectedPathSet, Object source ) {

	}

	public void actionPerformed(ActionEvent ae) {

		Object source = ae.getSource();

		if( source == deleteFills ) {

			int [] selectedIndices = fillList.getSelectedIndices();
			if( selectedIndices.length < 1 ) {
				IJ.error("No fill was selected for deletion");
				return;
			}
			pathAndFillManager.deleteFills( selectedIndices );
			plugin.repaintAllPanes();

		} else if( source == reloadFill ) {

			int [] selectedIndices = fillList.getSelectedIndices();
			if( selectedIndices.length != 1 ) {
				IJ.error("You must have a single fill selected in order to reload.");
				return;
			}
			pathAndFillManager.reloadFill(selectedIndices[0]);

		} else if( source == setMaxThreshold ) {

			plugin.setFillThreshold( maxThresholdValue );

		} else if( source == setThreshold ) {

			try {
				double t = Double.parseDouble( thresholdField.getText() );
				if( t < 0 ) {
					IJ.error("The fill threshold cannot be negative.");
					return;
				}
				plugin.setFillThreshold( t );
			} catch( NumberFormatException nfe ) {
				IJ.error("The threshold '" + thresholdField.getText() + "' wasn't a valid number.");
				return;
			}

		} else if( source == discardFill ) {

			plugin.discardFill();

		} else if( source == saveFill ) {

			plugin.saveFill();

		} else if( source == pauseOrRestartFilling ) {

			plugin.pauseOrRestartFilling();

		} else if( source == view3D ) {

			plugin.viewFillIn3D( ! maskNotReal.isSelected() );

		} else {
			IJ.error("BUG: FillWindow received an event from an unknown source");
		}

	}

	public void itemStateChanged(ItemEvent ie) {
		if( ie.getSource() == transparent )
			plugin.setFillTransparent( transparent.isSelected() );
	}

	public void thresholdChanged( double f ) {
		thresholdField.setText(""+f);
	}

	public void maximumDistanceCompletelyExplored( SearchThread source, float f ) {
		maxThreshold.setText("("+f+")");
		maxThresholdValue = f;
	}

	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// Do nothing...
	}

	public void finished( SearchThread source, boolean success ) {
		// Do nothing...
	}

	public void threadStatus( SearchThread source, int currentStatus ) {
		switch(currentStatus) {
		case FillerThread.STOPPING:
			pauseOrRestartFilling.setLabel("Stopped");
			pauseOrRestartFilling.setEnabled(false);
			saveFill.setEnabled(false);

			break;
		case FillerThread.PAUSED:
			pauseOrRestartFilling.setLabel("Continue");
			saveFill.setEnabled(true);
			break;
		case FillerThread.RUNNING:
			pauseOrRestartFilling.setLabel("Pause");
			saveFill.setEnabled(false);
			break;
		}
		fillControlPanel.doLayout();
	}


}
