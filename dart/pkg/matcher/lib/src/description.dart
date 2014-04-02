// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library matcher.description;

import 'interfaces.dart';
import 'pretty_print.dart';
import 'utils.dart';

/**
 * The default implementation of IDescription. This should rarely need
 * substitution, although conceivably it is a place where other languages
 * could be supported.
 */
class StringDescription implements Description {
  var _out;

  /** Initialize the description with initial contents [init]. */
  StringDescription([String init = '']) {
    _out = init;
  }

  int get length => _out.length;

  /** Get the description as a string. */
  String toString() => _out;

  /** Append [text] to the description.  */
  Description add(text) {
    _out = '${_out}${text}';
    return this;
  }

  /** Change the value of the description. */
  Description replace(String text) {
    _out = text;
    return this;
  }

  /**
   * Appends a description of [value]. If it is an IMatcher use its
   * describe method; if it is a string use its literal value after
   * escaping any embedded control characters; otherwise use its
   * toString() value and wrap it in angular "quotes".
   */
  Description addDescriptionOf(value) {
    if (value is Matcher) {
      value.describe(this);
    } else {
      add(prettyPrint(value, maxLineLength: 80, maxItems: 25));
    }
    return this;
  }

  /**
   * Append an [Iterable] [list] of objects to the description, using the
   * specified [separator] and framing the list with [start]
   * and [end].
   */
  Description addAll(String start, String separator, String end,
                       Iterable list) {
    var separate = false;
    add(start);
    for (var item in list) {
      if (separate) {
        add(separator);
      }
      addDescriptionOf(item);
      separate = true;
    }
    add(end);
    return this;
  }

  /** Escape the control characters in [string] so that they are visible. */
  _addEscapedString(String string) {
    add("'");
    add(escapeString(string));
    add("'");
  }
}
