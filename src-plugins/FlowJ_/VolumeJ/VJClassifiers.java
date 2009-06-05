package VolumeJ;
import java.util.*;

/**
 * This class is a class whose only
 * purpose is to contain all VJClassifier class definitions.
 * The reason is that there is no general way (in Java 1.1) to access all subclasses
 * of a known class, if these may be either in a resource or in a directory.
 * So you have to declare them here.
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public abstract class VJClassifiers
{
        public static final int LEVOYNOINDEX = 0;
        public static final int ISOSURFACE = 1;
        public static final int GRADIENTCT = 2;
        public static final int LEVOY = 3;
        public static final int LEVOYINDEXNE0 = 4;
        public static final int LINEAR = 5;
        public static final int VALUE = 6;
        public static final int LEVOYDEPTHCUEING = 7;
        // Define all VJClassifier subclasses.
        // If you define a new VJClassifier, you will have to add it to "classes", for it
        // to be available to the renderer.
        // Applets do not support dynamic loading of .class files.
        public static Vector classes = new Vector();

        public static void initClassifiers()
        {
                if (classes.size() < 1)
                {
                        classes = new Vector();
                        classes.addElement((new VJClassifierLNoIndex()).getClass());
                        classes.addElement((new VJClassifierIsosurface()).getClass());
                        classes.addElement((new VJClassifierGradientCT()).getClass());
                        classes.addElement((new VJClassifierLevoy()).getClass());
                        classes.addElement((new VJClassifierLNotIndex0()).getClass());
                        classes.addElement((new VJClassifierLinear()).getClass());
                        classes.addElement((new VJClassifierValue()).getClass());
                        classes.addElement((new VJClassifierLevoyCueing()).getClass());
                }
        }
        /**
         * Return an array with the names of all VJClassifier classes.
         * @return an array of Strings, null if no classes available.
         */
        public static String [] getNames()
        {
                initClassifiers();
                String [] names = new String[classes.size()];
                for (int i = 0; i < names.length; i++)
                {
                        VJClassifier instance = null;
                        try
                        {
                                instance = (VJClassifier) ((Class)classes.elementAt(i)).newInstance();
                        }
                        catch (InstantiationException e) { VJUserInterface.write("Cannot instantiate "+e);}
                        catch (IllegalAccessException e) {VJUserInterface.write("Cannot instantiate (acc)");}
                        try
                        {
                                names[i] = instance.toString();
                        }
                        catch (NoSuchMethodError e) { VJUserInterface.write("Please define instantiation and/or toString() function"); }
                }
                return names;
        }
        /**
         * Return the class belonging to index i.
	 * @param i the number of the class.
         */
        public static Class getClass(int i) { return (Class)classes.elementAt(i); }
        /**
	 * @param i the number of the class.
         * @return the VJClassifier belonging to index i.
         */
        public static VJClassifier getClassifier(int i)
        {
                VJClassifier classifier = null;
                try
                {
                        classifier = (VJClassifier) getClass(i).newInstance();
                }
                catch (InstantiationException ie) {VJUserInterface.write("Classifier error "+ie);}
                catch (IllegalAccessException ie) {VJUserInterface.write("Classifier error "+ie);}
                return classifier;
        }
}

