/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.graalvm.compiler.hotspot.replacements;

import static org.graalvm.compiler.core.common.CompilationIdentifier.INVALID_COMPILATION_ID;

import java.lang.reflect.Method;

import org.graalvm.compiler.core.common.type.StampPair;
import org.graalvm.compiler.debug.Debug;
import org.graalvm.compiler.debug.Debug.Scope;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import org.graalvm.compiler.nodes.ParameterNode;
import org.graalvm.compiler.nodes.ReturnNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.compiler.nodes.java.NewInstanceNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.spi.ArrayLengthProvider;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.Replacements;
import org.graalvm.compiler.nodes.spi.VirtualizableAllocation;
import org.graalvm.compiler.nodes.type.StampTool;
import org.graalvm.compiler.replacements.nodes.BasicObjectCloneNode;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

@NodeInfo
public final class ObjectCloneNode extends BasicObjectCloneNode implements VirtualizableAllocation, ArrayLengthProvider {

    public static final NodeClass<ObjectCloneNode> TYPE = NodeClass.create(ObjectCloneNode.class);

    public ObjectCloneNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode receiver) {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, receiver);
    }

    @Override
    @SuppressWarnings("try")
    protected StructuredGraph getLoweredSnippetGraph(LoweringTool tool) {
        ResolvedJavaType type = StampTool.typeOrNull(getObject());
        if (type != null) {
            if (type.isArray()) {
                Method method = ObjectCloneSnippets.arrayCloneMethods.get(type.getComponentType().getJavaKind());
                if (method != null) {
                    final ResolvedJavaMethod snippetMethod = tool.getMetaAccess().lookupJavaMethod(method);
                    final Replacements replacements = tool.getReplacements();
                    StructuredGraph snippetGraph = null;
                    try (Scope s = Debug.scope("ArrayCloneSnippet", snippetMethod)) {
                        snippetGraph = replacements.getSnippet(snippetMethod, null);
                    } catch (Throwable e) {
                        throw Debug.handle(e);
                    }

                    assert snippetGraph != null : "ObjectCloneSnippets should be installed";
                    return lowerReplacement((StructuredGraph) snippetGraph.copy(), tool);
                }
                assert false : "unhandled array type " + type.getComponentType().getJavaKind();
            } else {
                Assumptions assumptions = graph().getAssumptions();
                type = getConcreteType(getObject().stamp());
                if (type != null) {
                    StructuredGraph newGraph = new StructuredGraph(AllowAssumptions.from(assumptions != null), INVALID_COMPILATION_ID);
                    ParameterNode param = newGraph.addWithoutUnique(new ParameterNode(0, StampPair.createSingle(getObject().stamp())));
                    NewInstanceNode newInstance = newGraph.add(new NewInstanceNode(type, true));
                    newGraph.addAfterFixed(newGraph.start(), newInstance);
                    ReturnNode returnNode = newGraph.add(new ReturnNode(newInstance));
                    newGraph.addAfterFixed(newInstance, returnNode);

                    for (ResolvedJavaField field : type.getInstanceFields(true)) {
                        LoadFieldNode load = newGraph.add(LoadFieldNode.create(newGraph.getAssumptions(), param, field));
                        newGraph.addBeforeFixed(returnNode, load);
                        newGraph.addBeforeFixed(returnNode, newGraph.add(new StoreFieldNode(newInstance, field, load)));
                    }
                    return lowerReplacement(newGraph, tool);
                }
            }
        }
        return null;
    }
}
