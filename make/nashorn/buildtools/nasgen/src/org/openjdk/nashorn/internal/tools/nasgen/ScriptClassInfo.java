/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.nashorn.internal.tools.nasgen;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;
import org.openjdk.nashorn.internal.tools.nasgen.MemberInfo.Kind;

import static org.openjdk.nashorn.internal.tools.nasgen.StringConstants.OBJ_ANNO_PKG;
import static org.openjdk.nashorn.internal.tools.nasgen.StringConstants.RUNTIME_PKG;

/**
 * All annotation information from a class that is annotated with
 * the annotation com.sun.oracle.objects.annotations.ScriptClass.
 *
 */
public final class ScriptClassInfo {
    private static String getTypeDescriptor(final String pkg, final String name) {
        return "L" + pkg + name + ";";
    }

    // descriptors for various annotations
    static final String SCRIPT_CLASS_ANNO_DESC  = getTypeDescriptor(OBJ_ANNO_PKG, "ScriptClass");
    static final String CONSTRUCTOR_ANNO_DESC   = getTypeDescriptor(OBJ_ANNO_PKG, "Constructor");
    static final String FUNCTION_ANNO_DESC      = getTypeDescriptor(OBJ_ANNO_PKG, "Function");
    static final String GETTER_ANNO_DESC        = getTypeDescriptor(OBJ_ANNO_PKG, "Getter");
    static final String SETTER_ANNO_DESC        = getTypeDescriptor(OBJ_ANNO_PKG, "Setter");
    static final String PROPERTY_ANNO_DESC      = getTypeDescriptor(OBJ_ANNO_PKG, "Property");
    static final String WHERE_ENUM_DESC         = getTypeDescriptor(OBJ_ANNO_PKG, "Where");
    static final String LINK_LOGIC_DESC         = getTypeDescriptor(OBJ_ANNO_PKG, "SpecializedFunction$LinkLogic");
    static final String SPECIALIZED_FUNCTION    = getTypeDescriptor(OBJ_ANNO_PKG, "SpecializedFunction");

    static final Map<String, Kind> annotations = new HashMap<>();

    static {
        annotations.put(SCRIPT_CLASS_ANNO_DESC, Kind.SCRIPT_CLASS);
        annotations.put(FUNCTION_ANNO_DESC, Kind.FUNCTION);
        annotations.put(CONSTRUCTOR_ANNO_DESC, Kind.CONSTRUCTOR);
        annotations.put(GETTER_ANNO_DESC, Kind.GETTER);
        annotations.put(SETTER_ANNO_DESC, Kind.SETTER);
        annotations.put(PROPERTY_ANNO_DESC, Kind.PROPERTY);
        annotations.put(SPECIALIZED_FUNCTION, Kind.SPECIALIZED_FUNCTION);
    }

    // name of the script class
    private String name;
    // member info for script properties
    private List<MemberInfo> members = Collections.emptyList();
    // java class name that is annotated with @ScriptClass
    private String javaName;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the members
     */
    public List<MemberInfo> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * @param members the members to set
     */
    public void setMembers(final List<MemberInfo> members) {
        this.members = members;
    }

    MemberInfo getConstructor() {
        for (final MemberInfo memInfo : members) {
            if (memInfo.getKind() == Kind.CONSTRUCTOR) {
                return memInfo;
            }
        }
        return null;
    }

    List<MemberInfo> getSpecializedConstructors() {
        final List<MemberInfo> res = new LinkedList<>();
        for (final MemberInfo memInfo : members) {
            if (memInfo.isSpecializedConstructor()) {
                assert memInfo.getKind() == Kind.SPECIALIZED_FUNCTION;
                res.add(memInfo);
            }
        }
        return Collections.unmodifiableList(res);
    }

    boolean isConstructorNeeded() {
        // Constructor class generation is needed if we one or
        // more constructor properties are defined or @Constructor
        // is defined in the class.
        for (final MemberInfo memInfo : members) {
            if (memInfo.getKind() == Kind.CONSTRUCTOR ||
                memInfo.getWhere() == Where.CONSTRUCTOR) {
                return true;
            }
        }
        return false;
    }

