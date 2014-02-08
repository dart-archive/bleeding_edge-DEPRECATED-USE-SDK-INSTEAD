// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of unittest.matcher;

/**
 * Returns a matcher that matches empty strings, maps or iterables
 * (including collections).
 */
const Matcher isEmpty = const _Empty();

class _Empty extends Matcher {
  const _Empty();
  bool matches(item, Map matchState) {
    if (item is Map || item is Iterable) {
      return item.isEmpty;
    } else if (item is String) {
      return item.length == 0;
    } else {
      return false;
    }
  }
  Description describe(Description description) =>
      description.add('empty');
}

/** A matcher that matches any null value. */
const Matcher isNull = const _IsNull();

/** A matcher that matches any non-null value. */
const Matcher isNotNull = const _IsNotNull();

class _IsNull extends Matcher {
  const _IsNull();
  bool matches(item, Map matchState) => item == null;
  Description describe(Description description) =>
      description.add('null');
}

class _IsNotNull extends Matcher {
  const _IsNotNull();
  bool matches(item, Map matchState) => item != null;
  Description describe(Description description) =>
      description.add('not null');
}

/** A matcher that matches the Boolean value true. */
const Matcher isTrue = const _IsTrue();

/** A matcher that matches anything except the Boolean value true. */
const Matcher isFalse = const _IsFalse();

class _IsTrue extends Matcher {
  const _IsTrue();
  bool matches(item, Map matchState) => item == true;
  Description describe(Description description) =>
      description.add('true');
}

class _IsFalse extends Matcher {
  const _IsFalse();
  bool matches(item, Map matchState) => item == false;
  Description describe(Description description) =>
      description.add('false');
}

/**
 * Returns a matches that matches if the value is the same instance
 * as [object] (`===`).
 */
Matcher same(expected) => new _IsSameAs(expected);

class _IsSameAs extends Matcher {
  final _expected;
  const _IsSameAs(this._expected);
  bool matches(item, Map matchState) => identical(item, _expected);
  // If all types were hashable we could show a hash here.
  Description describe(Description description) =>
      description.add('same instance as ').addDescriptionOf(_expected);
}

class _SymbolEqualsMatcher extends Matcher {
  final Symbol _expected;
  const _SymbolEqualsMatcher(this._expected);
  bool matches(item, Map matchState) {
    final Symbol sym = item is Symbol ? item : item is String ? new Symbol(item) : new Symbol("$item");
    return sym == _expected;
  }
  
  Description describe(Description description) =>
      description.add('same Symbol as ').addDescriptionOf(_expected);
}

/**
 * Returns a matcher that does a deep recursive match. This only works
 * with scalars, Maps and Iterables. To handle cyclic structures a
 * recursion depth [limit] can be provided. The default limit is 100.
 */
Matcher equals(expected, [limit=100]) {
    if (expected is Symbol)
        return new _SymbolEqualsMatcher(expected);
    if (expected is String)
        return new _StringEqualsMatcher(expected);
    else 
        return new _DeepMatcher(expected, limit);
}

class _DeepMatcher extends Matcher {
  final _expected;
  final int _limit;
  var count;

  _DeepMatcher(this._expected, [limit = 1000]) : this._limit = limit;

  // Returns a pair (reason, location)
  List _compareIterables(expected, actual, matcher, depth, location) {
    if (actual is! Iterable) {
      return ['is not Iterable', location];
    }
    var expectedIterator = expected.iterator;
    var actualIterator = actual.iterator;
    var index = 0;
    while (true) {
      var newLocation = '${location}[${index}]';
      if (expectedIterator.moveNext()) {
        if (actualIterator.moveNext()) {
          var rp = matcher(expectedIterator.current,
                           actualIterator.current, newLocation,
                           depth);
          if (rp != null) return rp;
          ++index;
        } else {
          return ['shorter than expected', newLocation];
        }
      } else if (actualIterator.moveNext()) {
        return ['longer than expected', newLocation];
      } else {
        return null;
      }
    }
    return null;
  }

