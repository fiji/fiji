import ij.plugin.filter.PlugInFilter;
import ij.plugin.PlugIn;
import java.awt.Color;
import java.util.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
 

/*
	Uses ImageJ's particle analyzer to track the movement of
	multiple objects through a stack. 
	Based on the Object Tracker plugin filter by Wayne Rasband

	Based on Multitracker, but should be quite a bit more intelligent
	Nico Stuurman, Vale Lab, UCSF/HHMI, May,June 2003

	"Tool for Automated Sporozoite Tracking"
	a bit modified my Misha Kudryashev outputing some extra params
	for the use of tracking of malaria parasites that run in circles
	Frischknecht AG, University of Heidelberg, 
	march 2008
	See publication S. Hegge et al, Biotechnology Journal, 2009
	email:misha.kudryashev@gmail.com
*/
public class ToAST25_ implements PlugInFilter,Measurements  {

	ImagePlus	imp;
	int		nParticles;
	float[][]	ssx;
	float[][]	ssy;
	String directory,filename;

	static int	minSize = 15;
	static int	maxSize = 69;
	static int 	minTrackLength = 20;	
	static double 	pixelSize = 627;
	static double 	timeLapse = 2;
	static boolean 	bSaveResultsFile = true;
	static boolean 	bShowLabels = true;
	static boolean 	bShowPositions = true;
	static boolean 	bShowPaths = true;
	static boolean 	bShowPathLengths = true;
	static boolean	bCalculateCurvature = false; 
	static boolean	bDisplayRaw = true; 
	static boolean	bDisplayMotParam = false; 
	static boolean	patchglidersExist = false; 
    	static float   	maxVelocity = 20;
	static int 	maxColumns=75;
	static double 	spfLimit = 0.4; //minimal circularity to be trached
	static double 	velLimit1 = 0.5;//attached speed limit, microm/sec
	static double 	velLimit2 = 0;//2.95*(double)(pixelSize)/1000/(double)timeLapse;
	static double 	sepfactor = 0.0222, sepfactor2 = 0.2;//1/45;  
	static int 	filenumber = 1; 
	static int 	slidingAverageHalfSize = 4; 
	static double 	displacementLimit = 4.5*pixelSize/1000; //displacementLimit is a limit of average_displacement/average_velosity ratio to separate PGs
	
	double [][]spfactors; 
	double [][]plengths; 
	int [][]pixnumbers; 
	double [][]curvatures; 
	double [][]perimeter; 

	public class particle {
		float	x;
		float	y;
		int	z;
		int	trackNr;
		boolean inTrack=false;
		boolean flag=false;

		public void copy(particle source) {
			this.x=source.x;
			this.y=source.y;
			this.z=source.z;
			this.inTrack=source.inTrack;
			this.flag=source.flag;
		}

		public float distance (particle p) {
			return (float) Math.sqrt(sqr(this.x-p.x) + sqr(this.y-p.y));
		}
	}

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (IJ.versionLessThan("1.17y")){
			IJ.log("your imageJ version is below 1.17y, quittin'"); 
			return DONE;
		}
		else
			return DOES_8G;//+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
//		if(isMacintosh())IJ.log("usage of Macintosh can badly infuence your brain!!!"); 
		GenericDialog gd = new GenericDialog("ToAST25");
		gd.addMessage("TOol for Automated Sporozoite Tracking");
		gd.addNumericField("Min Object Size (pixels): ", minSize, 0);
		gd.addNumericField("Max Object Size (pixels): ", maxSize, 0);
                gd.addNumericField("Maximum Speed (pixels(/frame):", maxVelocity, 0);
		gd.addNumericField("Minimum track length (frames)", minTrackLength, 0);		
		gd.addNumericField("Pixel size (in nanometers)", pixelSize, 0);
		gd.addNumericField("Time lapse (in sec)", timeLapse, 0);
		gd.addNumericField("Sliding Average HalfSize ", slidingAverageHalfSize, 0);
		gd.addNumericField("Minimal elongation (0..1)", spfLimit, 1);
		gd.addNumericField("File number", filenumber, 0);
		gd.addCheckbox("Save_Results File", bSaveResultsFile);
		gd.addCheckbox("Display Path Lengths", bShowPathLengths);
		gd.addCheckbox("Show_Labels", bShowLabels);
		gd.addCheckbox("Show_Positions", bShowPositions);
		gd.addCheckbox("Show_Paths", bShowPaths);
		gd.addCheckbox("Calculate Curvature", bCalculateCurvature);
		gd.addCheckbox("Output_Raw Tracking", bDisplayRaw);
		gd.addCheckbox("Output_Motility Features", bDisplayMotParam);
//		gd.addCheckbox("Patchgliders exist", patchglidersExist);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		minSize = (int)gd.getNextNumber();
		maxSize = (int)gd.getNextNumber();
		maxVelocity = (float)gd.getNextNumber();
		minTrackLength = (int)gd.getNextNumber();
		pixelSize = gd.getNextNumber();
		timeLapse = gd.getNextNumber();
		slidingAverageHalfSize = (int)gd.getNextNumber();
		spfLimit = gd.getNextNumber();
		filenumber = (int)gd.getNextNumber();
		bSaveResultsFile = gd.getNextBoolean();
		bShowPathLengths = gd.getNextBoolean(); 
		bShowLabels = gd.getNextBoolean();
		bShowPositions = gd.getNextBoolean();
		bShowPaths = gd.getNextBoolean();
		bCalculateCurvature = gd.getNextBoolean();
		bDisplayRaw = gd.getNextBoolean();
		bDisplayMotParam = gd.getNextBoolean();
		patchglidersExist = false;//gd.getNextBoolean();
		if (bShowPositions)
			bShowLabels =true;
		if (bSaveResultsFile) {
			SaveDialog sd=new  SaveDialog("Save Track Results","logfile_"+filenumber,".txt");
			directory=sd.getDirectory();
			filename=sd.getFileName();
		}

		track(imp, minSize, maxSize, maxVelocity, directory, filename);
	}
	

	public void track(ImagePlus imp, int minSize, int maxSize, float maxVelocity, String directory, String filename) {
		int nFrames = imp.getStackSize();
		if (nFrames<2) {
			IJ.showMessage("Tracker", "Stack required");
			return;
		}

		ImageStack stack = imp.getStack();
		int options = 0; // set all PA options false
		int measurements = CENTROID;

		// Initialize results table
		ResultsTable rt = new ResultsTable();
		rt.reset();

		// create storage for particle positions
		List[] theParticles = new ArrayList[nFrames];
		int trackCount=0;

		// record particle positions for each frame in an ArrayList
		for (int iFrame=1; iFrame<=nFrames; iFrame++) {
			theParticles[iFrame-1]=new ArrayList<particle>();
			rt.reset();
			ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize);
			pa.analyze(imp, stack.getProcessor(iFrame));
			float[] sxRes = rt.getColumn(ResultsTable.X_CENTROID);				
			float[] syRes = rt.getColumn(ResultsTable.Y_CENTROID);
			if (sxRes==null)
				return;

			for (int iPart=0; iPart<sxRes.length; iPart++) {
				particle aParticle = new particle();
				aParticle.x=sxRes[iPart];
				aParticle.y=syRes[iPart];
				aParticle.z=iFrame-1;
				theParticles[iFrame-1].add(aParticle);
			}
			IJ.showProgress((double)iFrame/nFrames);
		}

		// now assemble tracks out of the particle lists
		// Also record to which track a particle belongs in ArrayLists
		List theTracks = new ArrayList();
		for (int i=0; i<=(nFrames-1); i++) {
			IJ.showProgress((double)i/nFrames);
			for (ListIterator j=theParticles[i].listIterator();j.hasNext();) {
				particle aParticle=(particle) j.next();
				if (!aParticle.inTrack) {
					// This must be the beginning of a new track
					List aTrack = new ArrayList();
					trackCount++;
					aParticle.inTrack=true;
					aParticle.trackNr=trackCount;
					aTrack.add(aParticle);
					// search in next frames for more particles to be added to track
					boolean searchOn=true;
					particle oldParticle=new particle();
					particle tmpParticle=new particle();
					oldParticle.copy(aParticle);
					for (int iF=i+1; iF<=(nFrames-1);iF++) {
						boolean foundOne=false;
						particle newParticle=new particle();
						for (ListIterator jF=theParticles[iF].listIterator();jF.hasNext() && searchOn;) { 
							particle testParticle =(particle) jF.next();
							float distance = testParticle.distance(oldParticle);
							// record a particle when it is within the search radius, and when it had not yet been claimed by another track
							if ( (distance < maxVelocity) && !testParticle.inTrack) {
								// if we had not found a particle before, it is easy
								if (!foundOne) {
									tmpParticle=testParticle;
									testParticle.inTrack=true;
									testParticle.trackNr=trackCount;
									newParticle.copy(testParticle);
									foundOne=true;
								}
								else {
									// if we had one before, we'll take this one if it is closer.  In any case, flag these particles
									testParticle.flag=true;
									if (distance < newParticle.distance(oldParticle)) {
										testParticle.inTrack=true;
										testParticle.trackNr=trackCount;
										newParticle.copy(testParticle);
										tmpParticle.inTrack=false;
										tmpParticle.trackNr=0;
										tmpParticle=testParticle;
									}
									else {
										newParticle.flag=true;
									}	
								}
							}
							else if (distance < maxVelocity) {
							// this particle is already in another track but could have been part of this one
							// We have a number of choices here:
							// 1. Sort out to which track this particle really belongs (but how?)
							// 2. Stop this track
							// 3. Stop this track, and also delete the remainder of the other one
							// 4. Stop this track and flag this particle:
								testParticle.flag=true;
							}
						}
						if (foundOne)
							aTrack.add(newParticle);
						else
							searchOn=false;
						oldParticle.copy(newParticle);
					}
					theTracks.add(aTrack);
				}
			}
		}
