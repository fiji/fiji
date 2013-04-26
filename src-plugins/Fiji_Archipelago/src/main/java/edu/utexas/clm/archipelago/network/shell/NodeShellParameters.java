package edu.utexas.clm.archipelago.network.shell;

import edu.utexas.clm.archipelago.FijiArchipelago;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * A class to support parameters for various kinds of node shells.
 * 
 */
public class NodeShellParameters
{
    
    private final HashMap<String, String> parameterMap;
    private final ArrayList<String> fileKeys, stringKeys, floatKeys, intKeys;


    public NodeShellParameters()
    {
        parameterMap = new HashMap<String, String>();
        fileKeys = new ArrayList<String>();
        intKeys = new ArrayList<String>();
        floatKeys = new ArrayList<String>();
        stringKeys = new ArrayList<String>();
    }

    protected void addKey(final String key, final File defaultValue)
    {
        parameterMap.put(key, defaultValue.getAbsolutePath());
        fileKeys.add(key);
    }

    protected void addKey(final String key, final String defaultValue)
    {
        parameterMap.put(key, defaultValue);
        stringKeys.add(key);
    }

    protected void addKey(final String key, final int defaultValue)
    {
        parameterMap.put(key, "" + defaultValue);
        intKeys.add(key);
    }

    protected void addKey(final String key, final float defaultValue)
    {
        parameterMap.put(key, "" + defaultValue);
        floatKeys.add(key);
    }
    
    private void checkKey(final String key) throws Exception
    {
        if (!parameterMap.containsKey(key))
        {
            throw new Exception("No such key: " + key);
        }
    }
    
    public int getInteger(final String key) throws Exception
    {
        checkKey(key);
        final String val = getString(key);
        return Integer.parseInt(val);
    }
    
    public float getFloat(final String key) throws Exception
    {
        checkKey(key);
        final String val = getString(key);
        return Float.parseFloat(val);
    }
    
    public String getStringOrEmpty(final String key)
    {
        final String val = parameterMap.get(key);
        return val == null ? "" : val;
    }
    
    public String getString(final String key) throws Exception
    {
        checkKey(key);
        final String val = parameterMap.get(key);
        if (val == null)
        {
            throw new Exception("Null value associated with key: " + key);
        }
        return val;
    }
    
    public Set<String> getKeys()
    {
        return parameterMap.keySet();
    }
    
    public boolean isFile(final String key)
    {
        return fileKeys.contains(key);
    }
    
    public boolean isInt(final String key)
    {
        return intKeys.contains(key);
    }
    
    public boolean isFloat(final String key)
    {
        return floatKeys.contains(key);
    }
    
    public boolean isString(final String key)
    {
        return stringKeys.contains(key);
    }
    
    public void putValue(final String key, final String value) throws Exception
    {
        if (!parameterMap.containsKey(key))
        {
            throw new Exception("No such key: " + key);
        }
        parameterMap.put(key, value);
    }

    public void fromXML(final Element e) throws Exception
    {
        final NodeList children = e.getChildNodes();
        final ArrayList<String> keyList = new ArrayList<String>(parameterMap.keySet());
        
        for (int i = 0; i < children.getLength() && !keyList.isEmpty(); ++i)
        {
            //e.getElementsByTagName(tag).item(0).getTextContent();
            //final Element subE = (Element)children.item(i);
            final String name = children.item(i).getNodeName();            
            if (keyList.contains(name))
            {
                final String value = children.item(i).getTextContent();
                keyList.remove(name);
                FijiArchipelago.debug("fromXML: " + name + " <- " + value);
                parameterMap.put(name, value);
            }
        }
        
        if (!keyList.isEmpty())
        {
            throw new Exception("XML did not contain all parameter keys");
        }
        
    }

    public void toXML(final Element e) throws Exception
    {        
        for (final String key : parameterMap.keySet())
        {
            Element subE = e.getOwnerDocument().createElement(key);
            subE.appendChild(e.getOwnerDocument().createTextNode(getString(key)));
            e.appendChild(subE);
        }
    }
}
