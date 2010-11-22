run("Mitosis (26MB, 5D stack)");
run("Reduce Dimensionality...", "  slices frames");
wait(500); // to repaint the window
run("Temporal-Color Code", "lut=Spectrum start=1 end=51 create");
