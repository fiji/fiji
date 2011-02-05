package mpicbg.panorama;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.*;

import mpicbg.ij.InverseTransformMapping;
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

public class Panorama_View implements PlugIn, KeyListener, MouseWheelListener, MouseListener, MouseMotionListener
{
	static private enum NaviMode {
		PAN_TILT,
		PAN_ONLY,
		TILT_ONLY,
		ROLL_ONLY
	}
	
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
			canvas.addKeyListener( Panorama_View.this );
			window.addKeyListener( Panorama_View.this );
			
			canvas.addMouseMotionListener( Panorama_View.this );
			
			canvas.addMouseListener( Panorama_View.this );
			
			ij.addKeyListener( Panorama_View.this );
			
			window.addMouseWheelListener( Panorama_View.this );
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
			
			canvas.removeKeyListener( Panorama_View.this );
			window.removeKeyListener( Panorama_View.this );
			ij.removeKeyListener( Panorama_View.this );
			canvas.removeMouseListener( Panorama_View.this );
			canvas.removeMouseMotionListener( Panorama_View.this );
			window.removeMouseWheelListener( Panorama_View.this );
		}
	}
	
	abstract private class Mapper
	{
		protected boolean interpolate = true;
		abstract public void map( final ImageProcessor target );
		final public void toggleInterpolation()
		{
			interpolate = !interpolate;
		}
	}
	
	final private class CubeFaceMapper extends Mapper
	{
		final private ImageProcessor frontSource;
		final private ImageProcessor backSource;
		final private ImageProcessor leftSource;
		final private ImageProcessor rightSource;
		final private ImageProcessor topSource;
		final private ImageProcessor bottomSource;
		final private HomogeneousMapping< RectlinearCamera > mapping = new HomogeneousMapping< RectlinearCamera >( new RectlinearCamera() );
		final private PanoramaCamera< ? > camera;
		final private RectlinearCamera front;
		
		public CubeFaceMapper(
				final ImageProcessor frontSource,
				final ImageProcessor backSource,
				final ImageProcessor leftSource,
				final ImageProcessor rightSource,
				final ImageProcessor topSource,
				final ImageProcessor bottomSource,
				final PanoramaCamera< ? > camera )
		{
			this.frontSource = frontSource;
			this.backSource = backSource;
			this.leftSource = leftSource;
			this.rightSource = rightSource;
			this.topSource = topSource;
			this.bottomSource = bottomSource;
			this.camera = camera;
			this.front = new RectlinearCamera();
			front.setCamera( camera );
			
			/* cubefaces have a width and height of +1px each for off-range interpolation */ 
			front.setSourceWidth( frontSource.getWidth() - 1 );
			front.setSourceHeight( frontSource.getHeight() - 1 );
		}
		
		final public void map( final ImageProcessor target )
		{
			front.setTargetWidth( target.getWidth() );
			front.setTargetHeight( target.getHeight() );
			
			front.setCamera( camera );
			final RectlinearCamera face = front.clone();
			face.resetOrientation();
			target.reset();
			if ( interpolate ) /* TODO This is stupid---the mapping should have the interpolation method as a state */
			{
				mapping.getTransform().set( front );
				mapping.mapInterpolated( frontSource, target );
				
				face.pan( ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.mapInterpolated( rightSource, target );
				
				face.resetOrientation();
				face.pan( ( float )Math.PI );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.mapInterpolated( backSource, target );
				
				face.resetOrientation();
				face.pan( 3 * ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.mapInterpolated( leftSource, target );
				
				face.resetOrientation();
				face.tilt( ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.mapInterpolated( topSource, target );
				
				face.resetOrientation();
				face.tilt( -( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.mapInterpolated( bottomSource, target );
			}
			else
			{
				mapping.getTransform().set( front );
				mapping.map( frontSource, target );
				
				face.pan( ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.map( rightSource, target );
				
				face.resetOrientation();
				face.pan( ( float )Math.PI );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.map( backSource, target );
				
				face.resetOrientation();
				face.pan( 3 * ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.map( leftSource, target );
				
				face.resetOrientation();
				face.tilt( ( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.map( topSource, target );
				
				face.resetOrientation();
				face.tilt( -( float )Math.PI / 2 );
				face.preConcatenateOrientation( front );
				mapping.getTransform().set( face );
				mapping.map( bottomSource, target );
			}
		}
	}
	
	final private class MappingThread extends Thread
	{
		final protected ImagePlus impSource;
		final protected ImagePlus impTarget;
		final protected Mapper mapper;
		final protected ImageProcessor target;
		final protected ImageProcessor temp;
		final protected PanoramaCamera< ? > camera;
		
		private boolean visualize = true;
		private boolean pleaseRepaint;
		private boolean keepPainting;
		private float dt = 1;
		
		public MappingThread(
				final ImagePlus impSource,
				final ImagePlus impTarget,
				final Mapper mapper,
				final ImageProcessor target,
				final PanoramaCamera< ? > camera )
		{
			this.impSource = impSource;
			this.impTarget = impTarget;
			
			this.mapper = mapper;
			
			this.target = target;
			this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
			temp.snapshot();
			this.camera = camera;
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
					
					lambda += dt * dLambda;
					phi += dt * dPhi;
					
					this.camera.setOrientation( lambda, phi, rho );
					
					mapper.map( temp );
					
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
			mapper.toggleInterpolation();
		}
		
		final public void toggleVisualization()
		{
			visualize = !visualize;
		}
	}
	
	private ImagePlus imp;
	private ImageProcessor ip;
	private ImageProcessor ipSource;
	private ImageProcessor frontSource;
	private ImageProcessor backSource;
	private ImageProcessor leftSource;
	private ImageProcessor rightSource;
	private ImageProcessor topSource;
	private ImageProcessor bottomSource;
	private GUI gui;
	
	static private int width = 400;
	static private int height = 300;
	static private float minLambda = 0;
	static private float minPhi = 0;
	static private float hfov = 2 * ( float )Math.PI;
	static private float vfov = ( float )Math.PI;
	static private boolean showCubefaces = false;
	
	final private EquirectangularProjection p = new EquirectangularProjection();
	
	final static private float step = ( float )Math.PI / 180;
	
	private float lambda;
	private float phi;
	private float rho;
	private float dLambda = 0;
	private float dPhi = 0;
	
	/* coordinates where mouse dragging started and the drag distance */
	private int oX, oY, dX, dY;
	private float oRho;
	
	private NaviMode naviMode = NaviMode.PAN_TILT;
	
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
		
		gd.addMessage( "Miscellaneous" );
		gd.addCheckbox( "show cube-faces", showCubefaces );
		
//		gd.addHelp( "http://pacific.mpi-cbg.de/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
		
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
		
		showCubefaces = gd.getNextBoolean();
		
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
		p.setLambdaPiScale( ( (float) Math.PI ) / hfov * ( imp.getWidth() ) );
		p.setPhiPiScale( ( (float) Math.PI ) / vfov * ( imp.getHeight() - 1 ) );
		p.setTargetWidth( ip.getWidth() );
		p.setTargetHeight( ip.getHeight() );
		p.setF( 0.5f );
		
		System.out.println( p.getLambdaPiScale() + " " + p.getPhiPiScale() );
		
		/* TODO calculate proper size */
		
		//final int cubeSize = 500;
		final int cubeSize = Util.round( Math.max( p.getPhiPiScale(), p.getLambdaPiScale() ) * 2 / ( (float) Math.PI ) );
		
		frontSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		backSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		leftSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		rightSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		topSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		bottomSource = ip.createProcessor( cubeSize + 1, cubeSize + 1 );
		
		renderCubeFaces( hfov, vfov );
		
		/* instantiate and run mapper and painter */
		final Mapper mapper = new CubeFaceMapper(
				frontSource,
				backSource,
				leftSource,
				rightSource,
				topSource,
				bottomSource,
				p );
		painter = new MappingThread(
				imp,
				impViewer,
				mapper,
				ip,
				p );
		
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
	
	
	final private void renderCubeFaces( final float hfov, final float vfov )
	{
		/* fragile, but that's not public API and we know what we're doing... */
		final float cubeSize = frontSource.getWidth() - 1;
		
		/* prepare extended image */
		ipSource = ip.createProcessor(
				hfov == ( float )( 2 * Math.PI ) ? imp.getWidth() + 1 : imp.getWidth(),
				vfov == ( float )Math.PI ? imp.getHeight() + 1 : imp.getHeight() );
		prepareExtendedImage( imp.getProcessor(), ipSource );
		
		/* render cube faces */
		final EquirectangularProjection q = p.clone();
		q.resetOrientation();
		q.setTargetWidth( cubeSize );
		q.setTargetHeight( cubeSize );
		q.setF( 0.5f );
		
		final InverseTransformMapping< EquirectangularProjection > qMapping = new InverseTransformMapping< EquirectangularProjection >( q );
		
		IJ.showStatus( "Rendering cube faces..." );
		IJ.showProgress( 0, 6 );
		qMapping.mapInterpolated( ipSource, frontSource );
		IJ.showProgress( 1, 6 );
		q.pan( ( (float) Math.PI ) );
		qMapping.mapInterpolated( ipSource, backSource );
		IJ.showProgress( 2, 6 );
		q.resetOrientation();
		q.pan( ( (float) Math.PI ) / 2 );
		qMapping.mapInterpolated( ipSource, leftSource );
		IJ.showProgress( 3, 6 );
		q.resetOrientation();
		q.pan( -( (float) Math.PI ) / 2 );
		qMapping.mapInterpolated( ipSource, rightSource );
		IJ.showProgress( 4, 6 );
		q.resetOrientation();
		q.tilt( -( (float) Math.PI ) / 2 );
		qMapping.mapInterpolated( ipSource, topSource );
		IJ.showProgress( 5, 6 );
		q.resetOrientation();
		q.tilt( ( (float) Math.PI ) / 2 );
		qMapping.mapInterpolated( ipSource, bottomSource );
		IJ.showProgress( 6, 6 );
		
		if ( showCubefaces )
		{
			new ImagePlus( "front", frontSource ).show();
			new ImagePlus( "back", backSource ).show();
			new ImagePlus( "left", leftSource ).show();
			new ImagePlus( "right", rightSource ).show();
			new ImagePlus( "top", topSource ).show();
			new ImagePlus( "bottom", bottomSource ).show();
		}
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
				lambda -= v * step;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
			{
				lambda += v * step;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				phi -= v * step;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				phi += v * step;
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
				renderCubeFaces( hfov, vfov );
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
			else if ( e.getKeyCode() == KeyEvent.VK_A )
			{
				naviMode = NaviMode.PAN_TILT;
				dLambda = dPhi = 0;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_P )
			{
				naviMode = NaviMode.PAN_ONLY;
				dLambda = dPhi = 0;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_T )
			{
				naviMode = NaviMode.TILT_ONLY;
				dLambda = dPhi = 0;
				update( false );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_R )
			{
				naviMode = NaviMode.ROLL_ONLY;
				dLambda = dPhi = 0;
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
						"SHIFT - Move 10x faster." + NL +
						"CTRL - Move browse 10x slower." + NL +
						"ENTER/ESC - Leave interactive mode." + NL +
						"I - Toggle interpolation." + NL +
						"V - Toggle FOV visualization." + NL +
						"R - Roll-mode (roll via mouse drag)." + NL +
						"P - Pan/Tilt-mode (pan/tilt via mouse drag)." );
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
		dX = oX - e.getX();
		dY = oY - e.getY();
		if ( naviMode == NaviMode.ROLL_ONLY )
		{
			final float d = ( float )Math.sqrt( dX * dX + dY * dY );
			rho = oRho + ( float )Math.atan2( dY / d, dX / d );
		}
		else
		{
			final float v = 0.1f * step * keyModfiedSpeed( e.getModifiersEx() );
			dLambda = v * dX;
			dPhi = -v * dY;
		}
		update( true );
	}

	public void mouseMoved( MouseEvent e ){}
	public void mouseClicked( MouseEvent e ){}
	public void mouseEntered( MouseEvent e ){}
	public void mouseExited( MouseEvent e ){}
	public void mouseReleased( MouseEvent e )
	{
		dLambda = dPhi = 0;
		oRho = rho;
		update( false );
	}
	public void mousePressed( MouseEvent e )
	{
		if ( naviMode == NaviMode.ROLL_ONLY )
		{
			oX = width / 2;
			oY = height / 2;
			dX = oX - e.getX();
			dY = oY - e.getY();
			final float d = ( float )Math.sqrt( dX * dX + dY * dY );
			oRho -= ( float )Math.atan2( dY / d, dX / d ); 
		}
		else
		{
			oX = e.getX();
			oY = e.getY();
		}
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
