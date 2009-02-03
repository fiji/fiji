from ij import IJ, ImageJ

ImageJ()
IJ.run("MRI Stack (528K)")
IJ.runMacro("setVoxelSize(1, 1, 4, 'mm')")
IJ.run("3D Viewer", "display=Volume color=None threshold=0 resampling=1 red green blue")

