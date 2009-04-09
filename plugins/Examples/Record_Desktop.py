# Take a snapshot of the desktop every X miliseconds,
# and then make a stack out of it.
# Limited by RAM for speed, this plugin is intended
# for short recordings.

import thread
import time

from java.awt import Robot, Rectangle

def run(title):
	gd = GenericDialog('Delay')
	gd.addNumericField('Frames:', 50, 0)
	gd.addNumericField('Milisecond interval:', 300, 0)
	gd.showDialog()
	if gd.wasCanceled():
		return
	n_frames = int(gd.getNextNumber())
	interval = int(gd.getNextNumber() / 1000.0)
	try:
		System.out.println("Starting...")
		# start capturing
		robot = Robot()
		box = Rectangle(IJ.getScreenSize())
		last = System.currentTimeMillis()
		snaps = []
		intervals = []
		real_interval = 0
		snaps.append(robot.createScreenCapture(box))
		for i in range(n_frames-1):
			now = System.currentTimeMillis()
			real_interval = now - last
			if real_interval >= interval:
				last = now
				snaps.append(robot.createScreenCapture(box))
				intervals.append(real_interval)
				time.sleep(interval / 3) # time in seconds
		# Create stack
		System.out.println("End")
		awt = snaps[0]
		stack = ImageStack(awt.getWidth(None), awt.getHeight(None), None)
		for snap,real_interval in zip(snaps,intervals):
			stack.addSlice(str(real_interval), ImagePlus('', snap).getProcessor())

		ImagePlus("Desktop recording", stack).show()
	except Exception, e:
		print "Some error ocurred:"
		print e

thread.start_new_thread(run, ("Do it",))
