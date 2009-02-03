// Draws concentric rings running out from
// the center of the image window.
// Based on the bullseye.js script distributed
// with  Kas Thomas' JavaScript Editor plugin at
// http://rsb.info.nih.gov/ij/plugins/javascript.html

  requires("1.29m");
  w = 512; h = 512;
  start = getTime();
  xcenter = w/2; ycenter = h/2;
  run("New...", "name='Bulls Eye' type='32-bit RGB'  width="+w+" height="+h);
  for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
          dx = x - xcenter; 
          dy = y - ycenter;
          d = sqrt(dx*dx + dy*dy)/10;
          v = (sin(d)+1)*128;
          putPixel(x, y, v<<16);
      }     
      if (y%25==0) updateDisplay();
  }
  showStatus(round((w*h)/((getTime()-start)/1000)) + " pixels/sec");

