package fiji.ffmpeg;

// This class is not meant to be a fully-fledged
// .h -> JNA converter as JNAerator tries to do.
// It is meant to be just good enough for FFMPEG.

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Stack;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateFFMPEGClasses {
	protected static class LineIterator {
		protected String contents;
		protected int offset, nextOffset;
		protected boolean breakAtSpecials;
		protected Stack<String> stack = new Stack<String>();

		public LineIterator(final String contents) {
			this(contents, false);
		}

		public LineIterator(final String contents, final boolean breakAtSpecials) {
			this.contents = contents;
			this.breakAtSpecials = breakAtSpecials;
			offset = 0;
			nextOffset = -2;
		}

		public boolean next() {
			if (!stack.isEmpty())
				return true;
			if (nextOffset == -1 || (nextOffset == contents.length() - 1 && contents.charAt(nextOffset) == '\n'))
				return false;

			offset = nextOffset < 0 ? 0 : nextOffset + (contents.charAt(nextOffset) == '\n' ? 1 : 0);
			nextOffset = contents.indexOf('\n', offset);
			if (breakAtSpecials) {
				for (int off = offset + 1; off < nextOffset; off++) {
					char c = contents.charAt(off);
					if ("{;".indexOf(c) >= 0) {
						nextOffset = off + 1;
						break;
					}
				}
			}
			return true;
		}

		public String getLine() {
			if (!stack.isEmpty())
				return stack.pop();
			if (nextOffset < 0)
				return contents.substring(offset);
			return contents.substring(offset, nextOffset);
		}

		public void push(final String line) {
			stack.push(line);
		}
	}

	private String filterOutIf0(final String contents) {
		StringBuffer buf = new StringBuffer();
		LineIterator iter = new LineIterator(contents);
		int nest = 0;
		while (iter.next()) {
			String line = iter.getLine();
			if (nest > 0) {
				if (line.trim().startsWith("#if"))
					nest++;
				else if (line.trim().startsWith("#endif"))
					nest--;
			}
			else if (line.trim().startsWith("#if 0"))
				nest++;
			else
				buf.append(line).append('\n');
		}
		return buf.toString();
	}

	private String reduceEmptyLines(final String contents) {
		StringBuffer buf = new StringBuffer();
		int offset = 0, length = contents.length();
		while (offset < length && contents.charAt(offset) == '\n')
			offset++;
		for (; offset < length; offset++) {
			char c = contents.charAt(offset);
			if (offset + 1 < length && c == '\n' && contents.charAt(offset + 1) == '\n') {
				offset++;
				while (offset + 1 < length && contents.charAt(offset + 1) == '\n')
					offset++;
				if (offset + 1 < length)
					buf.append(c);
			}
			buf.append(c);
		}
		return buf.toString();
	}

	private String filterOutComments(final String contents) {
		StringBuffer buf = new StringBuffer();
		int length = contents.length();
		for (int offset = 0; offset < length; offset++) {
			char c = contents.charAt(offset);
			if (c == '"') {
				buf.append(c);
				while (++offset < length) {
					c = contents.charAt(offset);
					buf.append(c);
					if (c == '\\')
						buf.append(contents.charAt(++offset));
					else if (c == '"')
						break;
				}
				continue;
			}
			if (c == '/' && offset + 1 < length) {
				switch (contents.charAt(offset + 1)) {
					case '/':
						offset++;
						while (++offset < length)
							if (contents.charAt(offset) == '\n')
								break;
						buf.append('\n');
						break;
					case '*':
						offset++;
						while (++offset < length)
							if (offset + 2 > length ||
									contents.substring(offset, offset + 2).equals("*/")) {
								offset++;
								break;
							}
						break;
					default:
						buf.append(c);
				}
			}
			else
				buf.append(c);
		}
		return buf.toString().trim();
	}

	private String indent(final String contents, final int indentLevel) {
		int level = indentLevel;
		if (level < 1)
			return contents;
		String tabs = "\t";
		while (level-- > 1)
			tabs += "\t";
		LineIterator iter = new LineIterator(contents);
		StringBuffer buf = new StringBuffer();
		while (iter.next()) {
			String line = iter.getLine();
			if (!line.equals(""))
				buf.append(tabs);
			buf.append(line).append("\n");
		}
		return buf.toString();
	}

	private static Pattern compile(final String regex) {
		return Pattern.compile(regex
			.replace("PARAMTYPE", "(?:TYPE|TYPE *\\(\\*IDENT\\)\\([^\\)]*\\))")
			.replace("PARAMNAME", "IDENT(?:\\[[^\\]]*\\])*")
			.replace("TYPE", "(?:unsigned |const )*(?:unsigned|void|char|short|int|long|float|double|u?int(?:8|16|32|64)_t|"
				+ "enum IDENT|struct\\s+IDENT|AV[A-Za-z_0-9]+|"
				+ "ReSampleContext|RcOverride|ByteIOContext|SwsContext|SwsFilter|SwsVector|URLContext|)\\**\\s*?(?:(?:const\\s*?)?\\*\\s*?(?:const\\s*?)?)*")
			.replace("IDENT", "[A-Za-z_][A-Za-z_0-9]*")
			.replace(" *", "\\s*")
			.replace(" ", "\\s+")
			.replace("SPACE", " "), Pattern.DOTALL);
	}

	private static Pattern guardPattern =
		compile("^#ifndef (IDENT)[^\n]*\n"
		+ " *#define (IDENT)[^\n]*\n"
		+ "(.*\n|)"
		+ " *#endif[^\n]*\n?$");
	private static Pattern definePattern =
		compile("^# *define (IDENT) *(.*) *$");
	private static Pattern externVariablePattern =
		compile("^extern (TYPE) *(IDENT); *$");
	private static Pattern staticVariablePattern =
		compile("^static (TYPE) *(IDENT)( *\\[[^\\]]*\\])*( *=.*)?; *$");
	private static Pattern functionPattern =
		compile("^(?:extern )?(TYPE) *(IDENT)\\((void|((PARAMTYPE *(?:PARAMNAME)?, *)*PARAMTYPE *(?:PARAMNAME)?)?)\\) *(?:av_const *)?(?:av_malloc_attrib av_alloc_size\\(\\d+\\) *)?;$");
	private static Pattern callbackPattern =
		compile("^?(TYPE) *\\(\\*(IDENT)\\)\\((void|((PARAMTYPE *(?:PARAMNAME)?, *)*PARAMTYPE *(?:PARAMNAME)?)?)\\) *;$");
	private static Pattern motionValTablePattern =
		compile("^?(TYPE) *\\(\\*(IDENT)((?:\\[[^\\]]*\\])*)\\) *((?:\\[[^\\]]*\\])*);$");
	private static Pattern parameterPattern =
		compile("^(PARAMTYPE) *(PARAMNAME)?$");
	private static Pattern enumPattern =
		//compile("^enum (IDENT)? *\\{ *((IDENT( *= *-?\\d+)?,? *)*) *,? *\\};$");
		compile("^enum (IDENT)? *\\{([^\\}]*)\\};$");
	private static Pattern skipDefine =
		compile("^(?:AV_(?:STRINGIFY|TOSTRING|GLUE|JOIN|PRAGMA|VERSION_INT|VERSION_DOT|VERSION|NOPTS_VALUE|TIME_BASE_Q)"
			+ "|PIX_FMT_NE|CodecType|CODEC_TYPE_UNKNOWN|av_malloc_attrib"
			+ "|FF_MM_(?:FORCE|MMX|3DNOW|SSE|SSE2|SSE2SLOW|3DNOWEXT|SSE3|SSE3SLOW|SSSE3|SSE4|SSE42|IWMMXT|ALTIVEC)"
			+ ")"); // + "|(?:INT64_MIN|UINT64_MAX|INT_BIT|offsetof|LABEL_MANGLE|LOCAL_MANGLE|MANGLE|dprintf))");
	private static Pattern structPattern =
		compile("^(?:typedef )?struct (IDENT)? *\\{$");
	private static Pattern structMemberPattern =
		compile("^(TYPE) *(IDENT(?: *, *IDENT)*)((?:\\[([^\\]]+)\\])*); *$");
	private static Pattern bitFieldPattern =
		compile("^(?:unsigned )?int (IDENT):(\\d+);$");
	private static Pattern structEndPattern =
		compile("^\\} *(IDENT)? *(?: DECLARE_ALIGNED\\([^\\)], *(IDENT)*\\) *)?(?:attribute_deprecated *)?;$");
	private static Pattern privateStructs =
		compile("^(?:struct )?(SwsContext|AVSHA|AVAES|ReSampleContext|AVResampleContext|AVMetadata"
			+ "|AVMetadataConv|ByteIOContext|URLContext)$");

	private String commonFrame;

	private static String filterOutGuard(final String contents) {
		Matcher matcher = guardPattern.matcher(contents);
		if (matcher.matches() && matcher.group(1).trim().equals(matcher.group(2).trim()))
			return matcher.group(3);
		return contents;
	}

	private static Matcher match(final Pattern pattern, final String line) {
		Matcher matcher = pattern.matcher(line);
		return matcher.matches() ? matcher : null;
	}

	private static String replace(final String text, final String needle, final String replacement) {
		String haystack = text;
		int offset = haystack.indexOf(needle);
		while (offset >= 0) {
			haystack = haystack.substring(0, offset)
				+ replacement
				+ haystack.substring(offset + needle.length());
			offset = haystack.indexOf(needle, offset + replacement.length());
		}
		return haystack;
	}

	private static String capitalize(final String name) {
		if (name.length() == 0)
			return name;
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private static String camelCasify(final String text) {
		String name = text;
		if (name.length() == 0)
			return name;

		name = capitalize(name);
		for (int offset = name.indexOf('_', 1); offset > 0; offset = name.indexOf('_', offset))
			name = name.substring(0, offset) + capitalize(name.substring(offset + 1));

		return name;
	}

	private static String stripPrefix(final String string, final String prefix) {
		return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
	}

	private String currentLib;
	private TreeMap<String, String> name2lib = new TreeMap<String, String>();
	protected TreeMap<String, String> majorVersions = new TreeMap<String, String>();

	private String addLibPrefix(final String name) {
		String lib = name2lib.get(name);
		if (lib == null || lib.equals(currentLib))
			return name;
		return lib + "." + name;
	}

	private static String plusStars(final String string, final int starCount) {
		int stars = starCount;
		if (stars == 0)
			return string;
		String suffix = " ";
		while (--stars >= 0)
			suffix += "*";
		return string + suffix;
	}

	private String translateType(final String originalType, final boolean inStruct) {
		String type = originalType;
		if (type.equals("const char *"))
			return "String";
		if (type.startsWith("Pointer "))
			return type;

		type = type.replaceAll("\\s*const\\s*", " ").trim();
		int stars = 0;
		for (;;) {
			if (type.endsWith("*"))
				stars++;
			else if (!type.endsWith(" "))
				break;
			type = type.substring(0, type.length() - 1);
		}

		if (!inStruct && stars == 1 && match(privateStructs, type) != null)
			return "Pointer /* " + type + " * */";

		if (type.startsWith("const "))
			type = type.substring(6);
		if (type.startsWith("struct ") || Character.isUpperCase(type.charAt(0))) {
			type = stripPrefix(type, "struct ");
			type = addLibPrefix(type);
			if (inStruct) {
				if (stars == 0)
					return type;
				return "Pointer /* " + plusStars(type, stars) + " */";
			}
			if (stars == 0)
				return type + ".ByValue";
			if (stars == 1)
				return type;
			return "PointerByReference /* " + plusStars(type, stars) + " */";
		}
		if (type.equals("unsigned"))
			type = "int";
		else if (type.startsWith("unsigned "))
			type = type.substring(9);
		if (type.equals("uint8_t") || type.equals("int8_t") || type.equals("char"))
			type = "byte";
		else if (type.equals("long"))
			type = "NativeLong";
		else if (type.equals("uint64_t") || type.equals("int64_t"))
			type = "long";
		else if (type.equals("uint32_t") || type.equals("int32_t") || type.equals("AVCRC"))
			type = "int";
		else if (type.equals("uint16_t") || type.equals("int16_t"))
			type = "short";
		else if (type.startsWith("enum "))
			type = "int /* " + type + " */";
		if (stars == 0)
			return type;
		if (stars == 1)
			if (type.equals("byte")) {
				if (inStruct)
					return "Pointer";
				return "byte[]"; // assume that it's a buffer
			}
			else if (type.equals("void"))
				return "Pointer";
			else {
				int space = type.indexOf(" ");
				if (space < 0)
					return capitalize(type) + "ByReference";
				return capitalize(type.substring(0, space)) + "ByReference" + type.substring(space);
			}
		return "PointerByReference /* " + plusStars(type, stars) + " */";
	}

	private static boolean unbalancedParens(final String line) {
		int count = 0;
		for (char c : line.toCharArray())
			if (c == '(')
				count++;
			else if (c == ')')
				if (--count < 0)
					throw new RuntimeException("Negative paren balance: " + line);
		return count > 0;
	}

	private String functionParameters(final String original) {
		Matcher matcher;
		StringBuffer buf = new StringBuffer();
		int unnamedNumber = 1; // we call unnamed parameters param1, param2, etc
		boolean first = true;
		// Can't use 'for (param : ...)' since the split() can cut callbacks in the middle
		String[] parameters = original.split("\\s*,\\s*");
		for (int i = 0; i < parameters.length; i++) {
			String parameter = parameters[i];
			// Handle erroneously cut callbacks
			while (unbalancedParens(parameter) && i < parameters.length)
				parameter += parameters[++i];

			if (first)
				first = false;
			else
				buf.append(", ");
			if (parameter.equals("void"))
				continue;
			if ((matcher = match(parameterPattern, parameter)) != null) {
				String name = matcher.group(2);
				if (name == null)
					name = "param" + unnamedNumber++;
				else if (name.startsWith("param")) try {
					unnamedNumber = Math.max(unnamedNumber,
						1 + Integer.parseInt(stripPrefix(name, "param")));
				} catch (NumberFormatException e) { /* ignore */ }
				String type = matcher.group(1);
				if (type.indexOf('(') >= 0)
					type = "Pointer /* " + type + " */";
				else if (name.indexOf('[') >= 0) {
					int offset = name.indexOf('[');
					if (offset == name.length() - 2 && name.endsWith("[]")) {
						if (type.startsWith("const "))
							type = stripPrefix(type, "const ");
						if (type.indexOf("*") >= 0)
							type = "Pointer";
						type = type + "[]";
					}
					else
						type = "Pointer /* " + type + name.substring(offset) + " */";
					name = name.substring(0, offset);
				}
				else
					type = translateType(type, false);
				buf.append(type).append(" ").append(name);
			}
			else
				buf.append("UNHANDLED parameter ").append(parameter);
		}
		return buf.toString();
	}

	protected int bitFieldBitCount = 0, bitFieldCount = 1;
	protected StringBuffer bitFieldBuffer = new StringBuffer();

	protected void flushBitField(final StringBuffer out) {
		if (bitFieldBitCount == 0)
			return;
		String name = "bitfield" + (bitFieldCount++);
		out.append("public int[] ").append(name).append(" = new int[" + ((bitFieldCount + 31) / 32) + "];\n");
		out.append(bitFieldBuffer.toString());
		bitFieldBuffer.setLength(0);
		bitFieldBitCount = 0;
	}

	protected String bitField(final int shift, final int bits) {
		int index = shift / 32;
		int left = shift - index * 32;
		return (left > 0 ? "(" : "")
			+ "(bitfield" + bitFieldCount + "[" + index + "] "
			+ (left > 0 ? ">> " + left + ")": "")
			+ " & 0x" + Integer.toHexString((int)((1l << bits) - 1)) + ")";
	}

	protected String setBitField(final int shift, final int bits) {
		int index = shift / 32;
		int left = shift - index * 32;
		String mask = "0x" + Integer.toHexString(((1 << bits) - 1) << left);
		String var = "bitfield" + bitFieldCount + "[" + index + "]";
		return var + " = (" + var + " & ~" + mask
			+ ") | (" + (left > 0 ? "(value << " + left + ")" : "value") + " & " + mask + ");\n";
	}

	protected void stageBitFieldEntry(final String name, final int bitCount) {
		int bits = bitCount;
		if (bits > 31)
			throw new RuntimeException("Can only handle bitfield entries < 32 bit");
		// TODO: handle signed
		String setter = "public void " + name + "(int /* int:" + bits + " */ value) {\n";
		bitFieldBuffer.append("public int ").append(name + (name.equals("size") ? "_" : "")).append("() {\n\treturn ");
		int index = bitFieldBitCount / 32;
		int left = 32 - index * 32;
		if (left == 0)
			left = 32;
		while (bits > left) {
			bitFieldBuffer.append(bitField(bitFieldBitCount, left) + " | ");
			setter += "\t" + setBitField(bitFieldBitCount, left)
				+ "\n\tvalue >>= " + left + ";\n";
			bits -= left;
			bitFieldBitCount += left;
		}
		bitFieldBuffer.append(bitField(bitFieldBitCount, bits) + ";\n}\n"
			+ setter + "\t" + setBitField(bitFieldBitCount, bits) + "}\n");
		bitFieldBitCount += bits;
	}

	private String handleStruct(final String text, final int level, final LineIterator iter) {
		String line = text;
		if (level == 0 && bitFieldBitCount > 0)
			throw new RuntimeException("Bit fields not flushed!");
		Matcher matcher = match(structPattern, line);
		if (matcher == null)
			return null;
		String name = matcher.group(1);
		StringBuffer constants = new StringBuffer();
		StringBuffer buf = new StringBuffer();
		flushBitField(buf);
		while (iter.next()) {
			line = iter.getLine().trim();

			line = replace(line, "FF_COMMON_FRAME", commonFrame);
			line = replace(line, "unsigned char *buf_ptr, *buf_end;", "char *buf_ptr; char *buf_end;");

			int semicolon = line.indexOf(';');
			if (semicolon >= 0 && semicolon + 1 < line.length()) {
				iter.push(line.substring(semicolon + 1));
				line = line.substring(0, semicolon + 1);
			}

			if ((matcher = match(structEndPattern, line)) != null) {
				flushBitField(buf);
				if (matcher.group(1) != null)
					name = matcher.group(1);
				else if (matcher.group(2) != null)
					name = matcher.group(2);
				break;
			}

			if (level == 0 && (matcher = match(definePattern, line)) != null) {
				constants.append("public static final int ").append(matcher.group(1))
					.append(" = ").append(addLibPrefix(matcher.group(2))).append(";\n");
				name2lib.put(matcher.group(1), currentLib);
				continue;
			}

			String inner = handleStruct(line, level + 1, iter);
			if (inner != null) {
				flushBitField(buf);
				buf.append(inner);
				continue;
			}

			// Make sure that callback declarations are complete
			while (unbalancedParens(line))
				if (iter.next())
					line += iter.getLine();
				else
					throw new RuntimeException("EOF with unbalanced parens: " + line);

			if ((matcher = match(callbackPattern, line)) != null) {
				flushBitField(buf);
				String name2 = matcher.group(2);
				String callback = camelCasify(name2);
				buf.append("public static interface ").append(callback).append(" extends Callback {\n")
					.append("\tpublic ").append(translateType(matcher.group(1), true))
					.append(" callback(");
				buf.append(functionParameters(matcher.group(3)));
				buf.append(");\n}\npublic ").append(callback).append(" ").append(name2).append(";\n");
				continue;
			}

			if ((matcher = match(motionValTablePattern, line)) != null) {
				flushBitField(buf);
				String name2 = matcher.group(2);
				String suffix = matcher.group(3);
				buf.append("public Pointer").append(suffix.replaceAll("[^\\[\\]]", ""))
					.append(" ").append(name2).append(" = new Pointer").append(suffix)
					.append("; // ").append(line).append("\n");
				continue;
			}

			if ((matcher = match(bitFieldPattern, line)) != null) {
				stageBitFieldEntry(matcher.group(1), Integer.parseInt(matcher.group(2)));
				continue;
			}

			if ((matcher = match(structMemberPattern, line)) == null) {
				flushBitField(buf);
				if (!line.equals(""))
					buf.append("UNHANDLED: ").append(line).append("\n");
				continue;
			}

			flushBitField(buf);
			String type = matcher.group(1);
			String suffix = matcher.group(3);
			String originalType = type;
			type = translateType(type, true);
			if (!suffix.equals("")) {
				// add [][][] to type, turn suffix into
				String brackets = suffix.replaceAll("[^\\[\\]]", "");
				if (type.endsWith("[]"))
					type = "Pointer /* " + originalType + " */";
				int space = type.indexOf(' ');
				if (space < 0) {
					suffix = " = new " + type + suffix;
					type += brackets;
				}
				else {
					suffix = " = new " + type.substring(0, space) + suffix;
					type = type.substring(0, space) + brackets + type.substring(space);
				}
			}

			buf.append("public ").append(type)
				.append(" ").append(matcher.group(2))
				.append(suffix).append(";\n");
		}
		flushBitField(buf);
		if (level == 0)
			name2lib.put(name, currentLib);
		String prefix = constants.toString();
		if (!prefix.equals(""))
			prefix += "\n";

		return indent(prefix + "public static class " + name + " extends Structure {\n"
				+ "\tpublic static class ByValue extends " + name + " implements Structure.ByValue {\n"
				+ "\t\tpublic ByValue() {\n"
				+ "\t\t\tsuper();\n"
				+ "\t\t}\n"
				+ "\n"
				+ "\t\t// make a copy\n"
				+ "\t\tpublic ByValue(" + name + " from) {\n"
				+ "\t\t\tsuper();\n"
				+ "\t\t\tbyte[] buffer_ = new byte[size()];\n"
				+ "\t\t\tfrom.getPointer().read(0, buffer_, 0, buffer_.length);\n"
				+ "\t\t\tgetPointer().write(0, buffer_, 0, buffer_.length);\n"
				+ "\t\t\tread();\n"
				+ "\t\t}\n"
				+ "\t}\n"
				+ "\n"
				+ "\tpublic " + name + "() {\n"
				+ "\t\tsuper();\n"
				// need to calculate the size now, as array members are only initialized at this point
				+ "\t\tensureAllocated();\n"
				+ "\t}\n"
				+ "\n"
				+ "\tpublic " + name + "(Pointer p) {\n"
				// cannot use super(p); otherwise array members are not initialized and the wrong size is calculated!
				+ "\t\tsuper();\n"
				+ "\t\tuseMemory(p);\n"
				+ "\t\tread();\n"
				+ "\t}\n"
				+ "\n", level)
			+ indent(buf.toString(), level + 1)
			+ indent("}\n", level);
	}

	private static String handleMacro(final String text) {
		String value = text;
		String[] parameters = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')')).split("\\s*,\\s*");
		if (value.startsWith("AV_VERSION_INT(")) {
			value = ")";
			int shift = 0;
			for (int i = parameters.length - 1; i >= 0; i--, shift += 8)
				value = (i > 0 ? " | " : "")
					+ (shift > 0 ? "(" : "")
					+ parameters[i]
					+ (shift > 0 ? " << " + shift + ")" : "")
					+ value;
			return "(" + value;
		}
		if (value.startsWith("AV_VERSION(")) {
			value = "\"\" + " + parameters[0];
			for (int i = 1; i < parameters.length; i++)
				value += " + \".\" + " + parameters[i];
			return value;
		}
		if (value.startsWith("PIX_FMT_NE("))
			return "PIX_FMT_" + parameters[0];
		throw new RuntimeException("Don't know how to handle macro " + value);
	}

	private String toJNA(final String contents) {
		Matcher matcher;
		StringBuffer buf = new StringBuffer();
		LineIterator iter = new LineIterator(contents, false);
		while (iter.next()) {
			String line = iter.getLine().trim();

			// Handle backslashes at the end of the line
			while (line.endsWith("\\") && iter.next())
				line = line.substring(0, line.length() - 1) + iter.getLine();

			if (line.equals("")) {
				buf.append("\n");
				continue;
			}

			if (line.charAt(0) == '#') {
				if ((matcher = match(definePattern, line)) != null &&
						!skipDefine.matcher(matcher.group(1)).matches()) {
					if (matcher.group(1).endsWith("_MAJOR"))
						majorVersions.put(matcher.group(1), matcher.group(2));
					if (matcher.group(2).startsWith("(") &&
							line.charAt(line.indexOf(matcher.group(2)) - 1) != ' ')
						continue; // cannot handle macros
					String value = matcher.group(2).replaceAll("\\s+", " ")
						.replace("AV_STRINGIFY(", "+ (");
					if (matcher.group(1).equals("FF_COMMON_FRAME")) {
						commonFrame = value;
						continue;
					}
					String type = "int";
					if (value.startsWith("AV_VERSION(") || value.startsWith("AV_VERSION_INT(") ||
							value.startsWith("PIX_FMT_NE("))
						value = handleMacro(value);
					if (value.startsWith("\""))
						type = "String";
					else if (value.endsWith("LL") || value.endsWith("ll")) {
						type = "long";
						value = value.substring(0, value.length() - 1);
					}
					else if (value.endsWith("l"))
						type = "NativeLong";
					else if (value.startsWith("INT64_C(")) {
						type = "long";
						value = value.substring(8, value.length() - 1) + "l";
					}
					else if ((value.indexOf('<') > 0 || value.indexOf('>') > 0 || value.indexOf("==") > 0) && (value.indexOf("<<") < 0 && value.indexOf(">>") < 0))
						type = "boolean";
					else if (value.indexOf('.') >= 0)
						type = value.endsWith("f") ? "float" : "double";
					if (!value.equals("")) {
						name2lib.put(matcher.group(1), currentLib);
						buf.append("public static final ").append(type).append(" ")
							.append(matcher.group(1)).append(" = ").append(addLibPrefix(value)).append(";\n");
					}
				}
				continue;
			}

			// Handle structs
			String struct = handleStruct(line, 0, iter);
			if (struct != null) {
				buf.append(struct);
				continue;
			}

			// Read until end of statement
			while (!line.endsWith(";") && iter.next())
				line += "\n" + iter.getLine();

			int semicolon = line.indexOf(';');
			if (semicolon >= 0 && semicolon + 1 < line.length()) {
				iter.push(line.substring(semicolon + 1));
				line = line.substring(0, semicolon + 1);
			}

			// Handle extern variables
			if ((matcher = match(externVariablePattern, line)) != null)
				continue; // TODO: handle via library.getFunction(name).getInt(0)

			// Handle local variables
			if ((matcher = match(staticVariablePattern, line)) != null)
				continue;

			// Handle deprecated
			if (line.startsWith("attribute_deprecated")) {
				//buf.append("@deprecated\n");
				line = stripPrefix(line, "attribute_deprecated").trim();
			}

			// Make sure that function declarations are complete
			if (line.indexOf('(') > 0)
				while (line.indexOf(')') < 0 && iter.next())
					line += iter.getLine();

			// Handle function
			if ((matcher = match(functionPattern, line)) != null) {
				if (matcher.group(3).endsWith("va_list"))
					buf.append("/* Skipping vararg function ").append(line).append(" */\n");
				else if (line.equals("void avSetLogCallback(void (*callback)(const char *line));"))
					buf.append("public interface AvLog extends Callback {\n"
							+ "\tpublic void callback(String line);\n"
							+ "}\n"
							+ "public void avSetLogCallback(AvLog callback);\n");
				else
					buf.append(translateType(matcher.group(1), false))
					.append(" ").append(matcher.group(2))
					.append("(").append(functionParameters(matcher.group(3))).append(");\n");
				continue;
			}

			// Handle enums
			if ((matcher = match(enumPattern, line)) != null) {
				buf.append("// enum ");
				if (matcher.group(1) != null)
					buf.append(matcher.group(1));
				buf.append("\n");
				int number = 0;
				for (String item : matcher.group(2).split("\\s*,\\s*")) {
					int equals = item.indexOf('=');
					if (equals > 0) {
						number = parseInt(item.substring(equals + 1).trim());
						item = item.substring(0, equals);
					}
					item = item.trim();
					buf.append("public static final int ").append(item).append(" = " + number++).append(";\n");
					name2lib.put(item, currentLib);
				}
				continue;
			}

			buf.append("/* UNHANDLED: ").append(line).append(" */\n");
		}
		return buf.toString();
	}

	private static String readFile(final File path) throws IOException {
		FileReader reader = new FileReader(path);
		StringBuffer buf = new StringBuffer();
		char[] cBuf = new char[65536];
		for (;;) {
			int count = reader.read(cBuf);
			if (count < 0)
				break;
			buf.append(cBuf, 0, count);
		}
		reader.close();
		return buf.toString();
	}

	private static String preprocessWithoutDefines(final String contents) {
		StringBuffer buf = new StringBuffer();
		LineIterator iter = new LineIterator(contents);
		while (iter.next()) {
			String line = iter.getLine();
			while (line.endsWith("\\") && iter.next())
				line = line.substring(0, line.length() - 1) + iter.getLine();
			if (!line.startsWith("#") || definePattern.matcher(line).matches())
				buf.append(line).append("\n");
		}
		return buf.toString();
	}

	private void handleHeaders(final File pathToHeaders, final String libName, final String packageName, final File outDir) throws IOException {
		File outFile = new File(outDir, packageName.replace('.', '/') + "/" + libName + ".java");
		outFile.getParentFile().mkdirs();
		FileWriter out = new FileWriter(outFile);

		out.write("package " + packageName + ";\n\n");
		for (String c : new String[] {
				"Callback",
				"Library",
				"NativeLong",
				"Pointer",
				"Structure",
				"ptr.DoubleByReference",
				"ptr.IntByReference",
				"ptr.LongByReference",
				"ptr.PointerByReference",
				"ptr.ShortByReference"
		})
			out.write("import com.sun.jna." + c + ";\n");
		out.write("\n");
		out.write("public interface " + libName + " extends Library {\n");

		print("Generating " + libName);
		String[] list = libName.equals("AVUTIL") ? pathToHeaders.list() :
			(libName.equals("AVFORMAT") ?
			 new String[] { "avformat.h", "avio.h" } :
			 new String[] { libName.toLowerCase() + ".h" });
		Pattern ignorePattern =
			compile("(internal|timer|colorspace|attributes|bswap|intmath|intreadwrite|libm|common"
				+ "|error|crc|pixdesc|fifo|md5|tree|pca|sha1|x86_cpu|eval)\\.h");
		for (String file : list) {
			if (!file.endsWith(".h") || match(ignorePattern, file) != null)
				continue;
			print("Translating " + file);
			out.write("\n\t// Header: " + file + "\n");
			String contents = readFile(new File(pathToHeaders, file));
			contents = filterOutIf0(contents);
			contents = filterOutComments(contents);
			contents = filterOutGuard(contents).trim();
			contents = reduceEmptyLines(contents);
			contents = preprocessWithoutDefines(contents);
			contents = toJNA(contents);
			out.write(indent(contents, 1));
		}
		out.write("\t/* avoid compiler warnings */\n");
		out.write("\tinterface __Dummy__{\n");
		out.write("\t\tvoid __dummy__(Callback a, Library b, NativeLong c, Pointer d, Structure e, DoubleByReference f, IntByReference g, LongByReference h, PointerByReference i, ShortByReference j);\n");
		out.write("\t}\n");
		out.write("}\n");
		out.close();
	}

	public static int parseInt(final String number) {
		if (number.startsWith("0x"))
			return Integer.parseInt(number.substring(2), 16);
		if (number.length() > 1 && number.startsWith("0"))
			return Integer.parseInt(number.substring(1), 8);
		return Integer.parseInt(number);
	}

	public static void print(final String message) {
		System.err.println(message);
	}

	public static void handleException(final Exception e) {
		e.printStackTrace();
	}

	public static String addSlash(final String path) {
		if (path.endsWith("/"))
			return path;
		return path + "/";
	}

	public static void main(final String[] args) {
		if (args.length != 2) {
			print("Usage: generator <ffmpeg-dir> <output-dir>");
			System.exit(1);
		}
		String ffmpegDir = addSlash(args[0]);
		String outDir = addSlash(args[1]);
		new GenerateFFMPEGClasses().generate(new File(ffmpegDir), new File(outDir));
	}

	protected void generate(final File ffmpegDir, final File outDir) {
		for (String lib : new String[] { "avutil", "avcore", "avdevice", "swscale", /* "avfilter", */ "avcodec", "avformat", "avlog" }) {
			currentLib = lib.toUpperCase();
			try {
				handleHeaders(new File(ffmpegDir, "lib" + lib), currentLib, "fiji.ffmpeg", outDir);
			} catch (IOException e) {
				handleException(e);
				print("Could not handle " + lib + ": " + e);
			}
		}
	}
}