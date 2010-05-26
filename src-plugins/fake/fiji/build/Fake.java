/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */
package fiji.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import java.util.zip.ZipException;

import java.util.regex.Pattern;

public class Fake {
	protected static Method javac;
	protected static String toolsPath;
	protected static String fijiBuildJar;
	protected static long mtimeFijiBuild;
	public PrintStream out = System.out, err = System.err;

	public static void main(String[] args) {
		if (runPrecompiledFakeIfNewer(args))
			return;
		new Fake().make(null, null, args);
	}

	public static boolean runPrecompiledFakeIfNewer(String[] args) {
		String url = Fake.class.getResource("Fake.class").toString();
		String prefix = "jar:file:";
		String suffix = "/fake.jar!/fiji/build/Fake.class";
		if (!url.startsWith(prefix) || !url.endsWith(suffix))
			return false;
		url = url.substring(9, url.length() - suffix.length());
		File precompiled = new File(url + "/precompiled/fake.jar");
		if (!precompiled.exists())
			return false;
		File current = new File(url + "/fake.jar");
		if (!current.exists() || current.lastModified() >=
				precompiled.lastModified())
			return false;
		System.err.println("Please copy the precompiled fake.jar "
			+ "over the current fake.jar; the latter is older!");
		System.exit(1);
		return true;
	}

	final static Set variableNames = new HashSet();

	public Fake() {
		variableNames.add("DEBUG");
		variableNames.add("JAVAVERSION");
		variableNames.add("SHOWDEPRECATION");
		variableNames.add("VERBOSE");
		variableNames.add("IGNOREMISSINGFAKEFILES");
		variableNames.add("CFLAGS");
		variableNames.add("CXXFLAGS");
		variableNames.add("LDFLAGS");
		variableNames.add("MAINCLASS");
		variableNames.add("INCLUDESOURCE");
		fijiHome = discoverFijiHome();
	}

	protected static String fijiHome;

	protected String discoverFijiHome() {
		URL url = getClass().getResource("Fake.class");
		String fijiHome = URLDecoder.decode(url.toString());
		if (getPlatform().startsWith("win"))
			fijiHome = fijiHome.replace('\\', '/');
		if (!fijiHome.endsWith("/Fake.class"))
			throw new RuntimeException("unexpected URL: " + url);
		fijiHome = fijiHome.substring(0, fijiHome.length() - 10);
		if (fijiHome.endsWith("/fiji/build/"))
			fijiHome = fijiHome.substring(0, fijiHome.length() - 11);
		int slash = fijiHome.lastIndexOf('/', fijiHome.length() - 2);
		if (fijiHome.startsWith("jar:file:") &&
				fijiHome.endsWith(".jar!/")) {
			fijiBuildJar = fijiHome.substring(9,
					fijiHome.length() - 2);
			mtimeFijiBuild = new File(fijiBuildJar).lastModified();
			fijiHome = fijiHome.substring(9, slash + 1);
		}
		else if (fijiHome.startsWith("file:/")) {
			fijiHome = fijiHome.substring(5, slash + 1);
			if (fijiHome.endsWith("/src-plugins/"))
				fijiHome = stripSuffix(fijiHome, "src-plugins/");
		}
		if (getPlatform().startsWith("win") && fijiHome.startsWith("/"))
			fijiHome = fijiHome.substring(1);
		if (fijiHome.endsWith("precompiled/"))
			fijiHome = fijiHome.substring(0,
					fijiHome.length() - 12);
		else if (fijiHome.endsWith("jars/"))
			fijiHome = fijiHome.substring(0,
					fijiHome.length() - 5);

		return fijiHome;
	}

	protected static void discoverJython() throws IOException {
		String pythonHome = fijiHome + "jars";
		System.setProperty("python.home", pythonHome);
		System.setProperty("python.cachedir.skip", "false");
		String jythonJar = pythonHome + "/jython.jar";
		if (!new File(jythonJar).exists())
			jythonJar = fijiHome + "/precompiled/jython.jar";
		getClassLoader(jythonJar);
	}

	protected static void discoverJavac() throws IOException {
		String path = fijiHome + "jars/javac.jar";
		if (!new File(path).exists())
			path = fijiHome + "precompiled/javac.jar";
		getClassLoader(path);
	}

	protected List discoverJars() throws FakeException {
		List jars = new ArrayList();
		File cwd = new File(".");
		/*
		 * Since View5D contains an ImageCanvas (d'oh!) which would
		 * be picked up instead of ImageJ's, we cannot blindly
		 * include all plugin's jars...
		 */
		// expandGlob(fijiHome + "plugins/**/*.jar", jars, cwd);
		expandGlob(fijiHome + "jars/**/*.jar", jars, cwd, 0, null);
		if (getPlatform().startsWith("win")) {
			String[] paths =
				split(System.getProperty("java.ext.dirs"),
						File.pathSeparator);
			for (int i = 0; i < paths.length; i++) {
				if (!new File(paths[i]).exists())
					continue;
				expandGlob(paths[i].replace('\\', '/')
						+ "/*.jar", jars, cwd, 0, null);
			}
		}

		return jars;
	}

	protected String discoverClassPath() throws FakeException {
		Iterator iter = discoverJars().iterator();
		String classPath = "";
		while (iter.hasNext())
			classPath += (classPath.equals("") ?
					"" : File.pathSeparator)
				+ iter.next();
		return classPath;
	}

	/* input defaults to reading the Fakefile, cwd to "." */
	public Parser parse(InputStream input, File cwd) throws FakeException {
		return new Parser(input, cwd);
	}