  List _recursiveMatch(expected, actual, String location, int depth) {
    String reason = null;
    // If _limit is 1 we can only recurse one level into object.
    bool canRecurse = depth == 0 || _limit > 1;
    bool equal;
    try {
      equal = (expected == actual);
    } catch (e, s) {
      // TODO(gram): Add a test for this case.
      reason = '== threw "$e"';
      return [reason, location];
    }
    if (equal) {
      // Do nothing.
    } else if (depth > _limit) {
      reason = 'recursion depth limit exceeded';
    } else {
      if (expected is Iterable && canRecurse) {
        List result = _compareIterables(expected, actual,
            _recursiveMatch, depth + 1, location);
        if (result != null) {
          reason = result[0];
          location = result[1];
        }
      } else if (expected is Map && canRecurse) {
        if (actual is! Map) {
          reason = 'expected a map';
        } else {
          var err = (expected.length == actual.length) ? '' :
                    'has different length and ';
          for (var key in expected.keys) {
            if (!actual.containsKey(key)) {
              reason = '${err}is missing map key \'$key\'';
              break;
            }
          }
          if (reason == null) {
            for (var key in actual.keys) {
              if (!expected.containsKey(key)) {
                reason = '${err}has extra map key \'$key\'';
                break;
              }
            }
            if (reason == null) {
              for (var key in expected.keys) {
                var rp = _recursiveMatch(expected[key], actual[key],
                    "${location}['${key}']", depth + 1);
                if (rp != null) {
                  reason = rp[0];
                  location = rp[1];
                  break;
                }
              }
            }
          }
        }
      } else {
        var description = new StringDescription();
        // If we have recursed, show the expected value too; if not,
        // expect() will show it for us.
        if (depth > 0) {
          description.add('was ').
              addDescriptionOf(actual).
              add(' instead of ').
              addDescriptionOf(expected);
          reason = description.toString();
        } else {
          reason = ''; // We're not adding any value to the actual value.
        }
      }
    }
    if (reason == null) return null;
    return [reason, location];
  }

  String _match(expected, actual, Map matchState) {
    var rp = _recursiveMatch(expected, actual, '', 0);
    if (rp == null) return null;
    var reason;
    if (rp[0].length > 0) {
      if (rp[1].length > 0) {
        reason = "${rp[0]} at location ${rp[1]}";
      } else {
        reason = rp[0];
      }
    } else {
      reason = '';
    }
    // Cache the failure reason in the matchState.
    addStateInfo(matchState, {'reason': reason});
    return reason;
  }

  bool matches(item, Map matchState) =>
      _match(_expected, item, matchState) == null;

  Description describe(Description description) =>
    description.addDescriptionOf(_expected);

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState, bool verbose) {
    var reason = matchState['reason'];
    // If we didn't get a good reason, that would normally be a
    // simple 'is <value>' message. We only add that if the mismatch
    // description is non empty (so we are supplementing the mismatch
    // description).
    if (reason.length == 0 && mismatchDescription.length > 0) {
      mismatchDescription.add('is ').addDescriptionOf(item);
    } else {
      mismatchDescription.add(reason);
    }
    return mismatchDescription;
  }
}

/** A special equality matcher for strings. */
class _StringEqualsMatcher extends Matcher {
  final String _value;

  _StringEqualsMatcher(this._value);

  bool get showActualValue => true;

  bool matches(item, Map matchState) => _value == item;

  Description describe(Description description) =>
      description.addDescriptionOf(_value);

  Description describeMismatch(item, Description mismatchDescription,
      Map matchState, bool verbose) {
    if (item is! String) {
      return mismatchDescription.addDescriptionOf(item).add('is not a string');
    } else {
      var buff = new StringBuffer();
      buff.write('is different.');
      var escapedItem = _escape(item);
      var escapedValue = _escape(_value);
      int minLength = escapedItem.length < escapedValue.length ?
          escapedItem.length : escapedValue.length;
      int start;
      for (start = 0; start < minLength; start++) {
        if (escapedValue.codeUnitAt(start) != escapedItem.codeUnitAt(start)) {
          break;
        }
      }
      if (start == minLength) {
        if (escapedValue.length < escapedItem.length) {
          buff.write(' Both strings start the same, but the given value also'
              ' has the following trailing characters: ');
          _writeTrailing(buff, escapedItem, escapedValue.length);
        } else {
          buff.write(' Both strings start the same, but the given value is'
              ' missing the following trailing characters: ');
          _writeTrailing(buff, escapedValue, escapedItem.length);
        }
      } else {
        buff.write('\nExpected: ');
        _writeLeading(buff, escapedValue, start);
        _writeTrailing(buff, escapedValue, start);
        buff.write('\n  Actual: ');
        _writeLeading(buff, escapedItem, start);
        _writeTrailing(buff, escapedItem, start);
        buff.write('\n          ');
        for (int i = (start > 10 ? 14 : start); i > 0; i--) buff.write(' ');
        buff.write('^\n Differ at offset $start');
      }

      return mismatchDescription.replace(buff.toString());
    }
  }

