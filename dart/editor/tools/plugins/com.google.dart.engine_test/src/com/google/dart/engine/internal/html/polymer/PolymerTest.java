/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.html.polymer;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.Source;

import junit.framework.AssertionFailedError;

import static org.fest.assertions.Assertions.assertThat;

abstract public class PolymerTest extends EngineTestCase {
  /**
   * @return the offset of given <code>search</code> string in <code>contents</code>. Fails test if
   *         not found.
   */
  protected static int findOffset(String contents, String search) {
    int offset = contents.indexOf(search);
    assertThat(offset).describedAs(contents).isNotEqualTo(-1);
    return offset;
  }

  protected AnalysisContextHelper contextHelper = new AnalysisContextHelper();
  protected AnalysisContext context;

  protected Source tagDartSource;
  protected String tagDartContents;
  protected Source tagHtmlSource;
  protected String tagHtmlContents;
  protected CompilationUnitElement tagDartUnitElement;
  protected HtmlElement tagHtmlUnitElement;
  protected PolymerTagDartElement tagDartElement;
  protected PolymerTagHtmlElement tagHtmlElement;

  protected final void addTagDartSource(String contents) {
    tagDartContents = contents;
    tagDartSource = contextHelper.addSource("/my-element.dart", contents);
  }

  protected final void addTagHtmlSource(String contents) {
    tagHtmlContents = contents;
    tagHtmlSource = contextHelper.addSource("/my-element.html", contents);
  }

  /**
   * Assert that the number of errors reported against the given source matches the number of errors
   * that are given and that they have the expected error codes. The order in which the errors were
   * gathered is ignored.
   * 
   * @param source the source against which the errors should have been reported
   * @param expectedErrorCodes the error codes of the errors that should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if a different number of errors have been reported than were
   *           expected
   */
  protected final void assertErrors(Source source, ErrorCode... expectedErrorCodes) {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    AnalysisErrorInfo errorsInfo = context.getErrors(source);
    for (AnalysisError error : errorsInfo.getErrors()) {
      errorListener.onError(error);
    }
    errorListener.assertErrorsWithCodes(expectedErrorCodes);
  }

  protected final void assertNoErrorsTag() {
    assertNoErrorsTagDart();
    assertNoErrorsTagHtml();
  }

  protected final void assertNoErrorsTagDart() {
    assertErrors(tagDartSource);
  }

  protected final void assertNoErrorsTagHtml() {
    assertErrors(tagHtmlSource);
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #tagDartContents}. Fails test
   *         if not found.
   */
  protected final int findTagDartOffset(String search) {
    return findOffset(tagDartContents, search);
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #tagHtmlContents}. Fails test
   *         if not found.
   */
  protected final int findTagHtmlOffset(String search) {
    return findOffset(tagHtmlContents, search);
  }

  protected final void resolveTagDart() throws Exception {
    contextHelper.runTasks();
    tagDartUnitElement = contextHelper.getDefiningUnitElement(tagDartSource);
    // try to find a PolymerTagDartElement
    for (ClassElement classElement : tagDartUnitElement.getTypes()) {
      for (ToolkitObjectElement toolkitObject : classElement.getToolkitObjects()) {
        if (toolkitObject instanceof PolymerTagDartElement) {
          tagDartElement = (PolymerTagDartElement) toolkitObject;
        }
      }
    }
  }

  protected final void resolveTagHtml() throws Exception {
    contextHelper.runTasks();
    tagHtmlUnitElement = context.getHtmlElement(tagHtmlSource);
    // try to find a PolymerTagHtmlElement
    PolymerTagHtmlElement[] polymerTags = tagHtmlUnitElement.getPolymerTags();
    if (polymerTags.length != 0) {
      tagHtmlElement = polymerTags[0];
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    configureForPolymer(contextHelper);
    context = contextHelper.context;
  }

  @Override
  protected void tearDown() throws Exception {
    contextHelper = null;
    context = null;
    tagDartSource = null;
    tagDartContents = null;
    tagHtmlSource = null;
    tagHtmlContents = null;
    tagDartUnitElement = null;
    tagHtmlUnitElement = null;
    tagDartElement = null;
    tagHtmlElement = null;
    super.tearDown();
  }

  private void configureForPolymer(AnalysisContextHelper contextHelper) {
    contextHelper.addSource(
        "/polymer.dart",
        createSource(
            "library polymer;",
            "",
            "class CustomTag {",
            "  final String tagName;",
            "  const CustomTag(this.tagName);",
            "}",
            "",
            "class ObservableProperty {",
            "  const ObservableProperty();",
            "}",
            "const ObservableProperty observable = const ObservableProperty();",
            "",
            "class PublishedProperty extends ObservableProperty {",
            "  const PublishedProperty();",
            "}",
            "const published = const PublishedProperty();",
            ""));
  }
}
