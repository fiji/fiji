package jRenderer3D;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;

/**
 * The framework JRenderer3D provides the possibility to implement easily a 3D display for ImageJ plugins.
 * <p>
 * 
 * 
 * <p>
 * 		Tne next section describes the basic scheme for rendering 3D scenes.<br />
 * 		After initializing a new 3D scene, objects are added, drawing parameters and global render parameters have to be set.<br />
 * 		At the end a rendering is performed that is writen to an image, which can be displayed with ImageJ.
 * </p>
 * 
 * <p>
 * 		<i>Example:</i>
 * </p>
 * 
 *	<p>
 *	  	Setup 3D rendering<br />
 *	  	<tt>JRenderer3D jRenderer3D;<br />
 *	  	jRenderer3D = new JRenderer3D(xCenter, yCenter, zCenter); // size of the rendered image</tt>
 *  </p>
 *  
 *  <p>
 *  	Set or add one or more 3D objects like points or spheres:<br />
 *  	<tt>jRenderer3D.addPoint3D(250, 250,  20, 20, Color.WHITE);</tt>
 * </p>
 * 
 * <p>
 * 		Or lines, texts, cubes:<br />
 * 		<tt>jRenderer3D.add3DCube(0, 0, 0, xSize, ySize, zSize, 0xFF000000);</tt>
 * </p>
 * 
 * <p>
 *	  	Set one image for a surface plot<br />
 *	  	<tt>jRenderer3D.setSurfacePlot(imp);</tt>
 * </p>
 * 
 * <p>
 * 		Or set a volume (a 3D-stack) <br />
 * 		<tt>jRenderer3D.setVolume(imagePlus); // set the volume</tt>
 *  </p>
 *  
 * <p>
 * 		Set the drawing modes<br />
 * 		<tt>jRenderer3D.setPoints3DDrawMode(JRenderer3D.POINTS_SPHERES);<br />
 * 		jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_ORANGE); // select a LUT<br />
 * 		jRenderer3D.setSurfacePlotLight(0.5); // set lighting <br />
 * 		jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_MESH); // set plot mode to mesh</tt>
 * </p>
 * 
 * <p>
 * 		Set global parameters<br />
 * 		<tt>jRenderer3D.setBackgroundColor(0xFF000050); // dark blue background<br />
 * 		jRenderer3D.setZAspect(4); 					// set the z-aspect ratio to 4<br />
 * 		jRenderer3D.setTransformScale(1.5); 		// scale factor<br />
 * 		jRenderer3D.setTransformRotationXYZ(80, 0, 160); // rotation angles (in degrees)</tt>
 * </p>
 * 
 * <p>
 * 		Render the image <br />
 * 		<tt>jRenderer3D.doRendering();</tt>
 * </p>
 * 
 * <p>
 * 		Get the rendered new image<br />
 * 		<tt>Image image = jRenderer3D.getImage();</tt>
 * </p>
 * 
 * <p>
 * 		And display it<br />
 * 		<tt>ImagePlus impNew = new ImagePlus(&quot;Rendered Image&quot;, image);<br />
 * 		impNew.show();<br />
 * 		impNew.updateAndDraw();</tt>
 * </p>
 * 
 * <p/>
 * <p>
 *		<i>Possible 3D elements to draw:</i>
 *		<ul>
 *			<li>{@link Point3D}</li>
 *			<li>{@link Line3D}</li>
 * 			<li>{@link Text3D}</li>
 *          <li><strong>Surface: </strong><br>
 *          	<p>Surface plots can be generate from all image types. Selections, which can be non-rectangular, are supported.</p>
 *				<p>The luminance of an image is interpreted as height for the plot. 
 *					Internally the image is scaled to a 256x256 image using nearest neighbor sampling. 
 *					Other dimensions for this image can be set by the setSurfacePlotGridSize method. </p>
 *				<p>The surface plot has several drawing modes which av be set by setSurfacePlotMode:
 *				<ul>
 *				<li>Dots: All pixels are drawn as small dots.</li>
 *				<li>Lines: All pixels are connected in the x-direction.</li>
 *				<li>Mesh: All pixels are connected in the x- and y-direction.</li>
 *				<li>Filled: All pixels are connected without leaving holes </li>
 *				</ul>
 *				</p>
 *				<p>Display Colors can be chosen from the original color, grayscale, different LUTs and orange.</p>
 *				<p>Noisy images can be smoothed using the applySurfaceSmoothingFilter method. 
 *					Setting lighting with the setSurfacePlotLight methode gives the impression that the 
 *					plot was illuminated and so improves the visibility of small differences.
 *				</p>
 *          </li>
 *          <li><strong>Volume</strong>
 *          	<p>Stacks can be shown as volumes or slices.
 *				8, 16 and 32 bit stacks are supported. RGB-stacks are not supported for the moment.</p>
 *				<p>The display mode of a volume may be set with the setVolumeDrawMode method by using these constants:
 *				<ul>
 *					<li>VOLUME_SLICE_NEARESTNEIGHBOR: A slice through the volume. No interpolation is performed. </li>
 *					<li>VOLUME_SLICE_TRILINEAR: A slice through the volume using trilinear interpolation. </li>
 *					<li>VOLUME_DOTS: Voxels are shown according to their distance, no interpolation is performed.</li>
 *					<li>VOLUME_PROJECTION_TRILINEAR_BACK: A 3D volume is rendered. 
 *						The opacity of the voxels is controlled by the setVolumeThreshold method. 
 *						Values below the threshold are interpreted as transparent.</li>
 *				</ul>
 *				<p>The setVolumeCutDistance method sets the distance of the slice. 
 *					For volume rendering only voxels further away than this distance are displayed. 
 *					The z-aspect ratio of a volume is read automatically, but it can also be set 
 *					using the setTransformZAspectRatio. </p>
 *				<p> Different LUTs can be used for display by using the setVolumeLut method.</p>
 *			</li>
 *     </ul>
 * <p>
 * <p>
 * Before rendering a scene a geometrical transform is applied. Rotation angles 
 * for the x, y, and z-coordinate achses may be set by the 
 * setTransformRotation* methods. the global scale is set by the 
 * setTransformScale method. JRederer3D assumes the following coordinate system:</p>
 * <p><img src="img/xyz.gif"/></p>
 * <p>The z-axis is pointing towards the viewer. The orientation of the z-axis may be changed with the method setTransformZOrientation. 
 * 		The entire scene is centered to the center coordinates that are passed with the constructor of JRenderer3D.</p>
 * <p> The 3D scene is rendered to an image buffer. The initial size of this buffer
 *		is 512x512 pixels: This size can be changed with the setBufferSize method. </p>
 *
 * @version 1.0
 * @author Kai Uwe Barthel
 * 
 
 */
public class JRenderer3D {

	private MemoryImageSource source = null;
	//private Image  image;    				// the rendered image
	private BufferedImage bufferedImage  = null;
	private Graphics2D g2D  = null;

	
	private int[] bufferPixels = null;		// render buffer
	private double[] zbufferPixels = null;		// Z-buffer
	private int bufferWidth = 512;			// size of the buffers
	private int bufferHeight = 512;

	
	//	 Global rendering parameters
	private Color backgroundColor = Color.gray;
	
	
	// transform parameters
	private Transform transform  = null;			// the actual transformation
	private double 	tr_rotationX = 1.34;
	private double 	tr_rotationY = 0;
	private double 	tr_rotationZ = 1; 
	private double  tr_perspective = 0;
	private double tr_maxDistance = 256;
		
