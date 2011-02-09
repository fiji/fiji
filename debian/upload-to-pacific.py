#!/usr/bin/python

import os
from subprocess import check_call, call
from common import *
import sys
from glob import glob

ssh_identity_file = os.path.join(os.environ['HOME'],".ssh/id_dsa.pacific")

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

version = get_version_from_changelog(os.path.join(script_directory,"changelog"))

pacific_root = "/var/www/downloads/apt-experimental"

file_path = os.path.realpath(os.path.join(script_directory,"..",".."))

architectures = { "i386" : "/var/chroot/squeeze-i386/home/mark/fiji-build/",
                  "amd64" : file_path,
                  "all" : file_path }

dsc_file = os.path.join(file_path,"fiji_"+version+".dsc")
tar_file = os.path.join(file_path,"fiji_"+version+".tar.gz")

source_files = [ dsc_file, tar_file ]

# From http://stackoverflow.com/questions/35817/whats-the-best-way-to-escape-os-system-calls-in-python
def shellquote(s):
    return "'" + s.replace("'", "'\\''") + "'"

ssh_control_file = "/tmp/ssh-%h-%p-%r.control"

def ssh( command, master=False, control_command=None ):
    if master and control_command:
        raise Exception, "You can't specify a control command when creating a control master"
    options = [ "-o", "ControlPath="+ssh_control_file ]
    options += [ "-i", ssh_identity_file ]
    if master:
        options += [ "-N", "-f", "-o", "ControlMaster=yes" ]
    if control_command:
        options += [ "-O", control_command ]
    full_command = ["ssh"]+options+["longair@pacific.mpi-cbg.de"]
    if command:
        full_command.append(command)
    return 0 == call(full_command)

def scp( src_paths, dst_path ):
    return 0 == call(["scp","-i",ssh_identity_file,"-o","ControlPath="+ssh_control_file]+src_paths+["longair@pacific.mpi-cbg.de:"+shellquote(dst_path)])

# Just set up the control master:
ssh( None, master=True )

try:

    updated_directories = []

    def copy_to_directory( files, pacific_directory ):
        global updated_directories
        if not ssh("mkdir -p "+shellquote(pacific_directory)):
            raise Exception, "Failed to ensure that "+pacific_directory+" on pacific exists"
        pacific_directory_new = pacific_directory+".new"
        if not ssh("mkdir -p "+shellquote(pacific_directory_new)):
            raise Exception, "Failed to ensure that "+pacific_directory_new+" on pacific exists"
        if not scp( files, pacific_directory_new ):
            raise Exception, "scp to "+pacific_directory_new+" failed"
        updated_directories.append( (pacific_directory_new, pacific_directory) )

    pacific_directory = os.path.join(pacific_root,"source")
    copy_to_directory( source_files, pacific_directory )

    for a in architectures:
        binary_file_path = architectures[a]
        pattern = os.path.join(binary_file_path,"fiji*"+version+"_"+a+".deb")
        binary_files = glob(pattern)
        pacific_directory = os.path.join(pacific_root,"binary-"+a)
        copy_to_directory( binary_files, pacific_directory )

    if not ssh("/usr/local/bin/rotate-apt-experimental"):
        raise Exception, "Failed to update the Packages.gz and Sources.gz files"

finally:
    ssh(None,control_command="exit")