	/* input defaults to reading the Fakefile, cwd to "." */
	public void make(InputStream input, File cwd, String[] args) {
		try {
			Parser parser = parse(input, cwd);

			// filter out variable definitions
			int firstArg = 0;
			while (firstArg < args.length &&
					args[firstArg].indexOf('=') >= 0)
				firstArg++;

			List list = null;
			if (args.length > firstArg) {
				list = new ArrayList();
				for (int i = firstArg; i < args.length; i++)
					list.add(args[i]);
			}
			Parser.Rule all = parser.parseRules(list);

			for (int i = 0; i < firstArg; i++) {
				int equal = args[i].indexOf('=');
				parser.setVariable(args[i].substring(0, equal),
						args[i].substring(equal + 1));
			}

			all.make();

			/*
			 * By definition, everything is up-to-date now, but for
			 * performance, we set the mtimes so that we do not need
			 * to run our clever .jar checking again (which is
			 * quite expensive performance-wise, even if not as
			 * expensive as compiling everything again.
			 */
			all.setUpToDate();
		}
		catch (FakeException e) {
			System.err.println(e);
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	// the parser

	public class Parser {
		public final static String path = "Fakefile";
		protected long mtimeFakefile;
		BufferedReader reader;
		String line;
		int lineNumber;
		File cwd;
		protected Map allRules = new HashMap();
		protected Set allPrerequisites = new HashSet();
		protected Set allPlatforms;
		protected Rule allRule;
		protected String buildDir;

		public Parser() throws FakeException {
			this(null, null);
		}

		public Parser(String path) throws FakeException, IOException {
			this(new FileInputStream(path == null ||
				path.equals("") ? Parser.path : path), null);
			mtimeFakefile = new File(path).lastModified();
		}

		public Parser(InputStream stream, File cwd)
				throws FakeException {
			if (stream == null) try {
				stream = new FileInputStream(path);
			} catch (IOException e) {
				error("File read error: " + e + ": " + path);
			}
			InputStreamReader input = new InputStreamReader(stream);
			reader = new BufferedReader(input);

			lineNumber = 0;
			this.cwd = cwd != null ? cwd : new File(".");

			if (allPlatforms == null) {
				allPlatforms = new HashSet();
				allPlatforms.add("linux");
				allPlatforms.add("linux64");
				allPlatforms.add("win32");
				allPlatforms.add("win64");
				allPlatforms.add("macosx");
				allPlatforms.add("osx10.1");
				allPlatforms.add("osx10.2");
				allPlatforms.add("osx10.3");
				allPlatforms.add("osx10.4");
				allPlatforms.add("osx10.5");
				allPlatforms.add("osx10.6");
				Iterator iter = allPlatforms.iterator();
				while (iter.hasNext()) {
					String platform = (String)iter.next();
					setVariable("platform(" + platform
						+ ")", platform);
				}
			}

			setVariable("platform", getPlatform());

			setVariable("FIJIHOME", fijiHome);

			addSpecialRule(new Special("show-rules") {
				void action() { showMap(allRules, false); }
			});

			addSpecialRule(new Special("show-vars") {
				void action() { showMap(variables, true); }
			});

			addSpecialRule(new Special("clean") {
				void action() { cleanAll(false); }
			});

			addSpecialRule(new Special("clean-dry-run") {
				void action() { cleanAll(true); }
			});

			addSpecialRule(new Special("dry-run") {
				void action() { check(); }
			});

			addSpecialRule(new Special("check") {
				void action() { check(); }
			});
		}

		protected void showMap(Map map, boolean showKeys) {
			List list = new ArrayList(map.keySet());
			Collections.sort(list);
			Iterator iter = list.iterator();
			while (iter.hasNext()) {
				Object key = iter.next();
				out.println((showKeys ?
						key.toString() + " = " : "")
					+ map.get(key));
			}
		}

		protected void cleanAll(boolean dry_run) {
			Iterator iter = allRules.keySet().iterator();
			while (iter.hasNext()) {
				Rule rule = (Rule)allRules.get(iter.next());
				rule.clean(dry_run);
			}
		}

		protected void check() {
			List list = new ArrayList(allRules.keySet());
			Collections.sort(list);
			Iterator iter = list.iterator();
			while (iter.hasNext()) {
				Rule rule = (Rule)allRules.get(iter.next());
				if (rule instanceof All)
					continue;
				if (rule instanceof Special)
					continue;
				if (rule instanceof SubFake) {
					out.println("Subfake '"
						+ rule.getLastPrerequisite()
						+ "' would make '"
						+ rule.target + "'");
					continue;
				}
				if (rule.upToDate())
					continue;
				if (rule instanceof ExecuteProgram) {
					String program =
						((ExecuteProgram)rule).program;
					if (program.equals(""))
						continue;
					out.println("Program '" + program
						+ "' would maybe make '"
						+ rule.target + "'");
					continue;
				}
				out.println("'" + rule.target
						+ "' is not up-to-date");
			}
		}


		public Rule parseRules(List targets) throws FakeException {
			Rule result = null;

			for (;;) {
				try {
					line = reader.readLine();
				} catch (IOException e) {
					error("Error reading file");
				}

				if (line == null)
					break;

				lineNumber++;
				line = line.trim();

				if (line.length() == 0 || line.startsWith("#"))
					continue;

				while (line.endsWith("\\"))
					try {
						String next = reader.readLine();
						lineNumber++;
						line = line.substring(0,
							line.length() - 1)
							+ next;
					} catch (IOException e) {
						error("Error reading file");
					}

				int arrow = line.indexOf("<-");
				if (arrow < 0) {
					int equal = line.indexOf('=');
					if (equal < 0)
						error("Invalid line");
					String key = line.substring(0, equal);
					String val = line.substring(equal + 1);
					setVariable(key.trim(), val.trim());
					continue;
				}

				String target = line.substring(0, arrow).trim();
				int bracket = target.endsWith("]") ?
					target.indexOf('[') : -1;
				target = bracket < 0 ?
					expandVariables(target) :
					expandVariables(target.substring(0,
								bracket)) +
						target.substring(bracket);

				String list = line.substring(arrow + 2).trim();
				try {
					addRule(target, list);
				} catch (Exception e) {
					error(e.getMessage());
				}
			}

			lineNumber = -1;

			result = allRule;
			if (result == null)
				error("Could not find default rule");

			checkVariableNames();

			if (targets != null)
				return new All("", targets);

			return result;
		}

		public Rule addRule(String target, String prerequisites)
				throws FakeException {
			Rule rule = null;

			if (target.indexOf('*') >= 0) {
				int bracket = target.indexOf('[');
				String program = "";
				if (bracket >= 0) {
					program = target.substring(bracket);
					target = target.substring(0, bracket);
				}
				GlobFilter filter = new GlobFilter(target);
				Iterator iter = new ArrayList(allPrerequisites)
					.iterator();
				while (iter.hasNext()) {
					target = (String)iter.next();
					if (allRules.containsKey(target))
						continue;
					if (!filter.accept(null, target))
						continue;
					rule = addRule(target
						+ filter.replace(program),
						filter.replace(prerequisites));
				}
				return rule;
			}

			List list = new ArrayList();
			StringTokenizer tokenizer = new
				StringTokenizer(expandVariables(prerequisites,
							target), " \t\n");

			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (expandGlob(token, list, cwd, 0, buildDir)
						+ addMatchingTargets(token, list)
						== 0)
					throw new FakeException("Glob did not "
						+ "match any file: '"
						+ token + "'");
			}

			String lastPrereq = list.size() == 0 ? null :
				(String)list.get(list.size() - 1);

			if (allRule == null)
				rule = allRule = new All(target, list);
			else if (target.endsWith("]")) {
				int paren = target.indexOf('[');

				if (paren < 0)
					throw new FakeException("Invalid rule");

				String program = target.substring(paren + 1,
					target.length() - 1);
				target = target.substring(0, paren).trim();

				rule = new ExecuteProgram(target, list,
					program);
			}
			else if (isSubmodule(lastPrereq))
				rule = new SubFake(target, list);
			else if (target.endsWith(".jar")) {
				if (expandVariables(prerequisites, target)
						.endsWith(".jar"))
					rule = new CopyJar(target, list);
				else
					rule = new CompileJar(target, list);
			}
			else if (prerequisites.endsWith(".c") ||
					prerequisites.endsWith(".cxx"))
				rule = new CompileCProgram(target, list);
			else if (target.endsWith(".class"))
				rule = new CompileClass(target, list);
			if (rule == null)
				throw new FakeException("Unrecognized rule");

			rule.prerequisiteString = prerequisites;

			allRules.put(target, rule);

			Iterator iter = list.iterator();
			while (iter.hasNext())
				allPrerequisites.add(iter.next());

			return rule;
		}

		boolean isSubmodule(String directory) {
			if (directory == null)
				return false;
			File dir = new File(makePath(cwd, directory));
			return dir.isDirectory() ||
				(!dir.exists() && directory.endsWith("/") &&
				 allRules.get(stripSuffix(directory, "/")) == null);
		}

		int addMatchingTargets(String glob, List sortedPrereqs) {
			if (glob.indexOf('*') < 0)
				return 0;
			int count = 0;
			GlobFilter filter = new GlobFilter(glob);
			Iterator iter = allRules.keySet().iterator();
			while (iter.hasNext()) {
				String target = (String)iter.next();
				Rule rule = (Rule)allRules.get(target);
				if (rule instanceof Special || rule instanceof All)
					continue;
				if (!filter.accept(null, target))
					continue;
				int index = Collections
					.binarySearch(sortedPrereqs, target);
				if (index >= 0)
					continue;
				sortedPrereqs.add(-1 - index, target);
				count++;
			}
			return count;
		}

		protected void addSpecialRule(Special rule) {
			allRules.put(rule.target, rule);
		}

		protected void error(String message) throws FakeException {
			if (lineNumber < 0)
				throw new FakeException(path + ":" + message);
			throw new FakeException(path + ":" + lineNumber + ": "
					+ message + "\n\t" + line);
		}

		// the variables

		protected Map variables = new HashMap();

		public int getClosingParenthesis(String value, int offset) {
			char closing;
			switch (value.charAt(offset)) {
			case '(': closing = ')'; break;
			case '[': closing = ']'; break;
			case '{': closing = '}'; break;
			default: return -1;
			}
			while (++offset < value.length())
				if (value.charAt(offset) == closing)
					return offset;
			return -1;
		}

		public int getVariableNameEnd(String value, int offset) {
			while (offset < value.length()) {
				char c = value.charAt(offset);
				if (c == '(') {
					int end = getClosingParenthesis(value,
						offset);
					if (end < 0)
						return offset;
					return end + 1;
				}
				if (!isVarChar(value.charAt(offset)))
					return offset;
				offset++;
			}
			return offset;
		}

		/*
		 * This handles
		 *
		 * 	$SOMETHING(*) = $SOME $THINK $ELSE
		 *
		 * by searching for all available subkeys of $SOME, $THING
		 * and $ELSE, and setting $SOMETHING(subkey) for all of them.
		 */
		public void setVariableWildcard(String key, String value)
				throws FakeException {
			/* get all variable names */
			int offset = 0;
			Set variableNames = new HashSet();
			for (;;) {
				int dollar = value.indexOf('$', offset);
				if (dollar < 0)
					break;
				offset = getVariableNameEnd(value, dollar + 1);
				variableNames.add(value.substring(dollar + 1,
							offset).toUpperCase());
			}
			key = key.toUpperCase();
			Set subkeys = new HashSet();
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String var = (String)iter.next();
				int paren = var.indexOf('(');
				if (paren < 0)
					continue;
				String name = var.substring(0, paren);
				if (!variableNames.contains(name))
					continue;
				subkeys.add(var.substring(paren));
			}
			/* 3rd loop to avoid concurrent modification */
			iter = subkeys.iterator();
			while (iter.hasNext())
				setVariable(key + iter.next(), value);
		}

		public void setVariable(String key, String value)
				throws FakeException {
			int paren = key.indexOf('(');
			String name = (paren < 0 ? key :
				key.substring(0, paren)).toUpperCase();

			if (key.charAt(paren + 1) == '*') {
				setVariableWildcard(name, value);
				return;
			}

			if (name.equals("CLASSPATH"))
				value = prefixPaths(cwd, value, true);

			value = expandVariables(value, paren < 0 ? null :
				key.substring(paren + 1, key.length() - 1));

			if (value.indexOf('*') >= 0 ||
					value.indexOf('?') >= 0) {
				String separator = name.equals("CLASSPATH") ?
					":" : " ";
				List files = new ArrayList();
				StringTokenizer tokenizer = new
					StringTokenizer(value.replace('\t',
							' '), separator);
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (expandGlob(token, files, cwd, 0,
								buildDir) < 1)
						err.println("Warning: "
							+ "no match for "
							+ token);
				}
				value = "";
				if (separator.equals(":"))
					separator = File.separator;
				Iterator iter = files.iterator();
				while (iter.hasNext())
					value += separator +
						quoteArg((String)iter.next());
			}

			name = name.toUpperCase() + (paren < 0 ?
				"" : key.substring(paren));
			variables.put(name, value);
			if (name.equals("BUILDDIR"))
				buildDir = value;
		}

		public String expandVariables(String value) {
			return expandVariables(value, null, null);
		}

		public String expandVariables(String value, String subkey) {
			return expandVariables(value, subkey, null);
		}

		public String expandVariables(String value,
				String subkey, String subkey2) {
			int offset = 0;
			for (;;) {
				int dollar = value.indexOf('$', offset);
				if (dollar < 0)
					return value;

				int end = getVariableNameEnd(value, dollar + 1);
				String name = value.substring(dollar + 1, end);
				int paren = name.indexOf('(');
				String substitute;
				if (paren < 0)
					substitute =
						getVariable(name.toUpperCase(),
						subkey, subkey2);
				else
					substitute = (String)variables.get(
					name.substring(0, paren).toUpperCase()
					+ name.substring(paren));
				if (substitute == null)
					substitute = "";
				value = value.substring(0, dollar)
					+ substitute
					+ (end < value.length() ?
						value.substring(end) : "");
				offset = dollar + substitute.length();
			}
		}

		public boolean isVarChar(char c) {
			return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || c == '_';
		}