  static String _escape(String s) =>
      s.replaceAll('\n', '\\n').replaceAll('\r', '\\r').replaceAll('\t', '\\t');

  static String _writeLeading(StringBuffer buff, String s, int start) {
    if (start > 10) {
      buff.write('... ');
      buff.write(s.substring(start - 10, start));
    } else {
      buff.write(s.substring(0, start));
    }
  }

  static String _writeTrailing(StringBuffer buff, String s, int start) {
    if (start + 10 > s.length) {
      buff.write(s.substring(start));
    } else {
      buff.write(s.substring(start, start + 10));
      buff.write(' ...');
    }
  }
}

/** A matcher that matches any value. */
const Matcher anything = const _IsAnything();

class _IsAnything extends Matcher {
  const _IsAnything();
  bool matches(item, Map matchState) => true;
  Description describe(Description description) =>
      description.add('anything');
}

/**
 * Returns a matcher that matches if an object is an instance
 * of [type] (or a subtype).
 *
 * As types are not first class objects in Dart we can only
 * approximate this test by using a generic wrapper class.
 *
 * For example, to test whether 'bar' is an instance of type
 * 'Foo', we would write:
 *
 *     expect(bar, new isInstanceOf<Foo>());
 *
 * To get better error message, supply a name when creating the
 * Type wrapper; e.g.:
 *
 *     expect(bar, new isInstanceOf<Foo>('Foo'));
 *
 * Note that this does not currently work in dart2js; it will
 * match any type, and isNot(new isInstanceof<T>()) will always
 * fail. This is because dart2js currently ignores template type
 * parameters.
 */
class isInstanceOf<T> extends Matcher {
  final String _name;
  const isInstanceOf([name = 'specified type']) : this._name = name;
  bool matches(obj, Map matchState) => obj is T;
  // The description here is lame :-(
  Description describe(Description description) =>
      description.add('an instance of ${_name}');
}

/**
 * This can be used to match two kinds of objects:
 *
 *   * A [Function] that throws an exception when called. The function cannot
 *     take any arguments. If you want to test that a function expecting
 *     arguments throws, wrap it in another zero-argument function that calls
 *     the one you want to test.
 *
 *   * A [Future] that completes with an exception. Note that this creates an
 *     asynchronous expectation. The call to `expect()` that includes this will
 *     return immediately and execution will continue. Later, when the future
 *     completes, the actual expectation will run.
 */
const Matcher throws = const Throws();

/**
 * This can be used to match two kinds of objects:
 *
 *   * A [Function] that throws an exception when called. The function cannot
 *     take any arguments. If you want to test that a function expecting
 *     arguments throws, wrap it in another zero-argument function that calls
 *     the one you want to test.
 *
 *   * A [Future] that completes with an exception. Note that this creates an
 *     asynchronous expectation. The call to `expect()` that includes this will
 *     return immediately and execution will continue. Later, when the future
 *     completes, the actual expectation will run.
 *
 * In both cases, when an exception is thrown, this will test that the exception
 * object matches [matcher]. If [matcher] is not an instance of [Matcher], it
 * will implicitly be treated as `equals(matcher)`.
 */
Matcher throwsA(matcher) => new Throws(wrapMatcher(matcher));

/**
 * A matcher that matches a function call against no exception.
 * The function will be called once. Any exceptions will be silently swallowed.
 * The value passed to expect() should be a reference to the function.
 * Note that the function cannot take arguments; to handle this
 * a wrapper will have to be created.
 */
const Matcher returnsNormally = const _ReturnsNormally();

class Throws extends Matcher {
  final Matcher _matcher;

  const Throws([Matcher matcher]) :
    this._matcher = matcher;

  bool matches(item, Map matchState) {
    if (item is! Function && item is! Future) return false;
    if (item is Future) {
      var done = wrapAsync((fn) => fn());

      // Queue up an asynchronous expectation that validates when the future
      // completes.
      item.then((value) {
        done(() => fail("Expected future to fail, but succeeded with '$value'."));
      }, onError: (error, trace) {
        done(() {
          if (_matcher == null) return;
          var reason;
          if (trace != null) {
            var stackTrace = trace.toString();
            stackTrace = "  ${stackTrace.replaceAll("\n", "\n  ")}";
            reason = "Actual exception trace:\n$stackTrace";
          }
          expect(error, _matcher, reason: reason);
        });
      });
      // It hasn't failed yet.
      return true;
    }

    try {
      item();
      return false;
    } catch (e, s) {
      if (_matcher == null || _matcher.matches(e, matchState)) {
        return true;
      } else {
        addStateInfo(matchState, {'exception': e, 'stack': s});
        return false;
      }
    }
  }

