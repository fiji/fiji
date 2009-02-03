# Test whether any menu items contain pointers to non-existent classes
# which likely indicate a missconfiguration of a plugins.config file in a .jar plugin.

import ij
from ij import ImageJ, Menus
import java
from java.lang import Class, ClassNotFoundException, System, NoClassDefFoundError

# Launch ImageJ
ImageJ()

ok = 1

# Inspect each menu command
for it in ij.Menus.getCommands().entrySet().iterator():
  try:
    cl = it.value
    k = cl.find('(')
    if -1 != k:
      cl = cl[:k]
    #print "testing: ", cl
    cl = Class.forName(cl)
  except ClassNotFoundException:
    error = 0
    # Try without the first package name, since it may be fake
    # for plugins in subfolders of the plugins directory:
    k = cl.find('.')
    if -1 != k:
      try:
	# print 'Searching ij.IJ.getClassLoader for', cl
	cl = ij.IJ.getClassLoader().loadClass(cl[k+1:])
      except NoClassDefFoundError:
        error = 1
    else: error = 1
    if error:
      print 'ERROR: Class not found for menu command: ', it.key, '=>', it.value
      ok = 0

if ok:
    print "ok - Menu commands all correct."

System.exit(0)
