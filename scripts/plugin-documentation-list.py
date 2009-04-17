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
WIKI_URL = 'http://pacific.mpi-cbg.de'
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
                        return None
                    else:
                        return td.text
                
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
        str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            plugin_without_documentation_page.append(str)
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)
            if mtnr is None:
                plugin_without_maintainer.append(str)
                
    packagetree = getPackageTree()
    package_els = packagetree.findall('.//package')
    for el in package_els:
        str =  '* [[' + el.attrib['name'] + ']]'
        if el.attrib['has_page'] == 'no':
            package_without_documentation_page.append(str)
        else:
            html = getPluginHTMLpage(el)
            mtnr = findMaintainer(html)
            if mtnr is None:
                package_without_maintainer.append(str)
    
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

createBlamePage()


