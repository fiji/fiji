#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --headless --jython "$0" "$@" # (call again with fiji)'''

from ij import IJ
import os, stat, types
import zipfile
import sys
from xml.etree.cElementTree import *


"""
This script parses the plugins folder content, and tries to build a list
of Fiji plugins from it, formatted to be pasted in MediaWiki. Optionally,
it can upload the changes right away.

It fetches information on menu item position and files called by
parsing jars and the staged-plugins.

J.Y. Tinevez - 2009
"""

URL = 'http://pacific.mpi-cbg.de/wiki/index.php'
PAGE = 'Template:PluginList'

def walktree(top = ".", depthfirst = True):
    """Walk the directory tree, starting from top. Credit to Noah Spurrier and Doug Fort."""
    import os, stat, types
    names = os.listdir(top)
    names.sort()
    if not depthfirst:
        yield top, names
    for name in names:
        try:
            st = os.lstat(os.path.join(top, name))
        except os.error:
            continue
        if stat.S_ISDIR(st.st_mode):
            for (newtop, children) in walktree (os.path.join(top, name), depthfirst):
                yield newtop, children
    if depthfirst:
        yield top, names

def branchTree(branch, list):
    """Add a list of element whose tag are given in a list to the given ElementTree element.
    If a sub-element with the same tag exists, it is not created, but walked through, so as
    to avoid duplicate branches.
    Returns the element leaf."""
    # Remove all empty strings elements
    while True:
        try:
            list.remove('')
        except ValueError:
            break
    # Tags don't like white space
    new_list = []
    for l in list:
      new_list.append(l.replace(' ',''))
    unique_tag = [reduce(lambda x,y: x + '>' + y, new_list[:n+1]) for n in range(len(new_list))]
    menu_path = [reduce(lambda x,y: x + ' > ' + y, list[:n+1]) for n in range(len(list))]

    current_branch = branch
    for el in unique_tag:
        if len(el) < 2:
            # The string item is too small, skip it
            continue
        # print(' ')
        # print('Trying to find "' + el + '" in "' + current_branch.tag + '"')
        # print('Content of ' + current_branch.tag)
        # for i in current_branch:
        #  print('\t"' + i.tag +'"')
        new_branch = current_branch.find(el)
        if new_branch is None:
        #    print('Not found')
            # sub-element does not exist, create it
            new_branch = SubElement(current_branch, el)
            new_branch.set('tittle_string', menu_path[unique_tag.index(el)])
        #else:
        #    print('Found')
        current_branch = new_branch
    return current_branch

def appendJarToTree(root_tree, path_string, name, class_name, package_name, type):
    menu_path = path_string.split('>')
    leaf = branchTree(root_tree, menu_path)
    leaf.attrib[name] = {'file':class_name, 'package':package_name, 'type':type}

def appendPluginToTree(root_tree, path, plugin_filename, type):
    if plugin_filename.find('_') == -1:
        # Plugin filename has no underscore in it, so it won't appear in the
        # menu. Skip.
        return
    rel_path = path.split(PLUGINS_FOLDER)[1][1:]  # Get the path after plugin folder, removing trailing '/'
    rel_path = list(os.path.split(rel_path))
    # We must add a fake plugin root folder:
    rel_path.insert(0, PLUGINS_MENU_NAME,)
    leaf = branchTree(root_tree, rel_path)
    name = plugin_filename.split('.')[0].replace('_',' ').strip()
    leaf.attrib[name] = {'file':plugin_filename, 'package':'', 'type':type}

def appendConfigFile(root_tree, config_file_iterable, package_name, type):
    """Analyze the content of a .config file and append its  indexed compenents
    to the plugin tree."""
    for line in config_file_iterable:
        if line.startswith('#'):      continue # Comment
        elif len(line.strip()) == 0:  continue # Empty, or so

        # The rest, should be formatted as MainMenu>SubMenu, "Plugin Name in menu", path.to.class.in.jar.file
        line_parts = line.split(',')
        if len(line_parts) < 3:
            # Typically a '-' to insert a separator in menus
            continue
        menu_location = line_parts[0]
        plugin_name = line_parts[1].replace('"','').strip() # remove '"'
        class_file_called = line_parts[2].strip()
        appendJarToTree(root_tree, menu_location, plugin_name, class_file_called, package_name, type)

def appendJarWithStagedConfigFile(root_tree, config_file_path, type):
    """Open an external .config file and get the location of its
    indexed compenents."""
    package_name = os.path.split(config_file_path)[-1].split('.')[0]
    file = open(config_file_path);
    appendConfigFile(root_tree, file, package_name, type)


def appendJarWithConfigFile(root_tree, jarfile_path, type):
    """Analyze the content of a plugins.config embeded in a jar, and get the
    location of its indexed compenents."""
    package_name = os.path.split(jarfile_path)[-1].split('.')[0]
    jar = zipfile.ZipFile(jarfile_path)
    config_file = jar.read(PLUGINS_CONFIG_FILENAME)
    jar.close()
    lines = config_file.split('\n')
    appendConfigFile(root_tree, lines, package_name, type)

def hasConfigFileInJar(jarfile_path):
    """Returns true if the jar file whose path is given in argument has a file
    called plugins.congi in it."""
    if not zipfile.is_zipfile(jarfile_path): return False
    jar = zipfile.ZipFile(jarfile_path)
    files_in_jar = jar.namelist()
    jar.close();
    if PLUGINS_CONFIG_FILENAME in files_in_jar: return True
    return False



