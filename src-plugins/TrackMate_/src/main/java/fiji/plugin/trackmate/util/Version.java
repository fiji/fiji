package fiji.plugin.trackmate.util;

/**
 * A utility class to deal with version numbers. Found on StackOverflow:
 * http://stackoverflow.com/a/11024200/201698
 */
public class Version implements Comparable<Version> {

    private final String version;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        if(version == null)
            throw new IllegalArgumentException("Version can not be null");
        // remove post dash stuff
        final int dash = version.indexOf('-');
        if (dash > 0)
        	version = version.substring(0, dash);
        if(!version.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format: " + version);
        this.version = version;
    }

    @Override
    public String toString() {
    	return version;
    }

    @Override public int compareTo(final Version that) {
        if(that == null)
            return 1;
        final String[] thisParts = this.get().split("\\.");
        final String[] thatParts = that.get().split("\\.");
        final int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            final int thisPart = i < thisParts.length ?
                Integer.parseInt(thisParts[i]) : 0;
            final int thatPart = i < thatParts.length ?
                Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override public boolean equals(final Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((Version) that) == 0;
    }

    @Override
    public int hashCode() {
    	return version.hashCode();
    }

}
