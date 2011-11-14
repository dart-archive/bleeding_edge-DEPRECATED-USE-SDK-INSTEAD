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

add(var a, var b) {
  return JS(@"$0 + $1", a, b);
}

div(var a, var b) {
  return JS(@"$0 / $1", a, b);
}

mul(var a, var b) {
  return JS(@"$0 * $1", a, b);
}

sub(var a, var b) {
  return JS(@"$0 - $1", a, b);
}

eq(var a, var b) {
  return JS(@"$0 == $1", a, b);
}

tdiv(var a, var b) {
  var tmp = a / b;
  // TODO(ngeoffray): Use tmp.floor and tmp.ceil when
  // we can handle them.
  if (tmp < 0) {
    return JS(@"Math.ceil($0)", tmp);
  } else {
    return JS(@"Math.floor($0)", tmp);
  }
}
