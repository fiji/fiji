package edu.utexas.clm.archipelago.ui;


import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.NodeShellParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClusterXML
{
    public static boolean saveToFile(final Cluster cluster, final File file)
    {
        try
        {
            FijiArchipelago.debug("Save called");
            final DocumentBuilder docBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final NodeManager nm = cluster.getNodeManager();
            final ArrayList<NodeManager.NodeParameters> params = cluster.getNodeParameters();
            final Document doc = docBuilder.newDocument();
            final Element clusterXML = doc.createElement("Cluster");
            final Element rootNode = doc.createElement("RootNode");
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            final StreamResult result = new StreamResult(file);
            DOMSource source;

            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            doc.appendChild(clusterXML);

            addXMLField(doc, rootNode, "exec", FijiArchipelago.getExecRoot());
            addXMLField(doc, rootNode, "file", FijiArchipelago.getFileRoot());
            addXMLField(doc, rootNode, "default-exec", nm.getDefaultParameters().getExecRoot());
            addXMLField(doc, rootNode, "default-file", nm.getDefaultParameters().getFileRoot());
            addXMLField(doc, rootNode, "default-user", nm.getDefaultParameters().getUser());

            clusterXML.appendChild(rootNode);

            for (NodeManager.NodeParameters np : params)
            {
                final Element clusterNode = doc.createElement("ClusterNode");
                final Element shellParams = doc.createElement("ShellParameters");

                addXMLField(doc, clusterNode, "host", np.getHost());
                addXMLField(doc, clusterNode, "user", np.getUser());
                addXMLField(doc, clusterNode, "exec", np.getExecRoot());
                addXMLField(doc, clusterNode, "file", np.getFileRoot());
                addXMLField(doc, clusterNode, "limit", "" + np.getThreadLimit());
                addXMLField(doc, clusterNode, "shell", np.getShell().name());

                try
                {
                    np.getShellParams().toXML(shellParams);
                    clusterNode.appendChild(shellParams);
                    clusterXML.appendChild(clusterNode);
                }
                catch (Exception e)
                {/**/}
            }

            source = new DOMSource(doc);
            transformer.transform(source, result);

            FijiArchipelago.log("Saved configuration to " + file);

            return true;
        }
        catch (Exception e)
        {
            FijiArchipelago.err("Error saving file: " + e);
            return false;
        }
    }

    public static String replaceProperties(final String instring)
    {
        String string = instring;
        String key = "";
        int b, e;
        try
        {
        while ((b = string.indexOf("[")) >= 0)
        {
            e = string.indexOf("]");
            key = string.substring(b + 1, e);
            string = string.replace("[" + key + "]", System.getProperty(key));
        }
        }
        catch (StringIndexOutOfBoundsException ioobe)
        {
            FijiArchipelago.debug("Warning: Malformed input string " + instring);
        }
        catch (NullPointerException npe)
        {
            FijiArchipelago.debug("No such system property: " + key + ". Input string: " + instring);
        }

        FijiArchipelago.debug("Translated " + instring + " -> " + string);

        return string;
    }

    private static String getXMLField(final Element e, final String tag)
    {
        final Node n = e.getElementsByTagName(tag).item(0);
        return n == null ? null : replaceProperties(n.getTextContent());
    }

    private static void addXMLField(Document doc, Element parent, String field, String value)
    {
        Element e = doc.createElement(field);
        e.appendChild(doc.createTextNode(value));
        parent.appendChild(e);
    }

    /**
     * Configures a Cluster as determined by the given XML
     * file. This method does not start the cluster or the nodes, this must be done manually by
     * calling cluster.start().
     * @param file an XML file containing a Cluster and ClusterNode configuration, as written
     *              by saveClusterFile
     * @param cluster a Cluster to configure
     * @param nodeExceptions an Exception List, which will be populated with Exceptions for any
     *                       NodeParameters that cannot be formed correctly, may be null;
     * @return true if the Cluster was configured correctly
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public static boolean loadClusterFile(final File file, final Cluster cluster,
                                          List<Exception> nodeExceptions)
            throws ParserConfigurationException, SAXException, IOException
    {
        final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(file);
        Element rootNode;
        NodeList clusterNodes;
        String execRoot, fileRoot, dExecRoot, dFileRoot, user, autoStart;
        boolean ok;

        if (nodeExceptions == null)
        {
            nodeExceptions = new ArrayList<Exception>();
        }

        doc.getDocumentElement().normalize();

        // Should only have one node in this list             
        rootNode = (Element)doc.getElementsByTagName("RootNode").item(0);

        execRoot = getXMLField(rootNode, "exec");
        fileRoot = getXMLField(rootNode, "file");
        dExecRoot = getXMLField(rootNode, "default-exec");
        dFileRoot = getXMLField(rootNode, "default-file");
        user = getXMLField(rootNode, "default-user");
        autoStart = getXMLField(rootNode, "auto-start");

        ok = Cluster.configureCluster(cluster, dExecRoot, dFileRoot, execRoot, fileRoot, user);

        clusterNodes = doc.getElementsByTagName("ClusterNode");

        for (int i = 0; i < clusterNodes.getLength(); ++i)
        {
            try
            {
                cluster.addNodeToStart(xmlToNodeParameter(cluster, (Element) clusterNodes.item(i)));
            }
            catch (Exception e)
            {
                FijiArchipelago.debug("loadClusterFile: could not add node: " + e);
                e.printStackTrace();
                nodeExceptions.add(e);
            }
        }

        if ("yes".equals(autoStart))
        {
            cluster.start();
        }

        return ok;
    }

    private static NodeManager.NodeParameters xmlToNodeParameter(
            final Cluster cluster, final Element node) throws Exception
    {
        final NodeManager.NodeParameters nodeParam = cluster.getNodeManager().newParam();
        NodeShellParameters shellParams;

        nodeParam.setHost(getXMLField(node, "host"));
        nodeParam.setUser(getXMLField(node, "user"));
        nodeParam.setExecRoot(getXMLField(node, "exec"));
        nodeParam.setFileRoot(getXMLField(node, "file"));
        nodeParam.setThreadLimit(Integer.parseInt(getXMLField(node, "limit")));
        nodeParam.setShell(getXMLField(node, "shell"));

        if (nodeParam.getShell() == null)
        {
            throw new Exception("Could not load shell " + getXMLField(node, "shell"));
        }

        shellParams = nodeParam.getShell().defaultParameters();
        shellParams.fromXML((Element)node.getElementsByTagName("ShellParameters").item(0));
        nodeParam.setShellParams(shellParams);
        return nodeParam;
    }
}
