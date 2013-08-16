import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

/** Plugin for multiple line selction. Data of the lines are stored in a 32-bit gray-scale
      picture. Additionally an overlay image is created where the chosen line is drawn
      in red.  
    
    please send comments to seitz@embl.de
*/

public class MultipleOverlay_ implements PlugIn {
	double [] profile;
		
	public void run(String arg) 
	{
        int overID=0;
		int storeID=0;
		
		double x1=0,x2=0,y1=0,y2=0;
		String wintitle;
		boolean overexist=false;
		boolean storeexist=false;
		
		ImagePlus imp=WindowManager.getCurrentImage();
		ImageProcessor ip = imp.getProcessor();
		
		ImagePlus overlay, test, store;
		ImageProcessor inv_ip, store_ip;
		
		int w = ip.getWidth();
		int h = ip.getHeight();
		
		int wincount=WindowManager.getWindowCount();
		
		int [] idlist =WindowManager.getIDList();
		for (int i=0;i<wincount;i++){
			test=WindowManager.getImage(idlist[i]);
			wintitle=test.getTitle();
			if (wintitle=="Overlay image")  {
				overexist=true;
				overID=idlist[i];
			}
			if (wintitle=="Storage")  {
				storeexist=true;
				storeID=idlist[i];
					 
			}
			
		}
		if (storeexist) {	store = WindowManager.getImage(storeID);
				store_ip=store.getProcessor();
						
		} else {store=NewImage.createShortImage("Storage",50,50,1,NewImage.FILL_BLACK);
				store_ip=store.getProcessor();
				store.show();			
		}

		if (overexist) {	overlay = WindowManager.getImage(overID);
				inv_ip=overlay.getProcessor();
															
		
		} else {		overlay = NewImage.createRGBImage("Overlay image", w,h,1, NewImage.FILL_BLACK);
				inv_ip = overlay.getProcessor();
				int bd=imp.getBitDepth();
				
				if (bd==8) inv_ip.copyBits(ip,0,0,Blitter.COPY);
				if (bd>8) inv_ip.copyBits(ip.convertToRGB(),0,0,Blitter.COPY);
				
						
				
				
		}
				
			
		Roi roi = imp.getRoi();
		int roiType = roi.getType();
		if (roi==null){
			IJ.error("Selection required");
			return;
		}
		
		
		if (roiType==Roi.LINE) {
            			x1=((Line)roi).x1;
			x2=((Line)roi).x2;
			y1=((Line)roi).y1;
			y2=((Line)roi).y2;
			inv_ip=over(inv_ip,roi,x1,x2,y1,y2); 
			store_ip=storage(roi, store_ip); 
		}
		else 	{
			int n = ((PolygonRoi)roi).getNCoordinates();
        	int[] cx = ((PolygonRoi)roi).getXCoordinates();
        	int[] cy = ((PolygonRoi)roi).getYCoordinates();
			Rectangle rect=roi.getBoundingRect();
			
			int x0=rect.x;
			int y0=rect.y;
			store_ip=storage(roi, store_ip); 

			for (int i=0;i<n-1;i++){
				x1=cx[i]+x0;
				x2=cx[i+1]+x0;
				y1=cy[i]+y0;
				y2=cy[i+1]+y0;
				inv_ip=over(inv_ip, roi, x1,x2,y1,y2);
			}			
			
			
		}
	
	overlay.show();
	overlay.updateAndDraw();
	store.show();
	store.updateAndDraw();
		
	}
	ImageProcessor storage (Roi roi, ImageProcessor store_ip){
				
		short[] pixels = (short[]) store_ip.getPixels();
				
		int num=1;
		for (int i=0; i< pixels[0];i++){
			num+=((pixels[num]*2)+1);
		}
		
		int roiType = roi.getType();
		pixels[0]++;		
		if (roiType==Roi.LINE) {
			pixels[num]=2;
			pixels[num+1]=(short) ((Line)roi).x1;
			pixels[num+2]=(short) ((Line)roi).x2;
			pixels[num+3]=(short) ((Line)roi).y1;
			pixels[num+4]=(short) ((Line)roi).y2;
					 
		}
		else 	{
			int n = ((PolygonRoi)roi).getNCoordinates();
			int[] cx = ((PolygonRoi)roi).getXCoordinates();
        			int[] cy = ((PolygonRoi)roi).getYCoordinates();
			Rectangle rect=roi.getBoundingRect();
			int x0=rect.x;
			int y0=rect.y;
        			
			for (int i=0; i<n;i++){
				pixels[num]=(short)n;
				pixels[num+i+1]=(short)(cx[i]+x0);
				pixels[num+n+i+1]=(short)(cy[i]+y0);
			}
		}
		store_ip.setPixels(pixels);
		return store_ip;
	}
			
	ImageProcessor over (ImageProcessor over_ip, Roi roi,
			double x1, double x2, double y1, double y2){
		
		double a1,a2,b1,b2;
		double slope=(y2-y1)/(x2-x1);
		
		int w = over_ip.getWidth();
		int h = over_ip.getHeight();
		
		
		int[] pixels = (int[]) over_ip.getPixels();
		
		int s=0;
		int start;
		int ende;
		int j=0;
		
		if (slope > -1 && slope <=1){
			
		      if (x1>x2){
			a1=x2;
			a2=x1;
			b1=y2;
			b2=y1;
			
		      } 
		      else {
			a1=x1;
			a2=x2;
			b1=y1;
			b2=y2;
			
		      }	
			
		   
		      start=(int)a1;
		      ende=(int)a2;
		      int x=(int)a1;
		      int y;
		
		      for (int i=start;i<ende;i++){
						
			y=(int)(0.5+b1+j*slope);
			s=(int)(y*w+i);
			j++;
					
			int c = pixels[s];
			int r = 255;
			int g = 0;
			int b = 0;
			pixels[s] = ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
		      }
		}
		else {
		      if (y1>y2){
			a1=x2;
			a2=x1;
			b1=y2;
			b2=y1;
					
		      } else {
			a1=x1;
			a2=x2;
			b1=y1;
			b2=y2;
			
		      }
		      start=(int)b1;
		      ende=(int)b2;
		      int x=(int)b1;
		      int y;
		
		      for (int i=start;i<ende;i++){
			
			
			x=(int)(a1+(i-b1)/slope);
			s=(int)(i*w+x);
			x++;
			j++;
			
			
			int c = pixels[s];
			int r = 255;
			int g = 0;
			int b = 0;
			pixels[s] = ((r & 0xff) << 16) + ((g & 0xff) << 8) + (b & 0xff);
		   }
		}   
		
		over_ip.setPixels(pixels);
		
	return over_ip;
		
       	 	
	        		
	}
	
	

}