  Description describe(Description description) {
    if (_matcher == null) {
      return description.add("throws");
    } else {
      return description.add('throws ').addDescriptionOf(_matcher);
    }
  }

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState,
                               bool verbose) {
    if (item is! Function && item is! Future) {
      return mismatchDescription.add('is not a Function or Future');
    } else if (_matcher == null || matchState['exception'] == null) {
      return mismatchDescription.add('did not throw');
    } else {
      mismatchDescription. add('threw ').
          addDescriptionOf(matchState['exception']);
      if (verbose) {
        mismatchDescription.add(' at ').add(matchState['stack'].toString());
      }
      return mismatchDescription;
    }
  }
}

class _ReturnsNormally extends Matcher {
  const _ReturnsNormally();

  bool matches(f, Map matchState) {
    try {
      f();
      return true;
    } catch (e, s) {
      addStateInfo(matchState, {'exception': e, 'stack': s});
      return false;
    }
  }

  Description describe(Description description) =>
      description.add("return normally");

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState,
                               bool verbose) {
    mismatchDescription.add('threw ').addDescriptionOf(matchState['exception']);
    if (verbose) {
      mismatchDescription.add(' at ').add(matchState['stack'].toString());
    }
    return mismatchDescription;
  }
}

/*
 * Matchers for different exception types. Ideally we should just be able to
 * use something like:
 *
 * final Matcher throwsException =
 *     const _Throws(const isInstanceOf<Exception>());
 *
 * Unfortunately instanceOf is not working with dart2js.
 *
 * Alternatively, if static functions could be used in const expressions,
 * we could use:
 *
 * bool _isException(x) => x is Exception;
 * final Matcher isException = const _Predicate(_isException, "Exception");
 * final Matcher throwsException = const _Throws(isException);
 *
 * But currently using static functions in const expressions is not supported.
 * For now the only solution for all platforms seems to be separate classes
 * for each exception type.
 */

abstract class TypeMatcher extends Matcher {
  final String _name;
  const TypeMatcher(this._name);
  Description describe(Description description) =>
      description.add(_name);
}

/** A matcher for FormatExceptions. */
const isFormatException = const _FormatException();

/** A matcher for functions that throw FormatException. */
const Matcher throwsFormatException =
    const Throws(isFormatException);

class _FormatException extends TypeMatcher {
  const _FormatException() : super("FormatException");
  bool matches(item, Map matchState) => item is FormatException;
}

/** A matcher for Exceptions. */
const isException = const _Exception();

/** A matcher for functions that throw Exception. */
const Matcher throwsException = const Throws(isException);

class _Exception extends TypeMatcher {
  const _Exception() : super("Exception");
  bool matches(item, Map matchState) => item is Exception;
}

/** A matcher for ArgumentErrors. */
const isArgumentError = const _ArgumentError();

/** A matcher for functions that throw ArgumentError. */
const Matcher throwsArgumentError =
    const Throws(isArgumentError);

class _ArgumentError extends TypeMatcher {
  const _ArgumentError() : super("ArgumentError");
  bool matches(item, Map matchState) => item is ArgumentError;
}

/** A matcher for RangeErrors. */
const isRangeError = const _RangeError();

/** A matcher for functions that throw RangeError. */
const Matcher throwsRangeError =
    const Throws(isRangeError);

class _RangeError extends TypeMatcher {
  const _RangeError() : super("RangeError");
  bool matches(item, Map matchState) => item is RangeError;
}

/** A matcher for NoSuchMethodErrors. */
const isNoSuchMethodError = const _NoSuchMethodError();

/** A matcher for functions that throw NoSuchMethodError. */
const Matcher throwsNoSuchMethodError =
    const Throws(isNoSuchMethodError);

class _NoSuchMethodError extends TypeMatcher {
  const _NoSuchMethodError() : super("NoSuchMethodError");
  bool matches(item, Map matchState) => item is NoSuchMethodError;
}

/** A matcher for UnimplementedErrors. */
const isUnimplementedError = const _UnimplementedError();

