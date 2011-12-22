#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

from xml.dom.minidom import parseString
import re
from elementtidy import TidyHTMLTreeBuilder
import urllib
import elementtree.ElementTree as ET
import urlparse
import posixpath


# -------------------------------
#       PUBLIC ATTRIBUTES
# -------------------------------

# regular expression to find a maintainer
MAINTAINER_TAG = 'Maintainer'
# URL to the wiki
WIKI_URL = 'http://fiji.sc'
# URL to the plugin list
PLUGIN_LIST_URL = WIKI_URL + '/wiki/index.php/Template:PluginList';


# -------------------------------
#       PRIVATE ATTRIBUTES
# -------------------------------

# Xhtml tag
__XHTML = "{http://www.w3.org/1999/xhtml}"
# Rex rule to find header tags in html
__HEADER_REGEXP = re.compile('h(?P<hlevel>\d)')

# -------------------------------
#       PUBLIC FUNCTIONS
# -------------------------------


def findMaintainer(tree):    
    for table in tree.findall(".//"+__XHTML+"table"):
        table_class = table.get("class")
        if table_class != "infobox": continue
        for tr in table.getiterator():
            for child in tr:
                text = child.text
                if text is None: continue
                if MAINTAINER_TAG.lower() in text.lower():
                    # the parent (tel) of this child has a "maintainer" text in it
                    plugin_name = tree.find(".//"+__XHTML+"h1").text
                    td = tr.find(__XHTML+'td')
                    if (td.text is None) or (td.text.strip() is None):
                        # Check if we have a maintainer in the shape of a link or somthing else
                        if len(td) == 0:
                            # It has no children, so it is empty
                            return None
                        else:
                            # Find the first subelement which text is not empty
                            for sub_td in td:
                                if not ( (sub_td.text is None) or (sub_td.text.strip() is None) ):
                                    return sub_td.text.strip()
                            return 'Could not parse mainainer name.'
                    else:
                        # We have a maintainer entered as plain text in the td tag
                        return td.text.strip()
                
def prettyPrint(element):
    txt = ET.tostring(element)
    print parseString(txt).toprettyxml()


def getPluginListHTMLpage():
    """Returns the raw html of the wiki age that collect plugin hierarchy."""
    plugin_list_page_tree = TidyHTMLTreeBuilder.parse(urllib.urlopen(PLUGIN_LIST_URL))
    return plugin_list_page_tree
    

def getPluginListTree():
    """Returns the ElementTree of the plugin hierarchy, as it is on the plugin
    list wiki page."""
    
    plugin_list_page_tree = getPluginListHTMLpage();
    # Get the body-content div
    body = plugin_list_page_tree.find( __XHTML+'body')
    body_content = body.find(__XHTML+"div[@id='globalWrapper']/"+
                             __XHTML+"div[@id='column-content']/"+
                             __XHTML+"div[@id='content']/"+
                             __XHTML+"div[@id='bodyContent']/")
    # Get rid of the toc
    body_elements = body_content[6:] 
    # Build the tree recursively
    root = ET.Element('plugin_hierarchy')
    root.append( __createChildElement("toplevel", 1, body_elements) )
    return root
    
def getPackageTree():
    """Returns the ElementTree of the packages that can be found in the plugin
    hierarchy, as it is on the plugin list wiki page."""
    plugin_list_page_tree = getPluginListHTMLpage();
    # Get the body-content div
    body = plugin_list_page_tree.find( __XHTML+'body')
    body_content = body.find(__XHTML+"div[@id='globalWrapper']/"+
                             __XHTML+"div[@id='column-content']/"+
                             __XHTML+"div[@id='content']/"+
                             __XHTML+"div[@id='bodyContent']/")
    # Get rid of the toc
    body_elements = body_content[6:] 
    # Build the tree 
    root = ET.Element('package_list')
    for element in body_content.getiterator():
        
        # Plugins are in HTML unordered list
        if not __XHTML+'ul' in element.tag:
            continue
        
        for li_child in element:
            
            # Each plugin is listed in a html list item
            if not __XHTML+'li' in li_child.tag: continue
            
            # For this, we depend strongly on html markups chosen by the
            # script that has generated this page
            a_el = li_child.find(__XHTML+'a')
            if a_el is None: continue
            plugin_package = li_child.find(__XHTML+'b')
            if plugin_package is not None:
                plugin_package = plugin_package.find(__XHTML+'a')
                if plugin_package is not None:
                    package_attrib = __parseAElement(plugin_package)
                    # Check if we did not already parsed this one
                    if root.find("package[@name='" + package_attrib['name'] + "']") is None:
                        new_element = ET.SubElement(root, 'package')
                        new_element.attrib = package_attrib
    
    return root


