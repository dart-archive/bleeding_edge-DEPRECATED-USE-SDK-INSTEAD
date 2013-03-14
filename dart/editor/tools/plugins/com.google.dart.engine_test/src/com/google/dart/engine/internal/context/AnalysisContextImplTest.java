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
import com.google.dart.engine.context.ChangeResult;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.File;

public class AnalysisContextImplTest extends EngineTestCase {
  public void fail_getElement_location() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    ElementLocation location = new ElementLocationImpl("dart:core;Object");
    Element element = context.getElement(location);
    assertNotNull(element);
    assertEquals(location, element.getLocation());
  }

  public void fail_getErrors_none() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    sourceFactory.setContents(source, "library lib;");
    AnalysisError[] errors = context.getErrors(source);
    assertNotNull(errors);
    assertLength(0, errors);
  }

  public void fail_getErrors_some() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    sourceFactory.setContents(source, "library lib;");
    AnalysisError[] errors = context.getErrors(source);
    assertNotNull(errors);
    assertTrue(errors.length > 0);
  }

  public void fail_getKnownKindOf_unknown() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    assertSame(SourceKind.UNKNOWN, context.getKnownKindOf(new TestSource()));
  }

  public void fail_parse_non_existent_source() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory(new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, new File("/does/not/exist.dart"));
    CompilationUnit unit = context.parse(source);
    assertNotNull(unit);
  }

  public void test_applyChanges_empty() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    ChangeResult result = context.applyChanges(new ChangeSet());
    assertNotNull(result);
  }

  public void test_creation() {
    assertNotNull(new AnalysisContextImpl());
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
    CompilationUnit compilationUnit = context.parse(source);
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
    CompilationUnit compilationUnit = context.parse(source);
    assertTrue("Expected syntax errors", compilationUnit.getParsingErrors().length > 0);
    // TODO (danrubel): assert no semantic errors
//  assertEquals(null, compilationUnit.getSemanticErrors());
//  assertEquals(null, compilationUnit.getErrors());
  }

  public void test_parse_with_listener() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    sourceFactory.setContents(source, "library lib;");
    GatheringErrorListener listener = new GatheringErrorListener();
    CompilationUnit compilationUnit = context.parse(source, listener);
    assertNotNull(compilationUnit);
  }

  public void test_parseHtml_no_errors() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    String content = createSource("<!DOCTYPE html/>", "<html><h1>Foo</h1><p>bar</p></html>");
    Source source = new TestSource(sourceFactory, createFile("/lib.html"), content);
    HtmlParseResult result = context.parseHtml(source);
    assertNotNull(result);
    HtmlUnit unit = result.getHtmlUnit();
    assertNotNull(unit);
    assertEquals("h1", unit.getTagNodes().get(0).getTagNodes().get(0).getTag().getLexeme());
    assertEquals(3, result.getLineStarts().length);
    assertNotNull(result.getToken());
  }

  public void test_resolve() throws Exception {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    SourceFactory sourceFactory = context.getSourceFactory();
    Source source = new FileBasedSource(sourceFactory, createFile("/lib.dart"));
    sourceFactory.setContents(source, "library lib;");
    CompilationUnit compilationUnit = context.resolve(source, null);
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
