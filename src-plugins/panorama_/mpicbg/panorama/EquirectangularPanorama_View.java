package mpicbg.panorama;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.*;

import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.util.Util;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;

public class EquirectangularPanorama_View implements PlugIn, KeyListener, MouseWheelListener, MouseListener, MouseMotionListener
{	
	final static private String NL = System.getProperty( "line.separator" );
	
	final private class GUI
	{
		final private ImageWindow window;
		final private Canvas canvas;
		
		final private ImageJ ij;
		
		/* backup */
		private KeyListener[] windowKeyListeners;
		private KeyListener[] canvasKeyListeners;
		private KeyListener[] ijKeyListeners;
		
		private MouseListener[] canvasMouseListeners;
		private MouseMotionListener[] canvasMouseMotionListeners;
		
		private MouseWheelListener[] windowMouseWheelListeners;
		
		GUI( final ImagePlus imp )
		{
			window = imp.getWindow();
			canvas = imp.getCanvas();
			
			ij = IJ.getInstance();
		}
		
		/**
		 * Close the window
		 */
		final public void close()
		{
			window.close();
		}
		
		/**
		 * Add new event handlers.
		 */
		final void takeOverGui()
		{
			canvas.addKeyListener( EquirectangularPanorama_View.this );
			window.addKeyListener( EquirectangularPanorama_View.this );
			
			canvas.addMouseMotionListener( EquirectangularPanorama_View.this );
			
			canvas.addMouseListener( EquirectangularPanorama_View.this );
			
			ij.addKeyListener( EquirectangularPanorama_View.this );
			
			window.addMouseWheelListener( EquirectangularPanorama_View.this );
		}
		
		/**
		 * Backup old event handlers for restore.
		 */
		final void backupGui()
		{
			canvasKeyListeners = canvas.getKeyListeners();
			windowKeyListeners = window.getKeyListeners();
			ijKeyListeners = IJ.getInstance().getKeyListeners();
			canvasMouseListeners = canvas.getMouseListeners();
			canvasMouseMotionListeners = canvas.getMouseMotionListeners();
			windowMouseWheelListeners = window.getMouseWheelListeners();
			clearGui();	
		}
		
		/**
		 * Restore the previously active Event handlers.
		 */
		final void restoreGui()
		{
			clearGui();
			for ( final KeyListener l : canvasKeyListeners )
				canvas.addKeyListener( l );
			for ( final KeyListener l : windowKeyListeners )
				window.addKeyListener( l );
			for ( final KeyListener l : ijKeyListeners )
				ij.addKeyListener( l );
			for ( final MouseListener l : canvasMouseListeners )
				canvas.addMouseListener( l );
			for ( final MouseMotionListener l : canvasMouseMotionListeners )
				canvas.addMouseMotionListener( l );
			for ( final MouseWheelListener l : windowMouseWheelListeners )
				window.addMouseWheelListener( l );
		}
		
		/**
		 * Remove both ours and the backed up event handlers.
		 */
		final void clearGui()
		{
			for ( final KeyListener l : canvasKeyListeners )
				canvas.removeKeyListener( l );
			for ( final KeyListener l : windowKeyListeners )
				window.removeKeyListener( l );
			for ( final KeyListener l : ijKeyListeners )
				ij.removeKeyListener( l );
			for ( final MouseListener l : canvasMouseListeners )
				canvas.removeMouseListener( l );
			for ( final MouseMotionListener l : canvasMouseMotionListeners )
				canvas.removeMouseMotionListener( l );
			for ( final MouseWheelListener l : windowMouseWheelListeners )
				window.removeMouseWheelListener( l );
			
			canvas.removeKeyListener( EquirectangularPanorama_View.this );
			window.removeKeyListener( EquirectangularPanorama_View.this );
			ij.removeKeyListener( EquirectangularPanorama_View.this );
			canvas.removeMouseListener( EquirectangularPanorama_View.this );
			canvas.removeMouseMotionListener( EquirectangularPanorama_View.this );
			window.removeMouseWheelListener( EquirectangularPanorama_View.this );
		}
	}
	
	final private class MappingThread extends Thread
	{
		final protected ImagePlus impSource;
		final protected ImagePlus impTarget;
		final protected ImageProcessor source;
		final protected ImageProcessor target;
		final protected ImageProcessor temp;
		final protected Mapping< EquirectangularProjection > mapping;
		final protected EquirectangularProjection p;
		private boolean interpolate = true;
		private boolean visualize = true;
		private boolean pleaseRepaint;
		private boolean keepPainting;
		private float dt = 1;
		
