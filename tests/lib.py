# This is a small library of function which should make testing Fiji/ImageJ
# much easier.

from jarray import zeros
from threading import Lock
from sys import exit, stderr
from os.path import realpath

from fiji import Main
from ij import IJ, ImageJ
from java.awt import Button, Container, Dialog, Frame, Toolkit
from java.awt.event import ActionEvent, MouseEvent
from java.io import File
from java.lang import Runtime, System, Thread

currentWindow = None
def startIJ():
	Main.premain()
	global currentWindow
	currentWindow = ImageJ()
	currentWindow.exitWhenQuitting(True)
	Main.postmain()

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
		exit(1)

def waitForWindow(title):
	global currentWindow
	currentWindow = Main.waitForWindow(title)
	return currentWindow

def getMenuEntry(menuBar, path):
	if menuBar == None:
		global currentWindow
		menuBar = currentWindow.getMenuBar()
	if isinstance(path, str):
		path = path.split('>')
	try:
		menu = None
		for i in range(0, menuBar.getMenuCount()):
			if path[0] == menuBar.getMenu(i).getLabel():
				menu = menuBar.getMenu(i)
				break
		for j in range(1, len(path)):
			entry = None
			for i in range(0, menu.getItemCount()):
				if path[j] == menu.getItem(i).getLabel():
					entry = menu.getItem(i)
					break
			menu = entry
		return menu
	except:
		return None

def dispatchActionEvent(component):
	event = ActionEvent(component, ActionEvent.ACTION_PERFORMED, \
		component.getLabel(), MouseEvent.BUTTON1)
	component.dispatchEvent(event)

def clickMenuItem(path):
	menuEntry = getMenuEntry(None, path)
	dispatchActionEvent(menuEntry)

def getButton(container, label):
	if container == None:
		global currentWindow
		container = currentWindow
	components = container.getComponents()
	for i in range(0, len(components)):
		if isinstance(components[i], Container):
			result = getButton(components[i], label)
			if result != None:
				return result
		elif isinstance(components[i], Button) and \
				components[i].getLabel() == label:
			return components[i]

def clickButton(label):
	button = getButton(None, label)
	dispatchActionEvent(button)

def quitIJ():
	global currentWindow
	IJ.getInstance().quit()
	currentWindow = None

class OutputThread(Thread):
	def __init__(self, input, output):
		self.buffer = zeros(65536, 'b')
		self.input = input
		self.output = output

	def run(self):
		while True:
			count = self.input.read(self.buffer)
			if count < 0:
				return
			self.output.write(self.buffer, 0, count)

def launchProgramNoWait(args, workingDir = None):
	if workingDir != None and not isinstance(workingDir, File):
		workingDir = File(workingDir)
	process = Runtime.getRuntime().exec(args, None, workingDir)
	OutputThread(process.getInputStream(), System.out).start()
	OutputThread(process.getErrorStream(), System.err).start()
	return process

def launchProgram(args, workingDir = None):
	process = launchProgramNoWait(args, workingDir)
	return process.waitFor()

def launchFiji(args, workingDir = None):
	args.insert(0, realpath(System.getProperty('fiji.executable')))
	try:
		launchProgram(args, workingDir)
	except:
		return -1