def createPluginsTree(fiji_folder):

    plugins_location = os.path.join(fiji_folder, PLUGINS_FOLDER)
    staged_plugins_location = os.path.join(fiji_folder, STAGED_PLUGINS_FOLDER)

    # Initiate the tree
    tree = Element('root')
    # Immediatly add a 'Plugins' branch to it
    plugin_branch = SubElement(tree, PLUGINS_MENU_NAME)

    top = plugins_location
    for top, names in walktree(top):
        for name in names:

            # Get filename and type
            split_filename = os.path.splitext(name)
            file_extension = split_filename[1]
            file_name = split_filename[0]
            type = PLUGINS_TYPE.get(file_extension)

            if type == None: # Folders or gremlins
                continue

            elif type == PLUGINS_TYPE.get(JAR_EXTENSION): # Look for location in case of jar file
                config_file_path = os.path.join(staged_plugins_location,file_name+'.config')
                jar_file_path = os.path.join(plugins_location, name)
                if os.path.exists(config_file_path):
                    # A config file was found in the staged-plugins folder, use this one
                    appendJarWithStagedConfigFile(tree, config_file_path, type)
                elif hasConfigFileInJar(jar_file_path):
                    # look for a plugins.config file within the jar
                    appendJarWithConfigFile(tree, jar_file_path, type)
                else: # Append a jar as it is
                    appendPluginToTree(tree, top, name, type)

            else: # Plain plugin
                appendPluginToTree(tree, top, name, type)

    return tree

firstNode = True

def nodeToString(node, level=0):
    result = ''
    title_tag = (2+level)*'='
    title_string = title_tag + ' ' + node.get('tittle_string','Plugins') + ' ' + title_tag + '\n'
    # Echo section title
    global firstNode
    if firstNode:
        firstNode = False
    else:
        result += (4-level)*'\n'
    result += title_string + '\n'
    # Echo content
    keys = node.attrib.keys()
    for key in keys:
        if key == 'tittle_string':  # Skip the tittle
          continue
        plugin_line = '* ' + '[[' + key + ']]' + ' - file ' + "<tt>" + node.get(key,'{}').get('file','#') + "</tt>"
        if node.get('package','#') != '#':
            plugin_line += ' in package ' + "'''[[" + node.get(key,{}).get('package','#') +"]]'''"
        plugin_line += "  -- ''" + node.get(key,{}).get('type','#') + "''"

        if plugin_line != '':
            result += plugin_line + '\n'
    # Recursive into children
    for child in node.getchildren():
        result += nodeToString(child, level+1)
        if result == '\n' or result == '\n\n' or result == '\n\n\n' or result == '\n\n\n\n':
             print 'child', child
    return result

def pluginsTreeToString(tree):
    global firstNode
    firstNode = True
    result = ''
    nodes = tree.getchildren()
    for node in nodes:
        if result != '':
		result += 3*'\n'
        result += nodeToString(node)
    return result



# -------------------------------
#       MAIN
# -------------------------------

# Define dictionaries
JAR_EXTENSION = '.jar'
PLUGINS_TYPE = {JAR_EXTENSION:'java jar file',
                '.class':'java class file',
                '.txt':'macro',
                '.js':'javascript file',
                '.rb':'jruby script',
                '.py':'jython script',
                '.clj':'clojure script'}
# Folder names
PLUGINS_FOLDER = 'plugins'
STAGED_PLUGINS_FOLDER = 'staged-plugins'
PLUGINS_MENU_NAME = 'Plugins'
PLUGINS_CONFIG_FILENAME = 'plugins.config'

uploadToWiki = False
if len(sys.argv) > 1 and sys.argv[1] == '--upload-to-wiki':
    uploadToWiki = True
    sys.argv = sys.argv[:1] + sys.argv[2:]

if len(sys.argv) < 2:
    fiji_folder = os.path.curdir
else:
    fiji_folder = sys.argv[1]

# Create the tree
plugins_tree = createPluginsTree(fiji_folder)

# Output it
result = pluginsTreeToString(plugins_tree)
if uploadToWiki:
    from fiji import MediaWikiClient

    client = MediaWikiClient(URL)
    wiki = client.sendRequest(['title', PAGE, 'action', 'edit'], None)
    if wiki.replace('&lt;', '<') != result:
        # get username and password
        user = None
        password = None
        from os import getenv, path
        home = getenv('HOME')
        if home != None and path.exists(home + '/.netrc'):
            host = URL
            if host.startswith('http://'):
                host = host[7:]
            elif host.startswith('https://'):
                host = host[8:]
            slash = host.find('/')
            if slash > 0:
                host = host[:slash]

            found = False
            f = open(home + '/.netrc')
            for line in f.readlines():
                line = line.strip()
                if line == 'machine ' + host:
                    found = True
                elif found == False:
                    continue
                elif line.startswith('login '):
                    user = line[6:]
                elif line.startswith('password '):
                    password = line[9:]
                elif line.startswith('machine '):
                    break
            f.close()

        if not client.isLoggedIn():
            if user != None and password != None:
                client.logIn(user, password)
		        response = client.uploadPage(PAGE, result, 'Updated by plugin-list-parser')
		        if client.isLoggedIn():
		            client.logOut()
		        if not response:
		            print 'There was a problem with uploading', PAGE
		            if IJ.getInstance() == None:
			            sys.exit(1)
            else:
                print 'No .netrc entry for', URL
                if IJ.getInstance() == None:
	                sys.exit(1)
else:
    print result