/** A matcher for functions that throw Exception. */
const Matcher throwsUnimplementedError =
    const Throws(isUnimplementedError);

class _UnimplementedError extends TypeMatcher {
  const _UnimplementedError() : super("UnimplementedError");
  bool matches(item, Map matchState) => item is UnimplementedError;
}

/** A matcher for UnsupportedError. */
const isUnsupportedError = const _UnsupportedError();

/** A matcher for functions that throw UnsupportedError. */
const Matcher throwsUnsupportedError = const Throws(isUnsupportedError);

class _UnsupportedError extends TypeMatcher {
  const _UnsupportedError() :
      super("UnsupportedError");
  bool matches(item, Map matchState) => item is UnsupportedError;
}

/** A matcher for StateErrors. */
const isStateError = const _StateError();

/** A matcher for functions that throw StateError. */
const Matcher throwsStateError =
    const Throws(isStateError);

class _StateError extends TypeMatcher {
  const _StateError() : super("StateError");
  bool matches(item, Map matchState) => item is StateError;
}

/** A matcher for FallThroughError. */
const isFallThroughError = const _FallThroughError();

/** A matcher for functions that throw FallThroughError. */
const Matcher throwsFallThroughError =
    const Throws(isFallThroughError);

class _FallThroughError extends TypeMatcher {
  const _FallThroughError() : super("FallThroughError");
  bool matches(item, Map matchState) => item is FallThroughError;
}

/** A matcher for NullThrownError. */
const isNullThrownError = const _NullThrownError();

/** A matcher for functions that throw NullThrownError. */
const Matcher throwsNullThrownError =
    const Throws(isNullThrownError);

class _NullThrownError extends TypeMatcher {
  const _NullThrownError() : super("NullThrownError");
  bool matches(item, Map matchState) => item is NullThrownError;
}

/** A matcher for ConcurrentModificationError. */
const isConcurrentModificationError = const _ConcurrentModificationError();

/** A matcher for functions that throw ConcurrentModificationError. */
const Matcher throwsConcurrentModificationError =
    const Throws(isConcurrentModificationError);

class _ConcurrentModificationError extends TypeMatcher {
  const _ConcurrentModificationError() : super("ConcurrentModificationError");
  bool matches(item, Map matchState) => item is ConcurrentModificationError;
}

/** A matcher for AbstractClassInstantiationError. */
const isAbstractClassInstantiationError =
    const _AbstractClassInstantiationError();

/** A matcher for functions that throw AbstractClassInstantiationError. */
const Matcher throwsAbstractClassInstantiationError =
    const Throws(isAbstractClassInstantiationError);

class _AbstractClassInstantiationError extends TypeMatcher {
  const _AbstractClassInstantiationError() :
  super("AbstractClassInstantiationError");
  bool matches(item, Map matchState) => item is AbstractClassInstantiationError;
}

/** A matcher for CyclicInitializationError. */
const isCyclicInitializationError = const _CyclicInitializationError();

/** A matcher for functions that throw CyclicInitializationError. */
const Matcher throwsCyclicInitializationError =
    const Throws(isCyclicInitializationError);

class _CyclicInitializationError extends TypeMatcher {
  const _CyclicInitializationError() : super("CyclicInitializationError");
  bool matches(item, Map matchState) => item is CyclicInitializationError;
}

/** A matcher for Map types. */
const isMap = const _IsMap();

class _IsMap extends TypeMatcher {
  const _IsMap() : super("Map");
  bool matches(item, Map matchState) => item is Map;
}

/** A matcher for List types. */
const isList = const _IsList();

class _IsList extends TypeMatcher {
  const _IsList() : super("List");
  bool matches(item, Map matchState) => item is List;
}

/**
 * Returns a matcher that matches if an object has a length property
 * that matches [matcher].
 */
Matcher hasLength(matcher) =>
    new _HasLength(wrapMatcher(matcher));

class _HasLength extends Matcher {
  final Matcher _matcher;
  const _HasLength([Matcher matcher = null]) : this._matcher = matcher;

  bool matches(item, Map matchState) {
    try {
      // This is harmless code that will throw if no length property
      // but subtle enough that an optimizer shouldn't strip it out.
      if (item.length * item.length >= 0) {
        return _matcher.matches(item.length, matchState);
      }
    } catch (e) {
      return false;
    }
  }

