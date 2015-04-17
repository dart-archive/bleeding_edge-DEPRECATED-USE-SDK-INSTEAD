// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library compiler_helper;

import 'dart:async';
import "package:expect/expect.dart";

import 'package:compiler/compiler.dart' as api;

import 'package:compiler/src/elements/elements.dart'
       as lego;
export 'package:compiler/src/elements/elements.dart';

import 'package:compiler/src/js_backend/js_backend.dart'
       as js;

import 'package:compiler/src/dart2jslib.dart'
       as leg;
export 'package:compiler/src/dart2jslib.dart'
       show Constant,
            Message,
            MessageKind,
            Selector,
            TypedSelector,
            SourceSpan,
            World;

import 'package:compiler/src/types/types.dart'
       as types;
export 'package:compiler/src/types/types.dart'
       show TypeMask;

import 'package:compiler/src/util/util.dart';
export 'package:compiler/src/util/util.dart';

import 'package:compiler/src/dart2jslib.dart'
       show Compiler;

export 'package:compiler/src/tree/tree.dart';

import 'mock_compiler.dart';
export 'mock_compiler.dart';

import 'output_collector.dart';
export 'output_collector.dart';

Future<String> compile(String code,
                       {String entry: 'main',
                        bool enableTypeAssertions: false,
                        bool minify: false,
                        bool analyzeAll: false,
                        bool disableInlining: true,
                        void check(String generated)}) {
  MockCompiler compiler = new MockCompiler.internal(
      enableTypeAssertions: enableTypeAssertions,
      // Type inference does not run when manually
      // compiling a method.
      disableTypeInference: true,
      enableMinification: minify,
      disableInlining: disableInlining);
  return compiler.init().then((_) {
    compiler.parseScript(code);
    lego.Element element = compiler.mainApp.find(entry);
    if (element == null) return null;
    compiler.phase = Compiler.PHASE_RESOLVING;
    compiler.backend.enqueueHelpers(compiler.enqueuer.resolution,
                                    compiler.globalDependencies);
    compiler.processQueue(compiler.enqueuer.resolution, element);
    compiler.world.populate();
    compiler.backend.onResolutionComplete();
    var context = new js.JavaScriptItemCompilationContext();
    leg.ResolutionWorkItem resolutionWork =
        new leg.ResolutionWorkItem(element, context);
    resolutionWork.run(compiler, compiler.enqueuer.resolution);
    leg.CodegenWorkItem work =
        new leg.CodegenWorkItem(element, context);
    compiler.phase = Compiler.PHASE_COMPILING;
    work.run(compiler, compiler.enqueuer.codegen);
    js.JavaScriptBackend backend = compiler.backend;
    String generated = backend.assembleCode(element);
    if (check != null) {
      check(generated);
    }
    return generated;
  });
}

// TODO(herhut): Disallow warnings and errors during compilation by default.
MockCompiler compilerFor(String code, Uri uri,
                         {bool analyzeAll: false,
                          bool analyzeOnly: false,
                          Map<String, String> coreSource,
                          bool disableInlining: true,
                          bool minify: false,
                          bool trustTypeAnnotations: false,
                          int expectedErrors,
                          int expectedWarnings,
                          api.CompilerOutputProvider outputProvider}) {
  MockCompiler compiler = new MockCompiler.internal(
      analyzeAll: analyzeAll,
      analyzeOnly: analyzeOnly,
      coreSource: coreSource,
      disableInlining: disableInlining,
      enableMinification: minify,
      trustTypeAnnotations: trustTypeAnnotations,
      expectedErrors: expectedErrors,
      expectedWarnings: expectedWarnings,
      outputProvider: outputProvider);
  compiler.registerSource(uri, code);
  return compiler;
}

Future<String> compileAll(String code,
                          {Map<String, String> coreSource,
                           bool disableInlining: true,
                           bool trustTypeAnnotations: false,
                           bool minify: false,
                           int expectedErrors,
                           int expectedWarnings}) {
  Uri uri = new Uri(scheme: 'source');
  OutputCollector outputCollector = new OutputCollector();
  MockCompiler compiler = compilerFor(
      code, uri, coreSource: coreSource, disableInlining: disableInlining,
      minify: minify, expectedErrors: expectedErrors,
      trustTypeAnnotations: trustTypeAnnotations,
      expectedWarnings: expectedWarnings,
      outputProvider: outputCollector);
  return compiler.runCompiler(uri).then((_) {
    Expect.isFalse(compiler.compilationFailed,
                   'Unexpected compilation error(s): ${compiler.errors}');
    return outputCollector.getOutput('', 'js');
  });
}

