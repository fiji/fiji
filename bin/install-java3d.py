#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from ij3d import Install_J3D
from sys import exit

Install_J3D().run(None)
exit()
