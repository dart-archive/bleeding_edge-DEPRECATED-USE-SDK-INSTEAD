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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.Method;

/**
 * Instances of the class <code>MethodOverrideContributor</code> implement a contributor that adds a
 * reference from a method to any methods that it directly overrides.
 */
public class MethodOverrideContributor extends DartContributor {
  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) element;
      MethodElement overridenMethodElement = findOverriddenMethod(methodElement);
      DartFunction function = getDartElement(overridenMethodElement);
      if (function instanceof Method) {
        Method method = (Method) function;
        recordRelationship(
            node,
            new MethodLocation(method, getSourceRange(method, overridenMethodElement)));
      }
    }
    return super.visitMethodDefinition(node);
  }

  /**
   * Return the method defined in the given type that matches the given method.
   * 
   * @param method the method being matched against
   * @param type the type in which the candidate methods are declared
   * @return the method defined in the type that matches the method
   */
  private MethodElement findMatchingMethod(MethodElement method, ClassElement type) {
    if (method instanceof ConstructorElement) {
      for (ConstructorElement candidateMethod : type.getConstructors()) {
        if (matches(method, candidateMethod)) {
          return candidateMethod;
        }
      }
    } else {
      for (Element member : type.getMembers()) {
        if (member instanceof MethodElement) {
          MethodElement candidateMethod = (MethodElement) member;
          if (matches(method, candidateMethod)) {
            return candidateMethod;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the method that the given method overrides, or <code>null</code> if the given method
   * does not override another method.
   * 
   * @param method the method that might override another method
   * @return the method that the given method overrides
   */
  private MethodElement findOverriddenMethod(MethodElement method) {
    Element enclosingElement = method.getEnclosingElement();
    if (!(enclosingElement instanceof ClassElement)) {
      // The element represents a function, and functions cannot override other functions.
      return null;
    }
    ClassElement superclass = getSuperclass((ClassElement) enclosingElement);
    while (superclass != null) {
      MethodElement matchingMethod = findMatchingMethod(method, superclass);
      if (matchingMethod != null) {
        return matchingMethod;
      }
      superclass = getSuperclass(superclass);
    }
    return null;
  }

  /**
   * Return the superclass of the given class, or <code>null</code> if the given class does not have
   * a superclass or if the superclass cannot be determined.
   * 
   * @param classElement the class being accessed
   * @return the superclass of the given class
   */
  private ClassElement getSuperclass(ClassElement classElement) {
    InterfaceType superType = classElement.getSupertype();
    if (superType == null) {
      return null;
    }
    return superType.getElement();
  }

  /**
   * Return <code>true</code> if the given candidate matches the given target.
   * 
   * @param targetMethod the method being matched against
   * @param candidateMethod the candidate being compared to the target
   * @return <code>true</code> if the candidate matches the target
   */
  private boolean matches(MethodElement targetMethod, MethodElement candidateMethod) {
    return targetMethod.getName().equals(candidateMethod.getName());
  }
}
