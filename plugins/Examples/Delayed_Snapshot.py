# Take a snapshot after a delay specified in a dialog
# 
# The plugin has to fork, which is done by:
# 1 - declaring a function to do the work, 'snasphot'
# 2 - invoking the function via thread.start_new_thread,
#     which runs it in a separate thread.

from ij import IJ
from ij.gui import GenericDialog
import thread
import time

def snapshot(delay):
   time.sleep(delay)
   IJ.doCommand('Capture Screen')

gd = GenericDialog('Delay')
gd.addSlider('Delay (secs.): ', 0, 20, 5)
gd.showDialog()

if not gd.wasCanceled():
	# the 'extra' comma signals tuple, a kind of list in python.
	thread.start_new_thread(snapshot, (int(gd.getNextNumber()),))
