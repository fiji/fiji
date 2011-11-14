#!/usr/bin/python

import os
import stat
import sys
import urllib2
import re
import gzip
import StringIO
import datetime
import time
from optparse import OptionParser
from lxml import etree
from subprocess import call, check_call, Popen, PIPE
from common import *
import textwrap
from tempfile import NamedTemporaryFile

# On Ubuntu and Debian, the required Java3D jars are in these packages:
#
#   j3dcore.jar =>          libjava3d-java
#   j3dutils.jar =>         libjava3d-java
#   vecmath.jar =>          libvecmath-java
#   libj3dcore-ogl.so =>    libjava3d-jni
#   libj3dcore-ogl-cg.so => [missing - probably not needed?]

# We can't expect any of this to work without the basic build
# dependencies for the Debian package in place.  (It might look odd
# having this run every time, sometimes with the git committed
# dependencies and sometimes with the rebuilt control file, but in
# either case it's an error to be missing any build dependency.)

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

check_call(["dpkg-checkbuilddeps",os.path.join(script_directory,"control")])

build_dependencies = []
with open(os.path.join(script_directory,'build-dependencies')) as f:
    for line in f:
        if re.search('^\s*$',line):
            continue
        build_dependencies.append(line.strip())

# This is the package that will contain all files that haven't been
# assigned to a package in package_name_to_file_matchers
default_package = "fiji-plugins"

# Where most of the Fiji files will ultimately be installed to in the
# packaged version:
destination_fiji_prefix = "usr/lib/fiji/"

# This dictionary specifies which files are contained in particular
# packages.  If you add a file to a package that was previously in
# another package you will need to add a conflict of the new package
# with the old one in the "conflicts_and_replaces" dictionary below.
# Otherwise, you will get errors that one package is trying to
# overwrite a file in another package on upgrade.
package_name_to_file_matchers = {

    "fiji-imagej" :
        [ "jars/ij.jar",
          "jars/javac.jar",
          "macros/StartupMacros.txt",
          "misc/headless.jar" ],

    "fiji-rsyntaxtextarea" :
        [ "jars/rsyntaxtextarea.jar" ],

    "fiji-autocomplete" :
        [ "jars/autocomplete.jar" ],

    "fiji-base" :
        [ "fiji-linux",
          "fiji-linux64",
          "jars/fake.jar",
          "misc/Fiji.jar",
          "plugins/Fiji_Updater.jar",
          "plugins/Bug_Submitter.jar",
          "jars/fiji-lib.jar",
          re.compile("images/.*") ],

    "fiji-scripting" :
        [ "plugins/Clojure_Interpreter.jar",
          "plugins/Fiji_Developer.jar",
          "plugins/Script_Editor.jar",
          "plugins/Jython_Interpreter.jar",
          "plugins/JRuby_Interpreter.jar",
          "plugins/JRuby/imagej.rb",
          "plugins/BeanShell_Interpreter.jar",
          "plugins/Javascript_.jar",
          "jars/fiji-scripting.jar",
          "plugins/Refresh_Javas.jar",
          "plugins/CLI_.jar" ],

    "fiji-luts" :
        [ re.compile('^luts/.*\.lut'),
          "luts/README.txt"],

    "fiji-examples" :
        [ re.compile('^plugins/Examples/') ],

    "fiji-mpicbg" :
        [ "plugins/mpicbg_.jar",
          "jars/mpicbg.jar" ],

    "fiji-ij-imageio":
        [ "plugins/ij-ImageIO_.jar" ],

    "fiji-jai":
        [ "jars/clibwrapper_jiio.jar",
          "jars/jai_codec.jar",
          "jars/jai_core.jar" ],

    "fiji-imglib" :
        [ "jars/imglib.jar",
          "jars/imglib-scripting.jar",
          "jars/imglib-algorithms.jar",
          "jars/imglib-ij.jar",
          "jars/imglib-io.jar" ],

    "fiji-vib" :
        [ "plugins/VIB_.jar",
          "jars/VIB-lib.jar" ],

    "fiji-trakem2" :
        [ "plugins/TrakEM2_.jar" ],

    "fiji-edu-mines-jtk" :
        [ "jars/edu_mines_jtk.jar" ],

    "fiji-jruby" :
        [ "jars/jruby.jar" ],

    "fiji-jpedal" :
        [ "jars/jpedalSTD.jar" ],

    "fiji-tcljava" :
        [ "jars/jacl.jar" ],

    "fiji-mtrack2" :
        [ "plugins/MTrack2_.jar" ],

    "fiji-bunwarpj" :
        [ "plugins/bUnwarpJ_.jar" ],

    "fiji-lsm-toolbox" :
        [ "plugins/LSM_Toolbox.jar" ],

    "fiji-lsm-reader" :
        [ "plugins/LSM_Reader.jar" ],

    "fiji-image5d" :
        [ "plugins/Image_5D.jar" ],

    "fiji-loci-tools" :
        [ "plugins/loci_tools.jar" ],

    "fiji-color-histogram" :
        [ "plugins/Color_Histogram.jar" ],

    "fiji-barthel-plugins" :
        [ "plugins/Interactive_3D_Surface_Plot.jar",
          "plugins/Color_Inspector_3D.jar",
          "plugins/Volume_Viewer.jar" ],

    "fiji-zs" :
        [ "jars/zs.jar" ],

    "fiji-view5d" :
        [ "plugins/View5D_.jar" ],

    "fiji-mip" :
        [ "plugins/M_I_P.jar" ],

    "fiji-io" :
        [ "plugins/IO_.jar" ],

    "fiji-weka" :
        [ "jars/weka.jar" ],

    "fiji-3d-viewer" :
        [ "plugins/3D_Viewer.jar" ],

    "fiji-tracer" :
        [ "plugins/Simple_Neurite_Tracer.jar" ],

    "fiji-jython" :
        [ "jars/jython.jar" ],

    "fiji-itext" :
        [ "jars/itextpdf-5.1.1.jar" ]

}

