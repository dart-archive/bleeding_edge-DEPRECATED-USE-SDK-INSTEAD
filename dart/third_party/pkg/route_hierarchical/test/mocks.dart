// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test_mocks;

import 'dart:html';
import 'package:unittest/mock.dart';

class MockWindow extends Mock implements Window {
  MockHistory history = new MockHistory();
  MockLocation location = new MockLocation();
  MockDocument document = new MockDocument();
}

class MockHistory extends Mock implements History {
  //TODO(pavelgj): ugly hack for making tests run in dart2js
  void pushState(Object data, String title, [String url]) {
    log.add(new LogEntry(name, 'pushState', [data, title, url], Action.IGNORE));
  }

  //TODO(pavelgj): ugly hack for making tests run in dart2js
  void replaceState(Object data, String title, [String url]) {
    log.add(new LogEntry(name, 'replaceState', [data, title, url], Action.IGNORE));
  }
}

class MockLocation extends Mock implements Location {}

class MockDocument extends Mock implements HtmlDocument {
  //TODO(pavelgj): ugly hack for making tests run in dart2js
  String set title(title) =>
      log.add(new LogEntry(name, '=title', [title], Action.IGNORE));
}
