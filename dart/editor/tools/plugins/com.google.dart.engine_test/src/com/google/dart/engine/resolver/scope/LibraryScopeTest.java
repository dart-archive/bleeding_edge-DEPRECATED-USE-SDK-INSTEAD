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
package com.google.dart.engine.resolver.scope;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class LibraryScopeTest extends ResolverTestCase {
  public void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryScope(definingLibrary, errorListener);
  }

  public void test_creation_nonEmpty() {
    AnalysisContext context = new AnalysisContextImpl();
    String importedTypeName = "A";
    ClassElement importedType = new ClassElementImpl(identifier(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary(context, "imported");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {importedType});
    LibraryElementImpl definingLibrary = createTestLibrary(context, "importing");
    ImportElementImpl importElement = new ImportElementImpl();
    importElement.setImportedLibrary(importedLibrary);
    definingLibrary.setImports(new ImportElement[] {importElement});
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryScope(definingLibrary, errorListener);
    assertEquals(importedType, scope.lookup(importedTypeName, definingLibrary));
  }

  public void test_getDefiningLibrary() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryScope(definingLibrary, errorListener);
    assertEquals(definingLibrary, scope.getDefiningLibrary());
  }

  public void test_getErrorListener() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryScope(definingLibrary, errorListener);
    assertEquals(errorListener, scope.getErrorListener());
  }
}
