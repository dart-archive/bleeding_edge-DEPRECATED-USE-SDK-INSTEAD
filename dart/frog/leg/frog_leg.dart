// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('frog_leg');

#import('../lang.dart', prefix: 'frog');
#import('elements/elements.dart');
#import('io/io.dart', prefix: 'io');
#import('leg.dart');
#import('tree/tree.dart');

bool compile(frog.World world) {
  final file = world.readFile(frog.options.dartScript);
  final throwOnError = frog.options.throwOnErrors;
  final compiler = new WorldCompiler(world, throwOnError);
  final script = new Script(file);
  return compiler.run(script);
}

class WorldCompiler extends Compiler {
  final frog.World world;
  final bool throwOnError;

  WorldCompiler(this.world, this.throwOnError) : super();

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
    final endOffset = end.charOffset + end.toString().length;
    return new frog.SourceSpan(current.file, startOffset, endOffset);
  }

  currentScript() {
    CompilationUnitElement compilationUnit =
        currentElement.getEnclosingCompilationUnit();
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

  Script readScript(String filename) {
    String text = frog.world.files.readAll(filename);
    frog.SourceFile sourceFile = new frog.SourceFile(filename, text);
    return new Script(sourceFile);
  }

  String get legDirectory() => io.join([frog.options.libDir, '..', 'leg']);

  void cancel([String reason, Node node, token, instruction, element]) {
    Script script = currentScript();
    if (node !== null) {
      print(spanFromNode(node, script).toMessageString("cancel leg: $reason"));
    } else if (token !== null) {
      String tokenString = token.toString();
      int begin = token.charOffset;
      int end = begin + tokenString.length;
      print(script.file.getLocationMessage("cancel leg: $reason",
                                           begin, end, true));
    } else if (element !== null) {
      currentElement = element;
      cancel(reason: reason, token: element.position());
    }
    if (throwOnError) {
      throw new AbortLeg(reason);
    }
    super.cancel(reason, node, token, instruction);
  }
}

class AbortLeg {
  final message;
  AbortLeg(this.message);
  toString() => 'Aborted due to --throw-on-error: $message';
}
