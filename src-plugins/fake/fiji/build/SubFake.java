package fiji.build;

import fiji.build.MiniMaven.Dependency;
import fiji.build.MiniMaven.POM;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

public class SubFake extends Rule {
	protected String jarName;
	protected String baseName;
	protected String source;
	protected String configPath;

	SubFake(Parser parser, String target, List<String> prerequisites) {
		super(parser, target, prerequisites);
		jarName = new File(target).getName();
		String directory = getLastPrerequisite();
		source = directory + jarName;
		baseName = Util.stripSuffix(jarName, ".jar");
		configPath = getPluginsConfig();

		String[] paths =
			Util.split(getVar("CLASSPATH"), ":");
		for (int i = 0; i < paths.length; i++)
			prerequisites.add(prerequisites.size() - 1, paths[i]);
		if (!new File(Util.makePath(parser.cwd, directory)).exists())
			parser.fake.err.println("Warning: " + directory
				+ " does not exist!");
	}

	@Override
	boolean checkUpToDate() {
		if (!upToDate(configPath))
			return false;
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
			if (pom != null)
				return pom.upToDate();

			if (!upToDateRecursive(new File(Util.makePath(parser.cwd, directory)), target, true))
				return false;
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

	public POM getPOM() {
		File file = new File(Util.makePath(parser.cwd, getLastPrerequisite()), "pom.xml");
		if (!file.exists())
			return null;
		String targetBasename = jarName.substring(jarName.lastIndexOf('/') + 1);
		if (targetBasename.endsWith(".jar"))
			targetBasename = targetBasename.substring(0, targetBasename.length() - 4);
		// TODO: targetBasename could end in "-<version>"
		try {
			POM pom = new MiniMaven(parser.fake, parser.fake.err, getVarBool("VERBOSE")).parse(file);
			if (targetBasename.equals(pom.getArtifact()))
				return pom;
			return pom.findPOM(new Dependency(null, targetBasename, null));
		} catch (Exception e) {
			e.printStackTrace(parser.fake.err);
			return null;
		}
	}

	void action() throws FakeException {
		String directory = getLastPrerequisite();
		checkObsoleteLocation(directory);

		if (getFakefile() != null || new File(directory, "Makefile").exists())
			fakeOrMake(jarName);
		else {
			POM pom = getPOM();
			if (pom != null) try {
				pom.downloadDependencies();
				pom.buildJar();
				copyJar(pom.getTarget().getPath(), target, parser.cwd, configPath);
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
}