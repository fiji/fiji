// Adelson Checker-shadow illusion
w = 700; h = 600;
n = 5;
grey1 = 50; grey3 = 200;
grey2 = (grey1 + grey3) / 2; grey4 = (grey3 + 255) / 2;
x1 = w / 3; x2 = 2 * w / 3;
y1 = h / 5; y2 = 2 * h / 5; y4 = 9 * h / 10; y3 = y4 - y2 + y1;
dxx = (x2 - 0) / n; dxy = (y4 - y3) / n;
dyx = (x1 - 0) / n; dyy = (y1 - y3) / n;
dy = h - y4;

function getX(i, j) {
	return dxx * i + dyx * j;
}

function getY(i, j) {
	return y3 + dxy * i + dyy * j;
}

function square(i, j) {
	makePolygon(getX(i, j), getY(i, j),
		getX(i + 1, j), getY(i + 1, j),
		getX(i + 1, j + 1), getY(i + 1, j + 1),
		getX(i, j + 1), getY(i, j + 1));
}

function edge(i, j) {
	makePolygon(getX(i, j), getY(i, j),
		getX(i, j), getY(i, j) + dy,
		getX(i + 1, j), getY(i + 1, j) + dy,
		getX(i + 1, j), getY(i + 1, j));
}
function edge2(i, j) {
	makePolygon(getX(i, j), getY(i, j),
		getX(i, j), getY(i, j) + dy,
		getX(i, j + 1), getY(i, j + 1) + dy,
		getX(i, j + 1), getY(i, j + 1));
}

function diamond(i, j) {
	x = getX(i, j) + (dxx + dyx) / 2;
	y = getY(i, j) + (dxy + dyy) / 2;
	r1 = 1 / 6; r2 = 1 / 4;
	ow = dxx * r1; oh = dyy * r2;
	makePolygon(x - dxx * r1, y - dxy * r1,
		x - dyx * r2, y - dyy * r2,
		x + dxx * r1, y + dxy * r1,
		x + dyx * r2, y + dyy * r2);
}

newImage("Grey square optical illusion", "RGB White", w, h, 1);
image = getImageID();

setBatchMode(true);
setForegroundColor(grey2, grey2, grey2);
for (i = 0; i < n; i++)
	for (j = ((i + 1) % 2); j < n; j += 2) {
		square(i, j);
		run("Fill");
	}
for (i = 1; i < n - 1; i++) {
	square(i, i);
	run("Fill");
}
setForegroundColor(grey3, grey3, grey3);
for (i = 0; i < n; i += 2) {
	square(0, i);
	run("Fill");
	square(i, 0);
	run("Fill");
	square(n - 1, i);
	run("Fill");
	square(i, n - 1);
	run("Fill");
}

setForegroundColor(grey3+48, grey3+48, grey3+48);
for (i = 0; i < n; i += 2) {
	edge2(n, i);
	run("Fill");
}

setForegroundColor(grey2, grey2, grey2);
for (i = 0; i < n; i += 2) {
	edge(i, 0);
	run("Fill");
}

setForegroundColor(grey3, grey3, grey3);
for (i = 1; i < n - 1; i += 2) {
	edge2(n, i);
	run("Fill");
}

setForegroundColor(grey1, grey1, grey1);
for (i = 1; i < n; i += 2) {
	edge(i, 0);
	run("Fill");
}
setForegroundColor(grey4, grey4, grey4);
square(n - 1, n - 1);
run("Fill");
newImage("scratch pad", "RGB White", w, h, 1);
scratch = getImageID();
setBackgroundColor(grey3, grey3, grey3);
setForegroundColor(grey1, grey1, grey1);
makeLine(getX(0, 4), getY(0, 4), getX(2, 2), getY(2, 2));
run("Linear Gradient");
square(0, 3);
run("Copy");
selectImage(image);
for (i = 2; i < n - 1; i++) {
	square(i - 2, i);
	run("Paste");
}
selectImage(scratch);
square(1, 2);
run("Copy");
selectImage(image);
for (i = 1; i < n - 1; i++) {
	square(i - 1, i);
	run("Paste");
}

selectImage(scratch);
makeLine(getX(4, 0), getY(4, 0), getX(2, 2), getY(2, 2));
run("Linear Gradient");
square(3, 0);
run("Copy");
selectImage(image);
for (i = 0; i < n - 3; i++) {
	square(i + 2, i);
	run("Paste");
}
selectImage(scratch);
square(2, 1);
run("Copy");
selectImage(image);
for (i = 0; i < n - 2; i++) {
	square(i + 1, i);
	run("Paste");
}

x1 = getX(3.25, 3); y1 = getY(4, 4.5);
x2 = getX(4.25, 5); y2 = getY(4, 3);
h1 = h / 3; dy = (y2 - y1) / 2;
selectImage(scratch);
makeLine(x2, y1, x1, y1);
setBackgroundColor(255, 255, 255);
run("Linear Gradient");
makeRectangle(x1, y1 - h1, x2 - x1, h1);
run("Copy");
selectImage(image);
makeRectangle(x1, y1 - h1 + dy, x2 - x1, h1);
run("Paste");
selectImage(scratch);
makeOval(x1, y1, x2 - x1, y2 - y1);
run("Copy");
selectImage(image);
makeOval(x1, y1, x2 - x1, y2 - y1);
run("Paste");

setForegroundColor(grey2, grey2, grey2);
makeOval(x1, y1 - h1, x2 - x1, y2 - y1);
run("Fill");

setForegroundColor(grey1, grey1, grey1);
selectImage(image);
diamond(1, 4);
run("Fill");
diamond(2, 2);
run("Fill");

run("Select None");
selectImage(scratch);
close();
run("8-bit");
setBatchMode(false);