		public MappingThread(
				final ImagePlus impSource,
				final ImagePlus impTarget,
				final ImageProcessor source,
				final ImageProcessor target,
				final Mapping< EquirectangularProjection > mapping,
				final EquirectangularProjection p )
		{
			this.impSource = impSource;
			this.impTarget = impTarget;
			this.source = source;
			this.target = target;
			this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
			temp.snapshot();
			this.mapping = mapping;
			this.p = p;
			this.setName( "MappingThread" );
		}
		
		@Override
		final public void run()
		{
			while ( !isInterrupted() )
			{
				final boolean b;
				synchronized ( this )
				{
					b = pleaseRepaint;
					pleaseRepaint = keepPainting;
				}
				if ( b )
				{
					final long t = System.currentTimeMillis();
				        final float PI2 = ( float )( Math.PI * Math.PI );
					lambda = Util.mod( lambda + dt * dLambda, PI2 );
					phi = Util.mod( phi + dt * dPhi, PI2 );
					
					p.pan( lambda );
					p.tilt( phi );
					
					mapping.getTransform().set( p );
					temp.reset();
					if ( interpolate )
						mapping.mapInterpolated( source, temp );
					else
						mapping.map( source, temp );
					
					final Object targetPixels = target.getPixels();
					target.setPixels( temp.getPixels() );
					temp.setPixels( targetPixels );
					impTarget.updateAndDraw();
					
					if ( visualize )
						visualize( impSource, temp.getWidth(), temp.getHeight(), p );
					
					dt = ( System.currentTimeMillis() - t ) / 1000f;
				}
				synchronized ( this )
				{
					try
					{
						if ( !pleaseRepaint ) wait();
					}
					catch ( InterruptedException e ){}
				}
			}
		}
		
		final public void repaint( final boolean keepPainting )
		{
			synchronized ( this )
			{
				this.keepPainting = keepPainting;
				pleaseRepaint = true;
				notify();
			}
		}
		
		final public void toggleInterpolation()
		{
			interpolate = !interpolate;
		}
		
		final public void toggleVisualization()
		{
			visualize = !visualize;
		}
	}
	
	private ImagePlus imp;
	private ImageProcessor ip;
	private ImageProcessor ipSource;
	private GUI gui;
	
	static private int width = 400;
	static private int height = 300;
	static private float minLambda = 0;
	static private float minPhi = 0;
	static private float hfov = 2 * ( float )Math.PI;
	static private float vfov = ( float )Math.PI;
	
	
	final private EquirectangularProjection p = new EquirectangularProjection();
	final static private float step = ( float )Math.PI / 180;
	final private Mapping< EquirectangularProjection > mapping = new InverseTransformMapping< EquirectangularProjection >( p.clone() );
	
	private float lambda = 0;
	private float phi = 0;
	private float dLambda = 0;
	private float dPhi = 0;
	
	/* coordinates where mouse dragging started and the drag distance */
	private int oX, oY, dX, dY;
	
	final static private boolean setup( final ImagePlus imp )
	{
		final GenericDialog gd = new GenericDialog( "Panorama Viewer" );
		
		gd.addMessage( "Panorama" );
		gd.addNumericField( "min lambda : ", minLambda / Math.PI * 180, 2 );
		gd.addNumericField( "min phi : ", minPhi / Math.PI * 180, 2 );
		gd.addNumericField( "hfov : ", hfov / Math.PI * 180, 2 );
		gd.addNumericField( "vfov : ", vfov / Math.PI * 180, 2 );	
		
		gd.addMessage( "Viewer Window" );
		gd.addNumericField( "width : ", width, 0 );
		gd.addNumericField( "height : ", height, 0 );
		
//		gd.addHelp( "http://fiji.sc/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		minLambda = ( float )( Util.mod( ( float )gd.getNextNumber(), 360 ) / 180 * Math.PI );
		minPhi = ( float )( Util.mod( ( float )gd.getNextNumber(), 180 ) / 180 * Math.PI );
		hfov = Math.min( ( float )( Math.PI * 2 - minLambda ), ( float )( Util.mod( ( float )gd.getNextNumber(), 360 ) / 180 * Math.PI ) );
		vfov = Math.min( ( float )( Math.PI - minPhi ), ( float )( Util.mod( ( float )gd.getNextNumber(), 180 ) / 180 * Math.PI ) );
		
		if ( hfov == 0 ) hfov = ( float )( 2 * Math.PI );
		if ( vfov == 0 ) vfov = ( float )Math.PI;
		
		System.out.println( minLambda + " " + minPhi + " " + hfov + " " + vfov );
		
		width = ( int )gd.getNextNumber();
		height = ( int )gd.getNextNumber();
		
		return true;
	}

