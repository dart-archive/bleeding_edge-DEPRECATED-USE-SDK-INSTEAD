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

package com.google.dart.server.internal.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.AssistsConsumer;
import com.google.dart.server.CompletionIdConsumer;
import com.google.dart.server.ContentChange;
import com.google.dart.server.Element;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.InternalAnalysisServer;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.RefactoringApplyConsumer;
import com.google.dart.server.RefactoringExtractLocalConsumer;
import com.google.dart.server.RefactoringExtractMethodConsumer;
import com.google.dart.server.RefactoringExtractMethodOptionsValidationConsumer;
import com.google.dart.server.RefactoringOptionsValidationConsumer;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.ServerService;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.VersionConsumer;
import com.google.dart.server.internal.local.operation.GetVersionOperation;
import com.google.dart.server.internal.local.operation.PerformAnalysisOperation;
import com.google.dart.server.internal.local.operation.ServerOperation;
import com.google.dart.server.internal.local.operation.ServerOperationQueue;
import com.google.dart.server.internal.local.operation.ShutdownOperation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-process implementation of {@link AnalysisServer}.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class LocalAnalysisServerImpl implements AnalysisServer, InternalAnalysisServer {
//  /**
//   * The thread that runs {@link #index}.
//   */
//  private class LocalAnalysisServerIndexThread extends Thread {
//    public LocalAnalysisServerIndexThread() {
//      setName("LocalAnalysisServerIndexThread");
//      setDaemon(true);
//    }
//
//    @Override
//    public void run() {
//      index.run();
//    }
//  }

  /**
   * The thread that executes {@link ServerOperation}.
   */
  private class LocalAnalysisServerOperationThread extends Thread {
    public LocalAnalysisServerOperationThread() {
      setName("LocalAnalysisServerOperationThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        ServerOperation operation = operationQueue.take(0);
        while (test_paused) {
          Thread.yield();
        }
        if (operation == ShutdownOperation.INSTANCE) {
          break;
        }
        try {
          operation.performOperation(LocalAnalysisServerImpl.this);
        } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
        }
        operationQueue.markLastOperationCompleted();
      }
    }
  }

  private static final String VERSION = "0.0.1";

  /**
   * A table mapping SDK directories to the corresponding {@link DartSdk} instances.
   */
  private static final Map<String, DartSdk> sdkMap = Maps.newHashMap();

  /**
   * The queue of {@link ServerOperation}s to execute.
   */
  private final ServerOperationQueue operationQueue = new ServerOperationQueue();

//  /**
//   * The {@link Index} instance for this server.
//   */
//  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
//
//  /**
//   * The {@link SearchEngine} instance for this server.
//   */
//  private final SearchEngine searchEngine = SearchEngineFactory.createSearchEngine(index);

  /**
   * This is used only for testing purposes and allows tests to control the order of operations on
   * multiple threads.
   */
  private boolean test_paused;

  /**
   * This is used only for testing purposes and allows tests to enable logging.
   */
  private boolean test_log;

  /**
   * This is used only for testing purposes and allows tests to check the order of operations.
   */
  private List<String> test_analyzedContexts;

  /**
   * The unique ID for the next context.
   */
  private int nextContextId = 0;

  /**
   * The unique ID for the next context.
   */
  private int nextRefactoringId = 0;

  /**
   * A table mapping context id's to the analysis contexts associated with them.
   */
  private final Map<String, AnalysisContext> contextMap = Maps.newHashMap();

  /**
   * A table mapping context analysis contexts to id's associated with them.
   */
  private final Map<AnalysisContext, String> contextIdMap = Maps.newHashMap();

  /**
   * A function mapping analysis contexts to id's associated with them.
   */
  private final Function<AnalysisContext, String> contextToIdFunction = new Function<AnalysisContext, String>() {
    @Override
    public String apply(AnalysisContext context) {
      return contextIdMap.get(context);
    }
  };

  /**
   * A table mapping context id's to the sources known in the associated contexts.
   */
  private final Map<String, Set<Source>> contextKnownSourcesMap = Maps.newHashMap();

  /**
   * A table mapping context id's to the sources explicitly added to the associated contexts.
   */
  private final Map<String, Set<Source>> contextAddedSourcesMap = Maps.newHashMap();

  /**
   * A set of context id's with priority sources.
   */
  private final Set<String> priorityContexts = Sets.newHashSet();

  /**
   * A table mapping context id's to the subscriptions for notifications associated with them.
   */
  private final Map<String, Map<NotificationKind, SourceSetBasedProvider>> notificationMap = Maps.newHashMap();

  /**
   * A table mapping refactoring id's to the refactorings associated with them.
   */
  private final Map<String, Refactoring> refactoringMap = Maps.newHashMap();

  /**
   * The listener that will receive notification when new analysis results become available.
   */
  private final BroadcastAnalysisServerListener listener = new BroadcastAnalysisServerListener();

  public LocalAnalysisServerImpl() {
//    new LocalAnalysisServerIndexThread().start();
    new LocalAnalysisServerOperationThread().start();
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.addListener(listener);
  }

