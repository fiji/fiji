; Albert Cardona 20080427 at MPI-CBG Dresden FIJI hackathon.

; Opens a URL file path as an image
(let [opener (new ij.io.Opener)]
  (defn open-url [url]
	(.openURL opener url)))

; Fetch two example 512x512 images from the net
(let [baboon (open-url "http://rsb.info.nih.gov/ij/images/baboon.jpg")
      bridge (open-url "http://rsb.info.nih.gov/ij/images/bridge.gif")]
  ; Obtain color channel byte arrays for baboon color image
  (let [len (count (.. baboon (getProcessor) (getPixels))) ; could also say (* 512 512)
	r (make-array Byte/TYPE len)
	g (make-array Byte/TYPE len)
	b (make-array Byte/TYPE len)
	br (.. bridge (getProcessor) (getPixels))]
    ; Fill a copy of the channel arrays
    (.. baboon (getProcessor) (getRGB r g b))
    ; Blend the bridge pixels into each color channel of the baboon image
    (defn avg-byte [a b]
	  (byte (/ (+ (bit-and a 255) b) 2)))
    (dotimes [i len]
      (let [pix (bit-and (aget br i) 255)]
	(aset r i (avg-byte (aget r i) pix))
	(aset g i (avg-byte (aget g i) pix))
	(aset b i (avg-byte (aget b i) pix))))
    ; Set the color channels
    (.. baboon (getProcessor) (setRGB r g b))
    ; Done!
    (.show baboon)))

; The above script is ready for a lot of macro abstraction