	private MappingThread painter;
	
	public void run( String arg )
    {
		imp = IJ.getImage();
		
		if ( imp == null )
		{
			IJ.error( "No image open." );
			return;
		}
		
		if ( !setup( imp ) )
			return;
		
		run( imp, width, height, minLambda, minPhi, hfov, vfov );
    }
	
	final public void run(
			final ImagePlus imp,
			final int width,
			final int height,
			final float minLambda,
			final float minPhi,
			final float hfov,
			final float vfov )
	{
		ip = imp.getProcessor().createProcessor( width, height );
		final ImagePlus impViewer = new ImagePlus( "Panorama View", ip );
		
		/* initialize projection */
		p.setMinLambda( minLambda );
		p.setMinPhi( minPhi );
		p.setLambdaPiScale( ( float )Math.PI / hfov * ( imp.getWidth() ) );
		p.setPhiPiScale( ( float )Math.PI / vfov * ( imp.getHeight() - 1 ) );
		p.setTargetWidth( ip.getWidth() );
		p.setTargetHeight( ip.getHeight() );
		p.setF( 0.5f );
		
		/* prepare extended image */
		ipSource = ip.createProcessor(
				hfov == ( float )( 2 * Math.PI ) ? imp.getWidth() + 1 : imp.getWidth(),
				vfov == ( float )Math.PI ? imp.getHeight() + 1 : imp.getHeight() );
		prepareExtendedImage( imp.getProcessor(), ipSource );
		
		/* instantiate and run painter */
		painter = new MappingThread( imp, impViewer, ipSource, ip, mapping, p );
		
		impViewer.show();
		
		gui = new GUI( impViewer );
		
		gui.backupGui();
		gui.takeOverGui();
		
		painter.start();
		update( false );
    }
	
	final static private void prepareExtendedImage(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		if ( target.getWidth() > source.getWidth() )
		{
			target.copyBits( source, source.getWidth(), 0, Blitter.COPY );
			if ( target.getHeight() > source.getHeight() )
				target.copyBits( source, source.getWidth(), 0, Blitter.COPY );
		}
		if ( target.getHeight() > source.getHeight() )
			target.copyBits( source, 0, 1, Blitter.COPY );
		target.copyBits( source, 0, 0, Blitter.COPY );
	}
	
	final private void update( final boolean keepPainting )
	{
		painter.repaint( keepPainting );
	}
	
