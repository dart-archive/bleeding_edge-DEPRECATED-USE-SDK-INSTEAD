// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class RegExpWrapper {
  final String pattern;
  final bool multiLine;
  final bool ignoreCase;
  final bool global;

  const RegExpWrapper(this.pattern,
                      this.multiLine, this.ignoreCase, this.global);

  const RegExpWrapper.fromRegExp(other, global)
    : this(other.pattern, other.multiLine, other.ignoreCase, global);

  exec(str) {
    var result = JS('List', @'#.exec(#)', re, checkString(str));
    if (JS('bool', @'# === null', result)) return null;
    return result;
  }

  test(str) => JS('bool', @'#.test(#)', re, checkString(str));

  static matchStart(m) => JS('int', @'#.index', m);

  get re() {
    var r = JS('var', @'#._re', this);
    if (r === null) {
      r = JS('var', @'#._re = #', this, makeRegExp());
    }
    return r;
  }

  makeRegExp() {
    checkString(pattern);
    StringBuffer sb = new StringBuffer();
    if (multiLine) sb.add('m');
    if (ignoreCase) sb.add('i');
    if (global) sb.add('g');
    try {
      return JS('Object', @'new RegExp(#, #)', pattern, sb.toString());
    } catch (var e) {
      throw new IllegalJSRegExpException(pattern,
                                         JS('String', @'String(#)', e));
    }
  }
}
