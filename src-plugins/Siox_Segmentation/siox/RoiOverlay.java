/**
 * Siox_Segmentation plug-in for ImageJ and Fiji.
 * 2009 Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld 
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

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.ShapeRoiHelper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * This class implements an overlay based on the image ROI.
 * The overlay paints the ROI with a specific color and composite mode.
 *  
 * @author Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld
 *
 */
public class RoiOverlay implements Overlay {
	Roi roi = null;
	Color color = Roi.getColor();
	Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	
	public RoiOverlay(){}
	
	public RoiOverlay(Roi roi, Composite composite, Color color)
	{
		setRoi( roi );
		setComposite( composite );
		setColor( color );
	}
	
	@Override
	public void paint(Graphics g, int x, int y, double magnification) {
		if ( null == this.roi )
			return;
		// Set ROI image to null to avoid repainting
		roi.setImage(null);
		Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(roi));
		final Rectangle roiBox = roi.getBounds();
		final Graphics2D g2d = (Graphics2D)g;
		final AffineTransform originalTransform = g2d.getTransform();
		final AffineTransform at = new AffineTransform();
		at.scale( magnification, magnification );
		at.translate( roiBox.x - x, roiBox.y - y );
		at.concatenate( originalTransform );
		
		g2d.setTransform( at );
		final Composite originalComposite = g2d.getComposite();
		g2d.setComposite( this.composite );
		g2d.setColor( this.color );
				
		g2d.fill(shape);
		
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
	}
	
	public void setRoi(Roi roi)
	{
		this.roi = roi;
	}
	
	public void setComposite (Composite composite)
	{this.composite = composite;}
	
	public void setColor(Color color)
	{this.color = color;}
	
	public String toString() {
		return "RoiOverlay(" + roi + ")";
	}
	
}
