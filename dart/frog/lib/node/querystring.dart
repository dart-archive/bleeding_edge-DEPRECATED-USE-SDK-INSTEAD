// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('querystring');
#import('node.dart');
#import('nodeimpl.dart');

// module querystring

typedef String QuerystringTranslator(String source);
class Querystring {
  var _qs;
  Querystring._from(this._qs);
  String stringify(Map obj, [String sep, String eq])
    native "return this._qs.stringify(obj, sep, eq);";
  Map<String,Object> parse(String str, [String sep, String eq])
    => new NativeMapPrimitiveValue(_parse(str, sep, eq));
  var _parse(String str, String sep, String eq)
    native "return this._qs.parse(str, sep, eq);";

  QuerystringTranslator get escape()
    native "return this._qs.escape;"; 

  void set escape(QuerystringTranslator t)
    native "this._qs.escape = t;";

  QuerystringTranslator get unescape()
    native "return this._qs.unescape;"; 

  void set unescape(QuerystringTranslator t)
    native "this._qs.unescape = t;";
}

Querystring get querystring() => new Querystring._from(require('querystring'));

