/*
 * Demonstration of human bias in estimating lengths
 *
 * Inspired by, and implemented during, Ivo Sbalzarini's talk
 * at EuBIAS 2015 by Johannes Schindelin
 */

x1 = 40; x2 = x1 + 100; x3 = x2 + 100; x4 = x3 + 60;
y1 = 40; y2 = y1 + 60; y3 = y2 + 20; y4 = y3 + 20; y5 = y4 + 20; y6 = y5 + 60;

lineWidth = 3;

width = x4 + x1;
height = y6 + y1;

setBatchMode(true);
newImage("Comparing Lengths", "RGB", width, height, 1);
Overlay.clear();

makePolygon(x1, y1, x2, y1, x3, y2, x4, y2, x4, y5, x3, y5, x2, y6, x1, y6);
Overlay.addSelection("black", lineWidth);

makePolygon(x1, y2, x2, y2, x3, y3, x4, y3, x4, y4, x3, y4, x2, y5, x1, y5);
Overlay.addSelection("black", lineWidth);

makeLine(x2, y1, x2, y2);
Overlay.addSelection("black", lineWidth);

makeLine(x2, y5, x2, y6);
Overlay.addSelection("black", lineWidth);

makeLine(x2, y2, x2, y5);
Overlay.addSelection("red", lineWidth);

makeLine(x3, y2, x3, y5);
Overlay.addSelection("red", lineWidth);

run("Select None");
run("Flatten");
setBatchMode(false);