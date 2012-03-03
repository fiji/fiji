package fiji.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import java.util.jar.JarOutputStream;

import java.util.zip.ZipEntry;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.DefaultHandler;

public class MiniMaven {
	protected String endLine = System.console() == null ? "\n" : "\033[K\r";
	protected boolean verbose, debug = false, downloadAutomatically, offlineMode, ignoreMavenRepositories;
	protected int updateInterval = 24 * 60; // by default, check once per 24h for new snapshot versions
	protected PrintStream err;
	protected Map<String, POM> localPOMCache = new HashMap<String, POM>();
	protected Fake fake;

	public MiniMaven(Fake fake, PrintStream err, boolean verbose) throws FakeException {
		this(fake, err, verbose, false);
	}

	public MiniMaven(Fake fake, PrintStream err, boolean verbose, boolean debug) throws FakeException {
		this.fake = fake == null ? new Fake() : fake;
		this.err = err;
		this.verbose = verbose;
		this.debug = debug;
		if ("true".equalsIgnoreCase(System.getProperty("minimaven.offline")))
			offlineMode = true;
		if ("ignore".equalsIgnoreCase(System.getProperty("minimaven.repositories")))
			ignoreMavenRepositories = true;
		String updateInterval = System.getProperty("minimaven.updateinterval");
		if (updateInterval != null && !updateInterval.equals("")) try {
			this.updateInterval = Integer.parseInt(updateInterval);
			if (verbose)
				err.println("Setting update interval to " + this.updateInterval + " minutes");
		} catch (NumberFormatException e) {
			err.println("Warning: ignoring invalid update interval " + updateInterval);
		}
	}

	protected void print80(String string) {
		int length = string.length();
		err.print((verbose || length < 80 ? string : string.substring(0, 80)) + endLine);
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
		return parse(file, parent, null);
	}

	public POM parse(File file, POM parent, String classifier) throws IOException, ParserConfigurationException, SAXException {
		if (!file.exists())
			return null;
		if (verbose)
			print80("Parsing " + file);
		File directory = file.getCanonicalFile().getParentFile();
		POM pom = new POM(directory, parent);
		pom.coordinate.classifier = classifier;
		if (parent != null)
			pom.sourceDirectory = parent.sourceDirectory;
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		reader.setContentHandler(pom);
		//reader.setXMLErrorHandler(...);
		reader.parse(new InputSource(new FileInputStream(file)));

		File sourceDirectory = pom.getSourceDirectory();
		if (sourceDirectory.exists()) {
			pom.buildFromSource = true;
			pom.target = new File(directory, "target/classes");
		}

		pom.children = new POM[pom.modules.size()];
		for (int i = 0; i < pom.children.length; i++) {
			file = new File(directory, pom.modules.get(i) + "/pom.xml");
			pom.children[i] = parse(file, pom);
		}

		if (pom.target == null) {
			String fileName = pom.coordinate.getJarName();
			pom.target = new File(directory, fileName);
		}

		return pom;
	}

	protected POM fakePOM(File target, Coordinate dependency) {
		POM pom = new POM(target, null);
		pom.directory = target.getParentFile();
		pom.target = target;
		pom.children = new POM[0];
		pom.coordinate = dependency;
		if (dependency.artifactId.equals("ij")) {
			String javac = pom.expand("${java.home}/../lib/tools.jar");
			if (new File(javac).exists())
				pom.dependencies.add(new Coordinate("com.sun", "tools", "1.4.2", null, false, javac, null));
		}
		else if (dependency.artifactId.equals("imglib2-io"))
			pom.dependencies.add(new Coordinate("loci", "bio-formats", "${bio-formats.version}"));
		else if (dependency.artifactId.equals("jfreechart"))
			pom.dependencies.add(new Coordinate("jfree", "jcommon", "1.0.16"));
		return pom;
	}

	protected static class Coordinate {
		protected String groupId, artifactId, version, systemPath, classifier, scope;
		protected boolean optional;

		public Coordinate() {}

		public Coordinate(String groupId, String artifactId, String version) {
			this(groupId, artifactId, version, null, false, null, null);
		}

		public Coordinate(String groupId, String artifactId, String version, String scope, boolean optional, String systemPath, String classifier) {
			this.groupId = normalize(groupId);
			this.artifactId = normalize(artifactId);
			this.version = normalize(version);
			this.scope = normalize(scope);
			this.optional = optional;
			this.systemPath = normalize(systemPath);
			this.classifier = classifier;
		}