	public void keyPressed( KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			imp.getCanvas().setDisplayList( null );
			gui.restoreGui();
			if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
				gui.close();
		}
		else if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			dLambda *= 10;
			dPhi *= 10;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_CONTROL )
		{
			dLambda /= 10;
			dPhi /= 10;
		}
		else
		{
			final float v = keyModfiedSpeed( e.getModifiersEx() );
			if ( e.getKeyCode() == KeyEvent.VK_LEFT )
			{
				p.pan( -v * step );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
			{
				p.pan( v * step );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				p.tilt( -v * step );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				p.tilt( v * step );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS )
			{
				p.setF( p.getF() * ( 1 + 0.1f * v ) );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_MINUS )
			{
				p.setF( p.getF() / ( 1 + 0.1f * v ) );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_SPACE )
			{
				prepareExtendedImage( imp.getProcessor(), ipSource );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_I )
			{
				painter.toggleInterpolation();
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_V )
			{
				painter.toggleVisualization();
				imp.getCanvas().setDisplayList( null );
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_F1 )
			{
				IJ.showMessage(
						"Interactive Panorama Viewer",
						"Mouse control:" + NL + " " + NL +
						"Pan and tilt the panorama by dragging the image in the canvas and" + NL +
						"zoom in and out using the mouse-wheel." + NL + " " + NL +
						"Key control:" + NL + " " + NL +
						"CURSOR LEFT - Pan left." + NL +
						"CURSOR RIGHT - Pan right." + NL +
						"CURSOR UP - Tilt up." + NL +
						"CURSOR DOWN - Tilt down." + NL +
						"SHIFT - Rotate and browse 10x faster." + NL +
						"CTRL - Rotate and browse 10x slower." + NL +
						"ENTER/ESC - Leave interactive mode." + NL +
						"I - Toggle interpolation." );
			}
		}
	}

	final private float keyModfiedSpeed( final int modifiers )
	{
		if ( ( modifiers & KeyEvent.SHIFT_DOWN_MASK ) != 0 )
			return 10;
		else if ( ( modifiers & KeyEvent.CTRL_DOWN_MASK ) != 0 )
			return 0.1f;
		else
			return 1;
	}

	public void keyReleased( final KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			dLambda /= 10;
			dPhi /= 10;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_CONTROL )
		{
			dLambda *= 10;
			dPhi *= 10;
		}
	}
	public void keyTyped( final KeyEvent e ){}
	
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		final float v = keyModfiedSpeed( e.getModifiersEx() );
		int s = e.getWheelRotation();
		p.setF( p.getF() * ( 1 - 0.05f * s * v ) );
		update( false );
	}

	public void mouseDragged( final MouseEvent e )
	{
		final float v = 0.1f * step * keyModfiedSpeed( e.getModifiersEx() );
		dX = oX - e.getX();
		dY = oY - e.getY();
		dLambda = -v * dX;
		dPhi = v * dY;
		update( true );
	}

	public void mouseMoved( MouseEvent e ){}
	public void mouseClicked( MouseEvent e ){}
	public void mouseEntered( MouseEvent e ){}
	public void mouseExited( MouseEvent e ){}
	public void mouseReleased( MouseEvent e )
	{
		dLambda = dPhi = 0;
		update( false );
	}
	public void mousePressed( MouseEvent e )
	{
		oX = e.getX();
		oY = e.getY();
	}
	
	
	final static private void visualize(
			final ImagePlus imp,
			final int w,
			final int h,
			final EquirectangularProjection p )
	{
		try
		{
			final float maxD = imp.getWidth() / 2;
			final GeneralPath gp = new GeneralPath();
			final float[] l = new float[]{ 0, 0 };
			p.applyInverseInPlace( l );
			float x0 = l[ 0 ];
			gp.moveTo( l[ 0 ], l[ 1 ] );
			for ( int x = 1; x < w; ++x )
			{
				l[ 0 ] = x;
				l[ 1 ] = 0;
				p.applyInverseInPlace( l );
				final float dx = l[ 0 ] - x0;
				if ( dx > maxD )
				{
					gp.lineTo( l[ 0 ] - imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else if ( dx < -maxD )
				{
					gp.lineTo( l[ 0 ] + imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else				 
					gp.lineTo( l[ 0 ], l[ 1 ] );
				x0 = l[ 0 ];
			}
			for ( int y = 1; y < h; ++y )
			{
				l[ 0 ] = w - 1;
				l[ 1 ] = y;
				p.applyInverseInPlace( l );
				final float dx = l[ 0 ] - x0;
				if ( dx > maxD )
				{
					gp.lineTo( l[ 0 ] - imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else if ( dx < -maxD )
				{
					gp.lineTo( l[ 0 ] + imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else				 
					gp.lineTo( l[ 0 ], l[ 1 ] );
				x0 = l[ 0 ];
			}
			for ( int x = w - 2; x >= 0; --x )
			{
				l[ 0 ] = x;
				l[ 1 ] = h - 1;
				p.applyInverseInPlace( l );
				final float dx = l[ 0 ] - x0;
				if ( dx > maxD )
				{
					gp.lineTo( l[ 0 ] - imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else if ( dx < -maxD )
				{
					gp.lineTo( l[ 0 ] + imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else				 
					gp.lineTo( l[ 0 ], l[ 1 ] );
				x0 = l[ 0 ];
			}
			for ( int y = h - 2; y >= 0; --y )
			{
				l[ 0 ] = 0;
				l[ 1 ] = y;
				p.applyInverseInPlace( l );
				final float dx = l[ 0 ] - x0;
				if ( dx > maxD )
				{
					gp.lineTo( l[ 0 ] - imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else if ( dx < -maxD )
				{
					gp.lineTo( l[ 0 ] + imp.getWidth(), l[ 1 ] );
					gp.moveTo( l[ 0 ], l[ 1 ] );
				}
				else				 
					gp.lineTo( l[ 0 ], l[ 1 ] );
				x0 = l[ 0 ];
			}
			imp.getCanvas().setDisplayList( gp, Color.YELLOW, null );
			imp.updateAndDraw();
		}
		catch ( NoninvertibleModelException e ){}
	}
}
