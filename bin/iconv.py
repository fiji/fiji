#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

from java.io import File, FileInputStream, FileOutputStream
from java.nio import ByteBuffer
from java.nio.charset import Charset
import sys

if sys.argv[1] == '-f':
	decoder = Charset.forName(sys.argv[2]).newDecoder()
	sys.argv[1:] = sys.argv[3:]
elif sys.argv[1] == '-l':
	for name in Charset.availableCharsets().keySet():
		print name
	sys.exit(0)
else:
	decoder = Charset.forName("ISO-8859-1").newDecoder()
encoder = Charset.forName("UTF-8").newEncoder()

def iconv(file):
	print 'Converting', file
	f = File(file)
	if not f.exists():
		print file, 'does not exist'
		sys.exit(1)
	buffer = ByteBuffer.allocate(f.length() * 2)
	input = FileInputStream(f)
	input.getChannel().read(buffer)
	buffer.limit(buffer.position())
	buffer.position(0)
	if buffer.limit() != f.length():
		print file, 'could not be read completely'
		sys.exit(1)
	input.close()
	buffer = encoder.encode(decoder.decode(buffer))
	buffer.position(0)
	output = FileOutputStream(file + '.cnv')
	if output.getChannel().write(buffer) != buffer.limit():
		print file, 'could not be reencoded'
		sys.exit(1)
	output.close()
	f.delete()
	File(file + '.cnv').renameTo(f)

for file in sys.argv[1:]:
	iconv(file)
