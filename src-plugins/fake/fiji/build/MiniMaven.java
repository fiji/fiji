package fiji.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.DefaultHandler;

public class MiniMaven {
	protected boolean verbose;
	protected PrintStream err;
	protected Map<String, POM> localPOMCache = new HashMap<String, POM>();
	protected Fake fake;

	public MiniMaven(Fake fake, PrintStream err, boolean verbose) throws FakeException {
		this.fake = fake == null ? new Fake() : fake;
		this.err = err;
		this.verbose = verbose;
	}

	protected void print80(String string) {
		int length = string.length();
		err.print((length < 80 ? string : string.substring(0, 80)) + "\r");
	}

	public POM parse(File file) throws IOException, ParserConfigurationException, SAXException {
		File directory = file.getCanonicalFile().getParentFile();

		// look for root pom.xml
		File parentDirectory = directory.getParentFile();
		if (!parentDirectory.exists() || !new File(parentDirectory, "pom.xml").exists())
			return parse(file, null);

		Stack<String> stack = new Stack<String>();
		for (;;) {
			stack.push(directory.getName());
			directory = parentDirectory;
			parentDirectory = directory.getParentFile();
			if (!parentDirectory.exists() || !new File(parentDirectory, "pom.xml").exists())
				break;
		}
		POM pom = parse(new File(directory, "pom.xml"), null);
		// walk back up to the desired pom.xml
		while (!stack.empty()) {
			String name = stack.pop();
			POM next = null;
			for (POM child : pom.children)
				if (child.directory.getName().equals(name)) {
					next = child;
					break;
				}
			if (next == null)
				next = pom.addModule(name);
			pom = next;
		}
		return pom;
	}

	public POM parse(File file, POM parent) throws IOException, ParserConfigurationException, SAXException {
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

		if (pom.target == null) {
			String fileName = file.getName();
			if (fileName.endsWith(".pom"))
				fileName = fileName.substring(0, fileName.length() - 4);
			fileName += ".jar";
			pom.target = new File(directory, fileName);
		}

		return pom;
	}

	protected class POM extends DefaultHandler implements Comparable<POM> {
		protected final boolean debug = false;
		protected String profile = "swing";

		protected boolean buildFromSource;
		protected File directory, target;
		protected POM parent;
		protected POM[] children;

		protected String groupId, artifactId, version;
		protected Map<String, String> properties = new HashMap<String, String>();
		protected List<String> modules = new ArrayList<String>();
		protected List<String[]> dependencies = new ArrayList<String[]>(); // contains String[3]
		protected Set<String> repositories = new TreeSet<String>();

		// only used during parsing
		protected String prefix = "";
		protected String[] latestDependency = new String[3];
		protected boolean isCurrentProfile;

		protected POM addModule(String name) throws IOException, ParserConfigurationException, SAXException {
			return addChild(parse(new File(new File(directory, name), "pom.xml"), this));
		}

		protected POM addChild(POM child) {
			POM[] newChildren = new POM[children.length + 1];
			System.arraycopy(children, 0, newChildren, 0, children.length);
			newChildren[children.length] = child;
			children = newChildren;
			return child;
		}

		protected POM(File directory, POM parent) {
			this.directory = directory;
			this.parent = parent;
			if (parent != null) {
				groupId = parent.groupId;
				version = parent.version;
			}

			if (new File(directory, "src").exists()) {
				buildFromSource = true;
				target = new File(directory, "target/classes");
			}
		}

