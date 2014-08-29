/*
 * This macro helps rename files with problematic file names, e.g. containing quotes, or
 * closing brackets (because the closing bracket is mistaken by ImageJ 1.x' macro language
 * to indicate the end of a macro value when passed as part of the run() function's second
 * parameter).
 * 
 * Example: if you record a macro to import an image sequence where the file names are of the
 * form x[1].tif, x[2].tif, x[3].tif, the recorded statement is:
 * 
 * 	run("Image Sequence...", "open=[C:\\imageDirectory\\x[1].tif] ...");
 * 
 * ImageJ 1.x misinterprets the closing bracket after the "1" to indicate that the path is
 * actually "C:\imageDirectory\x[1", i.e. it cuts off the full value. As a consequence, the
 * path is mistaken to indicate a directory instead of a file, and since that directory does
 * not exist, the macro will fail to open any image.
 * 
 * The workaround provided by this macro is to rename the files with problematic names,
 * substituting underscores for the closing brackets (and for other problematic characters
 * while we're at it, such as quotes, opening brackets, spaces, tabs, etc).
 * 
 * Suggested by Britta Schroth-Diez during the EMBO course "light-sheet microscopy" 2014.
 */

dir = getDirectory("Which directory contains file names with brackets?");
list = getFileList(dir);

/*
 * This function takes a list of file names and returns a list of file name pairs for
 * the problematic file names (a pair consists of the original file name and the suggested
 * fixed file name.
 */
function fixFileNames(list) {
	result = newArray(list.length * 2);
	j = 0;
	for (i = 0; i < list.length; i++) {
		fixedName = replace(list[i], "[\\]\\[\\\\ \t\"']", "_");
		if (list[i] != fixedName) {
			result[j++] = list[i];
			result[j++] = fixedName;
		}
	}
	if (j < result.length) {
		result = Array.trim(result, j);
	}
	return result;
}

todo = fixFileNames(list);
if (todo.length == 0) {
	exit("No file names need fixing!");
}

print("" + (todo.length / 2) + " files need to be renamed");

// Verify that the rename targets do not exist yet
for (i = 1; i < todo.length; i += 2) {
	if (File.exists(dir + todo[i])) {
		exit("Rename target '" + todo[i] + "' of '" + todo[i - 1] + "' already exists!");
	}
}

// Actually rename the problematic files
for (i = 0; i < todo.length; i += 2) {
	print("Renaming " + todo[i] + " to " + todo[i + 1]);
	File.rename(dir + todo[i], dir + todo[i + 1]);
}

print("Done!");