; Albert Cardona 20080427 at MPI-CBG Dresden FIJI hackathon.

(ns Examples.random_noise_example
  (:import [ij.process ByteProcessor]
	   [ij ImagePlus]
	   [java.awt.image IndexColorModel]
	   [java.util Random]))

; Variable named *random* of type Random and with metadata that specifies it is dynamically rebindable
(declare ^:dynamic ^Random *random*)

(defmacro with-random
  "Macro. Provides a binding to a *random* var that points to a new Random instance."
  [& body]
  `(binding [*random* (new Random)]
    ~@body))

(set! *warn-on-reflection* true)

(defmacro rand-byte
  "Macro. Returns a random byte. Must be run within a with-random binding."
  []
  `(byte (- (.nextInt *random* (int 256)) (int 128))))

(defn make-grey-channel
  "Returns a byte array of length 256, with values from 0 to 255."
  []
  (let [channel (byte-array 256)]
    (dotimes [i (int 256)]
      (aset channel i (byte (- i (int 128)))))
    channel))

(defn make-grayscale-lut
  "Create an IndexColorModel representing a greyscale LUT."
  []
  (let [ch (make-grey-channel)]
    (IndexColorModel. 8 256 ch ch ch)))

; Create a new image and set each pixel to a random byte
(let [bp (new ByteProcessor 512 512)
      ^bytes pix (. bp (getPixels))]
  (with-random
    (dotimes [i (count pix)]
      (aset pix i (rand-byte))))
  (.show (ImagePlus. "random" bp)))

; Create a second image directly from a byte array
(let [pix (byte-array (* 512 512))]
  (with-random
    (dotimes [i (count pix)]
      (aset pix i (rand-byte))))
  (.show (ImagePlus. "random 2" (ByteProcessor. 512 512 pix (make-grayscale-lut)))))
