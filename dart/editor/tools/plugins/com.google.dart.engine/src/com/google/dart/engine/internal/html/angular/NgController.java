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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.type.InterfaceType;

/**
 * {@link NgController} describes any <code>NgController</code> annotation instance.
 */
class NgController extends NgAnnotation {
  private final ClassElement element;
  private final String name;

  public NgController(ClassElement element, InjectSelector selector, String name) {
    super(selector);
    this.element = element;
    this.name = name;
  }

  @Override
  public void apply(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    InterfaceType type = element.getType();
    LocalVariableElementImpl variable = resolver.createLocalVariable(type, name);
    resolver.defineVariable(variable);
  }
}
