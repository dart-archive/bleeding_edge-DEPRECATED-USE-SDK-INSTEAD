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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.List;

/**
 * Instances of the class {@code HtmlWarningCodeTest} test the generation of HTML warning codes.
 */
public class HtmlWarningCodeTest extends EngineTestCase {
  /**
   * The source factory used to create the sources to be resolved.
   */
  private SourceFactory sourceFactory;

  /**
   * The analysis context used to resolve the HTML files.
   */
  private AnalysisContextImpl context;

  /**
   * The contents of the 'test.html' file.
   */
  private String contents;

  /**
   * The list of reported errors.
   */
  private List<AnalysisError> errors;

  public void test_invalidUri() throws Exception {
    verify(createSource(//
        "<html>",
        "<script type='application/dart' src='ht:'/>",
        "</html>"), HtmlWarningCode.INVALID_URI);
    assertErrorLocation(errors.get(0), "ht:");
  }

  public void test_uriDoesNotExist() throws Exception {
    verify(createSource(//
        "<html>",
        "<script type='application/dart' src='other.dart'/>",
        "</html>"), HtmlWarningCode.URI_DOES_NOT_EXIST);
    assertErrorLocation(errors.get(0), "other.dart");
  }

  @Override
  protected void setUp() throws Exception {
    sourceFactory = new SourceFactory(new FileUriResolver());
    context = new AnalysisContextImpl();
    context.setSourceFactory(sourceFactory);
  }

  @Override
  protected void tearDown() throws Exception {
    sourceFactory = null;
    context = null;
    contents = null;
    errors = null;
    super.tearDown();
  }

  private void assertErrorLocation(AnalysisError error, int expectedOffset, int expectedLength) {
    assertEquals(error.toString(), expectedOffset, error.getOffset());
    assertEquals(error.toString(), expectedLength, error.getLength());
  }

  private void assertErrorLocation(AnalysisError error, String expectedString) {
    assertErrorLocation(error, contents.indexOf(expectedString), expectedString.length());
  }

  private void verify(String contents, ErrorCode... expectedErrorCodes) throws Exception {
    this.contents = contents;
    TestSource source = new TestSource(createFile("/test.html"), contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);

    HtmlUnitBuilder builder = new HtmlUnitBuilder(context);
    builder.buildHtmlElement(
        source,
        context.getModificationStamp(source),
        context.parseHtmlUnit(source));

    GatheringErrorListener errorListener = new GatheringErrorListener();
    errorListener.addAll(builder.getErrorListener());
    errorListener.assertErrorsWithCodes(expectedErrorCodes);
    errors = errorListener.getErrors();
  }
}
