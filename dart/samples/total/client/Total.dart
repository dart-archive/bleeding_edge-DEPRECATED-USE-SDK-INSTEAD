// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("Total");
#import("dart:html");
#import("TotalLib.dart");

void main() {
  // Instantiate the app
  new Total().run();

  document.on.keyDown.add((KeyboardEvent event) {
    // TODO: Browsers need to fix the keyCode/keyIdentifier situation.
    if (event.ctrlKey && event.keyCode == 68 /* d */) {
      Element db = document.query('#debugbar');
      if (db.classes.contains('on')) {
        db.classes.remove('on');
      } else {
        db.classes.add('on');
      }
    }
  }, false);
}
