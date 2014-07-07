from ij import IJ, WindowManager
from ij.gui import Toolbar
from time import sleep

w = 40
h = 40

def setColor(color):
	IJ.run('Colors...', 'foreground=' + color)

def square(i, j, currentX, currentY):
	IJ.runMacro('makeRectangle(' + str(w * i) + ', '
		+ str(h * j) + ', '
		+  str(w) + ', ' + str(h) + ');')
	if i == currentX and j == currentY:
		color = 'orange'
	elif (i + j) & 1 == 1:
		color = 'black'
	else:
		color = 'white'
	setColor(color)
	IJ.run('Fill')

Pawn = [18,4,11,6,9,10,10,14,15,16,6,30,
	33,30,24,16,28,14,29,10,26,5]

Pawn = [18,15,14,17,14,19,16,21,11,34,24,34,20,20,21,18,20,17]

Rook = [2,5,2,10,6,10,6,16,2,35,36,35,32,
	16,32,9,35,9,35,3,29,3,29,6,27,
	9,23,9,23,3,15,3,15,9,9,9,8,6,8,4]

Knight = [6,10,17,7,21,2,24,3,23,7,27,12,30,
	21,30,29,31,34,14,33,19,27,18,20,
	17,17,12,18,10,16,6,15,4,13]

Bishop = [17,3,15,5,17,6,13,8,12,12,13,14,
	15,16,11,34,8,34,8,36,28,36,28,33,
	25,34,21,16,23,13,22,8,18,6,19,4]

Queen = [20,5,21,3,20,1,18,3,18,5,14,5,15,
	7,14,11,18,11,13,31,13,33,25,33,25,
	31,20,11,24,11,23,7,24,5,21,5]

King = [17,2,19,2,19,4,21,4,21,6,19,6,19,8,
	22,8,22,12,19,12,19,15,23,17,24,22,
	23,27,20,30,20,31,23,31,23,32,14,31,
	13,30,17,30,14,27,13,22,15,17,16,15,
	16,12,13,12,13,8,16,8,16,6,14,6]

def path(i, j, array):
	macro = 'makePolygon('
	for k in range(0, len(array), 2):
		if k > 0:
			macro = macro + ', '
		macro = macro + str(i * w + array[k]) + ', ' + str(j * h + array[k + 1])
	macro += ');'
	IJ.runMacro(macro)

def parseCoord(coord):
	return (int(ord(coord[0]) - ord('a')),
		9 - int(coord[1]) - 1)

def draw(i, j, array, color):
	if color == "white":
		antiColor = "black"
	else:
		antiColor = "white"
	path(i, j, array)
	setColor(color)
	IJ.run("Fill")
	setColor(antiColor)
	IJ.run("Draw")
	IJ.run("Select None")

def drawCoord(coord, array, color):
	(i, j) = parseCoord(coord)
	draw(i, j, array, color)

def erase():
	i = WindowManager.getImageCount()
	while i > 0:
		WindowManager.getImage(WindowManager.getNthImageID(i)).close()
		i = i - 1

erase()

IJ.runMacro('newImage("Chess", "RGB", ' + str(w * 8) + ', '
	+ str(h * 8) + ', 1);')

def initial_field():
	return [ 'Rb', 'Nb', 'Bb', 'Qb', 'Kb', 'Bb', 'Nb', 'Rb',
		'Pb', 'Pb', 'Pb', 'Pb', 'Pb', 'Pb', 'Pb', 'Pb',
		'', '', '', '', '', '', '', '',
		'', '', '', '', '', '', '', '',
		'', '', '', '', '', '', '', '',
		'', '', '', '', '', '', '', '',
		'Pw', 'Pw', 'Pw', 'Pw', 'Pw', 'Pw', 'Pw', 'Pw',
		'Rw', 'Nw', 'Bw', 'Qw', 'Kw', 'Bw', 'Nw', 'Rw']

def get_array(name):
	if name == 'P':
		return Pawn
	elif name == 'R':
		return Rook
	elif name == 'N':
		return Knight
	elif name == 'B':
		return Bishop
	elif name == 'Q':
		return Queen
	elif name == 'K':
		return King

def draw_one(i, j, field, selectedX, selectedY):
	square(i, j, selectedX, selectedY)
	f = field[i + j * 8]
	if f != '':
		array = get_array(f[0])
		if f[1] == 'b':
			color = 'black'
		else:
			color = 'white'
		draw(i, j, array, color)

def draw_field(field, selectedX, selectedY):
	for j in range(0, 8):
		for i in range(0, 8):
			draw_one(i, j, field, selectedX, selectedY)

IJ.setTool(Toolbar.HAND)
field = initial_field()
currentX = -1
currentY = -1
draw_field(field, currentX, currentY)
canvas = WindowManager.getCurrentImage().getCanvas()
clicked = 0

while True:
	p = canvas.getCursorLoc()
	x = int(p.x / w)
	y = int(p.y / h)
	newClicked = canvas.getModifiers() & 16
	if clicked and not newClicked:
		if currentX >= 0:
			if x != currentX or y != currentY:
				oldOffset = currentX + 8 * currentY
				field[x + 8 * y] = field[oldOffset]
				field[oldOffset] = ''
			draw_one(currentX, currentY, field, -1, -1)
			draw_one(x, y, field, -1, -1)
			currentX = currentY = -1
		else:
			draw_one(x, y, field, x, y)
			currentX = x
			currentY = y
	clicked = newClicked
	sleep(0.1)
