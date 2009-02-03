from jarray import zeros
from java.lang import *

def findRootThreadGroup():
	tg = Thread.currentThread().getThreadGroup()
	root_tg = tg.getParent()
	root_tg = tg
	parent = root_tg.getParent()
	while None != parent:
		root_tg = parent
		parent = parent.getParent()
	return root_tg

def listGroup(list, group):
	threads = zeros(group.activeCount(), Thread)
	group.enumerate(threads, 0)
	groups = zeros(group.activeGroupCount(), ThreadGroup)
	group.enumerate(groups, 0)
	for t in threads:
		if None is not t: list.append(t.getName())
	for g in groups:
		if None is not g: listGroup(list, g)

def listThreadNames():
	list = []
	listGroup(list, findRootThreadGroup())
	return list

IJ.log("Threads:")
i = 1
for thread in listThreadNames():
	IJ.log(str(i) + ": " + thread)
	i += 1

