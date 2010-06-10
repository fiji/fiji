// Fiji_Cube
// G. Landini 25/5/2010

setBatchMode(true);
run("Colors...", "foreground=white background=black selection=yellow");
newImage("Fiji", "8-bit Black", 256, 256, 1);
makePolygon(17,17,240,17,240,48,48,48,48,240,17,240,17,17);
run("Fill");
makePolygon(81,81,112,81,112,176,81,176);
run("Fill");
makePolygon(145,81,176,81,176,240,81,240,81,209,145,209);
run("Fill");
makePolygon(209,81,240,81,240,176,209,176);
run("Fill");
run("Select None");

run("Invert");
run("Duplicate...", "title=Fiji-f");
run("Flip Horizontally");
newImage("FijiCube", "8-bit Black", 256, 256, 256);
selectWindow("Fiji");
run("Copy");

max=256;
th=16;

selectWindow("FijiCube");
for (i=1;i<=th;i++) {
  setSlice(i);
  run("Paste");
}

selectWindow("Fiji-f");
run("Copy");
selectWindow("FijiCube");
for (i=max;i>=max-th;i--) {
  setSlice(i);
  run("Paste");
}

run("Reslice [/]...", "slice=1.000 start=Top");
selectWindow("Reslice of FijiCube");
for (i=1;i<=th;i++) {
  setSlice(i);
  run("Paste");
}

selectWindow("Fiji");
run("Copy");
selectWindow("Reslice of FijiCube");
for (i=max;i>=max-th;i--) {
  setSlice(i);
  run("Paste");
}
selectWindow("FijiCube");
close();

selectWindow("Reslice of FijiCube");
run("Reslice [/]...", "slice=1.000 start=Right");
selectWindow("Reslice of Reslice");

for (i=1;i<=th;i++) {
  setSlice(i);
  run("Paste");
}

selectWindow("Fiji-f");
run("Copy");
selectWindow("Reslice of Reslice");
for (i=max;i>=max-th;i--) {
  setSlice(i);
  run("Paste");
}
setBatchMode(false);

run("3D Viewer");
call("ij3d.ImageJ3DViewer.add", "Reslice of Reslice", "None", "Reslice of Reslice", "0", "true", "true", "true", "2", "0");
call("ij3d.ImageJ3DViewer.select", "Reslice of Reslice");
call("ij3d.ImageJ3DViewer.setColor", "125", "165", "255");
call("ij3d.ImageJ3DViewer.setTransform", "0.9800464 -0.120687135 -0.15793563 38.78557 0.15515418 0.96114224 0.2283259 -44.269543 0.12424261 -0.24827436 0.9606891 21.727379 0.0 0.0 0.0 1.0 ");
call("ij3d.ImageJ3DViewer.startAnimate");
selectWindow("Reslice of Reslice");
close();

