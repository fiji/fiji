package util;

public class XMLFunctions {
	private static void replaceAll( StringBuffer s, String substring, String replacement ) {
		int fromIndex = 0;
		while (true) {
			int foundIndex = s.indexOf(substring,fromIndex);
			if( foundIndex >= 0 ) {
				int afterEnd = foundIndex + substring.length();
				s.replace(foundIndex,afterEnd,replacement);
				fromIndex = afterEnd;
			} else
				break;
		}
	}

	// This is quite ineffficient, but not expected to be a serious problem:
	public static String escapeForXMLAttributeValue( String s ) {
		StringBuffer sb = new StringBuffer(s);
		replaceAll( sb, "&", "&amp;" );
		replaceAll( sb, "<", "&lt;" );
		replaceAll( sb, ">", "&gt;" );
		replaceAll( sb, "'", "&apos;" );
		replaceAll( sb, "\"", "&quot;" );
		return sb.toString();
	}
}