# Here you should specify packages that conflict with an earlier
# version of another package - this is almost always because a file
# has been moved from one package to another.
conflicts_and_replaces = {
    'fiji-3d-viewer' : ( 'fiji-plugins (<= 20100821202528)', ),
    'fiji-imglib' : ( 'fiji-plugins (<= 20110609134243)', ),
    'fiji-image5d' : ( 'fiji-plugins (<= 20111011070056)', ),
}

# A dictionary whose keys are regular expressions that match files in
# the Fiji build tree, and whose values are tuples of the external
# Debian packages that contain the replacement jar files that should
# be used instead:
#
# FIXME: in fact, this could be calculated from the replacement files
# has below and repeated invocations of dpkg --search, since all the
# build-dependencies should be installed.  That would be slow, but
# less brittle.
map_to_external_dependencies = {
    'jars/batik\.jar' : ( 'libbatik-java', 'libxml-commons-external-java' ),
    'jars/bsh.*\.jar' : ('bsh', ),
    'jars/clojure.*\.jar' : ( 'clojure', ),
    'jars/junit.*\.jar' : ( 'junit', ),
    'jars/js\.jar' : ( 'rhino', ),
    'jars/Jama.*\.jar': ( 'libjama-java', ),
    'jars/jzlib.*\.jar' : ( 'libjzlib-java', ),
    'jars/jfreechart.*\.jar' : ( 'libjfreechart-java', ),
    'jars/jcommon.*\.jar' : ( 'libjcommon-java', ),
    'jars/jsch.*\.jar' : ( 'libjsch-java', ),
    'jars/postgresql.*\.jar' : ( 'libpg-java', ),
    'jars/ant.*\.jar' : ( 'ant', 'ant-optional', ),
    'jars/javassist.*\.jar' : ( 'libjavassist-java', ),
    'jars/jna\.jar' : ( 'libjna-java', )
}

# A dictionary that maps a file in the Fiji build tree to tuples of
# replacement files to use, where that file has now been replaced by
# an external dependency.
replacement_files =  {
    'jars/batik.jar' : ( '/usr/share/java/batik-all.jar', '/usr/share/java/xml-apis-ext.jar' ),
    'jars/bsh-2.0b4.jar' : ( '/usr/share/java/bsh.jar', ),
    'jars/clojure.jar' : ( '/usr/share/java/clojure.jar', ),
    'jars/Jama-1.0.2.jar' : ( '/usr/share/java/jama.jar', ),
    'jars/jcommon-1.0.12.jar' : ( '/usr/share/java/jcommon.jar', ),
    'jars/jfreechart-1.0.13.jar' : ( '/usr/share/java/jfreechart.jar', ),
    'jars/js.jar' : ( '/usr/share/java/js.jar', ),
    'jars/jsch-0.1.44.jar' : ( '/usr/share/java/jsch.jar', ),
    'jars/junit-4.5.jar' : ( '/usr/share/java/junit4.jar', ),
    'jars/jzlib-1.0.7.jar' : ( '/usr/share/java/jzlib.jar', ),
    'jars/postgresql-8.2-506.jdbc3.jar' : ( '/usr/share/java/postgresql.jar', ),
    'jars/javassist.jar' : ( '/usr/share/java/javassist.jar', ),
    'jars/jna.jar' : ( '/usr/share/java/jna.jar', ),
    '$TOOLS_JAR' : ('/usr/lib/jvm/java-6-openjdk/lib/tools.jar', ),
    '$JAVA3D_JARS' : ('/usr/share/java/j3dcore.jar', '/usr/share/java/vecmath.jar', '/usr/share/java/j3dutils.jar', )
}

# Fake adds the jar files in "jars" to the classpath implicitly, so we
# need to explicitly add a couple of jars for classpaths here:
missing_dependecies_in_fakefile = {
    'plugins/TrakEM2_.jar' : ( '/usr/share/java/postgresql.jar', '/usr/share/java/jfreechart.jar' )
}

# Given a filename in the Fiji build tree, return a tuple of the
# external Debian package names that contain replacement jars, or
# return None if there is no such replacement:
def replacement_dependencies(fiji_file):
    for r in map_to_external_dependencies:
        if re.search(r,fiji_file):
            return map_to_external_dependencies[r]
    return None

# These dictionaries will contain the reverse mapping of the
# dictionary "package_name_to_file_matcher":
file_to_package_name_dictionary = {}
regular_expressions_to_package = []

