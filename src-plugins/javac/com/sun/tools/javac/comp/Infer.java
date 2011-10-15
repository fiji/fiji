/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Type.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;

/** Helper class for type parameter inference, used by the attribution phase.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Infer {
    protected static final Context.Key<Infer> inferKey =
        new Context.Key<Infer>();

    /** A value for prototypes that admit any type, including polymorphic ones. */
    public static final Type anyPoly = new Type(NONE, null);

    Symtab syms;
    Types types;

    public static Infer instance(Context context) {
        Infer instance = context.get(inferKey);
        if (instance == null)
            instance = new Infer(context);
        return instance;
    }

    protected Infer(Context context) {
        context.put(inferKey, this);
        syms = Symtab.instance(context);
        types = Types.instance(context);
    }

    public static class NoInstanceException extends RuntimeException {
        private static final long serialVersionUID = 0;

        boolean isAmbiguous; // exist several incomparable best instances?

        JCDiagnostic diagnostic;

        NoInstanceException(boolean isAmbiguous) {
            this.diagnostic = null;
            this.isAmbiguous = isAmbiguous;
        }
        NoInstanceException setMessage(String key) {
            this.diagnostic = JCDiagnostic.fragment(key);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1, Object arg2) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1, arg2);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1, Object arg2, Object arg3) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1, arg2, arg3);
            return this;
        }
        public JCDiagnostic getDiagnostic() {
            return diagnostic;
        }
    }
    private final NoInstanceException ambiguousNoInstanceException =
        new NoInstanceException(true);
    private final NoInstanceException unambiguousNoInstanceException =
        new NoInstanceException(false);

/***************************************************************************
 * Auxiliary type values and classes
 ***************************************************************************/

    /** A mapping that turns type variables into undetermined type variables.
     */
    Mapping fromTypeVarFun = new Mapping("fromTypeVarFun") {
            public Type apply(Type t) {
                if (t.tag == TYPEVAR) return new UndetVar(t);
                else return t.map(this);
            }
        };

    /** A mapping that returns its type argument with every UndetVar replaced
     *  by its `inst' field. Throws a NoInstanceException
     *  if this not possible because an `inst' field is null.
     */
    Mapping getInstFun = new Mapping("getInstFun") {
            public Type apply(Type t) {
                switch (t.tag) {
                case UNKNOWN:
                    throw ambiguousNoInstanceException
                        .setMessage("undetermined.type");
                case UNDETVAR:
                    UndetVar that = (UndetVar) t;
                    if (that.inst == null)
                        throw ambiguousNoInstanceException
                            .setMessage("type.variable.has.undetermined.type",
                                        that.qtype);
                    return apply(that.inst);
                default:
                    return t.map(this);
                }
            }
        };

