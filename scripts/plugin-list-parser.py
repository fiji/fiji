#!/usr/bin/python

import os, stat, types
import sys
from xml.etree.cElementTree import *

def walktree(top = ".", depthfirst = True):
    """Walk the directory tree, starting from top. Credit to Noah Spurrier and Doug Fort."""
    import os, stat, types
    names = os.listdir(top)
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
    If a sub-element with the same taf exists, it si not created, but walked through, so as
    to avoid duplicate branches.
    Returns the element leaf."""
    current_branch = branch
    for el in list:
        new_branch = current_branch.find(el)
        if new_branch is None:
            # sub-element does not exist, create it
            new_branch = SubElement(current_branch, el)
        current_branch = new_branch
    return current_branch

def appendJarToTree(root_tree, path_string, name, class_name, package_name):
    menu_path = path_string.split('>')
    leaf = branchTree(root_tree, menu_path)
    leaf.attrib[name] = {'file':class_name, 'package':package_name}

def appendPluginToTree(root_tree, path, plugin_filename):
    rel_path = os.path.split(path.split(PLUGINS_FOLDER)[1])[1:]
    leaf = branchTree(root_tree, rel_path)
    name = plugin_filename.split('.')[0].replace('_',' ').strip()
    leaf.attrib[name] = {'file':plugin_filename, 'package':''}        

def appendConfigFile(root_tree, path):
    """Analyze the content of a .config file and get the location of its
    indexed compenents."""
    package_name = os.path.split(path)[-1].split('.')[0]
    file = open(path);
    for line in file:
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
        appendJarToTree(root_tree, menu_location, plugin_name, class_file_called, package_name )
    
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
                if os.path.exists(config_file_path):
                    appendConfigFile(tree, config_file_path)
                else: # Append a jar as it is 
                    appendPluginToTree(plugin_branch, top, name)
                                
            else: # Plain plugin
                appendPluginToTree(plugin_branch, top, name)
    
    return tree

def outputNode(node, level=0):
    title_tag = (2+level)*'='
    title_string = '\n' + title_tag + ' ' + node.tag + ' ' + title_tag + '\n'
    # Echo section title
    print title_string
    # Echo content
    keys = node.attrib.keys()
    for key in keys:
        plugin_line = '* ' + '[[' + key + ']]' + ' - file ' + "''" + node.attrib[key]['file'] + "''"
        if node.attrib[key]['package'] != '':
            plugin_line += ' in package ' + "'''[[" + node.attrib[key]['package'] +"]]'''"
        
        print plugin_line 
    # Recursive into children
    for child in node.getchildren():
        outputNode(child, level+1)

def outputPluginsTree(tree):
    nodes = tree.getchildren()
    for node in nodes:
        print 5*'\n'
        outputNode(node)
        


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

if len(sys.argv) < 2:
    fiji_folder = os.path.curdir
else:
    fiji_folder = sys.argv[1]
    
plugins_tree = createPluginsTree(fiji_folder)
outputPluginsTree(plugins_tree)