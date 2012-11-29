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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ImportSpecificationImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class LibraryImportScopeTest extends ResolverTestCase {
  public void test_conflictingImports() {
    String typeNameA = "A";
    String typeNameB = "B";
    String typeNameC = "C";
    ClassElement typeA = new ClassElementImpl(identifier(typeNameA));
    ClassElement typeB1 = new ClassElementImpl(identifier(typeNameB));
    ClassElement typeB2 = new ClassElementImpl(identifier(typeNameB));
    ClassElement typeC = new ClassElementImpl(identifier(typeNameC));

    LibraryElement importedLibrary1 = createTestLibrary("imported1");
    ((CompilationUnitElementImpl) importedLibrary1.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        typeA, typeB1});
    ImportSpecificationImpl specification1 = new ImportSpecificationImpl();
    specification1.setImportedLibrary(importedLibrary1);

    LibraryElement importedLibrary2 = createTestLibrary("imported2");
    ((CompilationUnitElementImpl) importedLibrary2.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        typeB2, typeC});
    ImportSpecificationImpl specification2 = new ImportSpecificationImpl();
    specification2.setImportedLibrary(importedLibrary2);

    LibraryElementImpl importingLibrary = createTestLibrary("importing");
    importingLibrary.setImports(new ImportSpecification[] {specification1, specification2});
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);
    assertEquals(typeA, scope.lookup(typeNameA, importingLibrary));
    errorListener.assertNoErrors();
    assertEquals(typeC, scope.lookup(typeNameC, importingLibrary));
    errorListener.assertNoErrors();
    Element element = scope.lookup(typeNameB, importingLibrary);
    errorListener.assertNoErrors();
    assertInstanceOf(MultiplyDefinedElement.class, element);
    Element[] conflictingElements = ((MultiplyDefinedElement) element).getConflictingElements();
    assertEquals(typeB1, conflictingElements[0]);
    assertEquals(typeB2, conflictingElements[1]);
    assertEquals(2, conflictingElements.length);
  }

  public void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryImportScope(definingLibrary, errorListener);
  }

  public void test_creation_nonEmpty() {
    String importedTypeName = "A";
    ClassElement importedType = new ClassElementImpl(identifier(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary("imported");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {importedType});
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
    assertEquals(definingLibrary, scope.getDefiningLibrary());
  }

  public void test_getErrorListener() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(errorListener, scope.getErrorListener());
  }
}
