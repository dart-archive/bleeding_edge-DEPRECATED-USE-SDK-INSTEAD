// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void print(var obj) {
  // TODO(ngeoffray): enable when the parser accepts it.
  // obj = obj.toString();
  var hasConsole = JS(@"typeof console == 'object'");
  if (hasConsole) {
    JS(@"console.log($0)", obj);
  } else {
    JS(@"write($0)", obj);
    JS(@"write('\n')");
  }
}
