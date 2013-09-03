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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AnalysisContextImpl2Test extends EngineTestCase {
  /**
   * An analysis context whose source factory is {@link #sourceFactory}.
   */
  private AnalysisContextImpl2 context;

  /**
   * The source factory associated with the analysis {@link #context}.
   */
  private SourceFactory sourceFactory;

  public void fail_extractContext() {
    fail("Implement this");
  }

  public void fail_mergeContext() {
    fail("Implement this");
  }

  public void fail_recordLibraryElements() {
    fail("Implement this");
  }

  @Override
  public void setUp() {
    context = new AnalysisContextImpl2();
    sourceFactory = new SourceFactory(
        new DartUriResolver(DirectoryBasedDartSdk.getDefaultSdk()),
        new FileUriResolver());
    context.setSourceFactory(sourceFactory);
  }

  public void test_applyChanges_add() {
    Source source = addSource("/test.dart", "");
    sourceFactory.setContents(source, "main() {}");
    ChangeSet changeSet = new ChangeSet();
    changeSet.changed(source);
    context.applyChanges(changeSet);
  }

  public void test_applyChanges_change_flush_element() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "int a = 0;"));
    assertNotNull(context.computeLibraryElement(librarySource));

    context.setContents(librarySource, createSource(//
        "library lib;",
        "int aa = 0;"));
    assertNull(context.getLibraryElement(librarySource));
  }

  public void test_applyChanges_change_multiple() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "int a = 0;"));
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        "int b = a;"));
    context.computeLibraryElement(librarySource);

    context.setContents(librarySource, createSource(//
        "library lib;",
        "part 'part.dart';",
        "int aa = 0;"));
    context.setContents(partSource, createSource(//
        "part of lib;",
        "int b = aa;"));
    context.computeLibraryElement(librarySource);

    CompilationUnit libraryUnit = context.resolveCompilationUnit(librarySource, librarySource);
    CompilationUnit partUnit = context.resolveCompilationUnit(partSource, librarySource);
    TopLevelVariableDeclaration declaration = (TopLevelVariableDeclaration) libraryUnit.getDeclarations().get(
        0);
    Element declarationElement = declaration.getVariables().getVariables().get(0).getElement();
    TopLevelVariableDeclaration use = (TopLevelVariableDeclaration) partUnit.getDeclarations().get(
        0);
    Element useElement = ((SimpleIdentifier) use.getVariables().getVariables().get(0).getInitializer()).getStaticElement();
    assertSame(declarationElement, ((PropertyAccessorElement) useElement).getVariable());
  }

  public void test_applyChanges_empty() {
    context.applyChanges(new ChangeSet());
    assertNull(context.performAnalysisTask());
  }

  public void test_applyChanges_remove() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source libA = addSource("/libA.dart", createSource(//
        "library libA;",
        "import 'libB.dart';"));
    Source libB = addSource("/libB.dart", createSource(//
        "library libB;"));
    context.computeLibraryElement(libA);
    assertSize(0, context.getSourcesNeedingProcessing());

    ChangeSet changeSet = new ChangeSet();
    changeSet.removed(libB);
    context.applyChanges(changeSet);
    List<Source> sources = context.getSourcesNeedingProcessing();
    assertSize(1, sources);
    assertSame(libA, sources.get(0));
  }

  public void test_applyChanges_removeContainer() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source libA = addSource("/libA.dart", createSource(//
        "library libA;",
        "import 'libB.dart';"));
    final Source libB = addSource("/libB.dart", createSource(//
        "library libB;"));
    context.computeLibraryElement(libA);
    assertSize(0, context.getSourcesNeedingProcessing());

    ChangeSet changeSet = new ChangeSet();
    changeSet.removedContainer(new SourceContainer() {
      @Override
      public boolean contains(Source source) {
        return source.equals(libB);
      }
    });
    context.applyChanges(changeSet);
    List<Source> sources = context.getSourcesNeedingProcessing();
    assertSize(1, sources);
    assertSame(libA, sources.get(0));
  }

  public void test_computeDocumentationComment_block() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    String comment = "/** Comment */";
    Source source = addSource("/test.dart", createSource(//
        comment,
        "class A {}"));
    LibraryElement libraryElement = context.computeLibraryElement(source);
    assertNotNull(libraryElement);
    ClassElement classElement = libraryElement.getDefiningCompilationUnit().getTypes()[0];
    assertNotNull(libraryElement);
    assertEquals(comment, context.computeDocumentationComment(classElement));
  }

  public void test_computeDocumentationComment_none() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", createSource(//
        "class A {}"));
    LibraryElement libraryElement = context.computeLibraryElement(source);
    assertNotNull(libraryElement);
    ClassElement classElement = libraryElement.getDefiningCompilationUnit().getTypes()[0];
    assertNotNull(libraryElement);
    assertNull(context.computeDocumentationComment(classElement));
  }

  public void test_computeDocumentationComment_null() throws Exception {
    assertNull(context.computeDocumentationComment(null));
  }

  public void test_computeDocumentationComment_singleLine_multiple() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    String comment = createSource(//
        "/// line 1",
        "/// line 2",
        "/// line 3");
    Source source = addSource("/test.dart", createSource(//
        comment,
        "class A {}"));
    LibraryElement libraryElement = context.computeLibraryElement(source);
    assertNotNull(libraryElement);
    ClassElement classElement = libraryElement.getDefiningCompilationUnit().getTypes()[0];
    assertNotNull(libraryElement);
    assertEquals(comment.trim(), context.computeDocumentationComment(classElement));
  }

  public void test_computeErrors_dart_none() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    AnalysisError[] errors = context.computeErrors(source);
    assertLength(0, errors);
  }

  public void test_computeErrors_dart_part() throws Exception {
    Source librarySource = addSource("/lib.dart", "library lib; part 'part.dart';");
    Source partSource = addSource("/part.dart", "part of 'lib';");
    context.parseCompilationUnit(librarySource);
    AnalysisError[] errors = context.computeErrors(partSource);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_computeErrors_dart_some() throws Exception {
    Source source = addSource("/lib.dart", "library 'lib';");
    AnalysisError[] errors = context.computeErrors(source);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_computeErrors_html_none() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    AnalysisError[] errors = context.computeErrors(source);
    assertLength(0, errors);
  }

  public void test_computeExportedLibraries_none() throws Exception {
    Source source = addSource("/test.dart", "library test;");
    assertLength(0, context.computeExportedLibraries(source));
  }

  public void test_computeExportedLibraries_some() throws Exception {
//    addSource("/lib1.dart", "library lib1;");
//    addSource("/lib2.dart", "library lib2;");
    Source source = addSource("/test.dart", "library test; export 'lib1.dart'; export 'lib2.dart';");
    assertLength(2, context.computeExportedLibraries(source));
  }

  public void test_computeHtmlElement_nonHtml() throws Exception {
    Source source = addSource("/test.dart", "library test;");
    assertNull(context.computeHtmlElement(source));
  }

  public void test_computeHtmlElement_valid() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    HtmlElement element = context.computeHtmlElement(source);
    assertNotNull(element);
    assertSame(element, context.computeHtmlElement(source));
  }

  public void test_computeImportedLibraries_none() throws Exception {
    Source source = addSource("/test.dart", "library test;");
    assertLength(0, context.computeImportedLibraries(source));
  }

  public void test_computeImportedLibraries_some() throws Exception {
//    addSource("/lib1.dart", "library lib1;");
//    addSource("/lib2.dart", "library lib2;");
    Source source = addSource("/test.dart", "library test; import 'lib1.dart'; import 'lib2.dart';");
    assertLength(2, context.computeImportedLibraries(source));
  }

  public void test_computeKindOf_html() {
    Source source = addSource("/test.html", "");
    assertSame(SourceKind.HTML, context.computeKindOf(source));
  }

  public void test_computeKindOf_library() {
    Source source = addSource("/test.dart", "library lib;");
    assertSame(SourceKind.LIBRARY, context.computeKindOf(source));
  }

  public void test_computeKindOf_libraryAndPart() {
    Source source = addSource("/test.dart", "library lib; part of lib;");
    assertSame(SourceKind.LIBRARY, context.computeKindOf(source));
  }

  public void test_computeKindOf_part() {
    Source source = addSource("/test.dart", "part of lib;");
    assertSame(SourceKind.PART, context.computeKindOf(source));
  }

  public void test_computeLibraryElement() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
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

  public void test_computeResolvableCompilationUnit_exception() throws Exception {
    Source source = addSourceWithException("/test.dart");
    try {
      context.computeResolvableCompilationUnit(source);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  public void test_computeResolvableCompilationUnit_html() throws Exception {
    Source source = addSource("/lib.html", "<html></html>");
    try {
      context.computeResolvableCompilationUnit(source);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  public void test_computeResolvableCompilationUnit_valid() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertNotNull(compilationUnit);
    assertNotSame(compilationUnit, context.computeResolvableCompilationUnit(source));
  }

  public void test_getAnalysisOptions() throws Exception {
    assertNotNull(context.getAnalysisOptions());
  }

  public void test_getElement() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    LibraryElement core = context.computeLibraryElement(sourceFactory.forUri("dart:core"));
    assertNotNull(core);
    ClassElement classObject = findClass(core.getDefiningCompilationUnit(), "Object");
    assertNotNull(classObject);
    ElementLocation location = classObject.getLocation();
    Element element = context.getElement(location);
    assertSame(classObject, element);
  }

  public void test_getErrors_dart_none() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    AnalysisError[] errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
  }

  public void test_getErrors_dart_some() throws Exception {
    Source source = addSource("/lib.dart", "library 'lib';");
    AnalysisError[] errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source).getErrors();
    assertLength(1, errors);
  }

  public void test_getErrors_html_none() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    AnalysisError[] errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
  }

  public void test_getErrors_html_some() throws Exception {
    Source source = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.dart'/>",
        "</head></html>"));
    AnalysisError[] errors = context.getErrors(source).getErrors();
    assertLength(0, errors);
    context.computeErrors(source);
    errors = context.getErrors(source).getErrors();
    assertLength(1, errors);
  }

  public void test_getHtmlElement_dart() throws Exception {
    Source source = addSource("/test.dart", "");
    assertNull(context.getHtmlElement(source));
    assertNull(context.computeHtmlElement(source));
    assertNull(context.getHtmlElement(source));
  }

  public void test_getHtmlElement_html() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    HtmlElement element = context.getHtmlElement(source);
    assertNull(element);
    context.computeHtmlElement(source);
    element = context.getHtmlElement(source);
    assertNotNull(element);
  }

  public void test_getHtmlFilesReferencing_html() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    Source htmlSource = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.dart'/>",
        "<script type='application/dart' src='test.js'/>",
        "</head></html>"));
    Source librarySource = addSource("/test.dart", "library lib;");
    Source secondHtmlSource = addSource("/test.html", "<html></html>");
    context.computeLibraryElement(librarySource);
    Source[] result = context.getHtmlFilesReferencing(secondHtmlSource);
    assertLength(0, result);
    context.parseHtmlUnit(htmlSource);
    result = context.getHtmlFilesReferencing(secondHtmlSource);
    assertLength(0, result);
  }

  public void test_getHtmlFilesReferencing_library() throws Exception {
    Source htmlSource = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.dart'/>",
        "<script type='application/dart' src='test.js'/>",
        "</head></html>"));
    Source librarySource = addSource("/test.dart", "library lib;");
    Source[] result = context.getHtmlFilesReferencing(librarySource);
    assertLength(0, result);
    context.parseHtmlUnit(htmlSource);
    result = context.getHtmlFilesReferencing(librarySource);
    assertLength(1, result);
    assertEquals(htmlSource, result[0]);
  }

  public void test_getHtmlFilesReferencing_part() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    Source htmlSource = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.dart'/>",
        "<script type='application/dart' src='test.js'/>",
        "</head></html>"));
    Source librarySource = addSource("/test.dart", "library lib; part 'part.dart';");
    Source partSource = addSource("/part.dart", "part of lib;");
    context.computeLibraryElement(librarySource);
    Source[] result = context.getHtmlFilesReferencing(partSource);
    assertLength(0, result);
    context.parseHtmlUnit(htmlSource);
    result = context.getHtmlFilesReferencing(partSource);
    assertLength(1, result);
    assertEquals(htmlSource, result[0]);
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

  public void test_getLaunchableClientLibrarySources() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source[] sources = context.getLaunchableClientLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", createSource("import 'dart:html';", "main() {}"));
    context.computeLibraryElement(source);
    sources = context.getLaunchableClientLibrarySources();
    assertLength(1, sources);
  }

  public void test_getLaunchableServerLibrarySources() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source[] sources = context.getLaunchableServerLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", "main() {}");
    context.computeLibraryElement(source);
    sources = context.getLaunchableServerLibrarySources();
    assertLength(1, sources);
  }

  public void test_getLibrariesContaining() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource("library lib;", "part 'part.dart';"));
    Source partSource = addSource("/part.dart", "part of lib;");
    context.computeLibraryElement(librarySource);

    Source[] result = context.getLibrariesContaining(librarySource);
    assertLength(1, result);
    assertEquals(librarySource, result[0]);

    result = context.getLibrariesContaining(partSource);
    assertLength(1, result);
    assertEquals(librarySource, result[0]);
  }

  public void test_getLibrariesDependingOn() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source libASource = addSource("/libA.dart", "library libA;");
    addSource("/libB.dart", "library libB;");
    Source lib1Source = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "import 'libA.dart';",
        "export 'libB.dart';"));
    Source lib2Source = addSource("/lib2.dart", createSource(//
        "library lib2;",
        "import 'libB.dart';",
        "export 'libA.dart';"));
    context.computeLibraryElement(lib1Source);
    context.computeLibraryElement(lib2Source);
    Source[] result = context.getLibrariesDependingOn(libASource);
    assertContains(result, lib1Source, lib2Source);
  }

  public void test_getLibraryElement() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
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
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "class A {}");
    LibraryElement library = context.computeLibraryElement(source);
    Namespace namespace = context.getPublicNamespace(library);
    assertNotNull(namespace);
    assertInstanceOf(ClassElement.class, namespace.get("A"));
  }

  public void test_getPublicNamespace_source_dart() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", "class A {}");
    context.computeLibraryElement(source);
    Namespace namespace = context.getPublicNamespace(source);
    assertNotNull(namespace);
    assertInstanceOf(ClassElement.class, namespace.get("A"));
  }

  public void test_getPublicNamespace_source_html() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.html", "<html></html>");
    assertNull(context.getPublicNamespace(source));
  }

  public void test_getResolvedCompilationUnit_library() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library libb;");
    LibraryElement library = context.computeLibraryElement(source);
    assertNotNull(context.getResolvedCompilationUnit(source, library));
    context.setContents(source, "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, library));
  }

  public void test_getResolvedCompilationUnit_library_null() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, (LibraryElement) null));
  }

  public void test_getResolvedCompilationUnit_source_dart() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, source));
    context.resolveCompilationUnit(source, source);
    assertNotNull(context.getResolvedCompilationUnit(source, source));
  }

  public void test_getResolvedCompilationUnit_source_html() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.html", "<html></html>");
    assertNull(context.getResolvedCompilationUnit(source, source));
    assertNull(context.resolveCompilationUnit(source, source));
    assertNull(context.getResolvedCompilationUnit(source, source));
  }

  public void test_getSourceFactory() {
    assertSame(sourceFactory, context.getSourceFactory());
  }

  public void test_isClientLibrary_dart() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", createSource("import 'dart:html';", "", "main() {}"));
    assertFalse(context.isClientLibrary(source));
    assertFalse(context.isServerLibrary(source));
    context.computeLibraryElement(source);
    assertTrue(context.isClientLibrary(source));
    assertFalse(context.isServerLibrary(source));
  }

  public void test_isClientLibrary_html() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    assertFalse(context.isClientLibrary(source));
  }

  public void test_isServerLibrary_dart() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.dart", createSource("library lib;", "", "main() {}"));
    assertFalse(context.isClientLibrary(source));
    assertFalse(context.isServerLibrary(source));
    context.computeLibraryElement(source);
    assertFalse(context.isClientLibrary(source));
    assertTrue(context.isServerLibrary(source));
  }

  public void test_isServerLibrary_html() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    assertFalse(context.isServerLibrary(source));
  }

  public void test_parseCompilationUnit_errors() throws Exception {
    Source source = addSource("/lib.dart", "library {");
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertNotNull(compilationUnit);
    AnalysisError[] errors = context.getErrors(source).getErrors();
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_parseCompilationUnit_exception() throws Exception {
    Source source = addSourceWithException("/test.dart");
    try {
      context.parseCompilationUnit(source);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected.
    }
  }

  public void test_parseCompilationUnit_html() throws Exception {
    Source source = addSource("/test.html", "<html></html>");
    assertNull(context.parseCompilationUnit(source));
  }

  public void test_parseCompilationUnit_noErrors() throws Exception {
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertNotNull(compilationUnit);
    assertLength(0, context.getErrors(source).getErrors());
  }

  public void test_parseCompilationUnit_nonExistentSource() throws Exception {
    Source source = new FileBasedSource(sourceFactory.getContentCache(), createFile("/test.dart"));
    try {
      context.parseCompilationUnit(source);
      fail("Expected AnalysisException because file does not exist");
    } catch (AnalysisException exception) {
      // Expected result
    }
  }

  public void test_parseHtmlUnit_noErrors() throws Exception {
    Source source = addSource("/lib.html", "<html></html>");
    HtmlUnit unit = context.parseHtmlUnit(source);
    assertNotNull(unit);
  }

  public void test_performAnalysisTask_IOException() throws Exception {
    addSourceWithException("/test.dart");
    //
    // Simulate a typical analysis worker.
    //
    int maxCount = 5;
    context.performAnalysisTask();
    for (int count = 0; count < maxCount; count++) {
      if (context.performAnalysisTask() == null) {
        return;
      }
    }
    fail("Did not finish analysis after " + maxCount + " iterations");
  }

  public void test_performAnalysisTask_modifiedAfterParse() throws Exception {
    Source source = addSource("/test.dart", "library lib;");
    long initialTime = source.getModificationStamp();
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(source);
    context.setAnalysisPriorityOrder(sources);
    context.parseCompilationUnit(source);
    while (initialTime == System.currentTimeMillis()) {
      Thread.sleep(1); // Force the modification time to be different.
    }
    sourceFactory.getContentCache().setContents(source, "library test;");
    assertTrue(initialTime != source.getModificationStamp());
    for (int i = 0; i < 4; i++) {
      ChangeNotice[] notice = context.performAnalysisTask();
      if (notice == null) {
        break;
      }
    }
    ChangeNotice[] notice = context.performAnalysisTask();
    if (notice != null) {
      fail("performAnalysisTask failed to terminate after analyzing all sources");
    }
    assertNotNull(
        "performAnalysisTask failed to compute an element model",
        context.getLibraryElement(source));
  }

  public void test_performAnalysisTask_stress() throws Exception {
    Field field = AnalysisContextImpl2.class.getDeclaredField("MAX_CACHE_SIZE");
    field.setAccessible(true);
    int maxCacheSize = field.getInt(null);
    int sourceCount = maxCacheSize + 2;
    ArrayList<Source> sources = new ArrayList<Source>(sourceCount);
    ChangeSet changeSet = new ChangeSet();
    for (int i = 0; i < sourceCount; i++) {
      Source source = addSource("/lib" + i + ".dart", "library lib" + i + ";");
      sources.add(source);
      changeSet.added(source);
    }
    context.applyChanges(changeSet);
    context.setAnalysisPriorityOrder(sources);
    for (int i = 0; i < sourceCount * 3; i++) {
      ChangeNotice[] notice = context.performAnalysisTask();
      if (notice == null) {
        break;
      }
    }
    ChangeNotice[] notice = context.performAnalysisTask();
    if (notice != null) {
      fail("performAnalysisTask failed to terminate after analyzing all sources");
    }
  }

  public void test_resolveCompilationUnit_library() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    LibraryElement library = context.computeLibraryElement(source);
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, library);
    assertNotNull(compilationUnit);
  }

  public void test_resolveCompilationUnit_source() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
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

  public void test_setAnalysisOptions() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    context.setAnalysisOptions(options);
    assertEquals(options, context.getAnalysisOptions());
  }

  public void test_setAnalysisPriorityOrder_empty() {
    context.setAnalysisPriorityOrder(new ArrayList<Source>());
  }

  public void test_setAnalysisPriorityOrder_nonEmpty() {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource("/lib.dart", "library lib;"));
    context.setAnalysisPriorityOrder(sources);
  }

  public void test_setContents_libraryWithPart() throws Exception {
    context = AnalysisContextFactory.context2WithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "int a = 0;"));
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        "int b = a;"));
    context.computeLibraryElement(librarySource);

    context.setContents(librarySource, createSource(//
        "library lib;",
        "part 'part.dart';",
        "int aa = 0;"));
    assertNull(context.getResolvedCompilationUnit(partSource, librarySource));
  }

  public void test_setSourceFactory() {
    assertEquals(sourceFactory, context.getSourceFactory());
    SourceFactory factory = new SourceFactory();
    context.setSourceFactory(factory);
    assertEquals(factory, context.getSourceFactory());
  }

  private Source addSource(String fileName, String contents) {
    Source source = new FileBasedSource(sourceFactory.getContentCache(), createFile(fileName));
    sourceFactory.setContents(source, contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    return source;
  }

  private Source addSourceWithException(String fileName) {
    Source source = new FileBasedSource(sourceFactory.getContentCache(), createFile(fileName)) {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        throw new IOException("I/O Exception while getting the contents of " + getFullName());
      }
    };
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
      if (classElement.getDisplayName().equals(className)) {
        return classElement;
      }
    }
    return null;
  }
}
