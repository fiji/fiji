#!/usr/bin/python

import re
import os
import sys
from subprocess import check_call
from common import *

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

build_dependencies = []
with open(os.path.join(script_directory,'build-dependencies')) as f:
    for line in f:
        if re.search('^\s*$',line):
            continue
        line = re.sub(' .*','',line.rstrip())
        build_dependencies.append(line)

build_dependencies.append("fakeroot")

if os.path.exists(chroot_path):
    print >> sys.stderr, "The path "+chroot_path+" already exists."
    sys.exit(1)

files_to_copy = [ "etc/passwd", "etc/shadow", "etc/group" ]

check_call(["mkdir",chroot_path])
check_call(["mkdir","-p",home_in_chroot])
check_call(["mkdir",os.path.join(chroot_path,"etc")])
check_call(["chown","-R",owner_and_group,home_in_chroot])

for f in files_to_copy:
    check_call(["cp","-a","/"+f,os.path.join(chroot_path,f)])

check_call(["debootstrap",
            "--include="+",".join(build_dependencies),
            "--arch=i386",
            "squeeze",
            chroot_path,
            "http://ftp.de.debian.org/debian"])
