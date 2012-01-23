// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('url');
#import('node.dart');
#import('nodeimpl.dart');

// module url

class UrlObject {
  UrlObject._fromObj(var obj) {
    this.href = nativeGetStringProperty(obj, 'href');
    this.protocol = nativeGetStringProperty(obj, 'protocol');
    this.host = nativeGetStringProperty(obj, 'host');
    this.auth = nativeGetStringProperty(obj, 'auth');
    this.port = nativeGetStringProperty(obj, 'port');
    this.pathname = nativeGetStringProperty(obj, 'pathname');
    this.search = nativeGetStringProperty(obj, 'search');
    this.path = nativeGetStringProperty(obj, 'path');
    this.query = nativeGetStringProperty(obj, 'query');
    this.hash = nativeGetStringProperty(obj, 'hash');
    this.slashes = nativeGetBoolProperty(obj, 'slashes');
  }
  
  String href;
  String protocol;
  String host;
  String auth;
  String hostname;
  String port;
  String pathname;
  String search;
  String path;
  String query;
  String hash;
  bool slashes;
}

class Url {
  var _url;
  Url._from(this._url);
  
  UrlObject parse(String urlStr, [bool parseQueryString, bool slashesDenoteHost]
      )
    => new UrlObject._fromObj(_parse(urlStr, parseQueryString, slashesDenoteHost
        ));
  
  var _parse(String urlStr, bool parseQueryString, bool slashesDenoteHost)
    native
        "return this._url.parse(urlStr, parseQueryString, slashesDenoteHost);";

  String format(UrlObject urlObj)
    native "return this._url.format(urlObj);";
  
  String resolve(String from, String to)
    native "return this._url.resolve(from, to);";

  UrlObject resolveObject(UrlObject from, UrlObject to)
    => new UrlObject._fromObj(_resolveObject(from, to));
  
  var _resolveObject(UrlObject from, UrlObject to)
    native "return this._url.resolveOjbect(from, to);";
}

Url get url() => new Url._from(require('url'));