def createBlamePage():
    
    # Build blame list
    plugin_without_documentation_page = []
    plugin_without_maintainer = []
    package_without_documentation_page = []
    package_without_maintainer = []

    plugintree = getPluginListTree()
    plugin_els = plugintree.findall('.//plugin')
    for el in plugin_els:
        plugin_str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            plugin_without_documentation_page.append(plugin_str)
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)            
            if mtnr is None:
                plugin_without_maintainer.append(plugin_str)

                
    packagetree = getPackageTree()
    package_els = packagetree.findall('.//package')
    for el in package_els:
        package_str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            package_without_documentation_page.append(package_str)
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)
            if mtnr is None:
                package_without_maintainer.append(package_str)
    
    # Output blame list
    headers = [
        "== Plugins without documentation page ==",
        "== Plugins without maintainer ==",
        "== Packages without a documentation page ==",
        "== Packages without maintainer =="    ]
    lists = [
        plugin_without_documentation_page ,
        plugin_without_maintainer ,
        package_without_documentation_page, 
        package_without_maintainer     ]
    spacer = 2*'\n'
    
    for i in range(len(headers)):
        print spacer
        print headers[i]
        print spacer
        for line in lists[i]:
            print line
    
def createMaintainerPage():
    """Get the maintainer for each plugin and package, and generate a list of
    maintained item per maintainer."""
    
    # Build maintainer dicts
    plugin_maintainer_dict = {}
    package_maintainer_dict = {}
    
    plugintree = getPluginListTree()
    plugin_els = plugintree.findall('.//plugin')
    for el in plugin_els:
        plugin_str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            continue
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)            
            if mtnr is not None:
                mtnr = __cleanString(mtnr)
                if plugin_maintainer_dict.has_key(mtnr):
                    plugin_maintainer_dict[mtnr].append(plugin_str)
                else:
                    plugin_maintainer_dict[mtnr] = [ plugin_str ]
                    
                
    packagetree = getPackageTree()
    package_els = packagetree.findall('.//package')
    for el in package_els:
        package_str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            continue
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)
            if mtnr is not None:
                mtnr = __cleanString(mtnr)
                if package_maintainer_dict.has_key(mtnr):
                    package_maintainer_dict[mtnr].append(package_str)
                else:
                    package_maintainer_dict[mtnr] = [ package_str ]

    # Output maintainer dict
    maintainers = plugin_maintainer_dict.keys() + package_maintainer_dict.keys()
    maintainers = __unique(maintainers)
    
    wiki_page = '';
    wiki_page = wiki_page  + '{{ #switch:{{{maintainer|}}}' + 2*'\n'
    
    for maintainer in maintainers:
        wiki_page = wiki_page  + '| ' + maintainer + ' = \n'
        
        wiki_page = wiki_page  + '\n=== Plugins ===' + 2*'\n'
        plugins = plugin_maintainer_dict.get(maintainer,[])
        for plugin in plugins:
            wiki_page = wiki_page + plugin + '\n'

        wiki_page = wiki_page  + '\n=== Packages ===' + 2*'\n'
        packages = package_maintainer_dict.get(maintainer,[])
        for package in packages:
            wiki_page = wiki_page + package + '\n'
            
        wiki_page = wiki_page  + 2*'\n'
        
    wiki_page = wiki_page + '}}' + 2*'\n'
    wiki_page = wiki_page + '<noinclude>' + 2*'\n' \
                + '__NOTOC__' + 2*'\n' \
                + 'This template automatically generate a paragraph containing ' \
                + 'the list of plugins and packages maintained by a maintainer, as ' \
                + 'stated in the wiki. It is automatically generated from a python ' \
                + 'script in the Fiji development repository, that can be seen ' \
                + '[http:////fiji.sc/cgi-bin/gitweb.cgi?p=fiji.git;a=blob;f=scripts/plugin-documentation-list.py;hb=HEAD here]' \
                + '.\n\nSyntax is the ' \
                + 'following:' + 2*'\n' \
                + '<pre>\n' \
                + '== Plugins and Packages maintained by Mark Longair ==\n' \
                + '{{ Maintainers | maintainer = Mark Longair }} \n' \
                + '</pre>\n' \
                + '\n' \
                + '== Plugins and Packages maintained by Mark Longair ==\n' \
                + '{{ Maintainers | maintainer = Mark Longair }}\n' \
                + '<' + '\\' + 'noinclude>'
    
    print wiki_page
    

