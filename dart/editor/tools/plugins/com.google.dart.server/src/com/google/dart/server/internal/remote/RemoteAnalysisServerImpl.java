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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.CompletionSuggestionsConsumer;
import com.google.dart.server.Consumer;
import com.google.dart.server.ContentChange;
import com.google.dart.server.Element;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.MinorRefactoringsConsumer;
import com.google.dart.server.RefactoringApplyConsumer;
import com.google.dart.server.RefactoringExtractLocalConsumer;
import com.google.dart.server.RefactoringExtractMethodConsumer;
import com.google.dart.server.RefactoringExtractMethodOptionsValidationConsumer;
import com.google.dart.server.RefactoringOptionsValidationConsumer;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.ServerService;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.remote.utilities.RequestUtilities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This {@link AnalysisServer} calls out to the analysis server written in Dart and communicates
 * with the server over standard IO streams.
 * 
 * @coverage dart.server.remote
 */
public class RemoteAnalysisServerImpl implements AnalysisServer {

  /**
   * A thread which reads output from a passed {@link InputStream}, parses the input, and then calls
   * the associated {@link Consumer} from {@link RemoteAnalysisServerImpl#consumerMap}.
   */
  public class ServerResponseReaderThread extends Thread {

    private String[] lines = null;

    public ServerResponseReaderThread() {
    }

    @VisibleForTesting
    public ServerResponseReaderThread(String[] lines) {
      this.lines = lines;
    }

    @Override
    public void run() {
      while (true) {
        List<String> responses = null;
        if (lines == null) {
          responses = readResponse();
        } else {
          responses = new ArrayList<String>(lines.length);
          for (String line : lines) {
            responses.add(line);
          }
        }
        for (String response : responses) {
          JsonObject element = (JsonObject) new JsonParser().parse(response);
          JsonPrimitive idJsonPrimitive = (JsonPrimitive) element.get("id");
          if (idJsonPrimitive == null) {
            // TODO (jwren) handle this case
            continue;
          }
          String idString = idJsonPrimitive.getAsString();
          Consumer consumer = null;
          synchronized (consumerMapLock) {
            consumer = consumerMap.get(idString);
          }
          // TODO(jwren) handle error responses:
//              JsonObject errorObject = (JsonObject) element.get("error");
          JsonObject resultObject = (JsonObject) element.get("result");
          if (consumer instanceof VersionConsumer) {
            processVersionConsumer((VersionConsumer) consumer, resultObject);
          }
          synchronized (consumerMapLock) {
            consumerMap.remove(idString);
          }
        }
        if (consumerMap.isEmpty()) {
          Thread.yield();
        }
      }
    }

    private void processVersionConsumer(VersionConsumer versionConsumer, JsonObject resultObject) {
      String version = resultObject.get("version").getAsString();
      versionConsumer.computedVersion(version);
    }
  }

  /**
   * A mapping between {@link String} ids' and the associated {@link Consumer} that was passed when
   * the request was made.
   */
  private final HashMap<String, Consumer> consumerMap;

  /**
   * The object used to synchronize access to {@link #consumerMap}.
   */
  private final Object consumerMapLock = new Object();

  private final String runtimePath;

  private final String analysisServerPath;

  /**
   * The unique ID for the next request.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * The {@link Writer} for responses to the server.
   */
  private PrintWriter printWriter;

  private BufferedReader bufferedReader;

  /**
   * Create an instance of {@link RemoteAnalysisServerImpl} using some runtime (Dart VM) path, and
   * some analysis server path.
   */
  public RemoteAnalysisServerImpl(String runtimePath, String analysisServerPath) {
    this.runtimePath = runtimePath;
    this.analysisServerPath = analysisServerPath;
    this.consumerMap = new HashMap<String, Consumer>();
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    // TODO (jwren) implement
  }

