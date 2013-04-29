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
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

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

  public void test_invalidUri() throws Exception {
    verify(createSource(//
        "<html>",
        "<script type='application/dart' src='ht:'/>",
        "</html>"), HtmlWarningCode.INVALID_URI);
  }

  public void test_uriDoesNotExist() throws Exception {
    verify(createSource(//
        "<html>",
        "<script type='application/dart' src='other.dart'/>",
        "</html>"), HtmlWarningCode.URI_DOES_NOT_EXIST);
  }

  @Override
  protected void setUp() throws Exception {
    sourceFactory = new SourceFactory(new FileUriResolver());
    context = new AnalysisContextImpl();
    context.setSourceFactory(sourceFactory);
  }

  private HtmlElementImpl verify(String contents, ErrorCode... expectedErrorCodes) throws Exception {
    TestSource source = new TestSource(
        sourceFactory.getContentCache(),
        createFile("/test.html"),
        contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);

    GatheringErrorListener errorListener = new GatheringErrorListener();
    HtmlUnitBuilder builder = new HtmlUnitBuilder(context, errorListener);
    HtmlElementImpl element = builder.buildHtmlElement(source);
    errorListener.assertErrors(expectedErrorCodes);
    return element;
  }
}
