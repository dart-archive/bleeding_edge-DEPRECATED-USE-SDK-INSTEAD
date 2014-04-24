library fixed_unittest;

import 'package:unittest/unittest.dart';
import 'package:di/src/mirrors.dart';
import 'dart:async';

export 'package:unittest/unittest.dart';

// Jasmine-like syntax for unittest.
void describe(String spec, TestFunction body) => group(spec, body);
void ddescribe(String spec, TestFunction body) => solo_group(spec, body);
void xdescribe(String spec, TestFunction body) {}
void it(String spec, TestFunction body) => test(spec, body);
void xit(String spec, TestFunction body) {}
void iit(String spec, TestFunction body) => solo_test(spec, body);

Matcher toEqual(expected) => equals(expected);
Matcher toContain(expected) => contains(expected);
Matcher toBe(expected) => same(expected);
Matcher instanceOf(Type t) => new IsInstanceOfTypeMatcher(t);

Matcher toThrow(Type exceptionClass, [message]) => message == null
      ? new ThrowsMatcher(instanceOf(exceptionClass))
      : new ThrowsMatcher(new ComplexExceptionMatcher(
          instanceOf(exceptionClass),
          message is Matcher ? message : toContain(message)));

Matcher not(Matcher matcher) => new NegateMatcher(matcher);


class NegateMatcher extends Matcher {
  final Matcher _matcher;

  const NegateMatcher(this._matcher);

  bool matches(obj, Map ms) => !_matcher.matches(obj, ms);

  Description describe(Description description) =>
      _matcher.describe(description.add('NOT'));

  Description describeMismatch(item, Description mismatchDescription,
      Map matchState, bool verbose) {
    return _matcher.describeMismatch(
        item, mismatchDescription, matchState, verbose);
  }
}


class ThrowsMatcher extends Throws {
  final Matcher _matcher;

  const ThrowsMatcher([Matcher matcher]) : _matcher = matcher, super(matcher);

  Description describeMismatch(item, Description mismatchDescription,
                               Map matchState,
                               bool verbose) {
    if (item is! Function && item is! Future) {
      return mismatchDescription.add(' not a Function or Future');
    }

    if (_matcher == null || matchState == null) {
      return mismatchDescription.add(' did not throw any exception');
    }

    return _matcher.describeMismatch(item, mismatchDescription,
        matchState, verbose);
  }
}

class ComplexExceptionMatcher extends Matcher {
  Matcher classMatcher;
  Matcher messageMatcher;

  ComplexExceptionMatcher(this.classMatcher, this.messageMatcher);

  bool matches(obj, Map ms) {
    if (!classMatcher.matches(obj, ms)) {
      return false;
    }

    return messageMatcher.matches(obj.message, ms);
  }

  Description describe(Description description) =>
      messageMatcher.describe(classMatcher.describe(description).add(' with message '));

  Description describeMismatch(item, Description mismatchDescription,
      Map matchState, bool verbose) {
    var e = matchState['exception'];

    mismatchDescription.add('threw ').addDescriptionOf(e);

    try {
      var message = e.message; // does not have to be defined
      mismatchDescription.add(' with message ').addDescriptionOf(message);
    } catch (e) {}
  }
}

class IsInstanceOfTypeMatcher extends Matcher {
  Type t;

  IsInstanceOfTypeMatcher(this.t);

  bool matches(obj, Map matchState) =>
      reflect(obj).type.qualifiedName == reflectClass(t).qualifiedName;

  Description describe(Description description) =>
      description.add('an instance of ${t.toString()}');
}
