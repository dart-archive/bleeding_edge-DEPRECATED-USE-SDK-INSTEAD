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

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link AnalysisServerRequestUtilities}.
 */
public class AnalysisServerRequestUtilitiesTest extends TestCase {

  public void test_generateCreateContextRequest() throws Exception {
    // {"id":"id","method":"server.createContext",
    //  "params":{"contextId":"CONTEXT_ID","sdkDirectory":"/sdk/path"}}
    assertEquals(
        "{\"id\":\"id\",\"method\":\"server.createContext\",\"params\":{\"contextId\":\"CONTEXT_ID\",\"sdkDirectory\":\"/sdk/path\"}}",
        AnalysisServerRequestUtilities.generateCreateContextRequest(
            "id",
            "CONTEXT_ID",
            "/sdk/path",
            null));
  }

  public void test_generateCreateContextRequest_withPackageMap() throws Exception {
    // {"id":"id","method":"server.createContext",
    //  "params":{"contextId":"CONTEXT_ID","sdkDirectory":"/sdk/path",
    //  "packageMap":{"one":["1"],"two":["2","2"],"three":["3","3","3"]}}}
    Map<String, List<String>> packageMap = new LinkedHashMap<String, List<String>>();
    packageMap.put("one", ImmutableList.of("1"));
    packageMap.put("two", ImmutableList.of("2", "2"));
    packageMap.put("three", ImmutableList.of("3", "3", "3"));
    assertEquals(
        "{\"id\":\"id\",\"method\":\"server.createContext\","
            + "\"params\":{\"contextId\":\"CONTEXT_ID\",\"sdkDirectory\":\"/SDK/PATH\","
            + "\"packageMap\":{\"one\":[\"1\"],\"two\":[\"2\",\"2\"],\"three\":[\"3\",\"3\",\"3\"]}}}",
        AnalysisServerRequestUtilities.generateCreateContextRequest(
            "id",
            "CONTEXT_ID",
            "/SDK/PATH",
            packageMap));
  }

  public void test_generateVersionRequest() throws Exception {
    // {"id":"","method":"server.version"}
    assertEquals(
        "{\"id\":\"\",\"method\":\"server.version\"}",
        AnalysisServerRequestUtilities.generateVersionRequest(""));
  }

  public void test_generateVersionRequest_withId() throws Exception {
    // {"id":"ID","method":"server.version"}
    assertEquals(
        "{\"id\":\"ID\",\"method\":\"server.version\"}",
        AnalysisServerRequestUtilities.generateVersionRequest("ID"));
  }
}
