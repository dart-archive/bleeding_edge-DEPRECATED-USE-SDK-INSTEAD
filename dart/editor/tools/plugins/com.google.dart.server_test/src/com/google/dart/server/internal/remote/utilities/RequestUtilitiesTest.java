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
package com.google.dart.server.internal.remote.utilities;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.ContentChange;
import com.google.dart.server.ServerService;
import com.google.dart.server.internal.remote.processor.AnalysisErrorImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link RequestUtilities}.
 */
public class RequestUtilitiesTest extends TestCase {

//  public void test_generateContextApplyAnalysisDeltaRequest_emptyAnalysisMap() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.applyAnalysisDelta',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'delta': {}",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateContextApplyAnalysisDeltaRequest(
//        "id",
//        "CONTEXT_ID",
//        new LinkedHashMap<String, AnalysisDelta.AnalysisLevel>());
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextApplyAnalysisDeltaRequest_withAnalysisMap() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.applyAnalysisDelta',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'delta': {",
//        "      'one': 'ALL',",
//        "      'two': 'ERRORS',",
//        "      'three': 'RESOLVED',",
//        "      'four': 'NONE'",
//        "    }",
//        "  }",
//        "}");
//    Map<String, AnalysisDelta.AnalysisLevel> analysisMap = new LinkedHashMap<String, AnalysisDelta.AnalysisLevel>();
//    analysisMap.put("one", AnalysisDelta.AnalysisLevel.ALL);
//    analysisMap.put("two", AnalysisDelta.AnalysisLevel.ERRORS);
//    analysisMap.put("three", AnalysisDelta.AnalysisLevel.RESOLVED);
//    analysisMap.put("four", AnalysisDelta.AnalysisLevel.NONE);
//    JsonElement actual = RequestUtilities.generateContextApplyAnalysisDeltaRequest(
//        "id",
//        "CONTEXT_ID",
//        analysisMap);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextApplySourceDeltaRequest() throws Exception {
//    // TODO(jwren)
//  }
//
//  public void test_generateContextGetFixesRequest() throws Exception {
//    // TODO(jwren)
//  }
//
//  public void test_generateContextGetMinorRefactoringsRequest() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.getMinorRefactorings',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'source': 'source',",
//        "    'offset': 1,",
//        "    'length': 2",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateContextGetMinorRefactoringsRequest(
//        "id",
//        "CONTEXT_ID",
//        "source",
//        1,
//        2);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextSetOptionsRequest_defaultsValues() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.setOptions',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'options': {",
//        "      'analyzeAngular': true,",
//        "      'analyzePolymer': true,",
//        "      'cacheSize': 64,",
//        "      'enableDeferredLoading': true,",
//        "      'generateDart2jsHints': true,",
//        "      'generateHints': true",
//        "    }",
//        "  }",
//        "}");
//    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
//    JsonElement actual = RequestUtilities.generateContextSetOptionsRequest(
//        "id",
//        "CONTEXT_ID",
//        options);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextSetOptionsRequest_notDefaultValues() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.setOptions',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'options': {",
//        "      'analyzeAngular': false,",
//        "      'analyzePolymer': false,",
//        "      'cacheSize': 1,",
//        "      'enableDeferredLoading': false,",
//        "      'generateDart2jsHints': false,",
//        "      'generateHints': false",
//        "    }",
//        "  }",
//        "}");
//    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
//    options.setAnalyzeAngular(false);
//    options.setAnalyzePolymer(false);
//    options.setCacheSize(1);
//    options.setEnableDeferredLoading(false);
//    options.setDart2jsHint(false);
//    options.setHint(false);
//    JsonElement actual = RequestUtilities.generateContextSetOptionsRequest(
//        "id",
//        "CONTEXT_ID",
//        options);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextSetPrioritySourcesRequest() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.setPrioritySources',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'sources': ['1','2','3','4']",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateContextSetPrioritySourcesRequest(
//        "id",
//        "CONTEXT_ID",
//        ImmutableList.of("1", "2", "3", "4"));
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateContextSetPrioritySourcesRequest_emptySources() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'context.setPrioritySources',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'sources': []",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateContextSetPrioritySourcesRequest(
//        "id",
//        "CONTEXT_ID",
//        new ArrayList<String>());
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateServerCreateContextRequest() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'server.createContext',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'sdkDirectory': '/sdk/path'",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateServerCreateContextRequest(
//        "id",
//        "CONTEXT_ID",
//        "/sdk/path",
//        null);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateServerCreateContextRequest_withPackageMap() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'server.createContext',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID',",
//        "    'sdkDirectory': '/SDK/PATH',",
//        "    'packageMap': {",
//        "      'one': ['1'],",
//        "      'two': ['2', '2'],",
//        "      'three': ['3', '3', '3']",
//        "    }",
//        "  }",
//        "}");
//    Map<String, List<String>> packageMap = new LinkedHashMap<String, List<String>>();
//    packageMap.put("one", ImmutableList.of("1"));
//    packageMap.put("two", ImmutableList.of("2", "2"));
//    packageMap.put("three", ImmutableList.of("3", "3", "3"));
//    JsonElement actual = RequestUtilities.generateServerCreateContextRequest(
//        "id",
//        "CONTEXT_ID",
//        "/SDK/PATH",
//        packageMap);
//    assertEquals(expected, actual);
//  }
//
//  public void test_generateServerDeleteContextRequest() throws Exception {
//    JsonElement expected = parseJson(//
//        "{",
//        "  'id': 'id',",
//        "  'method': 'server.deleteContext',",
//        "  'params': {",
//        "    'contextId': 'CONTEXT_ID'",
//        "  }",
//        "}");
//    JsonElement actual = RequestUtilities.generateServerDeleteContextRequest("id", "CONTEXT_ID");
//    assertEquals(expected, actual);
//  }

