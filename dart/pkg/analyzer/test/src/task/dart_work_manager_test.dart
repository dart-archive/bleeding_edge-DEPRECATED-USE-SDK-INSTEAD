// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.src.task.dart_work_manager_test;

import 'package:analyzer/src/context/cache.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/engine.dart'
    show
        AnalysisErrorInfo,
        CacheState,
        ChangeNoticeImpl,
        InternalAnalysisContext;
import 'package:analyzer/src/generated/error.dart' show AnalysisError;
import 'package:analyzer/src/generated/java_engine.dart' show CaughtException;
import 'package:analyzer/src/generated/scanner.dart' show ScannerErrorCode;
import 'package:analyzer/src/generated/source.dart';
import 'package:analyzer/src/generated/testing/ast_factory.dart';
import 'package:analyzer/src/task/dart.dart';
import 'package:analyzer/src/task/dart_work_manager.dart';
import 'package:analyzer/src/task/driver.dart';
import 'package:analyzer/task/dart.dart';
import 'package:analyzer/task/general.dart';
import 'package:analyzer/task/model.dart';
import 'package:typed_mock/typed_mock.dart';
import 'package:unittest/unittest.dart';

import '../../generated/test_support.dart';
import '../../reflective_tests.dart';

main() {
  groupSep = ' | ';
  runReflectiveTests(DartWorkManagerTest);
}

@reflectiveTest
class DartWorkManagerTest {
  InternalAnalysisContext context = new _InternalAnalysisContextMock();
  DartWorkManager manager;

  CaughtException caughtException = new CaughtException(null, null);

  Source source1 = new TestSource('1.dart');
  Source source2 = new TestSource('2.dart');
  Source source3 = new TestSource('3.dart');
  Source source4 = new TestSource('4.dart');
  CacheEntry entry1;
  CacheEntry entry2;
  CacheEntry entry3;
  CacheEntry entry4;

  void expect_librarySourceQueue(List<Source> sources) {
    expect(manager.librarySourceQueue, unorderedEquals(sources));
  }

  void expect_unknownSourceQueue(List<Source> sources) {
    expect(manager.unknownSourceQueue, unorderedEquals(sources));
  }

  void setUp() {
    manager = new DartWorkManager(context);
    entry1 = context.getCacheEntry(source1);
    entry2 = context.getCacheEntry(source2);
    entry3 = context.getCacheEntry(source3);
    entry4 = context.getCacheEntry(source4);
  }

