// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('frog_leg');

#import('../../lib/uri/uri.dart');
#import('../lang.dart', prefix: 'frog');
#import('elements/elements.dart');
#import('io/io.dart', prefix: 'io');
#import('leg.dart');
#import('tree/tree.dart');
#import('ssa/tracer.dart');

bool compile(frog.World world) {
  final throwOnError = frog.options.throwOnErrors;
  final compiler = new WorldCompiler(world, throwOnError);
  Uri cwd = new Uri(scheme: 'file', path: compiler.currentDirectory);
  Uri uri = cwd.resolve(frog.options.dartScript);
  final script = compiler.readScript(uri);
  return compiler.run(script);
}

class WorldCompiler extends Compiler {
  final frog.World world;
  final bool throwOnError;

  WorldCompiler(this.world, this.throwOnError)
    : super.withCurrentDirectory(io.getCurrentDirectory(),
                                 tracer: new HTracer());

  void log(message) {
    if (frog.options.showInfo) {
      // Avoid calling message.toString() unless showInfo is on. It
      // could be slow.
      world.info('[leg] $message');
    }
  }

  bool run(Script script) {
    bool success = super.run(script);
    if (success) {
      var code = assembledCode;
      world.legCode = code;
      world.jsBytesWritten = code.length;
      for (final task in tasks) {
        log('${task.name} took ${task.timing}msec');
      }
    }
    return success;
  }

  spanFromNode(Node node, Script current) {
    final begin = node.getBeginToken();
    final end = node.getEndToken();
    if (begin === null || end === null) {
      cancel('cannot find tokens to produce error message for $node.');
    }
    final startOffset = begin.charOffset;
    // TODO(ahe): Compute proper end offset.
    final endOffset = end.charOffset + 1;
    return new frog.SourceSpan(current.file, startOffset, endOffset);
  }

  currentScript() {
    if (currentElement === null) return null;
    CompilationUnitElement compilationUnit =
      currentElement.getCompilationUnit();
    if (compilationUnit === null) return null;
    return compilationUnit.script;
  }

  reportWarning(Node node, var message) {
    frog.SourceSpan span;
    Script current = currentScript();
    if (current === null) {
      world.fatal('No current script');
    } else if (node === null) {
      span = new frog.SourceSpan(current.file, 0, 0);
    } else {
      span = spanFromNode(node, current);
    }
    world.warning('$message.', span);
  }

  reportError(Node node, var message) {
    cancel(message.toString(), node);
  }

  Uri getUriFor(String fileName) {
    Uri cwd = new Uri(scheme: 'file', path: currentDirectory);
    return cwd.resolve(fileName);
  }

  Script readScript(Uri uri, [ScriptTag node]) {
    String uriName = uri.toString();
    // TODO(ahe): Clean this up.
    if (uriName == 'dart:dom') {
      uri = getUriFor(io.join([legDirectory, '..', '..',
          'client', 'dom', 'frog', 'dom_frog.dart']));
    } else if (uriName == 'dart:html') {
      uri = getUriFor(io.join([legDirectory, '..', '..',
          'client', 'html', 'frog', 'html_frog.dart']));
    }
    if (uri.scheme != 'file') cancel('cannot read $uri', node: node);
    String text = "";
    try {
      text = frog.world.files.readAll(uri.path);
    } catch (var exception) {
      cancel("${uri.path}: $exception", node: node);
    }
    world.dartBytesRead += text.length;
    frog.SourceFile sourceFile = new frog.SourceFile(uri.toString(), text);
    return new Script(uri, sourceFile);
  }

  String get legDirectory() => io.join([frog.options.libDir, '..', 'leg']);

  void cancel([String reason, Node node, token, instruction, element]) {
    Script script = currentScript();
    if (node !== null) {
      print(spanFromNode(node, script).toMessageString("cancel leg: $reason"));
    } else if (token !== null) {
      int begin = token.charOffset;
      int end = begin + 1; // TODO(ahe): Compute proper length.
      print(script.file.getLocationMessage("cancel leg: $reason",
                                           begin, end, true));
    } else if (element !== null) {
      if (element.position() === null) {
        // Sometimes, the backend fakes up elements that have no
        // position. So we use the enclosing element instead. It is
        // not a good error location, but cancel really is "internal
        // error" or "not implemented yet", so the vicinity is good
        // enough for now.
        element = element.enclosingElement;
        // TODO(ahe): I plan to overhaul this infrastructure anyways.
      }
      if (element !== null) {
        withCurrentElement(element,
            () => cancel(reason: reason, token: element.position()));
      }
    } else if (currentElement !== null) {
      cancel(reason, element: currentElement);
      return;
    }
    if (throwOnError) {
      throw new AbortLeg(reason);
    }
    super.cancel(reason, node, token, instruction);
  }

  LibraryElement scanBuiltinLibrary(String filename) {
    String fileName = io.join([legDirectory, 'lib', filename]);
    Uri cwd = new Uri(scheme: 'file', path: currentDirectory);
    Uri uri = cwd.resolve(fileName);
    LibraryElement library = scanner.loadLibrary(uri, null);
    return library;
  }
}

class AbortLeg {
  final message;
  AbortLeg(this.message);
  toString() => 'Aborted due to --throw-on-error: $message';
}