for package_name, contents in package_name_to_file_matchers.items():
    for f in contents:
        if type(f) == str:
            file_to_package_name_dictionary[f] = package_name
        elif type(f) == tuple:
            file_to_package_name_dictionary[f[0]] = package_name
        elif hasattr(f,'pattern'):
            regular_expressions_to_package.append( (f,package_name) )

# Given a filename, return the package that should contain it:
def filename_to_package(filename):
    global file_to_package_name_dictionary, regular_expressions_to_package, default_package
    if filename in file_to_package_name_dictionary:
        return file_to_package_name_dictionary[filename]
    else:
        for t in regular_expressions_to_package:
            if t[0].search(filename):
                return t[1]
        return default_package

# ========================================================================
# Parse the command-line options:

usage_message = "Usage: %prog [OPTIONS]"
parser = OptionParser(usage=usage_message)

parser.add_option('--depends-on',
                  dest="depends_on",
                  metavar="<FILE>",
                  help="Show the files that <FILE> depends on (possibly indirectly)")

parser.add_option('--depended-on-by',
                  dest="depended_on_by",
                  metavar="<FILE>",
                  help="Show the files that depend on <FILE> (possibly indirectly)")

parser.add_option('--all-dependencies',
                  dest="all_dependencies",
                  metavar="<FILE-SUBSTRING>",
                  help="Show the (possibly indirect) dependencies for a file matching <FILE-SUBSTRING>")

parser.add_option('--clean',
                  dest="clean",
                  default=False,
                  action='store_true',
                  help="[DANGEROUS] Clean all untracked and ignored files")

parser.add_option('--clean-untracked',
                  dest="clean_untracked",
                  default=False,
                  action='store_true',
                  help="[DANGEROUS] Clean all untracked files")

parser.add_option('--check-git-clean',
                  dest="check_git_clean",
                  default=False,
                  action='store_true',
                  help="Check that git status is clean for every submodule")

parser.add_option('--install',
                  dest="install",
                  default=False,
                  action='store_true',
                  help="With a built tree, install everything to debian/fiji-imagej, debian/fiji-luts, etc.")

parser.add_option('--generate-complete-control',
                  dest="generate_complete_control",
                  default=False,
                  action='store_true',
                  help="Generate the complete debian/control file with correct dependencies")

parser.add_option('--generate-source-control',
                  dest="generate_source_control",
                  default=False,
                  action='store_true',
                  help="Generate a debian/control file with just the source package")

parser.add_option('--add-changelog-template',
                  dest="add_changelog_template",
                  default=False,
                  action='store_true',
                  help="Add a template entry into the changelog, based on the current timestamp")

parser.add_option('--generate-build-command',
                  dest="generate_build_command",
                  default=False,
                  action='store_true',
                  help="Generate debian/build-command from Fakefile")

options,args = parser.parse_args()

source_directory = os.path.split(script_directory)[0]
os.chdir(source_directory)

with open('debian/java-home') as fp:
    java_home = fp.read().strip()

# ========================================================================
# Fill in some template information at the top of the changelog, including
# the current git HEAD:

if options.add_changelog_template:
    if len(args) == 0:
        message = "[Fill in the rest of the commit message here.]"
    elif len(args) == 1:
        message = args[0]
    else:
        raise Exception, "You must provide a single argument with the changelog message"
    suggest_new_version = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    git_rev = Popen(["git","rev-parse","--verify","HEAD"],stdout=PIPE).communicate()[0].strip()
    fp = open("debian/changelog")
    old_changelog = fp.read()
    fp.close()
    fp = open("debian/changelog","w")
    fp.write("fiji (%s) unstable; urgency=low\n\n"%(suggest_new_version,))
    fp.write("  * Based on fiji.git at "+git_rev+"\n")
    fp.write("    %s\n"%(message))
    fp.write("\n")
    fp.write(" -- Mark Longair <mhl@pobox.com>  "+time.strftime("%a, %d %b %Y %H:%M:%S +0000",time.gmtime())+"\n\n")
    fp.write(old_changelog)
    fp.close()
    sys.exit(0)

# ========================================================================

# The debian/build-command file is generated by finding all
# CLASSPATH() definitions in the Fakefile that contain jar files that
# are now replaced with Debian-packaged versions, and overriding those
# CLASSPATH()s on the command line.

