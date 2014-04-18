// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage.test;

import 'package:unittest/unittest.dart';

import '../lib/numeric_list_parser.dart' as nlp;
import '../lib/coverage_reader.dart' as rdr;
import '../lib/coverage_data.dart';

/// Tests for non-UI elements of the coverage browser.
main() {
  group('Numeric List Parser:', () {
    test('Parse empty list', _testParseEmpty);
    test('Parse int list', _testParseInts);
    test('Parse double list', _testParseDoubles);
    test('Parse non-list', _testParseNonList);
    test('Parse non-int', _testParseNonInt);
    test('Parse non-double', _testParseNonDouble);
  });
  group('Coverage Data:', () {
    test('Class name is set', _testClassName);
    test('Visited length < instrumented length', _testData);
    test('Percentage 1x100%', _test1x100);
    test('Percentage 2x50%', _test2x50);
    test('Percentage 3x100%', _test3x100);
    test('Percentage 0x0%', _test0x0);
    test('Percentage 3x0%', _test3x0);
    test('Merge two partial', _testMerge1);
    test('Merge with empty', _testMerge2);
    test('Merge out of order', _testMerge3);
  });
  group('Package Data:', () {
    test('Create package', _testPackageCreation);
  });
  group('Coverage Reader:', () {
    test('Null data', _testDataNull);
    test('Class with no data', _testDataEmptyClass);
    test('Class not visited', _testDataClassNotVisited);
    test('Class visited', _testDataClassVisited);
  });
}

_testParseEmpty() {
  expect(nlp.parseNumericList('[]'), []);
}

_testParseInts() {
  var list = '[0,1,2,3,4,5,6,7,8,9,-1,23,-987654321,1e7,99E3]';
  expect(nlp.parseNumericList(list), [0,1,2,3,4,5,6,7,8,9,-1,23,-987654321,1e7,99e3]);
}

_testParseDoubles() {
  var list = '[0.0,-1.2,2.7e-5]';
  expect(nlp.parseNumericList(list), [0.0,-1.2,2.7e-5]);
}

_testParseNonList() {
  var list = '123';
  expect(() => nlp.parseNumericList(list), throwsException);
}

_testParseNonInt() {
  var list = '[0x37]';
  expect(() => nlp.parseNumericList(list), throwsException);
}

_testParseNonDouble() {
  var list = '[4.2.3]';
  expect(() => nlp.parseNumericList(list), throwsException);
}

_testClassName() {
  CoverageData data = new CoverageData('Sample', [1], [1]);
  expect(data.className, "Sample");
}

_testData() {
  expect(() => new CoverageData('Sample', [], [1]), throwsException);
}
_test1x100() {
  CoverageData data = new CoverageData('100% of one', [1], [1]);
  expect(data.percentCovered, 100);
}

_test2x50() {
  CoverageData data = new CoverageData('50% of two', [1,2], [1]);
  expect(data.percentCovered, 50);
}

_test3x100() {
  CoverageData data = new CoverageData('100% of three', [1,2,3], [1,2,3]);
  expect(data.percentCovered, 100);
}

_test0x0() {
  CoverageData data = new CoverageData('0% of none', [], []);
  expect(data.percentCovered, 0);
}

_test3x0() {
  CoverageData data = new CoverageData('0% of three', [1,2,3], []);
  expect(data.percentCovered, 0);
}

_testMerge1() {
  CoverageData dst = new CoverageData('Merge 1', [1,2], [1]);
  CoverageData src = new CoverageData('Merge 1', [1,2], [2]);
  dst.merge(src, true);
  expect(dst.percentCovered, 100);
}

_testMerge2() {
  CoverageData dst = new CoverageData('Merge 1', [1,2,3,4], []);
  CoverageData src = new CoverageData('Merge 1', [1,2,3,4], [1,2]);
  dst.merge(src, true);
  expect(dst.percentCovered, 50);
}

_testMerge3() {
  CoverageData dst = new CoverageData('Merge 1', [1,2,3,4,5], [3,1]);
  CoverageData src = new CoverageData('Merge 1', [1,2,3,4,5], [4,2]);
  dst.merge(src);
  expect(dst.visitedLines, [1,2,3,4]);
}

_testPackageCreation() {
  CoverageData dst = new CoverageData('Merge 1', [1,2,3,4], [3,1]);
  CoverageData src = new CoverageData('Merge 1', [1,2,3,4], [4,2]);
  PackageData pkg = new PackageData('PackageName', 0, 0);
  pkg.merge(dst);
  pkg.merge(src);
  expect(pkg.percentCovered, 50);
}

_testDataNull() {
  expect(() => rdr.processXml(''), throwsException);
}

_testDataEmptyClass() {
  var data = '''
<CoverageData>
  <ClassData name="com/google/dart/command/analyze/AnalyzerImpl">
    <Lines values="[]"/>
    <VisitedLines values="[]"/>
  </ClassData>
</CoverageData>
''';
  var name = 'com.google.dart.command.analyze.AnalyzerImpl';
  var map;
  expect(map = rdr.processXml(data), isNot(isNull));
  expect(map.length, 1);
  var cov;
  expect((cov = map[name]).className, name);
  expect(cov.visitedLines.length, 0);
  expect(cov.instrumentedLines.length, 0);
  expect(cov.percentCovered, 0);
}

_testDataClassNotVisited() {
  var data = '''
<CoverageData>
  <ClassData name="com/google/dart/engine/internal/task/IncrementalAnalysisTask">
    <Lines values="[54, 55, 56, 60, 69, 80, 89, 94, 99, 100, 104, 105, 109, 110,
111, 112, 113, 114, 115, 116, 117, 118, 122, 123, 124, 125, 126, 127, 128, 129,
130, 131, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 149, 158,
159, 160]"/>
    <VisitedLines values="[]"/>
  </ClassData>
</CoverageData>
''';
  var name = 'com.google.dart.engine.internal.task.IncrementalAnalysisTask';
  var map;
  expect(map = rdr.processXml(data), isNot(isNull));
  expect(map.length, 1);
  var cov;
  expect((cov = map[name]).className, name);
  expect(cov.visitedLines.length, 0);
  expect(cov.instrumentedLines.length, 48);
  expect(cov.percentCovered, 0);
}

_testDataClassVisited() {
  var data = '''
<CoverageData>
  <ClassData name="com/google/dart/engine/internal/search/pattern/PrefixSearchPattern">
    <Lines values="[47, 48, 49, 50, 54, 55, 57, 58, 59, 61, 62, 64, 65, 67]"/>
    <VisitedLines values="[47, 48, 49, 50, 54, 57, 58, 61, 64, 65, 67]"/>
  </ClassData>
</CoverageData>
''';
  var name = 'com.google.dart.engine.internal.search.pattern.PrefixSearchPattern';
  var map;
  expect(map = rdr.processXml(data), isNot(isNull));
  expect(map.length, 1);
  var cov;
  expect((cov = map[name]).className, name);
  expect(cov.visitedLines.length, 11);
  expect(cov.instrumentedLines.length, 14);
  expect(cov.percentCovered, 79);
}
