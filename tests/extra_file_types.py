#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

# Test whether HandleExtraFileTypes.java tryPlugIn(<classname>) calls
# will succeed, by testing if the class is in the classpath or in the plugins folder.

from ij import IJ, ImageJ, Prefs
from java.lang import Class, ClassNotFoundException, System, NoClassDefFoundError

# Launch ImageJ
ImageJ()
print "Testing HandleExtraFileTypes.java:"

# Try both system and IJ class loaders
def checkClassName(name):
  try:
    cl = Class.forName(name)
    return 1
  except ClassNotFoundException:
    try:
      cl = IJ.getClassLoader().loadClass(name)
      print "found :", cl
      return 1
    except NoClassDefFoundError:
      return 0
    except ClassNotFoundException:
      return 0

path = System.getProperty('ij.dir') + '/' + "src-plugins/IO_/"
print "path: ", path
f = open(path + "HandleExtraFileTypes.java")

error = 0

try:
  count = 0
  for line in f.readlines():
    count += 1
    #if not 'tryPlugIn' in line: continue
    itp = line.find('tryPlugIn')
    if -1 == itp: continue
    istart = line.find('"', itp + 8)
    if -1 == istart: continue
    istart += 1
    iend = line.find('"', istart)
    if -1 == iend:
      print 'Unclosed colon at line ', str(count), ':', line.strip()
      continue
    name = line[istart:iend]
    if not checkClassName(name):
      print 'Class not found: ', name, 'at line', str(count), ':', line.strip()
      error = 1
except error:
  print error
  print "Some error ocurred while parsing HandleExtraFileTypes.java"
  error = 1

f.close()

if not error:
  print 'ok - All classes in tryPlugIn(<classname>) in HandleExtraFileTypes exist'

System.exit(0)
