// This macro uses the Process>Math>Macro command
// to create a synthetic image.
// The formula that generates the image comes from
// MathMacroDemo macro from imageJ example macros page
// http://rsbweb.nih.gov/ij/macros/examples/MathMacroDemo.txt
// The image is a sin wave in x and a cos wave in y,
// giving a regular grid of blobs that look a bit like
// diffraction limited images of sub resolution objects

// get a new image to write into - it is 32 bit for high precision
run("Hyperstack...", "title=sine type=32-bit display=Grayscale width=1000 height=1000 channels=1 slices=1 frames=1");

// Macro... is the Process>Math>Macro command
// for making images from expresions.
// Here is the string expression for making the image
eqn = "v = (254/2) + ( (254/2) * (sin(0.4*(x+1)) * sin(0.4*(y+1)) ) )";
// why cant i just do:
//"code=[Eqn]"
//string concatenation or something i guess?

//Now run the Process>Math>Macro command with the expression
run("Macro...", "code=["+eqn+"]");

//
setMinAndMax(0.0, 255.0);
run("8-bit");