//		dos.write("nFrames "+ nFrames+ " trackCount "+ trackCount+ " " + theTracks.size());
		// Create the column headings based on the number of tracks
		// with length greater than minTrackLength
		// since the number of tracks can be larger than can be accomodated by Excell, we deliver the tracks in chunks of maxColumns
		// As a side-effect, this makes the code quite complicated
		String strHeadings = "Frame";
		int frameCount=1;
		for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) {
			List bTrack=(ArrayList) iT.next();
			if (bTrack.size() >= minTrackLength) {
				if (frameCount <= maxColumns)
					strHeadings += "\tX" + frameCount + "\tY" + frameCount +"\tFlag" + frameCount;
				frameCount++;
			}
		}

		String contents="";
		boolean writefile=false;
		if (filename != null) {
			File outputfile=new File (directory,filename);
			if (!outputfile.canWrite()) {
				try {
					outputfile.createNewFile();
				}
				catch (IOException e) {
					IJ.showMessage ("Error", "Could not create "+directory+filename);
				}
			}
			if (outputfile.canWrite())
				writefile=true;
			else
				IJ.showMessage ("Error", "Could not write to " + directory + filename);
		}

		String flag;
		int repeat=(int) ( (frameCount/maxColumns) );
		float reTest = (float) frameCount/ (float) maxColumns;
	
		int maxPossibleTrackNum = 1000; 
		int paramnumber = 14; 
		double [][][]parameters = new double[paramnumber][maxPossibleTrackNum][nFrames]; 
		float[][] xx = new float[maxPossibleTrackNum][nFrames];int pcnt = 0, fncnt = 0;
		float[][] yy = new float[maxPossibleTrackNum][nFrames];
		float [][] vel = new float[maxPossibleTrackNum][nFrames];
		double [][]angles = new double[maxPossibleTrackNum][nFrames]; 
		spfactors = new double[maxPossibleTrackNum][nFrames]; 
		plengths = new double[maxPossibleTrackNum][nFrames]; 
		pixnumbers = new int[maxPossibleTrackNum][nFrames]; 
		curvatures = new double[maxPossibleTrackNum][nFrames]; 
		perimeter = new double [maxPossibleTrackNum][nFrames]; 

		for (int j = 0; j<maxPossibleTrackNum*nFrames; j++){angles[j/nFrames][j%nFrames] = 0; spfactors[j/nFrames][j%nFrames] = 0;plengths[j/nFrames][j%nFrames] = 0;pixnumbers[j/nFrames][j%nFrames] = 0;curvatures[j/nFrames][j%nFrames] = 0;}
		int tracksNum = 0;

		if (reTest > repeat)
			repeat++;

		// display the table with particle positions
		// first when we only write to the screen
		if (!writefile) {
			IJ.setColumnHeadings(strHeadings);
			repeat = 1;
			for (int j=1; j<=repeat;j++) {
				int to=j*maxColumns;
				if (to > frameCount-1)
					to=frameCount-1;
				String stLine="Tracks " + ((j-1)*maxColumns+1) +" to " +to;
				IJ.write(stLine);
				for(int i=0; i<=(nFrames-1); i++) {
					String strLine = "" + (i+1);
					int trackNr=0;
					int listTrackNr=0;
					for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) {
						trackNr++;
						List bTrack=(ArrayList) iT.next();
						boolean particleFound=false;
						if (bTrack.size() >= minTrackLength) {
							listTrackNr++;
							if ( (listTrackNr>((j-1)*maxColumns)) && (listTrackNr<=(j*maxColumns))) {
								for (ListIterator k=theParticles[i].listIterator();k.hasNext() && !particleFound;) {
									particle aParticle=(particle) k.next();
									if (aParticle.trackNr==trackNr) {
										particleFound=true;
										if (aParticle.flag) 
											flag="*";
										else
											flag=" ";
										strLine+="\t" + aParticle.x + "\t" + aParticle.y + "\t" /*+ vel[listTrackNr][i] + "yo\t" + flag*/;
									}
								}
								if (!particleFound)
									strLine+="\t \t \t ";
							}
						}
					}
					IJ.write(strLine);
				}
			}
		}
		// and now when we write to file
