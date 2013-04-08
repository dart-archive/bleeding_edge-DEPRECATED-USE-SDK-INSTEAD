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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.ASTFactory.exportDirective;
import static com.google.dart.engine.ast.ASTFactory.importDirective;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class LibraryTest extends EngineTestCase {
  /**
   * The error listener to which all errors will be reported.
   */
  private GatheringErrorListener errorListener;

  /**
   * The source factory used to create libraries.
   */
  private SourceFactory sourceFactory;

  /**
   * The analysis context to pass in to all libraries created by the tests.
   */
  private AnalysisContextImpl analysisContext;

  /**
   * The library used by the tests.
   */
  private Library library;

  /*
   * In order to keep the tests fast there are no tests for getAST(Source),
   * getCompilationUnitSources(), getDefiningCompilationUnit()
   */

  @Override
  public void setUp() {
    sourceFactory = new SourceFactory(new FileUriResolver());
    analysisContext = new AnalysisContextImpl();
    analysisContext.setSourceFactory(sourceFactory);
    errorListener = new GatheringErrorListener();
    library = library("/lib.dart");
  }

  public void test_addExport() {
    Library exportLibrary = library("/exported.dart");
    library.addExport(exportDirective("exported.dart"), exportLibrary);
    Library[] exports = library.getExports();
    assertLength(1, exports);
    assertSame(exportLibrary, exports[0]);
    errorListener.assertNoErrors();
  }

  public void test_addImport() {
    Library importLibrary = library("/imported.dart");
    library.addImport(importDirective("imported.dart", null), importLibrary);
    Library[] imports = library.getImports();
    assertLength(1, imports);
    assertSame(importLibrary, imports[0]);
    errorListener.assertNoErrors();
  }

  public void test_getExplicitlyImportsCore() {
    assertFalse(library.getExplicitlyImportsCore());
    errorListener.assertNoErrors();
  }

  public void test_getExport() {
    ExportDirective directive = exportDirective("exported.dart");
    Library exportLibrary = library("/exported.dart");
    library.addExport(directive, exportLibrary);
    assertSame(exportLibrary, library.getExport(directive));
    errorListener.assertNoErrors();
  }

  public void test_getExports() {
    assertLength(0, library.getExports());
    errorListener.assertNoErrors();
  }

  public void test_getImport() {
    ImportDirective directive = importDirective("imported.dart", null);
    Library importLibrary = library("/imported.dart");
    library.addImport(directive, importLibrary);
    assertSame(importLibrary, library.getImport(directive));
    errorListener.assertNoErrors();
  }

  public void test_getImports() {
    assertLength(0, library.getImports());
    errorListener.assertNoErrors();
  }

  public void test_getImportsAndExports() {
    library.addImport(importDirective("imported.dart", null), library("/imported.dart"));
    library.addExport(exportDirective("exported.dart"), library("/exported.dart"));
    assertLength(2, library.getImportsAndExports());
    errorListener.assertNoErrors();
  }

  public void test_getLibraryScope() {
    LibraryElementImpl element = new LibraryElementImpl(analysisContext, libraryIdentifier("lib"));
    element.setDefiningCompilationUnit(new CompilationUnitElementImpl("lib.dart"));
    library.setLibraryElement(element);
    assertNotNull(library.getLibraryScope());
    errorListener.assertNoErrors();
  }

  public void test_getLibrarySource() {
    assertNotNull(library.getLibrarySource());
  }

  public void test_setExplicitlyImportsCore() {
    library.setExplicitlyImportsCore(true);
    assertTrue(library.getExplicitlyImportsCore());
    errorListener.assertNoErrors();
  }

  public void test_setLibraryElement() {
    LibraryElementImpl element = new LibraryElementImpl(analysisContext, libraryIdentifier("lib"));
    library.setLibraryElement(element);
    assertSame(element, library.getLibraryElement());
  }

  private Library library(String definingCompilationUnitPath) {
    return new Library(analysisContext, errorListener, new FileBasedSource(
        sourceFactory.getContentCache(),
        createFile(definingCompilationUnitPath)));
  }
}
