/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class NameElementImplTest extends EngineTestCase {
  @SuppressWarnings("unchecked")
  public void test_accept() throws Exception {
    ElementVisitor<Void> visitor = mock(ElementVisitor.class);
    // nothing to visit
    NameElementImpl element = new NameElementImpl("test");
    element.accept(visitor);
    verifyZeroInteractions(visitor);
  }

  public void test_new() throws Exception {
    NameElementImpl element = new NameElementImpl("test");
    assertEquals("name:test", element.getName());
    assertEquals(-1, element.getNameOffset());
    assertSame(ElementKind.NAME, element.getKind());
  }
}
