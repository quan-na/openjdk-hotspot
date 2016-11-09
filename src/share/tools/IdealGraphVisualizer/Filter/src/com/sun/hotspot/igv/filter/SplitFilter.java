/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.filter;

import com.sun.hotspot.igv.graph.*;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class SplitFilter extends AbstractFilter {

    private String name;
    private Selector selector;
    private String propertyName;

    public SplitFilter(String name, Selector selector, String propertyName) {
        this.name = name;
        this.selector = selector;
        this.propertyName = propertyName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apply(Diagram d) {
        List<Figure> list = selector.selected(d);

        for (Figure f : list) {

            for (InputSlot is : f.getInputSlots()) {
                for (Connection c : is.getConnections()) {
                    OutputSlot os = c.getOutputSlot();
                    if (f.getSource().getSourceNodes().size() > 0) {
                        os.getSource().addSourceNodes(f.getSource());
                        os.setAssociatedNode(f.getSource().getSourceNodes().get(0));
                        os.setColor(f.getColor());
                    }


                    String s = Figure.resolveString(propertyName, f.getProperties());
                    if (s != null) {
                        os.setShortName(s);
                    }

                }
            }
            for (OutputSlot os : f.getOutputSlots()) {
                for (Connection c : os.getConnections()) {
                    InputSlot is = c.getInputSlot();
                    if (f.getSource().getSourceNodes().size() > 0) {
                        is.getSource().addSourceNodes(f.getSource());
                        is.setAssociatedNode(f.getSource().getSourceNodes().get(0));
                        is.setColor(f.getColor());
                    }

                    String s = Figure.resolveString(propertyName, f.getProperties());
                    if (s != null) {
                        is.setShortName(s);
                    }
                }
            }

            d.removeFigure(f);
        }
    }
}
