FastRandomForest v0.9
---------------------
Copyright (c) 2008 Fran Supek (fran.supek[AT]irb.hr)

Contents:
* What is FastRandomForest?
* License
* Using from own Java code
* Using from Weka Explorer or Experimenter (3-5-7 or earlier)
* Using from Weka Explorer or Experimenter (3-5-8 or newer)



What is FastRandomForest?
-------------------------

FastRandomForest is a re-implementation of the Random Forest classifier (RF)
for the Weka environment that brings speed and memory use improvements over the 
original Weka RF, without sacrificing accuracy.

Speed gains depend on many factors, but a 10-20x increase on a quad-core desktop
computer is not uncommon, along with a 2x reduction in memory use.
 
For detailed tests of speed and classification accuracy, as well as description 
of changes to the code, please refer to the FastRandomForest wiki at

http://code.google.com/p/fast-random-forest/w

or email the author at fran.supek[AT]irb.hr.


License
-------

This program is free software; you can redistribute it and/or modify it under 
the terms of the GNU General Public License as published by the Free Software 
Foundation; either version 2 of the License, or (at your option) any later 
version.
 
This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License along with 
this program; if not, write to the Free Software Foundation, Inc., 675 Mass 
Ave, Cambridge, MA 02139, USA.



Using from own Java code
------------------------

Just add FastRandomForest.jar to your Java VM classpath by using the -cp 
switch, or by changing project dependencies in NetBeans/Eclipse/whatever IDE 
you use. Then use hr.irb.fastRandomForest.FastRandomForest as you would use 
any other classifier, see instructions at the WekaWiki:

http://weka.sourceforge.net/wiki/index.php/Use_Weka_in_your_Java_code 



Using from Weka Explorer or Experimenter (3-5-8 or newer)
---------------------------------------------------------

1. Add the FastRandomForest.jar to your Java classpath when starting Weka. This
is normally done by editing the line beginning with “cp=” in “RunWeka.ini”

2. You extract the “GenericObjectEditor.props” file from weka.jar
(jar files are in fact ordinary zip archives, the GenericObjectEditor.props is
under /weka/gui).

3. Place the file you've just extracted into the directory where you have
installed Weka (on Windows this is commonly "C:\Program Files\Weka-3-5")

4. Find the

     # Lists the Classifiers I want to choose from

heading and scroll far down to the end of the block (first empty line), then
add a line:

     hr.irb.fastRandomForest.FastRandomForest

Do not forget to append a comma and a backslash to the previous line.

5. The “FastRandomForest” class is in the "hr.irb.fastRandomForest" package
in the "Classify" tab. Enjoy.


 
Using from Weka Explorer or Experimenter (3-5-7 or earlier)
-----------------------------------------------------------

1. Add the FastRandomForest.jar to your Java classpath when starting Weka. This 
is normally done by editing the line beginning with “cp=” in “RunWeka.ini”
If "cp=" doesn't exist, search for "cmd_default=" and add after "#wekajar#;".

2. You need to extract the “GenericPropertiesCreator.props” file from your 
weka.jar (jar files are in fact ordinary zip archives, the 
GenericPropertiesCreator.props is under /weka/gui).

3. Place the file you've just extracted into the directory where you have
installed Weka (on Windows this is commonly "C:\Program Files\Weka-3-5")

4. Under the

     # Lists the Classifiers-Packages I want to choose from

heading, add the line

     hr.irb.fastRandomForest

Do not forget to add a comma and a backslash to the previous line.

5. Use the “FastRandomForest” class is in the hr.irb.fastRandomForest
package in the "Classify" tab. The other three classes cannot be used directly.




