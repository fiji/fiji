import os

# Jython does not support removedirs and symlink.
# Warning: this implementation is not space-safe!
if not 'JavaPOSIX' in dir(os):
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

if os.name == 'java':
	from compat_jython import execute
else:
	def execute(cmd):
		proc = os.popen(cmd)
		return "\n".join(proc.readlines())
