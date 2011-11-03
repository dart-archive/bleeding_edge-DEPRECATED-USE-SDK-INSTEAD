// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('../lang.dart');

#source('ParserTest.dart');
#source('TokenizerTest.dart');

// TODO(jimhug): Replace this with proper test harness integration.
void main() {
  TokenizerTest.main();
  ParserTest.main();
  print('all tests finished');
}