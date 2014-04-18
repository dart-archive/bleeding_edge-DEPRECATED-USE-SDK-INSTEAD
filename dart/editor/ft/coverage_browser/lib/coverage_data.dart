// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage_data;

import 'dart:collection';
import 'package:observe/observe.dart';

/**
 * Coverage data for a single class. [instrumentedLines] are the line numbers
 * that are observed during coverage analysis. [visitedLines] are the lines
 * actually executed.
 */
class CoverageData extends Observable {
  @observable String className;
  @observable int percentCovered;
  List<int> _instrumentedLines = [];
  List<int> _visitedLines = [];

  List<int> get instrumentedLines => _instrumentedLines;
  List<int> get visitedLines => _visitedLines;

  CoverageData(this.className, this._instrumentedLines, this._visitedLines) {
    _computePercentCovered();
  }

  /// Merge the [data] with this instance.
  void merge(CoverageData data, [bool checkValidity = false]) {
    var ins = _mergeList(_instrumentedLines, data._instrumentedLines);
    if (checkValidity && ins.length != 0) {
      var x = _instrumentedLines.length;
      var y = data._instrumentedLines.length;
      if (x != 0 && x != ins.length || y != 0 && y != ins.length) {
        // Not throwing an exception because lines change with each CL commit
        print("Discrepency in instrumented lines for $className");
      }
    }
    _instrumentedLines = ins;
    _visitedLines = _mergeList(_visitedLines, data._visitedLines);
    _computePercentCovered();
  }

  void _computePercentCovered() {
    var n = _instrumentedLines.length;
    var m = _visitedLines.length;
    if (m > n) throw new Exception("Visited lines may not exceed instrumented lines");
    percentCovered = n == 0 ? 0 : (100.0 * m / n).round();
  }

  List<int> _mergeList(List<int> one, List<int> two) {
    var tmp = new HashSet<int>();
    tmp.addAll(one);
    tmp.addAll(two);
    var result = new List.from(tmp);
    result.sort();
    return result;
  }
}

/**
 * Coverage data for a package. Synthesized from the class data.
 */
class PackageData extends Observable {
  @observable String packageName;
  @observable int percentCovered;
  int _nInstrumentedLines;
  int _nVisitedLines;

  int get instrumentedLineCount => _nInstrumentedLines;
  int get visitedLineCount => _nVisitedLines;

  PackageData(this.packageName, this._nInstrumentedLines, this._nVisitedLines) {
    _computePercentCovered();
  }

  /// Merge the [data] with this instance.
  void merge(CoverageData data) {
    _nInstrumentedLines += data.instrumentedLines.length;
    _nVisitedLines += data.visitedLines.length;
    _computePercentCovered();
  }

  void _computePercentCovered() {
    var n = _nInstrumentedLines;
    var m = _nVisitedLines;
    if (m > n) throw new Exception("Visited lines may not exceed instrumented lines");
    percentCovered = n == 0 ? 0 : (100.0 * m / n).round();
  }
}