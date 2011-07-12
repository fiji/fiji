import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

/** Time space plot is created. This plugin requires 
     a stack of Gray scale images and at least one line selection. 
     This line selection can also be made with the plugin "MultipleOverlay_.class"
    
     version 11.08.2006	bugfix
     version 21.08.2006 bugfix maxlength
     version 05.02.2007 bug fix line235 
     version 28.04.2008 works with 8-bit, 16 bit and 32 bit Grey scale images  
    
    please send comments to seitz@embl.de
*/

public class MultipleKymograph_ implements PlugIn {
	double [] profile;
	
	
	public void run(String arg){ 
	
       	String wintitle;
		
		boolean storeexist=false;
		int storeID=0,proflength;
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageProcessor ip = imp.getProcessor();
		int numStacks = imp.getStackSize();
		ImagePlus test, store;
		ImageProcessor store_ip;
		
		int wincount=WindowManager.getWindowCount();
		
		int [] idlist =WindowManager.getIDList();
		for (int i=0;i<wincount;i++){
			test=WindowManager.getImage(idlist[i]);
			wintitle=test.getTitle();
			

			if (wintitle.equals ("Storage") || wintitle.equals( "Storage.tif"))  {
				storeexist=true;
				storeID=idlist[i];
			}
		}
		
		
		int bd=imp.getBitDepth();
		if (numStacks==1){
			IJ.error("Sorry! This Plugin only works on Stacks");
			return;
		}
		Roi roi = imp.getRoi();;
		int roiType=0;
		if (!(roi==null)){
			roiType = roi.getType();
			if (!(roiType==Roi.LINE || roiType==Roi.POLYLINE )) {
            				IJ.error("Line or rectangular selection required.");
            				return;
        			}		
		}
		else if (!storeexist){
			IJ.error("Selection required");
			return;
		}
		

    	String sPrompt = "Linewidth ";
        int linewidth = (int)IJ.getNumber(sPrompt,1);
        
        if(linewidth==IJ.CANCELED) return;
		
        if(linewidth>=numStacks){
			
			IJ.error("Sorry, this makes no sense");
			return;
		}
		
		if (linewidth%2==0){
			IJ.error("Must enter an odd number");
			return;
		}
		if (storeexist) {
			IJ.showMessage("Line from Store");
			store=WindowManager.getImage(storeID);
			store_ip=store.getProcessor();
			
			multipleKymo(imp, ip, store_ip, numStacks,linewidth);
			return;
		}
		
		if (roiType==Roi.LINE) {
          	profile = ((Line)roi).getPixels();
		} 
		else{
			 profile = getIrregularProfile(roi, ip,0);
		}

		proflength=profile.length;
		double[]average=sKymo(imp, ip, roi,linewidth, proflength);
		FloatProcessor nip=new FloatProcessor (proflength,imp.getStackSize(),average);
		ImagePlus kymo=new ImagePlus("Kymograph",nip);
		kymo.show();
	}
	public double [] sKymo(ImagePlus imp, ImageProcessor ip, Roi roi, int linewidth, int proflength){
	    
		int numStacks=imp.getStackSize();	
		int dimension = proflength*numStacks;
		double [] sum = new double [dimension];
		
		int roiType = roi.getType();
		int shift=0;
		int count=0;
		
		for (int i=1; i<=numStacks; i++) {
			imp.setSlice(i);
				
			for (int ii=0;ii<linewidth;ii++){
				shift=(-1*(linewidth-1)/2)+ii;
								
				if (roiType==Roi.LINE) {
            					profile = getProfile(roi,ip,shift);
					
				} 
				else {
					profile = getIrregularProfile(roi, ip,shift);
				}
				for (int j=0;j<proflength;j++){
					count = (i-1)*proflength+j;
					sum[count]+=(profile[j]/linewidth);
					
				}
			}
			
		}
		
		return sum;
	}
	

