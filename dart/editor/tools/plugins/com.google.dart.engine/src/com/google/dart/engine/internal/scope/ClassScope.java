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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;

/**
 * Instances of the class {@code ClassScope} implement the scope defined by a class.
 * 
 * @coverage dart.engine.resolver
 */
public class ClassScope extends EnclosedScope {
  // TODO(brianwilkerson) This does not yet distinguish between static and instance scopes. It isn't
  // clear whether we need to do so or whether it might not be easier and better to simply check
  // when resolving to an instance element that the reference is in an instance scope.

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param typeElement the element representing the type represented by this scope
   */
  public ClassScope(Scope enclosingScope, ClassElement typeElement) {
    super(enclosingScope);
    if (typeElement == null) {
      throw new IllegalArgumentException("class element cannot be null");
    }
    defineMembers(typeElement);
  }

  @Override
  protected AnalysisError getErrorForDuplicate(Element existing, Element duplicate) {
    if (existing instanceof PropertyAccessorElement && duplicate instanceof MethodElement) {
      if (existing.getNameOffset() < duplicate.getNameOffset()) {
        return new AnalysisError(
            duplicate.getSource(),
            duplicate.getNameOffset(),
            duplicate.getDisplayName().length(),
            CompileTimeErrorCode.METHOD_AND_GETTER_WITH_SAME_NAME,
            existing.getDisplayName());
      } else {
        return new AnalysisError(
            existing.getSource(),
            existing.getNameOffset(),
            existing.getDisplayName().length(),
            CompileTimeErrorCode.GETTER_AND_METHOD_WITH_SAME_NAME,
            existing.getDisplayName());
      }
    }
    return super.getErrorForDuplicate(existing, duplicate);
  }

  /**
   * Define the instance members defined by the class.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineMembers(ClassElement typeElement) {
    for (PropertyAccessorElement accessor : typeElement.getAccessors()) {
      define(accessor);
    }
    for (MethodElement method : typeElement.getMethods()) {
      define(method);
    }
  }
}
