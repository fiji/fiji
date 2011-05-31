; Albert Cardona 20081011
;
; This file illustrates how to create a generic, reusable macro that uses as
; many threads as cores your CPU has.
; The macro, named "multithreader" and declared below, takes as arguments:
;  - the starting index
;  - the ending index (non-inclusive)
;  - any function and
;  - .. its required arguments
;
;  Then an example is shown on how to process an image line by line, where any
;  number of threads are editing one line each at a time, without collision.
;  In particular, we declare the "line-randomizer" function, which simply sets
;  each pixel of a 32-bit image to a random value from 0 to 1.
;  Then the function "line-randomizer" is invoked by giving it and a new image
;  to the "multithreader" macro, and then the resulting image is shown.
;
;
; As a step-by-step introduction, this file starts by declaring first a set of functions that illustrate:
; - how to loop from a starting to an ending index:
;     * "do-it"
;     * "do-it-iterating"
;     * "do-it-looping"
; - how to make a function that uses multiple threads:
;     * "do-it-multithreading"
; - ... and finally, how the "do-it-multithreading" function is broken apart into two: the "printer" function and the "multithreader" macro.
; - how to extract a macro from the "do-it-multithreading" function
; - how to declare a function inside a closure (the "line-randomizer"
;   function), so that the function has access to an otherwise invisible
;   variable (in this case, the Random number generator)
;
; Finally, the "multithreader" macro and the "line-randomizer" function are
; put to use, and the resulting image is show.
; 
; Note that the "multithreader" macro may be reused with ANY function you want.
; All you need to do is to subdivide an image in a way that makes sense to you,
; (such as by lines) and apply to each subdivision your function, in a
; multithreaded way, with the "multithreader" macro.
;
; See http://clojure.org for general documentation on Clojure
; See http://fiji.sc/wiki/index.php/Clojure_Scripting for help on
; using Clojure with ImageJ
; See http://fiji.sc/wiki/index.php/Scripting_Help for how to use fiji's built-in scripting languages and how to create scripts for ImageJ.
;
; As a final note: Clojure runs native. There is no such thing as a clojure
; interpreter: all clojure code compiles to java byte code, which runs native
; on the JVM.
; Sometimes though Clojure will use reflection, which may slow down processing.
; To avoid it, just add type declarations with #^ (see below for examples)
; where they really make a difference.
; To tell the compiler to warn you when reflection is used, uncomment this line:
; (set! *warn-on-reflection true)
;
; The ';' indicates a commented line, as you may have already guessed.
;
; Have fun -- Albert Cardona


(defn do-it [start end]
  "Print all numbers from start to end (exclusive)"
  (map println
       (range start end)))

; Invoke like
; (do-it 0 10)

(defn do-it-iterating [start end]
  "Print all numbers from start to end (inclusive)"
  (doseq [i (range start (+ 1 end))]
    (println i)))

(defn do-times [start end]
  "Print all numbers from start to end (exclusive)"
  (dotimes [i (range start end)]
    (println i)))

; Invoke like
; (do-it-iterating 0 10)

; Crude looping
(defn do-it-looping [start end]
  "Print all numbers from start to end (non-inclusive)"
  (loop [i start]
    (if (<= i end)
      (do
        (println i)
        (recur (inc i))))))

; Invoke like
; (do-it-looping 0 10)


(import '(java.util.concurrent.atomic AtomicInteger))

(defn do-it-multithreaded [start end]
  "Print all numbers from start to end (inclusive), multithreaded"
  (let [ai (AtomicInteger. start)]
    (defn looper []
      (loop [i (.getAndIncrement ai)]
        (if (< i (+ 1 end))
          (do
            (println i)
            (try
              (Thread/sleep 100)
              (catch Exception e (.printStackTrace e)))
            (recur (.getAndIncrement ai))))))
    (dotimes [i (.availableProcessors (Runtime/getRuntime))]
      (.start (Thread. looper)))))

; Invoke like
; (do-it-multithreaded 0 10)

; Now separated:

(defn printer [i]
  "Print the given number i and then sleep 100 ms"
  (println i)
  (try
    (Thread/sleep 100)
    (catch Exception e (.printStackTrace e))))

(defmacro multithreader [start end fun & args]
  "Call function fun with numeric arguments from start to end (non-inclusive), multithreaded"
  ; Below, the # is shorthand for (gensym <a-name>) to create unique names
  `(let [ai# (AtomicInteger. ~start)]
    ; Define a new function to represent one Thread
    ; (All functions implement Runnable, not only Callable)
    (defn looper []
      (loop [i# (.getAndIncrement ai#)]
        (if (< i# ~end)
          (do
            ; Execute the function given as argument
            ; with the current i and whatever other args it needs
            (~fun i# ~@args)
            (recur (.getAndIncrement ai#))))))
    ; Create as many looper threads as cores the CPU has
    (let [threads# (map (fn [x#] (Thread. looper (str "Looper-" x#)))
                       (range (.availableProcessors (Runtime/getRuntime))))]
      ; Start all threads
      (doseq [t# threads#]
        (.start t#))
      ; Wait until all threads are done
      (doseq [t# threads#]
        (.join t#)))))

; Invoke like:
;(multithreader 0 10 printer)

; So now we can define a processing function that will edit, for example, a line
; in an image, and then apply the multithreader function to process the image
; with as many threads as desired. In this case, as many as cores the CPU has:

(import '(java.util Random))

; Define line-randomizer function inside a closure
; so the Random seed and distribution is shared.
; Could use built-in rand function as well, but then can't control seed.

(let [r (Random. (System/currentTimeMillis))]
  (defn line-randomizer [row #^floats pixels width]
    "Randomize the value of all pixels in the given row of the image contained in the unidimensional array of pixels"
    (let [offset (int (* width row))]
      (dotimes [i width]
        (aset pixels (+ i offset) (.nextFloat r))))))

; Execute like

(import '(ij IJ ImagePlus)
        '(ij.process FloatProcessor))

; Use the float[] from an ImageJ image. An array of floats
; would be created like:
; #^floats pixels (make-array Float/TYPE (* width height))
; Note the #^floats, which is shorthand for (with-meta (make-array ...) {:floats}) and adds an obvious type declaration to avoid reflection.

(let [width (int 512)   ; without the cast, 512 would be a Number, not a primitive. Number (like Integer, Float, etc.) need unboxing, which is costly.
      height (int 512)
      #^ImagePlus imp (IJ/createImage "Random image" "32-bit" width height 1)
      #^floats pixels (.getPixels (.getProcessor imp))]
  (multithreader 0 height
                 line-randomizer pixels width)
  (.setMinAndMax (.getProcessor imp) 0 1)
  (.show imp))