//		if (writefile) {	
			try {
				File outputfile=new File (directory,filename);
				BufferedWriter dos= new BufferedWriter (new FileWriter (outputfile));
//				dos.close();
//		}

		// Now do the fancy stuff when requested:

		// makes a new stack with objects labeled with track nr
		// optionally also displays centroid position
		if (bShowLabels) {
			String strPart;
//			ImageStack newstack = imp.createEmptyStack();
//			int xHeight=newstack.getHeight();
//			int yWidth=newstack.getWidth();
			for (int i=0; i<=(nFrames-1); i++) {
				int iFrame=i+1;
				String strLine = "" + i;
				ImageProcessor ip = stack.getProcessor(iFrame);
//				newstack.addSlice(stack.getSliceLabel(iFrame),ip.crop());
//				ImageProcessor nip = newstack.getProcessor(iFrame);
//				nip.setColor(Color.black);
				// hack to only show tracks longerthan minTrackLength
				int trackNr=0;
				int displayTrackNr=0;
				for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) { 
					trackNr++;
					List bTrack=(ArrayList) iT.next();
					if (bTrack.size() >= minTrackLength) {
						displayTrackNr++;
						for (ListIterator k=theParticles[i].listIterator();k.hasNext();) {
							particle aParticle=(particle) k.next();
							if (aParticle.trackNr==trackNr) {
								strPart=""+displayTrackNr;
								if (bShowPositions) {
									strPart+="="+(int)aParticle.x+","+(int)aParticle.y;
									xx[displayTrackNr][i] = aParticle.x;
									yy[displayTrackNr][i] = aParticle.y;

								}
								// we could do someboundary testing here to place the labels better when we are close to the edge
//								nip.moveTo((int)aParticle.x+5,doOffset((int)aParticle.y,yWidth,5) );
								//nip.moveTo(doOffset((int)aParticle.x,xHeight,5),doOffset((int)aParticle.y,yWidth,5) );
//								nip.drawString(strPart);
							}
						}
					}
				}
				tracksNum = displayTrackNr;
				IJ.showProgress((double)iFrame/nFrames);
			}
//			ImagePlus nimp = new ImagePlus(imp.getTitle() + " labels",newstack);
//			nimp.show();
//			imp.show();
//			nimp.updateAndDraw();
		}
			
		dos.write("Filenum_" + filenumber + "\n");
		for (int iFrame = 1; iFrame< nFrames; iFrame++){
	        	IJ.showStatus("Processing frame " + iFrame + "/" + nFrames);//(int)(((double)iFrame/(double)nFrames)*100) + " %");
			ImageProcessor iProc = stack.getProcessor(iFrame);
			iProc = iProc.convertToRGB(); 
			int[] pixels = (int[])iProc.getPixels();
			int W = iProc.getWidth(); 
			int H = iProc.getHeight();
			int bright = 0, i, step = 1; 
			double currAngle = 0; 
			for(i = 0; i < W*H; i++){pixels[i] = /*255-*/((pixels[i]+ 16777216) & 0x0000ff);}
			for (i = 1; i<= tracksNum; i++){
				iFrame = iFrame-1;//shit with the array numeration so just did this way
				double borderShift = 2*Math.sqrt((maxSize-minSize)/2); currAngle = -500;
				if((xx[i][iFrame] > borderShift) && (yy[i][iFrame] > borderShift) && (xx[i][iFrame] < (W - borderShift)) && (yy[i][iFrame] < (H - borderShift)))
				if(pixels[(int)xx[i][iFrame] + ((int)yy[i][iFrame])*W] == bright ){
					currAngle = segmentation(pixels, W, H, (int)xx[i][iFrame],(int)yy[i][iFrame],bright, minSize,maxSize, i, iFrame);
				}else
				while(currAngle == -500 && step<16){
					if(pixels[(int)(xx[i][iFrame]+step) + ((int)yy[i][iFrame])*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)xx[i][iFrame]+step,(int)yy[i][iFrame],bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step) + ((int)yy[i][iFrame])*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)xx[i][iFrame]-step,(int)yy[i][iFrame],bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]) + ((int)(yy[i][iFrame]+step))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)xx[i][iFrame],(int)yy[i][iFrame]+step,bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]) + ((int)(yy[i][iFrame]-step))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)xx[i][iFrame],(int)yy[i][iFrame]-step,bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]+step/2) + ((int)(yy[i][iFrame]+step/2))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]+step/2),(int)(yy[i][iFrame]+step/2),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/2) + ((int)(yy[i][iFrame]+step/2))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/2),(int)(yy[i][iFrame]+step/2),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]+step/2) + ((int)(yy[i][iFrame]-step/2))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]+step/2),(int)(yy[i][iFrame]-step/2),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/2) + ((int)(yy[i][iFrame]-step/2))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/2),(int)(yy[i][iFrame]-step/2),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/4) + ((int)(yy[i][iFrame]-step/4))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/4),(int)(yy[i][iFrame]-step/4),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/4) + ((int)(yy[i][iFrame]-step/4))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/4),(int)(yy[i][iFrame]-step/4),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/4) + ((int)(yy[i][iFrame]-step/4))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/4),(int)(yy[i][iFrame]-step/4),bright, minSize,maxSize, i, iFrame);
					}else
					if(pixels[(int)(xx[i][iFrame]-step/4) + ((int)(yy[i][iFrame]-step/4))*W] == bright){
						currAngle = segmentation(pixels, W, H, (int)(xx[i][iFrame]-step/4),(int)(yy[i][iFrame]-step/4),bright, minSize,maxSize, i, iFrame);
					}
					step++;
				}iFrame = iFrame+1;

				angles[i][iFrame-1] = currAngle; 
				for (int b = 0; b < W; b++)if ((int)(b*Math.tan(currAngle*6.28/360)+yy[i][iFrame]*Math.tan(currAngle*6.28/360))>0 && (int)(b*Math.tan(currAngle*6.28/360)+yy[i][iFrame]*Math.tan(currAngle*6.28/360))<H ) 
					pixels[b + W*(int)(b*Math.tan(currAngle*6.28/360)+yy[i][iFrame]*Math.tan(currAngle*6.28/360))]= 255; 
			}

		}
		
		for(int i = 1; i <= tracksNum; i++){vel[i][0] = 0;
			double sumSpf = 0, spfcnt = 0; 
			for (int b =0; b < nFrames-1; b++)if(spfactors[i][b]!=0){sumSpf += spfactors[i][b]; spfcnt++;}
			sumSpf /= spfcnt;
			if (sumSpf < spfLimit)continue;
			for (int b = 1; b < nFrames-1; b++){
				if(xx[i][b-1]>0 && yy[i][b-1]>0 && xx[i][b]>0 && yy[i][b]>0)vel[i][b] = (float)Math.sqrt((xx[i][b]-xx[i][b-1])*(xx[i][b]-xx[i][b-1]) + (yy[i][b]-yy[i][b-1])*(yy[i][b]-yy[i][b-1]))*(float)pixelSize/(float)timeLapse/1000; else vel[i][b] = 0; 
				double angle = (Math.atan((double)(yy[i][b+1] - yy[i][b])/(double)(xx[i][b+1] - xx[i][b])) - Math.atan((double)(yy[i][b] - yy[i][b-1])/(double)(xx[i][b] - xx[i][b-1])))*360/6.28*timeLapse/2;
				double a2 = (xx[i][b-1]-xx[i][b])*(xx[i][b-1]-xx[i][b]) + (yy[i][b-1]-yy[i][b])*(yy[i][b-1]-yy[i][b]);
				double b2 = (xx[i][b+1]-xx[i][b])*(xx[i][b+1]-xx[i][b]) + (yy[i][b+1]-yy[i][b])*(yy[i][b+1]-yy[i][b]);
				double c2 = (xx[i][b+1]-xx[i][b-1])*(xx[i][b+1]-xx[i][b-1]) + (yy[i][b+1]-yy[i][b-1])*(yy[i][b+1]-yy[i][b-1]);
				if(((a2+b2)>c2 && Math.abs(angle) < -90) || ((a2+b2)<c2 && Math.abs(angle)>90))	angle = ((-1)*Math.min((180-Math.abs(angle)),Math.abs(angle))*(angle/Math.abs(angle))); 
				if(!((angle<180) && (angle>-180)))angle =0; 
				double angle_m = (double)Math.atan((double)(yy[i][b+1] - yy[i][b])/(double)(xx[i][b+1] - xx[i][b]))*360/6.28;
				if((xx[i][b+1]<xx[i][b])&&(yy[i][b+1]<=yy[i][b]))angle_m +=180;
				else if ((xx[i][b+1]<xx[i][b])&&(yy[i][b+1]>=yy[i][b]))angle_m +=360;
				else if ((xx[i][b+1]>xx[i][b])&&(yy[i][b+1]<=yy[i][b]))angle_m +=180;
				else if ((xx[i][b+1]==xx[i][b])&&(yy[i][b+1]<yy[i][b]))angle_m =90;
				else if ((xx[i][b+1]==xx[i][b])&&(yy[i][b+1]>yy[i][b]))angle_m =270;
				else if ((xx[i][b+1]==xx[i][b])&&(yy[i][b+1]==yy[i][b]))angle_m =0;
				parameters[0][i][b] = (double)vel[i][b]; 
				parameters[1][i][b] = (double)angle; //angle of the orientation
				parameters[2][i][b] = (double)angles[i][b]; //angle'
				if(spfactors[i][b]>=0 && spfactors[i][b]<=1) parameters[3][i][b] = (double)spfactors[i][b];//1-circularity 
				parameters[4][i][b] = (double)plengths[i][b]; //lenghth of the objects
				parameters[5][i][b] = (double)Math.abs(pixnumbers[i][b]); //number of pixels
				parameters[6][i][b] = (double)curvatures[i][b]; //curvatures
				parameters[7][i][b] = (double)perimeter[i][b]; //perimeter
				parameters[8][i][b] = (double)b; //time 
				parameters[10][i][b] = (double)xx[i][b]; //X coord
				parameters[11][i][b] = (double)yy[i][b]; // Y coord
				parameters[12][i][b] = angle_m;//displacement direction
				parameters[13][i][b] = i; //tracknumber
				
				if(b >= slidingAverageHalfSize*2+1){
					double vel1 = 0, angle1 = 0, angle2 = 0;
					for (int t = 0; t < slidingAverageHalfSize*2+1; t++){vel1+=vel[i][b-t]/(double)(slidingAverageHalfSize*2+1); angle1+=Math.abs(angles[i][b-t])/(double)(slidingAverageHalfSize*2+1);angle2 = angles[i][b-t]/(double)(slidingAverageHalfSize*2+1);}
					if(vel1<=velLimit1)parameters[9][i][b] = 0;
					else if((vel1 > Math.abs(angle1)*sepfactor2 + velLimit2) && patchglidersExist == true)parameters[9][i][b] = 4;
					else if((angle2>0) &&(vel1 > Math.abs(angle1)*sepfactor))parameters[9][i][b] = 2;
					else if((angle2<0) &&(vel1 > Math.abs(angle1)*sepfactor))parameters[9][i][b] = 3;
					else parameters[9][i][b] = 1;
				}
				if(bDisplayRaw == true)
					if(xx[i][b-1]==0 && yy[i][b-1]==0)dos.write("" + (i) + " picnum "+ (b+1) + " x&y " + xx[i][b] + " " + yy[i][b] + " Speed(microm/sec) " + vel[i][b] + " rotatonal_speed " + 0 + " angle_orientation " + 0 + " SpFactor " + spfactors[i][b] + " length "+ plengths[i][b] + " pixelnumber " + pixnumbers[i][b] + " curvature " + curvatures[i][b] + " perimeter "  + perimeter[i][b] +" moving_dir " + angle_m + " Moving_Type " + parameters[9][i][b] + "\n");
					else if(xx[i][b+1]==0 && yy[i][b+1]==0)dos.write("" + (i) + " picnum "+ (b+1) + " x&y " + xx[i][b] + " " + yy[i][b] + " speed(microm/sec) " + vel[i][b] + " rotatonal_speed " + 0 + " angle_orientation " + 0 + " SpFactor " + spfactors[i][b] + " length "+ plengths[i][b] + " pixelnumber " + pixnumbers[i][b] +" curvature " + curvatures[i][b] + " perimeter "  + perimeter[i][b] +" moving_dir " + angle_m + " Moving_Type " + parameters[9][i][b] + "\n");
					else if(xx[i][b]==0 && yy[i][b]==0)dos.write("" + (i) + " picnum "+ (b+1) + " x&y " + xx[i][b] + " " + yy[i][b] + " speed(microm/sec) " + 0 + " rotatonal_speed " + 0 + " angle_orientation " + 0 + " SpFactor " + spfactors[i][b] + " length "+ plengths[i][b] + " pixelnumber " + pixnumbers[i][b] +" curvature " + curvatures[i][b] + " perimeter "  + perimeter[i][b] + " moving_dir " + angle_m +" Moving_Type " + parameters[9][i][b] + "\n");
					else {dos.write("" + (i) + " picnum "+ (b+1) + " x&y " + xx[i][b] + " " + yy[i][b] + " speed(microm/sec) " + vel[i][b] + " rotatonal_speed " + angle + " angle_orientation " + angles[i][b]+ " SpFactor " + spfactors[i][b] + " length "+ plengths[i][b] + " pixelnumber " + pixnumbers[i][b]+ " curvature " + curvatures[i][b] + " perimeter "  + perimeter[i][b] + " moving_dir " + angle_m + " Moving_Type " + parameters[9][i][b] + "\n");}
					
				//angles[i][b] = angle;
				if(xx[i][b-1]>0 && yy[i][b-1]>0 && xx[i][b+1]>0 && yy[i][b+1]>0 ) angles[i][b] = angle;else angles[i][b] = 0; 
					
			}
		}
        	IJ.showStatus("Visualising ");		
		// makes a new stack with objects labeled with track nr
		// optionally also displays centroid position
		if (bShowLabels) {
			String strPart;
			ImageStack newstack = imp.createEmptyStack();
			int xHeight=newstack.getHeight();
			int yWidth=newstack.getWidth();
			for (int i=0; i<=(nFrames-1); i++) {
				int iFrame=i+1;
				String strLine = "" + i;
				ImageProcessor ip = stack.getProcessor(iFrame);
				newstack.addSlice(stack.getSliceLabel(iFrame),ip.crop());
				ImageProcessor nip = newstack.getProcessor(iFrame);
				nip.setColor(Color.black);
				// hack to only show tracks longerthan minTrackLength
				int trackNr=0;
				int displayTrackNr=0;
				for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) { 
					trackNr++;
					List bTrack=(ArrayList) iT.next();
					if (bTrack.size() >= minTrackLength) {
						displayTrackNr++;
						for (ListIterator k=theParticles[i].listIterator();k.hasNext();) {
							particle aParticle=(particle) k.next();
							if (aParticle.trackNr==trackNr) {
								strPart=""+displayTrackNr;
								if (bShowPositions) {
									strPart+="="+(int)aParticle.x+","+(int)aParticle.y+","+(int)parameters[9][displayTrackNr][i];//(int)perimeter[displayTrackNr][i]; //(int)(1/curvatures[displayTrackNr][i]);//+","+0+"."+(int)(100*spfactors[displayTprackNr][i]);
									//xx[displayTrackNr][i] = aParticle.x;
									//yy[displayTrackNr][i] = aParticle.y;

								}
								// we could do someboundary testing here to place the labels better when we are close to the edge
								nip.moveTo((int)aParticle.x+5,doOffset((int)aParticle.y,yWidth,5) );
								//nip.moveTo(doOffset((int)aParticle.x,xHeight,5),doOffset((int)aParticle.y,yWidth,5) );
								double sumSpf = 0, spfcnt = 0; 
								for (int b =0; b < nFrames-1; b++)if(spfactors[displayTrackNr][b]!=0){sumSpf += spfactors[displayTrackNr][b]; spfcnt++;}
								sumSpf /= spfcnt;
								if(sumSpf > spfLimit)nip.drawString(strPart);else nip.drawString("="+displayTrackNr+","+0+"."+(int)(100*sumSpf));
							}
						}
					}
				}
				tracksNum = displayTrackNr;
				IJ.showProgress((double)iFrame/nFrames);
			}
			ImagePlus nimp = new ImagePlus(imp.getTitle() + " labels",newstack);
			nimp.show();
			imp.show();
			nimp.updateAndDraw();
		}

		dos.write("overall statistics for each parasite" + "\n"); 
		long totalTime = 0, waverTime = 0, ccwTime = 0, cwTime = 0, attachedTime = 0, patchGlidingTime = 0; 
		long totalTimeParasite, waverTimeParasite, ccwTimeParasite, cwTimeParasite, attachedTimeParasite, patchGlidingTimeParasite; 
		for(int i = 1; i <= tracksNum; i++){
			totalTimeParasite = 0; waverTimeParasite = 0; ccwTimeParasite = 0; cwTimeParasite = 0; attachedTimeParasite = 0; patchGlidingTimeParasite = 0; 
			double sumSpf = 0, spfcnt = 0; 
			for (int b =0; b < nFrames-1; b++)if(spfactors[i][b]!=0){sumSpf += spfactors[i][b]; spfcnt++;}
			sumSpf /= spfcnt;
			if (sumSpf < spfLimit)continue;
			for (int b = 1; b < nFrames-1; b++){
				if ((angles[i][b]*vel[i][b])==0)continue; 
				totalTimeParasite++;
				if(vel[i][b]<=velLimit1)attachedTimeParasite++;
				else if((vel[i][b] > Math.abs(angles[i][b])*sepfactor2 + velLimit2) && patchglidersExist == true)patchGlidingTimeParasite++;
				else if((angles[i][b]>0) &&(vel[i][b] > Math.abs(angles[i][b])*sepfactor))cwTimeParasite++;
				else if((angles[i][b]<0) &&(vel[i][b] > Math.abs(angles[i][b])*sepfactor))ccwTimeParasite++;
				else waverTimeParasite++;
			}
			dos.write("Track"+ i + " Total_time(frames) " +totalTimeParasite + " Attached " + attachedTimeParasite + " Waving " +waverTimeParasite + " CWmover " + cwTimeParasite + " CCWmover " + ccwTimeParasite + " PatchGliding " + patchGlidingTimeParasite +"\n"); 
			totalTime+=totalTimeParasite; waverTime+=waverTimeParasite; ccwTime += ccwTimeParasite; cwTime+= cwTimeParasite; attachedTime+=attachedTimeParasite; patchGlidingTime+=patchGlidingTimeParasite;
		}
		dos.write("Total"+ " Total_time(frames) " +totalTime + " Attached " + attachedTime + " Waving " +waverTime + " CWmover " + cwTime + " CCWmover " + ccwTime + " patchGliding " + patchGlidingTime+ "\n"); 

		int arraysize = (int)Math.max(32000,(double)(nFrames*tracksNum)/1.5),j;
		double parameters_movingtypes[][][] = new double[paramnumber][5][arraysize]; 
		double angles_abs_movingtypes[][] = new double[5][arraysize]; //first index 0 - attached, 1 - waver, 2 - cw, 3 - ccw; 
		double vels_movingtypes[][] = new double[5][arraysize]; 
		double spfactors_movingtypes[][] = new double[5][arraysize]; 
		double lengths_movingtypes[][] = new double[5][arraysize]; 
		double pixelnums_movingtypes[][] = new double[5][arraysize]; 
		int cnt[] = new int[5];cnt[0] = 0; cnt[1] = 0; cnt[2] = 0; cnt[3] = 0; cnt[4] = 0; //cnt_angles_a=0, cnt_angles_w=0, cnt_angles_cw=0, cnt_angles_ccw=0, cnt_vel_a = 0, cnt_vel_w = 0, cnt_vel_cw = 0, cnt_vel_ccw = 0, cnt_spf_a=0,cnt_spf_w=0,cnt_spf_cw=0,cnt_spf_ccw=0,cnt_len_a=0,cnt_len_w=0,cnt_len_cw=0,cnt_len_ccw=0,cnt_
		double statelengths[][] = new double[5][arraysize]; //lists of the length of all the states.
		int statescnt[] = new int[5], unfinished_states[] = new int[5];
		dos.write("overall statistics for each parasite(sliding_average_of_" + (2*slidingAverageHalfSize+1)+"_frames)" + "\n"); 
		totalTime = 0; waverTime = 0; ccwTime = 0; cwTime = 0; attachedTime = 0; patchGlidingTime= 0; 
		int spfscrew = 0; 
		long stateTransmissions[][] = new long[5][5]; 
		long stateTransmissionsCondition[][] = new long[5][5]; //if the track is longer then 10 seconds; 
		for (int b = 0; b<5; b++)for(int g = 0; g<5; g++) {stateTransmissions[b][g] = 0; stateTransmissionsCondition[b][g] = 0;}
		double sameStateProbability[][] = new double[5][nFrames]; //second index numerates time
		long sameStateCnt[][] = new long[5][nFrames];//count of precessed timepoints.
		double stateDisplacement[][] = new double[5][nFrames];//displacements for esch state over time
		double stateDisplacementConditional[][] = new double[5][nFrames]; //displacement if parasite stays in the same condition
		long sameStateCntConditional[][] = new long [5][nFrames];//count to divide. 


		for(int i = 1; i <= tracksNum; i++){
			totalTimeParasite = 0; waverTimeParasite = 0; ccwTimeParasite = 0; cwTimeParasite = 0; attachedTimeParasite = 0;patchGlidingTimeParasite = 0; 
			double sumSpf = 0, spfcnt = 0; 
			for (int b =0; b < nFrames-1; b++)if(spfactors[i][b]!=0){sumSpf += spfactors[i][b]; spfcnt++;}
			sumSpf /= spfcnt;
			if (sumSpf < spfLimit)continue;
			double params_ave[][] = new double[paramnumber][nFrames-2*slidingAverageHalfSize]; 
			double angles_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 
			double angles_abs_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 
			double vel_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 
			double spfactors_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 
			double length_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 
			double pixelsize_ave[] = new double[nFrames-2*slidingAverageHalfSize]; 


			int index = 0, belongflag = 0, spffalse = 0, oldbelongflag = 0, timeshift = 0; 
			int states[] = new int[nFrames]; //states for one parasite
			for (int g = 0; g<nFrames-2*slidingAverageHalfSize; g++){angles_ave[g] = 0; vel_ave[g] = 0;}
			for (int b = 0; b < nFrames-2*slidingAverageHalfSize-1; b++){
				if ((vel[i][b]*angles[i][b] ==0)||(vel[i][b+2*slidingAverageHalfSize+1]*angles[i][b+2*slidingAverageHalfSize+1] ==0)){timeshift++;continue;} 
				for (int g = 0; g<2*slidingAverageHalfSize+1; g++){
					for (int k1 = 0; k1 < paramnumber; k1++){
						if (k1!=2)params_ave[k1][index] += parameters[k1][i][b+g]/(double)(2*slidingAverageHalfSize+1); else
						if(k1 == 2 && Math.abs(parameters[2][i][b+g]) < 180) params_ave[k1][index] += parameters[k1][i][b+g]/(2*slidingAverageHalfSize+1);
					}
					angles_ave[index] += angles[i][b+g]/(2*slidingAverageHalfSize+1); 
					if(Math.abs(angles[i][b+g]) < 180)angles_abs_ave[index] += Math.abs(angles[i][b+g])/(2*slidingAverageHalfSize+1); 
					if(vel[i][b+g]<maxVelocity)vel_ave[index] += vel[i][b+g]/(2*slidingAverageHalfSize+1);//else IJ.log("suxxvel tracknum/picnum " + i + " " + b + " " + vel[i][b+g]);
					if(spfactors[i][b+g] >= 0 && spfactors[i][b+g] <= 1)spfactors_ave[index] += spfactors[i][b+g];
					else spffalse ++;
					length_ave[index] += plengths[i][b+g]/(2*slidingAverageHalfSize+1);
					pixelsize_ave[index] += pixnumbers[i][b+g]/(2*slidingAverageHalfSize+1);
				}
//				dos.write("avevel "+vel_ave[index] + " aveangle "+ angles_ave[index]); 
				if (spfactors_ave[index] > 0 && spfactors_ave[index] < (2*slidingAverageHalfSize+1)&& (2*slidingAverageHalfSize+1 - spffalse) > 0)spfactors_ave[index] /= (double)(2*slidingAverageHalfSize+1 - spffalse); else index--;
				index++;
			}
			int curr_tracklen = 0;
			double[] displacement = new double[nFrames-2*slidingAverageHalfSize]; 
			double[] ave_displacement = new double[nFrames-2*slidingAverageHalfSize]; 
			for (int b = 0; b<index; b++){displacement[b] = Math.sqrt(params_ave[10][b]-parameters[10][i][timeshift+b]);}
			for (int b = 2*slidingAverageHalfSize+1; b<index; b++){
				for (int g = 0; g < 2*slidingAverageHalfSize+1; g++){ave_displacement[b] += Math.sqrt((params_ave[10][b-g]-parameters[10][i][timeshift+b-g])*(params_ave[10][b-g]-parameters[10][i][timeshift+b-g]) + (params_ave[11][b-g]-parameters[11][i][timeshift+b-g])*(params_ave[11][b-g]-parameters[11][i][timeshift+b-g]))/(double)(2*slidingAverageHalfSize+1);}
//				IJ.log(" " + (b+timeshift)+ " " + (ave_displacement[b]*pixelSize/1000.0)); 
			}
			for (int b = 0; b<index; b++){
				totalTimeParasite++;
				if(vel_ave[b]<=velLimit1){attachedTimeParasite++;belongflag = 0;cnt[0]++;}
				else if((vel_ave[b] > (Math.abs(angles_ave[b])*sepfactor2 + velLimit2))&& patchglidersExist == true){patchGlidingTimeParasite++;belongflag = 4;cnt[4]++;}
				else if((angles_ave[b]>0) &&(vel_ave[b] > (Math.abs(angles_abs_ave[b])*sepfactor))){
					if(patchglidersExist == true && (ave_displacement[b]*pixelSize/1000.0/params_ave[0][b] < displacementLimit)&&(b > 2*slidingAverageHalfSize)&& (params_ave[0][b]>velLimit2)){patchGlidingTimeParasite++;belongflag = 4;cnt[4]++;IJ.log("NUM " + i + " 1.displacement "+ave_displacement[b]*pixelSize/1000.0/params_ave[0][b] + " b " + b + " ave_vel " + params_ave[0][b] + " checked"); }
//check if the parasite is a "type2 patchglider" - one changing its direction slowly. if it moves more then 10 seconds then its tracked as mover.
					else {cwTimeParasite++;belongflag = 2;cnt[2]++;}
				}
				else if((angles_ave[b]<0) &&(vel_ave[b] > (Math.abs(angles_abs_ave[b])*sepfactor))){
					if(patchglidersExist == true && (ave_displacement[b]*pixelSize/1000.0/params_ave[0][b] < displacementLimit)&&(b > 2*slidingAverageHalfSize)&& (params_ave[0][b]>velLimit2)){patchGlidingTimeParasite++;belongflag = 4;cnt[4]++;IJ.log("NUM " + i + " 2.displacement "+ave_displacement[b]*pixelSize/1000.0/params_ave[0][b] + " b " + b + " ave_vel " + params_ave[0][b] + " checked");}
					else {ccwTimeParasite++;belongflag = 3;cnt[3]++;}
				}
				else {waverTimeParasite++; belongflag = 1;cnt[1]++;}
				for (int k1 = 0; k1 < paramnumber; k1++)parameters_movingtypes[k1][belongflag][cnt[belongflag]-1] = params_ave[k1][b]; 
				angles_abs_movingtypes[belongflag][cnt[belongflag]-1] = angles_abs_ave[b];
				vels_movingtypes[belongflag][cnt[belongflag]-1] = vel_ave[b];
				spfactors_movingtypes[belongflag][cnt[belongflag]-1] = spfactors_ave[b];
				lengths_movingtypes[belongflag][cnt[belongflag]-1] = length_ave[b];
				pixelnums_movingtypes[belongflag][cnt[belongflag]-1] = pixelsize_ave[b];
				if(b>0)stateTransmissions[oldbelongflag][belongflag]++;
				if(b >0 && oldbelongflag != belongflag && curr_tracklen*timeLapse > 5)stateTransmissionsCondition[oldbelongflag][belongflag]++;
				if(b >0 && oldbelongflag == belongflag){curr_tracklen++;}
				else {statelengths[oldbelongflag][statescnt[oldbelongflag]] = curr_tracklen*timeLapse;statescnt[oldbelongflag]++;curr_tracklen = 0;}
				if(b == (index-1) && oldbelongflag == belongflag){statelengths[oldbelongflag][statescnt[oldbelongflag]] = curr_tracklen*timeLapse;statescnt[oldbelongflag]++;curr_tracklen = 0;}
				oldbelongflag = belongflag; 
				states[b] = belongflag; 
			}
//probabilities of parasite beeing in the same state after time of t=t
			int finishflag;double tmp; 
			for (int len = 0; len<index-5; len++){finishflag = 0; 
				for (j = 0; j<5; j++)
					if(states[len] == j)
						for (int in = len; in <index; in++){
							sameStateCnt[j][in-len]++; 
							tmp = Math.sqrt(Math.pow(params_ave[10][len]-params_ave[10][in],2)+Math.pow(params_ave[11][len]-params_ave[11][in],2)); 							if(states[in] == j && tmp<100)sameStateProbability[j][in-len]++; 
							if(tmp<100)stateDisplacement[j][in-len] += tmp;

							if(states[len]!= states[j])finishflag = 1; 
							if(finishflag == 0){
								if(tmp<100)stateDisplacementConditional[j][in-len] += Math.sqrt(Math.pow(params_ave[10][len]-params_ave[10][in],2)+Math.pow(params_ave[11][len]-params_ave[11][in],2)); 
								if(tmp<100)sameStateCntConditional[j][in-len]++;
							}
						}
			}

			dos.write("Track"+ i + " Total_time(frames) " +totalTimeParasite + " Attached " + attachedTimeParasite + " Waving " +waverTimeParasite + " CWmover " + cwTimeParasite + " CCWmover " + ccwTimeParasite + " Patchgliding " + patchGlidingTimeParasite+ "\n"); 
			totalTime+=totalTimeParasite; waverTime+=waverTimeParasite; ccwTime += ccwTimeParasite; cwTime+= cwTimeParasite; attachedTime+=attachedTimeParasite; patchGlidingTime +=patchGlidingTimeParasite;
			for (j = 0; j<index; j++)dos.write(""+states[j] + " "); 
			dos.write("\n"); 
		}
		int i; j =0;
		if(bDisplayMotParam == true){
			dos.write("All attached points as list" + "\n");
			for (i = 0; i<cnt[j]; i++)dos.write(" vel " + parameters_movingtypes[0][j][i] + " angle_m " + parameters_movingtypes[1][j][i] + " angle_a " + parameters_movingtypes[2][j][i] + " spfactor " + parameters_movingtypes[3][j][i] + " para_length " + parameters_movingtypes[4][j][i] +" pixnumber " + parameters_movingtypes[5][j][i] + " curvature " + parameters_movingtypes[6][j][i] + " perimeter " + parameters_movingtypes[7][j][i] + " time " + parameters_movingtypes[8][j][i] + " moving_dir " + parameters_movingtypes[12][j][i] + " track_number " + parameters_movingtypes[13][j][i] + "\n");
			dos.write("All waving points as list" + "\n");j++;
			for (i = 0; i<cnt[j]; i++)dos.write(" vel " + parameters_movingtypes[0][j][i] + " angle_m " + parameters_movingtypes[1][j][i] + " angle_a " + parameters_movingtypes[2][j][i] + " spfactor " + parameters_movingtypes[3][j][i] + " para_length " + parameters_movingtypes[4][j][i] +" pixnumber " + parameters_movingtypes[5][j][i] + " curvature " + parameters_movingtypes[6][j][i] + " perimeter " + parameters_movingtypes[7][j][i] + " time " + parameters_movingtypes[8][j][i] + " moving_dir " + parameters_movingtypes[12][j][i] + " track_number " + parameters_movingtypes[13][j][i] + "\n");
			dos.write("All clockwaizzemoving points as list" + "\n");j++;
			for (i = 0; i<cnt[j]; i++)dos.write(" vel " + parameters_movingtypes[0][j][i] + " angle_m " + parameters_movingtypes[1][j][i] + " angle_a " + parameters_movingtypes[2][j][i] + " spfactor " + parameters_movingtypes[3][j][i] + " para_length " + parameters_movingtypes[4][j][i] +" pixnumber " + parameters_movingtypes[5][j][i] + " curvature " + parameters_movingtypes[6][j][i] + " perimeter " + parameters_movingtypes[7][j][i] + " time " + parameters_movingtypes[8][j][i] + " moving_dir " + parameters_movingtypes[12][j][i] + " track_number " + parameters_movingtypes[13][j][i] + "\n");
			dos.write("All CClockwaizzemoving points as list" + "\n");j++;
			for (i = 0; i<cnt[j]; i++)dos.write(" vel " + parameters_movingtypes[0][j][i] + " angle_m " + parameters_movingtypes[1][j][i] + " angle_a " + parameters_movingtypes[2][j][i] + " spfactor " + parameters_movingtypes[3][j][i] + " para_length " + parameters_movingtypes[4][j][i] +" pixnumber " + parameters_movingtypes[5][j][i] + " curvature " + parameters_movingtypes[6][j][i] + " perimeter " + parameters_movingtypes[7][j][i] + " time " + parameters_movingtypes[8][j][i] + " moving_dir " + parameters_movingtypes[12][j][i] + " track_number " + parameters_movingtypes[13][j][i] + "\n");
			dos.write("All PatchGliding points as list" + "\n");j++;
			for (i = 0; i<cnt[j]; i++)dos.write(" vel " + parameters_movingtypes[0][j][i] + " angle_m " + parameters_movingtypes[1][j][i] + " angle_a " + parameters_movingtypes[2][j][i] + " spfactor " + parameters_movingtypes[3][j][i] + " para_length " + parameters_movingtypes[4][j][i] +" pixnumber " + parameters_movingtypes[5][j][i] + " curvature " + parameters_movingtypes[6][j][i] + " perimeter " + parameters_movingtypes[7][j][i] + " time " + parameters_movingtypes[8][j][i] + " moving_dir " + parameters_movingtypes[12][j][i] + " track_number " + parameters_movingtypes[13][j][i] + "\n");
		}
		double means_angles[][] = new double[5][2];//second index: 0-mean value, 1 - standard deviation; 
		double means_spf[][] = new double[5][2];
		double means_lengths[][] = new double[5][2];
		double means_vels[][] = new double[5][2];
		double means_pixelsize[][] = new double[5][2];
		double means_absangle[][] = new double[5][2];
		double means_parameters[][][] = new double[paramnumber][5][2]; 
		for (j = 0; j<5; j++){
			means_absangle[j][0] = mean_a(angles_abs_movingtypes[j], cnt[j]); 
			means_absangle[j][1] = stddev_a(angles_abs_movingtypes[j], cnt[j], means_absangle[j][0]);
			means_spf[j][0] = mean_a(spfactors_movingtypes[j], cnt[j]); 
			means_spf[j][1] = stddev_a(spfactors_movingtypes[j], cnt[j], means_spf[j][0]);
			means_lengths[j][0] = mean_a(lengths_movingtypes[j], cnt[j]); 
			means_lengths[j][1] = stddev_a(lengths_movingtypes[j], cnt[j], means_lengths[j][0]);
			means_vels[j][0] = mean_a(vels_movingtypes[j], cnt[j]); 
			means_vels[j][1] = stddev_a(vels_movingtypes[j], cnt[j], means_vels[j][0]);
			means_pixelsize[j][0] = mean_a(pixelnums_movingtypes[j], cnt[j]); 
			means_pixelsize[j][1] = stddev_a(pixelnums_movingtypes[j], cnt[j], means_pixelsize[j][0]);
			for (int k1 = 0; k1< paramnumber; k1++){
				means_parameters[k1][j][0] = mean_a(parameters_movingtypes[k1][j], cnt[j]); 
				means_parameters[k1][j][1] = stddev_a(parameters_movingtypes[k1][j], cnt[j], means_parameters[k1][j][0]);
			}
		}

		dos.write("overall statistics for all parasites(sliding average of " + (2*slidingAverageHalfSize+1)+" frames)" + "\n"); 
		dos.write("Total"+ " Total_time(frames) " +totalTime + " Attached " + attachedTime + " Waving " +waverTime + " CWmover " + cwTime + " CCWmover " + ccwTime + " PatchGliding " + patchGlidingTime + "\n"); 
		j = 0; 

		dos.write("Attached(meanval+sttdev): velocity " + means_parameters[0][j][0] + " " + means_parameters[0][j][1] + " angle_moving " + means_parameters[1][j][0] + " " + means_parameters[1][j][1] + " angle_orientation " + means_parameters[2][j][0] + " " + means_parameters[2][j][1] + " spfactor " + means_parameters[3][j][0] + " " + means_parameters[3][j][1] + " length " + means_parameters[4][j][0] + " " + means_parameters[4][j][1] + " pixelsize " + means_parameters[5][j][0] + " " + means_parameters[5][j][1] + " curvature "+ means_parameters[6][j][0] + " " + means_parameters[6][j][1] + " perimeter " + means_parameters[7][j][0] + " " + means_parameters[7][j][1] + " time " + means_parameters[8][j][0] + " " + means_parameters[8][j][1] + " abs_angle " + means_absangle[j][0] + " " + means_absangle[j][1] + "\n");j++;
		dos.write("Waving(meanval+sttdev): velocity " + means_parameters[0][j][0] + " " + means_parameters[0][j][1] + " angle_moving " + means_parameters[1][j][0] + " " + means_parameters[1][j][1] + " angle_orientation " + means_parameters[2][j][0] + " " + means_parameters[2][j][1] + " spfactor " + means_parameters[3][j][0] + " " + means_parameters[3][j][1] + " length " + means_parameters[4][j][0] + " " + means_parameters[4][j][1] + " pixelsize " + means_parameters[5][j][0] + " " + means_parameters[5][j][1] + " curvature "+ means_parameters[6][j][0] + " " + means_parameters[6][j][1] + " perimeter " + means_parameters[7][j][0] + " " + means_parameters[7][j][1] + " time " + means_parameters[8][j][0] + " " + means_parameters[8][j][1] + " abs_angle " + means_absangle[j][0] + " " + means_absangle[j][1] + "\n");j++;
		dos.write("ClockwiseM(meanval+sttdev): velocity " + means_parameters[0][j][0] + " " + means_parameters[0][j][1] + " angle_moving " + means_parameters[1][j][0] + " " + means_parameters[1][j][1] + " angle_orientation " + means_parameters[2][j][0] + " " + means_parameters[2][j][1] + " spfactor " + means_parameters[3][j][0] + " " + means_parameters[3][j][1] + " length " + means_parameters[4][j][0] + " " + means_parameters[4][j][1] + " pixelsize " + means_parameters[5][j][0] + " " + means_parameters[5][j][1] + " curvature "+ means_parameters[6][j][0] + " " + means_parameters[6][j][1] + " perimeter " + means_parameters[7][j][0] + " " + means_parameters[7][j][1] + " time " + means_parameters[8][j][0] + " " + means_parameters[8][j][1] + " abs_angle " + means_absangle[j][0] + " " + means_absangle[j][1] + "\n");j++;
		dos.write("CounterClockwiseM(meanval+sttdev): velocity " + means_parameters[0][j][0] + " " + means_parameters[0][j][1] + " angle_moving " + means_parameters[1][j][0] + " " + means_parameters[1][j][1] + " angle_orientation " + means_parameters[2][j][0] + " " + means_parameters[2][j][1] + " spfactor " + means_parameters[3][j][0] + " " + means_parameters[3][j][1] + " length " + means_parameters[4][j][0] + " " + means_parameters[4][j][1] + " pixelsize " + means_parameters[5][j][0] + " " + means_parameters[5][j][1] + " curvature "+ means_parameters[6][j][0] + " " + means_parameters[6][j][1] + " perimeter " + means_parameters[7][j][0] + " " + means_parameters[7][j][1] + " time " + means_parameters[8][j][0] + " " + means_parameters[8][j][1] + " abs_angle " + means_absangle[j][0] + " " + means_absangle[j][1] + "\n");j++;
		dos.write("PatchGliders(meanval+sttdev): velocity " + means_parameters[0][j][0] + " " + means_parameters[0][j][1] + " angle_moving " + means_parameters[1][j][0] + " " + means_parameters[1][j][1] + " angle_orientation " + means_parameters[2][j][0] + " " + means_parameters[2][j][1] + " spfactor " + means_parameters[3][j][0] + " " + means_parameters[3][j][1] + " length " + means_parameters[4][j][0] + " " + means_parameters[4][j][1] + " pixelsize " + means_parameters[5][j][0] + " " + means_parameters[5][j][1] + " curvature "+ means_parameters[6][j][0] + " " + means_parameters[6][j][1] + " perimeter " + means_parameters[7][j][0] + " " + means_parameters[7][j][1] + " time " + means_parameters[8][j][0] + " " + means_parameters[8][j][1] + " abs_angle " + means_absangle[j][0] + " " + means_absangle[j][1] + "\n");j++;
		dos.write("Table_of_transmissions_between_states \n"); 
		for (j = 0; j<5; j++)dos.write(stateTransmissions[j][0] + " " + stateTransmissions[j][1] + " " + stateTransmissions[j][2] + " " + stateTransmissions[j][3] +" " + stateTransmissions[j][4] + "\n");
		dos.write("Table_of_Conditional_transmissions_between_states(counts_changes_if_previous_state_lasted_over_5sec) \n"); 
		for (j = 0; j<5; j++)dos.write(stateTransmissionsCondition[j][0] + " " + stateTransmissionsCondition[j][1] + " " + stateTransmissionsCondition[j][2] + " " + stateTransmissionsCondition[j][3] +" " + stateTransmissionsCondition[j][4] + "\n");
		double aveStateLen[] = new double[5]; 
		for (j = 0; j< 5; j++){
			for (i = 0; i<statescnt[j]; i++)
				aveStateLen[j] += statelengths[j][i];
			aveStateLen[j] /= (double)statescnt[j];
		}j = 0;
		dos.write("list_of_durations_for_all_states:Attached N= " + statescnt[j] + " Ave= " + aveStateLen[j] + " Median= " + median_a(statelengths[j], statescnt[j]) + " StdDev= " + stddev_a(statelengths[j], statescnt[j], aveStateLen[j])+ " ConditionalAverage(if_dur>5frames)= " +mean_cond(statelengths[j], statescnt[j],5)+ "\n");
