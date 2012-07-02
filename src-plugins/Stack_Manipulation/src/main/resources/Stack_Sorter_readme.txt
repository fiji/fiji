Stack_Sorter_readme.txt
Version 9
18/Nov/2005.

Author: Bob Dougherty (rpd@optinav.com) 

Installation:

Download Stack_Sorter.jar into the plugins folder and restart ImageJ.  Stack Sorter installs itself into the Plugins>Stacks menu. 

ImageJ version 1.32c or later is required.  Update from upgrade ImageJ . 

Source file: The source can be extracted with the command: jar xvf Stack_Sorter.jar

Description: ImageJ plugin to sort the slices of a stack.  

Menu 

The buttons operate on the front window, which should usually be a stack.   The first four (arrow) buttons reposition a slice within the stack, changing the order of the slices.   "Delete" deletes the current slice, "Duplicate" duplicates the current slice, and "Duplicate n" and "Delete n" duplicate and delete multiple slices.  "Insert" adds a new slice (or stack) from another window and increases the size of the stack.  "Insert File" and "Insert URL" obtain new slice(s) from a disk file or a URL.  Paste (system) takes an image from the system clipboard.  Duplicate, Duplicate n, and the Insert and Paste commands do not require a stack begin with; when used with a single image they will create stacks.  The Insert and Paste buttons will create a new window if necessary.  
  
 
 
Insert

Pressing "Insert" produces a dialog box that lets you choose the source image from the available windows. If there are only two windows open, then the dialog is not displayed, since Insert assumes that you want to insert the image from the "other" window.  If the source image has an ROI, then the new slice in the stack will contain only the ROI in the center of the image, with the outside filled with the background color.   It is not necessary for the source image and the destination stack to match in dimensions or type.   If the types do not match, then the values of the inserted pixels may not be the same as the source values. 
If the source and destination images match in dimensions, then the ROI is not moved to the center. 

If the source ROI to be inserted is larger than the destination image in one or both dimensions, then several options will be presented for adjusting the the source or the destination before the insert operation: 

Insert a cropped image 
Insert a reduced size image 
Resize the destination stack by adding border pixels 
Resize the destination stack by scaling up the image 
The second and fourth options change the size of the pixels in the inserted image and the destination stack, respectively. 
If a stack is chosen as the source to insert, then a dialog will ask how many slices to take from the stack, starting with the currently selected slice. 

Insert File, Insert URL, and Paste (system) insert images as above. 

Label Slices creates numerical slice labels.
Sort by Label sorts the slices based on the slice label.
Sort by Mean sorts the slices based on the mean value in the ROI.
Reverse reverses the slices.
 
Limitations

"Undo" is not supported. 
8-bit color images can give strange resuls due to LUT issues. 
"Scaling" is not carefully controlled on "Insert," so the values may not match.  
History: Version 0: 3/14/2004 
Version 1: 3/15/2004 Added "Insert." 
Version 2: 3/16/2004 Updated (by Wayne Rasband) for Mask type in ImageJ v. 1.32c. 
Version 3: 3/17/2004 Added Duplicate commands and improved the UI. 
Version 4: 3/20/2004 Added type conversions before insert. 
Version 5: 3/22/2004 Added size adjustments before insert and stack insertion. 
Version 6: 4/2/2004 Added Insert File. 
Version 7: 4/2/2004 Added Insert URL and Paste (system).
Version 8: 4/5/2004 Fixed a bug in duplicate and revised delete.


Acknowledgements

Some of the code was borrowed from Wayne Rasband's ROI Manager and Filler. 
Thanks to Simon Roussel for testing and feedback. 
See also System Clipboard and Stack Plugins on the ImageJ site.  
 

ImageJ: ImageJ can be freely downloaded from the ImageJ web site http://rsb.info.nih.gov/ij/
