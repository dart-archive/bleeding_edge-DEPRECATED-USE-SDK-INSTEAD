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

import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ImportSpecificationImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.TypeElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class LibraryImportScopeTest extends ResolverTestCase {
  public void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryImportScope(definingLibrary, errorListener);
  }

  public void test_creation_nonEmpty() {
    String importedTypeName = "A";
    TypeElement importedType = new TypeElementImpl(identifier(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary("imported");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new TypeElement[] {importedType});
    LibraryElementImpl definingLibrary = createTestLibrary("importing");
    ImportSpecificationImpl specification = new ImportSpecificationImpl();
    specification.setImportedLibrary(importedLibrary);
    definingLibrary.setImports(new ImportSpecification[] {specification});
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(importedType, scope.lookup(importedTypeName, definingLibrary));
  }

  public void test_getDefiningLibrary() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(definingLibrary, invokeMethod(scope, "getDefiningLibrary"));
  }

  public void test_getErrorListener() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(errorListener, invokeMethod(scope, "getErrorListener"));
  }
}
