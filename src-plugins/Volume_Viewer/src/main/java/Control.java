/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

import java.awt.Color;

public class Control {

	boolean LOG = false;

	final static int ORIG = 0;
	final static int GRAY = 1;
	final static int SPECTRUM = 2;
	final static int FIRE = 3;
	final static int THERMAL = 4;
	
	
	final static int SLICE = 0;
	final static int SLICE_AND_BORDERS = 1;
	final static int PROJECTION_MAX = 2;
	final static int PROJECTION = 3;
	final static int VOLUME = 4;
	
	final static String [] renderName = { 
			"Slice ", 
			"Slice & Borders", 
			"Max Projection", 
			"Projection", 
			"Volume" 
	};
	
	
	static final int NN = 0;
	static final int TRILINEAR = 1;
	static final int TRICUBIC_SPLINE = 2;
	static final int TRICUBIC_POLYNOMIAL = 3;

	static final String [] interpolationName = { 
			"Nearest Neighbor", 
			"Trilinear",
			"Tricubic smooth",
			"Tricubic sharp"
	} ;

	
	// transfer function modes 
	static final int ALPHA1 = 0;
	static final int ALPHA2 = 1;
	static final int ALPHA3 = 2;
	static final int ALPHA4 = 3;

	
	// stored vaiables (in prefs) //////////////////
	int renderMode = SLICE;
	int interpolationMode = TRILINEAR;
	Color backgroundColor = new Color(0xFF003465);
	int lutNr = ORIG;
	float zAspect = 1;
	float sampling = 1;
	float dist = 0;
	boolean showAxes = true;
	boolean showSlices = false;
	boolean showClipLines = false;
	float scale = 1;
	float degreeX = 115; 
	float degreeY = 41; 
	float degreeZ = 17;
	int alphaMode = ALPHA1;
	int windowMinHeight = 660;
	int windowHeight = windowMinHeight;
	
	int windowWidthImageRegion = 550; 
	
	// illumination
	boolean useLight = false;
	float ambientValue = 0.5f;
	float diffuseValue = 0.5f;
	float specularValue = 0.5f;
	float shineValue = 17.15f;
	float objectLightValue = 0.5f;
	int lightRed = 255;
	int lightGreen = 128;
	int lightBlue = 0;
	boolean snapshot = false;
	
	int xloc;
	int yloc;
	/////////////////////////////////////
		
	boolean drag;

	int maxDist;
	float scaledDist;

	boolean isRGB = false;
	int lumTolerance = 32, gradTolerance = 64;
	
	boolean showTF = true;
	boolean alphaWasChanged = true;
	boolean pickColor = false;
	
	int rPaint = 128, gPaint = 128, bPaint = 128, indexPaint = 128;
	int alphaPaint2 = 64, alphaPaint3 = 64, alphaPaint4 = 64;
	
	int windowWidthSliderRegion = 55; 
	int windowWidthSlices = 255; 

	float positionFactorX = 0.5f; // indicates the shown position of the xslice
	float positionFactorY = 0.5f; // indicates the shown position of the yslice
	float positionFactorZ = 0.5f; // indicates the shown position of the zslice
	
	private Volume_Viewer vv;

	public boolean distWasSet;

	public boolean isReady = false;

	
	public Control(Volume_Viewer volume_Viewer) {
		this.vv = volume_Viewer;
	}

	public void newDisplayMode() {
		vv.gui.newDisplayMode();
	}

	public void reset() {
		interpolationMode = TRILINEAR;
		backgroundColor = new Color(0xFF003465);
		lutNr = ORIG;
		zAspect = 1;
		sampling = 1;
		//float dist = 0;
		showAxes = true;
		showSlices = false;
		showClipLines = false;
		scale = 1;
		degreeX = 115; 
		degreeY = 41; 
		degreeZ = 17;
		alphaMode = ALPHA1;
		
		// illumination
		useLight = false;
		ambientValue = 0.5f;
		diffuseValue = 0.5f;
		specularValue = 0.5f;
		shineValue = 17.15f;
		objectLightValue = 0.5f;
		lightRed = 255;
		lightGreen = 128;
		lightBlue = 0;
		
	}
}
