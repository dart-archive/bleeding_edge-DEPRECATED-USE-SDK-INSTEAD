// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library analyzer.src.task.dart_work_manager;

import 'dart:collection';

import 'package:analyzer/src/context/cache.dart';
import 'package:analyzer/src/generated/engine.dart'
    show
        AnalysisEngine,
        AnalysisErrorInfo,
        AnalysisErrorInfoImpl,
        CacheState,
        InternalAnalysisContext;
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:analyzer/src/generated/utilities_collection.dart';
import 'package:analyzer/src/task/dart.dart';
import 'package:analyzer/src/task/driver.dart';
import 'package:analyzer/task/dart.dart';
import 'package:analyzer/task/general.dart';
import 'package:analyzer/task/model.dart';

/**
 * The manager for Dart specific analysis.
 */
class DartWorkManager implements WorkManager {
  /**
   * The list of errors that are reported for raw Dart [Source]s.
   */
  static final List<ResultDescriptor> _SOURCE_ERRORS = <ResultDescriptor>[
    BUILD_DIRECTIVES_ERRORS,
    BUILD_LIBRARY_ERRORS,
    PARSE_ERRORS,
    SCAN_ERRORS
  ];

  /**
   * The list of errors that are reported for raw Dart [LibrarySpecificUnit]s.
   */
  static final List<ResultDescriptor> _UNIT_ERRORS = <ResultDescriptor>[
    BUILD_FUNCTION_TYPE_ALIASES_ERRORS,
    HINTS,
    RESOLVE_REFERENCES_ERRORS,
    RESOLVE_TYPE_NAMES_ERRORS,
    VERIFY_ERRORS
  ];

  final InternalAnalysisContext context;

  /**
   * The [TargetedResult]s that should be computed with priority.
   */
  final LinkedHashSet<TargetedResult> priorityResultQueue =
      new LinkedHashSet<TargetedResult>();

  /**
   * The sources whose kind we don't know yet.
   */
  final LinkedHashSet<Source> unknownSourceQueue = new LinkedHashSet<Source>();

  /**
   * The queue of library sources to process.
   */
  final LinkedHashSet<Source> librarySourceQueue = new LinkedHashSet<Source>();

  /**
   * Initialize a newly created manager.
   */
  DartWorkManager(this.context);

  /**
   * Returns the correctly typed result of `context.analysisCache`.
   */
  AnalysisCache get analysisCache => context.analysisCache;

  /**
   * Specifies that the client want the given [result] of the given [target]
   * to be computed with priority.
   */
  void addPriorityResult(AnalysisTarget target, ResultDescriptor result) {
    priorityResultQueue.add(new TargetedResult(target, result));
  }

  /**
   * Notifies the manager about changes in the explicit source list.
   */
  void applyChange(List<Source> addedSources, List<Source> changedSources,
      List<Source> removedSources) {
    addedSources = addedSources.where(_isDartSource).toList();
    changedSources = changedSources.where(_isDartSource).toList();
    removedSources = removedSources.where(_isDartSource).toList();
    // unknown queue
    unknownSourceQueue.addAll(addedSources);
    unknownSourceQueue.addAll(changedSources);
    unknownSourceQueue.removeAll(removedSources);
    // library queue
    librarySourceQueue.removeAll(changedSources);
    librarySourceQueue.removeAll(removedSources);
    // Some of the libraries might have been invalidated, reschedule them.
    {
      MapIterator<AnalysisTarget, CacheEntry> iterator =
          analysisCache.iterator();
      while (iterator.moveNext()) {
        AnalysisTarget target = iterator.key;
        if (_isDartSource(target)) {
          CacheEntry entry = iterator.value;
          if (entry.getValue(SOURCE_KIND) == SourceKind.LIBRARY &&
              entry.getValue(LIBRARY_ERRORS_READY) != true) {
            librarySourceQueue.add(target);
          }
        }
      }
    }
  }

  @override
  void applyPriorityTargets(List<AnalysisTarget> targets) {
    // Unschedule the old targets.
    List<TargetedResult> resultsToUnschedule = <TargetedResult>[];
    for (TargetedResult result in priorityResultQueue) {
      if (result.result == LIBRARY_ERRORS_READY) {
        resultsToUnschedule.add(result);
      }
    }
    priorityResultQueue.removeAll(resultsToUnschedule);
    // Schedule new targets.
    for (AnalysisTarget target in targets) {
      if (_isDartSource(target)) {
        SourceKind sourceKind = analysisCache.getValue(target, SOURCE_KIND);
        if (sourceKind == SourceKind.LIBRARY) {
          addPriorityResult(target, LIBRARY_ERRORS_READY);
        } else if (sourceKind == SourceKind.PART) {
          List<Source> libraries = context.getLibrariesContaining(target);
          for (Source library in libraries) {
            addPriorityResult(library, LIBRARY_ERRORS_READY);
          }
        }
      }
    }
  }

