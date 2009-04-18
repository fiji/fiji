# Albert Cardona 20090418.
# Released under the General Public License v2.0
#
# Take snapshots of a user-specified window over time,
# and then make an image stack of of them all.
# Limited by RAM for speed, this plugin is intended for short recordings.

import thread
import time

from java.awt import Robot, Rectangle, Frame
from java.awt.image import BufferedImage

def snapshot(frame, box):
	bi = BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_RGB)
	g = bi.createGraphics()
	g.translate(-box.x, -box.y)
	frame.paint(g)
	return bi

def run(title):
	gd = GenericDialog('Record Window')
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
	gd.showDialog()
	if gd.wasCanceled():
		return
	n_frames = int(gd.getNextNumber())
	interval = gd.getNextNumber() / 1000.0 # in seconds
	frame = frames[gd.getNextChoiceIndex()]
	delay = int(gd.getNextNumber())

	snaps = []
	borders = None
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
		while len(snaps) < n_frames and last - start < n_frames * interval:
			now = System.currentTimeMillis() / 1000.0   # in seconds
			real_interval = now - last
			if real_interval >= interval:
				last = now
				snaps.append(snapshot(frame, box))
				intervals.append(real_interval)
			else:
				time.sleep(interval / 5)

		# debug:
		#print "insets:", insets
		#print "bounds:", bounds
		#print "box:", box
		#print "snap dimensions:", snaps[0].getWidth(), snaps[0].getHeight()

		# Create stack
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
		print e
		IJ.showStatus('')
		if borders is not None: borders.flush()
		for snap in snaps: snap.flush()

thread.start_new_thread(run, ("Do it",))

