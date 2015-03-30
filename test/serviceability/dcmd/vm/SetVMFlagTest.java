/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.java.testlibrary.OutputAnalyzer;
import com.oracle.java.testlibrary.dcmd.CommandExecutor;
import com.oracle.java.testlibrary.dcmd.JMXExecutor;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8054890
 * @summary Test of VM.set_flag diagnostic command
 * @library /testlibrary
 * @build com.oracle.java.testlibrary.*
 * @build com.oracle.java.testlibrary.dcmd.*
 * @run testng SetVMFlagTest
 */

public class SetVMFlagTest {
    private static final String PRINTGC_NAME = "PrintGC";
    private static final String XMX_NAME = "MaxHeapSize";

    private static final String PRINTGC_PATTERN = "\\s*bool " + PRINTGC_NAME +
                                                  "\\s+[\\:]?=\\s+(.*?)\\s+\\{manageable\\}";
    private static final String XMX_PATTERN = "\\s*uintx " + XMX_NAME +
                                                  "\\s+[\\:]?=\\s+(.*?)\\s+\\{product\\}";

    public void run(CommandExecutor executor) {
        setMutableFlag(executor);
        setMutableFlagWithInvalidValue(executor);
        setImmutableFlag(executor);
        setNonExistingFlag(executor);
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }

    private void setMutableFlag(CommandExecutor executor) {
        OutputAnalyzer out = getAllFlags(executor);
        String flagVal = out.firstMatch(PRINTGC_PATTERN, 1);

        if (flagVal == null) {
            System.err.println(out.getOutput());
            throw new Error("'" + PRINTGC_NAME + "' flag is either not available or manageable");
        }

        Boolean blnVal = Boolean.parseBoolean(flagVal);

        out = executor.execute("VM.set_flag " + PRINTGC_NAME + " " + (blnVal ? 0 : 1));
        out.stderrShouldBeEmpty();

        out = getAllFlags(executor);

        String newFlagVal = out.firstMatch(PRINTGC_PATTERN, 1);

        assertNotEquals(newFlagVal, flagVal);
    }

    private void setMutableFlagWithInvalidValue(CommandExecutor executor) {
        OutputAnalyzer out = getAllFlags(executor);
        String flagVal = out.firstMatch(PRINTGC_PATTERN, 1);

        if (flagVal == null) {
            System.err.println(out.getOutput());
            throw new Error("'" + PRINTGC_NAME + "' flag is either not available or manageable");
        }

        // PrintGC is a boolean flag and accepts only 0/1 as its value
        out = executor.execute("VM.set_flag " + PRINTGC_NAME + " unexpected_value");
        out.stderrShouldBeEmpty();
        out.stdoutShouldContain("flag value must be a boolean (1 or 0)");

        out = getAllFlags(executor);

        String newFlagVal = out.firstMatch(PRINTGC_PATTERN, 1);

        assertEquals(newFlagVal, flagVal);
    }

    private void setImmutableFlag(CommandExecutor executor) {
        OutputAnalyzer out = getAllFlags(executor);
        String flagVal = out.firstMatch(XMX_PATTERN, 1);

        if (flagVal == null) {
            System.err.println(out.getOutput());
            throw new Error("'" + XMX_NAME + "' flag is not available or immutable");
        }

        Long numVal = Long.parseLong(flagVal);

        out = executor.execute("VM.set_flag " + XMX_NAME + " " + (numVal + 1));
        out.stderrShouldBeEmpty();
        out.stdoutShouldContain("only 'writeable' flags can be set");

        out = getAllFlags(executor);

        String newFlagVal = out.firstMatch(XMX_PATTERN, 1);

        assertEquals(newFlagVal, flagVal);
    }

    private void setNonExistingFlag(CommandExecutor executor) {
        String unknownFlag = "ThisIsUnknownFlag";
        OutputAnalyzer out = executor.execute("VM.set_flag " + unknownFlag + " 1");
        out.stderrShouldBeEmpty();
        out.stdoutShouldContain("flag " + unknownFlag + " does not exist");
    }

    private OutputAnalyzer getAllFlags(CommandExecutor executor) {
        return executor.execute("VM.flags -all");
    }
}