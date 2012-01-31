// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('../lang.dart');

// TODO(jimhug): Turn this into proper benchmark!

// TODO(jimhug): This is BROKEN with the move to experimental - will fix.

/** Path to starting library or application. */
final String ROOT = '../samples/swarm/swarm.dart';

/** Path to core library. */
final String CORE = 'lib/corelib.dart';

void main() {
  var t0 = Clock.now();
  initializeWorld(CORE);
  world.getOrAddLibrary(ROOT);
  world.process();
  var t1 = Clock.now();
  print ('first pass in ${(t1 - t0) / Clock.frequency()}secs');

  testTokenize(world.sourcefiles);
  testParse(world.sourcefiles, true);
  testParse(world.sourcefiles, false);

  world.resolveAll();
}

void testTokenize(List<String> files) {
  int count = 0;
  final t0 = Clock.now();

  for (var source in files) {
    final tokenizer = new Tokenizer(source, true);

    while (true) {
      final tok = tokenizer.next();
      if (tok.kind == TokenKind.ERROR) {
        print(source.getLocationMessage('error ${tok}', tok.start, tok.end));
      }
      count += 1;
      if (tok.kind == TokenKind.END_OF_FILE) break;
    }
  }

  final t1 = Clock.now();
  final totalTime = (t1 - t0) / Clock.frequency();
  final tps = count / totalTime;
  print('$count tokens in $totalTime secs, $tps tokens/sec');
}

void testParse(List<String> files, [bool diet=false]) {
  int lines = 0;

  final t0 = Clock.now();

  for (var source in files) {
    final t00 = Clock.now();
    var p = new Parser(source, diet);
    var decl = p.compilationUnit();
    var lines0 = lines;
    lines += 1;
    var code = source.text;
    for (int i = 0; i < code.length; i++) {
      if (code.charCodeAt(i) == 10/*newline*/) lines++;
    }
    final t11 = Clock.now();
    final totalTime = (t11 - t00) / Clock.frequency();
    final lps = (lines - lines0) / totalTime;
    if (totalTime > 0.1) {
      print('${source.filename} parsed in ${totalTime}sec, $lps lines/sec');
    }
  }

  final t1 = Clock.now();
  final totalTime = (t1 - t0) / Clock.frequency();
  final lps = lines/totalTime;
  print('${diet ? "diet " : ""}parsed $lines lines in $totalTime sec, $lps lines/sec');
}
