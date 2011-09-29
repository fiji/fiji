import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.DefaultHandler;

public class QuickBuild {
	protected static boolean verbose = false;

	protected static void print80(String string) {
		int length = string.length();
		System.err.print((length < 80 ? string : string.substring(0, 80)) + "\r");
	}

	protected static class POM extends DefaultHandler implements Comparable<POM> {
		protected final boolean debug = false;

		protected File directory;
		protected POM parent;
		protected POM[] children;

		protected String groupId, artifactId, version;
		protected Map<String, String> properties = new HashMap<String, String>();
		protected List<String> modules = new ArrayList<String>();
		protected List<String[]> dependencies = new ArrayList<String[]>(); // contains String[3]

		// only used during parsing
		protected String prefix = "";
		protected String[] latestDependency = new String[3];

		protected static Map<String, POM> localPOMCache = new HashMap<String, POM>();

		public static POM parse(File file, POM parent) throws IOException, ParserConfigurationException, SAXException {
			if (!file.exists())
				return null;
			if (verbose)
				print80("Parsing " + file);
			File directory = file.getCanonicalFile().getParentFile();
			POM pom = new POM(directory, parent);
			XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			reader.setContentHandler(pom);
			//reader.setXMLErrorHandler(...);
			reader.parse(new InputSource(new FileInputStream(file)));

			pom.children = new POM[pom.modules.size()];
			for (int i = 0; i < pom.children.length; i++) {
				file = new File(directory, pom.modules.get(i) + "/pom.xml");
				pom.children[i] = parse(file, pom);
			}

			return pom;
		}

		protected POM(File directory, POM parent) {
			this.directory = directory;
			this.parent = parent;
			if (parent != null) {
				groupId = parent.groupId;
				version = parent.version;
			}
		}

		public String getGroup() {
			return groupId;
		}

		public String getArtifact() {
			return artifactId;
		}

		public String getVersion() {
			return version;
		}

		public String getTarget() {
			return groupId.replace('.', '/') + '/' + artifactId + '-' + version + ".jar";
		}

		public Set<POM> getDependencies() throws IOException, ParserConfigurationException, SAXException {
			Set<POM> set = new TreeSet<POM>();
			getDependencies(set);
			return set;
		}

		public void getDependencies(Set<POM> result) throws IOException, ParserConfigurationException, SAXException {
			for (String[] dependency : dependencies) {
				String groupId = expand(dependency[0]);
				String artifactId = expand(dependency[1]);
				String version = expand(dependency[2]);
				POM pom = getRoot().findPOM(groupId, artifactId, version);
				if (pom == null || result.contains(pom))
					continue;
				result.add(pom);
				pom.getDependencies(result);
			}
		}

		// expands ${<property-name>}
		public String expand(String string) {
			if (string == null)
				return null;
			for (;;) {
				int dollarCurly = string.indexOf("${");
				if (dollarCurly < 0)
					return string;
				int endCurly = string.indexOf("}", dollarCurly + 2);
				if (endCurly < 0)
					throw new RuntimeException("Invalid string: " + string);
				String property = getProperty(string.substring(dollarCurly + 2, endCurly));
				if (property == null) {
					if (dollarCurly == 0 && endCurly == string.length() - 1)
						return null;
					property = "";
				}
				string = string.substring(0, dollarCurly)
					+ property
					+ string.substring(endCurly + 1);
			}
		}

		public String getProperty(String key) {
			if (properties.containsKey(key))
				return properties.get(key);
			if (parent == null) {
				if (key.equals("bio-formats.groupId"))
					return "loci";
				if (key.equals("imagej.groupId"))
					return "imagej";
				return null;
			}
			return parent.getProperty(key);
		}

		protected POM getRoot() {
			POM result = this;
			while (result.parent != null)
				result = result.parent;
			return result;
		}

		protected POM findPOM(String groupId, String artifactId, String version) throws IOException, ParserConfigurationException, SAXException {
			if (artifactId.equals(this.artifactId) &&
					(groupId == null || groupId.equals(this.groupId)) &&
					(version == null || this.version == null || version.equals(this.version)))
				return this;
			for (POM child : children) {
				if (child == null)
					continue;
				POM result = child.findPOM(groupId, artifactId, version);
				if (result != null)
					return result;
			}
			// for the root POM, fall back to $HOME/.m2/repository/
			if (parent == null)
				return findLocallyCachedPOM(groupId, artifactId, version);
			return null;
		}

		protected static POM findLocallyCachedPOM(String groupId, String artifactId, String version) throws IOException, ParserConfigurationException, SAXException {
			String key = groupId + ">" + artifactId;
			POM result = localPOMCache.get(key);
			if (result == null) {
				String path = System.getProperty("user.home") + "/.m2/repository/" + groupId.replace('.', '/') + "/" + artifactId + "/";
				if (version == null)
					version = findLocallyCachedVersion(path);
				path += version + "/" + artifactId + "-" + version + ".pom";
				// TODO: if path does not exist, fetch the files
				result = parse(new File(path), null);
				localPOMCache.put(key, result);
			}
			if (result != null && version != null && compareVersion(version, result.version) > 0)
				throw new RuntimeException("Local artifact " + key + " has wrong version: " + result.version + " (< " + version + ")");
			return result;
		}

