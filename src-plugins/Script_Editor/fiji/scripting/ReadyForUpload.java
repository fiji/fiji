package fiji.scripting;

/**
 * This class checks several preconditions of .jar files before uploading:
 *
 * - the source files should be committed
 * - the latest commit touching the source files should be pushed
 * - Fiji Build should report it as up-to-date
 * - no stale leftover files from old versions
 * - no debug information in the .class files
 */

import fiji.SimpleExecuter;

import fiji.build.ByteCodeAnalyzer;
import fiji.build.CompileJar;
import fiji.build.Fake;
import fiji.build.FakeException;
import fiji.build.Parser;
import fiji.build.Rule;
import fiji.build.SubFake;
import fiji.build.Util;

import ij.IJ;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReadyForUpload {
	protected static String fijiDir;

	static {
		String dir = System.getProperty("fiji.dir");
		if (IJ.isWindows())
			dir = normalizeWinPath(dir);
		if (!dir.endsWith("/"))
			dir += "/";
		fijiDir = dir;
	}

	protected PrintStream out;
	protected Fake fake;
	protected Parser parser;
	protected Rule rule;

	public ReadyForUpload(PrintStream printStream) {
		out = printStream;
	}

	/* helper functions */
	protected void print(String message) {
		out.println(message);
	}

	/* Fiji Build helpers */
	protected void getRule(String path) throws FakeException, FileNotFoundException {
		path = new File(path).getAbsolutePath();
		if (IJ.isWindows())
			path = normalizeWinPath(path);
		if (!path.startsWith(fijiDir)) {
			print("Warning: Not in $FIJI_ROOT: " + path);
			return;
		}

		String target = path.substring(fijiDir.length());
		if (rule != null && rule.getTarget().equals(target))
			return;

		if (fake == null)
			fake = new Fake();
		if (parser == null) {
			parser = fake.parse(new FileInputStream(fijiDir + "/Fakefile"), new File(fijiDir));
			parser.parseRules(new ArrayList());
		}

		rule =  parser.getRule(target);
	}

	protected String getSourcePathForTarget(String path, boolean fromSubFakefile) throws FakeException, FileNotFoundException {
		getRule(path);
		if (rule == null) {
			print("Warning: No rule found for " + path);
			return null;
		}
		if (rule instanceof SubFake) {
			if (fromSubFakefile) {
				if (rule.getLastPrerequisite().equals("mpicbg/"))
					return fijiDir + "mpicbg/";
				File fakefile = ((SubFake)rule).getFakefile();
				if (fakefile == null)
					return null;
				Fake fake = new Fake();
				File cwd = new File(fijiDir, rule.getLastPrerequisite());
				if (!new File(cwd, ".git").exists()) {
					print(rule.getLastPrerequisite() + " not checked out");
					return null;
				}
				Parser parser = fake.parse(new FileInputStream(fakefile), cwd);
				parser.parseRules(new ArrayList());

				String target = rule.getTarget();
				target = target.substring(target.lastIndexOf('/') + 1);
				Rule subRule = parser.getRule(target);
				if (subRule == null) {
					print("Warning: rule for " + target + " not found in " + rule.getLastPrerequisite());
					return null;
				}
				if (!(subRule instanceof CompileJar)) {
					print("Warning: ignoring rule for " + target);
					return null;
				}

				String prereq = subRule.getPrerequisiteString();
				int star = prereq.indexOf("*");
				if (star >= 0)
					prereq = prereq.substring(0, star);

				return Util.makePath(cwd, prereq);
			}
			return rule.getLastPrerequisite();
		}
		if (!(rule instanceof CompileJar)) {
			print("Warning: ignoring rule for " + path);
			return null;
		}

		String prereq = rule.getPrerequisiteString();
		int starstar = prereq.indexOf("**");
		if (starstar >= 0)
			prereq = prereq.substring(0, starstar);

		return fijiDir + prereq;
	}

	protected boolean checkFakeTargetUpToDate(String path) throws FakeException, FileNotFoundException {
		getRule(path);
		if (rule == null) {
			print("Warning: could not determine target for " + path);
			return true; // ignore
		}
		return rule.upToDate();
	}

	protected boolean checkDirtyFiles(File directory, String fileName, String path) throws IOException {
		SimpleExecuter executer = new SimpleExecuter(directory, new String[] {
			"git", "ls-files", "--exclude-standard", "--other", "--modified", fileName
		});
		if (executer.getExitCode() != 0) {
			print("Failed looking for uncommitted/changed files for " + path + "\n" + executer.getError() + "\n" + executer.getOutput());
			return false;
		}
		if (!"".equals(executer.getOutput())) {
			print("Uncommitted files for " + path + ":\n" + executer.getOutput());
			return false;
		}
		return true;
	}

	protected boolean checkDirtyFiles(String path) throws FakeException, IOException {
		String fullPath = new File(path).getAbsolutePath();
		if (IJ.isWindows())
			fullPath = normalizeWinPath(fullPath);

		// check for uncommitted files
		String sourcePath = getSourcePathForTarget(fullPath, true);
		if (sourcePath == null)
			print("Warning: could not determine source directory for " + path);
		else if (!checkDirtyFiles(new File(sourcePath), ".", path))
			return false;

		// check whether submodules are correctly committed
		if (sourcePath == null)
			sourcePath = getSourcePathForTarget(fullPath, false);
		if (sourcePath != null && new File(sourcePath, ".git").exists()) {
			File dir = new File(sourcePath);
			if (!checkDirtyFiles(dir.getParentFile(), dir.getName(), path))
				return false;
		}

		return true;
	}

	protected String getSourceFileName(InputStream in) throws IOException {
		byte[] code = readStream(in);
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(code, true);
		return analyzer.getSourceFile();
	}

	protected long getMTime(String path) {
		return new File(path).lastModified();
	}

	protected boolean checkCRLF(InputStream input) throws IOException {
		byte[] buf = new byte[1024];
		boolean result = true;
		for (;;) {
			int count = input.read(buf);
			if (count <= 0)
				break;
			for (int i = 0; i < count; i++)
				if (buf[i] == 0) {
					// assume binary
					result = true;
					break;
				}
				else if (buf[i] == 13)
					// found carriage return
					result = false;
		}
		input.close();
		return result;
	}

	protected boolean checkCRLF(String path) throws IOException {
		boolean result = true;
		if (path.endsWith(".jar")) {
			ZipFile zip = new ZipFile(path);
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String name = entry.getName();
				if (!name.endsWith(".class") && !checkCRLF(zip.getInputStream(entry))) {
					print(name + " in " + path + " contains DOS-style line endings");
					result = false;
				}
			}
			zip.close();
		}
		else if (!checkCRLF(new FileInputStream(path))) {
			print(path + " contains DOS-style line endings");
			result = false;
		}
		return result;
	}

	protected boolean checkTimestamps(String path) throws FakeException, IOException {
		String fullPath = new File(path).getAbsolutePath();
		if (IJ.isWindows())
			fullPath = normalizeWinPath(fullPath);
		String sourcePath = getSourcePathForTarget(fullPath, true);
		if (sourcePath == null)
			return true; // ignore

		if (!sourcePath.endsWith(File.separator))
			sourcePath += File.separator;

		boolean result = true;
		ZipFile zip = new ZipFile(fullPath);
		for (ZipEntry entry : Collections.list(zip.entries())) {
			String name = entry.getName();
			if (name.startsWith("META-INF/"))
				continue;
			if (name.endsWith(".class")) {
				int dollar = name.indexOf('$');
				if (dollar >= 0)
					name = name.substring(0, dollar) + ".java";
				else
					name = name.substring(0, name.length() - 5) + "java";
				if (!new File(sourcePath + name).exists()) {
					String sourceName = getSourceFileName(zip.getInputStream(entry));
					if (sourceName == null) {
						print("No source file found for '" + sourcePath + entry.getName() + "'");
						continue;
					}
					name = name.substring(0, name.lastIndexOf('/') + 1) + sourceName;
				}
			}
			if (!new File(sourcePath + name).exists() &&
					(!path.endsWith("/Script_Editor.jar") ||
					 !new File(sourcePath + "templates/Java/" + name).exists()) &&
					(!path.endsWith("/ij.jar") ||
					 (!name.equals("aboutja.jpg") && !name.equals("icon.gif")))) {
				print(entry.getName() + " in " + path + " is missing the source " + name);
				result = false;
			}
			if (entry.getTime() < getMTime(sourcePath + name)) {
				print(entry.getName() + " in " + path + " is not up-to-date");
				result = false;
			}
		}
		zip.close();
		return result;
	}

	protected boolean checkPushed(String directory, String subdirectory, String path) throws IOException {
		String upstreamBranch = "master";
		if (subdirectory.endsWith("/"))
			subdirectory = subdirectory.substring(0, subdirectory.length() - 1);
		SimpleExecuter executer = new SimpleExecuter(new File(directory), new String[] {
			"git", "log", "--oneline", "origin/" + upstreamBranch + "..", "--", subdirectory == null ? "." : subdirectory
		});
		if (executer.getExitCode() != 0) {
			print("Failed looking for unpushed commits for " + path + "\n" + executer.getError() + "\n" + executer.getOutput());
			return false;
		}
		if (!"".equals(executer.getOutput())) {
			print("Unpushed commits for " + path + ":\n" + executer.getOutput());
			return false;
		}
		return true;
	}

	protected boolean checkPushed(String path) throws FakeException, IOException {
		String fullPath = new File(path).getAbsolutePath();
		if (IJ.isWindows())
			fullPath = normalizeWinPath(fullPath);
		String submodulePath = getSourcePathForTarget(fullPath, false);
		if (fullPath.startsWith(fijiDir) && !checkPushed(fijiDir, submodulePath, path))
			return false;

		// check whether submodules are pushed
		if (submodulePath != null && new File(submodulePath, ".git").exists() && !checkPushed(submodulePath, ".", path))
			return false;

		return true;
	}

	public boolean check(String path) {
		if (IJ.isWindows())
			path = normalizeWinPath(path);

		if (!path.endsWith(".jar")) {
			print("Warning: ignoring " + path);
			return true;
		}

		boolean result = true;
		rule = null;
		try {
			if (!checkTimestamps(path)) {
				print("");
				result = false;
			}
			if (!checkCRLF(path)) {
				print("");
				result = false;
			}
			if (containsDebugInfo(path)) {
				if (new File(path).getCanonicalPath().equals(new File(fijiDir, "plugins/loci_tools.jar").getCanonicalPath()))
					print("Ignoring debug info in Bio-Formats");
				else {
					print(path + " contains debug information");
					print("");
					result = false;
				}
			}

			if (!checkFakeTargetUpToDate(path)) {
				print(path + " is not up-to-date");
				print("");
				result = false;
			}
			if (!checkDirtyFiles(path))
				result = false;
			if (!checkPushed(path))
				result = false;
		} catch (Exception e) {
			e.printStackTrace(out);
			print("Error opening " + path + ": " + e);
			return false;
		}
		return result;
	}

	protected static String normalizeWinPath(String path) {
		try {
			return new File(path).getCanonicalPath().replace('\\', '/');
		} catch (IOException e) {
			return path;
		}
	}

	protected static byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[16384];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
		return out.toByteArray();
	}

	public static boolean containsDebugInfo(String filename) throws IOException {
		if (!filename.endsWith(".jar") || !new File(filename).exists())
			return false;

		final ZipFile jar = new ZipFile(filename);
		for (ZipEntry file : Collections.list(jar.entries())) {
			if (!file.getName().endsWith(".class"))
				continue;

			InputStream input = jar.getInputStream(file);
			byte[] code = readStream(input);
			ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(code, true);
			if (analyzer.containsDebugInfo())
				return true;
		}
		return false;
	}

	public static void main(String[] args) {
		if (args == null || args.length == 0 || (args.length == 1 && args[0].equals("")))
			args = new String[] { "plugins/Fiji_Plugins.jar" };

		ReadyForUpload checker = new ReadyForUpload(System.err);
		boolean result = true;
		for (String arg : args)
			if (!checker.check(arg))
				result = false;
		if (!result)
			checker.print("FAILED!");
	}
}