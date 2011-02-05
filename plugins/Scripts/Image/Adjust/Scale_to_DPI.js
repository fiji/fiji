//
// Scale_to_DPI.js
//

// This script resizes an image to match a particular number of
// dots per inch (DPI) with a specified width and height in inches.
// It also updates the spatial calibration accordingly.

scaleToDPI();

function scaleToDPI() {
  imp = IJ.getImage();
  dpi = 600;
  wPix = imp.getWidth();
  hPix = imp.getHeight();
  wInches = wPix / dpi;
  hInches = hPix / dpi;
  methods = ImageProcessor.getInterpolationMethods();
  title = WindowManager.getUniqueName(imp.getTitle());

  // use applicable Image Scale parameters by default
  interpolationMethod = getFieldValue("interpolationMethod").intValue();
  fillWithBackground = getFieldValue("fillWithBackground").booleanValue();
  averageWhenDownsizing = getFieldValue("averageWhenDownsizing").booleanValue();
  newWindow = getFieldValue("newWindow").booleanValue();

  // prompt for parameters
  gd = new GenericDialog("Scale by DPI");
  gd.addNumericField("DPI", dpi, 0);
  gd.addNumericField("Width (inches)", wInches, 3);
  gd.addNumericField("Height (inches)", hInches, 3);
  gd.addChoice("Interpolation:", methods, methods[interpolationMethod]);
  gd.addCheckbox("Fill with background color", fillWithBackground);
  gd.addCheckbox("Average when downsizing", averageWhenDownsizing);
  gd.addCheckbox("Create new window", newWindow);
  gd.addStringField("Title", title, 12);
  gd.showDialog();
  if (!gd.wasOKed()) return;
  dpi = gd.getNextNumber();
  wInches = gd.getNextNumber();
  hInches = gd.getNextNumber();
  interpolationMethod = gd.getNextChoiceIndex();
  fillWithBackground = gd.getNextBoolean();
  averageWhenDownsizing = gd.getNextBoolean();
  newWindow = gd.getNextBoolean();

  // update applicable Image Scale parameters
  setFieldValue("interpolationMethod", new Integer(interpolationMethod));
  setFieldValue("fillWithBackground", fillWithBackground);
  setFieldValue("averageWhenDownsizing", averageWhenDownsizing);
  setFieldValue("newWindow", newWindow);

  // compute width and height in pixels from DPI
  wPix = wInches * dpi;
  hPix = hInches * dpi;

  // rescale image
  arg = new StringBuilder();
  arg.append("x=- y=-");
  arg.append(" width=" + wPix);
  arg.append(" height=" + hPix);
  arg.append(" interpolation=" + methods[interpolationMethod]);
  if (fillWithBackground) arg.append(" fill");
  if (averageWhenDownsizing) arg.append(" average");
  if (newWindow) arg.append(" create title=" + title);
  IJ.run(imp, "Scale...", arg.toString());

  // update calibration
  newImp = IJ.getImage();
  pixelSize = 1 / dpi;
  cal = newImp.getCalibration();
  cal.setUnit("inch");
  cal.pixelWidth = pixelSize;
  cal.pixelHeight = pixelSize;
  cal.pixelDepth = 1;
  newImp.updateAndDraw();
}

function getFieldValue(fieldName) {
  c = Class.forName("ij.plugin.Scaler");
  f = c.getDeclaredField(fieldName);
  f.setAccessible(true);
  return f.get(null);
}

function setFieldValue(fieldName, value) {
  c = Class.forName("ij.plugin.Scaler");
  f = c.getDeclaredField(fieldName);
  f.setAccessible(true);
  f.set(null, value);
}
