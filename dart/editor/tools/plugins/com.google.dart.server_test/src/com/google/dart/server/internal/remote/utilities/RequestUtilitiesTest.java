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

import com.google.common.collect.ImmutableList;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link RequestUtilities}.
 */
public class RequestUtilitiesTest extends TestCase {

  public void test_generateContextApplyAnalysisDeltaRequest_emptyAnalysisMap() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"context.applyAnalysisDelta\",\"params\":{\"contextId\":\"CONTEXT_ID\",\"delta\":{}}}");
    JsonElement actual = RequestUtilities.generateContextApplyAnalysisDeltaRequest(
        "id",
        "CONTEXT_ID",
        new LinkedHashMap<String, AnalysisDelta.AnalysisLevel>());
    assertEquals(expected, actual);
  }

  public void test_generateContextApplyAnalysisDeltaRequest_withAnalysisMap() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"context.applyAnalysisDelta\",\"params\":{\"contextId\":\"CONTEXT_ID\",\"delta\":{\"one\":\"ALL\",\"two\":\"ERRORS\",\"three\":\"RESOLVED\",\"four\":\"NONE\"}}}");
    Map<String, AnalysisDelta.AnalysisLevel> analysisMap = new LinkedHashMap<String, AnalysisDelta.AnalysisLevel>();
    analysisMap.put("one", AnalysisDelta.AnalysisLevel.ALL);
    analysisMap.put("two", AnalysisDelta.AnalysisLevel.ERRORS);
    analysisMap.put("three", AnalysisDelta.AnalysisLevel.RESOLVED);
    analysisMap.put("four", AnalysisDelta.AnalysisLevel.NONE);
    JsonElement actual = RequestUtilities.generateContextApplyAnalysisDeltaRequest(
        "id",
        "CONTEXT_ID",
        analysisMap);
    assertEquals(expected, actual);
  }

  public void test_generateContextApplySourceDeltaRequest() throws Exception {
    // TODO(jwren)
  }

  public void test_generateContextGetFixesRequest() throws Exception {
    // TODO(jwren)
  }

  public void test_generateContextGetMinorRefactoringsRequest() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"context.getMinorRefactorings\",\"params\":{\"contextId\":\"CONTEXT_ID\",\"source\":\"source\",\"offset\":1,\"length\":2}}");
    JsonElement actual = RequestUtilities.generateContextGetMinorRefactoringsRequest(
        "id",
        "CONTEXT_ID",
        "source",
        1,
        2);
    assertEquals(expected, actual);
  }

  public void test_generateServerCreateContextRequest() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"server.createContext\",\"params\":{\"contextId\":\"CONTEXT_ID\",\"sdkDirectory\":\"/sdk/path\"}}");
    JsonElement actual = RequestUtilities.generateServerCreateContextRequest(
        "id",
        "CONTEXT_ID",
        "/sdk/path",
        null);
    assertEquals(expected, actual);
  }

  public void test_generateServerCreateContextRequest_withPackageMap() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"server.createContext\","
        + "\"params\":{\"contextId\":\"CONTEXT_ID\",\"sdkDirectory\":\"/SDK/PATH\","
        + "\"packageMap\":{\"one\":[\"1\"],\"two\":[\"2\",\"2\"],\"three\":[\"3\",\"3\",\"3\"]}}}");
    Map<String, List<String>> packageMap = new LinkedHashMap<String, List<String>>();
    packageMap.put("one", ImmutableList.of("1"));
    packageMap.put("two", ImmutableList.of("2", "2"));
    packageMap.put("three", ImmutableList.of("3", "3", "3"));
    JsonElement actual = RequestUtilities.generateServerCreateContextRequest(
        "id",
        "CONTEXT_ID",
        "/SDK/PATH",
        packageMap);
    assertEquals(expected, actual);
  }

  public void test_generateServerDeleteContextRequest() throws Exception {
    JsonElement expected = new JsonParser().parse("{\"id\":\"id\",\"method\":\"server.deleteContext\",\"params\":{\"contextId\":\"CONTEXT_ID\"}}");
    JsonElement actual = RequestUtilities.generateServerDeleteContextRequest("id", "CONTEXT_ID");
    assertEquals(expected, actual);
  }

  public void test_generateServerShutdownRequest() throws Exception {
    assertEquals(
        new JsonParser().parse("{\"id\":\"\",\"method\":\"server.shutdown\"}"),
        RequestUtilities.generateServerShutdownRequest(""));
  }

  public void test_generateServerShutdownRequest_withId() throws Exception {
    assertEquals(
        new JsonParser().parse("{\"id\":\"ID\",\"method\":\"server.shutdown\"}"),
        RequestUtilities.generateServerShutdownRequest("ID"));
  }

  public void test_generateServerVersionRequest() throws Exception {
    assertEquals(
        new JsonParser().parse("{\"id\":\"\",\"method\":\"server.version\"}"),
        RequestUtilities.generateServerVersionRequest(""));
  }

  public void test_generateServerVersionRequest_withId() throws Exception {
    assertEquals(
        new JsonParser().parse("{\"id\":\"ID\",\"method\":\"server.version\"}"),
        RequestUtilities.generateServerVersionRequest("ID"));
  }
}
