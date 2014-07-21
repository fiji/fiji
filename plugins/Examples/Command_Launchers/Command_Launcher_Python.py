from java.awt import Color
from java.awt.event import TextListener
from ij import IJ
from ij import Menus
from ij.gui import GenericDialog

#commands = [c for c in ij.Menus.getCommands().keySet()]
# Above, equivalent list as below:
commands = Menus.getCommands().keySet().toArray()
gd = GenericDialog('Command Launcher')
gd.addStringField('Command: ', '');
prompt = gd.getStringFields().get(0)
prompt.setForeground(Color.red)

class TypeListener(TextListener):
	def textValueChanged(self, tvc):
		if prompt.getText() in commands:
			prompt.setForeground(Color.black)
			return
		prompt.setForeground(Color.red)
		# or loop:
		#for c in commands:
		#	if c == text:
		#		prompt.setForeground(Color.black)
		#		return
		#
		#prompt.setForeground(Color.red)

prompt.addTextListener(TypeListener())
gd.showDialog()
if not gd.wasCanceled(): IJ.doCommand(gd.getNextString())

# This python version does not encapsulate the values of the variables, so they are all global when defined outside the class definition.
# In contrast, the lisp 'let' definitions encapsulates them in full
# As an advantage, each python script executes within its own namespace, whereas clojure scripts run all within a unique static interpreter.
