import os

# Jython does not support removedirs and symlink.
# Warning: this implementation is not space-safe!
if 'removedirs' in dir(os):
	def removedirs(dir):
		os.removedirs(dir)
else:
	def removedirs(dir):
		os.system('rm -rf ' + dir)
if 'symlink' in dir(os):
	def symlink(src, dest):
		os.symlink(src, dest)
else:
	def symlink(src, dest):
		os.system("ln -s '" + src + "' '" + dest + "'")
if 'chmod' in dir(os):
	def chmod(path, mode):
		os.chmod(path, mode)
else:
	def chmod(path, mode):
		os.system('chmod ' + ('%o' % mode) + ' ' + path)
try:
	from java.lang import Runtime
	from java.io import BufferedReader, InputStreamReader

	def execute(cmd):
		runtime = Runtime.getRuntime()
		p = runtime.exec(cmd)
		p.outputStream.close()
		result = ""
		reader = BufferedReader(InputStreamReader(p.inputStream))
		errorReader = BufferedReader(InputStreamReader(p.errorStream))
		while True:
			if p.errorStream.available() > 0:
				print errorReader.readLine()
			line=reader.readLine()
			if line == None:
				break
			result+=line + "\n"
		while True:
			line = errorReader.readLine()
			if line == None:
				break
			print line
		p.waitFor()
		if p.exitValue() != 0:
			print result
			raise RuntimeError, 'execution failure'
		return result
except:
	def execute(cmd):
		proc = os.popen(cmd)
		return "\n".join(proc.readlines())
