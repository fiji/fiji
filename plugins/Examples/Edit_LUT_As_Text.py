import jarray
from java.awt import Font, Menu, MenuItem
from java.awt.event import ActionListener
from java.awt.image import IndexColorModel

# Call this script to show the current Lookup Table in an editor.
# The user can edit it, and call Lookup Table>Set Lookup Table after editing
# the numbers.

def editLUTAsText():
	image = WindowManager.getCurrentImage()
	if image == None:
		IJ.error('Need an image')
		return
	ip = image.getProcessor()
	cm = ip.getCurrentColorModel()
	if not hasattr(cm, 'getMapSize'):
		IJ.error('Need an 8-bit color image')
		return

	size = cm.getMapSize()
	if size > 256:
		IJ.error('Need an 8-bit color image')
		return
	reds = jarray.zeros(size, 'b')
	greens = jarray.zeros(size, 'b')
	blues = jarray.zeros(size, 'b')
	cm.getReds(reds)
	cm.getGreens(greens)
	cm.getBlues(blues)

	def color(array, index):
		value = array[index]
		if value < 0:
			value += 256
		return '% 4d' % value

	text = ''
	for i in range(0, size):
		text = text + color(reds, i) + ' ' + color(greens, i) + ' ' \
			+ color(blues, i) + "\n"

	editor = Editor(25, 80, 12, Editor.MONOSPACED | Editor.MENU_BAR)
	editor.create('Lookup Table', text)

	def string2byte(string):
		value = int(string)
		if value > 127:
			value -= 256
		if value < -128:
			value = 128
		return value

	class SetLookupTable(ActionListener):
		def actionPerformed(self, event):
			text = editor.getText()
			i = 0
			for line in text.split("\n"):
				colors = line.split()
				if len(colors) < 3:
					continue
				reds[i] = string2byte(colors[0])
				greens[i] = string2byte(colors[1])
				blues[i] = string2byte(colors[2])
				i += 1
			cm = IndexColorModel(8, 256, reds, greens, blues)
			ip.setColorModel(cm)
			image.updateAndRepaintWindow()

	menuItem = MenuItem('Set Lookup Table')
	menuItem.addActionListener(SetLookupTable())

	menu = Menu('Lookup Table')
	menu.add(menuItem)

	menuBar = editor.getMenuBar()
	for i in range(menuBar.getMenuCount() - 1, -1, -1):
		label = menuBar.getMenu(i).getLabel()
		if label == 'Macros' or label == 'Debug':
			menuBar.remove(i)
	menuBar.add(menu)

editLUTAsText()
