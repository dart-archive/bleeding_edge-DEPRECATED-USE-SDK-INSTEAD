// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:html");
#import("editors_stub.dart");
#import("ui.dart");

void main() {
  PondUI ui = new PondUI();
  window.on.contentLoaded.add((e) {
    ui.setupAndRun(new EditorFactoryStub());
  });
}
