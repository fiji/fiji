; Albert Cardona 2008-11-16
; Released under the General Public License v2.0
; 
; This ImageJ plugin creates a plot window which is dynamically updated as the
; ROI is moved across the image.
; Requires a line, polyline, freeline or rectangular ROI.


; Declare a namespace for this script
; with a set of imports specific for it.
(ns roi.profiler.dynamic
  (:import (ij IJ)
           (ij.gui Plot ProfilePlot Roi)
           (java.awt.event MouseMotionAdapter WindowAdapter)
           (java.util.concurrent Executors)))

; All functions declared with defn- (notice the minus sign) are private to this namespace.

(def *plot-width* 600)
(def *plot-height* 400)

(defn- create-profile-plot [imp]
  (let [pp (ProfilePlot. imp)
        data (.getProfile pp)
        indices (double-array (range (count data)))
        plot (Plot. "Profile" "Index" "Pixel value" indices data)]
    (doto plot
      (.setLineWidth 2)
      (.setLimits 0 (count data)
                 (.getMin pp) (.getMax pp))
      (.setSize *plot-width* *plot-height*))
    plot))

(defn- create-empty-plot []
  (let [data (float-array 1)
        indices (float-array 1)
        plot (Plot. "Profile" "Index" "Pixel value" indices data)]
    (doto plot
      (.setLimits 0 1 0 1)
      (.setSize *plot-width* *plot-height*))
    plot))

(let [valid [Roi/LINE Roi/POLYLINE Roi/FREELINE Roi/RECTANGLE]]
  (defn- is-profilable [roi]
    "Returns true if roi is not null and is one of the valid types: a linear roi or a rectangle."
    (if roi
      (if (some #{(.getType roi)} valid)
        true
        false)
      false)))

(let [empty (create-empty-plot)]
  (defn- update [plot-win imp]
    "Update the plot window using the current ROI on the image, or set a blank plot if not profilable."
    (let [roi (.getRoi imp)
          plot-imp (.getImagePlus plot-win)]
      (.setProcessor plot-imp nil (.getProcessor
                                (if (is-profilable roi)
                                  (.getImagePlus (create-profile-plot imp))
                                  empty))))))

(defn- setup [imp]
  "Creates a plot window that monitors the line ROI of the image as it changes"
  (let [canvas (.getCanvas (.getWindow imp))
        plot-win (.show (create-profile-plot imp))
        exec (Executors/newFixedThreadPool 1)   ; An executor thread pool of 1 thread to run the plot updates
        canvas-listener (proxy [MouseMotionAdapter] []
                          (mouseDragged [event]
                                        ; Submit an update job to the executor thread.
                                        ; The job is a function with no arguments, created on the fly with #(...)
                                        ; which executes the 'update' function with args plot-win and imp:
                                        (.submit exec #(update plot-win imp) nil)))]
    (.addMouseMotionListener canvas canvas-listener)
    ; Remove the mouse listener when the plot window is closed:
    (.addWindowListener plot-win (proxy [WindowAdapter] []
                                   (windowClosing [event]
                                                  ; Tell the canvas to forget our mouse listener
                                                  (.removeMouseMotionListener canvas canvas-listener)
                                                  ; Quit the executor pool
                                                  (.shutdown exec))))))

; Execute on the current image if any
(let [imp (IJ/getImage)]
  (if imp
    (if (is-profilable (.getRoi imp))
      (setup imp)
      (IJ/showMessage "Need a line or rectangular ROI!"))
    (IJ/showMessage "Open an image first!")))
