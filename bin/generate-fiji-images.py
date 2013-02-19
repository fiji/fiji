#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

import sys

# .svg
from org.apache.batik.bridge \
	import BridgeContext, DocumentLoader, GVTBuilder, \
		UserAgent, UserAgentAdapter
from org.apache.batik.gvt.renderer import StaticRenderer
from org.w3c.dom import Document, Element

from java.awt import Rectangle
from java.awt.geom import AffineTransform
from java.awt.image import BufferedImage

# .ico
from net.sf.image4j.codec.ico import ICOEncoder

# .icns
from iconsupport.icns import IcnsCodec, IconSuite
from java.io import File, FileOutputStream

# .png
from javax.imageio import ImageIO

# .tif
from ij import ImagePlus
from ij.io import FileSaver

input = 'images/fiji-logo-1.0.svg'
ico = 'images/fiji.ico'
icns = 'Contents/Resources/Fiji.icns'

# load .svg

user_agent = UserAgentAdapter()
loader = DocumentLoader(user_agent)
context = BridgeContext(user_agent, loader)
user_agent.setBridgeContext(context)
document = loader.loadDocument(File(input).toURI().toString())
root = document.getRootElement()
svg_x = root.getX().getBaseVal().getValue()
svg_y = root.getY().getBaseVal().getValue()
svg_width = root.getWidth().getBaseVal().getValue()
svg_height = root.getHeight().getBaseVal().getValue()

def generate_image(width, height):
	renderer = StaticRenderer()
	renderer.setTree(GVTBuilder().build(context, document))
	transform = AffineTransform()
	transform.translate(-svg_x, -svg_y)
	transform.scale(width / svg_width, height / svg_height)
	renderer.setTransform(transform)
	renderer.updateOffScreen(width, height)
	renderer.repaint(Rectangle(0, 0, width, height))
	return renderer.getOffScreen()

# make .ico

def make_ico(ico):
	list = []
	for width in [ 256, 48, 32, 24, 16 ]:
		list.append(generate_image(width, width))
	ICOEncoder.write(list, File(ico))

# make .icns

def make_icns(icns):
	icons = IconSuite()
	icons.setSmallIcon(generate_image(16, 16))
	icons.setLargeIcon(generate_image(32, 32))
	icons.setHugeIcon(generate_image(48, 48))
	icons.setThumbnailIcon(generate_image(128, 128))
	codec = IcnsCodec()
	out = FileOutputStream(icns)
	codec.encode(icons, out)
	out.close()

def make_tiff(width, height, file):
	image = generate_image(width, height)
	FileSaver(ImagePlus("", image)).saveAsTiff(file)

def make_image(width, height, file, type):
	image = generate_image(width, height)
	ImageIO.write(image, type, File(file))

def extract_dimensions(filename):
	dot = filename.rfind('.')
	if dot < 0:
		dot = len(filename)
	x = filename.rfind('x', 0, dot)
	if x < 0:
		raise 'No dimensions found: ' + filename
	minus = filename.rfind('-', 0, x)
	if minus < 0:
		raise 'No dimensions found: ' + filename
	return [int(filename[minus + 1:x]), int(filename[x + 1:dot])]

if len(sys.argv) > 1:
	for file in sys.argv[1:]:
		if file.endswith('.ico'):
			make_ico(file)
		elif file.endswith('.icns'):
			make_icns(file)
		elif file.endswith('.png'):
			[width, height] = extract_dimensions(file)
			make_image(width, height, file, 'png')
		elif file.endswith('.jpg') or file.endswith('.jpeg'):
			[width, height] = extract_dimensions(file)
			make_image(width, height, file, 'jpg')
		elif file.endswith('.tiff') or file.endswith('.tif'):
			[width, height] = extract_dimensions(file)
			make_tiff(width, height, file)
		else:
			print 'Ignoring unknown file type:', file
else:
	make_ico('images/fiji.ico')
	make_icns('Contents/Resources/Fiji.icns')
