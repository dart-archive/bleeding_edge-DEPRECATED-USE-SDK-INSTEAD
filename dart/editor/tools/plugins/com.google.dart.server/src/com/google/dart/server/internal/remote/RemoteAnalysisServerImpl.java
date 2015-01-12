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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisServerSocket;
import com.google.dart.server.BasicConsumer;
import com.google.dart.server.Consumer;
import com.google.dart.server.CreateContextConsumer;
import com.google.dart.server.FindElementReferencesConsumer;
import com.google.dart.server.FindMemberDeclarationsConsumer;
import com.google.dart.server.FindMemberReferencesConsumer;
import com.google.dart.server.FindTopLevelDeclarationsConsumer;
import com.google.dart.server.FormatConsumer;
import com.google.dart.server.GetAssistsConsumer;
import com.google.dart.server.GetAvailableRefactoringsConsumer;
import com.google.dart.server.GetErrorsConsumer;
import com.google.dart.server.GetFixesConsumer;
import com.google.dart.server.GetHoverConsumer;
import com.google.dart.server.GetNavigationConsumer;
import com.google.dart.server.GetRefactoringConsumer;
import com.google.dart.server.GetSuggestionsConsumer;
import com.google.dart.server.GetTypeHierarchyConsumer;
import com.google.dart.server.GetVersionConsumer;
import com.google.dart.server.MapUriConsumer;
import com.google.dart.server.SortMembersConsumer;
import com.google.dart.server.generated.types.AnalysisOptions;
import com.google.dart.server.generated.types.RefactoringOptions;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.internal.BroadcastAnalysisServerListener;
import com.google.dart.server.internal.remote.processor.AnalysisErrorsProcessor;
import com.google.dart.server.internal.remote.processor.AssistsProcessor;
import com.google.dart.server.internal.remote.processor.CompletionIdProcessor;
import com.google.dart.server.internal.remote.processor.CreateContextProcessor;
import com.google.dart.server.internal.remote.processor.FindElementReferencesProcessor;
import com.google.dart.server.internal.remote.processor.FindMemberDeclarationsProcessor;
import com.google.dart.server.internal.remote.processor.FindMemberReferencesProcessor;
import com.google.dart.server.internal.remote.processor.FindTopLevelDeclarationsProcessor;
import com.google.dart.server.internal.remote.processor.FixesProcessor;
import com.google.dart.server.internal.remote.processor.FormatProcessor;
import com.google.dart.server.internal.remote.processor.GetRefactoringProcessor;
import com.google.dart.server.internal.remote.processor.HoverProcessor;
import com.google.dart.server.internal.remote.processor.MapUriProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisErrorsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisFlushResultsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisHighlightsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisNavigationProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisOccurrencesProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisOutlineProcessor;
import com.google.dart.server.internal.remote.processor.NotificationAnalysisOverridesProcessor;
import com.google.dart.server.internal.remote.processor.NotificationCompletionResultsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationExecutionLaunchDataProcessor;
import com.google.dart.server.internal.remote.processor.NotificationSearchResultsProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerConnectedProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerErrorProcessor;
import com.google.dart.server.internal.remote.processor.NotificationServerStatusProcessor;
import com.google.dart.server.internal.remote.processor.RefactoringGetAvailableProcessor;
import com.google.dart.server.internal.remote.processor.SortMembersProcessor;
import com.google.dart.server.internal.remote.processor.TypeHierarchyProcessor;
import com.google.dart.server.internal.remote.processor.VersionProcessor;
import com.google.dart.server.internal.remote.utilities.RequestUtilities;
import com.google.dart.server.utilities.instrumentation.Instrumentation;
import com.google.dart.server.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.server.utilities.logging.Logging;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.util.ArrayList;
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
          // Ignore exceptions during shutdown
          if (shutdownRequested) {
            return;
          }
          if (e instanceof IOException) {
            String message = e.getMessage();
            if (message != null && message.contains("closed")) {
              Logging.getLogger().logError("AnalysisServer stream unexpected closed", e);
              return;
            }
          }
          Logging.getLogger().logError(e.getMessage(), e);
        }
      }
    }
  }

  // Server domain
  private static final String SERVER_NOTIFICATION_CONNECTED = "server.connected";
  public static final String SERVER_NOTIFICATION_ERROR = "server.error";
  private static final String SERVER_NOTIFICATION_STATUS = "server.status";

  // Analysis domain
  private static final String ANALYSIS_NOTIFICATION_ERRORS = "analysis.errors";
  private static final String ANALYSIS_NOTIFICATION_FLUSH_RESULTS = "analysis.flushResults";
  private static final String ANALYSIS_NOTIFICATION_HIGHTLIGHTS = "analysis.highlights";
  private static final String ANALYSIS_NOTIFICATION_NAVIGATION = "analysis.navigation";
  private static final String ANALYSIS_NOTIFICATION_OCCURRENCES = "analysis.occurrences";
  private static final String ANALYSIS_NOTIFICATION_OUTLINE = "analysis.outline";
  private static final String ANALYSIS_NOTIFICATION_OVERRIDES = "analysis.overrides";

  // Code Completion domain
  private static final String COMPLETION_NOTIFICATION_RESULTS = "completion.results";

  // Search domain
  private static final String SEARCH_NOTIFICATION_RESULTS = "search.results";

  // Execution domain
  private static final String LAUNCH_DATA_NOTIFICATION_RESULTS = "execution.launchData";

  private final static List<String> EMPTY_STR_LIST = Lists.newArrayList();

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
   * A mapping between "getRefactoring" request ids and the requested refactoring kinds.
   */
  private final Map<String, String> requestToRefactoringKindMap = Maps.newHashMap();

  /**
   * The thread that restarts an unresponsive server or {@code null} if it has not been started.
   */
  private Thread watcher;

  /**
   * A flag indicating whether the watcher should continue monitoring the remote process.
   */
  private boolean watch;

  /**
   * A flag indicating whether the server shutdown process has been requested.
   */
  private boolean shutdownRequested;

  public RemoteAnalysisServerImpl(AnalysisServerSocket socket) {
    this.socket = socket;
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.addListener(listener);
  }

  @Override
  public void analysis_getErrors(String file, GetErrorsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisGetErrors(id, file), consumer);
  }

  @Override
  public void analysis_getHover(String file, int offset, GetHoverConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisGetHover(id, file, offset), consumer);
  }

  @Override
  public void analysis_getNavigation(String file, int offset, int length,
      GetNavigationConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateAnalysisGetNavigation(id, file, offset, length));
  }

  @Override
  public void analysis_reanalyze() {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisReanalyze(id));
  }

  @Override
  public void analysis_setAnalysisRoots(List<String> includedPaths, List<String> excludedPaths,
      Map<String, String> packageRoots) {
    String id = generateUniqueId();
    if (includedPaths == null) {
      includedPaths = EMPTY_STR_LIST;
    }
    if (excludedPaths == null) {
      excludedPaths = EMPTY_STR_LIST;
    }
    sendRequestToServer(id, RequestUtilities.generateAnalysisSetAnalysisRoots(
        id,
        includedPaths,
        excludedPaths,
        packageRoots));
  }

  @Override
  public void analysis_setPriorityFiles(List<String> files) {
    String id = generateUniqueId();
    if (files == null) {
      files = EMPTY_STR_LIST;
    }
    sendRequestToServer(id, RequestUtilities.generateAnalysisSetPriorityFiles(id, files));
  }

  @Override
  public void analysis_setSubscriptions(Map<String, List<String>> subscriptions) {
    String id = generateUniqueId();
    if (subscriptions == null) {
      subscriptions = Maps.newHashMap();
    }
    sendRequestToServer(id, RequestUtilities.generateAnalysisSetSubscriptions(id, subscriptions));
  }

  @Override
  public void analysis_updateContent(Map<String, Object> files) {
    String id = generateUniqueId();
    if (files == null) {
      files = Maps.newHashMap();
    }
    sendRequestToServer(id, RequestUtilities.generateAnalysisUpdateContent(id, files));
  }

  @Override
  public void analysis_updateOptions(AnalysisOptions options) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateAnalysisUpdateOptions(id, options));
  }

  @Override
  public void completion_getSuggestions(String file, int offset, GetSuggestionsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateCompletionGetSuggestions(id, file, offset),
        consumer);
  }

  @Override
  public void edit_format(String file, int selectionOffset, int selectionLength,
      FormatConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateEditFormat(id, file, selectionOffset, selectionLength),
        consumer);
  }

  @Override
  public void edit_getAssists(String file, int offset, int length, GetAssistsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateEditGetAssists(id, file, offset, length),
        consumer);
  }

  @Override
  public void edit_getAvailableRefactorings(String file, int offset, int length,
      GetAvailableRefactoringsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateEditGetAvaliableRefactorings(id, file, offset, length),
        consumer);
  }

  @Override
  public void edit_getFixes(String file, int offset, GetFixesConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateEditGetFixes(id, file, offset), consumer);
  }

  @Override
  public void edit_getRefactoring(String kindId, String file, int offset, int length,
      boolean validateOnly, RefactoringOptions options, GetRefactoringConsumer consumer) {
    String id = generateUniqueId();
    requestToRefactoringKindMap.put(id, kindId);
    sendRequestToServer(id, RequestUtilities.generateEditGetRefactoring(
        id,
        kindId,
        file,
        offset,
        length,
        validateOnly,
        options), consumer);
  }

  @Override
  public void edit_sortMembers(String file, SortMembersConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateEditSortMembers(id, file), consumer);
  }

  @Override
  public void execution_createContext(String contextRoot, CreateContextConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateExecutionCreateContext(id, contextRoot),
        consumer);
  }

  @Override
  public void execution_deleteContext(String contextId) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateExecutionDeleteContext(id, contextId));
  }

  @Override
  public void execution_mapUri(String contextId, String file, String uri, MapUriConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateExecutionMapUri(id, contextId, file, uri),
        consumer);
  }

  @Override
  public void execution_setSubscriptions(List<String> subscriptions) {
    String id = generateUniqueId();
    if (subscriptions == null) {
      subscriptions = new ArrayList<String>();
    }
    sendRequestToServer(id, RequestUtilities.generateExecutionSetSubscriptions(id, subscriptions));
  }

  public boolean isSocketOpen() {
    return socket.isOpen();
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
  }

  @Override
  public void search_findElementReferences(String file, int offset, boolean includePotential,
      FindElementReferencesConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateSearchFindElementReferences(id, file, offset, includePotential),
        consumer);
  }

  @Override
  public void search_findMemberDeclarations(String name, FindMemberDeclarationsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateSearchFindMemberDeclarations(id, name),
        consumer);
  }

  @Override
  public void search_findMemberReferences(String name, FindMemberReferencesConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateSearchFindMemberReferences(id, name), consumer);
  }

  @Override
  public void search_findTopLevelDeclarations(String pattern,
      FindTopLevelDeclarationsConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateSearchFindTopLevelDeclarations(id, pattern),
        consumer);
  }

  @Override
  public void search_getTypeHierarchy(String file, int offset, GetTypeHierarchyConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateSearchGetTypeHierarchy(id, file, offset),
        consumer);
  }

  @Override
  public void server_getVersion(GetVersionConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(id, RequestUtilities.generateServerGetVersion(id), consumer);
  }

  @Override
  public void server_setSubscriptions(List<String> subscriptions) {
    String id = generateUniqueId();
    if (subscriptions == null) {
      subscriptions = Lists.newArrayList();
    }
    sendRequestToServer(id, RequestUtilities.generateServerSetSubscriptions(id, subscriptions));
  }

  @Override
  public void server_shutdown() {
    shutdownRequested = true;
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
  public void start() throws Exception {
    startServer();
    //startWatcher(millisToRestart);
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!consumerMap.isEmpty()) {
      Thread.yield();
    }
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

  private RequestError processErrorResponse(JsonObject errorObject) throws Exception {
    // TODO (jwren) after Error section is done, revisit this.
    String errorCode = errorObject.get("code").getAsString();
    String errorMessage = errorObject.get("message").getAsString();
    String errorStackTrace = errorObject.get("stackTrace") != null
        ? errorObject.get("stackTrace").getAsString() : null;
//    System.err.println(errorCode + ": " + errorMessage);
    return new RequestError(errorCode, errorMessage, errorStackTrace);
  }

  /**
   * Attempts to handle the given {@link JsonObject} as a notification. Return {@code true} if it
   * was handled, otherwise {@code false} is returned.
   * 
   * @return {@code true} if it was handled, otherwise {@code false} is returned
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
    } else if (event.equals(ANALYSIS_NOTIFICATION_FLUSH_RESULTS)) {
      // analysis.flushResults
      new NotificationAnalysisFlushResultsProcessor(listener).process(response);
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
    } else if (event.equals(ANALYSIS_NOTIFICATION_OVERRIDES)) {
      // analysis.overrides
      new NotificationAnalysisOverridesProcessor(listener).process(response);
    } else if (event.equals(COMPLETION_NOTIFICATION_RESULTS)) {
      // completion.results
      new NotificationCompletionResultsProcessor(listener).process(response);
    } else if (event.equals(SEARCH_NOTIFICATION_RESULTS)) {
      // search.results
      new NotificationSearchResultsProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_STATUS)) {
      // server.status
      new NotificationServerStatusProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_ERROR)) {
      // server.error
      new NotificationServerErrorProcessor(listener).process(response);
    } else if (event.equals(SERVER_NOTIFICATION_CONNECTED)) {
      // server.connected
      new NotificationServerConnectedProcessor(listener).process(response);
    } else if (event.equals(LAUNCH_DATA_NOTIFICATION_RESULTS)) {
      new NotificationExecutionLaunchDataProcessor(listener).process(response);
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
    RequestError requestError = null;
    if (errorObject != null) {
      requestError = processErrorResponse(errorObject);
      listener.requestError(requestError);
    }
    // handle result
    JsonObject resultObject = (JsonObject) response.get("result");
    //
    // Completion Domain
    //
    if (consumer instanceof GetSuggestionsConsumer) {
      new CompletionIdProcessor((GetSuggestionsConsumer) consumer).process(
          resultObject,
          requestError);
    }
    //
    // Search Domain
    //
    else if (consumer instanceof FindElementReferencesConsumer) {
      new FindElementReferencesProcessor((FindElementReferencesConsumer) consumer).process(resultObject);
    } else if (consumer instanceof FindMemberDeclarationsConsumer) {
      new FindMemberDeclarationsProcessor((FindMemberDeclarationsConsumer) consumer).process(resultObject);
    } else if (consumer instanceof FindMemberReferencesConsumer) {
      new FindMemberReferencesProcessor((FindMemberReferencesConsumer) consumer).process(resultObject);
    } else if (consumer instanceof FindTopLevelDeclarationsConsumer) {
      new FindTopLevelDeclarationsProcessor((FindTopLevelDeclarationsConsumer) consumer).process(resultObject);
    } else if (consumer instanceof GetTypeHierarchyConsumer) {
      new TypeHierarchyProcessor((GetTypeHierarchyConsumer) consumer).process(resultObject);
    }
    //
    // Edit Domain
    //
    else if (consumer instanceof FormatConsumer) {
      new FormatProcessor((FormatConsumer) consumer).process(resultObject, requestError);
    } else if (consumer instanceof GetHoverConsumer) {
      new HoverProcessor((GetHoverConsumer) consumer).process(resultObject, requestError);
    } else if (consumer instanceof GetRefactoringConsumer) {
      new GetRefactoringProcessor(requestToRefactoringKindMap, (GetRefactoringConsumer) consumer).process(
          idString,
          resultObject,
          requestError);
    } else if (consumer instanceof GetAssistsConsumer) {
      new AssistsProcessor((GetAssistsConsumer) consumer).process(resultObject, requestError);
    } else if (consumer instanceof GetFixesConsumer) {
      new FixesProcessor((GetFixesConsumer) consumer).process(resultObject, requestError);
    } else if (consumer instanceof GetAvailableRefactoringsConsumer) {
      new RefactoringGetAvailableProcessor((GetAvailableRefactoringsConsumer) consumer).process(
          resultObject,
          requestError);
    } else if (consumer instanceof GetErrorsConsumer) {
      new AnalysisErrorsProcessor((GetErrorsConsumer) consumer).process(resultObject, requestError);
    } else if (consumer instanceof SortMembersConsumer) {
      new SortMembersProcessor((SortMembersConsumer) consumer).process(resultObject, requestError);
    }
    //
    // Execution Domain
    //
    else if (consumer instanceof CreateContextConsumer) {
      new CreateContextProcessor((CreateContextConsumer) consumer).process(
          resultObject,
          requestError);
    } else if (consumer instanceof MapUriConsumer) {
      new MapUriProcessor((MapUriConsumer) consumer).process(resultObject);
    }
    //
    // Server Domain
    //
    else if (consumer instanceof GetVersionConsumer) {
      new VersionProcessor((GetVersionConsumer) consumer).process(resultObject);
    } else if (consumer instanceof BasicConsumer) {
      ((BasicConsumer) consumer).received();
    }
    synchronized (consumerMapLock) {
      consumerMap.remove(idString);
    }
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
      new ServerErrorReaderThread(errorStream, listener).start();
    }
  }

  @SuppressWarnings("unused")
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
      //$FALL-THROUGH$
    }
    watcher = null;
  }

  private void watch(long millisToRestart) {
    long restartTime = System.currentTimeMillis();
    int restartCount = 0;
    boolean sentRequest = false;
    while (watch) {
      long millisToSleep = lastResponseTime.get() + millisToRestart - System.currentTimeMillis();
      if (millisToSleep > 0) {
        // If there has been a response from the server within the desired period, then sleep
        sentRequest = false;
        sleep(millisToSleep);
      } else if (!sentRequest) {
        // If no response from the server then send a request and wait for the response
        server_getVersion(null);
        sentRequest = true;
        sleep(millisToRestart / 2);
      } else {
        // If still no response from server then restart the server
        InstrumentationBuilder instrumentation = Instrumentation.builder("RemoteAnalysisServerImpl.restartServer");
        try {
          stopServer();

          // If the analysis server has been restarted several times in a 5 minute period, then give up
          long now = System.currentTimeMillis();
          if (now - restartTime < 5 * 60 * 1000) {
            if (++restartCount > 3) {
              Logging.getLogger().logError(
                  "Restarted analysis server several times in a short period of time. Giving up.");
              instrumentation.metric("restartedAnalysisServer", false);
              break;
            }
          } else {
            restartTime = now;
            restartCount = 0;
          }

          startServer();
        } catch (Exception e) {
          // Bail out if cannot restart the server
          Logging.getLogger().logError("Failed to restart analysis server", e);
          instrumentation.record(e);
          break;
        } finally {
          instrumentation.log();
        }
        sentRequest = false;
        sleep(millisToRestart);
      }
    }
  }
}
