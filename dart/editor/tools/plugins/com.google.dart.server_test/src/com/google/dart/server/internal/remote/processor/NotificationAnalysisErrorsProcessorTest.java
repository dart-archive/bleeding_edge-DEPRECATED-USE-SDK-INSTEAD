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

import com.google.dart.server.error.AngularCode;
import com.google.dart.server.error.CompileTimeErrorCode;
import com.google.dart.server.error.HintCode;
import com.google.dart.server.error.HtmlWarningCode;
import com.google.dart.server.error.ParserErrorCode;
import com.google.dart.server.error.PolymerCode;
import com.google.dart.server.error.PubSuggestionCode;
import com.google.dart.server.error.ResolverErrorCode;
import com.google.dart.server.error.ScannerErrorCode;
import com.google.dart.server.error.StaticTypeWarningCode;
import com.google.dart.server.error.StaticWarningCode;
import com.google.dart.server.error.TodoCode;

import static com.google.dart.server.internal.remote.processor.NotificationAnalysisErrorsProcessor.getErrorCode;

import junit.framework.TestCase;

public class NotificationAnalysisErrorsProcessorTest extends TestCase {

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

}
