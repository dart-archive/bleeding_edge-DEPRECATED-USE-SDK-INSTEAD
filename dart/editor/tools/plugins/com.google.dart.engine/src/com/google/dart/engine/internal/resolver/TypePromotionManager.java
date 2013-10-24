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
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.Type;

import java.util.Collection;
import java.util.HashMap;

/**
 * Instances of the class {@code TypePromotionManager} manage the ability to promote types of local
 * variables and formal parameters from their declared types based on control flow.
 */
public class TypePromotionManager {
  /**
   * Instances of the class {@code TypePromoteScope} represent a scope in which the types of
   * elements can be promoted.
   */
  private static class TypePromoteScope {
    /**
     * The outer scope in which types might be promoter.
     */
    private TypePromoteScope outerScope;

    /**
     * A table mapping elements to the promoted type of that element.
     */
    private HashMap<Element, Type> promotedTypes = new HashMap<Element, Type>();

    /**
     * Initialize a newly created scope to be an empty child of the given scope.
     * 
     * @param outerScope the outer scope in which types might be promoted
     */
    public TypePromoteScope(TypePromoteScope outerScope) {
      this.outerScope = outerScope;
    }

    /**
     * Returns the elements with promoted types.
     */
    public Collection<Element> getPromotedElements() {
      return promotedTypes.keySet();
    }

    /**
     * Return the promoted type of the given element, or {@code null} if the type of the element has
     * not been promoted.
     * 
     * @param element the element whose type might have been promoted
     * @return the promoted type of the given element
     */
    public Type getType(Element element) {
      Type type = promotedTypes.get(element);
      if (type == null && element instanceof PropertyAccessorElement) {
        type = promotedTypes.get(((PropertyAccessorElement) element).getVariable());
      }
      if (type != null) {
        return type;
      } else if (outerScope != null) {
        return outerScope.getType(element);
      }
      return null;
    }

    /**
     * Set the promoted type of the given element to the given type.
     * 
     * @param element the element whose type might have been promoted
     * @param type the promoted type of the given element
     */
    public void setType(Element element, Type type) {
      promotedTypes.put(element, type);
    }
  }

  /**
   * The current promotion scope, or {@code null} if no scope has been entered.
   */
  private TypePromoteScope currentScope;

  /**
   * Initialize a newly created promotion manager to not be in any scope.
   */
  public TypePromotionManager() {
    super();
  }

  /**
   * Enter a new promotions scope.
   */
  public void enterScope() {
    currentScope = new TypePromoteScope(currentScope);
  }

  /**
   * Exit the current promotion scope.
   */
  public void exitScope() {
    if (currentScope == null) {
      throw new IllegalStateException("No scope to exit");
    }
    currentScope = currentScope.outerScope;
  }

  /**
   * Returns the elements with promoted types.
   */
  public Collection<Element> getPromotedElements() {
    return currentScope.getPromotedElements();
  }

  /**
   * Returns static type of the given variable - declared or promoted.
   * 
   * @return the static type of the given variable - declared or promoted
   */
  public Type getStaticType(VariableElement variable) {
    Type staticType = getType(variable);
    if (staticType == null) {
      staticType = variable.getType();
    }
    return staticType;
  }

  /**
   * Return the promoted type of the given element, or {@code null} if the type of the element has
   * not been promoted.
   * 
   * @param element the element whose type might have been promoted
   * @return the promoted type of the given element
   */
  public Type getType(Element element) {
    if (currentScope == null) {
      return null;
    }
    return currentScope.getType(element);
  }

  /**
   * Set the promoted type of the given element to the given type.
   * 
   * @param element the element whose type might have been promoted
   * @param type the promoted type of the given element
   */
  public void setType(Element element, Type type) {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot promote without a scope");
    }
    currentScope.setType(element, type);
  }
}
