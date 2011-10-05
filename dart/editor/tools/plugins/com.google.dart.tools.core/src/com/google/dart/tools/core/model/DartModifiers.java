/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.model;

import com.google.dart.compiler.ast.Modifiers;

public class DartModifiers {
  private Modifiers modifiers;

  public DartModifiers(Modifiers modifiers) {
    this.modifiers = modifiers;
  }

  public boolean isAbstract() {
    return modifiers.isAbstract();
  }

  public boolean isConstant() {
    return modifiers.isConstant();
  }

  public boolean isFactory() {
    return modifiers.isFactory();
  }

  public boolean isFinal() {
    return modifiers.isFinal();
  }

  public boolean isGetter() {
    return modifiers.isGetter();
  }

  public boolean isSetter() {
    return modifiers.isSetter();
  }

  public boolean isStatic() {
    return modifiers.isStatic();
  }
}
