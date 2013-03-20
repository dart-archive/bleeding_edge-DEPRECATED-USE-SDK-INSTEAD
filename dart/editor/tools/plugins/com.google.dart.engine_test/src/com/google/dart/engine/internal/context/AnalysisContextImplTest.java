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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class AnalysisContextImplTest extends EngineTestCase {
  public void fail_getElement_location() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    ElementLocation location = new ElementLocationImpl("dart:core;Object");
    Element element = context.getElement(location);
    assertNotNull(element);
    assertEquals(location, element.getLocation());
  }

  public void fail_getKindOf_unknown() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    Source source = new TestSource();
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    assertSame(SourceKind.UNKNOWN, context.getKindOf(source));
  }

  public void fail_parseCompilationUnit_nonExistentSource() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory(new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/does/not/exist.dart"));
    CompilationUnit unit = context.parseCompilationUnit(source);
    assertNotNull(unit);
  }

  public void test_applyChanges_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.applyChanges(new ChangeSet());
    // TODO(brianwilkerson) Test that there are no tasks waiting to be performed.
  }

  public void test_computeErrors_some() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source, "library 'lib';");
    context.applyChanges(changeSet);
    AnalysisError[] errors = context.computeErrors(source);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void test_creation() {
    assertNotNull(new AnalysisContextImpl());
  }

  public void test_getErrors_none() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source, "library lib;");
    context.applyChanges(changeSet);
    AnalysisError[] errors = context.getErrors(source);
    assertNotNull(errors);
    assertLength(0, errors);
  }

  public void test_getHtmlSources_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    Source[] sources = context.getHtmlSources();
    assertLength(0, sources);
  }

  public void test_getLaunchableClientLibrarySources_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    Source[] sources = context.getLaunchableClientLibrarySources();
    assertLength(0, sources);
  }

  public void test_getLaunchableServerLibrarySources_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    Source[] sources = context.getLaunchableServerLibrarySources();
    assertLength(0, sources);
  }

  public void test_getLibrarySources_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    Source[] sources = context.getLibrarySources();
    assertLength(0, sources);
  }

  public void test_parse_no_errors() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new TestSource(sourceFactory, createFile("/lib.dart"), "library lib;");
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertLength(0, compilationUnit.getParsingErrors());
    // TODO (danrubel): assert no semantic errors
//    assertEquals(null, compilationUnit.getSemanticErrors());
//    assertEquals(null, compilationUnit.getErrors());
  }

  public void test_parse_with_errors() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new TestSource(sourceFactory, createFile("/lib.dart"), "library {");
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    CompilationUnit compilationUnit = context.parseCompilationUnit(source);
    assertTrue("Expected syntax errors", compilationUnit.getParsingErrors().length > 0);
    // TODO (danrubel): assert no semantic errors
//  assertEquals(null, compilationUnit.getSemanticErrors());
//  assertEquals(null, compilationUnit.getErrors());
  }

  public void test_parseHtmlUnit_no_errors() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    String content = createSource("<!DOCTYPE html/>", "<html><h1>Foo</h1><p>bar</p></html>");
    Source source = new TestSource(sourceFactory, createFile("/lib.html"), content);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    HtmlUnit unit = context.parseHtmlUnit(source);
    assertNotNull(unit);
    assertEquals("h1", unit.getTagNodes().get(0).getTagNodes().get(0).getTag().getLexeme());
  }

  public void test_resolveCompilationUnit() throws Exception {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    SourceFactory sourceFactory = context.getSourceFactory();
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source, "library lib;");
    context.applyChanges(changeSet);
    CompilationUnit compilationUnit = context.resolveCompilationUnit(source, source);
    assertNotNull(compilationUnit);
    assertLength(0, compilationUnit.getParsingErrors());
    assertLength(0, compilationUnit.getResolutionErrors());
    assertLength(0, compilationUnit.getErrors());
  }

  public void test_setSourceFactory() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    assertEquals(sourceFactory, context.getSourceFactory());
  }
}
