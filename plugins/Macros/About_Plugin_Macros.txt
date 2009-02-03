  path = getDirectory("plugins")+"/Macros/About Plugin Macros";
  if (!File.exists(path))
    exit("\"About Plugin Macros\" not found in ImageJ/plugins/Macros.");
  open(path);

