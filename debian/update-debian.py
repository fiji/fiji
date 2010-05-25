#!/usr/bin/python2.5

import os
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

# On Ubuntu and Debian, the required Java3D jars are in these packages:
#
#   j3dcore.jar =>          libjava3d-java
#   j3dutils.jar =>         libjava3d-java
#   vecmath.jar =>          libvecmath-java
#   libj3dcore-ogl.so =>    libjava3d-jni
#   libj3dcore-ogl-cg.so => [missing - probably not needed?]

build_dependencies = [ "debhelper (>= 5)",
                       "gcc",
                       "lsb-release",
                       "dpkg-dev",
                       "openjdk-6-jdk",
                       "libjama-java (>= 1.0.2)",
                       "ant",
                       "ant-optional",
                       "bsh",
                       "libbatik-java",
                       "libxml-commons-external-java",
                       "junit4 (>= 4.3)",
                       "weka",
                       "libitext1-java",
                       "jython",
                       "libjzlib-java",
                       "clojure",
                       # Annoyingly, clojure-contrib isn't yet in Ubuntu
                       "rhino",
                       "libxml-commons-external-java",
                       "libpg-java",
                       "libjsch-java",
                       "libjcommon-java",
                       "libjfreechart-java",
                       "libjava3d-java",
                       "libvecmath-java",
                       "libjava3d-jni" ]

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

default_package = "fiji-plugins"

destination_fiji_prefix = "usr/lib/fiji/"

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

    "fiji-clojure-contrib" :
        [ "jars/clojure-contrib.jar" ],

    "fiji-ij-imageio":
        [ "plugins/ij-ImageIO_.jar" ],

    "fiji-jai":
        [ "jars/clibwrapper_jiio.jar",
          "jars/jai_codec.jar",
          "jars/jai_core.jar" ],

    "fiji-imglib" :
        [ "jars/imglib.jar" ],

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
        [ "plugins/IO_.jar" ]
}

file_to_package_name_dictionary = {}
regular_expressions_to_package = []

# This is filled in when we actually list the files:
package_name_to_files = {}

for package_name, contents in package_name_to_file_matchers.items():
    for f in contents:
        if type(f) == str:
            file_to_package_name_dictionary[f] = package_name
        elif type(f) == tuple:
            file_to_package_name_dictionary[f[0]] = package_name
        elif hasattr(f,'pattern'):
            regular_expressions_to_package.append( (f,package_name) )

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

options,args = parser.parse_args()

source_directory = os.path.split(script_directory)[0]
os.chdir(source_directory)

if options.add_changelog_template:
    suggest_new_version = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    git_rev = Popen(["git","rev-parse","HEAD"],stdout=PIPE).communicate()[0].strip()
    fp = open("debian/changelog")
    old_changelog = fp.read()
    fp.close()
    fp = open("debian/changelog","w")
    fp.write("fiji (%s) unstable; urgency=low\n\n"%(suggest_new_version))
    fp.write("  * Based on fiji.git at "+git_rev+"\n")
    fp.write("    [Fill in the rest of the commit message here.]\n")
    fp.write("\n")
    fp.write(" -- Mark Longair <mhl@pobox.com>  "+time.strftime("%a, %d %b %Y %H:%M:%S +0000",time.gmtime())+"\n\n")
    fp.write(old_changelog)
    fp.close()
    sys.exit(0)

# Find the version from the changelog:
p = Popen(["dpkg-parsechangelog","-l"+os.path.join(script_directory,"changelog")],stdout=PIPE)
changelog = p.communicate()[0]
if p.returncode != 0:
    raise Exception, "Failed to parse debian/changelog"

version_match = re.search("(?ims)Version: (\S+)",changelog)
if not version_match:
    raise Exception, "Failed to find the Version field"

version_from_changelog = version_match.group(1)