if options.generate_build_command:
    new_classpaths = {}
    fakefile_path = os.path.join(source_directory,"Fakefile")
    classpath_definitions = classpath_definitions_from(fakefile_path)
    for file_to_build in classpath_definitions:
        dependents = classpath_definitions[file_to_build]
        new_classpath = set([])
        anything_changed = False
        for d in dependents:
            if d in replacement_files:
                anything_changed = True
                new_classpath.update(replacement_files[d])
            elif replacement_dependencies(d):
                raise Exception, 'No replacement file found for %s, but it would be replaced by packages: %s' % (d,', '.join(replacement_dependencies(d)))
            else:
                new_classpath.add(d)
        if file_to_build in missing_dependecies_in_fakefile:
            anything_changed = True
            new_classpath.update(missing_dependecies_in_fakefile[file_to_build])
        if anything_changed:
            new_classpaths[file_to_build] = new_classpath
    # Now write out the file:
    build_command_path = os.path.join(script_directory,"build-command")
    with open(build_command_path,"w") as f:
        f.write('''#!/bin/bash

## Warning: this file is automatically generated from the Fakefile
## with debian/update-debian --generate-build-command

DEBIAN_DIRECTORY=$(dirname $(readlink -f "$BASH_SOURCE"))
FIJI_DIRECTORY=$(readlink -f "$DEBIAN_DIRECTORY"/..)

export JAVA_HOME=`cat "$DEBIAN_DIRECTORY"/java-home`
JAVAC_PATH=$JAVA_HOME/bin/javac

if [ ! -e "$JAVAC_PATH" ]
then
    echo Could not find the Java compiler at "$JAVAC_PATH"
    exit 1
fi

echo In build-command, found JAVA_HOME was $JAVA_HOME

# These lines are taken from Build.sh to ensure that Fake
# is built:
source_dir=src-plugins/fake
source=$source_dir/fiji/build/*.java
export SYSTEM_JAVAC=$JAVA_HOME/bin/javac
export SYSTEM_JAVA=$JAVA_HOME/bin/java

mkdir -p "$FIJI_DIRECTORY"/build &&
  $SYSTEM_JAVAC -d "$FIJI_DIRECTORY"/build/ "$FIJI_DIRECTORY"/$source &&
  $SYSTEM_JAVA -classpath "$FIJI_DIRECTORY"/build fiji.build.Fake fiji &&
  $SYSTEM_JAVA -classpath "$FIJI_DIRECTORY"/build fiji.build.Fake jars/fake.jar

./fiji --build --java-home "$JAVA_HOME" -- FALLBACK=false VERBOSE=true \\
''')
        for k in sorted(new_classpaths.keys()):
            f.write('    "CLASSPATH(%s)=%s" \\\n' % (k,':'.join(sorted(new_classpaths[k]))))
        f.write('    "$@"\n')
    check_call(["chmod","a+rx",build_command_path])
    os.chdir(source_directory)
    call(["git","commit","-m","An automatic commit of the new debian/build-command from debian/update-debian.py","debian/build-command"])
    sys.exit(0)

# Extract the Java 3D dependencies from the build-command script:
# FIXME: this could now be discovered from the Fakefile in the code
# above, without reparsing the file we've just generated...

additional_java3d_dependencies = {}
build_command_script = os.path.join(script_directory,"build-command")
classpath_definitions = classpath_definitions_from(build_command_script)

for file_to_build, dependents in classpath_definitions.items():
    for j in dependents:
        if re.search('(j3dcore|j3dutils)\.jar',j):
            additional_java3d_dependencies.setdefault(file_to_build,set([]))
            additional_java3d_dependencies[file_to_build].add('libjava3d-java')
        elif re.search('vecmath\.jar',j):
            additional_java3d_dependencies.setdefault(file_to_build,set([]))
            additional_java3d_dependencies[file_to_build].add('libvecmath-java')

# The Debian package building scripts care deeply that the name of the
# directory that you're building in matches the most recent version in
# the changelog.

version_from_changelog = get_version_from_changelog(os.path.join(script_directory,"changelog"))

source_directory_leafname = os.path.split(source_directory)[1]

expected_directory_name = "fiji-"+version_from_changelog
realpath_source_directory_leafname = os.path.split(os.path.realpath(source_directory))[1]

if expected_directory_name != source_directory_leafname:
    print >> sys.stderr, "The source directory's leafname ("+source_directory_leafname+") didn't match the expected name ("+expected_directory_name+")"
    print >> sys.stderr, "The expected name is derived from debian/changelog and debian/control."
    sys.exit(1)

# Just to be extra safe, check that the real path is the same as well:

if expected_directory_name != realpath_source_directory_leafname:
    print >> sys.stderr, "The source directory's leafname after realpath ("+realpath_source_directory_leafname+") didn't match the expected name ("+expected_directory_name+")"
    sys.exit(2)

# ========================================================================

# This dictionary is filled in when we actually list the files; it
# needs a built tree in order to discover which are actually present:
package_name_to_files = {}

# These options all assume you have a built tree:
if options.install or options.generate_complete_control or options.generate_source_control:

    os.chdir(source_directory)

    # Walk these directories of the built tree to discover which files
    # are present and should be installed:
    directories_to_walk = [ "plugins",
                            "misc",
                            "jars",
                            "luts",
                            "macros",
                            "images" ]

    all_files_to_install = []

    for d in directories_to_walk:
        for root, dirs, files in os.walk(d):
            if re.match("jars/cachedir",root):
                continue
            for f in files:
                joined = os.path.join(root,f)
                package = filename_to_package(joined)
                package_name_to_files.setdefault(package,set([]))
                package_name_to_files[package].add(joined)
                all_files_to_install.append(joined)

    for p in package_name_to_files:
        print p
        for f in package_name_to_files[p]:
            print "  "+f

def install_file(source_filename,destination_filename):
    destination_directory = os.path.split(destination_filename)[0]
    if not os.path.exists(destination_directory):
        os.makedirs(destination_directory,mode=0755)
    if 0 != call(["cp",source_filename,destination_filename]):
        raise Exception, "Failed to copy '"+source_filename+"' to '"+destination_filename+"'"

# The --install option is used from debian/rules to actually put the
# files in the right subdirectory of debian/
if options.install:
    for f in all_files_to_install:
        print "Trying to install: "+f
        package = filename_to_package(f)
        destination_filename = os.path.join(source_directory,"debian",package,destination_fiji_prefix,f)
        install_file(f,destination_filename)

