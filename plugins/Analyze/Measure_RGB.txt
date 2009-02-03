// "MeasureRGB"
// This macro demonstrates how to separately measure
// the red, green and blue channels of an RGB image.

  requires("1.35b");
  if (bitDepth!=24)
     exit("This macro requires an RGB image");
  setRGBWeights(1, 0, 0);
  run("Measure");
  setResult("Label", nResults-1, "Red");
  setRGBWeights(0, 1, 0);
  run("Measure");
  setResult("Label", nResults-1, "Green");
  setRGBWeights(0, 0, 1);
  run("Measure");
  setResult("Label", nResults-1, "Blue");
  setRGBWeights(1/3, 1/3, 1/3);
  run("Measure");
  setResult("Label", nResults-1, "(R+G+B)/3");
  // weights uses in ImageJ 1.31 and earlier
  setRGBWeights(0.299, 0.587, 0.114);
  run("Measure");
  setResult("Label", nResults-1, "0.299R+0.587G+0.114B");
  updateResults();
