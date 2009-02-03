;; Example taken from Clojure website

(import '(javax.swing JFrame JLabel JTextField JButton)
        '(java.awt.event ActionListener)
	'(java.awt GridLayout)
	'(java.lang NumberFormatException))

; Define a function that does all, and which takes no arguments
(defn celsius []
 ; Define the gui elements as variables that point to new instances
 ; of buttons, labels, etc.
 (let [frame (new JFrame "Celsius Converter")
       temp-text (new JTextField)
       celsius-label (new JLabel "Celsius")
       convert-button (new JButton "Convert")
       fahrenheit-label (new JLabel "Fahrenheit")]
   ; On the convert button, add an anonymous listener (proxy)
   ; In java it would be:  button.addActionListener(new ActionListener() {
   ;                 public void actionPerformed(ActionEvent evt) { ... } } );
   (. convert-button
       (addActionListener
           (proxy [ActionListener] []
	        (actionPerformed [evt]
		    ; check that the entered string length is > 0
		    (let [snum (.getText temp-text)]
		      (if (> (.length snum) 0)
			; check if the string is a number at all
			(try (let [num (Double/parseDouble snum)]
			       (. fahrenheit-label
				  (setText (str (+ 32 (* 1.8 num)) " Fahrenheit"))))
                          (catch java.lang.NumberFormatException e
			       (. fahrenheit-label
				  (setText "Not a number"))))
			; else, hint what is needed
			(. fahrenheit-label
			   (setText "Enter a number"))))))))
   ; The number format error prevention could have been written by adding
   ; an extended PlainDocument to the JTextField which only accepts valid numbers.
   ; BUT one cannot call super, so the call to super.insertString() cannot be made!

   ; On the frame, call all the following methods
   ; It could be done with many blocks like (. frame (add celsius-label)) etc.
   (doto frame
              (.setLayout (new GridLayout 2 2 3 3))
	      (.add temp-text)
	      (.add celsius-label)
	      (.add convert-button)
	      (.add fahrenheit-label)
	      (.setSize 300 80)
	      (.setVisible true))))
; Execute the function
(celsius)
