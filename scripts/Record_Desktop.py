# Take a snapshot of the desktop every X miliseconds,
# and then make a stack out of it.
# Limited by RAM for speed, this plugin is intended
# for short recordings.

import thread
import time

from java.awt import Robot, Rectangle

def run(title):
	gd = GenericDialog('Record Desktop')
	gd.addNumericField('Max. frames:', 50, 0)
	gd.addNumericField('Milisecond interval:', 300, 0)
	gd.addSlider('Start in (seconds):', 0, 20, 5)
	gd.showDialog()
	if gd.wasCanceled():
		return
	n_frames = int(gd.getNextNumber())
	interval = gd.getNextNumber() / 1000.0 # in seconds
	delay = int(gd.getNextNumber())
	
	snaps = []

	try:
		while delay > 0:
			IJ.showStatus('Starting in ' + str(delay) + 's.')
			time.sleep(1) # one second
			delay -= 1
		IJ.showStatus('')
		System.out.println("Starting...")
		# start capturing
		robot = Robot()
		box = Rectangle(IJ.getScreenSize())
		start = System.currentTimeMillis() / 1000.0 # in seconds
		last = start
		intervals = []
		real_interval = 0
		# Initial shot
		snaps.append(robot.createScreenCapture(box))
		while len(snaps) < n_frames and last - start < n_frames * interval:
			now = System.currentTimeMillis() / 1000.0 # in seconds
			real_interval = now - last
			if real_interval >= interval:
				last = now
				snaps.append(robot.createScreenCapture(box))
				intervals.append(real_interval)
			else:
				time.sleep(interval / 5) # time in seconds
		# Create stack
		System.out.println("End")
		awt = snaps[0]
		stack = ImageStack(awt.getWidth(None), awt.getHeight(None), None)
		t = 0
		for snap,real_interval in zip(snaps,intervals):
			stack.addSlice(str(IJ.d2s(t, 3)), ImagePlus('', snap).getProcessor())
			snap.flush()
			t += real_interval

		ImagePlus("Desktop recording", stack).show()
	except Exception, e:
		print "Some error ocurred:"
		print e
		for snap in snaps: snap.flush()

thread.start_new_thread(run, ("Do it",))
