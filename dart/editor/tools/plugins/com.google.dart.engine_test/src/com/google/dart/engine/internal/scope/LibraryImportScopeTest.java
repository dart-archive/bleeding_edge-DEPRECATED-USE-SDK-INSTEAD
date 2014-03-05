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

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.importFor;
import static com.google.dart.engine.element.ElementFactory.prefix;

public class LibraryImportScopeTest extends ResolverTestCase {
  public void test_conflictingImports() {
    AnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    String typeNameA = "A";
    String typeNameB = "B";
    String typeNameC = "C";
    ClassElement typeA = classElement(typeNameA);
    ClassElement typeB1 = classElement(typeNameB);
    ClassElement typeB2 = classElement(typeNameB);
    ClassElement typeC = classElement(typeNameC);

    LibraryElement importedLibrary1 = createTestLibrary(context, "imported1");
    ((CompilationUnitElementImpl) importedLibrary1.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        typeA, typeB1});
    ImportElementImpl import1 = importFor(importedLibrary1, null);

    LibraryElement importedLibrary2 = createTestLibrary(context, "imported2");
    ((CompilationUnitElementImpl) importedLibrary2.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        typeB2, typeC});
    ImportElementImpl import2 = importFor(importedLibrary2, null);

    LibraryElementImpl importingLibrary = createTestLibrary(context, "importing");
    importingLibrary.setImports(new ImportElement[] {import1, import2});
    {
      GatheringErrorListener errorListener = new GatheringErrorListener();
      Scope scope = new LibraryImportScope(importingLibrary, errorListener);

      assertEquals(typeA, scope.lookup(identifier(typeNameA), importingLibrary));
      errorListener.assertNoErrors();

      assertEquals(typeC, scope.lookup(identifier(typeNameC), importingLibrary));
      errorListener.assertNoErrors();

      Element element = scope.lookup(identifier(typeNameB), importingLibrary);
      errorListener.assertErrorsWithCodes(StaticWarningCode.AMBIGUOUS_IMPORT);
      assertInstanceOf(MultiplyDefinedElement.class, element);

      Element[] conflictingElements = ((MultiplyDefinedElement) element).getConflictingElements();
      assertLength(2, conflictingElements);
      if (conflictingElements[0] == typeB1) {
        assertSame(typeB2, conflictingElements[1]);
      } else if (conflictingElements[0] == typeB2) {
        assertSame(typeB1, conflictingElements[1]);
      } else {
        assertSame(typeB1, conflictingElements[0]);
      }
    }

    {
      GatheringErrorListener errorListener = new GatheringErrorListener();
      Scope scope = new LibraryImportScope(importingLibrary, errorListener);

      Identifier identifier = identifier(typeNameB);
      methodDeclaration(null, typeName(identifier), null, null, identifier("foo"), null);
      Element element = scope.lookup(identifier, importingLibrary);
      errorListener.assertErrorsWithCodes(StaticWarningCode.AMBIGUOUS_IMPORT);
      assertInstanceOf(MultiplyDefinedElement.class, element);
    }
  }

  public void test_creation_empty() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    new LibraryImportScope(definingLibrary, errorListener);
  }

  public void test_creation_nonEmpty() {
    AnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    String importedTypeName = "A";
    ClassElement importedType = new ClassElementImpl(identifier(importedTypeName));
    LibraryElement importedLibrary = createTestLibrary(context, "imported");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {importedType});
    LibraryElementImpl definingLibrary = createTestLibrary(context, "importing");
    ImportElementImpl importElement = new ImportElementImpl(0);
    importElement.setImportedLibrary(importedLibrary);
    definingLibrary.setImports(new ImportElement[] {importElement});
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(importedType, scope.lookup(identifier(importedTypeName), definingLibrary));
  }

  public void test_getErrorListener() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    LibraryImportScope scope = new LibraryImportScope(definingLibrary, errorListener);
    assertEquals(errorListener, scope.getErrorListener());
  }

  public void test_nonConflictingImports_fromSdk() {
    AnalysisContext context = AnalysisContextFactory.contextWithCore();
    String typeName = "List";
    ClassElement type = classElement(typeName);

    LibraryElement importedLibrary = createTestLibrary(context, "lib");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {type});
    ImportElementImpl importCore = importFor(
        context.getLibraryElement(context.getSourceFactory().forUri("dart:core")),
        null);
    ImportElementImpl importLib = importFor(importedLibrary, null);

    LibraryElementImpl importingLibrary = createTestLibrary(context, "importing");
    importingLibrary.setImports(new ImportElement[] {importCore, importLib});

    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);

    assertEquals(type, scope.lookup(identifier(typeName), importingLibrary));
    errorListener.assertErrorsWithCodes(StaticWarningCode.CONFLICTING_DART_IMPORT);
  }

  public void test_nonConflictingImports_sameElement() {
    AnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    String typeNameA = "A";
    String typeNameB = "B";
    ClassElement typeA = classElement(typeNameA);
    ClassElement typeB = classElement(typeNameB);

    LibraryElement importedLibrary = createTestLibrary(context, "imported");
    ((CompilationUnitElementImpl) importedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        typeA, typeB});
    ImportElementImpl import1 = importFor(importedLibrary, null);
    ImportElementImpl import2 = importFor(importedLibrary, null);

    LibraryElementImpl importingLibrary = createTestLibrary(context, "importing");
    importingLibrary.setImports(new ImportElement[] {import1, import2});

    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);

    assertEquals(typeA, scope.lookup(identifier(typeNameA), importingLibrary));
    errorListener.assertNoErrors();

    assertEquals(typeB, scope.lookup(identifier(typeNameB), importingLibrary));
    errorListener.assertNoErrors();
  }

  public void test_prefixedAndNonPrefixed() {
    AnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    String typeName = "C";
    String prefixName = "p";
    ClassElement prefixedType = classElement(typeName);
    ClassElement nonPrefixedType = classElement(typeName);

    LibraryElement prefixedLibrary = createTestLibrary(context, "import.prefixed");
    ((CompilationUnitElementImpl) prefixedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {prefixedType});
    ImportElementImpl prefixedImport = importFor(prefixedLibrary, prefix(prefixName));

    LibraryElement nonPrefixedLibrary = createTestLibrary(context, "import.nonPrefixed");
    ((CompilationUnitElementImpl) nonPrefixedLibrary.getDefiningCompilationUnit()).setTypes(new ClassElement[] {nonPrefixedType});
    ImportElementImpl nonPrefixedImport = importFor(nonPrefixedLibrary, null);

    LibraryElementImpl importingLibrary = createTestLibrary(context, "importing");
    importingLibrary.setImports(new ImportElement[] {prefixedImport, nonPrefixedImport});

    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new LibraryImportScope(importingLibrary, errorListener);

    Element prefixedElement = scope.lookup(identifier(prefixName, typeName), importingLibrary);
    errorListener.assertNoErrors();
    assertSame(prefixedType, prefixedElement);

    Element nonPrefixedElement = scope.lookup(identifier(typeName), importingLibrary);
    errorListener.assertNoErrors();
    assertSame(nonPrefixedType, nonPrefixedElement);
  }
}
