package com.google.dart.server.internal.remote;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.CompletionSuggestionsConsumer;
import com.google.dart.server.Consumer;
import com.google.dart.server.Element;
import com.google.dart.server.FixableErrorCodesConsumer;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.MinorRefactoringsConsumer;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.SourceSet;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.remote.utilities.RequestUtilities;
import com.google.dart.server.internal.remote.utilities.StreamUtilities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
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

    private final InputStream inputStream;

    ServerResponseReaderThread(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public void run() {
      while (true) {
        String response = StreamUtilities.readResponse(inputStream);
        if (!response.trim().isEmpty()) {
          JsonObject element = (JsonObject) new JsonParser().parse(response);
          JsonPrimitive idJsonPrimitive = (JsonPrimitive) element.get("id");
          if (idJsonPrimitive == null) {
            // TODO (jwren) handle this case
            continue;
          }
          String idString = idJsonPrimitive.getAsString();
          synchronized (consumerMapLock) {
            Consumer consumer = consumerMap.get(idString);
            if (consumer == null) {
              // TODO (jwren) handle this error case
              continue;
            }
            // TODO(jwren) handle error responses:
//              JsonObject errorObject = (JsonObject) element.get("error");
            JsonObject resultObject = (JsonObject) element.get("result");
            if (consumer instanceof VersionConsumer) {
              processVersionConsumer((VersionConsumer) consumer, resultObject);
            }
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
   * The unique ID for the next request.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * The object used to synchronize access to {@link #consumerMap}.
   */
  private final Object consumerMapLock = new Object();

  /**
   * A mapping between {@link String} ids' and the associated {@link Consumer} that was passed when
   * the request was made.
   */
  private LinkedHashMap<String, Consumer> consumerMap = new LinkedHashMap<String, Consumer>();

  /**
   * The process running the analysis server.
   */
  private Process process;

  private final String runtimePath;
  private final String analysisServerPath;

  /**
   * Create an instance of {@link RemoteAnalysisServerImpl} using some runtime (Dart VM) path, and
   * some analysis server path.
   */
  public RemoteAnalysisServerImpl(String runtimePath, String analysisServerPath) {
    this.runtimePath = runtimePath;
    this.analysisServerPath = analysisServerPath;
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    // TODO (jwren) implement
  }

  @Override
  public void applyAnalysisDelta(String contextId, AnalysisDelta delta) {
    // TODO (jwren) implement
  }

  @Override
  public void applyChanges(String contextId, ChangeSet changeSet) {
    // TODO (jwren) implement
  }

  @Override
  public void computeCompletionSuggestions(String contextId, Source source, int offset,
      CompletionSuggestionsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void computeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer) {
    // TODO (jwren) implement
  }

  @Override
  public void computeMinorRefactorings(String contextId, Source source, int offset, int length,
      MinorRefactoringsConsumer consumer) {
    // TODO (jwren) implement
  }

  @Override
  public void computeTypeHierarchy(String contextId, Element element, TypeHierarchyConsumer consumer) {
    // TODO (jwren) implement
  }

  @Override
  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap) {
    // TODO (jwren) implement
    return null;
  }

  @Override
  public void deleteContext(String contextId) {
    // TODO (jwren) implement
  }

  @Override
  public void getFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer) {
    // TODO(scheglov) implement
  }

  public void initServerAndReaderThread() throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(runtimePath, analysisServerPath);
    Process process = processBuilder.start();
    InputStream inputStream = process.getInputStream();
    // TODO (jwren) swallow {"event":"server.connected"} response, connected state should be asserted
    StreamUtilities.readResponse(inputStream);
    ServerResponseReaderThread thread = new ServerResponseReaderThread(inputStream);
    thread.start();
    this.process = process;
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
    // TODO (jwren) implement
  }

  @Override
  public void searchTopLevelDeclarations(String contextId, String pattern,
      SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void setOptions(String contextId, AnalysisOptions options) {
    // TODO (jwren) implement
  }

  @Override
  public void setPrioritySources(String contextId, Source[] sources) {
    // TODO (jwren) implement
  }

  @Override
  public void shutdown() {
    // TODO (jwren) implement
  }

  @Override
  public void subscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions) {
    // TODO (jwren) implement
  }

  @VisibleForTesting
  public void test_putOnConsumerMap(String id, Consumer consumer) {
    consumerMap.put(id, consumer);
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!consumerMap.isEmpty()) {
      Thread.yield();
    }
  }

  @Override
  public void version(VersionConsumer consumer) {
    String id = generateUniqueId();
    sendRequestToServer(
        id,
        RequestUtilities.generateServerVersionRequest(id).toString()
            + System.getProperty("line.separator"),
        consumer);
  }

  /**
   * Generate and return a unique {@link String} id to be used in the requests sent to the analysis
   * server.
   * 
   * @return a unique {@link String} id to be used in the requests sent to the analysis server
   */
  private String generateUniqueId() {
    return nextId.getAndIncrement() + StringUtilities.EMPTY;
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
    OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
    try {
      writer.write(requestJson);
      writer.flush();
      synchronized (consumerMapLock) {
        consumerMap.put(id, consumer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
