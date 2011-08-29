/*
 * This macro scales all ROIs in the ROI Manager
 *
 * Note: since the scaling is performed using integers,
 * upscaling leads to step artifacts.
 *
 * For technical reasons, this macro cannot handle composite
 * selections.
 */

function scaleROI(factor) {
	type = selectionType();
	getSelectionCoordinates(x, y);
	for (i = 0; i < x.length; i++) {
		x[i] = x[i] * factor;
		y[i] = y[i] * factor;
	}
	makeSelection(type, x, y);
}

factor = getNumber("Factor", 0.5);

count = roiManager("count");
current = roiManager("index");
for (i = 0; i < count; i++) {
	roiManager("select", i);
	scaleROI(factor);
	roiManager("update");
}
if (current < 0)
	roiManager("deselect");
else
	roiManager("select", current);