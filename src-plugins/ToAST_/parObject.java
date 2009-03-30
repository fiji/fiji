import java.awt.*;
import ij.*;

public class parObject{
	public int n; //size of object in pixels
	public int maxpixnum = 10000;
	public double[] x = new double [maxpixnum], y = new double[maxpixnum]; 
	public double[] intensity = new double[maxpixnum]; 
	public int type; 
	public double princangle, curv, xave, yave; 
	public parObject(){
		n = 0;
		princangle = -32000;xave = -1; yave = -1;curv = -32000;  
	}
	public void addp(int i,int j, double k){
		if (isin(i,j) == 0 && n < (maxpixnum-3)){x[n] = (double)i; y[n] = (double)j;intensity[n] = k;  n++;}
	} 
	public void addp(double i,double j, double k){
		if (isin(i,j) == 0 && n < (maxpixnum-3)){x[n] = i; y[n] = j;intensity[n] = k;  n++;}
	} 
	public void print(){
		IJ.log("N "+ n); 
	}
	public double avef(){
		double s=0;int i;
		for(i = 0; i < n; i++)s+=intensity[i];
		return (double)s/(double)n;
	}
	public int objPixelSize(){return n;}
	public int isin(int i, int j){
		for(int b=0; b < n; b++)
			if(x[b]==i && y[b]==j)
				return 1;
		return 0;
	}
	public int isin(double i, double j){
		for(int b=0; b < n; b++)
			if(x[b]==i && y[b]==j)
				return 1;
		return 0;
	}
	public double sqrt(double sqrt_rslt){
		double sqrt_l = sqrt_rslt;
		double sqrt_div = (char)((sqrt_l+1)/2);
		for(;sqrt_rslt > sqrt_div; sqrt_div =  (char)((sqrt_l/sqrt_div+sqrt_div+1)/2))  
		sqrt_rslt = sqrt_div;   
		return sqrt_rslt;
	}