    boolean isPrototypeNeeded() {
        // Prototype class generation is needed if we have at least one
        // prototype property or @Constructor defined in the class.
        for (final MemberInfo memInfo : members) {
            if (memInfo.getWhere() == Where.PROTOTYPE || memInfo.isConstructor()) {
                return true;
            }
        }
        return false;
    }

    int getPrototypeMemberCount() {
        int count = 0;
        for (final MemberInfo memInfo : members) {
            switch (memInfo.getKind()) {
                case SETTER:
                case SPECIALIZED_FUNCTION:
                    // SETTER was counted when GETTER was encountered.
                    // SPECIALIZED_FUNCTION was counted as FUNCTION already.
                    continue;
            }

            if (memInfo.getWhere() == Where.PROTOTYPE) {
                count++;
            }
        }
        return count;
    }

    int getConstructorMemberCount() {
        int count = 0;
        for (final MemberInfo memInfo : members) {
            switch (memInfo.getKind()) {
                case CONSTRUCTOR:
                case SETTER:
                case SPECIALIZED_FUNCTION:
                    // SETTER was counted when GETTER was encountered.
                    // Constructor and constructor SpecializedFunctions
                    // are not added as members and so not counted.
                    continue;
            }

            if (memInfo.getWhere() == Where.CONSTRUCTOR) {
                count++;
            }
        }
        return count;
    }

    int getInstancePropertyCount() {
        int count = 0;
        for (final MemberInfo memInfo : members) {
            switch (memInfo.getKind()) {
                case SETTER:
                case SPECIALIZED_FUNCTION:
                    // SETTER was counted when GETTER was encountered.
                    // SPECIALIZED_FUNCTION was counted as FUNCTION already.
                    continue;
            }

            if (memInfo.getWhere() == Where.INSTANCE) {
                count++;
            }
        }
        return count;
    }

    MemberInfo find(final String findJavaName, final String findJavaDesc, final int findAccess) {
        for (final MemberInfo memInfo : members) {
            if (memInfo.getJavaName().equals(findJavaName) &&
                memInfo.getJavaDesc().equals(findJavaDesc) &&
                memInfo.getJavaAccess() == findAccess) {
                return memInfo;
            }
        }
        return null;
    }

    List<MemberInfo> findSpecializations(final String methodName) {
        final List<MemberInfo> res = new LinkedList<>();
        for (final MemberInfo memInfo : members) {
            if (memInfo.getName().equals(methodName) &&
                memInfo.getKind() == Kind.SPECIALIZED_FUNCTION) {
                res.add(memInfo);
            }
        }
        return Collections.unmodifiableList(res);
    }

    MemberInfo findSetter(final MemberInfo getter) {
        assert getter.getKind() == Kind.GETTER : "getter expected";
        final String getterName = getter.getName();
        final Where getterWhere = getter.getWhere();
        for (final MemberInfo memInfo : members) {
            if (memInfo.getKind() == Kind.SETTER &&
                getterName.equals(memInfo.getName()) &&
                getterWhere == memInfo.getWhere()) {
                return memInfo;
            }
        }
        return null;
    }

    /**
     * @return the javaName
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * @param javaName the javaName to set
     */
    void setJavaName(final String javaName) {
        this.javaName = javaName;
    }

    String getConstructorClassName() {
        return getJavaName() + StringConstants.CONSTRUCTOR_SUFFIX;
    }

    String getPrototypeClassName() {
        return getJavaName() + StringConstants.PROTOTYPE_SUFFIX;
    }

    void verify() {
        boolean constructorSeen = false;
        for (final MemberInfo memInfo : getMembers()) {
            if (memInfo.isConstructor()) {
                if (constructorSeen) {
                    error("more than @Constructor method");
                }
                constructorSeen = true;
            }
            try {
                memInfo.verify();
            } catch (final Exception e) {
                e.printStackTrace();
                error(e.getMessage());
            }
        }
    }

    private void error(final String msg) throws RuntimeException {
        throw new RuntimeException(javaName + " : " + msg);
    }
}
