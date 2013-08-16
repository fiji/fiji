import ij._
println ("Hello from external script :-)")
IJ.log("test ij")
IJ.log(IJ.getVersion())
val imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif")
imp.show()
IJ.wait(2000)
imp.close()
