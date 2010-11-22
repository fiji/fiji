/*

************* Temporal-Color Coder *******************************
Color code the temporal changes.

Kota Miura (miura@embl.de) +49 6221 387 404
Centre for Molecular and Cellular Imaging, EMBL Heidelberg, Germany

!!! Please do not distribute. If asked, please tell the person to contact me. !!!
If you publish a paper using this macro, it would be cratedful if you could acknowledge.


---- INSTRUCTION ----

1. Open a stack (8 bit or 16 bit)
2. Run the macro
3. In the dialog choose one of the LUT for time coding.
	select frame range (default is full).
	check if you want to have color scale bar.

History

080212	created ver1 K_TimeRGBcolorcode.ijm
080213	stack slice range option added.
		time color code scale option added.

		future probable addiition: none-linear assigning of gray intensity to color intensity
		--> but this is same as doing contrast enhancement before processing.
*****************************************************************************
*/


var Glut = "Fire";	//default LUT
var Gstartf = 1;
var Gendf = 10;
var GFrameColorScaleCheck=1;

macro "Time-Lapse Color Coder"{
	Gendf = nSlices;
	Glut = ChooseLut();
	run("Duplicate...", "title=listeriacells-1.stk duplicate");
	hh = getHeight();
	ww = getWidth();
	totalslice = nSlices;
	calcslices = Gendf - Gstartf +1;
	run("8-bit");
	imgID = getImageID();

	newImage("colored", "RGB White", ww, hh, calcslices);
	newimgID = getImageID();

	setBatchMode(true);

	newImage("stamp", "8-bit White", 10, 10, 1);
	run(Glut);
	getLut(rA, gA, bA);
	close();
	nrA = newArray(256);
	ngA = newArray(256);
	nbA = newArray(256);

	for (i=0; i<calcslices; i++) {
		colorscale=floor((256/calcslices)*i);
		//print(colorscale);
		for (j=0; j<256; j++) {
			intensityfactor=0;
			if (j!=0) intensityfactor = j/255;
			nrA[j] = round(rA[colorscale] * intensityfactor);
			ngA[j] = round(gA[colorscale] * intensityfactor);
			nbA[j] = round(bA[colorscale] * intensityfactor);
		}
		newImage("temp", "8-bit White", ww, hh, 1);
		tempID = getImageID;

		selectImage(imgID);
		setSlice(i+Gstartf);
		run("Select All");
		run("Copy");

		selectImage(tempID);
		run("Paste");
		setLut(nrA, ngA, nbA);
		run("RGB Color");
		run("Select All");
		run("Copy");
		close();

		selectImage(newimgID);
		setSlice(i+1);
		run("Select All");
		run("Paste");
	}

	selectImage(imgID);
	close();
	selectImage(newimgID);
	op = "start=1 stop="+totalslice+" projection=[Max Intensity]";
	run("Z Project...", op);
	setBatchMode("exit and display");
	if (GFrameColorScaleCheck) CreatGrayscale256(Glut, Gstartf, Gendf);
}

/*
run("Spectrum");
run("Fire");
run("Ice");
run("3-3-2 RGB");
run("brgbcmyw");
run("Green Fire Blue");
run("royal");
run("thal");
run("smart");
run("unionjack");
10 luts
*/

function ChooseLut() {
	lutA=newArray(10);
	lutA[0] = "Spectrum";
	lutA[1] = "Fire";
	lutA[2] = "Ice";
	lutA[3] = "3-3-2 RGB";
	lutA[4] = "brgbcmyw";
	lutA[5] = "Green Fire Blue";
	lutA[6] = "royal";
	lutA[7] = "thal";
	lutA[8] = "smart";
	lutA[9] = "unionjack";

 	Dialog.create("Color Code Settings");
	Dialog.addChoice("LUT", lutA);
	Dialog.addNumber("start frame", Gstartf);
	Dialog.addNumber("end frame", Gendf);
	Dialog.addCheckbox("create Time Color Scale Bar", GFrameColorScaleCheck);
 	Dialog.show();
 	Glut = Dialog.getChoice();
	Gstartf= Dialog.getNumber();
	Gendf= Dialog.getNumber();
	GFrameColorScaleCheck = Dialog.getCheckbox();
	print("selected lut:"+ Glut);
	return Glut;
}

function CreatGrayscale256(lutstr, beginf, endf){
	ww = 256;
	hh=32;
	newImage("color time scale", "8-bit White", ww, hh, 1);
	for (j=0; j<hh; j++) {
		for (i=0; i<ww; i++) {
			setPixel(i, j, i);
		}
	}
	run(lutstr);
	//setLut(nrA, ngA, nbA);
	run("RGB Color");
	op = "width="+ww+" height="+hh+16+" position=Top-Center zero";
	run("Canvas Size...", op);
	setFont("SansSerif", 12, "antiliased");
	run("Colors...", "foreground=white background=black selection=yellow");
	drawString("frame", round(ww/2)-12, hh+16);
	drawString(leftPad(beginf, 3), 0, hh+16);
	drawString(leftPad(endf, 3), ww-24, hh+16);

}

function leftPad(n, width) {
    s =""+n;
    while (lengthOf(s)<width)
        s = "0"+s;
    return s;
}
/*
macro "drawscale"{
	CreatGrayscale256("Fire", 1, 100);
}
*/