  void test_applyChange_add() {
    // add source1
    manager.applyChange([source1], [], []);
    expect_unknownSourceQueue([source1]);
    expect_librarySourceQueue([]);
    // add source2
    manager.applyChange([source2], [], []);
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1, source2]);
  }

  void test_applyChange_add_duplicate() {
    // add source1
    manager.applyChange([source1], [], []);
    expect_unknownSourceQueue([source1]);
    expect_librarySourceQueue([]);
    // add source2
    manager.applyChange([source1], [], []);
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1]);
  }

  void test_applyChange_addRemove() {
    manager.applyChange([source1, source2], [], [source2, source3]);
    expect_unknownSourceQueue([source1]);
    expect_librarySourceQueue([]);
  }

  void test_applyChange_change() {
    manager.librarySourceQueue.addAll([source1, source3]);
    manager.unknownSourceQueue.addAll([source4]);
    // change source1
    manager.applyChange([], [source1], []);
    expect_librarySourceQueue([source3]);
    expect_unknownSourceQueue([source4, source1]);
  }

  void test_applyChange_remove() {
    manager.librarySourceQueue.addAll([source1, source3]);
    manager.unknownSourceQueue.addAll([source4]);
    // remove source1
    manager.applyChange([], [], [source1]);
    expect_librarySourceQueue([source3]);
    expect_unknownSourceQueue([source4]);
    // remove source3
    manager.applyChange([], [], [source3]);
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source4]);
    // remove source4
    manager.applyChange([], [], [source4]);
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([]);
  }

  void test_applyChange_scheduleInvalidatedLibraries() {
    // libraries source1 and source3 are invalid
    entry1.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    entry2.setValue(SOURCE_KIND, SourceKind.PART, []);
    entry3.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    entry1.setValue(LIBRARY_ERRORS_READY, false, []);
    entry3.setValue(LIBRARY_ERRORS_READY, false, []);
    // change source2, schedule source1 and source3
    manager.applyChange([], [source2], []);
    expect_librarySourceQueue([source1, source3]);
  }

  void test_applyPriorityTargets_library() {
    entry1.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    entry2.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    entry3.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    manager.priorityResultQueue
        .add(new TargetedResult(source1, LIBRARY_ERRORS_READY));
    manager.priorityResultQueue
        .add(new TargetedResult(source2, LIBRARY_ERRORS_READY));
    // -source1 +source3
    manager.applyPriorityTargets([source2, source3]);
    expect(manager.priorityResultQueue, unorderedEquals([
      new TargetedResult(source2, LIBRARY_ERRORS_READY),
      new TargetedResult(source3, LIBRARY_ERRORS_READY)
    ]));
    // get next request
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, LIBRARY_ERRORS_READY);
  }

  void test_applyPriorityTargets_part() {
    entry1.setValue(SOURCE_KIND, SourceKind.PART, []);
    entry2.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    entry3.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    // +source2 +source3
    when(context.getLibrariesContaining(source1))
        .thenReturn([source2, source3]);
    manager.applyPriorityTargets([source1]);
    expect(manager.priorityResultQueue, unorderedEquals([
      new TargetedResult(source2, LIBRARY_ERRORS_READY),
      new TargetedResult(source3, LIBRARY_ERRORS_READY)
    ]));
    // get next request
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, LIBRARY_ERRORS_READY);
  }

  void test_getErrors() {
    AnalysisError error1 =
        new AnalysisError(source1, 1, 0, ScannerErrorCode.MISSING_DIGIT);
    AnalysisError error2 =
        new AnalysisError(source1, 2, 0, ScannerErrorCode.MISSING_DIGIT);
    when(context.getLibrariesContaining(source1)).thenReturn([source2]);
    LineInfo lineInfo = new LineInfo([0]);
    entry1.setValue(LINE_INFO, lineInfo, []);
    entry1.setValue(SCAN_ERRORS, <AnalysisError>[error1], []);
    context.getCacheEntry(new LibrarySpecificUnit(source2, source1)).setValue(
        VERIFY_ERRORS, <AnalysisError>[error2], []);
    AnalysisErrorInfo errorInfo = manager.getErrors(source1);
    expect(errorInfo.errors, unorderedEquals([error1, error2]));
    expect(errorInfo.lineInfo, lineInfo);
  }

  void test_getNextResult_hasLibraries_firstIsError() {
    entry1.setErrorState(caughtException, [LIBRARY_ERRORS_READY]);
    manager.librarySourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, LIBRARY_ERRORS_READY);
    // source1 is out, source2 is waiting
    expect_librarySourceQueue([source2]);
  }

  void test_getNextResult_hasLibraries_firstIsInvalid() {
    entry1.setState(LIBRARY_ERRORS_READY, CacheState.INVALID);
    manager.librarySourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source1);
    expect(request.result, LIBRARY_ERRORS_READY);
    // no changes until computed
    expect_librarySourceQueue([source1, source2]);
  }

  void test_getNextResult_hasLibraries_firstIsValid() {
    entry1.setValue(LIBRARY_ERRORS_READY, true, []);
    manager.librarySourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, LIBRARY_ERRORS_READY);
    // source1 is out, source2 is waiting
    expect_librarySourceQueue([source2]);
  }

  void test_getNextResult_hasPriority_firstIsError() {
    manager.addPriorityResult(source1, SOURCE_KIND);
    manager.addPriorityResult(source2, SOURCE_KIND);
    expect(manager.priorityResultQueue, unorderedEquals([
      new TargetedResult(source1, SOURCE_KIND),
      new TargetedResult(source2, SOURCE_KIND)
    ]));
    // configure state and get next result
    entry1.setErrorState(caughtException, [SOURCE_KIND]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, SOURCE_KIND);
    // source1 is out, source2 is waiting
    expect(manager.priorityResultQueue,
        unorderedEquals([new TargetedResult(source2, SOURCE_KIND)]));
  }

  void test_getNextResult_hasPriority_firstIsValid() {
    manager.addPriorityResult(source1, SOURCE_KIND);
    manager.addPriorityResult(source2, SOURCE_KIND);
    expect(manager.priorityResultQueue, unorderedEquals([
      new TargetedResult(source1, SOURCE_KIND),
      new TargetedResult(source2, SOURCE_KIND)
    ]));
    // configure state and get next result
    entry1.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, SOURCE_KIND);
    // source1 is out, source2 is waiting
    expect(manager.priorityResultQueue,
        unorderedEquals([new TargetedResult(source2, SOURCE_KIND)]));
  }

  void test_getNextResult_hasUnknown_firstIsError() {
    entry1.setErrorState(caughtException, [SOURCE_KIND]);
    manager.unknownSourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, SOURCE_KIND);
    // source1 is out, source2 is waiting
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source2]);
  }

  void test_getNextResult_hasUnknown_firstIsInvalid() {
    manager.unknownSourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source1);
    expect(request.result, SOURCE_KIND);
    // no changes until computed
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1, source2]);
  }

  void test_getNextResult_hasUnknown_firstIsValid() {
    entry1.setValue(SOURCE_KIND, SourceKind.LIBRARY, []);
    manager.unknownSourceQueue.addAll([source1, source2]);
    TargetedResult request = manager.getNextResult();
    expect(request.target, source2);
    expect(request.result, SOURCE_KIND);
    // source1 is out, source2 is waiting
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source2]);
  }

  void test_getNextResult_nothingToDo() {
    TargetedResult request = manager.getNextResult();
    expect(request, isNull);
  }

  void test_getNextResultPriority_hasLibrary() {
    manager.librarySourceQueue.addAll([source1]);
    expect(manager.getNextResultPriority(), WorkOrderPriority.NORMAL);
  }

  void test_getNextResultPriority_hasPriority() {
    manager.addPriorityResult(source1, SOURCE_KIND);
    expect(manager.getNextResultPriority(), WorkOrderPriority.PRIORITY);
  }

  void test_getNextResultPriority_hasUnknown() {
    manager.unknownSourceQueue.addAll([source1]);
    expect(manager.getNextResultPriority(), WorkOrderPriority.NORMAL);
  }

  void test_getNextResultPriority_nothingToDo() {
    expect(manager.getNextResultPriority(), WorkOrderPriority.NONE);
  }

  void test_resultsComputed_errors_forLibrarySpecificUnit() {
    AnalysisError error1 =
        new AnalysisError(source1, 1, 0, ScannerErrorCode.MISSING_DIGIT);
    AnalysisError error2 =
        new AnalysisError(source1, 2, 0, ScannerErrorCode.MISSING_DIGIT);
    when(context.getLibrariesContaining(source1)).thenReturn([source2]);
    LineInfo lineInfo = new LineInfo([0]);
    entry1.setValue(LINE_INFO, lineInfo, []);
    entry1.setValue(SCAN_ERRORS, <AnalysisError>[error1], []);
    AnalysisTarget unitTarget = new LibrarySpecificUnit(source2, source1);
    context.getCacheEntry(unitTarget).setValue(
        VERIFY_ERRORS, <AnalysisError>[error2], []);
    // notify about LibrarySpecificUnit specific errors
    manager.resultsComputed(unitTarget, {VERIFY_ERRORS: []});
    // all of the errors are included
    ChangeNoticeImpl notice = context.getNotice(source1);
    expect(notice.errors, unorderedEquals([error1, error2]));
    expect(notice.lineInfo, lineInfo);
  }

  void test_resultsComputed_errors_forSource() {
    AnalysisError error1 =
        new AnalysisError(source1, 1, 0, ScannerErrorCode.MISSING_DIGIT);
    AnalysisError error2 =
        new AnalysisError(source1, 2, 0, ScannerErrorCode.MISSING_DIGIT);
    when(context.getLibrariesContaining(source1)).thenReturn([source2]);
    LineInfo lineInfo = new LineInfo([0]);
    entry1.setValue(LINE_INFO, lineInfo, []);
    entry1.setValue(SCAN_ERRORS, <AnalysisError>[error1], []);
    AnalysisTarget unitTarget = new LibrarySpecificUnit(source2, source1);
    context.getCacheEntry(unitTarget).setValue(
        VERIFY_ERRORS, <AnalysisError>[error2], []);
    // notify about Source specific errors
    manager.resultsComputed(source1, {SCAN_ERRORS: []});
    // all of the errors are included
    ChangeNoticeImpl notice = context.getNotice(source1);
    expect(notice.errors, unorderedEquals([error1, error2]));
    expect(notice.lineInfo, lineInfo);
  }

  void test_resultsComputed_noSourceKind() {
    manager.unknownSourceQueue.addAll([source1, source2]);
    manager.resultsComputed(source1, {});
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1, source2]);
  }

  void test_resultsComputed_notDart() {
    manager.unknownSourceQueue.addAll([source1, source2]);
    manager.resultsComputed(new TestSource('test.html'), {});
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1, source2]);
  }

  void test_resultsComputed_parsedUnit() {
    when(context.getLibrariesContaining(source1)).thenReturn([]);
    LineInfo lineInfo = new LineInfo([0]);
    entry1.setValue(LINE_INFO, lineInfo, []);
    CompilationUnit unit = AstFactory.compilationUnit();
    manager.resultsComputed(source1, {PARSED_UNIT: unit});
    ChangeNoticeImpl notice = context.getNotice(source1);
    expect(notice.parsedDartUnit, unit);
    expect(notice.resolvedDartUnit, isNull);
    expect(notice.lineInfo, lineInfo);
  }

  void test_resultsComputed_resolvedUnit() {
    when(context.getLibrariesContaining(source2)).thenReturn([]);
    LineInfo lineInfo = new LineInfo([0]);
    entry2.setValue(LINE_INFO, lineInfo, []);
    CompilationUnit unit = AstFactory.compilationUnit();
    manager.resultsComputed(
        new LibrarySpecificUnit(source1, source2), {RESOLVED_UNIT: unit});
    ChangeNoticeImpl notice = context.getNotice(source2);
    expect(notice.parsedDartUnit, isNull);
    expect(notice.resolvedDartUnit, unit);
    expect(notice.lineInfo, lineInfo);
  }

  void test_resultsComputed_sourceKind_isLibrary() {
    manager.unknownSourceQueue.addAll([source1, source2, source3]);
    manager.resultsComputed(source2, {SOURCE_KIND: SourceKind.LIBRARY});
    expect_librarySourceQueue([source2]);
    expect_unknownSourceQueue([source1, source3]);
  }

  void test_resultsComputed_sourceKind_isPart() {
    manager.unknownSourceQueue.addAll([source1, source2, source3]);
    manager.resultsComputed(source2, {SOURCE_KIND: SourceKind.PART});
    expect_librarySourceQueue([]);
    expect_unknownSourceQueue([source1, source3]);
  }
}

class _InternalAnalysisContextMock extends TypedMock
    implements InternalAnalysisContext {
  @override
  AnalysisCache analysisCache;

  Map<Source, ChangeNoticeImpl> _pendingNotices = <Source, ChangeNoticeImpl>{};

  _InternalAnalysisContextMock() {
    analysisCache = new AnalysisCache([new UniversalCachePartition(this)]);
  }

  @override
  CacheEntry getCacheEntry(AnalysisTarget target) {
    CacheEntry entry = analysisCache.get(target);
    if (entry == null) {
      entry = new CacheEntry(target);
      analysisCache.put(entry);
    }
    return entry;
  }

  @override
  ChangeNoticeImpl getNotice(Source source) {
    return _pendingNotices.putIfAbsent(
        source, () => new ChangeNoticeImpl(source));
  }

  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
