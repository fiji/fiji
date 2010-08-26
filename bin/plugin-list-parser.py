#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --headless --jython "$0" "$@" # (call again with fiji)'''

from fiji import User_Plugins
from ij import IJ
from java.io import File
import os, stat, types
import zipfile
import sys


"""
This script parses the plugins folder content, and tries to build a list
of Fiji plugins from it, formatted to be pasted in MediaWiki. Optionally,
it can upload the changes right away. Or compare to the current version on
the Wiki.

It fetches information on menu item position and files called by
letting fiji.User_Plugins parse the jars.

J.Y. Tinevez - 2009, J. Schindelin - 2010
"""

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

def splitLast(string, separator, emptyLeft = None):
    """ Splits the string into two parts at the last location of the separator."""
    offset = string.rfind(separator)
    if offset < 0:
        return [emptyLeft, string]
    return string[:offset], string[offset + 1:]

def getTree(menuPath):
    """Get the tree (a list) for a given menuPath (e.g. File>New)"""
    global allElements, root
    if menuPath.endswith('>'):
        menuPath = menuPath[:-1]
    if menuPath in allElements:
        result = allElements[menuPath]
    else:
        result = []
        parentMenuPath, dummy = splitLast(menuPath, '>', '')
        parentTree = getTree(parentMenuPath)
        parentTree.append([menuPath, result])
        allElements[menuPath] = result
    return result

def appendPlugin(menuPath, name, class_name, package_name, type, path = None):
    tree = getTree(menuPath)
    if path != None and path.startswith(PLUGINS_FOLDER):
        path = path[len(PLUGINS_FOLDER):]
    tree.append({'name': name, 'path': path, 'class': class_name, 'package': package_name, 'type': type})

def appendJar(jarfile_path, type):
    """Analyze the content of a plugins.config embeded in a jar, and get the
    location of its indexed compenents."""
    for line in User_Plugins(False).getJarPluginList(File(jarfile_path), 'Plugins'):
        packageName, className = splitLast(line[2], '.')
        appendPlugin(line[0], line[1], className, packageName, type, jarfile_path)

def createPluginsTree(fiji_folder):
    plugins_location = os.path.join(fiji_folder, PLUGINS_FOLDER)

    for top, names in walktree(plugins_location):
        for name in names:
            # Get filename and type
            split_filename = os.path.splitext(name)
            file_extension = split_filename[1]
            file_name = split_filename[0]
            type = PLUGINS_TYPE.get(file_extension)

            if type == None or file_name.find('$') >= 0: # Folders or gremlins
                continue
            elif type == PLUGINS_TYPE.get(JAR_EXTENSION):
                appendJar(os.path.join(plugins_location, name), type)
            else: # Plain plugin
                menuPath = top[top.find(PLUGINS_FOLDER) + len(PLUGINS_FOLDER) + 1:].replace('/', '>')
                if menuPath == '':
                    menuPath = 'Plugins'
                else:
                    menuPath = 'Plugins>' + menuPath
                menuItemLabel = name[:-len(file_extension)].replace('_', ' ')
                appendPlugin(menuPath, menuItemLabel, name, None, type, name)

def treeToString(tree, level=1):
    global firstNode
    result = ''

    # first handle the commands
    for element in tree:
        if type(element) is dict:
            # if it is a dict, it describes one menu entry
            if element['class'] == None:
                element['class'] = '#'
            if element['package'] != None:
                element['class'] = element['package'] +"." + element['class']
                element['package'] = None
            plugin_line = '* ' + '[[' + element['name'] + ']]' + ' - file ' + "<tt>" + element['class'] + "</tt>"
            plugin_line += "  -- ''" + element['type'] + "''"
            result += plugin_line + '\n'

    # then handle the submenus
    for element in tree:
        if type(element) is list:
            # submenu: a list of the form [title, tree]
            # Echo section title
            if firstNode:
                firstNode = False
            else:
                result += (4-level)*'\n'
            title_tag = (1+level)*'='
            title_string = title_tag + ' ' + element[0].replace('>', ' > ') + ' ' + title_tag + '\n'
            result += title_string + '\n' + treeToString(element[1], level + 1)
    return result

def pluginsTreeToString():
    global firstNode
    firstNode = True
    return treeToString(allElements.get(''))



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
PLUGINS_MENU_NAME = 'Plugins'

URL = 'http://pacific.mpi-cbg.de/wiki/index.php'
PAGE = 'Template:PluginList'

allElements = dict()
allElements[''] = []

uploadToWiki = False
compareToWiki = False
if len(sys.argv) > 1 and sys.argv[1] == '--upload-to-wiki':
    uploadToWiki = True
    sys.argv = sys.argv[:1] + sys.argv[2:]
elif len(sys.argv) > 1 and sys.argv[1] == '--compare-to-wiki':
    compareToWiki = True
    sys.argv = sys.argv[:1] + sys.argv[2:]

if len(sys.argv) < 2:
    fiji_folder = os.path.curdir
else:
    fiji_folder = sys.argv[1]

# Create the tree
createPluginsTree(fiji_folder)

# Output it
result = pluginsTreeToString()
if uploadToWiki or compareToWiki:
    from fiji import MediaWikiClient

    client = MediaWikiClient(URL)
    wiki = client.sendRequest(['title', PAGE, 'action', 'edit'], None)
    begin = wiki.find('<textarea')
    begin = wiki.find('>', begin) + 1
    end = wiki.find('</textarea>', begin)
    wiki = wiki[begin:end].replace('&lt;', '<')
    if wiki != result:
        if compareToWiki:
            from fiji import SimpleExecuter
            from java.io import File, FileWriter
            file1 = File.createTempFile('PluginList', '.wiki')
            writer1 = FileWriter(file1)
            writer1.write(wiki)
            writer1.close()
            file2 = File.createTempFile('PluginList', '.wiki')
            writer2 = FileWriter(file2)
            writer2.write(result)
            writer2.close()
            diff = SimpleExecuter(['git', 'diff', '--patience', '--no-index', '--src-prefix=wiki/', '--dst-prefix=local/', file1.getAbsolutePath(), file2.getAbsolutePath()])
            file1.delete()
            file2.delete()
            print diff.getOutput()
        else:
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