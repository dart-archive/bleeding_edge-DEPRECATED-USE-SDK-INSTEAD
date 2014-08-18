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

package com.google.dart.server.internal;

import com.google.dart.server.TypeHierarchyItem;
import com.google.dart.server.generated.types.Element;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeHierarchyItemImplTest extends TestCase {
  private Element classElement = mock(Element.class);
  private String displayName = "displayName";
  private Element memberElement = mock(Element.class);

  public void test_access() throws Exception {
    TypeHierarchyItem extendedType = mock(TypeHierarchyItem.class);
    when(extendedType.toString()).thenReturn("MySuperType");
    TypeHierarchyItem[] mixedTypes = new TypeHierarchyItem[1];
    TypeHierarchyItem[] implementedTypes = new TypeHierarchyItem[2];
    TypeHierarchyItem[] subTypes = new TypeHierarchyItem[3];
    TypeHierarchyItemImpl item = new TypeHierarchyItemImpl(
        classElement,
        displayName,
        memberElement,
        extendedType,
        implementedTypes,
        mixedTypes,
        subTypes);
    assertSame(classElement, item.getClassElement());
    assertSame(displayName, item.getDisplayName());
    assertSame(memberElement, item.getMemberElement());
    assertSame(extendedType, item.getSuperclass());
    assertSame(mixedTypes, item.getMixins());
    assertSame(implementedTypes, item.getInterfaces());
    assertSame(subTypes, item.getSubclasses());
    // toString()
    assertNotNull(item.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(classElement.toString()).thenReturn("MyClass");
    when(memberElement.toString()).thenReturn("myMember");
  }
}
