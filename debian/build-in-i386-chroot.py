#!/usr/bin/python

import os
from subprocess import check_call
from common import *
import sys

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

version = get_version_from_changelog(os.path.join(script_directory,"changelog"))

dsc_file = os.path.realpath(os.path.join(script_directory,"..","..","fiji_"+version+".dsc"))
tar_file = os.path.realpath(os.path.join(script_directory,"..","..","fiji_"+version+".tar.gz"))

for f in (dsc_file,tar_file):
    if not os.path.exists(f):
        print >> sys.stderr("Couldn't find the expected file at "+f)
        sys.exit(1)

# Remove any old version and start again:
check_call(["rm","-rf",build_path_in_chroot])
check_call(["mkdir","-p",build_path_in_chroot])
check_call(["chown","-R",owner_and_group,build_path_in_chroot])

for f in (dsc_file,tar_file):
    destination = os.path.join(build_path_in_chroot,os.path.basename(f))
    check_call(["cp",f,destination])
    check_call(["chown",owner_and_group,destination])

# Now unpack the source package:

build_directory = "/"+os.path.relpath(build_path_in_chroot,chroot_path)

def run_in_chroot(directory, command):
    check_call(["schroot",
                "-c", "squeeze-i386",
                "--directory="+directory,
                "--user=mark",
                "--"] +
                command)

run_in_chroot(build_directory,["dpkg-source","-x",os.path.basename(dsc_file)])

source_directory = os.path.join(build_directory,"fiji-"+version)

run_in_chroot(source_directory,["linux32","dpkg-buildpackage","-ai386","-rfakeroot","-us","-uc"])