		public String normalize(String s) {
			return "".equals(s) ? null : s;
		}

		public String getJarName() {
			return getJarName(false);
		}

		public String getJarName(boolean withProjectPrefix) {
			return getFileName(withProjectPrefix, true, "jar");
		}

		public String getPOMName() {
			return getPOMName(false);
		}

		public String getPOMName(boolean withProjectPrefix) {
			return getFileName(withProjectPrefix, false, "pom");
		}

		public String getFileName(boolean withClassifier, String fileExtension) {
			return getFileName(false, withClassifier, fileExtension);
		}

		public String getFileName(boolean withProjectPrefix, boolean withClassifier, String fileExtension) {
			return (withProjectPrefix ? groupId + "/" : "")
				+ artifactId + "-" + version
				+ (withClassifier && classifier != null ? "-" + classifier : "")
				+ (fileExtension != null ? "." + fileExtension : "");
		}

		@Override
		public String toString() {
			String extra = "";
			if (optional)
				extra += " optional";
			if (scope != null)
				extra += " scope=" + scope;
			if (extra.startsWith(" "))
				extra = "{" + extra.substring(1) + "}";
			return getFileName(true, true, null) + extra;
		}
	}

	public class POM extends DefaultHandler implements Comparable<POM> {
		protected boolean buildFromSource, built;
		protected File directory, target;
		protected String sourceDirectory = "src/main/java";
		protected POM parent;
		protected POM[] children;

		protected Coordinate coordinate = new Coordinate();
		protected Map<String, String> properties = new HashMap<String, String>();
		protected List<String> modules = new ArrayList<String>();
		protected List<Coordinate> dependencies = new ArrayList<Coordinate>(); // contains String[3]
		protected Set<String> repositories = new TreeSet<String>();

