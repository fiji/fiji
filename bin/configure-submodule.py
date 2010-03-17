#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

#
# configure-submodule.py
#

# Obtains submodules for Fiji and switches to the proper branch for each.
#
# Author: Curtis Rueden (ctrueden at wisc.edu)

import os, re, sys

from fiji.build import Fake
from java.lang.System import getProperty
from java.io import File, FileInputStream

# script prefix, for error messages
prefix = sys.argv[0] + ':'

# path to fiji environment
fijiPath = getProperty('fiji.dir')

# TODO - Edit Fake.java to allow access to entire ruleset. Then we can iterate
#        over all rules and discover the mappings, instead of hardcoding them.

# mapping from known submodules to corresponding targets
targets = {
  'AutoComplete':'jars/autocomplete.jar',
  'ImageJA':'jars/ij.jar',
  'RSyntaxTextArea':'jars/rsyntaxtextarea.jar',
  #'Retrotranslator':'',
  'TrakEM2':'plugins/TrakEM2_.jar',
  'VIB':'plugins/VIB_.jar',
  'batik':'jars/batik.jar',
  'bio-formats':'plugins/loci_tools.jar',
  'clojure':'jars/clojure.jar',
  'clojure-contrib':'jars/clojure-contrib.jar',
  'ij-plugins':'plugins/ij-ImageIO_.jar',
  'imglib':'jars/imglib.jar',
  'junit':'jars/junit-4.5.jar',
  'jython':'jars/jython.jar',
  #'live-helper':'',
  'mpicbg':'plugins/mpicbg_.jar',
  'tcljava':'jars/jacl.jar',
  'weka':'jars/weka.jar',
}

# inverse mapping from corresponding targets to known submodules
submodules = {}
for key in targets:
  target = targets[key]
  if target is not '':
    submodules[target] = key

# TODO - Switch ImageJA submodule to use master branch with Fiji by default.

# non-default branch to use for certain known submodules
branches = {
  'ImageJA':'fiji',
}

# updates an already-initialized submodule
def updateSubModule(submodule):
  print '=== Updating', submodule, '==='
  os.system('cd ' + fijiPath + ' && git pull')

# initializes a submodule
def cloneSubModule(submodule):
  print '=== Cloning', submodule, '==='
  # initialize submodule
  os.system('cd ' + fijiPath +
    ' && git submodule init ' + submodule +
    ' && git submodule update ' + submodule)
  chooseBranchSubModule(submodule)

# switches a submodule to the appropriate branch
def chooseBranchSubModule(submodule):
  submodulePath = fijiPath + '/' + submodule
  if submodule in branches:
    # HACK - special case for non-default branches
    branch = branches[submodule]
    print '=== Switching', submodule, 'to branch', branch, '==='
    os.system('cd ' + submodulePath +
      ' && git checkout -b ' + branch + ' origin/' + branch)
  else:
    # discover which branch to use from the local branches available by default
    handle = os.popen('cd ' + submodulePath +
      ' && git branch', 'r')
    branchList = handle.readlines()
    handle.close()
    branch = None
    for b in branchList:
      b = b.strip('* \t\n\r')
      if b != '(no branch)':
        branch = b
        break
    if branch is None:
      print prefix, 'Cannot identify branch for', submodule
    else:
      print '=== Switching', submodule, 'to branch', branch, '==='
      os.system('cd ' + submodulePath + ' && git checkout ' + branch)
  updateSubModule(submodule)

# set of already processed submodules (avoids circular dependency loops)
processed = {}

def processSubModule(submodule, parser):
  if submodule in processed:
    return
  processed[submodule] = 1

  # parse submodule
  handle = os.popen('cd ' + fijiPath +
    ' && git submodule ' + submodule + ' 2>&1', 'r')
  status = handle.readline()
  handle.close()

  # process prerequisite submodules first
  if submodule in targets:
    target = targets[submodule]
    if target is not None:
      rule = parser.getRule(target)
      if rule is not None:
        prereq = rule.getPrerequisiteString()
        if prereq is not None:
          for token in prereq.split():
            if token in submodules:
              pre = submodules[token]
              processSubModule(pre, parser)

  # do the appropriate thing for this submodule
  if re.match(r'\+([0-9a-f]){40}', status):
    updateSubModule(submodule)
  elif re.match(r'\-([0-9a-f]){40}', status):
    cloneSubModule(submodule)
  elif re.match(r' ([0-9a-f]){40}', status):
    chooseBranchSubModule(submodule)
  elif re.match(r'error: pathspec .* did not match', status):
    print prefix, 'No such submodule:', submodule
  elif re.match(r'^$', status):
    print prefix, submodule, 'is not a submodule'
  else:
    print prefix, 'Cannot process', submodule, '===', status

def main(argv=None):
  if argv is None:
    argv = sys.argv

  if len(argv) < 2:
    print "Usage: ./configure-submodule.py submodule [submodule2 ...]"
    return 1

  # extract module dependencies using Fake
  fake = Fake()
  fakefile = fijiPath + "/Fakefile"
  fis = FileInputStream(fakefile)
  parser = fake.parse(fis, File(fijiPath))
  parser.parseRules([])
  fis.close()

  # process each submodule specified
  for arg in argv[1:]:
    processSubModule(arg, parser)

if __name__ == "__main__":
  sys.exit(main())