	public int isclose(int i, int j){
		for(int b  = 0; b < n; b++){
			if( sqrt((i-x[b])*(i-x[b])+(j-y[b])*(j-y[b])) <= 2)
			{return 1;}
		}
		return 0;
	}
	public double avex(){
		int i;double s = 0;
		for(i = 0; i < n; i++)s+=x[i];
		xave = (double)s/(double)n;
		return (double)s/(double)n;
	}
	public double avey(){
		int i;double s = 0;
		for(i = 0; i < n; i++)s+=y[i];
		yave = (double)s/(double)n; 
		return (double)s/(double)n;
	}
	public double dx(){//dispersion of x
		double s = avex(), d= 0;
		for(int i = 0; i < n; i++)d+=(double)(x[i]-s)*(double)(x[i]-s);
		return sqrt(d*100)/10;
	}
	public double dy(){//dispersion of y
		double s = avey(), d= 0;
		for(int i = 0; i < n; i++)d+=(double)(y[i]-s)*(double)(y[i]-s);
		return sqrt(d*100)/10;
	}
	public int sumint(){
		int res = 0, i; 
		for(i = 0; i < n; i++)res+=intensity[i];
		return res;
	}
	public double wavex(){//weighted average x
		int i;double s = 0;
		for(i = 0; i < n; i++)s+=x[i]*intensity[i];
		return (double)s/(double)sumint();
	}
	public double wavey(){//weighted average x
		int i;double s = 0;
		for(i = 0; i < n; i++)s+=y[i]*intensity[i];
		return (double)s/(double)sumint();
	}
	public double corrxy(){
		double sx = avex(), sy = avey(), cov = 0;
		for(int i = 0; i < n; i++)cov+=(double)(sx-x[i])*(double)(sy-y[i]);	
		return (cov/dx()/dy());
	} 
	public double corrxyweight(){
		double sx = wavex(), sy = wavey(), cov = 0;
		for(int i = 0; i < n; i++)cov+=(double)intensity[i]*(double)(sx-x[i])*(double)(sy-y[i]);
		return (n*cov/dx()/dy()/sumint());
	} 
/*	public double princ_angle(){
		double a = 0, b = 0;
		for(int i = 0; i < n; i++){b += ((double)x[i] - avex())*((double)y[i] - avey()); a += ((double)x[i] - avex())*((double)x[i] - avex());}
		return Math.atan(b/a)*360/6.28;
	}
*/	public int maxX(){
		double maxx = 0;
		for(int i = 0; i < n; i++)if(x[i]>maxx)maxx = x[i]; 
		return (int)maxx;  
	}
	public int minX(){
		double minx = 32000;
		for(int i = 0; i < n; i++)if(x[i]<minx)minx = x[i]; 
		return (int)minx;  
	}
	public int maxY(){
		double maxy = 0;
		for(int i = 0; i < n; i++)if(y[i]>maxy)maxy = y[i]; 
		return (int)maxy;  
	}
	public int minY(){
		double miny = 32000;
		for(int i = 0; i < n; i++)if(y[i]<miny)miny = y[i]; 
		return (int)miny;  
	}
	public double middleX(double yval){
		double maxx= 0, minx= 100000; 
		for (int i = 0; i<n; i++){
			if((int)y[i] < (int)(yval+1) && (int)y[i] > (int)(yval-1)){
				if(x[i] < minx)minx = x[i]; 
				if(x[i] > maxx)maxx = x[i]; 
			}				
		}
		return (minx + maxx)/2; 
	}
/*	public double middleX(){
		double maxx= 0, minx= 100000; 
		parObject rtpar = new parObject(); 
		rtpar = rotate(90-princ_angle()); 
		double yval = rtpar.avey(); 
		for (int i = 0; i<n; i++){
			if((int)rtpar.y[i] < (int)(yval+1) && (int)rtpar.y[i] > (int)(yval-1)){
				if(x[i] < minx)minx = rtpar.x[i]; 
				if(x[i] > maxx)maxx = rtpar.x[i]; 
			}				
		}
		double xxx = rtpar.avex()-(minx + maxx)/2; 
		
		return avex()-xxx*Math.sin(princ_angle()); 
	}*/
	public int deftype(){//for transformaion screen
		parObject rtpar = new parObject();
		rtpar = rotate(45 - princ_angle());
		double c = Math.max(Math.abs(rtpar.corrxy()), Math.abs(corrxy()));
		if(Math.abs(c) >= 0.52) return 3;//banana
		if((Math.abs(c) < 0.52)&&(Math.abs(c) > 0.15)) return 2;//demon head
		if(Math.abs(c) <= 0.15) return 1;//dot
		return 0;
	}
	public int deftype_ooc(){//for oocytecnt
		parObject rtpar = new parObject();
		rtpar = rotate(45);
		double c = Math.max(Math.abs(rtpar.corrxy()), Math.abs(corrxy()));
		if(Math.abs(c) > 0.25) return 2;
		if(Math.abs(c) <= 0.25) return 1;
		return 0;
	}
	public double mlen(){
		double d=0;int i,j;
		for(i = 0; i <n; i++)
			for(j = 0; j < i; j++)
				if(sqrt((x[i]-x[j])*(x[i]-x[j])+(y[j]-y[i])*(y[j]-y[i])) > d)
					d = Math.sqrt((x[i]-x[j])*(x[i]-x[j])+(y[j]-y[i])*(y[j]-y[i]));
		return d;
	}
	public double dispersion(){
		double d = 0, ax = avex(), ay = avey();
		for(int i = 0; i < n; i++)d+= sqrt((x[i]-ax)*(x[i]-ax)+(ay-y[i])*(ay-y[i]));
		return d;
	}
	parObject rotate(double alpha){//in angles
		double alpharad = alpha*6.28/360;
		parObject rtpar = new parObject();
		double ax = avex(), ay = avey();
		for(int i = 0; i < n; i++)
			rtpar.addp((Math.cos(alpharad)*(x[i]-ax) + Math.sin(alpharad)*(y[i]-ay) + ax), 
				   (Math.cos(alpharad)*(y[i]-ay) - Math.sin(alpharad)*(x[i]-ax) + ay), 
				   intensity[i]);
		return rtpar;
	}
	public double princ_angle3(){
		parObject rot45 = new parObject(); 
		rot45 = rotate(45); 		
		double a = 0, b = 0, nd = (double)n;
		for(int i = 0; i < n; i++){b += ((double)x[i] - avex())*((double)y[i] - avey()); a += ((double)x[i] - avex())*((double)x[i] - avex());}
		return Math.atan((nd*b - dx()*dy())/(nd*a - dx()*dx()))/6.28*360.0;
	}
	public double princ_angle2(){
		parObject rot45 = new parObject(); 
		double rangle = 0; 
		rot45 = rotate(rangle); 		
		double a = 0, b = 0, nd = (double)rot45.n;
		for(int i = 0; i < n; i++){b += ((double)rot45.x[i] - rot45.avex())*((double)rot45.y[i] - rot45.avey()); a += ((double)rot45.x[i] - rot45.avex())*((double)rot45.x[i] - rot45.avex());}
		return (Math.atan((nd*b - dx()*dy())/(nd*a - dx()*dx()))/6.28*360.0);
	}
	public double princ_angle(){
		if(princangle != -32000) return princangle; 
		double spf = 0, bestangle = 0;
		parObject rot = new parObject(); 
		for (double i = 0; i< 180; i+=15){
			rot = rotate(i); 
			if ((rot.dy()/rot.dx())> spf){spf = rot.dy()/rot.dx(); bestangle = i;}
		}double roughangle = bestangle; 
		for (double i = roughangle-8; i < roughangle+9; i+=0.5){
			rot = rotate(i); 
			if ((rot.dy()/rot.dx())> spf){spf = rot.dy()/rot.dx(); bestangle = i;}
		}
		princangle = bestangle-90; 
		return bestangle-90; 
	}

