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
import com.google.common.collect.Maps;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisServerSocket;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.AssistsConsumer;
import com.google.dart.server.BasicConsumer;
import com.google.dart.server.CompletionIdConsumer;
import com.google.dart.server.Consumer;
import com.google.dart.server.ContentChange;
import com.google.dart.server.Element;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.ServerService;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.BroadcastAnalysisServerListener;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisErrorsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisHighlightsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisNavigationProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisOccurrencesProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisOutlineProcessor;
import com.google.dart.server.internal.remote.processor.NotificationCompletionResultsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerConnectedProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerErrorProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerStatusProcessor;
import com.google.dart.server.internal.remote.utilities.RequestUtilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This {@link AnalysisServer} calls out to the analysis server written in Dart and communicates
 * with the server over standard IO streams.
 * 
 * @coverage dart.server.remote
 */
public class RemoteAnalysisServerImpl implements AnalysisServer {
  /**
   * For requests that do not have a {@link Consumer}, this object is created as a place holder so
   * that if an error occurs after the request, an error can be reported.
   */
  public static class LocalConsumer implements Consumer {
    private final JsonObject request;

    public LocalConsumer(JsonObject request) {
      this.request = request;
    }

    @SuppressWarnings("unused")
    private JsonObject getRequest() {
      return request;
    }

  }

  /**
   * A thread which reads input from the {@link LineReaderStream}.
   */
  public class ServerErrorReaderThread extends Thread {
    private LineReaderStream stream;

    public ServerErrorReaderThread(LineReaderStream stream) {
      setDaemon(true);
      setName("ServerErrorReaderThread");
      this.stream = stream;
    }

    @Override
    public void run() {
      while (true) {
        try {
          String line = stream.readLine();
          if (line == null) {
            return;
          }
          System.err.println(line);
        } catch (Exception e) {
          // TODO (jwren) handle error messages
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * A thread which reads responses from the {@link ResponseStream} and calls the associated
   * {@link Consumer}s from {@link RemoteAnalysisServerImpl#consumerMap}.
   */
  public class ServerResponseReaderThread extends Thread {

    private ResponseStream stream;

    public ServerResponseReaderThread(ResponseStream stream) {
      setDaemon(true);
      setName("ServerResponseReaderThread");
      this.stream = stream;
    }

    @Override
    public void run() {
      while (true) {
        try {
          JsonObject response = stream.take();
          if (response == null) {
            return;
          }
          lastResponseTime.set(System.currentTimeMillis());
          try {
            processResponse(response);
          } finally {
            stream.lastRequestProcessed();
          }
        } catch (Throwable e) {
          // TODO(scheglov) decide how to handle exceptions
          e.printStackTrace();
        }
      }
    }
  }

  // Server domain
  private static final String SERVER_NOTIFICATION_CONNECTED = "server.connected";
  private static final String SERVER_NOTIFICATION_ERROR = "server.error";
  private static final String SERVER_NOTIFICATION_STATUS = "server.status";

  // Analysis domain
  private static final String ANALYSIS_NOTIFICATION_ERRORS = "analysis.errors";
  private static final String ANALYSIS_NOTIFICATION_HIGHTLIGHTS = "analysis.highlights";
  private static final String ANALYSIS_NOTIFICATION_NAVIGATION = "analysis.navigation";
  private static final String ANALYSIS_NOTIFICATION_OCCURRENCES = "analysis.occurrences";
  private static final String ANALYSIS_NOTIFICATION_OUTLINE = "analysis.outline";

  // Code Completion domain
  private static final String COMPLETION_NOTIFICATION_RESULTS = "completion.results";

  private final AnalysisServerSocket socket;
  private RequestSink requestSink;
  private ResponseStream responseStream;
  private LineReaderStream errorStream;
  private final AtomicLong lastResponseTime = new AtomicLong(0);

  /**
   * The listener that will receive notification when new analysis results become available.
   */
  private final BroadcastAnalysisServerListener listener = new BroadcastAnalysisServerListener();

  /**
   * A mapping between {@link String} ids' and the associated {@link Consumer} that was passed when
   * the request was made.
   */
  private final Map<String, Consumer> consumerMap = Maps.newHashMap();

  /**
   * The object used to synchronize access to {@link #consumerMap}.
   */
  private final Object consumerMapLock = new Object();

  /**
   * The unique ID for the next request.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * The thread that restarts an unresponsive server or {@code null} if it has not been started.
   */
  private Thread watcher;

  /**
   * A flag indicating whether the watcher should continue monitoring the remote process.
   */
  private boolean watch;

  public RemoteAnalysisServerImpl(AnalysisServerSocket socket) {
    this.socket = socket;
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.addListener(listener);
  }

  @Override
  public void applyRefactoring() {
    // TODO(scheglov) implement
  }

  @Override
  public void createRefactoringExtractLocal() {
    // TODO(scheglov) implement
  }

  @Override
  public void createRefactoringExtractMethod() {
    // TODO(scheglov) implement
  }

  @Override
  public void deleteRefactoring(String refactoringId) {
    // TODO(scheglov) implement
  }

  @Override
  public void getAssists(String file, int offset, int length, AssistsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateEditGetAssists(id, file, offset, length),
        consumer);
  }

  @Override
  public void getCompletionSuggestions(String file, int offset, CompletionIdConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateCompletionGetSuggestions(id, file, offset),
        consumer);
  }

  @Override
  public void getFixes(List<AnalysisError> errors, FixesConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateEditGetFixes(id, errors), consumer);
  }

  @Override
  public void getTypeHierarchy(Element element, TypeHierarchyConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void getVersion(VersionConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerGetVersion(id), consumer);
  }

  @Override
  public void reanalyze() {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisReanalyze(id));
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
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
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateAnalysisSetAnalysisRoots(id, includedPaths, excludedPaths));
  }