def getPluginHTMLpage(element):
    """Returns the raw html from the wiki page of the plugin referenced by
    this element."""
    rel_link = element.attrib['link']
    url = __join(WIKI_URL,rel_link)
    plugin_page = TidyHTMLTreeBuilder.parse(urllib.urlopen(url))
    return plugin_page

# -------------------------------
#       PRIVATE FUNCTIONS
# -------------------------------

def __unique(li):
    """Return a list made of the unique item of the given list.
    Not order preserving"""
    keys = {}
    for e in li:
        keys[e] = 1
    return keys.keys()

def __cleanString(input_str):
    """Used to remove parenthesis and their content from maintainer strinfgs."""
    new_str = re.sub('\(.*?\)', '', input_str)
    new_str = new_str.replace('(','')
    new_str = new_str.replace(')','')
    return new_str.strip()

def __join(base,url):
    join = urlparse.urljoin(base,url)
    url = urlparse.urlparse(join)
    path = posixpath.normpath(url[2])
    return urlparse.urlunparse(
        (url.scheme,url.netloc,path,url.params,url.query,url.fragment)
        )


def __parseAElement(alement):
    """Parse a Element made from a html link in the shape of
    <a href="link" class="new" title="TransformJ">TransformJ_</a> """    
    attrib = {}
    attrib['name'] = alement.attrib.get('title')
    attrib['link'] = alement.attrib['href']
    if alement.attrib.get('class') == 'new':
        attrib['has_page'] = 'no'
    else:
        attrib['has_page'] = 'yes'
    return attrib
    


def __createChildElement(current_name, current_hlevel, body_elements):
    
    # Create element for this section    
    current_element = ET.Element("h"+str(current_hlevel))
    current_element.attrib['name']=current_name
    
    # Go trough each element of this list

    while len(body_elements) > 0:
        
        # Pop the first element out
        element = body_elements.pop(0)
        
        m = __HEADER_REGEXP.search(element.tag)
        if m is not None:
            
            # Case 1: is a header 
            hlevel = int(m.group('hlevel'))        
            if hlevel > current_hlevel:
                # The found header has a hierrachy level strictly deeper than
                # the one we are currently parsing (e.g h3 > h2). As a
                # consequence, a new element should be made out of the found
                # header content. This element is going to be child of the
                # current element
                new_name = element[1].text
                #new_body_elements = body_elements [ index + 1 : ]
                new_element = __createChildElement(new_name, hlevel, body_elements)
                current_element.append(new_element)
                
            else:
                # The found header has a hierachy level equal or superior than the
                # one we are currently parsing (e.g h2 <= h2). As a consequence,
                # we are done parsing this one and the current element should
                # be returned.
                # But before, we have to put back this poped-out element for
                # the parent process
                body_elements.insert(0,element)
                return current_element


        # Case 2: look for plugin items
        # Plugins are in HTML unordered list
        if not __XHTML+'ul' in element.tag:
            continue
        
        for li_child in element:
            
            # Each plugin is listed in a html list item
            if not __XHTML+'li' in li_child.tag: continue
                        
            # For this, we depend strongly on html markups chosen by the
            # script that has generated this page
            plugin_attrib = {}
            a_el = li_child.find(__XHTML+'a')
            if a_el is None: continue
            plugin_attrib = __parseAElement(a_el)
            plugin_attrib['type'] = li_child.find(__XHTML+'i').text
            plugin_attrib['file'] = li_child.find(__XHTML+'tt').text
            plugin_package = li_child.find(__XHTML+'b')
            if plugin_package is not None:
                plugin_package = plugin_package.find(__XHTML+'a')
                if plugin_package is not None:
                    plugin_package = __parseAElement(plugin_package)
                    plugin_attrib['package'] = plugin_package['name']
                    
            # Attach attributes to current element
            plugin_element = ET.SubElement(current_element, 'plugin')
            plugin_element.attrib = plugin_attrib
    
    # Done parsing all elements
    return current_element


# -------------------------------
#       MAIN
# -------------------------------

#createBlamePage()

createMaintainerPage()