# Remove all files that are listed as untracked.  (This doesn't
# include ignored files.)
def clean_untracked(top_level_working_directory):
    os.chdir(top_level_working_directory)
    call("git ls-files --others --directory -z | xargs -0 rm -rf",shell=True)
    for s in absolute_submodule_paths(top_level_working_directory):
        clean_untracked(s)

# Remove all files that are listed as untracked AND ignored files
def clean_aggressively(top_level_working_directory):
    os.chdir(top_level_working_directory)
    call("git ls-files --others --directory --no-empty-directory --exclude-standard -z | xargs -0 rm -rf",shell=True)
    call("git clean -f -X -d",shell=True)
    for s in absolute_submodule_paths(top_level_working_directory):
        clean_aggressively(s)

# Find the absolute paths of all submodules by parsing the output of
# "git submodule status" and prefixing them with the working directory:
def absolute_submodule_paths(top_level_working_directory):
    result = []
    for line in Popen(["git","submodule","status"],stdout=PIPE).communicate()[0].split('\n'):
        if not line.strip():
            continue
        m = re.search("([ \+\-])\S+\s+(\S+)",line)
        if not m:
            raise Exception, "Failed to parse the output of git submodule status, line: "+line

        if m.group(1) == "-":
            print >> sys.stderr, "Skipping a missing submodule: "+m.group(2)
        else:
            result.append(os.path.join(top_level_working_directory,m.group(2)))
    return result

# Take a package name and an optional minimum version and produce a
# string that can be used in debian/control fields:
def package_version_to_string(package,version=None):
    if version:
        return package + " (>= "+version+")"
    else:
        return package

# This method raises an exception unless "git status" is clean in the
# top level directory and in every submodule:
def check_git_status_clean(top_level_working_directory):
    os.chdir(top_level_working_directory)
    if 0 != call("git rev-parse --is-inside-work-tree > /dev/null",shell=True):
        raise Exception, "The directory '"+top_level_working_directory+"' is not a git working directory"
    if Popen(["git","rev-parse","--show-cdup"],stdout=PIPE).communicate()[0].strip():
        raise Exception, "The directory '"+top_level_working_directory+"' is not the top level of a git working directory"
    if 0 != call(["git","diff","--exit-code"]):
        raise Exception, "There was some output from 'git diff' in "+top_level_working_directory
    if 0 != call(["git","diff","--cached","--exit-code"]):
        raise Exception, "There was some output from 'git diff --cached' in "+top_level_working_directory
    untracked_files = Popen(["git","ls-files","--others","--directory","--no-empty-directory","--exclude-standard"],stdout=PIPE).communicate()[0].strip()
    if len(untracked_files) > 0:
        raise Exception, "There were untracked files in "+top_level_working_directory+":\n"+untracked_files
    for s in absolute_submodule_paths(top_level_working_directory):
        check_git_status_clean(s)

if options.check_git_clean:
    try:
        check_git_status_clean(source_directory)
    except Exception, e:
        print >> sys.stderr, "git status is not clean: "+str(e)
        sys.exit(4)
    sys.exit(0)

if options.clean_untracked:
    clean_untracked(source_directory)
    sys.exit(0)

