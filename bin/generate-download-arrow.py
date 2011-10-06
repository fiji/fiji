from ij import IJ
from ij.gui import ShapeRoi
from java.awt import Color, Polygon
from java.awt.geom import PathIterator

w = int(36)
h = int(42)
lineWidth = 2
arrowWidth = 16

image = IJ.createImage('Download arrow', 'rgb', w, h, 1)
ip = image.getProcessor()
ip.setLineWidth(lineWidth)
ip.setColor(Color(0x65a4e3))
roi = ShapeRoi([PathIterator.SEG_MOVETO, 0, 0,
	PathIterator.SEG_LINETO, w, 0,
	PathIterator.SEG_LINETO, w, w,
	PathIterator.SEG_LINETO, 0, w,
	PathIterator.SEG_CLOSE])
lw = lineWidth
roi = roi.not(ShapeRoi([PathIterator.SEG_MOVETO, lw, lw,
	PathIterator.SEG_LINETO, w - lw, lw,
	PathIterator.SEG_LINETO, w - lw, w - lw,
	PathIterator.SEG_LINETO, lw, w - lw,
	PathIterator.SEG_CLOSE]))
x1 = (w - arrowWidth) / 2
x2 = (w + arrowWidth) / 2
y1 = w * 2 / 3
roi = roi.or(ShapeRoi([PathIterator.SEG_MOVETO, x1, 0,
	PathIterator.SEG_LINETO, x1, y1,
	PathIterator.SEG_LINETO, 0, y1,
	PathIterator.SEG_LINETO, w / 2 - 1, h,
	PathIterator.SEG_LINETO, w / 2, h,
	PathIterator.SEG_LINETO, w - 1, y1,
	PathIterator.SEG_LINETO, x2, y1,
	PathIterator.SEG_LINETO, x2, 0,
	PathIterator.SEG_CLOSE]))
ip.fill(roi)
IJ.saveAs(image, "PNG", "resources/download-arrow.png")