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
package com.google.dart.server.internal.remote;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.AnalysisStatus;
import com.google.dart.server.AssistsConsumer;
import com.google.dart.server.ContentChange;
import com.google.dart.server.ElementKind;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.HighlightType;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.NavigationTarget;
import com.google.dart.server.Outline;
import com.google.dart.server.ServerService;
import com.google.dart.server.ServerStatus;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.integration.RemoteAnalysisServerImplIntegrationTest;
import com.google.dart.server.internal.remote.processor.AnalysisErrorImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link RemoteAnalysisServerImpl}, for integration tests which actually uses the
 * remote server, see {@link RemoteAnalysisServerImplIntegrationTest}.
 */
public class RemoteAnalysisServerImplTest extends AbstractRemoteServerTest {

  public void test_analysis_notification_errors() throws Exception {
    putResponse(//
        "{",
        "  'event': 'analysis.errors',",
        "  'params': {",
        "    'file': '/test.dart',",
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
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    listener.assertErrorsWithCodes(
        "/test.dart",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        CompileTimeErrorCode.AMBIGUOUS_EXPORT);
  }

  public void test_analysis_notification_highlights() throws Exception {
    putResponse(//
        "{",
        "  'event': 'analysis.highlights',",
        "  'params': {",
        "    'file': '/test.dart',",
        "    'regions' : [",
        "      {",
        "        'type': 'CLASS',",
        "        'offset': 1,",
        "        'length': 2",
        "      },",
        "      {",
        "        'type': 'FIELD',",
        "        'offset': 10,",
        "        'length': 20",
        "      }",
        "    ]",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    HighlightRegion[] regions = listener.getHighlightRegions("/test.dart");
    assertThat(regions).hasSize(2);
    {
      HighlightRegion error = regions[0];
      assertSame(HighlightType.CLASS, error.getType());
      assertEquals(1, error.getOffset());
      assertEquals(2, error.getLength());
    }
    {
      HighlightRegion error = regions[1];
      assertSame(HighlightType.FIELD, error.getType());
      assertEquals(10, error.getOffset());
      assertEquals(20, error.getLength());
    }
  }

  public void test_analysis_notification_navigation() throws Exception {
    putResponse(//
        "{",
        "  'event': 'analysis.navigation',",
        "  'params': {",
        "    'file': '/test.dart',",
        "    'regions' : [",
        "      {",
        "        'offset': 1,",
        "        'length': 2,",
        "        'targets': [",
        "          {",
        "            'file': '/test2.dart',",
        "            'offset': 3,",
        "            'length': 4,",
        "            'elementId': 'elementId0'",
        "          },",
        "          {",
        "            'file': '/test3.dart',",
        "            'offset': 5,",
        "            'length': 6,",
        "            'elementId': 'elementId1'",
        "          }",
        "        ]",
        "      },",
        "      {",
        "        'offset': 10,",
        "        'length': 20,",
        "        'targets': [",
        "          {",
        "            'file': '/test4.dart',",
        "            'offset': 30,",
        "            'length': 40,",
        "            'elementId': 'elementId2'",
        "          }",
        "        ]",
        "      }",
        "    ]",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    NavigationRegion[] regions = listener.getNavigationRegions("/test.dart");
    assertThat(regions).hasSize(2);
    {
      NavigationRegion region = regions[0];
      assertEquals(1, region.getOffset());
      assertEquals(2, region.getLength());
      NavigationTarget[] targets = region.getTargets();
      assertThat(targets).hasSize(2);
      {
        NavigationTarget target = targets[0];
        assertEquals("/test2.dart", target.getFile());
        assertEquals(3, target.getOffset());
        assertEquals(4, target.getLength());
        assertEquals("elementId0", target.getElementId());
      }
      {
        NavigationTarget target = targets[1];
        assertEquals("/test3.dart", target.getFile());
        assertEquals(5, target.getOffset());
        assertEquals(6, target.getLength());
        assertEquals("elementId1", target.getElementId());
      }
    }
    {
      NavigationRegion region = regions[1];
      assertEquals(10, region.getOffset());
      assertEquals(20, region.getLength());
      NavigationTarget[] targets = region.getTargets();
      assertThat(targets).hasSize(1);
      {
        NavigationTarget target = targets[0];
        assertEquals("/test4.dart", target.getFile());
        assertEquals(30, target.getOffset());
        assertEquals(40, target.getLength());
        assertEquals("elementId2", target.getElementId());
      }
    }
  }

  public void test_analysis_notification_outline() throws Exception {
    putResponse(//
        "{",
        "  'event': 'analysis.outline',",
        "  'params': {",
        "    'file': '/test.dart',",
        "    'outline' : {",
        "      'kind': 'COMPILATION_UNIT',",
        "      'name': 'name0',",
        "      'nameOffset': 1,",
        "      'nameLength': 2,",
        "      'elementOffset': 3,",
        "      'elementLength': 4,",
        "      'isAbstract': false,",
        "      'isStatic': false,",
        "      'parameters': 'parameters0',",
        "      'returnType': 'returnType0',",
        "      'children': [",
        "        {",
        "          'kind': 'CLASS',",
        "          'name': '_name1',",
        "          'nameOffset': 10,",
        "          'nameLength': 20,",
        "          'elementOffset': 30,",
        "          'elementLength': 40,",
        "          'isAbstract': true,",
        "          'isStatic': true",
        "        }",
        "      ]",
        "    }",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    Outline outline = listener.getOutline("/test.dart");
    // assertions on outline
    assertThat(outline.getChildren()).hasSize(1);
    assertEquals(ElementKind.COMPILATION_UNIT, outline.getKind());
    assertEquals("name0", outline.getName());
    assertEquals(1, outline.getNameOffset());
    assertEquals(2, outline.getNameLength());
    assertEquals(3, outline.getElementOffset());
    assertEquals(4, outline.getElementLength());
    assertFalse(outline.isAbstract());
    assertFalse(outline.isStatic());
    assertFalse(outline.isPrivate());
    assertEquals("parameters0", outline.getParameters());
    assertEquals("returnType0", outline.getReturnType());

    // assertions on child
    Outline child = outline.getChildren()[0];
    assertThat(child.getChildren()).hasSize(0);
    assertEquals(ElementKind.CLASS, child.getKind());
    assertEquals("_name1", child.getName());
    assertEquals(10, child.getNameOffset());
    assertEquals(20, child.getNameLength());
    assertEquals(30, child.getElementOffset());
    assertEquals(40, child.getElementLength());
    assertTrue(child.isAbstract());
    assertTrue(child.isStatic());
    assertTrue(child.isPrivate());
    assertNull(child.getParameters());
    assertNull(child.getReturnType());
  }

  public void test_analysis_setAnalysisRoots() throws Exception {
    server.setAnalysisRoots(
        ImmutableList.of("/fileA.dart", "/fileB.dart"),
        ImmutableList.of("/fileC.dart", "/fileD.dart"));
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setAnalysisRoots',",
        "  'params': {",
        "    'included': ['/fileA.dart', '/fileB.dart'],",
        "    'excluded': ['/fileC.dart', '/fileD.dart']",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_setAnalysisRoots_emptyLists() throws Exception {
    server.setAnalysisRoots(new ArrayList<String>(0), new ArrayList<String>(0));
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setAnalysisRoots',",
        "  'params': {",
        "    'included': [],",
        "    'excluded': []",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_setPriorityFiles() throws Exception {
    server.setPriorityFiles(ImmutableList.of("/fileA.dart", "/fileB.dart"));
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setPriorityFiles',",
        "  'params': {",
        "    'files': ['/fileA.dart', '/fileB.dart']",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_setSubscriptions() throws Exception {
    LinkedHashMap<AnalysisService, List<String>> subscriptions = new LinkedHashMap<AnalysisService, List<String>>();
    subscriptions.put(AnalysisService.ERRORS, new ArrayList<String>(0));
    subscriptions.put(AnalysisService.HIGHLIGHTS, ImmutableList.of("/fileA.dart"));
    subscriptions.put(AnalysisService.NAVIGATION, ImmutableList.of("/fileB.dart", "/fileC.dart"));
    subscriptions.put(
        AnalysisService.OUTLINE,
        ImmutableList.of("/fileD.dart", "/fileE.dart", "/fileF.dart"));

    server.setAnalysisSubscriptions(subscriptions);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': {",
        "      ERRORS: [],",
        "      HIGHLIGHTS: ['/fileA.dart'],",
        "      NAVIGATION: ['/fileB.dart', '/fileC.dart'],",
        "      OUTLINE: ['/fileD.dart', '/fileE.dart', '/fileF.dart']",
        "    }",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_setSubscriptions_emptyMap() throws Exception {
    server.setAnalysisSubscriptions(new HashMap<AnalysisService, List<String>>(0));
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': {}",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_updateAnalysisOptions_false() throws Exception {
    AnalysisOptions options = new AnalysisOptions();
    options.setAnalyzeAngular(false);
    options.setAnalyzePolymer(false);
    options.setEnableDeferredLoading(false);
    options.setEnableEnums(false);
    options.setGenerateDart2jsHints(false);
    options.setGenerateHints(false);
    server.updateAnalysisOptions(options);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.updateOptions',",
        "  'params': {",
        "    options: {",
        "      'analyzeAngular': false,",
        "      'analyzePolymer': false,",
        "      'enableDeferredLoading': false,",
        "      'enableEnums': false,",
        "      'generateDart2jsHints': false,",
        "      'generateHints': false",
        "    }",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_updateAnalysisOptions_true() throws Exception {
    AnalysisOptions options = new AnalysisOptions();
    options.setAnalyzeAngular(true);
    options.setAnalyzePolymer(true);
    options.setEnableDeferredLoading(true);
    options.setEnableEnums(true);
    options.setGenerateDart2jsHints(true);
    options.setGenerateHints(true);
    server.updateAnalysisOptions(options);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.updateOptions',",
        "  'params': {",
        "    options: {",
        "      'analyzeAngular': true,",
        "      'analyzePolymer': true,",
        "      'enableDeferredLoading': true,",
        "      'enableEnums': true,",
        "      'generateDart2jsHints': true,",
        "      'generateHints': true",
        "    }",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_updateContent() throws Exception {
    Map<String, ContentChange> files = ImmutableMap.of(
        "/fileA.dart",
        new ContentChange("aaa"),
        "/fileB.dart",
        new ContentChange("bbb", 1, 2, 3));
    server.updateContent(files);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
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
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_updateSdks() throws Exception {
    server.updateSdks(
        ImmutableList.of("/path/to/sdk/A", "/path/to/sdk/B"),
        ImmutableList.of("/path/to/sdk/C", "/path/to/sdk/D"),
        "/path/to/sdk/B");
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.updateSdks',",
        "  'params': {",
        "    'added': ['/path/to/sdk/A', '/path/to/sdk/B'],",
        "    'removed': ['/path/to/sdk/C', '/path/to/sdk/D'],",
        "    'default': '/path/to/sdk/B'",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_analysis_updateSdks_nullDefaultSDK() throws Exception {
    server.updateSdks(
        ImmutableList.of("/path/to/sdk/A", "/path/to/sdk/B"),
        ImmutableList.of("/path/to/sdk/C", "/path/to/sdk/D"),
        null);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'analysis.updateSdks',",
        "  'params': {",
        "    'added': ['/path/to/sdk/A', '/path/to/sdk/B'],",
        "    'removed': ['/path/to/sdk/C', '/path/to/sdk/D']",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_edit_getAssists() throws Exception {
    server.getAssists("/fileA.dart", 1, 2, new AssistsConsumer() {
      @Override
      public void computedSourceChanges(SourceChange[] sourceChanges, boolean isLastResult) {
        // TODO (jwren) not yet tested, specification still in flux
      }
    });
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getAssists',",
        "  'params': {",
        "    'file': '/fileA.dart',",
        "    'offset': 1,",
        "    'length': 2",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_edit_getFixes() throws Exception {
    List<AnalysisError> errors = ImmutableList.of((AnalysisError) new AnalysisErrorImpl(
        "/fileA.dart",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        1,
        2,
        "msg",
        null));
    server.getFixes(errors, new FixesConsumer() {
      @Override
      public void computedFixes(Map<AnalysisError, CorrectionProposal[]> fixesMap,
          boolean isLastResult) {
        // TODO (jwren) specification for notification back from server is TBD
      }
    });
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getFixes',",
        "  'params': {",
        "    'errors': [",
        "      {",
        "        'file': '/fileA.dart',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'msg'",
        "      }",
        "    ]",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_edit_getFixes_withCorrection() throws Exception {
    List<AnalysisError> errors = ImmutableList.of((AnalysisError) new AnalysisErrorImpl(
        "/fileA.dart",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER,
        1,
        2,
        "msg",
        "correction"));
    server.getFixes(errors, new FixesConsumer() {
      @Override
      public void computedFixes(Map<AnalysisError, CorrectionProposal[]> fixesMap,
          boolean isLastResult) {
      }
    });
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'edit.getFixes',",
        "  'params': {",
        "    'errors': [",
        "      {",
        "        'file': '/fileA.dart',",
        "        'errorCode': 'ParserErrorCode.ABSTRACT_CLASS_MEMBER',",
        "        'offset': 1,",
        "        'length': 2,",
        "        'message': 'msg',",
        "        'correction': 'correction'",
        "      }",
        "    ]",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_error() throws Exception {
    server.shutdown();
    putResponse(//
        "{",
        "  'id': '0',",
        "  'error': {",
        "    code: 'SOME_CODE',",
        "    message: 'testing parsing of error response'",
        "  }",
        "}");
    server.test_waitForWorkerComplete();
  }

  public void test_server_getVersion() throws Exception {
    final String[] versionPtr = {null};
    server.getVersion(new VersionConsumer() {
      @Override
      public void computedVersion(String version) {
        versionPtr[0] = version;
      }
    });
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'server.getVersion'",
        "}");
    assertTrue(requests.contains(expected));

    putResponse(//
        "{",
        "  'id': '0',",
        "  'result': {",
        "    'version': '0.0.1'",
        "  }",
        "}");
    server.test_waitForWorkerComplete();
    assertEquals("0.0.1", versionPtr[0]);
  }

  public void test_server_notification_connected() throws Exception {
    listener.assertServerConnected(false);
    putResponse(//
        "{",
        "  'event': 'server.connected'",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    listener.assertServerConnected(true);
  }

  public void test_server_notification_status_false() throws Exception {
    putResponse(//
        "{",
        "  'event': 'server.status',",
        "  'params': {",
        "    'analysis': {",
        "      analyzing: false",
        "    }",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    ServerStatus serverStatus = new ServerStatus();
    serverStatus.setAnalysisStatus(new AnalysisStatus(false, null));
    listener.assertServerStatus(serverStatus);
  }

  public void test_server_notification_status_true() throws Exception {
    putResponse(//
        "{",
        "  'event': 'server.status',",
        "  'params': {",
        "    'analysis': {",
        "      analyzing: true,",
        "      analysisTarget: 'target0'",
        "    }",
        "  }",
        "}");
    responseStream.waitForEmpty();
    server.test_waitForWorkerComplete();
    ServerStatus serverStatus = new ServerStatus();
    serverStatus.setAnalysisStatus(new AnalysisStatus(true, "target0"));
    listener.assertServerStatus(serverStatus);
  }

  public void test_server_setSubscriptions() throws Exception {
    server.setServerSubscriptions(new ArrayList<ServerService>(0));
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'server.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': []",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_server_setSubscriptions_status() throws Exception {
    ArrayList<ServerService> subscriptions = new ArrayList<ServerService>();
    subscriptions.add(ServerService.STATUS);
    server.setServerSubscriptions(subscriptions);
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'server.setSubscriptions',",
        "  'params': {",
        "    'subscriptions': [STATUS]",
        "  }",
        "}");
    assertTrue(requests.contains(expected));
  }

  public void test_server_shutdown() throws Exception {
    server.shutdown();
    List<JsonObject> requests = requestSink.getRequests();
    JsonElement expected = parseJson(//
        "{",
        "  'id': '0',",
        "  'method': 'server.shutdown'",
        "}");
    assertTrue(requests.contains(expected));
  }

  /**
   * Builds a JSON string from the given lines.
   */
  private JsonElement parseJson(String... lines) {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    return new JsonParser().parse(json);
  }
}
