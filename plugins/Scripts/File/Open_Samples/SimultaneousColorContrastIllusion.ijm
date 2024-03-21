// simultaneous colour contrast illusion
// same colour appears different in context of surrounding by other colours

newImage("SimultaneousColorContrastIllusion", "RGB White", 1000, 500, 1);
makeRectangle(15, 15, 470, 470);
setForegroundColor(255, 255, 0);
run("Fill", "slice");
makeRectangle(509, 15, 470, 470);
setForegroundColor(0, 255, 0);
run("Fill", "slice");
makeOval(185, 196, 125, 125);
setForegroundColor(170, 255, 0);
run("Fill", "slice");
makeOval(690, 196, 125, 125);
run("Fill", "slice");
run("Select None");