//  @Override
//  public void applyAnalysisDelta(String contextId, AnalysisDelta delta) {
//    operationQueue.add(new ApplyAnalysisDeltaOperation(contextId, delta));
//  }
//
//  @Override
//  public void applyChanges(String contextId, ChangeSet changeSet) {
//    operationQueue.add(new ApplyChangesOperation(contextId, changeSet));
//  }
//
//  @Override
//  public void applyRefactoring(String refactoringId, RefactoringApplyConsumer consumer) {
//    operationQueue.add(new ApplyRefactoringOperation(refactoringId, consumer));
//  }
//
//  @Override
//  public void computeCompletionSuggestions(String contextId, Source source, int offset,
//      CompletionSuggestionsConsumer consumer) {
//    operationQueue.add(new ComputeCompletionSuggestionsOperation(
//        contextId,
//        source,
//        offset,
//        offset,
//        consumer));
//  }
//
//  @Override
//  public void computeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer) {
//    operationQueue.add(new ComputeFixesOperation(contextId, errors, consumer));
//  }
//
//  @Override
//  public void computeMinorRefactorings(String contextId, Source source, int offset, int length,
//      MinorRefactoringsConsumer consumer) {
//    operationQueue.add(new ComputeMinorRefactoringsOperation(
//        contextId,
//        source,
//        offset,
//        length,
//        consumer));
//  }
//
//  @Override
//  public void computeTypeHierarchy(String contextId, Element element, TypeHierarchyConsumer consumer) {
//    operationQueue.add(new ComputeTypeHierarchyOperation(contextId, element, consumer));
//  }
//
//  @Override
//  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap) {
//    String contextId = name + "-" + nextContextId++;
//    operationQueue.add(new CreateContextOperation(contextId, sdkDirectory, packageMap));
//    return contextId;
//  }
//
//  @Override
//  public void createRefactoringExtractLocal(String contextId, Source source, int offset,
//      int length, RefactoringExtractLocalConsumer consumer) {
//    operationQueue.add(new CreateRefactoringExtractLocalOperation(
//        contextId,
//        source,
//        offset,
//        length,
//        consumer));
//  }
//
//  @Override
//  public void createRefactoringExtractMethod(String contextId, Source source, int offset,
//      int length, RefactoringExtractMethodConsumer consumer) {
//    operationQueue.add(new CreateRefactoringExtractMethodOperation(
//        contextId,
//        source,
//        offset,
//        length,
//        consumer));
//  }
//
//  @Override
//  public void deleteContext(String contextId) {
//    operationQueue.add(new DeleteContextOperation(contextId));
//  }
//
//  @Override
//  public void deleteRefactoring(String refactoringId) {
//    operationQueue.add(new DeleteRefactoringOperation(refactoringId));
//  }
//
//  @Override
//  public AnalysisContext getContext(String contextId) {
//    GetContextOperation operation = new GetContextOperation(contextId);
//    operationQueue.add(operation);
//    return operation.getContext();
//  }
//
//  @Override
//  public Map<String, AnalysisContext> getContextMap() {
//    return contextMap;
//  }
//
//  @Override
//  public void getFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer) {
//    operationQueue.add(new GetFixableErrorCodesOperation(contextId, consumer));
//  }

  @Override
  public void applyRefactoring(String refactoringId, RefactoringApplyConsumer consumer) {
  }

  @Override
  public void createRefactoringExtractLocal(String file, int offset, int length,
      RefactoringExtractLocalConsumer consumer) {
  }

