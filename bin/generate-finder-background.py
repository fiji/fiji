from ij import IJ, ImagePlus
from ij.gui import Toolbar, Roi
from ij.process import Blitter
from java.awt import Color, Font, Polygon, Rectangle

w = 472
h = 354
xOffset = 59
yOffset = 120
radius = 20
arrowThickness = 14
fontName = 'Arial'
upperFontSize = 18
lowerFontSize = 10

image = IJ.createImage("MacOSX background picture", "rgb", w, h, 1)

# background
Toolbar.setForegroundColor(Color(0x5886ea))
Toolbar.setBackgroundColor(Color(0x3464c9))
IJ.run(image, "Radial Gradient", "")

# rounded rectangle
# correct for MacOSX bug: do the rounded rectangle in another image
image2 = image
image = IJ.createImage("MacOSX background picture", "rgb", w, h, 1)
image.setRoi(Roi(xOffset, yOffset, w - 2 * xOffset, h - 2 * yOffset))
IJ.run(image, "Make rectangular selection rounded", "radius=" + str(radius))
Toolbar.setForegroundColor(Color(0x435a96))
Toolbar.setBackgroundColor(Color(0x294482))
IJ.run(image, "Radial Gradient", "")
ip = image.getProcessor()
ip.setColor(0x0071bc)
ip.setLineWidth(2)
image.getRoi().drawPixels(ip)
Roi.setPasteMode(Blitter.COPY_TRANSPARENT)
#grow = image.getRoi().getClass().getSuperclass().getDeclaredMethod('growConstrained', [Integer.TYPE, Integer.TYPE])
#grow.setAccessible(True)
#grow.invoke(image.getRoi(), [1, 1])
image.copy(True)
image = image2
image.paste()
image.killRoi()
ip = image.getProcessor()

# arrow
ip.setColor(0x123558)
arrowLength = int(arrowThickness * 2.5)
arrowWidth = arrowThickness * 2
x1 = (w - arrowLength) / 2
x2 = x1 + arrowLength
x3 = x2 - arrowThickness
y1 = (h - arrowThickness) / 2
y2 = y1 + arrowThickness
y3 = (h - arrowWidth) / 2
y4 = y3 + arrowWidth
y5 = h / 2
polygon = Polygon(
	[x1, x1, x3, x3, x2, x3, x3],
	[y1, y2, y2, y4, y5, y3, y1], 7)
ip.fillPolygon(polygon)

# upper text
# work around an ImageJ bug: anti-aliased text is always black
ip.setAntialiasedText(True)
ip.invert()

ip.setJustification(ip.CENTER_JUSTIFY)
ip.setFont(Font(fontName, Font.BOLD, upperFontSize))
ip.drawString('Fiji is just ImageJ - batteries included',
	int(w / 2), int((yOffset + upperFontSize) / 2))
ip.setFont(Font(fontName, Font.BOLD, lowerFontSize))
ip.drawString('To install, drag Fiji to your Applications folder',
	int(w / 2), h - yOffset + lowerFontSize * 2)
ip.drawString('If you cannot write to the Applications folder,',
	int(w / 2), h - yOffset + lowerFontSize * 4)
ip.drawString('drag Fiji to another folder, e.g. onto your Desktop',
	int(w / 2), h - yOffset + lowerFontSize * 5)
ip.invert()

IJ.saveAs(image, "jpeg", "resources/install-fiji.jpg")