  public void test_buildJsonElement_map_nullKey() throws Exception {
    try {
      Map<String, String> map = Maps.newHashMap();
      map.put(null, "bar");
      RequestUtilities.buildJsonElement(map);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void test_buildJsonElement_null() throws Exception {
    try {
      RequestUtilities.buildJsonElement(null);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void test_generateAnalysisSetAnalysisRoots() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setAnalysisRoots',",
        "  'params': {",
        "    'included': ['file0', 'file1', 'file2'],",
        "    'excluded': ['file3', 'file4', 'file5']",
        "  }",
        "}");
    assertEquals(
        expected,
        RequestUtilities.generateAnalysisSetAnalysisRoots(
            "0",
            ImmutableList.of("file0", "file1", "file2"),
            ImmutableList.of("file3", "file4", "file5")));
  }

  public void test_generateAnalysisSetAnalysisRoots_emptyLists() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setAnalysisRoots',",
        "  'params': {",
        "    'included': [],",
        "    'excluded': []",
        "  }",
        "}");
    assertEquals(expected, RequestUtilities.generateAnalysisSetAnalysisRoots(
        "0",
        new ArrayList<String>(0),
        new ArrayList<String>(0)));
  }

  public void test_generateAnalysisUpdateContents() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': 'ID',",
        "  'method': 'analysis.updateContent',",
        "  'params': {",
        "    'files': {",
        "      '/fileA.dart': {",
        "        content: 'aaa'",
        "      },",
        "      '/fileB.dart': {",
        "        content: 'bbb',",
        "        offset: 1,",
        "        oldLength: 2,",
        "        newLength: 3",
        "      }",
        "    }",
        "  }",
        "}");
    ArrayList<ServerService> subscriptions = new ArrayList<ServerService>();
    subscriptions.add(ServerService.STATUS);
    Map<String, ContentChange> files = ImmutableMap.of(
        "/fileA.dart",
        new ContentChange("aaa"),
        "/fileB.dart",
        new ContentChange("bbb", 1, 2, 3));
    assertEquals(expected, RequestUtilities.generateAnalysisUpdateContent("ID", files));
  }

  public void test_generateEditGetAssists() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getAssists',",
        "  'params': {",
        "    'file': 'file0',",
        "    'offset': 1,",
        "    'length': 2",
        "  }",
        "}");
    assertEquals(expected, RequestUtilities.generateEditGetAssists("0", "file0", 1, 2));
  }

  public void test_generateEditGetFixes_noCorrection() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getFixes',",
        "  'params': {",
        "    'errors': [",
        "      {",
        "        'file': 'file0',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'msg'",
        "      }",
        "    ]",
        "  }",
        "}");
    List<AnalysisError> errors = ImmutableList.of((AnalysisError) new AnalysisErrorImpl(
        "file0",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        1,
        2,
        "msg",
        null));
    assertEquals(expected, RequestUtilities.generateEditGetFixes("0", errors));
  }

  public void test_generateEditGetFixes_withCorrection() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getFixes',",
        "  'params': {",
        "    'errors': [",
        "      {",
        "        'file': 'file0',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'msg',",
        "        'correction': 'correction'",
        "      }",
        "    ]",
        "  }",
        "}");
    List<AnalysisError> errors = ImmutableList.of((AnalysisError) new AnalysisErrorImpl(
        "file0",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        1,
        2,
        "msg",
        "correction"));
    assertEquals(expected, RequestUtilities.generateEditGetFixes("0", errors));
  }

  public void test_generateServerGetVersionRequest() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '',",
        "  'method': 'server.getVersion'",
        "}");
    assertEquals(expected, RequestUtilities.generateServerGetVersion(""));
  }

  public void test_generateServerGetVersionRequest_withId() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': 'ID',",
        "  'method': 'server.getVersion'",
        "}");
    assertEquals(expected, RequestUtilities.generateServerGetVersion("ID"));
  }

  public void test_generateServerSetSubscriptions() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': 'ID',",
        "  'method': 'server.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': []",
        "  }",
        "}");
    assertEquals(
        expected,
        RequestUtilities.generateServerSetSubscriptions("ID", new ArrayList<ServerService>(0)));
  }

  public void test_generateServerSetSubscriptions_status() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': 'ID',",
        "  'method': 'server.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': [STATUS]",
        "  }",
        "}");
    ArrayList<ServerService> subscriptions = new ArrayList<ServerService>();
    subscriptions.add(ServerService.STATUS);
    assertEquals(expected, RequestUtilities.generateServerSetSubscriptions("ID", subscriptions));
  }

  public void test_generateServerShutdownRequest() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': '',",
        "  'method': 'server.shutdown'",
        "}");
    assertEquals(expected, RequestUtilities.generateServerShutdown(""));
  }

  public void test_generateServerShutdownRequest_withId() throws Exception {
    JsonElement expected = parseJson(//
        "{",
        "  'id': 'ID',",
        "  'method': 'server.shutdown'",
        "}");
    assertEquals(expected, RequestUtilities.generateServerShutdown("ID"));
  }

  /**
   * Builds a JSON string from the given lines. Replaces single quotes with double quotes. Then
   * parses this string as a {@link JsonElement}.
   */
  private JsonElement parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return new JsonParser().parse(json);
  }
}