	double curvature(){//intrinsic curvature 
		if (curv != -32000) return curv; 
		double ds = 0, bestds = 2147483647, yd, xd;
		long bestfit = -1000; 
		if(princangle == -32000)princ_angle();
		parObject rtpar = new parObject(); 
		rtpar = rotate(90+princangle);
		if(xave == -1)dx();
		if(yave == -1)dy();
		double ax = middleX(yave), ay = yave; 
		for (long r = -400; r<=400; r+=30){ds = 0; 		
			if(Math.abs(r)*2 < rtpar.mlen()+1)continue; 
			for (int i = 0; i <n; i++){
				ds += Math.abs(Math.sqrt((rtpar.x[i] - ax+r)*(rtpar.x[i] - ax+r) + (rtpar.y[i] - ay)*(rtpar.y[i] - ay))-Math.abs(r)); 
			}	
			if(ds < bestds){bestds = ds; bestfit = r;}
		}
		for (long r = bestfit-15; r<=bestfit+15; r++){ds = 0; 	
			if(Math.abs(r)*2 < rtpar.mlen()+1)continue; 	
			for (int i = 0; i <n; i++){
				ds += Math.abs(Math.sqrt((rtpar.x[i] - ax+r)*(rtpar.x[i] - ax+r) + (rtpar.y[i] - ay)*(rtpar.y[i] - ay))-Math.abs(r)); 
			}			
			if(ds < bestds){bestds = ds; bestfit = r;}
		}

		return 1/(double)bestfit; 
	}
	public int perimeter(){
		int per = 0; 
/*		for (int i = 0; i<n; i++)
			per+= (1-isin(x[i]+1, y[i]))*(1-isin(x[i]-1, y[i]))*(1-isin(x[i], y[i]-1))*(1-isin(x[i], y[i]+1)); 
		return per; 
*/		double maxdist = 1, check; 
		for (int i = 0; i<n; i++){check = 0; 
			for (int j = 0; j < n; j++)
				if (Math.sqrt((x[i] - x[j])*(x[i] - x[j]) + (y[i] - y[j])*(y[i] - y[j])) <= maxdist)check++;
			if (check < 5)per++; 
		}
		return per; 
	}
	public double circ_index(){
		return (double)perimeter()/(double)(n); 
	}
}