  Description describe(Description description) =>
    description.add('an object with length of ').
        addDescriptionOf(_matcher);

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState, bool verbose) {
    try {
      // We want to generate a different description if there is no length
      // property; we use the same trick as in matches().
      if (item.length * item.length >= 0) {
        return mismatchDescription.add('has length of ').
            addDescriptionOf(item.length);
      }
    } catch (e) {
      return mismatchDescription.add('has no length property');
    }
  }
}

/**
 * Returns a matcher that matches if the match argument contains
 * the expected value. For [String]s this means substring matching;
 * for [Map]s it means the map has the key, and for [Iterable]s
 * (including [Iterable]s) it means the iterable has a matching
 * element. In the case of iterables, [expected] can itself be a
 * matcher.
 */
Matcher contains(expected) => new _Contains(expected);

class _Contains extends Matcher {

  final _expected;

  const _Contains(this._expected);

  bool matches(item, Map matchState) {
    if (item is String) {
      return item.indexOf(_expected) >= 0;
    } else if (item is Iterable) {
      if (_expected is Matcher) {
        return item.any((e) => _expected.matches(e, matchState));
      } else {
        return item.contains(_expected);
      }
    } else if (item is Map) {
      return item.containsKey(_expected);
    }
    return false;
  }

  Description describe(Description description) =>
      description.add('contains ').addDescriptionOf(_expected);

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState, bool verbose) {
    if (item is String || item is Iterable || item is Map) {
      return super.describeMismatch(item, mismatchDescription, matchState,
          verbose);
    } else {
      return mismatchDescription.add('is not a string, map or iterable');
    }
  }
}

/**
 * Returns a matcher that matches if the match argument is in
 * the expected value. This is the converse of [contains].
 */
Matcher isIn(expected) => new _In(expected);

class _In extends Matcher {

  final _expected;

  const _In(this._expected);

  bool matches(item, Map matchState) {
    if (_expected is String) {
      return _expected.indexOf(item) >= 0;
    } else if (_expected is Iterable) {
      return _expected.any((e) => e == item);
    } else if (_expected is Map) {
      return _expected.containsKey(item);
    }
    return false;
  }

  Description describe(Description description) =>
      description.add('is in ').addDescriptionOf(_expected);
}

/**
 * Returns a matcher that uses an arbitrary function that returns
 * true or false for the actual value. For example:
 *
 *     expect(v, predicate((x) => ((x % 2) == 0), "is even"))
 */
Matcher predicate(Function f, [description ='satisfies function']) =>
    new _Predicate(f, description);

class _Predicate extends Matcher {

  final Function _matcher;
  final String _description;

  const _Predicate(this._matcher, this._description);

  bool matches(item, Map matchState) => _matcher(item);

  Description describe(Description description) =>
      description.add(_description);
}

/**
 * A useful utility class for implementing other matchers through inheritance.
 * Derived classes should call the base constructor with a feature name and
 * description, and an instance matcher, and should implement the
 * [featureValueOf] abstract method.
 *
 * The feature description will typically describe the item and the feature,
 * while the feature name will just name the feature. For example, we may
 * have a Widget class where each Widget has a price; we could make a
 * [CustomMatcher] that can make assertions about prices with:
 *
 *     class HasPrice extends CustomMatcher {
 *       const HasPrice(matcher) :
 *           super("Widget with price that is", "price", matcher);
 *       featureValueOf(actual) => actual.price;
 *     }
 *
 * and then use this for example like:
 *
 *      expect(inventoryItem, new HasPrice(greaterThan(0)));
 */
class CustomMatcher extends Matcher {
  final String _featureDescription;
  final String _featureName;
  final Matcher _matcher;

  CustomMatcher(this._featureDescription, this._featureName, matcher)
      : this._matcher = wrapMatcher(matcher);

  /** Override this to extract the interesting feature.*/
  featureValueOf(actual) => actual;

  bool matches(item, Map matchState) {
    var f = featureValueOf(item);
    if (_matcher.matches(f, matchState)) return true;
    addStateInfo(matchState, {'feature': f});
    return false;
  }

  Description describe(Description description) =>
      description.add(_featureDescription).add(' ').addDescriptionOf(_matcher);

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState, bool verbose) {
    mismatchDescription.add('has ').add(_featureName).add(' with value ').
        addDescriptionOf(matchState['feature']);
    var innerDescription = new StringDescription();
    _matcher.describeMismatch(matchState['feature'], innerDescription,
        matchState['state'], verbose);
    if (innerDescription.length > 0) {
      mismatchDescription.add(' which ').add(innerDescription.toString());
    }
    return mismatchDescription;
  }
}