		// only used during parsing
		protected String prefix = "";
		protected Coordinate latestDependency = new Coordinate();
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
				coordinate.groupId = parent.coordinate.groupId;
				coordinate.version = parent.coordinate.version;
			}
		}

		public void clean() throws IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource)
				return;
			for (POM child : getDependencies(true))
				if (child != null)
					child.clean();
			if (target.isDirectory())
				rmRF(target);
			else if (target.exists())
				target.delete();
			File jar = getTarget();
			if (jar.exists())
				jar.delete();
		}

		public void downloadDependencies() throws IOException, ParserConfigurationException, SAXException {
			downloadAutomatically = true;
			getDependencies(true, "test");
			download();
		}

		protected void download() throws FileNotFoundException {
			if (buildFromSource || target.exists())
				return;
			download(coordinate, true);
		}

		protected void download(Coordinate dependency, boolean quiet) throws FileNotFoundException {
			if (dependency.version == null) {
				err.println("Version of " + dependency.artifactId + " is null; Skipping.");
				return;
			}
			for (String url : getRoot().getRepositories()) try {
				downloadAndVerify(url, dependency, quiet);
				return;
			} catch (Exception e) { /* ignore */ }
			throw new FileNotFoundException("Could not download " + dependency.getJarName());
		}

		public boolean upToDate(boolean includingJar) throws IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource)
				return true;
			for (POM child : getDependencies(true, "test"))
				if (child != null && !child.upToDate(includingJar))
					return false;

			File source = getSourceDirectory();

			List<String> notUpToDates = new ArrayList<String>();
			long lastModified = addRecursively(notUpToDates, source, ".java", target, ".class");
			int count = notUpToDates.size();
			if (count != 0)
				return false;
			long lastModified2 = updateRecursively(new File(source.getParentFile(), "resources"), target, true);
			if (lastModified < lastModified2)
				lastModified = lastModified2;
			if (includingJar) {
				File jar = getTarget();
				if (!jar.exists() || jar.lastModified() < lastModified)
					return false;
			}
			return true;
		}

		public File getSourceDirectory() {
			String sourcePath = getSourcePath();
			File file = new File(sourcePath);
			if (file.isAbsolute())
				return file;
			return new File(directory, sourcePath);
		}

		public String getSourcePath() {
			return expand(sourceDirectory);
		}

		public void buildJar() throws FakeException, IOException, ParserConfigurationException, SAXException {
			build(true);
		}

		protected void addToJarRecursively(JarOutputStream out, File directory, String prefix) throws IOException {
			for (File file : directory.listFiles())
				if (file.isFile()) {
					out.putNextEntry(new ZipEntry(prefix + file.getName()));
					copy(new FileInputStream(file), out, false);
				}
				else if (file.isDirectory())
					addToJarRecursively(out, file, prefix + file.getName() + "/");
		}

		public void build() throws FakeException, IOException, ParserConfigurationException, SAXException {
			build(false);
		}

		public void build(boolean makeJar) throws FakeException, IOException, ParserConfigurationException, SAXException {
			if (!buildFromSource || built)
				return;
			for (POM child : getDependencies(true, "test"))
				if (child != null)
					child.build(makeJar);

			// do not build aggregator projects
			if (!new File(directory, "src").exists())
				return;

			target.mkdirs();
			File source = getSourceDirectory();

			List<String> arguments = new ArrayList<String>();
			// classpath
			String classPath = getClassPath(true);
			arguments.add("-classpath");
			arguments.add(classPath);
			// output directory
			arguments.add("-d");
			arguments.add(target.getPath());
			// the files
			int count = arguments.size();
			addRecursively(arguments, source, ".java", target, ".class");
			count = arguments.size() - count;

			if (count > 0) {
				err.println("Compiling " + count + " files in " + directory);
				if (verbose)
					err.println("using the class path: " + classPath);
				String[] array = arguments.toArray(new String[arguments.size()]);
				if (fake != null)
					fake.callJavac(array, verbose);
			}

			updateRecursively(new File(source.getParentFile(), "resources"), target, false);

			File pom = new File(directory, "pom.xml");
			if (pom.exists()) {
				File targetFile = new File(target, "META-INF/maven/" + coordinate.groupId + "/" + coordinate.artifactId + "/pom.xml");
				targetFile.getParentFile().mkdirs();
				copyFile(pom, targetFile);
			}

			if (makeJar) {
				JarOutputStream out = new JarOutputStream(new FileOutputStream(getTarget()));
				addToJarRecursively(out, target, "");
				out.close();
			}

			built = true;
		}

		protected long addRecursively(List<String> list, File directory, String extension, File targetDirectory, String targetExtension) {
			long lastModified = 0;
			if (list == null)
				return lastModified;
			File[] files = directory.listFiles();
			if (files == null)
				return lastModified;
			for (File file : files)
				if (file.isDirectory()) {
					long lastModified2 = addRecursively(list, file, extension, new File(targetDirectory, file.getName()), targetExtension);
					if (lastModified < lastModified2)
						lastModified = lastModified2;
				}
				else {
					String name = file.getName();
					if (!name.endsWith(extension))
						continue;
					File targetFile = new File(targetDirectory, name.substring(0, name.length() - extension.length()) + targetExtension);
					long lastModified2 = file.lastModified();
					if (lastModified < lastModified2)
						lastModified = lastModified2;
					if (!targetFile.exists() || targetFile.lastModified() < lastModified2)
						list.add(file.getPath());
				}
			return lastModified;
		}

		protected long updateRecursively(File source, File target, boolean dryRun) throws IOException {
			long lastModified = 0;
			File[] list = source.listFiles();
			if (list == null)
				return lastModified;
			for (File file : list) {
				File targetFile = new File(target, file.getName());
				if (file.isDirectory()) {
					long lastModified2 = updateRecursively(file, targetFile, dryRun);
					if (lastModified < lastModified2)
						lastModified = lastModified2;
				}
				else if (file.isFile()) {
					long lastModified2 = file.lastModified();
					if (lastModified < lastModified2)
						lastModified = lastModified2;
					if (dryRun || (targetFile.exists() && targetFile.lastModified() >= lastModified2))
						continue;
					targetFile.getParentFile().mkdirs();
					copyFile(file, targetFile);
				}
			}
			return lastModified;
		}

		public String getGroup() {
			return coordinate.groupId;
		}

		public String getArtifact() {
			return coordinate.artifactId;
		}

		public String getVersion() {
			return coordinate.version;
		}

		public String getJarName() {
			return coordinate.artifactId + '-' + coordinate.version + ".jar";
		}

		public File getTarget() {
			if (!buildFromSource)
				return target;
			return new File(new File(directory, "target"), getJarName());
		}

		public String getClassPath(boolean forCompile) throws IOException, ParserConfigurationException, SAXException {
			StringBuilder builder = new StringBuilder();
			builder.append(target);
			if (debug)
				err.println("Get classpath for " + coordinate + " for " + (forCompile ? "compile" : "runtime"));
			for (POM pom : getDependencies(true, "test", forCompile ? "runtime" : "provided")) {
				if (debug)
					err.println("Adding dependency " + pom.coordinate + " to classpath");
				builder.append(File.pathSeparator).append(pom.target);
			}
			return builder.toString();
		}

		/**
		 * Copy the runtime dependencies
		 *
		 * @param directory where to copy the files to
		 * @param onlyNewer whether to copy the files only if the sources are newer
		 * @throws IOException
		 * @throws ParserConfigurationException
		 * @throws SAXException
		 */
		public void copyDependencies(File directory, boolean onlyNewer) throws IOException, ParserConfigurationException, SAXException {
			for (POM pom : getDependencies(true, "test", "provided")) {
				File file = pom.getTarget();
				File destination = new File(directory, pom.coordinate.artifactId + ".jar");
				if (file.exists() && (!destination.exists() || destination.lastModified() < file.lastModified()))
					copyFile(file, destination);
			}
		}

		public Set<POM> getDependencies() throws IOException, ParserConfigurationException, SAXException {
			return getDependencies(false);
		}

		public Set<POM> getDependencies(boolean excludeOptionals, String... excludeScopes) throws IOException, ParserConfigurationException, SAXException {
			Set<POM> set = new TreeSet<POM>();
			getDependencies(set, excludeOptionals, excludeScopes);
			return set;
		}

		public void getDependencies(Set<POM> result, boolean excludeOptionals, String... excludeScopes) throws IOException, ParserConfigurationException, SAXException {
			for (Coordinate dependency : dependencies) {
				boolean optional = dependency.optional;
				if (excludeOptionals && optional)
					continue;
				String scope = expand(dependency.scope);
				if (scope != null && excludeScopes != null && arrayContainsString(excludeScopes, scope))
					continue;
				String groupId = expand(dependency.groupId);
				String artifactId = expand(dependency.artifactId);
				String version = expand(dependency.version);
				String classifier = expand(dependency.classifier);
				if (version == null && "aopalliance".equals(artifactId))
					optional = true; // guice has recorded this without a version
				String systemPath = expand(dependency.systemPath);
				Coordinate expanded = new Coordinate(groupId, artifactId, version, scope, optional, systemPath, classifier);
				if (systemPath != null) {
					File file = new File(systemPath);
					if (file.exists()) {
						result.add(fakePOM(file, expanded));
						continue;
					}
				}
				POM pom = getRoot().findPOM(expanded);
				if (pom == null || result.contains(pom))
					continue;
				result.add(pom);
				pom.getDependencies(result, excludeOptionals, excludeScopes);
			}
		}

		protected boolean arrayContainsString(String[] array, String key) {
			for (String string : array)
				if (string.equals(key))
					return true;
			return false;
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
			if (key.equals("project.basedir"))
				return directory.getPath();
			if (parent == null) {
				// hard-code a few variables
				if (key.equals("bio-formats.groupId"))
					return "loci";
				if (key.equals("bio-formats.version"))
					return "4.4-SNAPSHOT";
				if (key.equals("imagej.groupId"))
					return "imagej";
				if (key.equals("java.home"))
					return System.getProperty("java.home");
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

		protected POM findPOM(Coordinate dependency) throws IOException, ParserConfigurationException, SAXException {
			return findPOM(dependency, false);
		}

		protected POM findPOM(Coordinate dependency, boolean quiet) throws IOException, ParserConfigurationException, SAXException {
			if (dependency.artifactId.equals(coordinate.artifactId) &&
					(dependency.groupId == null || dependency.groupId.equals(coordinate.groupId)) &&
					(dependency.version == null || coordinate.version == null || dependency.version.equals(coordinate.version)))
				return this;
			if (dependency.groupId == null && dependency.artifactId.equals("jdom"))
				dependency.groupId = "jdom";
			for (POM child : children) {
				if (child == null)
					continue;
				POM result = child.findPOM(dependency, quiet);
				if (result != null)
					return result;
			}
			// for the root POM, fall back to $HOME/.m2/repository/ and Fiji's jars/ and plugins/ directories
			if (parent == null)
				return findLocallyCachedPOM(dependency, quiet);
			return null;
		}

		protected POM findLocallyCachedPOM(Coordinate dependency, boolean quiet) throws IOException, ParserConfigurationException, SAXException {
			if (dependency.groupId == null)
				return null;
			String key = dependency.groupId + ">" + dependency.artifactId;
			if (localPOMCache.containsKey(key)) {
				POM result = localPOMCache.get(key); // may be null
				if (result == null || dependency.version == null || compareVersion(dependency.version, result.coordinate.version) <= 0)
					return result;
			}

			if (ignoreMavenRepositories) {
				File file = findInFijiDirectories(dependency);
				if (file != null)
					return cacheAndReturn(key, fakePOM(file, dependency));
				if (!quiet && !dependency.optional)
					err.println("Skipping artifact " + dependency.artifactId + " (for " + coordinate.artifactId + "): not in jars/ nor plugins/");
				return cacheAndReturn(key, null);
			}

			String path = System.getProperty("user.home") + "/.m2/repository/" + dependency.groupId.replace('.', '/') + "/" + dependency.artifactId + "/";
			if (dependency.version == null)
				dependency.version = findLocallyCachedVersion(path);
			if (dependency.version == null && dependency.artifactId.equals("scifio"))
				dependency.version = "4.4-SNAPSHOT";
			if (dependency.version == null) {
				// try to find the .jar in Fiji's jars/ dir
				String jarName = dependency.artifactId + ".jar";
				File file = new File(System.getProperty("ij.dir"), "jars/" + jarName);
				if (file.exists())
					return cacheAndReturn(key, fakePOM(file, dependency));
				if (!quiet)
					err.println("Cannot find version for artifact " + dependency.artifactId + " (dependency of " + coordinate.artifactId + ")");
				return cacheAndReturn(key, null);
			}
			else if (dependency.version.startsWith("[")) try {
				if (!maybeDownloadAutomatically(dependency, quiet))
					return null;
				if (dependency.version.startsWith("["))
					dependency.version = parseVersion(new File(path, "maven-metadata-version.xml"));
			} catch (FileNotFoundException e) { /* ignore */ }
			path += dependency.version + "/";
			if (dependency.version.endsWith("-SNAPSHOT")) try {
				if (!maybeDownloadAutomatically(dependency, quiet))
					return null;
				if (dependency.version.endsWith("-SNAPSHOT"))
					dependency.version = parseSnapshotVersion(new File(path, "maven-metadata-snapshot.xml"));
			} catch (FileNotFoundException e) { /* ignore */ }
			else {
				File file = findInFijiDirectories(dependency);
				if (file != null)
					return cacheAndReturn(key, fakePOM(file, dependency));
			}

			path += dependency.getPOMName();

			File file = new File(path);
			if (!file.exists()) {
				if (downloadAutomatically) {
					if (!maybeDownloadAutomatically(dependency, quiet))
						return null;
				}
				else {
					if (!quiet && !dependency.optional)
						err.println("Skipping artifact " + dependency.artifactId + " (for " + coordinate.artifactId + "): not found");
					return cacheAndReturn(key, null);
				}
			}

			POM result = parse(new File(path), null, dependency.classifier);
			if (result != null) {
				if (result.target.getName().endsWith("-SNAPSHOT.jar")) {
					result.coordinate.version = dependency.version;
					result.target = new File(result.directory, dependency.getJarName());
				}
			}
			else if (!quiet && !dependency.optional)
				err.println("Artifact " + dependency.artifactId + " not found" + (downloadAutomatically ? "" : "; consider 'get-dependencies'"));
			return cacheAndReturn(key, result);
		}

		protected File findInFijiDirectories(Coordinate dependency) {
			for (String jarName : new String[] {
				"jars/" + dependency.artifactId + "-" + dependency.version + ".jar",
				"plugins/" + dependency.artifactId + "-" + dependency.version + ".jar",
				"jars/" + dependency.artifactId + ".jar",
				"plugins/" + dependency.artifactId + ".jar"
			}) {
				File file = new File(System.getProperty("ij.dir"), jarName);
				if (file.exists())
					return file;
			}
			return null;
		}

		protected POM cacheAndReturn(String key, POM pom) {
			localPOMCache.put(key, pom);
			return pom;
		}

		// TODO: if there is no internet connection, do not try to download -SNAPSHOT versions
		protected boolean maybeDownloadAutomatically(Coordinate dependency, boolean quiet) {
			if (!downloadAutomatically || offlineMode)
				return true;
			try {
				download(dependency, quiet);
			} catch (Exception e) {
				if (!quiet && !dependency.optional) {
					e.printStackTrace(err);
					err.println("Could not download " + dependency.artifactId + ": " + e.getMessage());
				}
				String key = dependency.groupId + ">" + dependency.artifactId;
				localPOMCache.put(key, null);
				return false;
			}
			return true;
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
				properties.put("project.groupId", coordinate.groupId);
			if (!properties.containsKey("project.version"))
				properties.put("project.version", coordinate.version);
		}

		public void startElement(String uri, String name, String qualifiedName, Attributes attributes) {
			prefix += ">" + qualifiedName;
			if (debug)
				err.println("start(" + uri + ", " + name + ", " + qualifiedName + ", " + toString(attributes) + ")");
		}

		public void endElement(String uri, String name, String qualifiedName) {
			if (prefix.equals(">project>dependencies>dependency") || (isCurrentProfile && prefix.equals(">project>profiles>profile>dependencies>dependency"))) {
				if (debug)
					err.println("Adding dependendency " + latestDependency + " to " + this);
				if (coordinate.artifactId.equals("javassist") && latestDependency.artifactId.equals("tools"))
					latestDependency.optional = false;
				dependencies.add(latestDependency);
				latestDependency = new Coordinate();
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
				coordinate.groupId = string;
			else if (prefix.equals(">project>parent>groupId")) {
				if (coordinate.groupId == null)
					coordinate.groupId = string;
			}
			else if (prefix.equals(">project>artifactId"))
				coordinate.artifactId = string;
			else if (prefix.equals(">project>version"))
				coordinate.version = string;
			else if (prefix.equals(">project>parent>version")) {
				if (coordinate.version == null)
					coordinate.version = string;
			}
			else if (prefix.equals(">project>modules"))
				buildFromSource = true; // might not be building a target
			else if (prefix.equals(">project>modules>module"))
				modules.add(string);
			else if (prefix.startsWith(">project>properties>"))
				properties.put(prefix.substring(">project>properties>".length()), string);
			else if (prefix.equals(">project>dependencies>dependency>groupId"))
				latestDependency.groupId = string;
			else if (prefix.equals(">project>dependencies>dependency>artifactId"))
				latestDependency.artifactId = string;
			else if (prefix.equals(">project>dependencies>dependency>version"))
				latestDependency.version = string;
			else if (prefix.equals(">project>dependencies>dependency>scope"))
				latestDependency.scope = string;
			else if (prefix.equals(">project>dependencies>dependency>optional"))
				latestDependency.optional = string.equalsIgnoreCase("true");
			else if (prefix.equals(">project>dependencies>dependency>systemPath"))
				latestDependency.systemPath = string;
			else if (prefix.equals(">project>dependencies>dependency>classifier"))
				latestDependency.classifier = string;
			else if (prefix.equals(">project>profiles>profile>id")) {
				isCurrentProfile = (!Util.getPlatform().equals("macosx") && "javac".equals(string)) || (coordinate.artifactId.equals("javassist") && string.equals("jdk16"));
				if (debug)
					err.println((isCurrentProfile ? "Activating" : "Ignoring") + " profile " + string);
			}
			else if (!isCurrentProfile && prefix.equals(">project>profiles>profile>activation>file>exists"))
				isCurrentProfile = new File(directory, string).exists();
			else if (!isCurrentProfile && prefix.equals(">project>profiles>profile>activation>activeByDefault"))
				isCurrentProfile = "true".equalsIgnoreCase(string);
			else if (prefix.equals(">project>repositories>repository>url"))
				repositories.add(string);
			else if (prefix.equals(">project>build>sourceDirectory"))
				sourceDirectory = string;
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
			int result = coordinate.artifactId.compareTo(other.coordinate.artifactId);
			if (result != 0)
				return result;
			if (coordinate.groupId != null && other.coordinate.groupId != null)
				result = coordinate.groupId.compareTo(other.coordinate.groupId);
			if (result != 0)
				return result;
			if (coordinate.version != null && other.coordinate.version != null)
				return compareVersion(coordinate.version, other.coordinate.version);
			return 0;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			append(builder, "");
			return builder.toString();
		}

		public void append(StringBuilder builder, String indent) {
			builder.append(indent + coordinate.groupId + ">" + coordinate.artifactId + "\n");
			if (children != null)
				for (POM child : children)
					child.append(builder, indent + "  ");
		}
	}

	protected void downloadAndVerify(String repositoryURL, Coordinate dependency, boolean quiet) throws MalformedURLException, IOException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
		String path = "/" + dependency.groupId.replace('.', '/') + "/" + dependency.artifactId + "/" + dependency.version + "/";
		File directory = new File(System.getProperty("user.home") + "/.m2/repository" + path);
		if (dependency.version.endsWith("-SNAPSHOT")) {
			// Only check snapshots once per day
			File snapshotMetaData = new File(directory, "maven-metadata-snapshot.xml");
			if (System.currentTimeMillis() - snapshotMetaData.lastModified() < updateInterval * 60 * 1000l)
				return;

			String message = quiet ? null : "Checking for new snapshot of " + dependency.artifactId;
			String metadataURL = repositoryURL + path + "maven-metadata.xml";
			downloadAndVerify(metadataURL, directory, snapshotMetaData.getName(), message);
			dependency.version = parseSnapshotVersion(snapshotMetaData);
			if (dependency.version == null)
				throw new IOException("No version found in " + metadataURL);
			if (new File(directory, dependency.getJarName()).exists() &&
					new File(directory, dependency.getPOMName()).exists())
				return;
		}
		else if (dependency.version.startsWith("[")) {
			path = "/" + dependency.groupId.replace('.', '/') + "/" + dependency.artifactId + "/";
			directory = new File(System.getProperty("user.home") + "/.m2/repository" + path);

			// Only check versions once per day
			File versionMetaData = new File(directory, "maven-metadata-version.xml");
			if (System.currentTimeMillis() - versionMetaData.lastModified() < 24 * 60 * 60 * 1000l)
				return;

			String message = quiet ? null : "Checking for new version of " + dependency.artifactId;
			String metadataURL = repositoryURL + path + "maven-metadata.xml";
			downloadAndVerify(metadataURL, directory, versionMetaData.getName(), message);
			dependency.version = parseVersion(versionMetaData);
			if (dependency.version == null)
				throw new IOException("No version found in " + metadataURL);
			path = "/" + dependency.groupId.replace('.', '/') + "/" + dependency.artifactId + "/" + dependency.version + "/";
			directory = new File(System.getProperty("user.home") + "/.m2/repository" + path);
			if (new File(directory, dependency.getJarName()).exists() &&
					new File(directory, dependency.getPOMName()).exists())
				return;
		}
		String message = quiet ? null : "Downloading " + dependency.artifactId;
		String baseURL = repositoryURL + path;
		downloadAndVerify(baseURL + dependency.getPOMName(), directory, null);
		downloadAndVerify(baseURL + dependency.getJarName(), directory, message);
	}

	protected void downloadAndVerify(String url, File directory, String message) throws IOException, NoSuchAlgorithmException {
		downloadAndVerify(url, directory, null, message);
	}

	protected void downloadAndVerify(String url, File directory, String fileName, String message) throws IOException, NoSuchAlgorithmException {
		File sha1 = download(new URL(url + ".sha1"), directory, fileName == null ? null : fileName + ".sha1", null);
		File file = download(new URL(url), directory, fileName, message);
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

	protected static String parseSnapshotVersion(File xml) throws IOException, ParserConfigurationException, SAXException {
		return parseSnapshotVersion(new FileInputStream(xml));
	}

	protected static String parseSnapshotVersion(InputStream in) throws IOException, ParserConfigurationException, SAXException {
		SnapshotPOMHandler handler = new SnapshotPOMHandler();
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		reader.setContentHandler(handler);
		reader.parse(new InputSource(in));
		if (handler.snapshotVersion != null && handler.timestamp != null && handler.buildNumber != null)
			return handler.snapshotVersion + "-" + handler.timestamp + "-" + handler.buildNumber;
		throw new IOException("Missing timestamp/build number: " + handler.timestamp + ", " + handler.buildNumber);
	}

	protected static class SnapshotPOMHandler extends DefaultHandler {
		protected String qName;
		protected String snapshotVersion, timestamp, buildNumber;

		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			this.qName = qName;
		}

		public void endElement(String uri, String localName, String qName) {
			this.qName = null;
		}

		public void characters(char[] ch, int start, int length) {
			if (qName == null)
				; // ignore
			else if (qName.equals("version")) {
				String version = new String(ch, start, length).trim();
				if (version.endsWith("-SNAPSHOT"))
					snapshotVersion = version.substring(0, version.length() - "-SNAPSHOT".length());
			}
			else if (qName.equals("timestamp"))
				timestamp = new String(ch, start, length).trim();
			else if (qName.equals("buildNumber"))
				buildNumber = new String(ch, start, length).trim();
		}
	}

	protected static String parseVersion(File xml) throws IOException, ParserConfigurationException, SAXException {
		return parseVersion(new FileInputStream(xml));
	}

	protected static String parseVersion(InputStream in) throws IOException, ParserConfigurationException, SAXException {
		VersionPOMHandler handler = new VersionPOMHandler();
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		reader.setContentHandler(handler);
		reader.parse(new InputSource(in));
		if (handler.version != null)
			return handler.version;
		throw new IOException("Missing version");
	}

	protected static class VersionPOMHandler extends DefaultHandler {
		protected String qName;
		protected String version;

		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			this.qName = qName;
		}

		public void endElement(String uri, String localName, String qName) {
			this.qName = null;
		}

		public void characters(char[] ch, int start, int length) {
			if (qName == null)
				; // ignore
			else if (qName.equals("version")) {
				version = new String(ch, start, length).trim();
			}
		}
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

	protected File download(URL url, File directory, String message) throws IOException {
		return download(url, directory, null, message);
	}

	protected File download(URL url, File directory, String fileName, String message) throws IOException {
		if (offlineMode)
			throw new RuntimeException("Offline!");
		if (verbose)
			err.println("Trying to download " + url);
		if (fileName == null) {
			fileName = url.getPath();
			fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
		InputStream in = url.openStream();
		if (message != null)
			err.println(message);
		directory.mkdirs();
		File result = new File(directory, fileName);
		copy(in, result);
		return result;
	}

	protected static void copyFile(File source, File target) throws IOException {
		copy(new FileInputStream(source), target);
	}

	protected static void copy(InputStream in, File target) throws IOException {
		copy(in, new FileOutputStream(target), true);
	}

	protected static void copy(InputStream in, OutputStream out, boolean closeOutput) throws IOException {
		byte[] buffer = new byte[131072];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
		}
		in.close();
		if (closeOutput)
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

	protected static void ensureIJDirIsSet() {
		String ijDir = System.getProperty("ij.dir");
		if (ijDir != null && new File(ijDir).isDirectory())
			return;
		ijDir = MiniMaven.class.getResource("MiniMaven.class").toString();
		for (String prefix : new String[] { "jar:", "file:" })
			if (ijDir.startsWith(prefix))
				ijDir = ijDir.substring(prefix.length());
		int bang = ijDir.indexOf("!/");
		if (bang < 0)
			throw new RuntimeException("Funny ?-) " + ijDir);
		ijDir = ijDir.substring(0, bang);
		for (String suffix : new String[] { "fake.jar", File.separator, "jars", File.separator })
			if (ijDir.endsWith(suffix))
				ijDir = ijDir.substring(0, ijDir.length() - suffix.length());
		System.setProperty("ij.dir", ijDir);
	}

	private final static String usage = "Usage: MiniMaven [command]\n"
		+ "\tSupported commands: compile, run, compile-and-run, clean, get-dependencies";

	public static void main(String[] args) throws Exception {
		ensureIJDirIsSet();
		MiniMaven miniMaven = new MiniMaven(null, System.err, false);
		POM root = miniMaven.parse(new File("pom.xml"), null);
		String command = args.length == 0 ? "compile-and-run" : args[0];
		String artifactId = getSystemProperty("artifactId", "ij-app");
		String mainClass = getSystemProperty("mainClass", "imagej.Main");

		POM pom = root.findPOM(new Coordinate(null, artifactId, null));
		if (pom == null)
			pom = root;
		if (command.equals("compile") || command.equals("build") || command.equals("compile-and-run")) {
			pom.build();
			if (command.equals("compile-and-run"))
				command = "run";
			else
				return;
		}
		else if (command.equals("jar") || command.equals("jars")) {
			if (!pom.buildFromSource) {
				System.err.println("Cannot build " + pom + " from source");
				System.exit(1);
			}
			pom.buildJar();
			if (command.equals("jars"))
				pom.copyDependencies(new File(pom.directory, "target"), true);
			return;
		}
		if (command.equals("clean"))
			pom.clean();
		else if (command.equals("get") || command.equals("get-dependencies"))
			pom.downloadDependencies();
		else if (command.equals("run")) {
			String[] paths = pom.getClassPath(false).split(File.pathSeparator);
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
			miniMaven.err.println(pom.getClassPath(false));
		else if (command.equals("list")) {
			Set<POM> result = new TreeSet<POM>();
			Stack<POM> stack = new Stack<POM>();
			stack.push(pom.getRoot());
			while (!stack.empty()) {
				pom = stack.pop();
				if (result.contains(pom) || !pom.buildFromSource)
					continue;
				result.add(pom);
				for (POM child : pom.children)
					stack.push(child);
			}
			for (POM pom2 : result)
				System.err.println(pom2);
		}
		else
			miniMaven.err.println("Unhandled command: " + command + "\n" + usage);
	}

	protected static String getSystemProperty(String key, String defaultValue) {
		String result = System.getProperty(key);
		return result == null ? defaultValue : result;
	}
}
