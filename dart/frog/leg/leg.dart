// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('leg');

#import('../lang.dart', prefix: 'frog');

#import('elements/elements.dart');
#import('scanner.dart');
#import('scanner_implementation.dart');
#import('ssa/ssa.dart');
#import('tree.dart');
#import('util.dart');

#source('compiler.dart');
#source('resolver.dart');
#source('scanner_task.dart');
#source('parser_task.dart');
#source('script.dart');
#source('typechecker.dart');
#source('universe.dart');

final bool GENERATE_SSA_TRACE = false;

void unreachable() {
  throw const Exception("Internal Error (Leg): UNREACHABLE");
}

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
}
