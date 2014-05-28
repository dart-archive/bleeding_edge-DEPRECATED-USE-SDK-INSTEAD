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
package com.google.dart.server.internal.remote.processor;

import com.google.common.base.Joiner;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.error.PolymerCode;
import com.google.dart.engine.error.PubSuggestionCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.error.TodoCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.scanner.ScannerErrorCode;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.internal.shared.TestAnalysisServerListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

public class NotificationErrorsProcessorTest extends TestCase {
  private TestAnalysisServerListener listener = new TestAnalysisServerListener();
  private NotificationErrorsProcessor processor = new NotificationErrorsProcessor(listener);

  public void test_getErrorCode() throws Exception {
    assertSame(AngularCode.MISSING_NAME, getErrorCode("AngularCode.MISSING_NAME"));
    assertSame(
        CompileTimeErrorCode.AMBIGUOUS_EXPORT,
        getErrorCode("CompileTimeErrorCode.AMBIGUOUS_EXPORT"));
    assertSame(HintCode.DEAD_CODE, getErrorCode("HintCode.DEAD_CODE"));
    assertSame(HtmlWarningCode.INVALID_URI, getErrorCode("HtmlWarningCode.INVALID_URI"));
    assertSame(
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        getErrorCode("ParserErrorCode.ABSTRACT_CLASS_MEMBER"));
    assertSame(PolymerCode.MISSING_TAG_NAME, getErrorCode("PolymerCode.MISSING_TAG_NAME"));
    assertSame(
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT,
        getErrorCode("PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT"));
    assertSame(
        ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER,
        getErrorCode("ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER"));
    assertSame(
        ScannerErrorCode.ILLEGAL_CHARACTER,
        getErrorCode("ScannerErrorCode.ILLEGAL_CHARACTER"));
    assertSame(
        StaticTypeWarningCode.INVALID_ASSIGNMENT,
        getErrorCode("StaticTypeWarningCode.INVALID_ASSIGNMENT"));
    assertSame(
        StaticWarningCode.AMBIGUOUS_IMPORT,
        getErrorCode("StaticWarningCode.AMBIGUOUS_IMPORT"));
    assertSame(TodoCode.TODO, getErrorCode("TodoCode.TODO"));
  }

  public void test_getErrorCode_unknown() throws Exception {
    assertSame(null, getErrorCode("NoSuch.ERROR"));
  }

  public void test_OK() throws Exception {
    processor.process(parseJson(//
        "{",
        "  'event': 'analysis.errors',",
        "  'params': {",
        "    'file': '/my/file.dart',",
        "    'errors' : [",
        "      {",
        "        'file': '/the/same/file.dart',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'message A',",
        "        'correction': 'correction A'",
        "      },",
        "      {",
        "        'file': '/the/same/file.dart',",
        "        'errorCode': 'CompileTimeErrorCode.AMBIGUOUS_EXPORT',",
        "        'offset': 10,",
        "        'length': 20,",
        "        'message': 'message B',",
        "        'correction': 'correction B'",
        "      }",
        "    ]",
        "  }",
        "}"));
    AnalysisError[] errors = listener.getErrors("/my/file.dart");
    assertThat(errors).hasSize(2);
    {
      AnalysisError error = errors[0];
      assertEquals("/my/file.dart", error.getFile());
      assertSame(ParserErrorCode.ABSTRACT_CLASS_MEMBER, error.getErrorCode());
      assertEquals(1, error.getOffset());
      assertEquals(2, error.getLength());
      assertEquals("message A", error.getMessage());
      assertEquals("correction A", error.getCorrection());
    }
    {
      AnalysisError error = errors[1];
      assertEquals("/my/file.dart", error.getFile());
      assertSame(CompileTimeErrorCode.AMBIGUOUS_EXPORT, error.getErrorCode());
      assertEquals(10, error.getOffset());
      assertEquals(20, error.getLength());
      assertEquals("message B", error.getMessage());
      assertEquals("correction B", error.getCorrection());
    }
    // check again using "assert"
    listener.assertErrorsWithCodes(
        "/my/file.dart",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        CompileTimeErrorCode.AMBIGUOUS_EXPORT);
  }

  private ErrorCode getErrorCode(String name) {
    return NotificationErrorsProcessor.getErrorCode(name);
  }

  /**
   * Builds a {@link JsonObject} from the given lines.
   */
  private JsonObject parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return new JsonParser().parse(json).getAsJsonObject();
  }
}
