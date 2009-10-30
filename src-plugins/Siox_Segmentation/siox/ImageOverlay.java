/**
 * Siox_Segmentation plug-in for ImageJ and Fiji.
 * Copyright (C) 2009 Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package siox;

import ij.process.ImageProcessor;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class ImageOverlay implements Overlay{

	ImageProcessor imp = null;
	Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	
	public ImageOverlay(){}
	
	public ImageOverlay(ImageProcessor imp){
		this.imp = imp;
	}
	
	@Override
	public void paint(Graphics g, int x, int y, double magnification) {
		if ( null == this.imp )
			return;
		
		Graphics2D g2d = (Graphics2D)g;		
		
		final AffineTransform orig = g2d.getTransform();
		final AffineTransform at = new AffineTransform();
		at.scale( magnification, magnification );
		at.translate( - x, - y );
		at.concatenate( orig );
		
		g2d.setTransform( at );
		
				
		g2d.setComposite(composite);
		g2d.drawImage(imp.getBufferedImage(), null, null);	
		
		g2d.setTransform( orig );
	}
	
	public void setComposite (Composite composite)
	{this.composite = composite;}
	
	public void setImage ( ImageProcessor imp)
	{this.imp = imp;}

}
