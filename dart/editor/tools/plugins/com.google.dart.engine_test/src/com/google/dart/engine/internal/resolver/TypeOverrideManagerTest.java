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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

public class TypeOverrideManagerTest extends EngineTestCase {
  public void test_exitScope_noScopes() {
    TypeOverrideManager manager = new TypeOverrideManager();
    try {
      manager.exitScope();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException exception) {
      // Expected
    }
  }

  public void test_exitScope_oneScope() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    manager.exitScope();
    try {
      manager.exitScope();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException exception) {
      // Expected
    }
  }

  public void test_exitScope_twoScopes() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    manager.exitScope();
    manager.enterScope();
    manager.exitScope();
    try {
      manager.exitScope();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException exception) {
      // Expected
    }
  }

  public void test_getType_enclosedOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    LocalVariableElementImpl element = localVariableElement("v");
    InterfaceType type = classElement("C").getType();
    manager.enterScope();
    manager.setType(element, type);
    manager.enterScope();
    assertSame(type, manager.getType(element));
  }

  public void test_getType_immediateOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    LocalVariableElementImpl element = localVariableElement("v");
    InterfaceType type = classElement("C").getType();
    manager.enterScope();
    manager.setType(element, type);
    assertSame(type, manager.getType(element));
  }

  public void test_getType_noOverride() {
    TypeOverrideManager manager = new TypeOverrideManager();
    manager.enterScope();
    assertNull(manager.getType(localVariableElement("v")));
  }

  public void test_getType_noScope() {
    TypeOverrideManager manager = new TypeOverrideManager();
    assertNull(manager.getType(localVariableElement("v")));
  }
}
