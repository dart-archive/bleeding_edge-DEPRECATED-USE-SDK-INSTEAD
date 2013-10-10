/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MultiplyDefinedElement;

import static com.google.dart.engine.element.ElementFactory.localVariableElement;

public class MultiplyDefinedElementImplTest extends EngineTestCase {
  public void test_fromElements_conflicting() {
    Element firstElement = localVariableElement("xx");
    Element secondElement = localVariableElement("yy");
    Element result = MultiplyDefinedElementImpl.fromElements(null, firstElement, secondElement);
    assertInstanceOf(MultiplyDefinedElement.class, result);
    Element[] elements = ((MultiplyDefinedElement) result).getConflictingElements();
    assertLength(2, elements);
    for (int i = 0; i < elements.length; i++) {
      assertInstanceOf(LocalVariableElement.class, elements[i]);
    }
  }

  public void test_fromElements_multiple() {
    Element firstElement = localVariableElement("xx");
    Element secondElement = localVariableElement("yy");
    Element thirdElement = localVariableElement("zz");
    Element result = MultiplyDefinedElementImpl.fromElements(
        null,
        MultiplyDefinedElementImpl.fromElements(null, firstElement, secondElement),
        thirdElement);
    assertInstanceOf(MultiplyDefinedElement.class, result);
    Element[] elements = ((MultiplyDefinedElement) result).getConflictingElements();
    assertLength(3, elements);
    for (int i = 0; i < elements.length; i++) {
      assertInstanceOf(LocalVariableElement.class, elements[i]);
    }
  }

  public void test_fromElements_nonConflicting() {
    Element element = localVariableElement("xx");
    assertSame(element, MultiplyDefinedElementImpl.fromElements(null, element, element));
  }
}
