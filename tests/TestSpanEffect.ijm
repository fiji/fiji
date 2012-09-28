
span= 0.3340001;

setBatchMode(true);

for (i=0; i<20; i++ ) {
	selectWindow("ClassIVddaC.tif");
	run("Advanced Sholl Analysis", "starting=0 ending=82.30 radius_step=0 radius_span="+ i*span +" span_type=Mean sholl=Intersections fit polynomial=[5th degree]");
   
}
setBatchMode("exit & display");