Future compileAndCheck(String code,
                       String name,
                       check(MockCompiler compiler, lego.Element element),
                       {int expectedErrors, int expectedWarnings}) {
  Uri uri = new Uri(scheme: 'source');
  MockCompiler compiler = compilerFor(code, uri,
      expectedErrors: expectedErrors,
      expectedWarnings: expectedWarnings);
  return compiler.runCompiler(uri).then((_) {
    lego.Element element = findElement(compiler, name);
    return check(compiler, element);
  });
}

Future compileSources(Map<String, String> sources,
               check(MockCompiler compiler)) {
  Uri base = new Uri(scheme: 'source');
  Uri mainUri = base.resolve('main.dart');
  String mainCode = sources['main.dart'];
  Expect.isNotNull(mainCode, 'No source code found for "main.dart"');
  MockCompiler compiler = compilerFor(mainCode, mainUri);
  sources.forEach((String path, String code) {
    if (path == 'main.dart') return;
    compiler.registerSource(base.resolve(path), code);
  });

  return compiler.runCompiler(mainUri).then((_) {
    return check(compiler);
  });
}

lego.Element findElement(compiler, String name, [Uri library]) {
  lego.LibraryElement lib = compiler.mainApp;
  if (library != null) {
    lib = compiler.libraryLoader.lookupLibrary(library);
    Expect.isNotNull(lib, 'Could not locate library $library.');
  }
  var element = lib.find(name);
  Expect.isNotNull(element, 'Could not locate $name.');
  return element;
}

types.TypeMask findTypeMask(compiler, String name,
                            [String how = 'nonNullExact']) {
  var sourceName = name;
  var element = compiler.mainApp.find(sourceName);
  if (element == null) {
    element = compiler.backend.interceptorsLibrary.find(sourceName);
  }
  if (element == null) {
    element = compiler.coreLibrary.find(sourceName);
  }
  Expect.isNotNull(element, 'Could not locate $name');
  switch (how) {
    case 'exact':
      return new types.TypeMask.exact(element, compiler.world);
    case 'nonNullExact':
      return new types.TypeMask.nonNullExact(element, compiler.world);
    case 'subclass':
      return new types.TypeMask.subclass(element, compiler.world);
    case 'nonNullSubclass':
      return new types.TypeMask.nonNullSubclass(element, compiler.world);
    case 'subtype':
      return new types.TypeMask.subtype(element, compiler.world);
    case 'nonNullSubtype':
      return new types.TypeMask.nonNullSubtype(element, compiler.world);
  }
  Expect.fail('Unknown TypeMask constructor $how');
  return null;
}

String anyIdentifier = "[a-zA-Z][a-zA-Z0-9]*";

String getIntTypeCheck(String variable) {
  return "\\($variable ?!== ?\\($variable ?\\| ?0\\)|"
         "\\($variable ?>>> ?0 ?!== ?$variable";
}

String getNumberTypeCheck(String variable) {
  return """\\(typeof $variable ?!== ?"number"\\)""";
}

void checkNumberOfMatches(Iterator it, int nb) {
  bool hasNext = it.moveNext();
  for (int i = 0; i < nb; i++) {
    Expect.isTrue(hasNext, "Found less than $nb matches");
    hasNext = it.moveNext();
  }
  Expect.isFalse(hasNext, "Found more than $nb matches");
}

Future compileAndMatch(String code, String entry, RegExp regexp) {
  return compile(code, entry: entry, check: (String generated) {
    Expect.isTrue(regexp.hasMatch(generated),
                  '"$generated" does not match /$regexp/');
  });
}

Future compileAndDoNotMatch(String code, String entry, RegExp regexp) {
  return compile(code, entry: entry, check: (String generated) {
    Expect.isFalse(regexp.hasMatch(generated),
                   '"$generated" has a match in /$regexp/');
  });
}

int length(Link link) => link.isEmpty ? 0 : length(link.tail) + 1;

// Does a compile and then a match where every 'x' is replaced by something
// that matches any variable, and every space is optional.
Future compileAndMatchFuzzy(String code, String entry, String regexp) {
  return compileAndMatchFuzzyHelper(code, entry, regexp, true);
}

Future compileAndDoNotMatchFuzzy(String code, String entry, String regexp) {
  return compileAndMatchFuzzyHelper(code, entry, regexp, false);
}

Future compileAndMatchFuzzyHelper(
    String code, String entry, String regexp, bool shouldMatch) {
  return compile(code, entry: entry, check: (String generated) {
    final xRe = new RegExp('\\bx\\b');
    regexp = regexp.replaceAll(xRe, '(?:$anyIdentifier)');
    final spaceRe = new RegExp('\\s+');
    regexp = regexp.replaceAll(spaceRe, '(?:\\s*)');
    if (shouldMatch) {
      Expect.isTrue(new RegExp(regexp).hasMatch(generated));
    } else {
      Expect.isFalse(new RegExp(regexp).hasMatch(generated));
    }
  });
}