		public void checkVariableNames() throws FakeException {
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String)iter.next();
				int paren = key.indexOf('(');
				if (paren < 0 || !key.endsWith(")") ||
						key.startsWith("ENVOVERRIDES("))
					continue;
				String name = key.substring(paren + 1,
						key.length() - 1);
				if (!allPrerequisites.contains(name) &&
						!allRules.containsKey(name) &&
						!allPlatforms.contains(name))
					throw new FakeException("Invalid target"
						+ " for variable " + key);
			}
		}

		public String getVariable(String key) {
			return getVariable(key, null, null);
		}

		public String getVariable(String key, String subkey) {
			return getVariable(key, subkey, null);
		}

		public String getVariable(String key,
				String subkey, String subkey2) {
			String res = null;
			if ("true".equals(variables.get("ENVOVERRIDES("
							+ key + ")")))
				res = stripFijiHome(System.getenv(key));
			key = key.toUpperCase();
			if (subkey != null && res == null)
				res = (String)variables.get(key
						+ "(" + subkey + ")");
			if (subkey2 != null && res == null)
				res = (String)variables.get(key
						+ "(" + subkey2 + ")");
			if (res == null && getPlatform().equals("macosx")) {
				String version =
					System.getProperty("os.version");
				int i = 0;
				if (version.startsWith("10.")) {
					int dot = version.indexOf('.', 3);
					if (dot > 0) {
						version = version.substring(3,
							dot);
						i = Integer.parseInt(version);
					}
				}
				while (i > 0 && res == null)
					res = (String)variables.get(key
						+ "(osx10." + (i--) + ")");
			}
			if (res == null)
				res = (String)variables.get(key
						+ "(" + getPlatform() + ")");
			if (res == null)
				res = (String)variables.get(key);
			return res;
		}

		public void dumpVariables() {
			err.println("Variable dump:");
			Iterator iter = variables.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String)iter.next();
				err.println(key + " = "
						+ variables.get(key));
			}
		}

		public boolean getBool(String string) {
			return string != null &&
				(string.equalsIgnoreCase("true") ||
				 string.equals("1") || string.equals("2"));
		}

		public void missingPrecompiledFallBack(String target)
				throws FakeException {
			Rule fallBack = getRule("missingPrecompiledFallBack");
			if (fallBack == null)
				throw new FakeException("No precompiled and "
					+ "no fallback for " + target + "!");
			synchronized(fallBack) {
				String save = fallBack.target;
				fallBack.target = target;
				fallBack.make();
				fallBack.target = save;
			}
		}

		public Rule getRule(String rule) {
			return (Rule)allRules.get(rule);
		}

		public Map getAllRules() {
			return allRules;
		}

		// the different rule types

		public abstract class Rule {
			protected String target;
			protected String prerequisiteString;
			protected List prerequisites, nonUpToDates;
			protected boolean wasAlreadyInvoked;
			protected boolean wasAlreadyChecked;

			/*
			 * 0 means upToDate() was not yet run,
			 * 1 means upToDate() is in the process of being run,
			 * 2 means upToDate() returns true
			 * 3 means upToDate() returns false
			 */
			protected int upToDateStage;

			Rule(String target, List prerequisites) {
				this.target = target;
				this.prerequisites = prerequisites;
				try {
					String name = "TARGET(" + target + ")";
					if (!variables.containsKey(name))
						setVariable(name, target);
					name = "PRE(" + target + ")";
					if (!variables.containsKey(name))
						setVariable(name,
							join(prerequisites));
				} catch (FakeException e) { /* ignore */ }
			}

			abstract void action() throws FakeException;

			final boolean upToDate() {
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
				File file = new File(makePath(cwd, target));
				if (!file.exists()) {
					nonUpToDates = prerequisites;
					return false;
				}
				long targetModifiedTime = file.lastModified();

				if (getVarBool("rebuildIfFakeIsNewer")) {
					if (targetModifiedTime < mtimeFakefile)
						return upToDateError(file,
								new File(path));
					if (targetModifiedTime < mtimeFijiBuild)
						return upToDateError(new File(fijiBuildJar), file);
				}

				nonUpToDates = new ArrayList();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					String path = makePath(cwd, prereq);
					if (new File(path).lastModified()
							> targetModifiedTime)
						nonUpToDates.add(prereq);
				}

				return nonUpToDates.size() == 0;
			}

			boolean upToDate(String path) {
				if (path == null)
					return true;
				return upToDate(new File(path),
					new File(cwd, target));
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
						compare(source, target) == 0)
					return true;
				if (targetModified < sourceModified)
					return upToDateError(source, target);
				return true;
			}

			boolean upToDate(String source, String target,
					File cwd) {
				return upToDate(new File(cwd, source),
					new File(cwd, target));
			}

			boolean upToDateError(File source, File target) {
				verbose("" + target + " is not up-to-date "
					+ "because " + source + " is newer.");
				return false;
			}

			boolean upToDateRecursive(File source, File target) {
				if (!source.exists())
					return true;
				if (!source.isDirectory())
					return upToDate(source, target);
				String[] entries = source.list();
				for (int i = 0; i < entries.length; i++) {
					if (entries[i].startsWith("."))
						continue;
					File file = new File(source,
							entries[i]);
					if (!upToDateRecursive(file, target))
						return false;
				}
				return true;
			}

			public void make() throws FakeException {
				if (wasAlreadyChecked)
					return;
				wasAlreadyChecked = true;
				if (wasAlreadyInvoked)
					error("Dependency cycle detected!");
				wasAlreadyInvoked = true;
				try {
					verbose("Checking prerequisites of "
						+ this);
					makePrerequisites();

					if (upToDate())
						return;
					err.println("Building " + this);
					action();
					if (new File(target).exists())
						upToDateStage = 2;
					else {
						upToDateStage = 0;
						wasAlreadyChecked = false;
					}
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
					new File(target).delete();
					error(e.getMessage());
				}
				wasAlreadyInvoked = false;
			}

			void setUpToDate() throws IOException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					Rule rule =
						getRule((String)iter.next());
					if (rule != null && rule != this)
						rule.setUpToDate();
				}

				if (!checkUpToDate())
					touchFile(target);
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
						out.println("rm "
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
				err.println(message);
			}

			protected void verbose(String message) {
				if (!getVarBool("VERBOSE"))
					return;
				err.println(message);
			}

			public Rule getRule(String prereq) {
				if (prereq.endsWith(".jar/"))
					prereq = stripSuffix(prereq, "/");
				return (Rule)allRules.get(prereq);
			}

			public void makePrerequisites() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					Rule rule = getRule(prereq);
					if (rule == null) {
						if (this instanceof All)
							error("Unknown target: "
								+ prereq);
						else
							continue;
					}
					rule.make();
				}
			}

			public String getLastPrerequisite() {
				int index = prerequisites.size() - 1;
				return (String)prerequisites.get(index);
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
				result += target + " <-";
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					result += " " + iter.next();
				if (maxCharacters > 0 && result.length()
						> maxCharacters)
					result = result.substring(0,
						Math.max(0, maxCharacters - 3))
						+ "...";
				return result;
			}

			public String getVar(String key) {
				return getVariable(key, target);
			}

			public String getVar(String key, String subkey) {
				return getVariable(key, subkey, target);
			}

			public boolean getVarBool(String key) {
				return getBool(getVariable(key, target));
			}

			public boolean getVarBool(String key, String subkey) {
				return getBool(getVariable(key,
							subkey, target));
			}

			public File getBuildDir() {
				String dir = getVar("builddir");
				if (dir == null || dir.equals(""))
					return null;
				return new File(makePath(cwd, dir + "/"
					+ stripSuffix(stripSuffix(target,
						".class"), ".jar")));
			}

			List compileJavas(List javas, File buildDir,
					Set exclude, Set noCompile)
					throws FakeException {
				toolsPath = getVar("TOOLSPATH");
				return Fake.this.compileJavas(javas, cwd, buildDir,
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
				String key = target;
				if (key.endsWith(".jar"))
					key = key.substring(0,
							key.length() - 4);
				int slash = key.lastIndexOf('/');
				if (slash >= 0)
					key = key.substring(slash + 1);
				path += "/" + key + ".config";
				if (!new File(path).exists())
					return null;
				return path;
			}

			/* Copy source to target if target is not up-to-date */
			void copyJar(String source, String target, File cwd,
					String configPath)
					throws FakeException {
				if (configPath == null) {
					if (upToDate(source, target, cwd))
						return;
					copyFile(source, target, cwd);
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
				if (jarUpToDate(source, target,
						getVarBool("VERBOSE"))) {
					if (upToDate(configPath)) {
						touchFile(target);
						return;
					}
					verbose(source + " is not up-to-date "
						+ " because of " + configPath);
				}

				File file = new File(cwd, source);
				InputStream input = new FileInputStream(file);
				JarInputStream in = new JarInputStream(input);

				Manifest manifest = in.getManifest();
				file = new File(cwd, target);
				OutputStream output =
					new FileOutputStream(file);
				JarOutputStream out = manifest == null ?
					new JarOutputStream(output) :
					new JarOutputStream(output, manifest);

				addPluginsConfigToJar(out, configPath);

				JarEntry entry;
				while ((entry = in.getNextJarEntry()) != null) {
					String name = entry.getName();
					if (name.equals("plugins.config") &&
							configPath != null) {
						in.closeEntry();
						continue;
					}
					byte[] buf = readStream(in);
					in.closeEntry();
					entry.setCompressedSize(-1);
					writeJarEntry(entry, out, buf);
				}
				in.close();
				out.close();
			}

			public String getPrerequisiteString() {
				return prerequisiteString;
			}
		}

		class All extends Rule {
			All(String target, List prerequisites) {
				super(target, prerequisites);
			}

			public void action() throws FakeException {
			}

			public boolean checkUpToDate() {
				return false;
			}
		}

		abstract class Special extends Rule {
			Special(String target) {
				super(target, new ArrayList());
			}

			boolean checkUpToDate() {
				return false;
			}
		}

		class SubFake extends Rule {
			String jarName;
			String baseName;
			String source;
			String configPath;

			SubFake(String target, List prerequisites) {
				super(target, prerequisites);
				jarName = new File(target).getName();
				String directory = getLastPrerequisite();
				source = directory + jarName;
				baseName = stripSuffix(jarName, ".jar");
				configPath = getPluginsConfig();

				String[] paths =
					split(getVar("CLASSPATH"), ":");
				for (int i = 0; i < paths.length; i++)
					prerequisites.add(paths[i]);
				if (!new File(makePath(cwd, directory)).exists())
					err.println("Warning: " + directory
						+ " does not exist!");
			}

			boolean checkUpToDate() {
				if (!upToDate(configPath))
					return false;
				File target = new File(this.target);
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String directory = (String)iter.next();
					if (!checkUpToDate(directory, target))
						return false;
				}
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
					File source = new File(cwd,
						precompiled + "/" + jarName);
					return upToDate(source, target);
				} else if (!upToDateRecursive(dir, target))
					return false;
				return true;
			}

			void action() throws FakeException {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext())
					action((String)iter.next());

				File file = new File(makePath(cwd, source));
				if (getVarBool("IGNOREMISSINGFAKEFILES") &&
						!file.exists() &&
						isDirEmpty(getLastPrerequisite())) {
					String precompiled =
						getVar("PRECOMPILEDDIRECTORY");
					if (precompiled == null)
						return;
					source = precompiled + file.getName();
					if (!new File(makePath(cwd,
							source)).exists()) {
						missingPrecompiledFallBack(target);
						return;
					}
				}
				else if (!file.exists())
					error("Target " + target + " was not built!");

				if (target.indexOf('.') >= 0)
					copyJar(source, target, cwd, configPath);
			}

			void action(String directory) throws FakeException {
				fakeOrMake(cwd, directory,
					getVarBool("VERBOSE", directory),
					getVarBool("IGNOREMISSINGFAKEFILES",
						directory),
					getVarPath("TOOLSPATH", directory),
					getVarPath("CLASSPATH", directory),
					getVar("PLUGINSCONFIGDIRECTORY")
						+ "/" + baseName + ".Fakefile",
					getBuildDir(),
					jarName);
			}

			String getVarPath(String variable, String subkey) {
				String value = getVar(variable, subkey);
				if (value == null)
					return null;

				String result = "";
				StringTokenizer tokenizer =
					new StringTokenizer(value,
							File.pathSeparator);
				while (tokenizer.hasMoreElements()) {
					if (!result.equals(""))
						result += File.pathSeparator;
					result += tokenizer.nextToken();
				}
				return result;
			}

			protected void clean(boolean dry_run) {
				File buildDir = getBuildDir();
				if (buildDir == null) {
					super.clean(dry_run);
					return;
				}
				if (dry_run)
					out.println("rm -rf "
							+ buildDir.getPath());
				else if (buildDir.exists())
					deleteRecursively(buildDir);
			}
		}

		class CopyJar extends Rule {
			String source, configPath;
			CopyJar(String target, List prerequisites) {
				super(target, prerequisites);
				source = getLastPrerequisite();
				configPath = getPluginsConfig();
			}

			void action() throws FakeException {
				copyJar(source, target, cwd, configPath);
			}

			boolean checkUpToDate() {
				if (super.checkUpToDate() &&
						upToDate(configPath))
					return true;

				return jarUpToDate(source, target,
					getVarBool("VERBOSE"));
			}
		}

		class CompileJar extends Rule {
			String configPath;
			String classPath;

			CompileJar(String target, List prerequisites) {
				super(target, uniq(prerequisites));
				configPath = getPluginsConfig();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					if (!prereq.endsWith(".jar/"))
						continue;
					prereq = stripSuffix(prereq, "/");
					if (classPath == null)
						classPath = prereq;
					else
						classPath += ":" + prereq;
				}
			}

			public String getVar(String var) {
				String value = super.getVar(var);
				if (var.toUpperCase().equals("CLASSPATH")) {
					if( classPath != null ) {
						return (value == null) ? classPath
							: (value + ":" + classPath);
					}
				}
				return value;
			}

			void action() throws FakeException {
				// check the classpath
				String[] paths =
					split(getVar("CLASSPATH"), ":");
				for (int i = 0; i < paths.length; i++)
					maybeMake((Rule)allRules.get(paths[i]));

				File buildDir = getBuildDir();
				Set noCompile =
					expandToSet(getVar("NO_COMPILE"), cwd);
				Set exclude =
					expandToSet(getVar("EXCLUDE"), cwd);
				compileJavas(prerequisites, buildDir, exclude,
					noCompile);
				List files = java2classFiles(prerequisites,
					cwd, buildDir, exclude, noCompile);
				if (getVarBool("includeSource"))
					addSources(files);
				makeJar(target, getMainClass(), files, cwd,
					buildDir, configPath, getStripPath(),
					getVarBool("VERBOSE"));
			}

			void addSources(List files) {
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String file = (String)iter.next();
					if (file.endsWith(".java"))
						files.add(file);
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
				verbose("" + this
					+ " is not up-to-date because of "
					+ reason);
				return false;
			}

			boolean checkUpToDate() {
				// handle xyz[from/here] targets
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String path = (String)iter.next();
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
				String[] paths = split(getVar("CLASSPATH"),
						File.pathSeparator);
				for (int i = 0; i < paths.length; i++) {
					if (!paths[i].equals(".") &&
							!upToDate(paths[i]))
						return notUpToDate(paths[i]);
					Rule rule = (Rule)allRules.get(paths[i]);
					if (rule != null && !rule.upToDate())
						return notUpToDate("" + rule);
				}
				return super.checkUpToDate() &&
					upToDate(configPath);
			}

			String getMainClass() {
				return getVariable("MAINCLASS", target);
			}

			String getStripPath() {
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

			protected void clean(boolean dry_run) {
				super.clean(dry_run);
				File buildDir = getBuildDir();
				if (buildDir != null) {
					if (dry_run)
						out.println("rm -rf "
							+ buildDir.getPath());
					else if (buildDir.exists())
						deleteRecursively(buildDir);
					return;
				}
				List javas = new ArrayList();
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String path = (String)iter.next();
					if (path.endsWith(".java"))
						javas.add(path);
				}

				try {
					Set exclude = expandToSet(
						getVar("EXCLUDE"), cwd);
					Set noCompile = expandToSet(
						getVar("NO_COMPILE"), cwd);
					exclude.addAll(noCompile);
					iter = java2classFiles(javas,
						cwd, getBuildDir(),
						exclude, noCompile).iterator();
				} catch (FakeException e) {
					err.println("Warning: could not "
						+ "find required .class files: "
						+ this);
					return;
				}
				while (iter.hasNext())
					clean((String)iter.next(), dry_run);
			}
		}

		class CompileClass extends Rule {
			CompileClass(String target, List prerequisites) {
				super(target, prerequisites);
			}

			void action() throws FakeException {
				compileJavas(prerequisites, getBuildDir(),
					new HashSet(), new HashSet());

				// copy class files, if necessary
				int slash = target.lastIndexOf('/') + 1;
				String destPrefix = target.substring(0, slash);

				String prefix = getLastPrerequisite();
				slash = prefix.lastIndexOf('/') + 1;
				prefix = prefix.substring(0, slash);

				Set exclude = expandToSet(getVar("NO_COMPILE"),
					cwd);
				Iterator iter = java2classFiles(prerequisites,
					cwd, getBuildDir(),
					exclude, new HashSet()).iterator();
				while (iter.hasNext()) {
					String source = (String)iter.next();
					if (!source.startsWith(prefix))
						continue;
					int slash2 = source.lastIndexOf('/');
					copyFile(source, destPrefix +
						source.substring(slash2), cwd);
				}
			}
		}

		class CompileCProgram extends Rule {
			boolean linkCPlusPlus = false;

			CompileCProgram(String target, List prerequisites) {
				super(target, prerequisites);
				if (getPlatform().startsWith("win"))
					this.target += ".exe";
			}

			void action() throws FakeException {
				try {
					action(prerequisites.iterator());
				} catch (IOException e) {
					fallBackToPrecompiled(e.getMessage());
				}
			}

			void action(Iterator iter)
					throws IOException, FakeException {
				List out = new ArrayList();
				while (iter.hasNext()) {
					String path = (String)iter.next();
					if (path.endsWith(".c")) {
						out.add(compileC(path));
					}
					else if (path.endsWith(".cxx"))
						out.add(compileCXX(path));
					else
						throw new FakeException("Cannot"
							+ " compile " + path);
				}
				link(target, out);
			}

			void addFlags(String variable, String path,
					List arguments) throws FakeException {
				String value = getVariable(variable,
						path, target);
				arguments.addAll(splitCommandLine(value));
			}

			String compileCXX(String path)
					throws IOException, FakeException {
				linkCPlusPlus = true;
				return compile(path, gxx(), "CXXFLAGS");
			}

			String compileC(String path)
					throws IOException, FakeException {
				return compile(path, gcc(), "CFLAGS");
			}

			String compile(String path, String compiler,
					String flags)
					throws IOException, FakeException {
				List arguments = new ArrayList();
				arguments.add(compiler);
				if (getVarBool("DEBUG"))
					arguments.add("-g");
				arguments.add("-c");
				addFlags(flags, path, arguments);
				arguments.add(path);
				try {
					execute(arguments, path,
						getVarBool("VERBOSE", path));
					return path.substring(0,
						path.length() - (compiler.endsWith("++") ? 4 : 2)) + ".o";
				} catch(FakeException e) {
					return error("compile", path, e);
				}
			}

			void link(String target, List objects)
					throws FakeException {
				File file = new File(target);
				try {
					moveFileOutOfTheWay(file);
				} catch(FakeException e) {
					file = moveToUpdateDirectory(file);
				}
				List arguments = new ArrayList();
				arguments.add(linkCPlusPlus ? gxx() : gcc());
				arguments.add("-o");
				arguments.add(file.getAbsolutePath());
				addFlags("LDFLAGS", target, arguments);
				arguments.addAll(objects);
				addFlags("LIBS", target, arguments);
				try {
					execute(arguments, target,
						getVarBool("VERBOSE", path));
				} catch(Exception e) {
					error("link", target, e);
				}
			}

			String gcc() {
				return getenv("CC", "gcc");
			}

			String gxx() {
				return getenv("CXX", "g++");
			}

			String getenv(String key, String fallback) {
				String value = System.getenv(key);
				return value == null ? fallback : value;
			}

			void fallBackToPrecompiled(String reason)
					throws FakeException {
				String precompiled =
					getVar("PRECOMPILEDDIRECTORY");
				if (precompiled == null)
					error(reason);
				err.println("Falling back to copying "
					+ target + " from " + precompiled);
				File file = new File(makePath(cwd, target));
				if (!precompiled.endsWith("/"))
					precompiled += "/";
				String source =
					precompiled + file.getName();
				if (!new File(source).exists()) {
					if (getPlatform().startsWith("win")) {
						int len = source.length();
						source = source.substring(0,
								len - 4) +
							"-" + getPlatform() +
							".exe";
					}
					else
						source += "-" + getPlatform();
				}
				moveFileOutOfTheWay(makePath(cwd, target));
				copyFile(source, target, cwd);
				if (!getPlatform().startsWith("win")) try {
					/* avoid Java6-ism */
					Runtime.getRuntime().exec(new String[]
						{ "chmod", "0755", target});
				} catch (Exception e) { /* ignore */ }
			}

			String error(String action, String file, Exception e)
					throws FakeException {
				throw new FakeException("Could not " + action
					+ " " + file + ": " + e);
			}
		}

		class ExecuteProgram extends Rule {
			String program;

			ExecuteProgram(String target, List prerequisites,
					String program) {
				super(target, prerequisites);
				this.program = program;
			}

			boolean checkUpToDate() {
				boolean result = super.checkUpToDate();

				if (!result)
					return result;

				/*
				 * Ignore prerequisites if none of them
				 * exist as files.
				 */
				Iterator iter = prerequisites.iterator();
				while (iter.hasNext()) {
					String prereq = (String)iter.next();
					if (new File(makePath(cwd,
							prereq)).exists())
						return true;
				}

				// special-case ant, since it's slow
				if (program.startsWith("../fiji --ant") &&
						prerequisites.size() == 0 &&
						upToDateRecursive(cwd,
							new File(target)))
					return true;

				return false;
			}

			void action() throws FakeException {
				if (program.equals(""))
					return;
				try {
					String expanded =
						expandVariables(program,
							target);
					execute(splitCommandLine(expanded), cwd,
						getVarBool("VERBOSE", program));
				} catch (Exception e) {
					if (!(e instanceof FakeException))
						e.printStackTrace();
					throw new FakeException("Program failed: '"
						+ program + "'\n" + e);
				}
			}
		}
	}


	// several utility functions

	static class GlobFilter implements FilenameFilter {
		Pattern pattern;
		long newerThan;
		String glob;
		String lastMatch;

		GlobFilter(String glob) {
			this(glob, 0);
		}

		GlobFilter(String glob, long newerThan) {
			this.glob = glob;
			String regex = "^" + replaceSpecials(glob) + "$";
			pattern = Pattern.compile(regex);
			this.newerThan = newerThan;
		}

		String replaceSpecials(String glob) {
			StringBuffer result = new StringBuffer();
			char[] array = glob.toCharArray();
			int len = array.length;
			for (int i = 0; i < len; i++) {
				char c = array[i];
				if (".^$".indexOf(c) >= 0)
					result.append("\\" + c);
				else if (c == '?')
					result.append("[^/]");
				else if (c == '*') {
					if (i + 1 >= len || array[i + 1] != '*')
						result.append("[^/]*");
					else {
						result.append(".*");
						i++;
						if (i + 1 < len && array[i + 1]
								== '/')
							i++;
					}
				} else
					result.append(c);
			}
			return result.toString();
		}

		public boolean accept(File dir, String name) {
			if (newerThan > 0 && newerThan > new File(dir, name)
					.lastModified())
				return false;
			if (pattern.matcher(name).matches()) {
				lastMatch = name;
				return true;
			}
			lastMatch = null;
			return false;
		}

		boolean wildcardContainsStarStar;
		int firstWildcardIndex = -1, suffixLength;
		String wildcardPattern;

		private void initReplace() throws FakeException {
			if (firstWildcardIndex >= 0)
				return;
			wildcardContainsStarStar = glob.indexOf("**") >= 0;
			int first = glob.indexOf('*');
			int first2 = glob.indexOf('?');
			if (first < 0 && first2 < 0)
				throw new FakeException("Expected glob: "
					+ glob);
			int last = glob.lastIndexOf('*');
			int last2 = glob.lastIndexOf('?');
			firstWildcardIndex = first < 0 ||
				(first2 >= 0 && first > first2) ?
				first2 : first;
			int lastWildcardIndex = last < 0 || last < last2 ?
				last2 : last;
			wildcardPattern = glob.substring(firstWildcardIndex,
				lastWildcardIndex + 1);
			suffixLength = glob.length() - lastWildcardIndex - 1;
		}

		public String replace(String name) throws FakeException {
			initReplace();
			int index = name.indexOf(wildcardPattern);
			while (!wildcardContainsStarStar && index >= 0 &&
					name.substring(index).startsWith("**"))
				index = name.indexOf(wildcardPattern,
					name.substring(index).startsWith("**/*")
					? index + 4 : index + 2);
			if (index < 0)
				return name;
			return replace(name.substring(0, index)
				+ lastMatch.substring(firstWildcardIndex,
					lastMatch.length() - suffixLength)
				+ name.substring(index
					+ wildcardPattern.length()));
		}

		public List replace(List names) throws FakeException {
			List result = new ArrayList();
			Iterator iter = names.iterator();
			while (iter.hasNext())
				result.add(replace((String)iter.next()));
			return result;
		}
	}

	protected int expandGlob(String glob, Collection list, File cwd,
			long newerThan, String buildDir) throws FakeException {
		if (glob == null)
			return 0;
		// find first wildcard
		int star = glob.indexOf('*'), qmark = glob.indexOf('?');

		// no wildcard?
		if (star < 0 && qmark < 0) {
			list.add(glob);
			return 1;
		}

		if (qmark >= 0 && qmark < star)
			star = qmark;
		boolean starstar = glob.substring(star).startsWith("**/");

		int prevSlash = glob.lastIndexOf('/', star);
		int nextSlash = glob.indexOf('/', star);

		String parentPath =
			prevSlash < 0 ? "" : glob.substring(0, prevSlash + 1);
		if (buildDir != null && parentPath.equals(buildDir))
			return 0;

		File parentDirectory = new File(makePath(cwd, parentPath));
		if (!parentDirectory.exists())
			throw new FakeException("Directory '" + parentDirectory
				+ "' not found");

		String pattern = nextSlash < 0 ?
			glob.substring(prevSlash + 1) :
			glob.substring(prevSlash + 1, nextSlash);

		String remainder = nextSlash < 0 ?
			null : glob.substring(nextSlash + 1);

		int count = 0;

		if (starstar) {
			count += expandGlob(parentPath + remainder, list,
						cwd, newerThan, buildDir);
			remainder = "**/" + remainder;
			pattern = "*";
		}

		String[] names = parentDirectory.list(new GlobFilter(pattern,
					newerThan));
		Arrays.sort(names);

		for (int i = 0; i < names.length; i++) {
			String path = parentPath + names[i];
			if (starstar && names[i].startsWith("."))
				continue;
			if (names[i].equals(".git") || names[i].endsWith(".swp")
					|| names[i].endsWith(".swo"))
				continue;
			File file = new File(makePath(cwd, path));
			if (nextSlash < 0) {
				if (file.isDirectory())
					continue;
				list.add(path);
				count++;
			}
			else if (file.isDirectory())
				count += expandGlob(path + "/" + remainder,
						list, cwd, newerThan, buildDir);
		}

		return count;
	}

	Set expandToSet(String glob, File cwd) throws FakeException {
		Set result = new HashSet();
		String[] globs = split(glob, " ");
		for (int i = 0; i < globs.length; i++)
			expandGlob(globs[i], result, cwd, 0, null);
		return result;
	}

	/*
	 * Sort .class entries at end of the given list
	 *
	 * Due to the recursive nature of java2classFiles(), the sorting of
	 * the glob expansion is not enough.
	 */
	protected void sortClassesAtEnd(List list) {
		int size = list.size();
		if (size == 0 || !isClass(list, size - 1))
			return;
		int start = size - 1;
		while (start > 0 && isClass(list, start - 1))
			start--;
		List classes = new ArrayList();
		classes.addAll(list.subList(start, size));
		Collections.sort(classes);
		while (size > start)
			list.remove(--size);
		list.addAll(classes);
	}
	final protected boolean isClass(List list, int index) {
		return ((String)list.get(index)).endsWith(".class");
	}


	/*
	 * This function inspects a .class file for a given .java file,
	 * infers the package name and all used classes, and adds to "all"
	 * the class file names of those classes used that have been found
	 * in the same class path.
	 */
	protected void java2classFiles(String java, File cwd,
			File buildDir, List result, Set all) {
		if (java.endsWith(".java"))
			java = java.substring(0, java.length() - 5) + ".class";
		else if (!java.endsWith(".class")) {
			if (!all.contains(java)) {
				if (buildDir == null)
					sortClassesAtEnd(result);
				result.add(java);
				all.add(java);
			}
			return;
		}
		byte[] buffer = readFile(makePath(cwd, java));
		if (buffer == null) {
			if (!java.endsWith("/package-info.class"))
				err.println("Warning: " + java
					+ " does not exist.  Skipping...");
			return;
		}
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer);
		String fullClass = analyzer.getPathForClass() + ".class";
		if (!java.endsWith(fullClass))
			throw new RuntimeException("Huh? " + fullClass
					+ " is not a suffix of " + java);
		java = java.substring(0, java.length() - fullClass.length());
		Iterator iter = analyzer.getClassNames();
		while (iter.hasNext()) {
			String className = (String)iter.next();
			String path = java + className + ".class";
			if (new File(makePath(cwd, path)).exists() &&
					!all.contains(path)) {
				result.add(path);
				all.add(path);
				java2classFiles(path, cwd, buildDir,
						result, all);
			}
		}
	}

	protected static void addRecursively(File dir, List result, Set all) {
		String[] files = dir.list();
		if (files == null || files.length == 0)
			return;
		Arrays.sort(files);
		for (int i = 0; i < files.length; i++) {
			File file = new File(dir, files[i]);
			if (file.isDirectory())
				addRecursively(file, result, all);
			else if (files[i].endsWith(".class")) {
				result.add(file.getAbsolutePath());
				all.add(file.getAbsolutePath());
			}
		}
	}

	protected String getPrefix(File cwd, String path) {
		try {
			InputStream input = new FileInputStream(makePath(cwd, path));
			InputStreamReader inputReader =
				new InputStreamReader(input);
			BufferedReader reader = new BufferedReader(inputReader);
			for (;;) {
				String line = reader.readLine();
				if (line == null)
					break;
				else
					line = line.trim();
				if (!line.startsWith("package "))
					continue;
				line = line.substring(8);
				if (line.endsWith(";"))
					line = line.substring(0,
							line.length() - 1);
				line = line.replace(".", "/");
				int slash = path.lastIndexOf("/");
				int backslash = path.lastIndexOf("\\");
				if (backslash > slash)
					slash = backslash;
				if (path.endsWith(line + path.substring(slash)))
					return path.substring(0, path.length() -
							line.length() -
							path.length() + slash);
			}
			int slash = path.lastIndexOf('/');
			return path.substring(0, slash + 1);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/* discovers all the .class files for a given set of .java files */
	protected List java2classFiles(List javas, File cwd,
			File buildDir, Set exclude, Set noCompile)
			throws FakeException {
		List result = new ArrayList();
		Set all = new HashSet();
		if (buildDir != null) {
			addRecursively(buildDir, result, all);
			Collections.sort(result);
		}
		String lastJava = null;
		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String file = (String)iter.next();
			if (exclude.contains(file))
				continue;
			boolean dontCompile = noCompile.contains(file);
			if (buildDir != null) {
				if (!dontCompile && file.endsWith(".java")) {
					lastJava = file;
					continue;
				}
				if (lastJava != null) {
					String prefix = getPrefix(cwd, lastJava);
					if (prefix != null)
						result.add(prefix);
					else
						err.println("Error: "
							+ lastJava);
					lastJava = null;
				}
			}
			if (dontCompile) {
				if (!all.contains(file)) {
					result.add(file);
					all.add(file);
				}
				continue;
			}
			java2classFiles(file, cwd, buildDir, result, all);
		}
		if (buildDir == null)
			sortClassesAtEnd(result);
		return result;
	}

	// this function handles the javac singleton
	protected synchronized void callJavac(String[] arguments,
			boolean verbose) throws FakeException {
		try {
			if (javac == null) {
				discoverJavac();
				JarClassLoader loader = (JarClassLoader)
					getClassLoader(toolsPath);
				String className = "com.sun.tools.javac.Main";
				Class main = loader.forceLoadClass(className);
				Class[] argsType = new Class[] {
					arguments.getClass(),
					PrintWriter.class
				};
				javac = main.getMethod("compile", argsType);
			}

			Object result = javac.invoke(null,
					new Object[] { arguments, new PrintWriter(err) });
			if (!result.equals(new Integer(0)))
				throw new FakeException("Compile error");
			return;
		} catch (FakeException e) {
			/* was compile error */
			throw e;
		} catch (Exception e) {
			err.println("Could not find javac " + e
				+ " (tools path = " + toolsPath + "), "
				+ "falling back to system javac");
		}

		// fall back to calling javac
		String[] newArguments = new String[arguments.length + 1];
		newArguments[0] = "javac";
		System.arraycopy(arguments, 0, newArguments, 1,
				arguments.length);
		try {
			execute(newArguments, new File("."), verbose);
		} catch (Exception e) {
			throw new FakeException("Could not even fall back "
				+ " to javac in the PATH");
		}
	}

	// returns all .java files in the list, and returns a list where
	// all the .java files have been replaced by their .class files.
	protected List compileJavas(List javas, File cwd, File buildDir,
			String javaVersion, boolean debug, boolean verbose,
			boolean showDeprecation, String extraClassPath,
			Set exclude, Set noCompile)
			throws FakeException {
		List arguments = new ArrayList();
		arguments.add("-encoding");
		arguments.add("UTF8");
		if (debug)
			arguments.add("-g");
		if (buildDir != null) {
			buildDir.mkdirs();
			arguments.add("-d");
			arguments.add(buildDir.getAbsolutePath());
		}
		if (javaVersion != null && !javaVersion.equals("")) {
			arguments.add("-source");
			arguments.add(javaVersion);
			arguments.add("-target");
			arguments.add(javaVersion);
		}
		if (showDeprecation) {
			arguments.add("-deprecation");
			arguments.add("-Xlint:unchecked");
		}
		String classPath = discoverClassPath();
		if (extraClassPath != null && !extraClassPath.equals("")) {
			StringTokenizer tokenizer =
				new StringTokenizer(extraClassPath, ":");
			while (tokenizer.hasMoreElements())
				classPath += File.pathSeparator
					+ tokenizer.nextToken();
		}
		if (classPath != null && !classPath.equals("")) {
			arguments.add("-classpath");
			arguments.add(classPath);
		}

		int optionCount = arguments.size();
		Iterator iter = javas.iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();

			if (path.endsWith(".java") && !exclude.contains(path))
				arguments.add(makePath(cwd, path));
		}

		/* Do nothing if there is nothing to do ;-) */
		if (optionCount == arguments.size())
			return javas;

		String[] args = (String[])arguments.toArray(new
				String[arguments.size()]);

		if (verbose) {
			String output = "Compiling .java files: javac";
			for (int i = 0; i < args.length; i++)
				output += " " + args[i];
			err.println(output);
		}

		try {
			callJavac(args, verbose);
		} catch (FakeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new FakeException("Compile error: " + e);
		}

		List result = java2classFiles(javas, cwd, buildDir, exclude,
			noCompile);
		return result;
	}

	protected static void addPluginsConfigToJar(JarOutputStream jar,
			String configPath) throws IOException {
		if (configPath == null)
			return;

		JarEntry entry = new JarEntry("plugins.config");
		jar.putNextEntry(entry);
		byte[] buffer = readFile(configPath);
		jar.write(buffer, 0, buffer.length);
		jar.closeEntry();
	}

	// TODO: we really need string pairs; real path and desired path.
	protected void makeJar(String path, String mainClass, List files,
			File cwd, File buildDir, String configPath,
			String stripPath, boolean verbose) throws FakeException {
		path = makePath(cwd, path);
		if (verbose) {
			String output = "Making " + path;
			if (mainClass != null)
				output += " with main-class " + mainClass;
			output += " from";
			Iterator iter = files.iterator();
			while (iter.hasNext())
				output += " " + iter.next();
			err.println(output);
		}
		Manifest manifest = null;
		if (mainClass != null) {
			String text = "Manifest-Version: 1.0\nMain-Class: "
				+ mainClass + "\n";
			InputStream input =
				new ByteArrayInputStream(text.getBytes());
			try {
				manifest = new Manifest(input);
			} catch(Exception e) { }
		}

		try {
			/*
			 * Avoid SIGBUS when writing fake.jar: it may be
			 * in use (mmap()ed), and overwriting that typically
			 * results in a crash.
			 */
			String origPath = null;
			try {
				if (moveFileOutOfTheWay(path)) {
					origPath = path;
					path += ".new";
				}
			} catch (FakeException e) {
				path = moveToUpdateDirectory(path);
			}

			OutputStream out = new FileOutputStream(path);
			JarOutputStream jar = manifest == null ?
				new JarOutputStream(out) :
				new JarOutputStream(out, manifest);

			addPluginsConfigToJar(jar, configPath);
			String lastBase = stripPath;
			Iterator iter = files.iterator();
			while (iter.hasNext()) {
				String realName = (String)iter.next();
				if (realName.endsWith(".jar/")) {
					copyJar(stripSuffix(makePath(cwd,
						realName), "/"), jar,
						verbose);
					continue;
				}
				if (realName.endsWith("/") || realName.endsWith("\\") ||
						realName.equals("")) {
					lastBase = realName;
					continue;
				}
				String name = realName;
				int bracket = name.indexOf('[');
				if (bracket >= 0 && name.endsWith("]")) {
					realName = name.substring(bracket + 1,
						name.length() - 1);
					name = name.substring(0, bracket);
				}
				byte[] buffer = readFile(makePath(cwd,
								realName));
				if (buffer == null)
					throw new FakeException("File "
						+ realName + " does not exist,"
						+ " could not make " + path);
				if (realName.endsWith(".class")) {
					ByteCodeAnalyzer analyzer =
						new ByteCodeAnalyzer(buffer);
					name = analyzer.getPathForClass()
						+ ".class";
				}
				else if (lastBase != null &&
						name.startsWith(lastBase)) {
					if (!lastBase.equals(stripPath))
						throw new FakeException("strip "
							+ "path mismatch: "
							+ lastBase + " != "
							+ stripPath);
					name = name
						.substring(lastBase.length());
				}

				JarEntry entry = new JarEntry(name);
				writeJarEntry(entry, jar, buffer);
			}

			jar.close();
			if (origPath != null)
				throw new FakeException("Could not remove "
					+ origPath
					+ " before building it anew\n"
					+ "Stored it as " + path
					+ " instead.");
		} catch (Exception e) {
			new File(path).delete();
			e.printStackTrace();
			throw new FakeException("Error writing "
				+ path + ": " + e);
		}
	}

	static void touchFile(String target) throws IOException {
		long now = new Date().getTime();
		new File(target).setLastModified(now);
	}

	void copyJar(String inJar, JarOutputStream out, boolean verbose)
			throws Exception {
		File file = new File(inJar);
		InputStream input = new FileInputStream(file);
		JarInputStream in = new JarInputStream(input);

		JarEntry entry;
		while ((entry = in.getNextJarEntry()) != null) {
			String name = entry.getName();
			if (name.startsWith("META-INF/")) {
				in.closeEntry();
				continue;
			}
			byte[] buf = readStream(in);
			in.closeEntry();
			entry.setCompressedSize(-1);
			writeJarEntry(entry, out, buf);
		}
		in.close();
	}

	void writeJarEntry(JarEntry entry, JarOutputStream out,
			byte[] buf) throws ZipException, IOException {
		try {
			out.putNextEntry(entry);
			out.write(buf, 0, buf.length);
			out.closeEntry();
		} catch (ZipException e) {
			String msg = e.getMessage();
			if (!msg.startsWith("duplicate entry: ")) {
				err.println("Error writing "
						+ entry.getName());
				throw e;
			}
			err.println("ignoring " + msg);
		}
	}

	static byte[] readFile(String fileName) {
		try {
			if (fileName.startsWith("jar:file:")) {
				URL url = new URL(fileName);
				return readStream(url.openStream());
			}
			File file = new File(fileName);
			if (!file.exists())
				return null;
			InputStream in = new FileInputStream(file);
			byte[] buffer = new byte[(int)file.length()];
			in.read(buffer);
			in.close();
			return buffer;
		} catch (Exception e) { return null; }
	}

	public static void copyFile(String source, String target, File cwd)
			throws FakeException {
		if (target.equals(source))
			return;
		try {
			if (!target.startsWith("/"))
				target = cwd + "/" + target;
			if (!source.startsWith("/"))
				source = cwd + "/" + source;
			File parent = new File(target).getParentFile();
			if (!parent.exists())
				parent.mkdirs();
			OutputStream out = new FileOutputStream(target);
			InputStream in = new FileInputStream(source);
			byte[] buffer = new byte[1<<16];
			for (;;) {
				int len = in.read(buffer);
				if (len < 0)
					break;
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new FakeException("Could not copy "
				+ source + " to " + target + ": " + e);
		}
	}

	public static int compare(File source, File target) {
		if (source.length() != target.length())
			return target.length() > source.length() ? 1 : -1;
		int result = 0;
		try {
			InputStream sourceIn = new FileInputStream(source);
			InputStream targetIn = new FileInputStream(target);
			byte[] buf1 = new byte[1<<16];
			byte[] buf2 = new byte[1<<16];
			while (result == 0) {
				int len = sourceIn.read(buf1);
				if (len < 0)
					break;
				int off = 0, count = 0;
				while (len > 0 && count >= 0) {
					count = targetIn.read(buf2, off, len);
					off += count;
					len -= count;
				}
				if (count < 0) {
					result = 1;
					break;
				}
				for (int i = 0; i < off; i++)
					if (buf1[i] != buf2[i]) {
						result = (buf2[i] & 0xff)
							- (buf1[i] & 0xff);
						break;
					}
			}
			sourceIn.close();
			targetIn.close();
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Could not compare "
				+ source + " to " + target + ": " + e);
		}
	}

	protected static class StreamDumper extends Thread {
		InputStream in;
		OutputStream out;

		StreamDumper(InputStream in, PrintStream out) {
			this.in = in;
			this.out = out;
		}

		public void run() {
			byte[] buffer = new byte[65536];
			for (;;) {
				try {
					int len = in.read(buffer, 0, buffer.length);
					if (len < 0)
						break;
					if (len > 0)
						out.write(buffer, 0, len);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// the parameter "file" is only used to set the cwd
	protected void execute(List arguments, String file,
			boolean verbose) throws IOException, FakeException {
		execute(arguments, new File(file).getParentFile(), verbose);
	}

	protected void execute(String[] args, String file,
			boolean verbose) throws IOException, FakeException {
		execute(args, new File(file).getParentFile(), verbose);
	}

	protected void execute(List arguments, File dir, boolean verbose)
			throws IOException, FakeException {
		String[] args = new String[arguments.size()];
		arguments.toArray(args);
		execute(args, dir, verbose);
	}

	protected void execute(String[] args, File dir, boolean verbose)
			throws IOException, FakeException {
		if (verbose) {
			String output = "Executing:";
			for (int i = 0; i < args.length; i++)
				output += " '" + args[i] + "'";
			err.println(output);
		}

		if (args[0].endsWith(".py")) {
			String args0orig = args[0];
			args[0] = makePath(dir, args[0]);
			if (executePython(args))
				return;
			if (verbose)
				err.println("Falling back to Python ("
					+ "Jython was not found in classpath)");
			args[0] = args0orig;
		}

		/* stupid, stupid Windows... */
		if (getPlatform().startsWith("win")) {
			for (int i = 0; i < args.length; i++)
				args[i] = quoteArg(args[i]);
			// stupid, stupid, stupid Windows taking all my time!!!
			if (args[0].startsWith("../"))
				args[0] = new File(dir,
						args[0]).getAbsolutePath();
			else if (args[0].equals("bash") && getPlatform().equals("win64")) {
				String[] newArgs = new String[args.length + 2];
				newArgs[0] = System.getenv("WINDIR") + "\\SYSWOW64\\cmd.exe";
				newArgs[1] = "/C";
				System.arraycopy(args, 0, newArgs, 2, args.length);
				args = newArgs;
			}
		}

		Process proc = Runtime.getRuntime().exec(args, null, dir);
		new StreamDumper(proc.getErrorStream(), err).start();
		new StreamDumper(proc.getInputStream(), out).start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new FakeException(e.getMessage());
		}
		int exitValue = proc.exitValue();
		if (exitValue != 0)
			throw new FakeException("Failed: " + exitValue);
	}

	private static String quotables = " \"\'";
	public static String quoteArg(String arg) {
		return quoteArg(arg, quotables);
	}

	public static String quoteArg(String arg, String quotables) {
		for (int j = 0; j < arg.length(); j++) {
			char c = arg.charAt(j);
			if (quotables.indexOf(c) >= 0) {
				String replacement;
				if (c == '"') {
					if (System.getenv("MSYSTEM") != null)
						replacement = "\\" + c;
					else
						replacement = "'" + c + "'";
				}
				else
					replacement = "\"" + c + "\"";
				arg = arg.substring(0, j)
					+ replacement
					+ arg.substring(j + 1);
				j += replacement.length() - 1;
			}
		}
		return arg;
	}


	protected static Constructor jythonCreate;
	protected static Method jythonExec, jythonExecfile;

	protected static boolean executePython(String[] args)
			throws FakeException {
		if (jythonExecfile == null) try {
			discoverJython();
			ClassLoader loader = getClassLoader();
			String className = "org.python.util.PythonInterpreter";
			Class main = loader.loadClass(className);
			Class[] argsType = new Class[] { };
			jythonCreate = main.getConstructor(argsType);
			argsType = new Class[] { args[0].getClass() };
			jythonExec = main.getMethod("exec", argsType);
			argsType = new Class[] { args[0].getClass() };
			jythonExecfile = main.getMethod("execfile", argsType);
		} catch (Exception e) {
			return false;
		}

		try {
			Object instance =
				jythonCreate.newInstance(new Object[] { });
			String init = "import sys\n" +
				"sys.argv = [";
			for (int i = 0; i < args.length; i++)
				init += (i > 0 ? ", " : "")
					+ "\"" + quoteArg(args[i], "\"") + "\"";
			init += "]\n";
			jythonExec.invoke(instance, new Object[] { init });
			String sysPath = "sys.path.insert(0, '"
				+ new File(args[0]).getParent() + "')";
			jythonExec.invoke(instance, new Object[] { sysPath });
			jythonExecfile.invoke(instance,
					new Object[] { args[0] });
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
			throw new FakeException("Jython failed");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	protected void fakeOrMake(File cwd, String directory, boolean verbose,
			boolean ignoreMissingFakefiles, String toolsPath,
			String classPath, String fallBackFakefile,
			File buildDir, String defaultTarget)
			throws FakeException {
		String[] files = new File(directory).list();
		if (files == null || files.length == 0)
			return;
		files = null;

		String fakeFile = directory + '/' + Parser.path;
		boolean tryFake = new File(fakeFile).exists();
		if (!tryFake) {
			fakeFile = fallBackFakefile;
			tryFake = new File(fakeFile).exists();
		}
		if (ignoreMissingFakefiles && !tryFake &&
				!(new File(directory + "/Makefile").exists())) {
			if (verbose)
				err.println("Ignore " + directory);
			return;
		}
		err.println((tryFake ? "Build" : "Mak") + "ing in "
			+ directory + (directory.endsWith("/") ? "" : "/"));

		try {
			if (tryFake) {
				// Try "Fake"
				Parser parser = new Parser(fakeFile);
				if (verbose)
					parser.setVariable("VERBOSE", "true");
				if (toolsPath != null)
					parser.setVariable("TOOLSPATH",
							toolsPath);
				if (classPath != null)
					parser.setVariable("CLASSPATH",
							classPath);
				if (buildDir != null)
					parser.setVariable("BUILDDIR",
						buildDir.getAbsolutePath());
				parser.cwd = new File(cwd, directory);
				Parser.Rule all = parser.parseRules(null);
				if (defaultTarget != null) {
					Parser.Rule rule =
						all.getRule(defaultTarget);
					if (rule != null)
						all = rule;
				}
				all.make();
			} else
				// Try "make"
				execute(new String[] { "make" },
					new File(directory), verbose);
		} catch (Exception e) {
			if (!(e instanceof FakeException))
				e.printStackTrace();
			throw new FakeException((tryFake ?  "Fake" : "make")
				+ " failed: " + e);
		}
		err.println("Leaving " + directory);
	}

	protected boolean jarUpToDate(String source, String target,
			boolean verbose) {
		JarFile targetJar, sourceJar;

		try {
			targetJar = new JarFile(target);
		} catch(IOException e) {
			if (verbose)
				err.println(target
						+ " does not exist yet");
			return false;
		}
		try {
			sourceJar = new JarFile(source);
		} catch(IOException e) {
			return true;
		}

		Enumeration iter = sourceJar.entries();
		while (iter.hasMoreElements()) {
			JarEntry entry = (JarEntry)iter.nextElement();
			JarEntry other =
				(JarEntry)targetJar.getEntry(entry.getName());
			if (other == null) {
				if (verbose)
					err.println(target
						+ " lacks the file "
						+ entry.getName());
				return false;
			}
			if (entry.getTime() > other.getTime()) {
				if (verbose)
					err.println(target + " is not "
						+ "up-to-date because of "
						+ entry.getName());
				return false;
			}
		}
		try {
			targetJar.close();
			sourceJar.close();
		} catch(IOException e) { }

		return true;
	}

	protected static List splitCommandLine(String program)
			throws FakeException {
		List result = new ArrayList();
		if (program == null)
			return result;
		int len = program.length();
		String current = "";

		for (int i = 0; i < len; i++) {
			char c = program.charAt(i);
			if (isQuote(c)) {
				int i2 = findClosingQuote(program,
						c, i + 1, len);
				current += program.substring(i + 1, i2);
				i = i2;
				continue;
			}
			if (c == ' ' || c == '\t') {
				if (current.equals(""))
					continue;
				result.add(current);
				current = "";
			} else
				current += c;
		}
		if (!current.equals(""))
			result.add(current);
		return result;
	}

	protected static int findClosingQuote(String s, char quote,
			int index, int len) throws FakeException {
		for (int i = index; i < len; i++) {
			char c = s.charAt(i);
			if (c == quote)
				return i;
			if (isQuote(c))
				i = findClosingQuote(s, c, i + 1, len);
		}
		String spaces = "               ";
		for (int i = 0; i < index; i++)
			spaces += " ";
		throw new FakeException("Unclosed quote: "
			+ s + "\n" + spaces + "^");
	}

	protected static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	public String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		err.println("Unknown platform: " + osName);
		return osName;
	}

	public boolean isAbsolutePath(String path) {
		boolean isWindows = getPlatform().startsWith("win");
		return (isWindows && path.length() > 1 && path.charAt(1) == ':')
			|| (!isWindows && path.startsWith("/"));
	}

	public String makePath(File cwd, String path) {
		String prefix = "", suffix = "";
		if (path.startsWith("jar:file:")) {
			prefix = "jar:file:";
			int exclamation = path.indexOf('!');
			suffix = path.substring(exclamation);
			path = path.substring(prefix.length(), exclamation);
		}
		if (isAbsolutePath(path))
			return prefix + path + suffix;
		if (path.equals("."))
			return prefix + cwd.toString() + suffix;
		if (cwd.toString().equals("."))
			return prefix + (path.equals("") ? "." : path) + suffix;
		return prefix + new File(cwd, path).toString() + suffix;
	}

	static class ByteCodeAnalyzer {
		byte[] buffer;
		int[] poolOffsets;
		int endOffset;

		public ByteCodeAnalyzer(byte[] buffer) {
			this.buffer = buffer;
			if ((int)getU4(0) != 0xcafebabe)
				throw new RuntimeException("No class");
			getConstantPoolOffsets();
		}

		public String getPathForClass() {
			int thisOffset = dereferenceOffset(endOffset + 2);
			if (getU1(thisOffset) != 7)
				throw new RuntimeException("Parse error");
			return getString(dereferenceOffset(thisOffset + 1));
		}

		int dereferenceOffset(int offset) {
			int index = getU2(offset);
			return poolOffsets[index - 1];
		}

		void getConstantPoolOffsets() {
			int poolCount = getU2(8) - 1;
			poolOffsets = new int[poolCount];
			int offset = 10;
			for (int i = 0; i < poolCount; i++) {
				poolOffsets[i] = offset;
				int tag = getU1(offset);
				if (tag == 7 || tag == 8)
					offset += 3;
				else if (tag == 9 || tag == 10 || tag == 11 ||
						tag == 3 || tag == 4 ||
						tag == 12)
					offset += 5;
				else if (tag == 5 || tag == 6) {
					poolOffsets[++i] = offset;
					offset += 9;
				}
				else if (tag == 1)
					offset += 3 + getU2(offset + 1);
				else
					throw new RuntimeException("Unknown tag"
						+ " " + tag);
			}
			endOffset = offset;
		}

		class ClassNameIterator implements Iterator {
			int index;

			ClassNameIterator() {
				index = -1;
				findNext();
			}

			void findNext() {
				while (++index < poolOffsets.length)
					if (getU1(poolOffsets[index]) == 7)
						break;
			}

			public boolean hasNext() {
				return index < poolOffsets.length;
			}

			public Object next() {
				int offset = poolOffsets[index];
				findNext();
				return getString(dereferenceOffset(offset + 1));
			}

			public void remove()
					throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		}

		public Iterator getClassNames() {
			return new ClassNameIterator();
		}

		public String toString() {
			String result = "";
			for (int i = 0; i < poolOffsets.length; i++) {
				int offset = poolOffsets[i];
				result += "index #" + i + ": "
					+ format(offset) + "\n";
				int tag = getU1(offset);
				if (tag == 5 || tag == 6)
					i++;
			}
			return result;
		}

		int getU1(int offset) {
			return buffer[offset] & 0xff;
		}

		int getU2(int offset) {
			return getU1(offset) << 8 | getU1(offset + 1);
		}

		long getU4(int offset) {
			return ((long)getU2(offset)) << 16 | getU2(offset + 2);
		}

		String getString(int offset) {
			try {
				return new String(buffer, offset + 3,
						getU2(offset + 1), "UTF-8");
			} catch (Exception e) { return ""; }
		}

		String format(int offset) {
			int tag = getU1(offset);
			int u2 = getU2(offset + 1);
			String result = "offset: " + offset + "(" + tag + "), ";
			if (tag == 7)
				return result + "class " + u2;
			if (tag == 9)
				return result + "field " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 10)
				return result + "method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 11)
				return result + "interface method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 8)
				return result + "string #" + u2;
			if (tag == 3)
				return result + "integer " + getU4(offset + 1);
			if (tag == 4)
				return result + "float " + getU4(offset + 1);
			if (tag == 12)
				return result + "name and type " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 5)
				return result + "long "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 6)
				return result + "double "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 1)
				return result + "utf8 " + u2
					+ " " + getString(offset);
			return result + "unknown";
		}
	}

	private static JarClassLoader classLoader;

	public static ClassLoader getClassLoader() throws IOException {
		return getClassLoader(null);
	}

	protected static ClassLoader getClassLoader(String jarFile)
			throws IOException {
		if (classLoader == null)
			classLoader = new JarClassLoader();
		if (jarFile != null &&
		    ! classLoader.jarFilesMap.containsKey(jarFile)) {
			JarFile jar = new JarFile(jarFile);
			synchronized (classLoader) {
				/* n.b. We don't need to synchronize
				   fetching since nothing is ever removed */
				classLoader.jarFilesMap.put(jarFile, jar);
				classLoader.jarFilesNames.add(jarFile);
				classLoader.jarFilesObjects.add(jar);
			}
		}
		return classLoader;
	}

	private static class JarClassLoader extends ClassLoader {
		Map jarFilesMap;
		List jarFilesNames;
		List jarFilesObjects;
		Map cache;

		JarClassLoader() {
			super(Thread.currentThread().getContextClassLoader());
			jarFilesMap = new HashMap();
			jarFilesNames = new ArrayList(10);
			jarFilesObjects = new ArrayList(10);
			cache = new HashMap();
		}

		public URL getResource(String name) {
			int n = jarFilesNames.size();
			for (int i = n - 1; i >= 0; --i) {
				JarFile jar = (JarFile)jarFilesObjects.get(i);
				String file = (String)jarFilesNames.get(i);
				if (jar.getEntry(name) == null)
					continue;
				String url = "file:///"
					+ file.replace('\\', '/')
					+ "!/" + name;
				try {
					return new URL("jar", "", url);
				} catch (MalformedURLException e) { }
			}
			return getSystemResource(name);
		}

		public InputStream getResourceAsStream(String name) {
			return getResourceAsStream(name, false);
		}

		public InputStream getResourceAsStream(String name,
				boolean nonSystemOnly) {
			int n = jarFilesNames.size();
			for (int i = n - 1; i >= 0; --i) {
				JarFile jar = (JarFile)jarFilesObjects.get(i);
				JarEntry entry = jar.getJarEntry(name);
				if (entry == null)
					continue;
				try {
					return jar.getInputStream(entry);
				} catch (IOException e) { }
			}
			if (nonSystemOnly)
				return null;
			return super.getResourceAsStream(name);
		}

		public Class forceLoadClass(String name)
				throws ClassNotFoundException {
			return loadClass(name, true, true);
		}

		public Class loadClass(String name)
				throws ClassNotFoundException {
			return loadClass(name, true);
		}

		public synchronized Class loadClass(String name,
				boolean resolve) throws ClassNotFoundException {
			return loadClass(name, resolve, false);
		}

		public synchronized Class loadClass(String name,
					boolean resolve, boolean forceReload)
				throws ClassNotFoundException {
			Object cached = forceReload ? null : cache.get(name);
			if (cached != null)
				return (Class)cached;
			Class result;
			try {
				if (!forceReload) {
					result = super.loadClass(name, resolve);
					if (result != null)
						return result;
				}
			} catch (Exception e) { }
			String path = name.replace('.', '/') + ".class";
			InputStream input = getResourceAsStream(path, !true);
			if (input == null)
				throw new ClassNotFoundException(name);
			try {
				byte[] buffer = readStream(input);
				input.close();
				result = defineClass(name,
						buffer, 0, buffer.length);
				cache.put(name, result);
				return result;
			} catch (IOException e) {
				result = forceReload ?
					super.loadClass(name, resolve) : null;
				return result;
			}
		}
	}

	static byte[] readStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;) {
			if (offset == buffer.length)
				buffer = realloc(buffer,
						2 * buffer.length);
			len = input.read(buffer, offset,
					buffer.length - offset);
			if (len < 0)
				return realloc(buffer, offset);
			offset += len;
		}
	}

	protected void delete(File file) throws FakeException {
		if (!file.delete())
			throw new FakeException("Could not delete "
					+ file.getPath());
	}

	protected void deleteRecursively(File dir) {
		try {
			File[] list = dir.listFiles();
			if (list != null)
				for (int i = 0; i < list.length; i++) {
					if (list[i].isDirectory())
						deleteRecursively(list[i]);
					else
						delete(list[i]);
				}
			delete(dir);
		} catch (FakeException e) {
			out.println("Error: " + e.getMessage());
		}
	}

	protected static boolean isDirEmpty(String path) {
		String[] list = new File(path).list();
		return list == null || list.length == 0;
	}

	static byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}

	public static List uniq(List list) {
		Set set = new HashSet();
		List result = new ArrayList();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Object key = iter.next();
			if (set.contains(key))
				continue;
			result.add(key);
			set.add(key);
		}
		return result;
	}

	public static String join(List list) {
		return join(list, " ");
	}

	public static String stripFijiHome(String string) {
		if (string == null)
			return string;
		String slashes = string.replace('\\', '/');
		if (slashes.startsWith(fijiHome))
			return stripPrefix(slashes, fijiHome);
		return string;
	}

	public static String stripPrefix(String string, String prefix) {
		if (!string.startsWith(prefix))
			return string;
		return string.substring(prefix.length());
	}

	public static String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public static String join(List list, String separator) {
		Iterator iter = list.iterator();
		String result = iter.hasNext() ? iter.next().toString() : "";
		while (iter.hasNext())
			result += separator + iter.next();
		return result;
	}

	public static String join(String[] list, String separator) {
		String result = list.length > 0 ? list[0] : "";
		for (int i = 1; i < list.length; i++)
			result += separator + list[i];
		return result;
	}

	public static String[] split(String string, String delimiter) {
		if (string == null || string.equals(""))
			return new String[0];
		List list = new ArrayList();
		int offset = 0;
		for (;;) {
			int nextOffset = string.indexOf(delimiter, offset);
			if (nextOffset < 0) {
				list.add(string.substring(offset));
				break;
			}
			list.add(string.substring(offset, nextOffset));
			offset = nextOffset + 1;
		}
		String[] result = new String[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = (String)list.get(i);
		return result;
	}

	public String prefixPaths(File cwd, String pathList,
			boolean skipVariables) {
		if (pathList == null || pathList.equals(""))
			return pathList;
		String[] paths = split(pathList, ":");
		for (int i = 0; i < paths.length; i++)
			if (!skipVariables || !paths[i].startsWith("$"))
				paths[i] = makePath(cwd, paths[i]);
		return join(paths, ":");
	}

	static boolean moveFileOutOfTheWay(String file) throws FakeException {
		return moveFileOutOfTheWay(new File(file));
	}

	static boolean moveFileOutOfTheWay(File file) throws FakeException {
		if (!file.exists())
			return false;
		if (file.delete())
			return false;
		if (file.renameTo(new File(file.getPath() + ".old")))
			return true;
		throw new FakeException("Could not move " + file
				+ " out of the way");
	}

	static String moveToUpdateDirectory(String path) throws FakeException {
		return new File(path).getAbsolutePath();
	}

	static File moveToUpdateDirectory(File file) throws FakeException {
		String absolute = file.getAbsolutePath().replace('\\', '/');
		if (!absolute.startsWith(fijiHome))
			throw new FakeException("The file " + file
					+ " could not be deleted!");
		int len = fijiHome.length();
		File result = new File(absolute.substring(0, len)
			+ "update/" + absolute.substring(len));
		result.getParentFile().mkdirs();
		return result;
	}


	// our very own exception

	static public class FakeException extends Exception {
		public static final long serialVersionUID = 1;
		public FakeException(String message) {
			super(message);
		}

		public String toString() {
			return getMessage();
		}
	}
}
