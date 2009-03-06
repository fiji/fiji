from ij import IJ, ImageJ

import lib

lib.startIJ()
lib.test(lambda: IJ.run("Fiji Logo 3D"))
lib.waitForFrame("ImageJ 3D Viewer")
lib.quitIJ()
