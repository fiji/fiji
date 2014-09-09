/*
 * This optical illusion demonstrates how scientists are frequently
 * fooled by their own eyes by presenting them with seemingly curved
 * lines. The lines, however, are straight.
 * 
 * The origin of this optical illusion is unclear as of time of writing;
 * it was in a retweet of 
 * 
 * https://twitter.com/Sci_Phile/status/508406541892845568
 */

makeOverlay = false;

if (makeOverlay) {
	run("Remove Overlay");
} else {
	newImage("Straight Lines", "8-bit", 600, 600, 1);
}

width = getWidth();

function box(x, y, w, h) {
	if (makeOverlay) {
		makeRectangle(x, y, w, h);
		run("Add Selection...");
	} else {
		setColor(0);
		fillRect(x, y, w, h);
	}
}

function line(x1, y1, x2, y2) {
	if (makeOverlay) {
		makeLine(x1, y1, x2, y2);
		run("Add Selection...");
	} else {
		setColor(128);
		drawLine(x1, y1, x2, y2);
	}
}

function row(x, y, w, h, xdelta) {
	while (x < width) {
		box(x, y, w, h);
		x += xdelta;
	}
}

function hline(y) {
	line(0, y, width, y);
}

offsets = newArray(-8, 10, -8, -1, -1, 22, -8, -15, 10, -8, -1, -8,
	-1, -2, -1, 10, -8, -2, -15, -2, 6, -15, -27, -8, 6, -27);
y = -3;
for (i = 0; i < offsets.length; i++) {
	w = 20 + 4 * ((i == 5) + (i & 1) + 2 * ((i & 3) == 2));
	w2 = 3 + w * 15 / 8;
	row(offsets[i], y + 1, w, w - 1, w2);
	y += w;
	hline(y - 1);
	hline(y);
}
run("Gaussian Blur...", "sigma=1");
setOption("Changes", false);