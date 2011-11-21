// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('frog_leg');

#import('../lang.dart', prefix: 'frog');
#import('io/io.dart', prefix: 'io');
#import('leg.dart');
#import('tree/tree.dart');

bool compile(frog.World world) {
  final file = world.readFile(frog.options.dartScript);
  final script = new Script(file);
  final compiler = new WorldCompiler(world, script);
  return compiler.run();
}

class WorldCompiler extends Compiler {
  final frog.World world;

  WorldCompiler(this.world, Script script) : super(script);

  void log(message) {
    if (frog.options.showInfo) {
      // Avoid calling message.toString() unless showInfo is on. It
      // could be slow.
      world.info('[leg] $message');
    }
  }

  bool run() {
    bool success = super.run();
    if (success) {
      var code = getGeneratedCode();
      world.legCode = code;
      world.jsBytesWritten = code.length;
      for (final task in tasks) {
        log('${task.name} took ${task.timing}msec');
      }
    }
    return success;
  }

  spanFromNode(Node node) {
    final begin = node.getBeginToken();
    final end = node.getEndToken();
    if (begin === null || end === null) {
      cancel('cannot find tokens to produce error message for $node.');
    }
    final startOffset = begin.charOffset;
    final endOffset = end.charOffset + end.toString().length;
    return new frog.SourceSpan(script.file, startOffset, endOffset);
  }

  reportWarning(Node node, String message) {
    world.warning('$message.', spanFromNode(node));
  }

  Script readScript(String filename) {
    String text = frog.world.files.readAll(filename);
    frog.SourceFile sourceFile = new frog.SourceFile(filename, text);
    return new Script(sourceFile);
  }

  String get legDirectory() => io.join([frog.options.libDir, '..', 'leg']);
}
