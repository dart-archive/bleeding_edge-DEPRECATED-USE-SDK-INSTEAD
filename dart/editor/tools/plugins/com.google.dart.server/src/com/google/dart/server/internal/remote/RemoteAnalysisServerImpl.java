package com.google.dart.server.internal.remote;

import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.Element;
import com.google.dart.server.FixableErrorCodesConsumer;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.MinorRefactoringsConsumer;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.SourceSet;
import com.google.dart.server.TypeHierarchyConsumer;

import java.util.Map;

/**
 * This {@link AnalysisServer} calls out to the analysis server written in Dart and communicates
 * with the server over standard IO streams.
 */
public class RemoteAnalysisServerImpl implements AnalysisServer {

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

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    // TODO (jwren) implement
  }

  @Override
  public void searchElementReferences(Element element, SearchResultsConsumer consumer) {
    // TODO (jwren) implement
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

  @Override
  public String version() {
    // TODO (jwren) implement
    return null;
  }

}
