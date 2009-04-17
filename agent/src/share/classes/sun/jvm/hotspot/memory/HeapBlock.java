/*
 * Copyright 2000-2005 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

package sun.jvm.hotspot.memory;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;

public class HeapBlock extends VMObject {
  private static long          heapBlockSize;
  private static Field         headerField;
  // Fields for class Header
  private static CIntegerField headerLengthField;
  private static CIntegerField headerUsedField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    // HeapBlock
    Type type = db.lookupType("HeapBlock");
    heapBlockSize     = type.getSize();
    headerField       = type.getField("_header");

    // Header
    type = db.lookupType("HeapBlock::Header");
    headerLengthField = type.getCIntegerField("_length");
    headerUsedField   = type.getCIntegerField("_used");
  }

  public HeapBlock(Address addr) {
    super(addr);
  }

  public long getLength() {
    return getHeader().getLength();
  }

  public boolean isFree() {
    return getHeader().isFree();
  }

  public Address getAllocatedSpace() {
    return addr.addOffsetTo(heapBlockSize);
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  public static class Header extends VMObject {
    public Header(Address addr) {
      super(addr);
    }

    public long getLength() {
      return headerLengthField.getValue(addr);
    }

    public boolean isFree() {
      return (headerUsedField.getValue(addr) == 0);
    }
  }

  private Header getHeader() {
    return (Header) VMObjectFactory.newObject(HeapBlock.Header.class, addr.addOffsetTo(headerField.getOffset()));
  }
}
