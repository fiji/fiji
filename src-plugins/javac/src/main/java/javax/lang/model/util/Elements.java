/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.lang.model.util;


import java.util.List;
import java.util.Map;

import javax.lang.model.element.*;
import javax.lang.model.type.*;


/**
 * Utility methods for operating on program elements.
 *
 * <p><b>Compatibility Note:</b> Methods may be added to this interface
 * in future releases of the platform.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see javax.annotation.processing.ProcessingEnvironment#getElementUtils
 * @since 1.6
 */
public interface Elements {

    /**
     * Returns a package given its fully qualified name.
     *
     * @param name  fully qualified package name, or "" for an unnamed package
     * @return the named package, or {@code null} if it cannot be found
     */
    PackageElement getPackageElement(CharSequence name);

    /**
     * Returns a type element given its canonical name.
     *
     * @param name  the canonical name
     * @return the named type element, or {@code null} if it cannot be found
     */
    TypeElement getTypeElement(CharSequence name);

    /**
     * Returns the values of an annotation's elements, including defaults.
     *
     * @see AnnotationMirror#getElementValues()
     * @param a  annotation to examine
     * @return the values of the annotation's elements, including defaults
     */
    Map<? extends ExecutableElement, ? extends AnnotationValue>
            getElementValuesWithDefaults(AnnotationMirror a);

    /**
     * Returns the text of the documentation (&quot;Javadoc&quot;)
     * comment of an element.
     *
     * @param e  the element being examined
     * @return the documentation comment of the element, or {@code null}
     *          if there is none
     */
    String getDocComment(Element e);

    /**
     * Returns {@code true} if the element is deprecated, {@code false} otherwise.
     *
     * @param e  the element being examined
     * @return {@code true} if the element is deprecated, {@code false} otherwise
     */
    boolean isDeprecated(Element e);

    /**
     * Returns the <i>binary name</i> of a type element.
     *
     * @param type  the type element being examined
     * @return the binary name
     *
     * @see TypeElement#getQualifiedName
     * @jls3 13.1 The Form of a Binary
     */
    Name getBinaryName(TypeElement type);


    /**
     * Returns the package of an element.  The package of a package is
     * itself.
     *
     * @param type the element being examined
     * @return the package of an element
     */
    PackageElement getPackageOf(Element type);

    /**
     * Returns all members of a type element, whether inherited or
     * declared directly.  For a class the result also includes its
     * constructors, but not local or anonymous classes.
     *
     * <p>Note that elements of certain kinds can be isolated using
     * methods in {@link ElementFilter}.
     *
     * @param type  the type being examined
     * @return all members of the type
     * @see Element#getEnclosedElements
     */
    List<? extends Element> getAllMembers(TypeElement type);

    /**
     * Returns all annotations of an element, whether
     * inherited or directly present.
     *
     * @param e  the element being examined
     * @return all annotations of the element
     * @see Element#getAnnotationMirrors
     */
    List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e);

    /**
     * Tests whether one type, method, or field hides another.
     *
     * @param hider   the first element
     * @param hidden  the second element
     * @return {@code true} if and only if the first element hides
     *          the second
     */
    boolean hides(Element hider, Element hidden);

    /**
     * Tests whether one method, as a member of a given type,
     * overrides another method.
     * When a non-abstract method overrides an abstract one, the
     * former is also said to <i>implement</i> the latter.
     *
     * <p> In the simplest and most typical usage, the value of the
     * {@code type} parameter will simply be the class or interface
     * directly enclosing {@code overrider} (the possibly-overriding
     * method).  For example, suppose {@code m1} represents the method
     * {@code String.hashCode} and {@code m2} represents {@code
     * Object.hashCode}.  We can then ask whether {@code m1} overrides
     * {@code m2} within the class {@code String} (it does):
     *
     * <blockquote>
     * {@code assert elements.overrides(m1, m2,
     *          elements.getTypeElement("java.lang.String")); }
     * </blockquote>
     *
     * A more interesting case can be illustrated by the following example
     * in which a method in type {@code A} does not override a
     * like-named method in type {@code B}:
     *
     * <blockquote>
     * {@code class A { public void m() {} } }<br>
     * {@code interface B { void m(); } }<br>
     * ...<br>
     * {@code m1 = ...;  // A.m }<br>
     * {@code m2 = ...;  // B.m }<br>
     * {@code assert ! elements.overrides(m1, m2,
     *          elements.getTypeElement("A")); }
     * </blockquote>
     *
     * When viewed as a member of a third type {@code C}, however,
     * the method in {@code A} does override the one in {@code B}:
     *
     * <blockquote>
     * {@code class C extends A implements B {} }<br>
     * ...<br>
     * {@code assert elements.overrides(m1, m2,
     *          elements.getTypeElement("C")); }
     * </blockquote>
     *
     * @param overrider  the first method, possible overrider
     * @param overridden  the second method, possibly being overridden
     * @param type   the type of which the first method is a member
     * @return {@code true} if and only if the first method overrides
     *          the second
     * @jls3 8.4.8 Inheritance, Overriding, and Hiding
     * @jls3 9.4.1 Inheritance and Overriding
     */
    boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
                      TypeElement type);

    /**
     * Returns the text of a <i>constant expression</i> representing a
     * primitive value or a string.
     * The text returned is in a form suitable for representing the value
     * in source code.
     *
     * @param value  a primitive value or string
     * @return the text of a constant expression
     * @throws IllegalArgumentException if the argument is not a primitive
     *          value or string
     *
     * @see VariableElement#getConstantValue()
     */
    String getConstantExpression(Object value);

    /**
     * Prints a representation of the elements to the given writer in
     * the specified order.  The main purpose of this method is for
     * diagnostics.  The exact format of the output is <em>not</em>
     * specified and is subject to change.
     *
     * @param w the writer to print the output to
     * @param elements the elements to print
     */
    void printElements(java.io.Writer w, Element... elements);

    /**
     * Return a name with the same sequence of characters as the
     * argument.
     *
     * @param cs the character sequence to return as a name
     */
    Name getName(CharSequence cs);
}
