package fiji.plugin.trackmate.util;

import fiji.util.NumberParser;

/**
 * A utility class to deal with version numbers. Found on StackOverflow:
 * http://stackoverflow.com/a/11024200/201698
 */
public class Version implements Comparable<Version> {

    private String version;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        if(version == null)
            throw new IllegalArgumentException("Version can not be null");
        // remove post dash stuff
        int dash = version.indexOf('-');
        if (dash > 0) 
        	version = version.substring(0, dash);
        if(!version.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format: " + version);
        this.version = version;
    }

    @Override public int compareTo(Version that) {
        if(that == null)
            return 1;
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                NumberParser.parseInteger(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                NumberParser.parseInteger(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override public boolean equals(Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((Version) that) == 0;
    }

}
