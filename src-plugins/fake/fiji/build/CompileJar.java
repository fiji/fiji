package fiji.build;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompileJar extends Rule {
	protected String configPath;
	protected String classPath;
	protected CompileNativeLibrary compileLibrary;

	public CompileJar(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, Util.uniq(prerequisites));
		configPath = getPluginsConfig();
		for (String prereq : prerequisites) {
			if (!prereq.endsWith(".jar/"))
				continue;
			prereq = Util.stripSuffix(prereq, "/");
			if (classPath == null)
				classPath = prereq;
			else
				classPath += ":" + prereq;
		}

		List<String> nativeSources = CompileNativeLibrary.filterNativeSources(prerequisites);
		if (nativeSources.size() > 0) {
			File buildDir = getVarBool("includeSources") ? getBuildDir() : null;
			compileLibrary = new CompileNativeLibrary(parser, getBaseName(target), new File(target), buildDir, nativeSources);
			if (parser.allRules.containsKey(compileLibrary.target) && (parser.getRule(compileLibrary.target) instanceof CompileNativeLibrary))
				compileLibrary = (CompileNativeLibrary)parser.getRule(compileLibrary.target);
			else
				parser.allRules.put(compileLibrary.target, compileLibrary);
		}
	}

	public String getVar(String var) {
		String value = super.getVar(var);
		if (parser.isVarName(var, "CLASSPATH")) {
			if (classPath != null) {
				return (value == null) ? classPath
					: (value + ":" + classPath);
			}
		}
		return value;
	}

	void action() throws FakeException {
		File buildDir = getBuildDir();
		Set<String> noCompile =
			parser.fake.expandToSet(getVar("NO_COMPILE"), parser.cwd);
		Set<String> exclude =
			parser.fake.expandToSet(getVar("EXCLUDE"), parser.cwd);
		if (getVar("PREBUILTDIR") == null) {
			compileJavas(prerequisites, buildDir, exclude, noCompile);
			if (!target.equals("jacl.jar")) // known to mix *two* directories of sources
				new ObsoleteClassFiles(parser.fake.err, new File(Util.makePath(parser.cwd, getStripPath())), buildDir).removeFiles();
		}
		List<String> files = parser.fake.java2classFiles(prerequisites,
			parser.cwd, buildDir, exclude, noCompile);

		if (getVarBool("includeSource"))
			addSources(files);
		else if (compileLibrary != null)
			for (String fileName : compileLibrary.prerequisites)
				files.remove(fileName);

		if (buildDir != null)
			addNonClasses(files, buildDir, "");

		parser.fake.makeJar(target, getMainClass(), files, parser.cwd,
			buildDir, configPath, getStripPath(),
			getVarBool("VERBOSE"));
		if (compileLibrary != null)
			compileLibrary.action();
	}

	void addSources(List<String> files) {
		for (String file : prerequisites)
			if (file.endsWith(".java"))
				files.add(file);
	}

	void addNonClasses(List<String> files, File buildDir, String prefix) {
		for (File file : buildDir.listFiles()) {
			String name = file.getName();
			if (file.isDirectory()) {
				if (!name.startsWith("."))
					addNonClasses(files, file, prefix + name + "/");
			}
			else if (file.isFile() && !name.endsWith(".class"))
				files.add(prefix + name + "[" + file.getAbsolutePath() + "]");
		}
	}

	void maybeMake(Rule rule) throws FakeException {
		if (rule == null || rule.upToDate())
			return;
		verbose("Making " + rule
			+ " because it is in the CLASSPATH of "
			+ this);
		rule.make();
	}

	boolean notUpToDate(String reason) {
		verbose("" + target
			+ " is not up-to-date because of "
			+ reason);
		return false;
	}

	boolean checkUpToDate() {
		// handle xyz[from/here] targets
		for (String path : prerequisites) {
			int bracket = path.indexOf('[');
			if (bracket < 0 || !path.endsWith("]"))
				continue;
			path = path.substring(bracket + 1,
				path.length() - 1);
			if (path.startsWith("jar:file:")) {
				int exclamation =
					path.indexOf('!');
				path = path.substring(9,
						exclamation);
			}
			if (!upToDate(path))
				return notUpToDate(path);
		}
		// check the classpath
		String[] paths = Util.splitPaths(getVar("CLASSPATH"));
		for (int i = 0; i < paths.length; i++) {
			if (!paths[i].equals(".") &&
					!upToDate(paths[i]))
				return notUpToDate(paths[i]);
			Rule rule = (Rule)parser.allRules.get(paths[i]);
			if (rule != null && !rule.upToDate())
				return notUpToDate(rule.target);
		}
		if (compileLibrary != null && !compileLibrary.upToDate())
			return notUpToDate("Native library: " + compileLibrary.target);
		return super.checkUpToDate() &&
			upToDate(configPath);
	}

	String getMainClass() {
		return parser.getVariable("MAINCLASS", target);
	}

	protected void clean(boolean dry_run) {
		super.clean(dry_run);
		File buildDir = getBuildDir();
		if (buildDir != null) {
			if (dry_run)
				parser.fake.out.println("rm -rf "
					+ buildDir.getPath());
			else if (buildDir.exists())
				parser.fake.deleteRecursively(buildDir);
			return;
		}
		List<String> javas = new ArrayList<String>();
		addSources(javas);

		try {
			Set<String> exclude = parser.fake.expandToSet(
				getVar("EXCLUDE"), parser.cwd);
			Set<String> noCompile = parser.fake.expandToSet(
				getVar("NO_COMPILE"), parser.cwd);
			exclude.addAll(noCompile);
			for (String file : parser.fake.java2classFiles(javas, parser.cwd, getBuildDir(), exclude, noCompile))
				clean(file, dry_run);
		} catch (FakeException e) {
			parser.fake.err.println("Warning: could not "
				+ "find required .class files: "
				+ this);
			return;
		}
	}

	@Override
	public CompileJar copy() {
		CompileJar copy = new CompileJar(parser, target, prerequisites);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}