		protected static String findLocallyCachedVersion(String path) throws IOException {
			File file = new File(path, "maven-metadata-local.xml");
			if (!file.exists())
				return new File(path).list()[0];
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for (;;) {
				String line = reader.readLine();
				if (line == null)
					throw new RuntimeException("Could not determine version for " + path);
				int tag = line.indexOf("<version>");
				if (tag < 0)
					continue;
				reader.close();
				int endTag = line.indexOf("</version>");
				return line.substring(tag + "<version>".length(), endTag);
			}
		}

		protected static int compareVersion(String version1, String version2) {
			if (version1.equals(version2))
				return 0;
			String[] split1 = version1.split("\\.");
			String[] split2 = version2.split("\\.");

			for (int i = 0; ; i++) {
				if (i == split1.length)
					return i == split2.length ? 0 : -1;
				if (i == split2.length)
					return +1;
				int end1 = firstNonDigit(split1[i]);
				int end2 = firstNonDigit(split2[i]);
				if (end1 != end2)
					return end1 - end2;
				int result = end1 == 0 ? 0 :
					Integer.parseInt(split1[i].substring(0, end1))
					- Integer.parseInt(split2[i].substring(0, end2));
				if (result != 0)
					return result;
				result = split1[i].substring(end1).compareTo(split2[i].substring(end2));
				if (result != 0)
					return result;
			}
		}

		protected static int firstNonDigit(String string) {
			int length = string.length();
			for (int i = 0; i < length; i++)
				if (!Character.isDigit(string.charAt(i)))
					return i;
			return length;
		}

		// XML parsing

		public void startDocument() {}

		public void endDocument() {
			if (!properties.containsKey("project.groupId"))
				properties.put("project.groupId", groupId);
			if (!properties.containsKey("project.version"))
				properties.put("project.version", version);
		}

		public void startElement(String uri, String name, String qualifiedName, Attributes attributes) {
			prefix += ">" + qualifiedName;
			if (debug)
				System.err.println("start(" + uri + ", " + name + ", " + qualifiedName + ", " + toString(attributes) + ")");
		}

		public void endElement(String uri, String name, String qualifiedName) {
			if (prefix.equals(">project>dependencies>dependency")) {
				dependencies.add(latestDependency);
				latestDependency = new String[3];
			}
			prefix = prefix.substring(0, prefix.length() - 1 - qualifiedName.length());
			if (debug)
				System.err.println("end(" + uri + ", " + name + ", " + qualifiedName + ")");
		}

		public void characters(char[] buffer, int offset, int length) {
			if (debug)
				System.err.println("characters: " + new String(buffer, offset, length) + " (prefix: " + prefix + ")");
			if (prefix.equals(">project>groupId"))
				groupId = new String(buffer, offset, length);
			else if (prefix.equals(">project>parent>groupId")) {
				if (groupId == null)
					groupId = new String(buffer, offset, length);
			}
			else if (prefix.equals(">project>artifactId"))
				artifactId = new String(buffer, offset, length);
			else if (prefix.equals(">project>version"))
				version = new String(buffer, offset, length);
			else if (prefix.equals(">project>parent>version")) {
				if (version == null)
					version = new String(buffer, offset, length);
			}
			else if (prefix.equals(">project>modules>module"))
				modules.add(new String(buffer, offset, length));
			else if (prefix.startsWith(">project>properties>"))
				properties.put(prefix.substring(">project>properties>".length()), new String(buffer, offset, length));
			else if (prefix.equals(">project>dependencies>dependency>groupId"))
				latestDependency[0] = new String(buffer, offset, length);
			else if (prefix.equals(">project>dependencies>dependency>artifactId"))
				latestDependency[1] = new String(buffer, offset, length);
			else if (prefix.equals(">project>dependencies>dependency>version"))
				latestDependency[2] = new String(buffer, offset, length);
			else if (debug)
				System.err.println("Ignoring " + prefix);
		}

		public String toString(Attributes attributes) {
			StringBuilder builder = new StringBuilder();
			builder.append("[ ");
			for (int i = 0; i < attributes.getLength(); i++)
				builder.append(attributes.getQName(i))
					. append("='").append(attributes.getValue(i))
					. append("' ");
			builder.append("]");
			return builder.toString();
		}

		public int compareTo(POM other) {
			int result = artifactId.compareTo(other.artifactId);
			if (result != 0)
				return result;
			if (groupId != null && other.groupId != null)
				result = groupId.compareTo(other.groupId);
			if (result != 0)
				return result;
			if (version != null && other.version != null)
				return compareVersion(version, other.version);
			return 0;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			append(builder, "");
			return builder.toString();
		}

		public void append(StringBuilder builder, String indent) {
			builder.append(indent + groupId + ">" + artifactId + "\n");
			if (children != null)
				for (POM child : children)
					child.append(builder, indent + "  ");
		}
	}

	public static void main(String[] args) throws Exception {
		POM root = POM.parse(new File("pom.xml"), null);
		for (POM pom : root.findPOM(null, "imagej", null).getDependencies())
			System.err.println(pom.artifactId);
	}
}
