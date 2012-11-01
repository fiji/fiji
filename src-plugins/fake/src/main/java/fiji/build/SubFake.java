package fiji.build;

import fiji.build.minimaven.BuildEnvironment;
import fiji.build.minimaven.Coordinate;
import fiji.build.minimaven.JavaCompiler.CompileError;
import fiji.build.minimaven.POM;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class SubFake extends Rule {
	protected String jarName;
	protected String baseName;
	protected String source;
	protected String configPath;
	protected POM pom;
	protected boolean pomRead;

	SubFake(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
		jarName = new File(target).getName();
		String directory = getLastPrerequisite();
		source = directory + jarName;
		baseName = Util.stripSuffix(jarName, ".jar");
		configPath = getPluginsConfig();

		String[] paths =
			Util.splitPaths(getVar("CLASSPATH"));
		for (int i = 0; i < paths.length; i++)
			prerequisites.add(prerequisites.size() - 1, paths[i]);
		if (!new File(Util.makePath(parser.cwd, directory)).exists())
			parser.fake.err.println("Warning: " + directory
				+ " does not exist!");

		// Special-case: if we're adding an aggregator pom, add implicit rules for all child poms
		if (getFakefile() == null) {
			POM pom = getPOM();
			if (pom != null && "pom".equals(pom.getPackaging()))
				addChildren(parser, pom);
		}
	}

	protected void addChildren(Parser parser, POM pom) {
		if (pom.getChildren() == null)
			return;
		for (POM child : pom.getChildren()) {
			String packaging = child.getPackaging();
			if (child.getBuildFromSource() && "jar".equals(packaging)) {
				String target = (isImageJ1Plugin(child.getDirectory()) ? "plugins" : "jars") + "/" + child.getArtifactId() + ".jar";
				parser.allRules.put(target, new SubFake(parser, target, Arrays.asList(child.getDirectory().getPath())));
			}
			else if ("pom".equals(packaging))
				addChildren(parser, child);
		}
	}

	@Override
	boolean checkUpToDate() {
		if (!upToDate(configPath)) {
			verbose(target + " is not up-to-date because of " + configPath);
			return false;
		}

		// check the classpath
		for (String path : Util.splitPaths(getVar("CLASSPATH"))) {
			Rule rule = (Rule)parser.allRules.get(path);
			if (rule != null && !rule.upToDate()) {
				verbose(target + " is not up-to-date because of " + path);
				return false;
			}
		}

		File target = new File(this.target);
		for (String directory : prerequisites) try {
			if (!checkUpToDate(directory, target))
				return false;
		} catch (FakeException e) {
			e.printStackTrace();
			return false;
		}

		String directory = getLastPrerequisite();
		if (!Util.isDirEmpty(Util.makePath(parser.cwd, directory))) try {
			File file = getFakefile();
			if (file != null) {
				Parser parser = this.parser.fake.parseFakefile(new File(this.parser.cwd, getLastPrerequisite()), file, getVarBool("VERBOSE", directory), getVarPath("TOOLSPATH", directory), getVarPath("CLASSPATH", directory), getBuildDir());
				Rule all = parser.parseRules(null);
				Rule rule = parser.getRule(jarName);
				if (rule == null)
					rule = all;
				return rule.checkUpToDate();
			}

			POM pom = getPOM();
			if (pom != null) {
				if (!pom.upToDate(true)) {
					verbose("MiniMaven says " + target + " is not up-to-date");
					return false;
				}
				if (!upToDate(pom.getTarget(), target)) {
					verbose(pom.getTarget() + " is not up-to-date because of " + target);
					return false;
				}
				return true;
			}

			if (!upToDateRecursive(new File(Util.makePath(parser.cwd, directory)), target, true)) {
				verbose(target + " not up-to-date because of " + directory);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace(parser.fake.err);
			return false;
		}

		return true;
	}

	boolean checkUpToDate(String directory, File target) throws FakeException {
		if (!target.exists())
			return false;

		File dir = new File(Util.makePath(parser.cwd, directory));
		if (!dir.exists() || (dir.isDirectory()) &&
				dir.listFiles().length == 0) {
			String precompiled =
				getVar("PRECOMPILEDDIRECTORY");
			if (precompiled == null)
				return true;
			File source = new File(parser.cwd, precompiled + "/" + jarName);
			return upToDate(source, target);
		}
		return true;
	}

	public File getFakefile() {
		File file = new File(getLastPrerequisite(), "Fakefile");
		if (!file.exists())
			file = new File(getVar("PLUGINSCONFIGDIRECTORY"), baseName + ".Fakefile");
		return file.exists() ? file : null;
	}

	protected static BuildEnvironment miniMaven;

	public POM getPOM() {
		boolean verbose = getVarBool("VERBOSE");
		if (miniMaven != null) {
			miniMaven.setVerbose(verbose);
		}
		if (pomRead)
			return pom;
		File file = new File(Util.makePath(parser.cwd, getLastPrerequisite()), "pom.xml");
		if (!file.exists()) {
			pomRead = true;
			return pom;
		}
		String targetBasename = jarName.substring(jarName.lastIndexOf('/') + 1);
		if (targetBasename.endsWith(".jar"))
			targetBasename = targetBasename.substring(0, targetBasename.length() - 4);
		// TODO: targetBasename could end in "-<version>"
		try {
			boolean debug = getVarBool("DEBUG");
			if (miniMaven == null) {
				miniMaven = new BuildEnvironment(parser.fake.err, true, verbose, debug);
				MiniMaven.ensureIJDirIsSet();
				String ijDir = System.getProperty("ij.dir");
				File submodules = new File(ijDir, "modules");
				File srcPlugins = new File(ijDir, "src-plugins");
				miniMaven.excludeFromMultiProjects(file.getParentFile());
				if (submodules.exists()) {
					miniMaven.excludeFromMultiProjects(new File(submodules, "clojure"));
					miniMaven.addMultiProjectRoot(submodules);
				}
				if (srcPlugins.exists()) {
					miniMaven.addMultiProjectRoot(srcPlugins);
					File pom = new File(srcPlugins, "pom.xml");
					if (pom.exists())
						miniMaven.parse(pom);
				}
			}
			miniMaven.setVerbose(verbose);
			pom = miniMaven.parse(file);
			if (!targetBasename.equals(pom.getArtifactId())) {
				String groupId = pom.getGroupId();
				if (targetBasename.equals("imglib") || targetBasename.startsWith("imglib-"))
					groupId = "mpicbg";
				pom = pom.findPOM(new Coordinate(groupId, targetBasename, pom.getVersion()),
					verbose, miniMaven.getDownloadAutomatically());
			}
		} catch (Exception e) {
			e.printStackTrace(parser.fake.err);
		}
		pomRead = true;
		return pom;
	}

	void action() throws FakeException {
		String directory = getLastPrerequisite();
		checkObsoleteLocation(directory);

		// check the classpath
		for (String path : Util.splitPaths(getVar("CLASSPATH"))) {
			Rule rule = (Rule)parser.allRules.get(path);
			if (rule != null && !rule.upToDate())
				rule.action();
		}

		if (getFakefile() != null || new File(directory, "Makefile").exists())
			fakeOrMake(jarName);
		else {
			POM pom = getPOM();
			if (pom != null) try {
				buildPOM(pom);
				return;
			} catch (Exception e) {
				e.printStackTrace(parser.fake.err);
				throw new FakeException(e.getMessage());
			}
		}

		File file = new File(Util.makePath(parser.cwd, source));
		if (getVarBool("IGNOREMISSINGFAKEFILES") &&
				!file.exists() &&
				Util.isDirEmpty(getLastPrerequisite())) {
			String precompiled =
				getVar("PRECOMPILEDDIRECTORY");
			if (precompiled == null)
				return;
			source = precompiled + file.getName();
			if (!new File(Util.makePath(parser.cwd, source)).exists()) {
				parser.missingPrecompiledFallBack(target);
				return;
			}
		}
		else if (!file.exists())
			error("Target " + target + " was not built!");

		if (target.indexOf('.') >= 0)
			copyJar(source, target, parser.cwd, configPath);
	}

	protected void buildPOM(final POM pom) throws CompileError, FakeException, IOException, ParserConfigurationException, SAXException {
		if ("pom".equals(pom.getPackaging())) {
			for (POM child : pom.getChildren())
				buildPOM(child);
			return;
		}

		boolean isIJ1Plugin = isImageJ1Plugin(pom.getDirectory());
		final String subDirectory = isIJ1Plugin ? "plugins" : "jars";
		final String unversionedPath = subDirectory + "/" + pom.getArtifactId() + ".jar";
		boolean keepVersion = getVarBool("keepVersion", unversionedPath);
		boolean copyDependencies = getVarBool("copyDependencies", unversionedPath);
		final File targetDirectory = new File(System.getProperty("ij.dir"), subDirectory);
		final File unversioned = new File(targetDirectory, pom.getArtifactId() + ".jar");
		final File targetFile = keepVersion ? new File(targetDirectory, pom.getJarName()) : unversioned;
		if (pom.upToDate(true) && upToDate(pom.getTarget(), targetFile))
			return;
		pom.downloadDependencies();
		pom.buildJar();
		if (keepVersion && unversioned.exists()) {
			unversioned.delete();
		}
		copyJar(pom.getTarget().getPath(), targetFile.getPath(), parser.cwd, configPath);
		if (copyDependencies) {
			copyDependencies(pom, targetDirectory);
		}
	}

	protected void fakeOrMake(String subTarget) throws FakeException {
		String directory = getLastPrerequisite();
		parser.fake.fakeOrMake(parser.cwd, directory,
			getVarBool("VERBOSE", directory),
			getVarBool("IGNOREMISSINGFAKEFILES",
				directory),
			getVarPath("TOOLSPATH", directory),
			getVarPath("CLASSPATH", directory),
			getVar("PLUGINSCONFIGDIRECTORY")
				+ "/" + baseName + ".Fakefile",
			getBuildDir(),
			subTarget);
	}

	// if targetDirectory is one of jars/ & plugins/ and the other exists also, be clever
	protected void copyDependencies(POM pom, File targetDirectory) throws IOException, ParserConfigurationException, SAXException {
		File plugins = null;
		if (targetDirectory.getName().equals("plugins")) {
			File jars = new File(targetDirectory.getParentFile(), "jars");
			if (jars.isDirectory()) {
				plugins = targetDirectory;
				targetDirectory = jars;
			}
		}
		else if (targetDirectory.getName().equals("jars")) {
			plugins = new File(targetDirectory.getParentFile(), "plugins");
			if (!plugins.isDirectory())
				plugins = null;
		}

		for (POM dependency : pom.getDependencies(true, false, "test", "provided", "system")) {
			File file = dependency.getTarget();
			File directory = plugins != null && isImageJ1Plugin(file) ? plugins : targetDirectory;
			String jarName;
			final String artifactId = dependency.getArtifactId();
			if ((getVarBool("keepVersion") || artifactId.startsWith("imglib2")) && !artifactId.equals("Fiji_Updater")) {
				File unversioned = new File(directory, artifactId + ".jar");
				if (unversioned.exists())
					unversioned.delete();
				jarName = dependency.getJarName();
			} else
				jarName = dependency.getArtifactId() + ".jar";
			File destination = new File(directory, jarName);
			if (file.exists() && (!destination.exists() || destination.lastModified() < file.lastModified()))
				BuildEnvironment.copyFile(file, destination);
		}
	}

	protected boolean isImageJ1Plugin(File file) {
		String name = file.getName();
		if (name.indexOf('_') < 0 || !file.exists())
			return false;
		if (file.isDirectory())
			return new File(file, "src/main/resources/plugins.config").exists();
		if (name.endsWith(".jar")) try {
			JarFile jar = new JarFile(file);
			for (JarEntry entry : Collections.list(jar.entries()))
				if (entry.getName().equals("plugins.config")) {
					jar.close();
					return true;
			}
			jar.close();
		} catch (Throwable t) {
			// obviously not a plugin...
		}
		return false;
	}

	String getVarPath(String variable, String subkey) {
		String value = getVar(variable, subkey);
		if (value == null)
			return null;

		// Skip empty elements
		String result = "";
		StringTokenizer tokenizer = new StringTokenizer(value, ":");
		while (tokenizer.hasMoreElements()) {
			if (!result.equals(""))
				result += ":";
			String path = tokenizer.nextToken();
			result += path;
		}
		return result;
	}

	@Override
	protected void clean(boolean dry_run) {
		super.clean(dry_run);
		clean(getLastPrerequisite() + jarName, dry_run);
		File fakefile = getFakefile();
		if (fakefile != null) try {
			fakeOrMake(jarName + "-clean"
				+ (dry_run ? "-dry-run" : ""));
		} catch (FakeException e) {
			e.printStackTrace(parser.fake.err);
		}
		else {
			POM pom = getPOM();
			if (pom != null) {
				boolean isIJ1Plugin = isImageJ1Plugin(pom.getDirectory());
				final String subDirectory = isIJ1Plugin ? "plugins" : "jars";
				final File targetDirectory = new File(System.getProperty("ij.dir"), subDirectory);
				final File unversioned = new File(targetDirectory, pom.getArtifactId() + ".jar");
				if (unversioned.exists()) {
					unversioned.delete();
				}
				final File versioned = new File(targetDirectory, pom.getJarName());
				if (versioned.exists()) {
					versioned.delete();
				}
				try {
					pom.clean();
				} catch (Exception e) {
					e.printStackTrace(parser.fake.err);
				}
				return;
			}
		}
		File buildDir = getBuildDir();
		if (buildDir != null) {
			if (dry_run)
				parser.fake.out.println("rm -rf " + buildDir.getPath());
			else if (buildDir.exists())
				parser.fake.deleteRecursively(buildDir);
			return;
		}
	}

	/*
	 * During the Madison hackathon in February 2011, the submodules
	 * were moved from $PROJECT_ROOT/ into $PROJECT_ROOT/modules/
	 * as suggested by Albert Cardona.
	 *
	 * Check that the modules were moved correctly, offering an
	 * automatic move.
	 */
	protected void checkObsoleteLocation(String directory) throws FakeException {
		if (!directory.startsWith("modules/"))
			return;
		File submodule = new File(Util.makePath(parser.cwd, directory));
		if (submodule.isDirectory() && !Util.isDirEmpty(submodule.getAbsolutePath()))
			return;

		// check whether there is a directory in the obsolete location
		File oldSubmodule = new File(Util.makePath(parser.cwd, directory.substring("modules/".length())));
		if (!oldSubmodule.isDirectory())
			return;

		if (getVarBool("movesubmodules")) {
			if (submodule.exists() && !submodule.delete())
				throw new FakeException("Cannot delete submodule directory " + submodule.getAbsolutePath());
			submodule.getParentFile().mkdirs();
			if (!oldSubmodule.renameTo(submodule))
				throw new FakeException("Cannot move " + oldSubmodule.getAbsolutePath() + " to " + submodule.getAbsolutePath());
		}
		else
			throw new FakeException("Detected submodule in obsolete location: " + submodule.getAbsolutePath()
				+ "\nTo move submodules automatically, call Fiji Build again with moveSubmodules=true");
	}

	@Override
	public SubFake copy() {
		SubFake copy = new SubFake(parser, target, prerequisites);
		copy.prerequisiteString = prerequisiteString;
		return copy;
	}
}