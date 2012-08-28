package fiji.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;

public class Parser {
	public final static String path = "Fakefile";
	protected Fake fake;
	protected long mtimeFakefile;
	BufferedReader reader;
	String line;
	int lineNumber;
	File cwd;
	protected Map<String, Rule> allRules = new TreeMap<String, Rule>();
	protected Set<String> allPrerequisites = new HashSet<String>();
	protected Set<String> allPlatforms;
	protected Rule allRule;
	protected String buildDir;

	public Parser(Fake fake) throws FakeException {
		this(fake, null, null);
	}

	public Parser(Fake fake, String path) throws FakeException, IOException {
		this(fake, new FileInputStream(path == null ||
			path.equals("") ? Parser.path : path), null);
		mtimeFakefile = new File(path).lastModified();
	}

	public Parser(Fake fake, InputStream stream, File cwd) throws FakeException {
		this.fake = fake;
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
			allPlatforms = new HashSet<String>();
			allPlatforms.add("linux32");
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
			allPlatforms.add("freebsd");
			for (String platform : allPlatforms)
				setVariable("platform(" + platform + ")", platform);
		}

		setVariable("platform", Util.getPlatform());

		setVariable("FIJIHOME", Fake.ijHome);
		setVariable("IJHOME", Fake.ijHome);

		// Include all libraries in classpath when not building Fiji itself
		// (when building submodules, the classpath will be overridden)
		try {
			if (!new File(Fake.ijHome).getCanonicalFile().equals(this.cwd.getCanonicalFile()))
				setVariable("CLASSPATH", Util.join(fake.discoverJars(), ":"));
		} catch (IOException e) { /* ignore */ }

		addSpecialRule(new Special(Parser.this, "show-rules") {
			void action() { showMap(allRules, false); }
		});

		addSpecialRule(new Special(Parser.this, "show-vars") {
			void action() { showMap(variables, true); }
		});

		addSpecialRule(new Special(Parser.this, "clean") {
			void action() { cleanAll(false); }
		});

		addSpecialRule(new Special(Parser.this, "clean-dry-run") {
			void action() { cleanAll(true); }
		});

		addSpecialRule(new Special(Parser.this, "dry-run") {
			void action() { check(); }
		});

		addSpecialRule(new Special(Parser.this, "check") {
			void action() { check(); }
		});

