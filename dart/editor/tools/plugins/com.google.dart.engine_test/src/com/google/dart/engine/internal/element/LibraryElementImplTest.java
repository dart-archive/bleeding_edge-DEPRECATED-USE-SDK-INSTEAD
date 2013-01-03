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
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.source.FileBasedSource;

import static com.google.dart.engine.ast.ASTFactory.identifier;

import java.io.File;

public class LibraryElementImplTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new LibraryElementImpl(new AnalysisContextImpl(), identifier("l")));
  }

  public void test_getImportedLibraries() {
    AnalysisContext context = new AnalysisContextImpl();
    LibraryElementImpl library1 = createLibrary(context, "l1");
    LibraryElementImpl library2 = createLibrary(context, "l2");
    LibraryElementImpl library3 = createLibrary(context, "l3");
    LibraryElementImpl library4 = createLibrary(context, "l4");
    PrefixElement prefixA = new PrefixElementImpl(identifier("a"));
    PrefixElement prefixB = new PrefixElementImpl(identifier("b"));
    ImportSpecificationImpl[] imports = {
        createImport(library2, null), createImport(library2, prefixB),
        createImport(library3, null), createImport(library3, prefixA),
        createImport(library3, prefixB), createImport(library4, prefixA),};
    library1.setImports(imports);
    LibraryElement[] libraries = library1.getImportedLibraries();
    assertEqualsIgnoreOrder(new LibraryElement[] {library2, library3, library4}, libraries);
  }

  public void test_getPrefixes() {
    AnalysisContext context = new AnalysisContextImpl();
    LibraryElementImpl library = createLibrary(context, "l1");
    PrefixElement prefixA = new PrefixElementImpl(identifier("a"));
    PrefixElement prefixB = new PrefixElementImpl(identifier("b"));
    ImportSpecificationImpl[] imports = {
        createImport(createLibrary(context, "l2"), null),
        createImport(createLibrary(context, "l3"), null),
        createImport(createLibrary(context, "l4"), prefixA),
        createImport(createLibrary(context, "l5"), prefixA),
        createImport(createLibrary(context, "l6"), prefixB),};
    library.setImports(imports);
    PrefixElement[] prefixes = library.getPrefixes();
    assertLength(2, prefixes);
    if (prefixA == prefixes[0]) {
      assertEquals(prefixB, prefixes[1]);
    } else {
      assertEquals(prefixB, prefixes[0]);
      assertEquals(prefixA, prefixes[1]);
    }
  }

  public void test_setImports() {
    AnalysisContext context = new AnalysisContextImpl();
    LibraryElementImpl library = new LibraryElementImpl(context, identifier("l1"));
    ImportSpecificationImpl[] expectedImports = {
        createImport(createLibrary(context, "l2"), null),
        createImport(createLibrary(context, "l3"), null)};
    library.setImports(expectedImports);
    ImportSpecification[] actualImports = library.getImports();
    assertLength(expectedImports.length, actualImports);
    for (int i = 0; i < actualImports.length; i++) {
      assertEquals(expectedImports[i], actualImports[i]);
    }
  }

  private ImportSpecificationImpl createImport(LibraryElement importedLibrary, PrefixElement prefix) {
    ImportSpecificationImpl spec = new ImportSpecificationImpl();
    spec.setImportedLibrary(importedLibrary);
    spec.setPrefix(prefix);
    return spec;
  }

  private LibraryElementImpl createLibrary(AnalysisContext context, String libraryName) {
    String fileName = libraryName + ".dart";
    FileBasedSource source = new FileBasedSource(null, new File(fileName));
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl(fileName);
    unit.setSource(source);
    LibraryElementImpl library = new LibraryElementImpl(context, identifier(libraryName));
    library.setDefiningCompilationUnit(unit);
    return library;
  }
}