  /**
   * Return an [AnalysisErrorInfo] containing the list of all of the errors and
   * the line info associated with the given [source]. The list of errors will
   * be empty if the source is not known to the context or if there are no
   * errors in the source. The errors contained in the list can be incomplete.
   */
  AnalysisErrorInfo getErrors(Source source) {
    List<AnalysisError> errors = <AnalysisError>[];
    for (ResultDescriptor descriptor in _SOURCE_ERRORS) {
      errors.addAll(analysisCache.getValue(source, descriptor));
    }
    for (Source library in context.getLibrariesContaining(source)) {
      LibrarySpecificUnit unit = new LibrarySpecificUnit(library, source);
      for (ResultDescriptor descriptor in _UNIT_ERRORS) {
        errors.addAll(analysisCache.getValue(unit, descriptor));
      }
    }
    LineInfo lineInfo = analysisCache.getValue(source, LINE_INFO);
    return new AnalysisErrorInfoImpl(errors, lineInfo);
  }

  @override
  TargetedResult getNextResult() {
    // Try to find a priority result to compute.
    while (priorityResultQueue.isNotEmpty) {
      TargetedResult result = priorityResultQueue.first;
      if (!_needsComputing(result.target, result.result)) {
        priorityResultQueue.remove(result);
        continue;
      }
      return result;
    }
    // Try to find a new library to analyze.
    while (librarySourceQueue.isNotEmpty) {
      Source librarySource = librarySourceQueue.first;
      // Maybe done with this library.
      if (!_needsComputing(librarySource, LIBRARY_ERRORS_READY)) {
        librarySourceQueue.remove(librarySource);
        continue;
      }
      // Analyze this library.
      return new TargetedResult(librarySource, LIBRARY_ERRORS_READY);
    }
    // No libraries in the queue, check whether there are sources to organize.
    while (unknownSourceQueue.isNotEmpty) {
      Source source = unknownSourceQueue.first;
      // Maybe done with this source.
      if (!_needsComputing(source, SOURCE_KIND)) {
        unknownSourceQueue.remove(source);
        continue;
      }
      // Compute the kind of this source.
      return new TargetedResult(source, SOURCE_KIND);
    }
    // TODO(scheglov) Report errors for parts that remained in the queue after
    // all libraries had been processed.
    // No results to compute.
    return null;
  }

  @override
  WorkOrderPriority getNextResultPriority() {
    if (priorityResultQueue.isNotEmpty) {
      return WorkOrderPriority.PRIORITY;
    }
    if (unknownSourceQueue.isNotEmpty || librarySourceQueue.isNotEmpty) {
      return WorkOrderPriority.NORMAL;
    }
    return WorkOrderPriority.NONE;
  }

  @override
  void resultsComputed(
      AnalysisTarget target, Map<ResultDescriptor, dynamic> outputs) {
    // Organize sources.
    if (_isDartSource(target)) {
      SourceKind kind = outputs[SOURCE_KIND];
      if (kind != null) {
        unknownSourceQueue.remove(target);
        if (kind == SourceKind.LIBRARY) {
          librarySourceQueue.add(target);
        }
      }
    }
    // Update notice.
    if (_isDartSource(target)) {
      bool shouldSetErrors = false;
      outputs.forEach((ResultDescriptor descriptor, value) {
        if (descriptor == PARSED_UNIT && value != null) {
          context.getNotice(target).parsedDartUnit = value;
          shouldSetErrors = true;
        }
        if (_isErrorResult(descriptor)) {
          shouldSetErrors = true;
        }
      });
      if (shouldSetErrors) {
        AnalysisErrorInfo info = getErrors(target);
        context.getNotice(target).setErrors(info.errors, info.lineInfo);
      }
    }
    if (target is LibrarySpecificUnit) {
      Source source = target.source;
      bool shouldSetErrors = false;
      outputs.forEach((ResultDescriptor descriptor, value) {
        if (descriptor == RESOLVED_UNIT && value != null) {
          context.getNotice(source).resolvedDartUnit = value;
          shouldSetErrors = true;
        }
        if (_isErrorResult(descriptor)) {
          shouldSetErrors = true;
        }
      });
      if (shouldSetErrors) {
        AnalysisErrorInfo info = getErrors(source);
        context.getNotice(source).setErrors(info.errors, info.lineInfo);
      }
    }
  }

  /**
   * Returns `true` if the given [result] of the given [target] needs
   * computing, i.e. it is not in the valid and not in the error state.
   */
  bool _needsComputing(AnalysisTarget target, ResultDescriptor result) {
    CacheState state = analysisCache.getState(target, result);
    return state != CacheState.VALID && state != CacheState.ERROR;
  }

  static bool _isDartSource(AnalysisTarget target) {
    return target is Source && AnalysisEngine.isDartFileName(target.fullName);
  }

  static bool _isErrorResult(ResultDescriptor descriptor) {
    return _SOURCE_ERRORS.contains(descriptor) ||
        _UNIT_ERRORS.contains(descriptor);
  }
}
