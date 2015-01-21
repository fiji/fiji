// run all the scripts provided in plugins/Scripts/Plugins/AutoRun/
autoRunDirectory = getDirectory("imagej") + "/plugins/Scripts/Plugins/AutoRun/";
if (File.isDirectory(autoRunDirectory)) {
    list = getFileList(autoRunDirectory);
    // make sure startup order is consistent
    Array.sort(list);
    for (i = 0; i < list.length; i++) {
        runMacro(autoRunDirectory + list[i]);
    }
}

