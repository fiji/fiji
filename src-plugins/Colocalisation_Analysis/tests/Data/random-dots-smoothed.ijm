// This macro draws randomly positioned dots on the active
// image in the current foreground color, then gaussian smooths them.

dotSize = 3;
numberOfDots = 5000;
  width = getWidth();
  height = getHeight();
  for (i=0; i<numberOfDots; i++) {
      x = random()*width-dotSize/2;
      y = random()*height-dotSize/2;
      makeOval(x, y, dotSize, dotSize);
      run("Fill");
      run("Select None");
   }
   run("Gaussian Blur...", "sigma=1");