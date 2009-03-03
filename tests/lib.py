#!/usr/bin/python

# This is a small library of function which should make testing Fiji/ImageJ
# much easier.

from ij import IJ, ImageJ

from java.awt import Frame, Toolkit

from java.awt.event import AWTEventListener

from threading import Lock

import sys

def startIJ():
	ImageJ().exitWhenQuitting(True)

def catchIJErrors(function):
	try:
		IJ.redirectErrorMessages()
		return function()
	except:
		logWindow = WindowManager.getFrame("Log")
		if not logWindow is None:
			error_message = logWindow.getTextPanel().getText()
			logWindow.close()
			return error_message

def test(function):
	result = catchIJErrors(function)
	if not result == None:
		print 'Failed:', function
		sys.exit(1)

def waitForFrame(title):
	all = Frame.getFrames()
	for i in range(0, len(all)):
		if all[i].getTitle() == title:
			return

	class Listener(AWTEventListener):
		lock = Lock()
		lock.acquire();
		def eventDispatched(self, event):
			source = event.getSource()
			if isinstance(source, Frame) and \
					source.getTitle() == title:
				self.lock.release()

	listener = Listener()
	Toolkit.getDefaultToolkit().addAWTEventListener(listener, -1)
	listener.lock.acquire()
	listener.lock.release()
	Toolkit.getDefaultToolkit().removeAWTEventListener(listener)

def quitIJ():
	IJ.getInstance().quit()