	private int zOrientation = -1;

	private double 	scale = 1;
	private double 	zAspectRatio = 1;
	
	private double xCenter = 0;	// the x, y, and z - coordinates of the rotation center
	private double yCenter = 0;
	private double zCenter = 0;
		
	
	// objects to be drawn
	private ArrayList lines3D = null;  //TODO : Extra classes  
	private ArrayList cubeLines3D = null;
	private ArrayList text3D = null;
	
	
	// 3D points
	private PointsPlot pointsPlot = null;
	
	/**
	 * Draws a point as a sphere (slowest).
	 */
	public final static int POINT_SPHERE = Point3D.SPHERE;
	
	/**
	 * Draws a point as a (2D) circle.
	 */
	public final static int POINT_CIRCLE = Point3D.CIRCLE;
	
	/**
	 * Draws a point as a dot. Size information has no effect.
	 */
	public final static int POINT_DOT = Point3D.DOT;
	


	////////////////////////////////////////////////////////
	//
	// surfacePlot constants & parameters
	//
	
	private final static int SURFACEGRID_DEFAULTWIDTH = 256;
	private final static int SURFACEGRID_DEFAULTHEIGHT = 256;
	
	/**
	 * Draws a surface plot using only dots, no illumination is used (fastest mode).
	 */
	public static final int SURFACEPLOT_DOTSNOLIGHT = 10; 
	/**
	 * Draws a surface plot using only dots, illumination is active.
	 */
	public static final int SURFACEPLOT_DOTS = 11;
	
	/**
	 * Draws a surface plot using lines. Number of lines can be adjusted.
	 */
	public final static int SURFACEPLOT_LINES = 12;
	
	/**
	 * Draws a surface plot using a mesh. Mesh size can be adapted.
	 */
	public final static int SURFACEPLOT_MESH = 13;
	
	/**
	 * Draws a filled surface plot .
	 */
	public final static int SURFACEPLOT_FILLED = 14;
	/**
	 * Draws a filled surface plot (Slowest mode).
	 */
	public final static int SURFACEPLOT_ISOLINES = 15;
	
	
	
	private SurfacePlot surfacePlot = null;
	
	
	private int 		surfacePlot_gridWidth = SURFACEGRID_DEFAULTWIDTH;
	private int 		surfacePlot_gridHeight= SURFACEGRID_DEFAULTHEIGHT;
	private ImagePlus 	surfacePlot_imagePlusData = null;   // image used for the surface plot 
	private ImagePlus 	surfacePlot_imagePlusTexture = null; // texture image
	private int 		surfacePlot_plotMode = SURFACEPLOT_LINES;
	private int 		surfacePlot_lutNr = LUT_ORIGINAL;
	private double		surfacePlot_light = 0;
	
	
	////////////////////////////////////////////////////////
	//
	//  volume constants & parameters
	//
	
	/**
	 * Draws a volume using only dots (fastest mode).
	 */
	public static final int VOLUME_DOTS = 20;
	
	/**
	 * Draws a volume using nearest neighbor interpolation.
	 */
	public static final int VOLUME_SLICE_NEAREST_NEIGHBOR = 21;
	
	/**
	 * Draws a volume using trilinear interpolation.
	 */
	public static final int VOLUME_SLICE_TRILINEAR = 22;
//	public final static int VOLUME_SLICE_AND_BORDERS = 23;
//	public final static int VOLUME_SLICE_AND_VOLUME_DOTS = 24;
	/**
	 * Draws a volume using trilinear interpolation projection from the front
	 */
		public static final int VOLUME_PROJECTION_TRILINEAR_FRONT = 25;
	
	/**
	 * Draws a volume using trilinear interpolation projection from the back
	 */
//	public static final int VOLUME_PROJECTION_TRILINEAR_BACK = 26;
	
	private Volume volume  = null;
	
	private int volume_drawMode = VOLUME_DOTS;
	private int volume_threshold = 0;
	private int volume_cutDist = 0;  // clips the view sceen
	private int volume_lutNr = LUT_ORIGINAL;  // LUT type 
	private int volume_dotsDeltaX = 1;   // subsampling factor in x direction (used by dots drawing)
	private int volume_dotsDeltaY = 1;   // subsampling factor in x direction (used by dots drawing)
	private int volume_dotsDeltaZ = 1;   // subsampling factor in x direction (used by dots drawing) 
	private int surfacePlot_min = 0;  // minimum value for the luminance transform
	private int surfacePlot_max = 255;  // maximum value for the luminance transform
	private Image image;
	private boolean axes = true;
	private boolean lines = true;
	private boolean text = true;
	private boolean legend = true;
	private double minZ;
	private double maxZ;
	
	
	 	
	////////////////////////////////////////////////////////////
	
	// LUT constants
	/**
	 * 3D represenations of objects are drawn with their original colors.
	 */
	public final static int LUT_ORIGINAL = 50;
	
	/**
	 * 3D representations of objects are drawn with grayscale colors.
	 */
	public final static int LUT_GRAY = 51;
	
	/**
	 * 3D representations of objects are drawn with spectrum colors.
	 */
	public final static int LUT_SPECTRUM = 52;
	
	/**
	 * 3D representations of objects are drawn with fire colors.
	 */
	public final static int LUT_FIRE = 53;
	
	/**
	 * 3D representations of objects are drawn with thermal colors.
	 */
	public final static int LUT_THERMAL = 54;
	
	/**
	 * 3D representations of objects are drawn in orange.
	 */
	public final static int LUT_ORANGE = 55;
	
	/**
	 * 3D representations of objects are drawn in blue.
	 */
	public final static int LUT_BLUE = 56;
	
	/**
	 * 3D representations of objects are drawn in black.
	 */
	public final static int LUT_BLACK = 57;
	
	/**
	 * 3D representations of objects are cored according to their gradient.
	 */
	public static final int LUT_GRADIENT = 58;
	public static final int LUT_GRADIENT2 = 59;
	
	
	/**
	 * Creates a new JRenderer3D object.
	 * 
	 * This has always to be the first step to generate a 3D scene.
	 * 
	 * The center is assumed at (0,0,0)
	 *
	 */
	public JRenderer3D() {
		//initBuffer();
	}
	
