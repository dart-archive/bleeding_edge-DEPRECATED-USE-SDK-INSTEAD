// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('server');

#import('../../../fling/fling.dart');

void main() {
  final port = 9090;
  HttpServer server = new HttpServer();
  server.handle("/", ClientApp.create("."));

  // TODO: Terrible hack to allow the html file to
  // request ../out/Total.app.js. This will actually get
  // requested as /out/Total.app.js so we can create a
  // a handler for those. Yuck.
  server.handle("/out/", ClientApp.create("."));

  server.listen(port);
  print("http://localhost:${port}/Total.html");
  Fling.goForth();
}
