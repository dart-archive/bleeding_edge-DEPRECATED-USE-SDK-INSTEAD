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
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.task.ResolveDartLibraryTask;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AnalysisContextImplTest extends EngineTestCase {
  /**
   * Returns {@code true} if there is an {@link AnalysisError} with {@link ErrorSeverity#ERROR} in
   * the given {@link AnalysisErrorInfo}.
   */
  private static boolean hasAnalysisErrorWithErrorSeverity(AnalysisErrorInfo errorInfo) {
    AnalysisError[] errors = errorInfo.getErrors();
    for (AnalysisError analysisError : errors) {
      if (analysisError.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
        return true;
      }
    }
    return false;
  }

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

  public void fail_mergeContext() {
    fail("Implement this");
  }

  public void fail_performAnalysisTask_importedLibraryDelete_html() throws Exception {
    Source htmlSource = addSource("/page.html", createSource(//
        "<html><body><script type=\"application/dart\">",
        "  import 'libB.dart';",
        "  main() {print('hello dart');}",
        "</script></body></html>"));
    Source libBSource = addSource("/libB.dart", "library libB;");
    analyzeAll_assertFinished();
    assertNotNull("htmlUnit resolved 1", context.getResolvedHtmlUnit(htmlSource));
    assertNotNull("libB resolved 1", context.getResolvedCompilationUnit(libBSource, libBSource));
    assertTrue(
        "htmlSource doesn't have errors",
        !hasAnalysisErrorWithErrorSeverity(context.getErrors(htmlSource)));
    // remove libB.dart content and analyze
    context.setContents(libBSource, null);
    analyzeAll_assertFinished();
    assertNotNull("htmlUnit resolved 1", context.getResolvedHtmlUnit(htmlSource));
    AnalysisErrorInfo errors = context.getErrors(htmlSource);
    assertTrue("htmlSource has an error", hasAnalysisErrorWithErrorSeverity(errors));
  }

  public void fail_recordLibraryElements() {
    fail("Implement this");
  }

  @Override
  public void setUp() {
    context = new AnalysisContextImpl();
    sourceFactory = new SourceFactory(
        new DartUriResolver(DirectoryBasedDartSdk.getDefaultSdk()),
        new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    options.setCacheSize(256);
    context.setAnalysisOptions(options);
  }

  public void test_applyChanges_add() {
    assertTrue(context.getSourcesNeedingProcessing().isEmpty());
    Source source = addSource("/test.dart", "main() {}");
    assertTrue(context.getSourcesNeedingProcessing().contains(source));
  }

  public void test_applyChanges_change_flush_element() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
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
    context = AnalysisContextFactory.contextWithCore();
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
    assertNull(context.performAnalysisTask().getChangeNotices());
  }

  public void test_applyChanges_overriddenSource() throws Exception {
    // Note: addSource adds the source to the contentCache.
    Source source = addSource("/test.dart", "library test;");
    context.computeErrors(source);
    while (!context.getSourcesNeedingProcessing().isEmpty()) {
      context.performAnalysisTask();
    }
    // Adding the source as a changedSource should have no effect since it is already overridden
    // in the content cache.
    ChangeSet changeSet = new ChangeSet();
    changeSet.changedSource(source);
    context.applyChanges(changeSet);
    assertSizeOfList(0, context.getSourcesNeedingProcessing());
  }

  public void test_applyChanges_remove() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source libA = addSource("/libA.dart", createSource(//
        "library libA;",
        "import 'libB.dart';"));
    Source libB = addSource("/libB.dart", createSource(//
        "library libB;"));
    LibraryElement libAElement = context.computeLibraryElement(libA);
    LibraryElement[] importedLibraries = libAElement.getImportedLibraries();
    assertLength(2, importedLibraries);
    context.computeErrors(libA);
    context.computeErrors(libB);
    assertSizeOfList(0, context.getSourcesNeedingProcessing());
    context.setContents(libB, null);
    removeSource(libB);
    List<Source> sources = context.getSourcesNeedingProcessing();
    assertSizeOfList(1, sources);
    assertSame(libA, sources.get(0));

    libAElement = context.computeLibraryElement(libA);
    importedLibraries = libAElement.getImportedLibraries();
    assertLength(1, importedLibraries);
  }

  public void test_applyChanges_removeContainer() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source libA = addSource("/libA.dart", createSource(//
        "library libA;",
        "import 'libB.dart';"));
    final Source libB = addSource("/libB.dart", createSource(//
        "library libB;"));
    context.computeLibraryElement(libA);
    context.computeErrors(libA);
    context.computeErrors(libB);
    assertSizeOfList(0, context.getSourcesNeedingProcessing());

    ChangeSet changeSet = new ChangeSet();
    changeSet.removedContainer(new SourceContainer() {
      @Override
      public boolean contains(Source source) {
        return source.equals(libB);
      }
    });
    context.applyChanges(changeSet);
    List<Source> sources = context.getSourcesNeedingProcessing();
    assertSizeOfList(1, sources);
    assertSame(libA, sources.get(0));
  }

  public void test_computeDocumentationComment_block() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
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
    context = AnalysisContextFactory.contextWithCore();
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

  public void test_computeDocumentationComment_singleLine_multiple_EOL_n() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    String comment = "/// line 1\n" + "/// line 2\n" + "/// line 3\n";
    Source source = addSource("/test.dart", comment + "class A {}");
    LibraryElement libraryElement = context.computeLibraryElement(source);
    assertNotNull(libraryElement);
    ClassElement classElement = libraryElement.getDefiningCompilationUnit().getTypes()[0];
    assertNotNull(libraryElement);
    String actual = context.computeDocumentationComment(classElement);
    assertEquals("/// line 1\n" + "/// line 2\n" + "/// line 3", actual);
  }

  public void test_computeDocumentationComment_singleLine_multiple_EOL_rn() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    String comment = "/// line 1\r\n" + "/// line 2\r\n" + "/// line 3\r\n";
    Source source = addSource("/test.dart", comment + "class A {}");
    LibraryElement libraryElement = context.computeLibraryElement(source);
    assertNotNull(libraryElement);
    ClassElement classElement = libraryElement.getDefiningCompilationUnit().getTypes()[0];
    assertNotNull(libraryElement);
    String actual = context.computeDocumentationComment(classElement);
    assertEquals("/// line 1\n" + "/// line 2\n" + "/// line 3", actual);
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

  public void test_computeResolvableCompilationUnit_dart_exception() throws Exception {
    TestSource source = addSourceWithException("/test.dart");
    try {
      context.computeResolvableCompilationUnit(source);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  public void test_computeResolvableCompilationUnit_html_exception() throws Exception {
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
    CompilationUnit parsedUnit = context.parseCompilationUnit(source);
    assertNotNull(parsedUnit);
    CompilationUnit resolvedUnit = context.computeResolvableCompilationUnit(source).getCompilationUnit();
    assertNotNull(resolvedUnit);
    assertNotSame(parsedUnit, resolvedUnit);
  }

  public void test_dispose() throws Exception {
    assertFalse(context.isDisposed());
    context.dispose();
    assertTrue(context.isDisposed());
  }

  public void test_exists_false() throws Exception {
    assertFalse(context.exists(new TestSource()));
  }

  public void test_exists_null() throws Exception {
    assertFalse(context.exists(null));
  }

  public void test_exists_overridden() throws Exception {
    Source source = new TestSource();
    context.setContents(source, "");
    assertTrue(context.exists(source));
  }

  public void test_exists_true() throws Exception {
    assertTrue(context.exists(new TestSource() {
      @Override
      public boolean exists() {
        return true;
      }
    }));
  }

  public void test_getAnalysisOptions() throws Exception {
    assertNotNull(context.getAnalysisOptions());
  }

  public void test_getContents_fromSource() throws Exception {
    final String content = "library lib;";
    TimestampedData<CharSequence> contents = context.getContents(new TestSource(content));
    assertEquals(content, contents.getData().toString());
  }

  public void test_getContents_overridden() throws Exception {
    final String content = "library lib;";
    Source source = new TestSource();
    context.setContents(source, content);
    TimestampedData<CharSequence> contents = context.getContents(source);
    assertEquals(content, contents.getData().toString());
  }

  public void test_getContents_unoverridden() throws Exception {
    final String content = "library lib;";
    Source source = new TestSource(content);
    context.setContents(source, "part of lib;");
    context.setContents(source, null);
    TimestampedData<CharSequence> contents = context.getContents(source);
    assertEquals(content, contents.getData().toString());
  }

  public void test_getDeclaredVariables() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    assertNotNull(context.getDeclaredVariables());
  }

  public void test_getElement() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    LibraryElement core = context.computeLibraryElement(sourceFactory.forUri("dart:core"));
    assertNotNull(core);
    ClassElement classObject = findClass(core.getDefiningCompilationUnit(), "Object");
    assertNotNull(classObject);
    ElementLocation location = classObject.getLocation();
    Element element = context.getElement(location);
    assertSame(classObject, element);
  }

  public void test_getElement_constructor_named() throws Exception {
    Source source = addSource("/lib.dart", createSource(//
        "class A {",
        "  A.named() {}",
        "}"));
    analyzeAll_assertFinished();
    LibraryElement library = context.computeLibraryElement(source);
    ClassElement classA = findClass(library.getDefiningCompilationUnit(), "A");
    ConstructorElement constructor = classA.getConstructors()[0];
    ElementLocation location = constructor.getLocation();
    Element element = context.getElement(location);
    assertSame(constructor, element);
  }

  public void test_getElement_constructor_unnamed() throws Exception {
    Source source = addSource("/lib.dart", createSource(//
        "class A {",
        "  A() {}",
        "}"));
    analyzeAll_assertFinished();
    LibraryElement library = context.computeLibraryElement(source);
    ClassElement classA = findClass(library.getDefiningCompilationUnit(), "A");
    ConstructorElement constructor = classA.getConstructors()[0];
    ElementLocation location = constructor.getLocation();
    Element element = context.getElement(location);
    assertSame(constructor, element);
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
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
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
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
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
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source[] sources = context.getLaunchableClientLibrarySources();
    assertLength(0, sources);
    Source source = addSource("/test.dart", createSource("import 'dart:html';", "main() {}"));
    context.computeLibraryElement(source);
    sources = context.getLaunchableClientLibrarySources();
    assertLength(1, sources);
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

  public void test_getLibrariesContaining() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
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
    context = AnalysisContextFactory.contextWithCore();
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

  public void test_getLibrariesReferencedFromHtml() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source htmlSource = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.dart'/>",
        "<script type='application/dart' src='test.js'/>",
        "</head></html>"));
    Source librarySource = addSource("/test.dart", "library lib;");
    context.computeLibraryElement(librarySource);
    context.parseHtmlUnit(htmlSource);
    Source[] result = context.getLibrariesReferencedFromHtml(htmlSource);
    assertLength(1, result);
    assertEquals(librarySource, result[0]);
  }

  public void test_getLibrariesReferencedFromHtml_no() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source htmlSource = addSource("/test.html", createSource(//
        "<html><head>",
        "<script type='application/dart' src='test.js'/>",
        "</head></html>"));
    addSource("/test.dart", "library lib;");
    context.parseHtmlUnit(htmlSource);
    Source[] result = context.getLibrariesReferencedFromHtml(htmlSource);
    assertLength(0, result);
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
    int originalLength = sources.length;
    Source source = addSource("/test.dart", "library lib;");
    context.computeKindOf(source);
    sources = context.getLibrarySources();
    assertLength(originalLength + 1, sources);
    for (Source returnedSource : sources) {
      if (returnedSource.equals(source)) {
        return;
      }
    }
    fail("The added source was not in the list of library sources");
  }

  public void test_getLineInfo() throws Exception {
    Source source = addSource("/test.dart", createSource("library lib;", "", "main() {}"));
    LineInfo info = context.getLineInfo(source);
    assertNull(info);
    context.parseCompilationUnit(source);
    info = context.getLineInfo(source);
    assertNotNull(info);
  }

  public void test_getModificationStamp_fromSource() throws Exception {
    final long stamp = 42L;
    assertEquals(stamp, context.getModificationStamp(new TestSource() {
      @Override
      public long getModificationStamp() {
        return stamp;
      }
    }));
  }

  public void test_getModificationStamp_overridden() throws Exception {
    final long stamp = 42L;
    Source source = new TestSource() {
      @Override
      public long getModificationStamp() {
        return stamp;
      }
    };
    context.setContents(source, "");
    assertTrue(stamp != context.getModificationStamp(source));
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

  public void test_getRefactoringUnsafeSources() throws Exception {
    // not sources initially
    Source[] sources = context.getRefactoringUnsafeSources();
    assertLength(0, sources);
    // add new source, unresolved
    Source source = addSource("/test.dart", "library lib;");
    sources = context.getRefactoringUnsafeSources();
    assertLength(1, sources);
    assertEquals(source, sources[0]);
    // resolve source
    context.computeLibraryElement(source);
    sources = context.getRefactoringUnsafeSources();
    assertLength(0, sources);
  }

  public void test_getResolvedCompilationUnit_library() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library libb;");
    LibraryElement library = context.computeLibraryElement(source);
    assertNotNull(context.getResolvedCompilationUnit(source, library));
    context.setContents(source, "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, library));
  }

  public void test_getResolvedCompilationUnit_library_null() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, (LibraryElement) null));
  }

  public void test_getResolvedCompilationUnit_source_dart() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    assertNull(context.getResolvedCompilationUnit(source, source));
    context.resolveCompilationUnit(source, source);
    assertNotNull(context.getResolvedCompilationUnit(source, source));
  }

  public void test_getResolvedCompilationUnit_source_html() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.html", "<html></html>");
    assertNull(context.getResolvedCompilationUnit(source, source));
    assertNull(context.resolveCompilationUnit(source, source));
    assertNull(context.getResolvedCompilationUnit(source, source));
  }

  public void test_getResolvedHtmlUnit() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/test.html", "<html></html>");
    assertNull(context.getResolvedHtmlUnit(source));
    context.resolveHtmlUnit(source);
    assertNotNull(context.getResolvedHtmlUnit(source));
  }

  public void test_getSourceFactory() {
    assertSame(sourceFactory, context.getSourceFactory());
  }

  public void test_getStatistics() {
    AnalysisContextStatistics statistics = context.getStatistics();
    assertNotNull(statistics);
    // The following lines are fragile. The values depend on the number of libraries in the SDK.
//    assertLength(0, statistics.getCacheRows());
//    assertLength(0, statistics.getExceptions());
//    assertLength(0, statistics.getSources());
  }

  public void test_isClientLibrary_dart() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
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
    context = AnalysisContextFactory.contextWithCore();
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
    Source source = new FileBasedSource(createFile("/test.dart"));
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

  public void test_parseHtmlUnit_resolveDirectives() throws Exception {
    Source libSource = addSource("/lib.dart", createSource(//
        "library lib;",
        "class ClassA {}"));
    Source source = addSource("/lib.html", createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart'>",
        "    import 'lib.dart';",
        "    ClassA v = null;",
        "  </script>",
        "</head>",
        "<body>",
        "</body>",
        "</html>"));
    HtmlUnit unit = context.parseHtmlUnit(source);
    // import directive should be resolved
    XmlTagNode htmlNode = unit.getTagNodes().get(0);
    XmlTagNode headNode = htmlNode.getTagNodes().get(0);
    HtmlScriptTagNode scriptNode = (HtmlScriptTagNode) headNode.getTagNodes().get(0);
    CompilationUnit script = scriptNode.getScript();
    ImportDirective importNode = (ImportDirective) script.getDirectives().get(0);
    assertNotNull(importNode.getUriContent());
    assertEquals(libSource, importNode.getSource());
  }

  public void test_performAnalysisTask_addPart() throws Exception {
    Source libSource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';"));
    // run all tasks without part
    analyzeAll_assertFinished();
    // add part and run all tasks
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        ""));
    analyzeAll_assertFinished();
    // "libSource" should be here 
    Source[] librariesWithPart = context.getLibrariesContaining(partSource);
    assertContains(librariesWithPart, libSource);
  }

  public void test_performAnalysisTask_changeLibraryContents() throws Exception {
    Source libSource = addSource("/test.dart", "library lib; part 'test-part.dart';");
    Source partSource = addSource("/test-part.dart", "part of lib;");
    analyzeAll_assertFinished();
    assertNotNull("library resolved 1", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 1", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #1
    context.setContents(libSource, "library lib;");
    assertNull("library changed 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 2", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part resolved 2", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #2
    context.setContents(libSource, "library lib; part 'test-part.dart';");
    assertNull("library changed 3", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 3", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 3", context.getResolvedCompilationUnit(partSource, libSource));
  }

  public void test_performAnalysisTask_changeLibraryThenPartContents() throws Exception {
    Source libSource = addSource("/test.dart", "library lib; part 'test-part.dart';");
    Source partSource = addSource("/test-part.dart", "part of lib;");
    analyzeAll_assertFinished();
    assertNotNull("library resolved 1", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 1", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #1
    context.setContents(libSource, "library lib;");
    assertNull("library changed 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 2", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part resolved 2", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #2
    context.setContents(partSource, "part of lib; // 1");
    // Assert that changing the part's content does not effect the library 
    // now that it is no longer part of that library
    assertNotNull("library changed 3", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 3", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 3", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part resolved 3", context.getResolvedCompilationUnit(partSource, libSource));
  }

  public void test_performAnalysisTask_changePartContents_makeItAPart() throws Exception {
    Source libSource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "void f(x) {}"));
    Source partSource = addSource("/part.dart", createSource(//
        "void g() { f(null); }"));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 1", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 1", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze
    context.setContents(partSource, createSource(//
        "part of lib;",
        "void g() { f(null); }"));
    assertNull("library changed 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 2", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 2", context.getResolvedCompilationUnit(partSource, libSource));

    assertLength(0, context.getErrors(libSource).getErrors());
    assertLength(0, context.getErrors(partSource).getErrors());
  }

  /**
   * https://code.google.com/p/dart/issues/detail?id=12424
   */
  public void test_performAnalysisTask_changePartContents_makeItNotPart() throws Exception {
    Source libSource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "void f(x) {}"));
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        "void g() { f(null); }"));
    analyzeAll_assertFinished();
    assertLength(0, context.getErrors(libSource).getErrors());
    assertLength(0, context.getErrors(partSource).getErrors());
    // Remove 'part' directive, which should make "f(null)" an error.
    context.setContents(partSource, createSource(//
        "//part of lib;",
        "void g() { f(null); }"));
    analyzeAll_assertFinished();
    assertTrue(context.getErrors(libSource).getErrors().length != 0);
  }

  public void test_performAnalysisTask_changePartContents_noSemanticChanges() throws Exception {
    Source libSource = addSource("/test.dart", "library lib; part 'test-part.dart';");
    Source partSource = addSource("/test-part.dart", "part of lib;");
    analyzeAll_assertFinished();
    assertNotNull("library resolved 1", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 1", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #1
    context.setContents(partSource, "part of lib; // 1");
    assertNull("library changed 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 2", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 2", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 2", context.getResolvedCompilationUnit(partSource, libSource));
    // update and analyze #2
    context.setContents(partSource, "part of lib; // 12");
    assertNull("library changed 3", context.getResolvedCompilationUnit(libSource, libSource));
    assertNull("part changed 3", context.getResolvedCompilationUnit(partSource, libSource));
    analyzeAll_assertFinished();
    assertNotNull("library resolved 3", context.getResolvedCompilationUnit(libSource, libSource));
    assertNotNull("part resolved 3", context.getResolvedCompilationUnit(partSource, libSource));
  }

  public void test_performAnalysisTask_importedLibraryAdd() throws Exception {
    Source libASource = addSource("/libA.dart", "library libA; import 'libB.dart';");
    analyzeAll_assertFinished();
    assertNotNull("libA resolved 1", context.getResolvedCompilationUnit(libASource, libASource));
    assertTrue(
        "libA has an error",
        hasAnalysisErrorWithErrorSeverity(context.getErrors(libASource)));

    // add libB.dart and analyze
    Source libBSource = addSource("/libB.dart", "library libB;");
    analyzeAll_assertFinished();
    assertNotNull("libA resolved 2", context.getResolvedCompilationUnit(libASource, libASource));
    assertNotNull("libB resolved 2", context.getResolvedCompilationUnit(libBSource, libBSource));
    assertTrue(
        "libA doesn't have errors",
        !hasAnalysisErrorWithErrorSeverity(context.getErrors(libASource)));
  }

  public void test_performAnalysisTask_importedLibraryAdd_html() throws Exception {
    Source htmlSource = addSource("/page.html", createSource(//
        "<html><body><script type=\"application/dart\">",
        "  import '/libB.dart';",
        "  main() {print('hello dart');}",
        "</script></body></html>"));
    analyzeAll_assertFinished();
    assertNotNull("htmlUnit resolved 1", context.getResolvedHtmlUnit(htmlSource));
    assertTrue(
        "htmlSource has an error",
        hasAnalysisErrorWithErrorSeverity(context.getErrors(htmlSource)));
    // add libB.dart and analyze
    Source libBSource = addSource("/libB.dart", "library libB;");
    analyzeAll_assertFinished();
    assertNotNull("htmlUnit resolved 1", context.getResolvedHtmlUnit(htmlSource));
    assertNotNull("libB resolved 2", context.getResolvedCompilationUnit(libBSource, libBSource));
    AnalysisErrorInfo errors = context.getErrors(htmlSource);
    assertTrue("htmlSource doesn't have errors", !hasAnalysisErrorWithErrorSeverity(errors));
  }

  public void test_performAnalysisTask_importedLibraryDelete() throws Exception {
    Source libASource = addSource("/libA.dart", "library libA; import 'libB.dart';");
    Source libBSource = addSource("/libB.dart", "library libB;");
    analyzeAll_assertFinished();
    assertNotNull("libA resolved 1", context.getResolvedCompilationUnit(libASource, libASource));
    assertNotNull("libB resolved 1", context.getResolvedCompilationUnit(libBSource, libBSource));
    assertTrue(
        "libA doesn't have errors",
        !hasAnalysisErrorWithErrorSeverity(context.getErrors(libASource)));
    // remove libB.dart content and analyze
    context.setContents(libBSource, null);
    analyzeAll_assertFinished();
    assertNotNull("libA resolved 2", context.getResolvedCompilationUnit(libASource, libASource));
    assertTrue(
        "libA has an error",
        hasAnalysisErrorWithErrorSeverity(context.getErrors(libASource)));
  }

  public void test_performAnalysisTask_IOException() throws Exception {
    TestSource source = addSourceWithException("/test.dart", "library test;");
    long oldTimestamp = context.getModificationStamp(source);
    source.setGenerateExceptionOnRead(false);
    analyzeAll_assertFinished();
    assertEquals(1, source.getReadCount());
    source.setGenerateExceptionOnRead(true);
    do {
      changeSource(source, "");
      // Ensure that the timestamp differs so that analysis engine notices the change
    } while (oldTimestamp == context.getModificationStamp(source));
    analyzeAll_assertFinished();
    assertEquals(2, source.getReadCount());
  }

  public void test_performAnalysisTask_missingPart() throws Exception {
    Source source = addSource("/test.dart", "library lib; part 'no-such-file.dart';");
    analyzeAll_assertFinished();
    assertNotNull(
        "performAnalysisTask failed to compute an element model",
        context.getLibraryElement(source));
  }

  public void test_performAnalysisTask_modifiedAfterParse() throws Exception {
    Source source = addSource("/test.dart", "library lib;");
    long initialTime = context.getModificationStamp(source);
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(source);
    context.setAnalysisPriorityOrder(sources);
    context.parseCompilationUnit(source);
    while (initialTime == System.currentTimeMillis()) {
      Thread.sleep(1); // Force the modification time to be different.
    }
    context.setContents(source, "library test;");
    assertTrue(initialTime != context.getModificationStamp(source));
    analyzeAll_assertFinished();
    assertNotNull(
        "performAnalysisTask failed to compute an element model",
        context.getLibraryElement(source));
  }

  public void test_resolveCompilationUnit_import_relative() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    Source sourceA = addSource("/libA.dart", "library libA; import 'libB.dart'; class A{}");
    addSource("/libB.dart", "library libB; class B{}");
    CompilationUnit compilationUnit = context.resolveCompilationUnit(sourceA, sourceA);
    LibraryElement library = compilationUnit.getElement().getLibrary();
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core", "libB");
    LibraryElement[] visibleLibraries = library.getVisibleLibraries();
    assertNamedElements(visibleLibraries, "dart.core", "libA", "libB");
  }

  public void test_resolveCompilationUnit_import_relative_cyclic() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    Source sourceA = addSource("/libA.dart", "library libA; import 'libB.dart'; class A{}");
    addSource("/libB.dart", "library libB; import 'libA.dart'; class B{}");
    CompilationUnit compilationUnit = context.resolveCompilationUnit(sourceA, sourceA);
    LibraryElement library = compilationUnit.getElement().getLibrary();
    LibraryElement[] importedLibraries = library.getImportedLibraries();
    assertNamedElements(importedLibraries, "dart.core", "libB");
    LibraryElement[] visibleLibraries = library.getVisibleLibraries();
    assertNamedElements(visibleLibraries, "dart.core", "libA", "libB");
  }

  public void test_resolveCompilationUnit_library() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    LibraryElement library = context.computeLibraryElement(source);
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, library);
    assertNotNull(compilationUnit);
    assertNotNull(compilationUnit.getElement());
  }

  public void test_resolveCompilationUnit_source() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(compilationUnit);
  }

  public void test_resolveCompilationUnit_sourceChangeDuringResolution() throws Exception {
    context = new AnalysisContextFactory.AnalysisContextForTests() {
      @Override
      protected DartEntry recordResolveDartLibraryTaskResults(ResolveDartLibraryTask task)
          throws AnalysisException {
        ChangeSet changeSet = new ChangeSet();
        changeSet.changedSource(task.getLibrarySource());
        applyChanges(changeSet);
        return super.recordResolveDartLibraryTaskResults(task);
      }
    };
    AnalysisContextFactory.initContextWithCore(context);
    sourceFactory = context.getSourceFactory();
    Source source = addSource("/lib.dart", "library lib;");
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(compilationUnit);
    assertNotNull(context.getLineInfo(source));
  }

  public void test_resolveHtmlUnit() throws Exception {
    Source source = addSource("/lib.html", "<html></html>");
    HtmlUnit unit = context.resolveHtmlUnit(source);
    assertNotNull(unit);
  }

  public void test_setAnalysisOptions() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.setCacheSize(42);
    options.setDart2jsHint(false);
    options.setHint(false);
    options.setEnableTypeChecks(true);
    context.setAnalysisOptions(options);
    AnalysisOptions result = context.getAnalysisOptions();
    assertEquals(options.getCacheSize(), result.getCacheSize());
    assertEquals(options.getDart2jsHint(), result.getDart2jsHint());
    assertEquals(options.getHint(), result.getHint());
    assertEquals(options.getEnableTypeChecks(), result.getEnableTypeChecks());
  }

  public void test_setAnalysisOptions_reduceAnalysisPriorityOrder() throws Exception {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    ArrayList<Source> sources = new ArrayList<Source>();
    for (int index = 0; index < options.getCacheSize(); index++) {
      sources.add(addSource("/lib.dart" + index, ""));
    }
    context.setAnalysisPriorityOrder(sources);
    int oldPriorityOrderSize = getPriorityOrder(context).length;
    options.setCacheSize(options.getCacheSize() - 10);
    context.setAnalysisOptions(options);
    assertTrue(oldPriorityOrderSize > getPriorityOrder(context).length);
  }

  public void test_setAnalysisPriorityOrder_empty() {
    context.setAnalysisPriorityOrder(new ArrayList<Source>());
  }

  public void test_setAnalysisPriorityOrder_lessThanCacheSize() throws Exception {
    AnalysisOptions options = context.getAnalysisOptions();
    ArrayList<Source> sources = new ArrayList<Source>();
    for (int index = 0; index < options.getCacheSize(); index++) {
      sources.add(addSource("/lib.dart" + index, ""));
    }
    context.setAnalysisPriorityOrder(sources);
    assertTrue(options.getCacheSize() > getPriorityOrder(context).length);
  }

  public void test_setAnalysisPriorityOrder_nonEmpty() {
    ArrayList<Source> sources = new ArrayList<Source>();
    sources.add(addSource("/lib.dart", "library lib;"));
    context.setAnalysisPriorityOrder(sources);
  }

  public void test_setChangedContents_libraryWithPart() throws Exception {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.setIncremental(true);
    context = AnalysisContextFactory.contextWithCoreAndOptions(options);
    sourceFactory = context.getSourceFactory();
    String oldCode = createSource(//
        "library lib;",
        "part 'part.dart';",
        "int a = 0;");
    Source librarySource = addSource("/lib.dart", oldCode);
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        "int b = a;"));
    LibraryElement element = context.computeLibraryElement(librarySource);
    CompilationUnit unit = context.getResolvedCompilationUnit(librarySource, element);
    assertNotNull(unit);

    int offset = oldCode.indexOf("int a") + 4;
    final String newCode = createSource(//
        "library lib;",
        "part 'part.dart';",
        "int ya = 0;");
    assertNull(getIncrementalAnalysisCache(context));
    context.setChangedContents(librarySource, newCode, offset, 0, 1);
    assertEquals(newCode, context.getContents(librarySource).getData());
    IncrementalAnalysisCache incrementalCache = getIncrementalAnalysisCache(context);
    assertEquals(librarySource, incrementalCache.getLibrarySource());
    assertSame(unit, incrementalCache.getResolvedUnit());
    assertNull(context.getResolvedCompilationUnit(partSource, librarySource));
    assertEquals(newCode, incrementalCache.getNewContents());
  }

  public void test_setChangedContents_notResolved() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    options.setIncremental(true);
    context.setAnalysisOptions(options);
    sourceFactory = context.getSourceFactory();
    String oldCode = createSource(//
        "library lib;",
        "int a = 0;");
    Source librarySource = addSource("/lib.dart", oldCode);

    int offset = oldCode.indexOf("int a") + 4;
    final String newCode = createSource(//
        "library lib;",
        "int ya = 0;");
    context.setChangedContents(librarySource, newCode, offset, 0, 1);
    assertEquals(newCode, context.getContents(librarySource).getData());
    assertNull(getIncrementalAnalysisCache(context));
  }

  public void test_setContents_libraryWithPart() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "int a = 0;"));
    Source partSource = addSource("/part.dart", createSource(//
        "part of lib;",
        "int b = a;"));
    context.computeLibraryElement(librarySource);

    IncrementalAnalysisCache incrementalCache = new IncrementalAnalysisCache(
        librarySource,
        librarySource,
        null,
        null,
        null,
        0,
        0,
        0);
    setIncrementalAnalysisCache(context, incrementalCache);
    assertSame(incrementalCache, getIncrementalAnalysisCache(context));

    context.setContents(librarySource, createSource(//
        "library lib;",
        "part 'part.dart';",
        "int aa = 0;"));
    assertNull(context.getResolvedCompilationUnit(partSource, librarySource));
    assertNull(getIncrementalAnalysisCache(context));
  }

  public void test_setContents_null() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "int a = 0;"));
    context.computeLibraryElement(librarySource);

    IncrementalAnalysisCache incrementalCache = new IncrementalAnalysisCache(
        librarySource,
        librarySource,
        null,
        null,
        null,
        0,
        0,
        0);
    setIncrementalAnalysisCache(context, incrementalCache);
    assertSame(incrementalCache, getIncrementalAnalysisCache(context));

    context.setContents(librarySource, null);
    assertNull(context.getResolvedCompilationUnit(librarySource, librarySource));
    assertNull(getIncrementalAnalysisCache(context));
  }

  public void test_setSourceFactory() {
    assertEquals(sourceFactory, context.getSourceFactory());
    SourceFactory factory = new SourceFactory();
    context.setSourceFactory(factory);
    assertEquals(factory, context.getSourceFactory());
  }

  public void test_unreadableSource() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    Source test1 = addSource("/test1.dart", createSource(//
        "import 'test2.dart';",
        "library test1;"));
    Source test2 = addSource("/test2.dart", createSource(//
        "import 'test1.dart';",
        "import 'test3.dart';",
        "library test2;"));
    Source test3 = addSourceWithException("/test3.dart");
    analyzeAll_assertFinished();
    // test1 and test2 should have been successfully analyzed despite the fact that
    // test3 couldn't be read.
    assertNotNull(context.computeLibraryElement(test1));
    assertNotNull(context.computeLibraryElement(test2));
    assertNull(context.computeLibraryElement(test3));
  }

  public void test_updateAnalysis() {
    assertTrue(context.getSourcesNeedingProcessing().isEmpty());
    Source source = new FileBasedSource(createFile("/test.dart"));
    AnalysisDelta delta = new AnalysisDelta();
    delta.setAnalysisLevel(source, AnalysisLevel.ALL);
    context.applyAnalysisDelta(delta);
    assertTrue(context.getSourcesNeedingProcessing().contains(source));
    delta = new AnalysisDelta();
    delta.setAnalysisLevel(source, AnalysisLevel.NONE);
    context.applyAnalysisDelta(delta);
    assertFalse(context.getSourcesNeedingProcessing().contains(source));
  }

  public void xtest_performAnalysisTask_stress() throws Exception {
    int maxCacheSize = 4;
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    options.setCacheSize(maxCacheSize);
    context.setAnalysisOptions(options);
    int sourceCount = maxCacheSize + 2;
    ArrayList<Source> sources = new ArrayList<Source>(sourceCount);
    ChangeSet changeSet = new ChangeSet();
    for (int i = 0; i < sourceCount; i++) {
      Source source = addSource("/lib" + i + ".dart", "library lib" + i + ";");
      sources.add(source);
      changeSet.addedSource(source);
    }
    context.applyChanges(changeSet);
    context.setAnalysisPriorityOrder(sources);
    for (int i = 0; i < 1000; i++) { // (sourceCount * 20)
      ChangeNotice[] notice = context.performAnalysisTask().getChangeNotices();
      if (notice == null) {
        //System.out.println("test_performAnalysisTask_stress: " + i);
        break;
      }
    }
    ChangeNotice[] notice = context.performAnalysisTask().getChangeNotices();
    if (notice != null) {
      fail("performAnalysisTask failed to terminate after analyzing all sources");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    context = null;
    sourceFactory = null;
    super.tearDown();
  }

  private Source addSource(String fileName, String contents) {
    Source source = new FileBasedSource(createFile(fileName));
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);
    context.setContents(source, contents);
    return source;
  }

  private TestSource addSourceWithException(String fileName) {
    return addSourceWithException(fileName, "");
  }

  private TestSource addSourceWithException(String fileName, String contents) {
    TestSource source = new TestSource(createFile(fileName), contents);
    source.setGenerateExceptionOnRead(true);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);
    return source;
  }

  /**
   * Perform analysis tasks up to 512 times and asserts that that was enough.
   */
  private void analyzeAll_assertFinished() {
    analyzeAll_assertFinished(512);
  }

  /**
   * Perform analysis tasks up to the given number of times and asserts that that was enough.
   * 
   * @param maxIterations the maximum number of tasks to perform
   */
  private void analyzeAll_assertFinished(int maxIterations) {
    for (int i = 0; i < maxIterations; i++) {
      ChangeNotice[] notice = context.performAnalysisTask().getChangeNotices();
      if (notice == null) {
        return;
      }
    }
    fail("performAnalysisTask failed to terminate after analyzing all sources");
  }

  private void changeSource(TestSource source, String contents) {
    source.setContents(contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.changedSource(source);
    context.applyChanges(changeSet);
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

  private IncrementalAnalysisCache getIncrementalAnalysisCache(AnalysisContextImpl context2)
      throws Exception {
    Field field = AnalysisContextImpl.class.getDeclaredField("incrementalAnalysisCache");
    field.setAccessible(true);
    return (IncrementalAnalysisCache) field.get(context2);
  }

  private Source[] getPriorityOrder(AnalysisContextImpl context2) throws Exception {
    Field field = AnalysisContextImpl.class.getDeclaredField("priorityOrder");
    field.setAccessible(true);
    return (Source[]) field.get(context2);
  }

  private void removeSource(Source source) {
    ChangeSet changeSet = new ChangeSet();
    changeSet.removedSource(source);
    context.applyChanges(changeSet);
  }

  private void setIncrementalAnalysisCache(AnalysisContextImpl context2,
      IncrementalAnalysisCache incrementalCache) throws Exception {
    Field field = AnalysisContextImpl.class.getDeclaredField("incrementalAnalysisCache");
    field.setAccessible(true);
    field.set(context2, incrementalCache);
  }
}