if options.clean:
    # Make sure that the excludes in the submodules are
    # up to date:
    check_call([os.path.join(script_directory,"..","bin","gitignore-in-submodules.sh"),
                "submodule"])

    clean_aggressively(source_directory)

    # Now remove some non-free parts of the Fiji distribution:

    os.chdir(source_directory)

    # TurboReg_:
    to_remove = [ "staged-plugins/TurboReg_.config",
                  "src-plugins/TurboReg_",
                  "jars/imagescience.jar",
                  "staged-plugins/TransformJ_.config",
                  "staged-plugins/TransformJ_.jar" ]

    # Also remove submodules which are now provided by external dependencies:
    to_remove.append("batik")
    to_remove.append("java/linux")
    to_remove.append("java/linux-amd64")
    to_remove.append("java/macosx-java3d")
    to_remove.append("src-plugins/Jama-1.0.2")
    to_remove.append("java/win32")
    to_remove.append("java/win64")
    to_remove.append("livehelper")
    to_remove.append("Retrotranslator")
    to_remove.append("clojure")
    to_remove.append("junit")
    to_remove.append("javassist")

    # Remove files that are now provided by external dependencies.
    # FIXME: This list could (and should) be taken from the keys of
    # map_to_external_dependencies above:
    to_remove.append("jars/js.jar")
    to_remove.append("jars/bsh*.jar")
    to_remove.append("jars/Jama*.jar")
    to_remove.append("jars/jzlib*.jar")
    to_remove.append("jars/jcommon*.jar")
    to_remove.append("jars/jfreechart*.jar")
    to_remove.append("jars/jsch*.jar")
    to_remove.append("jars/postgresql*.jar")
    to_remove.append("jars/ant*.jar")
    to_remove.append("jars/batik.jar")
    to_remove.append("jars/junit*.jar")

    for f in to_remove:
        call(["rm -rf "+f],shell=True)

    # Now rewrite the Fakefile to remove references to some of these
    # plugins.  This is an effort to make everything build cleanly,
    # despite some items in the Fakefile being impossible to override
    # from the command line, it seems.

    fp = open("Fakefile")
    oldlines = fp.readlines()
    fp.close()

    skip_next_line = False

    fp = open("Fakefile","w")
    for line in oldlines:
        if skip_next_line:
            skip_next_line = False
            continue
        # Don't exclude the dummy targets:
        if not re.search('\[\] *<- *$',line):
            if re.search("TurboReg_",line):
                continue
            if re.search("TransformJ_",line):
                continue
            if re.search("(^\s*jars|precompiled)/clojure.jar",line):
                continue
            if re.search("(^\s*jars|precompiled)/javassist.jar",line):
                continue
            if re.search("(^\s*jars|precompiled)/jsch-0.1.44.jar",line):
                continue
            if re.search("(^\s*jars|precompiled)/junit-4.5.jar",line):
                continue
            if re.search("(^\s*jars|precompiled)/batik.jar",line):
                continue
            if re.search("imagej2",line):
                continue
        if re.search("^\s*missingPrecompiledFallBack",line):
            skip_next_line = True
            continue
        # grrr, src-plugins/Jama-1.0.2 seems particularly awkward to
        # get rid of.  Probably should do everything like this, just
        # rewrite the Fakefile entirely, with a proper parser of the
        # format.  FIXME FIXME FIXME
        line = re.sub('\s+jars/Jama-1\.0\.2\.jar\s+',' ',line)
        line = re.sub('jars/Jama-1\.0\.2\.jar','/usr/share/java/jama.jar',line)
        line = re.sub('jars/javassist.jar','/usr/share/java/javassist.jar',line)
        fp.write(line)
    fp.close()

    # Hopefully there'll be a better fix for this at some stage, but
    # for the moment rewrite any occurence of "fiji --ant" in
    # staged-plugins/* and bin/build-jython.sh to include the
    # --java-home parameter:

    files_to_rewrite = [ os.path.join('staged-plugins',s) for s in os.listdir('staged-plugins') ]
    files_to_rewrite.append('bin/build-jython.sh')

    for filename in files_to_rewrite:
        original_permissions = stat.S_IMODE(os.stat(filename).st_mode)
        with NamedTemporaryFile(delete=False) as tfp:
            with open(filename) as original:
                for line in original:
                    line = re.sub('../../fiji\s+',"../../fiji --java-home '%s' "%(java_home,),line)
                    line = re.sub('/../bin/jar','/bin/jar',line)
                    tfp.write(line)
        os.chmod(tfp.name, original_permissions)
        os.rename(tfp.name, original.name)

    # Remove all the files in precompiled - we want to build
    # everything from source:
    call("rm -rfv precompiled/*",shell=True)
    sys.exit(0)