  @Override
  public void applyRefactoring(String refactoringId, RefactoringApplyConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void createRefactoringExtractLocal(String file, int offset, int length,
      RefactoringExtractLocalConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void createRefactoringExtractMethod(String file, int offset, int length,
      RefactoringExtractMethodConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void deleteRefactoring(String refactoringId) {
    // TODO(scheglov) implement
  }

  @Override
  public void getCompletionSuggestions(String file, int offset,
      CompletionSuggestionsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void getFixes(List<AnalysisError> errors, FixesConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void getMinorRefactorings(String file, int offset, int length,
      MinorRefactoringsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void getTypeHierarchy(Element element, TypeHierarchyConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void getVersion(VersionConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerVersionRequest(id).toString(), consumer);
  }

  public void initServerAndReaderThread() throws IOException {
    //
    // Initialize the process using runtimePath and analysisServerPath
    //
    ProcessBuilder processBuilder = new ProcessBuilder(runtimePath, analysisServerPath);
    Process process = processBuilder.start();

    //
    // Initialize the reader for the input stream from the process
    //
    bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

    //
    // TODO (jwren) The following swallows the {"event":"server.connected"} response, but the
    // connected state should be asserted.
    // TODO (jwren) We also need to wait for the "server.connected" response, which we currently are
    // not doing.
    //
    readResponse();

    //
    // Create and start the ServerResponseReaderThread thread.
    //
    new ServerResponseReaderThread().start();

    //
    // Initialize the print writer.
    //
    printWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    // TODO (jwren) implement
  }

  @Override
  public void searchClassMemberDeclarations(String name, SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void searchClassMemberReferences(String name, SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void searchElementReferences(Element element, boolean withPotential,
      SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void searchTopLevelDeclarations(String pattern, SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void setAnalysisRoots(List<String> includedPaths, List<String> excludedPaths) {
    // TODO(scheglov) implement
  }

  @Override
  public void setAnalysisSubscriptions(Map<AnalysisService, List<String>> subscriptions) {
    // TODO(scheglov) implement
  }

  @Override
  public void setContent(Map<String, ContentChange> files) {
    // TODO(scheglov) implement
  }

  @Override
  public void setPriorityFiles(List<String> files) {
    // TODO(scheglov) implement
  }

  @Override
  public void setRefactoringExtractLocalOptions(String refactoringId, boolean allOccurrences,
      String name, RefactoringOptionsValidationConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void setRefactoringExtractMethodOptions(String refactoringId, String name,
      boolean asGetter, boolean allOccurrences, Parameter[] parameters,
      RefactoringExtractMethodOptionsValidationConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void setServerSubscriptions(List<ServerService> subscriptions) {
    // TODO(scheglov) implement
  }

  @Override
  public void shutdown() {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerShutdownRequest(id).toString(), null);
  }

  @VisibleForTesting
  public void test_setPrintWriter(PrintWriter printWriter) {
    this.printWriter = printWriter;
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!consumerMap.isEmpty()) {
      Thread.yield();
    }
  }

  @Override
  public void updateAnalysisOptions(AnalysisOptions options) {
    // TODO(scheglov) implement
  }

  @Override
  public void updateSdks(List<String> added, List<String> removed, String defaultSdk) {
    // TODO(scheglov) implement
  }

  /**
   * Generate and return a unique {@link String} id to be used in the requests sent to the analysis
   * server.
   * 
   * @return a unique {@link String} id to be used in the requests sent to the analysis server
   */
  private String generateUniqueId() {
    return Integer.toString(nextId.getAndIncrement());
  }

  private List<String> readResponse() {
    List<String> lines = new ArrayList<String>(1);
    try {
      String currentLine;
      while (bufferedReader.ready() && (currentLine = bufferedReader.readLine()) != null) {
        lines.add(currentLine.trim());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

  /**
   * Given some {@link String} id, a {@link String} Json request, and the associated consumer, this
   * method writes the request to standard out, and stores the id/ consumer key/ value pair in
   * {@link #consumerMap}.
   * 
   * @param id the {@link String} used in the request Json object
   * @param requestJson the {@link String} representation of the Json object
   * @param consumer the {@link Consumer}
   */
  private void sendRequestToServer(String id, String requestJson, Consumer consumer) {
    synchronized (consumerMapLock) {
      consumerMap.put(id, consumer);
    }
    printWriter.println(requestJson);
    printWriter.flush();
  }
}
