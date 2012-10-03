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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class TypeVariableTypeImplTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new TypeVariableTypeImpl(new TypeVariableElementImpl(identifier("E"))));
  }

  public void test_getElement() {
    TypeVariableElementImpl element = new TypeVariableElementImpl(identifier("E"));
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);
    assertEquals(element, type.getElement());
  }
}
