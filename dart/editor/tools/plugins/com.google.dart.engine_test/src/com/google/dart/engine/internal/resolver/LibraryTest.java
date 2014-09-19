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
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
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

  @Override
  public void setUp() {
    sourceFactory = new SourceFactory(new FileUriResolver());
    analysisContext = new AnalysisContextImpl();
    analysisContext.setSourceFactory(sourceFactory);
    errorListener = new GatheringErrorListener();
    library = createLibrary("/lib.dart");
  }

  /*
   * In order to keep the tests fast there are no tests for getAST(Source),
   * getCompilationUnitSources(), getDefiningCompilationUnit()
   */

  public void test_getExplicitlyImportsCore() {
    assertFalse(library.getExplicitlyImportsCore());
    errorListener.assertNoErrors();
  }

  public void test_getExports() {
    assertLength(0, library.getExports());
    errorListener.assertNoErrors();
  }

  public void test_getImports() {
    assertLength(0, library.getImports());
    errorListener.assertNoErrors();
  }

  public void test_getImportsAndExports() {
    library.setImportedLibraries(new Library[] {createLibrary("/imported.dart")});
    library.setExportedLibraries(new Library[] {createLibrary("/exported.dart")});
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

  public void test_setExportedLibraries() {
    Library exportLibrary = createLibrary("/exported.dart");
    library.setExportedLibraries(new Library[] {exportLibrary});
    Library[] exports = library.getExports();
    assertLength(1, exports);
    assertSame(exportLibrary, exports[0]);
    errorListener.assertNoErrors();
  }

  public void test_setImportedLibraries() {
    Library importLibrary = createLibrary("/imported.dart");
    library.setImportedLibraries(new Library[] {importLibrary});
    Library[] imports = library.getImports();
    assertLength(1, imports);
    assertSame(importLibrary, imports[0]);
    errorListener.assertNoErrors();
  }

  public void test_setLibraryElement() {
    LibraryElementImpl element = new LibraryElementImpl(analysisContext, libraryIdentifier("lib"));
    library.setLibraryElement(element);
    assertSame(element, library.getLibraryElement());
  }

  @Override
  protected void tearDown() throws Exception {
    errorListener = null;
    sourceFactory = null;
    analysisContext = null;
    library = null;
    super.tearDown();
  }

  private Library createLibrary(String definingCompilationUnitPath) {
    return new Library(analysisContext, errorListener, new FileBasedSource(
        createFile(definingCompilationUnitPath)));
  }
}
