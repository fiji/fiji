//
// AlgorithmParser.java
//

/*
Simple imglib algorithm launcher.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import ij.IJ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/** Helper class for parsing algorithm configuration file. */
public class AlgorithmParser<T extends RealType<T>> {

	// -- Constants --

	// patterns for regular expression parsing
	private static final String WS = "\\s*";
	private static final String P_LABEL = "([^:]*):";
	private static final String P_CLASS = "([^()\\s]+)";
	private static final String P_ARG_TYPE = "(\\w+)";
	private static final String P_ARG_NAME = "(\\w+)";
	private static final String P_ARG_VALUE = "([^\\s()]+)";
	private static final String P_ARG_ASSIGN =
		"(=" + WS + P_ARG_VALUE + ")?";
	private static final String P_ARG =
		P_ARG_TYPE + WS + P_ARG_NAME + WS + P_ARG_ASSIGN;
	private static final String P_ARG_LIST = "(\\((" +
		WS + P_ARG + WS + ",?" + WS + ")+\\))";
	private static final String P_LINE = P_LABEL +
		WS + P_CLASS + WS + P_ARG_LIST;
	private static final Pattern LINE_PATTERN = Pattern.compile(P_LINE);
	private static final Pattern ARG_PATTERN = Pattern.compile(P_ARG);

	/** Table of lookups from short class names to class objects. */
	private static final HashMap<String, Class<?>> CLASS_SHORTCUTS =
		createClassShortcuts();
	private static HashMap<String, Class<?>> createClassShortcuts() {
		HashMap<String, Class<?>> shortcuts = new HashMap<String, Class<?>>();
		shortcuts.put("Image", Image.class);
		shortcuts.put("byte", byte.class);
		shortcuts.put("Byte", Byte.class);
		shortcuts.put("char", char.class);
		shortcuts.put("Character", Character.class);
		shortcuts.put("boolean", boolean.class);
		shortcuts.put("Boolean", Boolean.class);
		shortcuts.put("double", double.class);
		shortcuts.put("Double", Double.class);
		shortcuts.put("float", float.class);
		shortcuts.put("Float", Float.class);
		shortcuts.put("int", int.class);
		shortcuts.put("Integer", Integer.class);
		shortcuts.put("long", long.class);
		shortcuts.put("Long", Long.class);
		shortcuts.put("short", short.class);
		shortcuts.put("Short", Short.class);
		shortcuts.put("String", String.class);
		shortcuts.put("BigDecimal", BigDecimal.class);
		shortcuts.put("BigInteger", BigInteger.class);
		return shortcuts;
	}

	// -- AlgorithmParser API methods --

	public ArrayList<AlgorithmInfo<T>> parse(String configPath)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(
			getClass().getResourceAsStream(configPath)));
		ArrayList<AlgorithmInfo<T>> infoList =
			new ArrayList<AlgorithmInfo<T>>();
		int lineNo = 0;
		while (true) {
			lineNo++;
			String line = in.readLine();
			if (line == null) break;
			int hash = line.indexOf("#");
			if (hash >= 0) line = line.substring(0, hash); // strip comments
			line = line.trim();
			if (line.equals("")) continue; // skip blank lines

			// parse label and class name
			Matcher matcher = LINE_PATTERN.matcher(line);
			if (!matcher.find()) {
				IJ.error("AlgorithmParser: Ignoring malformed line #" + lineNo);
				continue;
			}
			String label = matcher.group(1);
			String className = matcher.group(2);
			String argList = matcher.group(3);

			// parse constructor argument list
			matcher = ARG_PATTERN.matcher(argList);
			ArrayList<String> argTypeList = new ArrayList<String>();
			ArrayList<String> argNameList = new ArrayList<String>();
			ArrayList<String> argValueList = new ArrayList<String>();
			while (matcher.find()) {
				String type = matcher.group(1);
				String name = matcher.group(2);
				String value = matcher.groupCount() >= 4 ? matcher.group(4) : "";
				argTypeList.add(type);
				argNameList.add(name);
				argValueList.add(value);
			}
			addAlgorithm(infoList, label, className,
				argTypeList, argNameList, argValueList);
		}
		in.close();
		return infoList;
	}

	// -- Helper methods --

	private void addAlgorithm(ArrayList<AlgorithmInfo<T>> infoList,
		String label, String className, ArrayList<String> argTypeList,
		ArrayList<String> argNameList, ArrayList<String> argValueList)
	{
		Exception exc = null;
		try {
			// verify algorithm class is present
			Class<? extends OutputAlgorithm> algClass =
				Class.forName(className).asSubclass(OutputAlgorithm.class);

			// verify constructor argument classes are present
			Class<?>[] argTypes = new Class<?>[argTypeList.size()];
			for (int i=0; i<argTypes.length; i++) {
				String argType = argTypeList.get(i);
				Class<?> argClass = CLASS_SHORTCUTS.get(argType);
				if (argClass == null) argClass = Class.forName(argType);
				argTypes[i] = argClass;
			}
			Constructor<? extends OutputAlgorithm> con =
				algClass.getConstructor(argTypes);
			String[] argNames = argNameList.toArray(new String[0]);
			String[] argValues = argValueList.toArray(new String[0]);

			// save algorithm information to the list
			AlgorithmInfo<T> info =
				new AlgorithmInfo<T>(label, argTypes, argNames, argValues, con);
			infoList.add(info);
		}
		catch (ClassNotFoundException e) { exc = e; }
		catch (NoSuchMethodException e) { exc = e; }
		if (exc != null) IJ.handleException(exc);
	}

}