//		for (i = 0; i<statescnt[j]; i++)dos.write(" "+statelengths[j][i] + "\n"); 
		j++;
		dos.write("list_of_durations_for_all_states:Waving N= " + statescnt[j] + " Ave= " + aveStateLen[j] + " Median= " + median_a(statelengths[j], statescnt[j]) + " StdDev= " + stddev_a(statelengths[j], statescnt[j], aveStateLen[j])+ " ConditionalAverage(if_dur>5frames)= " +mean_cond(statelengths[j], statescnt[j],5)+ "\n");
//		for (i = 0; i<statescnt[j]; i++)dos.write(" "+statelengths[j][i] + "\n"); 
		j++;
		dos.write("list_of_durations_for_all_states:CwizzeMover N= " + statescnt[j] + " Ave= " + aveStateLen[j] + " Median= " + median_a(statelengths[j], statescnt[j]) + " StdDev= " + stddev_a(statelengths[j], statescnt[j], aveStateLen[j])+ " ConditionalAverage(if_dur>5frames)= " +mean_cond(statelengths[j], statescnt[j],5)+ "\n");
//		for (i = 0; i<statescnt[j]; i++)dos.write(" "+statelengths[j][i] + "\n"); 
		j++;
		dos.write("list_of_durations_for_all_states:CCwizzeMover N= " + statescnt[j] + " Ave= " + aveStateLen[j] + " Median= " + median_a(statelengths[j], statescnt[j]) + " StdDev= " + stddev_a(statelengths[j], statescnt[j], aveStateLen[j])+ " ConditionalAverage(if_dur>5frames)= " +mean_cond(statelengths[j], statescnt[j],5)+ "\n");
