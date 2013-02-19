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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.internal.refactoring.RenameClassMemberRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameConstructorRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameUnitMemberRefactoringImpl;

/**
 * Factory for creating {@link Refactoring} instances.
 */
public class RefactoringFactory {
  /**
   * @return the {@link RenameRefactoring} instance to perform {@link Element} rename to the given
   *         name, may be <code>null</code> if there are no support for renaming given
   *         {@link Element}.
   */
  public static RenameRefactoring createRenameRefactoring(SearchEngine searchEngine, Element element) {
    Preconditions.checkNotNull(searchEngine);
    Preconditions.checkNotNull(element);
    if (element instanceof ConstructorElement) {
      return new RenameConstructorRefactoringImpl(searchEngine, (ConstructorElement) element);
    }
    if (element.getEnclosingElement() instanceof ExecutableElement) {
      if (element instanceof LocalElement) {
        return new RenameLocalRefactoringImpl(searchEngine, (LocalElement) element);
      }
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
