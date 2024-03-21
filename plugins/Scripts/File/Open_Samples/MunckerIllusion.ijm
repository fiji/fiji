// generates the Muncker illusion

// image size
w = 512;
h = 512;

// stripes Line height
lineHeight = 4;

// RGB colours, and array of colour names for stripes
yellow = 0x999900;
red = 0xbb0000;
green = 0x009900;
blue = 0x0000ff;
stripesColours = newArray(red, green, blue);

// black image to fill in
newImage("Muncker's illusion", "RGB Black", w, h, 1);

// for loop to generate the horizontal stripes of desired height
// in order to make the background image for the illusion

for (j = 0; j < h; j++)
	for (i = 0; i < w; i++) {
		if
			((j/lineHeight) % 3 == 0)
			colour = red;
		else if
			((j/lineHeight) % 3 == 1)
			colour = green; 
		else if
			((j/lineHeight) % 3 == 2)
			colour = blue;
	setPixel(i, j, colour);
	}

// Loop to draw several yellow "circles" over all stripes except 1 of the coloured stripes
// leave 1 different colour stripe for the different circles
for (i = 0; i < stripesColours.length; i++) {
replaceColour = stripesColours[i];

// make oval selection to draw in
makeOval(100*(i+1), 100*(i+1), 70, 70);

// get the pixels locations of the pixels in the ROI as 2 arrays
Roi.getContainedPoints(xpoints, ypoints);

// for the current line colour to replace, 
// fill in the circle with yellow only in the pixels of that line colour
	for (j = 0; j < xpoints.length; j++)  {
		if ((getPixel(xpoints[j], ypoints[j])) != replaceColour)
			setPixel(xpoints[j], ypoints[j], yellow);
	}
}
Roi.remove;

// Add an explination test
// on the image in a non-destructive overlay.

text = "The circles are the same colour!";
setFont("SansSerif", 24, " antialiased");
makeText(text, 10, 20);
run("Add Selection...", "stroke=yellow fill=#660000ff new");
run("Select None");