if options.generate_complete_control or options.all_dependencies or options.depended_on_by or options.depends_on:

    # We need to fetch the current db.xml.gz to get the most accurate
    # dependencies.  (The file-based dependencies db.xml.gz are
    # generated by looking at which classes are referenced in each jar
    # file, and finding the jar that contains those classes.)

    package_url = 'http://fiji.sc/update/db.xml.gz'

    if True:
        f = urllib2.urlopen(package_url)
        data = f.read()
        f.close()
        gz = gzip.GzipFile(fileobj=StringIO.StringIO(data))
    else:
        # This is just here to make it simple to switch to use an old
        # copy while debugging:
        gz = gzip.GzipFile(fileobj=open("/home/mark/tmp/db.xml.gz"))
    plain_text = gz.read()

    # ... and write it out in the debian directory:

    f = open("db.xml","w")
    f.write(plain_text)
    f.close()

    root = etree.fromstring(plain_text)

    # Build up a dictionary that maps filenames to a FileVersion
    # object, with all the metadata about the file that's contained in
    # db.xml.gz:
    filename_to_object = {}

    class FileVersion(object):
        """Represents the most recent version of a file, with all the information
        taken from db.xml"""

        def __init__(self,filename):
            self.filename = filename
            self.timestamp = None
            self.checksum = None
            self.filesize = None
            self.description = None
            self.raw_dependencies = []
            self.depends_on = set([])
            self.depended_on_by = set([])
        def set_from_element(self,element):
            self.timestamp = element.attrib['timestamp']
            self.checksum = element.attrib['checksum']
            self.filesize = element.attrib['filesize']
            description_element = element.find("description")
            if description_element is not None:
                self.description = description_element.text.strip()
            for d in element.xpath("dependency"):
                a = d.attrib
                self.raw_dependencies.append( (a['filename'],a['timestamp'] ) )
        def update_dependency_list(self,filename_to_object_dictionary):
            for d, t in self.raw_dependencies:
                other_object = filename_to_object_dictionary[d]
                self.depends_on.add(other_object)
                other_object.depended_on_by.add(self)
        def __str__(self):
            return "[%s] (%s) %s" % (self.checksum,self.timestamp,self.filename)

    # Got through every <plugin> element, looking for the <version>
    # element, and store those data in FileVersion objects.

    for plugin_element in root.xpath("//plugin"):
        filename = plugin_element.attrib['filename']
        version_element = plugin_element.find("version")
        if version_element is not None:
            o = FileVersion(plugin_element.attrib['filename'])
            o.set_from_element(version_element)
            filename_to_object[filename] = o
            # Check whether the timestamp is greater than this version:
            if o.timestamp > version_from_changelog:
                print >> sys.stderr, "A timestamp in db.xml (%s) seems to be later than the version number" % (o.timestamp,)
                print >> sys.stderr, "in the changelog; this suggests you've forgotten to create a new"
                print >> sys.stderr, "entry in debian/changelog with a current timestamp."
                sys.exit(1)

    # Now make sure there are bidirectional dependency relationships
    # between all the objects, so we can traverse the graph in either direction:

    for filename, o in filename_to_object.items():
        o.update_dependency_list(filename_to_object)

    def strip_extension(filename):
        return re.sub('\..*$','',filename)

    # It's sometimes useful to generate a dependency graph - this
    # generates the source for such a graph in the graphviz .dot
    # format.  (This isn't used at the moment, but it's here for
    # debugging purposes.)

    def generate_dependency_graph(filename_to_object_dictionary,graph_basename):

        fp = open(graph_basename+".dot","w")
        fp.write('''digraph dependencies {
        overlap=false
        splines=true
        sep=0.1
        node [fontname="DejaVuSans"]
''')

        for filename, o in filename_to_object_dictionary.items():
            for d_filename, d_timestamp in o.raw_dependencies:
                fp.write("    \"%s\" -> \"%s\"\n"%(strip_extension(filename),
                                                   strip_extension(d_filename)))

        fp.write("\n}\n")
        fp.close()

    # A helper method to find all file-based dependencies of a particular file:
    def dependencies_of_file(filename_to_object_dictionary,dependent_file,reverse=False):
        start_object = filename_to_object_dictionary[dependent_file]
        done = set([])
        if reverse:
            unexplored = set(start_object.depended_on_by)
        else:
            unexplored = set(start_object.depends_on)
        current_distance = 0
        while len(unexplored) > 0:
            u = unexplored.pop()
            done.add(u)
            if reverse:
                next = u.depended_on_by
            else:
                next = u.depends_on
            for o in next:
                if o not in done:
                    unexplored.add(o)
        return done

    # Taken from a blog post by Guido van Rossum:
    #   http://neopythonic.blogspot.com/2009/01/detecting-cycles-in-directed-graph.html

    def find_cycle(NODES, EDGES, READY):
        todo = set(NODES())
        while todo:
            node = todo.pop()
            stack = [node]
            while stack:
                top = stack[-1]
                for node in EDGES(top):
                    if node in stack:
                        return stack[stack.index(node):]
                    if node in todo:
                        stack.append(node)
                        todo.remove(node)
                        break
                else:
                    node = stack.pop()
                    READY(node)
        return None

    def NODES():
        return [ filename_to_object[f] for f in filename_to_object ]

    def EDGES(node):
        return node.depends_on

    def READY(node):
        if False:
            print "Established that "+str(node)+" is not in a cycle."

    # Check that there are no cyclical dependencies:
    cycle_element = find_cycle(NODES,EDGES,READY)
    if cycle_element:
        print >> sys.stderr, "Found a cycle in the dependencies, containing: "+str(cycle_element)
        sys.exit(3)

    if options.generate_source_control or options.generate_complete_control:
        print "++++ Writing the source section"

        control_fp = open("debian/control","w")
        control_fp.write("""Source: fiji
Section: graphics
Priority: extra
Maintainer: Mark Longair <mhl@pobox.com>
Build-Depends: %s
Standards-Version: 3.7.2""" % (", ".join(build_dependencies),))
        # FIXME: also add a meta-package here.  (Really?)
        control_fp.close()

    if options.generate_complete_control:

        control_fp = open("debian/control","a")

        most_recent_package_version = {}

        for p in package_name_to_files:
            print "package "+p
            architecture = "all"
            # It's only fiji-base that has any native code at the moment, I think:
            if p == "fiji-base":
                architecture = "any"
            files = package_name_to_files[p]
            required_packages = {}
            full_description = ""
            descriptions_found = 0
            first_description = None
            readable_names = []
            java3d_dependencies = set([])
            for x in files:
                if x in additional_java3d_dependencies:
                    java3d_dependencies.update(additional_java3d_dependencies[x])
                readable_names.append( re.sub('_',' ',re.sub('\.[^\.]+$','',os.path.basename(x))).strip() )
                if x not in filename_to_object:
                    print >> sys.stderr, "Warning: couldn't find dependencies for "+x
                    continue
                most_recent_package_version.setdefault(p,-1)
                x_timestamp = filename_to_object[x].timestamp
                if x_timestamp > most_recent_package_version[p]:
                    most_recent_package_version[p]  = x_timestamp
                x_description = filename_to_object[x].description
                if x_description:
                    descriptions_found += 1
                    if not first_description:
                        first_description = x_description
                    full_description += " .\n"
                    x_description = decode_htmlentities(x_description)
                    # Carriage return is used to separate paragraphs in the Fiji
                    # descriptions.  Turn them into a paragraph mark:
                    x_description = re.sub('(?ims)\s*\r\s*',u' \u00b6 ',x_description)
                    wrapped_lines = textwrap.wrap( x + ": " + x_description, 72 )
                    for line in wrapped_lines:
                        full_description += " " + line + "\n"
                # So for each file in this package, find its dependent
                # files:
                for d in filename_to_object[x].depends_on:
                    package_replacements = replacement_dependencies(d.filename)
                    if package_replacements:
                        print >> sys.stderr, "        Using external dependencies for %s" % (d,)
                        for pr in package_replacements:
                            print >> sys.stderr, "            Adding a dependency on %s" % (pr,)
                            # Don't specify a particular version - might want
                            # to change this...
                            required_packages.setdefault(pr,None)
                    else:
                        if os.path.exists(d.filename):
                            other_package = filename_to_package(d.filename)
                            print "        ("+x+" => "+d.filename+" ["+other_package+"])"
                            # If the dependent file is actually in the same
                            # package then just ignore it:
                            if other_package == p:
                                continue
                            required_packages.setdefault(other_package,d.timestamp)
                            if d.timestamp > required_packages[other_package]:
                                required_packages[other_package] = d.timestamp
                        else:
                            print >> sys.stderr, "        Skipping dependent file %s since it doesn't exist, and there's no replacement package" % (d.filename)
            for j in java3d_dependencies:
                required_packages.setdefault(j,None)
            for package_name, most_recent_requirement in required_packages.items():
                print "   requires "+ package_version_to_string(package_name,most_recent_requirement)

            dependencies = []
            if required_packages:
                for dependent_package, timestamp in required_packages.items():
                    dependencies.append(package_version_to_string(dependent_package,timestamp))

            replace_and_conflict_with = [ 'fiji (<= 20090513)' ]
            if p in conflicts_and_replaces:
                replace_and_conflict_with += conflicts_and_replaces[p]
            replace_and_conflict_with_string = ", ".join(replace_and_conflict_with)

            control_fp.write("\n\nPackage: "+p+"\n")
            control_fp.write("Section: graphics\n")
            control_fp.write("Architecture: %s\n"%(architecture,))
            control_fp.write("Priority: extra\n")
            control_fp.write("Replaces: %s\n"%(replace_and_conflict_with_string,))
            control_fp.write("Conflicts: %s\n"%(replace_and_conflict_with_string,))
            # control_fp.write("Version: "+version_from_changelog+"\n")
            control_fp.write("Depends: "+", ".join(dependencies)+"\n")
            # FIXME: it may still be useful at some point to be able
            # to override the generated descriptions, so leave this
            # code here for the moment:
            if False:
                description_filename = os.path.join(source_directory,"debian","package-extras","default-description")
                ideal_description_filename = os.path.join(source_directory,"debian","package-extras",p,"description")
                if os.path.exists(ideal_description_filename):
                    description_filename = ideal_description_filename
                fp = open(description_filename)
                description = fp.read().strip()
                fp.close()
                control_fp.write(description)
            else:
                synopsis = None
                if descriptions_found == 1:
                    synopsis = first_description
                else:
                    synopsis = "components from Fiji, including: "+", ".join(readable_names)
                synopsis = re.sub('\s+',' ',synopsis)
                control_fp.write('Description: '+trim_line(synopsis,70)+"\n")
                if full_description:
                    if descriptions_found == 1:
                        control_fp.write(" The description below is extracted from the Fiji Updater database.\n")
                    else:
                        control_fp.write(" The descriptions below are extracted from the Fiji Updater database:\n")
                    control_fp.write(full_description.encode('UTF-8'))

        control_fp.write("\n\nPackage: fiji\n")
        control_fp.write("Section: graphics\n")
        control_fp.write("Architecture: all\n")
        control_fp.write("Priority: extra\n")
        # control_fp.write("Version: "+version_from_changelog+"\n")
        control_fp.write("Depends: ")
        depends_line = ", ".join([ x+" (>= "+most_recent_package_version[x]+")" for x in most_recent_package_version ])
        control_fp.write(depends_line+"\n")
        fp = open(os.path.join(source_directory,"debian/package-extras/fiji/description"))
        description = fp.read().strip()
        fp.close()
        control_fp.write(description+"\n")
        control_fp.close()

        os.chdir(source_directory)
        call(["git","commit","-m","An automatic commit of the new debian/control from debian/update-debian.py","debian/control"])

        sys.exit(0)

    if options.all_dependencies:
        r = re.compile(re.escape(options.all_dependencies))
        matching_files = [ f for f in filename_to_object if r.search(f) ]
        if len(matching_files) > 1:
            print >> sys.stderr, "More than one file matched '"+options.all_dependencies+"':"
            for m in matching_files:
                print >> sys.stderr, "  "+m
            sys.exit(1)
        elif len(matching_files) == 0:
            print >> sys.stderr, "No files matched '"+options.all_dependencies+"'"
            sys.exit(1)
        options.depended_on_by = matching_files[0]
        options.depends_on = matching_files[0]

    if options.depended_on_by:
        for t in dependencies_of_file(filename_to_object,options.depended_on_by):
            print options.depended_on_by+ " depends (possibly indirectly) on "+t.filename
    if options.depends_on:
        for t in dependencies_of_file(filename_to_object,options.depends_on,reverse=True):
            print t.filename+" depends (possibly indirectly) on "+options.depends_on

    if options.depends_on or options.depended_on_by:
        sys.exit(0)

    if False:
        generate_dependency_graph(filename_to_object,"fiji-dependencies")
