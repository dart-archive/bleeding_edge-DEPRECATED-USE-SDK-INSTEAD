// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('frog_leg');

#import('../../lib/uri/uri.dart');
#import('../lang.dart', prefix: 'frog');
#import('api.dart', prefix: 'api');
#import('io/io.dart', prefix: 'io');

bool compile(frog.World world) {
  final throwOnError = frog.options.throwOnErrors;
  // final compiler = new WorldCompiler(world, throwOnError);
  Uri cwd = new Uri(scheme: 'file', path: io.getCurrentDirectory());
  Uri uri = cwd.resolve(frog.options.dartScript);
  String frogLibDir = frog.options.libDir;
  if (!frogLibDir.endsWith("/")) frogLibDir = "$frogLibDir/";
  Uri frogLib = new Uri(scheme: 'file', path: frogLibDir);
  Uri libraryRoot = frogLib.resolve('../leg/lib/');

  Future<String> provider(Uri uri) {
    if (uri.scheme != 'file') {
      throw new IllegalArgumentException(uri);
    }
    String source = world.files.readAll(uri.path);
    world.dartBytesRead += source.length;
    Completer<String> completer = new Completer<String>();
    completer.complete(source);
    return completer.future;
  }

  void handler(Uri uri, int begin, int end, String message, bool fatal) {
    if (uri === null && !fatal) {
      world.info('[leg] $message');
      return;
    }
    print(message);
    if (fatal && throwOnError) new AbortLeg(message);
  }

  // TODO(ahe): We expect the future to be complete and call value
  // directly. In effect, we don't support truly asynchronous API.
  String code = api.compile(uri, libraryRoot, provider, handler).value;
  if (code === null) return false;
  world.legCode = code;
  world.jsBytesWritten = code.length;
  return true;
}

class AbortLeg {
  final message;
  AbortLeg(this.message);
  toString() => 'Aborted due to --throw-on-error: $message';
}
