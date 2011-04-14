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
import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.util.HashSet;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("serial")
public class FillWindow extends JFrame implements PathAndFillListener, ActionListener, ItemListener, FillerProgressCallback {

	protected SimpleNeuriteTracer plugin;
	protected PathAndFillManager pathAndFillManager;

	public FillWindow(PathAndFillManager pathAndFillManager, SimpleNeuriteTracer plugin) {
		this( pathAndFillManager, plugin, 200, 60 );
	}

	protected JScrollPane scrollPane;

	protected JList fillList;
	protected DefaultListModel listModel;

	protected JButton deleteFills;
	protected JButton reloadFill;

	protected JPanel fillControlPanel;

	protected JLabel fillStatus;

	protected float maxThresholdValue = 0;

	protected JTextField thresholdField;
	protected JLabel maxThreshold;
	protected JButton setThreshold;
	protected JButton setMaxThreshold;

	protected JButton view3D;
	protected JCheckBox maskNotReal;
	protected JCheckBox transparent;

	protected boolean currentlyFilling = true;
	protected JButton pauseOrRestartFilling;

	protected JButton saveFill;
	protected JButton discardFill;

	protected JButton exportAsCSV;

	public void setEnabledWhileFilling( ) {
		assert SwingUtilities.isEventDispatchThread();
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
		assert SwingUtilities.isEventDispatchThread();
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
		assert SwingUtilities.isEventDispatchThread();
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
		assert SwingUtilities.isEventDispatchThread();

		new ClarifyingKeyListener().addKeyAndContainerListenerRecursively(this);

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
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		add( scrollPane, c );

		c.weightx = 0;
		c.weighty = 0;

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
			cf.anchor = GridBagConstraints.LINE_START;
			cf.fill = GridBagConstraints.HORIZONTAL;
			fillStatus = new JLabel("(Not filling at the moment.)");
			fillingOptionsPanel.add(fillStatus,cf);

			thresholdField = new JTextField("",20);
			thresholdField.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 1;
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

			c.gridx = 0;
			++ c.gridy;
			c.insets = new Insets( 8, 8, 8, 8 );
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.LINE_START;
			add(fillingOptionsPanel,c);


			{
				fillControlPanel = new JPanel();
				fillControlPanel.setLayout(new FlowLayout());

				pauseOrRestartFilling = new JButton("Pause");
				currentlyFilling = true;
				pauseOrRestartFilling.addActionListener(this);
				fillControlPanel.add(pauseOrRestartFilling);

				saveFill = new JButton("Save Fill");
				saveFill.addActionListener(this);
				fillControlPanel.add(saveFill);

				discardFill = new JButton("Cancel Fill");
				discardFill.addActionListener(this);
				fillControlPanel.add(discardFill);

				c.gridx = 0;
				++ c.gridy;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.anchor = GridBagConstraints.CENTER;

				add(fillControlPanel,c);
			}

			++ c.gridy;
			c.fill = GridBagConstraints.NONE;
			exportAsCSV = new JButton("Export as CSV");
			exportAsCSV.addActionListener(this);
			add(exportAsCSV,c);
		}
	}

	@Override
	public void setPathList( String [] pathList, Path justAdded, boolean expandAll ) { }

	@Override
	public void setFillList( final String [] newList ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listModel.removeAllElements();
				for( int i = 0; i < newList.length; ++i )
					listModel.addElement( newList[i] );
			}
		});
	}

	@Override
	public void setSelectedPaths( HashSet<Path> selectedPathSet, Object source ) {
		// This dialog doesn't deal with paths, so ignore this.
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		assert SwingUtilities.isEventDispatchThread();

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

		} else if( source == setThreshold || source == thresholdField ) {

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

		} else if( source == exportAsCSV ) {

			SaveDialog sd = new SaveDialog("Export fill summary as...",
						       "fills",
						       ".csv");

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

			IJ.showStatus("Exporting CSV data to "+saveFile.getAbsolutePath());

			try {
				pathAndFillManager.exportFillsAsCSV( saveFile );

			} catch( IOException ioe) {
				IJ.error("Saving to "+saveFile.getAbsolutePath()+" failed");
				return;
			}

		} else {
			IJ.error("BUG: FillWindow received an event from an unknown source");
		}

	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		assert SwingUtilities.isEventDispatchThread();
		if( ie.getSource() == transparent )
			plugin.setFillTransparent( transparent.isSelected() );
	}

	public void thresholdChanged( final double f ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				assert SwingUtilities.isEventDispatchThread();
				thresholdField.setText(""+f);
			}
		});
	}

	@Override
	public void maximumDistanceCompletelyExplored( SearchThread source, final float f ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				maxThreshold.setText("("+f+")");
				maxThresholdValue = f;
			}
		});
	}

	@Override
	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// Do nothing...
	}

	@Override
	public void finished( SearchThread source, boolean success ) {
		// Do nothing...
	}

	@Override
	public void threadStatus( SearchThread source, final int currentStatus ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				switch(currentStatus) {
				case FillerThread.STOPPING:
					pauseOrRestartFilling.setText("Stopped");
					pauseOrRestartFilling.setEnabled(false);
					saveFill.setEnabled(false);

					break;
				case FillerThread.PAUSED:
					pauseOrRestartFilling.setText("Continue");
					saveFill.setEnabled(true);
					break;
				case FillerThread.RUNNING:
					pauseOrRestartFilling.setText("Pause");
					saveFill.setEnabled(false);
					break;
				}
				fillControlPanel.doLayout();
			}
		});
	}


}
