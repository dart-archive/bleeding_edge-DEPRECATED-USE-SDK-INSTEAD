library angular.core.parser.utils;

import 'package:angular/core/parser/syntax.dart' show Expression;
import 'package:angular/core/module.dart';
import 'package:angular/utils.dart' show isReservedWord;
export 'package:angular/utils.dart' show relaxFnApply, relaxFnArgs, toBool;

/// Marker for an uninitialized value.
const UNINITIALIZED = const _Uninitialized();
class _Uninitialized { const _Uninitialized(); }

class EvalError {
  final String message;
  EvalError(this.message);

  String unwrap(String input, stack) {
    String location = (stack == null) ? '' : '\n\nFROM:\n$stack';
    return 'Eval Error: $message while evaling [$input]$location';
  }
}

/// Evaluate the [list] in context of the [scope].
List evalList(scope, List<Expression> list, [FilterMap filters]) {
  int length = list.length;
  for (int cacheLength = _evalListCache.length; cacheLength <= length; cacheLength++) {
    _evalListCache.add(new List(cacheLength));
  }
  List result = _evalListCache[length];
  for (int i = 0; i < length; i++) {
    result[i] = list[i].eval(scope, filters);
  }
  return result;
}
final List _evalListCache = [[],[0],[0,0],[0,0,0],[0,0,0,0],[0,0,0,0,0]];

/// Add the two arguments with automatic type conversion.
autoConvertAdd(a, b) {
  if (a != null && b != null) {
    // TODO(deboer): Support others.
    if (a is String && b is! String) {
      return a + b.toString();
    }
    if (a is! String && b is String) {
      return a.toString() + b;
    }
    return a + b;
  }
  if (a != null) return a;
  if (b != null) return b;
  return null;
}

/**
 * Ensures that the given [function] is a function and return it. Throws
 * an [EvalError] if it isn't.
 */
Function ensureFunction(function, String name) {
  if (function is Function) return function;
  if (function == null) {
    throw new EvalError("Undefined function $name");
  } else {
    throw new EvalError("$name is not a function");
  }
}

/**
 * Ensures that the map entry with the given [name] is a function and
 * return it. Throws an [EvalError] if it isn't.
 */
Function ensureFunctionFromMap(Map map, String name) {
  return ensureFunction(map[name], name);
}

/// Get a keyed element from the given [object].
getKeyed(object, key) {
  if (object is List) {
    return object[key.toInt()];
  } else if (object is Map) {
    return object["$key"]; // toString dangerous?
  } else if (object == null) {
    throw new EvalError('Accessing null object');
  } else {
    return object[key];
  }
}

/// Set a keyed element in the given [object].
setKeyed(object, key, value) {
  if (object is List) {
    int index = key.toInt();
    if (object.length <= index) object.length = index + 1;
    object[index] = value;
  } else if (object is Map) {
    object["$key"] = value; // toString dangerous?
  } else {
    object[key] = value;
  }
  return value;
}

/// Returns a new symbol with the given name if the name is a legal
/// symbol name. Otherwise, returns null.
Symbol newSymbol(String name) {
  return isReservedWord(name) ? null : new Symbol(name);
}