	public void multipleKymo (ImagePlus imp, ImageProcessor ip, ImageProcessor store_ip, int numStacks, int linewidth){
		
		
		short [] zahl =(short[])store_ip.getPixels();
		int shift=0;
		
		for (int i=1;i<=(int)zahl[0];i++){
			profile = getStoreProfile(store_ip, ip, 0, i);	
			int proflength=profile.length;
		  	                        
			int dimension= numStacks*proflength;
			
			double sum []=new double [dimension];     		
			
			for (int j=1; j<=numStacks; j++) {
				imp.setSlice(j);
				
				for (int ii=0;ii<linewidth;ii++){
					shift=(-1*(linewidth-1)/2)+ii;
					profile = getStoreProfile(store_ip,ip,shift,i);
					for (int k=0;k<proflength;k++){
						int count1=((j-1)*proflength+k);
						sum[count1]+=(profile[k]/linewidth);
					}
				}
													
			}
		
		FloatProcessor nip=new FloatProcessor (proflength,imp.getStackSize(),sum);
		ImagePlus kymo=new ImagePlus("Kymograph"+i,nip);
		kymo.show();
		}
		
	}	  
          


			
		
     public double[]getStoreProfile(ImageProcessor store_ip, ImageProcessor ip, int shift, int numline){
		
		int [] x=new int[500];
		int [] y=new int[500];
		int anfang,stop;
		short [] pixels = (short[]) store_ip.getPixels();
		int n=pixels[0];
		int num=1;
		for (int i=0;i<n;i++){
			
			anfang=num+1;
			stop=(int)(pixels[num]);
			num+=((pixels[num]*2)+1);
			if (i+1==numline){
				n=pixels[anfang-1];
				for (int j=0;j<stop;j++){
					x[j]=pixels[anfang+j];
					y[j]=pixels[anfang+stop+j];
				}
			}
		}
	
		for (int i=0;i<=n;i++){
			x[i]+=shift;
			y[i]+=shift;
		}
        		int xbase = 0;
        		int ybase = 0;
        		double length = 0.0;
        		double segmentLength;
        		int xdelta, ydelta;
        		double[] segmentLengths = new double[n];
        		int[] dx = new int[n];
        		int[] dy = new int[n];
        		for (int i=0; i<(n-1); i++) {
            			xdelta = x[i+1] - x[i];
            			ydelta = y[i+1] - y[i];
            			segmentLength = Math.sqrt(xdelta*xdelta+ydelta*ydelta);
            			length += segmentLength;
            			segmentLengths[i] = segmentLength;
            			dx[i] = xdelta;
            			dy[i] = ydelta;
        		}
        	double[] values = new double[(int)length];
        	double leftOver = 1.0;
        	double distance = 0.0;
        	int index;
        	//double oldx=xbase, oldy=ybase;
        	for (int i=0; i<n; i++) {
            		double len = segmentLengths[i];
            		if (len==0.0)
                		continue;
            		double xinc = dx[i]/len;
            		double yinc = dy[i]/len;
            		double start = 1.0-leftOver;
            		double rx = xbase+x[i]+start*xinc;
            		double ry = ybase+y[i]+start*yinc;
            		double len2 = len - start;
            		int n2 = (int)len2;
           			//double d=0;;
            		//IJ.write("new segment: "+IJ.d2s(xinc)+" "+IJ.d2s(yinc)+" "+IJ.d2s(len)+" "+IJ.d2s(len2)+" "+IJ.d2s(n2)+" "+IJ.d2s(leftOver));
            		for (int j=0; j<=n2; j++) {
                			index = (int)distance+j;
                			if (index<values.length)
                   			values[index] = ip.getInterpolatedValue(rx, ry);
              				 //d = Math.sqrt((rx-oldx)*(rx-oldx)+(ry-oldy)*(ry-oldy));
                				//IJ.write(IJ.d2s(rx)+"    "+IJ.d2s(ry)+"    "+IJ.d2s(d));
                				//IJ.log(IJ.d2s(rx)+"    "+IJ.d2s(ry));
                				//oldx = rx; oldy = ry;
                				rx += xinc;
             	   			ry += yinc;
            		}
            	distance += len;
            	leftOver = len2 - n2;
        	}

	return values;


    
	}

