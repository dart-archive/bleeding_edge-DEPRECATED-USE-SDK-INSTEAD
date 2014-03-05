/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.InheritanceManager;

/**
 * Instances of the class {@code OverrideVerifier} visit all of the declarations in a compilation
 * unit to verify that if they have an override annotation it is being used correctly.
 */
public class OverrideVerifier extends RecursiveAstVisitor<Void> {
  /**
   * The inheritance manager used to find overridden methods.
   */
  private InheritanceManager manager;

  /**
   * The error reporter used to report errors.
   */
  private ErrorReporter errorReporter;

  /**
   * Initialize a newly created verifier to look for inappropriate uses of the override annotation.
   * 
   * @param manager the inheritance manager used to find overridden methods
   * @param errorReporter the error reporter used to report errors
   */
  public OverrideVerifier(InheritanceManager manager, ErrorReporter errorReporter) {
    this.manager = manager;
    this.errorReporter = errorReporter;
  }

  //
  // As future enhancements, consider adding a hint when
  // - an override annotation is found on anything other than a method or field, or
  // - a method or field that overrides another does not have an annotation.
  //

//  @Override
//  public Void visitFieldDeclaration(FieldDeclaration node) {
//    // TODO(brianwilkerson) Override can also be applied to fields, in which case we need to check
//    // the getter and setter (not clear whether both should override, or if it's enough that one
//    // overrides something; probably the latter).
//    return super.visitFieldDeclaration(node);
//  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement element = node.getElement();
    if (isOverride(element)) {
      if (getOverriddenMember(element) == null) {
        if (element instanceof MethodElement) {
          errorReporter.reportErrorForNode(HintCode.OVERRIDE_ON_NON_OVERRIDING_METHOD, node.getName());
        } else if (element instanceof PropertyAccessorElement) {
          if (((PropertyAccessorElement) element).isGetter()) {
            errorReporter.reportErrorForNode(HintCode.OVERRIDE_ON_NON_OVERRIDING_GETTER, node.getName());
          } else {
            errorReporter.reportErrorForNode(HintCode.OVERRIDE_ON_NON_OVERRIDING_SETTER, node.getName());
          }
        }
      }
    }
    return super.visitMethodDeclaration(node);
  }

  /**
   * Return the member that overrides the given member.
   * 
   * @param member the member that overrides the returned member
   * @return the member that overrides the given member
   */
  private ExecutableElement getOverriddenMember(ExecutableElement member) {
    LibraryElement library = member.getLibrary();
    if (library == null) {
      return null;
    }
    ClassElement classElement = member.getAncestor(ClassElement.class);
    if (classElement == null) {
      return null;
    }
    return manager.lookupInheritance(classElement, member.getName());
  }

  /**
   * Return {@code true} if the given element has an override annotation associated with it.
   * 
   * @param element the element being tested
   * @return {@code true} if the element has an override annotation associated with it
   */
  private boolean isOverride(Element element) {
    return element != null && element.isOverride();
  }
}
