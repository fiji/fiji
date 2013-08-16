; Albert Cardona 20080427 at MPI-CBG Dresden FIJI hackathon.

(ns Examples.blend_to_images
  (:import [ij IJ ImagePlus]))

; Opens a URL file path as an image
(let [opener (new ij.io.Opener)]
  (defn open-url [url]
	(.openURL opener url)))

(set! *warn-on-reflection* true)

(defmacro avg-byte [a b]
  `(byte (min (int 127)
              (/ (+ (bit-and (int ~a) (int 255))
                    ~b)
                 2))))

(binding [*unchecked-math* true]
  ; Fetch two example 512x512 images from the net
  (let [^ImagePlus baboon (open-url "http://rsb.info.nih.gov/ij/images/baboon.jpg")
        ^ImagePlus bridge (open-url "http://rsb.info.nih.gov/ij/images/bridge.gif")]
    ; Obtain color channel byte arrays for baboon color image
    (let [len (count (.. baboon getProcessor getPixels)) ; could also say (* 512 512)
	  r (byte-array len)
	  g (byte-array len)
	  b (byte-array len)
	  ^bytes br (.. bridge getProcessor getPixels)]
      ; Fill a copy of the channel arrays
      (.. baboon getProcessor (getRGB r g b))
      ; Blend the bridge pixels into each color channel of the baboon image
      (dotimes [i (int len)]
        (let [pix (bit-and (int (aget br i)) (int 255))]
	  (aset r i (avg-byte (aget r i) pix))
	  (aset g i (avg-byte (aget g i) pix))
	  (aset b i (avg-byte (aget b i) pix))))
      ; Set the color channels
      (.. baboon (getProcessor) (setRGB r g b))
      ; Done!
      (.show baboon))))

; The above script is ready for a lot of macro abstraction
