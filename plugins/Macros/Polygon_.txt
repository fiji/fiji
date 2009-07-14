// Draws three n-sided polygon that have each corner connected to all other
// corners. The first is drawn with a screen update for each line, the second
// with an update for each corner and the third with just one update.

requires("1.29i");

// global variables
var size = 560;
var n = 25;
var delay = 0; // milliseconds.

drawPolygon3Ways();
//dump();

function drawPolygon3Ways() {
    setup();
    t1 = drawWithUpdatePerLine();
    t2 = drawWithUpdatePerCorner();
    t3 = drawWithOneUpdate();
    showTimes(t1, t2, t3);
}

function drawWithUpdatePerLine() {
    updatePerLine = true;
    updatePerCorner = false;
    return drawPolygon();
}

function drawWithUpdatePerCorner() {
    erase();
    updatePerLine = false;
    updatePerCorner = true;
    return drawPolygon();
}

function drawWithOneUpdate() {
    erase();
    updatePerLine = false;
    updatePerCorner = false;
    return drawPolygon();
}

function setup() {
    run("New...", "name=Polygon type='8-bit Unsigned' fill=White width="+size+" height="+size);
    run("Line Width...", "line=1");
}

function erase() {
     //wait(1000);
     setForegroundColor(255, 255, 255);
     makeRectangle(0, 0, size, size);
     run("Fill");
     run("Select None");
   }

function showTimes(t1, t2, t3) {
    drawString('Screen updates:', 10, size-50);
    drawString('  per line: '+ t1 + ' msec.', 10, size-35);
    drawString('  per corner: '+ t2 + ' msec.', 10, size-20);
    drawString('  per polygon: '+ t3 + ' msec.', 10, size-5);
}

function drawPolygon() {
    //if (delay>0)
    //    updatePerLine = true;
    autoUpdate(updatePerLine);
    start = getTime();
    setForegroundColor(0,0,0);
    center = size/2;
    r = 0.95*center;
    twoPi = 2*PI;
    inc = twoPi/n;
    for (a1=0; a1<twoPi; a1+=inc) {
        x1 = r*sin(a1) + center;
        y1 = r*cos(a1) + center;
        for (a2=a1; a2<twoPi; a2+=inc) {
            x2 = r*sin(a2) + center;
            y2 = r*cos(a2) + center;
            drawLine(x1,y1,x2,y2);
            if (delay>0) wait(delay);
        }
        if (updatePerCorner)
            updateDisplay();
     }
     updateDisplay();
     return getTime()-start);
}

macro "Draw with an update for each line" {
    setup();
    drawWithUpdatePerLine();
}

macro "Draw with an update for each corner" {
    setup();
    drawWithUpdatePerCorner();
}

macro "Draw with only one update" {
    setup();
    drawWithOneUpdate();
}

macro "Draw all three ways" {
     drawPolygon3Ways();
}

macro "-" {}  // menu divider

macro "Set Sides..." {
     n = getNumber("Number of sides:", n);
}

macro "Set Size..." {
     size = getNumber("Window width (pixels):", size);
}

macro "Set Delay..." {
     delay = getNumber("Delay per line (milliseconds):", delay);
}




