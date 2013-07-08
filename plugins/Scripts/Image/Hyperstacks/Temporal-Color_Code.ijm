/*

************* Temporal-Color Coder *******************************
Color code the temporal changes.

Kota Miura (miura@embl.de)
Centre for Molecular and Cellular Imaging, EMBL Heidelberg, Germany

If you publish a paper using this macro, please acknowledge.


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
101122  plugin'ified it
101123	fixed for cases when slices > 1 and frames == 1
*****************************************************************************
*/

var Glut = "Fire";	//default LUT
var Gstartf = 1;
var Gendf = 10;
var GFrameColorScaleCheck = 1;

macro "Time-Lapse Color Coder" {
	Stack.getDimensions(ww, hh, channels, slices, frames);
	if (channels > 1)
		exit("Cannot color-code multi-channel images!");
	//swap slices and frames in case:
	if ((slices > 1) && (frames == 1)) {
		frames = slices;
		slices = 1;
		Stack.setDimensions(1, slices, frames);
		print("slices and frames swapped");
	}
	Gendf = frames;
	showDialog();
	if (Gstartf <1) Gstartf = 1;
	if (Gendf > frames) Gendf = frames;
	totalframes = Gendf - Gstartf + 1;
	calcslices = slices * totalframes;
	imgID = getImageID();

	setBatchMode(true);

	newImage("colored", "RGB White", ww, hh, calcslices);
	run("Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="
		+ slices + " frames=" + totalframes + " display=Color");
	newimgID = getImageID();

	selectImage(imgID);
	run("Duplicate...", "duplicate");
	run("8-bit");
	imgID = getImageID();

	newImage("stamp", "8-bit White", 10, 10, 1);
	run(Glut);
	getLut(rA, gA, bA);
	close();
	nrA = newArray(256);
	ngA = newArray(256);
	nbA = newArray(256);

	newImage("temp", "8-bit White", ww, hh, 1);
	tempID = getImageID();

	for (i = 0; i < totalframes; i++) {
		colorscale = floor((256 / totalframes) * i);
		for (j = 0; j < 256; j++) {
			intensityfactor = j / 255;
			nrA[j] = round(rA[colorscale] * intensityfactor);
			ngA[j] = round(gA[colorscale] * intensityfactor);
			nbA[j] = round(bA[colorscale] * intensityfactor);
		}

		for (j = 0; j < slices; j++) {
			selectImage(imgID);
			Stack.setPosition(1, j + 1, i + Gstartf);
			run("Select All");
			run("Copy");

			selectImage(tempID);
			run("Paste");
			setLut(nrA, ngA, nbA);
			run("RGB Color");
			run("Select All");
			run("Copy");
			run("8-bit");

			selectImage(newimgID);
			Stack.setPosition(1, j + 1, i + 1);
			run("Select All");
			run("Paste");
		}
	}

	selectImage(tempID);
	close();

	selectImage(imgID);
	close();

	selectImage(newimgID);

	run("Stack to Hyperstack...", "order=xyctz channels=1 slices="
		+ totalframes + " frames=" + slices + " display=Color");
	op = "start=1 stop=" + Gendf + " projection=[Max Intensity] all";
	run("Z Project...", op);
	if (slices > 1)
		run("Stack to Hyperstack...", "order=xyczt(default) channels=1 slices=" + slices
			+ " frames=1 display=Color");
	resultImageID = getImageID();

	selectImage(newimgID);
	close();

	selectImage(resultImageID);
	setBatchMode("exit and display");

	if (GFrameColorScaleCheck)
		CreateScale(Glut, Gstartf, Gendf);
}

function makeLUTsArray() {
	eval("script", "importClass(Packages.ij.IJ);\n"
		+ "\n"
		+ "result = [];\n"
		+ "if (IJ.getInstance() != null) {\n"
		+ "	importClass(Packages.fiji.User_Plugins);\n"
		+ "	importClass(Packages.ij.Menus);\n"
		+ "\n"
		+ "	commands = Menus.getCommands();\n"
		+ "	lutsMenu = User_Plugins.getMenu('Image>Lookup Tables');\n"
		+ "	if (lutsMenu != null) {\n"
		+ "		for (i = 0; i < lutsMenu.getItemCount(); i++) {\n"
		+ "			menuItem = lutsMenu.getItem(i);\n"
		+ "			if (menuItem.getActionListeners().length == 0) {\n"
		+ "				// is a separator\n"
		+ "				continue;\n"
		+ "			}\n"
		+ "			label = menuItem.getLabel();\n"
		+ "			if (label.equals('Invert LUT') || label.equals('Apply LUT')) {\n"
		+ "				// no lookup table\n"
		+ "				continue;\n"
		+ "			}\n"
		+ "			command = commands.get(label);\n"
		+ "			if (command == null || command.startsWith('ij.plugin.LutLoader')) {\n"
		+ "				result.push(label);\n"
		+ "			}\n"
		+ "		}\n"
		+ "	}\n"
		+ "}\n"
		+ "// ImageJ < 1.47n always returned null from eval('script', script)\n"
		+ "// To work around this, we set a special system property. Hacky, but works.\n"
		+ "System.setProperty('result', result.join('\\n'));\n"
		+ "// ImageJ >= 1.47n *does* return the value of the last evaluated statement\n"
		+ "null;\n");
	return split(call("java.lang.System.getProperty", "result"), "\n");
}

function showDialog() {
	lutA = makeLUTsArray();

 	Dialog.create("Color Code Settings");
	Dialog.addChoice("LUT", lutA);
	Dialog.addNumber("start frame", Gstartf);
	Dialog.addNumber("end frame", Gendf);
	Dialog.addCheckbox("Create Time Color Scale Bar", GFrameColorScaleCheck);
	Dialog.show();
 	Glut = Dialog.getChoice();
	Gstartf = Dialog.getNumber();
	Gendf = Dialog.getNumber();
	GFrameColorScaleCheck = Dialog.getCheckbox();
}

function CreateScale(lutstr, beginf, endf){
	ww = 256;
	hh = 32;
	newImage("color time scale", "8-bit White", ww, hh, 1);
	for (j = 0; j < hh; j++) {
		for (i = 0; i < ww; i++) {
			setPixel(i, j, i);
		}
	}
	run(lutstr);
	run("RGB Color");
	op = "width=" + ww + " height=" + (hh + 16) + " position=Top-Center zero";
	run("Canvas Size...", op);
	setFont("SansSerif", 12, "antiliased");
	run("Colors...", "foreground=white background=black selection=yellow");
	drawString("frame", round(ww / 2) - 12, hh + 16);
	drawString(leftPad(beginf, 3), 0, hh + 16);
	drawString(leftPad(endf, 3), ww - 24, hh + 16);

}

function leftPad(n, width) {
    s = "" + n;
    while (lengthOf(s) < width)
        s = "0" + s;
    return s;
}