#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import lib
from java.awt import AWTEvent, Button, Dialog, Frame, Menu, MenuBar, MenuItem
from java.awt import Toolkit, Window
from java.awt.event import AWTEventListener
from java.awt.event import ActionEvent, ContainerEvent, ComponentEvent
from java.awt.event import FocusEvent
from java.awt.event import HierarchyEvent, InputMethodEvent, MouseEvent
from java.awt.event import PaintEvent, WindowEvent

verbose = False

lib.startIJ()

def record(function, argument):
	print 'lib.' + function + "('" + argument + "')"

def getMenuPath(menuItem):
	result = ''
	while not isinstance(menuItem, MenuBar):
		if result != '':
			result = '>' + result
		result = menuItem.getLabel() + result
		menuItem = menuItem.getParent()
	if isinstance(menuItem.getParent(), Frame):
		record('waitForWindow', menuItem.getParent().getTitle())
	record('clickMenuItem', result)

def getButton(button):
	label = button.getLabel()
	while button != None:
		if isinstance(button, Frame) or isinstance(button, Dialog):
			record('waitForWindow', button.getTitle())
			break
		button = button.getParent()
	record('clickButton', label)

class Listener(AWTEventListener):
	def eventDispatched(self, event):
		if isinstance(event, ContainerEvent) or \
				isinstance(event, HierarchyEvent) or \
				isinstance(event, InputMethodEvent) or \
				isinstance(event, MouseEvent) or \
				isinstance(event, PaintEvent):
			return
		if isinstance(event, ActionEvent):
			if isinstance(event.getSource(), MenuItem):
				getMenuPath(event.getSource())
			elif isinstance(event.getSource(), Button):
				getButton(event.getSource())
			else:
				print 'Unknown action event:', event
		elif (event.getID() == FocusEvent.FOCUS_GAINED and \
				    isinstance(event, Window)) or \
				event.getID() == WindowEvent.WINDOW_OPENED:
			record('waitForWindow', event.getSource().getTitle())
		else:
			global verbose
			if verbose:
				print 'event', event, 'from source', \
					event.getSource()

listener = Listener()
Toolkit.getDefaultToolkit().addAWTEventListener(listener, -1)

print 'import lib'
print ''
print 'lib.startIJ()'
