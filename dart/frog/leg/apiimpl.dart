// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('leg_apiimpl');

#import('leg.dart', prefix: 'leg');
#import('ssa/tracer.dart', prefix: 'ssa');
#import('../lang.dart', prefix: 'frog');

class Compiler extends leg.Compiler {
  ReadUriFromString provider;
  DiagnosticHandler handler;
  Uri libraryRoot;

  Compiler(this.provider, this.handler, this.libraryRoot)
    : super.withCurrentDirectory(null, tracer: new ssa.HTracer());

  LibraryElement scanBuiltinLibrary(String filename) {
    Uri uri = libraryRoot.resolve(filename);
    LibraryElement library = scanner.loadLibrary(uri, null);
    return library;
  }

  void log(message) {
    handler(null, null, null, message, false);
  }

  Script readScript(Uri uri, [ScriptTag node]) {
    String uriName = uri.toString();
    // TODO(ahe): Clean this up.
    if (uriName == 'dart:dom') {
      uri = libraryRoot.resolve('../../../client/dom/frog/dom_frog.dart');
    } else if (uriName == 'dart:html') {
      uri = libraryRoot.resolve('../../../client/html/frog/html_frog.dart');
    } else if (uriName == 'dart:json') {
      uri = libraryRoot.resolve('../../../lib/json/json.dart');
    }
    String text = "";
    try {
      // TODO(ahe): We expect the future to be complete and call value
      // directly. In effect, we don't support truly asynchronous API.
      text = provider(uri).value;
    } catch (var exception) {
      cancel("${uri.path}: $exception", node: node);
    }
    frog.SourceFile sourceFile = new frog.SourceFile(uri.toString(), text);
    return new leg.Script(uri, sourceFile);
  }

  bool run(Uri uri) {
    bool success = super.run(uri);
    for (final task in tasks) {
      log('${task.name} took ${task.timing}msec');
    }
    return success;
  }
}
