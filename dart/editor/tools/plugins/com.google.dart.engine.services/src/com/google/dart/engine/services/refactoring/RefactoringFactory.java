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

package com.google.dart.engine.services.refactoring;

import com.google.common.base.Preconditions;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularFilterElement;
import com.google.dart.engine.element.angular.AngularHasAttributeSelectorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularTagSelectorElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.internal.refactoring.ConvertGetterToMethodRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.ConvertMethodToGetterRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.ExtractLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.ExtractMethodRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.InlineLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.InlineMethodRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularComponentRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularControllerRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularFilterRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularHasAttributeSelectorRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularPropertyRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularScopePropertyRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameAngularTagSelectorRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameClassMemberRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameConstructorRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameImportRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameLibraryRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameUnitMemberRefactoringImpl;

/**
 * Factory for creating {@link Refactoring} instances.
 */
public class RefactoringFactory {
  /**
   * @return the {@link ConvertGetterToMethodRefactoring} the {@link MethodDeclaration} of the given
   *         {@link PropertyAccessorElement} into normal method.
   */
  public static ConvertGetterToMethodRefactoring createConvertGetterToMethodRefactoring(
      SearchEngine searchEngine, PropertyAccessorElement element) {
    return new ConvertGetterToMethodRefactoringImpl(searchEngine, element);
  }

  /**
   * @return the {@link ConvertMethodToGetterRefactoring} the {@link MethodDeclaration} of the given
   *         {@link Element} into getter.
   */
  public static ConvertMethodToGetterRefactoring createConvertMethodToGetterRefactoring(
      SearchEngine searchEngine, ExecutableElement element) {
    return new ConvertMethodToGetterRefactoringImpl(searchEngine, element);
  }

  /**
   * @return the {@link ExtractLocalRefactoring} to extract {@link Expression} selected in given
   *         {@link AssistContext}.
   */
  public static ExtractLocalRefactoring createExtractLocalRefactoring(AssistContext context)
      throws Exception {
    return new ExtractLocalRefactoringImpl(context);
  }

  /**
   * @return the {@link ExtractMethodRefactoring} to extract {@link Expression} selected in given
   *         {@link AssistContext}.
   */
  public static ExtractMethodRefactoring createExtractMethodRefactoring(AssistContext context)
      throws Exception {
    return new ExtractMethodRefactoringImpl(context);
  }

  /**
   * @return the {@link Refactoring} to inline local {@link VariableDeclaration}.
   */
  public static InlineLocalRefactoring createInlineLocalRefactoring(AssistContext context)
      throws Exception {
    return new InlineLocalRefactoringImpl(context);
  }

  /**
   * @return the {@link Refactoring} to inline {@link MethodElement} or {@link FunctionElement}.
   */
  public static InlineMethodRefactoring createInlineMethodRefactoring(AssistContext context)
      throws Exception {
    return new InlineMethodRefactoringImpl(context);
  }

  /**
   * @return the {@link RenameRefactoring} instance to perform {@link Element} rename to the given
   *         name, may be <code>null</code> if there are no support for renaming given
   *         {@link Element}.
   */
  public static RenameRefactoring createRenameRefactoring(SearchEngine searchEngine, Element element) {
    Preconditions.checkNotNull(searchEngine);
    Preconditions.checkNotNull(element);
    if (element instanceof AngularFilterElement) {
      AngularFilterElement filter = (AngularFilterElement) element;
      return new RenameAngularFilterRefactoringImpl(searchEngine, filter);
    }
    if (element instanceof AngularComponentElement) {
      AngularComponentElement component = (AngularComponentElement) element;
      return new RenameAngularComponentRefactoringImpl(searchEngine, component);
    }
    if (element instanceof AngularControllerElement) {
      AngularControllerElement controller = (AngularControllerElement) element;
      return new RenameAngularControllerRefactoringImpl(searchEngine, controller);
    }
    if (element instanceof AngularPropertyElement) {
      AngularPropertyElement property = (AngularPropertyElement) element;
      Element selector = RenameAngularHasAttributeSelectorRefactoringImpl.getAttributeSelectorElement(property);
      if (selector != null) {
        element = selector;
      } else {
        return new RenameAngularPropertyRefactoringImpl(searchEngine, property);
      }
    }
    if (element instanceof AngularScopePropertyElement) {
      AngularScopePropertyElement property = (AngularScopePropertyElement) element;
      return new RenameAngularScopePropertyRefactoringImpl(searchEngine, property);
    }
    if (element instanceof AngularTagSelectorElement) {
      AngularTagSelectorElement selector = (AngularTagSelectorElement) element;
      return new RenameAngularTagSelectorRefactoringImpl(searchEngine, selector);
    }
    if (element instanceof AngularHasAttributeSelectorElement) {
      AngularHasAttributeSelectorElement selector = (AngularHasAttributeSelectorElement) element;
      return new RenameAngularHasAttributeSelectorRefactoringImpl(searchEngine, selector);
    }
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof FieldFormalParameterElement) {
      element = ((FieldFormalParameterElement) element).getField();
    }
    if (element instanceof LibraryElement) {
      return new RenameLibraryRefactoringImpl(searchEngine, (LibraryElement) element);
    }
    if (element instanceof ConstructorElement) {
      return new RenameConstructorRefactoringImpl(searchEngine, (ConstructorElement) element);
    }
    if (element.getEnclosingElement() instanceof ExecutableElement) {
      if (element instanceof LocalElement) {
        return new RenameLocalRefactoringImpl(searchEngine, (LocalElement) element);
      }
    }
    if (element instanceof ImportElement) {
      return new RenameImportRefactoringImpl(searchEngine, (ImportElement) element);
    }
    if (element.getEnclosingElement() instanceof LibraryElement
        || element.getEnclosingElement() instanceof CompilationUnitElement) {
      return new RenameUnitMemberRefactoringImpl(searchEngine, element);
    }
    if (element.getEnclosingElement() instanceof ClassElement) {
      return new RenameClassMemberRefactoringImpl(searchEngine, element);
    }
    return null;
  }
}
