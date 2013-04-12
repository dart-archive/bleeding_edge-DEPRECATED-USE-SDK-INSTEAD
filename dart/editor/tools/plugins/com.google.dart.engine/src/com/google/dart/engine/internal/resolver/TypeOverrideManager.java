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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.type.Type;

import java.util.HashMap;

/**
 * Instances of the class {@code TypeOverrideManager} manage the ability to override the type of an
 * element within a given context.
 */
public class TypeOverrideManager {
  /**
   * Instances of the class {@code TypeOverrideScope} represent a scope in which the types of
   * elements can be overridden.
   */
  private static class TypeOverrideScope {
    /**
     * The outer scope in which types might be overridden.
     */
    private TypeOverrideScope outerScope;

    /**
     * A table mapping elements to the overridden type of that element.
     */
    private HashMap<Element, Type> overridenTypes = new HashMap<Element, Type>();

    /**
     * Initialize a newly created scope to be an empty child of the given scope.
     * 
     * @param outerScope the outer scope in which types might be overridden
     */
    public TypeOverrideScope(TypeOverrideScope outerScope) {
      this.outerScope = outerScope;
    }

    /**
     * Return the overridden type of the given element, or {@code null} if the type of the element
     * has not been overridden.
     * 
     * @param element the element whose type might have been overridden
     * @return the overridden type of the given element
     */
    public Type getType(Element element) {
      Type type = overridenTypes.get(element);
      if (type == null && element instanceof PropertyAccessorElement) {
        type = overridenTypes.get(((PropertyAccessorElement) element).getVariable());
      }
      if (type != null) {
        return type;
      } else if (outerScope != null) {
        return outerScope.getType(element);
      }
      return null;
    }

    /**
     * Set the overridden type of the given element to the given type
     * 
     * @param element the element whose type might have been overridden
     * @param type the overridden type of the given element
     */
    public void setType(Element element, Type type) {
      overridenTypes.put(element, type);
    }
  }

  /**
   * The current override scope, or {@code null} if no scope has been entered.
   */
  private TypeOverrideScope currentScope;

  /**
   * Initialize a newly created override manager to not be in any scope.
   */
  public TypeOverrideManager() {
    super();
  }

  /**
   * Enter a new override scope.
   */
  public void enterScope() {
    currentScope = new TypeOverrideScope(currentScope);
  }

  /**
   * Exit the current override scope.
   */
  public void exitScope() {
    if (currentScope == null) {
      throw new IllegalStateException("No scope to exit");
    }
    currentScope = currentScope.outerScope;
  }

  /**
   * Return the overridden type of the given element, or {@code null} if the type of the element has
   * not been overridden.
   * 
   * @param element the element whose type might have been overridden
   * @return the overridden type of the given element
   */
  public Type getType(Element element) {
    if (currentScope == null) {
      return null;
    }
    return currentScope.getType(element);
  }

  /**
   * Set the overridden type of the given element to the given type
   * 
   * @param element the element whose type might have been overridden
   * @param type the overridden type of the given element
   */
  public void setType(Element element, Type type) {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot override without a scope");
    }
    currentScope.setType(element, type);
  }
}