	/**
	 * Creates a new JRenderer3D object. <p> 
	 *
	 * This has always to be the first step to generate a 3D scene.
	 *  
	 * @param xCenter The x-coordinate of the rotation / plot center.
	 * @param yCenter The y-coordinate of the rotation / plot center.
	 * @param zCenter The z-coordinate of the rotation / plot center.
	 */
	public JRenderer3D(double xCenter, double yCenter, double zCenter) {
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.zCenter = zCenter;
		
		//initBuffer();
	}
	
	
	
	
   /*************************************************************
	*            P R I V A T E   M E T H O D S                  *
	*************************************************************/
	
	
	private void initBuffer(){
		//IJ.log("BufferWidth " + bufferWidth + " BufferHeight " + bufferHeight);
		bufferPixels = new int[bufferWidth*bufferHeight];
		zbufferPixels = new double[bufferWidth*bufferHeight];
		
		if (transform != null) { // read previous rotation angles
			tr_rotationX = transform.getRotationX();
			tr_rotationY = transform.getRotationY();
			tr_rotationZ = transform.getRotationZ();
			scale = transform.getScale();
			zAspectRatio = transform.getZAspectRatio();
			tr_perspective = transform.getPerspective();
			tr_maxDistance = transform.getMaxDistance();
		}
		transform = new Transform(bufferWidth, bufferHeight);	
		
		// restore values from previous transformation 
		transform.setZOrientation(zOrientation);
		transform.setRotationXYZ(tr_rotationX, tr_rotationY, tr_rotationZ);
		transform.setScale(scale);
		transform.setZAspectRatio(zAspectRatio);
		transform.setPerspective(tr_perspective);
		transform.setMaxDistance(tr_maxDistance);
		
		source = new MemoryImageSource(bufferWidth, bufferHeight, bufferPixels, 0, bufferWidth);
		
		// if the surfacePlot exists, then update the references to the buffers
		if (surfacePlot != null) {
			surfacePlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			surfacePlot.setTransform(transform);
		}	
		if (volume != null) {
			volume.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			volume.setTransform(transform);
		}	
	}
	
	private void lines(){
		
		Point3D p0 = new Point3D();
		Point3D p1 = new Point3D();
		
		for (int i = 0; i < lines3D.size(); i++) {
			
			if (lines3D.get(i) != null && lines3D.get(i) instanceof Line3D) {
				Line3D line = (Line3D)lines3D.get(i);
				int color = line.color;
				
				setPoints(line, p0, p1);
				
				transform.transform(p0);
				double x0 = transform.X, y0 = transform.Y, z0 = transform.Z+2;
				
				transform.transform(p1);
				double x1 = transform.X, y1 = transform.Y, z1 = transform.Z+2;
				
				if (line.isPair) {
					i++;
					Line3D line2 = (Line3D)lines3D.get(i);
					int color2 = line2.color;
					
					setPoints(line2, p0, p1);
					
					transform.transform(p0);
					double x0_2 = transform.X, y0_2 = transform.Y, z0_2 = transform.Z+2;
					
					transform.transform(p1);
					double x1_2 = transform.X, y1_2 = transform.Y, z1_2 = transform.Z+2;
					
					if (z0_2+z1_2 > z0+z1) {
						x0 = x0_2;
						y0 = y0_2;
						z0 = z0_2;
						x1 = x1_2;
						y1 = y1_2;
						z1 = z1_2;
						color = color2;
					}
					
				}
				
				double dx1 = x1-x0, dy1 = y1-y0, dz1 = z1-z0;
				
				int numSteps = (int) Math.max(Math.abs(dx1),Math.abs(dy1));
				double step = (numSteps > 0) ? 1 / (double)numSteps : 1;
				
				for (int s = 0; s < numSteps; s++) {
					double f = s * step;
					
					int x = (int) (x0 + f*dx1);
					int y = (int) (y0 + f*dy1);
					
					if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
						int pos = y*bufferWidth + x;  
						double z = z0 + f*dz1;
						
						int v_ = (int) ((z * 20) + 128);
						v_ = Math.min(Math.max(0, v_), 255);
						v_ = 0xFF000000 |(v_ << 8);
						
						if (z <= zbufferPixels[pos]) {
							zbufferPixels[pos] = z;
							bufferPixels[pos] = color; // v_; 
						}
					}
				}
			}
		}
	}
	
	private void cubeLines(){
		
		Point3D p0 = new Point3D();
		Point3D p1 = new Point3D();
		for (int i = 0; i < cubeLines3D.size(); i++) {
			
			if (cubeLines3D.get(i) != null && cubeLines3D.get(i) instanceof Line3D) {
				Line3D line = (Line3D)cubeLines3D.get(i);
				int color = line.color;
				
				setPoints(line, p0, p1);
				
				transform.transform(p0);
				double x0 = transform.X, y0 = transform.Y, z0 = transform.Z;
				
				transform.transform(p1);
				double x1 = transform.X, y1 = transform.Y, z1 = transform.Z;
				
				double dx1 = x1-x0, dy1 = y1-y0, dz1 = z1-z0;
				
				int numSteps = (int) Math.max(Math.abs(dx1),Math.abs(dy1));
				double step = (numSteps > 0) ? 1 / (double)numSteps : 1;
				
				for (int s = 0; s < numSteps; s++) {
					double f = s * step;
					
					int x = (int) (x0 + f*dx1);
					int y = (int) (y0 + f*dy1);
					
					if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
						int pos = y*bufferWidth + x;  
						double z = z0 + f*dz1;
						
						if (z <= zbufferPixels[pos]) {
							zbufferPixels[pos] = z;
							bufferPixels[pos] = color; 
						}
					}
				}
			}
		}
	}
	
	private void setPoints(Line3D l0, Point3D p0, Point3D p1) {
		p0.x = l0.x1;
		p0.y = l0.y1;
		p0.z = l0.z1;
		
		p1.x = l0.x2;
		p1.y = l0.y2;
		p1.z = l0.z2;
	}


	
	private void finishAndDrawText() {
		
		// get image from pixels
		image = Toolkit.getDefaultToolkit().createImage(source);
		
		// use bufferedImage to draw background amd text
		if (bufferedImage == null || 
				bufferedImage.getHeight() != bufferHeight ||
				bufferedImage.getWidth() != bufferWidth ) {
			bufferedImage =  new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);

			g2D = bufferedImage.createGraphics();
		}
		g2D.setColor(backgroundColor);
		g2D.fillRect(0,0,bufferWidth,bufferHeight);
		
		Font font;
		
		if (text) {
			if(text3D != null){
				double scale = transform.getScale();

				for (int i = 0; i < text3D.size(); i++) {

					if (text3D.get(i) != null && text3D.get(i) instanceof Text3D) {
						Text3D ti = (Text3D)text3D.get(i);

						transform.transform(ti);
						double x = transform.X;
						double y = transform.Y;
						double z = transform.Z;
						double x2=0;
						double y2=0;
						double z2=0;

						if (ti.number == 2){
							i++;
							Text3D ti2 = (Text3D)text3D.get(i);

							transform.transform(ti2);
							x2 = transform.X;
							y2 = transform.Y;
							z2 = transform.Z;
							
							if (z2 < z) {
								x = x2;
								y = y2;
								z = z2;
							}
						}
						if (ti.number == 4){
							i++;
							Text3D ti2 = (Text3D)text3D.get(i);

							transform.transform(ti2);
							x2 = transform.X;
							y2 = transform.Y;
							z2 = transform.Z;
							i++;
							ti = (Text3D)text3D.get(i);
							transform.transform(ti);
							double x3 = transform.X;
							double y3 = transform.Y;
							double z3 = transform.Z;
							
							i++;
							ti = (Text3D)text3D.get(i);
							transform.transform(ti);
							double x4 = transform.X;
							double y4 = transform.Y;
							double z4 = transform.Z;
							
							if (x2 < x) {
								x = x2;
								y = y2;
								z = z3;
							}
							if (x3 < x) {
								x = x3;
								y = y3;
								z = z3;
							}
							if (x4 < x) {
								x = x4;
								y = y4;
								z = z4;
							}
						}
						
						if (z >= 0) 
						{
							g2D.setColor(ti.color);
							int strHeight = (int)(scale*ti.size);
							
							font = new Font("Sans", Font.BOLD, strHeight);
							g2D.setFont(font);
							FontMetrics metrics = g2D.getFontMetrics();
							int strWidth = metrics.stringWidth(ti.text);
							
							g2D.drawString(ti.text, (int)x-strWidth/2, (int)y+strHeight/2);
//							System.out.println("--> " + ti.text);
						}
					}
				}
			}
		}
		
		g2D.drawImage(image, 0, 0, null);
		
		if (text) {
			if(text3D != null){
				double scale = transform.getScale();

				for (int i = 0; i < text3D.size(); i++) {

					if (text3D.get(i) != null && text3D.get(i) instanceof Text3D) {
						Text3D ti = (Text3D)text3D.get(i);

						transform.transform(ti);
						double x = transform.X;
						double y = transform.Y;
						double z = transform.Z;
						double x2=0;
						double y2=0;
						double z2=0;

						if (ti.number == 2){
							i++;
							Text3D ti2 = (Text3D)text3D.get(i);

							transform.transform(ti2);
							x2 = transform.X;
							y2 = transform.Y;
							z2 = transform.Z;
							
							if (z2 < z) {
								x = x2;
								y = y2;
								z = z2;
							}
						}
						if (ti.number == 4){
							i++;
							Text3D ti2 = (Text3D)text3D.get(i);

							transform.transform(ti2);
							x2 = transform.X;
							y2 = transform.Y;
							z2 = transform.Z;
							
							i++;
							ti = (Text3D)text3D.get(i);
							transform.transform(ti);
							double x3 = transform.X;
							double y3 = transform.Y;
							double z3 = transform.Z;
							
							i++;
							ti = (Text3D)text3D.get(i);
							transform.transform(ti);
							double x4 = transform.X;
							double y4 = transform.Y;
							double z4 = transform.Z;
							
							if (x2 < x) {
								x = x2;
								y = y2;
								z = z3;
							}
							if (x3 < x) {
								x = x3;
								y = y3;
								z = z3;
							}
							if (x4 < x) {
								x = x4;
								y = y4;
								z = z4;
							}
						}
						
						//System.out.println("2 x:" + x + " y: " + y + " z: " + z + " " + ti.text);
						if (z < 0)  {
							g2D.setColor(ti.color);
							int strHeight = (int)(scale*ti.size);
							
							font = new Font("Sans", Font.BOLD, strHeight);
							g2D.setFont(font);
							FontMetrics metrics = g2D.getFontMetrics();
							int strWidth = metrics.stringWidth(ti.text);
							
							g2D.drawString(ti.text, (int)x-strWidth/2, (int)y+strHeight/2);
						}
					}
				}
			}
		}
		
		
		// show lut
		if (legend && surfacePlot != null) {
			int lutNr = surfacePlot.getSurfacePlotLut();	

			if (lutNr >= LUT_GRAY && lutNr <= LUT_THERMAL) {
				int hLut = 256;
				int wLut = 20;
				int xs = bufferWidth - 30;
				int xe = xs + wLut;
				int ys = (bufferHeight-hLut)/2;
				boolean isInverse = (surfacePlot.getInversefactor() == -1);
				g2D.setColor(new Color (255, 255, 255));
				g2D.drawRect(xs-1, ys-1, wLut+2, hLut+1);

				for (int j = 0; j < 256; j++) {
					g2D.setColor(new Color(surfacePlot.lut.colors[255-j]));
					g2D.drawLine(xs, ys+j, xe, ys+j);
				}	

				double d = maxZ-minZ;
				double stepValue = calcStepSize(d, 11);

				double minStart = Math.floor(minZ/stepValue)*stepValue;
				double delta = minStart - minZ;

				g2D.setColor(new Color (255, 255, 255));
				font = new Font("Sans", Font.PLAIN, 11);
				g2D.setFont(font);
				FontMetrics metrics = g2D.getFontMetrics();
				int stringHeight = 5;

				for (double value=0; value+delta<=d; value += stepValue) {
					String s;
					if (Math.floor(minStart+value) - (minStart+value) == 0)
						s = "" + (int)(minStart+value);
					else
						s = "" + (int)Math.round((minStart+value)*1000)/1000.;
					double pos = ((value+delta) * 256 / d);
					int y;
					if (pos >= 0) {
						y = (int) (-pos + 255 + ys );
						if (isInverse)
							y = (int) (pos + ys);

						int strWidth = metrics.stringWidth(s);			
						g2D.drawString(s, xs - 5 - strWidth, y + stringHeight);
						g2D.drawLine(xs-3, y, xs-1, y);				
					}
				}		
			}
		}		
	
		
		font = new Font("Sans", Font.PLAIN, 13);
		g2D.setFont(font);
		
		g2D.setColor(new Color (20, 25, 100));
		g2D.drawString("ImageJ3D", bufferWidth - 66, bufferHeight - 6); 
		g2D.setColor(new Color (255, 255, 255));
		g2D.drawString("ImageJ3D", bufferWidth - 68, bufferHeight - 8); 
		