 	double[] getIrregularProfile(Roi roi, ImageProcessor ip,int shift) {
        		int n = ((PolygonRoi)roi).getNCoordinates();
        		int[] x = ((PolygonRoi)roi).getXCoordinates();
        		int[] y = ((PolygonRoi)roi).getYCoordinates();
		
		for (int i=0;i<n;i++){
			x[i]+=shift;
			y[i]+=shift;
		}
        		Rectangle r = roi.getBoundingRect();
        		int xbase = r.x;
        		int ybase = r.y;
        		double length = 0.0;
        		double segmentLength;
        		int xdelta, ydelta;
        		double[] segmentLengths = new double[n];
        		int[] dx = new int[n];
        		int[] dy = new int[n];
        		for (int i=0; i<(n-1); i++) {
            			xdelta = x[i+1] - x[i];
            			ydelta = y[i+1] - y[i];
            			segmentLength = Math.sqrt(xdelta*xdelta+ydelta*ydelta);
            			length += segmentLength;
            			segmentLengths[i] = segmentLength;
            			dx[i] = xdelta;
            			dy[i] = ydelta;
        		}
        	double[] values = new double[(int)length];
        	double leftOver = 1.0;
        	double distance = 0.0;
        	int index;
        	//double oldx=xbase, oldy=ybase;
        	for (int i=0; i<n; i++) {
            		double len = segmentLengths[i];
            		if (len==0.0)
                		continue;
            		double xinc = dx[i]/len;
            		double yinc = dy[i]/len;
            		double start = 1.0-leftOver;
            		double rx = xbase+x[i]+start*xinc;
            		double ry = ybase+y[i]+start*yinc;
            		double len2 = len - start;
            		int n2 = (int)len2;
           		 	//double d=0;;
            		//IJ.write("new segment: "+IJ.d2s(xinc)+" "+IJ.d2s(yinc)+" "+IJ.d2s(len)+" "+IJ.d2s(len2)+" "+IJ.d2s(n2)+" "+IJ.d2s(leftOver));
            		for (int j=0; j<=n2; j++) {
                			index = (int)distance+j;
                			if (index<values.length)
                   			values[index] = ip.getInterpolatedValue(rx, ry);
              				 //d = Math.sqrt((rx-oldx)*(rx-oldx)+(ry-oldy)*(ry-oldy));
                				//IJ.write(IJ.d2s(rx)+"    "+IJ.d2s(ry)+"    "+IJ.d2s(d));
                				//IJ.log(IJ.d2s(rx)+"    "+IJ.d2s(ry));
                				//oldx = rx; oldy = ry;
                				rx += xinc;
             	   			ry += yinc;
            		}
            	distance += len;
            	leftOver = len2 - n2;
        	}

	return values;


    
	}
	double[]getProfile(Roi roi,ImageProcessor ip, int shift){
	double [] values;
	
	int x1=((Line)roi).x1;
	int x2=((Line)roi).x2;
	int y1=((Line)roi).y1;
	int y2=((Line)roi).y2;

	((Line)roi).x1=x1+shift;
	((Line)roi).x2=x2+shift;
	((Line)roi).y1=y1+shift;
	((Line)roi).y2=y2+shift;
	
	values=((Line)roi).getPixels();
	((Line)roi).x1=x1;
	((Line)roi).x2=x2;
	((Line)roi).y1=y1;
	((Line)roi).y2=y2;
	
	return values;
	}

	void showAbout(){
		IJ.showMessage("About Kymograph...",
		"This plugin creates a so called time space plot. Look\n"+ 
		"It requires a stack of 8-Bit-Gray scale pictures and a line selection.");
	}
}
