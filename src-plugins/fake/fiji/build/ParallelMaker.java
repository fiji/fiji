package fiji.build;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ParallelMaker {
	protected Parser parser;
	protected ExecutorService pool;
	protected Map<Rule, FutureTask<FakeException>> futures;
	protected Map<Rule, List<Rule>> dependencyMap;
	protected Map<Rule, FakeException> results;
	protected Rule finalRule;

	protected final FakeException success = new FakeException("Dummy for success");

	public ParallelMaker(Parser parser, final int maxThreads, final List<Rule> targets) throws FakeException {
		this.parser = parser;
		futures = new HashMap<Rule, FutureTask<FakeException>>();
		results = new LinkedHashMap<Rule, FakeException>();
		pool = Executors.newFixedThreadPool(maxThreads);

		finalRule = getFinalRule(targets);
		List<Rule> rules = new ArrayList<Rule>(targets);
		rules.add(finalRule);
		dependencyMap = parser.buildDependencyMap(rules);
	}

	protected Rule getFinalRule(final List<Rule> targets) {
		return new Rule(parser, "final", new ArrayList<String>()) {
			public Iterable<Rule> getDependencies() {
				return targets;
			}

			public boolean checkUpToDate() {
				return false;
			}

			public void action() {
				finalRule.notify();
			}
		};
	}

	public FakeException run() {
		// First, make sure that the status is determined
		for (Rule rule : dependencyMap.keySet())
			if (!rule.upToDate())
				rule.verbose("Not up-to-date: " + rule.target);

		// Then, make sure that certain components are built first
		for (String target : new String[] { "jars/javac.jar", "jars/fake.jar" }) {
			Rule rule = parser.getRule(target);
			if (rule != null && dependencyMap.containsKey(rule)) try {
				rule.make();
				results.put(rule, success);
			} catch (FakeException e) {
				results.put(rule, e);
			}
		}

		// Now, queue all rules that can be made already
		for (Rule rule : dependencyMap.keySet())
			submitTaskIfReady(rule);

		synchronized (finalRule) {
			try {
				finalRule.wait();
			} catch (InterruptedException e) {
				parser.fake.err.println("Interrupted: " + e);
			}
		}
		FakeException result = results.get(finalRule);
		pool.shutdown();

		for (Rule rule : new ArrayList<Rule>(results.keySet()))
			if (results.get(rule) == success) try {
				rule.setUpToDate();
			} catch (IOException e) {
				results.put(rule, new FakeException("Could not set up-to-date: " + e));
			}

		return result == success ? null : result;
	}

	/**
	 * Schedule a rule for building if all dependencies were done already.
	 *
	 * This should only ever be called from a synchronized(futures) block.
	 */
	protected void submitTaskIfReady(final Rule rule) {
		synchronized (futures) {
			if (results.get(rule) != null)
				return;
			FakeException result = allDependenciesDone(rule);
			if (result == null)
				return;
			if (rule == finalRule) {
				results.put(rule, result);
				synchronized (finalRule) {
					finalRule.notify();
				}
				return;
			}
			if (result != success) {
				parser.fake.err.println(rule.target + ": " + result.getMessage());
				results.put(rule, result);
				submitDependencees(rule);
				return;
			}
			if (futures.get(rule) != null)
				return;
			futures.put(rule, new FutureTask<FakeException>(new Callable<FakeException>() {
				public FakeException call() {
					return make(rule);
				}
			}));
			pool.submit(futures.get(rule));
		}
	}

	protected void submitDependencees(Rule rule) {
		synchronized (futures) {
			for (Rule dependencee : dependencyMap.get(rule))
				submitTaskIfReady(dependencee);
		}
	}

	protected FakeException make(Rule rule) {
		FakeException result = success;
		try {
			rule.make(false);
		} catch (FakeException e) {
			parser.fake.err.println("Failed: " + rule.target);
			result = e;
		}
		synchronized (futures) {
			results.put(rule, result);
			submitDependencees(rule);
		}
		return result;
	}

	/* This method _must_ be called in a synchronized(futures) block */
	protected FakeException allDependenciesDone(final Rule rule) {
		List<Rule> failed = new ArrayList<Rule>();
		try {
			for (Rule dependency : rule.getDependencies())
				if (results.get(dependency) == null)
					return null;
				else if (results.get(dependency) != success)
					failed.add(dependency);
			return failed.size() > 0 ? new DependencyFakeException(failed) : success;
		} catch (FakeException e) {
			return e;
		}
	}

	protected class DependencyFakeException extends FakeException {
		public DependencyFakeException(List<Rule> rules) {
			super("Problem in dependenc" + (rules.size() == 1 ? "y" : "ies")
				+ " " + getTargetsAsString(rules));
		}
	}

	protected String getTargetsAsString(Iterable<Rule> rules) {
		StringBuffer buffer = new StringBuffer();
		Iterator<Rule> iter = rules.iterator();
		if (iter.hasNext())
			buffer.append(iter.next().target);
		while (iter.hasNext())
			buffer.append(" ").append(iter.next().target);
		return buffer.toString();
	}

	/* Debug function; must be called in a synchronized(futures) block */
	protected List<Rule> unfinished() {
		List<Rule> result = new ArrayList<Rule>();
		for (Rule rule : dependencyMap.keySet())
			if (results.get(rule) == null)
				result.add(rule);
		return result;
	}

	public List<Rule> unfinishedBecause(Rule rule) throws FakeException {
		List<Rule> result = new ArrayList<Rule>();
		for (Rule dependency : rule.getDependencies())
			if (results.get(dependency) == null)
				result.add(dependency);
		return result;
	}
}


