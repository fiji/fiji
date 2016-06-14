// Albert Cardona 20081109
// This code is released under the public domain
//
// A multithreading framework for ImageJ in javascript
//
// First a function named doItMultithreaded shows how to run a process in
// parallel, in as many threads as CPU cores. The key idea is that of iterating
// over a list of numbers from start to end, where each index in the list means
// something: for example, a line of pixels in an image.
//
// The example simply prints a list of numbers in a multithreaded way.
//
// Then, the multithreading part and the printing part are separated into the
// "multithreader" function and the "printer" function. The "printer" is
// invoked by passing it to the "multithreader" function as an argument.
//
// Finally, a more real-world example is show, in which lines of an image, 10
// lines at a time, are processed independelty in separate threads and filled
// with random pixel values, using the "multithreader" framework function.
//
// Please note that generating random values has so little overhead that a
// multithreaded setup does not pay off for small images, no matter how many
// lines at a time are processed together. This is intended as an example of
// what could be done, for example, for very computationally expensive filters
// like a large median filter or a gaussian with a large standard deviation.
//
// Have fun!

importClass(Packages.ij.IJ);
importClass(Packages.ij.ImagePlus);
importClass(Packages.ij.process.FloatProcessor);

importClass(Packages.java.lang.Runnable);
importClass(Packages.java.lang.Runtime);
importClass(Packages.java.lang.System);
importClass(Packages.java.lang.Thread);
importClass(Packages.java.util.concurrent.atomic.AtomicInteger);

// Print all numbers from start to end (inclusive), multithreaded
function doItMultithreaded(start, end) {
	var threads = java.lang.reflect.Array.newInstance(java.lang.Thread.class, Runtime.getRuntime().availableProcessors());
	var ai = new AtomicInteger(start);
	var body = {
		run: function() {
			for (var i = ai.getAndIncrement(); i <= end; i = ai.getAndIncrement()) {
				IJ.log("i is " + i);
				Thread.sleep(100); // NOT NEEDED, just to pretend we are doing something!
			}
		}
	}
	// start all threads
	for (var i = 0; i < threads.length; i++) {
		threads[i] = new Thread(new Runnable(body)); // automatically as Runnable
		threads[i].start();
	}
	// wait until all threads finish
	for (var i = 0; i < threads.length; i++) {
		threads[i].join();
	}
}

// execute like:
// doItMultithreaded(0, 10);

// Now, abstract away the multithreading framework into a function
// that takes another function as argument:
function multithreader(fun, start, end) {
	var threads = java.lang.reflect.Array.newInstance(java.lang.Thread.class, Runtime.getRuntime().availableProcessors());
	var ai = new AtomicInteger(start);
	// Prepare arguments: all other arguments passed to this function
	// beyond the mandatory arguments fun, start and end:
	var args = new Array();
	var b = 0;
	IJ.log("Multithreading function \"" + fun.name + "\" with arguments:\n  argument 0 is index from " + start + " to " + end);
	for (var a = 3; a < arguments.length; a++) {
		args[b] = arguments[a];
		IJ.log("  argument " + (b+1) + " is " + args[b]);
		b++;
	}
	var body = {
		run: function() {
			for (var i = ai.getAndIncrement(); i <= end; i = ai.getAndIncrement()) {
				// Execute the function given as argument,
				// passing to it all optional arguments:
				fun(i, args);
			}
		}
	}
	// start all threads
	for (var i = 0; i < threads.length; i++) {
		threads[i] = new Thread(new Runnable(body)); // automatically as Runnable
		threads[i].start();
	}
	// wait until all threads finish
	for (var i = 0; i < threads.length; i++) {
		threads[i].join();
	}
}

// The actual desired effect: the printer
function printer(i) {
	IJ.log("i is " + i);
}

// Execute like (uncomment!):
// multithreader(printer, 0, 10);


// Above, notice how we are passing the printer function as an argument to the
// multithreader function, which is executed simply by putting parenthesis to
// its name. Simple!


// Now, armed with the multithreader, we can parallelize any function we want:
// for example, filling each pixel of an image with a random value.
//
// The key for best performance is to break down the task in significant
// chunks. Multithreading for each pixel makes little sense--to much overhead
// wipes away the gain. Multithreading for one line, same thing: a random value
// is not so costly to compute, still too much overhead. So we are going to
// multithread the processing of for example 100 lines at a time:


// Takes a starting line and a number of lines to process,
// and sets their pixels to a random value
function randomizer(line, args) {
	// Obtain and check the arguments:
	if (args.length < 5) {
		IJ.log("randomizer needs at least 5 arguments: line, pix, width, height, n_lines and rand");
		return;
	}
	var pix = args[0];
	var width = args[1];
	var height = args[2];
	var n_lines = args[3];
	var rand = args[4];
	for (var y = line; y < height && y < height + n_lines; y++) {
		var offset = y * width;
		for (var x = 0; x < width; x++) {
			pix[offset + x] = rand.nextFloat();
		}
	}
}

// Test: create a new image, fill it with random values, and show it
width = 512;
height = 512;
imp = new ImagePlus("Random", new FloatProcessor(width, height));
pix = imp.getProcessor().getPixels();
importClass(Packages.java.util.Random);
rand = new Random(System.currentTimeMillis());
block_size = 100; // number of lines to be processed together
n_blocks = ((height / block_size)|0) + 1; // casting to int with bitwise or to zero

// Execute the randomizer in multithreaded fashion:
//   - At the top row, the three arguments for the multithreading framework
//   - At the bottom row, the N arguments for the function to parallelize
multithreader(randomizer, 0, n_blocks,
              pix, width, height, block_size, rand);

// Show the image:
imp.getProcessor().setMinAndMax(0, 1); // random values between 0 and 1
imp.show();