//		String str = "x: " + (int)Math.toDegrees(tr_rotationX) + " y: " + (int)Math.toDegrees(tr_rotationY) + " z: " + (int)Math.toDegrees(tr_rotationZ);
//		g2D.drawString(str, 10, 20); 
//		String str1 = "x: " + (int)Math.toDegrees(transform.angleX) + " y: " + (int)Math.toDegrees(transform.angleY) + " z: " + (int)Math.toDegrees(transform.angleZ);
//		g2D.drawString(str1, 10, 40); 		
	}
	
	double calcStepSize( double range, double targetSteps )
	{
	    // Calculate an initial guess at step size
	    double tempStep = range / targetSteps;

	    // Get the magnitude of the step size
	    double mag = Math.floor( Math.log(tempStep)/Math.log(10.) );
	    double magPow = Math.pow( (double) 10.0, mag );

	    // Calculate most significant digit of the new step size
	    double magMsd =  ( (int) (tempStep / magPow + .5) );

	    // promote the MSD to either 1, 2, 4, or 5
	    if ( magMsd > 6 ) // 5
	        magMsd = 10.0;
	    else if ( magMsd > 3 )
	        magMsd = 5.0;
	    else if ( magMsd > 2 )
	        magMsd = 4.0;
	    else if ( magMsd > 1 )
	        magMsd = 2.0;

	    return magMsd * magPow;
	}
	
	private void clearBuffers() {
		// clear image and z-buffer
		
//		java.util.Arrays.fill(bufferPixels, 0);
//		//java.util.Arrays.fill(bufferPixels, backgroundColor.getRGB());
//		java.util.Arrays.fill(zbufferPixels, 1000000);
		
		for (int i = bufferPixels.length-1; i >= 0; i--)  {
			bufferPixels[i] = 0; 
			zbufferPixels[i] = 1000000;
		}
	}
	
	private Line3D setLinePoints(Line3D lineItem, double[] p1, double[] p2, int color) {
		lineItem.x1 = p1[0];
		lineItem.y1 = p1[1];
		lineItem.z1 = p1[2];
		lineItem.x2 = p2[0];
		lineItem.y2 = p2[1];
		lineItem.z2 = p2[2];
		
		lineItem.color = color;
		
		return lineItem;
	}
	
	private void addCubeLinesList(Line3D[] lines3D) {
		if (cubeLines3D == null) cubeLines3D = new ArrayList();
		for (int i=0; i<lines3D.length; i++){
			this.cubeLines3D.add(lines3D[i]);
		}
	}
	
	private void clearCubeLines3D() {
		if (cubeLines3D != null)
			this.cubeLines3D.clear();
	}

   /*************************************************************
	*            P U B L I C   M E T H O D S                    *
	*************************************************************/
	
	
	/**
	 * This methods does the rendering and creates the 3D output. 
	 * It has to be redone after all setting changes (scale, angle, surface modes, lightning modes, color modes etc.)
	 * Draws all given input data (lines, text, points, cubes, surfaces).
	 */
	public void doRendering() {
		clearBuffers();
		
		if (volume != null) {
			transform.setOffsets(xCenter, yCenter, zCenter);
			volume.draw();
			transform.setOffsets(0, 0, 0);
		}
		if (surfacePlot != null) {
			surfacePlot.draw();
		}
		if (pointsPlot != null) {
			pointsPlot.draw();
		}
		if (lines && lines3D != null) {
			lines();
		}
		if (axes && cubeLines3D != null) {
			cubeLines();
		}
		finishAndDrawText();
	}
	
   /*************************************************************
	*                    S E T T E R                            *
	*************************************************************/
	
	
	/**
	 * Adds an array with {@link Point3D} to the 3D scene. 
	 * 
	 * @param points3D array with points in a threedimensional coordinate system
	 */
	public void addPoints3D(Point3D[] points3D){
		if (pointsPlot == null){
			pointsPlot = new PointsPlot();
			pointsPlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			pointsPlot.setTransform(transform);
		}
		
		for (int i = 0; i < points3D.length; i++) {
			if (points3D[i] != null) {
				points3D[i].x -= xCenter;
				points3D[i].y -= yCenter;
				points3D[i].z -= zCenter;
				pointsPlot.addPoint3D(points3D[i]);
			}
		}	
	}
	
	/**
	 * Adds a single {@link Point3D} to the 3D scene.
	 * 
	 * @param point3D the 3D point to add
	 */
	public void addPoint3D(Point3D point3D){
		if (pointsPlot == null){
			pointsPlot = new PointsPlot();
			pointsPlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			pointsPlot.setTransform(transform);
		}
		point3D.x -= xCenter;
		point3D.y -= yCenter;
		point3D.z -= zCenter;
		pointsPlot.addPoint3D(point3D);
	}
	
	
	/**
	 * Adds a point to the 3D scene.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param size
	 * @param color
	 * @param drawMode
	 */
	public void addPoint3D(int x, int y, int z, int size, Color color, int drawMode){
		if (pointsPlot == null){
			pointsPlot = new PointsPlot();
			pointsPlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			pointsPlot.setTransform(transform);
		}
		Point3D point = new Point3D(x-xCenter, y-yCenter, z-zCenter, size, color, drawMode);
		pointsPlot.addPoint3D(point);
	}
	
	/**
	 * Adds a point to the 3D scene.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param size
	 * @param rgb
	 * @param drawMode
	 */
	public void addPoint3D(int x, int y, int z, int size, int rgb, int drawMode){
		if (pointsPlot == null){
			pointsPlot = new PointsPlot();
			pointsPlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
			pointsPlot.setTransform(transform);
		}
		Point3D point = new Point3D(x-xCenter, y-yCenter, z-zCenter, size, rgb, drawMode);
		pointsPlot.addPoint3D(point);
	}
	
	
	/**
	 * Creates the surface plot. 
	 * 
	 * @param imp The images to be drawn in 3D.
	 */
	public void setSurfacePlot(ImagePlus imp){
		surfacePlot_imagePlusData = imp;	
		surfacePlot = new SurfacePlot();
		surfacePlot.setSurfacePlotCenter(xCenter, yCenter, zCenter);
		surfacePlot.setSurfaceGridSize(surfacePlot_gridWidth, surfacePlot_gridHeight);
		surfacePlot.setSurfacePlotImage(surfacePlot_imagePlusData);
		if (surfacePlot_imagePlusTexture != null)
			surfacePlot.setSurfacePlotTextureImage(surfacePlot_imagePlusTexture);
		
		surfacePlot.resample();
		
		
		surfacePlot.setSurfacePlotMode(surfacePlot_plotMode);
		surfacePlot.setSurfacePlotLut(surfacePlot_lutNr);
		surfacePlot.setSurfacePLotSetLight(surfacePlot_light);

		surfacePlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
		surfacePlot.setTransform(transform);
	}
	
	public void setSurfacePlotTexture(ImagePlus impTexture) {
		surfacePlot_imagePlusTexture = impTexture;
		if (surfacePlot != null) {
			surfacePlot.setSurfacePlotTextureImage(impTexture);
			surfacePlot.resample();
		}
	}

	/**
	 * Creates the surface plot with texture. 
	 * 
	 * @param imp The images to be drawn in 3D.
	 * @param impTexture The image which shall be used as texture
	 */
	public void setSurfacePlotWithTexture(ImagePlus imp, ImagePlus impTexture) {
		setSurfacePlot(imp, impTexture);
	}
	
	private void setSurfacePlot(ImagePlus imp, ImagePlus impTexture){
		surfacePlot_imagePlusData = imp;	
		surfacePlot_imagePlusTexture = impTexture;	
		
		surfacePlot = new SurfacePlot();
		surfacePlot.setSurfacePlotCenter(xCenter, yCenter, zCenter);
		
		surfacePlot.setSurfaceGridSize(surfacePlot_gridWidth, surfacePlot_gridHeight);
		if (surfacePlot_imagePlusData != null) {
			surfacePlot.setSurfacePlotImage(imp);
		}
		if (surfacePlot_imagePlusTexture != null) {
			surfacePlot.setSurfacePlotTextureImage(surfacePlot_imagePlusTexture);
		}
		surfacePlot.setSurfacePlotMode(surfacePlot_plotMode);
		surfacePlot.setSurfacePlotLut(surfacePlot_lutNr);
		surfacePlot.setSurfacePLotSetLight(surfacePlot_light);
		surfacePlot.setMinMax(surfacePlot_min, surfacePlot_max);
	
		surfacePlot.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
		surfacePlot.setTransform(transform);
	}
	
	
	/**
	 * Sets the surface plot mode.
	 * <p>
	 * Available options: 
	 * <ul>
	 * <li> <strong>PLOT_DOTSNOLIGHT</strong>: if used, no lightning options are applied (fastest)</li>
	 * <li> <strong>PLOT_DOTS</strong>: draws dots</li>
	 * <li> <strong>PLOT_LINES</strong>: draws lines</li>
	 * <li> <strong>PLOT_MESH</strong>: draws a mesh</li>
	 * <li> <strong>PLOT_FILLED</strong>: fills the 3d object completely (slowest)</li>
	 * </ul>
	 * 
	 * @param surfacePlot_plotMode the surface plot mode
	 */
	public void setSurfacePlotMode(int surfacePlot_plotMode) {
		this.surfacePlot_plotMode = surfacePlot_plotMode;
		if (surfacePlot != null)
			surfacePlot.setSurfacePlotMode(surfacePlot_plotMode);
	}
	
	
	/**
	 * Sets the mode for drawing volumes.
	 * <p>
	 * <ul>
	 * <li> <strong>VOLUME_DOTS</strong>: draws volumes with dots(fastest)</li>
	 * </ul>
	 * 
	 * @param volume_drawMode
	 */
	public void setVolumeDrawMode(int volume_drawMode) {
		this.volume_drawMode = volume_drawMode;	
		if (volume != null) {
			volume.setVolumeDrawMode(volume_drawMode);
		}
	}
	
	public void setVolumeLut(int volume_lutNr) {
		this.volume_lutNr = volume_lutNr;	
		if (volume != null) {
			volume.setVolumeLut(volume_lutNr);
		}
	}
	/**
	 * Set subsampling factors (used by the drawing mode volume dots). 
	 * This allows faster drawing in the VOLUME_DOTS mode. 
	 * (default values are 1)
	 * 
	 * @param volume_dotsDeltaX  	subsampling factor in x direction
	 * @param volume_dotsDeltaY
	 * @param volume_dotsDeltaZ
	 */
	public void setVolumeDotsSubsampling(int volume_dotsDeltaX, int volume_dotsDeltaY, int volume_dotsDeltaZ) {
		this.volume_dotsDeltaX = volume_dotsDeltaX;
		this.volume_dotsDeltaY = volume_dotsDeltaY;
		this.volume_dotsDeltaZ = volume_dotsDeltaZ;
		
		if (volume != null) {
			volume.setVolumeDotsSubsampling(volume_dotsDeltaX, volume_dotsDeltaY, volume_dotsDeltaZ);
		}
	}
	
	/**
	 * Sets the view distance.
	 * Just the voxels behind the distance are drawn.
	 * 
	 * @param volume_cutDist the cut distance
	 */
	public void setVolumeCutDistance(int volume_cutDist) {
		this.volume_cutDist = volume_cutDist*transform.getZOrientation();	
		if (volume != null) {
			volume.setVolumeCutDist(this.volume_cutDist);
		}
	}

	
	/**
	 * Sets the surface grid size (sampling rate).
	 * Should be set to an integer fraction of the image of which the surface plot has to be generated.
	 * 
	 * @param width the width of surface grid
	 * @param height the height of surface grid
	 */
	public void setSurfacePlotGridSize(int width, int height) {
		this.surfacePlot_gridWidth = width;
		this.surfacePlot_gridHeight = height;
		if (surfacePlot != null) {
			surfacePlot.setSurfaceGridSize(surfacePlot_gridWidth, surfacePlot_gridHeight);
			surfacePlot.resample();
		}
		
//		// if the surface grid size changes, we have to set up the surfaceplot again
//		if (surfacePlot != null) {
//			setSurfacePlot(surfacePlot_imagePlus, surfacePlot_imagePlusTexture);
//		}
	}	
	
