// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Separate entrypoint for the frog compiler with experimental support for the
// 'await' keyword.

#import('../lang.dart');
#import('../minfrog.dart', prefix:'minfrog');
#source('transformation.dart');

_awaitCompilationPhase() {
  world.withTiming('remove await expressions', awaitTransformation);
}

void main() {
  experimentalAwaitPhase = _awaitCompilationPhase;
  minfrog.main();
}