//		for (i = 0; i<statescnt[j]; i++)dos.write(" "+statelengths[j][i] + "\n"); 
		j++;
		dos.write("list_of_durations_for_all_states:PatchGlider N= " + statescnt[j] + " Ave= " + aveStateLen[j] + " Median= " + median_a(statelengths[j], statescnt[j]) + " StdDev= " + stddev_a(statelengths[j], statescnt[j], aveStateLen[j])+ " ConditionalAverage(if_dur>5frames)= " +mean_cond(statelengths[j], statescnt[j],5)+ "\n");

		dos.close();

//end of TRY loop
		}
		catch (IOException e) {
			if (filename != null)
				IJ.error ("An error occurred writing the file. \n \n " + e);
		}

		// total length traversed
		if (bShowPathLengths && !writefile) {
			double[] lengths = new double[trackCount];
			double[] distances = new double[trackCount];
			int[] frames = new int[trackCount];
			double x1, y1, x2, y2;
			int trackNr=0,i;
			int displayTrackNr=0;
			for (ListIterator iT=theTracks.listIterator(); iT.hasNext();) {
				trackNr++;
				List bTrack=(ArrayList) iT.next();
				if (bTrack.size() >= minTrackLength) {
					displayTrackNr++;
					ListIterator jT=bTrack.listIterator();
					particle oldParticle=(particle) jT.next();
					particle firstParticle=new particle();
					firstParticle.copy(oldParticle);
					frames[displayTrackNr-1]=bTrack.size();
					for (;jT.hasNext();) {
						particle newParticle=(particle) jT.next();
						lengths[displayTrackNr-1]+=Math.sqrt(sqr(oldParticle.x-newParticle.x)+sqr(oldParticle.y-newParticle.y));
						oldParticle=newParticle;
					}
					distances[displayTrackNr-1]=Math.sqrt(sqr(oldParticle.x-firstParticle.x)+sqr(oldParticle.y-firstParticle.y));
				}
			}
			IJ.write("");
			IJ.write("Track \tLength\tDistance traveled\tNr of Frames");
			for (i=0; i<displayTrackNr; i++) {
				String str = "" + (i+1) + ":\t" + (float)lengths[i] + "\t" + (float)distances[i] + "\t" + (int)frames[i];
				IJ.write(str);
			}
		}


		// 'map' of tracks
		if (bShowPaths) {
			if (imp.getCalibration().scaled()) {
				IJ.showMessage("MultiTracker", "Cannot display paths if image is spatially calibrated");
				return;
			}
			ImageProcessor ip = new ByteProcessor(imp.getWidth(), imp.getHeight());
			ip.setColor(Color.white);
			ip.fill();
			trackCount=0;
			int color;
			for (ListIterator iT=theTracks.listIterator();iT.hasNext();) {
				trackCount++;
				List bTrack=(ArrayList) iT.next();
				if (bTrack.size() >= minTrackLength) {
					ListIterator jT=bTrack.listIterator();
					particle oldParticle=(particle) jT.next();
					for (;jT.hasNext();) {
						particle newParticle=(particle) jT.next();
						color =Math.min(trackCount+1,254);
						ip.setValue(color);
						ip.moveTo((int)oldParticle.x, (int)oldParticle.y);
						ip.lineTo((int)newParticle.x, (int)newParticle.y);
						oldParticle=newParticle;
					}
				}
			}
			new ImagePlus("Paths", ip).show();
		}
	}

	// Utility functions
	double sqr(double n) {return n*n;}
	
	int doOffset (int center, int maxSize, int displacement) {
		if ((center - displacement) < 2*displacement) {
			return (center + 4*displacement);
		}
		else {
			return (center - displacement);
		}
	}

 	double s2d(String s) {
		Double d;
		try {d = new Double(s);}
		catch (NumberFormatException e) {d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return(0.0);
	}
	public double segmentation(int[] pixels, int W, int H, int x,int y, int bright, int mnsize, int mxsize, int i1, int i2){
//	public double segmentation(ImageProcessor imp1,int x,int y, int bright, int mnsize, int mxsize){
		int b,i=0,j=0,flag=1, t, closeness = 1; 
		parObject obj = new parObject();
		obj.addp(x,y,(double)pixels[x+y*W]);
		while((flag == 1) && (obj.n<(obj.maxpixnum-2))){
			flag = 0;
			for(b = 0; b < obj.n; b++)
				for(i = (int)obj.x[b] - closeness; i < obj.x[b] +1 + closeness; i++)
					for(j = (int)obj.y[b] - closeness; j < obj.y[b] +1 + closeness; j++){
//						dos.write("xy " + obj.x[b] + " " + obj.y[b] + " " + pixels[i+j*W]);
						if( (i+j*W) >= 0 && (i+j*W) < W*H)
						if((pixels[i + j*W] == bright)&&(obj.isin(i,j) == 0)){
							obj.addp(i,j,(double)pixels[i + j*W]);flag=1;
//							dos.write("added xy "+ i + " " + j + " intens " + pixels[i + j*W]); 
//							dos.write("coords "+ obj.avex()+ " " + obj.avey()+ " " + obj.wavex()+ " " + obj.wavey()+ " " + obj.n + " "+ obj.sumint());
//							dos.write("correls "+ pixels[i+j*W]+ " " + obj.intensity[obj.n-1]+ " " + obj.sumint()+ " " + obj.corrxyweight()+ " " + obj.corrxy());
						}
					}
			if(obj.n == (obj.maxpixnum-3)){
//				dos.write("Object too big - overload! " + x + " " + y + " " + bright + " " + pixels[x+y*W]);
//				dos.write("pixnum " + obj.n + " avex " + obj.avex() + " avey "+ obj.avey() + " anlgle " + obj.princ_angle() + " anlgle2 " + obj.princ_angle2()+ " sumint " + obj.sumint()+ " min/max X " + obj.minX() + " " + obj.maxX() + " min/max Y " + obj.minY()+ " "+ obj.maxY());
				//for (i = 0; i<obj.n; i++)dos.write(""+ i+" "+(int)obj.x[i]+" "+(int)obj.y[i]+" "+(int)obj.intensity[i] + " surroundings " + pixels[(int)obj.x[i]-1 + W*(int)obj.y[i]] + " "+ pixels[(int)obj.x[i] + W*((int)obj.y[i]-1)] + " "+ pixels[(int)obj.x[i]+1 + W*(int)obj.y[i]] + " "+ pixels[(int)obj.x[i] + W*((int)obj.y[i]+1)]);
				return -600;
			}
		} 
//		dos.write("pixnum " + obj.n + " avex " + obj.avex() + " avey "+ obj.avey() + " anlgle " + obj.princ_angle() + " anlgle2 " + obj.princ_angle2()+ " sumint " + obj.sumint()+ " min/max X " + obj.minX() + " " + obj.maxX() + " min/max Y " + obj.minY()+ " "+ obj.maxY());
//		for (i = 0; i<obj.n; i++)dos.write("point " + i + " x "+ obj.x[i] + " y " + obj.y[i] + " value " + obj.intensity[i]);  
		parObject rtpar = new parObject();
		rtpar = obj.rotate(45 + obj.princ_angle());
		spfactors[i1][i2] = Math.min(Math.max(Math.abs(rtpar.corrxy()),Math.abs(obj.corrxy())), 1);
		plengths[i1][i2] = obj.mlen(); 
		pixnumbers[i1][i2] = obj.n; 
		perimeter[i1][i2] = obj.perimeter();
		if(bCalculateCurvature == true){
			curvatures[i1][i2] = obj.curvature(); //obj.curvature(); 
		}else curvatures[i1][i2] = 0; 

		if((obj.n < mxsize)&&(obj.n > mnsize)){
			return obj.princ_angle();
		}
		return -550;//obj.princ_angle();
	}
	double mean_a(double a[], int len){
		double sum = 0; 
		for (int i = 0; i<len; i++)sum+=a[i]; 
		return sum/(double)len; 
	}
	double mean_cond(double a[], int len, double limit){
		double sum = 0, cnt = 0; 
		for (int i = 0; i<len; i++)if(a[i] > limit){sum+=a[i]; cnt++;}
		return sum/(double)cnt; 
	}
	double stddev_a(double a[], int len, double mean){
		double stddev = 0; 
		for (int i = 0; i<len; i++)stddev+=Math.abs(a[i] - mean); 
		return stddev/(double)(len-1); 		
	}
	double median_a(double a[], int len){
		double temp; 
		for (int i = 0; i<len; i++)
			for (int j = 0; j < i; j++)
				if(a[i]<a[j]){
					temp = a[i]; a[i] = a[j]; a[j] = temp; 
				}
		return a[(int)(len/2.0)];
	}
}
