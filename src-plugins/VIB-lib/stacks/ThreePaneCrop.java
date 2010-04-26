/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Three Pane Crop".

    The ImageJ plugin "Three Pane Crop" is free software; you can
    redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Three Pane Crop" is distributed in the hope
    that it will be useful, but WITHOUT ANY WARRANTY; without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
    PURPOSE.  See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.gui.ImageCanvas;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Iterator;

import java.awt.image.ColorModel;

/* A small dialog for confirming the region to crop and reporting
 * numerically what the current crop boundaries are. */

class CropDialog extends Dialog implements ActionListener, WindowListener {

	Button setFromFields;
	Button setFromThreshold;
	TextField threshold;

	Button cropButton;
	Button cancelButton;

	public void windowClosing( WindowEvent e ) {
		owner.cancel();
		dispose();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }

	ThreePaneCrop owner;

	TextField x_min_field;
	TextField y_min_field;
	TextField z_min_field;

	TextField x_max_field;
	TextField y_max_field;
	TextField z_max_field;

	ArrayList<ImagePlus> otherImages = new ArrayList<ImagePlus>();
	ArrayList<Checkbox> otherImagesCheckboxes = new ArrayList<Checkbox>();

	public CropDialog( String title, ThreePaneCrop owner ) {

		super( IJ.getInstance(), title, false );

		x_min_field = new TextField( "", 4 );
		y_min_field = new TextField( "", 4 );
		z_min_field = new TextField( "", 4 );
		x_max_field = new TextField( "", 4 );
		y_max_field = new TextField( "", 4 );
		z_max_field = new TextField( "", 4 );

		/*
		  x_min_field.setEnabled(false);
		  y_min_field.setEnabled(false);
		  z_min_field.setEnabled(false);
		  x_max_field.setEnabled(false);
		  y_max_field.setEnabled(false);
		  z_max_field.setEnabled(false);
		*/

		addWindowListener( this );

		this.owner = owner;

		setLayout( new GridBagLayout() );
		GridBagConstraints co = new GridBagConstraints();

		Panel parametersPanel = new Panel();

		parametersPanel.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 5;
		parametersPanel.add( new Label("Current Crop Boundaries (Maxima)"), c );

		c.gridwidth = 1;
		c.gridy = 1;
		c.gridx = 0; parametersPanel.add( new Label( "x from " ), c );
		c.gridx = 1; parametersPanel.add( x_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + owner.overall_min_x + ")  to "), c );
		c.gridx = 3; parametersPanel.add( x_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + owner.overall_max_x + ")"), c );

		c.gridy = 2;
		c.gridx = 0; parametersPanel.add( new Label( "y from " ), c );
		c.gridx = 1; parametersPanel.add( y_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + owner.overall_min_y + ")  to "), c );
		c.gridx = 3; parametersPanel.add( y_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + owner.overall_max_y + ")"), c );

		c.gridy = 3;
		c.gridx = 0; parametersPanel.add( new Label( "z from " ), c );
		c.gridx = 1; parametersPanel.add( z_min_field, c );
		c.gridx = 2; parametersPanel.add( new Label( " (" + (owner.overall_min_z + 1) + ")  to "), c );
		c.gridx = 3; parametersPanel.add( z_max_field, c );
		c.gridx = 4; parametersPanel.add( new Label( " (" + (owner.overall_max_z + 1) + ")"), c );

		Panel fieldsOptionsPanel = new Panel();

		fieldsOptionsPanel.setLayout( new GridBagLayout() );
		GridBagConstraints cf = new GridBagConstraints();

		setFromFields = new Button("Set from fields above");
		setFromFields.addActionListener( this );
		cf.gridx = 0;
		cf.gridy = 0;
		cf.gridwidth = 2;
		co.anchor = GridBagConstraints.LINE_START;
		fieldsOptionsPanel.add( setFromFields, cf );
		setFromThreshold = new Button( "Set crop above value: " );
		setFromThreshold.addActionListener( this );
		threshold = new TextField("50");
		cf.gridx = 0;
		cf.gridy = 1;
		cf.gridwidth = 1;
		co.anchor = GridBagConstraints.LINE_START;
		fieldsOptionsPanel.add( setFromThreshold, cf );
		cf.gridx = 1;
		cf.gridy = 1;
		cf.gridwidth = 1;
		fieldsOptionsPanel.add( threshold, cf );

		Panel otherImagesPanel = new Panel();
		otherImagesPanel.setLayout( new GridBagLayout() );
		GridBagConstraints cw = new GridBagConstraints();

		int[] wList = WindowManager.getIDList();
		if (wList!=null) {

			cw.gridx = 0;
			cw.gridy = 0;
			cw.anchor = GridBagConstraints.LINE_START;

			otherImagesPanel.add(new Label("Also crop these images:"));
			++ cw.gridy;

			for (int i=0; i<wList.length; i++) {

				ImagePlus imp = WindowManager.getImage(wList[i]);

				if( (imp.getWidth() == (owner.overall_max_x + 1)) &&
				    (imp.getHeight() == (owner.overall_max_y + 1)) &&
				    (imp.getStackSize() == (owner.overall_max_z + 1)) ) {

					if( imp == owner.xy )
						continue;

					otherImages.add(imp);
					Checkbox checkbox = new Checkbox( imp.getTitle() );
					otherImagesPanel.add(checkbox,cw);
					++ cw.gridy;
					otherImagesCheckboxes.add( checkbox );
				}
			}

		}

		Panel buttonPanel = new Panel();
		buttonPanel.setLayout( new FlowLayout() );

		cropButton = new Button("Crop");
		cropButton.addActionListener( this );
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener( this );

		buttonPanel.add( cropButton );
		buttonPanel.add( cancelButton );

		co.gridx = 0;
		add( parametersPanel, co );
		co.gridx = 0;
		co.gridy = 1;
		co.anchor = GridBagConstraints.LINE_START;
		add( fieldsOptionsPanel, co );
		co.gridx = 0;
		co.gridy = 2;
		add( otherImagesPanel, co );
		co.gridx = 0;
		co.gridy = 3;
		co.anchor = GridBagConstraints.CENTER;
		add( buttonPanel, co );
		co.gridx = 0;
		co.gridy = 4;
		add( new Label("(Move mouse with shift to update panes.)"), co );



		pack();
		setVisible( true );
	}

	public void actionPerformed( ActionEvent e ) {

		Object source = e.getSource();

		if( source == cropButton ) {
			ArrayList<ImagePlus> toCrop = new ArrayList<ImagePlus>();
			toCrop.add(owner.xy);
			for( int i = 0; i < otherImagesCheckboxes.size(); ++ i ) {
				Checkbox c = (Checkbox)otherImagesCheckboxes.get(i);
				if( c.getState() )
					toCrop.add((ImagePlus)otherImages.get(i));
			}
			owner.performMultipleCrops(toCrop);
		} else if( source == cancelButton ) {
			owner.cancel();
			dispose();
		} else if( source == setFromFields ) {
			setFromFields();
		} else if( source == setFromThreshold ) {
			setFromThreshold();
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
	}

	public void updateCropBounds( int min_x, int max_x,
				      int min_y, int max_y,
				      int min_z, int max_z ) {

		x_min_field.setText( Integer.toString(min_x) );
		x_max_field.setText( Integer.toString(max_x) );

		y_min_field.setText( Integer.toString(min_y) );
		y_max_field.setText( Integer.toString(max_y) );

		z_min_field.setText( Integer.toString(min_z+1) );
		z_max_field.setText( Integer.toString(max_z+1) );

	}

	public void setFromThreshold() {

		double t;

		try {

			/* Parse all the fields as string - throws
			 * NumberFormatException if there's a
			 * malformed field. */

			String threshold_s = threshold.getText( );
			t = Double.parseDouble( threshold_s );

		} catch( NumberFormatException e ) {
			IJ.error( "The threshold must be a number." );
			return;
		}

		owner.setCropAbove(t);
	}

	public void setFromFields( ) {

		int new_x_min, new_x_max, new_y_min, new_y_max, new_z_min, new_z_max;

		try {

			/* Parse all the fields as string - throws
			 * NumberFormatException if there's a
			 * malformed field. */

			String x_min_string = x_min_field.getText( );
			new_x_min = Integer.parseInt( x_min_string );

			String x_max_string = x_max_field.getText( );
			new_x_max = Integer.parseInt( x_max_string );

			String y_min_string = y_min_field.getText( );
			new_y_min = Integer.parseInt( y_min_string );

			String y_max_string = y_max_field.getText( );
			new_y_max = Integer.parseInt( y_max_string );

			String z_min_string = z_min_field.getText( );
			new_z_min = Integer.parseInt( z_min_string );

			String z_max_string = z_max_field.getText( );
			new_z_max = Integer.parseInt( z_max_string );

		} catch( NumberFormatException e ) {
			IJ.error( "The fields must all be integers." );
			return;
		}

		/* The interface should obey the ImageJ convention
		 * that slices are indexed from 1, but we don't
		 * internally */

		-- new_z_min;
		-- new_z_max;

		/* Just check that the maximum is >= the minimum. */

		if( new_x_max < new_x_min ) {
			IJ.error( "The maximum x must be >= the minimum x." );
			return;
		}

		if( new_y_max < new_y_min ) {
			IJ.error( "The maximum y must be >= the minimum y." );
			return;
		}

		if( new_z_max < new_z_min ) {
			IJ.error( "The maximum z must be >= the minimum z." );
			return;
		}

		/* Now check that each new value is between
		 * the minimum and maxium (inclusive) */

		if( new_x_min < owner.overall_min_x || new_x_min > owner.overall_max_x ) {
			IJ.error( "The minimum x must be between "+
				  owner.overall_min_x+" and "+
				  owner.overall_max_x+" inclusive." );
			return;
		}

		if( new_x_max < owner.overall_min_x || new_x_max > owner.overall_max_x ) {
			IJ.error( "The maximum x must be between "+
				  owner.overall_min_x+" and "+
				  owner.overall_max_x+" inclusive." );
			return;
		}


		if( new_y_min < owner.overall_min_y || new_y_min > owner.overall_max_y ) {
			IJ.error( "The minimum y must be between "+
				  owner.overall_min_y+" and "+
				  owner.overall_max_y+" inclusive." );
			return;
		}

		if( new_y_max < owner.overall_min_y || new_y_max > owner.overall_max_y ) {
			IJ.error( "The maximum y must be between "+
				  owner.overall_min_y+" and "+
				  owner.overall_max_y+" inclusive." );
			return;
		}


		if( new_z_min < owner.overall_min_z || new_z_min > owner.overall_max_z ) {
			IJ.error( "The minimum z must be between "+
				  owner.overall_min_z+" and "+
				  owner.overall_max_z+" inclusive." );
			return;
		}

		if( new_z_max < owner.overall_min_z || new_z_max > owner.overall_max_z ) {
			IJ.error( "The maximum z must be between "+
				  owner.overall_min_z+" and "+
				  owner.overall_max_z+" inclusive." );
			return;
		}

		// If we get to here then the new values look OK, so
		// update the crop boundaries in the canvases...

		owner.setCropCuboid( new_x_min, new_x_max,
				     new_y_min, new_y_max,
				     new_z_min, new_z_max );

		owner.repaintAllPanes();
	}

}

