/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */
package fiji.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public abstract class Rule implements Comparable<Rule> {
	protected Parser parser;
	protected String target;
	protected String prerequisiteString;
	protected List<String> prerequisites, nonUpToDates;
	protected boolean wasAlreadyInvoked;
	protected boolean wasAlreadyChecked;

	/*
	 * 0 means upToDate() was not yet run,
	 * 1 means upToDate() is in the process of being run,
	 * 2 means upToDate() returns true
	 * 3 means upToDate() returns false
	 */
	protected int upToDateStage;

	public Rule(Parser parser, String target, final List<String> prerequisites) {
		this.parser = parser;
		this.target = target;
		this.prerequisites = prerequisites;
		try {
			String name = "TARGET(" + target + ")";
			if (!parser.variables.containsKey(name))
				parser.setVariable(name, target);
			name = "PRE(" + target + ")";
			if (!parser.variables.containsKey(name))
				parser.setVariable(name,
					new Object() {
						public String toString() {
							return Util.join(prerequisites);
						}
					});
		} catch (FakeException e) { /* ignore */ }
	}

	abstract void action() throws FakeException;

	public final boolean upToDate() {
		if (upToDateStage == 1)
			throw new RuntimeException("Circular "
				+ "dependency detected in rule "
				+ this);
		if (upToDateStage > 0)
			return upToDateStage == 2;
		upToDateStage = 1;
		upToDateStage = checkUpToDate() ? 2 : 3;
		return upToDateStage == 2;
	}

	boolean checkUpToDate() {
		// this implements the mtime check
		File file = new File(Util.makePath(parser.cwd, target));
		if (!file.exists()) {
			nonUpToDates = prerequisites;
			return false;
		}
		if (!newerThanFake(file))
			return false;

		long targetModifiedTime = file.lastModified();
		nonUpToDates = new ArrayList<String>();
		for (String prereq : prerequisites) {
			String path = Util.makePath(parser.cwd, prereq);
			if (new File(path).lastModified()
					> targetModifiedTime)
				nonUpToDates.add(prereq);
		}

		return nonUpToDates.size() == 0;
	}

	protected boolean newerThanFake(File file) {
		long modifiedTime = file.lastModified();

		if (getVarBool("rebuildIfFakeIsNewer")) {
			if (modifiedTime < parser.mtimeFakefile)
				return upToDateError(file, new File(Parser.path));
			if (modifiedTime < Fake.mtimeFijiBuild)
				return upToDateError(new File(Fake.fijiBuildJar), file);
		}
		return true;
	}

	boolean upToDate(String path) {
		if (path == null)
			return true;
		return upToDate(new File(path),
			new File(parser.cwd, target));
	}

	boolean upToDate(File source, File target) {
		if (target.equals(source))
			return true;
		if (!source.exists())
			return true;
		if (!target.exists())
			return false;
		long targetModified = target.lastModified();
		long sourceModified = source.lastModified();
		if (targetModified == sourceModified &&
				Util.compare(source, target) == 0)
			return true;
		if (targetModified < sourceModified)
			return upToDateError(source, target);
		return true;
	}

	boolean upToDate(String source, String target, File cwd) {
		return upToDate(new File(Util.makePath(cwd, source)),
			new File(Util.makePath(cwd, target)));
	}

	boolean upToDateError(File source, File target) {
		verbose("" + target + " is not up-to-date "
			+ "because " + source + " is newer: "
			+ new Date(target.lastModified()) + " < "
			+ new Date(source.lastModified()));
		return false;
	}

	protected boolean upToDateRecursive(File source, File target) {
		return upToDateRecursive(source, target, false);
	}

	protected boolean upToDateRecursive(File source, File target, boolean excludeTopLevelJars) {
		if (!source.exists())
			return true;
		if (!source.isDirectory())
			return upToDate(source, target);
		String[] entries = source.list();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].startsWith("."))
				continue;
			if (excludeTopLevelJars && entries[i].endsWith(".jar"))
				continue;
			File file = new File(source,
					entries[i]);
			if (!upToDateRecursive(file, target, false))
				return false;
		}
		return true;
	}

	public void makeParallel(int maxThreads) throws FakeException {
		ParallelMaker make = new ParallelMaker(parser, maxThreads, Collections.singletonList(this));
		FakeException result = make.run();
		if (result != null)
			throw result;
	}

	public void make() throws FakeException {
		make(true);
	}

	public void make(boolean makePrerequisitesFirst) throws FakeException {
		if (wasAlreadyChecked)
			return;
		wasAlreadyChecked = true;
		if (wasAlreadyInvoked)
			error("Dependency cycle detected!");
		wasAlreadyInvoked = true;

		try {
			if (makePrerequisitesFirst) {
				verbose("Checking prerequisites of " + this);
				makePrerequisites();
			}

			if (upToDate())
				return;
			parser.fake.err.println("Building " + this);
			action();
			if (new File(target).exists())
				upToDateStage = 2;
			else {
				upToDateStage = 0;
				wasAlreadyChecked = false;
			}
			setUpToDate();
		} catch (Exception e) {
			if (!(e instanceof FakeException))
				e.printStackTrace();
			new File(target).delete();
			error(e.getMessage());
			if (e instanceof FakeException)
				throw (FakeException)e;
		}
		wasAlreadyInvoked = false;
	}

	protected void setUpToDate() throws IOException, FakeException {
		upToDateStage = 2;
		if (target.equals(""))
			return;
		if (prerequisites.size() == 0) {
			if (newerThanFake(new File(target)))
				return;
		}
		else if (checkUpToDate())
			return;
		Util.touchFile(target);
	}

	protected void clean(boolean dry_run) {
		clean(target, dry_run);
	}

	protected void clean(String path, boolean dry_run) {
		String precomp = getVar("PRECOMPILEDDIRECTORY");
		if (precomp != null && path.startsWith(precomp))
			return;
		File file = new File(path);
		if (file.exists() && !file.isDirectory()) {
			if (dry_run)
				parser.fake.out.println("rm "
					+ path);
			else
				file.delete();
		}
	}

	protected void error(String message)
			throws FakeException {
		throw new FakeException(message
				+ "\n\tin rule " + this);
	}

	protected void debugLog(String message) {
		if (!getVarBool("DEBUG"))
			return;
		parser.fake.err.println(message);
	}

	protected void verbose(String message) {
		if (!getVarBool("VERBOSE"))
			return;
		parser.fake.err.println(message);
	}

	public String getTarget() {
		return target;
	}

	public Rule getRule(String prereq) {
		if (prereq.endsWith(".jar/"))
			prereq = Util.stripSuffix(prereq, "/");
		return (Rule)parser.allRules.get(prereq);
	}

	public Iterable<String> getJarDependencies() throws FakeException {
		Set<String> result = new TreeSet<String>();
		for (String prereq : prerequisites)
			if (prereq.endsWith(".jar"))
				result.add(prereq);

		// check the classpath
		for (String jarFile : Util.splitPaths(getVar("CLASSPATH")))
			if (jarFile.endsWith(".jar"))
				result.add(jarFile);

		return result;
	}

	public Iterable<Rule> getDependencies() throws FakeException {
		Set<Rule> result = new HashSet<Rule>();
		for (String prereq : prerequisites) {
			Rule rule = getRule(prereq);
			if (rule != null)
				result.add(rule);
			else if (this instanceof All)
				error("Unknown target: " + prereq);
		}

		// check the classpath
		for (String jarFile : Util.splitPaths(getVar("CLASSPATH"))) {
			Rule rule = getRule(Util.stripIJDir(jarFile));
			if (rule != null)
				result.add(rule);
		}
		return result;
	}

	public Iterable<Rule> getDependenciesRecursively() throws FakeException {
		Set<Rule> result = new TreeSet<Rule>();
		getDependenciesRecursively(result);
		return result;
	}

	public void getDependenciesRecursively(Set<Rule> result) throws FakeException {
		for (Rule rule : getDependencies())
			if (!result.contains(rule) &&
					(rule instanceof CompileCProgram ||
					 rule instanceof CompileClass ||
					 rule instanceof CompileJar ||
					 rule instanceof CopyJar ||
					 rule instanceof SubFake)) {
				result.add(rule);
				rule.getDependenciesRecursively(result);
			}
	}

	public List<String> getDependenciesAsStrings() {
		List<String> dependencies = new ArrayList<String>(prerequisites);
		Collections.addAll(dependencies, Util.splitPaths(getVar("CLASSPATH")));
		return dependencies;
	}

	public void makePrerequisites() throws FakeException {
		for (Rule rule : getDependencies())
			rule.make();
	}

	public String getLastPrerequisite() {
		int index = prerequisites.size() - 1;
		return prerequisites.get(index);
	}

	public List<String> getPrerequisites() {
		return new ArrayList<String>(prerequisites);
	}

	public String toString() {
		return toString("2".equals(getVar("VERBOSE")) ?
				0 : 60);
	}

	public String toString(int maxCharacters) {
		String target = this.target;
		String result = "";
		if (getVarBool("VERBOSE") &&
				!(this instanceof Special)) {
			String type = getClass().getName();
			int dollar = type.lastIndexOf('$');
			if (dollar >= 0)
				type = type.substring(dollar + 1);
			result += "(" + type + ") ";
		}
		if (getVarBool("DEBUG") && (this
				instanceof ExecuteProgram))
			target += "[" +
				((ExecuteProgram)this).program
				+ "]";
		result += target + " <- " + Util.join(prerequisites, " ");
		if (maxCharacters > 0 && result.length()
				> maxCharacters)
			result = result.substring(0,
				Math.max(0, maxCharacters - 3))
				+ "...";
		return result;
	}

	public String getVar(String key) {
		return getVar(key, target);
	}

	public String getVar(String key, String subkey) {
		return parser.getVariable(key, subkey, target);
	}

	public boolean getVarBool(String key) {
		return Util.getBool(getVar(key, target));
	}

	public boolean getVarBool(String key, String subkey) {
		return Util.getBool(getVar(key, subkey));
	}

	public File getBuildDir() {
		String prebuilt = getVar("prebuiltdir");
		if (prebuilt != null)
			return new File(Util.makePath(parser.cwd, prebuilt));
		String dir = getVar("builddir");
		if (dir == null || dir.equals(""))
			return null;
		String suffix = Util.stripIJDir(target);
		// strip DOS drive prefix
		if (suffix.length() > 2 && suffix.charAt(1) == ':')
			suffix = suffix.substring(2);
		suffix = Util.stripSuffix(suffix, ".class");
		suffix = Util.stripSuffix(suffix, ".jar");
		return new File(Util.makePath(parser.cwd, dir + "/" + suffix));
	}

	List<String> compileJavas(List<String> javas, File buildDir,
			Set<String> exclude, Set<String> noCompile)
			throws FakeException {
		return parser.fake.compileJavas(javas, parser.cwd, buildDir,
			getVar("JAVAVERSION"),
			getVarBool("DEBUG"),
			getVarBool("VERBOSE"),
			getVarBool("SHOWDEPRECATION"),
			getVar("CLASSPATH"),
			exclude, noCompile);
	}

	String getPluginsConfig() {
		String path = getVar("pluginsConfigDirectory");
		if (path == null || path.equals(""))
			return null;
		path += "/" + getBaseName(target) + ".config";
		if (!new File(path).exists())
			return null;
		return path;
	}

	protected String getBaseName(String target) {
		String key = target;
		if (key.endsWith(".jar"))
			key = key.substring(0,
					key.length() - 4);
		int slash = key.lastIndexOf('/');
		if (slash >= 0)
			key = key.substring(slash + 1);
		return key;
	}

	/* Copy source to target if target is not up-to-date */
	void copyJar(String source, String target, File cwd,
			String configPath)
			throws FakeException {
		if (configPath == null) {
			if (upToDate(source, target, cwd))
				return;
			Util.copyFile(source, target, cwd);
		}
		else try {
			copyJarWithPluginsConfig(source, target,
				cwd, configPath);
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Couldn't copy "
				+ source + " to " + target
				+ ": " + e);
		}
	}

	void copyJarWithPluginsConfig(String source,
			String target, File cwd,
			String configPath) throws Exception {
		if (upToDate(source) && upToDate(configPath,
				target, cwd))
			return;
		if (parser.fake.jarUpToDate(source, target,
				getVarBool("VERBOSE"))) {
			if (upToDate(configPath)) {
				Util.touchFile(target);
				return;
			}
			verbose(source + " is not up-to-date "
				+ " because of " + configPath);
		}

		File file = new File(Util.makePath(cwd, source));
		InputStream input = new FileInputStream(file);
		JarInputStream in = new JarInputStream(input);

		Manifest manifest = in.getManifest();
		file = new File(Util.makePath(cwd, target));
		OutputStream output =
			new FileOutputStream(file);
		JarOutputStream out = manifest == null ?
			new JarOutputStream(output) :
			new JarOutputStream(output, manifest);

		Fake.addPluginsConfigToJar(out, configPath);

		JarEntry entry;
		while ((entry = in.getNextJarEntry()) != null) {
			String name = entry.getName();
			if (name.equals("plugins.config") &&
					configPath != null) {
				in.closeEntry();
				continue;
			}
			byte[] buf = Util.readStream(in);
			in.closeEntry();
			entry.setCompressedSize(-1);
			parser.fake.writeJarEntry(entry, out, buf);
		}
		in.close();
		out.close();
	}

	public String getPrerequisiteString() {
		return prerequisiteString;
	}

	public String getStripPath() {
		String s = prerequisiteString.trim();
		if (s.startsWith("**/"))
			return "";
		int stars = s.indexOf("/**/");
		if (stars < 0)
			return "";
		int space = s.indexOf(' ');
		if (space > 0 && space < stars) {
			if (s.charAt(space - 1) == '/')
				return s.substring(0, space);
			return "";
		}
		return s.substring(0, stars + 1);
	}

	public File getWorkingDirectory() {
		return parser.cwd;
	}

	/* Java5 does not like @Override for methods implementing an interface */
	public int compareTo(Rule other) {
		return target.compareTo(other.target);
	}

	public abstract Rule copy();
}