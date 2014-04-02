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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementFactory;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.element.ElementFactory.compilationUnit;
import static com.google.dart.engine.element.ElementFactory.exportFor;
import static com.google.dart.engine.element.ElementFactory.importFor;
import static com.google.dart.engine.element.ElementFactory.library;

public class LibraryElementImplTest extends EngineTestCase {

  public void test_creation() {
    assertNotNull(new LibraryElementImpl(createAnalysisContext(), libraryIdentifier("l")));
  }

  public void test_getImportedLibraries() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library1 = library(context, "l1");
    LibraryElementImpl library2 = library(context, "l2");
    LibraryElementImpl library3 = library(context, "l3");
    LibraryElementImpl library4 = library(context, "l4");
    PrefixElement prefixA = new PrefixElementImpl(identifier("a"));
    PrefixElement prefixB = new PrefixElementImpl(identifier("b"));
    ImportElementImpl[] imports = {
        importFor(library2, null), importFor(library2, prefixB), importFor(library3, null),
        importFor(library3, prefixA), importFor(library3, prefixB), importFor(library4, prefixA),};
    library1.setImports(imports);
    LibraryElement[] libraries = library1.getImportedLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library2, library3, library4}, libraries);
  }

  public void test_getPrefixes() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "l1");
    PrefixElement prefixA = new PrefixElementImpl(identifier("a"));
    PrefixElement prefixB = new PrefixElementImpl(identifier("b"));
    ImportElementImpl[] imports = {
        importFor(library(context, "l2"), null), importFor(library(context, "l3"), null),
        importFor(library(context, "l4"), prefixA), importFor(library(context, "l5"), prefixA),
        importFor(library(context, "l6"), prefixB),};
    library.setImports(imports);
    PrefixElement[] prefixes = library.getPrefixes();
    assertLength(2, prefixes);
    if (prefixA == prefixes[0]) {
      assertSame(prefixB, prefixes[1]);
    } else {
      assertSame(prefixB, prefixes[0]);
      assertSame(prefixA, prefixes[1]);
    }
  }

  public void test_getUnits() throws Exception {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "test");
    CompilationUnitElement unitLib = library.getDefiningCompilationUnit();
    CompilationUnitElementImpl unitA = compilationUnit("unit_a.dart");
    CompilationUnitElementImpl unitB = compilationUnit("unit_b.dart");
    library.setParts(new CompilationUnitElement[] {unitA, unitB});
    assertEqualsIgnoreOrder(
        new CompilationUnitElement[] {unitLib, unitA, unitB},
        library.getUnits());
  }

  public void test_getVisibleLibraries_cycle() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    LibraryElementImpl libraryA = library(context, "A");
    libraryA.setImports(new ImportElementImpl[] {importFor(library, null),});
    library.setImports(new ImportElementImpl[] {importFor(libraryA, null)});
    LibraryElement[] libraries = library.getVisibleLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library, libraryA}, libraries);
  }

  public void test_getVisibleLibraries_directExports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    LibraryElementImpl libraryA = library(context, "A");
    library.setExports(new ExportElementImpl[] {exportFor(libraryA),});
    LibraryElement[] libraries = library.getVisibleLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library}, libraries);
  }

  public void test_getVisibleLibraries_directImports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    LibraryElementImpl libraryA = library(context, "A");
    library.setImports(new ImportElementImpl[] {importFor(libraryA, null),});
    LibraryElement[] libraries = library.getVisibleLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library, libraryA}, libraries);
  }

  public void test_getVisibleLibraries_indirectExports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    LibraryElementImpl libraryA = library(context, "A");
    LibraryElementImpl libraryAA = library(context, "AA");
    libraryA.setExports(new ExportElementImpl[] {exportFor(libraryAA),});
    library.setImports(new ImportElementImpl[] {importFor(libraryA, null)});
    LibraryElement[] libraries = library.getVisibleLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library, libraryA, libraryAA}, libraries);
  }

  public void test_getVisibleLibraries_indirectImports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    LibraryElementImpl libraryA = library(context, "A");
    LibraryElementImpl libraryAA = library(context, "AA");
    LibraryElementImpl libraryB = library(context, "B");
    libraryA.setImports(new ImportElementImpl[] {importFor(libraryAA, null),});
    library.setImports(new ImportElementImpl[] {
        importFor(libraryA, null), importFor(libraryB, null),});
    LibraryElement[] libraries = library.getVisibleLibraries();
    assertEqualsIgnoreOrder(
        new LibraryElement[] {library, libraryA, libraryAA, libraryB},
        libraries);
  }

  public void test_getVisibleLibraries_noImports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = library(context, "app");
    assertEqualsIgnoreOrder(new LibraryElement[] {library}, library.getVisibleLibraries());
  }

  public void test_isUpToDate() {
    AnalysisContext context = createAnalysisContext();
    context.setSourceFactory(new SourceFactory());
    LibraryElement library = ElementFactory.library(context, "foo");

    context.setContents(library.getDefiningCompilationUnit().getSource(), "sdfsdff");

    // Assert that we are not up to date if the target has an old time stamp.
    assertFalse(library.isUpToDate(0));

    // Assert that we are up to date with a target modification time in the future.
    assertTrue(library.isUpToDate(System.currentTimeMillis() + 1000));
  }

  public void test_setImports() {
    AnalysisContext context = createAnalysisContext();
    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier("l1"));
    ImportElementImpl[] expectedImports = {
        importFor(library(context, "l2"), null), importFor(library(context, "l3"), null)};
    library.setImports(expectedImports);
    ImportElement[] actualImports = library.getImports();
    assertLength(expectedImports.length, actualImports);
    for (int i = 0; i < actualImports.length; i++) {
      assertSame(expectedImports[i], actualImports[i]);
    }
  }
}
