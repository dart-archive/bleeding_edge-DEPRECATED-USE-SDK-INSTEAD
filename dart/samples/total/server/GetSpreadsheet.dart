// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class GetSpreadsheet {

  static void getSample(HttpRequest req, HttpResponse res) {
    switch (req.method) {
      case 'GET':
        String name = req.queryParameters['name'];
        res.outputStream.writeString(new SYLKProducer().makeExample(name));
        res.outputStream.close();
        break;
      case 'POST': // Fall through intended
      default:
        res.statusCode = HttpStatus.METHOD_NOT_ALLOWED;
        res.outputStream.close();
        break;
    }
  }

  static void listSamples(HttpRequest req, HttpResponse res) {
    switch (req.method) {
      case 'GET':
        res.outputStream.writeString(new SYLKProducer().listSamples());
        res.outputStream.close();
        break;
      case 'POST': // Fall through intended
      default:
        res.statusCode = HttpStatus.METHOD_NOT_ALLOWED;
        res.outputStream.close();
        break;
    }
  }
}



