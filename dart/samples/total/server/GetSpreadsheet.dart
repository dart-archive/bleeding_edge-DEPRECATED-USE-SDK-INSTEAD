// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class GetSpreadsheet {

  static void getSample(HTTPRequest req, HTTPResponse res) {
    switch (req.method) {
      case 'GET':
        String name = req.queryParameters['name'];
        res.writeString(new SYLKProducer().makeExample(name));
        res.writeDone();
        break;
      case 'POST': // Fall through intended
      default:
        res.statusCode = HTTPStatus.METHOD_NOT_ALLOWED;
        res.writeDone();
        break;
    }
  }

  static void listSamples(HTTPRequest req, HTTPResponse res) {
    switch (req.method) {
      case 'GET':
        res.writeString(new SYLKProducer().listSamples());
        res.writeDone();
        break;
      case 'POST': // Fall through intended
      default:
        res.statusCode = HTTPStatus.METHOD_NOT_ALLOWED;
        res.writeDone();
        break;
    }
  }
}