		public void clean() throws IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource)
				return;
			for (POM child : getDependencies())
				if (child != null)
					child.clean();
			if (target.isDirectory())
				rmRF(target);
			else if (target.exists())
				target.delete();
		}

		public void downloadDependencies() throws IOException, ParserConfigurationException, SAXException {
			for (POM dependency : getDependencies())
				if (dependency != null)
					dependency.download();

			download();
		}

		protected void download() throws FileNotFoundException {
			if (buildFromSource || target.exists())
				return;
			print80("Downloading " + target);
			for (String url : getRoot().getRepositories()) try {
				download(url);
				return;
			} catch (Exception e) { /* ignore */ }
			throw new FileNotFoundException("Could not download " + target);
		}

		protected void download(String url) throws MalformedURLException, IOException, NoSuchAlgorithmException {
			if (version == null) {
				err.println("Version of " + artifactId + " is null; Skipping.");
				return;
			}
			String path = "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
			String baseURL = url + path + artifactId + "-" + version;
			File directory = new File(System.getProperty("user.home") + "/.m2/repository" + path);
			downloadAndVerify(baseURL + ".pom", directory);
			downloadAndVerify(baseURL + ".jar", directory);
		}

		public boolean upToDate() throws IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource)
				return true;
			for (POM child : getDependencies())
				if (child != null && !child.upToDate())
					return false;

			File source = new File(directory, "src/main/java");

			List<String> notUpToDates = new ArrayList<String>();
			addRecursively(notUpToDates, source, ".java", target, ".class");
			int count = notUpToDates.size();
			return count == 0;
		}

		public void build() throws FakeException, IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource)
				return;
			for (POM child : getDependencies())
				if (child != null)
					child.build();

			target.mkdirs();
			File source = new File(directory, "src/main/java");

			List<String> arguments = new ArrayList<String>();
			// classpath
			arguments.add("-classpath");
			arguments.add(getClassPath());
			// output directory
			arguments.add("-d");
			arguments.add(target.getPath());
			// the files
			int count = arguments.size();
			addRecursively(arguments, source, ".java", target, ".class");
			count = arguments.size() - count;

			if (count > 0) {
				err.println("Compiling " + (arguments.size() - count) + " files in " + directory);
				String[] array = arguments.toArray(new String[arguments.size()]);
				if (fake != null)
					fake.callJavac(array, verbose);
			}

			updateRecursively(new File(source.getParentFile(), "resources"), target);

			buildFromSource = false;
		}

		protected void addRecursively(List<String> list, File directory, String extension, File targetDirectory, String targetExtension) {
			for (File file : directory.listFiles())
				if (file.isDirectory())
					addRecursively(list, file, extension, new File(targetDirectory, file.getName()), targetExtension);
				else {
					String name = file.getName();
					if (!name.endsWith(extension))
						continue;
					File targetFile = new File(targetDirectory, name.substring(0, name.length() - extension.length()) + targetExtension);
					if (!targetFile.exists() || targetFile.lastModified() < file.lastModified())
						list.add(file.getPath());
				}
		}

		protected void updateRecursively(File source, File target) throws IOException {
			File[] list = source.listFiles();
			if (list == null)
				return;
			for (File file : list) {
				File targetFile = new File(target, file.getName());
				if (file.isDirectory())
					updateRecursively(file, targetFile);
				else if (file.isFile()) {
					if (targetFile.exists() && targetFile.lastModified() >= file.lastModified())
						continue;
					targetFile.getParentFile().mkdirs();
					copyFile(file, targetFile);
				}
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

		public String getClassPath() throws IOException, ParserConfigurationException, SAXException {
			StringBuilder builder = new StringBuilder();
			builder.append(target);
			for (POM pom : getDependencies())
				builder.append(File.pathSeparator).append(pom.target);
			return builder.toString();
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

		protected Set<String> getRepositories() {
			Set<String> result = new TreeSet<String>();
			getRepositories(result);
			return result;
		}

		protected void getRepositories(Set<String> result) {
			// add a default to the root
			if (parent == null)
				result.add("http://repo1.maven.org/maven2/");
			result.addAll(repositories);
			for (POM child : children)
				if (child != null)
					child.getRepositories(result);
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

		protected POM findLocallyCachedPOM(String groupId, String artifactId, String version) throws IOException, ParserConfigurationException, SAXException {
			String key = groupId + ">" + artifactId;
			POM result = localPOMCache.get(key);
			if (result == null) {
				if (groupId == null)
					return null;
				String path = System.getProperty("user.home") + "/.m2/repository/" + groupId.replace('.', '/') + "/" + artifactId + "/";
				if (version == null)
					version = findLocallyCachedVersion(path);
				path += version + "/" + artifactId + "-" + version + ".pom";
				result = parse(new File(path), null);
				if (result == null)
					err.println("Artifact not found; consider 'get-dependencies': " + artifactId);
				localPOMCache.put(key, result);
			}
			if (result != null && version != null && compareVersion(version, result.version) > 0)
				throw new RuntimeException("Local artifact " + key + " has wrong version: " + result.version + " (< " + version + ")");
			return result;
		}

		protected String findLocallyCachedVersion(String path) throws IOException {
			File file = new File(path, "maven-metadata-local.xml");
			if (!file.exists()) {
				String[] list = new File(path).list();
				return list != null && list.length > 0 ? list[0] : null;
			}
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
				err.println("start(" + uri + ", " + name + ", " + qualifiedName + ", " + toString(attributes) + ")");
		}

		public void endElement(String uri, String name, String qualifiedName) {
			if (prefix.equals(">project>dependencies>dependency") || (isCurrentProfile && prefix.equals(">project>profiles>profile>dependencies>dependency"))) {
				dependencies.add(latestDependency);
				latestDependency = new String[3];
			}
			if (prefix.equals(">project>profiles>profile"))
				isCurrentProfile = false;
			prefix = prefix.substring(0, prefix.length() - 1 - qualifiedName.length());
			if (debug)
				err.println("end(" + uri + ", " + name + ", " + qualifiedName + ")");
		}

		public void characters(char[] buffer, int offset, int length) {
			String string = new String(buffer, offset, length);
			if (debug)
				err.println("characters: " + string + " (prefix: " + prefix + ")");

			String prefix = this.prefix;
			if (isCurrentProfile)
				prefix = ">project" + prefix.substring(">project>profiles>profile".length());

			if (prefix.equals(">project>groupId"))
				groupId = string;
			else if (prefix.equals(">project>parent>groupId")) {
				if (groupId == null)
					groupId = string;
			}
			else if (prefix.equals(">project>artifactId"))
				artifactId = string;
			else if (prefix.equals(">project>version"))
				version = string;
			else if (prefix.equals(">project>parent>version")) {
				if (version == null)
					version = string;
			}
			else if (prefix.equals(">project>modules>module"))
				modules.add(string);
			else if (prefix.startsWith(">project>properties>"))
				properties.put(prefix.substring(">project>properties>".length()), string);
			else if (prefix.equals(">project>dependencies>dependency>groupId"))
				latestDependency[0] = string;
			else if (prefix.equals(">project>dependencies>dependency>artifactId"))
				latestDependency[1] = string;
			else if (prefix.equals(">project>dependencies>dependency>version"))
				latestDependency[2] = string;
			else if (prefix.equals(">project>profiles>profile>id"))
				isCurrentProfile = profile.equals(string);
			else if (prefix.equals(">project>repositories>repository>url"))
				repositories.add(string);
			else if (debug)
				err.println("Ignoring " + prefix);
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

	protected static void downloadAndVerify(String url, File directory) throws IOException, NoSuchAlgorithmException {
		File sha1 = download(new URL(url + ".sha1"), directory);
		File file = download(new URL(url), directory);
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		FileInputStream fileStream = new FileInputStream(file);
		DigestInputStream digestStream = new DigestInputStream(fileStream, digest);
		byte[] buffer = new byte[131072];
		while (digestStream.read(buffer) >= 0)
			; /* do nothing */
		digestStream.close();

		byte[] digestBytes = digest.digest();
		fileStream = new FileInputStream(sha1);
		for (int i = 0; i < digestBytes.length; i++) {
			int value = (hexNybble(fileStream.read()) << 4) |
				hexNybble(fileStream.read());
			int d = digestBytes[i] & 0xff;
			if (value != d)
				throw new IOException("SHA1 mismatch: " + sha1 + ": " + Integer.toHexString(value) + " != " + Integer.toHexString(d));
		}
		fileStream.close();
	}

	protected static int hexNybble(int b) {
		return (b < 'A' ? (b < 'a' ? b - '0' : b - 'a' + 10) : b - 'A' + 10) & 0xf;
	}

	protected static void rmRF(File directory) {
		for (File file : directory.listFiles())
			if (file.isDirectory())
				rmRF(file);
			else
				file.delete();
		directory.delete();
	}

	protected static File download(URL url, File directory) throws IOException {
		directory.mkdirs();
		InputStream in = url.openStream();
		String fileName = url.getPath();
		fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
		File result = new File(directory, fileName);
		copy(in, result);
		return result;
	}

	protected static void copyFile(File source, File target) throws IOException {
		copy(new FileInputStream(source), target);
	}

	protected static void copy(InputStream in, File target) throws IOException {
		FileOutputStream out = new FileOutputStream(target);
		byte[] buffer = new byte[131072];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
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

	private final static String usage = "Usage: MiniMaven [command]\n"
		+ "\tSupported commands: compile, run, compile-and-run, clean, get-dependencies";

	public static void main(String[] args) throws Exception {
		MiniMaven miniMaven = new MiniMaven(null, System.err, false);
		POM root = miniMaven.parse(new File("pom.xml"), null);
		String command = args.length == 0 ? "compile-and-run" : args[0];
		String artifactId = getSystemProperty("artifactId", "imagej");
		String mainClass = getSystemProperty("mainClass", "imagej.Main");

		POM pom = root.findPOM(null, artifactId, null);
		if (pom == null)
			pom = root;
		if (command.equals("compile") || command.equals("build") || command.equals("compile-and-run")) {
			pom.build();
			if (command.equals("compile-and-run"))
				command = "run";
			else
				return;
		}
		if (command.equals("clean"))
			pom.clean();
		else if (command.equals("get") || command.equals("get-dependencies"))
			pom.downloadDependencies();
		else if (command.equals("run")) {
			String[] paths = pom.getClassPath().split(File.pathSeparator);
			URL[] urls = new URL[paths.length];
			for (int i = 0; i < urls.length; i++)
				urls[i] = new URL("file:" + paths[i] + (paths[i].endsWith(".jar") ? "" : "/"));
			URLClassLoader classLoader = new URLClassLoader(urls);
			// needed for sezpoz
			Thread.currentThread().setContextClassLoader(classLoader);
			Class clazz = classLoader.loadClass(mainClass);
			Method main = clazz.getMethod("main", new Class[] { String[].class });
			main.invoke(null, new Object[] { new String[0] });
		}
		else if (command.equals("classpath"))
			miniMaven.err.println(pom.getClassPath());
		else
			miniMaven.err.println("Unhandled command: " + command + "\n" + usage);
	}

	protected static String getSystemProperty(String key, String defaultValue) {
		String result = System.getProperty(key);
		return result == null ? defaultValue : result;
	}
}
