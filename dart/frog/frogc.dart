// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('lang.dart', prefix: 'lang');
#import('leg/frog_leg.dart', prefix: 'leg');
#import('minfrogc.dart', prefix: 'minfrogc');

void main() {
  lang.legCompile = leg.compile;
  minfrogc.main();
}