//  /**
//   * Implementation for {@link #applyAnalysisDelta(String, AnalysisDelta)}.
//   */
//  public void internalApplyAnalysisDelta(String contextId, AnalysisDelta delta) {
//    AnalysisContext context = getAnalysisContext(contextId);
//    Set<Source> sourcesMap = getSourcesMap(contextId, contextAddedSourcesMap);
//    for (Entry<Source, AnalysisLevel> entry : delta.getAnalysisLevels().entrySet()) {
//      Source source = entry.getKey();
//      if (entry.getValue() == AnalysisLevel.NONE) {
//        sourcesMap.remove(source);
//      } else {
//        sourcesMap.add(source);
//      }
//    }
//    context.applyAnalysisDelta(delta);
//    schedulePerformAnalysisOperation(contextId, false);
//  }
//
//  /**
//   * Implementation for {@link #applyChanges(String, ChangeSet)}.
//   */
//  public void internalApplyChanges(String contextId, ChangeSet changeSet) throws Exception {
//    AnalysisContext context = getAnalysisContext(contextId);
//    getSourcesMap(contextId, contextAddedSourcesMap).addAll(changeSet.getAddedSources());
//    context.applyChanges(changeSet);
//    schedulePerformAnalysisOperation(contextId, false);
//  }
//
//  public void internalApplyRefactoring(String refactoringId, RefactoringApplyConsumer consumer)
//      throws Exception {
//    Refactoring refactoring = getRefactoring(refactoringId);
//    RefactoringStatus status = refactoring.checkFinalConditions(null);
//    Change change = null;
//    if (!status.hasFatalError()) {
//      change = refactoring.createChange(null);
//    }
//    consumer.computed(status, change);
//  }
//
//  /**
//   * Implementation for {@link #computeCompletionSuggestions}.
//   */
//  public void internalComputeCompletionSuggestions(String contextId, Source source, int offset,
//      int length, CompletionSuggestionsConsumer consumer) throws Exception {
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Source[] librarySources = analysisContext.getLibrariesContaining(source);
//    if (librarySources.length != 0) {
//      Source librarySource = librarySources[0];
//      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//      CompletionSuggestion[] suggestions = CompletionSuggestion.EMPTY_ARRAY;
//      if (unit != null) {
//        suggestions = new DartUnitCompletionSuggestionsComputer(
//            searchEngine,
//            contextId,
//            analysisContext,
//            source,
//            unit,
//            offset).compute();
//      }
//      consumer.computed(suggestions);
//    }
//  }
//
//  /**
//   * Implementation for {@link #computeFixes(String, AnalysisError[], FixesConsumer)}.
//   */
//  public void internalComputeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer)
//      throws Exception {
//    log("internalComputeFixes: %s", errors.length);
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    for (AnalysisError error : errors) {
//      Source source = error.getSource();
//      Source[] librarySources = analysisContext.getLibrariesContaining(source);
//      if (librarySources.length != 0) {
//        Source librarySource = librarySources[0];
//        CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//        if (unit != null) {
//          new DartUnitFixesComputer(searchEngine, contextId, analysisContext, unit, error, consumer).compute();
//        }
//      }
//    }
//    // send "done" notification
//    consumer.computedFixes(Maps.<AnalysisError, CorrectionProposal[]> newHashMap(), true);
//  }
//
//  /**
//   * Implementation for
//   * {@link #computeMinorRefactorings(String, Source, int, MinorRefactoringsConsumer)}.
//   */
//  public void internalComputeMinorRefactorings(String contextId, Source source, int offset,
//      int length, MinorRefactoringsConsumer consumer) throws Exception {
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Source[] librarySources = analysisContext.getLibrariesContaining(source);
//    if (librarySources.length != 0) {
//      Source librarySource = librarySources[0];
//      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//      if (unit != null) {
//        new DartUnitMinorRefactoringsComputer(
//            searchEngine,
//            contextId,
//            analysisContext,
//            source,
//            unit,
//            offset,
//            length,
//            consumer).compute();
//      }
//    }
//    consumer.computedProposals(CorrectionProposal.EMPTY_ARRAY, true);
//  }
//
//  /**
//   * Implementation for {@link #computeTypeHierarchy(String, Element, TypeHierarchyConsumer)}.
//   */
//  public void internalComputeTypeHierarchy(String contextId, Element element,
//      TypeHierarchyConsumer consumer) throws Exception {
//    TypeHierarchyItem result = null;
//    // prepare context
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Source source = element.getSource();
//    Source[] librarySources = analysisContext.getLibrariesContaining(source);
//    // compute
//    if (librarySources.length != 0) {
//      Source librarySource = librarySources[0];
//      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//      if (unit != null) {
//        CompilationUnitElement unitElement = unit.getElement();
//        result = new TypeHierarchyComputer(searchEngine, contextId, unitElement, element).compute();
//      }
//    }
//    // done
//    consumer.computedHierarchy(result);
//  }
//
//  /**
//   * Implementation for {@link #createContext(String, String, Map)}.
//   */
//  public void internalCreateContext(String contextId, String sdkDirectory,
//      Map<String, String> packageMap) throws Exception {
//    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
//    DartSdk sdk = getSdk(contextId, sdkDirectory);
//    // prepare package map
//    Map<String, Resource> packageResourceMap = Maps.newHashMap();
//    for (Entry<String, String> entry : packageMap.entrySet()) {
//      String packageName = entry.getKey();
//      String packageDirName = entry.getValue();
//      File packageDir = new File(packageDirName);
//      FileResource packageResource = new FileResource(packageDir);
//      packageResourceMap.put(packageName, packageResource);
//    }
//    // set source factory
//    SourceFactory sourceFactory = new SourceFactory(
//        new DartUriResolver(sdk),
//        new FileUriResolver(),
//        new PackageMapUriResolver(packageResourceMap));
//    context.setSourceFactory(sourceFactory);
//    // add context
//    contextMap.put(contextId, context);
//    schedulePerformAnalysisOperation(contextId, false);
//  }
//
//  /**
//   * Implementation for {@link #createRefactoringExtractLocal}.
//   */
//  public void internalCreateRefactoringExtractLocal(String contextId, Source source, int offset,
//      int length, RefactoringExtractLocalConsumer consumer) throws Exception {
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Source[] librarySources = analysisContext.getLibrariesContaining(source);
//    if (librarySources.length != 0) {
//      Source librarySource = librarySources[0];
//      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//      if (unit != null) {
//        // prepare context
//        AssistContext assistContext = new AssistContext(
//            searchEngine,
//            analysisContext,
//            contextId,
//            source,
//            unit,
//            offset,
//            length);
//        // prepare refactoring
//        ExtractLocalRefactoring refactoring = RefactoringFactory.createExtractLocalRefactoring(assistContext);
//        RefactoringStatus status = refactoring.checkInitialConditions(null);
//        // fail if FATAL
//        if (status.hasFatalError()) {
//          consumer.computed(null, status, false, null);
//          return;
//        }
//        // OK, register this refactoring
//        String refactoringId = "extractLocal-" + nextRefactoringId++;
//        refactoringMap.put(refactoringId, refactoring);
//        consumer.computed(
//            refactoringId,
//            status,
//            refactoring.hasSeveralOccurrences(),
//            refactoring.guessNames());
//      }
//    }
//  }
//
//  /**
//   * Implementation for {@link #createRefactoringExtractMethod}.
//   */
//  public void internalCreateRefactoringExtractMethod(String contextId, Source source, int offset,
//      int length, RefactoringExtractMethodConsumer consumer) throws Exception {
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Source[] librarySources = analysisContext.getLibrariesContaining(source);
//    if (librarySources.length != 0) {
//      Source librarySource = librarySources[0];
//      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
//      if (unit != null) {
//        // prepare context
//        AssistContext assistContext = new AssistContext(
//            searchEngine,
//            analysisContext,
//            contextId,
//            source,
//            unit,
//            offset,
//            length);
//        // prepare refactoring
//        ExtractMethodRefactoring refactoring = RefactoringFactory.createExtractMethodRefactoring(assistContext);
//        RefactoringStatus status = refactoring.checkInitialConditions(null);
//        // fail if FATAL
//        if (status.hasFatalError()) {
//          consumer.computed(null, status, 0, false, null);
//          return;
//        }
//        // OK, register this refactoring
//        String refactoringId = "extractMethod-" + nextRefactoringId++;
//        refactoringMap.put(refactoringId, refactoring);
//        consumer.computed(
//            refactoringId,
//            status,
//            refactoring.getNumberOfOccurrences(),
//            refactoring.canExtractGetter(),
//            refactoring.getParameters());
//      }
//    }
//  }
//
//  /**
//   * Implementation for {@link #deleteContext(String)}.
//   */
//  public void internalDeleteContext(String contextId) throws Exception {
//    // stop processing this context
//    operationQueue.removeWithContextId(contextId);
//    // remove associated information
//    contextKnownSourcesMap.remove(contextId);
//    contextAddedSourcesMap.remove(contextId);
//    notificationMap.remove(contextId);
//    // prepare context
//    AnalysisContext context = contextMap.remove(contextId);
//    if (context == null) {
//      throw new AnalysisServerErrorException(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
//    }
//    // remove from index
//    index.removeContext(context);
//  }
//
//  public void internalDeleteRefactoring(String refactoringId) {
//    Refactoring refactoring = refactoringMap.remove(refactoringId);
//    if (refactoring == null) {
//      onServerError(AnalysisServerErrorCode.INVALID_REFACTORING_ID, refactoring);
//    }
//  }
//
//  public void internalGetFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer) {
//    ErrorCode[] fixableErrorCodes = DartUnitFixesComputer.getFixableErrorCodes();
//    consumer.computed(fixableErrorCodes);
//  }

  @Override
  public void createRefactoringExtractMethod(String file, int offset, int length,
      RefactoringExtractMethodConsumer consumer) {
  }

//  /**
//   * Sends one of the {@link NotificationKind}s.
//   */
//  public void internalNotification(String contextId, ChangeNotice changeNotice,
//      NotificationKind kind) throws Exception {
//    Source source = changeNotice.getSource();
//    log("internalNotification: %s with %s", kind, changeNotice);
//    switch (kind) {
//      case ERRORS: {
//        AnalysisError[] errors = changeNotice.getErrors();
//        if (errors == null) {
//          errors = AnalysisError.NO_ERRORS;
//        }
//        log("\tERRORS %s", errors.length);
//        listener.computedErrors(contextId, source, errors);
//        break;
//      }
//      case HIGHLIGHTS: {
//        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
//        log("\tHIGHLIGHTS %s", dartUnit);
//        if (dartUnit != null) {
//          listener.computedHighlights(
//              contextId,
//              source,
//              new DartUnitHighlightsComputer(dartUnit).compute());
//        }
//        break;
//      }
//      case NAVIGATION: {
//        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
//        if (dartUnit != null) {
//          listener.computedNavigation(contextId, source, new DartUnitNavigationComputer(
//              contextId,
//              dartUnit).compute());
//        }
//        break;
//      }
//      case OUTLINE: {
//        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
//        if (dartUnit != null) {
//          listener.computedOutline(contextId, source, new DartUnitOutlineComputer(
//              contextId,
//              source,
//              dartUnit).compute());
//        }
//        break;
//      }
//    }
//  }
//
//  /**
//   * Performs analysis in the given {@link AnalysisContext}.
//   */
//  public void internalPerformAnalysis(String contextId) throws Exception {
//    if (test_analyzedContexts != null) {
//      test_analyzedContexts.add(contextId);
//    }
//    AnalysisContext context = getAnalysisContext(contextId);
//    Set<Source> knownSources = getSourcesMap(contextId, contextKnownSourcesMap);
//    // prepare results
//    AnalysisResult result = context.performAnalysisTask();
//    ChangeNotice[] notices = result.getChangeNotices();
//    log("internalPerformAnalysis: %s", result.getTaskClassName());
//    if (notices == null) {
//      return;
//    }
//    // remember known sources
//    for (ChangeNotice changeNotice : notices) {
//      Source source = changeNotice.getSource();
//      log("\tsource: %s", source);
//      knownSources.add(source);
//    }
//    // index units
//    for (ChangeNotice changeNotice : notices) {
//      CompilationUnit dartUnit = changeNotice.getCompilationUnit();
//      if (dartUnit != null) {
//        index.indexUnit(context, dartUnit);
//      }
//    }
//    // schedule analysis again
//    schedulePerformAnalysisOperation(contextId, true);
//    // schedule notifications
//    Map<NotificationKind, SourceSetBasedProvider> notifications = notificationMap.get(contextId);
//    log("\tnotifications: %s", notifications);
//    if (notifications != null) {
//      for (Entry<NotificationKind, SourceSetBasedProvider> entry : notifications.entrySet()) {
//        NotificationKind notificationKind = entry.getKey();
//        SourceSetBasedProvider sourceProvider = entry.getValue();
//        for (ChangeNotice changeNotice : notices) {
//          Source source = changeNotice.getSource();
//          if (sourceProvider.apply(source)) {
//            log("\tadd NotificationOperation: %s with %s", notificationKind, changeNotice);
//            operationQueue.add(new NotificationOperation(contextId, changeNotice, notificationKind));
//          }
//        }
//      }
//    }
//  }
//
//  /**
//   * Implementation for {@link #searchClassMemberDeclarations(String, SearchResultsConsumer)}.
//   */
//  public void internalSearchClassMemberDeclarations(String name, SearchResultsConsumer consumer)
//      throws Exception {
//    new ClassMemberDeclarationsComputer(searchEngine, contextToIdFunction, name, consumer).compute();
//    consumer.computed(SearchResult.EMPTY_ARRAY, true);
//  }
//
//  /**
//   * Implementation for {@link #searchClassMemberReferences(String, SearchResultsConsumer)}.
//   */
//  public void internalSearchClassMemberReferences(String name, SearchResultsConsumer consumer)
//      throws Exception {
//    new ClassMemberReferencesComputer(searchEngine, contextToIdFunction, name, consumer).compute();
//    consumer.computed(SearchResult.EMPTY_ARRAY, true);
//  }
//
//  /**
//   * Implementation for {@link #searchElementReferences(Element, boolean, SearchResultsConsumer)}.
//   */
//  public void internalSearchElementReferences(String contextId, Element element,
//      boolean withPotential, SearchResultsConsumer consumer) throws Exception {
//    AnalysisContext context = getAnalysisContext(contextId);
//    new ElementReferencesComputer(
//        searchEngine,
//        contextToIdFunction,
//        context,
//        element,
//        withPotential,
//        consumer).compute();
//    consumer.computed(SearchResult.EMPTY_ARRAY, true);
//  }
//
//  /**
//   * Implementation for {@link #searchTopLevelDeclarations(String, String, SearchResultsConsumer)}.
//   */
//  public void internalSearchTopLevelDeclarations(String contextId, String pattern,
//      SearchResultsConsumer consumer) throws Exception {
//    AnalysisContext context = contextId != null ? getAnalysisContext(contextId) : null;
//    new TopLevelDeclarationsComputer(searchEngine, contextToIdFunction, context, pattern, consumer).compute();
//    consumer.computed(SearchResult.EMPTY_ARRAY, true);
//  }
//
//  /**
//   * Implementation for {@link #setOptions(String, AnalysisOptions)}.
//   */
//  public void internalSetOptions(String contextId, AnalysisOptions options) throws Exception {
//    AnalysisContext context = getAnalysisContext(contextId);
//    context.setAnalysisOptions(options);
//    schedulePerformAnalysisOperation(contextId, false);
//  }
//
//  /**
//   * Implementation for {@link #setPrioritySources(String, Source[])}.
//   */
//  public void internalSetPrioritySources(String contextId, Source[] sources) throws Exception {
//    AnalysisContext context = getAnalysisContext(contextId);
//    context.setAnalysisPriorityOrder(Lists.newArrayList(sources));
//    schedulePerformAnalysisOperation(contextId, false);
//  }
//
//  /**
//   * Implementation for {@link #setRefactoringExtractLocalOptions}.
//   */
//  public void internalSetRefactoringExtractLocalOptions(String refactoringId,
//      boolean allOccurrences, String name, RefactoringOptionsValidationConsumer consumer) {
//    ExtractLocalRefactoring refactoring = (ExtractLocalRefactoring) getRefactoring(refactoringId);
//    refactoring.setReplaceAllOccurrences(allOccurrences);
//    refactoring.setLocalName(name);
//    RefactoringStatus status = refactoring.checkLocalName(name);
//    consumer.computed(status);
//  }
//
//  /**
//   * Implementation for {@link #setRefactoringExtractMethodOptions}.
//   */
//  public void internalSetRefactoringExtractLocalOptions(String refactoringId, String name,
//      boolean asGetter, boolean allOccurrences, Parameter[] parameters,
//      RefactoringExtractMethodOptionsValidationConsumer consumer) {
//    ExtractMethodRefactoring refactoring = (ExtractMethodRefactoring) getRefactoring(refactoringId);
//    refactoring.setMethodName(name);
//    refactoring.setReplaceAllOccurrences(allOccurrences);
//    refactoring.setExtractGetter(asGetter);
//    refactoring.setParameters(parameters);
//    RefactoringStatus status = refactoring.checkMethodName();
//    String signature = refactoring.getSignature();
//    consumer.computed(status, signature);
//  }
//
//  /**
//   * Implementation for {@link #subscribe(String, Map)}.
//   */
//  public void internalSubscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions)
//      throws Exception {
//    log("internalSubscribe: %s", subscriptions);
//    AnalysisContext analysisContext = getAnalysisContext(contextId);
//    Set<Source> knownSources = getSourcesMap(contextId, contextKnownSourcesMap);
//    Set<Source> addedSources = getSourcesMap(contextId, contextAddedSourcesMap);
//    Map<NotificationKind, SourceSetBasedProvider> notifications = notificationMap.get(contextId);
//    if (notifications == null) {
//      notifications = Maps.newHashMap();
//      notificationMap.put(contextId, notifications);
//    }
//    // prepare new sources to send notifications for
//    for (Entry<NotificationKind, SourceSet> entry : subscriptions.entrySet()) {
//      NotificationKind kind = entry.getKey();
//      SourceSet sourceSet = entry.getValue();
//      SourceSetBasedProvider oldProvider = notifications.get(kind);
//      SourceSetBasedProvider newProvider = new SourceSetBasedProvider(
//          sourceSet,
//          knownSources,
//          addedSources);
//      // schedule notification operations for new sources
//      Set<Source> newSources = newProvider.computeNewSources(oldProvider);
//      log("\tnewSources: %s", newSources);
//      for (Source unitSource : newSources) {
//        log("\tunitSource: %s", unitSource);
//        Source[] librarySources = analysisContext.getLibrariesContaining(unitSource);
//        log("\t\tlibrarySources: %s", librarySources.length);
//        if (librarySources.length != 0) {
//          Source librarySource = librarySources[0];
//          log("\t\tlibrarySource: %s", librarySource);
//          CompilationUnit unit = analysisContext.resolveCompilationUnit(unitSource, librarySource);
//          log("\t\tunit: %s", unit);
//          if (unit != null) {
//            AnalysisErrorInfo errorsInfo = analysisContext.getErrors(unitSource);
//            ChangeNoticeImpl changeNotice = new ChangeNoticeImpl(unitSource);
//            changeNotice.setCompilationUnit(unit);
//            changeNotice.setErrors(errorsInfo.getErrors(), errorsInfo.getLineInfo());
//            log("\t\tadd NotificationOperation: %s with %s", kind, changeNotice);
//            operationQueue.add(new NotificationOperation(contextId, changeNotice, kind));
//          }
//        }
//      }
//      // put new provider
//      notifications.put(kind, newProvider);
//    }
//  }

  @Override
  public void deleteRefactoring(String refactoringId) {
  }

//  @Override
//  public void searchClassMemberDeclarations(String name, SearchResultsConsumer consumer) {
//    operationQueue.add(new SearchClassMemberDeclarationsOperation(name, consumer));
//  }
//
//  @Override
//  public void searchClassMemberReferences(String name, SearchResultsConsumer consumer) {
//    operationQueue.add(new SearchClassMemberReferencesOperation(name, consumer));
//  }
//
//  @Override
//  public void searchElementReferences(Element element, boolean withPotential,
//      SearchResultsConsumer consumer) {
//    operationQueue.add(new SearchElementReferencesOperation(element, withPotential, consumer));
//  }
//
//  @Override
//  public void searchTopLevelDeclarations(String contextId, String pattern,
//      SearchResultsConsumer consumer) {
//    operationQueue.add(new SearchTopLevelDeclarationsOperation(contextId, pattern, consumer));
//  }
//
//  @Override
//  public void setOptions(String contextId, AnalysisOptions options) {
//    operationQueue.add(new SetOptionsOperation(contextId, options));
//  }
//
//  @Override
//  public void setPrioritySources(String contextId, Source[] sources) {
//    operationQueue.add(new SetPrioritySourcesOperation(contextId, sources));
//    if (sources.length != 0) {
//      priorityContexts.add(contextId);
//    } else {
//      priorityContexts.remove(contextId);
//    }
//  }
//
//  @Override
//  public void setRefactoringExtractLocalOptions(String refactoringId, boolean allOccurrences,
//      String name, RefactoringOptionsValidationConsumer consumer) {
//    operationQueue.add(new SetRefactoringExtractLocalOptionsOperation(
//        refactoringId,
//        allOccurrences,
//        name,
//        consumer));
//  }
//
//  @Override
//  public void setRefactoringExtractMethodOptions(String refactoringId, String name,
//      boolean asGetter, boolean allOccurrences, Parameter[] parameters,
//      RefactoringExtractMethodOptionsValidationConsumer consumer) {
//    operationQueue.add(new SetRefactoringExtractMethodOptionsOperation(
//        refactoringId,
//        name,
//        asGetter,
//        allOccurrences,
//        parameters,
//        consumer));
//  }

  @Override
  public void getAssists(String file, int offset, int length, AssistsConsumer consumer) {
  }

//  @Override
//  public void subscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions) {
//    operationQueue.add(new SubscribeOperation(contextId, subscriptions));
//  }

  @Override
  public void getCompletionSuggestions(String file, int offset, CompletionIdConsumer consumer) {
  }

  @Override
  public void getFixes(List<AnalysisError> errors, FixesConsumer consumer) {
  }

  @Override
  public Index getIndex() {
    return null;
//    return index;
  }

  @Override
  public void getTypeHierarchy(Element element, TypeHierarchyConsumer consumer) {
  }

//  /**
//   * Sets the {@link List} to record analyzed contexts into.
//   */
//  public void test_setAnalyzedContexts(List<String> analyzedContexts) {
//    test_analyzedContexts = analyzedContexts;
//  }

  @Override
  public void getVersion(VersionConsumer consumer) {
    operationQueue.add(new GetVersionOperation(consumer));
  }

  /**
   * Implementation for {@link #getVersion(String)}.
   */
  public void internalGetVersion(VersionConsumer consumer) throws Exception {
    consumer.computedVersion(VERSION);
  }

  @Override
  public void reanalyze() {
    // TODO(jwren) not yet implemented
  }

//  /**
//   * Returns the {@link AnalysisContext} for the given identifier, not null.
//   * 
//   * @throws AnalysisServerErrorException if there are no context with the given identifier
//   */
//  private AnalysisContext getAnalysisContext(String contextId) {
//    AnalysisContext context = contextMap.get(contextId);
//    if (context == null) {
//      throw new AnalysisServerErrorException(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
//    }
//    return context;
//  }
//
//  /**
//   * Returns the {@link Refactoring} for the given identifier, not {@code null}.
//   * 
//   * @throws AnalysisServerErrorException if there are no refactoring with the given identifier
//   */
//  private Refactoring getRefactoring(String contextId) {
//    Refactoring refactoring = refactoringMap.get(contextId);
//    if (refactoring == null) {
//      throw new AnalysisServerErrorException(
//          AnalysisServerErrorCode.INVALID_REFACTORING_ID,
//          contextId);
//    }
//    return refactoring;
//  }
//
//  private DartSdk getSdk(String contextId, String directory) {
//    DartSdk sdk = sdkMap.get(directory);
//    if (sdk == null) {
//      File directoryFile = new File(directory);
//      sdk = new DirectoryBasedDartSdk(directoryFile);
//      sdkMap.put(directory, sdk);
//      AnalysisContext context = sdk.getContext();
//      SourceFactory factory = context.getSourceFactory();
//      AnalysisDelta delta = new AnalysisDelta();
//      for (String uri : sdk.getUris()) {
//        delta.setAnalysisLevel(factory.forUri(uri), AnalysisLevel.RESOLVED);
//      }
//      context.applyAnalysisDelta(delta);
//      String sdkContextId = "dart-sdk-internal-" + nextContextId++;
//      contextMap.put(sdkContextId, context);
//      schedulePerformAnalysisOperation(sdkContextId, false);
//    }
//    return sdk;
//  }
//
//  /**
//   * Returns an existing or just added {@link Source} set associated with the given context.
//   */
//  private Set<Source> getSourcesMap(String contextId, Map<String, Set<Source>> contextSourcesMap) {
//    Set<Source> sources = contextSourcesMap.get(contextId);
//    if (sources == null) {
//      sources = Sets.newHashSet();
//      contextSourcesMap.put(contextId, sources);
//    }
//    return sources;
//  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
  }

  @Override
  public void searchClassMemberDeclarations(String name, SearchResultsConsumer consumer) {
  }

  @Override
  public void searchClassMemberReferences(String name, SearchResultsConsumer consumer) {
  }

  @Override
  public void searchElementReferences(Element element, boolean withPotential,
      SearchResultsConsumer consumer) {
  }

  @Override
  public void searchTopLevelDeclarations(String pattern, SearchResultsConsumer consumer) {
  }

  @Override
  public void setAnalysisRoots(List<String> includedPaths, List<String> excludedPaths) {
  }

  @Override
  public void setAnalysisSubscriptions(Map<AnalysisService, List<String>> subscriptions) {
  }

  @Override
  public void setPriorityFiles(List<String> files) {
  }

  @Override
  public void setRefactoringExtractLocalOptions(String refactoringId, boolean allOccurrences,
      String name, RefactoringOptionsValidationConsumer consumer) {
  }

  @Override
  public void setRefactoringExtractMethodOptions(String refactoringId, String name,
      boolean asGetter, boolean allOccurrences, Parameter[] parameters,
      RefactoringExtractMethodOptionsValidationConsumer consumer) {
  }

  @Override
  public void setServerSubscriptions(List<ServerService> subscriptions) {
  }

  @Override
  public void shutdown() {
    operationQueue.add(ShutdownOperation.INSTANCE);
//    index.stop();
  }

  @VisibleForTesting
  public void test_addOperation(ServerOperation operation) {
    operationQueue.add(operation);
  }

  @VisibleForTesting
  public Map<String, Refactoring> test_getRefactoringMap() {
    return refactoringMap;
  }

  @VisibleForTesting
  public void test_pingListeners() {
    listener.computedErrors(null, null);
  }

  @VisibleForTesting
  public void test_setLog(boolean log) {
    this.test_log = log;
  }

  @VisibleForTesting
  public void test_setPaused(boolean paused) {
    this.test_paused = paused;
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!operationQueue.isEmpty()) {
      Thread.yield();
    }
  }

  @Override
  public void updateAnalysisOptions(AnalysisOptions options) {
  }

  @Override
  public void updateContent(Map<String, ContentChange> files) {
  }

  @Override
  public void updateSdks(List<String> added, List<String> removed, String defaultSdk) {
  }

  private void log(String msg, Object... arguments) {
    if (test_log) {
      String message = String.format(msg, arguments);
      System.out.println(message);
    }
  }

  /**
   * Schedules analysis for the given context.
   * 
   * @param isContinue is {@code true} if the new operation is continuation of analysis of the same
   *          contexts which was analyzed before.
   */
  private void schedulePerformAnalysisOperation(String contextId, boolean isContinue) {
    boolean isPriority = priorityContexts.contains(contextId);
    operationQueue.add(new PerformAnalysisOperation(contextId, isPriority, isContinue));
  }
}