if not re.match('^\d{14}$',version_from_changelog):
    print >> sys.stderr, "Error: The version number is not of the form YYYYMMDDHHMMSS"
    sys.exit(6)

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

if options.install or options.generate_complete_control or options.generate_source_control:

    os.chdir(source_directory)

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

if options.install:
    for f in all_files_to_install:
        print "Trying to install: "+f
        package = filename_to_package(f)
        destination_filename = os.path.join(source_directory,"debian",package,destination_fiji_prefix,f)
        install_file(f,destination_filename)

def clean_untracked(top_level_working_directory):
    os.chdir(top_level_working_directory)
    call("git ls-files --others --directory -z | xargs -0 rm -rf",shell=True)
    for s in absolute_submodule_paths(top_level_working_directory):
        clean_untracked(s)

def clean_aggressively(top_level_working_directory):
    os.chdir(top_level_working_directory)
    call("git ls-files --others --directory --no-empty-directory --exclude-standard -z | xargs -0 rm -rf",shell=True)
    call("git clean -f -X -d",shell=True)
    for s in absolute_submodule_paths(top_level_working_directory):
        clean_aggressively(s)

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
    to_remove.append("weka")
    to_remove.append("java/linux")
    to_remove.append("java/linux-amd64")
    to_remove.append("java/macosx-java3d")
    to_remove.append("java/win32")
    to_remove.append("java/win64")
    to_remove.append("livehelper")
    to_remove.append("Retrotranslator")
    to_remove.append("jython")
    to_remove.append("clojure")
    to_remove.append("junit")
    to_remove.append("jars/js.jar")
    to_remove.append("jars/itext*.jar")
    to_remove.append("jars/jzlib*.jar")
    to_remove.append("jars/jcommon*.jar")
    to_remove.append("jars/jfreechart*.jar")
    to_remove.append("jars/jsch*.jar")
    to_remove.append("jars/postgresql*.jar")
    to_remove.append("jars/ant*.jar")
    to_remove.append("jars/batik.jar")
    to_remove.append("jars/junit*.jar")
    to_remove.append("jars/weka.jar")

    for f in to_remove:
        call(["rm -rf "+f],shell=True)

    # Now rewrite the Fakefile to remove these plugins:

    fp = open("Fakefile")
    oldlines = fp.readlines()
    fp.close()

    fp = open("Fakefile","w")
    for line in oldlines:
        if re.search("TurboReg_",line):
            continue
        if re.search("TransformJ_",line):
            continue
        if re.search("(^\s*jars|precompiled)/weka.jar",line):
            continue
        if re.search("(^\s*jars|precompiled)/jython.jar",line):
            continue
        if re.search("(^\s*jars|precompiled)/clojure.jar",line):
            continue
        if re.search("(^\s*jars|precompiled)/junit-4.5.jar",line):
            continue
        fp.write(line)
    fp.close()

    call("rm -rfv precompiled/*",shell=True)
    sys.exit(0)

