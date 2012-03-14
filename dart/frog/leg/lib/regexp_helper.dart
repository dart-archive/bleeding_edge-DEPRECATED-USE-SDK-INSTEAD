// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class RegExpWrapper {
  final re;

  // TODO(ahe): This constructor is clearly not const. We need some
  // better way to handle constant regular expressions. One might
  // question if regular expressions are really constant as we have
  // tests that expect an exception from the constructor.
  const RegExpWrapper(pattern, multiLine, ignoreCase, global)
    : re = makeRegExp(pattern, "${multiLine == true ? 'm' : ''}${
                                  ignoreCase == true ? 'i' : ''}${
                                  global == true ? 'g' : ''}");

  RegExpWrapper.fromRegExp(other, global)
    // TODO(ahe): Use redirection.
    : re = makeRegExp(other.pattern, "${other.multiLine == true ? 'm' : ''}${
                                        other.ignoreCase == true ? 'i' : ''}${
                                        global == true ? 'g' : ''}");

  exec(str) {
    var result = JS('List', @'#.exec(#)', re, checkString(str));
    if (JS('bool', @'# === null', result)) return null;
    return result;
  }

  test(str) => JS('bool', @'#.test(#)', re, checkString(str));

  static matchStart(m) => JS('int', @'#.index', m);

  static makeRegExp(pattern, flags) {
    checkString(pattern);
    try {
      return JS('Object', @'new RegExp(#, #)', pattern, flags);
    } catch (var e) {
      throw new IllegalJSRegExpException(pattern,
                                         JS('String', @'String(#)', e));
    }
  }
}