		addSpecialRule(new Special(Parser.this, "dependency-map") {
			void action() throws FakeException {
				List<Rule> targets = new ArrayList<Rule>();
				for (Rule target : allRules.values())
					if (!(target instanceof Special) && !target.target.equals(""))
						targets.add(target);
				showMap(buildDependencyMap(targets));
			}
		});
	}

	protected<T> void showMap(Map<String, T> map, boolean showKeys) {
		List<String> list = new ArrayList<String>(map.keySet());
		Collections.sort(list);
		for (String key : list)
			fake.out.println((showKeys ?
					key.toString() + " = " : "")
				+ map.get(key));
	}

	protected void showMap(Map<Rule, List<Rule>> map) {
		for (Map.Entry<Rule, List<Rule>> pair : map.entrySet()) {
			String neededBy = "";
			for (Rule rule : pair.getValue())
				if (rule != null)
					neededBy += " " + rule.target;
			fake.out.println(pair.getKey().target + (neededBy.equals("") ?
				"" : " needed by" + neededBy));
		}
	}

	protected void cleanAll(boolean dry_run) {
		for (Rule rule : allRules.values())
			rule.clean(dry_run);
	}

	protected void check() {
		for (Rule rule : new TreeMap<String, Rule>(allRules).values()) {
			if (rule instanceof All)
				continue;
			if (rule instanceof Special)
				continue;
			if (rule instanceof SubFake) {
				fake.out.println("Subfake '"
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
				fake.out.println("Program '" + program
					+ "' would maybe make '"
					+ rule.target + "'");
				continue;
			}
			fake.out.println("'" + rule.target
					+ "' is not up-to-date");
		}
	}

	public Map<Rule, List<Rule>> buildDependencyMap() throws FakeException {
		return buildDependencyMap(Collections.singletonList(getDefaultRule()));
	}

	public Map<Rule, List<Rule>> buildDependencyMap(String[] targets) throws FakeException {
		List<Rule> rules = new ArrayList<Rule>();
		for (String target : targets) {
			Rule rule = getRule(target);
			if (rule == null)
				throw new FakeException("Rule for target '" + target + "' not found!");
			rules.add(rule);
		}
		return buildDependencyMap(rules);
	}

	/**
	 * Builds a dependency map of the given targets.
	 *
	 * This guarantees that the order of the keySet is suitable for building, i.e. all
	 * rules depending on a given rule will be returned later by keySet().iterator().
	 */
	public Map<Rule, List<Rule>> buildDependencyMap(List<Rule> targets) throws FakeException {
		LinkedHashMap<Rule, List<Rule>> result = new LinkedHashMap<Rule, List<Rule>>();
		for (Rule target : targets)
			buildDependencyMap(result, target, null, new HashMap<Rule, Integer>(), 0);
		return result;
	}

	protected void buildDependencyMap(Map<Rule, List<Rule>> map, Rule target, Rule neededBy, Map<Rule, Integer> degrees, int degree) throws FakeException {
		List<Rule> depending = map.get(target);
		if (depending != null) {
			if (neededBy != null)
				depending.add(neededBy);
			return;
		}

		if (degrees.get(target) != null)
			throw new FakeException("Cycle detected: " + getCycle(target, map, degree - degrees.get(target).intValue()));
		degrees.put(target, new Integer(degree));

		for (Rule rule : target.getDependencies())
			buildDependencyMap(map, rule, target, degrees, degree + 1);

		List<Rule> list = neededBy == null ? Collections.<Rule>emptyList() : Collections.singletonList(neededBy);
		map.put(target, new ArrayList<Rule>(list));
		degrees.remove(target);
	}

	/**
	 * Given a graph, find a cycle of a given distance.
	 *
	 * This does a standard backtrack search. Likely has a horrible runtime, but it is probably not worth optimizing for.
	 */
	protected String getCycle(Rule target, Map<Rule, List<Rule>> map, int distance) throws FakeException {
		Stack<Rule> stack = new Stack<Rule>();
		stack.push(target);
		getCycle(stack, map, distance);

		String result = stack.pop().target;
		while (!stack.empty())
			result += " -> " + stack.pop().target;
		return result;
	}

	protected Stack<Rule> getCycle(Stack<Rule> partialCycle, Map<Rule, List<Rule>> map, int distance) throws FakeException {
		if (distance == 0 && partialCycle.get(0) == partialCycle.peek())
			return partialCycle;
		for (Rule rule : partialCycle.peek().getDependencies()) {
			partialCycle.push(rule);
			if (getCycle(partialCycle, map, distance - 1) != null)
				return partialCycle;
			partialCycle.pop();
		}
		return null;
	}

	public Rule parseRules(List<String> targets) throws FakeException {
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

		// add special <target>-<suffix> rules (-clean, -clean-dry-run, etc)
		for (String key : new ArrayList<String>(allRules.keySet())) {
			final Rule rule = getRule(key);
			if (key.endsWith("-clean") ||
					key.endsWith("-clean-dry-run") ||
					key.endsWith("-dependency-map") ||
					key.endsWith("-rebuild") ||
					(rule instanceof Special))
				continue;
			final String cleanKey = key + "-clean";
			// avoid concurrent modification
			if (!allRules.containsKey(cleanKey))
				addSpecialRule(new Special(Parser.this, cleanKey) {
					void action() { rule.clean(false); }
				});
			final String dryRunCleanKey = cleanKey + "-dry-run";
			if (!allRules.containsKey(dryRunCleanKey))
				addSpecialRule(new Special(Parser.this, dryRunCleanKey) {
					void action() { rule.clean(true); }
				});
			final String dependencyMapKey = key + "-dependency-map";
			if (!allRules.containsKey(dependencyMapKey))
				addSpecialRule(new Special(Parser.this, dependencyMapKey) {
					void action() throws FakeException {
						showMap(buildDependencyMap(Collections.singletonList(rule)));
					}
				});
			final String rebuildKey = key + "-rebuild";
			if (!allRules.containsKey(rebuildKey))
				addSpecialRule(new Special(Parser.this, rebuildKey) {
					void action() throws FakeException {
						rule.clean(false);
						rule.action();
					}
				});
		}

		lineNumber = -1;

		result = allRule;
		if (result == null)
			error("Could not find default rule");

		checkVariableNames();

		if (targets != null) {
			for (int i = 0; i < targets.size(); i++) {
				if (!allRules.containsKey(targets.get(i))) {
					Matcher matcher = Fake.matchVersionedFilename(targets.get(i));
					if (matcher.matches())
						targets.set(i, matcher.group(1) + matcher.group(5));
				}
			}
			result = new All(this, "", targets);
			allRules.put("", result);
		}

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
			for (String prereq : new ArrayList<String>(allPrerequisites)) {
				if (allRules.containsKey(prereq))
					continue;
				if (!filter.accept(null, prereq))
					continue;
				rule = addRule(prereq
					+ filter.replace(program),
					filter.replace(prerequisites));
			}
			return rule;
		}

		List<String> list = new ArrayList<String>();
		StringTokenizer tokenizer = new
			StringTokenizer(expandVariables(prerequisites,
						target), " \t\n");

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (fake.expandGlob(token, list, cwd, 0, buildDir)
					+ addMatchingTargets(token, list)
					== 0)
				throw new FakeException("Glob did not "
					+ "match any file: '"
					+ token + "'");
		}

		String lastPrereq = list.size() == 0 ? null :
			list.get(list.size() - 1);

		if (allRule == null)
			rule = allRule = new All(this, target, list);
		else if (target.endsWith("]")) {
			int paren = target.indexOf('[');

			if (paren < 0)
				throw new FakeException("Invalid rule");

			String program = target.substring(paren + 1,
				target.length() - 1);
			target = target.substring(0, paren).trim();

			rule = new ExecuteProgram(this, target, list,
				program);
		}
		else if (isSubmodule(lastPrereq))
			rule = new SubFake(this, target, list);
		else if (target.endsWith(".jar")) {
			String expanded = expandVariables(prerequisites, target);
			if (expanded.endsWith(".jar"))
				rule = new CopyJar(this, target, list);
			else if (expanded.endsWith("/pom.xml")) {
				int i = list.size() - 1;
				String last = list.get(i);
				last = last.substring(0, last.length() - 7);
				list.set(i, last);
				rule = new SubFake(this, target, list);
			}
			else
				rule = new CompileJar(this, target, list);
		}
		else if (prerequisites.endsWith(".c") ||
				prerequisites.endsWith(".cxx"))
			rule = new CompileCProgram(this, target, list);
		else if (target.endsWith(".class"))
			rule = new CompileClass(this, target, list);
		if (rule == null)
			throw new FakeException("Unrecognized rule");

		rule.prerequisiteString = prerequisites;

		allRules.put(target, rule);

		for (String prereq : list)
			allPrerequisites.add(prereq);

		return rule;
	}

	boolean isSubmodule(String directory) {
		if (directory == null)
			return false;
		File dir = new File(Util.makePath(cwd, directory));
		return dir.isDirectory() ||
			(!dir.exists() && directory.endsWith("/") &&
			 allRules.get(Util.stripSuffix(directory, "/")) == null);
	}

	int addMatchingTargets(String glob, List<String> sortedPrereqs) {
		if (glob.indexOf('*') < 0)
			return 0;
		int count = 0;
		GlobFilter filter = new GlobFilter(glob);
		for (String target : new ArrayList<String>(allRules.keySet())) {
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

	protected Map<String, Object> variables = new HashMap<String, Object>();

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

	public boolean isVarName(String key, String name) {
		if (key == null || name == null)
			return false;
		key = key.toUpperCase();
		name = name.toUpperCase();
		return key.equals(name) || key.startsWith(name + "(");
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
		Set<String> variableNames = new HashSet<String>();
		for (;;) {
			int dollar = value.indexOf('$', offset);
			if (dollar < 0)
				break;
			offset = getVariableNameEnd(value, dollar + 1);
			variableNames.add(value.substring(dollar + 1,
						offset).toUpperCase());
		}
		key = key.toUpperCase();
		for (String var : new ArrayList<String>(variables.keySet())) {
			int paren = var.indexOf('(');
			if (paren < 0)
				continue;
			String name = var.substring(0, paren);
			if (!variableNames.contains(name))
				continue;
			setVariable(key + var.substring(paren), value);
		}
	}

	public void setVariable(String key, Object value)
			throws FakeException {
		Object origValue = value;
		int paren = key.indexOf('(');
		String name = (paren < 0 ? key :
			key.substring(0, paren)).toUpperCase();

		if (key.charAt(paren + 1) == '*') {
			setVariableWildcard(name, value.toString());
			return;
		}

		String key2 = null;
		if (name.equals("ENVOVERRIDES"))
			key2 = key.substring(paren + 1, key.length() - 1);
		else if ("true".equals(variables.get("ENVOVERRIDES(" + key + ")")))
			key2 = key;
		if (key2 != null) {
			Object value2 = System.getenv(key2);
			if (value2 != null) {
				variables.put(key2, value2);
				return;
			}
		}

		if (isVarName(name, "CLASSPATH") || isVarName(name, "TOOLSPATH") || isVarName(name, "TOOLS_JAR")  || isVarName(name, "FIJI_JAVA_HOME"))
			value = fake.prefixPaths(cwd, value.toString(), true);

		if (value instanceof String) {
			String string = (String)value;
			string = expandVariables(string, paren < 0 ? null :
				key.substring(paren + 1, key.length() - 1));

			if (string.indexOf('*') >= 0 ||
					string.indexOf('?') >= 0) {
				String separator = isVarName(name, "CLASSPATH") ?
					":" : " ";
				List<String> files = new ArrayList<String>();
				StringTokenizer tokenizer = new
					StringTokenizer(string.replace('\t',
							' '), separator);
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (fake.expandGlob(token, files, cwd, 0,
								buildDir) < 1)
						fake.err.println("Warning: "
							+ "no match for "
							+ token);
				}
				string = "";
				if (separator.equals(":"))
					separator = File.separator;
				for (String file : files)
					string += separator + Fake.quoteArg(file);
			}
			value = string;
		}

		String origName = name.toUpperCase() + "_UNEXPANDED"
			+ (paren < 0 ? "" : key.substring(paren));
		if (!variables.containsKey(origName))
			variables.put(origName, origValue);
		name = name.toUpperCase() + (paren < 0 ?
			"" : key.substring(paren));
		variables.put(name, value);
		if (name.equals("BUILDDIR"))
			buildDir = value.toString();
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
			else {
				Object object = variables.get(
					name.substring(0, paren).toUpperCase()
					+ name.substring(paren));
				substitute = object == null ? null : object.toString();
			}
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
		for (String key : variables.keySet()) {
			int paren = key.indexOf('(');
			if (paren < 0 || !key.endsWith(")") ||
					key.startsWith("ENVOVERRIDES_UNEXPANDED(") ||
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
		Object res = null;
		key = key.toUpperCase();
		if (subkey != null && res == null)
			res = variables.get(key
					+ "(" + subkey + ")");
		if (subkey2 != null && res == null)
			res = variables.get(key
					+ "(" + subkey2 + ")");
		if (res == null && Util.getPlatform().equals("macosx")) {
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
				res = variables.get(key
					+ "(osx10." + (i--) + ")");
		}
		if (res == null)
			res = variables.get(key
					+ "(" + Util.getPlatform() + ")");
		if (res == null)
			res = variables.get(key);
		return res == null ? null : res.toString();
	}

	public void dumpVariables() {
		fake.err.println("Variable dump:");
		for (String key : variables.keySet())
			fake.err.println(key + " = " + variables.get(key));
	}

	public void missingPrecompiledFallBack(String target)
			throws FakeException {
		Rule fallBack = getRule("missingPrecompiledFallBack");
		if (fallBack == null)
			throw new FakeException("No precompiled and "
				+ "no fallback for " + target + "!");
		fallBack = fallBack.copy();
		fallBack.target = target;
		fallBack.make();
	}

	public Rule getRule(String rule) {
		return allRules.get(rule);
	}

	public Map<String, Rule> getAllRules() {
		return allRules;
	}

	public Rule getDefaultRule() {
		for (Rule rule : allRules.values())
			if (rule instanceof All)
				return rule;
		return null;
	}
}