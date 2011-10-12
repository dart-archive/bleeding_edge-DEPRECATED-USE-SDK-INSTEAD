// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface TotalException {
  String toString();
}

class FormulaException implements TotalException {
  final String _message;

  // when Dart supports optional args, message will be optional
  const FormulaException(String this._message);

  String toString() => _message;
}

class BadFormulaException extends FormulaException {
  const BadFormulaException() : super("#BADFORMULA!");
}

class CycleException extends FormulaException {
  const CycleException() : super("#CYCLE!");
}

class DivByZeroException extends FormulaException {
  const DivByZeroException() : super("#DIV/0!");
}

class FunctionException extends FormulaException {
  const FunctionException() : super("#FUNC!");
}

class NumArgsException extends FormulaException {
  const NumArgsException() : super("#NARGS!");
}

class NumberException extends FormulaException {
  const NumberException() : super("#NUM!");
}

class RefException extends FormulaException {
  const RefException() : super("#REF!");
}

class ValueException extends FormulaException {
  const ValueException() : super("#VALUE!");
}

class RuntimeException implements TotalException {
  final String _message;

  // when Dart supports optional args, message will be optional
  const RuntimeException(String this._message);

  String toString() => _message;
}
