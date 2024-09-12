// run all the scripts provided in the following directories
autoRunDirs = newArray(2);
autoRunDirs[0] = getDirectory("imagej") + "/plugins/Scripts/Plugins/AutoRun/";
autoRunDirs[1] = getDirectory("imagej") + "/scripts/Plugins/AutoRun/";

for (d=0; d<autoRunDirs.length; d++) {
    autoRunDir = autoRunDirs[d];
    if (File.isDirectory(autoRunDir)) {
        list = getFileList(autoRunDir);
        // make sure startup order is consistent
        Array.sort(list);
        for (i = 0; i < list.length; i++) {
            runMacro(autoRunDir + list[i]);
        }
    }
}
