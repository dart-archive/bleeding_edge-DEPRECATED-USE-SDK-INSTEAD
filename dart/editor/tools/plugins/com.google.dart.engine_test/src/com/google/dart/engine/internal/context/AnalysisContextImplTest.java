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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.IOException;

public class AnalysisContextImplTest extends EngineTestCase {
  /**
   * An analysis context whose source factory is {@link #sourceFactory}.
   */
  private AnalysisContextImpl context;

  /**
   * The source factory associated with the analysis {@link #context}.
   */
  private SourceFactory sourceFactory;

  public void fail_extractContext() {
    fail("Implement this");
  }

  public void fail_getLaunchableClientLibrarySources() throws Exception {
    // Fails because we are not correctly determining whether a source is part of the SDK
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source[] sources = context.getLaunchableClientLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", createSource("import 'dart:html';", "main() {}"));
    context.computeLibraryElement(source);
    sources = context.getLaunchableClientLibrarySources();
    assertLength(1, sources);
  }

  public void fail_getLibrariesContaining() throws Exception {
    // Fails because we're not currently caching this information
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource("library lib;", "part 'part.dart';"));
    Source partSource = addSource("/part.dart", "part of lib;");
    context.computeLibraryElement(librarySource);
    Source[] result = context.getLibrariesContaining(partSource);
    assertLength(1, result);
    assertEquals(librarySource, result[0]);
  }

  public void fail_mergeContext() {
    fail("Implement this");
  }

  @Override
  public void setUp() {
    context = new AnalysisContextImpl();
    sourceFactory = new SourceFactory(new FileUriResolver());
    context.setSourceFactory(sourceFactory);
  }

  public void test_applyChanges_add() {
    Source source = addSource("/test.dart", "");
    sourceFactory.setContents(source, "main() {}");
    ChangeSet changeSet = new ChangeSet();
    changeSet.changed(source);
    context.applyChanges(changeSet);
  }

  public void test_applyChanges_empty() {
    context.applyChanges(new ChangeSet());
    assertNull(context.performAnalysisTask());
  }

  public void test_computeErrors_none() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    AnalysisError[] errors = context.computeErrors(source);
    assertLength(0, errors);
  }

  public void test_computeErrors_some() throws Exception {
    Source source = addSource("/lib.dart", "library 'lib';");
    AnalysisError[] errors = context.computeErrors(source);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_computeHtmlElement() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    HtmlElement element = context.computeHtmlElement(source);
    assertNotNull(element);
  }

  public void test_computeKindOf_html() {
    Source source = addSource("/test.html", "");
    assertSame(SourceKind.HTML, context.computeKindOf(source));
  }

  public void test_computeKindOf_library() {
    Source source = addSource("/test.dart", "library lib;");
    assertSame(SourceKind.LIBRARY, context.computeKindOf(source));
  }

  public void test_computeKindOf_part() {
    Source source = addSource("/test.dart", "part of lib;");
    assertSame(SourceKind.PART, context.computeKindOf(source));
  }

  public void test_computeKindOf_unknown() {
    Source source = addSource("/test.css", "");
    assertSame(SourceKind.UNKNOWN, context.computeKindOf(source));
  }

  public void test_computeLibraryElement() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "library lib;");
    LibraryElement element = context.computeLibraryElement(source);
    assertNotNull(element);
  }

  public void test_computeLineInfo_dart() throws Exception {
    Source source = addSource("/test.dart", createSource("library lib;", "", "main() {}"));
    LineInfo info = context.computeLineInfo(source);
    assertNotNull(info);
  }

  public void test_computeLineInfo_html() throws Exception {
    Source source = addSource(
        "/test.html",
        createSource("<html>", "  <body>", "    <h1>A</h1>", "  </body>", "</html>"));
    LineInfo info = context.computeLineInfo(source);
    assertNotNull(info);
  }

  public void test_getElement() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    LibraryElement core = context.computeLibraryElement(sourceFactory.fromEncoding("dart:core"));
    assertNotNull(core);
    ClassElement classObject = findClass(core.getDefiningCompilationUnit(), "Object");
    assertNotNull(classObject);
    ElementLocation location = classObject.getLocation();
    Element element = context.getElement(location);
    assertSame(classObject, element);
  }

  public void test_getErrors_none() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    AnalysisError[] errors = context.getErrors(source);
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source);
    assertLength(0, errors);
  }

  public void test_getErrors_some() throws Exception {
    Source source = addSource("/lib.dart", "library 'lib';");
    AnalysisError[] errors = context.getErrors(source);
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source);
    assertLength(1, errors);
  }

  public void test_getHtmlElement() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    HtmlElement element = context.getHtmlElement(source);
    assertNull(element);
    context.computeHtmlElement(source);
    element = context.getHtmlElement(source);
    assertNotNull(element);
  }

  public void test_getHtmlSources() {
    Source[] sources = context.getHtmlSources();
    assertLength(0, sources);
    Source source = addSource("/test.html", "");
    context.computeKindOf(source);
    sources = context.getHtmlSources();
    assertLength(1, sources);
    assertEquals(source, sources[0]);
  }

  public void test_getKindOf_html() {
    Source source = addSource("/test.html", "");
    assertSame(SourceKind.HTML, context.getKindOf(source));
  }

  public void test_getKindOf_library() {
    Source source = addSource("/test.dart", "library lib;");
    assertSame(SourceKind.UNKNOWN, context.getKindOf(source));
    context.computeKindOf(source);
    assertSame(SourceKind.LIBRARY, context.getKindOf(source));
  }

  public void test_getKindOf_part() {
    Source source = addSource("/test.dart", "part of lib;");
    assertSame(SourceKind.UNKNOWN, context.getKindOf(source));
    context.computeKindOf(source);
    assertSame(SourceKind.PART, context.getKindOf(source));
  }

  public void test_getKindOf_unknown() {
    Source source = addSource("/test.css", "");
    assertSame(SourceKind.UNKNOWN, context.getKindOf(source));
  }

  public void test_getLaunchableServerLibrarySources() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source[] sources = context.getLaunchableServerLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", "main() {}");
    context.computeLibraryElement(source);
    sources = context.getLaunchableServerLibrarySources();
    assertLength(1, sources);
  }

  public void test_getLibraryElement() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "library lib;");
    LibraryElement element = context.getLibraryElement(source);
    assertNull(element);
    context.computeLibraryElement(source);
    element = context.getLibraryElement(source);
    assertNotNull(element);
  }

  public void test_getLibrarySources() {
    Source[] sources = context.getLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", "library lib;");
    context.computeKindOf(source);
    sources = context.getLibrarySources();
    assertLength(1, sources);
    assertEquals(source, sources[0]);
  }

  public void test_getLineInfo() throws Exception {
    Source source = addSource("/test.dart", createSource("library lib;", "", "main() {}"));
    LineInfo info = context.getLineInfo(source);
    assertNull(info);
    context.parseCompilationUnit(source);
    info = context.getLineInfo(source);
    assertNotNull(info);
  }

  public void test_getPublicNamespace_element() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "class A {}");
    LibraryElement library = context.computeLibraryElement(source);
    Namespace namespace = context.getPublicNamespace(library);
    assertNotNull(namespace);
    assertInstanceOf(ClassElement.class, namespace.get("A"));
  }

  public void test_getPublicNamespace_source() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "class A {}");
    context.computeLibraryElement(source);
    Namespace namespace = context.getPublicNamespace(source);
    assertNotNull(namespace);
    assertInstanceOf(ClassElement.class, namespace.get("A"));
  }

  public void test_getSourceFactory() {
    assertSame(sourceFactory, context.getSourceFactory());
  }

  public void test_parseCompilationUnit_errors() throws Exception {
    Source source = addSource("/lib.dart", "library {");
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertNotNull(compilationUnit);
    AnalysisError[] errors = context.getErrors(source);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_parseCompilationUnit_noErrors() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertNotNull(compilationUnit);
    assertLength(0, context.getErrors(source));
  }

  public void test_parseCompilationUnit_nonExistentSource() throws Exception {
    Source source = new FileBasedSource(sourceFactory, createFile("/test.dart"));
    CompilationUnit unit = context.parseCompilationUnit(source);
    assertNull(unit);
  }

  public void test_parseHtmlUnit_noErrors() throws Exception {
    Source source = addSource("/lib.html", "<html></html>");
    HtmlUnit unit = context.parseHtmlUnit(source);
    assertNotNull(unit);
  }

  public void test_performAnalysisTask_IOException() throws Exception {
    final boolean[] ioExceptionOccurred = new boolean[] {false};
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart")) {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        ioExceptionOccurred[0] = true;
        if (ioExceptionOccurred[0]) {
          throw new IOException("Some random I/O Exception");
        }
        super.getContents(receiver);
      }
    };
    sourceFactory.setContents(source, "library lib;");
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);

    // Simulate a typical analysis worker
    int count = 0;
    final int maxCount = 1000000;
    ChangeNotice[] notices = context.performAnalysisTask();
    while (notices != null && count < maxCount) {
      notices = context.performAnalysisTask();
      count++;
    }

    assertTrue(ioExceptionOccurred[0]);
    assertTrue(count < maxCount);
  }

  public void test_resolveCompilationUnit_library() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    LibraryElement library = context.computeLibraryElement(source);
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, library);
    assertNotNull(compilationUnit);
  }

  public void test_resolveCompilationUnit_source() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(compilationUnit);
  }

  public void test_resolveHtmlUnit() throws Exception {
    Source source = addSource("/lib.html", "<html></html>");
    HtmlUnit unit = context.resolveHtmlUnit(source);
    assertNotNull(unit);
  }

  public void test_setSourceFactory() {
    assertEquals(sourceFactory, context.getSourceFactory());
    SourceFactory factory = new SourceFactory();
    context.setSourceFactory(factory);
    assertEquals(factory, context.getSourceFactory());
  }

  private Source addSource(String fileName, String contents) {
    Source source = new FileBasedSource(sourceFactory, createFile(fileName));
    sourceFactory.setContents(source, contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    return source;
  }

  /**
   * Search the given compilation unit for a class with the given name. Return the class with the
   * given name, or {@code null} if the class cannot be found.
   * 
   * @param unit the compilation unit being searched
   * @param className the name of the class being searched for
   * @return the class with the given name
   */
  private ClassElement findClass(CompilationUnitElement unit, String className) {
    for (ClassElement classElement : unit.getTypes()) {
      if (classElement.getName().equals(className)) {
        return classElement;
      }
    }
    return null;
  }
}
