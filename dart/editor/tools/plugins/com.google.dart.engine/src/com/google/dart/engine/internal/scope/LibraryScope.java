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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;

/**
 * Instances of the class {@code LibraryScope} implement a scope containing all of the names defined
 * in a given library.
 * 
 * @coverage dart.engine.resolver
 */
public class LibraryScope extends EnclosedScope {
  /**
   * Initialize a newly created scope representing the names defined in the given library.
   * 
   * @param definingLibrary the element representing the library represented by this scope
   * @param errorListener the listener that is to be informed when an error is encountered
   */
  public LibraryScope(LibraryElement definingLibrary, AnalysisErrorListener errorListener) {
    super(new LibraryImportScope(definingLibrary, errorListener));
    defineTopLevelNames(definingLibrary);
  }

  @Override
  protected AnalysisError getErrorForDuplicate(Element existing, Element duplicate) {
    if (existing instanceof PrefixElement) {
      // TODO(scheglov) consider providing actual 'nameOffset' from the synthetic accessor
      int offset = duplicate.getNameOffset();
      if (duplicate instanceof PropertyAccessorElement) {
        PropertyAccessorElement accessor = (PropertyAccessorElement) duplicate;
        if (accessor.isSynthetic()) {
          offset = accessor.getVariable().getNameOffset();
        }
      }
      return new AnalysisError(
          duplicate.getSource(),
          offset,
          duplicate.getDisplayName().length(),
          CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER,
          existing.getDisplayName());
    }
    return super.getErrorForDuplicate(existing, duplicate);
  }

  /**
   * Add to this scope all of the public top-level names that are defined in the given compilation
   * unit.
   * 
   * @param compilationUnit the compilation unit defining the top-level names to be added to this
   *          scope
   */
  private void defineLocalNames(CompilationUnitElement compilationUnit) {
    for (PropertyAccessorElement element : compilationUnit.getAccessors()) {
      define(element);
    }
    for (ClassElement element : compilationUnit.getEnums()) {
      define(element);
    }
    for (FunctionElement element : compilationUnit.getFunctions()) {
      define(element);
    }
    for (FunctionTypeAliasElement element : compilationUnit.getFunctionTypeAliases()) {
      define(element);
    }
    for (ClassElement element : compilationUnit.getTypes()) {
      define(element);
    }
  }

  /**
   * Add to this scope all of the names that are explicitly defined in the given library.
   * 
   * @param definingLibrary the element representing the library that defines the names in this
   *          scope
   */
  private final void defineTopLevelNames(LibraryElement definingLibrary) {
    for (PrefixElement prefix : definingLibrary.getPrefixes()) {
      define(prefix);
    }
    defineLocalNames(definingLibrary.getDefiningCompilationUnit());
    for (CompilationUnitElement compilationUnit : definingLibrary.getParts()) {
      defineLocalNames(compilationUnit);
    }
  }
}
