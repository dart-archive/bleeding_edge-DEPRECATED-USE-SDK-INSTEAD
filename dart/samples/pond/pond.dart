// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("ui.dart");
#import("editors_isolate.dart"); // TODO(sigmund): remove this dependency
#import("editors_stub.dart");

void main() {
  // TODO(sigmund): rewrite as spawnFromCode('editors_isolate.dart.js');
  Future<SendPort> editorsPort = new EditorsIsolate().spawn();
  PondUI ui = new PondUI();
  editorsPort.then((SendPort port) {
    ui.setupAndRun(new EditorFactoryProxy(editorsPort));
  });
}
