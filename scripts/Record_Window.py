# Albert Cardona 20090418.
# Released under the General Public License v2.0
#
# Take snapshots of a user-specified window over time,
# and then make an image stack of of them all.
# 
# In the dialog, 0 frames mean infinite recording, to be interrupted by ESC
# pressed on the ImageJ toolbar or other frames with the same listener.
# 
# If not saving to file, then you are limited to RAM.
#
# When done, a stack or a virtual stack opens.

import thread
import time
import sys

from java.awt import Robot, Rectangle, Frame
from java.awt.image import BufferedImage
from javax.swing import SwingUtilities
from java.io import File, FilenameFilter
from java.util.concurrent import Executors
from java.util import Arrays

class PrintAll(Runnable):
	def __init__(self, frame, g):
		self.frame = frame
		self.g = g
	def run(self):
		self.frame.printAll(self.g)

def snapshot(frame, box):
	bi = BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_RGB)
	g = bi.createGraphics()
	g.translate(-box.x, -box.y)
	#all black! # frame.paintAll(g)
	#only swing components! # frame.paint(g)
	#only swing components! # frame.update(g)
	#together, also only swing and with errors
	##frame.update(g)
	##frame.paint(g)
	# locks the entire graphics machinery # frame.printAll(g)
	# Finally, the right one:
	SwingUtilities.invokeAndWait(PrintAll(frame, g))
	return bi

class Saver(Runnable):
	def __init__(self, i, dir, bounds, borders, img, insets):
		self.i = i
		self.dir = dir
		self.bounds = bounds
		self.borders = borders
		self.img = img
		self.insets = insets
	def run(self):
		System.out.println("run")
		# zero-pad up to 10 digits
		bi = None
		try:
			title = str(self.i)
			while len(title) < 10:
				title = '0' + title
			bi = BufferedImage(self.bounds.width, self.bounds.height, BufferedImage.TYPE_INT_RGB)
			g = bi.createGraphics()
			g.drawImage(self.borders, 0, 0, None)
			g.drawImage(self.img, self.insets.left, self.insets.top, None)
			FileSaver(ImagePlus(title, ColorProcessor(bi))).saveAsTiff(self.dir + title + '.tif')
		except Exception, e:
			print e
			e.printStackTrace()
		if bi is not None: bi.flush()
		self.img.flush()

class TifFilter(FilenameFilter):
	def accept(self, dir, name):
		return name.endswith('.tif')

def run(title):
	gd = GenericDialog('Record Window')
	gd.addMessage("Maximum number of frames to record.\nZero means infinite, interrupt with ESC key.")
	gd.addNumericField('Max. frames:', 50, 0)
	gd.addNumericField('Milisecond interval:', 300, 0)
	gd.addSlider('Start in (seconds):', 0, 20, 5)
	frames = []
	titles = []
	for f in Frame.getFrames():
		if f.isEnabled() and f.isVisible():
			frames.append(f)
			titles.append(f.getTitle())
	gd.addChoice('Window:', titles, titles[0])
	gd.addCheckbox("To file", False)
	gd.showDialog()
	if gd.wasCanceled():
		return
	n_frames = int(gd.getNextNumber())
	interval = gd.getNextNumber() / 1000.0 # in seconds
	frame = frames[gd.getNextChoiceIndex()]
	delay = int(gd.getNextNumber())
	tofile = gd.getNextBoolean()

	dir = None
	if tofile:
		dc = DirectoryChooser("Directory to store image frames")
		dir = dc.getDirectory()
		if dir is None:
			return # dialog canceled

	snaps = []
	borders = None
	executors = Executors.newFixedThreadPool(1)
	try:
		while delay > 0:
			IJ.showStatus('Starting in ' + str(delay) + 's.')
			time.sleep(1) # one second
			delay -= 1

		IJ.showStatus('Capturing frame borders...')
		bounds = frame.getBounds()
		robot = Robot()
		frame.toFront()
		time.sleep(0.5) # half a second
		borders = robot.createScreenCapture(bounds)

		IJ.showStatus("Recording " + frame.getTitle())

		# Set box to the inside borders of the frame
		insets = frame.getInsets()
		box = bounds.clone()
		box.x = insets.left
		box.y = insets.top
		box.width -= insets.left + insets.right
		box.height -= insets.top + insets.bottom

		start = System.currentTimeMillis() / 1000.0 # in seconds
		last = start
		intervals = []
		real_interval = 0
		i = 1
		fus = None
		if tofile:
			fus = []

		# 0 n_frames means continuous acquisition
		while 0 == n_frames or (len(snaps) < n_frames and last - start < n_frames * interval):
			now = System.currentTimeMillis() / 1000.0   # in seconds
			real_interval = now - last
			if real_interval >= interval:
				last = now
				img = snapshot(frame, box)
				if tofile:
					fus.append(executors.submit(Saver(i, dir, bounds, borders, img, insets))) # will flush img
					i += 1
				else:
					snaps.append(img)
				intervals.append(real_interval)
			else:
				time.sleep(interval / 5)
			# interrupt capturing:
			if IJ.escapePressed():
				IJ.showStatus("Recording user-interrupted")
				break

		# debug:
		#print "insets:", insets
		#print "bounds:", bounds
		#print "box:", box
		#print "snap dimensions:", snaps[0].getWidth(), snaps[0].getHeight()

		# Create stack
		stack = None;
		if tofile:
			for fu in snaps: fu.get() # wait on all
			stack = VirtualStack(bounds.width, bounds.height, None, dir)
			files = File(dir).list(TifFilter())
			Arrays.sort(files)
			for f in files:
				stack.addSlice(f)
		else:
			stack = ImageStack(bounds.width, bounds.height, None)
			t = 0
			for snap,real_interval in zip(snaps,intervals):
				bi = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB)
				g = bi.createGraphics()
				g.drawImage(borders, 0, 0, None)
				g.drawImage(snap, insets.left, insets.top, None)
				stack.addSlice(str(IJ.d2s(t, 3)), ImagePlus('', bi).getProcessor())
				t += real_interval
				snap.flush()
				bi.flush()

		borders.flush()

		ImagePlus(frame.getTitle() + " recording", stack).show()
		IJ.showStatus('Done recording ' + frame.getTitle())
	except Exception, e:
		print "Some error ocurred:"
		print e.printStackTrace()
		IJ.showStatus('')
		if borders is not None: borders.flush()
		for snap in snaps: snap.flush()
	
	executors.shutdown()

thread.start_new_thread(run, ("Do it",))