//	general transform parameters
	
	/**
	 * Chooses the coordinate system.
	 * 
	 * @param zOrientation 
	 * 	 <br> -1: left hand coordinate system
	 *   <br>  1: right hand coordinate system 

	 */
	public void setTransformZOrientation(int zOrientation) {
		transform.setZOrientation(zOrientation);
	}
	
	/**
	 * Sets scale of the redered 3D scene.
	 * 
	 * @param scale scales the scene
	 */
	public void setTransformScale(double scale) {
		transform.setScale(scale);
	}
	
	/**
	 * Sets the rotation on the x-axis.
	 * 
	 * @param ax angle in degree
	 */
	public void setTransformRotationX(double ax) {
		transform.setRotationX(Math.toRadians(ax));
	}
	
	/**
	 * Sets the rotation on the y-axis.
	 * 
	 * @param ay angle in degree
	 */
	public void setTransformRotationY(double ay) {
		transform.setRotationY(Math.toRadians(ay));
	}

	/**
	 * Sets the rotation on the z-axis.
	 * 
	 * @param az angle in degree
	 */
	public void setTransformRotationZ(double az) {
		transform.setRotationZ(Math.toRadians(az));
	}
	
	/**
	 * Sets the rotation on both  x- and z-axis.
	 * 
	 * @param ax angle of x-axis in degree
	 * @param az angle of z-axis in degree
	 */
	public void setTransformRotationXZ(double ax, double az) {
		transform.setRotationXZ(Math.toRadians(ax), Math.toRadians(az));
	}
	
	/**
	 * Sets the rotation on  x-, y- and z-axis.
	 * 
	 * @param ax rotation angle of x-axis in degree
	 * @param ay rotation angle of y-axis in degree
	 * @param az rotation angle of z-axis in degree
	 */
	public void setTransformRotationXYZ(double ax, double ay, double az) {
		transform.setRotationXYZ(Math.toRadians(ax), Math.toRadians(ay), Math.toRadians(az));
	}
	
	/**
	 * Changes the rotation of x- and z-axis by the values of changeX and changeY. 
	 * 
	 * @param changeX change of rotation angle of x-axis in degree
	 * @param changeZ change of rotation angle of z-axis in degree
	 */
	public void changeTransformRotationXZ(double changeX, double changeZ){
		transform.changeRotationXZ(Math.toRadians(changeX), Math.toRadians(changeZ));
	}
	
	/**
	 * Changes the rotation of x-, y-, and z-axis by the values of changeX and changeY. 
	 * 
	 * @param changeX change of rotation angle of x-axis in degree
	 * @param changeY change of rotation angle of y-axis in degree
	 * @param changeZ change of rotation angle of z-axis in degree
	 */
	public void applyTransformRotationXYZ(double changeX, double changeY, double changeZ){
		transform.rotateTransformation(Math.toRadians(changeX), Math.toRadians(changeY), Math.toRadians(changeZ));
	}
	
	/**
	 * Changes the rotation of x-, y-, and z-axis by the values of changeX and changeY. 
	 * 
	 * @param changeX change of rotation angle of x-axis in degree
	 * @param changeY change of rotation angle of y-axis in degree
	 * @param changeZ change of rotation angle of z-axis in degree
	 */
	public void changeTransformRotationXYZ(double changeX, double changeY, double changeZ){
		transform.changeRotationXYZ(Math.toRadians(changeX), Math.toRadians(changeY), Math.toRadians(changeZ));
	}
	
	/**
	 * Sets the background color of the rendered 3D presenation
	 * 
	 * @param rgb the background color (argb int)
	 */
	public void setBackgroundColor(int rgb) {
		this.backgroundColor = new Color(rgb, true);
	}
	
	/**
	 * Sets the background color of the rendered 3D presentation
	 * 
	 * @param color Color
	 */
	public void setBackgroundColor(Color color) {
		this.backgroundColor = color;
	}
	
	
	/**
	 * Adds an array of lines to the 3D scene.
	 * 
	 * @param lines3D array of 3d lines
	 */
	public void addLines3D(Line3D[] lines3D) {
		if (this.lines3D == null) this.lines3D = new ArrayList();
		
//		this.lines3D.addAll(Arrays.asList(lines3D));
		
		for (int i = 0; i < lines3D.length; i++) {
			if (lines3D[i] != null) {
				lines3D[i].x1 -= xCenter;
				lines3D[i].y1 -= yCenter;
				lines3D[i].z1 -= zCenter;
				lines3D[i].x2 -= xCenter;
				lines3D[i].y2 -= yCenter;
				lines3D[i].z2 -= zCenter;
				this.lines3D.add(lines3D[i]);
			}
		}
	}
	
	/**
	 * Adds a single 3D line to the 3D scene.
	 * 
	 * @param line3D the 3D line to add
	 */
	public void addLine3D(Line3D line3D){
		if (this.lines3D == null) this.lines3D = new ArrayList();
		
		line3D.x1 -= xCenter;
		line3D.y1 -= yCenter;
		line3D.z1 -= zCenter;
		line3D.x2 -= xCenter;
		line3D.y2 -= yCenter;
		line3D.z2 -= zCenter;
		
		this.lines3D.add(line3D);
	}
	
	/**
	 * Adds a single 3D line to the 3D scene.
	 * @param xStart
	 * @param yStart
	 * @param zStart
	 * @param xEnd
	 * @param yEnd
	 * @param zEnd
	 * @param color
	 */
	public void addLine3D(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, Color color){
		Line3D line3D = new Line3D(xStart, yStart, zStart, xEnd, yEnd, zEnd, color);
		line3D.x1 -= xCenter;
		line3D.y1 -= yCenter;
		line3D.z1 -= zCenter;
		line3D.x2 -= xCenter;
		line3D.y2 -= yCenter;
		line3D.z2 -= zCenter;
		
		if (this.lines3D == null) this.lines3D = new ArrayList();
		this.lines3D.add(line3D);
	}
	
	public void clearLines() {
		if (this.lines3D != null) 
			this.lines3D.clear();
	}
	
	/**
	 * Adds an array of text to the 3D scene.
	 * 
	 * @param text3D
	 */
	public void addText3D(Text3D[] text3D) {
		if (this.text3D == null) this.text3D = new ArrayList();
		
		for (int i = 0; i < text3D.length; i++) {
			if (text3D[i] != null) {
				text3D[i].x -= xCenter;
				text3D[i].y -= yCenter;
				text3D[i].z -= zCenter;
				this.text3D.add(text3D[i]);
			}
		}
	}
	
	/**
	 * Adds a text to the 3D scene.
	 * @param text
	 * @param x		the x position of the text  
	 * @param y		the y position
	 * @param z		the z position
	 * @param color	the text color
	 * @param size	the font size 
	 */
	public void addText3D(String text, int x, int y, int z, Color color, int size) {
		if (this.text3D == null) this.text3D = new ArrayList();
		Text3D t3D = new Text3D(text, x-xCenter, y-yCenter, z-zCenter, color, size);
		this.text3D.add(t3D);
	}
	
	/**
	 * Adds a single 3D text to the 3D scene.
	 * 
	 * @param text3D the 3D text to add
	 */
	
	/**
	 * Adds a text to the 3D scene.
	 * @param text
	 * @param x		the x position of the text  
	 * @param y		the y position
	 * @param z		the z position
	 * @param color	the text color
	 * @param size	the font size 
	 */
	
	public void addText3D(String text, int x, int y, int z, int rgb, int size) {
		if (this.text3D == null) this.text3D = new ArrayList();
		Text3D t3D = new Text3D(text, x-xCenter, y-yCenter, z-zCenter, new Color(rgb), size);
		this.text3D.add(t3D);
	}
	
	/**
	 * Adds a single 3D text to the 3D scene.
	 * 
	 * @param text3D the 3D text to add
	 */
	public void addText3D(Text3D text3D){
		if (this.text3D == null) this.text3D = new ArrayList();
		
		text3D.x -= xCenter;
		text3D.y -= yCenter;
		text3D.z -= zCenter;
		
		this.text3D.add(text3D);
	}
	
	public void addTextPair3D(Text3D text1_3D, Text3D text2_3D){
		if (this.text3D == null) this.text3D = new ArrayList();
		
		text1_3D.x -= xCenter;
		text1_3D.y -= yCenter;
		text1_3D.z -= zCenter;
		
		text2_3D.x -= xCenter;
		text2_3D.y -= yCenter;
		text2_3D.z -= zCenter;
		
		this.text3D.add(text1_3D);
		this.text3D.add(text2_3D);
	}
	
	public void clearText() {
		if (this.text3D != null)
			this.text3D.clear();
	}
	
	
	/**
	 * Sets the buffer size. (The size of the rendered 2D image) 
	 * 
	 * @param width The width to be set.
	 * @param height The height to be set.
	 */
	public void setBufferSize(int width, int height){
		this.bufferWidth = width;
		this.bufferHeight = height;
		initBuffer();
	}
	
	/**
	 * Adds a cube (drawn with 12 lines) to the 3D scene.
	 * 
	 * @param xMin minimum value x
	 * @param yMin minimum value y
	 * @param zMin minimum value z
	 * @param xMax maximum value x
	 * @param yMax maximum value y
	 * @param zMax maximum value z
	 * @param color Color of cube lines (argb)
	 */
	public void add3DCube(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Color color) {
		int colorInt = color.getRGB();
		add3DCube(xMin, yMin, zMin, xMax, yMax, zMax, colorInt); 	
	}
	
	/**
	 * Adds a cube (drawn with 12 lines) to the 3D scene.
	 * 
	 * @param xMin minimum value x
	 * @param yMin minimum value y
	 * @param zMin minimum value z
	 * @param xMax maximum value x
	 * @param yMax maximum value y
	 * @param zMax maximum value z
	 * @param rgb Color of cube lines (argb)
	 */
	public void add3DCube(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int rgb) {
		double[][] cube = { 
				{ xMin-xCenter, yMin-yCenter, zMin-zCenter}, // left  front down [0] 
				{ xMax-xCenter, yMin-yCenter, zMin-zCenter}, // right front down [1] 
				{ xMin-xCenter, yMin-yCenter, zMax-zCenter}, // left  front up   [2]
				{ xMax-xCenter, yMin-yCenter, zMax-zCenter}, // right front up   [3]
				{ xMin-xCenter, yMax-yCenter, zMin-zCenter}, // left  back  down [4]
				{ xMax-xCenter, yMax-yCenter, zMin-zCenter}, // right back  down [5]
				{ xMin-xCenter, yMax-yCenter, zMax-zCenter}, // left  back  up   [6]
				{ xMax-xCenter, yMax-yCenter, zMax-zCenter}  // right back  up   [7]
		};
		
		
		// set up new 3D lines list
		Line3D[] lines3D = new Line3D[12];
		
		lines3D[0] = setLinePoints(new Line3D(),cube[0], cube[1], rgb);
		lines3D[1] = setLinePoints(new Line3D(),cube[0], cube[2], rgb);
		lines3D[2] = setLinePoints(new Line3D(),cube[0], cube[4], rgb);
		lines3D[3] = setLinePoints(new Line3D(),cube[2], cube[6], rgb);
		lines3D[4] = setLinePoints(new Line3D(),cube[4], cube[6], rgb);
		lines3D[5] = setLinePoints(new Line3D(),cube[6], cube[7], rgb);
		lines3D[6] = setLinePoints(new Line3D(),cube[3], cube[7], rgb);
		lines3D[7] = setLinePoints(new Line3D(),cube[2], cube[3], rgb);
		lines3D[8] = setLinePoints(new Line3D(),cube[1], cube[3], rgb);
		lines3D[9] = setLinePoints(new Line3D(),cube[1], cube[5], rgb);
		lines3D[10] = setLinePoints(new Line3D(),cube[4], cube[5], rgb);
		lines3D[11] = setLinePoints(new Line3D(),cube[5], cube[7], rgb);
		
		addCubeLinesList(lines3D);	
	}
	
	public void clearCubes() {
		this.clearCubeLines3D();
	}
	
	/**
	 * Sets a volume stack.
	 * 
	 * @param imagePlus the volume stack to be set
	 * 
	 */
	public void setVolume(ImagePlus imagePlus) {
		volume = new Volume(imagePlus);
		this.zAspectRatio = volume.getZaspectRatio(); // try to read the z-aspect ratio
		transform.setZAspectRatio(zAspectRatio);
		volume.setBuffers(bufferPixels, zbufferPixels, bufferWidth, bufferHeight);
		
		volume.setVolumeLut(volume_lutNr);
		
		volume.setVolumeDrawMode(volume_drawMode);
		volume.setVolumeThreshold(volume_threshold);
		volume.setVolumeCutDist(volume_cutDist);
		
		volume.setVolumeDotsSubsampling(volume_dotsDeltaX, volume_dotsDeltaY, volume_dotsDeltaZ);
		
		volume.setTransform(transform);
	}
	
	
	
	/**
	 * Sets the color lut for the surface plot.
	 * <p>
	 * <ul>
	 * <li> <strong>ORIGINAL</strong></li>
	 * <li> <strong>GRAY</strong></li>
	 * <li> <strong>SPECTRUM</strong></li>
	 * <li> <strong>FIRE</strong></li>
	 * <li> <strong>THERMAL</strong></li>
	 * <li> <strong>ORANGE</strong></li>
	 * <li> <strong>BLUE</strong></li>
	 * <li> <strong>BLACK</strong></li>
	 *</ul>
	 *
	 * @param surfacePlot_lutNr color of look-up-table
	 */
	public void setSurfacePlotLut(int surfacePlot_lutNr) {
		this.surfacePlot_lutNr  = surfacePlot_lutNr;
		if (surfacePlot != null)
			surfacePlot.setSurfacePlotLut(surfacePlot_lutNr);
	}
	
	
	/**
	 * Sets the lightning for the surface plot illumination .
	 * <p>
	 * <ul>
	 * <li>0: no light</li>
	 * <li>1: strong light</li>
	 * </ul>
	 * 
	 * @param surfacePlot_light intensity value between 0 and 1 (default is 0)
	 * 
	 */
	public void setSurfacePlotLight(double surfacePlot_light) {
		this.surfacePlot_light = surfacePlot_light;
		if (surfacePlot != null)
			surfacePlot.setSurfacePLotSetLight(surfacePlot_light);
	}
	
	public void setSurfacePlotMinMax(int surfacePlot_min, int surfacePlot_max) {
		//IJ.log("Min: " + surfacePlot_min + " Max: " + surfacePlot_max);
		this.surfacePlot_min  = surfacePlot_min;
		this.surfacePlot_max  = surfacePlot_max;
		if (surfacePlot != null) {
			surfacePlot.setMinMax(surfacePlot_min, surfacePlot_max);
			surfacePlot.applyMinMax();
		}
	}

	
	/**
	 * Aplies a smoothing to the surface plot data  
	 * 
	 * @param smoothRadius the radius of the smoothing kernel
	 *
	 */
	
	public void setSurfaceSmoothingFactor(double smoothRadius) {
		//System.out.println("Smoothing :" + smoothRadius);
		surfacePlot.applySmoothingFilter(smoothRadius);
	}
	
	
	/**
	 * Sets the threshold.
	 * 
	 * All voxels below this threshold are displayed transparent.
	 * 
	 * @param threshold Threshold to be set (0 .. 255).
	 */
	public void setVolumeThreshold(int threshold) {
		this.volume_threshold = threshold;
		if (volume != null)
			volume.setVolumeThreshold(threshold);
	}	
	
	
	/**
	 * Sets the z-aspect ratio (the relative z-height of a voxel).
	 * When setting a volume, the z-aspect ratio is reset to ratio read from the stack.
	 * If the z-aspect ratio needs to set this has to be done after calling the setVolume method.
	 * 
	 * @param zAspectRatio z-aspect ratio (the relative z-ratio of a voxel) to be set.
	 */
	public void setTransformZAspectRatio(double zAspectRatio) {
		this.zAspectRatio = zAspectRatio;
		transform.setZAspectRatio(zAspectRatio);
	}
	
	
	
   /*************************************************************
	*                    G E T T E R                            *
	*************************************************************/
	
	/**
	 * Returns the buffer width of the image.
	 * 
	 * @return buffer width
	 */
	public int getWidth(){
		return bufferWidth;
	}
	
	/**
	 * Returns the buffer height of the image.
	 * 
	 * @return buffer height
	 */
	public int getHeight(){
		return bufferHeight;
	}
	
	/**
	 * Returns actual rendered image.
	 * 
	 * @return image
	 */
	public Image getImage(){
		return bufferedImage;
	}
	
	/**
	 * Gets the z-aspect ratio (the relative z-height of a voxel).
	 *
	 * @return  the z-aspect ratio
	 */
	public double getTransformZAspectRatio() {
		return transform.getZAspectRatio();
	}

	public void removeLastPoint3D() {
		if (pointsPlot != null){
			pointsPlot.removeLastPoint();
		}
	}

	public void removeLastLine3D() {
		if (lines3D != null) {
			int size = lines3D.size();
			if (size > 0)
				this.lines3D.remove(size-1);
		}
	}

	public int getNumPoints3D() {
		return pointsPlot.getSize();
	}

	public void setTransformPerspective(double perspective) {
		transform.setPerspective(perspective);
		
	}

	public void setTransformMaxDistance(double maxDistance) {
		transform.setMaxDistance(maxDistance);
		
	}

	public void setAxes(boolean axes) {
		this.axes = axes; 	
	}
	
	public void setLines(boolean lines) {
		this.lines = lines; 	
	}
	
	public void setText(boolean text) {
		this.text = text; 	
	}
	
	public void setLegend(boolean legend) {
		this.legend = legend; 	
	}
	
	

	public void surfacePlotSetInverse(boolean b) {
		if (surfacePlot != null)
			surfacePlot.setInverse(b);
	}

	public double getTransformScale() {
		if (transform!= null)
			return transform.getScale();
		else
			return 1;
	}

	public void setMinZValue(double minZ) {
		this.minZ = minZ;
		
	}

	public void setMaxZValue(double maxZ) {
		this.maxZ = maxZ;
	}

	public double getTransformRotationX() {
		return Math.toDegrees(transform.getRotationX());
	}
	
	public double getTransformRotationY() {
		return Math.toDegrees(transform.getRotationY());
	}
	
	public double getTransformRotationZ() {
		return Math.toDegrees(transform.getRotationZ());
	}
	
}
