package fiji.build;

import java.io.File;

import java.util.List;
import java.util.StringTokenizer;

public class SubFake extends Rule {
	String jarName;
	String baseName;
	String source;
	String configPath;

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

	boolean checkUpToDate() {
		if (!upToDate(configPath))
			return false;
		File target = new File(this.target);
		for (String directory : prerequisites)
			if (!checkUpToDate(directory, target))
				return false;
		return true;
	}

	boolean checkUpToDate(String directory, File target) {
		if (!target.exists())
			return false;

		File dir = new File(directory);
		if (!dir.exists() || (dir.isDirectory()) &&
				dir.listFiles().length == 0) {
			String precompiled =
				getVar("PRECOMPILEDDIRECTORY");
			if (precompiled == null)
				return true;
			File source = new File(parser.cwd, precompiled + "/" + jarName);
			return upToDate(source, target);
		} else if (!upToDateRecursive(dir, target))
			return false;
		return true;
	}

	public File getFakefile() {
		File file = new File(getLastPrerequisite(), "Fakefile");
		if (!file.exists())
			file = new File(getVar("PLUGINSCONFIGDIRECTORY"), baseName + ".Fakefile");
		return file.exists() ? file : null;
	}

	void action() throws FakeException {
		checkObsoleteLocation(getLastPrerequisite());

		for (String prereq : prerequisites)
			action(prereq);

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

	void action(String directory) throws FakeException {
		action(directory, jarName);
	}

	void action(String directory, String subTarget) throws FakeException {
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
			action(getLastPrerequisite(), jarName + "-clean"
				+ (dry_run ? "-dry-run" : ""));
		} catch (FakeException e) {
			e.printStackTrace(parser.fake.err);
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