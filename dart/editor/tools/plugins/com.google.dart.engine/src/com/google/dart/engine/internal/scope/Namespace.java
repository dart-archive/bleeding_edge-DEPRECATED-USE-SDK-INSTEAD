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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.element.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code Namespace} implement a mapping of identifiers to the elements
 * represented by those identifiers. Namespaces are the building blocks for scopes.
 * 
 * @coverage dart.engine.resolver
 */
public class Namespace {
  /**
   * A table mapping names that are defined in this namespace to the element representing the thing
   * declared with that name.
   */
  private HashMap<String, Element> definedNames;

  /**
   * An empty namespace.
   */
  public static final Namespace EMPTY = new Namespace(new HashMap<String, Element>());

  /**
   * Initialize a newly created namespace to have the given defined names.
   * 
   * @param definedNames the mapping from names that are defined in this namespace to the
   *          corresponding elements
   */
  public Namespace(HashMap<String, Element> definedNames) {
    this.definedNames = definedNames;
  }

  /**
   * Return the element in this namespace that is available to the containing scope using the given
   * name.
   * 
   * @param name the name used to reference the
   * @return the element represented by the given identifier
   */
  public Element get(String name) {
    return definedNames.get(name);
  }

  /**
   * Return a table containing the same mappings as those defined by this namespace.
   * 
   * @return a table containing the same mappings as those defined by this namespace
   */
  public Map<String, Element> getDefinedNames() {
    return new HashMap<String, Element>(definedNames);
  }
}
