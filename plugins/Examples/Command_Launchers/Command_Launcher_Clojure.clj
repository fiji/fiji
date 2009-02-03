(import '(java.awt Color)
	'(java.awt.event TextListener))

; Commented out are three lines that would make the loop be with an integer iterator
; As it is now it's a java.util.Iterator

;(let [commands (. (.. ij.Menus (getCommands) (keySet)) (toArray))
(let [commands (.. ij.Menus (getCommands) (keySet))
      gd (new ij.gui.GenericDialog "Command Launcher")]
  (. gd (addStringField "Command: " "" ))
  (let [prompt (.. gd (getStringFields) (get 0))]
    (. prompt (setForeground (. Color red)))
    (. prompt (addTextListener (proxy [TextListener] []
		   (textValueChanged [tvc]
				     (let [text (. prompt (getText))
					   len (count commands)]
				       ;(loop [i 0]
				       (loop [it (. commands (iterator))]
					     (if (. it (hasNext))
					       ; (if (= text (aget commands i))
					       (if (= text (. it (next)))
						 (. prompt (setForeground (. Color black)))
						 ;(recur (inc i)))
						 (recur it)) ; needs as arguments a value for each variable declared in the loop 'let' statement; in this case, the very same iterator.
					       (. prompt (setForeground (. Color red)))))))))))
  (. gd (showDialog))
  (if (not (. gd (wasCanceled)))
   (. ij.IJ (doCommand (. gd (getNextString))))))
