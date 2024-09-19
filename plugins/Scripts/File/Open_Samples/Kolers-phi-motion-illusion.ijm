//imagej macro to show the "phi" or  "moving spot" illusion.
//Wertheimer, M. 1912. 
// Kolers P A, von Grunau M, 1976 "Shape and color in apparent motion' Vision Research 16. 329-335
//
// "The spot" doesnt move, there are 2 spots,
// one always off if the other is on.
// works with less than 4 degree view angle, 
// and with white spots and colored spots. 
// with coloured spots the "moving" spot may appear to change colour
// halfway through its "motion", but it can not be true,
// since the other spot is not shown yet. 


newImage("phi motion illusion", "RGB Black", 256, 256, 1);

w=10;
h=w;
sep=10;
xl=128-(sep/2)-(w/2);
xr=xl+sep;
yl=128-(w/2);
yr=yl;


spotTime=150;
blackTime=50;

for (i=0; i<1000; i++) {

	setColor(0,255,0);
	fillOval(xl,yl,w,h);
	wait(spotTime);
	
	setColor(0,0,0);
	fillOval(xl,yl,w,h);
	wait(blackTime);

	setColor(255,0,0);
	fillOval(xr,yr,w,h);
	wait(spotTime);

	setColor(0,0,0);
	fillOval(xr,yr,w,h);
	wait(blackTime);
	}