  @Override
  public void setAnalysisSubscriptions(Map<AnalysisService, List<String>> subscriptions) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisSetSubscriptions(id, subscriptions));
  }

  @Override
  public void setPriorityFiles(List<String> files) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisSetPriorityFiles(id, files));
  }

  @Override
  public void setRefactoringExtractLocalOptions(/*String refactoringId, boolean allOccurrences,
                                                String name, RefactoringOptionsValidationConsumer consumer*/) {
    // TODO(scheglov) implement
  }

  @Override
  public void setRefactoringExtractMethodOptions(/*String refactoringId, String name,
                                                 boolean asGetter, boolean allOccurrences, Parameter[] parameters,
                                                 RefactoringExtractMethodOptionsValidationConsumer consumer*/) {
    // TODO(scheglov) implement
  }

  @Override
  public void setServerSubscriptions(List<ServerService> subscriptions) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerSetSubscriptions(id, subscriptions));
  }

  @Override
  public void shutdown() {
    stopWatcher();
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerShutdown(id), new BasicConsumer() {
      @Override
      public void received() {
        // Close communication channels once response has been received
        requestSink.close();
      }
    });
    stopServer();
  }

  @Override
  public void start(long millisToRestart) throws Exception {
    startServer();
    startWatcher(millisToRestart);
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!consumerMap.isEmpty()) {
      Thread.yield();
    }
  }

  @Override
  public void updateAnalysisOptions(AnalysisOptions options) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisUpdateOptions(id, options));
  }

  @Override
  public void updateContent(Map<String, ContentChange> files) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisUpdateContent(id, files));
  }

  @Override
  public void updateSdks(List<String> added, List<String> removed, String defaultSdk) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateAnalysisUpdateSdks(id, added, removed, defaultSdk));
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

  private void processCompletionIdConsumer(CompletionIdConsumer consumer, JsonObject resultObject) {
    String completionId = resultObject.get("id").getAsString();
    consumer.computedCompletionId(completionId);
  }

  private void processErrorResponse(JsonObject errorObject) throws Exception {
    // TODO (jwren) after Error section is done, revisit this.
    String errorCode = errorObject.get("code").getAsString();
    String errorMessage = errorObject.get("message").getAsString();
    //errorObject.get("data").getAsString();
    System.err.println(errorCode + ": " + errorMessage);
  }

  /**
   * Attempts to handle the given {@link JsonObject} as a notification.
   * 
   * @return {@code true} if it was handled or {@code false} otherwise
   */
  private boolean processNotification(JsonObject response) throws Exception {
    // prepare notification kind
    JsonElement eventElement = response.get("event");
    if (eventElement == null || !eventElement.isJsonPrimitive()) {
      return false;
    }
    String event = eventElement.getAsString();
    // handle each supported notification kind
    if (event.equals(ANALYSIS_NOTIFICATION_ERRORS)) {
      // analysis.errors
      new NotificationAnalysisErrorsProcessor(listener).process(response);
    } else if (event.equals(ANALYSIS_NOTIFICATION_HIGHTLIGHTS)) {
      // analysis.highlights
      new NotificationAnalysisHighlightsProcessor(listener).process(response);
    } else if (event.equals(ANALYSIS_NOTIFICATION_NAVIGATION)) {
      // analysis.navigation
      new NotificationAnalysisNavigationProcessor(listener).process(response);
    } else if (event.equals(ANALYSIS_NOTIFICATION_OCCURRENCES)) {
      // analysis.occurrences
      new NotificationAnalysisOccurrencesProcessor(listener).process(response);
    } else if (event.equals(ANALYSIS_NOTIFICATION_OUTLINE)) {
      // analysis.outline
      new NotificationAnalysisOutlineProcessor(listener).process(response);
    } else if (event.equals(COMPLETION_NOTIFICATION_RESULTS)) {
      // completion.results
      new NotificationCompletionResultsProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_STATUS)) {
      // server.status
      new NotificationServerStatusProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_ERROR)) {
      // server.error
      new NotificationServerErrorProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_CONNECTED)) {
      // server.connected
      new NotificationServerConnectedProcessor(listener).process(response);
    }
    // it is a notification, even if we did not handle it
    return true;
  }

  private void processResponse(JsonObject response) throws Exception {
    // handle notification
    if (processNotification(response)) {
      return;
    }
    // prepare ID
    JsonPrimitive idJsonPrimitive = (JsonPrimitive) response.get("id");
    if (idJsonPrimitive == null) {
      // TODO (jwren) handle this case
      return;
    }
    String idString = idJsonPrimitive.getAsString();
    // prepare consumer
    Consumer consumer = null;
    synchronized (consumerMapLock) {
      consumer = consumerMap.get(idString);
    }
    JsonObject errorObject = (JsonObject) response.get("error");
    if (errorObject != null) {
      processErrorResponse(errorObject);
    }
    // handle result
    JsonObject resultObject = (JsonObject) response.get("result");
    if (consumer instanceof CompletionIdConsumer) {
      processCompletionIdConsumer((CompletionIdConsumer) consumer, resultObject);
    } else if (consumer instanceof VersionConsumer) {
      processVersionConsumer((VersionConsumer) consumer, resultObject);
    } else if (consumer instanceof BasicConsumer) {
      ((BasicConsumer) consumer).received();
    }
    synchronized (consumerMapLock) {
      consumerMap.remove(idString);
    }
  }

  private void processVersionConsumer(VersionConsumer consumer, JsonObject resultObject) {
    String version = resultObject.get("version").getAsString();
    consumer.computedVersion(version);
  }

  /**
   * Sends the request, and associates the request with a {@link LocalConsumer}, a simple consumer
   * which only holds onto the the request {@link JsonObject}, for the purposes of error reporting.
   * 
   * @param id the identifier of the request
   * @param request the request to send
   */
  private void sendRequestToServer(String id, JsonObject request) {
    sendRequestToServer(id, request, new LocalConsumer(request));
  }

  /**
   * Sends the request and associates the request with the passed {@link Consumer}.
   * 
   * @param id the identifier of the request
   * @param request the request to send
   * @param consumer the {@link Consumer} to process a response
   */
  private void sendRequestToServer(String id, JsonObject request, Consumer consumer) {
    synchronized (consumerMapLock) {
      consumerMap.put(id, consumer);
    }
    requestSink.add(request);
  }

  private void sleep(long millisToSleep) {
    try {
      Thread.sleep(millisToSleep);
    } catch (InterruptedException e) {
      //$FALL-THROUGH$
    }
  }

  private void startServer() throws Exception {
    socket.start();
    consumerMap.clear();
    requestSink = socket.getRequestSink();
    responseStream = socket.getResponseStream();
    errorStream = socket.getErrorStream();
    new ServerResponseReaderThread(responseStream).start();
    if (errorStream != null) {
      new ServerErrorReaderThread(errorStream).start();
    }
  }

  private void startWatcher(final long millisToRestart) {
    if (millisToRestart <= 0 || watcher != null) {
      return;
    }
    watch = true;
    watcher = new Thread(getClass().getSimpleName() + " watcher") {
      @Override
      public void run() {
        watch(millisToRestart);
      };
    };
    watcher.setDaemon(true);
    watcher.start();
  }

  private void stopServer() {
    socket.stop();
  }

  private void stopWatcher() {
    if (watcher == null) {
      return;
    }
    watch = false;
    watcher.interrupt();
    try {
      watcher.join(5000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    watcher = null;
  }

  private void watch(long millisToRestart) {
    boolean sentRequest = false;
    while (watch) {
      long millisToSleep = lastResponseTime.get() + millisToRestart - System.currentTimeMillis();
      if (millisToSleep > 0) {
        // If there has been a response from the server within the desired period, then sleep
        sentRequest = false;
        sleep(millisToSleep);
      } else if (!sentRequest) {
        // If no response from the server then send a request and wait for the response
        getVersion(null);
        sentRequest = true;
        sleep(millisToRestart / 2);
      } else {
        // If still no response from server then restart the server
        stopServer();
        try {
          startServer();
        } catch (Exception e) {
          //TODO (danrubel): What to do if cannot restart server?
          System.err.println("Failed to restart analysis server");
          e.printStackTrace();
          break;
        }
        sentRequest = false;
        sleep(millisToRestart);
      }
    }
  }
}