/***************************************************************************
 * Mini/Maximization of UndetVars
 ***************************************************************************/

    /** Instantiate undetermined type variable to its minimal upper bound.
     *  Throw a NoInstanceException if this not possible.
     */
    void maximizeInst(UndetVar that, Warner warn) throws NoInstanceException {
        if (that.inst == null) {
            if (that.hibounds.isEmpty())
                that.inst = syms.objectType;
            else if (that.hibounds.tail.isEmpty())
                that.inst = that.hibounds.head;
            else {
                for (List<Type> bs = that.hibounds;
                     bs.nonEmpty() && that.inst == null;
                     bs = bs.tail) {
                    // System.out.println("hibounds = " + that.hibounds);//DEBUG
                    if (isSubClass(bs.head, that.hibounds))
                        that.inst = types.fromUnknownFun.apply(bs.head);
                }
                if (that.inst == null) {
                    int classCount = 0, interfaceCount = 0;
                    for (Type t : that.hibounds) {
                        if (t.tag == CLASS) {
                            if (t.isInterface())
                                interfaceCount++;
                            else
                                classCount++;
                        }
                    }
                    if ((that.hibounds.size() == classCount + interfaceCount) && classCount == 1)
                        that.inst = types.makeCompoundType(that.hibounds);
                }
                if (that.inst == null || !types.isSubtypeUnchecked(that.inst, that.hibounds, warn))
                    throw ambiguousNoInstanceException
                        .setMessage("no.unique.maximal.instance.exists",
                                    that.qtype, that.hibounds);
            }
        }
    }
    //where
        private boolean isSubClass(Type t, final List<Type> ts) {
            t = t.baseType();
            if (t.tag == TYPEVAR) {
                List<Type> bounds = types.getBounds((TypeVar)t);
                for (Type s : ts) {
                    if (!types.isSameType(t, s.baseType())) {
                        for (Type bound : bounds) {
                            if (!isSubClass(bound, List.of(s.baseType())))
                                return false;
                        }
                    }
                }
            } else {
                for (Type s : ts) {
                    if (!t.tsym.isSubClass(s.baseType().tsym, types))
                        return false;
                }
            }
            return true;
        }

    /** Instaniate undetermined type variable to the lub of all its lower bounds.
     *  Throw a NoInstanceException if this not possible.
     */
    void minimizeInst(UndetVar that, Warner warn) throws NoInstanceException {
        if (that.inst == null) {
            if (that.lobounds.isEmpty())
                that.inst = syms.botType;
            else if (that.lobounds.tail.isEmpty())
                that.inst = that.lobounds.head;
            else {
                that.inst = types.lub(that.lobounds);
                if (that.inst == null)
                    throw ambiguousNoInstanceException
                        .setMessage("no.unique.minimal.instance.exists",
                                    that.qtype, that.lobounds);
            }
            // VGJ: sort of inlined maximizeInst() below.  Adding
            // bounds can cause lobounds that are above hibounds.
            if (that.hibounds.isEmpty())
                return;
            Type hb = null;
            if (that.hibounds.tail.isEmpty())
                hb = that.hibounds.head;
            else for (List<Type> bs = that.hibounds;
                      bs.nonEmpty() && hb == null;
                      bs = bs.tail) {
                if (isSubClass(bs.head, that.hibounds))
                    hb = types.fromUnknownFun.apply(bs.head);
            }
            if (hb == null ||
                !types.isSubtypeUnchecked(hb, that.hibounds, warn) ||
                !types.isSubtypeUnchecked(that.inst, hb, warn))
                throw ambiguousNoInstanceException;
        }
    }

