// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test_utils;

import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/parser.dart';
import 'package:analyzer/src/generated/scanner.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:unittest/unittest.dart';


/// Parse the given [source] as a statement and assert, if provided, that
/// exactly a given set of [expectedErrorCodes] are encountered.
Statement parseStatement(String source, [List<ErrorCode> expectedErrorCodes]) {

  var listener = new _GatheringErrorListener();
  var reader = new CharSequenceReader(source);
  var scanner = new Scanner(null, reader, listener);
  listener.setLineInfo(new _TestSource(), scanner.lineStarts);

  var token = scanner.tokenize();
  var parser = new Parser(null, listener);
  var statement = parser.parseStatement(token);
  expect(statement, isNotNull);

  if (expectedErrorCodes != null) {
    listener.expectErrors(expectedErrorCodes);
  }

  return statement;
}


Set<_MapEntry> _getMapEntrySet(Map m) {
  var result = new Set();
  m.forEach((k, v) {
    result.add(new _MapEntry(k, v));
  });
  return result;
}


_unsupported() => throw new _UnsupportedOperationException();


/// Instances of the class [_GatheringErrorListener] implement an error listener
/// that collects all of the errors passed to it for later examination.
class _GatheringErrorListener implements AnalysisErrorListener {
  /// A list containing the errors that were collected.
  final List<AnalysisError> _errors = new List<AnalysisError>();

  /// A table mapping sources to the line information for the source.
  final Map<Source, LineInfo> _lineInfoMap = new Map<Source, LineInfo>();

  /// Asserts that the number of errors that have been gathered matches the
  /// number of errors that are given and that they have the expected error
  /// codes. The order in which the errors were gathered is ignored.
  void expectErrors(List<ErrorCode> expectedErrorCodes) {
    var builder = new StringBuffer();
    var expectedCounts = new Map<ErrorCode, int>();

    for (var code in expectedErrorCodes) {
      var count = expectedCounts[code];
      if (count == null) {
        count = 1;
      } else {
        count = count + 1;
      }
      expectedCounts[code] = count;
    }

    var errorsByCode = new Map<ErrorCode, List<AnalysisError>>();
    for (var error in _errors) {
      var code = error.errorCode;
      var list = errorsByCode[code];
      if (list == null) {
        list = new List<AnalysisError>();
        errorsByCode[code] = list;
      }
      list.add(error);
    }

    for (var entry in _getMapEntrySet(expectedCounts)) {
      var code = entry.getKey();
      var expectedCount = entry.getValue();
      var actualCount;

      var list = errorsByCode.remove(code);
      if (list == null) {
        actualCount = 0;
      } else {
        actualCount = list.length;
      }

      if (actualCount != expectedCount) {
        if (builder.length == 0) {
          builder.write('Expected ');
        } else {
          builder.write('; ');
        }
        builder.write(expectedCount);
        builder.write(' errors of type ');
        builder.write(code);
        builder.write(', found ');
        builder.write(actualCount);
      }
    }

    for (var entry in _getMapEntrySet(errorsByCode)) {
      var code = entry.getKey();
      var actualErrors = entry.getValue();
      var actualCount = actualErrors.length;

      if (builder.length == 0) {
        builder.write('Expected ');
      } else {
        builder.write('; ');
      }

      builder.write('0 errors of type ');
      builder.write(code);
      builder.write(', found ');
      builder.write(actualCount);
      builder.write(' (');

      for (int i = 0; i < actualErrors.length; i++) {
        var error = actualErrors[i];
        if (i > 0) {
          builder.write(', ');
        }
        builder.write(error.offset);
      }

      builder.write(')');
    }

    if (builder.length > 0) {
      fail(builder.toString());
    }
  }


  void onError(AnalysisError error) {
    _errors.add(error);
  }


  /// Sets the line information associated with the given source to the given
  /// information.
  void setLineInfo(Source source, List<int> lineStarts) {
    _lineInfoMap[source] = new LineInfo(lineStarts);
  }

}


class _MapEntry<K, V> {
  K _key;
  V _value;
  _MapEntry(this._key, this._value);
  K getKey() => _key;
  V getValue() => _value;
}

class _TestSource extends Source {

  TimestampedData<String> get contents => _unsupported();

  AnalysisContext get context => _unsupported();

  String get encoding => _unsupported();

  String get fullName => _unsupported();

  bool get isInSystemLibrary => _unsupported();

  int get modificationStamp => _unsupported();

  String get shortName => _unsupported();

  Uri get uri => _unsupported();

  UriKind get uriKind => _unsupported();

  bool operator ==(Object object) => object is _TestSource;

  bool exists() => true;

  void getContentsToReceiver(Source_ContentReceiver receiver) => _unsupported();

  Source resolve(String uri) => _unsupported();

  Uri resolveRelativeUri(Uri uri) => _unsupported();
}


class _UnsupportedOperationException implements Exception {
  String toString() => 'UnsupportedOperationException';
}