public class ThreePaneCrop extends ThreePanes {
	public static final String PLUGIN_VERSION = "1.2";

	public ThreePanesCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new ThreePaneCropCanvas( imagePlus, this, plane );
	}

	public void setCropAbove( double above ) {

		int min_x_above = Integer.MAX_VALUE;
		int max_x_above = Integer.MIN_VALUE;

		int min_y_above = Integer.MAX_VALUE;
		int max_y_above = Integer.MIN_VALUE;

		int min_z_above = Integer.MAX_VALUE;
		int max_z_above = Integer.MIN_VALUE;

		ImageStack stack = xy.getStack();

		int width = xy.getWidth();
		int height = xy.getHeight();
		int depth = xy.getStackSize();

		int type = xy.getType();

		for( int z = 0; z < depth; z ++ ) {

			switch (type) {

			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
			{
				byte [] slice_bytes = (byte [])stack.getPixels(z+1);

				for( int x = 0; x < width; ++x )
					for( int y = 0; y < height; ++y ) {

						int value = slice_bytes[y*width+x]&0xFF;

						if( value > above ) {

							if( x < min_x_above ) min_x_above = x;
							if( y < min_y_above ) min_y_above = y;
							if( z < min_z_above ) min_z_above = z;

							if( x > max_x_above ) max_x_above = x;
							if( y > max_y_above ) max_y_above = y;
							if( z > max_z_above ) max_z_above = z;

						}

					}
			}
			break;

			case ImagePlus.GRAY16:
			{
				short [] slice_shorts = (short [])stack.getPixels(z+1);

				for( int x = 0; x < width; ++x )
					for( int y = 0; y < height; ++y ) {

						short value = slice_shorts[y*width+x];

						if( value > above ) {

							if( x < min_x_above ) min_x_above = x;
							if( y < min_y_above ) min_y_above = y;
							if( z < min_z_above ) min_z_above = z;

							if( x > max_x_above ) max_x_above = x;
							if( y > max_y_above ) max_y_above = y;
							if( z > max_z_above ) max_z_above = z;

						}

					}
			}

			case ImagePlus.COLOR_RGB:
			{
				int [] slice_ints = (int [])stack.getPixels(z+1);

				for( int x = 0; x < width; ++x )
					for( int y = 0; y < height; ++y ) {

						// FIXME: this isn't very sensible; should probably allow
						// the user to set different thresholds for R, G & B or
						// something...  (COLOR_RGB seems to be ARGB.)

						int raw_value = slice_ints[y*width+x];

						int r = (raw_value & 0xFF0000) >> 16;
						int g = (raw_value & 0xFF00) >> 8;
						int b = raw_value & 0xFF;

						int value = (r + g + b) / 3;

						if( value > above ) {

							if( x < min_x_above ) min_x_above = x;
							if( y < min_y_above ) min_y_above = y;
							if( z < min_z_above ) min_z_above = z;

							if( x > max_x_above ) max_x_above = x;
							if( y > max_y_above ) max_y_above = y;
							if( z > max_z_above ) max_z_above = z;

						}

					}
			}
			break;

			case ImagePlus.GRAY32:
			{
				float [] slice_floats = (float [])stack.getPixels(z+1);

				for( int x = 0; x < width; ++x )
					for( int y = 0; y < height; ++y ) {

						float value = slice_floats[y*width+x];

						if( value > above ) {

							if( x < min_x_above ) min_x_above = x;
							if( y < min_y_above ) min_y_above = y;
							if( z < min_z_above ) min_z_above = z;

							if( x > max_x_above ) max_x_above = x;
							if( y > max_y_above ) max_y_above = y;
							if( z > max_z_above ) max_z_above = z;

						}

					}
			}
			break;

			}


			IJ.showProgress( z / (float)depth );
		}

		IJ.showProgress( 1 );

		if( min_x_above == Integer.MAX_VALUE ) {
			IJ.error("There were no voxels with value greater than "+above );
			return;
		} else {

			setCropCuboid( min_x_above, max_x_above,
				       min_y_above, max_y_above,
				       min_z_above, max_z_above );

		}

		repaintAllPanes();

	}

	public void performMultipleCrops( ArrayList<ImagePlus> images ) {

		for( Iterator i = images.iterator();
		     i.hasNext();
			) {

			ImagePlus imp = (ImagePlus)i.next();

			ImagePlus newImagePlus =
				performCrop(
					imp,
					min_x_offscreen, max_x_offscreen,
					min_y_offscreen, max_y_offscreen,
					min_z_offscreen, max_z_offscreen,
					changeOrigin );

			newImagePlus.show();
		}
	}

	static public ImagePlus performCrop( ImagePlus imp,
					     int min_x, int max_x,
					     int min_y, int max_y,
					     int min_z, int max_z,
					     boolean adjust_origin ) {

		int original_width = imp.getWidth();

		int new_width = (max_x - min_x) + 1;
		int new_height = (max_y - min_y) + 1;

		int first_slice = min_z + 1;
		int last_slice = max_z + 1;

		ImageStack stack=imp.getStack();
		ImageStack new_stack=new ImageStack( new_width, new_height );

		int type = imp.getType();

		ColorModel cm = null;
		if( ImagePlus.COLOR_256 == type ) {
			cm = stack.getColorModel();
		}

		switch (type) {

		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:

			for( int slice = first_slice; slice <= last_slice; slice ++ ) {

				byte [] slice_bytes = (byte [])stack.getPixels(slice);

				byte [] new_slice = new byte[new_width * new_height];
				for( int y = min_y; y <= max_y; ++y ) {
					System.arraycopy( slice_bytes, y * original_width + min_x,
							  new_slice, (y - min_y) * new_width,
							  new_width );
				}

				ByteProcessor bp = new ByteProcessor( new_width, new_height );

				bp.setPixels( new_slice );

				new_stack.addSlice( null, bp );

				IJ.showProgress( (slice - first_slice) / ((last_slice - first_slice) + 1) );
			}
			break;


		case ImagePlus.GRAY16:

			for( int slice = first_slice; slice <= last_slice; slice ++ ) {

				short [] slice_shorts = (short [])stack.getPixels(slice);

				short [] new_slice = new short[new_width * new_height];
				for( int y = min_y; y <= max_y; ++y ) {
					System.arraycopy( slice_shorts, y * original_width + min_x,
							  new_slice, (y - min_y) * new_width,
							  new_width );
				}

				ShortProcessor sp = new ShortProcessor( new_width, new_height );

				sp.setPixels( new_slice );

				new_stack.addSlice( null, sp );

				IJ.showProgress( (slice - first_slice) / ((last_slice - first_slice) + 1) );
			}
			break;

		case ImagePlus.COLOR_RGB:

			for( int slice = first_slice; slice <= last_slice; slice ++ ) {

				int [] slice_ints = (int [])stack.getPixels(slice);

				int [] new_slice = new int[new_width * new_height];
				for( int y = min_y; y <= max_y; ++y ) {
					System.arraycopy( slice_ints, y * original_width + min_x,
							  new_slice, (y - min_y) * new_width,
							  new_width );
				}

				ColorProcessor cp = new ColorProcessor( new_width, new_height );

				cp.setPixels( new_slice );

				new_stack.addSlice( null, cp );

				IJ.showProgress( (slice - first_slice) / ((last_slice - first_slice) + 1) );
			}
			break;

		case ImagePlus.GRAY32:

			for( int slice = first_slice; slice <= last_slice; slice ++ ) {

				float [] slice_floats = (float [])stack.getPixels(slice);

				float [] new_slice = new float[new_width * new_height];
				for( int y = min_y; y <= max_y; ++y ) {
					System.arraycopy( slice_floats, y * original_width + min_x,
							  new_slice, (y - min_y) * new_width,
							  new_width );
				}

				FloatProcessor fp = new FloatProcessor( new_width, new_height );

				fp.setPixels( new_slice );

				new_stack.addSlice( null, fp );

				IJ.showProgress( (slice - first_slice) / ((last_slice - first_slice) + 1) );
			}
			break;

		}

		if( ImagePlus.COLOR_256 == type ) {
			if( cm != null ) {
				new_stack.setColorModel( cm );
			}
		}

		IJ.showProgress( 1 );

		ImagePlus imagePlus = new ImagePlus( "cropped "+imp.getTitle(), new_stack );

		/* Should we adjust the origin according to the crop
		 * rather than just copying the Calibration?  I'm not
		 * doing so by default, since the ImageJ built-in Crop
		 * command doesn't.  However, you can select that
		 * option in the interface.  */

		Calibration oldCalibration = imp.getCalibration();

		if( oldCalibration != null ) {

			Calibration newCalibration = (Calibration)oldCalibration.clone();

			if( adjust_origin ) {
				newCalibration.xOrigin -= min_x;
				newCalibration.yOrigin -= min_y;
				newCalibration.zOrigin -= min_z;
			}

			if( newCalibration != null ) {
				imagePlus.setCalibration(newCalibration);
			}

		}

		if( imp.getProperty("Info") != null)
			imagePlus.setProperty("Info",imp.getProperty("Info"));

		imagePlus.setFileInfo(imp.getOriginalFileInfo());

		return imagePlus;
	}

	public void cancel() {
		closeAndReset();
	}

	int max_x_offscreen, min_x_offscreen;
	int max_y_offscreen, min_y_offscreen;
	int max_z_offscreen, min_z_offscreen;

	protected int overall_min_x, overall_max_x;
	protected int overall_min_y, overall_max_y;
	protected int overall_min_z, overall_max_z;

	public void setCropCuboid( int min_x, int max_x,
				   int min_y, int max_y,
				   int min_z, int max_z ) {

		min_x = Math.max( min_x, overall_min_x );
		min_y = Math.max( min_y, overall_min_y );
		min_z = Math.max( min_z, overall_min_z );

		max_x = Math.min( max_x, overall_max_x );
		max_y = Math.min( max_y, overall_max_y );
		max_z = Math.min( max_z, overall_max_z );

		((ThreePaneCropCanvas)xy_canvas).setCropBounds( min_x, max_x,
								min_y, max_y );

		if( ! single_pane ) {
			((ThreePaneCropCanvas)xz_canvas).setCropBounds( min_x, max_x,
									min_z, max_z );

			((ThreePaneCropCanvas)zy_canvas).setCropBounds( min_z, max_z,
									min_y, max_y );

		}

		min_x_offscreen = min_x;
		max_x_offscreen = max_x;

		min_y_offscreen = min_y;
		max_y_offscreen = max_y;

		min_z_offscreen = min_z;
		max_z_offscreen = max_z;

		dialog.updateCropBounds( min_x_offscreen,
					 max_x_offscreen,
					 min_y_offscreen,
					 max_y_offscreen,
					 min_z_offscreen,
					 max_z_offscreen );
	}

	CropDialog dialog;

	public ThreePaneCrop( ) {

	}

	/* Should we attempt to update the origin in the cropped version?
	   (Almost certainly, but there's an option for it anyway.) */

	boolean changeOrigin;

	public void initialize( ImagePlus imagePlus ) {

		/* We might need up to three times the memory; the two
		   panes and the cropped image. */

		checkMemory( imagePlus, 3 );

		/* Pop up a dialog asking about:
		   - how many panes to use
		   - whether to preserve the origin
		*/

		boolean singleSlice =  imagePlus.getStackSize() == 1;

		{
			GenericDialog gd = new GenericDialog("Three Pane Crop (v" +
							     PLUGIN_VERSION + ")");
			gd.addMessage("Cropping: "+imagePlus.getTitle());
			if( ! singleSlice )
				gd.addCheckbox("Three pane view?", true);
			gd.addCheckbox("Change origin?", true);

			gd.showDialog();
			if (gd.wasCanceled())
				return;

			single_pane = singleSlice || (! gd.getNextBoolean());
			changeOrigin = gd.getNextBoolean();
		}

		super.initialize( imagePlus );

		overall_min_x = 0;
		overall_min_y = 0;
		overall_min_z = 0;

		overall_max_x = imagePlus.getWidth() - 1;
		overall_max_y = imagePlus.getHeight() - 1;
		overall_max_z = imagePlus.getStackSize() - 1;

		dialog = new CropDialog("Crop Options",this);

		setCropCuboid( 0, imagePlus.getWidth() - 1,
			       0, imagePlus.getHeight() - 1,
			       0, imagePlus.getStackSize() - 1 );

	}

	public void handleDraggedTo( int off_screen_x, int off_screen_y, int dragging, int in_plane ) {

		/* There may be one of 12 handles dragged (each corner
		   of the new cube).  FIXME: all the nearly repeated
		   code here is ugly: simplify that... */

		int point[] = new int[3];

		findPointInStack( off_screen_x, off_screen_y, in_plane, point );

		if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_min_y = Math.min( point[1], max_y_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_max_y = Math.max( point[1], min_y_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_min_y = Math.min( point[1], max_y_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_max_y = Math.max( point[1], min_y_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_min_x = Math.min( point[0], max_x_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( new_min_x, max_x_offscreen,
				       min_y_offscreen, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.XZ_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_x = Math.max( point[0], min_x_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, new_max_x,
				       min_y_offscreen, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NW)) ) {

			int new_min_y = Math.min( point[1], max_y_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_NE)) ) {

			int new_min_y = Math.min( point[1], max_y_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       new_min_y, max_y_offscreen,
				       min_z_offscreen, new_max_z );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SW)) ) {

			int new_max_y = Math.max( point[1], min_y_offscreen );
			int new_min_z = Math.min( point[2], max_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       new_min_z, max_z_offscreen );

		} else if( ((in_plane == ThreePanes.ZY_PLANE) && (dragging == ThreePaneCropCanvas.HANDLE_SE)) ) {

			int new_max_y = Math.max( point[1], min_y_offscreen );
			int new_max_z = Math.max( point[2], min_z_offscreen );

			setCropCuboid( min_x_offscreen, max_x_offscreen,
				       min_y_offscreen, new_max_y,
				       min_z_offscreen, new_max_z );

		}

		repaintAllPanes();

	}


}
