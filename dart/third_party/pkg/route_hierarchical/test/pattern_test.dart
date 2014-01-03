// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'package:unittest/unittest.dart';
import 'package:route_hierarchical/pattern.dart';

main() {
  test('matchAny', () {
    var p = matchAny(['a', 'b'], exclude: ['ac', 'aa']);
    expect(p.allMatches('a').map(matchValue), ['a']);
    expect(p.allMatches('ab').map(matchValue), ['a', 'b']);
    expect(p.allMatches('aa').map(matchValue), []);
    expect(p.allMatches('bb').map(matchValue), ['b', 'b']);
    expect(p.allMatches('aca').map(matchValue), []);
  });

  test('matchesFull', () {
    expect(matchesFull('a', 'aa'), false);
    expect(matchesFull('aa', 'aa'), true);
  });
}

String matchValue(Match m) => m.input.substring(m.start, m.end);
