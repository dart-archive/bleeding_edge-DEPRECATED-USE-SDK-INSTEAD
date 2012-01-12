// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("ui.dart");

void main() {
  // TODO(mattsh): temporary, the if(false) here is so that tree-shaking doesn't
  // delete frogPondMain.  (We want to be able to call frogPondMain at
  // the appropriate time after js setup.)
  if (false) {
    pondMain();
  }
}

void pondMain() {
  new PondUI().run();
}