/***************************************************************************
 * Exported Methods
 ***************************************************************************/

    /** Try to instantiate expression type `that' to given type `to'.
     *  If a maximal instantiation exists which makes this type
     *  a subtype of type `to', return the instantiated type.
     *  If no instantiation exists, or if several incomparable
     *  best instantiations exist throw a NoInstanceException.
     */
    public Type instantiateExpr(ForAll that,
                                Type to,
                                Warner warn) throws NoInstanceException {
        List<Type> undetvars = Type.map(that.tvars, fromTypeVarFun);
        for (List<Type> l = undetvars; l.nonEmpty(); l = l.tail) {
            UndetVar v = (UndetVar) l.head;
            ListBuffer<Type> hibounds = new ListBuffer<Type>();
            for (List<Type> l1 = types.getBounds((TypeVar) v.qtype); l1.nonEmpty(); l1 = l1.tail) {
                if (!l1.head.containsSome(that.tvars)) {
                    hibounds.append(l1.head);
                }
            }
            v.hibounds = hibounds.toList();
        }
        Type qtype1 = types.subst(that.qtype, that.tvars, undetvars);
        if (!types.isSubtype(qtype1, to)) {
            throw unambiguousNoInstanceException
                .setMessage("no.conforming.instance.exists",
                            that.tvars, that.qtype, to);
        }
        for (List<Type> l = undetvars; l.nonEmpty(); l = l.tail)
            maximizeInst((UndetVar) l.head, warn);
        // System.out.println(" = " + qtype1.map(getInstFun));//DEBUG

        // check bounds
        List<Type> targs = Type.map(undetvars, getInstFun);
        targs = types.subst(targs, that.tvars, targs);
        checkWithinBounds(that.tvars, targs, warn);

        return getInstFun.apply(qtype1);
    }

    /** Instantiate method type `mt' by finding instantiations of
     *  `tvars' so that method can be applied to `argtypes'.
     */
    public Type instantiateMethod(List<Type> tvars,
                                  MethodType mt,
                                  List<Type> argtypes,
                                  boolean allowBoxing,
                                  boolean useVarargs,
                                  Warner warn) throws NoInstanceException {
        //-System.err.println("instantiateMethod(" + tvars + ", " + mt + ", " + argtypes + ")"); //DEBUG
        List<Type> undetvars = Type.map(tvars, fromTypeVarFun);
        List<Type> formals = mt.argtypes;

        // instantiate all polymorphic argument types and
        // set up lower bounds constraints for undetvars
        Type varargsFormal = useVarargs ? formals.last() : null;
        while (argtypes.nonEmpty() && formals.head != varargsFormal) {
            Type ft = formals.head;
            Type at = argtypes.head.baseType();
            if (at.tag == FORALL)
                at = instantiateArg((ForAll) at, ft, tvars, warn);
            Type sft = types.subst(ft, tvars, undetvars);
            boolean works = allowBoxing
                ? types.isConvertible(at, sft, warn)
                : types.isSubtypeUnchecked(at, sft, warn);
            if (!works) {
                throw unambiguousNoInstanceException
                    .setMessage("no.conforming.assignment.exists",
                                tvars, at, ft);
            }
            formals = formals.tail;
            argtypes = argtypes.tail;
        }
        if (formals.head != varargsFormal || // not enough args
            !useVarargs && argtypes.nonEmpty()) { // too many args
            // argument lists differ in length
            throw unambiguousNoInstanceException
                .setMessage("arg.length.mismatch");
        }

        // for varargs arguments as well
        if (useVarargs) {
            Type elt = types.elemtype(varargsFormal);
            Type sft = types.subst(elt, tvars, undetvars);
            while (argtypes.nonEmpty()) {
                Type ft = sft;
                Type at = argtypes.head.baseType();
                if (at.tag == FORALL)
                    at = instantiateArg((ForAll) at, ft, tvars, warn);
                boolean works = types.isConvertible(at, sft, warn);
                if (!works) {
                    throw unambiguousNoInstanceException
                        .setMessage("no.conforming.assignment.exists",
                                    tvars, at, ft);
                }
                argtypes = argtypes.tail;
            }
        }

        // minimize as yet undetermined type variables
        for (Type t : undetvars)
            minimizeInst((UndetVar) t, warn);

        /** Type variables instantiated to bottom */
        ListBuffer<Type> restvars = new ListBuffer<Type>();

        /** Instantiated types or TypeVars if under-constrained */
        ListBuffer<Type> insttypes = new ListBuffer<Type>();

        /** Instantiated types or UndetVars if under-constrained */
        ListBuffer<Type> undettypes = new ListBuffer<Type>();

        for (Type t : undetvars) {
            UndetVar uv = (UndetVar)t;
            if (uv.inst.tag == BOT) {
                restvars.append(uv.qtype);
                insttypes.append(uv.qtype);
                undettypes.append(uv);
                uv.inst = null;
            } else {
                insttypes.append(uv.inst);
                undettypes.append(uv.inst);
            }
        }
        checkWithinBounds(tvars, undettypes.toList(), warn);

        if (!restvars.isEmpty()) {
            // if there are uninstantiated variables,
            // quantify result type with them
            mt = new MethodType(mt.argtypes,
                                new ForAll(restvars.toList(), mt.restype),
                                mt.thrown, syms.methodClass);
        }

        // return instantiated version of method type
        return types.subst(mt, tvars, insttypes.toList());
    }
    //where

        /** Try to instantiate argument type `that' to given type `to'.
         *  If this fails, try to insantiate `that' to `to' where
         *  every occurrence of a type variable in `tvars' is replaced
         *  by an unknown type.
         */
        private Type instantiateArg(ForAll that,
                                    Type to,
                                    List<Type> tvars,
                                    Warner warn) throws NoInstanceException {
            List<Type> targs;
            try {
                return instantiateExpr(that, to, warn);
            } catch (NoInstanceException ex) {
                Type to1 = to;
                for (List<Type> l = tvars; l.nonEmpty(); l = l.tail)
                    to1 = types.subst(to1, List.of(l.head), List.of(syms.unknownType));
                return instantiateExpr(that, to1, warn);
            }
        }

    /** check that type parameters are within their bounds.
     */
    private void checkWithinBounds(List<Type> tvars,
                                   List<Type> arguments,
                                   Warner warn)
        throws NoInstanceException {
        for (List<Type> tvs = tvars, args = arguments;
             tvs.nonEmpty();
             tvs = tvs.tail, args = args.tail) {
            if (args.head instanceof UndetVar) continue;
            List<Type> bounds = types.subst(types.getBounds((TypeVar)tvs.head), tvars, arguments);
            if (!types.isSubtypeUnchecked(args.head, bounds, warn))
                throw unambiguousNoInstanceException
                    .setMessage("inferred.do.not.conform.to.bounds",
                                arguments, tvars);
        }
    }
}