if options.generate_complete_control or options.all_dependencies or options.depended_on_by or options.depends_on:

    # Check that the source:

    package_url = 'http://pacific.mpi-cbg.de/update/db.xml.gz'

    # Fetch a new db.xml:

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
    # between all the objects:

    for filename, o in filename_to_object.items():
        o.update_dependency_list(filename_to_object)

    def strip_extension(filename):
        return re.sub('\..*$','',filename)

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
        # FIXME: also add a meta-package here...
        control_fp.close()

    if options.generate_complete_control:

        control_fp = open("debian/control","a")

        most_recent_package_version = {}

        for p in package_name_to_files:
            print "package "+p
            files = package_name_to_files[p]
            required_packages = {}
            for x in files:
                if x not in filename_to_object:
                    print >> sys.stderr, "Warning: couldn't find dependencies for "+x
                    continue
                most_recent_package_version.setdefault(p,-1)
                x_timestamp = filename_to_object[x].timestamp
                if x_timestamp > most_recent_package_version[p]:
                    most_recent_package_version[p]  = x_timestamp
                # So for each file in this package, find its dependent
                # files:
                for d in filename_to_object[x].depends_on:
                    if not os.path.exists(d.filename):
                        print >> sys.stderr, "        Skipping dependent file %s since it doesn't exist"
                        continue
                    other_package = filename_to_package(d.filename)
                    print "        ("+x+" => "+d.filename+" ["+other_package+"])"
                    # If the dependent file is actually in the same
                    # package then just ignore it:
                    if other_package == p:
                        continue
                    required_packages.setdefault(other_package,d.timestamp)
                    if d.timestamp > required_packages[other_package]:
                        required_packages[other_package] = d.timestamp
            for package_name, most_recent_requirement in required_packages.items():
                print "   requires "+package_name+" >= "+most_recent_requirement

            dependencies = []
            if required_packages:
                for dependent_package, timestamp in required_packages.items():
                    s = dependent_package+" (>= "+timestamp+")"
                    dependencies.append(s)

            control_fp.write("\n\nPackage: "+p+"\n")
            control_fp.write("Section: graphics\n")
            control_fp.write("Architecture: any\n")
            control_fp.write("Priority: extra\n")
            # control_fp.write("Version: "+version_from_changelog+"\n")
            control_fp.write("Depends: "+", ".join(dependencies)+"\n")
            description_filename = os.path.join(source_directory,"debian","package-extras","default-description")
            ideal_description_filename = os.path.join(source_directory,"debian","package-extras",p,"description")
            if os.path.exists(ideal_description_filename):
                description_filename = ideal_description_filename
            fp = open(description_filename)
            description = fp.read().strip()
            fp.close()
            control_fp.write(description)

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

# Group these files into their Debian packages:

# Undecided:

# == plugins/AnalyzeSkeleton_.jar
# == plugins/Analyze_Reader_Writer.jar
# == plugins/Arrow_.jar
# == plugins/Auto_Threshold.jar
# == plugins/Colocalisation_Analysis.jar
# == plugins/Daltonize_.jar
# == plugins/Fiji_Plugins.jar
# == plugins/FlowJ_.jar
# == plugins/Gray_Morphology.jar
# == plugins/IJ_Robot.jar
# == plugins/Image_5D.jar
# == plugins/PIV_analyser.jar
# == plugins/Record_Screen.jar
# == plugins/Skeletonize3D_.jar
# == plugins/SplineDeformationGenerator_.jar
# == plugins/Stack_Manipulation.jar
# == plugins/Statistical_Region_Merging.jar
# == plugins/Stitching_.jar
# == plugins/Time_Stamper.jar
# == plugins/ToAST_.jar
# == plugins/View5D_.jar
# == plugins/level_sets.jar
# == plugins/registration_3d.jar
# == plugins/Calculator_Plus.jar
# == plugins/Sync_Win.jar
# == plugins/Macros/Bulls_Eye.txt
# == plugins/Macros/About_Plugin_Macros.txt
# == plugins/Macros/batch_convert_any_to_tif.txt
# == plugins/Macros/RGB_Histogram.txt
# == plugins/Macros/Polygon_.txt
# == plugins/Utilities/Close_All_Without_Saving.txt
# == plugins/Analyze/Measure_RGB.txt
# == macros/listManagement.txt
# == macros/toolsets/Drawing Tools.txt
# == plugins/Analyze/Dynamic_ROI_Profiler.clj
# == scripts/Record_Desktop.py
# == scripts/Record_Window.py
# == macros/spirals.txt
# == plugins/Manual_Tracking.jar
# == plugins/3D_Objects_Counter.jar
# == plugins/LocalThickness_.jar
# == plugins/Siox_Segmentation.jar
# == jars/VectorString.jar
# == plugins/RATS_.jar
# == plugins/Directionality_.jar
# == plugins/Video_Editing.jar
# == plugins/register_virtual_stack_slices.jar
