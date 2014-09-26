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

import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.type.UnionTypeImpl;
import com.google.dart.engine.type.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Map<VariableElement, Type> overridenTypes = new HashMap<VariableElement, Type>();

    /**
     * Initialize a newly created scope to be an empty child of the given scope.
     * 
     * @param outerScope the outer scope in which types might be overridden
     */
    public TypeOverrideScope(TypeOverrideScope outerScope) {
      this.outerScope = outerScope;
    }

    /**
     * Apply a set of overrides that were previously captured.
     * 
     * @param overrides the overrides to be applied
     */
    public void applyOverrides(Map<VariableElement, Type> overrides) {
      for (Map.Entry<VariableElement, Type> entry : overrides.entrySet()) {
        overridenTypes.put(entry.getKey(), entry.getValue());
      }
    }

    /**
     * Return a table mapping the elements whose type is overridden in the current scope to the
     * overriding type.
     * 
     * @return the overrides in the current scope
     */
    public Map<VariableElement, Type> captureLocalOverrides() {
      return overridenTypes;
    }

    /**
     * Return a map from the elements for the variables in the given list that have their types
     * overridden to the overriding type.
     * 
     * @param variableList the list of variables whose overriding types are to be captured
     * @return a table mapping elements to their overriding types
     */
    public Map<VariableElement, Type> captureOverrides(VariableDeclarationList variableList) {
      Map<VariableElement, Type> overrides = new HashMap<VariableElement, Type>();
      if (variableList.isConst() || variableList.isFinal()) {
        for (VariableDeclaration variable : variableList.getVariables()) {
          VariableElement element = variable.getElement();
          if (element != null) {
            Type type = overridenTypes.get(element);
            if (type != null) {
              overrides.put(element, type);
            }
          }
        }
      }
      return overrides;
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
    public void setType(VariableElement element, Type type) {
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
   * Apply a set of overrides that were previously captured.
   * 
   * @param overrides the overrides to be applied
   */
  public void applyOverrides(Map<VariableElement, Type> overrides) {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot apply overrides without a scope");
    }
    currentScope.applyOverrides(overrides);
  }

  /**
   * Return a table mapping the elements whose type is overridden in the current scope to the
   * overriding type.
   * 
   * @return the overrides in the current scope
   */
  public Map<VariableElement, Type> captureLocalOverrides() {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot capture local overrides without a scope");
    }
    return currentScope.captureLocalOverrides();
  }

  /**
   * Return a map from the elements for the variables in the given list that have their types
   * overridden to the overriding type.
   * 
   * @param variableList the list of variables whose overriding types are to be captured
   * @return a table mapping elements to their overriding types
   */
  public Map<VariableElement, Type> captureOverrides(VariableDeclarationList variableList) {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot capture overrides without a scope");
    }
    return currentScope.captureOverrides(variableList);
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
   * Return the best type information available for the given element. If the type of the element
   * has been overridden, then return the overriding type. Otherwise, return the static type.
   * 
   * @param element the element for which type information is to be returned
   * @return the best type information available for the given element
   */
  public Type getBestType(VariableElement element) {
    Type bestType = getType(element);
    return bestType == null ? element.getType() : bestType;
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
   * Update overrides assuming {@code perBranchOverrides} is the collection of per-branch overrides
   * for *all* branches flowing into a join point. If a variable is updated in each per-branch
   * override, then its type before the branching is ignored. Otherwise, its type before the
   * branching is merged with all updates in the branches.
   * <p>
   * Although this method would do the right thing for a single set of overrides, we require there
   * to be at least two override sets. Instead use {@code applyOverrides} for to apply a single set.
   * <p>
   * For example, for the code
   * 
   * <pre>
   *   if (c) {
   *     ...
   *   } else {
   *     ...
   *   }
   * </pre>
   * the {@code perBranchOverrides} would include overrides for the then and else branches, and for
   * the code
   * 
   * <pre>
   *   ...
   *   while(c) {
   *     ...
   *   }
   * </pre>
   * the {@code perBranchOverrides} would include overrides for before the loop and for the loop
   * body.
   * 
   * @param perBranchOverrides one set of overrides for each (at least two) branch flowing into the
   *          join point
   */
  public void joinOverrides(List<Map<VariableElement, Type>> perBranchOverrides) {
    if (perBranchOverrides.size() < 2) {
      throw new IllegalArgumentException("There is no point in joining zero or one override sets.");
    }
    Set<VariableElement> allElements = new HashSet<VariableElement>();
    Set<VariableElement> commonElements = new HashSet<VariableElement>(
        perBranchOverrides.get(0).keySet());
    for (Map<VariableElement, Type> os : perBranchOverrides) {
      // Union: elements updated in some branch.
      allElements.addAll(os.keySet());
      // Intersection: elements updated in all branches.
      commonElements.retainAll(os.keySet());
    }
    Set<VariableElement> uncommonElements = allElements;
    // Difference: elements updated in some but not all branches.
    uncommonElements.removeAll(commonElements);

    Map<VariableElement, Type> joinOverrides = new HashMap<VariableElement, Type>();
    // The common elements were updated in all branches, so their type
    // before branching can be ignored.
    for (VariableElement e : commonElements) {
      joinOverrides.put(e, perBranchOverrides.get(0).get(e));
      for (Map<VariableElement, Type> os : perBranchOverrides) {
        joinOverrides.put(e, UnionTypeImpl.union(joinOverrides.get(e), os.get(e)));
      }
    }
    // The uncommon elements were updated in some but not all branches,
    // so they may still have the type they had before branching.
    for (VariableElement e : uncommonElements) {
      joinOverrides.put(e, getBestType(e));
      for (Map<VariableElement, Type> os : perBranchOverrides) {
        if (os.containsKey(e)) {
          joinOverrides.put(e, UnionTypeImpl.union(joinOverrides.get(e), os.get(e)));
        }
      }
    }
    applyOverrides(joinOverrides);
  }

  /**
   * Set the overridden type of the given element to the given type
   * 
   * @param element the element whose type might have been overridden
   * @param type the overridden type of the given element
   */
  public void setType(VariableElement element, Type type) {
    if (currentScope == null) {
      throw new IllegalStateException("Cannot override without a scope");
    }
    currentScope.setType(element, type);
  }
}
