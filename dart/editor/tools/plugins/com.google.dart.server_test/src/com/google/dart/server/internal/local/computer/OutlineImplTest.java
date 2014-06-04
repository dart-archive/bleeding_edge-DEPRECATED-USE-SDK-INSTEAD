/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.server.internal.local.computer;

import com.google.dart.server.ElementKind;
import com.google.dart.server.Outline;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutlineImplTest extends TestCase {
  public void test_access() throws Exception {
    Outline parent = mock(Outline.class);
    Outline childA = mock(Outline.class);
    Outline childB = mock(Outline.class);
    when(childA.toString()).thenReturn("childA");
    when(childB.toString()).thenReturn("childB");
    Outline[] children = new Outline[] {childA, childB};
    OutlineImpl outline = new OutlineImpl(
        parent,
        ElementKind.COMPILATION_UNIT,
        "name0",
        1,
        2,
        true,
        true,
        "args0",
        "returnType0");
    assertSame(parent, outline.getParent());
    assertEquals(ElementKind.COMPILATION_UNIT, outline.getKind());
    assertEquals("name0", outline.getName());
    assertEquals(1, outline.getOffset());
    assertEquals(2, outline.getLength());
    assertTrue(outline.isAbstract());
    assertFalse(outline.isPrivate());
    assertTrue(outline.isStatic());
    assertEquals("args0", outline.getArguments());
    assertEquals("returnType0", outline.getReturnType());
    // children
    outline.setChildren(children);
    assertEquals(children, outline.getChildren());
  }

}
