// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.constant_system;

import '../dart_types.dart';
import '../dart2jslib.dart' show Compiler;
import '../tree/tree.dart' show DartString;
import 'values.dart';

abstract class Operation {
  String get name;
}

abstract class UnaryOperation extends Operation {
  /** Returns [:null:] if it was unable to fold the operation. */
  ConstantValue fold(ConstantValue constant);
}

abstract class BinaryOperation extends Operation {
  /** Returns [:null:] if it was unable to fold the operation. */
  ConstantValue fold(ConstantValue left, ConstantValue right);
  apply(left, right);
}

/**
 * A [ConstantSystem] is responsible for creating constants and folding them.
 */
abstract class ConstantSystem {
  BinaryOperation get add;
  BinaryOperation get bitAnd;
  UnaryOperation get bitNot;
  BinaryOperation get bitOr;
  BinaryOperation get bitXor;
  BinaryOperation get booleanAnd;
  BinaryOperation get booleanOr;
  BinaryOperation get divide;
  BinaryOperation get equal;
  BinaryOperation get greaterEqual;
  BinaryOperation get greater;
  BinaryOperation get identity;
  BinaryOperation get lessEqual;
  BinaryOperation get less;
  BinaryOperation get modulo;
  BinaryOperation get multiply;
  UnaryOperation get negate;
  UnaryOperation get not;
  BinaryOperation get shiftLeft;
  BinaryOperation get shiftRight;
  BinaryOperation get subtract;
  BinaryOperation get truncatingDivide;

  BinaryOperation get codeUnitAt;

  const ConstantSystem();

  ConstantValue createInt(int i);
  ConstantValue createDouble(double d);
  ConstantValue createString(DartString string);
  ConstantValue createBool(bool value);
  ConstantValue createNull();
  ConstantValue createMap(Compiler compiler,
                          InterfaceType type,
                          List<ConstantValue> keys,
                          List<ConstantValue> values);

  // We need to special case the subtype check for JavaScript constant
  // system because an int is a double at runtime.
  bool isSubtype(DartTypes types, DartType s, DartType t);

  /** Returns true if the [constant] is an integer at runtime. */
  bool isInt(ConstantValue constant);
  /** Returns true if the [constant] is a double at runtime. */
  bool isDouble(ConstantValue constant);
  /** Returns true if the [constant] is a string at runtime. */
  bool isString(ConstantValue constant);
  /** Returns true if the [constant] is a boolean at runtime. */
  bool isBool(ConstantValue constant);
  /** Returns true if the [constant] is null at runtime. */
  bool isNull(ConstantValue constant);

  UnaryOperation lookupUnary(String operator) {
    switch (operator) {
      case '~': return bitNot;
      case '-': return negate;
      case '!': return not;
      default:  return null;
    }
  }

  BinaryOperation lookupBinary(String operator) {
    switch (operator) {
      case "+":   return add;
      case "-":   return subtract;
      case "*":   return multiply;
      case "/":   return divide;
      case "%":   return modulo;
      case "~/":  return truncatingDivide;
      case "|":   return bitOr;
      case "&":   return bitAnd;
      case "^":   return bitXor;
      case "||":  return booleanOr;
      case "&&":  return booleanAnd;
      case "<<":  return shiftLeft;
      case ">>":  return shiftRight;
      case "<":   return less;
      case "<=":  return lessEqual;
      case ">":   return greater;
      case ">=":  return greaterEqual;
      case "==":  return equal;
      default:    return null;
    }
  }
}
