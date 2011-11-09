// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("io");

#import('../../lang.dart', prefix: 'frog');

String join(List<String> strings) => Strings.join(strings, '/');

frog.SourceFile readSync(String fileName) {
  return new frog.SourceFile(fileName, frog.world.files.readAll(fileName));
}
