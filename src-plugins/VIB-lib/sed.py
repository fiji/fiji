#!/usr/bin/python

import re
import sys
import os

def sed(argv):
	if len(argv) < 3:
		print "Invalid invocation:", ' '.join(argv)
		sys.exit(1)

	base = argv[2]
	if base.startswith('src-plugins/VIB-lib/'):
		base = base[20:]
	if base in ["vib/FloatMatrix.java", "math3d/FloatMatrixN.java", \
			"math3d/JacobiFloat.java"]:
		replace = [r"double", r"float",
			r"FastMatrix", r"FloatMatrix",
			r"Double", r"Float",
			r"([0-9][0-9]*\.[0-9][0-9]*)(?!f)", r"\1f"]
	elif base in ["math3d/Eigensystem3x3Float.java", \
			"math3d/Eigensystem2x2Float.java"]:
		replace = [r"/\*change\*/double", r"float",
			r"Double", r"Float"]
	elif base == "util/FibonacciHeapInt.java":
		replace = [r"FibonacciHeap", r"FibonacciHeapInt",
			r" implements Comparable", r"",
			r"Comparable", r"int",
			r"\.compareTo\(([^\)]*)\)", r"- \1",
			r"Object other", r"int other",
			r"heap.add\(p, p\);",
				r"heap.add((int)prios[i], new Double((int)prios[i]));",
			r"Node\(null", r"Node(0"]

	if replace == None or len(replace) == 0 or (len(replace) % 2) != 0:
		print "Invalid replace array"
		sys.exit(1)

	dir = os.path.dirname(argv[0])
	for i in range(1,3):
		path = dir + '/' + argv[i]
		if not os.path.exists(argv[i]) and os.path.exists(path):
			argv[i] = path

	print 'Handling', argv[1], '->', argv[2]
	f = open(argv[1], 'r')
	buf = ''.join(f.readlines())
	f.close()

	for i in range(0, len(replace), 2):
		pattern = re.compile(replace[i])
		buf = pattern.sub(replace[i + 1], buf)

	f = open(argv[2], 'w')
	f.write(buf)
	f.close()

sed(sys.argv)
